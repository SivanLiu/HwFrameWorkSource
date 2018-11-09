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
                                                throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + pSource + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                                            }
                                            throw new InvalidAlgorithmParameterException("Unsupported source of encoding input P: " + pSource + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
                                        }
                                        throw new InvalidAlgorithmParameterException("Unsupported MGF1 digest: " + mgf1JcaDigest + ". Only " + HwKeyProperties.DIGEST_SHA1 + " supported");
                                    } else {
                                        throw new InvalidAlgorithmParameterException("Unsupported MGF parameters: " + mgfParams + ". Only MGF1ParameterSpec supported");
                                    }
                                default:
                                    throw new InvalidAlgorithmParameterException("Unsupported digest: " + jcaDigest);
                            }
                        } catch (IllegalArgumentException e) {
                            throw new InvalidAlgorithmParameterException("Unsupported digest: " + jcaDigest, e);
                        }
                    }
                    throw new InvalidAlgorithmParameterException("Unsupported MGF: " + spec.getMGFAlgorithm() + ". Only " + MGF_ALGORITGM_MGF1 + " supported");
                }
                throw new InvalidAlgorithmParameterException("Unsupported parameter spec: " + params + ". Only OAEPParameterSpec supported");
            }
        }

        protected final void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                try {
                    AlgorithmParameterSpec spec = (OAEPParameterSpec) params.getParameterSpec(OAEPParameterSpec.class);
                    if (spec == null) {
                        throw new InvalidAlgorithmParameterException("OAEP parameters required, but not provided in parameters: " + params);
                    }
                    initAlgorithmSpecificParameters(spec);
                } catch (InvalidParameterSpecException e) {
                    throw new InvalidAlgorithmParameterException("OAEP parameters required, but not found in parameters: " + params, e);
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
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                throw new InvalidAlgorithmParameterException("Unexpected parameters: " + params + ". No parameters supported");
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
        if (this.mKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        HwKeymasterArguments keymasterInputArgs = new HwKeymasterArguments();
        addAlgorithmSpecificParametersToBegin(keymasterInputArgs);
        byte[] additionalEntropy = HwUniversalKeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForBegin());
        int purpose = this.mKeymasterPurposeOverride != -1 ? this.mKeymasterPurposeOverride : this.mEncrypting ? 0 : 1;
        HwOperationResult opResult = this.mKeyStore.begin(this.mKey.getAlias(), purpose, true, keymasterInputArgs, additionalEntropy, this.mKey.getUid());
        if (opResult == null) {
            throw new ProviderException("Failed to communicate with keystore service");
        }
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
        } else if (this.mOperationHandle == 0) {
            throw new ProviderException("Keystore returned invalid operation handle");
        } else {
            loadAlgorithmSpecificParametersFromBeginResult(opResult.outParams);
            this.mMainDataStreamer = createMainDataStreamer(this.mKeyStore, opResult.token);
            this.mAdditionalAuthenticationDataStreamer = createAdditionalAuthenticationDataStreamer(this.mKeyStore, opResult.token);
            this.mAdditionalAuthenticationDataStreamerClosed = false;
        }
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
        if (key == null) {
            throw new InvalidKeyException("Unsupported key: null");
        } else if (HwKeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            HwUniversalKeyStoreKey keystoreKey;
            if (key instanceof HwUniversalKeyStorePrivateKey) {
                keystoreKey = (HwUniversalKeyStoreKey) key;
            } else if (key instanceof HwUniversalKeyStorePublicKey) {
                keystoreKey = (HwUniversalKeyStoreKey) key;
            } else {
                throw new InvalidKeyException("Unsupported key type: " + key);
            }
            if (keystoreKey instanceof PrivateKey) {
                switch (opmode) {
                    case 1:
                    case 3:
                        if (!adjustConfigForEncryptingWithPrivateKey()) {
                            throw new InvalidKeyException("RSA private keys cannot be used with " + opmodeToString(opmode) + " and padding " + EncryptionPadding.fromKeymaster(this.mKeymasterPadding) + ". Only RSA public keys supported for this mode");
                        }
                        break;
                    case 2:
                    case 4:
                        break;
                    default:
                        throw new InvalidKeyException("RSA private keys cannot be used with opmode: " + opmode);
                }
            }
            switch (opmode) {
                case 1:
                case 3:
                    break;
                case 2:
                case 4:
                    throw new InvalidKeyException("RSA public keys cannot be used with " + opmodeToString(opmode) + " and padding " + EncryptionPadding.fromKeymaster(this.mKeymasterPadding) + ". Only RSA private keys supported for this opmode.");
                default:
                    throw new InvalidKeyException("RSA public keys cannot be used with " + opmodeToString(opmode));
            }
            HwKeyCharacteristics keyCharacteristics = new HwKeyCharacteristics();
            int errorCode = getKeyStore().getKeyCharacteristics(keystoreKey.getAlias(), null, null, keystoreKey.getUid(), keyCharacteristics);
            if (errorCode != 1) {
                throw getKeyStore().getInvalidKeyException(keystoreKey.getAlias(), keystoreKey.getUid(), errorCode);
            }
            long keySizeBits = keyCharacteristics.getUnsignedInt(HwKeymasterDefs.KM_TAG_KEY_SIZE, -1);
            if (keySizeBits == -1) {
                throw new InvalidKeyException("Size of key not known");
            } else if (keySizeBits > 2147483647L) {
                throw new InvalidKeyException("Key too large: " + keySizeBits + " bits");
            } else {
                this.mModulusSizeBytes = (int) ((7 + keySizeBits) / 8);
                setKey(keystoreKey);
            }
        } else {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only " + HwKeyProperties.KEY_ALGORITHM_RSA + " supported");
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
                throw new InvalidParameterException("Unsupported opmode: " + opmode);
        }
        initKey(opmode, key);
        if (this.mKey == null) {
            throw new ProviderException("initKey did not initialize the key");
        }
        this.mRng = random;
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
        } catch (GeneralSecurityException e2) {
            this.mCachedException = e2;
            return output;
        }
    }

    private void flushAAD() throws HwUniversalKeyStoreException {
        if (this.mAdditionalAuthenticationDataStreamer != null && (this.mAdditionalAuthenticationDataStreamerClosed ^ 1) != 0) {
            try {
                byte[] output = this.mAdditionalAuthenticationDataStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, null);
                if (output != null && output.length > 0) {
                    throw new ProviderException("AAD update unexpectedly returned data: " + output.length + " bytes");
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
        if (outputCopy.length > outputAvailable) {
            throw new ShortBufferException("Output buffer too short. Produced: " + outputCopy.length + ", available: " + outputAvailable);
        }
        System.arraycopy(outputCopy, 0, output, outputOffset, outputCopy.length);
        return outputCopy.length;
    }

    protected final void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                if (this.mAdditionalAuthenticationDataStreamerClosed) {
                    throw new IllegalStateException("AAD can only be provided before Cipher.update is invoked");
                } else if (this.mAdditionalAuthenticationDataStreamer == null) {
                    throw new IllegalStateException("This cipher does not support AAD");
                } else {
                    try {
                        byte[] output = this.mAdditionalAuthenticationDataStreamer.update(input, inputOffset, inputLen);
                        if (output != null && output.length > 0) {
                            throw new ProviderException("AAD update unexpectedly produced output: " + output.length + " bytes");
                        }
                    } catch (HwUniversalKeyStoreException e) {
                        this.mCachedException = e;
                    }
                }
            } catch (GeneralSecurityException e2) {
                this.mCachedException = e2;
            }
        }
    }

    protected final int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        if (input == null) {
            throw new NullPointerException("input == null");
        } else if (output == null) {
            throw new NullPointerException("output == null");
        } else {
            byte[] outputArray;
            int inputSize = input.remaining();
            if (input.hasArray()) {
                outputArray = engineUpdate(input.array(), input.arrayOffset() + input.position(), inputSize);
                input.position(input.position() + inputSize);
            } else {
                byte[] inputArray = new byte[inputSize];
                input.get(inputArray);
                outputArray = engineUpdate(inputArray, 0, inputSize);
            }
            int outputSize = (outputArray == null || outputArray.length <= 0) ? 0 : outputArray.length;
            if (outputSize > 0) {
                int outputBufferAvailable = output.remaining();
                try {
                    output.put(outputArray);
                } catch (BufferOverflowException e) {
                    throw new ShortBufferException("Output buffer too small. Produced: " + outputSize + ", available: " + outputBufferAvailable);
                }
            }
            return outputSize;
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
        if (this.mCachedException != null) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(this.mCachedException));
        }
        try {
            ensureKeystoreOperationInitialized();
            try {
                flushAAD();
                byte[] output = this.mMainDataStreamer.doFinal(input, inputOffset, inputLen, null, HwUniversalKeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.mRng, getAdditionalEntropyAmountForFinish()));
                resetWhilePreservingInitState();
                return output;
            } catch (HwUniversalKeyStoreException e) {
                switch (e.getErrorCode()) {
                    case HwKeymasterDefs.KM_ERROR_INVALID_ARGUMENT /*-38*/:
                        throw ((BadPaddingException) new BadPaddingException().initCause(e));
                    case HwKeymasterDefs.KM_ERROR_VERIFICATION_FAILED /*-30*/:
                        throw ((AEADBadTagException) new AEADBadTagException().initCause(e));
                    case HwKeymasterDefs.KM_ERROR_INVALID_INPUT_LENGTH /*-21*/:
                        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                    default:
                        throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e));
                }
            }
        } catch (GeneralSecurityException e2) {
            throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e2));
        }
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
        if (outputCopy.length > outputAvailable) {
            throw new ShortBufferException("Output buffer too short. Produced: " + outputCopy.length + ", available: " + outputAvailable);
        }
        System.arraycopy(outputCopy, 0, output, outputOffset, outputCopy.length);
        return outputCopy.length;
    }

    protected final int engineDoFinal(ByteBuffer input, ByteBuffer output) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        if (input == null) {
            throw new NullPointerException("input == null");
        } else if (output == null) {
            throw new NullPointerException("output == null");
        } else {
            byte[] outputArray;
            int inputSize = input.remaining();
            if (input.hasArray()) {
                outputArray = engineDoFinal(input.array(), input.arrayOffset() + input.position(), inputSize);
                input.position(input.position() + inputSize);
            } else {
                byte[] inputArray = new byte[inputSize];
                input.get(inputArray);
                outputArray = engineDoFinal(inputArray, 0, inputSize);
            }
            int outputSize = outputArray != null ? outputArray.length : 0;
            if (outputSize > 0) {
                int outputBufferAvailable = output.remaining();
                try {
                    output.put(outputArray);
                } catch (BufferOverflowException e) {
                    throw new ShortBufferException("Output buffer too small. Produced: " + outputSize + ", available: " + outputBufferAvailable);
                }
            }
            return outputSize;
        }
    }

    protected final byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        } else if (!isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        } else if (key == null) {
            throw new NullPointerException("key == null");
        } else {
            byte[] encoded = null;
            if (key instanceof SecretKey) {
                if ("RAW".equalsIgnoreCase(key.getFormat())) {
                    encoded = key.getEncoded();
                }
                if (encoded == null) {
                    try {
                        encoded = ((SecretKeySpec) SecretKeyFactory.getInstance(key.getAlgorithm()).getKeySpec((SecretKey) key, SecretKeySpec.class)).getEncoded();
                    } catch (GeneralSecurityException e) {
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
                    } catch (GeneralSecurityException e2) {
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
                    } catch (GeneralSecurityException e22) {
                        throw new InvalidKeyException("Failed to wrap key because it does not export its key material", e22);
                    }
                }
            } else {
                throw new InvalidKeyException("Unsupported key type: " + key.getClass().getName());
            }
            if (encoded == null) {
                throw new InvalidKeyException("Failed to wrap key because it does not export its key material");
            }
            try {
                return engineDoFinal(encoded, 0, encoded.length);
            } catch (BadPaddingException e3) {
                throw ((IllegalBlockSizeException) new IllegalBlockSizeException().initCause(e3));
            }
        }
    }

    protected final Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException, NoSuchAlgorithmException {
        if (this.mKey == null) {
            throw new IllegalStateException("Not initilized");
        } else if (isEncrypting()) {
            throw new IllegalStateException("Cipher must be initialized in Cipher.WRAP_MODE to wrap keys");
        } else if (wrappedKey == null) {
            throw new NullPointerException("wrappedKey == null");
        } else {
            try {
                byte[] encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
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
                        throw new InvalidParameterException("Unsupported wrappedKeyType: " + wrappedKeyType);
                }
            } catch (GeneralSecurityException e3) {
                throw new InvalidKeyException("Failed to unwrap key", e3);
            }
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
