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
        byte[] bArr2 = new byte[8];
        this.sha1.update(bArr, 0, bArr.length);
        this.sha1.doFinal(this.digest, 0);
        System.arraycopy(this.digest, 0, bArr2, 0, 8);
        return bArr2;
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
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, i, bArr2, 0, i2);
            for (int i3 = 0; i3 < bArr2.length / this.engine.getBlockSize(); i3++) {
                i = this.engine.getBlockSize() * i3;
                this.engine.processBlock(bArr2, i, bArr2, i);
            }
            bArr = new byte[bArr2.length];
            i = 0;
            while (i < bArr2.length) {
                int i4 = i + 1;
                bArr[i] = bArr2[bArr2.length - i4];
                i = i4;
            }
            this.iv = new byte[8];
            byte[] bArr3 = new byte[(bArr.length - 8)];
            System.arraycopy(bArr, 0, this.iv, 0, 8);
            System.arraycopy(bArr, 8, bArr3, 0, bArr.length - 8);
            this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
            this.engine.init(false, this.paramPlusIV);
            bArr = new byte[bArr3.length];
            System.arraycopy(bArr3, 0, bArr, 0, bArr3.length);
            for (i2 = 0; i2 < bArr.length / this.engine.getBlockSize(); i2++) {
                int blockSize = this.engine.getBlockSize() * i2;
                this.engine.processBlock(bArr, blockSize, bArr, blockSize);
            }
            bArr3 = new byte[(bArr.length - 8)];
            bArr2 = new byte[8];
            System.arraycopy(bArr, 0, bArr3, 0, bArr.length - 8);
            System.arraycopy(bArr, bArr.length - 8, bArr2, 0, 8);
            if (!checkCMSKeyChecksum(bArr3, bArr2)) {
                throw new InvalidCipherTextException("Checksum inside ciphertext is corrupted");
            } else if (bArr3.length - ((bArr3[0] & 255) + 1) <= 7) {
                bArr = new byte[bArr3[0]];
                System.arraycopy(bArr3, 1, bArr, 0, bArr.length);
                return bArr;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("too many pad bytes (");
                stringBuilder.append(bArr3.length - ((bArr3[0] & 255) + 1));
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
            byte[] bArr2 = new byte[(i4 != 0 ? (8 - i4) + i3 : i3)];
            int i5 = 0;
            bArr2[0] = (byte) i2;
            System.arraycopy(bArr, i, bArr2, 1, i2);
            bArr = new byte[((bArr2.length - i2) - 1)];
            if (bArr.length > 0) {
                this.sr.nextBytes(bArr);
                System.arraycopy(bArr, 0, bArr2, i3, bArr.length);
            }
            bArr = calculateCMSKeyChecksum(bArr2);
            byte[] bArr3 = new byte[(bArr2.length + bArr.length)];
            System.arraycopy(bArr2, 0, bArr3, 0, bArr2.length);
            System.arraycopy(bArr, 0, bArr3, bArr2.length, bArr.length);
            bArr = new byte[bArr3.length];
            System.arraycopy(bArr3, 0, bArr, 0, bArr3.length);
            i2 = bArr3.length / this.engine.getBlockSize();
            if (bArr3.length % this.engine.getBlockSize() == 0) {
                this.engine.init(true, this.paramPlusIV);
                for (i = 0; i < i2; i++) {
                    i3 = this.engine.getBlockSize() * i;
                    this.engine.processBlock(bArr, i3, bArr, i3);
                }
                bArr3 = new byte[(this.iv.length + bArr.length)];
                System.arraycopy(this.iv, 0, bArr3, 0, this.iv.length);
                System.arraycopy(bArr, 0, bArr3, this.iv.length, bArr.length);
                bArr = new byte[bArr3.length];
                i3 = 0;
                while (i3 < bArr3.length) {
                    int i6 = i3 + 1;
                    bArr[i3] = bArr3[bArr3.length - i6];
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
