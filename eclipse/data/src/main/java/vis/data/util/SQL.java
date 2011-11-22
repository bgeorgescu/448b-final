package vis.data.util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

import vis.data.model.RawDoc;

public class SQL {

	public static java.util.Collection<String> getNonGenerated(Class cls) {
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

	public static void createTable(Connection conn, Class cls) throws SQLException {
		String TABLE_NAME = ((Table)cls.getAnnotation(Table.class)).name();

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
		st.execute("CREATE TABLE " + TABLE_NAME + "(" + table_spec  + ")");
		st.close();
	}
}
