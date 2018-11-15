package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.c;
import java.util.Arrays;

public abstract class a implements b {
    private Context appCtx;
    protected byte[] hl;
    protected String hm;
    protected PushDataReqMessage hn;
    protected int ho;
    protected String pkgName;
    protected byte[] tokenBytes;

    protected abstract boolean yy(Context context);

    protected abstract boolean yz(Context context);

    protected abstract boolean zb(Context context);

    protected abstract boolean zc(Context context);

    protected abstract boolean zd(Context context);

    protected abstract void zf(Context context);

    public a(Context context, PushDataReqMessage pushDataReqMessage) {
        this.appCtx = context.getApplicationContext();
        this.hn = pushDataReqMessage;
    }

    private boolean isValid() {
        if (this.hn.isValid()) {
            return true;
        }
        return false;
    }

    private void zg() {
        if (c.cr()) {
            c.ct(2, 180);
        } else {
            g.ge(2, 180);
        }
    }

    protected void zh() {
        this.pkgName = this.hn.getPkgName();
        this.ho = this.hn.getUserId();
        this.tokenBytes = this.hn.js();
        this.hl = this.hn.jt();
        this.hm = b.el(this.hn.ju());
    }

    protected void ze() {
    }

    private boolean zi() {
        if (g.gc(this.appCtx, this.pkgName, this.ho)) {
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", this.pkgName + " is not installed in " + this.ho + " user");
        return false;
    }

    private boolean yx(Context context) {
        boolean ry = k.rh(context).ry();
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "needCheckAgreement:" + ry);
        if (ry) {
            ry = i.qx(context).qy(this.pkgName);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "disAgree:" + ry);
            if (ry) {
                return false;
            }
        }
        return true;
    }

    protected boolean zk(Context context) {
        Object trim = k.rh(context).rz().trim();
        if (TextUtils.isEmpty(trim) || !Arrays.asList(trim.split("#")).contains(this.pkgName)) {
            return false;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", this.pkgName + " is in pass white list, not valid token.");
        return true;
    }

    protected boolean zj(Context context) {
        Object trim = k.rh(context).sa().trim();
        if (TextUtils.isEmpty(trim) || !Arrays.asList(trim.split("#")).contains(this.pkgName)) {
            return false;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", this.pkgName + " is in notice white list, not valid token.");
        return true;
    }

    private boolean za(Context context) {
        return g.gf(context, this.pkgName, this.ho);
    }

    public byte zl() {
        byte b = (byte) 0;
        if (!isValid()) {
            return (byte) 0;
        }
        zg();
        zh();
        ze();
        if (!zi()) {
            return (byte) 2;
        }
        if (!yx(this.appCtx)) {
            return (byte) 17;
        }
        if (!zd(this.appCtx)) {
            return (byte) 5;
        }
        if (!zc(this.appCtx)) {
            return (byte) 19;
        }
        if (!yz(this.appCtx)) {
            return (byte) 6;
        }
        if (!zb(this.appCtx)) {
            return (byte) 9;
        }
        if (!yy(this.appCtx)) {
            b = (byte) 7;
        }
        if (!za(this.appCtx)) {
            b = (byte) 18;
        }
        zf(this.appCtx);
        return b;
    }
}
