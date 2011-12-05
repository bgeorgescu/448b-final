package vis.data.model.query;

import java.util.Arrays;

public abstract class LogicalTerm {
	public abstract static class Parameters {
		//if this is set then the counts for terms_[0] are propagated
		public boolean filterOnly_;
		//these are the terms to combine
		public QueryExpression terms_[];
		
		//force implementation
		@Override
		public abstract int hashCode();
		
		protected int hashCodeBase() {
			int hashCode = new Boolean(filterOnly_).hashCode();
			hashCode ^= Arrays.hashCode(terms_);
			return hashCode;
		}
		
		//force implementation
		@Override
		public abstract boolean equals(Object obj);
		
		protected boolean equalsBase(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(filterOnly_ != p.filterOnly_) {
				return false;
			}
			return Arrays.equals(terms_, p.terms_);
		}	
	}
}
