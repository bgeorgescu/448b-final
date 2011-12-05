package vis.data.model.meta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import vis.data.model.DocCoref;
import vis.data.model.DocLemma;
import vis.data.model.RawDoc;
import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.model.WikiRedirect;
import vis.data.util.SQL;

public class IdListAccessor {
	public static int[] all(String table, String field, String order) {
		Connection conn = SQL.forThread();
		int[] ids;
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table + order);
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
	public static int max(String table, String field) {
		Connection conn = SQL.forThread();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT MAX(" + field + ") FROM " + table);
			try {
				if(!rs.next())
					throw new RuntimeException("fatal sql mistake");
				return rs.getInt(1);
			} finally {
				rs.close();
			}
		} catch(Exception e) {
			System.err.println("failed to load list of " + table);
			e.printStackTrace(System.err);
			return 0;
		}
	}
	public static int[] allCoreferencedDocuments() {
		return all(DocCoref.TABLE, DocCoref.DOC_ID, "");
	}
	public static int[] allDocs() {
		return all(RawDoc.TABLE, RawDoc.ID, "");
	}
	public static int[] allProcessedDocs() {
		return all(DocLemma.TABLE, DocLemma.DOC_ID, "");
	}
	public static int[] allLemmas() {
		return all(RawLemma.TABLE, RawLemma.ID, "");
	}
	public static int[] allEntities() {
		return all(RawEntity.TABLE, RawEntity.ID, "");
	}
	
	
	public static int[] allCoreferencedDocumentsInOrder() {
		return all(DocCoref.TABLE, DocCoref.DOC_ID, " ORDER BY " + DocCoref.DOC_ID);
	}
	public static int[] allDocsInOrder() {
		return all(RawDoc.TABLE, RawDoc.ID, " ORDER BY " + RawDoc.ID);
	}
	public static int[] allProcessedDocsInOrder() {
		return all(DocLemma.TABLE, DocLemma.DOC_ID, " ORDER BY " + DocLemma.DOC_ID);
	}
	public static int[] allLemmasInOrder() {
		return all(RawLemma.TABLE, RawLemma.ID, " ORDER BY " + RawLemma.ID);
	}
	public static int[] allEntitiesInOrder() {
		return all(RawEntity.TABLE, RawEntity.ID, " ORDER BY " + RawEntity.ID);
	}

	
	public static int maxDocs() {
		return max(RawDoc.TABLE, RawDoc.ID);
	}
	public static int maxLemmas() {
		return max(RawLemma.TABLE, RawLemma.ID);
	}
	public static int maxEntities() {
		return max(RawEntity.TABLE, RawEntity.ID);
	}
	public static int maxRedirect() {
		return max(WikiRedirect.TABLE, WikiRedirect.FROM);
	}

}
