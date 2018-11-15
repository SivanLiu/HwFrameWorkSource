package org.bouncycastle.cert.crmf.bc;

import java.security.SecureRandom;
import org.bouncycastle.cert.crmf.EncryptedValuePadder;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.MGF1BytesGenerator;
import org.bouncycastle.crypto.params.MGFParameters;

public class BcFixedLengthMGF1Padder implements EncryptedValuePadder {
    private Digest dig;
    private int length;
    private SecureRandom random;

    public BcFixedLengthMGF1Padder(int i) {
        this(i, null);
    }

    public BcFixedLengthMGF1Padder(int i, SecureRandom secureRandom) {
        this.dig = new SHA1Digest();
        this.length = i;
        this.random = secureRandom;
    }

    public byte[] getPaddedData(byte[] bArr) {
        Object obj = new byte[this.length];
        Object obj2 = new byte[this.dig.getDigestSize()];
        byte[] bArr2 = new byte[(this.length - this.dig.getDigestSize())];
        if (this.random == null) {
            this.random = new SecureRandom();
        }
        this.random.nextBytes(obj2);
        MGF1BytesGenerator mGF1BytesGenerator = new MGF1BytesGenerator(this.dig);
        mGF1BytesGenerator.init(new MGFParameters(obj2));
        int i = 0;
        mGF1BytesGenerator.generateBytes(bArr2, 0, bArr2.length);
        System.arraycopy(obj2, 0, obj, 0, obj2.length);
        System.arraycopy(bArr, 0, obj, obj2.length, bArr.length);
        for (int length = (obj2.length + bArr.length) + 1; length != obj.length; length++) {
            obj[length] = (byte) (this.random.nextInt(255) + 1);
        }
        while (i != bArr2.length) {
            int length2 = obj2.length + i;
            obj[length2] = (byte) (obj[length2] ^ bArr2[i]);
            i++;
        }
        return obj;
    }

    public byte[] getUnpaddedData(byte[] bArr) {
        Object obj = new byte[this.dig.getDigestSize()];
        byte[] bArr2 = new byte[(this.length - this.dig.getDigestSize())];
        System.arraycopy(bArr, 0, obj, 0, obj.length);
        MGF1BytesGenerator mGF1BytesGenerator = new MGF1BytesGenerator(this.dig);
        mGF1BytesGenerator.init(new MGFParameters(obj));
        mGF1BytesGenerator.generateBytes(bArr2, 0, bArr2.length);
        for (int i = 0; i != bArr2.length; i++) {
            int length = obj.length + i;
            bArr[length] = (byte) (bArr[length] ^ bArr2[i]);
        }
        int length2 = bArr.length;
        while (true) {
            length2--;
            if (length2 == obj.length) {
                length2 = 0;
                break;
            } else if (bArr[length2] == (byte) 0) {
                break;
            }
        }
        if (length2 != 0) {
            Object obj2 = new byte[(length2 - obj.length)];
            System.arraycopy(bArr, obj.length, obj2, 0, obj2.length);
            return obj2;
        }
        throw new IllegalStateException("bad padding in encoding");
    }
}
