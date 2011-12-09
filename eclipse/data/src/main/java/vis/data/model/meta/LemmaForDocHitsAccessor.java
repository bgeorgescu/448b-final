package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.DocLemma;
import vis.data.util.SQL;

public class LemmaForDocHitsAccessor extends BaseHitsAccessor {
	PreparedStatement query_, update_;
	public LemmaForDocHitsAccessor() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + DocLemma.LEMMA_LIST + " FROM " + DocLemma.TABLE + " WHERE " + DocLemma.DOC_ID + " = ?");
		update_ = conn.prepareStatement("UPDATE " + DocLemma.TABLE + " SET " + DocLemma.LEMMA_LIST + " = ? " + " WHERE " + DocLemma.DOC_ID + " = ? ");
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
		return IdListAccessor.maxLemmas();
	}
	@Override
	int maxCountedItemId() {
		return IdListAccessor.maxDocs();
	}
	public int[] getLemmas(int doc_id) throws SQLException {
		return getItems(doc_id);
	}
	final static int BATCH_SIZE = 1024;
	public int[] getLemmas(int docs[]) throws SQLException {
		return getItems(docs);
	}
	public static class Counts {
		public int docId_;
		public int[] lemmaId_;
		public int[] count_;
	}
	public Counts getLemmaCounts(int doc_id) throws SQLException {
		Pair<int[], int[]> rc = getCounts(doc_id);
		Counts c = new Counts();
		c.docId_ = doc_id;
		c.lemmaId_ = rc.getKey();
		c.count_ = rc.getValue();
		return c;
	}
	public Pair<int[], int[]> getLemmaCounts(int docs[]) throws SQLException {
		return getCounts(docs);
	}

	public static void pack(DocLemma dl, Counts c) {
		dl.lemmaList_ = pack(c.lemmaId_, c.count_);
		dl.docId_ = c.docId_;
	}
}
