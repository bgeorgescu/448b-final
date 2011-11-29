package vis.data.model.meta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import vis.data.model.RawDoc;
import vis.data.util.SQL;

public class TimeSortedDoc {
	static int[] date_ = null;
	static int[] id_ = null;
	public TimeSortedDoc() throws SQLException {
		synchronized(TimeSortedDoc.class) {
			if(date_ != null)
				return;
			Connection conn = SQL.forThread();
			Statement st = conn.createStatement();
			try {
				ResultSet rs = st.executeQuery("SELECT COUNT("  + RawDoc.ID + ") FROM " + RawDoc.TABLE);
				try {
					if(!rs.next()) {
						date_ = new int[0];
						id_ = new int[0];
						return;
					} else {
						int max_id = rs.getInt(1);
						date_ = new int[max_id];
						id_ = new int[max_id];
					}
					
				} finally {
					rs.close();
				}
				rs = st.executeQuery("SELECT "  + RawDoc.ID + "," + RawDoc.DATE + " FROM " + RawDoc.TABLE + " ORDER BY " + RawDoc.DATE);
				try {
					for(int i = 0; rs.next(); ++i) {
						id_[i] = rs.getInt(1);
						date_[i] = rs.getInt(2);
					}
				} finally {
					rs.close();
				}
				
			} finally {
				st.close();
			}
		}
	}
	public int[] getDocsAfter(int after) {
		int pos = Arrays.binarySearch(date_, after);
		if(pos < 0)
			pos = -(pos + 1);
		return ArrayUtils.subarray(id_, pos, id_.length);
	}
	public int[] getDocsBefore(int before) {
		int pos = Arrays.binarySearch(date_, before);
		if(pos < 0)
			pos = -(pos + 1);
		return ArrayUtils.subarray(id_, 0, pos);
	}
	public int[] getDocsBetween(int before, int after) {
		int pos0 = Arrays.binarySearch(date_, before);
		int pos1 = Arrays.binarySearch(date_, after);
		if(pos0 < 0)
			pos0 = -(pos0 + 1);
		if(pos1 < 1)
			pos1 = -(pos1 + 1);
		
		return ArrayUtils.subarray(id_, Math.min(pos0,  pos1), Math.max(pos0, pos1));
	}
}
