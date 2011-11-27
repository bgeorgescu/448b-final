package vis.data.model.query;

import java.sql.SQLException;
import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;

public abstract class Term {
	//return the amount of work this term does, so the terms can be applied in an appropriate order
	public abstract int size();

	//terms are cached
	public abstract int hashCode();
	public abstract boolean equals(Object other);
	
	public static abstract class Filter extends Term {
		//take the item/count list and apply this term to it
		//null is passed in if the item set is **ALL** items, e.g. at first
		public abstract int[] filter(int items[]) throws SQLException;

	}
	public static abstract class Aggregate extends Term {
		//take the item/count list and filter it by this term to it
		//null is passed in if the item set is **ALL** items, e.g. at first
		public abstract Pair<int[], int[]> aggregate (int items[], int counts[]) throws SQLException;

	}
	public static class WorkOrder implements Comparator<Term> {
		@Override
		public int compare(Term o1, Term o2) {
			if(o1.size() < o2.size())
				return -1;
			if(o1.size() > o2.size())
				return 1;
			return 0;
		}
	}
}
