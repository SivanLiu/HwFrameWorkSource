package com.android.server.rms.iaware.appmng;

import android.annotation.SuppressLint;
import android.app.ActivityManagerNative;
import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.rms.HwSysResManager;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CmpTypeInfo;
import android.rms.iaware.ComponentRecoManager;
import android.rms.iaware.DeviceInfo;
import android.rms.iaware.IAwareCMSManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.webkit.WebViewZygote;
import com.android.internal.content.ValidGPackageHelper;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.MemInfoReader;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.location.HwLogRecordManager;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessWindowInfo;
import com.android.server.mtm.iaware.appmng.CloudPushManager;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appstart.comm.AppStartupUtil;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.mtm.iaware.appmng.rule.AppMngRule;
import com.android.server.mtm.iaware.appmng.rule.ListItem;
import com.android.server.mtm.iaware.appmng.rule.RuleNode;
import com.android.server.mtm.iaware.srms.AwareBroadcastDebug;
import com.android.server.mtm.iaware.srms.AwareBroadcastPolicy;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.AppStatusUtils;
import com.android.server.mtm.utils.InnerUtils;
import com.android.server.mtm.utils.SparseSet;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.dualfwk.AwareMiddleware;
import com.android.server.rms.iaware.appmng.AppStartPolicyCfg;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.feature.AppAccurateRecgFeature;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.qos.AwareBinderSchedManager;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.android.server.wm.WindowProcessController;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.android.pgmng.plug.PowerKit;
import com.huawei.android.view.HwWindowManager;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ConstS32;

