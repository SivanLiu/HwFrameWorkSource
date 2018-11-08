package com.huawei.android.pushagent.b;

import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import java.util.Map;

final class e implements Runnable {
    e() {
    }

    public void run() {
        if (a.appCtx == null || a.ir == null) {
            b.y("PushLog2976", "Please init reporter first");
            return;
        }
        c cVar = new c(a.appCtx, "push_report_cache");
        Map all = cVar.getAll();
        if (all != null && all.size() > 0) {
            for (String str : all.keySet()) {
                if (str.startsWith("shutdown")) {
                    try {
                        long longValue = Long.valueOf(str.substring("shutdown".length())).longValue();
                        String str2 = (String) all.get(str);
                        if (a.aap(1)) {
                            a.ir.zy(longValue, str2, String.valueOf(1), "");
                        }
                    } catch (NumberFormatException e) {
                        b.y("PushLog2976", "time format error");
                    }
                }
            }
            cVar.ao();
        }
    }
}
