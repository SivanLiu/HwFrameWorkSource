package com.huawei.android.pushagent.model.prefs;

import com.huawei.android.pushagent.utils.b.a;
import java.util.Comparator;
import java.util.Map.Entry;

class n implements Comparator<Entry<String, ?>> {
    n() {
    }

    /* renamed from: mu */
    public int compare(Entry<String, ?> entry, Entry<String, ?> entry2) {
        long parseLong;
        long parseLong2;
        try {
            parseLong = Long.parseLong(((String) entry.getValue()).split("\\|")[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()]);
        } catch (Exception e) {
            a.st("PushLog3414", "time1 parse exception");
            parseLong = 0;
        }
        try {
            parseLong2 = Long.parseLong(((String) entry2.getValue()).split("\\|")[HeartbeatControlSp$HeartBeatKey.LastBestHBTime.ordinal()]);
        } catch (Exception e2) {
            a.st("PushLog3414", "time2 parse exception");
            parseLong2 = 0;
        }
        return Long.compare(parseLong, parseLong2);
    }
}
