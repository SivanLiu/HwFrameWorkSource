package com.android.server.broadcastradio;

import android.util.Slog;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;

class Convert {
    private static final String TAG = "BroadcastRadioService.Convert";

    Convert() {
    }

    static String[][] stringMapToNative(Map<String, String> map) {
        if (map == null) {
            Slog.v(TAG, "map is null, returning zero-elements array");
            return (String[][]) Array.newInstance(String.class, new int[]{0, 0});
        }
        String[][] arr = (String[][]) Array.newInstance(String.class, new int[]{map.entrySet().size(), 2});
        int i = 0;
        for (Entry<String, String> entry : map.entrySet()) {
            arr[i][0] = (String) entry.getKey();
            arr[i][1] = (String) entry.getValue();
            i++;
        }
        Slog.v(TAG, "converted " + i + " element(s)");
        return arr;
    }
}
