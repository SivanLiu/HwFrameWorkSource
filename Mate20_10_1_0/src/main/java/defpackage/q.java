package defpackage;

import com.huawei.android.feature.tasks.OnCompleteListener;
import com.huawei.android.feature.tasks.Task;
import java.util.concurrent.Executor;

/* renamed from: q  reason: default package */
final class q<TResult> implements p<TResult> {
    private final Executor mExecutor;
    final Object r = new Object();
    OnCompleteListener<TResult> s;

    public q(Executor executor, OnCompleteListener<TResult> onCompleteListener) {
        this.mExecutor = executor;
        this.s = onCompleteListener;
    }

    @Override // defpackage.p
    public final void a(Task<TResult> task) {
        synchronized (this.r) {
            if (this.s != null) {
                this.mExecutor.execute(new r(this, task));
            }
        }
    }
}
