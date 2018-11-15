package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.math.raw.Interleave;
import org.bouncycastle.math.raw.Nat256;

public class SecT233Field {
    private static final long M41 = 2199023255551L;
    private static final long M59 = 576460752303423487L;

    public static void add(long[] jArr, long[] jArr2, long[] jArr3) {
        jArr3[0] = jArr[0] ^ jArr2[0];
        jArr3[1] = jArr[1] ^ jArr2[1];
        jArr3[2] = jArr[2] ^ jArr2[2];
        jArr3[3] = jArr2[3] ^ jArr[3];
    }

    public static void addExt(long[] jArr, long[] jArr2, long[] jArr3) {
        jArr3[0] = jArr[0] ^ jArr2[0];
        jArr3[1] = jArr[1] ^ jArr2[1];
        jArr3[2] = jArr[2] ^ jArr2[2];
        jArr3[3] = jArr[3] ^ jArr2[3];
        jArr3[4] = jArr[4] ^ jArr2[4];
        jArr3[5] = jArr[5] ^ jArr2[5];
        jArr3[6] = jArr[6] ^ jArr2[6];
        jArr3[7] = jArr2[7] ^ jArr[7];
    }

    public static void addOne(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0] ^ 1;
        jArr2[1] = jArr[1];
        jArr2[2] = jArr[2];
        jArr2[3] = jArr[3];
    }

    public static long[] fromBigInteger(BigInteger bigInteger) {
        long[] fromBigInteger64 = Nat256.fromBigInteger64(bigInteger);
        reduce23(fromBigInteger64, 0);
        return fromBigInteger64;
    }

    protected static void implCompactExt(long[] jArr) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        long j5 = jArr[4];
        long j6 = jArr[5];
        long j7 = jArr[6];
        long j8 = jArr[7];
        jArr[0] = j ^ (j2 << 59);
        jArr[1] = (j2 >>> 5) ^ (j3 << 54);
        jArr[2] = (j3 >>> 10) ^ (j4 << 49);
        jArr[3] = (j4 >>> 15) ^ (j5 << 44);
        jArr[4] = (j5 >>> 20) ^ (j6 << 39);
        jArr[5] = (j6 >>> 25) ^ (j7 << 34);
        jArr[6] = (j7 >>> 30) ^ (j8 << 29);
        jArr[7] = j8 >>> 35;
    }

    protected static void implExpand(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        jArr2[0] = j & M59;
        jArr2[1] = ((j >>> 59) ^ (j2 << 5)) & M59;
        jArr2[2] = ((j2 >>> 54) ^ (j3 << 10)) & M59;
        jArr2[3] = (j3 >>> 49) ^ (j4 << 15);
    }

    protected static void implMultiply(long[] jArr, long[] jArr2, long[] jArr3) {
        int i;
        long[] jArr4 = new long[4];
        long[] jArr5 = new long[4];
        implExpand(jArr, jArr4);
        implExpand(jArr2, jArr5);
        long[] jArr6 = jArr3;
        implMulwAcc(jArr4[0], jArr5[0], jArr6, 0);
        implMulwAcc(jArr4[1], jArr5[1], jArr6, 1);
        implMulwAcc(jArr4[2], jArr5[2], jArr6, 2);
        implMulwAcc(jArr4[3], jArr5[3], jArr6, 3);
        for (i = 5; i > 0; i--) {
            jArr3[i] = jArr3[i] ^ jArr3[i - 1];
        }
        implMulwAcc(jArr4[0] ^ jArr4[1], jArr5[0] ^ jArr5[1], jArr3, 1);
        implMulwAcc(jArr4[2] ^ jArr4[3], jArr5[2] ^ jArr5[3], jArr3, 3);
        for (i = 7; i > 1; i--) {
            jArr3[i] = jArr3[i] ^ jArr3[i - 2];
        }
        long j = jArr4[0] ^ jArr4[2];
        long j2 = jArr4[1] ^ jArr4[3];
        long j3 = jArr5[0] ^ jArr5[2];
        long j4 = jArr5[1] ^ jArr5[3];
        implMulwAcc(j ^ j2, j3 ^ j4, jArr3, 3);
        long[] jArr7 = new long[3];
        implMulwAcc(j, j3, jArr7, 0);
        implMulwAcc(j2, j4, jArr7, 1);
        long j5 = jArr7[0];
        long j6 = jArr7[1];
        j4 = jArr7[2];
        jArr3[2] = jArr3[2] ^ j5;
        jArr3[3] = (j5 ^ j6) ^ jArr3[3];
        jArr3[4] = jArr3[4] ^ (j4 ^ j6);
        jArr3[5] = jArr3[5] ^ j4;
        implCompactExt(jArr3);
    }

    protected static void implMulwAcc(long j, long j2, long[] jArr, int i) {
        long j3 = j;
        long[] jArr2 = new long[8];
        jArr2[1] = j2;
        jArr2[2] = jArr2[1] << 1;
        jArr2[3] = jArr2[2] ^ j2;
        jArr2[4] = jArr2[2] << 1;
        jArr2[5] = jArr2[4] ^ j2;
        jArr2[6] = jArr2[3] << 1;
        int i2 = 7;
        jArr2[7] = jArr2[6] ^ j2;
        int i3 = (int) j3;
        long j4 = (jArr2[(i3 >>> 3) & 7] << 3) ^ jArr2[i3 & 7];
        long j5 = 0;
        long j6 = 54;
        while (true) {
            int i4 = (int) (j3 >>> j6);
            long j7 = jArr2[i4 & 7] ^ (jArr2[(i4 >>> 3) & i2] << 3);
            j4 ^= j7 << j6;
            j5 ^= j7 >>> (-j6);
            j6 -= 6;
            if (j6 <= null) {
                jArr[i] = jArr[i] ^ (M59 & j4);
                int i5 = i + 1;
                jArr[i5] = ((j4 >>> 59) ^ (j5 << 5)) ^ jArr[i5];
                return;
            }
            i2 = 7;
        }
    }

    protected static void implSquare(long[] jArr, long[] jArr2) {
        Interleave.expand64To128(jArr[0], jArr2, 0);
        Interleave.expand64To128(jArr[1], jArr2, 2);
        Interleave.expand64To128(jArr[2], jArr2, 4);
        long j = jArr[3];
        jArr2[6] = Interleave.expand32to64((int) j);
        jArr2[7] = ((long) Interleave.expand16to32((int) (j >>> 32))) & BodyPartID.bodyIdMax;
    }

    public static void invert(long[] jArr, long[] jArr2) {
        if (Nat256.isZero64(jArr)) {
            throw new IllegalStateException();
        }
        long[] create64 = Nat256.create64();
        long[] create642 = Nat256.create64();
        square(jArr, create64);
        multiply(create64, jArr, create64);
        square(create64, create64);
        multiply(create64, jArr, create64);
        squareN(create64, 3, create642);
        multiply(create642, create64, create642);
        square(create642, create642);
        multiply(create642, jArr, create642);
        squareN(create642, 7, create64);
        multiply(create64, create642, create64);
        squareN(create64, 14, create642);
        multiply(create642, create64, create642);
        square(create642, create642);
        multiply(create642, jArr, create642);
        squareN(create642, 29, create64);
        multiply(create64, create642, create64);
        squareN(create64, 58, create642);
        multiply(create642, create64, create642);
        squareN(create642, 116, create64);
        multiply(create64, create642, create64);
        square(create64, jArr2);
    }

    public static void multiply(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat256.createExt64();
        implMultiply(jArr, jArr2, createExt64);
        reduce(createExt64, jArr3);
    }

    public static void multiplyAddToExt(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat256.createExt64();
        implMultiply(jArr, jArr2, createExt64);
        addExt(jArr3, createExt64, jArr3);
    }

    public static void reduce(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        long j5 = jArr[4];
        long j6 = jArr[5];
        long j7 = jArr[6];
        long j8 = jArr[7];
        j6 ^= j8 >>> 31;
        j5 = (j5 ^ ((j8 >>> 41) ^ (j8 << 33))) ^ (j7 >>> 31);
        j4 = ((j4 ^ (j8 << 23)) ^ ((j7 >>> 41) ^ (j7 << 33))) ^ (j6 >>> 31);
        j ^= j5 << 23;
        j2 = (j2 ^ (j6 << 23)) ^ ((j5 >>> 41) ^ (j5 << 33));
        j3 = ((j3 ^ (j7 << 23)) ^ ((j6 >>> 41) ^ (j6 << 33))) ^ (j5 >>> 31);
        j5 = j4 >>> 41;
        jArr2[0] = j ^ j5;
        jArr2[1] = (j5 << 10) ^ j2;
        jArr2[2] = j3;
        jArr2[3] = M41 & j4;
    }

    public static void reduce23(long[] jArr, int i) {
        int i2 = i + 3;
        long j = jArr[i2];
        long j2 = j >>> 41;
        jArr[i] = jArr[i] ^ j2;
        i++;
        jArr[i] = (j2 << 10) ^ jArr[i];
        jArr[i2] = j & M41;
    }

    public static void sqrt(long[] jArr, long[] jArr2) {
        long[] jArr3 = jArr2;
        long unshuffle = Interleave.unshuffle(jArr[0]);
        long unshuffle2 = Interleave.unshuffle(jArr[1]);
        long j = (unshuffle & BodyPartID.bodyIdMax) | (unshuffle2 << 32);
        unshuffle = (unshuffle >>> 32) | (unshuffle2 & -4294967296L);
        unshuffle2 = Interleave.unshuffle(jArr[2]);
        long j2 = unshuffle;
        long unshuffle3 = Interleave.unshuffle(jArr[3]);
        long j3 = (BodyPartID.bodyIdMax & unshuffle2) | (unshuffle3 << 32);
        unshuffle3 = (unshuffle3 & -4294967296L) | (unshuffle2 >>> 32);
        unshuffle2 = unshuffle3 >>> 27;
        unshuffle3 ^= (j2 >>> 27) | (unshuffle3 << 37);
        long j4 = j2 ^ (j2 << 37);
        long[] createExt64 = Nat256.createExt64();
        int[] iArr = new int[]{32, 117, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256};
        int i = 0;
        while (i < iArr.length) {
            int i2 = iArr[i] >>> 6;
            int i3 = iArr[i] & 63;
            createExt64[i2] = createExt64[i2] ^ (j4 << i3);
            int i4 = i2 + 1;
            int[] iArr2 = iArr;
            int i5 = -i3;
            createExt64[i4] = createExt64[i4] ^ ((unshuffle3 << i3) | (j4 >>> i5));
            i4 = i2 + 2;
            createExt64[i4] = createExt64[i4] ^ ((unshuffle2 << i3) | (unshuffle3 >>> i5));
            i2 += 3;
            createExt64[i2] = createExt64[i2] ^ (unshuffle2 >>> i5);
            i++;
            iArr = iArr2;
        }
        reduce(createExt64, jArr3);
        jArr3[0] = jArr3[0] ^ j;
        jArr3[1] = jArr3[1] ^ j3;
    }

    public static void square(long[] jArr, long[] jArr2) {
        long[] createExt64 = Nat256.createExt64();
        implSquare(jArr, createExt64);
        reduce(createExt64, jArr2);
    }

    public static void squareAddToExt(long[] jArr, long[] jArr2) {
        long[] createExt64 = Nat256.createExt64();
        implSquare(jArr, createExt64);
        addExt(jArr2, createExt64, jArr2);
    }

    public static void squareN(long[] jArr, int i, long[] jArr2) {
        long[] createExt64 = Nat256.createExt64();
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
        return ((int) (jArr[0] ^ (jArr[2] >>> 31))) & 1;
    }
}
