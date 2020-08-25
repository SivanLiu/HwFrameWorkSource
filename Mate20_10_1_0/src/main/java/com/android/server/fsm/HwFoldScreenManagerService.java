package com.android.server.fsm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.CoordinationModeUtils;
import android.util.Flog;
import android.util.HwLog;
import android.util.IMonitor;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import com.huawei.android.fsm.IFoldDisplayModeListener;
import com.huawei.android.fsm.IFoldFsmTipsRequestListener;
import com.huawei.android.fsm.IFoldableStateListener;
import com.huawei.android.fsm.IHwFoldScreenManager;
import com.huawei.android.pgmng.plug.PowerKit;
import huawei.android.hardware.tp.HwTpManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public final class HwFoldScreenManagerService extends SystemService implements Watchdog.Monitor {
    private static final String ACTION_SMART_NOTIFY_FAULT = "huawei.intent.action.SMART_NOTIFY_FAULT";
    private static final String ACTION_THERMAL_LOW_TEMP_WARNING = "huawei.intent.action.THERMAL_LOW_TEMP_WARNING";
    private static final int BAT0_TEMP_FLAG = 33;
    private static final int CAMERA_STATE_OFF = 2;
    private static final int CAMERA_STATE_ON = 1;
    private static final int ENTER_LOW_TEMP = 1;
    private static final String FRONT_CAMERA = "1";
    private static final int FSM_FINGERPRINT_NOTIFY_SLEEP_CMD = 5;
    private static final int FSM_FORCE_WAKEUP_CMD = 3;
    private static final int FSM_FORCE_WAKEUP_TIMEOUT = 1500;
    private static final int FSM_FREEZE_FOLD_ROTATION_CMD = 8;
    private static final int FSM_MAGNETOMETER_TURNOFF_SENSOR_CMD = 4;
    private static final int FSM_MAGNETOMETER_TURNOFF_SENSOR_TIMEOUT = 5000;
    private static final int FSM_NOTIFY_DISPLAYMODE_CHANGE_CMD = 2;
    private static final int FSM_NOTIFY_FOLDSTATE_CHANGE_CMD = 1;
    private static final int FSM_NOTIFY_POSTURE_CHANGE_CMD = 0;
    private static final int FSM_SWITCH_DISP_MODE_FROM_RESUME_CMD = 7;
    private static final int FSM_SWITCH_FOREGROUND_USER_CMD = 6;
    private static final int FSM_UNFREEZE_FOLD_ROTATION_CMD = 9;
    private static final int HALL_THRESHOLD = 1;
    private static final String HW_FOLD_DISPLAY_MODE = "hw_fold_display_mode";
    private static final String HW_FOLD_DISPLAY_MODE_PREPARE = "hw_fold_display_mode_prepare";
    private static final String HW_FOLD_DISPMODE = "persist.sys.foldDispMode";
    private static final String HW_FOLD_SCREEN_COUNTER = "hw_fold_screen_counter";
    private static final String HW_FOLD_SCRREN_STATE = "hw_fold_screen_state";
    private static final int IMONITOR_TEMP_EVENT_ID = 936004016;
    private static final int INTELLIGENT_AWAKEN_ON = 1;
    private static final String KEY_FAULT_CODE = "FAULT_CODE";
    private static final String KEY_FAULT_DESCRIPTION = "FAULT_DESCRIPTION";
    private static final String KEY_FAULT_SUGGESTION = "FAULT_SUGGESTION";
    private static final String KEY_INTELLIGENT_AWAKEN = "intelligent_awaken_enabled";
    private static final int LEAVE_LOW_TEMP = 0;
    private static final String LOW_TEMP_WARNING = "low_temp_warning";
    private static final int MAX_FOLD_CREEN_NUM = 500000;
    private static final int MSG_NOTIFY_INTELLIGENT_MODE_TO_TP = 10;
    private static final int MSG_SET_FOLD_DISPLAY_MODE_FINISHED = 11;
    private static final String PERMISSION_FOLD_SCREEN = "com.huawei.permission.MANAGE_FOLD_SCREEN";
    private static final String PERMISSION_FOLD_SCREEN_PERMISSION = "Requires MANAGE_FOLD_SCREEN permission";
    private static final String PERMISSION_FOLD_SCREEN_PRIVILEGED = "com.huawei.permission.MANAGE_FOLD_SCREEN_PRIVILEGED";
    private static final String REAR_CAMERA = "0";
    private static final String SMART_NOTIFY_FAULT_PERMISSION = "huawei.permission.SMART_NOTIFY_FAULT";
    private static final String TAG = "Fsm_FoldScreenManagerService";
    private static final String THERMAL_RECEIVE_PERMISSION = "com.huawei.thermal.receiverPermission";
    private static final String VALUE_FAULT_CODE = "642003014";
    private static final String VALUE_FAULT_DESCRIPTION = "842003014";
    private static final String VALUE_FAULT_SUGGESTION = "542003014";
    private CameraManager.AvailabilityCallback mCameraAvailableCallback = new CameraManager.AvailabilityCallback() {
        /* class com.android.server.fsm.HwFoldScreenManagerService.AnonymousClass1 */

        public void onCameraAvailable(String cameraId) {
            HwFoldScreenManagerService.this.processCameraState(cameraId, 2);
            if ("0".equals(cameraId) || "1".equals(cameraId)) {
                boolean unused = HwFoldScreenManagerService.this.mIsCameraOn = false;
                String unused2 = HwFoldScreenManagerService.this.mCurCameraId = null;
            }
        }

        public void onCameraUnavailable(String cameraId) {
            HwFoldScreenManagerService.this.processCameraState(cameraId, 1);
            if ("0".equals(cameraId) || "1".equals(cameraId)) {
                boolean unused = HwFoldScreenManagerService.this.mIsCameraOn = true;
                String unused2 = HwFoldScreenManagerService.this.mCurCameraId = cameraId;
            }
        }
    };
    private CameraManager mCameraManager;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.fsm.HwFoldScreenManagerService.AnonymousClass2 */

        public void onChange(boolean isSelfChange) {
            int intelligent = Settings.Secure.getIntForUser(HwFoldScreenManagerService.this.mContext.getContentResolver(), HwFoldScreenManagerService.KEY_INTELLIGENT_AWAKEN, 1, -2);
            HwFoldScreenManagerService.this.changeIntelligentMode(intelligent);
            if (intelligent != 1) {
                return;
            }
            if (!HwFoldScreenManagerService.this.isIncall()) {
                PostureStateMachine access$500 = HwFoldScreenManagerService.this.mPostureSM;
                PostureStateMachine unused = HwFoldScreenManagerService.this.mPostureSM;
                access$500.setScreeStateWhenCallComing(0);
            } else if (HwFoldScreenManagerService.this.isScreenOn()) {
                PostureStateMachine access$5002 = HwFoldScreenManagerService.this.mPostureSM;
                PostureStateMachine unused2 = HwFoldScreenManagerService.this.mPostureSM;
                access$5002.setScreeStateWhenCallComing(1);
            } else {
                PostureStateMachine access$5003 = HwFoldScreenManagerService.this.mPostureSM;
                PostureStateMachine unused3 = HwFoldScreenManagerService.this.mPostureSM;
                access$5003.setScreeStateWhenCallComing(2);
            }
        }
    };
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public String mCurCameraId = null;
    private final Object mDeferDispLock = new Object();
    @GuardedBy({"mDeferDispLock"})
    private int mDeferredDispMode = 0;
    @GuardedBy({"mDeferDispLock"})
    private int mDeferredDispModeChangeCount = 0;
    /* access modifiers changed from: private */
    public final RemoteCallbackList<IFoldDisplayModeListener> mDisplayModeListeners = new RemoteCallbackList<>();
    private DoubleClickWakeupManager mDoubleClickWakeup;
    private FingerPrintWakeupManager mFingerprintWakeup;
    private HwFoldScreenManagerInternal.FoldScreenOnListener mFoldScreenOnListener;
    private int mFoldState;
    /* access modifiers changed from: private */
    public final RemoteCallbackList<IFoldableStateListener> mFoldableStateListeners = new RemoteCallbackList<>();
    /* access modifiers changed from: private */
    public final RemoteCallbackList<IFoldFsmTipsRequestListener> mFsmTipsRequestListeners = new RemoteCallbackList<>();
    /* access modifiers changed from: private */
    public final HwFoldScreenManagerHandler mHandler;
    private final ServiceThread mHandlerThread;
    /* access modifiers changed from: private */
    public volatile boolean mIsCameraOn = false;
    /* access modifiers changed from: private */
    public boolean mIsDisplayLocked = false;
    /* access modifiers changed from: private */
    public volatile boolean mIsDrawingScreenOff = false;
    /* access modifiers changed from: private */
    public boolean mIsFingerPrintReady = false;
    /* access modifiers changed from: private */
    public volatile boolean mIsIntelligent = true;
    /* access modifiers changed from: private */
    public boolean mIsShouldScreenOn = false;
    /* access modifiers changed from: private */
    public boolean mIsWakeUp = true;
    private int mLastDisplayMode = 0;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private BroadcastReceiver mLowTempWarningReceiver = new BroadcastReceiver() {
        /* class com.android.server.fsm.HwFoldScreenManagerService.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Slog.e("Fsm_FoldScreenManagerService", "lowTempWarningReceiver intent is null");
                return;
            }
            int unused = HwFoldScreenManagerService.this.mScreenOnLowTemp = intent.getIntExtra(HwFoldScreenManagerService.LOW_TEMP_WARNING, 0);
            Slog.i("Fsm_FoldScreenManagerService", "on receive lowTempWarningReceiver, value = " + HwFoldScreenManagerService.this.mScreenOnLowTemp);
        }
    };
    private MagnetometerWakeupManager mMagnetometerWakeup;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /* class com.android.server.fsm.HwFoldScreenManagerService.AnonymousClass4 */

        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == 0) {
                Slog.i("Fsm_FoldScreenManagerService", "setScreeStateWhenCallComing:CALL_STATE_IDLE");
                PostureStateMachine access$500 = HwFoldScreenManagerService.this.mPostureSM;
                PostureStateMachine unused = HwFoldScreenManagerService.this.mPostureSM;
                access$500.setScreeStateWhenCallComing(0);
            } else if (state == 1) {
                Slog.i("Fsm_FoldScreenManagerService", "setScreeStateWhenCallComing:CALL_STATE_RINGING");
                if (HwFoldScreenManagerService.this.isScreenOn()) {
                    PostureStateMachine access$5002 = HwFoldScreenManagerService.this.mPostureSM;
                    PostureStateMachine unused2 = HwFoldScreenManagerService.this.mPostureSM;
                    access$5002.setScreeStateWhenCallComing(1);
                } else {
                    PostureStateMachine access$5003 = HwFoldScreenManagerService.this.mPostureSM;
                    PostureStateMachine unused3 = HwFoldScreenManagerService.this.mPostureSM;
                    access$5003.setScreeStateWhenCallComing(2);
                }
            } else if (state != 2) {
                Slog.e("Fsm_FoldScreenManagerService", "setScreeStateWhenCallComing error state=" + state);
            } else {
                Slog.i("Fsm_FoldScreenManagerService", "setScreeStateWhenCallComing:CALL_STATE_OFFHOOK");
                if (HwFoldScreenManagerService.this.isScreenOn()) {
                    PostureStateMachine access$5004 = HwFoldScreenManagerService.this.mPostureSM;
                    PostureStateMachine unused4 = HwFoldScreenManagerService.this.mPostureSM;
                    access$5004.setScreeStateWhenCallComing(1);
                } else {
                    PostureStateMachine access$5005 = HwFoldScreenManagerService.this.mPostureSM;
                    PostureStateMachine unused5 = HwFoldScreenManagerService.this.mPostureSM;
                    access$5005.setScreeStateWhenCallComing(2);
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    private WindowManagerPolicy mPolicy;
    /* access modifiers changed from: private */
    public final PostureStateMachine mPostureSM;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    private PowerWakeupManager mPowerWakeup;
    /* access modifiers changed from: private */
    public final PosturePreprocessManager mPreprocess;
    /* access modifiers changed from: private */
    public int mScreenOffLowTemp = 0;
    /* access modifiers changed from: private */
    public int mScreenOnLowTemp = 0;
    /* access modifiers changed from: private */
    public SensorManager mSensorManager;
    /* access modifiers changed from: private */
    public boolean mShouldNotifyLowTemp = false;
    private String mSpecifiedCameraId = null;
    private TelephonyManager mTelephonyManager;
    private HwTpManager mTpManager;
    private BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() {
        /* class com.android.server.fsm.HwFoldScreenManagerService.AnonymousClass5 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Slog.e("Fsm_FoldScreenManagerService", "userPresentReceiver intent is null");
                return;
            }
            Slog.i("Fsm_FoldScreenManagerService", "on receive userPresentReceiver");
            if (HwFoldScreenManagerService.this.mShouldNotifyLowTemp) {
                HwFoldScreenManagerService.this.sendLowTempWarningBroadcast();
                boolean unused = HwFoldScreenManagerService.this.mShouldNotifyLowTemp = false;
            }
        }
    };
    /* access modifiers changed from: private */
    public WakeupManager mWakeupManager;
    /* access modifiers changed from: private */
    public final WindowManagerInternal mWm;

    public HwFoldScreenManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mHandlerThread = new ServiceThread("Fsm_FoldScreenManagerService", -4, false);
        this.mHandlerThread.start();
        this.mHandler = new HwFoldScreenManagerHandler(this.mHandlerThread.getLooper());
        this.mPostureSM = PostureStateMachine.getInstance();
        this.mPostureSM.init(this);
        this.mPostureSM.start();
        this.mIsIntelligent = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), KEY_INTELLIGENT_AWAKEN, 1, -2) == 1;
        this.mPreprocess = PosturePreprocessManager.getInstance();
        this.mIsDisplayLocked = isLockedDisplayMode(SystemProperties.getInt(HW_FOLD_DISPMODE, 0));
        this.mPreprocess.init(context, getPolicy());
        notifyIntelligentModeChangeToTp(this.mIsIntelligent);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_INTELLIGENT_AWAKEN), true, this.mContentObserver, -1);
        this.mPowerWakeup = new PowerWakeupManager();
        this.mFingerprintWakeup = new FingerPrintWakeupManager();
        this.mDoubleClickWakeup = new DoubleClickWakeupManager();
        this.mMagnetometerWakeup = MagnetometerWakeupManager.getInstance(this.mContext);
        this.mMagnetometerWakeup.initSensorListener();
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        this.mWm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mContext.registerReceiverAsUser(this.mLowTempWarningReceiver, UserHandle.ALL, new IntentFilter(ACTION_THERMAL_LOW_TEMP_WARNING), THERMAL_RECEIVE_PERMISSION, this.mHandler);
        this.mContext.registerReceiverAsUser(this.mUserPresentReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_PRESENT"), null, this.mHandler);
        this.mFoldState = Settings.Secure.getInt(this.mContext.getContentResolver(), HW_FOLD_SCRREN_STATE, 0);
    }

    public void setDrawWindowFlag(boolean isNeedDraw) {
        MagnetometerWakeupManager magnetometerWakeupManager = this.mMagnetometerWakeup;
        if (magnetometerWakeupManager != null && magnetometerWakeupManager.getHallData() == 1) {
            Slog.i("Fsm_FoldScreenManagerService", "FLod screen has expend state, return.");
        } else if (isScreenOnEary()) {
            Slog.i("Fsm_FoldScreenManagerService", "Current display is ScrrenON, return.");
        } else {
            this.mIsDrawingScreenOff = isNeedDraw;
            this.mPostureSM.setPosture(103);
        }
    }

    public boolean isCameraOn() {
        return this.mIsCameraOn;
    }

    public boolean isFrontCameraOn() {
        if (this.mIsCameraOn) {
            Slog.i("Fsm_FoldScreenManagerService", "isFrontCameraOn, mCurCameraId = " + this.mCurCameraId);
            String str = this.mCurCameraId;
            if (str != null) {
                if (!"1".equals(str) && !"1".equals(this.mSpecifiedCameraId)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public boolean isIntelligentOn() {
        return this.mIsIntelligent;
    }

    public boolean isScreenOn() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        return this.mPowerManager.isScreenOn();
    }

    public boolean isIncall() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (this.mTelephonyManager.getCallState() != 0) {
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.fsm.HwFoldScreenManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [android.os.IBinder, com.android.server.fsm.HwFoldScreenManagerService$BinderService] */
    public void onStart() {
        publishBinderService("fold_screen", new BinderService());
        publishLocalService(HwFoldScreenManagerInternal.class, new LocalService());
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
        CameraManager cameraManager = this.mCameraManager;
        if (cameraManager != null) {
            cameraManager.registerAvailabilityCallback(this.mCameraAvailableCallback, (Handler) null);
        }
    }

    public void onBootPhase(int phase) {
    }

    public void monitor() {
        synchronized (this) {
        }
    }

    public void onSwitchUser(int userHandle) {
        HwFoldScreenManagerHandler hwFoldScreenManagerHandler = this.mHandler;
        hwFoldScreenManagerHandler.sendMessage(Message.obtain(hwFoldScreenManagerHandler, 6, userHandle, 0));
    }

    public void systemReady() {
    }

    /* access modifiers changed from: private */
    public void processCameraState(String cameraId, int state) {
        Slog.i("Fsm_FoldScreenManagerService", "processCameraState cameraId: " + cameraId + " , state: " + state);
        if (this.mTpManager == null) {
            this.mTpManager = HwTpManager.getInstance();
        }
        if (state == 1) {
            this.mTpManager.hwTsSetAftConfig("version:3+camera_ON");
        } else {
            this.mTpManager.hwTsSetAftConfig("version:3+camera_OFF");
        }
    }

    private boolean isScreenOnEary() {
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        if (windowManagerPolicy != null) {
            return windowManagerPolicy.isScreenOn();
        }
        return false;
    }

    private boolean isKeyguardLocked() {
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        if (windowManagerPolicy != null) {
            return windowManagerPolicy.isKeyguardLocked();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void notifyFoldStateChangeInner(int foldState) {
        int i = this.mFoldableStateListeners.beginBroadcast();
        while (i > 0) {
            i--;
            IFoldableStateListener listener = this.mFoldableStateListeners.getBroadcastItem(i);
            Integer type = (Integer) this.mFoldableStateListeners.getBroadcastCookie(i);
            if (!(listener == null || type == null)) {
                Bundle extra = new Bundle();
                try {
                    if (type.intValue() == 1) {
                        extra.putInt("android.intent.extra.REASON", 1);
                        extra.putInt("fold_state", foldState);
                        listener.onStateChange(extra);
                    }
                } catch (RemoteException e) {
                    Slog.e("Fsm_FoldScreenManagerService", "notifyFoldStateChangeInner RemoteException");
                }
            }
        }
        this.mFoldableStateListeners.finishBroadcast();
        Slog.i("Fsm_FoldScreenManagerService", "notifyFoldStateChangeInner foldState : " + foldState);
        if (this.mFoldState != foldState) {
            if ((foldState == 1 || foldState == 2) && this.mScreenOnLowTemp == 1) {
                sendLowTempWarningBroadcast();
            }
            this.mFoldState = foldState;
            reportTempWhenFolding();
        }
    }

    private void reportTempWhenFolding() {
        PowerKit powerKit = PowerKit.getInstance();
        if (powerKit == null) {
            Slog.d("Fsm_FoldScreenManagerService", "powerKit is null");
            return;
        }
        try {
            int curTemp = powerKit.getThermalInfo(this.mContext, 33);
            Slog.i("Fsm_FoldScreenManagerService", "report tmepture, curTemp = " + curTemp);
            IMonitor.EventStream stream = IMonitor.openEventStream((int) IMONITOR_TEMP_EVENT_ID);
            stream.setParam(0, curTemp);
            stream.setParam(1, (byte) 1);
            IMonitor.sendEvent(stream);
            IMonitor.closeEventStream(stream);
        } catch (RemoteException e) {
            Slog.e("Fsm_FoldScreenManagerService", "powerKit RemoteException error");
        }
    }

    /* access modifiers changed from: private */
    public void sendLowTempWarningBroadcast() {
        if (!isScreenOn()) {
            Slog.i("Fsm_FoldScreenManagerService", "screen is off, not sendLowTempWarningBroadcast");
        } else if (isKeyguardLocked()) {
            Slog.i("Fsm_FoldScreenManagerService", "in keyguard state, not sendLowTempWarningBroadcast");
            this.mShouldNotifyLowTemp = true;
        } else {
            Slog.i("Fsm_FoldScreenManagerService", "sendLowTempWarningBroadcast");
            Intent intent = new Intent(ACTION_SMART_NOTIFY_FAULT);
            intent.putExtra(KEY_FAULT_DESCRIPTION, VALUE_FAULT_DESCRIPTION);
            intent.putExtra(KEY_FAULT_SUGGESTION, VALUE_FAULT_SUGGESTION);
            intent.putExtra(KEY_FAULT_CODE, VALUE_FAULT_CODE);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, SMART_NOTIFY_FAULT_PERMISSION);
        }
    }

    /* access modifiers changed from: private */
    public void notifyPostureChangeInner(int posture) {
        int i = this.mFoldableStateListeners.beginBroadcast();
        while (i > 0) {
            i--;
            IFoldableStateListener listener = this.mFoldableStateListeners.getBroadcastItem(i);
            Integer type = (Integer) this.mFoldableStateListeners.getBroadcastCookie(i);
            if (!(listener == null || type == null)) {
                Bundle extra = new Bundle();
                try {
                    if (type.intValue() == 2) {
                        extra.putInt("android.intent.extra.REASON", 2);
                        extra.putInt("posture_mode", posture);
                        listener.onStateChange(extra);
                    }
                } catch (RemoteException e) {
                    Slog.e("Fsm_FoldScreenManagerService", "notifyPostureChangeInner RemoteException");
                }
            }
        }
        this.mFoldableStateListeners.finishBroadcast();
    }

    /* access modifiers changed from: private */
    public void notifyDisplayModeChangeInner(int displayMode) {
        int i = this.mDisplayModeListeners.beginBroadcast();
        while (i > 0) {
            i--;
            IFoldDisplayModeListener listener = this.mDisplayModeListeners.getBroadcastItem(i);
            if (listener != null) {
                try {
                    listener.onScreenDisplayModeChange(displayMode);
                } catch (RemoteException e) {
                    Slog.e("Fsm_FoldScreenManagerService", "notifyDisplayModeChangeInner RemoteException");
                }
            }
        }
        this.mDisplayModeListeners.finishBroadcast();
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        if (windowManagerPolicy != null) {
            windowManagerPolicy.setDisplayMode(displayMode);
        }
    }

    public void notifyDispalyModeChangePrepare(int displayMode) {
        long ident = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(this.mContext.getContentResolver(), HW_FOLD_DISPLAY_MODE_PREPARE, displayMode);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: private */
    public void notifyDisplayModeToSubScreenView(int displayMode) {
        HwPhoneWindowManager hwPhoneWindowManager = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        Slog.d("Fsm_FoldScreenManagerService", "notifyDisplayModeChangeBefore displayMode:" + displayMode);
        if (hwPhoneWindowManager != null) {
            hwPhoneWindowManager.notifyDispalyModeChangeBefore(displayMode);
        }
    }

    private void notifyFsmTipsRequestInner(int reqTipsType, Bundle data) {
        synchronized (this.mFsmTipsRequestListeners) {
            int i = this.mFsmTipsRequestListeners.beginBroadcast();
            while (i > 0) {
                i--;
                IFoldFsmTipsRequestListener listener = this.mFsmTipsRequestListeners.getBroadcastItem(i);
                Integer type = (Integer) this.mFsmTipsRequestListeners.getBroadcastCookie(i);
                if (!(listener == null || type == null)) {
                    try {
                        if ((type.intValue() & reqTipsType) > 0) {
                            listener.onRequestFsmTips(reqTipsType, data);
                        }
                    } catch (RemoteException e) {
                        Slog.e("Fsm_FoldScreenManagerService", "notifyFsmTipsRequestInner RemoteException");
                    }
                }
            }
            this.mFsmTipsRequestListeners.finishBroadcast();
        }
    }

    /* access modifiers changed from: private */
    public void notifyFoldStateChangeToTp(int foldState) {
        String config;
        Slog.d("Fsm_FoldScreenManagerService", "notifyFoldStateChangeToTp foldState:" + foldState);
        if (foldState == 1) {
            config = "version:3+expand";
        } else if (foldState == 2) {
            config = "version:3+folder";
        } else if (foldState != 3) {
            Slog.w("Fsm_FoldScreenManagerService", "Invalid foldState=" + foldState);
            return;
        } else {
            config = "version:3+trestle";
        }
        if (this.mTpManager == null) {
            this.mTpManager = HwTpManager.getInstance();
        }
        this.mTpManager.hwTsSetAftConfig(config);
    }

    /* access modifiers changed from: private */
    public void notifyDisplayModeChangeToTp(int displayMode) {
        String config;
        Slog.d("Fsm_FoldScreenManagerService", "notifyDisplayModeChangeToTp displayMode:" + displayMode);
        if (displayMode == 1) {
            config = "version:3+whole";
        } else if (displayMode == 2) {
            config = "version:3+main";
        } else if (displayMode == 3) {
            config = "version:3+minor";
        } else if (displayMode != 4) {
            Slog.w("Fsm_FoldScreenManagerService", "Invalid displayMode=" + displayMode);
            return;
        } else {
            config = "version:3+s_main";
            if (CoordinationModeUtils.getInstance(this.mContext).getCoordinationCreateMode() == 3) {
                config = "version:3+s_minor";
            }
        }
        if (this.mTpManager == null) {
            this.mTpManager = HwTpManager.getInstance();
        }
        this.mTpManager.hwTsSetAftConfig(config);
    }

    /* access modifiers changed from: private */
    public void notifyIntelligentModeChangeToTp(boolean isIntelligent) {
        String config;
        Slog.d("Fsm_FoldScreenManagerService", "notifyIntelligentModeChangeToTp isIntelligent:" + isIntelligent);
        if (isIntelligent) {
            config = "version:3+gesture_ON";
        } else {
            config = "version:3+gesture_OFF";
        }
        if (this.mTpManager == null) {
            this.mTpManager = HwTpManager.getInstance();
        }
        this.mTpManager.hwTsSetAftConfig(config);
    }

    /* access modifiers changed from: package-private */
    public void notifyFoldStateChange(int foldState) {
        HwFoldScreenManagerHandler hwFoldScreenManagerHandler = this.mHandler;
        hwFoldScreenManagerHandler.sendMessage(Message.obtain(hwFoldScreenManagerHandler, 1, foldState, 0));
    }

    /* access modifiers changed from: package-private */
    public void notifyPostureChange(int posture) {
        HwFoldScreenManagerHandler hwFoldScreenManagerHandler = this.mHandler;
        hwFoldScreenManagerHandler.sendMessage(Message.obtain(hwFoldScreenManagerHandler, 0, posture, 0));
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
    }

    /* access modifiers changed from: package-private */
    public void notifyDisplayModeChange(int displayMode) {
        HwFoldScreenManagerHandler hwFoldScreenManagerHandler = this.mHandler;
        hwFoldScreenManagerHandler.sendMessage(Message.obtain(hwFoldScreenManagerHandler, 2, displayMode, 0));
    }

    /* access modifiers changed from: private */
    public int getPostureInner() {
        PostureStateMachine postureStateMachine = this.mPostureSM;
        if (postureStateMachine != null) {
            return postureStateMachine.getPosture();
        }
        return 100;
    }

    /* access modifiers changed from: private */
    public int getFoldableStateInner() {
        PostureStateMachine postureStateMachine = this.mPostureSM;
        if (postureStateMachine != null) {
            return postureStateMachine.getFoldableState();
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public int setDisplayModeInner(int mode) {
        int displayModeBeforeSet = getDisplayModeInner();
        if (!isScreenOn() && getDisplayModeInner() != 4) {
            Slog.w("Fsm_FoldScreenManagerService", "can not set display mode when screenoff");
            return displayModeBeforeSet;
        } else if (mode < 1 || mode > 4 || mode == displayModeBeforeSet) {
            return displayModeBeforeSet;
        } else {
            if (this.mIsDisplayLocked) {
                Slog.w("Fsm_FoldScreenManagerService", "Display mode already be locked.");
                return displayModeBeforeSet;
            }
            Slog.w("Fsm_FoldScreenManagerService", "mPostureSM.setDisplayMode");
            PostureStateMachine postureStateMachine = this.mPostureSM;
            if (postureStateMachine != null) {
                return postureStateMachine.setDisplayMode(mode);
            }
            return displayModeBeforeSet;
        }
    }

    /* access modifiers changed from: private */
    public int getDisplayModeInner() {
        PostureStateMachine postureStateMachine = this.mPostureSM;
        if (postureStateMachine != null) {
            return postureStateMachine.getDisplayMode();
        }
        return 0;
    }

    private boolean isLockedDisplayMode(int mode) {
        if (mode < 1 || mode > 3) {
            return false;
        }
        return true;
    }

    private int getPolicy() {
        if (this.mIsDisplayLocked) {
            return 2;
        }
        if (this.mIsIntelligent) {
            return 1;
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public int lockDisplayModeInner(int mode) {
        if (!isLockedDisplayMode(mode)) {
            Slog.w("Fsm_FoldScreenManagerService", "lockDisplayModeInner: invalid mode " + mode);
            return getDisplayModeInner();
        } else if (!isScreenOn()) {
            Slog.w("Fsm_FoldScreenManagerService", "lockDisplayModeInner: can not set display mode when screenoff");
            return getDisplayModeInner();
        } else if (!this.mIsDisplayLocked || mode != SystemProperties.getInt(HW_FOLD_DISPMODE, 0)) {
            this.mIsDisplayLocked = true;
            SystemProperties.set(HW_FOLD_DISPMODE, Integer.toString(mode));
            this.mPreprocess.updatePolicy(2);
            Slog.i("Fsm_FoldScreenManagerService", "lockDisplayModeInner: mode is " + mode);
            return mode;
        } else {
            Slog.w("Fsm_FoldScreenManagerService", "lockDisplayModeInner: lock the mode again");
            return getDisplayModeInner();
        }
    }

    /* access modifiers changed from: private */
    public int unlockDisplayModeInner() {
        if (!isScreenOn()) {
            Slog.w("Fsm_FoldScreenManagerService", "lockDisplayModeInner: can not set display mode when screenoff");
            return getDisplayModeInner();
        }
        this.mIsDisplayLocked = false;
        SystemProperties.set(HW_FOLD_DISPMODE, Integer.toString(0));
        this.mPreprocess.updatePolicy(getPolicy());
        Slog.i("Fsm_FoldScreenManagerService", "unlockDisplayModeInner");
        return getDisplayModeInner();
    }

    private void setDisplayModeByForce(int mode) {
        if (mode == 0) {
            unlockDisplayModeInner();
        } else {
            lockDisplayModeInner(mode);
        }
    }

    private void dumpAllInfo(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("FOLD SCREEN MANAGER STARTER (dumpsys fold_screen)");
        pw.println("  " + "StateMachine");
        this.mPostureSM.dump(fd, pw, args);
        pw.println("");
        pw.println("  " + "PreprocessManager");
        this.mPreprocess.dump("  ", pw);
        pw.println("");
        pw.println("  " + "DisplayMode = " + getDisplayModeInner());
    }

    /* access modifiers changed from: private */
    public void doDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int len = args.length;
        if (len > 1) {
            String cmd = args[0];
            try {
                if ("setDisplayMode".equals(cmd)) {
                    setDisplayModeInner(Integer.parseInt(args[1]));
                } else if ("setPosture".equals(cmd)) {
                    int posture = Integer.parseInt(args[1]);
                    if (this.mPostureSM != null) {
                        this.mPostureSM.setPosture(posture);
                    }
                } else if ("lockDisplayMode".equals(cmd)) {
                    setDisplayModeByForce(Integer.parseInt(args[1]));
                }
            } catch (NumberFormatException e) {
                Slog.e("Fsm_FoldScreenManagerService", "parseInt fail with NumberFormatException");
            }
        } else if (1 == len) {
            pw.println("FOLD SCREEN MANAGER STARTER: args number error");
            pw.println("  Example: dumpsys fold_screen setDisplayMode 1");
        } else if (len == 0) {
            dumpAllInfo(fd, pw, args);
        }
    }

    /* access modifiers changed from: private */
    public void changeIntelligentMode(int intelligent) {
        synchronized (this.mLock) {
            boolean isIntelligentNow = intelligent == 1;
            if (isIntelligentNow) {
                if (!isIncall()) {
                    PostureStateMachine postureStateMachine = this.mPostureSM;
                    PostureStateMachine postureStateMachine2 = this.mPostureSM;
                    postureStateMachine.setScreeStateWhenCallComing(0);
                } else if (isScreenOn()) {
                    PostureStateMachine postureStateMachine3 = this.mPostureSM;
                    PostureStateMachine postureStateMachine4 = this.mPostureSM;
                    postureStateMachine3.setScreeStateWhenCallComing(1);
                } else {
                    PostureStateMachine postureStateMachine5 = this.mPostureSM;
                    PostureStateMachine postureStateMachine6 = this.mPostureSM;
                    postureStateMachine5.setScreeStateWhenCallComing(2);
                }
            }
            if (isIntelligentNow != this.mIsIntelligent) {
                this.mIsIntelligent = isIntelligentNow;
                Slog.i("Fsm_FoldScreenManagerService", "changeIntelligentMode mIsIntelligent = " + this.mIsIntelligent);
                if (!this.mIsDisplayLocked) {
                    this.mPreprocess.updatePolicy(getPolicy());
                }
                notifyIntelligentModeChangeToTp(this.mIsIntelligent);
            }
        }
    }

    /* access modifiers changed from: private */
    public final class HwFoldScreenManagerHandler extends Handler {
        HwFoldScreenManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    HwFoldScreenManagerService.this.notifyPostureChangeInner(msg.arg1);
                    return;
                case 1:
                    Settings.Secure.putInt(HwFoldScreenManagerService.this.mContext.getContentResolver(), HwFoldScreenManagerService.HW_FOLD_SCRREN_STATE, msg.arg1);
                    HwFoldScreenManagerService.this.notifyFoldStateChangeInner(msg.arg1);
                    HwFoldScreenManagerService.this.notifyFoldStateChangeToTp(msg.arg1);
                    return;
                case 2:
                    Settings.Secure.putInt(HwFoldScreenManagerService.this.mContext.getContentResolver(), "hw_fold_display_mode", msg.arg1);
                    if (HwFoldScreenManagerService.this.mSensorManager == null) {
                        HwFoldScreenManagerService hwFoldScreenManagerService = HwFoldScreenManagerService.this;
                        SensorManager unused = hwFoldScreenManagerService.mSensorManager = (SensorManager) hwFoldScreenManagerService.mContext.getSystemService("sensor");
                    }
                    SensorManager access$1400 = HwFoldScreenManagerService.this.mSensorManager;
                    access$1400.hwSetSensorConfig("setDisplayMode::" + msg.arg1);
                    HwFoldScreenManagerService.this.notifyDisplayModeToSubScreenView(msg.arg1);
                    HwFoldScreenManagerService.this.removeFsmTipsOnDisplaymodeChange(msg.arg1);
                    HwFoldScreenManagerService.this.notifyDisplayModeChangeInner(msg.arg1);
                    HwFoldScreenManagerService.this.notifyDisplayModeChangeToTp(msg.arg1);
                    HwLog.dubaie("DUBAI_TAG_DISPLAY_MODE", "mode=" + msg.arg1);
                    HwFoldScreenManagerService.this.recordFoldScreenCounter(msg.arg1);
                    return;
                case 3:
                    Slog.i("Fsm_FoldScreenManagerService", "sensor error, force wakeup in main screen");
                    HwFoldScreenManagerService.this.mPostureSM.setPosture(106);
                    return;
                case 4:
                    Slog.i("Fsm_FoldScreenManagerService", "magnetometer timeout, turnoff sensor");
                    if (!HwFoldScreenManagerService.this.isScreenOn()) {
                        HwFoldScreenManagerService.this.mPreprocess.stop();
                        WakeupManager unused2 = HwFoldScreenManagerService.this.mWakeupManager = null;
                        return;
                    }
                    return;
                case 5:
                    Slog.i("Fsm_FoldScreenManagerService", "fingerPrint timeout, turnoff sensor and init stateMachine");
                    synchronized (HwFoldScreenManagerService.this.mLock) {
                        HwFoldScreenManagerService.this.notifySleepInner();
                    }
                    return;
                case 6:
                    Slog.i("Fsm_FoldScreenManagerService", "handle user switch ");
                    HwFoldScreenManagerService.this.changeIntelligentMode(Settings.Secure.getIntForUser(HwFoldScreenManagerService.this.mContext.getContentResolver(), HwFoldScreenManagerService.KEY_INTELLIGENT_AWAKEN, 1, -2));
                    return;
                case 7:
                    int displayMode = msg.arg1;
                    Slog.i("Fsm_FoldScreenManagerService", "switch display mode from FSM resume, displayMode=" + displayMode);
                    int unused3 = HwFoldScreenManagerService.this.setDisplayModeInner(displayMode);
                    return;
                case 8:
                    Slog.d("Fsm_FoldScreenManagerService", "handle msg FSM_FREEZE_FOLD_ROTATION_CMD");
                    if (HwFoldScreenManagerService.this.mWm != null) {
                        HwFoldScreenManagerService.this.mWm.setFoldSwitchState(true);
                        return;
                    }
                    return;
                case 9:
                    Slog.d("Fsm_FoldScreenManagerService", "handle msg FSM_UNFREEZE_FOLD_ROTATION_CMD");
                    if (HwFoldScreenManagerService.this.mWm != null) {
                        HwFoldScreenManagerService.this.mWm.setFoldSwitchState(false);
                        return;
                    }
                    return;
                case 10:
                    synchronized (HwFoldScreenManagerService.this.mLock) {
                        HwFoldScreenManagerService.this.notifyIntelligentModeChangeToTp(HwFoldScreenManagerService.this.mIsIntelligent);
                    }
                    return;
                case 11:
                    HwFoldScreenManagerService.this.finishSetFoldDisplayMode(msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    public void pauseDispModeChange() {
        pauseDispModeChangeInner();
    }

    public void resumeDispModeChange() {
        resumeDispModeChangeInner();
    }

    public void freezeFoldRotation() {
        handleDeferDispModeCmd(8);
    }

    public void unFreezeFoldRotation() {
        handleDeferDispModeCmd(9);
    }

    public boolean isPausedDispModeChange() {
        return isPausedDispModeChangeInner();
    }

    public void setDeferredDispMode(int mode) {
        synchronized (this.mDeferDispLock) {
            this.mDeferredDispMode = mode;
        }
    }

    public void resetDispModeChange() {
        synchronized (this.mDeferDispLock) {
            Slog.w("Fsm_FoldScreenManagerService", "resetDispModeChange mDeferredDispModeChangeCount = " + this.mDeferredDispModeChangeCount);
            this.mDeferredDispModeChangeCount = 0;
            this.mDeferredDispMode = 0;
        }
    }

    /* access modifiers changed from: private */
    public void pauseDispModeChangeInner() {
        synchronized (this.mDeferDispLock) {
            this.mDeferredDispModeChangeCount++;
            Slog.d("Fsm_FoldScreenManagerService", "pauseDispModeChangeInner mDeferredDispModeChangeCount = " + this.mDeferredDispModeChangeCount);
        }
    }

    /* access modifiers changed from: private */
    public boolean isPausedDispModeChangeInner() {
        boolean z;
        synchronized (this.mDeferDispLock) {
            z = this.mDeferredDispModeChangeCount > 0;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void resumeDispModeChangeInner() {
        synchronized (this.mDeferDispLock) {
            if (this.mDeferredDispModeChangeCount > 0) {
                Slog.d("Fsm_FoldScreenManagerService", "resumeDispModeChangeInner mDeferredDispModeChangeCount = " + this.mDeferredDispModeChangeCount);
                this.mDeferredDispModeChangeCount = 0;
                unFreezeFoldRotation();
                if (this.mDeferredDispMode != 0) {
                    this.mHandler.removeMessages(7);
                    this.mHandler.sendMessage(Message.obtain(this.mHandler, 7, this.mDeferredDispMode, 0));
                    this.mDeferredDispMode = 0;
                }
            }
        }
    }

    private void handleDeferDispModeCmd(int cmd) {
        HwFoldScreenManagerHandler hwFoldScreenManagerHandler = this.mHandler;
        hwFoldScreenManagerHandler.sendMessage(hwFoldScreenManagerHandler.obtainMessage(cmd));
    }

    private final class BinderService extends IHwFoldScreenManager.Stub {
        private BinderService() {
        }

        public int getPosture() {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                return HwFoldScreenManagerService.this.getPostureInner();
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public int getFoldableState() {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                return HwFoldScreenManagerService.this.getFoldableStateInner();
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public void registerFoldableState(IFoldableStateListener listener, int type) {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                synchronized (this) {
                    HwFoldScreenManagerService.this.mFoldableStateListeners.register(listener, new Integer(type));
                }
                return;
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public void unregisterFoldableState(IFoldableStateListener listener) {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                synchronized (this) {
                    HwFoldScreenManagerService.this.mFoldableStateListeners.unregister(listener);
                }
                return;
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public int setDisplayMode(int mode) {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                Slog.i("Fsm_FoldScreenManagerService", "setDisplayMode mode=" + mode);
                return HwFoldScreenManagerService.this.setDisplayModeInner(mode);
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public int getDisplayMode() {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                return HwFoldScreenManagerService.this.getDisplayModeInner();
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public int lockDisplayMode(int mode) {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PRIVILEGED) == 0) {
                return HwFoldScreenManagerService.this.lockDisplayModeInner(mode);
            }
            throw new SecurityException("Requires PERMISSION_FOLD_SCREEN_PRIVILEGED permission");
        }

        public int unlockDisplayMode() {
            if (Binder.getCallingUid() == 1000 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PRIVILEGED) == 0) {
                return HwFoldScreenManagerService.this.unlockDisplayModeInner();
            }
            throw new SecurityException("Requires PERMISSION_FOLD_SCREEN_PRIVILEGED permission");
        }

        public void registerFoldDisplayMode(IFoldDisplayModeListener listener) {
            if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == 1047 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                synchronized (this) {
                    HwFoldScreenManagerService.this.mDisplayModeListeners.register(listener);
                }
                return;
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public void unregisterFoldDisplayMode(IFoldDisplayModeListener listener) {
            if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == 1047 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                synchronized (this) {
                    HwFoldScreenManagerService.this.mDisplayModeListeners.unregister(listener);
                }
                return;
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        public void registerFsmTipsRequestListener(IFoldFsmTipsRequestListener listener, int type) {
            if (Binder.getCallingUid() != 1000 && Binder.getCallingUid() != 1047 && HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) != 0) {
                throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
            } else if (Binder.getCallingUid() == 1000 || type == 4) {
                synchronized (HwFoldScreenManagerService.this.mFsmTipsRequestListeners) {
                    HwFoldScreenManagerService.this.mFsmTipsRequestListeners.register(listener, new Integer(type));
                }
            } else {
                throw new SecurityException("only provide monitor REQ_BROADCAST_TIPS_REMOVED permission");
            }
        }

        public void unregisterFsmTipsRequestListener(IFoldFsmTipsRequestListener listener) {
            if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == 1047 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                synchronized (HwFoldScreenManagerService.this.mFsmTipsRequestListeners) {
                    HwFoldScreenManagerService.this.mFsmTipsRequestListeners.unregister(listener);
                }
                return;
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }

        /* access modifiers changed from: protected */
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(HwFoldScreenManagerService.this.mContext, "Fsm_FoldScreenManagerService", pw)) {
                HwFoldScreenManagerService.this.doDump(fd, pw, args);
                pw.println("HwFoldScreenManagerService is running...");
            }
        }

        public int reqShowTipsToFsm(int reqTipsType, Bundle data) {
            if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == 1047 || HwFoldScreenManagerService.this.mContext.checkCallingPermission(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN) == 0) {
                return HwFoldScreenManagerService.this.reqShowTipsToFsmInner(reqTipsType, data, Binder.getCallingUid());
            }
            throw new SecurityException(HwFoldScreenManagerService.PERMISSION_FOLD_SCREEN_PERMISSION);
        }
    }

    private void scheduleNotifyIntellientMode() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10));
    }

    private void prepareSetFoldScreenModeLocked() {
        Slog.w("Fsm_FoldScreenManagerService", "prepareSetFoldScreenModeLocked");
        this.mWakeupManager = getWakeupManagerInner(0);
        WakeupManager wakeupManager = this.mWakeupManager;
        if (wakeupManager != null) {
            HwFoldScreenStateImpl.setWakeUpManager(wakeupManager);
        }
        this.mIsDrawingScreenOff = false;
        this.mPreprocess.start(0);
        scheduleNotifyIntellientMode();
    }

    /* access modifiers changed from: private */
    public boolean waitForSetFoldDisplayMode(HwFoldScreenManagerInternal.FoldScreenOnListener listener) {
        synchronized (this.mLock) {
            this.mFoldScreenOnListener = listener;
            Slog.i("Fsm_FoldScreenManagerService", "waitForSetFoldDisplayMode mWakeupManager:" + this.mWakeupManager);
            if (this.mWakeupManager == null || !(this.mWakeupManager instanceof MagnetometerWakeupManager)) {
                sendFinishSetModeMessage(0, 0, 1500);
                prepareSetFoldScreenModeLocked();
                return true;
            }
            sendFinishSetModeMessage(0, 0, 0);
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void sendFinishSetModeMessage(int newMode, int oldMode, int delay) {
        this.mHandler.removeMessages(11);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(11, newMode, oldMode), (long) delay);
    }

    /* access modifiers changed from: private */
    public void finishSetFoldDisplayMode(int newMode, int oldMode) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(11);
            HwFoldScreenManagerInternal.FoldScreenOnListener listener = this.mFoldScreenOnListener;
            Slog.i("Fsm_FoldScreenManagerService", "finishSetFoldDisplayMode listener:" + listener + ", newMode:" + newMode + ", oldMode:" + oldMode);
            if (listener != null) {
                int adjustNewMode = newMode;
                int adjustOldMode = oldMode;
                if (adjustNewMode == 0 && adjustOldMode == 0) {
                    int currentMode = getDisplayModeInner();
                    adjustNewMode = currentMode;
                    adjustOldMode = currentMode;
                }
                listener.onFoldScreenOn(adjustNewMode, adjustOldMode);
            }
            this.mFoldScreenOnListener = null;
        }
    }

    private final class LocalService extends HwFoldScreenManagerInternal {
        private LocalService() {
        }

        public int getPosture() {
            return HwFoldScreenManagerService.this.getPostureInner();
        }

        public int getFoldableState() {
            return HwFoldScreenManagerService.this.getFoldableStateInner();
        }

        public int setDisplayMode(int mode) {
            return HwFoldScreenManagerService.this.setDisplayModeInner(mode);
        }

        public int getDisplayMode() {
            return HwFoldScreenManagerService.this.getDisplayModeInner();
        }

        public int lockDisplayMode(int mode) {
            return HwFoldScreenManagerService.this.lockDisplayModeInner(mode);
        }

        public int unlockDisplayMode() {
            return HwFoldScreenManagerService.this.unlockDisplayModeInner();
        }

        public int doubleClickToSetDisplayMode(Point point) {
            if (HwFoldScreenManagerService.this.mIsDisplayLocked) {
                Slog.w("Fsm_FoldScreenManagerService", "doubleClickToSetDisplayMode: Display mode already be locked");
                return HwFoldScreenManagerService.this.getDisplayModeInner();
            }
            return HwFoldScreenManagerService.this.mPostureSM.doubleClickToSetDisplayMode(HwFoldScreenStateImpl.getDisplayRect(point));
        }

        public int onDoubleClick(boolean isScreenOn, Bundle extra) {
            if (extra == null) {
                return 0;
            }
            Point clickPosition = (Point) extra.getParcelable("position");
            if (isScreenOn) {
                return doubleClickToSetDisplayMode(clickPosition);
            }
            HwFoldScreenManagerService.this.mPostureSM.setDisplayRectForDoubleClick(HwFoldScreenStateImpl.getDisplayRect(clickPosition));
            if (HwFoldScreenManagerService.this.mPowerManager == null) {
                HwFoldScreenManagerService hwFoldScreenManagerService = HwFoldScreenManagerService.this;
                PowerManager unused = hwFoldScreenManagerService.mPowerManager = (PowerManager) hwFoldScreenManagerService.mContext.getSystemService("power");
            }
            HwFoldScreenManagerService.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 4, "doubleclick");
            return 0;
        }

        public void notifySleep() {
            synchronized (HwFoldScreenManagerService.this.mLock) {
                HwFoldScreenManagerService.this.notifySleepInner();
            }
        }

        public boolean foldScreenTurningOn(HwFoldScreenManagerInternal.FoldScreenOnListener listener) {
            return HwFoldScreenManagerService.this.waitForSetFoldDisplayMode(listener);
        }

        public void prepareWakeup(int wakeupType, Bundle extra) {
            if (HwFoldScreenManagerService.this.isScreenOn() || extra == null) {
                Slog.w("Fsm_FoldScreenManagerService", "screen is on, or extra is null");
                return;
            }
            synchronized (HwFoldScreenManagerService.this.mLock) {
                if (HwFoldScreenManagerService.this.mIsWakeUp) {
                    if (wakeupType != 3 || !HwFoldScreenManagerService.this.mIsShouldScreenOn) {
                        HwFoldScreenManagerService.this.notifySleepInner();
                    } else {
                        Slog.e("Fsm_FoldScreenManagerService", "ignore fast fingerPrint, screen is turning on");
                        return;
                    }
                }
                boolean unused = HwFoldScreenManagerService.this.mIsWakeUp = true;
                int uid = extra.getInt("uid");
                String opPackageName = extra.getString("opPackageName");
                int reason = extra.getInt("reason");
                String details = extra.getString("details");
                boolean unused2 = HwFoldScreenManagerService.this.mIsShouldScreenOn = true;
                if (wakeupType == 5) {
                    HwFoldScreenManagerService.this.mPostureSM.setDisplayRectForDoubleClick(HwFoldScreenStateImpl.getDisplayRect((Point) extra.getParcelable("position")));
                }
                if (wakeupType == 4) {
                    HwFoldScreenManagerService.this.mHandler.sendEmptyMessageDelayed(4, 5000);
                    boolean unused3 = HwFoldScreenManagerService.this.mIsShouldScreenOn = false;
                } else if (wakeupType == 3) {
                    boolean unused4 = HwFoldScreenManagerService.this.mIsFingerPrintReady = true;
                    boolean unused5 = HwFoldScreenManagerService.this.mIsShouldScreenOn = false;
                    if (opPackageName != null && opPackageName.equals(AppStartupDataMgr.HWPUSH_PKGNAME)) {
                        HwFoldScreenManagerService.this.mPostureSM.setPickupWakeUp(true);
                    }
                    HwFoldScreenManagerService.this.mHandler.sendEmptyMessageDelayed(3, 1500);
                    HwFoldScreenManagerService.this.mPreprocess.start(wakeupType);
                    HwFoldScreenManagerService.this.notifyIntelligentModeChangeToTp(HwFoldScreenManagerService.this.mIsIntelligent);
                    return;
                } else {
                    HwFoldScreenManagerService.this.mHandler.sendEmptyMessageDelayed(3, 1500);
                }
                WakeupManager unused6 = HwFoldScreenManagerService.this.mWakeupManager = HwFoldScreenManagerService.this.getWakeupManagerInner(wakeupType);
                if (HwFoldScreenManagerService.this.mWakeupManager != null) {
                    HwFoldScreenManagerService.this.mWakeupManager.setWakeUpInfo(uid, opPackageName, reason, details);
                    HwFoldScreenStateImpl.setWakeUpManager(HwFoldScreenManagerService.this.mWakeupManager);
                }
                boolean unused7 = HwFoldScreenManagerService.this.mIsDrawingScreenOff = false;
                HwFoldScreenManagerService.this.mPreprocess.start(wakeupType);
                HwFoldScreenManagerService.this.notifyIntelligentModeChangeToTp(HwFoldScreenManagerService.this.mIsIntelligent);
            }
        }

        public boolean onSetFoldDisplayModeFinished(int newMode, int oldMode) {
            synchronized (HwFoldScreenManagerService.this.mLock) {
                Slog.d("Fsm_FoldScreenManagerService", "onSetFoldDisplayModeFinished newMode:" + newMode + ", oldMode:" + oldMode);
                if (HwFoldScreenManagerService.this.mIsDrawingScreenOff || HwFoldScreenManagerService.this.mWakeupManager == null || !(HwFoldScreenManagerService.this.mWakeupManager instanceof MagnetometerWakeupManager)) {
                    HwFoldScreenManagerService.this.sendFinishSetModeMessage(newMode, oldMode, 0);
                } else {
                    HwFoldScreenManagerService.this.mWakeupManager.setFoldScreenReady(true);
                    HwFoldScreenManagerService.this.mWakeupManager.wakeup();
                }
            }
            return true;
        }

        public void startWakeup(int wakeupType, Bundle extra) {
            if (extra == null) {
                Slog.w("Fsm_FoldScreenManagerService", "extra is null, return");
                return;
            }
            synchronized (HwFoldScreenManagerService.this.mLock) {
                String details = extra.getString("details");
                Slog.w("Fsm_FoldScreenManagerService", "Fingerprint startWakeup details: " + details);
                if ("goingWakeUp".equals(details)) {
                    boolean unused = HwFoldScreenManagerService.this.mIsShouldScreenOn = true;
                    HwFoldScreenManagerService.this.mPreprocess.start(wakeupType);
                    return;
                }
                if (HwFoldScreenManagerService.this.mIsFingerPrintReady && HwFoldScreenManagerService.this.mIsWakeUp && !HwFoldScreenManagerService.this.mIsShouldScreenOn) {
                    HwFoldScreenManagerService.this.notifySleepInner();
                }
            }
        }

        public void notifyFlip() {
            if (HwFoldScreenManagerService.this.mIsDisplayLocked) {
                Slog.w("Fsm_FoldScreenManagerService", "notifyFlip: Display mode already be locked");
                return;
            }
            synchronized (HwFoldScreenManagerService.this.mLock) {
                HwFoldScreenManagerService.this.mPostureSM.handleFlipPosture();
            }
        }

        public void handleDrawWindow() {
            if (HwFoldScreenManagerService.this.isScreenOn()) {
                Slog.w("Fsm_FoldScreenManagerService", "screen is on");
                return;
            }
            boolean unused = HwFoldScreenManagerService.this.mIsDrawingScreenOff = true;
            HwFoldScreenManagerService.this.mPostureSM.setPosture(103);
        }

        public boolean getInfoDrawWindow() {
            return HwFoldScreenManagerService.this.mIsDrawingScreenOff;
        }

        public void resetInfoDrawWindow() {
            boolean unused = HwFoldScreenManagerService.this.mIsDrawingScreenOff = false;
            HwFoldScreenManagerService.this.mPostureSM.notifySleep();
            WakeupManager unused2 = HwFoldScreenManagerService.this.mWakeupManager = null;
        }

        public int reqShowTipsToFsm(int reqTipsType, Bundle data) {
            return HwFoldScreenManagerService.this.reqShowTipsToFsmInner(reqTipsType, data, Process.myUid());
        }

        public void pauseDispModeChange() {
            HwFoldScreenManagerService.this.pauseDispModeChangeInner();
        }

        public void resumeDispModeChange() {
            HwFoldScreenManagerService.this.resumeDispModeChangeInner();
        }

        public boolean isPausedDispModeChange() {
            return HwFoldScreenManagerService.this.isPausedDispModeChangeInner();
        }

        public void notifyLowTempWarning(int value) {
            int unused = HwFoldScreenManagerService.this.mScreenOffLowTemp = value;
        }

        public void notifyScreenOn() {
            if (HwFoldScreenManagerService.this.mScreenOffLowTemp == 1) {
                HwFoldScreenManagerService.this.sendLowTempWarningBroadcast();
                int unused = HwFoldScreenManagerService.this.mScreenOffLowTemp = 0;
            }
        }
    }

    /* access modifiers changed from: private */
    public WakeupManager getWakeupManagerInner(int type) {
        if (type == 3) {
            return this.mFingerprintWakeup;
        }
        if (type == 4) {
            return this.mMagnetometerWakeup;
        }
        if (type != 5) {
            return this.mPowerWakeup;
        }
        return this.mDoubleClickWakeup;
    }

    /* access modifiers changed from: private */
    public void notifySleepInner() {
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(4);
        this.mPreprocess.stop();
        this.mPostureSM.notifySleep();
        WakeupManager wakeupManager = this.mWakeupManager;
        if (wakeupManager != null) {
            wakeupManager.setFingerprintReady(false);
            this.mWakeupManager.setFoldScreenReady(false);
        }
        this.mWakeupManager = null;
        HwFoldScreenStateImpl.setWakeUpManager(null);
        this.mIsFingerPrintReady = false;
        this.mIsWakeUp = false;
        this.mPostureSM.setPickupWakeUp(false);
    }

    /* access modifiers changed from: protected */
    public void exitCoordinationDisplayMode() {
        ActivityTaskManagerInternal activityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        if (activityTaskManagerInternal != null) {
            activityTaskManagerInternal.exitCoordinationMode(true, false);
        }
    }

    /* access modifiers changed from: protected */
    public void removeForceWakeUp() {
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
    }

    /* access modifiers changed from: protected */
    public void bdReport(int eventId, String eventMsg) {
        Flog.bdReport(this.mContext, eventId, eventMsg);
    }

    /* access modifiers changed from: private */
    public int reqShowTipsToFsmInner(int reqTipsType, Bundle data, int callerUid) {
        Bundle fsmTipsData;
        int targetDisplayMode;
        String callerUidName = this.mContext.getPackageManager().getNameForUid(callerUid);
        Slog.d("Fsm_FoldScreenManagerService", "reqShowTipsToFsmInner reqTipsType = " + reqTipsType + " ,Bundle = " + data + ", callerName = " + callerUidName);
        if (reqTipsType != 4 || callerUid == 1000) {
            if (data == null) {
                fsmTipsData = new Bundle();
                fsmTipsData.putString("KEY_TIPS_STR_CALLER_NAME", callerUidName);
            } else {
                if (callerUid != 1000) {
                    data.putString("KEY_TIPS_STR_CALLER_NAME", callerUidName);
                } else if (data.getString("KEY_TIPS_STR_CALLER_NAME") == null) {
                    data.putString("KEY_TIPS_STR_CALLER_NAME", callerUidName);
                }
                String cameraId = data.getString("KEY_TIPS_STR_CAMERA_ID", null);
                if (cameraId != null) {
                    if ("0".equals(cameraId) || "1".equals(cameraId)) {
                        this.mSpecifiedCameraId = cameraId;
                    }
                    Slog.i("Fsm_FoldScreenManagerService", "reqShowTipsToFsmInner set new mSpecifiedCameraId = " + this.mSpecifiedCameraId);
                }
                fsmTipsData = data;
            }
            if (reqTipsType == 2 && fsmTipsData.getInt("KEY_TIPS_INT_VIEW_TYPE", 0) == 1 && (targetDisplayMode = fsmTipsData.getInt("KEY_TIPS_INT_DISPLAY_MODE", 0)) == 2 && getDisplayModeInner() == 3) {
                notifyDisplayModeToSubScreenView(targetDisplayMode);
            }
            notifyFsmTipsRequestInner(reqTipsType, fsmTipsData);
            return reqTipsType;
        }
        Slog.w("Fsm_FoldScreenManagerService", "permission check fail");
        return reqTipsType;
    }

    /* access modifiers changed from: private */
    public void removeFsmTipsOnDisplaymodeChange(int displayMode) {
        Slog.i("Fsm_FoldScreenManagerService", "removeFsmTipsOnDisplaymodeChange:");
        if (this.mLastDisplayMode == displayMode) {
            Slog.i("Fsm_FoldScreenManagerService", "displayMode does not change, return");
            return;
        }
        this.mSpecifiedCameraId = null;
        String callerUidName = this.mContext.getPackageManager().getNameForUid(1000);
        Bundle data = new Bundle();
        data.putString("KEY_TIPS_STR_CALLER_NAME", callerUidName);
        data.putInt("KEY_TIPS_INT_REMOVED_REASON", 1);
        data.putInt("KEY_TIPS_INT_DISPLAY_MODE", displayMode);
        reqShowTipsToFsmInner(1, null, 1000);
    }

    /* access modifiers changed from: private */
    public void recordFoldScreenCounter(int displayMode) {
        int i = this.mLastDisplayMode;
        if (i == displayMode) {
            Slog.d("Fsm_FoldScreenManagerService", "displayMode does not change, return");
            return;
        }
        if (displayMode == 1 && i != 0) {
            int foldCounter = Settings.Global.getInt(this.mContext.getContentResolver(), HW_FOLD_SCREEN_COUNTER, 0);
            if (foldCounter == 0 || foldCounter > MAX_FOLD_CREEN_NUM) {
                Slog.d("Fsm_FoldScreenManagerService", "Begin to record folding number");
                foldCounter = 0;
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), HW_FOLD_SCREEN_COUNTER, foldCounter + 1);
        }
        this.mLastDisplayMode = displayMode;
    }
}
