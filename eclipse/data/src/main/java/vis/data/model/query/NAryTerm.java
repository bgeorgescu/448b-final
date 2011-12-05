package vis.data.model.query;

import java.util.Arrays;

public abstract class NAryTerm extends Term {
	public abstract static class Parameters implements Term.Parameters {
		//forces this term to return 0's for counts;
		public boolean filterOnly_;
		//these are the terms to combine
		public QueryExpression terms_[];
		
		public int hashCode() {
			int hashCode = new Boolean(filterOnly_).hashCode();
			hashCode ^= Arrays.hashCode(terms_);
			return hashCode;
		}
		
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(filterOnly_ != p.filterOnly_) {
				return false;
			}
			return Arrays.equals(terms_, p.terms_);
		}	
		@Override
		public void validate() {
			if(terms_ == null)
				throw new RuntimeException("terms missing for nary operation");
			if(terms_.length < 2)
				throw new RuntimeException("too few terms for nary operation");
			ResultType rt = terms_[0].parameters_.resultType();
			for(QueryExpression qe : terms_) {
				qe.validate();
				if(qe.parameters_.resultType() != rt) 
					throw new RuntimeException("result type mismatch " + rt + " != " + qe.parameters_.resultType());
			}
		}
		@Override
		public ResultType resultType() {
			return terms_[0].parameters_.resultType();
		}
	}
	final Parameters parameters_;
	public NAryTerm(Parameters p) {
		parameters_ = p;
	}
	@Override
	public Term.Parameters parameters() {
		return parameters_;
	}
}
