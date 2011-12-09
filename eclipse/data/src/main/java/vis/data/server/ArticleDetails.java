package vis.data.server;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.ArrayUtils;

import vis.data.model.RawDoc;
import vis.data.model.meta.DocAccessor;
import vis.data.model.query.InputExpression.Filtered;
import vis.data.model.query.Term.ResultType;
import vis.data.util.CountAggregator;

//ideally we would be able to return the most relevant N articles here,
//but for now that will be computed based on hits of the query expression
//this doesn't do it bucketed because its for details on demand
//it does accept series
@Path("/api/query/details/one")
public class ArticleDetails {
	public static class FilteredLimited extends Filtered {
		public Integer maxResults_;
	}
	EvaluateOne eo = new EvaluateOne();
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public RawDoc[][] getDetails(FilteredLimited f) throws SQLException {
		int limit = Integer.MAX_VALUE;
		if(f.maxResults_ != null)
			limit = f.maxResults_;
		EvaluateOne.Results r = eo.evaluateOne(f);
		if(r.type_ != ResultType.DOC_HITS)
			throw new RuntimeException("query not returning correct type " + r.type_);
		DocAccessor da = new DocAccessor();
		TIntObjectHashMap<RawDoc> work = new TIntObjectHashMap<RawDoc>();
		for(int i = 0; i < r.items_.length; ++i) {
			CountAggregator.sortByCountDesc(r.items_[i], r.counts_[i]);
			if(limit < r.items_[i].length) {
				r.items_[i] = ArrayUtils.subarray(r.items_[i], 0, limit);
				r.counts_[i] = ArrayUtils.subarray(r.counts_[i], 0, limit);
			}
			for(int j = 0; j < r.items_[i].length; ++j) {
				RawDoc rd = work.get(r.items_[i][j]);
				if(rd == null) {
					rd = da.getDocMeta(r.items_[i][j]);
					work.put(r.items_[i][j], rd);
				}
			}
		}
		RawDoc details[][] = new RawDoc[r.items_.length][];
		for(int i = 0; i < r.items_.length; ++i) {
			details[i] = new RawDoc[r.items_[i].length];
			for(int j = 0; j < r.items_[i].length; ++j) {
				details[i][j] = work.get(r.items_[i][j]);
			}
		}
		return details;
	}
}
