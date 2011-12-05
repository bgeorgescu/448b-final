package vis.data.model.query;
import java.sql.SQLException;

import vis.data.model.RawDoc;

public class SQLPublicationTerm extends SQLTerm {
	public static class Parameters {
		public int publication_;
		
		@Override
		public int hashCode() {
			int hashCode = new Integer(publication_).hashCode();
			hashCode ^= Parameters.class.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(publication_ != p.publication_) {
				return false;
			}
			return true;
		}
	}
	
	public final Parameters parameters_;
	public SQLPublicationTerm(Parameters p) throws SQLException {
		super(buildQuery(p));
		parameters_ = p;
	}

	private static String buildQuery(Parameters p) {
		//the order by is critical!
		return "SELECT " + RawDoc.ID + " FROM " + RawDoc.TABLE + 
			" WHERE " + RawDoc.PUB_ID + "=" + p.publication_ +
			" ORDER BY " + RawDoc.ID;
	}

	@Override
	public Object parameters() {
		return parameters_;
	}	
}
