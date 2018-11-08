package com.huawei.android.pushagent;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.huawei.android.pushagent.model.a.d;

class b extends Thread {
    private static long jh = 2000;
    public Handler jg;
    private Context ji;
    private MessageQueue jj;
    private WakeLock jk = ((PowerManager) this.ji.getSystemService("power")).newWakeLock(1, "eventloop");

    public b(Context context) {
        super("ReceiverDispatcher");
        this.ji = context;
    }

    public void run() {
        try {
            Looper.prepare();
            this.jg = new Handler();
            this.jj = Looper.myQueue();
            this.jj.addIdleHandler(new g(this));
            Looper.loop();
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "ReceiverDispatcher thread exit!");
        } catch (Throwable th) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", com.huawei.android.pushagent.utils.a.b.ag(th));
        } finally {
            abr();
        }
    }

    void abq(d dVar, Intent intent) {
        if (this.jg == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "ReceiverDispatcher: the handler is null");
            PushService.abd().stopSelf();
            return;
        }
        try {
            if (!this.jk.isHeld()) {
                this.jk.acquire(jh);
            }
            if (!this.jg.postDelayed(new c(this, dVar, intent), 1)) {
                com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "postDelayed runnable error");
                throw new Exception("postDelayed runnable error");
            }
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "dispatchIntent error," + e.toString(), e);
            abr();
        }
    }

    private void abr() {
        try {
            if (this.jk != null && this.jk.isHeld()) {
                this.jk.release();
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e.toString());
        }
    }
}
