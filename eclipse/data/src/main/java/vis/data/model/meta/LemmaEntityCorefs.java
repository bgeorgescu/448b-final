package vis.data.model.meta;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vis.data.model.DocCoref;
import vis.data.model.RawEntity;
import vis.data.model.RawLemma;
import vis.data.util.SQL;

public class LemmaEntityCorefs {
	PreparedStatement query_;
	public LemmaEntityCorefs() throws SQLException {
		Connection conn = SQL.forThread();
		query_ = conn.prepareStatement("SELECT " + DocCoref.COREF_LIST + " FROM " + DocCoref.TABLE + " WHERE " + DocCoref.DOC_ID + " = ?");
	}
	public static enum PhraseType {
		Entity,
		Lemma,
	};
	public static class Ref {
		public PhraseType type_[];
		public int id_[];
	}
	public static class Corefs {
		public int docId_;
		public Ref[][] ref_;
	}
	public Corefs getCorefs(int doc_id) throws SQLException {
		Corefs c = new Corefs();
		c.docId_ = doc_id;
		query_.setInt(1, doc_id);
		ResultSet rs = query_.executeQuery();
		try {
			if(!rs.next())
				throw new RuntimeException("failed to find doc_id " + doc_id);
			
			byte[] data = rs.getBytes(1);
			ByteBuffer bb = ByteBuffer.wrap(data);
			int sets = bb.getInt();
			c.ref_ = new Ref[sets][];
			for(int set = 0; set < sets; ++set) {
				int equivalences = bb.getInt();
				c.ref_[set] = new Ref[equivalences];
				for(int item = 0; item < equivalences; ++item) {
					c.ref_[set][item] = new Ref();
					int words = bb.getInt();
					c.ref_[set][item].type_ = new PhraseType[words];
					c.ref_[set][item].id_ = new int[words];
					for(int word = 0; word < words; ++word) {
						int packed_id = bb.getInt();
						c.ref_[set][item].type_[word] = packed_id < 0 ? PhraseType.Entity : PhraseType.Lemma;
						c.ref_[set][item].id_[word] = Math.abs(packed_id);
					}
				}
			}
			return c;
		} finally {
			rs.close();
		}
	}
	static int computeSize(Corefs c) {
		final int INT_SIZE = Integer.SIZE / 8;
		int sz = 0;
		sz += INT_SIZE; // # of coref sets
		for(int set = 0; set < c.ref_.length; ++set) {
			sz += INT_SIZE; // # of equivalences
			for(int item = 0; item < c.ref_[set].length; ++item) {
				sz += INT_SIZE; // # of words
				sz += c.ref_[set][item].id_.length * INT_SIZE;
			}
		}
		return sz;
	}
	public static DocCoref pack(Corefs c) {
		ByteBuffer bb = ByteBuffer.allocate(computeSize(c));
		
		bb.putInt(c.ref_.length);
		for(int set = 0; set < c.ref_.length; ++set) {
			bb.putInt(c.ref_[set].length);
			for(int item = 0; item < c.ref_[set].length; ++item) {
				bb.putInt(c.ref_[set][item].id_.length);
				for(int word = 0; word < c.ref_[set][item].id_.length; ++word) {
					switch(c.ref_[set][item].type_[word]) {
					case Entity:
						bb.putInt(-c.ref_[set][item].id_[word]);
						break;
					case Lemma:
						bb.putInt(c.ref_[set][item].id_[word]);
						break;
					}
				}
			}
		}
		DocCoref dc = new DocCoref();
		dc.docId_ = c.docId_;
		dc.corefList_ = bb.array();
		return dc;
	}
	public static void dumpCorefs(PrintStream o, String title, EntityRaw er, LemmaRaw lr, Corefs c) throws SQLException {
		o.println("--- " + title + " ---");
		for(int set = 0; set < c.ref_.length; ++set) {
			for(int item = 0; item < c.ref_[set].length; ++item) {
				for(int word = 0; word < c.ref_[set][item].id_.length; ++word) {
					switch(c.ref_[set][item].type_[word]) {
					case Entity:
						RawEntity re = er.getEntity(c.ref_[set][item].id_[word]);
						o.print(" " + re.entity_ + "/" + re.type_);
						break;
					case Lemma:
						RawLemma rl = lr.getLemma(c.ref_[set][item].id_[word]);
						o.print(" " + rl.lemma_ + "/" + rl.pos_);
						break;
					}
				}
				o.print(",");
			}
			o.println();
		}
	}
}
