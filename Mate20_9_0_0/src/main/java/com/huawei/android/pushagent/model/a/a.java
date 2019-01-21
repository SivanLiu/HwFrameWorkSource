package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.b.c;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.tools.b;
import java.util.Arrays;

public abstract class a implements e {
    private Context appCtx;
    protected byte[] cd;
    protected String ce;
    protected PushDataReqMessage cf;
    protected int cg;
    protected String pkgName;
    protected byte[] tokenBytes;

    protected abstract boolean en(Context context);

    protected abstract boolean eo(Context context);

    protected abstract boolean eq(Context context);

    protected abstract boolean er(Context context);

    protected abstract boolean es(Context context);

    protected abstract void eu(Context context);

    public a(Context context, PushDataReqMessage pushDataReqMessage) {
        this.appCtx = context.getApplicationContext();
        this.cf = pushDataReqMessage;
    }

    private boolean isValid() {
        if (this.cf.isValid()) {
            return true;
        }
        return false;
    }

    private void ev() {
        if (b.sf()) {
            b.sg(2, 180);
        } else {
            d.ym(2, 180);
        }
    }

    protected void ew() {
        this.pkgName = this.cf.getPkgName();
        this.cg = this.cf.getUserId();
        this.tokenBytes = this.cf.w();
        this.cd = this.cf.x();
        this.ce = c.tr(this.cf.y());
    }

    protected void et() {
    }

    private boolean ex() {
        if (d.yn(this.appCtx, this.pkgName, this.cg)) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + " is not installed in " + this.cg + " user");
        return false;
    }

    private boolean em(Context context) {
        boolean fq = com.huawei.android.pushagent.model.prefs.a.ff(context).fq();
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "needCheckAgreement:" + fq);
        if (fq) {
            fq = k.ls(context).lt(this.pkgName);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "disAgree:" + fq);
            if (fq) {
                return false;
            }
        }
        return true;
    }

    protected boolean ez(Context context) {
        String trim = com.huawei.android.pushagent.model.prefs.a.ff(context).fr().trim();
        if (TextUtils.isEmpty(trim) || !Arrays.asList(trim.split("#")).contains(this.pkgName)) {
            return false;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + " is in pass white list, not valid token.");
        return true;
    }

    protected boolean ey(Context context) {
        String trim = com.huawei.android.pushagent.model.prefs.a.ff(context).fs().trim();
        if (TextUtils.isEmpty(trim) || !Arrays.asList(trim.split("#")).contains(this.pkgName)) {
            return false;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", this.pkgName + " is in notice white list, not valid token.");
        return true;
    }

    private boolean ep(Context context) {
        return d.yo(context, this.pkgName, this.cg);
    }

    public byte fa() {
        byte b = (byte) 0;
        if (!isValid()) {
            return (byte) 0;
        }
        ev();
        ew();
        et();
        if (!ex()) {
            return (byte) 2;
        }
        if (!em(this.appCtx)) {
            return (byte) 17;
        }
        if (!es(this.appCtx)) {
            return (byte) 5;
        }
        if (!er(this.appCtx)) {
            return (byte) 19;
        }
        if (!eo(this.appCtx)) {
            return (byte) 6;
        }
        if (!eq(this.appCtx)) {
            return (byte) 9;
        }
        if (!en(this.appCtx)) {
            b = (byte) 7;
        }
        if (!ep(this.appCtx)) {
            b = (byte) 18;
        }
        eu(this.appCtx);
        return b;
    }
}
