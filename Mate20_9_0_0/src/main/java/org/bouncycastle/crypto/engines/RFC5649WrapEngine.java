package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public class RFC5649WrapEngine implements Wrapper {
    private BlockCipher engine;
    private byte[] extractedAIV = null;
    private boolean forWrapping;
    private byte[] highOrderIV = new byte[]{(byte) -90, (byte) 89, (byte) 89, (byte) -90};
    private KeyParameter param;
    private byte[] preIV = this.highOrderIV;

    public RFC5649WrapEngine(BlockCipher blockCipher) {
        this.engine = blockCipher;
    }

    private byte[] padPlaintext(byte[] bArr) {
        int length = bArr.length;
        int i = (8 - (length % 8)) % 8;
        byte[] bArr2 = new byte[(length + i)];
        System.arraycopy(bArr, 0, bArr2, 0, length);
        if (i != 0) {
            System.arraycopy(new byte[i], 0, bArr2, length, i);
        }
        return bArr2;
    }

    private byte[] rfc3394UnwrapNoIvCheck(byte[] bArr, int i, int i2) {
        Object obj = bArr;
        int i3 = i;
        byte[] bArr2 = new byte[8];
        byte[] bArr3 = new byte[(i2 - bArr2.length)];
        byte[] bArr4 = new byte[bArr2.length];
        byte[] bArr5 = new byte[(bArr2.length + 8)];
        System.arraycopy(obj, i3, bArr4, 0, bArr2.length);
        System.arraycopy(obj, i3 + bArr2.length, bArr3, 0, i2 - bArr2.length);
        this.engine.init(false, this.param);
        int i4 = (i2 / 8) - 1;
        for (int i5 = 5; i5 >= 0; i5--) {
            for (int i6 = i4; i6 >= 1; i6--) {
                System.arraycopy(bArr4, 0, bArr5, 0, bArr2.length);
                int i7 = (i6 - 1) * 8;
                System.arraycopy(bArr3, i7, bArr5, bArr2.length, 8);
                int i8 = (i4 * i5) + i6;
                int i9 = 1;
                while (i8 != 0) {
                    int length = bArr2.length - i9;
                    bArr5[length] = (byte) (bArr5[length] ^ ((byte) i8));
                    i8 >>>= 8;
                    i9++;
                }
                this.engine.processBlock(bArr5, 0, bArr5, 0);
                System.arraycopy(bArr5, 0, bArr4, 0, 8);
                System.arraycopy(bArr5, 8, bArr3, i7, 8);
            }
        }
        this.extractedAIV = bArr4;
        return bArr3;
    }

    public String getAlgorithmName() {
        return this.engine.getAlgorithmName();
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forWrapping = z;
        if (cipherParameters instanceof ParametersWithRandom) {
            cipherParameters = ((ParametersWithRandom) cipherParameters).getParameters();
        }
        if (cipherParameters instanceof KeyParameter) {
            this.param = (KeyParameter) cipherParameters;
            this.preIV = this.highOrderIV;
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            this.preIV = parametersWithIV.getIV();
            this.param = (KeyParameter) parametersWithIV.getParameters();
            if (this.preIV.length != 4) {
                throw new IllegalArgumentException("IV length not equal to 4");
            }
        }
    }

    public byte[] unwrap(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.forWrapping) {
            throw new IllegalStateException("not set for unwrapping");
        }
        int i3 = i2 / 8;
        if (i3 * 8 != i2) {
            throw new InvalidCipherTextException("unwrap data must be a multiple of 8 bytes");
        } else if (i3 != 1) {
            Object obj;
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, i, bArr2, 0, i2);
            byte[] bArr3 = new byte[i2];
            if (i3 == 2) {
                this.engine.init(false, this.param);
                int i4 = 0;
                while (i4 < bArr2.length) {
                    this.engine.processBlock(bArr2, i4, bArr3, i4);
                    i4 += this.engine.getBlockSize();
                }
                this.extractedAIV = new byte[8];
                System.arraycopy(bArr3, 0, this.extractedAIV, 0, this.extractedAIV.length);
                obj = new byte[(bArr3.length - this.extractedAIV.length)];
                System.arraycopy(bArr3, this.extractedAIV.length, obj, 0, obj.length);
            } else {
                obj = rfc3394UnwrapNoIvCheck(bArr, i, i2);
            }
            byte[] bArr4 = new byte[4];
            byte[] bArr5 = new byte[4];
            System.arraycopy(this.extractedAIV, 0, bArr4, 0, bArr4.length);
            System.arraycopy(this.extractedAIV, bArr4.length, bArr5, 0, bArr5.length);
            i = Pack.bigEndianToInt(bArr5, 0);
            boolean constantTimeAreEqual = Arrays.constantTimeAreEqual(bArr4, this.preIV);
            i3 = obj.length;
            if (i <= i3 - 8) {
                constantTimeAreEqual = false;
            }
            if (i > i3) {
                constantTimeAreEqual = false;
            }
            i3 -= i;
            if (i3 >= obj.length) {
                i3 = obj.length;
                constantTimeAreEqual = false;
            }
            bArr2 = new byte[i3];
            bArr3 = new byte[i3];
            System.arraycopy(obj, obj.length - i3, bArr3, 0, i3);
            if (!Arrays.constantTimeAreEqual(bArr3, bArr2)) {
                constantTimeAreEqual = false;
            }
            if (constantTimeAreEqual) {
                bArr5 = new byte[i];
                System.arraycopy(obj, 0, bArr5, 0, bArr5.length);
                return bArr5;
            }
            throw new InvalidCipherTextException("checksum failed");
        } else {
            throw new InvalidCipherTextException("unwrap data must be at least 16 bytes");
        }
    }

    public byte[] wrap(byte[] bArr, int i, int i2) {
        if (this.forWrapping) {
            byte[] bArr2 = new byte[8];
            byte[] intToBigEndian = Pack.intToBigEndian(i2);
            int i3 = 0;
            System.arraycopy(this.preIV, 0, bArr2, 0, this.preIV.length);
            System.arraycopy(intToBigEndian, 0, bArr2, this.preIV.length, intToBigEndian.length);
            intToBigEndian = new byte[i2];
            System.arraycopy(bArr, i, intToBigEndian, 0, i2);
            bArr = padPlaintext(intToBigEndian);
            if (bArr.length == 8) {
                byte[] bArr3 = new byte[(bArr.length + bArr2.length)];
                System.arraycopy(bArr2, 0, bArr3, 0, bArr2.length);
                System.arraycopy(bArr, 0, bArr3, bArr2.length, bArr.length);
                this.engine.init(true, this.param);
                while (i3 < bArr3.length) {
                    this.engine.processBlock(bArr3, i3, bArr3, i3);
                    i3 += this.engine.getBlockSize();
                }
                return bArr3;
            }
            RFC3394WrapEngine rFC3394WrapEngine = new RFC3394WrapEngine(this.engine);
            rFC3394WrapEngine.init(true, new ParametersWithIV(this.param, bArr2));
            return rFC3394WrapEngine.wrap(bArr, 0, bArr.length);
        }
        throw new IllegalStateException("not set for wrapping");
    }
}
