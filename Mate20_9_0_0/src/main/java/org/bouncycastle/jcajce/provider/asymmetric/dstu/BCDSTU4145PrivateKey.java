package org.bouncycastle.jcajce.provider.asymmetric.dstu;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.ua.DSTU4145NamedCurves;
import org.bouncycastle.asn1.ua.UAObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.bouncycastle.jce.interfaces.ECPointEncoder;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECCurve;

public class BCDSTU4145PrivateKey implements ECPrivateKey, org.bouncycastle.jce.interfaces.ECPrivateKey, PKCS12BagAttributeCarrier, ECPointEncoder {
    static final long serialVersionUID = 7245981689601667138L;
    private String algorithm;
    private transient PKCS12BagAttributeCarrierImpl attrCarrier;
    private transient BigInteger d;
    private transient ECParameterSpec ecSpec;
    private transient DERBitString publicKey;
    private boolean withCompression;

    protected BCDSTU4145PrivateKey() {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    public BCDSTU4145PrivateKey(String str, ECPrivateKeyParameters eCPrivateKeyParameters) {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        this.algorithm = str;
        this.d = eCPrivateKeyParameters.getD();
        this.ecSpec = null;
    }

    public BCDSTU4145PrivateKey(String str, ECPrivateKeyParameters eCPrivateKeyParameters, BCDSTU4145PublicKey bCDSTU4145PublicKey, ECParameterSpec eCParameterSpec) {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        ECDomainParameters parameters = eCPrivateKeyParameters.getParameters();
        this.algorithm = str;
        this.d = eCPrivateKeyParameters.getD();
        if (eCParameterSpec == null) {
            this.ecSpec = new ECParameterSpec(EC5Util.convertCurve(parameters.getCurve(), parameters.getSeed()), EC5Util.convertPoint(parameters.getG()), parameters.getN(), parameters.getH().intValue());
        } else {
            this.ecSpec = eCParameterSpec;
        }
        this.publicKey = getPublicKeyDetails(bCDSTU4145PublicKey);
    }

    public BCDSTU4145PrivateKey(String str, ECPrivateKeyParameters eCPrivateKeyParameters, BCDSTU4145PublicKey bCDSTU4145PublicKey, org.bouncycastle.jce.spec.ECParameterSpec eCParameterSpec) {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        ECDomainParameters parameters = eCPrivateKeyParameters.getParameters();
        this.algorithm = str;
        this.d = eCPrivateKeyParameters.getD();
        this.ecSpec = eCParameterSpec == null ? new ECParameterSpec(EC5Util.convertCurve(parameters.getCurve(), parameters.getSeed()), EC5Util.convertPoint(parameters.getG()), parameters.getN(), parameters.getH().intValue()) : new ECParameterSpec(EC5Util.convertCurve(eCParameterSpec.getCurve(), eCParameterSpec.getSeed()), EC5Util.convertPoint(eCParameterSpec.getG()), eCParameterSpec.getN(), eCParameterSpec.getH().intValue());
        this.publicKey = getPublicKeyDetails(bCDSTU4145PublicKey);
    }

    public BCDSTU4145PrivateKey(ECPrivateKey eCPrivateKey) {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        this.d = eCPrivateKey.getS();
        this.algorithm = eCPrivateKey.getAlgorithm();
        this.ecSpec = eCPrivateKey.getParams();
    }

    public BCDSTU4145PrivateKey(ECPrivateKeySpec eCPrivateKeySpec) {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        this.d = eCPrivateKeySpec.getS();
        this.ecSpec = eCPrivateKeySpec.getParams();
    }

    BCDSTU4145PrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        populateFromPrivKeyInfo(privateKeyInfo);
    }

