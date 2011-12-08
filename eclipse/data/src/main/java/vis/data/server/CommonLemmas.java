package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawLemma;
import vis.data.model.meta.LemmaAccessor;
import vis.data.model.query.InputExpression.ReferencedFiltered;
import vis.data.model.query.Term.ResultType;
import vis.data.util.CountAggregator;

@Path("/api/query/lemmas")
public class CommonLemmas {
	public static class LemmaCounts {
		public int id_[];
		public int count_[];
		public String lemma_[];
		public String posPrefix_[];
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
		EvaluateOne.Results r = eo.evaluateOne(rf);
		if(r.type_ != ResultType.LEMMA_HITS)
			throw new RuntimeException("query not returning correct type " + r.type_);
		

		LemmaCounts lc[] = new LemmaCounts[rf.series_.length];
		for(int i = 0; i < rf.series_.length; ++i) {
			Pair<int[], int[]> result = Pair.of(r.items_[i], r.counts_[i]);
			if(rf.threshold_ != null) {
				result = CountAggregator.threshold(result.getKey(), result.getValue(), rf.threshold_);
			}
			CountAggregator.sortByCountDesc(result.getKey(), result.getValue());
			if(rf.maxResults_ != null && rf.posPrefix_ == null) {
				result = Pair.of(
					ArrayUtils.subarray(result.getKey(), 0, rf.maxResults_),
					ArrayUtils.subarray(result.getValue(), 0, rf.maxResults_));
			}
			lc[i] = new LemmaCounts();
			lc[i].id_ = result.getKey();
			lc[i].count_ = result.getValue();
			
			
			if(rf.includeText_ || rf.posPrefix_ != null) {
				int id[] = lc[i].id_;
				int count[] = lc[i].count_;
				LemmaAccessor lr = new LemmaAccessor();
				//TODO: make this batched
				lc[i].id_ = new int[id.length];
				lc[i].count_ = new int[id.length];
				lc[i].lemma_ = new String[id.length];
				lc[i].posPrefix_ = new String[id.length];
				int j, k;
				for(j = 0, k = 0; j < id.length; ++j) {
					RawLemma rl = lr.getLemma(id[j]);
					if(rf.posPrefix_ == null || rl.pos_.startsWith(rf.posPrefix_)) {
						lc[i].id_[k] = id[j];
						lc[i].count_[k] = count[j];
						lc[i].lemma_[k] = rl.lemma_;
						lc[i].posPrefix_[k] = rl.pos_;
						++k;
					}
					if(rf.maxResults_ != null && k == rf.maxResults_)
						break;
				}
				if(j != k) {
					lc[i].id_ = ArrayUtils.subarray(lc[i].id_, 0, k);
					lc[i].count_ = ArrayUtils.subarray(lc[i].count_, 0, k);
					lc[i].lemma_ = ArrayUtils.subarray(lc[i].lemma_, 0, k);
					lc[i].posPrefix_ = ArrayUtils.subarray(lc[i].posPrefix_, 0, k);
				}
			}
		}
		return lc;
	}
}
