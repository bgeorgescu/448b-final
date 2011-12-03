package vis.data.model;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name=ResolvedEntity.TABLE,
	uniqueConstraints={
		@UniqueConstraint(columnNames={ResolvedEntity.FROM})})
public class ResolvedEntity {
	public static final String TABLE = "resolvedentity";
	
	public static final String FROM="entity_from";
	//also want index
	@Column(name=FROM, columnDefinition="INT NOT NULL")
	public int from_;

	public static final String TO="entity_to";
	//also want index
	@Column(name=TO, columnDefinition="INT NOT NULL")
	public int to_;
}
