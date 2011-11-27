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
	
	public static class Lemma {
		public String word_;
		public String pos_;
	}

	public static class Parameters {
		public Lemma lemmas_[];
		public int lemmaIds_[];
	}
	
	public final int[] lemmas_;
	public LemmaFilterTerm(Parameters p) throws SQLException {
		if(p.lemmaIds_ != null) {
			lemmas_ = p.lemmaIds_;
		} else if (p.lemmas_ != null){
			Connection conn = SQL.forThread();
			LemmaRaw lr = new LemmaRaw(conn);
			int[] lemmas = null;
			for(Lemma l : p.lemmas_) {
				if(lemmas != null && lemmas.length == 0)
					break;
				int[] ids = null;					
				if(l.pos_ == null && l.word_ != null) {
					RawLemma[] rls = lr.lookupLemma(l.word_);
					if(rls == null) {
						ids = new int[0];
					} else {
						ids = new int[rls.length];
						for(int i = 0; i < ids.length; ++i) {
							ids[i] = rls[i].id_;
						}
					}
				}
				if(l.pos_ != null && l.word_ != null) {
					RawLemma rl = lr.lookupLemma(l.word_, l.pos_);
					if(rl == null) {
						ids = new int[0];
					} else {
						ids = new int[1];
						ids[0] = rl.id_;
					}
					
				}
				if(ids == null)
					throw new RuntimeException("incomplete lemma term");
				//may not come back from the db sorted
				Arrays.sort(ids);
				if(lemmas == null) {
					lemmas = ids;
				} else {
					lemmas = SetAggregator.and(lemmas, ids);
				}
			}				
			lemmas_ = lemmas;
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
				docs = SetAggregator.and(docs, partial_docs);
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
