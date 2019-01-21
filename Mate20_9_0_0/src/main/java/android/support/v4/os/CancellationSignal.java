package android.support.v4.os;

import android.os.Build.VERSION;

public final class CancellationSignal {
    private boolean mCancelInProgress;
    private Object mCancellationSignalObj;
    private boolean mIsCanceled;
    private OnCancelListener mOnCancelListener;

    public interface OnCancelListener {
        void onCancel();
    }

    public boolean isCanceled() {
        boolean z;
        synchronized (this) {
            z = this.mIsCanceled;
        }
        return z;
    }

    public void throwIfCanceled() {
        if (isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r1.onCancel();
     */
    /* JADX WARNING: Missing block: B:17:0x001c, code skipped:
            if (r0 == null) goto L_0x0036;
     */
    /* JADX WARNING: Missing block: B:19:0x0022, code skipped:
            if (android.os.Build.VERSION.SDK_INT < 16) goto L_0x0036;
     */
    /* JADX WARNING: Missing block: B:20:0x0024, code skipped:
            ((android.os.CancellationSignal) r0).cancel();
     */
    /* JADX WARNING: Missing block: B:21:0x002b, code skipped:
            monitor-enter(r6);
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r6.mCancelInProgress = false;
            notifyAll();
     */
    /* JADX WARNING: Missing block: B:30:0x0036, code skipped:
            monitor-enter(r6);
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            r6.mCancelInProgress = false;
            notifyAll();
     */
    /* JADX WARNING: Missing block: B:33:0x003c, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:35:0x003e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void cancel() {
        Throwable th;
        synchronized (this) {
            try {
                if (this.mIsCanceled) {
                    return;
                }
                this.mIsCanceled = true;
                this.mCancelInProgress = true;
                OnCancelListener listener = this.mOnCancelListener;
                try {
                    OnCancelListener listener2 = this.mCancellationSignalObj;
                } catch (Throwable th2) {
                    Throwable th3 = th2;
                    OnCancelListener onCancelListener = null;
                    th = th3;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                Object obj = null;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    public void setOnCancelListener(OnCancelListener listener) {
        synchronized (this) {
            waitForCancelFinishedLocked();
            if (this.mOnCancelListener == listener) {
                return;
            }
            this.mOnCancelListener = listener;
            if (this.mIsCanceled) {
                if (listener != null) {
                    listener.onCancel();
                    return;
                }
            }
        }
    }

    public Object getCancellationSignalObject() {
        if (VERSION.SDK_INT < 16) {
            return null;
        }
        Object obj;
        synchronized (this) {
            if (this.mCancellationSignalObj == null) {
                this.mCancellationSignalObj = new android.os.CancellationSignal();
                if (this.mIsCanceled) {
                    ((android.os.CancellationSignal) this.mCancellationSignalObj).cancel();
                }
            }
            obj = this.mCancellationSignalObj;
        }
        return obj;
    }

    private void waitForCancelFinishedLocked() {
        while (this.mCancelInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }
}
