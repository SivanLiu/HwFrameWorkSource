package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.b.a;
import com.huawei.android.pushagent.model.b.b;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;

public class e extends a {
    public e(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected boolean zd(Context context) {
        if (1 != k.rh(context).sw() || (zk(context) ^ 1) == 0 || (a.wy(context, this.pkgName, String.valueOf(this.ho)) ^ 1) == 0) {
            return true;
        }
        c.ep("PushLog3413", this.pkgName + " is not registed,user id is " + this.ho);
        return false;
    }

    protected boolean zc(Context context) {
        if (1 != k.rh(context).sw() || (zk(context) ^ 1) == 0 || (a.xb(context, this.pkgName, String.valueOf(this.ho), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        c.ep("PushLog3413", this.pkgName + " token is not equal,user id is " + this.ho);
        return false;
    }

    protected boolean yz(Context context) {
        return true;
    }

    protected boolean zb(Context context) {
        return true;
    }

    protected boolean yy(Context context) {
        return true;
    }

    protected void zf(Context context) {
        if (b.xd().xj(context, this.pkgName, 0)) {
            b.xd().xq(context, 0, this.hl, this.pkgName, this.tokenBytes, this.hm, this.ho);
            return;
        }
        zo(context, this.pkgName, this.tokenBytes, this.hl, this.ho, this.hm);
    }

    private void zo(Context context, String str, byte[] bArr, byte[] bArr2, int i, String str2) {
        if (com.huawei.android.pushagent.utils.tools.c.cr()) {
            com.huawei.android.pushagent.utils.tools.c.ct(2, 180);
        } else {
            g.ge(2, 180);
        }
        Intent intent = new Intent("com.huawei.android.push.intent.RECEIVE");
        intent.setPackage(str).putExtra("msg_data", bArr2).putExtra("device_token", bArr).putExtra("msgIdStr", com.huawei.android.pushagent.utils.a.c.k(str2)).setFlags(32);
        com.huawei.android.pushagent.model.b.c.xr().xu(str, str2);
        g.gm(context, intent, i);
        d.cw(context, new Intent("com.huawei.android.push.intent.MSG_RSP_TIMEOUT").setPackage(context.getPackageName()), k.rh(context).se());
    }
}
