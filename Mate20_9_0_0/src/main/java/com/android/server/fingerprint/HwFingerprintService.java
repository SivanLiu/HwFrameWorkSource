package com.android.server.fingerprint;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.SynchronousUserSwitchObserver;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.IWindowManager;
import com.android.server.LocalServices;
import com.android.server.fingerprint.HwFingerprintSets.HwFingerprintGroup;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.PickUpWakeScreenManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.content.pm.UserInfoEx;
import com.huawei.android.os.UserManagerEx;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.fingerprint.IAuthenticator;
import com.huawei.fingerprint.IAuthenticator.Stub;
import com.huawei.fingerprint.IAuthenticatorListener;
import huawei.android.aod.HwAodManager;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.com.android.server.fingerprint.FingerViewController.ICallBack;
import huawei.com.android.server.fingerprint.FingerprintCalibrarionView;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import vendor.huawei.hardware.biometrics.fingerprint.V2_1.IExtBiometricsFingerprint;
import vendor.huawei.hardware.biometrics.fingerprint.V2_1.IFidoAuthenticationCallback;

public class HwFingerprintService extends FingerprintService {
    private static final String ACTIVITYNAME_OF_WECHAT_ENROLL = "com.tencent.mm.plugin.fingerprint.ui.FingerPrintAuthUI";
    private static final String APS_INIT_HEIGHT = "aps_init_height";
    private static final String APS_INIT_WIDTH = "aps_init_width";
    public static final int CHECK_NEED_REENROLL_FINGER = 1003;
    private static final int CODE_DISABLE_FINGERPRINT_VIEW_RULE = 1114;
    private static final int CODE_ENABLE_FINGERPRINT_VIEW_RULE = 1115;
    private static final int CODE_GET_FINGERPRINT_LIST_ENROLLED = 1118;
    private static final int CODE_GET_HARDWARE_POSITION = 1110;
    private static final int CODE_GET_HARDWARE_TYPE = 1109;
    private static final int CODE_GET_HIGHLIGHT_SPOT_RADIUS_RULE = 1122;
    private static final int CODE_GET_HOVER_SUPPORT = 1113;
    private static final int CODE_GET_TOKEN_LEN_RULE = 1103;
    private static final int CODE_IS_FINGERPRINT_HARDWARE_DETECTED = 1119;
    private static final int CODE_IS_FP_NEED_CALIBRATE_RULE = 1101;
    private static final int CODE_IS_SUPPORT_DUAL_FINGERPRINT = 1120;
    private static final int CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE = 1116;
    private static final int CODE_NOTIFY_OPTICAL_CAPTURE = 1111;
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
    private static final int DEFAULT_CAPTURE_BRIGHTNESS = 248;
    private static final int DEFAULT_RADIUS = 125;
    private static final int DENSITY_DEFUALT_HEIGHT = 2880;
    private static final int DENSITY_DEFUALT_WIDTH = 1440;
    private static final String DESCRIPTOR_FINGERPRINT_SERVICE = "android.hardware.fingerprint.IFingerprintService";
    private static final String FACE_KEYGUARD_WITH_LOCK = "face_bind_with_lock";
    private static final String FACE_RECOGNIZE_SLIDE_UNLOCK = "face_recognize_slide_unlock";
    private static final String FACE_RECOGNIZE_UNLOCK = "face_recognize_unlock";
    private static final String FIDO_ASM = "com.huawei.hwasm";
    private static final int FINGERPRINT_HARDWARE_OPTICAL = 1;
    private static final int FINGERPRINT_HARDWARE_OUTSCREEN = 0;
    private static final int FINGERPRINT_HARDWARE_ULTRASONIC = 2;
    private static final String FINGERPRINT_METADATA_KEY = "fingerprint.system.view";
    private static final int FLAG_FINGERPRINT_LOCATION_BACK = 1;
    private static final int FLAG_FINGERPRINT_LOCATION_FRONT = 2;
    private static final int FLAG_FINGERPRINT_LOCATION_UNDER_DISPLAY = 4;
    private static final int FLAG_FINGERPRINT_POSITION_MASK = 65535;
    private static final int FLAG_FINGERPRINT_TYPE_CAPACITANCE = 1;
    private static final int FLAG_FINGERPRINT_TYPE_MASK = 15;
    private static final int FLAG_FINGERPRINT_TYPE_OPTICAL = 2;
    private static final int FLAG_FINGERPRINT_TYPE_ULTRASONIC = 3;
    private static final int FLAG_USE_UD_FINGERPRINT = 134217728;
    private static final int FP_CLOSE = 0;
    private static final int FP_OPEN = 1;
    public static final int GET_OLD_DATA = 100;
    private static final int HIDDEN_SPACE_ID = -100;
    private static final int INVALID_VALUE = -1;
    private static final String KEY_DB_CHILDREN_MODE_FPID = "fp_children_mode_fp_id";
    private static final String KEY_DB_CHILDREN_MODE_STATUS = "fp_children_enabled";
    private static final String KEY_KEYGUARD_ENABLE = "fp_keyguard_enable";
    public static final int MASK_TYPE_BACK = 4;
    public static final int MASK_TYPE_BUTTON = 1;
    public static final int MASK_TYPE_FULL = 0;
    public static final int MASK_TYPE_IMAGE = 3;
    public static final int MASK_TYPE_NONE = 2;
    private static final String METADATA_KEY = "fingerprint.system.view";
    private static final int MSG_CHECK_AND_DEL_TEMPLATES = 84;
    private static final int MSG_CHECK_HOVER_SUPPORT = 56;
    private static final int MSG_CHECK_OLD_TEMPLATES = 85;
    private static final int MSG_CHECK_SWITCH_FREQUENCE_SUPPORT = 81;
    private static final int MSG_DEL_OLD_TEMPLATES = 86;
    private static final int MSG_DISABLE_FACE_RECOGNIZATION = 210;
    private static final int MSG_ENABLE_FACE_RECOGNIZATION = 211;
    private static final int MSG_FACE_RECOGNIZATION_SUCC = 212;
    private static final int MSG_GET_BRIGHTNEWSS_FROM_HAL = 909;
    private static final int MSG_GET_RADIUS_FROM_HAL = 902;
    private static final int MSG_GET_SENSOR_POSITION_BOTTOM_RIGHT = 60;
    private static final int MSG_GET_SENSOR_POSITION_TOP_LEFT = 59;
    private static final int MSG_GET_SENSOR_TYPE = 55;
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
    private static final int MSG_UPGRADING_FREQUENCE = 82;
    private static final String PATH_CHILDMODE_STATUS = "childmode_status";
    private static final String PKGNAME_OF_KEYGUARD = "com.android.systemui";
    private static final String PKGNAME_OF_SETTINGS = "com.android.settings";
    private static final String PKGNAME_OF_WECHAT = "com.tencent.mm";
    private static final long POWER_PUSH_DOWN_TIME_THR = 430;
    private static final int PRIMARY_USER_ID = 0;
    public static final int REMOVE_USER_DATA = 101;
    public static final int RESTORE_AUTHENTICATE = 0;
    public static final int RESTORE_ENROLL = 0;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_CHECKING = 3;
    public static final int SET_LIVENESS_SWITCH = 1002;
    private static final int STATUS_PARENT_CTRL_OFF = 0;
    private static final int STATUS_PARENT_CTRL_STUDENT = 1;
    public static final int SUSPEND_AUTHENTICATE = 1;
    public static final int SUSPEND_ENROLL = 1;
    private static final int SWITCH_FREQUENCE_SUPPORT = 1;
    private static final String TAG = "HwFingerprintService";
    public static final int TYPE_DISMISS_ = 0;
    public static final int TYPE_FINGERPRINT_BUTTON = 2;
    public static final int TYPE_FINGERPRINT_VIEW = 1;
    private static final int UNDEFINED_TYPE = -1;
    private static final int UPDATE_SECURITY_USER_ID = 102;
    public static final int VERIFY_USER = 1001;
    private static boolean mCheckNeedEnroll = true;
    private static boolean mIsChinaArea = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static boolean mLivenessNeedBetaQualification = false;
    private static boolean mNeedRecreateDialog = false;
    private static boolean mRemoveFingerprintBGE = SystemProperties.getBoolean("ro.config.remove_finger_bge", false);
    private static boolean mRemoveOldTemplatesFeature = SystemProperties.getBoolean("ro.config.remove_old_templates", false);
    private final int DEFAULT_SCREEN_SIZE_STRING_LENGHT = 2;
    private final int MSG_SCREEOFF_UNLOCK_LIGHTBRIGHT = 200;
    private final int MSG_SCREEON_UNLOCK_LIGHTBRIGHT = 201;
    private final int SCREENOFF_UNLOCK = 1;
    private final int SCREENON_UNLOCK = 2;
    ContentObserver fpObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            if (HwFingerprintService.this.mContext == null) {
                Log.w(HwFingerprintService.TAG, "mContext is null");
                return;
            }
            HwFingerprintService.this.mState = Secure.getIntForUser(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
            if (HwFingerprintService.this.mState == 0) {
                HwFingerprintService.this.sendCommandToHal(0);
            }
            String str = HwFingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fp_keyguard_state onChange: ");
            stringBuilder.append(HwFingerprintService.this.mState);
            Log.i(str, stringBuilder.toString());
        }
    };
    private int mAppDefinedMaskType = -1;
    private final Context mContext;
    private String mDefinedAppName = "";
    private DisplayEngineManager mDisplayEngineManager;
    private FingerViewController mFingerViewController;
    private int mFingerprintType = -1;
    private boolean mForbideKeyguardCall = false;
    private IAuthenticator mIAuthenticator = new Stub() {
        public int verifyUser(IFingerprintServiceReceiver receiver, IAuthenticatorListener listener, int userid, byte[] nonce, String aaid) {
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
                String str = HwFingerprintService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uid =");
                stringBuilder.append(uid);
                Log.d(str, stringBuilder.toString());
                if (uid != 1000) {
                    Log.e(HwFingerprintService.TAG, "permission denied.");
                    return -1;
                }
                Class[] paramTypes = new Class[]{String.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE};
                Object[] params = new Object[]{HwFingerprintService.FIDO_ASM, Boolean.valueOf(true), Integer.valueOf(uid), Integer.valueOf(pid), Integer.valueOf(userid)};
                if (((Boolean) HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "canUseFingerprint", paramTypes, params)).booleanValue()) {
                    int effectiveGroupId = HwFingerprintService.this.getEffectiveUserId(userid);
                    final IFingerprintServiceReceiver iFingerprintServiceReceiver = receiver;
                    final int callingUserId = UserHandle.getCallingUserId();
                    AnonymousClass1 anonymousClass1 = r0;
                    final int i = effectiveGroupId;
                    Handler handler = HwFingerprintService.this.mHandler;
                    final IAuthenticatorListener iAuthenticatorListener = listener;
                    final String str2 = aaid;
                    params = nonce;
                    AnonymousClass1 anonymousClass12 = new Runnable() {
                        public void run() {
                            HwFingerprintService.this.setLivenessSwitch("fido");
                            HwFingerprintService.this.startAuthentication(iFingerprintServiceReceiver.asBinder(), 0, callingUserId, i, iFingerprintServiceReceiver, 0, true, HwFingerprintService.FIDO_ASM, iAuthenticatorListener, str2, params);
                        }
                    };
                    handler.post(anonymousClass1);
                    return 0;
                }
                Log.w(HwFingerprintService.TAG, "FIDO_ASM can't use fingerprint");
                return -1;
            }
        }
    };
    private IWindowManager mIWm;
    private int mInitDisplayHeight = -1;
    private int mInitDisplayWidth = -1;
    private boolean mIsFaceDetectEnabled;
    private boolean mIsFingerInScreenSupported;
    private boolean mIsHighLightNeed;
    private boolean mIsSupportDualFingerprint = false;
    private boolean mIsUdAuthenticating = false;
    private boolean mIsUdEnrolling = false;
    private boolean mIsUdFingerprintChecking = false;
    private boolean mIsUdFingerprintNeed = true;
    private boolean mKeepMaskAfterAuthentication = false;
    private Bundle mMaskViewBundle;
    private ContentObserver mNavModeObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            if (HwFingerprintService.this.mContext == null || HwFingerprintService.this.mContext.getContentResolver() == null) {
                Log.d(HwFingerprintService.TAG, "mContext or the resolver is null");
                return;
            }
            boolean virNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(HwFingerprintService.this.mContext.getContentResolver());
            if (HwFingerprintService.this.mVirNavModeEnabled != virNavModeEnabled) {
                HwFingerprintService.this.mVirNavModeEnabled = virNavModeEnabled;
                String str = HwFingerprintService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Navigation mode changed, mVirNavModeEnabled = ");
                stringBuilder.append(HwFingerprintService.this.mVirNavModeEnabled);
                Log.d(str, stringBuilder.toString());
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.this.mVirNavModeEnabled ? 45 : 43);
            }
        }
    };
    private boolean mNeedResumeTouchSwitch = false;
    private String mPackageDisableMask;
    private AlertDialog mReEnrollDialog;
    private BroadcastReceiver mReceiver;
    private String mScreen;
    protected final ContentObserver mSettingsObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            HwFingerprintService.this.notifyCfgSettingsIfNeed(false);
        }
    };
    private int mState = 0;
    private BroadcastReceiver mSwitchFrequenceMonitor = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.SCREEN_ON") || action.equals("android.intent.action.SCREEN_OFF")) {
                    HwFingerprintService.this.mScreen = action;
                    HwFingerprintService.this.handleScreenOnOrOff();
                }
            }
        }
    };
    private BroadcastReceiver mUserDeletedMonitor = new BroadcastReceiver() {
        private static final String FP_DATA_DIR = "fpdata";

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                int userId;
                int newUserId;
                if (action.equals("android.intent.action.USER_REMOVED")) {
                    userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    String str = HwFingerprintService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("user deleted:");
                    stringBuilder.append(userId);
                    Slog.i(str, stringBuilder.toString());
                    if (userId == -1) {
                        Slog.i(HwFingerprintService.TAG, "get User id failed");
                        return;
                    }
                    newUserId = userId;
                    int newPathId = userId;
                    if (UserManagerEx.isHwHiddenSpace(UserManagerEx.getUserInfoEx(UserManager.get(HwFingerprintService.this.mContext), userId))) {
                        newUserId = -100;
                        newPathId = 0;
                    }
                    File fpDir = new File(Environment.getFingerprintFileDirectory(newPathId), FP_DATA_DIR);
                    if (fpDir.exists()) {
                        HwFingerprintService.this.removeUserData(newUserId, fpDir.getAbsolutePath());
                    } else {
                        Slog.v(HwFingerprintService.TAG, "no fpdata!");
                    }
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    HwFingerprintService.this.sendCommandToHal(63);
                    if (HwFingerprintService.mCheckNeedEnroll) {
                        if (HwFingerprintService.mRemoveOldTemplatesFeature) {
                            if (HwFingerprintService.mIsChinaArea) {
                                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_CHECK_AND_DEL_TEMPLATES);
                            } else {
                                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_CHECK_OLD_TEMPLATES);
                            }
                        }
                        userId = HwFingerprintService.this.checkNeedReEnrollFingerPrints();
                        newUserId = HwFingerprintService.this.checkNeedCalibrateFingerPrint();
                        String str2 = HwFingerprintService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("USER_PRESENT mUserDeletedMonitor need enrol : ");
                        stringBuilder2.append(userId);
                        stringBuilder2.append("need calibrate:");
                        stringBuilder2.append(newUserId);
                        Log.e(str2, stringBuilder2.toString());
                        if (HwFingerprintService.mRemoveOldTemplatesFeature) {
                            if (HwFingerprintService.mIsChinaArea && userId == 1) {
                                HwFingerprintService.this.updateActiveGroupEx(-100);
                                HwFingerprintService.this.updateActiveGroupEx(0);
                                HwFingerprintService.this.showDialog(false);
                            } else if (!HwFingerprintService.mIsChinaArea && userId == 3) {
                                HwFingerprintService.this.showDialog(true);
                            }
                        } else if (userId == 1 && newUserId != 1) {
                            HwFingerprintService.this.intentOthers(context);
                        }
                        HwFingerprintService.mCheckNeedEnroll = false;
                    }
                    if (HwFingerprintService.this.mFingerViewController != null && "com.android.systemui".equals(HwFingerprintService.this.mFingerViewController.getCurrentPackage())) {
                        Log.d(HwFingerprintService.TAG, "USER_PRESENT removeMaskOrButton");
                        HwFingerprintService.this.mFingerViewController.removeMaskOrButton();
                    }
                    HwFingerprintService.this.mForbideKeyguardCall = false;
                }
            }
        }
    };
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.d(HwFingerprintService.TAG, "intent is null");
                return;
            }
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                HwFingerprintService.this.mVirNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(HwFingerprintService.this.mContext.getContentResolver());
                String str = HwFingerprintService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Read the navigation mode after user switch, mVirNavModeEnabled = ");
                stringBuilder.append(HwFingerprintService.this.mVirNavModeEnabled);
                Log.d(str, stringBuilder.toString());
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.this.mVirNavModeEnabled ? 45 : 43);
            }
        }
    };
    private boolean mVirNavModeEnabled = false;
    private HashSet<String> mWhitelist = new HashSet();
    private boolean mflagFirstIn = true;
    private long mtimeStart = 0;
    private final Runnable screenOnOrOffRunnable = new Runnable() {
        public void run() {
            boolean z = false;
            int fpState = Secure.getIntForUser(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
            if (FingerprintUtils.getInstance().getFingerprintsForUser(HwFingerprintService.this.mContext, ActivityManager.getCurrentUser()).size() > 0) {
                z = true;
            }
            boolean hasFingerprints = z;
            if (fpState != 0 && hasFingerprints) {
                return;
            }
            if ("android.intent.action.SCREEN_ON".equals(HwFingerprintService.this.mScreen)) {
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_UPGRADING_FREQUENCE);
            } else if ("android.intent.action.SCREEN_OFF".equals(HwFingerprintService.this.mScreen)) {
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_REDUCING_FREQUENCE);
            }
        }
    };

    private class HwFIDOAuthenticationClient extends AuthenticationClient {
        private String aaid;
        private int groupId;
        private IAuthenticatorListener listener;
        private IFidoAuthenticationCallback mFidoAuthenticationCallback = new IFidoAuthenticationCallback.Stub() {
            public void onUserVerificationResult(final int result, long opId, final ArrayList<Byte> userId, final ArrayList<Byte> encapsulatedResult) {
                Log.d(HwFingerprintService.TAG, "onUserVerificationResult");
                HwFIDOAuthenticationClient.this.this$0.mHandler.post(new Runnable() {
                    public void run() {
                        Log.d(HwFingerprintService.TAG, "onUserVerificationResult-run");
                        if (HwFIDOAuthenticationClient.this.listener != null) {
                            try {
                                byte[] byteUserId = new byte[userId.size()];
                                int userIdLen = userId.size();
                                for (int i = 0; i < userIdLen; i++) {
                                    byteUserId[i] = ((Byte) userId.get(i)).byteValue();
                                }
                                byte[] byteEncapsulatedResult = new byte[encapsulatedResult.size()];
                                int encapsulatedResultLen = encapsulatedResult.size();
                                for (userIdLen = 0; userIdLen < encapsulatedResultLen; userIdLen++) {
                                    byteEncapsulatedResult[userIdLen] = ((Byte) encapsulatedResult.get(userIdLen)).byteValue();
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
        final /* synthetic */ HwFingerprintService this$0;
        private int user_id;

        public HwFIDOAuthenticationClient(HwFingerprintService hwFingerprintService, Context context, long halDeviceId, IBinder token, IFingerprintServiceReceiver receiver, int callingUserId, int groupId, long opId, boolean restricted, String owner, IAuthenticatorListener listener, String aaid, byte[] nonce) {
            this.this$0 = hwFingerprintService;
            super(context, halDeviceId, token, receiver, callingUserId, groupId, opId, restricted, owner, null, null, null);
            this.pkgName = owner;
            this.listener = listener;
            this.groupId = groupId;
            this.aaid = aaid;
            this.nonce = nonce;
            this.user_id = callingUserId;
        }

        public boolean onAuthenticated(int fingerId, int groupId) {
            if (fingerId != 0) {
            }
            return super.onAuthenticated(fingerId, groupId);
        }

        public int handleFailedAttempt() {
            int currentUser = ActivityManager.getCurrentUser();
            SparseIntArray failedAttempts = (SparseIntArray) HwFingerprintService.getParentPrivateField(this.this$0, "mFailedAttempts");
            failedAttempts.put(currentUser, failedAttempts.get(currentUser, 0) + 1);
            int lockoutMode = this.this$0.getLockoutMode();
            if (!inLockoutMode()) {
                return 0;
            }
            HwFingerprintService.setParentPrivateField(this.this$0, "mLockoutTime", Long.valueOf(SystemClock.elapsedRealtime()));
            HwFingerprintService.invokeParentPrivateFunction(this.this$0, "scheduleLockoutResetForUser", new Class[]{Integer.TYPE}, new Object[]{Integer.valueOf(this.user_id)});
            onError(7, 0);
            stop(true);
            return lockoutMode;
        }

        public void resetFailedAttempts() {
            HwFingerprintService.invokeParentPrivateFunction(this.this$0, "resetFailedAttemptsForUser", new Class[]{Boolean.TYPE, Integer.TYPE}, new Object[]{Boolean.valueOf(true), Integer.valueOf(this.user_id)});
        }

        public void notifyUserActivity() {
            HwFingerprintService.invokeParentPrivateFunction(this.this$0, "userActivity", null, null);
        }

        public IBiometricsFingerprint getFingerprintDaemon() {
            return (IBiometricsFingerprint) HwFingerprintService.invokeParentPrivateFunction(this.this$0, "getFingerprintDaemon", null, null);
        }

        public void handleHwFailedAttempt(int flags, String packagesName) {
            HwFingerprintService.invokeParentPrivateFunction(this.this$0, "handleHwFailedAttempt", new Class[]{Integer.TYPE, String.class}, new Object[]{Integer.valueOf(0), null});
        }

        public boolean inLockoutMode() {
            return ((Boolean) HwFingerprintService.invokeParentPrivateFunction(this.this$0, "inLockoutMode", null, null)).booleanValue();
        }

        public int start() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start pkgName:");
            stringBuilder.append(this.pkgName);
            Slog.d("FingerprintService", stringBuilder.toString());
            try {
                doVerifyUser(this.groupId, this.aaid, this.nonce);
            } catch (RemoteException e) {
                Log.w(HwFingerprintService.TAG, "call fingerprintD verify user failed");
            }
            return 0;
        }

        public void onStart() {
        }

        public void onStop() {
        }

        private void doVerifyUser(int groupId, String aaid, byte[] nonce) throws RemoteException {
            if (this.this$0.isFingerprintDReady()) {
                IExtBiometricsFingerprint daemon = this.this$0.getFingerprintDaemonEx();
                if (daemon == null) {
                    Slog.e("FingerprintService", "Fingerprintd is not available!");
                    return;
                }
                ArrayList<Byte> arrayNonce = new ArrayList();
                for (byte valueOf : nonce) {
                    arrayNonce.add(Byte.valueOf(valueOf));
                }
                try {
                    daemon.verifyUser(this.mFidoAuthenticationCallback, groupId, aaid, arrayNonce);
                } catch (RemoteException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("doVerifyUser RemoteException:");
                    stringBuilder.append(e);
                    Slog.e("FingerprintService", stringBuilder.toString());
                }
            }
        }
    }

    private class FingerViewChangeCallback implements ICallBack {
        private FingerViewChangeCallback() {
        }

        /* synthetic */ FingerViewChangeCallback(HwFingerprintService x0, AnonymousClass1 x1) {
            this();
        }

        public void onFingerViewStateChange(int type) {
            String str = HwFingerprintService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("View State Change to ");
            stringBuilder.append(type);
            Log.d(str, stringBuilder.toString());
            HwFingerprintService.this.suspendAuthentication(type == 2 ? 1 : 0);
        }

        public void onNotifyCaptureImage() {
            Log.d(HwFingerprintService.TAG, "onNotifyCaptureImage");
            HwFingerprintService.this.notifyCaptureOpticalImage();
        }

        public void onNotifyBlueSpotDismiss() {
            HwFingerprintService.this.notifyBluespotDismiss();
        }
    }

    private IExtBiometricsFingerprint getFingerprintDaemonEx() {
        IExtBiometricsFingerprint mDaemonEx = null;
        try {
            mDaemonEx = IExtBiometricsFingerprint.getService();
        } catch (NoSuchElementException e) {
        } catch (RemoteException e2) {
            Slog.e(TAG, "Failed to get biometric interface", e2);
        }
        if (mDaemonEx == null) {
            Slog.w(TAG, "fingerprint HIDL not available");
            return null;
        }
        mDaemonEx.asBinder().linkToDeath(this, 0);
        return mDaemonEx;
    }

    private int getkidsFingerId(String whichMode, int userID, Context context) {
        if (context != null) {
            return Secure.getIntForUser(context.getContentResolver(), whichMode, 0, userID);
        }
        Slog.w(TAG, "getkidsFingerId - context = null");
        return 0;
    }

    private boolean isKidSwitchOn(int userID, Context context) {
        if (1 == Secure.getIntForUser(context.getContentResolver(), KEY_DB_CHILDREN_MODE_STATUS, 0, userID)) {
            return true;
        }
        return false;
    }

    private boolean isParentControl(int userID, Context context) {
        boolean isInStudent = false;
        if (context == null || context.getContentResolver() == null) {
            return false;
        }
        int status = Secure.getIntForUser(context.getContentResolver(), PATH_CHILDMODE_STATUS, 0, userID);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ParentControl status is ");
        stringBuilder.append(status);
        Slog.d(str, stringBuilder.toString());
        if (status == 1) {
            isInStudent = true;
        }
        return isInStudent;
    }

    protected void setKidsFingerprint(int userID, boolean isKeyguard) {
        Slog.d(TAG, "setKidsFingerprint:start");
        int kidFpId = getkidsFingerId(KEY_DB_CHILDREN_MODE_FPID, userID, this.mContext);
        if (kidFpId != 0) {
            boolean isParent = isParentControl(userID, this.mContext);
            boolean isPcCastMode = HwPCUtils.isPcCastModeInServer();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setKidsFingerprint-isParent = ");
            stringBuilder.append(isParent);
            stringBuilder.append(", isPcCastMode =");
            stringBuilder.append(isPcCastMode);
            Slog.d(str, stringBuilder.toString());
            if (isKeyguard && isKidSwitchOn(userID, this.mContext) && !isParent && !isPcCastMode) {
                kidFpId = 0;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setKidsFingerprint-kidFpId = ");
            stringBuilder.append(kidFpId);
            Slog.d(str, stringBuilder.toString());
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            try {
                daemon.setKidsFingerprint(kidFpId);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setLivenessSwitch RemoteException:");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
            }
            Slog.d(TAG, "framework setLivenessSwitch is ok ---end");
        }
    }

    private void startAuthentication(IBinder token, long opId, int callingUserId, int groupId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName, IAuthenticatorListener listener, String aaid, byte[] nonce) {
        boolean z;
        String str = opPackageName;
        Class[] paramTypes = new Class[]{Integer.TYPE, String.class};
        Object[] params = new Object[]{Integer.valueOf(groupId), str};
        invokeParentPrivateFunction(this, "updateActiveGroup", paramTypes, params);
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwFingerprintService-startAuthentication(");
        stringBuilder.append(str);
        stringBuilder.append(")");
        Log.v(str2, stringBuilder.toString());
        String str3 = str;
        AuthenticationClient client = new HwFIDOAuthenticationClient(this, getContext(), 0, token, receiver, callingUserId, groupId, opId, restricted, str, listener, aaid, nonce);
        int i;
        if (((Boolean) invokeParentPrivateFunction(this, "inLockoutMode", null, null)).booleanValue()) {
            z = true;
            Class[] clsArr = new Class[1];
            i = 0;
            clsArr[0] = String.class;
            if (!((Boolean) invokeParentPrivateFunction(this, "isKeyguard", clsArr, new Object[]{str3})).booleanValue()) {
                Log.v(TAG, "In lockout mode; disallowing authentication");
                if (!client.onError(7, 0)) {
                    Log.w(TAG, "Cannot send timeout message to client");
                }
                return;
            }
        }
        z = true;
        i = 0;
        invokeParentPrivateFunction(this, "startClient", new Class[]{ClientMonitor.class, Boolean.TYPE}, new Object[]{client, Boolean.valueOf(z)});
    }

    private static Field getAccessibleField(Class targetClass, String variableName) {
        try {
            final Field field = targetClass.getDeclaredField(variableName);
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    field.setAccessible(true);
                    return null;
                }
            });
            return field;
        } catch (Exception e) {
            Log.v(TAG, "getAccessibleField error", e);
            return null;
        }
    }

    private static Object getParentPrivateField(Object instance, String variableName) {
        Class targetClass = instance.getClass().getSuperclass();
        Object superInst = targetClass.cast(instance);
        Field field = getAccessibleField(targetClass, variableName);
        if (field != null) {
            try {
                return field.get(superInst);
            } catch (IllegalAccessException e) {
                Log.v(TAG, "getParentPrivateField error", e);
            }
        }
        return null;
    }

    private static void setParentPrivateField(Object instance, String variableName, Object value) {
        Class targetClass = instance.getClass().getSuperclass();
        Object superInst = targetClass.cast(instance);
        Field field = getAccessibleField(targetClass, variableName);
        if (field != null) {
            try {
                field.set(superInst, value);
            } catch (IllegalAccessException e) {
                Log.v(TAG, "setParentPrivateField error", e);
            }
        }
    }

    private static Object invokeParentPrivateFunction(Object instance, String method, Class[] paramTypes, Object[] params) {
        Class targetClass = instance.getClass().getSuperclass();
        Object superInst = targetClass.cast(instance);
        try {
            final Method med = targetClass.getDeclaredMethod(method, paramTypes);
            AccessController.doPrivileged(new PrivilegedAction() {
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

    private void showDialog(final boolean withConfirm) {
        Builder builder = new Builder(this.mContext, 33947691).setPositiveButton(withConfirm ? 33686166 : 33686163, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                HwFingerprintService.mNeedRecreateDialog = false;
                if (withConfirm) {
                    HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_DEL_OLD_TEMPLATES);
                    if (HwFingerprintService.this.checkNeedReEnrollFingerPrints() == 1) {
                        HwFingerprintService.this.updateActiveGroupEx(-100);
                        HwFingerprintService.this.updateActiveGroupEx(0);
                    }
                }
                HwFingerprintService.this.intentOthers(HwFingerprintService.this.mContext);
            }
        }).setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if (!HwFingerprintService.mNeedRecreateDialog) {
                    HwFingerprintService.this.unRegisterPhoneStateReceiver();
                }
            }
        }).setTitle(this.mContext.getString(33685797)).setMessage(this.mContext.getString(withConfirm ? 33686165 : 33686164));
        if (withConfirm) {
            builder.setNegativeButton(17039360, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    HwFingerprintService.mNeedRecreateDialog = false;
                    HwFingerprintService.this.mReEnrollDialog.dismiss();
                }
            });
        }
        this.mReEnrollDialog = builder.create();
        if (this.mReEnrollDialog != null) {
            this.mReEnrollDialog.getWindow().setType(2003);
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
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null) {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
                    if (telephonyManager != null && HwFingerprintService.this.mReEnrollDialog != null) {
                        if (telephonyManager.getCallState() == 1 && HwFingerprintService.this.mReEnrollDialog.isShowing()) {
                            HwFingerprintService.mNeedRecreateDialog = true;
                            HwFingerprintService.this.mReEnrollDialog.dismiss();
                        } else if (telephonyManager.getCallState() == 0 && !HwFingerprintService.this.mReEnrollDialog.isShowing()) {
                            HwFingerprintService.this.mReEnrollDialog.show();
                        }
                    }
                }
            }
        };
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.mReceiver, filter);
        }
    }

    private void unRegisterPhoneStateReceiver() {
        if (this.mReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    private void updateActiveGroupEx(int userId) {
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon != null) {
            int userIdForHal = userId;
            try {
                File systemDir;
                UserInfoEx infoEx = UserManagerEx.getUserInfoEx(UserManager.get(this.mContext), userId);
                if (infoEx != null && UserManagerEx.isHwHiddenSpace(infoEx)) {
                    userIdForHal = -100;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("userIdForHal is ");
                    stringBuilder.append(-100);
                    Slog.i(str, stringBuilder.toString());
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
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cannot make directory: ");
                        stringBuilder2.append(fpDir.getAbsolutePath());
                        Slog.v(str2, stringBuilder2.toString());
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

    private void handleScreenOnOrOff() {
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

    private void intentOthers(Context context) {
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
    }

    private void initObserver() {
        if (this.fpObserver == null) {
            Log.w(TAG, "fpObserver is null");
            return;
        }
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor(KEY_KEYGUARD_ENABLE), false, this.fpObserver, -1);
        this.mState = Secure.getIntForUser(this.mContext.getContentResolver(), KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
        this.fpObserver.onChange(true);
    }

    private void updateTPState(String opPackageName, boolean udFingerprintExist) {
        if (!"com.android.systemui".equals(opPackageName)) {
            sendCommandToHal(1);
        } else if (!udFingerprintExist || this.mState == 0) {
            sendCommandToHal(0);
        } else {
            sendCommandToHal(1);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateTPState udFingerprintExist ");
        stringBuilder.append(udFingerprintExist);
        stringBuilder.append(",opPackageName:");
        stringBuilder.append(opPackageName);
        Log.i(str, stringBuilder.toString());
    }

    private void initNavModeObserver() {
        if (this.mNavModeObserver == null) {
            Log.d(TAG, "mNavModeObserver is null");
        } else if (this.mContext == null || this.mContext.getContentResolver() == null) {
            Log.d(TAG, "mContext or the resolver is null");
        } else {
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("enable_navbar"), true, this.mNavModeObserver, -1);
            this.mVirNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(this.mContext.getContentResolver());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Read the navigation mode after boot, mVirNavModeEnabled = ");
            stringBuilder.append(this.mVirNavModeEnabled);
            Log.d(str, stringBuilder.toString());
            sendCommandToHal(this.mVirNavModeEnabled ? 45 : 43);
        }
    }

    private void initUserSwitchReceiver() {
        if (this.mUserSwitchReceiver == null) {
            Log.d(TAG, "mUserSwitchReceiver is null");
        } else if (this.mContext == null) {
            Log.d(TAG, "mContext is null");
        } else {
            this.mContext.registerReceiverAsUser(this.mUserSwitchReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
        }
    }

    public void onStart() {
        super.onStart();
        publishBinderService("fido_authenticator", this.mIAuthenticator.asBinder());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mUserDeletedMonitor, filter);
        Slog.v(TAG, "HwFingerprintService onstart");
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                public void onUserSwitching(int newUserId) {
                    if (HwFingerprintService.this.mFingerViewController != null) {
                        Slog.v(HwFingerprintService.TAG, "onUserSwitching removeMaskOrButton");
                        HwFingerprintService.this.mFingerViewController.removeMaskOrButton();
                        HwFingerprintService.this.mForbideKeyguardCall = true;
                    }
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    Slog.v(HwFingerprintService.TAG, "onUserSwitchComplete ");
                    HwFingerprintService.this.mForbideKeyguardCall = false;
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
        }
        this.mForbideKeyguardCall = false;
    }

    private int getSwitchFrequenceSupport() {
        int result = -1;
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        try {
            result = daemon.sendCmdToHal(81);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkSwitchFrequenceSupport RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        return result;
    }

    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwFingerprintService onBootPhase:");
        stringBuilder.append(phase);
        Slog.v(str, stringBuilder.toString());
        if (phase == 1000) {
            initSwitchFrequence();
            initPositionAndType();
            if (mRemoveFingerprintBGE) {
                initUserSwitchReceiver();
                initNavModeObserver();
            }
            notifyCfgSettingsIfNeed(true);
        }
    }

    private void initSwitchFrequence() {
        if (getSwitchFrequenceSupport() == 1) {
            IntentFilter filterScreen = new IntentFilter();
            filterScreen.addAction("android.intent.action.SCREEN_ON");
            filterScreen.addAction("android.intent.action.SCREEN_OFF");
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
        if (hwFpSets != null) {
            FingerprintUtils utils = FingerprintUtils.getInstance();
            if (utils != null) {
                ArrayList<Fingerprint> mNewFingerprints = null;
                int fingerprintGpSize = hwFpSets.mFingerprintGroups.size();
                for (int i = 0; i < fingerprintGpSize; i++) {
                    HwFingerprintGroup fpGroup = (HwFingerprintGroup) hwFpSets.mFingerprintGroups.get(i);
                    int realGroupId = fpGroup.mGroupId;
                    if (fpGroup.mGroupId == -100) {
                        realGroupId = getRealUserIdForApp(fpGroup.mGroupId);
                    }
                    if (realGroupId == userId) {
                        mNewFingerprints = fpGroup.mFingerprints;
                    }
                }
                if (mNewFingerprints == null) {
                    mNewFingerprints = new ArrayList();
                }
                for (Fingerprint oldFp : utils.isDualFp() ? utils.getFingerprintsForUser(this.mContext, userId, deviceIndex) : utils.getFingerprintsForUser(this.mContext, userId)) {
                    if (!checkItemExist(oldFp.getFingerId(), mNewFingerprints)) {
                        utils.removeFingerprintIdForUser(this.mContext, oldFp.getFingerId(), userId);
                    }
                }
                int size = mNewFingerprints.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Fingerprint fp = (Fingerprint) mNewFingerprints.get(i2);
                    if (utils.isDualFp()) {
                        utils.addFingerprintForUser(this.mContext, fp.getFingerId(), userId, deviceIndex);
                    } else {
                        utils.addFingerprintForUser(this.mContext, fp.getFingerId(), userId);
                    }
                    CharSequence fpName = fp.getName();
                    if (!(fpName == null || fpName.toString().isEmpty())) {
                        utils.renameFingerprintForUser(this.mContext, fp.getFingerId(), userId, fpName);
                    }
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("timepassed is  ");
        stringBuilder.append(timePassed);
        Slog.v(str, stringBuilder.toString());
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
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkNeedReEnrollFingerPrints RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        return 0;
    }

    private int checkNeedReEnrollFingerPrints() {
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkNeedReEnrollFingerPrints RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("framework checkNeedReEnrollFingerPrints is finish return = ");
        stringBuilder2.append(result);
        Log.w(str2, stringBuilder2.toString());
        return result;
    }

    public boolean onHwTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        int result;
        int result2;
        if (code == CODE_IS_FP_NEED_CALIBRATE_RULE) {
            Slog.d(TAG, "code == CODE_IS_FP_NEED_CALIBRATE_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            result = checkNeedCalibrateFingerPrint();
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
            result = getTokenLen();
            reply.writeNoException();
            reply.writeInt(result);
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
        } else if (code == CODE_GET_HARDWARE_TYPE) {
            Slog.d(TAG, "CODE_GET_HARDWARE_TYPE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            result = getFingerprintHardwareType();
            reply.writeNoException();
            reply.writeInt(result);
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
            result = getHoverEventSupport();
            reply.writeNoException();
            reply.writeInt(result);
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
        } else if (code == 1119) {
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
        } else if (code == 1120) {
            Slog.d(TAG, "CODE_IS_SUPPORT_DUAL_FINGERPRINT");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            reply.writeNoException();
            reply.writeBoolean(isSupportDualFingerprint());
            return true;
        } else if (code == 1121) {
            Slog.d(TAG, "CODE_SEND_UNLOCK_LIGHTBRIGHT");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            result2 = sendUnlockAndLightbright(data.readInt());
            reply.writeNoException();
            reply.writeInt(result2);
            return true;
        } else if (code == 1122) {
            Slog.d(TAG, "CODE_GET_HIGHLIGHT_SPOT_RADIUS_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            result = getHighLightspotRadius();
            reply.writeNoException();
            reply.writeInt(result);
            return true;
        } else if (code != 1123) {
            return super.onHwTransact(code, data, reply, flags);
        } else {
            Slog.d(TAG, "CODE_SUSPEND_ENROLL");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            result2 = suspendEnroll(data.readInt());
            reply.writeNoException();
            reply.writeInt(result2);
            return true;
        }
    }

    protected boolean isSupportDualFingerprint() {
        if (!this.mIsSupportDualFingerprint) {
            int typeDetails = getFingerprintHardwareType();
            if (!(typeDetails == -1 || (typeDetails & 1) == 0 || (typeDetails & 4) == 0)) {
                FingerprintUtils.getInstance().setDualFp(true);
                this.mIsSupportDualFingerprint = true;
                this.mWhitelist.add("com.android.settings");
                this.mWhitelist.add("com.android.systemui");
                this.mWhitelist.add("com.huawei.aod");
                this.mWhitelist.add(HwRecentsTaskUtils.PKG_SYS_MANAGER);
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
        }
        return this.mIsSupportDualFingerprint;
    }

    protected int sendCommandToHal(int command) {
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            return -1;
        }
        try {
            daemon.sendCmdToHal(command);
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dualfingerprint sendCmdToHal RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
    }

    protected boolean canUseUdFingerprint(String opPackageName) {
        String str;
        StringBuilder stringBuilder;
        if (opPackageName == null || "".equals(opPackageName)) {
            Slog.d(TAG, "calling opPackageName is invalid");
            return false;
        } else if (opPackageName.equals(this.mDefinedAppName)) {
            return true;
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("canUseUdFingerprint opPackageName is ");
            stringBuilder2.append(opPackageName);
            Slog.d(str2, stringBuilder2.toString());
            Iterator it = this.mWhitelist.iterator();
            while (it.hasNext()) {
                if (((String) it.next()).equals(opPackageName)) {
                    return true;
                }
            }
            long token = Binder.clearCallingIdentity();
            try {
                str2 = "";
                Bundle metaData = this.mContext.getPackageManager().getApplicationInfo(opPackageName, 128).metaData;
                if (metaData == null) {
                    Slog.d(TAG, "metaData is null");
                }
                if (metaData != null) {
                    str2 = metaData.getString("fingerprint.system.view");
                    if (!(str2 == null || "".equals(str2))) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("calling opPackageName  metaData value is: ");
                        stringBuilder3.append(str2);
                        Slog.d(str3, stringBuilder3.toString());
                        Binder.restoreCallingIdentity(token);
                        return true;
                    }
                }
            } catch (NameNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("cannot find metaData of package: ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception occured: ");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
            return false;
        }
    }

    protected List<Fingerprint> getEnrolledFingerprintsEx(String opPackageName, int targetDevice, int userId) {
        FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
        if (canUseUdFingerprint(opPackageName)) {
            return fingerprintUtils.getFingerprintsForUser(this.mContext, userId, targetDevice);
        }
        if (targetDevice == 1) {
            return Collections.emptyList();
        }
        return fingerprintUtils.getFingerprintsForUser(this.mContext, userId, 0);
    }

    private List<Fingerprint> getEnrolledFingerprintsNoWhitelist(String opPackageName, int targetDevice, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dualFingerprint getEnrolledFingerprints opPackageName is ");
        stringBuilder.append(opPackageName);
        stringBuilder.append(" userId is ");
        stringBuilder.append(userId);
        Slog.d(str, stringBuilder.toString());
        return FingerprintUtils.getInstance().getFingerprintsForUser(this.mContext, userId, targetDevice);
    }

    protected boolean isHardwareDetectedEx(String opPackageName, int targetDevice) {
        boolean z = false;
        if (getFingerprintDaemon() == null) {
            Slog.d(TAG, "Daemon is not available!");
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (canUseUdFingerprint(opPackageName)) {
                if (targetDevice == 1) {
                    if (getUdHalDeviceId() != 0) {
                        z = true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                } else if (targetDevice == 0) {
                    if (getHalDeviceId() != 0) {
                        z = true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                } else {
                    if (!(getHalDeviceId() == 0 || getUdHalDeviceId() == 0)) {
                        z = true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                }
            } else if (targetDevice == 0) {
                if (getHalDeviceId() != 0) {
                    z = true;
                }
                Binder.restoreCallingIdentity(token);
                return z;
            } else {
                Binder.restoreCallingIdentity(token);
                return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isHardwareDetectedNoWhitelist(String opPackageName, int targetDevice) {
        boolean z = false;
        if (getFingerprintDaemon() == null) {
            Slog.d(TAG, "Daemon is not available!");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dualFingerprint isHardwareDetected opPackageName is ");
        stringBuilder.append(opPackageName);
        stringBuilder.append(" targetDevice is ");
        stringBuilder.append(targetDevice);
        Slog.d(str, stringBuilder.toString());
        long token = Binder.clearCallingIdentity();
        if (targetDevice == 1) {
            try {
                if (getUdHalDeviceId() != 0) {
                    z = true;
                }
                Binder.restoreCallingIdentity(token);
                return z;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else if (targetDevice == 0) {
            if (getHalDeviceId() != 0) {
                z = true;
            }
            Binder.restoreCallingIdentity(token);
            return z;
        } else {
            if (!(getHalDeviceId() == 0 || getUdHalDeviceId() == 0)) {
                z = true;
            }
            Binder.restoreCallingIdentity(token);
            return z;
        }
    }

    public int checkNeedCalibrateFingerPrint() {
        int result = -1;
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        Slog.d(TAG, "pacel  packaged :checkNeedCalibrateFingerPrint");
        try {
            result = daemon.checkNeedCalibrateFingerPrint();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkNeedCalibrateFingerPrint RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerprintd calibrate return = ");
        stringBuilder2.append(result);
        Slog.d(str2, stringBuilder2.toString());
        return result;
    }

    public void setCalibrateMode(int mode) {
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pacel  packaged setCalibrateMode: ");
            stringBuilder.append(mode);
            Slog.d(str, stringBuilder.toString());
            try {
                daemon.setCalibrateMode(mode);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setCalibrateMode RemoteException:");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
            }
            return;
        }
        Log.w(TAG, "FingerprintD is not Ready");
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getTokenLen RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerprintd getTokenLen token len = ");
        stringBuilder2.append(result);
        Slog.d(str2, stringBuilder2.toString());
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("callingApp = ");
                stringBuilder.append(callingApp);
                Log.d(str, stringBuilder.toString());
                str = bundle.getString("PackageName");
                this.mMaskViewBundle = bundle;
                if (this.mFingerViewController != null) {
                    if (!"com.android.systemui".equals(str)) {
                        this.mFingerViewController.updateMaskViewAttributes(bundle, callingApp);
                    } else if (!this.mForbideKeyguardCall) {
                        this.mFingerViewController.parseBundle4Keyguard(bundle);
                    }
                }
            }
        }
    }

    public void notifyAuthenticationStarted(String pkgName, IFingerprintServiceReceiver receiver, int flag, int userID, Bundle bundle, IBiometricPromptReceiver dialogReceiver) {
        String str = pkgName;
        int i = flag;
        int i2 = userID;
        Bundle bundle2 = bundle;
        initPositionAndType();
        if (!this.mIsFingerInScreenSupported) {
            return;
        }
        if (str == null) {
            Log.d(TAG, "pkgname is null");
            return;
        }
        checkPermissions();
        String str2;
        StringBuilder stringBuilder;
        if (i == 0 || (FLAG_USE_UD_FINGERPRINT & i) != 0) {
            String str3;
            StringBuilder stringBuilder2;
            boolean hasUdFingerprint;
            boolean hasUdFingerprint2;
            if ("com.android.systemui".equals(str)) {
                notifyAuthenticationCanceled(pkgName);
            }
            if (bundle2 != null) {
                this.mMaskViewBundle = bundle2;
                this.mMaskViewBundle.putString("googleFlag", "googleFlag");
            }
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("show,pkgName =");
            stringBuilder.append(str);
            stringBuilder.append(" userID = ");
            stringBuilder.append(i2);
            Log.d(str2, stringBuilder.toString());
            int initType = -1;
            if (str.equals(this.mDefinedAppName)) {
                initType = this.mAppDefinedMaskType;
                String str4 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("initType = ");
                stringBuilder3.append(initType);
                stringBuilder3.append(",defined by enable interface");
                Log.d(str4, stringBuilder3.toString());
            }
            if (initType == -1) {
                initType = getAppType(pkgName);
            }
            if (initType == -1) {
                Iterator it = this.mWhitelist.iterator();
                while (it.hasNext()) {
                    if (((String) it.next()).equals(str)) {
                        Log.d(TAG, "pkgName in whitelist, show default mask for it");
                        initType = 0;
                    }
                }
            }
            if (str.equals("com.android.cts.verifier")) {
                initType = 0;
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("com.android.cts.verifier initType = ");
                stringBuilder2.append(0);
                Log.d(str3, stringBuilder2.toString());
            }
            FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
            if (!fingerprintUtils.isDualFp() && initType == -1) {
                Log.i(TAG, "single inscreen");
                initType = 0;
            } else if (fingerprintUtils.isDualFp() && bundle2 != null) {
                initType = 4;
            }
            if (!fingerprintUtils.isDualFp()) {
                initType = adjustMaskTypeForWechat(initType, str);
            }
            int initType2 = initType;
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("final initType = ");
            stringBuilder.append(initType2);
            Log.i(str2, stringBuilder.toString());
            boolean hasBackFingerprint = false;
            boolean z = false;
            if (fingerprintUtils.isDualFp()) {
                hasUdFingerprint = fingerprintUtils.getFingerprintsForUser(this.mContext, i2, 1).size() > 0;
                if (fingerprintUtils.getFingerprintsForUser(this.mContext, i2, 0).size() > 0) {
                    z = true;
                }
                hasBackFingerprint = z;
                if (!hasUdFingerprint) {
                    str3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("userID:");
                    stringBuilder2.append(i2);
                    stringBuilder2.append("has no UD_fingerprint");
                    Log.d(str3, stringBuilder2.toString());
                    if (!(initType2 == 3 || initType2 == 4)) {
                        return;
                    }
                }
            }
            if (fingerprintUtils.getFingerprintsForUser(this.mContext, i2).size() > 0) {
                z = true;
            }
            hasUdFingerprint = z;
            boolean hasUdFingerprint3 = hasUdFingerprint;
            boolean hasBackFingerprint2 = hasBackFingerprint;
            if (!this.mIsUdFingerprintNeed || checkIfCallerDisableMask()) {
                hasUdFingerprint2 = hasUdFingerprint3;
            } else {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("dialogReceiver = ");
                stringBuilder.append(dialogReceiver);
                Log.d(str2, stringBuilder.toString());
                hasUdFingerprint2 = hasUdFingerprint3;
                this.mFingerViewController.showMaskOrButton(str, this.mMaskViewBundle, receiver, initType2, hasUdFingerprint3, hasBackFingerprint2, dialogReceiver);
            }
            Log.d(TAG, " begin add windowservice view");
            this.mIsUdAuthenticating = true;
            updateTPState(str, hasUdFingerprint2);
            return;
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("flag = ");
        stringBuilder.append(i);
        Log.d(str2, stringBuilder.toString());
    }

    private int adjustMaskTypeForWechat(int initType, String pkgName) {
        if (!PKGNAME_OF_WECHAT.equals(pkgName)) {
            return initType;
        }
        if (ACTIVITYNAME_OF_WECHAT_ENROLL.equals(getForegroundActivityName())) {
            return 0;
        }
        return 3;
    }

    public void notifyAuthenticationCanceled(String pkgName) {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            this.mMaskViewBundle = null;
            if ("com.android.systemui".equals(pkgName)) {
                Log.d(TAG, " KEYGUARD notifyAuthenticationCanceled removeHighlightview");
                this.mFingerViewController.removeHighlightview(4);
            }
            if (!(this.mKeepMaskAfterAuthentication || this.mFingerViewController == null)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mKeepMaskAfterAuthentication is false,start remove,pkgName = ");
                stringBuilder.append(pkgName);
                Log.d(str, stringBuilder.toString());
                this.mFingerViewController.removeMaskOrButton();
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeFingerprintView,callingApp = ");
            stringBuilder.append(callingApp);
            Log.d(str, stringBuilder.toString());
            checkPermissions();
            this.mMaskViewBundle = null;
            this.mFingerViewController.removeMaskOrButton();
        }
    }

    public void notifyFingerDown(int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyFingerDown mIsFingerInScreenSupported:");
        stringBuilder.append(this.mIsFingerInScreenSupported);
        Log.d(str, stringBuilder.toString());
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            this.mIsUdFingerprintChecking = true;
            if (this.mIsHighLightNeed) {
                if (type == 1) {
                    Log.d(TAG, "FINGER_DOWN_TYPE_AUTHENTICATING notifyFingerDown ");
                    long token = Binder.clearCallingIdentity();
                    try {
                        if (System.getIntForUser(this.mContext.getContentResolver(), "show_touches", -2) == 1) {
                            Log.d(TAG, "turn off the show_touch switch when authenticating");
                            this.mNeedResumeTouchSwitch = true;
                            System.putIntForUser(this.mContext.getContentResolver(), "show_touches", 0, -2);
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "catch SecurityException");
                    } catch (SettingNotFoundException e2) {
                        Log.d(TAG, "settings show_touches not found");
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(token);
                    }
                    Binder.restoreCallingIdentity(token);
                    this.mFingerViewController.showHighlightview(1);
                } else if (type == 0) {
                    Log.d(TAG, " AUTHENTICATING_SETTINGS or FINGER_DOWN_TYPE_ENROLLING notifyFingerDown ");
                    this.mFingerViewController.showHighlightCircle();
                }
            }
            if (this.mIsUdFingerprintNeed && type == 1) {
                this.mFingerViewController.updateFingerprintView(3, this.mKeepMaskAfterAuthentication);
            }
        }
    }

    public void notifyEnrollingFingerUp() {
        if (this.mIsFingerInScreenSupported && this.mIsUdEnrolling && this.mIsHighLightNeed) {
            Log.d(TAG, "notifyEnrollingFingerUp removeHighlightCircle");
            this.mFingerViewController.removeHighlightCircle();
        }
    }

    public void notifyCaptureFinished(int type) {
        Log.d(TAG, "notifyCaptureFinished");
        if (this.mIsHighLightNeed && this.mFingerViewController != null) {
            this.mFingerViewController.removeHighlightCircle();
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
                    if (this.mDisplayEngineManager != null) {
                        this.mDisplayEngineManager.setScene(31, 16);
                    } else {
                        Log.w(TAG, "mDisplayEngineManager is null!");
                    }
                }
                if (this.mFingerViewController != null) {
                    this.mFingerViewController.showHighlightview(0);
                }
                Log.d(TAG, "start enroll begin add Highlight view");
                return;
            }
            Log.d(TAG, "not enrolling UD fingerprint");
        }
    }

    public void notifyEnrollmentCanceled() {
        if (this.mIsFingerInScreenSupported) {
            FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
            int currentUser = ActivityManager.getCurrentUser();
            boolean hasUdFingerprint = fingerprintUtils.isDualFp() ? fingerprintUtils.getFingerprintsForUser(this.mContext, currentUser, 1).size() > 0 : fingerprintUtils.getFingerprintsForUser(this.mContext, currentUser).size() > 0;
            if (hasUdFingerprint) {
                sendCommandToHal(1);
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyEnrollmentEnd mIsUdEnrolling: ");
        stringBuilder.append(this.mIsUdEnrolling);
        Log.d(str, stringBuilder.toString());
        if (this.mIsHighLightNeed) {
            checkPermissions();
            if (this.mIsUdEnrolling && this.mFingerViewController != null) {
                this.mFingerViewController.removeHighlightview(-1);
            }
            if (this.mIsUdEnrolling) {
                if (this.mDisplayEngineManager != null) {
                    this.mDisplayEngineManager.setScene(31, 17);
                } else {
                    Log.w(TAG, "mDisplayEngineManager is null!");
                }
            }
            this.mIsUdEnrolling = false;
        }
    }

    public void notifyAuthenticationFinished(String opName, int result, int failTimes) {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyAuthenticationFinished,mIsUdFingerprintChecking = ");
            stringBuilder.append(this.mIsUdFingerprintChecking);
            stringBuilder.append(",result =");
            stringBuilder.append(result);
            stringBuilder.append("failTimes = ");
            stringBuilder.append(failTimes);
            Log.d(str, stringBuilder.toString());
            if (this.mIsHighLightNeed && this.mIsUdFingerprintChecking) {
                int i;
                Log.d(TAG, "UdFingerprint Checking,AuthenticationFinished begin remove Highlight view");
                if (this.mNeedResumeTouchSwitch) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        this.mNeedResumeTouchSwitch = false;
                        Log.d(TAG, "turn on the show_touch switch after authenticating");
                        System.putIntForUser(this.mContext.getContentResolver(), "show_touches", 1, -2);
                    } catch (SecurityException e) {
                        Log.e(TAG, "catch SecurityException");
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(token);
                    }
                    Binder.restoreCallingIdentity(token);
                }
                FingerViewController fingerViewController = this.mFingerViewController;
                if (result == 0) {
                    i = 2;
                } else {
                    i = -1;
                }
                fingerViewController.removeHighlightview(i);
            }
            if (this.mIsUdFingerprintNeed) {
                this.mFingerViewController.updateFingerprintView(result, failTimes);
                if (result == 0 && "com.android.systemui".equals(opName)) {
                    this.mFingerViewController.removeMaskOrButton();
                }
            }
            this.mIsUdFingerprintChecking = false;
        }
    }

    private void disableFingerprintView(boolean hasAnimation) {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("callingApp = ");
            stringBuilder.append(callingApp);
            Log.d(str, stringBuilder.toString());
            if (this.mIsUdAuthenticating && callingApp.equals(getForegroundActivity())) {
                this.mFingerViewController.removeMaskOrButton();
            }
            this.mPackageDisableMask = callingApp;
        }
    }

    private void enableFingerprintView(boolean hasAnimation, int initStatus) {
        if (this.mIsFingerInScreenSupported) {
            checkPermissions();
            String callingApp = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableFingerprintView,callingApp = ");
            stringBuilder.append(callingApp);
            Log.d(str, stringBuilder.toString());
            if (this.mPackageDisableMask != null && this.mPackageDisableMask.equals(callingApp)) {
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pkgName is ");
                stringBuilder.append(pkgName);
                stringBuilder.append("metaData is ");
                stringBuilder.append(metaData.getString("fingerprint.system.view"));
                Log.d(str, stringBuilder.toString());
                if (metaData.getString("fingerprint.system.view") != null) {
                    if (metaData.getString("fingerprint.system.view").equals("full")) {
                        type = 0;
                    } else if (metaData.getString("fingerprint.system.view").equals("button")) {
                        type = 1;
                    } else if (metaData.getString("fingerprint.system.view").equals("image")) {
                        type = 3;
                    } else {
                        type = 2;
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("metaData type is ");
                stringBuilder.append(type);
                Log.d(str, stringBuilder.toString());
            }
        } catch (NameNotFoundException e) {
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

    private boolean isForegroundActivity(String packageName) {
        try {
            List<RunningAppProcessInfo> procs = ActivityManager.getService().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                RunningAppProcessInfo proc = (RunningAppProcessInfo) procs.get(i);
                if (proc.processName.equals(packageName) && proc.importance == 100) {
                    return true;
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
        }
        return false;
    }

    private String getForegroundActivity() {
        try {
            List<RunningAppProcessInfo> procs = ActivityManager.getService().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                RunningAppProcessInfo proc = (RunningAppProcessInfo) procs.get(i);
                if (proc.importance == 100) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("foreground processName = ");
                    stringBuilder.append(proc.processName);
                    Log.d(str, stringBuilder.toString());
                    return proc.processName;
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
        }
        return "";
    }

    private String getForegroundActivityName() {
        long token = Binder.clearCallingIdentity();
        try {
            ActivityInfo info = ActivityManagerEx.getLastResumedActivity();
            String name = "";
            if (info != null) {
                name = info.name;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("foreground wechat activity name is");
            stringBuilder.append(name);
            Log.d(str, stringBuilder.toString());
            return name;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean checkIfCallerDisableMask() {
        if (this.mPackageDisableMask == null) {
            return false;
        }
        if (isForegroundActivity(this.mPackageDisableMask)) {
            return true;
        }
        this.mPackageDisableMask = null;
        return false;
    }

    private void checkPermissions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.USE_FINGERPRINT", null);
    }

    private void resetFingerprintPosition() {
        int[] position = getFingerprintHardwarePosition();
        this.mFingerViewController.setFingerprintPosition(position);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("defaultScreenSize be init second, position = ");
        stringBuilder.append(Arrays.toString(position));
        Log.d(str, stringBuilder.toString());
    }

    private void initPositionAndType() {
        String str;
        StringBuilder stringBuilder;
        if (this.mFingerprintType == 0) {
            Log.d(TAG, "FingerprintType do not support inscreen fingerprint");
        } else if (this.mFingerprintType <= 0 || this.mFingerViewController == null) {
            this.mFingerprintType = getFingerprintHardwareTypeInternal();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("FingerprintType type = ");
            stringBuilder.append(this.mFingerprintType);
            Log.d(str, stringBuilder.toString());
            boolean z = false;
            this.mIsFingerInScreenSupported = this.mFingerprintType > 0;
            if (this.mFingerprintType == 1) {
                z = true;
            }
            this.mIsHighLightNeed = z;
            if (this.mIsFingerInScreenSupported) {
                this.mFingerViewController = FingerViewController.getInstance(this.mContext);
                this.mFingerViewController.registCallback(new FingerViewChangeCallback(this, null));
                int[] position = getFingerprintHardwarePosition();
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" hardware type = ");
                stringBuilder2.append(this.mFingerprintType);
                Log.d(str2, stringBuilder2.toString());
                this.mFingerViewController.setFingerprintPosition(position);
                this.mFingerViewController.setHighLightBrightnessLevel(getHighLightBrightnessLevel());
                this.mFingerViewController.setHighLightSpotRadius(getHighLightspotRadius());
                initObserver();
                listenCfgChange();
            }
        } else {
            if (this.mInitDisplayHeight == -1) {
                resetFingerprintPosition();
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("FingerprintType has been inited, FingerprintType = ");
            stringBuilder.append(this.mFingerprintType);
            Log.d(str, stringBuilder.toString());
        }
    }

    protected void triggerFaceRecognization() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager == null || !powerManager.isInteractive()) {
            HwPhoneWindowManager policy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
            if (policy != null) {
                policy.doFaceRecognize(true, "fingerprint");
            }
            return;
        }
        Log.i(TAG, "screen on, do not trigger face detection");
    }

    private void suspendAuthentication(int status) {
        if (!isFingerprintDReady()) {
            Log.w(TAG, "FingerprintD is not Ready");
        } else if (this.mIsFingerInScreenSupported) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pacel  packaged suspendAuthentication: ");
            stringBuilder.append(status);
            Slog.d(str, stringBuilder.toString());
            if (status == 1) {
                try {
                    daemon.sendCmdToHal(53);
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("suspendAuthentication RemoteException:");
                    stringBuilder2.append(e);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } else {
                daemon.sendCmdToHal(MSG_RESUME_AUTHENTICATION);
            }
        } else {
            Log.w(TAG, "do not have UD device suspend invalid");
        }
    }

    private int suspendEnroll(int status) {
        int result = -1;
        if (!isFingerprintDReady()) {
            Log.w(TAG, "FingerprintD is not Ready");
            return -1;
        } else if (this.mIsFingerInScreenSupported) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return -1;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pacel  packaged suspendEnroll: ");
            stringBuilder.append(status);
            Slog.d(str, stringBuilder.toString());
            if (status == 1) {
                try {
                    result = daemon.sendCmdToHal(64);
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("suspendEnroll RemoteException:");
                    stringBuilder2.append(e);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } else {
                result = daemon.sendCmdToHal(MSG_RESUME_ENROLLMENT);
            }
            return result;
        } else {
            Log.w(TAG, "do not have UD device suspend invalid");
            return -1;
        }
    }

    private int getFingerprintHardwareType() {
        if (!isFingerprintDReady()) {
            return -1;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return -1;
        }
        int result = -1;
        Slog.d(TAG, "pacel  packaged :HardwareType");
        try {
            result = daemon.sendCmdToHal(55);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HardwareType RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerprintd HardwareType = ");
        stringBuilder2.append(result);
        Slog.d(str2, stringBuilder2.toString());
        return result;
    }

    private int getFingerprintHardwareTypeInternal() {
        int typeDetails = getFingerprintHardwareType();
        if (typeDetails == -1) {
            return -1;
        }
        int offset = -1;
        if ((typeDetails & 1) != 0) {
            offset = -1 + 1;
        }
        if ((typeDetails & 2) != 0) {
            offset++;
        }
        if ((typeDetails & 4) == 0) {
            return 0;
        }
        int physicalType = (typeDetails >> (8 + ((offset + 1) * 4))) & 15;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LOCATION_UNDER_DISPLAY physicalType :");
        stringBuilder.append(physicalType);
        Log.d(str, stringBuilder.toString());
        if (physicalType == 2) {
            return 1;
        }
        if (physicalType == 3) {
            return 2;
        }
        return -1;
    }

    private int[] getFingerprintHardwarePosition() {
        int[] result = new int[]{-1, -1};
        int[] pxPosition = new int[]{-1, -1, -1, -1};
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HardwarePosition RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerprintd HardwarePosition = ");
        stringBuilder2.append(result[0]);
        stringBuilder2.append(" ");
        stringBuilder2.append(result[1]);
        Slog.d(str2, stringBuilder2.toString());
        if (result[0] == -1) {
            String[] positionG = SystemProperties.get("persist.sys.fingerprint.hardwarePosition", "-1,-1,-1,-1").split(",");
            pxPosition[0] = Integer.parseInt(positionG[0]);
            pxPosition[1] = Integer.parseInt(positionG[1]);
            pxPosition[2] = Integer.parseInt(positionG[2]);
            pxPosition[3] = Integer.parseInt(positionG[3]);
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getHardwarePosition from SystemProperties: ");
            stringBuilder3.append(pxPosition[0]);
            Log.d(str3, stringBuilder3.toString());
        } else {
            int[] parsedPosition = new int[]{-1, -1, -1, -1};
            parsedPosition[0] = result[0] >> 16;
            parsedPosition[1] = result[0] & FLAG_FINGERPRINT_POSITION_MASK;
            parsedPosition[2] = result[1] >> 16;
            parsedPosition[3] = result[1] & FLAG_FINGERPRINT_POSITION_MASK;
            pxPosition = physicalConvert2Px(parsedPosition);
        }
        return pxPosition;
    }

    private int[] physicalConvert2Px(int[] input) {
        String defaultScreenSize;
        int[] covertPosition = new int[]{-1, -1, -1, -1};
        int i = 0;
        if (this.mInitDisplayHeight == -1 && this.mContext != null) {
            defaultScreenSize = SystemProperties.get("ro.config.default_screensize");
            if (defaultScreenSize == null || defaultScreenSize.equals("")) {
                this.mInitDisplayHeight = Global.getInt(this.mContext.getContentResolver(), APS_INIT_HEIGHT, -1);
                this.mInitDisplayWidth = Global.getInt(this.mContext.getContentResolver(), APS_INIT_WIDTH, -1);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("defaultScreenSizePoint mInitDisplayWidth =");
                stringBuilder.append(this.mInitDisplayWidth);
                stringBuilder.append(",mInitDisplayHeight=");
                stringBuilder.append(this.mInitDisplayHeight);
                Log.i(str, stringBuilder.toString());
            } else {
                String[] array = defaultScreenSize.split(",");
                String str2;
                StringBuilder stringBuilder2;
                if (array.length == 2) {
                    try {
                        this.mInitDisplayWidth = Integer.parseInt(array[0]);
                        this.mInitDisplayHeight = Integer.parseInt(array[1]);
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("defaultScreenSizePoint get from prop : mInitDisplayWidth=");
                        stringBuilder2.append(this.mInitDisplayWidth);
                        stringBuilder2.append(",mInitDisplayHeight=");
                        stringBuilder2.append(this.mInitDisplayHeight);
                        Log.i(str2, stringBuilder2.toString());
                    } catch (NumberFormatException e) {
                        Log.i(TAG, "defaultScreenSizePoint: NumberFormatException");
                    }
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("defaultScreenSizePoint the defaultScreenSize prop is error,defaultScreenSize=");
                    stringBuilder2.append(defaultScreenSize);
                    Log.i(str2, stringBuilder2.toString());
                }
            }
        }
        covertPosition[0] = (input[0] * this.mInitDisplayWidth) / 1000;
        covertPosition[1] = (input[1] * this.mInitDisplayHeight) / 1000;
        covertPosition[2] = (input[2] * this.mInitDisplayWidth) / 1000;
        covertPosition[3] = (input[3] * this.mInitDisplayHeight) / 1000;
        defaultScreenSize = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Width: ");
        stringBuilder3.append(this.mInitDisplayWidth);
        stringBuilder3.append(" height: ");
        stringBuilder3.append(this.mInitDisplayHeight);
        Log.d(defaultScreenSize, stringBuilder3.toString());
        while (true) {
            int i2 = i;
            if (i2 >= 4) {
                return covertPosition;
            }
            String str3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("use hal after covert: ");
            stringBuilder4.append(covertPosition[i2]);
            Log.d(str3, stringBuilder4.toString());
            i = i2 + 1;
        }
    }

    private void notifyCaptureOpticalImage() {
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            Slog.d(TAG, "pacel  packaged :notifyCaptureOpticalImage");
            try {
                daemon.sendCmdToHal(52);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyCaptureOpticalImage RemoteException:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    private void notifyBluespotDismiss() {
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            Slog.d(TAG, "pacel  packaged :notifyBluespotDismiss");
            try {
                daemon.sendCmdToHal(62);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyBluespotDismiss RemoteException:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    private void setHoverEventSwitch(int enabled) {
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pacel  packaged setHoverEventSwitch: ");
            stringBuilder.append(enabled);
            Slog.d(str, stringBuilder.toString());
            if (enabled == 1) {
                try {
                    daemon.sendCmdToHal(MSG_SET_HOVER_ENABLE);
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setHoverEventSwitch RemoteException:");
                    stringBuilder2.append(e);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } else {
                daemon.sendCmdToHal(MSG_SET_HOVER_DISABLE);
            }
            return;
        }
        Log.w(TAG, "FingerprintD is not Ready");
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
        Slog.d(TAG, "pacel  packaged :getHoverEventSupport");
        try {
            result = daemon.sendCmdToHal(MSG_CHECK_HOVER_SUPPORT);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getHoverEventSupport RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerprintd getHoverEventSupport = ");
        stringBuilder2.append(result);
        Slog.d(str2, stringBuilder2.toString());
        return result;
    }

    private int getHighLightBrightnessLevel() {
        int result = DEFAULT_CAPTURE_BRIGHTNESS;
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return DEFAULT_CAPTURE_BRIGHTNESS;
            }
            Slog.d(TAG, "pacel  packaged :getHighLightBrightnessLevel");
            try {
                result = daemon.sendCmdToHal(MSG_GET_BRIGHTNEWSS_FROM_HAL);
            } catch (RemoteException e) {
                Slog.e(TAG, "getHighLightBrightnessLevel RemoteException");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fingerprintd getHighLightBrightnessLevel = ");
            stringBuilder.append(result);
            Slog.d(str, stringBuilder.toString());
            return result;
        }
        Slog.e(TAG, "Fingerprintd is not ready!");
        return DEFAULT_CAPTURE_BRIGHTNESS;
    }

    private int checkForegroundNeedLiveness() {
        Slog.w(TAG, "checkForegroundNeedLiveness:start");
        int noNeedLiveness = 0;
        try {
            List<RunningAppProcessInfo> procs = ActivityManagerNative.getDefault().getRunningAppProcesses();
            if (procs == null) {
                return 0;
            }
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                RunningAppProcessInfo proc = (RunningAppProcessInfo) procs.get(i);
                if (proc.importance == 100) {
                    String str;
                    StringBuilder stringBuilder;
                    if ("com.alipay.security.mobile.authentication.huawei".equals(proc.processName)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ForegroundActivity is ");
                        stringBuilder.append(proc.processName);
                        stringBuilder.append("need liveness auth");
                        Slog.w(str, stringBuilder.toString());
                        return 1;
                    } else if ("com.huawei.wallet".equals(proc.processName)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ForegroundActivity is ");
                        stringBuilder.append(proc.processName);
                        stringBuilder.append("need liveness auth");
                        Slog.w(str, stringBuilder.toString());
                        return 1;
                    } else if ("com.huawei.android.hwpay".equals(proc.processName)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ForegroundActivity is ");
                        stringBuilder.append(proc.processName);
                        stringBuilder.append("need liveness auth");
                        Slog.w(str, stringBuilder.toString());
                        return 1;
                    }
                }
            }
            return 0;
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed in checkForegroundNeedLiveness");
        }
    }

    private int checkNeedLivenessList(String opPackageName) {
        Slog.w(TAG, "checkNeedLivenessList:start");
        if (opPackageName == null || opPackageName.equals("com.android.keyguard")) {
            return 0;
        }
        if (opPackageName.equals("com.huawei.securitymgr")) {
            return checkForegroundNeedLiveness();
        }
        if (opPackageName.equals("com.eg.android.AlipayGphone") || opPackageName.equals("fido") || opPackageName.equals("com.alipay.security.mobile.authentication.huawei") || opPackageName.equals("com.huawei.wallet") || opPackageName.equals("com.huawei.android.hwpay")) {
            return 1;
        }
        return 0;
    }

    protected void setLivenessSwitch(String opPackageName) {
        Slog.w(TAG, "setLivenessSwitch:start");
        if ((!mLivenessNeedBetaQualification || isBetaUser()) && isFingerprintDReady()) {
            int NEED_LIVENESS_AUTHENTICATION = checkNeedLivenessList(opPackageName);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NEED_LIVENESS_AUTHENTICATION = ");
            stringBuilder.append(NEED_LIVENESS_AUTHENTICATION);
            Slog.w(str, stringBuilder.toString());
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            try {
                daemon.setLivenessSwitch(NEED_LIVENESS_AUTHENTICATION);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setLivenessSwitch RemoteException:");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
            }
            Slog.w(TAG, "framework setLivenessSwitch is ok ---end");
        }
    }

    private boolean checkPackageName(String opPackageName) {
        if (opPackageName == null || !opPackageName.equals("com.android.systemui")) {
            return false;
        }
        return true;
    }

    public boolean shouldAuthBothSpaceFingerprints(String opPackageName, int flags) {
        if (!checkPackageName(opPackageName) || (33554432 & flags) == 0) {
            return false;
        }
        return true;
    }

    private HwFingerprintSets remoteGetOldData() {
        Slog.i(TAG, "remoteGetOldData:start");
        if (!isFingerprintDReady()) {
            return null;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return null;
        }
        HwFingerprintSets _result;
        ArrayList<Integer> fingerprintInfo = new ArrayList();
        try {
            fingerprintInfo = daemon.getFpOldData();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remoteGetOldData RemoteException:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        Parcel _reply = Parcel.obtain();
        int fingerprintInfoLen = fingerprintInfo.size();
        for (int i = 0; i < fingerprintInfoLen; i++) {
            int intValue = ((Integer) fingerprintInfo.get(i)).intValue();
            if (intValue != -1) {
                _reply.writeInt(intValue);
            }
        }
        _reply.setDataPosition(0);
        if (_reply.readInt() != 0) {
            _result = (HwFingerprintSets) HwFingerprintSets.CREATOR.createFromParcel(_reply);
        } else {
            _result = null;
        }
        _reply.recycle();
        return _result;
    }

    private static boolean checkItemExist(int oldFpId, ArrayList<Fingerprint> fingerprints) {
        int size = fingerprints.size();
        for (int i = 0; i < size; i++) {
            if (((Fingerprint) fingerprints.get(i)).getFingerId() == oldFpId) {
                fingerprints.remove(i);
                return true;
            }
        }
        return false;
    }

    private boolean isFingerprintDReady() {
        if (getFingerprintDaemon() != null) {
            return true;
        }
        Slog.w(TAG, "isFingerprintDReady: no fingeprintd!");
        return false;
    }

    protected void handleAuthenticated(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
        if (fingerId != 0) {
            AwareFakeActivityRecg.self().setFingerprintWakeup(true);
        }
        super.handleAuthenticated(deviceId, fingerId, groupId, token);
    }

    protected void stopPickupTrunOff() {
        if (PickUpWakeScreenManager.isPickupSensorSupport(this.mContext) && PickUpWakeScreenManager.getInstance() != null) {
            PickUpWakeScreenManager.getInstance().stopTrunOffScrren();
        }
    }

    public void setPowerState(int powerState) {
        HwAodManager.getInstance().setPowerState(powerState);
    }

    private int sendUnlockAndLightbright(int unlockType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendUnlockAndLightbright unlockType:");
        stringBuilder.append(unlockType);
        Slog.d(str, stringBuilder.toString());
        int result = -1;
        if (2 == unlockType) {
            result = sendCommandToHal(201);
        } else if (1 == unlockType) {
            result = sendCommandToHal(200);
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sendCommandToHal result:");
        stringBuilder2.append(result);
        Slog.d(str2, stringBuilder2.toString());
        return result;
    }

    private int getHighLightspotRadius() {
        Slog.d(TAG, "getHighLightspotRadius start");
        int radius = 125;
        if (isFingerprintDReady()) {
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return 125;
            }
            try {
                radius = daemon.sendCmdToHal(MSG_GET_RADIUS_FROM_HAL);
            } catch (RemoteException e) {
                Slog.e(TAG, "getHighLightspotRadius RemoteException");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fingerprintd getHighLightspotRadius = ");
            stringBuilder.append(radius);
            Slog.d(str, stringBuilder.toString());
            return radius;
        }
        Slog.e(TAG, "Fingerprintd is not ready!");
        return 125;
    }

    private void notifyCfgSettingsIfNeed(boolean isBooting) {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean z = false;
        if (Secure.getIntForUser(resolver, FACE_KEYGUARD_WITH_LOCK, 0, 0) == 1 || Secure.getIntForUser(resolver, FACE_RECOGNIZE_UNLOCK, 0, 0) == 1 || Secure.getIntForUser(resolver, FACE_RECOGNIZE_SLIDE_UNLOCK, 0, 0) == 1) {
            z = true;
        }
        boolean isFaceDetectEnabled = z;
        int cmd = isFaceDetectEnabled ? 211 : 210;
        if (isBooting || isFaceDetectEnabled != this.mIsFaceDetectEnabled) {
            sendCommandToHal(cmd);
            this.mIsFaceDetectEnabled = isFaceDetectEnabled;
        }
    }

    private void listenCfgChange() {
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Secure.getUriFor(FACE_KEYGUARD_WITH_LOCK), false, this.mSettingsObserver, 0);
        resolver.registerContentObserver(Secure.getUriFor(FACE_RECOGNIZE_SLIDE_UNLOCK), false, this.mSettingsObserver, 0);
        resolver.registerContentObserver(Secure.getUriFor(FACE_RECOGNIZE_UNLOCK), false, this.mSettingsObserver, 0);
    }
}
