package defpackage;

import com.huawei.android.feature.tasks.Task;
import java.util.ArrayDeque;
import java.util.Queue;

/* renamed from: x  reason: default package */
public final class x<TResult> {
    private Queue<p<TResult>> B;
    private boolean C;
    private final Object r = new Object();

    x() {
    }

    public final void a(p<TResult> pVar) {
        synchronized (this.r) {
            if (this.B == null) {
                this.B = new ArrayDeque();
            }
            this.B.add(pVar);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0011, code lost:
        r1 = r2.r;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0013, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:?, code lost:
        r0 = r2.B.poll();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x001c, code lost:
        if (r0 != null) goto L_0x0029;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x001e, code lost:
        r2.C = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0021, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0029, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x002a, code lost:
        r0.a(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:?, code lost:
        return;
     */
    public final void b(Task<TResult> task) {
        synchronized (this.r) {
            if (this.B != null && !this.C) {
                this.C = true;
            }
        }
    }
}
