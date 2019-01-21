package com.huawei.android.pushagent.a;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.a.f;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import java.util.Map.Entry;
import java.util.Set;

public class b {
    private Context appCtx;
    private g ig = g.kp(this.appCtx);

    public b(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void aaa() {
        if (!aae()) {
            aaj();
        } else if (aag()) {
            a.sv("PushLog3414", "The GDPR config is exist and valid, no need update again.");
        } else {
            a.sv("PushLog3414", "The GDPR config is exist but invalid, need recorrect and delete wrong files.");
            com.huawei.android.pushagent.b.a.abd(13);
            aad();
            aaj();
        }
    }

    private void aad() {
        com.huawei.android.pushagent.model.prefs.b il = com.huawei.android.pushagent.model.prefs.b.il(this.appCtx);
        for (String str : il.ip()) {
            if (!TextUtils.isEmpty(str)) {
                j.lm(this.appCtx).ln(str);
                a.sv("PushLog3414", "pkgName: " + str + " need register again");
            }
        }
        il.ij();
        this.ig.ks();
        com.huawei.android.pushagent.model.prefs.a.ff(this.appCtx).setValue("connId", "");
    }

    private void aaj() {
        aal();
        aai();
        aam();
    }

    private boolean aae() {
        if (this.ig.getDeviceIdType() == -1 || this.ig.getDeviceId().length() <= 0) {
            return false;
        }
        return true;
    }

    private boolean aag() {
        a.sv("PushLog3414", "check GDPR config is valid, DeviceType:" + this.ig.kq() + ", DeviceIdType:" + this.ig.getDeviceIdType() + ", DeviceId length:" + this.ig.getDeviceId().length());
        return 1 == this.ig.kq() ? (this.ig.getDeviceIdType() == 0 || 6 == this.ig.getDeviceIdType()) && 16 == this.ig.getDeviceId().length() : (9 == this.ig.getDeviceIdType() || 6 == this.ig.getDeviceIdType()) && this.ig.getDeviceId().length() > 16;
    }

    public void aam() {
        Set<Entry> ma = l.lw(this.appCtx).ma();
        if (ma.size() == 0) {
            a.st("PushLog3414", "no notify_switch need to update");
            return;
        }
        for (Entry entry : ma) {
            String str = (String) entry.getKey();
            boolean booleanValue = ((Boolean) entry.getValue()).booleanValue();
            if (TextUtils.isEmpty(str)) {
                a.sx("PushLog3414", "pkgNameWithUid is empty");
            } else {
                l.lw(this.appCtx).ly(d.yx(str), booleanValue);
                l.lw(this.appCtx).lx(str);
            }
        }
    }

    private void aai() {
        com.huawei.android.pushagent.model.prefs.a ff = com.huawei.android.pushagent.model.prefs.a.ff(this.appCtx);
        ff.setValue("serverIp", "");
        ff.setValue("serverPort", Integer.valueOf(-1));
        ff.setValue("result", Integer.valueOf(-1));
        e.jj(this.appCtx).jz(0);
        e.jj(this.appCtx).jy(0);
    }

    private boolean aah() {
        if (TextUtils.isEmpty(f.rz(this.appCtx, 2).getDeviceId())) {
            a.sv("PushLog3414", "not support get UDID.");
            return false;
        }
        a.sv("PushLog3414", "support get UDID.");
        return true;
    }

    private boolean aaf() {
        if (com.huawei.android.pushagent.model.prefs.b.il(this.appCtx).io() || (TextUtils.isEmpty(this.ig.kt()) ^ 1) != 0) {
            a.sv("PushLog3414", "exsit old IMEI config.");
            return true;
        }
        a.sv("PushLog3414", "It is a new device.");
        return false;
    }

    private void aak(int i) {
        String kt = this.ig.kt();
        if (1 != i || (TextUtils.isEmpty(kt) ^ 1) == 0) {
            this.ig.kr("");
            d.yq(this.appCtx);
        } else {
            this.ig.kz(kt);
            this.ig.setDeviceIdType(0);
        }
        this.ig.kw();
        this.ig.kx();
    }

    private void aal() {
        if (!aah() || aaf()) {
            this.ig.ky(1);
            aak(1);
            return;
        }
        this.ig.ky(2);
        aak(2);
    }
}
