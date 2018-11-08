package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.tools.a;
import java.util.Arrays;

public abstract class f implements d {
    private Context appCtx;
    protected int dy;
    protected byte[] dz;
    protected String ea;
    protected PushDataReqMessage eb;
    protected String pkgName;
    protected byte[] tokenBytes;

    protected abstract boolean re(Context context);

    protected abstract boolean rf(Context context);

    protected abstract boolean rg(Context context);

    protected abstract boolean rh(Context context);

    protected abstract boolean ri(Context context);

    protected abstract void rk(Context context);

    public f(Context context, PushDataReqMessage pushDataReqMessage) {
        this.appCtx = context.getApplicationContext();
        this.eb = pushDataReqMessage;
    }

    private boolean isValid() {
        if (this.eb.isValid()) {
            return true;
        }
        return false;
    }

    private void rt() {
        if (a.j()) {
            a.g(2, 180);
        } else {
            com.huawei.android.pushagent.utils.f.fv(2, 180);
        }
    }

    protected void ru() {
        this.pkgName = this.eb.getPkgName();
        this.dy = this.eb.getUserId();
        this.tokenBytes = this.eb.zj();
        this.dz = this.eb.zg();
        this.ea = com.huawei.android.pushagent.utils.a.a.u(this.eb.zh());
    }

    protected void rj() {
    }

    private boolean rv() {
        if (com.huawei.android.pushagent.utils.f.gq(this.appCtx, this.pkgName, this.dy)) {
            return true;
        }
        b.z("PushLog2976", this.pkgName + " is not installed in " + this.dy + " user");
        return false;
    }

    private boolean rr(Context context) {
        boolean pm = i.mj(context).pm();
        b.z("PushLog2976", "needCheckAgreement:" + pm);
        if (pm) {
            pm = com.huawei.android.pushagent.model.prefs.f.lu(context).lt(this.pkgName);
            b.z("PushLog2976", "disAgree:" + pm);
            if (pm) {
                return false;
            }
        }
        return true;
    }

    protected boolean rq(Context context) {
        Object trim = i.mj(context).pf().trim();
        if (TextUtils.isEmpty(trim) || !Arrays.asList(trim.split("#")).contains(this.pkgName)) {
            return false;
        }
        b.z("PushLog2976", this.pkgName + " is in pass white list, not valid token.");
        return true;
    }

    protected boolean rp(Context context) {
        Object trim = i.mj(context).pe().trim();
        if (TextUtils.isEmpty(trim) || !Arrays.asList(trim.split("#")).contains(this.pkgName)) {
            return false;
        }
        b.z("PushLog2976", this.pkgName + " is in notice white list, not valid token.");
        return true;
    }

    private boolean rs(Context context) {
        return com.huawei.android.pushagent.utils.f.gw(context, this.pkgName, this.dy);
    }

    public byte rn() {
        byte b = (byte) 0;
        if (!isValid()) {
            return (byte) 0;
        }
        rt();
        ru();
        rj();
        if (!rv()) {
            return (byte) 2;
        }
        if (!rr(this.appCtx)) {
            return (byte) 17;
        }
        if (!ri(this.appCtx)) {
            return (byte) 5;
        }
        if (!rh(this.appCtx)) {
            return (byte) 19;
        }
        if (!rf(this.appCtx)) {
            return (byte) 6;
        }
        if (!rg(this.appCtx)) {
            return (byte) 9;
        }
        if (!re(this.appCtx)) {
            b = (byte) 7;
        }
        if (!rs(this.appCtx)) {
            b = (byte) 18;
        }
        rk(this.appCtx);
        return b;
    }
}
