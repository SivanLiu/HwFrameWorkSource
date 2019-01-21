package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.datatype.a.b;
import com.huawei.android.pushagent.utils.d;
import java.util.ArrayList;
import java.util.Iterator;

public class c {
    private static c fp = new c();
    private ArrayList<b> fo = new ArrayList();

    private c() {
    }

    public static c po() {
        return fp;
    }

    public void pr(String str, String str2) {
        b bVar = new b();
        bVar.setPkgName(str);
        bVar.ar(str2);
        bVar.aq(System.currentTimeMillis());
        ps(bVar);
    }

    private void ps(b bVar) {
        if (this.fo.size() >= 50) {
            this.fo.remove(0);
        }
        this.fo.add(bVar);
    }

    public void pp(Context context, String str) {
        Iterator it = this.fo.iterator();
        while (it.hasNext()) {
            if (str.equals(((b) it.next()).ao())) {
                a.abc(90, str);
                it.remove();
                break;
            }
        }
        if (this.fo.isEmpty()) {
            com.huawei.android.pushagent.utils.tools.a.sc(context, "com.huawei.android.push.intent.MSG_RSP_TIMEOUT");
        }
    }

    public void pq(Context context) {
        long currentTimeMillis = System.currentTimeMillis();
        long gu = com.huawei.android.pushagent.model.prefs.a.ff(context).gu();
        Iterator it = this.fo.iterator();
        while (it.hasNext()) {
            b bVar = (b) it.next();
            if (currentTimeMillis - bVar.an() > gu) {
                String ao = bVar.ao();
                if (d.zk(context, bVar.ap())) {
                    a.abc(92, ao);
                } else {
                    a.abc(91, ao);
                }
                it.remove();
            }
        }
        if (!this.fo.isEmpty()) {
            com.huawei.android.pushagent.utils.tools.a.sa(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), com.huawei.android.pushagent.model.prefs.a.ff(context).gu());
        }
    }
}
