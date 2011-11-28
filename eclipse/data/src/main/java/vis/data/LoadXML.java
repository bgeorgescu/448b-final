package vis.data;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vis.data.model.RawDoc;
import vis.data.model.annotations.DML;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

//load the raw xml files
public class LoadXML {	
	public static void main(String[] args) {
		Date start = new Date();
		ExceptionHandler.terminateOnUncaught();
		if(args.length < 1) {
			throw new RuntimeException("must specify a directory of xml files to scan");
		}
		final File root = new File(args[0]);
		if(!root.isDirectory()) {
			throw new RuntimeException("specified path is not a directory to scan for xml files");
		}
		final BlockingQueue<File> files_to_process = new ArrayBlockingQueue<File>(1000);
		//thread to scan for file to load
		final Thread file_scan_thread = new Thread() {
			public void run() {
				LinkedList<File> directories_to_scan = new LinkedList<File>();
				directories_to_scan.add(root);
				
				while(!directories_to_scan.isEmpty()) {
					File dir = directories_to_scan.poll();
					File[] files = dir.listFiles();
					if(files.length > 50)
						System.out.println ("Scanning " + dir);
					for(File file : files) {
						if(file.isDirectory()) {
							directories_to_scan.addFirst(file);
							continue;
						} 
						String name = file.getName().toLowerCase();
						if(name.endsWith(".xml")) {
							try {
								files_to_process.put(file);
							} catch (InterruptedException e) {
								throw new RuntimeException("Unknown interupt while inserting into file queue", e);
							}
						}
						
					}
				}
			}
		};
		file_scan_thread.start();
		
		
		//threads to process individual files
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];		
		
		final BlockingQueue<TreeMap<String, String>> documents_to_process = new ArrayBlockingQueue<TreeMap<String, String>>(1000);
		final XPathFactory xpf = XPathFactory.newInstance();
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true); // never forget this!
		for(int i = 0; i < processing_threads.length; ++i) {
			DocumentBuilder builder;
			try {
				builder = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new RuntimeException("failed to create document builder", e);
			}
			final DocumentBuilder db = builder;
			processing_threads[i] = new Thread() {
				XPath xp = xpf.newXPath();
				public void run() {
					Map<String, XPathExpression> fields = new TreeMap<String, XPathExpression>();
					XPathExpression para_xp;
					Pattern p = Pattern.compile("\\s+");
					try {
						Field model_fields[] = RawDoc.class.getFields();
						for(Field f : model_fields) {
							DML dml = f.getAnnotation(DML.class);
							if(dml == null)
								continue;
							Column col = f.getAnnotation(Column.class);
							if(col == null)
								continue;
							fields.put(col.name(), xp.compile(dml.xpath()));
						}
						para_xp = xp.compile("/DMLDOC/txtdt/text/paragraph");
					} catch (XPathExpressionException e) {
						throw new RuntimeException("failed to create xpath expressions", e);
					}
					
					String FULL_TEXT_COLUMN;
					try {
						FULL_TEXT_COLUMN = RawDoc.class.getField("fullText_").getAnnotation(Column.class).name();
					} catch (Exception e) {
						throw new RuntimeException("failed to get column name for full text");
					}
					
					while(!files_to_process.isEmpty() || file_scan_thread.isAlive()) {
						File f;
						try {
							f = files_to_process.poll(5, TimeUnit.MILLISECONDS);
							//maybe we are out of work
							if(f == null) {
								//System.out.println("starving parser");
								continue;
							}
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling from file queue", e);
						}
						//System.out.println ("Processing " + f);
						Document doc;
						try {
							 doc = db.parse(f);
						} catch(Exception e) {
							System.err.println("Failed to parse: " + f);
							continue;
						}
						
						TreeMap<String, String> data = new TreeMap<String, String>();
						for(Map.Entry<String, XPathExpression> entry : fields.entrySet()) {
							String sql_field = entry.getKey();
							XPathExpression xp_field = entry.getValue();
							
							try {
								String result =  xp_field.evaluate(doc);
								data.put(sql_field, result);
							} catch (XPathExpressionException e) {
								data.put(sql_field, null);
								System.err.println("Failed to run xpath query on " + f + " " + e.getMessage() + ":");
								e.printStackTrace(System.err);
							}
						}
						
						try {
							NodeList paragraphs = (NodeList)para_xp.evaluate(doc, XPathConstants.NODESET);
							StringBuilder text = new StringBuilder();
							int num_paragraphs = paragraphs.getLength();
							for(int i = 0; i < num_paragraphs; ++i) {
								Node n = paragraphs.item(i);
								String partial = n.getTextContent();
								Matcher m = p.matcher(partial);
								partial = m.replaceAll(" ");
								text.append(partial);
								text.append('\n');
							}
							data.put(FULL_TEXT_COLUMN, text.toString());
						} catch (XPathExpressionException e) {
							System.err.println("Failed to run xpath query on " + f + " " + e.getMessage() + ":");
							e.printStackTrace(System.err);
						}
						try {
							documents_to_process.put(data);
						} catch (InterruptedException e) {
							throw new RuntimeException("Unexpected failure inserting document to upload", e);
						}
					}
				}
			};
			processing_threads[i].start();
		}
		final int BATCH_SIZE = 1000;
		final String TABLE_NAME = RawDoc.class.getAnnotation(Table.class).name();
		Thread mysql_thread = new Thread(){
			public void run() {
				Connection conn = SQL.forThread();
				int current_batch_partial = 0;
				int batch = 0;
				PreparedStatement insert = null;
				try {
					Collection<String> insert_fields = SQL.getNonGenerated(RawDoc.class);
					SQL.createTable(conn, RawDoc.class);

					StringBuilder questions = new StringBuilder("?");
					Iterator<String> j = insert_fields.iterator();
					StringBuilder parameters = new StringBuilder(j.next());
					for(int i = 1; i < insert_fields.size(); ++i) {
						questions.append(", ?");
						parameters.append(", ");
						parameters.append(j.next());
					}
					System.out.println("INSERT INTO " + TABLE_NAME + " (" + parameters + ") VALUES(" + questions + ") ... ");
					//always take the longer full_text
					insert = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (" + parameters + ") VALUES(" + questions + ")" +
					"");
//							" ON DUPLICATE KEY UPDATE " + RawDoc.FULL_TEXT + " = IF(LENGTH(VALUES(" + RawDoc.FULL_TEXT + ")) > LENGTH(" + RawDoc.FULL_TEXT + 
//							"), VALUES(" + RawDoc.FULL_TEXT + "), " + RawDoc.FULL_TEXT + ")");

					
					for(;;) {
						if(documents_to_process.isEmpty()) {
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
						TreeMap<String, String> data;
						try {
							data = documents_to_process.poll(5, TimeUnit.MILLISECONDS);
							//maybe we are out of work
							if(data == null) {
								//System.out.println("starving mysql");
								continue;
							}
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling from document mysql queue", e);
						}
						
						int param_count = data.size();
						if(data.size() != param_count) {
							throw new RuntimeException("missing some parameter");
						}
						Iterator<String> k = data.values().iterator();
						for(int i = 1; i <= param_count; ++i) {
							insert.setString(i, k.next());
						}
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
			file_scan_thread.join();
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
