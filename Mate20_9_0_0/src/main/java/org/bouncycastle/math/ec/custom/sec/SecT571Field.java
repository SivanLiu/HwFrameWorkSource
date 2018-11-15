package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.math.raw.Interleave;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat576;

public class SecT571Field {
    private static final long M59 = 576460752303423487L;
    private static final long RM = -1190112520884487202L;
    private static final long[] ROOT_Z = new long[]{3161836309350906777L, -7642453882179322845L, -3821226941089661423L, 7312758566309945096L, -556661012383879292L, 8945041530681231562L, -4750851271514160027L, 6847946401097695794L, 541669439031730457L};

    private static void add(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3) {
        for (int i4 = 0; i4 < 9; i4++) {
            jArr3[i3 + i4] = jArr[i + i4] ^ jArr2[i2 + i4];
        }
    }

    public static void add(long[] jArr, long[] jArr2, long[] jArr3) {
        for (int i = 0; i < 9; i++) {
            jArr3[i] = jArr[i] ^ jArr2[i];
        }
    }

    private static void addBothTo(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3) {
        for (int i4 = 0; i4 < 9; i4++) {
            int i5 = i3 + i4;
            jArr3[i5] = jArr3[i5] ^ (jArr[i + i4] ^ jArr2[i2 + i4]);
        }
    }

    public static void addBothTo(long[] jArr, long[] jArr2, long[] jArr3) {
        for (int i = 0; i < 9; i++) {
            jArr3[i] = jArr3[i] ^ (jArr[i] ^ jArr2[i]);
        }
    }

    public static void addExt(long[] jArr, long[] jArr2, long[] jArr3) {
        for (int i = 0; i < 18; i++) {
            jArr3[i] = jArr[i] ^ jArr2[i];
        }
    }

