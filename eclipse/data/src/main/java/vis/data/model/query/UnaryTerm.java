package vis.data.model.query;

import java.util.Collection;
import java.util.LinkedList;


public abstract class UnaryTerm extends Term {
	public abstract static class Parameters implements Term.Parameters {
		//forces this term to return 0's for counts;
		public boolean filterOnly_;
		//these are the terms to combine
		public QueryExpression term_;
		
		public int hashCode() {
			int hashCode = new Boolean(filterOnly_).hashCode();
			hashCode ^= term_.hashCode();
			return hashCode;
		}
		
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(filterOnly_ != p.filterOnly_) {
				return false;
			}
			return term_.equals(p.term_);
		}	
		@Override
		public void validate() {
			if(term_ == null)
				throw new RuntimeException("term missing for unary operation");
			term_.validate();
		}
		@Override
		public ResultType resultType() {
			return term_.parameters_.resultType();
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			LinkedList<Term.Parameters> c = new LinkedList<Term.Parameters>();
			c.add(term_.parameters_);
			Collection<Term.Parameters> cc = term_.parameters_.withChildren();
			if(cc != null)
				c.addAll(cc);
			c.add(this);
			return c;
		}
		@Override
		public void setFilterOnly() {
			filterOnly_ = true;
		}
	}
	final Parameters parameters_;
	public UnaryTerm(Parameters p) {
		parameters_ = p;
	}
	@Override
	public Term.Parameters parameters() {
		return parameters_;
	}
}
