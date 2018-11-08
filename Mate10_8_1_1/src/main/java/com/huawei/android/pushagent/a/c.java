package com.huawei.android.pushagent.a;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f;
import java.util.Map.Entry;

public class c {
    private Context appCtx;
    private b hj = b.kp(this.appCtx);

    public c(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void xj() {
        if (!xn()) {
            xs();
        } else if (xp()) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "The GDPR config is exist and valid, no need update again.");
        } else {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "The GDPR config is exist but invalid, need recorrect and delete wrong files.");
            a.aak(13);
            xm();
            xs();
        }
    }

    private void xm() {
        com.huawei.android.pushagent.model.prefs.a kc = com.huawei.android.pushagent.model.prefs.a.kc(this.appCtx);
        for (String str : kc.kg()) {
            if (!TextUtils.isEmpty(str)) {
                j.pn(this.appCtx).pq(str);
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "pkgName: " + str + " need register again");
            }
        }
        kc.ka();
        this.hj.kl();
        i.mj(this.appCtx).setValue("connId", "");
    }

    private void xs() {
        xu();
        xr();
        xv();
    }

    private boolean xn() {
        if (this.hj.getDeviceIdType() == -1 || this.hj.getDeviceId().length() <= 0) {
            return false;
        }
        return true;
    }

    private boolean xp() {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "check GDPR config is valid, DeviceType:" + this.hj.km() + ", DeviceIdType:" + this.hj.getDeviceIdType() + ", DeviceId length:" + this.hj.getDeviceId().length());
        return 1 == this.hj.km() ? (this.hj.getDeviceIdType() == 0 || 6 == this.hj.getDeviceIdType()) && 16 == this.hj.getDeviceId().length() : (9 == this.hj.getDeviceIdType() || 6 == this.hj.getDeviceIdType()) && this.hj.getDeviceId().length() > 16;
    }

    public void xv() {
        Iterable<Entry> ky = com.huawei.android.pushagent.model.prefs.c.kw(this.appCtx).ky();
        if (ky.size() == 0) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "no notify_switch need to update");
            return;
        }
        for (Entry entry : ky) {
            String str = (String) entry.getKey();
            boolean booleanValue = ((Boolean) entry.getValue()).booleanValue();
            if (TextUtils.isEmpty(str)) {
                com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "pkgNameWithUid is empty");
            } else {
                com.huawei.android.pushagent.model.prefs.c.kw(this.appCtx).lb(f.gl(str), booleanValue);
                com.huawei.android.pushagent.model.prefs.c.kw(this.appCtx).la(str);
            }
        }
    }

    private void xr() {
        i mj = i.mj(this.appCtx);
        mj.setValue("serverIp", "");
        mj.setValue("serverPort", Integer.valueOf(-1));
        mj.setValue("result", Integer.valueOf(-1));
        k.pt(this.appCtx).qc(0);
        k.pt(this.appCtx).qf(0);
    }

    private boolean xq() {
        if (TextUtils.isEmpty(com.huawei.android.pushagent.utils.b.b.av(this.appCtx, 2).getDeviceId())) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "not support get UDID.");
            return false;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "support get UDID.");
        return true;
    }

    private boolean xo() {
        if (com.huawei.android.pushagent.model.prefs.a.kc(this.appCtx).kf() || (TextUtils.isEmpty(this.hj.kn()) ^ 1) != 0) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "exsit old IMEI config.");
            return true;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "It is a new device.");
        return false;
    }

    private void xt(int i) {
        Object kn = this.hj.kn();
        if (1 != i || (TextUtils.isEmpty(kn) ^ 1) == 0) {
            this.hj.kt("");
            f.gk(this.appCtx);
        } else {
            this.hj.kv(kn);
            this.hj.setDeviceIdType(0);
        }
        this.hj.kr();
        this.hj.ks();
    }

    private void xu() {
        if (!xq() || xo()) {
            this.hj.ku(1);
            xt(1);
            return;
        }
        this.hj.ku(2);
        xt(2);
    }
}
