package vis.data.model.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawLemma;
import vis.data.model.meta.DocLemmaHits;
import vis.data.model.meta.LemmaRaw;
import vis.data.util.SQL;
import vis.data.util.SetAggregator;

public class LemmaFilterTerm extends Term.Filter {
	Pair<String, String> f;
	DocLemmaHits dlh = new DocLemmaHits();
	
	public final int[] lemmas_;
	public LemmaFilterTerm(RawLemma p) throws SQLException {
		if(p.id_ != 0) {
			lemmas_ = new int[1];
			lemmas_[0] = p.id_;
		} else if (p.lemma_ != null || p.pos_ != null){
			Connection conn = SQL.forThread();
			LemmaRaw lr = new LemmaRaw(conn);
			RawLemma rls[] = null;
			if(p.lemma_ != null && p.pos_ != null) {
				RawLemma rl = lr.lookupLemma(p.lemma_, p.pos_);
				if(rl == null) {
					rls = new RawLemma[0];
				} else {
					rls = new RawLemma[1];
					rls[0] = rl; 
				}
			} else if(p.lemma_ != null) {
				rls = lr.lookupLemmaByWord(p.lemma_);
			} else if(p.pos_ != null) {
				rls = lr.lookupLemmaByPos(p.pos_);
			} else {
				throw new RuntimeException("incomplete lemma term");
			}
			
			int[] ids = new int[rls.length];
			for(int i = 0; i < ids.length; ++i) {
				ids[i] = rls[i].id_;
			}
			Arrays.sort(ids);
			lemmas_ = ids;
		} else {
			throw new RuntimeException("failed setting up LemmaTerm");
		}
	}

	@Override
	public int size() {
		return lemmas_.length;
	}

	@Override
	public int[] filter(int[] items) throws SQLException {
		int[] docs = null;
		for(int lemma : lemmas_) {
			int[] partial_docs = dlh.getDocs(lemma);
			if(docs == null)
				docs = partial_docs;
			else 
				docs = SetAggregator.or(docs, partial_docs);
		}
		if(items == null)
			return docs;
		else
			return SetAggregator.and(docs, items);
	}

	@Override
	public boolean equals(Object other) {
		if(other.getClass() != LemmaFilterTerm.class)
			return false;
		LemmaFilterTerm o = (LemmaFilterTerm)other;
		return Arrays.equals(lemmas_, o.lemmas_);
	}
	@Override
	public int hashCode() {
		return Arrays.hashCode(lemmas_) ^ LemmaFilterTerm.class.hashCode();
	}
}
