package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

@Table(name=RawWord.TABLE)
public class RawWord {
	public static final String TABLE = "rawword";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String WORD="word";
	//also want index
	@Column(name=WORD, columnDefinition="VARCHAR(255) UNIQUE NOT NULL")
	public int word_;
}
