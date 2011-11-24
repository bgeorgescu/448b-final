package vis.data.model.meta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import vis.data.model.RawDoc;
import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.model.RawWord;

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
	public static int[] allLemmas(Connection conn) {
		int[] doc_ids;
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + RawLemma.TABLE);
			int doc_count;
			try {
				if(!rs.next())
					throw new RuntimeException("fatal sql mistake");
				doc_count = rs.getInt(1);
			} finally {
				rs.close();
			}
			doc_ids = new int[doc_count];
			rs = st.executeQuery("SELECT " + RawLemma.ID + " FROM " + RawLemma.TABLE);
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
			throw new RuntimeException("failed to load lemma list", e);
		}
	}
	public static int[] allWords(Connection conn) {
		int[] doc_ids;
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + RawWord.TABLE);
			int doc_count;
			try {
				if(!rs.next())
					throw new RuntimeException("fatal sql mistake");
				doc_count = rs.getInt(1);
			} finally {
				rs.close();
			}
			doc_ids = new int[doc_count];
			rs = st.executeQuery("SELECT " + RawWord.ID + " FROM " + RawWord.TABLE);
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
			throw new RuntimeException("failed to load word list", e);
		}
	}
	public static int[] allEntities(Connection conn) {
		int[] doc_ids;
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + RawEntity.TABLE);
			int doc_count;
			try {
				if(!rs.next())
					throw new RuntimeException("fatal sql mistake");
				doc_count = rs.getInt(1);
			} finally {
				rs.close();
			}
			doc_ids = new int[doc_count];
			rs = st.executeQuery("SELECT " + RawEntity.ID + " FROM " + RawEntity.TABLE);
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
			throw new RuntimeException("failed to load entity list", e);
		}
	}
}
