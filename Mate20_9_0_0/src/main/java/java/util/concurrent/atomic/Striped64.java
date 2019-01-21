package java.util.concurrent.atomic;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import sun.misc.Unsafe;

abstract class Striped64 extends Number {
    private static final long BASE;
    private static final long CELLSBUSY;
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final long PROBE;
    private static final Unsafe U = Unsafe.getUnsafe();
    volatile transient long base;
    volatile transient Cell[] cells;
    volatile transient int cellsBusy;

    static final class Cell {
        private static final Unsafe U = Unsafe.getUnsafe();
        private static final long VALUE;
        volatile long value;

        Cell(long x) {
            this.value = x;
        }

        final boolean cas(long cmp, long val) {
            return U.compareAndSwapLong(this, VALUE, cmp, val);
        }

        final void reset() {
            U.putLongVolatile(this, VALUE, 0);
        }

        final void reset(long identity) {
            U.putLongVolatile(this, VALUE, identity);
        }

        static {
            try {
                VALUE = U.objectFieldOffset(Cell.class.getDeclaredField("value"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static {
        try {
            BASE = U.objectFieldOffset(Striped64.class.getDeclaredField("base"));
            CELLSBUSY = U.objectFieldOffset(Striped64.class.getDeclaredField("cellsBusy"));
            PROBE = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomProbe"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    Striped64() {
    }

    final boolean casBase(long cmp, long val) {
        return U.compareAndSwapLong(this, BASE, cmp, val);
    }

    final boolean casCellsBusy() {
        return U.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    static final int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }

    static final int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        U.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended) {
        boolean wasUncontended2;
        long j = x;
        LongBinaryOperator longBinaryOperator = fn;
        int probe = getProbe();
        int h = probe;
        if (probe == 0) {
            ThreadLocalRandom.current();
            h = getProbe();
            wasUncontended2 = true;
        } else {
            wasUncontended2 = wasUncontended;
        }
        int i = 0;
        boolean wasUncontended3 = wasUncontended2;
        wasUncontended2 = false;
        while (true) {
            boolean z;
            long j2;
            boolean collide = wasUncontended2;
            Cell[] cellArr = this.cells;
            Cell[] as = cellArr;
            if (cellArr != null) {
                probe = as.length;
                int n = probe;
                if (probe > 0) {
                    Cell cell = as[(n - 1) & h];
                    Cell a = cell;
                    if (cell == null) {
                        if (this.cellsBusy == 0) {
                            Cell r = new Cell(j);
                            if (this.cellsBusy == 0 && casCellsBusy()) {
                                try {
                                    cellArr = this.cells;
                                    Cell[] rs = cellArr;
                                    if (cellArr != null) {
                                        probe = rs.length;
                                        int m = probe;
                                        if (probe > 0) {
                                            probe = (m - 1) & h;
                                            int j3 = probe;
                                            if (rs[probe] == null) {
                                                rs[j3] = r;
                                                this.cellsBusy = i;
                                                z = wasUncontended3;
                                                return;
                                            }
                                        }
                                    }
                                    this.cellsBusy = i;
                                    wasUncontended2 = collide;
                                } catch (Throwable th) {
                                    this.cellsBusy = i;
                                }
                            }
                        }
                        collide = false;
                    } else if (wasUncontended3) {
                        j2 = a.value;
                        long v = j2;
                        z = wasUncontended3;
                        if (!a.cas(j2, longBinaryOperator == null ? v + j : longBinaryOperator.applyAsLong(v, j))) {
                            if (n >= NCPU || this.cells != as) {
                                collide = false;
                                h = advanceProbe(h);
                                wasUncontended2 = collide;
                                wasUncontended3 = z;
                                i = 0;
                            } else {
                                if (!collide) {
                                    collide = true;
                                } else if (this.cellsBusy == 0 && casCellsBusy()) {
                                    try {
                                        if (this.cells == as) {
                                            this.cells = (Cell[]) Arrays.copyOf((Object[]) as, n << 1);
                                        }
                                        i = 0;
                                        this.cellsBusy = 0;
                                        wasUncontended2 = false;
                                        wasUncontended3 = z;
                                    } catch (Throwable th2) {
                                        this.cellsBusy = 0;
                                    }
                                }
                                h = advanceProbe(h);
                                wasUncontended2 = collide;
                                wasUncontended3 = z;
                                i = 0;
                            }
                        } else {
                            return;
                        }
                    } else {
                        wasUncontended3 = true;
                    }
                    z = wasUncontended3;
                    h = advanceProbe(h);
                    wasUncontended2 = collide;
                    wasUncontended3 = z;
                    i = 0;
                }
            }
            z = wasUncontended3;
            if (this.cellsBusy == 0 && this.cells == as && casCellsBusy()) {
                try {
                    if (this.cells == as) {
                        cellArr = new Cell[2];
                        cellArr[h & 1] = new Cell(j);
                        this.cells = cellArr;
                        this.cellsBusy = 0;
                        return;
                    }
                } finally {
                    i = 0;
                    this.cellsBusy = 0;
                }
            } else {
                i = 0;
                long j4 = this.base;
                j2 = j4;
                if (casBase(j4, longBinaryOperator == null ? j2 + j : longBinaryOperator.applyAsLong(j2, j))) {
                    return;
                }
            }
            wasUncontended2 = collide;
            wasUncontended3 = z;
        }
    }

    private static long apply(DoubleBinaryOperator fn, long v, double x) {
        double d = Double.longBitsToDouble(v);
        return Double.doubleToRawLongBits(fn == null ? d + x : fn.applyAsDouble(d, x));
    }

    final void doubleAccumulate(double x, DoubleBinaryOperator fn, boolean wasUncontended) {
        boolean wasUncontended2;
        double d = x;
        DoubleBinaryOperator doubleBinaryOperator = fn;
        int probe = getProbe();
        int h = probe;
        if (probe == 0) {
            ThreadLocalRandom.current();
            h = getProbe();
            wasUncontended2 = true;
        } else {
            wasUncontended2 = wasUncontended;
        }
        int i = 0;
        boolean wasUncontended3 = wasUncontended2;
        wasUncontended2 = false;
        while (true) {
            boolean z;
            boolean collide = wasUncontended2;
            Cell[] cellArr = this.cells;
            Cell[] as = cellArr;
            if (cellArr != null) {
                probe = as.length;
                int n = probe;
                if (probe > 0) {
                    Cell cell = as[(n - 1) & h];
                    Cell a = cell;
                    if (cell == null) {
                        if (this.cellsBusy == 0) {
                            Cell r = new Cell(Double.doubleToRawLongBits(x));
                            if (this.cellsBusy == 0 && casCellsBusy()) {
                                try {
                                    cellArr = this.cells;
                                    Cell[] rs = cellArr;
                                    if (cellArr != null) {
                                        probe = rs.length;
                                        int m = probe;
                                        if (probe > 0) {
                                            probe = (m - 1) & h;
                                            int j = probe;
                                            if (rs[probe] == null) {
                                                rs[j] = r;
                                                this.cellsBusy = i;
                                                z = wasUncontended3;
                                                return;
                                            }
                                        }
                                    }
                                    this.cellsBusy = i;
                                    wasUncontended2 = collide;
                                } catch (Throwable th) {
                                    this.cellsBusy = i;
                                }
                            }
                        }
                        collide = false;
                    } else if (wasUncontended3) {
                        long j2 = a.value;
                        z = wasUncontended3;
                        if (!a.cas(j2, apply(doubleBinaryOperator, j2, d))) {
                            if (n >= NCPU || this.cells != as) {
                                collide = false;
                                h = advanceProbe(h);
                                wasUncontended2 = collide;
                                wasUncontended3 = z;
                                i = 0;
                            } else {
                                if (!collide) {
                                    collide = true;
                                } else if (this.cellsBusy == 0 && casCellsBusy()) {
                                    try {
                                        if (this.cells == as) {
                                            this.cells = (Cell[]) Arrays.copyOf((Object[]) as, n << 1);
                                        }
                                        i = 0;
                                        this.cellsBusy = 0;
                                        wasUncontended2 = false;
                                        wasUncontended3 = z;
                                    } catch (Throwable th2) {
                                        this.cellsBusy = 0;
                                    }
                                }
                                h = advanceProbe(h);
                                wasUncontended2 = collide;
                                wasUncontended3 = z;
                                i = 0;
                            }
                        } else {
                            return;
                        }
                    } else {
                        wasUncontended3 = true;
                    }
                    z = wasUncontended3;
                    h = advanceProbe(h);
                    wasUncontended2 = collide;
                    wasUncontended3 = z;
                    i = 0;
                }
            }
            z = wasUncontended3;
            if (this.cellsBusy == 0 && this.cells == as && casCellsBusy()) {
                try {
                    if (this.cells == as) {
                        cellArr = new Cell[2];
                        cellArr[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                        this.cells = cellArr;
                        this.cellsBusy = 0;
                        return;
                    }
                } finally {
                    i = 0;
                    this.cellsBusy = 0;
                }
            } else {
                i = 0;
                long j3 = this.base;
                if (casBase(j3, apply(doubleBinaryOperator, j3, d))) {
                    return;
                }
            }
            wasUncontended2 = collide;
            wasUncontended3 = z;
        }
    }
}
