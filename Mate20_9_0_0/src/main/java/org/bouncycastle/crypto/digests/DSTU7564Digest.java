package org.bouncycastle.crypto.digests;

import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class DSTU7564Digest implements ExtendedDigest, Memoable {
    private static final int NB_1024 = 16;
    private static final int NB_512 = 8;
    private static final int NR_1024 = 14;
    private static final int NR_512 = 10;
    private static final byte[] S0 = new byte[]{(byte) -88, (byte) 67, (byte) 95, (byte) 6, (byte) 107, (byte) 117, (byte) 108, (byte) 89, (byte) 113, (byte) -33, (byte) -121, (byte) -107, (byte) 23, (byte) -16, (byte) -40, (byte) 9, (byte) 109, (byte) -13, (byte) 29, (byte) -53, (byte) -55, (byte) 77, (byte) 44, (byte) -81, (byte) 121, (byte) -32, (byte) -105, (byte) -3, (byte) 111, (byte) 75, (byte) 69, (byte) 57, (byte) 62, (byte) -35, (byte) -93, (byte) 79, (byte) -76, (byte) -74, (byte) -102, (byte) 14, (byte) 31, (byte) -65, (byte) 21, (byte) -31, (byte) 73, (byte) -46, (byte) -109, (byte) -58, (byte) -110, (byte) 114, (byte) -98, (byte) 97, (byte) -47, (byte) 99, (byte) -6, (byte) -18, (byte) -12, (byte) 25, (byte) -43, (byte) -83, (byte) 88, (byte) -92, (byte) -69, (byte) -95, (byte) -36, (byte) -14, (byte) -125, (byte) 55, (byte) 66, (byte) -28, (byte) 122, (byte) 50, (byte) -100, (byte) -52, (byte) -85, (byte) 74, (byte) -113, (byte) 110, (byte) 4, (byte) 39, (byte) 46, (byte) -25, (byte) -30, (byte) 90, (byte) -106, (byte) 22, (byte) 35, (byte) 43, (byte) -62, (byte) 101, (byte) 102, (byte) 15, PSSSigner.TRAILER_IMPLICIT, (byte) -87, (byte) 71, (byte) 65, (byte) 52, (byte) 72, (byte) -4, (byte) -73, (byte) 106, (byte) -120, (byte) -91, (byte) 83, (byte) -122, (byte) -7, (byte) 91, (byte) -37, (byte) 56, (byte) 123, (byte) -61, (byte) 30, (byte) 34, (byte) 51, (byte) 36, (byte) 40, (byte) 54, (byte) -57, (byte) -78, (byte) 59, (byte) -114, (byte) 119, (byte) -70, (byte) -11, (byte) 20, (byte) -97, (byte) 8, (byte) 85, (byte) -101, (byte) 76, (byte) -2, (byte) 96, (byte) 92, (byte) -38, (byte) 24, (byte) 70, (byte) -51, (byte) 125, (byte) 33, (byte) -80, (byte) 63, (byte) 27, (byte) -119, (byte) -1, (byte) -21, (byte) -124, (byte) 105, (byte) 58, (byte) -99, (byte) -41, (byte) -45, (byte) 112, (byte) 103, (byte) 64, (byte) -75, (byte) -34, (byte) 93, (byte) 48, (byte) -111, (byte) -79, (byte) 120, (byte) 17, (byte) 1, (byte) -27, (byte) 0, (byte) 104, (byte) -104, (byte) -96, (byte) -59, (byte) 2, (byte) -90, (byte) 116, (byte) 45, (byte) 11, (byte) -94, (byte) 118, (byte) -77, (byte) -66, (byte) -50, (byte) -67, (byte) -82, (byte) -23, (byte) -118, (byte) 49, (byte) 28, (byte) -20, (byte) -15, (byte) -103, (byte) -108, (byte) -86, (byte) -10, (byte) 38, (byte) 47, (byte) -17, (byte) -24, (byte) -116, (byte) 53, (byte) 3, (byte) -44, Byte.MAX_VALUE, (byte) -5, (byte) 5, (byte) -63, (byte) 94, (byte) -112, (byte) 32, (byte) 61, (byte) -126, (byte) -9, (byte) -22, (byte) 10, (byte) 13, (byte) 126, (byte) -8, (byte) 80, (byte) 26, (byte) -60, (byte) 7, (byte) 87, (byte) -72, (byte) 60, (byte) 98, (byte) -29, (byte) -56, (byte) -84, (byte) 82, (byte) 100, Tnaf.POW_2_WIDTH, (byte) -48, (byte) -39, (byte) 19, (byte) 12, (byte) 18, (byte) 41, (byte) 81, (byte) -71, (byte) -49, (byte) -42, (byte) 115, (byte) -115, (byte) -127, (byte) 84, (byte) -64, (byte) -19, (byte) 78, (byte) 68, (byte) -89, (byte) 42, (byte) -123, (byte) 37, (byte) -26, (byte) -54, (byte) 124, (byte) -117, (byte) 86, Byte.MIN_VALUE};
    private static final byte[] S1 = new byte[]{(byte) -50, (byte) -69, (byte) -21, (byte) -110, (byte) -22, (byte) -53, (byte) 19, (byte) -63, (byte) -23, (byte) 58, (byte) -42, (byte) -78, (byte) -46, (byte) -112, (byte) 23, (byte) -8, (byte) 66, (byte) 21, (byte) 86, (byte) -76, (byte) 101, (byte) 28, (byte) -120, (byte) 67, (byte) -59, (byte) 92, (byte) 54, (byte) -70, (byte) -11, (byte) 87, (byte) 103, (byte) -115, (byte) 49, (byte) -10, (byte) 100, (byte) 88, (byte) -98, (byte) -12, (byte) 34, (byte) -86, (byte) 117, (byte) 15, (byte) 2, (byte) -79, (byte) -33, (byte) 109, (byte) 115, (byte) 77, (byte) 124, (byte) 38, (byte) 46, (byte) -9, (byte) 8, (byte) 93, (byte) 68, (byte) 62, (byte) -97, (byte) 20, (byte) -56, (byte) -82, (byte) 84, Tnaf.POW_2_WIDTH, (byte) -40, PSSSigner.TRAILER_IMPLICIT, (byte) 26, (byte) 107, (byte) 105, (byte) -13, (byte) -67, (byte) 51, (byte) -85, (byte) -6, (byte) -47, (byte) -101, (byte) 104, (byte) 78, (byte) 22, (byte) -107, (byte) -111, (byte) -18, (byte) 76, (byte) 99, (byte) -114, (byte) 91, (byte) -52, (byte) 60, (byte) 25, (byte) -95, (byte) -127, (byte) 73, (byte) 123, (byte) -39, (byte) 111, (byte) 55, (byte) 96, (byte) -54, (byte) -25, (byte) 43, (byte) 72, (byte) -3, (byte) -106, (byte) 69, (byte) -4, (byte) 65, (byte) 18, (byte) 13, (byte) 121, (byte) -27, (byte) -119, (byte) -116, (byte) -29, (byte) 32, (byte) 48, (byte) -36, (byte) -73, (byte) 108, (byte) 74, (byte) -75, (byte) 63, (byte) -105, (byte) -44, (byte) 98, (byte) 45, (byte) 6, (byte) -92, (byte) -91, (byte) -125, (byte) 95, (byte) 42, (byte) -38, (byte) -55, (byte) 0, (byte) 126, (byte) -94, (byte) 85, (byte) -65, (byte) 17, (byte) -43, (byte) -100, (byte) -49, (byte) 14, (byte) 10, (byte) 61, (byte) 81, (byte) 125, (byte) -109, (byte) 27, (byte) -2, (byte) -60, (byte) 71, (byte) 9, (byte) -122, (byte) 11, (byte) -113, (byte) -99, (byte) 106, (byte) 7, (byte) -71, (byte) -80, (byte) -104, (byte) 24, (byte) 50, (byte) 113, (byte) 75, (byte) -17, (byte) 59, (byte) 112, (byte) -96, (byte) -28, (byte) 64, (byte) -1, (byte) -61, (byte) -87, (byte) -26, (byte) 120, (byte) -7, (byte) -117, (byte) 70, Byte.MIN_VALUE, (byte) 30, (byte) 56, (byte) -31, (byte) -72, (byte) -88, (byte) -32, (byte) 12, (byte) 35, (byte) 118, (byte) 29, (byte) 37, (byte) 36, (byte) 5, (byte) -15, (byte) 110, (byte) -108, (byte) 40, (byte) -102, (byte) -124, (byte) -24, (byte) -93, (byte) 79, (byte) 119, (byte) -45, (byte) -123, (byte) -30, (byte) 82, (byte) -14, (byte) -126, (byte) 80, (byte) 122, (byte) 47, (byte) 116, (byte) 83, (byte) -77, (byte) 97, (byte) -81, (byte) 57, (byte) 53, (byte) -34, (byte) -51, (byte) 31, (byte) -103, (byte) -84, (byte) -83, (byte) 114, (byte) 44, (byte) -35, (byte) -48, (byte) -121, (byte) -66, (byte) 94, (byte) -90, (byte) -20, (byte) 4, (byte) -58, (byte) 3, (byte) 52, (byte) -5, (byte) -37, (byte) 89, (byte) -74, (byte) -62, (byte) 1, (byte) -16, (byte) 90, (byte) -19, (byte) -89, (byte) 102, (byte) 33, Byte.MAX_VALUE, (byte) -118, (byte) 39, (byte) -57, (byte) -64, (byte) 41, (byte) -41};
    private static final byte[] S2 = new byte[]{(byte) -109, (byte) -39, (byte) -102, (byte) -75, (byte) -104, (byte) 34, (byte) 69, (byte) -4, (byte) -70, (byte) 106, (byte) -33, (byte) 2, (byte) -97, (byte) -36, (byte) 81, (byte) 89, (byte) 74, (byte) 23, (byte) 43, (byte) -62, (byte) -108, (byte) -12, (byte) -69, (byte) -93, (byte) 98, (byte) -28, (byte) 113, (byte) -44, (byte) -51, (byte) 112, (byte) 22, (byte) -31, (byte) 73, (byte) 60, (byte) -64, (byte) -40, (byte) 92, (byte) -101, (byte) -83, (byte) -123, (byte) 83, (byte) -95, (byte) 122, (byte) -56, (byte) 45, (byte) -32, (byte) -47, (byte) 114, (byte) -90, (byte) 44, (byte) -60, (byte) -29, (byte) 118, (byte) 120, (byte) -73, (byte) -76, (byte) 9, (byte) 59, (byte) 14, (byte) 65, (byte) 76, (byte) -34, (byte) -78, (byte) -112, (byte) 37, (byte) -91, (byte) -41, (byte) 3, (byte) 17, (byte) 0, (byte) -61, (byte) 46, (byte) -110, (byte) -17, (byte) 78, (byte) 18, (byte) -99, (byte) 125, (byte) -53, (byte) 53, Tnaf.POW_2_WIDTH, (byte) -43, (byte) 79, (byte) -98, (byte) 77, (byte) -87, (byte) 85, (byte) -58, (byte) -48, (byte) 123, (byte) 24, (byte) -105, (byte) -45, (byte) 54, (byte) -26, (byte) 72, (byte) 86, (byte) -127, (byte) -113, (byte) 119, (byte) -52, (byte) -100, (byte) -71, (byte) -30, (byte) -84, (byte) -72, (byte) 47, (byte) 21, (byte) -92, (byte) 124, (byte) -38, (byte) 56, (byte) 30, (byte) 11, (byte) 5, (byte) -42, (byte) 20, (byte) 110, (byte) 108, (byte) 126, (byte) 102, (byte) -3, (byte) -79, (byte) -27, (byte) 96, (byte) -81, (byte) 94, (byte) 51, (byte) -121, (byte) -55, (byte) -16, (byte) 93, (byte) 109, (byte) 63, (byte) -120, (byte) -115, (byte) -57, (byte) -9, (byte) 29, (byte) -23, (byte) -20, (byte) -19, Byte.MIN_VALUE, (byte) 41, (byte) 39, (byte) -49, (byte) -103, (byte) -88, (byte) 80, (byte) 15, (byte) 55, (byte) 36, (byte) 40, (byte) 48, (byte) -107, (byte) -46, (byte) 62, (byte) 91, (byte) 64, (byte) -125, (byte) -77, (byte) 105, (byte) 87, (byte) 31, (byte) 7, (byte) 28, (byte) -118, PSSSigner.TRAILER_IMPLICIT, (byte) 32, (byte) -21, (byte) -50, (byte) -114, (byte) -85, (byte) -18, (byte) 49, (byte) -94, (byte) 115, (byte) -7, (byte) -54, (byte) 58, (byte) 26, (byte) -5, (byte) 13, (byte) -63, (byte) -2, (byte) -6, (byte) -14, (byte) 111, (byte) -67, (byte) -106, (byte) -35, (byte) 67, (byte) 82, (byte) -74, (byte) 8, (byte) -13, (byte) -82, (byte) -66, (byte) 25, (byte) -119, (byte) 50, (byte) 38, (byte) -80, (byte) -22, (byte) 75, (byte) 100, (byte) -124, (byte) -126, (byte) 107, (byte) -11, (byte) 121, (byte) -65, (byte) 1, (byte) 95, (byte) 117, (byte) 99, (byte) 27, (byte) 35, (byte) 61, (byte) 104, (byte) 42, (byte) 101, (byte) -24, (byte) -111, (byte) -10, (byte) -1, (byte) 19, (byte) 88, (byte) -15, (byte) 71, (byte) 10, Byte.MAX_VALUE, (byte) -59, (byte) -89, (byte) -25, (byte) 97, (byte) 90, (byte) 6, (byte) 70, (byte) 68, (byte) 66, (byte) 4, (byte) -96, (byte) -37, (byte) 57, (byte) -122, (byte) 84, (byte) -86, (byte) -116, (byte) 52, (byte) 33, (byte) -117, (byte) -8, (byte) 12, (byte) 116, (byte) 103};
    private static final byte[] S3 = new byte[]{(byte) 104, (byte) -115, (byte) -54, (byte) 77, (byte) 115, (byte) 75, (byte) 78, (byte) 42, (byte) -44, (byte) 82, (byte) 38, (byte) -77, (byte) 84, (byte) 30, (byte) 25, (byte) 31, (byte) 34, (byte) 3, (byte) 70, (byte) 61, (byte) 45, (byte) 74, (byte) 83, (byte) -125, (byte) 19, (byte) -118, (byte) -73, (byte) -43, (byte) 37, (byte) 121, (byte) -11, (byte) -67, (byte) 88, (byte) 47, (byte) 13, (byte) 2, (byte) -19, (byte) 81, (byte) -98, (byte) 17, (byte) -14, (byte) 62, (byte) 85, (byte) 94, (byte) -47, (byte) 22, (byte) 60, (byte) 102, (byte) 112, (byte) 93, (byte) -13, (byte) 69, (byte) 64, (byte) -52, (byte) -24, (byte) -108, (byte) 86, (byte) 8, (byte) -50, (byte) 26, (byte) 58, (byte) -46, (byte) -31, (byte) -33, (byte) -75, (byte) 56, (byte) 110, (byte) 14, (byte) -27, (byte) -12, (byte) -7, (byte) -122, (byte) -23, (byte) 79, (byte) -42, (byte) -123, (byte) 35, (byte) -49, (byte) 50, (byte) -103, (byte) 49, (byte) 20, (byte) -82, (byte) -18, (byte) -56, (byte) 72, (byte) -45, (byte) 48, (byte) -95, (byte) -110, (byte) 65, (byte) -79, (byte) 24, (byte) -60, (byte) 44, (byte) 113, (byte) 114, (byte) 68, (byte) 21, (byte) -3, (byte) 55, (byte) -66, (byte) 95, (byte) -86, (byte) -101, (byte) -120, (byte) -40, (byte) -85, (byte) -119, (byte) -100, (byte) -6, (byte) 96, (byte) -22, PSSSigner.TRAILER_IMPLICIT, (byte) 98, (byte) 12, (byte) 36, (byte) -90, (byte) -88, (byte) -20, (byte) 103, (byte) 32, (byte) -37, (byte) 124, (byte) 40, (byte) -35, (byte) -84, (byte) 91, (byte) 52, (byte) 126, Tnaf.POW_2_WIDTH, (byte) -15, (byte) 123, (byte) -113, (byte) 99, (byte) -96, (byte) 5, (byte) -102, (byte) 67, (byte) 119, (byte) 33, (byte) -65, (byte) 39, (byte) 9, (byte) -61, (byte) -97, (byte) -74, (byte) -41, (byte) 41, (byte) -62, (byte) -21, (byte) -64, (byte) -92, (byte) -117, (byte) -116, (byte) 29, (byte) -5, (byte) -1, (byte) -63, (byte) -78, (byte) -105, (byte) 46, (byte) -8, (byte) 101, (byte) -10, (byte) 117, (byte) 7, (byte) 4, (byte) 73, (byte) 51, (byte) -28, (byte) -39, (byte) -71, (byte) -48, (byte) 66, (byte) -57, (byte) 108, (byte) -112, (byte) 0, (byte) -114, (byte) 111, (byte) 80, (byte) 1, (byte) -59, (byte) -38, (byte) 71, (byte) 63, (byte) -51, (byte) 105, (byte) -94, (byte) -30, (byte) 122, (byte) -89, (byte) -58, (byte) -109, (byte) 15, (byte) 10, (byte) 6, (byte) -26, (byte) 43, (byte) -106, (byte) -93, (byte) 28, (byte) -81, (byte) 106, (byte) 18, (byte) -124, (byte) 57, (byte) -25, (byte) -80, (byte) -126, (byte) -9, (byte) -2, (byte) -99, (byte) -121, (byte) 92, (byte) -127, (byte) 53, (byte) -34, (byte) -76, (byte) -91, (byte) -4, Byte.MIN_VALUE, (byte) -17, (byte) -53, (byte) -69, (byte) 107, (byte) 118, (byte) -70, (byte) 90, (byte) 125, (byte) 120, (byte) 11, (byte) -107, (byte) -29, (byte) -83, (byte) 116, (byte) -104, (byte) 59, (byte) 54, (byte) 100, (byte) 109, (byte) -36, (byte) -16, (byte) 89, (byte) -87, (byte) 76, (byte) 23, Byte.MAX_VALUE, (byte) -111, (byte) -72, (byte) -55, (byte) 87, (byte) 27, (byte) -32, (byte) 97};
    private int blockSize;
    private byte[] buf;
    private int bufOff;
    private int columns;
    private int hashSize;
    private long inputBlocks;
    private int rounds;
    private long[] state;
    private long[] tempState1;
    private long[] tempState2;

    public DSTU7564Digest(int i) {
        if (i == 256 || i == 384 || i == 512) {
            this.hashSize = i >>> 3;
            if (i > 256) {
                this.columns = 16;
                i = 14;
            } else {
                this.columns = 8;
                i = 10;
            }
            this.rounds = i;
            this.blockSize = this.columns << 3;
            this.state = new long[this.columns];
            this.state[0] = (long) this.blockSize;
            this.tempState1 = new long[this.columns];
            this.tempState2 = new long[this.columns];
            this.buf = new byte[this.blockSize];
            return;
        }
        throw new IllegalArgumentException("Hash size is not recommended. Use 256/384/512 instead");
    }

    public DSTU7564Digest(DSTU7564Digest dSTU7564Digest) {
        copyIn(dSTU7564Digest);
    }

    private void P(long[] jArr) {
        for (int i = 0; i < this.rounds; i++) {
            long j = (long) i;
            for (int i2 = 0; i2 < this.columns; i2++) {
                jArr[i2] = jArr[i2] ^ j;
                j += 16;
            }
            shiftRows(jArr);
            subBytes(jArr);
            mixColumns(jArr);
        }
    }

    private void Q(long[] jArr) {
        for (int i = 0; i < this.rounds; i++) {
            long j = (((long) (((this.columns - 1) << 4) ^ i)) << 56) | 67818912035696883L;
            for (int i2 = 0; i2 < this.columns; i2++) {
                jArr[i2] = jArr[i2] + j;
                j -= 1152921504606846976L;
            }
            shiftRows(jArr);
            subBytes(jArr);
            mixColumns(jArr);
        }
    }

    private void copyIn(DSTU7564Digest dSTU7564Digest) {
        this.hashSize = dSTU7564Digest.hashSize;
        this.blockSize = dSTU7564Digest.blockSize;
        this.rounds = dSTU7564Digest.rounds;
        if (this.columns <= 0 || this.columns != dSTU7564Digest.columns) {
            this.columns = dSTU7564Digest.columns;
            this.state = Arrays.clone(dSTU7564Digest.state);
            this.tempState1 = new long[this.columns];
            this.tempState2 = new long[this.columns];
            this.buf = Arrays.clone(dSTU7564Digest.buf);
        } else {
            System.arraycopy(dSTU7564Digest.state, 0, this.state, 0, this.columns);
            System.arraycopy(dSTU7564Digest.buf, 0, this.buf, 0, this.blockSize);
        }
        this.inputBlocks = dSTU7564Digest.inputBlocks;
        this.bufOff = dSTU7564Digest.bufOff;
    }

    private static long mixColumn(long j) {
        long j2 = ((9187201950435737471L & j) << 1) ^ (((j & -9187201950435737472L) >>> 7) * 29);
        long rotate = rotate(8, j) ^ j;
        rotate = (rotate ^ rotate(16, rotate)) ^ rotate(48, j);
        j = (j ^ rotate) ^ j2;
        long j3 = ((-9187201950435737472L & j) >>> 6) * 29;
        return ((rotate(32, (((j & 4629771061636907072L) >>> 6) * 29) ^ (j3 ^ ((4557430888798830399L & j) << 2))) ^ rotate) ^ rotate(40, j2)) ^ rotate(48, j2);
    }

    private void mixColumns(long[] jArr) {
        for (int i = 0; i < this.columns; i++) {
            jArr[i] = mixColumn(jArr[i]);
        }
    }

    private void processBlock(byte[] bArr, int i) {
        int i2 = 0;
        int i3 = i;
        for (i = 0; i < this.columns; i++) {
            long littleEndianToLong = Pack.littleEndianToLong(bArr, i3);
            i3 += 8;
            this.tempState1[i] = this.state[i] ^ littleEndianToLong;
            this.tempState2[i] = littleEndianToLong;
        }
        P(this.tempState1);
        Q(this.tempState2);
        while (i2 < this.columns) {
            long[] jArr = this.state;
            jArr[i2] = jArr[i2] ^ (this.tempState1[i2] ^ this.tempState2[i2]);
            i2++;
        }
    }

    private static long rotate(int i, long j) {
        return (j << (-i)) | (j >>> i);
    }

    private void shiftRows(long[] jArr) {
        int i = this.columns;
        long j;
        long j2;
        long j3;
        long j4;
        long j5;
        long j6;
        long j7;
        long j8;
        long j9;
        long j10;
        long j11;
        long j12;
        if (i == 8) {
            j = jArr[0];
            j2 = jArr[1];
            j3 = jArr[2];
            j4 = jArr[3];
            j5 = jArr[4];
            j6 = jArr[5];
            j7 = jArr[6];
            j8 = jArr[7];
            j9 = (j ^ j5) & -4294967296L;
            j ^= j9;
            j5 ^= j9;
            j9 = (j2 ^ j6) & 72057594021150720L;
            j2 ^= j9;
            j6 ^= j9;
            j9 = (j3 ^ j7) & 281474976645120L;
            j3 ^= j9;
            j7 ^= j9;
            j9 = (j4 ^ j8) & 1099511627520L;
            j4 ^= j9;
            j8 ^= j9;
            j9 = (j ^ j3) & -281470681808896L;
            j ^= j9;
            j3 ^= j9;
            j9 = (j2 ^ j4) & 72056494543077120L;
            j2 ^= j9;
            j4 ^= j9;
            j10 = (j5 ^ j7) & -281470681808896L;
            j5 ^= j10;
            j10 = j7 ^ j10;
            j11 = (j6 ^ j8) & 72056494543077120L;
            j6 ^= j11;
            j11 = j8 ^ j11;
            j7 = (j ^ j2) & -71777214294589696L;
            j ^= j7;
            j2 ^= j7;
            j7 = (j3 ^ j4) & -71777214294589696L;
            j3 ^= j7;
            j4 ^= j7;
            j7 = (j5 ^ j6) & -71777214294589696L;
            j5 ^= j7;
            j6 ^= j7;
            j12 = (j10 ^ j11) & -71777214294589696L;
            j10 ^= j12;
            j11 ^= j12;
            jArr[0] = j;
            jArr[1] = j2;
            jArr[2] = j3;
            jArr[3] = j4;
            jArr[4] = j5;
            jArr[5] = j6;
            jArr[6] = j10;
            jArr[7] = j11;
        } else if (i == 16) {
            j = jArr[0];
            j2 = jArr[1];
            j3 = jArr[2];
            j4 = jArr[3];
            j5 = jArr[4];
            j6 = jArr[5];
            j7 = jArr[6];
            j8 = jArr[7];
            j9 = jArr[8];
            long j13 = jArr[9];
            long j14 = jArr[10];
            long j15 = jArr[11];
            long j16 = jArr[12];
            long j17 = jArr[13];
            long j18 = jArr[14];
            long j19 = jArr[15];
            long j20 = (j ^ j9) & -72057594037927936L;
            j ^= j20;
            j9 ^= j20;
            j20 = (j2 ^ j13) & -72057594037927936L;
            j2 ^= j20;
            j13 ^= j20;
            j20 = (j3 ^ j14) & -281474976710656L;
            j3 ^= j20;
            j14 ^= j20;
            j20 = (j4 ^ j15) & -1099511627776L;
            j4 ^= j20;
            j15 ^= j20;
            j20 = (j5 ^ j16) & -4294967296L;
            j5 ^= j20;
            j16 ^= j20;
            j20 = (j6 ^ j17) & 72057594021150720L;
            j6 ^= j20;
            j17 ^= j20;
            j20 = (j7 ^ j18) & 72057594037862400L;
            j7 ^= j20;
            j18 ^= j20;
            j20 = (j8 ^ j19) & 72057594037927680L;
            j8 ^= j20;
            j19 ^= j20;
            j20 = (j ^ j5) & 72057589742960640L;
            j ^= j20;
            j5 ^= j20;
            j20 = (j2 ^ j6) & -16777216;
            j2 ^= j20;
            j6 ^= j20;
            j20 = (j3 ^ j7) & -71776119061282816L;
            j3 ^= j20;
            j7 ^= j20;
            j20 = (j4 ^ j8) & -72056494526300416L;
            j4 ^= j20;
            j8 ^= j20;
            j20 = (j9 ^ j16) & 72057589742960640L;
            j9 ^= j20;
            j16 ^= j20;
            j20 = (j13 ^ j17) & -16777216;
            j13 ^= j20;
            j17 ^= j20;
            j20 = (j14 ^ j18) & -71776119061282816L;
            j14 ^= j20;
            j18 ^= j20;
            j20 = (j15 ^ j19) & -72056494526300416L;
            j15 ^= j20;
            j19 ^= j20;
            j20 = (j ^ j3) & -281470681808896L;
            j ^= j20;
            j3 ^= j20;
            j20 = (j2 ^ j4) & 72056494543077120L;
            j2 ^= j20;
            j4 ^= j20;
            j20 = (j5 ^ j7) & -281470681808896L;
            j5 ^= j20;
            j7 ^= j20;
            j20 = (j6 ^ j8) & 72056494543077120L;
            j6 ^= j20;
            j8 ^= j20;
            j20 = (j9 ^ j14) & -281470681808896L;
            j9 ^= j20;
            j14 ^= j20;
            j20 = (j13 ^ j15) & 72056494543077120L;
            j13 ^= j20;
            j15 ^= j20;
            j10 = (j16 ^ j18) & -281470681808896L;
            j16 ^= j10;
            j10 = j18 ^ j10;
            j11 = (j17 ^ j19) & 72056494543077120L;
            j17 ^= j11;
            j11 = j19 ^ j11;
            j18 = (j ^ j2) & -71777214294589696L;
            j ^= j18;
            j2 ^= j18;
            j18 = (j3 ^ j4) & -71777214294589696L;
            j3 ^= j18;
            j4 ^= j18;
            j18 = (j5 ^ j6) & -71777214294589696L;
            j5 ^= j18;
            j6 ^= j18;
            j18 = (j7 ^ j8) & -71777214294589696L;
            j7 ^= j18;
            j8 ^= j18;
            j18 = (j9 ^ j13) & -71777214294589696L;
            j9 ^= j18;
            j13 ^= j18;
            j18 = (j14 ^ j15) & -71777214294589696L;
            j14 ^= j18;
            j15 ^= j18;
            j18 = (j16 ^ j17) & -71777214294589696L;
            j16 ^= j18;
            j17 ^= j18;
            j12 = (j10 ^ j11) & -71777214294589696L;
            j10 ^= j12;
            j11 ^= j12;
            jArr[0] = j;
            jArr[1] = j2;
            jArr[2] = j3;
            jArr[3] = j4;
            jArr[4] = j5;
            jArr[5] = j6;
            jArr[6] = j7;
            jArr[7] = j8;
            jArr[8] = j9;
            jArr[9] = j13;
            jArr[10] = j14;
            jArr[11] = j15;
            jArr[12] = j16;
            jArr[13] = j17;
            jArr[14] = j10;
            jArr[15] = j11;
        } else {
            throw new IllegalStateException("unsupported state size: only 512/1024 are allowed");
        }
    }

    private void subBytes(long[] jArr) {
        for (int i = 0; i < this.columns; i++) {
            long j = jArr[i];
            int i2 = (int) j;
            int i3 = (int) (j >>> 32);
            int i4 = (((S0[i2 & 255] & 255) | ((S1[(i2 >>> 8) & 255] & 255) << 8)) | ((S2[(i2 >>> 16) & 255] & 255) << 16)) | (S3[i2 >>> 24] << 24);
            byte b = S0[i3 & 255];
            byte b2 = S1[(i3 >>> 8) & 255];
            byte b3 = S2[(i3 >>> 16) & 255];
            jArr[i] = (((long) i4) & BodyPartID.bodyIdMax) | (((long) ((S3[i3 >>> 24] << 24) | (((b & 255) | ((b2 & 255) << 8)) | ((b3 & 255) << 16)))) << 32);
        }
    }

    public Memoable copy() {
        return new DSTU7564Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        byte[] bArr2;
        int i2;
        int i3 = this.bufOff;
        byte[] bArr3 = this.buf;
        int i4 = this.bufOff;
        this.bufOff = i4 + 1;
        bArr3[i4] = Byte.MIN_VALUE;
        int i5 = this.blockSize - 12;
        int i6 = 0;
        if (this.bufOff > i5) {
            while (this.bufOff < this.blockSize) {
                bArr2 = this.buf;
                i2 = this.bufOff;
                this.bufOff = i2 + 1;
                bArr2[i2] = (byte) 0;
            }
            this.bufOff = 0;
            processBlock(this.buf, 0);
        }
        while (this.bufOff < i5) {
            bArr2 = this.buf;
            i2 = this.bufOff;
            this.bufOff = i2 + 1;
            bArr2[i2] = (byte) 0;
        }
        long j = (((this.inputBlocks & BodyPartID.bodyIdMax) * ((long) this.blockSize)) + ((long) i3)) << 3;
        Pack.intToLittleEndian((int) j, this.buf, this.bufOff);
        this.bufOff += 4;
        Pack.longToLittleEndian((long) ((int) ((j >>> 32) + (((this.inputBlocks >>> 32) * ((long) this.blockSize)) << 3))), this.buf, this.bufOff);
        processBlock(this.buf, 0);
        System.arraycopy(this.state, 0, this.tempState1, 0, this.columns);
        P(this.tempState1);
        while (i6 < this.columns) {
            long[] jArr = this.state;
            jArr[i6] = jArr[i6] ^ this.tempState1[i6];
            i6++;
        }
        for (i5 = this.columns - (this.hashSize >>> 3); i5 < this.columns; i5++) {
            Pack.longToLittleEndian(this.state[i5], bArr, i);
            i += 8;
        }
        reset();
        return this.hashSize;
    }

    public String getAlgorithmName() {
        return "DSTU7564";
    }

    public int getByteLength() {
        return this.blockSize;
    }

    public int getDigestSize() {
        return this.hashSize;
    }

    public void reset() {
        Arrays.fill(this.state, 0);
        this.state[0] = (long) this.blockSize;
        this.inputBlocks = 0;
        this.bufOff = 0;
    }

    public void reset(Memoable memoable) {
        copyIn((DSTU7564Digest) memoable);
    }

    public void update(byte b) {
        byte[] bArr = this.buf;
        int i = this.bufOff;
        this.bufOff = i + 1;
        bArr[i] = b;
        if (this.bufOff == this.blockSize) {
            processBlock(this.buf, 0);
            this.bufOff = 0;
            this.inputBlocks++;
        }
    }

    public void update(byte[] bArr, int i, int i2) {
        int i3;
        while (this.bufOff != 0 && i2 > 0) {
            i3 = i + 1;
            update(bArr[i]);
            i2--;
            i = i3;
        }
        if (i2 > 0) {
            while (i2 >= this.blockSize) {
                processBlock(bArr, i);
                i += this.blockSize;
                i2 -= this.blockSize;
                this.inputBlocks++;
            }
            while (i2 > 0) {
                i3 = i + 1;
                update(bArr[i]);
                i2--;
                i = i3;
            }
        }
    }
}
