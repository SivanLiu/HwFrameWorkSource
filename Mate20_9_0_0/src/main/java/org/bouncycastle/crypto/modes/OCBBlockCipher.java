package org.bouncycastle.crypto.modes;

import java.util.Vector;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;

public class OCBBlockCipher implements AEADBlockCipher {
    private static final int BLOCK_SIZE = 16;
    private byte[] Checksum;
    private byte[] KtopInput = null;
    private Vector L;
    private byte[] L_Asterisk;
    private byte[] L_Dollar;
    private byte[] OffsetHASH;
    private byte[] OffsetMAIN = new byte[16];
    private byte[] OffsetMAIN_0 = new byte[16];
    private byte[] Stretch = new byte[24];
    private byte[] Sum;
    private boolean forEncryption;
    private byte[] hashBlock;
    private long hashBlockCount;
    private int hashBlockPos;
    private BlockCipher hashCipher;
    private byte[] initialAssociatedText;
    private byte[] macBlock;
    private int macSize;
    private byte[] mainBlock;
    private long mainBlockCount;
    private int mainBlockPos;
    private BlockCipher mainCipher;

    public OCBBlockCipher(BlockCipher blockCipher, BlockCipher blockCipher2) {
        if (blockCipher == null) {
            throw new IllegalArgumentException("'hashCipher' cannot be null");
        } else if (blockCipher.getBlockSize() != 16) {
            throw new IllegalArgumentException("'hashCipher' must have a block size of 16");
        } else if (blockCipher2 == null) {
            throw new IllegalArgumentException("'mainCipher' cannot be null");
        } else if (blockCipher2.getBlockSize() != 16) {
            throw new IllegalArgumentException("'mainCipher' must have a block size of 16");
        } else if (blockCipher.getAlgorithmName().equals(blockCipher2.getAlgorithmName())) {
            this.hashCipher = blockCipher;
            this.mainCipher = blockCipher2;
        } else {
            throw new IllegalArgumentException("'hashCipher' and 'mainCipher' must be the same algorithm");
        }
    }

    protected static byte[] OCB_double(byte[] bArr) {
        byte[] bArr2 = new byte[16];
        bArr2[15] = (byte) ((CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA >>> ((1 - shiftLeft(bArr, bArr2)) << 3)) ^ bArr2[15]);
        return bArr2;
    }

    protected static void OCB_extend(byte[] bArr, int i) {
        bArr[i] = Byte.MIN_VALUE;
        while (true) {
            i++;
            if (i < 16) {
                bArr[i] = (byte) 0;
            } else {
                return;
            }
        }
    }

    protected static int OCB_ntz(long j) {
        if (j == 0) {
            return 64;
        }
        int i = 0;
        while ((1 & j) == 0) {
            i++;
            j >>>= 1;
        }
        return i;
    }

    protected static int shiftLeft(byte[] bArr, byte[] bArr2) {
        int i = 16;
        int i2 = 0;
        while (true) {
            i--;
            if (i < 0) {
                return i2;
            }
            int i3 = bArr[i] & 255;
            bArr2[i] = (byte) (i2 | (i3 << 1));
            i2 = (i3 >>> 7) & 1;
        }
    }

    protected static void xor(byte[] bArr, byte[] bArr2) {
        for (int i = 15; i >= 0; i--) {
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
        }
    }

