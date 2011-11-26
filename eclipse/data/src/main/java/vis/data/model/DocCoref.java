package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name=DocCoref.TABLE)
public class DocCoref {
	public static final String TABLE = "doccoref";
	
	public static final String DOC_ID="doc_id";
	@Column(name=DOC_ID, columnDefinition="INT PRIMARY KEY")
	public int docId_;

	public static final String COREF_LIST="coref_list";
	@Column(name=COREF_LIST, columnDefinition="LARGEBLOB")
	public byte[] corefList_;

	public static final String ENTITY_LIST="entity_list";
	@Column(name=ENTITY_LIST, columnDefinition="MEDIUMBLOB")
	public byte[] entityList_;
}
