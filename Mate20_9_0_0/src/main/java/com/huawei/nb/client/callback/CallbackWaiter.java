package com.huawei.nb.client.callback;

import com.huawei.nb.utils.Waiter;
import com.huawei.nb.utils.logger.DSLog;

public class CallbackWaiter<T> {
    private static final int INVALID_TRANSACTION_ID = -1;
    private final T failedValue;
    private final Object locker = new Object();
    private T result = null;
    private Status status = Status.INIT;
    private int transactionId = -1;
    private final Waiter waiter = new Waiter();

    private enum Status {
        INIT,
        WAITING,
        INVOKED
    }

    public CallbackWaiter(T failedValue) {
        this.failedValue = failedValue;
    }

    public void interrupt() {
        this.waiter.interrupt();
    }

    public T await(int transactionId, long milliSeconds) {
        if (transactionId == -1) {
            DSLog.e("Failed to wait callback, error: received invalid transaction id.", new Object[0]);
            return this.failedValue;
        } else if (waitCallbackToBeInvoked(transactionId, milliSeconds)) {
            T t;
            synchronized (this.locker) {
                t = this.result;
            }
            return t;
        } else {
            DSLog.e("Failed to wait callback for transaction %d, error: timeout.", Integer.valueOf(transactionId));
            return this.failedValue;
        }
    }

    private boolean waitCallbackToBeInvoked(int transactionId, long millSeconds) {
        synchronized (this.locker) {
            if (this.status == Status.INVOKED) {
                return true;
            }
            this.transactionId = transactionId;
            this.status = Status.WAITING;
            return this.waiter.await(millSeconds);
        }
    }

    public void set(int transactionId, T result) {
        synchronized (this.locker) {
            if (this.status == Status.INIT) {
                this.result = result;
                this.status = Status.INVOKED;
            } else if (this.status == Status.WAITING && transactionId == this.transactionId) {
                this.result = result;
                this.status = Status.INVOKED;
                this.waiter.signal();
            }
        }
    }
}
