package java.lang;

import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import sun.misc.VM;

public class ThreadGroup implements UncaughtExceptionHandler {
    static final ThreadGroup mainThreadGroup = new ThreadGroup(systemThreadGroup, "main");
    static final ThreadGroup systemThreadGroup = new ThreadGroup();
    boolean daemon;
    boolean destroyed;
    ThreadGroup[] groups;
    int maxPriority;
    int nUnstartedThreads;
    String name;
    int ngroups;
    int nthreads;
    private final ThreadGroup parent;
    Thread[] threads;
    boolean vmAllowSuspension;

    private ThreadGroup() {
        this.nUnstartedThreads = 0;
        this.name = "system";
        this.maxPriority = 10;
        this.parent = null;
    }

    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    public ThreadGroup(ThreadGroup parent, String name) {
        this(checkParentAccess(parent), parent, name);
    }

    private ThreadGroup(Void unused, ThreadGroup parent, String name) {
        this.nUnstartedThreads = 0;
        this.name = name;
        this.maxPriority = parent.maxPriority;
        this.daemon = parent.daemon;
        this.vmAllowSuspension = parent.vmAllowSuspension;
        this.parent = parent;
        parent.add(this);
    }

    private static Void checkParentAccess(ThreadGroup parent) {
        parent.checkAccess();
        return null;
    }

    public final String getName() {
        return this.name;
    }

    public final ThreadGroup getParent() {
        if (this.parent != null) {
            this.parent.checkAccess();
        }
        return this.parent;
    }

    public final int getMaxPriority() {
        return this.maxPriority;
    }

    public final boolean isDaemon() {
        return this.daemon;
    }

    public synchronized boolean isDestroyed() {
        return this.destroyed;
    }

    public final void setDaemon(boolean daemon) {
        checkAccess();
        this.daemon = daemon;
    }

