package org.bouncycastle.pqc.jcajce.provider.xmss;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public class XMSSMTKeyFactorySpi extends KeyFactorySpi implements AsymmetricKeyInfoConverter {
    public PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                return generatePrivate(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(((PKCS8EncodedKeySpec) keySpec).getEncoded())));
            } catch (Exception e) {
                throw new InvalidKeySpecException(e.toString());
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unsupported key specification: ");
        stringBuilder.append(keySpec.getClass());
        stringBuilder.append(".");
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    public PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                return generatePublic(SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec) keySpec).getEncoded()));
            } catch (Exception e) {
                throw new InvalidKeySpecException(e.toString());
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown key specification: ");
        stringBuilder.append(keySpec);
        stringBuilder.append(".");
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    public final KeySpec engineGetKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (key instanceof BCXMSSMTPrivateKey) {
            if (PKCS8EncodedKeySpec.class.isAssignableFrom(cls)) {
                return new PKCS8EncodedKeySpec(key.getEncoded());
            }
        } else if (!(key instanceof BCXMSSMTPublicKey)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unsupported key type: ");
            stringBuilder.append(key.getClass());
            stringBuilder.append(".");
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (X509EncodedKeySpec.class.isAssignableFrom(cls)) {
            return new X509EncodedKeySpec(key.getEncoded());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown key specification: ");
        stringBuilder.append(cls);
        stringBuilder.append(".");
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    public final Key engineTranslateKey(Key key) throws InvalidKeyException {
        if ((key instanceof BCXMSSMTPrivateKey) || (key instanceof BCXMSSMTPublicKey)) {
            return key;
        }
        throw new InvalidKeyException("unsupported key type");
    }

    public PrivateKey generatePrivate(PrivateKeyInfo privateKeyInfo) throws IOException {
        return new BCXMSSMTPrivateKey(privateKeyInfo);
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        return new BCXMSSMTPublicKey(subjectPublicKeyInfo);
    }
}
