package java.util.concurrent.atomic;

import java.io.Serializable;
import sun.misc.Unsafe;

public class AtomicBoolean implements Serializable {
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long VALUE;
    private static final long serialVersionUID = 4654671469794556979L;
    private volatile int value;

    static {
        try {
            VALUE = U.objectFieldOffset(AtomicBoolean.class.getDeclaredField("value"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    public AtomicBoolean(boolean initialValue) {
        this.value = initialValue;
    }

    public final boolean get() {
        return this.value != 0;
    }

    public final boolean compareAndSet(boolean expect, boolean update) {
        return U.compareAndSwapInt(this, VALUE, expect, update);
    }

    public boolean weakCompareAndSet(boolean expect, boolean update) {
        return U.compareAndSwapInt(this, VALUE, expect, update);
    }

    public final void set(boolean newValue) {
        this.value = newValue;
    }

    public final void lazySet(boolean newValue) {
        U.putOrderedInt(this, VALUE, newValue);
    }

    public final boolean getAndSet(boolean newValue) {
        boolean prev;
        do {
            prev = get();
        } while (!compareAndSet(prev, newValue));
        return prev;
    }

    public String toString() {
        return Boolean.toString(get());
    }
}
