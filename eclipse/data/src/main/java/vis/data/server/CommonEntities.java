package vis.data.server;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.model.meta.EntityAccessor;
import vis.data.model.query.InputExpression;
import vis.data.model.query.InputExpression.ReferencedFiltered;
import vis.data.model.query.Term.ResultType;

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
		//this will supersede the later processing, sort of... its a little wasteful because evaluateone will do it too
		InputExpression.mergeAndValidate(rf);
		EvaluateOne.Results r = eo.evaluateOne(rf);
		if(r.type_ != ResultType.ENTITY_HITS)
			throw new RuntimeException("query not returning correct type " + r.type_);
		

		EntityCounts ec[] = new EntityCounts[rf.series_.length];
		for(int i = 0; i < rf.series_.length; ++i) {
			if(rf.series_[i].validate().resultType() != ResultType.ENTITY_HITS)
				throw new RuntimeException("entity query not returning correct type " + rf.series_[i].validate().resultType());
			Pair<int[], int[]> result = Pair.of(r.items_[i], r.counts_[i]);
			ec[i] = new EntityCounts();
			ec[i].id_ = result.getKey();
			ec[i].count_ = result.getValue();
			
			
			if(rf.includeText_) {
				int id[] = ec[i].id_;
				int count[] = ec[i].count_;
				EntityAccessor ea = new EntityAccessor();
				TIntObjectHashMap<RawEntity> entities = ea.getEntities(id);
				ec[i].id_ = new int[id.length];
				ec[i].count_ = new int[id.length];
				ec[i].entity_ = new String[id.length];
				ec[i].type_ = new String[id.length];
				for(int j = 0; j < id.length; ++j) {
					RawEntity re = entities.get(id[j]);
					ec[i].id_[j] = id[j];
					ec[i].count_[j] = count[j];
					ec[i].entity_[j] = re.entity_;
					ec[i].type_[j] = re.type_;
				}
			}
		}
		return ec;
	}
}
