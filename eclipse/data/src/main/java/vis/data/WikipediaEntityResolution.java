package vis.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import vis.data.util.SQL;

public class WikipediaEntityResolution {
	public static void main(String[] args) {
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

}
