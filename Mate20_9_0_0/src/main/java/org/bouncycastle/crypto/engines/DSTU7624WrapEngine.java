package org.bouncycastle.crypto.engines;

import java.util.ArrayList;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.Arrays;

public class DSTU7624WrapEngine implements Wrapper {
    private static final int BYTES_IN_INTEGER = 4;
    private byte[] B = new byte[(this.engine.getBlockSize() / 2)];
    private ArrayList<byte[]> Btemp = new ArrayList();
    private byte[] checkSumArray = new byte[this.engine.getBlockSize()];
    private DSTU7624Engine engine;
    private boolean forWrapping;
    private byte[] intArray = new byte[4];
    private byte[] zeroArray = new byte[this.engine.getBlockSize()];

    public DSTU7624WrapEngine(int i) {
        this.engine = new DSTU7624Engine(i);
    }

    private void intToBytes(int i, byte[] bArr, int i2) {
        bArr[i2 + 3] = (byte) (i >> 24);
        bArr[i2 + 2] = (byte) (i >> 16);
        bArr[i2 + 1] = (byte) (i >> 8);
        bArr[i2] = (byte) i;
    }

    public String getAlgorithmName() {
        return "DSTU7624WrapEngine";
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof ParametersWithRandom) {
            cipherParameters = ((ParametersWithRandom) cipherParameters).getParameters();
        }
        this.forWrapping = z;
        if (cipherParameters instanceof KeyParameter) {
            this.engine.init(z, cipherParameters);
            return;
        }
        throw new IllegalArgumentException("invalid parameters passed to DSTU7624WrapEngine");
    }

    public byte[] unwrap(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.forWrapping) {
            throw new IllegalStateException("not set for unwrapping");
        } else if (i2 % this.engine.getBlockSize() == 0) {
            int blockSize = (2 * i2) / this.engine.getBlockSize();
            int i3 = blockSize - 1;
            int i4 = i3 * 6;
            Object obj = new byte[i2];
            System.arraycopy(bArr, i, obj, 0, i2);
            Object obj2 = new byte[(this.engine.getBlockSize() / 2)];
            System.arraycopy(obj, 0, obj2, 0, this.engine.getBlockSize() / 2);
            this.Btemp.clear();
            i = obj.length - (this.engine.getBlockSize() / 2);
            i2 = this.engine.getBlockSize() / 2;
            while (i != 0) {
                Object obj3 = new byte[(this.engine.getBlockSize() / 2)];
                System.arraycopy(obj, i2, obj3, 0, this.engine.getBlockSize() / 2);
                this.Btemp.add(obj3);
                i -= this.engine.getBlockSize() / 2;
                i2 += this.engine.getBlockSize() / 2;
            }
            for (i = 0; i < i4; i++) {
                System.arraycopy(this.Btemp.get(blockSize - 2), 0, obj, 0, this.engine.getBlockSize() / 2);
                System.arraycopy(obj2, 0, obj, this.engine.getBlockSize() / 2, this.engine.getBlockSize() / 2);
                intToBytes(i4 - i, this.intArray, 0);
                for (i2 = 0; i2 < 4; i2++) {
                    int blockSize2 = (this.engine.getBlockSize() / 2) + i2;
                    obj[blockSize2] = (byte) (obj[blockSize2] ^ this.intArray[i2]);
                }
                this.engine.processBlock(obj, 0, obj, 0);
                System.arraycopy(obj, 0, obj2, 0, this.engine.getBlockSize() / 2);
                for (i2 = 2; i2 < blockSize; i2++) {
                    int i5 = blockSize - i2;
                    System.arraycopy(this.Btemp.get(i5 - 1), 0, this.Btemp.get(i5), 0, this.engine.getBlockSize() / 2);
                }
                System.arraycopy(obj, this.engine.getBlockSize() / 2, this.Btemp.get(0), 0, this.engine.getBlockSize() / 2);
            }
            System.arraycopy(obj2, 0, obj, 0, this.engine.getBlockSize() / 2);
            i = this.engine.getBlockSize() / 2;
            for (int i6 = 0; i6 < i3; i6++) {
                System.arraycopy(this.Btemp.get(i6), 0, obj, i, this.engine.getBlockSize() / 2);
                i += this.engine.getBlockSize() / 2;
            }
            System.arraycopy(obj, obj.length - this.engine.getBlockSize(), this.checkSumArray, 0, this.engine.getBlockSize());
            obj2 = new byte[(obj.length - this.engine.getBlockSize())];
            if (Arrays.areEqual(this.checkSumArray, this.zeroArray)) {
                System.arraycopy(obj, 0, obj2, 0, obj.length - this.engine.getBlockSize());
                return obj2;
            }
            throw new InvalidCipherTextException("checksum failed");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unwrap data must be a multiple of ");
            stringBuilder.append(this.engine.getBlockSize());
            stringBuilder.append(" bytes");
            throw new DataLengthException(stringBuilder.toString());
        }
    }

    public byte[] wrap(byte[] bArr, int i, int i2) {
        if (!this.forWrapping) {
            throw new IllegalStateException("not set for wrapping");
        } else if (i2 % this.engine.getBlockSize() != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("wrap data must be a multiple of ");
            stringBuilder.append(this.engine.getBlockSize());
            stringBuilder.append(" bytes");
            throw new DataLengthException(stringBuilder.toString());
        } else if (i + i2 <= bArr.length) {
            int blockSize = (1 + (i2 / this.engine.getBlockSize())) * 2;
            int i3 = blockSize - 1;
            int i4 = i3 * 6;
            Object obj = new byte[(this.engine.getBlockSize() + i2)];
            System.arraycopy(bArr, i, obj, 0, i2);
            System.arraycopy(obj, 0, this.B, 0, this.engine.getBlockSize() / 2);
            this.Btemp.clear();
            int length = obj.length - (this.engine.getBlockSize() / 2);
            i = this.engine.getBlockSize() / 2;
            while (length != 0) {
                Object obj2 = new byte[(this.engine.getBlockSize() / 2)];
                System.arraycopy(obj, i, obj2, 0, this.engine.getBlockSize() / 2);
                this.Btemp.add(obj2);
                length -= this.engine.getBlockSize() / 2;
                i += this.engine.getBlockSize() / 2;
            }
            length = 0;
            while (length < i4) {
                System.arraycopy(this.B, 0, obj, 0, this.engine.getBlockSize() / 2);
                System.arraycopy(this.Btemp.get(0), 0, obj, this.engine.getBlockSize() / 2, this.engine.getBlockSize() / 2);
                this.engine.processBlock(obj, 0, obj, 0);
                length++;
                intToBytes(length, this.intArray, 0);
                for (i = 0; i < 4; i++) {
                    i2 = (this.engine.getBlockSize() / 2) + i;
                    obj[i2] = (byte) (obj[i2] ^ this.intArray[i]);
                }
                System.arraycopy(obj, this.engine.getBlockSize() / 2, this.B, 0, this.engine.getBlockSize() / 2);
                for (i = 2; i < blockSize; i++) {
                    System.arraycopy(this.Btemp.get(i - 1), 0, this.Btemp.get(i - 2), 0, this.engine.getBlockSize() / 2);
                }
                System.arraycopy(obj, 0, this.Btemp.get(blockSize - 2), 0, this.engine.getBlockSize() / 2);
            }
            System.arraycopy(this.B, 0, obj, 0, this.engine.getBlockSize() / 2);
            i = this.engine.getBlockSize() / 2;
            for (length = 0; length < i3; length++) {
                System.arraycopy(this.Btemp.get(length), 0, obj, i, this.engine.getBlockSize() / 2);
                i += this.engine.getBlockSize() / 2;
            }
            return obj;
        } else {
            throw new DataLengthException("input buffer too short");
        }
    }
}
