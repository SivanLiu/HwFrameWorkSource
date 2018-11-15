package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.c;

public class a {
    private static final byte[] ey = new byte[0];
    private static a ez;
    private final com.huawei.android.pushagent.utils.f.a fa;

    private a(Context context) {
        this.fa = new com.huawei.android.pushagent.utils.f.a(context, "PushConnectControl");
    }

    public static a of(Context context) {
        return ol(context);
    }

    private static a ol(Context context) {
        a aVar;
        synchronized (ey) {
            if (ez == null) {
                ez = new a(context);
            }
            aVar = ez;
        }
        return aVar;
    }

    public int oj() {
        return this.fa.getInt("firstHBFailCnt", 0);
    }

    public boolean oo(int i) {
        c.ep("PushLog3413", "setFirstHBFailCnt:" + i);
        return this.fa.ea("firstHBFailCnt", Integer.valueOf(i));
    }

    public boolean og() {
        int oj = oj();
        if (oj < 100) {
            oj++;
        }
        return oo(oj);
    }

    public void oh() {
        oo(0);
        op(false);
    }

    public boolean ok() {
        return this.fa.eb("nonWakeAlarmExist", false);
    }

    public boolean op(boolean z) {
        c.er("PushLog3413", "setNonWakeAlarmExist:" + z);
        return this.fa.ea("nonWakeAlarmExist", Boolean.valueOf(z));
    }

    public String oi() {
        return this.fa.ec("connectPushSvrInfos");
    }

    public boolean on(String str) {
        return this.fa.ea("connectPushSvrInfos", str);
    }

    public boolean om() {
        return this.fa.ed("connectPushSvrInfos");
    }
}
