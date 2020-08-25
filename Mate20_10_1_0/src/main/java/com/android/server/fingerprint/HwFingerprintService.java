package com.android.server.fingerprint;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.trust.TrustManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.IBiometricServiceLockoutResetCallback;
import android.hardware.biometrics.IBiometricServiceReceiver;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.server.FingerprintDataInterface;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.biometrics.AuthenticationClient;
import com.android.server.biometrics.BiometricServiceBase;
import com.android.server.biometrics.ClientMonitor;
import com.android.server.biometrics.Constants;
import com.android.server.biometrics.EnrollClient;
import com.android.server.biometrics.RemovalClient;
import com.android.server.biometrics.fingerprint.FingerprintService;
import com.android.server.biometrics.fingerprint.FingerprintUtils;
import com.android.server.fingerprint.HwFingerprintSets;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.PickUpWakeScreenManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.security.permissionmanager.util.PermConst;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.content.pm.UserInfoEx;
import com.huawei.android.os.UserManagerEx;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.fingerprint.IAuthenticator;
import com.huawei.fingerprint.IAuthenticatorListener;
import com.huawei.hiai.BuildConfig;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.fingerprint.FingerprintCalibrarionView;
import com.huawei.server.fingerprint.FingerprintViewUtils;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import vendor.huawei.hardware.biometrics.fingerprint.V2_2.IExtBiometricsFingerprint;
import vendor.huawei.hardware.biometrics.fingerprint.V2_2.IFidoAuthenticationCallback;
import vendor.huawei.hardware.tp.V1_0.ITouchscreen;

