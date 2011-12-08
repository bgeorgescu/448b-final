package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.query.InputExpression;
import vis.data.model.query.InputExpression.Filtered;
import vis.data.model.query.Term.ResultType;

@Path("/api/query/evaluate/one")
public class EvaluateOne {
	public static class Results {
		public int items_[/*series*/][/*item*/];
		public int counts_[/*series*/][/*item*/];
		public ResultType type_;
	}
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Results evaluateOne(Filtered f) throws SQLException {
		InputExpression.mergeAndValidate(f);
		Results r = new Results();
		r.items_ = new int[f.series_.length][];
		r.counts_ = new int[f.series_.length][];
		r.type_ = f.series_[0].validate().resultType();
		for(int i = 0; i < f.series_.length; ++i) {
			if(r.type_ != f.series_[i].validate().resultType()) 
				throw new RuntimeException("inconsistent result types");
			Pair<int[], int[]> result = f.series_[i].term().result();
			r.items_[i] = result.getKey();
			r.counts_[i] = result.getValue();
		}
		return r;
	}
}
