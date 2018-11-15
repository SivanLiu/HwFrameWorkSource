package com.android.server.hidata.wavemapping.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CommUtil {
    public static int getModalNums(int[] arr) {
        int n = arr.length;
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return arr[0];
        }
        Map<Integer, Integer> freqMap = new HashMap();
        for (int i = 0; i < n; i++) {
            Integer v = (Integer) freqMap.get(Integer.valueOf(arr[i]));
            freqMap.put(Integer.valueOf(arr[i]), Integer.valueOf(v == null ? 1 : v.intValue() + 1));
        }
        List<Entry<Integer, Integer>> entries = new ArrayList(freqMap.entrySet());
        Collections.sort(entries, new Comparator<Entry<Integer, Integer>>() {
            public int compare(Entry<Integer, Integer> e1, Entry<Integer, Integer> e2) {
                return ((Integer) e2.getValue()).intValue() - ((Integer) e1.getValue()).intValue();
            }
        });
        return ((Integer) ((Entry) entries.get(0)).getKey()).intValue();
    }
}
