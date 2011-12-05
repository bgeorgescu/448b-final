package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.TimeSortedDocCache;

public class DateTerm extends Term {
	public static class Parameters implements Term.Parameters {
		public Integer before_;
		public Integer after_;
		
		@Override
		public int hashCode() {
			int hashCode = 0;
			hashCode ^= Parameters.class.hashCode();
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

		@Override
		public void validate() {
			if(before_ == null && after_ == null)
				throw new RuntimeException("missing at least one restriction in date term");
		}

		@Override
		public ResultType resultType() {
			return ResultType.DOC_HITS;
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
		@Override
		public void setFilterOnly() {
			//always is
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

	public Term.Parameters parameters() {
		return parameters_;
	}	
	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		return Pair.of(docs_, new int[docs_.length]);
	}
}
