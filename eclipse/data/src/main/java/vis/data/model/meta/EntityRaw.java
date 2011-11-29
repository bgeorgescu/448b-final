package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import vis.data.model.RawEntity;
import vis.data.util.SQL;

public class EntityRaw {
	PreparedStatement query_, queryByEntity_, queryByBoth_, queryByType_;
	public EntityRaw() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + RawEntity.ENTITY + "," + RawEntity.TYPE + " FROM " + RawEntity.TABLE + " WHERE " + RawEntity.ID + " = ?");
		queryByEntity_ = conn.prepareStatement("SELECT " + RawEntity.ID + "," + RawEntity.TYPE + " FROM " + RawEntity.TABLE + " WHERE " + RawEntity.ENTITY + " = ?");
		queryByBoth_ = conn.prepareStatement("SELECT " + RawEntity.ID + " FROM " + RawEntity.TABLE + " WHERE " + RawEntity.ENTITY + " = ? AND " + RawEntity.TYPE + " = ?");
		queryByType_ = conn.prepareStatement("SELECT " + RawEntity.ID + "," + RawEntity.ENTITY + " FROM " + RawEntity.TABLE + " WHERE " + RawEntity.TYPE + " = ?");
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
	public RawEntity[] lookupEntityByName(String entity) throws SQLException {
		entity = entity.toLowerCase();
		queryByEntity_.setString(1, entity);
		ResultSet rs = queryByEntity_.executeQuery();
		try {
			if(!rs.next())
				return new RawEntity[0];
			
			ArrayList<RawEntity> hits = new ArrayList<RawEntity>(32);
			do {
				RawEntity re = new RawEntity();
				re.id_ =  rs.getInt(1);
				re.entity_ = entity;
				re.type_ = rs.getString(2);
				hits.add(re);
			} while(rs.next());
			return hits.toArray(new RawEntity[hits.size()]);
		} finally {
			rs.close();
		}
	}
	public RawEntity lookupEntity(String entity, String type) throws SQLException {
		entity = entity.toLowerCase();
		type = type.toUpperCase();
		queryByBoth_.setString(1, entity);
		queryByBoth_.setString(2, type);
		ResultSet rs = queryByBoth_.executeQuery();
		try {
			if(!rs.next())
				return null;
			
			RawEntity re = new RawEntity();
			re.id_ =  rs.getInt(1);
			re.entity_ = entity;
			re.type_ = type;
			return re;
		} finally {
			rs.close();
		}
	}
	public RawEntity[] lookupEntityByType(String type) throws SQLException {
		type = type.toUpperCase();
		queryByType_.setString(1, type);
		ResultSet rs = queryByType_.executeQuery();
		try {
			if(!rs.next())
				return new RawEntity[0];
			
			ArrayList<RawEntity> hits = new ArrayList<RawEntity>(32);
			do {
				RawEntity re = new RawEntity();
				re.id_ =  rs.getInt(1);
				re.entity_ = rs.getString(2);
				re.type_ = type;
				hits.add(re);
			} while(rs.next());
			return hits.toArray(new RawEntity[hits.size()]);
		} finally {
			rs.close();
		}
	}
}
