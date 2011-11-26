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
import vis.data.util.SQL;

public class IdLists {
	public static int[] all(String table, String field) {
		Connection conn = SQL.forThread();
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
			throw new RuntimeException("failed to load list of " + table, e);
		}
	}
	public static int[] allCoreferencedDocuments() {
		return all(DocCoref.TABLE, DocCoref.DOC_ID);
	}
	public static int[] allDocs() {
		return all(RawDoc.TABLE, RawDoc.ID);
	}
	public static int[] allProcessedDocs() {
		return all(DocLemma.TABLE, DocLemma.DOC_ID);
	}
	public static int[] allLemmas() {
		return all(RawLemma.TABLE, RawLemma.ID);
	}
	public static int[] allWords() {
		return all(RawWord.TABLE, RawWord.ID);
	}
	public static int[] allEntities() {
		return all(RawEntity.TABLE, RawEntity.ID);
	}
}
