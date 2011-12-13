package vis.data.model.query;

import vis.data.server.CommonEntities.ReferencedFilteredTyped;
import vis.data.server.CommonLemmas.ReferencedFilteredPosed;

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
			fb.filter_ = null;
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
	public static void mergeAndValidate(ReferencedFilteredPosed rfp) {
		mergeAndValidate((Filtered)rfp);
		//TODO:kill backwards compat
		//this folds the part of speech prefix into the query engine functionality
		//as a backwards compatiility mechanism
		if(rfp.threshold_ != null) {
			for(int i = 0; i < rfp.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.threshold_= new ThresholdTerm.Parameters();
				qe.threshold_.threshold_ = rfp.threshold_;
				qe.threshold_.term_ = rfp.series_[i];
				qe.validate();
				rfp.series_[i] = qe;
			}
		}
		if(rfp.posPrefix_ != null) {
			for(int i = 0; i < rfp.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.and_= new AndTerm.Parameters();
				qe.and_.terms_ = new QueryExpression[2];
				qe.and_.terms_[0] = rfp.series_[i];
				QueryExpression qeFilter = new QueryExpression();
				qeFilter.pos_ = new PosTerm.Parameters();
				qeFilter.pos_.posPrefix_ = rfp.posPrefix_;
				qe.and_.terms_[1] = qeFilter;
				qe.validate();
				rfp.series_[i] = qe;
			}
		}
		if(rfp.maxResults_ != null) {
			for(int i = 0; i < rfp.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.countSort_ = new CountSortTerm.Parameters();
				qe.countSort_.term_ = rfp.series_[i];
				qe.validate();
				rfp.series_[i] = qe;
				qe = new QueryExpression();
				qe.limit_= new LimitTerm.Parameters();
				qe.limit_.limit_ = rfp.maxResults_;
				qe.limit_.term_ = rfp.series_[i];
				qe.validate();
				rfp.series_[i] = qe;
			}
		}
	}
	public static void mergeAndValidate(ReferencedFilteredTyped rft) {
		mergeAndValidate((Filtered)rft);
		//TODO:kill backwards compat
		//this folds the type  into the query engine functionality
		//as a backwards compatiility mechanism
		if(rft.threshold_ != null) {
			for(int i = 0; i < rft.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.threshold_= new ThresholdTerm.Parameters();
				qe.threshold_.threshold_ = rft.threshold_;
				qe.threshold_.term_ = rft.series_[i];
				qe.validate();
				rft.series_[i] = qe;
			}
		}
		if(rft.type_ != null) {
			for(int i = 0; i < rft.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.and_= new AndTerm.Parameters();
				qe.and_.terms_ = new QueryExpression[2];
				qe.and_.terms_[0] = rft.series_[i];
				QueryExpression qeFilter = new QueryExpression();
				qeFilter.type_ = new TypeTerm.Parameters();
				qeFilter.type_.type_ = rft.type_;
				qe.and_.terms_[1] = qeFilter;
				qe.validate();
				rft.series_[i] = qe;
			}
		}
		if(rft.maxResults_ != null) {
			for(int i = 0; i < rft.series_.length; ++i) {
				QueryExpression qe = new QueryExpression();
				qe.countSort_ = new CountSortTerm.Parameters();
				qe.countSort_.term_ = rft.series_[i];
				qe.validate();
				rft.series_[i] = qe;
				qe = new QueryExpression();
				qe.limit_= new LimitTerm.Parameters();
				qe.limit_.limit_ = rft.maxResults_;
				qe.limit_.term_ = rft.series_[i];
				qe.validate();
				rft.series_[i] = qe;
			}
		}
	}
}
