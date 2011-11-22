package vis.data;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
					File[] files = dir.listFiles();
					for(File file : files) {
						if(file.isDirectory()) {
							directories_to_scan.add(file);
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
		Thread processing_threads[];
		processing_threads = new Thread[Runtime.getRuntime().availableProcessors()];
		
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
						System.out.println(data.get("title"));
						//TODO: something useful
					}
				}
			});
			processing_threads[i].start();
		}
		
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
