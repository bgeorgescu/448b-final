package vis.data.model.query;


public class ThresholdTerm {
	public static class Parameters {
		public int threshold_;

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
			return threshold_ == p.threshold_;
		}
		
	}
}
