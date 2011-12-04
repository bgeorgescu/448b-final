package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbutils.DbUtils;

import vis.data.model.AutoCompleteTerm;
import vis.data.util.SQL;

//basically this talks to the db and caches the word to id mapping.
//it will automatically add new words, so no one else should mess with
//this table while the cache is active
public class TermInsertionCache {
	private static TermInsertionCache g_instance;
	private ConcurrentHashMap<String, Integer> mapping_ = new ConcurrentHashMap<String, Integer>();
	private PreparedStatement insert_;
	private int maxId_ = 0;
	//this has ites own connection which they handle synchronization because it does insertion on whatever thread happens to call
	private Connection conn_;
	private TermInsertionCache(Connection conn){
		conn_ = conn;
		try {
			//load the whole word list
			Statement st = conn_.createStatement();
			ResultSet rs = st.executeQuery("SELECT " + AutoCompleteTerm.ID + "," + AutoCompleteTerm.TERM + " FROM " + AutoCompleteTerm.TABLE);
	
			if(rs.first()) do {
				int i = rs.getInt(1);
				String w = rs.getString(2);
				mapping_.put(w, i);
				if(i > maxId_)
					maxId_ = i;
			} while(rs.next());
			st.close();
			
			insert_ = conn_.prepareStatement("INSERT INTO " + AutoCompleteTerm.TABLE + " (" + AutoCompleteTerm.ID + "," + AutoCompleteTerm.TERM + ") VALUES (?, ?)");
		} catch(SQLException e) {
			try {
				conn_.close();
			} catch (SQLException e1) {
				throw new RuntimeException("weird close failure", e);
			}
			throw new RuntimeException("failed to prepare term cache", e);
		}
	}
	public void close() {
		DbUtils.closeQuietly(insert_);
		DbUtils.closeQuietly(conn_);
	}
	public static synchronized TermInsertionCache getInstance() {
		if(g_instance != null)
			return g_instance;
		Connection conn = SQL.open();
		try {
			SQL.createTable(conn, AutoCompleteTerm.class);
		} catch(SQLException e) {
			System.err.println("WARNING Term table already exists!");
		}
		return new TermInsertionCache(conn);
	}
	public int getEntity(String term) {
		term = term.toLowerCase();
		return mapping_.get(term);
	}
	public int getOrAddTerm(String term) {
		term = term.toLowerCase();
		try {
			Integer i = mapping_.get(term);
			if(i != null)
				return i;
			synchronized(this) {
				i = mapping_.get(term);
				if(i != null)
					return i;
				insert_.setInt(1, ++maxId_);
				insert_.setString(2, term);
				insert_.executeUpdate();
				mapping_.put(term, maxId_);
				return maxId_;
				
			}
		} catch(SQLException e) {
			throw new RuntimeException("term cache failed to insert word", e);
		}
	}
}
