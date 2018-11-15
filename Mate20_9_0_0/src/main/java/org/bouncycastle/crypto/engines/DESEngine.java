package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;

public class DESEngine implements BlockCipher {
    protected static final int BLOCK_SIZE = 8;
    private static final int[] SP1 = new int[]{16843776, 0, PKIFailureInfo.notAuthorized, 16843780, 16842756, 66564, 4, PKIFailureInfo.notAuthorized, 1024, 16843776, 16843780, 1024, 16778244, 16842756, 16777216, 4, 1028, 16778240, 16778240, 66560, 66560, 16842752, 16842752, 16778244, 65540, 16777220, 16777220, 65540, 0, 1028, 66564, 16777216, PKIFailureInfo.notAuthorized, 16843780, 4, 16842752, 16843776, 16777216, 16777216, 1024, 16842756, PKIFailureInfo.notAuthorized, 66560, 16777220, 1024, 4, 16778244, 66564, 16843780, 65540, 16842752, 16778244, 16777220, 1028, 66564, 16843776, 1028, 16778240, 16778240, 0, 65540, 66560, 0, 16842756};
    private static final int[] SP2 = new int[]{-2146402272, -2147450880, 32768, 1081376, PKIFailureInfo.badCertTemplate, 32, -2146435040, -2147450848, -2147483616, -2146402272, -2146402304, PKIFailureInfo.systemUnavail, -2147450880, PKIFailureInfo.badCertTemplate, 32, -2146435040, 1081344, 1048608, -2147450848, 0, PKIFailureInfo.systemUnavail, 32768, 1081376, -2146435072, 1048608, -2147483616, 0, 1081344, 32800, -2146402304, -2146435072, 32800, 0, 1081376, -2146435040, PKIFailureInfo.badCertTemplate, -2147450848, -2146435072, -2146402304, 32768, -2146435072, -2147450880, 32, -2146402272, 1081376, 32, 32768, PKIFailureInfo.systemUnavail, 32800, -2146402304, PKIFailureInfo.badCertTemplate, -2147483616, 1048608, -2147450848, -2147483616, 1048608, 1081344, 0, -2147450880, 32800, PKIFailureInfo.systemUnavail, -2146435040, -2146402272, 1081344};
    private static final int[] SP3 = new int[]{520, 134349312, 0, 134348808, 134218240, 0, 131592, 134218240, 131080, 134217736, 134217736, PKIFailureInfo.unsupportedVersion, 134349320, 131080, 134348800, 520, 134217728, 8, 134349312, 512, 131584, 134348800, 134348808, 131592, 134218248, 131584, PKIFailureInfo.unsupportedVersion, 134218248, 8, 134349320, 512, 134217728, 134349312, 134217728, 131080, 520, PKIFailureInfo.unsupportedVersion, 134349312, 134218240, 0, 512, 131080, 134349320, 134218240, 134217736, 512, 0, 134348808, 134218248, PKIFailureInfo.unsupportedVersion, 134217728, 134349320, 8, 131592, 131584, 134217736, 134348800, 134218248, 520, 134348800, 131592, 8, 134348808, 131584};
    private static final int[] SP4 = new int[]{8396801, 8321, 8321, 128, 8396928, 8388737, 8388609, 8193, 0, 8396800, 8396800, 8396929, 129, 0, 8388736, 8388609, 1, PKIFailureInfo.certRevoked, 8388608, 8396801, 128, 8388608, 8193, 8320, 8388737, 1, 8320, 8388736, PKIFailureInfo.certRevoked, 8396928, 8396929, 129, 8388736, 8388609, 8396800, 8396929, 129, 0, 0, 8396800, 8320, 8388736, 8388737, 1, 8396801, 8321, 8321, 128, 8396929, 129, 1, PKIFailureInfo.certRevoked, 8388609, 8193, 8396928, 8388737, 8193, 8320, 8388608, 8396801, 128, 8388608, PKIFailureInfo.certRevoked, 8396928};
    private static final int[] SP5 = new int[]{256, 34078976, 34078720, 1107296512, PKIFailureInfo.signerNotTrusted, 256, 1073741824, 34078720, 1074266368, PKIFailureInfo.signerNotTrusted, 33554688, 1074266368, 1107296512, 1107820544, 524544, 1073741824, 33554432, 1074266112, 1074266112, 0, 1073742080, 1107820800, 1107820800, 33554688, 1107820544, 1073742080, 0, 1107296256, 34078976, 33554432, 1107296256, 524544, PKIFailureInfo.signerNotTrusted, 1107296512, 256, 33554432, 1073741824, 34078720, 1107296512, 1074266368, 33554688, 1073741824, 1107820544, 34078976, 1074266368, 256, 33554432, 1107820544, 1107820800, 524544, 1107296256, 1107820800, 34078720, 0, 1074266112, 1107296256, 524544, 33554688, 1073742080, PKIFailureInfo.signerNotTrusted, 0, 1074266112, 34078976, 1073742080};
    private static final int[] SP6 = new int[]{536870928, 541065216, 16384, 541081616, 541065216, 16, 541081616, 4194304, 536887296, 4210704, 4194304, 536870928, 4194320, 536887296, PKIFailureInfo.duplicateCertReq, 16400, 0, 4194320, 536887312, 16384, 4210688, 536887312, 16, 541065232, 541065232, 0, 4210704, 541081600, 16400, 4210688, 541081600, PKIFailureInfo.duplicateCertReq, 536887296, 16, 541065232, 4210688, 541081616, 4194304, 16400, 536870928, 4194304, 536887296, PKIFailureInfo.duplicateCertReq, 16400, 536870928, 541081616, 4210688, 541065216, 4210704, 541081600, 0, 541065232, 16, 16384, 541065216, 4210704, 16384, 4194320, 536887312, 0, 541081600, PKIFailureInfo.duplicateCertReq, 4194320, 536887312};
    private static final int[] SP7 = new int[]{PKIFailureInfo.badSenderNonce, 69206018, 67110914, 0, 2048, 67110914, 2099202, 69208064, 69208066, PKIFailureInfo.badSenderNonce, 0, 67108866, 2, 67108864, 69206018, 2050, 67110912, 2099202, 2097154, 67110912, 67108866, 69206016, 69208064, 2097154, 69206016, 2048, 2050, 69208066, 2099200, 2, 67108864, 2099200, 67108864, 2099200, PKIFailureInfo.badSenderNonce, 67110914, 67110914, 69206018, 69206018, 2, 2097154, 67108864, 67110912, PKIFailureInfo.badSenderNonce, 69208064, 2050, 2099202, 69208064, 2050, 67108866, 69208066, 69206016, 2099200, 0, 2, 69208066, 0, 2099202, 69206016, 2048, 67108866, 67110912, 2048, 2097154};
    private static final int[] SP8 = new int[]{268439616, PKIFailureInfo.certConfirmed, PKIFailureInfo.transactionIdInUse, 268701760, 268435456, 268439616, 64, 268435456, 262208, 268697600, 268701760, 266240, 268701696, 266304, PKIFailureInfo.certConfirmed, 64, 268697600, 268435520, 268439552, 4160, 266240, 262208, 268697664, 268701696, 4160, 0, 0, 268697664, 268435520, 268439552, 266304, PKIFailureInfo.transactionIdInUse, 266304, PKIFailureInfo.transactionIdInUse, 268701696, PKIFailureInfo.certConfirmed, 64, 268697664, PKIFailureInfo.certConfirmed, 266304, 268439552, 64, 268435520, 268697600, 268697664, 268435456, PKIFailureInfo.transactionIdInUse, 268439616, 0, 268701760, 262208, 268435520, 268697600, 268439552, 268439616, 0, 268701760, 266240, 266240, 4160, 4160, 262208, 268435456, 268701696};
    private static final int[] bigbyte = new int[]{8388608, 4194304, PKIFailureInfo.badSenderNonce, PKIFailureInfo.badCertTemplate, PKIFailureInfo.signerNotTrusted, PKIFailureInfo.transactionIdInUse, PKIFailureInfo.unsupportedVersion, PKIFailureInfo.notAuthorized, 32768, 16384, PKIFailureInfo.certRevoked, PKIFailureInfo.certConfirmed, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1};
    private static final short[] bytebit = new short[]{(short) 128, (short) 64, (short) 32, (short) 16, (short) 8, (short) 4, (short) 2, (short) 1};
    private static final byte[] pc1 = new byte[]{(byte) 56, (byte) 48, (byte) 40, (byte) 32, (byte) 24, Tnaf.POW_2_WIDTH, (byte) 8, (byte) 0, (byte) 57, (byte) 49, (byte) 41, (byte) 33, (byte) 25, (byte) 17, (byte) 9, (byte) 1, (byte) 58, (byte) 50, (byte) 42, (byte) 34, (byte) 26, (byte) 18, (byte) 10, (byte) 2, (byte) 59, (byte) 51, (byte) 43, (byte) 35, (byte) 62, (byte) 54, (byte) 46, (byte) 38, (byte) 30, (byte) 22, (byte) 14, (byte) 6, (byte) 61, (byte) 53, (byte) 45, (byte) 37, (byte) 29, (byte) 21, (byte) 13, (byte) 5, (byte) 60, (byte) 52, (byte) 44, (byte) 36, (byte) 28, (byte) 20, (byte) 12, (byte) 4, (byte) 27, (byte) 19, (byte) 11, (byte) 3};
    private static final byte[] pc2 = new byte[]{(byte) 13, Tnaf.POW_2_WIDTH, (byte) 10, (byte) 23, (byte) 0, (byte) 4, (byte) 2, (byte) 27, (byte) 14, (byte) 5, (byte) 20, (byte) 9, (byte) 22, (byte) 18, (byte) 11, (byte) 3, (byte) 25, (byte) 7, (byte) 15, (byte) 6, (byte) 26, (byte) 19, (byte) 12, (byte) 1, (byte) 40, (byte) 51, (byte) 30, (byte) 36, (byte) 46, (byte) 54, (byte) 29, (byte) 39, (byte) 50, (byte) 44, (byte) 32, (byte) 47, (byte) 43, (byte) 48, (byte) 38, (byte) 55, (byte) 33, (byte) 52, (byte) 45, (byte) 41, (byte) 49, (byte) 35, (byte) 28, (byte) 31};
    private static final byte[] totrot = new byte[]{(byte) 1, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10, (byte) 12, (byte) 14, (byte) 15, (byte) 17, (byte) 19, (byte) 21, (byte) 23, (byte) 25, (byte) 27, (byte) 28};
    private int[] workingKey = null;

