package com.android.server.fingerprint;

import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.UserManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Slog;
import java.util.NoSuchElementException;

public abstract class ClientMonitor implements DeathRecipient {
    protected static final boolean DEBUG = true;
    private static final long[] DEFAULT_SUCCESS_VIBRATION_PATTERN = new long[]{0, 30};
    protected static final int ERROR_ESRCH = 3;
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int FRONT_FP_ERROR_VIBRATE_PATTERN = 14;
    private static final int HIDDEN_SPACE_ID = -100;
    private static final long[] HW_FP_ERROR_VIBRATE_PATTERN = new long[]{0, 30};
    protected static final String TAG = "FingerprintService";
    public static int mAcquiredInfo = -1;
    protected boolean mAlreadyCancelled;
    private final Context mContext;
    private final VibrationEffect mErrorVibrationEffect = VibrationEffect.get(1);
    private final int mGroupId;
    private final long mHalDeviceId;
    private final boolean mIsRestricted;
    private final String mOwner;
    private IFingerprintServiceReceiver mReceiver;
    private final VibrationEffect mSuccessVibrationEffect;
    private int mTargetDevice;
    private final int mTargetUserId;
    private IBinder mToken;
    private final UserManager mUserManager;

    public abstract IBiometricsFingerprint getFingerprintDaemon();

    public abstract void notifyUserActivity();

    public abstract boolean onAuthenticated(int i, int i2);

    public abstract boolean onEnrollResult(int i, int i2, int i3);

    public abstract boolean onEnumerationResult(int i, int i2, int i3);

    public abstract boolean onRemoved(int i, int i2, int i3);

    public abstract int start();

    public abstract int stop(boolean z);

    public ClientMonitor(Context context, long halDeviceId, IBinder token, IFingerprintServiceReceiver receiver, int userId, int groupId, boolean restricted, String owner) {
        this.mContext = context;
        this.mHalDeviceId = halDeviceId;
        this.mToken = token;
        this.mReceiver = receiver;
        this.mTargetUserId = userId;
        this.mGroupId = groupId;
        this.mIsRestricted = restricted;
        this.mOwner = owner;
        this.mSuccessVibrationEffect = getSuccessVibrationEffect(context);
        if (token != null) {
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "caught remote exception in linkToDeath: ", e);
            }
        }
        this.mUserManager = UserManager.get(this.mContext);
        this.mTargetDevice = 0;
    }

    public boolean onAcquired(int acquiredInfo, int vendorCode) {
        boolean z;
        if (this.mReceiver == null) {
            return true;
        }
        try {
            this.mReceiver.onAcquired(getHalDeviceId(), acquiredInfo, vendorCode);
            z = false;
            return z;
        } catch (RemoteException e) {
            z = TAG;
            Slog.w(z, "Failed to invoke sendAcquired:", e);
            return true;
        } finally {
            mAcquiredInfo = acquiredInfo;
        }
    }

    public boolean onError(int error, int vendorCode) {
        if (this.mReceiver != null) {
            try {
                this.mReceiver.onError(getHalDeviceId(), error, vendorCode);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to invoke sendError:", e);
            }
        }
        return true;
    }

    public void destroy() {
        Slog.v(TAG, "ClientMonitor destroy");
        if (this.mToken != null) {
            try {
                this.mToken.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Slog.e(TAG, "destroy(): " + this + ":", new Exception("here"));
            }
            this.mToken = null;
        }
        this.mReceiver = null;
    }

    public void binderDied() {
        Slog.v(TAG, "fingerprint app died");
        stop(false);
        this.mToken = null;
        this.mReceiver = null;
        onError(1, 0);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mToken != null) {
                Slog.w(TAG, "removing leaked reference: " + this.mToken);
                onError(1, 0);
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final long getHalDeviceId() {
        return this.mHalDeviceId;
    }

    public final String getOwnerString() {
        return this.mOwner;
    }

    public final IFingerprintServiceReceiver getReceiver() {
        return this.mReceiver;
    }

    public final boolean getIsRestricted() {
        return this.mIsRestricted;
    }

    public final int getTargetUserId() {
        return this.mTargetUserId;
    }

    public final int getGroupId() {
        return this.mGroupId;
    }

    public final IBinder getToken() {
        return this.mToken;
    }

    public int getTargetDevice() {
        return this.mTargetDevice;
    }

    public void setTargetDevice(int deviceIndex) {
        this.mTargetDevice = deviceIndex;
    }

    public final void vibrateSuccess() {
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        if (vibrator != null) {
            vibrator.vibrate(this.mSuccessVibrationEffect);
        }
    }

    public final void vibrateError() {
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        if (vibrator != null) {
            vibrator.vibrate(this.mErrorVibrationEffect);
        }
    }

    public final void vibrateFingerprintErrorHw() {
        if (FRONT_FINGERPRINT_NAVIGATION) {
            Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
            if (vibrator != null) {
                SystemVibrator sysVibrator = (SystemVibrator) vibrator;
                if (sysVibrator != null) {
                    sysVibrator.hwVibrate(null, 14);
                    return;
                }
                return;
            }
            return;
        }
        vibrateError();
    }

    private static VibrationEffect getSuccessVibrationEffect(Context ctx) {
        long[] vibePattern;
        int[] arr = ctx.getResources().getIntArray(17236014);
        if (arr == null || arr.length == 0) {
            vibePattern = DEFAULT_SUCCESS_VIBRATION_PATTERN;
        } else {
            vibePattern = new long[arr.length];
            for (int i = 0; i < arr.length; i++) {
                vibePattern[i] = (long) arr[i];
            }
        }
        if (vibePattern.length == 1) {
            return VibrationEffect.createOneShot(vibePattern[0], -1);
        }
        return VibrationEffect.createWaveform(vibePattern, -1);
    }

    protected final int getRealUserIdForHal(int groupId) {
        UserInfo info = this.mUserManager.getUserInfo(groupId);
        if (info == null || !info.isHwHiddenSpace()) {
            return groupId;
        }
        return HIDDEN_SPACE_ID;
    }
}
