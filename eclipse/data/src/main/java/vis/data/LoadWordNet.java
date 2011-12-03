package vis.data;

import java.io.File;

import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;


public class LoadWordNet {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		SQL.importMysqlDump(new File("extra/wordnet/mysql-wn-schema.sql"));
		SQL.importMysqlDump(new File("extra/wordnet/mysql-wn-data.sql"));
		SQL.importMysqlDump(new File("extra/wordnet/mysql-wn-constraints.sql"));
		SQL.importMysqlDump(new File("extra/wordnet/mysql-wn-views.sql"));
	}

}
