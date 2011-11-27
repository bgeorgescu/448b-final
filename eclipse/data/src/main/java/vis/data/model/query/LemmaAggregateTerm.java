package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawLemma;
import vis.data.model.meta.DocLemmaHits;
import vis.data.model.meta.LemmaRaw;
import vis.data.util.CountAggregator;

public class LemmaAggregateTerm extends Term.Aggregate {
	DocLemmaHits dlh = new DocLemmaHits();
	
	public final int[] lemmas_;
	public LemmaAggregateTerm(RawLemma p) throws SQLException {
		if(p.id_ != 0) {
			lemmas_ = new int[1];
			lemmas_[0] = p.id_;
		} else if (p.lemma_ != null || p.pos_ != null){
			LemmaRaw lr = new LemmaRaw();
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
	public Pair<int[], int[]> aggregate(int[] in_docs, int[] in_counts)
			throws SQLException {
		if(lemmas_.length == 0)
			return Pair.of(new int[0], new int[0]);
		DocLemmaHits.Counts initial = dlh.getDocCounts(lemmas_[0]);
		int[] docs = initial.docId_;
		int[] counts = initial.count_;
		//TODO: absolutely must be cached if it is applied to multiple items
		for(int i = 1; i < lemmas_.length; ++i) {
			DocLemmaHits.Counts partial = dlh.getDocCounts(lemmas_[1]);
			Pair<int[], int[]> res = CountAggregator.or(docs, counts, partial.docId_, partial.count_);
			docs = res.getKey();
			counts = res.getValue();
		}
		if(in_docs == null)
			return Pair.of(docs, counts);
		else
			return CountAggregator.and(docs, counts, in_docs, in_counts);
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
		return Arrays.hashCode(lemmas_) ^ LemmaAggregateTerm.class.hashCode();
	}	
}
