package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import vis.data.model.query.InputExpression.Filtered;
import vis.data.model.query.InputExpression.FilteredBucketed;
import vis.data.model.query.Term.ResultType;

public class DocHits {
	@Path("/api/query/docs/one")
	public static class TallyDocsOne {
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
				hits[i] += r.counts_[i].length;
			}
			return hits;
		}
	}

	@Path("/api/query/docs/bucketed")
	public static class TallyDocsBucketed {
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
					hits[i][j] += r.counts_[i][j].length;
				}
			}
			return hits;
		}
	}
}
