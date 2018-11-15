package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.TweakableBlockCipherParameters;

public class ThreefishEngine implements BlockCipher {
    public static final int BLOCKSIZE_1024 = 1024;
    public static final int BLOCKSIZE_256 = 256;
    public static final int BLOCKSIZE_512 = 512;
    private static final long C_240 = 2004413935125273122L;
    private static final int MAX_ROUNDS = 80;
    private static int[] MOD17 = new int[MOD9.length];
    private static int[] MOD3 = new int[MOD9.length];
    private static int[] MOD5 = new int[MOD9.length];
    private static int[] MOD9 = new int[80];
    private static final int ROUNDS_1024 = 80;
    private static final int ROUNDS_256 = 72;
    private static final int ROUNDS_512 = 72;
    private static final int TWEAK_SIZE_BYTES = 16;
    private static final int TWEAK_SIZE_WORDS = 2;
    private int blocksizeBytes;
    private int blocksizeWords;
    private ThreefishCipher cipher;
    private long[] currentBlock;
    private boolean forEncryption;
    private long[] kw;
    private long[] t = new long[5];

    private static abstract class ThreefishCipher {
        protected final long[] kw;
        protected final long[] t;

        protected ThreefishCipher(long[] jArr, long[] jArr2) {
            this.kw = jArr;
            this.t = jArr2;
        }

        abstract void decryptBlock(long[] jArr, long[] jArr2);

        abstract void encryptBlock(long[] jArr, long[] jArr2);
    }

    private static final class Threefish1024Cipher extends ThreefishCipher {
        private static final int ROTATION_0_0 = 24;
        private static final int ROTATION_0_1 = 13;
        private static final int ROTATION_0_2 = 8;
        private static final int ROTATION_0_3 = 47;
        private static final int ROTATION_0_4 = 8;
        private static final int ROTATION_0_5 = 17;
        private static final int ROTATION_0_6 = 22;
        private static final int ROTATION_0_7 = 37;
        private static final int ROTATION_1_0 = 38;
        private static final int ROTATION_1_1 = 19;
        private static final int ROTATION_1_2 = 10;
        private static final int ROTATION_1_3 = 55;
        private static final int ROTATION_1_4 = 49;
        private static final int ROTATION_1_5 = 18;
        private static final int ROTATION_1_6 = 23;
        private static final int ROTATION_1_7 = 52;
        private static final int ROTATION_2_0 = 33;
        private static final int ROTATION_2_1 = 4;
        private static final int ROTATION_2_2 = 51;
        private static final int ROTATION_2_3 = 13;
        private static final int ROTATION_2_4 = 34;
        private static final int ROTATION_2_5 = 41;
        private static final int ROTATION_2_6 = 59;
        private static final int ROTATION_2_7 = 17;
        private static final int ROTATION_3_0 = 5;
        private static final int ROTATION_3_1 = 20;
        private static final int ROTATION_3_2 = 48;
        private static final int ROTATION_3_3 = 41;
        private static final int ROTATION_3_4 = 47;
        private static final int ROTATION_3_5 = 28;
        private static final int ROTATION_3_6 = 16;
        private static final int ROTATION_3_7 = 25;
        private static final int ROTATION_4_0 = 41;
        private static final int ROTATION_4_1 = 9;
        private static final int ROTATION_4_2 = 37;
        private static final int ROTATION_4_3 = 31;
        private static final int ROTATION_4_4 = 12;
        private static final int ROTATION_4_5 = 47;
        private static final int ROTATION_4_6 = 44;
        private static final int ROTATION_4_7 = 30;
        private static final int ROTATION_5_0 = 16;
        private static final int ROTATION_5_1 = 34;
        private static final int ROTATION_5_2 = 56;
        private static final int ROTATION_5_3 = 51;
        private static final int ROTATION_5_4 = 4;
        private static final int ROTATION_5_5 = 53;
        private static final int ROTATION_5_6 = 42;
        private static final int ROTATION_5_7 = 41;
        private static final int ROTATION_6_0 = 31;
        private static final int ROTATION_6_1 = 44;
        private static final int ROTATION_6_2 = 47;
        private static final int ROTATION_6_3 = 46;
        private static final int ROTATION_6_4 = 19;
        private static final int ROTATION_6_5 = 42;
        private static final int ROTATION_6_6 = 44;
        private static final int ROTATION_6_7 = 25;
        private static final int ROTATION_7_0 = 9;
        private static final int ROTATION_7_1 = 48;
        private static final int ROTATION_7_2 = 35;
        private static final int ROTATION_7_3 = 52;
        private static final int ROTATION_7_4 = 23;
        private static final int ROTATION_7_5 = 31;
        private static final int ROTATION_7_6 = 37;
        private static final int ROTATION_7_7 = 20;

        public Threefish1024Cipher(long[] jArr, long[] jArr2) {
            super(jArr, jArr2);
        }

