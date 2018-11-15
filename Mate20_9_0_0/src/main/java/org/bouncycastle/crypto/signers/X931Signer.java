package org.bouncycastle.crypto.signers;

import java.math.BigInteger;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;

public class X931Signer implements Signer {
    public static final int TRAILER_IMPLICIT = 188;
    public static final int TRAILER_RIPEMD128 = 13004;
    public static final int TRAILER_RIPEMD160 = 12748;
    public static final int TRAILER_SHA1 = 13260;
    public static final int TRAILER_SHA224 = 14540;
    public static final int TRAILER_SHA256 = 13516;
    public static final int TRAILER_SHA384 = 14028;
    public static final int TRAILER_SHA512 = 13772;
    public static final int TRAILER_WHIRLPOOL = 14284;
    private byte[] block;
    private AsymmetricBlockCipher cipher;
    private Digest digest;
    private RSAKeyParameters kParam;
    private int keyBits;
    private int trailer;

    public X931Signer(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest) {
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
    public X931Signer(org.bouncycastle.crypto.AsymmetricBlockCipher r2, org.bouncycastle.crypto.Digest r3, boolean r4) {
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
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.signers.X931Signer.<init>(org.bouncycastle.crypto.AsymmetricBlockCipher, org.bouncycastle.crypto.Digest, boolean):void");
    }

    private void clearBlock(byte[] bArr) {
        for (int i = 0; i != bArr.length; i++) {
            bArr[i] = (byte) 0;
        }
    }

    private void createSignatureBlock(int i) {
        int digestSize = this.digest.getDigestSize();
        if (i == 188) {
            i = (this.block.length - digestSize) - 1;
            this.digest.doFinal(this.block, i);
            this.block[this.block.length - 1] = PSSSigner.TRAILER_IMPLICIT;
        } else {
            digestSize = (this.block.length - digestSize) - 2;
            this.digest.doFinal(this.block, digestSize);
            this.block[this.block.length - 2] = (byte) (i >>> 8);
            this.block[this.block.length - 1] = (byte) i;
            i = digestSize;
        }
        this.block[0] = (byte) 107;
        for (digestSize = i - 2; digestSize != 0; digestSize--) {
            this.block[digestSize] = (byte) -69;
        }
        this.block[i - 1] = (byte) -70;
    }

    public byte[] generateSignature() throws CryptoException {
        createSignatureBlock(this.trailer);
        BigInteger bigInteger = new BigInteger(1, this.cipher.processBlock(this.block, 0, this.block.length));
        clearBlock(this.block);
        return BigIntegers.asUnsignedByteArray((this.kParam.getModulus().bitLength() + 7) / 8, bigInteger.min(this.kParam.getModulus().subtract(bigInteger)));
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.kParam = (RSAKeyParameters) cipherParameters;
        this.cipher.init(z, this.kParam);
        this.keyBits = this.kParam.getModulus().bitLength();
        this.block = new byte[((this.keyBits + 7) / 8)];
        reset();
    }

    public void reset() {
        this.digest.reset();
    }

    public void update(byte b) {
        this.digest.update(b);
    }

    public void update(byte[] bArr, int i, int i2) {
        this.digest.update(bArr, i, i2);
    }

    /* JADX WARNING: Missing block: B:6:0x002d, code:
            if ((r4.intValue() & 15) == 12) goto L_0x002f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean verifySignature(byte[] bArr) {
        boolean z = false;
        try {
            this.block = this.cipher.processBlock(bArr, 0, bArr.length);
            BigInteger bigInteger = new BigInteger(1, this.block);
            if ((bigInteger.intValue() & 15) != 12) {
                bigInteger = this.kParam.getModulus().subtract(bigInteger);
            }
            createSignatureBlock(this.trailer);
            bArr = BigIntegers.asUnsignedByteArray(this.block.length, bigInteger);
            z = Arrays.constantTimeAreEqual(this.block, bArr);
            if (this.trailer == ISOTrailers.TRAILER_SHA512_256 && !z) {
                this.block[this.block.length - 2] = (byte) 64;
                z = Arrays.constantTimeAreEqual(this.block, bArr);
            }
            clearBlock(this.block);
            clearBlock(bArr);
            return z;
        } catch (Exception e) {
            return false;
        }
    }
}
