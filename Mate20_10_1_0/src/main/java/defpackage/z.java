package defpackage;

import com.huawei.android.feature.tasks.Task;

/* renamed from: z  reason: default package */
final class z implements Runnable {
    final /* synthetic */ y E;
    final /* synthetic */ Task t;

    z(y yVar, Task task) {
        this.E = yVar;
        this.t = task;
    }

    public final void run() {
        synchronized (this.E.r) {
            if (this.E.D != null) {
                this.E.D.onSuccess((Object) this.t.getResult());
            }
        }
    }
}
