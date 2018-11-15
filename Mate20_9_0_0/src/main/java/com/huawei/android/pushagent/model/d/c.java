package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.b.b;
import com.huawei.android.pushagent.model.prefs.d;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.g;
import java.io.UnsupportedEncodingException;

public class c extends a {
    public c(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected void ze() {
        if (-1 == this.ho) {
            this.ho = a.fb();
        }
    }

    protected boolean zd(Context context) {
        if (1 != k.rh(context).sw() || (zj(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.b.a.wy(context, this.pkgName, String.valueOf(this.ho)) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", this.pkgName + "'s token is not exist in current user");
        return false;
    }

    protected boolean zc(Context context) {
        if (1 != k.rh(context).sw() || (zj(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.b.a.xb(context, this.pkgName, String.valueOf(this.ho), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", this.pkgName + "'s token is not equal in current user");
        return false;
    }

    protected boolean yz(Context context) {
        if (!d.ph(context).pl(g.fs(this.pkgName, String.valueOf(this.ho)))) {
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "closePush_Notify, pkgName is " + this.pkgName);
        return false;
    }

    protected boolean zb(Context context) {
        if (g.gn(context, this.pkgName, this.ho)) {
            return true;
        }
        return false;
    }

    protected boolean yy(Context context) {
        if (g.gn(context, "com.huawei.android.pushagent", this.ho)) {
            return true;
        }
        return false;
    }

    protected void zf(Context context) {
        if (b.xd().xj(context, this.pkgName, 2)) {
            b.xd().xq(context, 2, this.hl, this.pkgName, this.tokenBytes, this.hm, this.ho);
            return;
        }
        zm(context, this.pkgName, this.tokenBytes, this.hl, this.ho);
    }

    private void zm(Context context, String str, byte[] bArr, byte[] bArr2, int i) {
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        if (yy(context)) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send selfshow msg to NC to display.");
            intent.setPackage("com.huawei.android.pushagent");
            try {
                intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.a.c.o("com.huawei.android.pushagent", g.go().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", e.toString());
            }
        } else {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "NC is disable, send selfshow msg to [" + str + "] to display.");
            intent.setPackage(str);
        }
        g.gm(context, intent, i);
    }
}
