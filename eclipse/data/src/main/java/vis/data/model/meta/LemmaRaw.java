package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import vis.data.model.RawLemma;
import vis.data.util.SQL;

public class LemmaRaw {
	PreparedStatement query_, queryByWord_, queryByLemma_, queryByPos_;
	public LemmaRaw() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + RawLemma.LEMMA + "," + RawLemma.POS + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.ID + " = ?");
		queryByWord_ = conn.prepareStatement("SELECT " + RawLemma.ID + "," + RawLemma.POS + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.LEMMA + " = ?");
		queryByLemma_ = conn.prepareStatement("SELECT " + RawLemma.ID + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.LEMMA + " = ? AND " + RawLemma.POS + " = ?");
		queryByPos_ = conn.prepareStatement("SELECT " + RawLemma.ID + "," + RawLemma.LEMMA + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.POS + " = ?");
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
}
