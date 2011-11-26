package vis.data.model.query;

import org.apache.commons.lang3.tuple.Pair;

public abstract class Term {
	//return the amount of work this term does, so the terms can be applied in an appropriate order
	public abstract int size();
	
	//take the item/count list and apply this term to it
	public abstract Pair<int[], int[]> apply(int items[], int counts[]);

	public abstract boolean equals(Object other);

	public abstract class State {
		public abstract boolean equals(Object other);
	}
	//do any early work that can be cached across multiple applications
	//pre merge the list of items.  terms will be cached, so that it
	public abstract Pair<int[], State> preapply(int[] items);
}
