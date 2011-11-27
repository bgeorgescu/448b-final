package vis.data.util;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

public class CountAggregator {
	public static void sort(int a[], int a_count[]) {
		assert(a.length == a_count.length);
		TreeMap<Integer, Integer> v = new TreeMap<Integer, Integer>();
		for(int i = 0; i < a.length; ++i) {
			v.put(a[i], a_count[i]);
		}
		int i = 0;
		for(Entry<Integer, Integer> e : v.entrySet()) {
			a[i] = e.getKey();
			a_count[i] = e.getValue();
			++i;
		}
	}	
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
		while(i < a.length)
			c[k++] = a[i++];
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}
	public static Pair<int[], int[]> filter(int a[], int a_count[], int[] f) {
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
		while(i < a.length)
			c[k++] = a[i++];
		while(j < b.length)
			c[k++] = b[j++];
		
		if(c.length != k) {
			c = ArrayUtils.subarray(c, 0, k);
			c_count = ArrayUtils.subarray(c_count, 0, k);
		}
		return Pair.of(c, c_count);
	}
	public static Pair<int[], int[]> and(int a[], int a_count[], int b[], int b_count[]) {
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

}
