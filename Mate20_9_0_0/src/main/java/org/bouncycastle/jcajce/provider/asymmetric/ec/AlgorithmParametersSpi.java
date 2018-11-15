package org.bouncycastle.jcajce.provider.asymmetric.ec;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECCurve;

public class AlgorithmParametersSpi extends java.security.AlgorithmParametersSpi {
    private String curveName;
    private ECParameterSpec ecParameterSpec;

    protected byte[] engineGetEncoded() throws IOException {
        return engineGetEncoded("ASN.1");
    }

    protected byte[] engineGetEncoded(String str) throws IOException {
        if (isASN1FormatString(str)) {
            X962Parameters x962Parameters;
            if (this.ecParameterSpec == null) {
                x962Parameters = new X962Parameters(DERNull.INSTANCE);
            } else if (this.curveName != null) {
                x962Parameters = new X962Parameters(ECUtil.getNamedCurveOid(this.curveName));
            } else {
                org.bouncycastle.jce.spec.ECParameterSpec convertSpec = EC5Util.convertSpec(this.ecParameterSpec, false);
                x962Parameters = new X962Parameters(new X9ECParameters(convertSpec.getCurve(), convertSpec.getG(), convertSpec.getN(), convertSpec.getH(), convertSpec.getSeed()));
            }
            return x962Parameters.getEncoded();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown parameters format in AlgorithmParameters object: ");
        stringBuilder.append(str);
        throw new IOException(stringBuilder.toString());
    }

    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> cls) throws InvalidParameterSpecException {
        if (ECParameterSpec.class.isAssignableFrom(cls) || cls == AlgorithmParameterSpec.class) {
            return this.ecParameterSpec;
        }
        if (ECGenParameterSpec.class.isAssignableFrom(cls)) {
            if (this.curveName != null) {
                ASN1ObjectIdentifier namedCurveOid = ECUtil.getNamedCurveOid(this.curveName);
                return namedCurveOid != null ? new ECGenParameterSpec(namedCurveOid.getId()) : new ECGenParameterSpec(this.curveName);
            } else {
                ASN1ObjectIdentifier namedCurveOid2 = ECUtil.getNamedCurveOid(EC5Util.convertSpec(this.ecParameterSpec, false));
                if (namedCurveOid2 != null) {
                    return new ECGenParameterSpec(namedCurveOid2.getId());
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("EC AlgorithmParameters cannot convert to ");
        stringBuilder.append(cls.getName());
        throw new InvalidParameterSpecException(stringBuilder.toString());
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:17:0x004b in {4, 6, 8, 13, 15, 16, 19} preds:[]
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
    protected void engineInit(java.security.spec.AlgorithmParameterSpec r4) throws java.security.spec.InvalidParameterSpecException {
        /*
        r3 = this;
        r0 = r4 instanceof java.security.spec.ECGenParameterSpec;
        if (r0 == 0) goto L_0x0034;
    L_0x0004:
        r4 = (java.security.spec.ECGenParameterSpec) r4;
        r0 = org.bouncycastle.jcajce.provider.asymmetric.ec.ECUtils.getDomainParametersFromGenSpec(r4);
        if (r0 == 0) goto L_0x0019;
    L_0x000c:
        r4 = r4.getName();
        r3.curveName = r4;
        r4 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertToSpec(r0);
    L_0x0016:
        r3.ecParameterSpec = r4;
        return;
    L_0x0019:
        r0 = new java.security.spec.InvalidParameterSpecException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "EC curve name not recognized: ";
        r1.append(r2);
        r4 = r4.getName();
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
    L_0x0034:
        r0 = r4 instanceof java.security.spec.ECParameterSpec;
        if (r0 == 0) goto L_0x004c;
    L_0x0038:
        r0 = r4 instanceof org.bouncycastle.jce.spec.ECNamedCurveSpec;
        if (r0 == 0) goto L_0x0046;
    L_0x003c:
        r0 = r4;
        r0 = (org.bouncycastle.jce.spec.ECNamedCurveSpec) r0;
        r0 = r0.getName();
    L_0x0043:
        r3.curveName = r0;
        goto L_0x0048;
    L_0x0046:
        r0 = 0;
        goto L_0x0043;
    L_0x0048:
        r4 = (java.security.spec.ECParameterSpec) r4;
        goto L_0x0016;
        return;
    L_0x004c:
        r0 = new java.security.spec.InvalidParameterSpecException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "AlgorithmParameterSpec class not recognized: ";
        r1.append(r2);
        r4 = r4.getClass();
        r4 = r4.getName();
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.ec.AlgorithmParametersSpi.engineInit(java.security.spec.AlgorithmParameterSpec):void");
    }

    protected void engineInit(byte[] bArr) throws IOException {
        engineInit(bArr, "ASN.1");
    }

    protected void engineInit(byte[] bArr, String str) throws IOException {
        if (isASN1FormatString(str)) {
            X962Parameters instance = X962Parameters.getInstance(bArr);
            ECCurve curve = EC5Util.getCurve(BouncyCastleProvider.CONFIGURATION, instance);
            if (instance.isNamedCurve()) {
                ASN1ObjectIdentifier instance2 = ASN1ObjectIdentifier.getInstance(instance.getParameters());
                this.curveName = ECNamedCurveTable.getName(instance2);
                if (this.curveName == null) {
                    this.curveName = instance2.getId();
                }
            }
            this.ecParameterSpec = EC5Util.convertToSpec(instance, curve);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown encoded parameters format in AlgorithmParameters object: ");
        stringBuilder.append(str);
        throw new IOException(stringBuilder.toString());
    }

    protected String engineToString() {
        return "EC AlgorithmParameters ";
    }

    protected boolean isASN1FormatString(String str) {
        return str == null || str.equals("ASN.1");
    }
}
