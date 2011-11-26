package vis.data.assortedjunk;

import java.sql.Connection;
import java.sql.SQLException;

import vis.data.model.RawDoc;
import vis.data.model.meta.DocRaw;
import vis.data.model.meta.EntityRaw;
import vis.data.model.meta.IdLists;
import vis.data.model.meta.LemmaEntityCorefs;
import vis.data.model.meta.LemmaRaw;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

public class DumpCoreferences {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		Connection conn = SQL.forThread();
		try {
			int[] all_corefed = IdLists.allCoreferencedDocuments();
			LemmaEntityCorefs lec = new LemmaEntityCorefs();
			DocRaw dr = new DocRaw(conn);
			//note this is not particularly efficient because these do not cache
			EntityRaw er = new EntityRaw(conn);
			LemmaRaw lr = new LemmaRaw(conn);
			
			for(int i : all_corefed) {
				RawDoc rd = dr.getDocMeta(i);
				LemmaEntityCorefs.dumpCorefs(System.out, rd.title_, er, lr, lec.getCorefs(i));
			}
		} catch (SQLException e) {
			throw new RuntimeException("sql error", e);
		}

	}
}
