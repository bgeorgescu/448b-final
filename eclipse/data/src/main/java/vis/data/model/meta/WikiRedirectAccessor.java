package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.WikiPage;
import vis.data.model.WikiRedirect;
import vis.data.util.SQL;
import vis.data.util.StringArrayResultSetIterator;


public class WikiRedirectAccessor {
	PreparedStatement queryList_;
	public WikiRedirectAccessor() throws SQLException {
		Connection conn = SQL.forThread();
		queryList_ = conn.prepareStatement("SELECT " + WikiPage.TITLE + "," + WikiRedirect.TITLE + " FROM " + WikiPage.TABLE + " JOIN " + WikiRedirect.TABLE + " ON " + WikiPage.ID + " = " + WikiRedirect.FROM);
		//stream these
		queryList_.setFetchSize(Integer.MIN_VALUE);
	}
	public StringArrayResultSetIterator redirectIterator() throws SQLException {
		ResultSet rs = queryList_.executeQuery();
		return new StringArrayResultSetIterator(rs);
	}
}