    protected void clear(byte[] bArr) {
        if (bArr != null) {
            Arrays.fill(bArr, (byte) 0);
        }
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        byte[] bArr2;
        if (this.forEncryption) {
            bArr2 = null;
        } else if (this.mainBlockPos >= this.macSize) {
            this.mainBlockPos -= this.macSize;
            bArr2 = new byte[this.macSize];
            System.arraycopy(this.mainBlock, this.mainBlockPos, bArr2, 0, this.macSize);
        } else {
            throw new InvalidCipherTextException("data too short");
        }
        if (this.hashBlockPos > 0) {
            OCB_extend(this.hashBlock, this.hashBlockPos);
            updateHASH(this.L_Asterisk);
        }
        if (this.mainBlockPos > 0) {
            if (this.forEncryption) {
                OCB_extend(this.mainBlock, this.mainBlockPos);
                xor(this.Checksum, this.mainBlock);
            }
            xor(this.OffsetMAIN, this.L_Asterisk);
            byte[] bArr3 = new byte[16];
            this.hashCipher.processBlock(this.OffsetMAIN, 0, bArr3, 0);
            xor(this.mainBlock, bArr3);
            if (bArr.length >= this.mainBlockPos + i) {
                System.arraycopy(this.mainBlock, 0, bArr, i, this.mainBlockPos);
                if (!this.forEncryption) {
                    OCB_extend(this.mainBlock, this.mainBlockPos);
                    xor(this.Checksum, this.mainBlock);
                }
            } else {
                throw new OutputLengthException("Output buffer too short");
            }
        }
        xor(this.Checksum, this.OffsetMAIN);
        xor(this.Checksum, this.L_Dollar);
        this.hashCipher.processBlock(this.Checksum, 0, this.Checksum, 0);
        xor(this.Checksum, this.Sum);
        this.macBlock = new byte[this.macSize];
        System.arraycopy(this.Checksum, 0, this.macBlock, 0, this.macSize);
        int i2 = this.mainBlockPos;
        if (this.forEncryption) {
            i += i2;
            if (bArr.length >= this.macSize + i) {
                System.arraycopy(this.macBlock, 0, bArr, i, this.macSize);
                i2 += this.macSize;
            } else {
                throw new OutputLengthException("Output buffer too short");
            }
        } else if (!Arrays.constantTimeAreEqual(this.macBlock, bArr2)) {
            throw new InvalidCipherTextException("mac check in OCB failed");
        }
        reset(false);
        return i2;
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mainCipher.getAlgorithmName());
        stringBuilder.append("/OCB");
        return stringBuilder.toString();
    }

    protected byte[] getLSub(int i) {
        while (i >= this.L.size()) {
            this.L.addElement(OCB_double((byte[]) this.L.lastElement()));
        }
        return (byte[]) this.L.elementAt(i);
    }

    public byte[] getMac() {
        return this.macBlock == null ? new byte[this.macSize] : Arrays.clone(this.macBlock);
    }

    public int getOutputSize(int i) {
        i += this.mainBlockPos;
        return this.forEncryption ? i + this.macSize : i < this.macSize ? 0 : i - this.macSize;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.mainCipher;
    }

    public int getUpdateOutputSize(int i) {
        i += this.mainBlockPos;
        if (!this.forEncryption) {
            if (i < this.macSize) {
                return 0;
            }
            i -= this.macSize;
        }
        return i - (i % 16);
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        byte[] nonce;
        boolean z2 = this.forEncryption;
        this.forEncryption = z;
        this.macBlock = null;
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            nonce = aEADParameters.getNonce();
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            int macSize = aEADParameters.getMacSize();
            if (macSize < 64 || macSize > 128 || macSize % 8 != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid value for MAC size: ");
                stringBuilder.append(macSize);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.macSize = macSize / 8;
            cipherParameters = aEADParameters.getKey();
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            nonce = parametersWithIV.getIV();
            this.initialAssociatedText = null;
            this.macSize = 16;
            cipherParameters = (KeyParameter) parametersWithIV.getParameters();
        } else {
            throw new IllegalArgumentException("invalid parameters passed to OCB");
        }
        this.hashBlock = new byte[16];
        this.mainBlock = new byte[(z ? 16 : this.macSize + 16)];
        if (nonce == null) {
            nonce = new byte[0];
        }
        if (nonce.length <= 15) {
            if (cipherParameters != null) {
                this.hashCipher.init(true, cipherParameters);
                this.mainCipher.init(z, cipherParameters);
                this.KtopInput = null;
            } else if (z2 != z) {
                throw new IllegalArgumentException("cannot change encrypting state without providing key.");
            }
            this.L_Asterisk = new byte[16];
            this.hashCipher.processBlock(this.L_Asterisk, 0, this.L_Asterisk, 0);
            this.L_Dollar = OCB_double(this.L_Asterisk);
            this.L = new Vector();
            this.L.addElement(OCB_double(this.L_Dollar));
            int processNonce = processNonce(nonce);
            int i = processNonce % 8;
            processNonce /= 8;
            if (i == 0) {
                System.arraycopy(this.Stretch, processNonce, this.OffsetMAIN_0, 0, 16);
            } else {
                int i2 = processNonce;
                for (processNonce = 0; processNonce < 16; processNonce++) {
                    int i3 = this.Stretch[i2] & 255;
                    i2++;
                    this.OffsetMAIN_0[processNonce] = (byte) ((i3 << i) | ((this.Stretch[i2] & 255) >>> (8 - i)));
                }
            }
            this.hashBlockPos = 0;
            this.mainBlockPos = 0;
            this.hashBlockCount = 0;
            this.mainBlockCount = 0;
            this.OffsetHASH = new byte[16];
            this.Sum = new byte[16];
            System.arraycopy(this.OffsetMAIN_0, 0, this.OffsetMAIN, 0, 16);
            this.Checksum = new byte[16];
            if (this.initialAssociatedText != null) {
                processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("IV must be no more than 15 bytes");
    }

    public void processAADByte(byte b) {
        this.hashBlock[this.hashBlockPos] = b;
        int i = this.hashBlockPos + 1;
        this.hashBlockPos = i;
        if (i == this.hashBlock.length) {
            processHashBlock();
        }
    }

    public void processAADBytes(byte[] bArr, int i, int i2) {
        for (int i3 = 0; i3 < i2; i3++) {
            this.hashBlock[this.hashBlockPos] = bArr[i + i3];
            int i4 = this.hashBlockPos + 1;
            this.hashBlockPos = i4;
            if (i4 == this.hashBlock.length) {
                processHashBlock();
            }
        }
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
        this.mainBlock[this.mainBlockPos] = b;
        int i2 = this.mainBlockPos + 1;
        this.mainBlockPos = i2;
        if (i2 != this.mainBlock.length) {
            return 0;
        }
        processMainBlock(bArr, i);
        return 16;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
        if (bArr.length >= i + i2) {
            int i4 = 0;
            int i5 = 0;
            while (i4 < i2) {
                this.mainBlock[this.mainBlockPos] = bArr[i + i4];
                int i6 = this.mainBlockPos + 1;
                this.mainBlockPos = i6;
                if (i6 == this.mainBlock.length) {
                    processMainBlock(bArr2, i3 + i5);
                    i5 += 16;
                }
                i4++;
            }
            return i5;
        }
        throw new DataLengthException("Input buffer too short");
    }

    protected void processHashBlock() {
        long j = this.hashBlockCount + 1;
        this.hashBlockCount = j;
        updateHASH(getLSub(OCB_ntz(j)));
        this.hashBlockPos = 0;
    }

    protected void processMainBlock(byte[] bArr, int i) {
        if (bArr.length >= i + 16) {
            if (this.forEncryption) {
                xor(this.Checksum, this.mainBlock);
                this.mainBlockPos = 0;
            }
            byte[] bArr2 = this.OffsetMAIN;
            long j = this.mainBlockCount + 1;
            this.mainBlockCount = j;
            xor(bArr2, getLSub(OCB_ntz(j)));
            xor(this.mainBlock, this.OffsetMAIN);
            this.mainCipher.processBlock(this.mainBlock, 0, this.mainBlock, 0);
            xor(this.mainBlock, this.OffsetMAIN);
            System.arraycopy(this.mainBlock, 0, bArr, i, 16);
            if (!this.forEncryption) {
                xor(this.Checksum, this.mainBlock);
                System.arraycopy(this.mainBlock, 16, this.mainBlock, 0, this.macSize);
                this.mainBlockPos = this.macSize;
                return;
            }
            return;
        }
        throw new OutputLengthException("Output buffer too short");
    }

    protected int processNonce(byte[] bArr) {
        byte[] bArr2 = new byte[16];
        int i = 0;
        System.arraycopy(bArr, 0, bArr2, bArr2.length - bArr.length, bArr.length);
        bArr2[0] = (byte) (this.macSize << 4);
        int length = 15 - bArr.length;
        bArr2[length] = (byte) (bArr2[length] | 1);
        length = bArr2[15] & 63;
        bArr2[15] = (byte) (bArr2[15] & 192);
        if (this.KtopInput == null || !Arrays.areEqual(bArr2, this.KtopInput)) {
            Object obj = new byte[16];
            this.KtopInput = bArr2;
            this.hashCipher.processBlock(this.KtopInput, 0, obj, 0);
            System.arraycopy(obj, 0, this.Stretch, 0, 16);
            while (i < 8) {
                bArr2 = this.Stretch;
                int i2 = 16 + i;
                byte b = obj[i];
                i++;
                bArr2[i2] = (byte) (b ^ obj[i]);
            }
        }
        return length;
    }

    public void reset() {
        reset(true);
    }

    protected void reset(boolean z) {
        this.hashCipher.reset();
        this.mainCipher.reset();
        clear(this.hashBlock);
        clear(this.mainBlock);
        this.hashBlockPos = 0;
        this.mainBlockPos = 0;
        this.hashBlockCount = 0;
        this.mainBlockCount = 0;
        clear(this.OffsetHASH);
        clear(this.Sum);
        System.arraycopy(this.OffsetMAIN_0, 0, this.OffsetMAIN, 0, 16);
        clear(this.Checksum);
        if (z) {
            this.macBlock = null;
        }
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    protected void updateHASH(byte[] bArr) {
        xor(this.OffsetHASH, bArr);
        xor(this.hashBlock, this.OffsetHASH);
        this.hashCipher.processBlock(this.hashBlock, 0, this.hashBlock, 0);
        xor(this.Sum, this.hashBlock);
    }
}
