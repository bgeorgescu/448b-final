package vis.data.model.query;

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
	public DocLemmaTerm.Parameters docLemma_;
	public DocEntity.Parameters docEntity_;
	
	//some things might return a lot of results we don't care about, e.g.
	//the tag cloud queries returning lemmas with 1 hit.  allow thresholding
	public ThresholdTerm.Parameters threshold_;
}
