package vis.data.util;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.ResultSetIterator;

public class StringArrayResultSetIterator extends ResultSetIterator {
	public StringArrayResultSetIterator(ResultSet rs) {
		super(rs, new BasicRowProcessor() {
			@Override
			public Object[] toArray(ResultSet rs) throws SQLException {
				if(rs.isAfterLast())
					return null;
				String[] res = new String[rs.getMetaData().getColumnCount()];
				for(int i = 0; i < res.length; ++i)
					res[i] = rs.getString(i + 1);
				return res;
			}
		});
	}
	@Override
	public String[] next() {
		Object fields[] = super.next();
		if(fields == null)
			return null;
		return (String[])fields;
	}
}
