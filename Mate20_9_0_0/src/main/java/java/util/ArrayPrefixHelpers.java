package java.util;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

class ArrayPrefixHelpers {
    static final int CUMULATE = 1;
    static final int FINISHED = 4;
    static final int MIN_PARTITION = 16;
    static final int SUMMED = 2;

    static final class CumulateTask<T> extends CountedCompleter<Void> {
        private static final long serialVersionUID = 5293554502939613543L;
        final T[] array;
        final int fence;
        final BinaryOperator<T> function;
        final int hi;
        T in;
        CumulateTask<T> left;
        final int lo;
        final int origin;
        T out;
        CumulateTask<T> right;
        final int threshold;

        public CumulateTask(CumulateTask<T> parent, BinaryOperator<T> function, T[] array, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = lo;
            this.lo = lo;
            this.fence = hi;
            this.hi = hi;
            int commonPoolParallelism = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            int p = commonPoolParallelism;
            int i = 16;
            if (commonPoolParallelism > 16) {
                i = p;
            }
            this.threshold = i;
        }

        CumulateTask(CumulateTask<T> parent, BinaryOperator<T> function, T[] array, int origin, int fence, int threshold, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = origin;
            this.fence = fence;
            this.threshold = threshold;
            this.lo = lo;
            this.hi = hi;
        }

