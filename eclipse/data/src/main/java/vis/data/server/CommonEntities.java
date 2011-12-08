package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.model.meta.EntityAccessor;
import vis.data.model.query.InputExpression.ReferencedFiltered;
import vis.data.model.query.Term.ResultType;
import vis.data.util.CountAggregator;

@Path("/api/query/entities")
public class CommonEntities {
	public static class ReferencedFilteredTyped extends ReferencedFiltered {
		public String type_;
	}
	public static class EntityCounts {
		public int id_[];
		public int count_[];
		public String entity_[];
		public String type_[];
	}

	EvaluateOne eo = new EvaluateOne();
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public EntityCounts[] tallyEntities(ReferencedFilteredTyped rf) throws SQLException {
		if(rf.filter_ != null)
			throw new RuntimeException("filter_ not supported for entity tally, put it in a series");
		EvaluateOne.Results r = eo.evaluateOne(rf);
		if(r.type_ != ResultType.ENTITY_HITS)
			throw new RuntimeException("query not returning correct type " + r.type_);
		

		EntityCounts ec[] = new EntityCounts[rf.series_.length];
		for(int i = 0; i < rf.series_.length; ++i) {
			if(rf.series_[i].validate().resultType() != ResultType.ENTITY_HITS)
				throw new RuntimeException("expression does not produce a entity type");

			Pair<int[], int[]> result = Pair.of(r.items_[i], r.counts_[i]);
			if(rf.threshold_ != null) {
				result = CountAggregator.threshold(result.getKey(), result.getValue(), rf.threshold_);
			}
			CountAggregator.sortByCountDesc(result.getKey(), result.getValue());
			if(rf.maxResults_ != null && rf.type_ == null) {
				result = Pair.of(
					ArrayUtils.subarray(result.getKey(), 0, rf.maxResults_),
					ArrayUtils.subarray(result.getValue(), 0, rf.maxResults_));
			}
			ec[i] = new EntityCounts();
			ec[i].id_ = result.getKey();
			ec[i].count_ = result.getValue();
			
			
			if(rf.includeText_ || rf.type_ != null) {
				int id[] = ec[i].id_;
				int count[] = ec[i].count_;
				EntityAccessor ea = new EntityAccessor();
				//TODO: make this batched
				ec[i].id_ = new int[id.length];
				ec[i].count_ = new int[id.length];
				ec[i].entity_ = new String[id.length];
				ec[i].type_ = new String[id.length];
				int j, k;
				for(j = 0, k = 0; j < id.length; ++j) {
					RawEntity re = ea.getEntity(id[j]);
					if(rf.type_ == null || re.type_.equals(rf.type_)) {
						ec[i].id_[k] = id[j];
						ec[i].count_[k] = count[j];
						ec[i].entity_[k] = re.entity_;
						ec[i].type_[k] = re.type_;
						++k;
					}
					if(rf.maxResults_ != null && k == rf.maxResults_)
						break;
				}
				if(j != k) {
					ec[i].id_ = ArrayUtils.subarray(ec[i].id_, 0, k);
					ec[i].count_ = ArrayUtils.subarray(ec[i].count_, 0, k);
					ec[i].entity_ = ArrayUtils.subarray(ec[i].entity_, 0, k);
					ec[i].type_ = ArrayUtils.subarray(ec[i].type_, 0, k);
				}
			}
		}
		return ec;
	}
}
