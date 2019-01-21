package android.security.keystore;

import android.security.Credentials;
import android.security.KeyPairGeneratorSpec;
import android.security.KeyStore;
import android.security.KeyStore.State;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyGenParameterSpec.Builder;
import android.security.keystore.KeyProperties.BlockMode;
import android.security.keystore.KeyProperties.Digest;
import android.security.keystore.KeyProperties.EncryptionPadding;
import android.security.keystore.KeyProperties.KeyAlgorithm;
import android.security.keystore.KeyProperties.Purpose;
import android.telephony.ims.ImsReasonInfo;
import android.util.JlogConstants;
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
import libcore.util.EmptyArray;

public abstract class AndroidKeyStoreKeyPairGeneratorSpi extends KeyPairGeneratorSpi {
    private static final int EC_DEFAULT_KEY_SIZE = 256;
    private static final int RSA_DEFAULT_KEY_SIZE = 2048;
    private static final int RSA_MAX_KEY_SIZE = 8192;
    private static final int RSA_MIN_KEY_SIZE = 512;
    private static final List<String> SUPPORTED_EC_NIST_CURVE_NAMES = new ArrayList();
    private static final Map<String, Integer> SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE = new HashMap();
    private static final List<Integer> SUPPORTED_EC_NIST_CURVE_SIZES = new ArrayList();
    private boolean mEncryptionAtRestRequired;
    private String mEntryAlias;
    private int mEntryUid;
    private String mJcaKeyAlgorithm;
    private int mKeySizeBits;
    private KeyStore mKeyStore;
    private int mKeymasterAlgorithm = -1;
    private int[] mKeymasterBlockModes;
    private int[] mKeymasterDigests;
    private int[] mKeymasterEncryptionPaddings;
    private int[] mKeymasterPurposes;
    private int[] mKeymasterSignaturePaddings;
    private final int mOriginalKeymasterAlgorithm;
    private BigInteger mRSAPublicExponent;
    private SecureRandom mRng;
    private KeyGenParameterSpec mSpec;

    public static class EC extends AndroidKeyStoreKeyPairGeneratorSpi {
        public EC() {
            super(3);
        }
    }

    public static class RSA extends AndroidKeyStoreKeyPairGeneratorSpi {
        public RSA() {
            super(1);
        }
    }

