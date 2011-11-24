package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name=LemmaDoc.TABLE)
public class LemmaDoc {
	public static final String TABLE = "lemmadoc";
	
	public static final String LEMMA_ID="lemma_id";
	@Column(name=LEMMA_ID, columnDefinition="INT PRIMARY KEY")
	public int lemmaId_;

	public static final String DOC_LIST="doc_list";
	@Column(name=DOC_LIST, columnDefinition="LONGBLOB")
	public byte[] docList_;
}
