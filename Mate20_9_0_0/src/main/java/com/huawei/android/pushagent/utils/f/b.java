package com.huawei.android.pushagent.utils.f;

import android.content.Context;
import java.util.HashMap;

final class b implements Runnable {
    final /* synthetic */ Context ib;
    final /* synthetic */ com.huawei.android.pushagent.model.flowcontrol.a.b ic;
    final /* synthetic */ int id;
    final /* synthetic */ HashMap ie;

    b(Context context, com.huawei.android.pushagent.model.flowcontrol.a.b bVar, int i, HashMap hashMap) {
        this.ib = context;
        this.ic = bVar;
        this.id = i;
        this.ie = hashMap;
    }

    public void run() {
        a.xu(this.ib, this.ic, this.id, this.ie);
    }
}
