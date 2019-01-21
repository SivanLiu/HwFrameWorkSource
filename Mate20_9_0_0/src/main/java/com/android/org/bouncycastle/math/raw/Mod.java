package com.android.org.bouncycastle.math.raw;

import java.util.Random;

public abstract class Mod {
    public static int inverse32(int d) {
        int x = d;
        x *= 2 - (d * x);
        x *= 2 - (d * x);
        x *= 2 - (d * x);
        return x * (2 - (d * x));
    }

    public static void invert(int[] p, int[] x, int[] z) {
        int len = p.length;
        if (Nat.isZero(len, x)) {
            throw new IllegalArgumentException("'x' cannot be 0");
        } else if (Nat.isOne(len, x)) {
            System.arraycopy(x, 0, z, 0, len);
        } else {
            int[] u = Nat.copy(len, x);
            int[] a = Nat.create(len);
            a[0] = 1;
            int ac = 0;
            if ((u[0] & 1) == 0) {
                ac = inversionStep(p, u, len, a, 0);
            }
            if (Nat.isOne(len, u)) {
                inversionResult(p, ac, a, z);
                return;
            }
            int[] v = Nat.copy(len, p);
            int[] b = Nat.create(len);
            int bc = 0;
            int ac2 = ac;
            ac = len;
            while (true) {
                if (u[ac - 1] == 0 && v[ac - 1] == 0) {
                    ac--;
                } else if (Nat.gte(ac, u, v)) {
                    Nat.subFrom(ac, v, u);
                    ac2 = inversionStep(p, u, ac, a, ac2 + (Nat.subFrom(len, b, a) - bc));
                    if (Nat.isOne(ac, u)) {
                        inversionResult(p, ac2, a, z);
                        return;
                    }
                } else {
                    Nat.subFrom(ac, u, v);
                    bc = inversionStep(p, v, ac, b, bc + (Nat.subFrom(len, a, b) - ac2));
                    if (Nat.isOne(ac, v)) {
                        inversionResult(p, bc, b, z);
                        return;
                    }
                }
            }
        }
    }

    public static int[] random(int[] p) {
        int len = p.length;
        Random rand = new Random();
        int[] s = Nat.create(len);
        int m = p[len - 1];
        m |= m >>> 1;
        m |= m >>> 2;
        m |= m >>> 4;
        m |= m >>> 8;
        m |= m >>> 16;
        do {
            int i;
            for (i = 0; i != len; i++) {
                s[i] = rand.nextInt();
            }
            i = len - 1;
            s[i] = s[i] & m;
        } while (Nat.gte(len, s, p));
        return s;
    }

    public static void add(int[] p, int[] x, int[] y, int[] z) {
        int len = p.length;
        if (Nat.add(len, x, y, z) != 0) {
            Nat.subFrom(len, p, z);
        }
    }

    public static void subtract(int[] p, int[] x, int[] y, int[] z) {
        int len = p.length;
        if (Nat.sub(len, x, y, z) != 0) {
            Nat.addTo(len, p, z);
        }
    }

    private static void inversionResult(int[] p, int ac, int[] a, int[] z) {
        if (ac < 0) {
            Nat.add(p.length, a, p, z);
        } else {
            System.arraycopy(a, 0, z, 0, p.length);
        }
    }

    private static int inversionStep(int[] p, int[] u, int uLen, int[] x, int xc) {
        int len = p.length;
        int count = 0;
        while (u[0] == 0) {
            Nat.shiftDownWord(uLen, u, 0);
            count += 32;
        }
        int zeroes = getTrailingZeroes(u[0]);
        if (zeroes > 0) {
            Nat.shiftDownBits(uLen, u, zeroes, 0);
            count += zeroes;
        }
        zeroes = xc;
        for (xc = 0; xc < count; xc++) {
            if ((x[0] & 1) != 0) {
                if (zeroes < 0) {
                    zeroes += Nat.addTo(len, p, x);
                } else {
                    zeroes += Nat.subFrom(len, p, x);
                }
            }
            Nat.shiftDownBit(len, x, zeroes);
        }
        return zeroes;
    }

    private static int getTrailingZeroes(int x) {
        int count = 0;
        while ((x & 1) == 0) {
            x >>>= 1;
            count++;
        }
        return count;
    }
}
