package java.util.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import sun.misc.Unsafe;

public class ForkJoinWorkerThread extends Thread {
    private static final long INHERITABLETHREADLOCALS;
    private static final long INHERITEDACCESSCONTROLCONTEXT;
    private static final long THREADLOCALS;
    private static final Unsafe U = Unsafe.getUnsafe();
    final ForkJoinPool pool;
    final WorkQueue workQueue;

    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {
        private static final AccessControlContext INNOCUOUS_ACC = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, null)});
        private static final ThreadGroup innocuousThreadGroup = createThreadGroup();

        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool, innocuousThreadGroup, INNOCUOUS_ACC);
        }

        void afterTopLevelExec() {
            eraseThreadLocals();
        }

        public ClassLoader getContextClassLoader() {
            return ClassLoader.getSystemClassLoader();
        }

        public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) {
        }

        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }

        private static ThreadGroup createThreadGroup() {
            try {
                Unsafe u = Unsafe.getUnsafe();
                long tg = u.objectFieldOffset(Thread.class.getDeclaredField("group"));
                long gp = u.objectFieldOffset(ThreadGroup.class.getDeclaredField("parent"));
                ThreadGroup group = (ThreadGroup) u.getObject(Thread.currentThread(), tg);
                while (group != null) {
                    ThreadGroup parent = (ThreadGroup) u.getObject(group, gp);
                    if (parent == null) {
                        return new ThreadGroup(group, "InnocuousForkJoinWorkerThreadGroup");
                    }
                    group = parent;
                }
                throw new Error("Cannot create ThreadGroup");
            } catch (Throwable e) {
                throw new Error(e);
            }
        }
    }

    public void run() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:18:0x0019
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.modifyBlocksTree(BlockProcessor.java:248)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r5 = this;
        r3 = r5.workQueue;
        r3 = r3.array;
        if (r3 != 0) goto L_0x0019;
    L_0x0006:
        r2 = 0;
        r5.onStart();	 Catch:{ Throwable -> 0x0029, all -> 0x0045 }
        r3 = r5.pool;	 Catch:{ Throwable -> 0x0029, all -> 0x0045 }
        r4 = r5.workQueue;	 Catch:{ Throwable -> 0x0029, all -> 0x0045 }
        r3.runWorker(r4);	 Catch:{ Throwable -> 0x0029, all -> 0x0045 }
        r5.onTermination(r2);
        r3 = r5.pool;
        r3.deregisterWorker(r5, r2);
    L_0x0019:
        return;
    L_0x001a:
        r0 = move-exception;
        r2 = r0;
        r3 = r5.pool;
        r3.deregisterWorker(r5, r2);
        goto L_0x0019;
    L_0x0022:
        r3 = move-exception;
        r4 = r5.pool;
        r4.deregisterWorker(r5, r2);
        throw r3;
    L_0x0029:
        r0 = move-exception;
        r2 = r0;
        r5.onTermination(r0);
        r3 = r5.pool;
        r3.deregisterWorker(r5, r0);
        goto L_0x0019;
    L_0x0034:
        r1 = move-exception;
        if (r0 != 0) goto L_0x0038;
    L_0x0037:
        r2 = r1;
    L_0x0038:
        r3 = r5.pool;
        r3.deregisterWorker(r5, r2);
        goto L_0x0019;
    L_0x003e:
        r3 = move-exception;
        r4 = r5.pool;
        r4.deregisterWorker(r5, r0);
        throw r3;
    L_0x0045:
        r3 = move-exception;
        r5.onTermination(r2);	 Catch:{ Throwable -> 0x004f, all -> 0x0057 }
        r4 = r5.pool;
        r4.deregisterWorker(r5, r2);
    L_0x004e:
        throw r3;
    L_0x004f:
        r0 = move-exception;
        r2 = r0;
        r4 = r5.pool;
        r4.deregisterWorker(r5, r2);
        goto L_0x004e;
    L_0x0057:
        r3 = move-exception;
        r4 = r5.pool;
        r4.deregisterWorker(r5, r2);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.ForkJoinWorkerThread.run():void");
    }

    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    ForkJoinWorkerThread(ForkJoinPool pool, ThreadGroup threadGroup, AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        U.putOrderedObject(this, INHERITEDACCESSCONTROLCONTEXT, acc);
        eraseThreadLocals();
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    public ForkJoinPool getPool() {
        return this.pool;
    }

    public int getPoolIndex() {
        return this.workQueue.getPoolIndex();
    }

    protected void onStart() {
    }

    protected void onTermination(Throwable exception) {
    }

    final void eraseThreadLocals() {
        U.putObject(this, THREADLOCALS, null);
        U.putObject(this, INHERITABLETHREADLOCALS, null);
    }

    void afterTopLevelExec() {
    }

    static {
        try {
            THREADLOCALS = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocals"));
            INHERITABLETHREADLOCALS = U.objectFieldOffset(Thread.class.getDeclaredField("inheritableThreadLocals"));
            INHERITEDACCESSCONTROLCONTEXT = U.objectFieldOffset(Thread.class.getDeclaredField("inheritedAccessControlContext"));
        } catch (Throwable e) {
            throw new Error(e);
        }
    }
}
