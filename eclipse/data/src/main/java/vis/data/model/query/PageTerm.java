package vis.data.model.query;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.PageAccessor;

public class PageTerm extends Term {
	public static class Parameters extends SQLTerm.Parameters {
		public Integer begin_;
		public Integer end_;
		@Override
		public int hashCode() {
			int hashCode = Parameters.class.hashCode();
			if(begin_ != null)
				hashCode ^= begin_;
			if(end_ != null)
				hashCode ^= end_;
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(begin_ != null ^ p.begin_ != null) {
				return false;
			}
			if(begin_ != null && !begin_.equals(p.begin_)) {
				return false;
			}
			if(end_ != null ^ p.end_ != null) {
				return false;
			}
			if(end_ != null && !end_.equals(p.end_)) {
				return false;
			}
			return true;
		}
		@Override
		public void validate() {
			if(begin_ == null && end_ == null)
				throw new RuntimeException("missing page limitation");
		}
	}
	
	public final Parameters parameters_;
	public final int docs_[];
	public PageTerm(Parameters p) throws SQLException {
		parameters_ = p;
		PageAccessor pa = new PageAccessor();
		docs_ = pa.getDocsForPageRange(parameters_.begin_, parameters_.end_);
	}
	@Override
	public Term.Parameters parameters() {
		return parameters_;
	}
	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		return Pair.of(docs_, new int[docs_.length]);
	}	
}
