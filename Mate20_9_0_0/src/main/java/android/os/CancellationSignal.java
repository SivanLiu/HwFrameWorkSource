package android.os;

import android.os.ICancellationSignal.Stub;

public final class CancellationSignal {
    private boolean mCancelInProgress;
    private boolean mIsCanceled;
    private OnCancelListener mOnCancelListener;
    private ICancellationSignal mRemote;

    public interface OnCancelListener {
        void onCancel();
    }

    private static final class Transport extends Stub {
        final CancellationSignal mCancellationSignal;

        private Transport() {
            this.mCancellationSignal = new CancellationSignal();
        }

        public void cancel() throws RemoteException {
            this.mCancellationSignal.cancel();
        }
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

    /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            if (r0 == null) goto L_0x001a;
     */
    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r0.onCancel();
     */
    /* JADX WARNING: Missing block: B:13:0x001a, code skipped:
            if (r1 == null) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:15:?, code skipped:
            r1.cancel();
     */
    /* JADX WARNING: Missing block: B:16:0x0020, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            r4.mCancelInProgress = false;
            notifyAll();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void cancel() {
        synchronized (this) {
            if (this.mIsCanceled) {
                return;
            }
            this.mIsCanceled = true;
            this.mCancelInProgress = true;
            OnCancelListener listener = this.mOnCancelListener;
            ICancellationSignal remote = this.mRemote;
        }
        synchronized (this) {
            this.mCancelInProgress = false;
            notifyAll();
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

    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r2.cancel();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setRemote(ICancellationSignal remote) {
        synchronized (this) {
            waitForCancelFinishedLocked();
            if (this.mRemote == remote) {
                return;
            }
            this.mRemote = remote;
            if (this.mIsCanceled) {
                if (remote == null) {
                }
            }
        }
    }

    private void waitForCancelFinishedLocked() {
        while (this.mCancelInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public static ICancellationSignal createTransport() {
        return new Transport();
    }

    public static CancellationSignal fromTransport(ICancellationSignal transport) {
        if (transport instanceof Transport) {
            return ((Transport) transport).mCancellationSignal;
        }
        return null;
    }
}
