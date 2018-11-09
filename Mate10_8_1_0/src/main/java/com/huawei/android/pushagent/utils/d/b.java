package com.huawei.android.pushagent.utils.d;

import android.content.Context;
import java.util.HashMap;

final class b implements Runnable {
    final /* synthetic */ Context af;
    final /* synthetic */ com.huawei.android.pushagent.model.flowcontrol.a.b ag;
    final /* synthetic */ int ah;
    final /* synthetic */ HashMap ai;

    b(Context context, com.huawei.android.pushagent.model.flowcontrol.a.b bVar, int i, HashMap hashMap) {
        this.af = context;
        this.ag = bVar;
        this.ah = i;
        this.ai = hashMap;
    }

    public void run() {
        a.cx(this.af, this.ag, this.ah, this.ai);
    }
}