    protected void desFunc(int[] iArr, byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3 = ((((bArr[i + 0] & 255) << 24) | ((bArr[i + 1] & 255) << 16)) | ((bArr[i + 2] & 255) << 8)) | (bArr[i + 3] & 255);
        int i4 = (bArr[i + 7] & 255) | ((((bArr[i + 4] & 255) << 24) | ((bArr[i + 5] & 255) << 16)) | ((bArr[i + 6] & 255) << 8));
        i = ((i3 >>> 4) ^ i4) & 252645135;
        i4 ^= i;
        i = (i << 4) ^ i3;
        i3 = ((i >>> 16) ^ i4) & 65535;
        i4 ^= i3;
        i ^= i3 << 16;
        i3 = ((i4 >>> 2) ^ i) & 858993459;
        i ^= i3;
        i4 ^= i3 << 2;
        i3 = ((i4 >>> 8) ^ i) & 16711935;
        i ^= i3;
        i4 ^= i3 << 8;
        i4 = (((i4 >>> 31) & 1) | (i4 << 1)) & -1;
        i3 = (i ^ i4) & -1431655766;
        i ^= i3;
        i4 ^= i3;
        i = (((i >>> 31) & 1) | (i << 1)) & -1;
        for (i3 = 0; i3 < 8; i3++) {
            int i5 = i3 * 4;
            int i6 = ((i4 << 28) | (i4 >>> 4)) ^ iArr[i5 + 0];
            i6 = SP1[(i6 >>> 24) & 63] | ((SP7[i6 & 63] | SP5[(i6 >>> 8) & 63]) | SP3[(i6 >>> 16) & 63]);
            int i7 = iArr[i5 + 1] ^ i4;
            i ^= (((i6 | SP8[i7 & 63]) | SP6[(i7 >>> 8) & 63]) | SP4[(i7 >>> 16) & 63]) | SP2[(i7 >>> 24) & 63];
            i6 = ((i << 28) | (i >>> 4)) ^ iArr[i5 + 2];
            i5 = iArr[i5 + 3] ^ i;
            i4 ^= ((((SP1[(i6 >>> 24) & 63] | ((SP7[i6 & 63] | SP5[(i6 >>> 8) & 63]) | SP3[(i6 >>> 16) & 63])) | SP8[i5 & 63]) | SP6[(i5 >>> 8) & 63]) | SP4[(i5 >>> 16) & 63]) | SP2[(i5 >>> 24) & 63];
        }
        int i8 = (i4 << 31) | (i4 >>> 1);
        i4 = (i ^ i8) & -1431655766;
        i ^= i4;
        i8 ^= i4;
        i4 = (i << 31) | (i >>> 1);
        i = ((i4 >>> 8) ^ i8) & 16711935;
        i8 ^= i;
        i4 ^= i << 8;
        i = ((i4 >>> 2) ^ i8) & 858993459;
        i8 ^= i;
        i4 ^= i << 2;
        i = ((i8 >>> 16) ^ i4) & 65535;
        i4 ^= i;
        i8 ^= i << 16;
        i = ((i8 >>> 4) ^ i4) & 252645135;
        i4 ^= i;
        i8 ^= i << 4;
        bArr2[i2 + 0] = (byte) ((i8 >>> 24) & 255);
        bArr2[i2 + 1] = (byte) ((i8 >>> 16) & 255);
        bArr2[i2 + 2] = (byte) ((i8 >>> 8) & 255);
        bArr2[i2 + 3] = (byte) (i8 & 255);
        bArr2[i2 + 4] = (byte) ((i4 >>> 24) & 255);
        bArr2[i2 + 5] = (byte) ((i4 >>> 16) & 255);
        bArr2[i2 + 6] = (byte) ((i4 >>> 8) & 255);
        bArr2[i2 + 7] = (byte) (i4 & 255);
    }

