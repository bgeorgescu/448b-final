package vis.data;

import java.sql.SQLException;

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
	}
}
