package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.DocLemma;
import vis.data.util.SQL;

public class EntityHits extends BaseHits {
	PreparedStatement query_;
	public EntityHits() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + DocLemma.ENTITY_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " = ?");
	}
	@Override
	PreparedStatement countsQuery() {
		return query_;
	}
	@Override
	int maxItemId() {
		return IdLists.maxEntities();
	}
	@Override
	int maxCountedItemId() {
		return IdLists.maxDocs();
	}
	public int[] getEntities(int doc_id) throws SQLException {
		return getItems(doc_id);
	}
	final static int BATCH_SIZE = 1024;
	public int[] getEntities(int docs[]) throws SQLException {
		return getItems(docs);
	}
	public static class Counts {
		public int docId_;
		public int[] entityId_;
		public int[] count_;
	}
	public Counts getEntityCounts(int doc_id) throws SQLException {
		Pair<int[], int[]> rc = getCounts(doc_id);
		Counts c = new Counts();
		c.docId_ = doc_id;
		c.entityId_ = rc.getKey();
		c.count_ = rc.getValue();
		return c;
	}
	public Pair<int[], int[]> getEntityCounts(int docs[]) throws SQLException {
		return getCounts(docs);
	}
	public static void pack(DocLemma dl, Counts c) {
		dl.entityList_ = pack(c.entityId_, c.count_);
		dl.docId_ = c.docId_;
	}
}
