package vis.data.model.meta;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.DocWord;

public class WordHits {
	PreparedStatement query_;
	public WordHits(Connection conn) throws SQLException {
		query_ = conn.prepareStatement("SELECT " + DocWord.WORD_LIST + " FROM " + DocWord.TABLE + " WHERE doc_id = ?");
	}
	public int[] getWords(int doc_id) throws SQLException {
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + doc_id);
			
			byte[] data = rs.getBytes(1);
			int[] word_ids = new int[data.length / (Integer.SIZE / 8) / 2];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < word_ids.length; ++i) {
				word_ids[i] = bb.getInt();
				/*int count =*/ bb.getInt();
			}
			return word_ids;
		} finally {
			rs.close();
		}
	}
	public class WordCount {
		int docId_;
		int[] wordId_;
		int[] count_;
	}
	public WordCount getWordCounts(int doc_id) throws SQLException {
		WordCount c = new WordCount();
		c.docId_ = doc_id;
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + doc_id);
			
			byte[] data = rs.getBytes(1);
			int num = data.length / (Integer.SIZE / 8) / 2;
			c.wordId_ = new int[num];
			c.count_ = new int[num];
			ByteBuffer bb = ByteBuffer.wrap(data);
			for(int i = 0; i < num; ++i) {
				c.wordId_[i] = bb.getInt();
				c.count_[i] = bb.getInt();
			}
			return c;
		} finally {
			rs.close();
		}
	}
}
