package org.bouncycastle.crypto.agreement.kdf;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KDFParameters;

public class ConcatenationKDFGenerator implements DerivationFunction {
    private Digest digest;
    private int hLen;
    private byte[] otherInfo;
    private byte[] shared;

    public ConcatenationKDFGenerator(Digest digest) {
        this.digest = digest;
        this.hLen = digest.getDigestSize();
    }

    private void ItoOSP(int i, byte[] bArr) {
        bArr[0] = (byte) (i >>> 24);
        bArr[1] = (byte) (i >>> 16);
        bArr[2] = (byte) (i >>> 8);
        bArr[3] = (byte) (i >>> 0);
    }

    public int generateBytes(byte[] bArr, int i, int i2) throws DataLengthException, IllegalArgumentException {
        if (bArr.length - i2 >= i) {
            int i3;
            Object obj = new byte[this.hLen];
            byte[] bArr2 = new byte[4];
            this.digest.reset();
            int i4 = 1;
            if (i2 > this.hLen) {
                int i5;
                i3 = 0;
                while (true) {
                    ItoOSP(i4, bArr2);
                    this.digest.update(bArr2, 0, bArr2.length);
                    this.digest.update(this.shared, 0, this.shared.length);
                    this.digest.update(this.otherInfo, 0, this.otherInfo.length);
                    this.digest.doFinal(obj, 0);
                    System.arraycopy(obj, 0, bArr, i + i3, this.hLen);
                    i3 += this.hLen;
                    i5 = i4 + 1;
                    if (i4 >= i2 / this.hLen) {
                        break;
                    }
                    i4 = i5;
                }
                i4 = i5;
            } else {
                i3 = 0;
            }
            if (i3 < i2) {
                ItoOSP(i4, bArr2);
                this.digest.update(bArr2, 0, bArr2.length);
                this.digest.update(this.shared, 0, this.shared.length);
                this.digest.update(this.otherInfo, 0, this.otherInfo.length);
                this.digest.doFinal(obj, 0);
                System.arraycopy(obj, 0, bArr, i + i3, i2 - i3);
            }
            return i2;
        }
        throw new OutputLengthException("output buffer too small");
    }

    public Digest getDigest() {
        return this.digest;
    }

    public void init(DerivationParameters derivationParameters) {
        if (derivationParameters instanceof KDFParameters) {
            KDFParameters kDFParameters = (KDFParameters) derivationParameters;
            this.shared = kDFParameters.getSharedSecret();
            this.otherInfo = kDFParameters.getIV();
            return;
        }
        throw new IllegalArgumentException("KDF parameters required for generator");
    }
}
