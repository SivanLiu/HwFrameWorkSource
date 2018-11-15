package org.bouncycastle.pqc.jcajce.provider.mceliece;

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
import org.bouncycastle.pqc.asn1.McElieceCCA2PrivateKey;
import org.bouncycastle.pqc.asn1.McElieceCCA2PublicKey;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.crypto.mceliece.McElieceCCA2PrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mceliece.McElieceCCA2PublicKeyParameters;

public class McElieceCCA2KeyFactorySpi extends KeyFactorySpi implements AsymmetricKeyInfoConverter {
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.4.2";

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                PrivateKeyInfo instance = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(((PKCS8EncodedKeySpec) keySpec).getEncoded()));
                try {
                    if (PQCObjectIdentifiers.mcElieceCca2.equals(instance.getPrivateKeyAlgorithm().getAlgorithm())) {
                        McElieceCCA2PrivateKey instance2 = McElieceCCA2PrivateKey.getInstance(instance.parsePrivateKey());
                        return new BCMcElieceCCA2PrivateKey(new McElieceCCA2PrivateKeyParameters(instance2.getN(), instance2.getK(), instance2.getField(), instance2.getGoppaPoly(), instance2.getP(), Utils.getDigest(instance2.getDigest()).getAlgorithmName()));
                    }
                    throw new InvalidKeySpecException("Unable to recognise OID in McEliece public key");
                } catch (IOException e) {
                    throw new InvalidKeySpecException("Unable to decode PKCS8EncodedKeySpec.");
                }
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to decode PKCS8EncodedKeySpec: ");
                stringBuilder.append(e2);
                throw new InvalidKeySpecException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported key specification: ");
        stringBuilder.append(keySpec.getClass());
        stringBuilder.append(".");
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                SubjectPublicKeyInfo instance = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(((X509EncodedKeySpec) keySpec).getEncoded()));
                try {
                    if (PQCObjectIdentifiers.mcElieceCca2.equals(instance.getAlgorithm().getAlgorithm())) {
                        McElieceCCA2PublicKey instance2 = McElieceCCA2PublicKey.getInstance(instance.parsePublicKey());
                        return new BCMcElieceCCA2PublicKey(new McElieceCCA2PublicKeyParameters(instance2.getN(), instance2.getT(), instance2.getG(), Utils.getDigest(instance2.getDigest()).getAlgorithmName()));
                    }
                    throw new InvalidKeySpecException("Unable to recognise OID in McEliece private key");
                } catch (IOException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to decode X509EncodedKeySpec: ");
                    stringBuilder.append(e.getMessage());
                    throw new InvalidKeySpecException(stringBuilder.toString());
                }
            } catch (IOException e2) {
                throw new InvalidKeySpecException(e2.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported key specification: ");
        stringBuilder.append(keySpec.getClass());
        stringBuilder.append(".");
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected KeySpec engineGetKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        return null;
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        return null;
    }

    public PrivateKey generatePrivate(PrivateKeyInfo privateKeyInfo) throws IOException {
        McElieceCCA2PrivateKey instance = McElieceCCA2PrivateKey.getInstance(privateKeyInfo.parsePrivateKey().toASN1Primitive());
        return new BCMcElieceCCA2PrivateKey(new McElieceCCA2PrivateKeyParameters(instance.getN(), instance.getK(), instance.getField(), instance.getGoppaPoly(), instance.getP(), null));
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        McElieceCCA2PublicKey instance = McElieceCCA2PublicKey.getInstance(subjectPublicKeyInfo.parsePublicKey());
        return new BCMcElieceCCA2PublicKey(new McElieceCCA2PublicKeyParameters(instance.getN(), instance.getT(), instance.getG(), Utils.getDigest(instance.getDigest()).getAlgorithmName()));
    }

    public KeySpec getKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (key instanceof BCMcElieceCCA2PrivateKey) {
            if (PKCS8EncodedKeySpec.class.isAssignableFrom(cls)) {
                return new PKCS8EncodedKeySpec(key.getEncoded());
            }
        } else if (!(key instanceof BCMcElieceCCA2PublicKey)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key type: ");
            stringBuilder.append(key.getClass());
            stringBuilder.append(".");
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (X509EncodedKeySpec.class.isAssignableFrom(cls)) {
            return new X509EncodedKeySpec(key.getEncoded());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown key specification: ");
        stringBuilder.append(cls);
        stringBuilder.append(".");
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    public Key translateKey(Key key) throws InvalidKeyException {
        if ((key instanceof BCMcElieceCCA2PrivateKey) || (key instanceof BCMcElieceCCA2PublicKey)) {
            return key;
        }
        throw new InvalidKeyException("Unsupported key type.");
    }
}
