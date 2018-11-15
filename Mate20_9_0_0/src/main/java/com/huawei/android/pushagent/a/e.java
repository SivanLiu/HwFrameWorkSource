package com.huawei.android.pushagent.a;

import com.huawei.android.pushagent.utils.f.a;
import com.huawei.android.pushagent.utils.f.c;
import java.util.Map;

final class e implements Runnable {
    e() {
    }

    public void run() {
        if (a.appCtx == null || a.bs == null) {
            c.eq("PushLog3413", "Please init reporter first");
            return;
        }
        a aVar = new a(a.appCtx, "push_report_cache");
        Map all = aVar.getAll();
        if (all != null && all.size() > 0) {
            for (String str : all.keySet()) {
                if (str.startsWith("shutdown")) {
                    try {
                        long longValue = Long.valueOf(str.substring("shutdown".length())).longValue();
                        String str2 = (String) all.get(str);
                        if (a.hu(1)) {
                            a.bs.hf(longValue, str2, String.valueOf(1), "");
                        }
                    } catch (NumberFormatException e) {
                        c.eq("PushLog3413", "time format error");
                    }
                }
            }
            aVar.dz();
        }
    }
}
