package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name=AutoCompleteTerm.TABLE, uniqueConstraints={@UniqueConstraint(columnNames={AutoCompleteTerm.TERM})})
public class AutoCompleteTerm {
	public static final String TABLE = "term";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String TERM="term";
	public static final int TERM_LENGTH=64;
	@Column(name=TERM, columnDefinition="VARCHAR(" + TERM_LENGTH + ") NOT NULL")
	public String term_;
}
