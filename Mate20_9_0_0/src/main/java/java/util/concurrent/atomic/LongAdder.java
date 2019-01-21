package java.util.concurrent.atomic;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class LongAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        private final long value;

        SerializationProxy(LongAdder a) {
            this.value = a.sum();
        }

        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = this.value;
            return a;
        }
    }

    public void add(long x) {
        Cell[] cellArr = this.cells;
        Cell[] as = cellArr;
        if (cellArr == null) {
            long j = this.base;
            if (casBase(j, j + x)) {
                return;
            }
        }
        boolean uncontended = true;
        if (as != null) {
            int length = as.length - 1;
            int m = length;
            if (length >= 0) {
                Cell cell = as[Striped64.getProbe() & m];
                Cell a = cell;
                if (cell != null) {
                    long j2 = a.value;
                    boolean cas = a.cas(j2, j2 + x);
                    uncontended = cas;
                    if (cas) {
                        return;
                    }
                }
            }
        }
        longAccumulate(x, null, uncontended);
    }

    public void increment() {
        add(1);
    }

    public void decrement() {
        add(-1);
    }

    public long sum() {
        Cell[] as = this.cells;
        long sum = this.base;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    sum += a.value;
                }
            }
        }
        return sum;
    }

    public void reset() {
        Cell[] as = this.cells;
        this.base = 0;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    a.reset();
                }
            }
        }
    }

    public long sumThenReset() {
        Cell[] as = this.cells;
        long sum = this.base;
        this.base = 0;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    sum += a.value;
                    a.reset();
                }
            }
        }
        return sum;
    }

    public String toString() {
        return Long.toString(sum());
    }

    public long longValue() {
        return sum();
    }

    public int intValue() {
        return (int) sum();
    }

    public float floatValue() {
        return (float) sum();
    }

    public double doubleValue() {
        return (double) sum();
    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
