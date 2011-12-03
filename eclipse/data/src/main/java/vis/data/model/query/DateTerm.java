package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.TimeSortedDocCache;
import vis.data.util.CountAggregator;
import vis.data.util.SetAggregator;

public class DateTerm extends Term {
	public static class Parameters {
		public Integer before_;
		public Integer after_;
		
		@Override
		public int hashCode() {
			int hashCode = 0;
			if(before_ != null)
				hashCode ^= before_.hashCode();
			if(after_ != null)
				hashCode ^= after_.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(before_ != null ^ p.before_ != null) {
				return false;
			}
			if(before_ != null && !before_.equals(p.before_)) {
				return false;
			}
			if(after_ != null ^ p.after_ != null) {
				return false;
			}
			if(after_ != null && !after_.equals(p.after_)) {
				return false;
			}
			return true;
		}
	}
	
	public final Parameters parameters_;
	public final int docs_[];
	public DateTerm(Parameters p) throws SQLException {
		parameters_ = p;
		TimeSortedDocCache tsd = new TimeSortedDocCache();
		if(p.before_ != null && p.after_ != null) {
			docs_ = tsd.getDocsBetween(p.before_, p.after_);
		} else if(p.before_ != null) {
			docs_ = tsd.getDocsBefore(p.before_);
		} else if(p.after_ != null) {
			docs_ = tsd.getDocsAfter(p.after_);
		} else {
			throw new RuntimeException("date term missing a filter");
		}
		//must be in doc id order
		Arrays.sort(docs_);
	}

	public Object parameters() {
		return parameters_;
	}	

	@Override
	public boolean isFilter() {
		return true;
	}

	@Override
	public int size() {
		return docs_.length;
	}

	@Override
	public int[] filter(int[] items) throws SQLException {
		if(items == null)
			return docs_;
		else
			return SetAggregator.and(docs_, items);
	}

	@Override
	public Pair<int[], int[]> filter(int[] in_docs, int[] in_counts)
			throws SQLException {
		if(in_docs == null)
			return Pair.of(docs_, new int[docs_.length]);
		else
			return CountAggregator.filter(in_docs, in_counts, docs_);
	}

	@Override
	public Pair<int[], int[]> aggregate(int[] in_docs, int[] in_counts)
			throws SQLException {
		return filter(in_docs, in_counts);
	}
}
