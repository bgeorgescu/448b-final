package vis.data.model.query;

import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.util.CountAggregator;


public class ThresholdTerm extends UnaryTerm {

	public static class Parameters extends UnaryTerm.Parameters {
		public Integer threshold_;

		@Override
		public int hashCode() {
			int hashCode = new Integer(threshold_).hashCode();
			hashCode ^= Parameters.class.hashCode();
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(threshold_ == p.threshold_)
				return false;
			return super.equals(obj);
		}

		@Override
		public void validate() {
			if(threshold_ == null)
				throw new RuntimeException("missing threshold");
			if(threshold_ < 0)
				throw new RuntimeException("threshold " + threshold_ + " doesn't make sense");
		}

		@Override
		public ResultType resultType() {
			return term_.parameters_.resultType();
		}
		
	}

	public ThresholdTerm(Parameters p) {
		super(p);
	}

	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> operand = parameters_.term_.term().result();
		Pair<int[], int[]> result = CountAggregator.threshold(operand.getKey(), operand.getValue(), ((Parameters)parameters_).threshold_);
		if(parameters_.filterOnly_)
			return Pair.of(result.getKey(), new int[result.getKey().length]);
		return result;
	}
}
