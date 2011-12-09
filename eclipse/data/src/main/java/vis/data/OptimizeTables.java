package vis.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import vis.data.util.SQL;

public class OptimizeTables {
	public static void main(String[] args) {
		Connection conn = SQL.forThread();
		
		try {
			ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
			LinkedList<String> tables = new LinkedList<String>();
			while(rs.next()) {
				tables.add(rs.getString("TABLE_NAME"));
			}
			rs.close();
			Statement st = conn.createStatement();
			for(String table : tables) {
				System.out.println("optimizing table " + table);
				st.execute("OPTIMIZE TABLE " + table);
			}
			
		} catch (SQLException e) {
			throw new RuntimeException("failed to optimize tables", e);
		}

	}

}
