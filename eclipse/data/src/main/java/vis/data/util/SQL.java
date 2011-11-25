package vis.data.util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

public class SQL {

	public static <T> java.util.Collection<String> getNonGenerated(Class<T> cls) {
		Set<String> fields = new TreeSet<String>();
		Field model_fields[] = cls.getFields();
		for(Field f : model_fields) {
			Column col = f.getAnnotation(Column.class);
			if(col == null)
				continue;
			GeneratedValue gen = f.getAnnotation(GeneratedValue.class);
			if(gen != null)
				continue;
			fields.add(col.name());
		}
		return fields;
	}

	public static <T> void createTable(Connection conn, Class<T> cls) throws SQLException {
		String TABLE_NAME = cls.getAnnotation(Table.class).name();

		TreeMap<String, String> col_def = new TreeMap<String, String>();
		Field model_fields[] = cls.getFields();
		for(Field f : model_fields) {
			Column col = f.getAnnotation(Column.class);
			if(col == null)
				continue;
			col_def.put(col.name(), col.columnDefinition());
		}		

		//also create the table... die if you run this without deleting the old table
		Iterator<String> i_name = col_def.keySet().iterator();
		Iterator<String> i_type = col_def.values().iterator();
		StringBuilder table_spec = new StringBuilder();
		table_spec.append(i_name.next());
		table_spec.append(' ');
		table_spec.append(i_type.next());
		for(;i_name.hasNext();) {
			table_spec.append(',');
			table_spec.append(i_name.next());
			table_spec.append(' ');
			table_spec.append(i_type.next());
			
		}
		System.out.println(table_spec);
		Statement st = conn.createStatement();
		try {
			st.execute("CREATE TABLE " + TABLE_NAME + "(" + table_spec  + ")");
		} finally {
			st.close();
		}
		createIndices(conn, cls);
	}
	public static <T> void createIndices(Connection conn, Class<T> cls) throws SQLException {
		String TABLE_NAME = cls.getAnnotation(Table.class).name();
		UniqueConstraint uniq[] = cls.getAnnotation(Table.class).uniqueConstraints();
		for(UniqueConstraint u : uniq) {
			String index_name = TABLE_NAME + "_by";
			String index_fields = "";
			for(String s : u.columnNames()) {
				if(!index_fields.isEmpty())
					index_fields += ", ";
				index_fields += s;
				index_name += "_" + s;
			}
			Statement st = conn.createStatement();
			try {
				st.execute("CREATE UNIQUE INDEX " + index_name + " ON " + TABLE_NAME + " (" + index_fields + ")");
			} finally {
				st.close();
			}
		}
		
	}

	public static Connection open() {
		Connection conn = null;
		try
		{
			System.out.println ("Trying to connect to database");
			String userName = "vis";
			String password = "vis";
			String url = "jdbc:mysql://localhost/vis";
			Class.forName ("com.mysql.jdbc.Driver").newInstance ();
			conn = DriverManager.getConnection (url, userName, password);
			if(conn == null)
				throw new RuntimeException("unknown sql connection creation returned null");
			System.out.println ("Database connection established");
			return conn;
		}
		catch (Exception e)
		{
			System.err.println ("Cannot connect to database server");
			throw new RuntimeException("Sql connection failed", e);
		}
	}
}
