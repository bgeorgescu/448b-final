package vis.data;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

import vis.data.model.meta.EntityForDocAccessor;
import vis.data.model.meta.IdListAccessor;
import vis.data.model.meta.LemmaForDocHitsAccessor;
import vis.data.util.CountAggregator;
import vis.data.util.ExceptionHandler;

//i initially forgot to sort the doc and entity lists
public class ResortHitLists {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		try {
			int items[];
			items = IdListAccessor.allDocs();
			EntityForDocAccessor ed = new EntityForDocAccessor();
			for(int item : items) {
				Pair<int[], int[]> hits = ed.getCounts(item);
				int old_id[] = hits.getKey().clone();
				CountAggregator.sortByIdAsc(hits.getKey(), hits.getValue());
				if(Arrays.equals(hits.getKey(), old_id)) {
					continue;
				}
				System.out.println("mismatch for entity doc: " + item);
				int changed = ed.updateHitList(item, hits.getKey(), hits.getValue());
				if(changed != 1) 
					throw new RuntimeException("unexpect update count " + changed);
			}
			LemmaForDocHitsAccessor ld = new LemmaForDocHitsAccessor();
			for(int item : items) {
				Pair<int[], int[]> hits = ld.getCounts(item);
				int old_id[] = hits.getKey().clone();
				CountAggregator.sortByIdAsc(hits.getKey(), hits.getValue());
				if(Arrays.equals(hits.getKey(), old_id)) {
					continue;
				}
				System.out.println("mismatch for lemma doc: " + item);
				int changed = ld.updateHitList(item, hits.getKey(), hits.getValue());
				if(changed != 1) 
					throw new RuntimeException("unexpect update count " + changed);
			}
		} catch(SQLException e) {
			throw new RuntimeException("error resorting hit lists", e);
		}
	}
}
