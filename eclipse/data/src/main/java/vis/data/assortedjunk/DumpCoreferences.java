package vis.data.assortedjunk;

import java.sql.Connection;
import java.sql.SQLException;

import vis.data.model.RawDoc;
import vis.data.model.meta.DocAccessor;
import vis.data.model.meta.EntityAccessor;
import vis.data.model.meta.IdListAccessor;
import vis.data.model.meta.LemmaEntityCorefForDocAccessor;
import vis.data.model.meta.LemmaAccessor;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

public class DumpCoreferences {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Connection conn = SQL.forThread();
		try {
			int[] all_corefed = IdListAccessor.allCoreferencedDocuments();
			LemmaEntityCorefForDocAccessor lec = new LemmaEntityCorefForDocAccessor();
			DocAccessor dr = new DocAccessor();
			//note this is not particularly efficient because these do not cache
			EntityAccessor er = new EntityAccessor();
			LemmaAccessor lr = new LemmaAccessor();
			
			for(int i : all_corefed) {
				RawDoc rd = dr.getDocMeta(i);
				LemmaEntityCorefForDocAccessor.dumpCorefs(System.out, rd.title_, er, lr, lec.getCorefs(i));
			}
		} catch (SQLException e) {
			throw new RuntimeException("sql error", e);
		} finally {
			try { conn.close(); } catch (SQLException e) {}
		}

	}
}
