package vis.data.assortedjunk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import vis.data.model.DocLemma;
import vis.data.model.meta.EntityHits;
import vis.data.model.meta.IdLists;
import vis.data.model.meta.LemmaHits;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

//i accidentally had an off by one error in the word/lemma/entity cache.
//this fixes up the table that needed 20 hrs to generate
//shouldnt be necessary for anyone else, but my be a good skeleton
public class FixDocLemma {
	static int g_batch = 0;
	static int g_next_doc = 0;
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		Connection conn = SQL.open();

		//first load all the document ids and lemma ids
		final int[] all_doc_ids = IdLists.allProcessedDocs(conn);
		
		final int BATCH_SIZE = 100;
 		final Thread doc_scan_thread[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < doc_scan_thread.length; ++i) {
			doc_scan_thread[i] = new Thread() {
				public void run() {
					Connection conn = SQL.open();
					int current_batch_partial = 0;
					try {
						LemmaHits lh = new LemmaHits(conn);
						EntityHits eh = new EntityHits(conn);
						PreparedStatement update = conn.prepareStatement("UPDATE " + DocLemma.TABLE+ " SET " + DocLemma.LEMMA_LIST + " = ?, " + 
								DocLemma.ENTITY_LIST + " = ? WHERE " + DocLemma.DOC_ID + " = ?");

						for(;;) {
							int doc_id = -1;
							synchronized(doc_scan_thread) {
								if(g_next_doc == all_doc_ids.length) {
									if(current_batch_partial != 0)
										update.executeBatch();
									break;
								}
								doc_id = all_doc_ids[g_next_doc++];
							}
							LemmaHits.Counts lc = lh.getLemmaCounts(doc_id);
							EntityHits.Counts ec = eh.getEntityCounts(doc_id);
							
							//actual bug was that sql transparent remaps insert (primary key) values (0) to have a key of 1
							//then the subsequent insert of item one fails.  So we lost the real item 1 effectively
							for(int i = 0; i < lc.lemmaId_.length; ++i) {
								if(lc.lemmaId_[i] == 0)
									lc.lemmaId_[i] = 1;
								else if(lc.lemmaId_[i] == 1)
									lc.lemmaId_[i] = 0;
							}
							for(int i = 0; i < ec.entityId_.length; ++i) {
								if(ec.entityId_[i] == 0)
									ec.entityId_[i] = 1;
								else if(ec.entityId_[i] == 1)
									ec.entityId_[i] = 0;
							}
							DocLemma dl = new DocLemma();
							dl.docId_ = doc_id;
							LemmaHits.pack(dl, lc);
							EntityHits.pack(dl, ec);
							update.setBytes(1, dl.lemmaList_);
							update.setBytes(2, dl.entityList_);
							update.setInt(3, doc_id);
							update.addBatch();

							if(++current_batch_partial == BATCH_SIZE) {
								synchronized(FixDocLemma.class) {
									System.out.println ("Inserting Batch " + g_batch++);
								}
								update.executeBatch();
								current_batch_partial = 0;
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
		
	}
}
