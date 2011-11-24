package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.EntityDoc;

public class DocEntityHits {
	PreparedStatement query_;
	public DocEntityHits(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + EntityDoc.DOC_LIST + " FROM " + EntityDoc.TABLE + " WHERE " + EntityDoc.ENTITY_ID + " = ?");
	}
	public int[] getDocs(int entity_id) throws SQLException {
		query_.setInt(1, entity_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find lemma_id " + entity_id);
			
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
		public Counts(){
		}
		public Counts(int entityId){
			entityId_ = entityId;
		}
		public int entityId_;
		public int[] docId_;
		public int[] count_;
	}
	public Counts getDocCounts(int entity_id) throws SQLException {
		Counts c = new Counts();
		c.entityId_ = entity_id;
		query_.setInt(1, entity_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + entity_id);
			
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
	public static EntityDoc pack(Counts c) {
		int num = c.docId_.length;
		ByteBuffer bb = ByteBuffer.allocate(num * 2 * Integer.SIZE / 8);
		for(int i = 0; i < num; ++i) {
			bb.putInt(c.docId_[i]);
			bb.putInt(c.count_[i]);
		}
		EntityDoc ed = new EntityDoc();
		ed.entityId_ = c.entityId_;
		ed.docList_ = bb.array();
		return ed;
	}
}
