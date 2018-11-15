package com.huawei.security.keystore;

import android.security.keystore.KeyGenParameterSpec;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERInteger;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.Certificate;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x509.TBSCertificate;
import com.android.org.bouncycastle.asn1.x509.Time;
import com.android.org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.jce.X509Principal;
import com.android.org.bouncycastle.jce.provider.X509CertificateObject;
import com.android.org.bouncycastle.x509.X509V3CertificateGenerator;
import com.huawei.security.HwCredentials;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.HwKeystoreManager.State;
import com.huawei.security.keymaster.HwKeyCharacteristics;
import com.huawei.security.keymaster.HwKeymasterArguments;
import com.huawei.security.keymaster.HwKeymasterBlob;
import com.huawei.security.keymaster.HwKeymasterCertificateChain;
import com.huawei.security.keymaster.HwKeymasterDefs;
import com.huawei.security.keymaster.HwKeymasterUtils;
import com.huawei.security.keystore.HwKeyProperties.BlockMode;
import com.huawei.security.keystore.HwKeyProperties.Digest;
import com.huawei.security.keystore.HwKeyProperties.EncryptionPadding;
import com.huawei.security.keystore.HwKeyProperties.Purpose;
import com.huawei.security.keystore.HwKeyProperties.SignaturePadding;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HwUniversalKeyStoreKeyPairGeneratorSpi extends KeyPairGeneratorSpi {
    private static final int EC_DEFAULT_KEY_SIZE = 256;
    private static final int RSA_DEFAULT_KEY_SIZE = 2048;
    private static final int RSA_MAX_KEY_SIZE = 4096;
    private static final int RSA_MIN_KEY_SIZE = 512;
    private static final List<String> SUPPORTED_EC_NIST_CURVE_NAMES = new ArrayList();
    private static final Map<String, Integer> SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE = new HashMap();
    private static final List<Integer> SUPPORTED_EC_NIST_CURVE_SIZES = new ArrayList();
    public static final String TAG = "HwKeyPairGenerator";
    private boolean mEncryptionAtRestRequired;
    private String mEntryAlias;
    private int mEntryUid;
    private int mKeySizeBits;
    private HwKeystoreManager mKeyStore;
    private int mKeymasterAlgorithm = -1;
    private int[] mKeymasterBlockModes;
    private int[] mKeymasterDigests;
    private int[] mKeymasterEncryptionPaddings;
    private int[] mKeymasterPurposes;
    private int[] mKeymasterSignaturePaddings;
    private int mOriginalKeymasterAlgorithm;
    private BigInteger mRSAPublicExponent;
    private SecureRandom mRng;
    private HwKeyGenParameterSpec mSpec;

    public static class EC extends HwUniversalKeyStoreKeyPairGeneratorSpi {
        public EC() {
            super(3);
        }
    }

    public static class RSA extends HwUniversalKeyStoreKeyPairGeneratorSpi {
        public RSA() {
            super(1);
        }
    }

    static {
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-224", Integer.valueOf(224));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp224r1", Integer.valueOf(224));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-256", Integer.valueOf(EC_DEFAULT_KEY_SIZE));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp256r1", Integer.valueOf(EC_DEFAULT_KEY_SIZE));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("prime256v1", Integer.valueOf(EC_DEFAULT_KEY_SIZE));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-384", Integer.valueOf(384));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp384r1", Integer.valueOf(384));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-521", Integer.valueOf(521));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp521r1", Integer.valueOf(521));
        SUPPORTED_EC_NIST_CURVE_NAMES.addAll(SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.keySet());
        Collections.sort(SUPPORTED_EC_NIST_CURVE_NAMES);
        SUPPORTED_EC_NIST_CURVE_SIZES.addAll(new HashSet(SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.values()));
        Collections.sort(SUPPORTED_EC_NIST_CURVE_SIZES);
    }

    protected HwUniversalKeyStoreKeyPairGeneratorSpi(int keymasterAlgorithm) {
        this.mOriginalKeymasterAlgorithm = keymasterAlgorithm;
    }

    public void initialize(int keysize, SecureRandom random) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HwKeyGenParameterSpec.class.getName());
        stringBuilder.append(" required to initialize this HwKeyPairGenerator");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x00a1 A:{ExcHandler: java.lang.IllegalArgumentException (r3_22 'e' java.lang.RuntimeException), Splitter: B:14:0x0057} */
    /* JADX WARNING: Missing block: B:23:0x00a1, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:26:0x00a7, code:
            throw new java.security.InvalidAlgorithmParameterException(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        resetAll();
        Log.e(TAG, "HwUniversalKeyStoreKeyPairGeneratorSpi initialize");
        if (params != null) {
            try {
                HwKeyGenParameterSpec spec;
                int keymasterAlgorithm = this.mOriginalKeymasterAlgorithm;
                if (params instanceof HwKeyGenParameterSpec) {
                    spec = (HwKeyGenParameterSpec) params;
                } else if (params instanceof KeyGenParameterSpec) {
                    spec = HwKeyGenParameterSpec.getInstance((KeyGenParameterSpec) params);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported params class: ");
                    stringBuilder.append(params.getClass().getName());
                    stringBuilder.append(". Supported: ");
                    stringBuilder.append(HwKeyGenParameterSpec.class.getName());
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                }
                this.mEntryAlias = getEntryAlias(spec.getKeystoreAlias());
                this.mEntryUid = spec.getUid();
                this.mSpec = spec;
                this.mKeymasterAlgorithm = keymasterAlgorithm;
                this.mKeySizeBits = spec.getKeySize();
                initAlgorithmSpecificParameters();
                if (this.mKeySizeBits == -1) {
                    this.mKeySizeBits = getDefaultKeySize(keymasterAlgorithm);
                }
                checkValidKeySize(keymasterAlgorithm, this.mKeySizeBits);
                if (spec.getKeystoreAlias() != null) {
                    this.mKeymasterPurposes = Purpose.allToKeymaster(spec.getPurposes());
                    this.mKeymasterBlockModes = BlockMode.allToKeymaster(spec.getBlockModes());
                    this.mKeymasterEncryptionPaddings = EncryptionPadding.allToKeymaster(spec.getEncryptionPaddings());
                    this.mKeymasterSignaturePaddings = SignaturePadding.allToKeymaster(spec.getSignaturePaddings());
                    if (spec.isDigestsSpecified()) {
                        this.mKeymasterDigests = Digest.allToKeymaster(spec.getDigests());
                    }
                    this.mRng = random;
                    this.mKeyStore = HwKeystoreManager.getInstance();
                    if (!true) {
                        resetAll();
                        return;
                    }
                    return;
                }
                throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");
            } catch (RuntimeException e) {
            } catch (Throwable th) {
                if (!false) {
                    resetAll();
                }
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Must supply params of type ");
            stringBuilder2.append(HwKeyGenParameterSpec.class.getName());
            throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
        }
    }

    private void initAlgorithmSpecificParameters() throws InvalidAlgorithmParameterException {
        AlgorithmParameterSpec algSpecificSpec = this.mSpec.getAlgorithmParameterSpec();
        int i = this.mKeymasterAlgorithm;
        if (i == 1) {
            StringBuilder stringBuilder;
            BigInteger publicExponent = null;
            if (algSpecificSpec instanceof RSAKeyGenParameterSpec) {
                RSAKeyGenParameterSpec rsaSpec = (RSAKeyGenParameterSpec) algSpecificSpec;
                if (this.mKeySizeBits == -1) {
                    this.mKeySizeBits = rsaSpec.getKeysize();
                } else if (this.mKeySizeBits != rsaSpec.getKeysize()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RSA key size must match  between ");
                    stringBuilder.append(this.mSpec);
                    stringBuilder.append(" and ");
                    stringBuilder.append(algSpecificSpec);
                    stringBuilder.append(": ");
                    stringBuilder.append(this.mKeySizeBits);
                    stringBuilder.append(" vs ");
                    stringBuilder.append(rsaSpec.getKeysize());
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                }
                publicExponent = rsaSpec.getPublicExponent();
            } else if (algSpecificSpec != null) {
                throw new InvalidAlgorithmParameterException("RSA may only use RSAKeyGenParameterSpec");
            }
            if (publicExponent == null) {
                publicExponent = RSAKeyGenParameterSpec.F4;
            }
            if (publicExponent.compareTo(BigInteger.ZERO) < 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("RSA public exponent must be positive: ");
                stringBuilder.append(publicExponent);
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            } else if (publicExponent.compareTo(HwKeymasterArguments.UINT64_MAX_VALUE) <= 0) {
                this.mRSAPublicExponent = publicExponent;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported RSA public exponent: ");
                stringBuilder.append(publicExponent);
                stringBuilder.append(". Maximum supported value: ");
                stringBuilder.append(HwKeymasterArguments.UINT64_MAX_VALUE);
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        } else if (i != 3) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unsupported algorithm: ");
            stringBuilder2.append(this.mKeymasterAlgorithm);
            throw new ProviderException(stringBuilder2.toString());
        } else if (algSpecificSpec instanceof ECGenParameterSpec) {
            String curveName = ((ECGenParameterSpec) algSpecificSpec).getName();
            Integer ecSpecKeySizeBits = (Integer) SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.get(curveName.toLowerCase(Locale.US));
            StringBuilder stringBuilder3;
            if (ecSpecKeySizeBits == null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unsupported EC curve name: ");
                stringBuilder3.append(curveName);
                stringBuilder3.append(". Supported: ");
                stringBuilder3.append(SUPPORTED_EC_NIST_CURVE_NAMES);
                throw new InvalidAlgorithmParameterException(stringBuilder3.toString());
            } else if (this.mKeySizeBits == -1) {
                this.mKeySizeBits = ecSpecKeySizeBits.intValue();
            } else if (this.mKeySizeBits != ecSpecKeySizeBits.intValue()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("EC key size must match  between ");
                stringBuilder3.append(this.mSpec);
                stringBuilder3.append(" and ");
                stringBuilder3.append(algSpecificSpec);
                stringBuilder3.append(": ");
                stringBuilder3.append(this.mKeySizeBits);
                stringBuilder3.append(" vs ");
                stringBuilder3.append(ecSpecKeySizeBits);
                throw new InvalidAlgorithmParameterException(stringBuilder3.toString());
            }
        } else if (algSpecificSpec != null) {
            throw new InvalidAlgorithmParameterException("EC may only use ECGenParameterSpec");
        }
    }

    private static void checkValidKeySize(int keymasterAlgorithm, int keySize) throws InvalidAlgorithmParameterException {
        if (keymasterAlgorithm != 1) {
            StringBuilder stringBuilder;
            if (keymasterAlgorithm != 3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported algorithm: ");
                stringBuilder.append(keymasterAlgorithm);
                throw new ProviderException(stringBuilder.toString());
            } else if (!SUPPORTED_EC_NIST_CURVE_SIZES.contains(Integer.valueOf(keySize))) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported EC key size: ");
                stringBuilder.append(keySize);
                stringBuilder.append(" bits. Supported: ");
                stringBuilder.append(SUPPORTED_EC_NIST_CURVE_SIZES);
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        } else if (keySize < RSA_MIN_KEY_SIZE || keySize > RSA_MAX_KEY_SIZE) {
            throw new InvalidAlgorithmParameterException("RSA key size must be >= 512 and <= 4096");
        }
    }

    private static int getDefaultKeySize(int keymasterAlgorithm) {
        if (keymasterAlgorithm == 1) {
            return RSA_DEFAULT_KEY_SIZE;
        }
        if (keymasterAlgorithm == 3) {
            return EC_DEFAULT_KEY_SIZE;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported algorithm: ");
        stringBuilder.append(keymasterAlgorithm);
        throw new ProviderException(stringBuilder.toString());
    }

    public KeyPair generateKeyPair() {
        if (this.mKeyStore == null || this.mSpec == null) {
            throw new IllegalStateException("Not initialized");
        }
        int flags = this.mEncryptionAtRestRequired;
        if ((flags & 1) == 0 || this.mKeyStore.state() == State.UNLOCKED) {
            byte[] additionalEntropy = getRandomBytesToMixIntoKeystoreRng(this.mRng, (this.mKeySizeBits + 7) / 8);
            HwCredentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
            String privateKeyAlias = new StringBuilder();
            privateKeyAlias.append(HwCredentials.USER_PRIVATE_KEY);
            privateKeyAlias.append(this.mEntryAlias);
            privateKeyAlias = privateKeyAlias.toString();
            boolean success = false;
            KeyPair keyPair;
            try {
                generateKeystoreKeyPair(privateKeyAlias, constructKeyGenerationArguments(), additionalEntropy, flags);
                KeyPair keyPair2 = loadKeystoreKeyPair(privateKeyAlias);
                byte[] certChainBytes = createCertificateChainBytes(privateKeyAlias, keyPair2);
                if (certChainBytes == null) {
                    Log.e(TAG, "generateKeyPair failed, CertificateChain is null!");
                    success = false;
                    keyPair = null;
                    return keyPair;
                }
                storeCertificateChainBytes(flags, certChainBytes);
                keyPair = TAG;
                Log.i(keyPair, "generateKeyPair successed");
                if (!true) {
                    HwCredentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
                }
                return keyPair2;
            } finally {
                if (!success) {
                    HwKeystoreManager hwKeystoreManager = this.mKeyStore;
                    keyPair = this.mEntryAlias;
                    HwCredentials.deleteAllTypesForAlias(hwKeystoreManager, keyPair, this.mEntryUid);
                }
            }
        } else {
            throw new IllegalStateException("Encryption at rest using secure lock screen credential requested for key pair, but the user has not yet entered the credential");
        }
    }

    private void storeCertificateChainBytes(int flags, byte[] bytes) throws ProviderException {
        if (bytes != null) {
            HwKeymasterBlob blob = new HwKeymasterBlob(bytes);
            int insertErrorCode = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(HwCredentials.CERTIFICATE_CHAIN);
            stringBuilder.append(this.mEntryAlias);
            insertErrorCode = insertErrorCode.set(stringBuilder.toString(), blob, this.mEntryUid);
            if (insertErrorCode != 1) {
                throw new ProviderException("Failed to store attestation certificate chain", HwKeystoreManager.getKeyStoreException(insertErrorCode));
            }
            return;
        }
        throw new ProviderException("Input param is invalid.");
    }

    private void storeCertificateChain(int flags, Iterable<byte[]> iterable) throws ProviderException {
        if (iterable != null) {
            Iterator<byte[]> iter = iterable.iterator();
            storeCertificate(HwCredentials.USER_CERTIFICATE, (byte[]) iter.next(), flags, "Failed to store certificate");
            if (iter.hasNext()) {
                ByteArrayOutputStream certificateConcatenationStream = new ByteArrayOutputStream();
                while (iter.hasNext()) {
                    byte[] data = (byte[]) iter.next();
                    certificateConcatenationStream.write(data, 0, data.length);
                }
                storeCertificate(HwCredentials.CA_CERTIFICATE, certificateConcatenationStream.toByteArray(), flags, "Failed to store attestation CA certificate");
                return;
            }
            return;
        }
        throw new ProviderException("Input param is invalid.");
    }

    private void storeCertificate(String prefix, byte[] certificateBytes, int flags, String failureMessage) throws ProviderException {
        if (certificateBytes == null) {
            Log.e(TAG, "storeCertificate certificateBytes is null");
            return;
        }
        HwKeymasterBlob blob = new HwKeymasterBlob(certificateBytes);
        int insertErrorCode = this.mKeyStore;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(this.mEntryAlias);
        insertErrorCode = insertErrorCode.set(stringBuilder.toString(), blob, this.mEntryUid);
        if (insertErrorCode != 1) {
            throw new ProviderException(failureMessage, HwKeystoreManager.getKeyStoreException(insertErrorCode));
        }
    }

    private Iterable<byte[]> createCertificateChain(String privateKeyAlias, KeyPair keyPair) throws ProviderException {
        byte[] challenge = this.mSpec.getAttestationChallenge();
        if (challenge == null) {
            return Collections.singleton(generateSelfSignedCertificateBytes(keyPair));
        }
        HwKeymasterArguments args = new HwKeymasterArguments();
        args.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, challenge);
        return getAttestationChain(privateKeyAlias, keyPair, args);
    }

    private byte[] createCertificateChainBytes(String privateKeyAlias, KeyPair keyPair) throws ProviderException {
        byte[] challenge = getChallenge(this.mSpec);
        if (challenge == null) {
            return generateSelfSignedCertificateBytes(keyPair);
        }
        HwKeymasterArguments args = new HwKeymasterArguments();
        args.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, challenge);
        return getAttestationChainBytes(privateKeyAlias, keyPair, args);
    }

    /* JADX WARNING: Removed duplicated region for block: B:6:0x001a A:{ExcHandler: java.io.IOException (r0_4 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:6:0x001a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:8:0x0022, code:
            throw new java.security.ProviderException("Failed to generate self-signed certificate", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private byte[] generateSelfSignedCertificateBytes(KeyPair keyPair) throws ProviderException {
        try {
            return generateSelfSignedCertificate(keyPair.getPrivate(), keyPair.getPublic()).getEncoded();
        } catch (Exception e) {
        } catch (CertificateEncodingException e2) {
            throw new ProviderException("Failed to obtain encoded form of self-signed certificate", e2);
        }
    }

    private X509Certificate generateSelfSignedCertificate(PrivateKey privateKey, PublicKey publicKey) throws CertificateParsingException, IOException {
        String signatureAlgorithm = getCertificateSignatureAlgorithm(this.mKeymasterAlgorithm, this.mKeySizeBits, this.mSpec);
        if (signatureAlgorithm == null) {
            return generateSelfSignedCertificateWithFakeSignature(publicKey);
        }
        try {
            return generateSelfSignedCertificateWithValidSignature(privateKey, publicKey, signatureAlgorithm);
        } catch (Exception e) {
            return generateSelfSignedCertificateWithFakeSignature(publicKey);
        }
    }

    private X509Certificate generateSelfSignedCertificateWithValidSignature(PrivateKey privateKey, PublicKey publicKey, String signatureAlgorithm) throws Exception {
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setPublicKey(publicKey);
        certGen.setSerialNumber(this.mSpec.getCertificateSerialNumber());
        certGen.setSubjectDN(this.mSpec.getCertificateSubject());
        certGen.setIssuerDN(this.mSpec.getCertificateSubject());
        certGen.setNotBefore(this.mSpec.getCertificateNotBefore());
        certGen.setNotAfter(this.mSpec.getCertificateNotAfter());
        certGen.setSignatureAlgorithm(signatureAlgorithm);
        return certGen.generate(privateKey);
    }

    /* JADX WARNING: Missing block: B:17:0x00e1, code:
            if (r5 != null) goto L_0x00e3;
     */
    /* JADX WARNING: Missing block: B:19:?, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:20:0x00e7, code:
            r7 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:0x00e8, code:
            r5.addSuppressed(r7);
     */
    /* JADX WARNING: Missing block: B:22:0x00ec, code:
            r4.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private X509Certificate generateSelfSignedCertificateWithFakeSignature(PublicKey publicKey) throws IOException, CertificateParsingException {
        byte[] signature;
        AlgorithmIdentifier sigAlgId;
        V3TBSCertificateGenerator tbsGenerator = new V3TBSCertificateGenerator();
        int i = this.mKeymasterAlgorithm;
        if (i == 1) {
            signature = new byte[1];
            sigAlgId = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption, DERNull.INSTANCE);
        } else if (i == 3) {
            sigAlgId = new AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA256);
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new DERInteger(0));
            v.add(new DERInteger(0));
            signature = new DERSequence().getEncoded();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key algorithm: ");
            stringBuilder.append(this.mKeymasterAlgorithm);
            throw new ProviderException(stringBuilder.toString());
        }
        byte[] signature2 = signature;
        ASN1InputStream publicKeyInfoIn = new ASN1InputStream(publicKey.getEncoded());
        tbsGenerator.setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(publicKeyInfoIn.readObject()));
        publicKeyInfoIn.close();
        tbsGenerator.setSerialNumber(new ASN1Integer(this.mSpec.getCertificateSerialNumber()));
        publicKeyInfoIn = new X509Principal(this.mSpec.getCertificateSubject().getEncoded());
        tbsGenerator.setSubject(publicKeyInfoIn);
        tbsGenerator.setIssuer(publicKeyInfoIn);
        tbsGenerator.setStartDate(new Time(this.mSpec.getCertificateNotBefore()));
        tbsGenerator.setEndDate(new Time(this.mSpec.getCertificateNotAfter()));
        tbsGenerator.setSignature(sigAlgId);
        TBSCertificate tbsCertificate = tbsGenerator.generateTBSCertificate();
        ASN1EncodableVector result = new ASN1EncodableVector();
        result.add(tbsCertificate);
        result.add(sigAlgId);
        result.add(new DERBitString(signature2));
        return new X509CertificateObject(Certificate.getInstance(new DERSequence(result)));
    }

    @Nullable
    private static String getCertificateSignatureAlgorithm(int keymasterAlgorithm, int keySizeBits, HwKeyGenParameterSpec spec) {
        if ((spec.getPurposes() & 4) == 0 || spec.isUserAuthenticationRequired() || !spec.isDigestsSpecified()) {
            return null;
        }
        int bestDigestOutputSizeBits;
        int keymasterDigest;
        StringBuilder stringBuilder;
        if (keymasterAlgorithm != 1) {
            if (keymasterAlgorithm == 3) {
                int bestKeymasterDigest = -1;
                bestDigestOutputSizeBits = -1;
                for (Integer keymasterDigest2 : getAvailableKeymasterSignatureDigests(spec.getDigests(), HwUniversalKeyStoreProvider.getSupportedEcdsaSignatureDigests())) {
                    keymasterDigest = keymasterDigest2.intValue();
                    int outputSizeBits = HwKeymasterUtils.getDigestOutputSizeBits(keymasterDigest);
                    if (outputSizeBits == keySizeBits) {
                        bestKeymasterDigest = keymasterDigest;
                        bestDigestOutputSizeBits = outputSizeBits;
                        break;
                    } else if (bestKeymasterDigest == -1) {
                        bestKeymasterDigest = keymasterDigest;
                        bestDigestOutputSizeBits = outputSizeBits;
                    } else if (bestDigestOutputSizeBits < keySizeBits) {
                        if (outputSizeBits > bestDigestOutputSizeBits) {
                            bestKeymasterDigest = keymasterDigest;
                            bestDigestOutputSizeBits = outputSizeBits;
                        }
                    } else if (outputSizeBits < bestDigestOutputSizeBits && outputSizeBits >= keySizeBits) {
                        bestKeymasterDigest = keymasterDigest;
                        bestDigestOutputSizeBits = outputSizeBits;
                    }
                }
                if (bestKeymasterDigest == -1) {
                    return null;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(Digest.fromKeymasterToSignatureAlgorithmDigest(bestKeymasterDigest));
                stringBuilder.append("WithECDSA");
                return stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported algorithm: ");
            stringBuilder.append(keymasterAlgorithm);
            throw new ProviderException(stringBuilder.toString());
        } else if (!ArrayUtils.contains(SignaturePadding.allToKeymaster(spec.getSignaturePaddings()), 5)) {
            return null;
        } else {
            bestDigestOutputSizeBits = keySizeBits - 240;
            int bestKeymasterDigest2 = -1;
            keymasterDigest = -1;
            for (Integer keymasterDigest3 : getAvailableKeymasterSignatureDigests(spec.getDigests(), HwUniversalKeyStoreProvider.getSupportedEcdsaSignatureDigests())) {
                int keymasterDigest4 = keymasterDigest3.intValue();
                int outputSizeBits2 = HwKeymasterUtils.getDigestOutputSizeBits(keymasterDigest4);
                if (outputSizeBits2 <= bestDigestOutputSizeBits) {
                    if (bestKeymasterDigest2 == -1) {
                        bestKeymasterDigest2 = keymasterDigest4;
                        keymasterDigest = outputSizeBits2;
                    } else if (outputSizeBits2 > keymasterDigest) {
                        bestKeymasterDigest2 = keymasterDigest4;
                        keymasterDigest = outputSizeBits2;
                    }
                }
            }
            if (bestKeymasterDigest2 == -1) {
                return null;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(Digest.fromKeymasterToSignatureAlgorithmDigest(bestKeymasterDigest2));
            stringBuilder.append("WithRSA");
            return stringBuilder.toString();
        }
    }

    private static Set<Integer> getAvailableKeymasterSignatureDigests(String[] authorizedKeyDigests, String[] supportedSignatureDigests) {
        Set<Integer> authorizedKeymasterKeyDigests = new HashSet();
        int i = 0;
        for (int keymasterDigest : Digest.allToKeymaster(authorizedKeyDigests)) {
            authorizedKeymasterKeyDigests.add(Integer.valueOf(keymasterDigest));
        }
        Set<Integer> supportedKeymasterSignatureDigests = new HashSet();
        int[] allToKeymaster = Digest.allToKeymaster(supportedSignatureDigests);
        int length = allToKeymaster.length;
        while (i < length) {
            supportedKeymasterSignatureDigests.add(Integer.valueOf(allToKeymaster[i]));
            i++;
        }
        Set<Integer> result = new HashSet(supportedKeymasterSignatureDigests);
        result.retainAll(authorizedKeymasterKeyDigests);
        return result;
    }

    private Iterable<byte[]> getAttestationChain(String privateKeyAlias, KeyPair keyPair, HwKeymasterArguments args) throws ProviderException {
        HwKeymasterCertificateChain outChain = new HwKeymasterCertificateChain();
        int errorCode = this.mKeyStore.attestKey(privateKeyAlias, this.mEntryUid, args, outChain);
        if (errorCode == 1) {
            Collection<byte[]> chain = outChain.getCertificates();
            if (chain.size() >= 2) {
                return chain;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attestation certificate chain contained ");
            stringBuilder.append(chain.size());
            stringBuilder.append(" entries. At least two are required.");
            throw new ProviderException(stringBuilder.toString());
        }
        throw new ProviderException("Failed to generate attestation certificate chain", HwKeystoreManager.getKeyStoreException(errorCode));
    }

    private byte[] getAttestationChainBytes(String privateKeyAlias, KeyPair keyPair, HwKeymasterArguments args) throws ProviderException {
        Iterator<byte[]> iter = getAttestationChain(privateKeyAlias, keyPair, args).iterator();
        ByteArrayOutputStream certificateConcatenationStream = new ByteArrayOutputStream();
        do {
            byte[] data = (byte[]) iter.next();
            certificateConcatenationStream.write(data, 0, data.length);
        } while (iter.hasNext());
        return certificateConcatenationStream.toByteArray();
    }

    private void generateKeystoreKeyPair(String privateKeyAlias, HwKeymasterArguments args, byte[] additionalEntropy, int flags) throws ProviderException {
        String str = privateKeyAlias;
        HwKeymasterArguments hwKeymasterArguments = args;
        byte[] bArr = additionalEntropy;
        int errorCode = this.mKeyStore.generateKey(str, hwKeymasterArguments, bArr, this.mEntryUid, flags, new HwKeyCharacteristics());
        if (errorCode != 1) {
            throw new ProviderException("Failed to generate key pair", HwKeystoreManager.getKeyStoreException(errorCode));
        }
    }

    protected KeyPair loadKeystoreKeyPair(String privateKeyAlias) throws ProviderException {
        try {
            return HwUniversalKeyStoreProvider.loadAndroidKeyStoreKeyPairFromKeystore(this.mKeyStore, privateKeyAlias, this.mEntryUid);
        } catch (UnrecoverableKeyException e) {
            throw new ProviderException("Failed to load generated key pair from keystore", e);
        }
    }

    private HwKeymasterArguments constructKeyGenerationArguments() {
        HwKeymasterArguments args = new HwKeymasterArguments();
        args.addUnsignedInt(HwKeymasterDefs.KM_TAG_KEY_SIZE, (long) this.mKeySizeBits);
        args.addEnum(HwKeymasterDefs.KM_TAG_ALGORITHM, this.mKeymasterAlgorithm);
        args.addEnums(HwKeymasterDefs.KM_TAG_PURPOSE, this.mKeymasterPurposes);
        args.addEnums(HwKeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockModes);
        args.addEnums(HwKeymasterDefs.KM_TAG_PADDING, this.mKeymasterEncryptionPaddings);
        args.addEnums(HwKeymasterDefs.KM_TAG_PADDING, this.mKeymasterSignaturePaddings);
        args.addEnums(HwKeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigests);
        if (this.mSpec.isAdditionalProtectionAllowed()) {
            args.addBoolean(HwKeymasterDefs.KM_TAG_ADDITIONAL_PROTECTION_ALLOWED);
        }
        HwKeymasterUtils.addUserAuthArgs(args, this.mSpec.isUserAuthenticationRequired(), this.mSpec.getUserAuthenticationValidityDurationSeconds(), this.mSpec.isUserAuthenticationValidWhileOnBody(), this.mSpec.isInvalidatedByBiometricEnrollment(), 0);
        args.addDateIfNotNull(HwKeymasterDefs.KM_TAG_ACTIVE_DATETIME, this.mSpec.getKeyValidityStart());
        args.addDateIfNotNull(HwKeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, this.mSpec.getKeyValidityForOriginationEnd());
        args.addDateIfNotNull(HwKeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, this.mSpec.getKeyValidityForConsumptionEnd());
        addAlgorithmSpecificParameters(args);
        if (this.mSpec.isUniqueIdIncluded()) {
            args.addBoolean(HwKeymasterDefs.KM_TAG_INCLUDE_UNIQUE_ID);
        }
        return args;
    }

    private void addAlgorithmSpecificParameters(HwKeymasterArguments keymasterArgs) {
        int i = this.mKeymasterAlgorithm;
        if (i == 1) {
            keymasterArgs.addUnsignedLong(HwKeymasterDefs.KM_TAG_RSA_PUBLIC_EXPONENT, this.mRSAPublicExponent);
            addExtraParameters(keymasterArgs);
        } else if (i != 3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported algorithm: ");
            stringBuilder.append(this.mKeymasterAlgorithm);
            throw new ProviderException(stringBuilder.toString());
        }
    }

    private byte[] getRandomBytesToMixIntoKeystoreRng(SecureRandom rng, int sizeBytes) {
        if (sizeBytes <= 0) {
            return new byte[0];
        }
        if (rng == null) {
            rng = new SecureRandom();
        }
        byte[] result = new byte[sizeBytes];
        rng.nextBytes(result);
        return result;
    }

    protected String getEntryAlias(String keystoreAlias) {
        return keystoreAlias;
    }

    protected byte[] getChallenge(HwKeyGenParameterSpec mSpec) {
        return mSpec.getAttestationChallenge();
    }

    protected void addExtraParameters(HwKeymasterArguments keymasterArgs) {
    }

    protected HwKeystoreManager getKeyStoreManager() {
        return this.mKeyStore;
    }

    protected int getEntryUid() {
        return this.mEntryUid;
    }

    protected void resetAll() {
        this.mEntryAlias = null;
        this.mEntryUid = -1;
        this.mKeymasterAlgorithm = -1;
        this.mKeymasterPurposes = null;
        this.mKeymasterBlockModes = null;
        this.mKeymasterEncryptionPaddings = null;
        this.mKeymasterSignaturePaddings = null;
        this.mKeymasterDigests = null;
        this.mKeySizeBits = 0;
        this.mSpec = null;
        this.mRSAPublicExponent = null;
        this.mEncryptionAtRestRequired = false;
        this.mRng = null;
        this.mKeyStore = null;
    }
}
