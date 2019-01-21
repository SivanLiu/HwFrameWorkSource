package com.huawei.android.pushagent.b;

import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.b;
import java.util.Map;

final class e implements Runnable {
    e() {
    }

    public void run() {
        if (a.appCtx == null || a.iw == null) {
            a.su("PushLog3414", "Please init reporter first");
            return;
        }
        b bVar = new b(a.appCtx, "push_report_cache");
        Map all = bVar.getAll();
        if (all != null && all.size() > 0) {
            for (String str : all.keySet()) {
                if (str.startsWith("shutdown")) {
                    try {
                        long longValue = Long.valueOf(str.substring("shutdown".length())).longValue();
                        String str2 = (String) all.get(str);
                        if (a.abh(1)) {
                            a.iw.aaz(longValue, str2, String.valueOf(1), "");
                        }
                    } catch (NumberFormatException e) {
                        a.su("PushLog3414", "time format error");
                    }
                }
            }
            bVar.tj();
        }
    }
}
