package defpackage;

import com.huawei.android.feature.tasks.OnCompleteListener;
import com.huawei.android.feature.tasks.OnFailureListener;
import com.huawei.android.feature.tasks.OnSuccessListener;
import com.huawei.android.feature.tasks.RuntimeExecutionException;
import com.huawei.android.feature.tasks.Task;
import com.huawei.android.feature.tasks.TaskExecutors;
import java.util.concurrent.Executor;

/* renamed from: w  reason: default package */
public final class w<TResult> extends Task<TResult> {
    private Exception A;
    public final Object r = new Object();
    public final x<TResult> x = new x<>();
    public boolean y;
    public TResult z;

    private void a() {
        synchronized (this.r) {
            if (this.y) {
                this.x.b(this);
            }
        }
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> onCompleteListener) {
        return addOnCompleteListener(TaskExecutors.MAIN_THREAD, onCompleteListener);
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> onCompleteListener) {
        this.x.a(new q(executor, onCompleteListener));
        a();
        return this;
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Task<TResult> addOnFailureListener(OnFailureListener onFailureListener) {
        return addOnFailureListener(TaskExecutors.MAIN_THREAD, onFailureListener);
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Task<TResult> addOnFailureListener(Executor executor, OnFailureListener onFailureListener) {
        this.x.a(new u(executor, onFailureListener));
        a();
        return this;
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> onSuccessListener) {
        return addOnSuccessListener(TaskExecutors.MAIN_THREAD, onSuccessListener);
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<? super TResult> onSuccessListener) {
        this.x.a(new y(executor, onSuccessListener));
        a();
        return this;
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final Exception getException() {
        Exception exc;
        synchronized (this.r) {
            exc = this.A;
        }
        return exc;
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final TResult getResult() {
        TResult tresult;
        synchronized (this.r) {
            if (!this.y) {
                throw new IllegalStateException("Task is not yet complete");
            } else if (this.A != null) {
                throw new RuntimeExecutionException(this.A);
            } else {
                tresult = this.z;
            }
        }
        return tresult;
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final boolean isComplete() {
        boolean z2;
        synchronized (this.r) {
            z2 = this.y;
        }
        return z2;
    }

    @Override // com.huawei.android.feature.tasks.Task
    public final boolean isSuccessful() {
        boolean z2;
        synchronized (this.r) {
            z2 = this.y && this.A == null;
        }
        return z2;
    }

    public final boolean notifyException(Exception exc) {
        if (exc == null) {
            return false;
        }
        synchronized (this.r) {
            if (this.y) {
                return false;
            }
            this.y = true;
            this.A = exc;
            this.x.b(this);
            return true;
        }
    }

    public final boolean notifyResult(TResult tresult) {
        boolean z2 = true;
        synchronized (this.r) {
            if (this.y) {
                z2 = false;
            } else {
                this.y = true;
                this.z = tresult;
                this.x.b(this);
            }
        }
        return z2;
    }
}
