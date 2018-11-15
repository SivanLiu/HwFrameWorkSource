package org.bouncycastle.jcajce.provider.asymmetric.dstu;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.ua.UAObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseKeyFactorySpi;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

public class KeyFactorySpi extends BaseKeyFactorySpi {
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        return keySpec instanceof ECPrivateKeySpec ? new BCDSTU4145PrivateKey((ECPrivateKeySpec) keySpec) : keySpec instanceof java.security.spec.ECPrivateKeySpec ? new BCDSTU4145PrivateKey((java.security.spec.ECPrivateKeySpec) keySpec) : super.engineGeneratePrivate(keySpec);
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        return keySpec instanceof ECPublicKeySpec ? new BCDSTU4145PublicKey((ECPublicKeySpec) keySpec, BouncyCastleProvider.CONFIGURATION) : keySpec instanceof java.security.spec.ECPublicKeySpec ? new BCDSTU4145PublicKey((java.security.spec.ECPublicKeySpec) keySpec) : super.engineGeneratePublic(keySpec);
    }

    protected KeySpec engineGetKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        ECPublicKey eCPublicKey;
        ECParameterSpec ecImplicitlyCa;
        ECPrivateKey eCPrivateKey;
        if (cls.isAssignableFrom(java.security.spec.ECPublicKeySpec.class) && (key instanceof ECPublicKey)) {
            eCPublicKey = (ECPublicKey) key;
            if (eCPublicKey.getParams() != null) {
                return new java.security.spec.ECPublicKeySpec(eCPublicKey.getW(), eCPublicKey.getParams());
            }
            ecImplicitlyCa = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
            return new java.security.spec.ECPublicKeySpec(eCPublicKey.getW(), EC5Util.convertSpec(EC5Util.convertCurve(ecImplicitlyCa.getCurve(), ecImplicitlyCa.getSeed()), ecImplicitlyCa));
        } else if (cls.isAssignableFrom(java.security.spec.ECPrivateKeySpec.class) && (key instanceof ECPrivateKey)) {
            eCPrivateKey = (ECPrivateKey) key;
            if (eCPrivateKey.getParams() != null) {
                return new java.security.spec.ECPrivateKeySpec(eCPrivateKey.getS(), eCPrivateKey.getParams());
            }
            ecImplicitlyCa = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
            return new java.security.spec.ECPrivateKeySpec(eCPrivateKey.getS(), EC5Util.convertSpec(EC5Util.convertCurve(ecImplicitlyCa.getCurve(), ecImplicitlyCa.getSeed()), ecImplicitlyCa));
        } else if (cls.isAssignableFrom(ECPublicKeySpec.class) && (key instanceof ECPublicKey)) {
            eCPublicKey = (ECPublicKey) key;
            if (eCPublicKey.getParams() != null) {
                return new ECPublicKeySpec(EC5Util.convertPoint(eCPublicKey.getParams(), eCPublicKey.getW(), false), EC5Util.convertSpec(eCPublicKey.getParams(), false));
            }
            return new ECPublicKeySpec(EC5Util.convertPoint(eCPublicKey.getParams(), eCPublicKey.getW(), false), BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa());
        } else if (!cls.isAssignableFrom(ECPrivateKeySpec.class) || !(key instanceof ECPrivateKey)) {
            return super.engineGetKeySpec(key, cls);
        } else {
            eCPrivateKey = (ECPrivateKey) key;
            if (eCPrivateKey.getParams() != null) {
                return new ECPrivateKeySpec(eCPrivateKey.getS(), EC5Util.convertSpec(eCPrivateKey.getParams(), false));
            }
            return new ECPrivateKeySpec(eCPrivateKey.getS(), BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa());
        }
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        throw new InvalidKeyException("key type unknown");
    }

    public PrivateKey generatePrivate(PrivateKeyInfo privateKeyInfo) throws IOException {
        ASN1ObjectIdentifier algorithm = privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm();
        if (algorithm.equals(UAObjectIdentifiers.dstu4145le) || algorithm.equals(UAObjectIdentifiers.dstu4145be)) {
            return new BCDSTU4145PrivateKey(privateKeyInfo);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("algorithm identifier ");
        stringBuilder.append(algorithm);
        stringBuilder.append(" in key not recognised");
        throw new IOException(stringBuilder.toString());
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        ASN1ObjectIdentifier algorithm = subjectPublicKeyInfo.getAlgorithm().getAlgorithm();
        if (algorithm.equals(UAObjectIdentifiers.dstu4145le) || algorithm.equals(UAObjectIdentifiers.dstu4145be)) {
            return new BCDSTU4145PublicKey(subjectPublicKeyInfo);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("algorithm identifier ");
        stringBuilder.append(algorithm);
        stringBuilder.append(" in key not recognised");
        throw new IOException(stringBuilder.toString());
    }
}