    public static void addOne(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0] ^ 1;
        for (int i = 1; i < 9; i++) {
            jArr2[i] = jArr[i];
        }
    }

    public static long[] fromBigInteger(BigInteger bigInteger) {
        long[] fromBigInteger64 = Nat576.fromBigInteger64(bigInteger);
        reduce5(fromBigInteger64, 0);
        return fromBigInteger64;
    }

    protected static void implMultiply(long[] jArr, long[] jArr2, long[] jArr3) {
        implMultiplyPrecomp(jArr, precompMultiplicand(jArr2), jArr3);
    }

    protected static void implMultiplyPrecomp(long[] jArr, long[] jArr2, long[] jArr3) {
        int i;
        int i2;
        int i3;
        int i4 = 56;
        for (i = 56; i >= 0; i -= 8) {
            for (i2 = 1; i2 < 9; i2 += 2) {
                i3 = (int) (jArr[i2] >>> i);
                long[] jArr4 = jArr2;
                long[] jArr5 = jArr2;
                long[] jArr6 = jArr3;
                addBothTo(jArr4, 9 * (i3 & 15), jArr5, 9 * (((i3 >>> 4) & 15) + 16), jArr6, i2 - 1);
            }
            Nat.shiftUpBits64(16, jArr3, 0, 8, 0);
        }
        while (i4 >= 0) {
            for (i = 0; i < 9; i += 2) {
                i2 = (int) (jArr[i] >>> i4);
                i3 = i2 & 15;
                int i5 = 9 * (((i2 >>> 4) & 15) + 16);
                addBothTo(jArr2, i3 * 9, jArr2, i5, jArr3, i);
            }
            if (i4 > 0) {
                Nat.shiftUpBits64(18, jArr3, 0, 8, 0);
            }
            i4 -= 8;
        }
    }

    protected static void implMulwAcc(long[] jArr, long j, long[] jArr2, int i) {
        long[] jArr3 = new long[32];
        jArr3[1] = j;
        for (int i2 = 2; i2 < 32; i2 += 2) {
            jArr3[i2] = jArr3[i2 >>> 1] << 1;
            jArr3[i2 + 1] = jArr3[i2] ^ j;
        }
        int i3 = 0;
        long j2 = 0;
        while (i3 < 9) {
            int i4;
            long j3 = jArr[i3];
            j2 ^= jArr3[((int) j3) & 31];
            long j4 = 60;
            long j5 = 0;
            do {
                long j6 = jArr3[((int) (j3 >>> j4)) & 31];
                j2 ^= j6 << j4;
                j5 ^= j6 >>> (-j4);
                j4 -= 5;
            } while (j4 > null);
            for (i4 = 0; i4 < 4; i4++) {
                j3 = (RM & j3) >>> 1;
                j5 ^= ((j << i4) >> 63) & j3;
            }
            i4 = i + i3;
            jArr2[i4] = jArr2[i4] ^ j2;
            i3++;
            j2 = j5;
        }
        int i5 = i + 9;
        jArr2[i5] = jArr2[i5] ^ j2;
    }

    protected static void implSquare(long[] jArr, long[] jArr2) {
        for (int i = 0; i < 9; i++) {
            Interleave.expand64To128(jArr[i], jArr2, i << 1);
        }
    }

    public static void invert(long[] jArr, long[] jArr2) {
        if (Nat576.isZero64(jArr)) {
            throw new IllegalStateException();
        }
        long[] create64 = Nat576.create64();
        long[] create642 = Nat576.create64();
        long[] create643 = Nat576.create64();
        square(jArr, create643);
        square(create643, create64);
        square(create64, create642);
        multiply(create64, create642, create64);
        squareN(create64, 2, create642);
        multiply(create64, create642, create64);
        multiply(create64, create643, create64);
        squareN(create64, 5, create642);
        multiply(create64, create642, create64);
        squareN(create642, 5, create642);
        multiply(create64, create642, create64);
        squareN(create64, 15, create642);
        multiply(create64, create642, create643);
        squareN(create643, 30, create64);
        squareN(create64, 30, create642);
        multiply(create64, create642, create64);
        squareN(create64, 60, create642);
        multiply(create64, create642, create64);
        squareN(create642, 60, create642);
        multiply(create64, create642, create64);
        squareN(create64, CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256, create642);
        multiply(create64, create642, create64);
        squareN(create642, CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256, create642);
        multiply(create64, create642, create64);
        multiply(create64, create643, jArr2);
    }

    public static void multiply(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat576.createExt64();
        implMultiply(jArr, jArr2, createExt64);
        reduce(createExt64, jArr3);
    }

    public static void multiplyAddToExt(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat576.createExt64();
        implMultiply(jArr, jArr2, createExt64);
        addExt(jArr3, createExt64, jArr3);
    }

    public static void multiplyPrecomp(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat576.createExt64();
        implMultiplyPrecomp(jArr, jArr2, createExt64);
        reduce(createExt64, jArr3);
    }

    public static void multiplyPrecompAddToExt(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat576.createExt64();
        implMultiplyPrecomp(jArr, jArr2, createExt64);
        addExt(jArr3, createExt64, jArr3);
    }

    public static long[] precompMultiplicand(long[] jArr) {
        Object obj = new long[288];
        int i = 0;
        System.arraycopy(jArr, 0, obj, 9, 9);
        int i2 = 7;
        while (i2 > 0) {
            int i3 = i + 18;
            Nat.shiftUpBit64(9, obj, i3 >>> 1, 0, obj, i3);
            reduce5(obj, i3);
            add(obj, 9, obj, i3, obj, i3 + 9);
            i2--;
            i = i3;
        }
        Nat.shiftUpBits64(CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA, obj, 0, 4, 0, obj, CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA);
        return obj;
    }

    public static void reduce(long[] jArr, long[] jArr2) {
        long j = jArr[9];
        long j2 = jArr[17];
        j = (((j ^ (j2 >>> 59)) ^ (j2 >>> 57)) ^ (j2 >>> 54)) ^ (j2 >>> 49);
        j2 = (j2 << 15) ^ (((jArr[8] ^ (j2 << 5)) ^ (j2 << 7)) ^ (j2 << 10));
        for (int i = 16; i >= 10; i--) {
            long j3 = jArr[i];
            jArr2[i - 8] = (((j2 ^ (j3 >>> 59)) ^ (j3 >>> 57)) ^ (j3 >>> 54)) ^ (j3 >>> 49);
            j2 = (((jArr[i - 9] ^ (j3 << 5)) ^ (j3 << 7)) ^ (j3 << 10)) ^ (j3 << 15);
        }
        jArr2[1] = (((j2 ^ (j >>> 59)) ^ (j >>> 57)) ^ (j >>> 54)) ^ (j >>> 49);
        j = (j << 15) ^ (((jArr[0] ^ (j << 5)) ^ (j << 7)) ^ (j << 10));
        long j4 = jArr2[8];
        long j5 = j4 >>> 59;
        jArr2[0] = (((j ^ j5) ^ (j5 << 2)) ^ (j5 << 5)) ^ (j5 << 10);
        jArr2[8] = M59 & j4;
    }

    public static void reduce5(long[] jArr, int i) {
        int i2 = i + 8;
        long j = jArr[i2];
        long j2 = j >>> 59;
        jArr[i] = ((j2 << 10) ^ (((j2 << 2) ^ j2) ^ (j2 << 5))) ^ jArr[i];
        jArr[i2] = j & M59;
    }

    public static void sqrt(long[] jArr, long[] jArr2) {
        long[] create64 = Nat576.create64();
        long[] create642 = Nat576.create64();
        int i = 0;
        int i2 = 0;
        while (i < 4) {
            int i3 = i2 + 1;
            long unshuffle = Interleave.unshuffle(jArr[i2]);
            i2 = i3 + 1;
            long unshuffle2 = Interleave.unshuffle(jArr[i3]);
            create64[i] = (BodyPartID.bodyIdMax & unshuffle) | (unshuffle2 << 32);
            create642[i] = (unshuffle >>> 32) | (-4294967296L & unshuffle2);
            i++;
        }
        long unshuffle3 = Interleave.unshuffle(jArr[i2]);
        create64[4] = BodyPartID.bodyIdMax & unshuffle3;
        create642[4] = unshuffle3 >>> 32;
        multiply(create642, ROOT_Z, jArr2);
        add(jArr2, create64, jArr2);
    }

    public static void square(long[] jArr, long[] jArr2) {
        long[] createExt64 = Nat576.createExt64();
        implSquare(jArr, createExt64);
        reduce(createExt64, jArr2);
    }

    public static void squareAddToExt(long[] jArr, long[] jArr2) {
        long[] createExt64 = Nat576.createExt64();
        implSquare(jArr, createExt64);
        addExt(jArr2, createExt64, jArr2);
    }

    public static void squareN(long[] jArr, int i, long[] jArr2) {
        long[] createExt64 = Nat576.createExt64();
        implSquare(jArr, createExt64);
        while (true) {
            reduce(createExt64, jArr2);
            i--;
            if (i > 0) {
                implSquare(jArr2, createExt64);
            } else {
                return;
            }
        }
    }

    public static int trace(long[] jArr) {
        return ((int) ((jArr[0] ^ (jArr[8] >>> 49)) ^ (jArr[8] >>> 57))) & 1;
    }
}
