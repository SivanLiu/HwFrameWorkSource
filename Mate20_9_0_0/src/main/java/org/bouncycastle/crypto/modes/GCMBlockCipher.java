package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.modes.gcm.BasicGCMExponentiator;
import org.bouncycastle.crypto.modes.gcm.GCMExponentiator;
import org.bouncycastle.crypto.modes.gcm.GCMMultiplier;
import org.bouncycastle.crypto.modes.gcm.GCMUtil;
import org.bouncycastle.crypto.modes.gcm.Tables4kGCMMultiplier;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public class GCMBlockCipher implements AEADBlockCipher {
    private static final int BLOCK_SIZE = 16;
    private byte[] H;
    private byte[] J0;
    private byte[] S;
    private byte[] S_at;
    private byte[] S_atPre;
    private byte[] atBlock;
    private int atBlockPos;
    private long atLength;
    private long atLengthPre;
    private int blocksRemaining;
    private byte[] bufBlock;
    private int bufOff;
    private BlockCipher cipher;
    private byte[] counter;
    private GCMExponentiator exp;
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private boolean initialised;
    private byte[] lastKey;
    private byte[] macBlock;
    private int macSize;
    private GCMMultiplier multiplier;
    private byte[] nonce;
    private long totalLength;

    public GCMBlockCipher(BlockCipher blockCipher) {
        this(blockCipher, null);
    }

    public GCMBlockCipher(BlockCipher blockCipher, GCMMultiplier gCMMultiplier) {
        if (blockCipher.getBlockSize() == 16) {
            if (gCMMultiplier == null) {
                gCMMultiplier = new Tables4kGCMMultiplier();
            }
            this.cipher = blockCipher;
            this.multiplier = gCMMultiplier;
            return;
        }
        throw new IllegalArgumentException("cipher required with a block size of 16.");
    }

    private void checkStatus() {
        if (!this.initialised) {
            if (this.forEncryption) {
                throw new IllegalStateException("GCM cipher cannot be reused for encryption");
            }
            throw new IllegalStateException("GCM cipher needs to be initialised");
        }
    }

    private void gHASH(byte[] bArr, byte[] bArr2, int i) {
        for (int i2 = 0; i2 < i; i2 += 16) {
            gHASHPartial(bArr, bArr2, i2, Math.min(i - i2, 16));
        }
    }

    private void gHASHBlock(byte[] bArr, byte[] bArr2) {
        GCMUtil.xor(bArr, bArr2);
        this.multiplier.multiplyH(bArr);
    }

    private void gHASHBlock(byte[] bArr, byte[] bArr2, int i) {
        GCMUtil.xor(bArr, bArr2, i);
        this.multiplier.multiplyH(bArr);
    }

    private void gHASHPartial(byte[] bArr, byte[] bArr2, int i, int i2) {
        GCMUtil.xor(bArr, bArr2, i, i2);
        this.multiplier.multiplyH(bArr);
    }

    private void getNextCTRBlock(byte[] bArr) {
        if (this.blocksRemaining != 0) {
            this.blocksRemaining--;
            int i = 1 + (this.counter[15] & 255);
            this.counter[15] = (byte) i;
            int i2 = (i >>> 8) + (this.counter[14] & 255);
            this.counter[14] = (byte) i2;
            i2 = (i2 >>> 8) + (this.counter[13] & 255);
            this.counter[13] = (byte) i2;
            this.counter[12] = (byte) ((i2 >>> 8) + (this.counter[12] & 255));
            this.cipher.processBlock(this.counter, 0, bArr, 0);
            return;
        }
        throw new IllegalStateException("Attempt to process too many blocks");
    }

    private void initCipher() {
        if (this.atLength > 0) {
            System.arraycopy(this.S_at, 0, this.S_atPre, 0, 16);
            this.atLengthPre = this.atLength;
        }
        if (this.atBlockPos > 0) {
            gHASHPartial(this.S_atPre, this.atBlock, 0, this.atBlockPos);
            this.atLengthPre += (long) this.atBlockPos;
        }
        if (this.atLengthPre > 0) {
            System.arraycopy(this.S_atPre, 0, this.S, 0, 16);
        }
    }

    private void processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (bArr2.length - i2 >= 16) {
            if (this.totalLength == 0) {
                initCipher();
            }
            byte[] bArr3 = new byte[16];
            getNextCTRBlock(bArr3);
            if (this.forEncryption) {
                GCMUtil.xor(bArr3, bArr, i);
                gHASHBlock(this.S, bArr3);
                System.arraycopy(bArr3, 0, bArr2, i2, 16);
            } else {
                gHASHBlock(this.S, bArr, i);
                GCMUtil.xor(bArr3, 0, bArr, i, bArr2, i2);
            }
            this.totalLength += 16;
            return;
        }
        throw new OutputLengthException("Output buffer too short");
    }

    private void processPartial(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        byte[] bArr3 = new byte[16];
        getNextCTRBlock(bArr3);
        if (this.forEncryption) {
            GCMUtil.xor(bArr, i, bArr3, 0, i2);
            gHASHPartial(this.S, bArr, i, i2);
        } else {
            gHASHPartial(this.S, bArr, i, i2);
            GCMUtil.xor(bArr, i, bArr3, 0, i2);
        }
        System.arraycopy(bArr, i, bArr2, i3, i2);
        this.totalLength += (long) i2;
    }

    private void reset(boolean z) {
        this.cipher.reset();
        this.S = new byte[16];
        this.S_at = new byte[16];
        this.S_atPre = new byte[16];
        this.atBlock = new byte[16];
        this.atBlockPos = 0;
        this.atLength = 0;
        this.atLengthPre = 0;
        this.counter = Arrays.clone(this.J0);
        this.blocksRemaining = -2;
        this.bufOff = 0;
        this.totalLength = 0;
        if (this.bufBlock != null) {
            Arrays.fill(this.bufBlock, (byte) 0);
        }
        if (z) {
            this.macBlock = null;
        }
        if (this.forEncryption) {
            this.initialised = false;
            return;
        }
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        checkStatus();
        if (this.totalLength == 0) {
            initCipher();
        }
        int i2 = this.bufOff;
        if (this.forEncryption) {
            if (bArr.length - i < this.macSize + i2) {
                throw new OutputLengthException("Output buffer too short");
            }
        } else if (i2 >= this.macSize) {
            i2 -= this.macSize;
            if (bArr.length - i < i2) {
                throw new OutputLengthException("Output buffer too short");
            }
        } else {
            throw new InvalidCipherTextException("data too short");
        }
        if (i2 > 0) {
            processPartial(this.bufBlock, 0, i2, bArr, i);
        }
        this.atLength += (long) this.atBlockPos;
        if (this.atLength > this.atLengthPre) {
            if (this.atBlockPos > 0) {
                gHASHPartial(this.S_at, this.atBlock, 0, this.atBlockPos);
            }
            if (this.atLengthPre > 0) {
                GCMUtil.xor(this.S_at, this.S_atPre);
            }
            long j = ((this.totalLength * 8) + 127) >>> 7;
            byte[] bArr2 = new byte[16];
            if (this.exp == null) {
                this.exp = new BasicGCMExponentiator();
                this.exp.init(this.H);
            }
            this.exp.exponentiateX(j, bArr2);
            GCMUtil.multiply(this.S_at, bArr2);
            GCMUtil.xor(this.S, this.S_at);
        }
        byte[] bArr3 = new byte[16];
        Pack.longToBigEndian(this.atLength * 8, bArr3, 0);
        Pack.longToBigEndian(this.totalLength * 8, bArr3, 8);
        gHASHBlock(this.S, bArr3);
        bArr3 = new byte[16];
        this.cipher.processBlock(this.J0, 0, bArr3, 0);
        GCMUtil.xor(bArr3, this.S);
        this.macBlock = new byte[this.macSize];
        System.arraycopy(bArr3, 0, this.macBlock, 0, this.macSize);
        if (this.forEncryption) {
            System.arraycopy(this.macBlock, 0, bArr, i + this.bufOff, this.macSize);
            i2 += this.macSize;
        } else {
            bArr = new byte[this.macSize];
            System.arraycopy(this.bufBlock, i2, bArr, 0, this.macSize);
            if (!Arrays.constantTimeAreEqual(this.macBlock, bArr)) {
                throw new InvalidCipherTextException("mac check in GCM failed");
            }
        }
        reset(false);
        return i2;
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cipher.getAlgorithmName());
        stringBuilder.append("/GCM");
        return stringBuilder.toString();
    }

    public byte[] getMac() {
        return this.macBlock == null ? new byte[this.macSize] : Arrays.clone(this.macBlock);
    }

    public int getOutputSize(int i) {
        i += this.bufOff;
        return this.forEncryption ? i + this.macSize : i < this.macSize ? 0 : i - this.macSize;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public int getUpdateOutputSize(int i) {
        i += this.bufOff;
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
        KeyParameter key;
        this.forEncryption = z;
        this.macBlock = null;
        this.initialised = true;
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            nonce = aEADParameters.getNonce();
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            int macSize = aEADParameters.getMacSize();
            if (macSize < 32 || macSize > 128 || macSize % 8 != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid value for MAC size: ");
                stringBuilder.append(macSize);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.macSize = macSize / 8;
            key = aEADParameters.getKey();
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            nonce = parametersWithIV.getIV();
            this.initialAssociatedText = null;
            this.macSize = 16;
            key = (KeyParameter) parametersWithIV.getParameters();
        } else {
            throw new IllegalArgumentException("invalid parameters passed to GCM");
        }
        this.bufBlock = new byte[(z ? 16 : this.macSize + 16)];
        if (nonce == null || nonce.length < 1) {
            throw new IllegalArgumentException("IV must be at least 1 byte");
        }
        if (z && this.nonce != null && Arrays.areEqual(this.nonce, nonce)) {
            if (key == null) {
                throw new IllegalArgumentException("cannot reuse nonce for GCM encryption");
            } else if (this.lastKey != null && Arrays.areEqual(this.lastKey, key.getKey())) {
                throw new IllegalArgumentException("cannot reuse nonce for GCM encryption");
            }
        }
        this.nonce = nonce;
        if (key != null) {
            this.lastKey = key.getKey();
        }
        if (key != null) {
            this.cipher.init(true, key);
            this.H = new byte[16];
            this.cipher.processBlock(this.H, 0, this.H, 0);
            this.multiplier.init(this.H);
            this.exp = null;
        } else if (this.H == null) {
            throw new IllegalArgumentException("Key must be specified in initial init");
        }
        this.J0 = new byte[16];
        if (this.nonce.length == 12) {
            System.arraycopy(this.nonce, 0, this.J0, 0, this.nonce.length);
            this.J0[15] = (byte) 1;
        } else {
            gHASH(this.J0, this.nonce, this.nonce.length);
            byte[] bArr = new byte[16];
            Pack.longToBigEndian(((long) this.nonce.length) * 8, bArr, 8);
            gHASHBlock(this.J0, bArr);
        }
        this.S = new byte[16];
        this.S_at = new byte[16];
        this.S_atPre = new byte[16];
        this.atBlock = new byte[16];
        this.atBlockPos = 0;
        this.atLength = 0;
        this.atLengthPre = 0;
        this.counter = Arrays.clone(this.J0);
        this.blocksRemaining = -2;
        this.bufOff = 0;
        this.totalLength = 0;
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    public void processAADByte(byte b) {
        checkStatus();
        this.atBlock[this.atBlockPos] = b;
        int i = this.atBlockPos + 1;
        this.atBlockPos = i;
        if (i == 16) {
            gHASHBlock(this.S_at, this.atBlock);
            this.atBlockPos = 0;
            this.atLength += 16;
        }
    }

    public void processAADBytes(byte[] bArr, int i, int i2) {
        checkStatus();
        for (int i3 = 0; i3 < i2; i3++) {
            this.atBlock[this.atBlockPos] = bArr[i + i3];
            int i4 = this.atBlockPos + 1;
            this.atBlockPos = i4;
            if (i4 == 16) {
                gHASHBlock(this.S_at, this.atBlock);
                this.atBlockPos = 0;
                this.atLength += 16;
            }
        }
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
        checkStatus();
        this.bufBlock[this.bufOff] = b;
        int i2 = this.bufOff + 1;
        this.bufOff = i2;
        if (i2 != this.bufBlock.length) {
            return 0;
        }
        processBlock(this.bufBlock, 0, bArr, i);
        if (this.forEncryption) {
            this.bufOff = 0;
            return 16;
        }
        System.arraycopy(this.bufBlock, 16, this.bufBlock, 0, this.macSize);
        this.bufOff = this.macSize;
        return 16;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
        checkStatus();
        if (bArr.length - i >= i2) {
            int i4;
            int i5;
            if (this.forEncryption) {
                if (this.bufOff != 0) {
                    while (i2 > 0) {
                        i2--;
                        i5 = i + 1;
                        this.bufBlock[this.bufOff] = bArr[i];
                        i = this.bufOff + 1;
                        this.bufOff = i;
                        if (i == 16) {
                            processBlock(this.bufBlock, 0, bArr2, i3);
                            this.bufOff = 0;
                            i4 = 16;
                            i = i5;
                            break;
                        }
                        i = i5;
                    }
                }
                i4 = 0;
                while (i2 >= 16) {
                    processBlock(bArr, i, bArr2, i3 + i4);
                    i += 16;
                    i2 -= 16;
                    i4 += 16;
                }
                if (i2 > 0) {
                    System.arraycopy(bArr, i, this.bufBlock, 0, i2);
                    this.bufOff = i2;
                    return i4;
                }
            }
            i4 = 0;
            int i6 = i4;
            while (i4 < i2) {
                this.bufBlock[this.bufOff] = bArr[i + i4];
                i5 = this.bufOff + 1;
                this.bufOff = i5;
                if (i5 == this.bufBlock.length) {
                    processBlock(this.bufBlock, 0, bArr2, i3 + i6);
                    System.arraycopy(this.bufBlock, 16, this.bufBlock, 0, this.macSize);
                    this.bufOff = this.macSize;
                    i6 += 16;
                }
                i4++;
            }
            i4 = i6;
            return i4;
        }
        throw new DataLengthException("Input buffer too short");
    }

    public void reset() {
        reset(true);
    }
}
