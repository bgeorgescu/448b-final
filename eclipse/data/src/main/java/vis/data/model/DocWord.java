package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name=DocWord.TABLE)
public class DocWord {
	public static final String TABLE = "docword";
	
	public static final String DOC_ID="doc_id";
	@Column(name=DOC_ID, columnDefinition="INT PRIMARY KEY")
	public int docId_;

	public static final String WORD_LIST="word_list";
	@Column(name=WORD_LIST, columnDefinition="MEDIUMBLOB")
	public byte[] wordList_;
}
