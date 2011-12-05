package vis.data.model.query;
import java.sql.SQLException;

import vis.data.model.RawDoc;

public class SQLPageTerm extends SQLTerm {
	public static class Parameters {
		public String section_;
		
		@Override
		public int hashCode() {
			int hashCode = section_.hashCode();
			hashCode ^= Parameters.class.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(!section_.equals(p.section_)) {
				return false;
			}
			return true;
		}
	}
	
	public final Parameters parameters_;
	public SQLPageTerm(Parameters p) throws SQLException {
		super(buildQuery(p));
		parameters_ = p;
	}

	private static String buildQuery(Parameters p) {
		//the order by is critical!
		return "SELECT " + RawDoc.ID + " FROM " + RawDoc.TABLE + 
			" WHERE " + RawDoc.PAGE + " LIKE '%" + SQLTerm.clean(p.section_) + "%'" +
			" ORDER BY " + RawDoc.ID;
	}

	@Override
	public Object parameters() {
		return parameters_;
	}	
}