    protected int[] generateWorkingKey(boolean z, byte[] bArr) {
        int[] iArr = new int[32];
        boolean[] zArr = new boolean[56];
        boolean[] zArr2 = new boolean[56];
        int i = 0;
        int i2 = 0;
        while (true) {
            boolean z2 = true;
            if (i2 >= 56) {
                break;
            }
            byte b = pc1[i2];
            if ((bytebit[b & 7] & bArr[b >>> 3]) == 0) {
                z2 = false;
            }
            zArr[i2] = z2;
            i2++;
        }
        int i3 = 0;
        while (i3 < 16) {
            int i4;
            i2 = z ? i3 << 1 : (15 - i3) << 1;
            int i5 = i2 + 1;
            iArr[i5] = 0;
            iArr[i2] = 0;
            int i6 = 0;
            while (true) {
                i4 = 28;
                if (i6 >= 28) {
                    break;
                }
                int i7 = totrot[i3] + i6;
                if (i7 < 28) {
                    zArr2[i6] = zArr[i7];
                } else {
                    zArr2[i6] = zArr[i7 - 28];
                }
                i6++;
            }
            while (i4 < 56) {
                i6 = totrot[i3] + i4;
                if (i6 < 56) {
                    zArr2[i4] = zArr[i6];
                } else {
                    zArr2[i4] = zArr[i6 - 28];
                }
                i4++;
            }
            for (i6 = 0; i6 < 24; i6++) {
                if (zArr2[pc2[i6]]) {
                    iArr[i2] = iArr[i2] | bigbyte[i6];
                }
                if (zArr2[pc2[i6 + 24]]) {
                    iArr[i5] = iArr[i5] | bigbyte[i6];
                }
            }
            i3++;
        }
        while (i != 32) {
            int i8 = iArr[i];
            i3 = i + 1;
            int i9 = iArr[i3];
            iArr[i] = (((16515072 & i9) >>> 10) | (((i8 & 16515072) << 6) | ((i8 & 4032) << 10))) | ((i9 & 4032) >>> 6);
            iArr[i3] = ((((i8 & 63) << 16) | ((i8 & 258048) << 12)) | ((258048 & i9) >>> 4)) | (i9 & 63);
            i += 2;
        }
        return iArr;
    }

    public String getAlgorithmName() {
        return "DES";
    }

    public int getBlockSize() {
        return 8;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            KeyParameter keyParameter = (KeyParameter) cipherParameters;
            if (keyParameter.getKey().length <= 8) {
                this.workingKey = generateWorkingKey(z, keyParameter.getKey());
                return;
            }
            throw new IllegalArgumentException("DES key too long - should be 8 bytes");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to DES init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("DES engine not initialised");
        } else if (i + 8 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 8 <= bArr2.length) {
            desFunc(this.workingKey, bArr, i, bArr2, i2);
            return 8;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
