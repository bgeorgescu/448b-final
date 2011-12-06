package vis.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;

import vis.data.model.RawDoc;
import vis.data.model.ResolvedPage;
import vis.data.model.meta.DocAccessor;
import vis.data.model.meta.DocAccessor.ResultSetIterator;
import vis.data.model.meta.PageAccessor;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

public class ResolvePageNumber {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		Connection conn = SQL.forThread();
		try {
			SQL.createTable(conn, ResolvedPage.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create resolved page table", e);
		}
		conn = SQL.open();
		try {
			//streaming needs its own connection
			DocAccessor da = new DocAccessor(conn);
			PageAccessor pa = new PageAccessor();
			ResultSetIterator i = da.docMetaIterator();
			RawDoc doc;
			
			Pattern p = Pattern.compile("[^\\d]*(\\d\\d*)[^\\d]*(?:(\\d\\d*)[^\\d]*)?");
			HashSet<String> uniques = new HashSet<String>();
			while ((doc = i.nextDocMetadata()) != null) {
				Matcher matcher = p.matcher(doc.page_);
				if(!matcher.matches()) {
					if(uniques.contains(doc.page_))
						continue;
					System.err.println("can't resolve page " + doc.page_);
					uniques.add(doc.page_);
					continue;
				}
				try {
					int page_number;
					if(matcher.group(2) != null)
						page_number = Integer.parseInt(matcher.group(2));
					else
						page_number = Integer.parseInt(matcher.group(1));
					pa.setResolution(doc.id_, page_number);

					if(uniques.contains(doc.page_))
						continue;
					System.out.println("resolved page " + doc.page_ + " to " + page_number);
					uniques.add(doc.page_);
				} catch(NumberFormatException e) {
					if(uniques.contains(doc.page_))
						continue;
					System.err.println("can't resolve page " + doc.page_);
					uniques.add(doc.page_);
					continue;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("failed to resolve pages", e);
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}

}
