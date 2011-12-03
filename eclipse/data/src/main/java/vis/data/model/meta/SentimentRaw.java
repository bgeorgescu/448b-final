package vis.data.model.meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import vis.data.model.RawSentiment;
import vis.data.util.SQL;

public class SentimentRaw {
	PreparedStatement query_, queryBySentiment_, queryList_;
	public SentimentRaw() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + RawSentiment.SENTIMENT  + " FROM " + RawSentiment.TABLE + " WHERE " + RawSentiment.ID + " = ?");
		queryBySentiment_ = conn.prepareStatement("SELECT " + RawSentiment.ID + " FROM " + RawSentiment.TABLE + " WHERE " + RawSentiment.SENTIMENT + " = ?");
		queryList_ = conn.prepareStatement("SELECT " + RawSentiment.ID + "," + RawSentiment.SENTIMENT + " FROM " + RawSentiment.TABLE);
	}

	public RawSentiment getSentiment(int sentiment_id) throws SQLException {
		query_.setInt(1, sentiment_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find sentiment_id " + sentiment_id);
			
			RawSentiment raws = new RawSentiment();
			raws.id_ = sentiment_id;
			raws.sentiment_ = rs.getString(1);
			return raws;
		} finally {
			rs.close();
		}
	}
	public RawSentiment lookupSentiment(String sentiment) throws SQLException {
		sentiment = sentiment.toLowerCase();
		queryBySentiment_.setString(1, sentiment);
		ResultSet rs = queryBySentiment_.executeQuery();
		try {
			if(!rs.next())
				return null;
			
			RawSentiment raws = new RawSentiment();
			raws.id_ =  rs.getInt(1);
			raws.sentiment_ = sentiment;
			return raws;
		} finally {
			rs.close();
		}
	}

	public List<RawSentiment> listSentiments() throws SQLException {
		List<RawSentiment> sentiments = new LinkedList<RawSentiment>();
		ResultSet rs = queryList_.executeQuery();
		try {
			while(rs.next()) {
				RawSentiment raws = new RawSentiment();
				raws.id_ =  rs.getInt(1);
				raws.sentiment_ = rs.getString(2);
				sentiments.add(raws);
			}
		} finally {
			rs.close();
		}
		return sentiments;
	}
}
