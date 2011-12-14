package vis.data.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

@Table(name=RequestLogEntry.TABLE)
public class RequestLogEntry {
	public static final String TABLE = "requestlogentry";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String DURATION="duration";
	@Column(name=DURATION, columnDefinition="BIGINT NOT NULL")
	public int duration_;

	public static final String DATE="date";
	@Column(name=DATE, columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	public Timestamp date_;
	
	
	public static final String URI="uri";
	@Column(name=URI, columnDefinition="VARCHAR(128) NOT NULL")
	public int uri_;

	public static final String QUERY="query";
	@Column(name=QUERY, columnDefinition="LONGTEXT")
	public String query_;
}
