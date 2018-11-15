package org.bouncycastle.crypto.signers;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.SignerWithRecovery;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.Arrays;

public class ISO9796d2Signer implements SignerWithRecovery {
    public static final int TRAILER_IMPLICIT = 188;
    public static final int TRAILER_RIPEMD128 = 13004;
    public static final int TRAILER_RIPEMD160 = 12748;
    public static final int TRAILER_SHA1 = 13260;
    public static final int TRAILER_SHA256 = 13516;
    public static final int TRAILER_SHA384 = 14028;
    public static final int TRAILER_SHA512 = 13772;
    public static final int TRAILER_WHIRLPOOL = 14284;
    private byte[] block;
    private AsymmetricBlockCipher cipher;
    private Digest digest;
    private boolean fullMessage;
    private int keyBits;
    private byte[] mBuf;
    private int messageLength;
    private byte[] preBlock;
    private byte[] preSig;
    private byte[] recoveredMessage;
    private int trailer;

    public ISO9796d2Signer(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest) {
        this(asymmetricBlockCipher, digest, false);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0019 in {2, 4, 7, 10} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public ISO9796d2Signer(org.bouncycastle.crypto.AsymmetricBlockCipher r2, org.bouncycastle.crypto.Digest r3, boolean r4) {
        /*
        r1 = this;
        r1.<init>();
        r1.cipher = r2;
        r1.digest = r3;
        if (r4 == 0) goto L_0x000e;
    L_0x0009:
        r2 = 188; // 0xbc float:2.63E-43 double:9.3E-322;
    L_0x000b:
        r1.trailer = r2;
        return;
    L_0x000e:
        r2 = org.bouncycastle.crypto.signers.ISOTrailers.getTrailer(r3);
        if (r2 == 0) goto L_0x001a;
    L_0x0014:
        r2 = r2.intValue();
        goto L_0x000b;
        return;
    L_0x001a:
        r2 = new java.lang.IllegalArgumentException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r0 = "no valid trailer for digest: ";
        r4.append(r0);
        r3 = r3.getAlgorithmName();
        r4.append(r3);
        r3 = r4.toString();
        r2.<init>(r3);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.signers.ISO9796d2Signer.<init>(org.bouncycastle.crypto.AsymmetricBlockCipher, org.bouncycastle.crypto.Digest, boolean):void");
    }

    private void clearBlock(byte[] bArr) {
        for (int i = 0; i != bArr.length; i++) {
            bArr[i] = (byte) 0;
        }
    }

    private boolean isSameAs(byte[] bArr, byte[] bArr2) {
        boolean z = true;
        int i;
        if (this.messageLength > this.mBuf.length) {
            if (this.mBuf.length > bArr2.length) {
                z = false;
            }
            for (i = 0; i != this.mBuf.length; i++) {
                if (bArr[i] != bArr2[i]) {
                    z = false;
                }
            }
        } else {
            if (this.messageLength != bArr2.length) {
                z = false;
            }
            for (i = 0; i != bArr2.length; i++) {
                if (bArr[i] != bArr2[i]) {
                    z = false;
                }
            }
        }
        return z;
    }

    private boolean returnFalse(byte[] bArr) {
        this.messageLength = 0;
        clearBlock(this.mBuf);
        clearBlock(bArr);
        return false;
    }

    public byte[] generateSignature() throws CryptoException {
        int length;
        int i;
        byte[] bArr;
        int digestSize = this.digest.getDigestSize();
        boolean z = true;
        if (this.trailer == 188) {
            length = (this.block.length - digestSize) - 1;
            this.digest.doFinal(this.block, length);
            this.block[this.block.length - 1] = PSSSigner.TRAILER_IMPLICIT;
            i = length;
            length = 8;
        } else {
            length = 16;
            i = (this.block.length - digestSize) - 2;
            this.digest.doFinal(this.block, i);
            this.block[this.block.length - 2] = (byte) (this.trailer >>> 8);
            this.block[this.block.length - 1] = (byte) this.trailer;
        }
        digestSize = ((((digestSize + this.messageLength) * 8) + length) + 4) - this.keyBits;
        if (digestSize > 0) {
            int i2 = this.messageLength - ((digestSize + 7) / 8);
            digestSize = 96;
            i -= i2;
            System.arraycopy(this.mBuf, 0, this.block, i, i2);
            bArr = new byte[i2];
        } else {
            digestSize = 64;
            i -= this.messageLength;
            System.arraycopy(this.mBuf, 0, this.block, i, this.messageLength);
            bArr = new byte[this.messageLength];
        }
        this.recoveredMessage = bArr;
        i--;
        if (i > 0) {
            for (int i3 = i; i3 != 0; i3--) {
                this.block[i3] = (byte) -69;
            }
            bArr = this.block;
            bArr[i] = (byte) (bArr[i] ^ 1);
            this.block[0] = (byte) 11;
            bArr = this.block;
            bArr[0] = (byte) (bArr[0] | digestSize);
        } else {
            this.block[0] = (byte) 10;
            bArr = this.block;
            bArr[0] = (byte) (bArr[0] | digestSize);
        }
        bArr = this.cipher.processBlock(this.block, 0, this.block.length);
        if ((digestSize & 32) != 0) {
            z = false;
        }
        this.fullMessage = z;
        System.arraycopy(this.mBuf, 0, this.recoveredMessage, 0, this.recoveredMessage.length);
        this.messageLength = 0;
        clearBlock(this.mBuf);
        clearBlock(this.block);
        return bArr;
    }

    public byte[] getRecoveredMessage() {
        return this.recoveredMessage;
    }

    public boolean hasFullMessage() {
        return this.fullMessage;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        RSAKeyParameters rSAKeyParameters = (RSAKeyParameters) cipherParameters;
        this.cipher.init(z, rSAKeyParameters);
        this.keyBits = rSAKeyParameters.getModulus().bitLength();
        this.block = new byte[((this.keyBits + 7) / 8)];
        this.mBuf = new byte[(this.trailer == 188 ? (this.block.length - this.digest.getDigestSize()) - 2 : (this.block.length - this.digest.getDigestSize()) - 3)];
        reset();
    }

    public void reset() {
        this.digest.reset();
        this.messageLength = 0;
        clearBlock(this.mBuf);
        if (this.recoveredMessage != null) {
            clearBlock(this.recoveredMessage);
        }
        this.recoveredMessage = null;
        this.fullMessage = false;
        if (this.preSig != null) {
            this.preSig = null;
            clearBlock(this.preBlock);
            this.preBlock = null;
        }
    }

    public void update(byte b) {
        this.digest.update(b);
        if (this.messageLength < this.mBuf.length) {
            this.mBuf[this.messageLength] = b;
        }
        this.messageLength++;
    }

    public void update(byte[] bArr, int i, int i2) {
        while (i2 > 0 && this.messageLength < this.mBuf.length) {
            update(bArr[i]);
            i++;
            i2--;
        }
        this.digest.update(bArr, i, i2);
        this.messageLength += i2;
    }

    public void updateWithRecoveredMessage(byte[] bArr) throws InvalidCipherTextException {
        Object processBlock = this.cipher.processBlock(bArr, 0, bArr.length);
        if (((processBlock[0] & 192) ^ 64) != 0) {
            throw new InvalidCipherTextException("malformed signature");
        } else if (((processBlock[processBlock.length - 1] & 15) ^ 12) == 0) {
            int i;
            int intValue;
            int i2 = 2;
            if (((processBlock[processBlock.length - 1] & 255) ^ 188) == 0) {
                i2 = 1;
            } else {
                i = ((processBlock[processBlock.length - 2] & 255) << 8) | (processBlock[processBlock.length - 1] & 255);
                Integer trailer = ISOTrailers.getTrailer(this.digest);
                if (trailer != null) {
                    intValue = trailer.intValue();
                    if (!(i == intValue || (intValue == ISOTrailers.TRAILER_SHA512_256 && i == 16588))) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("signer initialised with wrong digest for trailer ");
                        stringBuilder.append(i);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
                throw new IllegalArgumentException("unrecognised hash in signature");
            }
            i = 0;
            while (i != processBlock.length && ((processBlock[i] & 15) ^ 10) != 0) {
                i++;
            }
            i++;
            intValue = ((processBlock.length - i2) - this.digest.getDigestSize()) - i;
            if (intValue > 0) {
                Object obj;
                if ((processBlock[0] & 32) == 0) {
                    this.fullMessage = true;
                    this.recoveredMessage = new byte[intValue];
                    obj = this.recoveredMessage;
                    i2 = this.recoveredMessage.length;
                } else {
                    this.fullMessage = false;
                    this.recoveredMessage = new byte[intValue];
                    obj = this.recoveredMessage;
                    i2 = this.recoveredMessage.length;
                }
                System.arraycopy(processBlock, i, obj, 0, i2);
                this.preSig = bArr;
                this.preBlock = processBlock;
                this.digest.update(this.recoveredMessage, 0, this.recoveredMessage.length);
                this.messageLength = this.recoveredMessage.length;
                System.arraycopy(this.recoveredMessage, 0, this.mBuf, 0, this.recoveredMessage.length);
                return;
            }
            throw new InvalidCipherTextException("malformed block");
        } else {
            throw new InvalidCipherTextException("malformed signature");
        }
    }

    public boolean verifySignature(byte[] bArr) {
        Object processBlock;
        if (this.preSig == null) {
            try {
                processBlock = this.cipher.processBlock(bArr, 0, bArr.length);
            } catch (Exception e) {
                return false;
            }
        } else if (Arrays.areEqual(this.preSig, bArr)) {
            processBlock = this.preBlock;
            this.preSig = null;
            this.preBlock = null;
        } else {
            throw new IllegalStateException("updateWithRecoveredMessage called on different signature");
        }
        if (((processBlock[0] & 192) ^ 64) != 0) {
            return returnFalse(processBlock);
        }
        if (((processBlock[processBlock.length - 1] & 15) ^ 12) != 0) {
            return returnFalse(processBlock);
        }
        int i;
        int intValue;
        int i2 = 2;
        if (((processBlock[processBlock.length - 1] & 255) ^ 188) == 0) {
            i2 = 1;
        } else {
            i = ((processBlock[processBlock.length - 2] & 255) << 8) | (processBlock[processBlock.length - 1] & 255);
            Integer trailer = ISOTrailers.getTrailer(this.digest);
            if (trailer != null) {
                intValue = trailer.intValue();
                if (!(i == intValue || (intValue == ISOTrailers.TRAILER_SHA512_256 && i == 16588))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("signer initialised with wrong digest for trailer ");
                    stringBuilder.append(i);
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
            throw new IllegalArgumentException("unrecognised hash in signature");
        }
        i = 0;
        while (i != processBlock.length && ((processBlock[i] & 15) ^ 10) != 0) {
            i++;
        }
        i++;
        byte[] bArr2 = new byte[this.digest.getDigestSize()];
        int length = (processBlock.length - i2) - bArr2.length;
        i2 = length - i;
        if (i2 <= 0) {
            return returnFalse(processBlock);
        }
        Object obj;
        boolean z;
        int i3;
        int i4;
        if ((processBlock[0] & 32) == 0) {
            this.fullMessage = true;
            if (this.messageLength > i2) {
                return returnFalse(processBlock);
            }
            this.digest.reset();
            this.digest.update(processBlock, i, i2);
            this.digest.doFinal(bArr2, 0);
            z = true;
            for (i3 = 0; i3 != bArr2.length; i3++) {
                i4 = length + i3;
                processBlock[i4] = (byte) (processBlock[i4] ^ bArr2[i3]);
                if (processBlock[i4] != (byte) 0) {
                    z = false;
                }
            }
            if (!z) {
                return returnFalse(processBlock);
            }
            this.recoveredMessage = new byte[i2];
            obj = this.recoveredMessage;
            intValue = this.recoveredMessage.length;
        } else {
            this.fullMessage = false;
            this.digest.doFinal(bArr2, 0);
            z = true;
            for (i3 = 0; i3 != bArr2.length; i3++) {
                i4 = length + i3;
                processBlock[i4] = (byte) (processBlock[i4] ^ bArr2[i3]);
                if (processBlock[i4] != (byte) 0) {
                    z = false;
                }
            }
            if (!z) {
                return returnFalse(processBlock);
            }
            this.recoveredMessage = new byte[i2];
            obj = this.recoveredMessage;
            intValue = this.recoveredMessage.length;
        }
        System.arraycopy(processBlock, i, obj, 0, intValue);
        if (this.messageLength != 0 && !isSameAs(this.mBuf, this.recoveredMessage)) {
            return returnFalse(processBlock);
        }
        clearBlock(this.mBuf);
        clearBlock(processBlock);
        this.messageLength = 0;
        return true;
    }
}
