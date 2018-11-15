package com.huawei.android.pushagent.utils.e;

import android.content.Context;
import java.util.HashMap;

final class b implements Runnable {
    final /* synthetic */ Context am;
    final /* synthetic */ com.huawei.android.pushagent.model.flowcontrol.a.b an;
    final /* synthetic */ int ao;
    final /* synthetic */ HashMap ap;

    b(Context context, com.huawei.android.pushagent.model.flowcontrol.a.b bVar, int i, HashMap hashMap) {
        this.am = context;
        this.an = bVar;
        this.ao = i;
        this.ap = hashMap;
    }

    public void run() {
        a.dd(this.am, this.an, this.ao, this.ap);
    }
}
