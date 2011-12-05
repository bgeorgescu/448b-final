package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.model.meta.DocForEntityAccessor;
import vis.data.model.meta.EntityAccessor;
import vis.data.util.CountAggregator;

public class EntityTerm extends Term {
	public static class Parameters extends RawEntity implements Term.Parameters {
		public boolean filterOnly_;

		@Override
		public int hashCode() {
			int hashCode = new Boolean(filterOnly_).hashCode();
			hashCode ^= id_;
			hashCode ^= Parameters.class.hashCode();
			if(entity_ != null)
				hashCode ^= entity_.hashCode();
			if(type_ != null)
				hashCode ^= type_.hashCode();
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			Parameters p = (Parameters)obj;
			if(filterOnly_ != p.filterOnly_) {
				return false;
			}
			if(id_ != p.id_) {
				return false;
			}
			if(entity_ != null ^ p.entity_ != null) {
				return false;
			}
			if(entity_ != null && !entity_.equals(p.entity_)) {
				return false;
			}
			if(type_ != null ^ p.type_ != null) {
				return false;
			}
			if(type_ != null && !type_.equals(p.type_)) {
				return false;
			}
			return true;
		}	
		@Override
		public ResultType resultType() {
			return ResultType.DOC_HITS;
		}

		@Override
		public void validate() {
			if(id_ == 0 && entity_ == null && type_ == null)
				throw new RuntimeException("must specify either an entity id, entity, or type");
		}
		@Override
		public Collection<Term.Parameters> withChildren() {
			return Arrays.asList((Term.Parameters)this);
		}
		@Override
		public void setFilterOnly() {
			filterOnly_ = true;
		}
	}
	
	public final Parameters parameters_;
	public final int docs_[];
	public final int count_[];
	public EntityTerm(Parameters parameters) throws SQLException {
		DocForEntityAccessor deh = new DocForEntityAccessor();
		int entities[];
		parameters_ = parameters;
		if(parameters.id_ != 0) {
			entities = new int[1];
			entities[0] = parameters.id_;
		} else if (parameters.entity_ != null || parameters.type_ != null){
			EntityAccessor er = new EntityAccessor();
			RawEntity rls[] = null;
			if(parameters.entity_ != null && parameters.type_ != null) {
				RawEntity rl = er.lookupEntity(parameters.entity_, parameters.type_);
				if(rl == null) {
					rls = new RawEntity[0];
				} else {
					rls = new RawEntity[1];
					rls[0] = rl; 
				}
			} else if(parameters.entity_ != null) {
				rls = er.lookupEntityByName(parameters.entity_);
			} else if(parameters.type_ != null) {
				rls = er.lookupEntityByType(parameters.type_);
			} else {
				throw new RuntimeException("incomplete lemma term");
			}
			
			int[] ids = new int[rls.length];
			for(int i = 0; i < ids.length; ++i) {
				ids[i] = rls[i].id_;
			}
			Arrays.sort(ids);
			entities = ids;
		} else {
			throw new RuntimeException("failed setting up LemmaTerm");
		}

		if(entities.length == 0) {
			docs_ = new int[0];
			count_ = new int[0];
		} else {
			DocForEntityAccessor.Counts initial = deh.getDocCounts(entities[0]);
			for(int i = 1; i < entities.length; ++i) {
				DocForEntityAccessor.Counts partial = deh.getDocCounts(entities[1]);
				Pair<int[], int[]> res = CountAggregator.or(initial.docId_, initial.count_, partial.docId_, partial.count_);
				initial.docId_ = res.getKey();
				initial.count_ = res.getValue();
			}
			docs_ = initial.docId_;
			//TODO: wasteful
			count_ = parameters_.filterOnly_ ? new int[docs_.length] : initial.count_;
		}
	}

	public Term.Parameters parameters() {
		return parameters_;
	}	

	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		return Pair.of(docs_, count_);
	}
}
