package vis.data.server;

import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import vis.data.model.RawSentiment;
import vis.data.model.meta.SentimentAccessor;

public class Sentiments {
	@Path("/api/sentiments")
	public static class All {
		@GET
		@Produces("application/json")
		public List<RawSentiment> listAll() throws SQLException {
			SentimentAccessor sr = new SentimentAccessor();
			return sr.listSentiments();
		}
	}
}
