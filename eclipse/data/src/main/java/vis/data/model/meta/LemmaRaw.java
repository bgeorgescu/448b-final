package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.RawLemma;

public class LemmaRaw {
	PreparedStatement query_;
	public LemmaRaw(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + RawLemma.LEMMA + "," + RawLemma.POS + " FROM " + RawLemma.TABLE + " WHERE " + RawLemma.ID + " = ?");
	}
	public RawLemma getLemma(int lemma_id) throws SQLException {
		query_.setInt(1, lemma_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find lemma_id " + lemma_id);
			
			RawLemma rl = new RawLemma();
			rl.id_ = lemma_id;
			rl.lemma_ = rs.getString(1);
			rl.pos_ = rs.getString(2);
			return rl;
		} finally {
			rs.close();
		}
	}
}
