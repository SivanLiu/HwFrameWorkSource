package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.b.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.d;
import java.util.ArrayList;
import java.util.Iterator;

public class a {
    private static a ed = new a();
    private ArrayList<c> ec = new ArrayList();

    private a() {
    }

    public static a ry() {
        return ed;
    }

    public void rx(String str, String str2) {
        c cVar = new c();
        cVar.setPkgName(str);
        cVar.ye(str2);
        cVar.yf(System.currentTimeMillis());
        rw(cVar);
    }

    private void rw(c cVar) {
        if (this.ec.size() >= 50) {
            this.ec.remove(0);
        }
        this.ec.add(cVar);
    }

    public void rz(Context context, String str) {
        Iterator it = this.ec.iterator();
        while (it.hasNext()) {
            if (str.equals(((c) it.next()).yg())) {
                com.huawei.android.pushagent.b.a.aaj(90, str);
                it.remove();
                break;
            }
        }
        if (this.ec.isEmpty()) {
            d.o(context, "com.huawei.android.push.intent.MSG_RSP_TIMEOUT");
        }
    }

    public void sa(Context context) {
        long currentTimeMillis = System.currentTimeMillis();
        long ms = i.mj(context).ms();
        Iterator it = this.ec.iterator();
        while (it.hasNext()) {
            c cVar = (c) it.next();
            if (currentTimeMillis - cVar.yh() > ms) {
                String yg = cVar.yg();
                if (f.fq(context, cVar.yi())) {
                    com.huawei.android.pushagent.b.a.aaj(92, yg);
                } else {
                    com.huawei.android.pushagent.b.a.aaj(91, yg);
                }
                it.remove();
            }
        }
        if (!this.ec.isEmpty()) {
            d.p(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), i.mj(context).ms());
        }
    }
}
