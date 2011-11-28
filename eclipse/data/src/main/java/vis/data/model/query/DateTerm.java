package vis.data.model.query;

import java.sql.SQLException;

import vis.data.model.RawDoc;

public class DateTerm extends SQLTerm {
	public static class Parameters {
		public Integer before_;
		public Integer after_;
	}
	
	@Override
	public boolean isFilter() {
		return false;
	}
	public DateTerm(Parameters p) throws SQLException {
		super(buildQuery(p));
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
}
