package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.query.InputExpression;
import vis.data.model.query.InputExpression.FilteredBucketed;
import vis.data.model.query.Term.ResultType;
import vis.data.util.CountAggregator;

@Path("/api/query/evaluate/bucketed")
public class EvaluateBucketed {
	public static class Results {
		public int items_[/*series*/][/*bucket*/][/*doc*/];
		public int counts_[/*series*/][/*bucket*/][/*doc*/];
		public ResultType type_;
	}
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Results evalueteBucketed(FilteredBucketed f) throws SQLException {
		InputExpression.mergeAndValidate(f);
		Results r = new Results();
		r.items_ = new int[f.series_.length][][];
		r.counts_ = new int[f.series_.length][][];
		r.type_ = f.series_[0].validate().resultType();
		for(int i = 0; i < f.buckets_.length; ++i) {
			if(r.type_ != f.buckets_[i].validate().resultType()) 
				throw new RuntimeException("inconsistent result types");
		}
		for(int i = 0; i < f.series_.length; ++i) {
			if(r.type_ != f.series_[i].validate().resultType()) 
				throw new RuntimeException("inconsistent result types");
			r.items_[i] = new int[f.buckets_.length][];
			r.counts_[i] = new int[f.buckets_.length][];
			for(int j = 0; j < f.buckets_.length; ++j) {
				Pair<int[], int[]> a = f.series_[i].term().result();
				Pair<int[], int[]> b = f.buckets_[j].term().result();
				Pair<int[], int[]> result = CountAggregator.and(a.getKey(), a.getValue(), b.getKey(), b.getValue());
				r.items_[i][j] = result.getKey();
				r.counts_[i][j] = result.getValue();
			}
		}
		return r;
	}
}