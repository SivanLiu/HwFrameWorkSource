package org.bouncycastle.jcajce.provider.asymmetric.util;

import java.math.BigInteger;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.F2m;
import org.bouncycastle.math.ec.ECCurve.Fp;
import org.bouncycastle.math.field.FiniteField;
import org.bouncycastle.math.field.Polynomial;
import org.bouncycastle.math.field.PolynomialExtensionField;
import org.bouncycastle.util.Arrays;

public class EC5Util {
    private static Map customCurves = new HashMap();

    static {
        Enumeration names = CustomNamedCurves.getNames();
        while (names.hasMoreElements()) {
            String str = (String) names.nextElement();
            X9ECParameters byName = ECNamedCurveTable.getByName(str);
            if (byName != null) {
                customCurves.put(byName.getCurve(), CustomNamedCurves.getByName(str).getCurve());
            }
        }
        X9ECParameters byName2 = CustomNamedCurves.getByName("Curve25519");
        customCurves.put(new Fp(byName2.getCurve().getField().getCharacteristic(), byName2.getCurve().getA().toBigInteger(), byName2.getCurve().getB().toBigInteger()), byName2.getCurve());
    }

    public static EllipticCurve convertCurve(ECCurve eCCurve, byte[] bArr) {
        return new EllipticCurve(convertField(eCCurve.getField()), eCCurve.getA().toBigInteger(), eCCurve.getB().toBigInteger(), null);
    }

    public static ECCurve convertCurve(EllipticCurve ellipticCurve) {
        ECField field = ellipticCurve.getField();
        BigInteger a = ellipticCurve.getA();
        BigInteger b = ellipticCurve.getB();
        if (field instanceof ECFieldFp) {
            ECCurve fp = new Fp(((ECFieldFp) field).getP(), a, b);
            if (customCurves.containsKey(fp)) {
                fp = (ECCurve) customCurves.get(fp);
            }
            return fp;
        }
        ECFieldF2m eCFieldF2m = (ECFieldF2m) field;
        int m = eCFieldF2m.getM();
        int[] convertMidTerms = ECUtil.convertMidTerms(eCFieldF2m.getMidTermsOfReductionPolynomial());
        return new F2m(m, convertMidTerms[0], convertMidTerms[1], convertMidTerms[2], a, b);
    }

    public static ECField convertField(FiniteField finiteField) {
        if (ECAlgorithms.isFpField(finiteField)) {
            return new ECFieldFp(finiteField.getCharacteristic());
        }
        Polynomial minimalPolynomial = ((PolynomialExtensionField) finiteField).getMinimalPolynomial();
        int[] exponentsPresent = minimalPolynomial.getExponentsPresent();
        return new ECFieldF2m(minimalPolynomial.getDegree(), Arrays.reverse(Arrays.copyOfRange(exponentsPresent, 1, exponentsPresent.length - 1)));
    }

    public static ECPoint convertPoint(org.bouncycastle.math.ec.ECPoint eCPoint) {
        eCPoint = eCPoint.normalize();
        return new ECPoint(eCPoint.getAffineXCoord().toBigInteger(), eCPoint.getAffineYCoord().toBigInteger());
    }

    public static org.bouncycastle.math.ec.ECPoint convertPoint(ECParameterSpec eCParameterSpec, ECPoint eCPoint, boolean z) {
        return convertPoint(convertCurve(eCParameterSpec.getCurve()), eCPoint, z);
    }

    public static org.bouncycastle.math.ec.ECPoint convertPoint(ECCurve eCCurve, ECPoint eCPoint, boolean z) {
        return eCCurve.createPoint(eCPoint.getAffineX(), eCPoint.getAffineY());
    }

    public static ECParameterSpec convertSpec(EllipticCurve ellipticCurve, org.bouncycastle.jce.spec.ECParameterSpec eCParameterSpec) {
        if (!(eCParameterSpec instanceof ECNamedCurveParameterSpec)) {
            return new ECParameterSpec(ellipticCurve, convertPoint(eCParameterSpec.getG()), eCParameterSpec.getN(), eCParameterSpec.getH().intValue());
        }
        return new ECNamedCurveSpec(((ECNamedCurveParameterSpec) eCParameterSpec).getName(), ellipticCurve, convertPoint(eCParameterSpec.getG()), eCParameterSpec.getN(), eCParameterSpec.getH());
    }

