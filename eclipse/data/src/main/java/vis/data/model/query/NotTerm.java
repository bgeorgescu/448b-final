package vis.data.model.query;

import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.IdListAccessor;
import vis.data.util.SetAggregator;


public class NotTerm extends UnaryTerm {

	public static class Parameters extends UnaryTerm.Parameters {
		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode ^= Parameters.class.hashCode();
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if(!Parameters.class.isInstance(obj))
				return false;
			return super.equals(obj);
		}
		
	}
	public NotTerm(Parameters p) {
		super(p);
	}
	@Override
	public Pair<int[], int[]> compute() throws SQLException {
		Pair<int[], int[]> operand = parameters_.term_.term().result();
		int[] ids;
		switch(parameters_.term_.parameters_.resultType()) {
		case DOC_HITS:
			ids = IdListAccessor.allDocsInOrder();
			break;
		case LEMMA_HITS:
			ids = IdListAccessor.allLemmasInOrder();
			break;
		case ENTITY_HITS:
			ids = IdListAccessor.allEntitiesInOrder();
			break;
		default:
			throw new RuntimeException("unknown result type in not term");
		}
		ids = SetAggregator.remove(ids, operand.getKey());
		return Pair.of(ids, new int[ids.length]);
	}

}
