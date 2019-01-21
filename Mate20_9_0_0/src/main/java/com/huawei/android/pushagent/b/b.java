package com.huawei.android.pushagent.b;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.a;

final class b implements Runnable {
    final /* synthetic */ Context ix;

    b(Context context) {
        this.ix = context;
    }

    public void run() {
        if (this.ix == null) {
            a.su("PushLog3414", "init reporter failed, context is null");
            return;
        }
        a.appCtx = this.ix.getApplicationContext();
        a.iw = new com.huawei.android.pushagent.b.a.a();
        a.iw.aax(this.ix);
    }
}
