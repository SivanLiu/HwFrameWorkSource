package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.HwRecentTaskInfo;
import android.app.IActivityController;
import android.app.IHwActivityNotifier;
import android.app.ITaskStackListener;
import android.app.KeyguardManager;
import android.app.mtm.MultiTaskUtils;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.cover.CoverManager;
import android.cover.HallState;
import android.cover.IHallCallback;
import android.database.ContentObserver;
import android.freeform.HwFreeFormUtils;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.HwFoldScreenState;
import android.hdm.HwDeviceManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IMWThirdpartyCallback;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.rms.iaware.AppTypeRecoManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.CoordinationModeUtils;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.IApplicationToken;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.server.CoordinationStackDividerManager;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.PPPOEStateMachine;
import com.android.server.ServiceThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppTimeTracker;
import com.android.server.am.BaseErrorDialog;
import com.android.server.am.HwActivityManagerService;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.pm.HwThemeInstaller;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.security.hsm.HwAddViewHelper;
import com.android.server.security.permissionmanager.util.PermConst;
import com.android.server.security.securityprofile.ISecurityProfileController;
import com.android.server.security.securityprofile.IntentCaller;
import com.android.server.security.trustspace.ITrustSpaceController;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.ActivityStack;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.app.IGameObserver;
import com.huawei.android.app.IGameObserverEx;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hwaps.HwApsImpl;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.HwPCFactory;
import com.huawei.server.multiwindowtip.HwMultiWindowTips;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class HwActivityTaskManagerServiceEx implements IHwActivityTaskManagerServiceEx {
    private static final String ACTION_HWOUC_SHOW_UPGRADE_REMIND = "com.huawei.android.hwouc.action.SHOW_UPGRADE_REMIND";
    private static final Set<String> ACTIVITY_NOTIFIER_TYPES = new HashSet<String>() {
        /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass1 */

        {
            add("returnToHome");
            add(SceneRecogFeature.REASON_INFO);
            add("appSwitch");
            add("appDie");
            add("toggleFreeform");
        }
    };
    private static final int APP_ASSOC_HOME_UPDATE = 11;
    private static final int APP_FOCUS_CHANGE = 230;
    private static final String ASSOC_PID = "pid";
    private static final String ASSOC_PKGNAME = "pkgname";
    private static final String ASSOC_RELATION_TYPE = "relationType";
    private static final String ASSOC_TGT_UID = "tgtUid";
    private static final String BOOT_PERMISSION = "android.permission.RECEIVE_BOOT_COMPLETED";
    private static final long ENABLE_EVENT_DISPATCHING_DELAY_MILLIS = 1000;
    private static final int EXIT_COORDINATION_MODE_TIMEOUT = 1500;
    private static final int FULLSCREEN_REC_TOP = -1;
    private static final Rect FULLSCREEN_WINDOW_RECT_REF = new Rect(-1, -1, -1, -1);
    private static final String HMS_PKG_NAME = "com.huawei.hwid";
    private static final int HWOUC_UPDATE_REMIND_MSG = 80;
    private static final String HW_LAUNCHER_PKGNAME = "com.huawei.android.launcher";
    private static final boolean HW_SHOW_INCOMPATIBLE_DIALOG = SystemProperties.getBoolean("ro.config.incompatible_dialog", false);
    private static final boolean HW_SNAPSHOT = SystemProperties.getBoolean("ro.huawei.only_hwsnapshot", true);
    private static final boolean HW_SUPPORT_LAUNCHER_EXIT_ANIM = (!SystemProperties.getBoolean("ro.config.disable_launcher_exit_anim", false));
    private static final String HW_SYSTEM_SERVER_START = "com.huawei.systemserver.START";
    private static final boolean IS_BOPD = SystemProperties.getBoolean("sys.bopd", false);
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final boolean IS_PROP_FINGER_BOOST = SystemProperties.getBoolean("persist.debug.finger_boost", true);
    private static final int MAXIMIZE_REC_TOP = 0;
    private static final Rect MAXIMIZE_WINDOW_RECT_REF = new Rect(0, 0, 0, 0);
    private static final int MULTI_WINDOW_MODE_CHANGED_MSG = 23;
    private static final Set<String> NERVER_USE_COMPAT_MODE_APPS = new HashSet<String>() {
        /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass2 */

        {
            add("com.huawei.camera");
            add("com.android.incallui");
            add(HwThemeInstaller.HWT_OLD_CONTACT);
        }
    };
    private static final int NOTIFY_ACTIVITY_STATE = 71;
    private static final int NOTIFY_CALL = 24;
    private static final int NOTIFY_NEED_SWING_ROTATION = 90;
    private static final int NOTIFY_SHOW_EXSPLASH = 91;
    private static final Set<String> ONLY_NOTIFY_SYSTEM_USER_PROCESS = new HashSet<String>() {
        /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass3 */

        {
            add(FingerViewController.PKGNAME_OF_KEYGUARD);
            add("com.huawei.android.extdisplay");
            add(GestureNavConst.DEFAULT_DOCK_PACKAGE);
            add("com.huawei.systemserver");
            add("com.huawei.iaware");
        }
    };
    private static final String PACKAGE_HWOUC = "com.huawei.android.hwouc";
    private static final String PERMISSION_HWOUC_UPGRADE_REMIND = "com.huawei.android.hwouc.permission.UPGRADE_REMIND";
    /* access modifiers changed from: private */
    public static final File PRELOAD_APP_NOT_REMIND_XML = new File(Environment.getDataSystemDirectory(), "NotRemindIncompatibleApp.xml");
    public static final int PROVISIONED_OFF = 0;
    public static final int PROVISIONED_ON = 1;
    private static final int READ_INCOMPATIBLE_XML_MSG = 50;
    private static final int READ_NOT_REMIND_XML_MSG = 51;
    private static final String RESOURCE_APPASSOC = "RESOURCE_APPASSOC";
    private static final int SHOW_UNINSTALL_LAUNCHER_MSG = 48;
    private static final int SHOW_UPDATE_INCOMPATIBLE_APP_MSG = 49;
    private static final int SIDELEFT_REC_TOP = -2;
    private static final Rect SIDELEFT_WINDOW_RECT_REF = new Rect(-2, -2, -2, -2);
    private static final int SIDERIGHT_REC_TOP = -3;
    private static final Rect SIDERIGHT_WINDOW_RECT_REF = new Rect(-3, -3, -3, -3);
    private static final String SPLIT_SCREEN_APP_NAME = "splitscreen.SplitScreenAppActivity";
    private static final String STRING_INCOMPATIBLE_CFG_DIR = "xml/PreloadAppCompatibility";
    private static final String STRING_INCOMPATIBLE_CFG_FILE_PATH = "xml/PreloadAppCompatibility/PreloadAppCompatibility.xml";
    private static final String STRING_INCOMPATIBLE_XML_ATTRIBUTE_NAME = "name";
    private static final String STRING_INCOMPATIBLE_XML_ATTRIBUTE_PKG_VERSION = "pkgVersion";
    private static final String STRING_INCOMPATIBLE_XML_PKG = "package";
    private static final String STRING_INCOMPATIBLE_XML_PKG_LIST = "package_list";
    static final String TAG = "HwActivityTaskManagerServiceEx";
    private static final int TYPE_INCOMPATIBLE_XML_TO_HASHMAP = 0;
    private static final int TYPE_NOT_REMIND_XML_TO_SET = 1;
    private static final int WRITE_NOT_REMIND_XML_MSG = 52;
    private static float mDeviceMaxRatio = -1.0f;
    private static Set<String> mPIPWhitelists = new HashSet();
    private static Set<String> mTranslucentWhitelists = new HashSet();
    private static Set<String> mWhitelistActivities = new HashSet();
    private static HashMap<String, String> sCompatibileFileHashMap = new HashMap<>();
    private static Set<String> sPreventStartWhenSleeping = new HashSet();
    private final String faceKeyguardUriName = "face_bind_with_lock";
    private final String fingerPrintKeyguardUriName = "fp_keyguard_enable";
    private final String fingerUnlockBoostWhiteListFileName = "hw_finger_unlock_boost_whitelist";
    private boolean isLastMultiMode = false;
    /* access modifiers changed from: private */
    public final RemoteCallbackList<IHwActivityNotifier> mActivityNotifiers = new RemoteCallbackList<>();
    private Set<String> mCompatibileNoRemindSet = new HashSet();
    private File mCompatibileXmlFile = HwCfgFilePolicy.getCfgFile(STRING_INCOMPATIBLE_CFG_FILE_PATH, 0);
    final Context mContext;
    private Intent mCoordinationIntent;
    private String mCurrentPkgName;
    private IActivityController mCustomController = null;
    private FaceSettingsObserver mFaceSettingsObserver;
    private FingerprintSettingsObserver mFingerprintSettingsObserver;
    private List<String> mFingerprintUnlockBoostWhiteList;
    final HwGameAssistantController mHwGameAssistantController;
    Handler mHwHandler = null;
    ServiceThread mHwHandlerThread = null;
    HwMultiWindowManager mHwMwm = null;
    private final TaskChangeNotificationController mHwTaskChangeNotificationController;
    IHwActivityTaskManagerInner mIAtmsInner = null;
    private boolean mIsClickCancelButton = false;
    private boolean mIsDialogShow = false;
    private boolean mIsInitMultiWindowDisabledState = false;
    private boolean mIsMultiWindowDisabled = false;
    private boolean mIsSetFingerprintOrFaceKeyGuard = false;
    private boolean mIsSupportsFreeformBefore = false;
    private boolean mIsSupportsSplitScreenBefore = false;
    public boolean mKeepPrimaryCoordinationResumed;
    /* access modifiers changed from: private */
    public String mLastLauncherName;
    /* access modifiers changed from: private */
    public boolean mNeedRemindHwOUC = false;
    OverscanTimeout mOverscanTimeout = new OverscanTimeout();
    private HashMap<String, Long> mPCUsageStats = new HashMap<>();
    private HashMap<String, Integer> mPkgDisplayMaps = new HashMap<>();
    private String mPkgNameInCoordinationMode = "";
    private ActivityRecord mPreviousResumedActivity;
    private Intent mQuickSlideIntent = null;
    private long mQuickSlideStartTime;
    private SettingsObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public final RemoteCallbackList<IMWThirdpartyCallback> mThirdPartyCallbackList;
    ITrustSpaceController mTrustSpaceController;

    static {
        mWhitelistActivities.add("com.vlocker.settings.DismissActivity");
        mTranslucentWhitelists.add(HwMagicWinAmsPolicy.PERMISSION_ACTIVITY);
        mPIPWhitelists.add("com.android.systemui.pip.phone.PipMenuActivity");
        sPreventStartWhenSleeping.add("com.ss.android.article.news/com.ss.android.message.sswo.SswoActivity");
        sPreventStartWhenSleeping.add("com.ss.android.article.video/com.ss.android.message.sswo.SswoActivity");
        sPreventStartWhenSleeping.add("dongzheng.szkingdom.android.phone/com.dgzq.IM.ui.activity.KeepAliveActivity");
        sPreventStartWhenSleeping.add("com.tencent.news/.push.alive.offactivity.OffActivity");
    }

    public HwActivityTaskManagerServiceEx(IHwActivityTaskManagerInner atms, Context context) {
        this.mIAtmsInner = atms;
        this.mContext = context;
        this.mHwHandlerThread = new ServiceThread(TAG, -2, false);
        this.mHwHandlerThread.start();
        this.mHwHandler = new Handler(this.mHwHandlerThread.getLooper()) {
            /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass4 */

            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 23) {
                    boolean isInMultiWindowMode = ((Boolean) msg.obj).booleanValue();
                    synchronized (HwActivityTaskManagerServiceEx.this.mThirdPartyCallbackList) {
                        try {
                            int i2 = HwActivityTaskManagerServiceEx.this.mThirdPartyCallbackList.beginBroadcast();
                            Flog.i(100, "onMultiWindowModeChanged begin : mThirdPartyCallbackList size : " + i2);
                            while (i2 > 0) {
                                i2--;
                                try {
                                    HwActivityTaskManagerServiceEx.this.mThirdPartyCallbackList.getBroadcastItem(i2).onModeChanged(isInMultiWindowMode);
                                } catch (Exception e) {
                                    Flog.e(100, "Error in sending the Callback");
                                }
                            }
                            Flog.i(100, "onMultiWindowModeChanged end : mThirdPartyCallbackList size : " + i2);
                            HwActivityTaskManagerServiceEx.this.mThirdPartyCallbackList.finishBroadcast();
                        } catch (IllegalStateException e2) {
                            Flog.e(100, "beginBroadcast() called while already in a broadcast");
                        }
                    }
                } else if (i == 24) {
                    synchronized (HwActivityTaskManagerServiceEx.this.mActivityNotifiers) {
                        try {
                            Bundle bundle = (Bundle) msg.obj;
                            String userId = String.valueOf(bundle.getInt("android.intent.extra.user_handle"));
                            String reason = bundle.getString("android.intent.extra.REASON");
                            int i3 = HwActivityTaskManagerServiceEx.this.mActivityNotifiers.beginBroadcast();
                            while (i3 > 0) {
                                i3--;
                                IHwActivityNotifier notifier = HwActivityTaskManagerServiceEx.this.mActivityNotifiers.getBroadcastItem(i3);
                                HashMap<String, String> cookie = (HashMap) HwActivityTaskManagerServiceEx.this.mActivityNotifiers.getBroadcastCookie(i3);
                                if ((userId.equals(cookie.get("android.intent.extra.user_handle")) || cookie.get("android.intent.extra.USER") != null) && reason != null && reason.equals(cookie.get("android.intent.extra.REASON"))) {
                                    try {
                                        HwActivityTaskManagerServiceEx.this.mActivityNotifiers.getBroadcastItem(i3).call(bundle);
                                    } catch (Exception e3) {
                                        Flog.e(100, "observer.call get Exception, remove notifier " + notifier, e3);
                                        HwActivityTaskManagerServiceEx.this.mActivityNotifiers.unregister(notifier);
                                    }
                                }
                            }
                            HwActivityTaskManagerServiceEx.this.mActivityNotifiers.finishBroadcast();
                        } catch (Exception e4) {
                            Flog.e(100, "HwActivityNotifier call error");
                        }
                    }
                } else if (i == HwActivityTaskManagerServiceEx.NOTIFY_ACTIVITY_STATE) {
                    HwActivityTaskManagerServiceEx.this.handleNotifyActivityState(msg);
                } else if (i == 80) {
                    Slog.i(HwActivityTaskManagerServiceEx.TAG, "send UPDATE REMIND broacast to HWOUC");
                    Intent intent = new Intent(HwActivityTaskManagerServiceEx.ACTION_HWOUC_SHOW_UPGRADE_REMIND);
                    intent.setPackage(HwActivityTaskManagerServiceEx.PACKAGE_HWOUC);
                    HwActivityTaskManagerServiceEx.this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM, HwActivityTaskManagerServiceEx.PERMISSION_HWOUC_UPGRADE_REMIND);
                } else if (i == HwActivityTaskManagerServiceEx.NOTIFY_NEED_SWING_ROTATION) {
                    HwActivityTaskManagerServiceEx.this.handleAppNeedsSwingRotation(msg);
                } else if (i != 91) {
                    switch (i) {
                        case 48:
                            HwActivityTaskManagerServiceEx.this.showUninstallLauncher();
                            return;
                        case 49:
                            HwActivityTaskManagerServiceEx.this.showIncompatibleDialog();
                            return;
                        case 50:
                            HwActivityTaskManagerServiceEx.this.readIncompatibleXmlToHashMap();
                            return;
                        case 51:
                            HwActivityTaskManagerServiceEx.this.readNotRemindXmlToSet();
                            return;
                        case 52:
                            HwActivityTaskManagerServiceEx.this.writeNotRemindIncompatibleAppXml(HwActivityTaskManagerServiceEx.PRELOAD_APP_NOT_REMIND_XML);
                            return;
                        default:
                            return;
                    }
                } else {
                    HwActivityTaskManagerServiceEx.this.showExSplash(msg);
                }
            }
        };
        this.mThirdPartyCallbackList = new RemoteCallbackList<>();
        this.mHwTaskChangeNotificationController = new TaskChangeNotificationController(this.mIAtmsInner.getATMS().getGlobalLock(), this.mIAtmsInner.getStackSupervisor(), this.mHwHandler);
        if (SystemProperties.getInt("ro.config.gameassist", 0) == 1) {
            this.mHwGameAssistantController = new HwGameAssistantController(this.mContext);
        } else {
            this.mHwGameAssistantController = null;
        }
        initFingerBoostWhiteListData();
        this.mHwMwm = HwMultiWindowManager.getInstance(atms.getATMS());
    }

    public void onSystemReady() {
        this.mSettingsObserver = new SettingsObserver(this.mHwHandler);
        this.mSettingsObserver.init();
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            hwGameAssistantController.systemReady();
        }
        registerBroadcastReceiver();
        initTrustSpace();
        this.mFingerprintSettingsObserver = new FingerprintSettingsObserver();
        this.mFaceSettingsObserver = new FaceSettingsObserver();
        IntentFilter userSwitchedFilter = new IntentFilter();
        userSwitchedFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass5 */

            public void onReceive(Context context, Intent intent) {
                HwActivityTaskManagerServiceEx hwActivityTaskManagerServiceEx = HwActivityTaskManagerServiceEx.this;
                hwActivityTaskManagerServiceEx.updateUnlockBoostStatus(hwActivityTaskManagerServiceEx.mIAtmsInner.getATMS().getCurrentUserId());
            }
        }, userSwitchedFilter);
        this.mHwHandler.post(new Runnable() {
            /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass6 */

            public void run() {
                HwActivityTaskManagerServiceEx hwActivityTaskManagerServiceEx = HwActivityTaskManagerServiceEx.this;
                hwActivityTaskManagerServiceEx.updateUnlockBoostStatus(hwActivityTaskManagerServiceEx.mIAtmsInner.getATMS().getCurrentUserId());
            }
        });
        this.mHwMwm.onSystemReady();
        this.mIsMultiWindowDisabled = HwDeviceManager.disallowOp(54);
    }

    public void call(Bundle extras) {
        Message msg = this.mHwHandler.obtainMessage(24);
        msg.obj = extras;
        this.mHwHandler.sendMessage(msg);
    }

    public void registerHwActivityNotifier(IHwActivityNotifier notifier, String reason) {
        if (notifier != null && ACTIVITY_NOTIFIER_TYPES.contains(reason) && this.mContext.checkCallingOrSelfPermission("com.huawei.permission.ACTIVITY_NOTIFIER_PERMISSION") == 0) {
            Map<String, String> cookie = new HashMap<>();
            cookie.put("android.intent.extra.REASON", reason);
            cookie.put("android.intent.extra.user_handle", String.valueOf(Binder.getCallingUserHandle().getIdentifier()));
            if (!UserHandle.isApp(Binder.getCallingUid())) {
                Iterator<String> it = ONLY_NOTIFY_SYSTEM_USER_PROCESS.iterator();
                while (true) {
                    if (it.hasNext()) {
                        WindowProcessController wpc = this.mIAtmsInner.getATMS().getProcessController(it.next(), Binder.getCallingUid());
                        if (wpc != null && wpc.mPid == Binder.getCallingPid()) {
                            cookie.put("android.intent.extra.USER", String.valueOf(Binder.getCallingUserHandle().getIdentifier()));
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } else {
                WindowProcessController wpc2 = this.mIAtmsInner.getATMS().getProcessController(Binder.getCallingPid(), Binder.getCallingUid());
                if (wpc2 != null && ONLY_NOTIFY_SYSTEM_USER_PROCESS.contains(wpc2.mInfo.packageName)) {
                    cookie.put("android.intent.extra.USER", String.valueOf(Binder.getCallingUserHandle().getIdentifier()));
                }
            }
            if (Binder.getCallingPid() == Process.myPid()) {
                cookie.put("android.intent.extra.USER", String.valueOf(Binder.getCallingUserHandle().getIdentifier()));
            }
            synchronized (this.mActivityNotifiers) {
                Slog.i(TAG, "registerHwActivityNotifier notifier: " + notifier + ", reason: " + reason + ", callingUid: " + Binder.getCallingUid() + " callingPid: " + Binder.getCallingPid());
                this.mActivityNotifiers.register(notifier, cookie);
            }
        }
    }

    public void unregisterHwActivityNotifier(IHwActivityNotifier notifier) {
        if (notifier != null && this.mContext.checkCallingOrSelfPermission("com.huawei.permission.ACTIVITY_NOTIFIER_PERMISSION") == 0) {
            synchronized (this.mActivityNotifiers) {
                Slog.i(TAG, "unregisterHwActivityNotifier notifier: " + notifier + ", callingUid: " + Binder.getCallingUid() + " callingPid: " + Binder.getCallingPid());
                this.mActivityNotifiers.unregister(notifier);
            }
        }
    }

    public boolean requestContentNode(ComponentName componentName, Bundle data, int token) {
        this.mIAtmsInner.getATMS().mAmInternal.enforceCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "requestContentNode()");
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            ActivityRecord realActivity = getTopActivityAppToken(componentName, this.mIAtmsInner.getATMS().getTopDisplayFocusedStack().getTopActivity());
            if (realActivity == null || realActivity.app == null || realActivity.app.mThread == null) {
                Slog.w(TAG, "requestContentNode failed! ");
                return false;
            }
            try {
                realActivity.app.mThread.requestContentNode(realActivity.appToken, data, token);
                return true;
            } catch (RemoteException e) {
                Slog.w(TAG, "requestContentNode failed: crash calling " + realActivity);
                return false;
            }
        }
    }

    public boolean requestContentOther(ComponentName componentName, Bundle data, int token) {
        this.mIAtmsInner.getATMS().mAmInternal.enforceCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "requestContentOther()");
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            ActivityRecord realActivity = getTopActivityAppToken(componentName, this.mIAtmsInner.getATMS().getTopDisplayFocusedStack().getTopActivity());
            if (realActivity == null || realActivity.app == null || realActivity.app.mThread == null) {
                return false;
            }
            try {
                realActivity.app.mThread.requestContentOther(realActivity.appToken, data, token);
                return true;
            } catch (RemoteException e) {
                Slog.w(TAG, "requestContentOther failed: crash calling " + realActivity);
                return false;
            }
        }
    }

    public void setResumedActivityUncheckLocked(ActivityRecord from, ActivityRecord to) {
        ActivityDisplay display;
        ActivityRecord topActivity;
        if (HW_SUPPORT_LAUNCHER_EXIT_ANIM && from != null && to != null && from != to && to.isActivityTypeHome() && "com.huawei.android.launcher".equals(to.packageName)) {
            String packageName = from.packageName;
            TaskRecord tr = from.getTaskRecord();
            if (HwActivityStartInterceptor.isAppLockPackageName(packageName) && HwActivityStartInterceptor.isAppLockAction(from.intent.getAction())) {
                packageName = from.intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            } else if (!(tr == null || tr.getRootActivity() == null)) {
                packageName = tr.getRootActivity().packageName;
            }
            Slog.w(TAG, "setResumedActivityUncheckLocked start call, from: " + from + ", to: " + to);
            Bundle bundle = new Bundle();
            bundle.putString(STRING_INCOMPATIBLE_XML_PKG, packageName);
            bundle.putString("topPackage", from.packageName);
            bundle.putBoolean("isTransluent", from.isTransluent());
            bundle.putInt(PermConst.USER_ID, from.mUserId);
            bundle.putString("android.intent.extra.REASON", "returnToHome");
            bundle.putInt("android.intent.extra.user_handle", to.mUserId);
            bundle.putInt("windowingMode", from.getWindowingMode());
            Message msg = this.mHwHandler.obtainMessage(24);
            msg.obj = bundle;
            this.mHwHandler.sendMessageAtFrontOfQueue(msg);
            if (HW_SNAPSHOT && from.getConfiguration().orientation == 2 && from.appToken != null) {
                Slog.v(TAG, "takeTaskSnapShot package " + from.packageName);
                this.mIAtmsInner.getATMS().mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(from.appToken, false);
            }
        }
        if (to != null && (from == null || !from.packageName.equals(to.packageName) || from.getUid() != to.getUid())) {
            String str = null;
            LogPower.push(230, from == null ? null : from.packageName, to.packageName);
            StringBuilder sb = new StringBuilder();
            sb.append("appSwitch from: ");
            if (from != null) {
                str = from.packageName;
            }
            sb.append(str);
            sb.append(" to: ");
            sb.append(to.packageName);
            Slog.w(TAG, sb.toString());
            Bundle bundle2 = new Bundle();
            if (from != null) {
                bundle2.putString("fromPackage", from.packageName);
                bundle2.putInt("fromUid", from.getUid());
            }
            bundle2.putString("toPackage", to.packageName);
            bundle2.putInt("toUid", to.getUid());
            bundle2.putString("android.intent.extra.REASON", "appSwitch");
            bundle2.putInt("android.intent.extra.user_handle", this.mIAtmsInner.getATMS().mWindowManager.mCurrentUserId);
            TaskRecord task = to.getTaskRecord();
            if (task != null) {
                bundle2.putInt("toTaskId", task.taskId);
                bundle2.putBoolean("toTaskInMultiWindowMode", task.inMultiWindowMode());
            }
            bundle2.putParcelable("toActivity", to.info.getComponentName());
            bundle2.putInt("toPid", to.app != null ? to.app.mPid : 0);
            bundle2.putInt("windowingMode", to.getWindowingMode());
            bundle2.putString("toProcessName", to.processName);
            Message msg2 = this.mHwHandler.obtainMessage(24);
            msg2.obj = bundle2;
            this.mHwHandler.sendMessage(msg2);
            if (DisplayRotation.IS_SWING_ENABLED) {
                Bundle swingBundle = new Bundle();
                swingBundle.putString("PackageName", to.packageName);
                Message swingMsg = this.mHwHandler.obtainMessage(NOTIFY_NEED_SWING_ROTATION);
                swingMsg.obj = swingBundle;
                this.mHwHandler.sendMessage(swingMsg);
            }
            if (from != null && from.inHwMultiStackWindowingMode() && !to.inMultiWindowMode() && from.getActivityStack() != to.getActivityStack() && (display = from.getDisplay()) != null) {
                for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    ActivityStack stack = display.getChildAt(stackNdx);
                    if (!stack.inMultiWindowMode() && stack != to.getActivityStack()) {
                        break;
                    }
                    if (stack.inHwMultiStackWindowingMode() && !stack.isAlwaysOnTop() && (topActivity = stack.getTopActivity()) != null && topActivity.visible && topActivity.appToken != null) {
                        Slog.v(TAG, "takeTaskSnapShot multiwindow package " + topActivity.packageName);
                        this.mIAtmsInner.getATMS().mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topActivity.appToken, false);
                    }
                }
            }
        }
        this.mPreviousResumedActivity = from;
    }

    public void dispatchActivityLifeState(ActivityRecord r, String state) {
        if (r != null) {
            TaskRecord task = r.task;
            if (mTranslucentWhitelists.contains(r.info.name) && task != null) {
                int i = task.mActivities.size() - 1;
                while (true) {
                    if (i >= 0) {
                        ActivityRecord activityRecord = (ActivityRecord) task.mActivities.get(i);
                        if (activityRecord != null && !activityRecord.finishing && activityRecord != r) {
                            r = activityRecord;
                            break;
                        }
                        i--;
                    } else {
                        break;
                    }
                }
            }
            Bundle bundle = new Bundle();
            if (r.app != null) {
                bundle.putInt("uid", r.app.mUid);
                bundle.putInt("pid", r.app.mPid);
            }
            ComponentName comp = r.info.getComponentName();
            Rect bounds = r.getBounds();
            int displayMode = 0;
            if (HwFoldScreenState.isFoldScreenDevice()) {
                displayMode = ((HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class)).getDisplayMode();
                bundle.putInt("displayMode", displayMode);
            }
            if (displayMode != 1 && (r.maxAspectRatio <= 0.0f || r.maxAspectRatio >= mDeviceMaxRatio || mWhitelistActivities.contains(comp.getClassName()))) {
                bounds = new Rect();
            } else if (displayMode == 1 && (r.maxAspectRatio <= 0.0f || ((double) Math.abs(r.maxAspectRatio - HwFoldScreenState.getScreenFoldFullRatio())) < 1.0E-8d)) {
                bounds = new Rect();
            } else if (r.inMultiWindowMode()) {
                bounds = new Rect();
            }
            bundle.putBoolean("isTop", r == this.mIAtmsInner.getLastResumedActivityRecord());
            bundle.putString(SceneRecogFeature.DATA_STATE, state);
            bundle.putParcelable(SceneRecogFeature.DATA_COMP, comp);
            bundle.putParcelable("bounds", bounds);
            bundle.putBoolean("isFloating", mPIPWhitelists.contains(comp.getClassName()) || r.isFloating());
            bundle.putFloat("maxRatio", r.maxAspectRatio);
            bundle.putBoolean("isHomeActivity", r.isActivityTypeHome());
            bundle.putString("android.intent.extra.REASON", SceneRecogFeature.REASON_INFO);
            bundle.putInt("android.intent.extra.user_handle", this.mIAtmsInner.getATMS().mWindowManager.mCurrentUserId);
            Message msg = this.mHwHandler.obtainMessage(24);
            msg.obj = bundle;
            this.mHwHandler.sendMessage(msg);
            if (SceneRecogFeature.EVENT_RESUME.equals(state) && HwMwUtils.ENABLED) {
                HwMwUtils.performPolicy(51, new Object[]{r.appToken, comp.getPackageName()});
            }
        }
    }

    public HashMap<String, Integer> getPkgDisplayMaps() {
        return this.mPkgDisplayMaps;
    }

    public int canAppBoost(ActivityInfo aInfo, boolean isScreenOn) {
        String packageName;
        if (isScreenOn || aInfo == null) {
            return 1;
        }
        int type = MultiTaskUtils.getAppType(-1, aInfo.applicationInfo);
        if (type == 4) {
            return 0;
        }
        if (type == 1 || (packageName = aInfo.packageName) == null || packageName.startsWith("com.huawei")) {
            return 1;
        }
        return 0;
    }

    public boolean isTaskSupportResize(int taskId, boolean isFullscreen, boolean isMaximized) {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        if (!HwPCUtils.isPcCastModeInServer() || (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx(this.mIAtmsInner.getATMS()))) == null || !(this.mIAtmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor)) {
            return false;
        }
        RootActivityContainer rootActivityContainer = this.mIAtmsInner.getRootActivityContainer();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                TaskRecord task = rootActivityContainer.anyTaskForId(taskId);
                if (task == null) {
                    return false;
                }
                boolean isSupportResize = multiWindowMgr.isSupportResize(buildTaskRecordEx(task), isFullscreen, isMaximized);
                Binder.restoreCallingIdentity(origId);
                return isSupportResize;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getTopTaskIdInDisplay(int displayId, String pkgName, boolean invisibleAlso) {
        if (displayId == 0 || !HwPCUtils.isPcCastModeInServer() || HwPCUtils.isValidExtDisplayId(displayId)) {
            int N = this.mIAtmsInner.getATMS().mRootActivityContainer.getChildCount();
            if (Log.HWINFO) {
                HwPCUtils.log(TAG, "getTopTaskIdInDisplay displayId = " + displayId + ", N = " + N + ", pkgName = " + pkgName);
            }
            if (displayId < 0) {
                return -1;
            }
            ActivityDisplay activityDisplay = getPCActivityDisplay(displayId);
            if (activityDisplay == null) {
                HwPCUtils.log(TAG, "getTopTaskIdInDisplay activityDisplay not exist");
                return -1;
            }
            for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                int taskId = getSpecialTaskId(displayId == 0, activityDisplay.getChildAt(stackNdx), pkgName, invisibleAlso);
                if (taskId != -1) {
                    return taskId;
                }
            }
            return -1;
        }
        Slog.e(TAG, "is not a valid pc display id");
        return -1;
    }

    private ActivityDisplay getPCActivityDisplay(int displayId) {
        RootActivityContainer rootActivityContainer = this.mIAtmsInner.getATMS().mRootActivityContainer;
        if (rootActivityContainer == null) {
            return null;
        }
        for (int i = rootActivityContainer.getChildCount() - 1; i >= 0; i--) {
            if (rootActivityContainer.getChildAt(i).mDisplayId == displayId) {
                return rootActivityContainer.getChildAt(i);
            }
        }
        return null;
    }

    public Rect getPCTopTaskBounds(int displayId) {
        if (!HwPCUtils.isPcCastModeInServer() || HwPCUtils.isValidExtDisplayId(displayId)) {
            HwPCUtils.log(TAG, "getPCTopTaskBounds displayId = " + displayId + ", N = " + this.mIAtmsInner.getATMS().mRootActivityContainer.getChildCount());
            if (displayId < 0) {
                return null;
            }
            ActivityDisplay activityDisplay = getPCActivityDisplay(displayId);
            if (activityDisplay == null) {
                HwPCUtils.log(TAG, "getPCTopTaskBounds activityDisplay not exist");
                return null;
            }
            Rect rect = new Rect();
            int stackNdx = activityDisplay.getChildCount() - 1;
            while (stackNdx >= 0) {
                TaskRecord tr = activityDisplay.getChildAt(stackNdx).topTask();
                if (tr == null || !tr.isVisible()) {
                    stackNdx--;
                } else {
                    tr.getWindowContainerBounds(rect);
                    HwPCUtils.log(TAG, "getTaskIdInPCDisplayLocked tr.taskId = " + tr.taskId + ", rect = " + rect);
                    return rect;
                }
            }
            return null;
        }
        Slog.e(TAG, "is not a valid pc display id");
        return null;
    }

    private int getSpecialTaskId(boolean isDefaultDisplay, ActivityStack stack, String pkgName, boolean invisibleAlso) {
        if (pkgName != null && !"".equals(pkgName)) {
            ArrayList<TaskRecord> tasks = stack.getAllTasks();
            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                TaskRecord tr = tasks.get(taskNdx);
                if (tr != null && (invisibleAlso || tr.isVisible())) {
                    ActivityRecord[] candicatedArs = {tr.topRunningActivityLocked(), tr.getRootActivity()};
                    for (ActivityRecord ar : candicatedArs) {
                        HwPCUtils.log(TAG, "getSpecialTaskId ar = " + ar + ", tr.isVisible() = " + tr.isVisible());
                        if (ar != null && ar.packageName != null && ar.packageName.equals(pkgName)) {
                            return tr.taskId;
                        }
                    }
                    continue;
                }
            }
            return -1;
        } else if (!isDefaultDisplay) {
            TaskRecord tr2 = stack.topTask();
            if (tr2 == null) {
                return -1;
            }
            if (!invisibleAlso && !tr2.isVisible()) {
                return -1;
            }
            HwPCUtils.log(TAG, "getSpecialTaskId tr.taskId = " + tr2.taskId);
            return tr2.taskId;
        } else {
            ArrayList<TaskRecord> tasks2 = stack.getAllTasks();
            for (int taskNdx2 = tasks2.size() - 1; taskNdx2 >= 0; taskNdx2--) {
                TaskRecord tr3 = tasks2.get(taskNdx2);
                if (tr3 != null && (invisibleAlso || tr3.isVisible())) {
                    return tr3.taskId;
                }
            }
            return -1;
        }
    }

    public void noteActivityStart(String packageName, String processName, String activityName, int pid, int uid, boolean started) {
        WindowProcessController app;
        if (this.mIAtmsInner.getSystemReady()) {
            if (pid < 1 && (app = this.mIAtmsInner.getProcessControllerForHwAtmsEx(processName, uid)) != null) {
                pid = app.getPid();
            }
            this.mIAtmsInner.getAtmDAMonitor().noteActivityStart(packageName, processName, activityName, pid, uid, started);
            if (!started) {
                HwApsImpl.notifyActivityIdle(packageName, processName, activityName);
            }
        }
    }

    public boolean noteActivityInitializing(ActivityRecord startActivity, ActivityRecord reusedActivity) {
        WindowProcessController app;
        TaskRecord taskRecord;
        boolean notReady = !this.mIAtmsInner.getSystemReady() || startActivity == null || startActivity.info == null;
        if (HwMwUtils.ENABLED && reusedActivity != null) {
            HwMwUtils.performPolicy((int) CPUFeature.MSG_RESET_BOOST_CPUS, new Object[]{reusedActivity});
        }
        if (notReady) {
            return false;
        }
        if ((reusedActivity != null && ((taskRecord = reusedActivity.task) == null || taskRecord.realActivity == null || taskRecord.realActivity.equals(startActivity.mActivityComponent) || taskRecord.findActivityInHistoryLocked(startActivity) != null)) || (app = this.mIAtmsInner.getProcessControllerForHwAtmsEx(startActivity.processName, startActivity.getUid())) == null || app.getPid() <= 0) {
            return false;
        }
        this.mIAtmsInner.getAtmDAMonitor().noteActivityDisplayed(startActivity.shortComponentName, startActivity.getUid(), app.getPid(), true);
        return true;
    }

    public void noteActivityDisplayed(String componentName, int uid, int pid, boolean isStart) {
        if (this.mIAtmsInner.getSystemReady()) {
            this.mIAtmsInner.getAtmDAMonitor().noteActivityDisplayed(componentName, uid, pid, isStart);
        }
    }

    public boolean isInMultiWindowMode() {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                ActivityStack focusedStack = this.mIAtmsInner.getRootActivityContainer().getTopDisplayFocusedStack();
                boolean z = false;
                if (focusedStack == null) {
                    return false;
                }
                if (focusedStack.inMultiWindowMode() && !focusedStack.inHwMagicWindowingMode()) {
                    z = true;
                }
                Binder.restoreCallingIdentity(origId);
                return z;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (this.isLastMultiMode != isInMultiWindowMode) {
            this.isLastMultiMode = isInMultiWindowMode;
            Message msg = this.mHwHandler.obtainMessage(23);
            msg.obj = Boolean.valueOf(isInMultiWindowMode);
            this.mHwHandler.sendMessage(msg);
        }
    }

    public boolean registerThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) {
        boolean lRegistered = false;
        if (aCallBackHandler != null) {
            synchronized (this.mThirdPartyCallbackList) {
                lRegistered = this.mThirdPartyCallbackList.register(aCallBackHandler);
            }
        }
        return lRegistered;
    }

    public boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) {
        boolean lUnregistered = false;
        if (aCallBackHandler != null) {
            synchronized (this.mThirdPartyCallbackList) {
                lUnregistered = this.mThirdPartyCallbackList.unregister(aCallBackHandler);
            }
        }
        return lUnregistered;
    }

    public boolean isTaskVisible(int id) {
        ActivityStackSupervisor mStackSupervisor;
        TaskRecord tr;
        int callerUid = Binder.getCallingUid();
        if (callerUid == 1000) {
            IHwActivityTaskManagerInner iHwActivityTaskManagerInner = this.mIAtmsInner;
            if (iHwActivityTaskManagerInner == null || (mStackSupervisor = iHwActivityTaskManagerInner.getStackSupervisor()) == null || (tr = mStackSupervisor.mRootActivityContainer.anyTaskForId(id)) == null || tr.getTopActivity() == null || !tr.getTopActivity().visible) {
                return false;
            }
            return true;
        }
        throw new SecurityException("Process with uid=" + callerUid + " cannot call function isTaskVisible.");
    }

    private void initTrustSpace() {
        this.mTrustSpaceController = HwServiceFactory.getTrustSpaceController();
        ITrustSpaceController iTrustSpaceController = this.mTrustSpaceController;
        if (iTrustSpaceController != null) {
            iTrustSpaceController.initTrustSpace();
        }
    }

    private boolean shouldPreventStartComponent(int type, String calleePackage, int callerUid, int callerPid, String callerPackage, int userId) {
        boolean shouldPrevent = false;
        if (this.mIAtmsInner.getSystemReady()) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (this.mTrustSpaceController != null) {
                    shouldPrevent = this.mTrustSpaceController.checkIntent(type, calleePackage, callerUid, callerPid, callerPackage, userId);
                }
                ISecurityProfileController spc = HwServiceFactory.getSecurityProfileController();
                if (spc != null) {
                    try {
                    } catch (Throwable th) {
                        spc = th;
                        Binder.restoreCallingIdentity(ident);
                        throw spc;
                    }
                    try {
                        shouldPrevent |= spc.shouldPreventInteraction(type, calleePackage, new IntentCaller(callerPackage, callerUid, callerPid), userId);
                    } catch (Throwable th2) {
                        spc = th2;
                        Binder.restoreCallingIdentity(ident);
                        throw spc;
                    }
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th3) {
                spc = th3;
                Binder.restoreCallingIdentity(ident);
                throw spc;
            }
        }
        return shouldPrevent;
    }

    public boolean shouldPreventStartService(ServiceInfo sInfo, int callerUid, int callerPid, String callerPackage, int userId) {
        if (sInfo == null) {
            return false;
        }
        return shouldPreventStartComponent(2, sInfo.applicationInfo.packageName, callerUid, callerPid, callerPackage, userId);
    }

    private boolean iAwareShouldPreventActivity(Intent intent, ActivityInfo aInfo, int callerPid, int callerUid, WindowProcessController callerApp) {
        if (intent == null || aInfo == null) {
            return false;
        }
        if (this.mIAtmsInner.getATMS().isSleepingLocked() && sPreventStartWhenSleeping.contains(aInfo.getComponentName().flattenToShortString())) {
            return true;
        }
        AwareAppStartupPolicy appStartupPolicy = AwareAppStartupPolicy.self();
        if (appStartupPolicy == null) {
            return false;
        }
        boolean shouldPrevent = appStartupPolicy.shouldPreventStartActivity(intent, aInfo, callerPid, callerUid, callerApp);
        if (!shouldPrevent) {
            return AwareFakeActivityRecg.self().shouldPreventStartActivity(aInfo, callerPid, callerUid);
        }
        return shouldPrevent;
    }

    private boolean isInstall(Intent intent) {
        ComponentName componentName;
        if (intent == null) {
            return false;
        }
        String action = intent.getAction();
        String type = intent.getType();
        boolean story = false;
        if ("android.intent.action.INSTALL_PACKAGE".equals(action)) {
            story = true;
        }
        if ("application/vnd.android.package-archive".equals(type)) {
            story = true;
        }
        if (!"android.intent.action.VIEW".equals(action) || (componentName = intent.getComponent()) == null || !"com.android.packageinstaller".equals(componentName.getPackageName())) {
            return story;
        }
        return true;
    }

    private boolean mdmShouldPreventStartActivity(Intent intent, WindowProcessController callerApp) {
        String callerPackage = null;
        if (callerApp != null) {
            callerPackage = callerApp.mInfo.packageName;
        }
        if (callerPackage == null) {
            return false;
        }
        String callerIndex = null;
        if (!isInstall(intent)) {
            return false;
        }
        try {
            callerIndex = intent.getStringExtra("caller_package");
        } catch (Exception e) {
            Slog.e(TAG, "mdmShouldPreventStartActivity, Get package info faild catch Exception");
        }
        if (callerIndex != null) {
            callerPackage = callerIndex;
        }
        intent.putExtra("caller_package", callerPackage);
        if (!HwDeviceManager.disallowOp(intent)) {
            return false;
        }
        Slog.i(TAG, "due to disallow op launching activity aborted");
        this.mIAtmsInner.getATMS().mUiHandler.post(new Runnable() {
            /* class com.android.server.wm.$$Lambda$HwActivityTaskManagerServiceEx$8ujufDQT1riML7UgdA_JrnA7PU */

            public final void run() {
                HwActivityTaskManagerServiceEx.this.lambda$mdmShouldPreventStartActivity$0$HwActivityTaskManagerServiceEx();
            }
        });
        return true;
    }

    public /* synthetic */ void lambda$mdmShouldPreventStartActivity$0$HwActivityTaskManagerServiceEx() {
        Context context = this.mContext;
        if (context != null) {
            Toast toast = Toast.makeText(context, context.getString(33685986), 0);
            toast.getWindowParams().privateFlags |= 16;
            toast.show();
        }
    }

    public boolean shouldPreventStartActivity(ActivityInfo aInfo, int callerUid, int callerPid, String callerPackage, int userId, Intent intent, WindowProcessController callerApp) {
        if (aInfo == null) {
            return false;
        }
        if (!iAwareShouldPreventActivity(intent, aInfo, callerPid, callerUid, callerApp) && !mdmShouldPreventStartActivity(intent, callerApp) && !customActivityStarting(intent, aInfo.applicationInfo.packageName)) {
            return shouldPreventStartComponent(0, aInfo.applicationInfo.packageName, callerUid, callerPid, callerPackage, userId);
        }
        return true;
    }

    public boolean shouldPreventStartProvider(ProviderInfo cpi, int callerUid, int callerPid, String callerPackage, int userId) {
        if (cpi == null) {
            return false;
        }
        return shouldPreventStartComponent(3, cpi.packageName, callerUid, callerPid, callerPackage, userId);
    }

    public boolean shouldPreventSendBroadcast(Intent intent, String receiver, int callerUid, int callerPid, String callingPackage, int userId) {
        return shouldPreventStartComponent(1, receiver, callerUid, callerPid, callingPackage, userId);
    }

    public void dismissSplitScreenModeWithFinish(ActivityRecord r) {
        ActivityRecord nextTargetAR;
        if (r.getWindowingMode() == 4 && r.getActivityType() == 1) {
            ActivityStackSupervisor mStackSupervisor = this.mIAtmsInner.getStackSupervisor();
            RootActivityContainer mRootActivityContainer = this.mIAtmsInner.getRootActivityContainer();
            if (mStackSupervisor == null) {
                Slog.w(TAG, "dismissSplitScreenModeWithFinish:mStackSupervisor not found.");
            } else if (r.info.name.contains(SPLIT_SCREEN_APP_NAME)) {
                dismissSplitScreenToPrimaryStack(mRootActivityContainer);
            } else {
                ActivityStack nextTargetAs = mStackSupervisor.getNextStackInSplitSecondary(r.getActivityStack());
                if (nextTargetAs != null && (nextTargetAR = nextTargetAs.topRunningActivityLocked()) != null && nextTargetAR.info.name.contains(SPLIT_SCREEN_APP_NAME)) {
                    dismissSplitScreenToPrimaryStack(mRootActivityContainer);
                }
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private static final String KEY_HW_UPGRADE_REMIND = "hw_upgrade_remind";
        private final Uri URI_HW_UPGRADE_REMIND = Settings.Secure.getUriFor(KEY_HW_UPGRADE_REMIND);

        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void init() {
            ContentResolver resolver = HwActivityTaskManagerServiceEx.this.mContext.getContentResolver();
            boolean z = false;
            resolver.registerContentObserver(this.URI_HW_UPGRADE_REMIND, false, this, 0);
            HwActivityTaskManagerServiceEx hwActivityTaskManagerServiceEx = HwActivityTaskManagerServiceEx.this;
            if (Settings.Secure.getIntForUser(resolver, KEY_HW_UPGRADE_REMIND, 0, 0) != 0) {
                z = true;
            }
            boolean unused = hwActivityTaskManagerServiceEx.mNeedRemindHwOUC = z;
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (this.URI_HW_UPGRADE_REMIND.equals(uri)) {
                HwActivityTaskManagerServiceEx hwActivityTaskManagerServiceEx = HwActivityTaskManagerServiceEx.this;
                boolean z = false;
                if (Settings.Secure.getIntForUser(hwActivityTaskManagerServiceEx.mContext.getContentResolver(), KEY_HW_UPGRADE_REMIND, 0, 0) != 0) {
                    z = true;
                }
                boolean unused = hwActivityTaskManagerServiceEx.mNeedRemindHwOUC = z;
                Slog.i(HwActivityTaskManagerServiceEx.TAG, "mNeedRemindHwOUC has changed to : " + HwActivityTaskManagerServiceEx.this.mNeedRemindHwOUC);
            }
        }
    }

    private void dismissSplitScreenToPrimaryStack(RootActivityContainer mRootActivityContainer) {
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityStack stack = mRootActivityContainer.getDefaultDisplay().getSplitScreenPrimaryStack();
            if (stack == null) {
                Slog.w(TAG, "dismissSplitScreenToPrimaryStack: primary split-screen stack not found.");
                return;
            }
            this.mIAtmsInner.getATMS().mWindowManager.mShouldResetTime = true;
            this.mIAtmsInner.getATMS().mWindowManager.startFreezingScreen(0, 0);
            stack.moveToFront("dismissSplitScreenToPrimaryStack");
            stack.setWindowingMode(1);
            this.mIAtmsInner.getATMS().mWindowManager.stopFreezingScreen();
            Binder.restoreCallingIdentity(ident);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void notifyActivityState(ActivityRecord r, String state) {
        Message msg = this.mHwHandler.obtainMessage(NOTIFY_ACTIVITY_STATE);
        String activityInfo = parseActivityStateInfo(r, state);
        if (activityInfo == null) {
            Slog.e(TAG, "parse activity info error.");
            return;
        }
        msg.obj = activityInfo;
        this.mHwHandler.sendMessage(msg);
        if (this.mNeedRemindHwOUC && r.mUserId == 0 && r.isActivityTypeHome() && state.equals(ActivityStack.ActivityState.RESUMED.toString())) {
            this.mHwHandler.removeMessages(80);
            this.mHwHandler.sendEmptyMessage(80);
        }
    }

    private String parseActivityStateInfo(ActivityRecord r, String state) {
        if (r == null || state == null) {
            Slog.e(TAG, "invalid input param, error.");
            return null;
        } else if (r.packageName == null || r.shortComponentName == null || r.app == null || r.appInfo == null || r.appInfo.uid <= 1000) {
            Slog.e(TAG, "invalid ActivityRecord, error.");
            return null;
        } else {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(r.packageName);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.shortComponentName);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.appInfo.uid);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.app.mPid);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(state);
            return stringBuffer.toString();
        }
    }

    /* access modifiers changed from: private */
    public void handleAppNeedsSwingRotation(Message msg) {
        if (msg != null) {
            Bundle bundle = (Bundle) msg.obj;
            String packageName = "";
            if (bundle != null) {
                packageName = bundle.getString("PackageName", "");
            }
            if (packageName != null && !packageName.isEmpty()) {
                int type = AppTypeRecoManager.getInstance().getAppType(packageName);
                Slog.v(TAG, "handleAppNeedsSwingRotation packageName: " + packageName + " type: " + type);
                DisplayRotation displayRotation = this.mIAtmsInner.getATMS().mWindowManager.getDefaultDisplayContentLocked().getDisplayRotation();
                if (type == 3 || type == 9) {
                    displayRotation.setSwingDisabled(true);
                } else {
                    displayRotation.setSwingDisabled(false);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public final void handleNotifyActivityState(Message msg) {
        if (msg != null) {
            String activityInfo = null;
            if (msg.obj instanceof String) {
                activityInfo = (String) msg.obj;
            }
            if (activityInfo == null) {
                Slog.e(TAG, "msg.obj type error.");
                return;
            }
            IHwActivityTaskManagerInner iHwActivityTaskManagerInner = this.mIAtmsInner;
            if (iHwActivityTaskManagerInner != null && iHwActivityTaskManagerInner.getAtmDAMonitor() != null) {
                this.mIAtmsInner.getAtmDAMonitor().notifyActivityState(activityInfo);
            }
        }
    }

    private Intent slideGetDefaultIntent() {
        Intent intent = new Intent();
        intent.setPackage("com.android.settings");
        intent.setAction("android.intent.action.MAIN");
        intent.setClassName("com.android.settings", "com.android.settings.accessibility.FirstSlideCoverDialogActivity");
        return intent;
    }

    private boolean isSupportKeyguardQuickCamera(ContentResolver resolver, int uid) {
        return Settings.Secure.getIntForUser(resolver, "keyguard_slide_open_camera_state", -1, uid) == 1;
    }

    private Intent slideGetIntentFromSetting(boolean isSecure) {
        String keyStr = isSecure ? "quick_slide_app_db_secure" : "quick_slide_app_db";
        ContentResolver resolver = this.mContext.getContentResolver();
        int uid = this.mIAtmsInner.getATMS().getCurrentUserId();
        if (!isSecure || isSupportKeyguardQuickCamera(resolver, uid)) {
            String intentStr = Settings.Secure.getStringForUser(resolver, keyStr, uid);
            if (intentStr == null) {
                return null;
            }
            if (intentStr.equals("first_slide")) {
                return slideGetDefaultIntent();
            }
            try {
                return Intent.parseUri(intentStr, 0);
            } catch (Exception e) {
                Slog.e(TAG, "startActivity get intent err : " + intentStr);
                return null;
            }
        } else {
            Slog.v(TAG, "slideGetIntentFromSetting skipped as not support");
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void slideOpenStartActivity() {
        boolean isSecure = this.mIAtmsInner.getATMS().isSleepingLocked();
        if (!isSecure || !this.mIAtmsInner.getATMS().mWindowManager.mPolicy.isKeyguardOccluded()) {
            Intent intent = slideGetIntentFromSetting(isSecure);
            if (intent == null) {
                Slog.i(TAG, "slideOpenStartActivity get intent is null, return!");
                return;
            }
            this.mQuickSlideIntent = intent;
            this.mQuickSlideStartTime = SystemClock.uptimeMillis();
            ActivityRecord lastResumedActivity = this.mIAtmsInner.getLastResumedActivityRecord();
            String lastResumedPkg = lastResumedActivity != null ? lastResumedActivity.packageName : null;
            Context context = this.mContext;
            Flog.bdReport(context, (int) PPPOEStateMachine.PPPOE_EVENT_CODE, "{curPkgName:" + lastResumedPkg + ",startPkgName:" + intent.getPackage() + "}");
            StringBuilder sb = new StringBuilder();
            sb.append("slideOpenStartActivity lastResumedPkg:");
            sb.append(lastResumedPkg);
            sb.append(", startPkgName:");
            sb.append(intent.getPackage());
            Slog.i(TAG, sb.toString());
            if (intent.getPackage() == null || intent.getPackage().equals("no_set") || (lastResumedActivity != null && lastResumedActivity.visible && !isSecure && !intent.getPackage().equals("com.android.settings") && lastResumedActivity.packageName.equals(intent.getPackage()))) {
                Slog.i(TAG, "no_set or has been started, need not start activity! sleep " + isSecure);
                return;
            }
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return;
        }
        Slog.i(TAG, "slideOpenStartActivity skip as occluded, return!");
    }

    /* access modifiers changed from: private */
    public void slideCloseMoveActivityToBack() {
        Intent intent = this.mQuickSlideIntent;
        String pkgName = intent != null ? intent.getPackage() : null;
        long curTime = SystemClock.uptimeMillis();
        long j = this.mQuickSlideStartTime;
        long durTime = (j == 0 || curTime < j) ? 0 : curTime - j;
        Context context = this.mContext;
        Flog.bdReport(context, 653, "{durTime:" + durTime + ",pkgName:" + pkgName + "}");
        StringBuilder sb = new StringBuilder();
        sb.append("slideCloseMoveActivityToBack durTime:");
        sb.append(durTime);
        sb.append(", pkgName:");
        sb.append(pkgName);
        Slog.i(TAG, sb.toString());
        this.mQuickSlideIntent = null;
        this.mQuickSlideStartTime = 0;
    }

    /* access modifiers changed from: private */
    public void registerHallCallback() {
        if (!new CoverManager().registerHallCallback(AppStartupDataMgr.HWPUSH_PKGNAME, 1, new IHallCallback.Stub() {
            /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass7 */

            public void onStateChange(HallState hallState) {
                long ident = Binder.clearCallingIdentity();
                try {
                    if (hallState.state == 2) {
                        HwActivityTaskManagerServiceEx.this.slideOpenStartActivity();
                    } else if (hallState.state == 0) {
                        HwActivityTaskManagerServiceEx.this.slideCloseMoveActivityToBack();
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        })) {
            Slog.i(TAG, "registerHallCallback err!");
        }
    }

    public void registerBroadcastReceiver() {
        if ((SystemProperties.getInt("ro.config.hw_hall_prop", 0) & 1) != 0) {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass8 */

                public void onReceive(Context context, Intent intent) {
                    if (intent != null && HwActivityTaskManagerServiceEx.HW_SYSTEM_SERVER_START.equals(intent.getAction())) {
                        Slog.i(HwActivityTaskManagerServiceEx.TAG, "registerBroadcastReceiver");
                        HwActivityTaskManagerServiceEx.this.registerHallCallback();
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(HW_SYSTEM_SERVER_START);
            this.mContext.registerReceiverAsUser(receiver, UserHandle.ALL, filter, BOOT_PERMISSION, null);
        }
    }

    public void hwRestoreTask(int taskId, float xPos, float yPos) {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        if (HwPCUtils.isPcCastModeInServer() && (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx(this.mIAtmsInner.getATMS()))) != null) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                    TaskRecord tr = this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId);
                    if (tr != null) {
                        if (!HwPCMultiWindowCompatibility.isRestorable(tr.mWindowState)) {
                            Binder.restoreCallingIdentity(origId);
                            return;
                        }
                        if (tr.getStack() != null) {
                            tr.getStack().mHwActivityStackEx.resetOtherStacksVisible(true);
                        }
                        Rect rect = multiWindowMgr.getWindowBounds(buildTaskRecordEx(tr));
                        if (rect == null) {
                            Binder.restoreCallingIdentity(origId);
                            return;
                        }
                        if (!(xPos == -1.0f || yPos == -1.0f)) {
                            Rect bounds = tr.getRequestedOverrideBounds();
                            if (bounds == null) {
                                bounds = multiWindowMgr.getMaximizedBounds();
                            }
                            if (bounds.width() == 0 || bounds.height() == 0) {
                                Binder.restoreCallingIdentity(origId);
                                return;
                            }
                            rect.offsetTo((int) (xPos - (((float) rect.width()) * ((xPos - ((float) bounds.left)) / ((float) bounds.width())))), (int) (yPos - (((float) rect.height()) * ((yPos - ((float) bounds.top)) / ((float) bounds.height())))));
                        }
                        tr.resize(rect, 3, true, false);
                        Binder.restoreCallingIdentity(origId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void hwResizeTask(int taskId, Rect bounds) {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        String str;
        if (HwPCUtils.isPcCastModeInServer() && (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx(this.mIAtmsInner.getATMS()))) != null) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                    boolean isFullscreen = false;
                    boolean isMaximized = false;
                    boolean isSplitWindow = false;
                    if (bounds.isEmpty() && bounds.top == bounds.bottom && bounds.left == bounds.right) {
                        int i = bounds.top;
                        if (i == -3) {
                            bounds.set(multiWindowMgr.getSplitRightWindowBounds());
                            isSplitWindow = true;
                        } else if (i == -2) {
                            bounds.set(multiWindowMgr.getSplitLeftWindowBounds());
                            isSplitWindow = true;
                        } else if (i == -1) {
                            bounds = null;
                            isFullscreen = true;
                        } else if (i == 0) {
                            bounds.set(multiWindowMgr.getMaximizedBounds());
                            isMaximized = true;
                        }
                    }
                    TaskRecord task = this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId);
                    if (task != null) {
                        if (!multiWindowMgr.isSupportResize(buildTaskRecordEx(task), isFullscreen, isMaximized)) {
                            HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "hwResizeTask-fail: (" + Integer.toHexString(task.mWindowState) + ")isFullscreen:" + isFullscreen + "; isMax:" + isMaximized + "; isSplitWindow:" + isSplitWindow);
                            Binder.restoreCallingIdentity(origId);
                            return;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("hwResizeTask: ");
                        if (bounds == null) {
                            str = "null";
                        } else {
                            str = bounds.toShortString() + " (" + bounds.width() + ", " + bounds.height() + ")";
                        }
                        sb.append(str);
                        HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, sb.toString());
                        task.resize(bounds, 3, true, false);
                        if (task.getStack() != null && (isFullscreen || isMaximized || isSplitWindow)) {
                            task.getStack().mHwActivityStackEx.resetOtherStacksVisible(false);
                        }
                        Binder.restoreCallingIdentity(origId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public int getWindowState(IBinder token) {
        ActivityRecord r;
        if (!HwPCUtils.isPcCastModeInServer()) {
            return -1;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                r = ActivityRecord.isInStackLocked(token);
            }
            if (r == null || r.task == null) {
                Binder.restoreCallingIdentity(ident);
                return -1;
            }
            return r.task.getWindowState();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public HwRecentTaskInfo getHwRecentTaskInfo(int taskId) {
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            long origId = Binder.clearCallingIdentity();
            try {
                TaskRecord tr = this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId);
                if (tr != null) {
                    return createHwRecentTaskInfoFromTaskRecord(tr);
                }
                Binder.restoreCallingIdentity(origId);
                return null;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    private HwRecentTaskInfo createHwRecentTaskInfoFromTaskRecord(TaskRecord tr) {
        ActivityManager.RecentTaskInfo rti = this.mIAtmsInner.getATMS().getRecentTasks().createRecentTaskInfo(tr);
        HwRecentTaskInfo hwRti = new HwRecentTaskInfo();
        hwRti.translateRecentTaskinfo(rti);
        ActivityStack stack = tr.getStack();
        if (stack != null) {
            hwRti.displayId = stack.mDisplayId;
        }
        hwRti.windowState = tr.getWindowState();
        if (!tr.mActivities.isEmpty() && (this.mIAtmsInner.getATMS().mWindowManager instanceof HwWindowManagerService)) {
            hwRti.systemUiVisibility = this.mIAtmsInner.getATMS().mWindowManager.getWindowSystemUiVisibility(((ActivityRecord) tr.mActivities.get(0)).appToken);
        }
        return hwRti;
    }

    public boolean skipOverridePendingTransitionForPC(ActivityRecord self) {
        if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(self.getDisplayId())) {
            return false;
        }
        return true;
    }

    public boolean skipOverridePendingTransitionForMagicWindow(ActivityRecord self) {
        if (self == null || !self.inHwMagicWindowingMode()) {
            return false;
        }
        if (!HwMwUtils.performPolicy((int) HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED, new Object[]{self.packageName}).getBoolean("RESULT_NEED_SYSTEM_ANIMATION", true)) {
            return false;
        }
        return true;
    }

    public boolean isTaskNotResizeableEx(TaskRecord task, Rect bounds) {
        return (isTaskSizeChange(task, bounds) && !HwPCMultiWindowCompatibility.isResizable(task.getWindowState())) || (!isTaskSizeChange(task, bounds) && !HwPCMultiWindowCompatibility.isLayoutHadBounds(task.getWindowState()));
    }

    private boolean isTaskSizeChange(TaskRecord task, Rect rect) {
        return (task.getRequestedOverrideBounds().width() == rect.width() && task.getRequestedOverrideBounds().height() == rect.height()) ? false : true;
    }

    public void togglePCMode(boolean pcMode, int displayId) {
        ActivityDisplay activityDisplay;
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            if (!pcMode) {
                if (!(this.mIAtmsInner.getStackSupervisor() == null || (activityDisplay = getPCActivityDisplay(displayId)) == null)) {
                    int size = activityDisplay.getChildCount();
                    ArrayList<ActivityStack> stacks = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        stacks.add(activityDisplay.getChildAt(i));
                    }
                    this.mIAtmsInner.getStackSupervisor().mHwActivityStackSupervisorEx.onDisplayRemoved(stacks);
                }
            }
            this.mIAtmsInner.getATMS().mWindowManager.mHwWMSEx.togglePCMode(pcMode, displayId);
        }
    }

    public void toggleHome() {
        if (HwPCUtils.isPcCastModeInServer()) {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                long origId = Binder.clearCallingIdentity();
                try {
                    int displayId = HwPCUtils.getPCDisplayID();
                    if (HwPCUtils.isValidExtDisplayId(displayId)) {
                        ActivityDisplay activityDisplay = getPCActivityDisplay(displayId);
                        if (activityDisplay == null) {
                            Binder.restoreCallingIdentity(origId);
                            return;
                        }
                        ArrayList<ActivityStack> stacks = new ArrayList<>();
                        int size = activityDisplay.getChildCount();
                        for (int i = 0; i < size; i++) {
                            stacks.add(activityDisplay.getChildAt(i));
                        }
                        boolean moveAllToBack = true;
                        boolean forceAllToBack = HwPCUtils.isHiCarCastMode();
                        if (!forceAllToBack) {
                            int stackNdx = stacks.size() - 1;
                            while (true) {
                                if (stackNdx >= 0) {
                                    ActivityStack stack = stacks.get(stackNdx);
                                    if (stack != null && stack.mHwActivityStackEx.getHiddenFromHome()) {
                                        moveAllToBack = false;
                                        break;
                                    }
                                    stackNdx--;
                                } else {
                                    break;
                                }
                            }
                        }
                        for (int stackNdx2 = stacks.size() - 1; stackNdx2 >= 0; stackNdx2--) {
                            ActivityStack stack2 = stacks.get(stackNdx2);
                            TaskRecord task = stack2.topTask();
                            if (task != null) {
                                if (moveAllToBack) {
                                    if (stack2.shouldBeVisible((ActivityRecord) null) && (forceAllToBack || !stack2.mHwActivityStackEx.getHiddenFromHome())) {
                                        stack2.moveTaskToBackLocked(task.taskId);
                                        stack2.mHwActivityStackEx.setHiddenFromHome(true);
                                    }
                                } else if (!stack2.shouldBeVisible((ActivityRecord) null) && stack2.mHwActivityStackEx.getHiddenFromHome()) {
                                    stack2.moveTaskToFrontLocked(task, true, (ActivityOptions) null, (AppTimeTracker) null, "moveToFrontFromHomeKey.");
                                    stack2.mHwActivityStackEx.setHiddenFromHome(false);
                                }
                            }
                        }
                        Binder.restoreCallingIdentity(origId);
                    }
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    public boolean isMaximizedPortraitAppOnPCMode(ActivityRecord r) {
        if (!HwPCUtils.isPcCastModeInServer() || r.getActivityStack() == null || r.info == null || r.info.getComponentName() == null || !HwPCUtils.isValidExtDisplayId(r.getActivityStack().mDisplayId)) {
            return false;
        }
        return getHwPCMultiWindowManager(buildAtmsEx(this.mIAtmsInner.getATMS())).getPortraitMaximizedPkgList().contains(r.info.getComponentName().getPackageName());
    }

    public TaskChangeNotificationController getHwTaskChangeController() {
        return this.mHwTaskChangeNotificationController;
    }

    public void registerHwTaskStackListener(ITaskStackListener listener) {
        TaskChangeNotificationController taskctl = getHwTaskChangeController();
        if (taskctl != null) {
            taskctl.registerTaskStackListener(listener);
        }
    }

    public void unRegisterHwTaskStackListener(ITaskStackListener listener) {
        if (getHwTaskChangeController() != null) {
            getHwTaskChangeController().unregisterTaskStackListener(listener);
        }
    }

    public void updateUsageStatsForPCMode(ActivityRecord component, boolean visible, UsageStatsManagerInternal usageStatsService) {
        if (component != null && HwPCUtils.isPcDynamicStack(component.getStackId()) && usageStatsService != null) {
            if (!visible) {
                usageStatsService.reportEvent(component.mActivityComponent, component.mUserId, 2, HwPCUtils.getPCDisplayID(), (ComponentName) null);
                this.mPCUsageStats.remove(component.mActivityComponent.toShortString());
            } else if (!this.mPCUsageStats.containsKey(component.mActivityComponent.toShortString())) {
                usageStatsService.reportEvent(component.mActivityComponent, component.mUserId, 1, HwPCUtils.getPCDisplayID(), (ComponentName) null);
                this.mPCUsageStats.put(component.mActivityComponent.toShortString(), Long.valueOf(System.currentTimeMillis()));
            }
        }
    }

    public void moveTaskBackwards(int taskId) {
        if (HwPCUtils.isPcCastModeInServer()) {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                long origId = Binder.clearCallingIdentity();
                TaskRecord tr = this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId);
                if (tr != null && HwPCUtils.isExtDynamicStack(tr.getStackId())) {
                    tr.getStack().moveTaskToBackLocked(taskId);
                }
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public boolean checkTaskId(int taskId) {
        if (this.mIAtmsInner.getATMS().mRootActivityContainer == null || this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId) == null) {
            return false;
        }
        return true;
    }

    public Bitmap getTaskThumbnailOnPCMode(int taskId) {
        ActivityRecord r;
        Bitmap bitmap = null;
        synchronized (this) {
            TaskRecord tr = this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId, 1);
            if (!(tr == null || tr.mStack == null || !(this.mIAtmsInner.getATMS().mWindowManager instanceof HwWindowManagerService) || (r = tr.topRunningActivityLocked()) == null)) {
                bitmap = this.mIAtmsInner.getATMS().mWindowManager.getTaskSnapshotForPc(r.getDisplayId(), r.appToken, tr.taskId, tr.userId);
            }
        }
        return bitmap;
    }

    private ActivityRecord getTopActivityAppToken(ComponentName componentName, ActivityRecord activity) {
        ActivityRecord lastActivityRecord = this.mPreviousResumedActivity;
        if (activity == null) {
            Slog.w(TAG, "requestContent failed: no activity");
            return null;
        } else if (activity.app == null || activity.app.mThread == null) {
            Slog.w(TAG, "requestContent failed: no process for " + activity);
            return null;
        } else if (componentName == null) {
            return null;
        } else {
            if (componentName.equals(activity.info.getComponentName())) {
                Slog.w(TAG, "componentName = " + componentName + " realActivity = " + activity.info.getComponentName() + " isEqual = " + componentName.equals(activity.info.getComponentName()));
                if (lastActivityRecord != null) {
                    Slog.w(TAG, "lastActivityRecord = " + lastActivityRecord.info.getComponentName());
                }
                return activity;
            } else if (lastActivityRecord == null || !componentName.equals(lastActivityRecord.info.getComponentName())) {
                return null;
            } else {
                Slog.w(TAG, "lastActivityRecord = " + lastActivityRecord.info.getComponentName());
                return lastActivityRecord;
            }
        }
    }

    public boolean isGameDndOn() {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.isGameDndOn();
        }
        return false;
    }

    public boolean isGameDndOnEx() {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.isGameDndOnEx();
        }
        return false;
    }

    public boolean addGameSpacePackageList(List<String> packageList) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.addGameSpacePackageList(packageList);
        }
        return false;
    }

    public boolean delGameSpacePackageList(List<String> packageList) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.delGameSpacePackageList(packageList);
        }
        return false;
    }

    public boolean isInGameSpace(String packageName) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.isInGameSpace(packageName);
        }
        return false;
    }

    public List<String> getGameList() {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.getGameList();
        }
        return null;
    }

    public void registerGameObserver(IGameObserver observer) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            hwGameAssistantController.registerGameObserver(observer);
        }
    }

    public void unregisterGameObserver(IGameObserver observer) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            hwGameAssistantController.unregisterGameObserver(observer);
        }
    }

    public void registerGameObserverEx(IGameObserverEx observer) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            hwGameAssistantController.registerGameObserverEx(observer);
        }
    }

    public void unregisterGameObserverEx(IGameObserverEx observer) {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            hwGameAssistantController.unregisterGameObserverEx(observer);
        }
    }

    public boolean isGameKeyControlOn() {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.isGameKeyControlOn();
        }
        return false;
    }

    public boolean isGameGestureDisabled() {
        HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
        if (hwGameAssistantController != null) {
            return hwGameAssistantController.isGameGestureDisabled();
        }
        return false;
    }

    public void reportPreviousInfo(int relationType, WindowProcessController prevProc) {
        if (prevProc != null && this.mIAtmsInner.getAtmDAMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("pid", prevProc.mPid);
            bundleArgs.putInt(ASSOC_TGT_UID, prevProc.mUid);
            bundleArgs.putInt(ASSOC_RELATION_TYPE, relationType);
            this.mIAtmsInner.getAtmDAMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), bundleArgs);
        }
    }

    public void reportHomeProcess(WindowProcessController homeProc) {
        if (this.mIAtmsInner.getAtmDAMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            int pid = 0;
            int uid = 0;
            ArrayList<String> pkgs = new ArrayList<>();
            if (homeProc != null) {
                try {
                    pid = homeProc.mPid;
                    uid = homeProc.mUid;
                    Iterator it = homeProc.mPkgList.iterator();
                    while (it.hasNext()) {
                        String pkg = (String) it.next();
                        if (!pkgs.contains(pkg)) {
                            pkgs.add(pkg);
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    Slog.i(TAG, "reportHomeProcess error happened.");
                }
            }
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("pid", pid);
            bundleArgs.putInt(ASSOC_TGT_UID, uid);
            bundleArgs.putStringArrayList("pkgname", pkgs);
            bundleArgs.putInt(ASSOC_RELATION_TYPE, 11);
            this.mIAtmsInner.getAtmDAMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), bundleArgs);
        }
    }

    public float getAspectRatioWithUserSet(String packageName, String aspectName, ActivityInfo info) {
        if (!"maxAspectRatio".equals(aspectName)) {
            return 0.0f;
        }
        float maxAspectRatio = info.maxAspectRatio;
        mDeviceMaxRatio = this.mIAtmsInner.getATMS().mWindowManager.getDeviceMaxRatio();
        float userMaxAspectRatio = 0.0f;
        if (mDeviceMaxRatio > 0.0f && this.mIAtmsInner.getATMS() != null && !TextUtils.isEmpty(packageName)) {
            userMaxAspectRatio = this.mIAtmsInner.getATMS().getPackageManagerInternalLocked().getUserAspectRatio(packageName, aspectName);
        }
        return (userMaxAspectRatio == 0.0f || userMaxAspectRatio < mDeviceMaxRatio) ? maxAspectRatio : userMaxAspectRatio;
    }

    public boolean isFreeFormVisible() {
        KeyguardManager km;
        Object keyguardService = this.mContext.getSystemService("keyguard");
        boolean stackVisible = false;
        if ((keyguardService instanceof KeyguardManager) && (km = (KeyguardManager) keyguardService) != null && km.isKeyguardLocked()) {
            return false;
        }
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            ActivityStack stack = this.mIAtmsInner.getRootActivityContainer().getStack(5, 1);
            ActivityStack hwFreeFormStack = this.mIAtmsInner.getRootActivityContainer().getStack(102, 1);
            if (stack == null || hwFreeFormStack == null) {
                if (stack != null) {
                    stackVisible = stack.isTopActivityVisible();
                } else if (hwFreeFormStack == null) {
                    return false;
                } else {
                    stackVisible = hwFreeFormStack.isTopActivityVisible();
                }
            } else if (stack.isTopActivityVisible() || hwFreeFormStack.isTopActivityVisible()) {
                stackVisible = true;
            }
            return stackVisible;
        }
    }

    public Intent changeStartActivityIfNeed(Intent intent) {
        if (IS_BOPD) {
            if (intent == null || Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
                Slog.i(TAG, "failed to set activity as EmergencyBackupActivity for bopd due to oobe not finished.");
                return intent;
            }
            intent.removeCategory("android.intent.category.HOME");
            intent.addFlags(4194304);
            intent.setComponent(new ComponentName("com.huawei.KoBackup", "com.huawei.KoBackup.EmergencyBackupActivity"));
            Slog.i(TAG, "set activity as EmergencyBackupActivity in the mode of bopd successfully.");
        }
        return intent;
    }

    public void exitSingleHandMode() {
        this.mHwHandler.removeCallbacks(this.mOverscanTimeout);
        this.mHwHandler.postDelayed(this.mOverscanTimeout, 200);
    }

    private class OverscanTimeout implements Runnable {
        private OverscanTimeout() {
        }

        public void run() {
            Slog.i(HwActivityTaskManagerServiceEx.TAG, "OverscanTimeout run");
            Settings.Global.putString(HwActivityTaskManagerServiceEx.this.mContext.getContentResolver(), "single_hand_mode", "");
        }
    }

    public boolean isSpecialVideoForPCMode(ActivityRecord r) {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        if (!HwPCUtils.isPcCastModeInServer() || (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx(this.mIAtmsInner.getATMS()))) == null || r == null || r.task == null) {
            return false;
        }
        int stackId = r.task.getStackId();
        String packageName = r.packageName;
        if (packageName == null || !HwPCUtils.isPcDynamicStack(stackId)) {
            return false;
        }
        if ((!HwPCUtils.enabledInPad() || !multiWindowMgr.isOlnyFullscreen(packageName)) && !multiWindowMgr.isPortraitApp(buildTaskRecordEx(r.getTaskRecord()))) {
            return false;
        }
        return true;
    }

    public void updateFreeFormOutLine(int colorState) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                ActivityStackSupervisor stackSupervisor = this.mIAtmsInner.getStackSupervisor();
                if (stackSupervisor.mRootActivityContainer != null) {
                    boolean isNull = true;
                    ActivityStack freeformStack = stackSupervisor.mRootActivityContainer.getStack(5, 1);
                    if (freeformStack != null && freeformStack.isFocusable()) {
                        ActivityRecord activity = freeformStack.topRunningActivityLocked();
                        if (activity == null) {
                            Slog.w(TAG, "updateFreeFormOutLine failed: no top activity");
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        if (!(activity.task == null || activity.task.getStack() == null || activity.app == null || activity.app.getThread() == null)) {
                            isNull = false;
                        }
                        if (isNull) {
                            Binder.restoreCallingIdentity(ident);
                            return;
                        } else {
                            try {
                                activity.app.getThread().scheduleFreeFormOutLineChanged(activity.appToken, colorState);
                            } catch (RemoteException e) {
                                Slog.e(TAG, "scheduleFreeFormOutLineChanged error!");
                            }
                        }
                    }
                    Binder.restoreCallingIdentity(ident);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getCaptionState(IBinder token) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                ActivityRecord r = ActivityRecord.isInStackLocked(token);
                if (r == null) {
                    Binder.restoreCallingIdentity(ident);
                    return 0;
                } else if (HwFreeFormUtils.sHideCaptionActivity.contains(r.info.getComponentName().flattenToString())) {
                    return 8;
                } else {
                    Binder.restoreCallingIdentity(ident);
                    return 0;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getActivityWindowMode(IBinder token) {
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            ActivityStack stack = ActivityRecord.getStackLocked(token);
            if (stack == null) {
                return 0;
            }
            return stack.getWindowingMode();
        }
    }

    public boolean canCleanTaskRecord(String packageName, int maxFoundNum, String recentTaskPkg) {
        if (packageName == null || recentTaskPkg == null) {
            return true;
        }
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            ArrayList<TaskRecord> recentTasks = this.mIAtmsInner.getRecentRawTasks();
            if (recentTasks == null) {
                return true;
            }
            int size = recentTasks.size();
            int foundNum = 0;
            for (int i = 0; i < size && foundNum < maxFoundNum; i++) {
                TaskRecord tr = recentTasks.get(i);
                if (tr != null) {
                    if (tr.mActivities != null) {
                        if (!(tr.mActivities.size() <= 0 || tr.getBaseIntent() == null || tr.getBaseIntent().getComponent() == null)) {
                            if (packageName.equals(tr.getBaseIntent().getComponent().getPackageName())) {
                                return false;
                            }
                            if (!recentTaskPkg.equals(tr.getBaseIntent().getComponent().flattenToShortString())) {
                                if ((tr.getBaseIntent().getFlags() & 8388608) != 0) {
                                }
                            }
                        }
                        foundNum++;
                    }
                }
            }
            if (!(this.mIAtmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor) || !this.mIAtmsInner.getStackSupervisor().isInVisibleStack(packageName)) {
                return true;
            }
            return false;
        }
    }

    public int getPreferedDisplayId(ActivityRecord startingActivity, ActivityOptions options, int preferredDisplayId) {
        if (this.mIAtmsInner.getATMS().mVrMananger.isVRDeviceConnected()) {
            return this.mIAtmsInner.getATMS().mVrMananger.getVrPreferredDisplayId(startingActivity.launchedFromPackage, startingActivity.packageName, preferredDisplayId);
        }
        if (HwPCUtils.isPcCastModeInServer()) {
            if (startingActivity != null) {
                if ((startingActivity.isActivityTypeHome() && AppStartupDataMgr.HWPUSH_PKGNAME.equals(startingActivity.launchedFromPackage)) || TextUtils.equals(startingActivity.packageName, "com.huawei.desktop.systemui")) {
                    return 0;
                }
                if (HwPCUtils.isHiCarCastMode() && "com.android.settings".equals(startingActivity.packageName)) {
                    return 0;
                }
            }
            if (HwPCUtils.enabledInPad()) {
                return HwPCUtils.getPCDisplayID();
            }
            if (options != null && HwPCUtils.isValidExtDisplayId(options.getLaunchDisplayId())) {
                return options.getLaunchDisplayId();
            }
            if (startingActivity != null) {
                HashMap<String, Integer> maps = getPkgDisplayMaps();
                int displayId = 0;
                if (!TextUtils.isEmpty(startingActivity.launchedFromPackage)) {
                    if (maps.containsKey(startingActivity.launchedFromPackage)) {
                        displayId = maps.get(startingActivity.launchedFromPackage).intValue();
                    }
                } else if (!TextUtils.isEmpty(startingActivity.packageName) && maps.containsKey(startingActivity.packageName)) {
                    displayId = maps.get(startingActivity.packageName).intValue();
                }
                if (HwPCUtils.isValidExtDisplayId(displayId)) {
                    return displayId;
                }
            }
            if (startingActivity != null && HwPCUtils.isHiCarCastMode() && "com.android.incallui".equals(startingActivity.packageName)) {
                return HwPCUtils.getPCDisplayID();
            }
        }
        return preferredDisplayId;
    }

    private final class FingerprintSettingsObserver extends ContentObserver {
        private final Uri fingerPrintKeyguardUri = Settings.Secure.getUriFor("fp_keyguard_enable");

        FingerprintSettingsObserver() {
            super(HwActivityTaskManagerServiceEx.this.mHwHandler);
            HwActivityTaskManagerServiceEx.this.mContext.getContentResolver().registerContentObserver(this.fingerPrintKeyguardUri, false, this, -1);
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            HwActivityTaskManagerServiceEx.this.updateUnlockBoostStatus(userId);
        }
    }

    /* access modifiers changed from: private */
    public void updateUnlockBoostStatus(int userId) {
        boolean isFaceEnabled = false;
        boolean isFpEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "fp_keyguard_enable", 0, userId) > 0;
        Slog.i(TAG, "update fingerprint unlock boost status [" + isFpEnabled + "]");
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_bind_with_lock", 0, userId) != 0) {
            isFaceEnabled = true;
        }
        Slog.i(TAG, "update face unlock boost status [" + isFaceEnabled + "]");
        this.mIsSetFingerprintOrFaceKeyGuard = isFpEnabled | isFaceEnabled;
    }

    private final class FaceSettingsObserver extends ContentObserver {
        private final Uri faceKeyguardURI = Settings.Secure.getUriFor("face_bind_with_lock");

        FaceSettingsObserver() {
            super(HwActivityTaskManagerServiceEx.this.mHwHandler);
            HwActivityTaskManagerServiceEx.this.mContext.getContentResolver().registerContentObserver(this.faceKeyguardURI, false, this, -1);
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            HwActivityTaskManagerServiceEx.this.updateUnlockBoostStatus(userId);
        }
    }

    public boolean isActivityVisiableInFingerBoost(ActivityRecord r) {
        List<String> list;
        if (!IS_PROP_FINGER_BOOST || !this.mIsSetFingerprintOrFaceKeyGuard || r == null || r.info == null || (list = this.mFingerprintUnlockBoostWhiteList) == null || !list.contains(r.info.packageName) || isLandscapeActivity(r)) {
            return false;
        }
        return true;
    }

    private boolean isLandscapeActivity(ActivityRecord r) {
        int iOrientation = r.getOrientation();
        if (iOrientation == 0 || iOrientation == 6 || iOrientation == 8 || iOrientation == 11) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x003d A[Catch:{ FileNotFoundException -> 0x0033, XmlPullParserException -> 0x0030, IOException -> 0x002d, all -> 0x002a }] */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0066  */
    private void initFingerBoostWhiteListData() {
        StringBuilder sb;
        this.mFingerprintUnlockBoostWhiteList = new ArrayList();
        InputStream inputStream = null;
        try {
            File fingerBoostWhiteListXMLFile = HwCfgFilePolicy.getCfgFile("xml/hw_finger_unlock_boost_whitelist.xml", 0);
            if (fingerBoostWhiteListXMLFile != null) {
                try {
                    if (fingerBoostWhiteListXMLFile.exists()) {
                        inputStream = new FileInputStream(fingerBoostWhiteListXMLFile);
                        if (inputStream != null) {
                            Slog.e(TAG, "load finger boost whitelist fail,file not found: hw_finger_unlock_boost_whitelist");
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    Slog.e(TAG, "load finger boost whitelist: IO Exception while closing stream");
                                }
                            }
                            Slog.d(TAG, "finger boost whitelist size:" + this.mFingerprintUnlockBoostWhiteList.size());
                            return;
                        }
                        XmlPullParser xmlParser = Xml.newPullParser();
                        xmlParser.setInput(inputStream, null);
                        for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                            if (xmlEventType == 2 && "fingeer_unlock_boost".equals(xmlParser.getName())) {
                                addWhitListPackageName(xmlParser.getAttributeValue(null, AwareUserHabit.USERHABIT_PACKAGE_NAME));
                            }
                        }
                        try {
                            inputStream.close();
                        } catch (IOException e2) {
                            Slog.e(TAG, "load finger boost whitelist: IO Exception while closing stream");
                        }
                        sb = new StringBuilder();
                        sb.append("finger boost whitelist size:");
                        sb.append(this.mFingerprintUnlockBoostWhiteList.size());
                        Slog.d(TAG, sb.toString());
                        return;
                    }
                } catch (FileNotFoundException e3) {
                    Slog.e(TAG, "load finger boost whitelist fail,FileNotFound ");
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (IOException e4) {
                            Slog.e(TAG, "load finger boost whitelist: IO Exception while closing stream");
                        }
                    }
                    sb = new StringBuilder();
                } catch (XmlPullParserException e5) {
                    Slog.e(TAG, "load finger boost whitelist fail: ParserException ");
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (IOException e6) {
                            Slog.e(TAG, "load finger boost whitelist: IO Exception while closing stream");
                        }
                    }
                    sb = new StringBuilder();
                } catch (IOException e7) {
                    Slog.e(TAG, "load finger boost whitelist fail: ");
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (IOException e8) {
                            Slog.e(TAG, "load finger boost whitelist: IO Exception while closing stream");
                        }
                    }
                    sb = new StringBuilder();
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (IOException e9) {
                            Slog.e(TAG, "load finger boost whitelist: IO Exception while closing stream");
                        }
                    }
                    Slog.d(TAG, "finger boost whitelist size:" + this.mFingerprintUnlockBoostWhiteList.size());
                    throw th;
                }
            }
            Slog.w(TAG, "hw_finger_unlock_boost_whitelist is not exist");
            if (inputStream != null) {
            }
        } catch (NoClassDefFoundError e10) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
    }

    private void addWhitListPackageName(String packageName) {
        if (packageName != null && !this.mFingerprintUnlockBoostWhiteList.contains(packageName)) {
            this.mFingerprintUnlockBoostWhiteList.add(packageName);
        }
    }

    public void showUninstallLauncherDialog(String pkgName) {
        this.mLastLauncherName = pkgName;
        Handler handler = this.mHwHandler;
        handler.sendMessage(handler.obtainMessage(48));
    }

    /* access modifiers changed from: private */
    public void showUninstallLauncher() {
        Context uiContext = this.mIAtmsInner.getUiContext();
        try {
            PackageInfo pInfo = this.mContext.getPackageManager().getPackageInfo(this.mLastLauncherName, 0);
            if (pInfo != null) {
                AlertDialog d = new BaseErrorDialog(uiContext);
                d.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL);
                d.setCancelable(false);
                d.setTitle(uiContext.getString(33685930));
                d.setMessage(uiContext.getString(33685932, this.mContext.getPackageManager().getApplicationLabel(pInfo.applicationInfo).toString()));
                d.setButton(-1, uiContext.getString(33685931), new DialogInterface.OnClickListener() {
                    /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass9 */

                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            HwActivityTaskManagerServiceEx.this.mContext.getPackageManager().deletePackage(HwActivityTaskManagerServiceEx.this.mLastLauncherName, null, 0);
                        } catch (Exception e) {
                            Slog.e(HwActivityTaskManagerServiceEx.TAG, "showUninstallLauncher error because of Exception!");
                        }
                    }
                });
                d.setButton(-2, uiContext.getString(17039360), new DialogInterface.OnClickListener() {
                    /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass10 */

                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                d.show();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    public boolean isAllowToStartActivity(Context context, String callerPkg, ActivityInfo aInfo, boolean isKeyguard, ActivityInfo topActivity) {
        int activityMode;
        if (!IS_CHINA || callerPkg == null || aInfo == null) {
            return true;
        }
        int uid = 0;
        if (isKeyguard) {
            activityMode = 8;
        } else {
            boolean isNotTop = (topActivity == null || aInfo.packageName == null || aInfo.packageName.equals(topActivity.packageName)) ? false : true;
            if (!callerPkg.equals(aInfo.packageName) || !isNotTop) {
                return true;
            }
            activityMode = 4;
        }
        if (aInfo.applicationInfo != null) {
            uid = aInfo.applicationInfo.uid;
        }
        boolean hsmCheck = HwAddViewHelper.getInstance(context).addViewPermissionCheck(aInfo.packageName, activityMode, uid);
        if (hsmCheck || activityMode != 4 || !this.mIAtmsInner.getATMS().mPendingIntentController.isStartFromPendingIntent(aInfo.packageName, UserHandle.getUserId(uid))) {
            if (!hsmCheck) {
                Slog.i(TAG, "isAllowToStartActivity:" + hsmCheck + ", activityMode:" + activityMode + ", callerPkg:" + callerPkg + ", destInfo:" + aInfo + ", topActivity:" + topActivity);
            }
            return hsmCheck;
        }
        Slog.i(TAG, "starting activity through the pendingintent!");
        return true;
    }

    public void setRequestedOrientation(int requestedOrientation) {
        boolean isInLazyMode = false;
        if (this.mIAtmsInner.getATMS().mWindowManager.getLazyMode() != 0) {
            isInLazyMode = true;
        }
        if (isInLazyMode) {
            long origId = Binder.clearCallingIdentity();
            if (requestedOrientation == 0 || requestedOrientation == 6 || requestedOrientation == 8 || requestedOrientation == 11) {
                try {
                    Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    public Bundle getTopActivity() {
        Bundle activityInfoBundle = new Bundle();
        int callingUid = UserHandle.getAppId(Binder.getCallingUid());
        if (callingUid != 1000) {
            int callingPid = Binder.getCallingPid();
            this.mIAtmsInner.getATMS();
            if (!(ActivityTaskManagerService.checkPermission("android.permission.REAL_GET_TASKS", callingPid, callingUid) == 0)) {
                Slog.d(TAG, "permission denied for, callingPid:" + callingPid + ", callingUid:" + callingUid + ", requires: android.Manifest.permission.REAL_GET_TASKS");
                return activityInfoBundle;
            }
        }
        ActivityRecord r = this.mIAtmsInner.getLastResumedActivityRecord();
        if (r == null) {
            return activityInfoBundle;
        }
        boolean isShowExtend = false;
        if (!r.inMultiWindowMode() && r.maxAspectRatio > 0.0f && r.maxAspectRatio < mDeviceMaxRatio && !mWhitelistActivities.contains(r.info.getComponentName().getClassName())) {
            isShowExtend = true;
        }
        activityInfoBundle.putBoolean("isShowExtend", isShowExtend);
        activityInfoBundle.putBoolean("visible", r.visible);
        activityInfoBundle.putParcelable("activityInfo", r.info);
        if (r.app != null) {
            activityInfoBundle.putInt("pid", r.app.mPid);
        }
        if (r.getTaskRecord() != null) {
            activityInfoBundle.putInt("taskId", r.getTaskRecord().taskId);
        }
        activityInfoBundle.putBoolean("inMultiWindowMode", r.inMultiWindowMode());
        return activityInfoBundle;
    }

    public void addStackReferenceIfNeeded(ActivityStack stack) {
        this.mHwMwm.addStackReferenceIfNeeded(stack);
    }

    public void removeStackReferenceIfNeeded(ActivityStack stack) {
        this.mHwMwm.removeStackReferenceIfNeeded(stack);
    }

    public List<ActivityStack> findCombinedSplitScreenStacks(ActivityStack stack) {
        return this.mHwMwm.findCombinedSplitScreenStacks(stack);
    }

    public int[] getCombinedSplitScreenTaskIds(ActivityStack stack) {
        return this.mHwMwm.getCombinedSplitScreenTaskIds(stack);
    }

    public void calcHwMultiWindowStackBoundsForConfigChange(ActivityStack stack, Rect outBounds, Rect oldStackBounds, int oldDisplayWidth, int oldDisplayHeight, int newDisplayWidth, int newDisplayHeight, boolean isModeChanged) {
        this.mHwMwm.calcHwMultiWindowStackBoundsForConfigChange(stack, outBounds, oldStackBounds, oldDisplayWidth, oldDisplayHeight, newDisplayWidth, newDisplayHeight, isModeChanged);
    }

    public void onDisplayConfigurationChanged(int displayId) {
        this.mHwMwm.onConfigurationChanged(displayId);
    }

    public void focusStackChange(int currentUser, int displayId, ActivityStack currentFocusedStack, ActivityStack lastFocusedStack) {
        this.mHwMwm.focusStackChange(currentUser, displayId, currentFocusedStack, lastFocusedStack);
    }

    public void addSurfaceInNotchIfNeed() {
        this.mHwMwm.addSurfaceInNotchIfNeed();
    }

    public List<ActivityManager.RunningTaskInfo> getVisibleTasks() {
        RootActivityContainer rootActivityContainer;
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        this.mIAtmsInner.getATMS();
        int i = 1;
        if (!(ActivityTaskManagerService.checkPermission("android.permission.REAL_GET_TASKS", callingPid, callingUid) == 0)) {
            Slog.d(TAG, "permission denied for getVisibleTasks, callingPid:" + callingPid + ", callingUid:" + callingUid + ", requires: android.Manifest.permission.REAL_GET_TASKS");
            return null;
        }
        List<ActivityManager.RunningTaskInfo> list = new ArrayList<>();
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            RootActivityContainer rootActivityContainer2 = this.mIAtmsInner.getATMS().mRootActivityContainer;
            int i2 = rootActivityContainer2.getChildCount() - 1;
            while (i2 >= 0) {
                ActivityDisplay display = rootActivityContainer2.getChildAt(i2);
                int stackNdx = display.getChildCount() - i;
                while (stackNdx >= 0) {
                    List<TaskRecord> taskHistory = display.getChildAt(stackNdx).getTaskHistory();
                    int taskNdx = taskHistory.size() - i;
                    while (taskNdx >= 0) {
                        TaskRecord task = taskHistory.get(taskNdx);
                        ActivityRecord topActivity = task.getTopActivity();
                        if (topActivity == null) {
                            rootActivityContainer = rootActivityContainer2;
                        } else if (!topActivity.visible) {
                            rootActivityContainer = rootActivityContainer2;
                        } else {
                            ActivityManager.RunningTaskInfo rti = new ActivityManager.RunningTaskInfo();
                            task.fillTaskInfo(rti);
                            rootActivityContainer = rootActivityContainer2;
                            rti.id = rti.taskId;
                            list.add(rti);
                        }
                        taskNdx--;
                        rootActivityContainer2 = rootActivityContainer;
                    }
                    stackNdx--;
                    i = 1;
                }
                i2--;
                i = 1;
            }
        }
        return list;
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0053  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x002e  */
    public ActivityManager.TaskSnapshot getTaskSnapshot(int taskId, boolean reducedResolution) {
        boolean isAllowed;
        AppWindowToken appWindowToken;
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        boolean isCallerFromHome = this.mIAtmsInner.getATMS().getRecentTasks().isCallerRecents(callingUid);
        boolean z = false;
        if (!isCallerFromHome) {
            this.mIAtmsInner.getATMS();
            if (ActivityTaskManagerService.checkPermission("android.permission.READ_FRAME_BUFFER", callingPid, callingUid) != 0) {
                isAllowed = false;
                if (isAllowed) {
                    Slog.d(TAG, "permission denied for getTaskSnapshot, callingPid:" + callingPid + ", callingUid:" + callingUid + ", requires: android.Manifest.permission.READ_FRAME_BUFFER");
                    return null;
                }
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                        TaskRecord task = this.mIAtmsInner.getATMS().mRootActivityContainer.anyTaskForId(taskId, 1);
                        if (task == null) {
                            Slog.w(TAG, "getTaskSnapshot: taskId=" + taskId + " not found");
                            return null;
                        }
                        ActivityRecord topActivity = task.getTopActivity();
                        if (topActivity != null && topActivity.visible) {
                            if (isCallerFromHome && (appWindowToken = this.mIAtmsInner.getATMS().mWindowManager.getRoot().getAppWindowToken(topActivity.appToken)) != null) {
                                appWindowToken.mHadTakenSnapShot = false;
                            }
                            IHwWindowManagerServiceEx windowManagerServiceEx = this.mIAtmsInner.getATMS().mWindowManager.getWindowManagerServiceEx();
                            IApplicationToken.Stub stub = topActivity.appToken;
                            if (!isCallerFromHome) {
                                z = true;
                            }
                            windowManagerServiceEx.takeTaskSnapshot(stub, z);
                        }
                        ActivityManager.TaskSnapshot snapshot = task.getSnapshot(reducedResolution, true);
                        Binder.restoreCallingIdentity(ident);
                        return snapshot;
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
        isAllowed = true;
        if (isAllowed) {
        }
    }

    public void onCaptionDropAnimationDone(IBinder activityToken) {
        this.mIAtmsInner.getATMS().mUiHandler.post(new Runnable(activityToken) {
            /* class com.android.server.wm.$$Lambda$HwActivityTaskManagerServiceEx$XiOTPQHYtkOcv3nAzFolgbRJUEc */
            private final /* synthetic */ IBinder f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwMultiWindowSwitchManager.this.onCaptionDropAnimationDone(this.f$1);
            }
        });
    }

    public void moveStackToFrontEx(ActivityOptions options, ActivityStack stack, ActivityRecord startActivity) {
        this.mHwMwm.moveStackToFrontEx(options, stack, startActivity);
    }

    public void dismissSplitScreenToFocusedStack() {
        this.mIAtmsInner.getATMS().enforceCallerIsRecentsOrHasPermission("android.permission.MANAGE_ACTIVITY_STACKS", "dismissSplitScreenToFocusedStack()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                ActivityStackSupervisor stackSupervisor = this.mIAtmsInner.getStackSupervisor();
                if (!(stackSupervisor == null || stackSupervisor.mRootActivityContainer == null)) {
                    if (stackSupervisor.mRootActivityContainer.getDefaultDisplay() != null) {
                        ActivityStack stack = stackSupervisor.mRootActivityContainer.getDefaultDisplay().getSplitScreenPrimaryStack();
                        if (stack == null) {
                            Slog.e(TAG, "dismissSplitScreenToFocusedStack: primary split-screen stack not found.");
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        ActivityStack focusStack = stackSupervisor.mRootActivityContainer.getDefaultDisplay().getFocusedStack();
                        if (focusStack == null) {
                            Slog.e(TAG, "dismissSplitScreenToFocusedStack: focusStack == null return");
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        focusStack.moveToFront("dismissSplitScreenModeToFocusedStack");
                        stack.setWindowingMode(1);
                        Binder.restoreCallingIdentity(ident);
                        return;
                    }
                }
                Slog.e(TAG, "dismissSplitScreenToFocusedStack:stackSupervisor not found.");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void handleMultiWindowSwitch(IBinder activityToken, Bundle info) {
        this.mIAtmsInner.getATMS().mUiHandler.post(new Runnable(activityToken, info) {
            /* class com.android.server.wm.$$Lambda$HwActivityTaskManagerServiceEx$nirOzeO0WxVjNFkkUD4twcPnpAo */
            private final /* synthetic */ IBinder f$1;
            private final /* synthetic */ Bundle f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                HwMultiWindowSwitchManager.this.addHotArea(this.f$1, this.f$2);
            }
        });
    }

    public Rect relocateOffScreenWindow(Rect originalWindowBounds, ActivityStack stack) {
        return this.mHwMwm.relocateOffScreenWindow(originalWindowBounds, stack);
    }

    public Point getDragBarCenterPoint(Rect originalWindowBounds, ActivityStack stack) {
        return this.mHwMwm.getDragBarCenterPoint(originalWindowBounds, stack);
    }

    public int[] setFreeformStackVisibility(int displayId, int[] stackIdArray, boolean isVisible) {
        Binder.getCallingPid();
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        if (!(appId == 1000 || appId == 0)) {
            this.mIAtmsInner.getATMS().enforceCallerIsRecentsOrHasPermission("android.permission.MANAGE_ACTIVITY_STACKS", "setFreeformStackVisibility()");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mHwMwm.setFreeformStackVisibility(displayId, stackIdArray, isVisible);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateDragFreeFormPos(Rect bounds, ActivityDisplay activityDisplay) {
        this.mHwMwm.updateDragFreeFormPos(bounds, activityDisplay);
    }

    public Bundle getSplitStacksPos(int displayId, int splitRatio) {
        return this.mHwMwm.getSplitStacksPos(displayId, splitRatio);
    }

    public boolean enterCoordinationMode(Intent intent) {
        int currentState = ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)).getDisplayMode();
        if (!HwFoldScreenState.isFoldScreenDevice() || currentState == 1 || currentState == 4 || currentState == 0) {
            return false;
        }
        HwFoldScreenManagerInternal fsmInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
        if (fsmInternal == null) {
            Slog.i(TAG, "Not find HwFoldScreenManagerInternal service, enterCoordinationMode return");
            return false;
        } else if (fsmInternal.getFoldableState() == 1) {
            Slog.i(TAG, "Current FoldableState EXPAND, enterCoordinationMode return");
            return false;
        } else if (fsmInternal.isPausedDispModeChange()) {
            Slog.i(TAG, "FSM isPausedDispModeChange enterCoordinationMode return");
            return false;
        } else {
            Slog.d(TAG, "enterCoordinationMode");
            synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityStack stack = this.mIAtmsInner.getStackSupervisor().mRootActivityContainer.getDefaultDisplay().getFocusedStack();
                    if (stack == null) {
                        Slog.w(TAG, "enterCoordinationMode:No stack:" + stack);
                        return false;
                    }
                    CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
                    if (utils.isEnterOrExitCoordinationMode()) {
                        Slog.w(TAG, "enterCoordinationMode:It is now Entering or Exiting CoordinationMode");
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    } else if (utils.getCoordinationCreateMode() != 0) {
                        Slog.w(TAG, "enterCoordinationMode:It is now In CoordinationMode");
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    } else {
                        this.mIAtmsInner.getATMS().mWindowManager.disableEventDispatching(1000);
                        utils.setCoordinationState(1);
                        setFreeformStackVisibility(0, null, false);
                        setDisplayMode(4);
                        setCoordinationCreateMode(currentState);
                        this.mCoordinationIntent = intent;
                        this.mCoordinationIntent.addFlags(268435456);
                        this.mCoordinationIntent.addHwFlags(AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION);
                        this.mPkgNameInCoordinationMode = this.mCoordinationIntent.getPackage();
                        Slog.d(TAG, "enterCoordinationMode step one");
                        Binder.restoreCallingIdentity(ident);
                        return true;
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    public void enterCoordinationMode() {
        WindowState mainWindow;
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            long ident = Binder.clearCallingIdentity();
            try {
                Slog.d(TAG, "enterCoordinationMode step two");
                ActivityStack stack = this.mIAtmsInner.getStackSupervisor().mRootActivityContainer.getDefaultDisplay().getFocusedStack();
                if (stack != null) {
                    if (this.mCoordinationIntent != null) {
                        CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
                        boolean z = true;
                        if (utils.getCoordinationState() != 1) {
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        ActivityRecord topActivity = stack.getTopActivity();
                        if (topActivity == null || topActivity.packageName == null || topActivity.packageName.equals(this.mCoordinationIntent.getPackage())) {
                            stack.moveToFront("enterCoordinationMode", (TaskRecord) null);
                            stack.setWindowingMode(11);
                            Rect dockedBounds = new Rect();
                            int rotation = this.mIAtmsInner.getATMS().mWindowManager.getDefaultDisplayContentLocked().getDisplayInfo().rotation;
                            utils.getStackCoordinationModeBounds(true, rotation, dockedBounds);
                            resizeCoordinationStackLocked(dockedBounds);
                            this.mKeepPrimaryCoordinationResumed = true;
                            CoordinationStackDividerManager instance = CoordinationStackDividerManager.getInstance(this.mContext);
                            if (this.mContext.getResources().getConfiguration().orientation != 2) {
                                z = false;
                            }
                            instance.addDividerView(z);
                            ActivityOptions opts = ActivityOptions.makeBasic();
                            Rect secondaryBounds = new Rect();
                            CoordinationModeUtils.getInstance(this.mContext).getStackCoordinationModeBounds(false, rotation, secondaryBounds);
                            opts.setLaunchBounds(secondaryBounds);
                            opts.setLaunchWindowingMode(12);
                            this.mContext.startActivityAsUser(new Intent(this.mCoordinationIntent), opts.toBundle(), new UserHandle(this.mIAtmsInner.getATMS().getCurrentUserId()));
                            utils.setCoordinationState(2);
                            Slog.d(TAG, "enterCoordinationMode over");
                            this.mCoordinationIntent = null;
                            if (!(topActivity == null || topActivity.mAppWindowToken == null || (mainWindow = topActivity.mAppWindowToken.findMainWindow()) == null)) {
                                mainWindow.reportResized();
                            }
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        Slog.w(TAG, "enterCoordinationMode, no match package, primary:" + topActivity.packageName);
                        utils.setCoordinationState(2);
                        Binder.restoreCallingIdentity(ident);
                        return;
                    }
                }
                Slog.w(TAG, "enterCoordinationMode: No stack:" + stack);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void setDisplayMode(int state) {
        HwFoldScreenManagerInternal foldScreenManagerInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
        if (foldScreenManagerInternal != null) {
            foldScreenManagerInternal.setDisplayMode(state);
        }
    }

    private void setCoordinationCreateMode(int state) {
        CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
        if (state == 3) {
            utils.setCoordinationCreateMode(3);
        } else {
            utils.setCoordinationCreateMode(4);
        }
    }

    private void resizeCoordinationStackLocked(Rect dockedBounds) {
        ActivityStackSupervisor stackSupervisor = this.mIAtmsInner.getStackSupervisor();
        if (stackSupervisor.mRootActivityContainer != null) {
            ActivityDisplay display = stackSupervisor.mRootActivityContainer.getDefaultDisplay();
            ActivityStack coordinationPrimaryStack = display.getCoordinationPrimaryStack();
            if (coordinationPrimaryStack == null) {
                Slog.w(TAG, "resizeCoordinationStackLocked: coordinationPrimaryStack not found");
                return;
            }
            WindowManagerService windowManager = this.mIAtmsInner.getATMS().mWindowManager;
            windowManager.deferSurfaceLayout();
            try {
                ActivityRecord r = coordinationPrimaryStack.topRunningActivityLocked();
                try {
                    coordinationPrimaryStack.resize(dockedBounds, (Rect) null, (Rect) null);
                    Rect otherTaskRect = new Rect();
                    Rect tempRect = new Rect();
                    for (int i = display.getChildCount() - 1; i >= 0; i--) {
                        ActivityStack current = display.getChildAt(i);
                        if (current.getWindowingMode() != 11) {
                            if (!current.inHwFreeFormWindowingMode()) {
                                current.setWindowingMode(12);
                                current.getStackDockedModeBounds((Rect) null, (Rect) null, tempRect, otherTaskRect);
                                stackSupervisor.mRootActivityContainer.resizeStack(current, !tempRect.isEmpty() ? tempRect : null, !otherTaskRect.isEmpty() ? otherTaskRect : null, (Rect) null, true, true, false);
                            }
                        }
                    }
                    coordinationPrimaryStack.ensureVisibleActivitiesConfigurationLocked(r, true);
                    windowManager.continueSurfaceLayout();
                } catch (Throwable th) {
                    th = th;
                    windowManager.continueSurfaceLayout();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                windowManager.continueSurfaceLayout();
                throw th;
            }
        }
    }

    public boolean exitCoordinationMode(boolean toTop, boolean changeMode) {
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            long ident = Binder.clearCallingIdentity();
            try {
                Slog.d(TAG, "exitCoordinationMode");
                final CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
                if (utils.isEnterOrExitCoordinationMode()) {
                    Slog.w(TAG, "exitCoordinationMode:It is now Exiting or Entering CoordinationMode");
                    return false;
                } else if (utils.getCoordinationCreateMode() == 0) {
                    Slog.w(TAG, "exitCoordinationMode:It is not in CoordinationMode");
                    Binder.restoreCallingIdentity(ident);
                    return false;
                } else {
                    utils.setCoordinationState(3);
                    int currentCreateMode = utils.getCoordinationCreateMode();
                    utils.setCoordinationCreateMode(2);
                    if (changeMode) {
                        if (currentCreateMode == 3) {
                            setDisplayMode(3);
                        } else if (currentCreateMode == 4) {
                            setDisplayMode(2);
                        }
                    }
                    Slog.d(TAG, "exitCoordinationMode step one");
                    this.mHwHandler.postDelayed(new Runnable() {
                        /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass11 */

                        public void run() {
                            if (utils.getCoordinationState() == 3) {
                                Slog.d(HwActivityTaskManagerServiceEx.TAG, "exitCoordinationMode step two timeout");
                                HwActivityTaskManagerServiceEx.this.exitCoordinationMode();
                            }
                        }
                    }, 1500);
                    Binder.restoreCallingIdentity(ident);
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void exitCoordinationMode() {
        synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
            long ident = Binder.clearCallingIdentity();
            try {
                Slog.d(TAG, "exitCoordinationMode step two");
                CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
                if (utils.getCoordinationState() == 3) {
                    ActivityStackSupervisor stackSupervisor = this.mIAtmsInner.getStackSupervisor();
                    if (stackSupervisor == null || stackSupervisor.mRootActivityContainer == null) {
                        Binder.restoreCallingIdentity(ident);
                        return;
                    }
                    ActivityDisplay display = stackSupervisor.mRootActivityContainer.getDefaultDisplay();
                    ActivityStack primaryCoordinationStack = display.getCoordinationPrimaryStack();
                    ActivityStack secondaryCoordinationStack = display.getTopStackInWindowingMode(12);
                    if (primaryCoordinationStack == null || secondaryCoordinationStack == null) {
                        Slog.w(TAG, "exitCoordinationMode:not found primaryCoordinationStack:" + primaryCoordinationStack + " or secondaryCoordinationStack:" + secondaryCoordinationStack);
                        ArrayList<ActivityStack> stacks = display.getAllStacksInWindowingMode(1);
                        int stackSize = stacks.size();
                        for (int i = 0; i <= stackSize - 1; i++) {
                            ActivityStack otherstack = stacks.get(i);
                            if (otherstack != null && otherstack.getTopActivity() != null) {
                                if (otherstack.getTopActivity().packageName.equals(this.mPkgNameInCoordinationMode)) {
                                    if (otherstack.getTopActivity().shortComponentName.contains("CollaborationActivity")) {
                                        otherstack.finishAllActivitiesLocked(true);
                                    }
                                }
                            }
                        }
                        utils.setCoordinationCreateMode(0);
                        CoordinationStackDividerManager.getInstance(this.mContext).removeDividerView();
                        utils.setCoordinationState(4);
                        this.mPkgNameInCoordinationMode = "";
                        Binder.restoreCallingIdentity(ident);
                        return;
                    }
                    ArrayList<ActivityStack> stacks2 = display.getAllStacksInWindowingMode(12);
                    int stackSize2 = stacks2.size();
                    for (int i2 = 0; i2 <= stackSize2 - 1; i2++) {
                        ActivityStack otherstack2 = stacks2.get(i2);
                        ActivityRecord topActivity = otherstack2.getTopActivity();
                        if (!(topActivity == null || topActivity.packageName == null || topActivity.shortComponentName == null || !topActivity.packageName.equals(this.mPkgNameInCoordinationMode) || !topActivity.shortComponentName.contains("CollaborationActivity"))) {
                            otherstack2.finishAllActivitiesLocked(true);
                        }
                    }
                    utils.setCoordinationCreateMode(0);
                    primaryCoordinationStack.setWindowingMode(1);
                    CoordinationStackDividerManager.getInstance(this.mContext).removeDividerView();
                    utils.setCoordinationState(4);
                    this.mPkgNameInCoordinationMode = "";
                    Slog.d(TAG, "exitCoordinationMode over");
                    Binder.restoreCallingIdentity(ident);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void resumeCoordinationPrimaryStack(ActivityRecord r) {
        ActivityStackSupervisor stackSupervisor;
        if (r != null && this.mKeepPrimaryCoordinationResumed && (stackSupervisor = this.mIAtmsInner.getStackSupervisor()) != null && stackSupervisor.mRootActivityContainer != null) {
            ActivityDisplay display = stackSupervisor.mRootActivityContainer.getDefaultDisplay();
            ActivityStack primaryCoordinationStack = display.getCoordinationPrimaryStack();
            ActivityStack secondaryCoordinationStack = display.getTopStackInWindowingMode(12);
            if (primaryCoordinationStack == null || r.getStackId() != secondaryCoordinationStack.mStackId) {
                this.mKeepPrimaryCoordinationResumed = false;
                return;
            }
            ActivityRecord ar = primaryCoordinationStack.topRunningActivityLocked();
            this.mKeepPrimaryCoordinationResumed = false;
            ar.moveFocusableActivityToTop("setFocusedTask");
        }
    }

    public boolean shouldResumeCoordinationPrimaryStack() {
        return this.mKeepPrimaryCoordinationResumed;
    }

    public void setSplitBarVisibility(boolean isVisibility) {
        this.mHwMwm.setSplitBarVisibility(isVisibility);
    }

    public boolean isSwitchToMagicWin(int stackId, boolean isFreeze, int orientation) {
        if (!HwMwUtils.ENABLED) {
            return false;
        }
        return HwMwUtils.performPolicy(10, new Object[]{Integer.valueOf(stackId), Boolean.valueOf(isFreeze), Integer.valueOf(orientation)}).getBoolean("RESULT_SPLITE_SCREEN", false);
    }

    public boolean showIncompatibleAppDialog(ActivityInfo activityInfo, String callingPackage) {
        if (!HW_SHOW_INCOMPATIBLE_DIALOG || activityInfo == null || TextUtils.isEmpty(callingPackage) || this.mIsDialogShow) {
            return false;
        }
        if (this.mIsClickCancelButton) {
            this.mIsClickCancelButton = false;
            return false;
        }
        String pkgName = activityInfo.packageName;
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        if (checkNeedShowDialog(activityInfo, callingPackage, pkgName)) {
            this.mCurrentPkgName = pkgName;
            Handler handler = this.mHwHandler;
            handler.sendMessage(handler.obtainMessage(49));
            this.mIsDialogShow = true;
            return true;
        }
        this.mIsDialogShow = false;
        return false;
    }

    private boolean checkNeedShowDialog(ActivityInfo activityInfo, String callingPackage, String pkgName) {
        File file;
        if (!isColdLaunch(activityInfo) || !isCallerHome(callingPackage) || (file = this.mCompatibileXmlFile) == null || !file.exists()) {
            return false;
        }
        if (sCompatibileFileHashMap.isEmpty()) {
            String[] compatibilityXmlDir = HwCfgFilePolicy.getDownloadCfgFile(STRING_INCOMPATIBLE_CFG_DIR, STRING_INCOMPATIBLE_CFG_FILE_PATH);
            if (compatibilityXmlDir != null) {
                this.mCompatibileXmlFile = new File(compatibilityXmlDir[0]);
            }
            Handler handler = this.mHwHandler;
            handler.sendMessage(handler.obtainMessage(50));
            if (!readIncompatibleOrNotRemindXml(this.mCompatibileXmlFile, pkgName, true)) {
                return false;
            }
        } else {
            String xmlPkgVersion = sCompatibileFileHashMap.getOrDefault(pkgName, "");
            if (TextUtils.isEmpty(xmlPkgVersion) || !checkPreloadAppVersion(pkgName, xmlPkgVersion)) {
                return false;
            }
        }
        if (!PRELOAD_APP_NOT_REMIND_XML.exists()) {
            if (this.mCompatibileNoRemindSet.size() != 0) {
                this.mCompatibileNoRemindSet.clear();
            }
            return true;
        } else if (this.mCompatibileNoRemindSet.size() != 0) {
            return !this.mCompatibileNoRemindSet.contains(pkgName);
        } else {
            Handler handler2 = this.mHwHandler;
            handler2.sendMessage(handler2.obtainMessage(51));
            return !readIncompatibleOrNotRemindXml(PRELOAD_APP_NOT_REMIND_XML, pkgName, false);
        }
    }

    private boolean isCallerHome(String callingPackage) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        List<ResolveInfo> infos = this.mContext.getPackageManager().queryIntentActivities(intent, 65536);
        if (infos == null || infos.size() == 0) {
            return false;
        }
        for (ResolveInfo homeInfo : infos) {
            if (homeInfo != null && homeInfo.activityInfo != null && callingPackage.equals(homeInfo.activityInfo.packageName)) {
                if (!ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                    return true;
                }
                Log.i(TAG, "showIncompatibleAppDialog caller is home");
                return true;
            }
        }
        return false;
    }

    private boolean isColdLaunch(ActivityInfo activityInfo) {
        WindowProcessController windowProcessController = this.mIAtmsInner.getATMS().getProcessController(activityInfo.processName, activityInfo.applicationInfo.uid);
        if (windowProcessController != null && windowProcessController.hasThread()) {
            return false;
        }
        if (!ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
            return true;
        }
        Log.i(TAG, "showIncompatibleAppDialog isColdLaunch true");
        return true;
    }

    private boolean readIncompatibleOrNotRemindXml(File xmlFile, String pkgName, boolean needCheckPkgVersion) {
        InputStream inputStream = null;
        try {
            InputStream inputStream2 = new FileInputStream(xmlFile);
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream2, null);
            int xmlEventType = xmlParser.next();
            while (xmlEventType != 1) {
                xmlEventType = xmlParser.next();
                if (xmlEventType == 2) {
                    if (STRING_INCOMPATIBLE_XML_PKG.equals(xmlParser.getName())) {
                        String xmlPkgName = xmlParser.getAttributeValue(null, "name");
                        if (TextUtils.isEmpty(xmlPkgName)) {
                            continue;
                        } else if (xmlPkgName.equals(pkgName)) {
                            if (needCheckPkgVersion) {
                                boolean checkPreloadAppVersion = checkPreloadAppVersion(pkgName, xmlParser.getAttributeValue(null, STRING_INCOMPATIBLE_XML_ATTRIBUTE_PKG_VERSION));
                                try {
                                    inputStream2.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "readIncompatibleOrNotRemindXml IOException while closing stream");
                                }
                                return checkPreloadAppVersion;
                            }
                            try {
                                inputStream2.close();
                            } catch (IOException e2) {
                                Log.e(TAG, "readIncompatibleOrNotRemindXml IOException while closing stream");
                            }
                            return true;
                        }
                    }
                }
            }
            try {
                inputStream2.close();
                return false;
            } catch (IOException e3) {
                Log.e(TAG, "readIncompatibleOrNotRemindXml IOException while closing stream");
                return false;
            }
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "readIncompatibleOrNotRemindXml FileNotFoundException");
            if (0 == 0) {
                return false;
            }
            inputStream.close();
            return false;
        } catch (XmlPullParserException e5) {
            Log.e(TAG, "readIncompatibleOrNotRemindXml XmlPullParserException");
            if (0 == 0) {
                return false;
            }
            inputStream.close();
            return false;
        } catch (IOException e6) {
            Log.e(TAG, "readIncompatibleOrNotRemindXml IOException");
            if (0 == 0) {
                return false;
            }
            inputStream.close();
            return false;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                    Log.e(TAG, "readIncompatibleOrNotRemindXml IOException while closing stream");
                }
            }
            throw th;
        }
    }

    private void readXmlToSetOrMap(File xmlFile, int typeSetOrMap) {
        boolean isTypeHashMap;
        if (typeSetOrMap == 0) {
            isTypeHashMap = true;
        } else if (typeSetOrMap == 1) {
            isTypeHashMap = false;
        } else {
            return;
        }
        InputStream inputStream = null;
        try {
            InputStream inputStream2 = new FileInputStream(xmlFile);
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream2, null);
            int xmlEventType = xmlParser.next();
            Set<String> tempSet = new HashSet<>();
            HashMap<String, String> tempMap = new HashMap<>();
            while (xmlEventType != 1) {
                xmlEventType = xmlParser.next();
                if (xmlEventType == 2) {
                    if (STRING_INCOMPATIBLE_XML_PKG.equals(xmlParser.getName())) {
                        String xmlPkgName = xmlParser.getAttributeValue(null, "name");
                        String xmlPkgVersion = xmlParser.getAttributeValue(null, STRING_INCOMPATIBLE_XML_ATTRIBUTE_PKG_VERSION);
                        if (!TextUtils.isEmpty(xmlPkgName)) {
                            if (isTypeHashMap) {
                                tempMap.put(xmlPkgName, xmlPkgVersion);
                            } else {
                                tempSet.add(xmlPkgName);
                            }
                        }
                    }
                }
            }
            if (isTypeHashMap) {
                sCompatibileFileHashMap = tempMap;
            } else {
                this.mCompatibileNoRemindSet = tempSet;
            }
            try {
                inputStream2.close();
            } catch (IOException e) {
                Log.e(TAG, "readXmlToSetOrMap IOException while closing stream");
            }
        } catch (FileNotFoundException e2) {
            Log.e(TAG, "readXmlToSetOrMap FileNotFoundException");
            if (0 != 0) {
                inputStream.close();
            }
        } catch (XmlPullParserException e3) {
            Log.e(TAG, "readXmlToSetOrMap XmlPullParserException");
            if (0 != 0) {
                inputStream.close();
            }
        } catch (IOException e4) {
            Log.e(TAG, "readXmlToSetOrMap IOException");
            if (0 != 0) {
                inputStream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                    Log.e(TAG, "readXmlToSetOrMap IOException while closing stream");
                }
            }
            throw th;
        }
    }

    private boolean checkPreloadAppVersion(String pkgName, String xmlPkgVersion) {
        int currentVersionCode;
        int xmlVersionCode;
        if (TextUtils.isEmpty(xmlPkgVersion)) {
            return false;
        }
        try {
            PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfo(pkgName, 0);
            if (packageInfo == null || (currentVersionCode = packageInfo.versionCode) >= (xmlVersionCode = Integer.parseInt(xmlPkgVersion))) {
                return false;
            }
            if (!ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                return true;
            }
            Log.i(TAG, "showIncompatibleAppDialog checkVersion pkg:" + pkgName + " currentVersion:" + currentVersionCode + " xmlVersion:" + xmlVersionCode);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "checkPreloadAppVersion PackageManager.NameNotFoundException");
            return false;
        } catch (NumberFormatException e2) {
            Log.e(TAG, "checkPreloadAppVersion NumberFormatException");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void showIncompatibleDialog() {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.mCurrentPkgName, 0);
            if (packageInfo == null) {
                this.mIsDialogShow = false;
            } else {
                initShowDialog(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
            }
        } catch (PackageManager.NameNotFoundException e) {
            this.mIsDialogShow = false;
        }
    }

    private void initShowDialog(String appName) {
        Context uiContext = this.mIAtmsInner.getUiContext();
        AlertDialog alterDialog = new AlertDialog.Builder(uiContext, 33948078).create();
        alterDialog.getWindow().setType(2003);
        alterDialog.setCancelable(false);
        alterDialog.setTitle(uiContext.getString(33686238));
        alterDialog.setMessage(uiContext.getString(33686239, appName));
        View dialogView = LayoutInflater.from(uiContext).cloneInContext(new ContextThemeWrapper(uiContext, 33948078)).inflate(34013289, (ViewGroup) null);
        alterDialog.setView(dialogView);
        final CheckBox checkBox = (CheckBox) dialogView.findViewById(16908289);
        checkBox.setText(33685785);
        alterDialog.setButton(-1, uiContext.getString(33686241), new DialogInterface.OnClickListener() {
            /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass12 */

            public void onClick(DialogInterface dialog, int which) {
                HwActivityTaskManagerServiceEx.this.doIncompatibleDialogUpdate();
            }
        });
        alterDialog.setButton(-2, uiContext.getString(33686240), new DialogInterface.OnClickListener() {
            /* class com.android.server.wm.HwActivityTaskManagerServiceEx.AnonymousClass13 */

            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    HwActivityTaskManagerServiceEx.this.doIncompatibleDialogNoNotifity();
                }
                HwActivityTaskManagerServiceEx.this.doIncompatibleDialogCancel();
            }
        });
        WindowManager.LayoutParams attrs = alterDialog.getWindow().getAttributes();
        attrs.privateFlags = 16;
        alterDialog.getWindow().setAttributes(attrs);
        alterDialog.show();
    }

    /* access modifiers changed from: private */
    public void doIncompatibleDialogNoNotifity() {
        this.mCompatibileNoRemindSet.add(this.mCurrentPkgName);
        Handler handler = this.mHwHandler;
        handler.sendMessage(handler.obtainMessage(52));
        Log.i(TAG, "showIncompatibleAppDialog add not remind pkg:" + this.mCurrentPkgName);
    }

    /* access modifiers changed from: private */
    public void doIncompatibleDialogUpdate() {
        this.mIsDialogShow = false;
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + this.mCurrentPkgName));
        intent.addFlags(268435456);
        try {
            this.mContext.startActivityAsUser(intent, new UserHandle(this.mIAtmsInner.getATMS().mWindowManager.mCurrentUserId));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "doIncompatibleDialogUpdate activity not found");
        } catch (Exception e2) {
            Log.e(TAG, "doIncompatibleDialogUpdate Exception");
        }
    }

    /* access modifiers changed from: private */
    public void doIncompatibleDialogCancel() {
        this.mIsDialogShow = false;
        if (!noNeedLaunchActivity()) {
            this.mIsClickCancelButton = true;
            new Intent();
            Intent intent = this.mContext.getPackageManager().getLaunchIntentForPackage(this.mCurrentPkgName);
            if (intent != null) {
                intent.setFlags(270532608);
                try {
                    this.mContext.startActivityAsUser(intent, new UserHandle(this.mIAtmsInner.getATMS().mWindowManager.mCurrentUserId));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "doIncompatibleDialogCancel activity not found");
                } catch (Exception e2) {
                    Log.e(TAG, "doIncompatibleDialogCancel Exception");
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void readIncompatibleXmlToHashMap() {
        readXmlToSetOrMap(this.mCompatibileXmlFile, 0);
    }

    /* access modifiers changed from: private */
    public void readNotRemindXmlToSet() {
        readXmlToSetOrMap(PRELOAD_APP_NOT_REMIND_XML, 1);
    }

    /* JADX INFO: Multiple debug info for r2v1 java.io.FileOutputStream: [D('fos' java.io.FileOutputStream), D('deleted' boolean)] */
    /* access modifiers changed from: private */
    public void writeNotRemindIncompatibleAppXml(File xmlFile) {
        if (xmlFile.exists()) {
            boolean deleted = false;
            try {
                deleted = xmlFile.delete();
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when delete xmlFile in writeNotRemindIncompatibleAppXml");
            }
            if (!deleted) {
                Log.e(TAG, "canot delete xmlFile in writeNotRemindIncompatibleAppXml");
                return;
            }
        }
        FileOutputStream fos = null;
        try {
            XmlSerializer serializer = Xml.newSerializer();
            FileOutputStream fos2 = new FileOutputStream(xmlFile);
            serializer.setOutput(fos2, "utf-8");
            String enter = System.getProperty("line.separator");
            serializer.startTag(null, STRING_INCOMPATIBLE_XML_PKG_LIST);
            serializer.text(enter);
            for (String pkgName : this.mCompatibileNoRemindSet) {
                writePkgNameToNoNotifyXml(serializer, pkgName);
            }
            serializer.endTag(null, STRING_INCOMPATIBLE_XML_PKG_LIST);
            serializer.endDocument();
            try {
                fos2.close();
            } catch (IOException e2) {
                Log.e(TAG, "writeNotRemindIncompatibleAppXml:- IOE while closing stream");
            }
        } catch (IOException e3) {
            Log.e(TAG, "writeNotRemindIncompatibleAppXml exception");
            if (0 != 0) {
                fos.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    Log.e(TAG, "writeNotRemindIncompatibleAppXml:- IOE while closing stream");
                }
            }
            throw th;
        }
    }

    private void writePkgNameToNoNotifyXml(XmlSerializer serializer, String pkgName) {
        try {
            String enter = System.getProperty("line.separator");
            serializer.startTag(null, STRING_INCOMPATIBLE_XML_PKG);
            serializer.attribute(null, "name", pkgName);
            serializer.endTag(null, STRING_INCOMPATIBLE_XML_PKG);
            serializer.text(enter);
        } catch (IOException e) {
            Log.e(TAG, "writePkgNameToNoNotifyXml exception");
        }
    }

    private boolean noNeedLaunchActivity() {
        if (isAlarm(ActivityManager.getCurrentUser()) || isCallRinging()) {
            return true;
        }
        return false;
    }

    private boolean isAlarm(int user) {
        ComponentName oldCmpName = ComponentName.unflattenFromString("com.android.deskclock/.alarmclock.AlarmKlaxon");
        ComponentName newCmpName = ComponentName.unflattenFromString("com.huawei.deskclock/.alarmclock.AlarmKlaxon");
        HwActivityManagerService hwAms = (HwActivityManagerService) ServiceManager.getService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        return hwAms.serviceIsRunning(oldCmpName, user) || hwAms.serviceIsRunning(newCmpName, user);
    }

    private boolean isCallRinging() {
        return getPhoneState() == 1;
    }

    private int getPhoneState() {
        int phoneState = 0;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        int simCount = telephonyManager.getPhoneCount();
        for (int i = 0; i < simCount; i++) {
            phoneState = telephonyManager.getCallState(i);
            if (phoneState != 0) {
                return phoneState;
            }
        }
        return phoneState;
    }

    public void moveActivityTaskToBackEx(IBinder token) {
        ActivityRecord record;
        if (token != null && (record = ActivityRecord.forTokenLocked(token)) != null) {
            if (record.inHwSplitScreenWindowingMode() && record.visible) {
                this.mIAtmsInner.getATMS().mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(record.appToken, false);
            } else if (record.inHwFreeFormWindowingMode()) {
                bdReport(1032, record.packageName);
            }
        }
    }

    public void toggleFreeformWindowingModeEx(ActivityRecord record) {
        if (record != null) {
            if (record.inHwFreeFormWindowingMode()) {
                bdReport(1033, record.packageName);
            }
            this.mHwMwm.removeSplitScreenDividerBar(-1, true);
            if (record.inMultiWindowMode()) {
                int pid = record.app != null ? record.app.mPid : 0;
                HwGameAssistantController hwGameAssistantController = this.mHwGameAssistantController;
                if (hwGameAssistantController != null) {
                    hwGameAssistantController.updateByToggleFreeFormMaximize(pid, record.getUid(), record.packageName);
                }
                Bundle bundle = new Bundle();
                TaskRecord task = record.task;
                if (task != null) {
                    bundle.putInt("taskId", task.taskId);
                }
                bundle.putInt("pid", pid);
                bundle.putInt("uid", record.getUid());
                bundle.putString(STRING_INCOMPATIBLE_XML_PKG, record.packageName);
                bundle.putString("android.intent.extra.REASON", "toggleFreeform");
                bundle.putInt("android.intent.extra.user_handle", this.mIAtmsInner.getATMS().mWindowManager.mCurrentUserId);
                Message msg = this.mHwHandler.obtainMessage(24);
                msg.obj = bundle;
                this.mHwHandler.sendMessage(msg);
            }
        }
    }

    private void bdReport(int id, String packageName) {
        try {
            JSONObject eventMsg = new JSONObject();
            eventMsg.put("pkgName", packageName);
            Flog.bdReport(this.mContext, id, eventMsg);
        } catch (JSONException e) {
            Slog.e(TAG, "create JSONObject failed.");
        }
    }

    public boolean setCustomActivityController(IActivityController controller) {
        this.mIAtmsInner.getATMS().mAmInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "setCustomActivityController()");
        this.mCustomController = controller;
        return true;
    }

    public boolean customActivityStarting(Intent intent, String packageName) {
        if (this.mCustomController == null) {
            return false;
        }
        boolean isStartOk = true;
        try {
            isStartOk = this.mCustomController.activityStarting(intent.cloneFilter(), packageName);
        } catch (RemoteException e) {
            this.mCustomController = null;
        }
        if (isStartOk) {
            return false;
        }
        Slog.i(TAG, "Not starting activity because custom controller stop it");
        return true;
    }

    public boolean customActivityResuming(String packageName) {
        ISecurityProfileController spc = HwServiceFactory.getSecurityProfileController();
        if (spc != null) {
            spc.handleActivityResuming(packageName);
        }
        IActivityController iActivityController = this.mCustomController;
        if (iActivityController == null) {
            return false;
        }
        boolean isResumeOk = true;
        try {
            isResumeOk = iActivityController.activityResuming(packageName);
        } catch (RemoteException e) {
            this.mCustomController = null;
        }
        if (isResumeOk) {
            return false;
        }
        Slog.i(TAG, "Not resuming activity because custom controller stop it");
        return true;
    }

    public boolean isExSplashEnable(Bundle bundle) {
        String[] appArray;
        List<String> appList;
        if (bundle == null) {
            return false;
        }
        String callingPackage = bundle.getString("exsplash_callingpackage");
        String packageName = bundle.getString("exsplash_package");
        int requestCode = bundle.getInt("exsplash_requestcode");
        boolean isIntercepted = bundle.getBoolean("exsplash_isintercepted", true);
        if (TextUtils.isEmpty(packageName) || requestCode >= 0 || isIntercepted) {
            return false;
        }
        Parcelable parcelable = bundle.getParcelable("exsplash_info");
        ActivityInfo info = null;
        if (parcelable instanceof ActivityInfo) {
            info = (ActivityInfo) parcelable;
        }
        if (!isColdStart(packageName, info) || HMS_PKG_NAME.equals(callingPackage)) {
            return false;
        }
        try {
            if (isExSplashServiceAvaiable()) {
                String appListStr = Settings.Global.getString(this.mContext.getContentResolver(), "ex_splash_list");
                if (1 != Settings.Global.getInt(this.mContext.getContentResolver(), "ex_splash_func_status") || TextUtils.isEmpty(appListStr) || (appArray = appListStr.split(";")) == null || (appList = Arrays.asList(appArray)) == null || !appList.contains(packageName)) {
                    return false;
                }
                return true;
            }
        } catch (Settings.SettingNotFoundException e) {
            Slog.w(TAG, "read exsplash setting error");
        }
        return false;
    }

    public boolean isColdStart(String packageName, ActivityInfo info) {
        WindowProcessController wpc;
        if (TextUtils.isEmpty(packageName) || info == null || info.applicationInfo == null || (wpc = this.mIAtmsInner.getATMS().getProcessController(packageName, info.applicationInfo.uid)) == null || !wpc.hasActivities()) {
            return true;
        }
        return false;
    }

    private boolean isExSplashServiceAvaiable() {
        Intent intent = new Intent("com.huawei.android.hms.ppskit.PPS_EXSPLASH_SERVICE");
        intent.setFlags(276840448);
        intent.setPackage(HMS_PKG_NAME);
        if (this.mContext.getPackageManager().resolveService(intent, 0) != null) {
            return true;
        }
        return false;
    }

    public void startExSplash(Bundle bundle, ActivityOptions checkedOptions) {
        if (bundle != null) {
            Parcelable parcelable = bundle.getParcelable("android.intent.extra.INTENT");
            Intent intent = null;
            if (parcelable instanceof Intent) {
                intent = (Intent) parcelable;
            }
            if (intent != null && intent.getComponent() != null) {
                String packageName = intent.getComponent().getPackageName();
                IntentSender target = createIntentSenderForExSplash(intent, checkedOptions, bundle);
                Bundle extras = new Bundle();
                extras.putString("android.intent.extra.PACKAGE_NAME", packageName);
                extras.putParcelable("android.intent.extra.INTENT", target);
                this.mHwHandler.sendMessage(this.mHwHandler.obtainMessage(91, extras));
            }
        }
    }

    /* access modifiers changed from: private */
    public void showExSplash(Message msg) {
        Bundle bundle;
        if (isExSplashServiceAvaiable()) {
            try {
                Intent newIntent = new Intent("com.huawei.android.hms.ppskit.PPS_EXSPLASH_SERVICE");
                newIntent.setFlags(276840448);
                if ((msg.obj instanceof Bundle) && (bundle = (Bundle) msg.obj) != null) {
                    newIntent.putExtra("android.intent.extra.PACKAGE_NAME", bundle.getString("android.intent.extra.PACKAGE_NAME"));
                    Parcelable target = bundle.getParcelable("android.intent.extra.INTENT");
                    if (target instanceof IntentSender) {
                        newIntent.putExtra("android.intent.extra.INTENT", (IntentSender) target);
                    }
                }
                newIntent.setPackage(HMS_PKG_NAME);
                this.mContext.startService(newIntent);
            } catch (ClassCastException e) {
                Slog.w(TAG, "start exsplash error");
            }
        }
    }

    private IntentSender createIntentSenderForExSplash(Intent intent, ActivityOptions checkedOptions, Bundle bundle) {
        ActivityOptions options;
        Bundle activityOptions;
        if (bundle == null) {
            return null;
        }
        String callingPackage = bundle.getString("exsplash_callingpackage");
        int callingUid = bundle.getInt("exsplash_callingUid");
        int userId = bundle.getInt("exsplash_userId");
        String resolvedType = bundle.getString("exsplash_resolvedType");
        Bundle activityOptions2 = null;
        if (checkedOptions == null || checkedOptions.getAnimationType() != 12) {
            options = checkedOptions;
        } else {
            activityOptions2 = ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle();
            options = null;
        }
        if (!HwPCUtils.isPcCastModeInServer() || options == null || !HwPCUtils.isValidExtDisplayId(options.getLaunchDisplayId())) {
            activityOptions = activityOptions2;
        } else {
            ActivityOptions aos = ActivityOptions.makeBasic();
            aos.setLaunchDisplayId(HwPCUtils.getPCDisplayID());
            aos.setLaunchWindowingMode(10);
            activityOptions = aos.toBundle();
        }
        return new IntentSender(this.mIAtmsInner.getATMS().getIntentSenderLocked(2, callingPackage, callingUid, userId, (IBinder) null, (String) null, 0, new Intent[]{intent}, new String[]{resolvedType}, 1409286144, activityOptions));
    }

    public boolean isSplitStackVisible(ActivityDisplay display, int primaryPosition) {
        return this.mHwMwm.isSplitStackVisible(display, primaryPosition);
    }

    public static void updateIncompatibleListByOuc() {
        Log.i(TAG, "updateIncompatibleListByOuc");
        sCompatibileFileHashMap.clear();
    }

    public boolean isResizableApp(String packageName, int mode) {
        if (!HwMultiWindowManager.IS_HW_MULTIWINDOW_SUPPORTED || TextUtils.isEmpty(packageName)) {
            return ActivityInfo.isResizeableMode(mode);
        }
        int appAttr = AppTypeRecoManager.getInstance().getAppAttribute(packageName);
        if (appAttr == -1) {
            return ActivityInfo.isResizeableMode(mode);
        }
        if ((appAttr & 131072) == 131072) {
            return false;
        }
        if ((appAttr & 65536) == 65536) {
            return true;
        }
        if (ActivityManagerService.MY_PID == Binder.getCallingPid()) {
            return ActivityInfo.isResizeableMode(mode);
        }
        if (!ActivityInfo.isResizeableMode(mode) || ActivityInfo.isPreserveOrientationMode(mode)) {
            return false;
        }
        return true;
    }

    public boolean isHwResizableApp(String packageName, int mode) {
        if (!IS_CHINA || !HwActivityManager.IS_PHONE || !HwMultiWindowManager.IS_HW_MULTIWINDOW_SUPPORTED || TextUtils.isEmpty(packageName)) {
            return isResizableApp(packageName, mode);
        }
        int appAttr = AppTypeRecoManager.getInstance().getAppAttribute(packageName);
        if (appAttr == -1 || (appAttr & 65536) != 65536) {
            return false;
        }
        return true;
    }

    public Bundle getHwMultiWindowAppControlLists() {
        Bundle bundle = new Bundle();
        if (!HwMultiWindowManager.IS_HW_MULTIWINDOW_SUPPORTED) {
            return bundle;
        }
        Map<Integer, List<String>> listMap = AppTypeRecoManager.getInstance().getAppsByAttributes(new ArrayList<>(Arrays.asList(65536, 131072, 262144)));
        if (listMap == null) {
            return bundle;
        }
        bundle.putStringArrayList("whitelist", (ArrayList) listMap.get(65536));
        bundle.putStringArrayList("blacklist", (ArrayList) listMap.get(131072));
        bundle.putStringArrayList("recomlist", (ArrayList) listMap.get(262144));
        return bundle;
    }

    public void saveMultiWindowTipState(String tipKey, int state) {
        HwMultiWindowTips.getInstance(this.mContext).saveMultiWindowTipState(tipKey, state);
    }

    public boolean isSupportDragForMultiWin(IBinder token) {
        return HwMultiWindowSwitchManager.getInstance(this).isSupportedSplit(token);
    }

    public boolean isOverrideConfigByMagicWin(Configuration config) {
        if (!HwMwUtils.ENABLED) {
            return false;
        }
        boolean isFoldScreen = HwFoldScreenState.isFoldScreenDevice();
        boolean isNormalScreenLand = !isFoldScreen && config.orientation == 2;
        boolean isFoldScreenPort = isFoldScreen && config.orientation == 1;
        if (!isNormalScreenLand && !isFoldScreenPort) {
            return false;
        }
        if (HwMwUtils.performPolicy(6, new Object[]{config}) == Bundle.EMPTY) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x002d  */
    public List<String> getVisiblePackages() {
        boolean isAllowed;
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(callingUid);
        int i = 1;
        if (!(appId == 1000 || appId == 0)) {
            this.mIAtmsInner.getATMS();
            if (ActivityTaskManagerService.checkPermission("android.permission.REAL_GET_TASKS", callingPid, callingUid) != 0) {
                isAllowed = false;
                List<String> list = new ArrayList<>();
                if (isAllowed) {
                    Slog.d(TAG, "permission denied for getVisibleTasks, callingPid:" + callingPid + ", callingUid:" + callingUid + ", requires: android.Manifest.permission.REAL_GET_TASKS");
                    return list;
                }
                synchronized (this.mIAtmsInner.getATMS().getGlobalLock()) {
                    RootActivityContainer rootActivityContainer = this.mIAtmsInner.getATMS().mRootActivityContainer;
                    ActivityRecord lastResumed = this.mIAtmsInner.getATMS().mLastResumedActivity;
                    int i2 = rootActivityContainer.getChildCount() - 1;
                    while (i2 >= 0) {
                        ActivityDisplay display = rootActivityContainer.getChildAt(i2);
                        boolean isSleeping = display.shouldSleep();
                        int stackNdx = display.getChildCount() - i;
                        while (true) {
                            if (stackNdx < 0) {
                                break;
                            }
                            ActivityStack stack = display.getChildAt(stackNdx);
                            ActivityRecord topActivity = stack.getTopActivity();
                            if (!(topActivity != null && (topActivity.visible || (lastResumed != null && (((topActivity.packageName.equals(lastResumed.packageName) && topActivity.getUid() == lastResumed.getUid()) || lastResumed == topActivity) && (!isSleeping || topActivity.canShowWhenLocked())))))) {
                                if (!stack.inMultiWindowMode() && topActivity != null) {
                                    break;
                                }
                            } else {
                                list.add(topActivity.packageName);
                            }
                            stackNdx--;
                        }
                        i2--;
                        i = 1;
                    }
                }
                return list;
            }
        }
        isAllowed = true;
        List<String> list2 = new ArrayList<>();
        if (isAllowed) {
        }
    }

    public boolean setMultiWindowDisabled(boolean disabled) {
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        if (appId == 1000 || appId == 0) {
            ActivityTaskManagerService atms = this.mIAtmsInner.getATMS();
            if (!this.mIsInitMultiWindowDisabledState) {
                this.mIsSupportsFreeformBefore = atms.mSupportsFreeformWindowManagement;
                this.mIsSupportsSplitScreenBefore = atms.mSupportsSplitScreenMultiWindow;
                atms.mSupportsFreeformWindowManagement = this.mIsSupportsFreeformBefore && !this.mIsMultiWindowDisabled;
                atms.mSupportsSplitScreenMultiWindow = this.mIsSupportsSplitScreenBefore && !this.mIsMultiWindowDisabled;
                this.mIsInitMultiWindowDisabledState = true;
                Slog.d(TAG, "setMultiWindowDisabled init state set " + this.mIsMultiWindowDisabled);
                return false;
            } else if (disabled || this.mIsSupportsFreeformBefore || this.mIsSupportsSplitScreenBefore) {
                if (this.mIsSupportsFreeformBefore) {
                    atms.mSupportsFreeformWindowManagement = !disabled;
                } else {
                    Slog.d(TAG, "setMultiWindowDisabled freeform is not supported");
                }
                if (this.mIsSupportsSplitScreenBefore) {
                    atms.mSupportsSplitScreenMultiWindow = !disabled;
                } else {
                    Slog.d(TAG, "setMultiWindowDisabled split-screen is not supported");
                }
                this.mIsMultiWindowDisabled = disabled;
                Slog.d(TAG, "setMultiWindowDisabled set: " + disabled);
                return true;
            } else {
                Slog.d(TAG, "setMultiWindowDisabled freeform and split-screen are not supported");
                return false;
            }
        } else {
            Slog.d(TAG, "setMultiWindowDisabled the caller is not system");
            return false;
        }
    }

    public boolean getMultiWindowDisabled() {
        return this.mIsMultiWindowDisabled;
    }

    private TaskRecordEx buildTaskRecordEx(TaskRecord taskRecord) {
        TaskRecordEx taskRecordEx = new TaskRecordEx();
        taskRecordEx.setTaskRecord(taskRecord);
        return taskRecordEx;
    }

    private ActivityTaskManagerServiceEx buildAtmsEx(ActivityTaskManagerService atms) {
        ActivityTaskManagerServiceEx atmsEx = new ActivityTaskManagerServiceEx();
        atmsEx.setActivityTaskManagerService(atms);
        return atmsEx;
    }

    private DefaultHwPCMultiWindowManager getHwPCMultiWindowManager(ActivityTaskManagerServiceEx atmsEx) {
        return HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwPCMultiWindowManager(atmsEx);
    }

    public void setCallingPkg(String callingPkg) {
        this.mHwMwm.setCallingPackage(callingPkg);
    }

    public void setAlwaysOnTopOnly(ActivityDisplay display, ActivityStack stack, boolean isNewStack, boolean alwaysOnTop) {
        this.mHwMwm.setAlwaysOnTopOnly(display, stack, isNewStack, alwaysOnTop);
    }

    public boolean isMagicWinExcludeTaskFromRecents(TaskRecord task) {
        HwMagicWinCombineManager combineManager;
        String packageName;
        Set<Integer> splitScreenStackIds;
        if (!HwMwUtils.ENABLED || !task.inHwMagicWindowingMode() || (splitScreenStackIds = combineManager.getSplitScreenStackIds((packageName = (combineManager = HwMagicWinCombineManager.getInstance()).getTaskPackageName(task)), task.userId)) == null || combineManager.isForegroundTaskIds(combineManager.getForegroundTaskIds(packageName, task.userId), task.taskId)) {
            return false;
        }
        return splitScreenStackIds.contains(Integer.valueOf(task.getStackId()));
    }

    public boolean isMagicWinSkipRemoveFromRecentTasks(TaskRecord addingTask, TaskRecord removingTask) {
        if (!HwMwUtils.ENABLED || !addingTask.inHwMagicWindowingMode() || !removingTask.inHwMagicWindowingMode() || addingTask.affinity == null || !addingTask.affinity.equals(removingTask.affinity)) {
            return false;
        }
        HwMagicWinCombineManager combineManager = HwMagicWinCombineManager.getInstance();
        String packageName = combineManager.getTaskPackageName(addingTask);
        if (combineManager.getSplitScreenStackIds(packageName, addingTask.userId) == null) {
            return false;
        }
        int[] foregourndTaskIds = combineManager.getForegroundTaskIds(packageName, addingTask.userId);
        if (!combineManager.isForegroundTaskIds(foregourndTaskIds, addingTask.taskId) || !combineManager.isForegroundTaskIds(foregourndTaskIds, removingTask.taskId)) {
            return false;
        }
        return true;
    }

    public void updateSplitBarPosForIm(int position) {
        this.mHwMwm.updateSplitBarPosForIm(position);
    }

    public boolean isNerverUseSizeCompateMode(String packageName) {
        int appAttr;
        if (NERVER_USE_COMPAT_MODE_APPS.contains(packageName)) {
            return true;
        }
        if (!HwMultiWindowManager.IS_HW_MULTIWINDOW_SUPPORTED || TextUtils.isEmpty(packageName) || (appAttr = AppTypeRecoManager.getInstance().getAppAttribute(packageName)) == -1 || (appAttr & 65536) != 65536) {
            return false;
        }
        return true;
    }

    public Bundle getHwMultiWindowState() {
        return this.mHwMwm.getHwMultiWindowState();
    }

    public boolean isPhoneLandscape(DisplayContent displayContent) {
        return this.mHwMwm.isPhoneLandscape(displayContent);
    }

    public boolean isStatusBarPermenantlyShowing() {
        return this.mHwMwm.isStatusBarPermenantlyShowing();
    }

    public void adjustHwFreeformPosIfNeed(DisplayContent displayContent, boolean isStatusShowing) {
        this.mHwMwm.adjustHwFreeformPosIfNeed(displayContent, isStatusShowing);
    }

    public boolean blockSwipeFromTop(MotionEvent event, DisplayContent display) {
        return this.mHwMwm.blockSwipeFromTop(event, display);
    }

    public void setHwWinCornerRaduis(WindowState win, SurfaceControl control) {
        this.mHwMwm.setHwWinCornerRaduis(win, control);
    }

    public float getHwMultiWinCornerRadius(int windowingMode) {
        return this.mHwMwm.getHwMultiWinCornerRadius(windowingMode);
    }
}
