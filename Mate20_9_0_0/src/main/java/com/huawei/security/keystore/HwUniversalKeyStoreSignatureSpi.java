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

    static class PSSPadding extends HwUniversalKeyStoreSignatureSpi {
        private static final int SALT_LENGTH_BYTES = 20;

        PSSPadding(int keymasterDigest) {
            super(keymasterDigest, 3);
        }

        protected final int getAdditionalEntropyAmountForSign() {
            return SALT_LENGTH_BYTES;
        }
    }

    public static final class NONEWithPKCS1Padding extends PKCS1Padding {
        public NONEWithPKCS1Padding() {
            super(0);
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("engineInitVerify publicKey:");
        stringBuilder.append(publicKey.getAlgorithm());
        Log.e(str, stringBuilder.toString());
        boolean z = false;
        boolean success = false;
        if (publicKey != null) {
            try {
                if (publicKey instanceof HwUniversalKeyStorePublicKey) {
                    HwUniversalKeyStorePublicKey keystoreKey = (HwUniversalKeyStorePublicKey) publicKey;
                    this.mSigning = false;
                    initKey(keystoreKey);
                    this.appRandom = null;
                    ensureKeystoreOperationInitialized();
                    z = true;
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unsupported public key type: ");
                stringBuilder2.append(publicKey);
                throw new InvalidKeyException(stringBuilder2.toString());
            } finally {
                if (!(
/*
Method generation error in method: com.huawei.security.keystore.HwUniversalKeyStoreSignatureSpi.engineInitVerify(java.security.PublicKey):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r1_3 'success' boolean) = (r1_2 'success' boolean), (r0_5 'z' boolean) in method: com.huawei.security.keystore.HwUniversalKeyStoreSignatureSpi.engineInitVerify(java.security.PublicKey):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:101)
	at jadx.core.codegen.ConditionGen.wrap(ConditionGen.java:94)
	at jadx.core.codegen.ConditionGen.addCompare(ConditionGen.java:116)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:56)
	at jadx.core.codegen.ConditionGen.wrap(ConditionGen.java:83)
	at jadx.core.codegen.ConditionGen.addNot(ConditionGen.java:143)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:64)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:45)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:118)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
	... 33 more

*/

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    protected void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
        resetAll();
        boolean success = false;
        if (privateKey != null) {
            try {
                if (privateKey instanceof HwUniversalKeyStorePrivateKey) {
                    HwUniversalKeyStoreKey keystoreKey = (HwUniversalKeyStoreKey) privateKey;
                    this.mSigning = true;
                    initKey(keystoreKey);
                    this.appRandom = random;
                    ensureKeystoreOperationInitialized();
                    success = true;
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported private key type: ");
                stringBuilder.append(privateKey);
                throw new InvalidKeyException(stringBuilder.toString());
            } finally {
                if (!success) {
                    resetAll();
                }
            }
        } else {
            throw new InvalidKeyException("Unsupported key: null");
        }
    }

    protected void engineUpdate(byte b) throws SignatureException {
        engineUpdate(new byte[]{b}, 0, 1);
    }

    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                if (len == 0) {
                    Log.e(TAG, "engineUpdate len is 0");
                    return;
                }
                try {
                    byte[] output = this.mMessageStreamer.update(b, off, len);
                    if (output.length != 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Update operation unexpectedly produced output: ");
                        stringBuilder.append(output.length);
                        stringBuilder.append(" bytes");
                        throw new ProviderException(stringBuilder.toString());
                    }
                    return;
                } catch (HwUniversalKeyStoreException e) {
                    throw new SignatureException(e);
                }
            } catch (InvalidKeyException e2) {
                throw new SignatureException(e2);
            }
        }
        throw new SignatureException(this.mCachedException);
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
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                byte[] signature = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, HwUniversalKeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.appRandom, getAdditionalEntropyAmountForSign()));
                resetWhilePreservingInitState();
                return signature;
            } catch (HwUniversalKeyStoreException | InvalidKeyException e) {
                throw new SignatureException(e);
            }
        }
        throw new SignatureException(this.mCachedException);
    }

    protected int engineSign(byte[] outbuf, int offset, int len) throws SignatureException {
        return super.engineSign(outbuf, offset, len);
    }

    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                HwUniversalKeyStoreException e;
                try {
                    byte[] output = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, sigBytes, null);
                    if (output.length == 0) {
                        e = true;
                        resetWhilePreservingInitState();
                        return e;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Signature verification unexpected produced output: ");
                    stringBuilder.append(output.length);
                    stringBuilder.append(" bytes");
                    throw new ProviderException(stringBuilder.toString());
                } catch (HwUniversalKeyStoreException e2) {
                    if (e2.getErrorCode() == -30) {
                        e2 = false;
                    } else {
                        throw new SignatureException(e2);
                    }
                }
            } catch (InvalidKeyException e3) {
                throw new SignatureException(e3);
            }
        }
        throw new SignatureException(this.mCachedException);
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported key algorithm: ");
        stringBuilder.append(key.getAlgorithm());
        stringBuilder.append(". Only");
        stringBuilder.append(HwKeyProperties.KEY_ALGORITHM_RSA);
        stringBuilder.append(" supported");
        throw new InvalidKeyException(stringBuilder.toString());
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
        if (this.mKey != null) {
            HwKeymasterArguments keymasterInputArgs = new HwKeymasterArguments();
            addAlgorithmSpecificParametersToBegin(keymasterInputArgs);
            HwOperationResult opResult = this.mKeyStore.begin(this.mKey.getAlias(), this.mSigning ? 2 : 3, true, keymasterInputArgs, null, this.mKey.getUid());
            if (opResult != null) {
                this.mOperationToken = opResult.token;
                this.mOperationHandle = opResult.operationHandle;
                InvalidKeyException e = HwUniversalKeyStoreCryptoOperationUtils.getInvalidKeyExceptionForInit(this.mKeyStore, this.mKey, opResult.resultCode);
                if (e != null) {
                    throw e;
                } else if (this.mOperationToken == null) {
                    throw new ProviderException("Keystore returned null operation token");
                } else if (this.mOperationHandle != 0) {
                    this.mMessageStreamer = createMainDataStreamer(this.mKeyStore, opResult.token);
                    return;
                } else {
                    throw new ProviderException("Keystore returned invalid operation handle");
                }
            }
            throw new ProviderException("Failed to communicate with keystore service");
        }
        throw new IllegalStateException("Not initialized");
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
