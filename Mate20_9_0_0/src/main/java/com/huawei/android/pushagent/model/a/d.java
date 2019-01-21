package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.c.b;
import com.huawei.android.pushagent.model.c.c;
import com.huawei.android.pushagent.model.c.f;
import com.huawei.android.pushagent.model.prefs.a;

public class d extends a {
    public d(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected boolean es(Context context) {
        if (1 != a.ff(context).fz() || (ez(context) ^ 1) == 0 || (f.ql(context, this.pkgName, String.valueOf(this.cg)) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + " is not registed,user id is " + this.cg);
        return false;
    }

    protected boolean er(Context context) {
        if (1 != a.ff(context).fz() || (ez(context) ^ 1) == 0 || (f.qm(context, this.pkgName, String.valueOf(this.cg), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + " token is not equal,user id is " + this.cg);
        return false;
    }

    protected boolean eo(Context context) {
        return true;
    }

    protected boolean eq(Context context) {
        return true;
    }

    protected boolean en(Context context) {
        return true;
    }

    protected void eu(Context context) {
        if (b.pa().pg(context, this.pkgName, 0)) {
            b.pa().pn(context, 0, this.cd, this.pkgName, this.tokenBytes, this.ce, this.cg);
            return;
        }
        fd(context, this.pkgName, this.tokenBytes, this.cd, this.cg, this.ce);
    }

    private void fd(Context context, String str, byte[] bArr, byte[] bArr2, int i, String str2) {
        if (com.huawei.android.pushagent.utils.tools.b.sf()) {
            com.huawei.android.pushagent.utils.tools.b.sg(2, 180);
        } else {
            com.huawei.android.pushagent.utils.d.ym(2, 180);
        }
        Intent intent = new Intent("com.huawei.android.push.intent.RECEIVE");
        intent.setPackage(str).putExtra("msg_data", bArr2).putExtra("device_token", bArr).putExtra("msgIdStr", com.huawei.android.pushagent.utils.e.a.vu(str2)).setFlags(32);
        c.po().pr(str, str2);
        com.huawei.android.pushagent.utils.d.zq(context, intent, i);
        com.huawei.android.pushagent.utils.tools.a.sa(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), a.ff(context).gu());
    }
}
