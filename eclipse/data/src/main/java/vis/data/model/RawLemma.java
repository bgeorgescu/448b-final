package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import vis.data.model.annotations.Index;
import vis.data.model.annotations.NonUniqueIndexes;

@Table(name=RawLemma.TABLE, 
	uniqueConstraints={
		@UniqueConstraint(columnNames={RawLemma.LEMMA, RawLemma.POS})
	})
@NonUniqueIndexes(indexes=@Index(columnNames={RawLemma.POS, RawLemma.ID}))
public class RawLemma {
	public static final String TABLE = "rawlemma";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String LEMMA="lemma";

	@Column(name=LEMMA, columnDefinition="VARCHAR(256) NOT NULL")
	public String lemma_;
	public static final String POS="pos";

	@Column(name=POS, columnDefinition="VARCHAR(8) NOT NULL")
	public String pos_;
}
