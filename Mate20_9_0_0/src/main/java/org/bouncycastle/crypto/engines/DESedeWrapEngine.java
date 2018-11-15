package org.bouncycastle.crypto.engines;

import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.util.Arrays;

public class DESedeWrapEngine implements Wrapper {
    private static final byte[] IV2 = new byte[]{(byte) 74, (byte) -35, (byte) -94, (byte) 44, (byte) 121, (byte) -24, (byte) 33, (byte) 5};
    byte[] digest = new byte[20];
    private CBCBlockCipher engine;
    private boolean forWrapping;
    private byte[] iv;
    private KeyParameter param;
    private ParametersWithIV paramPlusIV;
    Digest sha1 = DigestFactory.createSHA1();

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

    private static byte[] reverse(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length];
        int i = 0;
        while (i < bArr.length) {
            int i2 = i + 1;
            bArr2[i] = bArr[bArr.length - i2];
            i = i2;
        }
        return bArr2;
    }

    public String getAlgorithmName() {
        return "DESede";
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        CipherParameters parameters;
        SecureRandom random;
        this.forWrapping = z;
        this.engine = new CBCBlockCipher(new DESedeEngine());
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            parameters = parametersWithRandom.getParameters();
            random = parametersWithRandom.getRandom();
        } else {
            CipherParameters cipherParameters2 = cipherParameters;
            random = new SecureRandom();
            parameters = cipherParameters2;
        }
        if (parameters instanceof KeyParameter) {
            this.param = (KeyParameter) parameters;
            if (this.forWrapping) {
                this.iv = new byte[8];
                random.nextBytes(this.iv);
                this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
            }
        } else if (parameters instanceof ParametersWithIV) {
            this.paramPlusIV = (ParametersWithIV) parameters;
            this.iv = this.paramPlusIV.getIV();
            this.param = (KeyParameter) this.paramPlusIV.getParameters();
            if (!this.forWrapping) {
                throw new IllegalArgumentException("You should not supply an IV for unwrapping");
            } else if (this.iv == null || this.iv.length != 8) {
                throw new IllegalArgumentException("IV is not 8 octets");
            }
        }
    }

    public byte[] unwrap(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.forWrapping) {
            throw new IllegalStateException("Not set for unwrapping");
        } else if (bArr != null) {
            int blockSize = this.engine.getBlockSize();
            if (i2 % blockSize == 0) {
                this.engine.init(false, new ParametersWithIV(this.param, IV2));
                byte[] bArr2 = new byte[i2];
                for (int i3 = 0; i3 != i2; i3 += blockSize) {
                    this.engine.processBlock(bArr, i + i3, bArr2, i3);
                }
                Object reverse = reverse(bArr2);
                this.iv = new byte[8];
                Object obj = new byte[(reverse.length - 8)];
                System.arraycopy(reverse, 0, this.iv, 0, 8);
                System.arraycopy(reverse, 8, obj, 0, reverse.length - 8);
                this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
                this.engine.init(false, this.paramPlusIV);
                reverse = new byte[obj.length];
                for (int i4 = 0; i4 != reverse.length; i4 += blockSize) {
                    this.engine.processBlock(obj, i4, reverse, i4);
                }
                obj = new byte[(reverse.length - 8)];
                Object obj2 = new byte[8];
                System.arraycopy(reverse, 0, obj, 0, reverse.length - 8);
                System.arraycopy(reverse, reverse.length - 8, obj2, 0, 8);
                if (checkCMSKeyChecksum(obj, obj2)) {
                    return obj;
                }
                throw new InvalidCipherTextException("Checksum inside ciphertext is corrupted");
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ciphertext not multiple of ");
            stringBuilder.append(blockSize);
            throw new InvalidCipherTextException(stringBuilder.toString());
        } else {
            throw new InvalidCipherTextException("Null pointer as ciphertext");
        }
    }

    public byte[] wrap(byte[] bArr, int i, int i2) {
        if (this.forWrapping) {
            Object obj = new byte[i2];
            int i3 = 0;
            System.arraycopy(bArr, i, obj, 0, i2);
            Object calculateCMSKeyChecksum = calculateCMSKeyChecksum(obj);
            Object obj2 = new byte[(obj.length + calculateCMSKeyChecksum.length)];
            System.arraycopy(obj, 0, obj2, 0, obj.length);
            System.arraycopy(calculateCMSKeyChecksum, 0, obj2, obj.length, calculateCMSKeyChecksum.length);
            int blockSize = this.engine.getBlockSize();
            if (obj2.length % blockSize == 0) {
                this.engine.init(true, this.paramPlusIV);
                Object obj3 = new byte[obj2.length];
                for (int i4 = 0; i4 != obj2.length; i4 += blockSize) {
                    this.engine.processBlock(obj2, i4, obj3, i4);
                }
                obj2 = new byte[(this.iv.length + obj3.length)];
                System.arraycopy(this.iv, 0, obj2, 0, this.iv.length);
                System.arraycopy(obj3, 0, obj2, this.iv.length, obj3.length);
                byte[] reverse = reverse(obj2);
                this.engine.init(true, new ParametersWithIV(this.param, IV2));
                while (i3 != reverse.length) {
                    this.engine.processBlock(reverse, i3, reverse, i3);
                    i3 += blockSize;
                }
                return reverse;
            }
            throw new IllegalStateException("Not multiple of block length");
        }
        throw new IllegalStateException("Not initialized for wrapping");
    }
}
