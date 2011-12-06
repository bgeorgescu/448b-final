package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.RawDoc;
import vis.data.util.SQL;
import vis.data.util.SQL.NullForLastRowProcessor;

public class DocAccessor {
	PreparedStatement queryFull_;
	PreparedStatement queryMeta_;
	PreparedStatement queryAllMetaUnordered_;
	public DocAccessor() throws SQLException {
		this(SQL.forThread());
	}
	public DocAccessor(Connection conn) throws SQLException {
		queryFull_ = conn.prepareStatement("SELECT " + 
					RawDoc.DATE + 
					"," + RawDoc.PAGE + 
					"," + RawDoc.PUB_ID + 
					"," + RawDoc.SECTION_RAW + 
					"," + RawDoc.TITLE + 
					"," + RawDoc.SUBTITLE + 
					"," + RawDoc.FULL_TEXT + 
					"," + RawDoc.ORIGINAL_DOC_ID + 
					"," + RawDoc.ORIGINAL_FILENAME + 
					" FROM " + RawDoc.TABLE + " WHERE " + RawDoc.ID + " = ?");
		queryMeta_ = conn.prepareStatement("SELECT " +
				RawDoc.DATE + 
				"," + RawDoc.PAGE + 
				"," + RawDoc.PUB_ID + 
				"," + RawDoc.SECTION_RAW + 
				"," + RawDoc.TITLE + 
				"," + RawDoc.SUBTITLE + 
				"," + RawDoc.ORIGINAL_DOC_ID + 
				"," + RawDoc.ORIGINAL_FILENAME + 
				" FROM " + RawDoc.TABLE + " WHERE " + RawDoc.ID + " = ?");
		queryAllMetaUnordered_ = conn.prepareStatement("SELECT " +
				RawDoc.ID + 
				"," + RawDoc.DATE + 
				"," + RawDoc.PAGE + 
				"," + RawDoc.PUB_ID + 
				"," + RawDoc.SECTION_RAW + 
				"," + RawDoc.TITLE + 
				"," + RawDoc.SUBTITLE + 
				"," + RawDoc.ORIGINAL_DOC_ID + 
				"," + RawDoc.ORIGINAL_FILENAME + 
				" FROM " + RawDoc.TABLE);
		queryAllMetaUnordered_.setFetchSize(Integer.MIN_VALUE);
	}
	public RawDoc getDocMeta(int doc_id) throws SQLException {
		queryMeta_.setInt(1, doc_id);
		ResultSet rs = queryMeta_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find entity_id " + doc_id);
			
			RawDoc rd = new RawDoc();
			rd.id_ = doc_id;
			rd.date_ = rs.getInt(1);
			rd.page_ = rs.getString(2);
			rd.pubId_ = rs.getInt(3);
			rd.sectionRaw_ = rs.getString(4);
			rd.title_ = rs.getString(5);
			rd.subtitle_ = rs.getString(6);
			rd.docId_ = rs.getLong(7);
			rd.originalFilename_ = rs.getString(8);
			return rd;
		} finally {
			rs.close();
		}
	}
	public RawDoc getDocFull(int doc_id) throws SQLException {
		queryFull_.setInt(1, doc_id);
		ResultSet rs = queryFull_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find entity_id " + doc_id);
			
			RawDoc rd = new RawDoc();
			rd.id_ = doc_id;
			rd.date_ = rs.getInt(1);
			rd.page_ = rs.getString(2);
			rd.pubId_ = rs.getInt(3);
			rd.sectionRaw_ = rs.getString(4);
			rd.title_ = rs.getString(5);
			rd.subtitle_ = rs.getString(6);
			rd.fullText_ = rs.getString(7);
			rd.docId_ = rs.getLong(8);
			rd.originalFilename_ = rs.getString(9);
			return rd;
		} finally {
			rs.close();
		}
	}

	public static class ResultSetIterator extends org.apache.commons.dbutils.ResultSetIterator {
		public ResultSetIterator(ResultSet rs) {
			super(rs, new NullForLastRowProcessor());
		}
		
		public RawDoc nextDocMetadata() {
			RawDoc rd = new RawDoc();
			Object fields[] = super.next();
			if(fields == null)
				return null;
			rd.id_ = (Integer)fields[0];
			rd.date_ = (Integer)fields[1];
			rd.page_ = (String)fields[2];
			rd.pubId_ = (Integer)fields[3];
			rd.sectionRaw_ = (String)fields[4];
			rd.title_ = (String)fields[5];
			rd.subtitle_ = (String)fields[6];
			rd.docId_ = (Long)fields[7];
			rd.originalFilename_ = (String)fields[8];
			return rd;
		}
	}
	public ResultSetIterator docMetaIterator() throws SQLException {
		ResultSet rs = queryAllMetaUnordered_.executeQuery();
		return new ResultSetIterator(rs);
	}
}
