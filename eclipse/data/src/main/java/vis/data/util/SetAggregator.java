package vis.data.util;

import org.apache.commons.lang3.ArrayUtils;

public class SetAggregator {
	int[] or(int a[], int b[]) {
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
	int[] and(int a[], int b[]) {
		int c[] = new int[Math.min(a.length, b.length)];
		int i = 0, j = 0, k = 0;
		for(; i < a.length && j < b.length;) {
			if(a[i] == b[j])
				c[k++] = a[i];
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
