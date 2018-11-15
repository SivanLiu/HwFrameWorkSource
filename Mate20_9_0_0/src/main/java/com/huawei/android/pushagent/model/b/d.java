package com.huawei.android.pushagent.model.b;

import android.text.TextUtils;
import com.huawei.android.pushagent.a.a;
import com.huawei.android.pushagent.utils.f.c;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class d {
    private static d gz = new d();
    private Map<String, Integer> gy = new HashMap();

    private d() {
    }

    public static d xw() {
        return gz;
    }

    public void xy(String str) {
        if (!TextUtils.isEmpty(str)) {
            int i = 0;
            if (this.gy.containsKey(str)) {
                i = ((Integer) this.gy.get(str)).intValue();
            }
            this.gy.put(str, Integer.valueOf(i + 1));
            c.er("PushLog3413", "reportEvent cacheTokenApplyTimesList:" + this.gy);
        }
    }

    public void xx() {
        int i = 20;
        int size = this.gy.size();
        if (size <= 20) {
            i = size;
        }
        Iterator it = this.gy.entrySet().iterator();
        while (it.hasNext() && i > 0) {
            int i2 = i - 1;
            Entry entry = (Entry) it.next();
            a.hq(61, a.hr((String) entry.getKey(), String.valueOf(entry.getValue())));
            it.remove();
            i = i2;
        }
    }
}
