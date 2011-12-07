package vis.data;

import java.sql.SQLException;
import java.util.LinkedList;

import vis.data.model.meta.AutoCompleteAccessor;
import vis.data.util.ExceptionHandler;

public class PrecomputeAutoComplete {
	static final int ROW_THRESHOLD = 1000;
	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		try {
			AutoCompleteAccessor aca = new AutoCompleteAccessor();
			LinkedList<String> todo = new LinkedList<String>();
			LinkedList<String> work = new LinkedList<String>();
			work.add("");
			while(!work.isEmpty()) {
				String prefix = work.pollLast();
				int possibilities = aca.countPossibilites(prefix);
				if(possibilities < ROW_THRESHOLD)
					continue;
				System.out.println(prefix + ":" + possibilities);
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
