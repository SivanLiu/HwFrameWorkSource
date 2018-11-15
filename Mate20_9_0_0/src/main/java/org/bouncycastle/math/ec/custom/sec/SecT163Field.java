package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.math.raw.Interleave;
import org.bouncycastle.math.raw.Nat192;

public class SecT163Field {
    private static final long M35 = 34359738367L;
    private static final long M55 = 36028797018963967L;
    private static final long[] ROOT_Z = new long[]{-5270498306774157648L, 5270498306774195053L, 19634136210L};

    public static void add(long[] jArr, long[] jArr2, long[] jArr3) {
        jArr3[0] = jArr[0] ^ jArr2[0];
        jArr3[1] = jArr[1] ^ jArr2[1];
        jArr3[2] = jArr2[2] ^ jArr[2];
    }

    public static void addExt(long[] jArr, long[] jArr2, long[] jArr3) {
        jArr3[0] = jArr[0] ^ jArr2[0];
        jArr3[1] = jArr[1] ^ jArr2[1];
        jArr3[2] = jArr[2] ^ jArr2[2];
        jArr3[3] = jArr[3] ^ jArr2[3];
        jArr3[4] = jArr[4] ^ jArr2[4];
        jArr3[5] = jArr2[5] ^ jArr[5];
    }

    public static void addOne(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0] ^ 1;
        jArr2[1] = jArr[1];
        jArr2[2] = jArr[2];
    }

    public static long[] fromBigInteger(BigInteger bigInteger) {
        long[] fromBigInteger64 = Nat192.fromBigInteger64(bigInteger);
        reduce29(fromBigInteger64, 0);
        return fromBigInteger64;
    }

    protected static void implCompactExt(long[] jArr) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        long j5 = jArr[4];
        long j6 = jArr[5];
        jArr[0] = j ^ (j2 << 55);
        jArr[1] = (j2 >>> 9) ^ (j3 << 46);
        jArr[2] = (j3 >>> 18) ^ (j4 << 37);
        jArr[3] = (j4 >>> 27) ^ (j5 << 28);
        jArr[4] = (j5 >>> 36) ^ (j6 << 19);
        jArr[5] = j6 >>> 45;
    }

    protected static void implMultiply(long[] jArr, long[] jArr2, long[] jArr3) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = (jArr[2] << 18) ^ (j2 >>> 46);
        j2 = ((j2 << 9) ^ (j >>> 55)) & M55;
        j &= M55;
        long j4 = jArr2[0];
        long j5 = jArr2[1];
        long j6 = (j5 >>> 46) ^ (jArr2[2] << 18);
        long j7 = ((j4 >>> 55) ^ (j5 << 9)) & M55;
        long j8 = j4 & M55;
        long[] jArr4 = new long[10];
        long[] jArr5 = jArr4;
        implMulw(j, j8, jArr5, 0);
        implMulw(j3, j6, jArr5, 2);
        long j9 = (j ^ j2) ^ j3;
        long j10 = (j8 ^ j7) ^ j6;
        implMulw(j9, j10, jArr5, 4);
        j2 = (j2 << 1) ^ (j3 << 2);
        j6 = (j6 << 2) ^ (j7 << 1);
        implMulw(j ^ j2, j8 ^ j6, jArr5, 6);
        implMulw(j9 ^ j2, j10 ^ j6, jArr5, 8);
        j = jArr4[6] ^ jArr4[8];
        j2 = jArr4[7] ^ jArr4[9];
        j3 = (j << 1) ^ jArr4[6];
        j = (j ^ (j2 << 1)) ^ jArr4[7];
        j4 = jArr4[0];
        j5 = (jArr4[1] ^ jArr4[0]) ^ jArr4[4];
        long j11 = jArr4[1] ^ jArr4[5];
        j3 = ((j3 ^ j4) ^ (jArr4[2] << 4)) ^ (jArr4[2] << 1);
        j = (((j5 ^ j) ^ (jArr4[3] << 4)) ^ (jArr4[3] << 1)) ^ (j3 >>> 55);
        j2 = (j11 ^ j2) ^ (j >>> 55);
        j &= M55;
        j3 = ((j3 & M55) >>> 1) ^ ((j & 1) << 54);
        j3 ^= j3 << 1;
        j3 ^= j3 << 2;
        j3 ^= j3 << 4;
        j3 ^= j3 << 8;
        j3 ^= j3 << 16;
        j3 = (j3 ^ (j3 << 32)) & M55;
        j = ((j >>> 1) ^ ((j2 & 1) << 54)) ^ (j3 >>> 54);
        j ^= j << 1;
        j ^= j << 2;
        j ^= j << 4;
        j ^= j << 8;
        j ^= j << 16;
        j = (j ^ (j << 32)) & M55;
        j2 = (j2 >>> 1) ^ (j >>> 54);
        j2 ^= j2 << 1;
        j2 ^= j2 << 2;
        j2 ^= j2 << 4;
        j2 ^= j2 << 8;
        j2 ^= j2 << 16;
        j2 ^= j2 << 32;
        jArr3[0] = j4;
        jArr3[1] = (j5 ^ j3) ^ jArr4[2];
        jArr3[2] = (j3 ^ (j11 ^ j)) ^ jArr4[3];
        jArr3[3] = j2 ^ j;
        jArr3[4] = jArr4[2] ^ j2;
        jArr3[5] = jArr4[3];
        implCompactExt(jArr3);
    }

    protected static void implMulw(long j, long j2, long[] jArr, int i) {
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
        long j4 = jArr2[((int) j3) & 3];
        long j5 = 0;
        long j6 = 47;
        while (true) {
            int i3 = (int) (j3 >>> j6);
            long j7 = (jArr2[i3 & 7] ^ (jArr2[(i3 >>> 3) & 7] << 3)) ^ (jArr2[(i3 >>> 6) & i2] << 6);
            j4 ^= j7 << j6;
            j5 ^= j7 >>> (-j6);
            j6 -= 9;
            if (j6 <= null) {
                jArr[i] = M55 & j4;
                jArr[i + 1] = (j4 >>> 55) ^ (j5 << 9);
                return;
            }
            i2 = 7;
        }
    }

    protected static void implSquare(long[] jArr, long[] jArr2) {
        Interleave.expand64To128(jArr[0], jArr2, 0);
        Interleave.expand64To128(jArr[1], jArr2, 2);
        long j = jArr[2];
        jArr2[4] = Interleave.expand32to64((int) j);
        jArr2[5] = ((long) Interleave.expand8to16((int) (j >>> 32))) & BodyPartID.bodyIdMax;
    }

    public static void invert(long[] jArr, long[] jArr2) {
        if (Nat192.isZero64(jArr)) {
            throw new IllegalStateException();
        }
        long[] create64 = Nat192.create64();
        long[] create642 = Nat192.create64();
        square(jArr, create64);
        squareN(create64, 1, create642);
        multiply(create64, create642, create64);
        squareN(create642, 1, create642);
        multiply(create64, create642, create64);
        squareN(create64, 3, create642);
        multiply(create64, create642, create64);
        squareN(create642, 3, create642);
        multiply(create64, create642, create64);
        squareN(create64, 9, create642);
        multiply(create64, create642, create64);
        squareN(create642, 9, create642);
        multiply(create64, create642, create64);
        squareN(create64, 27, create642);
        multiply(create64, create642, create64);
        squareN(create642, 27, create642);
        multiply(create64, create642, create64);
        squareN(create64, 81, create642);
        multiply(create64, create642, jArr2);
    }

    public static void multiply(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat192.createExt64();
        implMultiply(jArr, jArr2, createExt64);
        reduce(createExt64, jArr3);
    }

    public static void multiplyAddToExt(long[] jArr, long[] jArr2, long[] jArr3) {
        long[] createExt64 = Nat192.createExt64();
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
        j4 ^= (((j6 >>> 35) ^ (j6 >>> 32)) ^ (j6 >>> 29)) ^ (j6 >>> 28);
        j2 ^= (((j5 << 29) ^ (j5 << 32)) ^ (j5 << 35)) ^ (j5 << 36);
        j3 = (j3 ^ ((((j6 << 29) ^ (j6 << 32)) ^ (j6 << 35)) ^ (j6 << 36))) ^ ((j5 >>> 28) ^ (((j5 >>> 35) ^ (j5 >>> 32)) ^ (j5 >>> 29)));
        j ^= (((j4 << 29) ^ (j4 << 32)) ^ (j4 << 35)) ^ (j4 << 36);
        j2 ^= (j4 >>> 28) ^ (((j4 >>> 35) ^ (j4 >>> 32)) ^ (j4 >>> 29));
        j4 = j3 >>> 35;
        jArr2[0] = (((j ^ j4) ^ (j4 << 3)) ^ (j4 << 6)) ^ (j4 << 7);
        jArr2[1] = j2;
        jArr2[2] = M35 & j3;
    }

    public static void reduce29(long[] jArr, int i) {
        int i2 = i + 2;
        long j = jArr[i2];
        long j2 = j >>> 35;
        jArr[i] = ((j2 << 7) ^ (((j2 << 3) ^ j2) ^ (j2 << 6))) ^ jArr[i];
        jArr[i2] = j & M35;
    }

    public static void sqrt(long[] jArr, long[] jArr2) {
        long[] create64 = Nat192.create64();
        long unshuffle = Interleave.unshuffle(jArr[0]);
        long unshuffle2 = Interleave.unshuffle(jArr[1]);
        long j = (unshuffle & BodyPartID.bodyIdMax) | (unshuffle2 << 32);
        create64[0] = (unshuffle >>> 32) | (unshuffle2 & -4294967296L);
        unshuffle = Interleave.unshuffle(jArr[2]);
        unshuffle2 = unshuffle & BodyPartID.bodyIdMax;
        create64[1] = unshuffle >>> 32;
        multiply(create64, ROOT_Z, jArr2);
        jArr2[0] = jArr2[0] ^ j;
        jArr2[1] = jArr2[1] ^ unshuffle2;
    }

    public static void square(long[] jArr, long[] jArr2) {
        long[] createExt64 = Nat192.createExt64();
        implSquare(jArr, createExt64);
        reduce(createExt64, jArr2);
    }

    public static void squareAddToExt(long[] jArr, long[] jArr2) {
        long[] createExt64 = Nat192.createExt64();
        implSquare(jArr, createExt64);
        addExt(jArr2, createExt64, jArr2);
    }

    public static void squareN(long[] jArr, int i, long[] jArr2) {
        long[] createExt64 = Nat192.createExt64();
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
        return ((int) (jArr[0] ^ (jArr[2] >>> 29))) & 1;
    }
}
