package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.EntityAccessor;

public class TypeTerm extends Term {
	public static class Parameters implements Term.Parameters {
		public String type_;
		@Override
		public int hashCode() {
			int hashCode = Parameters.class.hashCode();
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
			if(!type_.equals(p.type_)) {
				return false;
			}
			return true;
		}	
		@Override
		public void validate() {
			if(type_ == null)
				throw new RuntimeException("must specify either type to filter on");
		}

		@Override
		public ResultType resultType() {
			return ResultType.ENTITY_HITS;		}

		@Override
		public Collection<vis.data.model.query.Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
		@Override
		public void setFilterOnly() {
			//always is
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
		return Pair.of(entities_, new int[entities_.length]);
	}
}
