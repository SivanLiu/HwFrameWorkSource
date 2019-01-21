package com.huawei.security.keystore;

import android.os.IBinder;
import android.security.keystore.KeyStoreCryptoOperation;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.keymaster.HwKeyCharacteristics;
import com.huawei.security.keymaster.HwKeymasterArguments;
import com.huawei.security.keymaster.HwKeymasterDefs;
import com.huawei.security.keymaster.HwKeymasterUtils;
import com.huawei.security.keymaster.HwOperationResult;
import com.huawei.security.keystore.ArrayUtils.EmptyArray;
import com.huawei.security.keystore.HwKeyProperties.Digest;
import com.huawei.security.keystore.HwKeyProperties.EncryptionPadding;
import com.huawei.security.keystore.HwUniversalKeyStoreCryptoOperationStreamer.MainDataStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;

public class HwUniversalKeyStoreRSACipherSpi extends CipherSpi implements KeyStoreCryptoOperation {
    private HwUniversalKeyStoreCryptoOperationStreamer mAdditionalAuthenticationDataStreamer;
    private boolean mAdditionalAuthenticationDataStreamerClosed;
    private Exception mCachedException;
    private boolean mEncrypting;
    private HwUniversalKeyStoreKey mKey;
    private final HwKeystoreManager mKeyStore = HwKeystoreManager.getInstance();
    private int mKeymasterPadding;
    private int mKeymasterPaddingOverride = -1;
    private int mKeymasterPurposeOverride = -1;
    private HwUniversalKeyStoreCryptoOperationStreamer mMainDataStreamer;
    private int mModulusSizeBytes = -1;
    private long mOperationHandle;
    private IBinder mOperationToken;
    private SecureRandom mRng;

    static class OAEPWithMGF1Padding extends HwUniversalKeyStoreRSACipherSpi {
        private static final String MGF_ALGORITGM_MGF1 = "MGF1";
        private int mDigestOutputSizeBytes;
        private int mKeymasterDigest = -1;

