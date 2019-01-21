package android.hardware.fingerprint;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.fingerprint.IFingerprintServiceReceiver.Stub;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.R;
import java.security.Signature;
import java.util.List;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.Mac;

@Deprecated
public class FingerprintManager implements BiometricFingerprintConstants {
    private static final boolean DEBUG = true;
    public static final int HW_FINGERPRINT_ACQUIRED_VENDOR_BASE = 2000;
    public static final int HW_FINGERPRINT_ACQUIRED_VENDOR_BASE_END = 3000;
    public static final int HW_UD_FINGERPRINT_HELP_PAUSE_VENDORCODE = 1011;
    public static final int HW_UD_FINGERPRINT_HELP_RESUME_VENDORCODE = 1012;
    private static final int MSG_ACQUIRED = 101;
    private static final int MSG_AUTHENTICATION_FAILED = 103;
    private static final int MSG_AUTHENTICATION_SUCCEEDED = 102;
    private static final int MSG_ENROLL_RESULT = 100;
    private static final int MSG_ENUMERATED = 106;
    private static final int MSG_ERROR = 104;
    private static final int MSG_REMOVED = 105;
    private static final String TAG = "FingerprintManager";
    private android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback mAuthenticationCallback;
    private Context mContext;
    private android.hardware.biometrics.CryptoObject mCryptoObject;
    private EnrollmentCallback mEnrollmentCallback;
    private EnumerateCallback mEnumerateCallback;
    private Executor mExecutor;
    private Handler mHandler;
    private RemovalCallback mRemovalCallback;
    private Fingerprint mRemovalFingerprint;
    private IFingerprintService mService;
    private IFingerprintServiceReceiver mServiceReceiver = new Stub() {
        public void onEnrollResult(long deviceId, int fingerId, int groupId, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(100, remaining, 0, new Fingerprint(null, groupId, fingerId, deviceId)).sendToTarget();
        }

        public void onAcquired(long deviceId, int acquireInfo, int vendorCode) {
            if (FingerprintManager.this.mExecutor != null) {
                FingerprintManager.this.mExecutor.execute(new -$$Lambda$FingerprintManager$2$-CkUh5EAfiFsfsEamQtkeaLZq6M(this, deviceId, acquireInfo, vendorCode));
            } else {
                FingerprintManager.this.mHandler.obtainMessage(101, acquireInfo, vendorCode, Long.valueOf(deviceId)).sendToTarget();
            }
        }

        public void onAuthenticationSucceeded(long deviceId, Fingerprint fp, int userId) {
            Log.d(FingerprintManager.TAG, "binder call and send onAuthenticationSucceeded msg");
            if (FingerprintManager.this.mExecutor != null) {
                FingerprintManager.this.mExecutor.execute(new -$$Lambda$FingerprintManager$2$O5sigT8DLDwmCzdvD-k13MacOBU(this, fp, userId));
                return;
            }
            Message msg = FingerprintManager.this.mHandler.obtainMessage(102, userId, 0, fp);
            if (FingerprintManager.this.mHandler.hasMessages(101)) {
                msg.sendToTarget();
            } else {
                FingerprintManager.this.mHandler.sendMessageAtFrontOfQueue(msg);
            }
        }

        public void onAuthenticationFailed(long deviceId) {
            if (FingerprintManager.this.mExecutor != null) {
                FingerprintManager.this.mExecutor.execute(new -$$Lambda$FingerprintManager$2$ycpCnXGQKksU_rpxKvBm1XDbloE(this));
            } else {
                FingerprintManager.this.mHandler.obtainMessage(103).sendToTarget();
            }
        }

        public void onError(long deviceId, int error, int vendorCode) {
            if (FingerprintManager.this.mExecutor == null) {
                FingerprintManager.this.mHandler.obtainMessage(104, error, vendorCode, Long.valueOf(deviceId)).sendToTarget();
            } else if (error == 10 || error == 5) {
                FingerprintManager.this.mExecutor.execute(new -$$Lambda$FingerprintManager$2$iiSGvjInjtzVqJ-wXw-4RQIjKDs(this, deviceId, error, vendorCode));
            } else {
                FingerprintManager.this.mHandler.postDelayed(new -$$Lambda$FingerprintManager$2$n67wlbYWr0PNZwBB3xLLO4RgAq4(this, deviceId, error, vendorCode), 2000);
            }
        }

        public void onRemoved(long deviceId, int fingerId, int groupId, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(105, remaining, 0, new Fingerprint(null, groupId, fingerId, deviceId)).sendToTarget();
        }

        public void onEnumerated(long deviceId, int fingerId, int groupId, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(106, fingerId, groupId, Long.valueOf(deviceId)).sendToTarget();
        }
    };
    private IBinder mToken = new Binder();