public final class AwareIntelligentRecg {
    private static final int ACTIONINDEX = 0;
    private static final int ACTIONLENGTH = 1;
    private static final String ACT_TOP_IM_CN = "act_topim_cn";
    private static final int ALARMACTIONINDEX = 1;
    private static final int ALARMTAGLENGTH = 2;
    private static final int ALARM_PERCEPTION_TIME = 15000;
    private static final int ALARM_RECG_FACTOR = 1;
    private static final int ALARM_RECG_FACTOR_FORCLOCK = 5;
    private static final int ALLOW_BOOT_START_IM_APP_NUM = 3;
    private static final int ALLOW_BOOT_START_IM_MEM_THRESHOLD = 3072;
    private static final int ALLPROC = 1000;
    public static final int APP_ACTTOP1 = 1;
    public static final int APP_ACTTOP10 = 4;
    public static final int APP_ACTTOP100 = 7;
    public static final int APP_ACTTOP2 = 2;
    public static final int APP_ACTTOP20 = 5;
    public static final int APP_ACTTOP5 = 3;
    public static final int APP_ACTTOP50 = 6;
    public static final int APP_ACTTOP_DEFAULT = 0;
    private static final String APP_LOCK_CLASS = "app_lock_class";
    private static final String BACKGROUND_CHECK_EXCLUDED_ACTION = "bgcheck_excluded_action";
    private static final int BLE_TYPE = 0;
    private static final String BLIND_PREFABRICATED = "blind_prefabricated";
    private static final int BLUETOOTH_LAST_USED_PARAMS = 2;
    private static final String BLUETOOTH_PKG = "com.android.bluetooth";
    private static final String BTMEDIABROWSER_ACTION = "android.media.browse.MediaBrowserService";
    private static final String CHINA_MCC = "460";
    private static final int CLSINDEX = 1;
    private static final int CMPLENGTH = 2;
    public static final String CMP_CLS = "cls";
    public static final String CMP_PKGNAME = "pkgName";
    public static final String CMP_TYPE = "cmpType";
    private static final int CONNECT_PG_DELAYED = 5000;
    /* access modifiers changed from: private */
    public static boolean DEBUG = false;
    private static final int DEFAULT_INVAILD_UID = -1;
    private static final int DEFAULT_MEMORY = -1;
    private static final long DEFAULT_REQUEST_TIMEOUT = 1800000;
    private static final int DUMP_APPSCENEINFO_BLE_ON = 3;
    private static final int DUMP_APPSCENEINFO_NOTUSED = 1;
    private static final int DUMP_APPSCENEINFO_SIM = 2;
    private static final int FREEZENOTCLEAN_ENTER = 1;
    private static final int FREEZENOTCLEAN_EXIT = 0;
    private static final int FREEZE_NOT_CLEAN = 10;
    private static final int FREEZE_PIDS = 100;
    private static final long GOOGLE_CONN_DELAY_TIME = 604800000;
    private static final int INVALID_TASKID = -1;
    public static final long INVALID_TIME = -1;
    private static final int LOAD_CMPTYPE_MESSAGE_DELAY = 20000;
    private static final int MCC_LENGTH = 3;
    private static final int MINCOUNT = 0;
    private static final int MSG_ALARM_NOTIFICATION = 9;
    private static final int MSG_ALARM_SOUND = 10;
    private static final int MSG_ALARM_UNPERCEPTION = 11;
    private static final int MSG_ALARM_VIBRATOR = 8;
    private static final int MSG_CACHEDATA_FLUSHTODISK = 6;
    private static final int MSG_CHECK_CMPDATA = 16;
    private static final int MSG_CONNECT_WITH_PG_SDK = 5;
    private static final int MSG_DELETEDATA = 3;
    private static final int MSG_INPUTMETHODSET = 15;
    private static final int MSG_INSERTDATA = 2;
    private static final int MSG_KBG_UPDATE = 7;
    private static final int MSG_LOADDATA = 1;
    private static final int MSG_RECG_PUSHSDK = 4;
    private static final int MSG_UNFREEZE_NOTCLEAN = 17;
    private static final int MSG_UPDATEDB = 12;
    private static final int MSG_UPDATE_BLE_STATUS = 20;
    private static final int MSG_UPDOWN_CLEAR = 19;
    private static final int MSG_WAKENESS_CHANGE = 18;
    private static final int MSG_WALLPAPERINIT = 14;
    private static final int MSG_WALLPAPERSET = 13;
    private static final String NOTIFY_LISTENER_ACTION = "android.service.notification.NotificationListenerService";
    private static final int ONE_GIGA_BYTE = 1024;
    private static final int ONE_MINUTE = 60000;
    private static final int ONE_SECOND = 1000;
    public static final int PENDING_ALARM = 0;
    public static final int PENDING_PERC = 2;
    public static final int PENDING_UNPERC = 1;
    private static final int PKGINDEX = 0;
    private static final String PROPERTIES_GOOELE_CONNECTION = "persist.sys.iaware_google_conn";
    private static final String PUSHSDK_BAD = "push_bad";
    private static final String PUSHSDK_GOOD = "push_good";
    private static final int PUSHSDK_START_SAME_CMP_COUNT = 2;
    private static final int REPORT_APPUPDATE_MSG = 0;
    private static final String SEPARATOR_COMMA = ",";
    private static final String SEPARATOR_POUND = "#";
    private static final String SEPARATOR_SEMICOLON = ":";
    private static final int SPEC_VALUE_DEFAULT = -1;
    private static final int SPEC_VALUE_DEFAULT_MAX_TOPN = 999;
    private static final int SPEC_VALUE_TRUE = 1;
    private static final int STARTINFO_CACHE_TIME = 20000;
    private static final int STATUS_DEFAULT = 0;
    private static final int STATUS_PERCEPTION = 1;
    private static final int STATUS_UNPERCEPTION = 2;
    private static final String SWITCH_STATUS_STR = "1";
    private static final String SYSTEM_PROCESS_NAME = "system";
    private static final String TAG = "RMS.AwareIntelligentRecg";
    private static final String TAG_DOZE_PROTECT = "frz_protect";
    private static final String TAG_GET_DOZE_LIST = "hsm_get_freeze_list";
    private static final String TOP_IM_CN_PROP = "persist.sys.iaware.topimcn";
    private static final int TYPE_TOPN_EXTINCTION_TIME = 172800000;
    private static final int UNFREEZE_NOTCLEAN_INTERVAL = 60000;
    private static final String UNKNOWN_PKG = "unknownpkg";
    private static final int UNPERCEPTION_COUNT = 1;
    private static final int UPDATETIME = 10000000;
    private static final long UPDATE_DB_INTERVAL = 86400000;
    private static final int UPDATE_TIME_FOR_HABIT = 10000;
    private static final String URI_PREFIX = "content://";
    private static final String URI_SYSTEM_MANAGER_SMART_PROVIDER = "@com.huawei.android.smartpowerprovider";
    private static final String URI_SYSTEM_MANAGER_UNIFIED_POWER_APP = "content://com.huawei.android.smartpowerprovider/unifiedpowerapps";
    private static final String VALUE_BT_BLE_CONNECT_APPS = "huawei_bt_ble_connect_apps";
    private static final String VALUE_BT_LAST_BLE_DISCONNECT = "huawei_bt_ble_last_disconnect";
    public static final long WIDGET_INVALID_ELAPSETIME = -1;
    private static AwareIntelligentRecg mAwareIntlgRecg = null;
    private static boolean mBleStatus = true;
    private static boolean mEnabled = false;
    private int APPMNGPROP_APPSTARTINDEX = 0;
    private int APPMNGPROP_MAXINDEX = (this.APPMNGPROP_APPSTARTINDEX + 1);
    private int GOOGLECONNPROP_MAXINDEX = (this.GOOGLECONNPROP_STARTINDEX + 2);
    private int GOOGLECONNPROP_STARTINDEX = 0;
    private final Set<String> mAccessPkg = new ArraySet();
    private String mActTopIMCN = UNKNOWN_PKG;
    private final ArrayMap<String, CmpTypeInfo> mAlarmCmps = new ArrayMap<>();
    private final SparseArray<ArrayMap<String, AlarmInfo>> mAlarmMap = new SparseArray<>();
    private Set<String> mAlarmPkgList = null;
    private final Map<String, Long> mAllowStartPkgs = new ArrayMap();
    private final ArrayMap<String, Long> mAppChangeToBGTime = new ArrayMap<>();
    private PowerKit.Sink mAppFreezeListener = new PowerKit.Sink() {
        /* class com.android.server.rms.iaware.appmng.AwareIntelligentRecg.AnonymousClass1 */

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            if (AwareIntelligentRecg.DEBUG) {
                AwareLog.d(AwareIntelligentRecg.TAG, "pkg:" + pkg + ",uid:" + uid + ",pid:" + pid + ",stateType: " + stateType + ",eventType:" + eventType);
            }
            if (stateType != 2) {
                if (stateType == 10) {
                    AwareIntelligentRecg.this.resolvePkgsName(pkg, eventType);
                } else if (stateType == 12) {
                    AwareIntelligentRecg.this.refreshAlivedApps(eventType, pkg, uid);
                } else if (stateType == 100) {
                    AwareIntelligentRecg.this.resolvePids(pkg, uid, eventType);
                } else if (stateType == 6) {
                    synchronized (AwareIntelligentRecg.this.mFrozenAppList) {
                        if (eventType == 1) {
                            try {
                                AwareIntelligentRecg.this.mFrozenAppList.add(uid);
                            } catch (Throwable th) {
                                throw th;
                            }
                        } else if (eventType == 2) {
                            AwareIntelligentRecg.this.mFrozenAppList.remove(uid);
                        }
                    }
                } else if (stateType == 7) {
                    synchronized (AwareIntelligentRecg.this.mFrozenAppList) {
                        AwareIntelligentRecg.this.mFrozenAppList.clear();
                    }
                    synchronized (AwareIntelligentRecg.this.mNotCleanPkgList) {
                        AwareIntelligentRecg.this.mNotCleanPkgList.clear();
                    }
                    SysLoadManager.getInstance().exitGameSceneMsg();
                    ProcessInfoCollector.getInstance().resetAwareProcessStatePgRestart();
                } else if (stateType == 8) {
                    synchronized (AwareIntelligentRecg.this.mBluetoothAppList) {
                        if (eventType == 1) {
                            try {
                                AwareIntelligentRecg.this.mBluetoothAppList.add(uid);
                            } catch (Throwable th2) {
                                throw th2;
                            }
                        } else if (eventType == 2) {
                            AwareIntelligentRecg.this.mBluetoothAppList.remove(uid);
                        }
                    }
                }
            } else if (eventType == 1) {
                AwareIntelligentRecg.this.sendPerceptionMessage(10, uid, null);
                synchronized (AwareIntelligentRecg.this.mAudioOutInstant) {
                    AwareIntelligentRecg.this.mAudioOutInstant.add(uid);
                }
            } else if (eventType == 2) {
                synchronized (AwareIntelligentRecg.this.mAudioOutInstant) {
                    AwareIntelligentRecg.this.mAudioOutInstant.remove(uid);
                }
            }
        }
    };
    private String mAppLockClass = UNKNOWN_PKG;
    private final long mAppMngAllowTime = SystemProperties.getLong("persist.sys.iaware.mngallowtime", 1800);
    private boolean mAppMngPropCfgInit = false;
    private int mAppStartAreaCfg = -1;
    private boolean mAppStartEnabled = false;
    private final Set<AwareAppStartInfo> mAppStartInfoList = new ArraySet();
    /* access modifiers changed from: private */
    public SparseSet mAudioOutInstant = new SparseSet();
    private AwareStateCallback mAwareStateCallback = null;
    private ArraySet<String> mBGCheckExcludedAction = new ArraySet<>();
    private ArraySet<String> mBGCheckExcludedPkg = new ArraySet<>();
    private final Set<String> mBlindPkg = new ArraySet();
    /* access modifiers changed from: private */
    public SparseSet mBluetoothAppList = new SparseSet();
    private final Object mBluetoothLock = new Object();
    private int mBluetoothUid;
    private final ArrayList<Integer> mBtoothConnectList = new ArrayList<>();
    private int mBtoothLastPid = -1;
    private long mBtoothLastTime = 0;
    private volatile ArrayMap<Integer, Set<String>> mCachedTypeTopN = new ArrayMap<>();
    private final ArraySet<IAwareToastCallback> mCallbacks = new ArraySet<>();
    private final SparseSet mCameraUseAppList = new SparseSet();
    private final ArraySet<String> mControlGmsApp = new ArraySet<>();
    private int mCurUserId = 0;
    private int mDBUpdateCount = 0;
    private final AtomicInteger mDataLoadCount = new AtomicInteger(2);
    /* access modifiers changed from: private */
    public AppStartupDataMgr mDataMgr = null;
    /* access modifiers changed from: private */
    public String mDefaultInputMethod = "";
    private int mDefaultInputMethodUid = -1;
    /* access modifiers changed from: private */
    public String mDefaultSms = "";
    private String mDefaultTTS = "";
    private String mDefaultWallPaper = "";
    private int mDefaultWallPaperUid = -1;
    public int mDeviceLevel = -1;
    private int mDeviceMemoryOfGIGA = -1;
    private long mDeviceTotalMemory = -1;
    private final ArraySet<String> mDozeProtectPkg = new ArraySet<>();
    /* access modifiers changed from: private */
    public SparseSet mFrozenAppList = new SparseSet();
    private ArrayMap<AppMngConstant.EnumWithDesc, ArrayMap<String, ListItem>> mGMSAppCleanPolicyList = null;
    private ArrayMap<AppMngConstant.EnumWithDesc, ArrayMap<String, ListItem>> mGMSAppStartPolicyList = null;
    private ArraySet<String> mGmsAppPkg = new ArraySet<>();
    private ArraySet<String> mGmsCallerAppPkg = new ArraySet<>();
    private SparseSet mGmsCallerAppUid = new SparseSet();
    private final Set<String> mGoodPushSDKClsList = new ArraySet();
    private long mGoogleConnDealyTime = 604800000;
    private boolean mGoogleConnStat = false;
    private boolean mGoogleConnStatDecay = false;
    private long mGoogleDisConnTime = 0;
    private ContentObserver mHSMObserver = null;
    private List<String> mHabbitTopN = null;
    /* access modifiers changed from: private */
    public IntlRecgHandler mHandler = null;
    private final Set<String> mHwStopUserIdPkg = new ArraySet();
    private AwareBroadcastPolicy mIawareBrPolicy = null;
    private final AtomicInteger mInitWallPaperCount = new AtomicInteger(2);
    private boolean mIsAbroadArea = true;
    private final boolean mIsAppMngEnhance = SystemProperties.getBoolean("persist.sys.iaware.mngenhance", false);
    /* access modifiers changed from: private */
    public boolean mIsChinaOperator = false;
    private AtomicBoolean mIsDozeObsvInit = new AtomicBoolean(false);
    private boolean mIsGmsCoreValid = true;
    private boolean mIsGmsPhone = true;
    private boolean mIsInitGoogleConfig = false;
    private AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private AtomicBoolean mIsObsvInit = new AtomicBoolean(false);
    private AtomicBoolean mIsScreenOn = new AtomicBoolean(true);
    private boolean mIsScreenOnPm = true;
    private AtomicLong mLastPerceptionTime = new AtomicLong(0);
    private volatile ArrayMap<Integer, Long> mLastUpdateTime = new ArrayMap<>();
    private MultiTaskManagerService mMtmService = null;
    private int mNotCleanDuration = 60000;
    /* access modifiers changed from: private */
    public final Set<String> mNotCleanPkgList = new ArraySet();
    private PowerKit mPGSdk = null;
    private PackageManager mPackageManager = null;
    private Set<String> mPayPkgList = null;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /* class com.android.server.rms.iaware.appmng.AwareIntelligentRecg.AnonymousClass2 */
        private String mLastOperator = "";

        public void onServiceStateChanged(ServiceState ss) {
            if (ss != null && ss.getState() == 0) {
                String operatorInfo = ss.getOperatorNumeric();
                if (operatorInfo != null && !operatorInfo.equals(this.mLastOperator) && operatorInfo.length() >= 3) {
                    if ("460".equals(operatorInfo.substring(0, 3))) {
                        boolean unused = AwareIntelligentRecg.this.mIsChinaOperator = true;
                    } else {
                        boolean unused2 = AwareIntelligentRecg.this.mIsChinaOperator = false;
                    }
                }
                this.mLastOperator = operatorInfo;
            }
        }
    };
    private final Set<String> mPushSDKClsList = new ArraySet();
    private Set<String> mRecent = null;
    private final SparseArray<ArrayMap<String, Long>> mRegKeepAlivePkgs = new SparseArray<>();
    private final LinkedList<Long> mScreenChangedTime = new LinkedList<>();
    private final SparseSet mScreenRecordAppList = new SparseSet();
    private final SparseSet mScreenRecordPidList = new SparseSet();
    private ContentObserver mSettingsObserver = null;
    private Set<String> mSharePkgList = null;
    private final List<String> mSmallSampleList = new ArrayList();
    private final SparseSet mStatusAudioIn = new SparseSet();
    private final SparseSet mStatusAudioOut = new SparseSet();
    private final SparseSet mStatusGps = new SparseSet();
    private final SparseSet mStatusSensor = new SparseSet();
    private final SparseSet mStatusUpDown = new SparseSet();
    private final Map<Integer, Long> mStatusUpDownElapse = new ArrayMap();
    private final Set<String> mTTSPkg = new ArraySet();
    private int mTempHTopN = 0;
    private int mTempRecent = 0;
    private final SparseArray<AwareProcessWindowInfo> mToasts = new SparseArray<>();
    private SparseSet mUnRemindAppTypeList = null;
    private long mUpdateTimeHabbitTopN = -1;
    private long mUpdateTimeRecent = -1;
    private int mWebViewUid;
    private int mWidgetCheckUpdateCnt = 5;
    private long mWidgetCheckUpdateInterval = 86400000;

    public interface IAwareToastCallback {
        void onToastWindowsChanged(int i, int i2);
    }

    private AwareIntelligentRecg() {
    }

    public static synchronized AwareIntelligentRecg getInstance() {
        AwareIntelligentRecg awareIntelligentRecg;
        synchronized (AwareIntelligentRecg.class) {
            if (mAwareIntlgRecg == null) {
                mAwareIntlgRecg = new AwareIntelligentRecg();
            }
            awareIntelligentRecg = mAwareIntlgRecg;
        }
        return awareIntelligentRecg;
    }

    /* access modifiers changed from: private */
    public class IntlRecgHandler extends Handler {
        public IntlRecgHandler(Looper looper) {
            super(looper);
        }

        /* JADX INFO: Multiple debug info for r0v1 int: [D('uid' int), D('alarm' com.android.server.rms.iaware.appmng.AwareIntelligentRecg$AlarmInfo)] */
        private void handleMessageBecauseOfNSIQ(Message msg) {
            String pkg = null;
            String pkg2 = null;
            AlarmInfo alarm = null;
            switch (msg.what) {
                case 8:
                case 9:
                case 10:
                    int uid = msg.arg1;
                    if (msg.obj instanceof String) {
                        pkg = (String) msg.obj;
                    }
                    AwareIntelligentRecg.this.handlePerceptionEvent(uid, pkg, msg.what);
                    return;
                case 11:
                    if (msg.obj instanceof AlarmInfo) {
                        alarm = (AlarmInfo) msg.obj;
                    }
                    AwareIntelligentRecg.this.handleUnPerceptionEvent(alarm, msg.what);
                    return;
                case 12:
                default:
                    handleMessageEx(msg);
                    return;
                case 13:
                    if (msg.obj instanceof String) {
                        pkg2 = (String) msg.obj;
                    }
                    AwareIntelligentRecg.this.handleWallpaperSetMessage(pkg2);
                    return;
                case 14:
                    AwareIntelligentRecg.this.initDefaultWallPaper();
                    return;
                case 15:
                    AwareIntelligentRecg.this.handleInputMethodSetMessage();
                    return;
                case 16:
                    AwareIntelligentRecg.this.checkAlarmCmpDataForUnistalled();
                    return;
                case 17:
                    synchronized (AwareIntelligentRecg.this.mNotCleanPkgList) {
                        AwareIntelligentRecg.this.mNotCleanPkgList.clear();
                    }
                    return;
                case 18:
                    AwareIntelligentRecg.this.handleWakenessChangeHandle();
                    return;
                case 19:
                    AwareIntelligentRecg.this.handleWidgetUpDownLoadClear();
                    return;
            }
        }

        private void handleMessageEx(Message msg) {
            if (msg.what == 20) {
                AwareIntelligentRecg.this.updateBleState();
            }
        }

        public void handleMessage(Message msg) {
            if (AwareIntelligentRecg.DEBUG) {
                AwareLog.i(AwareIntelligentRecg.TAG, "handleMessage message " + msg.what);
            }
            int i = msg.what;
            if (i != 12) {
                AwareAppStartInfo startinfo = null;
                switch (i) {
                    case 0:
                        AwareIntelligentRecg.this.handleReportAppUpdateMsg(msg);
                        return;
                    case 1:
                        AwareIntelligentRecg.this.initRecgResultInfo();
                        return;
                    case 2:
                        if (msg.obj instanceof CmpTypeInfo) {
                            startinfo = (CmpTypeInfo) msg.obj;
                        }
                        boolean unused = AwareIntelligentRecg.this.insertCmpRecgInfo(startinfo);
                        return;
                    case 3:
                        if (msg.obj instanceof CmpTypeInfo) {
                            startinfo = (CmpTypeInfo) msg.obj;
                        }
                        boolean unused2 = AwareIntelligentRecg.this.deleteCmpRecgInfo(startinfo);
                        return;
                    case 4:
                        if (msg.obj instanceof AwareAppStartInfo) {
                            startinfo = (AwareAppStartInfo) msg.obj;
                        }
                        if (AwareIntelligentRecg.this.needRecgPushSDK(startinfo)) {
                            AwareIntelligentRecg.this.recordStartInfo(startinfo);
                            AwareIntelligentRecg.this.adjustPushSDKCmp(startinfo);
                            return;
                        }
                        return;
                    case 5:
                        AwareIntelligentRecg.this.initPGSDK();
                        return;
                    case 6:
                        if (AwareIntelligentRecg.this.mDataMgr != null) {
                            AwareIntelligentRecg.this.mDataMgr.flushBootCacheDataToDisk();
                            return;
                        }
                        return;
                    case 7:
                        AwareIntelligentRecg.this.updateKbgStatus(msg);
                        return;
                    default:
                        handleMessageBecauseOfNSIQ(msg);
                        return;
                }
            } else {
                AwareIntelligentRecg.this.handleUpdateDB();
            }
        }
    }

    /* access modifiers changed from: private */
    public void initPGSDK() {
        if (this.mPGSdk == null) {
            this.mPGSdk = PowerKit.getInstance();
            PowerKit powerKit = this.mPGSdk;
            if (powerKit == null) {
                delayConnectPGSDK();
                return;
            }
            try {
                powerKit.enableStateEvent(this.mAppFreezeListener, 6);
                this.mPGSdk.enableStateEvent(this.mAppFreezeListener, 7);
                this.mPGSdk.enableStateEvent(this.mAppFreezeListener, 8);
                this.mPGSdk.enableStateEvent(this.mAppFreezeListener, 2);
                this.mPGSdk.enableStateEvent(this.mAppFreezeListener, 10);
                this.mPGSdk.enableStateEvent(this.mAppFreezeListener, 100);
                this.mPGSdk.enableStateEvent(this.mAppFreezeListener, 12);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "registerPGListener happend RemoteException!");
                this.mPGSdk = null;
                delayConnectPGSDK();
            }
        }
    }

    private void delayConnectPGSDK() {
        this.mHandler.removeMessages(5);
        this.mHandler.sendEmptyMessageDelayed(5, 5000);
    }

    private void unRegisterPGListener() {
        PowerKit powerKit = this.mPGSdk;
        if (powerKit != null) {
            try {
                powerKit.disableStateEvent(this.mAppFreezeListener, 6);
                this.mPGSdk.disableStateEvent(this.mAppFreezeListener, 7);
                this.mPGSdk.disableStateEvent(this.mAppFreezeListener, 8);
                this.mPGSdk.disableStateEvent(this.mAppFreezeListener, 2);
                this.mPGSdk.disableStateEvent(this.mAppFreezeListener, 10);
                this.mPGSdk.disableStateEvent(this.mAppFreezeListener, 100);
                this.mPGSdk.disableStateEvent(this.mAppFreezeListener, 12);
            } catch (RemoteException e) {
                AwareLog.e(TAG, "unRegisterPGListener  happend RemoteException!");
            }
        }
    }

    /* access modifiers changed from: private */
    public void resolvePids(String pkgs, int uid, int eventType) {
        int state;
        if (AwareBroadcastDebug.getFilterDebug()) {
            AwareLog.i(TAG, "iaware_brFilter freeze pkgs:" + pkgs + ", uid:" + uid + ", eventType:" + eventType);
        }
        if (pkgs != null) {
            if (eventType == 1) {
                state = 1;
            } else if (eventType == 2) {
                state = 2;
            } else {
                return;
            }
            String[] pkglist = pkgs.split(":");
            int i = 0;
            int count = pkglist.length;
            while (i < count) {
                try {
                    ProcessInfoCollector.getInstance().setAwareProcessState(Integer.parseInt(pkglist[i]), uid, state);
                    i++;
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "pid param error");
                    return;
                }
            }
        }
    }

    public void addScreenRecord(int uid, int pid) {
        synchronized (this.mScreenRecordAppList) {
            this.mScreenRecordAppList.add(uid);
        }
        synchronized (this.mScreenRecordPidList) {
            this.mScreenRecordPidList.add(pid);
        }
    }

    public void removeScreenRecord(int uid, int pid) {
        synchronized (this.mScreenRecordAppList) {
            this.mScreenRecordAppList.remove(uid);
        }
        synchronized (this.mScreenRecordPidList) {
            this.mScreenRecordPidList.remove(pid);
        }
    }

    public boolean isScreenRecord(AwareProcessInfo awareProcInfo) {
        if (!mEnabled || awareProcInfo == null) {
            return false;
        }
        return isScreenRecordEx(awareProcInfo.procProcInfo);
    }

    public boolean isScreenRecordEx(ProcessInfo procInfo) {
        boolean contains;
        if (!mEnabled || procInfo == null) {
            return false;
        }
        synchronized (this.mScreenRecordAppList) {
            contains = this.mScreenRecordAppList.contains(procInfo.mAppUid);
        }
        return contains;
    }

    public void removeDiedScreenProc(int uid, int pid) {
        synchronized (this.mScreenRecordPidList) {
            if (this.mScreenRecordPidList.contains(pid)) {
                synchronized (this.mScreenRecordAppList) {
                    this.mScreenRecordAppList.remove(uid);
                }
            }
        }
    }

    public void addCamera(int uid) {
        synchronized (this.mCameraUseAppList) {
            this.mCameraUseAppList.add(uid);
        }
    }

    public void removeCamera(int uid) {
        synchronized (this.mCameraUseAppList) {
            this.mCameraUseAppList.remove(uid);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0014, code lost:
        if (r4 == null) goto L_0x0043;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0018, code lost:
        if (r4.procProcInfo != null) goto L_0x001b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x001b, code lost:
        r0 = r4.procProcInfo.mAppUid;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0027, code lost:
        if (com.android.server.rms.iaware.appmng.AwareAppAssociate.getInstance().isForeGroundApp(r0) == false) goto L_0x002a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0029, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0032, code lost:
        if (com.android.server.rms.iaware.appmng.AwareAppAssociate.getInstance().hasWindow(r0) != false) goto L_0x0035;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0034, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0035, code lost:
        r2 = r3.mCameraUseAppList;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0037, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:?, code lost:
        r1 = r3.mCameraUseAppList.contains(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x003e, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x003f, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0043, code lost:
        return false;
     */
    public boolean isCameraRecord(AwareProcessInfo awareProcInfo) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mCameraUseAppList) {
            if (this.mCameraUseAppList.isEmpty()) {
                return false;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleReportAppUpdateMsg(Message msg) {
        int eventId = msg.arg1;
        Bundle args = msg.getData();
        if (eventId == 1) {
            String pkgName = args.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
            if (pkgName != null) {
                initializeForInstallApp(pkgName);
                updateIsGmsCoreValid(pkgName);
            }
        } else if (eventId == 2) {
            String pkgName2 = args.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
            int uid = args.getInt("uid");
            int userId = UserHandle.getUserId(uid);
            AwareComponentPreloadManager.getInstance().reportUninstallApp(pkgName2, userId);
            if (pkgName2 != null && userId == this.mCurUserId) {
                initializeForUninstallApp(pkgName2);
                clearAlarmForUninstallApp(pkgName2, uid);
                setHwStopFlag(userId, pkgName2, false);
                updateAllowStartPkgs(userId, pkgName2, false);
                updateIsGmsCoreValid(pkgName2);
            } else if (this.mCurUserId == 0 && isCloneUserId(userId)) {
                setHwStopFlag(userId, pkgName2, false);
            }
        }
    }

    private void initialize() {
        if (!this.mIsInitialized.get()) {
            if (this.mMtmService == null) {
                this.mMtmService = MultiTaskManagerService.self();
            }
            if (this.mMtmService != null) {
                DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.APP_START, this.mMtmService.context());
                DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.COMMON, this.mMtmService.context());
                if (this.mPackageManager == null) {
                    this.mPackageManager = this.mMtmService.context().getPackageManager();
                }
            }
            if (this.mHandler == null) {
                this.mHandler = new IntlRecgHandler(BackgroundThread.get().getLooper());
            }
            if (this.mSettingsObserver == null) {
                this.mSettingsObserver = new SettingsObserver(this.mHandler);
            }
            if (this.mHSMObserver == null) {
                this.mHSMObserver = new HSMObserver(this.mHandler);
            }
            try {
                UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
                if (currentUser != null) {
                    this.mCurUserId = currentUser.id;
                }
            } catch (RemoteException e) {
                AwareLog.e(TAG, "getCurrentUserId RemoteException");
            }
            MultiTaskManagerService multiTaskManagerService = this.mMtmService;
            if (multiTaskManagerService != null) {
                unregisterContentObserver(multiTaskManagerService.context(), this.mSettingsObserver);
                registerContentObserver(this.mMtmService.context(), this.mSettingsObserver);
                unregisterHsmProviderObserver(this.mMtmService.context(), this.mHSMObserver);
                registerHsmProviderObserver(this.mMtmService.context(), this.mHSMObserver);
            }
            this.mIsAbroadArea = AwareDefaultConfigList.isAbroadArea();
            this.mDeviceLevel = DeviceInfo.getDeviceLevel();
            initializeForUser();
            initPushSDK();
            sendLoadAppTypeMessage();
            initToastWindows();
            initPGSDK();
            initPayPkg();
            initSharePkg();
            initAlarmPkg();
            initUnRemindAppType();
            initAppStatus();
            initMinWindowSize();
            MultiTaskManagerService multiTaskManagerService2 = this.mMtmService;
            if (multiTaskManagerService2 != null) {
                updateGmsCallerAppInit(multiTaskManagerService2.context());
                updateNetWorkOperatorInit(this.mMtmService.context());
            }
            initCommonInfo();
            AppStartupUtil.initCtsPkgList();
            initAppMngCustPropConfig();
            sendScreenOnFromPmMsg(true);
            sendMsgWidgetUpDownLoadClear();
            this.mIsInitialized.set(true);
            this.mBluetoothUid = getUIDByPackageName("com.android.bluetooth");
            this.mWebViewUid = getUIDByPackageName(WebViewZygote.getPackageName());
            initMemoryInfo();
            CloudPushManager.getInstance();
            initGmsControlInfo();
        }
    }

    public void updateAppMngConfig() {
        initPayPkg();
        initSharePkg();
        loadPushSDK();
        initBlindPkg();
        initAlarmPkg();
        initUnRemindAppType();
        updateAccessbilityService();
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null) {
            updateGmsCallerAppInit(multiTaskManagerService.context());
        }
        AwareAppMngSort.getInstance().updateAppMngConfig();
        initGmsAppConfig();
        initCommonInfo();
        initGmsControlInfo();
    }

    private void loadPushSDK() {
        initPushSDK();
        loadPushSDKResultFromDB();
    }

    private List<CmpTypeInfo> readCmpTypeInfoFromDB() {
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                return IAwareCMSManager.getCmpTypeList(awareservice);
            }
            AwareLog.e(TAG, "can not find service IAwareCMSService.");
            return null;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "loadRecgResultInfo RemoteException");
            return null;
        }
    }

    private void updatePushSDKClsFromDB(String cls) {
        if (isInGoodPushSDKList(cls)) {
            sendUpdateCmpInfoMessage("", cls, 0, false);
            return;
        }
        synchronized (this.mPushSDKClsList) {
            this.mPushSDKClsList.add(cls);
        }
    }

    private void loadPushSDKResultFromDB() {
        List<CmpTypeInfo> list = readCmpTypeInfoFromDB();
        if (list != null) {
            for (CmpTypeInfo info : list) {
                String cls = info.getCls();
                if (info.getType() == 0) {
                    updatePushSDKClsFromDB(cls);
                }
            }
        }
    }

    private boolean isInGoodPushSDKList(String cls) {
        if (cls == null) {
            return false;
        }
        synchronized (this.mGoodPushSDKClsList) {
            if (this.mGoodPushSDKClsList.contains(cls)) {
                return true;
            }
            return false;
        }
    }

    private void initMinWindowSize() {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService == null) {
            AwareLog.i(TAG, "mMtmService is null for min window size");
            return;
        }
        Context context = multiTaskManagerService.context();
        if (context == null) {
            AwareLog.i(TAG, "context is null for  min window size");
            return;
        }
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm == null) {
            AwareLog.i(TAG, "dm is null for  min window size");
            return;
        }
        AwareProcessWindowInfo.setMinWindowWidth((int) (((float) AwareProcessWindowInfo.getMinWindowWidth()) * dm.density));
        AwareProcessWindowInfo.setMinWindowHeight((int) (((float) AwareProcessWindowInfo.getMinWindowHeight()) * dm.density));
        AwareLog.i(TAG, "initialize start!" + AwareProcessWindowInfo.getMinWindowWidth());
    }

    private void initializeForUser() {
        initTTSPkg();
        initSettings();
    }

    private void deInitialize() {
        if (this.mIsInitialized.get()) {
            synchronized (this.mAccessPkg) {
                this.mAccessPkg.clear();
            }
            synchronized (this.mTTSPkg) {
                this.mTTSPkg.clear();
            }
            synchronized (this.mGoodPushSDKClsList) {
                this.mGoodPushSDKClsList.clear();
            }
            synchronized (this.mPushSDKClsList) {
                this.mPushSDKClsList.clear();
            }
            synchronized (this.mAppStartInfoList) {
                this.mAppStartInfoList.clear();
            }
            synchronized (this.mToasts) {
                this.mToasts.clear();
                notifyToastChange(3, -1);
            }
            synchronized (this.mFrozenAppList) {
                this.mFrozenAppList.clear();
            }
            synchronized (this.mBluetoothAppList) {
                this.mBluetoothAppList.clear();
            }
            synchronized (this.mAudioOutInstant) {
                this.mAudioOutInstant.clear();
            }
            synchronized (this.mBlindPkg) {
                this.mBlindPkg.clear();
            }
            unRegisterPGListener();
            this.mPGSdk = null;
            synchronized (this.mRegKeepAlivePkgs) {
                this.mRegKeepAlivePkgs.clear();
            }
            synchronized (this.mAlarmMap) {
                this.mAlarmMap.clear();
            }
            synchronized (this.mAlarmCmps) {
                this.mAlarmCmps.clear();
            }
            synchronized (this.mSmallSampleList) {
                this.mSmallSampleList.clear();
            }
            synchronized (this.mScreenChangedTime) {
                this.mScreenChangedTime.clear();
            }
            synchronized (this.mAppChangeToBGTime) {
                this.mAppChangeToBGTime.clear();
            }
            synchronized (this.mHwStopUserIdPkg) {
                this.mHwStopUserIdPkg.clear();
            }
            synchronized (this.mScreenRecordAppList) {
                this.mScreenRecordAppList.clear();
            }
            synchronized (this.mScreenRecordPidList) {
                this.mScreenRecordPidList.clear();
            }
            synchronized (this.mCameraUseAppList) {
                this.mCameraUseAppList.clear();
            }
            MultiTaskManagerService multiTaskManagerService = this.mMtmService;
            if (multiTaskManagerService != null) {
                unregisterContentObserver(multiTaskManagerService.context(), this.mSettingsObserver);
                unregisterContentObserver(this.mMtmService.context(), this.mHSMObserver);
            }
            this.mDefaultInputMethod = "";
            this.mDefaultWallPaper = "";
            this.mDefaultTTS = "";
            this.mDefaultSms = "";
            deInitAppStatus();
            this.mIsInitialized.set(false);
        }
    }

    public void initUserSwitch(int userId) {
        if (mEnabled) {
            this.mCurUserId = userId;
            initializeForUser();
            sendCheckCmpDataMessage();
        }
    }

    private void initializeForInstallApp(String pkg) {
        initSpecTTSPkg(pkg);
        updateGmsCallerAppForInstall(pkg);
    }

    private void initializeForUninstallApp(String pkg) {
        removeSpecTTSPkg(pkg);
    }

    private void clearAlarmForUninstallApp(String pkgName, int uid) {
        deleteAppCmpRecgInfo(UserHandle.getUserId(uid), pkgName);
        removeAlarmCmp(uid, pkgName);
        removeAlarms(uid);
    }

    private void clearAlarmForRemoveUser(int userid) {
        deleteAppCmpRecgInfo(userid, null);
        removeAlarmCmpByUserid(userid);
        removeAlarmsByUserid(userid);
    }

    private void sendLoadAppTypeMessage() {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 1;
        this.mHandler.sendMessageDelayed(msg, 20000);
    }

    private void sendUpdateDbMessage() {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 12;
        this.mHandler.sendMessage(msg);
    }

    private void sendUpdateCmpInfoMessage(String pkg, String cls, int type, boolean added) {
        CmpTypeInfo info = new CmpTypeInfo();
        info.setPkgName(pkg);
        info.setCls(cls);
        info.setType(type);
        info.setTime(System.currentTimeMillis());
        Message msg = this.mHandler.obtainMessage();
        msg.obj = info;
        msg.what = added ? 2 : 3;
        this.mHandler.sendMessage(msg);
    }

    private void sendRecgPushSdkMessage(int callerUid, String compName, int targetUid) {
        if (DEBUG) {
            AwareLog.d(TAG, "PushSDK callerUid: " + callerUid + " cmp:" + compName + " targetUid: " + targetUid);
        }
        AwareAppStartInfo appStartInfo = new AwareAppStartInfo(callerUid, compName);
        Message msg = this.mHandler.obtainMessage();
        msg.obj = appStartInfo;
        msg.what = 4;
        this.mHandler.sendMessage(msg);
    }

    /* access modifiers changed from: private */
    public void sendPerceptionMessage(int msgid, int uid, String pkg) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = msgid;
        msg.arg1 = uid;
        msg.obj = pkg;
        this.mHandler.sendMessage(msg);
    }

    private void sendUnPerceptionMessage(AlarmInfo alarm) {
        if (alarm != null && !AwareAppAssociate.getInstance().isForeGroundApp(alarm.uid)) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 11;
            msg.obj = alarm;
            this.mHandler.sendMessageDelayed(msg, HwArbitrationDEFS.WIFI_RX_BYTES_THRESHOLD);
        }
    }

    private void sendInitWallPaperMessage() {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 14;
        this.mHandler.sendMessageDelayed(msg, 20000);
    }

    private void initPushSDK() {
        ArrayList<String> badList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), PUSHSDK_BAD);
        if (badList != null) {
            synchronized (this.mPushSDKClsList) {
                this.mPushSDKClsList.clear();
                this.mPushSDKClsList.addAll(badList);
            }
        }
        ArrayList<String> goodList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), PUSHSDK_GOOD);
        if (goodList != null) {
            synchronized (this.mGoodPushSDKClsList) {
                this.mGoodPushSDKClsList.clear();
                this.mGoodPushSDKClsList.addAll(goodList);
            }
        }
    }

    private void initPayPkg() {
        Collection<? extends String> pkgList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "payPkgList");
        if (pkgList != null) {
            Set<String> pkgSet = new ArraySet<>();
            pkgSet.addAll(pkgList);
            this.mPayPkgList = pkgSet;
        }
    }

    private void initSharePkg() {
        Collection<? extends String> pkgList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "sharePkgList");
        if (pkgList != null) {
            Set<String> pkgSet = new ArraySet<>();
            pkgSet.addAll(pkgList);
            this.mSharePkgList = pkgSet;
        }
    }

    private void initAlarmPkg() {
        Collection<? extends String> alarmList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "alarmPkgList");
        if (alarmList != null) {
            Set<String> alarmSet = new ArraySet<>();
            alarmSet.addAll(alarmList);
            this.mAlarmPkgList = alarmSet;
        }
    }

    private Integer[] stringToIntArray(String str) {
        if (str == null || str.isEmpty()) {
            return new Integer[0];
        }
        String[] strs = str.split(",");
        Integer[] array = new Integer[strs.length];
        try {
            int length = strs.length;
            for (int i = 0; i < length; i++) {
                array[i] = Integer.valueOf(Integer.parseInt(strs[i]));
            }
            return array;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "stringToIntArray args is illegal!");
            return new Integer[0];
        }
    }

    private void initUnRemindAppType() {
        String unRemindTag;
        int i = this.mDeviceLevel;
        if (i == 3 || i == 2) {
            unRemindTag = "unRemindAppTypesLowLevel";
        } else {
            unRemindTag = "unRemindAppTypes";
        }
        ArrayList<String> appTypeList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), unRemindTag);
        AwareLog.i(TAG, "initUnRemindAppType " + appTypeList);
        if (appTypeList != null) {
            SparseSet unRemindAppSet = new SparseSet();
            int size = appTypeList.size();
            for (int i2 = 0; i2 < size; i2++) {
                Integer[] types = stringToIntArray(appTypeList.get(i2));
                if (types.length > 0) {
                    List<Integer> typeList = Arrays.asList(types);
                    AwareLog.i(TAG, "initUnRemindAppType integer " + typeList);
                    unRemindAppSet.addAll(typeList);
                }
            }
            this.mUnRemindAppTypeList = unRemindAppSet;
        }
    }

    /* access modifiers changed from: private */
    public void initRecgResultInfo() {
        if (loadRecgResultInfo()) {
            AwareLog.i(TAG, "AwareIntelligentRecg load recg pkg OK ");
        } else if (this.mDataLoadCount.get() >= 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("AwareIntelligentRecg send load recg pkg message again mDataLoadCount=");
            sb.append(this.mDataLoadCount.get() - 1);
            AwareLog.i(TAG, sb.toString());
            sendLoadAppTypeMessage();
            this.mDataLoadCount.decrementAndGet();
        } else {
            AwareLog.e(TAG, "AwareIntelligentRecg recg service is error");
        }
    }

    private void initAppStatus() {
        if (this.mAwareStateCallback == null) {
            this.mAwareStateCallback = new AwareStateCallback();
            AwareAppKeyBackgroup keyBackgroupInstance = AwareAppKeyBackgroup.getInstance();
            keyBackgroupInstance.registerStateCallback(this.mAwareStateCallback, 1);
            keyBackgroupInstance.registerStateCallback(this.mAwareStateCallback, 2);
            keyBackgroupInstance.registerStateCallback(this.mAwareStateCallback, 3);
            keyBackgroupInstance.registerStateCallback(this.mAwareStateCallback, 4);
            keyBackgroupInstance.registerStateCallback(this.mAwareStateCallback, 5);
        }
    }

    private void deInitAppStatus() {
        if (this.mAwareStateCallback != null) {
            AwareAppKeyBackgroup keyBackgroupInstance = AwareAppKeyBackgroup.getInstance();
            keyBackgroupInstance.unregisterStateCallback(this.mAwareStateCallback, 1);
            keyBackgroupInstance.unregisterStateCallback(this.mAwareStateCallback, 2);
            keyBackgroupInstance.unregisterStateCallback(this.mAwareStateCallback, 3);
            keyBackgroupInstance.unregisterStateCallback(this.mAwareStateCallback, 4);
            keyBackgroupInstance.unregisterStateCallback(this.mAwareStateCallback, 5);
            this.mAwareStateCallback = null;
        }
    }

    private class AwareStateCallback implements AwareAppKeyBackgroup.IAwareStateCallback {
        private AwareStateCallback() {
        }

        @Override // com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback
        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            Message msg = AwareIntelligentRecg.this.mHandler.obtainMessage();
            msg.what = 7;
            msg.arg1 = stateType;
            msg.arg2 = eventType;
            msg.obj = Integer.valueOf(uid);
            AwareIntelligentRecg.this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void updateKbgStatus(Message msg) {
        SparseSet status;
        int stateType = msg.arg1;
        int eventType = msg.arg2;
        int uid = ((Integer) msg.obj).intValue();
        if (stateType == 1) {
            status = this.mStatusAudioIn;
        } else if (stateType == 2) {
            status = this.mStatusAudioOut;
        } else if (stateType == 3) {
            status = this.mStatusGps;
        } else if (stateType == 4) {
            status = this.mStatusSensor;
        } else if (stateType == 5) {
            status = this.mStatusUpDown;
            flushUpDownLoadTrigTime(uid);
        } else {
            return;
        }
        if (1 == eventType) {
            synchronized (status) {
                if (!status.contains(uid)) {
                    status.add(uid);
                }
            }
        } else if (2 == eventType) {
            synchronized (status) {
                if (status.contains(uid)) {
                    status.remove(uid);
                }
            }
        }
    }

    private boolean isAudioInStatus(int uid) {
        synchronized (this.mStatusAudioIn) {
            if (this.mStatusAudioIn.contains(uid)) {
                return true;
            }
            return false;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0019, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x001a, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0017, code lost:
        if (com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.getInstance().isAudioCache(r4) == false) goto L_0x001a;
     */
    private boolean isAudioOutStatus(int uid) {
        synchronized (this.mStatusAudioOut) {
            if (this.mStatusAudioOut.contains(uid)) {
                return true;
            }
        }
    }

    private boolean isGpsStatus(int uid) {
        synchronized (this.mStatusGps) {
            if (this.mStatusGps.contains(uid)) {
                return true;
            }
            return false;
        }
    }

    private boolean isUpDownStatus(int uid) {
        synchronized (this.mStatusUpDown) {
            if (this.mStatusUpDown.contains(uid)) {
                return true;
            }
            return false;
        }
    }

    private boolean isSensorStatus(int uid) {
        synchronized (this.mStatusSensor) {
            if (this.mStatusSensor.contains(uid)) {
                return true;
            }
            return false;
        }
    }

    private boolean loadRecgResultInfo() {
        List<CmpTypeInfo> list = readCmpTypeInfoFromDB();
        if (list == null) {
            return false;
        }
        for (CmpTypeInfo info : list) {
            if (DEBUG) {
                AwareLog.d(TAG, "loadRecgResultInfo info " + info.toString());
            }
            int type = info.getType();
            if (type == 0) {
                updatePushSDKClsFromDB(info.getCls());
            } else if (type == 3 || type == 4) {
                synchronized (this.mAlarmCmps) {
                    ArrayMap<String, CmpTypeInfo> arrayMap = this.mAlarmCmps;
                    arrayMap.put(info.getUserId() + "#" + info.getPkgName() + "#" + info.getCls(), info);
                }
            }
        }
        return true;
    }

    private void sendCheckCmpDataMessage() {
        AwareLog.v(TAG, "checkCmpDataForUnistalled start0000");
        IntlRecgHandler intlRecgHandler = this.mHandler;
        if (intlRecgHandler != null) {
            if (intlRecgHandler.hasMessages(16)) {
                this.mHandler.removeMessages(16);
            }
            Message msg = this.mHandler.obtainMessage();
            msg.what = 16;
            this.mHandler.sendMessageDelayed(msg, AppHibernateCst.DELAY_ONE_MINS);
        }
    }

    /* access modifiers changed from: private */
    public void checkAlarmCmpDataForUnistalled() {
        List<PackageInfo> installedApps = InnerUtils.getAllInstalledAppsInfo(this.mCurUserId);
        if (installedApps != null && !installedApps.isEmpty()) {
            checkCmpDataForUnistalled(installedApps);
            checkAlarmDataForUnistalled(installedApps);
        }
    }

    private void checkCmpDataForUnistalled(List<PackageInfo> installedApps) {
        int size = installedApps.size();
        AwareLog.v(TAG, "checkCmpDataForUnistalled app size:" + size);
        synchronized (this.mAlarmCmps) {
            Iterator<Map.Entry<String, CmpTypeInfo>> it = this.mAlarmCmps.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = it.next();
                String key = entry.getKey();
                CmpTypeInfo value = entry.getValue();
                if (value.getUserId() == this.mCurUserId) {
                    boolean exist = false;
                    int i = 0;
                    while (true) {
                        if (i >= size) {
                            break;
                        }
                        if (key.indexOf(this.mCurUserId + "#" + installedApps.get(i).packageName + "#") == 0) {
                            exist = true;
                            break;
                        }
                        i++;
                    }
                    if (!exist) {
                        AwareLog.v(TAG, "checkCmpDataForUnistalled remove cmp:" + value);
                        it.remove();
                        deleteCmpRecgInfo(value);
                    }
                }
            }
        }
    }

    private void checkAlarmDataForUnistalled(List<PackageInfo> installedApps) {
        int size = installedApps.size();
        AwareLog.v(TAG, "checkAlarmDataForUnistalled app size:" + size);
        synchronized (this.mAlarmMap) {
            for (int j = this.mAlarmMap.size() - 1; j >= 0; j--) {
                int uid = this.mAlarmMap.keyAt(j);
                if (UserHandle.getUserId(uid) == this.mCurUserId) {
                    boolean exist = false;
                    int i = 0;
                    while (true) {
                        if (i < size) {
                            ApplicationInfo aInfo = installedApps.get(i).applicationInfo;
                            if (aInfo != null && aInfo.uid == uid) {
                                exist = true;
                                break;
                            }
                            i++;
                        } else {
                            break;
                        }
                    }
                    if (!exist) {
                        AwareLog.v(TAG, "checkAlarmDataForUnistalled remove uid :" + uid);
                        this.mAlarmMap.removeAt(j);
                    }
                }
            }
        }
    }

    private void getToastWindows(SparseSet toastPids, SparseSet evilPids) {
        if (mEnabled && toastPids != null) {
            synchronized (this.mToasts) {
                for (int i = this.mToasts.size() - 1; i >= 0; i--) {
                    AwareLog.d(TAG, "getToastWindows pid:" + this.mToasts.keyAt(i));
                    if (!this.mToasts.valueAt(i).isEvil()) {
                        toastPids.add(this.mToasts.keyAt(i));
                    } else if (evilPids != null) {
                        evilPids.add(this.mToasts.keyAt(i));
                    }
                }
            }
            if (DEBUG) {
                AwareLog.d(TAG, "ToastPids:" + toastPids);
            }
        }
    }

    public boolean isToastWindows(int userid, String pkg) {
        if (!mEnabled || pkg == null) {
            return true;
        }
        synchronized (this.mToasts) {
            for (int i = this.mToasts.size() - 1; i >= 0; i--) {
                AwareProcessWindowInfo toastInfo = this.mToasts.valueAt(i);
                AwareLog.i(TAG, "[isToastWindows]:" + this.mToasts.keyAt(i) + " pkg:" + pkg + " isEvil:" + toastInfo.isEvil());
                if (pkg.equals(toastInfo.pkg) && ((userid == -1 || userid == UserHandle.getUserId(toastInfo.uid)) && !toastInfo.isEvil())) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isEvilToastWindow(int window, int code) {
        boolean result;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mToasts) {
            AwareProcessWindowInfo toastInfo = this.mToasts.get(window);
            if (toastInfo == null || !toastInfo.isEvil(code)) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x00d1  */
    private void initToastWindows() {
        Iterator<Bundle> it;
        boolean isEvil;
        AwareProcessWindowInfo toastInfo;
        List<Bundle> windowsList = HwWindowManager.getVisibleWindows(45);
        if (windowsList == null) {
            AwareLog.w(TAG, "Catch null when initToastWindows.");
            return;
        }
        synchronized (this.mToasts) {
            this.mToasts.clear();
            notifyToastChange(3, -1);
            Iterator<Bundle> it2 = windowsList.iterator();
            while (it2.hasNext()) {
                Bundle windowState = it2.next();
                if (windowState != null) {
                    int window = windowState.getInt("window_pid");
                    int mode = windowState.getInt("window_value");
                    int code = windowState.getInt("window_state");
                    int width = windowState.getInt("window_width");
                    int height = windowState.getInt("window_height");
                    float alpha = windowState.getFloat("window_alpha");
                    boolean phide = windowState.getBoolean("window_hidden");
                    String pkg = windowState.getString("window_package");
                    int uid = windowState.getInt("window_uid");
                    if (DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        it = it2;
                        sb.append("initToastWindows pid:");
                        sb.append(window);
                        sb.append(" mode:");
                        sb.append(mode);
                        sb.append(" code:");
                        sb.append(code);
                        sb.append(" width:");
                        sb.append(width);
                        sb.append(" height:");
                        sb.append(height);
                        AwareLog.i(TAG, sb.toString());
                    } else {
                        it = it2;
                    }
                    if (!(width == AwareProcessWindowInfo.getMinWindowWidth() || height == AwareProcessWindowInfo.getMinWindowHeight() || alpha == 0.0f)) {
                        if (!phide) {
                            isEvil = false;
                            toastInfo = this.mToasts.get(window);
                            if (toastInfo == null) {
                                toastInfo = new AwareProcessWindowInfo(mode, pkg, uid);
                                this.mToasts.put(window, toastInfo);
                                notifyToastChange(5, window);
                            }
                            toastInfo.addWindow(Integer.valueOf(code), isEvil);
                            it2 = it;
                        }
                    }
                    isEvil = true;
                    toastInfo = this.mToasts.get(window);
                    if (toastInfo == null) {
                    }
                    toastInfo.addWindow(Integer.valueOf(code), isEvil);
                    it2 = it;
                }
            }
        }
    }

    private void addToast(int window, int code, int width, int height, float alpha, String pkg, int uid) {
        AwareLog.i(TAG, "[addToast]:" + window + " [code]:" + code + " width:" + width + " height:" + height + " alpha:" + alpha);
        if (window > 0) {
            synchronized (this.mToasts) {
                AwareProcessWindowInfo toastInfo = this.mToasts.get(window);
                boolean isEvil = true;
                boolean isInvalidWidth = width > 0 && width <= AwareProcessWindowInfo.getMinWindowWidth();
                boolean isInvalidHeight = height > 0 && height <= AwareProcessWindowInfo.getMinWindowHeight();
                boolean isTransparent = alpha == 0.0f;
                if (!isInvalidWidth && !isInvalidHeight) {
                    if (!isTransparent) {
                        isEvil = false;
                    }
                }
                if (toastInfo == null) {
                    toastInfo = new AwareProcessWindowInfo(3, pkg, uid);
                    this.mToasts.put(window, toastInfo);
                    notifyToastChange(5, window);
                }
                toastInfo.addWindow(Integer.valueOf(code), isEvil);
            }
            if (DEBUG) {
                AwareLog.i(TAG, "[addToast]:" + window);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0050, code lost:
        if (com.android.server.rms.iaware.appmng.AwareIntelligentRecg.DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0052, code lost:
        android.rms.iaware.AwareLog.i(com.android.server.rms.iaware.appmng.AwareIntelligentRecg.TAG, "[removeToast]:" + r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:?, code lost:
        return;
     */
    private void removeToast(int window, int code) {
        AwareLog.i(TAG, "[removeToast]:" + window + " [code]:" + code);
        if (window > 0) {
            synchronized (this.mToasts) {
                AwareProcessWindowInfo toastInfo = this.mToasts.get(window);
                if (toastInfo == null) {
                    this.mToasts.remove(window);
                    return;
                }
                toastInfo.removeWindow(Integer.valueOf(code));
                if (toastInfo.windows.size() == 0) {
                    this.mToasts.remove(window);
                    notifyToastChange(4, window);
                }
            }
        }
    }

    private void updateToast(int window, int code, int width, int height, float alpha, boolean phide) {
        boolean isEvil;
        AwareLog.i(TAG, "[updateToast]:" + window + " [code]:" + code + " width:" + width + " height:" + height + " alpha:" + alpha);
        if (window > 0) {
            if (width >= 0 || height >= 0) {
                synchronized (this.mToasts) {
                    AwareProcessWindowInfo toastInfo = this.mToasts.get(window);
                    if (width > AwareProcessWindowInfo.getMinWindowWidth() && height > AwareProcessWindowInfo.getMinWindowHeight() && alpha != 0.0f) {
                        if (!phide) {
                            isEvil = false;
                            if (toastInfo != null && toastInfo.containsWindow(code)) {
                                toastInfo.addWindow(Integer.valueOf(code), isEvil);
                            }
                        }
                    }
                    isEvil = true;
                    toastInfo.addWindow(Integer.valueOf(code), isEvil);
                }
                if (DEBUG) {
                    AwareLog.i(TAG, "[updateToast]:" + window);
                }
            }
        }
    }

    private void hideToast(int window, int code) {
        AwareLog.i(TAG, "[hideToast]:" + window + " [code]:" + code);
        if (window > 0) {
            synchronized (this.mToasts) {
                AwareProcessWindowInfo toastInfo = this.mToasts.get(window);
                if (toastInfo != null && toastInfo.containsWindow(code)) {
                    toastInfo.addWindow(Integer.valueOf(code), true);
                }
            }
            if (DEBUG) {
                AwareLog.i(TAG, "[hideToast]:" + window);
            }
        }
    }

    private Map<String, CmpTypeInfo> getAlarmCmpFromDB() {
        Map<String, CmpTypeInfo> result = new ArrayMap<>();
        List<CmpTypeInfo> cmplist = null;
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                cmplist = IAwareCMSManager.getCmpTypeList(awareservice);
            } else {
                AwareLog.e(TAG, "can not find service IAwareCMSService.");
            }
        } catch (RemoteException e) {
            AwareLog.e(TAG, "getAlarmCmpFromDB RemoteException");
        }
        if (cmplist == null) {
            return result;
        }
        for (CmpTypeInfo info : cmplist) {
            if (info != null && (info.getType() == 3 || info.getType() == 4)) {
                result.put(info.getUserId() + "#" + info.getPkgName() + "#" + info.getCls(), info);
            }
        }
        return result;
    }

    /* access modifiers changed from: private */
    public boolean insertCmpRecgInfo(CmpTypeInfo info) {
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                IAwareCMSManager.insertCmpTypeInfo(awareservice, info);
                return true;
            }
            AwareLog.e(TAG, "can not find service IAwareCMSService.");
            return false;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "inSertCmpRecgInfo RemoteException");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean deleteCmpRecgInfo(CmpTypeInfo info) {
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                IAwareCMSManager.deleteCmpTypeInfo(awareservice, info);
                return true;
            }
            AwareLog.e(TAG, "can not find service IAwareCMSService.");
            return false;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "deleteCmpRecgInfo RemoteException");
            return false;
        }
    }

    private boolean deleteAppCmpRecgInfo(int userid, String pkg) {
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                IAwareCMSManager.deleteAppCmpTypeInfo(awareservice, userid, pkg);
                AwareLog.e(TAG, "delete pkg:" + pkg + " userid:" + userid + " from iAware.db");
                return true;
            }
            AwareLog.e(TAG, "can not find service IAwareCMSService.");
            return false;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "deleteAppCmpRecgInfo RemoteException");
            return false;
        }
    }

    public void report(int eventId, Bundle bundleArgs) {
        if (mEnabled) {
            if (DEBUG) {
                AwareLog.d(TAG, "eventId: " + eventId);
            }
            if (bundleArgs != null) {
                if (!this.mIsInitialized.get()) {
                    initialize();
                }
                if (eventId == 8) {
                    addToast(bundleArgs.getInt("window"), bundleArgs.getInt("hashcode"), bundleArgs.getInt("width"), bundleArgs.getInt("height"), bundleArgs.getFloat("alpha"), bundleArgs.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME), bundleArgs.getInt("uid"));
                } else if (eventId == 9) {
                    removeToast(bundleArgs.getInt("window"), bundleArgs.getInt("hashcode"));
                } else if (eventId == 17) {
                    reportWallpaper(bundleArgs.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                } else if (eventId == 22) {
                    reportAlarm(bundleArgs.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME), bundleArgs.getString("statstag"), bundleArgs.getInt("alarm_operation"), bundleArgs.getInt("tgtUid"));
                } else if (eventId == 20011) {
                    this.mIsScreenOn.set(true);
                } else if (eventId == 90011) {
                    this.mIsScreenOn.set(false);
                } else if (eventId == 19) {
                    sendPerceptionMessage(8, bundleArgs.getInt("tgtUid"), null);
                } else if (eventId != 20) {
                    switch (eventId) {
                        case 27:
                            updateToast(bundleArgs.getInt("window"), bundleArgs.getInt("hashcode"), bundleArgs.getInt("width"), bundleArgs.getInt("height"), bundleArgs.getFloat("alpha"), bundleArgs.getBoolean("permanentlyhidden"));
                            return;
                        case 28:
                            hideToast(bundleArgs.getInt("window"), bundleArgs.getInt("hashcode"));
                            return;
                        case 29:
                            int userid = bundleArgs.getInt("userid");
                            if (userid != -10000) {
                                clearAlarmForRemoveUser(userid);
                                removeHwStopFlagByUserId(userid);
                                return;
                            }
                            return;
                        default:
                            reportBecauseOfNSIQ(eventId, bundleArgs);
                            return;
                    }
                } else {
                    sendPerceptionMessage(9, bundleArgs.getInt("tgtUid"), bundleArgs.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                }
            }
        } else if (DEBUG) {
            AwareLog.d(TAG, "AwareIntelligentRecg feature disabled!");
        }
    }

    private void reportBecauseOfNSIQ(int eventId, Bundle bundleArgs) {
        if (bundleArgs != null) {
            if (eventId == 35) {
                AwareAudioFocusManager manager = AwareAudioFocusManager.getInstance();
                if (manager != null) {
                    manager.reportAudioFocusRequest(bundleArgs.getInt("state_type"), bundleArgs.getInt("callUid"), bundleArgs.getString("request_name"));
                }
            } else if (eventId == 36) {
                AwareAudioFocusManager manager2 = AwareAudioFocusManager.getInstance();
                if (manager2 != null) {
                    manager2.reportAudioFocusLoss(bundleArgs.getInt("state_type"), bundleArgs.getInt("callUid"), bundleArgs.getString("request_name"));
                }
            } else if (DEBUG) {
                AwareLog.e(TAG, "Unknown EventID: " + eventId);
            }
        }
    }

    public void reportAppUpdate(int eventId, Bundle args) {
        IntlRecgHandler intlRecgHandler;
        if (mEnabled && args != null && (intlRecgHandler = this.mHandler) != null) {
            Message msg = intlRecgHandler.obtainMessage();
            msg.what = 0;
            msg.arg1 = eventId;
            msg.setData(args);
            this.mHandler.sendMessage(msg);
        }
    }

    private void removeSpecTTSPkg(String pkg) {
        synchronized (this.mTTSPkg) {
            this.mTTSPkg.remove(pkg);
        }
    }

    private List<ResolveInfo> queryTTSService(String pkg) {
        if (this.mPackageManager == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.TTS_SERVICE");
        if (pkg != null) {
            intent.setPackage(pkg);
        }
        return this.mPackageManager.queryIntentServicesAsUser(intent, 851968, this.mCurUserId);
    }

    private void initSpecTTSPkg(String pkg) {
        List<ResolveInfo> resolveInfos = queryTTSService(pkg);
        if (resolveInfos != null && resolveInfos.size() > 0) {
            synchronized (this.mTTSPkg) {
                this.mTTSPkg.add(pkg);
            }
        }
    }

    private void initTTSPkg() {
        List<ResolveInfo> resolveInfos = queryTTSService(null);
        Set<String> ttsPkg = new ArraySet<>();
        if (resolveInfos != null && resolveInfos.size() > 0) {
            for (ResolveInfo ri : resolveInfos) {
                String pkg = ri.getComponentInfo().packageName;
                if (!ttsPkg.contains(pkg)) {
                    ttsPkg.add(pkg);
                }
            }
        }
        synchronized (this.mTTSPkg) {
            this.mTTSPkg.clear();
            this.mTTSPkg.addAll(ttsPkg);
        }
    }

    private void initSettings() {
        this.mDefaultInputMethod = readDefaultInputMethod();
        sendInputMethodSetMessage();
        initBlindPkg();
        updateAccessbilityService();
        updateDefaultTTS();
        initDefaultWallPaper();
        this.mDefaultSms = readDefaultSmsPackage();
        updateDozeProtectList();
        sendBleStatusUpdate();
    }

    private void initBlindPkg() {
        ArrayList<String> blindList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), BLIND_PREFABRICATED);
        if (blindList != null) {
            synchronized (this.mBlindPkg) {
                this.mBlindPkg.clear();
                this.mBlindPkg.addAll(blindList);
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Secure.getUriFor("default_input_method").equals(uri)) {
                AwareIntelligentRecg awareIntelligentRecg = AwareIntelligentRecg.this;
                String unused = awareIntelligentRecg.mDefaultInputMethod = awareIntelligentRecg.readDefaultInputMethod();
                AwareIntelligentRecg.this.sendInputMethodSetMessage();
            } else if (Settings.Secure.getUriFor("enabled_accessibility_services").equals(uri)) {
                AwareIntelligentRecg.this.updateAccessbilityService();
            } else if (Settings.Secure.getUriFor("tts_default_synth").equals(uri)) {
                AwareIntelligentRecg.this.updateDefaultTTS();
            } else if (Settings.Secure.getUriFor(AwareIntelligentRecg.VALUE_BT_BLE_CONNECT_APPS).equals(uri)) {
                AwareIntelligentRecg.this.updateBluetoothConnect();
            } else if (Settings.Secure.getUriFor(AwareIntelligentRecg.VALUE_BT_LAST_BLE_DISCONNECT).equals(uri)) {
                AwareIntelligentRecg.this.updateBluetoothLastDisconnect();
            } else if (Settings.Secure.getUriFor("sms_default_application").equals(uri)) {
                AwareIntelligentRecg awareIntelligentRecg2 = AwareIntelligentRecg.this;
                String unused2 = awareIntelligentRecg2.mDefaultSms = awareIntelligentRecg2.readDefaultSmsPackage();
            } else if (Settings.Secure.getUriFor(HwArbitrationDEFS.KEY_BLUETOOTH_ON).equals(uri)) {
                AwareIntelligentRecg.this.sendBleStatusUpdate();
            } else if (Settings.Global.getUriFor("unified_device_name").equals(uri)) {
                AwareMiddleware.getInstance().onProfileChanged();
            }
        }
    }

    private class HSMObserver extends ContentObserver {
        public HSMObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            AwareIntelligentRecg.this.updateDozeProtectList();
        }
    }

    /* access modifiers changed from: private */
    public void updateAccessbilityService() {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null) {
            String settingValue = Settings.Secure.getStringForUser(multiTaskManagerService.context().getContentResolver(), "enabled_accessibility_services", this.mCurUserId);
            if (DEBUG) {
                AwareLog.i(TAG, "Accessbility:" + settingValue);
            }
            Set<String> accessPkg = new ArraySet<>();
            if (settingValue != null && !settingValue.equals("")) {
                String[] accessbilityServices = settingValue.split(":");
                for (String accessbilityService : accessbilityServices) {
                    int indexof = accessbilityService.indexOf(47);
                    if (indexof > 0) {
                        accessPkg.add(accessbilityService.substring(0, indexof));
                    }
                }
            }
            synchronized (this.mAccessPkg) {
                this.mAccessPkg.clear();
                this.mAccessPkg.addAll(accessPkg);
            }
            Set<String> blindPkgs = new ArraySet<>();
            for (String access : accessPkg) {
                synchronized (this.mTTSPkg) {
                    if (this.mTTSPkg.contains(access)) {
                        blindPkgs.add(access);
                    }
                }
                synchronized (this.mBlindPkg) {
                    if (this.mBlindPkg.contains(access)) {
                        blindPkgs.add(access);
                    }
                }
            }
            AppStartupDataMgr appStartupDataMgr = this.mDataMgr;
            if (appStartupDataMgr != null) {
                appStartupDataMgr.updateBlind(blindPkgs);
                AppStatusUtils.getInstance().updateBlind(blindPkgs);
                sendFlushToDiskMessage();
            }
        }
    }

    private void sendFlushToDiskMessage() {
        IntlRecgHandler intlRecgHandler = this.mHandler;
        if (intlRecgHandler != null) {
            Message msg = intlRecgHandler.obtainMessage();
            msg.what = 6;
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void updateDefaultTTS() {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null) {
            String settingValue = Settings.Secure.getStringForUser(multiTaskManagerService.context().getContentResolver(), "tts_default_synth", this.mCurUserId);
            if (DEBUG) {
                AwareLog.i(TAG, "Default TTS :" + settingValue);
            }
            this.mDefaultTTS = settingValue;
        }
    }

    /* access modifiers changed from: private */
    public void updateBluetoothConnect() {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null) {
            String settingValue = Settings.Secure.getStringForUser(multiTaskManagerService.context().getContentResolver(), VALUE_BT_BLE_CONNECT_APPS, this.mCurUserId);
            if (DEBUG) {
                AwareLog.i(TAG, "Bluetooth connect :" + settingValue);
            }
            synchronized (this.mBluetoothLock) {
                this.mBtoothConnectList.clear();
                if (settingValue != null) {
                    if (!settingValue.equals("")) {
                        for (String strPid : settingValue.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE)) {
                            try {
                                this.mBtoothConnectList.add(Integer.valueOf(Integer.parseInt(strPid)));
                            } catch (NumberFormatException e) {
                                AwareLog.i(TAG, "updateBluetoothConnect error");
                            }
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateBluetoothLastDisconnect() {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null) {
            String settingValue = Settings.Secure.getStringForUser(multiTaskManagerService.context().getContentResolver(), VALUE_BT_LAST_BLE_DISCONNECT, this.mCurUserId);
            if (DEBUG) {
                AwareLog.i(TAG, "Bluetooth last disconnect :" + settingValue);
            }
            synchronized (this.mBluetoothLock) {
                this.mBtoothLastPid = -1;
                if (settingValue != null) {
                    if (!settingValue.equals("")) {
                        String[] strings = settingValue.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE);
                        if (2 == strings.length) {
                            try {
                                this.mBtoothLastPid = Integer.parseInt(strings[0]);
                                this.mBtoothLastTime = Long.parseLong(strings[1]);
                            } catch (NumberFormatException e) {
                                this.mBtoothLastPid = -1;
                                AwareLog.i(TAG, "updateBluetoothLastDisconnect error");
                            }
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateDozeProtectList() {
        if (this.mMtmService != null) {
            int curUserId = AwareAppAssociate.getInstance().getCurUserId();
            try {
                Bundle dozeProtectBundle = this.mMtmService.context().getContentResolver().call(Uri.parse(URI_PREFIX + curUserId + URI_SYSTEM_MANAGER_SMART_PROVIDER), TAG_GET_DOZE_LIST, AppMngRule.VALUE_ALL, (Bundle) null);
                if (dozeProtectBundle == null) {
                    AwareLog.i(TAG, "updateDozeProtectList failed: not implemented !");
                    return;
                }
                ArrayList<String> dozeProtectApps = dozeProtectBundle.getStringArrayList(TAG_DOZE_PROTECT);
                if (dozeProtectApps != null) {
                    synchronized (this.mDozeProtectPkg) {
                        this.mDozeProtectPkg.clear();
                        this.mDozeProtectPkg.addAll(dozeProtectApps);
                    }
                } else {
                    AwareLog.i(TAG, "updateDozeProtectList failed: no pkg found !");
                }
                AwareLog.i(TAG, "updateDozeProtectList :" + dozeProtectApps);
            } catch (IllegalArgumentException e) {
                AwareLog.e(TAG, "updateDozeProtectList failed: illegal argument !");
            }
        }
    }

    /* access modifiers changed from: private */
    public String readDefaultInputMethod() {
        String inputInfo;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (!(multiTaskManagerService == null || (inputInfo = Settings.Secure.getStringForUser(multiTaskManagerService.context().getContentResolver(), "default_input_method", this.mCurUserId)) == null)) {
            String[] defaultIms = inputInfo.split("/");
            if (defaultIms[0] != null) {
                return defaultIms[0];
            }
        }
        return "";
    }

    /* access modifiers changed from: private */
    public void initDefaultWallPaper() {
        String pkg = readDefaultWallPaper();
        if (pkg != null) {
            AwareLog.i(TAG, "initDefaultWallPaper  pkg =" + pkg);
            sendWallpaperSetMessage(pkg);
        } else if (this.mInitWallPaperCount.get() >= 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("AwareIntelligentRecg send read wallpaper message again mInitWallPaperCount=");
            sb.append(this.mInitWallPaperCount.get() - 1);
            AwareLog.i(TAG, sb.toString());
            sendInitWallPaperMessage();
            this.mInitWallPaperCount.decrementAndGet();
        } else {
            AwareLog.e(TAG, "AwareIntelligentRecg has no wallpaper");
        }
    }

    private String readDefaultWallPaper() {
        WallpaperInfo winfo = null;
        IWallpaperManager wallpaper = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
        if (wallpaper != null) {
            try {
                winfo = wallpaper.getWallpaperInfo(UserHandle.myUserId());
            } catch (RemoteException e) {
                AwareLog.e(TAG, "Couldn't read  Default WallPaper");
            }
        }
        if (winfo != null) {
            return winfo.getPackageName();
        }
        return null;
    }

    /* access modifiers changed from: private */
    public String readDefaultSmsPackage() {
        String inputInfo;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (!(multiTaskManagerService == null || (inputInfo = Settings.Secure.getStringForUser(multiTaskManagerService.context().getContentResolver(), "sms_default_application", this.mCurUserId)) == null)) {
            String[] defaultIms = inputInfo.split("/");
            if (defaultIms[0] != null) {
                return defaultIms[0];
            }
        }
        return "";
    }

    private boolean isTTSPkg(String pkg) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mTTSPkg) {
            if (this.mTTSPkg.contains(pkg)) {
                return true;
            }
            return false;
        }
    }

    private boolean isPayPkg(String pkg, AwareAppStartStatusCache status) {
        if (!mEnabled) {
            return false;
        }
        Set<String> set = this.mPayPkgList;
        if (set == null || !set.contains(pkg)) {
            return isAttrApp(pkg, status, 1);
        }
        return true;
    }

    private boolean isSharePkg(String pkg, AwareAppStartStatusCache status) {
        if (!mEnabled) {
            return false;
        }
        Set<String> set = this.mSharePkgList;
        if (set == null || !set.contains(pkg)) {
            return isAttrApp(pkg, status, 2);
        }
        return true;
    }

    private boolean isAttrApp(String pkg, AwareAppStartStatusCache status, int attr) {
        int appAttri;
        if (mEnabled && (appAttri = getAttributeCache(pkg, status)) != -1 && (appAttri & attr) == attr) {
            return true;
        }
        return false;
    }

    private int getAttributeCache(String pkg, AwareAppStartStatusCache status) {
        int appAttri = status.cacheAppAttribute;
        if (appAttri != -1) {
            return appAttri;
        }
        int appAttri2 = AppTypeRecoManager.getInstance().getAppAttribute(pkg);
        status.cacheAppAttribute = appAttri2;
        return appAttri2;
    }

    public String getDefaultInputMethod() {
        return this.mDefaultInputMethod;
    }

    public String getDefaultTTS() {
        return this.mDefaultTTS;
    }

    public String getDefaultWallPaper() {
        return this.mDefaultWallPaper;
    }

    public String getDefaultSmsPackage() {
        return this.mDefaultSms;
    }

    private void reportWallpaper(String pkg) {
        if (DEBUG) {
            AwareLog.i(TAG, "reportWallpaper  pkg=" + pkg);
        }
        if (pkg != null && !pkg.isEmpty()) {
            sendWallpaperSetMessage(pkg);
        }
    }

    public int getDefaultInputMethodUid() {
        return this.mDefaultInputMethodUid;
    }

    private int getDefaultWallPaperUid() {
        return this.mDefaultWallPaperUid;
    }

    private void sendWallpaperSetMessage(String pkg) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 13;
        msg.obj = pkg;
        this.mHandler.sendMessage(msg);
    }

    /* access modifiers changed from: private */
    public void handleWallpaperSetMessage(String pkg) {
        this.mDefaultWallPaper = pkg;
        this.mDefaultWallPaperUid = getUIDByPackageName(pkg);
    }

    /* access modifiers changed from: private */
    public void sendInputMethodSetMessage() {
        IntlRecgHandler intlRecgHandler = this.mHandler;
        if (intlRecgHandler != null) {
            Message msg = intlRecgHandler.obtainMessage();
            msg.what = 15;
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void handleInputMethodSetMessage() {
        this.mDefaultInputMethodUid = getUIDByPackageName(this.mDefaultInputMethod);
        AwareBinderSchedManager.getInstance().reportDefaultInputMethod(this.mDefaultInputMethodUid, this.mDefaultInputMethod);
    }

    private int getUIDByPackageName(String pkg) {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService == null) {
            return -1;
        }
        return getUIDByPackageName(multiTaskManagerService.context(), pkg);
    }

    private int getUIDByPackageName(Context context, String pkg) {
        PackageManager pm;
        if (pkg == null || context == null || (pm = context.getPackageManager()) == null) {
            return -1;
        }
        try {
            ApplicationInfo appInfo = pm.getApplicationInfoAsUser(pkg, 0, this.mCurUserId);
            if (appInfo != null) {
                return appInfo.uid;
            }
            return -1;
        } catch (PackageManager.NameNotFoundException e) {
            AwareLog.e(TAG, "get the package uid faied");
            return -1;
        }
    }

    /* access modifiers changed from: private */
    public void recordStartInfo(AwareAppStartInfo startInfo) {
        if (startInfo != null) {
            ArrayList<AwareAppStartInfo> mRemoveList = new ArrayList<>();
            long currentTime = SystemClock.elapsedRealtime();
            synchronized (this.mAppStartInfoList) {
                for (AwareAppStartInfo item : this.mAppStartInfoList) {
                    if (currentTime - item.timestamp > 20000) {
                        mRemoveList.add(item);
                    }
                }
                this.mAppStartInfoList.add(startInfo);
                if (mRemoveList.size() > 0) {
                    this.mAppStartInfoList.removeAll(mRemoveList);
                }
            }
        }
    }

    private boolean isExist(ArraySet<AwareAppStartInfo> infos, AwareAppStartInfo startInfo) {
        if (infos == null || startInfo == null) {
            return false;
        }
        Iterator<AwareAppStartInfo> it = infos.iterator();
        while (it.hasNext()) {
            if (startInfo.equals(it.next())) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean needRecgPushSDK(AwareAppStartInfo startInfo) {
        String cls;
        if (startInfo == null || (cls = startInfo.cls) == null) {
            return false;
        }
        synchronized (this.mGoodPushSDKClsList) {
            if (this.mGoodPushSDKClsList.contains(cls)) {
                return false;
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void adjustPushSDKCmp(AwareAppStartInfo startInfo) {
        if (startInfo != null) {
            ArrayMap<Integer, ArraySet<AwareAppStartInfo>> mStartMap = new ArrayMap<>();
            long currentTime = SystemClock.elapsedRealtime();
            synchronized (this.mAppStartInfoList) {
                for (AwareAppStartInfo item : this.mAppStartInfoList) {
                    if (currentTime - item.timestamp < 20000) {
                        if (!mStartMap.containsKey(Integer.valueOf(item.callerUid))) {
                            ArraySet<AwareAppStartInfo> initset = new ArraySet<>();
                            initset.add(item);
                            mStartMap.put(Integer.valueOf(item.callerUid), initset);
                        } else if (!isExist(mStartMap.get(Integer.valueOf(item.callerUid)), item)) {
                            mStartMap.get(Integer.valueOf(item.callerUid)).add(item);
                        }
                    }
                }
            }
            ArrayMap<String, Integer> clsMap = new ArrayMap<>();
            for (Map.Entry<Integer, ArraySet<AwareAppStartInfo>> map : mStartMap.entrySet()) {
                int uid = map.getKey().intValue();
                ArraySet<AwareAppStartInfo> sameCallerStartInfos = map.getValue();
                if (DEBUG) {
                    AwareLog.d(TAG, "PushSDK check cls uid:" + uid + " size:" + sameCallerStartInfos.size());
                }
                clsMap.clear();
                Iterator<AwareAppStartInfo> it = sameCallerStartInfos.iterator();
                while (it.hasNext()) {
                    AwareAppStartInfo item2 = it.next();
                    if (item2.cls != null) {
                        if (DEBUG) {
                            AwareLog.d(TAG, "PushSDK check cls :" + item2.cls);
                        }
                        if (!clsMap.containsKey(item2.cls)) {
                            clsMap.put(item2.cls, 1);
                        } else {
                            int count = clsMap.get(item2.cls).intValue();
                            clsMap.put(item2.cls, Integer.valueOf(count + 1));
                            updatePushSDKCls(item2.cls, count + 1);
                        }
                    }
                }
            }
        }
    }

    private void updatePushSDKCls(String cls, int count) {
        if (count >= 2) {
            synchronized (this.mPushSDKClsList) {
                if (!this.mPushSDKClsList.contains(cls)) {
                    this.mPushSDKClsList.add(cls);
                    sendUpdateCmpInfoMessage("", cls, 0, true);
                }
            }
        } else if (DEBUG) {
            AwareLog.d(TAG, "PushSDK start cls count must be > :2");
        }
    }

    private boolean isUnReminderApp(List<String> packageList) {
        if (packageList == null) {
            return false;
        }
        int packageListSize = packageList.size();
        for (int i = 0; i < packageListSize; i++) {
            if (!isUnReminderApp(packageList.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isUnReminderApp(String pkgName) {
        int type = AppTypeRecoManager.getInstance().getAppType(pkgName);
        if (DEBUG) {
            AwareLog.d(TAG, "getAppType From Habit pkg " + pkgName + " type : " + type);
        }
        SparseSet sparseSet = this.mUnRemindAppTypeList;
        if (sparseSet == null || !sparseSet.contains(type)) {
            return false;
        }
        int appAttr = AppTypeRecoManager.getInstance().getAppAttribute(pkgName);
        if (appAttr == -1) {
            return true;
        }
        if ((appAttr & 4) == 4) {
            return false;
        }
        return true;
    }

    private void initAlarm(AlarmInfo alarm, CmpTypeInfo alarmRecgInfo, boolean isAdd, boolean isWakeup, long time) {
        boolean z = true;
        alarm.status = alarmRecgInfo != null ? alarmRecgInfo.getType() == 4 ? 2 : 1 : 0;
        alarm.count = isAdd ? 1 : 0;
        alarm.startTime = isWakeup ? time : 0;
        alarm.perception_count = alarmRecgInfo != null ? alarmRecgInfo.getPerceptionCount() : 0;
        alarm.unperception_count = alarmRecgInfo != null ? alarmRecgInfo.getUnPerceptionCount() : 0;
        if (isAdd) {
            if (!AwareAppAssociate.getInstance().isForeGroundApp(alarm.uid) || !this.mIsScreenOn.get()) {
                z = false;
            }
            alarm.bfgset = z;
        }
    }

    private void reportAlarm(String packageName, String statustag, int operation, int uid) {
        SparseArray<ArrayMap<String, AlarmInfo>> sparseArray;
        long time;
        AlarmInfo alarm;
        ArrayMap<String, AlarmInfo> alarms;
        AlarmInfo alarm2;
        if (UserHandle.getAppId(uid) >= 10000 && packageName != null && statustag != null && !isUnReminderApp(packageName)) {
            Set<String> set = this.mAlarmPkgList;
            if (set == null || !set.contains(packageName)) {
                String[] strs = statustag.split(":");
                if (strs.length == 2) {
                    String tag = strs[1];
                    if (DEBUG) {
                        AwareLog.i(TAG, "reportAlarm for clock app pkg : " + packageName + " tag : " + tag + " uid : " + uid + " operation : " + operation);
                    }
                    int userId = UserHandle.getUserId(uid);
                    CmpTypeInfo alarmRecgInfo = getAlarmRecgResult(uid, packageName, tag);
                    boolean isAdd = operation == 0;
                    boolean isWakeup = operation == 2;
                    long time2 = SystemClock.elapsedRealtime();
                    SparseArray<ArrayMap<String, AlarmInfo>> sparseArray2 = this.mAlarmMap;
                    synchronized (sparseArray2) {
                        try {
                            ArrayMap<String, AlarmInfo> alarms2 = this.mAlarmMap.get(uid);
                            if (alarms2 == null) {
                                try {
                                    alarms = new ArrayMap<>();
                                    alarm2 = new AlarmInfo(uid, packageName, tag);
                                    sparseArray = sparseArray2;
                                } catch (Throwable th) {
                                    th = th;
                                    sparseArray = sparseArray2;
                                    throw th;
                                }
                                try {
                                    initAlarm(alarm2, alarmRecgInfo, isAdd, isWakeup, time2);
                                    alarms.put(tag, alarm2);
                                    this.mAlarmMap.put(uid, alarms);
                                    alarm = alarm2;
                                    time = time2;
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            } else {
                                sparseArray = sparseArray2;
                                try {
                                    alarm = alarms2.get(tag);
                                    if (alarm == null) {
                                        try {
                                            AlarmInfo alarm3 = new AlarmInfo(uid, packageName, tag);
                                            initAlarm(alarm3, alarmRecgInfo, isAdd, isWakeup, time2);
                                            alarms2.put(tag, alarm3);
                                            alarm = alarm3;
                                            time = time2;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            throw th;
                                        }
                                    } else {
                                        if (!isWakeup || alarm.count <= 0) {
                                            time = time2;
                                        } else {
                                            time = time2;
                                            try {
                                                alarm.startTime = time;
                                                alarm.bhandled = false;
                                            } catch (Throwable th4) {
                                                th = th4;
                                                throw th;
                                            }
                                        }
                                        if (isAdd) {
                                            alarm.count++;
                                        } else if (alarm.count > 0) {
                                            alarm.count--;
                                        }
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    throw th;
                                }
                            }
                            try {
                                if (isWakeup && userId == this.mCurUserId && time > this.mLastPerceptionTime.get() + HwArbitrationDEFS.WIFI_RX_BYTES_THRESHOLD) {
                                    sendUnPerceptionMessage(alarm);
                                }
                                if (SystemClock.elapsedRealtime() / 86400000 > ((long) this.mDBUpdateCount)) {
                                    sendUpdateDbMessage();
                                    this.mDBUpdateCount++;
                                    if (DEBUG) {
                                        AwareLog.i(TAG, "UPDATE_DB_INTERVAL mDBUpdateCount : " + this.mDBUpdateCount);
                                    }
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            sparseArray = sparseArray2;
                            throw th;
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x010b, code lost:
        r0 = r7.keySet().iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x0117, code lost:
        if (r0.hasNext() == false) goto L_0x012d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x0119, code lost:
        r2 = r0.next();
        updateAlarmCmp(r2, r7.get(r2).booleanValue());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x012d, code lost:
        return;
     */
    public void handlePerceptionEvent(int uid, String pkg, int event) {
        if (DEBUG) {
            AwareLog.i(TAG, "handlePerceptionEvent uid : " + uid + " pkg : " + pkg + " for event : " + event);
        }
        if (UserHandle.getAppId(uid) >= 10000) {
            int i = 9;
            if (event != 9 || !AwareAppAssociate.getInstance().isForeGroundApp(uid)) {
                long now = SystemClock.elapsedRealtime();
                this.mLastPerceptionTime.set(now);
                ArrayMap<AlarmInfo, Boolean> alarmMap = new ArrayMap<>();
                synchronized (this.mAlarmMap) {
                    ArrayMap<String, AlarmInfo> alarms = this.mAlarmMap.get(uid);
                    if (alarms != null) {
                        boolean bUpdateDb = false;
                        for (String tag : alarms.keySet()) {
                            AlarmInfo alarm = alarms.get(tag);
                            if (event != i || alarm.bfgset) {
                                if (this.mHandler.hasMessages(11, alarm)) {
                                    this.mHandler.removeMessages(11, alarm);
                                }
                                long alarmtime = alarm.startTime;
                                if (alarmtime + HwArbitrationDEFS.WIFI_RX_BYTES_THRESHOLD >= now && alarmtime <= now && (!alarm.bhandled || alarm.reason != event)) {
                                    alarm.reason = event;
                                    alarm.perception_count++;
                                    alarm.bhandled = true;
                                    int newStatus = getNewPerceptionStatus(alarm);
                                    if (newStatus != alarm.status && newStatus == 1) {
                                        alarm.status = 1;
                                        bUpdateDb = true;
                                    }
                                    alarmMap.put(alarm.copy(), Boolean.valueOf(bUpdateDb));
                                    if (DEBUG) {
                                        AwareLog.i(TAG, "alarm is a clock alarm : " + alarm.packagename + " tag : " + tag + " for reason : " + event);
                                    }
                                }
                                i = 9;
                            }
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleUnPerceptionEvent(AlarmInfo alarm, int event) {
        AlarmInfo updateAlarm;
        if (alarm != null) {
            boolean bUpdateDb = false;
            synchronized (this.mAlarmMap) {
                alarm.reason = event;
                alarm.unperception_count++;
                alarm.bhandled = true;
                if (DEBUG) {
                    AwareLog.i(TAG, "alarm is a un clock alarm : " + alarm.toString());
                }
                int newStatus = getNewPerceptionStatus(alarm);
                if (newStatus != alarm.status && newStatus == 2) {
                    alarm.status = 2;
                    bUpdateDb = true;
                }
                updateAlarm = alarm.copy();
            }
            updateAlarmCmp(updateAlarm, bUpdateDb);
        }
    }

    private int getNewPerceptionStatus(AlarmInfo alarm) {
        if (alarm == null) {
            return 0;
        }
        int factor = 5;
        if (AppTypeRecoManager.getInstance().getAppType(alarm.packagename) != 5) {
            factor = 1;
        }
        if (alarm.unperception_count < 1 || alarm.unperception_count <= alarm.perception_count * factor) {
            return 1;
        }
        return 2;
    }

    private CmpTypeInfo getAlarmRecgResult(int uid, String pkg, String tag) {
        if (pkg == null || tag == null) {
            return null;
        }
        String key = UserHandle.getUserId(uid) + "#" + pkg + "#" + tag;
        synchronized (this.mAlarmCmps) {
            CmpTypeInfo info = this.mAlarmCmps.get(key);
            if (info != null) {
                return info;
            }
            return null;
        }
    }

    private void updateAlarmCmp(AlarmInfo alarm, boolean binsert) {
        CmpTypeInfo cmpInfo;
        if (alarm != null) {
            String key = UserHandle.getUserId(alarm.uid) + "#" + alarm.packagename + "#" + alarm.tag;
            synchronized (this.mAlarmCmps) {
                cmpInfo = this.mAlarmCmps.get(key);
                if (cmpInfo == null) {
                    cmpInfo = new CmpTypeInfo();
                    cmpInfo.setUserId(UserHandle.getUserId(alarm.uid));
                    cmpInfo.setPkgName(alarm.packagename);
                    cmpInfo.setCls(alarm.tag);
                    cmpInfo.setTime(System.currentTimeMillis());
                    this.mAlarmCmps.put(key, cmpInfo);
                }
                cmpInfo.setType(alarm.status == 2 ? 4 : 3);
                cmpInfo.setPerceptionCount(alarm.perception_count);
                cmpInfo.setUnPerceptionCount(alarm.unperception_count);
            }
            if (binsert) {
                Message msg = this.mHandler.obtainMessage();
                msg.obj = cmpInfo;
                msg.what = 2;
                this.mHandler.sendMessage(msg);
            }
        }
    }

    private void removeAlarmCmp(int uid, String pkg) {
        if (pkg != null) {
            synchronized (this.mAlarmCmps) {
                Iterator<Map.Entry<String, CmpTypeInfo>> it = this.mAlarmCmps.entrySet().iterator();
                String pkgKey = UserHandle.getUserId(uid) + "#" + pkg + "#";
                while (it.hasNext()) {
                    String key = it.next().getKey();
                    if (key != null && key.indexOf(pkgKey) == 0) {
                        it.remove();
                    }
                }
            }
        }
    }

    private void removeAlarmCmpByUserid(int userid) {
        synchronized (this.mAlarmCmps) {
            Iterator<Map.Entry<String, CmpTypeInfo>> it = this.mAlarmCmps.entrySet().iterator();
            String useridKey = userid + "#";
            while (it.hasNext()) {
                String key = it.next().getKey();
                if (key != null && key.indexOf(useridKey) == 0) {
                    it.remove();
                }
            }
        }
    }

    private void removeAlarms(int uid) {
        if (uid > 0) {
            if (DEBUG) {
                AwareLog.i(TAG, "removeAlarms uid : " + uid);
            }
            synchronized (this.mAlarmMap) {
                this.mAlarmMap.remove(uid);
            }
        }
    }

    private void removeAlarmsByUserid(int userid) {
        if (DEBUG) {
            AwareLog.i(TAG, "removeAlarmsByUserid userid : " + userid);
        }
        synchronized (this.mAlarmMap) {
            for (int i = this.mAlarmMap.size() - 1; i >= 0; i--) {
                if (userid == UserHandle.getUserId(this.mAlarmMap.keyAt(i))) {
                    this.mAlarmMap.removeAt(i);
                }
            }
        }
    }

    public int getAlarmActionType(int uid, String pkg, String action) {
        String key = UserHandle.getUserId(uid) + "#" + pkg + "#" + action;
        synchronized (this.mAlarmCmps) {
            CmpTypeInfo info = this.mAlarmCmps.get(key);
            if (info == null) {
                return -1;
            }
            return info.getType();
        }
    }

    public List<String> getAllInvalidAlarmTags(int uid, String pkg) {
        if (pkg == null) {
            return null;
        }
        List<String> result = new ArrayList<>();
        Set<String> set = this.mAlarmPkgList;
        if (set != null && set.contains(pkg)) {
            return result;
        }
        if (isUnReminderApp(pkg)) {
            return null;
        }
        synchronized (this.mAlarmCmps) {
            String pkgKey = UserHandle.getUserId(uid) + "#" + pkg + "#";
            for (Map.Entry<String, CmpTypeInfo> m : this.mAlarmCmps.entrySet()) {
                if (m != null) {
                    String key = m.getKey();
                    if (key != null) {
                        if (key.indexOf(pkgKey) == 0) {
                            CmpTypeInfo value = m.getValue();
                            if (value.getType() == 4) {
                                result.add(value.getCls());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public boolean hasPerceptAlarm(int uid, List<String> packageList) {
        if (!mEnabled) {
            return false;
        }
        if (!(packageList == null || this.mAlarmPkgList == null)) {
            int packageListSize = packageList.size();
            for (int i = 0; i < packageListSize; i++) {
                if (this.mAlarmPkgList.contains(packageList.get(i))) {
                    return true;
                }
            }
        }
        if (isUnReminderApp(packageList)) {
            return false;
        }
        synchronized (this.mAlarmMap) {
            ArrayMap<String, AlarmInfo> alarms = this.mAlarmMap.get(uid);
            if (alarms == null) {
                return false;
            }
            for (Map.Entry<String, AlarmInfo> entry : alarms.entrySet()) {
                if (entry != null) {
                    AlarmInfo info = entry.getValue();
                    if (info != null && (info.status == 0 || info.status == 1)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void handleUpdateDB() {
        Map<String, CmpTypeInfo> alarmsInDB = getAlarmCmpFromDB();
        synchronized (this.mAlarmCmps) {
            for (Map.Entry entry : this.mAlarmCmps.entrySet()) {
                CmpTypeInfo value = entry.getValue();
                CmpTypeInfo alarmInDB = alarmsInDB.get(entry.getKey());
                if (alarmInDB == null || alarmInDB.getPerceptionCount() != value.getPerceptionCount() || alarmInDB.getUnPerceptionCount() != value.getUnPerceptionCount()) {
                    insertCmpRecgInfo(value);
                }
            }
        }
    }

    public void updateWidget(Set<String> widgets, String pkgName) {
        AppStartupDataMgr appStartupDataMgr = this.mDataMgr;
        if (appStartupDataMgr != null) {
            if (appStartupDataMgr.updateWidgetList(widgets)) {
                sendFlushToDiskMessage();
            }
            widgetTrigUpdate(pkgName);
        }
    }

    public void widgetTrigUpdate(String pkgName) {
        if (pkgName != null && this.mDataMgr != null) {
            if (DEBUG) {
                AwareLog.i(TAG, "widgetTrigUpdate pkg:" + pkgName);
            }
            this.mDataMgr.updateWidgetUpdateTime(pkgName);
        }
    }

    private void flushUpDownLoadTrigTime(int uid) {
        long curTime = SystemClock.elapsedRealtime();
        synchronized (this.mStatusUpDownElapse) {
            this.mStatusUpDownElapse.put(Integer.valueOf(uid), Long.valueOf(curTime));
        }
    }

    private boolean isProtectUpdateWidget(String pkgName, int uid) {
        AppStartupDataMgr appStartupDataMgr;
        if (pkgName == null || (appStartupDataMgr = this.mDataMgr) == null) {
            return false;
        }
        long time = appStartupDataMgr.getWidgetExistPkgUpdateTime(pkgName);
        if (time == -1) {
            return false;
        }
        if (this.mDataMgr.getWidgetCnt() <= this.mWidgetCheckUpdateCnt) {
            return true;
        }
        long j = this.mWidgetCheckUpdateInterval;
        if (SystemClock.elapsedRealtime() - time <= j) {
            return true;
        }
        return !isUpDownTrigInIntervalTime(uid, j);
    }

    public boolean isUpDownTrigInIntervalTime(int uid, long timeMax) {
        long timeCur = SystemClock.elapsedRealtime();
        synchronized (this.mStatusUpDownElapse) {
            Long updownTime = this.mStatusUpDownElapse.get(Integer.valueOf(uid));
            if (updownTime == null) {
                return false;
            }
            if (timeCur - updownTime.longValue() <= timeMax) {
                return true;
            }
            return false;
        }
    }

    private void sendMsgWidgetUpDownLoadClear() {
        IntlRecgHandler intlRecgHandler = this.mHandler;
        if (intlRecgHandler != null) {
            Message msg = intlRecgHandler.obtainMessage();
            msg.what = 19;
            this.mHandler.removeMessages(19);
            this.mHandler.sendMessageDelayed(msg, this.mWidgetCheckUpdateInterval);
        }
    }

    /* access modifiers changed from: private */
    public void handleWidgetUpDownLoadClear() {
        long curTime = SystemClock.elapsedRealtime();
        synchronized (this.mStatusUpDownElapse) {
            Iterator<Map.Entry<Integer, Long>> iter = this.mStatusUpDownElapse.entrySet().iterator();
            while (iter.hasNext()) {
                if (curTime - iter.next().getValue().longValue() > this.mWidgetCheckUpdateInterval) {
                    iter.remove();
                }
            }
        }
        sendMsgWidgetUpDownLoadClear();
    }

    public boolean isPushSdkComp(int callerUid, String compName, int targetUid, AppMngConstant.AppStartSource source, boolean isSysApp, boolean abroad) {
        int index;
        if (!mEnabled) {
            return false;
        }
        if (DEBUG) {
            AwareLog.d(TAG, "PushSDK isPushSdkComp callerUid :" + callerUid + " targetUid : " + targetUid + " cmp: " + compName);
        }
        if (compName == null || callerUid == targetUid || AwareAppAssociate.getInstance().getCurHomeProcessUid() == callerUid || compName.isEmpty()) {
            return false;
        }
        AppStartupDataMgr appStartupDataMgr = this.mDataMgr;
        if (appStartupDataMgr != null && (isSysApp || appStartupDataMgr.isSpecialCaller(callerUid))) {
            return false;
        }
        String[] strs = compName.split("/");
        if (strs.length == 1) {
            index = 0;
        } else if (strs.length != 2) {
            return false;
        } else {
            index = 1;
        }
        if (isBadPushSdkComp(strs[index])) {
            return true;
        }
        if (!AppMngConstant.AppStartSource.THIRD_ACTIVITY.equals(source) && !AppMngConstant.AppStartSource.BIND_SERVICE.equals(source) && !abroad && strs.length == 2) {
            sendRecgPushSdkMessage(callerUid, compName, targetUid);
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x001a, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x001d, code lost:
        if ((r0 & 1) != 1) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:?, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:?, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x000f, code lost:
        r0 = android.rms.iaware.ComponentRecoManager.getInstance().getComponentBadFunc(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0018, code lost:
        if (r0 != 0) goto L_0x001b;
     */
    private boolean isBadPushSdkComp(String cls) {
        synchronized (this.mPushSDKClsList) {
            if (this.mPushSDKClsList.contains(cls)) {
                return true;
            }
        }
    }

    private boolean isAllowStartAppByTopN(String pkg, AwareAppStartStatusCache status, int type, int topN, boolean isBoot) {
        if (this.mDeviceTotalMemory <= 3072) {
            return false;
        }
        if (status.cacheAppType == -100) {
            status.cacheAppType = getAppMngSpecType(pkg);
        }
        if (status.cacheAppType == type) {
            return isTopHabitAppInStart(pkg, type, topN, isBoot);
        }
        return false;
    }

    private boolean isAlarmApp(String pkg, AwareAppStartStatusCache status) {
        if (status.unPercetibleAlarm == 1 || isUnReminderApp(pkg)) {
            return false;
        }
        Set<String> set = this.mAlarmPkgList;
        if (set != null && set.contains(pkg)) {
            return true;
        }
        int alarmType = -1;
        if (status.cacheAction != null) {
            alarmType = getAlarmActionType(status.cacheUid, pkg, status.cacheAction);
        } else if (status.cacheCompName != null) {
            alarmType = getAlarmActionType(status.cacheUid, pkg, status.cacheCompName);
        }
        return alarmType != 4;
    }

    public int getAppStartSpecCallerAction(String packageName, AwareAppStartStatusCache status, ArrayList<Integer> actionList, AppMngConstant.AppStartSource source) {
        if (!this.mAppStartEnabled || actionList == null) {
            return -1;
        }
        int size = actionList.size();
        for (int i = 0; i < size; i++) {
            Integer expectItem = actionList.get(i);
            if (isAppStartSpecCallerAction(packageName, expectItem.intValue(), status, source)) {
                return expectItem.intValue();
            }
        }
        return -1;
    }

    public int getAppStartSpecCallerStatus(String packageName, AwareAppStartStatusCache status, ArrayList<Integer> statusList) {
        if (!this.mAppStartEnabled || statusList == null) {
            return -1;
        }
        int size = statusList.size();
        for (int i = 0; i < size; i++) {
            Integer expectItem = statusList.get(i);
            if (isAppStartSpecCallerStatus(packageName, expectItem.intValue(), status)) {
                return expectItem.intValue();
            }
        }
        return -1;
    }

    public int getAppStartSpecTargetType(String packageName, AwareAppStartStatusCache status, ArrayList<Integer> typeList) {
        if (!this.mAppStartEnabled || typeList == null) {
            return -1;
        }
        int size = typeList.size();
        for (int i = 0; i < size; i++) {
            Integer expectItem = typeList.get(i);
            if (isAppStartSpecType(packageName, expectItem.intValue(), status)) {
                return expectItem.intValue();
            }
        }
        return -1;
    }

    public int getAppStartSpecTargetStatus(String packageName, AwareAppStartStatusCache status, ArrayList<Integer> statusList) {
        if (!this.mAppStartEnabled || statusList == null) {
            return -1;
        }
        int size = statusList.size();
        for (int i = 0; i < size; i++) {
            Integer expectItem = statusList.get(i);
            if (isAppStartSpecStat(packageName, expectItem.intValue(), status)) {
                return expectItem.intValue();
            }
        }
        return -1;
    }

    public int getAppStartSpecVerOversea(String packageName, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return -1;
        }
        if (status.cacheStatusCacheExt.abroad) {
            return AppStartPolicyCfg.AppStartOversea.OVERSEA.ordinal();
        }
        return AppStartPolicyCfg.AppStartOversea.CHINA.ordinal();
    }

    public int getAppStartSpecAppSrcRange(String packageName, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return -1;
        }
        if (status.cacheStatusCacheExt.appSrcRange == -100) {
            status.cacheStatusCacheExt.appSrcRange = getAppStartSpecAppSrcRangeResult(packageName);
        }
        boolean srcRange = true;
        if (!(status.cacheStatusCacheExt.appSrcRange == 0 || status.cacheStatusCacheExt.appSrcRange == 1)) {
            srcRange = false;
        }
        if (srcRange) {
            return AppStartPolicyCfg.AppStartAppSrcRange.PRERECG.ordinal();
        }
        return AppStartPolicyCfg.AppStartAppSrcRange.NONPRERECG.ordinal();
    }

    public int getAppStartSpecAppOversea(String packageName, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return -1;
        }
        if (status.cacheStatusCacheExt.appSrcRange == -100) {
            status.cacheStatusCacheExt.appSrcRange = getAppStartSpecAppSrcRangeResult(packageName);
        }
        if (status.cacheStatusCacheExt.appSrcRange == 0) {
            return AppStartPolicyCfg.AppStartAppOversea.CHINA.ordinal();
        }
        if (status.cacheStatusCacheExt.appSrcRange == 1) {
            return AppStartPolicyCfg.AppStartAppOversea.OVERSEA.ordinal();
        }
        return AppStartPolicyCfg.AppStartAppOversea.UNKNONW.ordinal();
    }

    public int getAppStartSpecScreenStatus(String packageName, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return -1;
        }
        if (this.mIsScreenOnPm) {
            return AppStartPolicyCfg.AppStartScreenStatus.SCREENON.ordinal();
        }
        return AppStartPolicyCfg.AppStartScreenStatus.SCREENOFF.ordinal();
    }

    public int getAppStartSpecRegion(String packageName, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return -1;
        }
        return this.mAppStartAreaCfg;
    }

    public int getAppMngDeviceLevel() {
        if (!mEnabled) {
            return 1;
        }
        return this.mDeviceLevel;
    }

    private boolean isAppStartSpecCallerAction(String pkg, int appAction, AwareAppStartStatusCache status, AppMngConstant.AppStartSource source) {
        if (!this.mAppStartEnabled) {
            return false;
        }
        if (DEBUG) {
            AwareLog.i(TAG, "isAppStartSpecAction " + pkg + ",action:" + appAction);
        }
        if (appAction < 0 || appAction >= AppStartPolicyCfg.AppStartCallerAction.values().length) {
            return false;
        }
        return isAppStartSpecCallerActionComm(pkg, AppStartPolicyCfg.AppStartCallerAction.values()[appAction], status, source);
    }

    private boolean isAppStartSpecCallerActionComm(String pkg, AppStartPolicyCfg.AppStartCallerAction appAction, AwareAppStartStatusCache status, AppMngConstant.AppStartSource source) {
        if (!mEnabled) {
            return false;
        }
        int i = AnonymousClass3.$SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerAction[appAction.ordinal()];
        if (i == 1) {
            return isPushSdkComp(status.cacheCallerUid, (AppMngConstant.AppStartSource.THIRD_BROADCAST.equals(source) || AppMngConstant.AppStartSource.SYSTEM_BROADCAST.equals(source)) ? status.cacheAction : status.cacheCompName, status.cacheUid, source, status.cacheIsSystemApp, status.cacheStatusCacheExt.abroad);
        } else if (i == 2) {
            return status.cacheIsBtMediaBrowserCaller;
        } else {
            if (i == 3) {
                return status.cacheNotifyListenerCaller;
            }
            if (i != 4) {
                return false;
            }
            return status.cacheStatusCacheExt.hwPush;
        }
    }

    private boolean isAppStartSpecCallerStatus(String pkg, int appStatus, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return false;
        }
        if (DEBUG) {
            AwareLog.i(TAG, "isAppStartSpecCallerStatus " + pkg + ",action:" + appStatus);
        }
        if (appStatus < 0 || appStatus >= AppStartPolicyCfg.AppStartCallerStatus.values().length) {
            return false;
        }
        int i = AnonymousClass3.$SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerStatus[AppStartPolicyCfg.AppStartCallerStatus.values()[appStatus].ordinal()];
        if (i == 1) {
            return !status.cacheIsCallerFg;
        }
        if (i != 2) {
            return false;
        }
        return status.cacheIsCallerFg;
    }

    private boolean isAppStartSpecType(String pkg, int appType, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return false;
        }
        if (DEBUG) {
            AwareLog.i(TAG, "isAppStartSpecType " + pkg + ",type:" + appType);
        }
        if (appType < 0 || appType >= AppStartPolicyCfg.AppStartTargetType.values().length) {
            return false;
        }
        AppStartPolicyCfg.AppStartTargetType appStartEnum = AppStartPolicyCfg.AppStartTargetType.values()[appType];
        if (AnonymousClass3.$SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[appStartEnum.ordinal()] != 1) {
            return isAppStartSpecTypeComm(pkg, appStartEnum, status);
        }
        AppStartupDataMgr appStartupDataMgr = this.mDataMgr;
        if (appStartupDataMgr != null) {
            return appStartupDataMgr.isBlindAssistPkg(pkg);
        }
        return false;
    }

    private boolean isAppStartSpecTypeComm(String pkg, AppStartPolicyCfg.AppStartTargetType appType, AwareAppStartStatusCache status) {
        if (!mEnabled) {
            return false;
        }
        switch (appType) {
            case TTS:
                return isTTSPkg(pkg);
            case IM:
                if (status.cacheAppType == -100) {
                    status.cacheAppType = getAppMngSpecType(pkg);
                }
                if (status.cacheAppType == 0) {
                    return true;
                }
                return false;
            case CLOCK:
                return isAlarmApp(pkg, status);
            case PAY:
                return isPayPkg(pkg, status);
            case SHARE:
                return isSharePkg(pkg, status);
            case BUSINESS:
                if (status.cacheAppType == -100) {
                    status.cacheAppType = getAppMngSpecType(pkg);
                }
                if (status.cacheAppType == 11) {
                    return true;
                }
                return false;
            case EMAIL:
                return isAppMngSpecTypeFreqTopN(pkg, 1, -1);
            case RCV_MONEY:
                if (status.cacheAppType == -100) {
                    status.cacheAppType = getAppMngSpecType(pkg);
                }
                if (status.cacheAppType == 34) {
                    return true;
                }
                return false;
            case HABIT_IM:
                return isAllowStartAppByTopN(pkg, status, 0, 3, true);
            case MOSTFREQIM:
                return isAllowStartAppByTopN(pkg, status, 0, 3, false);
            default:
                return isAppStartSpecTypeCommExt(pkg, appType, status);
        }
    }

    private boolean isAppStartSpecTypeCommExt(String pkg, AppStartPolicyCfg.AppStartTargetType appType, AwareAppStartStatusCache status) {
        if (AnonymousClass3.$SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[appType.ordinal()] != 12) {
            return false;
        }
        if (status.cacheAppType == -100) {
            status.cacheAppType = getAppMngSpecType(pkg);
        }
        if (status.cacheAppType == 5) {
            return true;
        }
        return false;
    }

    private boolean isAppStartSpecStat(String pkg, int statType, AwareAppStartStatusCache status) {
        if (!this.mAppStartEnabled) {
            return false;
        }
        if (DEBUG) {
            AwareLog.i(TAG, "isAppStartSpecStat " + pkg + ",status:" + statType);
        }
        if (statType < 0 || statType >= AppStartPolicyCfg.AppStartTargetStat.values().length) {
            return false;
        }
        AppStartPolicyCfg.AppStartTargetStat appStartEnum = AppStartPolicyCfg.AppStartTargetStat.values()[statType];
        int i = AnonymousClass3.$SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[appStartEnum.ordinal()];
        if (i == 1) {
            AppStartupDataMgr appStartupDataMgr = this.mDataMgr;
            if (appStartupDataMgr != null) {
                return appStartupDataMgr.isWidgetExistPkg(pkg);
            }
            return false;
        } else if (i == 2) {
            return isProtectUpdateWidget(pkg, status.cacheUid);
        } else {
            if (i != 3) {
                return isAppStartSpecStatComm(pkg, appStartEnum, status);
            }
            return !status.cacheIsAppStop;
        }
    }

    /* renamed from: com.android.server.rms.iaware.appmng.AwareIntelligentRecg$3  reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerAction = new int[AppStartPolicyCfg.AppStartCallerAction.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerStatus = new int[AppStartPolicyCfg.AppStartCallerStatus.values().length];

        static {
            $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat = new int[AppStartPolicyCfg.AppStartTargetStat.values().length];
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.WIDGET.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.WIDGETUPDATE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.ALIVE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.MUSIC.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.RECORD.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.GUIDE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.UPDOWNLOAD.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.HEALTH.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.FGACTIVITY.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.WALLPAPER.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetStat[AppStartPolicyCfg.AppStartTargetStat.INPUTMETHOD.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType = new int[AppStartPolicyCfg.AppStartTargetType.values().length];
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.BLIND.ordinal()] = 1;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.TTS.ordinal()] = 2;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.IM.ordinal()] = 3;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.CLOCK.ordinal()] = 4;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.PAY.ordinal()] = 5;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.SHARE.ordinal()] = 6;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.BUSINESS.ordinal()] = 7;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.EMAIL.ordinal()] = 8;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.RCV_MONEY.ordinal()] = 9;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.HABIT_IM.ordinal()] = 10;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.MOSTFREQIM.ordinal()] = 11;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartTargetType[AppStartPolicyCfg.AppStartTargetType.CLOCKTYPE.ordinal()] = 12;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerStatus[AppStartPolicyCfg.AppStartCallerStatus.BACKGROUND.ordinal()] = 1;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerStatus[AppStartPolicyCfg.AppStartCallerStatus.FOREGROUND.ordinal()] = 2;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerAction[AppStartPolicyCfg.AppStartCallerAction.PUSHSDK.ordinal()] = 1;
            } catch (NoSuchFieldError e26) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerAction[AppStartPolicyCfg.AppStartCallerAction.BTMEDIABROWSER.ordinal()] = 2;
            } catch (NoSuchFieldError e27) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerAction[AppStartPolicyCfg.AppStartCallerAction.NOTIFYLISTENER.ordinal()] = 3;
            } catch (NoSuchFieldError e28) {
            }
            try {
                $SwitchMap$com$android$server$rms$iaware$appmng$AppStartPolicyCfg$AppStartCallerAction[AppStartPolicyCfg.AppStartCallerAction.HWPUSH.ordinal()] = 4;
            } catch (NoSuchFieldError e29) {
            }
        }
    }

    private boolean isAppStartSpecStatComm(String pkg, AppStartPolicyCfg.AppStartTargetStat appStartEnum, AwareAppStartStatusCache status) {
        if (!mEnabled) {
            return false;
        }
        switch (appStartEnum) {
            case MUSIC:
                return isAudioOutStatus(status.cacheUid);
            case RECORD:
                return isAudioInStatus(status.cacheUid);
            case GUIDE:
                return isGpsStatus(status.cacheUid);
            case UPDOWNLOAD:
                return isUpDownStatus(status.cacheUid);
            case HEALTH:
                return isSensorStatus(status.cacheUid);
            case FGACTIVITY:
                return status.cacheIsTargetFg;
            case WALLPAPER:
                if (status.cacheUid == getDefaultWallPaperUid()) {
                    return true;
                }
                return false;
            case INPUTMETHOD:
                if (status.cacheUid == getDefaultInputMethodUid()) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /* JADX INFO: Multiple debug info for r0v2 java.util.Set<java.lang.String>: [D('tmpRecent' java.util.Set<java.lang.String>), D('habit' com.android.server.rms.algorithm.AwareUserHabit)] */
    public boolean isAppMngSpecRecent(String pkg, int isRecent, int recent) {
        if (!mEnabled || recent <= 0) {
            return false;
        }
        if (this.mRecent == null || this.mTempRecent != recent || SystemClock.elapsedRealtimeNanos() - this.mUpdateTimeRecent > 10000000) {
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit == null) {
                return false;
            }
            this.mRecent = habit.getBackgroundApps((long) recent);
            this.mUpdateTimeRecent = SystemClock.elapsedRealtimeNanos();
            this.mTempRecent = recent;
        }
        Set<String> tmpRecent = this.mRecent;
        if (tmpRecent == null) {
            return false;
        }
        if (isRecent == 0) {
            return !tmpRecent.contains(pkg);
        }
        return tmpRecent.contains(pkg);
    }

    public int getAppStartSpecAppSrcRangeResult(String pkg) {
        if (this.mDataMgr.isAutoMngPkg(pkg)) {
            return 0;
        }
        return AppTypeRecoManager.getInstance().getAppWhereFrom(pkg);
    }

    public int getAppMngSpecRecent(String pkg, LinkedHashMap<Integer, RuleNode> recents) {
        if (!mEnabled || recents == null) {
            return -1;
        }
        for (Integer recent : recents.keySet()) {
            if (recent != null && recent.intValue() > 0 && isAppMngSpecRecent(pkg, 1, recent.intValue())) {
                return recent.intValue();
            }
        }
        return -1;
    }

    public boolean isAppMngSpecTypeFreqTopN(String pkg, int type, int topN) {
        if (!mEnabled || pkg == null) {
            return false;
        }
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit == null) {
            AwareLog.e(TAG, "AwareUserHabit is null");
            return false;
        } else if (topN == -1) {
            updateCacheIfNeed(habit, type);
            Set<String> cachedTopN = this.mCachedTypeTopN.get(Integer.valueOf(type));
            if (cachedTopN == null) {
                return false;
            }
            return cachedTopN.contains(pkg);
        } else {
            List<String> topNList = habit.getMostFreqAppByType(type, topN);
            if (topNList == null) {
                return false;
            }
            return topNList.contains(pkg);
        }
    }

    public boolean isAppMngSpecTypeFreqTopNInDay(String pkg, int type, int topN) {
        if (!mEnabled || pkg == null) {
            return false;
        }
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit == null) {
            AwareLog.e(TAG, "AwareUserHabit is null");
            return false;
        }
        List<String> topNList = habit.getMostFreqAppByTypeEx(type, topN);
        if (topNList == null) {
            return false;
        }
        return topNList.contains(pkg);
    }

    private boolean isTopHabitAppInStart(String pkg, int type, int topN, boolean isBoot) {
        if (!this.mAppStartEnabled || pkg == null || topN <= 0) {
            return false;
        }
        if (!isBoot) {
            return isAppMngSpecTypeFreqTopNInDay(pkg, type, topN);
        }
        AppTypeRecoManager appTypeRecoManager = AppTypeRecoManager.getInstance();
        if (appTypeRecoManager != null) {
            return appTypeRecoManager.isTopIM(pkg, topN);
        }
        AwareLog.w(TAG, "AppTypeRecoManager is null");
        return false;
    }

    private void updateCacheIfNeed(AwareUserHabit habit, int type) {
        long curTime = SystemClock.elapsedRealtime();
        ArrayMap<Integer, Set<String>> cachedTopNMap = new ArrayMap<>(this.mCachedTypeTopN);
        ArrayMap<Integer, Long> lastUpdateTimeMap = new ArrayMap<>(this.mLastUpdateTime);
        Long lastUpdateTime = lastUpdateTimeMap.get(Integer.valueOf(type));
        if (lastUpdateTime == null) {
            lastUpdateTime = 0L;
        }
        if (curTime - lastUpdateTime.longValue() >= 10000) {
            List<String> topN = habit.getMostFreqAppByType(type, -1);
            if (topN == null) {
                topN = new ArrayList<>();
            }
            ArraySet<String> cachedTopN = new ArraySet<>();
            cachedTopN.addAll(topN);
            cachedTopNMap.put(Integer.valueOf(type), cachedTopN);
            lastUpdateTimeMap.put(Integer.valueOf(type), Long.valueOf(curTime));
            this.mLastUpdateTime = lastUpdateTimeMap;
            this.mCachedTypeTopN = cachedTopNMap;
        }
    }

    public int getAppMngSpecTypeFreqTopN(String pkg, LinkedHashMap<Integer, RuleNode> topNs) {
        if (!mEnabled || pkg == null || topNs == null) {
            return -1;
        }
        for (Integer topN : topNs.keySet()) {
            if (topN != null && topN.intValue() >= 0) {
                int topNInUserHabit = topN.intValue();
                if (topN.intValue() == 0) {
                    topNInUserHabit = -1;
                }
                AppTypeRecoManager atrm = AppTypeRecoManager.getInstance();
                if (isAppMngSpecTypeFreqTopN(pkg, atrm.convertType(atrm.getAppType(pkg)), topNInUserHabit)) {
                    return topN.intValue();
                }
            }
        }
        return -1;
    }

    /* JADX INFO: Multiple debug info for r0v2 java.util.List<java.lang.String>: [D('habit' com.android.server.rms.algorithm.AwareUserHabit), D('tmpHabbitTopN' java.util.List<java.lang.String>)] */
    public boolean isAppMngSpecHabbitTopN(String pkg, int isHabbitTopN, int topN) {
        if (!mEnabled || topN <= 0) {
            return false;
        }
        if (this.mHabbitTopN == null || this.mTempHTopN != topN || SystemClock.elapsedRealtimeNanos() - this.mUpdateTimeHabbitTopN > 10000000) {
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit == null) {
                return false;
            }
            this.mHabbitTopN = habit.getTopN(topN);
            this.mUpdateTimeHabbitTopN = SystemClock.elapsedRealtimeNanos();
            this.mTempHTopN = topN;
        }
        List<String> tmpHabbitTopN = this.mHabbitTopN;
        if (tmpHabbitTopN == null) {
            return false;
        }
        if (isHabbitTopN == 0) {
            return !tmpHabbitTopN.contains(pkg);
        }
        return tmpHabbitTopN.contains(pkg);
    }

    public int getAppMngSpecHabbitTopN(String pkg, LinkedHashMap<Integer, RuleNode> topNs) {
        if (!mEnabled || topNs == null || pkg == null) {
            return -1;
        }
        for (Integer topN : topNs.keySet()) {
            if (topN != null && topN.intValue() > 0 && isAppMngSpecHabbitTopN(pkg, 1, topN.intValue())) {
                return topN.intValue();
            }
        }
        return -1;
    }

    public boolean isAppMngSpecType(String pkg, int appType) {
        if (mEnabled && appType == AppTypeRecoManager.getInstance().getAppType(pkg)) {
            return true;
        }
        return false;
    }

    public int getAppMngSpecType(String pkg) {
        if (!mEnabled || pkg == null) {
            return -1;
        }
        return AppTypeRecoManager.getInstance().getAppType(pkg);
    }

    public boolean isAppMngSpecStat(AwareProcessInfo info, int statType) {
        if (!mEnabled || info == null) {
            return false;
        }
        AppStatusUtils.Status[] values = AppStatusUtils.Status.values();
        if (values.length <= statType || statType < 0) {
            return false;
        }
        return AppStatusUtils.getInstance().checkAppStatus(values[statType], info);
    }

    public int getAppMngSpecStat(AwareProcessInfo info, LinkedHashMap<Integer, RuleNode> statTypes) {
        if (!mEnabled || statTypes == null || info == null) {
            return -1;
        }
        AppStatusUtils.Status[] values = AppStatusUtils.Status.values();
        for (Integer statType : statTypes.keySet()) {
            if (statType != null && values.length > statType.intValue() && statType.intValue() >= 0 && AppStatusUtils.getInstance().checkAppStatus(values[statType.intValue()], info)) {
                return statType.intValue();
            }
        }
        return -1;
    }

    @SuppressLint({"PreferForInArrayList"})
    public boolean isInSmallSampleList(AwareProcessInfo awareProcInfo) {
        if (!mEnabled || awareProcInfo == null || awareProcInfo.procProcInfo == null) {
            return false;
        }
        synchronized (this.mSmallSampleList) {
            Iterator it = awareProcInfo.procProcInfo.mPackageName.iterator();
            while (it.hasNext()) {
                if (!this.mSmallSampleList.contains((String) it.next())) {
                    return false;
                }
            }
            return true;
        }
    }

    public void setSmallSampleList(List<String> samplesList) {
        if (samplesList != null) {
            synchronized (this.mSmallSampleList) {
                this.mSmallSampleList.clear();
                this.mSmallSampleList.addAll(samplesList);
            }
        }
    }

    public List<String> getSmallSampleList() {
        List<String> smallSamples;
        synchronized (this.mSmallSampleList) {
            smallSamples = new ArrayList<>(this.mSmallSampleList);
        }
        return smallSamples;
    }

    public boolean isAppFrozen(int uid) {
        boolean contains;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mFrozenAppList) {
            contains = this.mFrozenAppList.contains(uid);
        }
        return contains;
    }

    public boolean isAppBluetooth(int uid) {
        boolean contains;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mBluetoothAppList) {
            contains = this.mBluetoothAppList.contains(uid);
        }
        return contains;
    }

    public boolean isAudioOutInstant(int uid) {
        boolean contains;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mAudioOutInstant) {
            contains = this.mAudioOutInstant.contains(uid);
        }
        return contains;
    }

    public boolean isToastWindow(int pid) {
        boolean z = false;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mToasts) {
            AwareProcessWindowInfo toastInfo = this.mToasts.get(pid);
            if (toastInfo != null && !toastInfo.isEvil()) {
                z = true;
            }
        }
        return z;
    }

    private void registerContentObserver(Context context, ContentObserver observer) {
        if (!this.mIsObsvInit.get()) {
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("enabled_accessibility_services"), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("tts_default_synth"), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(VALUE_BT_BLE_CONNECT_APPS), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(VALUE_BT_LAST_BLE_DISCONNECT), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("sms_default_application"), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(HwArbitrationDEFS.KEY_BLUETOOTH_ON), false, observer, -1);
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("unified_device_name"), false, observer, -1);
            this.mIsObsvInit.set(true);
        }
    }

    private void unregisterContentObserver(Context context, ContentObserver observer) {
        if (this.mIsObsvInit.get()) {
            context.getContentResolver().unregisterContentObserver(observer);
            this.mIsObsvInit.set(false);
        }
    }

    private void registerHsmProviderObserver(Context context, ContentObserver observer) {
        if (!this.mIsDozeObsvInit.get()) {
            Uri dozeUri = Uri.parse(URI_SYSTEM_MANAGER_UNIFIED_POWER_APP);
            if (context.getContentResolver().acquireProvider(dozeUri) != null) {
                context.getContentResolver().registerContentObserver(dozeUri, true, observer, -1);
                this.mIsDozeObsvInit.set(true);
                return;
            }
            AwareLog.i(TAG, "register observer failed: doze database is not exist!");
        }
    }

    private void unregisterHsmProviderObserver(Context context, ContentObserver observer) {
        if (this.mIsDozeObsvInit.get()) {
            if (context.getContentResolver().acquireProvider(Uri.parse(URI_SYSTEM_MANAGER_UNIFIED_POWER_APP)) != null) {
                context.getContentResolver().unregisterContentObserver(observer);
                this.mIsDozeObsvInit.set(false);
                return;
            }
            AwareLog.i(TAG, "unregister observer failed: doze database is not exist!");
        }
    }

    /* access modifiers changed from: package-private */
    public static class AwareAppStartInfo {
        public int callerUid;
        public String cls = null;
        public String cmp;
        public long timestamp;

        public AwareAppStartInfo(int callerUid2, String cmp2) {
            this.callerUid = callerUid2;
            this.cmp = cmp2;
            parseCmp(cmp2);
            this.timestamp = SystemClock.elapsedRealtime();
        }

        private void parseCmp(String cmp2) {
            if (cmp2 != null) {
                String[] strs = cmp2.split("/");
                if (strs.length == 2) {
                    this.cls = strs[1];
                }
            }
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object obj) {
            String str;
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AwareAppStartInfo info = (AwareAppStartInfo) obj;
            String str2 = this.cmp;
            if (str2 == null || (str = info.cmp) == null || this.callerUid != info.callerUid || !str2.equals(str)) {
                return false;
            }
            return true;
        }

        public String toString() {
            return "{caller:" + this.callerUid + "cmp:" + this.cmp + " time:" + this.timestamp + "}";
        }
    }

    /* access modifiers changed from: package-private */
    public static class AlarmInfo {
        public boolean bfgset = false;
        public boolean bhandled = false;
        public int count = 0;
        public String packagename;
        public int perception_count = 0;
        public int reason;
        public long startTime = 0;
        public int status = 0;
        public String tag;
        public int uid;
        public int unperception_count = 0;

        public AlarmInfo(int uid2, String packagename2, String tag2) {
            this.uid = uid2;
            this.packagename = packagename2;
            this.tag = tag2;
            this.bhandled = false;
        }

        public AlarmInfo copy() {
            AlarmInfo dst = new AlarmInfo(this.uid, this.packagename, this.tag);
            dst.uid = this.uid;
            dst.packagename = this.packagename;
            dst.tag = this.tag;
            dst.startTime = this.startTime;
            dst.status = this.status;
            dst.reason = this.reason;
            dst.count = this.count;
            dst.perception_count = this.perception_count;
            dst.unperception_count = this.unperception_count;
            dst.bhandled = this.bhandled;
            dst.bfgset = this.bfgset;
            return dst;
        }

        public String toString() {
            return "package:" + this.packagename + ',' + "uid:" + this.uid + ',' + "tag:" + this.tag + ',' + "startTime:" + this.startTime + ',' + "status:" + this.status + ',' + "reason:" + this.reason + ',' + "count:" + this.count + ',' + "unperception_count:" + this.unperception_count + ',' + "perception_count:" + this.perception_count + ',' + "bfgset:" + this.bfgset + '.';
        }
    }

    public void dumpInputMethod(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump default InputMethod]");
            pw.println(this.mDefaultInputMethod + ",uid:" + this.mDefaultInputMethodUid);
        }
    }

    public void dumpWallpaper(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump Wallpaper]");
            pw.println(this.mDefaultWallPaper + ",uid:" + this.mDefaultWallPaperUid);
        }
    }

    public void dumpAccessibility(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump Accessibility]");
            synchronized (this.mAccessPkg) {
                for (String pkg : this.mAccessPkg) {
                    pw.println(pkg);
                }
            }
        }
    }

    public void dumpTts(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump TTS]");
            synchronized (this.mTTSPkg) {
                for (String pkg : this.mTTSPkg) {
                    pw.println(pkg);
                }
            }
        }
    }

    public void dumpPushSDK(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpPushSDK start");
            synchronized (this.mPushSDKClsList) {
                pw.println("recg push sdk cls:" + this.mPushSDKClsList);
            }
            synchronized (this.mGoodPushSDKClsList) {
                pw.println("good push sdk cls:" + this.mGoodPushSDKClsList);
            }
            synchronized (this.mAppStartInfoList) {
                Iterator<AwareAppStartInfo> it = this.mAppStartInfoList.iterator();
                while (it.hasNext()) {
                    pw.println("start record:" + it.next().toString());
                }
            }
            ComponentRecoManager.getInstance().dumpBadComponent(pw);
            pw.println("AwareIntelligentRecg dumpPushSDK end");
        }
    }

    public void dumpSms(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump default sms package]");
            pw.println(this.mDefaultSms);
        }
    }

    private void updateGmsCallerApp(Context context) {
        SparseSet gmsUid = new SparseSet();
        Iterator<String> it = this.mGmsCallerAppPkg.iterator();
        while (it.hasNext()) {
            int uid = getUIDByPackageName(context, it.next());
            if (uid != -1) {
                gmsUid.add(uid);
            }
        }
        this.mGmsCallerAppUid = gmsUid;
    }

    private void updateGmsCallerAppInit(Context context) {
        ArrayList<String> gmsList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "gmscaller");
        ArraySet<String> gmsSet = new ArraySet<>();
        if (gmsList != null) {
            gmsSet.addAll(gmsList);
        }
        this.mGmsCallerAppPkg = gmsSet;
        updateGmsCallerApp(context);
    }

    private void updateGmsCallerAppForInstall(String pkg) {
        MultiTaskManagerService multiTaskManagerService;
        if (this.mGmsCallerAppPkg.contains(pkg) && (multiTaskManagerService = this.mMtmService) != null) {
            updateGmsCallerApp(multiTaskManagerService.context());
        }
    }

    public void appStartEnable(AppStartupDataMgr dataMgr, Context context) {
        this.mIsAbroadArea = AwareDefaultConfigList.isAbroadArea();
        this.mDataMgr = dataMgr;
        updateGmsCallerAppInit(context);
        initAppMngCustPropConfig();
        initGmsAppConfig();
        initMemoryInfo();
        initCommonInfo();
        this.mAppStartEnabled = true;
    }

    public void appStartDisable() {
        this.mAppStartEnabled = false;
    }

    public void dumpToastWindow(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            SparseSet toasts = new SparseSet();
            SparseSet toastsEvil = new SparseSet();
            getToastWindows(toasts, toastsEvil);
            pw.println("");
            pw.println("[ToastList]:" + toasts);
            pw.println("[EvilToastList]:" + toastsEvil);
            synchronized (this.mToasts) {
                for (int i = this.mToasts.size() - 1; i >= 0; i += -1) {
                    AwareProcessWindowInfo toastInfo = this.mToasts.valueAt(i);
                    pw.println("[Toast pid ]:" + this.mToasts.keyAt(i) + " pkg:" + toastInfo.pkg + " isEvil:" + toastInfo.isEvil());
                }
            }
        }
    }

    public void dumpIsVisibleWindow(PrintWriter pw, int userid, String pkg, int type) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("");
            boolean result = HwSysResManager.getInstance().isVisibleWindow(userid, pkg, type);
            pw.println("[dumpIsVisibleWindow ]:" + result);
        }
    }

    public void dumpDefaultTts(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump Default TTS]");
            pw.println(this.mDefaultTTS);
        }
    }

    public void dumpDefaultAppType(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[dump Pay Pkg]");
            Set<String> set = this.mPayPkgList;
            if (set == null) {
                set = HwAPPQoEUtils.INVALID_STRING_VALUE;
            }
            pw.println(set);
            pw.println("[dump Share Pkg]");
            Set<String> set2 = this.mSharePkgList;
            if (set2 == null) {
                set2 = HwAPPQoEUtils.INVALID_STRING_VALUE;
            }
            pw.println(set2);
            pw.println("[dump ActTopIMCN]");
            pw.println(this.mActTopIMCN);
            pw.println("[dump AppLockClass]");
            pw.println(this.mAppLockClass);
        }
    }

    public void dumpFrozen(PrintWriter pw, int uid) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpFrozen start");
            synchronized (this.mFrozenAppList) {
                if (uid == 0) {
                    for (int i = this.mFrozenAppList.size() - 1; i >= 0; i += -1) {
                        int appUid = this.mFrozenAppList.keyAt(i);
                        pw.println("frozen app:" + InnerUtils.getPackageNameByUid(appUid) + ",uid:" + appUid);
                    }
                } else {
                    pw.println("uid:" + uid + ",frozen:" + isAppFrozen(uid));
                }
            }
        }
    }

    public void dumpBluetooth(PrintWriter pw, int uid) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpBluetooth start");
            synchronized (this.mBluetoothAppList) {
                if (uid == 0) {
                    for (int i = this.mBluetoothAppList.size() - 1; i >= 0; i += -1) {
                        int appUid = this.mBluetoothAppList.keyAt(i);
                        pw.println("bluetooth app:" + InnerUtils.getPackageNameByUid(appUid) + ",uid:" + appUid);
                    }
                } else {
                    pw.println("uid:" + uid + ",bluetooth:" + isAppBluetooth(uid));
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void resolvePkgsName(String pkgs, int eventType) {
        if (eventType == 1) {
            if (pkgs != null) {
                this.mHandler.removeMessages(17);
                String[] pkglist = pkgs.split("#");
                synchronized (this.mNotCleanPkgList) {
                    for (String str : pkglist) {
                        this.mNotCleanPkgList.add(str);
                    }
                }
            }
        } else if (eventType == 0) {
            this.mHandler.sendEmptyMessageDelayed(17, (long) this.mNotCleanDuration);
        }
    }

    public boolean isNotBeClean(String pkg) {
        boolean contains;
        synchronized (this.mNotCleanPkgList) {
            contains = this.mNotCleanPkgList.contains(pkg);
        }
        return contains;
    }

    public void dumpNotClean(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpNotClean start");
            StringBuffer sb = new StringBuffer();
            synchronized (this.mNotCleanPkgList) {
                for (String pkgName : this.mNotCleanPkgList) {
                    sb.append(pkgName);
                    sb.append(" ");
                }
            }
            pw.println(sb.toString());
        }
    }

    public void dumpKbgApp(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpKbgApp start");
            synchronized (this.mStatusAudioIn) {
                for (int i = this.mStatusAudioIn.size() - 1; i >= 0; i += -1) {
                    pw.println("mStatusAudioIn app:" + this.mStatusAudioIn.keyAt(i));
                }
            }
            synchronized (this.mStatusAudioOut) {
                for (int i2 = this.mStatusAudioOut.size() - 1; i2 >= 0; i2 += -1) {
                    pw.println("mStatusAudioOut app:" + this.mStatusAudioOut.keyAt(i2));
                }
            }
            synchronized (this.mStatusGps) {
                for (int i3 = this.mStatusGps.size() - 1; i3 >= 0; i3 += -1) {
                    pw.println("mStatusGps app:" + this.mStatusGps.keyAt(i3));
                }
            }
            synchronized (this.mStatusUpDown) {
                for (int i4 = this.mStatusUpDown.size() - 1; i4 >= 0; i4 += -1) {
                    pw.println("mStatusUpDown app:" + this.mStatusUpDown.keyAt(i4));
                }
            }
            synchronized (this.mStatusSensor) {
                for (int i5 = this.mStatusSensor.size() - 1; i5 >= 0; i5 += -1) {
                    pw.println("mStatusSensor app:" + this.mStatusSensor.keyAt(i5));
                }
            }
            synchronized (this.mStatusUpDownElapse) {
                long timeCur = SystemClock.elapsedRealtime();
                for (Map.Entry<Integer, Long> m : this.mStatusUpDownElapse.entrySet()) {
                    int uid = m.getKey().intValue();
                    long time = m.getValue().longValue();
                    pw.println("mStatusUpDown History:" + uid + "," + ((timeCur - time) / 1000));
                }
            }
            pw.println("mIsScreenOnPm:" + this.mIsScreenOnPm);
            pw.println("mAppStartAreaCfg:" + this.mAppStartAreaCfg);
            pw.println("mDeviceLevel:" + this.mDeviceLevel);
        }
    }

    public void dumpAlarms(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpAlarms start");
            synchronized (this.mAlarmMap) {
                int i = this.mAlarmMap.size() - 1;
                while (i >= 0) {
                    ArrayMap<String, AlarmInfo> alarms = this.mAlarmMap.valueAt(i);
                    if (alarms != null) {
                        Iterator<String> it = alarms.keySet().iterator();
                        while (it.hasNext()) {
                            pw.println("AwareIntelligentRecg alarm:" + alarms.get(it.next()));
                        }
                        i--;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void dumpAlarmActions(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpAlarmActions start");
            synchronized (this.mAlarmCmps) {
                for (Map.Entry<String, CmpTypeInfo> entry : this.mAlarmCmps.entrySet()) {
                    CmpTypeInfo info = entry.getValue();
                    StringBuilder sb = new StringBuilder();
                    sb.append("AwareIntelligentRecg alarm action:");
                    sb.append(info != null ? info.toString() : "");
                    pw.println(sb.toString());
                }
            }
            pw.println("Alarm White Pkg:");
            Object obj = this.mAlarmPkgList;
            if (obj == null) {
                obj = HwAPPQoEUtils.INVALID_STRING_VALUE;
            }
            pw.println(obj);
            pw.println("UnRemind App Types:");
            Object obj2 = this.mUnRemindAppTypeList;
            if (obj2 == null) {
                obj2 = HwAPPQoEUtils.INVALID_STRING_VALUE;
            }
            pw.println(obj2);
        }
    }

    public void dumpHwStopList(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpHwStopList start");
            synchronized (this.mHwStopUserIdPkg) {
                Iterator<String> it = this.mHwStopUserIdPkg.iterator();
                while (it.hasNext()) {
                    pw.println("  pkg#userId : " + it.next());
                }
            }
        }
    }

    public void dumpScreenRecording(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpScreenRecord start");
            synchronized (this.mScreenRecordAppList) {
                for (int i = this.mScreenRecordAppList.size() - 1; i >= 0; i += -1) {
                    int uid = this.mScreenRecordAppList.keyAt(i);
                    pw.println(" screenRecord uid:" + uid + " and pkgName is " + InnerUtils.getPackageNameByUid(uid));
                }
            }
        }
    }

    public void dumpCameraRecording(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpCameraRecording start");
            synchronized (this.mCameraUseAppList) {
                for (int i = this.mCameraUseAppList.size() - 1; i >= 0; i--) {
                    int uid = this.mCameraUseAppList.keyAt(i);
                    if (!AwareAppAssociate.getInstance().isForeGroundApp(uid)) {
                        if (AwareAppAssociate.getInstance().hasWindow(uid)) {
                            pw.println("  cameraRecord uid:" + uid + " PKG is " + InnerUtils.getPackageNameByUid(uid));
                        }
                    }
                }
            }
        }
    }

    public void dumpGmsCallerList(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpGmsCallerList start");
            pw.println("Gms UID:" + this.mGmsCallerAppUid);
            pw.println("Gms PKG:" + this.mGmsCallerAppPkg);
        }
    }

    public void dumpWidgetUpdateInterval(PrintWriter pw, int intervalMs) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("mWidgetCheckUpdateCnt " + this.mWidgetCheckUpdateCnt);
            pw.println("mWidgetCheckUpdateInterval raw(ms) " + this.mWidgetCheckUpdateInterval);
            this.mWidgetCheckUpdateInterval = (long) intervalMs;
            sendMsgWidgetUpDownLoadClear();
            pw.println("mWidgetCheckUpdateInterval current(ms) " + this.mWidgetCheckUpdateInterval);
        }
    }

    public static void commEnable() {
        getInstance().initialize();
        mEnabled = true;
    }

    public static void commDisable() {
        mEnabled = false;
        getInstance().deInitialize();
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public void registerToastCallback(IAwareToastCallback callback) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                if (!this.mCallbacks.contains(callback)) {
                    this.mCallbacks.add(callback);
                }
            }
        }
    }

    public void unregisterToastCallback(IAwareToastCallback callback) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                if (this.mCallbacks.contains(callback)) {
                    this.mCallbacks.remove(callback);
                }
            }
        }
    }

    private void notifyToastChange(int type, int pid) {
        synchronized (this.mCallbacks) {
            if (!this.mCallbacks.isEmpty()) {
                int size = this.mCallbacks.size();
                for (int i = 0; i < size; i++) {
                    this.mCallbacks.valueAt(i).onToastWindowsChanged(type, pid);
                }
            }
        }
    }

    public boolean isBtMediaBrowserCaller(int callerUid, String action) {
        if (callerUid != this.mBluetoothUid || !BTMEDIABROWSER_ACTION.equals(action)) {
            return false;
        }
        return true;
    }

    public boolean isNotifyListenerCaller(int callerUid, String action, WindowProcessController callerApp) {
        if (callerApp != null && callerUid == 1000 && SYSTEM_PROCESS_NAME.equals(callerApp.mName) && NOTIFY_LISTENER_ACTION.equals(action)) {
            return true;
        }
        return false;
    }

    public boolean isGmsCaller(int callerUid) {
        if (!this.mAppStartEnabled) {
            return false;
        }
        return this.mGmsCallerAppUid.contains(UserHandle.getAppId(callerUid));
    }

    @SuppressLint({"PreferForInArrayList"})
    public boolean isAchScreenChangedNum(AwareProcessInfo awareProcInfo) {
        if (!mEnabled || awareProcInfo == null || awareProcInfo.procProcInfo == null) {
            return false;
        }
        Iterator it = awareProcInfo.procProcInfo.mPackageName.iterator();
        while (it.hasNext()) {
            if (!isScreenChangedMeetCondition((String) it.next())) {
                return false;
            }
        }
        return true;
    }

    public void reportScreenChangedTime(long currTime) {
        synchronized (this.mScreenChangedTime) {
            this.mScreenChangedTime.add(Long.valueOf(currTime));
            while (AppMngConfig.getScreenChangedThreshold() < this.mScreenChangedTime.size()) {
                this.mScreenChangedTime.removeFirst();
            }
        }
    }

    public void reportAppChangeToBackground(String pkg, long currTime) {
        if (pkg != null) {
            synchronized (this.mAppChangeToBGTime) {
                this.mAppChangeToBGTime.put(pkg, Long.valueOf(currTime));
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0027, code lost:
        r6 = r9.mAppChangeToBGTime;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0029, code lost:
        monitor-enter(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0030, code lost:
        if (r9.mAppChangeToBGTime.containsKey(r10) != false) goto L_0x0034;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0032, code lost:
        monitor-exit(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0033, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0034, code lost:
        r7 = r9.mAppChangeToBGTime.get(r10).longValue();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0041, code lost:
        monitor-exit(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0044, code lost:
        if (r6 >= r7) goto L_0x0047;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0046, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0047, code lost:
        return true;
     */
    private boolean isScreenChangedMeetCondition(String pkg) {
        if (pkg == null) {
            return false;
        }
        synchronized (this.mScreenChangedTime) {
            if (AppMngConfig.getScreenChangedThreshold() > this.mScreenChangedTime.size()) {
                return false;
            }
            long screenChangedTime = this.mScreenChangedTime.getFirst().longValue();
        }
    }

    public boolean isCurrentUser(int checkUid, int currentUserId) {
        UserManager usm;
        int userId = UserHandle.getUserId(checkUid);
        boolean isCloned = false;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (!(multiTaskManagerService == null || (usm = UserManager.get(multiTaskManagerService.context())) == null)) {
            UserInfo info = usm.getUserInfo(userId);
            isCloned = info != null ? info.isClonedProfile() : false;
        }
        if (userId == currentUserId || (isCloned && currentUserId == 0)) {
            return true;
        }
        return false;
    }

    public boolean isWebViewUid(int uid) {
        return uid == this.mWebViewUid;
    }

    private void removeHwStopFlagByUserId(int removeUserId) {
        if (mEnabled) {
            String removeUserIdStr = String.valueOf(removeUserId);
            synchronized (this.mHwStopUserIdPkg) {
                Iterator<String> it = this.mHwStopUserIdPkg.iterator();
                while (it.hasNext()) {
                    String[] strs = it.next().split("#");
                    int strSize = strs.length;
                    if (strSize > 1 && removeUserIdStr.equals(strs[strSize - 1])) {
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean isCloneUserId(int userId) {
        UserManager usm;
        UserInfo info;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService == null || (usm = UserManager.get(multiTaskManagerService.context())) == null || (info = usm.getUserInfo(userId)) == null) {
            return false;
        }
        return info.isClonedProfile();
    }

    public void setHwStopFlag(int userId, String pkg, boolean hwStop) {
        if (mEnabled && pkg != null && !"".equals(pkg)) {
            String userIdPkg = pkg + "#" + userId;
            synchronized (this.mHwStopUserIdPkg) {
                if (hwStop) {
                    this.mHwStopUserIdPkg.add(userIdPkg);
                } else {
                    this.mHwStopUserIdPkg.remove(userIdPkg);
                }
            }
        }
    }

    public boolean isPkgHasHwStopFlag(int userId, String pkg) {
        boolean isHwStopPkg;
        if (!mEnabled || pkg == null || "".equals(pkg)) {
            return false;
        }
        String userIdPkg = pkg + "#" + userId;
        synchronized (this.mHwStopUserIdPkg) {
            isHwStopPkg = this.mHwStopUserIdPkg.contains(userIdPkg);
        }
        if (DEBUG && isHwStopPkg) {
            AwareLog.i(TAG, "pkg:" + pkg + " userId:" + userId + " is hwStop pkg.");
        }
        return isHwStopPkg;
    }

    public void updateScreenOnFromPm(boolean wakenessChange) {
        if (mEnabled && wakenessChange) {
            sendScreenOnFromPmMsg(false);
        }
    }

    private void sendScreenOnFromPmMsg(boolean delay) {
        IntlRecgHandler intlRecgHandler = this.mHandler;
        if (intlRecgHandler != null) {
            Message msg = intlRecgHandler.obtainMessage();
            msg.what = 18;
            if (delay) {
                this.mHandler.sendMessageDelayed(msg, 1000);
            } else {
                this.mHandler.sendMessage(msg);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleWakenessChangeHandle() {
        PowerManager pm;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null && (pm = (PowerManager) multiTaskManagerService.context().getSystemService("power")) != null) {
            this.mIsScreenOnPm = pm.isScreenOn();
        }
    }

    private void initAppMngCustPropConfig() {
        if (!this.mAppMngPropCfgInit) {
            this.mAppMngPropCfgInit = true;
            String prop = SystemProperties.get("ro.config.iaware_appmngconfigs");
            if (prop != null && !prop.equals("")) {
                String[] strs = prop.split(",");
                if (strs.length >= this.APPMNGPROP_MAXINDEX) {
                    try {
                        this.mAppStartAreaCfg = Integer.parseInt(strs[this.APPMNGPROP_APPSTARTINDEX]);
                    } catch (NumberFormatException e) {
                        AwareLog.i(TAG, "initAppMngCustPropConfig error");
                    }
                }
            }
        }
    }

    public Bundle getTypeTopN(int[] appTypes) {
        List<String> topN;
        int[] iArr = appTypes;
        Bundle result = new Bundle();
        if (iArr == null || iArr.length == 0) {
            return result;
        }
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit == null) {
            AwareLog.e(TAG, "AwareUserHabit is null");
            return result;
        }
        LinkedHashMap<String, Long> lruCache = habit.getLruCache();
        if (lruCache == null) {
            return result;
        }
        ArrayList<String> topNList = new ArrayList<>();
        ArrayList<Integer> typeList = new ArrayList<>();
        int i = 0;
        int size = iArr.length;
        while (i < size) {
            int type = iArr[i];
            if (type >= 0 && (topN = habit.getMostFreqAppByType(AppTypeRecoManager.getInstance().convertType(type), 999)) != null) {
                long now = SystemClock.elapsedRealtime();
                int topNSize = topN.size();
                for (int j = 0; j < topNSize; j++) {
                    String name = topN.get(j);
                    Long lastUseTime = lruCache.get(name);
                    if (lastUseTime != null && now - lastUseTime.longValue() < 172800000) {
                        topNList.add(name);
                        typeList.add(Integer.valueOf(type));
                    }
                }
            }
            i++;
            iArr = appTypes;
        }
        result.putIntegerArrayList(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, typeList);
        result.putStringArrayList("pkg", topNList);
        return result;
    }

    private AwareBroadcastPolicy getIawareBrPolicy() {
        if (this.mIawareBrPolicy == null && MultiTaskManagerService.self() != null) {
            this.mIawareBrPolicy = MultiTaskManagerService.self().getIawareBrPolicy();
        }
        return this.mIawareBrPolicy;
    }

    public void updateBGCheckExcludedInfo(ArraySet<String> excludedPkg) {
        this.mBGCheckExcludedPkg = excludedPkg;
        ArrayList<String> excludedAction = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), BACKGROUND_CHECK_EXCLUDED_ACTION);
        if (excludedAction != null) {
            this.mBGCheckExcludedAction = new ArraySet<>(excludedAction);
        }
    }

    public boolean isExcludedInBGCheck(String pkg, String action) {
        boolean result = false;
        if (action != null && this.mBGCheckExcludedAction.contains(action)) {
            result = true;
        }
        if (pkg == null || !this.mBGCheckExcludedPkg.contains(pkg)) {
            return result;
        }
        return true;
    }

    public void dumpBGCheckExcludeInfo(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("BGCheckExcludedAction :" + this.mBGCheckExcludedAction);
            pw.println("BGCheckExcludedPkg :" + this.mBGCheckExcludedPkg);
        }
    }

    private void initGmsAppConfig() {
        if (!this.mIsAbroadArea) {
            ArrayList<String> gmsList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "gmsapp");
            this.mIsInitGoogleConfig = true;
            ArraySet<String> gmsPkgs = new ArraySet<>();
            if (gmsList != null) {
                gmsPkgs.addAll(gmsList);
            }
            this.mGmsAppPkg = gmsPkgs;
            ArrayList<String> delaytime = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "google_delaytime");
            if (delaytime != null && delaytime.size() == 1) {
                try {
                    this.mGoogleConnDealyTime = Long.parseLong(delaytime.get(0)) * 1000;
                } catch (NumberFormatException e) {
                    AwareLog.i(TAG, "initGoogledelayTime error");
                }
            }
            initGoogleConnTime();
            removeAppStartFeatureGMSList();
        }
    }

    private void initMemoryInfo() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        this.mDeviceTotalMemory = minfo.getTotalSize() / 1048576;
        if (this.mDeviceTotalMemory == -1) {
            AwareLog.e(TAG, "read device memory faile");
        }
        this.mDeviceMemoryOfGIGA = calMemorySizeOfGiga(this.mDeviceTotalMemory);
        AwareLog.i(TAG, "Current Device Total Memory is " + this.mDeviceTotalMemory + " and about " + this.mDeviceMemoryOfGIGA + "G");
    }

    private void initGoogleConnTime() {
        String prop = SystemProperties.get(PROPERTIES_GOOELE_CONNECTION);
        if (prop == null || prop.equals("")) {
            this.mGoogleConnStat = false;
            this.mGoogleDisConnTime = 0;
            this.mGoogleConnStatDecay = false;
            return;
        }
        String[] strs = prop.split(",");
        if (strs.length < this.GOOGLECONNPROP_MAXINDEX) {
            this.mGoogleConnStat = false;
            this.mGoogleDisConnTime = 0;
            this.mGoogleConnStatDecay = false;
            return;
        }
        try {
            this.mGoogleConnStat = Integer.parseInt(strs[this.GOOGLECONNPROP_STARTINDEX + 1]) == 1;
            this.mGoogleDisConnTime = Long.parseLong(strs[this.GOOGLECONNPROP_STARTINDEX]) * 1000;
            long now = System.currentTimeMillis();
            if (this.mGoogleConnStat) {
                this.mGoogleConnStatDecay = true;
            } else if (now - this.mGoogleDisConnTime < this.mGoogleConnDealyTime) {
                this.mGoogleConnStatDecay = true;
            } else {
                this.mGoogleConnStatDecay = false;
            }
        } catch (NumberFormatException e) {
            AwareLog.i(TAG, "initGoogleConnTime error");
        }
    }

    public boolean isGmsApp(String pkg) {
        return this.mGmsAppPkg.contains(pkg);
    }

    public boolean isGmsAppAndNeedCtrl(String pkg) {
        if (!this.mIsAbroadArea && !this.mGoogleConnStatDecay && isGmsApp(pkg)) {
            return true;
        }
        return false;
    }

    public void reportGoogleConn(boolean conn) {
        if (getIawareBrPolicy() != null) {
            this.mIawareBrPolicy.reportGoogleConn(conn);
        }
        if (!this.mIsAbroadArea) {
            if (!conn) {
                long now = System.currentTimeMillis();
                if (this.mGoogleConnStat) {
                    this.mGoogleDisConnTime = now;
                } else if (now - this.mGoogleDisConnTime >= this.mGoogleConnDealyTime && this.mGoogleConnStatDecay) {
                    this.mGoogleConnStatDecay = false;
                    removeFeatureGMSList();
                }
            } else if (!this.mGoogleConnStatDecay) {
                this.mGoogleConnStatDecay = true;
                addFeatureGMSList();
            }
            if (this.mGoogleConnStat != conn) {
                this.mGoogleConnStat = conn;
                restoreGoogleConnTime();
            }
        }
    }

    private void addFeatureGMSList() {
        DecisionMaker.getInstance().addFeatureList(AppMngConstant.AppMngFeature.APP_CLEAN, this.mGMSAppCleanPolicyList);
        DecisionMaker.getInstance().addFeatureList(AppMngConstant.AppMngFeature.APP_START, this.mGMSAppStartPolicyList);
    }

    private void removeFeatureGMSList() {
        removeAppStartFeatureGMSList();
        removeAppCleanFeatureGMSList();
    }

    private void restoreGoogleConnTime() {
        String conn_info = this.mGoogleConnStat ? "1" : "0";
        SystemProperties.set(PROPERTIES_GOOELE_CONNECTION, String.valueOf(this.mGoogleDisConnTime / 1000) + "," + conn_info);
    }

    public void removeAppStartFeatureGMSList() {
        if (this.mIsInitGoogleConfig && !this.mIsAbroadArea && !this.mGoogleConnStatDecay) {
            this.mGMSAppStartPolicyList = DecisionMaker.getInstance().removeFeatureList(AppMngConstant.AppMngFeature.APP_START, this.mGmsAppPkg);
        }
    }

    public void removeAppCleanFeatureGMSList() {
        if (this.mIsInitGoogleConfig && !this.mIsAbroadArea && !this.mGoogleConnStatDecay) {
            this.mGMSAppCleanPolicyList = DecisionMaker.getInstance().removeFeatureList(AppMngConstant.AppMngFeature.APP_CLEAN, this.mGmsAppPkg);
        }
    }

    public void dumpGmsAppList(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpGmsAppList start");
            Iterator<String> it = this.mGmsAppPkg.iterator();
            while (it.hasNext()) {
                pw.println("gms app pkgName: " + it.next());
            }
            pw.println("google connection delay: " + this.mGoogleConnStatDecay);
            pw.println("google connection: " + this.mGoogleConnStat);
            pw.println("google disconn time: " + this.mGoogleDisConnTime);
            pw.println("google delay time: " + this.mGoogleConnDealyTime);
            pw.println("google config init: " + this.mIsInitGoogleConfig);
        }
    }

    /* access modifiers changed from: private */
    public void refreshAlivedApps(int eventType, String pkgAndTime, int userId) {
        if (pkgAndTime != null && !pkgAndTime.isEmpty()) {
            String[] splitRet = new String[2];
            int splitIndex = pkgAndTime.indexOf("#");
            try {
                splitRet[0] = pkgAndTime.substring(0, splitIndex);
                splitRet[1] = pkgAndTime.substring(splitIndex + 1);
            } catch (IndexOutOfBoundsException e) {
                AwareLog.i(TAG, "IndexOutOfBoundsException! pkgAndTime is: " + pkgAndTime);
            }
            if (splitRet[0] == null || splitRet[0].trim().isEmpty()) {
                AwareLog.i(TAG, "get pkg failed : " + pkgAndTime);
                return;
            }
            synchronized (this.mRegKeepAlivePkgs) {
                ArrayMap<String, Long> appMap = this.mRegKeepAlivePkgs.get(userId);
                if (eventType == 1) {
                    long elapsedRealtime = strToLong(splitRet[1]);
                    if (elapsedRealtime <= SystemClock.elapsedRealtime()) {
                        AwareLog.d(TAG, elapsedRealtime + " < " + SystemClock.elapsedRealtime());
                        return;
                    }
                    if (appMap == null) {
                        appMap = new ArrayMap<>();
                    }
                    AwareLog.d(TAG, "pkg: " + splitRet[0] + ", elapsedRealtime: " + elapsedRealtime);
                    appMap.put(splitRet[0], Long.valueOf(elapsedRealtime));
                    this.mRegKeepAlivePkgs.put(userId, appMap);
                } else if (eventType == 2) {
                    if (appMap != null) {
                        appMap.remove(splitRet[0]);
                    }
                    if (appMap == null || appMap.isEmpty()) {
                        this.mRegKeepAlivePkgs.remove(userId);
                    }
                }
            }
        }
    }

    private long strToLong(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            AwareLog.i(TAG, "number format exception! str is: " + str);
            return 0;
        }
    }

    public boolean isCurUserKeepALive(String pkg, int uid) {
        int userId;
        boolean ret;
        boolean ret2;
        if (pkg == null || pkg.isEmpty() || (userId = UserHandle.getUserId(uid)) != 0) {
            return false;
        }
        synchronized (this.mRegKeepAlivePkgs) {
            ArrayMap<String, Long> appMap = this.mRegKeepAlivePkgs.get(userId);
            if (appMap != null) {
                if (!appMap.isEmpty()) {
                    if (appMap.containsKey(pkg)) {
                        long keptTime = appMap.get(pkg).longValue() - SystemClock.elapsedRealtime();
                        AwareLog.d(TAG, pkg + " still need kept in iaware for " + keptTime + " ms");
                        if (keptTime <= 0) {
                            appMap.remove(pkg);
                            ret2 = false;
                        } else if (keptTime < DEFAULT_REQUEST_TIMEOUT) {
                            ret2 = true;
                        } else if (isKeptAliveAppByPg(pkg, uid)) {
                            ret2 = true;
                        } else {
                            appMap.remove(pkg);
                            ret2 = false;
                        }
                        ret = ret2;
                    } else {
                        ret = false;
                        AwareLog.d(TAG, pkg + " is not in iaware alive record.");
                    }
                    return ret;
                }
            }
            return false;
        }
    }

    private boolean isKeptAliveAppByPg(String pkg, int uid) {
        MultiTaskManagerService multiTaskManagerService;
        Context context;
        if (this.mPGSdk == null || (multiTaskManagerService = this.mMtmService) == null || (context = multiTaskManagerService.context()) == null) {
            return false;
        }
        try {
            return this.mPGSdk.isKeptAliveApp(context, pkg, uid);
        } catch (RemoteException e) {
            if (DEBUG) {
                AwareLog.w(TAG, "call isKeptAliveApp happened RemoteException.");
            }
            return false;
        }
    }

    public void dumpKeepAlivePkgs(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpKeepAlivePkgs start");
            StringBuffer sb = new StringBuffer();
            synchronized (this.mRegKeepAlivePkgs) {
                for (int i = this.mRegKeepAlivePkgs.size() - 1; i >= 0; i--) {
                    ArrayMap<String, Long> pkgMap = this.mRegKeepAlivePkgs.valueAt(i);
                    if (pkgMap != null) {
                        if (!pkgMap.isEmpty()) {
                            long current = SystemClock.elapsedRealtime();
                            sb.append("====userId:");
                            sb.append(this.mRegKeepAlivePkgs.keyAt(i));
                            sb.append(", cached size:");
                            sb.append(pkgMap.size());
                            sb.append(", elapsedRealtime:");
                            sb.append(current);
                            sb.append("====");
                            for (String pkg : pkgMap.keySet()) {
                                sb.append("\n{");
                                sb.append(pkg);
                                sb.append(" ");
                                sb.append(pkgMap.get(pkg));
                                sb.append(" ");
                                sb.append(pkgMap.get(pkg).longValue() - current);
                                sb.append("}");
                            }
                        }
                    }
                }
            }
            sb.append("\n");
            pw.println(sb.toString());
        }
    }

    private int calMemorySizeOfGiga(long deviceMemory) {
        if (deviceMemory == -1) {
            return -1;
        }
        if (deviceMemory % 1024 == 0) {
            return (int) (deviceMemory / 1024);
        }
        return (int) ((deviceMemory / 1024) + 1);
    }

    public int getMemorySize() {
        return this.mDeviceMemoryOfGIGA;
    }

    private void initCommonInfo() {
        initCommonActTopIM();
        initCommonAppLockClass();
    }

    private void initCommonActTopIM() {
        String actTopIMCN = DecisionMaker.getInstance().getCommonCfg(AppMngConstant.AppMngFeature.COMMON.getDesc(), ACT_TOP_IM_CN);
        if (actTopIMCN != null) {
            this.mActTopIMCN = actTopIMCN;
            if (!this.mActTopIMCN.equals(SystemProperties.get(TOP_IM_CN_PROP, UNKNOWN_PKG))) {
                SystemProperties.set(TOP_IM_CN_PROP, this.mActTopIMCN);
            }
        }
    }

    private void initCommonAppLockClass() {
        String appLock = DecisionMaker.getInstance().getCommonCfg(AppMngConstant.AppMngFeature.COMMON.getDesc(), APP_LOCK_CLASS);
        if (appLock != null) {
            this.mAppLockClass = appLock;
        }
    }

    public String getActTopIMCN() {
        if (mEnabled || this.mAppStartEnabled) {
            return this.mActTopIMCN;
        }
        return UNKNOWN_PKG;
    }

    private int getAppTypeActTop(String pkgName, int area) {
        int atti = AppTypeRecoManager.getInstance().getAppAttribute(pkgName);
        if (atti == -1) {
            return 0;
        }
        if (area == 0) {
            return (atti & ConstS32.HIGH_BITS_AL_MARK) >> 8;
        }
        if (area == 1) {
            return (61440 & atti) >> 12;
        }
        return 0;
    }

    public boolean isTopImAppBase(String pkg) {
        int actTop;
        if (!mEnabled || getAppMngSpecType(pkg) != 0 || (actTop = getAppTypeActTop(pkg, 1)) == 0) {
            return false;
        }
        if (actTop <= 3) {
            return true;
        }
        return false;
    }

    public boolean isBluetoothConnect(int pid) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mBluetoothLock) {
            int len = this.mBtoothConnectList.size();
            if (len == 0) {
                return false;
            }
            if (this.mBtoothConnectList.get(len - 1).intValue() == pid) {
                return true;
            }
            return false;
        }
    }

    public boolean isBluetoothLast(int pid, long time) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mBluetoothLock) {
            if (!this.mBtoothConnectList.isEmpty()) {
                return false;
            }
            if (this.mBtoothLastPid != -1) {
                if (this.mBtoothLastPid == pid) {
                    if (SystemClock.elapsedRealtime() - this.mBtoothLastTime > time) {
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public ArraySet<String> getDozeProtectedApps() {
        ArraySet<String> result = new ArraySet<>();
        synchronized (this.mDozeProtectPkg) {
            result.addAll((ArraySet<? extends String>) this.mDozeProtectPkg);
        }
        return result;
    }

    public boolean isRecogOptEnable() {
        return AppAccurateRecgFeature.isEnable();
    }

    public boolean checkBleStatus() {
        if (!mEnabled) {
            return true;
        }
        return mBleStatus;
    }

    private long getElapsedAppUseTime(String pkgName) {
        if ((!mEnabled && !this.mAppStartEnabled) || pkgName == null) {
            return -1;
        }
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit == null) {
            AwareLog.e(TAG, "AwareUserHabit is null");
            return -1;
        }
        long switchFgTime = habit.getAppSwitchFgTime(pkgName);
        if (switchFgTime == -1) {
            return -1;
        }
        long now = System.currentTimeMillis();
        if (switchFgTime > now || switchFgTime <= -1) {
            return -1;
        }
        return (now - switchFgTime) / 1000;
    }

    private boolean isAppUnusedRecent(String pkgName, int recent) {
        if (isAppMngSpecRecent(pkgName, 1, recent)) {
            return false;
        }
        long elapsedAppUseTime = getElapsedAppUseTime(pkgName);
        return elapsedAppUseTime == -1 || elapsedAppUseTime > ((long) recent);
    }

    public int getAppUnusedRecent(String pkgName, LinkedHashMap<Integer, RuleNode> recents) {
        if (recents == null || pkgName == null) {
            return -1;
        }
        for (Integer recent : recents.keySet()) {
            if (recent != null && recent.intValue() > 0 && isAppUnusedRecent(pkgName, recent.intValue())) {
                return recent.intValue();
            }
        }
        return -1;
    }

    /* access modifiers changed from: private */
    public void updateBleState() {
        mBleStatus = fetchSwitchStatus(HwArbitrationDEFS.KEY_BLUETOOTH_ON);
    }

    public boolean fetchSwitchStatus(String name) {
        Context context;
        String switchStatusStr;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService == null || (context = multiTaskManagerService.context()) == null || (switchStatusStr = Settings.Secure.getStringForUser(context.getContentResolver(), name, this.mCurUserId)) == null || switchStatusStr.equals("1")) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void sendBleStatusUpdate() {
        IntlRecgHandler intlRecgHandler = this.mHandler;
        if (intlRecgHandler != null) {
            Message msg = intlRecgHandler.obtainMessage();
            msg.what = 20;
            this.mHandler.removeMessages(20);
            this.mHandler.sendMessage(msg);
        }
    }

    public void dumpAppSceneInfo(PrintWriter pw, int type, String pkgName) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("AwareIntelligentRecg dumpAppSceneInfo start:");
            if (type == 1) {
                pw.println("pkgName: " + pkgName + ",getElapsedAppUseTime: " + getElapsedAppUseTime(pkgName));
            } else if (type == 2) {
                pw.println("china sim: " + isChinaOperrator());
            } else if (type == 3) {
                pw.println("ble swicth:" + checkBleStatus());
            }
        }
    }

    public boolean isChinaOperrator() {
        if (mEnabled && this.mIsChinaOperator && !this.mIsAbroadArea) {
            return true;
        }
        return false;
    }

    private void updateNetWorkOperatorInit(Context context) {
        Object obj = this.mMtmService.context().getSystemService("phone");
        if (obj != null && (obj instanceof TelephonyManager)) {
            TelephonyManager tm = (TelephonyManager) obj;
            String networkOperator = tm.getSimOperator();
            if (networkOperator != null && networkOperator.startsWith("460")) {
                this.mIsChinaOperator = true;
            }
            tm.listen(this.mPhoneStateListener, 1);
        }
    }

    public void reportAllowAppStartClean(AwareProcessBlockInfo procGroup) {
        List<AwareProcessInfo> processList;
        if (procGroup != null && (processList = procGroup.procProcessList) != null) {
            boolean needToUpdate = false;
            int size = processList.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                AwareProcessInfo awareProc = processList.get(i);
                if (awareProc != null && awareProc.procTaskId != -1) {
                    needToUpdate = true;
                    break;
                }
                i++;
            }
            if (needToUpdate) {
                updateAllowStartPkgs(UserHandle.getUserId(procGroup.procUid), procGroup.procPackageName, true);
            }
        }
    }

    public void reportAbnormalClean(AwareProcessBlockInfo procGroup) {
        if (procGroup != null) {
            updateAllowStartPkgs(UserHandle.getUserId(procGroup.procUid), procGroup.procPackageName, false);
        }
    }

    private void updateAllowStartPkgs(int userId, String pkg, boolean isAdd) {
        if (this.mAppStartEnabled && !this.mIsAppMngEnhance && pkg != null && !"".equals(pkg) && userId == 0) {
            long nowTime = SystemClock.elapsedRealtime() / 1000;
            synchronized (this.mAllowStartPkgs) {
                if (isAdd) {
                    this.mAllowStartPkgs.put(pkg, Long.valueOf(nowTime));
                } else {
                    removeAllowStartPkgIfNeedLock(pkg, nowTime);
                }
            }
        }
    }

    private void removeAllowStartPkgIfNeedLock(String pkg, long nowTime) {
        Long time = this.mAllowStartPkgs.get(pkg);
        if (time != null) {
            long delTime = nowTime - time.longValue();
            if (delTime > this.mAppMngAllowTime || delTime < 0) {
                this.mAllowStartPkgs.remove(pkg);
            }
        }
    }

    public boolean isAllowStartPkgs(String pkg) {
        boolean containsKey;
        if (!this.mAppStartEnabled || this.mIsAppMngEnhance || pkg == null || "".equals(pkg)) {
            return false;
        }
        synchronized (this.mAllowStartPkgs) {
            containsKey = this.mAllowStartPkgs.containsKey(pkg);
        }
        return containsKey;
    }

    public void dumpAllowStartPkgs(PrintWriter pw) {
        if (pw != null) {
            if (!this.mAppStartEnabled) {
                pw.println("AppStart feature disabled.");
                return;
            }
            pw.println("mIsAppMngEnhance:" + this.mIsAppMngEnhance);
            pw.println("mAppMngAllowTime:" + this.mAppMngAllowTime);
            pw.println("== DumpAllowStartPkgs Start ==");
            long nowTime = SystemClock.elapsedRealtime() / 1000;
            synchronized (this.mAllowStartPkgs) {
                for (Map.Entry<String, Long> entry : this.mAllowStartPkgs.entrySet()) {
                    if (entry != null) {
                        pw.println("  pkg : " + entry.getKey() + " time : " + (nowTime - entry.getValue().longValue()));
                    }
                }
            }
        }
    }

    public boolean isAppMngEnhance() {
        return this.mIsAppMngEnhance;
    }

    public boolean isAppLockClassName(String name) {
        if ((mEnabled || this.mAppStartEnabled) && name != null) {
            return name.equals(this.mAppLockClass);
        }
        return false;
    }

    private void initGmsControlInfo() {
        this.mIsGmsPhone = !TextUtils.isEmpty(SystemPropertiesEx.get("ro.com.google.gmsversion", (String) null));
        if (!this.mIsGmsPhone) {
            loadControlGmsApp();
            updateIsGmsCoreValid();
        }
    }

    private void loadControlGmsApp() {
        ArrayList<String> pkgList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "controlgmsapp");
        if (pkgList != null) {
            synchronized (this.mControlGmsApp) {
                this.mControlGmsApp.addAll(pkgList);
            }
        }
    }

    public boolean isGmsControlApp(String pkg) {
        if (!isGmsControlEnable() || pkg == null || this.mCurUserId != 0) {
            return false;
        }
        synchronized (this.mControlGmsApp) {
            if (!this.mControlGmsApp.contains(pkg)) {
                return false;
            }
            return !this.mIsGmsCoreValid;
        }
    }

    public void dumpGmsControlInfo(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareIntelligentRecg feature disabled.");
                return;
            }
            pw.println("[Is Gms Phone]");
            pw.println(this.mIsGmsPhone);
            pw.println("[Is Gms Core Valid]");
            pw.println(this.mIsGmsCoreValid);
            pw.println("[dump Control GMS App]");
            synchronized (this.mControlGmsApp) {
                Iterator<String> it = this.mControlGmsApp.iterator();
                while (it.hasNext()) {
                    pw.println(it.next());
                }
            }
        }
    }

    private boolean isGmsControlEnable() {
        return mEnabled && !this.mIsGmsPhone;
    }

    private void updateIsGmsCoreValid() {
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService != null && multiTaskManagerService.context() != null) {
            this.mIsGmsCoreValid = ValidGPackageHelper.isValidForG(this.mMtmService.context());
        }
    }

    private void updateIsGmsCoreValid(String pkg) {
        MultiTaskManagerService multiTaskManagerService;
        if (isGmsControlEnable() && pkg != null && (multiTaskManagerService = this.mMtmService) != null && multiTaskManagerService.context() != null && ValidGPackageHelper.GMS_CORE_PKGS != null) {
            for (String gmsCorePkg : ValidGPackageHelper.GMS_CORE_PKGS) {
                if (pkg.equals(gmsCorePkg)) {
                    this.mIsGmsCoreValid = ValidGPackageHelper.isValidForG(this.mMtmService.context());
                    return;
                }
            }
        }
    }
}
