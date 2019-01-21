package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.c.b;
import com.huawei.android.pushagent.model.c.f;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.d;
import java.io.UnsupportedEncodingException;

public class c extends a {
    public c(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected void et() {
        if (-1 == this.cg) {
            this.cg = a.xy();
        }
    }

    protected boolean es(Context context) {
        int fy;
        if (2 == g.kp(context).kq()) {
            fy = com.huawei.android.pushagent.model.prefs.a.ff(context).fy();
        } else {
            fy = 0;
        }
        if (1 != fy || (ey(context) ^ 1) == 0 || (f.ql(context, this.pkgName, String.valueOf(this.cg)) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + "'s token is not exist in current user");
        return false;
    }

    protected boolean er(Context context) {
        int fy;
        if (2 == g.kp(context).kq()) {
            fy = com.huawei.android.pushagent.model.prefs.a.ff(context).fy();
        } else {
            fy = 0;
        }
        if (1 != fy || (ey(context) ^ 1) == 0 || (f.qm(context, this.pkgName, String.valueOf(this.cg), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + "'s token is not equal in current user");
        return false;
    }

    protected boolean eo(Context context) {
        if (!l.lw(context).lz(d.ys(this.pkgName, String.valueOf(this.cg)))) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "closePush_Notify, pkgName is " + this.pkgName);
        return false;
    }

    protected boolean eq(Context context) {
        if (d.yy(context, this.pkgName, this.cg)) {
            return true;
        }
        return false;
    }

    protected boolean en(Context context) {
        return true;
    }

    protected void eu(Context context) {
        if (b.pa().pg(context, this.pkgName, 1)) {
            b.pa().pn(context, 1, this.cd, "com.huawei.android.pushagent", this.tokenBytes, this.ce, this.cg);
            return;
        }
        fc(context, this.tokenBytes, this.cd, this.cg);
    }

    private void fc(Context context, byte[] bArr, byte[] bArr2, int i) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "try to send selfshow msg to NC");
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        intent.setPackage("com.huawei.android.pushagent");
        try {
            intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.e.a.vv("com.huawei.android.pushagent", d.zd().getBytes("UTF-8")));
            d.zq(context, intent, i);
        } catch (UnsupportedEncodingException e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", e.toString());
        }
    }
}
