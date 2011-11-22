package vis.data;

import java.io.File;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
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

//load the raw xml files
public class LoadXML {	
	public static void main(String[] args) {
		if(args.length < 1) {
			throw new RuntimeException("must specify a directory of xml files to scan");
		}
		final File root = new File(args[0]);
		if(!root.isDirectory()) {
			throw new RuntimeException("specified path is not a directory to scan for xml files");
		}
		final BlockingQueue<File> files_to_process = new ArrayBlockingQueue<File>(1000);
		//thread to scan for file to load
		final Thread file_scan_thread = new Thread(new Runnable() {
			public void run() {
				LinkedList<File> directories_to_scan = new LinkedList<File>();
				directories_to_scan.add(root);
				
				while(!directories_to_scan.isEmpty()) {
					File dir = directories_to_scan.poll();
					System.out.println ("Scanning " + dir);
					File[] files = dir.listFiles();
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
		});
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
			processing_threads[i] = new Thread(new Runnable() {
				XPath xp = xpf.newXPath();
				public void run() {
					Map<String, XPathExpression> fields = new TreeMap<String, XPathExpression>();
					XPathExpression para_xp;
					Pattern p = Pattern.compile("\\s+");
					try {
						fields.put("pub_id", xp.compile("/DMLDOC/pmdt/pmid"));
						fields.put("section_raw", xp.compile("/DMDLDOC/docdt/docsec"));
						fields.put("date", xp.compile("/DMLDOC/pcdt/pcdtn"));
						fields.put("doc_id", xp.compile("/DMLDOC/docdt/docid"));
						fields.put("title", xp.compile("/DMLDOC/docdt/doctitle"));
						fields.put("subtitle", xp.compile("/DMLDOC/docdt/docsubt"));
						fields.put("page", xp.compile("/DMLDOC/docdt/docpgn"));
						para_xp = xp.compile("/DMLDOC/txtdt/text/paragraph");
					} catch (XPathExpressionException e) {
						throw new RuntimeException("failed to create xpath expressions", e);
					}
					TreeMap<String, String> data = new TreeMap<String, String>();
	
					while(!files_to_process.isEmpty() || file_scan_thread.isAlive()) {
						File f;
						try {
							f = files_to_process.poll(5, TimeUnit.SECONDS);
							//maybe we are out of work
							if(f == null)
								continue;
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while inserting into file queue", e);
						}
						System.out.println ("Processing " + f);
						Document doc;
						try {
							 doc = db.parse(f);
						} catch(Exception e) {
							System.err.println("Failed to parse: " + f);
							continue;
						}
						
						for(Map.Entry<String, XPathExpression> entry : fields.entrySet()) {
							String sql_field = entry.getKey();
							XPathExpression xp_field = entry.getValue();
							
							try {
								String result =  xp_field.evaluate(doc);
								data.put(sql_field, result);
							} catch (XPathExpressionException e) {
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
							data.put("full_text", text.toString());
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
			});
			processing_threads[i].start();
		}
		final int BATCH_SIZE = 100;
		final String TABLE_NAME = "rawdoc";
		Thread mysql_thread = new Thread(new Runnable() {
			public void run() {
				Connection conn = null;

				try
				{
					System.out.println ("Trying to connect to database");
					String userName = "testuser";
					String password = "testpass";
					String url = "jdbc:mysql://localhost/test";
					Class.forName ("com.mysql.jdbc.Driver").newInstance ();
					conn = DriverManager.getConnection (url, userName, password);
					if(conn == null)
						throw new RuntimeException("unknown sql connection creation returned null");
					System.out.println ("Database connection established");
				}
				catch (Exception e)
				{
					System.err.println ("Cannot connect to database server");
					throw new RuntimeException("Sql connection failed", e);
				}
				int current_batch = 0;
				PreparedStatement insert = null;
				try {
					for(;;) {
						if(documents_to_process.isEmpty()) {
							boolean still_running = false;
							for(int i = 0; i < processing_threads.length; ++i)
								still_running |= processing_threads[i].isAlive();
							if(!still_running)
								break;
						}
						TreeMap<String, String> data;
						try {
							data = documents_to_process.poll(5, TimeUnit.SECONDS);
							//maybe we are out of work
							if(data == null)
								continue;
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while inserting into mysql queue", e);
						}
						//lazily create this so we don't have to have a copy of the field list in multiple places
						if(insert == null) {
							StringBuilder questions = new StringBuilder("?");
							Iterator<String> j = data.keySet().iterator();
							StringBuilder parameters = new StringBuilder(j.next());
							for(int i = 1; i < data.size(); ++i) {
								questions.append(", ?");
								parameters.append(", " + j.next());
							}
							insert = conn.prepareStatement("INSERT INTO " + TABLE_NAME + "VALUES(" + questions + ")");
						}
						
						Iterator<String> j = data.values().iterator();
						int param_count = data.size();
						for(int i = 0; i < param_count; ++i) {
							insert.setString(i, j.next());
						}
						insert.addBatch();

						if(++current_batch == BATCH_SIZE) {
							System.out.println ("Inserting Batch " + current_batch);
							insert.executeBatch();
							current_batch = 0;
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
		});
		mysql_thread.start();
		
		//join on them all
		
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
		
		//wait until all scanning is complete
		try {
			file_scan_thread.join();
			//then wait until all processing is complete
			for(Thread t : processing_threads)
				t.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("unknwon interrupt", e);
		}
	}
}
