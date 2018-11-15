package com.huawei.android.pushagent;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.huawei.android.pushagent.model.a.a;
import com.huawei.android.pushagent.utils.f.c;

class b extends Thread {
    private static long jm = 2000;
    public Handler jl;
    private Context jn;
    private MessageQueue jo;
    private WakeLock jp = ((PowerManager) this.jn.getSystemService("power")).newWakeLock(1, "eventloop");

    public b(Context context) {
        super("ReceiverDispatcher");
        this.jn = context;
    }

    public void run() {
        try {
            Looper.prepare();
            this.jl = new Handler();
            this.jo = Looper.myQueue();
            this.jo.addIdleHandler(new g(this));
            Looper.loop();
            c.ep("PushLog3413", "ReceiverDispatcher thread exit!");
        } catch (Throwable th) {
            c.eq("PushLog3413", c.ew(th));
        } finally {
            acj();
        }
    }

    void aci(a aVar, Intent intent) {
        if (this.jl == null) {
            c.eq("PushLog3413", "ReceiverDispatcher: the handler is null");
            PushService.abp().stopSelf();
            return;
        }
        try {
            if (!this.jp.isHeld()) {
                this.jp.acquire(jm);
            }
            if (!this.jl.postDelayed(new c(this, aVar, intent, null), 1)) {
                c.eo("PushLog3413", "postDelayed runnable error");
                throw new Exception("postDelayed runnable error");
            }
        } catch (Throwable e) {
            c.es("PushLog3413", "dispatchIntent error," + e.toString(), e);
            acj();
        }
    }

    private void acj() {
        try {
            if (this.jp != null && this.jp.isHeld()) {
                this.jp.release();
            }
        } catch (Exception e) {
            c.eq("PushLog3413", e.toString());
        }
    }
}
