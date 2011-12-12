package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.LemmaAccessor;
import vis.data.util.CountAggregator;

public class PosTerm extends Term {
	public static class Parameters extends UnaryTerm.Parameters {
		public String pos_;
		public String posPrefix_;
		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			if(posPrefix_ != null)
				hashCode ^= posPrefix_.hashCode();
			if(pos_ != null)
				hashCode ^= pos_.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(posPrefix_ != null ^ p.posPrefix_ != null) {
				return false;
			}
			if(posPrefix_ != null && !posPrefix_.equals(p.posPrefix_)) {
				return false;
			}
			if(pos_ != null ^ p.pos_ != null) {
				return false;
			}
			if(pos_ != null && !pos_.equals(p.pos_)) {
				return false;
			}
			return super.equals(obj);
		}	
		@Override
		public void validate() {
			if(posPrefix_ == null && pos_ == null)
				throw new RuntimeException("must specify either a pos or posPrefix");
			super.validate();
			if(term_.parameters_.resultType() != ResultType.LEMMA_HITS)
				throw new RuntimeException("pos term requires lemma hits child expresion");
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
	}
	
	public final Parameters parameters_;
	public final int lemmas_[];
	public PosTerm(Parameters parameters) throws SQLException {
		parameters_ = parameters;
		LemmaAccessor la = new LemmaAccessor();
		if(parameters_.pos_ != null)
			lemmas_ = la.lookupLemmaIdsByPos(parameters_.pos_);
		else
			lemmas_ = la.lookupLemmaIdsByPosPrefix(parameters_.posPrefix_);
	}

	public Term.Parameters parameters() {
		return parameters_;
	}	

	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> r = parameters_.term_.term().result();
		return CountAggregator.filter(r.getKey(), r.getValue(), lemmas_);
	}
}
