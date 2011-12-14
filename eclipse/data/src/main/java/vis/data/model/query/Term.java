package vis.data.model.query;

import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	protected abstract Pair<int[], int[]> compute() throws SQLException;
	
	public Pair<int[], int[]> result() throws SQLException {
		Pair<int[], int[]> cached = getCache(parameters());
		if(cached != null)
			return cached;
		cached = compute();
		putCache(parameters(), cached);
		return cached;
	}
	static Map<Term.Parameters, SoftReference<Pair<int[], int[]> >> g_result_cache = Collections.synchronizedMap(new HashMap<Term.Parameters, SoftReference<Pair<int[], int[]> >>());
	static Pair<int[], int[]> getCache(Term.Parameters param) {
		SoftReference<Pair<int[], int[]>> srs = g_result_cache.get(param);
		if(srs == null) {
//			System.out.println("missed cache: " + param.getClass().getName());
			return null;
		}
//		System.out.println("partial hit cache: " + param.getClass().getName());
		Pair<int[], int[]> res = srs.get();
		if(res == null)
			return null;
//		System.out.println("hit result cache, sz: " + res.getKey().length);
		return res;
	}
	static void putCache(Term.Parameters param, Pair<int[], int[]> t) {
		g_result_cache.put(param, new SoftReference<Pair<int[], int[]>>(t));
	}
}
