package vis.data.model.query;

import org.apache.commons.lang3.tuple.Pair;

public abstract class Term {
	//return the amount of work this term does, so the terms can be applied in an appropriate order
	public abstract int size();

	//terms are cached
	public abstract int hashCode();
	public abstract boolean equals(Object other);
	
	public static abstract class Filter extends Term {
		//apply this filter to the list of possible items, e.g. limit the set of documents to aggregate over
		public abstract int[] prefilter(int[] items);

		//take the item/count list and apply this term to it
		public abstract int[] filter(int items[]);

	}
	public static abstract class Aggregate extends Term {
		//take the item/count list and apply this term to it, e.g. extract a count from each document and sum
		public abstract Pair<int[], int[]> aggregate (int items[], int counts[]);

	}
}
