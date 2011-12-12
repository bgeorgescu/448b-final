package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.EntityAccessor;
import vis.data.util.CountAggregator;

public class TypeTerm extends Term {
	public static class Parameters extends UnaryTerm.Parameters {
		public String type_;
		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			if(type_ != null)
				hashCode ^= type_.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(type_ != null ^ p.type_ != null) {
				return false;
			}
			if(type_ != null && !type_.equals(p.type_)) {
				return false;
			}
			return super.equals(obj);
		}	
		@Override
		public void validate() {
			if(type_ == null)
				throw new RuntimeException("must specify either type to filter on");
			super.validate();
			if(term_.parameters_.resultType() != ResultType.ENTITY_HITS)
				throw new RuntimeException("type term requires entity hits child expresion");
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
	}
	
	public final Parameters parameters_;
	public final int entities_[];
	public TypeTerm(Parameters parameters) throws SQLException {
		parameters_ = parameters;
		EntityAccessor ea = new EntityAccessor();
		entities_ = ea.lookupEntityIdsByType(parameters_.type_);
	}

	public Term.Parameters parameters() {
		return parameters_;
	}	

	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> r = parameters_.term_.term().result();
		return CountAggregator.filter(r.getKey(), r.getValue(), entities_);
	}
}
