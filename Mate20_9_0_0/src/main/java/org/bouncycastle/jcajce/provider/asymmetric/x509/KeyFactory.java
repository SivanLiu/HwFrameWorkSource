package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeyFactory extends KeyFactorySpi {
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                PrivateKeyInfo instance = PrivateKeyInfo.getInstance(((PKCS8EncodedKeySpec) keySpec).getEncoded());
                PrivateKey privateKey = BouncyCastleProvider.getPrivateKey(instance);
                if (privateKey != null) {
                    return privateKey;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("no factory found for OID: ");
                stringBuilder.append(instance.getPrivateKeyAlgorithm().getAlgorithm());
                throw new InvalidKeySpecException(stringBuilder.toString());
            } catch (Exception e) {
                throw new InvalidKeySpecException(e.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown KeySpec type: ");
        stringBuilder.append(keySpec.getClass().getName());
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                SubjectPublicKeyInfo instance = SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec) keySpec).getEncoded());
                PublicKey publicKey = BouncyCastleProvider.getPublicKey(instance);
                if (publicKey != null) {
                    return publicKey;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("no factory found for OID: ");
                stringBuilder.append(instance.getAlgorithm().getAlgorithm());
                throw new InvalidKeySpecException(stringBuilder.toString());
            } catch (Exception e) {
                throw new InvalidKeySpecException(e.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown KeySpec type: ");
        stringBuilder.append(keySpec.getClass().getName());
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected KeySpec engineGetKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        if (cls.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8")) {
            return new PKCS8EncodedKeySpec(key.getEncoded());
        }
        if (cls.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509")) {
            return new X509EncodedKeySpec(key.getEncoded());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("not implemented yet ");
        stringBuilder.append(key);
        stringBuilder.append(" ");
        stringBuilder.append(cls);
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("not implemented yet ");
        stringBuilder.append(key);
        throw new InvalidKeyException(stringBuilder.toString());
    }
}
