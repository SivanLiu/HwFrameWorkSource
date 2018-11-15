package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class PKCS12ParametersGenerator extends PBEParametersGenerator {
    public static final int IV_MATERIAL = 2;
    public static final int KEY_MATERIAL = 1;
    public static final int MAC_MATERIAL = 3;
    private Digest digest;
    private int u;
    private int v;

    public PKCS12ParametersGenerator(Digest digest) {
        this.digest = digest;
        if (digest instanceof ExtendedDigest) {
            this.u = digest.getDigestSize();
            this.v = ((ExtendedDigest) digest).getByteLength();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Digest ");
        stringBuilder.append(digest.getAlgorithmName());
        stringBuilder.append(" unsupported");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void adjust(byte[] bArr, int i, byte[] bArr2) {
        int i2 = ((bArr2[bArr2.length - 1] & 255) + (bArr[(bArr2.length + i) - 1] & 255)) + 1;
        bArr[(bArr2.length + i) - 1] = (byte) i2;
        i2 >>>= 8;
        for (int length = bArr2.length - 2; length >= 0; length--) {
            int i3 = i + length;
            i2 += (bArr2[length] & 255) + (bArr[i3] & 255);
            bArr[i3] = (byte) i2;
            i2 >>>= 8;
        }
    }

    private byte[] generateDerivedKey(int i, int i2) {
        Object obj;
        Object obj2;
        byte[] bArr = new byte[this.v];
        Object obj3 = new byte[i2];
        for (int i3 = 0; i3 != bArr.length; i3++) {
            bArr[i3] = (byte) i;
        }
        if (this.salt == null || this.salt.length == 0) {
            obj = new byte[0];
        } else {
            obj = new byte[(this.v * (((this.salt.length + this.v) - 1) / this.v))];
            for (int i4 = 0; i4 != obj.length; i4++) {
                obj[i4] = this.salt[i4 % this.salt.length];
            }
        }
        if (this.password == null || this.password.length == 0) {
            obj2 = new byte[0];
        } else {
            obj2 = new byte[(this.v * (((this.password.length + this.v) - 1) / this.v))];
            for (int i5 = 0; i5 != obj2.length; i5++) {
                obj2[i5] = this.password[i5 % this.password.length];
            }
        }
        Object obj4 = new byte[(obj.length + obj2.length)];
        System.arraycopy(obj, 0, obj4, 0, obj.length);
        System.arraycopy(obj2, 0, obj4, obj.length, obj2.length);
        byte[] bArr2 = new byte[this.v];
        i2 = ((i2 + this.u) - 1) / this.u;
        obj2 = new byte[this.u];
        for (int i6 = 1; i6 <= i2; i6++) {
            int i7;
            this.digest.update(bArr, 0, bArr.length);
            this.digest.update(obj4, 0, obj4.length);
            this.digest.doFinal(obj2, 0);
            for (i7 = 1; i7 < this.iterationCount; i7++) {
                this.digest.update(obj2, 0, obj2.length);
                this.digest.doFinal(obj2, 0);
            }
            for (i7 = 0; i7 != bArr2.length; i7++) {
                bArr2[i7] = obj2[i7 % obj2.length];
            }
            for (i7 = 0; i7 != obj4.length / this.v; i7++) {
                adjust(obj4, this.v * i7, bArr2);
            }
            if (i6 == i2) {
                i7 = i6 - 1;
                System.arraycopy(obj2, 0, obj3, this.u * i7, obj3.length - (i7 * this.u));
            } else {
                System.arraycopy(obj2, 0, obj3, (i6 - 1) * this.u, obj2.length);
            }
        }
        return obj3;
    }

    public CipherParameters generateDerivedMacParameters(int i) {
        i /= 8;
        return new KeyParameter(generateDerivedKey(3, i), 0, i);
    }

    public CipherParameters generateDerivedParameters(int i) {
        i /= 8;
        return new KeyParameter(generateDerivedKey(1, i), 0, i);
    }

    public CipherParameters generateDerivedParameters(int i, int i2) {
        i /= 8;
        i2 /= 8;
        byte[] generateDerivedKey = generateDerivedKey(1, i);
        return new ParametersWithIV(new KeyParameter(generateDerivedKey, 0, i), generateDerivedKey(2, i2), 0, i2);
    }
}
