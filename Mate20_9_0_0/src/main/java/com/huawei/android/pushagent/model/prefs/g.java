package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.b;

public class g {
    private static final byte[] cw = new byte[0];
    private static g cx;
    private final b cy;

    private g(Context context) {
        this.cy = new b(context, "device_info");
    }

    public static g kp(Context context) {
        return kv(context);
    }

    private static g kv(Context context) {
        g gVar;
        synchronized (cw) {
            if (cx == null) {
                cx = new g(context);
            }
            gVar = cx;
        }
        return gVar;
    }

    public int kq() {
        return this.cy.getInt("pushDeviceType", 1);
    }

    public boolean ky(int i) {
        a.sv("PushLog3414", "setDeviceType: " + i + "[1:NOT_GDPR, 2:GDPR]");
        return this.cy.th("pushDeviceType", Integer.valueOf(i));
    }

    public String ku() {
        return this.cy.tg("pushDeviceId");
    }

    public boolean kz(String str) {
        return this.cy.th("pushDeviceId", str);
    }

    public String getDeviceId() {
        String str = "";
        try {
            return com.huawei.android.pushagent.utils.e.a.vt(ku());
        } catch (Exception e) {
            a.sw("PushLog3414", e.toString(), e);
            return str;
        }
    }

    public boolean kr(String str) {
        return kz(com.huawei.android.pushagent.utils.e.a.vu(str));
    }

    public int getDeviceIdType() {
        return this.cy.getInt("pushDeviceIdType", -1);
    }

    public boolean setDeviceIdType(int i) {
        return this.cy.th("pushDeviceIdType", Integer.valueOf(i));
    }

    public String kt() {
        return this.cy.tg("deviceId_v2");
    }

    public boolean kw() {
        return this.cy.ti("deviceId_v2");
    }

    public boolean kx() {
        return this.cy.ti("macAddress");
    }

    public void ks() {
        this.cy.tj();
    }
}
