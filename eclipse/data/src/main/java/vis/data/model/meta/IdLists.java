package vis.data.model.meta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import vis.data.model.RawDoc;

public class IdLists {
	public static int[] allDocs(Connection conn) {
		int[] doc_ids;
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + RawDoc.TABLE);
			int doc_count;
			try {
				if(!rs.next())
					throw new RuntimeException("fatal sql mistake");
				doc_count = rs.getInt(1);
			} finally {
				rs.close();
			}
			doc_ids = new int[doc_count];
			rs = st.executeQuery("SELECT " + RawDoc.ID + " FROM " + RawDoc.TABLE);
			try {
				int i = 0;
				while(rs.next()) {
					doc_ids[i++] = rs.getInt(1);
				}
			} finally {
				rs.close();
			}
			return doc_ids;
		} catch(Exception e) {
			throw new RuntimeException("failed to load doc list", e);
		}
	}

}
