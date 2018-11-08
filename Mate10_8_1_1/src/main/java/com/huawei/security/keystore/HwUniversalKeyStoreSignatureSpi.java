package com.huawei.security.keystore;

import android.os.IBinder;
import android.security.keystore.KeyStoreCryptoOperation;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.Log;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.keymaster.HwKeymasterArguments;
import com.huawei.security.keymaster.HwKeymasterDefs;
import com.huawei.security.keymaster.HwOperationResult;
import com.huawei.security.keystore.ArrayUtils.EmptyArray;
import com.huawei.security.keystore.HwUniversalKeyStoreCryptoOperationStreamer.MainDataStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;

public class HwUniversalKeyStoreSignatureSpi extends SignatureSpi implements KeyStoreCryptoOperation {
    public static final String TAG = "HwKeyStoreSignature";
    private Exception mCachedException;
    private HwUniversalKeyStoreKey mKey;
    private final HwKeystoreManager mKeyStore = HwKeystoreManager.getInstance();
    private int mKeymasterDigest = 4;
    private int mKeymasterPadding = 5;
    private HwUniversalKeyStoreCryptoOperationStreamer mMessageStreamer;
    private long mOperationHandle;
    private IBinder mOperationToken;
    private boolean mSigning;

    static class PKCS1Padding extends HwUniversalKeyStoreSignatureSpi {
        PKCS1Padding(int keymasterDigest) {
            super(keymasterDigest, 5);
        }

        protected final int getAdditionalEntropyAmountForSign() {
            return 0;
        }
    }

    public static final class NONEWithPKCS1Padding extends PKCS1Padding {
        public NONEWithPKCS1Padding() {
            super(0);
        }
    }

    static class PSSPadding extends HwUniversalKeyStoreSignatureSpi {
        private static final int SALT_LENGTH_BYTES = 20;

        PSSPadding(int keymasterDigest) {
            super(keymasterDigest, 3);
        }

        protected final int getAdditionalEntropyAmountForSign() {
            return SALT_LENGTH_BYTES;
        }
    }

    public static final class SHA256WithPKCS1Padding extends PKCS1Padding {
        public SHA256WithPKCS1Padding() {
            super(4);
        }
    }

    public static final class SHA256WithPSSPadding extends PSSPadding {
        public SHA256WithPSSPadding() {
            super(4);
        }
    }

    public static final class SHA384WithPKCS1Padding extends PKCS1Padding {
        public SHA384WithPKCS1Padding() {
            super(5);
        }
    }

    public static final class SHA384WithPSSPadding extends PSSPadding {
        public SHA384WithPSSPadding() {
            super(5);
        }
    }

    public static final class SHA512WithPKCS1Padding extends PKCS1Padding {
        public SHA512WithPKCS1Padding() {
            super(6);
        }
    }

    public static final class SHA512WithPSSPadding extends PSSPadding {
        public SHA512WithPSSPadding() {
            super(6);
        }
    }

    HwUniversalKeyStoreSignatureSpi(int keymasterDigest, int keymasterPadding) {
        this.mKeymasterDigest = keymasterDigest;
        this.mKeymasterPadding = keymasterPadding;
    }

    protected int getAdditionalEntropyAmountForSign() {
        return 0;
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        resetAll();
        Log.e(TAG, "engineInitVerify publicKey:" + publicKey.getAlgorithm());
        if (publicKey == null) {
            try {
                throw new InvalidKeyException("Unsupported key: null");
            } catch (Throwable th) {
                if (!false) {
                    resetAll();
                }
            }
        } else if (publicKey instanceof HwUniversalKeyStorePublicKey) {
            HwUniversalKeyStoreKey keystoreKey = (HwUniversalKeyStorePublicKey) publicKey;
            this.mSigning = false;
            initKey(keystoreKey);
            this.appRandom = null;
            ensureKeystoreOperationInitialized();
            if (!true) {
                resetAll();
            }
        } else {
            throw new InvalidKeyException("Unsupported public key type: " + publicKey);
        }
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    protected void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
        resetAll();
        if (privateKey == null) {
            try {
                throw new InvalidKeyException("Unsupported key: null");
            } catch (Throwable th) {
                if (!false) {
                    resetAll();
                }
            }
        } else if (privateKey instanceof HwUniversalKeyStorePrivateKey) {
            HwUniversalKeyStoreKey keystoreKey = (HwUniversalKeyStoreKey) privateKey;
            this.mSigning = true;
            initKey(keystoreKey);
            this.appRandom = random;
            ensureKeystoreOperationInitialized();
            if (!true) {
                resetAll();
            }
        } else {
            throw new InvalidKeyException("Unsupported private key type: " + privateKey);
        }
    }

