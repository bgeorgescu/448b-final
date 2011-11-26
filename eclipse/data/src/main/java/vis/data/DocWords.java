package vis.data;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vis.data.model.DocWord;
import vis.data.model.RawDoc;
import vis.data.model.meta.IdLists;
import vis.data.model.meta.WordCache;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

//take the raw table and process each doc into a list of words that hit
//insert those words into a worddoc table
//later we transform this to a docword table
public class DocWords {	
	static int g_next_doc = 0;
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Date start = new Date();
		
		Connection conn = SQL.forThread();

		//first load all the document ids
		final int[] all_doc_ids = IdLists.allDocs();
		
		try {
			SQL.createTable(conn, DocWord.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create table of words for documents", e);
		}
		
		final BlockingQueue<RawDoc> doc_to_process = new ArrayBlockingQueue<RawDoc>(100);
		//thread to scan for documents to process
		
		final int BATCH_SIZE = 1000;
 		final Thread doc_scan_thread[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < doc_scan_thread.length; ++i) {
			doc_scan_thread[i] = new Thread() {
				public void run() {
					Connection conn = SQL.forThread();
					try {
						PreparedStatement query_fulltext = conn.prepareStatement("SELECT " + RawDoc.FULL_TEXT + " FROM " + RawDoc.TABLE + " WHERE " + RawDoc.ID + " = ?");

						for(;;) {
							int doc_id = -1;
							synchronized(doc_scan_thread) {
								if(g_next_doc == all_doc_ids.length) {
									break;
								}
								doc_id = all_doc_ids[g_next_doc++];
							}
							query_fulltext.setInt(1, doc_id);
							ResultSet rs = query_fulltext.executeQuery();
							try {
								if(!rs.next()) {
									throw new RuntimeException("failed to get full text for doc " + doc_id);
								}
								RawDoc doc = new RawDoc();
								doc.id_ = doc_id;
								doc.fullText_ = rs.getString(1);
								try {
									doc_to_process.put(doc);
								} catch (InterruptedException e) {
									throw new RuntimeException("Unknown interupt while pulling inserting in doc queue", e);
								}
							} finally {
								rs.close();
							}
						}
					} catch (SQLException e) {
						throw new RuntimeException("failed to enumerate documents", e);
					}
					finally
					{
						try {
							conn.close();
							System.out.println ("Database connection terminated");
						} catch (SQLException e) {
							throw new RuntimeException("Database connection terminated funny", e);
						}
					}
				}
			};
			doc_scan_thread[i].start();
		}
		final WordCache wc = WordCache.getInstance();
		
		//TODO: XXXX super lame, neglects punctuation, etc
		final Pattern word_pattern = Pattern.compile("\\w+");
		//threads to process individual files
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
		final BlockingQueue<DocWord> hits_to_record = new ArrayBlockingQueue<DocWord>(10000);
		for(int i = 0; i < processing_threads.length; ++i) {
			processing_threads[i] = new Thread() {
				public void run() {
					for(;;) {
						if(doc_to_process.isEmpty()) {
							boolean still_running = false;
							for(int i = 0; i < doc_scan_thread.length; ++i)
								still_running |= doc_scan_thread[i].isAlive();
							if(!still_running) {
								break;
							}
						}
						RawDoc doc;
						try {
							doc = doc_to_process.poll(5, TimeUnit.MILLISECONDS);
							//maybe we are out of work
							if(doc == null) {
								//System.out.println("starving counter");
								continue;
							}
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling from doc queue", e);
						}
						
						Matcher m = word_pattern.matcher(doc.fullText_);
						HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
						while(m.find()) {
							int word_id = wc.getOrAddWord(m.group());
							Integer count = counts.get(word_id);
							if(count == null) {
								counts.put(word_id, 1);
							} else {
								counts.put(word_id, count.intValue() + 1);
							}
						}
						ByteBuffer bb = ByteBuffer.allocate(counts.size() * 2 * Integer.SIZE / 8);
						for(Entry<Integer, Integer> entry : counts.entrySet()) {
							bb.putInt(entry.getKey()); //word id
							bb.putInt(entry.getValue()); //count
						}
						DocWord wd = new DocWord();
						wd.docId_ = doc.id_;
						wd.wordList_ = bb.array();
						try {
							hits_to_record.put(wd);
						} catch (InterruptedException e) {
							throw new RuntimeException("failure record hit results", e);
						}
					}
				}
			};
			processing_threads[i].start();
		}
		Thread mysql_thread = new Thread() {
			public void run() {
				Connection conn = SQL.forThread();
				
				int current_batch_partial = 0;
				int batch = 0;
				try {
					conn.setAutoCommit(false);

					PreparedStatement insert = conn.prepareStatement(
							"INSERT INTO " + DocWord.TABLE + "(" + DocWord.DOC_ID + "," + DocWord.WORD_LIST + ") " + 
							"VALUES (?, ?)");
					for(;;) {
						if(hits_to_record.isEmpty()) {
							boolean still_running = false;
							for(int i = 0; i < processing_threads.length; ++i)
								still_running |= processing_threads[i].isAlive();
							if(!still_running) {
								//submit the remaining incomplete batch
								if(current_batch_partial != 0)
									insert.executeBatch();
								break;
							}
						}
						DocWord hit;
						try {
							hit = hits_to_record.poll(5, TimeUnit.MILLISECONDS);
							//maybe we are out of work
							if(hit == null) {
								//System.out.println("starving mysql");
								continue;
							}
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling from hit queue", e);
						}

						insert.setInt(1, hit.docId_);
						insert.setBytes(2, hit.wordList_);
						insert.addBatch();

						if(++current_batch_partial == BATCH_SIZE) {
							System.out.println ("Inserting Batch " + batch++);
							insert.executeBatch();
							current_batch_partial = 0;
						}
						
					}
				} catch (SQLException e) {
					throw new RuntimeException("failed to insert documents", e);
				}
				finally
				{
					try {
						conn.close();
						System.out.println ("Database connection terminated");
					} catch (SQLException e) {
						throw new RuntimeException("Database connection terminated funny", e);
					}
				}
			}
		};
		mysql_thread.start();
		
		//wait until all scanning is complete
		try {
			for(Thread t : doc_scan_thread)
				t.join();
			//then wait until all processing is complete
			for(Thread t : processing_threads)
				t.join();
			//then wait until all the sql is complete
			mysql_thread.join();
			Date end = new Date();
			long millis = end.getTime() - start.getTime();
			System.err.println("completed insert in " + millis + " milliseconds");
		} catch (InterruptedException e) {
			throw new RuntimeException("unknwon interrupt", e);
		}
	}
}
