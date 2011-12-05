package vis.data.model.query;

import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vis.data.model.query.Term.Parameters;

public class QueryExpression {
	//set operations
	public OrTerm.Parameters or_;
	public AndTerm.Parameters and_;
	public NotTerm.Parameters not_; //only allowed as filter, not top level

	//full text queries - can be aggregated
	public LemmaTerm.Parameters lemma_;
	public EntityTerm.Parameters entity_;
	
	//meta data queries - filter only
	public DateTerm.Parameters date_;
	public SQLSectionTerm.Parameters section_;
	public SQLPageTerm.Parameters page_;
	public SQLPublicationTerm.Parameters publication_;
	
	//tag cloud queries - requires all full text queries to be filter only
	//also these two are mutually exclusive
//	public DocLemmaTerm.Parameters docLemma_;
//	public DocEntity.Parameters docEntity_;
	
	//some things might return a lot of results we don't care about, e.g.
	//the tag cloud queries returning lemmas with 1 hit.  allow thresholding
	public ThresholdTerm.Parameters threshold_;
	
	
	//This is the processed state for the term
	protected Term.Parameters parameters_;
	
	private void checkOnly() {
		if(parameters_ != null) 
			throw new RuntimeException("an expression can only have one operator");
	}
	public Parameters validate() {
		if(parameters_ != null)
			return parameters_;
		Field fields[] = QueryExpression.class.getDeclaredFields();
		for(Field f : fields) {
			if((f.getModifiers() & Modifier.PUBLIC) == 0)
				continue;
			Object p;
			try {
				p = f.get(this);
			} catch (Exception e) {
				throw new RuntimeException("failed to extract parameter instance", e);
			}
			if(p == null)
				continue;
			if(!Term.Parameters.class.isInstance(p))
				continue;
			checkOnly();
			parameters_ = (Term.Parameters)p;
			parameters_.validate();
		}
		return parameters_;
	}
	//must validate first
	public Term term() {
		Term cache = QueryExpression.getCache(parameters_);
		if(cache != null)
			return cache;
		try {
			Class<?> term_class = parameters_.getClass().getEnclosingClass();
			Constructor<Term> constructor = (Constructor<Term>)term_class.getConstructor(parameters_.getClass());
			cache = constructor.newInstance(parameters_);
			QueryExpression.putCache(parameters_, cache);
			return cache;
		} catch (Exception e) {
			throw new RuntimeException("failed to create term", e);
		}
	}
	//this discards stuff too early
	//static Map<Object, Term> g_term_cache = Collections.synchronizedMap(new WeakHashMap<Object, Term>());
	//this "leaks" term parameter blocks
	static Map<Object, SoftReference<Term>> g_term_cache = Collections.synchronizedMap(new HashMap<Object, SoftReference<Term>>());
	static Term getCache(Object param) {
		SoftReference<Term> srt = g_term_cache.get(param);
		if(srt == null)
			return null;
		return srt.get();
	}
	static void putCache(Object param, Term t) {
		g_term_cache.put(param, new SoftReference<Term>(t));
	}
}
