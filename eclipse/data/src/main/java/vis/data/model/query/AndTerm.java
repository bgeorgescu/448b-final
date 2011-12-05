package vis.data.model.query;

import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.util.CountAggregator;

public class AndTerm extends NAryTerm {
	public static class Parameters extends NAryTerm.Parameters {
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
	}
	public AndTerm(Parameters p) {
		super(p);
	}
	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> partial = parameters_.terms_[0].term().result();
		for(int i = 1; i < parameters_.terms_.length; ++i) {
			Pair<int[], int[]> operand = parameters_.terms_[i].term().result();
			partial = CountAggregator.and(partial.getKey(), partial.getValue(), operand.getKey(), operand.getValue());
		}
		if(parameters_.filterOnly_)
			return Pair.of(partial.getKey(), new int[partial.getKey().length]);
		return partial;
	}

}
