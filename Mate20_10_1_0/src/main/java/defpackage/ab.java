package defpackage;

import android.util.Log;
import com.huawei.android.pushagent.PushService;

/* renamed from: ab  reason: default package */
public final class ab implements Runnable {
    final /* synthetic */ int Q;
    final /* synthetic */ PushService R;

    public ab(PushService pushService, int i) {
        this.R = pushService;
        this.Q = i;
    }

    public final void run() {
        if (this.Q != 0) {
            Log.e("PushLogSys", "handle local pushcore install error");
            synchronized (PushService.G) {
                ai.b(this.R.M, this.R.L);
                ai.a(this.Q);
            }
        }
        PushService.a(this.R, this.R.c());
    }
}
