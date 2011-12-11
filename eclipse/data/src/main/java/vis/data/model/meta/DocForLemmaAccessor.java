package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.LemmaDoc;
import vis.data.util.SQL;

public class DocForLemmaAccessor extends BaseHitsAccessor {
	PreparedStatement query_, update_;
	public DocForLemmaAccessor() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + LemmaDoc.DOC_LIST + " FROM " + LemmaDoc.TABLE + " WHERE " + LemmaDoc.LEMMA_ID + " = ?");
		update_ = conn.prepareStatement("UPDATE " + LemmaDoc.TABLE + " SET " + LemmaDoc.DOC_LIST + " = ? " + " WHERE " + LemmaDoc.LEMMA_ID + " = ? ");
	}
	@Override
	String bulkCountsQueryBase() {
		return "SELECT " + LemmaDoc.DOC_LIST + " FROM " + LemmaDoc.TABLE  + " WHERE " + LemmaDoc.LEMMA_ID + " IN ";
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
		return IdListAccessor.maxDocs();
	}
	@Override
	int maxCountedItemId() {
		return IdListAccessor.maxLemmas();
	}

	public static class Counts {
		public int lemmaId_;
		public int[] docId_;
		public int[] count_;
	}
	public Counts getDocCounts(int lemma_id) throws SQLException {
		Pair<int[], int[]> rc = getCounts(lemma_id);
		Counts c = new Counts();
		c.lemmaId_ = lemma_id;
		c.docId_ = rc.getKey();
		c.count_ = rc.getValue();
		return c;
	}
	public static LemmaDoc pack(Counts c) {
		LemmaDoc ld = new LemmaDoc();
		ld.docList_ = pack(c.docId_, c.count_);
		ld.lemmaId_ = c.lemmaId_;
		return ld;
	}
}
