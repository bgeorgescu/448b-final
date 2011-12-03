package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

//this is not a complete representation

@Table(name=WikiPage.TABLE)
public class WikiPage {
	public static final String TABLE = "page";
	
	public static final String ID="page_id";
	@Column(name=ID)
	public int id_;

	public static final String NAMESPACE="page_namespace";
	@Column(name=NAMESPACE)
	public int namespace_;

	public static final String IS_REDIRECT="page_is_redirect";
	@Column(name=IS_REDIRECT)
	public boolean isRedirect_;

	public static final String TITLE="page_title";
	@Column(name=TITLE)
	public String title_;
}
