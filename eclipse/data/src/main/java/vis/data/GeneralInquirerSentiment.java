package vis.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import vis.data.model.RawSentiment;
import vis.data.model.RawSentimentWord;
import vis.data.model.meta.SentimentAccessor;
import vis.data.util.SQL;

public class GeneralInquirerSentiment {
	public static void main(String[] args) {
		File dir = new File("extra/sentiments/");
		if(!dir.exists())
			throw new RuntimeException("sentiment list folder not found");
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if(name.toLowerCase().endsWith(".txt"))
					return true;
				return false;
			}
		});
		if(files == null || files.length == 0)
			throw new RuntimeException("no sentiments found");
		
		Connection conn = SQL.forThread();
		try {
			PreparedStatement pss = conn.prepareStatement("INSERT INTO " + RawSentiment.TABLE + " (" + RawSentiment.SENTIMENT + ") VALUES (?)");
			//there are duplicate words in the original processed data
			PreparedStatement pssw = conn.prepareStatement("INSERT IGNORE INTO " + RawSentimentWord.TABLE + 
					" (" + RawSentimentWord.SENTIMENT_ID + "," + RawSentimentWord.WORD + ") VALUES (?,?)");
			SQL.createTable(conn, RawSentimentWord.class);
			SQL.createTable(conn, RawSentiment.class);
			
			SentimentAccessor sr = new SentimentAccessor();
			for(File f : files) {
				String sentiment = f.getName();
				String bits[] = sentiment.split("\\.");
				if(bits == null || bits.length == 0) {
					throw new RuntimeException("files in sentiment directory don't have nice names");
				}
				sentiment = bits[0].toLowerCase();
				pss.setString(1, sentiment);
				pss.executeUpdate();
				RawSentiment rs = sr.lookupSentiment(sentiment);
				List<String> lines = IOUtils.readLines(new FileInputStream(f));
				for(String s : lines) {
					String tokens[] = s.split("\\s+");
					String word = tokens[0].toLowerCase();
					pssw.setInt(1, rs.id_);
					pssw.setString(2, word);
					pssw.executeUpdate();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("error loading sentiments from disk", e);
		} finally {
			try { conn.close(); } catch (SQLException e) {}
		}
	}
}
