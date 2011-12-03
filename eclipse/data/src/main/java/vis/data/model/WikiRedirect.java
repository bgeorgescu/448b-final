package vis.data.model;

import javax.persistence.Column;

//this is not a complete representation
public class WikiRedirect {
	public static final String TABLE = "redirect";
	
	public static final String FROM="rd_from";
	@Column(name=FROM)
	public int from_;

	public static final String NAMESPACE="rd_namespace";
	@Column(name=NAMESPACE)
	public int namespace_;


	public static final String TITLE="rd_title";
	@Column(name=TITLE)
	public String title_;
}
