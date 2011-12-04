package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import vis.data.model.annotations.Index;
import vis.data.model.annotations.NonUniqueIndexes;

@Table(name=AutoComplete.TABLE, uniqueConstraints={@UniqueConstraint(columnNames={AutoComplete.TYPE, AutoComplete.REFERENCE_ID})})
@NonUniqueIndexes(indexes={
	@Index(columnNames={AutoComplete.TERM}),
	@Index(columnNames={AutoComplete.TYPE, AutoComplete.TERM})
})
public class AutoComplete {
		public static final String TABLE = "autocomplete";
		
		public static final String TERM="term";
		//also want index
		@Column(name=TERM, columnDefinition="VARCHAR(64) NOT NULL")
		public String term_;

		public static enum Type {
			//basic types
			ENTITY,
			LEMMA,
			//wordnet types
			SISTER,
			PARENT,
			CHILD,
			//sentiment groups
			SENTIMENT,
			//article metadata
			PAGE,
			SECTION,
			PUBLICATION,
		}
		public static final String TYPE="termtype";
		//also want index
		@Column(name=TYPE, columnDefinition="INT NOT NULL")
		public Type type_;
		
		public static final String REFERENCE_ID="reference_id";
		@Column(name=REFERENCE_ID, columnDefinition="INT NOT NULL")
		public int referenceId_;
		
		public static final String SCORE="score";
		//also want index
		@Column(name=SCORE, columnDefinition="INT NOT NULL")
		public int score_;
}
