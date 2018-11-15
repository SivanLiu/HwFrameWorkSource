package org.bouncycastle.crypto.signers;

import java.security.SecureRandom;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.SignerWithRecovery;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.ParametersWithSalt;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.Arrays;

public class ISO9796d2PSSSigner implements SignerWithRecovery {
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
    private int hLen;
    private int keyBits;
    private byte[] mBuf;
    private int messageLength;
    private byte[] preBlock;
    private int preMStart;
    private byte[] preSig;
    private int preTLength;
    private SecureRandom random;
    private byte[] recoveredMessage;
    private int saltLength;
    private byte[] standardSalt;
    private int trailer;

    public ISO9796d2PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, int i) {
        this(asymmetricBlockCipher, digest, i, false);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0021 in {2, 4, 7, 10} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public ISO9796d2PSSSigner(org.bouncycastle.crypto.AsymmetricBlockCipher r1, org.bouncycastle.crypto.Digest r2, int r3, boolean r4) {
        /*
        r0 = this;
        r0.<init>();
        r0.cipher = r1;
        r0.digest = r2;
        r1 = r2.getDigestSize();
        r0.hLen = r1;
        r0.saltLength = r3;
        if (r4 == 0) goto L_0x0016;
    L_0x0011:
        r1 = 188; // 0xbc float:2.63E-43 double:9.3E-322;
    L_0x0013:
        r0.trailer = r1;
        return;
    L_0x0016:
        r1 = org.bouncycastle.crypto.signers.ISOTrailers.getTrailer(r2);
        if (r1 == 0) goto L_0x0022;
    L_0x001c:
        r1 = r1.intValue();
        goto L_0x0013;
        return;
    L_0x0022:
        r1 = new java.lang.IllegalArgumentException;
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "no valid trailer for digest: ";
        r3.append(r4);
        r2 = r2.getAlgorithmName();
        r3.append(r2);
        r2 = r3.toString();
        r1.<init>(r2);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.signers.ISO9796d2PSSSigner.<init>(org.bouncycastle.crypto.AsymmetricBlockCipher, org.bouncycastle.crypto.Digest, int, boolean):void");
    }

    private void ItoOSP(int i, byte[] bArr) {
        bArr[0] = (byte) (i >>> 24);
        bArr[1] = (byte) (i >>> 16);
        bArr[2] = (byte) (i >>> 8);
        bArr[3] = (byte) (i >>> 0);
    }

    private void LtoOSP(long j, byte[] bArr) {
        bArr[0] = (byte) ((int) (j >>> 56));
        bArr[1] = (byte) ((int) (j >>> 48));
        bArr[2] = (byte) ((int) (j >>> 40));
        bArr[3] = (byte) ((int) (j >>> 32));
        bArr[4] = (byte) ((int) (j >>> 24));
        bArr[5] = (byte) ((int) (j >>> 16));
        bArr[6] = (byte) ((int) (j >>> 8));
        bArr[7] = (byte) ((int) (j >>> 0));
    }

    private void clearBlock(byte[] bArr) {
        for (int i = 0; i != bArr.length; i++) {
            bArr[i] = (byte) 0;
        }
    }

    private boolean isSameAs(byte[] bArr, byte[] bArr2) {
        boolean z = this.messageLength == bArr2.length;
        for (int i = 0; i != bArr2.length; i++) {
            if (bArr[i] != bArr2[i]) {
                z = false;
            }
        }
        return z;
    }

    private byte[] maskGeneratorFunction1(byte[] bArr, int i, int i2, int i3) {
        Object obj = new byte[i3];
        Object obj2 = new byte[this.hLen];
        byte[] bArr2 = new byte[4];
        this.digest.reset();
        int i4 = 0;
        while (i4 < i3 / this.hLen) {
            ItoOSP(i4, bArr2);
            this.digest.update(bArr, i, i2);
            this.digest.update(bArr2, 0, bArr2.length);
            this.digest.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, this.hLen * i4, this.hLen);
            i4++;
        }
        if (this.hLen * i4 < i3) {
            ItoOSP(i4, bArr2);
            this.digest.update(bArr, i, i2);
            this.digest.update(bArr2, 0, bArr2.length);
            this.digest.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, this.hLen * i4, obj.length - (i4 * this.hLen));
        }
        return obj;
    }

    public byte[] generateSignature() throws CryptoException {
        Object obj;
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        byte[] bArr2 = new byte[8];
        LtoOSP((long) (this.messageLength * 8), bArr2);
        this.digest.update(bArr2, 0, bArr2.length);
        this.digest.update(this.mBuf, 0, this.messageLength);
        this.digest.update(bArr, 0, bArr.length);
        if (this.standardSalt != null) {
            obj = this.standardSalt;
        } else {
            obj = new byte[this.saltLength];
            this.random.nextBytes(obj);
        }
        this.digest.update(obj, 0, obj.length);
        Object obj2 = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(obj2, 0);
        boolean z = true;
        int i = this.trailer == 188 ? 1 : 2;
        int length = ((((this.block.length - this.messageLength) - obj.length) - this.hLen) - i) - 1;
        this.block[length] = (byte) 1;
        length++;
        System.arraycopy(this.mBuf, 0, this.block, length, this.messageLength);
        System.arraycopy(obj, 0, this.block, length + this.messageLength, obj.length);
        bArr = maskGeneratorFunction1(obj2, 0, obj2.length, (this.block.length - this.hLen) - i);
        for (length = 0; length != bArr.length; length++) {
            byte[] bArr3 = this.block;
            bArr3[length] = (byte) (bArr3[length] ^ bArr[length]);
        }
        System.arraycopy(obj2, 0, this.block, (this.block.length - this.hLen) - i, this.hLen);
        if (this.trailer == 188) {
            this.block[this.block.length - 1] = PSSSigner.TRAILER_IMPLICIT;
        } else {
            this.block[this.block.length - 2] = (byte) (this.trailer >>> 8);
            this.block[this.block.length - 1] = (byte) this.trailer;
        }
        bArr = this.block;
        bArr[0] = (byte) (bArr[0] & CertificateBody.profileType);
        bArr = this.cipher.processBlock(this.block, 0, this.block.length);
        this.recoveredMessage = new byte[this.messageLength];
        if (this.messageLength > this.mBuf.length) {
            z = false;
        }
        this.fullMessage = z;
        System.arraycopy(this.mBuf, 0, this.recoveredMessage, 0, this.recoveredMessage.length);
        clearBlock(this.mBuf);
        clearBlock(this.block);
        this.messageLength = 0;
        return bArr;
    }

    public byte[] getRecoveredMessage() {
        return this.recoveredMessage;
    }

    public boolean hasFullMessage() {
        return this.fullMessage;
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x007b  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0067  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0067  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x007b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void init(boolean z, CipherParameters cipherParameters) {
        Object obj;
        SecureRandom random;
        int i = this.saltLength;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            obj = (RSAKeyParameters) parametersWithRandom.getParameters();
            if (z) {
                random = parametersWithRandom.getRandom();
            }
            this.cipher.init(z, obj);
            this.keyBits = obj.getModulus().bitLength();
            this.block = new byte[((this.keyBits + 7) / 8)];
            this.mBuf = new byte[(this.trailer == 188 ? (((this.block.length - this.digest.getDigestSize()) - i) - 1) - 1 : (((this.block.length - this.digest.getDigestSize()) - i) - 1) - 2)];
            reset();
        }
        if (cipherParameters instanceof ParametersWithSalt) {
            ParametersWithSalt parametersWithSalt = (ParametersWithSalt) cipherParameters;
            obj = (RSAKeyParameters) parametersWithSalt.getParameters();
            this.standardSalt = parametersWithSalt.getSalt();
            i = this.standardSalt.length;
            if (this.standardSalt.length != this.saltLength) {
                throw new IllegalArgumentException("Fixed salt is of wrong length");
            }
        }
        obj = (RSAKeyParameters) cipherParameters;
        if (z) {
            random = new SecureRandom();
        }
        this.cipher.init(z, obj);
        this.keyBits = obj.getModulus().bitLength();
        this.block = new byte[((this.keyBits + 7) / 8)];
        if (this.trailer == 188) {
        }
        this.mBuf = new byte[(this.trailer == 188 ? (((this.block.length - this.digest.getDigestSize()) - i) - 1) - 1 : (((this.block.length - this.digest.getDigestSize()) - i) - 1) - 2)];
        reset();
        this.random = random;
        this.cipher.init(z, obj);
        this.keyBits = obj.getModulus().bitLength();
        this.block = new byte[((this.keyBits + 7) / 8)];
        if (this.trailer == 188) {
        }
        this.mBuf = new byte[(this.trailer == 188 ? (((this.block.length - this.digest.getDigestSize()) - i) - 1) - 1 : (((this.block.length - this.digest.getDigestSize()) - i) - 1) - 2)];
        reset();
    }

    public void reset() {
        this.digest.reset();
        this.messageLength = 0;
        if (this.mBuf != null) {
            clearBlock(this.mBuf);
        }
        if (this.recoveredMessage != null) {
            clearBlock(this.recoveredMessage);
            this.recoveredMessage = null;
        }
        this.fullMessage = false;
        if (this.preSig != null) {
            this.preSig = null;
            clearBlock(this.preBlock);
            this.preBlock = null;
        }
    }

    public void update(byte b) {
        if (this.preSig != null || this.messageLength >= this.mBuf.length) {
            this.digest.update(b);
            return;
        }
        byte[] bArr = this.mBuf;
        int i = this.messageLength;
        this.messageLength = i + 1;
        bArr[i] = b;
    }

    public void update(byte[] bArr, int i, int i2) {
        if (this.preSig == null) {
            while (i2 > 0 && this.messageLength < this.mBuf.length) {
                update(bArr[i]);
                i++;
                i2--;
            }
        }
        if (i2 > 0) {
            this.digest.update(bArr, i, i2);
        }
    }

    public void updateWithRecoveredMessage(byte[] bArr) throws InvalidCipherTextException {
        int intValue;
        Object processBlock = this.cipher.processBlock(bArr, 0, bArr.length);
        if (processBlock.length < (this.keyBits + 7) / 8) {
            Object obj = new byte[((this.keyBits + 7) / 8)];
            System.arraycopy(processBlock, 0, obj, obj.length - processBlock.length, processBlock.length);
            clearBlock(processBlock);
            processBlock = obj;
        }
        boolean z = true;
        int i = 2;
        if (((processBlock[processBlock.length - 1] & 255) ^ 188) == 0) {
            i = 1;
        } else {
            int i2 = ((processBlock[processBlock.length - 2] & 255) << 8) | (processBlock[processBlock.length - 1] & 255);
            Integer trailer = ISOTrailers.getTrailer(this.digest);
            if (trailer != null) {
                intValue = trailer.intValue();
                if (!(i2 == intValue || (intValue == ISOTrailers.TRAILER_SHA512_256 && i2 == 16588))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("signer initialised with wrong digest for trailer ");
                    stringBuilder.append(i2);
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
            throw new IllegalArgumentException("unrecognised hash in signature");
        }
        this.digest.doFinal(new byte[this.hLen], 0);
        byte[] maskGeneratorFunction1 = maskGeneratorFunction1(processBlock, (processBlock.length - this.hLen) - i, this.hLen, (processBlock.length - this.hLen) - i);
        for (intValue = 0; intValue != maskGeneratorFunction1.length; intValue++) {
            processBlock[intValue] = (byte) (processBlock[intValue] ^ maskGeneratorFunction1[intValue]);
        }
        processBlock[0] = (byte) (processBlock[0] & CertificateBody.profileType);
        intValue = 0;
        while (intValue != processBlock.length && processBlock[intValue] != (byte) 1) {
            intValue++;
        }
        intValue++;
        if (intValue >= processBlock.length) {
            clearBlock(processBlock);
        }
        if (intValue <= 1) {
            z = false;
        }
        this.fullMessage = z;
        this.recoveredMessage = new byte[((maskGeneratorFunction1.length - intValue) - this.saltLength)];
        System.arraycopy(processBlock, intValue, this.recoveredMessage, 0, this.recoveredMessage.length);
        System.arraycopy(this.recoveredMessage, 0, this.mBuf, 0, this.recoveredMessage.length);
        this.preSig = bArr;
        this.preBlock = processBlock;
        this.preMStart = intValue;
        this.preTLength = i;
    }

    public boolean verifySignature(byte[] bArr) {
        byte[] bArr2 = new byte[this.hLen];
        this.digest.doFinal(bArr2, 0);
        if (this.preSig == null) {
            try {
                updateWithRecoveredMessage(bArr);
            } catch (Exception e) {
                return false;
            }
        } else if (!Arrays.areEqual(this.preSig, bArr)) {
            throw new IllegalStateException("updateWithRecoveredMessage called on different signature");
        }
        bArr = this.preBlock;
        int i = this.preMStart;
        int i2 = this.preTLength;
        this.preSig = null;
        this.preBlock = null;
        byte[] bArr3 = new byte[8];
        LtoOSP((long) (this.recoveredMessage.length * 8), bArr3);
        this.digest.update(bArr3, 0, bArr3.length);
        if (this.recoveredMessage.length != 0) {
            this.digest.update(this.recoveredMessage, 0, this.recoveredMessage.length);
        }
        this.digest.update(bArr2, 0, bArr2.length);
        if (this.standardSalt != null) {
            this.digest.update(this.standardSalt, 0, this.standardSalt.length);
        } else {
            this.digest.update(bArr, i + this.recoveredMessage.length, this.saltLength);
        }
        bArr2 = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr2, 0);
        i = (bArr.length - i2) - bArr2.length;
        boolean z = true;
        for (int i3 = 0; i3 != bArr2.length; i3++) {
            if (bArr2[i3] != bArr[i + i3]) {
                z = false;
            }
        }
        clearBlock(bArr);
        clearBlock(bArr2);
        if (!z) {
            this.fullMessage = false;
            this.messageLength = 0;
            bArr = this.recoveredMessage;
        } else if (this.messageLength == 0 || isSameAs(this.mBuf, this.recoveredMessage)) {
            this.messageLength = 0;
            clearBlock(this.mBuf);
            return true;
        } else {
            this.messageLength = 0;
            bArr = this.mBuf;
        }
        clearBlock(bArr);
        return false;
    }
}
