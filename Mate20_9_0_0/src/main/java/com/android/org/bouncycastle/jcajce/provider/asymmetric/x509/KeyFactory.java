package com.android.org.bouncycastle.jcajce.provider.asymmetric.x509;

import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyFactory extends KeyFactorySpi {
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                PrivateKeyInfo info = PrivateKeyInfo.getInstance(((PKCS8EncodedKeySpec) keySpec).getEncoded());
                PrivateKey key = BouncyCastleProvider.getPrivateKey(info);
                if (key != null) {
                    return key;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no factory found for OID: ");
                stringBuilder.append(info.getPrivateKeyAlgorithm().getAlgorithm());
                throw new InvalidKeySpecException(stringBuilder.toString());
            } catch (Exception e) {
                throw new InvalidKeySpecException(e.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unknown KeySpec type: ");
        stringBuilder2.append(keySpec.getClass().getName());
        throw new InvalidKeySpecException(stringBuilder2.toString());
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec) keySpec).getEncoded());
                PublicKey key = BouncyCastleProvider.getPublicKey(info);
                if (key != null) {
                    return key;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no factory found for OID: ");
                stringBuilder.append(info.getAlgorithm().getAlgorithm());
                throw new InvalidKeySpecException(stringBuilder.toString());
            } catch (Exception e) {
                throw new InvalidKeySpecException(e.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unknown KeySpec type: ");
        stringBuilder2.append(keySpec.getClass().getName());
        throw new InvalidKeySpecException(stringBuilder2.toString());
    }

    protected KeySpec engineGetKeySpec(Key key, Class keySpec) throws InvalidKeySpecException {
        if (keySpec.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8")) {
            return new PKCS8EncodedKeySpec(key.getEncoded());
        }
        if (keySpec.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509")) {
            return new X509EncodedKeySpec(key.getEncoded());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("not implemented yet ");
        stringBuilder.append(key);
        stringBuilder.append(" ");
        stringBuilder.append(keySpec);
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("not implemented yet ");
        stringBuilder.append(key);
        throw new InvalidKeyException(stringBuilder.toString());
    }
}
