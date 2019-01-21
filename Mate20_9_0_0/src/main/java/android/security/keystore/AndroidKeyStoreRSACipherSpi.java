package android.security.keystore;

import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties.Digest;
import android.security.keystore.KeyProperties.EncryptionPadding;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.PSource.PSpecified;

abstract class AndroidKeyStoreRSACipherSpi extends AndroidKeyStoreCipherSpiBase {
    private final int mKeymasterPadding;
    private int mKeymasterPaddingOverride;
    private int mModulusSizeBytes = -1;

    public static final class NoPadding extends AndroidKeyStoreRSACipherSpi {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

        public NoPadding() {
            super(1);
        }

        protected boolean adjustConfigForEncryptingWithPrivateKey() {
            setKeymasterPurposeOverride(2);
            return true;
        }

        protected void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected parameters: ");
                stringBuilder.append(params);
                stringBuilder.append(". No parameters supported");
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }

        protected void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
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
            return 0;
        }
    }

    static abstract class OAEPWithMGF1Padding extends AndroidKeyStoreRSACipherSpi {
        private static final String MGF_ALGORITGM_MGF1 = "MGF1";
        private int mDigestOutputSizeBytes;
        private int mKeymasterDigest = -1;

        OAEPWithMGF1Padding(int keymasterDigest) {
            super(2);
            this.mKeymasterDigest = keymasterDigest;
            this.mDigestOutputSizeBytes = (KeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
        }

        protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {
        }

        protected final void initAlgorithmSpecificParameters(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
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
                                        if (KeyProperties.DIGEST_SHA1.equalsIgnoreCase(mgf1JcaDigest)) {
                                            PSource pSource = spec.getPSource();
                                            if (pSource instanceof PSpecified) {
                                                byte[] pSourceValue = ((PSpecified) pSource).getValue();
                                                if (pSourceValue == null || pSourceValue.length <= 0) {
                                                    this.mKeymasterDigest = keymasterDigest;
                                                    this.mDigestOutputSizeBytes = (KeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
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
                                        stringBuilder4.append(KeyProperties.DIGEST_SHA1);
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

        protected final void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
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

        protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArgs) {
            super.addAlgorithmSpecificParametersToBegin(keymasterArgs);
            keymasterArgs.addEnum(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
        }

        protected final void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArgs) {
            super.loadAlgorithmSpecificParametersFromBeginResult(keymasterArgs);
        }

        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        protected final int getAdditionalEntropyAmountForFinish() {
            return isEncrypting() ? this.mDigestOutputSizeBytes : 0;
        }
    }

    public static final class PKCS1Padding extends AndroidKeyStoreRSACipherSpi {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

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

        protected void initAlgorithmSpecificParameters(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected parameters: ");
                stringBuilder.append(params);
                stringBuilder.append(". No parameters supported");
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }

        protected void initAlgorithmSpecificParameters(AlgorithmParameters params) throws InvalidAlgorithmParameterException {
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

    public static class OAEPWithSHA1AndMGF1Padding extends OAEPWithMGF1Padding {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA1AndMGF1Padding() {
            super(2);
        }
    }

    public static class OAEPWithSHA224AndMGF1Padding extends OAEPWithMGF1Padding {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA224AndMGF1Padding() {
            super(3);
        }
    }

    public static class OAEPWithSHA256AndMGF1Padding extends OAEPWithMGF1Padding {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA256AndMGF1Padding() {
            super(4);
        }
    }

    public static class OAEPWithSHA384AndMGF1Padding extends OAEPWithMGF1Padding {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA384AndMGF1Padding() {
            super(5);
        }
    }

    public static class OAEPWithSHA512AndMGF1Padding extends OAEPWithMGF1Padding {
        public /* bridge */ /* synthetic */ void finalize() throws Throwable {
            super.finalize();
        }

        public OAEPWithSHA512AndMGF1Padding() {
            super(6);
        }
    }

    AndroidKeyStoreRSACipherSpi(int keymasterPadding) {
        this.mKeymasterPadding = keymasterPadding;
    }

    protected final void initKey(int opmode, Key key) throws InvalidKeyException {
        StringBuilder stringBuilder;
        if (key == null) {
            throw new InvalidKeyException("Unsupported key: null");
        } else if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            AndroidKeyStoreKey keystoreKey;
            if (key instanceof AndroidKeyStorePrivateKey) {
                keystoreKey = (AndroidKeyStoreKey) key;
            } else if (key instanceof AndroidKeyStorePublicKey) {
                keystoreKey = (AndroidKeyStoreKey) key;
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
                            stringBuilder2.append(AndroidKeyStoreCipherSpiBase.opmodeToString(opmode));
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
                    stringBuilder2.append(AndroidKeyStoreCipherSpiBase.opmodeToString(opmode));
                    stringBuilder2.append(" and padding ");
                    stringBuilder2.append(EncryptionPadding.fromKeymaster(this.mKeymasterPadding));
                    stringBuilder2.append(". Only RSA private keys supported for this opmode.");
                    throw new InvalidKeyException(stringBuilder2.toString());
                default:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RSA public keys cannot be used with ");
                    stringBuilder2.append(AndroidKeyStoreCipherSpiBase.opmodeToString(opmode));
                    throw new InvalidKeyException(stringBuilder2.toString());
            }
            KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
            int errorCode = getKeyStore().getKeyCharacteristics(keystoreKey.getAlias(), null, null, keystoreKey.getUid(), keyCharacteristics);
            if (errorCode == 1) {
                long keySizeBits = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, -1);
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
            stringBuilder.append(KeyProperties.KEY_ALGORITHM_RSA);
            stringBuilder.append(" supported");
            throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    protected boolean adjustConfigForEncryptingWithPrivateKey() {
        return false;
    }

    protected final void resetAll() {
        this.mModulusSizeBytes = -1;
        this.mKeymasterPaddingOverride = -1;
        super.resetAll();
    }

    protected final void resetWhilePreservingInitState() {
        super.resetWhilePreservingInitState();
    }

    protected void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArgs) {
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 1);
        int keymasterPadding = getKeymasterPaddingOverride();
        if (keymasterPadding == -1) {
            keymasterPadding = this.mKeymasterPadding;
        }
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_PADDING, keymasterPadding);
        int purposeOverride = getKeymasterPurposeOverride();
        if (purposeOverride == -1) {
            return;
        }
        if (purposeOverride == 2 || purposeOverride == 3) {
            keymasterArgs.addEnum(KeymasterDefs.KM_TAG_DIGEST, 0);
        }
    }

    protected void loadAlgorithmSpecificParametersFromBeginResult(KeymasterArguments keymasterArgs) {
    }

    protected final int engineGetBlockSize() {
        return 0;
    }

    protected final byte[] engineGetIV() {
        return null;
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

    protected final void setKeymasterPaddingOverride(int keymasterPadding) {
        this.mKeymasterPaddingOverride = keymasterPadding;
    }

    protected final int getKeymasterPaddingOverride() {
        return this.mKeymasterPaddingOverride;
    }
}
