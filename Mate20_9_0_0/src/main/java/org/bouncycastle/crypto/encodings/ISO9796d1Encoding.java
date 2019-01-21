package org.bouncycastle.crypto.encodings;

import java.math.BigInteger;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;

public class ISO9796d1Encoding implements AsymmetricBlockCipher {
    private static final BigInteger SIX = BigInteger.valueOf(6);
    private static final BigInteger SIXTEEN = BigInteger.valueOf(16);
    private static byte[] inverse = new byte[]{(byte) 8, (byte) 15, (byte) 6, (byte) 1, (byte) 5, (byte) 2, (byte) 11, (byte) 12, (byte) 3, (byte) 4, (byte) 13, (byte) 10, (byte) 14, (byte) 9, (byte) 0, (byte) 7};
    private static byte[] shadows = new byte[]{(byte) 14, (byte) 3, (byte) 5, (byte) 8, (byte) 9, (byte) 4, (byte) 2, (byte) 15, (byte) 0, (byte) 13, (byte) 11, (byte) 6, (byte) 7, (byte) 10, (byte) 12, (byte) 1};
    private int bitSize;
    private AsymmetricBlockCipher engine;
    private boolean forEncryption;
    private BigInteger modulus;
    private int padBits = 0;

    public ISO9796d1Encoding(AsymmetricBlockCipher asymmetricBlockCipher) {
        this.engine = asymmetricBlockCipher;
    }

    private static byte[] convertOutputDecryptOnly(BigInteger bigInteger) {
        byte[] toByteArray = bigInteger.toByteArray();
        if (toByteArray[0] != (byte) 0) {
            return toByteArray;
        }
        byte[] bArr = new byte[(toByteArray.length - 1)];
        System.arraycopy(toByteArray, 1, bArr, 0, bArr.length);
        return bArr;
    }

    private byte[] decodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        bArr = this.engine.processBlock(bArr, i, i2);
        i = (this.bitSize + 13) / 16;
        BigInteger bigInteger = new BigInteger(1, bArr);
        if (!bigInteger.mod(SIXTEEN).equals(SIX)) {
            if (this.modulus.subtract(bigInteger).mod(SIXTEEN).equals(SIX)) {
                bigInteger = this.modulus.subtract(bigInteger);
            } else {
                throw new InvalidCipherTextException("resulting integer iS or (modulus - iS) is not congruent to 6 mod 16");
            }
        }
        bArr = convertOutputDecryptOnly(bigInteger);
        if ((bArr[bArr.length - 1] & 15) == 6) {
            bArr[bArr.length - 1] = (byte) (((bArr[bArr.length - 1] & 255) >>> 4) | (inverse[(bArr[bArr.length - 2] & 255) >> 4] << 4));
            int i3 = 0;
            bArr[0] = (byte) ((shadows[(bArr[1] & 255) >>> 4] << 4) | shadows[bArr[1] & 15]);
            int i4 = 1;
            int i5 = 0;
            int i6 = i5;
            for (i2 = bArr.length - 1; i2 >= bArr.length - (2 * i); i2 -= 2) {
                int i7 = (shadows[(bArr[i2] & 255) >>> 4] << 4) | shadows[bArr[i2] & 15];
                int i8 = i2 - 1;
                if (((bArr[i8] ^ i7) & 255) != 0) {
                    if (i6 == 0) {
                        i6 = 1;
                        i4 = (bArr[i8] ^ i7) & 255;
                        i5 = i8;
                    } else {
                        throw new InvalidCipherTextException("invalid tsums in block");
                    }
                }
            }
            bArr[i5] = (byte) 0;
            byte[] bArr2 = new byte[((bArr.length - i5) / 2)];
            while (i3 < bArr2.length) {
                bArr2[i3] = bArr[((2 * i3) + i5) + 1];
                i3++;
            }
            this.padBits = i4 - 1;
            return bArr2;
        }
        throw new InvalidCipherTextException("invalid forcing byte in block");
    }

    private byte[] encodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        int length;
        byte[] bArr2 = new byte[((this.bitSize + 7) / 8)];
        int i3 = 1;
        int i4 = this.padBits + 1;
        int i5 = (this.bitSize + 13) / 16;
        int i6 = 0;
        while (i6 < i5) {
            if (i6 > i5 - i2) {
                int i7 = i5 - i6;
                System.arraycopy(bArr, (i + i2) - i7, bArr2, bArr2.length - i5, i7);
            } else {
                System.arraycopy(bArr, i, bArr2, bArr2.length - (i6 + i2), i2);
            }
            i6 += i2;
        }
        for (length = bArr2.length - (2 * i5); length != bArr2.length; length += 2) {
            byte b = bArr2[(bArr2.length - i5) + (length / 2)];
            bArr2[length] = (byte) ((shadows[(b & 255) >>> 4] << 4) | shadows[b & 15]);
            bArr2[length + 1] = b;
        }
        length = bArr2.length - (2 * i2);
        bArr2[length] = (byte) (bArr2[length] ^ i4);
        bArr2[bArr2.length - 1] = (byte) ((bArr2[bArr2.length - 1] << 4) | 6);
        length = 8 - ((this.bitSize - 1) % 8);
        if (length != 8) {
            bArr2[0] = (byte) (bArr2[0] & (255 >>> length));
            bArr2[0] = (byte) ((128 >>> length) | bArr2[0]);
            i3 = 0;
        } else {
            bArr2[0] = (byte) 0;
            bArr2[1] = (byte) (bArr2[1] | 128);
        }
        return this.engine.processBlock(bArr2, i3, bArr2.length - i3);
    }

    public int getInputBlockSize() {
        int inputBlockSize = this.engine.getInputBlockSize();
        return this.forEncryption ? (inputBlockSize + 1) / 2 : inputBlockSize;
    }

    public int getOutputBlockSize() {
        int outputBlockSize = this.engine.getOutputBlockSize();
        return this.forEncryption ? outputBlockSize : (outputBlockSize + 1) / 2;
    }

    public int getPadBits() {
        return this.padBits;
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        RSAKeyParameters rSAKeyParameters = cipherParameters instanceof ParametersWithRandom ? (RSAKeyParameters) ((ParametersWithRandom) cipherParameters).getParameters() : (RSAKeyParameters) cipherParameters;
        this.engine.init(z, cipherParameters);
        this.modulus = rSAKeyParameters.getModulus();
        this.bitSize = this.modulus.bitLength();
        this.forEncryption = z;
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        return this.forEncryption ? encodeBlock(bArr, i, i2) : decodeBlock(bArr, i, i2);
    }

    public void setPadBits(int i) {
        if (i <= 7) {
            this.padBits = i;
            return;
        }
        throw new IllegalArgumentException("padBits > 7");
    }
}
