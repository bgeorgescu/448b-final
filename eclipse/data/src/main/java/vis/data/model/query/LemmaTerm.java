package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawLemma;
import vis.data.model.meta.DocForLemmaAccessor;
import vis.data.model.meta.LemmaAccessor;

public class LemmaTerm extends Term {
	public static class Parameters extends RawLemma implements Term.Parameters {
		public boolean filterOnly_;

		@Override
		public int hashCode() {
			int hashCode = new Boolean(filterOnly_).hashCode();
			hashCode ^= id_;
			hashCode ^= Parameters.class.hashCode();
			if(lemma_ != null)
				hashCode ^= lemma_.hashCode();
			if(pos_ != null)
				hashCode ^= pos_.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(filterOnly_ != p.filterOnly_) {
				return false;
			}
			if(id_ != p.id_) {
				return false;
			}
			if(lemma_ != null ^ p.lemma_ != null) {
				return false;
			}
			if(lemma_ != null && !lemma_.equals(p.lemma_)) {
				return false;
			}
			if(pos_ != null ^ p.pos_ != null) {
				return false;
			}
			if(pos_ != null && !pos_.equals(p.pos_)) {
				return false;
			}
			return true;
		}	
		@Override
		public ResultType resultType() {
			return ResultType.DOC_HITS;
		}

		@Override
		public void validate() {
			if(id_ == 0 && lemma_ == null && pos_ == null)
				throw new RuntimeException("must specify either a lemma id, lemma, or pos");
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
		@Override
		public void setFilterOnly() {
			filterOnly_ = true;
		}
	}
	
	public final Parameters parameters_;
	public final int docs_[];
	public final int count_[];
	public LemmaTerm(Parameters parameters) throws SQLException {
		int lemmas[];
		DocForLemmaAccessor dlh = new DocForLemmaAccessor();
		parameters_ = parameters;
		if(parameters.id_ != 0) {
			lemmas = new int[1];
			lemmas[0] = parameters.id_;
		} else if (parameters.lemma_ != null || parameters.pos_ != null){
			LemmaAccessor lr = new LemmaAccessor();
			RawLemma rls[] = null;
			if(parameters.lemma_ != null && parameters.pos_ != null) {
				RawLemma rl = lr.lookupLemma(parameters.lemma_, parameters.pos_);
				if(rl == null) {
					rls = new RawLemma[0];
				} else {
					rls = new RawLemma[1];
					rls[0] = rl; 
				}
			} else if(parameters.lemma_ != null) {
				rls = lr.lookupLemmaByWord(parameters.lemma_);
			} else if(parameters.pos_ != null) {
				rls = lr.lookupLemmaByPos(parameters.pos_);
			} else {
				throw new RuntimeException("incomplete lemma term");
			}
			
			int[] ids = new int[rls.length];
			for(int i = 0; i < ids.length; ++i) {
				ids[i] = rls[i].id_;
			}
			Arrays.sort(ids);
			lemmas = ids;
		} else {
			throw new RuntimeException("failed setting up LemmaTerm");
		}

		if(lemmas.length == 0) {
			docs_ = new int[0];
			count_ = new int[0];
		} else {
			Pair<int[], int[]> res = dlh.getCounts(lemmas);
			docs_ = res.getKey();
			//TODO: wasteful
			count_ = parameters_.filterOnly_ ? new int[docs_.length] : res.getValue();
		}
	}

	public Term.Parameters parameters() {
		return parameters_;
	}	

	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		if(parameters_.filterOnly_)
			return Pair.of(docs_, new int[docs_.length]);
		return Pair.of(docs_, count_);
	}
}
