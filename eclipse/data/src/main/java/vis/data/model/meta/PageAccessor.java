package vis.data.model.meta;

import gnu.trove.list.linked.TIntLinkedList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import vis.data.model.ResolvedPage;
import vis.data.util.SQL;

public class PageAccessor {
	PreparedStatement insert_, query_;
	public PageAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public PageAccessor(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + ResolvedPage.PAGE_NUMBER +" FROM " + ResolvedPage.TABLE + " WHERE " + ResolvedPage.DOC_ID + " = ?");
		insert_ = conn.prepareStatement("INSERT INTO " + ResolvedPage.TABLE + " (" + ResolvedPage.DOC_ID + "," + ResolvedPage.PAGE_NUMBER + ") VALUES (?,?)");
	}
	public ResolvedPage getResolvedPage(int doc_id) throws SQLException {
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				return null;
			
			ResolvedPage rp = new ResolvedPage();
			rp.docId_ = doc_id;
			rp.pageNumber_ = rs.getInt(1);
			return rp;
		} finally {
			rs.close();
		}
	}
	public int[] getDocsForPageRange(Integer begin_inclusive, Integer end_exclusive) throws SQLException {
		Statement st = SQL.forThread().createStatement();
		try {
			String query = "SELECT " + ResolvedPage.PAGE_NUMBER +" FROM " + ResolvedPage.TABLE;
			if(begin_inclusive != null && end_exclusive != null) {
				query += " WHERE " + ResolvedPage.PAGE_NUMBER + ">=" + begin_inclusive + " AND " + ResolvedPage.PAGE_NUMBER + "<" + end_exclusive;
			} else if(begin_inclusive != null) {
				query += " WHERE " + ResolvedPage.PAGE_NUMBER + ">=" + begin_inclusive;
			} else if(end_exclusive != null) {
				query += " WHERE " + ResolvedPage.PAGE_NUMBER + "<" + end_exclusive;
			}
			
			query += " ORDER BY " + ResolvedPage.DOC_ID;
			ResultSet rs = st.executeQuery(query);
			try {
				TIntLinkedList docs = new TIntLinkedList();
				while(rs.next()) {
					docs.add(rs.getInt(1));
				}
				return docs.toArray(new int[docs.size()]);
			} finally {
				rs.close();
			}
		} finally {
			st.close();
		}
	}
	public void setResolution(int doc_id, int page_number) throws SQLException {
		insert_.setInt(1, doc_id);
		insert_.setInt(2, page_number);
		insert_.executeUpdate();
	}

}