    @Deprecated
    public static class AuthenticationResult {
        private CryptoObject mCryptoObject;
        private Fingerprint mFingerprint;
        private int mUserId;

        public AuthenticationResult(CryptoObject crypto, Fingerprint fingerprint, int userId) {
            this.mCryptoObject = crypto;
            this.mFingerprint = fingerprint;
            this.mUserId = userId;
        }

        public CryptoObject getCryptoObject() {
            return this.mCryptoObject;
        }

        public Fingerprint getFingerprint() {
            return this.mFingerprint;
        }

        public int getUserId() {
            return this.mUserId;
        }
    }

    public static abstract class EnrollmentCallback {
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        }

        public void onEnrollmentProgress(int remaining) {
        }
    }

    public static abstract class EnumerateCallback {
        public void onEnumerateError(int errMsgId, CharSequence errString) {
        }

        public void onEnumerate(Fingerprint fingerprint) {
        }
    }

    public static abstract class LockoutResetCallback {
        public void onLockoutReset() {
        }
    }

    public static abstract class RemovalCallback {
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
        }

        public void onRemovalSucceeded(Fingerprint fp, int remaining) {
        }
    }

    @Deprecated
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
            onAuthenticationSucceeded(new AuthenticationResult((CryptoObject) result.getCryptoObject(), (Fingerprint) result.getId(), result.getUserId()));
        }
    }

    @Deprecated
    public static final class CryptoObject extends android.hardware.biometrics.CryptoObject {
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

    private class MyHandler extends Handler {
        private MyHandler(Context context) {
            super(context.getMainLooper());
        }

        private MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    sendEnrollResult((Fingerprint) msg.obj, msg.arg1);
                    return;
                case 101:
                    FingerprintManager.this.sendAcquiredResult(((Long) msg.obj).longValue(), msg.arg1, msg.arg2);
                    return;
                case 102:
                    FingerprintManager.this.sendAuthenticatedSucceeded((Fingerprint) msg.obj, msg.arg1);
                    return;
                case 103:
                    FingerprintManager.this.sendAuthenticatedFailed();
                    return;
                case 104:
                    FingerprintManager.this.sendErrorResult(((Long) msg.obj).longValue(), msg.arg1, msg.arg2);
                    return;
                case 105:
                    sendRemovedResult((Fingerprint) msg.obj, msg.arg1);
                    return;
                case 106:
                    sendEnumeratedResult(((Long) msg.obj).longValue(), msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }

        private void sendRemovedResult(Fingerprint fingerprint, int remaining) {
            if (FingerprintManager.this.mRemovalCallback != null) {
                if (fingerprint == null) {
                    Slog.e(FingerprintManager.TAG, "Received MSG_REMOVED, but fingerprint is null");
                    return;
                }
                int fingerId = fingerprint.getFingerId();
                int reqFingerId = FingerprintManager.this.mRemovalFingerprint.getFingerId();
                if (reqFingerId == 0 || fingerId == 0 || fingerId == reqFingerId) {
                    int groupId = fingerprint.getGroupId();
                    int reqGroupId = FingerprintManager.this.mRemovalFingerprint.getGroupId();
                    if (groupId != reqGroupId) {
                        String str = FingerprintManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Group id didn't match: ");
                        stringBuilder.append(groupId);
                        stringBuilder.append(" != ");
                        stringBuilder.append(reqGroupId);
                        Slog.w(str, stringBuilder.toString());
                        return;
                    }
                    FingerprintManager.this.mRemovalCallback.onRemovalSucceeded(fingerprint, remaining);
                    return;
                }
                String str2 = FingerprintManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Finger id didn't match: ");
                stringBuilder2.append(fingerId);
                stringBuilder2.append(" != ");
                stringBuilder2.append(reqFingerId);
                Slog.w(str2, stringBuilder2.toString());
            }
        }

        private void sendEnumeratedResult(long deviceId, int fingerId, int groupId) {
            if (FingerprintManager.this.mEnumerateCallback != null) {
                FingerprintManager.this.mEnumerateCallback.onEnumerate(new Fingerprint(null, groupId, fingerId, deviceId));
            }
        }

        private void sendEnrollResult(Fingerprint fp, int remaining) {
            if (FingerprintManager.this.mEnrollmentCallback != null) {
                FingerprintManager.this.mEnrollmentCallback.onEnrollmentProgress(remaining);
            }
        }
    }

    private class OnAuthenticationCancelListener implements OnCancelListener {
        private android.hardware.biometrics.CryptoObject mCrypto;

        public OnAuthenticationCancelListener(android.hardware.biometrics.CryptoObject crypto) {
            this.mCrypto = crypto;
        }

        public void onCancel() {
            FingerprintManager.this.cancelAuthentication(this.mCrypto);
        }
    }

    private class OnEnrollCancelListener implements OnCancelListener {
        private OnEnrollCancelListener() {
        }

        /* synthetic */ OnEnrollCancelListener(FingerprintManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onCancel() {
            FingerprintManager.this.cancelEnrollment();
        }
    }

    @Deprecated
    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, AuthenticationCallback callback, Handler handler) {
        authenticate(crypto, cancel, flags, callback, handler, this.mContext.getUserId());
    }

    private void useHandler(Handler handler) {
        if (handler != null) {
            this.mHandler = new MyHandler(this, handler.getLooper(), null);
        } else if (this.mHandler.getLooper() != this.mContext.getMainLooper()) {
            this.mHandler = new MyHandler(this, this.mContext.getMainLooper(), null);
        }
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, AuthenticationCallback callback, Handler handler, int userId) {
        android.hardware.biometrics.CryptoObject cryptoObject = crypto;
        CancellationSignal cancellationSignal = cancel;
        android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback authenticationCallback = callback;
        Handler handler2;
        if (authenticationCallback != null) {
            if (cancellationSignal != null) {
                if (cancel.isCanceled()) {
                    Slog.w(TAG, "authentication already canceled");
                    return;
                }
                cancellationSignal.setOnCancelListener(new OnAuthenticationCancelListener(cryptoObject));
            }
            if (this.mService != null) {
                try {
                    useHandler(handler);
                    this.mAuthenticationCallback = authenticationCallback;
                    this.mCryptoObject = cryptoObject;
                    this.mService.authenticate(this.mToken, cryptoObject != null ? crypto.getOpId() : 0, userId, this.mServiceReceiver, flags, this.mContext.getOpPackageName(), null, null);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Remote exception while authenticating: ", e);
                    if (authenticationCallback != null) {
                        authenticationCallback.onAuthenticationError(1, getErrorString(1, 0));
                    }
                }
            } else {
                handler2 = handler;
            }
            return;
        }
        handler2 = handler;
        throw new IllegalArgumentException("Must supply an authentication callback");
    }

    private void authenticate(int userId, android.hardware.biometrics.CryptoObject crypto, CancellationSignal cancel, Bundle bundle, Executor executor, IBiometricPromptReceiver receiver, android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback callback) {
        android.hardware.biometrics.CryptoObject cryptoObject = crypto;
        android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback authenticationCallback = callback;
        this.mCryptoObject = cryptoObject;
        if (cancel.isCanceled()) {
            Slog.w(TAG, "authentication already canceled");
            return;
        }
        cancel.setOnCancelListener(new OnAuthenticationCancelListener(cryptoObject));
        if (this.mService != null) {
            try {
                this.mExecutor = executor;
                this.mAuthenticationCallback = authenticationCallback;
                this.mService.authenticate(this.mToken, cryptoObject != null ? crypto.getOpId() : 0, userId, this.mServiceReceiver, 0, this.mContext.getOpPackageName(), bundle, receiver);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while authenticating", e);
                this.mExecutor.execute(new -$$Lambda$FingerprintManager$0Q_OnkqSSy_nQ9iUWqvqVi6QjNE(this, authenticationCallback));
            }
        } else {
            Executor executor2 = executor;
        }
    }

    public void authenticate(CancellationSignal cancel, Bundle bundle, Executor executor, IBiometricPromptReceiver receiver, android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback callback) {
        if (cancel == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        } else if (bundle == null) {
            throw new IllegalArgumentException("Must supply a bundle");
        } else if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        } else if (receiver == null) {
            throw new IllegalArgumentException("Must supply a receiver");
        } else if (callback != null) {
            authenticate(this.mContext.getUserId(), null, cancel, bundle, executor, receiver, callback);
        } else {
            throw new IllegalArgumentException("Must supply a calback");
        }
    }

    public void authenticate(android.hardware.biometrics.CryptoObject crypto, CancellationSignal cancel, Bundle bundle, Executor executor, IBiometricPromptReceiver receiver, android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback callback) {
        if (crypto == null) {
            throw new IllegalArgumentException("Must supply a crypto object");
        } else if (cancel == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        } else if (bundle == null) {
            throw new IllegalArgumentException("Must supply a bundle");
        } else if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        } else if (receiver == null) {
            throw new IllegalArgumentException("Must supply a receiver");
        } else if (callback != null) {
            authenticate(this.mContext.getUserId(), crypto, cancel, bundle, executor, receiver, callback);
        } else {
            throw new IllegalArgumentException("Must supply a callback");
        }
    }

    public void enroll(byte[] token, CancellationSignal cancel, int flags, int userId, EnrollmentCallback callback) {
        if (userId == -2) {
            userId = getCurrentUserId();
        }
        if (callback != null) {
            if (cancel != null) {
                if (cancel.isCanceled()) {
                    Slog.w(TAG, "enrollment already canceled");
                    return;
                }
                cancel.setOnCancelListener(new OnEnrollCancelListener(this, null));
            }
            if (this.mService != null) {
                try {
                    this.mEnrollmentCallback = callback;
                    this.mService.enroll(this.mToken, token, userId, this.mServiceReceiver, flags, this.mContext.getOpPackageName());
                } catch (RemoteException e) {
                    Slog.w(TAG, "Remote exception in enroll: ", e);
                    if (callback != null) {
                        callback.onEnrollmentError(1, getErrorString(1, 0));
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException("Must supply an enrollment callback");
    }

    public long preEnroll() {
        if (this.mService == null) {
            return 0;
        }
        try {
            return this.mService.preEnroll(this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int postEnroll() {
        if (this.mService == null) {
            return 0;
        }
        try {
            return this.mService.postEnroll(this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setActiveUser(int userId) {
        if (this.mService != null) {
            try {
                this.mService.setActiveUser(userId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void remove(Fingerprint fp, int userId, RemovalCallback callback) {
        if (this.mService != null) {
            try {
                this.mRemovalCallback = callback;
                this.mRemovalFingerprint = fp;
                this.mService.remove(this.mToken, fp.getFingerId(), fp.getGroupId(), userId, this.mServiceReceiver);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception in remove: ", e);
                if (callback != null) {
                    callback.onRemovalError(fp, 1, getErrorString(1, 0));
                }
            }
        }
    }

    public void enumerate(int userId, EnumerateCallback callback) {
        if (this.mService != null) {
            try {
                this.mEnumerateCallback = callback;
                this.mService.enumerate(this.mToken, userId, this.mServiceReceiver);
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception in enumerate: ", e);
                if (callback != null) {
                    callback.onEnumerateError(1, getErrorString(1, 0));
                }
            }
        }
    }

    public void rename(int fpId, int userId, String newName) {
        if (this.mService != null) {
            try {
                this.mService.rename(fpId, userId, newName);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "rename(): Service not connected!");
    }

    public List<Fingerprint> getEnrolledFingerprints(int userId) {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getEnrolledFingerprints(userId, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<Fingerprint> getEnrolledFingerprints() {
        return getEnrolledFingerprints(this.mContext.getUserId());
    }

    @Deprecated
    public boolean hasEnrolledFingerprints() {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.hasEnrolledFingerprints(this.mContext.getUserId(), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasEnrolledFingerprints(int userId) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.hasEnrolledFingerprints(userId, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean isHardwareDetected() {
        if (this.mService != null) {
            try {
                return this.mService.isHardwareDetected(0, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "isFingerprintHardwareDetected(): Service not connected!");
        return false;
    }

    public long getAuthenticatorId() {
        if (this.mService != null) {
            try {
                return this.mService.getAuthenticatorId(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "getAuthenticatorId(): Service not connected!");
        return 0;
    }

    public void resetTimeout(byte[] token) {
        if (this.mService != null) {
            try {
                this.mService.resetTimeout(token);
                return;
            } catch (RemoteException e) {
                Log.v(TAG, "Remote exception in resetTimeout(): ", e);
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "resetTimeout(): Service not connected!");
    }

    public void addLockoutResetCallback(final LockoutResetCallback callback) {
        if (this.mService != null) {
            try {
                final PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
                this.mService.addLockoutResetCallback(new IFingerprintServiceLockoutResetCallback.Stub() {
                    public void onLockoutReset(long deviceId, IRemoteCallback serverCallback) throws RemoteException {
                        try {
                            WakeLock wakeLock = powerManager.newWakeLock(1, "lockoutResetCallback");
                            wakeLock.acquire();
                            FingerprintManager.this.mHandler.post(new -$$Lambda$FingerprintManager$1$4i3tUU8mafgvA9HaB2UPD31L6UY(callback, wakeLock));
                        } finally {
                            serverCallback.sendResult(null);
                        }
                    }

                    static /* synthetic */ void lambda$onLockoutReset$0(LockoutResetCallback callback, WakeLock wakeLock) {
                        try {
                            callback.onLockoutReset();
                        } finally {
                            wakeLock.release();
                        }
                    }
                });
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "addLockoutResetCallback(): Service not connected!");
    }

    private void sendAuthenticatedSucceeded(Fingerprint fp, int userId) {
        if (this.mAuthenticationCallback != null) {
            android.hardware.biometrics.BiometricAuthenticator.AuthenticationResult result = new android.hardware.biometrics.BiometricAuthenticator.AuthenticationResult(this.mCryptoObject, fp, userId);
            Log.v(TAG, "callback sendAuthenticatedSucceeded");
            this.mAuthenticationCallback.onAuthenticationSucceeded(result);
            return;
        }
        Log.w(TAG, "mAuthenticationCallback is null send Failed");
    }

    private void sendAuthenticatedFailed() {
        if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationFailed();
        }
    }

    private void sendAcquiredResult(long deviceId, int acquireInfo, int vendorCode) {
        int clientAcquireInfo = acquireInfo == 6 ? vendorCode + 1000 : acquireInfo;
        if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationAcquired(clientAcquireInfo);
        }
        String msg = getAcquiredString(acquireInfo, vendorCode);
        if (msg != null) {
            int clientInfo = acquireInfo == 6 ? vendorCode + 1000 : acquireInfo;
            if (this.mEnrollmentCallback != null) {
                this.mEnrollmentCallback.onEnrollmentHelp(clientInfo, msg);
            } else if (this.mAuthenticationCallback != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendAcquiredResult,clientInfo = ");
                stringBuilder.append(clientInfo);
                Log.e(str, stringBuilder.toString());
                this.mAuthenticationCallback.onAuthenticationHelp(clientInfo, msg);
            }
        }
    }

    private void sendErrorResult(long deviceId, int errMsgId, int vendorCode) {
        int clientErrMsgId = errMsgId == 8 ? vendorCode + 1000 : errMsgId;
        if (this.mEnrollmentCallback != null) {
            this.mEnrollmentCallback.onEnrollmentError(clientErrMsgId, getErrorString(errMsgId, vendorCode));
        } else if (this.mAuthenticationCallback != null) {
            this.mAuthenticationCallback.onAuthenticationError(clientErrMsgId, getErrorString(errMsgId, vendorCode));
        } else if (this.mRemovalCallback != null) {
            this.mRemovalCallback.onRemovalError(this.mRemovalFingerprint, clientErrMsgId, getErrorString(errMsgId, vendorCode));
        } else if (this.mEnumerateCallback != null) {
            this.mEnumerateCallback.onEnumerateError(clientErrMsgId, getErrorString(errMsgId, vendorCode));
        }
    }

    public FingerprintManager(Context context, IFingerprintService service) {
        this.mContext = context;
        this.mService = service;
        if (this.mService == null) {
            Slog.v(TAG, "FingerprintManagerService was null");
        }
        this.mHandler = new MyHandler(this, context, null);
    }

    private int getCurrentUserId() {
        try {
            return ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void cancelEnrollment() {
        if (this.mService != null) {
            try {
                this.mService.cancelEnrollment(this.mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private void cancelAuthentication(android.hardware.biometrics.CryptoObject cryptoObject) {
        if (this.mService != null) {
            try {
                this.mService.cancelAuthentication(this.mToken, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String getErrorString(int errMsg, int vendorCode) {
        switch (errMsg) {
            case 1:
                return this.mContext.getString(R.string.fingerprint_error_hw_not_available);
            case 2:
                return this.mContext.getString(R.string.fingerprint_error_unable_to_process);
            case 3:
                return this.mContext.getString(R.string.fingerprint_error_timeout);
            case 4:
                return this.mContext.getString(R.string.fingerprint_error_no_space);
            case 5:
                return this.mContext.getString(R.string.fingerprint_error_canceled);
            case 7:
                return this.mContext.getString(R.string.fingerprint_error_lockout);
            case 8:
                String[] msgArray = this.mContext.getResources().getStringArray(R.array.fingerprint_error_vendor);
                if (vendorCode < msgArray.length) {
                    return msgArray[vendorCode];
                }
                break;
            case 9:
                return this.mContext.getString(R.string.fingerprint_error_lockout_permanent);
            case 10:
                return this.mContext.getString(R.string.fingerprint_error_user_canceled);
            case 11:
                return this.mContext.getString(R.string.fingerprint_error_no_fingerprints);
            case 12:
                return this.mContext.getString(R.string.fingerprint_error_hw_not_present);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid error message: ");
        stringBuilder.append(errMsg);
        stringBuilder.append(", ");
        stringBuilder.append(vendorCode);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    public String getAcquiredString(int acquireInfo, int vendorCode) {
        switch (acquireInfo) {
            case 0:
                return null;
            case 1:
                return this.mContext.getString(R.string.fingerprint_acquired_partial);
            case 2:
                return this.mContext.getString(R.string.fingerprint_acquired_insufficient);
            case 3:
                return this.mContext.getString(R.string.fingerprint_acquired_imager_dirty);
            case 4:
                return this.mContext.getString(R.string.fingerprint_acquired_too_slow);
            case 5:
                return this.mContext.getString(R.string.fingerprint_acquired_too_fast);
            case 6:
                if (this.mEnrollmentCallback != null && vendorCode + 1000 >= 2000 && vendorCode + 1000 <= HW_FINGERPRINT_ACQUIRED_VENDOR_BASE_END) {
                    return "";
                }
                if (this.mAuthenticationCallback == null || !(vendorCode + 1000 == 1011 || vendorCode + 1000 == 1012)) {
                    String[] msgArray = this.mContext.getResources().getStringArray(R.array.fingerprint_acquired_vendor);
                    if (vendorCode < msgArray.length) {
                        return msgArray[vendorCode];
                    }
                }
                Slog.w(TAG, "ud fingerprint send mask hide or resume helpcode");
                return "";
                break;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid acquired message: ");
        stringBuilder.append(acquireInfo);
        stringBuilder.append(", ");
        stringBuilder.append(vendorCode);
        Slog.w(str, stringBuilder.toString());
        return null;
    }
}
