package java.util.concurrent.atomic;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.function.DoubleBinaryOperator;

public class DoubleAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    private final DoubleBinaryOperator function;
    private final long identity;

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        private final DoubleBinaryOperator function;
        private final long identity;
        private final double value;

        SerializationProxy(double value, DoubleBinaryOperator function, long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }

        private Object readResolve() {
            DoubleAccumulator a = new DoubleAccumulator(this.function, Double.longBitsToDouble(this.identity));
            a.base = Double.doubleToRawLongBits(this.value);
            return a;
        }
    }

    public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity) {
        this.function = accumulatorFunction;
        long doubleToRawLongBits = Double.doubleToRawLongBits(identity);
        this.identity = doubleToRawLongBits;
        this.base = doubleToRawLongBits;
    }

    public void accumulate(double x) {
        long r;
        Cell[] cellArr = this.cells;
        Cell[] as = cellArr;
        if (cellArr == null) {
            DoubleBinaryOperator doubleBinaryOperator = this.function;
            long j = this.base;
            long b = j;
            j = Double.doubleToRawLongBits(doubleBinaryOperator.applyAsDouble(Double.longBitsToDouble(j), x));
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
                    DoubleBinaryOperator doubleBinaryOperator2 = this.function;
                    r = a.value;
                    long v = r;
                    r = Double.doubleToRawLongBits(doubleBinaryOperator2.applyAsDouble(Double.longBitsToDouble(r), x));
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
        doubleAccumulate(x, this.function, uncontended);
    }

    public double get() {
        Cell[] as = this.cells;
        double result = Double.longBitsToDouble(this.base);
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    result = this.function.applyAsDouble(result, Double.longBitsToDouble(a.value));
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

    public double getThenReset() {
        Cell[] as = this.cells;
        double result = Double.longBitsToDouble(this.base);
        this.base = this.identity;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    double v = Double.longBitsToDouble(a.value);
                    a.reset(this.identity);
                    result = this.function.applyAsDouble(result, v);
                }
            }
        }
        return result;
    }

    public String toString() {
        return Double.toString(get());
    }

    public double doubleValue() {
        return get();
    }

    public long longValue() {
        return (long) get();
    }

    public int intValue() {
        return (int) get();
    }

    public float floatValue() {
        return (float) get();
    }

    private Object writeReplace() {
        return new SerializationProxy(get(), this.function, this.identity);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
