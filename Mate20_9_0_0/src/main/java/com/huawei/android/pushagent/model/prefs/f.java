package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.b;

public class f {
    private static final byte[] ct = new byte[0];
    private static f cu;
    private final b cv;

    private f(Context context) {
        this.cv = new b(context, "PushConnectControl");
    }

    public static f ke(Context context) {
        return kl(context);
    }

    private static f kl(Context context) {
        f fVar;
        synchronized (ct) {
            if (cu == null) {
                cu = new f(context);
            }
            fVar = cu;
        }
        return fVar;
    }

    public int ki() {
        return this.cv.getInt("firstHBFailCnt", 0);
    }

    public boolean ko(int i) {
        a.sv("PushLog3414", "setFirstHBFailCnt:" + i);
        return this.cv.th("firstHBFailCnt", Integer.valueOf(i));
    }

    public boolean kf() {
        int ki = ki();
        if (ki < 100) {
            ki++;
        }
        return ko(ki);
    }

    public void kg() {
        ko(0);
        kh(false);
    }

    public boolean kj() {
        return this.cv.tk("nonWakeAlarmExist", false);
    }

    public boolean kh(boolean z) {
        a.st("PushLog3414", "setNonWakeAlarmExist:" + z);
        return this.cv.th("nonWakeAlarmExist", Boolean.valueOf(z));
    }

    public String kk() {
        return this.cv.tg("connectPushSvrInfos");
    }

    public boolean kn(String str) {
        return this.cv.th("connectPushSvrInfos", str);
    }

    public boolean km() {
        return this.cv.ti("connectPushSvrInfos");
    }
}