    public BCDSTU4145PrivateKey(BCDSTU4145PrivateKey bCDSTU4145PrivateKey) {
        this.algorithm = "DSTU4145";
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        this.d = bCDSTU4145PrivateKey.d;
        this.ecSpec = bCDSTU4145PrivateKey.ecSpec;
        this.withCompression = bCDSTU4145PrivateKey.withCompression;
        this.attrCarrier = bCDSTU4145PrivateKey.attrCarrier;
        this.publicKey = bCDSTU4145PrivateKey.publicKey;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x003b in {2, 4, 5} preds:[]
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
    public BCDSTU4145PrivateKey(org.bouncycastle.jce.spec.ECPrivateKeySpec r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = "DSTU4145";
        r2.algorithm = r0;
        r0 = new org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
        r0.<init>();
        r2.attrCarrier = r0;
        r0 = r3.getD();
        r2.d = r0;
        r0 = r3.getParams();
        if (r0 == 0) goto L_0x0039;
    L_0x001a:
        r0 = r3.getParams();
        r0 = r0.getCurve();
        r1 = r3.getParams();
        r1 = r1.getSeed();
        r0 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertCurve(r0, r1);
        r3 = r3.getParams();
        r3 = org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util.convertSpec(r0, r3);
    L_0x0036:
        r2.ecSpec = r3;
        return;
    L_0x0039:
        r3 = 0;
        goto L_0x0036;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.dstu.BCDSTU4145PrivateKey.<init>(org.bouncycastle.jce.spec.ECPrivateKeySpec):void");
    }

    private DERBitString getPublicKeyDetails(BCDSTU4145PublicKey bCDSTU4145PublicKey) {
        try {
            return SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(bCDSTU4145PublicKey.getEncoded())).getPublicKeyData();
        } catch (IOException e) {
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x00b6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void populateFromPrivKeyInfo(PrivateKeyInfo privateKeyInfo) throws IOException {
        ASN1Encodable parsePrivateKey;
        ECParameterSpec eCParameterSpec;
        X962Parameters x962Parameters = new X962Parameters((ASN1Primitive) privateKeyInfo.getPrivateKeyAlgorithm().getParameters());
        if (x962Parameters.isNamedCurve()) {
            ASN1ObjectIdentifier instance = ASN1ObjectIdentifier.getInstance(x962Parameters.getParameters());
            X9ECParameters namedCurveByOid = ECUtil.getNamedCurveByOid(instance);
            ECParameterSpec eCNamedCurveSpec;
            if (namedCurveByOid == null) {
                ECDomainParameters byOID = DSTU4145NamedCurves.getByOID(instance);
                eCNamedCurveSpec = new ECNamedCurveSpec(instance.getId(), EC5Util.convertCurve(byOID.getCurve(), byOID.getSeed()), EC5Util.convertPoint(byOID.getG()), byOID.getN(), byOID.getH());
            } else {
                eCNamedCurveSpec = new ECNamedCurveSpec(ECUtil.getCurveName(instance), EC5Util.convertCurve(namedCurveByOid.getCurve(), namedCurveByOid.getSeed()), EC5Util.convertPoint(namedCurveByOid.getG()), namedCurveByOid.getN(), namedCurveByOid.getH());
            }
        } else if (x962Parameters.isImplicitlyCA()) {
            this.ecSpec = null;
            parsePrivateKey = privateKeyInfo.parsePrivateKey();
            if (parsePrivateKey instanceof ASN1Integer) {
                org.bouncycastle.asn1.sec.ECPrivateKey instance2 = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(parsePrivateKey);
                this.d = instance2.getKey();
                this.publicKey = instance2.getPublicKey();
                return;
            }
            this.d = ASN1Integer.getInstance(parsePrivateKey).getValue();
            return;
        } else {
            X9ECParameters instance3 = X9ECParameters.getInstance(x962Parameters.getParameters());
            eCParameterSpec = new ECParameterSpec(EC5Util.convertCurve(instance3.getCurve(), instance3.getSeed()), EC5Util.convertPoint(instance3.getG()), instance3.getN(), instance3.getH().intValue());
        }
        this.ecSpec = eCParameterSpec;
        parsePrivateKey = privateKeyInfo.parsePrivateKey();
        if (parsePrivateKey instanceof ASN1Integer) {
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        populateFromPrivKeyInfo(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray((byte[]) objectInputStream.readObject())));
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(getEncoded());
    }

    org.bouncycastle.jce.spec.ECParameterSpec engineGetSpec() {
        return this.ecSpec != null ? EC5Util.convertSpec(this.ecSpec, this.withCompression) : BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof BCDSTU4145PrivateKey)) {
            return false;
        }
        BCDSTU4145PrivateKey bCDSTU4145PrivateKey = (BCDSTU4145PrivateKey) obj;
        if (getD().equals(bCDSTU4145PrivateKey.getD()) && engineGetSpec().equals(bCDSTU4145PrivateKey.engineGetSpec())) {
            z = true;
        }
        return z;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public ASN1Encodable getBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.attrCarrier.getBagAttribute(aSN1ObjectIdentifier);
    }

