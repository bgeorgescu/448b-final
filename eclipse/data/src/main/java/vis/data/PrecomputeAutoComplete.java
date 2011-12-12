package vis.data;

import java.sql.SQLException;
import java.util.LinkedList;

import vis.data.model.AutoCompleteEntry;
import vis.data.model.AutoCompletePrecomputed;
import vis.data.model.meta.AutoCompleteAccessor;
import vis.data.model.meta.AutoCompleteAccessor.RawResultSetIterator;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;

public class PrecomputeAutoComplete {
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		try {
			SQL.createTable(SQL.forThread(), AutoCompletePrecomputed.class);
		} catch(SQLException e) {
			throw new RuntimeException("failed to create precomputed autocomplete table", e);
		}
		
		try {
			AutoCompleteAccessor ac_enum = new AutoCompleteAccessor(SQL.open());
			AutoCompleteAccessor aca = new AutoCompleteAccessor();
			LinkedList<String> todo = new LinkedList<String>();
			LinkedList<String> work = new LinkedList<String>();
			work.add("");
			while(!work.isEmpty()) {
				String prefix = work.pollLast();
				if(prefix.length() > 0) {
					int possibilities = aca.countPossibilites(prefix);
					if(possibilities < AutoCompletePrecomputed.ROW_THRESHOLD)
						continue;
					System.out.println(prefix + ":" + possibilities);
					RawResultSetIterator i = ac_enum.autoCompleteRawIterator(prefix);
					AutoCompleteEntry ace;
					LinkedList<AutoCompleteEntry> aces = new LinkedList<AutoCompleteEntry>();
					while((ace = i.nextRaw()) != null) {
						aces.add(ace);
					}
					aca.addAutoCompletePrecomputed(prefix, aces);
				}
				todo.add(prefix);
				for(char c = 'a'; c <= 'z'; ++c) {
					work.add(prefix + c);
				}
			}
			System.out.println("TODO: " + todo.size());
		} catch (SQLException e) {
			throw new RuntimeException("failed to precompute autocomplete", e);
		}
	}
}
