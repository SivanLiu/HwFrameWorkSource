package com.huawei.android.pushagent;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.huawei.android.pushagent.model.d.c;

class a extends Thread {
    private static long jb = 2000;
    private Context jc;
    public Handler jd;
    private MessageQueue je;
    private WakeLock jf = ((PowerManager) this.jc.getSystemService("power")).newWakeLock(1, "eventloop");

    public a(Context context) {
        super("ReceiverDispatcher");
        this.jc = context;
    }

    public void run() {
        try {
            Looper.prepare();
            this.jd = new Handler();
            this.je = Looper.myQueue();
            this.je.addIdleHandler(new d(this));
            Looper.loop();
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "ReceiverDispatcher thread exit!");
        } catch (Throwable th) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", com.huawei.android.pushagent.utils.b.a.ta(th));
        } finally {
            abq();
        }
    }

    void abp(c cVar, Intent intent) {
        if (this.jd == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "ReceiverDispatcher: the handler is null");
            PushService.abt().stopSelf();
            return;
        }
        try {
            if (!this.jf.isHeld()) {
                this.jf.acquire(jb);
            }
            if (!this.jd.postDelayed(new b(this, cVar, intent, null), 1)) {
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "postDelayed runnable error");
                throw new Exception("postDelayed runnable error");
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "dispatchIntent error," + e.toString(), e);
            abq();
        }
    }

    private void abq() {
        try {
            if (this.jf != null && this.jf.isHeld()) {
                this.jf.release();
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", e.toString());
        }
    }
}
