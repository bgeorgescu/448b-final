package vis.data.server;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import vis.data.model.query.InputExpression.Filtered;
import vis.data.model.query.InputExpression.FilteredBucketed;
import vis.data.model.query.Term.ResultType;

public class DocHits {
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
	static class Winner {
		TIntHashSet series = new TIntHashSet();
		int score;
	}
	@Path("/api/query/winnerdocs/one")
	public static class TallyWinnerDocsOne {
		EvaluateOne eo = new EvaluateOne();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[] tallyHits(Filtered f) throws SQLException {
			EvaluateOne.Results r = eo.evaluateOne(f);
			if(r.type_ != ResultType.DOC_HITS)
				throw new RuntimeException("query not returning correct type " + r.type_);
			final int hits[] = new int[f.series_.length];

			TIntObjectHashMap<Winner> winners = new TIntObjectHashMap<Winner>();
			
			for(int i = 0; i < f.series_.length; ++i) {
				for(int j = 0; j < r.counts_[i].length; ++j) {
					Winner w = winners.get(r.items_[i][j]);
					if(w == null) {
						w = new Winner();
						winners.put(r.items_[i][j], w);
						w.series.add(i);
						w.score = r.counts_[i][j];
					} else if(r.counts_[i][j] == w.score) {
						w.series.add(i);
					} else if(r.counts_[i][j] > w.score) {
						w.series.clear();
						w.series.add(i);
						w.score = r.counts_[i][j];
					}
				}
			}
			winners.forEachValue(new TObjectProcedure<Winner>() {
				@Override
				public boolean execute(Winner w) {
					return w.series.forEach(new TIntProcedure() {
						@Override
						public boolean execute(int s) {
							++hits[s];
							return true;
						}
					});
				}
			});
				
			return hits;
		}
	}

	@Path("/api/query/winnerdocs/bucketed")
	public static class TallyWinnerDocsBucketed {
		EvaluateBucketed eb = new EvaluateBucketed();
		@POST
		@Consumes("application/json")
		@Produces("application/json")
		public int[][] tallyHits(FilteredBucketed fb) throws SQLException {
			EvaluateBucketed.Results r = eb.evalueteBucketed(fb);
			if(r.type_ != ResultType.DOC_HITS)
				throw new RuntimeException("query not returning correct type " + r.type_);
			final int hits[][] = new int[fb.series_.length][fb.buckets_.length];
			for(int i = 0; i < fb.series_.length; ++i) {
				hits[i] = new int[fb.buckets_.length];
			}
			for(int j = 0; j < fb.buckets_.length; ++j) {
				TIntObjectHashMap<Winner> winners = new TIntObjectHashMap<Winner>();
				for(int i = 0; i < fb.series_.length; ++i) {
					for(int k = 0; k < r.counts_[i][j].length; ++k) {
						Winner w = winners.get(r.items_[i][j][k]);
						if(w == null) {
							w = new Winner();
							winners.put(r.items_[i][j][k], w);
							w.score = r.counts_[i][j][k];
							w.series.add(i);
						} else if(r.counts_[i][j][k] == w.score) {
							w.series.add(i);
						} else if(r.counts_[i][j][k] > w.score) {
							w.series.clear();
							w.series.add(i);
							w.score = r.counts_[i][j][k];
						}
					}
				}
				final int bucket = j;
				winners.forEachValue(new TObjectProcedure<Winner>() {
					@Override
					public boolean execute(Winner w) {
						return w.series.forEach(new TIntProcedure() {
							@Override
							public boolean execute(int s) {
								++hits[s][bucket];
								return true;
							}
						});
					}
				});
			}
			return hits;
		}
	}
}
