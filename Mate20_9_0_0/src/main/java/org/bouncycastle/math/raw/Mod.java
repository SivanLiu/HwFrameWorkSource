package org.bouncycastle.math.raw;

import java.util.Random;

public abstract class Mod {
    public static void add(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4) {
        int length = iArr.length;
        if (Nat.add(length, iArr2, iArr3, iArr4) != 0) {
            Nat.subFrom(length, iArr, iArr4);
        }
    }

    private static int getTrailingZeroes(int i) {
        int i2 = 0;
        while ((i & 1) == 0) {
            i >>>= 1;
            i2++;
        }
        return i2;
    }

    public static int inverse32(int i) {
        int i2 = (2 - (i * i)) * i;
        i2 *= 2 - (i * i2);
        i2 *= 2 - (i * i2);
        return i2 * (2 - (i * i2));
    }

    private static void inversionResult(int[] iArr, int i, int[] iArr2, int[] iArr3) {
        if (i < 0) {
            Nat.add(iArr.length, iArr2, iArr, iArr3);
        } else {
            System.arraycopy(iArr2, 0, iArr3, 0, iArr.length);
        }
    }

    private static int inversionStep(int[] iArr, int[] iArr2, int i, int[] iArr3, int i2) {
        int length = iArr.length;
        int i3 = 0;
        while (iArr2[0] == 0) {
            Nat.shiftDownWord(i, iArr2, 0);
            i3 += 32;
        }
        int trailingZeroes = getTrailingZeroes(iArr2[0]);
        if (trailingZeroes > 0) {
            Nat.shiftDownBits(i, iArr2, trailingZeroes, 0);
            i3 += trailingZeroes;
        }
        for (int i4 = 0; i4 < i3; i4++) {
            if ((iArr3[0] & 1) != 0) {
                i2 += i2 < 0 ? Nat.addTo(length, iArr, iArr3) : Nat.subFrom(length, iArr, iArr3);
            }
            Nat.shiftDownBit(length, iArr3, i2);
        }
        return i2;
    }

    public static void invert(int[] iArr, int[] iArr2, int[] iArr3) {
        int length = iArr.length;
        if (Nat.isZero(length, iArr2)) {
            throw new IllegalArgumentException("'x' cannot be 0");
        } else if (Nat.isOne(length, iArr2)) {
            System.arraycopy(iArr2, 0, iArr3, 0, length);
        } else {
            iArr2 = Nat.copy(length, iArr2);
            int[] create = Nat.create(length);
            create[0] = 1;
            int inversionStep = (1 & iArr2[0]) == 0 ? inversionStep(iArr, iArr2, length, create, 0) : 0;
            if (Nat.isOne(length, iArr2)) {
                inversionResult(iArr, inversionStep, create, iArr3);
                return;
            }
            int[] copy = Nat.copy(length, iArr);
            int[] create2 = Nat.create(length);
            int i = 0;
            int i2 = length;
            while (true) {
                int i3 = i2 - 1;
                if (iArr2[i3] == 0 && copy[i3] == 0) {
                    i2--;
                } else if (Nat.gte(i2, iArr2, copy)) {
                    Nat.subFrom(i2, copy, iArr2);
                    inversionStep = inversionStep(iArr, iArr2, i2, create, inversionStep + (Nat.subFrom(length, create2, create) - i));
                    if (Nat.isOne(i2, iArr2)) {
                        inversionResult(iArr, inversionStep, create, iArr3);
                        return;
                    }
                } else {
                    Nat.subFrom(i2, iArr2, copy);
                    i = inversionStep(iArr, copy, i2, create2, i + (Nat.subFrom(length, create, create2) - inversionStep));
                    if (Nat.isOne(i2, copy)) {
                        inversionResult(iArr, i, create2, iArr3);
                        return;
                    }
                }
            }
        }
    }

    public static int[] random(int[] iArr) {
        int length = iArr.length;
        Random random = new Random();
        int[] create = Nat.create(length);
        int i = length - 1;
        int i2 = iArr[i];
        i2 |= i2 >>> 1;
        i2 |= i2 >>> 2;
        i2 |= i2 >>> 4;
        i2 |= i2 >>> 8;
        i2 |= i2 >>> 16;
        do {
            for (int i3 = 0; i3 != length; i3++) {
                create[i3] = random.nextInt();
            }
            create[i] = create[i] & i2;
        } while (Nat.gte(length, create, iArr));
        return create;
    }

    public static void subtract(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4) {
        int length = iArr.length;
        if (Nat.sub(length, iArr2, iArr3, iArr4) != 0) {
            Nat.addTo(length, iArr, iArr4);
        }
    }
}
