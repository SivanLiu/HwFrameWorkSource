package org.bouncycastle.crypto.engines;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.util.BigIntegers;

public class RSABlindedEngine implements AsymmetricBlockCipher {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private RSACoreEngine core = new RSACoreEngine();
    private RSAKeyParameters key;
    private SecureRandom random;

    public int getInputBlockSize() {
        return this.core.getInputBlockSize();
    }

    public int getOutputBlockSize() {
        return this.core.getOutputBlockSize();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0024 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
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
    public void init(boolean r2, org.bouncycastle.crypto.CipherParameters r3) {
        /*
        r1 = this;
        r0 = r1.core;
        r0.init(r2, r3);
        r2 = r3 instanceof org.bouncycastle.crypto.params.ParametersWithRandom;
        if (r2 == 0) goto L_0x001a;
        r3 = (org.bouncycastle.crypto.params.ParametersWithRandom) r3;
        r2 = r3.getParameters();
        r2 = (org.bouncycastle.crypto.params.RSAKeyParameters) r2;
        r1.key = r2;
        r2 = r3.getRandom();
        r1.random = r2;
        return;
        r3 = (org.bouncycastle.crypto.params.RSAKeyParameters) r3;
        r1.key = r3;
        r2 = new java.security.SecureRandom;
        r2.<init>();
        goto L_0x0017;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.RSABlindedEngine.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) {
        if (this.key != null) {
            BigInteger createRandomInRange;
            BigInteger convertInput = this.core.convertInput(bArr, i, i2);
            if (this.key instanceof RSAPrivateCrtKeyParameters) {
                RSAPrivateCrtKeyParameters rSAPrivateCrtKeyParameters = (RSAPrivateCrtKeyParameters) this.key;
                BigInteger publicExponent = rSAPrivateCrtKeyParameters.getPublicExponent();
                if (publicExponent != null) {
                    BigInteger modulus = rSAPrivateCrtKeyParameters.getModulus();
                    createRandomInRange = BigIntegers.createRandomInRange(ONE, modulus.subtract(ONE), this.random);
                    createRandomInRange = this.core.processBlock(createRandomInRange.modPow(publicExponent, modulus).multiply(convertInput).mod(modulus)).multiply(createRandomInRange.modInverse(modulus)).mod(modulus);
                    if (!convertInput.equals(createRandomInRange.modPow(publicExponent, modulus))) {
                        throw new IllegalStateException("RSA engine faulty decryption/signing detected");
                    }
                    return this.core.convertOutput(createRandomInRange);
                }
            }
            createRandomInRange = this.core.processBlock(convertInput);
            return this.core.convertOutput(createRandomInRange);
        }
        throw new IllegalStateException("RSA engine not initialised");
    }
}
