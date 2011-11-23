package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name=DocLemma.TABLE)
public class DocLemma {
	public static final String TABLE = "doclemma";
	
	public static final String DOC_ID="doc_id";
	@Column(name=DOC_ID, columnDefinition="INT PRIMARY KEY")
	public int docId_;

	public static final String LEMMA_LIST="lemma_list";
	@Column(name=LEMMA_LIST, columnDefinition="MEDIUMBLOB")
	public byte[] lemmaList_;

	public static final String ENTITY_LIST="entity_list";
	@Column(name=ENTITY_LIST, columnDefinition="MEDIUMBLOB")
	public byte[] entityList_;
}
