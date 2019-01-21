package org.bouncycastle.jce.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves;
import org.bouncycastle.asn1.cryptopro.GOST3410PublicKeyAlgParameters;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ECPoint;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.bouncycastle.jce.ECGOST3410NamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPointEncoder;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Strings;

public class JCEECPublicKey implements ECPublicKey, org.bouncycastle.jce.interfaces.ECPublicKey, ECPointEncoder {
    private String algorithm;
    private ECParameterSpec ecSpec;
    private GOST3410PublicKeyAlgParameters gostParams;
    private ECPoint q;
    private boolean withCompression;

    public JCEECPublicKey(String str, ECPublicKeySpec eCPublicKeySpec) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.ecSpec = eCPublicKeySpec.getParams();
        this.q = EC5Util.convertPoint(this.ecSpec, eCPublicKeySpec.getW(), false);
    }

    public JCEECPublicKey(String str, ECPublicKeyParameters eCPublicKeyParameters) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.q = eCPublicKeyParameters.getQ();
        this.ecSpec = null;
    }

    public JCEECPublicKey(String str, ECPublicKeyParameters eCPublicKeyParameters, ECParameterSpec eCParameterSpec) {
        this.algorithm = "EC";
        ECDomainParameters parameters = eCPublicKeyParameters.getParameters();
        this.algorithm = str;
        this.q = eCPublicKeyParameters.getQ();
        if (eCParameterSpec == null) {
            this.ecSpec = createSpec(EC5Util.convertCurve(parameters.getCurve(), parameters.getSeed()), parameters);
        } else {
            this.ecSpec = eCParameterSpec;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0039 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public JCEECPublicKey(java.lang.String r2, org.bouncycastle.crypto.params.ECPublicKeyParameters r3, org.bouncycastle.jce.spec.ECParameterSpec r4) {
        /*
        r1 = this;
        r1.<init>();
        r0 = "EC";
        r1.algorithm = r0;
        r0 = r3.getParameters();
        r1.algorithm = r2;
        r2 = r3.getQ();
        r1.q = r2;
        if (r4 != 0) goto L_0x0028;
        r2 = r0.getCurve();
        r3 = r0.getSeed();
        r2 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertCurve(r2, r3);
        r2 = r1.createSpec(r2, r0);
        r1.ecSpec = r2;
        return;
        r2 = r4.getCurve();
        r3 = r4.getSeed();
        r2 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertCurve(r2, r3);
        r2 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertSpec(r2, r4);
        goto L_0x0025;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.JCEECPublicKey.<init>(java.lang.String, org.bouncycastle.crypto.params.ECPublicKeyParameters, org.bouncycastle.jce.spec.ECParameterSpec):void");
    }

    public JCEECPublicKey(String str, JCEECPublicKey jCEECPublicKey) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.q = jCEECPublicKey.q;
        this.ecSpec = jCEECPublicKey.ecSpec;
        this.withCompression = jCEECPublicKey.withCompression;
        this.gostParams = jCEECPublicKey.gostParams;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:9:0x0063 in {2, 4, 7, 8} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public JCEECPublicKey(java.lang.String r3, org.bouncycastle.jce.spec.ECPublicKeySpec r4) {
        /*
        r2 = this;
        r2.<init>();
        r0 = "EC";
        r2.algorithm = r0;
        r2.algorithm = r3;
        r3 = r4.getQ();
        r2.q = r3;
        r3 = r4.getParams();
        if (r3 == 0) goto L_0x0034;
        r3 = r4.getParams();
        r3 = r3.getCurve();
        r0 = r4.getParams();
        r0 = r0.getSeed();
        r3 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertCurve(r3, r0);
        r4 = r4.getParams();
        r3 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertSpec(r3, r4);
        r2.ecSpec = r3;
        return;
        r3 = r2.q;
        r3 = r3.getCurve();
        if (r3 != 0) goto L_0x0061;
        r3 = org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION;
        r3 = r3.getEcImplicitlyCa();
        r3 = r3.getCurve();
        r4 = r2.q;
        r4 = r4.getAffineXCoord();
        r4 = r4.toBigInteger();
        r0 = r2.q;
        r0 = r0.getAffineYCoord();
        r0 = r0.toBigInteger();
        r1 = 0;
        r3 = r3.createPoint(r4, r0, r1);
        r2.q = r3;
        r3 = 0;
        goto L_0x0031;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.JCEECPublicKey.<init>(java.lang.String, org.bouncycastle.jce.spec.ECPublicKeySpec):void");
    }

    public JCEECPublicKey(ECPublicKey eCPublicKey) {
        this.algorithm = "EC";
        this.algorithm = eCPublicKey.getAlgorithm();
        this.ecSpec = eCPublicKey.getParams();
        this.q = EC5Util.convertPoint(this.ecSpec, eCPublicKey.getW(), false);
    }

    JCEECPublicKey(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        this.algorithm = "EC";
        populateFromPubKeyInfo(subjectPublicKeyInfo);
    }

    private ECParameterSpec createSpec(EllipticCurve ellipticCurve, ECDomainParameters eCDomainParameters) {
        return new ECParameterSpec(ellipticCurve, EC5Util.convertPoint(eCDomainParameters.getG()), eCDomainParameters.getN(), eCDomainParameters.getH().intValue());
    }

    private void extractBytes(byte[] bArr, int i, BigInteger bigInteger) {
        byte[] toByteArray = bigInteger.toByteArray();
        int i2 = 0;
        if (toByteArray.length < 32) {
            byte[] bArr2 = new byte[32];
            System.arraycopy(toByteArray, 0, bArr2, bArr2.length - toByteArray.length, toByteArray.length);
            toByteArray = bArr2;
        }
        while (i2 != 32) {
            bArr[i + i2] = toByteArray[(toByteArray.length - 1) - i2];
            i2++;
        }
    }

    private void populateFromPubKeyInfo(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        int i = 1;
        if (subjectPublicKeyInfo.getAlgorithmId().getAlgorithm().equals(CryptoProObjectIdentifiers.gostR3410_2001)) {
            DERBitString publicKeyData = subjectPublicKeyInfo.getPublicKeyData();
            this.algorithm = "ECGOST3410";
            try {
                byte[] octets = ((ASN1OctetString) ASN1Primitive.fromByteArray(publicKeyData.getBytes())).getOctets();
                byte[] bArr = new byte[65];
                bArr[0] = (byte) 4;
                while (i <= 32) {
                    bArr[i] = octets[32 - i];
                    bArr[i + 32] = octets[64 - i];
                    i++;
                }
                this.gostParams = new GOST3410PublicKeyAlgParameters((ASN1Sequence) subjectPublicKeyInfo.getAlgorithmId().getParameters());
                ECNamedCurveParameterSpec parameterSpec = ECGOST3410NamedCurveTable.getParameterSpec(ECGOST3410NamedCurves.getName(this.gostParams.getPublicKeyParamSet()));
                ECCurve curve = parameterSpec.getCurve();
                EllipticCurve convertCurve = EC5Util.convertCurve(curve, parameterSpec.getSeed());
                this.q = curve.decodePoint(bArr);
                this.ecSpec = new ECNamedCurveSpec(ECGOST3410NamedCurves.getName(this.gostParams.getPublicKeyParamSet()), convertCurve, EC5Util.convertPoint(parameterSpec.getG()), parameterSpec.getN(), parameterSpec.getH());
                return;
            } catch (IOException e) {
                throw new IllegalArgumentException("error recovering public key");
            }
        }
        ECCurve curve2;
        byte[] bytes;
        ASN1OctetString dEROctetString;
        ECParameterSpec eCParameterSpec;
        X962Parameters x962Parameters = new X962Parameters((ASN1Primitive) subjectPublicKeyInfo.getAlgorithmId().getParameters());
        if (x962Parameters.isNamedCurve()) {
            ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) x962Parameters.getParameters();
            X9ECParameters namedCurveByOid = ECUtil.getNamedCurveByOid(aSN1ObjectIdentifier);
            curve2 = namedCurveByOid.getCurve();
            ECParameterSpec eCNamedCurveSpec = new ECNamedCurveSpec(ECUtil.getCurveName(aSN1ObjectIdentifier), EC5Util.convertCurve(curve2, namedCurveByOid.getSeed()), EC5Util.convertPoint(namedCurveByOid.getG()), namedCurveByOid.getN(), namedCurveByOid.getH());
        } else if (x962Parameters.isImplicitlyCA()) {
            this.ecSpec = null;
            curve2 = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa().getCurve();
            bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
            dEROctetString = new DEROctetString(bytes);
            if (bytes[0] == (byte) 4 && bytes[1] == bytes.length - 2 && ((bytes[2] == (byte) 2 || bytes[2] == (byte) 3) && new X9IntegerConverter().getByteLength(curve2) >= bytes.length - 3)) {
                dEROctetString = (ASN1OctetString) ASN1Primitive.fromByteArray(bytes);
            }
            this.q = new X9ECPoint(curve2, dEROctetString).getPoint();
        } else {
            X9ECParameters instance = X9ECParameters.getInstance(x962Parameters.getParameters());
            curve2 = instance.getCurve();
            eCParameterSpec = new ECParameterSpec(EC5Util.convertCurve(curve2, instance.getSeed()), EC5Util.convertPoint(instance.getG()), instance.getN(), instance.getH().intValue());
        }
        this.ecSpec = eCParameterSpec;
        bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        dEROctetString = new DEROctetString(bytes);
        try {
            dEROctetString = (ASN1OctetString) ASN1Primitive.fromByteArray(bytes);
            this.q = new X9ECPoint(curve2, dEROctetString).getPoint();
        } catch (IOException e2) {
            throw new IllegalArgumentException("error recovering public key");
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        populateFromPubKeyInfo(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray((byte[]) objectInputStream.readObject())));
        this.algorithm = (String) objectInputStream.readObject();
        this.withCompression = objectInputStream.readBoolean();
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(getEncoded());
        objectOutputStream.writeObject(this.algorithm);
        objectOutputStream.writeBoolean(this.withCompression);
    }

    public ECPoint engineGetQ() {
        return this.q;
    }

    org.bouncycastle.jce.spec.ECParameterSpec engineGetSpec() {
        return this.ecSpec != null ? EC5Util.convertSpec(this.ecSpec, this.withCompression) : BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof JCEECPublicKey)) {
            return false;
        }
        JCEECPublicKey jCEECPublicKey = (JCEECPublicKey) obj;
        if (engineGetQ().equals(jCEECPublicKey.engineGetQ()) && engineGetSpec().equals(jCEECPublicKey.engineGetSpec())) {
            z = true;
        }
        return z;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public byte[] getEncoded() {
        SubjectPublicKeyInfo subjectPublicKeyInfo;
        ECCurve convertCurve;
        if (this.algorithm.equals("ECGOST3410")) {
            ASN1Encodable aSN1Encodable;
            if (this.gostParams != null) {
                aSN1Encodable = this.gostParams;
            } else if (this.ecSpec instanceof ECNamedCurveSpec) {
                aSN1Encodable = new GOST3410PublicKeyAlgParameters(ECGOST3410NamedCurves.getOID(((ECNamedCurveSpec) this.ecSpec).getName()), CryptoProObjectIdentifiers.gostR3411_94_CryptoProParamSet);
            } else {
                convertCurve = EC5Util.convertCurve(this.ecSpec.getCurve());
                aSN1Encodable = new X962Parameters(new X9ECParameters(convertCurve, EC5Util.convertPoint(convertCurve, this.ecSpec.getGenerator(), this.withCompression), this.ecSpec.getOrder(), BigInteger.valueOf((long) this.ecSpec.getCofactor()), this.ecSpec.getCurve().getSeed()));
            }
            BigInteger toBigInteger = this.q.getAffineXCoord().toBigInteger();
            BigInteger toBigInteger2 = this.q.getAffineYCoord().toBigInteger();
            byte[] bArr = new byte[64];
            extractBytes(bArr, 0, toBigInteger);
            extractBytes(bArr, 32, toBigInteger2);
            try {
                subjectPublicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(CryptoProObjectIdentifiers.gostR3410_2001, aSN1Encodable), new DEROctetString(bArr));
            } catch (IOException e) {
                return null;
            }
        }
        ASN1Encodable x962Parameters;
        if (this.ecSpec instanceof ECNamedCurveSpec) {
            ASN1ObjectIdentifier namedCurveOid = ECUtil.getNamedCurveOid(((ECNamedCurveSpec) this.ecSpec).getName());
            if (namedCurveOid == null) {
                namedCurveOid = new ASN1ObjectIdentifier(((ECNamedCurveSpec) this.ecSpec).getName());
            }
            x962Parameters = new X962Parameters(namedCurveOid);
        } else if (this.ecSpec == null) {
            x962Parameters = new X962Parameters(DERNull.INSTANCE);
        } else {
            convertCurve = EC5Util.convertCurve(this.ecSpec.getCurve());
            x962Parameters = new X962Parameters(new X9ECParameters(convertCurve, EC5Util.convertPoint(convertCurve, this.ecSpec.getGenerator(), this.withCompression), this.ecSpec.getOrder(), BigInteger.valueOf((long) this.ecSpec.getCofactor()), this.ecSpec.getCurve().getSeed()));
        }
        subjectPublicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, x962Parameters), ((ASN1OctetString) new X9ECPoint(engineGetQ().getCurve().createPoint(getQ().getAffineXCoord().toBigInteger(), getQ().getAffineYCoord().toBigInteger(), this.withCompression)).toASN1Primitive()).getOctets());
        return KeyUtil.getEncodedSubjectPublicKeyInfo(subjectPublicKeyInfo);
    }

    public String getFormat() {
        return "X.509";
    }

    public org.bouncycastle.jce.spec.ECParameterSpec getParameters() {
        return this.ecSpec == null ? null : EC5Util.convertSpec(this.ecSpec, this.withCompression);
    }

    public ECParameterSpec getParams() {
        return this.ecSpec;
    }

    public ECPoint getQ() {
        return this.ecSpec == null ? this.q.getDetachedPoint() : this.q;
    }

    public java.security.spec.ECPoint getW() {
        return EC5Util.convertPoint(this.q);
    }

    public int hashCode() {
        return engineGetQ().hashCode() ^ engineGetSpec().hashCode();
    }

    public void setPointFormat(String str) {
        this.withCompression = "UNCOMPRESSED".equalsIgnoreCase(str) ^ 1;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        String lineSeparator = Strings.lineSeparator();
        stringBuffer.append("EC Public Key");
        stringBuffer.append(lineSeparator);
        stringBuffer.append("            X: ");
        stringBuffer.append(this.q.getAffineXCoord().toBigInteger().toString(16));
        stringBuffer.append(lineSeparator);
        stringBuffer.append("            Y: ");
        stringBuffer.append(this.q.getAffineYCoord().toBigInteger().toString(16));
        stringBuffer.append(lineSeparator);
        return stringBuffer.toString();
    }
}
