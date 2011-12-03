package vis.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import vis.data.model.RawEntity;
import vis.data.model.ResolvedEntity;
import vis.data.model.WikiPage;
import vis.data.model.WikiRedirect;
import vis.data.model.meta.EntityAccessor;
import vis.data.model.meta.EntityInsertionCache;
import vis.data.model.meta.IdListAccessor;
import vis.data.model.meta.ResolvedEntityAccessor;
import vis.data.model.meta.WikiRedirectAccessor;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;
import vis.data.util.StringArrayResultSetIterator;

public class WikipediaEntityResolution {
	public static void loadPages() {
		if(SQL.tableExists(WikiPage.TABLE))
			return;
		HttpClient hc = new DefaultHttpClient();
        File f = new File("extra/enwiki-latest-page.sql");
        if(!f.exists()) {
	        try {
	            HttpGet hg = new HttpGet("http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-page.sql.gz");
	            System.out.println("downloading wikipedia dump " + hg.getURI());
	            HttpResponse hr = hc.execute(hg);
	            InputStream in = hr.getEntity().getContent();
	            byte[] buffer = new byte[1024*1024];
	            if(!f.createNewFile())
	            	throw new RuntimeException("failed to open file");
	            OutputStream out = new FileOutputStream(f);
	            GZIPInputStream gzin = new GZIPInputStream(in);
	            for(;;) {
	            	int bytes_read = gzin.read(buffer);
	            	if(bytes_read == -1)
	            		break;
	            	out.write(buffer, 0, bytes_read);
	            }
	            out.close();
	        } catch (ClientProtocolException e) {
	        	throw new RuntimeException("protocol failure downloading", e);
			} catch (IOException e) {
	        	throw new RuntimeException("ioexception failure downloading", e);
			} finally {
	            hc.getConnectionManager().shutdown();
	        }
        }
        SQL.importMysqlDump(f);
	}
	public static void loadRedirects() {
		if(SQL.tableExists(WikiRedirect.TABLE))
			return;
		HttpClient hc = new DefaultHttpClient();
        File f = new File("extra/enwiki-latest-redirect.sql");
        if(!f.exists()) {
	        try {
	            HttpGet hg = new HttpGet("http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-redirect.sql.gz");
	            System.out.println("downloading wikipedia dump " + hg.getURI());
	            HttpResponse hr = hc.execute(hg);
	            InputStream in = hr.getEntity().getContent();
	            byte[] buffer = new byte[1024*1024];
	            if(!f.createNewFile())
	            	throw new RuntimeException("failed to open file");
	            OutputStream out = new FileOutputStream(f);
	            GZIPInputStream gzin = new GZIPInputStream(in);
	            for(;;) {
	            	int bytes_read = gzin.read(buffer);
	            	if(bytes_read == -1)
	            		break;
	            	out.write(buffer, 0, bytes_read);
	            }
	            out.close();
	        } catch (ClientProtocolException e) {
	        	throw new RuntimeException("protocol failure downloading", e);
			} catch (IOException e) {
	        	throw new RuntimeException("ioexception failure downloading", e);
			} finally {
	            hc.getConnectionManager().shutdown();
	        }
	    }
        SQL.importMysqlDump(f);
	}
	public static String clean(String s) {
		return s.toLowerCase()
				.replaceAll("\\([^\\)]+\\)", " ")  // parentheticals
				.replaceAll("_", " ") //underbars
				.replaceAll("\\s+", " "); // whitespace
	}
	public static void resolveEntities() {
		if(SQL.tableExists(ResolvedEntity.TABLE))
			return;
		try {
			SQL.createTable(SQL.forThread(), ResolvedEntity.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create resolved entity table", e);
		}
		final EntityInsertionCache eic = EntityInsertionCache.getInstance();
		Date start = new Date();
		int mr = IdListAccessor.maxRedirect() + 1;
		final Thread processing_threads[] = new Thread[Runtime.getRuntime().availableProcessors()];
		for(int i = 0; i < processing_threads.length; ++i) {
			final int minId = mr * i / (processing_threads.length);
			final int maxId = mr * (i + 1) / (processing_threads.length);
			processing_threads[i] = new Thread() {
				public void run() {
					try {
						WikiRedirectAccessor wra = new WikiRedirectAccessor();
						StringArrayResultSetIterator it = wra.redirectIterator(minId, maxId);
						Connection second = SQL.open(); //need because previous is streaming mode now
						try {
							ResolvedEntityAccessor rea = new ResolvedEntityAccessor(second);
							EntityAccessor ea = new EntityAccessor(second);
							String redirect[];
							while((redirect = it.next()) != null) {
								RawEntity re[] = ea.lookupEntityByName(redirect[0]);
								if(re.length == 0)
									continue;
								for(int i = 0; i < re.length; ++i) {
									int to = eic.getOrAddEntity(redirect[1], re[i].type_);
									rea.setResolution(re[i].id_, to);  // batch?
								}
							}
						} catch(RuntimeException e) {
							if(!e.getMessage().toLowerCase().startsWith("after end of result set")) {
								throw e;
							}
						} finally {
							second.close();
						} 
					} catch (SQLException e) {
						throw new RuntimeException("wiki redirect processing failed", e);
					}
				}
			};
			processing_threads[i].start();
		}
		try {
			//wait until all processing is complete
			for(Thread t : processing_threads)
				t.join();
			Date end = new Date();
			long millis = end.getTime() - start.getTime();
			System.err.println("completed redir processing in " + millis + " milliseconds");
		} catch (InterruptedException e) {
			throw new RuntimeException("unknwon interrupt", e);
		}
	}
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		loadRedirects();
		loadPages();
		resolveEntities();

	}

}
