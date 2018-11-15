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
        Object obj = new byte[(length + i)];
        System.arraycopy(bArr, 0, obj, 0, length);
        if (i != 0) {
            System.arraycopy(new byte[i], 0, obj, length, i);
        }
        return obj;
    }

    private byte[] rfc3394UnwrapNoIvCheck(byte[] bArr, int i, int i2) {
        Object obj = bArr;
        int i3 = i;
        byte[] bArr2 = new byte[8];
        Object obj2 = new byte[(i2 - bArr2.length)];
        Object obj3 = new byte[bArr2.length];
        Object obj4 = new byte[(bArr2.length + 8)];
        System.arraycopy(obj, i3, obj3, 0, bArr2.length);
        System.arraycopy(obj, i3 + bArr2.length, obj2, 0, i2 - bArr2.length);
        this.engine.init(false, this.param);
        int i4 = (i2 / 8) - 1;
        for (int i5 = 5; i5 >= 0; i5--) {
            for (int i6 = i4; i6 >= 1; i6--) {
                System.arraycopy(obj3, 0, obj4, 0, bArr2.length);
                int i7 = (i6 - 1) * 8;
                System.arraycopy(obj2, i7, obj4, bArr2.length, 8);
                int i8 = (i4 * i5) + i6;
                int i9 = 1;
                while (i8 != 0) {
                    int length = bArr2.length - i9;
                    obj4[length] = (byte) (obj4[length] ^ ((byte) i8));
                    i8 >>>= 8;
                    i9++;
                }
                this.engine.processBlock(obj4, 0, obj4, 0);
                System.arraycopy(obj4, 0, obj3, 0, 8);
                System.arraycopy(obj4, 8, obj2, i7, 8);
            }
        }
        this.extractedAIV = obj3;
        return obj2;
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
            Object obj2 = new byte[i2];
            System.arraycopy(bArr, i, obj2, 0, i2);
            Object obj3 = new byte[i2];
            if (i3 == 2) {
                this.engine.init(false, this.param);
                int i4 = 0;
                while (i4 < obj2.length) {
                    this.engine.processBlock(obj2, i4, obj3, i4);
                    i4 += this.engine.getBlockSize();
                }
                this.extractedAIV = new byte[8];
                System.arraycopy(obj3, 0, this.extractedAIV, 0, this.extractedAIV.length);
                obj = new byte[(obj3.length - this.extractedAIV.length)];
                System.arraycopy(obj3, this.extractedAIV.length, obj, 0, obj.length);
            } else {
                obj = rfc3394UnwrapNoIvCheck(bArr, i, i2);
            }
            Object obj4 = new byte[4];
            Object obj5 = new byte[4];
            System.arraycopy(this.extractedAIV, 0, obj4, 0, obj4.length);
            System.arraycopy(this.extractedAIV, obj4.length, obj5, 0, obj5.length);
            i = Pack.bigEndianToInt(obj5, 0);
            boolean constantTimeAreEqual = Arrays.constantTimeAreEqual(obj4, this.preIV);
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
            byte[] bArr2 = new byte[i3];
            obj3 = new byte[i3];
            System.arraycopy(obj, obj.length - i3, obj3, 0, i3);
            if (!Arrays.constantTimeAreEqual(obj3, bArr2)) {
                constantTimeAreEqual = false;
            }
            if (constantTimeAreEqual) {
                obj5 = new byte[i];
                System.arraycopy(obj, 0, obj5, 0, obj5.length);
                return obj5;
            }
            throw new InvalidCipherTextException("checksum failed");
        } else {
            throw new InvalidCipherTextException("unwrap data must be at least 16 bytes");
        }
    }

    public byte[] wrap(byte[] bArr, int i, int i2) {
        if (this.forWrapping) {
            Object obj = new byte[8];
            Object intToBigEndian = Pack.intToBigEndian(i2);
            int i3 = 0;
            System.arraycopy(this.preIV, 0, obj, 0, this.preIV.length);
            System.arraycopy(intToBigEndian, 0, obj, this.preIV.length, intToBigEndian.length);
            intToBigEndian = new byte[i2];
            System.arraycopy(bArr, i, intToBigEndian, 0, i2);
            Object padPlaintext = padPlaintext(intToBigEndian);
            if (padPlaintext.length == 8) {
                Object obj2 = new byte[(padPlaintext.length + obj.length)];
                System.arraycopy(obj, 0, obj2, 0, obj.length);
                System.arraycopy(padPlaintext, 0, obj2, obj.length, padPlaintext.length);
                this.engine.init(true, this.param);
                while (i3 < obj2.length) {
                    this.engine.processBlock(obj2, i3, obj2, i3);
                    i3 += this.engine.getBlockSize();
                }
                return obj2;
            }
            Wrapper rFC3394WrapEngine = new RFC3394WrapEngine(this.engine);
            rFC3394WrapEngine.init(true, new ParametersWithIV(this.param, obj));
            return rFC3394WrapEngine.wrap(padPlaintext, 0, padPlaintext.length);
        }
        throw new IllegalStateException("not set for wrapping");
    }
}
