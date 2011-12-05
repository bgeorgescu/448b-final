package vis.data.server;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public class Publications {
	@Path("/api/publications")
	public static class All {
		@GET
		@Produces("application/json")
		public Map<Integer, String> listAll() throws SQLException {
			Map<Integer,String> m = new TreeMap<Integer, String>();
			m.put(7556, "Baltimore Sun");
			m.put(7683, "Los Angeles Times");
			m.put(7684, "Chicago Tribune");
			return m;
		}
	}
}
