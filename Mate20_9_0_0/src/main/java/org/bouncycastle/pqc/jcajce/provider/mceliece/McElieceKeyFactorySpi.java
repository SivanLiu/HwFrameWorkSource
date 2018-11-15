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
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import org.bouncycastle.pqc.asn1.McEliecePrivateKey;
import org.bouncycastle.pqc.asn1.McEliecePublicKey;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.crypto.mceliece.McEliecePrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mceliece.McEliecePublicKeyParameters;

public class McElieceKeyFactorySpi extends KeyFactorySpi implements AsymmetricKeyInfoConverter {
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.4.1";

    private static Digest getDigest(AlgorithmIdentifier algorithmIdentifier) {
        return new SHA256Digest();
    }

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                PrivateKeyInfo instance = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(((PKCS8EncodedKeySpec) keySpec).getEncoded()));
                try {
                    if (PQCObjectIdentifiers.mcEliece.equals(instance.getPrivateKeyAlgorithm().getAlgorithm())) {
                        McEliecePrivateKey instance2 = McEliecePrivateKey.getInstance(instance.parsePrivateKey());
                        return new BCMcEliecePrivateKey(new McEliecePrivateKeyParameters(instance2.getN(), instance2.getK(), instance2.getField(), instance2.getGoppaPoly(), instance2.getP1(), instance2.getP2(), instance2.getSInv()));
                    }
                    throw new InvalidKeySpecException("Unable to recognise OID in McEliece private key");
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
                    if (PQCObjectIdentifiers.mcEliece.equals(instance.getAlgorithm().getAlgorithm())) {
                        McEliecePublicKey instance2 = McEliecePublicKey.getInstance(instance.parsePublicKey());
                        return new BCMcEliecePublicKey(new McEliecePublicKeyParameters(instance2.getN(), instance2.getT(), instance2.getG()));
                    }
                    throw new InvalidKeySpecException("Unable to recognise OID in McEliece public key");
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
        McEliecePrivateKey instance = McEliecePrivateKey.getInstance(privateKeyInfo.parsePrivateKey().toASN1Primitive());
        return new BCMcEliecePrivateKey(new McEliecePrivateKeyParameters(instance.getN(), instance.getK(), instance.getField(), instance.getGoppaPoly(), instance.getP1(), instance.getP2(), instance.getSInv()));
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        McEliecePublicKey instance = McEliecePublicKey.getInstance(subjectPublicKeyInfo.parsePublicKey());
        return new BCMcEliecePublicKey(new McEliecePublicKeyParameters(instance.getN(), instance.getT(), instance.getG()));
    }

    public KeySpec getKeySpec(Key key, Class cls) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (key instanceof BCMcEliecePrivateKey) {
            if (PKCS8EncodedKeySpec.class.isAssignableFrom(cls)) {
                return new PKCS8EncodedKeySpec(key.getEncoded());
            }
        } else if (!(key instanceof BCMcEliecePublicKey)) {
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
        if ((key instanceof BCMcEliecePrivateKey) || (key instanceof BCMcEliecePublicKey)) {
            return key;
        }
        throw new InvalidKeyException("Unsupported key type.");
    }
}
