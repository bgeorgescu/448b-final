package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.RawEntity;

public class EntityRaw {
	PreparedStatement query_;
	public EntityRaw(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + RawEntity.ENTITY + "," + RawEntity.TYPE + " FROM " + RawEntity.TABLE + " WHERE " + RawEntity.ID + " = ?");
	}
	public RawEntity getEntity(int entity_id) throws SQLException {
		query_.setInt(1, entity_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find entity_id " + entity_id);
			
			RawEntity re = new RawEntity();
			re.id_ = entity_id;
			re.entity_ = rs.getString(1);
			re.type_ = rs.getString(2);
			return re;
		} finally {
			rs.close();
		}
	}
}
