package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.a;
import com.huawei.android.pushagent.utils.f.c;

public class b {
    private static final byte[] fb = new byte[0];
    private static b fc;
    private final a fd;

    private b(Context context) {
        this.fd = new a(context, "device_info");
    }

    public static b oq(Context context) {
        return ov(context);
    }

    private static b ov(Context context) {
        b bVar;
        synchronized (fb) {
            if (fc == null) {
                fc = new b(context);
            }
            bVar = fc;
        }
        return bVar;
    }

    public int or() {
        return this.fd.getInt("pushDeviceType", 1);
    }

    public boolean oz(int i) {
        c.ep("PushLog3413", "setDeviceType: " + i + "[1:NOT_GDPR, 2:GDPR]");
        return this.fd.ea("pushDeviceType", Integer.valueOf(i));
    }

    public String ou() {
        return this.fd.ec("pushDeviceId");
    }

    public boolean pa(String str) {
        return this.fd.ea("pushDeviceId", str);
    }

    public String getDeviceId() {
        String str = "";
        try {
            return com.huawei.android.pushagent.utils.a.c.j(ou());
        } catch (Throwable e) {
            c.es("PushLog3413", e.toString(), e);
            return str;
        }
    }

    public boolean oy(String str) {
        return pa(com.huawei.android.pushagent.utils.a.c.k(str));
    }

    public int getDeviceIdType() {
        return this.fd.getInt("pushDeviceIdType", -1);
    }

    public boolean setDeviceIdType(int i) {
        return this.fd.ea("pushDeviceIdType", Integer.valueOf(i));
    }

    public String ot() {
        return this.fd.ec("deviceId_v2");
    }

    public boolean ow() {
        return this.fd.ed("deviceId_v2");
    }

    public boolean ox() {
        return this.fd.ed("macAddress");
    }

    public void os() {
        this.fd.dz();
    }
}
