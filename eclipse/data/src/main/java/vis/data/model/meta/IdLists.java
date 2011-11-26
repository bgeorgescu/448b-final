package vis.data.model.meta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import vis.data.model.DocCoref;
import vis.data.model.DocLemma;
import vis.data.model.RawDoc;
import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.model.RawWord;

public class IdLists {
	public static int[] all(Connection conn, String table, String field) {
		int[] ids;
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table);
			int doc_count;
			try {
				if(!rs.next())
					throw new RuntimeException("fatal sql mistake");
				doc_count = rs.getInt(1);
			} finally {
				rs.close();
			}
			ids = new int[doc_count];
			rs = st.executeQuery("SELECT " + field + " FROM " + table);
			try {
				int i = 0;
				while(rs.next()) {
					ids[i++] = rs.getInt(1);
				}
			} finally {
				rs.close();
			}
			return ids;
		} catch(Exception e) {
			System.err.println("failed to load list of " + table);
			e.printStackTrace(System.err);
			return new int[0];
		}
	}
	public static int[] allCoreferencedDocuments(Connection conn) {
		return all(conn, DocCoref.TABLE, DocCoref.DOC_ID);
	}
	public static int[] allDocs(Connection conn) {
		return all(conn, RawDoc.TABLE, RawDoc.ID);
	}
	public static int[] allProcessedDocs(Connection conn) {
		return all(conn, DocLemma.TABLE, DocLemma.DOC_ID);
	}
	public static int[] allLemmas(Connection conn) {
		return all(conn, RawLemma.TABLE, RawLemma.ID);
	}
	public static int[] allWords(Connection conn) {
		return all(conn, RawWord.TABLE, RawWord.ID);
	}
	public static int[] allEntities(Connection conn) {
		return all(conn, RawEntity.TABLE, RawEntity.ID);
	}
}
