package org.apache.http.impl.conn.tsccm;

import java.lang.ref.ReferenceQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class RefQueueWorker implements Runnable {
    private final Log log = LogFactory.getLog(getClass());
    protected final RefQueueHandler refHandler;
    protected final ReferenceQueue<?> refQueue;
    protected volatile Thread workerThread;

    public RefQueueWorker(ReferenceQueue<?> queue, RefQueueHandler handler) {
        if (queue == null) {
            throw new IllegalArgumentException("Queue must not be null.");
        } else if (handler != null) {
            this.refQueue = queue;
            this.refHandler = handler;
        } else {
            throw new IllegalArgumentException("Handler must not be null.");
        }
    }

    public void run() {
        if (this.workerThread == null) {
            this.workerThread = Thread.currentThread();
        }
        while (this.workerThread == Thread.currentThread()) {
            try {
                this.refHandler.handleReference(this.refQueue.remove());
            } catch (InterruptedException e) {
                if (this.log.isDebugEnabled()) {
                    Log log = this.log;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(toString());
                    stringBuilder.append(" interrupted");
                    log.debug(stringBuilder.toString(), e);
                }
            }
        }
    }

    public void shutdown() {
        Thread wt = this.workerThread;
        if (wt != null) {
            this.workerThread = null;
            wt.interrupt();
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RefQueueWorker::");
        stringBuilder.append(this.workerThread);
        return stringBuilder.toString();
    }
}
