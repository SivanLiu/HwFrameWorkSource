package defpackage;

import com.huawei.android.feature.tasks.OnFailureListener;
import com.huawei.android.feature.tasks.Task;
import java.util.concurrent.Executor;

/* renamed from: u  reason: default package */
final class u<TResult> implements p<TResult> {
    private Executor mExecutor;
    final Object r = new Object();
    OnFailureListener v;

    public u(Executor executor, OnFailureListener onFailureListener) {
        this.mExecutor = executor;
        this.v = onFailureListener;
    }

    @Override // defpackage.p
    public final void a(Task<TResult> task) {
        if (!task.isSuccessful()) {
            synchronized (this.r) {
                if (this.v != null) {
                    this.mExecutor.execute(new v(this, task));
                }
            }
        }
    }
}
