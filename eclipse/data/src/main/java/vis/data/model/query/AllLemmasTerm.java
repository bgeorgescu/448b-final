package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.IdListAccessor;

public class AllLemmasTerm extends Term {
	public static class Parameters implements Term.Parameters {		
		@Override
		public int hashCode() {
			int hashCode = 0;
			hashCode ^= AllLemmasTerm.class.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			return true;

		}

		@Override
		public void validate() {
		}

		@Override
		public ResultType resultType() {
			return ResultType.LEMMA_HITS;
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
		@Override
		public void setFilterOnly() {
			//always is
		}
	}
	
	public final Parameters parameters_;
	public final int lemmas_[];
	public AllLemmasTerm(Parameters p) throws SQLException {
		parameters_ = p;
		lemmas_ = IdListAccessor.allLemmasInOrder();
	}

	public Term.Parameters parameters() {
		return parameters_;
	}	
	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		return Pair.of(lemmas_, new int[lemmas_.length]);
	}
}
