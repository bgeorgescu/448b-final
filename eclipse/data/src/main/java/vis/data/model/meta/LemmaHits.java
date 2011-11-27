package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.DocLemma;
import vis.data.util.SQL;

public class LemmaHits {
	PreparedStatement query_;
	public LemmaHits() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + DocLemma.LEMMA_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " = ?");
	}
	public int[] getLemmas(int doc_id) throws SQLException {
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next()) {
				return new int[0];
			}
			
			byte[] data = rs.getBytes(1);
			int[] lemma_ids = new int[data.length / (Integer.SIZE / 8) / 2];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < lemma_ids.length; ++i) {
				lemma_ids[i] = bb.getInt();
				/*int count =*/ bb.getInt();
			}
			return lemma_ids;
		} finally {
			rs.close();
		}
	}
	public static class Counts {
		public int docId_;
		public int[] lemmaId_;
		public int[] count_;
	}
	public Counts getLemmaCounts(int doc_id) throws SQLException {
		Counts c = new Counts();
		c.docId_ = doc_id;
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next()) {
				c.count_ = new int[0];
				c.lemmaId_ = new int[0];
				return c;
			}
			
			byte[] data = rs.getBytes(1);
			int num = data.length / (Integer.SIZE / 8) / 2;
			c.lemmaId_ = new int[num];
			c.count_ = new int[num];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < num; ++i) {
				c.lemmaId_[i] = bb.getInt();
				c.count_[i] = bb.getInt();
			}
			return c;
		} finally {
			rs.close();
		}
	}
	public static void pack(DocLemma dl, Counts c) {
		int num = c.lemmaId_.length;
		ByteBuffer bb = ByteBuffer.allocate(num * 2 * Integer.SIZE / 8);
		for(int i = 0; i < num; ++i) {
			bb.putInt(c.lemmaId_[i]);
			bb.putInt(c.count_[i]);
		}
		dl.lemmaList_ = bb.array();
		dl.docId_ = c.docId_;
	}
}
