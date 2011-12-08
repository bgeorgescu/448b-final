package vis.data.model.query;

public class InputExpression {
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
	public static class ReferencedFiltered extends Filtered {
		public boolean includeText_;
		public Integer threshold_;
		public Integer maxResults_;
	}
		
	public static void mergeAndValidate(Filtered fb) {
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
	public static void mergeAndValidate(FilteredBucketed fb) {
		mergeAndValidate((Filtered)fb);
		if(fb.buckets_ == null) {
			throw new RuntimeException("bucketed query must have buckets, yo!");
		}
		for(QueryExpression qe : fb.buckets_) 
			qe.validate();
	}
}
