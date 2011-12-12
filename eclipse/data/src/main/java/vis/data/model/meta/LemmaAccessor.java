package vis.data.model.meta;

import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import vis.data.model.LemmaDoc;
import vis.data.model.RawLemma;
import vis.data.util.SQL;
import vis.data.util.SQL.NullForLastRowProcessor;

public class LemmaAccessor {
	PreparedStatement query_, queryByWord_, queryByLemma_, queryByPos_, queryList_, queryListScore_, queryIdsByPos_, queryIdsByPosPrefix_;
	public LemmaAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public LemmaAccessor(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + RawLemma.LEMMA + "," + RawLemma.POS + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.ID + " = ?");
		queryByWord_ = conn.prepareStatement("SELECT " + RawLemma.ID + "," + RawLemma.POS + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.LEMMA + " = ?");
		queryByLemma_ = conn.prepareStatement("SELECT " + RawLemma.ID + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.LEMMA + " = ? AND " + RawLemma.POS + " = ?");
		queryByPos_ = conn.prepareStatement("SELECT " + RawLemma.ID + "," + RawLemma.LEMMA + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.POS + " = ?");
		queryList_ = conn.prepareStatement("SELECT " + RawLemma.ID + "," + RawLemma.LEMMA + "," + RawLemma.POS + " FROM " + RawLemma.TABLE);
		queryList_.setFetchSize(Integer.MIN_VALUE); //streaming
		queryListScore_ = conn.prepareStatement("SELECT " + 
			RawLemma.ID + "," + RawLemma.LEMMA + "," + RawLemma.POS + ", LENGTH(" + LemmaDoc.DOC_LIST + ")/8" + 
			" FROM " + RawLemma.TABLE +
			" JOIN " + LemmaDoc.TABLE + " ON " + RawLemma.ID + "=" + LemmaDoc.LEMMA_ID);
		queryListScore_.setFetchSize(Integer.MIN_VALUE); //streaming
		queryIdsByPos_ = conn.prepareStatement("SELECT " + RawLemma.ID + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.POS + " = ?");
		queryIdsByPosPrefix_ = conn.prepareStatement("SELECT " + RawLemma.ID + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.POS + " LIKE CONCAT(?,'%')");
	}
	public RawLemma getLemma(int lemma_id) throws SQLException {
		query_.setInt(1, lemma_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find lemma_id " + lemma_id);
			
			RawLemma rl = new RawLemma();
			rl.id_ = lemma_id;
			rl.lemma_ = rs.getString(1);
			rl.pos_ = rs.getString(2);
			return rl;
		} finally {
			rs.close();
		}
	}
	public RawLemma[] lookupLemmaByWord(String word) throws SQLException {
		word = word.toLowerCase();
		queryByWord_.setString(1, word);
		ResultSet rs = queryByWord_.executeQuery();
		try {
			if(!rs.next())
				return new RawLemma[0];
			
			ArrayList<RawLemma> hits = new ArrayList<RawLemma>(32);
			do {
				RawLemma rl = new RawLemma();
				rl.id_ =  rs.getInt(1);
				rl.lemma_ = word;
				rl.pos_ = rs.getString(2);
				hits.add(rl);
			} while(rs.next());
			return hits.toArray(new RawLemma[hits.size()]);
		} finally {
			rs.close();
		}
	}
	public RawLemma[] lookupLemmaByPos(String pos) throws SQLException {
		pos = pos.toUpperCase();
		queryByPos_.setString(1, pos);
		ResultSet rs = queryByPos_.executeQuery();
		try {
			if(!rs.next())
				return new RawLemma[0];
			
			ArrayList<RawLemma> hits = new ArrayList<RawLemma>(32);
			do {
				RawLemma rl = new RawLemma();
				rl.id_ =  rs.getInt(1);
				rl.lemma_ = rs.getString(2);
				rl.pos_ = pos;
				hits.add(rl);
			} while(rs.next());
			return hits.toArray(new RawLemma[hits.size()]);
		} finally {
			rs.close();
		}
	}
	public RawLemma lookupLemma(String word, String pos) throws SQLException {
		word = word.toLowerCase();
		pos = pos.toUpperCase();
		queryByLemma_.setString(1, word);
		queryByLemma_.setString(2, pos);
		ResultSet rs = queryByLemma_.executeQuery();
		try {
			if(!rs.next())
				return null;
			
			RawLemma rl = new RawLemma();
			rl.id_ =  rs.getInt(1);
			rl.lemma_ = word;
			rl.pos_ = pos;
			return rl;
		} finally {
			rs.close();
		}
	}
	public static class ResultSetIterator extends org.apache.commons.dbutils.ResultSetIterator {
		public ResultSetIterator(ResultSet rs) {
			super(rs, new NullForLastRowProcessor());
		}

		public RawLemma nextLemma() {
			RawLemma rl = new RawLemma();
			Object fields[] = super.next();
			if(fields == null)
				return null;
			rl.id_ = (Integer)fields[0];
			rl.lemma_ = (String)fields[1];
			rl.pos_ = (String)fields[2];
			return rl;
		}
	}
	public ResultSetIterator lemmaIterator() throws SQLException {
		ResultSet rs = queryList_.executeQuery();
		return new ResultSetIterator(rs);
	}
	public static class ScoredResultSetIterator extends org.apache.commons.dbutils.ResultSetIterator {
		public ScoredResultSetIterator(ResultSet rs) {
			super(rs, new NullForLastRowProcessor());
		}
		
		public ScoredLemma nextLemma() {
			ScoredLemma sl = new ScoredLemma();
			Object fields[] = super.next();
			if(fields == null)
				return null;
			sl.id_ = (Integer)fields[0];
			sl.lemma_ = (String)fields[1];
			sl.pos_ = (String)fields[2];
			sl.score_ = ((BigDecimal)fields[3]).toBigInteger().intValue();
			return sl;
		}
	}
	public static class ScoredLemma extends RawLemma {
		public int score_;
	}
	public ScoredResultSetIterator lemmaIteratorWithScore() throws SQLException {
		ResultSet rs = queryListScore_.executeQuery();
		return new ScoredResultSetIterator(rs);
	}
	public int[] lookupLemmaIdsByPos(String pos) throws SQLException {
		pos = pos.toUpperCase();
		queryIdsByPos_.setString(1, pos);
		ResultSet rs = queryIdsByPos_.executeQuery();
		try {
			TIntLinkedList hits = new TIntLinkedList();
			while(rs.next())
				hits.add(rs.getInt(1));
			
			return hits.toArray();
		} finally {
			rs.close();
		}
	}
	public int[] lookupLemmaIdsByPosPrefix(String posPrefix) throws SQLException {
		posPrefix = posPrefix.toUpperCase();
		queryIdsByPosPrefix_.setString(1, posPrefix);
		ResultSet rs = queryIdsByPosPrefix_.executeQuery();
		try {
			TIntLinkedList hits = new TIntLinkedList();
			while(rs.next())
				hits.add(rs.getInt(1));
			
			return hits.toArray();
		} finally {
			rs.close();
		}
	}
	public TIntObjectHashMap<RawLemma> getLemmas(int[] id) throws SQLException {
		TIntObjectHashMap<RawLemma> res = new TIntObjectHashMap<RawLemma>();
		if(id.length == 0)
			return res;
		StringBuilder sb = new StringBuilder(id.length * 16);
		sb.append("SELECT " + RawLemma.ID + "," + RawLemma.LEMMA + "," + RawLemma.POS + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.ID + " IN ");
		sb.append("(");
		sb.append(id[0]);
		for(int i = 1; i < id.length; ++i) {
			sb.append(",");
			sb.append(id[i]);
		}
		sb.append(")");
		Statement st = SQL.forThread().createStatement();
		ResultSet rs = st.executeQuery(sb.toString());
		try {
			while(rs.next()) {
				RawLemma rl = new RawLemma();
				rl.id_ =  rs.getInt(1);
				rl.lemma_ = rs.getString(2);
				rl.pos_ = rs.getString(3);
				res.put(rl.id_, rl);
			}
	
			return res;
		} finally {
			rs.close();
		}
	}
}
