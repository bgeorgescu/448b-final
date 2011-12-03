package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.ResolvedEntity;
import vis.data.util.SQL;

public class ResolvedEntityAccessor {
	PreparedStatement insert_, query_;
	public ResolvedEntityAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public ResolvedEntityAccessor(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + ResolvedEntity.TO +" FROM " + ResolvedEntity.TABLE + " WHERE " + ResolvedEntity.FROM + " = ?");
		insert_ = conn.prepareStatement("INSERT INTO " + ResolvedEntity.TABLE + " (" + ResolvedEntity.FROM + "," + ResolvedEntity.TO + ") VALUES (?,?) ON DUPLICATE KEY UPDATE " + ResolvedEntity.TO + " = VALUES(" + ResolvedEntity.TO + ")");
	}
	public ResolvedEntity getResolved(int entity_id) throws SQLException {
		query_.setInt(1, entity_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				return null;
			
			ResolvedEntity re = new ResolvedEntity();
			re.from_ = entity_id;
			re.to_ = rs.getInt(1);
			return re;
		} finally {
			rs.close();
		}
	}
	public void setResolution(int from, int to) throws SQLException {
		insert_.setInt(1, from);
		insert_.setInt(2, to);
		insert_.executeUpdate();
	}
}
