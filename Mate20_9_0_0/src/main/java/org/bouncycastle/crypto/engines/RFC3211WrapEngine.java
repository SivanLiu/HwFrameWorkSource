package org.bouncycastle.crypto.engines;

import java.security.SecureRandom;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;

public class RFC3211WrapEngine implements Wrapper {
    private CBCBlockCipher engine;
    private boolean forWrapping;
    private ParametersWithIV param;
    private SecureRandom rand;

    public RFC3211WrapEngine(BlockCipher blockCipher) {
        this.engine = new CBCBlockCipher(blockCipher);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.engine.getUnderlyingCipher().getAlgorithmName());
        stringBuilder.append("/RFC3211Wrap");
        return stringBuilder.toString();
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forWrapping = z;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.rand = parametersWithRandom.getRandom();
            this.param = (ParametersWithIV) parametersWithRandom.getParameters();
            return;
        }
        if (z) {
            this.rand = new SecureRandom();
        }
        this.param = (ParametersWithIV) cipherParameters;
    }

    public byte[] unwrap(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.forWrapping) {
            throw new IllegalStateException("not set for unwrapping");
        }
        int blockSize = this.engine.getBlockSize();
        if (i2 >= 2 * blockSize) {
            int i3;
            Object obj = new byte[i2];
            Object obj2 = new byte[blockSize];
            int i4 = 0;
            System.arraycopy(bArr, i, obj, 0, i2);
            System.arraycopy(bArr, i, obj2, 0, obj2.length);
            this.engine.init(false, new ParametersWithIV(this.param.getParameters(), obj2));
            for (i3 = blockSize; i3 < obj.length; i3 += blockSize) {
                this.engine.processBlock(obj, i3, obj, i3);
            }
            System.arraycopy(obj, obj.length - obj2.length, obj2, 0, obj2.length);
            this.engine.init(false, new ParametersWithIV(this.param.getParameters(), obj2));
            this.engine.processBlock(obj, 0, obj, 0);
            this.engine.init(false, this.param);
            for (i3 = 0; i3 < obj.length; i3 += blockSize) {
                this.engine.processBlock(obj, i3, obj, i3);
            }
            if ((obj[0] & 255) <= obj.length - 4) {
                Object obj3 = new byte[(obj[0] & 255)];
                System.arraycopy(obj, 4, obj3, 0, obj[0]);
                i = 0;
                while (i4 != 3) {
                    i2 = 1 + i4;
                    i |= ((byte) (~obj[i2])) ^ obj3[i4];
                    i4 = i2;
                }
                if (i == 0) {
                    return obj3;
                }
                throw new InvalidCipherTextException("wrapped key fails checksum");
            }
            throw new InvalidCipherTextException("wrapped key corrupted");
        }
        throw new InvalidCipherTextException("input too short");
    }

    public byte[] wrap(byte[] bArr, int i, int i2) {
        if (this.forWrapping) {
            this.engine.init(true, this.param);
            int blockSize = this.engine.getBlockSize();
            int i3 = i2 + 4;
            int i4 = blockSize * 2;
            if (i3 >= i4) {
                i4 = i3 % blockSize == 0 ? i3 : ((i3 / blockSize) + 1) * blockSize;
            }
            Object obj = new byte[i4];
            int i5 = 0;
            obj[0] = (byte) i2;
            obj[1] = (byte) (~bArr[i]);
            obj[2] = (byte) (~bArr[i + 1]);
            obj[3] = (byte) (~bArr[i + 2]);
            System.arraycopy(bArr, i, obj, 4, i2);
            Object obj2 = new byte[(obj.length - i3)];
            this.rand.nextBytes(obj2);
            System.arraycopy(obj2, 0, obj, i3, obj2.length);
            for (int i6 = 0; i6 < obj.length; i6 += blockSize) {
                this.engine.processBlock(obj, i6, obj, i6);
            }
            while (i5 < obj.length) {
                this.engine.processBlock(obj, i5, obj, i5);
                i5 += blockSize;
            }
            return obj;
        }
        throw new IllegalStateException("not set for wrapping");
    }
}
