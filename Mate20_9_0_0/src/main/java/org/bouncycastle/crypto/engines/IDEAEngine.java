package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.tls.CipherSuite;

public class IDEAEngine implements BlockCipher {
    private static final int BASE = 65537;
    protected static final int BLOCK_SIZE = 8;
    private static final int MASK = 65535;
    private int[] workingKey = null;

    private int bytesToWord(byte[] bArr, int i) {
        return ((bArr[i] << 8) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB) + (bArr[i + 1] & 255);
    }

    private int[] expandKey(byte[] bArr) {
        int i;
        int[] iArr = new int[52];
        int i2 = 0;
        if (bArr.length < 16) {
            Object obj = new byte[16];
            System.arraycopy(bArr, 0, obj, obj.length - bArr.length, bArr.length);
            bArr = obj;
        }
        while (true) {
            i = 8;
            if (i2 >= 8) {
                break;
            }
            iArr[i2] = bytesToWord(bArr, i2 * 2);
            i2++;
        }
        while (i < 52) {
            int i3 = i & 7;
            if (i3 < 6) {
                iArr[i] = (((iArr[i - 7] & CertificateBody.profileType) << 9) | (iArr[i - 6] >> 7)) & MASK;
            } else if (i3 == 6) {
                iArr[i] = (((iArr[i - 7] & CertificateBody.profileType) << 9) | (iArr[i - 14] >> 7)) & MASK;
            } else {
                iArr[i] = (((iArr[i - 15] & CertificateBody.profileType) << 9) | (iArr[i - 14] >> 7)) & MASK;
            }
            i++;
        }
        return iArr;
    }

    private int[] generateWorkingKey(boolean z, byte[] bArr) {
        return z ? expandKey(bArr) : invertKey(expandKey(bArr));
    }

    private void ideaFunc(int[] iArr, byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToWord = bytesToWord(bArr, i);
        int bytesToWord2 = bytesToWord(bArr, i + 2);
        int bytesToWord3 = bytesToWord(bArr, i + 4);
        int bytesToWord4 = bytesToWord(bArr, i + 6);
        i = 0;
        int i3 = bytesToWord4;
        bytesToWord4 = 0;
        while (i < 8) {
            int i4 = bytesToWord4 + 1;
            bytesToWord4 = mul(bytesToWord, iArr[bytesToWord4]);
            bytesToWord = i4 + 1;
            bytesToWord2 = (bytesToWord2 + iArr[i4]) & MASK;
            int i5 = bytesToWord + 1;
            bytesToWord = (bytesToWord3 + iArr[bytesToWord]) & MASK;
            bytesToWord3 = i5 + 1;
            i3 = mul(i3, iArr[i5]);
            int i6 = bytesToWord2 ^ i3;
            int i7 = bytesToWord3 + 1;
            bytesToWord3 = mul(bytesToWord ^ bytesToWord4, iArr[bytesToWord3]);
            i5 = (i6 + bytesToWord3) & MASK;
            i6 = i7 + 1;
            i5 = mul(i5, iArr[i7]);
            bytesToWord3 = (bytesToWord3 + i5) & MASK;
            i3 ^= bytesToWord3;
            bytesToWord3 ^= bytesToWord2;
            i++;
            bytesToWord2 = bytesToWord ^ i5;
            bytesToWord = bytesToWord4 ^ i5;
            bytesToWord4 = i6;
        }
        i = bytesToWord4 + 1;
        wordToBytes(mul(bytesToWord, iArr[bytesToWord4]), bArr2, i2);
        bytesToWord4 = i + 1;
        wordToBytes(bytesToWord3 + iArr[i], bArr2, i2 + 2);
        i = bytesToWord4 + 1;
        wordToBytes(bytesToWord2 + iArr[bytesToWord4], bArr2, i2 + 4);
        wordToBytes(mul(i3, iArr[i]), bArr2, i2 + 6);
    }

    private int[] invertKey(int[] iArr) {
        int i;
        int[] iArr2 = new int[52];
        int mulInv = mulInv(iArr[0]);
        int i2 = 1;
        int addInv = addInv(iArr[1]);
        int addInv2 = addInv(iArr[2]);
        iArr2[51] = mulInv(iArr[3]);
        iArr2[50] = addInv2;
        iArr2[49] = addInv;
        addInv = 48;
        iArr2[48] = mulInv;
        mulInv = 4;
        while (i2 < 8) {
            addInv2 = mulInv + 1;
            mulInv = iArr[mulInv];
            i = addInv2 + 1;
            addInv--;
            iArr2[addInv] = iArr[addInv2];
            addInv--;
            iArr2[addInv] = mulInv;
            mulInv = i + 1;
            addInv2 = mulInv(iArr[i]);
            i = mulInv + 1;
            mulInv = addInv(iArr[mulInv]);
            int i3 = i + 1;
            i = addInv(iArr[i]);
            int i4 = i3 + 1;
            addInv--;
            iArr2[addInv] = mulInv(iArr[i3]);
            addInv--;
            iArr2[addInv] = mulInv;
            addInv--;
            iArr2[addInv] = i;
            addInv--;
            iArr2[addInv] = addInv2;
            i2++;
            mulInv = i4;
        }
        i2 = mulInv + 1;
        mulInv = iArr[mulInv];
        addInv2 = i2 + 1;
        addInv--;
        iArr2[addInv] = iArr[i2];
        addInv--;
        iArr2[addInv] = mulInv;
        mulInv = addInv2 + 1;
        i2 = mulInv(iArr[addInv2]);
        addInv2 = mulInv + 1;
        mulInv = addInv(iArr[mulInv]);
        i = addInv2 + 1;
        addInv2 = addInv(iArr[addInv2]);
        addInv--;
        iArr2[addInv] = mulInv(iArr[i]);
        addInv--;
        iArr2[addInv] = addInv2;
        addInv--;
        iArr2[addInv] = mulInv;
        iArr2[addInv - 1] = i2;
        return iArr2;
    }

    private int mul(int i, int i2) {
        int i3;
        if (i == 0) {
            i3 = BASE - i2;
        } else if (i2 == 0) {
            i3 = BASE - i;
        } else {
            i *= i2;
            i2 = i & MASK;
            i >>>= 16;
            i3 = (i2 - i) + (i2 < i ? 1 : 0);
        }
        return i3 & MASK;
    }

    private int mulInv(int i) {
        if (i < 2) {
            return i;
        }
        int i2 = BASE % i;
        int i3 = BASE / i;
        int i4 = 1;
        while (i2 != 1) {
            int i5 = i / i2;
            i %= i2;
            i4 = (i4 + (i5 * i3)) & MASK;
            if (i == 1) {
                return i4;
            }
            i5 = i2 / i;
            i2 %= i;
            i3 = (i3 + (i5 * i4)) & MASK;
        }
        return (1 - i3) & MASK;
    }

    private void wordToBytes(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) (i >>> 8);
        bArr[i2 + 1] = (byte) i;
    }

    int addInv(int i) {
        return (0 - i) & MASK;
    }

    public String getAlgorithmName() {
        return "IDEA";
    }

    public int getBlockSize() {
        return 8;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.workingKey = generateWorkingKey(z, ((KeyParameter) cipherParameters).getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to IDEA init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("IDEA engine not initialised");
        } else if (i + 8 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 8 <= bArr2.length) {
            ideaFunc(this.workingKey, bArr, i, bArr2, i2);
            return 8;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
