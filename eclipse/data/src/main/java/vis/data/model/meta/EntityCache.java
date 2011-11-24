package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.util.SQL;

//basically this talks to the db and caches the word to id mapping.
//it will automatically add new words, so no one else should mess with
//this table while the cache is active
public class EntityCache {
	private static EntityCache g_instance;
	private ConcurrentHashMap<Pair<String, String>, Integer> mapping_ = new ConcurrentHashMap<Pair<String, String>, Integer>();
	private PreparedStatement insert_;
	private int maxId_ = 0;
	private Connection conn_;
	private EntityCache(Connection conn){
		conn_ = conn;
		try {
			//load the whole word list
			Statement st = conn_.createStatement();
			ResultSet rs = st.executeQuery("SELECT " + RawEntity.ID + "," + RawEntity.ENTITY + ", " + RawEntity.TYPE + " FROM " + RawEntity.TABLE);
	
			if(rs.first()) do {
				int i = rs.getInt(1);
				String w = rs.getString(2);
				String t = rs.getString(3);
				mapping_.put(Pair.of(w, t), i);
				if(i > maxId_)
					maxId_ = i;
			} while(rs.next());
			st.close();
			
			insert_ = conn_.prepareStatement("INSERT IGNORE INTO " + RawEntity.TABLE + " (" + RawEntity.ID + "," + RawEntity.ENTITY + "," + RawEntity.TYPE + ") VALUES (?, ?, ?)");
		} catch(SQLException e) {
			try {
				conn_.close();
			} catch (SQLException e1) {
				throw new RuntimeException("weird close failure", e);
			}
			throw new RuntimeException("failed to prepare entity cache", e);
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
	public static synchronized EntityCache getInstance() {
		if(g_instance != null)
			return g_instance;
		Connection conn = SQL.open();
		try {
			SQL.createTable(conn, RawEntity.class);
		} catch(SQLException e) {
			System.err.println("WARNING RawEntity table already exists!");
		}
		return new EntityCache(conn);
	}
	public int getEntity(String entity, String type) {
		entity = entity.toLowerCase();
		Pair<String, String> key = Pair.of(entity, type);
		return mapping_.get(key);
	}
	public int getOrAddEntity(String entity, String type) {
		entity = entity.toLowerCase();
		try {
			Pair<String, String> key = Pair.of(entity, type);
			Integer i = mapping_.get(key);
			if(i != null)
				return i;
			synchronized(this) {
				i = mapping_.get(key);
				if(i != null)
					return i;
				insert_.setInt(1, ++maxId_);
				insert_.setString(2, entity);
				insert_.setString(3, type);
				insert_.executeUpdate();
				mapping_.put(key, maxId_);
				return maxId_;
				
			}
		} catch(SQLException e) {
			throw new RuntimeException("entity cache failed to insert word", e);
		}
	}
}
