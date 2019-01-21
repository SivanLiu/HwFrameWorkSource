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
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            if (pri < 1) {
                pri = 1;
            }
            if (pri > 10) {
                pri = 10;
            }
            this.maxPriority = this.parent != null ? Math.min(pri, this.parent.maxPriority) : pri;
            ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].setMaxPriority(pri);
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

    /* JADX WARNING: Missing block: B:11:0x001c, code skipped:
            if (r1 >= r2) goto L_0x0028;
     */
    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            r0 = r0 + r3[r1].activeCount();
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int activeCount() {
        synchronized (this) {
            int i = 0;
            if (this.destroyed) {
                return 0;
            }
            int result = this.nthreads;
            int ngroupsSnapshot = this.ngroups;
            ThreadGroup[] groupsSnapshot;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
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

    /* JADX WARNING: Missing block: B:33:0x0049, code skipped:
            if (r10 == false) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:34:0x004c, code skipped:
            r9 = r3;
     */
    /* JADX WARNING: Missing block: B:35:0x004d, code skipped:
            if (r9 >= r0) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:36:0x004f, code skipped:
            r4 = r1[r9].enumerate(r8, r4, true);
            r3 = r9 + 1;
     */
    /* JADX WARNING: Missing block: B:37:0x0059, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int enumerate(Thread[] list, int n, boolean recurse) {
        Throwable th;
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            try {
                int i = 0;
                if (this.destroyed) {
                    return 0;
                }
                int nt = this.nthreads;
                if (nt > list.length - n) {
                    nt = list.length - n;
                }
                int n2 = n;
                n = 0;
                while (n < nt) {
                    try {
                        if (this.threads[n].isAlive()) {
                            int n3 = n2 + 1;
                            try {
                                list[n2] = this.threads[n];
                                n2 = n3;
                            } catch (Throwable th2) {
                                th = th2;
                                n = n3;
                                throw th;
                            }
                        }
                        n++;
                    } catch (Throwable th3) {
                        th = th3;
                        n = n2;
                        throw th;
                    }
                }
                if (recurse) {
                    ThreadGroup[] threadGroupArr;
                    ngroupsSnapshot = this.ngroups;
                    if (this.groups != null) {
                        threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
                    } else {
                        threadGroupArr = null;
                    }
                    groupsSnapshot = threadGroupArr;
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0019, code skipped:
            r3 = r0;
     */
    /* JADX WARNING: Missing block: B:12:0x001b, code skipped:
            if (r1 >= r0) goto L_0x0027;
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            r3 = r3 + r2[r1].activeGroupCount();
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:14:0x0027, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int activeGroupCount() {
        synchronized (this) {
            int i = 0;
            if (this.destroyed) {
                return 0;
            }
            int ngroupsSnapshot = this.ngroups;
            ThreadGroup[] groupsSnapshot;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
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

    /* JADX WARNING: Missing block: B:18:0x0030, code skipped:
            if (r8 == false) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:19:0x0033, code skipped:
            r2 = r3;
     */
    /* JADX WARNING: Missing block: B:20:0x0034, code skipped:
            if (r2 >= r0) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:21:0x0036, code skipped:
            r7 = r1[r2].enumerate(r6, r7, true);
            r3 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:22:0x0040, code skipped:
            return r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int enumerate(ThreadGroup[] list, int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            int i = 0;
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
                    groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
                } else {
                    groupsSnapshot = null;
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
        int i;
        int i2;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            i = 0;
            for (i2 = 0; i2 < this.nthreads; i2++) {
                this.threads[i2].interrupt();
            }
            i2 = this.ngroups;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
            } else {
                groupsSnapshot = null;
            }
        }
        while (i < i2) {
            groupsSnapshot[i].interrupt();
            i++;
        }
    }

    @Deprecated
    public final void suspend() {
        if (stopOrSuspend(true)) {
            Thread.currentThread().suspend();
        }
    }

    /* JADX WARNING: Missing block: B:19:0x003d, code skipped:
            r5 = r4;
            r4 = 0;
     */
    /* JADX WARNING: Missing block: B:20:0x003f, code skipped:
            if (r4 >= r0) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:22:0x0047, code skipped:
            if (r2[r4].stopOrSuspend(r8) != false) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:23:0x0049, code skipped:
            if (r5 == false) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:24:0x004c, code skipped:
            r6 = false;
     */
    /* JADX WARNING: Missing block: B:25:0x004e, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:26:0x004f, code skipped:
            r5 = r6;
            r4 = r4 + 1;
     */
    /* JADX WARNING: Missing block: B:27:0x0053, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean stopOrSuspend(boolean suspend) {
        boolean suicide;
        Thread us = Thread.currentThread();
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            boolean suicide2;
            try {
                checkAccess();
                suicide2 = false;
                int i = 0;
                while (i < this.nthreads) {
                    try {
                        if (this.threads[i] == us) {
                            suicide2 = true;
                        } else if (suspend) {
                            this.threads[i].suspend();
                        } else {
                            this.threads[i].stop();
                        }
                        i++;
                    } catch (Throwable th) {
                        suicide = th;
                        throw suicide;
                    }
                }
                i = this.ngroups;
                if (this.groups != null) {
                    groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, i);
                }
            } catch (Throwable th2) {
                suicide2 = false;
                suicide = th2;
                throw suicide;
            }
        }
    }

    @Deprecated
    public final void resume() {
        int i;
        int i2;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            i = 0;
            for (i2 = 0; i2 < this.nthreads; i2++) {
                this.threads[i2].resume();
            }
            i2 = this.ngroups;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
            } else {
                groupsSnapshot = null;
            }
        }
        while (i < i2) {
            groupsSnapshot[i].resume();
            i++;
        }
    }

    public final void destroy() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        int i;
        synchronized (this) {
            checkAccess();
            if (this.destroyed || this.nthreads > 0) {
                throw new IllegalThreadStateException();
            }
            ngroupsSnapshot = this.ngroups;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
            i = 0;
            if (this.parent != null) {
                this.destroyed = true;
                this.ngroups = 0;
                this.groups = null;
                this.nthreads = 0;
                this.threads = null;
            }
        }
        while (true) {
            int i2 = i;
            if (i2 >= ngroupsSnapshot) {
                break;
            }
            groupsSnapshot[i2].destroy();
            i = i2 + 1;
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

    /* JADX WARNING: Missing block: B:26:0x004a, code skipped:
            return;
     */
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

    /* JADX WARNING: Missing block: B:14:0x002e, code skipped:
            return;
     */
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
        int j;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            i = 0;
            j = 0;
            while (j < indent) {
                try {
                    out.print(" ");
                    j++;
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
            out.println((Object) this);
            indent += 4;
            for (j = 0; j < this.nthreads; j++) {
                for (int j2 = 0; j2 < indent; j2++) {
                    out.print(" ");
                }
                out.println(this.threads[j]);
            }
            j = this.ngroups;
            if (this.groups != null) {
                groupsSnapshot = (ThreadGroup[]) Arrays.copyOf(this.groups, j);
            } else {
                groupsSnapshot = null;
            }
        }
        while (i < j) {
            groupsSnapshot[i].list(out, indent);
            i++;
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
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in thread \"");
            stringBuilder.append(t.getName());
            stringBuilder.append("\" ");
            printStream.print(stringBuilder.toString());
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[name=");
        stringBuilder.append(getName());
        stringBuilder.append(",maxpri=");
        stringBuilder.append(this.maxPriority);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
