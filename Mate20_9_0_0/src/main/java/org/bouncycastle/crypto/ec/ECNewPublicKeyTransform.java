package org.bouncycastle.crypto.ec;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECMultiplier;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

public class ECNewPublicKeyTransform implements ECPairTransform {
    private ECPublicKeyParameters key;
    private SecureRandom random;

    protected ECMultiplier createBasePointMultiplier() {
        return new FixedPointCombMultiplier();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:12:0x0033 in {4, 6, 8, 11, 14} preds:[]
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
    public void init(org.bouncycastle.crypto.CipherParameters r2) {
        /*
        r1 = this;
        r0 = r2 instanceof org.bouncycastle.crypto.params.ParametersWithRandom;
        if (r0 == 0) goto L_0x0025;
    L_0x0004:
        r2 = (org.bouncycastle.crypto.params.ParametersWithRandom) r2;
        r0 = r2.getParameters();
        r0 = r0 instanceof org.bouncycastle.crypto.params.ECPublicKeyParameters;
        if (r0 == 0) goto L_0x001d;
    L_0x000e:
        r0 = r2.getParameters();
        r0 = (org.bouncycastle.crypto.params.ECPublicKeyParameters) r0;
        r1.key = r0;
        r2 = r2.getRandom();
    L_0x001a:
        r1.random = r2;
        return;
    L_0x001d:
        r2 = new java.lang.IllegalArgumentException;
        r0 = "ECPublicKeyParameters are required for new public key transform.";
        r2.<init>(r0);
        throw r2;
    L_0x0025:
        r0 = r2 instanceof org.bouncycastle.crypto.params.ECPublicKeyParameters;
        if (r0 == 0) goto L_0x0034;
    L_0x0029:
        r2 = (org.bouncycastle.crypto.params.ECPublicKeyParameters) r2;
        r1.key = r2;
        r2 = new java.security.SecureRandom;
        r2.<init>();
        goto L_0x001a;
        return;
    L_0x0034:
        r2 = new java.lang.IllegalArgumentException;
        r0 = "ECPublicKeyParameters are required for new public key transform.";
        r2.<init>(r0);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.ec.ECNewPublicKeyTransform.init(org.bouncycastle.crypto.CipherParameters):void");
    }

    public ECPair transform(ECPair eCPair) {
        if (this.key != null) {
            ECDomainParameters parameters = this.key.getParameters();
            BigInteger n = parameters.getN();
            ECMultiplier createBasePointMultiplier = createBasePointMultiplier();
            n = ECUtil.generateK(n, this.random);
            ECPoint[] eCPointArr = new ECPoint[]{createBasePointMultiplier.multiply(parameters.getG(), n), this.key.getQ().multiply(n).add(eCPair.getY())};
            parameters.getCurve().normalizeAll(eCPointArr);
            return new ECPair(eCPointArr[0], eCPointArr[1]);
        }
        throw new IllegalStateException("ECNewPublicKeyTransform not initialised");
    }
}
