package org.bouncycastle.operator.jcajce;

import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.AsymmetricKeyWrapper;
import org.bouncycastle.operator.GenericKey;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;

public class JceAsymmetricKeyWrapper extends AsymmetricKeyWrapper {
    private static final Map digests = new HashMap();
    private Map extraMappings;
    private OperatorHelper helper;
    private PublicKey publicKey;
    private SecureRandom random;

    static {
        digests.put(McElieceCCA2KeyGenParameterSpec.SHA1, new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, DERNull.INSTANCE));
        digests.put(McElieceCCA2KeyGenParameterSpec.SHA1, new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, DERNull.INSTANCE));
        digests.put("SHA224", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha224, DERNull.INSTANCE));
        digests.put(McElieceCCA2KeyGenParameterSpec.SHA224, new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha224, DERNull.INSTANCE));
        digests.put("SHA256", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256, DERNull.INSTANCE));
        digests.put(McElieceCCA2KeyGenParameterSpec.SHA256, new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256, DERNull.INSTANCE));
        digests.put("SHA384", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha384, DERNull.INSTANCE));
        digests.put(McElieceCCA2KeyGenParameterSpec.SHA384, new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha384, DERNull.INSTANCE));
        digests.put("SHA512", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512, DERNull.INSTANCE));
        digests.put(McElieceCCA2KeyGenParameterSpec.SHA512, new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512, DERNull.INSTANCE));
        digests.put("SHA512/224", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_224, DERNull.INSTANCE));
        digests.put("SHA-512/224", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_224, DERNull.INSTANCE));
        digests.put("SHA-512(224)", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_224, DERNull.INSTANCE));
        digests.put("SHA512/256", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_256, DERNull.INSTANCE));
        digests.put("SHA-512/256", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_256, DERNull.INSTANCE));
        digests.put("SHA-512(256)", new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_256, DERNull.INSTANCE));
    }

    public JceAsymmetricKeyWrapper(PublicKey publicKey) {
        super(SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()).getAlgorithm());
        this.helper = new OperatorHelper(new DefaultJcaJceHelper());
        this.extraMappings = new HashMap();
        this.publicKey = publicKey;
    }

    public JceAsymmetricKeyWrapper(X509Certificate x509Certificate) {
        this(x509Certificate.getPublicKey());
    }

    public JceAsymmetricKeyWrapper(AlgorithmParameterSpec algorithmParameterSpec, PublicKey publicKey) {
        super(extractFromSpec(algorithmParameterSpec));
        this.helper = new OperatorHelper(new DefaultJcaJceHelper());
        this.extraMappings = new HashMap();
        this.publicKey = publicKey;
    }

    public JceAsymmetricKeyWrapper(AlgorithmIdentifier algorithmIdentifier, PublicKey publicKey) {
        super(algorithmIdentifier);
        this.helper = new OperatorHelper(new DefaultJcaJceHelper());
        this.extraMappings = new HashMap();
        this.publicKey = publicKey;
    }

    private static AlgorithmIdentifier extractFromSpec(AlgorithmParameterSpec algorithmParameterSpec) {
        StringBuilder stringBuilder;
        if (algorithmParameterSpec instanceof OAEPParameterSpec) {
            OAEPParameterSpec oAEPParameterSpec = (OAEPParameterSpec) algorithmParameterSpec;
            if (!oAEPParameterSpec.getMGFAlgorithm().equals(OAEPParameterSpec.DEFAULT.getMGFAlgorithm())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unknown MGF: ");
                stringBuilder.append(oAEPParameterSpec.getMGFAlgorithm());
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (oAEPParameterSpec.getPSource() instanceof PSpecified) {
                return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSAES_OAEP, new RSAESOAEPparams(getDigest(oAEPParameterSpec.getDigestAlgorithm()), new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, getDigest(((MGF1ParameterSpec) oAEPParameterSpec.getMGFParameters()).getDigestAlgorithm())), new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(((PSpecified) oAEPParameterSpec.getPSource()).getValue()))));
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unknown PSource: ");
                stringBuilder.append(oAEPParameterSpec.getPSource().getAlgorithm());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown spec: ");
        stringBuilder.append(algorithmParameterSpec.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static AlgorithmIdentifier getDigest(String str) {
        AlgorithmIdentifier algorithmIdentifier = (AlgorithmIdentifier) digests.get(str);
        if (algorithmIdentifier != null) {
            return algorithmIdentifier;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown digest name: ");
        stringBuilder.append(str);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0025 A:{Catch:{ InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025 }, Splitter: B:2:0x001d, ExcHandler: java.security.InvalidKeyException (e java.security.InvalidKeyException)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0025 A:{Catch:{ InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025 }, Splitter: B:2:0x001d, ExcHandler: java.security.InvalidKeyException (e java.security.InvalidKeyException)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0025 A:{Catch:{ InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025 }, Splitter: B:2:0x001d, ExcHandler: java.security.InvalidKeyException (e java.security.InvalidKeyException)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0025 A:{Catch:{ InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025, InvalidKeyException -> 0x0025 }, Splitter: B:2:0x001d, ExcHandler: java.security.InvalidKeyException (e java.security.InvalidKeyException)} */
    /* JADX WARNING: Missing block: B:7:0x0037, code:
            r1 = null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] generateWrappedKey(GenericKey genericKey) throws OperatorException {
        Cipher createAsymmetricWrapper = this.helper.createAsymmetricWrapper(getAlgorithmIdentifier().getAlgorithm(), this.extraMappings);
        AlgorithmParameters createAlgorithmParameters = this.helper.createAlgorithmParameters(getAlgorithmIdentifier());
        if (createAlgorithmParameters != null) {
            try {
                createAsymmetricWrapper.init(3, this.publicKey, createAlgorithmParameters, this.random);
            } catch (InvalidKeyException e) {
            }
        } else {
            createAsymmetricWrapper.init(3, this.publicKey, this.random);
        }
        byte[] wrap = createAsymmetricWrapper.wrap(OperatorUtils.getJceKey(genericKey));
        if (wrap != null) {
            return wrap;
        }
        try {
            createAsymmetricWrapper.init(1, this.publicKey, this.random);
            return createAsymmetricWrapper.doFinal(OperatorUtils.getJceKey(genericKey).getEncoded());
        } catch (Throwable e2) {
            throw new OperatorException("unable to encrypt contents key", e2);
        } catch (Throwable e22) {
            throw new OperatorException("unable to encrypt contents key", e22);
        }
    }

    public JceAsymmetricKeyWrapper setAlgorithmMapping(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        this.extraMappings.put(aSN1ObjectIdentifier, str);
        return this;
    }

    public JceAsymmetricKeyWrapper setProvider(String str) {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(str));
        return this;
    }

    public JceAsymmetricKeyWrapper setProvider(Provider provider) {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));
        return this;
    }

    public JceAsymmetricKeyWrapper setSecureRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
        return this;
    }
}
