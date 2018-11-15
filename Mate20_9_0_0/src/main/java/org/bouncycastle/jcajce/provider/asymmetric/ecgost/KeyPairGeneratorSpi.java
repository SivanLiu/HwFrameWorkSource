package org.bouncycastle.jcajce.provider.asymmetric.ecgost;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;

public class KeyPairGeneratorSpi extends KeyPairGenerator {
    String algorithm = "ECGOST3410";
    Object ecParams = null;
    ECKeyPairGenerator engine = new ECKeyPairGenerator();
    boolean initialised = false;
    ECKeyGenerationParameters param;
    SecureRandom random = null;
    int strength = 239;

    public KeyPairGeneratorSpi() {
        super("ECGOST3410");
    }

    public KeyPair generateKeyPair() {
        if (this.initialised) {
            AsymmetricCipherKeyPair generateKeyPair = this.engine.generateKeyPair();
            ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) generateKeyPair.getPublic();
            ECPrivateKeyParameters eCPrivateKeyParameters = (ECPrivateKeyParameters) generateKeyPair.getPrivate();
            BCECGOST3410PublicKey bCECGOST3410PublicKey;
            if (this.ecParams instanceof ECParameterSpec) {
                ECParameterSpec eCParameterSpec = (ECParameterSpec) this.ecParams;
                bCECGOST3410PublicKey = new BCECGOST3410PublicKey(this.algorithm, eCPublicKeyParameters, eCParameterSpec);
                return new KeyPair(bCECGOST3410PublicKey, new BCECGOST3410PrivateKey(this.algorithm, eCPrivateKeyParameters, bCECGOST3410PublicKey, eCParameterSpec));
            } else if (this.ecParams == null) {
                return new KeyPair(new BCECGOST3410PublicKey(this.algorithm, eCPublicKeyParameters), new BCECGOST3410PrivateKey(this.algorithm, eCPrivateKeyParameters));
            } else {
                java.security.spec.ECParameterSpec eCParameterSpec2 = (java.security.spec.ECParameterSpec) this.ecParams;
                bCECGOST3410PublicKey = new BCECGOST3410PublicKey(this.algorithm, eCPublicKeyParameters, eCParameterSpec2);
                return new KeyPair(bCECGOST3410PublicKey, new BCECGOST3410PrivateKey(this.algorithm, eCPrivateKeyParameters, bCECGOST3410PublicKey, eCParameterSpec2));
            }
        }
        throw new IllegalStateException("EC Key Pair Generator not initialised");
    }

    public void initialize(int i, SecureRandom secureRandom) {
        this.strength = i;
        this.random = secureRandom;
        if (this.ecParams != null) {
            try {
                initialize((ECGenParameterSpec) this.ecParams, secureRandom);
                return;
            } catch (InvalidAlgorithmParameterException e) {
                throw new InvalidParameterException("key size not configurable.");
            }
        }
        throw new InvalidParameterException("unknown key size.");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:33:0x012b in {2, 3, 5, 8, 9, 14, 18, 23, 25, 27, 29, 32, 35} preds:[]
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
    public void initialize(java.security.spec.AlgorithmParameterSpec r11, java.security.SecureRandom r12) throws java.security.InvalidAlgorithmParameterException {
        /*
        r10 = this;
        r0 = r11 instanceof org.bouncycastle.jce.spec.ECParameterSpec;
        r1 = 1;
        if (r0 == 0) goto L_0x0030;
    L_0x0005:
        r0 = r11;
        r0 = (org.bouncycastle.jce.spec.ECParameterSpec) r0;
        r10.ecParams = r11;
        r11 = new org.bouncycastle.crypto.params.ECKeyGenerationParameters;
        r2 = new org.bouncycastle.crypto.params.ECDomainParameters;
        r3 = r0.getCurve();
        r4 = r0.getG();
        r5 = r0.getN();
        r0 = r0.getH();
        r2.<init>(r3, r4, r5, r0);
        r11.<init>(r2, r12);
    L_0x0024:
        r10.param = r11;
    L_0x0026:
        r11 = r10.engine;
        r12 = r10.param;
        r11.init(r12);
        r10.initialised = r1;
        return;
    L_0x0030:
        r0 = r11 instanceof java.security.spec.ECParameterSpec;
        r2 = 0;
        if (r0 == 0) goto L_0x0064;
    L_0x0035:
        r0 = r11;
        r0 = (java.security.spec.ECParameterSpec) r0;
        r10.ecParams = r11;
        r11 = r0.getCurve();
        r11 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertCurve(r11);
        r3 = r0.getGenerator();
        r2 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertPoint(r11, r3, r2);
        r3 = new org.bouncycastle.crypto.params.ECKeyGenerationParameters;
        r4 = new org.bouncycastle.crypto.params.ECDomainParameters;
        r5 = r0.getOrder();
        r0 = r0.getCofactor();
        r6 = (long) r0;
        r0 = java.math.BigInteger.valueOf(r6);
        r4.<init>(r11, r2, r5, r0);
        r3.<init>(r4, r12);
    L_0x0061:
        r10.param = r3;
        goto L_0x0026;
    L_0x0064:
        r0 = r11 instanceof java.security.spec.ECGenParameterSpec;
        if (r0 != 0) goto L_0x00cb;
    L_0x0068:
        r3 = r11 instanceof org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec;
        if (r3 == 0) goto L_0x006d;
    L_0x006c:
        goto L_0x00cb;
    L_0x006d:
        if (r11 != 0) goto L_0x009a;
    L_0x006f:
        r0 = org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION;
        r0 = r0.getEcImplicitlyCa();
        if (r0 == 0) goto L_0x009a;
    L_0x0077:
        r0 = org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION;
        r0 = r0.getEcImplicitlyCa();
        r10.ecParams = r11;
        r11 = new org.bouncycastle.crypto.params.ECKeyGenerationParameters;
        r2 = new org.bouncycastle.crypto.params.ECDomainParameters;
        r3 = r0.getCurve();
        r4 = r0.getG();
        r5 = r0.getN();
        r0 = r0.getH();
        r2.<init>(r3, r4, r5, r0);
        r11.<init>(r2, r12);
        goto L_0x0024;
    L_0x009a:
        if (r11 != 0) goto L_0x00ac;
    L_0x009c:
        r12 = org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION;
        r12 = r12.getEcImplicitlyCa();
        if (r12 != 0) goto L_0x00ac;
    L_0x00a4:
        r11 = new java.security.InvalidAlgorithmParameterException;
        r12 = "null parameter passed but no implicitCA set";
        r11.<init>(r12);
        throw r11;
    L_0x00ac:
        r12 = new java.security.InvalidAlgorithmParameterException;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "parameter object not a ECParameterSpec: ";
        r0.append(r1);
        r11 = r11.getClass();
        r11 = r11.getName();
        r0.append(r11);
        r11 = r0.toString();
        r12.<init>(r11);
        throw r12;
    L_0x00cb:
        if (r0 == 0) goto L_0x00d5;
    L_0x00cd:
        r11 = (java.security.spec.ECGenParameterSpec) r11;
        r11 = r11.getName();
    L_0x00d3:
        r4 = r11;
        goto L_0x00dc;
    L_0x00d5:
        r11 = (org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec) r11;
        r11 = r11.getName();
        goto L_0x00d3;
    L_0x00dc:
        r11 = org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves.getByName(r4);
        if (r11 == 0) goto L_0x012c;
    L_0x00e2:
        r0 = new org.bouncycastle.jce.spec.ECNamedCurveSpec;
        r5 = r11.getCurve();
        r6 = r11.getG();
        r7 = r11.getN();
        r8 = r11.getH();
        r9 = r11.getSeed();
        r3 = r0;
        r3.<init>(r4, r5, r6, r7, r8, r9);
        r10.ecParams = r0;
        r11 = r10.ecParams;
        r11 = (java.security.spec.ECParameterSpec) r11;
        r0 = r11.getCurve();
        r0 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertCurve(r0);
        r3 = r11.getGenerator();
        r2 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertPoint(r0, r3, r2);
        r3 = new org.bouncycastle.crypto.params.ECKeyGenerationParameters;
        r4 = new org.bouncycastle.crypto.params.ECDomainParameters;
        r5 = r11.getOrder();
        r11 = r11.getCofactor();
        r6 = (long) r11;
        r11 = java.math.BigInteger.valueOf(r6);
        r4.<init>(r0, r2, r5, r11);
        r3.<init>(r4, r12);
        goto L_0x0061;
        return;
    L_0x012c:
        r11 = new java.security.InvalidAlgorithmParameterException;
        r12 = new java.lang.StringBuilder;
        r12.<init>();
        r0 = "unknown curve name: ";
        r12.append(r0);
        r12.append(r4);
        r12 = r12.toString();
        r11.<init>(r12);
        throw r11;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.ecgost.KeyPairGeneratorSpi.initialize(java.security.spec.AlgorithmParameterSpec, java.security.SecureRandom):void");
    }
}
