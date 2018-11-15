package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.a.a;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;
import java.util.ArrayList;
import java.util.Iterator;

public class c {
    private static c gx = new c();
    private ArrayList<com.huawei.android.pushagent.datatype.a.c> gw = new ArrayList();

    private c() {
    }

    public static c xr() {
        return gx;
    }

    public void xu(String str, String str2) {
        com.huawei.android.pushagent.datatype.a.c cVar = new com.huawei.android.pushagent.datatype.a.c();
        cVar.setPkgName(str);
        cVar.kk(str2);
        cVar.kl(System.currentTimeMillis());
        xv(cVar);
    }

    private void xv(com.huawei.android.pushagent.datatype.a.c cVar) {
        if (this.gw.size() >= 50) {
            this.gw.remove(0);
        }
        this.gw.add(cVar);
    }

    public void xs(Context context, String str) {
        Iterator it = this.gw.iterator();
        while (it.hasNext()) {
            if (str.equals(((com.huawei.android.pushagent.datatype.a.c) it.next()).km())) {
                a.hq(90, str);
                it.remove();
                break;
            }
        }
        if (this.gw.isEmpty()) {
            d.cy(context, "com.huawei.android.push.intent.MSG_RSP_TIMEOUT");
        }
    }

    public void xt(Context context) {
        long currentTimeMillis = System.currentTimeMillis();
        long se = k.rh(context).se();
        Iterator it = this.gw.iterator();
        while (it.hasNext()) {
            com.huawei.android.pushagent.datatype.a.c cVar = (com.huawei.android.pushagent.datatype.a.c) it.next();
            if (currentTimeMillis - cVar.kn() > se) {
                String km = cVar.km();
                if (g.gu(context, cVar.ko())) {
                    a.hq(92, km);
                } else {
                    a.hq(91, km);
                }
                it.remove();
            }
        }
        if (!this.gw.isEmpty()) {
            d.cw(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), k.rh(context).se());
        }
    }
}
