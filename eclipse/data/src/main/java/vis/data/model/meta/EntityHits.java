package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.DocLemma;
import vis.data.util.SQL;

public class EntityHits {
	PreparedStatement query_;
	public EntityHits() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + DocLemma.ENTITY_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " = ?");
	}
	public int[] getEntities(int doc_id) throws SQLException {
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next()) {
				return new int[0];
			}
			
			byte[] data = rs.getBytes(1);
			int[] entity_ids = new int[data.length / (Integer.SIZE / 8) / 2];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < entity_ids.length; ++i) {
				entity_ids[i] = bb.getInt();
				/*int count =*/ bb.getInt();
			}
			return entity_ids;
		} finally {
			rs.close();
		}
	}
	public static class Counts {
		public int docId_;
		public int[] entityId_;
		public int[] count_;
	}
	public Counts getEntityCounts(int doc_id) throws SQLException {
		Counts c = new Counts();
		c.docId_ = doc_id;
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next()) {
				c.count_ = new int[0];
				c.entityId_ = new int[0];
				return c;
			}
			
			byte[] data = rs.getBytes(1);
			int num = data.length / (Integer.SIZE / 8) / 2;
			c.entityId_ = new int[num];
			c.count_ = new int[num];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < num; ++i) {
				c.entityId_[i] = bb.getInt();
				c.count_[i] = bb.getInt();
			}
			return c;
		} finally {
			rs.close();
		}
	}
	public static void pack(DocLemma dl, Counts c) {
		int num = c.entityId_.length;
		ByteBuffer bb = ByteBuffer.allocate(num * 2 * Integer.SIZE / 8);
		for(int i = 0; i < num; ++i) {
			bb.putInt(c.entityId_[i]);
			bb.putInt(c.count_[i]);
		}
		dl.entityList_ = bb.array();
		dl.docId_ = c.docId_;
	}
}
