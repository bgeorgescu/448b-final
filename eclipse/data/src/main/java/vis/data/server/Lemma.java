package vis.data.server;

import java.sql.Connection;
import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import vis.data.model.RawLemma;
import vis.data.model.meta.LemmaRaw;
import vis.data.util.SQL;

public class Lemma {
	@Path("/word/{word}/lemma")
	public static class LemmasForWord {
		@GET
		@Produces("application/json")
		public RawLemma[] get(@PathParam("word") String word){
			//TODO: actually stem the word	
			Connection conn = SQL.open();
			try {
				LemmaRaw lr = new LemmaRaw(conn);
				return lr.lookupLemma(word);
			} catch (SQLException e) {
				throw new RuntimeException("sql fail", e);
			} finally {
				try { conn.close(); } catch (SQLException e) {}
			}
		}
	}
}
