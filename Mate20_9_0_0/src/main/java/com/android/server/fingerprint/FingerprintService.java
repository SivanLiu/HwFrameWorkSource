package com.android.server.fingerprint;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.PendingIntent;
import android.app.SynchronousUserSwitchObserver;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.Stub;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintClientActiveCallback;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintServiceLockoutResetCallback;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyStore;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.DumpUtils;
import com.android.server.FingerprintUnlockDataCollector;
import com.android.server.ServiceThread;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.am.AssistDataRequester;
import com.android.server.utils.PriorityDump;
import com.huawei.pgmng.log.LogPower;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FingerprintService extends AbsFingerprintService implements DeathRecipient {
    private static final String ACTION_AUTH_FINGER_UP = "com.huawei.finger.action_up";
    private static final String ACTION_LOCKOUT_RESET = "com.android.server.fingerprint.ACTION_LOCKOUT_RESET";
    private static final int BASE_BRIGHTNESS = 3000;
    private static final long CANCEL_TIMEOUT_LIMIT = 3000;
    private static final boolean CLEANUP_UNUSED_FP = true;
    private static final int CODE_DISABLE_FINGERPRINT_VIEW = 1114;
    private static final int CODE_ENABLE_FINGERPRINT_VIEW = 1115;
    private static final int CODE_GET_FINGERPRINT_LIST_ENROLLED = 1118;
    private static final int CODE_GET_HARDWARE_POSITION = 1110;
    private static final int CODE_GET_HARDWARE_TYPE = 1109;
    private static final int CODE_GET_HIGHLIGHT_SPOT_RADIUS = 1122;
    private static final int CODE_GET_HOVER_SUPPORT = 1113;
    private static final int CODE_GET_TOKEN_LEN_RULE = 1103;
    private static final int CODE_IS_FINGERPRINT_HARDWARE_DETECTED = 1119;
    private static final int CODE_IS_FP_NEED_CALIBRATE_RULE = 1101;
    private static final int CODE_IS_SUPPORT_DUAL_FINGERPRINT = 1120;
    private static final int CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION = 1116;
    private static final int CODE_NOTIFY_OPTICAL_CAPTURE = 1111;
    private static final int CODE_REMOVE_FINGERPRINT = 1107;
    private static final int CODE_REMOVE_MASK_AND_SHOW_BUTTON = 1117;
    private static final int CODE_SEND_UNLOCK_LIGHTBRIGHT = 1121;
    private static final int CODE_SET_CALIBRATE_MODE_RULE = 1102;
    private static final int CODE_SET_FINGERPRINT_MASK_VIEW = 1104;
    private static final int CODE_SET_HOVER_SWITCH = 1112;
    private static final int CODE_SHOW_FINGERPRINT_BUTTON = 1106;
    private static final int CODE_SHOW_FINGERPRINT_VIEW = 1105;
    private static final int CODE_SUSPEND_AUTHENTICATE = 1108;
    private static final int CODE_SUSPEND_ENROLL = 1123;
    static final boolean DEBUG = true;
    private static boolean DEBUG_FPLOG = true;
    protected static final int ENROLL_UD = 4096;
    private static final int ERROR_CODE_COMMEN_ERROR = 8;
    private static final long FAIL_LOCKOUT_TIMEOUT_MS = 30000;
    private static final int FINGERPRINT_ACQUIRED_FINGER_DOWN = 2002;
    protected static final int FINGER_DOWN_TYPE_AUTHENTICATING = 1;
    protected static final int FINGER_DOWN_TYPE_AUTHENTICATING_SETTINGS = 2;
    protected static final int FINGER_DOWN_TYPE_ENROLLING = 0;
    private static final int FP_CLOSE = 0;
    private static final String FP_DATA_DIR = "fpdata";
    private static final int HIDDEN_SPACE_ID = -100;
    private static final int HUAWEI_FINGERPRINT_CAPTURE_COMPLETE = 0;
    private static final int HUAWEI_FINGERPRINT_DOWN_UD = 2102;
    private static final int HUAWEI_FINGERPRINT_TRIGGER_FACE_RECOGNIZATION = 2104;
    private static final int HUAWEI_FINGERPRINT_UP = 2003;
    protected static final int HW_FP_AUTH_BOTH_SPACE = 33554432;
    protected static final int HW_FP_AUTH_UD = 134217728;
    protected static final int HW_FP_AUTH_UG = 67108864;
    private static final int HW_FP_NO_COUNT_FAILED_ATTEMPS = 16777216;
    private static final String KEY_LOCKOUT_RESET_USER = "lockout_reset_user";
    private static final int MAX_BRIGHTNESS = 255;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_PERMANENT = 20;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
    private static final int MSG_USER_SWITCHING = 10;
    private static final String PERM_AUTH_FINGER_UP = "com.huawei.authentication.HW_ACCESS_AUTH_SERVICE";
    private static final int PRIMARY_USER_ID = 0;
    protected static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final int SET_FINGER_DOWN_ERROR_END = 4;
    private static final int SET_FINGER_DOWN_NORMAL_END = 3;
    private static final int SET_FINGER_DOWN_START = 2;
    private static final int SPECIAL_USER_ID = -101;
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    static final String TAG = "FingerprintService";
    private static final int TYPE_FINGERPRINT_AUTHENTICATION_RESULT_FAIL = 1;
    private static final int TYPE_FINGERPRINT_AUTHENTICATION_RESULT_SUCCESS = 0;
    private static final int TYPE_FINGERPRINT_AUTHENTICATION_UNCHECKED = 2;
    private long auTime;
    private long downTime;
    private FingerprintUnlockDataCollector fpDataCollector;
    private final IActivityManager mActivityManager;
    private AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private final Map<Integer, Long> mAuthenticatorIds = Collections.synchronizedMap(new HashMap());
    private final CopyOnWriteArrayList<IFingerprintClientActiveCallback> mClientActiveCallbacks = new CopyOnWriteArrayList();
    private Context mContext;
    private HashMap<Integer, PerformanceStats> mCryptoPerformanceMap = new HashMap();
    private int mCurrentAuthFpDev;
    private long mCurrentAuthenticatorId;
    private ClientMonitor mCurrentClient;
    private int mCurrentUserId = -10000;
    @GuardedBy("this")
    private IBiometricsFingerprint mDaemon;
    private IBiometricsFingerprintClientCallback mDaemonCallback = new Stub() {
        public void onEnrollResult(long deviceId, int fingerId, int groupId, int remaining) {
            Slog.w(FingerprintService.TAG, "onEnrollResult 1");
            final long j = deviceId;
            final int i = fingerId;
            final int i2 = groupId;
            final int i3 = remaining;
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    Slog.w(FingerprintService.TAG, "onEnrollResult 2");
                    FingerprintService.this.handleEnrollResult(j, i, i2, i3);
                }
            });
        }

        public void onAcquired(long deviceId, int acquiredInfo, int vendorCode) {
            Slog.w(FingerprintService.TAG, "onAcquired 1");
            final long j = deviceId;
            final int i = acquiredInfo;
            final int i2 = vendorCode;
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    Slog.w(FingerprintService.TAG, "onAcquired 2");
                    FingerprintService.this.handleAcquired(j, i, i2);
                }
            });
            int clientAcquireInfo = acquiredInfo == 6 ? vendorCode + 1000 : acquiredInfo;
            if (FingerprintService.DEBUG_FPLOG) {
                if (clientAcquireInfo == FingerprintService.FINGERPRINT_ACQUIRED_FINGER_DOWN && FingerprintService.this.fpDataCollector != null) {
                    FingerprintService.this.fpDataCollector.reportFingerDown();
                } else if (clientAcquireInfo == 0 && FingerprintService.this.fpDataCollector != null) {
                    FingerprintService.this.fpDataCollector.reportCaptureCompleted();
                }
            }
            if (clientAcquireInfo == FingerprintService.FINGERPRINT_ACQUIRED_FINGER_DOWN) {
                FingerprintService.this.downTime = System.currentTimeMillis();
            }
            if (clientAcquireInfo == FingerprintService.FINGERPRINT_ACQUIRED_FINGER_DOWN && FingerprintService.this.currentClient(FingerprintService.this.mKeyguardPackage)) {
                LogPower.push(HdmiCecKeycode.UI_SOUND_PRESENTATION_BASS_STEP_MINUS);
            }
            if (FingerprintService.this.mCurrentClient == null) {
                Log.e(FingerprintService.TAG, "mCurrentClient is null notifyFinger failed");
                return;
            }
            if (clientAcquireInfo == FingerprintService.FINGERPRINT_ACQUIRED_FINGER_DOWN) {
                Log.d(FingerprintService.TAG, "onAcquired set mCurrentAuthFpDev DEVICE_BACK");
                FingerprintService.this.mCurrentAuthFpDev = 0;
            }
            String currentOpName = FingerprintService.this.mCurrentClient.getOwnerString();
            String str;
            StringBuilder stringBuilder;
            if (clientAcquireInfo == FingerprintService.HUAWEI_FINGERPRINT_DOWN_UD) {
                if (FingerprintService.this.mCurrentClient instanceof AuthenticationClient) {
                    str = FingerprintService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("notify that AuthenticationClient finger down:");
                    stringBuilder.append(currentOpName);
                    Log.d(str, stringBuilder.toString());
                    FingerprintService.this.mCurrentAuthFpDev = 1;
                    FingerprintService.this.notifyFingerDown(1);
                } else if (FingerprintService.this.mCurrentClient instanceof EnrollClient) {
                    Log.d(FingerprintService.TAG, "notify that EnrollClient finger down");
                    FingerprintService.this.notifyFingerDown(0);
                }
            } else if (clientAcquireInfo == 5 || clientAcquireInfo == 1 || clientAcquireInfo == FingerprintService.HUAWEI_FINGERPRINT_UP) {
                if (clientAcquireInfo == 5) {
                    Log.d(FingerprintService.TAG, "FINGERPRINT_ACQUIRED_TOO_FAST notifyCaptureFinished");
                    FingerprintService.this.notifyCaptureFinished(1);
                }
                if (FingerprintService.this.mCurrentClient instanceof AuthenticationClient) {
                    String str2 = FingerprintService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("clientAcquireInfo = ");
                    stringBuilder2.append(clientAcquireInfo);
                    Log.d(str2, stringBuilder2.toString());
                    FingerprintService.this.notifyAuthenticationFinished(currentOpName, 2, FingerprintService.this.mHwFailedAttempts);
                }
            } else if (clientAcquireInfo == 0) {
                if (FingerprintService.this.mCurrentClient instanceof AuthenticationClient) {
                    if (FingerprintService.SETTINGS_PACKAGE_NAME.equals(currentOpName)) {
                        FingerprintService.this.notifyCaptureFinished(2);
                    } else {
                        FingerprintService.this.notifyCaptureFinished(1);
                    }
                }
            } else if (clientAcquireInfo == FingerprintService.HUAWEI_FINGERPRINT_TRIGGER_FACE_RECOGNIZATION) {
                str = FingerprintService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("clientAcquireInfo = ");
                stringBuilder.append(clientAcquireInfo);
                Log.d(str, stringBuilder.toString());
                FingerprintService.this.triggerFaceRecognization();
            }
        }

        public void onAuthenticated(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
            Slog.w(FingerprintService.TAG, "onAuthenticated 1");
            if (FingerprintService.DEBUG_FPLOG && FingerprintService.this.fpDataCollector != null) {
                FingerprintService.this.fpDataCollector.reportFingerprintAuthenticated(fingerId != 0);
            }
            FingerprintService.this.auTime = System.currentTimeMillis();
            final long j = deviceId;
            final int i = fingerId;
            final int i2 = groupId;
            final ArrayList<Byte> arrayList = token;
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    Slog.w(FingerprintService.TAG, "onAuthenticated 2");
                    FingerprintService.this.handleAuthenticated(j, i, i2, arrayList);
                }
            });
        }

        public void onError(long deviceId, int error, int vendorCode) {
            Slog.w(FingerprintService.TAG, "onError 1");
            final long j = deviceId;
            final int i = error;
            final int i2 = vendorCode;
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    Slog.w(FingerprintService.TAG, "onError 2");
                    FingerprintService.this.setPowerState(4);
                    FingerprintService.this.handleError(j, i, i2);
                }
            });
        }

        public void onRemoved(long deviceId, int fingerId, int groupId, int remaining) {
            Slog.w(FingerprintService.TAG, "onRemoved 1");
            final long j = deviceId;
            final int i = fingerId;
            final int i2 = groupId;
            final int i3 = remaining;
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    Slog.w(FingerprintService.TAG, "onRemoved 2");
                    FingerprintService.this.handleRemoved(j, i, i2, i3);
                }
            });
        }

        public void onEnumerate(long deviceId, int fingerId, int groupId, int remaining) {
            final long j = deviceId;
            final int i = fingerId;
            final int i2 = groupId;
            final int i3 = remaining;
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintService.this.handleEnumerate(j, i, i2, i3);
                }
            });
        }
    };
    private SparseIntArray mFailedAttempts;
    private final FingerprintUtils mFingerprintUtils = FingerprintUtils.getInstance();
    private long mHalDeviceId;
    Handler mHandler = null;
    private HwCustFingerprintService mHwCust = null;
    int mHwFailedAttempts = 0;
    private final String mKeyguardPackage;
    private final ArrayList<FingerprintServiceLockoutResetMonitor> mLockoutMonitors = new ArrayList();
    private final BroadcastReceiver mLockoutReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (FingerprintService.ACTION_LOCKOUT_RESET.equals(intent.getAction())) {
                FingerprintService.this.resetFailedAttemptsForUser(true, intent.getIntExtra(FingerprintService.KEY_LOCKOUT_RESET_USER, 0));
            }
        }
    };
    long mLockoutTime = 0;
    private ClientMonitor mPendingClient;
    private HashMap<Integer, PerformanceStats> mPerformanceMap = new HashMap();
    private PerformanceStats mPerformanceStats;
    private final PowerManager mPowerManager;
    private final Runnable mResetClientState = new Runnable() {
        public void run() {
            String str = FingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Client ");
            stringBuilder.append(FingerprintService.this.mCurrentClient != null ? FingerprintService.this.mCurrentClient.getOwnerString() : "null");
            stringBuilder.append(" failed to respond to cancel, starting client ");
            stringBuilder.append(FingerprintService.this.mPendingClient != null ? FingerprintService.this.mPendingClient.getOwnerString() : "null");
            Slog.w(str, stringBuilder.toString());
            FingerprintService.this.mCurrentClient = null;
            FingerprintService.this.startClient(FingerprintService.this.mPendingClient, false);
            FingerprintService.this.mPendingClient = null;
        }
    };
    private final Runnable mResetFailedAttemptsForCurrentUserRunnable = new Runnable() {
        public void run() {
            FingerprintService.this.resetFailedAttemptsForUser(true, ActivityManager.getCurrentUser());
        }
    };
    private IStatusBarService mStatusBarService;
    private boolean mSupportKids = SystemProperties.getBoolean("ro.config.kidsfinger_enable", false);
    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        public void onTaskStackChanged() {
            try {
                if (FingerprintService.this.mCurrentClient instanceof AuthenticationClient) {
                    String currentClient = FingerprintService.this.mCurrentClient.getOwnerString();
                    if (!FingerprintService.this.isKeyguard(currentClient)) {
                        List<RunningTaskInfo> runningTasks = FingerprintService.this.mActivityManager.getTasks(1);
                        if (!runningTasks.isEmpty()) {
                            String topPackage = ((RunningTaskInfo) runningTasks.get(0)).topActivity.getPackageName();
                            if (!topPackage.contentEquals(currentClient)) {
                                String str = FingerprintService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Stopping background authentication, top: ");
                                stringBuilder.append(topPackage);
                                stringBuilder.append(" currentClient: ");
                                stringBuilder.append(currentClient);
                                Slog.e(str, stringBuilder.toString());
                                FingerprintService.this.mCurrentClient.stop(false);
                                FingerprintService.this.notifyAuthenticationCanceled(topPackage);
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.e(FingerprintService.TAG, "Unable to get running tasks", e);
            }
        }
    };
    private SparseBooleanArray mTimedLockoutCleared;
    private IBinder mToken = new Binder();
    private long mUDHalDeviceId;
    private ArrayList<UserFingerprint> mUnknownFingerprints = new ArrayList();
    private final UserManager mUserManager;
    private ClientMonitor mWaitupClient;
    private String opPackageName;

    private class FingerprintServiceLockoutResetMonitor implements IBinder.DeathRecipient {
        private static final long WAKELOCK_TIMEOUT_MS = 2000;
        private final IFingerprintServiceLockoutResetCallback mCallback;
        private final Runnable mRemoveCallbackRunnable = new Runnable() {
            public void run() {
                FingerprintServiceLockoutResetMonitor.this.releaseWakelock();
                FingerprintService.this.removeLockoutResetCallback(FingerprintServiceLockoutResetMonitor.this);
            }
        };
        private final WakeLock mWakeLock;

        public FingerprintServiceLockoutResetMonitor(IFingerprintServiceLockoutResetCallback callback) {
            this.mCallback = callback;
            this.mWakeLock = FingerprintService.this.mPowerManager.newWakeLock(1, "lockout reset callback");
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.w(FingerprintService.TAG, "caught remote exception in linkToDeath", e);
            }
        }

        public void sendLockoutReset() {
            if (this.mCallback != null) {
                try {
                    this.mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
                    this.mCallback.onLockoutReset(FingerprintService.this.mHalDeviceId, new IRemoteCallback.Stub() {
                        public void sendResult(Bundle data) throws RemoteException {
                            FingerprintServiceLockoutResetMonitor.this.releaseWakelock();
                        }
                    });
                } catch (DeadObjectException e) {
                    Slog.w(FingerprintService.TAG, "Death object while invoking onLockoutReset: ", e);
                    FingerprintService.this.mHandler.post(this.mRemoveCallbackRunnable);
                } catch (RemoteException e2) {
                    Slog.w(FingerprintService.TAG, "Failed to invoke onLockoutReset: ", e2);
                    releaseWakelock();
                }
            }
        }

        public void binderDied() {
            Slog.e(FingerprintService.TAG, "Lockout reset callback binder died");
            FingerprintService.this.mHandler.post(this.mRemoveCallbackRunnable);
        }

        private void releaseWakelock() {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    private final class FingerprintServiceWrapper extends IFingerprintService.Stub {
        private FingerprintServiceWrapper() {
        }

        /* synthetic */ FingerprintServiceWrapper(FingerprintService x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (!FingerprintService.this.isHwTransactInterest(code)) {
                return super.onTransact(code, data, reply, flags);
            }
            FingerprintService.this.checkPermission("android.permission.USE_FINGERPRINT");
            return FingerprintService.this.onHwTransact(code, data, reply, flags);
        }

        public long preEnroll(IBinder token) {
            Flog.i(1303, "FingerprintService preEnroll");
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            return FingerprintService.this.startPreEnroll(token);
        }

        public int postEnroll(IBinder token) {
            Flog.i(1303, "FingerprintService postEnroll");
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("postEnroll client uid = ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", postEnroll client pid = ");
            stringBuilder.append(Binder.getCallingPid());
            Flog.i(1303, stringBuilder.toString());
            return FingerprintService.this.startPostEnroll(token);
        }

        public void enroll(IBinder token, byte[] cryptoToken, int userId, IFingerprintServiceReceiver receiver, int flags, String opPackageName) {
            int i;
            String str;
            int enrolled;
            int i2 = userId;
            Flog.i(1303, "FingerprintService enroll");
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            int limit = FingerprintService.this.mContext.getResources().getInteger(17694789);
            if (FingerprintService.this.mFingerprintUtils.isDualFp()) {
                i = flags;
                str = opPackageName;
                enrolled = FingerprintService.this.getEnrolledFingerprintsEx(str, i == 4096 ? 1 : 0, i2).size();
            } else {
                i = flags;
                str = opPackageName;
                enrolled = FingerprintService.this.getEnrolledFingerprints(i2).size();
            }
            if (enrolled >= limit) {
                Slog.w(FingerprintService.TAG, "Too many fingerprints registered");
                return;
            }
            boolean isPrivacyUser = FingerprintService.this.checkPrivacySpaceEnroll(i2, ActivityManager.getCurrentUser());
            if (FingerprintService.this.isCurrentUserOrProfile(i2) || isPrivacyUser) {
                final IBinder iBinder = token;
                final byte[] bArr = cryptoToken;
                final int i3 = i2;
                final IFingerprintServiceReceiver iFingerprintServiceReceiver = receiver;
                final int i4 = i;
                AnonymousClass1 anonymousClass1 = r0;
                final boolean isRestricted = isRestricted();
                Handler handler = FingerprintService.this.mHandler;
                final String str2 = str;
                AnonymousClass1 anonymousClass12 = new Runnable() {
                    public void run() {
                        FingerprintService.this.startEnrollment(iBinder, bArr, i3, iFingerprintServiceReceiver, i4, isRestricted, str2);
                    }
                };
                handler.post(anonymousClass1);
                return;
            }
            Flog.w(1303, "user invalid enroll error");
        }

        private boolean isRestricted() {
            return FingerprintService.this.hasPermission("android.permission.MANAGE_FINGERPRINT") ^ 1;
        }

        public void cancelEnrollment(final IBinder token) {
            Flog.i(1303, "FingerprintService cancelEnrollment");
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.notifyEnrollmentCanceled();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelEnrollment client uid = ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", cancelEnrollment client pid = ");
            stringBuilder.append(Binder.getCallingPid());
            Flog.i(1303, stringBuilder.toString());
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    ClientMonitor client = FingerprintService.this.mCurrentClient;
                    if ((client instanceof EnrollClient) && client.getToken() == token) {
                        client.stop(client.getToken() == token);
                    }
                }
            });
        }

        public void authenticate(IBinder token, long opId, int groupId, IFingerprintServiceReceiver receiver, int flags, String opPackageName, Bundle bundle, IBiometricPromptReceiver dialogReceiver) {
            int hwGroupId;
            int i = groupId;
            String str = opPackageName;
            Flog.i(1303, "FingerprintService authenticate");
            if (i == 0 || !FingerprintService.this.isClonedProfile(i)) {
                hwGroupId = i;
            } else {
                Log.i(FingerprintService.TAG, "Clone profile authenticate,change userid to 0");
                hwGroupId = 0;
            }
            if (HwDeviceManager.disallowOp(50)) {
                Slog.i(FingerprintService.TAG, "MDM forbid fingerprint authentication");
                FingerprintService.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        Toast toast = Toast.makeText(FingerprintService.this.mContext, 33686051, 0);
                        toast.getWindowParams().type = 2010;
                        LayoutParams windowParams = toast.getWindowParams();
                        windowParams.privateFlags |= 16;
                        toast.show();
                    }
                }, 300);
                return;
            }
            String str2;
            StringBuilder stringBuilder;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int callingUserId = UserHandle.getCallingUserId();
            boolean restricted = isRestricted();
            FingerprintService.this.setLivenessSwitch(str);
            if (FingerprintService.this.mSupportKids) {
                str2 = FingerprintService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mSupportKids=");
                stringBuilder.append(FingerprintService.this.mSupportKids);
                Slog.i(str2, stringBuilder.toString());
                FingerprintService.this.setKidsFingerprint(i, FingerprintService.this.isKeyguard(str));
            }
            if (FingerprintService.this.canUseFingerprint(str, true, callingUid, callingPid, callingUserId)) {
                final long j = opId;
                final String str3 = str;
                final IFingerprintServiceReceiver iFingerprintServiceReceiver = receiver;
                final int i2 = flags;
                final Bundle bundle2 = bundle;
                final IBiometricPromptReceiver iBiometricPromptReceiver = dialogReceiver;
                final IBinder iBinder = token;
                AnonymousClass4 anonymousClass4 = r0;
                final int i3 = callingUserId;
                Handler handler = FingerprintService.this.mHandler;
                final boolean z = restricted;
                AnonymousClass4 anonymousClass42 = new Runnable() {
                    public void run() {
                        Slog.i(FingerprintService.TAG, "authenticate run");
                        MetricsLogger.histogram(FingerprintService.this.mContext, "fingerprint_token", j != 0 ? 1 : 0);
                        HashMap<Integer, PerformanceStats> pmap = j == 0 ? FingerprintService.this.mPerformanceMap : FingerprintService.this.mCryptoPerformanceMap;
                        PerformanceStats stats = (PerformanceStats) pmap.get(Integer.valueOf(FingerprintService.this.mCurrentUserId));
                        if (stats == null) {
                            stats = new PerformanceStats(FingerprintService.this, null);
                            pmap.put(Integer.valueOf(FingerprintService.this.mCurrentUserId), stats);
                        }
                        FingerprintService.this.mPerformanceStats = stats;
                        FingerprintService.this.notifyAuthenticationStarted(str3, iFingerprintServiceReceiver, i2, hwGroupId, bundle2, iBiometricPromptReceiver);
                        FingerprintService.this.startAuthentication(iBinder, j, i3, hwGroupId, iFingerprintServiceReceiver, i2, z, str3, bundle2, iBiometricPromptReceiver);
                    }
                };
                handler.post(anonymousClass4);
                return;
            }
            str2 = FingerprintService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("authenticate(): reject ");
            stringBuilder.append(str);
            Slog.v(str2, stringBuilder.toString());
        }

        public void cancelAuthentication(final IBinder token, final String opPackageName) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int callingUserId = UserHandle.getCallingUserId();
            StringBuilder stringBuilder;
            if (FingerprintService.this.canUseFingerprint(opPackageName, false, callingUid, callingPid, callingUserId)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("FingerprintService cancelAuthentication client uid = ");
                stringBuilder.append(callingUid);
                stringBuilder.append(", cancelAuthentication client pid = ");
                stringBuilder.append(callingPid);
                stringBuilder.append(" callingUserId = ");
                stringBuilder.append(callingUserId);
                Flog.i(1303, stringBuilder.toString());
                FingerprintService.this.mHandler.post(new Runnable() {
                    public void run() {
                        FingerprintService.this.notifyAuthenticationCanceled(opPackageName);
                        ClientMonitor client = FingerprintService.this.mCurrentClient;
                        String str;
                        StringBuilder stringBuilder;
                        if (client instanceof AuthenticationClient) {
                            if (client.getToken() == token) {
                                str = FingerprintService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("stop client ");
                                stringBuilder.append(client.getOwnerString());
                                Slog.v(str, stringBuilder.toString());
                                client.stop(client.getToken() == token);
                                return;
                            }
                            str = FingerprintService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("can't stop client ");
                            stringBuilder.append(client.getOwnerString());
                            stringBuilder.append(" since tokens don't match");
                            Slog.v(str, stringBuilder.toString());
                        } else if (client != null) {
                            str = FingerprintService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("can't cancel non-authenticating client ");
                            stringBuilder.append(client.getOwnerString());
                            Slog.v(str, stringBuilder.toString());
                        }
                    }
                });
                return;
            }
            String str = FingerprintService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("cancelAuthentication(): reject ");
            stringBuilder.append(opPackageName);
            Slog.v(str, stringBuilder.toString());
        }

        public void setActiveUser(final int userId) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintService.this.updateActiveGroup(userId, null);
                }
            });
        }

        public void remove(IBinder token, int fingerId, int groupId, int userId, IFingerprintServiceReceiver receiver) {
            Flog.i(1303, "FingerprintService remove");
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            final IBinder iBinder = token;
            final int i = fingerId;
            final int i2 = groupId;
            final int i3 = userId;
            final IFingerprintServiceReceiver iFingerprintServiceReceiver = receiver;
            final boolean isRestricted = isRestricted();
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintService.this.startRemove(iBinder, i, i2, i3, iFingerprintServiceReceiver, isRestricted, false);
                }
            });
        }

        public void enumerate(IBinder token, int userId, IFingerprintServiceReceiver receiver) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            final IBinder iBinder = token;
            final int i = userId;
            final IFingerprintServiceReceiver iFingerprintServiceReceiver = receiver;
            final boolean isRestricted = isRestricted();
            FingerprintService.this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintService.this.startEnumerate(iBinder, i, iFingerprintServiceReceiver, isRestricted, false);
                }
            });
        }

        public boolean isHardwareDetected(long deviceId, String opPackageName) {
            boolean z = false;
            if (!FingerprintService.this.canUseFingerprint(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId(), true)) {
                return false;
            }
            long token = Binder.clearCallingIdentity();
            try {
                if (!(FingerprintService.this.getFingerprintDaemon() == null || FingerprintService.this.mHalDeviceId == 0)) {
                    z = true;
                }
                Binder.restoreCallingIdentity(token);
                return z;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void rename(final int fingerId, final int groupId, final String name) {
            Flog.i(1303, "FingerprintService rename");
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            boolean isPrivacyUser = FingerprintService.this.checkPrivacySpaceEnroll(groupId, ActivityManager.getCurrentUser());
            if (FingerprintService.this.isCurrentUserOrProfile(groupId) || isPrivacyUser) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    public void run() {
                        FingerprintService.this.mFingerprintUtils.renameFingerprintForUser(FingerprintService.this.mContext, fingerId, groupId, name);
                    }
                });
            } else {
                Flog.w(1303, "user invalid rename error");
            }
        }

        public List<Fingerprint> getEnrolledFingerprints(int userId, String opPackageName) {
            Flog.i(1303, "FingerprintService getEnrolledFingerprints");
            if (!FingerprintService.this.canUseFingerprint(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return Collections.emptyList();
            }
            if (!FingerprintService.this.mFingerprintUtils.isDualFp()) {
                return FingerprintService.this.getEnrolledFingerprints(userId);
            }
            String str = FingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dualFingerprint getEnrolledFingerprints and userId is: ");
            stringBuilder.append(userId);
            Slog.d(str, stringBuilder.toString());
            return FingerprintService.this.getEnrolledFingerprintsEx(opPackageName, -1, userId);
        }

        public boolean hasEnrolledFingerprints(int userId, String opPackageName) {
            Flog.i(1303, "FingerprintService hasEnrolledFingerprints");
            boolean z = false;
            if (!FingerprintService.this.canUseFingerprint(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return false;
            }
            if (!FingerprintService.this.mFingerprintUtils.isDualFp()) {
                return FingerprintService.this.hasEnrolledFingerprints(userId);
            }
            String str = FingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dualFingerprint hasEnrolledFingerprints and userId is: ");
            stringBuilder.append(userId);
            Slog.d(str, stringBuilder.toString());
            if (FingerprintService.this.getEnrolledFingerprintsEx(opPackageName, -1, userId).size() > 0) {
                z = true;
            }
            boolean hasEnrollFingerprints = z;
            String str2 = FingerprintService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("dualFingerprint hasEnrolledFingerprints: ");
            stringBuilder.append(hasEnrollFingerprints);
            Slog.d(str2, stringBuilder.toString());
            return hasEnrollFingerprints;
        }

        public long getAuthenticatorId(String opPackageName) {
            Flog.i(1303, "FingerprintService getAuthenticatorId");
            return FingerprintService.this.getAuthenticatorId(opPackageName);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(FingerprintService.this.mContext, FingerprintService.TAG, pw)) {
                long ident = Binder.clearCallingIdentity();
                try {
                    if (args.length <= 0 || !PriorityDump.PROTO_ARG.equals(args[0])) {
                        FingerprintService.this.dumpInternal(pw);
                    } else {
                        FingerprintService.this.dumpProto(fd);
                    }
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public void resetTimeout(byte[] token) {
            FingerprintService.this.checkPermission("android.permission.RESET_FINGERPRINT_LOCKOUT");
            FingerprintService.this.mHandler.post(FingerprintService.this.mResetFailedAttemptsForCurrentUserRunnable);
        }

        public void addLockoutResetCallback(final IFingerprintServiceLockoutResetCallback callback) throws RemoteException {
            if (callback == null) {
                Log.e(FingerprintService.TAG, " FingerprintServiceLockoutResetCallback is null, cannot addLockoutResetMonitor, return");
            } else {
                FingerprintService.this.mHandler.post(new Runnable() {
                    public void run() {
                        FingerprintService.this.addLockoutResetMonitor(new FingerprintServiceLockoutResetMonitor(callback));
                    }
                });
            }
        }

        public boolean isClientActive() {
            boolean z;
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            synchronized (FingerprintService.this) {
                if (FingerprintService.this.mCurrentClient == null) {
                    if (FingerprintService.this.mPendingClient == null) {
                        z = false;
                    }
                }
                z = true;
            }
            return z;
        }

        public void addClientActiveCallback(IFingerprintClientActiveCallback callback) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mClientActiveCallbacks.add(callback);
        }

        public void removeClientActiveCallback(IFingerprintClientActiveCallback callback) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mClientActiveCallbacks.remove(callback);
        }

        public int getRemainingNum() {
            FingerprintService.this.checkPermission("android.permission.USE_FINGERPRINT");
            if (FingerprintService.this.mHwCust != null && FingerprintService.this.mHwCust.isAtt()) {
                return FingerprintService.this.mHwCust.getRemainingNum(FingerprintService.this.mHwFailedAttempts, FingerprintService.this.mContext);
            }
            String str = FingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" Remaining Num Attempts = ");
            stringBuilder.append(5 - FingerprintService.this.mHwFailedAttempts);
            Slog.d(str, stringBuilder.toString());
            return 5 - FingerprintService.this.mHwFailedAttempts;
        }

        public long getRemainingTime() {
            FingerprintService.this.checkPermission("android.permission.USE_FINGERPRINT");
            long now = SystemClock.elapsedRealtime();
            long nowToLockout = now - FingerprintService.this.mLockoutTime;
            String str = FingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remaining Time mLockoutTime = ");
            stringBuilder.append(FingerprintService.this.mLockoutTime);
            stringBuilder.append("  now = ");
            stringBuilder.append(now);
            Slog.d(str, stringBuilder.toString());
            if (nowToLockout <= 0 || nowToLockout >= 30000) {
                return 0;
            }
            return 30000 - nowToLockout;
        }
    }

    private class PerformanceStats {
        int accept;
        int acquire;
        int lockout;
        int permanentLockout;
        int reject;

        private PerformanceStats() {
        }

        /* synthetic */ PerformanceStats(FingerprintService x0, AnonymousClass1 x1) {
            this();
        }
    }

    private class UserFingerprint {
        Fingerprint f;
        int userId;

        public UserFingerprint(Fingerprint f, int userId) {
            this.f = f;
            this.userId = userId;
        }
    }

    public FingerprintService(Context context) {
        super(context);
        this.mContext = context;
        this.mKeyguardPackage = ComponentName.unflattenFromString(context.getResources().getString(17039825)).getPackageName();
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mContext.registerReceiver(this.mLockoutReceiver, new IntentFilter(ACTION_LOCKOUT_RESET), "android.permission.RESET_FINGERPRINT_LOCKOUT", null);
        this.mUserManager = UserManager.get(this.mContext);
        this.mTimedLockoutCleared = new SparseBooleanArray();
        this.mFailedAttempts = new SparseIntArray();
        this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        this.mActivityManager = ActivityManager.getService();
        this.fpDataCollector = FingerprintUnlockDataCollector.getInstance();
        ServiceThread fingerprintThread = new ServiceThread("fingerprintServcie", -8, false);
        fingerprintThread.start();
        this.mHandler = new Handler(fingerprintThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what != 10) {
                    String str = FingerprintService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown message:");
                    stringBuilder.append(msg.what);
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
                Slog.i(FingerprintService.TAG, "MSG_USER_SWITCHING");
                FingerprintService.this.handleUserSwitching(msg.arg1);
            }
        };
    }

    public void serviceDied(long cookie) {
        Slog.v(TAG, "fingerprint HAL died");
        MetricsLogger.count(this.mContext, "fingerprintd_died", 1);
        handleError(this.mHalDeviceId, 1, 0);
    }

    public synchronized IBiometricsFingerprint getFingerprintDaemon() {
        if (this.mDaemon == null) {
            Slog.v(TAG, "mDaemon was null, reconnect to fingerprint");
            try {
                this.mDaemon = IBiometricsFingerprint.getService();
            } catch (NoSuchElementException e) {
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to get biometric interface", e2);
            }
            if (this.mDaemon == null) {
                Slog.w(TAG, "fingerprint HIDL not available");
                return null;
            }
            this.mDaemon.asBinder().linkToDeath(this, 0);
            try {
                this.mHalDeviceId = this.mDaemon.setNotify(this.mDaemonCallback);
            } catch (RemoteException e22) {
                Slog.e(TAG, "Failed to open fingerprint HAL", e22);
                this.mDaemon = null;
            }
            if (isSupportDualFingerprint() && this.mDaemon != null && sendCommandToHal(100) == 0) {
                try {
                    this.mUDHalDeviceId = this.mDaemon.setNotify(this.mDaemonCallback);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("dualFingerprint:mUDHalDeviceId is ");
                    stringBuilder.append(this.mUDHalDeviceId);
                    Slog.d(str, stringBuilder.toString());
                } catch (RemoteException e3) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("dualFingerprint Failed to setNotify callback for UD");
                    stringBuilder2.append(e3);
                    Slog.e(str2, stringBuilder2.toString());
                }
            }
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Fingerprint HAL id: ");
            stringBuilder3.append(this.mHalDeviceId);
            Slog.v(str3, stringBuilder3.toString());
            if (this.mHalDeviceId == 0) {
                if (!FingerprintUtils.getInstance().isDualFp() || this.mUDHalDeviceId == 0) {
                    Slog.w(TAG, "Failed to open Fingerprint HAL!");
                    MetricsLogger.count(this.mContext, "fingerprintd_openhal_error", 1);
                    this.mDaemon = null;
                }
            }
            loadAuthenticatorIds();
            updateActiveGroup(ActivityManager.getCurrentUser(), null);
            doFingerprintCleanupForUser(ActivityManager.getCurrentUser());
        }
        return this.mDaemon;
    }

    protected long getHalDeviceId() {
        return this.mHalDeviceId;
    }

    protected long getUdHalDeviceId() {
        return this.mUDHalDeviceId;
    }

    private void loadAuthenticatorIds() {
        long t = System.currentTimeMillis();
        this.mAuthenticatorIds.clear();
        for (UserInfo user : UserManager.get(this.mContext).getUsers(true)) {
            int userId = getUserOrWorkProfileId(null, user.id);
            if (!this.mAuthenticatorIds.containsKey(Integer.valueOf(userId))) {
                updateActiveGroup(userId, null);
            }
        }
        long t2 = System.currentTimeMillis() - t;
        if (t2 > 1000) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadAuthenticatorIds() taking too long: ");
            stringBuilder.append(t2);
            stringBuilder.append("ms");
            Slog.w(str, stringBuilder.toString());
        }
    }

    private void doFingerprintCleanupForUser(int userId) {
        enumerateUser(userId);
    }

    private void clearEnumerateState() {
        Slog.v(TAG, "clearEnumerateState()");
        this.mUnknownFingerprints.clear();
    }

    private void enumerateUser(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Enumerating user(");
        stringBuilder.append(userId);
        stringBuilder.append(")");
        Slog.v(str, stringBuilder.toString());
        startEnumerate(this.mToken, userId, null, hasPermission("android.permission.MANAGE_FINGERPRINT") ^ 1, true);
    }

    private void cleanupUnknownFingerprints() {
        if (this.mUnknownFingerprints.isEmpty()) {
            clearEnumerateState();
            return;
        }
        UserFingerprint uf = (UserFingerprint) this.mUnknownFingerprints.get(0);
        this.mUnknownFingerprints.remove(uf);
        startRemove(this.mToken, uf.f.getFingerId(), uf.f.getGroupId(), uf.userId, null, hasPermission("android.permission.MANAGE_FINGERPRINT") ^ 1, true);
    }

    protected void handleEnumerate(long deviceId, int fingerId, int groupId, int remaining) {
        ClientMonitor client = this.mCurrentClient;
        if ((client instanceof InternalRemovalClient) || (client instanceof EnumerateClient)) {
            client.onEnumerationResult(fingerId, groupId, remaining);
            if (remaining == 0) {
                if (client instanceof InternalEnumerateClient) {
                    List<Fingerprint> unknownFingerprints = ((InternalEnumerateClient) client).getUnknownFingerprints();
                    if (!unknownFingerprints.isEmpty()) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Adding ");
                        stringBuilder.append(unknownFingerprints.size());
                        stringBuilder.append(" fingerprints for deletion");
                        Slog.w(str, stringBuilder.toString());
                    }
                    for (Fingerprint f : unknownFingerprints) {
                        this.mUnknownFingerprints.add(new UserFingerprint(f, client.getTargetUserId()));
                    }
                    removeClient(client);
                    cleanupUnknownFingerprints();
                } else {
                    removeClient(client);
                }
            }
        }
    }

    protected void handleError(long deviceId, int error, int vendorCode) {
        String str;
        StringBuilder stringBuilder;
        ClientMonitor client = this.mCurrentClient;
        if (client instanceof EnrollClient) {
            notifyEnrollmentCanceled();
        }
        if (error == 8 && vendorCode > BASE_BRIGHTNESS) {
            vendorCode -= 3000;
            int i = 255;
            if (vendorCode < 255) {
                i = vendorCode;
            }
            vendorCode = i;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("change brightness to ");
            stringBuilder.append(vendorCode);
            Slog.w(str, stringBuilder.toString());
            notifyFingerCalibrarion(vendorCode);
        }
        if ((client instanceof InternalRemovalClient) || (client instanceof InternalEnumerateClient)) {
            clearEnumerateState();
        }
        if (client != null && client.onError(error, vendorCode)) {
            removeClient(client);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("handleError(client=");
        stringBuilder.append(client != null ? client.getOwnerString() : "null");
        stringBuilder.append(", error = ");
        stringBuilder.append(error);
        stringBuilder.append(")");
        Slog.v(str, stringBuilder.toString());
        if (error == 5) {
            this.mHandler.removeCallbacks(this.mResetClientState);
            if (this.mPendingClient != null) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("start pending client ");
                stringBuilder2.append(this.mPendingClient.getOwnerString());
                Slog.v(str, stringBuilder2.toString());
                startClient(this.mPendingClient, false);
                this.mPendingClient = null;
            }
        } else if (error == 1) {
            Slog.w(TAG, "Got ERROR_HW_UNAVAILABLE; try reconnecting next client.");
            synchronized (this) {
                this.mDaemon = null;
                this.mHalDeviceId = 0;
                this.mCurrentUserId = -10000;
            }
        }
    }

    protected void handleRemoved(long deviceId, int fingerId, int groupId, int remaining) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Removed: fid=");
        stringBuilder.append(fingerId);
        stringBuilder.append(", gid=");
        stringBuilder.append(groupId);
        stringBuilder.append(", dev=");
        stringBuilder.append(deviceId);
        stringBuilder.append(", rem=");
        stringBuilder.append(remaining);
        Slog.w(str, stringBuilder.toString());
        ClientMonitor client = this.mCurrentClient;
        groupId = getRealUserIdForApp(groupId);
        if (client != null && client.onRemoved(fingerId, groupId, remaining)) {
            if (this.mFingerprintUtils.isDualFp() && (client instanceof RemovalClient)) {
                RemovalClient removeClient = (RemovalClient) client;
                boolean hasUDFingerprints = true;
                boolean hasFingerprints = this.mFingerprintUtils.getFingerprintsForUser(this.mContext, groupId, -1).size() > 0;
                if (this.mFingerprintUtils.getFingerprintsForUser(this.mContext, groupId, 1).size() <= 0) {
                    hasUDFingerprints = false;
                }
                if (!hasUDFingerprints) {
                    sendCommandToHal(0);
                    Slog.d(TAG, "UDFingerprint all removed so TP CLOSE");
                }
                if (removeClient.getFingerId() == 0 && hasFingerprints) {
                    Slog.d(TAG, "dualFingerprint-> handleRemoved, but do not destory client.");
                    return;
                }
            }
            removeClient(client);
            if (!hasEnrolledFingerprints(groupId)) {
                updateActiveGroup(groupId, null);
            }
        }
        if ((client instanceof InternalRemovalClient) && !this.mUnknownFingerprints.isEmpty()) {
            cleanupUnknownFingerprints();
        } else if (client instanceof InternalRemovalClient) {
            clearEnumerateState();
        }
    }

    protected void handleAuthenticated(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
        ClientMonitor client = this.mCurrentClient;
        groupId = getRealUserIdForApp(groupId);
        if (fingerId != 0) {
            byte[] byteToken = new byte[token.size()];
            for (int i = 0; i < token.size(); i++) {
                byteToken[i] = ((Byte) token.get(i)).byteValue();
            }
            KeyStore.getInstance().addAuthToken(byteToken);
        }
        if (client != null && client.onAuthenticated(fingerId, groupId)) {
            if ((((AuthenticationClient) client).mFlags & 4096) != 0) {
                Slog.w(TAG, "AuthenticationClient remvoe with waitup");
                this.mWaitupClient = client;
            }
            removeClient(client);
        }
        if (this.mPerformanceStats == null) {
            Slog.w(TAG, "mPerformanceStats is null");
            return;
        }
        PerformanceStats performanceStats;
        if (fingerId != 0) {
            performanceStats = this.mPerformanceStats;
            performanceStats.accept++;
        } else {
            performanceStats = this.mPerformanceStats;
            performanceStats.reject++;
        }
    }

    protected void handleAcquired(long deviceId, int acquiredInfo, int vendorCode) {
        String str;
        StringBuilder stringBuilder;
        stopPickupTrunOff();
        String currentOpName = null;
        if (this.mWaitupClient != null) {
            int clientCode = acquiredInfo == 6 ? vendorCode + 1000 : acquiredInfo;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleAcquired for waitClient AC:");
            stringBuilder.append(acquiredInfo);
            stringBuilder.append(" VC:");
            stringBuilder.append(vendorCode);
            stringBuilder.append(" CC:");
            stringBuilder.append(clientCode);
            Slog.w(str, stringBuilder.toString());
            if (clientCode == HUAWEI_FINGERPRINT_UP) {
                this.mWaitupClient = null;
                Slog.w(TAG, "wait clint ActionUp.");
                this.mContext.sendBroadcastAsUser(new Intent(ACTION_AUTH_FINGER_UP), new UserHandle(0), PERM_AUTH_FINGER_UP);
            }
        }
        ClientMonitor client = this.mCurrentClient;
        int hwClientCode = acquiredInfo == 6 ? vendorCode + 1000 : acquiredInfo;
        if (hwClientCode == FINGERPRINT_ACQUIRED_FINGER_DOWN || hwClientCode == HUAWEI_FINGERPRINT_DOWN_UD) {
            if (client != null) {
                currentOpName = client.getOwnerString();
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleAcquired, with currentOpName : ");
            stringBuilder.append(currentOpName);
            stringBuilder.append(" hwClientCode : ");
            stringBuilder.append(hwClientCode);
            Slog.i(str, stringBuilder.toString());
            if (SYSTEMUI_PACKAGE_NAME.equals(currentOpName)) {
                setPowerState(2);
            }
        }
        if (client != null && client.onAcquired(acquiredInfo, vendorCode)) {
            removeClient(client);
        }
        if (this.mPerformanceStats != null && getLockoutMode() == 0 && (client instanceof AuthenticationClient)) {
            PerformanceStats performanceStats = this.mPerformanceStats;
            performanceStats.acquire++;
        }
    }

    protected void handleEnrollResult(long deviceId, int fingerId, int groupId, int remaining) {
        ClientMonitor client = this.mCurrentClient;
        groupId = getRealUserIdForApp(groupId);
        if (client == null || !client.onEnrollResult(fingerId, groupId, remaining)) {
            notifyEnrollingFingerUp();
            Slog.w(TAG, "no eroll client, remove erolled fingerprint");
            if (remaining == 0) {
                IBiometricsFingerprint daemon = getFingerprintDaemon();
                if (daemon != null) {
                    try {
                        daemon.remove(fingerId, ActivityManager.getCurrentUser());
                    } catch (RemoteException e) {
                    }
                } else {
                    return;
                }
            }
        }
        removeClient(client);
        notifyEnrollmentCanceled();
        updateActiveGroup(groupId, null);
    }

    protected int getRealUserIdForApp(int groupId) {
        if (groupId != HIDDEN_SPACE_ID) {
            return groupId;
        }
        for (UserInfo user : this.mUserManager.getUsers(true)) {
            if (user != null && user.isHwHiddenSpace()) {
                return user.id;
            }
        }
        Slog.w(TAG, "getRealUserIdForApp error return 0");
        return 0;
    }

    private void userActivity() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 2, 0);
    }

    void handleUserSwitching(int userId) {
        if ((this.mCurrentClient instanceof InternalRemovalClient) || (this.mCurrentClient instanceof InternalEnumerateClient)) {
            Slog.w(TAG, "User switched while performing cleanup");
            removeClient(this.mCurrentClient);
            clearEnumerateState();
        }
        updateActiveGroup(userId, null);
        doFingerprintCleanupForUser(userId);
    }

    private void removeClient(ClientMonitor client) {
        String str;
        StringBuilder stringBuilder;
        if (client != null) {
            client.destroy();
            if (!(client == this.mCurrentClient || this.mCurrentClient == null)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected client: ");
                stringBuilder.append(client.getOwnerString());
                stringBuilder.append("expected: ");
                stringBuilder.append(this.mCurrentClient);
                Slog.w(str, stringBuilder.toString() != null ? this.mCurrentClient.getOwnerString() : "null");
            }
        }
        if (this.mCurrentClient != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Done with client: ");
            stringBuilder.append(client.getOwnerString());
            Slog.v(str, stringBuilder.toString());
            this.mCurrentClient = null;
        }
        if (this.mPendingClient == null) {
            notifyClientActiveCallbacks(false);
        }
    }

    private boolean inLockoutMode() {
        boolean z = false;
        int failedAttempts = this.mFailedAttempts.get(ActivityManager.getCurrentUser(), 0);
        if (this.mHwCust != null && this.mHwCust.isAtt()) {
            return this.mHwCust.inLockoutMode(failedAttempts, this.mContext);
        }
        if (failedAttempts >= 5) {
            z = true;
        }
        return z;
    }

    protected int getLockoutMode() {
        int currentUser = ActivityManager.getCurrentUser();
        int failedAttempts = this.mFailedAttempts.get(currentUser, 0);
        if (failedAttempts >= 20) {
            return 2;
        }
        if (failedAttempts > 0 && !this.mTimedLockoutCleared.get(currentUser, false)) {
            if (this.mHwCust == null || !this.mHwCust.isAtt()) {
                if (failedAttempts % 5 == 0) {
                    return 1;
                }
            } else if (this.mHwCust.isLockoutMode(failedAttempts, this.mContext)) {
                return 1;
            }
        }
        return 0;
    }

    private void scheduleLockoutResetForUser(int userId) {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + 30000, getLockoutResetIntentForUser(userId));
    }

    private void cancelLockoutResetForUser(int userId) {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }
        this.mAlarmManager.cancel(getLockoutResetIntentForUser(userId));
    }

    private PendingIntent getLockoutResetIntentForUser(int userId) {
        return PendingIntent.getBroadcast(this.mContext, userId, new Intent(ACTION_LOCKOUT_RESET).putExtra(KEY_LOCKOUT_RESET_USER, userId), HW_FP_AUTH_UD);
    }

    protected void handleHwFailedAttempt(int flags, String packagesName) {
        if ((16777216 & flags) == 0 || !SETTINGS_PACKAGE_NAME.equals(packagesName)) {
            this.mHwFailedAttempts++;
        } else {
            Slog.i(TAG, "no need count hw failed attempts");
        }
    }

    public long startPreEnroll(IBinder token) {
        IBiometricsFingerprint daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w(TAG, "startPreEnroll: no fingerprint HAL!");
            return 0;
        }
        try {
            return daemon.preEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "startPreEnroll failed", e);
            return 0;
        }
    }

    public int startPostEnroll(IBinder token) {
        IBiometricsFingerprint daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w(TAG, "startPostEnroll: no fingerprint HAL!");
            return 0;
        }
        try {
            return daemon.postEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "startPostEnroll failed", e);
            return 0;
        }
    }

    protected void setLivenessSwitch(String opPackageName) {
        Slog.w(TAG, "father class call setLivenessSwitch");
    }

    private void startClient(ClientMonitor newClient, boolean initiatedByClient) {
        ClientMonitor currentClient = this.mCurrentClient;
        this.mHandler.removeCallbacks(this.mResetClientState);
        String str;
        StringBuilder stringBuilder;
        if (currentClient != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("request stop current client ");
            stringBuilder.append(currentClient.getOwnerString());
            Slog.v(str, stringBuilder.toString());
            if (!(currentClient instanceof InternalEnumerateClient) && !(currentClient instanceof InternalRemovalClient)) {
                currentClient.stop(initiatedByClient);
                if (this.mPendingClient != null) {
                    this.mPendingClient.destroy();
                }
            } else if (newClient != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Internal cleanup in progress but trying to start client ");
                stringBuilder.append(newClient.getClass().getSuperclass().getSimpleName());
                stringBuilder.append("(");
                stringBuilder.append(newClient.getOwnerString());
                stringBuilder.append("), initiatedByClient = ");
                stringBuilder.append(initiatedByClient);
                Slog.w(str, stringBuilder.toString());
            }
            this.mPendingClient = newClient;
            this.mHandler.removeCallbacks(this.mResetClientState);
            this.mHandler.postDelayed(this.mResetClientState, CANCEL_TIMEOUT_LIMIT);
        } else if (newClient != null) {
            this.mCurrentClient = newClient;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("starting client ");
            stringBuilder.append(newClient.getClass().getSuperclass().getSimpleName());
            stringBuilder.append("(");
            stringBuilder.append(newClient.getOwnerString());
            stringBuilder.append("), initiatedByClient = ");
            stringBuilder.append(initiatedByClient);
            Slog.v(str, stringBuilder.toString());
            notifyClientActiveCallbacks(true);
            newClient.start();
        }
    }

    void startRemove(IBinder token, int fingerId, int groupId, int userId, IFingerprintServiceReceiver receiver, boolean restricted, boolean internal) {
        int i = fingerId;
        if (token == null) {
            Slog.w(TAG, "startRemove: token is null");
        } else if (receiver == null) {
            Slog.w(TAG, "startRemove: receiver is null");
        } else if (getFingerprintDaemon() == null) {
            Slog.w(TAG, "startRemove: no fingerprint HAL!");
        } else {
            int i2;
            if (!this.mFingerprintUtils.isDualFp() || i == 0) {
                i2 = userId;
            } else {
                i2 = userId;
                List<Fingerprint> finerprints = FingerprintUtils.getInstance().getFingerprintsForUser(getContext(), i2, 1);
                int fingerprintSize = finerprints.size();
                for (int i3 = 0; i3 < fingerprintSize; i3++) {
                    if (((Fingerprint) finerprints.get(i3)).getFingerId() == i) {
                        Slog.d(TAG, "dualFingerprint send MSG_REMOVE_UD");
                        sendCommandToHal(104);
                        break;
                    }
                }
            }
            if (this.mFingerprintUtils.isDualFp() && i == 0) {
                Slog.d(TAG, "dualFingerprint send MSG_REMOVE_ALL");
                sendCommandToHal(107);
            }
            boolean z;
            if (internal) {
                Context context = getContext();
                z = true;
                startClient(new InternalRemovalClient(this, context, this.mHalDeviceId, token, receiver, i, groupId, i2, restricted, context.getOpPackageName()) {
                    final /* synthetic */ FingerprintService this$0;

                    public void notifyUserActivity() {
                    }

                    public IBiometricsFingerprint getFingerprintDaemon() {
                        return this.this$0.getFingerprintDaemon();
                    }
                }, z);
            } else {
                z = true;
                if (token == null) {
                    Slog.e(TAG, "startRemove error: token null!");
                    return;
                }
                startClient(new RemovalClient(this, getContext(), this.mHalDeviceId, token, receiver, fingerId, groupId, userId, restricted, token.toString()) {
                    final /* synthetic */ FingerprintService this$0;

                    public void notifyUserActivity() {
                        this.this$0.userActivity();
                    }

                    public IBiometricsFingerprint getFingerprintDaemon() {
                        return this.this$0.getFingerprintDaemon();
                    }
                }, z);
            }
        }
    }

    void startEnumerate(IBinder token, int userId, IFingerprintServiceReceiver receiver, boolean restricted, boolean internal) {
        if (getFingerprintDaemon() == null) {
            Slog.w(TAG, "startEnumerate: no fingerprint HAL!");
            return;
        }
        if (internal) {
            int i = userId;
            List<Fingerprint> enrolledList = getEnrolledFingerprints(i);
            Context context = getContext();
            startClient(new InternalEnumerateClient(this, context, this.mHalDeviceId, token, receiver, i, i, restricted, context.getOpPackageName(), enrolledList) {
                final /* synthetic */ FingerprintService this$0;

                public void notifyUserActivity() {
                }

                public IBiometricsFingerprint getFingerprintDaemon() {
                    return this.this$0.getFingerprintDaemon();
                }
            }, true);
        } else if (token == null) {
            Slog.e(TAG, "startEnumerate error: token null!");
        } else {
            startClient(new EnumerateClient(this, getContext(), this.mHalDeviceId, token, receiver, userId, userId, restricted, token.toString()) {
                final /* synthetic */ FingerprintService this$0;

                public void notifyUserActivity() {
                    this.this$0.userActivity();
                }

                public IBiometricsFingerprint getFingerprintDaemon() {
                    return this.this$0.getFingerprintDaemon();
                }
            }, true);
        }
    }

    public List<Fingerprint> getEnrolledFingerprints(int userId) {
        return this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId);
    }

    public boolean hasEnrolledFingerprints(int userId) {
        if (userId != UserHandle.getCallingUserId()) {
            checkPermission("android.permission.INTERACT_ACROSS_USERS");
        }
        if (userId != 0 && isClonedProfile(userId)) {
            Log.i(TAG, "Clone profile get Enrolled Fingerprints,change userid to 0");
            userId = 0;
        }
        return this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId).size() > 0;
    }

    boolean hasPermission(String permission) {
        return getContext().checkCallingOrSelfPermission(permission) == 0;
    }

    void checkPermission(String permission) {
        Context context = getContext();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Must have ");
        stringBuilder.append(permission);
        stringBuilder.append(" permission.");
        context.enforceCallingOrSelfPermission(permission, stringBuilder.toString());
    }

    int getEffectiveUserId(int userId) {
        UserManager um = UserManager.get(this.mContext);
        if (um != null) {
            long callingIdentity = Binder.clearCallingIdentity();
            userId = um.getCredentialOwnerProfile(userId);
            Binder.restoreCallingIdentity(callingIdentity);
            return userId;
        }
        Slog.e(TAG, "Unable to acquire UserManager");
        return userId;
    }

    boolean isCurrentUserOrProfile(int userId) {
        UserManager um = UserManager.get(this.mContext);
        if (um == null) {
            Slog.e(TAG, "Unable to acquire UserManager");
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            for (int profileId : um.getEnabledProfileIds(ActivityManager.getCurrentUser())) {
                if (profileId == userId) {
                    return true;
                }
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isForegroundActivity(int uid, int pid) {
        try {
            List<RunningAppProcessInfo> procs = ActivityManager.getService().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                RunningAppProcessInfo proc = (RunningAppProcessInfo) procs.get(i);
                if (proc.pid == pid && proc.uid == uid && proc.importance <= 125) {
                    return true;
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
        }
        return false;
    }

    private boolean canUseFingerprint(String opPackageName, boolean requireForeground, int uid, int pid, int userId) {
        return canUseFingerprint(opPackageName, requireForeground, uid, pid, userId, false);
    }

    private boolean canUseFingerprint(String opPackageName, boolean requireForeground, int uid, int pid, int userId, boolean isDetected) {
        if (opPackageName == null || opPackageName.equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) {
            Slog.i(TAG, "opPackageName is null or opPackageName is invalid");
            return false;
        }
        if (getContext().checkCallingPermission("android.permission.USE_FINGERPRINT") != 0) {
            checkPermission("android.permission.USE_BIOMETRIC");
        }
        this.opPackageName = opPackageName;
        if (opPackageName != null && (opPackageName.equals("com.huawei.hwasm") || opPackageName.equals("com.huawei.securitymgr") || opPackageName.equals("com.huawei.aod") || ((isDetected && opPackageName.equals("com.tencent.mm")) || isKeyguard(opPackageName)))) {
            return true;
        }
        String str;
        if (isCurrentUserOrProfile(userId)) {
            try {
                StringBuilder stringBuilder;
                if (this.mAppOps.noteOp(55, uid, opPackageName) != 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Rejecting ");
                    stringBuilder.append(opPackageName);
                    stringBuilder.append(" ; permission denied");
                    Slog.w(str, stringBuilder.toString());
                    return false;
                } else if (!requireForeground || isForegroundActivity(uid, pid) || currentClient(opPackageName)) {
                    return true;
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Rejecting ");
                    stringBuilder.append(opPackageName);
                    stringBuilder.append(" ; not in foreground");
                    Slog.w(str, stringBuilder.toString());
                    return false;
                }
            } catch (SecurityException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AppOpsManager noteOp error:");
                stringBuilder2.append(e);
                Slog.w(str2, stringBuilder2.toString());
                return false;
            }
        }
        str = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Rejecting ");
        stringBuilder3.append(opPackageName);
        stringBuilder3.append(" ; not a current user or profile");
        Slog.w(str, stringBuilder3.toString());
        return false;
    }

    private boolean currentClient(String opPackageName) {
        return this.mCurrentClient != null && this.mCurrentClient.getOwnerString().equals(opPackageName);
    }

    private boolean isKeyguard(String clientPackage) {
        return this.mKeyguardPackage.equals(clientPackage);
    }

    private void addLockoutResetMonitor(FingerprintServiceLockoutResetMonitor monitor) {
        if (!this.mLockoutMonitors.contains(monitor)) {
            this.mLockoutMonitors.add(monitor);
        }
    }

    private void removeLockoutResetCallback(FingerprintServiceLockoutResetMonitor monitor) {
        this.mLockoutMonitors.remove(monitor);
    }

    private void notifyLockoutResetMonitors() {
        for (int i = 0; i < this.mLockoutMonitors.size(); i++) {
            ((FingerprintServiceLockoutResetMonitor) this.mLockoutMonitors.get(i)).sendLockoutReset();
        }
    }

    private void notifyClientActiveCallbacks(boolean isActive) {
        List<IFingerprintClientActiveCallback> callbacks = this.mClientActiveCallbacks;
        for (int i = 0; i < callbacks.size(); i++) {
            try {
                ((IFingerprintClientActiveCallback) callbacks.get(i)).onClientActiveChanged(isActive);
            } catch (RemoteException e) {
                this.mClientActiveCallbacks.remove(callbacks.get(i));
            }
        }
    }

    private void startAuthentication(IBinder token, long opId, int callingUserId, int groupId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName, Bundle bundle, IBiometricPromptReceiver dialogReceiver) {
        FingerprintService fingerprintService;
        String str;
        String str2;
        StringBuilder stringBuilder;
        int i = flags;
        String str3 = opPackageName;
        int newGroupId = groupId;
        updateActiveGroup(groupId, str3);
        String str4 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("startAuthentication(");
        stringBuilder2.append(str3);
        stringBuilder2.append(")");
        Slog.v(str4, stringBuilder2.toString());
        if (shouldAuthBothSpaceFingerprints(str3, i)) {
            Slog.i(TAG, "should authenticate both space fingerprints");
            newGroupId = SPECIAL_USER_ID;
        }
        int newGroupId2 = newGroupId;
        final String str5 = opPackageName;
        AuthenticationClient client = new AuthenticationClient(this, getContext(), this.mHalDeviceId, token, receiver, this.mCurrentUserId, newGroupId2, opId, restricted, str3, i, bundle, dialogReceiver, this.mStatusBarService) {
            final /* synthetic */ FingerprintService this$0;

            public boolean onAuthenticated(int fingerId, int groupId) {
                IFingerprintServiceReceiver receiver = getReceiver();
                boolean authenticated = fingerId != 0;
                if (receiver != null) {
                    Context access$800;
                    StringBuilder stringBuilder;
                    String str;
                    StringBuilder stringBuilder2;
                    if (authenticated) {
                        Log.e(FingerprintService.TAG, "onAuthenticated, pass");
                        this.this$0.notifyAuthenticationFinished(str5, 0, 0);
                        access$800 = this.this$0.mContext;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("{pkg:");
                        stringBuilder.append(str5);
                        stringBuilder.append(",ErrorCount:");
                        stringBuilder.append(this.this$0.mHwFailedAttempts);
                        stringBuilder.append(",DeviceType:");
                        stringBuilder.append(this.this$0.mCurrentAuthFpDev);
                        stringBuilder.append("}");
                        Flog.bdReport(access$800, 8, stringBuilder.toString());
                        str = FingerprintService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onAuthenticated success:{pkg:");
                        stringBuilder2.append(str5);
                        stringBuilder2.append(",ErrorCount:");
                        stringBuilder2.append(this.this$0.mHwFailedAttempts);
                        stringBuilder2.append(",DeviceType:");
                        stringBuilder2.append(this.this$0.mCurrentAuthFpDev);
                        stringBuilder2.append("}");
                        Log.i(str, stringBuilder2.toString());
                    } else {
                        str = FingerprintService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("onAuthenticated,fail ,mHwFailedAttempts = ");
                        stringBuilder.append(this.this$0.mHwFailedAttempts);
                        Log.e(str, stringBuilder.toString());
                        this.this$0.notifyAuthenticationFinished(str5, 1, this.this$0.mHwFailedAttempts + 1);
                        if (this.this$0.auTime - this.this$0.downTime > 0) {
                            access$800 = this.this$0.mContext;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("{CostTime:");
                            stringBuilder.append(this.this$0.auTime - this.this$0.downTime);
                            stringBuilder.append(",pkg:");
                            stringBuilder.append(str5);
                            stringBuilder.append(",DeviceType:");
                            stringBuilder.append(this.this$0.mCurrentAuthFpDev);
                            stringBuilder.append("}");
                            Flog.bdReport(access$800, 7, stringBuilder.toString());
                            str = FingerprintService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("onAuthenticated fail:{CostTime:");
                            stringBuilder2.append(this.this$0.auTime - this.this$0.downTime);
                            stringBuilder2.append(",pkg:");
                            stringBuilder2.append(str5);
                            stringBuilder2.append(",DeviceType:");
                            stringBuilder2.append(this.this$0.mCurrentAuthFpDev);
                            stringBuilder2.append("}");
                            Log.i(str, stringBuilder2.toString());
                        } else {
                            Log.i(FingerprintService.TAG, "Fingerprint authenticate time less than equal to or equal to Fingerprint down time");
                        }
                    }
                }
                return super.onAuthenticated(fingerId, groupId);
            }

            public void onStart() {
                try {
                    this.this$0.mActivityManager.registerTaskStackListener(this.this$0.mTaskStackListener);
                } catch (RemoteException e) {
                    Slog.e(FingerprintService.TAG, "Could not register task stack listener", e);
                }
            }

            public void onStop() {
                try {
                    this.this$0.mActivityManager.unregisterTaskStackListener(this.this$0.mTaskStackListener);
                } catch (RemoteException e) {
                    Slog.e(FingerprintService.TAG, "Could not unregister task stack listener", e);
                }
            }

            public int handleFailedAttempt() {
                boolean noNeedAddFailedAttemps = false;
                if ((this.mFlags & 16777216) != 0 && FingerprintService.SETTINGS_PACKAGE_NAME.equals(getOwnerString())) {
                    noNeedAddFailedAttemps = true;
                    Slog.i(FingerprintService.TAG, "no need count failed attempts");
                }
                int currentUser = ActivityManager.getCurrentUser();
                if (!noNeedAddFailedAttemps) {
                    this.this$0.mFailedAttempts.put(currentUser, this.this$0.mFailedAttempts.get(currentUser, 0) + 1);
                }
                this.this$0.mTimedLockoutCleared.put(ActivityManager.getCurrentUser(), false);
                int lockoutMode = this.this$0.getLockoutMode();
                PerformanceStats access$1300;
                if (lockoutMode == 2) {
                    access$1300 = this.this$0.mPerformanceStats;
                    access$1300.permanentLockout++;
                } else if (lockoutMode == 1) {
                    access$1300 = this.this$0.mPerformanceStats;
                    access$1300.lockout++;
                }
                if (lockoutMode == 0) {
                    return 0;
                }
                this.this$0.mLockoutTime = SystemClock.elapsedRealtime();
                this.this$0.scheduleLockoutResetForUser(currentUser);
                if (this.this$0.isKeyguard(str5)) {
                    return 0;
                }
                return lockoutMode;
            }

            public void resetFailedAttempts() {
                if (inLockoutMode()) {
                    Slog.v(FingerprintService.TAG, "resetFailedAttempts should be called from APP");
                } else {
                    this.this$0.resetFailedAttemptsForUser(true, ActivityManager.getCurrentUser());
                }
            }

            public void notifyUserActivity() {
                this.this$0.userActivity();
            }

            public IBiometricsFingerprint getFingerprintDaemon() {
                return this.this$0.getFingerprintDaemon();
            }

            public void handleHwFailedAttempt(int flags, String packagesName) {
                this.this$0.handleHwFailedAttempt(flags, packagesName);
            }

            public boolean inLockoutMode() {
                return this.this$0.inLockoutMode();
            }
        };
        int lockoutMode = getLockoutMode();
        if (lockoutMode != 0) {
            fingerprintService = this;
            str = opPackageName;
            if (!fingerprintService.isKeyguard(str)) {
                int errorCode;
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("In lockout mode(");
                stringBuilder.append(lockoutMode);
                stringBuilder.append(") ; disallowing authentication");
                Slog.v(str2, stringBuilder.toString());
                if (lockoutMode == 1) {
                    errorCode = 7;
                } else {
                    errorCode = 9;
                }
                if (!client.onError(errorCode, 0)) {
                    Slog.w(TAG, "Cannot send permanent lockout message to client");
                }
                return;
            }
        }
        fingerprintService = this;
        str = opPackageName;
        int i2;
        if (fingerprintService.mFingerprintUtils.isDualFp()) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("dualFingerprint startAuthentication and flag is: ");
            i2 = flags;
            stringBuilder.append(i2);
            Slog.d(str2, stringBuilder.toString());
            if (i2 == 0) {
                if (fingerprintService.canUseUdFingerprint(str)) {
                    Slog.d(TAG, "dualFingerprint send MSG_AUTH_ALL");
                    fingerprintService.sendCommandToHal(103);
                }
            } else if ((HW_FP_AUTH_UD & i2) != 0) {
                if ((HW_FP_AUTH_UG & i2) != 0) {
                    Slog.d(TAG, "dualFingerprint send MSG_AUTH_ALL");
                    fingerprintService.sendCommandToHal(103);
                } else {
                    Slog.d(TAG, "dualFingerprint send MSG_AUTH_UD");
                    fingerprintService.sendCommandToHal(102);
                }
            }
        } else {
            i2 = flags;
        }
        fingerprintService.startClient(client, true);
    }

    private void startEnrollment(IBinder token, byte[] cryptoToken, int userId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName) {
        int i = flags;
        String str = opPackageName;
        int i2 = userId;
        updateActiveGroup(i2, str);
        EnrollClient client = new EnrollClient(this, getContext(), this.mHalDeviceId, token, receiver, i2, i2, cryptoToken, restricted, str) {
            final /* synthetic */ FingerprintService this$0;

            public IBiometricsFingerprint getFingerprintDaemon() {
                return this.this$0.getFingerprintDaemon();
            }

            public void notifyUserActivity() {
                this.this$0.userActivity();
            }
        };
        if (this.mFingerprintUtils.isDualFp() && SETTINGS_PACKAGE_NAME.equals(str)) {
            int targetDevice = i == 4096 ? 1 : 0;
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dualFingerprint enroll targetDevice is: ");
            stringBuilder.append(targetDevice);
            Slog.d(str2, stringBuilder.toString());
            if (targetDevice == 1) {
                Slog.d(TAG, "dualFingerprint send MSG_ENROLL_UD");
                sendCommandToHal(101);
                client.setTargetDevice(1);
            }
        }
        notifyEnrollmentStarted(i);
        startClient(client, true);
    }

    protected void resetFailedAttemptsForUser(boolean clearAttemptCounter, int userId) {
        if (getLockoutMode() != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reset fingerprint lockout, clearAttemptCounter=");
            stringBuilder.append(clearAttemptCounter);
            Slog.v(str, stringBuilder.toString());
        }
        if (clearAttemptCounter) {
            this.mFailedAttempts.put(userId, 0);
        }
        this.mTimedLockoutCleared.put(userId, true);
        cancelLockoutResetForUser(userId);
        notifyLockoutResetMonitors();
        this.mLockoutTime = 0;
        this.mHwFailedAttempts = 0;
    }

    private boolean isHwTransactInterest(int code) {
        if (code == CODE_IS_FP_NEED_CALIBRATE_RULE || code == CODE_SET_CALIBRATE_MODE_RULE || code == CODE_GET_TOKEN_LEN_RULE || code == CODE_SET_FINGERPRINT_MASK_VIEW || code == CODE_SHOW_FINGERPRINT_VIEW || code == CODE_SHOW_FINGERPRINT_BUTTON || code == 1107 || code == 1110 || code == CODE_GET_HARDWARE_TYPE || code == CODE_NOTIFY_OPTICAL_CAPTURE || code == CODE_SUSPEND_AUTHENTICATE || code == CODE_SET_HOVER_SWITCH || code == CODE_GET_HOVER_SUPPORT || code == CODE_DISABLE_FINGERPRINT_VIEW || code == CODE_ENABLE_FINGERPRINT_VIEW || code == CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION || code == CODE_REMOVE_MASK_AND_SHOW_BUTTON || code == CODE_IS_SUPPORT_DUAL_FINGERPRINT || code == CODE_GET_FINGERPRINT_LIST_ENROLLED || code == CODE_IS_FINGERPRINT_HARDWARE_DETECTED || code == CODE_SEND_UNLOCK_LIGHTBRIGHT || code == CODE_GET_HIGHLIGHT_SPOT_RADIUS || code == CODE_SUSPEND_ENROLL) {
            return true;
        }
        return false;
    }

    private void dumpInternal(PrintWriter pw) {
        JSONObject dump = new JSONObject();
        try {
            dump.put("service", "Fingerprint Manager");
            JSONArray sets = new JSONArray();
            for (UserInfo user : UserManager.get(getContext()).getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                int N = this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId).size();
                PerformanceStats stats = (PerformanceStats) this.mPerformanceMap.get(Integer.valueOf(userId));
                PerformanceStats cryptoStats = (PerformanceStats) this.mCryptoPerformanceMap.get(Integer.valueOf(userId));
                JSONObject set = new JSONObject();
                set.put("id", userId);
                set.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, N);
                int i = 0;
                set.put("accept", stats != null ? stats.accept : 0);
                set.put("reject", stats != null ? stats.reject : 0);
                set.put("acquire", stats != null ? stats.acquire : 0);
                set.put("lockout", stats != null ? stats.lockout : 0);
                set.put("permanentLockout", stats != null ? stats.permanentLockout : 0);
                set.put("acceptCrypto", cryptoStats != null ? cryptoStats.accept : 0);
                set.put("rejectCrypto", cryptoStats != null ? cryptoStats.reject : 0);
                set.put("acquireCrypto", cryptoStats != null ? cryptoStats.acquire : 0);
                set.put("lockoutCrypto", cryptoStats != null ? cryptoStats.lockout : 0);
                String str = "permanentLockoutCrypto";
                if (cryptoStats != null) {
                    i = cryptoStats.permanentLockout;
                }
                set.put(str, i);
                sets.put(set);
            }
            dump.put("prints", sets);
        } catch (JSONException e) {
            Slog.e(TAG, "dump formatting failure", e);
        }
        pw.println(dump);
    }

    private void dumpProto(FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        for (UserInfo user : UserManager.get(getContext()).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            long userToken = proto.start(2246267895809L);
            proto.write(1120986464257L, userId);
            proto.write(1120986464258L, this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId).size());
            PerformanceStats normal = (PerformanceStats) this.mPerformanceMap.get(Integer.valueOf(userId));
            if (normal != null) {
                long countsToken = proto.start(1146756268035L);
                proto.write(1120986464257L, normal.accept);
                proto.write(1120986464258L, normal.reject);
                proto.write(1120986464259L, normal.acquire);
                proto.write(1120986464260L, normal.lockout);
                proto.write(1120986464261L, normal.permanentLockout);
                proto.end(countsToken);
            }
            PerformanceStats crypto = (PerformanceStats) this.mCryptoPerformanceMap.get(Integer.valueOf(userId));
            if (crypto != null) {
                long countsToken2 = proto.start(1146756268036L);
                proto.write(1120986464257L, crypto.accept);
                proto.write(1120986464258L, crypto.reject);
                proto.write(1120986464259L, crypto.acquire);
                proto.write(1120986464260L, crypto.lockout);
                proto.write(1120986464261L, crypto.permanentLockout);
                proto.end(countsToken2);
            }
            proto.end(userToken);
        }
        proto.flush();
        this.mPerformanceMap.clear();
        this.mCryptoPerformanceMap.clear();
    }

    public void onStart() {
        publishBinderService("fingerprint", new FingerprintServiceWrapper(this, null));
        SystemServerInitThreadPool.get().submit(new -$$Lambda$l42rkDmfSgEoarEM7da3vinr3Iw(this), "FingerprintService.onStart");
        listenForUserSwitches();
    }

    public void onBootPhase(int phase) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Fingerprint daemon is phase :");
        stringBuilder.append(phase);
        Slog.d(str, stringBuilder.toString());
        if (phase == 1000) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Fingerprint mDaemon is ");
            stringBuilder.append(this.mDaemon);
            Slog.d(str, stringBuilder.toString());
            if (getFingerprintDaemon() == null) {
                Slog.w(TAG, "Fingerprint daemon is null");
            }
        }
    }

    private void updateActiveGroup(int userId, String clientPackage) {
        IBiometricsFingerprint daemon = getFingerprintDaemon();
        if (daemon != null) {
            try {
                String str;
                StringBuilder stringBuilder;
                userId = getUserOrWorkProfileId(clientPackage, userId);
                boolean z = false;
                if (userId != this.mCurrentUserId) {
                    String str2;
                    StringBuilder stringBuilder2;
                    File baseDir;
                    int firstSdkInt = VERSION.FIRST_SDK_INT;
                    if (firstSdkInt < 1) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("First SDK version ");
                        stringBuilder.append(firstSdkInt);
                        stringBuilder.append(" is invalid; must be at least VERSION_CODES.BASE");
                        Slog.e(str, stringBuilder.toString());
                    }
                    int userIdForHal = userId;
                    UserInfo info = this.mUserManager.getUserInfo(userId);
                    if (info != null && info.isHwHiddenSpace()) {
                        userIdForHal = HIDDEN_SPACE_ID;
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("userIdForHal is ");
                        stringBuilder2.append(HIDDEN_SPACE_ID);
                        Slog.i(str2, stringBuilder2.toString());
                    }
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("FIRST_SDK_INT:");
                    stringBuilder2.append(firstSdkInt);
                    Slog.i(str2, stringBuilder2.toString());
                    if (userIdForHal == HIDDEN_SPACE_ID) {
                        Slog.i(TAG, "userIdForHal == HIDDEN_SPACE_ID");
                        baseDir = Environment.getFingerprintFileDirectory(0);
                    } else {
                        baseDir = Environment.getFingerprintFileDirectory(userId);
                    }
                    File fpDir = new File(baseDir, FP_DATA_DIR);
                    if (!fpDir.exists()) {
                        if (!fpDir.mkdir()) {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Cannot make directory: ");
                            stringBuilder3.append(fpDir.getAbsolutePath());
                            Slog.v(str3, stringBuilder3.toString());
                            return;
                        } else if (!SELinux.restorecon(fpDir)) {
                            Slog.w(TAG, "Restorecons failed. Directory will have wrong label.");
                            return;
                        }
                    }
                    daemon.setActiveGroup(userIdForHal, fpDir.getAbsolutePath());
                    this.mCurrentUserId = userId;
                    updateFingerprints(userId);
                }
                long j = 0;
                if (this.mFingerprintUtils.isDualFp()) {
                    if (this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId, -1).size() > 0) {
                        z = true;
                    }
                    if (z) {
                        j = daemon.getAuthenticatorId();
                    }
                    long authenticatorId = j;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("daemon getAuthenticatorId = ");
                    stringBuilder.append(authenticatorId);
                    stringBuilder.append(" userId = ");
                    stringBuilder.append(userId);
                    Slog.d(str, stringBuilder.toString());
                    this.mAuthenticatorIds.put(Integer.valueOf(userId), Long.valueOf(authenticatorId));
                } else {
                    Map map = this.mAuthenticatorIds;
                    Integer valueOf = Integer.valueOf(userId);
                    if (hasEnrolledFingerprints(userId)) {
                        j = daemon.getAuthenticatorId();
                    }
                    map.put(valueOf, Long.valueOf(j));
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to setActiveGroup():", e);
            }
        }
    }

    private int getUserOrWorkProfileId(String clientPackage, int userId) {
        if (isKeyguard(clientPackage) || !isWorkProfile(userId)) {
            return getEffectiveUserId(userId);
        }
        return userId;
    }

    private boolean isWorkProfile(int userId) {
        UserInfo userInfo = null;
        long token = Binder.clearCallingIdentity();
        try {
            userInfo = this.mUserManager.getUserInfo(userId);
            return userInfo != null && userInfo.isManagedProfile();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isClonedProfile(int userId) {
        UserInfo userInfo = null;
        long token = Binder.clearCallingIdentity();
        try {
            userInfo = this.mUserManager.getUserInfo(userId);
            return userInfo != null && userInfo.isClonedProfile();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void listenForUserSwitches() {
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                public void onUserSwitching(int newUserId) throws RemoteException {
                    Slog.w(FingerprintService.TAG, "onUserSwitching");
                    FingerprintService.this.mHandler.obtainMessage(10, newUserId, 0).sendToTarget();
                }
            }, TAG);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to listen for user switching event", e);
        }
    }

    public long getAuthenticatorId(String opPackageName) {
        return ((Long) this.mAuthenticatorIds.getOrDefault(Integer.valueOf(getUserOrWorkProfileId(opPackageName, UserHandle.getCallingUserId())), Long.valueOf(0))).longValue();
    }
}
