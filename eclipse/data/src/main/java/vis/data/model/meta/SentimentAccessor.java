package vis.data.model.meta;

import gnu.trove.list.array.TIntArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import vis.data.model.RawLemma;
import vis.data.model.RawSentiment;
import vis.data.model.RawSentimentWord;
import vis.data.util.SQL;

public class SentimentAccessor {
	PreparedStatement query_, queryBySentiment_, queryList_,queryLemmasForSentiment_;
	public SentimentAccessor() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + RawSentiment.SENTIMENT  + " FROM " + RawSentiment.TABLE + " WHERE " + RawSentiment.ID + " = ?");
		queryBySentiment_ = conn.prepareStatement("SELECT " + RawSentiment.ID + " FROM " + RawSentiment.TABLE + " WHERE " + RawSentiment.SENTIMENT + " = ?");
		queryList_ = conn.prepareStatement("SELECT " + RawSentiment.ID + "," + RawSentiment.SENTIMENT + " FROM " + RawSentiment.TABLE);
		queryLemmasForSentiment_ = conn.prepareStatement("SELECT " + RawLemma.ID + " FROM " + RawSentimentWord.TABLE + " JOIN " + RawLemma.TABLE + " ON " + RawLemma.LEMMA + "=" + RawSentimentWord.WORD + " WHERE " + RawSentimentWord.SENTIMENT_ID + " = ?");
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
	public int[] getLemmasForSentiment(int sentiment_id) throws SQLException {
		queryLemmasForSentiment_.setInt(1, sentiment_id);
		ResultSet rs = queryLemmasForSentiment_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find any words for sentiment_id " + sentiment_id);
			
			TIntArrayList lemmas = new TIntArrayList();
			do {
				lemmas.add(rs.getInt(1));
			} while(rs.next());
			return lemmas.toArray();
		} finally {
			rs.close();
		}
	}
}
