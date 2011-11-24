package vis.data.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

public class CountAggregator {
	public static Pair<int[], int[]> filter(int a[], int a_count[], int[] f) {
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
				c_count[k++] = b_count[i++];
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
