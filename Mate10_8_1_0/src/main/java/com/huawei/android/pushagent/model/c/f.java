package com.huawei.android.pushagent.model.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.utils.a.b;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class f {
    private static f es = new f();
    private Map<String, Integer> er = new HashMap();

    private f() {
    }

    public static f tk() {
        return es;
    }

    public void tm(String str) {
        if (!TextUtils.isEmpty(str)) {
            int i = 0;
            if (this.er.containsKey(str)) {
                i = ((Integer) this.er.get(str)).intValue();
            }
            this.er.put(str, Integer.valueOf(i + 1));
            b.x("PushLog2976", "reportEvent cacheTokenApplyTimesList:" + this.er);
        }
    }

    public void tl() {
        int i = 20;
        int size = this.er.size();
        if (size <= 20) {
            i = size;
        }
        Iterator it = this.er.entrySet().iterator();
        while (it.hasNext() && i > 0) {
            int i2 = i - 1;
            Entry entry = (Entry) it.next();
            a.aaj(61, a.aal((String) entry.getKey(), String.valueOf(entry.getValue())));
            it.remove();
            i = i2;
        }
    }
}
