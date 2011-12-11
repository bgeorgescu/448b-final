package vis.data.model.query;

import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.LemmaForDocHitsAccessor;


public class DocLemmaTerm extends UnaryTerm {

	public static class Parameters extends UnaryTerm.Parameters {
		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode ^= Parameters.class.hashCode();
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			return super.equals(obj);
		}

		@Override
		public void validate() {
			super.validate();
			if(term_.parameters_.resultType() != ResultType.DOC_HITS)
				throw new RuntimeException("doclemma term must transform a document hit count map");
		}

		@Override
		public ResultType resultType() {
			return ResultType.LEMMA_HITS;
		}
		
	}

	public DocLemmaTerm(Parameters p) {
		super(p);
	}

	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> operand = parameters_.term_.term().result();

		LemmaForDocHitsAccessor lh = new LemmaForDocHitsAccessor();
		Pair<int[], int[]> result = lh.getCounts(operand.getKey()); 
		
		if(parameters_.filterOnly_)
			return Pair.of(result.getKey(), new int[result.getKey().length]);
		return result;
	}
}
