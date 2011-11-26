package vis.data.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name=RawEntity.TABLE,
	uniqueConstraints={
		@UniqueConstraint(columnNames={RawEntity.ENTITY, RawEntity.TYPE}), 
		@UniqueConstraint(columnNames={RawEntity.TYPE, RawEntity.ENTITY})})
public class RawEntity {
	public static final String TABLE = "rawentity";
	
	public static final String ID="id";
	@GeneratedValue
	@Column(name=ID, columnDefinition="INT AUTO_INCREMENT PRIMARY KEY")
	public int id_;

	public static final String ENTITY="entity";
	//also want index
	@Column(name=ENTITY, columnDefinition="VARCHAR(255) NOT NULL")
	public String entity_;

	public static final String TYPE="type";
	//also want index
	@Column(name=TYPE, columnDefinition="VARCHAR(32) NOT NULL")
	public String type_;
}
