package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

@Table(name=RawHit.TABLE)
public class RawHit {
	public static final String TABLE = "rawhit";
	

	//want these two to be unique
	///////////////////////////////
	public static final String DOC_ID="doc_id";
	@Column(name=DOC_ID, columnDefinition="INT NOT NULL")
	public int docId_;
	
	public static final String WORD_ID="word_id";
	@Column(name=WORD_ID, columnDefinition="INT NOT NULL")
	public int wordId_;
	///////////////////////////////

	public static final String COUNT="count";
	@Column(name=COUNT, columnDefinition="INT NOT NULL")
	public int count_;

}
