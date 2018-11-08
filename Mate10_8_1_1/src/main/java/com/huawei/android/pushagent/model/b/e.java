package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.prefs.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.io.UnsupportedEncodingException;

public class e extends f {
    public e(Context context, PushDataReqMessage pushDataReqMessage) {
        super(context, pushDataReqMessage);
    }

    protected void rj() {
        if (-1 == this.dy) {
            this.dy = a.fc();
        }
    }

    protected boolean ri(Context context) {
        if (1 != i.mj(context).oe() || (rp(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.c.e.tf(context, this.pkgName, String.valueOf(this.dy)) ^ 1) == 0) {
            return true;
        }
        b.z("PushLog2976", this.pkgName + "'s token is not exist in current user");
        return false;
    }

    protected boolean rh(Context context) {
        if (1 != i.mj(context).oe() || (rp(context) ^ 1) == 0 || (com.huawei.android.pushagent.model.c.e.tg(context, this.pkgName, String.valueOf(this.dy), this.tokenBytes) ^ 1) == 0) {
            return true;
        }
        b.z("PushLog2976", this.pkgName + "'s token is not equal in current user");
        return false;
    }

    protected boolean rf(Context context) {
        if (!c.kw(context).kx(f.fz(this.pkgName, String.valueOf(this.dy)))) {
            return true;
        }
        b.x("PushLog2976", "closePush_Notify, pkgName is " + this.pkgName);
        return false;
    }

    protected boolean rg(Context context) {
        if (f.fx(context, this.pkgName, this.dy)) {
            return true;
        }
        return false;
    }

    protected boolean re(Context context) {
        if (f.fx(context, "com.huawei.android.pushagent", this.dy)) {
            return true;
        }
        return false;
    }

    protected void rk(Context context) {
        if (com.huawei.android.pushagent.model.c.b.sc().sb(context, this.pkgName, 2)) {
            com.huawei.android.pushagent.model.c.b.sc().so(context, 2, this.dz, this.pkgName, this.tokenBytes, this.ea, this.dy);
            return;
        }
        ro(context, this.pkgName, this.tokenBytes, this.dz, this.dy);
    }

    private void ro(Context context, String str, byte[] bArr, byte[] bArr2, int i) {
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("selfshow_info", bArr2);
        intent.putExtra("selfshow_token", bArr);
        intent.setFlags(32);
        if (re(context)) {
            b.z("PushLog2976", "send selfshow msg to NC to display.");
            intent.setPackage("com.huawei.android.pushagent");
            try {
                intent.putExtra("extra_encrypt_data", com.huawei.android.pushagent.utils.c.c.bd("com.huawei.android.pushagent", f.fy().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                b.y("PushLog2976", e.toString());
            }
        } else {
            b.z("PushLog2976", "NC is disable, send selfshow msg to [" + str + "] to display.");
            intent.setPackage(str);
        }
        f.fw(context, intent, i);
    }
}
