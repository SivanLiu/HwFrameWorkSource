package defpackage;

import com.huawei.android.feature.tasks.Task;

/* renamed from: v  reason: default package */
final class v implements Runnable {
    final /* synthetic */ Task t;
    final /* synthetic */ u w;

    v(u uVar, Task task) {
        this.w = uVar;
        this.t = task;
    }

    public final void run() {
        synchronized (this.w.r) {
            if (this.w.v != null) {
                this.w.v.onFailure(this.t.getException());
            }
        }
    }
}
