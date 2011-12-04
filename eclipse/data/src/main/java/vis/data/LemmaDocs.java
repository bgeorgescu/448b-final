package vis.data;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.DbUtils;

import vis.data.model.DocLemma;
import vis.data.model.LemmaDoc;
import vis.data.model.meta.DocForLemmaAccessor;
import vis.data.model.meta.IdListAccessor;
import vis.data.model.meta.LemmaForDocHitsAccessor;
import vis.data.model.meta.LemmaForDocHitsAccessor.Counts;
import vis.data.util.CountAggregator;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

//take doc lemma lists, load them in
//transform them to be by lemma with doc lists
public class LemmaDocs {	
	static int g_batch = 0;
	static int g_next_doc = 0;
	static int g_next_lemma = 0;
	static int g_max_err = 100;
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Date start = new Date();
		
		Connection conn = SQL.forThread();

		//first load all the document ids and lemma ids
		final int[] all_doc_ids = IdListAccessor.allProcessedDocs();
		final int[] all_lemma_ids = IdListAccessor.allLemmas();

		class PartialDocLemmaHitsCounts {
			TIntArrayList docId_ = new TIntArrayList();
			TIntArrayList count_ = new TIntArrayList();
		};
		final TIntObjectHashMap<PartialDocLemmaHitsCounts> lemma_doc = new TIntObjectHashMap<PartialDocLemmaHitsCounts>();
		for(int i = 0; i < all_lemma_ids.length; ++i) {
			lemma_doc.put(all_lemma_ids[i], new PartialDocLemmaHitsCounts());
		}
		
		try {
			SQL.createTable(conn, LemmaDoc.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create table of words for documents", e);
		}

		final BlockingQueue<LemmaForDocHitsAccessor.Counts> doc_to_process = new ArrayBlockingQueue<LemmaForDocHitsAccessor.Counts>(100);
		//thread to scan for documents to process
		
		final int BATCH_SIZE = 100;
 		final Thread doc_scan_thread[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < doc_scan_thread.length; ++i) {
			doc_scan_thread[i] = new Thread() {
				public void run() {
					Connection conn = SQL.forThread();
					try {
						LemmaForDocHitsAccessor lh = new LemmaForDocHitsAccessor();
						PreparedStatement query_lemma_list = conn.prepareStatement("SELECT " + DocLemma.LEMMA_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " = ?");

						for(;;) {
							int doc_id = -1;
							synchronized(doc_scan_thread) {
								if(g_next_doc == all_doc_ids.length) {
									break;
								}
								doc_id = all_doc_ids[g_next_doc++];
							}
							query_lemma_list.setInt(1, doc_id);
							ResultSet rs = query_lemma_list.executeQuery();
							try {
								if(!rs.next()) {
									throw new RuntimeException("failed to get doc list for  " + doc_id);
								}
								LemmaForDocHitsAccessor.Counts doc = lh.getLemmaCounts(doc_id);
								try {
									doc_to_process.put(doc);
								} catch (InterruptedException e) {
									throw new RuntimeException("Unknown interupt while inserting in doc queue", e);
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
						DbUtils.closeQuietly(conn);
						System.out.println ("Database connection terminated");
					}
				}
			};
			doc_scan_thread[i].start();
		}
		//threads to process individual docs
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
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
						Counts doc;
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
						
						//this is just a 1-1 re-transform of the data
						for(int i = 0; i < doc.lemmaId_.length; ++i) {
							PartialDocLemmaHitsCounts pdc = lemma_doc.get(doc.lemmaId_[i]);
							if(pdc == null) {
								if(g_max_err > 0) {
									g_max_err--;
									System.err.println("fail " + doc.docId_ + " " + doc.lemmaId_[i]);
									if(g_max_err == 0)
										System.err.println("failed too many times supressing");
									
								}
								continue;
							}
							synchronized (pdc) {
								pdc.docId_.add(doc.docId_);
								pdc.count_.add(doc.count_[i]);
							}
						}
					}
				}
			};
			processing_threads[i].start();
		}
		//wait until all scanning is complete
		try {
			for(Thread t : doc_scan_thread)
				t.join();
			//then wait until all processing is complete
			for(Thread t : processing_threads)
				t.join();
			Date end = new Date();
			long millis = end.getTime() - start.getTime();
			System.err.println("completed process in " + millis + " milliseconds");
			start = new Date();
		} catch (InterruptedException e) {
			throw new RuntimeException("unknwon interrupt", e);
		}

		//threads to process individual sort/pack/inserts
		final Thread mysql_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < mysql_threads.length; ++i) {
			mysql_threads[i] = new Thread() {
				public void run() {
					Connection conn = SQL.forThread();
					
					int current_batch_partial = 0;
					try {
						conn.setAutoCommit(false);
	
						PreparedStatement insert = conn.prepareStatement(
								"INSERT INTO " + LemmaDoc.TABLE + "(" + LemmaDoc.LEMMA_ID + "," + LemmaDoc.DOC_LIST  + ") " + 
								"VALUES (?, ?)");
						for(;;) {
							DocForLemmaAccessor.Counts dlc = new DocForLemmaAccessor.Counts();
							dlc.lemmaId_ = -1;
							synchronized(mysql_threads) {
								if(g_next_lemma == all_lemma_ids.length) {
									break;
								}
								dlc.lemmaId_ = all_lemma_ids[g_next_lemma++];
							}
							
							PartialDocLemmaHitsCounts pdc = lemma_doc.get(dlc.lemmaId_);
							dlc.docId_ = pdc.docId_.toArray();
							dlc.count_ = pdc.count_.toArray();
							CountAggregator.sortByIdAsc(dlc.docId_, dlc.count_);
							LemmaDoc ld = DocForLemmaAccessor.pack(dlc);
							
							insert.setInt(1, ld.lemmaId_);
							insert.setBytes(2, ld.docList_);
	
							insert.addBatch();
	
							if(++current_batch_partial == BATCH_SIZE) {
								synchronized(LemmaDocs.class) {
									System.out.println ("Inserting Batch " + g_batch++);
								}
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
			mysql_threads[i].start();
		}
		
		//wait until all inserts are complete
		try {
			//then wait until all the sql is complete
			for(Thread t : mysql_threads)
				t.join();
			Date end = new Date();
			long millis = end.getTime() - start.getTime();
			System.err.println("completed insert in " + millis + " milliseconds");
		} catch (InterruptedException e) {
			throw new RuntimeException("unknwon interrupt", e);
		}
	}
}
