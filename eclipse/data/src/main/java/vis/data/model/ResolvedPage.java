package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

import vis.data.model.annotations.Index;
import vis.data.model.annotations.NonUniqueIndexes;

@Table(name=ResolvedPage.TABLE)
@NonUniqueIndexes(indexes=@Index(columnNames={ResolvedPage.PAGE_NUMBER, ResolvedPage.DOC_ID}))
public class ResolvedPage {
	public static final String TABLE = "resolvedpage";
	
	public static final String DOC_ID="doc_id";
	//also want index
	@Column(name=DOC_ID, columnDefinition="INT PRIMARY KEY")
	public int docId_;

	public static final String PAGE_NUMBER="page_number";
	//also want index
	@Column(name=PAGE_NUMBER, columnDefinition="INT NOT NULL")
	public int pageNumber_;
}
