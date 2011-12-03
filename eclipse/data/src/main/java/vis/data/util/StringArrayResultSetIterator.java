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
				String[] res = new String[rs.getMetaData().getColumnCount()];
				for(int i = 0; i < res.length; ++i)
					res[i] = rs.getString(i);
				return res;
			}
		});
	}
	@Override
	public String[] next() {
		return (String[])super.next();
	}
}
