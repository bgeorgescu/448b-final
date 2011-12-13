package vis.data.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

public class CountAggregator {
	public static void sortByIdAsc(int a[], int a_count[]) {
		assert(a.length == a_count.length);
		class CountTuple implements Comparable<CountTuple>{ 
			int a, count;

			@Override
			public int compareTo(CountTuple o) {
				if(a < o.a)
					return -1;
				if(a > o.a)
					return 1;
				if(count > o.count)
					return -1;
				if(count < o.count)
					return 1;
				return 0;
			}
		}
		ArrayList<CountTuple> act = new ArrayList<CountTuple>(a.length);
		for(int i = 0; i < a.length; ++i) {
			CountTuple ct = new CountTuple();
			ct.a = a[i];
			ct.count = a_count[i];
			act.add(ct);
		}
		Collections.sort(act);
		int i = 0;
		for(CountTuple e : act) {
			a[i] = e.a;
			a_count[i] = e.count;
			++i;
		}
	}	
	public static void sortByCountDesc(int a[], int a_count[]) {
		assert(a.length == a_count.length);
		class CountTuple implements Comparable<CountTuple>{ 
			int a, count;

			@Override
			public int compareTo(CountTuple o) {
				if(count > o.count)
					return -1;
				if(count < o.count)
					return 1;
				if(a < o.a)
					return -1;
				if(a > o.a)
					return 1;
				return 0;
			}
		}
		ArrayList<CountTuple> act = new ArrayList<CountTuple>(a.length);
		for(int i = 0; i < a.length; ++i) {
			CountTuple ct = new CountTuple();
			ct.a = a[i];
			ct.count = a_count[i];
			act.add(ct);
		}
		Collections.sort(act);
		int i = 0;
		for(CountTuple e : act) {
			a[i] = e.a;
			a_count[i] = e.count;
			++i;
		}
	}	
	//TODO:big/small size mismatch optimized version
	public static Pair<int[], int[]> remove(int a[], int a_count[], int[] f) {
		assert(a.length == a_count.length);
		int max_size = a.length;
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < f.length;) {
			if(a[i] == f[j]) {
				i++;
			} else if(a[i] < f[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++];
			} else {
				j++;
			}
		}
		while(i < a.length) {
			c[k] = a[i];
			c_count[k++] = a_count[i++];
		}
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}
	//TODO:ratio should include something about the overlap of the ranges too
	static final int MIN_SPAN_RATIO = 16;
	public static Pair<int[], int[]> filter(int a[], int a_count[], int[] f) {
		int min_length = Math.min(a.length, f.length);
		if(min_length == 0)
			return Pair.of(new int[0], new int[0]);
//		int a_span = a[a.length - 1] - a[0];
//		int f_span = f[f.length - 1] - f[0];
//		int a_rat = a_span / a.length;
//		int f_rat = f_span / f.length;
//		if(a_rat >  MIN_SPAN_RATIO * f_rat || f_rat > MIN_SPAN_RATIO * a_rat)
//			return filterBig(a, a_count, f);
		return filterSmall(a, a_count, f);
	}
	public static Pair<int[], int[]> filterSmall(int a[], int a_count[], int[] f) {
		assert(a.length == a_count.length);
		int max_size = a.length;
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < f.length;) {
			if(a[i] == f[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++];
			} else if(a[i] < f[j]) {
				i++;
			} else {
				j++;
			}
		}
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}
	public static Pair<int[], int[]> filterBig(int a[], int a_count[], int[] f) {
		assert(a.length == a_count.length);
		int max_size = a.length;
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < f.length;) {
			if(a[i] == f[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++];
			} else if(a[i] < f[j]) {
				int pos = Arrays.binarySearch(a, f[j]);
				if(pos >= 0) {
					i = pos;
				} else {
					i = -(pos + 1);
				}
			} else if(a[i] > f[j]){
				int pos = Arrays.binarySearch(f, a[i]);
				if(pos >= 0) {
					j = pos;
				} else {
					j = -(pos + 1);
				}
			}

		}
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}	
	public static Pair<int[], int[]> or(int a[], int a_count[], int b[], int b_count[]) {
		assert(a.length == a_count.length);
		assert(b.length == b_count.length);
		int max_size = a.length + b.length;
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] == b[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++] + b_count[j++];
			} else if(a[i] < b[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++];
			} else {
				c[k] = b[j];
				c_count[k++] = b_count[j++];
			}
		}
		while(i < a.length) {
			c[k] = a[i];
			c_count[k++] = a_count[i++];
		}
		while(j < b.length) {
			c[k] = b[j];
			c_count[k++] = b_count[j++];
		}
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}
	public static Pair<int[], int[]> and(int a[], int a_count[], int[] b, int b_count[]) {
		int min_length = Math.min(a.length, b.length);
		if(min_length == 0)
			return Pair.of(new int[0], new int[0]);
//		int a_span = a[a.length - 1] - a[0];
//		int f_span = b[b.length - 1] - b[0];
//		int a_rat = a_span / a.length;
//		int f_rat = f_span / b.length;
//		if(a_rat >  MIN_SPAN_RATIO * f_rat || f_rat > MIN_SPAN_RATIO * a_rat)
//			return andBig(a, a_count, b, b_count);
		return andSmall(a, a_count, b, b_count);
	}	
	public static Pair<int[], int[]> andSmall(int a[], int a_count[], int b[], int b_count[]) {
		assert(a.length == a_count.length);
		assert(b.length == b_count.length);
		int max_size = Math.min(a.length, b.length);
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] == b[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++] + b_count[j++];
			} else if(a[i] < b[j]) {
				i++;
			} else {
				j++;
			}
		}
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}

	public static Pair<int[], int[]> andBig(int a[], int a_count[], int b[], int b_count[]) {
		assert(a.length == a_count.length);
		assert(b.length == b_count.length);
		int max_size = Math.min(a.length, b.length);
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] == b[j]) {
				c[k] = a[i];
				c_count[k++] = a_count[i++] + b_count[j++];
			} else if(a[i] < b[j]) {
				int pos = Arrays.binarySearch(a, b[j]);
				if(pos >= 0) {
					i = pos;
				} else {
					i = -(pos + 1);
				}
			} else if(a[i] > b[j]){
				int pos = Arrays.binarySearch(b, a[i]);
				if(pos >= 0) {
					j = pos;
				} else {
					j = -(pos + 1);
				}
			}
		}
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}
	public static Pair<int[], int[]> threshold(int[] a, int[] a_count, int min) {
		assert(a.length == a_count.length);
		int max_size = a.length;
		int c[] = new int[max_size];
		int c_count[] = new int[max_size];
		int i = 0, j = 0;
		for(; i < a.length; ++i) {
			if(a_count[i] > min) {
				c[j] = a[i];
				c_count[j++] = a_count[i];
			}
		}
		if(c.length != j) {
			c = ArrayUtils.subarray(c, 0, j);
			c_count = ArrayUtils.subarray(c_count, 0, j);
		}
		return Pair.of(c, c_count);
	}

}
