package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import vis.data.model.RequestLogEntry;
import vis.data.util.SQL;

public class RequestLogAccessor {
	PreparedStatement insert_;
	public RequestLogAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public RequestLogAccessor(Connection conn) throws SQLException {
		insert_ = conn.prepareStatement("INSERT INTO " + RequestLogEntry.TABLE + "(" + RequestLogEntry.URI + "," + RequestLogEntry.QUERY+ "," + RequestLogEntry.DURATION + ") VALUES (?,?,?)");
	}
	public void logRequest(long duration, String uri, String json) throws SQLException {
		insert_.setString(1, uri);
		insert_.setString(2, json);
		insert_.setLong(3, duration);
		insert_.executeUpdate();
	}
}
