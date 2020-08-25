package defpackage;

import com.huawei.android.feature.tasks.OnSuccessListener;
import com.huawei.android.feature.tasks.Task;
import java.util.concurrent.Executor;

/* renamed from: y  reason: default package */
final class y<TResult> implements p<TResult> {
    OnSuccessListener<? super TResult> D;
    private final Executor mExecutor;
    final Object r = new Object();

    public y(Executor executor, OnSuccessListener<? super TResult> onSuccessListener) {
        this.mExecutor = executor;
        this.D = onSuccessListener;
    }

    @Override // defpackage.p
    public final void a(Task<TResult> task) {
        if (task.isSuccessful()) {
            synchronized (this.r) {
                if (this.D != null) {
                    this.mExecutor.execute(new z(this, task));
                }
            }
        }
    }
}
