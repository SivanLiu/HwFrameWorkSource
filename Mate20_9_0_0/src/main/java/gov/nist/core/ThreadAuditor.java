package gov.nist.core;

import java.util.HashMap;
import java.util.Map;

public class ThreadAuditor {
    private long pingIntervalInMillisecs = 0;
    private Map<Thread, ThreadHandle> threadHandles = new HashMap();

    public class ThreadHandle {
        private boolean isThreadActive = false;
        private Thread thread = Thread.currentThread();
        private ThreadAuditor threadAuditor;

        public ThreadHandle(ThreadAuditor aThreadAuditor) {
            this.threadAuditor = aThreadAuditor;
        }

        public boolean isThreadActive() {
            return this.isThreadActive;
        }

        protected void setThreadActive(boolean value) {
            this.isThreadActive = value;
        }

        public Thread getThread() {
            return this.thread;
        }

        public void ping() {
            this.threadAuditor.ping(this);
        }

        public long getPingIntervalInMillisecs() {
            return this.threadAuditor.getPingIntervalInMillisecs();
        }

        public String toString() {
            StringBuffer toString = new StringBuffer();
            toString.append("Thread Name: ");
            toString.append(this.thread.getName());
            toString.append(", Alive: ");
            return toString.append(this.thread.isAlive()).toString();
        }
    }

    public long getPingIntervalInMillisecs() {
        return this.pingIntervalInMillisecs;
    }

    public void setPingIntervalInMillisecs(long value) {
        this.pingIntervalInMillisecs = value;
    }

    public boolean isEnabled() {
        return this.pingIntervalInMillisecs > 0;
    }

    public synchronized ThreadHandle addCurrentThread() {
        ThreadHandle threadHandle;
        threadHandle = new ThreadHandle(this);
        if (isEnabled()) {
            this.threadHandles.put(Thread.currentThread(), threadHandle);
        }
        return threadHandle;
    }

    public synchronized void removeThread(Thread thread) {
        this.threadHandles.remove(thread);
    }

    public synchronized void ping(ThreadHandle threadHandle) {
        threadHandle.setThreadActive(true);
    }

    public synchronized void reset() {
        this.threadHandles.clear();
    }

    public synchronized String auditThreads() {
        String auditReport;
        auditReport = null;
        for (ThreadHandle threadHandle : this.threadHandles.values()) {
            if (!threadHandle.isThreadActive()) {
                Thread thread = threadHandle.getThread();
                if (auditReport == null) {
                    auditReport = "Thread Auditor Report:\n";
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(auditReport);
                stringBuilder.append("   Thread [");
                stringBuilder.append(thread.getName());
                stringBuilder.append("] has failed to respond to an audit request.\n");
                auditReport = stringBuilder.toString();
            }
            threadHandle.setThreadActive(false);
        }
        return auditReport;
    }

    public synchronized String toString() {
        String toString;
        toString = "Thread Auditor - List of monitored threads:\n";
        for (ThreadHandle threadHandle : this.threadHandles.values()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(toString);
            stringBuilder.append("   ");
            stringBuilder.append(threadHandle.toString());
            stringBuilder.append(Separators.RETURN);
            toString = stringBuilder.toString();
        }
        return toString;
    }
}
