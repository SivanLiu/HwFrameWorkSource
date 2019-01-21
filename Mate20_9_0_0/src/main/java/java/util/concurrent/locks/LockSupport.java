package java.util.concurrent.locks;

import java.util.concurrent.ThreadLocalRandom;
import sun.misc.Unsafe;

public class LockSupport {
    private static final long PARKBLOCKER;
    private static final long SECONDARY;
    private static final Unsafe U = Unsafe.getUnsafe();

    private LockSupport() {
    }

    private static void setBlocker(Thread t, Object arg) {
        U.putObject(t, PARKBLOCKER, arg);
    }

    public static void unpark(Thread thread) {
        if (thread != null) {
            U.unpark(thread);
        }
    }

    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(false, 0);
        setBlocker(t, null);
    }

    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            U.park(false, nanos);
            setBlocker(t, null);
        }
    }

    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(true, deadline);
        setBlocker(t, null);
    }

    public static Object getBlocker(Thread t) {
        if (t != null) {
            return U.getObjectVolatile(t, PARKBLOCKER);
        }
        throw new NullPointerException();
    }

    public static void park() {
        U.park(false, 0);
    }

    public static void parkNanos(long nanos) {
        if (nanos > 0) {
            U.park(false, nanos);
        }
    }

    public static void parkUntil(long deadline) {
        U.park(true, deadline);
    }

    static final int nextSecondarySeed() {
        Thread t = Thread.currentThread();
        int i = U.getInt(t, SECONDARY);
        int r = i;
        if (i != 0) {
            i = (r << 13) ^ r;
            i ^= i >>> 17;
            i ^= i << 5;
        } else {
            i = ThreadLocalRandom.current().nextInt();
            r = i;
            if (i == 0) {
                i = 1;
            } else {
                i = r;
            }
        }
        U.putInt(t, SECONDARY, i);
        return i;
    }

    static {
        try {
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            SECONDARY = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
