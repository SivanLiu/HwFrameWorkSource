package java.util.concurrent.atomic;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.function.LongBinaryOperator;

public class LongAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    private final LongBinaryOperator function;
    private final long identity;

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        private final LongBinaryOperator function;
        private final long identity;
        private final long value;

        SerializationProxy(long value, LongBinaryOperator function, long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }

        private Object readResolve() {
            LongAccumulator a = new LongAccumulator(this.function, this.identity);
            a.base = this.value;
            return a;
        }
    }

    public LongAccumulator(LongBinaryOperator accumulatorFunction, long identity) {
        this.function = accumulatorFunction;
        this.identity = identity;
        this.base = identity;
    }

    public void accumulate(long x) {
        long r;
        Cell[] cellArr = this.cells;
        Cell[] as = cellArr;
        if (cellArr == null) {
            LongBinaryOperator longBinaryOperator = this.function;
            long j = this.base;
            long b = j;
            j = longBinaryOperator.applyAsLong(j, x);
            r = j;
            if (j == b || casBase(b, r)) {
                return;
            }
        }
        boolean uncontended = true;
        if (as != null) {
            boolean z = true;
            int length = as.length - 1;
            int m = length;
            if (length >= 0) {
                Cell cell = as[Striped64.getProbe() & m];
                Cell a = cell;
                if (cell != null) {
                    LongBinaryOperator longBinaryOperator2 = this.function;
                    r = a.value;
                    long v = r;
                    r = longBinaryOperator2.applyAsLong(r, x);
                    long r2 = r;
                    if (!(r == v || a.cas(v, r2))) {
                        z = false;
                    }
                    uncontended = z;
                    if (z) {
                        return;
                    }
                }
            }
        }
        longAccumulate(x, this.function, uncontended);
    }

    public long get() {
        Cell[] as = this.cells;
        long result = this.base;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    result = this.function.applyAsLong(result, a.value);
                }
            }
        }
        return result;
    }

    public void reset() {
        Cell[] as = this.cells;
        this.base = this.identity;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    a.reset(this.identity);
                }
            }
        }
    }

    public long getThenReset() {
        Cell[] as = this.cells;
        long result = this.base;
        this.base = this.identity;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    long v = a.value;
                    a.reset(this.identity);
                    result = this.function.applyAsLong(result, v);
                }
            }
        }
        return result;
    }

    public String toString() {
        return Long.toString(get());
    }

    public long longValue() {
        return get();
    }

    public int intValue() {
        return (int) get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }

    private Object writeReplace() {
        return new SerializationProxy(get(), this.function, this.identity);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
