package vis.data.model.query;

import java.sql.SQLException;

import vis.data.model.RawDoc;


//warning, hella slow.  thnx mysql
public class SQLDateTerm extends SQLTerm {
	public static class Parameters extends SQLTerm.Parameters {
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
	}
	
	public final Parameters parameters_;
	public SQLDateTerm(Parameters p) throws SQLException {
		super(buildQuery(p));
		parameters_ = p;
	}

	private static String buildQuery(Parameters p) {
		String filter;
		if(p.before_ != null && p.after_ != null) {
			filter = RawDoc.DATE + " < " + p.before_ + " AND " + RawDoc.DATE + " > " +p.after_;
		} else if(p.before_ != null) {
			filter = RawDoc.DATE + " < " + p.before_;
		} else if(p.after_ != null) {
			filter = RawDoc.DATE + " > " + p.after_;
		} else {
			throw new RuntimeException("date term missing a filter");
		}
		//the order by is critical!
		return "SELECT " + RawDoc.ID + " FROM " + RawDoc.TABLE + " WHERE " + filter + " ORDER BY " + RawDoc.ID;
	}

	@Override
	public Term.Parameters parameters() {
		return parameters_;
	}	
}
