package vis.data.model.query;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.RawEntity;
import vis.data.model.meta.DocFroEntityAccessor;
import vis.data.model.meta.EntityAccessor;
import vis.data.util.CountAggregator;
import vis.data.util.SetAggregator;

public class EntityTerm extends Term {
	public static class Parameters extends RawEntity {
		public boolean filterOnly_;

		@Override
		public int hashCode() {
			int hashCode = new Boolean(filterOnly_).hashCode();
			hashCode ^= id_;
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
	}

	DocFroEntityAccessor deh = new DocFroEntityAccessor();
	
	public final int[] entities_;
	public final boolean filterOnly_;
	public final Parameters parameters_;
	public final int docs_[];
	public final int count_[];
	public EntityTerm(Parameters p) throws SQLException {
		parameters_ = p;
		filterOnly_ = p.filterOnly_;
		if(p.id_ != 0) {
			entities_ = new int[1];
			entities_[0] = p.id_;
		} else if (p.entity_ != null || p.type_ != null){
			EntityAccessor er = new EntityAccessor();
			RawEntity rls[] = null;
			if(p.entity_ != null && p.type_ != null) {
				RawEntity rl = er.lookupEntity(p.entity_, p.type_);
				if(rl == null) {
					rls = new RawEntity[0];
				} else {
					rls = new RawEntity[1];
					rls[0] = rl; 
				}
			} else if(p.entity_ != null) {
				rls = er.lookupEntityByName(p.entity_);
			} else if(p.type_ != null) {
				rls = er.lookupEntityByType(p.type_);
			} else {
				throw new RuntimeException("incomplete lemma term");
			}
			
			int[] ids = new int[rls.length];
			for(int i = 0; i < ids.length; ++i) {
				ids[i] = rls[i].id_;
			}
			Arrays.sort(ids);
			entities_ = ids;
		} else {
			throw new RuntimeException("failed setting up LemmaTerm");
		}

		if(entities_.length == 0) {
			docs_ = new int[0];
			count_ = new int[0];
		} else {
			DocFroEntityAccessor.Counts initial = deh.getDocCounts(entities_[0]);
			for(int i = 1; i < entities_.length; ++i) {
				DocFroEntityAccessor.Counts partial = deh.getDocCounts(entities_[1]);
				Pair<int[], int[]> res = CountAggregator.or(initial.docId_, initial.count_, partial.docId_, partial.count_);
				initial.docId_ = res.getKey();
				initial.count_ = res.getValue();
			}
			docs_ = initial.docId_;
			count_ = initial.count_;
		}
	}

	public Object parameters() {
		return parameters_;
	}	

	@Override
	public boolean isFilter() {
		return filterOnly_;
	}

	@Override
	public int size() {
		return docs_.length;
	}

	@Override
	public int[] filter(int[] items) throws SQLException {
		if(entities_.length == 0)
			return new int[0];
		if(items == null)
			return docs_;
		else
			return SetAggregator.and(docs_, items);
	}

	@Override
	public Pair<int[], int[]> filter(int[] in_docs, int[] in_counts)
			throws SQLException {
		if(entities_.length == 0)
			return Pair.of(new int[0], new int[0]);
		if(in_docs == null)
			return Pair.of(docs_, new int[docs_.length]);
		else
			return CountAggregator.filter(in_docs, in_counts, docs_);
	}

	@Override
	public Pair<int[], int[]> aggregate(int[] in_docs, int[] in_counts)
			throws SQLException {
		if(entities_.length == 0)
			return Pair.of(new int[0], new int[0]);
		if(in_docs == null)
			return Pair.of(docs_, count_);
		else
			return CountAggregator.and(docs_, count_, in_docs, in_counts);
	}
}