        OAEPWithMGF1Padding(int keymasterDigest) {
            super(2);
            this.mKeymasterDigest = keymasterDigest;
            this.mDigestOutputSizeBytes = (HwKeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
        }

        protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        protected final void initAlgorithmSpecificParameters(@Nullable AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                if (params instanceof OAEPParameterSpec) {
                    OAEPParameterSpec spec = (OAEPParameterSpec) params;
                    if (MGF_ALGORITGM_MGF1.equalsIgnoreCase(spec.getMGFAlgorithm())) {
                        String jcaDigest = spec.getDigestAlgorithm();
                        StringBuilder stringBuilder;
                        try {
                            int keymasterDigest = Digest.toKeymaster(jcaDigest);
                            switch (keymasterDigest) {
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                    AlgorithmParameterSpec mgfParams = spec.getMGFParameters();
                                    if (mgfParams == null) {
                                        throw new InvalidAlgorithmParameterException("MGF parameters must be provided");
                                    } else if (mgfParams instanceof MGF1ParameterSpec) {
                                        String mgf1JcaDigest = ((MGF1ParameterSpec) mgfParams).getDigestAlgorithm();
                                        if (HwKeyProperties.DIGEST_SHA1.equalsIgnoreCase(mgf1JcaDigest)) {
                                            PSource pSource = spec.getPSource();
                                            if (pSource instanceof PSpecified) {
                                                byte[] pSourceValue = ((PSpecified) pSource).getValue();
                                                if (pSourceValue == null || pSourceValue.length <= 0) {
                                                    this.mKeymasterDigest = keymasterDigest;
                                                    this.mDigestOutputSizeBytes = (HwKeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
                                                    return;
                                                }
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Unsupported source of encoding input P: ");
                                                stringBuilder2.append(pSource);
                                                stringBuilder2.append(". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                                                throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
                                            }
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Unsupported source of encoding input P: ");
                                            stringBuilder3.append(pSource);
                                            stringBuilder3.append(". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                                            throw new InvalidAlgorithmParameterException(stringBuilder3.toString());
                                        }
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Unsupported MGF1 digest: ");
                                        stringBuilder4.append(mgf1JcaDigest);
                                        stringBuilder4.append(". Only ");
                                        stringBuilder4.append(HwKeyProperties.DIGEST_SHA1);
                                        stringBuilder4.append(" supported");
                                        throw new InvalidAlgorithmParameterException(stringBuilder4.toString());
                                    } else {
                                        StringBuilder stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("Unsupported MGF parameters: ");
                                        stringBuilder5.append(mgfParams);
                                        stringBuilder5.append(". Only MGF1ParameterSpec supported");
                                        throw new InvalidAlgorithmParameterException(stringBuilder5.toString());
                                    }
                                default:
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unsupported digest: ");
                                    stringBuilder.append(jcaDigest);
                                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                            }
                        } catch (IllegalArgumentException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unsupported digest: ");
                            stringBuilder.append(jcaDigest);
                            throw new InvalidAlgorithmParameterException(stringBuilder.toString(), e);
                        }
                    }
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("Unsupported MGF: ");
                    stringBuilder6.append(spec.getMGFAlgorithm());
                    stringBuilder6.append(". Only ");
                    stringBuilder6.append(MGF_ALGORITGM_MGF1);
                    stringBuilder6.append(" supported");
                    throw new InvalidAlgorithmParameterException(stringBuilder6.toString());
                }
                StringBuilder stringBuilder7 = new StringBuilder();
                stringBuilder7.append("Unsupported parameter spec: ");
                stringBuilder7.append(params);
                stringBuilder7.append(". Only OAEPParameterSpec supported");
                throw new InvalidAlgorithmParameterException(stringBuilder7.toString());
            }
        }

        protected final void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                StringBuilder stringBuilder;
                try {
                    AlgorithmParameterSpec spec = (OAEPParameterSpec) params.getParameterSpec(OAEPParameterSpec.class);
                    if (spec != null) {
                        initAlgorithmSpecificParameters(spec);
                        return;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("OAEP parameters required, but not provided in parameters: ");
                    stringBuilder.append(params);
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                } catch (InvalidParameterSpecException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("OAEP parameters required, but not found in parameters: ");
                    stringBuilder.append(params);
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString(), e);
                }
            }
        }

        protected final AlgorithmParameters engineGetParameters() {
            OAEPParameterSpec spec = new OAEPParameterSpec(Digest.fromKeymaster(this.mKeymasterDigest), MGF_ALGORITGM_MGF1, MGF1ParameterSpec.SHA1, PSpecified.DEFAULT);
            try {
                AlgorithmParameters params = AlgorithmParameters.getInstance("OAEP");
                params.init(spec);
                return params;
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException("Failed to obtain OAEP AlgorithmParameters", e);
            } catch (InvalidParameterSpecException e2) {
                throw new ProviderException("Failed to initialize OAEP AlgorithmParameters with an IV", e2);
            }
        }

        protected final void addAlgorithmSpecificParametersToBegin(HwKeymasterArguments keymasterArgs) {
            super.addAlgorithmSpecificParametersToBegin(keymasterArgs);
            keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
        }

        protected final void loadAlgorithmSpecificParametersFromBeginResult(@NonNull HwKeymasterArguments keymasterArgs) {
            super.loadAlgorithmSpecificParametersFromBeginResult(keymasterArgs);
        }

        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        protected final int getAdditionalEntropyAmountForFinish() {
            return isEncrypting() ? this.mDigestOutputSizeBytes : 0;
        }
    }

    public static final class PKCS1Padding extends HwUniversalKeyStoreRSACipherSpi {
        public PKCS1Padding() {
            super(4);
        }

        protected boolean adjustConfigForEncryptingWithPrivateKey() {
            setKeymasterPurposeOverride(2);
            setKeymasterPaddingOverride(5);
            return true;
        }

