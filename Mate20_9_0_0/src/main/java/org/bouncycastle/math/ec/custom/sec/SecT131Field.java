package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.math.raw.Interleave;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat192;

public class SecT131Field {
    private static final long M03 = 7;
    private static final long M44 = 17592186044415L;
    private static final long[] ROOT_Z = new long[]{2791191049453778211L, 2791191049453778402L, 6};

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
        jArr3[4] = jArr2[4] ^ jArr[4];
    }

    public static void addOne(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0] ^ 1;
        jArr2[1] = jArr[1];
        jArr2[2] = jArr[2];
    }

    public static long[] fromBigInteger(BigInteger bigInteger) {
        long[] fromBigInteger64 = Nat192.fromBigInteger64(bigInteger);
        reduce61(fromBigInteger64, 0);
        return fromBigInteger64;
    }

    protected static void implCompactExt(long[] jArr) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        long j5 = jArr[4];
        long j6 = jArr[5];
        jArr[0] = j ^ (j2 << 44);
        jArr[1] = (j2 >>> 20) ^ (j3 << 24);
        jArr[2] = ((j3 >>> 40) ^ (j4 << 4)) ^ (j5 << 48);
        jArr[3] = ((j4 >>> 60) ^ (j6 << 28)) ^ (j5 >>> 16);
        jArr[4] = j6 >>> 36;
        jArr[5] = 0;
    }

    protected static void implMultiply(long[] jArr, long[] jArr2, long[] jArr3) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = ((jArr[2] << 40) ^ (j2 >>> 24)) & M44;
        j2 = ((j2 << 20) ^ (j >>> 44)) & M44;
        j &= M44;
        long j4 = jArr2[0];
        long j5 = jArr2[1];
        long j6 = ((j5 >>> 24) ^ (jArr2[2] << 40)) & M44;
        long j7 = ((j4 >>> 44) ^ (j5 << 20)) & M44;
        long j8 = j4 & M44;
        long[] jArr4 = new long[10];
        implMulw(j, j8, jArr4, 0);
        long[] jArr5 = jArr4;
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
        long j11 = (jArr4[1] ^ jArr4[0]) ^ jArr4[4];
        long j12 = jArr4[1] ^ jArr4[5];
        j3 = ((j3 ^ j4) ^ (jArr4[2] << 4)) ^ (jArr4[2] << 1);
        j = (((j11 ^ j) ^ (jArr4[3] << 4)) ^ (jArr4[3] << 1)) ^ (j3 >>> 44);
        j2 = (j12 ^ j2) ^ (j >>> 44);
        j &= M44;
        j3 = ((j3 & M44) >>> 1) ^ ((j & 1) << 43);
        j3 ^= j3 << 1;
        j3 ^= j3 << 2;
        j3 ^= j3 << 4;
        j3 ^= j3 << 8;
        j3 ^= j3 << 16;
        j3 = (j3 ^ (j3 << 32)) & M44;
        j = ((j >>> 1) ^ ((j2 & 1) << 43)) ^ (j3 >>> 43);
        j ^= j << 1;
        j ^= j << 2;
        j ^= j << 4;
        j ^= j << 8;
        j ^= j << 16;
        j = (j ^ (j << 32)) & M44;
        j2 = (j2 >>> 1) ^ (j >>> 43);
        j2 ^= j2 << 1;
        j2 ^= j2 << 2;
        j2 ^= j2 << 4;
        j2 ^= j2 << 8;
        j2 ^= j2 << 16;
        j2 ^= j2 << 32;
        jArr3[0] = j4;
        jArr3[1] = (j11 ^ j3) ^ jArr4[2];
        jArr3[2] = (j3 ^ (j12 ^ j)) ^ jArr4[3];
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
        int i3 = (int) j3;
        long j4 = (jArr2[(i3 >>> 6) & 7] << 6) ^ (jArr2[i3 & 7] ^ (jArr2[(i3 >>> 3) & 7] << 3));
        long j5 = 0;
        long j6 = 33;
        while (true) {
            int i4 = (int) (j3 >>> j6);
            long j7 = ((jArr2[i4 & 7] ^ (jArr2[(i4 >>> 3) & 7] << 3)) ^ (jArr2[(i4 >>> 6) & 7] << 6)) ^ (jArr2[(i4 >>> 9) & i2] << 9);
            j4 ^= j7 << j6;
            j5 ^= j7 >>> (-j6);
            j6 -= 12;
            if (j6 <= null) {
                jArr[i] = M44 & j4;
                jArr[i + 1] = (j4 >>> 44) ^ (j5 << 20);
                return;
            }
            i2 = 7;
        }
    }

    protected static void implSquare(long[] jArr, long[] jArr2) {
        Interleave.expand64To128(jArr[0], jArr2, 0);
        Interleave.expand64To128(jArr[1], jArr2, 2);
        jArr2[4] = ((long) Interleave.expand8to16((int) jArr[2])) & BodyPartID.bodyIdMax;
    }

    public static void invert(long[] jArr, long[] jArr2) {
        if (Nat192.isZero64(jArr)) {
            throw new IllegalStateException();
        }
        long[] create64 = Nat192.create64();
        long[] create642 = Nat192.create64();
        square(jArr, create64);
        multiply(create64, jArr, create64);
        squareN(create64, 2, create642);
        multiply(create642, create64, create642);
        squareN(create642, 4, create64);
        multiply(create64, create642, create64);
        squareN(create64, 8, create642);
        multiply(create642, create64, create642);
        squareN(create642, 16, create64);
        multiply(create64, create642, create64);
        squareN(create64, 32, create642);
        multiply(create642, create64, create642);
        square(create642, create642);
        multiply(create642, jArr, create642);
        squareN(create642, 65, create64);
        multiply(create64, create642, create64);
        square(create64, jArr2);
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
        j4 ^= j5 >>> 59;
        j ^= (j4 << 61) ^ (j4 << 63);
        j2 = (j2 ^ ((j5 << 61) ^ (j5 << 63))) ^ ((((j4 >>> 3) ^ (j4 >>> 1)) ^ j4) ^ (j4 << 5));
        j3 = (j3 ^ ((((j5 >>> 3) ^ (j5 >>> 1)) ^ j5) ^ (j5 << 5))) ^ (j4 >>> 59);
        j4 = j3 >>> 3;
        jArr2[0] = (((j ^ j4) ^ (j4 << 2)) ^ (j4 << 3)) ^ (j4 << 8);
        jArr2[1] = (j4 >>> 56) ^ j2;
        jArr2[2] = M03 & j3;
    }

    public static void reduce61(long[] jArr, int i) {
        int i2 = i + 2;
        long j = jArr[i2];
        long j2 = j >>> 3;
        jArr[i] = jArr[i] ^ ((((j2 << 2) ^ j2) ^ (j2 << 3)) ^ (j2 << 8));
        i++;
        jArr[i] = (j2 >>> 56) ^ jArr[i];
        jArr[i2] = j & M03;
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
        long[] create64 = Nat.create64(5);
        implSquare(jArr, create64);
        reduce(create64, jArr2);
    }

    public static void squareAddToExt(long[] jArr, long[] jArr2) {
        long[] create64 = Nat.create64(5);
        implSquare(jArr, create64);
        addExt(jArr2, create64, jArr2);
    }

    public static void squareN(long[] jArr, int i, long[] jArr2) {
        long[] create64 = Nat.create64(5);
        implSquare(jArr, create64);
        while (true) {
            reduce(create64, jArr2);
            i--;
            if (i > 0) {
                implSquare(jArr2, create64);
            } else {
                return;
            }
        }
    }

    public static int trace(long[] jArr) {
        return ((int) ((jArr[0] ^ (jArr[1] >>> 59)) ^ (jArr[2] >>> 1))) & 1;
    }
}
