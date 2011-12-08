package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import vis.data.model.query.InputExpression.Filtered;
import vis.data.model.query.InputExpression.FilteredBucketed;
import vis.data.model.query.Term.ResultType;

public class TermHits {
	@Path("/api/query/hits/one")
	public static class TallyHitsOne {
		EvaluateOne eo = new EvaluateOne();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[] tallyHits(Filtered f) throws SQLException {
			EvaluateOne.Results r = eo.evaluateOne(f);
			if(r.type_ != ResultType.DOC_HITS)
				throw new RuntimeException("query not returning correct type " + r.type_);
			int hits[] = new int[f.series_.length];
			for(int i = 0; i < f.series_.length; ++i) {
				for(int k : r.counts_[i])
					hits[i] += k;
			}
			return hits;
		}
	}

	@Path("/api/query/hits/bucketed")
	public static class TallyHitsBucketed {
		EvaluateBucketed eb = new EvaluateBucketed();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[][] tallyHits(FilteredBucketed fb) throws SQLException {
			EvaluateBucketed.Results r = eb.evalueteBucketed(fb);
			if(r.type_ != ResultType.DOC_HITS)
				throw new RuntimeException("query not returning correct type " + r.type_);
			int hits[][] = new int[fb.series_.length][fb.buckets_.length];
			for(int i = 0; i < fb.series_.length; ++i) {
				hits[i] = new int[fb.buckets_.length];
				for(int j = 0; j < fb.buckets_.length; ++j) {
					for(int k : r.counts_[i][j])
						hits[i][j] += k;
				}
			}
			return hits;
		}
	}
}
