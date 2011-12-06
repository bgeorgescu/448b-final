package vis.data.server;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.model.meta.EntityAccessor;
import vis.data.model.meta.LemmaAccessor;
import vis.data.model.query.AndTerm;
import vis.data.model.query.QueryExpression;
import vis.data.model.query.Term;
import vis.data.model.query.Term.ResultType;
import vis.data.util.CountAggregator;

public class MultiSeriesQuery {

	public static class Filtered {
		//restrict to a set of documents
		public QueryExpression filter_;
		//compute these values
		public QueryExpression series_[];
	}
	public static class FilteredBucketed extends Filtered{
		//but sliced into these divisions
		public QueryExpression buckets_[];
	}
		
	static void mergeAndValidate(Filtered fb) {
		if(fb.filter_ != null) {
			Term.Parameters parameters = fb.filter_.validate();
			for(Term.Parameters p : parameters.withChildren()) {
				p.setFilterOnly();
			}
		}
		if(fb.series_ == null || fb.series_.length < 1) {
			throw new RuntimeException("at least one series is required if there is no filter");
		}
		//validate caches, so you have to do it for these guys first
		//just in case they end up under an and term because of filter merging
		//(the filter will already have been validated
		for(QueryExpression qe : fb.series_) {
			qe.validate();
		}
		//merge it
		if(fb.filter_ != null) {
			for(int i = 0; i < fb.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.and_= new AndTerm.Parameters();
				qe.and_.terms_ = new QueryExpression[2];
				qe.and_.terms_[0] = fb.filter_;
				qe.and_.terms_[1] = fb.series_[i];
				qe.validate();
				fb.series_[i] = qe;
			}
		}
	}
	static void mergeAndValidate(FilteredBucketed fb) {
		mergeAndValidate((Filtered)fb);
		if(fb.buckets_ == null) {
			throw new RuntimeException("bucketed query must have buckets, yo!");
		}
		for(QueryExpression qe : fb.buckets_) 
			qe.validate();
	}
	
	@Path("/api/query/evaluate/one")
	public static class EvaluateOne {
		public static class Results {
			public int items_[/*series*/][/*item*/];
			public int counts_[/*series*/][/*item*/];
			public ResultType type_;
		}
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public Results evaluateOne(Filtered f) throws SQLException {
			mergeAndValidate(f);
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
	
	@Path("/api/query/evaluate/bucketed")
	public static class EvaluateBucketed {
		public static class Results {
			public int items_[/*series*/][/*bucket*/][/*doc*/];
			public int counts_[/*series*/][/*bucket*/][/*doc*/];
			public ResultType type_;
		}
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public Results evalueteBucketed(FilteredBucketed f) throws SQLException {
			mergeAndValidate(f);
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

	@Path("/api/query/docids")
	public static class FilterDocs {
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
	
	public static class LemmaCounts {
		public int id_[];
		public int count_[];
		public String lemma_[];
		public String posPrefix_[];
	}
	public static class ReferencedFiltered extends Filtered {
		public boolean includeText_;
		public Integer threshold_;
		public Integer maxResults_;
	}
	public static class ReferencedFilteredPosed extends ReferencedFiltered {
		public String posPrefix_;
	}
	@Path("/api/query/lemmas")
	public static class TallyLemmas {
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
	public static class ReferencedFilteredTyped extends ReferencedFiltered {
		public String type_;
	}
	public static class EntityCounts {
		public int id_[];
		public int count_[];
		public String entity_[];
		public String type_[];
	}
	@Path("/api/query/entities")
	public static class TallyEntities {
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
