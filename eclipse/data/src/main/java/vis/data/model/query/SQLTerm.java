package vis.data.model.query;

import gnu.trove.list.linked.TIntLinkedList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.util.CountAggregator;
import vis.data.util.SQL;
import vis.data.util.SetAggregator;

public abstract class SQLTerm extends Term {
	final int ids_[];
	final String query_;
	protected SQLTerm(String id_query) throws SQLException {
		query_ = id_query;
		Connection conn = SQL.forThread();
		Statement st = conn.createStatement();
		try {
			ResultSet rs = st.executeQuery(id_query);
			if(!rs.next()) {
				ids_ = new int[0];
				return;
			}
			TIntLinkedList ids = new TIntLinkedList();
			do {
				ids.add(rs.getInt(1));
			} while(rs.next());
			ids_ = ids.toArray(new int[ids.size()]);
		} finally {
			st.close();
		}
	}

	@Override
	public int size() {
		return ids_.length;
	}

	@Override
	public int[] filter(int[] items) throws SQLException {
		if(ids_.length == 0)
			return new int[0];
		if(items == null)
			return ids_;
		else
			return SetAggregator.and(items, ids_);
	}

	@Override
	public Pair<int[], int[]> filter(int[] in_docs, int[] in_counts)
			throws SQLException {
		if(ids_.length == 0)
			return Pair.of(new int[0], new int[0]);
		if(in_docs == null)
			return Pair.of(ids_, new int[ids_.length]);
		else
			return CountAggregator.filter(in_docs, in_counts, ids_);
	}

	@Override
	public Pair<int[], int[]> aggregate(int[] in_docs, int[] in_counts)
			throws SQLException {
		return filter(in_docs, in_counts);
	}
}
