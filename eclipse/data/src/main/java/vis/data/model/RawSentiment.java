package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name=RawSentiment.TABLE, 
	uniqueConstraints={
			@UniqueConstraint(columnNames=RawSentiment.SENTIMENT)})
public class RawSentiment {
	public static final String TABLE = "rawsentiment";
	

	public static final String ID="id";
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String SENTIMENT="word";
	@Column(name=SENTIMENT, columnDefinition="VARCHAR(256) NOT NULL")
	public String sentiment_;

}
