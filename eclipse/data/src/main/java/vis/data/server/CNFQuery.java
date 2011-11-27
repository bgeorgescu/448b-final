package vis.data.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import vis.data.model.RawLemma;
import vis.data.model.query.LemmaFilterTerm;
import vis.data.model.query.Term.Filter;

public class CNFQuery {
	public static class Term {
		public RawLemma lemma_;
		//public EntityFilterTerm.Parameters entities_;
	}
	public static class Conjunction {
		public Term[] terms_;
	}
	
	
	@Path("/api/query/doc")
	public static class MatchingDocs {
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[] post(Conjunction conj) throws SQLException {
			ArrayList<Filter> filters = new ArrayList<>(conj.terms_.length * 2);
			for(int i = 0; i < conj.terms_.length; ++i) {
				filters.add(new LemmaFilterTerm(conj.terms_[i].lemma_));
			}
			//put them in reverse order so that we get the fastest removal of items
			Collections.sort(filters, new Comparator<Filter>() {
				@Override
				public int compare(Filter o1, Filter o2) {
					if(o1.size() < o2.size())
						return -1;
					if(o1.size() > o2.size())
						return 1;
					return 0;
				}
			});
			if(filters.isEmpty()) {
				throw new RuntimeException("no query terms");
			}
			int[] docs = null;
			for(Filter f : filters)
				docs = f.filter(docs);			
			
			return docs;
		}
	}
}
