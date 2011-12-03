package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name=RawSentimentWord.TABLE, 
	uniqueConstraints={
			@UniqueConstraint(columnNames={RawSentimentWord.SENTIMENT_ID, RawSentimentWord.WORD})})
public class RawSentimentWord {
	public static final String TABLE = "rawsentimentword";
	

	public static final String SENTIMENT_ID="sentiment_id";
	@Column(name=SENTIMENT_ID, columnDefinition="INT NOT NULL")
	public int sentimentId_;

	public static final String WORD="word";
	@Column(name=WORD, columnDefinition="VARCHAR(256) NOT NULL")
	public String word_;
}
