package vis.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vis.data.model.RawDoc;
import vis.data.model.RawHit;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;
import vis.data.util.WordCache;

//take the raw table and process the full_text into words
//insert those words into a rawword table
//write count for the words out in a rawhit table
public class RawHits {	
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		final BlockingQueue<RawDoc> doc_to_process = new ArrayBlockingQueue<RawDoc>(100);
		//thread to scan for documents to process
		
		final int BATCH_SIZE = 200;
		final Thread doc_scan_thread = new Thread() {
			public void run() {
				Connection conn = SQL.open();
				try {
					Statement st = null;
					//TODO: better to split slice this across a few threads because it is the bottleneck
					st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
					st.setFetchSize(Integer.MIN_VALUE);
					ResultSet rs = st.executeQuery("SELECT " + RawDoc.ID + "," + RawDoc.FULL_TEXT + " FROM " + RawDoc.TABLE);

					if(!rs.next())
						throw new RuntimeException("no docs to processs");
					
					do {
						RawDoc doc = new RawDoc();
						doc.id_ = rs.getInt(1);
						doc.fullText_ = rs.getString(2);
						try {
							doc_to_process.put(doc);
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling inserting in doc queue", e);
						}
					} while(rs.next());
					st.close();
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
		doc_scan_thread.start();
		final WordCache wc = WordCache.getInstance();
		
		//TODO: XXXX super lame, neglects punctuation, etc
		final Pattern word_pattern = Pattern.compile("\\w+");
		//threads to process individual files
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
		final BlockingQueue<RawHit> hits_to_record = new ArrayBlockingQueue<RawHit>(1000);
		for(int i = 0; i < processing_threads.length; ++i) {
			processing_threads[i] = new Thread() {
				public void run() {
					while(!doc_to_process.isEmpty() || doc_scan_thread.isAlive()) {
						RawDoc doc;
						try {
							doc = doc_to_process.poll(5, TimeUnit.MILLISECONDS);
							//maybe we are out of work
							if(doc == null)
								continue;
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
						for(Entry<Integer, Integer> entry : counts.entrySet()) {
							RawHit hit = new RawHit();
							hit.docId_ = doc.id_;
							hit.wordId_ = entry.getKey();
							hit.count_ = entry.getValue();
							try {
								hits_to_record.put(hit);
							} catch (InterruptedException e) {
								throw new RuntimeException("failure record hit results", e);
							}
						}
					}
				}
			};
			processing_threads[i].start();
		}
		Thread mysql_thread = new Thread() {
			public void run() {
				Connection conn = SQL.open();
				
				int current_batch_partial = 0;
				int batch = 0;
				try {
					SQL.createTable(conn, RawHit.class);

					PreparedStatement insert = conn.prepareStatement(
							"INSERT INTO " + RawHit.TABLE + "(" + RawHit.DOC_ID + "," + RawHit.WORD_ID + "," + RawHit.COUNT + ") " + 
							"VALUES (?, ?, ?)");
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
						RawHit hit;
						try {
							hit = hits_to_record.poll(5, TimeUnit.MILLISECONDS);
							//maybe we are out of work
							if(hit == null)
								continue;
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling from hit queue", e);
						}

						insert.setInt(1, hit.docId_);
						insert.setInt(2, hit.wordId_);
						insert.setInt(3, hit.count_);
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
			doc_scan_thread.join();
			//then wait until all processing is complete
			for(Thread t : processing_threads)
				t.join();
			//then wait until all the sql is complete
			mysql_thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("unknwon interrupt", e);
		}
	}
}
