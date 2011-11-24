package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name=EntityDoc.TABLE)
public class EntityDoc {
	public static final String TABLE = "entitydoc";
	
	public static final String ENTITY_ID="entity_id";
	@Column(name=ENTITY_ID, columnDefinition="INT PRIMARY KEY")
	public int entityId_;

	public static final String DOC_LIST="doc_list";
	@Column(name=DOC_LIST, columnDefinition="LARGEBLOB")
	public byte[] docList_;
}
