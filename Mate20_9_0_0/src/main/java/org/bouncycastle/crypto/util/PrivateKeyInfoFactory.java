package org.bouncycastle.crypto.util;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECNamedDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

public class PrivateKeyInfoFactory {
    private PrivateKeyInfoFactory() {
    }

    public static PrivateKeyInfo createPrivateKeyInfo(AsymmetricKeyParameter asymmetricKeyParameter) throws IOException {
        if (asymmetricKeyParameter instanceof RSAKeyParameters) {
            RSAPrivateCrtKeyParameters rSAPrivateCrtKeyParameters = (RSAPrivateCrtKeyParameters) asymmetricKeyParameter;
            return new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), new RSAPrivateKey(rSAPrivateCrtKeyParameters.getModulus(), rSAPrivateCrtKeyParameters.getPublicExponent(), rSAPrivateCrtKeyParameters.getExponent(), rSAPrivateCrtKeyParameters.getP(), rSAPrivateCrtKeyParameters.getQ(), rSAPrivateCrtKeyParameters.getDP(), rSAPrivateCrtKeyParameters.getDQ(), rSAPrivateCrtKeyParameters.getQInv()));
        } else if (asymmetricKeyParameter instanceof DSAPrivateKeyParameters) {
            DSAPrivateKeyParameters dSAPrivateKeyParameters = (DSAPrivateKeyParameters) asymmetricKeyParameter;
            DSAParameters parameters = dSAPrivateKeyParameters.getParameters();
            return new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(parameters.getP(), parameters.getQ(), parameters.getG())), new ASN1Integer(dSAPrivateKeyParameters.getX()));
        } else if (asymmetricKeyParameter instanceof ECPrivateKeyParameters) {
            ASN1Encodable x962Parameters;
            int bitLength;
            ECPrivateKeyParameters eCPrivateKeyParameters = (ECPrivateKeyParameters) asymmetricKeyParameter;
            ECDomainParameters parameters2 = eCPrivateKeyParameters.getParameters();
            if (parameters2 == null) {
                x962Parameters = new X962Parameters(DERNull.INSTANCE);
                bitLength = eCPrivateKeyParameters.getD().bitLength();
            } else if (parameters2 instanceof ECNamedDomainParameters) {
                X962Parameters x962Parameters2 = new X962Parameters(((ECNamedDomainParameters) parameters2).getName());
                bitLength = parameters2.getN().bitLength();
                x962Parameters = x962Parameters2;
            } else {
                X962Parameters x962Parameters3 = new X962Parameters(new X9ECParameters(parameters2.getCurve(), parameters2.getG(), parameters2.getN(), parameters2.getH(), parameters2.getSeed()));
                bitLength = parameters2.getN().bitLength();
                x962Parameters = x962Parameters3;
            }
            return new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, x962Parameters), new ECPrivateKey(bitLength, eCPrivateKeyParameters.getD(), x962Parameters));
        } else {
            throw new IOException("key parameters not recognised.");
        }
    }
}
