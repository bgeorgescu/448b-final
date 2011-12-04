package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import vis.data.model.annotations.Index;
import vis.data.model.annotations.NonUniqueIndexes;

@Table(name=AutoComplete.TABLE, uniqueConstraints={@UniqueConstraint(columnNames={AutoComplete.TERM_ID, AutoComplete.TYPE, AutoComplete.REFERENCE_ID})})
@NonUniqueIndexes(indexes={
	@Index(columnNames={AutoComplete.TERM_ID}),
	@Index(columnNames={AutoComplete.TYPE, AutoComplete.TERM_ID})
})
public class AutoComplete {
		public static final String TABLE = "autocomplete";
		
		public static final String TERM_ID="term_id";
		@Column(name=TERM_ID, columnDefinition="INT NOT NULL")
		public int termId_;

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
		@Column(name=TYPE, columnDefinition="INT NOT NULL")
		public Type type_;
		
		public static final String REFERENCE_ID="reference_id";
		@Column(name=REFERENCE_ID, columnDefinition="INT NOT NULL")
		public int referenceId_;
		
		public static final String SCORE="score";
		@Column(name=SCORE, columnDefinition="INT NOT NULL")
		public int score_;
}