public class HwFingerprintService extends FingerprintService {
    private static final String ACTIVITYNAME_OF_WECHAT_ENROLL = "com.tencent.mm.plugin.fingerprint.ui.FingerPrintAuthUI";
    private static final String APS_INIT_HEIGHT = "aps_init_height";
    private static final String APS_INIT_WIDTH = "aps_init_width";
    private static final int BASE_BRIGHTNESS = 3000;
    public static final int CHECK_NEED_REENROLL_FINGER = 1003;
    protected static final int CLIENT_OFF = 0;
    protected static final int CLIENT_ON = 1;
    private static final int CODE_DISABLE_FINGERPRINT_VIEW_RULE = 1114;
    private static final int CODE_ENABLE_FINGERPRINT_VIEW_RULE = 1115;
    private static final int CODE_FINGERPRINT_FORBID_GOTOSLEEP = 1125;
    private static final int CODE_FINGERPRINT_LOGO_POSITION = 1130;
    private static final int CODE_FINGERPRINT_LOGO_RADIUS = 1129;
    private static final int CODE_FINGERPRINT_WEATHER_DATA = 1128;
    private static final int CODE_GET_FINGERPRINT_LIST_ENROLLED = 1118;
    private static final int CODE_GET_HARDWARE_POSITION = 1110;
    private static final int CODE_GET_HARDWARE_TYPE = 1109;
    private static final int CODE_GET_HIGHLIGHT_SPOT_RADIUS_RULE = 1122;
    private static final int CODE_GET_HOVER_SUPPORT = 1113;
    private static final int CODE_GET_TOKEN_LEN_RULE = 1103;
    private static final int CODE_IS_FINGERPRINT_HARDWARE_DETECTED = 1119;
    private static final int CODE_IS_FP_NEED_CALIBRATE_RULE = 1101;
    private static final int CODE_IS_SUPPORT_DUAL_FINGERPRINT = 1120;
    private static final int CODE_IS_WAIT_AUTHEN = 1127;
    private static final int CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE = 1116;
    private static final int CODE_NOTIFY_OPTICAL_CAPTURE = 1111;
    private static final int CODE_POWER_KEYCODE = 1126;
    private static final int CODE_REMOVE_FINGERPRINT_RULE = 1107;
    private static final int CODE_REMOVE_MASK_AND_SHOW_BUTTON_RULE = 1117;
    private static final int CODE_SEND_UNLOCK_LIGHTBRIGHT = 1121;
    private static final int CODE_SET_CALIBRATE_MODE_RULE = 1102;
    private static final int CODE_SET_FINGERPRINT_MASK_VIEW_RULE = 1104;
    private static final int CODE_SET_HOVER_SWITCH = 1112;
    private static final int CODE_SHOW_FINGERPRINT_BUTTON_RULE = 1106;
    private static final int CODE_SHOW_FINGERPRINT_VIEW_RULE = 1105;
    private static final int CODE_SUSPEND_AUTHENTICATE = 1108;
    private static final int CODE_SUSPEND_ENROLL = 1123;
    private static final int CODE_UDFINGERPRINT_SPOTCOLOR = 1124;
    private static final int DEFAULT_CAPTURE_BRIGHTNESS = 248;
    private static final int DEFAULT_COLOR = -16711681;
    private static final int DEFAULT_FINGER_LOGO_RADIUS = 87;
    private static final int DEFAULT_RADIUS = 95;
    private static final int DENSITY_DEFUALT_HEIGHT = 2880;
    private static final int DENSITY_DEFUALT_WIDTH = 1440;
    private static final String DESCRIPTOR_FINGERPRINT_SERVICE = "android.hardware.fingerprint.IFingerprintService";
    private static final int ERROR_CODE_COMMEN_ERROR = 8;
    private static final String FACE_DETECT_REASON = "fingerprint";
    private static final String FIDO_ASM = "com.huawei.hwasm";
    private static final int FINGERPRINT_ACQUIRED_FINGER_DOWN = 2002;
    private static final int FINGERPRINT_HARDWARE_OPTICAL = 1;
    private static final int FINGERPRINT_HARDWARE_OUTSCREEN = 0;
    private static final int FINGERPRINT_HARDWARE_ULTRASONIC = 2;
    private static final String FINGERPRINT_METADATA_KEY = "fingerprint.system.view";
    private static final int FINGER_DOWN_TYPE_AUTHENTICATING = 1;
    private static final int FINGER_DOWN_TYPE_AUTHENTICATING_SETTINGS = 2;
    private static final int FINGER_DOWN_TYPE_AUTHENTICATING_SYSTEMUI = 3;
    private static final int FINGER_DOWN_TYPE_ENROLLING = 0;
    private static final int FLAG_FINGERPRINT_LOCATION_BACK = 1;
    private static final int FLAG_FINGERPRINT_LOCATION_FRONT = 2;
    private static final int FLAG_FINGERPRINT_LOCATION_UNDER_DISPLAY = 4;
    private static final int FLAG_FINGERPRINT_TYPE_CAPACITANCE = 1;
    private static final int FLAG_FINGERPRINT_TYPE_MASK = 15;
    private static final int FLAG_FINGERPRINT_TYPE_OPTICAL = 2;
    private static final int FLAG_FINGERPRINT_TYPE_ULTRASONIC = 3;
    private static final int FLAG_USE_UD_FINGERPRINT = 134217728;
    private static final int FP_CLOSE = 0;
    private static final int FP_OPEN = 1;
    public static final int GET_OLD_DATA = 100;
    private static final int HAL_CMD_NOT_FOUND_RET = 0;
    private static final int HUAWEI_FINGERPRINT_CAPTURE_COMPLETE = 0;
    private static final int HUAWEI_FINGERPRINT_DOWN = 2002;
    private static final int HUAWEI_FINGERPRINT_DOWN_UD = 2102;
    private static final int HUAWEI_FINGERPRINT_TRIGGER_FACE_RECOGNIZATION = 2104;
    private static final int HUAWEI_FINGERPRINT_UP = 2003;
    private static final int HW_FP_NO_COUNT_FAILED_ATTEMPS = 16777216;
    private static final int INVALID_VALUE = -1;
    private static final int IN_SET_FINGERPRINT_IDENTIFY = 1;
    private static final String KEY_DB_CHILDREN_MODE_FPID = "fp_children_mode_fp_id";
    private static final String KEY_DB_CHILDREN_MODE_STATUS = "fp_children_enabled";
    private static final String KEY_KEYGUARD_ENABLE = "fp_keyguard_enable";
    public static final int MASK_TYPE_BACK = 4;
    public static final int MASK_TYPE_BUTTON = 1;
    public static final int MASK_TYPE_FULL = 0;
    public static final int MASK_TYPE_IMAGE = 3;
    public static final int MASK_TYPE_NONE = 2;
    private static final int MAX_BRIGHTNESS = 255;
    private static final String METADATA_KEY = "fingerprint.system.view";
    private static final int MMI_TYPE_GET_HIGHLIGHT_LEVEL = 906;
    private static final int MSG_AUTHENTICATEDFINISH_TO_HAL = 199;
    private static final int MSG_CHECK_AND_DEL_TEMPLATES = 84;
    private static final int MSG_CHECK_HOVER_SUPPORT = 56;
    private static final int MSG_CHECK_OLD_TEMPLATES = 85;
    private static final int MSG_CHECK_SWITCH_FREQUENCE_SUPPORT = 81;
    private static final int MSG_DEL_OLD_TEMPLATES = 86;
    private static final int MSG_DISABLE_FACE_RECOGNIZATION = 210;
    private static final int MSG_ENABLE_FACE_RECOGNIZATION = 211;
    private static final int MSG_FACE_RECOGNIZATION_SUCC = 212;
    private static final int MSG_GET_BRIGHTNEWSS_FROM_HAL = 909;
    private static final int MSG_GET_HIGHLIGHT_SPOT_COLOR_FROM_HAL = 903;
    private static final int MSG_GET_LOGO_POSITION_FROM_HAL = 912;
    private static final int MSG_GET_RADIUS_FROM_HAL = 902;
    private static final int MSG_GET_SENSOR_POSITION_BOTTOM_RIGHT = 60;
    private static final int MSG_GET_SENSOR_POSITION_TOP_LEFT = 59;
    private static final int MSG_GET_SENSOR_TYPE = 55;
    private static final int MSG_JUMP_IN_SETTING_VIEW = 66;
    private static final int MSG_JUMP_OUT_SETTING_VIEW = 67;
    private static final int MSG_MMI_UD_UI_LOGO_SIZE = 910;
    private static final int MSG_NOTIFY_AUTHENCATION = 87;
    private static final int MSG_NOTIFY_BLUESPOT_DISMISS = 62;
    private static final int MSG_OPTICAL_HBM_FOR_CAPTURE_IMAGE = 52;
    private static final int MSG_REDUCING_FREQUENCE = 83;
    private static final int MSG_RESUME_AUTHENTICATION = 54;
    private static final int MSG_RESUME_ENROLLMENT = 65;
    private static final int MSG_SET_HOVER_DISABLE = 58;
    private static final int MSG_SET_HOVER_ENABLE = 57;
    private static final int MSG_SUSPEND_AUTHENTICATION = 53;
    private static final int MSG_SUSPEND_ENROLLMENT = 64;
    private static final int MSG_TYPE_FINGERPRINT_NAV = 43;
    private static final int MSG_TYPE_VIRTUAL_NAV = 45;
    private static final int MSG_UNLOCK_LEARNING = 63;
    private static final int MSG_UPDATE_AUTHENTICATION_SCENARIO = 300;
    private static final int MSG_UPGRADING_FREQUENCE = 82;
    private static final int OUT_SET_FINGERPRINT_IDENTIFY = 0;
    private static final String PATH_CHILDMODE_STATUS = "childmode_status";
    private static final String PKGNAME_OF_KEYGUARD = "com.android.systemui";
    private static final String PKGNAME_OF_SETTINGS = "com.android.settings";
    private static final String PKGNAME_OF_SYSTEM_MANAGER = "com.huawei.systemmanager";
    private static final String PKGNAME_OF_WECHAT = "com.tencent.mm";
    private static final long POWER_PUSH_DOWN_TIME_THR = 430;
    private static final int PROTECT_TIME_FROM_END = 600;
    private static final int PROTECT_TIME_FROM_FINGER_DOWN = 1000;
    private static final int PROTECT_TIME_FROM_START = 700;
    private static final String PROXIMITY_TP = "proximity-tp";
    public static final int REMOVE_USER_DATA = 101;
    public static final int RESTORE_AUTHENTICATE = 0;
    public static final int RESTORE_ENROLL = 0;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_CHECKING = 3;
    protected static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SET_FINGERPRINT_IDENTIFY = "set_fingerprint_identify";
    public static final int SET_LIVENESS_SWITCH = 1002;
    private static final int STATUS_PARENT_CTRL_OFF = 0;
    private static final int STATUS_PARENT_CTRL_STUDENT = 1;
    /* access modifiers changed from: private */
    public static final boolean SUPPORT_INFORM_FACE = SystemProperties.getBoolean("ro.config.fp_notice_face", false);
    public static final int SUSPEND_AUTHENTICATE = 1;
    public static final int SUSPEND_ENROLL = 1;
    private static final int SWITCH_FREQUENCE_SUPPORT = 1;
    private static final String TAG = "HwFingerprintService";
    private static final int TOUCHE_SWITCH_OFF = 0;
    private static final int TOUCHE_SWITCH_ON = 1;
    protected static final String TP_HAL_CONFIG_OFF = "1,0";
    protected static final String TP_HAL_CONFIG_ON = "1,1";
    private static final int TP_HAL_DEATH_COOKIE = 1001;
    private static final int TP_HAL_FEATURE_FLAG = 2;
    public static final int TYPE_DISMISS_ = 0;
    private static final int TYPE_FINGERPRINT_AUTHENTICATION_RESULT_FAIL = 1;
    private static final int TYPE_FINGERPRINT_AUTHENTICATION_RESULT_SUCCESS = 0;
    private static final int TYPE_FINGERPRINT_AUTHENTICATION_UNCHECKED = 2;
    public static final int TYPE_FINGERPRINT_BUTTON = 2;
    public static final int TYPE_FINGERPRINT_VIEW = 1;
    private static final int UNDEFINED_TYPE = -1;
    private static final int UPDATE_SECURITY_USER_ID = 102;
    public static final int VERIFY_USER = 1001;
    /* access modifiers changed from: private */
    public static boolean mCheckNeedEnroll = true;
    /* access modifiers changed from: private */
    public static boolean mIsChinaArea = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static boolean mLivenessNeedBetaQualification = false;
    /* access modifiers changed from: private */
    public static boolean mNeedRecreateDialog = false;
    private static boolean mRemoveFingerprintBGE = SystemProperties.getBoolean("ro.config.remove_finger_bge", false);
    /* access modifiers changed from: private */
    public static boolean mRemoveOldTemplatesFeature = SystemProperties.getBoolean("ro.config.remove_old_templates", false);
    private final int DEFAULT_SCREEN_SIZE_STRING_LENGHT = 2;
    private final int MSG_BINDER_SUCCESS_FLAG = HwSecDiagnoseConstant.OEMINFO_ID_DEVICE_RENEW;
    private final int MSG_SCREEOFF_UNLOCK_LIGHTBRIGHT = 200;
    private final int MSG_SCREEON_UNLOCK_BACKLIGHT = 202;
    private final int MSG_SCREEON_UNLOCK_LIGHTBRIGHT = 201;
    private final int SCREENOFF_UNLOCK = 1;
    private final int SCREENON_BACKLIGHT_UNLOCK = 3;
    private final int SCREENON_UNLOCK = 2;
    /* access modifiers changed from: private */
    public long downTime;
    ContentObserver fpObserver = new ContentObserver(null) {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass11 */

        public void onChange(boolean selfChange) {
            if (HwFingerprintService.this.mContext == null) {
                Log.w(HwFingerprintService.TAG, "mContext is null");
                return;
            }
            HwFingerprintService hwFingerprintService = HwFingerprintService.this;
            int unused = hwFingerprintService.mState = Settings.Secure.getIntForUser(hwFingerprintService.mContext.getContentResolver(), HwFingerprintService.KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
            if (HwFingerprintService.this.mState == 0) {
                HwFingerprintService.this.sendCommandToHal(0);
            }
            Log.i(HwFingerprintService.TAG, "fp_keyguard_state onChange: " + HwFingerprintService.this.mState);
        }
    };
    private AODFaceUpdateMonitor mAodFaceUpdateMonitor;
    private int mAppDefinedMaskType = -1;
    private AuthenticatedParam mAuthenticatedParam = null;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentAuthFpDev;
    IExtBiometricsFingerprint mDaemonEx = null;
    private String mDefinedAppName = "";
    private DisplayEngineManager mDisplayEngineManager;
    private Handler mFingerHandler;
    /* access modifiers changed from: private */
    public FingerViewController mFingerViewController;
    private int mFingerprintType = -1;
    /* access modifiers changed from: private */
    public boolean mForbideKeyguardCall = false;
    protected FingerprintDataInterface mFpDataCollector;
    private int mFpDelayPowerTime = 0;
    /* access modifiers changed from: private */
    public int mHwFailedAttempts = 0;
    private IAuthenticator mIAuthenticator = new IAuthenticator.Stub() {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass1 */

        public int verifyUser(final IFingerprintServiceReceiver receiver, final IAuthenticatorListener listener, int userid, final byte[] nonce, final String aaid) {
            Log.d(HwFingerprintService.TAG, "verifyUser");
            if (!HwFingerprintService.this.isCurrentUserOrProfile(UserHandle.getCallingUserId())) {
                Log.w(HwFingerprintService.TAG, "Can't authenticate non-current user");
                return -1;
            } else if (receiver == null || listener == null || nonce == null || aaid == null) {
                Log.e(HwFingerprintService.TAG, "wrong paramers.");
                return -1;
            } else {
                int uid = Binder.getCallingUid();
                int pid = Binder.getCallingPid();
                Log.d(HwFingerprintService.TAG, "uid =" + uid);
                if (uid != 1000) {
                    Log.e(HwFingerprintService.TAG, "permission denied.");
                    return -1;
                } else if (!HwFingerprintService.this.canUseBiometric(HwFingerprintService.FIDO_ASM, true, uid, pid, userid)) {
                    Log.w(HwFingerprintService.TAG, "FIDO_ASM can't use fingerprint");
                    return -1;
                } else {
                    final int effectiveGroupId = HwFingerprintService.this.getEffectiveUserId(userid);
                    final int callingUserId = UserHandle.getCallingUserId();
                    HwFingerprintService.this.mHandler.post(new Runnable() {
                        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass1.AnonymousClass1 */

                        public void run() {
                            HwFingerprintService.this.setLivenessSwitch("fido");
                            HwFingerprintService.this.startAuthentication(receiver.asBinder(), 0, callingUserId, effectiveGroupId, receiver, 0, true, HwFingerprintService.FIDO_ASM, listener, aaid, nonce);
                        }
                    });
                    return 0;
                }
            }
        }

        public int cancelVerifyUser(final IFingerprintServiceReceiver receiver, int userId) {
            if (receiver == null) {
                Log.e(HwFingerprintService.TAG, "wrong paramers.");
                return -1;
            }
            Log.d(HwFingerprintService.TAG, "cancelVerify");
            if (!HwFingerprintService.this.isCurrentUserOrProfile(UserHandle.getCallingUserId())) {
                Log.w(HwFingerprintService.TAG, "Can't cancel authenticate non-current user");
                return -1;
            }
            int uId = Binder.getCallingUid();
            Log.d(HwFingerprintService.TAG, "uId =" + uId);
            if (uId != 1000) {
                Log.e(HwFingerprintService.TAG, "permission denied.");
                return -1;
            }
            if (!HwFingerprintService.this.canUseBiometric(HwFingerprintService.FIDO_ASM, true, uId, Binder.getCallingPid(), userId)) {
                Log.w(HwFingerprintService.TAG, "FIDO_ASM can't cancel fingerprint auth");
                return -1;
            }
            HwFingerprintService.this.mHandler.post(new Runnable() {
                /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass1.AnonymousClass2 */

                public void run() {
                    HwFingerprintService.this.stopFidoClient(receiver.asBinder());
                }
            });
            return 0;
        }
    };
    private IWindowManager mIWm;
    private int mInitDisplayHeight = -1;
    private int mInitDisplayWidth = -1;
    private boolean mIsBlackAuthenticateEvent = false;
    /* access modifiers changed from: private */
    public boolean mIsFingerInScreenSupported;
    private boolean mIsHighLightNeed;
    private boolean mIsInteractive = false;
    /* access modifiers changed from: private */
    public AtomicBoolean mIsKeyguardAuthenStatus = new AtomicBoolean(false);
    private boolean mIsPowerKeyDown = false;
    private boolean mIsSupportDualFingerprint = false;
    private boolean mIsUdAuthenticating = false;
    private boolean mIsUdEnrolling = false;
    private boolean mIsUdFingerprintChecking = false;
    private boolean mKeepMaskAfterAuthentication = false;
    private long mLastAuthenticatedEndTime = 0;
    private long mLastAuthenticatedStartTime = 0;
    private long mLastFingerDownTime = 0;
    private long mLastPowerKeyDownTime = 0;
    private long mLastPowerKeyUpTime = 0;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private Bundle mMaskViewBundle;
    private final int mMsgUdEnvParaData = 400;
    private ContentObserver mNavModeObserver = new ContentObserver(null) {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass12 */

        public void onChange(boolean selfChange) {
            if (HwFingerprintService.this.mContext == null || HwFingerprintService.this.mContext.getContentResolver() == null) {
                Log.d(HwFingerprintService.TAG, "mContext or the resolver is null");
                return;
            }
            boolean virNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(HwFingerprintService.this.mContext.getContentResolver());
            if (HwFingerprintService.this.mVirNavModeEnabled != virNavModeEnabled) {
                boolean unused = HwFingerprintService.this.mVirNavModeEnabled = virNavModeEnabled;
                Log.d(HwFingerprintService.TAG, "Navigation mode changed, mVirNavModeEnabled = " + HwFingerprintService.this.mVirNavModeEnabled);
                HwFingerprintService hwFingerprintService = HwFingerprintService.this;
                hwFingerprintService.sendCommandToHal(hwFingerprintService.mVirNavModeEnabled ? 45 : 43);
            }
        }
    };
    private boolean mNeedResumeTouchSwitch = false;
    private String mPackageDisableMask;
    private int mPowerDelayFpTime = 0;
    private Runnable mPowerFingerWakeUpRunable = new Runnable() {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass17 */

        public void run() {
            Slog.i(HwFingerprintService.TAG, "powerfp wakeup run");
            HwFingerprintService.this.mIsKeyguardAuthenStatus.set(false);
            HwFingerprintService.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 1, "android.policy:POWER_FINGERPRINT");
        }
    };
    /* access modifiers changed from: private */
    public final PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public ITouchscreen mProxy = null;
    /* access modifiers changed from: private */
    public AlertDialog mReEnrollDialog;
    private BroadcastReceiver mReceiver;
    /* access modifiers changed from: private */
    public String mScreen;
    /* access modifiers changed from: private */
    public int mState = 0;
    private boolean mSupportBlackAuthentication = false;
    /* access modifiers changed from: private */
    public boolean mSupportKids = SystemProperties.getBoolean("ro.config.kidsfinger_enable", false);
    private boolean mSupportPowerFp = false;
    private BroadcastReceiver mSwitchFrequenceMonitor = new BroadcastReceiver() {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass15 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON) || action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF)) {
                    String unused = HwFingerprintService.this.mScreen = action;
                    HwFingerprintService.this.handleScreenOnOrOff();
                }
            }
        }
    };
    private TelecomManager mTelecomManager = null;
    private String mTpSensorName = null;
    private BroadcastReceiver mUserDeletedMonitor = new BroadcastReceiver() {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass3 */
        private static final String FP_DATA_DIR = "fpdata";

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.USER_REMOVED")) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    Slog.i(HwFingerprintService.TAG, "user deleted:" + userId);
                    if (userId == -1) {
                        Slog.i(HwFingerprintService.TAG, "get User id failed");
                        return;
                    }
                    int newUserId = userId;
                    int newPathId = userId;
                    if (UserManagerEx.isHwHiddenSpace(UserManagerEx.getUserInfoEx(UserManager.get(HwFingerprintService.this.mContext), userId))) {
                        newUserId = -100;
                        newPathId = 0;
                    }
                    File fpDir = new File(Environment.getFingerprintFileDirectory(newPathId), FP_DATA_DIR);
                    if (!fpDir.exists()) {
                        Slog.v(HwFingerprintService.TAG, "no fpdata!");
                    } else {
                        HwFingerprintService.this.removeUserData(newUserId, fpDir.getAbsolutePath());
                    }
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    if (HwFingerprintService.mCheckNeedEnroll) {
                        if (HwFingerprintService.mRemoveOldTemplatesFeature) {
                            if (HwFingerprintService.mIsChinaArea) {
                                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_CHECK_AND_DEL_TEMPLATES);
                            } else {
                                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_CHECK_OLD_TEMPLATES);
                            }
                        }
                        int checkValReEnroll = HwFingerprintService.this.checkNeedReEnrollFingerPrints();
                        int checkValCalibrate = HwFingerprintService.this.checkNeedCalibrateFingerPrint();
                        Log.e(HwFingerprintService.TAG, "USER_PRESENT mUserDeletedMonitor need enrol : " + checkValReEnroll + "need calibrate:" + checkValCalibrate);
                        if (HwFingerprintService.mRemoveOldTemplatesFeature) {
                            if (HwFingerprintService.mIsChinaArea && checkValReEnroll == 1) {
                                HwFingerprintService.this.updateActiveGroupEx(-100);
                                HwFingerprintService.this.updateActiveGroupEx(0);
                                HwFingerprintService.this.showDialog(false);
                            } else if (!HwFingerprintService.mIsChinaArea && checkValReEnroll == 3) {
                                HwFingerprintService.this.showDialog(true);
                            }
                        } else if (checkValReEnroll == 1 && checkValCalibrate != 1) {
                            HwFingerprintService.this.intentOthers(context);
                        }
                        boolean unused = HwFingerprintService.mCheckNeedEnroll = false;
                    }
                    if (HwFingerprintService.this.mFingerViewController != null && "com.android.systemui".equals(HwFingerprintService.this.mFingerViewController.getCurrentPackage())) {
                        Log.d(HwFingerprintService.TAG, "USER_PRESENT removeMaskOrButton");
                        HwFingerprintService.this.mFingerViewController.removeMaskOrButton();
                    }
                    boolean unused2 = HwFingerprintService.this.mForbideKeyguardCall = false;
                    if (HwFingerprintService.FACE_DETECT_REASON.equals(intent.getStringExtra("unlockReason"))) {
                        HwFingerprintService.this.sendCommandToHal(212);
                    } else {
                        HwFingerprintService.this.sendCommandToHal(63);
                    }
                }
            }
        }
    };
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass13 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.d(HwFingerprintService.TAG, "intent is null");
            } else if (SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED.equals(intent.getAction())) {
                HwFingerprintService hwFingerprintService = HwFingerprintService.this;
                boolean unused = hwFingerprintService.mVirNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(hwFingerprintService.mContext.getContentResolver());
                Log.d(HwFingerprintService.TAG, "Read the navigation mode after user switch, mVirNavModeEnabled = " + HwFingerprintService.this.mVirNavModeEnabled);
                HwFingerprintService hwFingerprintService2 = HwFingerprintService.this;
                hwFingerprintService2.sendCommandToHal(hwFingerprintService2.mVirNavModeEnabled ? 45 : 43);
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mVirNavModeEnabled = false;
    private HashSet<String> mWhitelist = new HashSet<>();
    private WindowManagerInternal mWindowManagerInternal;
    private boolean mflagFirstIn = true;
    private long mtimeStart = 0;
    /* access modifiers changed from: private */
    public String opPackageName;
    private final Runnable screenOnOrOffRunnable = new Runnable() {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass8 */

        public void run() {
            boolean hasFingerprints = false;
            int fpState = Settings.Secure.getIntForUser(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
            if (FingerprintUtils.getInstance().getBiometricsForUser(HwFingerprintService.this.mContext, ActivityManager.getCurrentUser()).size() > 0) {
                hasFingerprints = true;
            }
            if (fpState != 0 && hasFingerprints) {
                return;
            }
            if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON.equals(HwFingerprintService.this.mScreen)) {
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_UPGRADING_FREQUENCE);
            } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF.equals(HwFingerprintService.this.mScreen)) {
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_REDUCING_FREQUENCE);
            }
        }
    };
    ContentObserver setFpIdentifyViewObserver = new ContentObserver(null) {
        /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass10 */

        public void onChange(boolean selfChange) {
            if (HwFingerprintService.this.mContext == null) {
                Log.w(HwFingerprintService.TAG, "setFpIdentifyViewObserver mContext is null");
                return;
            }
            int setState = Settings.Secure.getIntForUser(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.SET_FINGERPRINT_IDENTIFY, 0, ActivityManager.getCurrentUser());
            int ret = HwFingerprintService.this.sendCommandToHal(setState == 1 ? HwFingerprintService.MSG_JUMP_IN_SETTING_VIEW : HwFingerprintService.MSG_JUMP_OUT_SETTING_VIEW);
            Log.i(HwFingerprintService.TAG, "setFpIdentifyViewObserver setState: " + setState + " ,ret: " + ret);
        }
    };
    private int typeDetails = -1;

    public void serviceDied(long cookie) {
        HwFingerprintService.super.serviceDied(cookie);
        this.mDaemonEx = null;
    }

    /* access modifiers changed from: private */
    public IExtBiometricsFingerprint getFingerprintDaemonEx() {
        IExtBiometricsFingerprint iExtBiometricsFingerprint = this.mDaemonEx;
        if (iExtBiometricsFingerprint != null) {
            return iExtBiometricsFingerprint;
        }
        try {
            this.mDaemonEx = IExtBiometricsFingerprint.getService();
        } catch (NoSuchElementException e) {
        } catch (RemoteException e2) {
            Slog.e(TAG, "Failed to get biometric interface", e2);
        }
        Slog.w(TAG, "getFingerprintDaemonEx inst = " + this.mDaemonEx);
        return this.mDaemonEx;
    }

    /* access modifiers changed from: private */
    public boolean inLockoutMode() {
        if (this.mFailedAttempts.get(ActivityManager.getCurrentUser(), 0) >= 5) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean canUseBiometric(String opPackageName2) {
        return canUseBiometric(opPackageName2, true, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId());
    }

    private int getkidsFingerId(String whichMode, int userID, Context context) {
        if (context != null) {
            return Settings.Secure.getIntForUser(context.getContentResolver(), whichMode, 0, userID);
        }
        Slog.w(TAG, "getkidsFingerId - context = null");
        return 0;
    }

    private boolean isKidSwitchOn(int userID, Context context) {
        if (1 == Settings.Secure.getIntForUser(context.getContentResolver(), KEY_DB_CHILDREN_MODE_STATUS, 0, userID)) {
            return true;
        }
        return false;
    }

    private boolean isParentControl(int userID, Context context) {
        if (context == null || context.getContentResolver() == null) {
            return false;
        }
        int status = Settings.Secure.getIntForUser(context.getContentResolver(), PATH_CHILDMODE_STATUS, 0, userID);
        Slog.d(TAG, "ParentControl status is " + status);
        if (status == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void setKidsFingerprint(int userID, boolean isKeyguard) {
        Slog.d(TAG, "setKidsFingerprint:start");
        int kidFpId = getkidsFingerId(KEY_DB_CHILDREN_MODE_FPID, userID, this.mContext);
        if (kidFpId != 0) {
            boolean isParent = isParentControl(userID, this.mContext);
            boolean isPcCastMode = HwPCUtils.isPcCastModeInServer();
            Slog.d(TAG, "setKidsFingerprint-isParent = " + isParent + ", isPcCastMode =" + isPcCastMode);
            if (isKeyguard && isKidSwitchOn(userID, this.mContext) && !isParent && !isPcCastMode) {
                kidFpId = 0;
            }
            Slog.d(TAG, "setKidsFingerprint-kidFpId = " + kidFpId);
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            try {
                daemon.setKidsFingerprint(kidFpId);
            } catch (RemoteException e) {
                Slog.e(TAG, "setKidsFingerprint RemoteException:" + e);
            }
            Slog.d(TAG, "framework setKidsFingerprint is ok ---end");
        }
    }

    /* access modifiers changed from: private */
    public void startAuthentication(IBinder token, long opId, int callingUserId, int groupId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName2, IAuthenticatorListener listener, String aaid, byte[] nonce) {
        HwFingerprintService hwFingerprintService;
        updateActiveGroup(groupId, opPackageName2);
        Log.v(TAG, "HwFingerprintService-startAuthentication(" + opPackageName2 + ")");
        AuthenticationClient client = new HwFIDOAuthenticationClient(getContext(), getConstants(), getDaemonWrapper(), 0, token, new ServiceListenerImpl(this, receiver), callingUserId, this.mCurrentUserId, groupId, opId, restricted, opPackageName2, listener, false, aaid, nonce);
        if (inLockoutMode()) {
            hwFingerprintService = this;
            if (!hwFingerprintService.isKeyguard(opPackageName2)) {
                Log.v(TAG, "In lockout mode; disallowing authentication");
                if (!client.onError(0, 7, 0)) {
                    Log.w(TAG, "Cannot send timeout message to client");
                    return;
                }
                return;
            }
        } else {
            hwFingerprintService = this;
        }
        invokeParentPrivateFunction(hwFingerprintService, getClass().getSuperclass().getSuperclass(), "startClient", new Class[]{ClientMonitor.class, Boolean.TYPE}, new Object[]{client, true});
    }

    /* access modifiers changed from: protected */
    public void notifyEnrollCanceled() {
        notifyEnrollmentCanceled();
    }

    /* access modifiers changed from: protected */
    public void handleAcquired(long deviceId, int acquiredInfo, int vendorCode) {
        int clientCode;
        stopPickupTrunOff();
        HwFingerprintService.super.handleAcquired(deviceId, acquiredInfo, vendorCode);
        if (acquiredInfo == 6) {
            clientCode = vendorCode + 1000;
        } else {
            clientCode = acquiredInfo;
        }
        if (clientCode == 2002) {
            this.mLastFingerDownTime = System.currentTimeMillis();
            this.mIsBlackAuthenticateEvent = !this.mPowerManager.isInteractive();
            Slog.i(TAG, "powerfp mLastFingerDownTime=" + this.mLastFingerDownTime + " mIsBlackAuthenticateEvent=" + this.mIsBlackAuthenticateEvent);
        }
    }

    public boolean handleEnrollResult(BiometricAuthenticator.Identifier identifier, int remaining) {
        boolean result = HwFingerprintService.super.handleEnrollResult(identifier, remaining);
        if (!result) {
            notifyEnrollingFingerUp();
            Slog.w(TAG, "no eroll client, remove erolled fingerprint");
            if (remaining == 0) {
                IBiometricsFingerprint daemon = getFingerprintDaemon();
                if (daemon == null) {
                    return false;
                }
                try {
                    daemon.remove(identifier.getBiometricId(), ActivityManager.getCurrentUser());
                } catch (RemoteException e) {
                }
            }
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public void notifyAuthCanceled(String topPackage) {
        notifyAuthenticationCanceled(topPackage);
    }

    public AuthenticationClientImpl creatAuthenticationClient(Context context, DaemonWrapper daemon, long halDeviceId, IBinder token, ServiceListener listener, int targetUserId, int groupId, long opId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int flag) {
        return new HwFingerprintAuthClient(context, daemon, halDeviceId, token, listener, targetUserId, groupId, opId, restricted, owner, cookie, requireConfirmation, flag);
    }

    public void handleHwFailedAttempt(int flags, String packagesName) {
        if ((HW_FP_NO_COUNT_FAILED_ATTEMPS & flags) == 0 || !"com.android.settings".equals(packagesName)) {
            this.mHwFailedAttempts++;
        } else {
            Slog.i(TAG, "no need count hw failed attempts");
        }
    }

    public void resetFailedAttemptsForUser(boolean clearAttemptCounter, int userId) {
        HwFingerprintService.super.resetFailedAttemptsForUser(clearAttemptCounter, userId);
        this.mHwFailedAttempts = 0;
    }

    /* access modifiers changed from: protected */
    public void addHighlightOnAcquired(int acquiredInfo, int vendorCode) {
        FingerprintDataInterface fingerprintDataInterface;
        FingerprintDataInterface fingerprintDataInterface2;
        int clientAcquireInfo = acquiredInfo == 6 ? vendorCode + 1000 : acquiredInfo;
        if (clientAcquireInfo == 2002 && (fingerprintDataInterface2 = this.mFpDataCollector) != null) {
            fingerprintDataInterface2.reportFingerDown();
        } else if (clientAcquireInfo == 0 && (fingerprintDataInterface = this.mFpDataCollector) != null) {
            fingerprintDataInterface.reportCaptureCompleted();
        }
        if (clientAcquireInfo == 2002) {
            this.downTime = System.currentTimeMillis();
        }
        if (clientAcquireInfo == 2002 && isKeyguardCurrentClient()) {
            LogPower.push(179);
        }
        ClientMonitor clientMonitor = getCurrentClient();
        if (clientMonitor == null) {
            Log.e(TAG, "mCurrentClient is null notifyFinger failed");
            return;
        }
        if (clientAcquireInfo == 2002) {
            Log.d(TAG, "onAcquired set mCurrentAuthFpDev DEVICE_BACK");
            this.mCurrentAuthFpDev = 0;
        }
        String currentOpName = clientMonitor.getOwnerString();
        if (isSupportPowerFp() && currentClient(this.mKeyguardPackage)) {
            if (clientAcquireInfo == 2002) {
                setKeyguardAuthenScreenOn();
            } else if (clientAcquireInfo == 2003) {
                setNoWaitPowerEvent();
            }
        }
        if (clientAcquireInfo == HUAWEI_FINGERPRINT_DOWN_UD) {
            if (clientMonitor instanceof AuthenticationClient) {
                Log.d(TAG, "notify that AuthenticationClient finger down:" + currentOpName);
                this.mCurrentAuthFpDev = 1;
                if ("com.android.systemui".equals(currentOpName)) {
                    notifyFingerDown(3);
                } else {
                    notifyFingerDown(1);
                }
            } else if (clientMonitor instanceof EnrollClient) {
                Log.d(TAG, "notify that EnrollClient finger down");
                notifyFingerDown(0);
            }
        } else if (clientAcquireInfo == 5 || clientAcquireInfo == 1 || clientAcquireInfo == 2003) {
            if (clientAcquireInfo == 5) {
                Log.d(TAG, "FINGERPRINT_ACQUIRED_TOO_FAST notifyCaptureFinished");
                notifyCaptureFinished(1);
            }
            if (clientMonitor instanceof AuthenticationClient) {
                Log.d(TAG, "clientAcquireInfo = " + clientAcquireInfo);
                notifyAuthenticationFinished(currentOpName, 2, this.mHwFailedAttempts);
            }
        } else if (clientAcquireInfo == 0) {
            if (!(clientMonitor instanceof AuthenticationClient)) {
                return;
            }
            if ("com.android.settings".equals(currentOpName)) {
                notifyCaptureFinished(2);
            } else {
                notifyCaptureFinished(1);
            }
        } else if (clientAcquireInfo == HUAWEI_FINGERPRINT_TRIGGER_FACE_RECOGNIZATION) {
            Log.d(TAG, "clientAcquireInfo = " + clientAcquireInfo);
            triggerFaceRecognization();
        }
    }

    /* access modifiers changed from: protected */
    public void handleError(long deviceId, int error, int vendorCode) {
        ClientMonitor client = getCurrentClient();
        if (client instanceof EnrollClient) {
            notifyEnrollmentCanceled();
        }
        if (error == 8 && vendorCode > 3000) {
            int vendorCode2 = vendorCode - 3000;
            int i = 255;
            if (vendorCode2 < 255) {
                i = vendorCode2;
            }
            vendorCode = i;
            Slog.w(TAG, "change brightness to " + vendorCode);
            notifyFingerCalibrarion(vendorCode);
        }
        if (client != null && "com.android.systemui".equals(client.getOwnerString())) {
            clearRepeatAuthentication();
        }
        HwFingerprintService.super.handleError(deviceId, error, vendorCode);
    }

    public boolean canUseBiometric(String opPackageName2, boolean requireForeground, int uid, int pid, int userId, boolean isDetected) {
        if (opPackageName2 == null || opPackageName2.equals("")) {
            Slog.i(TAG, "opPackageName is null or opPackageName is invalid");
            return false;
        }
        this.opPackageName = opPackageName2;
        if (opPackageName2.equals(FIDO_ASM) || opPackageName2.equals("com.huawei.securitymgr") || opPackageName2.equals("com.huawei.aod")) {
            return true;
        }
        if (!isDetected || !opPackageName2.equals(PKGNAME_OF_WECHAT)) {
            return HwFingerprintService.super.canUseBiometric(opPackageName2, requireForeground, uid, pid, userId, isDetected);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void removeInternal(RemovalClient client) {
        if (client.getListener() == null) {
            Slog.w(TAG, "startRemove: receiver is null");
            return;
        }
        if (getBiometricUtils().isDualFp() && client.getBiometricId() != 0) {
            List<Fingerprint> finerprints = FingerprintUtils.getInstance().getFingerprintsForUser(getContext(), client.getTargetUserId(), 1);
            int fingerprintSize = finerprints.size();
            int i = 0;
            while (true) {
                if (i >= fingerprintSize) {
                    break;
                } else if (finerprints.get(i).getBiometricId() == client.getBiometricId()) {
                    Slog.d(TAG, "dualFingerprint send MSG_REMOVE_UD");
                    sendCommandToHal(104);
                    break;
                } else {
                    i++;
                }
            }
        }
        if (getBiometricUtils().isDualFp() && client.getBiometricId() == 0) {
            Slog.d(TAG, "dualFingerprint send MSG_REMOVE_ALL");
            sendCommandToHal(107);
        }
        HwFingerprintService.super.removeInternal(client);
    }

    /* access modifiers changed from: protected */
    public void udFingerprintAllRemoved(ClientMonitor client, int groupId) {
        HwFingerprintService.super.udFingerprintAllRemoved(client, groupId);
        if (getBiometricUtils().isDualFp() && (client instanceof RemovalClient)) {
            RemovalClient removeClient = (RemovalClient) client;
            FingerprintUtils fingerUtil = FingerprintUtils.getInstance();
            boolean hasUDFingerprints = true;
            boolean hasFingerprints = fingerUtil.getFingerprintsForUser(this.mContext, groupId, -1).size() > 0;
            if (fingerUtil.getFingerprintsForUser(this.mContext, groupId, 1).size() <= 0) {
                hasUDFingerprints = false;
            }
            if (!hasUDFingerprints) {
                sendCommandToHal(0);
                Slog.d(TAG, "UDFingerprint all removed so TP CLOSE");
            }
            if (removeClient.getBiometricId() == 0 && hasFingerprints) {
                Slog.d(TAG, "dualFingerprint-> handleRemoved, but do not destory client.");
            }
        }
    }

    /* access modifiers changed from: protected */
    public void dualFingerprintStartAuth(int flags, String opPackageName2) {
        if (getBiometricUtils().isDualFp()) {
            Slog.d(TAG, "dualFingerprint startAuthentication and flag is: " + flags);
            if (flags == 0) {
                if (canUseUdFingerprint(opPackageName2)) {
                    Slog.d(TAG, "dualFingerprint send MSG_AUTH_ALL");
                    sendCommandToHal(103);
                }
            } else if ((FLAG_USE_UD_FINGERPRINT & flags) == 0) {
            } else {
                if ((67108864 & flags) != 0) {
                    Slog.d(TAG, "dualFingerprint send MSG_AUTH_ALL");
                    sendCommandToHal(103);
                    return;
                }
                Slog.d(TAG, "dualFingerprint send MSG_AUTH_UD");
                sendCommandToHal(102);
            }
        }
    }

    public void enrollInternal(EnrollClientImpl client, int userId, int flags, String opPackageName2) {
        boolean dualFp = getBiometricUtils().isDualFp();
        int targetDevice = 0;
        if (dualFp) {
            this.mEnrolled = getEnrolledFingerprintsEx(opPackageName2, flags == 4096 ? 1 : 0, userId).size();
        }
        if (!hasReachedEnrollmentLimit(userId)) {
            boolean isPrivacyUser = checkPrivacySpaceEnroll(userId, ActivityManager.getCurrentUser());
            if (isCurrentUserOrProfile(userId) || isPrivacyUser) {
                updateActiveGroup(userId, opPackageName2);
                client.setGroupId(userId);
                if (dualFp && "com.android.settings".equals(opPackageName2)) {
                    if (flags == 4096) {
                        targetDevice = 1;
                    }
                    Slog.d(TAG, "dualFingerprint enroll targetDevice is: " + targetDevice);
                    if (targetDevice == 1) {
                        Slog.d(TAG, "dualFingerprint send MSG_ENROLL_UD");
                        sendCommandToHal(101);
                        client.setTargetDevice(1);
                    }
                }
                notifyEnrollmentStarted(flags);
                HwFingerprintService.super.enrollInternal(client);
                return;
            }
            Flog.w(1303, "user invalid enroll error");
        }
    }

    public FingerprintServiceWrapper creatFingerprintServiceWrapper() {
        return new HwFingerprintServiceWrapper();
    }

    /* access modifiers changed from: private */
    public boolean isHwTransactInterest(int code) {
        if (code == CODE_IS_FP_NEED_CALIBRATE_RULE || code == CODE_SET_CALIBRATE_MODE_RULE || code == CODE_GET_TOKEN_LEN_RULE || code == CODE_SET_FINGERPRINT_MASK_VIEW_RULE || code == CODE_SHOW_FINGERPRINT_VIEW_RULE || code == 1106 || code == 1107 || code == CODE_GET_HARDWARE_POSITION || code == CODE_FINGERPRINT_LOGO_POSITION || code == CODE_GET_HARDWARE_TYPE || code == CODE_NOTIFY_OPTICAL_CAPTURE || code == 1108 || code == CODE_SET_HOVER_SWITCH || code == CODE_GET_HOVER_SUPPORT || code == CODE_DISABLE_FINGERPRINT_VIEW_RULE || code == CODE_ENABLE_FINGERPRINT_VIEW_RULE || code == CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE || code == 1117 || code == CODE_IS_SUPPORT_DUAL_FINGERPRINT || code == 1118 || code == CODE_IS_FINGERPRINT_HARDWARE_DETECTED || code == CODE_SEND_UNLOCK_LIGHTBRIGHT || code == CODE_GET_HIGHLIGHT_SPOT_RADIUS_RULE || code == CODE_SUSPEND_ENROLL || code == CODE_UDFINGERPRINT_SPOTCOLOR || code == CODE_FINGERPRINT_FORBID_GOTOSLEEP || code == CODE_POWER_KEYCODE || code == 1127 || code == CODE_FINGERPRINT_WEATHER_DATA) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isClonedProfile(int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            return userInfo != null && userInfo.isClonedProfile();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* access modifiers changed from: private */
    public boolean isFingerprintTransactInterest(int code) {
        if (code == CODE_FINGERPRINT_LOGO_RADIUS) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean onHwFingerprintTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (onTransactToHal(code, data, reply, flags)) {
            return true;
        }
        return onHwTransact(code, data, reply, flags);
    }

    private final class HwFingerprintServiceWrapper extends FingerprintServiceWrapper {
        private HwFingerprintServiceWrapper() {
            super(HwFingerprintService.this);
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (HwFingerprintService.this.isHwTransactInterest(code)) {
                HwFingerprintService.this.checkPermissions();
                return HwFingerprintService.this.onHwTransact(code, data, reply, flags);
            } else if (!HwFingerprintService.this.isFingerprintTransactInterest(code)) {
                return HwFingerprintService.super.onTransact(code, data, reply, flags);
            } else {
                HwFingerprintService.this.checkPermissions();
                return HwFingerprintService.this.onHwFingerprintTransact(code, data, reply, flags);
            }
        }

        public long preEnroll(IBinder token) {
            Flog.i(1303, "FingerprintService preEnroll");
            return HwFingerprintService.super.preEnroll(token);
        }

        public int postEnroll(IBinder token) {
            Flog.i(1303, "FingerprintService postEnroll");
            return HwFingerprintService.super.postEnroll(token);
        }

        public void enroll(IBinder token, byte[] cryptoToken, int userId, IFingerprintServiceReceiver receiver, int flags, String opPackageName) {
            Flog.i(1303, "FingerprintService enroll");
            HwFingerprintService.super.enroll(token, cryptoToken, userId, receiver, flags, opPackageName);
        }

        public void cancelEnrollment(IBinder token) {
            Flog.i(1303, "FingerprintService cancelEnrollment");
            HwFingerprintService.super.cancelEnrollment(token);
            HwFingerprintService.this.notifyEnrollmentCanceled();
        }

        public void authenticate(IBinder token, long opId, int groupId, IFingerprintServiceReceiver receiver, int flags, String opPackageName) {
            int hwGroupId;
            Flog.i(1303, "FingerprintService authenticate");
            if (!"com.huawei.systemmanager".equals(opPackageName) || !HwFingerprintService.this.isKeyguardLocked()) {
                if (groupId == 0 || !HwFingerprintService.this.isClonedProfile(groupId)) {
                    hwGroupId = groupId;
                } else {
                    Log.i(HwFingerprintService.TAG, "Clone profile authenticate,change userid to 0");
                    hwGroupId = 0;
                }
                if (!HwFingerprintService.this.canUseBiometric(opPackageName)) {
                    Log.i(HwFingerprintService.TAG, "authenticate reject opPackageName:" + opPackageName);
                    return;
                }
                HwFingerprintService.this.setLivenessSwitch(opPackageName);
                if (HwFingerprintService.this.mSupportKids) {
                    Slog.i(HwFingerprintService.TAG, "mSupportKids=" + HwFingerprintService.this.mSupportKids);
                    HwFingerprintService hwFingerprintService = HwFingerprintService.this;
                    hwFingerprintService.setKidsFingerprint(groupId, hwFingerprintService.isKeyguard(opPackageName));
                }
                HwFingerprintService.this.notifyAuthenticationStarted(opPackageName, receiver, flags, hwGroupId, null, null, false);
                HwFingerprintService.super.authenticate(token, opId, hwGroupId, receiver, flags, opPackageName);
                return;
            }
            Slog.w(HwFingerprintService.TAG, "interrupt authenticate in keyguard locked");
        }

        public void cancelAuthentication(IBinder token, String opPackageName) {
            HwFingerprintService.super.cancelAuthentication(token, opPackageName);
            HwFingerprintService.this.notifyAuthenticationCanceled(opPackageName);
        }

        public void remove(IBinder token, int fingerId, int groupId, int userId, IFingerprintServiceReceiver receiver) {
            Flog.i(1303, "FingerprintService remove");
            HwFingerprintService.super.remove(token, fingerId, groupId, userId, receiver);
        }

        public void rename(int fingerId, int groupId, String name) {
            Flog.i(1303, "FingerprintService rename");
            HwFingerprintService.super.rename(fingerId, groupId, name);
        }

        public List<Fingerprint> getEnrolledFingerprints(int userId, String opPackageName) {
            Flog.i(1303, "FingerprintService getEnrolledFingerprints");
            if (!HwFingerprintService.this.canUseBiometric(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId(), false)) {
                return Collections.emptyList();
            }
            if (!HwFingerprintService.this.getBiometricUtils().isDualFp()) {
                return HwFingerprintService.super.getEnrolledFingerprints(userId, opPackageName);
            }
            Slog.d(HwFingerprintService.TAG, "dualFingerprint getEnrolledFingerprints and userId is: " + userId);
            return HwFingerprintService.this.getEnrolledFingerprintsEx(opPackageName, -1, userId);
        }

        public boolean hasEnrolledFingerprints(int userId, String opPackageName) {
            Flog.i(1303, "FingerprintService hasEnrolledFingerprints");
            boolean hasEnrollFingerprints = false;
            if (!HwFingerprintService.this.canUseBiometric(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId(), false)) {
                return false;
            }
            if (HwFingerprintService.this.getBiometricUtils().isDualFp()) {
                Slog.d(HwFingerprintService.TAG, "dualFingerprint hasEnrolledFingerprints and userId is: " + userId);
                if (HwFingerprintService.this.getEnrolledFingerprintsEx(opPackageName, -1, userId).size() > 0) {
                    hasEnrollFingerprints = true;
                }
                Slog.d(HwFingerprintService.TAG, "dualFingerprint hasEnrolledFingerprints: " + hasEnrollFingerprints);
                return hasEnrollFingerprints;
            }
            if (userId != 0 && HwFingerprintService.this.isClonedProfile(userId)) {
                Log.i(HwFingerprintService.TAG, "Clone profile get Enrolled Fingerprints,change userid to 0");
                userId = 0;
            }
            return HwFingerprintService.super.hasEnrolledFingerprints(userId, opPackageName);
        }

        public long getAuthenticatorId(String opPackageName) {
            Flog.i(1303, "FingerprintService getAuthenticatorId");
            return HwFingerprintService.super.getAuthenticatorId(opPackageName);
        }

        public int getRemainingNum() {
            HwFingerprintService.this.checkPermissions();
            long token = Binder.clearCallingIdentity();
            int failedAttempts = 0;
            try {
                failedAttempts = HwFingerprintService.this.mFailedAttempts.get(ActivityManager.getCurrentUser(), 0);
            } catch (SecurityException e) {
                Slog.e(HwFingerprintService.TAG, "failed getCurrentUser");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
            Binder.restoreCallingIdentity(token);
            Slog.d(HwFingerprintService.TAG, " Remaining Num Attempts = " + (5 - failedAttempts));
            return 5 - failedAttempts;
        }

        public long getRemainingTime() {
            HwFingerprintService.this.checkPermissions();
            long now = SystemClock.elapsedRealtime();
            long nowToLockout = now - HwFingerprintService.this.mLockoutTime.get(ActivityManager.getCurrentUser(), 0);
            Slog.d(HwFingerprintService.TAG, "Remaining Time mLockoutTime = " + HwFingerprintService.this.mLockoutTime + "  now = " + now);
            if (nowToLockout <= 0 || nowToLockout >= HwArbitrationDEFS.DelayTimeMillisA) {
                return 0;
            }
            return HwArbitrationDEFS.DelayTimeMillisA - nowToLockout;
        }

        public void addLockoutResetCallback(IBiometricServiceLockoutResetCallback callback) throws RemoteException {
            if (callback == null) {
                Log.e(HwFingerprintService.TAG, " FingerprintServiceLockoutResetCallback is null, cannot addLockoutResetMonitor, return");
            } else {
                HwFingerprintService.super.addLockoutResetCallback(callback);
            }
        }
    }

    private final class HwFingerprintAuthClient extends FingerprintAuthClient {
        public HwFingerprintAuthClient(Context context, DaemonWrapper daemon, long halDeviceId, IBinder token, ServiceListener listener, int targetUserId, int groupId, long opId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int flag) {
            super(HwFingerprintService.this, context, daemon, halDeviceId, token, listener, targetUserId, groupId, opId, restricted, owner, cookie, requireConfirmation);
            this.mPackageName = owner;
            this.mFlags = flag;
        }

        public void handleHwFailedAttempt(int flags, String packagesName) {
            HwFingerprintService.this.handleHwFailedAttempt(flags, packagesName);
        }

        public boolean onAuthenticated(BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> token) {
            if (getListener() != null) {
                if (!authenticated) {
                    Log.e("BiometricStats", "onAuthenticated,fail ,mHwFailedAttempts = " + HwFingerprintService.this.mHwFailedAttempts);
                    HwFingerprintService.this.notifyAuthenticationFinished(this.mPackageName, 1, HwFingerprintService.this.mHwFailedAttempts + 1);
                    if (HwFingerprintService.this.auTime - HwFingerprintService.this.downTime > 0) {
                        Context access$2700 = HwFingerprintService.this.mContext;
                        Flog.bdReport(access$2700, 7, "{CostTime:" + (HwFingerprintService.this.auTime - HwFingerprintService.this.downTime) + ",pkg:" + HwFingerprintService.this.opPackageName + ",DeviceType:" + HwFingerprintService.this.mCurrentAuthFpDev + "}");
                        Log.i("BiometricStats", "onAuthenticated fail:{CostTime:" + (HwFingerprintService.this.auTime - HwFingerprintService.this.downTime) + ",pkg:" + HwFingerprintService.this.opPackageName + ",DeviceType:" + HwFingerprintService.this.mCurrentAuthFpDev + "}");
                    } else {
                        Log.i("BiometricStats", "Fingerprint authenticate time less than equal to or equal to Fingerprint down time");
                    }
                    if (isScreenOn(getContext())) {
                        handleHwFailedAttempt(this.mFlags, this.mPackageName);
                    }
                    try {
                        HwFingerprintService.this.mStatusBarService.onBiometricHelp(getContext().getResources().getString(33685688));
                    } catch (RemoteException e) {
                        Log.e("BiometricStats", "onBiometricHelp fail");
                    }
                } else {
                    Log.e("BiometricStats", "onAuthenticated, pass");
                    HwFingerprintService.this.notifyAuthenticationFinished(this.mPackageName, 0, 0);
                    Context access$27002 = HwFingerprintService.this.mContext;
                    Flog.bdReport(access$27002, 8, "{pkg:" + HwFingerprintService.this.opPackageName + ",ErrorCount:" + HwFingerprintService.this.mHwFailedAttempts + ",DeviceType:" + HwFingerprintService.this.mCurrentAuthFpDev + "}");
                    Log.i("BiometricStats", "onAuthenticated success:{pkg:" + HwFingerprintService.this.opPackageName + ",ErrorCount:" + HwFingerprintService.this.mHwFailedAttempts + ",DeviceType:" + HwFingerprintService.this.mCurrentAuthFpDev + "}");
                    try {
                        HwFingerprintService.this.mStatusBarService.onBiometricAuthenticated(authenticated, (String) null);
                    } catch (RemoteException e2) {
                        Log.e("BiometricStats", "Failed to notify Authenticated");
                    }
                }
            }
            return HwFingerprintService.super.onAuthenticated(identifier, authenticated, token);
        }

        public int handleFailedAttempt() {
            int result = HwFingerprintService.super.handleFailedAttempt();
            boolean noNeedAddFailedAttemps = false;
            if ((this.mFlags & HwFingerprintService.HW_FP_NO_COUNT_FAILED_ATTEMPS) != 0 && "com.android.settings".equals(getOwnerString())) {
                noNeedAddFailedAttemps = true;
                Slog.i("BiometricStats", "no need count failed attempts");
            }
            int currentUser = ActivityManager.getCurrentUser();
            if (noNeedAddFailedAttemps) {
                HwFingerprintService.this.mFailedAttempts.put(currentUser, HwFingerprintService.this.mFailedAttempts.get(currentUser, 0) - 1);
            }
            if (result == 0 || !HwFingerprintService.this.isKeyguard(this.mPackageName)) {
                return result;
            }
            return 0;
        }

        public boolean inLockoutMode() {
            return HwFingerprintService.this.inLockoutMode();
        }

        public void resetFailedAttempts() {
            if (inLockoutMode()) {
                Slog.v(HwFingerprintService.this.getTag(), "resetFailedAttempts should be called from APP");
            } else {
                HwFingerprintService.super.resetFailedAttempts();
            }
        }

        public void onStart() {
            HwFingerprintService.super.onStart();
        }

        public void onStop() {
            HwFingerprintService.this.notifyAuthenticationCanceled(this.mPackageName);
            HwFingerprintService.super.onStop();
        }
    }

    /* access modifiers changed from: private */
    public void stopFidoClient(IBinder token) {
        ClientMonitor currentClient = this.mCurrentClient;
        if (currentClient != null) {
            if (!(currentClient instanceof AuthenticationClient)) {
                Log.v(TAG, "can't cancel non-authenticating client" + currentClient.getOwnerString());
            } else if (currentClient.getToken() == token) {
                Log.v(TAG, "stop client " + currentClient.getOwnerString());
                currentClient.stop(true);
            } else {
                Log.v(TAG, "can't stop client " + currentClient.getOwnerString() + " since tokens don't match");
            }
        }
    }

    private class FingerViewChangeCallback implements FingerViewController.ICallBack {
        private FingerViewChangeCallback() {
        }

        @Override // huawei.com.android.server.fingerprint.FingerViewController.ICallBack
        public void onFingerViewStateChange(int type) {
            Log.d(HwFingerprintService.TAG, "View State Change to " + type);
            HwFingerprintService.this.suspendAuthentication(type == 2 ? 1 : 0);
        }

        @Override // huawei.com.android.server.fingerprint.FingerViewController.ICallBack
        public void onNotifyCaptureImage() {
            HwFingerprintService.this.notifyCaptureOpticalImage();
        }

        @Override // huawei.com.android.server.fingerprint.FingerViewController.ICallBack
        public void onNotifyBlueSpotDismiss() {
            HwFingerprintService.this.notifyBluespotDismiss();
        }
    }

    private class HwFIDOAuthenticationClient extends AuthenticationClient {
        private String aaid;
        private int groupId;
        /* access modifiers changed from: private */
        public IAuthenticatorListener listener;
        private IFidoAuthenticationCallback mFidoAuthenticationCallback = new IFidoAuthenticationCallback.Stub() {
            /* class com.android.server.fingerprint.HwFingerprintService.HwFIDOAuthenticationClient.AnonymousClass1 */

            @Override // vendor.huawei.hardware.biometrics.fingerprint.V2_1.IFidoAuthenticationCallback
            public void onUserVerificationResult(final int result, long opId, final ArrayList<Byte> userId, final ArrayList<Byte> encapsulatedResult) {
                Log.d(HwFingerprintService.TAG, "onUserVerificationResult");
                HwFingerprintService.this.mHandler.post(new Runnable() {
                    /* class com.android.server.fingerprint.HwFingerprintService.HwFIDOAuthenticationClient.AnonymousClass1.AnonymousClass1 */

                    public void run() {
                        Log.d(HwFingerprintService.TAG, "onUserVerificationResult-run");
                        HwFingerprintService.this.resumeTouchSwitch();
                        if (HwFIDOAuthenticationClient.this.listener != null) {
                            try {
                                byte[] byteUserId = new byte[userId.size()];
                                int userIdLen = userId.size();
                                for (int i = 0; i < userIdLen; i++) {
                                    byteUserId[i] = ((Byte) userId.get(i)).byteValue();
                                }
                                byte[] byteEncapsulatedResult = new byte[encapsulatedResult.size()];
                                int encapsulatedResultLen = encapsulatedResult.size();
                                for (int i2 = 0; i2 < encapsulatedResultLen; i2++) {
                                    byteEncapsulatedResult[i2] = ((Byte) encapsulatedResult.get(i2)).byteValue();
                                }
                                HwFIDOAuthenticationClient.this.listener.onUserVerificationResult(result, byteUserId, byteEncapsulatedResult);
                            } catch (RemoteException e) {
                                Log.w(HwFingerprintService.TAG, "onUserVerificationResult RemoteException");
                            }
                        }
                    }
                });
            }
        };
        private byte[] nonce;
        private String pkgName;
        private int user_id;

        public HwFIDOAuthenticationClient(Context context, Constants metrics, DaemonWrapper daemon, long halDeviceId, IBinder token, ServiceListener listener2, int callingUserId, int targetUserId, int groupId2, long opId, boolean restricted, String owner, IAuthenticatorListener authenticatorListener, boolean requireConfirmation, String aaid2, byte[] nonce2) {
            super(context, metrics, daemon, halDeviceId, token, listener2, targetUserId, groupId2, opId, restricted, owner, 0, requireConfirmation);
            this.pkgName = owner;
            this.listener = authenticatorListener;
            this.groupId = groupId2;
            this.aaid = aaid2;
            this.nonce = nonce2;
            this.user_id = callingUserId;
        }

        public boolean wasUserDetected() {
            return false;
        }

        public boolean onAuthenticated(BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> token) {
            if (!authenticated) {
                if (isScreenOn(getContext())) {
                    handleHwFailedAttempt(this.mFlags, this.pkgName);
                }
                try {
                    HwFingerprintService.this.mStatusBarService.onBiometricHelp(getContext().getResources().getString(33685688));
                } catch (RemoteException e) {
                    Log.e("BiometricStats", "onBiometricHelp fail");
                }
            } else {
                try {
                    HwFingerprintService.this.mStatusBarService.onBiometricAuthenticated(authenticated, (String) null);
                } catch (RemoteException e2) {
                    Log.e("BiometricStats", "Failed to notify Authenticated");
                }
            }
            return HwFingerprintService.super.onAuthenticated(identifier, authenticated, token);
        }

        public String getErrorString(int error, int vendorCode) {
            return FingerprintManager.getErrorString(getContext(), error, vendorCode);
        }

        public String getAcquiredString(int acquireInfo, int vendorCode) {
            return FingerprintManager.getAcquiredString(getContext(), acquireInfo, vendorCode);
        }

        public int getBiometricType() {
            return 1;
        }

        public boolean shouldFrameworkHandleLockout() {
            return true;
        }

        /* access modifiers changed from: protected */
        public int statsModality() {
            return 0;
        }

        public int handleFailedAttempt() {
            int currentUser = ActivityManager.getCurrentUser();
            HwFingerprintService.this.mFailedAttempts.put(currentUser, HwFingerprintService.this.mFailedAttempts.get(currentUser, 0) + 1);
            HwFingerprintService.this.mTimedLockoutCleared.put(ActivityManager.getCurrentUser(), false);
            int lockoutMode = HwFingerprintService.this.getLockoutMode();
            if (!inLockoutMode()) {
                return 0;
            }
            HwFingerprintService.this.mLockoutTime.put(currentUser, SystemClock.elapsedRealtime());
            Class targetClass = HwFingerprintService.this.getClass().getSuperclass();
            Object unused = HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, targetClass, "scheduleLockoutResetForUser", new Class[]{Integer.TYPE}, new Object[]{Integer.valueOf(this.user_id)});
            onError(getHalDeviceId(), 7, 0);
            stop(true);
            return lockoutMode;
        }

        public void resetFailedAttempts() {
            HwFingerprintService.this.resetFailedAttemptsForUser(true, this.user_id);
        }

        public void notifyUserActivity() {
            Object unused = HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, HwFingerprintService.this.getClass().getSuperclass().getSuperclass(), "userActivity", null, null);
        }

        public IBiometricsFingerprint getFingerprintDaemon() {
            return HwFingerprintService.this.getFingerprintDaemon();
        }

        public void handleHwFailedAttempt(int flags, String packagesName) {
            HwFingerprintService.this.handleHwFailedAttempt(0, null);
        }

        public boolean inLockoutMode() {
            return HwFingerprintService.this.inLockoutMode();
        }

        public int start() {
            Slog.d("BiometricStats", "start pkgName:" + this.pkgName);
            try {
                doVerifyUser(this.groupId, this.aaid, this.nonce);
                return 0;
            } catch (RemoteException e) {
                Log.w(HwFingerprintService.TAG, "call fingerprintD verify user failed");
                return 0;
            }
        }

        public void onStart() {
        }

        public void onStop() {
        }

        private void doVerifyUser(int groupId2, String aaid2, byte[] nonce2) throws RemoteException {
            if (HwFingerprintService.this.isFingerprintDReady()) {
                IExtBiometricsFingerprint daemon = HwFingerprintService.this.getFingerprintDaemonEx();
                if (daemon == null) {
                    Slog.e("BiometricStats", "Fingerprintd is not available!");
                    return;
                }
                ArrayList<Byte> arrayNonce = new ArrayList<>();
                for (byte b : nonce2) {
                    arrayNonce.add(Byte.valueOf(b));
                }
                try {
                    daemon.verifyUser(this.mFidoAuthenticationCallback, groupId2, aaid2, arrayNonce);
                } catch (RemoteException e) {
                    Slog.e("BiometricStats", "doVerifyUser RemoteException:" + e);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public static Object invokeParentPrivateFunction(Object instance, Class targetClass, String method, Class[] paramTypes, Object[] params) {
        Object superInst = targetClass.cast(instance);
        try {
            final Method med = targetClass.getDeclaredMethod(method, paramTypes);
            AccessController.doPrivileged(new PrivilegedAction() {
                /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass2 */

                @Override // java.security.PrivilegedAction
                public Object run() {
                    med.setAccessible(true);
                    return null;
                }
            });
            return med.invoke(superInst, params);
        } catch (Exception e) {
            Log.v(TAG, "invokeParentPrivateFunction error", e);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void showDialog(final boolean withConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext, 33947691).setPositiveButton(withConfirm ? 33686174 : 33686171, new DialogInterface.OnClickListener() {
            /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass5 */

            public void onClick(DialogInterface dialog, int which) {
                boolean unused = HwFingerprintService.mNeedRecreateDialog = false;
                if (withConfirm) {
                    HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_DEL_OLD_TEMPLATES);
                    if (HwFingerprintService.this.checkNeedReEnrollFingerPrints() == 1) {
                        HwFingerprintService.this.updateActiveGroupEx(-100);
                        HwFingerprintService.this.updateActiveGroupEx(0);
                    }
                }
                HwFingerprintService hwFingerprintService = HwFingerprintService.this;
                hwFingerprintService.intentOthers(hwFingerprintService.mContext);
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass4 */

            public void onDismiss(DialogInterface dialog) {
                if (!HwFingerprintService.mNeedRecreateDialog) {
                    HwFingerprintService.this.unRegisterPhoneStateReceiver();
                }
            }
        }).setTitle(this.mContext.getString(33685797)).setMessage(this.mContext.getString(withConfirm ? 33686173 : 33686172));
        if (withConfirm) {
            builder.setNegativeButton(17039360, new DialogInterface.OnClickListener() {
                /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass6 */

                public void onClick(DialogInterface dialog, int which) {
                    boolean unused = HwFingerprintService.mNeedRecreateDialog = false;
                    HwFingerprintService.this.mReEnrollDialog.dismiss();
                }
            });
        }
        this.mReEnrollDialog = builder.create();
        AlertDialog alertDialog = this.mReEnrollDialog;
        if (alertDialog != null) {
            alertDialog.getWindow().setType(2003);
            this.mReEnrollDialog.setCanceledOnTouchOutside(false);
            this.mReEnrollDialog.setCancelable(false);
            this.mReEnrollDialog.show();
        }
        registerPhoneStateReceiver();
    }

    private void registerPhoneStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        this.mReceiver = new BroadcastReceiver() {
            /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass7 */

            public void onReceive(Context context, Intent intent) {
                TelephonyManager telephonyManager;
                if (intent.getAction() != null && (telephonyManager = (TelephonyManager) context.getSystemService("phone")) != null && HwFingerprintService.this.mReEnrollDialog != null) {
                    if (telephonyManager.getCallState() == 1 && HwFingerprintService.this.mReEnrollDialog.isShowing()) {
                        boolean unused = HwFingerprintService.mNeedRecreateDialog = true;
                        HwFingerprintService.this.mReEnrollDialog.dismiss();
                    } else if (telephonyManager.getCallState() == 0 && !HwFingerprintService.this.mReEnrollDialog.isShowing()) {
                        HwFingerprintService.this.mReEnrollDialog.show();
                    }
                }
            }
        };
        Context context = this.mContext;
        if (context != null) {
            context.registerReceiver(this.mReceiver, filter);
        }
    }

    /* access modifiers changed from: private */
    public void unRegisterPhoneStateReceiver() {
        Context context;
        BroadcastReceiver broadcastReceiver = this.mReceiver;
        if (broadcastReceiver != null && (context = this.mContext) != null) {
            context.unregisterReceiver(broadcastReceiver);
        }
    }

    /* access modifiers changed from: private */
    public void updateActiveGroupEx(int userId) {
        File systemDir;
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon != null) {
            int userIdForHal = userId;
            try {
                UserInfoEx infoEx = UserManagerEx.getUserInfoEx(UserManager.get(this.mContext), userId);
                if (infoEx != null && UserManagerEx.isHwHiddenSpace(infoEx)) {
                    userIdForHal = -100;
                    Slog.i(TAG, "userIdForHal is " + -100);
                }
                if (userIdForHal == -100) {
                    Slog.i(TAG, "userIdForHal == HIDDEN_SPACE_ID");
                    systemDir = Environment.getUserSystemDirectory(0);
                } else {
                    systemDir = Environment.getUserSystemDirectory(userId);
                }
                File fpDir = new File(systemDir, "fpdata");
                if (!fpDir.exists()) {
                    if (!fpDir.mkdir()) {
                        Slog.v(TAG, "Cannot make directory: " + fpDir.getAbsolutePath());
                        return;
                    } else if (!SELinux.restorecon(fpDir)) {
                        Slog.w(TAG, "Restorecons failed. Directory will have wrong label.");
                        return;
                    }
                }
                daemon.setActiveGroup(userIdForHal, fpDir.getAbsolutePath());
                updateFingerprints(userId);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to setActiveGroup():", e);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleScreenOnOrOff() {
        this.mHandler.removeCallbacks(this.screenOnOrOffRunnable);
        this.mHandler.post(this.screenOnOrOffRunnable);
    }

    private boolean isBetaUser() {
        int userType = SystemProperties.getInt("ro.logsystem.usertype", 0);
        if (userType == 3 || userType == 5) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void intentOthers(Context context) {
        Intent intent = new Intent();
        if (SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false)) {
            intent.setAction("com.android.settings.fingerprint.FingerprintSettings");
        } else {
            intent.setAction("com.android.settings.fingerprint.FingerprintMainSettings");
        }
        intent.setPackage("com.android.settings");
        intent.addFlags(268435456);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Activity not found");
        }
    }

    public HwFingerprintService(Context context) {
        super(context);
        this.mContext = context;
        this.mDisplayEngineManager = new DisplayEngineManager();
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        ServiceThread fingerprintThread = new ServiceThread("fingerprintServcie", -8, false);
        fingerprintThread.start();
        this.mFingerHandler = new Handler(fingerprintThread.getLooper()) {
            /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass9 */

            public void handleMessage(Message msg) {
                if (msg.what != 87) {
                    Slog.w(HwFingerprintService.TAG, "Unknown message:" + msg.what);
                    return;
                }
                Slog.i(HwFingerprintService.TAG, "MSG_NOTIFY_AUTHENCATION");
                HwFingerprintService.this.handlerNotifyAuthencation(msg);
            }
        };
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        initPropHwFpType();
        this.mFpDataCollector = FingerprintDataInterface.getInstance();
    }

    private void initFpIdentifyViewObserver() {
        if (this.setFpIdentifyViewObserver == null) {
            Log.w(TAG, "setFpIdentifyViewObserver is null");
            return;
        }
        Log.i(TAG, "initFpIdentifyViewObserver");
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(SET_FINGERPRINT_IDENTIFY), false, this.setFpIdentifyViewObserver, -1);
    }

    /* access modifiers changed from: private */
    public void handlerNotifyAuthencation(Message msg) {
        String packageName;
        Bundle data = msg.getData();
        if (data == null) {
            Log.w(TAG, "handlerNotifyAuthencation, getData is null");
            return;
        }
        int userId = data.getInt(PermConst.USER_ID);
        Bundle bundle = data.getBundle("bundle");
        if (bundle != null) {
            packageName = bundle.getString("packagename");
        } else {
            packageName = "";
        }
        notifyAuthenticationStarted(packageName, null, 0, userId, bundle, null, true);
    }

    private void initObserver() {
        if (this.fpObserver == null) {
            Log.w(TAG, "fpObserver is null");
            return;
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_KEYGUARD_ENABLE), false, this.fpObserver, -1);
        this.mState = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
        this.fpObserver.onChange(true);
    }

    private void updateTPState(String opPackageName2, boolean udFingerprintExist) {
        if (!"com.android.systemui".equals(opPackageName2)) {
            sendCommandToHal(1);
        } else if (!udFingerprintExist || this.mState == 0) {
            sendCommandToHal(0);
        } else {
            sendCommandToHal(1);
        }
        Log.i(TAG, "updateTPState udFingerprintExist " + udFingerprintExist + ",opPackageName:" + opPackageName2);
    }

    private void initNavModeObserver() {
        if (this.mNavModeObserver == null) {
            Log.d(TAG, "mNavModeObserver is null");
            return;
        }
        Context context = this.mContext;
        if (context == null || context.getContentResolver() == null) {
            Log.d(TAG, "mContext or the resolver is null");
            return;
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("enable_navbar"), true, this.mNavModeObserver, -1);
        this.mVirNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(this.mContext.getContentResolver());
        Log.d(TAG, "Read the navigation mode after boot, mVirNavModeEnabled = " + this.mVirNavModeEnabled);
        sendCommandToHal(this.mVirNavModeEnabled ? 45 : 43);
    }

    private void initUserSwitchReceiver() {
        BroadcastReceiver broadcastReceiver = this.mUserSwitchReceiver;
        if (broadcastReceiver == null) {
            Log.d(TAG, "mUserSwitchReceiver is null");
            return;
        }
        Context context = this.mContext;
        if (context == null) {
            Log.d(TAG, "mContext is null");
        } else {
            context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED), null, null);
        }
    }

    public void onStart() {
        HwFingerprintService.super.onStart();
        publishBinderService("fido_authenticator", this.mIAuthenticator.asBinder());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mUserDeletedMonitor, filter);
        Slog.v(TAG, "HwFingerprintService onstart");
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass14 */

                public void onUserSwitching(int newUserId) {
                    if (HwFingerprintService.this.mFingerViewController != null) {
                        Slog.v(HwFingerprintService.TAG, "onUserSwitching removeMaskOrButton");
                        HwFingerprintService.this.mFingerViewController.removeMaskOrButton(true);
                        boolean unused = HwFingerprintService.this.mForbideKeyguardCall = true;
                    }
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    Slog.v(HwFingerprintService.TAG, "onUserSwitchComplete ");
                    boolean unused = HwFingerprintService.this.mForbideKeyguardCall = false;
                    if (HwFingerprintService.this.fpObserver != null) {
                        HwFingerprintService.this.fpObserver.onChange(true);
                    }
                }
            }, TAG);
        } catch (RemoteException e) {
            Slog.e(TAG, "registerUserSwitchObserver fail", e);
        } catch (SecurityException e2) {
            Slog.w(TAG, "registerReceiverAsUser fail ", e2);
        } catch (Throwable th) {
            this.mForbideKeyguardCall = false;
            throw th;
        }
        this.mForbideKeyguardCall = false;
    }

    private int getSwitchFrequenceSupport() {
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        try {
            return daemon.sendCmdToHal(81);
        } catch (RemoteException e) {
            Slog.e(TAG, "checkSwitchFrequenceSupport RemoteException:" + e);
            return -1;
        }
    }

    public void onBootPhase(int phase) {
        HwFingerprintService.super.onBootPhase(phase);
        Slog.v(TAG, "HwFingerprintService onBootPhase:" + phase);
        if (phase == 1000) {
            initSwitchFrequence();
            initPositionAndType();
            if (mRemoveFingerprintBGE) {
                initUserSwitchReceiver();
                initNavModeObserver();
            }
            getAodFace().sendFaceStatusToHal(true);
            connectToProxy();
            startCurrentClient();
        }
    }

    private void initSwitchFrequence() {
        if (getSwitchFrequenceSupport() == 1) {
            IntentFilter filterScreen = new IntentFilter();
            filterScreen.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
            filterScreen.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
            this.mContext.registerReceiver(this.mSwitchFrequenceMonitor, filterScreen);
        }
    }

    public void updateFingerprints(int userId) {
        if (FingerprintUtils.getInstance().isDualFp()) {
            Slog.d(TAG, "dualFingerprint-> updateFingerprints");
            sendCommandToHal(106);
            refreshData(remoteGetOldData(), userId, 1);
            refreshData(remoteGetOldData(), userId, 0);
            return;
        }
        refreshData(remoteGetOldData(), userId, 0);
    }

    private void refreshData(HwFingerprintSets hwFpSets, int userId, int deviceIndex) {
        FingerprintUtils utils;
        if (hwFpSets != null && (utils = FingerprintUtils.getInstance()) != null) {
            ArrayList<Fingerprint> mNewFingerprints = null;
            int fingerprintGpSize = hwFpSets.mFingerprintGroups.size();
            for (int i = 0; i < fingerprintGpSize; i++) {
                HwFingerprintSets.HwFingerprintGroup fpGroup = hwFpSets.mFingerprintGroups.get(i);
                int realGroupId = fpGroup.mGroupId;
                if (fpGroup.mGroupId == -100) {
                    realGroupId = getRealUserIdForApp(fpGroup.mGroupId);
                }
                if (realGroupId == userId) {
                    mNewFingerprints = fpGroup.mFingerprints;
                }
            }
            if (mNewFingerprints == null) {
                mNewFingerprints = new ArrayList<>();
            }
            for (Fingerprint oldFp : utils.isDualFp() ? utils.getFingerprintsForUser(this.mContext, userId, deviceIndex) : utils.getBiometricsForUser(this.mContext, userId)) {
                if (!checkItemExist(oldFp.getBiometricId(), mNewFingerprints)) {
                    utils.removeFingerprintIdForUser(this.mContext, oldFp.getBiometricId(), userId);
                }
            }
            int size = mNewFingerprints.size();
            for (int i2 = 0; i2 < size; i2++) {
                Fingerprint fp = mNewFingerprints.get(i2);
                if (utils.isDualFp()) {
                    utils.addFingerprintForUser(this.mContext, fp.getBiometricId(), userId, deviceIndex);
                } else {
                    utils.addBiometricForUser(this.mContext, userId, fp);
                }
                CharSequence fpName = fp.getName();
                if (fpName != null && !fpName.toString().isEmpty()) {
                    utils.renameFingerprintForUser(this.mContext, fp.getBiometricId(), userId, fpName);
                }
            }
        }
    }

    public boolean checkPrivacySpaceEnroll(int userId, int currentUserId) {
        if (!UserManagerEx.isHwHiddenSpace(UserManagerEx.getUserInfoEx(UserManager.get(this.mContext), userId)) || currentUserId != 0) {
            return false;
        }
        Slog.v(TAG, "enroll privacy fingerprint in primary user ");
        return true;
    }

    public boolean checkNeedPowerpush() {
        if (this.mflagFirstIn) {
            this.mtimeStart = System.currentTimeMillis();
            this.mflagFirstIn = false;
            return true;
        }
        long timePassed = System.currentTimeMillis() - this.mtimeStart;
        Slog.v(TAG, "timepassed is  " + timePassed);
        this.mtimeStart = System.currentTimeMillis();
        return POWER_PUSH_DOWN_TIME_THR < timePassed;
    }

    public int removeUserData(int groupId, String storePath) {
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        try {
            daemon.removeUserData(groupId, storePath);
            return 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "checkNeedReEnrollFingerPrints RemoteException:" + e);
            return 0;
        }
    }

    /* access modifiers changed from: private */
    public int checkNeedReEnrollFingerPrints() {
        int result = -1;
        Log.w(TAG, "checkNeedReEnrollFingerPrints");
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        try {
            result = daemon.checkNeedReEnrollFinger();
        } catch (RemoteException e) {
            Slog.e(TAG, "checkNeedReEnrollFingerPrints RemoteException:" + e);
        }
        Log.w(TAG, "framework checkNeedReEnrollFingerPrints is finish return = " + result);
        return result;
    }

    public boolean onHwTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == CODE_IS_FP_NEED_CALIBRATE_RULE) {
            Slog.d(TAG, "code == CODE_IS_FP_NEED_CALIBRATE_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result = checkNeedCalibrateFingerPrint();
            reply.writeNoException();
            reply.writeInt(result);
            return true;
        } else if (code == CODE_SET_CALIBRATE_MODE_RULE) {
            Slog.d(TAG, "code == CODE_SET_CALIBRATE_MODE_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            setCalibrateMode(data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == CODE_GET_TOKEN_LEN_RULE) {
            Slog.d(TAG, "code == CODE_GET_TOKEN_LEN_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result2 = getTokenLen();
            reply.writeNoException();
            reply.writeInt(result2);
            return true;
        } else if (code == CODE_SET_FINGERPRINT_MASK_VIEW_RULE) {
            Slog.d(TAG, "code == CODE_SET_FINGERPRINT_MASK_VIEW_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            setFingerprintMaskView(data.readBundle());
            reply.writeNoException();
            return true;
        } else if (code == CODE_SHOW_FINGERPRINT_VIEW_RULE) {
            Slog.d(TAG, "code == CODE_SHOW_FINGERPRINT_VIEW_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            showFingerprintView();
            reply.writeNoException();
            return true;
        } else if (code == 1106) {
            Slog.d(TAG, "code == CODE_SHOW_FINGERPRINT_BUTTON_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            showSuspensionButton(data.readInt(), data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == 1107) {
            Slog.d(TAG, "code == CODE_REMOVE_FINGERPRINT_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            removeFingerprintView();
            reply.writeNoException();
            return true;
        } else if (code == CODE_GET_HARDWARE_POSITION) {
            Slog.d(TAG, "CODE_GET_HARDWARE_POSITION");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int[] result3 = getFingerprintHardwarePosition();
            reply.writeNoException();
            reply.writeInt(result3[0]);
            reply.writeInt(result3[1]);
            reply.writeInt(result3[2]);
            reply.writeInt(result3[3]);
            return true;
        } else if (code == CODE_FINGERPRINT_LOGO_POSITION) {
            Slog.d(TAG, "CODE_FINGERPRINT_LOGO_POSITION");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int[] result4 = getFingerprintLogoPosition();
            reply.writeNoException();
            reply.writeInt(result4[0]);
            reply.writeInt(result4[1]);
            reply.writeInt(result4[2]);
            reply.writeInt(result4[3]);
            return true;
        } else if (code == CODE_GET_HARDWARE_TYPE) {
            Slog.d(TAG, "CODE_GET_HARDWARE_TYPE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result5 = getFingerprintHardwareType();
            reply.writeNoException();
            reply.writeInt(result5);
            return true;
        } else if (code == CODE_NOTIFY_OPTICAL_CAPTURE) {
            Slog.d(TAG, "CODE_NOTIFY_OPTICAL_CAPTURE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            notifyCaptureOpticalImage();
            reply.writeNoException();
            return true;
        } else if (code == 1108) {
            Slog.d(TAG, "CODE_SUSPEND_AUTHENTICATE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            suspendAuthentication(data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == CODE_SET_HOVER_SWITCH) {
            Slog.d(TAG, "CODE_SET_HOVER_SWITCH");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            setHoverEventSwitch(data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == CODE_GET_HOVER_SUPPORT) {
            Slog.d(TAG, "CODE_GET_HOVER_SUPPORT");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result6 = getHoverEventSupport();
            reply.writeNoException();
            reply.writeInt(result6);
            return true;
        } else if (code == CODE_DISABLE_FINGERPRINT_VIEW_RULE) {
            Slog.d(TAG, "CODE_DISABLE_FINGERPRINT_VIEW_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            disableFingerprintView(data.readBoolean());
            reply.writeNoException();
            return true;
        } else if (code == CODE_ENABLE_FINGERPRINT_VIEW_RULE) {
            Slog.d(TAG, "CODE_ENABLE_FINGERPRINT_VIEW_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            enableFingerprintView(data.readBoolean(), data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE) {
            Slog.d(TAG, "CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            keepMaskShowAfterAuthentication();
            reply.writeNoException();
            return true;
        } else if (code == 1117) {
            Slog.d(TAG, "CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            removeMaskAndShowButton();
            reply.writeNoException();
            return true;
        } else if (code == CODE_IS_FINGERPRINT_HARDWARE_DETECTED) {
            Slog.d(TAG, "CODE_IS_FINGERPRINT_HARDWARE_DETECTED");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            boolean isHardwareDetected = isHardwareDetectedNoWhitelist(data.readString(), data.readInt());
            reply.writeNoException();
            reply.writeBoolean(isHardwareDetected);
            return true;
        } else if (code == 1118) {
            Slog.d(TAG, "CODE_GET_FINGERPRINT_LIST_ENROLLED");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            List<Fingerprint> fingerprints = getEnrolledFingerprintsNoWhitelist(data.readString(), data.readInt(), data.readInt());
            reply.writeNoException();
            reply.writeTypedList(fingerprints);
            return true;
        } else if (code == CODE_IS_SUPPORT_DUAL_FINGERPRINT) {
            Slog.d(TAG, "CODE_IS_SUPPORT_DUAL_FINGERPRINT");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            reply.writeNoException();
            reply.writeBoolean(isSupportDualFingerprint());
            return true;
        } else if (code == CODE_SEND_UNLOCK_LIGHTBRIGHT) {
            Slog.d(TAG, "CODE_SEND_UNLOCK_LIGHTBRIGHT");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result7 = sendUnlockAndLightbright(data.readInt());
            reply.writeNoException();
            reply.writeInt(result7);
            return true;
        } else if (code == CODE_GET_HIGHLIGHT_SPOT_RADIUS_RULE) {
            Slog.d(TAG, "CODE_GET_HIGHLIGHT_SPOT_RADIUS_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int radius = getHighLightspotRadius();
            reply.writeNoException();
            reply.writeInt(radius);
            return true;
        } else if (code == CODE_SUSPEND_ENROLL) {
            Slog.d(TAG, "CODE_SUSPEND_ENROLL");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result8 = suspendEnroll(data.readInt());
            reply.writeNoException();
            reply.writeInt(result8);
            return true;
        } else if (code == CODE_UDFINGERPRINT_SPOTCOLOR) {
            Slog.d(TAG, "CODE_UDFINGERPRINT_SPOTCOLOR");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int result9 = getSpotColor();
            reply.writeNoException();
            reply.writeInt(result9);
            return true;
        } else if (code == CODE_FINGERPRINT_FORBID_GOTOSLEEP) {
            Slog.i(TAG, "CODE_FINGERPRINT_FORBID_GOTOSLEEP");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            boolean isPowerFpForbidGotoSleep = isPowerFpForbidGotoSleep();
            reply.writeNoException();
            reply.writeBoolean(isPowerFpForbidGotoSleep);
            return true;
        } else if (code == CODE_POWER_KEYCODE) {
            Slog.d(TAG, "CODE_POWER_KEYCODE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int resultKeyCode = operPowerFpPowerKeyCode(data.readInt(), data.readBoolean(), data.readBoolean());
            reply.writeNoException();
            reply.writeInt(resultKeyCode);
            return true;
        } else if (code == 1127) {
            Slog.d(TAG, "CODE_IS_WAIT_AUTHEN");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            boolean isAuthenStatus = isKeyguardAuthenStatus();
            reply.writeNoException();
            reply.writeBoolean(isAuthenStatus);
            return true;
        } else if (code != CODE_FINGERPRINT_WEATHER_DATA) {
            return HwFingerprintService.super.onHwTransact(code, data, reply, flags);
        } else {
            Slog.d(TAG, "CODE_FP_ENV_DATA");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            setWeatherDataToHal(data.readInt(), data.readString());
            reply.writeNoException();
            reply.writeInt(HwSecDiagnoseConstant.OEMINFO_ID_DEVICE_RENEW);
            return true;
        }
    }

    private boolean onTransactToHal(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != CODE_FINGERPRINT_LOGO_RADIUS) {
            return false;
        }
        Slog.d(TAG, "CODE_FINGERPRINT_LOGO_RADIUS");
        data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
        int result = getFingerPrintLogoRadius();
        reply.writeNoException();
        reply.writeInt(result);
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isSupportDualFingerprint() {
        if (!(this.mIsSupportDualFingerprint || getFingerprintHardwareType() == -1 || (getFingerprintHardwareType() & 1) == 0 || (getFingerprintHardwareType() & 4) == 0)) {
            FingerprintUtils.getInstance().setDualFp(true);
            this.mIsSupportDualFingerprint = true;
            this.mWhitelist.add("com.android.settings");
            this.mWhitelist.add("com.android.systemui");
            this.mWhitelist.add("com.huawei.aod");
            this.mWhitelist.add("com.huawei.systemmanager");
            this.mWhitelist.add("com.huawei.hidisk");
            this.mWhitelist.add("com.huawei.wallet");
            this.mWhitelist.add("com.huawei.hwid");
            this.mWhitelist.add("com.huawei.systemserver");
            this.mWhitelist.add("com.android.cts.verifier");
            this.mWhitelist.add("com.eg.android.AlipayGphone");
            this.mWhitelist.add("com.tmall.wireless");
            this.mWhitelist.add("com.taobao.taobao");
            this.mWhitelist.add("com.alibaba.wireless");
            this.mWhitelist.add("com.taobao.trip");
            this.mWhitelist.add("com.taobao.idlefish");
            this.mWhitelist.add("com.taobao.mobile.dipei");
            this.mWhitelist.add("com.taobao.movie.android");
            this.mWhitelist.add("com.alibaba.wireless.microsupply");
            this.mWhitelist.add("com.alibaba.wireless.lstretailer");
            this.mWhitelist.add("com.wudaokou.hippo");
            this.mWhitelist.add("com.taobao.ju.android");
            this.mWhitelist.add("com.taobao.htao.android");
            this.mWhitelist.add("com.taobao.kepler");
            this.mWhitelist.add("com.taobao.shoppingstreets");
            this.mWhitelist.add("com.antfortune.wealth");
            this.mWhitelist.add("com.taobao.qianniu");
            this.mWhitelist.add("com.taobao.litetao");
            this.mWhitelist.add("com.taobao.auction");
            this.mWhitelist.add("com.alibaba.cun.assistant");
            this.mWhitelist.add("com.taobao.caipiao");
        }
        return this.mIsSupportDualFingerprint;
    }

    /* access modifiers changed from: protected */
    public int sendCommandToHal(int command) {
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            return -1;
        }
        try {
            return daemon.sendCmdToHal(command);
        } catch (RemoteException e) {
            Slog.e(TAG, "dualfingerprint sendCmdToHal RemoteException:" + e);
            return -1;
        }
    }

    /* access modifiers changed from: protected */
    public boolean canUseUdFingerprint(String opPackageName2) {
        String type;
        if (opPackageName2 == null || "".equals(opPackageName2)) {
            Slog.d(TAG, "calling opPackageName is invalid");
            return false;
        } else if (opPackageName2.equals(this.mDefinedAppName)) {
            return true;
        } else {
            Slog.d(TAG, "canUseUdFingerprint opPackageName is " + opPackageName2);
            Iterator<String> it = this.mWhitelist.iterator();
            while (it.hasNext()) {
                if (it.next().equals(opPackageName2)) {
                    return true;
                }
            }
            long token = Binder.clearCallingIdentity();
            try {
                Bundle metaData = this.mContext.getPackageManager().getApplicationInfo(opPackageName2, 128).metaData;
                if (metaData == null) {
                    Slog.d(TAG, "metaData is null");
                }
                if (!(metaData == null || (type = metaData.getString("fingerprint.system.view")) == null || "".equals(type))) {
                    Slog.d(TAG, "calling opPackageName  metaData value is: " + type);
                    Binder.restoreCallingIdentity(token);
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(TAG, "cannot find metaData of package: " + e);
            } catch (Exception e2) {
                Slog.e(TAG, "exception occured: " + e2);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
            Binder.restoreCallingIdentity(token);
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public List<Fingerprint> getEnrolledFingerprintsEx(String opPackageName2, int targetDevice, int userId) {
        if (userId != 0 && isClonedProfile(userId)) {
            Slog.d(TAG, "Clone profile get Enrolled Fingerprints,change userid to 0");
            userId = 0;
        }
        FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
        if (canUseUdFingerprint(opPackageName2)) {
            return fingerprintUtils.getFingerprintsForUser(this.mContext, userId, targetDevice);
        }
        if (targetDevice == 1) {
            return Collections.emptyList();
        }
        return fingerprintUtils.getFingerprintsForUser(this.mContext, userId, 0);
    }

    private List<Fingerprint> getEnrolledFingerprintsNoWhitelist(String opPackageName2, int targetDevice, int userId) {
        Slog.d(TAG, "dualFingerprint getEnrolledFingerprints opPackageName is " + opPackageName2 + " userId is " + userId);
        return FingerprintUtils.getInstance().getFingerprintsForUser(this.mContext, userId, targetDevice);
    }

    /* access modifiers changed from: protected */
    public boolean isHardwareDetectedEx(String opPackageName2, int targetDevice) {
        boolean z = false;
        if (getFingerprintDaemon() == null) {
            Slog.d(TAG, "Daemon is not available!");
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (canUseUdFingerprint(opPackageName2)) {
                if (targetDevice == 1) {
                    if (this.mUDHalDeviceId != 0) {
                        z = true;
                    }
                    return z;
                } else if (targetDevice == 0) {
                    if (this.mHalDeviceId != 0) {
                        z = true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                } else {
                    if (!(this.mHalDeviceId == 0 || this.mUDHalDeviceId == 0)) {
                        z = true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                }
            } else if (targetDevice == 0) {
                if (this.mHalDeviceId != 0) {
                    z = true;
                }
                Binder.restoreCallingIdentity(token);
                return z;
            } else {
                Binder.restoreCallingIdentity(token);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isHardwareDetectedNoWhitelist(String opPackageName2, int targetDevice) {
        if (getFingerprintDaemon() == null) {
            Slog.d(TAG, "Daemon is not available!");
            return false;
        }
        Slog.d(TAG, "dualFingerprint isHardwareDetected opPackageName is " + opPackageName2 + " targetDevice is " + targetDevice);
        long token = Binder.clearCallingIdentity();
        boolean z = true;
        if (targetDevice == 1) {
            try {
                if (this.mUDHalDeviceId == 0) {
                    z = false;
                }
                return z;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else if (targetDevice == 0) {
            if (this.mHalDeviceId == 0) {
                z = false;
            }
            Binder.restoreCallingIdentity(token);
            return z;
        } else {
            if (this.mHalDeviceId == 0 || this.mUDHalDeviceId == 0) {
                z = false;
            }
            Binder.restoreCallingIdentity(token);
            return z;
        }
    }

    public int checkNeedCalibrateFingerPrint() {
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        Slog.d(TAG, "pacel  packaged :checkNeedCalibrateFingerPrint");
        int result = -1;
        try {
            result = daemon.checkNeedCalibrateFingerPrint();
        } catch (RemoteException e) {
            Slog.e(TAG, "checkNeedCalibrateFingerPrint RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd calibrate return = " + result);
        return result;
    }

    public void setCalibrateMode(int mode) {
        if (!isFingerprintDReady()) {
            Log.w(TAG, "FingerprintD is not Ready");
            return;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return;
        }
        Slog.d(TAG, "pacel  packaged setCalibrateMode: " + mode);
        try {
            daemon.setCalibrateMode(mode);
        } catch (RemoteException e) {
            Slog.e(TAG, "setCalibrateMode RemoteException:" + e);
        }
    }

    public int getTokenLen() {
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        int result = -1;
        Slog.d(TAG, "pacel  packaged :getTokenLen");
        try {
            result = daemon.getTokenLen();
        } catch (RemoteException e) {
            Slog.e(TAG, "getTokenLen RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd getTokenLen token len = " + result);
        return result;
    }

    public void showFingerprintView() {
        initPositionAndType();
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            this.mFingerViewController.showMaskForApp(this.mMaskViewBundle);
        }
    }

    public void showSuspensionButton(int centerX, int centerY) {
        initPositionAndType();
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            this.mFingerViewController.showSuspensionButtonForApp(centerX, centerY, this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()));
        }
    }

    public void setFingerprintMaskView(Bundle bundle) {
        initPositionAndType();
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            if (bundle != null) {
                String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
                Log.d(TAG, "callingApp = " + callingApp);
                String pkgNamefromBundle = bundle.getString("PackageName");
                this.mMaskViewBundle = bundle;
                if (this.mFingerViewController == null) {
                    return;
                }
                if (!"com.android.systemui".equals(pkgNamefromBundle)) {
                    this.mFingerViewController.updateMaskViewAttributes(bundle, callingApp);
                } else if (!this.mForbideKeyguardCall) {
                    this.mFingerViewController.parseBundle4Keyguard(bundle);
                }
            }
        }
    }

    private boolean isStopAuthenticationStarted(String pkgName, int flag) {
        if (!this.mIsFingerInScreenSupported) {
            return true;
        }
        if (pkgName == null) {
            Log.d(TAG, "pkgname is null");
            return true;
        }
        checkPermissions();
        if (flag == 0 || (FLAG_USE_UD_FINGERPRINT & flag) != 0) {
            return false;
        }
        Log.d(TAG, "flag = " + flag);
        return true;
    }

    public void notifyAuthenticationStarted(String pkgName, IFingerprintServiceReceiver receiver, int flag, int userID, Bundle bundle, IBiometricServiceReceiver dialogReceiver, boolean isBiometricPrompt) {
        int initType;
        boolean hasBackFingerprint;
        boolean hasUdFingerprint;
        boolean hasUdFingerprint2;
        boolean z;
        initPositionAndType();
        if (this.mIsFingerInScreenSupported || isSupportPowerFp() || SUPPORT_INFORM_FACE) {
            getAodFace().checkIsPrimaryUser(userID, pkgName);
        }
        if (!isStopAuthenticationStarted(pkgName, flag)) {
            if ("com.android.systemui".equals(pkgName)) {
                notifyAuthenticationCanceled(pkgName);
            }
            if (bundle != null) {
                this.mMaskViewBundle = bundle;
                this.mMaskViewBundle.putString("googleFlag", "googleFlag");
            }
            Log.i(TAG, "show,pkgName =" + pkgName + " userID = " + userID);
            int initType2 = -1;
            if (pkgName.equals(this.mDefinedAppName)) {
                initType2 = this.mAppDefinedMaskType;
                Log.i(TAG, "initType = " + initType2 + ",defined by enable interface");
            }
            if (initType2 == -1) {
                initType2 = getAppType(pkgName);
            }
            if (initType2 == -1) {
                Iterator<String> it = this.mWhitelist.iterator();
                while (it.hasNext()) {
                    if (it.next().equals(pkgName)) {
                        Log.i(TAG, "pkgName in whitelist, show default mask for it");
                        initType2 = 0;
                    }
                }
            }
            if ("com.android.cts.verifier".equals(pkgName)) {
                initType2 = 0;
                Log.i(TAG, "com.android.cts.verifier initType = " + 0);
            }
            if (isBiometricPrompt && pkgName.equals("com.android.settings")) {
                initType2 = 0;
            }
            FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
            if (!fingerprintUtils.isDualFp() && initType2 == -1) {
                Log.i(TAG, "single inscreen");
                initType2 = 0;
            } else if (fingerprintUtils.isDualFp() && bundle != null) {
                initType2 = 4;
            }
            if (!fingerprintUtils.isDualFp()) {
                initType = adjustMaskTypeForWechat(initType2, pkgName);
            } else {
                initType = initType2;
            }
            Log.i(TAG, "final initType = " + initType);
            boolean hasBackFingerprint2 = false;
            if (fingerprintUtils.isDualFp()) {
                boolean hasUdFingerprint3 = fingerprintUtils.getFingerprintsForUser(this.mContext, userID, 1).size() > 0;
                if (fingerprintUtils.getFingerprintsForUser(this.mContext, userID, 0).size() > 0) {
                    hasBackFingerprint2 = true;
                }
                if (!hasUdFingerprint3) {
                    Log.d(TAG, "userID:" + userID + "has no UD_fingerprint");
                    if (!(initType == 3 || initType == 4)) {
                        return;
                    }
                }
                hasUdFingerprint = hasUdFingerprint3;
                hasBackFingerprint = hasBackFingerprint2;
            } else {
                if (fingerprintUtils.getBiometricsForUser(this.mContext, userID).size() > 0) {
                    hasBackFingerprint2 = true;
                }
                hasUdFingerprint = hasBackFingerprint2;
                hasBackFingerprint = false;
            }
            this.mFingerViewController.closeEyeProtecttionMode(pkgName);
            FingerprintViewUtils.setScreenRefreshRate(this.mContext, 2, pkgName, this.mIsFingerInScreenSupported, this.mFingerViewController.getHandler());
            if (!checkIfCallerDisableMask()) {
                Log.d(TAG, "dialogReceiver = " + dialogReceiver);
                this.mFingerViewController.setBiometricPrompt(isBiometricPrompt);
                hasUdFingerprint2 = hasUdFingerprint;
                z = true;
                this.mFingerViewController.showMaskOrButton(pkgName, this.mMaskViewBundle, receiver, initType, hasUdFingerprint, hasBackFingerprint, dialogReceiver);
            } else {
                hasUdFingerprint2 = hasUdFingerprint;
                z = true;
            }
            Log.i(TAG, " begin add windowservice view");
            LoadHighlightAndAnimOnKeyguard(pkgName);
            this.mIsUdAuthenticating = z;
            updateTPState(pkgName, hasUdFingerprint2);
        }
    }

    private void LoadHighlightAndAnimOnKeyguard(String pkgName) {
        if (this.mFingerViewController != null && "com.android.systemui".equals(pkgName)) {
            Log.i(TAG, "keyguard show highlight mask and fingerviewAim");
            this.mFingerViewController.showHighlightviewOnKeyguard();
            this.mFingerViewController.loadFingerviewAnimOnKeyguard();
        }
    }

    private int adjustMaskTypeForWechat(int initType, String pkgName) {
        if (PKGNAME_OF_WECHAT.equals(pkgName)) {
            return 3;
        }
        return initType;
    }

    public void notifyAuthenticationCanceled(String pkgName) {
        if (this.mIsFingerInScreenSupported && this.mFingerViewController != null) {
            Log.i(TAG, "notifyAuthenticationCanceled pkgName = " + pkgName);
            checkPermissions();
            this.mMaskViewBundle = null;
            if ("com.android.systemui".equals(pkgName)) {
                this.mFingerViewController.destroyHighlightviewOnKeyguard();
                this.mFingerViewController.destroyFingerAnimViewOnKeyguard();
            }
            if (!this.mKeepMaskAfterAuthentication) {
                Log.i(TAG, "mKeepMaskAfterAuthentication is false,start remove,pkgName = " + pkgName);
                this.mFingerViewController.removeMaskOrButton();
                this.mFingerViewController.reopenEyeProtecttionMode(pkgName);
                FingerprintViewUtils.setScreenRefreshRate(this.mContext, 0, pkgName, this.mIsFingerInScreenSupported, this.mFingerViewController.getHandler());
            }
            this.mIsUdAuthenticating = false;
            this.mKeepMaskAfterAuthentication = false;
            this.mAppDefinedMaskType = -1;
            this.mDefinedAppName = "";
        }
    }

    private void removeFingerprintView() {
        if (this.mIsFingerInScreenSupported) {
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.i(TAG, "removeFingerprintView,callingApp = " + callingApp);
            checkPermissions();
            this.mMaskViewBundle = null;
            this.mFingerViewController.removeMaskOrButton();
        }
    }

    public void notifyFingerDown(int type) {
        Log.i(TAG, "notifyFingerDown mIsFingerInScreenSupported:" + this.mIsFingerInScreenSupported);
        if (this.mIsFingerInScreenSupported) {
            if (this.mWindowManagerInternal == null) {
                this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            }
            WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
            if (windowManagerInternal == null || windowManagerInternal.isCoverOpen()) {
                checkPermissions();
                this.mIsUdFingerprintChecking = true;
                if (this.mIsHighLightNeed) {
                    if (type == 1) {
                        Log.i(TAG, "FINGER_DOWN_TYPE_AUTHENTICATING notifyFingerDown ");
                        long token = Binder.clearCallingIdentity();
                        try {
                            if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "show_touches", -2) == 1) {
                                Log.i(TAG, "turn off the show_touch switch when authenticating");
                                this.mNeedResumeTouchSwitch = true;
                                Settings.System.putIntForUser(this.mContext.getContentResolver(), "show_touches", 0, -2);
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "catch SecurityException");
                        } catch (Settings.SettingNotFoundException e2) {
                            Log.e(TAG, "settings show_touches not found");
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(token);
                            throw th;
                        }
                        Binder.restoreCallingIdentity(token);
                        this.mFingerViewController.showHighlightview(1);
                    } else if (type == 0) {
                        Log.i(TAG, "FINGER_DOWN_TYPE_ENROLLING notifyFingerDown ");
                        this.mFingerViewController.showHighlightCircle();
                    } else if (type == 3) {
                        Log.i(TAG, "FINGER_DOWN_TYPE_AUTHENTICATING_SYSTEMUI notifyFingerDown ");
                        this.mFingerViewController.showHighlightCircleOnKeyguard();
                    }
                }
                if (type == 1) {
                    this.mFingerViewController.updateFingerprintView(3, this.mKeepMaskAfterAuthentication);
                    return;
                }
                return;
            }
            Log.i(TAG, "mWindowManagerInternal.isCover added");
        }
    }

    public void notifyEnrollingFingerUp() {
        if (this.mIsFingerInScreenSupported && this.mIsUdEnrolling && this.mIsHighLightNeed) {
            Log.i(TAG, "notifyEnrollingFingerUp removeHighlightCircle");
            this.mFingerViewController.removeHighlightCircle();
        }
    }

    public void notifyCaptureFinished(int type) {
        FingerViewController fingerViewController;
        Log.i(TAG, "notifyCaptureFinished");
        if (this.mIsHighLightNeed && (fingerViewController = this.mFingerViewController) != null) {
            fingerViewController.removeHighlightCircle();
        }
    }

    public void notifyFingerCalibrarion(int value) {
        initPositionAndType();
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            int[] position = getFingerprintHardwarePosition();
            FingerprintCalibrarionView caliview = FingerprintCalibrarionView.getInstance(this.mContext);
            caliview.setCenterPoints((position[0] + position[2]) / 2, (position[1] + position[3]) / 2);
            caliview.showHighlightviewCali(value);
        }
    }

    public void notifyEnrollmentStarted(int flags) {
        initPositionAndType();
        if (this.mIsHighLightNeed) {
            FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
            if (!fingerprintUtils.isDualFp() || flags == 4096) {
                this.mIsUdEnrolling = true;
                checkPermissions();
                if (!fingerprintUtils.isDualFp() || flags == 4096) {
                    DisplayEngineManager displayEngineManager = this.mDisplayEngineManager;
                    if (displayEngineManager != null) {
                        displayEngineManager.setScene(31, 16);
                    } else {
                        Log.w(TAG, "mDisplayEngineManager is null!");
                    }
                }
                FingerViewController fingerViewController = this.mFingerViewController;
                if (fingerViewController != null) {
                    fingerViewController.showHighlightview(0);
                }
                Log.i(TAG, "start enroll begin add Highlight view");
                sendCommandToHal(1);
                return;
            }
            Log.i(TAG, "not enrolling UD fingerprint");
        }
    }

    public void notifyEnrollmentCanceled() {
        FingerViewController fingerViewController;
        boolean hasUdFingerprint;
        if (this.mIsFingerInScreenSupported) {
            FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
            int currentUser = ActivityManager.getCurrentUser();
            boolean z = true;
            if (fingerprintUtils.isDualFp()) {
                if (fingerprintUtils.getFingerprintsForUser(this.mContext, currentUser, 1).size() <= 0) {
                    z = false;
                }
                hasUdFingerprint = z;
            } else {
                if (fingerprintUtils.getBiometricsForUser(this.mContext, currentUser).size() <= 0) {
                    z = false;
                }
                hasUdFingerprint = z;
            }
            if (!hasUdFingerprint) {
                sendCommandToHal(0);
            }
        }
        Log.i(TAG, "notifyEnrollmentEnd mIsUdEnrolling: " + this.mIsUdEnrolling);
        if (this.mIsHighLightNeed) {
            checkPermissions();
            if (this.mIsUdEnrolling && (fingerViewController = this.mFingerViewController) != null) {
                fingerViewController.removeHighlightview(0);
            }
            if (this.mIsUdEnrolling) {
                DisplayEngineManager displayEngineManager = this.mDisplayEngineManager;
                if (displayEngineManager != null) {
                    displayEngineManager.setScene(31, 17);
                } else {
                    Log.w(TAG, "mDisplayEngineManager is null!");
                }
            }
            this.mIsUdEnrolling = false;
        }
    }

    public void notifyAuthenticationFinished(String opName, int result, int failTimes) {
        int i;
        if (this.mIsFingerInScreenSupported && this.mFingerViewController != null) {
            checkPermissions();
            Log.i(TAG, "notifyAuthenticationFinished,mIsUdFingerprintChecking = " + this.mIsUdFingerprintChecking + ",result =" + result + "failTimes = " + failTimes + ",mIsHighLightNeed=" + this.mIsHighLightNeed);
            if (this.mIsHighLightNeed && this.mIsUdFingerprintChecking) {
                resumeTouchSwitch();
                if (!"com.android.systemui".equals(opName)) {
                    FingerViewController fingerViewController = this.mFingerViewController;
                    if (result == 0) {
                        i = 2;
                    } else {
                        i = -1;
                    }
                    fingerViewController.removeHighlightview(i);
                } else {
                    this.mFingerViewController.removeHighlightviewOnKeyguard();
                    Log.i(TAG, "finger check finish removeHighlightCircle");
                }
            }
            if ("com.android.systemui".equals(opName) && result == 0) {
                Log.i(TAG, " KEYGUARD notifyAuthenticationFinished remove FingerviewAnim");
                this.mFingerViewController.destroyFingerAnimViewOnKeyguard();
            }
            if (result == 0) {
                this.mFingerViewController.reopenEyeProtecttionMode(opName);
                FingerprintViewUtils.setScreenRefreshRate(this.mContext, 0, opName, this.mIsFingerInScreenSupported, this.mFingerViewController.getHandler());
            }
            this.mFingerViewController.updateFingerprintView(result, failTimes);
            if (result == 0 && "com.android.systemui".equals(opName)) {
                this.mFingerViewController.removeMaskOrButton();
            }
            this.mIsUdFingerprintChecking = false;
        }
    }

    /* access modifiers changed from: private */
    public void resumeTouchSwitch() {
        if (this.mIsHighLightNeed && this.mIsUdFingerprintChecking && this.mNeedResumeTouchSwitch) {
            long token = Binder.clearCallingIdentity();
            try {
                this.mNeedResumeTouchSwitch = false;
                Log.i(TAG, "turn on the show_touch switch after authenticating");
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "show_touches", 1, -2);
            } catch (SecurityException e) {
                Log.e(TAG, "catch SecurityException");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    private void disableFingerprintView(boolean hasAnimation) {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.i(TAG, "callingApp = " + callingApp);
            if (this.mIsUdAuthenticating && callingApp.equals(FingerprintViewUtils.getForegroundActivity())) {
                this.mFingerViewController.removeMaskOrButton();
            }
            this.mPackageDisableMask = callingApp;
        }
    }

    private void enableFingerprintView(boolean hasAnimation, int initStatus) {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            Log.i(TAG, "enableFingerprintView,callingApp = " + callingApp);
            String str = this.mPackageDisableMask;
            if (str != null && str.equals(callingApp)) {
                this.mPackageDisableMask = null;
            }
            this.mAppDefinedMaskType = initStatus;
            this.mDefinedAppName = callingApp;
        }
    }

    private int getAppType(String pkgName) {
        int type = -1;
        try {
            Bundle metaData = this.mContext.getPackageManager().getApplicationInfo(pkgName, 128).metaData;
            if (metaData == null) {
                Log.d(TAG, "metaData is null");
            }
            if (metaData != null) {
                Log.i(TAG, "pkgName is " + pkgName + "metaData is " + metaData.getString("fingerprint.system.view"));
                if (metaData.getString("fingerprint.system.view") != null) {
                    if (metaData.getString("fingerprint.system.view").equals(BuildConfig.FLAVOR_product)) {
                        type = 0;
                    } else if (metaData.getString("fingerprint.system.view").equals("button")) {
                        type = 1;
                    } else if (metaData.getString("fingerprint.system.view").equals("image")) {
                        type = 3;
                    } else {
                        type = 2;
                    }
                }
                Log.i(TAG, "metaData type is " + type);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "cannot find metaData of package");
        } catch (SecurityException e2) {
            Log.e(TAG, "app don't have permissions");
        }
        return type;
    }

    private void keepMaskShowAfterAuthentication() {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            this.mKeepMaskAfterAuthentication = true;
        }
    }

    private void removeMaskAndShowButton() {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            this.mFingerViewController.removeMaskAndShowButton();
        }
    }

    /* JADX INFO: finally extract failed */
    private String getForegroundActivityName() {
        long token = Binder.clearCallingIdentity();
        try {
            ActivityInfo info = ActivityManagerEx.getLastResumedActivity();
            Binder.restoreCallingIdentity(token);
            String name = "";
            if (info != null) {
                name = info.name;
            }
            Log.i(TAG, "foreground wechat activity name is" + name);
            return name;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean checkIfCallerDisableMask() {
        String str = this.mPackageDisableMask;
        if (str == null) {
            return false;
        }
        if (FingerprintViewUtils.isForegroundActivity(str)) {
            return true;
        }
        this.mPackageDisableMask = null;
        return false;
    }

    /* access modifiers changed from: private */
    public void checkPermissions() {
        Context context = this.mContext;
        if (context != null && context.checkCallingOrSelfPermission("android.permission.USE_FINGERPRINT") != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.USE_BIOMETRIC", "Must have USE_BIOMETRIC permission");
        }
    }

    private void resetFingerprintPosition() {
        int[] position = getFingerprintHardwarePosition();
        this.mFingerViewController.setFingerprintPosition(position);
        Log.i(TAG, "defaultScreenSize be init second, position = " + Arrays.toString(position));
    }

    private int getEnrollDigitalBrigtness() {
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            return 0;
        }
        try {
            return daemon.sendCmdToHal(MMI_TYPE_GET_HIGHLIGHT_LEVEL);
        } catch (RemoteException e) {
            Log.e(TAG, "getDigitalBrigtness sendCmdToHal RemoteException");
            return 0;
        }
    }

    private int setWeatherDataToHal(int curTemperture, String curHumidity) {
        byte[] sendData = new byte[2];
        float[] huimidityData = new float[2];
        if (curHumidity == null) {
            return 0;
        }
        try {
            huimidityData[0] = Float.parseFloat(curHumidity.split("%")[0]);
        } catch (NumberFormatException e) {
            Log.e(TAG, "setDataToHal NumberFormatException");
        }
        sendData[0] = (byte) curTemperture;
        sendData[1] = (byte) ((int) huimidityData[0]);
        ArrayList<Byte> list = new ArrayList<>();
        list.add(Byte.valueOf(sendData[0]));
        list.add(Byte.valueOf(sendData[1]));
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            return 0;
        }
        try {
            return daemon.sendDataToHal(400, list);
        } catch (RemoteException e2) {
            Log.e(TAG, "setDataToHal RemoteException");
            return 0;
        }
    }

    private void initPositionAndType() {
        int i = this.mFingerprintType;
        if (i == 0) {
            Log.i(TAG, "FingerprintType do not support inscreen fingerprint");
        } else if (i <= 0 || this.mFingerViewController == null) {
            this.mFingerprintType = getFingerprintHardwareTypeInternal();
            Log.i(TAG, "FingerprintType type = " + this.mFingerprintType);
            boolean z = false;
            this.mIsFingerInScreenSupported = this.mFingerprintType > 0;
            if (this.mFingerprintType == 1) {
                z = true;
            }
            this.mIsHighLightNeed = z;
            if (this.mIsFingerInScreenSupported) {
                this.mFingerViewController = FingerViewController.getInstance(this.mContext);
                this.mFingerViewController.registCallback(new FingerViewChangeCallback());
                this.mFingerViewController.setFingerHandler(this.mFingerHandler);
                this.mFingerViewController.setFingerprintPosition(getFingerprintHardwarePosition());
                this.mFingerViewController.setHighLightBrightnessLevel(getHighLightBrightnessLevel());
                this.mFingerViewController.setEnrollDigitalBrigtness(getEnrollDigitalBrigtness());
                this.mFingerViewController.setFingerPrintLogoRadius(getFingerPrintLogoRadius());
                int spotColor = getSpotColor();
                if (spotColor == 0) {
                    spotColor = DEFAULT_COLOR;
                }
                this.mFingerViewController.setHighLightSpotColor(spotColor);
                this.mFingerViewController.setHighLightSpotRadius(getHighLightspotRadius());
                initObserver();
                initFpIdentifyViewObserver();
                getAodFace();
            }
        } else {
            if (this.mInitDisplayHeight == -1) {
                resetFingerprintPosition();
            }
            Log.i(TAG, "FingerprintType has been inited, FingerprintType = " + this.mFingerprintType);
        }
    }

    /* access modifiers changed from: protected */
    public void triggerFaceRecognization() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager == null || !powerManager.isInteractive()) {
            HwPhoneWindowManager policy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
            if (policy != null) {
                policy.doFaceRecognize(true, FACE_DETECT_REASON);
                return;
            }
            return;
        }
        Log.i(TAG, "screen on, do not trigger face detection");
    }

    /* access modifiers changed from: private */
    public void suspendAuthentication(int status) {
        if (!isFingerprintDReady()) {
            Log.w(TAG, "FingerprintD is not Ready");
        } else if (!this.mIsFingerInScreenSupported) {
            Log.w(TAG, "do not have UD device suspend invalid");
        } else {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            Slog.i(TAG, "pacel  packaged suspendAuthentication: " + status);
            if (status == 1) {
                try {
                    daemon.sendCmdToHal(53);
                } catch (RemoteException e) {
                    Slog.e(TAG, "suspendAuthentication RemoteException:" + e);
                }
            } else {
                daemon.sendCmdToHal(MSG_RESUME_AUTHENTICATION);
            }
        }
    }

    private int suspendEnroll(int status) {
        if (!isFingerprintDReady()) {
            Log.w(TAG, "FingerprintD is not Ready");
            return -1;
        } else if (!this.mIsFingerInScreenSupported) {
            Log.w(TAG, "do not have UD device suspend invalid");
            return -1;
        } else {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return -1;
            }
            Slog.i(TAG, "pacel  packaged suspendEnroll: " + status);
            if (status != 1) {
                return daemon.sendCmdToHal(MSG_RESUME_ENROLLMENT);
            }
            try {
                return daemon.sendCmdToHal(64);
            } catch (RemoteException e) {
                Slog.e(TAG, "suspendEnroll RemoteException:" + e);
                return -1;
            }
        }
    }

    private int getFingerprintHardwareType() {
        if (this.typeDetails == -1) {
            if (!isFingerprintDReady()) {
                return -1;
            }
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return -1;
            }
            Slog.i(TAG, "pacel  packaged :HardwareType");
            try {
                this.typeDetails = daemon.sendCmdToHal(55);
            } catch (RemoteException e) {
                Slog.e(TAG, "HardwareType RemoteException:" + e);
            }
            Slog.i(TAG, "fingerprintd HardwareType = " + this.typeDetails);
        }
        return this.typeDetails;
    }

    private int getFingerprintHardwareTypeInternal() {
        if (getFingerprintHardwareType() == -1) {
            return -1;
        }
        int offset = -1;
        if ((getFingerprintHardwareType() & 1) != 0) {
            offset = -1 + 1;
        }
        if ((getFingerprintHardwareType() & 2) != 0) {
            offset++;
        }
        if ((getFingerprintHardwareType() & 4) == 0) {
            return 0;
        }
        int physicalType = (getFingerprintHardwareType() >> (((offset + 1) * 4) + 8)) & 15;
        Log.i(TAG, "LOCATION_UNDER_DISPLAY physicalType :" + physicalType);
        if (physicalType == 2) {
            return 1;
        }
        if (physicalType == 3) {
            return 2;
        }
        return -1;
    }

    private int[] getFingerprintLogoPosition() {
        int[] result = {-1, -1};
        int[] pxPosition = {-1, -1, -1, -1};
        if (!isFingerprintDReady()) {
            return pxPosition;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return pxPosition;
        }
        try {
            result[0] = daemon.sendCmdToHal(MSG_GET_LOGO_POSITION_FROM_HAL);
            result[1] = result[0];
        } catch (RemoteException e) {
            Slog.e(TAG, "HardwarePosition RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd getFingerprintLogoPosition = " + result[0]);
        if (result[0] == -1 || result[0] == 0) {
            return getFingerprintHardwarePosition();
        }
        return FingerprintViewUtils.getFingerprintHardwarePosition(result);
    }

    private int[] getFingerprintHardwarePosition() {
        int[] result = {-1, -1};
        int[] pxPosition = {-1, -1, -1, -1};
        if (!isFingerprintDReady()) {
            return pxPosition;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return pxPosition;
        }
        try {
            result[0] = daemon.sendCmdToHal(MSG_GET_SENSOR_POSITION_TOP_LEFT);
            result[1] = daemon.sendCmdToHal(60);
        } catch (RemoteException e) {
            Slog.e(TAG, "HardwarePosition RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd HardwarePosition = " + result[0] + " " + result[1]);
        if (result[0] == -1) {
            return FingerprintViewUtils.getFingerprintHardwarePosition(result);
        }
        return physicalConvert2Px(FingerprintViewUtils.getFingerprintHardwarePosition(result));
    }

    private int[] physicalConvert2Px(int[] input) {
        int[] covertPosition = {-1, -1, -1, -1};
        if (this.mInitDisplayHeight == -1 && this.mContext != null) {
            String defaultScreenSize = SystemProperties.get("ro.config.default_screensize");
            if (defaultScreenSize == null || defaultScreenSize.equals("")) {
                this.mInitDisplayHeight = Settings.Global.getInt(this.mContext.getContentResolver(), APS_INIT_HEIGHT, -1);
                this.mInitDisplayWidth = Settings.Global.getInt(this.mContext.getContentResolver(), APS_INIT_WIDTH, -1);
                Log.i(TAG, "defaultScreenSizePoint mInitDisplayWidth =" + this.mInitDisplayWidth + ",mInitDisplayHeight=" + this.mInitDisplayHeight);
            } else {
                String[] array = defaultScreenSize.split(",");
                if (array.length == 2) {
                    try {
                        this.mInitDisplayWidth = Integer.parseInt(array[0]);
                        this.mInitDisplayHeight = Integer.parseInt(array[1]);
                        Log.i(TAG, "defaultScreenSizePoint get from prop : mInitDisplayWidth=" + this.mInitDisplayWidth + ",mInitDisplayHeight=" + this.mInitDisplayHeight);
                    } catch (NumberFormatException e) {
                        Log.i(TAG, "defaultScreenSizePoint: NumberFormatException");
                    }
                } else {
                    Log.i(TAG, "defaultScreenSizePoint the defaultScreenSize prop is error,defaultScreenSize=" + defaultScreenSize);
                }
            }
        }
        int i = input[0];
        int i2 = this.mInitDisplayWidth;
        covertPosition[0] = (i * i2) / 1000;
        int i3 = input[1];
        int i4 = this.mInitDisplayHeight;
        covertPosition[1] = (i3 * i4) / 1000;
        covertPosition[2] = (input[2] * i2) / 1000;
        covertPosition[3] = (input[3] * i4) / 1000;
        Log.d(TAG, "Width: " + this.mInitDisplayWidth + " height: " + this.mInitDisplayHeight);
        for (int i5 = 0; i5 < 4; i5++) {
            Log.d(TAG, "use hal after covert: " + covertPosition[i5]);
        }
        return covertPosition;
    }

    /* access modifiers changed from: private */
    public void notifyCaptureOpticalImage() {
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            Slog.i(TAG, "pacel  packaged :notifyCaptureOpticalImage");
            try {
                daemon.sendCmdToHal(52);
            } catch (RemoteException e) {
                Slog.e(TAG, "notifyCaptureOpticalImage RemoteException:" + e);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyBluespotDismiss() {
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            Slog.i(TAG, "pacel  packaged :notifyBluespotDismiss");
            try {
                daemon.sendCmdToHal(62);
            } catch (RemoteException e) {
                Slog.e(TAG, "notifyBluespotDismiss RemoteException:" + e);
            }
        }
    }

    private void setHoverEventSwitch(int enabled) {
        if (!isFingerprintDReady()) {
            Log.w(TAG, "FingerprintD is not Ready");
            return;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return;
        }
        Slog.i(TAG, "pacel  packaged setHoverEventSwitch: " + enabled);
        if (enabled == 1) {
            try {
                daemon.sendCmdToHal(MSG_SET_HOVER_ENABLE);
            } catch (RemoteException e) {
                Slog.e(TAG, "setHoverEventSwitch RemoteException:" + e);
            }
        } else {
            daemon.sendCmdToHal(MSG_SET_HOVER_DISABLE);
        }
    }

    private int getHoverEventSupport() {
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        int result = -1;
        Slog.i(TAG, "pacel  packaged :getHoverEventSupport");
        try {
            result = daemon.sendCmdToHal(MSG_CHECK_HOVER_SUPPORT);
        } catch (RemoteException e) {
            Slog.e(TAG, "getHoverEventSupport RemoteException:" + e);
        }
        Slog.i(TAG, "fingerprintd getHoverEventSupport = " + result);
        return result;
    }

    private int getHighLightBrightnessLevel() {
        int result = DEFAULT_CAPTURE_BRIGHTNESS;
        if (!isFingerprintDReady()) {
            Slog.e(TAG, "Fingerprintd is not ready!");
            return DEFAULT_CAPTURE_BRIGHTNESS;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return DEFAULT_CAPTURE_BRIGHTNESS;
        }
        Slog.i(TAG, "pacel  packaged :getHighLightBrightnessLevel");
        try {
            result = daemon.sendCmdToHal(MSG_GET_BRIGHTNEWSS_FROM_HAL);
        } catch (RemoteException e) {
            Slog.e(TAG, "getHighLightBrightnessLevel RemoteException");
        }
        Slog.i(TAG, "fingerprintd getHighLightBrightnessLevel = " + result);
        return result;
    }

    private int getSpotColor() {
        int color = 0;
        if (!isFingerprintDReady()) {
            Slog.e(TAG, "Fingerprintd is not ready!");
            return 0;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return 0;
        }
        Slog.i(TAG, "pacel  packaged :getHighLightColor");
        try {
            color = daemon.sendCmdToHal(MSG_GET_HIGHLIGHT_SPOT_COLOR_FROM_HAL);
        } catch (RemoteException e) {
            Slog.e(TAG, "getHighLightColor RemoteException");
        }
        Slog.i(TAG, "fingerprintd getHighLightColor = " + color);
        return color;
    }

    private int checkForegroundNeedLiveness() {
        Slog.w(TAG, "checkForegroundNeedLiveness:start");
        try {
            List<ActivityManager.RunningAppProcessInfo> procs = ActivityManagerNative.getDefault().getRunningAppProcesses();
            if (procs == null) {
                return 0;
            }
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                ActivityManager.RunningAppProcessInfo proc = procs.get(i);
                if (proc.importance == 100) {
                    if ("com.alipay.security.mobile.authentication.huawei".equals(proc.processName)) {
                        Slog.w(TAG, "ForegroundActivity is " + proc.processName + "need liveness auth");
                        return 1;
                    } else if ("com.huawei.wallet".equals(proc.processName)) {
                        Slog.w(TAG, "ForegroundActivity is " + proc.processName + "need liveness auth");
                        return 1;
                    } else if ("com.huawei.android.hwpay".equals(proc.processName)) {
                        Slog.w(TAG, "ForegroundActivity is " + proc.processName + "need liveness auth");
                        return 1;
                    }
                }
            }
            return 0;
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed in checkForegroundNeedLiveness");
        }
    }

    private int checkNeedLivenessList(String opPackageName2) {
        Slog.w(TAG, "checkNeedLivenessList:start");
        if (opPackageName2 == null || opPackageName2.equals("com.android.keyguard")) {
            return 0;
        }
        if (opPackageName2.equals("com.huawei.securitymgr")) {
            return checkForegroundNeedLiveness();
        }
        if (!opPackageName2.equals("com.eg.android.AlipayGphone") && !opPackageName2.equals("fido") && !opPackageName2.equals("com.alipay.security.mobile.authentication.huawei") && !opPackageName2.equals("com.huawei.wallet") && !opPackageName2.equals("com.huawei.android.hwpay") && !opPackageName2.equals(PKGNAME_OF_WECHAT)) {
            return 0;
        }
        return 1;
    }

    private List<Byte> getAuthenticationScenarioPackageName(String appPackageName) {
        if (appPackageName == null) {
            Slog.e(TAG, "updateAuthenticationScenario opPackageName is null");
            return new ArrayList<>(0);
        }
        byte[] packageNameBytes = appPackageName.getBytes(StandardCharsets.UTF_8);
        if (!(packageNameBytes == null || packageNameBytes.length == 0)) {
            List<Byte> packageNameArrayList = new ArrayList<>(packageNameBytes.length);
            for (byte i : packageNameBytes) {
                packageNameArrayList.add(Byte.valueOf(i));
            }
            if (!packageNameArrayList.isEmpty()) {
                return packageNameArrayList;
            }
        }
        return new ArrayList<>(0);
    }

    /* access modifiers changed from: protected */
    public void setLivenessSwitch(String opPackageName2) {
        Slog.w(TAG, "setLivenessSwitch:start");
        if ((!mLivenessNeedBetaQualification || isBetaUser()) && isFingerprintDReady()) {
            int NEED_LIVENESS_AUTHENTICATION = checkNeedLivenessList(opPackageName2);
            Slog.w(TAG, "NEED_LIVENESS_AUTHENTICATION = " + NEED_LIVENESS_AUTHENTICATION);
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            try {
                daemon.setLivenessSwitch(NEED_LIVENESS_AUTHENTICATION);
                List<Byte> packageNameArrays = getAuthenticationScenarioPackageName(opPackageName2);
                if (packageNameArrays != null && !packageNameArrays.isEmpty() && (packageNameArrays instanceof ArrayList)) {
                    daemon.sendDataToHal(300, (ArrayList) packageNameArrays);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "setLivenessSwitch RemoteException:" + e);
            }
            Slog.w(TAG, "framework setLivenessSwitch is ok ---end");
        }
    }

    private boolean checkPackageName(String opPackageName2) {
        if (opPackageName2 == null || !opPackageName2.equals("com.android.systemui")) {
            return false;
        }
        return true;
    }

    public boolean shouldAuthBothSpaceFingerprints(String opPackageName2, int flags) {
        if (!checkPackageName(opPackageName2) || (33554432 & flags) == 0) {
            return false;
        }
        return true;
    }

    private HwFingerprintSets remoteGetOldData() {
        HwFingerprintSets _result;
        Slog.i(TAG, "remoteGetOldData:start");
        if (!isFingerprintDReady()) {
            return null;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return null;
        }
        ArrayList<Integer> fingerprintInfo = new ArrayList<>();
        try {
            fingerprintInfo = daemon.getFpOldData();
        } catch (RemoteException e) {
            Slog.e(TAG, "remoteGetOldData RemoteException:" + e);
        }
        Parcel _reply = Parcel.obtain();
        int fingerprintInfoLen = fingerprintInfo.size();
        for (int i = 0; i < fingerprintInfoLen; i++) {
            int intValue = fingerprintInfo.get(i).intValue();
            if (intValue != -1) {
                _reply.writeInt(intValue);
            }
        }
        _reply.setDataPosition(0);
        if (_reply.readInt() != 0) {
            _result = HwFingerprintSets.CREATOR.createFromParcel(_reply);
        } else {
            _result = null;
        }
        _reply.recycle();
        return _result;
    }

    private static boolean checkItemExist(int oldFpId, ArrayList<Fingerprint> fingerprints) {
        int size = fingerprints.size();
        for (int i = 0; i < size; i++) {
            if (fingerprints.get(i).getBiometricId() == oldFpId) {
                fingerprints.remove(i);
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isFingerprintDReady() {
        if (getFingerprintDaemon() != null) {
            return true;
        }
        Slog.w(TAG, "isFingerprintDReady: no fingeprintd!");
        return false;
    }

    /* access modifiers changed from: protected */
    public void handleAuthenticated(BiometricAuthenticator.Identifier identifier, ArrayList<Byte> token) {
        this.mLastAuthenticatedStartTime = System.currentTimeMillis();
        boolean authenticated = identifier.getBiometricId() != 0;
        if (authenticated) {
            clearKeyguardAuthenScreenOn();
            AwareFakeActivityRecg.self().setFingerprintWakeup(true);
        }
        if (isPowerFpAbandonAuthenticated()) {
            Slog.i(TAG, "discard onAuthenticated:" + authenticated);
            return;
        }
        FingerprintDataInterface fingerprintDataInterface = this.mFpDataCollector;
        if (fingerprintDataInterface != null) {
            fingerprintDataInterface.reportFingerprintAuthenticated(authenticated);
        }
        this.auTime = System.currentTimeMillis();
        HwFingerprintService.super.handleAuthenticated(identifier, token);
        sendOnAuthenticatedFinishToHal();
        this.mLastAuthenticatedEndTime = System.currentTimeMillis();
        Slog.i(TAG, "powerfp duration=" + (this.mLastAuthenticatedEndTime - this.mLastAuthenticatedStartTime));
        this.mIsBlackAuthenticateEvent = false;
    }

    /* access modifiers changed from: protected */
    public void stopPickupTrunOff() {
        PickUpWakeScreenManager.getInstance().stopTurnOffController();
    }

    private int sendUnlockAndLightbright(int unlockType) {
        Slog.i(TAG, "sendUnlockAndLightbright unlockType:" + unlockType);
        int result = -1;
        if (2 == unlockType) {
            result = sendCommandToHal(201);
        } else if (1 == unlockType) {
            result = sendCommandToHal(200);
        } else if (3 == unlockType) {
            result = sendCommandToHal(202);
        }
        Slog.i(TAG, "sendCommandToHal result:" + result);
        return result;
    }

    private int getHighLightspotRadius() {
        Slog.d(TAG, "getHighLightspotRadius start");
        int radius = 95;
        if (!isFingerprintDReady()) {
            Slog.e(TAG, "Fingerprintd is not ready!");
            return 95;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return 95;
        }
        try {
            radius = daemon.sendCmdToHal(MSG_GET_RADIUS_FROM_HAL);
        } catch (RemoteException e) {
            Slog.e(TAG, "getHighLightspotRadius RemoteException");
        }
        Slog.i(TAG, "fingerprintd getHighLightspotRadius = " + radius);
        return radius;
    }

    private int getFingerPrintLogoRadius() {
        int result = sendCommandToHal(MSG_MMI_UD_UI_LOGO_SIZE);
        Slog.i(TAG, "getFingerPrintLogoRadius:" + result);
        if (result == -1 || result <= 0) {
            return FingerprintViewUtils.getFingerprintDefaultLogoRadius(this.mContext);
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public int shouldAuthBothSpaceBiometric(AuthenticationClientImpl client, String opPackageName2, int flags) {
        if (!shouldAuthBothSpaceFingerprints(opPackageName2, flags)) {
            return client.getGroupId();
        }
        Slog.i(TAG, "should authenticate both space fingerprints");
        return -101;
    }

    private AODFaceUpdateMonitor getAodFace() {
        if (this.mAodFaceUpdateMonitor == null) {
            this.mAodFaceUpdateMonitor = new AODFaceUpdateMonitor();
        }
        return this.mAodFaceUpdateMonitor;
    }

    public class AODFaceUpdateMonitor {
        private static final String ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_CHANGE_POWERMODE";
        private static final String ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE";
        private static final int CLOSE_SAVE_POWER = 0;
        private static final String FACE_KEYGUARD_WITH_LOCK = "face_bind_with_lock";
        private static final String FACE_RECOGNIZE_SLIDE_UNLOCK = "face_recognize_slide_unlock";
        private static final String FACE_RECOGNIZE_UNLOCK = "face_recognize_unlock";
        private static final int OPEN_SAVE_POWER = 3;
        private static final String PERMISSION = "com.huawei.android.launcher.permission.CHANGE_POWERMODE";
        private static final String POWER_MODE = "power_mode";
        private static final String SHUTDOWN_LIMIT_POWERMODE = "shutdomn_limit_powermode";
        private AODFaceTrustListener mAodFaceTrustListener;
        private boolean mIsFaceDetectEnabled;
        private boolean mIsPrimaryUser;
        /* access modifiers changed from: private */
        public boolean mIsSuperPowerEnabled;
        private boolean mIsSupportAODFace;
        /* access modifiers changed from: private */
        public boolean mIsTrustEnabled;
        /* access modifiers changed from: private */
        public boolean mIsTrustManageEnabled;
        /* access modifiers changed from: private */
        public boolean mIsTrustUnlock;
        protected final ContentObserver mSettingsObserver = new ContentObserver(null) {
            /* class com.android.server.fingerprint.HwFingerprintService.AODFaceUpdateMonitor.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                AODFaceUpdateMonitor.this.notifyFaceSettingModify(false);
            }
        };
        private TrustManager mTrustManager;
        private boolean mfaceStatus;
        private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
            /* class com.android.server.fingerprint.HwFingerprintService.AODFaceUpdateMonitor.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    Slog.i(HwFingerprintService.TAG, "super power action:" + action);
                    if (AODFaceUpdateMonitor.ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE.equals(action)) {
                        if (intent.getIntExtra(AODFaceUpdateMonitor.POWER_MODE, 0) == 3) {
                            boolean unused = AODFaceUpdateMonitor.this.mIsSuperPowerEnabled = true;
                        }
                    } else if (AODFaceUpdateMonitor.ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE.equals(action) && intent.getIntExtra(AODFaceUpdateMonitor.SHUTDOWN_LIMIT_POWERMODE, 0) == 0) {
                        boolean unused2 = AODFaceUpdateMonitor.this.mIsSuperPowerEnabled = false;
                    }
                    AODFaceUpdateMonitor.this.sendFaceStatusToHal(false);
                }
            }
        };

        public AODFaceUpdateMonitor() {
            if (HwFingerprintService.this.mIsFingerInScreenSupported || HwFingerprintService.this.isSupportPowerFp() || HwFingerprintService.SUPPORT_INFORM_FACE) {
                String board = SystemProperties.get("ro.product.board", "UNKOWN").toUpperCase(Locale.US);
                if (board.contains("TNY") || board.contains("TONY") || board.contains("NEO")) {
                    this.mIsSupportAODFace = false;
                    return;
                }
                this.mIsSupportAODFace = true;
                ContentResolver resolver = HwFingerprintService.this.mContext.getContentResolver();
                resolver.registerContentObserver(Settings.Secure.getUriFor(FACE_KEYGUARD_WITH_LOCK), false, this.mSettingsObserver, 0);
                resolver.registerContentObserver(Settings.Secure.getUriFor(FACE_RECOGNIZE_SLIDE_UNLOCK), false, this.mSettingsObserver, 0);
                resolver.registerContentObserver(Settings.Secure.getUriFor(FACE_RECOGNIZE_UNLOCK), false, this.mSettingsObserver, 0);
                IntentFilter powerFilter = new IntentFilter();
                powerFilter.addAction(ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE);
                powerFilter.addAction(ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE);
                HwFingerprintService.this.mContext.registerReceiver(this.powerReceiver, powerFilter, PERMISSION, null);
                try {
                    this.mAodFaceTrustListener = new AODFaceTrustListener();
                    this.mTrustManager = (TrustManager) HwFingerprintService.this.mContext.getSystemService("trust");
                    this.mTrustManager.registerTrustListener(this.mAodFaceTrustListener);
                } catch (Exception e) {
                    Slog.e(HwFingerprintService.TAG, "create AODFaceUpdateMonitor Exception", e);
                }
                getFaceSetting();
                return;
            }
            this.mIsSupportAODFace = false;
        }

        /* access modifiers changed from: private */
        public boolean isSupportAODFace() {
            return this.mIsSupportAODFace;
        }

        /* access modifiers changed from: private */
        public void notifyFaceSettingModify(boolean isBooting) {
            getFaceSetting();
            sendFaceStatusToHal(isBooting);
        }

        private void getFaceSetting() {
            ContentResolver resolver = HwFingerprintService.this.mContext.getContentResolver();
            boolean z = false;
            int faceKeyguardWithLock = Settings.Secure.getIntForUser(resolver, FACE_KEYGUARD_WITH_LOCK, -1, 0);
            if (faceKeyguardWithLock == -1) {
                if (Settings.Secure.getIntForUser(resolver, FACE_RECOGNIZE_UNLOCK, 0, 0) == 1 || Settings.Secure.getIntForUser(resolver, FACE_RECOGNIZE_SLIDE_UNLOCK, 0, 0) == 1) {
                    z = true;
                }
                this.mIsFaceDetectEnabled = z;
                return;
            }
            if (faceKeyguardWithLock == 1) {
                z = true;
            }
            this.mIsFaceDetectEnabled = z;
        }

        public void checkIsPrimaryUser(int userId, String opPackageName) {
            if (isSupportAODFace()) {
                if (userId == 0) {
                    this.mIsPrimaryUser = true;
                } else {
                    this.mIsPrimaryUser = false;
                }
                if ("com.android.systemui".equals(opPackageName)) {
                    sendFaceStatusToHal(false);
                }
            }
        }

        public void sendFaceStatusToHal(boolean isBooting) {
            if (isSupportAODFace()) {
                boolean faceStatus = this.mIsFaceDetectEnabled && !this.mIsSuperPowerEnabled && this.mIsPrimaryUser && !this.mIsTrustUnlock;
                Slog.i(HwFingerprintService.TAG, "sendFaceStatusToHal:mIsFaceDetectEnabled " + this.mIsFaceDetectEnabled + ",mIsSuperPowerEnabled " + this.mIsSuperPowerEnabled + ",mIsPrimaryUser " + this.mIsPrimaryUser + ",mIsTrustUnlock " + this.mIsTrustUnlock + ",faceStatus " + faceStatus + ",mfaceStatus " + this.mfaceStatus + ",isBooting " + isBooting);
                if (isBooting || faceStatus != this.mfaceStatus) {
                    int cmd = faceStatus ? 211 : 210;
                    this.mfaceStatus = faceStatus;
                    HwFingerprintService.this.sendCommandToHal(cmd);
                }
            }
        }

        public class AODFaceTrustListener implements TrustManager.TrustListener {
            private static final String CONTENT_URI = "content://com.android.huawei.keyguardstate";
            private static final String KCALL_GET_STRONG_AUTH_STATE = "getStrongAuthState";

            public AODFaceTrustListener() {
            }

            private void updateTrustUnlockStatus() {
                if (AODFaceUpdateMonitor.this.isSupportAODFace()) {
                    if (!AODFaceUpdateMonitor.this.mIsTrustEnabled || !AODFaceUpdateMonitor.this.mIsTrustManageEnabled) {
                        boolean unused = AODFaceUpdateMonitor.this.mIsTrustUnlock = false;
                    } else {
                        Slog.i(HwFingerprintService.TAG, "update TrustUnlock Status");
                        try {
                            Bundle outData = HwFingerprintService.this.mContext.getContentResolver().call(Uri.parse(CONTENT_URI), KCALL_GET_STRONG_AUTH_STATE, (String) null, (Bundle) null);
                            if (outData == null || outData.getInt("result") != 0 || outData.getBoolean("StrongAuth")) {
                                boolean unused2 = AODFaceUpdateMonitor.this.mIsTrustUnlock = false;
                            } else {
                                boolean unused3 = AODFaceUpdateMonitor.this.mIsTrustUnlock = true;
                            }
                        } catch (Exception e) {
                            Slog.e(HwFingerprintService.TAG, "getStrongAuthState Exception", e);
                        }
                    }
                    AODFaceUpdateMonitor.this.sendFaceStatusToHal(false);
                }
            }

            public void onTrustChanged(boolean enabled, int userId, int flags) {
                boolean unused = AODFaceUpdateMonitor.this.mIsTrustEnabled = enabled;
                updateTrustUnlockStatus();
            }

            public void onTrustManagedChanged(boolean enabled, int userId) {
                boolean unused = AODFaceUpdateMonitor.this.mIsTrustManageEnabled = enabled;
                updateTrustUnlockStatus();
            }

            public void onTrustError(CharSequence message) {
                Slog.d(HwFingerprintService.TAG, "onTrustError:message " + message.toString());
            }
        }
    }

    /* access modifiers changed from: protected */
    public void startCurrentClient(int cookie) {
        HwFingerprintService.super.startCurrentClient(cookie);
        if ((this.mCurrentClient instanceof AuthenticationClient) || (this.mCurrentClient instanceof EnrollClient)) {
            sendCmdToTpHal(1);
        }
    }

    private void clearRepeatAuthentication() {
        if (this.mPendingClient != null && !isKeyguardLocked()) {
            Log.i(TAG, "clearRepeatAuthentication.");
            if ("com.android.systemui".equals(this.mPendingClient.getOwnerString())) {
                this.mPendingClient.destroy();
                this.mPendingClient = null;
            }
        }
    }

    private void startCurrentClient() {
        Log.d(TAG, "startCurrentClient.");
        if ((this.mCurrentClient instanceof AuthenticationClient) || (this.mCurrentClient instanceof EnrollClient)) {
            sendCmdToTpHal(1);
        }
    }

    /* access modifiers changed from: protected */
    public void removeClient(ClientMonitor client) {
        HwFingerprintService.super.removeClient(client);
        if ((client instanceof AuthenticationClient) || (client instanceof EnrollClient)) {
            sendCmdToTpHal(0);
        }
    }

    /* access modifiers changed from: private */
    public boolean isKeyguardLocked() {
        KeyguardManager keyguard;
        Context context = this.mContext;
        if (context == null || (keyguard = (KeyguardManager) context.getSystemService("keyguard")) == null) {
            return false;
        }
        return keyguard.isKeyguardLocked();
    }

    private void sendCmdToTpHal(int config) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                Log.w(TAG, "mProxy is null, return");
                return;
            }
            Log.i(TAG, "sendCmdToTpHal start config = " + config);
            try {
                if (this.mProxy.hwSetFeatureConfig(2, config == 1 ? TP_HAL_CONFIG_ON : TP_HAL_CONFIG_OFF) == 0) {
                    Log.i(TAG, "sendCmdToTpHal success");
                } else {
                    Log.i(TAG, "sendCmdToTpHal error");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to set cmd to tp hal");
            }
        }
    }

    /* access modifiers changed from: protected */
    public void connectToProxy() {
        synchronized (this.mLock) {
            if (this.mProxy != null) {
                Log.i(TAG, "mProxy has registered, do not register again");
                return;
            }
            try {
                this.mProxy = ITouchscreen.getService();
                if (this.mProxy != null) {
                    Log.d(TAG, "connectToProxy: mProxy get success.");
                    this.mProxy.linkToDeath(new DeathRecipient(), 1001);
                } else {
                    Log.d(TAG, "connectToProxy: mProxy get failed.");
                }
            } catch (NoSuchElementException e) {
                Log.e(TAG, "connectToProxy: tp hal service not found. Did the service fail to start?");
            } catch (RemoteException e2) {
                Log.e(TAG, "connectToProxy: tp hal service not responding");
            }
        }
    }

    final class DeathRecipient implements IHwBinder.DeathRecipient {
        DeathRecipient() {
        }

        public void serviceDied(long cookie) {
            if (cookie == 1001) {
                Log.d(HwFingerprintService.TAG, "tp hal service died cookie: " + cookie);
                synchronized (HwFingerprintService.this.mLock) {
                    ITouchscreen unused = HwFingerprintService.this.mProxy = null;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public AuthenticatedParam getPowerFpParam() {
        if (this.mAuthenticatedParam == null) {
            this.mAuthenticatedParam = new AuthenticatedParam();
        }
        return this.mAuthenticatedParam;
    }

    private boolean currentClient(String opPackageName2) {
        return this.mCurrentClient != null && this.mCurrentClient.getOwnerString().equals(opPackageName2);
    }

    private boolean isSupportBlackAuthentication() {
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "screen_lock_fingerprint_unlock_protection_db", 0);
        if (settingValue != null) {
            return "0".equals(settingValue);
        }
        return this.mSupportBlackAuthentication;
    }

    public boolean isSupportPowerFp() {
        return this.mSupportPowerFp;
    }

    public boolean isPowerFpForbidGotoSleep() {
        if (!isSupportPowerFp()) {
            return false;
        }
        if (isThreesidesAuthenticating()) {
            Slog.i(TAG, "powerfp forbid gotosleep isThreesidesAuthenticating");
            return true;
        }
        if (isSupportBlackAuthentication() && this.mIsBlackAuthenticateEvent) {
            long duration = System.currentTimeMillis() - this.mLastFingerDownTime;
            if (duration > 1000) {
                Slog.i(TAG, "powerfp forbid gotosleep do nothing, lastFingerDown duration=" + duration);
                return false;
            } else if (this.mLastAuthenticatedEndTime > this.mLastAuthenticatedStartTime) {
                long duration2 = System.currentTimeMillis() - this.mLastAuthenticatedEndTime;
                Slog.i(TAG, "powerfp forbid gotosleep 1 duration=" + duration2);
                if (duration2 < 600) {
                    return true;
                }
            } else {
                long duration3 = System.currentTimeMillis() - this.mLastAuthenticatedStartTime;
                Slog.i(TAG, "powerfp forbid gotosleep 2 duration=" + duration3);
                if (duration3 < 700) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isThreesidesAuthenticating() {
        return this.mCurrentClient != null && (((this.mCurrentClient instanceof AuthenticationClient) && !"com.android.systemui".equals(this.mCurrentClient.getOwnerString())) || (this.mCurrentClient instanceof EnrollClient));
    }

    private boolean isKeyguardAuthenticating() {
        return this.mCurrentClient != null && (this.mCurrentClient instanceof AuthenticationClient) && "com.android.systemui".equals(this.mCurrentClient.getOwnerString());
    }

    private int operPowerFpPowerKeyCode(int keycode, boolean isPowerDown, boolean interactive) {
        if (!isSupportPowerFp() || isSupportBlackAuthentication() || keycode != 26) {
            return -1;
        }
        if (isPowerDown) {
            this.mLastPowerKeyDownTime = System.currentTimeMillis();
        } else {
            this.mLastPowerKeyUpTime = System.currentTimeMillis();
        }
        this.mIsInteractive = interactive;
        if (isPowerDown && !interactive) {
            immediatelyOnAuthenticatedRunnable();
        }
        Slog.i(TAG, "operPowerFpPowerKeyCode: mLastPowerKeyDownTime=" + this.mLastPowerKeyDownTime + " mLastPowerKeyUpTime=" + this.mLastPowerKeyUpTime + " isPowerDown=" + isPowerDown + " mIsInteractive=" + this.mIsInteractive);
        return 0;
    }

    private void immediatelyOnAuthenticatedRunnable() {
        if (isKeyguardAuthenticating() && getPowerFpParam().isWaitPowerEvent()) {
            this.mHandler.post(new Runnable() {
                /* class com.android.server.fingerprint.HwFingerprintService.AnonymousClass16 */

                public void run() {
                    Slog.w(HwFingerprintService.TAG, "process FP delayed authentication");
                    AuthenticatedParam param = HwFingerprintService.this.getPowerFpParam();
                    Fingerprint fp = new Fingerprint("", param.getGroupId(), param.getFingerId(), param.getDeviceId());
                    HwFingerprintService hwFingerprintService = HwFingerprintService.this;
                    hwFingerprintService.handleAuthenticated(fp, hwFingerprintService.getPowerFpParam().getToken());
                }
            });
        }
    }

    public boolean isPowerFpAbandonAuthenticated() {
        if (!isSupportPowerFp() || isSupportBlackAuthentication() || !isKeyguardAuthenticating()) {
            return false;
        }
        if (this.mLastPowerKeyUpTime >= this.mLastPowerKeyDownTime) {
            boolean isInteractive = this.mPowerManager.isInteractive();
            if (!isInteractive && isSupportBlackAuthentication()) {
                return false;
            }
            if (!isInteractive) {
                Slog.i(TAG, "dev is sleep and no support black authenticated");
            }
            return !isInteractive;
        }
        if (this.mIsInteractive) {
            Slog.i(TAG, "power down finish, wait up.");
        }
        return this.mIsInteractive;
    }

    public long getPowerDelayFpTime() {
        if (!isSupportPowerFp() || isSupportBlackAuthentication() || !isKeyguardAuthenticating()) {
            return 0;
        }
        long nowTime = System.currentTimeMillis();
        long j = this.mLastPowerKeyUpTime;
        long j2 = this.mLastPowerKeyDownTime;
        if (j < j2 || nowTime <= j2) {
            Slog.i(TAG, "powerfp mLastPowerKeyUpTime:" + this.mLastPowerKeyUpTime + ",mLastPowerKeyDownTime:" + this.mLastPowerKeyDownTime + ",nowTime:" + nowTime);
            return 0;
        } else if (this.mPowerManager.isInteractive()) {
            return (long) this.mPowerDelayFpTime;
        } else {
            return -1;
        }
    }

    private int sendOnAuthenticatedFinishToHal() {
        int result = -1;
        if (!isSupportPowerFp() || !isFingerprintDReady()) {
            Slog.e(TAG, "Fingerprintd is not ready!");
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        Slog.d(TAG, "pacel  packaged :sendOnAuthenticatedFinishToHal");
        try {
            result = daemon.sendCmdToHal(MSG_AUTHENTICATEDFINISH_TO_HAL);
        } catch (RemoteException e) {
            Slog.e(TAG, "sendOnAuthenticatedFinishToHal RemoteException");
        }
        Slog.d(TAG, "fingerprintd sendOnAuthenticatedFinishToHal = " + result);
        return result;
    }

    public void saveWaitRunonAuthenticated(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
        getPowerFpParam().setAuthenticatedParam(deviceId, fingerId, groupId, token);
    }

    public void setNoWaitPowerEvent() {
        getPowerFpParam().setNoWaitPowerEvent();
    }

    private boolean isKeyguardAuthenStatus() {
        if (!isSupportPowerFp() || !isKeyguardAuthenticating() || isInCallAndTpSenser()) {
            return false;
        }
        boolean result = this.mIsKeyguardAuthenStatus.getAndSet(false);
        Slog.i(TAG, "powerfp isKeyguardAuthenStatus wakeup delayed=" + result);
        if (result) {
            this.mHandler.removeCallbacks(this.mPowerFingerWakeUpRunable);
            this.mHandler.postDelayed(this.mPowerFingerWakeUpRunable, (long) this.mFpDelayPowerTime);
        }
        return result;
    }

    private boolean isInCallAndTpSenser() {
        boolean isInCallAndTpSenser = isTpSensor() && isInCall();
        Slog.i(TAG, "current is calling and tp sensor : " + isInCallAndTpSenser);
        return isInCallAndTpSenser;
    }

    private boolean isTpSensor() {
        if (this.mTpSensorName == null) {
            Sensor proximityTpSensor = ((SensorManager) this.mContext.getSystemService(SensorManager.class)).getDefaultSensor(8);
            if (proximityTpSensor == null) {
                Slog.d(TAG, "proximity tp sensor is null .");
                return false;
            }
            this.mTpSensorName = proximityTpSensor.getName();
        }
        return PROXIMITY_TP.equals(this.mTpSensorName);
    }

    private boolean isInCall() {
        if (this.mTelecomManager == null) {
            this.mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        }
        TelecomManager telecomManager = this.mTelecomManager;
        if (telecomManager != null) {
            return telecomManager.isInCall();
        }
        return false;
    }

    public void setKeyguardAuthenScreenOn() {
        if (isSupportPowerFp() && isKeyguardAuthenticating()) {
            this.mIsKeyguardAuthenStatus.set(true);
        }
    }

    private void clearKeyguardAuthenScreenOn() {
        if (isSupportPowerFp() && isKeyguardAuthenticating()) {
            this.mHandler.removeCallbacks(this.mPowerFingerWakeUpRunable);
        }
    }

    /* access modifiers changed from: private */
    public class AuthenticatedParam {
        private long mDeviceId;
        private int mFingerId;
        private int mGroupId;
        private AtomicBoolean mIsWaitPowerEvent;
        private ArrayList<Byte> mToken;

        private AuthenticatedParam() {
            this.mIsWaitPowerEvent = new AtomicBoolean(false);
        }

        public void setAuthenticatedParam(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
            this.mDeviceId = deviceId;
            this.mFingerId = fingerId;
            this.mGroupId = groupId;
            this.mToken = token;
            this.mIsWaitPowerEvent.set(true);
        }

        public void setNoWaitPowerEvent() {
            this.mIsWaitPowerEvent.set(false);
        }

        public boolean isWaitPowerEvent() {
            return this.mIsWaitPowerEvent.getAndSet(false);
        }

        public long getDeviceId() {
            return this.mDeviceId;
        }

        public int getFingerId() {
            return this.mFingerId;
        }

        public int getGroupId() {
            return this.mGroupId;
        }

        public ArrayList<Byte> getToken() {
            return this.mToken;
        }
    }

    private void initPropHwFpType() {
        String config = SystemProperties.get("ro.config.hw_fp_type", "");
        Slog.i(TAG, "powerfp initPropHwFpType config=" + config);
        String[] bufs = config.split(",");
        if (bufs.length == 4) {
            this.mSupportPowerFp = "3".equals(bufs[0]);
            this.mSupportBlackAuthentication = "1".equals(bufs[1]);
            try {
                this.mFpDelayPowerTime = Integer.parseInt(bufs[2]);
                this.mPowerDelayFpTime = Integer.parseInt(bufs[3]);
            } catch (NumberFormatException e) {
                Slog.w(TAG, "initPropHwFpType error:" + config);
            }
        }
    }

    public void notifyFingerRemovedAtAuth(ClientMonitor client) {
        if (isSupportPowerFp()) {
            Slog.i(TAG, "power finger client.stop");
            client.stop(true);
        }
    }
}
