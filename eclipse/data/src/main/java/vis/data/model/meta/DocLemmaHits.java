package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.LemmaDoc;

public class DocLemmaHits {
	PreparedStatement query_;
	public DocLemmaHits(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + LemmaDoc.DOC_LIST + " FROM " + LemmaDoc.TABLE + " WHERE " + LemmaDoc.LEMMA_ID + " = ?");
	}
	public int[] getDocs(int lemma_id) throws SQLException {
		query_.setInt(1, lemma_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find lemma_id " + lemma_id);
			
			byte[] data = rs.getBytes(1);
			int[] doc_ids = new int[data.length / (Integer.SIZE / 8) / 2];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < doc_ids.length; ++i) {
				doc_ids[i] = bb.getInt();
				/*int count =*/ bb.getInt();
			}
			return doc_ids;
		} finally {
			rs.close();
		}
	}
	public static class Counts {
		public int lemmaId_;
		public int[] docId_;
		public int[] count_;
	}
	public Counts getDocCounts(int lemma_id) throws SQLException {
		Counts c = new Counts();
		c.lemmaId_ = lemma_id;
		query_.setInt(1, lemma_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + lemma_id);
			
			byte[] data = rs.getBytes(1);
			int num = data.length / (Integer.SIZE / 8) / 2;
			c.docId_ = new int[num];
			c.count_ = new int[num];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < num; ++i) {
				c.docId_[i] = bb.getInt();
				c.count_[i] = bb.getInt();
			}
			return c;
		} finally {
			rs.close();
		}
	}
	public static LemmaDoc pack(Counts c) {
		int num = c.docId_.length;
		ByteBuffer bb = ByteBuffer.allocate(num * 2 * Integer.SIZE / 8);
		for(int i = 0; i < num; ++i) {
			bb.putInt(c.docId_[i]);
			bb.putInt(c.count_[i]);
		}
		LemmaDoc ld = new LemmaDoc();
		ld.lemmaId_ = c.lemmaId_;
		ld.docList_ = bb.array();
		return ld;
	}
}
