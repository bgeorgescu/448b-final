package vis.data.assortedjunk;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbutils.DbUtils;

import vis.data.model.RawDoc;
import vis.data.model.RawLemma;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

public class RunSomeSQL {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Connection conn = SQL.forThread();
		try {
			try {
				Statement st = conn.createStatement();
				try {
					
					SQL.createUniqueIndexes(conn, RawLemma.class);
					SQL.createNonUniqueIndexes(conn, RawDoc.class);
					
				} finally {
					st.close();
				}
			} catch (SQLException e) {
				throw new RuntimeException("sql error", e);
			}
		} finally {
			DbUtils.closeQuietly(conn);
		}

	}
}
