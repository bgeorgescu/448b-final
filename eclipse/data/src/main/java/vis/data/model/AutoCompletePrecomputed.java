package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name=AutoCompletePrecomputed.TABLE)
public class AutoCompletePrecomputed {
		public static final String TABLE = "autocompleteprecomputed";
		
		public static final String PARTIAL_TERM="partial_term";
		@Column(name=PARTIAL_TERM, columnDefinition="VARCHAR(32) PRIMARY KEY")
		public String partialTerm_;

		public static final String PACKED_COMPLETIONS="packed_completion";
		@Column(name=PACKED_COMPLETIONS, columnDefinition="LONGBLOB NOT NULL")
		public int packedCompletions_;

		public static final String PACKED_PARTIAL_COMPLETIONS="packed_partial_completions";
		@Column(name=PACKED_PARTIAL_COMPLETIONS, columnDefinition="BLOB")
		public int packedPartialCompletions_;
}
