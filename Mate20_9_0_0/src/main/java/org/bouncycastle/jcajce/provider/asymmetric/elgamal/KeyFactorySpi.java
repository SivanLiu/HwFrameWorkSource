package org.bouncycastle.jcajce.provider.asymmetric.elgamal;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseKeyFactorySpi;
import org.bouncycastle.jce.interfaces.ElGamalPrivateKey;
import org.bouncycastle.jce.interfaces.ElGamalPublicKey;
import org.bouncycastle.jce.spec.ElGamalPrivateKeySpec;
import org.bouncycastle.jce.spec.ElGamalPublicKeySpec;

public class KeyFactorySpi extends BaseKeyFactorySpi {
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        return keySpec instanceof ElGamalPrivateKeySpec ? new BCElGamalPrivateKey((ElGamalPrivateKeySpec) keySpec) : keySpec instanceof DHPrivateKeySpec ? new BCElGamalPrivateKey((DHPrivateKeySpec) keySpec) : super.engineGeneratePrivate(keySpec);
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        return keySpec instanceof ElGamalPublicKeySpec ? new BCElGamalPublicKey((ElGamalPublicKeySpec) keySpec) : keySpec instanceof DHPublicKeySpec ? new BCElGamalPublicKey((DHPublicKeySpec) keySpec) : super.engineGeneratePublic(keySpec);
    }

    protected KeySpec engineGetKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        if (cls.isAssignableFrom(DHPrivateKeySpec.class) && (key instanceof DHPrivateKey)) {
            DHPrivateKey dHPrivateKey = (DHPrivateKey) key;
            return new DHPrivateKeySpec(dHPrivateKey.getX(), dHPrivateKey.getParams().getP(), dHPrivateKey.getParams().getG());
        } else if (!cls.isAssignableFrom(DHPublicKeySpec.class) || !(key instanceof DHPublicKey)) {
            return super.engineGetKeySpec(key, cls);
        } else {
            DHPublicKey dHPublicKey = (DHPublicKey) key;
            return new DHPublicKeySpec(dHPublicKey.getY(), dHPublicKey.getParams().getP(), dHPublicKey.getParams().getG());
        }
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key instanceof DHPublicKey) {
            return new BCElGamalPublicKey((DHPublicKey) key);
        }
        if (key instanceof DHPrivateKey) {
            return new BCElGamalPrivateKey((DHPrivateKey) key);
        }
        if (key instanceof ElGamalPublicKey) {
            return new BCElGamalPublicKey((ElGamalPublicKey) key);
        }
        if (key instanceof ElGamalPrivateKey) {
            return new BCElGamalPrivateKey((ElGamalPrivateKey) key);
        }
        throw new InvalidKeyException("key type unknown");
    }

    public PrivateKey generatePrivate(PrivateKeyInfo privateKeyInfo) throws IOException {
        ASN1ObjectIdentifier algorithm = privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm();
        if (algorithm.equals(PKCSObjectIdentifiers.dhKeyAgreement)) {
            return new BCElGamalPrivateKey(privateKeyInfo);
        }
        if (algorithm.equals(X9ObjectIdentifiers.dhpublicnumber)) {
            return new BCElGamalPrivateKey(privateKeyInfo);
        }
        if (algorithm.equals(OIWObjectIdentifiers.elGamalAlgorithm)) {
            return new BCElGamalPrivateKey(privateKeyInfo);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("algorithm identifier ");
        stringBuilder.append(algorithm);
        stringBuilder.append(" in key not recognised");
        throw new IOException(stringBuilder.toString());
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        ASN1ObjectIdentifier algorithm = subjectPublicKeyInfo.getAlgorithm().getAlgorithm();
        if (algorithm.equals(PKCSObjectIdentifiers.dhKeyAgreement)) {
            return new BCElGamalPublicKey(subjectPublicKeyInfo);
        }
        if (algorithm.equals(X9ObjectIdentifiers.dhpublicnumber)) {
            return new BCElGamalPublicKey(subjectPublicKeyInfo);
        }
        if (algorithm.equals(OIWObjectIdentifiers.elGamalAlgorithm)) {
            return new BCElGamalPublicKey(subjectPublicKeyInfo);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("algorithm identifier ");
        stringBuilder.append(algorithm);
        stringBuilder.append(" in key not recognised");
        throw new IOException(stringBuilder.toString());
    }
}