        void decryptBlock(long[] jArr, long[] jArr2) {
            long[] jArr3 = this.kw;
            long[] jArr4 = this.t;
            int[] access$300 = ThreefishEngine.MOD17;
            int[] access$100 = ThreefishEngine.MOD3;
            if (jArr3.length != 33) {
                throw new IllegalArgumentException();
            } else if (jArr4.length == 5) {
                long[] jArr5;
                long[] jArr6;
                long j = jArr[0];
                int i = 1;
                long j2 = jArr[1];
                long j3 = jArr[2];
                long j4 = jArr[3];
                long j5 = jArr[4];
                long j6 = jArr[5];
                long j7 = jArr[6];
                long j8 = jArr[7];
                long j9 = jArr[8];
                long j10 = jArr[9];
                long j11 = jArr[10];
                long j12 = jArr[11];
                long j13 = jArr[12];
                long j14 = jArr[13];
                long j15 = jArr[14];
                long j16 = jArr[15];
                int i2 = 19;
                while (i2 >= i) {
                    int i3 = access$300[i2];
                    int i4 = access$100[i2];
                    int i5 = i3 + 1;
                    j -= jArr3[i5];
                    int i6 = i3 + 2;
                    int i7 = i3 + 3;
                    int i8 = i3 + 4;
                    long j17 = j2 - jArr3[i6];
                    long j18 = j4 - jArr3[i8];
                    int i9 = i3 + 5;
                    long j19 = j3 - jArr3[i7];
                    long j20 = j5 - jArr3[i9];
                    int i10 = i3 + 6;
                    int i11 = i3 + 7;
                    int[] iArr = access$300;
                    int[] iArr2 = access$100;
                    long j21 = j7 - jArr3[i11];
                    int i12 = i3 + 8;
                    long j22 = j18;
                    int i13 = i3 + 9;
                    long j23 = j8 - jArr3[i12];
                    int i14 = i3 + 10;
                    long j24 = j6 - jArr3[i10];
                    int i15 = i3 + 11;
                    long j25 = j9 - jArr3[i13];
                    int i16 = i3 + 12;
                    long j26 = j11 - jArr3[i15];
                    j18 = j12 - jArr3[i16];
                    int i17 = i3 + 13;
                    long j27 = j10 - jArr3[i14];
                    int i18 = i3 + 14;
                    int i19 = i4 + 1;
                    long j28 = j13 - jArr3[i17];
                    long j29 = j14 - (jArr3[i18] + jArr4[i19]);
                    int i20 = i3 + 15;
                    int i21 = i9;
                    long j30 = j20;
                    long j31 = j15 - (jArr3[i20] + jArr4[i4 + 2]);
                    int i22 = i10;
                    j3 = (long) i2;
                    int i23 = i2;
                    long j32 = j3;
                    long xorRotr = ThreefishEngine.xorRotr(j16 - ((jArr3[i3 + 16] + j3) + 1), 9, j);
                    j -= xorRotr;
                    jArr5 = jArr4;
                    long j33 = xorRotr;
                    long j34 = j19;
                    j18 = ThreefishEngine.xorRotr(j18, 48, j34);
                    j3 = j34 - j18;
                    j34 = ThreefishEngine.xorRotr(j29, 35, j21);
                    j21 -= j34;
                    long j35 = j18;
                    j29 = j30;
                    j18 = ThreefishEngine.xorRotr(j27, 52, j29);
                    j29 -= j18;
                    long j36 = j18;
                    j18 = ThreefishEngine.xorRotr(j17, 23, j31);
                    long j37 = j34;
                    long j38 = j31 - j18;
                    j31 = j25;
                    j34 = ThreefishEngine.xorRotr(j24, 31, j31);
                    jArr6 = jArr3;
                    long j39 = j21;
                    long j40 = j31 - j34;
                    long j41 = j26;
                    j31 = ThreefishEngine.xorRotr(j22, 37, j41);
                    long j42 = j41 - j31;
                    long j43 = j18;
                    j18 = j28;
                    long xorRotr2 = ThreefishEngine.xorRotr(j23, 20, j18);
                    j21 = j18 - xorRotr2;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 31, j);
                    j -= xorRotr2;
                    j34 = ThreefishEngine.xorRotr(j34, 44, j3);
                    j3 -= j34;
                    j18 = ThreefishEngine.xorRotr(j31, 47, j29);
                    j29 -= j18;
                    long j44 = j34;
                    long j45 = j18;
                    j34 = j39;
                    j18 = ThreefishEngine.xorRotr(j43, 46, j34);
                    j34 -= j18;
                    long j46 = j18;
                    j18 = ThreefishEngine.xorRotr(j33, 19, j21);
                    long j47 = xorRotr2;
                    long j48 = j21 - j18;
                    xorRotr2 = j38;
                    j21 = ThreefishEngine.xorRotr(j37, 42, xorRotr2);
                    long j49 = j29;
                    long j50 = xorRotr2 - j21;
                    j29 = j40;
                    xorRotr2 = ThreefishEngine.xorRotr(j35, 44, j29);
                    long j51 = j18;
                    long j52 = j29 - xorRotr2;
                    j18 = j42;
                    j29 = ThreefishEngine.xorRotr(j36, 25, j18);
                    j18 -= j29;
                    j29 = ThreefishEngine.xorRotr(j29, 16, j);
                    j -= j29;
                    j21 = ThreefishEngine.xorRotr(j21, 34, j3);
                    j3 -= j21;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 56, j34);
                    j34 -= xorRotr2;
                    long j53 = xorRotr2;
                    long j54 = j21;
                    j21 = j49;
                    xorRotr2 = ThreefishEngine.xorRotr(j51, 51, j21);
                    j21 -= xorRotr2;
                    long j55 = xorRotr2;
                    j31 = ThreefishEngine.xorRotr(j47, 4, j18);
                    long j56 = j29;
                    long j57 = j18 - j31;
                    j29 = j48;
                    xorRotr2 = ThreefishEngine.xorRotr(j45, 53, j29);
                    long j58 = j34;
                    long j59 = j29 - xorRotr2;
                    j34 = j50;
                    j29 = ThreefishEngine.xorRotr(j44, 42, j34);
                    long j60 = j34 - j29;
                    long j61 = j31;
                    j2 = j52;
                    j34 = ThreefishEngine.xorRotr(j46, 41, j2);
                    j18 = j2 - j34;
                    j34 = ThreefishEngine.xorRotr(j34, 41, j);
                    j -= j34;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 9, j3);
                    j3 -= xorRotr2;
                    j29 = ThreefishEngine.xorRotr(j29, 37, j21);
                    long j62 = j21 - j29;
                    long j63 = j29;
                    j29 = j58;
                    j21 = ThreefishEngine.xorRotr(j61, 31, j29);
                    j29 -= j21;
                    long j64 = j21;
                    j31 = ThreefishEngine.xorRotr(j56, 12, j18);
                    long j65 = j18 - j31;
                    long j66 = j31;
                    j18 = j57;
                    j21 = ThreefishEngine.xorRotr(j53, 47, j18);
                    long j67 = j21;
                    long j68 = j18 - j21;
                    j18 = j59;
                    j21 = ThreefishEngine.xorRotr(j54, 44, j18);
                    long j69 = j21;
                    long j70 = j18 - j21;
                    j18 = j60;
                    j21 = ThreefishEngine.xorRotr(j55, 30, j18);
                    j -= jArr6[i3];
                    j3 -= jArr6[i6];
                    j31 = j62 - jArr6[i8];
                    long j71 = xorRotr2 - jArr6[i7];
                    j29 -= jArr6[i22];
                    long j72 = j63 - jArr6[i21];
                    long j73 = j64 - jArr6[i11];
                    long j74 = j65 - jArr6[i12];
                    long j75 = j34 - jArr6[i5];
                    long j76 = j68 - jArr6[i14];
                    j34 = j67 - jArr6[i15];
                    long j77 = j66 - jArr6[i13];
                    long j78 = j70 - jArr6[i16];
                    xorRotr2 = j69 - (jArr6[i17] + jArr5[i4]);
                    long j79 = (j18 - j21) - (jArr6[i18] + jArr5[i19]);
                    j21 = ThreefishEngine.xorRotr(j21 - (jArr6[i20] + j32), 5, j);
                    j -= j21;
                    j34 = ThreefishEngine.xorRotr(j34, 20, j3);
                    j3 -= j34;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 48, j29);
                    j29 -= xorRotr2;
                    long j80 = j34;
                    j34 = ThreefishEngine.xorRotr(j77, 41, j31);
                    j31 -= j34;
                    long j81 = j34;
                    long j82 = xorRotr2;
                    xorRotr2 = j79;
                    j34 = ThreefishEngine.xorRotr(j75, 47, xorRotr2);
                    long j83 = j21;
                    long j84 = xorRotr2 - j34;
                    j41 = j74;
                    j18 = ThreefishEngine.xorRotr(j72, 28, j41);
                    long j85 = j34;
                    long j86 = j41 - j18;
                    j34 = j76;
                    xorRotr2 = ThreefishEngine.xorRotr(j71, 16, j34);
                    long j87 = j34 - xorRotr2;
                    long j88 = j29;
                    long j89 = j78;
                    j34 = ThreefishEngine.xorRotr(j73, 25, j89);
                    j21 = j89 - j34;
                    j34 = ThreefishEngine.xorRotr(j34, 33, j);
                    j -= j34;
                    j18 = ThreefishEngine.xorRotr(j18, 4, j3);
                    j3 -= j18;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 51, j31);
                    j31 -= xorRotr2;
                    long j90 = xorRotr2;
                    long j91 = j18;
                    j29 = j88;
                    j18 = ThreefishEngine.xorRotr(j85, 13, j29);
                    j29 -= j18;
                    long j92 = j18;
                    xorRotr2 = ThreefishEngine.xorRotr(j83, 34, j21);
                    long j93 = j34;
                    long j94 = j21 - xorRotr2;
                    j34 = j84;
                    j21 = ThreefishEngine.xorRotr(j82, 41, j34);
                    long j95 = xorRotr2;
                    long j96 = j34 - j21;
                    xorRotr = j86;
                    j18 = ThreefishEngine.xorRotr(j80, 59, xorRotr);
                    long j97 = xorRotr - j18;
                    long j98 = j31;
                    j31 = j87;
                    j34 = ThreefishEngine.xorRotr(j81, 17, j31);
                    xorRotr2 = j31 - j34;
                    j34 = ThreefishEngine.xorRotr(j34, 38, j);
                    j -= j34;
                    j21 = ThreefishEngine.xorRotr(j21, 19, j3);
                    j3 -= j21;
                    j18 = ThreefishEngine.xorRotr(j18, 10, j29);
                    j29 -= j18;
                    long j99 = j21;
                    long j100 = j18;
                    j21 = j98;
                    j18 = ThreefishEngine.xorRotr(j95, 55, j21);
                    j31 = j21 - j18;
                    long j101 = j18;
                    j21 = ThreefishEngine.xorRotr(j93, 49, xorRotr2);
                    long j102 = j34;
                    long j103 = xorRotr2 - j21;
                    j34 = j94;
                    xorRotr2 = ThreefishEngine.xorRotr(j90, 18, j34);
                    long j104 = j34 - xorRotr2;
                    long j105 = j21;
                    j21 = j96;
                    j34 = ThreefishEngine.xorRotr(j91, 23, j21);
                    long j106 = j29;
                    long j107 = j21 - j34;
                    j89 = j97;
                    j18 = ThreefishEngine.xorRotr(j92, 52, j89);
                    j21 = j89 - j18;
                    j29 = ThreefishEngine.xorRotr(j18, 24, j);
                    j -= j29;
                    j4 = ThreefishEngine.xorRotr(xorRotr2, 13, j3);
                    j3 -= j4;
                    j6 = ThreefishEngine.xorRotr(j34, 8, j31);
                    j5 = j31 - j6;
                    j18 = j106;
                    j8 = ThreefishEngine.xorRotr(j105, 47, j18);
                    j7 = j18 - j8;
                    j10 = ThreefishEngine.xorRotr(j102, 8, j21);
                    j9 = j21 - j10;
                    j41 = j103;
                    j12 = ThreefishEngine.xorRotr(j100, 17, j41);
                    j11 = j41 - j12;
                    j21 = j104;
                    j14 = ThreefishEngine.xorRotr(j99, 22, j21);
                    j13 = j21 - j14;
                    j18 = j107;
                    j16 = ThreefishEngine.xorRotr(j101, 37, j18);
                    j15 = j18 - j16;
                    i2 = i23 - 2;
                    j2 = j29;
                    access$300 = iArr;
                    access$100 = iArr2;
                    jArr4 = jArr5;
                    jArr3 = jArr6;
                    i = 1;
                }
                jArr5 = jArr4;
                jArr6 = jArr3;
                j2 -= jArr6[1];
                j3 -= jArr6[2];
                j4 -= jArr6[3];
                j5 -= jArr6[4];
                j6 -= jArr6[5];
                j7 -= jArr6[6];
                j8 -= jArr6[7];
                j9 -= jArr6[8];
                j10 -= jArr6[9];
                j11 -= jArr6[10];
                j12 -= jArr6[11];
                j13 -= jArr6[12];
                j14 -= jArr6[13] + jArr5[0];
                j15 -= jArr6[14] + jArr5[1];
                j16 -= jArr6[15];
                jArr2[0] = j - jArr6[0];
                jArr2[1] = j2;
                jArr2[2] = j3;
                jArr2[3] = j4;
                jArr2[4] = j5;
                jArr2[5] = j6;
                jArr2[6] = j7;
                jArr2[7] = j8;
                jArr2[8] = j9;
                jArr2[9] = j10;
                jArr2[10] = j11;
                jArr2[11] = j12;
                jArr2[12] = j13;
                jArr2[13] = j14;
                jArr2[14] = j15;
                jArr2[15] = j16;
            } else {
                throw new IllegalArgumentException();
            }
        }

        void encryptBlock(long[] jArr, long[] jArr2) {
            long[] jArr3 = this.kw;
            long[] jArr4 = this.t;
            int[] access$300 = ThreefishEngine.MOD17;
            int[] access$100 = ThreefishEngine.MOD3;
            if (jArr3.length != 33) {
                throw new IllegalArgumentException();
            } else if (jArr4.length == 5) {
                long j;
                long j2;
                long j3;
                long j4;
                int i = 0;
                long j5 = jArr[0];
                long j6 = jArr[1];
                long j7 = jArr[2];
                long j8 = jArr[3];
                long j9 = jArr[4];
                long j10 = jArr[5];
                long j11 = jArr[6];
                long j12 = jArr[7];
                long j13 = jArr[8];
                long j14 = jArr[9];
                long j15 = jArr[10];
                long j16 = jArr[11];
                long j17 = jArr[12];
                int i2 = 13;
                long j18 = jArr[13];
                long j19 = jArr[14];
                j5 += jArr3[0];
                j6 += jArr3[1];
                j9 += jArr3[4];
                j11 += jArr3[6];
                j13 += jArr3[8];
                j15 += jArr3[10];
                j17 += jArr3[12];
                j19 += jArr3[14] + jArr4[1];
                int i3 = 1;
                long j20 = j10 + jArr3[5];
                long j21 = j12 + jArr3[7];
                long j22 = j14 + jArr3[9];
                long j23 = j16 + jArr3[11];
                long j24 = j18 + (jArr3[13] + jArr4[0]);
                long j25 = jArr[15] + jArr3[15];
                long j26 = j7 + jArr3[2];
                j7 = j8 + jArr3[3];
                j8 = j26;
                while (i3 < 20) {
                    int i4 = access$300[i3];
                    int i5 = access$100[i3];
                    j5 += j6;
                    j6 = ThreefishEngine.rotlXor(j6, 24, j5);
                    int[] iArr = access$300;
                    int[] iArr2 = access$100;
                    long j27 = j8 + j7;
                    long j28 = j5;
                    long rotlXor = ThreefishEngine.rotlXor(j7, i2, j27);
                    j = j20;
                    long j29 = j9 + j;
                    j = ThreefishEngine.rotlXor(j, 8, j29);
                    int i6 = i3;
                    long j30 = j29;
                    j2 = j21;
                    j3 = j11 + j2;
                    j2 = ThreefishEngine.rotlXor(j2, 47, j3);
                    int i7 = i4;
                    long j31 = j6;
                    j5 = j22;
                    j4 = j13 + j5;
                    j5 = ThreefishEngine.rotlXor(j5, 8, j4);
                    long j32 = j4;
                    long j33 = j;
                    j4 = j23;
                    j = j15 + j4;
                    j4 = ThreefishEngine.rotlXor(j4, 17, j);
                    long j34 = j2;
                    long j35 = j;
                    j2 = j24;
                    j = j17 + j2;
                    j2 = ThreefishEngine.rotlXor(j2, 22, j);
                    long[] jArr5 = jArr4;
                    long j36 = j3;
                    long j37 = j;
                    j = j25;
                    long j38 = j19 + j;
                    long rotlXor2 = ThreefishEngine.rotlXor(j, 37, j38);
                    j7 = j28 + j5;
                    j5 = ThreefishEngine.rotlXor(j5, 38, j7);
                    j27 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 19, j27);
                    long[] jArr6 = jArr3;
                    long j39 = j36 + j4;
                    long j40 = j5;
                    long rotlXor3 = ThreefishEngine.rotlXor(j4, 10, j39);
                    long j41 = j30 + rotlXor2;
                    long j42 = j2;
                    j2 = j35 + j34;
                    long rotlXor4 = ThreefishEngine.rotlXor(rotlXor2, 55, j41);
                    j6 = ThreefishEngine.rotlXor(j34, 49, j2);
                    long j43 = j2;
                    j2 = j37 + rotlXor;
                    long j44 = j39;
                    j39 = ThreefishEngine.rotlXor(rotlXor, 18, j2);
                    j38 += j33;
                    long j45 = j2;
                    j2 = ThreefishEngine.rotlXor(j33, 23, j38);
                    long j46 = j38;
                    j38 = j32 + j31;
                    long j47 = j39;
                    j39 = ThreefishEngine.rotlXor(j31, 52, j38);
                    j7 += j6;
                    j6 = ThreefishEngine.rotlXor(j6, 33, j7);
                    j27 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 4, j27);
                    j41 += j47;
                    long j48 = j6;
                    long rotlXor5 = ThreefishEngine.rotlXor(j47, 51, j41);
                    j6 = j44 + j39;
                    long j49 = j2;
                    j2 = j45 + rotlXor4;
                    long rotlXor6 = ThreefishEngine.rotlXor(j39, 13, j6);
                    j39 = ThreefishEngine.rotlXor(rotlXor4, 34, j2);
                    long j50 = j2;
                    j2 = j46 + j42;
                    long j51 = j41;
                    j5 = ThreefishEngine.rotlXor(j42, 41, j2);
                    j38 += rotlXor3;
                    long j52 = j2;
                    j2 = ThreefishEngine.rotlXor(rotlXor3, 59, j38);
                    long j53 = j38;
                    j38 = j43 + j40;
                    long j54 = j5;
                    j5 = ThreefishEngine.rotlXor(j40, 17, j38);
                    j7 += j39;
                    j39 = ThreefishEngine.rotlXor(j39, 5, j7);
                    j27 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 20, j27);
                    j6 += j54;
                    long j55 = j39;
                    long rotlXor7 = ThreefishEngine.rotlXor(j54, 48, j6);
                    j39 = j51 + j5;
                    long j56 = j2;
                    j2 = j52 + rotlXor6;
                    long rotlXor8 = ThreefishEngine.rotlXor(j5, 41, j39);
                    j5 = ThreefishEngine.rotlXor(rotlXor6, 47, j2);
                    long j57 = j2;
                    j2 = j53 + j49;
                    long j58 = j6;
                    j4 = ThreefishEngine.rotlXor(j49, 28, j2);
                    j38 += rotlXor5;
                    long j59 = j2;
                    j2 = ThreefishEngine.rotlXor(rotlXor5, 16, j38);
                    long j60 = j38;
                    j38 = j50 + j48;
                    long j61 = j4;
                    int i8 = i7 + 1;
                    j5 += jArr6[i8];
                    int i9 = i7 + 2;
                    j27 += jArr6[i9];
                    int i10 = i7 + 3;
                    j2 += jArr6[i10];
                    int i11 = i7 + 4;
                    j39 += jArr6[i11];
                    int i12 = i7 + 5;
                    int i13 = i8;
                    int i14 = i9;
                    rotlXor2 = j61 + jArr6[i12];
                    int i15 = i7 + 6;
                    j11 = j58 + jArr6[i15];
                    int i16 = i7 + 7;
                    int i17 = i7 + 8;
                    j13 = j59 + jArr6[i17];
                    int i18 = i7 + 9;
                    long rotlXor9 = ThreefishEngine.rotlXor(j48, 25, j38) + jArr6[i16];
                    int i19 = i7 + 10;
                    j15 = j60 + jArr6[i19];
                    int i20 = i7 + 11;
                    long j62 = rotlXor8 + jArr6[i18];
                    int i21 = i7 + 12;
                    int i22 = i7 + 13;
                    long j63 = j38 + jArr6[i21];
                    int i23 = i7 + 14;
                    int i24 = i5 + 1;
                    j18 = j57 + (jArr6[i23] + jArr5[i24]);
                    int i25 = i7 + 15;
                    long j64 = rotlXor7 + (jArr6[i22] + jArr5[i5]);
                    long j65 = j56 + jArr6[i20];
                    int i26 = i6;
                    j4 = (long) i26;
                    int i27 = i26;
                    j38 = j55 + (jArr6[i25] + j4);
                    long j66 = j4;
                    j7 = (j7 + jArr6[i7]) + j5;
                    j5 = ThreefishEngine.rotlXor(j5, 41, j7);
                    j27 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 9, j27);
                    j39 += rotlXor2;
                    long j67 = j5;
                    j5 = j11 + rotlXor9;
                    long rotlXor10 = ThreefishEngine.rotlXor(rotlXor2, 37, j39);
                    rotlXor2 = j13 + j62;
                    long j68 = j2;
                    long rotlXor11 = ThreefishEngine.rotlXor(rotlXor9, 31, j5);
                    j2 = ThreefishEngine.rotlXor(j62, 12, rotlXor2);
                    long j69 = rotlXor2;
                    j6 = j15 + j65;
                    long j70 = j39;
                    j39 = ThreefishEngine.rotlXor(j65, 47, j6);
                    long j71 = j6;
                    j6 = j63 + j64;
                    long j72 = j39;
                    j39 = ThreefishEngine.rotlXor(j64, 44, j6);
                    long j73 = j6;
                    j6 = j18 + j38;
                    j38 = ThreefishEngine.rotlXor(j38, 30, j6);
                    j7 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 16, j7);
                    j27 += j39;
                    j39 = ThreefishEngine.rotlXor(j39, 34, j27);
                    j5 += j72;
                    long j74 = j2;
                    long rotlXor12 = ThreefishEngine.rotlXor(j72, 56, j5);
                    j2 = j70 + j38;
                    long j75 = j39;
                    j39 = j71 + rotlXor11;
                    long rotlXor13 = ThreefishEngine.rotlXor(j38, 51, j2);
                    j38 = ThreefishEngine.rotlXor(rotlXor11, 4, j39);
                    long j76 = j39;
                    j39 = j73 + j68;
                    long j77 = j5;
                    j5 = ThreefishEngine.rotlXor(j68, 53, j39);
                    j6 += rotlXor10;
                    long j78 = j39;
                    j39 = ThreefishEngine.rotlXor(rotlXor10, 42, j6);
                    long j79 = j6;
                    j6 = j69 + j67;
                    long j80 = j2;
                    j2 = ThreefishEngine.rotlXor(j67, 41, j6);
                    j7 += j38;
                    j38 = ThreefishEngine.rotlXor(j38, 31, j7);
                    j27 += j39;
                    j39 = ThreefishEngine.rotlXor(j39, 44, j27);
                    long j81 = j38;
                    j38 = j80 + j5;
                    long rotlXor14 = ThreefishEngine.rotlXor(j5, 47, j38);
                    j5 = j77 + j2;
                    long j82 = j39;
                    j39 = j78 + rotlXor13;
                    long rotlXor15 = ThreefishEngine.rotlXor(j2, 46, j5);
                    j2 = ThreefishEngine.rotlXor(rotlXor13, 19, j39);
                    long j83 = j39;
                    j39 = j79 + j75;
                    long j84 = j38;
                    j38 = ThreefishEngine.rotlXor(j75, 42, j39);
                    j6 += rotlXor12;
                    long j85 = j39;
                    j39 = ThreefishEngine.rotlXor(rotlXor12, 44, j6);
                    long j86 = j6;
                    j6 = j76 + j74;
                    long j87 = j38;
                    j38 = ThreefishEngine.rotlXor(j74, 25, j6);
                    j7 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 9, j7);
                    j27 += j39;
                    j39 = ThreefishEngine.rotlXor(j39, 48, j27);
                    j5 += j87;
                    long j88 = j2;
                    long rotlXor16 = ThreefishEngine.rotlXor(j87, 35, j5);
                    j2 = j84 + j38;
                    long j89 = j39;
                    j39 = j85 + rotlXor15;
                    long rotlXor17 = ThreefishEngine.rotlXor(j38, 52, j2);
                    j38 = ThreefishEngine.rotlXor(rotlXor15, 23, j39);
                    long j90 = j39;
                    j39 = j86 + j82;
                    long j91 = j5;
                    j5 = ThreefishEngine.rotlXor(j82, 31, j39);
                    j6 += rotlXor14;
                    long j92 = j39;
                    j39 = ThreefishEngine.rotlXor(rotlXor14, 37, j6);
                    long j93 = j6;
                    j6 = j83 + j81;
                    long j94 = j5;
                    j20 = j94 + jArr6[i15];
                    j11 = j91 + jArr6[i16];
                    j21 = ThreefishEngine.rotlXor(j81, 20, j6) + jArr6[i17];
                    j13 = j92 + jArr6[i18];
                    j22 = rotlXor17 + jArr6[i19];
                    j15 = j93 + jArr6[i20];
                    j23 = j89 + jArr6[i21];
                    j5 = jArr6[i22] + j6;
                    j24 = rotlXor16 + (jArr6[i23] + jArr5[i24]);
                    j19 = j90 + (jArr6[i25] + jArr5[i5 + 2]);
                    j25 = j88 + ((jArr6[i7 + 16] + j66) + 1);
                    j6 = j38 + jArr6[i14];
                    j8 = j27 + jArr6[i10];
                    j9 = j2 + jArr6[i12];
                    j17 = j5;
                    i3 = i27 + 2;
                    j5 = j7 + jArr6[i13];
                    access$300 = iArr;
                    access$100 = iArr2;
                    jArr4 = jArr5;
                    i = 0;
                    i2 = 13;
                    j7 = j39 + jArr6[i11];
                    jArr3 = jArr6;
                }
                int i28 = i;
                j3 = j6;
                long j95 = j7;
                j10 = j20;
                j4 = j23;
                j2 = j24;
                j = j25;
                jArr2[i28] = j5;
                jArr2[1] = j3;
                jArr2[2] = j8;
                jArr2[3] = j95;
                jArr2[4] = j9;
                jArr2[5] = j10;
                jArr2[6] = j11;
                jArr2[7] = j21;
                jArr2[8] = j13;
                jArr2[9] = j22;
                jArr2[10] = j15;
                jArr2[11] = j4;
                jArr2[12] = j17;
                jArr2[13] = j2;
                jArr2[14] = j19;
                jArr2[15] = j;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    private static final class Threefish256Cipher extends ThreefishCipher {
        private static final int ROTATION_0_0 = 14;
        private static final int ROTATION_0_1 = 16;
        private static final int ROTATION_1_0 = 52;
        private static final int ROTATION_1_1 = 57;
        private static final int ROTATION_2_0 = 23;
        private static final int ROTATION_2_1 = 40;
        private static final int ROTATION_3_0 = 5;
        private static final int ROTATION_3_1 = 37;
        private static final int ROTATION_4_0 = 25;
        private static final int ROTATION_4_1 = 33;
        private static final int ROTATION_5_0 = 46;
        private static final int ROTATION_5_1 = 12;
        private static final int ROTATION_6_0 = 58;
        private static final int ROTATION_6_1 = 22;
        private static final int ROTATION_7_0 = 32;
        private static final int ROTATION_7_1 = 32;

        public Threefish256Cipher(long[] jArr, long[] jArr2) {
            super(jArr, jArr2);
        }

        void decryptBlock(long[] jArr, long[] jArr2) {
            long[] jArr3 = this.kw;
            long[] jArr4 = this.t;
            int[] access$000 = ThreefishEngine.MOD5;
            int[] access$100 = ThreefishEngine.MOD3;
            if (jArr3.length != 9) {
                throw new IllegalArgumentException();
            } else if (jArr4.length == 5) {
                int i = 0;
                long j = jArr[0];
                int i2 = 1;
                long j2 = jArr[1];
                long j3 = jArr[2];
                long j4 = jArr[3];
                int i3 = 17;
                while (i3 >= i2) {
                    int i4 = access$000[i3];
                    int i5 = access$100[i3];
                    int i6 = i4 + 1;
                    j -= jArr3[i6];
                    int i7 = i4 + 2;
                    int i8 = i5 + 1;
                    int i9 = i4 + 3;
                    j3 -= jArr3[i9] + jArr4[i5 + 2];
                    long j5 = (long) i3;
                    long j6 = j2 - (jArr3[i7] + jArr4[i8]);
                    long xorRotr = ThreefishEngine.xorRotr(j4 - ((jArr3[i4 + 4] + j5) + 1), 32, j);
                    j -= xorRotr;
                    int[] iArr = access$000;
                    int[] iArr2 = access$100;
                    long xorRotr2 = ThreefishEngine.xorRotr(j6, 32, j3);
                    j3 -= xorRotr2;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 58, j);
                    j -= xorRotr2;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 22, j3);
                    j3 -= xorRotr;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 46, j);
                    j -= xorRotr;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 12, j3);
                    j3 -= xorRotr2;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 25, j);
                    j -= xorRotr2;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 33, j3);
                    j -= jArr3[i4];
                    xorRotr2 -= jArr3[i6] + jArr4[i5];
                    j3 = (j3 - xorRotr) - (jArr3[i7] + jArr4[i8]);
                    xorRotr = ThreefishEngine.xorRotr(xorRotr - (jArr3[i9] + j5), 5, j);
                    j -= xorRotr;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 37, j3);
                    j3 -= xorRotr2;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 23, j);
                    j -= xorRotr2;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 40, j3);
                    j3 -= xorRotr;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 52, j);
                    j -= xorRotr;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 57, j3);
                    j3 -= xorRotr2;
                    xorRotr2 = ThreefishEngine.xorRotr(xorRotr2, 14, j);
                    j -= xorRotr2;
                    j4 = ThreefishEngine.xorRotr(xorRotr, 16, j3);
                    j3 -= j4;
                    i3 -= 2;
                    j2 = xorRotr2;
                    int i10 = 5;
                    access$000 = iArr;
                    access$100 = iArr2;
                    i = 0;
                    i2 = 1;
                }
                i3 = i;
                j2 -= jArr3[1] + jArr4[i3];
                j3 -= jArr3[2] + jArr4[1];
                j4 -= jArr3[3];
                jArr2[i3] = j - jArr3[i3];
                jArr2[1] = j2;
                jArr2[2] = j3;
                jArr2[3] = j4;
            } else {
                throw new IllegalArgumentException();
            }
        }

        void encryptBlock(long[] jArr, long[] jArr2) {
            long[] jArr3 = this.kw;
            long[] jArr4 = this.t;
            int[] access$000 = ThreefishEngine.MOD5;
            int[] access$100 = ThreefishEngine.MOD3;
            if (jArr3.length != 9) {
                throw new IllegalArgumentException();
            } else if (jArr4.length == 5) {
                int i = 0;
                long j = jArr[0];
                long j2 = jArr[1];
                long j3 = jArr[2];
                j += jArr3[0];
                j2 += jArr3[1] + jArr4[0];
                int i2 = 1;
                long j4 = j3 + (jArr3[2] + jArr4[1]);
                j3 = jArr[3] + jArr3[3];
                long j5 = j4;
                while (i2 < 18) {
                    int i3 = access$000[i2];
                    int i4 = access$100[i2];
                    j += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 14, j);
                    long j6 = j;
                    long j7 = j5 + j3;
                    j3 = ThreefishEngine.rotlXor(j3, 16, j7);
                    int[] iArr = access$000;
                    int[] iArr2 = access$100;
                    long j8 = j6 + j3;
                    j3 = ThreefishEngine.rotlXor(j3, 52, j8);
                    j7 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 57, j7);
                    j8 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 23, j8);
                    j7 += j3;
                    j3 = ThreefishEngine.rotlXor(j3, 40, j7);
                    j8 += j3;
                    j3 = ThreefishEngine.rotlXor(j3, 5, j8);
                    j7 += j2;
                    int i5 = i3 + 1;
                    j2 = ThreefishEngine.rotlXor(j2, 37, j7) + (jArr3[i5] + jArr4[i4]);
                    int i6 = i3 + 2;
                    int i7 = i4 + 1;
                    int i8 = i3 + 3;
                    long j9 = j7 + (jArr3[i6] + jArr4[i7]);
                    long j10 = (long) i2;
                    j3 += jArr3[i8] + j10;
                    j8 = (j8 + jArr3[i3]) + j2;
                    j2 = ThreefishEngine.rotlXor(j2, 25, j8);
                    int i9 = i2;
                    long j11 = j9 + j3;
                    j3 = ThreefishEngine.rotlXor(j3, 33, j11);
                    j8 += j3;
                    j3 = ThreefishEngine.rotlXor(j3, 46, j8);
                    j11 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 12, j11);
                    j8 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 58, j8);
                    j11 += j3;
                    j3 = ThreefishEngine.rotlXor(j3, 22, j11);
                    j8 += j3;
                    j3 = ThreefishEngine.rotlXor(j3, 32, j8);
                    j11 += j2;
                    j = jArr3[i5] + j8;
                    j2 = ThreefishEngine.rotlXor(j2, 32, j11) + (jArr3[i6] + jArr4[i7]);
                    j5 = j11 + (jArr3[i8] + jArr4[i4 + 2]);
                    j3 += (jArr3[i3 + 4] + j10) + 1;
                    i2 = i9 + 2;
                    access$000 = iArr;
                    access$100 = iArr2;
                    i = 0;
                }
                jArr2[i] = j;
                jArr2[1] = j2;
                jArr2[2] = j5;
                jArr2[3] = j3;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    private static final class Threefish512Cipher extends ThreefishCipher {
        private static final int ROTATION_0_0 = 46;
        private static final int ROTATION_0_1 = 36;
        private static final int ROTATION_0_2 = 19;
        private static final int ROTATION_0_3 = 37;
        private static final int ROTATION_1_0 = 33;
        private static final int ROTATION_1_1 = 27;
        private static final int ROTATION_1_2 = 14;
        private static final int ROTATION_1_3 = 42;
        private static final int ROTATION_2_0 = 17;
        private static final int ROTATION_2_1 = 49;
        private static final int ROTATION_2_2 = 36;
        private static final int ROTATION_2_3 = 39;
        private static final int ROTATION_3_0 = 44;
        private static final int ROTATION_3_1 = 9;
        private static final int ROTATION_3_2 = 54;
        private static final int ROTATION_3_3 = 56;
        private static final int ROTATION_4_0 = 39;
        private static final int ROTATION_4_1 = 30;
        private static final int ROTATION_4_2 = 34;
        private static final int ROTATION_4_3 = 24;
        private static final int ROTATION_5_0 = 13;
        private static final int ROTATION_5_1 = 50;
        private static final int ROTATION_5_2 = 10;
        private static final int ROTATION_5_3 = 17;
        private static final int ROTATION_6_0 = 25;
        private static final int ROTATION_6_1 = 29;
        private static final int ROTATION_6_2 = 39;
        private static final int ROTATION_6_3 = 43;
        private static final int ROTATION_7_0 = 8;
        private static final int ROTATION_7_1 = 35;
        private static final int ROTATION_7_2 = 56;
        private static final int ROTATION_7_3 = 22;

        protected Threefish512Cipher(long[] jArr, long[] jArr2) {
            super(jArr, jArr2);
        }

        public void decryptBlock(long[] jArr, long[] jArr2) {
            long[] jArr3 = this.kw;
            long[] jArr4 = this.t;
            int[] access$200 = ThreefishEngine.MOD9;
            int[] access$100 = ThreefishEngine.MOD3;
            if (jArr3.length != 17) {
                throw new IllegalArgumentException();
            } else if (jArr4.length == 5) {
                long[] jArr5;
                long[] jArr6;
                int i = 0;
                long j = jArr[0];
                int i2 = 1;
                long j2 = jArr[1];
                long j3 = jArr[2];
                long j4 = jArr[3];
                long j5 = jArr[4];
                long j6 = jArr[5];
                long j7 = jArr[6];
                long j8 = jArr[7];
                int i3 = 17;
                while (i3 >= i2) {
                    int i4 = access$200[i3];
                    int i5 = access$100[i3];
                    int i6 = i4 + 1;
                    int i7 = i4 + 2;
                    int i8 = i4 + 3;
                    long j9 = j3 - jArr3[i8];
                    int i9 = i4 + 4;
                    long j10 = j4 - jArr3[i9];
                    int i10 = i4 + 5;
                    long j11 = j - jArr3[i6];
                    long j12 = j5 - jArr3[i10];
                    int i11 = i4 + 6;
                    int i12 = i5 + 1;
                    long j13 = j6 - (jArr3[i11] + jArr4[i12]);
                    int i13 = i4 + 7;
                    int[] iArr = access$200;
                    int[] iArr2 = access$100;
                    long j14 = j7 - (jArr3[i13] + jArr4[i5 + 2]);
                    jArr5 = jArr3;
                    long j15 = (long) i3;
                    int i14 = i3;
                    long j16 = j15;
                    long j17 = j8 - ((jArr3[i4 + 8] + j15) + 1);
                    long j18 = j10;
                    j10 = ThreefishEngine.xorRotr(j2 - jArr3[i7], 8, j14);
                    long j19 = j14 - j10;
                    j14 = j11;
                    j17 = ThreefishEngine.xorRotr(j17, 35, j14);
                    long j20 = j14 - j17;
                    j13 = ThreefishEngine.xorRotr(j13, 56, j9);
                    j9 -= j13;
                    jArr6 = jArr4;
                    long j21 = j17;
                    long xorRotr = ThreefishEngine.xorRotr(j18, 22, j12);
                    j12 -= xorRotr;
                    long xorRotr2 = ThreefishEngine.xorRotr(j10, 25, j12);
                    long j22 = j12 - xorRotr2;
                    long j23 = j19;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 29, j23);
                    j23 -= xorRotr;
                    j13 = ThreefishEngine.xorRotr(j13, 39, j20);
                    long j24 = j20 - j13;
                    j15 = ThreefishEngine.xorRotr(j21, 43, j9);
                    j9 -= j15;
                    j14 = ThreefishEngine.xorRotr(xorRotr2, 13, j9);
                    j9 -= j14;
                    int i15 = i11;
                    j = j22;
                    j15 = ThreefishEngine.xorRotr(j15, 50, j);
                    j -= j15;
                    j13 = ThreefishEngine.xorRotr(j13, 10, j23);
                    j23 -= j13;
                    long j25 = j15;
                    j15 = j24;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 17, j15);
                    j15 -= xorRotr;
                    j14 = ThreefishEngine.xorRotr(j14, 39, j15);
                    j15 -= j14;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 30, j9);
                    j9 -= xorRotr;
                    j13 = ThreefishEngine.xorRotr(j13, 34, j);
                    j -= j13;
                    long j26 = j13;
                    j13 = ThreefishEngine.xorRotr(j25, 24, j23);
                    j15 -= jArr5[i4];
                    j9 -= jArr5[i7];
                    j -= jArr5[i9];
                    long j27 = xorRotr - jArr5[i8];
                    xorRotr = j26 - (jArr5[i10] + jArr6[i5]);
                    j23 = (j23 - j13) - (jArr5[i15] + jArr6[i12]);
                    j13 -= jArr5[i13] + j16;
                    j14 = ThreefishEngine.xorRotr(j14 - jArr5[i6], 44, j23);
                    j23 -= j14;
                    j13 = ThreefishEngine.xorRotr(j13, 9, j15);
                    j15 -= j13;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 54, j9);
                    j9 -= xorRotr;
                    long j28 = j13;
                    j13 = ThreefishEngine.xorRotr(j27, 56, j);
                    j -= j13;
                    j14 = ThreefishEngine.xorRotr(j14, 17, j);
                    j -= j14;
                    j13 = ThreefishEngine.xorRotr(j13, 49, j23);
                    j23 -= j13;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 36, j15);
                    j15 -= xorRotr;
                    long j29 = j23;
                    j10 = ThreefishEngine.xorRotr(j28, 39, j9);
                    j9 -= j10;
                    j14 = ThreefishEngine.xorRotr(j14, 33, j9);
                    j9 -= j14;
                    j10 = ThreefishEngine.xorRotr(j10, 27, j);
                    j -= j10;
                    long j30 = j9;
                    long j31 = j29;
                    xorRotr = ThreefishEngine.xorRotr(xorRotr, 14, j31);
                    j31 -= xorRotr;
                    j13 = ThreefishEngine.xorRotr(j13, 42, j15);
                    j15 -= j13;
                    j14 = ThreefishEngine.xorRotr(j14, 46, j15);
                    long j32 = j15 - j14;
                    j15 = j30;
                    j4 = ThreefishEngine.xorRotr(j13, 36, j15);
                    j3 = j15 - j4;
                    j6 = ThreefishEngine.xorRotr(xorRotr, 19, j);
                    j5 = j - j6;
                    j8 = ThreefishEngine.xorRotr(j10, 37, j31);
                    j7 = j31 - j8;
                    i3 = i14 - 2;
                    j2 = j14;
                    access$200 = iArr;
                    access$100 = iArr2;
                    jArr3 = jArr5;
                    jArr4 = jArr6;
                    j = j32;
                    i = 0;
                    i2 = 1;
                }
                jArr6 = jArr4;
                jArr5 = jArr3;
                int i16 = i;
                j2 -= jArr5[1];
                j3 -= jArr5[2];
                j4 -= jArr5[3];
                j5 -= jArr5[4];
                j6 -= jArr5[5] + jArr6[i16];
                j7 -= jArr5[6] + jArr6[1];
                j8 -= jArr5[7];
                jArr2[i16] = j - jArr5[i16];
                jArr2[1] = j2;
                jArr2[2] = j3;
                jArr2[3] = j4;
                jArr2[4] = j5;
                jArr2[5] = j6;
                jArr2[6] = j7;
                jArr2[7] = j8;
            } else {
                throw new IllegalArgumentException();
            }
        }

        public void encryptBlock(long[] jArr, long[] jArr2) {
            long[] jArr3 = this.kw;
            long[] jArr4 = this.t;
            int[] access$200 = ThreefishEngine.MOD9;
            int[] access$100 = ThreefishEngine.MOD3;
            if (jArr3.length != 17) {
                throw new IllegalArgumentException();
            } else if (jArr4.length == 5) {
                long j;
                long j2;
                long j3 = jArr[0];
                long j4 = jArr[1];
                long j5 = jArr[2];
                long j6 = jArr[3];
                long j7 = jArr[4];
                long j8 = jArr[5];
                long j9 = jArr[6];
                j3 += jArr3[0];
                j4 += jArr3[1];
                j7 += jArr3[4];
                j9 += jArr3[6] + jArr4[1];
                int i = 1;
                long j10 = j8 + (jArr3[5] + jArr4[0]);
                long j11 = jArr[7] + jArr3[7];
                long j12 = j6 + jArr3[3];
                j6 = j5 + jArr3[2];
                while (i < 18) {
                    int i2 = access$200[i];
                    int i3 = access$100[i];
                    j3 += j4;
                    long rotlXor = ThreefishEngine.rotlXor(j4, 46, j3);
                    int[] iArr = access$200;
                    int[] iArr2 = access$100;
                    j = j6 + j12;
                    j12 = ThreefishEngine.rotlXor(j12, 36, j);
                    int i4 = i;
                    long j13 = j7 + j10;
                    j10 = ThreefishEngine.rotlXor(j10, 19, j13);
                    int i5 = i2;
                    long j14 = j3;
                    long j15 = j12;
                    j2 = j11;
                    j12 = j9 + j2;
                    j2 = ThreefishEngine.rotlXor(j2, 37, j12);
                    j += rotlXor;
                    long rotlXor2 = ThreefishEngine.rotlXor(rotlXor, 33, j);
                    j13 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 27, j13);
                    j12 += j10;
                    j10 = ThreefishEngine.rotlXor(j10, 14, j12);
                    long[] jArr5 = jArr3;
                    long j16 = j;
                    long j17 = j14 + j15;
                    long j18 = j2;
                    j2 = ThreefishEngine.rotlXor(j15, 42, j17);
                    j13 += rotlXor2;
                    rotlXor2 = ThreefishEngine.rotlXor(rotlXor2, 17, j13);
                    j12 += j2;
                    j2 = ThreefishEngine.rotlXor(j2, 49, j12);
                    j17 += j10;
                    long rotlXor3 = ThreefishEngine.rotlXor(j10, 36, j17);
                    j4 = j16 + j18;
                    long[] jArr6 = jArr4;
                    long j19 = j13;
                    long rotlXor4 = ThreefishEngine.rotlXor(j18, 39, j4);
                    j12 += rotlXor2;
                    rotlXor2 = ThreefishEngine.rotlXor(rotlXor2, 44, j12);
                    j17 += rotlXor4;
                    rotlXor4 = ThreefishEngine.rotlXor(rotlXor4, 9, j17);
                    j4 += rotlXor3;
                    rotlXor3 = ThreefishEngine.rotlXor(rotlXor3, 54, j4);
                    long j20 = rotlXor4;
                    rotlXor4 = j19 + j2;
                    int i6 = i5 + 1;
                    rotlXor2 += jArr5[i6];
                    int i7 = i5 + 2;
                    j4 += jArr5[i7];
                    int i8 = i5 + 3;
                    int i9 = i5 + 4;
                    rotlXor4 += jArr5[i9];
                    int i10 = i5 + 5;
                    rotlXor3 += jArr5[i10] + jArr6[i3];
                    int i11 = i5 + 6;
                    int i12 = i3 + 1;
                    j12 += jArr5[i11] + jArr6[i12];
                    int i13 = i5 + 7;
                    long rotlXor5 = ThreefishEngine.rotlXor(j2, 56, rotlXor4) + jArr5[i8];
                    int i14 = i4;
                    long j21 = (long) i14;
                    long j22 = j21;
                    j21 = j20 + (jArr5[i13] + j21);
                    j17 = (j17 + jArr5[i5]) + rotlXor2;
                    rotlXor2 = ThreefishEngine.rotlXor(rotlXor2, 39, j17);
                    j4 += rotlXor5;
                    int i15 = i14;
                    long j23 = j17;
                    long rotlXor6 = ThreefishEngine.rotlXor(rotlXor5, 30, j4);
                    rotlXor4 += rotlXor3;
                    j = ThreefishEngine.rotlXor(rotlXor3, 34, rotlXor4);
                    j12 += j21;
                    j10 = ThreefishEngine.rotlXor(j21, 24, j12);
                    j4 += rotlXor2;
                    j2 = ThreefishEngine.rotlXor(rotlXor2, 13, j4);
                    rotlXor4 += j10;
                    j10 = ThreefishEngine.rotlXor(j10, 50, rotlXor4);
                    j12 += j;
                    j = ThreefishEngine.rotlXor(j, 10, j12);
                    rotlXor2 = j23 + rotlXor6;
                    long j24 = j10;
                    rotlXor6 = ThreefishEngine.rotlXor(rotlXor6, 17, rotlXor2);
                    rotlXor4 += j2;
                    j21 = ThreefishEngine.rotlXor(j2, 25, rotlXor4);
                    j12 += rotlXor6;
                    rotlXor6 = ThreefishEngine.rotlXor(rotlXor6, 29, j12);
                    rotlXor2 += j;
                    j = ThreefishEngine.rotlXor(j, 39, rotlXor2);
                    j4 += j24;
                    long j25 = rotlXor4;
                    rotlXor4 = ThreefishEngine.rotlXor(j24, 43, j4);
                    j12 += j21;
                    j21 = ThreefishEngine.rotlXor(j21, 8, j12);
                    rotlXor2 += rotlXor4;
                    rotlXor4 = ThreefishEngine.rotlXor(rotlXor4, 35, rotlXor2);
                    j4 += j;
                    j = ThreefishEngine.rotlXor(j, 56, j4);
                    long j26 = rotlXor2;
                    j3 = j25 + rotlXor6;
                    long j27 = j26 + jArr5[i6];
                    j6 = j4 + jArr5[i8];
                    j7 = j3 + jArr5[i10];
                    j9 = j12 + (jArr5[i13] + jArr6[i3 + 2]);
                    j11 = rotlXor4 + ((jArr5[i5 + 8] + j22) + 1);
                    i = i15 + 2;
                    j12 = ThreefishEngine.rotlXor(rotlXor6, 22, j3) + jArr5[i9];
                    j4 = j21 + jArr5[i7];
                    j3 = j27;
                    jArr3 = jArr5;
                    jArr4 = jArr6;
                    j10 = j + (jArr5[i11] + jArr6[i12]);
                    access$200 = iArr;
                    access$100 = iArr2;
                }
                j = j10;
                j2 = j11;
                jArr2[0] = j3;
                jArr2[1] = j4;
                jArr2[2] = j6;
                jArr2[3] = j12;
                jArr2[4] = j7;
                jArr2[5] = j;
                jArr2[6] = j9;
                jArr2[7] = j2;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    static {
        for (int i = 0; i < MOD9.length; i++) {
            MOD17[i] = i % 17;
            MOD9[i] = i % 9;
            MOD5[i] = i % 5;
            MOD3[i] = i % 3;
        }
    }

    public ThreefishEngine(int i) {
        ThreefishCipher threefish256Cipher;
        this.blocksizeBytes = i / 8;
        this.blocksizeWords = this.blocksizeBytes / 8;
        this.currentBlock = new long[this.blocksizeWords];
        this.kw = new long[((2 * this.blocksizeWords) + 1)];
        if (i == 256) {
            threefish256Cipher = new Threefish256Cipher(this.kw, this.t);
        } else if (i == 512) {
            threefish256Cipher = new Threefish512Cipher(this.kw, this.t);
        } else if (i == 1024) {
            threefish256Cipher = new Threefish1024Cipher(this.kw, this.t);
        } else {
            throw new IllegalArgumentException("Invalid blocksize - Threefish is defined with block size of 256, 512, or 1024 bits");
        }
        this.cipher = threefish256Cipher;
    }

    public static long bytesToWord(byte[] bArr, int i) {
        if (i + 8 <= bArr.length) {
            int i2 = i + 1;
            i = i2 + 1;
            int i3 = i + 1;
            i = i3 + 1;
            i3 = i + 1;
            i = i3 + 1;
            return ((((long) bArr[i + 1]) & 255) << 56) | (((((((((long) bArr[i]) & 255) | ((((long) bArr[i2]) & 255) << 8)) | ((((long) bArr[i]) & 255) << 16)) | ((((long) bArr[i3]) & 255) << 24)) | ((((long) bArr[i]) & 255) << 32)) | ((((long) bArr[i3]) & 255) << 40)) | ((((long) bArr[i]) & 255) << 48));
        }
        throw new IllegalArgumentException();
    }

    static long rotlXor(long j, int i, long j2) {
        return ((j >>> (-i)) | (j << i)) ^ j2;
    }

    private void setKey(long[] jArr) {
        if (jArr.length == this.blocksizeWords) {
            long j = C_240;
            for (int i = 0; i < this.blocksizeWords; i++) {
                this.kw[i] = jArr[i];
                j ^= this.kw[i];
            }
            this.kw[this.blocksizeWords] = j;
            System.arraycopy(this.kw, 0, this.kw, this.blocksizeWords + 1, this.blocksizeWords);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Threefish key must be same size as block (");
        stringBuilder.append(this.blocksizeWords);
        stringBuilder.append(" words)");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void setTweak(long[] jArr) {
        if (jArr.length == 2) {
            this.t[0] = jArr[0];
            this.t[1] = jArr[1];
            this.t[2] = this.t[0] ^ this.t[1];
            this.t[3] = this.t[0];
            this.t[4] = this.t[1];
            return;
        }
        throw new IllegalArgumentException("Tweak must be 2 words.");
    }

    public static void wordToBytes(long j, byte[] bArr, int i) {
        if (i + 8 <= bArr.length) {
            int i2 = i + 1;
            bArr[i] = (byte) ((int) j);
            i = i2 + 1;
            bArr[i2] = (byte) ((int) (j >> 8));
            i2 = i + 1;
            bArr[i] = (byte) ((int) (j >> 16));
            i = i2 + 1;
            bArr[i2] = (byte) ((int) (j >> 24));
            i2 = i + 1;
            bArr[i] = (byte) ((int) (j >> 32));
            i = i2 + 1;
            bArr[i2] = (byte) ((int) (j >> 40));
            i2 = i + 1;
            bArr[i] = (byte) ((int) (j >> 48));
            bArr[i2] = (byte) ((int) (j >> 56));
            return;
        }
        throw new IllegalArgumentException();
    }

    static long xorRotr(long j, int i, long j2) {
        j ^= j2;
        return (j << (-i)) | (j >>> i);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Threefish-");
        stringBuilder.append(this.blocksizeBytes * 8);
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.blocksizeBytes;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        byte[] key;
        byte[] tweak;
        long[] jArr;
        long[] jArr2 = null;
        if (cipherParameters instanceof TweakableBlockCipherParameters) {
            TweakableBlockCipherParameters tweakableBlockCipherParameters = (TweakableBlockCipherParameters) cipherParameters;
            key = tweakableBlockCipherParameters.getKey().getKey();
            tweak = tweakableBlockCipherParameters.getTweak();
        } else if (cipherParameters instanceof KeyParameter) {
            key = ((KeyParameter) cipherParameters).getKey();
            tweak = null;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid parameter passed to Threefish init - ");
            stringBuilder.append(cipherParameters.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        if (key == null) {
            jArr = null;
        } else if (key.length == this.blocksizeBytes) {
            jArr = new long[this.blocksizeWords];
            for (int i = 0; i < jArr.length; i++) {
                jArr[i] = bytesToWord(key, i * 8);
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Threefish key must be same size as block (");
            stringBuilder2.append(this.blocksizeBytes);
            stringBuilder2.append(" bytes)");
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
        if (tweak != null) {
            if (tweak.length == 16) {
                jArr2 = new long[]{bytesToWord(tweak, 0), bytesToWord(tweak, 8)};
            } else {
                throw new IllegalArgumentException("Threefish tweak must be 16 bytes");
            }
        }
        init(z, jArr, jArr2);
    }

    public void init(boolean z, long[] jArr, long[] jArr2) {
        this.forEncryption = z;
        if (jArr != null) {
            setKey(jArr);
        }
        if (jArr2 != null) {
            setTweak(jArr2);
        }
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.blocksizeBytes + i > bArr.length) {
            throw new DataLengthException("Input buffer too short");
        } else if (this.blocksizeBytes + i2 <= bArr2.length) {
            int i3 = 0;
            for (int i4 = 0; i4 < this.blocksizeBytes; i4 += 8) {
                this.currentBlock[i4 >> 3] = bytesToWord(bArr, i + i4);
            }
            processBlock(this.currentBlock, this.currentBlock);
            while (i3 < this.blocksizeBytes) {
                wordToBytes(this.currentBlock[i3 >> 3], bArr2, i2 + i3);
                i3 += 8;
            }
            return this.blocksizeBytes;
        } else {
            throw new OutputLengthException("Output buffer too short");
        }
    }

    public int processBlock(long[] jArr, long[] jArr2) throws DataLengthException, IllegalStateException {
        if (this.kw[this.blocksizeWords] == 0) {
            throw new IllegalStateException("Threefish engine not initialised");
        } else if (jArr.length != this.blocksizeWords) {
            throw new DataLengthException("Input buffer too short");
        } else if (jArr2.length == this.blocksizeWords) {
            if (this.forEncryption) {
                this.cipher.encryptBlock(jArr, jArr2);
            } else {
                this.cipher.decryptBlock(jArr, jArr2);
            }
            return this.blocksizeWords;
        } else {
            throw new OutputLengthException("Output buffer too short");
        }
    }

    public void reset() {
    }
}
