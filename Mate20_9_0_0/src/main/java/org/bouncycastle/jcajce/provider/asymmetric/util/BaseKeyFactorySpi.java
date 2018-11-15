package org.bouncycastle.jcajce.provider.asymmetric.util;

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
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public abstract class BaseKeyFactorySpi extends KeyFactorySpi implements AsymmetricKeyInfoConverter {
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                return generatePrivate(PrivateKeyInfo.getInstance(((PKCS8EncodedKeySpec) keySpec).getEncoded()));
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("encoded key spec not recognized: ");
                stringBuilder.append(e.getMessage());
                throw new InvalidKeySpecException(stringBuilder.toString());
            }
        }
        throw new InvalidKeySpecException("key spec not recognized");
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                return generatePublic(SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec) keySpec).getEncoded()));
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("encoded key spec not recognized: ");
                stringBuilder.append(e.getMessage());
                throw new InvalidKeySpecException(stringBuilder.toString());
            }
        }
        throw new InvalidKeySpecException("key spec not recognized");
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
}
