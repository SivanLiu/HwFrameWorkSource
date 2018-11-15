package com.huawei.android.pushagent.utils.tools;

import android.content.Context;
import com.huawei.android.pushagent.utils.g;

class a extends Thread {
    private Context appCtx;

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void run() {
        new com.huawei.android.pushagent.utils.f.a(this.appCtx, "push_report_cache").ea("shutdown" + System.currentTimeMillis(), String.valueOf(g.fw(this.appCtx)));
    }
}
