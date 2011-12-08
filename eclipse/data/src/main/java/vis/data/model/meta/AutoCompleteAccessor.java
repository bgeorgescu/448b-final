package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import vis.data.model.AutoCompleteEntry;
import vis.data.model.AutoCompleteEntry.Type;
import vis.data.model.AutoCompletePrecomputed;
import vis.data.model.AutoCompleteTerm;
import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.util.SQL;
import vis.data.util.SQL.NullForLastRowProcessor;

public class AutoCompleteAccessor {
	PreparedStatement insert_, insertPrecomputed_, query_, queryRaw_, queryLimit_, queryType_, queryTypeLimit_, queryCountRows_, queryPrecomputed_;
	public AutoCompleteAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public AutoCompleteAccessor(Connection conn) throws SQLException {
		//these need to match the ones defined in the query methods for precomputed results
		String resolved = "CASE " + AutoCompleteEntry.TYPE +
			" WHEN " + AutoCompleteEntry.Type.LEMMA.ordinal() + " THEN (SELECT CONCAT(" + RawLemma.LEMMA + ",'/'," + RawLemma.POS + ") FROM " + RawLemma.TABLE + " WHERE " + RawLemma.ID + "=" + AutoCompleteEntry.REFERENCE_ID + ")" +
		    " WHEN " + AutoCompleteEntry.Type.ENTITY.ordinal() + " THEN (SELECT CONCAT(" + RawEntity.ENTITY + ",'/'," + RawEntity.TYPE + ") FROM " + RawEntity.TABLE + " WHERE " + RawEntity.ID + "=" + AutoCompleteEntry.REFERENCE_ID + ")" +
		    " WHEN " + AutoCompleteEntry.Type.CHILD.ordinal() + " THEN " + AutoCompleteTerm.TERM +
		    " WHEN " + AutoCompleteEntry.Type.PARENT.ordinal() + " THEN " + AutoCompleteTerm.TERM + 
		    " WHEN " + AutoCompleteEntry.Type.SISTER.ordinal() + " THEN " + AutoCompleteTerm.TERM + 
		    " WHEN " + AutoCompleteEntry.Type.SENTIMENT.ordinal() + " THEN " + AutoCompleteTerm.TERM + 
		    " WHEN " + AutoCompleteEntry.Type.PUBLICATION.ordinal() + " THEN " + AutoCompleteTerm.TERM + 
		    " WHEN " + AutoCompleteEntry.Type.SECTION.ordinal() + " THEN " + AutoCompleteTerm.TERM +
		    " WHEN " + AutoCompleteEntry.Type.PAGE.ordinal() + " THEN " + AutoCompleteTerm.TERM +
		    " ELSE CONCAT('unknown type:'," + AutoCompleteEntry.TYPE + ",':'," + AutoCompleteTerm.TERM + ")" +
		    " END";
		String columns = AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.REFERENCE_ID + "," + AutoCompleteEntry.SCORE + "," + resolved;
		String joinTermTable = " JOIN " + AutoCompleteTerm.TABLE + " ON " + AutoCompleteTerm.ID + "=" + AutoCompleteEntry.TERM_ID;
		String orderBy = " ORDER BY " + AutoCompleteEntry.SCORE + " DESC," + AutoCompleteEntry.TYPE;
		query_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + joinTermTable +
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" +
			orderBy);
		queryRaw_ = conn.prepareStatement("SELECT " + 
				AutoCompleteEntry.TERM_ID + "," + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.REFERENCE_ID + "," + AutoCompleteEntry.SCORE +
				" FROM " + AutoCompleteEntry.TABLE + joinTermTable +
				" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" +
				orderBy);
		queryCountRows_ = conn.prepareStatement("SELECT COUNT(*)" +
				" FROM " + AutoCompleteEntry.TABLE + joinTermTable +
				" WHERE " + AutoCompleteTerm.TERM + " LIKE ?");
		queryLimit_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + joinTermTable +
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" + 
			orderBy + " LIMIT ?");
		queryType_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + joinTermTable +
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?"  + " AND " + AutoCompleteEntry.TYPE + " = ?" +
			orderBy);
		queryTypeLimit_ = conn.prepareStatement("SELECT " + columns +
			" FROM " + AutoCompleteEntry.TABLE + joinTermTable + 
			" WHERE " + AutoCompleteTerm.TERM + " LIKE ?" + " AND " + AutoCompleteEntry.TYPE + " = ?" +
			orderBy + " LIMIT ?");
		insert_ = conn.prepareStatement("INSERT INTO " + AutoCompleteEntry.TABLE + 
			" (" + AutoCompleteEntry.TERM_ID + "," + AutoCompleteEntry.TYPE + "," + AutoCompleteEntry.REFERENCE_ID + "," + AutoCompleteEntry.SCORE + ")" +
			" VALUES (?,?,?,?)");
		insertPrecomputed_ = conn.prepareStatement("INSERT INTO " + AutoCompletePrecomputed.TABLE + 
				" (" + AutoCompletePrecomputed.PARTIAL_TERM + "," + AutoCompletePrecomputed.PACKED_COMPLETIONS + "," + AutoCompletePrecomputed.PACKED_PARTIAL_COMPLETIONS + ")" +
				" VALUES (?,?,?)");

		queryPrecomputed_ = conn.prepareStatement("SELECT " + AutoCompletePrecomputed.PACKED_COMPLETIONS + " FROM " + AutoCompletePrecomputed.TABLE + " WHERE " + AutoCompletePrecomputed.PARTIAL_TERM + " = ?");
	
	}
	//this is slow for big result sets and is just used in the precomputation process
	public int countPossibilites(String q) throws SQLException {
		queryCountRows_.setString(1, q + "%");
		ResultSet rs = queryCountRows_.executeQuery();
		try {
			if(!rs.next())
				return 0;
			return rs.getInt(1);
		} finally {
			rs.close();
		}
	}
	//returns null if there was no entry, look up with the slower variant
	public NamedAutoComplete[] lookupPrecomputed(String q) throws SQLException {
		query_.setString(1, q);
		ResultSet rs = queryPrecomputed_.executeQuery();
		try {
			if(!rs.next()) {
				return null;
			}
			byte[] data = rs.getBytes(1);
			ByteBuffer bb = ByteBuffer.wrap(data);
			
			NamedAutoComplete ae[] = new NamedAutoComplete[data.length / (4 * Integer.SIZE / 8)];
			int ids[] = new int[ae.length];
			for(int i = 0; i < ae.length; ++i) {
				ids[i] = bb.getInt();
				ae[i].type_ =   AutoCompleteEntry.Type.values()[bb.getInt()];
				ae[i].referenceId_ = bb.getInt();
				ae[i].score_ = bb.getInt();
			}
			Statement st = SQL.forThread().createStatement();
			try {
				//these need to match the ones defined in the query methods from the constructor
				String query_term = "SELECT " + AutoCompleteTerm.TERM + " FROM " + AutoCompleteTerm.TABLE + " WHERE " + AutoCompleteTerm.ID + " IN (";
				String query_lemma = "SELECT CONCAT(" + RawLemma.LEMMA + ",'/'," + RawLemma.POS + ") FROM " + RawLemma.TABLE + " WHERE " + RawLemma.ID + " IN (";
				String query_entity = "SELECT CONCAT(" + RawEntity.ENTITY + ",'/'," + RawEntity.TYPE + ") FROM " + RawEntity.TABLE + " WHERE " + RawEntity.ID + " IN (";
				
				query_term += "0"; // there will never be one of these
				query_lemma += "0"; // there will never be one of these
				query_entity += "0"; // there will never be one of these
				for(int i = 0; i < ids.length; ++i) {
					switch(ae[i].type_) {
					case LEMMA:
						query_lemma += "," + ids[i];
						break;
					case ENTITY:
						query_entity += "," + ids[i];
						break;
					default:
						query_term += "," + ids[i];
						break;
					}
				}
				ResultSet terms = st.executeQuery(query_term);
				ResultSet lemmas = st.executeQuery(query_lemma);
				ResultSet entities = st.executeQuery(query_entity);
				terms.next();
				lemmas.next();
				entities.next();
				for(int i = 0; i < ids.length; ++i) {
					switch(ae[i].type_) {
					case LEMMA:
						ae[i].resolved_ = lemmas.getString(1);
						break;
					case ENTITY:
						ae[i].resolved_ = entities.getString(1);
						break;
					default:
						ae[i].resolved_ = terms.getString(1);
						break;
					}
				}
				return ae;
			} finally {
				st.close();
			}
		} finally {
			rs.close();
		}
	}
	public NamedAutoComplete[] lookup(String q) throws SQLException {
		query_.setString(1, q + "%");
		return processResults(query_.executeQuery());
	}
	public NamedAutoComplete[] lookup(String q, AutoCompleteEntry.Type t) throws SQLException {
		queryType_.setString(1, q + "%");
		queryType_.setInt(2, t.ordinal());
		return processResults(queryType_.executeQuery());
	}
	public NamedAutoComplete[] lookup(String q, int limit) throws SQLException {
		queryLimit_.setString(1, q + "%");
		queryLimit_.setInt(2, limit);
		return processResults(queryLimit_.executeQuery());
	}
	public NamedAutoComplete[] lookup(String q, AutoCompleteEntry.Type t, int limit) throws SQLException {
		queryTypeLimit_.setString(1, q + "%");
		queryTypeLimit_.setInt(2, t.ordinal());
		queryTypeLimit_.setInt(3, limit);
		return processResults(queryTypeLimit_.executeQuery());
	}
	public static class NamedAutoComplete {
		public AutoCompleteEntry.Type type_;
		public int referenceId_;
		public int score_;
		public String resolved_;
	}
	static NamedAutoComplete[] processResults(ResultSet rs) throws SQLException {
		try {
			List<NamedAutoComplete> res = new LinkedList<NamedAutoComplete>();
			while(rs.next()) {
				NamedAutoComplete ac = new NamedAutoComplete();
				ac.type_ = AutoCompleteEntry.Type.values()[rs.getInt(1)];
				ac.referenceId_ = rs.getInt(2);
				ac.score_ = rs.getInt(3);
				ac.resolved_ = rs.getString(4);
				res.add(ac);
			}
			return res.toArray(new NamedAutoComplete[res.size()]);
		} finally {
			rs.close();
		}
	}
	public void addAutoComplete(int term_id, Type type, int ref, int score) throws SQLException {
		insert_.setInt(1, term_id);
		insert_.setInt(2, type.ordinal());
		insert_.setInt(3, ref);
		insert_.setInt(4, score);
		insert_.executeUpdate();
	}
	public void addAutoCompleteBatch(int term_id, Type type, int ref, int score) throws SQLException {
		insert_.setInt(1, term_id);
		insert_.setInt(2, type.ordinal());
		insert_.setInt(3, ref);
		insert_.setInt(4, score);
		insert_.addBatch();
	}
	static final int PARTIAL_COUNT=1000;
	public void addAutoCompletePrecomputed(String q, Collection<AutoCompleteEntry> unsorted_aces) throws SQLException {
		AutoCompleteEntry[] aces = unsorted_aces.toArray(new AutoCompleteEntry[unsorted_aces.size()]);
		Arrays.sort(aces, new Comparator<AutoCompleteEntry>(){
			@Override
			public int compare(AutoCompleteEntry a, AutoCompleteEntry b) {
				if(a.score_ > b.score_)
					return -1;
				if(a.score_ < b.score_)
					return 1;
				return 0;
			}});
		ByteBuffer bb = ByteBuffer.allocate(aces.length * Integer.SIZE * 4 / 8);
		for(AutoCompleteEntry ae : aces) {
			bb.putInt(ae.termId_);
			bb.putInt(ae.type_.ordinal());
			bb.putInt(ae.referenceId_);
			bb.putInt(ae.score_);
		}
		insertPrecomputed_.setString(1, q);
		insertPrecomputed_.setBytes(2, bb.array());
		if(aces.length > PARTIAL_COUNT)
			insertPrecomputed_.setBytes(3, ArrayUtils.subarray(bb.array(), 0, PARTIAL_COUNT * Integer.SIZE * 4 / 8));
		else 
			insertPrecomputed_.setBytes(3, null);
		insertPrecomputed_.executeUpdate();
	}
	public static class RawResultSetIterator extends org.apache.commons.dbutils.ResultSetIterator {
		public RawResultSetIterator(ResultSet rs) {
			super(rs, new NullForLastRowProcessor());
		}
		
		public AutoCompleteEntry nextRaw() {
			Object fields[] = super.next();
			if(fields == null)
				return null;
			AutoCompleteEntry ac = new AutoCompleteEntry();
			ac.termId_ = (Integer)fields[0];
			ac.type_ = AutoCompleteEntry.Type.values()[(Integer)fields[1]];
			ac.referenceId_ = (Integer)fields[2];
			ac.score_ = (Integer)fields[3];
			return ac;
		}
	}
	//used to build the precomputed cache, use a new conn
	public RawResultSetIterator autoCompleteRawIterator(String q) throws SQLException {
		int fetch_size = query_.getFetchSize();
		try {
			queryRaw_.setFetchSize(Integer.MIN_VALUE);
			queryRaw_.setString(1, q + "%");
			return new RawResultSetIterator(queryRaw_.executeQuery());
		} finally {
			queryRaw_.setFetchSize(fetch_size);
		}
	}

}
