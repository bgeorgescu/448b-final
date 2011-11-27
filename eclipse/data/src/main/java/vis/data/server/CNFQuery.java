package vis.data.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.model.query.LemmaAggregateTerm;
import vis.data.model.query.LemmaFilterTerm;
import vis.data.model.query.Term.Aggregate;
import vis.data.model.query.Term.Filter;
import vis.data.util.SetAggregator;

public class CNFQuery {
	public static class Term {
		public RawLemma lemma_;
		public RawEntity entity_; // not hooked up yet
	}
	public static class Conjunction {
		public Term[][] terms_;
	}
	
	static Filter filterFor(Term t) throws SQLException {
		//TODO: cache terms
		//TODO: other types
		return new LemmaFilterTerm(t.lemma_);
	};
	static Aggregate aggregateFor(Term t) throws SQLException {
		//TODO: cache terms
		//TODO: other types
		return new LemmaAggregateTerm(t.lemma_);
	};
	static ArrayList<Filter>[] filterPipeline(Conjunction conj) throws SQLException {
		ArrayList<Filter> pipeline[] = new ArrayList[conj.terms_.length];
		for(int i = 0; i < conj.terms_.length; ++i) {
			for(int j = 0; j < conj.terms_[i].length; ++j) {
				pipeline[i].add(filterFor(conj.terms_[i][j]));
			}
			//put them in reverse order so that we get the fastest removal of items
			Collections.sort(pipeline[i], new vis.data.model.query.Term.WorkOrder());
		}
		return pipeline;
	}

	
	static Aggregate[][] aggregatePipeline(Conjunction conj) throws SQLException {
		Aggregate pipeline[][] = new Aggregate[conj.terms_.length][];
		for(int i = 0; i < conj.terms_.length; ++i) {
			pipeline[i] = new Aggregate[conj.terms_[i].length];
			for(int j = 0; j < conj.terms_[i].length; ++j) {
				pipeline[i][j] = aggregateFor(conj.terms_[i][j]);
			}
		}
		return pipeline;
	}
	
	
	@Path("/api/filter/docs")
	public static class FilterDocs {
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[] filterDocs(Conjunction conj) throws SQLException {
			ArrayList<Filter>[] pipeline = filterPipeline(conj);

			if(pipeline.length == 0) {
				throw new RuntimeException("no query terms");
			}
			int[] docs = null;
			for(int i = 0; i < pipeline.length; ++i) {
				int[] partial_docs = null;
				for(Filter f : pipeline[i])
					partial_docs = f.filter(partial_docs);	
				
				if(docs == null) {
					docs = partial_docs;
				} else {
					SetAggregator.or(docs, partial_docs);
				}
			}
			return docs;
		}
	}
	public static class Aggregations {
		public Conjunction filter_;
		public Conjunction buckets_[];
	}
	public static class ClauseResults {
		public int docs[][][];
		public int counts[][][];
	}
	@Path("/api/evaluate/clause")
	public static class EvaluateClauses {
		FilterDocs md = new FilterDocs();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public ClauseResults evaluateClauses(Aggregations aggr) throws SQLException {
			//TODO: merge filter terms with the aggregation terms (but flag them for not participating in the counts)
			//first do the filter part
			int base_docs[] = md.filterDocs(aggr.filter_);
			
			class TermRef {
				int bucket_;
				int clause_;
			}
			HashMap<Aggregate, List<TermRef>> plan_elements = new HashMap<Aggregate, List<TermRef>>();
			ArrayList<Aggregate> plan = new ArrayList<>();
			
			Aggregate[][][] buckets = new Aggregate[aggr.buckets_.length][][]; 
			int docs[][][] = new int[buckets.length][][];
			int counts[][][] = new int[buckets.length][][];
			for(int i = 0; i < aggr.buckets_.length; ++i) {
				buckets[i] = aggregatePipeline(aggr.buckets_[i]);
				counts[i] = new int[buckets[i].length][];
				docs[i] = new int[buckets[i].length][];
				for(int j = 0; j < buckets[i].length; ++j) {
					counts[i][j] = new int[base_docs.length];
					docs[i][j] = base_docs;
					for(Aggregate t : buckets[i][j]) {
						List<TermRef> uses = plan_elements.get(t);
						if(uses == null) {
							uses = new ArrayList<>();
							plan.add(t);
							plan_elements.put(t, uses);
						}
						TermRef ref = new TermRef();
						ref.bucket_ = i;
						ref.clause_ = j;
						uses.add(ref);
					}
				}
			}
			
			//sort by difficulty
			Collections.sort(plan, new vis.data.model.query.Term.WorkOrder());
			
			
			//do all the work for each term
			for(Aggregate phase : plan) {
				for(TermRef term : plan_elements.get(phase)) {
					Pair<int[], int[]> result = phase.aggregate(docs[term.bucket_][term.clause_], counts[term.bucket_][term.clause_]);
					docs[term.bucket_][term.clause_] = result.getKey();
					counts[term.bucket_][term.clause_] = result.getValue();
				}
			}
			ClauseResults cr = new ClauseResults();
			cr.docs = docs;
			cr.counts = counts;
			return cr;
		}
	}
	@Path("/api/tally/hits")
	public static class TallyHits {
		EvaluateClauses ec = new EvaluateClauses();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[] tallyHits(Aggregations aggr) throws SQLException {
			ClauseResults cr = ec.evaluateClauses(aggr);
			int buckets = cr.counts.length;
			
			int[] tallies = new int[buckets];
			for(int b = 0; b < buckets; ++b) {
				int hits = 0;
				for(int i = 0; i < cr.counts[b].length; ++i) {
					for(int h : cr.counts[b][i]) {
						hits += h;
					}
				}
				tallies[b] = hits;
			}
			return tallies;
		}
	}
	//TODO: probably can make a version of the clause code that does bother storing the counts
	//  along with merging the two term interface sub types
	@Path("/api/tally/docs")
	public static class TallyDocs {
		EvaluateClauses ec = new EvaluateClauses();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[] tallyDocs(Aggregations aggr) throws SQLException {
			ClauseResults cr = ec.evaluateClauses(aggr);
			int buckets = cr.counts.length;
			
			int[] tallies = new int[buckets];
			for(int b = 0; b < buckets; ++b) {
				int docs[] = cr.docs[b][0];
				for(int i = 1; i < cr.counts[b].length; ++i) {
					docs = SetAggregator.and(docs, cr.docs[b][i]);
				}
				tallies[b] = docs.length;
			}
			return tallies;
		}
	}
}