    public Enumeration getBagAttributeKeys() {
        return this.attrCarrier.getBagAttributeKeys();
    }

    public BigInteger getD() {
        return this.d;
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x009b  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x008f  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x00c3 A:{Catch:{ IOException -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x00ae A:{Catch:{ IOException -> 0x00de }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] getEncoded() {
        ASN1Encodable x962Parameters;
        int orderBitLength;
        org.bouncycastle.asn1.sec.ECPrivateKey eCPrivateKey;
        if (this.ecSpec instanceof ECNamedCurveSpec) {
            ASN1ObjectIdentifier namedCurveOid = ECUtil.getNamedCurveOid(((ECNamedCurveSpec) this.ecSpec).getName());
            if (namedCurveOid == null) {
                namedCurveOid = new ASN1ObjectIdentifier(((ECNamedCurveSpec) this.ecSpec).getName());
            }
            x962Parameters = new X962Parameters(namedCurveOid);
        } else if (this.ecSpec == null) {
            x962Parameters = new X962Parameters(DERNull.INSTANCE);
            orderBitLength = ECUtil.getOrderBitLength(BouncyCastleProvider.CONFIGURATION, null, getS());
            eCPrivateKey = this.publicKey == null ? new org.bouncycastle.asn1.sec.ECPrivateKey(orderBitLength, getS(), this.publicKey, x962Parameters) : new org.bouncycastle.asn1.sec.ECPrivateKey(orderBitLength, getS(), x962Parameters);
            return (this.algorithm.equals("DSTU4145") ? new PrivateKeyInfo(new AlgorithmIdentifier(UAObjectIdentifiers.dstu4145be, x962Parameters.toASN1Primitive()), eCPrivateKey.toASN1Primitive()) : new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, x962Parameters.toASN1Primitive()), eCPrivateKey.toASN1Primitive())).getEncoded(ASN1Encoding.DER);
        } else {
            ECCurve convertCurve = EC5Util.convertCurve(this.ecSpec.getCurve());
            x962Parameters = new X962Parameters(new X9ECParameters(convertCurve, EC5Util.convertPoint(convertCurve, this.ecSpec.getGenerator(), this.withCompression), this.ecSpec.getOrder(), BigInteger.valueOf((long) this.ecSpec.getCofactor()), this.ecSpec.getCurve().getSeed()));
        }
        orderBitLength = ECUtil.getOrderBitLength(BouncyCastleProvider.CONFIGURATION, this.ecSpec.getOrder(), getS());
        if (this.publicKey == null) {
        }
        try {
            if (this.algorithm.equals("DSTU4145")) {
            }
            return (this.algorithm.equals("DSTU4145") ? new PrivateKeyInfo(new AlgorithmIdentifier(UAObjectIdentifiers.dstu4145be, x962Parameters.toASN1Primitive()), eCPrivateKey.toASN1Primitive()) : new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, x962Parameters.toASN1Primitive()), eCPrivateKey.toASN1Primitive())).getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            return null;
        }
    }

    public String getFormat() {
        return "PKCS#8";
    }

    public org.bouncycastle.jce.spec.ECParameterSpec getParameters() {
        return this.ecSpec == null ? null : EC5Util.convertSpec(this.ecSpec, this.withCompression);
    }

    public ECParameterSpec getParams() {
        return this.ecSpec;
    }

    public BigInteger getS() {
        return this.d;
    }

    public int hashCode() {
        return getD().hashCode() ^ engineGetSpec().hashCode();
    }

    public void setBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.attrCarrier.setBagAttribute(aSN1ObjectIdentifier, aSN1Encodable);
    }

    public void setPointFormat(String str) {
        this.withCompression = "UNCOMPRESSED".equalsIgnoreCase(str) ^ 1;
    }

    public String toString() {
        return ECUtil.privateKeyToString(this.algorithm, this.d, engineGetSpec());
    }
}
