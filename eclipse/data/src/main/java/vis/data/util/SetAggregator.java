package vis.data.util;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

public class SetAggregator {
	public static void sort(int a[]) {
		Arrays.sort(a);
	}
	public static int[] remove(int a[], int b[]) {
		int c[] = new int[a.length];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] == b[j])
				i++;
			else if(a[i] < b[j])
				c[k++] = a[i++];
			else
				j++;
		}
		while(i < a.length)
			c[k++] = a[i++];
		
		if(c.length != k)
			c = ArrayUtils.subarray(c, 0, k);
		return c;
	}
	public static int[] filter(int a[], int f[]) {
		return and(a, f);
	}
	public static int[] or(int a[], int b[]) {
		int c[] = new int[a.length + b.length];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] < b[j])
				c[k++] = a[i++];
			else
				c[k++] = b[j++];
		}
		while(i < a.length)
			c[k++] = a[i++];
		while(j < b.length)
			c[k++] = b[j++];
		
		if(c.length != k)
			c = ArrayUtils.subarray(c, 0, k);
		return c;
	}
	public static int[] and(int a[], int b[]) {
		int c[] = new int[Math.min(a.length, b.length)];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] == b[j])
				c[k++] = a[i++];
			else if(a[i] < b[j])
				i++;
			else
				j++;
		}
		
		if(c.length != k)
			c = ArrayUtils.subarray(c, 0, k);
		return c;
	}
}