    static {
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-224", Integer.valueOf(224));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp224r1", Integer.valueOf(224));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-256", Integer.valueOf(256));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp256r1", Integer.valueOf(256));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("prime256v1", Integer.valueOf(256));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-384", Integer.valueOf(JlogConstants.JLID_ACTIVITY_START_RECORD_TIME));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp384r1", Integer.valueOf(JlogConstants.JLID_ACTIVITY_START_RECORD_TIME));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("p-521", Integer.valueOf(ImsReasonInfo.CODE_USER_DECLINE_WITH_CAUSE));
        SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.put("secp521r1", Integer.valueOf(ImsReasonInfo.CODE_USER_DECLINE_WITH_CAUSE));
        SUPPORTED_EC_NIST_CURVE_NAMES.addAll(SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.keySet());
        Collections.sort(SUPPORTED_EC_NIST_CURVE_NAMES);
        SUPPORTED_EC_NIST_CURVE_SIZES.addAll(new HashSet(SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE.values()));
        Collections.sort(SUPPORTED_EC_NIST_CURVE_SIZES);
    }

    protected AndroidKeyStoreKeyPairGeneratorSpi(int keymasterAlgorithm) {
        this.mOriginalKeymasterAlgorithm = keymasterAlgorithm;
    }

    public void initialize(int keysize, SecureRandom random) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(KeyGenParameterSpec.class.getName());
        stringBuilder.append(" or ");
        stringBuilder.append(KeyPairGeneratorSpec.class.getName());
        stringBuilder.append(" required to initialize this KeyPairGenerator");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:85:0x0264  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        SecureRandom secureRandom;
        Throwable th;
        AlgorithmParameterSpec algorithmParameterSpec = params;
        resetAll();
        int i = 0;
        boolean success = false;
        if (algorithmParameterSpec != null) {
            boolean encryptionAtRestRequired = false;
            try {
                KeyPairGeneratorSpec legacySpec;
                int keymasterAlgorithm = this.mOriginalKeymasterAlgorithm;
                if (algorithmParameterSpec instanceof KeyGenParameterSpec) {
                    legacySpec = (KeyGenParameterSpec) algorithmParameterSpec;
                } else if (algorithmParameterSpec instanceof KeyPairGeneratorSpec) {
                    legacySpec = (KeyPairGeneratorSpec) algorithmParameterSpec;
                    try {
                        Builder specBuilder;
                        String specKeyAlgorithm = legacySpec.getKeyType();
                        if (specKeyAlgorithm != null) {
                            keymasterAlgorithm = KeyAlgorithm.toKeymasterAsymmetricKeyAlgorithm(specKeyAlgorithm);
                        }
                        if (keymasterAlgorithm == 1) {
                            specBuilder = new Builder(legacySpec.getKeystoreAlias(), 15);
                            specBuilder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
                            specBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
                            specBuilder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS);
                            specBuilder.setRandomizedEncryptionRequired(false);
                        } else if (keymasterAlgorithm == 3) {
                            specBuilder = new Builder(legacySpec.getKeystoreAlias(), 12);
                            specBuilder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unsupported algorithm: ");
                            stringBuilder.append(this.mKeymasterAlgorithm);
                            throw new ProviderException(stringBuilder.toString());
                        }
                        if (legacySpec.getKeySize() != -1) {
                            specBuilder.setKeySize(legacySpec.getKeySize());
                        }
                        if (legacySpec.getAlgorithmParameterSpec() != null) {
                            specBuilder.setAlgorithmParameterSpec(legacySpec.getAlgorithmParameterSpec());
                        }
                        specBuilder.setCertificateSubject(legacySpec.getSubjectDN());
                        specBuilder.setCertificateSerialNumber(legacySpec.getSerialNumber());
                        specBuilder.setCertificateNotBefore(legacySpec.getStartDate());
                        specBuilder.setCertificateNotAfter(legacySpec.getEndDate());
                        encryptionAtRestRequired = legacySpec.isEncryptionRequired();
                        specBuilder.setUserAuthenticationRequired(false);
                        legacySpec = specBuilder.build();
                    } catch (IllegalArgumentException e) {
                        IllegalArgumentException illegalArgumentException = e;
                        throw new InvalidAlgorithmParameterException("Invalid key type in parameters", e);
                    } catch (IllegalArgumentException | NullPointerException e2) {
                        secureRandom = random;
                        throw new InvalidAlgorithmParameterException(e2);
                    }
                } else {
                    secureRandom = random;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unsupported params class: ");
                    stringBuilder2.append(params.getClass().getName());
                    stringBuilder2.append(". Supported: ");
                    stringBuilder2.append(KeyGenParameterSpec.class.getName());
                    stringBuilder2.append(", ");
                    stringBuilder2.append(KeyPairGeneratorSpec.class.getName());
                    throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
                }
                this.mEntryAlias = legacySpec.getKeystoreAlias();
                this.mEntryUid = legacySpec.getUid();
                this.mSpec = legacySpec;
                this.mKeymasterAlgorithm = keymasterAlgorithm;
                this.mEncryptionAtRestRequired = encryptionAtRestRequired;
                this.mKeySizeBits = legacySpec.getKeySize();
                initAlgorithmSpecificParameters();
                if (this.mKeySizeBits == -1) {
                    this.mKeySizeBits = getDefaultKeySize(keymasterAlgorithm);
                }
                checkValidKeySize(keymasterAlgorithm, this.mKeySizeBits);
                if (legacySpec.getKeystoreAlias() != null) {
                    try {
                        String jcaKeyAlgorithm = KeyAlgorithm.fromKeymasterAsymmetricKeyAlgorithm(keymasterAlgorithm);
                        this.mKeymasterPurposes = Purpose.allToKeymaster(legacySpec.getPurposes());
                        this.mKeymasterBlockModes = BlockMode.allToKeymaster(legacySpec.getBlockModes());
                        this.mKeymasterEncryptionPaddings = EncryptionPadding.allToKeymaster(legacySpec.getEncryptionPaddings());
                        if ((1 & legacySpec.getPurposes()) != 0 && legacySpec.isRandomizedEncryptionRequired()) {
                            int[] iArr = this.mKeymasterEncryptionPaddings;
                            int length = iArr.length;
                            while (i < length) {
                                int keymasterPadding = iArr[i];
                                if (KeymasterUtils.isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(keymasterPadding)) {
                                    i++;
                                } else {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Randomized encryption (IND-CPA) required but may be violated by padding scheme: ");
                                    stringBuilder3.append(EncryptionPadding.fromKeymaster(keymasterPadding));
                                    stringBuilder3.append(". See ");
                                    stringBuilder3.append(KeyGenParameterSpec.class.getName());
                                    stringBuilder3.append(" documentation.");
                                    throw new InvalidAlgorithmParameterException(stringBuilder3.toString());
                                }
                            }
                        }
                        this.mKeymasterSignaturePaddings = SignaturePadding.allToKeymaster(legacySpec.getSignaturePaddings());
                        if (legacySpec.isDigestsSpecified()) {
                            this.mKeymasterDigests = Digest.allToKeymaster(legacySpec.getDigests());
                        } else {
                            this.mKeymasterDigests = EmptyArray.INT;
                        }
                        KeymasterUtils.addUserAuthArgs(new KeymasterArguments(), this.mSpec);
                        this.mJcaKeyAlgorithm = jcaKeyAlgorithm;
                        this.mRng = random;
                        this.mKeyStore = KeyStore.getInstance();
                        if (!true) {
                            resetAll();
                            return;
                        }
                        return;
                    } catch (IllegalArgumentException | IllegalStateException e22) {
                        secureRandom = random;
                        throw new InvalidAlgorithmParameterException(e22);
                    } catch (Throwable th2) {
                        th = th2;
                        if (!success) {
                        }
                        throw th;
                    }
                }
                secureRandom = random;
                throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");
            } catch (Throwable th3) {
                th = th3;
                secureRandom = random;
                if (success) {
                    resetAll();
                }
                throw th;
            }
        }
        secureRandom = random;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Must supply params of type ");
        stringBuilder4.append(KeyGenParameterSpec.class.getName());
        stringBuilder4.append(" or ");
        stringBuilder4.append(KeyPairGeneratorSpec.class.getName());
        throw new InvalidAlgorithmParameterException(stringBuilder4.toString());
    }

    private void resetAll() {
        this.mEntryAlias = null;
        this.mEntryUid = -1;
        this.mJcaKeyAlgorithm = null;
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
            } else if (publicExponent.compareTo(KeymasterArguments.UINT64_MAX_VALUE) <= 0) {
                this.mRSAPublicExponent = publicExponent;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported RSA public exponent: ");
                stringBuilder.append(publicExponent);
                stringBuilder.append(". Maximum supported value: ");
                stringBuilder.append(KeymasterArguments.UINT64_MAX_VALUE);
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

    public KeyPair generateKeyPair() {
        if (this.mKeyStore == null || this.mSpec == null) {
            throw new IllegalStateException("Not initialized");
        }
        int flags = this.mEncryptionAtRestRequired;
        if ((flags & 1) == 0 || this.mKeyStore.state() == State.UNLOCKED) {
            if (this.mSpec.isStrongBoxBacked()) {
                flags |= 16;
            }
            byte[] additionalEntropy = KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, (this.mKeySizeBits + 7) / 8);
            Credentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
            String privateKeyAlias = new StringBuilder();
            privateKeyAlias.append(Credentials.USER_PRIVATE_KEY);
            privateKeyAlias.append(this.mEntryAlias);
            privateKeyAlias = privateKeyAlias.toString();
            try {
                generateKeystoreKeyPair(privateKeyAlias, constructKeyGenerationArguments(), additionalEntropy, flags);
                KeyPair keyPair = loadKeystoreKeyPair(privateKeyAlias);
                storeCertificateChain(flags, createCertificateChain(privateKeyAlias, keyPair));
                if (!true) {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
                }
                return keyPair;
            } catch (ProviderException e) {
                if ((this.mSpec.getPurposes() & 32) != 0) {
                    throw new SecureKeyImportUnavailableException(e);
                }
                throw e;
            } catch (Throwable th) {
                if (!false) {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, this.mEntryAlias, this.mEntryUid);
                }
            }
        } else {
            throw new IllegalStateException("Encryption at rest using secure lock screen credential requested for key pair, but the user has not yet entered the credential");
        }
    }

    private Iterable<byte[]> createCertificateChain(String privateKeyAlias, KeyPair keyPair) throws ProviderException {
        byte[] challenge = this.mSpec.getAttestationChallenge();
        if (challenge == null) {
            return Collections.singleton(generateSelfSignedCertificateBytes(keyPair));
        }
        KeymasterArguments args = new KeymasterArguments();
        args.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, challenge);
        return getAttestationChain(privateKeyAlias, keyPair, args);
    }

    private void generateKeystoreKeyPair(String privateKeyAlias, KeymasterArguments args, byte[] additionalEntropy, int flags) throws ProviderException {
        String str = privateKeyAlias;
        KeymasterArguments keymasterArguments = args;
        byte[] bArr = additionalEntropy;
        int errorCode = this.mKeyStore.generateKey(str, keymasterArguments, bArr, this.mEntryUid, flags, new KeyCharacteristics());
        if (errorCode == 1) {
            return;
        }
        if (errorCode == -68) {
            throw new StrongBoxUnavailableException("Failed to generate key pair");
        }
        throw new ProviderException("Failed to generate key pair", KeyStore.getKeyStoreException(errorCode));
    }

    private KeyPair loadKeystoreKeyPair(String privateKeyAlias) throws ProviderException {
        try {
            KeyPair result = AndroidKeyStoreProvider.loadAndroidKeyStoreKeyPairFromKeystore(this.mKeyStore, privateKeyAlias, this.mEntryUid);
            if (this.mJcaKeyAlgorithm.equalsIgnoreCase(result.getPrivate().getAlgorithm())) {
                return result;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Generated key pair algorithm does not match requested algorithm: ");
            stringBuilder.append(result.getPrivate().getAlgorithm());
            stringBuilder.append(" vs ");
            stringBuilder.append(this.mJcaKeyAlgorithm);
            throw new ProviderException(stringBuilder.toString());
        } catch (UnrecoverableKeyException e) {
            throw new ProviderException("Failed to load generated key pair from keystore", e);
        }
    }

    private KeymasterArguments constructKeyGenerationArguments() {
        KeymasterArguments args = new KeymasterArguments();
        args.addUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, (long) this.mKeySizeBits);
        args.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, this.mKeymasterAlgorithm);
        args.addEnums(KeymasterDefs.KM_TAG_PURPOSE, this.mKeymasterPurposes);
        args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockModes);
        args.addEnums(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterEncryptionPaddings);
        args.addEnums(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterSignaturePaddings);
        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigests);
        KeymasterUtils.addUserAuthArgs(args, this.mSpec);
        args.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, this.mSpec.getKeyValidityStart());
        args.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, this.mSpec.getKeyValidityForOriginationEnd());
        args.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, this.mSpec.getKeyValidityForConsumptionEnd());
        addAlgorithmSpecificParameters(args);
        if (this.mSpec.isUniqueIdIncluded()) {
            args.addBoolean(KeymasterDefs.KM_TAG_INCLUDE_UNIQUE_ID);
        }
        return args;
    }

    private void storeCertificateChain(int flags, Iterable<byte[]> iterable) throws ProviderException {
        Iterator<byte[]> iter = iterable.iterator();
        storeCertificate(Credentials.USER_CERTIFICATE, (byte[]) iter.next(), flags, "Failed to store certificate");
        if (iter.hasNext()) {
            ByteArrayOutputStream certificateConcatenationStream = new ByteArrayOutputStream();
            while (iter.hasNext()) {
                byte[] data = (byte[]) iter.next();
                certificateConcatenationStream.write(data, 0, data.length);
            }
            storeCertificate(Credentials.CA_CERTIFICATE, certificateConcatenationStream.toByteArray(), flags, "Failed to store attestation CA certificate");
        }
    }

    private void storeCertificate(String prefix, byte[] certificateBytes, int flags, String failureMessage) throws ProviderException {
        int insertErrorCode = this.mKeyStore;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(this.mEntryAlias);
        insertErrorCode = insertErrorCode.insert(stringBuilder.toString(), certificateBytes, this.mEntryUid, flags);
        if (insertErrorCode != 1) {
            throw new ProviderException(failureMessage, KeyStore.getKeyStoreException(insertErrorCode));
        }
    }

    private byte[] generateSelfSignedCertificateBytes(KeyPair keyPair) throws ProviderException {
        try {
            return generateSelfSignedCertificate(keyPair.getPrivate(), keyPair.getPublic()).getEncoded();
        } catch (IOException | CertificateParsingException e) {
            throw new ProviderException("Failed to generate self-signed certificate", e);
        } catch (CertificateEncodingException e2) {
            throw new ProviderException("Failed to obtain encoded form of self-signed certificate", e2);
        }
    }

    private Iterable<byte[]> getAttestationChain(String privateKeyAlias, KeyPair keyPair, KeymasterArguments args) throws ProviderException {
        KeymasterCertificateChain outChain = new KeymasterCertificateChain();
        int errorCode = this.mKeyStore.attestKey(privateKeyAlias, args, outChain);
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
        throw new ProviderException("Failed to generate attestation certificate chain", KeyStore.getKeyStoreException(errorCode));
    }

    private void addAlgorithmSpecificParameters(KeymasterArguments keymasterArgs) {
        int i = this.mKeymasterAlgorithm;
        if (i == 1) {
            keymasterArgs.addUnsignedLong(KeymasterDefs.KM_TAG_RSA_PUBLIC_EXPONENT, this.mRSAPublicExponent);
        } else if (i != 3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported algorithm: ");
            stringBuilder.append(this.mKeymasterAlgorithm);
            throw new ProviderException(stringBuilder.toString());
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

    /* JADX WARNING: Missing block: B:17:0x00e1, code skipped:
            if (r5 != null) goto L_0x00e3;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:20:0x00e7, code skipped:
            r7 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:0x00e8, code skipped:
            r5.addSuppressed(r7);
     */
    /* JADX WARNING: Missing block: B:22:0x00ec, code skipped:
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

    private static int getDefaultKeySize(int keymasterAlgorithm) {
        if (keymasterAlgorithm == 1) {
            return 2048;
        }
        if (keymasterAlgorithm == 3) {
            return 256;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported algorithm: ");
        stringBuilder.append(keymasterAlgorithm);
        throw new ProviderException(stringBuilder.toString());
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
        } else if (keySize < 512 || keySize > 8192) {
            throw new InvalidAlgorithmParameterException("RSA key size must be >= 512 and <= 8192");
        }
    }

    private static String getCertificateSignatureAlgorithm(int keymasterAlgorithm, int keySizeBits, KeyGenParameterSpec spec) {
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
                for (Integer keymasterDigest2 : getAvailableKeymasterSignatureDigests(spec.getDigests(), AndroidKeyStoreBCWorkaroundProvider.getSupportedEcdsaSignatureDigests())) {
                    keymasterDigest = keymasterDigest2.intValue();
                    int outputSizeBits = KeymasterUtils.getDigestOutputSizeBits(keymasterDigest);
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
            for (Integer keymasterDigest3 : getAvailableKeymasterSignatureDigests(spec.getDigests(), AndroidKeyStoreBCWorkaroundProvider.getSupportedEcdsaSignatureDigests())) {
                int keymasterDigest4 = keymasterDigest3.intValue();
                int outputSizeBits2 = KeymasterUtils.getDigestOutputSizeBits(keymasterDigest4);
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
}
