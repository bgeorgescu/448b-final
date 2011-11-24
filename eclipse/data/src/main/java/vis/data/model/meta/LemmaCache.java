package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawLemma;
import vis.data.util.SQL;

//basically this talks to the db and caches the word to id mapping.
//it will automatically add new words, so no one else should mess with
//this table while the cache is active
public class LemmaCache {
	private static LemmaCache g_instance;
	private ConcurrentHashMap<Pair<String, String>, Integer> mapping_ = new ConcurrentHashMap<Pair<String, String>, Integer>();
	private PreparedStatement insert_;
	private int maxId_ = 0;
	private Connection conn_;
	private LemmaCache(Connection conn){
		conn_ = conn;
		try {
			//load the whole word list
			Statement st = conn_.createStatement();
			ResultSet rs = st.executeQuery("SELECT " + RawLemma.ID + "," + RawLemma.LEMMA + "," + RawLemma.POS + " FROM " + RawLemma.TABLE);
	
			if(rs.first()) do {
				int i = rs.getInt(1);
				String w = rs.getString(2);
				String p = rs.getString(3);
				mapping_.put(Pair.of(w, p), i);
				if(i > maxId_)
					maxId_ = i;
			} while(rs.next());
			st.close();
			
			insert_ = conn_.prepareStatement("INSERT INTO " + RawLemma.TABLE + " (" + RawLemma.ID + "," + RawLemma.LEMMA + "," + RawLemma.POS + ") VALUES (?, ?, ?)");
		} catch(SQLException e) {
			try {
				conn_.close();
			} catch (SQLException e1) {
				throw new RuntimeException("weird close failure", e);
			}
			throw new RuntimeException("failed to prepare  cache", e);
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
	public static synchronized LemmaCache getInstance() {
		if(g_instance != null)
			return g_instance;
		Connection conn = SQL.open();
		try {
			SQL.createTable(conn, RawLemma.class);
		} catch(SQLException e) {
			System.err.println("WARNING rawlemma table already exists!");
		}
		return new LemmaCache(conn);
	}
	public int getLemma(String word, String pos) {
		word = word.toLowerCase();
		Pair<String, String> key = Pair.of(word, pos);
		return mapping_.get(key);
	}
	public int getOrAddLemma(String word, String pos) {
		word = word.toLowerCase();
		try {
			Pair<String, String> key = Pair.of(word, pos);
			Integer i = mapping_.get(key);
			if(i != null)
				return i;
			synchronized(this) {
				i = mapping_.get(key);
				if(i != null)
					return i;
				insert_.setInt(1, ++maxId_);
				insert_.setString(2, word);
				insert_.setString(3, pos);
				insert_.executeUpdate();
				mapping_.put(key, maxId_);
				return maxId_;
				
			}
		} catch(SQLException e) {
			throw new RuntimeException("lemma cache failed to insert word", e);
		}
	}
}
