package vis.data.server;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawLemma;
import vis.data.model.meta.LemmaAccessor;
import vis.data.model.query.InputExpression;
import vis.data.model.query.InputExpression.ReferencedFiltered;
import vis.data.model.query.Term.ResultType;

@Path("/api/query/lemmas")
public class CommonLemmas {
	public static class LemmaCounts {
		public int id_[];
		public int count_[];
		public String lemma_[];
		public String pos_[];
	}
	public static class ReferencedFilteredPosed extends ReferencedFiltered {
		public String posPrefix_;
	}
	EvaluateOne eo = new EvaluateOne();
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public LemmaCounts[] tallyLemmas(ReferencedFilteredPosed rf) throws SQLException {
		if(rf.filter_ != null)
			throw new RuntimeException("filter_ not supported for lemma tally, put it in a series");
		//this will supersede the later processing, sort of... its a little wasteful because evaluateone will do it too
		InputExpression.mergeAndValidate(rf);
		EvaluateOne.Results r = eo.evaluateOne(rf);
		if(r.type_ != ResultType.LEMMA_HITS)
			throw new RuntimeException("query not returning correct type " + r.type_);
		

		LemmaCounts lc[] = new LemmaCounts[rf.series_.length];
		for(int i = 0; i < rf.series_.length; ++i) {
			if(rf.series_[i].validate().resultType() != ResultType.LEMMA_HITS)
				throw new RuntimeException("lemma query not returning correct type " + rf.series_[i].validate().resultType());
			Pair<int[], int[]> result = Pair.of(r.items_[i], r.counts_[i]);
			lc[i] = new LemmaCounts();
			lc[i].id_ = result.getKey();
			lc[i].count_ = result.getValue();
			
			
			if(rf.includeText_) {
				int id[] = lc[i].id_;
				int count[] = lc[i].count_;
				LemmaAccessor lr = new LemmaAccessor();
				TIntObjectHashMap<RawLemma> lemmas = lr.getLemmas(id);
				lc[i].id_ = new int[id.length];
				lc[i].count_ = new int[id.length];
				lc[i].lemma_ = new String[id.length];
				lc[i].pos_ = new String[id.length];
				for(int j = 0; j < id.length; ++j) {
					RawLemma rl = lemmas.get(id[j]);
					lc[i].id_[j] = id[j];
					lc[i].count_[j] = count[j];
					lc[i].lemma_[j] = rl.lemma_;
					lc[i].pos_[j] = rl.pos_;
				}
			}
		}
		return lc;
	}
}