    public final void setMaxPriority(int pri) {
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            int min;
            checkAccess();
            if (pri < 1) {
                pri = 1;
            }
            if (pri > 10) {
                pri = 10;
            }
            if (this.parent != null) {
                min = Math.min(pri, this.parent.maxPriority);
            } else {
                min = pri;
            }
            this.maxPriority = min;
            int ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            threadGroupArr[i].setMaxPriority(pri);
        }
    }

    public final boolean parentOf(ThreadGroup g) {
        while (g != null) {
            if (g == this) {
                return true;
            }
            g = g.parent;
        }
        return false;
    }

    public final void checkAccess() {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int activeCount() {
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int result = this.nthreads;
            int ngroupsSnapshot = this.ngroups;
            ThreadGroup[] threadGroupArr;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
        }
    }

    public int enumerate(Thread[] list) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    public int enumerate(Thread[] list, boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int enumerate(Thread[] list, int n, boolean recurse) {
        Throwable th;
        int ngroupsSnapshot = 0;
        ThreadGroup[] threadGroupArr = null;
        synchronized (this) {
            try {
                if (this.destroyed) {
                    return 0;
                }
                int nt = this.nthreads;
                if (nt > list.length - n) {
                    nt = list.length - n;
                }
                int i = 0;
                int n2 = n;
                while (i < nt) {
                    try {
                        if (this.threads[i].isAlive()) {
                            n = n2 + 1;
                            list[n2] = this.threads[i];
                        } else {
                            n = n2;
                        }
                        i++;
                        n2 = n;
                    } catch (Throwable th2) {
                        th = th2;
                        n = n2;
                    }
                }
                if (recurse) {
                    ngroupsSnapshot = this.ngroups;
                    if (this.groups != null) {
                        threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
                    } else {
                        threadGroupArr = null;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int activeGroupCount() {
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int ngroupsSnapshot = this.ngroups;
            ThreadGroup[] threadGroupArr;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
        }
    }

    public int enumerate(ThreadGroup[] list) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    public int enumerate(ThreadGroup[] list, boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int enumerate(ThreadGroup[] list, int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] threadGroupArr = null;
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int ng = this.ngroups;
            if (ng > list.length - n) {
                ng = list.length - n;
            }
            if (ng > 0) {
                System.arraycopy(this.groups, 0, (Object) list, n, ng);
                n += ng;
            }
            if (recurse) {
                ngroupsSnapshot = this.ngroups;
                if (this.groups != null) {
                    threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
                } else {
                    threadGroupArr = null;
                }
            }
        }
    }

    @Deprecated
    public final void stop() {
        if (stopOrSuspend(false)) {
            Thread.currentThread().stop();
        }
    }

    public final void interrupt() {
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            int i;
            checkAccess();
            for (i = 0; i < this.nthreads; i++) {
                this.threads[i].interrupt();
            }
            int ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
        }
        for (i = 0; i < ngroupsSnapshot; i++) {
            threadGroupArr[i].interrupt();
        }
    }

    @Deprecated
    public final void suspend() {
        if (stopOrSuspend(true)) {
            Thread.currentThread().suspend();
        }
    }

    private boolean stopOrSuspend(boolean suspend) {
        int i;
        boolean suicide = false;
        Thread us = Thread.currentThread();
        ThreadGroup[] threadGroupArr = null;
        synchronized (this) {
            checkAccess();
            for (i = 0; i < this.nthreads; i++) {
                if (this.threads[i] == us) {
                    suicide = true;
                } else if (suspend) {
                    this.threads[i].suspend();
                } else {
                    this.threads[i].stop();
                }
            }
            int ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            }
        }
        for (i = 0; i < ngroupsSnapshot; i++) {
            if (threadGroupArr[i].stopOrSuspend(suspend)) {
                suicide = true;
            }
        }
        return suicide;
    }

    @Deprecated
    public final void resume() {
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            int i;
            checkAccess();
            for (i = 0; i < this.nthreads; i++) {
                this.threads[i].resume();
            }
            int ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
        }
        for (i = 0; i < ngroupsSnapshot; i++) {
            threadGroupArr[i].resume();
        }
    }

    public final void destroy() {
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            checkAccess();
            if (this.destroyed || this.nthreads > 0) {
                throw new IllegalThreadStateException();
            }
            int ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
            if (this.parent != null) {
                this.destroyed = true;
                this.ngroups = 0;
                this.groups = null;
                this.nthreads = 0;
                this.threads = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            threadGroupArr[i].destroy();
        }
        if (this.parent != null) {
            this.parent.remove(this);
        }
    }

    private final void add(ThreadGroup g) {
        synchronized (this) {
            if (this.destroyed) {
                throw new IllegalThreadStateException();
            }
            if (this.groups == null) {
                this.groups = new ThreadGroup[4];
            } else if (this.ngroups == this.groups.length) {
                this.groups = (ThreadGroup[]) Arrays.copyOf(this.groups, this.ngroups * 2);
            }
            this.groups[this.ngroups] = g;
            this.ngroups++;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void remove(ThreadGroup g) {
        synchronized (this) {
            if (this.destroyed) {
                return;
            }
            for (int i = 0; i < this.ngroups; i++) {
                if (this.groups[i] == g) {
                    this.ngroups--;
                    System.arraycopy(this.groups, i + 1, this.groups, i, this.ngroups - i);
                    this.groups[this.ngroups] = null;
                    break;
                }
            }
            if (this.nthreads == 0) {
                notifyAll();
            }
            if (this.daemon && this.nthreads == 0 && this.nUnstartedThreads == 0 && this.ngroups == 0) {
                destroy();
            }
        }
    }

    void addUnstarted() {
        synchronized (this) {
            if (this.destroyed) {
                throw new IllegalThreadStateException();
            }
            this.nUnstartedThreads++;
        }
    }

    void add(Thread t) {
        synchronized (this) {
            if (this.destroyed) {
                throw new IllegalThreadStateException();
            }
            if (this.threads == null) {
                this.threads = new Thread[4];
            } else if (this.nthreads == this.threads.length) {
                this.threads = (Thread[]) Arrays.copyOf(this.threads, this.nthreads * 2);
            }
            this.threads[this.nthreads] = t;
            this.nthreads++;
            this.nUnstartedThreads--;
        }
    }

    void threadStartFailed(Thread t) {
        synchronized (this) {
            remove(t);
            this.nUnstartedThreads++;
        }
    }

    void threadTerminated(Thread t) {
        synchronized (this) {
            remove(t);
            if (this.nthreads == 0) {
                notifyAll();
            }
            if (this.daemon && this.nthreads == 0 && this.nUnstartedThreads == 0 && this.ngroups == 0) {
                destroy();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void remove(Thread t) {
        synchronized (this) {
            if (this.destroyed) {
                return;
            }
            for (int i = 0; i < this.nthreads; i++) {
                if (this.threads[i] == t) {
                    Object obj = this.threads;
                    int i2 = i + 1;
                    Object obj2 = this.threads;
                    int i3 = this.nthreads - 1;
                    this.nthreads = i3;
                    System.arraycopy(obj, i2, obj2, i, i3 - i);
                    this.threads[this.nthreads] = null;
                    break;
                }
            }
        }
    }

    public void list() {
        list(System.out, 0);
    }

    void list(PrintStream out, int indent) {
        int i;
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            int j;
            for (j = 0; j < indent; j++) {
                out.print(" ");
            }
            out.println((Object) this);
            indent += 4;
            for (i = 0; i < this.nthreads; i++) {
                for (j = 0; j < indent; j++) {
                    out.print(" ");
                }
                out.println(this.threads[i]);
            }
            int ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                threadGroupArr = null;
            }
        }
        for (i = 0; i < ngroupsSnapshot; i++) {
            threadGroupArr[i].list(out, indent);
        }
    }

    public void uncaughtException(Thread t, Throwable e) {
        if (this.parent != null) {
            this.parent.uncaughtException(t, e);
            return;
        }
        UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        if (ueh != null) {
            ueh.uncaughtException(t, e);
        } else if (!(e instanceof ThreadDeath)) {
            System.err.print("Exception in thread \"" + t.getName() + "\" ");
            e.printStackTrace(System.err);
        }
    }

    @Deprecated
    public boolean allowThreadSuspension(boolean b) {
        this.vmAllowSuspension = b;
        if (!b) {
            VM.unsuspendSomeThreads();
        }
        return true;
    }

    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxpri=" + this.maxPriority + "]";
    }
}
