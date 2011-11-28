package vis.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import vis.data.model.DocCoref;
import vis.data.model.RawDoc;
import vis.data.model.meta.DocRaw;
import vis.data.model.meta.EntityCache;
import vis.data.model.meta.EntityRaw;
import vis.data.model.meta.IdLists;
import vis.data.model.meta.LemmaCache;
import vis.data.model.meta.LemmaEntityCorefs;
import vis.data.model.meta.LemmaEntityCorefs.Corefs;
import vis.data.model.meta.LemmaEntityCorefs.PhraseType;
import vis.data.model.meta.LemmaEntityCorefs.Ref;
import vis.data.model.meta.LemmaRaw;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import gnu.trove.list.array.TIntArrayList;

//take the raw table and process each doc into a set of coreferences
//using the stanford corenlp tools
//TBD: sentence trees
public class Coreferences {	
	static int g_next_doc = 0;
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Date start = new Date();
		
		Connection conn = SQL.forThread();

		//first load all the document ids
		final int[] all_doc_ids = IdLists.allDocs();
		
		try {
			SQL.createTable(conn, DocCoref.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create table of words for documents", e);
		}

		final BlockingQueue<RawDoc> doc_to_process = new ArrayBlockingQueue<RawDoc>(100);
		//thread to scan for documents to process
		
		final int BATCH_SIZE = 1;
 		final Thread doc_scan_thread[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < doc_scan_thread.length; ++i) {
			doc_scan_thread[i] = new Thread() {
				public void run() {
					Connection conn = SQL.forThread();
					try {
						DocRaw dr = new DocRaw();
						for(;;) {
							int doc_id = -1;
							synchronized(doc_scan_thread) {
								if(g_next_doc == all_doc_ids.length) {
									break;
								}
								doc_id = all_doc_ids[g_next_doc++];
							}
							RawDoc doc = dr.getDocFull(doc_id);
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
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		final Pattern p0 = Pattern.compile("(?:-|(?:\\/))");
		final Pattern p1 = Pattern.compile("[^\\.][\\r\\n]+");
		//threads to process individual files
		//TODO: looks like this is still thread unsafe in corenlp
		final Thread processing_threads[] = new Thread[1];//Runtime.getRuntime().availableProcessors()];
		final BlockingQueue<DocCoref> hits_to_record = new ArrayBlockingQueue<DocCoref>(10000);
		for(int i = 0; i < processing_threads.length; ++i) {
			processing_threads[i] = new Thread() {
				public void run() {									    				    
					Connection conn = SQL.forThread();
					try {
						//used for logging only
						EntityRaw er = new EntityRaw();
						LemmaRaw lr = new LemmaRaw();
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
							
							
							String full_text = p0.matcher(doc.fullText_).replaceAll(" ");
							full_text = p1.matcher(full_text).replaceAll(".\n");
							//this is dehyphenating and weirdo slashing
						    Annotation document = new Annotation(full_text);
						    pipeline.annotate(document);
						    
						    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	
	
						    ArrayList<LemmaEntityCorefs.Ref[]> corefs = new ArrayList<Ref[]>();
						    ArrayList<LemmaEntityCorefs.Ref> active_set = new ArrayList<Ref>();
						    ArrayList<LemmaEntityCorefs.PhraseType> active_type = new ArrayList<PhraseType>();
						    TIntArrayList active_id = new TIntArrayList();
	
						    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
						    for(CorefChain cc : graph.values()) {
						    	List<CorefMention> cms = cc.getCorefMentions();
						    	//not a corefernce
						    	if(cms.size() <= 1)
						    		continue;
						    	active_set.clear();
						    	CorefMention rep = cc.getRepresentativeMention();
					    		handleMention(ec, lc, sentences, active_set, active_type, active_id, rep);
						    	for(CorefMention cm : cms) {
						    		if(cm != rep)
						    			handleMention(ec, lc, sentences, active_set, active_type, active_id, cm);
							    }
						    	if(!active_set.isEmpty()) {
							    	corefs.add(active_set.toArray(new Ref[active_set.size()]));
						    	}
						    }
						    Corefs c = new Corefs();
						    c.docId_ = doc.id_;
						    c.ref_ = corefs.toArray(new Ref[corefs.size()][]);
	
							//LemmaEntityCorefs.dumpCorefs(System.out, doc.title_, er, lr, c);
						    
						    DocCoref dc = LemmaEntityCorefs.pack(c);
	
							try {
								hits_to_record.put(dc);
							} catch (InterruptedException e) {
								throw new RuntimeException("failure record hit results", e);
							}
						}
					} catch(SQLException e) {
						throw new RuntimeException("sql error dumping corefs on the fly", e);
					} finally {
						try {
							conn.close();
						} catch (SQLException e) {
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
							"INSERT INTO " + DocCoref.TABLE + "(" + DocCoref.DOC_ID + "," + DocCoref.COREF_LIST + ") " + 
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
						DocCoref hit;
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
						insert.setBytes(2, hit.corefList_);
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
	protected static void handleMention(EntityCache ec, LemmaCache lc, List<CoreMap> sentences, ArrayList<Ref> active_set, ArrayList<PhraseType> active_type, TIntArrayList active_id, CorefMention cm) {
		CoreMap sentence = sentences.get(cm.sentNum - 1);
		List<CoreLabel> ta = sentence.get(TokensAnnotation.class);
	    String last_ner = null;
	    String entity = "";
	    active_type.clear();
	    active_id.clear();
	    
		for(int i = cm.startIndex; i < cm.endIndex; ++i) {
			CoreLabel token = ta.get(i - 1);
    		String word = token.get(TextAnnotation.class);
    		String ne = token.get(NamedEntityTagAnnotation.class);
    		String lemma = token.get(LemmaAnnotation.class);
    		String pos = token.get(PartOfSpeechAnnotation.class);
    		if(last_ner != null && !last_ner.equals("O") && !last_ner.equals(ne)) {
				handleEntity(ec, last_ner, entity, active_type, active_id);
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
    		int lemma_id = lc.getOrAddLemma(lemma, pos);
			active_type.add(LemmaEntityCorefs.PhraseType.Lemma);
			active_id.add(lemma_id);
    	}
    	if(!last_ner.equals("O")) {
			handleEntity(ec, last_ner, entity, active_type, active_id);
    	}
    	if(!active_id.isEmpty()) {
	    	Ref r = new Ref();
	    	r.type_ = active_type.toArray(new LemmaEntityCorefs.PhraseType[active_type.size()]);
	    	r.id_ = active_id.toArray();
	    	active_set.add(r);
    	}
	}
	private static void handleEntity(final EntityCache ec, String ne, String entity, ArrayList<PhraseType> active_type, TIntArrayList active_id) {
		if(ne.equals("O")) {
			//nuttin
		} else if(ne.equals("NUMBER") ||
			ne.equals("DURATION") ||
			ne.equals("DATE") ||
			ne.equals("TIME") ||
			ne.equals("MONEY") ||
			ne.equals("ORDINAL") ||
			ne.equals("MISC") ||
			ne.equals("PERCENT") ||
			ne.equals("SET")) 
		{
			//System.err.println("wanted to be disabled named entity type: " + ne);
		} else if(ne.equals("PERSON") ||
			ne.equals("LOCATION") ||
			ne.equals("ORGANIZATION")) 
		{
			int entity_id = ec.getOrAddEntity(entity, ne);
			active_type.add(LemmaEntityCorefs.PhraseType.Entity);
			active_id.add(entity_id);
		} else {
			System.err.println("unknown named entity type: " + ne);
		}
	}
}
