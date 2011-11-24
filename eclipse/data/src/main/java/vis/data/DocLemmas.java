package vis.data;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import vis.data.model.DocLemma;
import vis.data.model.RawDoc;
import vis.data.model.meta.IdLists;
import vis.data.util.EntityCache;
import vis.data.util.ExceptionHandler;
import vis.data.util.LemmaCache;
import vis.data.util.SQL;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

//take the raw table and process each doc into a list of words that hit
//insert those words into a worddoc table
//later we transform this to a docword table
public class DocLemmas {	
	static int g_next_doc = 0;
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Date start = new Date();
		
		Connection conn = SQL.open();

		//first load all the document ids
		final int[] all_doc_ids = IdLists.allDocs(conn);
		
		try {
			SQL.createTable(conn, DocLemma.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create table of words for documents", e);
		}

		final BlockingQueue<RawDoc> doc_to_process = new ArrayBlockingQueue<RawDoc>(100);
		//thread to scan for documents to process
		
		final int BATCH_SIZE = 100;
 		final Thread doc_scan_thread[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < doc_scan_thread.length; ++i) {
			doc_scan_thread[i] = new Thread() {
				public void run() {
					Connection conn = SQL.open();
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
		final LemmaCache lc = LemmaCache.getInstance();
		final EntityCache ec = EntityCache.getInstance();
	    Properties props = new Properties();
	    props.put("ner.applyNumericClassifiers", "false"); //?
	    
	    //didnt do what i wanted
	    //props.put("ner.useNGrams", "true");
	    //props.put("ner.dehyphenateNGrams", "true");
	    
	    //props.put("ner.useSUTime", "false"); //?
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
	    final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		final Pattern p = Pattern.compile("(?:-|(?:\\/))");
		//threads to process individual files
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
		final BlockingQueue<DocLemma> hits_to_record = new ArrayBlockingQueue<DocLemma>(10000);
		for(int i = 0; i < processing_threads.length; ++i) {
			processing_threads[i] = new Thread() {
				public void run() {
					HashMap<Integer, Integer> lemma_counts = new HashMap<Integer, Integer>();
					HashMap<Integer, Integer> entity_counts = new HashMap<Integer, Integer>();
									    				    
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
						
						
						//this is dehyphenating and weirdo slashing
					    Annotation document = new Annotation(p.matcher(doc.fullText_).replaceAll(" "));
					    pipeline.annotate(document);
					    
					    lemma_counts.clear();
					    entity_counts.clear();
					    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
					    for(CoreMap sentence: sentences) {
						    String last_ner = "O";
						    String entity = "";
					    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
					    		String word = token.get(TextAnnotation.class);
					    		String ne = token.get(NamedEntityTagAnnotation.class);
					    		String lemma = token.get(LemmaAnnotation.class);
					    		String pos = token.get(PartOfSpeechAnnotation.class);
					    		if(ne.equals("O") || !last_ner.equals(ne)) {
				    				handleEntity(ec, last_ner, entity, entity_counts);
				    				entity = "";
					    		} 
					    		if(!ne.equals("O")) {
					    			if(!entity.isEmpty())
					    				entity += " ";
					    			entity += word;
						    		last_ner = ne;
					    			continue;
					    		}
			    				entity = "";
					    		last_ner = "O";
					    		if(!Character.isLetter(word.charAt(0))) {
					    			//skip punctuation
					    			continue;
					    		}
					    		//System.out.println("word: " + word + " lemma: " + lemma + " pos: " + pos + " ne: " + ne);
					    		int lemma_id = lc.getOrAddLemma(lemma, pos);
			    				Integer v = lemma_counts.get(lemma_id);
			    				if(v == null)
			    					lemma_counts.put(lemma_id, 1);
			    				else
			    					lemma_counts.put(lemma_id, v + 1);
					    	}
					    	if(!last_ner.equals("O")) {
			    				handleEntity(ec, last_ner, entity, entity_counts);
					    	}
					    }
						ByteBuffer lemma_bb = ByteBuffer.allocate(lemma_counts.size() * 2 * Integer.SIZE / 8);
						ByteBuffer entity_bb = ByteBuffer.allocate(entity_counts.size() * 2 * Integer.SIZE / 8);
						for(Entry<Integer, Integer> entry : lemma_counts.entrySet()) {
							lemma_bb.putInt(entry.getKey()); //lemma id
							lemma_bb.putInt(entry.getValue()); //count
						}
						for(Entry<Integer, Integer> entry : entity_counts.entrySet()) {
							entity_bb.putInt(entry.getKey()); //entity id
							entity_bb.putInt(entry.getValue()); //count
						}
						DocLemma wd = new DocLemma();
						wd.docId_ = doc.id_;
						wd.lemmaList_ = lemma_bb.array();
						wd.entityList_ = entity_bb.array();
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
				Connection conn = SQL.open();
				
				int current_batch_partial = 0;
				int batch = 0;
				try {
					conn.setAutoCommit(false);

					PreparedStatement insert = conn.prepareStatement(
							"INSERT INTO " + DocLemma.TABLE + "(" + DocLemma.DOC_ID + "," + DocLemma.LEMMA_LIST + "," + DocLemma.ENTITY_LIST + ") " + 
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
						DocLemma hit;
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
						insert.setBytes(2, hit.lemmaList_);
						insert.setBytes(3, hit.entityList_);
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
	private static void handleEntity(final EntityCache ec, String ne,
			String entity, HashMap<Integer, Integer> entity_counts) {
		switch(ne) {
		case "O":
			break;
		case "NUMBER":
		case "DURATION":
		case "DATE":
		case "TIME":
		case "MONEY":
		case "ORDINAL":
		case "MISC":
		case "PERCENT":
		case "SET":
			//System.err.println("wanted to be disabled named entity type: " + ne);
			break;
		case "PERSON":
		case "LOCATION":
		case "ORGANIZATION":
			int entity_id = ec.getOrAddEntity(entity, ne);
			Integer v = entity_counts.get(entity_id);
			if(v == null)
				entity_counts.put(entity_id, 1);
			else
				entity_counts.put(entity_id, v + 1);
			break;
		default:
			System.err.println("unknown named entity type: " + ne);
		}
	}
}
