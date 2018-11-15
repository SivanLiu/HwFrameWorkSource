package com.huawei.android.pushagent.model.prefs;

import java.util.Comparator;
import java.util.Map.Entry;

class g implements Comparator<Entry<String, ?>> {
    g() {
    }

    /* renamed from: qq */
    public int compare(Entry<String, ?> entry, Entry<String, ?> entry2) {
        try {
            try {
                int i;
                if (Long.parseLong(((String) entry.getValue()).split("\\|")[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()]) > Long.parseLong(((String) entry2.getValue()).split("\\|")[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()])) {
                    i = 1;
                } else {
                    i = -1;
                }
                return i;
            } catch (Exception e) {
                return 1;
            }
        } catch (Exception e2) {
            return -1;
        }
    }
}
