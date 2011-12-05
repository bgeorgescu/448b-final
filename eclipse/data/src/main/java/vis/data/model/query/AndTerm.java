package vis.data.model.query;

public class AndTerm extends LogicalTerm {
	public static class Parameters extends LogicalTerm.Parameters {
		@Override
		public int hashCode() {
			int hashCode = hashCodeBase();
			hashCode ^= Parameters.class.hashCode();
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			return equalsBase(obj);
		}
	}

}
