package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import vis.data.model.RawWord;
import vis.data.util.SQL;

//basically this talks to the db and caches the word to id mapping.
//it will automatically add new words, so no one else should mess with
//this table while the cache is active
public class WordCache {
	private static WordCache g_instance;
	private ConcurrentHashMap<String, Integer> mapping_ = new ConcurrentHashMap<String, Integer>();
	private PreparedStatement insert_;
	private int maxId_ = 0;
	private Connection conn_;
	private WordCache(Connection conn){
		conn_ = conn;
		try {
			//load the whole word list
			Statement st = conn_.createStatement();
			ResultSet rs = st.executeQuery("SELECT " + RawWord.ID + "," + RawWord.WORD + " FROM " + RawWord.TABLE);
	
			if(rs.first()) do {
				int i = rs.getInt(1);
				String w = rs.getString(2);
				mapping_.put(w, i);
				if(i > maxId_)
					maxId_ = i;
			} while(rs.next());
			st.close();
			
			insert_ = conn_.prepareStatement("INSERT IGNORE INTO " + RawWord.TABLE + " (" + RawWord.ID + "," + RawWord.WORD + ") VALUES (?, ?)");
		} catch(SQLException e) {
			try {
				conn_.close();
			} catch (SQLException e1) {
				throw new RuntimeException("weird close failure", e);
			}
			throw new RuntimeException("failed to prepare word cache", e);
		}
	}
	public void close() {
		try {
			insert_.close();
			conn_.close();
		} catch(SQLException e) {
			throw new RuntimeException("weird close failure", e);
		}
	}
	public static synchronized WordCache getInstance() {
		if(g_instance != null)
			return g_instance;
		Connection conn = SQL.open();
		try {
			SQL.createTable(conn, RawWord.class);
		} catch(SQLException e) {
			System.err.println("WARNING rawword table already exists!");
		}
		return new WordCache(conn);
	}
	public int getWord(String word) {
		word = word.toLowerCase();
		return mapping_.get(word);
	}
	public int getOrAddWord(String word) {
		word = word.toLowerCase();
		try {
			Integer i = mapping_.get(word);
			if(i != null)
				return i;
			synchronized(this) {
				i = mapping_.get(word);
				if(i != null)
					return i;
				insert_.setInt(1, ++maxId_);
				insert_.setString(2, word);
				insert_.executeUpdate();
				mapping_.put(word, maxId_);
				return maxId_;
				
			}
		} catch(SQLException e) {
			throw new RuntimeException("word cache failed to insert word", e);
		}
	}
}
