package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.prefs.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.f;
import java.io.UnsupportedEncodingException;

public class b extends f {
    public b(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected void rj() {
        if (-1 == this.dy) {
            this.dy = a.fc();
        }
    }

    protected boolean ri(Context context) {
        int mu;
        if (2 == com.huawei.android.pushagent.model.prefs.b.kp(context).km()) {
            mu = i.mj(context).mu();
        } else {
            mu = 0;
        }
        if (1 != mu || (rp(context) ^ 1) == 0 || (e.tf(context, this.pkgName, String.valueOf(this.dy)) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", this.pkgName + "'s token is not exist in current user");
        return false;
    }

    protected boolean rh(Context context) {
        int mu;
        if (2 == com.huawei.android.pushagent.model.prefs.b.kp(context).km()) {
            mu = i.mj(context).mu();
        } else {
            mu = 0;
        }
        if (1 != mu || (rp(context) ^ 1) == 0 || (e.tg(context, this.pkgName, String.valueOf(this.dy), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", this.pkgName + "'s token is not equal in current user");
        return false;
    }

    protected boolean rf(Context context) {
        if (!c.kw(context).kx(f.fz(this.pkgName, String.valueOf(this.dy)))) {
            return true;
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "closePush_Notify, pkgName is " + this.pkgName);
        return false;
    }

    protected boolean rg(Context context) {
        if (f.fx(context, this.pkgName, this.dy)) {
            return true;
        }
        return false;
    }

    protected boolean re(Context context) {
        return true;
    }

    protected void rk(Context context) {
        if (com.huawei.android.pushagent.model.c.b.sc().sb(context, this.pkgName, 1)) {
            com.huawei.android.pushagent.model.c.b.sc().so(context, 1, this.dz, "com.huawei.android.pushagent", this.tokenBytes, this.ea, this.dy);
            return;
        }
        rl(context, this.tokenBytes, this.dz, this.dy);
    }

    private void rl(Context context, byte[] bArr, byte[] bArr2, int i) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "try to send selfshow msg to NC");
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        intent.setPackage("com.huawei.android.pushagent");
        try {
            intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.c.c.bd("com.huawei.android.pushagent", f.fy().getBytes("UTF-8")));
            f.fw(context, intent, i);
        } catch (UnsupportedEncodingException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e.toString());
        }
    }
}
