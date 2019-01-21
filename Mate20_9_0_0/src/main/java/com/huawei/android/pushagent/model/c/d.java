package com.huawei.android.pushagent.model.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class d {
    private static d fr = new d();
    private Map<String, Integer> fq = new HashMap();

    private d() {
    }

    public static d pt() {
        return fr;
    }

    public void pv(String str) {
        if (!TextUtils.isEmpty(str)) {
            int i = 0;
            if (this.fq.containsKey(str)) {
                i = ((Integer) this.fq.get(str)).intValue();
            }
            this.fq.put(str, Integer.valueOf(i + 1));
            a.st("PushLog3414", "reportEvent cacheTokenApplyTimesList:" + this.fq);
        }
    }

    public void pu() {
        int i = 20;
        int size = this.fq.size();
        if (size <= 20) {
            i = size;
        }
        Iterator it = this.fq.entrySet().iterator();
        while (it.hasNext() && i > 0) {
            int i2 = i - 1;
            Entry entry = (Entry) it.next();
            com.huawei.android.pushagent.b.a.abc(61, com.huawei.android.pushagent.b.a.abb((String) entry.getKey(), String.valueOf(entry.getValue())));
            it.remove();
            i = i2;
        }
    }
}
