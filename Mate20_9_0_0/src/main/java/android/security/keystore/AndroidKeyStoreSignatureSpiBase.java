package android.security.keystore;

import android.os.IBinder;
import android.security.KeyStore;
import android.security.KeyStoreException;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.OperationResult;
import android.security.keystore.KeyStoreCryptoOperationChunkedStreamer.MainDataStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import libcore.util.EmptyArray;

abstract class AndroidKeyStoreSignatureSpiBase extends SignatureSpi implements KeyStoreCryptoOperation {
    private Exception mCachedException;
    private AndroidKeyStoreKey mKey;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private KeyStoreCryptoOperationStreamer mMessageStreamer;
    private long mOperationHandle;
    private IBinder mOperationToken;
    private boolean mSigning;

    protected abstract void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments);

    protected abstract int getAdditionalEntropyAmountForSign();

    AndroidKeyStoreSignatureSpiBase() {
    }

    protected final void engineInitSign(PrivateKey key) throws InvalidKeyException {
        engineInitSign(key, null);
    }

    protected final void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
        resetAll();
        boolean success = false;
        if (privateKey != null) {
            try {
                if (privateKey instanceof AndroidKeyStorePrivateKey) {
                    AndroidKeyStoreKey keystoreKey = (AndroidKeyStoreKey) privateKey;
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

    protected final void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        resetAll();
        boolean z = false;
        boolean success = false;
        if (publicKey != null) {
            try {
                if (publicKey instanceof AndroidKeyStorePublicKey) {
                    AndroidKeyStorePublicKey keystoreKey = (AndroidKeyStorePublicKey) publicKey;
                    this.mSigning = false;
                    initKey(keystoreKey);
                    this.appRandom = null;
                    ensureKeystoreOperationInitialized();
                    z = true;
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported public key type: ");
                stringBuilder.append(publicKey);
                throw new InvalidKeyException(stringBuilder.toString());
            } finally {
                if (!(
/*
Method generation error in method: android.security.keystore.AndroidKeyStoreSignatureSpiBase.engineInitVerify(java.security.PublicKey):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r1_1 'success' boolean) = (r1_0 'success' boolean), (r0_4 'z' boolean) in method: android.security.keystore.AndroidKeyStoreSignatureSpiBase.engineInitVerify(java.security.PublicKey):void, dex: 
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

    protected void initKey(AndroidKeyStoreKey key) throws InvalidKeyException {
        this.mKey = key;
    }

    protected void resetAll() {
        IBinder operationToken = this.mOperationToken;
        if (operationToken != null) {
            this.mOperationToken = null;
            this.mKeyStore.abort(operationToken);
        }
        this.mSigning = false;
        this.mKey = null;
        this.appRandom = null;
        this.mOperationToken = null;
        this.mOperationHandle = 0;
        this.mMessageStreamer = null;
        this.mCachedException = null;
    }

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
            KeymasterArguments keymasterInputArgs = new KeymasterArguments();
            addAlgorithmSpecificParametersToBegin(keymasterInputArgs);
            OperationResult opResult = this.mKeyStore.begin(this.mKey.getAlias(), this.mSigning ? 2 : 3, true, keymasterInputArgs, null, this.mKey.getUid());
            if (opResult != null) {
                this.mOperationToken = opResult.token;
                this.mOperationHandle = opResult.operationHandle;
                InvalidKeyException e = KeyStoreCryptoOperationUtils.getInvalidKeyExceptionForInit(this.mKeyStore, this.mKey, opResult.resultCode);
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
            throw new KeyStoreConnectException();
        }
        throw new IllegalStateException("Not initialized");
    }

    protected KeyStoreCryptoOperationStreamer createMainDataStreamer(KeyStore keyStore, IBinder operationToken) {
        return new KeyStoreCryptoOperationChunkedStreamer(new MainDataStream(keyStore, operationToken));
    }

    public final long getOperationHandle() {
        return this.mOperationHandle;
    }

    protected final void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                if (len != 0) {
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
                    } catch (KeyStoreException e) {
                        throw new SignatureException(e);
                    }
                }
                return;
            } catch (InvalidKeyException e2) {
                throw new SignatureException(e2);
            }
        }
        throw new SignatureException(this.mCachedException);
    }

    protected final void engineUpdate(byte b) throws SignatureException {
        engineUpdate(new byte[]{b}, 0, 1);
    }

    protected final void engineUpdate(ByteBuffer input) {
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

    protected final int engineSign(byte[] out, int outOffset, int outLen) throws SignatureException {
        return super.engineSign(out, outOffset, outLen);
    }

    protected final byte[] engineSign() throws SignatureException {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                byte[] signature = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, null, KeyStoreCryptoOperationUtils.getRandomBytesToMixIntoKeystoreRng(this.appRandom, getAdditionalEntropyAmountForSign()));
                resetWhilePreservingInitState();
                return signature;
            } catch (KeyStoreException | InvalidKeyException e) {
                throw new SignatureException(e);
            }
        }
        throw new SignatureException(this.mCachedException);
    }

    protected final boolean engineVerify(byte[] signature) throws SignatureException {
        if (this.mCachedException == null) {
            try {
                ensureKeystoreOperationInitialized();
                KeyStoreException e;
                try {
                    byte[] output = this.mMessageStreamer.doFinal(EmptyArray.BYTE, 0, 0, signature, null);
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
                } catch (KeyStoreException e2) {
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

    protected final boolean engineVerify(byte[] sigBytes, int offset, int len) throws SignatureException {
        return engineVerify(ArrayUtils.subarray(sigBytes, offset, len));
    }

    @Deprecated
    protected final Object engineGetParameter(String param) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    @Deprecated
    protected final void engineSetParameter(String param, Object value) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    protected final KeyStore getKeyStore() {
        return this.mKeyStore;
    }

    protected final boolean isSigning() {
        return this.mSigning;
    }
}
