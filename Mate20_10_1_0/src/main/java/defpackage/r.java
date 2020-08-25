package defpackage;

import com.huawei.android.feature.tasks.Task;

/* renamed from: r  reason: default package */
final class r implements Runnable {
    final /* synthetic */ Task t;
    final /* synthetic */ q u;

    r(q qVar, Task task) {
        this.u = qVar;
        this.t = task;
    }

    public final void run() {
        synchronized (this.u.r) {
            if (this.u.s != null) {
                this.u.s.onComplete(this.t);
            }
        }
    }
}
