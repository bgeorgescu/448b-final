package vis.data;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbutils.DbUtils;

import vis.data.model.AutoComplete;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

public class BuildAutoComplete {

	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		if(SQL.tableExists(AutoComplete.TABLE))
			return;
		try {
			SQL.createTable(SQL.forThread(), AutoComplete.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create autocomplete table", e);
		}
		
		Connection second = SQL.open();
		try {
			
		} finally {
			DbUtils.closeQuietly(second);
		}
	}
}
