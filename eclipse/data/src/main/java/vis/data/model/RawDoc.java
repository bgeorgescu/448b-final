package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import vis.data.model.annotations.DML;

@Table(name="rawdoc")
public class RawDoc {
	public static final String TABLE = "rawdoc";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String PUB_ID="pub_id";
	@DML(xpath="/DMLDOC/pmdt/pmid")
	@Column(name=PUB_ID, columnDefinition="TEXT")
	public String pubId_;

	public static final String SECTION_RAW="section_raw";
	@DML(xpath="/DMLDOC/docdt/docsec")
	@Column(name=SECTION_RAW, columnDefinition="TEXT")
	public String sectionRaw_;

	public static final String DATE="date";
	@DML(xpath="/DMLDOC/pcdt/pcdtn")
	@Column(name=DATE, columnDefinition="TEXT")
	public String date_;

	public static final String DOC_ID="doc_id";
	@DML(xpath="/DMLDOC/docdt/docid")
	@Column(name=DOC_ID, columnDefinition="TEXT")
	public String docId_;

	public static final String TITLE="title";
	@DML(xpath="/DMLDOC/docdt/doctitle")
	@Column(name=TITLE, columnDefinition="TEXT")
	public String title_;

	public static final String SUBTITLE="subtitle";
	@DML(xpath="/DMLDOC/docdt/docsubt")
	@Column(name=SUBTITLE, columnDefinition="TEXT")
	public String subtitle_;

	public static final String PAGE="page";
	@DML(xpath="/DMLDOC/docdt/docpgn")
	@Column(name=PAGE, columnDefinition="TEXT")
	public String page_;

	public static final String FULL_TEXT="full_text";
	@Column(name=FULL_TEXT, columnDefinition="MEDIUMTEXT")
	public String fullText_;
}
