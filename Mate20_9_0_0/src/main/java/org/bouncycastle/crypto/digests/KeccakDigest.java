package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public class KeccakDigest implements ExtendedDigest {
    private static long[] KeccakRoundConstants = new long[]{1, 32898, -9223372036854742902L, -9223372034707259392L, 32907, 2147483649L, -9223372034707259263L, -9223372036854743031L, 138, 136, 2147516425L, 2147483658L, 2147516555L, -9223372036854775669L, -9223372036854742903L, -9223372036854743037L, -9223372036854743038L, -9223372036854775680L, 32778, -9223372034707292150L, -9223372034707259263L, -9223372036854742912L, 2147483649L, -9223372034707259384L};
    protected int bitsInQueue;
    protected byte[] dataQueue;
    protected int fixedOutputLength;
    protected int rate;
    protected boolean squeezing;
    protected long[] state;

    public KeccakDigest() {
        this(288);
    }

    public KeccakDigest(int i) {
        this.state = new long[25];
        this.dataQueue = new byte[192];
        init(i);
    }

    public KeccakDigest(KeccakDigest keccakDigest) {
        this.state = new long[25];
        this.dataQueue = new byte[192];
        System.arraycopy(keccakDigest.state, 0, this.state, 0, keccakDigest.state.length);
        System.arraycopy(keccakDigest.dataQueue, 0, this.dataQueue, 0, keccakDigest.dataQueue.length);
        this.rate = keccakDigest.rate;
        this.bitsInQueue = keccakDigest.bitsInQueue;
        this.fixedOutputLength = keccakDigest.fixedOutputLength;
        this.squeezing = keccakDigest.squeezing;
    }

    private void KeccakAbsorb(byte[] bArr, int i) {
        int i2 = this.rate >> 6;
        for (int i3 = 0; i3 < i2; i3++) {
            long[] jArr = this.state;
            jArr[i3] = jArr[i3] ^ Pack.littleEndianToLong(bArr, i);
            i += 8;
        }
        KeccakPermutation();
    }

    private void KeccakExtract() {
        Pack.longToLittleEndian(this.state, 0, this.rate >> 6, this.dataQueue, 0);
    }

    private void KeccakPermutation() {
        long[] jArr = this.state;
        int i = 0;
        long j = jArr[0];
        long j2 = 1;
        long j3 = jArr[1];
        long j4 = jArr[2];
        long j5 = 3;
        long j6 = jArr[3];
        long j7 = jArr[4];
        long j8 = jArr[5];
        long j9 = jArr[6];
        long j10 = jArr[7];
        long j11 = jArr[8];
        long j12 = jArr[9];
        long j13 = jArr[10];
        long j14 = jArr[11];
        long j15 = jArr[12];
        long j16 = jArr[13];
        long j17 = jArr[14];
        long j18 = jArr[15];
        long j19 = jArr[16];
        long j20 = jArr[17];
        long j21 = jArr[18];
        long j22 = jArr[19];
        long j23 = jArr[20];
        long j24 = jArr[21];
        long j25 = jArr[22];
        long j26 = jArr[23];
        long j27 = jArr[24];
        long j28 = j22;
        j22 = j17;
        j17 = j12;
        j12 = j7;
        j7 = j6;
        j6 = j4;
        j4 = j3;
        j3 = j;
        int i2 = 0;
        while (i2 < 24) {
            long j29 = (((j3 ^ j8) ^ j13) ^ j18) ^ j23;
            long j30 = (((j4 ^ j9) ^ j14) ^ j19) ^ j24;
            long j31 = (((j6 ^ j10) ^ j15) ^ j20) ^ j25;
            long j32 = (((j7 ^ j11) ^ j16) ^ j21) ^ j26;
            long j33 = (((j12 ^ j17) ^ j22) ^ j28) ^ j27;
            long j34 = ((j30 << j2) | (j30 >>> -1)) ^ j33;
            long j35 = ((j31 << j2) | (j31 >>> -1)) ^ j29;
            j30 = ((j32 << j2) | (j32 >>> -1)) ^ j30;
            j31 = ((j33 << j2) | (j33 >>> -1)) ^ j31;
            j29 = ((j29 << j2) | (j29 >>> -1)) ^ j32;
            j8 ^= j34;
            j13 ^= j34;
            j18 ^= j34;
            j23 ^= j34;
            j4 ^= j35;
            j9 ^= j35;
            j14 ^= j35;
            j19 ^= j35;
            j24 ^= j35;
            j10 ^= j30;
            j15 ^= j30;
            j20 ^= j30;
            j25 ^= j30;
            j7 ^= j31;
            j11 ^= j31;
            j16 ^= j31;
            j21 ^= j31;
            j26 ^= j31;
            j12 ^= j29;
            j17 ^= j29;
            j22 ^= j29;
            j28 ^= j29;
            j27 ^= j29;
            j4 = (j4 << j2) | (j4 >>> 63);
            int i3 = i2;
            long j36 = (j9 << 44) | (j9 >>> 20);
            long j37 = j3 ^ j34;
            j2 = (j17 << 20) | (j17 >>> 44);
            long j38 = j4;
            long j39 = (j25 << 61) | (j25 >>> j5);
            long j40 = j6 ^ j30;
            long j41 = (j22 << 39) | (j22 >>> 25);
            long j42 = (j40 << 62) | (j40 >>> 2);
            j6 = (j15 << 43) | (j15 >>> 21);
            long j43 = (j23 << 18) | (j23 >>> 46);
            long j44 = (j16 << 25) | (j16 >>> 39);
            long j45 = (j28 << 8) | (j28 >>> 56);
            long j46 = (j26 << 56) | (j26 >>> 8);
            long j47 = (j18 << 41) | (j18 >>> 23);
            long j48 = (j12 << 27) | (j12 >>> 37);
            long j49 = (j27 << 14) | (j27 >>> 50);
            long j50 = j39;
            long j51 = (j24 << 2) | (j24 >>> 62);
            long j52 = (j11 << 55) | (j11 >>> 9);
            long j53 = (j19 << 45) | (j19 >>> 19);
            long j54 = (j8 << 36) | (j8 >>> 28);
            long j55 = j54;
            j54 = (j21 << 21) | (j21 >>> 43);
            long j56 = (j7 >>> 36) | (j7 << 28);
            long j57 = (j20 << 15) | (j20 >>> 49);
            long j58 = (j14 << 10) | (j14 >>> 54);
            long j59 = (j10 << 6) | (j10 >>> 58);
            long j60 = (j13 << 3) | (j13 >>> 61);
            long j61 = j37 ^ ((~j36) & j6);
            long j62 = ((~j6) & j54) ^ j36;
            j6 ^= (~j54) & j49;
            j7 = ((~j49) & j37) ^ j54;
            j12 = j49 ^ (j36 & (~j37));
            j8 = j56 ^ ((~j2) & j60);
            j36 = j60;
            j9 = j2 ^ ((~j36) & j53);
            j54 = j53;
            j10 = j36 ^ ((~j54) & j50);
            j36 = j50;
            j11 = j54 ^ ((~j36) & j56);
            j17 = j36 ^ ((~j56) & j2);
            j36 = j59;
            j13 = j38 ^ ((~j36) & j44);
            long j63 = j44;
            j14 = j36 ^ ((~j63) & j45);
            j3 = j45;
            j15 = j63 ^ ((~j3) & j43);
            j63 = j43;
            j16 = j3 ^ ((~j63) & j38);
            j22 = j63 ^ (j36 & (~j38));
            j36 = j55;
            j18 = j48 ^ ((~j36) & j58);
            j63 = j58;
            j19 = j36 ^ ((~j63) & j57);
            j3 = j57;
            j20 = j63 ^ ((~j3) & j46);
            j63 = j46;
            j21 = j3 ^ ((~j63) & j48);
            j28 = j63 ^ (j36 & (~j48));
            j36 = j52;
            j23 = j42 ^ ((~j36) & j41);
            j63 = j41;
            j24 = j36 ^ ((~j63) & j47);
            j3 = j47;
            j25 = j63 ^ ((~j3) & j51);
            j63 = j51;
            j26 = j3 ^ ((~j63) & j42);
            j27 = j63 ^ (j36 & (~j42));
            j3 = j61 ^ KeccakRoundConstants[i3];
            i2 = i3 + 1;
            j4 = j62;
            i = 0;
            j2 = 1;
            j5 = 3;
        }
        jArr[i] = j3;
        jArr[1] = j4;
        jArr[2] = j6;
        jArr[3] = j7;
        jArr[4] = j12;
        jArr[5] = j8;
        jArr[6] = j9;
        jArr[7] = j10;
        jArr[8] = j11;
        jArr[9] = j17;
        jArr[10] = j13;
        jArr[11] = j14;
        jArr[12] = j15;
        jArr[13] = j16;
        jArr[14] = j22;
        jArr[15] = j18;
        jArr[16] = j19;
        jArr[17] = j20;
        jArr[18] = j21;
        jArr[19] = j28;
        jArr[20] = j23;
        jArr[21] = j24;
        jArr[22] = j25;
        jArr[23] = j26;
        jArr[24] = j27;
    }

    private void init(int i) {
        if (i == 128 || i == 224 || i == 256 || i == 288 || i == 384 || i == 512) {
            initSponge(1600 - (i << 1));
            return;
        }
        throw new IllegalArgumentException("bitLength must be one of 128, 224, 256, 288, 384, or 512.");
    }

    private void initSponge(int i) {
        if (i <= 0 || i >= 1600 || i % 64 != 0) {
            throw new IllegalStateException("invalid rate value");
        }
        this.rate = i;
        for (int i2 = 0; i2 < this.state.length; i2++) {
            this.state[i2] = 0;
        }
        Arrays.fill(this.dataQueue, (byte) 0);
        this.bitsInQueue = 0;
        this.squeezing = false;
        this.fixedOutputLength = (1600 - i) / 2;
    }

    private void padAndSwitchToSqueezingPhase() {
        byte[] bArr = this.dataQueue;
        int i = this.bitsInQueue >> 3;
        bArr[i] = (byte) (bArr[i] | ((byte) ((int) (1 << (this.bitsInQueue & 7)))));
        int i2 = this.bitsInQueue + 1;
        this.bitsInQueue = i2;
        int i3 = 0;
        if (i2 == this.rate) {
            KeccakAbsorb(this.dataQueue, 0);
            this.bitsInQueue = 0;
        }
        i2 = this.bitsInQueue >> 6;
        int i4 = this.bitsInQueue & 63;
        int i5 = 0;
        while (i3 < i2) {
            long[] jArr = this.state;
            jArr[i3] = jArr[i3] ^ Pack.littleEndianToLong(this.dataQueue, i5);
            i5 += 8;
            i3++;
        }
        if (i4 > 0) {
            long j = (1 << i4) - 1;
            long[] jArr2 = this.state;
            jArr2[i2] = (j & Pack.littleEndianToLong(this.dataQueue, i5)) ^ jArr2[i2];
        }
        long[] jArr3 = this.state;
        i4 = (this.rate - 1) >> 6;
        jArr3[i4] = jArr3[i4] ^ Long.MIN_VALUE;
        KeccakPermutation();
        KeccakExtract();
        this.bitsInQueue = this.rate;
        this.squeezing = true;
    }

    protected void absorb(byte[] bArr, int i, int i2) {
        if (this.bitsInQueue % 8 != 0) {
            throw new IllegalStateException("attempt to absorb with odd length queue");
        } else if (this.squeezing) {
            throw new IllegalStateException("attempt to absorb while squeezing");
        } else {
            int i3 = this.rate >> 3;
            int i4 = this.bitsInQueue >> 3;
            int i5 = 0;
            while (i5 < i2) {
                int i6;
                if (i4 == 0) {
                    i6 = i2 - i3;
                    if (i5 <= i6) {
                        do {
                            KeccakAbsorb(bArr, i + i5);
                            i5 += i3;
                        } while (i5 <= i6);
                    }
                }
                i6 = Math.min(i3 - i4, i2 - i5);
                System.arraycopy(bArr, i + i5, this.dataQueue, i4, i6);
                i4 += i6;
                i5 += i6;
                if (i4 == i3) {
                    KeccakAbsorb(this.dataQueue, 0);
                    i4 = 0;
                }
            }
            this.bitsInQueue = i4 << 3;
        }
    }

    protected void absorbBits(int i, int i2) {
        if (i2 < 1 || i2 > 7) {
            throw new IllegalArgumentException("'bits' must be in the range 1 to 7");
        } else if (this.bitsInQueue % 8 != 0) {
            throw new IllegalStateException("attempt to absorb with odd length queue");
        } else if (this.squeezing) {
            throw new IllegalStateException("attempt to absorb while squeezing");
        } else {
            this.dataQueue[this.bitsInQueue >> 3] = (byte) (i & ((1 << i2) - 1));
            this.bitsInQueue += i2;
        }
    }

    public int doFinal(byte[] bArr, int i) {
        squeeze(bArr, i, (long) this.fixedOutputLength);
        reset();
        return getDigestSize();
    }

    protected int doFinal(byte[] bArr, int i, byte b, int i2) {
        if (i2 > 0) {
            absorbBits(b, i2);
        }
        squeeze(bArr, i, (long) this.fixedOutputLength);
        reset();
        return getDigestSize();
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Keccak-");
        stringBuilder.append(this.fixedOutputLength);
        return stringBuilder.toString();
    }

    public int getByteLength() {
        return this.rate / 8;
    }

    public int getDigestSize() {
        return this.fixedOutputLength / 8;
    }

    public void reset() {
        init(this.fixedOutputLength);
    }

    protected void squeeze(byte[] bArr, int i, long j) {
        if (!this.squeezing) {
            padAndSwitchToSqueezingPhase();
        }
        long j2 = 0;
        if (j % 8 == 0) {
            while (j2 < j) {
                if (this.bitsInQueue == 0) {
                    KeccakPermutation();
                    KeccakExtract();
                    this.bitsInQueue = this.rate;
                }
                int min = (int) Math.min((long) this.bitsInQueue, j - j2);
                System.arraycopy(this.dataQueue, (this.rate - this.bitsInQueue) / 8, bArr, ((int) (j2 / 8)) + i, min / 8);
                this.bitsInQueue -= min;
                j2 += (long) min;
            }
            return;
        }
        throw new IllegalStateException("outputLength not a multiple of 8");
    }

    public void update(byte b) {
        absorb(new byte[]{b}, 0, 1);
    }

    public void update(byte[] bArr, int i, int i2) {
        absorb(bArr, i, i2);
    }
}
