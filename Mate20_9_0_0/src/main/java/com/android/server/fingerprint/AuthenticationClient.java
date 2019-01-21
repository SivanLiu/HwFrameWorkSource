package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.biometrics.IBiometricPromptReceiver.Stub;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;

public abstract class AuthenticationClient extends ClientMonitor {
    public static final int LOCKOUT_NONE = 0;
    public static final int LOCKOUT_PERMANENT = 2;
    public static final int LOCKOUT_TIMED = 1;
    private static final int SPECIAL_USER_ID = -101;
    private static final String mKeyguardPackage = "com.android.systemui";
    private boolean mAlreadyCancelled;
    private Bundle mBundle;
    protected boolean mDialogDismissed;
    protected IBiometricPromptReceiver mDialogReceiver;
    private IBiometricPromptReceiver mDialogReceiverFromClient;
    private final FingerprintManager mFingerprintManager;
    protected int mFlags;
    private boolean mInLockout;
    private long mOpId;
    private IStatusBarService mStatusBarService;
    private String opPackageName;

    public abstract int handleFailedAttempt();

    public abstract boolean inLockoutMode();

    public abstract void onStart();

    public abstract void onStop();

    public abstract void resetFailedAttempts();

    public AuthenticationClient(Context context, long halDeviceId, IBinder token, IFingerprintServiceReceiver receiver, int targetUserId, int groupId, long opId, boolean restricted, String owner, Bundle bundle, IBiometricPromptReceiver dialogReceiver, IStatusBarService statusBarService) {
        this(context, halDeviceId, token, receiver, targetUserId, groupId, opId, restricted, owner, 0, bundle, dialogReceiver, statusBarService);
    }

    public AuthenticationClient(Context context, long halDeviceId, IBinder token, IFingerprintServiceReceiver receiver, int targetUserId, int groupId, long opId, boolean restricted, String owner, int flags, Bundle bundle, IBiometricPromptReceiver dialogReceiver, IStatusBarService statusBarService) {
        super(context, halDeviceId, token, receiver, targetUserId, groupId, restricted, owner);
        this.mDialogReceiver = new Stub() {
            public void onDialogDismissed(int reason) {
                if (AuthenticationClient.this.mBundle != null && AuthenticationClient.this.mDialogReceiverFromClient != null) {
                    try {
                        AuthenticationClient.this.mDialogReceiverFromClient.onDialogDismissed(reason);
                        if (reason == 3) {
                            AuthenticationClient.this.onError(10, 0);
                        }
                        AuthenticationClient.this.mDialogDismissed = true;
                    } catch (RemoteException e) {
                        Slog.e("FingerprintService", "Unable to notify dialog dismissed", e);
                    }
                    AuthenticationClient.this.stop(true);
                }
            }
        };
        this.mOpId = opId;
        this.mBundle = bundle;
        this.mDialogReceiverFromClient = dialogReceiver;
        this.mStatusBarService = statusBarService;
        this.mFingerprintManager = (FingerprintManager) getContext().getSystemService("fingerprint");
        this.opPackageName = owner;
        this.mFlags = flags;
    }

    public void binderDied() {
        super.binderDied();
        stop(false);
    }

    public boolean onAcquired(int acquiredInfo, int vendorCode) {
        if (this.mBundle == null) {
            return super.onAcquired(acquiredInfo, vendorCode);
        }
        if (acquiredInfo != 0) {
            boolean acquiredString;
            try {
                IStatusBarService iStatusBarService = this.mStatusBarService;
                acquiredString = this.mFingerprintManager.getAcquiredString(acquiredInfo, vendorCode);
                iStatusBarService.onFingerprintHelp(acquiredString);
            } catch (RemoteException e) {
                acquiredString = "FingerprintService";
                Slog.e(acquiredString, "Remote exception when sending acquired message", e);
                acquiredString = true;
                return acquiredString;
            } finally {
                if (acquiredInfo == 0) {
                    notifyUserActivity();
                }
            }
        }
        if (acquiredInfo == 0) {
            notifyUserActivity();
        }
        return false;
    }

