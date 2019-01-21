package java.util;

/* compiled from: Timer */
class TimerThread extends Thread {
    boolean newTasksMayBeScheduled = true;
    private TaskQueue queue;

    TimerThread(TaskQueue queue) {
        this.queue = queue;
    }

    public void run() {
        try {
            mainLoop();
            synchronized (this.queue) {
                this.newTasksMayBeScheduled = false;
                this.queue.clear();
            }
        } catch (Throwable th) {
            synchronized (this.queue) {
                this.newTasksMayBeScheduled = false;
                this.queue.clear();
            }
        }
    }

    /* JADX WARNING: Missing block: B:44:0x0077, code skipped:
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:45:0x0078, code skipped:
            if (r6 == false) goto L_0x0085;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            r0.run();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void mainLoop() {
        while (true) {
            try {
                synchronized (this.queue) {
                    while (this.queue.isEmpty() && this.newTasksMayBeScheduled) {
                        this.queue.wait();
                    }
                    if (this.queue.isEmpty()) {
                        return;
                    }
                    TimerTask task = this.queue.getMin();
                    synchronized (task.lock) {
                        if (task.state == 3) {
                            this.queue.removeMin();
                        } else {
                            long currentTime = System.currentTimeMillis();
                            long executionTime = task.nextExecutionTime;
                            boolean z = executionTime <= currentTime;
                            boolean taskFired = z;
                            if (z) {
                                if (task.period == 0) {
                                    this.queue.removeMin();
                                    task.state = 2;
                                } else {
                                    long j;
                                    TaskQueue taskQueue = this.queue;
                                    if (task.period < 0) {
                                        j = currentTime - task.period;
                                    } else {
                                        j = task.period + executionTime;
                                    }
                                    taskQueue.rescheduleMin(j);
                                }
                            }
                            long currentTime2 = currentTime;
                            long executionTime2 = executionTime;
                            boolean taskFired2 = taskFired;
                            if (!taskFired2) {
                                this.queue.wait(executionTime2 - currentTime2);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
