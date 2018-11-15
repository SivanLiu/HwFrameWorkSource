package com.huawei.android.pushagent.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.a.a;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.d;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.h;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.g;
import java.util.Map.Entry;

public class c {
    private Context appCtx;
    private b by = b.oq(this.appCtx);

    public c(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    /* renamed from: if */
    public void m0if() {
        if (!ij()) {
            io();
        } else if (il()) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "The GDPR config is exist and valid, no need update again.");
        } else {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "The GDPR config is exist but invalid, need recorrect and delete wrong files.");
            a.hx(13);
            ii();
            io();
        }
    }

    private void ii() {
        e pn = e.pn(this.appCtx);
        for (String str : pn.pq()) {
            if (!TextUtils.isEmpty(str)) {
                h.qr(this.appCtx).qv(str);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "pkgName: " + str + " need register again");
            }
        }
        pn.pv();
        this.by.os();
        k.rh(this.appCtx).setValue("connId", "");
    }

    private void io() {
        iq();
        in();
        ir();
    }

    private boolean ij() {
        if (this.by.getDeviceIdType() == -1 || this.by.getDeviceId().length() <= 0) {
            return false;
        }
        return true;
    }

    private boolean il() {
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "check GDPR config is valid, DeviceType:" + this.by.or() + ", DeviceIdType:" + this.by.getDeviceIdType() + ", DeviceId length:" + this.by.getDeviceId().length());
        return 1 == this.by.or() ? (this.by.getDeviceIdType() == 0 || 6 == this.by.getDeviceIdType()) && 16 == this.by.getDeviceId().length() : (9 == this.by.getDeviceIdType() || 6 == this.by.getDeviceIdType()) && this.by.getDeviceId().length() > 16;
    }

    public void ir() {
        Iterable<Entry> pk = d.ph(this.appCtx).pk();
        if (pk.size() == 0) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "no notify_switch need to update");
            return;
        }
        for (Entry entry : pk) {
            String str = (String) entry.getKey();
            boolean booleanValue = ((Boolean) entry.getValue()).booleanValue();
            if (TextUtils.isEmpty(str)) {
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "pkgNameWithUid is empty");
            } else {
                d.ph(this.appCtx).pj(g.gv(str), booleanValue);
                d.ph(this.appCtx).pi(str);
            }
        }
    }

    private void in() {
        k rh = k.rh(this.appCtx);
        rh.setValue("serverIp", "");
        rh.setValue("serverPort", Integer.valueOf(-1));
        rh.setValue("result", Integer.valueOf(-1));
        l.ul(this.appCtx).ut(0);
        l.ul(this.appCtx).uu(0);
    }

    private boolean im() {
        if (TextUtils.isEmpty(com.huawei.android.pushagent.utils.d.d.cn(this.appCtx, 2).getDeviceId())) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "not support get UDID.");
            return false;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "support get UDID.");
        return true;
    }

    private boolean ik() {
        if (e.pn(this.appCtx).pt() || (TextUtils.isEmpty(this.by.ot()) ^ 1) != 0) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "exsit old IMEI config.");
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "It is a new device.");
        return false;
    }

    private void ip(int i) {
        Object ot = this.by.ot();
        if (1 != i || (TextUtils.isEmpty(ot) ^ 1) == 0) {
            this.by.oy("");
            g.gi(this.appCtx);
        } else {
            this.by.pa(ot);
            this.by.setDeviceIdType(0);
        }
        this.by.ow();
        this.by.ox();
    }

    private void iq() {
        if (!im() || ik()) {
            this.by.oz(1);
            ip(1);
            return;
        }
        this.by.oz(2);
        ip(2);
    }
}
