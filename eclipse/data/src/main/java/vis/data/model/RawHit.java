package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name=RawHit.TABLE,uniqueConstraints=@UniqueConstraint(columnNames={RawHit.DOC_ID, RawHit.WORD_ID}))
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
