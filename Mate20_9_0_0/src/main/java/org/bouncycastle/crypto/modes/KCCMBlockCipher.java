package org.bouncycastle.crypto.modes;

import java.io.ByteArrayOutputStream;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;

public class KCCMBlockCipher implements AEADBlockCipher {
    private static final int BITS_IN_BYTE = 8;
    private static final int BYTES_IN_INT = 4;
    private static final int MAX_MAC_BIT_LENGTH = 512;
    private static final int MIN_MAC_BIT_LENGTH = 64;
    private byte[] G1;
    private int Nb_;
    private ExposedByteArrayOutputStream associatedText;
    private byte[] buffer;
    private byte[] counter;
    private ExposedByteArrayOutputStream data;
    private BlockCipher engine;
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private byte[] mac;
    private byte[] macBlock;
    private int macSize;
    private byte[] nonce;
    private byte[] s;

    private class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] getBuffer() {
            return this.buf;
        }
    }

    public KCCMBlockCipher(BlockCipher blockCipher) {
        this(blockCipher, 4);
    }

    public KCCMBlockCipher(BlockCipher blockCipher, int i) {
        this.associatedText = new ExposedByteArrayOutputStream();
        this.data = new ExposedByteArrayOutputStream();
        this.Nb_ = 4;
        this.engine = blockCipher;
        this.macSize = blockCipher.getBlockSize();
        this.nonce = new byte[blockCipher.getBlockSize()];
        this.initialAssociatedText = new byte[blockCipher.getBlockSize()];
        this.mac = new byte[blockCipher.getBlockSize()];
        this.macBlock = new byte[blockCipher.getBlockSize()];
        this.G1 = new byte[blockCipher.getBlockSize()];
        this.buffer = new byte[blockCipher.getBlockSize()];
        this.s = new byte[blockCipher.getBlockSize()];
        this.counter = new byte[blockCipher.getBlockSize()];
        setNb(i);
    }

    private void CalculateMac(byte[] bArr, int i, int i2) {
        while (i2 > 0) {
            for (int i3 = 0; i3 < this.engine.getBlockSize(); i3++) {
                byte[] bArr2 = this.macBlock;
                bArr2[i3] = (byte) (bArr2[i3] ^ bArr[i + i3]);
            }
            this.engine.processBlock(this.macBlock, 0, this.macBlock, 0);
            i2 -= this.engine.getBlockSize();
            i += this.engine.getBlockSize();
        }
    }

    private void ProcessBlock(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        i2 = 0;
        for (int i4 = 0; i4 < this.counter.length; i4++) {
            byte[] bArr3 = this.s;
            bArr3[i4] = (byte) (bArr3[i4] + this.counter[i4]);
        }
        this.engine.processBlock(this.s, 0, this.buffer, 0);
        while (i2 < this.engine.getBlockSize()) {
            bArr2[i3 + i2] = (byte) (this.buffer[i2] ^ bArr[i + i2]);
            i2++;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0045 A:{LOOP_END, LOOP:0: B:22:0x003e->B:24:0x0045} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private byte getFlag(boolean z, int i) {
        String str;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(z ? "1" : "0");
        if (i == 8) {
            str = "010";
        } else if (i == 16) {
            str = "011";
        } else if (i == 32) {
            str = "100";
        } else if (i != 48) {
            if (i == 64) {
                str = "110";
            }
            str = Integer.toBinaryString(this.Nb_ - 1);
            while (str.length() < 4) {
                str = new StringBuffer(str).insert(0, "0").toString();
            }
            stringBuffer.append(str);
            return (byte) Integer.parseInt(stringBuffer.toString(), 2);
        } else {
            str = "101";
        }
        stringBuffer.append(str);
        str = Integer.toBinaryString(this.Nb_ - 1);
        while (str.length() < 4) {
        }
        stringBuffer.append(str);
        return (byte) Integer.parseInt(stringBuffer.toString(), 2);
    }

    private void intToBytes(int i, byte[] bArr, int i2) {
        bArr[i2 + 3] = (byte) (i >> 24);
        bArr[i2 + 2] = (byte) (i >> 16);
        bArr[i2 + 1] = (byte) (i >> 8);
        bArr[i2] = (byte) i;
    }

    private void processAAD(byte[] bArr, int i, int i2, int i3) {
        if (i2 - i < this.engine.getBlockSize()) {
            throw new IllegalArgumentException("authText buffer too short");
        } else if (i2 % this.engine.getBlockSize() == 0) {
            System.arraycopy(this.nonce, 0, this.G1, 0, (this.nonce.length - this.Nb_) - 1);
            intToBytes(i3, this.buffer, 0);
            System.arraycopy(this.buffer, 0, this.G1, (this.nonce.length - this.Nb_) - 1, 4);
            this.G1[this.G1.length - 1] = getFlag(true, this.macSize);
            this.engine.processBlock(this.G1, 0, this.macBlock, 0);
            intToBytes(i2, this.buffer, 0);
            byte[] bArr2;
            if (i2 <= this.engine.getBlockSize() - this.Nb_) {
                for (i3 = 0; i3 < i2; i3++) {
                    bArr2 = this.buffer;
                    int i4 = this.Nb_ + i3;
                    bArr2[i4] = (byte) (bArr2[i4] ^ bArr[i + i3]);
                }
                for (int i5 = 0; i5 < this.engine.getBlockSize(); i5++) {
                    byte[] bArr3 = this.macBlock;
                    bArr3[i5] = (byte) (bArr3[i5] ^ this.buffer[i5]);
                }
                this.engine.processBlock(this.macBlock, 0, this.macBlock, 0);
                return;
            }
            for (i3 = 0; i3 < this.engine.getBlockSize(); i3++) {
                bArr2 = this.macBlock;
                bArr2[i3] = (byte) (bArr2[i3] ^ this.buffer[i3]);
            }
            this.engine.processBlock(this.macBlock, 0, this.macBlock, 0);
            while (i2 != 0) {
                for (i3 = 0; i3 < this.engine.getBlockSize(); i3++) {
                    bArr2 = this.macBlock;
                    bArr2[i3] = (byte) (bArr2[i3] ^ bArr[i3 + i]);
                }
                this.engine.processBlock(this.macBlock, 0, this.macBlock, 0);
                i += this.engine.getBlockSize();
                i2 -= this.engine.getBlockSize();
            }
        } else {
            throw new IllegalArgumentException("padding not supported");
        }
    }

    private void setNb(int i) {
        if (i == 4 || i == 6 || i == 8) {
            this.Nb_ = i;
            return;
        }
        throw new IllegalArgumentException("Nb = 4 is recommended by DSTU7624 but can be changed to only 6 or 8 in this implementation");
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        int processPacket = processPacket(this.data.getBuffer(), 0, this.data.size(), bArr, i);
        reset();
        return processPacket;
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.engine.getAlgorithmName());
        stringBuilder.append("/KCCM");
        return stringBuilder.toString();
    }

    public byte[] getMac() {
        return Arrays.clone(this.mac);
    }

    public int getOutputSize(int i) {
        return i + this.macSize;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    public int getUpdateOutputSize(int i) {
        return i;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            if (aEADParameters.getMacSize() > 512 || aEADParameters.getMacSize() < 64 || aEADParameters.getMacSize() % 8 != 0) {
                throw new IllegalArgumentException("Invalid mac size specified");
            }
            this.nonce = aEADParameters.getNonce();
            this.macSize = aEADParameters.getMacSize() / 8;
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            cipherParameters = aEADParameters.getKey();
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            this.nonce = parametersWithIV.getIV();
            this.macSize = this.engine.getBlockSize();
            this.initialAssociatedText = null;
            cipherParameters = parametersWithIV.getParameters();
        } else {
            throw new IllegalArgumentException("Invalid parameters specified");
        }
        this.mac = new byte[this.macSize];
        this.forEncryption = z;
        this.engine.init(true, cipherParameters);
        this.counter[0] = (byte) 1;
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    public void processAADByte(byte b) {
        this.associatedText.write(b);
    }

    public void processAADBytes(byte[] bArr, int i, int i2) {
        this.associatedText.write(bArr, i, i2);
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        this.data.write(b);
        return 0;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException, IllegalStateException {
        if (bArr.length >= i + i2) {
            this.data.write(bArr, i, i2);
            return 0;
        }
        throw new DataLengthException("input buffer too short");
    }

    public int processPacket(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalStateException, InvalidCipherTextException {
        if (bArr.length - i < i2) {
            throw new DataLengthException("input buffer too short");
        } else if (bArr2.length - i3 >= i2) {
            byte[] buffer;
            if (this.associatedText.size() > 0) {
                int size;
                int size2;
                if (this.forEncryption) {
                    buffer = this.associatedText.getBuffer();
                    size = this.associatedText.size();
                    size2 = this.data.size();
                } else {
                    buffer = this.associatedText.getBuffer();
                    size = this.associatedText.size();
                    size2 = this.data.size() - this.macSize;
                }
                processAAD(buffer, 0, size, size2);
            }
            int i4;
            int i5;
            byte[] bArr3;
            if (this.forEncryption) {
                if (i2 % this.engine.getBlockSize() == 0) {
                    CalculateMac(bArr, i, i2);
                    this.engine.processBlock(this.nonce, 0, this.s, 0);
                    i4 = i3;
                    i3 = i;
                    i = i2;
                    while (i > 0) {
                        ProcessBlock(bArr, i3, i2, bArr2, i4);
                        i -= this.engine.getBlockSize();
                        i3 += this.engine.getBlockSize();
                        i4 += this.engine.getBlockSize();
                    }
                    for (i5 = 0; i5 < this.counter.length; i5++) {
                        bArr3 = this.s;
                        bArr3[i5] = (byte) (bArr3[i5] + this.counter[i5]);
                    }
                    this.engine.processBlock(this.s, 0, this.buffer, 0);
                    for (i5 = 0; i5 < this.macSize; i5++) {
                        bArr2[i4 + i5] = (byte) (this.buffer[i5] ^ this.macBlock[i5]);
                    }
                    System.arraycopy(this.macBlock, 0, this.mac, 0, this.macSize);
                    reset();
                    return i2 + this.macSize;
                }
                throw new DataLengthException("partial blocks not supported");
            } else if ((i2 - this.macSize) % this.engine.getBlockSize() == 0) {
                this.engine.processBlock(this.nonce, 0, this.s, 0);
                i4 = i2 / this.engine.getBlockSize();
                int i6 = i3;
                i3 = i;
                for (i = 0; i < i4; i++) {
                    ProcessBlock(bArr, i3, i2, bArr2, i6);
                    i3 += this.engine.getBlockSize();
                    i6 += this.engine.getBlockSize();
                }
                if (i2 > i3) {
                    for (i = 0; i < this.counter.length; i++) {
                        buffer = this.s;
                        buffer[i] = (byte) (buffer[i] + this.counter[i]);
                    }
                    this.engine.processBlock(this.s, 0, this.buffer, 0);
                    for (i = 0; i < this.macSize; i++) {
                        bArr2[i6 + i] = (byte) (this.buffer[i] ^ bArr[i3 + i]);
                    }
                    i6 += this.macSize;
                }
                for (i5 = 0; i5 < this.counter.length; i5++) {
                    bArr3 = this.s;
                    bArr3[i5] = (byte) (bArr3[i5] + this.counter[i5]);
                }
                this.engine.processBlock(this.s, 0, this.buffer, 0);
                System.arraycopy(bArr2, i6 - this.macSize, this.buffer, 0, this.macSize);
                CalculateMac(bArr2, 0, i6 - this.macSize);
                System.arraycopy(this.macBlock, 0, this.mac, 0, this.macSize);
                bArr = new byte[this.macSize];
                System.arraycopy(this.buffer, 0, bArr, 0, this.macSize);
                if (Arrays.constantTimeAreEqual(this.mac, bArr)) {
                    reset();
                    return i2 - this.macSize;
                }
                throw new InvalidCipherTextException("mac check failed");
            } else {
                throw new DataLengthException("partial blocks not supported");
            }
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
        Arrays.fill(this.G1, (byte) 0);
        Arrays.fill(this.buffer, (byte) 0);
        Arrays.fill(this.counter, (byte) 0);
        Arrays.fill(this.macBlock, (byte) 0);
        this.counter[0] = (byte) 1;
        this.data.reset();
        this.associatedText.reset();
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }
}