        protected void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected parameters: ");
                stringBuilder.append(params);
                stringBuilder.append(". No parameters supported");
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }

        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected parameters: ");
                stringBuilder.append(params);
                stringBuilder.append(". No parameters supported");
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }

        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        protected final int getAdditionalEntropyAmountForFinish() {
            return isEncrypting() ? getModulusSizeBytes() : 0;
        }
    }

    public static class OAEPWithSHA256AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA256AndMGF1Padding() {
            super(4);
        }
    }

    public static class OAEPWithSHA384AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA384AndMGF1Padding() {
            super(5);
        }
    }

    public static class OAEPWithSHA512AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA512AndMGF1Padding() {
            super(6);
        }
    }

    HwUniversalKeyStoreRSACipherSpi(int keymasterPadding) {
        this.mKeymasterPadding = keymasterPadding;
    }

    protected final void engineSetMode(String mode) throws NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    protected final void engineSetPadding(String arg0) throws NoSuchPaddingException {
        throw new UnsupportedOperationException();
    }

    protected final int engineGetBlockSize() {
        return 0;
    }

    protected final int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected final int engineGetOutputSize(int inputLen) {
        return getModulusSizeBytes();
    }

    protected final int getModulusSizeBytes() {
        if (this.mModulusSizeBytes != -1) {
            return this.mModulusSizeBytes;
        }
        throw new IllegalStateException("Not initialized");
    }

    protected final byte[] engineGetIV() {
        return null;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    protected void resetAll() {
        this.mModulusSizeBytes = -1;
        this.mKeymasterPaddingOverride = -1;
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mKeyStore.abort(operationToken);
        }
        this.mEncrypting = false;
        this.mKeymasterPurposeOverride = -1;
        this.mKey = null;
        this.mRng = null;
        this.mOperationToken = null;
        this.mOperationHandle = 0;
        this.mMainDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamerClosed = false;
        this.mCachedException = null;
    }

    @CallSuper
    protected void resetWhilePreservingInitState() {
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mKeyStore.abort(operationToken);
        }
        this.mOperationToken = null;
        this.mOperationHandle = 0;
        this.mMainDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamer = null;
        this.mAdditionalAuthenticationDataStreamerClosed = false;
        this.mCachedException = null;
    }

    protected void addAlgorithmSpecificParametersToBegin(HwKeymasterArguments keymasterArgs) {
        keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_ALGORITHM, 1);
        int keymasterPadding = getKeymasterPaddingOverride();
        if (keymasterPadding == -1) {
            keymasterPadding = this.mKeymasterPadding;
        }
        keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_PADDING, keymasterPadding);
        int purposeOverride = getKeymasterPurposeOverride();
        if (purposeOverride == -1) {
            return;
        }
        if (purposeOverride == 2 || purposeOverride == 3) {
            keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_DIGEST, 0);
        }
    }

    protected int getAdditionalEntropyAmountForBegin() {
        return 0;
    }

    private void ensureKeystoreOperationInitialized() throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.mMainDataStreamer != null || this.mCachedException != null) {
            return;
        }
        if (this.mKey != null) {
            HwKeymasterArguments keymasterInputArgs = new HwKeymasterArguments();
            addAlgorithmSpecificParametersToBegin(keymasterInputArgs);
            byte[] additionalEntropy = HwUniversalKeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForBegin());
            int i = this.mKeymasterPurposeOverride != -1 ? this.mKeymasterPurposeOverride : this.mEncrypting ? 0 : 1;
            HwOperationResult opResult = this.mKeyStore.begin(this.mKey.getAlias(), i, true, keymasterInputArgs, additionalEntropy, this.mKey.getUid());
            if (opResult != null) {
                this.mOperationToken = opResult.token;
                this.mOperationHandle = opResult.operationHandle;
                GeneralSecurityException e = HwUniversalKeyStoreCryptoOperationUtils.getExceptionForCipherInit(this.mKeyStore, this.mKey, opResult.resultCode);
                if (e != null) {
                    if (e instanceof InvalidKeyException) {
                        throw ((InvalidKeyException) e);
                    } else if (e instanceof InvalidAlgorithmParameterException) {
                        throw ((InvalidAlgorithmParameterException) e);
                    } else {
                        throw new ProviderException("Unexpected exception type", e);
                    }
                } else if (this.mOperationToken == null) {
                    throw new ProviderException("Keystore returned null operation token");
                } else if (this.mOperationHandle != 0) {
                    loadAlgorithmSpecificParametersFromBeginResult(opResult.outParams);
                    this.mMainDataStreamer = createMainDataStreamer(this.mKeyStore, opResult.token);
                    this.mAdditionalAuthenticationDataStreamer = createAdditionalAuthenticationDataStreamer(this.mKeyStore, opResult.token);
                    this.mAdditionalAuthenticationDataStreamerClosed = false;
                    return;
                } else {
                    throw new ProviderException("Keystore returned invalid operation handle");
                }
            }
            throw new ProviderException("Failed to communicate with keystore service");
        }
        throw new IllegalStateException("Not initialized");
    }

    protected void loadAlgorithmSpecificParametersFromBeginResult(@NonNull HwKeymasterArguments keymasterArgs) {
    }

    @NonNull
    protected HwUniversalKeyStoreCryptoOperationStreamer createMainDataStreamer(HwKeystoreManager keyStore, IBinder operationToken) {
        return new HwUniversalKeyStoreCryptoOperationStreamer(new MainDataStream(keyStore, operationToken));
    }

    @Nullable
    protected HwUniversalKeyStoreCryptoOperationStreamer createAdditionalAuthenticationDataStreamer(HwKeystoreManager keyStore, IBinder operationToken) {
        return null;
    }

    protected boolean adjustConfigForEncryptingWithPrivateKey() {
        return false;
    }

    protected final void initKey(int opmode, Key key) throws InvalidKeyException {
        StringBuilder stringBuilder;
        if (key == null) {
            throw new InvalidKeyException("Unsupported key: null");
        } else if (HwKeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            HwUniversalKeyStoreKey keystoreKey;
            if (key instanceof HwUniversalKeyStorePrivateKey) {
                keystoreKey = (HwUniversalKeyStoreKey) key;
            } else if (key instanceof HwUniversalKeyStorePublicKey) {
                keystoreKey = (HwUniversalKeyStoreKey) key;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported key type: ");
                stringBuilder.append(key);
                throw new InvalidKeyException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2;
            if (keystoreKey instanceof PrivateKey) {
                switch (opmode) {
                    case 1:
                    case 3:
                        if (!adjustConfigForEncryptingWithPrivateKey()) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RSA private keys cannot be used with ");
                            stringBuilder2.append(opmodeToString(opmode));
                            stringBuilder2.append(" and padding ");
                            stringBuilder2.append(EncryptionPadding.fromKeymaster(this.mKeymasterPadding));
                            stringBuilder2.append(". Only RSA public keys supported for this mode");
                            throw new InvalidKeyException(stringBuilder2.toString());
                        }
                        break;
                    case 2:
                    case 4:
                        break;
                    default:
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("RSA private keys cannot be used with opmode: ");
                        stringBuilder2.append(opmode);
                        throw new InvalidKeyException(stringBuilder2.toString());
                }
            }
            switch (opmode) {
                case 1:
                case 3:
                    break;
                case 2:
                case 4:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RSA public keys cannot be used with ");
                    stringBuilder2.append(opmodeToString(opmode));
                    stringBuilder2.append(" and padding ");
                    stringBuilder2.append(EncryptionPadding.fromKeymaster(this.mKeymasterPadding));
                    stringBuilder2.append(". Only RSA private keys supported for this opmode.");
                    throw new InvalidKeyException(stringBuilder2.toString());
                default:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RSA public keys cannot be used with ");
                    stringBuilder2.append(opmodeToString(opmode));
                    throw new InvalidKeyException(stringBuilder2.toString());
            }
            HwKeyCharacteristics keyCharacteristics = new HwKeyCharacteristics();
            int errorCode = getKeyStore().getKeyCharacteristics(keystoreKey.getAlias(), null, null, keystoreKey.getUid(), keyCharacteristics);
            if (errorCode == 1) {
                long keySizeBits = keyCharacteristics.getUnsignedInt(HwKeymasterDefs.KM_TAG_KEY_SIZE, -1);
                if (keySizeBits == -1) {
                    throw new InvalidKeyException("Size of key not known");
                } else if (keySizeBits <= 2147483647L) {
                    this.mModulusSizeBytes = (int) ((7 + keySizeBits) / 8);
                    setKey(keystoreKey);
                    return;
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Key too large: ");
                    stringBuilder3.append(keySizeBits);
                    stringBuilder3.append(" bits");
                    throw new InvalidKeyException(stringBuilder3.toString());
                }
            }
            throw getKeyStore().getInvalidKeyException(keystoreKey.getAlias(), keystoreKey.getUid(), errorCode);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key algorithm: ");
            stringBuilder.append(key.getAlgorithm());
            stringBuilder.append(". Only ");
            stringBuilder.append(HwKeyProperties.KEY_ALGORITHM_RSA);
            stringBuilder.append(" supported");
            throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    private void init(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        switch (opmode) {
            case 1:
            case 3:
                this.mEncrypting = true;
                break;
            case 2:
            case 4:
                this.mEncrypting = false;
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported opmode: ");
                stringBuilder.append(opmode);
                throw new InvalidParameterException(stringBuilder.toString());
        }
        initKey(opmode, key);
        if (this.mKey != null) {
            this.mRng = random;
            return;
        }
        throw new ProviderException("initKey did not initialize the key");
    }

    protected void initAlgorithmSpecificParameters() throws InvalidKeyException {
    }

    protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
    }

    protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params) throws InvalidAlgorithmParameterException {
    }

    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        resetAll();
        try {
            init(opmode, key, random);
            initAlgorithmSpecificParameters();
            ensureKeystoreOperationInitialized();
            if (!true) {
                resetAll();
            }
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e);
        } catch (Throwable th) {
            if (!false) {
                resetAll();
            }
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        resetAll();
        boolean success = false;
        try {
            init(opmode, key, random);
            initAlgorithmSpecificParameters(params);
            ensureKeystoreOperationInitialized();
            success = true;
        } finally {
            if (!success) {
                resetAll();
            }
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        resetAll();
        boolean success = false;
        try {
            init(opmode, key, random);
            initAlgorithmSpecificParameters(params);
            ensureKeystoreOperationInitialized();
            success = true;
        } finally {
            if (!success) {
                resetAll();
            }
        }
    }

    protected final byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] output = new byte[0];
        if (this.mCachedException != null) {
            return output;
        }
        try {
            ensureKeystoreOperationInitialized();
            if (inputLen == 0) {
                return output;
            }
            try {
                flushAAD();
                return this.mMainDataStreamer.update(input, inputOffset, inputLen);
            } catch (HwUniversalKeyStoreException e) {
                this.mCachedException = e;
                return new byte[0];
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
            this.mCachedException = e2;
            return output;
        }
    }

    private void flushAAD() throws HwUniversalKeyStoreException {
        if (this.mAdditionalAuthenticationDataStreamer != null && !this.mAdditionalAuthenticationDataStreamerClosed) {
            try {
                byte[] output = this.mAdditionalAuthenticationDataStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, null);
                if (output != null && output.length > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AAD update unexpectedly returned data: ");
                    stringBuilder.append(output.length);
                    stringBuilder.append(" bytes");
                    throw new ProviderException(stringBuilder.toString());
                }
            } finally {
                this.mAdditionalAuthenticationDataStreamerClosed = true;
            }
        }
    }

    protected final int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        byte[] outputCopy = engineUpdate(input, inputOffset, inputLen);
        if (outputCopy == null || outputCopy.length == 0) {
            return 0;
        }
        int outputAvailable = output.length - outputOffset;
        if (outputCopy.length <= outputAvailable) {
            System.arraycopy(outputCopy, 0, output, outputOffset, outputCopy.length);
            return outputCopy.length;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Output buffer too short. Produced: ");
        stringBuilder.append(outputCopy.length);
        stringBuilder.append(", available: ");
        stringBuilder.append(outputAvailable);
        throw new ShortBufferException(stringBuilder.toString());
    }

    protected final void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                if (this.mAdditionalAuthenticationDataStreamerClosed) {
                    throw new IllegalStateException("AAD can only be provided before Cipher.update is invoked");
                } else if (this.mAdditionalAuthenticationDataStreamer != null) {
                    try {
                        byte[] output = this.mAdditionalAuthenticationDataStreamer.update(input, inputOffset, inputLen);
                        if (output != null && output.length > 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("AAD update unexpectedly produced output: ");
                            stringBuilder.append(output.length);
                            stringBuilder.append(" bytes");
                            throw new ProviderException(stringBuilder.toString());
                        }
                    } catch (HwUniversalKeyStoreException e) {
                        this.mCachedException = e;
                    }
                } else {
                    throw new IllegalStateException("This cipher does not support AAD");
                }
            } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
                this.mCachedException = e2;
            }
        }
    }

    protected final int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        if (input == null) {
            throw new NullPointerException("input == null");
        } else if (output != null) {
            byte[] outputArray;
            int inputSize = input.remaining();
            int outputSize = 0;
            if (input.hasArray()) {
                outputArray = engineUpdate(input.array(), input.arrayOffset() + input.position(), inputSize);
                input.position(input.position() + inputSize);
            } else {
                outputArray = new byte[inputSize];
                input.get(outputArray);
                outputArray = engineUpdate(outputArray, 0, inputSize);
            }
            if (outputArray != null && outputArray.length > 0) {
                outputSize = outputArray.length;
            }
            if (outputSize > 0) {
                int outputBufferAvailable = output.remaining();
                try {
                    output.put(outputArray);
                } catch (BufferOverflowException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Output buffer too small. Produced: ");
                    stringBuilder.append(outputSize);
                    stringBuilder.append(", available: ");
                    stringBuilder.append(outputBufferAvailable);
                    throw new ShortBufferException(stringBuilder.toString());
                }
            }
            return outputSize;
        } else {
            throw new NullPointerException("output == null");
        }
    }

    protected final void engineUpdateAAD(ByteBuffer src) {
        if (src == null) {
            throw new IllegalArgumentException("src == null");
        } else if (src.hasRemaining()) {
            byte[] input;
            int inputOffset;
            int inputLen;
            if (src.hasArray()) {
                input = src.array();
                inputOffset = src.arrayOffset() + src.position();
                inputLen = src.remaining();
                src.position(src.limit());
            } else {
                input = new byte[src.remaining()];
                inputOffset = 0;
                inputLen = input.length;
                src.get(input);
            }
            engineUpdateAAD(input, inputOffset, inputLen);
        }
    }

    protected final byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                try {
                    flushAAD();
                    byte[] output = this.mMainDataStreamer.doFinal(input, inputOffset, inputLen, null, HwUniversalKeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForFinish()));
                    resetWhilePreservingInitState();
                    return output;
                } catch (HwUniversalKeyStoreException e) {
                    int errorCode = e.getErrorCode();
                    if (errorCode == -38) {
                        throw ((BadPaddingException) new BadPaddingException().initCause(e));
                    } else if (errorCode == -30) {
                        throw ((AEADBadTagException) new AEADBadTagException().initCause(e));
                    } else if (errorCode != -21) {
                        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                    } else {
                        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                    }
                }
            } catch (InvalidAlgorithmParameterException | InvalidKeyException e2) {
                throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e2));
            }
        }
        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(this.mCachedException));
    }

    protected int getAdditionalEntropyAmountForFinish() {
        return 0;
    }

    protected final int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        byte[] outputCopy = engineDoFinal(input, inputOffset, inputLen);
        if (outputCopy == null) {
            return 0;
        }
        int outputAvailable = output.length - outputOffset;
        if (outputCopy.length <= outputAvailable) {
            System.arraycopy(outputCopy, 0, output, outputOffset, outputCopy.length);
            return outputCopy.length;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Output buffer too short. Produced: ");
        stringBuilder.append(outputCopy.length);
        stringBuilder.append(", available: ");
        stringBuilder.append(outputAvailable);
        throw new ShortBufferException(stringBuilder.toString());
    }

    protected final int engineDoFinal(ByteBuffer input, ByteBuffer output) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        if (input == null) {
            throw new NullPointerException("input == null");
        } else if (output != null) {
            byte[] outputArray;
            int inputSize = input.remaining();
            int outputSize = 0;
            if (input.hasArray()) {
                outputArray = engineDoFinal(input.array(), input.arrayOffset() + input.position(), inputSize);
                input.position(input.position() + inputSize);
            } else {
                outputArray = new byte[inputSize];
                input.get(outputArray);
                outputArray = engineDoFinal(outputArray, 0, inputSize);
            }
            if (outputArray != null) {
                outputSize = outputArray.length;
            }
            if (outputSize > 0) {
                int outputBufferAvailable = output.remaining();
                try {
                    output.put(outputArray);
                } catch (BufferOverflowException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Output buffer too small. Produced: ");
                    stringBuilder.append(outputSize);
                    stringBuilder.append(", available: ");
                    stringBuilder.append(outputBufferAvailable);
                    throw new ShortBufferException(stringBuilder.toString());
                }
            }
            return outputSize;
        } else {
            throw new NullPointerException("output == null");
        }
    }

    protected final byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        } else if (!isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        } else if (key != null) {
            byte[] encoded = null;
            if (key instanceof SecretKey) {
                if ("RAW".equalsIgnoreCase(key.getFormat())) {
                    encoded = key.getEncoded();
                }
                if (encoded == null) {
                    try {
                        encoded = ((SecretKeySpec) SecretKeyFactory.getInstance(key.getAlgorithm()).getKeySpec((SecretKey) key, SecretKeySpec.class)).getEncoded();
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e);
                    }
                }
            } else if (key instanceof PrivateKey) {
                if ("PKCS8".equalsIgnoreCase(key.getFormat())) {
                    encoded = key.getEncoded();
                }
                if (encoded == null) {
                    try {
                        encoded = ((PKCS8EncodedKeySpec) KeyFactory.getInstance(key.getAlgorithm()).getKeySpec(key, PKCS8EncodedKeySpec.class)).getEncoded();
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e2) {
                        throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e2);
                    }
                }
            } else if (key instanceof PublicKey) {
                if ("X.509".equalsIgnoreCase(key.getFormat())) {
                    encoded = key.getEncoded();
                }
                if (encoded == null) {
                    try {
                        encoded = ((X509EncodedKeySpec) KeyFactory.getInstance(key.getAlgorithm()).getKeySpec(key, X509EncodedKeySpec.class)).getEncoded();
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e22) {
                        throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e22);
                    }
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported key type: ");
                stringBuilder.append(key.getClass().getName());
                throw new InvalidKeyException(stringBuilder.toString());
            }
            if (encoded != null) {
                try {
                    return engineDoFinal(encoded, 0, encoded.length);
                } catch (BadPaddingException e3) {
                    throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e3));
                }
            }
            throw new InvalidKeyException("Failed to wrap key because it does not export its key material");
        } else {
            throw new NullPointerException("key == null");
        }
    }

    protected final Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException, NoSuchAlgorithmException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        } else if (isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        } else if (wrappedKey != null) {
            try {
                byte[] encoded = engineDoFinal(wrappedKey, null, wrappedKey.length);
                switch (wrappedKeyType) {
                    case 1:
                        try {
                            return KeyFactory.getInstance(wrappedKeyAlgorithm).generatePublic(new X509EncodedKeySpec(encoded));
                        } catch (InvalidKeySpecException e) {
                            throw new InvalidKeyException("Failed to create public key from its X.509 encoded form", e);
                        }
                    case 2:
                        try {
                            return KeyFactory.getInstance(wrappedKeyAlgorithm).generatePrivate(new PKCS8EncodedKeySpec(encoded));
                        } catch (InvalidKeySpecException e2) {
                            throw new InvalidKeyException("Failed to create private key from its PKCS#8 encoded form", e2);
                        }
                    case 3:
                        return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported wrappedKeyType: ");
                        stringBuilder.append(wrappedKeyType);
                        throw new InvalidParameterException(stringBuilder.toString());
                }
            } catch (BadPaddingException | IllegalBlockSizeException e3) {
                throw new InvalidKeyException("Failed to unwrap key", e3);
            }
        } else {
            throw new NullPointerException("wrappedKey == null");
        }
    }

    protected final void setKeymasterPurposeOverride(int keymasterPurpose) {
        this.mKeymasterPurposeOverride = keymasterPurpose;
    }

    protected final int getKeymasterPurposeOverride() {
        return this.mKeymasterPurposeOverride;
    }

    public final int getKeymasterPaddingOverride() {
        return this.mKeymasterPaddingOverride;
    }

    public final void setKeymasterPaddingOverride(int mKeymasterPaddingOverride) {
        this.mKeymasterPaddingOverride = mKeymasterPaddingOverride;
    }

    protected final boolean isEncrypting() {
        return this.mEncrypting;
    }

    public final long getOperationHandle() {
        return this.mOperationHandle;
    }

    protected final void setKey(@NonNull HwUniversalKeyStoreKey key) {
        this.mKey = key;
    }

    protected final HwUniversalKeyStoreKey getKey() {
        return this.mKey;
    }

    @NonNull
    protected final HwKeystoreManager getKeyStore() {
        return this.mKeyStore;
    }

    protected final long getConsumedInputSizeBytes() {
        if (this.mMainDataStreamer != null) {
            return this.mMainDataStreamer.getConsumedInputSizeBytes();
        }
        throw new IllegalStateException("Not initialized");
    }

    protected final long getProducedOutputSizeBytes() {
        if (this.mMainDataStreamer != null) {
            return this.mMainDataStreamer.getProducedOutputSizeBytes();
        }
        throw new IllegalStateException("Not initialized");
    }

    @CallSuper
    protected void finalize() throws Throwable {
        try {
            IBinder operationToken = this.mOperationToken;
            if (operationToken != null) {
                this.mKeyStore.abort(operationToken);
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    static String opmodeToString(int opmode) {
        switch (opmode) {
            case 1:
                return "ENCRYPT_MODE";
            case 2:
                return "DECRYPT_MODE";
            case 3:
                return "WRAP_MODE";
            case 4:
                return "UNWRAP_MODE";
            default:
                return String.valueOf(opmode);
        }
    }
}
