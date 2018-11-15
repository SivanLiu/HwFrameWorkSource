package org.bouncycastle.jcajce.provider.asymmetric.dh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Enumeration;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.DHParameter;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.DomainParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;

public class BCDHPrivateKey implements DHPrivateKey, PKCS12BagAttributeCarrier {
    static final long serialVersionUID = 311058815616901812L;
    private transient PKCS12BagAttributeCarrierImpl attrCarrier = new PKCS12BagAttributeCarrierImpl();
    private transient DHParameterSpec dhSpec;
    private transient PrivateKeyInfo info;
    private BigInteger x;

    protected BCDHPrivateKey() {
    }

    BCDHPrivateKey(DHPrivateKey dHPrivateKey) {
        this.x = dHPrivateKey.getX();
        this.dhSpec = dHPrivateKey.getParams();
    }

    BCDHPrivateKey(DHPrivateKeySpec dHPrivateKeySpec) {
        this.x = dHPrivateKeySpec.getX();
        this.dhSpec = new DHParameterSpec(dHPrivateKeySpec.getP(), dHPrivateKeySpec.getG());
    }

    public BCDHPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        DHParameterSpec dHParameterSpec;
        ASN1Sequence instance = ASN1Sequence.getInstance(privateKeyInfo.getPrivateKeyAlgorithm().getParameters());
        ASN1Integer aSN1Integer = (ASN1Integer) privateKeyInfo.parsePrivateKey();
        ASN1ObjectIdentifier algorithm = privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm();
        this.info = privateKeyInfo;
        this.x = aSN1Integer.getValue();
        if (algorithm.equals(PKCSObjectIdentifiers.dhKeyAgreement)) {
            DHParameter instance2 = DHParameter.getInstance(instance);
            dHParameterSpec = instance2.getL() != null ? new DHParameterSpec(instance2.getP(), instance2.getG(), instance2.getL().intValue()) : new DHParameterSpec(instance2.getP(), instance2.getG());
        } else if (algorithm.equals(X9ObjectIdentifiers.dhpublicnumber)) {
            DomainParameters instance3 = DomainParameters.getInstance(instance);
            dHParameterSpec = new DHParameterSpec(instance3.getP(), instance3.getG());
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown algorithm type: ");
            stringBuilder.append(algorithm);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.dhSpec = dHParameterSpec;
    }

    BCDHPrivateKey(DHPrivateKeyParameters dHPrivateKeyParameters) {
        this.x = dHPrivateKeyParameters.getX();
        this.dhSpec = new DHParameterSpec(dHPrivateKeyParameters.getParameters().getP(), dHPrivateKeyParameters.getParameters().getG(), dHPrivateKeyParameters.getParameters().getL());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.dhSpec = new DHParameterSpec((BigInteger) objectInputStream.readObject(), (BigInteger) objectInputStream.readObject(), objectInputStream.readInt());
        this.info = null;
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.dhSpec.getP());
        objectOutputStream.writeObject(this.dhSpec.getG());
        objectOutputStream.writeInt(this.dhSpec.getL());
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof DHPrivateKey)) {
            return false;
        }
        DHPrivateKey dHPrivateKey = (DHPrivateKey) obj;
        if (getX().equals(dHPrivateKey.getX()) && getParams().getG().equals(dHPrivateKey.getParams().getG()) && getParams().getP().equals(dHPrivateKey.getParams().getP()) && getParams().getL() == dHPrivateKey.getParams().getL()) {
            z = true;
        }
        return z;
    }

    public String getAlgorithm() {
        return "DH";
    }

    public ASN1Encodable getBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.attrCarrier.getBagAttribute(aSN1ObjectIdentifier);
    }

    public Enumeration getBagAttributeKeys() {
        return this.attrCarrier.getBagAttributeKeys();
    }

    public byte[] getEncoded() {
        try {
            return this.info != null ? this.info.getEncoded(ASN1Encoding.DER) : new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.dhKeyAgreement, new DHParameter(this.dhSpec.getP(), this.dhSpec.getG(), this.dhSpec.getL()).toASN1Primitive()), new ASN1Integer(getX())).getEncoded(ASN1Encoding.DER);
        } catch (Exception e) {
            return null;
        }
    }

    public String getFormat() {
        return "PKCS#8";
    }

    public DHParameterSpec getParams() {
        return this.dhSpec;
    }

    public BigInteger getX() {
        return this.x;
    }

    public int hashCode() {
        return ((getX().hashCode() ^ getParams().getG().hashCode()) ^ getParams().getP().hashCode()) ^ getParams().getL();
    }

    public void setBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.attrCarrier.setBagAttribute(aSN1ObjectIdentifier, aSN1Encodable);
    }
}
