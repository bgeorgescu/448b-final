package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.RawDoc;

public class DocRaw {
	PreparedStatement queryAll_;
	PreparedStatement queryMeta_;
	public DocRaw(Connection conn) throws SQLException {
		queryAll_ = conn.prepareStatement("SELECT " + 
					RawDoc.DATE + 
					"," + RawDoc.PAGE + 
					"," + RawDoc.PUB_ID + 
					"," + RawDoc.SECTION_RAW + 
					"," + RawDoc.TITLE + 
					"," + RawDoc.SUBTITLE + 
					"," + RawDoc.FULL_TEXT + 
					" FROM " + RawDoc.TABLE + " WHERE " + RawDoc.ID + " = ?");
		queryMeta_ = conn.prepareStatement("SELECT " +
				RawDoc.DATE + 
				"," + RawDoc.PAGE + 
				"," + RawDoc.PUB_ID + 
				"," + RawDoc.SECTION_RAW + 
				"," + RawDoc.TITLE + 
				"," + RawDoc.SUBTITLE + 
				" FROM " + RawDoc.TABLE + " WHERE " + RawDoc.ID + " = ?");
	}
	public RawDoc getDocMeta(int doc_id) throws SQLException {
		queryMeta_.setInt(1, doc_id);
		ResultSet rs = queryMeta_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find entity_id " + doc_id);
			
			RawDoc rd = new RawDoc();
			rd.id_ = doc_id;
			rd.date_ = rs.getString(1);
			rd.page_ = rs.getString(2);
			rd.pubId_ = rs.getString(3);
			rd.sectionRaw_ = rs.getString(4);
			rd.title_ = rs.getString(5);
			rd.subtitle_ = rs.getString(6);
			return rd;
		} finally {
			rs.close();
		}
	}
	public RawDoc getDocFull(int doc_id) throws SQLException {
		queryAll_.setInt(1, doc_id);
		ResultSet rs = queryAll_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find entity_id " + doc_id);
			
			RawDoc rd = new RawDoc();
			rd.id_ = doc_id;
			rd.date_ = rs.getString(1);
			rd.page_ = rs.getString(2);
			rd.pubId_ = rs.getString(3);
			rd.sectionRaw_ = rs.getString(4);
			rd.title_ = rs.getString(5);
			rd.subtitle_ = rs.getString(6);
			rd.fullText_ = rs.getString(7);
			return rd;
		} finally {
			rs.close();
		}
	}
}
