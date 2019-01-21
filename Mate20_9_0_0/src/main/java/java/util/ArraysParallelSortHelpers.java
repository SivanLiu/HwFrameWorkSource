package java.util;

import java.util.concurrent.CountedCompleter;

class ArraysParallelSortHelpers {

    static final class FJByte {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final byte[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final byte[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, byte[] a, byte[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                byte[] a = this.a;
                byte[] w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                byte[] bArr;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    bArr = a;
                    throw new IllegalStateException();
                }
                byte split;
                int lo;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i;
                    int lh;
                    int rh;
                    int lh2;
                    byte[] bArr2;
                    byte[] bArr3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        split = a[i + lb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + rb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        bArr2 = a;
                        bArr3 = w;
                        bArr = a;
                        i2 = 1;
                        ln = new Merger(this, bArr2, bArr3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = bArr;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        split = a[i + rb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + lb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        bArr2 = a;
                        bArr3 = w;
                        bArr = a;
                        i2 = 1;
                        ln = new Merger(this, bArr2, bArr3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = bArr;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    split = a[lb];
                    byte al = split;
                    byte b = a[rb];
                    byte ar = b;
                    if (split <= b) {
                        lb++;
                        split = al;
                    } else {
                        rb++;
                        split = ar;
                    }
                    lo = k + 1;
                    w[k] = split;
                    k = lo;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final byte[] a;
            final int base;
            final int gran;
            final int size;
            final byte[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, byte[] a, byte[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                byte[] a = this.a;
                byte[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int wb2;
                    byte[] w2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        byte[] w3 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w3, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w3, b + u, n2 - u, wb + u, i).fork();
                        byte[] bArr = a;
                        byte[] bArr2 = w3;
                        i = q;
                        wb2 = wb;
                        n = b;
                        new Sorter(rc, bArr, bArr2, b + h, i, wb + h, g3).fork();
                        fc = r6;
                        w2 = w3;
                        byte[] a2 = a;
                        CountedCompleter merger2 = new Merger(fc, bArr, bArr2, n, i, n + q, h - q, wb2, g3);
                        countedCompleter = new Relay(fc);
                        new Sorter(countedCompleter, a2, w2, n + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        b = n;
                        wb = wb2;
                        g = g3;
                        w = w2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        wb2 = wb;
                        n = b;
                        w2 = w;
                        DualPivotQuicksort.sort(a, n, (n + n2) - 1);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJByte() {
        }
    }

    static final class FJChar {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final char[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final char[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, char[] a, char[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    obj = a;
                    throw new IllegalStateException();
                }
                char split;
                int lo;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i;
                    int lh;
                    int rh;
                    int lh2;
                    Object obj2;
                    Object obj3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        split = a[i + lb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + rb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        split = a[i + rb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + lb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    split = a[lb];
                    char al = split;
                    char c = a[rb];
                    char ar = c;
                    if (split <= c) {
                        lb++;
                        split = al;
                    } else {
                        rb++;
                        split = ar;
                    }
                    lo = k + 1;
                    w[k] = split;
                    k = lo;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final char[] a;
            final int base;
            final int gran;
            final int size;
            final char[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, char[] a, char[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                char[] a = this.a;
                char[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        char[] w2 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w2, b + u, n2 - u, wb + u, i).fork();
                        char[] cArr = a;
                        char[] cArr2 = w2;
                        i = q;
                        int wb2 = wb;
                        b2 = b;
                        new Sorter(rc, cArr, cArr2, b + h, i, wb + h, g3).fork();
                        char[] a2 = a;
                        countedCompleter = new Relay(new Merger(fc, cArr, cArr2, b2, i, b2 + q, h - q, wb2, g3));
                        new Sorter(countedCompleter, a2, cArr2, b2 + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        g = g3;
                        wb = wb2;
                        w = w2;
                        b = b2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        DualPivotQuicksort.sort(a, b2, (b2 + n2) - 1, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJChar() {
        }
    }

    static final class FJDouble {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final double[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final double[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, double[] a, double[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    obj = a;
                    throw new IllegalStateException();
                }
                double split;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i;
                    int lh;
                    int rh;
                    int lh2;
                    Object obj2;
                    Object obj3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        split = a[i + lb];
                        while (ln < rn) {
                            i = (ln + rn) >>> 1;
                            if (split <= a[i + rb]) {
                                rn = i;
                            } else {
                                ln = i + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        split = a[i + rb];
                        while (ln < rn) {
                            i = (ln + rn) >>> 1;
                            if (split <= a[i + lb]) {
                                rn = i;
                            } else {
                                ln = i + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    double d = a[lb];
                    split = d;
                    double d2 = a[rb];
                    double ar = d2;
                    if (d <= d2) {
                        lb++;
                        d = split;
                    } else {
                        rb++;
                        d = ar;
                    }
                    int k2 = k + 1;
                    w[k] = d;
                    k = k2;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final double[] a;
            final int base;
            final int gran;
            final int size;
            final double[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, double[] a, double[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                double[] a = this.a;
                double[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        double[] w2 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w2, b + u, n2 - u, wb + u, i).fork();
                        double[] dArr = a;
                        double[] dArr2 = w2;
                        i = q;
                        int wb2 = wb;
                        b2 = b;
                        new Sorter(rc, dArr, dArr2, b + h, i, wb + h, g3).fork();
                        double[] a2 = a;
                        countedCompleter = new Relay(new Merger(fc, dArr, dArr2, b2, i, b2 + q, h - q, wb2, g3));
                        new Sorter(countedCompleter, a2, dArr2, b2 + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        g = g3;
                        wb = wb2;
                        w = w2;
                        b = b2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        DualPivotQuicksort.sort(a, b2, (b2 + n2) - 1, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJDouble() {
        }
    }

    static final class FJFloat {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final float[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final float[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, float[] a, float[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    obj = a;
                    throw new IllegalStateException();
                }
                float split;
                int lo;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i;
                    int lh;
                    int rh;
                    int lh2;
                    Object obj2;
                    Object obj3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        split = a[i + lb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + rb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        split = a[i + rb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + lb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    split = a[lb];
                    float al = split;
                    float f = a[rb];
                    float ar = f;
                    if (split <= f) {
                        lb++;
                        split = al;
                    } else {
                        rb++;
                        split = ar;
                    }
                    lo = k + 1;
                    w[k] = split;
                    k = lo;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final float[] a;
            final int base;
            final int gran;
            final int size;
            final float[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, float[] a, float[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                float[] a = this.a;
                float[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        float[] w2 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w2, b + u, n2 - u, wb + u, i).fork();
                        float[] fArr = a;
                        float[] fArr2 = w2;
                        i = q;
                        int wb2 = wb;
                        b2 = b;
                        new Sorter(rc, fArr, fArr2, b + h, i, wb + h, g3).fork();
                        float[] a2 = a;
                        countedCompleter = new Relay(new Merger(fc, fArr, fArr2, b2, i, b2 + q, h - q, wb2, g3));
                        new Sorter(countedCompleter, a2, fArr2, b2 + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        g = g3;
                        wb = wb2;
                        w = w2;
                        b = b2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        DualPivotQuicksort.sort(a, b2, (b2 + n2) - 1, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJFloat() {
        }
    }

    static final class FJInt {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final int[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final int[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, int[] a, int[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    obj = a;
                    throw new IllegalStateException();
                }
                int i;
                int lh;
                int lo;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int rh;
                    int lh2;
                    Object obj2;
                    Object obj3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        i = a[i + lb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (i <= a[lo + rb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        i = a[i + rb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (i <= a[lo + lb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    i = a[lb];
                    lh = i;
                    lo = a[rb];
                    int ar = lo;
                    if (i <= lo) {
                        lb++;
                        i = lh;
                    } else {
                        rb++;
                        i = ar;
                    }
                    lo = k + 1;
                    w[k] = i;
                    k = lo;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final int[] a;
            final int base;
            final int gran;
            final int size;
            final int[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, int[] a, int[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                int[] a = this.a;
                int[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        int[] w2 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w2, b + u, n2 - u, wb + u, i).fork();
                        int[] iArr = a;
                        int[] iArr2 = w2;
                        i = q;
                        int wb2 = wb;
                        b2 = b;
                        new Sorter(rc, iArr, iArr2, b + h, i, wb + h, g3).fork();
                        int[] a2 = a;
                        countedCompleter = new Relay(new Merger(fc, iArr, iArr2, b2, i, b2 + q, h - q, wb2, g3));
                        new Sorter(countedCompleter, a2, iArr2, b2 + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        g = g3;
                        wb = wb2;
                        w = w2;
                        b = b2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        DualPivotQuicksort.sort(a, b2, (b2 + n2) - 1, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJInt() {
        }
    }

    static final class FJLong {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final long[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final long[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, long[] a, long[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    obj = a;
                    throw new IllegalStateException();
                }
                long split;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i;
                    int lh;
                    int rh;
                    int lh2;
                    Object obj2;
                    Object obj3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        split = a[i + lb];
                        while (ln < rn) {
                            i = (ln + rn) >>> 1;
                            if (split <= a[i + rb]) {
                                rn = i;
                            } else {
                                ln = i + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        split = a[i + rb];
                        while (ln < rn) {
                            i = (ln + rn) >>> 1;
                            if (split <= a[i + lb]) {
                                rn = i;
                            } else {
                                ln = i + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    long j = a[lb];
                    split = j;
                    long j2 = a[rb];
                    long ar = j2;
                    if (j <= j2) {
                        lb++;
                        j = split;
                    } else {
                        rb++;
                        j = ar;
                    }
                    int k2 = k + 1;
                    w[k] = j;
                    k = k2;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final long[] a;
            final int base;
            final int gran;
            final int size;
            final long[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, long[] a, long[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                long[] a = this.a;
                long[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        long[] w2 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w2, b + u, n2 - u, wb + u, i).fork();
                        long[] jArr = a;
                        long[] jArr2 = w2;
                        i = q;
                        int wb2 = wb;
                        b2 = b;
                        new Sorter(rc, jArr, jArr2, b + h, i, wb + h, g3).fork();
                        long[] a2 = a;
                        countedCompleter = new Relay(new Merger(fc, jArr, jArr2, b2, i, b2 + q, h - q, wb2, g3));
                        new Sorter(countedCompleter, a2, jArr2, b2 + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        g = g3;
                        wb = wb2;
                        w = w2;
                        b = b2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        DualPivotQuicksort.sort(a, b2, (b2 + n2) - 1, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJLong() {
        }
    }

    static final class FJObject {

        static final class Merger<T> extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final T[] a;
            Comparator<? super T> comparator;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final T[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, T[] a, T[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran, Comparator<? super T> comparator) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
                this.comparator = comparator;
            }

            public final void compute() {
                Comparator<? super T> c = this.comparator;
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                int i;
                int i2;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0 || c == null) {
                    i = k;
                    i2 = rb;
                    obj = a;
                    throw new IllegalStateException();
                }
                T split;
                int lo;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i3 = 1;
                    int i4;
                    int lh;
                    int rh;
                    int lh2;
                    int i5;
                    Object obj2;
                    Object obj3;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i4 = ln2 >>> 1;
                        lh = i4;
                        split = a[i4 + lb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> i3;
                            if (c.compare(split, a[lo + rb]) <= 0) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                            i3 = 1;
                        }
                        rh = rn;
                        lh2 = lh;
                        i5 = rn2 - rh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        ln2 = i5;
                        i5 = g;
                        i = k;
                        i2 = rb;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, ln2, (k + lh2) + rh, i5, c);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(1);
                        ln.fork();
                        g = i5;
                        k = i;
                        a = obj;
                        rb = i2;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i4 = rn2 >>> 1;
                        lh = i4;
                        split = a[i4 + rb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (c.compare(split, a[lo + lb]) <= 0) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        i5 = rn2 - rh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        ln2 = i5;
                        i5 = g;
                        i = k;
                        i2 = rb;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, ln2, (k + lh2) + rh, i5, c);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(1);
                        ln.fork();
                        g = i5;
                        k = i;
                        a = obj;
                        rb = i2;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    split = a[lb];
                    T al = split;
                    T t = a[rb];
                    T ar = t;
                    if (c.compare(split, t) <= 0) {
                        lb++;
                        split = al;
                    } else {
                        rb++;
                        split = ar;
                    }
                    lo = k + 1;
                    w[k] = split;
                    k = lo;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter<T> extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final T[] a;
            final int base;
            Comparator<? super T> comparator;
            final int gran;
            final int size;
            final T[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, T[] a, T[] w, int base, int size, int wbase, int gran, Comparator<? super T> comparator) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
                this.comparator = comparator;
            }

            public final void compute() {
                Comparator<? super T> c = this.comparator;
                T[] a = this.a;
                T[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        T[] w2 = w;
                        int g3 = g2;
                        int i = b;
                        int wb2 = wb;
                        wb = g3;
                        b2 = b;
                        Comparator<? super T> comparator = c;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, i, wb, comparator);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b2 + h, q, b2 + u, n2 - u, wb2 + h, wb, comparator));
                        new Sorter(rc, a, w2, b2 + u, n2 - u, wb2 + u, wb, comparator).fork();
                        T[] tArr = w2;
                        wb = q;
                        T[] a2 = a;
                        new Sorter(rc, a, tArr, b2 + h, wb, wb2 + h, g3, c).fork();
                        T[] tArr2 = a2;
                        Comparator<? super T> c2 = c;
                        countedCompleter = new Relay(new Merger(fc, tArr2, tArr, b2, wb, b2 + q, h - q, wb2, g3, c2));
                        new Sorter(countedCompleter, tArr2, tArr, b2 + q, h - q, wb2 + q, g3, c2).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        wb = wb2;
                        w = w2;
                        g = g3;
                        b = b2;
                        a = a2;
                        c = c2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        TimSort.sort(a, b2, b2 + n2, c, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJObject() {
        }
    }

    static final class FJShort {

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final short[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final short[] w;
            final int wbase;

            Merger(CountedCompleter<?> par, short[] a, short[] w, int lbase, int lsize, int rbase, int rsize, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.lbase = lbase;
                this.lsize = lsize;
                this.rbase = rbase;
                this.rsize = rsize;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                Object a = this.a;
                Object w = this.w;
                int lb = this.lbase;
                int ln = this.lsize;
                int rb = this.rbase;
                int rn = this.rsize;
                int k = this.wbase;
                int g = this.gran;
                Object obj;
                if (a == null || w == null || lb < 0 || rb < 0 || k < 0) {
                    obj = a;
                    throw new IllegalStateException();
                }
                short split;
                int lo;
                int ln2 = ln;
                int rn2 = rn;
                while (true) {
                    ln = 0;
                    int i;
                    int lh;
                    int rh;
                    int lh2;
                    Object obj2;
                    Object obj3;
                    int i2;
                    if (ln2 >= rn2) {
                        if (ln2 <= g) {
                            break;
                        }
                        rn = rn2;
                        i = ln2 >>> 1;
                        lh = i;
                        split = a[i + lb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + rb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        rh = rn;
                        lh2 = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    } else if (rn2 <= g) {
                        break;
                    } else {
                        rn = ln2;
                        i = rn2 >>> 1;
                        lh = i;
                        split = a[i + rb];
                        while (ln < rn) {
                            lo = (ln + rn) >>> 1;
                            if (split <= a[lo + lb]) {
                                rn = lo;
                            } else {
                                ln = lo + 1;
                            }
                        }
                        lh2 = rn;
                        rh = lh;
                        obj2 = a;
                        obj3 = w;
                        obj = a;
                        i2 = 1;
                        ln = new Merger(this, obj2, obj3, lb + lh2, ln2 - lh2, rb + rh, rn2 - rh, (k + lh2) + rh, g);
                        rn2 = rh;
                        ln2 = lh2;
                        addToPendingCount(i2);
                        ln.fork();
                        a = obj;
                    }
                }
                ln = lb + ln2;
                rn = rb + rn2;
                while (lb < ln && rb < rn) {
                    split = a[lb];
                    short al = split;
                    short s = a[rb];
                    short ar = s;
                    if (split <= s) {
                        lb++;
                        split = al;
                    } else {
                        rb++;
                        split = ar;
                    }
                    lo = k + 1;
                    w[k] = split;
                    k = lo;
                }
                if (rb < rn) {
                    System.arraycopy(a, rb, w, k, rn - rb);
                } else if (lb < ln) {
                    System.arraycopy(a, lb, w, k, ln - lb);
                }
                tryComplete();
            }
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final short[] a;
            final int base;
            final int gran;
            final int size;
            final short[] w;
            final int wbase;

            Sorter(CountedCompleter<?> par, short[] a, short[] w, int base, int size, int wbase, int gran) {
                super(par);
                this.a = a;
                this.w = w;
                this.base = base;
                this.size = size;
                this.wbase = wbase;
                this.gran = gran;
            }

            public final void compute() {
                short[] a = this.a;
                short[] w = this.w;
                int b = this.base;
                int n = this.size;
                int wb = this.wbase;
                int g = this.gran;
                CountedCompleter<?> s = this;
                int n2 = n;
                while (true) {
                    int g2 = g;
                    int b2;
                    if (n2 > g2) {
                        int h = n2 >>> 1;
                        int q = h >>> 1;
                        int u = h + q;
                        CountedCompleter countedCompleter = r2;
                        short[] w2 = w;
                        int g3 = g2;
                        int i = g3;
                        CountedCompleter merger = new Merger(s, w, a, wb, h, wb + h, n2 - h, b, i);
                        CountedCompleter fc = new Relay(countedCompleter);
                        CountedCompleter rc = new Relay(new Merger(fc, a, w2, b + h, q, b + u, n2 - u, wb + h, i));
                        new Sorter(rc, a, w2, b + u, n2 - u, wb + u, i).fork();
                        short[] sArr = a;
                        short[] sArr2 = w2;
                        i = q;
                        int wb2 = wb;
                        b2 = b;
                        new Sorter(rc, sArr, sArr2, b + h, i, wb + h, g3).fork();
                        short[] a2 = a;
                        countedCompleter = new Relay(new Merger(fc, sArr, sArr2, b2, i, b2 + q, h - q, wb2, g3));
                        new Sorter(countedCompleter, a2, sArr2, b2 + q, h - q, wb2 + q, g3).fork();
                        s = new EmptyCompleter(countedCompleter);
                        n2 = q;
                        g = g3;
                        wb = wb2;
                        w = w2;
                        b = b2;
                        a = a2;
                    } else {
                        CountedCompleter<?> countedCompleter2 = s;
                        b2 = b;
                        DualPivotQuicksort.sort(a, b2, (b2 + n2) - 1, w, wb, n2);
                        s.tryComplete();
                        return;
                    }
                }
            }
        }

        FJShort() {
        }
    }

    static final class EmptyCompleter extends CountedCompleter<Void> {
        static final long serialVersionUID = 2446542900576103244L;

        EmptyCompleter(CountedCompleter<?> p) {
            super(p);
        }

        public final void compute() {
        }
    }

    static final class Relay extends CountedCompleter<Void> {
        static final long serialVersionUID = 2446542900576103244L;
        final CountedCompleter<?> task;

        Relay(CountedCompleter<?> task) {
            super(null, 1);
            this.task = task;
        }

        public final void compute() {
        }

        public final void onCompletion(CountedCompleter<?> countedCompleter) {
            this.task.compute();
        }
    }

    ArraysParallelSortHelpers() {
    }
}
