package vis.data.model.query;

import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.util.CountAggregator;


public class CountSortTerm extends UnaryTerm {

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
		}		
	}

	public CountSortTerm(Parameters p) {
		super(p);
	}

	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> operand = parameters_.term_.term().result();
		operand = Pair.of(operand.getKey().clone(), operand.getValue().clone());
		CountAggregator.sortByCountDesc(operand.getKey(), operand.getValue());
		return operand;
	}
}
