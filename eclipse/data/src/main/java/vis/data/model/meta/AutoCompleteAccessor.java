package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.AutoComplete;
import vis.data.model.AutoComplete.Type;
import vis.data.util.SQL;

public class AutoCompleteAccessor {
	PreparedStatement insert_, query_, queryLimit_, queryType_, queryTypeLimit_;
	public AutoCompleteAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public AutoCompleteAccessor(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + 
			AutoComplete.TERM + "," + AutoComplete.TYPE + "," + AutoComplete.REFERENCE_ID + "," + AutoComplete.SCORE + 
			" FROM " + AutoComplete.TABLE + 
			" WHERE " + AutoComplete.TERM + " LIKE ?" +
			" ORDER BY " + AutoComplete.TYPE + "," + AutoComplete.SCORE + " DESC");
		queryLimit_ = conn.prepareStatement("SELECT " + 
			AutoComplete.TERM + "," + AutoComplete.TYPE + "," + AutoComplete.REFERENCE_ID +
			" FROM " + AutoComplete.TABLE + 
			" WHERE " + AutoComplete.TERM + " LIKE ?" + 
			" ORDER BY " + AutoComplete.TYPE + "," + AutoComplete.SCORE + " DESC LIMIT ?");
		queryType_ = conn.prepareStatement("SELECT " + 
				AutoComplete.TERM + "," + AutoComplete.TYPE + "," + AutoComplete.REFERENCE_ID + "," + AutoComplete.SCORE + 
				" FROM " + AutoComplete.TABLE + 
				" WHERE " + AutoComplete.TERM + " LIKE ?"  + " AND " + AutoComplete.TYPE + " = ?" +
				" ORDER BY " + AutoComplete.TYPE + "," + AutoComplete.SCORE + " DESC");
		queryTypeLimit_ = conn.prepareStatement("SELECT " + 
			AutoComplete.TERM + "," + AutoComplete.TYPE + "," + AutoComplete.REFERENCE_ID +
			" FROM " + AutoComplete.TABLE + 
			" WHERE " + AutoComplete.TERM + " LIKE ?" + " AND " + AutoComplete.TYPE + " = ?" +
			" ORDER BY " + AutoComplete.TYPE + "," + AutoComplete.SCORE + " DESC LIMIT ?");
		insert_ = conn.prepareStatement("INSERT INTO " + AutoComplete.TABLE + 
			" (" + AutoComplete.TERM + "," + AutoComplete.TYPE + "," + AutoComplete.REFERENCE_ID + "," + AutoComplete.SCORE + ")" +
			" VALUES (?,?,?,?)");
	}
	public AutoComplete[] lookup(String q) throws SQLException {
		query_.setString(1, q);
		return processResults(query_.executeQuery());
	}
	public AutoComplete[] lookup(String q, AutoComplete.Type t) throws SQLException {
		query_.setString(1, q);
		query_.setInt(2, t.ordinal());
		return processResults(query_.executeQuery());
	}
	public AutoComplete[] lookup(String q, int limit) throws SQLException {
		query_.setString(1, q);
		query_.setInt(2, limit);
		return processResults(query_.executeQuery());
	}
	public AutoComplete[] lookup(String q, AutoComplete.Type t, int limit) throws SQLException {
		query_.setString(1, q);
		query_.setInt(2, limit);
		query_.setInt(3, t.ordinal());
		return processResults(query_.executeQuery());
	}
	//trying a new pattern - since the resultset caches the whole thing anyway
	static AutoComplete[] processResults(ResultSet rs) throws SQLException {
		try {
			rs.afterLast();
			int count = rs.getRow();
			AutoComplete res[] = new AutoComplete[count];
			rs.beforeFirst();
			int i = 0;
			while(rs.next()) {
				AutoComplete ac = new AutoComplete();
				ac.term_ = rs.getString(1);
				ac.type_ = AutoComplete.Type.values()[rs.getInt(2)];
				ac.referenceId_ = rs.getInt(3);
				ac.score_ = rs.getInt(4);
				res[++i] = ac;
			}
			return res;
		} finally {
			rs.close();
		}
	}
	public void addAutoComplete(String query, Type type, int ref, int score) throws SQLException {
		insert_.setString(1, query);
		insert_.setInt(2, type.ordinal());
		insert_.setInt(3, ref);
		insert_.setInt(4, score);
		insert_.executeUpdate();
	}
	public void addAutoCompleteBatch(String query, Type type, int ref, int score) throws SQLException {
		insert_.setString(1, query);
		insert_.setInt(2, type.ordinal());
		insert_.setInt(3, ref);
		insert_.setInt(4, score);
		insert_.addBatch();
	}
}
