package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import vis.data.model.annotations.DML;

@Table(name=RawDoc.TABLE, uniqueConstraints=@UniqueConstraint(columnNames=RawDoc.ORIGINAL_DOC_ID))
public class RawDoc {
	public static final String TABLE = "rawdoc";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String PUB_ID="pub_id";
	@DML(xpath="/DMLDOC/pmdt/pmid")
	@Column(name=PUB_ID, columnDefinition="INT NOT NULL")
	public int pubId_;

	public static final String SECTION_RAW="section_raw";
	@DML(xpath="/DMLDOC/docdt/docsec")
	@Column(name=SECTION_RAW, columnDefinition="VARCHAR(64)")
	public String sectionRaw_;

	public static final String DATE="date";
	@DML(xpath="/DMLDOC/pcdt/pcdtn")
	@Column(name=DATE, columnDefinition="INT NOT NULL")
	public int date_;

	public static final String ORIGINAL_DOC_ID="doc_id";
	@DML(xpath="/DMLDOC/docdt/docid")
	@Column(name=ORIGINAL_DOC_ID, columnDefinition="BIGINT NOT NULL")
	public long docId_;

	public static final String TITLE="title";
	@DML(xpath="/DMLDOC/docdt/doctitle")
	@Column(name=TITLE, columnDefinition="VARCHAR(1024)")
	public String title_;

	public static final String SUBTITLE="subtitle";
	@DML(xpath="/DMLDOC/docdt/docsubt")
	@Column(name=SUBTITLE, columnDefinition="VARCHAR(1024)")
	public String subtitle_;

	public static final String PAGE="page";
	@DML(xpath="/DMLDOC/docdt/docpgn")
	@Column(name=PAGE, columnDefinition="VARCHAR(16)")
	public String page_;

	public static final String FULL_TEXT="full_text";
	@Column(name=FULL_TEXT, columnDefinition="MEDIUMTEXT")
	public String fullText_;
}