    public static org.bouncycastle.jce.spec.ECParameterSpec convertSpec(ECParameterSpec eCParameterSpec, boolean z) {
        ECCurve convertCurve = convertCurve(eCParameterSpec.getCurve());
        return new org.bouncycastle.jce.spec.ECParameterSpec(convertCurve, convertPoint(convertCurve, eCParameterSpec.getGenerator(), z), eCParameterSpec.getOrder(), BigInteger.valueOf((long) eCParameterSpec.getCofactor()), eCParameterSpec.getCurve().getSeed());
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:19:0x0093 in {6, 8, 12, 15, 17, 18} preds:[]
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
    public static java.security.spec.ECParameterSpec convertToSpec(org.bouncycastle.asn1.x9.X962Parameters r8, org.bouncycastle.math.ec.ECCurve r9) {
        /*
        r0 = r8.isNamedCurve();
        if (r0 == 0) goto L_0x0047;
    L_0x0006:
        r8 = r8.getParameters();
        r8 = (org.bouncycastle.asn1.ASN1ObjectIdentifier) r8;
        r0 = org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil.getNamedCurveByOid(r8);
        if (r0 != 0) goto L_0x0024;
    L_0x0012:
        r1 = org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION;
        r1 = r1.getAdditionalECParameters();
        r2 = r1.isEmpty();
        if (r2 != 0) goto L_0x0024;
    L_0x001e:
        r0 = r1.get(r8);
        r0 = (org.bouncycastle.asn1.x9.X9ECParameters) r0;
    L_0x0024:
        r1 = r0.getSeed();
        r4 = convertCurve(r9, r1);
        r9 = new org.bouncycastle.jce.spec.ECNamedCurveSpec;
        r3 = org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil.getCurveName(r8);
        r8 = r0.getG();
        r5 = convertPoint(r8);
        r6 = r0.getN();
        r7 = r0.getH();
        r2 = r9;
        r2.<init>(r3, r4, r5, r6, r7);
        return r9;
    L_0x0047:
        r0 = r8.isImplicitlyCA();
        if (r0 == 0) goto L_0x004f;
    L_0x004d:
        r9 = 0;
        return r9;
    L_0x004f:
        r8 = r8.getParameters();
        r8 = org.bouncycastle.asn1.x9.X9ECParameters.getInstance(r8);
        r0 = r8.getSeed();
        r9 = convertCurve(r9, r0);
        r0 = r8.getH();
        if (r0 == 0) goto L_0x0080;
    L_0x0065:
        r0 = new java.security.spec.ECParameterSpec;
        r1 = r8.getG();
        r1 = convertPoint(r1);
        r2 = r8.getN();
        r8 = r8.getH();
        r8 = r8.intValue();
        r0.<init>(r9, r1, r2, r8);
    L_0x007e:
        r9 = r0;
        return r9;
    L_0x0080:
        r0 = new java.security.spec.ECParameterSpec;
        r1 = r8.getG();
        r1 = convertPoint(r1);
        r8 = r8.getN();
        r2 = 1;
        r0.<init>(r9, r1, r8, r2);
        goto L_0x007e;
        return r9;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertToSpec(org.bouncycastle.asn1.x9.X962Parameters, org.bouncycastle.math.ec.ECCurve):java.security.spec.ECParameterSpec");
    }

    public static ECParameterSpec convertToSpec(X9ECParameters x9ECParameters) {
        return new ECParameterSpec(convertCurve(x9ECParameters.getCurve(), null), convertPoint(x9ECParameters.getG()), x9ECParameters.getN(), x9ECParameters.getH().intValue());
    }

    public static ECCurve getCurve(ProviderConfiguration providerConfiguration, X962Parameters x962Parameters) {
        Set acceptableNamedCurves = providerConfiguration.getAcceptableNamedCurves();
        if (x962Parameters.isNamedCurve()) {
            ASN1ObjectIdentifier instance = ASN1ObjectIdentifier.getInstance(x962Parameters.getParameters());
            if (acceptableNamedCurves.isEmpty() || acceptableNamedCurves.contains(instance)) {
                X9ECParameters namedCurveByOid = ECUtil.getNamedCurveByOid(instance);
                if (namedCurveByOid == null) {
                    namedCurveByOid = (X9ECParameters) providerConfiguration.getAdditionalECParameters().get(instance);
                }
                return namedCurveByOid.getCurve();
            }
            throw new IllegalStateException("named curve not acceptable");
        } else if (x962Parameters.isImplicitlyCA()) {
            return providerConfiguration.getEcImplicitlyCa().getCurve();
        } else {
            if (acceptableNamedCurves.isEmpty()) {
                return X9ECParameters.getInstance(x962Parameters.getParameters()).getCurve();
            }
            throw new IllegalStateException("encoded parameters not acceptable");
        }
    }

    public static ECDomainParameters getDomainParameters(ProviderConfiguration providerConfiguration, ECParameterSpec eCParameterSpec) {
        if (eCParameterSpec != null) {
            return ECUtil.getDomainParameters(providerConfiguration, convertSpec(eCParameterSpec, false));
        }
        org.bouncycastle.jce.spec.ECParameterSpec ecImplicitlyCa = providerConfiguration.getEcImplicitlyCa();
        return new ECDomainParameters(ecImplicitlyCa.getCurve(), ecImplicitlyCa.getG(), ecImplicitlyCa.getN(), ecImplicitlyCa.getH(), ecImplicitlyCa.getSeed());
    }
}