        /* JADX WARNING: Removed duplicated region for block: B:127:0x019f A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:100:0x0190  */
        /* JADX WARNING: Missing block: B:104:0x01a6, code skipped:
            r24 = r1;
            r8 = r10;
     */
        /* JADX WARNING: Missing block: B:128:?, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public final void compute() {
            CumulateTask<T> thisR;
            BinaryOperator<T> binaryOperator = this.function;
            BinaryOperator<T> fn = binaryOperator;
            if (binaryOperator != null) {
                T[] tArr = this.array;
                T[] a = tArr;
                if (tArr != null) {
                    int th = this.threshold;
                    int org = this.origin;
                    int fnc = this.fence;
                    CumulateTask<T> t = this;
                    while (true) {
                        CumulateTask<T> t2 = t;
                        int i = t2.lo;
                        int l = i;
                        if (i < 0) {
                            break;
                        }
                        i = t2.hi;
                        int h = i;
                        CumulateTask<T> rt;
                        CumulateTask<T> cumulateTask;
                        int i2;
                        int i3;
                        CumulateTask<T> t3;
                        CumulateTask<T> rt2;
                        T lout;
                        if (i > a.length) {
                            break;
                        } else if (h - l > th) {
                            CumulateTask<T> f;
                            CumulateTask<T> lt = t2.left;
                            rt = t2.right;
                            int i4;
                            int l2;
                            CumulateTask<T> rt3;
                            if (lt == null) {
                                int mid = (l + h) >>> 1;
                                cumulateTask = t2;
                                BinaryOperator<T> binaryOperator2 = fn;
                                T[] tArr2 = a;
                                thisR = t;
                                int i5 = org;
                                i4 = fnc;
                                i2 = th;
                                l2 = l;
                                t = new CumulateTask(cumulateTask, binaryOperator2, tArr2, i5, i4, i2, mid, h);
                                t2.right = thisR;
                                rt3 = thisR;
                                CumulateTask<T> f2 = thisR;
                                thisR = t;
                                t = new CumulateTask(cumulateTask, binaryOperator2, tArr2, i5, i4, i2, l2, mid);
                                t2.left = thisR;
                                t2 = thisR;
                                t = thisR;
                                lt = l2;
                                f = f2;
                            } else {
                                rt3 = rt;
                                CumulateTask<T> lt2 = lt;
                                i3 = h;
                                l2 = l;
                                T pin = t2.in;
                                t = lt2;
                                t.in = pin;
                                f = null;
                                t3 = null;
                                rt2 = rt3;
                                if (rt2 != null) {
                                    lout = t.out;
                                    rt2.in = l2 == org ? lout : fn.apply(pin, lout);
                                    do {
                                        h = rt2.getPendingCount();
                                        l = h;
                                        if ((h & 1) != 0) {
                                            break;
                                        }
                                    } while (rt2.compareAndSetPendingCount(l, l | 1) == 0);
                                    t3 = rt2;
                                }
                                do {
                                    i4 = t.getPendingCount();
                                    h = i4;
                                    if ((i4 & 1) != 0) {
                                        t2 = t3;
                                        break;
                                    }
                                } while (!t.compareAndSetPendingCount(h, h | 1));
                                if (t3 != null) {
                                    f = t3;
                                }
                                t2 = t;
                                if (t2 == null) {
                                    break;
                                }
                                rt3 = rt2;
                            }
                            if (f != null) {
                                f.fork();
                            }
                            t = t2;
                        } else {
                            int state;
                            i3 = h;
                            i2 = l;
                            do {
                                state = t2.getPendingCount();
                                i = state;
                                if ((state & 4) != 0) {
                                    break;
                                }
                                state = (i & 1) != 0 ? 4 : i2 > org ? 2 : 6;
                            } while (!t2.compareAndSetPendingCount(i, i | state));
                            if (state != 2) {
                                T sum;
                                if (i2 == org) {
                                    sum = a[org];
                                    l = org + 1;
                                } else {
                                    sum = t2.in;
                                    l = i2;
                                }
                                lout = sum;
                                i = l;
                                while (true) {
                                    h = i3;
                                    if (i >= h) {
                                        break;
                                    }
                                    T apply = fn.apply(lout, a[i]);
                                    lout = apply;
                                    a[i] = apply;
                                    i++;
                                    i3 = h;
                                }
                            } else {
                                h = i3;
                                if (h < fnc) {
                                    l = i2 + 1;
                                    lout = a[i2];
                                    while (true) {
                                        i = l;
                                        if (i >= h) {
                                            break;
                                        }
                                        lout = fn.apply(lout, a[i]);
                                        l = i + 1;
                                    }
                                } else {
                                    lout = t2.in;
                                }
                            }
                            t2.out = lout;
                            while (true) {
                                rt2 = (CumulateTask) t2.getCompleter();
                                rt = rt2;
                                if (rt2 != null) {
                                    int th2;
                                    l = rt.getPendingCount();
                                    if (((l & state) & 4) != 0) {
                                        t2 = rt;
                                        th2 = th;
                                    } else if (((l & state) & 2) != 0) {
                                        int i6;
                                        int nextState;
                                        cumulateTask = rt.left;
                                        CumulateTask<T> lt3 = cumulateTask;
                                        if (cumulateTask != null) {
                                            cumulateTask = rt.right;
                                            CumulateTask<T> rt4 = cumulateTask;
                                            if (cumulateTask != null) {
                                                T lout2 = lt3.out;
                                                th2 = th;
                                                t3 = rt4;
                                                rt.out = t3.hi == fnc ? lout2 : fn.apply(lout2, t3.out);
                                                th = ((l & 1) == 0 || rt.lo != org) ? 0 : 1;
                                                i6 = (l | state) | th;
                                                nextState = i6;
                                                if (i6 != l || rt.compareAndSetPendingCount(l, nextState)) {
                                                    state = 2;
                                                    t2 = rt;
                                                    if (th == 0) {
                                                        rt.fork();
                                                    }
                                                }
                                            }
                                        }
                                        th2 = th;
                                        cumulateTask = lt3;
                                        if ((l & 1) == 0) {
                                        }
                                        i6 = (l | state) | th;
                                        nextState = i6;
                                        if (i6 != l) {
                                        }
                                        state = 2;
                                        t2 = rt;
                                        if (th == 0) {
                                        }
                                    } else {
                                        th2 = th;
                                        if (rt.compareAndSetPendingCount(l, l | state)) {
                                            return;
                                        }
                                    }
                                    th = th2;
                                } else if ((state & 4) != 0) {
                                    t2.quietlyComplete();
                                }
                            }
                        }
                    }
                    return;
                }
            }
            throw new NullPointerException();
        }
    }

    static final class DoubleCumulateTask extends CountedCompleter<Void> {
        private static final long serialVersionUID = -586947823794232033L;
        final double[] array;
        final int fence;
        final DoubleBinaryOperator function;
        final int hi;
        double in;
        DoubleCumulateTask left;
        final int lo;
        final int origin;
        double out;
        DoubleCumulateTask right;
        final int threshold;

        public DoubleCumulateTask(DoubleCumulateTask parent, DoubleBinaryOperator function, double[] array, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = lo;
            this.lo = lo;
            this.fence = hi;
            this.hi = hi;
            int commonPoolParallelism = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            int p = commonPoolParallelism;
            int i = 16;
            if (commonPoolParallelism > 16) {
                i = p;
            }
            this.threshold = i;
        }

        DoubleCumulateTask(DoubleCumulateTask parent, DoubleBinaryOperator function, double[] array, int origin, int fence, int threshold, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = origin;
            this.fence = fence;
            this.threshold = threshold;
            this.lo = lo;
            this.hi = hi;
        }

        /* JADX WARNING: Removed duplicated region for block: B:124:0x01a7 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:98:0x0198  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public final void compute() {
            DoubleCumulateTask thisR;
            DoubleBinaryOperator doubleBinaryOperator = this.function;
            DoubleBinaryOperator fn = doubleBinaryOperator;
            if (doubleBinaryOperator != null) {
                double[] dArr = this.array;
                double[] a = dArr;
                if (dArr != null) {
                    int th = this.threshold;
                    int org = this.origin;
                    int fnc = this.fence;
                    DoubleCumulateTask t = this;
                    while (true) {
                        DoubleCumulateTask t2 = t;
                        int i = t2.lo;
                        int l = i;
                        if (i >= 0) {
                            i = t2.hi;
                            int h = i;
                            if (i > a.length) {
                                return;
                            }
                            DoubleCumulateTask rt;
                            int i2;
                            int i3;
                            double pin;
                            DoubleCumulateTask t3;
                            double d;
                            int pendingCount;
                            int c;
                            if (h - l > th) {
                                DoubleCumulateTask lt = t2.left;
                                rt = t2.right;
                                int l2;
                                DoubleCumulateTask rt2;
                                if (lt == null) {
                                    int mid = (l + h) >>> 1;
                                    DoubleCumulateTask doubleCumulateTask = t2;
                                    DoubleBinaryOperator doubleBinaryOperator2 = fn;
                                    double[] dArr2 = a;
                                    thisR = t;
                                    i2 = org;
                                    int i4 = fnc;
                                    int i5 = th;
                                    l2 = l;
                                    t = new DoubleCumulateTask(doubleCumulateTask, doubleBinaryOperator2, dArr2, i2, i4, i5, mid, h);
                                    t2.right = thisR;
                                    rt2 = thisR;
                                    DoubleCumulateTask f = thisR;
                                    thisR = t;
                                    t = new DoubleCumulateTask(doubleCumulateTask, doubleBinaryOperator2, dArr2, i2, i4, i5, l2, mid);
                                    t2.left = thisR;
                                    lt = thisR;
                                    t2 = thisR;
                                    l = l2;
                                    t = f;
                                } else {
                                    DoubleCumulateTask lt2 = lt;
                                    i3 = h;
                                    l2 = l;
                                    pin = t2.in;
                                    thisR = lt2;
                                    thisR.in = pin;
                                    t = null;
                                    t3 = null;
                                    if (rt != null) {
                                        double lout = thisR.out;
                                        if (l2 == org) {
                                            double d2 = pin;
                                            d = lout;
                                        } else {
                                            d = fn.applyAsDouble(pin, lout);
                                        }
                                        rt.in = d;
                                        do {
                                            pendingCount = rt.getPendingCount();
                                            pin = pendingCount;
                                            if ((pendingCount & 1) != 0) {
                                                break;
                                            }
                                        } while (!rt.compareAndSetPendingCount(pin, pin | 1));
                                        t3 = rt;
                                    } else {
                                        l = l2;
                                    }
                                    do {
                                        pendingCount = thisR.getPendingCount();
                                        c = pendingCount;
                                        if ((pendingCount & 1) != 0) {
                                            t2 = t3;
                                            break;
                                        }
                                    } while (!thisR.compareAndSetPendingCount(c, c | 1));
                                    if (t3 != null) {
                                        t = t3;
                                    }
                                    t2 = thisR;
                                    if (t2 != null) {
                                        rt2 = rt;
                                    } else {
                                        return;
                                    }
                                }
                                if (t != null) {
                                    t.fork();
                                }
                                t = t2;
                            } else {
                                int state;
                                double sum;
                                int i6 = 1;
                                i3 = h;
                                do {
                                    state = t2.getPendingCount();
                                    i = state;
                                    pendingCount = 4;
                                    if ((state & 4) == 0) {
                                        state = (i & 1) != 0 ? 4 : l > org ? 2 : 6;
                                    } else {
                                        return;
                                    }
                                } while (!t2.compareAndSetPendingCount(i, i | state));
                                if (state != 2) {
                                    double sum2;
                                    if (l == org) {
                                        sum2 = a[org];
                                        i = org + 1;
                                    } else {
                                        sum2 = t2.in;
                                        i = l;
                                    }
                                    sum = sum2;
                                    i2 = i;
                                    while (true) {
                                        h = i3;
                                        if (i2 >= h) {
                                            break;
                                        }
                                        pin = fn.applyAsDouble(sum, a[i2]);
                                        sum = pin;
                                        a[i2] = pin;
                                        i2++;
                                        i3 = h;
                                    }
                                } else {
                                    h = i3;
                                    if (h < fnc) {
                                        sum = a[l];
                                        for (i = l + 1; i < h; i++) {
                                            sum = fn.applyAsDouble(sum, a[i]);
                                        }
                                    } else {
                                        sum = t2.in;
                                    }
                                }
                                pin = sum;
                                t2.out = pin;
                                while (true) {
                                    t = (DoubleCumulateTask) t2.getCompleter();
                                    t3 = t;
                                    if (t != null) {
                                        double sum3;
                                        i = t3.getPendingCount();
                                        if (((i & state) & pendingCount) != 0) {
                                            sum3 = pin;
                                            t2 = t3;
                                        } else if (((i & state) & 2) != 0) {
                                            DoubleCumulateTask doubleCumulateTask2;
                                            rt = t3.left;
                                            DoubleCumulateTask lt3 = rt;
                                            if (rt != null) {
                                                rt = t3.right;
                                                DoubleCumulateTask rt3 = rt;
                                                if (rt != null) {
                                                    sum3 = pin;
                                                    rt = lt3;
                                                    d = rt.out;
                                                    DoubleCumulateTask rt4 = rt3;
                                                    if (rt4.hi == fnc) {
                                                        doubleCumulateTask2 = rt;
                                                        sum = d;
                                                    } else {
                                                        sum = fn.applyAsDouble(d, rt4.out);
                                                    }
                                                    t3.out = sum;
                                                    pendingCount = ((i & 1) == 0 || t3.lo != org) ? 0 : 1;
                                                    c = (i | state) | pendingCount;
                                                    i6 = c;
                                                    if (c != i || t3.compareAndSetPendingCount(i, i6)) {
                                                        state = 2;
                                                        t2 = t3;
                                                        if (pendingCount == 0) {
                                                            t3.fork();
                                                        }
                                                    }
                                                }
                                            }
                                            sum3 = pin;
                                            doubleCumulateTask2 = lt3;
                                            if ((i & 1) == 0) {
                                            }
                                            c = (i | state) | pendingCount;
                                            i6 = c;
                                            if (c != i) {
                                            }
                                            state = 2;
                                            t2 = t3;
                                            if (pendingCount == 0) {
                                            }
                                        } else {
                                            sum3 = pin;
                                            if (t3.compareAndSetPendingCount(i, i | state)) {
                                                return;
                                            }
                                        }
                                        pin = sum3;
                                        pendingCount = 4;
                                    } else if ((state & 4) != 0) {
                                        t2.quietlyComplete();
                                        return;
                                    } else {
                                        return;
                                    }
                                }
                            }
                        }
                        return;
                    }
                }
            }
            throw new NullPointerException();
        }
    }

    static final class IntCumulateTask extends CountedCompleter<Void> {
        private static final long serialVersionUID = 3731755594596840961L;
        final int[] array;
        final int fence;
        final IntBinaryOperator function;
        final int hi;
        int in;
        IntCumulateTask left;
        final int lo;
        final int origin;
        int out;
        IntCumulateTask right;
        final int threshold;

        public IntCumulateTask(IntCumulateTask parent, IntBinaryOperator function, int[] array, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = lo;
            this.lo = lo;
            this.fence = hi;
            this.hi = hi;
            int commonPoolParallelism = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            int p = commonPoolParallelism;
            int i = 16;
            if (commonPoolParallelism > 16) {
                i = p;
            }
            this.threshold = i;
        }

        IntCumulateTask(IntCumulateTask parent, IntBinaryOperator function, int[] array, int origin, int fence, int threshold, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = origin;
            this.fence = fence;
            this.threshold = threshold;
            this.lo = lo;
            this.hi = hi;
        }

        /* JADX WARNING: Removed duplicated region for block: B:126:0x0193 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:99:0x0186  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public final void compute() {
            IntCumulateTask thisR;
            IntBinaryOperator intBinaryOperator = this.function;
            IntBinaryOperator fn = intBinaryOperator;
            if (intBinaryOperator != null) {
                int[] iArr = this.array;
                int[] a = iArr;
                if (iArr != null) {
                    int th = this.threshold;
                    int org = this.origin;
                    int fnc = this.fence;
                    IntCumulateTask t = this;
                    while (true) {
                        IntCumulateTask t2 = t;
                        int i = t2.lo;
                        int l = i;
                        if (i < 0) {
                            break;
                        }
                        i = t2.hi;
                        int h = i;
                        IntCumulateTask rt;
                        int i2;
                        int i3;
                        int i4;
                        IntCumulateTask f;
                        int i5;
                        int pin;
                        IntCumulateTask rt2;
                        if (i > a.length) {
                            break;
                        } else if (h - l > th) {
                            IntCumulateTask lt = t2.left;
                            rt = t2.right;
                            int l2;
                            IntCumulateTask rt3;
                            if (lt == null) {
                                int mid = (l + h) >>> 1;
                                IntCumulateTask intCumulateTask = t2;
                                IntBinaryOperator intBinaryOperator2 = fn;
                                int[] iArr2 = a;
                                thisR = t;
                                i2 = org;
                                i3 = fnc;
                                i4 = th;
                                l2 = l;
                                t = new IntCumulateTask(intCumulateTask, intBinaryOperator2, iArr2, i2, i3, i4, mid, h);
                                t2.right = thisR;
                                rt3 = thisR;
                                IntCumulateTask f2 = thisR;
                                thisR = t;
                                t = new IntCumulateTask(intCumulateTask, intBinaryOperator2, iArr2, i2, i3, i4, l2, mid);
                                t2.left = thisR;
                                t2 = thisR;
                                t = thisR;
                                lt = l2;
                                f = f2;
                            } else {
                                rt3 = rt;
                                IntCumulateTask lt2 = lt;
                                i5 = h;
                                l2 = l;
                                pin = t2.in;
                                t = lt2;
                                t.in = pin;
                                f = null;
                                IntCumulateTask t3 = null;
                                rt2 = rt3;
                                if (rt2 != null) {
                                    i3 = t.out;
                                    rt2.in = l2 == org ? i3 : fn.applyAsInt(pin, i3);
                                    do {
                                        h = rt2.getPendingCount();
                                        l = h;
                                        if ((h & 1) != 0) {
                                            break;
                                        }
                                    } while (rt2.compareAndSetPendingCount(l, l | 1) == 0);
                                    t3 = rt2;
                                }
                                do {
                                    i3 = t.getPendingCount();
                                    h = i3;
                                    if ((i3 & 1) != 0) {
                                        t2 = t3;
                                        break;
                                    }
                                } while (!t.compareAndSetPendingCount(h, h | 1));
                                if (t3 != null) {
                                    f = t3;
                                }
                                t2 = t;
                                if (t2 != null) {
                                    rt3 = rt2;
                                } else {
                                    return;
                                }
                            }
                            if (f != null) {
                                f.fork();
                            }
                            t = t2;
                        } else {
                            int i6;
                            int i7;
                            i5 = h;
                            i4 = l;
                            do {
                                pin = t2.getPendingCount();
                                i = pin;
                                i6 = 4;
                                if ((pin & 4) == 0) {
                                    i7 = 2;
                                    pin = (i & 1) != 0 ? 4 : i4 > org ? 2 : 6;
                                } else {
                                    return;
                                }
                            } while (!t2.compareAndSetPendingCount(i, i | pin));
                            if (pin != 2) {
                                if (i4 == org) {
                                    i = a[org];
                                    l = org + 1;
                                } else {
                                    i = t2.in;
                                    l = i4;
                                }
                                i3 = i;
                                i = l;
                                while (true) {
                                    h = i5;
                                    if (i >= h) {
                                        break;
                                    }
                                    l = fn.applyAsInt(i3, a[i]);
                                    i3 = l;
                                    a[i] = l;
                                    i++;
                                    i5 = h;
                                }
                            } else {
                                h = i5;
                                if (h < fnc) {
                                    l = i4 + 1;
                                    i3 = a[i4];
                                    while (true) {
                                        i = l;
                                        if (i >= h) {
                                            break;
                                        }
                                        i3 = fn.applyAsInt(i3, a[i]);
                                        l = i + 1;
                                    }
                                } else {
                                    i3 = t2.in;
                                }
                            }
                            t2.out = i3;
                            while (true) {
                                rt2 = (IntCumulateTask) t2.getCompleter();
                                rt = rt2;
                                if (rt2 != null) {
                                    i2 = rt.getPendingCount();
                                    if (((i2 & pin) & i6) != 0) {
                                        t2 = rt;
                                    } else if (((i2 & pin) & i7) != 0) {
                                        int lout;
                                        l = rt.left;
                                        IntCumulateTask lt3 = l;
                                        if (l != 0) {
                                            l = rt.right;
                                            IntCumulateTask rt4 = l;
                                            if (l != 0) {
                                                lout = lt3.out;
                                                f = rt4;
                                                rt.out = f.hi == fnc ? lout : fn.applyAsInt(lout, f.out);
                                                lout = ((i2 & 1) == 0 || rt.lo != org) ? 0 : 1;
                                                i6 = (i2 | pin) | lout;
                                                i7 = i6;
                                                if (i6 != i2 || rt.compareAndSetPendingCount(i2, i7)) {
                                                    pin = 2;
                                                    t2 = rt;
                                                    if (lout == 0) {
                                                        rt.fork();
                                                    }
                                                }
                                            }
                                        }
                                        l = lt3;
                                        if ((i2 & 1) == 0) {
                                        }
                                        i6 = (i2 | pin) | lout;
                                        i7 = i6;
                                        if (i6 != i2) {
                                        }
                                        pin = 2;
                                        t2 = rt;
                                        if (lout == 0) {
                                        }
                                    } else if (rt.compareAndSetPendingCount(i2, i2 | pin)) {
                                        return;
                                    }
                                    i6 = 4;
                                    i7 = 2;
                                } else if ((pin & 4) != 0) {
                                    t2.quietlyComplete();
                                    return;
                                } else {
                                    return;
                                }
                            }
                        }
                    }
                    return;
                }
            }
            throw new NullPointerException();
        }
    }

    static final class LongCumulateTask extends CountedCompleter<Void> {
        private static final long serialVersionUID = -5074099945909284273L;
        final long[] array;
        final int fence;
        final LongBinaryOperator function;
        final int hi;
        long in;
        LongCumulateTask left;
        final int lo;
        final int origin;
        long out;
        LongCumulateTask right;
        final int threshold;

        public LongCumulateTask(LongCumulateTask parent, LongBinaryOperator function, long[] array, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = lo;
            this.lo = lo;
            this.fence = hi;
            this.hi = hi;
            int commonPoolParallelism = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            int p = commonPoolParallelism;
            int i = 16;
            if (commonPoolParallelism > 16) {
                i = p;
            }
            this.threshold = i;
        }

        LongCumulateTask(LongCumulateTask parent, LongBinaryOperator function, long[] array, int origin, int fence, int threshold, int lo, int hi) {
            super(parent);
            this.function = function;
            this.array = array;
            this.origin = origin;
            this.fence = fence;
            this.threshold = threshold;
            this.lo = lo;
            this.hi = hi;
        }

        /* JADX WARNING: Removed duplicated region for block: B:124:0x01a7 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:98:0x0198  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public final void compute() {
            LongCumulateTask thisR;
            LongBinaryOperator longBinaryOperator = this.function;
            LongBinaryOperator fn = longBinaryOperator;
            if (longBinaryOperator != null) {
                long[] jArr = this.array;
                long[] a = jArr;
                if (jArr != null) {
                    int th = this.threshold;
                    int org = this.origin;
                    int fnc = this.fence;
                    LongCumulateTask t = this;
                    while (true) {
                        LongCumulateTask t2 = t;
                        int i = t2.lo;
                        int l = i;
                        if (i >= 0) {
                            i = t2.hi;
                            int h = i;
                            if (i > a.length) {
                                return;
                            }
                            LongCumulateTask rt;
                            int i2;
                            int i3;
                            long pin;
                            LongCumulateTask t3;
                            long j;
                            int pendingCount;
                            int c;
                            if (h - l > th) {
                                LongCumulateTask lt = t2.left;
                                rt = t2.right;
                                int l2;
                                LongCumulateTask rt2;
                                if (lt == null) {
                                    int mid = (l + h) >>> 1;
                                    LongCumulateTask longCumulateTask = t2;
                                    LongBinaryOperator longBinaryOperator2 = fn;
                                    long[] jArr2 = a;
                                    thisR = t;
                                    i2 = org;
                                    int i4 = fnc;
                                    int i5 = th;
                                    l2 = l;
                                    t = new LongCumulateTask(longCumulateTask, longBinaryOperator2, jArr2, i2, i4, i5, mid, h);
                                    t2.right = thisR;
                                    rt2 = thisR;
                                    LongCumulateTask f = thisR;
                                    thisR = t;
                                    t = new LongCumulateTask(longCumulateTask, longBinaryOperator2, jArr2, i2, i4, i5, l2, mid);
                                    t2.left = thisR;
                                    lt = thisR;
                                    t2 = thisR;
                                    l = l2;
                                    t = f;
                                } else {
                                    LongCumulateTask lt2 = lt;
                                    i3 = h;
                                    l2 = l;
                                    pin = t2.in;
                                    thisR = lt2;
                                    thisR.in = pin;
                                    t = null;
                                    t3 = null;
                                    if (rt != null) {
                                        long lout = thisR.out;
                                        if (l2 == org) {
                                            long j2 = pin;
                                            j = lout;
                                        } else {
                                            j = fn.applyAsLong(pin, lout);
                                        }
                                        rt.in = j;
                                        do {
                                            pendingCount = rt.getPendingCount();
                                            pin = pendingCount;
                                            if ((pendingCount & 1) != 0) {
                                                break;
                                            }
                                        } while (!rt.compareAndSetPendingCount(pin, pin | 1));
                                        t3 = rt;
                                    } else {
                                        l = l2;
                                    }
                                    do {
                                        pendingCount = thisR.getPendingCount();
                                        c = pendingCount;
                                        if ((pendingCount & 1) != 0) {
                                            t2 = t3;
                                            break;
                                        }
                                    } while (!thisR.compareAndSetPendingCount(c, c | 1));
                                    if (t3 != null) {
                                        t = t3;
                                    }
                                    t2 = thisR;
                                    if (t2 != null) {
                                        rt2 = rt;
                                    } else {
                                        return;
                                    }
                                }
                                if (t != null) {
                                    t.fork();
                                }
                                t = t2;
                            } else {
                                int state;
                                long sum;
                                int i6 = 1;
                                i3 = h;
                                do {
                                    state = t2.getPendingCount();
                                    i = state;
                                    pendingCount = 4;
                                    if ((state & 4) == 0) {
                                        state = (i & 1) != 0 ? 4 : l > org ? 2 : 6;
                                    } else {
                                        return;
                                    }
                                } while (!t2.compareAndSetPendingCount(i, i | state));
                                if (state != 2) {
                                    long sum2;
                                    if (l == org) {
                                        sum2 = a[org];
                                        i = org + 1;
                                    } else {
                                        sum2 = t2.in;
                                        i = l;
                                    }
                                    sum = sum2;
                                    i2 = i;
                                    while (true) {
                                        h = i3;
                                        if (i2 >= h) {
                                            break;
                                        }
                                        pin = fn.applyAsLong(sum, a[i2]);
                                        sum = pin;
                                        a[i2] = pin;
                                        i2++;
                                        i3 = h;
                                    }
                                } else {
                                    h = i3;
                                    if (h < fnc) {
                                        sum = a[l];
                                        for (i = l + 1; i < h; i++) {
                                            sum = fn.applyAsLong(sum, a[i]);
                                        }
                                    } else {
                                        sum = t2.in;
                                    }
                                }
                                pin = sum;
                                t2.out = pin;
                                while (true) {
                                    t = (LongCumulateTask) t2.getCompleter();
                                    t3 = t;
                                    if (t != null) {
                                        long sum3;
                                        i = t3.getPendingCount();
                                        if (((i & state) & pendingCount) != 0) {
                                            sum3 = pin;
                                            t2 = t3;
                                        } else if (((i & state) & 2) != 0) {
                                            LongCumulateTask longCumulateTask2;
                                            rt = t3.left;
                                            LongCumulateTask lt3 = rt;
                                            if (rt != null) {
                                                rt = t3.right;
                                                LongCumulateTask rt3 = rt;
                                                if (rt != null) {
                                                    sum3 = pin;
                                                    rt = lt3;
                                                    j = rt.out;
                                                    LongCumulateTask rt4 = rt3;
                                                    if (rt4.hi == fnc) {
                                                        longCumulateTask2 = rt;
                                                        sum = j;
                                                    } else {
                                                        sum = fn.applyAsLong(j, rt4.out);
                                                    }
                                                    t3.out = sum;
                                                    pendingCount = ((i & 1) == 0 || t3.lo != org) ? 0 : 1;
                                                    c = (i | state) | pendingCount;
                                                    i6 = c;
                                                    if (c != i || t3.compareAndSetPendingCount(i, i6)) {
                                                        state = 2;
                                                        t2 = t3;
                                                        if (pendingCount == 0) {
                                                            t3.fork();
                                                        }
                                                    }
                                                }
                                            }
                                            sum3 = pin;
                                            longCumulateTask2 = lt3;
                                            if ((i & 1) == 0) {
                                            }
                                            c = (i | state) | pendingCount;
                                            i6 = c;
                                            if (c != i) {
                                            }
                                            state = 2;
                                            t2 = t3;
                                            if (pendingCount == 0) {
                                            }
                                        } else {
                                            sum3 = pin;
                                            if (t3.compareAndSetPendingCount(i, i | state)) {
                                                return;
                                            }
                                        }
                                        pin = sum3;
                                        pendingCount = 4;
                                    } else if ((state & 4) != 0) {
                                        t2.quietlyComplete();
                                        return;
                                    } else {
                                        return;
                                    }
                                }
                            }
                        }
                        return;
                    }
                }
            }
            throw new NullPointerException();
        }
    }

    private ArrayPrefixHelpers() {
    }
}
