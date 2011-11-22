package vis.data;

import java.io.File;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

import javax.persistence.Column;
import javax.persistence.Table;
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

import vis.data.model.RawDoc;
import vis.data.model.RawHit;
import vis.data.model.RawWord;
import vis.data.util.WordCache;

//take the raw table and process the full_text into words
//insert those words into a rawword table
//write count for the words out in a rawhit table
public class RawHits {	
	public static void main(String[] args) {
		final BlockingQueue<RawDoc> doc_to_process = new ArrayBlockingQueue<RawDoc>(1000);
		//thread to scan for documents to process
		
		final Thread doc_scan_thread = new Thread(new Runnable() {
			public void run() {
				Connection conn = null;

				try
				{
					System.out.println ("Trying to connect to database");
					String userName = "vis";
					String password = "vis";
					String url = "jdbc:mysql://localhost/vis";
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
				try {
					Statement st = null;
					st = conn.createStatement();
					ResultSet rs = st.executeQuery("SELECT " + RawDoc.ID + "," + RawDoc.FULL_TEXT + " FROM " + RawDoc.TABLE);

					if(!rs.first())
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
		});
		doc_scan_thread.start();
		assert(false); //need to make this table
		WordCache wc = new WordCache();
		
		//threads to process individual files
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
		final BlockingQueue<RawHit> hits_to_record = new ArrayBlockingQueue<RawHit>(10000);
		for(int i = 0; i < processing_threads.length; ++i) {
			processing_threads[i] = new Thread(new Runnable() {
				public void run() {
					while(!doc_to_process.isEmpty() || doc_scan_thread.isAlive()) {
						RawDoc doc;
						try {
							doc = doc_to_process.poll(5, TimeUnit.SECONDS);
							//maybe we are out of work
							if(doc == null)
								continue;
						} catch (InterruptedException e) {
							throw new RuntimeException("Unknown interupt while pulling from doc queue", e);
						}
						
						throw new RuntimeException("need to iterate through the words and make an intermediate set of hit entries");

					}
				}
			});
			processing_threads[i].start();
		}
		assert(false); //need to make this table too
		final int BATCH_SIZE = 1000;
		Thread mysql_thread = new Thread(new Runnable() {
			public void run() {
				Connection conn = null;

				try
				{
					System.out.println ("Trying to connect to database");
					String userName = "vis";
					String password = "vis";
					String url = "jdbc:mysql://localhost/vis";
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
				int current_batch_partial = 0;
				int batch = 0;
				try {
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
							hit = hits_to_record.poll(5, TimeUnit.SECONDS);
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
		});
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
