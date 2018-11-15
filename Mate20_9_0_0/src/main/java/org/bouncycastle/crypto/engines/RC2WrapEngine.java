package org.bouncycastle.crypto.engines;

import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.util.Arrays;

public class RC2WrapEngine implements Wrapper {
    private static final byte[] IV2 = new byte[]{(byte) 74, (byte) -35, (byte) -94, (byte) 44, (byte) 121, (byte) -24, (byte) 33, (byte) 5};
    byte[] digest = new byte[20];
    private CBCBlockCipher engine;
    private boolean forWrapping;
    private byte[] iv;
    private CipherParameters param;
    private ParametersWithIV paramPlusIV;
    Digest sha1 = DigestFactory.createSHA1();
    private SecureRandom sr;

    private byte[] calculateCMSKeyChecksum(byte[] bArr) {
        Object obj = new byte[8];
        this.sha1.update(bArr, 0, bArr.length);
        this.sha1.doFinal(this.digest, 0);
        System.arraycopy(this.digest, 0, obj, 0, 8);
        return obj;
    }

    private boolean checkCMSKeyChecksum(byte[] bArr, byte[] bArr2) {
        return Arrays.constantTimeAreEqual(calculateCMSKeyChecksum(bArr), bArr2);
    }

    public String getAlgorithmName() {
        return "RC2";
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forWrapping = z;
        this.engine = new CBCBlockCipher(new RC2Engine());
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.sr = parametersWithRandom.getRandom();
            cipherParameters = parametersWithRandom.getParameters();
        } else {
            this.sr = new SecureRandom();
        }
        if (cipherParameters instanceof ParametersWithIV) {
            this.paramPlusIV = (ParametersWithIV) cipherParameters;
            this.iv = this.paramPlusIV.getIV();
            this.param = this.paramPlusIV.getParameters();
            if (!this.forWrapping) {
                throw new IllegalArgumentException("You should not supply an IV for unwrapping");
            } else if (this.iv == null || this.iv.length != 8) {
                throw new IllegalArgumentException("IV is not 8 octets");
            } else {
                return;
            }
        }
        this.param = cipherParameters;
        if (this.forWrapping) {
            this.iv = new byte[8];
            this.sr.nextBytes(this.iv);
            this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
        }
    }

    public byte[] unwrap(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        StringBuilder stringBuilder;
        if (this.forWrapping) {
            throw new IllegalStateException("Not set for unwrapping");
        } else if (bArr == null) {
            throw new InvalidCipherTextException("Null pointer as ciphertext");
        } else if (i2 % this.engine.getBlockSize() == 0) {
            this.engine.init(false, new ParametersWithIV(this.param, IV2));
            Object obj = new byte[i2];
            System.arraycopy(bArr, i, obj, 0, i2);
            for (int i3 = 0; i3 < obj.length / this.engine.getBlockSize(); i3++) {
                i = this.engine.getBlockSize() * i3;
                this.engine.processBlock(obj, i, obj, i);
            }
            Object obj2 = new byte[obj.length];
            i = 0;
            while (i < obj.length) {
                int i4 = i + 1;
                obj2[i] = obj[obj.length - i4];
                i = i4;
            }
            this.iv = new byte[8];
            Object obj3 = new byte[(obj2.length - 8)];
            System.arraycopy(obj2, 0, this.iv, 0, 8);
            System.arraycopy(obj2, 8, obj3, 0, obj2.length - 8);
            this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
            this.engine.init(false, this.paramPlusIV);
            obj2 = new byte[obj3.length];
            System.arraycopy(obj3, 0, obj2, 0, obj3.length);
            for (i2 = 0; i2 < obj2.length / this.engine.getBlockSize(); i2++) {
                int blockSize = this.engine.getBlockSize() * i2;
                this.engine.processBlock(obj2, blockSize, obj2, blockSize);
            }
            obj3 = new byte[(obj2.length - 8)];
            obj = new byte[8];
            System.arraycopy(obj2, 0, obj3, 0, obj2.length - 8);
            System.arraycopy(obj2, obj2.length - 8, obj, 0, 8);
            if (!checkCMSKeyChecksum(obj3, obj)) {
                throw new InvalidCipherTextException("Checksum inside ciphertext is corrupted");
            } else if (obj3.length - ((obj3[0] & 255) + 1) <= 7) {
                obj2 = new byte[obj3[0]];
                System.arraycopy(obj3, 1, obj2, 0, obj2.length);
                return obj2;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("too many pad bytes (");
                stringBuilder.append(obj3.length - ((obj3[0] & 255) + 1));
                stringBuilder.append(")");
                throw new InvalidCipherTextException(stringBuilder.toString());
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ciphertext not multiple of ");
            stringBuilder.append(this.engine.getBlockSize());
            throw new InvalidCipherTextException(stringBuilder.toString());
        }
    }

    public byte[] wrap(byte[] bArr, int i, int i2) {
        if (this.forWrapping) {
            int i3 = i2 + 1;
            int i4 = i3 % 8;
            Object obj = new byte[(i4 != 0 ? (8 - i4) + i3 : i3)];
            int i5 = 0;
            obj[0] = (byte) i2;
            System.arraycopy(bArr, i, obj, 1, i2);
            Object obj2 = new byte[((obj.length - i2) - 1)];
            if (obj2.length > 0) {
                this.sr.nextBytes(obj2);
                System.arraycopy(obj2, 0, obj, i3, obj2.length);
            }
            obj2 = calculateCMSKeyChecksum(obj);
            Object obj3 = new byte[(obj.length + obj2.length)];
            System.arraycopy(obj, 0, obj3, 0, obj.length);
            System.arraycopy(obj2, 0, obj3, obj.length, obj2.length);
            obj2 = new byte[obj3.length];
            System.arraycopy(obj3, 0, obj2, 0, obj3.length);
            i2 = obj3.length / this.engine.getBlockSize();
            if (obj3.length % this.engine.getBlockSize() == 0) {
                this.engine.init(true, this.paramPlusIV);
                for (i = 0; i < i2; i++) {
                    i3 = this.engine.getBlockSize() * i;
                    this.engine.processBlock(obj2, i3, obj2, i3);
                }
                obj3 = new byte[(this.iv.length + obj2.length)];
                System.arraycopy(this.iv, 0, obj3, 0, this.iv.length);
                System.arraycopy(obj2, 0, obj3, this.iv.length, obj2.length);
                bArr = new byte[obj3.length];
                i3 = 0;
                while (i3 < obj3.length) {
                    int i6 = i3 + 1;
                    bArr[i3] = obj3[obj3.length - i6];
                    i3 = i6;
                }
                this.engine.init(true, new ParametersWithIV(this.param, IV2));
                while (i5 < i2 + 1) {
                    i = this.engine.getBlockSize() * i5;
                    this.engine.processBlock(bArr, i, bArr, i);
                    i5++;
                }
                return bArr;
            }
            throw new IllegalStateException("Not multiple of block length");
        }
        throw new IllegalStateException("Not initialized for wrapping");
    }
}
