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
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import com.android.server.fingerprint.HwFingerprintSets.HwFingerprintGroup;
import com.android.server.policy.PickUpWakeScreenManager;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.huawei.android.content.pm.UserInfoEx;
import com.huawei.android.os.UserManagerEx;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.fingerprint.IAuthenticator;
import com.huawei.fingerprint.IAuthenticator.Stub;
import com.huawei.fingerprint.IAuthenticatorListener;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.com.android.server.fingerprint.FingerViewController.ICallBack;
import huawei.com.android.server.fingerprint.FingerprintCalibrarionView;
import huawei.com.android.server.policy.HwGlobalActionsData;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import vendor.huawei.hardware.biometrics.fingerprint.V2_1.IExtBiometricsFingerprint;
import vendor.huawei.hardware.biometrics.fingerprint.V2_1.IFidoAuthenticationCallback;

public class HwFingerprintService extends FingerprintService {
    private static final String APS_INIT_HEIGHT = "aps_init_height";
    private static final String APS_INIT_WIDTH = "aps_init_width";
    public static final int CHECK_NEED_REENROLL_FINGER = 1003;
    private static final int CODE_DISABLE_FINGERPRINT_VIEW_RULE = 1114;
    private static final int CODE_ENABLE_FINGERPRINT_VIEW_RULE = 1115;
    private static final int CODE_GET_FINGERPRINT_LIST_ENROLLED = 1118;
    private static final int CODE_GET_HARDWARE_POSITION = 1110;
    private static final int CODE_GET_HARDWARE_TYPE = 1109;
    private static final int CODE_GET_HOVER_SUPPORT = 1113;
    private static final int CODE_GET_TOKEN_LEN_RULE = 1103;
    private static final int CODE_IS_FINGERPRINT_HARDWARE_DETECTED = 1119;
    private static final int CODE_IS_FP_NEED_CALIBRATE_RULE = 1101;
    private static final int CODE_IS_SUPPORT_DUAL_FINGERPRINT = 1120;
    private static final int CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION_RULE = 1116;
    private static final int CODE_NOTIFY_OPTICAL_CAPTURE = 1111;
    private static final int CODE_REMOVE_FINGERPRINT_RULE = 1107;
    private static final int CODE_REMOVE_MASK_AND_SHOW_BUTTON_RULE = 1117;
    private static final int CODE_SET_CALIBRATE_MODE_RULE = 1102;
    private static final int CODE_SET_FINGERPRINT_MASK_VIEW_RULE = 1104;
    private static final int CODE_SET_HOVER_SWITCH = 1112;
    private static final int CODE_SHOW_FINGERPRINT_BUTTON_RULE = 1106;
    private static final int CODE_SHOW_FINGERPRINT_VIEW_RULE = 1105;
    private static final int CODE_SUSPEND_AUTHENTICATE = 1108;
    private static final int DENSITY_DEFUALT_HEIGHT = 2880;
    private static final int DENSITY_DEFUALT_WIDTH = 1440;
    private static final String DESCRIPTOR_FINGERPRINT_SERVICE = "android.hardware.fingerprint.IFingerprintService";
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
    private static final String LABEL_NAVI_SWITCH_REMINDER = "navi_switch_reminder";
    public static final int MASK_TYPE_BUTTON = 1;
    public static final int MASK_TYPE_FULL = 0;
    public static final int MASK_TYPE_IMAGE = 3;
    public static final int MASK_TYPE_NONE = 2;
    private static final String METADATA_KEY = "fingerprint.system.view";
    private static final int MSG_CHECK_AND_DEL_TEMPLATES = 84;
    private static final int MSG_CHECK_HOVER_SUPPORT = 56;
    private static final int MSG_CHECK_OLD_TEMPLATES = 85;
    private static final int MSG_CHECK_SWITCH_FREQUENCE_SUPPORT = 61;
    private static final int MSG_DEL_OLD_TEMPLATES = 86;
    private static final int MSG_GET_SENSOR_POSITION_BOTTOM_RIGHT = 60;
    private static final int MSG_GET_SENSOR_POSITION_TOP_LEFT = 59;
    private static final int MSG_GET_SENSOR_TYPE = 55;
    private static final int MSG_OPTICAL_HBM_FOR_CAPTURE_IMAGE = 52;
    private static final int MSG_REDUCING_FREQUENCE = 63;
    private static final int MSG_RESUME_AUTHENTICATION = 54;
    private static final int MSG_SET_HOVER_DISABLE = 58;
    private static final int MSG_SET_HOVER_ENABLE = 57;
    private static final int MSG_SUSPEND_AUTHENTICATION = 53;
    private static final int MSG_TYPE_FINGERPRINT_NAV = 43;
    private static final int MSG_TYPE_VIRTUAL_NAV = 45;
    private static final int MSG_UPGRADING_FREQUENCE = 62;
    private static final int NAVI_SWITCH_REMINDER_DISABLE = 1;
    private static final String PATH_CHILDMODE_STATUS = "childmode_status";
    private static final String PKGNAME_OF_KEYGUARD = "com.android.systemui";
    private static final String PKGNAME_OF_SETTINGS = "com.android.settings";
    private static final long POWER_PUSH_DOWN_TIME_THR = 430;
    private static final int PRIMARY_USER_ID = 0;
    public static final int REMOVE_USER_DATA = 101;
    public static final int RESTORE_AUTHENTICATE = 0;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_CHECKING = 3;
    public static final int SET_LIVENESS_SWITCH = 1002;
    private static final int STATUS_PARENT_CTRL_OFF = 0;
    private static final int STATUS_PARENT_CTRL_STUDENT = 1;
    public static final int SUSPEND_AUTHENTICATE = 1;
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
    private static boolean mNaviSwitchRemider = SystemProperties.getBoolean("ro.config.navi_switch_reminder", false);
    private static boolean mNeedRecreateDialog = false;
    private static boolean mRemoveFingerprintBGE = SystemProperties.getBoolean("ro.config.remove_finger_bge", false);
    private static boolean mRemoveOldTemplatesFeature = SystemProperties.getBoolean("ro.config.remove_old_templates", false);
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
            Log.i(HwFingerprintService.TAG, "fp_keyguard_state onChange: " + HwFingerprintService.this.mState);
        }
    };
    private int mAppDefinedMaskType = -1;
    private OnCheckedChangeListener mCheckedListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                return;
            }
            if (buttonView.getId() == 34603043) {
                HwFingerprintService.this.mTripleVirNaviRadio.setChecked(false);
                HwFingerprintService.this.mSingleVirNaviRadio.setChecked(false);
            } else if (buttonView.getId() == 34603204) {
                HwFingerprintService.this.mFrontPhysicRadio.setChecked(false);
                HwFingerprintService.this.mTripleVirNaviRadio.setChecked(false);
            } else if (buttonView.getId() == 34603233) {
                HwFingerprintService.this.mFrontPhysicRadio.setChecked(false);
                HwFingerprintService.this.mSingleVirNaviRadio.setChecked(false);
            }
        }
    };
    private final Context mContext;
    private String mDefinedAppName = "";
    private DisplayEngineManager mDisplayEngineManager;
    private FingerViewController mFingerViewController;
    private int mFingerprintType = -1;
    private boolean mForbideKeyguardCall = false;
    private RadioButton mFrontPhysicRadio;
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
                Log.d(HwFingerprintService.TAG, "uid =" + uid);
                if (uid != 1000) {
                    Log.e(HwFingerprintService.TAG, "permission denied.");
                    return -1;
                }
                if (((Boolean) HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "canUseFingerprint", new Class[]{String.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}, new Object[]{HwFingerprintService.FIDO_ASM, Boolean.valueOf(true), Integer.valueOf(uid), Integer.valueOf(pid), Integer.valueOf(userid)})).booleanValue()) {
                    final int effectiveGroupId = HwFingerprintService.this.getEffectiveUserId(userid);
                    final int callingUserId = UserHandle.getCallingUserId();
                    final IFingerprintServiceReceiver iFingerprintServiceReceiver = receiver;
                    final IAuthenticatorListener iAuthenticatorListener = listener;
                    final String str = aaid;
                    final byte[] bArr = nonce;
                    HwFingerprintService.this.mHandler.post(new Runnable() {
                        public void run() {
                            HwFingerprintService.this.setLivenessSwitch("fido");
                            HwFingerprintService.this.startAuthentication(iFingerprintServiceReceiver.asBinder(), 0, callingUserId, effectiveGroupId, iFingerprintServiceReceiver, 0, true, HwFingerprintService.FIDO_ASM, iAuthenticatorListener, str, bArr);
                        }
                    });
                    return 0;
                }
                Log.w(HwFingerprintService.TAG, "FIDO_ASM can't use fingerprint");
                return -1;
            }
        }
    };
    private int mInitDisplayHeight = -1;
    private int mInitDisplayWidth = -1;
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
                Log.d(HwFingerprintService.TAG, "Navigation mode changed, mVirNavModeEnabled = " + HwFingerprintService.this.mVirNavModeEnabled);
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.this.mVirNavModeEnabled ? 45 : 43);
            }
        }
    };
    private boolean mNeedResumeTouchSwitch = false;
    private String mPackageDisableMask;
    private AlertDialog mReEnrollDialog;
    private BroadcastReceiver mReceiver;
    private String mScreen;
    private AlertDialog mSetVirNavDialog;
    private RadioButton mSingleVirNaviRadio;
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
    private RadioButton mTripleVirNaviRadio;
    private BroadcastReceiver mUserDeletedMonitor = new BroadcastReceiver() {
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
                        newUserId = HwFingerprintService.HIDDEN_SPACE_ID;
                        newPathId = 0;
                    }
                    File fpDir = new File(Environment.getUserSystemDirectory(newPathId), FP_DATA_DIR);
                    if (fpDir.exists()) {
                        HwFingerprintService.this.removeUserData(newUserId, fpDir.getAbsolutePath());
                    } else {
                        Slog.v(HwFingerprintService.TAG, "no fpdata!");
                    }
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    boolean hasShowDialogOnce = false;
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
                                hasShowDialogOnce = true;
                                HwFingerprintService.this.updateActiveGroupEx(HwFingerprintService.HIDDEN_SPACE_ID);
                                HwFingerprintService.this.updateActiveGroupEx(0);
                                HwFingerprintService.this.showDialog(false);
                            } else if (!HwFingerprintService.mIsChinaArea && checkValReEnroll == 3) {
                                hasShowDialogOnce = true;
                                HwFingerprintService.this.showDialog(true);
                            }
                        } else if (checkValReEnroll == 1 && checkValCalibrate != 1) {
                            HwFingerprintService.this.intentOthers(context);
                        }
                        HwFingerprintService.mCheckNeedEnroll = false;
                        if (HwFingerprintService.mNaviSwitchRemider) {
                            boolean isNaviBarEnabled = FrontFingerPrintSettings.isNaviBarEnabled(HwFingerprintService.this.mContext.getContentResolver());
                            if (hasShowDialogOnce || isNaviBarEnabled) {
                                HwFingerprintService.this.putSystemSettings(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.LABEL_NAVI_SWITCH_REMINDER, 1);
                            }
                            if (!((HwFingerprintService.this.getSystemSetting(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.LABEL_NAVI_SWITCH_REMINDER) == 1) || (isNaviBarEnabled ^ 1) == 0 || (hasShowDialogOnce ^ 1) == 0)) {
                                HwFingerprintService.this.showSetVirNavDialog();
                            }
                        }
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
                Log.d(HwFingerprintService.TAG, "Read the navigation mode after user switch, mVirNavModeEnabled = " + HwFingerprintService.this.mVirNavModeEnabled);
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
            int fpState = Secure.getIntForUser(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.KEY_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser());
            boolean hasFingerprints = FingerprintUtils.getInstance().getFingerprintsForUser(HwFingerprintService.this.mContext, ActivityManager.getCurrentUser()).size() > 0;
            if (fpState != 0 && (hasFingerprints ^ 1) == 0) {
                return;
            }
            if ("android.intent.action.SCREEN_ON".equals(HwFingerprintService.this.mScreen)) {
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_UPGRADING_FREQUENCE);
            } else if ("android.intent.action.SCREEN_OFF".equals(HwFingerprintService.this.mScreen)) {
                HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_REDUCING_FREQUENCE);
            }
        }
    };

    private class FingerViewChangeCallback implements ICallBack {
        private FingerViewChangeCallback() {
        }

        public void onFingerViewStateChange(int type) {
            Log.d(HwFingerprintService.TAG, "View State Change to " + type);
            HwFingerprintService.this.suspendAuthentication(type == 2 ? 1 : 0);
        }

        public void onNotifyCaptureImage() {
            Log.d(HwFingerprintService.TAG, "onNotifyCaptureImage");
            HwFingerprintService.this.notifyCaptureOpticalImage();
        }
    }

    private class HwFIDOAuthenticationClient extends AuthenticationClient {
        private String aaid;
        private int groupId;
        private IAuthenticatorListener listener;
        private IFidoAuthenticationCallback mFidoAuthenticationCallback = new IFidoAuthenticationCallback.Stub() {
            public void onUserVerificationResult(final int result, long opId, final ArrayList<Byte> userId, final ArrayList<Byte> encapsulatedResult) {
                Log.d(HwFingerprintService.TAG, "onUserVerificationResult");
                HwFingerprintService.this.mHandler.post(new Runnable() {
                    public void run() {
                        Log.d(HwFingerprintService.TAG, "onUserVerificationResult-run");
                        if (HwFIDOAuthenticationClient.this.listener != null) {
                            try {
                                int i;
                                byte[] byteUserId = new byte[userId.size()];
                                int userIdLen = userId.size();
                                for (i = 0; i < userIdLen; i++) {
                                    byteUserId[i] = ((Byte) userId.get(i)).byteValue();
                                }
                                byte[] byteEncapsulatedResult = new byte[encapsulatedResult.size()];
                                int encapsulatedResultLen = encapsulatedResult.size();
                                for (i = 0; i < encapsulatedResultLen; i++) {
                                    byteEncapsulatedResult[i] = ((Byte) encapsulatedResult.get(i)).byteValue();
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

        public HwFIDOAuthenticationClient(Context context, long halDeviceId, IBinder token, IFingerprintServiceReceiver receiver, int callingUserId, int groupId, long opId, boolean restricted, String owner, IAuthenticatorListener listener, String aaid, byte[] nonce) {
            super(context, halDeviceId, token, receiver, callingUserId, groupId, opId, restricted, owner);
            this.pkgName = owner;
            this.listener = listener;
            this.groupId = groupId;
            this.aaid = aaid;
            this.nonce = nonce;
        }

        public boolean onAuthenticated(int fingerId, int groupId) {
            if (fingerId != 0) {
            }
            return super.onAuthenticated(fingerId, groupId);
        }

        public int handleFailedAttempt() {
            HwFingerprintService.setParentPrivateField(HwFingerprintService.this, "mFailedAttempts", Integer.valueOf(((Integer) HwFingerprintService.getParentPrivateField(HwFingerprintService.this, "mFailedAttempts")).intValue() + 1));
            int lockoutMode = HwFingerprintService.this.getLockoutMode();
            if (!inLockoutMode()) {
                return 0;
            }
            HwFingerprintService.setParentPrivateField(HwFingerprintService.this, "mLockoutTime", Long.valueOf(SystemClock.elapsedRealtime()));
            HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "scheduleLockoutReset", null, null);
            onError(7, 0);
            stop(true);
            return lockoutMode;
        }

        public void resetFailedAttempts() {
            HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "resetFailedAttempts", new Class[]{Boolean.TYPE}, new Object[]{Boolean.valueOf(true)});
        }

        public void notifyUserActivity() {
            HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "userActivity", null, null);
        }

        public IBiometricsFingerprint getFingerprintDaemon() {
            return (IBiometricsFingerprint) HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "getFingerprintDaemon", null, null);
        }

        public void handleHwFailedAttempt(int flags, String packagesName) {
            HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "handleHwFailedAttempt", new Class[]{Integer.TYPE, String.class}, new Object[]{Integer.valueOf(0), null});
        }

        public boolean inLockoutMode() {
            return ((Boolean) HwFingerprintService.invokeParentPrivateFunction(HwFingerprintService.this, "inLockoutMode", null, null)).booleanValue();
        }

        public int start() {
            Slog.d("FingerprintService", "start pkgName:" + this.pkgName);
            try {
                doVerifyUser(this.groupId, this.aaid, this.nonce);
            } catch (RemoteException e) {
                Log.w(HwFingerprintService.TAG, "call fingerprintD verify user failed");
            }
            return 0;
        }

        private void doVerifyUser(int groupId, String aaid, byte[] nonce) throws RemoteException {
            if (HwFingerprintService.this.isFingerprintDReady()) {
                IExtBiometricsFingerprint daemon = HwFingerprintService.this.getFingerprintDaemonEx();
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
                    Slog.e("FingerprintService", "doVerifyUser RemoteException:" + e);
                }
            }
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
        if (mDaemonEx != null) {
            return mDaemonEx;
        }
        Slog.w(TAG, "fingerprint HIDL not available");
        return null;
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
        Slog.d(TAG, "ParentControl status is " + status);
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
            Slog.d(TAG, "setKidsFingerprint-isParent = " + isParent + ", isPcCastMode =" + isPcCastMode);
            if (isKeyguard && isKidSwitchOn(userID, this.mContext) && (isParent ^ 1) != 0 && (isPcCastMode ^ 1) != 0) {
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
                Slog.e(TAG, "setLivenessSwitch RemoteException:" + e);
            }
            Slog.d(TAG, "framework setLivenessSwitch is ok ---end");
        }
    }

    private void startAuthentication(IBinder token, long opId, int callingUserId, int groupId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName, IAuthenticatorListener listener, String aaid, byte[] nonce) {
        invokeParentPrivateFunction(this, "updateActiveGroup", new Class[]{Integer.TYPE, String.class}, new Object[]{Integer.valueOf(groupId), opPackageName});
        Log.v(TAG, "HwFingerprintService-startAuthentication(" + opPackageName + ")");
        AuthenticationClient client = new HwFIDOAuthenticationClient(getContext(), 0, token, receiver, callingUserId, groupId, opId, restricted, opPackageName, listener, aaid, nonce);
        if (((Boolean) invokeParentPrivateFunction(this, "inLockoutMode", null, null)).booleanValue()) {
            if ((((Boolean) invokeParentPrivateFunction(this, "isKeyguard", new Class[]{String.class}, new Object[]{opPackageName})).booleanValue() ^ 1) != 0) {
                Log.v(TAG, "In lockout mode; disallowing authentication");
                if (!client.onError(7, 0)) {
                    Log.w(TAG, "Cannot send timeout message to client");
                }
                return;
            }
        }
        invokeParentPrivateFunction(this, "startClient", new Class[]{ClientMonitor.class, Boolean.TYPE}, new Object[]{client, Boolean.valueOf(true)});
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

    private void showSetVirNavDialog() {
        View view = LayoutInflater.from(this.mContext).inflate(34013293, null);
        if (view == null) {
            Log.e(TAG, "showSetVirNavDialog, view is null in dialog");
            return;
        }
        this.mFrontPhysicRadio = (RadioButton) view.findViewById(34603043);
        this.mSingleVirNaviRadio = (RadioButton) view.findViewById(34603204);
        this.mTripleVirNaviRadio = (RadioButton) view.findViewById(34603233);
        if (this.mFrontPhysicRadio == null || this.mSingleVirNaviRadio == null || this.mTripleVirNaviRadio == null) {
            Log.e(TAG, "showSetVirNavDialog, some of radio buttons is null in dialog");
            return;
        }
        this.mFrontPhysicRadio.setOnCheckedChangeListener(this.mCheckedListener);
        this.mSingleVirNaviRadio.setOnCheckedChangeListener(this.mCheckedListener);
        this.mTripleVirNaviRadio.setOnCheckedChangeListener(this.mCheckedListener);
        this.mSetVirNavDialog = new Builder(this.mContext, 33947691).setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                HwFingerprintService.mNeedRecreateDialog = false;
                if (HwFingerprintService.this.mSingleVirNaviRadio.isChecked()) {
                    HwFingerprintService.this.setVirtualNavBar(1);
                } else if (HwFingerprintService.this.mTripleVirNaviRadio.isChecked()) {
                    HwFingerprintService.this.setVirtualNavBar(0);
                } else {
                    boolean isChecked = HwFingerprintService.this.mFrontPhysicRadio.isChecked();
                }
                HwFingerprintService.this.putSystemSettings(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.LABEL_NAVI_SWITCH_REMINDER, 1);
            }
        }).setNegativeButton(17039360, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                HwFingerprintService.mNeedRecreateDialog = false;
                HwFingerprintService.this.mSetVirNavDialog.dismiss();
                HwFingerprintService.this.putSystemSettings(HwFingerprintService.this.mContext.getContentResolver(), HwFingerprintService.LABEL_NAVI_SWITCH_REMINDER, 1);
            }
        }).setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if (!HwFingerprintService.mNeedRecreateDialog) {
                    HwFingerprintService.this.unRegisterPhoneStateReceiver();
                }
            }
        }).setTitle(this.mContext.getString(33686089)).setView(view).create();
        if (this.mSetVirNavDialog != null) {
            this.mSetVirNavDialog.getWindow().setType(DeviceStatusConstant.MSDP_DEVICE_STATUS_MOVEMENT);
            this.mSetVirNavDialog.setCanceledOnTouchOutside(false);
            this.mSetVirNavDialog.setCancelable(false);
            this.mSetVirNavDialog.show();
        }
        registerPhoneStateReceiver();
    }

    private void setVirtualNavBar(int type) {
        System.putIntForUser(this.mContext.getContentResolver(), "enable_navbar", 1, -2);
        System.putIntForUser(this.mContext.getContentResolver(), "ai_navigationbar", type, -2);
    }

    private int getSystemSetting(ContentResolver resolver, String key) {
        return System.getIntForUser(resolver, key, -1, -2);
    }

    private void putSystemSettings(ContentResolver resolver, String key, int value) {
        System.putIntForUser(resolver, key, value, -2);
    }

    private void showDialog(final boolean withConfirm) {
        Builder builder = new Builder(this.mContext, 33947691).setPositiveButton(withConfirm ? 33686076 : 33686073, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                HwFingerprintService.mNeedRecreateDialog = false;
                if (withConfirm) {
                    HwFingerprintService.this.sendCommandToHal(HwFingerprintService.MSG_DEL_OLD_TEMPLATES);
                    if (HwFingerprintService.this.checkNeedReEnrollFingerPrints() == 1) {
                        HwFingerprintService.this.updateActiveGroupEx(HwFingerprintService.HIDDEN_SPACE_ID);
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
        }).setTitle(this.mContext.getString(33685797)).setMessage(this.mContext.getString(withConfirm ? 33686075 : 33686074));
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
            this.mReEnrollDialog.getWindow().setType(DeviceStatusConstant.MSDP_DEVICE_STATUS_MOVEMENT);
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
                    if (telephonyManager != null) {
                        if (HwFingerprintService.this.mReEnrollDialog != null) {
                            if (telephonyManager.getCallState() == 1 && HwFingerprintService.this.mReEnrollDialog.isShowing()) {
                                HwFingerprintService.mNeedRecreateDialog = true;
                                HwFingerprintService.this.mReEnrollDialog.dismiss();
                            } else if (telephonyManager.getCallState() == 0 && (HwFingerprintService.this.mReEnrollDialog.isShowing() ^ 1) != 0) {
                                HwFingerprintService.this.mReEnrollDialog.show();
                            }
                        }
                        if (HwFingerprintService.this.mSetVirNavDialog == null) {
                            return;
                        }
                        if (telephonyManager.getCallState() == 1 && HwFingerprintService.this.mSetVirNavDialog.isShowing()) {
                            HwFingerprintService.mNeedRecreateDialog = true;
                            HwFingerprintService.this.mSetVirNavDialog.dismiss();
                        } else if (telephonyManager.getCallState() == 0 && (HwFingerprintService.this.mSetVirNavDialog.isShowing() ^ 1) != 0) {
                            HwFingerprintService.this.mSetVirNavDialog.show();
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
                    userIdForHal = HIDDEN_SPACE_ID;
                    Slog.i(TAG, "userIdForHal is " + HIDDEN_SPACE_ID);
                }
                if (userIdForHal == HIDDEN_SPACE_ID) {
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
        Log.i(TAG, "updateTPState udFingerprintExist " + udFingerprintExist + ",opPackageName:" + opPackageName);
    }

    private void initNavModeObserver() {
        if (this.mNavModeObserver == null) {
            Log.d(TAG, "mNavModeObserver is null");
        } else if (this.mContext == null || this.mContext.getContentResolver() == null) {
            Log.d(TAG, "mContext or the resolver is null");
        } else {
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("enable_navbar"), true, this.mNavModeObserver, -1);
            this.mVirNavModeEnabled = FrontFingerPrintSettings.isNaviBarEnabled(this.mContext.getContentResolver());
            Log.d(TAG, "Read the navigation mode after boot, mVirNavModeEnabled = " + this.mVirNavModeEnabled);
            sendCommandToHal(this.mVirNavModeEnabled ? 45 : 43);
            this.mNavModeObserver.onChange(true);
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
            return result;
        }
        try {
            result = daemon.sendCmdToHal(MSG_CHECK_SWITCH_FREQUENCE_SUPPORT);
        } catch (RemoteException e) {
            Slog.e(TAG, "checkSwitchFrequenceSupport RemoteException:" + e);
        }
        return result;
    }

    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        Slog.v(TAG, "HwFingerprintService onBootPhase:" + phase);
        if (phase == 1000) {
            initSwitchFrequence();
            initPositionAndType();
            if (mRemoveFingerprintBGE) {
                initUserSwitchReceiver();
                initNavModeObserver();
            }
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
                int i;
                ArrayList mNewFingerprints = null;
                int fingerprintGpSize = hwFpSets.mFingerprintGroups.size();
                for (i = 0; i < fingerprintGpSize; i++) {
                    HwFingerprintGroup fpGroup = (HwFingerprintGroup) hwFpSets.mFingerprintGroups.get(i);
                    int realGroupId = fpGroup.mGroupId;
                    if (fpGroup.mGroupId == HIDDEN_SPACE_ID) {
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
                for (i = 0; i < size; i++) {
                    Fingerprint fp = (Fingerprint) mNewFingerprints.get(i);
                    if (utils.isDualFp()) {
                        utils.addFingerprintForUser(this.mContext, fp.getFingerId(), userId, deviceIndex);
                    } else {
                        utils.addFingerprintForUser(this.mContext, fp.getFingerId(), userId);
                    }
                    CharSequence fpName = fp.getName();
                    if (!(fpName == null || (fpName.toString().isEmpty() ^ 1) == 0)) {
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
        } catch (RemoteException e) {
            Slog.e(TAG, "checkNeedReEnrollFingerPrints RemoteException:" + e);
        }
        return 0;
    }

    private int checkNeedReEnrollFingerPrints() {
        int result = -1;
        Log.w(TAG, "checkNeedReEnrollFingerPrints");
        if (!isFingerprintDReady()) {
            return result;
        }
        IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
        if (daemon == null) {
            Slog.e(TAG, "Fingerprintd is not available!");
            return result;
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
        int result;
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
        } else if (code == CODE_SHOW_FINGERPRINT_BUTTON_RULE) {
            Slog.d(TAG, "code == CODE_SHOW_FINGERPRINT_BUTTON_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            showSuspensionButton(data.readInt(), data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == CODE_REMOVE_FINGERPRINT_RULE) {
            Slog.d(TAG, "code == CODE_REMOVE_FINGERPRINT_RULE");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            removeFingerprintView();
            reply.writeNoException();
            return true;
        } else if (code == CODE_GET_HARDWARE_POSITION) {
            Slog.d(TAG, "CODE_GET_HARDWARE_POSITION");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            int[] result2 = getFingerprintHardwarePosition();
            reply.writeNoException();
            reply.writeInt(result2[0]);
            reply.writeInt(result2[1]);
            reply.writeInt(result2[2]);
            reply.writeInt(result2[3]);
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
        } else if (code == CODE_SUSPEND_AUTHENTICATE) {
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
        } else if (code == CODE_REMOVE_MASK_AND_SHOW_BUTTON_RULE) {
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
        } else if (code == CODE_GET_FINGERPRINT_LIST_ENROLLED) {
            Slog.d(TAG, "CODE_GET_FINGERPRINT_LIST_ENROLLED");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            List<Fingerprint> fingerprints = getEnrolledFingerprintsNoWhitelist(data.readString(), data.readInt(), data.readInt());
            reply.writeNoException();
            reply.writeTypedList(fingerprints);
            return true;
        } else if (code != CODE_IS_SUPPORT_DUAL_FINGERPRINT) {
            return super.onHwTransact(code, data, reply, flags);
        } else {
            Slog.d(TAG, "CODE_IS_SUPPORT_DUAL_FINGERPRINT");
            data.enforceInterface(DESCRIPTOR_FINGERPRINT_SERVICE);
            reply.writeNoException();
            reply.writeBoolean(isSupportDualFingerprint());
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
            Slog.e(TAG, "dualfingerprint sendCmdToHal RemoteException:" + e);
            return -1;
        }
    }

    protected boolean canUseUdFingerprint(String opPackageName) {
        if (opPackageName == null || "".equals(opPackageName)) {
            Slog.d(TAG, "calling opPackageName is invalid");
            return false;
        } else if (opPackageName.equals(this.mDefinedAppName)) {
            Slog.d(TAG, opPackageName + " has enable inscreen fingerprint ");
            return true;
        } else {
            Slog.d(TAG, "canUseUdFingerprint opPackageName is " + opPackageName);
            for (String pkgName : this.mWhitelist) {
                if (pkgName.equals(opPackageName)) {
                    return true;
                }
            }
            long token = Binder.clearCallingIdentity();
            try {
                String type = "";
                Bundle metaData = this.mContext.getPackageManager().getApplicationInfo(opPackageName, 128).metaData;
                if (metaData == null) {
                    Slog.d(TAG, "metaData is null");
                }
                if (metaData != null) {
                    type = metaData.getString("fingerprint.system.view");
                    if (!(type == null || ("".equals(type) ^ 1) == 0)) {
                        Slog.d(TAG, "calling opPackageName  metaData value is: " + type);
                        return true;
                    }
                }
                Binder.restoreCallingIdentity(token);
            } catch (NameNotFoundException e) {
                Slog.e(TAG, "cannot find metaData of package: " + e);
            } catch (Exception e2) {
                Slog.e(TAG, "exception occured: " + e2);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
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
        Slog.d(TAG, "dualFingerprint getEnrolledFingerprints opPackageName is " + opPackageName + " userId is " + userId);
        return FingerprintUtils.getInstance().getFingerprintsForUser(this.mContext, userId, targetDevice);
    }

    protected boolean isHardwareDetectedEx(String opPackageName, int targetDevice) {
        boolean z = true;
        boolean z2 = false;
        if (getFingerprintDaemon() == null) {
            Slog.d(TAG, "Daemon is not available!");
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (canUseUdFingerprint(opPackageName)) {
                if (targetDevice == 1) {
                    if (getUdHalDeviceId() == 0) {
                        z = false;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                } else if (targetDevice == 0) {
                    if (getHalDeviceId() == 0) {
                        z = false;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z;
                } else {
                    if (!(getHalDeviceId() == 0 || getUdHalDeviceId() == 0)) {
                        z2 = true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return z2;
                }
            } else if (targetDevice == 0) {
                if (getHalDeviceId() == 0) {
                    z = false;
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
        boolean z = true;
        boolean z2 = false;
        if (getFingerprintDaemon() == null) {
            Slog.d(TAG, "Daemon is not available!");
            return false;
        }
        Slog.d(TAG, "dualFingerprint isHardwareDetected opPackageName is " + opPackageName + " targetDevice is " + targetDevice);
        long token = Binder.clearCallingIdentity();
        if (targetDevice == 1) {
            try {
                if (getUdHalDeviceId() == 0) {
                    z = false;
                }
                Binder.restoreCallingIdentity(token);
                return z;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else if (targetDevice == 0) {
            if (getHalDeviceId() == 0) {
                z = false;
            }
            Binder.restoreCallingIdentity(token);
            return z;
        } else {
            if (!(getHalDeviceId() == 0 || getUdHalDeviceId() == 0)) {
                z2 = true;
            }
            Binder.restoreCallingIdentity(token);
            return z2;
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
        if (isFingerprintDReady()) {
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
                if (this.mFingerViewController != null) {
                    if (!"com.android.systemui".equals(pkgNamefromBundle)) {
                        this.mFingerViewController.updateMaskViewAttributes(bundle, callingApp);
                    } else if (!this.mForbideKeyguardCall) {
                        this.mFingerViewController.parseBundle4Keyguard(bundle);
                    }
                }
            }
        }
    }

    public void notifyAuthenticationStarted(String pkgName, IFingerprintServiceReceiver receiver, int flag, int userID) {
        initPositionAndType();
        if (!this.mIsFingerInScreenSupported) {
            return;
        }
        if (pkgName == null) {
            Log.d(TAG, "pkgname is null");
            return;
        }
        checkPermissions();
        if (flag == 0 || (FLAG_USE_UD_FINGERPRINT & flag) != 0) {
            Log.d(TAG, "show,pkgName =" + pkgName + " userID = " + userID);
            int initType = -1;
            if (pkgName.equals(this.mDefinedAppName)) {
                initType = this.mAppDefinedMaskType;
                Log.d(TAG, "initType = " + initType + ",defined by enable interface");
            }
            if (initType == -1) {
                initType = getAppType(pkgName);
            }
            if (initType == -1) {
                for (String name : this.mWhitelist) {
                    if (name.equals(pkgName)) {
                        Log.d(TAG, "pkgName in whitelist, show default mask for it");
                        initType = 0;
                    }
                }
            }
            if (pkgName.equals("com.android.cts.verifier")) {
                initType = 0;
                Log.d(TAG, "com.android.cts.verifier initType = " + 0);
            }
            boolean hasUdFingerprint = true;
            boolean hasBackFingerprint = false;
            FingerprintUtils fingerprintUtils = FingerprintUtils.getInstance();
            if (fingerprintUtils.isDualFp()) {
                hasUdFingerprint = fingerprintUtils.getFingerprintsForUser(this.mContext, userID, 1).size() > 0;
                updateTPState(pkgName, hasUdFingerprint);
                hasBackFingerprint = fingerprintUtils.getFingerprintsForUser(this.mContext, userID, 0).size() > 0;
                if (!hasUdFingerprint) {
                    Log.d(TAG, "userID:" + userID + "has no UD_fingerprint");
                    if (initType != 3) {
                        return;
                    }
                }
            }
            if ("com.android.settings".equals(pkgName) && this.mIsUdFingerprintNeed) {
                Log.d(TAG, "hgy settings notifyAuthenticationStarted add highlight view");
                this.mFingerViewController.showHighlightview(3);
            }
            if (this.mIsUdFingerprintNeed && (checkIfCallerDisableMask() ^ 1) != 0) {
                this.mFingerViewController.showMaskOrButton(pkgName, this.mMaskViewBundle, receiver, initType, hasUdFingerprint, hasBackFingerprint);
            }
            Log.d(TAG, " begin add windowservice view");
            this.mIsUdAuthenticating = true;
            return;
        }
        Log.d(TAG, "flag = " + flag);
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
                Log.d(TAG, "mKeepMaskAfterAuthentication is false,start remove,pkgName = " + pkgName);
                this.mFingerViewController.removeMaskOrButton();
            }
            if ("com.android.settings".equals(pkgName)) {
                Log.d(TAG, " settings notifyAuthenticationCanceled remove highlight view");
                this.mFingerViewController.removeHighlightview(-1);
            }
            this.mIsUdAuthenticating = false;
            this.mKeepMaskAfterAuthentication = false;
            this.mAppDefinedMaskType = -1;
            this.mDefinedAppName = "";
        }
    }

    private void removeFingerprintView() {
        if (this.mIsFingerInScreenSupported) {
            Log.d(TAG, "removeFingerprintView,callingApp = " + this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()));
            checkPermissions();
            this.mMaskViewBundle = null;
            this.mFingerViewController.removeMaskOrButton();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyFingerDown(int type) {
        Log.d(TAG, "notifyFingerDown mIsFingerInScreenSupported:" + this.mIsFingerInScreenSupported);
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
                        Binder.restoreCallingIdentity(token);
                    } catch (SecurityException e) {
                        Log.e(TAG, "catch SecurityException");
                    } catch (SettingNotFoundException e2) {
                        Log.d(TAG, "settings not found");
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(token);
                    }
                    this.mFingerViewController.showHighlightview(1);
                } else if (type == 0 || type == 2) {
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
            if (flags == 4096) {
                if (this.mDisplayEngineManager != null) {
                    this.mDisplayEngineManager.setScene(31, 16);
                } else {
                    Log.w(TAG, "mDisplayEngineManager is null!");
                }
            }
            if (!FingerprintUtils.getInstance().isDualFp() || flags == 4096) {
                this.mIsUdEnrolling = true;
                checkPermissions();
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
        Log.d(TAG, "notifyEnrollmentEnd mIsUdEnrolling: " + this.mIsUdEnrolling);
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
            Log.d(TAG, "notifyAuthenticationFinished,mIsUdFingerprintChecking = " + this.mIsUdFingerprintChecking + ",result =" + result + "failTimes = " + failTimes);
            if (this.mIsHighLightNeed && ("com.android.settings".equals(opName) ^ 1) != 0 && this.mIsUdFingerprintChecking) {
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
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
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
            Log.d(TAG, "callingApp = " + callingApp);
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
            Log.d(TAG, "enableFingerprintView,callingApp = " + callingApp);
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
                Log.d(TAG, "pkgName is " + pkgName + "metaData is " + metaData.getString("fingerprint.system.view"));
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
                Log.d(TAG, "metaData type is " + type);
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
                    Log.d(TAG, "foreground processName = " + proc.processName);
                    return proc.processName;
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
        }
        return "";
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

    private void initPositionAndType() {
        boolean z = true;
        if (this.mFingerprintType != -1) {
            Log.e(TAG, "FingerprintType has benn inited ,Type =" + this.mFingerprintType);
            return;
        }
        this.mFingerprintType = getFingerprintHardwareTypeInternal();
        Log.d(TAG, "hardware type = " + this.mFingerprintType);
        this.mIsFingerInScreenSupported = this.mFingerprintType > 0;
        if (this.mFingerprintType != 1) {
            z = false;
        }
        this.mIsHighLightNeed = z;
        if (this.mIsFingerInScreenSupported) {
            this.mFingerViewController = FingerViewController.getInstance(this.mContext);
            this.mFingerViewController.registCallback(new FingerViewChangeCallback());
            int[] position = getFingerprintHardwarePosition();
            Log.d(TAG, " hardware type = " + this.mFingerprintType);
            this.mFingerViewController.setFingerprintPosition(position);
            initObserver();
        }
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
            Slog.d(TAG, "pacel  packaged suspendAuthentication: " + status);
            if (status == 1) {
                try {
                    daemon.sendCmdToHal(53);
                } catch (RemoteException e) {
                    Slog.e(TAG, "suspendAuthentication RemoteException:" + e);
                }
            } else {
                daemon.sendCmdToHal(MSG_RESUME_AUTHENTICATION);
            }
        } else {
            Log.w(TAG, "do not have UD device suspend invalid");
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
            result = daemon.sendCmdToHal(MSG_GET_SENSOR_TYPE);
        } catch (RemoteException e) {
            Slog.e(TAG, "HardwareType RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd HardwareType = " + result);
        return result;
    }

    private int getFingerprintHardwareTypeInternal() {
        int typeDetails = getFingerprintHardwareType();
        if (typeDetails == -1) {
            return -1;
        }
        int offset = -1;
        if ((typeDetails & 1) != 0) {
            offset = 0;
        }
        if ((typeDetails & 2) != 0) {
            offset++;
        }
        if ((typeDetails & 4) == 0) {
            return 0;
        }
        int physicalType = (typeDetails >> (((offset + 1) * 4) + 8)) & 15;
        Log.d(TAG, "LOCATION_UNDER_DISPLAY physicalType :" + physicalType);
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
            Slog.e(TAG, "HardwarePosition RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd HardwarePosition = " + result[0] + " " + result[1]);
        if (result[0] == -1) {
            String[] positionG = SystemProperties.get("persist.sys.fingerprint.hardwarePosition", "-1,-1,-1,-1").split(",");
            pxPosition[0] = Integer.parseInt(positionG[0]);
            pxPosition[1] = Integer.parseInt(positionG[1]);
            pxPosition[2] = Integer.parseInt(positionG[2]);
            pxPosition[3] = Integer.parseInt(positionG[3]);
            Log.d(TAG, "getHardwarePosition from SystemProperties: " + pxPosition[0]);
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
        int[] covertPosition = new int[]{-1, -1, -1, -1};
        if (this.mInitDisplayHeight == -1 && this.mContext != null) {
            this.mInitDisplayHeight = Global.getInt(this.mContext.getContentResolver(), APS_INIT_HEIGHT, DENSITY_DEFUALT_HEIGHT);
            this.mInitDisplayWidth = Global.getInt(this.mContext.getContentResolver(), APS_INIT_WIDTH, 1440);
        }
        covertPosition[0] = (input[0] * this.mInitDisplayWidth) / 1000;
        covertPosition[1] = (input[1] * this.mInitDisplayHeight) / 1000;
        covertPosition[2] = (input[2] * this.mInitDisplayWidth) / 1000;
        covertPosition[3] = (input[3] * this.mInitDisplayHeight) / 1000;
        Log.d(TAG, "Width: " + this.mInitDisplayWidth + " height: " + this.mInitDisplayHeight);
        for (int i = 0; i < 4; i++) {
            Log.d(TAG, "use hal after covert: " + covertPosition[i]);
        }
        return covertPosition;
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
                Slog.e(TAG, "notifyCaptureOpticalImage RemoteException:" + e);
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
            Slog.d(TAG, "pacel  packaged setHoverEventSwitch: " + enabled);
            if (enabled == 1) {
                try {
                    daemon.sendCmdToHal(MSG_SET_HOVER_ENABLE);
                } catch (RemoteException e) {
                    Slog.e(TAG, "setHoverEventSwitch RemoteException:" + e);
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
            Slog.e(TAG, "getHoverEventSupport RemoteException:" + e);
        }
        Slog.d(TAG, "fingerprintd getHoverEventSupport = " + result);
        return result;
    }

    private int checkForegroundNeedLiveness() {
        Slog.w(TAG, "checkForegroundNeedLiveness:start");
        try {
            List<RunningAppProcessInfo> procs = ActivityManagerNative.getDefault().getRunningAppProcesses();
            if (procs == null) {
                return 0;
            }
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                RunningAppProcessInfo proc = (RunningAppProcessInfo) procs.get(i);
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

    private int checkNeedLivenessList(String opPackageName) {
        Slog.w(TAG, "checkNeedLivenessList:start");
        if (opPackageName == null || opPackageName.equals("com.android.keyguard")) {
            return 0;
        }
        if (opPackageName.equals("com.huawei.securitymgr")) {
            return checkForegroundNeedLiveness();
        }
        return (opPackageName.equals("com.eg.android.AlipayGphone") || opPackageName.equals("fido") || opPackageName.equals("com.alipay.security.mobile.authentication.huawei") || opPackageName.equals("com.huawei.wallet") || opPackageName.equals("com.huawei.android.hwpay")) ? 1 : 0;
    }

    protected void setLivenessSwitch(String opPackageName) {
        Slog.w(TAG, "setLivenessSwitch:start");
        if ((!mLivenessNeedBetaQualification || (isBetaUser() ^ 1) == 0) && isFingerprintDReady()) {
            int NEED_LIVENESS_AUTHENTICATION = checkNeedLivenessList(opPackageName);
            Slog.w(TAG, "NEED_LIVENESS_AUTHENTICATION = " + NEED_LIVENESS_AUTHENTICATION);
            IExtBiometricsFingerprint daemon = getFingerprintDaemonEx();
            if (daemon == null) {
                Slog.e(TAG, "Fingerprintd is not available!");
                return;
            }
            try {
                daemon.setLivenessSwitch(NEED_LIVENESS_AUTHENTICATION);
            } catch (RemoteException e) {
                Slog.e(TAG, "setLivenessSwitch RemoteException:" + e);
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
        if (!checkPackageName(opPackageName) || (HwGlobalActionsData.FLAG_SHUTDOWN_CONFIRM & flags) == 0) {
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
        HwFingerprintSets hwFingerprintSets;
        ArrayList<Integer> fingerprintInfo = new ArrayList();
        try {
            fingerprintInfo = daemon.getFpOldData();
        } catch (RemoteException e) {
            Slog.e(TAG, "remoteGetOldData RemoteException:" + e);
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
            hwFingerprintSets = (HwFingerprintSets) HwFingerprintSets.CREATOR.createFromParcel(_reply);
        } else {
            hwFingerprintSets = null;
        }
        _reply.recycle();
        return hwFingerprintSets;
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
}
