package com.huawei.hsm.permission;

import android.util.Log;
import java.util.ArrayList;

public class ANRFilter {
    private static final String TAG = "ANRFilter";
    private static ANRFilter sANRFilterInstance = null;
    private ArrayList<Integer> mSkipedUidFIFO = new ArrayList();

    private ANRFilter() {
    }

    public static synchronized ANRFilter getInstance() {
        ANRFilter aNRFilter;
        synchronized (ANRFilter.class) {
            if (sANRFilterInstance == null) {
                sANRFilterInstance = new ANRFilter();
            }
            aNRFilter = sANRFilterInstance;
        }
        return aNRFilter;
    }

    public synchronized boolean addUid(int uid) {
        this.mSkipedUidFIFO.add(Integer.valueOf(uid));
        return true;
    }

    public synchronized boolean removeUid(int uid) {
        this.mSkipedUidFIFO.remove(Integer.valueOf(uid));
        return true;
    }

    public synchronized boolean checkUid(int uid) {
        boolean exist;
        exist = this.mSkipedUidFIFO.contains(Integer.valueOf(uid));
        if (exist) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exist uid:");
            stringBuilder.append(uid);
            Log.d(str, stringBuilder.toString());
        }
        return exist;
    }
}