    protected void engineUpdate(byte b) throws SignatureException {
        engineUpdate(new byte[]{b}, 0, 1);
    }

    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        if (this.mCachedException != null) {
            throw new SignatureException(this.mCachedException);
        }
        try {
            ensureKeystoreOperationInitialized();
            if (len == 0) {
                Log.e(TAG, "engineUpdate len is 0");
                return;
            }
            try {
                byte[] output = this.mMessageStreamer.update(b, off, len);
                if (output.length != 0) {
                    throw new ProviderException("Update operation unexpectedly produced output: " + output.length + " bytes");
                }
            } catch (HwUniversalKeyStoreException e) {
                throw new SignatureException(e);
            }
        } catch (InvalidKeyException e2) {
            throw new SignatureException(e2);
        }
    }

    protected void engineUpdate(ByteBuffer input) {
        byte[] b;
        int off;
        int len = input.remaining();
        if (input.hasArray()) {
            b = input.array();
            off = input.arrayOffset() + input.position();
            input.position(input.limit());
        } else {
            b = new byte[len];
            off = 0;
            input.get(b);
        }
        try {
            engineUpdate(b, off, len);
        } catch (SignatureException e) {
            this.mCachedException = e;
        }
    }

    protected byte[] engineSign() throws SignatureException {
        if (this.mCachedException != null) {
            throw new SignatureException(this.mCachedException);
        }
        try {
            ensureKeystoreOperationInitialized();
            byte[] signature = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, HwUniversalKeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.appRandom, getAdditionalEntropyAmountForSign()));
            resetWhilePreservingInitState();
            return signature;
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }

    protected int engineSign(byte[] outbuf, int offset, int len) throws SignatureException {
        return super.engineSign(outbuf, offset, len);
    }

    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (this.mCachedException != null) {
            throw new SignatureException(this.mCachedException);
        }
        try {
            ensureKeystoreOperationInitialized();
            boolean verified;
            try {
                byte[] output = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, sigBytes, null);
                if (output.length != 0) {
                    throw new ProviderException("Signature verification unexpected produced output: " + output.length + " bytes");
                }
                verified = true;
                resetWhilePreservingInitState();
                return verified;
            } catch (HwUniversalKeyStoreException e) {
                switch (e.getErrorCode()) {
                    case HwKeymasterDefs.KM_ERROR_VERIFICATION_FAILED /*-30*/:
                        verified = false;
                        break;
                    default:
                        throw new SignatureException(e);
                }
            }
        } catch (InvalidKeyException e2) {
            throw new SignatureException(e2);
        }
    }

    protected boolean engineVerify(byte[] sigBytes, int offset, int length) throws SignatureException {
        return engineVerify(ArrayUtils.subarray(sigBytes, offset, length));
    }

    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    protected void engineSetParameter(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
        super.engineSetParameter(params);
    }

    protected AlgorithmParameters engineGetParameters() {
        return super.engineGetParameters();
    }

    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @CallSuper
    protected void initKey(HwUniversalKeyStoreKey key) throws InvalidKeyException {
        if (HwKeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            this.mKey = key;
            return;
        }
        throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only" + HwKeyProperties.KEY_ALGORITHM_RSA + " supported");
    }

    @CallSuper
    protected void resetAll() {
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mOperationToken = null;
            this.mKeyStore.abort(operationToken);
        }
        this.mSigning = false;
        this.mKey = null;
        this.appRandom = null;
        this.mMessageStreamer = null;
        this.mOperationToken = null;
        this.mOperationHandle = 0;
        this.mCachedException = null;
    }

    @CallSuper
    protected void resetWhilePreservingInitState() {
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mOperationToken = null;
            this.mKeyStore.abort(operationToken);
        }
        this.mOperationHandle = 0;
        this.mMessageStreamer = null;
        this.mCachedException = null;
    }

    private void ensureKeystoreOperationInitialized() throws InvalidKeyException {
        if (this.mMessageStreamer != null || this.mCachedException != null) {
            return;
        }
        if (this.mKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        HwKeymasterArguments keymasterInputArgs = new HwKeymasterArguments();
        addAlgorithmSpecificParametersToBegin(keymasterInputArgs);
        HwOperationResult opResult = this.mKeyStore.begin(this.mKey.getAlias(), this.mSigning ? 2 : 3, true, keymasterInputArgs, null, this.mKey.getUid());
        if (opResult == null) {
            throw new ProviderException("Failed to communicate with keystore service");
        }
        this.mOperationToken = opResult.token;
        this.mOperationHandle = opResult.operationHandle;
        InvalidKeyException e = HwUniversalKeyStoreCryptoOperationUtils.getInvalidKeyExceptionForInit(this.mKeyStore, this.mKey, opResult.resultCode);
        if (e != null) {
            throw e;
        } else if (this.mOperationToken == null) {
            throw new ProviderException("Keystore returned null operation token");
        } else if (this.mOperationHandle == 0) {
            throw new ProviderException("Keystore returned invalid operation handle");
        } else {
            this.mMessageStreamer = createMainDataStreamer(this.mKeyStore, opResult.token);
        }
    }

    @NonNull
    protected HwUniversalKeyStoreCryptoOperationStreamer createMainDataStreamer(HwKeystoreManager keyStore, IBinder operationToken) {
        return new HwUniversalKeyStoreCryptoOperationStreamer(new MainDataStream(keyStore, operationToken));
    }

    protected final void addAlgorithmSpecificParametersToBegin(@NonNull HwKeymasterArguments keymasterArgs) {
        keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_ALGORITHM, 1);
        keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
        keymasterArgs.addEnum(HwKeymasterDefs.KM_TAG_PADDING, this.mKeymasterPadding);
    }

    public final long getOperationHandle() {
        return this.mOperationHandle;
    }
}
