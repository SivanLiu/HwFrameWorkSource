package android.hardware.biometrics;

import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricAuthenticator.BiometricIdentifier;
import android.hardware.biometrics.IBiometricPromptReceiver.Stub;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import java.security.Signature;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class BiometricPrompt implements BiometricAuthenticator, BiometricConstants {
    public static final int DISMISSED_REASON_NEGATIVE = 2;
    public static final int DISMISSED_REASON_POSITIVE = 1;
    public static final int DISMISSED_REASON_USER_CANCEL = 3;
    public static final int HIDE_DIALOG_DELAY = 2000;
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_NEGATIVE_TEXT = "negative_text";
    public static final String KEY_POSITIVE_TEXT = "positive_text";
    public static final String KEY_SUBTITLE = "subtitle";
    public static final String KEY_TITLE = "title";
    private Bundle mBundle;
    IBiometricPromptReceiver mDialogReceiver;
    private FingerprintManager mFingerprintManager;
    private ButtonInfo mNegativeButtonInfo;
    private PackageManager mPackageManager;
    private ButtonInfo mPositiveButtonInfo;

    public static class Builder {
        private final Bundle mBundle = new Bundle();
        private Context mContext;
        private ButtonInfo mNegativeButtonInfo;
        private ButtonInfo mPositiveButtonInfo;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setTitle(CharSequence title) {
            this.mBundle.putCharSequence("title", title);
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle) {
            this.mBundle.putCharSequence(BiometricPrompt.KEY_SUBTITLE, subtitle);
            return this;
        }

        public Builder setDescription(CharSequence description) {
            this.mBundle.putCharSequence("description", description);
            return this;
        }

        public Builder setPositiveButton(CharSequence text, Executor executor, OnClickListener listener) {
            if (TextUtils.isEmpty(text)) {
                throw new IllegalArgumentException("Text must be set and non-empty");
            } else if (executor == null) {
                throw new IllegalArgumentException("Executor must not be null");
            } else if (listener != null) {
                this.mBundle.putCharSequence(BiometricPrompt.KEY_POSITIVE_TEXT, text);
                this.mPositiveButtonInfo = new ButtonInfo(executor, listener);
                return this;
            } else {
                throw new IllegalArgumentException("Listener must not be null");
            }
        }

        public Builder setNegativeButton(CharSequence text, Executor executor, OnClickListener listener) {
            if (TextUtils.isEmpty(text)) {
                throw new IllegalArgumentException("Text must be set and non-empty");
            } else if (executor == null) {
                throw new IllegalArgumentException("Executor must not be null");
            } else if (listener != null) {
                this.mBundle.putCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT, text);
                this.mNegativeButtonInfo = new ButtonInfo(executor, listener);
                return this;
            } else {
                throw new IllegalArgumentException("Listener must not be null");
            }
        }

        public BiometricPrompt build() {
            CharSequence title = this.mBundle.getCharSequence("title");
            CharSequence negative = this.mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
            if (TextUtils.isEmpty(title)) {
                throw new IllegalArgumentException("Title must be set and non-empty");
            } else if (!TextUtils.isEmpty(negative)) {
                return new BiometricPrompt(this.mContext, this.mBundle, this.mPositiveButtonInfo, this.mNegativeButtonInfo, null);
            } else {
                throw new IllegalArgumentException("Negative text must be set and non-empty");
            }
        }
    }

    private static class ButtonInfo {
        Executor executor;
        OnClickListener listener;

        ButtonInfo(Executor ex, OnClickListener l) {
            this.executor = ex;
            this.listener = l;
        }
    }

    public static abstract class AuthenticationCallback extends android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback {
        public void onAuthenticationError(int errorCode, CharSequence errString) {
        }

        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        }

        public void onAuthenticationSucceeded(AuthenticationResult result) {
        }

        public void onAuthenticationFailed() {
        }

        public void onAuthenticationAcquired(int acquireInfo) {
        }

        public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricAuthenticator.AuthenticationResult result) {
            onAuthenticationSucceeded(new AuthenticationResult((CryptoObject) result.getCryptoObject(), result.getId(), result.getUserId()));
        }
    }

    public static class AuthenticationResult extends android.hardware.biometrics.BiometricAuthenticator.AuthenticationResult {
        public AuthenticationResult(CryptoObject crypto, BiometricIdentifier identifier, int userId) {
            super(crypto, identifier, userId);
        }

        public CryptoObject getCryptoObject() {
            return (CryptoObject) super.getCryptoObject();
        }
    }

    public static final class CryptoObject extends CryptoObject {
        public CryptoObject(Signature signature) {
            super(signature);
        }

        public CryptoObject(Cipher cipher) {
            super(cipher);
        }

        public CryptoObject(Mac mac) {
            super(mac);
        }

        public Signature getSignature() {
            return super.getSignature();
        }

        public Cipher getCipher() {
            return super.getCipher();
        }

        public Mac getMac() {
            return super.getMac();
        }
    }

    /* synthetic */ BiometricPrompt(Context x0, Bundle x1, ButtonInfo x2, ButtonInfo x3, AnonymousClass1 x4) {
        this(x0, x1, x2, x3);
    }

    private BiometricPrompt(Context context, Bundle bundle, ButtonInfo positiveButtonInfo, ButtonInfo negativeButtonInfo) {
        this.mDialogReceiver = new Stub() {
            public void onDialogDismissed(int reason) {
                if (reason == 1) {
                    BiometricPrompt.this.mPositiveButtonInfo.executor.execute(new -$$Lambda$BiometricPrompt$1$C3fuslKNv7eJTZG9_jFRfCo5_Y4(this));
                } else if (reason == 2) {
                    BiometricPrompt.this.mNegativeButtonInfo.executor.execute(new -$$Lambda$BiometricPrompt$1$J5PqpiT8xZNiNN1gy9VraVgknaQ(this));
                }
            }
        };
        this.mBundle = bundle;
        this.mPositiveButtonInfo = positiveButtonInfo;
        this.mNegativeButtonInfo = negativeButtonInfo;
        this.mFingerprintManager = (FingerprintManager) context.getSystemService(FingerprintManager.class);
        this.mPackageManager = context.getPackageManager();
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, Executor executor, android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback callback) {
        if (callback instanceof AuthenticationCallback) {
            authenticate(crypto, cancel, executor, (AuthenticationCallback) callback);
            return;
        }
        throw new IllegalArgumentException("Callback cannot be casted");
    }

    public void authenticate(CancellationSignal cancel, Executor executor, android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback callback) {
        if (callback instanceof AuthenticationCallback) {
            authenticate(cancel, executor, (AuthenticationCallback) callback);
            return;
        }
        throw new IllegalArgumentException("Callback cannot be casted");
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, Executor executor, AuthenticationCallback callback) {
        if (!handlePreAuthenticationErrors(callback, executor)) {
            this.mFingerprintManager.authenticate((CryptoObject) crypto, cancel, this.mBundle, executor, this.mDialogReceiver, (android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback) callback);
        }
    }

    public void authenticate(CancellationSignal cancel, Executor executor, AuthenticationCallback callback) {
        if (!handlePreAuthenticationErrors(callback, executor)) {
            this.mFingerprintManager.authenticate(cancel, this.mBundle, executor, this.mDialogReceiver, (android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback) callback);
        }
    }

    private boolean handlePreAuthenticationErrors(AuthenticationCallback callback, Executor executor) {
        if (!this.mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            sendError(12, callback, executor);
            return true;
        } else if (!this.mFingerprintManager.isHardwareDetected()) {
            sendError(1, callback, executor);
            return true;
        } else if (this.mFingerprintManager.hasEnrolledFingerprints()) {
            return false;
        } else {
            sendError(11, callback, executor);
            return true;
        }
    }

    private void sendError(int error, AuthenticationCallback callback, Executor executor) {
        executor.execute(new -$$Lambda$BiometricPrompt$HqBGXtBUWNc-v8NoHYsj2gLfaRw(this, callback, error));
    }
}