    public boolean onError(int error, int vendorCode) {
        if (this.mDialogDismissed) {
            return true;
        }
        if (this.mBundle != null) {
            try {
                this.mStatusBarService.onFingerprintError(this.mFingerprintManager.getErrorString(error, vendorCode));
            } catch (RemoteException e) {
                Slog.e("FingerprintService", "Remote exception when sending error", e);
            }
        }
        return super.onError(error, vendorCode);
    }

    public void handleHwFailedAttempt(int flags, String packagesName) {
    }

    private boolean isScreenOn(Context context) {
        if (context == null) {
            Slog.w("FingerprintService", "input context is null");
            return false;
        }
        PowerManager power = (PowerManager) context.getSystemService("power");
        if (power != null) {
            return power.isScreenOn();
        }
        Slog.w("FingerprintService", "PowerManager is null");
        return false;
    }

    public boolean onAuthenticated(int fingerId, int groupId) {
        boolean result = false;
        int i = 0;
        boolean authenticated = fingerId != 0;
        if (this.mBundle != null) {
            if (authenticated) {
                try {
                    this.mStatusBarService.onFingerprintAuthenticated();
                } catch (RemoteException e) {
                    Slog.e("FingerprintService", "Failed to notify Authenticated:", e);
                }
            } else {
                this.mStatusBarService.onFingerprintHelp(getContext().getResources().getString(17040082));
            }
        }
        IFingerprintServiceReceiver receiver = getReceiver();
        if (receiver != null) {
            try {
                MetricsLogger.action(getContext(), 252, authenticated);
                if (authenticated) {
                    Fingerprint fp;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onAuthenticated(owner=");
                    stringBuilder.append(getOwnerString());
                    stringBuilder.append(", id=");
                    stringBuilder.append(fingerId);
                    stringBuilder.append(", gp=");
                    stringBuilder.append(groupId);
                    stringBuilder.append(")");
                    Slog.v("FingerprintService", stringBuilder.toString());
                    if (getIsRestricted()) {
                        fp = null;
                    } else {
                        Fingerprint fingerprint = new Fingerprint(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, groupId, fingerId, getHalDeviceId());
                    }
                    receiver.onAuthenticationSucceeded(getHalDeviceId(), fp, getTargetUserId());
                } else {
                    if (isScreenOn(getContext())) {
                        handleHwFailedAttempt(this.mFlags, this.opPackageName);
                    }
                    receiver.onAuthenticationFailed(getHalDeviceId());
                }
                notifyUserActivity();
                mAcquiredInfo = -1;
            } catch (RemoteException e2) {
                Slog.w("FingerprintService", "Failed to notify Authenticated:", e2);
                result = true;
            }
        } else {
            result = true;
        }
        if (authenticated) {
            result |= 1;
            resetFailedAttempts();
            onStop();
            return result;
        }
        if (!(receiver == null || isKeyguard(this.opPackageName))) {
            vibrateFingerprintErrorHw();
        }
        if (!isScreenOn(getContext()) || inLockoutMode()) {
            return result;
        }
        int lockoutMode = handleFailedAttempt();
        if (lockoutMode != 0) {
            try {
                int errorCode;
                this.mInLockout = true;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Forcing lockout (fp driver code should do this!), mode(");
                stringBuilder2.append(lockoutMode);
                stringBuilder2.append(")");
                Slog.w("FingerprintService", stringBuilder2.toString());
                stop(false);
                if (lockoutMode == 1) {
                    errorCode = 7;
                } else {
                    errorCode = 9;
                }
                receiver.onError(getHalDeviceId(), errorCode, 0);
                if (this.mBundle != null) {
                    this.mStatusBarService.onFingerprintError(this.mFingerprintManager.getErrorString(errorCode, 0));
                }
            } catch (RemoteException e3) {
                Slog.w("FingerprintService", "Failed to notify lockout:", e3);
            }
        }
        if (lockoutMode != 0) {
            i = 1;
        }
        return result | i;
    }

    public int start() {
        IBiometricsFingerprint daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w("FingerprintService", "start authentication: no fingerprint HAL!");
            return 3;
        }
        onStart();
        try {
            int newGroupId = getGroupId();
            if (newGroupId != SPECIAL_USER_ID) {
                newGroupId = getRealUserIdForHal(getGroupId());
            }
            int result = daemon.authenticate(this.mOpId, newGroupId);
            StringBuilder stringBuilder;
            if (result != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("startAuthentication failed, result=");
                stringBuilder.append(result);
                Slog.w("FingerprintService", stringBuilder.toString());
                MetricsLogger.histogram(getContext(), "fingeprintd_auth_start_error", result);
                onError(1, 0);
                return result;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("client ");
            stringBuilder.append(getOwnerString());
            stringBuilder.append(" is authenticating...");
            Slog.w("FingerprintService", stringBuilder.toString());
            if (this.mBundle != null) {
                try {
                    this.mStatusBarService.showFingerprintDialog(this.mBundle, this.mDialogReceiver);
                } catch (RemoteException e) {
                    Slog.e("FingerprintService", "Unable to show fingerprint dialog", e);
                }
            }
            return 0;
        } catch (RemoteException e2) {
            Slog.e("FingerprintService", "startAuthentication failed", e2);
            return 3;
        }
    }

    private boolean isKeyguard(String clientPackage) {
        return mKeyguardPackage.equals(clientPackage);
    }

    public int stop(boolean initiatedByClient) {
        if (this.mAlreadyCancelled) {
            Slog.w("FingerprintService", "stopAuthentication: already cancelled!");
            return 0;
        }
        onStop();
        IBiometricsFingerprint daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w("FingerprintService", "stopAuthentication: no fingerprint HAL!");
            return 3;
        }
        try {
            int result = daemon.cancel();
            if (result != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopAuthentication failed, result=");
                stringBuilder.append(result);
                Slog.w("FingerprintService", stringBuilder.toString());
                if (!(this.mBundle == null || this.mDialogDismissed || this.mInLockout)) {
                    try {
                        this.mStatusBarService.hideFingerprintDialog();
                    } catch (RemoteException e) {
                        Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e);
                    }
                }
                return result;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("client ");
            stringBuilder2.append(getOwnerString());
            stringBuilder2.append(" is no longer authenticating");
            Slog.w("FingerprintService", stringBuilder2.toString());
            if (!(this.mBundle == null || this.mDialogDismissed || this.mInLockout)) {
                try {
                    this.mStatusBarService.hideFingerprintDialog();
                } catch (RemoteException e2) {
                    Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e2);
                }
            }
            this.mAlreadyCancelled = true;
            return 0;
        } catch (RemoteException e3) {
            Slog.e("FingerprintService", "stopAuthentication failed", e3);
            if (!(this.mBundle == null || this.mDialogDismissed || this.mInLockout)) {
                try {
                    this.mStatusBarService.hideFingerprintDialog();
                } catch (RemoteException e4) {
                    Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e4);
                }
            }
            return 3;
        } catch (Throwable th) {
            if (!(this.mBundle == null || this.mDialogDismissed || this.mInLockout)) {
                try {
                    this.mStatusBarService.hideFingerprintDialog();
                } catch (RemoteException e22) {
                    Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e22);
                }
            }
        }
    }

    public boolean onEnrollResult(int fingerId, int groupId, int remaining) {
        Slog.w("FingerprintService", "onEnrollResult() called for authenticate!");
        return true;
    }

    public boolean onRemoved(int fingerId, int groupId, int remaining) {
        Slog.w("FingerprintService", "onRemoved() called for authenticate!");
        return true;
    }

    public boolean onEnumerationResult(int fingerId, int groupId, int remaining) {
        Slog.w("FingerprintService", "onEnumerationResult() called for authenticate!");
        return true;
    }
}
