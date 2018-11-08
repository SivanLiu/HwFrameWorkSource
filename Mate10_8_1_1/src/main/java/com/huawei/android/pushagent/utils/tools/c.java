package com.huawei.android.pushagent.utils.tools;

import android.content.Context;
import com.huawei.android.pushagent.utils.f;

class c extends Thread {
    private Context appCtx;

    public c(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void run() {
        new com.huawei.android.pushagent.utils.a.c(this.appCtx, "push_report_cache").am("shutdown" + System.currentTimeMillis(), String.valueOf(f.fp(this.appCtx)));
    }
}
