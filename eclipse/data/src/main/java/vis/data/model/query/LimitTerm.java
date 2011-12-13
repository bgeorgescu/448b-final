package vis.data.model.query;

import java.sql.SQLException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;


public class LimitTerm extends UnaryTerm {

	public static class Parameters extends UnaryTerm.Parameters {
		public Integer limit_;

		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode ^= Parameters.class.hashCode();
			hashCode ^= limit_.hashCode();
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(limit_ != p.limit_)
				return false;
			return super.equals(obj);
		}

		@Override
		public void validate() {
			if(limit_ == null)
				throw new RuntimeException("limit terms must have a limit");
			super.validate();
		}
		
	}

	final int limit_;
	public LimitTerm(Parameters p) {
		super(p);
		limit_ = p.limit_;
	}

	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> operand = parameters_.term_.term().result();
		if(operand.getKey().length <= limit_)
			return operand;
		return Pair.of(
			ArrayUtils.subarray(operand.getKey().clone(), 0, limit_), 
			ArrayUtils.subarray(operand.getValue().clone(), 0, limit_));
	}
}
