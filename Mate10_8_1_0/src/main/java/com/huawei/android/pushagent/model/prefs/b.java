package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.a.c;

public class b {
    private static final byte[] co = new byte[0];
    private static b cp;
    private final c cq;

    private b(Context context) {
        this.cq = new c(context, "device_info");
    }

    public static b kp(Context context) {
        return kq(context);
    }

    private static b kq(Context context) {
        b bVar;
        synchronized (co) {
            if (cp == null) {
                cp = new b(context);
            }
            bVar = cp;
        }
        return bVar;
    }

    public int km() {
        return this.cq.getInt("pushDeviceType", 1);
    }

    public boolean ku(int i) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "setDeviceType: " + i + "[1:NOT_GDPR, 2:GDPR]");
        return this.cq.am("pushDeviceType", Integer.valueOf(i));
    }

    public String ko() {
        return this.cq.aj("pushDeviceId");
    }

    public boolean kv(String str) {
        return this.cq.am("pushDeviceId", str);
    }

    public String getDeviceId() {
        String str = "";
        try {
            return com.huawei.android.pushagent.utils.c.c.bb(ko());
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
            return str;
        }
    }

    public boolean kt(String str) {
        return kv(com.huawei.android.pushagent.utils.c.c.bc(str));
    }

    public int getDeviceIdType() {
        return this.cq.getInt("pushDeviceIdType", -1);
    }

    public boolean setDeviceIdType(int i) {
        return this.cq.am("pushDeviceIdType", Integer.valueOf(i));
    }

    public String kn() {
        return this.cq.aj("deviceId_v2");
    }

    public boolean kr() {
        return this.cq.an("deviceId_v2");
    }

    public boolean ks() {
        return this.cq.an("macAddress");
    }

    public void kl() {
        this.cq.ao();
    }
}
