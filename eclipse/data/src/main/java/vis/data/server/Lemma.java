package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import vis.data.model.RawLemma;
import vis.data.model.meta.LemmaRaw;

public class Lemma {
	@Path("/api/word/{word}/lemma")
	public static class LemmasForWord {
		@GET
		@Produces("application/json")
		public RawLemma[] get(@PathParam("word") String word) throws SQLException {
			//TODO: actually stem the word	
			LemmaRaw lr = new LemmaRaw();
			return lr.lookupLemmaByWord(word);
		}
	}
	@Path("/api/pos/{pos}/lemma")
	public static class LemmasForPos {
		@GET
		@Produces("application/json")
		public RawLemma[] get(@PathParam("pos") String pos) throws SQLException {
			//TODO: actually stem the word	
			LemmaRaw lr = new LemmaRaw();
			return lr.lookupLemmaByPos(pos);
		}
	}
	@Path("/api/word/{word}/pos/{pos}/lemma")
	public static class LemmasForWordAndPos {
		@GET
		@Produces("application/json")
		public RawLemma get(@PathParam("word") String word, @PathParam("pos") String pos) throws SQLException {
			//TODO: actually stem the word	
			LemmaRaw lr = new LemmaRaw();
			return lr.lookupLemma(word, pos);
		}
	}
}
