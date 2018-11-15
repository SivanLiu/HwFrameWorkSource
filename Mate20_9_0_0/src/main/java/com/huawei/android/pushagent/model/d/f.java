package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.d;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.UnsupportedEncodingException;

public class f extends a {
    public f(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected void ze() {
        if (-1 == this.ho) {
            this.ho = a.fb();
        }
    }

    protected boolean zd(Context context) {
        int tr;
        if (2 == b.oq(context).or()) {
            tr = k.rh(context).tr();
        } else {
            tr = 0;
        }
        if (1 != tr || (zj(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.b.a.wy(context, this.pkgName, String.valueOf(this.ho)) ^ 1) == 0) {
            return true;
        }
        c.ep("PushLog3413", this.pkgName + "'s token is not exist in current user");
        return false;
    }

    protected boolean zc(Context context) {
        int tr;
        if (2 == b.oq(context).or()) {
            tr = k.rh(context).tr();
        } else {
            tr = 0;
        }
        if (1 != tr || (zj(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.b.a.xb(context, this.pkgName, String.valueOf(this.ho), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        c.ep("PushLog3413", this.pkgName + "'s token is not equal in current user");
        return false;
    }

    protected boolean yz(Context context) {
        if (!d.ph(context).pl(g.fs(this.pkgName, String.valueOf(this.ho)))) {
            return true;
        }
        c.er("PushLog3413", "closePush_Notify, pkgName is " + this.pkgName);
        return false;
    }

    protected boolean zb(Context context) {
        if (g.gn(context, this.pkgName, this.ho)) {
            return true;
        }
        return false;
    }

    protected boolean yy(Context context) {
        return true;
    }

    protected void zf(Context context) {
        if (com.huawei.android.pushagent.model.b.b.xd().xj(context, this.pkgName, 1)) {
            com.huawei.android.pushagent.model.b.b.xd().xq(context, 1, this.hl, "com.huawei.android.pushagent", this.tokenBytes, this.hm, this.ho);
            return;
        }
        zp(context, this.tokenBytes, this.hl, this.ho);
    }

    private void zp(Context context, byte[] bArr, byte[] bArr2, int i) {
        c.er("PushLog3413", "try to send selfshow msg to NC");
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        intent.setPackage("com.huawei.android.pushagent");
        try {
            intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.a.c.o("com.huawei.android.pushagent", g.go().getBytes("UTF-8")));
            g.gm(context, intent, i);
        } catch (UnsupportedEncodingException e) {
            c.eq("PushLog3413", e.toString());
        }
    }
}
