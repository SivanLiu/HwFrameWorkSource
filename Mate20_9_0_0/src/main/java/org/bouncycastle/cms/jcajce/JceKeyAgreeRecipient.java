package org.bouncycastle.cms.jcajce;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.cms.ecc.ECCCMSSharedInfo;
import org.bouncycastle.asn1.cms.ecc.MQVuserKeyingMaterial;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.cryptopro.Gost2814789EncryptedKey;
import org.bouncycastle.asn1.cryptopro.Gost2814789KeyWrapParameters;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.KeyAgreeRecipient;
import org.bouncycastle.jcajce.spec.GOST28147WrapParameterSpec;
import org.bouncycastle.jcajce.spec.MQVParameterSpec;
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec;
import org.bouncycastle.operator.DefaultSecretKeySizeProvider;
import org.bouncycastle.operator.SecretKeySizeProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public abstract class JceKeyAgreeRecipient implements KeyAgreeRecipient {
    private static KeyMaterialGenerator ecc_cms_Generator = new RFC5753KeyMaterialGenerator();
    private static KeyMaterialGenerator old_ecc_cms_Generator = new KeyMaterialGenerator() {
        public byte[] generateKDFMaterial(AlgorithmIdentifier algorithmIdentifier, int i, byte[] bArr) {
            try {
                return new ECCCMSSharedInfo(new AlgorithmIdentifier(algorithmIdentifier.getAlgorithm(), DERNull.INSTANCE), bArr, Pack.intToBigEndian(i)).getEncoded(ASN1Encoding.DER);
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create KDF material: ");
                stringBuilder.append(e);
                throw new IllegalStateException(stringBuilder.toString());
            }
        }
    };
    private static final Set possibleOldMessages = new HashSet();
    protected EnvelopedDataHelper contentHelper = this.helper;
    protected EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
    private SecretKeySizeProvider keySizeProvider = new DefaultSecretKeySizeProvider();
    private PrivateKey recipientKey;

    static {
        possibleOldMessages.add(X9ObjectIdentifiers.dhSinglePass_stdDH_sha1kdf_scheme);
        possibleOldMessages.add(X9ObjectIdentifiers.mqvSinglePass_sha1kdf_scheme);
    }

    public JceKeyAgreeRecipient(PrivateKey privateKey) {
        this.recipientKey = privateKey;
    }

    private SecretKey calculateAgreedWrapKey(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2, PublicKey publicKey, ASN1OctetString aSN1OctetString, PrivateKey privateKey, KeyMaterialGenerator keyMaterialGenerator) throws CMSException, GeneralSecurityException, IOException {
        AlgorithmParameterSpec algorithmParameterSpec = null;
        if (CMSUtils.isMQV(algorithmIdentifier.getAlgorithm())) {
            byte[] octets;
            MQVuserKeyingMaterial instance = MQVuserKeyingMaterial.getInstance(aSN1OctetString.getOctets());
            PublicKey generatePublic = this.helper.createKeyFactory(algorithmIdentifier.getAlgorithm()).generatePublic(new X509EncodedKeySpec(new SubjectPublicKeyInfo(getPrivateKeyAlgorithmIdentifier(), instance.getEphemeralPublicKey().getPublicKey().getBytes()).getEncoded()));
            KeyAgreement createKeyAgreement = this.helper.createKeyAgreement(algorithmIdentifier.getAlgorithm());
            if (instance.getAddedukm() != null) {
                octets = instance.getAddedukm().getOctets();
            }
            if (keyMaterialGenerator == old_ecc_cms_Generator) {
                octets = old_ecc_cms_Generator.generateKDFMaterial(algorithmIdentifier2, this.keySizeProvider.getKeySize(algorithmIdentifier2), octets);
            }
            createKeyAgreement.init(privateKey, new MQVParameterSpec(privateKey, generatePublic, octets));
            createKeyAgreement.doPhase(publicKey, true);
            return createKeyAgreement.generateSecret(algorithmIdentifier2.getAlgorithm().getId());
        }
        KeyAgreement createKeyAgreement2 = this.helper.createKeyAgreement(algorithmIdentifier.getAlgorithm());
        if (CMSUtils.isEC(algorithmIdentifier.getAlgorithm())) {
            algorithmParameterSpec = aSN1OctetString != null ? new UserKeyingMaterialSpec(keyMaterialGenerator.generateKDFMaterial(algorithmIdentifier2, this.keySizeProvider.getKeySize(algorithmIdentifier2), aSN1OctetString.getOctets())) : new UserKeyingMaterialSpec(keyMaterialGenerator.generateKDFMaterial(algorithmIdentifier2, this.keySizeProvider.getKeySize(algorithmIdentifier2), null));
        } else if (CMSUtils.isRFC2631(algorithmIdentifier.getAlgorithm())) {
            if (aSN1OctetString != null) {
                algorithmParameterSpec = new UserKeyingMaterialSpec(aSN1OctetString.getOctets());
            }
        } else if (!CMSUtils.isGOST(algorithmIdentifier.getAlgorithm())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown key agreement algorithm: ");
            stringBuilder.append(algorithmIdentifier.getAlgorithm());
            throw new CMSException(stringBuilder.toString());
        } else if (aSN1OctetString != null) {
            algorithmParameterSpec = new UserKeyingMaterialSpec(aSN1OctetString.getOctets());
        }
        createKeyAgreement2.init(privateKey, algorithmParameterSpec);
        createKeyAgreement2.doPhase(publicKey, true);
        return createKeyAgreement2.generateSecret(algorithmIdentifier2.getAlgorithm().getId());
    }

    private Key unwrapSessionKey(ASN1ObjectIdentifier aSN1ObjectIdentifier, SecretKey secretKey, ASN1ObjectIdentifier aSN1ObjectIdentifier2, byte[] bArr) throws CMSException, InvalidKeyException, NoSuchAlgorithmException {
        Cipher createCipher = this.helper.createCipher(aSN1ObjectIdentifier);
        createCipher.init(4, secretKey);
        return createCipher.unwrap(bArr, this.helper.getBaseCipherName(aSN1ObjectIdentifier2), 3);
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x00e5 A:{Splitter: B:0:0x0000, ExcHandler: java.security.NoSuchAlgorithmException (r9_7 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x00d3 A:{Splitter: B:0:0x0000, ExcHandler: java.security.spec.InvalidKeySpecException (r9_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x00ca A:{Splitter: B:0:0x0000, ExcHandler: javax.crypto.NoSuchPaddingException (r9_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x00c1 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.Exception (r9_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:18:0x00c1, code:
            r9 = move-exception;
     */
    /* JADX WARNING: Missing block: B:20:0x00c9, code:
            throw new org.bouncycastle.cms.CMSException("originator key invalid.", r9);
     */
    /* JADX WARNING: Missing block: B:21:0x00ca, code:
            r9 = move-exception;
     */
    /* JADX WARNING: Missing block: B:23:0x00d2, code:
            throw new org.bouncycastle.cms.CMSException("required padding not supported.", r9);
     */
    /* JADX WARNING: Missing block: B:24:0x00d3, code:
            r9 = move-exception;
     */
    /* JADX WARNING: Missing block: B:26:0x00db, code:
            throw new org.bouncycastle.cms.CMSException("originator key spec invalid.", r9);
     */
    /* JADX WARNING: Missing block: B:30:0x00e5, code:
            r9 = move-exception;
     */
    /* JADX WARNING: Missing block: B:32:0x00ed, code:
            throw new org.bouncycastle.cms.CMSException("can't find algorithm.", r9);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Key extractSecretKey(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2, SubjectPublicKeyInfo subjectPublicKeyInfo, ASN1OctetString aSN1OctetString, byte[] bArr) throws CMSException {
        AlgorithmIdentifier instance;
        PublicKey generatePublic;
        try {
            instance = AlgorithmIdentifier.getInstance(algorithmIdentifier.getParameters());
            generatePublic = this.helper.createKeyFactory(subjectPublicKeyInfo.getAlgorithm().getAlgorithm()).generatePublic(new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded()));
            Key calculateAgreedWrapKey = calculateAgreedWrapKey(algorithmIdentifier, instance, generatePublic, aSN1OctetString, this.recipientKey, ecc_cms_Generator);
            if (!instance.getAlgorithm().equals(CryptoProObjectIdentifiers.id_Gost28147_89_None_KeyWrap) && !instance.getAlgorithm().equals(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_KeyWrap)) {
                return unwrapSessionKey(instance.getAlgorithm(), calculateAgreedWrapKey, algorithmIdentifier2.getAlgorithm(), bArr);
            }
            Gost2814789EncryptedKey instance2 = Gost2814789EncryptedKey.getInstance(bArr);
            Gost2814789KeyWrapParameters instance3 = Gost2814789KeyWrapParameters.getInstance(instance.getParameters());
            Cipher createCipher = this.helper.createCipher(instance.getAlgorithm());
            createCipher.init(4, calculateAgreedWrapKey, new GOST28147WrapParameterSpec(instance3.getEncryptionParamSet(), aSN1OctetString.getOctets()));
            return createCipher.unwrap(Arrays.concatenate(instance2.getEncryptedKey(), instance2.getMacKey()), this.helper.getBaseCipherName(algorithmIdentifier2.getAlgorithm()), 3);
        } catch (InvalidKeyException e) {
            if (possibleOldMessages.contains(algorithmIdentifier.getAlgorithm())) {
                return unwrapSessionKey(instance.getAlgorithm(), calculateAgreedWrapKey(algorithmIdentifier, instance, generatePublic, aSN1OctetString, this.recipientKey, old_ecc_cms_Generator), algorithmIdentifier2.getAlgorithm(), bArr);
            }
            throw e;
        } catch (Exception e2) {
        } catch (Exception e3) {
        } catch (Exception e4) {
        } catch (Exception e5) {
        } catch (Exception e6) {
            throw new CMSException("key invalid in message.", e6);
        }
    }

    public AlgorithmIdentifier getPrivateKeyAlgorithmIdentifier() {
        return PrivateKeyInfo.getInstance(this.recipientKey.getEncoded()).getPrivateKeyAlgorithm();
    }

    public JceKeyAgreeRecipient setContentProvider(String str) {
        this.contentHelper = CMSUtils.createContentHelper(str);
        return this;
    }

    public JceKeyAgreeRecipient setContentProvider(Provider provider) {
        this.contentHelper = CMSUtils.createContentHelper(provider);
        return this;
    }

    public JceKeyAgreeRecipient setProvider(String str) {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(str));
        this.contentHelper = this.helper;
        return this;
    }

    public JceKeyAgreeRecipient setProvider(Provider provider) {
        this.helper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));
        this.contentHelper = this.helper;
        return this;
    }
}
