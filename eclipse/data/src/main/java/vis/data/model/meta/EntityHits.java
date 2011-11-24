package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.DocLemma;

public class EntityHits {
	PreparedStatement query_;
	public EntityHits(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + DocLemma.ENTITY_LIST + " FROM " + DocLemma.TABLE + " WHERE doc_id = ?");
	}
	public int[] getEntities(int doc_id) throws SQLException {
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + doc_id);
			
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
	public class EntityCount {
		int docId_;
		int[] entityId_;
		int[] count_;
	}
	public EntityCount getEntityCounts(int doc_id) throws SQLException {
		EntityCount c = new EntityCount();
		c.docId_ = doc_id;
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + doc_id);
			
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
}
