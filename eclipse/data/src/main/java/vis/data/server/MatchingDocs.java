package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import vis.data.model.query.InputExpression.Filtered;
import vis.data.model.query.Term.ResultType;

@Path("/api/query/docids")
public class MatchingDocs {
	EvaluateOne eo = new EvaluateOne();
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public int[/*series*/][/*doc*/] filterDocs(Filtered f) throws SQLException {
		EvaluateOne.Results r = eo.evaluateOne(f);
		if(r.type_ != ResultType.DOC_HITS)
			throw new RuntimeException("query not returning correct type " + r.type_);
		return r.items_;
	}
}
