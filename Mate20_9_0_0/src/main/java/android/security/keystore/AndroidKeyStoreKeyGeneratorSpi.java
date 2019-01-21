package android.security.keystore;

import android.security.Credentials;
import android.security.KeyStore;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties.BlockMode;
import android.security.keystore.KeyProperties.Digest;
import android.security.keystore.KeyProperties.EncryptionPadding;
import android.security.keystore.KeyProperties.KeyAlgorithm;
import android.security.keystore.KeyProperties.Purpose;
import java.security.InvalidAlgorithmParameterException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import libcore.util.EmptyArray;

public abstract class AndroidKeyStoreKeyGeneratorSpi extends KeyGeneratorSpi {
    private final int mDefaultKeySizeBits;
    protected int mKeySizeBits;
    private final KeyStore mKeyStore;
    private final int mKeymasterAlgorithm;
    private int[] mKeymasterBlockModes;
    private final int mKeymasterDigest;
    private int[] mKeymasterDigests;
    private int[] mKeymasterPaddings;
    private int[] mKeymasterPurposes;
    private SecureRandom mRng;
    private KeyGenParameterSpec mSpec;

    public static class AES extends AndroidKeyStoreKeyGeneratorSpi {
        public AES() {
            super(32, 128);
        }

        protected void engineInit(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
            super.engineInit(params, random);
            if (this.mKeySizeBits != 128 && this.mKeySizeBits != 192 && this.mKeySizeBits != 256) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported key size: ");
                stringBuilder.append(this.mKeySizeBits);
                stringBuilder.append(". Supported: 128, 192, 256.");
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }
    }

    public static class DESede extends AndroidKeyStoreKeyGeneratorSpi {
        public DESede() {
            super(33, 168);
        }
    }

    protected static abstract class HmacBase extends AndroidKeyStoreKeyGeneratorSpi {
        protected HmacBase(int keymasterDigest) {
            super(128, keymasterDigest, KeymasterUtils.getDigestOutputSizeBits(keymasterDigest));
        }
    }

    public static class HmacSHA1 extends HmacBase {
        public HmacSHA1() {
            super(2);
        }
    }

    public static class HmacSHA224 extends HmacBase {
        public HmacSHA224() {
            super(3);
        }
    }

    public static class HmacSHA256 extends HmacBase {
        public HmacSHA256() {
            super(4);
        }
    }

    public static class HmacSHA384 extends HmacBase {
        public HmacSHA384() {
            super(5);
        }
    }

    public static class HmacSHA512 extends HmacBase {
        public HmacSHA512() {
            super(6);
        }
    }

    protected AndroidKeyStoreKeyGeneratorSpi(int keymasterAlgorithm, int defaultKeySizeBits) {
        this(keymasterAlgorithm, -1, defaultKeySizeBits);
    }

    protected AndroidKeyStoreKeyGeneratorSpi(int keymasterAlgorithm, int keymasterDigest, int defaultKeySizeBits) {
        this.mKeyStore = KeyStore.getInstance();
        this.mKeymasterAlgorithm = keymasterAlgorithm;
        this.mKeymasterDigest = keymasterDigest;
        this.mDefaultKeySizeBits = defaultKeySizeBits;
        if (this.mDefaultKeySizeBits <= 0) {
            throw new IllegalArgumentException("Default key size must be positive");
        } else if (this.mKeymasterAlgorithm == 128 && this.mKeymasterDigest == -1) {
            throw new IllegalArgumentException("Digest algorithm must be specified for HMAC key");
        }
    }

