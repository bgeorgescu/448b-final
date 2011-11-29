package vis.data.server;

import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.query.DateTerm;
import vis.data.model.query.LemmaTerm;
import vis.data.model.query.Term;
import vis.data.util.SetAggregator;

public class CNFQuery {
	public static class QueryTerm {
		public LemmaTerm.Parameters lemma_;
		public DateTerm.Parameters date_;
	}
	public static class Conjunction {
		public QueryTerm[][] terms_;
	}
	
	//this discards stuff too early
	//static Map<Object, Term> g_term_cache = Collections.synchronizedMap(new WeakHashMap<Object, Term>());
	//this "leaks" term parameter blocks
	static Map<Object, SoftReference<Term>> g_term_cache = Collections.synchronizedMap(new HashMap<Object, SoftReference<Term>>());
	static Term getCache(Object param) {
		SoftReference<Term> srt = g_term_cache.get(param);
		if(srt == null)
			return null;
		return srt.get();
	}
	static void putCache(Object param, Term t) {
		g_term_cache.put(param, new SoftReference<Term>(t));
	}
	static Term termFor(QueryTerm t) throws SQLException {
		Term filter = null;
		if(t.lemma_ != null) {
			if(filter != null) throw new RuntimeException("a term can only have one clause");
			filter = getCache(t.lemma_);
			if(filter == null)
				filter = new LemmaTerm(t.lemma_);
			putCache(t.lemma_, filter);
		}
		if(t.date_ != null) {
			if(filter != null) throw new RuntimeException("a term can only have one clause");
			filter = getCache(t.date_);
			if(filter == null)
				filter = new DateTerm(t.date_);
			putCache(t.date_, filter);
		}
		//TODO: other types
		return filter;
	};
	static ArrayList<Term>[] filterPipeline(Conjunction conj) throws SQLException {
		if(conj.terms_ == null || conj.terms_.length == 0) {
			throw new RuntimeException("missing filter terms");
		}
		ArrayList<Term> pipeline[] = new ArrayList[conj.terms_.length];
		for(int i = 0; i < conj.terms_.length; ++i) {
			pipeline[i] = new ArrayList<Term>();
			for(int j = 0; j < conj.terms_[i].length; ++j) {
				pipeline[i].add(termFor(conj.terms_[i][j]));
			}
			//put them in reverse order so that we get the fastest removal of items
			Collections.sort(pipeline[i], new vis.data.model.query.Term.WorkOrder());
		}
		return pipeline;
	}

	
	static Term[][] aggregatePipeline(Conjunction conj) throws SQLException {
		if(conj.terms_ == null || conj.terms_.length == 0) {
			throw new RuntimeException("missing aggregate terms");
		}
		Term pipeline[][] = new Term[conj.terms_.length][];
		for(int i = 0; i < conj.terms_.length; ++i) {
			pipeline[i] = new Term[conj.terms_[i].length];
			for(int j = 0; j < conj.terms_[i].length; ++j) {
				pipeline[i][j] = termFor(conj.terms_[i][j]);
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
			ArrayList<Term>[] pipeline = filterPipeline(conj);

			int[] docs = null;
			for(int i = 0; i < pipeline.length; ++i) {
				int[] partial_docs = null;
				for(Term f : pipeline[i])
					partial_docs = f.filter(partial_docs);	
				
				if(docs == null) {
					docs = partial_docs;
				} else {
					docs = SetAggregator.or(docs, partial_docs);
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
	public static QueryTerm[][] combineExpressions(QueryTerm a[][], QueryTerm b[][]) {
		if(a == null || b == null)
			throw new RuntimeException("an expression was not properly filled in");
		QueryTerm c[][] = new QueryTerm[a.length * b.length][];
		int k = 0;
		for(int i = 0; i < a.length; ++i) {
			for (int j = 0; j < b.length; ++j, ++k) {
				if(a[i] == null || b[j] == null)
					throw new RuntimeException("a term was not properly filled in");
				c[k] = new QueryTerm[a[i].length + b[i].length]; 
				int l = 0;
				for(int m = 0; m < a[i].length; ++m, ++l)
					c[k][l] = a[i][m];
				for(int m = 0; m < b[i].length; ++m, ++l)
					c[k][l] = b[i][m];
			}
		}
		return c;
	}
	@Path("/api/evaluate/clauses")
	public static class EvaluateClauses {
		FilterDocs md = new FilterDocs();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public ClauseResults evaluateClauses(Aggregations aggr) throws SQLException {
			if(aggr.buckets_ == null || aggr.buckets_.length == 0) {
				throw new RuntimeException("missing buckets for aggregation");
			}

			//merge the filter into the aggregations, so all steps can be done in an 'optimal' order
			if(aggr.filter_ != null) {
				for(int i = 0; i < aggr.buckets_.length; ++i) {
					aggr.buckets_[i].terms_ = CNFQuery.combineExpressions(aggr.buckets_[i].terms_, aggr.filter_.terms_);
				}
			}

			class TermRef {
				int bucket_;
				int clause_;
			}
			HashMap<Term, List<TermRef>> plan_elements = new HashMap<Term, List<TermRef>>();
			ArrayList<Term> plan = new ArrayList<Term>();
			
			Term[][][] buckets = new Term[aggr.buckets_.length][][]; 
			int docs[][][] = new int[buckets.length][][];
			int counts[][][] = new int[buckets.length][][];
			for(int i = 0; i < aggr.buckets_.length; ++i) {
				buckets[i] = aggregatePipeline(aggr.buckets_[i]);
				counts[i] = new int[buckets[i].length][];
				docs[i] = new int[buckets[i].length][];
				for(int j = 0; j < buckets[i].length; ++j) {
					for(Term t : buckets[i][j]) {
						List<TermRef> uses = plan_elements.get(t);
						if(uses == null) {
							uses = new ArrayList<TermRef>();
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
			for(Term phase : plan) {
				for(TermRef term : plan_elements.get(phase)) {
					Pair<int[], int[]> result;
					if(phase.isFilter()) {
						result = phase.filter(docs[term.bucket_][term.clause_], counts[term.bucket_][term.clause_]);
					} else {
						result = phase.aggregate(docs[term.bucket_][term.clause_], counts[term.bucket_][term.clause_]);
					}
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
					docs = SetAggregator.or(docs, cr.docs[b][i]);
				}
				tallies[b] = docs.length;
			}
			return tallies;
		}
	}
}
