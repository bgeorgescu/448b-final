package vis.data.model.query;

import java.sql.SQLException;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

public abstract class Term {
	//type of the result set
	public enum ResultType {
		DOC_HITS, //hits for lemmas/entities by doc id
		LEMMA_HITS, //hits for lemmas by lemma id
		ENTITY_HITS, //hits for entities by lemma id
	}

	public interface Parameters {
		//throw if the parameters are bad
		public void validate();
		public ResultType resultType();
		public Collection<Term.Parameters> withChildren();
		public void setFilterOnly();
	}
	
	//terms are cached
	public abstract Parameters parameters();

	public abstract Pair<int[], int[]> compute() throws SQLException;
	
	public Pair<int[], int[]> result() throws SQLException {
		//TODO: do the caching transparently here
		return compute();
	}
}
