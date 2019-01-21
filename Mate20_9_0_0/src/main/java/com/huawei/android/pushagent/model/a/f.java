package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.c.b;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.d;
import java.io.UnsupportedEncodingException;

public class f extends a {
    public f(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected void et() {
        if (-1 == this.cg) {
            this.cg = a.xy();
        }
    }

    protected boolean es(Context context) {
        if (1 != com.huawei.android.pushagent.model.prefs.a.ff(context).fz() || (ey(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.c.f.ql(context, this.pkgName, String.valueOf(this.cg)) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + "'s token is not exist in current user");
        return false;
    }

    protected boolean er(Context context) {
        if (1 != com.huawei.android.pushagent.model.prefs.a.ff(context).fz() || (ey(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.c.f.qm(context, this.pkgName, String.valueOf(this.cg), this.tokenBytes) ^ 1) == 0) {
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
        if (d.yy(context, "com.huawei.android.pushagent", this.cg)) {
            return true;
        }
        return false;
    }

    protected void eu(Context context) {
        if (b.pa().pg(context, this.pkgName, 2)) {
            b.pa().pn(context, 2, this.cd, this.pkgName, this.tokenBytes, this.ce, this.cg);
            return;
        }
        fe(context, this.pkgName, this.tokenBytes, this.cd, this.cg);
    }

    private void fe(Context context, String str, byte[] bArr, byte[] bArr2, int i) {
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        if (en(context)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send selfshow msg to NC to display.");
            intent.setPackage("com.huawei.android.pushagent");
            try {
                intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.e.a.vv("com.huawei.android.pushagent", d.zd().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", e.toString());
            }
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "NC is disable, send selfshow msg to [" + str + "] to display.");
            intent.setPackage(str);
        }
        d.zq(context, intent, i);
    }
}