    protected void engineInit(SecureRandom random) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot initialize without a ");
        stringBuilder.append(KeyGenParameterSpec.class.getName());
        stringBuilder.append(" parameter");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }

    protected void engineInit(int keySize, SecureRandom random) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot initialize without a ");
        stringBuilder.append(KeyGenParameterSpec.class.getName());
        stringBuilder.append(" parameter");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }

    protected void engineInit(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        resetAll();
        boolean success = false;
        if (params != null) {
            try {
                if (params instanceof KeyGenParameterSpec) {
                    KeyGenParameterSpec spec = (KeyGenParameterSpec) params;
                    if (spec.getKeystoreAlias() != null) {
                        this.mRng = random;
                        this.mSpec = spec;
                        this.mKeySizeBits = spec.getKeySize() != -1 ? spec.getKeySize() : this.mDefaultKeySizeBits;
                        StringBuilder stringBuilder;
                        if (this.mKeySizeBits <= 0) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Key size must be positive: ");
                            stringBuilder.append(this.mKeySizeBits);
                            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                        } else if (this.mKeySizeBits % 8 == 0) {
                            this.mKeymasterPurposes = Purpose.allToKeymaster(spec.getPurposes());
                            this.mKeymasterPaddings = EncryptionPadding.allToKeymaster(spec.getEncryptionPaddings());
                            if (spec.getSignaturePaddings().length <= 0) {
                                int[] iArr;
                                this.mKeymasterBlockModes = BlockMode.allToKeymaster(spec.getBlockModes());
                                if ((spec.getPurposes() & 1) != 0 && spec.isRandomizedEncryptionRequired()) {
                                    iArr = this.mKeymasterBlockModes;
                                    int length = iArr.length;
                                    int i = 0;
                                    while (i < length) {
                                        int keymasterBlockMode = iArr[i];
                                        if (KeymasterUtils.isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(keymasterBlockMode)) {
                                            i++;
                                        } else {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Randomized encryption (IND-CPA) required but may be violated by block mode: ");
                                            stringBuilder.append(BlockMode.fromKeymaster(keymasterBlockMode));
                                            stringBuilder.append(". See ");
                                            stringBuilder.append(KeyGenParameterSpec.class.getName());
                                            stringBuilder.append(" documentation.");
                                            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                                        }
                                    }
                                }
                                if (this.mKeymasterAlgorithm == 128) {
                                    if (this.mKeySizeBits >= 64) {
                                        this.mKeymasterDigests = new int[]{this.mKeymasterDigest};
                                        if (spec.isDigestsSpecified()) {
                                            iArr = Digest.allToKeymaster(spec.getDigests());
                                            if (iArr.length != 1 || iArr[0] != this.mKeymasterDigest) {
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Unsupported digests specification: ");
                                                stringBuilder2.append(Arrays.asList(spec.getDigests()));
                                                stringBuilder2.append(". Only ");
                                                stringBuilder2.append(Digest.fromKeymaster(this.mKeymasterDigest));
                                                stringBuilder2.append(" supported for this HMAC key algorithm");
                                                throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
                                            }
                                        }
                                    } else {
                                        throw new InvalidAlgorithmParameterException("HMAC key size must be at least 64 bits.");
                                    }
                                } else if (spec.isDigestsSpecified()) {
                                    this.mKeymasterDigests = Digest.allToKeymaster(spec.getDigests());
                                } else {
                                    this.mKeymasterDigests = EmptyArray.INT;
                                }
                                KeymasterUtils.addUserAuthArgs(new KeymasterArguments(), spec);
                                if (!true) {
                                    resetAll();
                                    return;
                                }
                                return;
                            }
                            throw new InvalidAlgorithmParameterException("Signature paddings not supported for symmetric key algorithms");
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Key size must be a multiple of 8: ");
                            stringBuilder.append(this.mKeySizeBits);
                            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                        }
                    }
                    throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new InvalidAlgorithmParameterException(e);
            } catch (Throwable th) {
                if (!success) {
                    resetAll();
                }
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Cannot initialize without a ");
        stringBuilder3.append(KeyGenParameterSpec.class.getName());
        stringBuilder3.append(" parameter");
        throw new InvalidAlgorithmParameterException(stringBuilder3.toString());
    }

    private void resetAll() {
        this.mSpec = null;
        this.mRng = null;
        this.mKeySizeBits = -1;
        this.mKeymasterPurposes = null;
        this.mKeymasterPaddings = null;
        this.mKeymasterBlockModes = null;
    }

    protected SecretKey engineGenerateKey() {
        KeyGenParameterSpec spec = this.mSpec;
        if (spec != null) {
            KeymasterArguments args = new KeymasterArguments();
            args.addUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, (long) this.mKeySizeBits);
            args.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, this.mKeymasterAlgorithm);
            args.addEnums(KeymasterDefs.KM_TAG_PURPOSE, this.mKeymasterPurposes);
            args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, this.mKeymasterBlockModes);
            args.addEnums(KeymasterDefs.KM_TAG_PADDING, this.mKeymasterPaddings);
            args.addEnums(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigests);
            KeymasterUtils.addUserAuthArgs(args, spec);
            KeymasterUtils.addMinMacLengthAuthorizationIfNecessary(args, this.mKeymasterAlgorithm, this.mKeymasterBlockModes, this.mKeymasterDigests);
            args.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, spec.getKeyValidityStart());
            args.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, spec.getKeyValidityForOriginationEnd());
            args.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, spec.getKeyValidityForConsumptionEnd());
            if (!((spec.getPurposes() & 1) == 0 || spec.isRandomizedEncryptionRequired())) {
                args.addBoolean(KeymasterDefs.KM_TAG_CALLER_NONCE);
            }
            byte[] additionalEntropy = KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, (this.mKeySizeBits + 7) / 8);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_PRIVATE_KEY);
            stringBuilder.append(spec.getKeystoreAlias());
            String keyAliasInKeystore = stringBuilder.toString();
            KeyCharacteristics resultingKeyCharacteristics = new KeyCharacteristics();
            boolean success = false;
            try {
                Credentials.deleteAllTypesForAlias(this.mKeyStore, spec.getKeystoreAlias(), spec.getUid());
                int errorCode = this.mKeyStore.generateKey(keyAliasInKeystore, args, additionalEntropy, spec.getUid(), 0, resultingKeyCharacteristics);
                if (errorCode == 1) {
                    SecretKey result = new AndroidKeyStoreSecretKey(keyAliasInKeystore, spec.getUid(), KeyAlgorithm.fromKeymasterSecretKeyAlgorithm(this.mKeymasterAlgorithm, this.mKeymasterDigest));
                    if (!true) {
                        Credentials.deleteAllTypesForAlias(this.mKeyStore, spec.getKeystoreAlias(), spec.getUid());
                    }
                    return result;
                }
                throw new ProviderException("Keystore operation failed", KeyStore.getKeyStoreException(errorCode));
            } catch (IllegalArgumentException e) {
                throw new ProviderException("Failed to obtain JCA secret key algorithm name", e);
            } catch (Throwable th) {
                if (!success) {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, spec.getKeystoreAlias(), spec.getUid());
                }
            }
        } else {
            throw new IllegalStateException("Not initialized");
        }
    }
}
