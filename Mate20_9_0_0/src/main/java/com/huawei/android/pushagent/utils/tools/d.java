package com.huawei.android.pushagent.utils.tools;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.b;

class d extends Thread {
    private Context appCtx;

    public d(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void run() {
        new b(this.appCtx, "push_report_cache").th("shutdown" + System.currentTimeMillis(), String.valueOf(com.huawei.android.pushagent.utils.d.yh(this.appCtx)));
    }
}
