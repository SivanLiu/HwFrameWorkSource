package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.a;
import com.huawei.android.pushagent.utils.tools.d;

public class c extends f {
    public c(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected boolean ri(Context context) {
        if (1 != i.mj(context).oe() || (rq(context) ^ 1) == 0 || (e.tf(context, this.pkgName, String.valueOf(this.dy)) ^ 1) == 0) {
            return true;
        }
        b.z("PushLog2976", this.pkgName + " is not registed,user id is " + this.dy);
        return false;
    }

    protected boolean rh(Context context) {
        if (1 != i.mj(context).oe() || (rq(context) ^ 1) == 0 || (e.tg(context, this.pkgName, String.valueOf(this.dy), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        b.z("PushLog2976", this.pkgName + " token is not equal,user id is " + this.dy);
        return false;
    }

    protected boolean rf(Context context) {
        return true;
    }

    protected boolean rg(Context context) {
        return true;
    }

    protected boolean re(Context context) {
        return true;
    }

    protected void rk(Context context) {
        if (com.huawei.android.pushagent.model.c.b.sc().sb(context, this.pkgName, 0)) {
            com.huawei.android.pushagent.model.c.b.sc().so(context, 0, this.dz, this.pkgName, this.tokenBytes, this.ea, this.dy);
            return;
        }
        rm(context, this.pkgName, this.tokenBytes, this.dz, this.dy, this.ea);
    }

    private void rm(Context context, String str, byte[] bArr, byte[] bArr2, int i, String str2) {
        if (a.j()) {
            a.g(2, 180);
        } else {
            f.fv(2, 180);
        }
        Intent intent = new Intent("com.huawei.android.push.intent.RECEIVE");
        intent.setPackage(str).putExtra("msg_data", bArr2).putExtra("device_token", bArr).putExtra("msgIdStr", com.huawei.android.pushagent.utils.c.c.bc(str2)).setFlags(32);
        com.huawei.android.pushagent.model.c.a.ry().rx(str, str2);
        f.fw(context, intent, i);
        d.p(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), i.mj(context).ms());
    }
}
