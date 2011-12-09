package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.DocLemma;
import vis.data.util.SQL;

public class EntityForDocAccessor extends BaseHitsAccessor {
	PreparedStatement query_, update_;
	public EntityForDocAccessor() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + DocLemma.ENTITY_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " = ?");
		update_ = conn.prepareStatement("UPDATE " + DocLemma.TABLE + " SET " + DocLemma.ENTITY_LIST + " = ? " + " WHERE " + DocLemma.DOC_ID + " = ? ");
	}
	@Override
	String bulkCountsQueryBase() {
		return "SELECT " + DocLemma.ENTITY_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " IN ";
	}
	@Override
	PreparedStatement countsQuery() {
		return query_;
	}
	@Override
	PreparedStatement updateQuery() {
		return update_;
	}
	@Override
	int maxItemId() {
		return IdListAccessor.maxEntities();
	}
	@Override
	int maxCountedItemId() {
		return IdListAccessor.maxDocs();
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
