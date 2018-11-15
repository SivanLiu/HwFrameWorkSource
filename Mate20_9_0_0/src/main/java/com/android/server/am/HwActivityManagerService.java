package com.android.server.am;

import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.ContentProviderHolder;
import android.app.HwRecentTaskInfo;
import android.app.IActivityController;
import android.app.IActivityController.Stub;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.ITaskStackListener;
import android.app.mtm.MultiTaskManager;
import android.app.mtm.iaware.appmng.AppCleanParam;
import android.app.mtm.iaware.appmng.AppCleanParam.AppCleanInfo;
import android.app.mtm.iaware.appmng.IAppCleanCallback;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.contentsensor.IActivityObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.rms.HwSysResManager;
import android.rms.HwSysResource;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.util.ArrayMap;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Log;
import android.util.Slog;
import android.util.TimingsTraceLog;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.SMCSAMSHelper;
import com.android.server.ServiceThread;
import com.android.server.am.ActivityManagerService.GrantUri;
import com.android.server.am.PendingIntentRecord.Key;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.input.HwInputManagerService.HwInputManagerServiceInternal;
import com.android.server.mtm.iaware.appmng.AwareProcessBaseInfo;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.auth.HwCertification;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.srms.BroadcastFeature;
import com.android.server.rms.iaware.srms.ResourceFeature;
import com.android.server.rms.iaware.srms.SRMSDumpRadar;
import com.android.server.security.hsm.HwSystemManagerPlugin;
import com.android.server.security.securityprofile.ISecurityProfileController;
import com.android.server.security.trustspace.TrustSpaceManagerInternal;
import com.android.server.util.AbsUserBehaviourRecord;
import com.android.server.util.HwUserBehaviourRecord;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.HwWindowManagerService;
import com.huawei.android.app.IGameObserver;
import com.huawei.android.pushagentproxy.PushService;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public final class HwActivityManagerService extends ActivityManagerService {
    public static final int BACKUP_APP_ADJ = 300;
    private static final int CACHED_PROCESS_LIMIT = 8;
    static final boolean DEBUG_HWTRIM = smcsLOGV;
    static final boolean DEBUG_HWTRIM_PERFORM = smcsLOGV;
    public static final int FOREGROUND_APP_ADJ = 0;
    public static final int HEAVY_WEIGHT_APP_ADJ = 400;
    public static final int HOME_APP_ADJ = 600;
    private static final String HW_TRIM_MEMORY_ACTION = "huawei.intent.action.HW_TRIM_MEMORY_ACTION";
    public static final boolean IS_SUPPORT_CLONE_APP = SystemProperties.getBoolean("ro.config.hw_support_clone_app", false);
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", MemoryConstant.MEM_SCENE_DEFAULT));
    private static final int MIN_CLEAN_PKG = 5;
    public static final int NATIVE_ADJ = -1000;
    public static final int PERCEPTIBLE_APP_ADJ = 200;
    private static final int PERSISTENT_MASK = 9;
    public static final int PERSISTENT_PROC_ADJ = -800;
    public static final int PERSISTENT_SERVICE_ADJ = -700;
    public static final int PREVIOUS_APP_ADJ = 700;
    private static final int QUEUE_NUM_DEFAULT = 2;
    private static final int QUEUE_NUM_IAWARE = 6;
    private static final int QUEUE_NUM_RMS = 4;
    public static final int SERVICE_ADJ = 500;
    public static final int SERVICE_B_ADJ = 800;
    static final int SET_CUSTOM_ACTIVITY_CONTROLLER_TRANSACTION = 2101;
    private static final int SMART_TRIM_ADJ_LIMIT = SystemProperties.getInt("ro.smart_trim.adj", 3);
    private static final int SMART_TRIM_POST_MSG_DELAY = 10;
    private static final int START_HW_SERVICE_POST_MSG_DELAY = 30000;
    public static final int SYSTEM_ADJ = -900;
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    static final String TAG = "HwActivityManagerService";
    public static final int VISIBLE_APP_ADJ = 100;
    private static final String descriptor = "android.app.IActivityManager";
    private static boolean enableIaware = SystemProperties.getBoolean("persist.sys.enable_iaware", false);
    static final boolean enableRms = SystemProperties.getBoolean("ro.config.enable_rms", false);
    private static final boolean mIsSMCSHWSYSMEnabled = SystemProperties.getBoolean("ro.enable.hwsysm_smcs", true);
    private static HwActivityManagerService mSelf;
    private static Set<String> sAllowedCrossUserForCloneArrays = new HashSet();
    private static HashMap<String, Integer> sHardCodeAppToSetOomAdjArrays = new HashMap();
    private static Set<String> sPreventStartWhenSleeping = new HashSet();
    static final boolean smcsLOGV = SystemProperties.getBoolean("ro.enable.st_debug", false);
    final RemoteCallbackList<IActivityObserver> mActivityObservers = new RemoteCallbackList();
    private HwSysResource mAppResource;
    public HwSysResource mAppServiceResource;
    private HwSysResource mBroadcastResource;
    private HashMap<Integer, Intent> mCurrentSplitIntent = new HashMap();
    private AbsUserBehaviourRecord mCust;
    IActivityController mCustomController = null;
    ActivityRecord mFocusedActivityForNavi = null;
    Handler mHandler = new Handler();
    final HwGameAssistantController mHwGameAssistantController;
    private Intent mLastSplitIntent;
    private HwSysResource mOrderedBroadcastResource;
    OverscanTimeout mOverscanTimeout = new OverscanTimeout();
    public HashMap<String, Integer> mPkgDisplayMaps = new HashMap();
    private boolean[] mScreenStatusRequest = new boolean[]{false, false};
    private HashMap<Integer, Stack<IBinder>> mSplitActivityEntryStack;
    private Bundle mSplitExtras;
    private int mSrvFlagLocked = 0;
    private TrustSpaceManagerInternal mTrustSpaceManagerInternal;

    class OverscanTimeout implements Runnable {
        OverscanTimeout() {
        }

        public void run() {
            Slog.i(HwActivityManagerService.TAG, "OverscanTimeout run");
            Global.putString(HwActivityManagerService.this.mContext.getContentResolver(), "single_hand_mode", "");
        }
    }

    class ScreenStatusReceiver extends BroadcastReceiver {
        ScreenStatusReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.stk.check_screen_idle".equals(intent.getAction())) {
                int slotId = intent.getIntExtra("slot_id", 0);
                String str;
                StringBuilder stringBuilder;
                if (slotId < 0 || slotId >= HwActivityManagerService.this.mScreenStatusRequest.length) {
                    str = HwActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ScreenStatusReceiver, slotId ");
                    stringBuilder.append(slotId);
                    stringBuilder.append(" Invalid");
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
                HwActivityManagerService.this.mScreenStatusRequest[slotId] = intent.getBooleanExtra("SCREEN_STATUS_REQUEST", false);
                if (HwActivityManagerService.this.mScreenStatusRequest[slotId]) {
                    ActivityRecord p = HwActivityManagerService.this.getFocusedStack().topRunningActivityLocked();
                    if (p != null) {
                        Intent StkIntent = new Intent("com.huawei.intent.action.stk.idle_screen");
                        if (p.intent.hasCategory("android.intent.category.HOME")) {
                            StkIntent.putExtra("SCREEN_IDLE", true);
                        } else {
                            StkIntent.putExtra("SCREEN_IDLE", false);
                        }
                        StkIntent.putExtra("slot_id", slotId);
                        StkIntent.addFlags(16777216);
                        HwActivityManagerService.this.mContext.sendBroadcast(StkIntent, "com.huawei.permission.CAT_IDLE_SCREEN");
                        if (ActivityManagerDebugConfig.DEBUG_ALL) {
                            str = HwActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Broadcasting Home Idle Screen Intent ... slot: ");
                            stringBuilder2.append(slotId);
                            Slog.v(str, stringBuilder2.toString());
                        }
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_ALL) {
                    str = HwActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Screen Status request is OFF, slot: ");
                    stringBuilder.append(slotId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }
    }

    class TrimMemoryReceiver extends BroadcastReceiver {
        TrimMemoryReceiver() {
        }

        /* JADX WARNING: Missing block: B:7:0x001d, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            if (!(intent == null || intent.getAction() == null || !HwActivityManagerService.HW_TRIM_MEMORY_ACTION.equals(intent.getAction()))) {
                HwActivityManagerService.this.trimGLMemory(80);
            }
        }
    }

    static {
        sHardCodeAppToSetOomAdjArrays.put("com.huawei.android.pushagent.PushService", Integer.valueOf(200));
        sHardCodeAppToSetOomAdjArrays.put("com.tencent.mm", Integer.valueOf(800));
        sAllowedCrossUserForCloneArrays.add(HwCertification.SIGNATURE_MEDIA);
        sAllowedCrossUserForCloneArrays.add("com.android.providers.media.documents");
        sAllowedCrossUserForCloneArrays.add("com.huawei.android.launcher.settings");
        sAllowedCrossUserForCloneArrays.add("com.android.badge");
        sAllowedCrossUserForCloneArrays.add("com.android.providers.media");
        sAllowedCrossUserForCloneArrays.add("android.media.IMediaScannerService");
        sAllowedCrossUserForCloneArrays.add("com.android.contacts.files");
        sAllowedCrossUserForCloneArrays.add("com.android.contacts.app");
        sAllowedCrossUserForCloneArrays.add("com.huawei.numberlocation");
        sAllowedCrossUserForCloneArrays.add("csp-prefs-cfg");
        sAllowedCrossUserForCloneArrays.add("contacts");
        sAllowedCrossUserForCloneArrays.add("com.android.contacts");
        sAllowedCrossUserForCloneArrays.add("android.process.media");
        sAllowedCrossUserForCloneArrays.add(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE);
        sAllowedCrossUserForCloneArrays.add("android.process.acore");
        sAllowedCrossUserForCloneArrays.add("call_log");
        sAllowedCrossUserForCloneArrays.add("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        sAllowedCrossUserForCloneArrays.add("android.intent.action.MEDIA_SCANNER_SCAN_FOLDER");
        sAllowedCrossUserForCloneArrays.add("com.android.launcher.action.INSTALL_SHORTCUT");
        sAllowedCrossUserForCloneArrays.add("mms");
        sAllowedCrossUserForCloneArrays.add("sms");
        sAllowedCrossUserForCloneArrays.add("mms-sms");
        sAllowedCrossUserForCloneArrays.add("com.android.providers.downloads");
        sAllowedCrossUserForCloneArrays.add("downloads");
        sAllowedCrossUserForCloneArrays.add("com.android.providers.downloads.documents");
        sPreventStartWhenSleeping.add("com.ss.android.article.news/com.ss.android.message.sswo.SswoActivity");
        sPreventStartWhenSleeping.add("com.ss.android.article.video/com.ss.android.message.sswo.SswoActivity");
        sPreventStartWhenSleeping.add("dongzheng.szkingdom.android.phone/com.dgzq.IM.ui.activity.KeepAliveActivity");
    }

    public HwActivityManagerService(Context mContext) {
        super(mContext);
        mSelf = this;
        if (SystemProperties.getInt("ro.config.gameassist", 0) == 1) {
            this.mHwGameAssistantController = new HwGameAssistantController(this);
        } else {
            this.mHwGameAssistantController = null;
        }
    }

    public static HwActivityManagerService self() {
        return mSelf;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        boolean res;
        boolean _result;
        if (code == 502) {
            data.enforceInterface(descriptor);
            res = handleANRFilterFIFO(data.readInt(), data.readInt());
            reply.writeNoException();
            reply.writeInt(res);
            return true;
        } else if (code == 504) {
            data.enforceInterface(descriptor);
            String packageName = getPackageNameForPid(data.readInt());
            reply.writeNoException();
            reply.writeString(packageName);
            return true;
        } else if (code == SET_CUSTOM_ACTIVITY_CONTROLLER_TRANSACTION) {
            data.enforceInterface(descriptor);
            setCustomActivityController(Stub.asInterface(data.readStrongBinder()));
            reply.writeNoException();
            return true;
        } else if (code != 3103) {
            boolean forLast = false;
            if (code != 1599294787) {
                Bundle _arg2 = null;
                switch (code) {
                    case WifiProCommonUtils.RESP_CODE_UNSTABLE /*601*/:
                        data.enforceInterface(descriptor);
                        Intent intent = (Intent) data.readParcelable(null);
                        int pid = data.readInt();
                        Bundle bundle = data.readBundle();
                        if (data.readInt() > 0) {
                            forLast = true;
                        }
                        setIntentInfo(intent, pid, bundle, forLast);
                        reply.writeNoException();
                        return true;
                    case WifiProCommonUtils.RESP_CODE_GATEWAY /*602*/:
                        data.enforceInterface(descriptor);
                        Parcelable[] p = getIntentInfo(data.readInt(), data.readInt() > 0);
                        reply.writeNoException();
                        reply.writeParcelableArray(p, 0);
                        return true;
                    case WifiProCommonUtils.RESP_CODE_INVALID_URL /*603*/:
                        data.enforceInterface(descriptor);
                        addToEntryStack(data.readInt(), data.readStrongBinder(), data.readInt(), (Intent) Intent.CREATOR.createFromParcel(data));
                        reply.writeNoException();
                        return true;
                    case WifiProCommonUtils.RESP_CODE_ABNORMAL_SERVER /*604*/:
                        data.enforceInterface(descriptor);
                        clearEntryStack(data.readInt(), data.readStrongBinder());
                        return true;
                    case WifiProCommonUtils.RESP_CODE_REDIRECTED_HOST_CHANGED /*605*/:
                        data.enforceInterface(descriptor);
                        removeFromEntryStack(data.readInt(), data.readStrongBinder());
                        return true;
                    case WifiProCommonUtils.RESP_CODE_CONN_RESET /*606*/:
                        data.enforceInterface(descriptor);
                        res = isTopSplitActivity(data.readInt(), data.readStrongBinder());
                        reply.writeNoException();
                        reply.writeInt(res);
                        return true;
                    default:
                        switch (code) {
                            case 3201:
                                data.enforceInterface(descriptor);
                                registerActivityObserver(IActivityObserver.Stub.asInterface(data.readStrongBinder()));
                                reply.writeNoException();
                                return true;
                            case 3202:
                                data.enforceInterface(descriptor);
                                unregisterActivityObserver(IActivityObserver.Stub.asInterface(data.readStrongBinder()));
                                reply.writeNoException();
                                return true;
                            default:
                                ComponentName _arg0;
                                boolean _result2;
                                switch (code) {
                                    case 3211:
                                        data.enforceInterface(descriptor);
                                        if (data.readInt() != 0) {
                                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                                        } else {
                                            _arg0 = null;
                                        }
                                        if (data.readInt() != 0) {
                                            _arg2 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                                        }
                                        _result2 = requestContentNode(_arg0, _arg2, data.readInt());
                                        reply.writeNoException();
                                        reply.writeInt(_result2);
                                        return true;
                                    case 3212:
                                        data.enforceInterface(descriptor);
                                        if (data.readInt() != 0) {
                                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                                        } else {
                                            _arg0 = null;
                                        }
                                        if (data.readInt() != 0) {
                                            _arg2 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                                        }
                                        _result2 = requestContentOther(_arg0, _arg2, data.readInt());
                                        reply.writeNoException();
                                        reply.writeInt(_result2);
                                        return true;
                                    default:
                                        List<String> addList;
                                        switch (code) {
                                            case 3301:
                                                addList = new ArrayList();
                                                data.enforceInterface(descriptor);
                                                data.readStringList(addList);
                                                forLast = addGameSpacePackageList(addList);
                                                reply.writeNoException();
                                                reply.writeInt(forLast);
                                                return true;
                                            case 3302:
                                                addList = new ArrayList();
                                                data.enforceInterface(descriptor);
                                                data.readStringList(addList);
                                                forLast = delGameSpacePackageList(addList);
                                                reply.writeNoException();
                                                reply.writeInt(forLast);
                                                return true;
                                            case 3303:
                                                data.enforceInterface(descriptor);
                                                _result = isInGameSpace(data.readString());
                                                reply.writeNoException();
                                                reply.writeInt(_result);
                                                return true;
                                            case 3304:
                                                data.enforceInterface(descriptor);
                                                addList = getGameList();
                                                reply.writeNoException();
                                                reply.writeStringList(addList);
                                                return true;
                                            case 3305:
                                                data.enforceInterface(descriptor);
                                                registerGameObserver(IGameObserver.Stub.asInterface(data.readStrongBinder()));
                                                reply.writeNoException();
                                                return true;
                                            case 3306:
                                                data.enforceInterface(descriptor);
                                                unregisterGameObserver(IGameObserver.Stub.asInterface(data.readStrongBinder()));
                                                reply.writeNoException();
                                                return true;
                                            case 3307:
                                                data.enforceInterface(descriptor);
                                                _result = isGameDndOn();
                                                reply.writeNoException();
                                                reply.writeInt(_result);
                                                return true;
                                            case 3308:
                                                data.enforceInterface(descriptor);
                                                _result = isGameKeyControlOn();
                                                reply.writeNoException();
                                                reply.writeInt(_result);
                                                return true;
                                            case 3309:
                                                data.enforceInterface(descriptor);
                                                _result = isGameGestureDisabled();
                                                reply.writeNoException();
                                                reply.writeInt(_result);
                                                return true;
                                        }
                                        break;
                                }
                        }
                }
            }
            if (DEBUG_HWTRIM) {
                Log.v(TAG, "AMS.onTransact: got HWMEMCLEAN_TRANSACTION");
            }
            if (mIsSMCSHWSYSMEnabled) {
                if (this.mContext == null) {
                    return false;
                }
                if (this.mContext.checkCallingPermission("huawei.permission.HSM_SMCS") != 0) {
                    if (DEBUG_HWTRIM) {
                        Log.e(TAG, "SMCSAMSHelper.handleTransact permission deny");
                    }
                    return false;
                } else if (SMCSAMSHelper.getInstance().handleTransact(data, reply, flags)) {
                    return true;
                }
            } else if (DEBUG_HWTRIM) {
                Log.v(TAG, "AMS.onTransact: HWSysM SMCS is disabled.");
            }
            return super.onTransact(code, data, reply, flags);
        } else {
            data.enforceInterface(descriptor);
            _result = isInMultiWindowMode();
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }
    }

    public void addCallerToIntent(Intent intent, IApplicationThread caller) {
        String callerPackage = null;
        if (caller != null) {
            ProcessRecord callerApp = getRecordForAppLocked(caller);
            if (callerApp != null) {
                callerPackage = callerApp.info.packageName;
            }
        }
        if (callerPackage != null) {
            String CALLER_PACKAGE = "caller_package";
            try {
                if (isInstall(intent)) {
                    String callerIndex = intent.getStringExtra("caller_package");
                    if (callerIndex != null) {
                        callerPackage = callerIndex;
                    }
                    intent.putExtra("caller_package", callerPackage);
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Get package info faild:");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private boolean isInstall(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        boolean story = false;
        if ("android.intent.action.INSTALL_PACKAGE".equals(action)) {
            story = true;
        }
        if ("application/vnd.android.package-archive".equals(type)) {
            return true;
        }
        return story;
    }

    private final boolean isOomAdjCustomized(ProcessRecord app) {
        if (sHardCodeAppToSetOomAdjArrays.containsKey(app.info.packageName) || sHardCodeAppToSetOomAdjArrays.containsKey(app.processName)) {
            return true;
        }
        return false;
    }

    private int retrieveCustedMaxAdj(String processName) {
        int rc = -901;
        if (sHardCodeAppToSetOomAdjArrays.containsKey(processName)) {
            rc = ((Integer) sHardCodeAppToSetOomAdjArrays.get(processName)).intValue();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("retrieveCustedMaxAdj for processName:");
        stringBuilder.append(processName);
        stringBuilder.append(", get adj:");
        stringBuilder.append(rc);
        Slog.i(str, stringBuilder.toString());
        return rc;
    }

    protected final boolean computeOomAdjLocked(ProcessRecord app, int cachedAdj, ProcessRecord TOP_APP, boolean doingAll, long now) {
        if (this.mAdjSeq != app.adjSeq) {
            return super.computeOomAdjLocked(app, cachedAdj, TOP_APP, doingAll, now);
        }
        boolean computeOomAdj = super.computeOomAdjLocked(app, cachedAdj, TOP_APP, doingAll, now);
        if (app.curAdj > app.maxAdj && isOomAdjCustomized(app)) {
            app.curAdj = app.maxAdj;
        }
        return computeOomAdj;
    }

    protected final boolean startProcessLocked(ProcessRecord app, String hostingType, String hostingNameStr, String abiOverride) {
        StringBuilder stringBuilder;
        if (HwPCUtils.isPcCastModeInServer()) {
            if (this.mPkgDisplayMaps.containsKey(app.info.packageName)) {
                int displayId = ((Integer) this.mPkgDisplayMaps.get(app.info.packageName)).intValue();
                if (HwPCUtils.isValidExtDisplayId(displayId)) {
                    app.mDisplayId = displayId;
                    if (app.entryPointArgs == null) {
                        app.entryPointArgs = new String[]{String.valueOf(displayId)};
                    }
                }
            }
            if (HwPCUtils.enabledInPad()) {
                List<InputMethodInfo> methodList = HwPCUtils.getInputMethodList();
                if (methodList != null) {
                    int listSize = methodList.size();
                    String pkgName = null;
                    for (int i = 0; listSize > i; i++) {
                        InputMethodInfo mi = (InputMethodInfo) methodList.get(i);
                        if (mi != null) {
                            pkgName = mi.getPackageName();
                        }
                        if (pkgName != null && pkgName.equals(app.info.packageName)) {
                            app.entryPointArgs = new String[]{String.valueOf(HwPCUtils.getPCDisplayID())};
                            break;
                        }
                    }
                }
            }
        }
        if (HwVRUtils.isVRMode()) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("hwAMS startProcessLocked is VR Display is ");
            stringBuilder.append(HwVRUtils.getVRDisplayID());
            Slog.e(str, stringBuilder.toString());
            app.entryPointArgs = new String[]{String.valueOf(HwVRUtils.getVRDisplayID()), String.valueOf(2880), String.valueOf(1600)};
        }
        boolean result = super.startProcessLocked(app, hostingType, hostingNameStr, abiOverride);
        if (isOomAdjCustomized(app)) {
            int custMaxAdj = retrieveCustedMaxAdj(app.processName);
            if (app.maxAdj > PERSISTENT_PROC_ADJ && custMaxAdj >= SYSTEM_ADJ && custMaxAdj <= 906) {
                app.maxAdj = custMaxAdj;
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("addAppLocked, app:");
                stringBuilder.append(app);
                stringBuilder.append(", set maxadj to ");
                stringBuilder.append(custMaxAdj);
                Slog.i(str2, stringBuilder.toString());
            }
        }
        return result;
    }

    protected void startPushService() {
        File jarFile = new File("/system/framework/hwpush.jar");
        File custFile = HwCfgFilePolicy.getCfgFile("jars/hwpush.jar", 0);
        if (jarFile.exists() || (custFile != null && custFile.exists())) {
            Slog.d(TAG, "start push service");
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    Intent serviceIntent = new Intent(HwActivityManagerService.this.mContext, PushService.class);
                    serviceIntent.putExtra("startFlag", "1");
                    HwActivityManagerService.this.mContext.startService(serviceIntent);
                }
            }, HwArbitrationDEFS.DelayTimeMillisA);
        }
    }

    public Configuration getCurNaviConfiguration() {
        return this.mWindowManager.getCurNaviConfiguration();
    }

    protected void setFocusedActivityLockedForNavi(ActivityRecord r) {
        if (this.mFocusedActivityForNavi != r) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setFocusedActivityLockedForNavi: r=");
                stringBuilder.append(r);
                Slog.d(str, stringBuilder.toString());
            }
            this.mFocusedActivityForNavi = r;
            if (r != null) {
                this.mWindowManager.setFocusedAppForNavi(r.appToken);
            }
        }
    }

    public String topAppName() {
        ActivityStack focusedStack;
        synchronized (this) {
            focusedStack = getFocusedStack();
        }
        ActivityRecord r = focusedStack.topRunningActivityLocked();
        if (r != null) {
            return r.shortComponentName;
        }
        return null;
    }

    public boolean serviceIsRunning(ComponentName serviceCmpName, int curUser) {
        boolean z;
        synchronized (this) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("serviceIsRunning, for user ");
            stringBuilder.append(curUser);
            stringBuilder.append(", serviceCmpName ");
            stringBuilder.append(serviceCmpName);
            Slog.d(str, stringBuilder.toString());
            z = this.mServices.getServicesLocked(curUser).get(serviceCmpName) != null;
        }
        return z;
    }

    void setDeviceProvisioned() {
        ContentResolver cr = this.mContext.getContentResolver();
        if ((Global.getInt(cr, "device_provisioned", 0) == 0 || Secure.getInt(cr, "user_setup_complete", 0) == 0) && ((PackageManagerService) ServiceManager.getService("package")).isSetupDisabled()) {
            Global.putInt(cr, "device_provisioned", 1);
            Secure.putInt(cr, "user_setup_complete", 1);
        }
    }

    public void systemReady(Runnable goingCallback, TimingsTraceLog traceLog) {
        if (!this.mSystemReady) {
            setDeviceProvisioned();
        }
        super.systemReady(goingCallback, traceLog);
        initTrustSpace();
        this.mContext.registerReceiver(new ScreenStatusReceiver(), new IntentFilter("android.intent.action.stk.check_screen_idle"), "com.huawei.permission.STK_CHECK_SCREEN_IDLE", null);
        this.mContext.registerReceiver(new TrimMemoryReceiver(), new IntentFilter(HW_TRIM_MEMORY_ACTION));
    }

    public ArrayList<Integer> getIawareDumpData() {
        ArrayList<Integer> queueSizes = new ArrayList();
        for (BroadcastQueue queue : this.mBroadcastQueues) {
            ArrayList<Integer> queueSizesTemp = queue.getIawareDumpData();
            if (queueSizesTemp != null) {
                queueSizes.addAll(queueSizesTemp);
            }
        }
        return queueSizes;
    }

    public void updateSRMSStatisticsData(int subTypeCode) {
        SRMSDumpRadar.getInstance().updateStatisticsData(subTypeCode);
    }

    public boolean getIawareResourceFeature(int type) {
        if (type >= 1 && type <= 2) {
            return ResourceFeature.getIawareResourceFeature(type);
        }
        if (type < 10 || type > 11) {
            return false;
        }
        return BroadcastFeature.isFeatureEnabled(type);
    }

    public long proxyBroadcast(List<String> pkgs, boolean proxy) {
        long delay;
        synchronized (this) {
            delay = 0;
            for (BroadcastQueue queue : this.mBroadcastQueues) {
                long temp = queue.proxyBroadcast(pkgs, proxy);
                delay = temp > delay ? temp : delay;
            }
        }
        return delay;
    }

    public long proxyBroadcastByPid(List<Integer> pids, boolean proxy) {
        long delay;
        synchronized (this) {
            delay = 0;
            for (BroadcastQueue queue : this.mBroadcastQueues) {
                long temp = queue.proxyBroadcastByPid(pids, proxy);
                delay = temp > delay ? temp : delay;
            }
        }
        return delay;
    }

    public void setProxyBCActions(List<String> actions) {
        synchronized (this) {
            for (BroadcastQueue queue : this.mBroadcastQueues) {
                queue.setProxyBCActions(actions);
            }
        }
    }

    public void setActionExcludePkg(String action, String pkg) {
        synchronized (this) {
            for (BroadcastQueue queue : this.mBroadcastQueues) {
                queue.setActionExcludePkg(action, pkg);
            }
        }
    }

    public void proxyBCConfig(int type, String key, List<String> value) {
        synchronized (this) {
            for (BroadcastQueue queue : this.mBroadcastQueues) {
                queue.proxyBCConfig(type, key, value);
            }
        }
    }

    public void checkIfScreenStatusRequestAndSendBroadcast() {
        for (int slotId = 0; slotId < this.mScreenStatusRequest.length; slotId++) {
            if (this.mScreenStatusRequest[slotId]) {
                Intent StkIntent = new Intent("com.huawei.intent.action.stk.idle_screen");
                StkIntent.addFlags(16777216);
                StkIntent.putExtra("SCREEN_IDLE", true);
                StkIntent.putExtra("slot_id", slotId);
                this.mContext.sendBroadcast(StkIntent, "com.huawei.permission.CAT_IDLE_SCREEN");
            }
        }
    }

    public void setServiceFlagLocked(int servFlag) {
        this.mSrvFlagLocked = servFlag;
    }

    public boolean isProcessExistLocked(String processName, int uid) {
        return getProcessRecordLocked(processName, uid, true) != null;
    }

    public ProcessRecord getProcessRecordLocked(int pid) {
        ProcessRecord proc = null;
        if (pid >= 0) {
            synchronized (this.mPidsSelfLocked) {
                proc = (ProcessRecord) this.mPidsSelfLocked.get(pid);
            }
        }
        return proc;
    }

    public boolean shouldPreventSendReceiver(Intent intent, ResolveInfo resolveInfo, int callerPid, int callerUid, ProcessRecord targetApp, ProcessRecord callerApp) {
        AwareAppStartupPolicy appStartupPolicy = AwareAppStartupPolicy.self();
        if (appStartupPolicy != null) {
            return appStartupPolicy.shouldPreventSendReceiver(intent, resolveInfo, callerPid, callerUid, targetApp, callerApp);
        }
        return false;
    }

    public boolean shouldPreventStartService(ServiceInfo servInfo, int callerPid, int callerUid, ProcessRecord callerApp, boolean servExist, Intent service) {
        AwareAppStartupPolicy appStartupPolicy = AwareAppStartupPolicy.self();
        if (appStartupPolicy == null) {
            return false;
        }
        return appStartupPolicy.shouldPreventStartService(servInfo, callerPid, callerUid, callerApp, this.mSrvFlagLocked, servExist, service);
    }

    public boolean shouldPreventActivity(Intent intent, ActivityInfo aInfo, ActivityRecord record, int callerPid, int callerUid, ProcessRecord callerApp) {
        if (intent == null || aInfo == null || record == null) {
            return false;
        }
        if (isSleepingLocked() && sPreventStartWhenSleeping.contains(record.shortComponentName)) {
            return true;
        }
        AwareAppStartupPolicy appStartupPolicy = AwareAppStartupPolicy.self();
        if (appStartupPolicy == null) {
            return false;
        }
        boolean shouldPrevent = appStartupPolicy.shouldPreventStartActivity(intent, aInfo, record, callerPid, callerUid, callerApp);
        if (!shouldPrevent) {
            shouldPrevent = AwareFakeActivityRecg.self().shouldPreventStartActivity(aInfo, callerPid, callerUid, this.mBatteryStatsService.getActiveStatistics().isScreenOn());
        }
        return shouldPrevent;
    }

    public boolean shouldPreventRestartService(ServiceInfo sInfo, boolean realStart) {
        AwareAppStartupPolicy appStartupPolicy = AwareAppStartupPolicy.self();
        if (appStartupPolicy != null) {
            return appStartupPolicy.shouldPreventRestartService(sInfo, realStart);
        }
        return false;
    }

    private void initTrustSpace() {
        this.mTrustSpaceManagerInternal = (TrustSpaceManagerInternal) LocalServices.getService(TrustSpaceManagerInternal.class);
        if (this.mTrustSpaceManagerInternal == null) {
            Slog.e(TAG, "TrustSpaceManagerInternal not find !");
        } else {
            this.mTrustSpaceManagerInternal.initTrustSpace();
        }
    }

    private boolean shouldPreventStartComponent(int type, String calleePackage, int callerUid, int callerPid, String callerPackage, int userId) {
        boolean shouldPrevent = false;
        if (!this.mSystemReady) {
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mTrustSpaceManagerInternal != null) {
                shouldPrevent = this.mTrustSpaceManagerInternal.checkIntent(type, calleePackage, callerUid, callerPid, callerPackage, userId);
            }
            ISecurityProfileController spc = HwServiceFactory.getSecurityProfileController();
            if (spc != null) {
                shouldPrevent |= spc.shouldPreventInteraction(type, calleePackage, callerUid, callerPid, callerPackage, userId);
            }
            shouldPrevent |= HwSystemManagerPlugin.getInstance(this.mContext).shouldPreventStartComponent(type, calleePackage, callerUid, callerPid, callerPackage, userId);
            return shouldPrevent;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean shouldPreventStartService(ServiceInfo sInfo, int callerUid, int callerPid, String callerPackage, int userId) {
        if (sInfo == null) {
            return false;
        }
        return shouldPreventStartComponent(2, sInfo.applicationInfo.packageName, callerUid, callerPid, callerPackage, userId);
    }

    public boolean shouldPreventStartActivity(ActivityInfo aInfo, int callerUid, int callerPid, String callerPackage, int userId) {
        if (aInfo == null) {
            return false;
        }
        return shouldPreventStartComponent(0, aInfo.applicationInfo.packageName, callerUid, callerPid, callerPackage, userId);
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

    protected void exitSingleHandMode() {
        this.mHandler.removeCallbacks(this.mOverscanTimeout);
        this.mHandler.postDelayed(this.mOverscanTimeout, 200);
    }

    public boolean shouldPreventStartProvider(ProviderInfo cpi, int callerPid, int callerUid, ProcessRecord callerApp) {
        AwareAppStartupPolicy appStartupPolicy = AwareAppStartupPolicy.self();
        if (appStartupPolicy != null) {
            return appStartupPolicy.shouldPreventStartProvider(cpi, callerPid, callerUid, callerApp);
        }
        return false;
    }

    protected void setCustomActivityController(IActivityController controller) {
        enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "setCustomActivityController()");
        synchronized (this) {
            this.mCustomController = controller;
        }
        HwInputManagerServiceInternal inputManager = (HwInputManagerServiceInternal) LocalServices.getService(HwInputManagerServiceInternal.class);
        if (inputManager != null) {
            inputManager.setCustomActivityController(controller);
        }
    }

    public void setRequestedOrientation(IBinder token, int requestedOrientation) {
        super.setRequestedOrientation(token, requestedOrientation);
        if (this.mWindowManager.getLazyMode() != 0) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this) {
                    if (requestedOrientation == 0 || requestedOrientation == 6 || requestedOrientation == 8 || requestedOrientation == 11) {
                        Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
                    }
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    protected boolean customActivityStarting(Intent intent, String packageName) {
        if (this.mCustomController != null) {
            boolean startOK = true;
            try {
                startOK = this.mCustomController.activityStarting(intent.cloneFilter(), packageName);
            } catch (RemoteException e) {
                this.mCustomController = null;
                HwInputManagerServiceInternal inputManager = (HwInputManagerServiceInternal) LocalServices.getService(HwInputManagerServiceInternal.class);
                if (inputManager != null) {
                    inputManager.setCustomActivityController(null);
                }
            }
            if (!startOK) {
                Slog.i(TAG, "Not starting activity because custom controller stop it");
                return true;
            }
        }
        return false;
    }

    protected boolean customActivityResuming(String packageName) {
        ISecurityProfileController spc = HwServiceFactory.getSecurityProfileController();
        if (spc != null) {
            spc.handleActivityResuming(packageName);
        }
        if (this.mCustomController != null) {
            boolean resumeOK = true;
            try {
                resumeOK = this.mCustomController.activityResuming(packageName);
            } catch (RemoteException e) {
                this.mCustomController = null;
                HwInputManagerServiceInternal inputManager = (HwInputManagerServiceInternal) LocalServices.getService(HwInputManagerServiceInternal.class);
                if (inputManager != null) {
                    inputManager.setCustomActivityController(null);
                }
            }
            if (!resumeOK) {
                Slog.i(TAG, "Not resuming activity because custom controller stop it");
                return true;
            }
        }
        return false;
    }

    protected BroadcastQueue[] initialBroadcastQueue() {
        int queueNum;
        if (enableIaware) {
            queueNum = 6;
        } else if (enableRms) {
            queueNum = 4;
        } else {
            queueNum = 2;
        }
        return new BroadcastQueue[queueNum];
    }

    protected void setThirdPartyAppBroadcastQueue(BroadcastQueue[] broadcastQueues) {
        if (enableRms || enableIaware) {
            ServiceThread thirdAppHandlerThread = new ServiceThread("ThirdAppHandlerThread", 10, false);
            thirdAppHandlerThread.start();
            Handler thirdAppHandler = new Handler(thirdAppHandlerThread.getLooper());
            this.mFgThirdAppBroadcastQueue = new HwBroadcastQueue(this, thirdAppHandler, "fgthirdapp", (long) BROADCAST_FG_TIMEOUT, false);
            this.mBgThirdAppBroadcastQueue = new HwBroadcastQueue(this, thirdAppHandler, "bgthirdapp", (long) BROADCAST_BG_TIMEOUT, false);
            broadcastQueues[2] = this.mFgThirdAppBroadcastQueue;
            broadcastQueues[3] = this.mBgThirdAppBroadcastQueue;
        }
    }

    protected void setKeyAppBroadcastQueue(BroadcastQueue[] broadcastQueues) {
        if (enableIaware) {
            ServiceThread keyAppHandlerThread = new ServiceThread("keyAppHanderThread", 0, false);
            keyAppHandlerThread.start();
            Handler keyAppHandler = new Handler(keyAppHandlerThread.getLooper());
            this.mFgKeyAppBroadcastQueue = new HwBroadcastQueue(this, keyAppHandler, "fgkeyapp", (long) BROADCAST_FG_TIMEOUT, false);
            this.mBgKeyAppBroadcastQueue = new HwBroadcastQueue(this, keyAppHandler, "bgkeyapp", (long) BROADCAST_BG_TIMEOUT, false);
            broadcastQueues[4] = this.mFgKeyAppBroadcastQueue;
            broadcastQueues[5] = this.mBgKeyAppBroadcastQueue;
        }
    }

    protected boolean isThirdPartyAppBroadcastQueue(ProcessRecord callerApp) {
        boolean z = false;
        if ((!enableRms && !getIawareResourceFeature(1)) || callerApp == null) {
            return false;
        }
        if (DEBUG_HWTRIM || Log.HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Split enqueueing broadcast [callerApp]:");
            stringBuilder.append(callerApp);
            Log.i(str, stringBuilder.toString());
        }
        if (callerApp.instr != null) {
            return false;
        }
        if ((callerApp.info.flags & 1) == 0 || (callerApp.info.hwFlags & 33554432) != 0) {
            z = true;
        }
        return z;
    }

    protected boolean isKeyAppBroadcastQueue(int type, String name) {
        return getIawareResourceFeature(1) && name != null && isKeyApp(type, 0, name);
    }

    protected boolean isThirdPartyAppPendingBroadcastProcessLocked(int pid) {
        boolean z = false;
        if (!enableRms && !getIawareResourceFeature(1)) {
            return false;
        }
        if (this.mFgThirdAppBroadcastQueue.isPendingBroadcastProcessLocked(pid) || this.mBgThirdAppBroadcastQueue.isPendingBroadcastProcessLocked(pid)) {
            z = true;
        }
        return z;
    }

    protected boolean isKeyAppPendingBroadcastProcessLocked(int pid) {
        boolean z = true;
        if (!getIawareResourceFeature(1) || this.mFgKeyAppBroadcastQueue == null || this.mBgKeyAppBroadcastQueue == null) {
            return false;
        }
        if (!(this.mFgKeyAppBroadcastQueue.isPendingBroadcastProcessLocked(pid) || this.mBgKeyAppBroadcastQueue.isPendingBroadcastProcessLocked(pid))) {
            z = false;
        }
        return z;
    }

    protected BroadcastQueue thirdPartyAppBroadcastQueueForIntent(Intent intent) {
        boolean isFg = (intent.getFlags() & 268435456) != 0;
        if (DEBUG_HWTRIM || Log.HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("thirdAppBroadcastQueueForIntent intent ");
            stringBuilder.append(intent);
            stringBuilder.append(" on ");
            stringBuilder.append(isFg ? "fgthirdapp" : "bgthirdapp");
            stringBuilder.append(" queue");
            Log.i(str, stringBuilder.toString());
        }
        return isFg ? this.mFgThirdAppBroadcastQueue : this.mBgThirdAppBroadcastQueue;
    }

    protected BroadcastQueue keyAppBroadcastQueueForIntent(Intent intent) {
        boolean isFg = (intent.getFlags() & 268435456) != 0;
        if (DEBUG_HWTRIM || Log.HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("keyAppBroadcastQueueForIntent intent ");
            stringBuilder.append(intent);
            stringBuilder.append(" on ");
            stringBuilder.append(isFg ? "fgkeyapp" : "bgkeyapp");
            stringBuilder.append(" queue");
            Log.i(str, stringBuilder.toString());
        }
        if (isFg) {
            updateSRMSStatisticsData(0);
        } else {
            updateSRMSStatisticsData(1);
        }
        return isFg ? this.mFgKeyAppBroadcastQueue : this.mBgKeyAppBroadcastQueue;
    }

    protected void initBroadcastResourceLocked() {
        if (this.mBroadcastResource == null) {
            if (DEBUG_HWTRIM || Log.HWINFO) {
                Log.d(TAG, "init BroadcastResource");
            }
            this.mBroadcastResource = HwFrameworkFactory.getHwResource(11);
        }
    }

    public void checkOrderedBroadcastTimeoutLocked(String actionOrPkg, int timeCost, boolean isInToOut) {
        if (getIawareResourceFeature(2)) {
            if (this.mOrderedBroadcastResource == null) {
                if (DEBUG_HWTRIM || Log.HWINFO) {
                    Log.d(TAG, "init OrderedBroadcastResource");
                }
                this.mOrderedBroadcastResource = HwFrameworkFactory.getHwResource(31);
            }
            if (!(this.mOrderedBroadcastResource == null || isInToOut)) {
                this.mOrderedBroadcastResource.acquire(0, actionOrPkg, 0);
            }
        }
    }

    protected void checkBroadcastRecordSpeed(int callingUid, String callerPackage, ProcessRecord callerApp) {
        if (this.mBroadcastResource != null && callerApp != null) {
            int uid = callingUid;
            String pkg = callerPackage;
            int processType = getProcessType(callerApp);
            if (("1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0")) || processType == 0) && 2 == this.mBroadcastResource.acquire(uid, pkg, processType) && ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("This App send broadcast speed is overload! uid = ");
                stringBuilder.append(uid);
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    protected void clearBroadcastResource(ProcessRecord app) {
        if (this.mBroadcastResource != null && app != null) {
            int uid = app.info.uid;
            String pkg = app.info.packageName;
            int processType = getProcessType(app);
            if ("1".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0")) || processType == 0) {
                this.mBroadcastResource.clear(uid, pkg, processType);
            }
        }
    }

    private int getProcessType(ProcessRecord app) {
        return (app.info.flags & 1) != 0 ? 2 : 0;
    }

    public boolean isKeyApp(int type, int value, String key) {
        if (this.mBroadcastResource == null || key == null || 1 != this.mBroadcastResource.queryPkgPolicy(type, value, key)) {
            return false;
        }
        if (Log.HWLog) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isKeyApp in whiteList key:");
            stringBuilder.append(key);
            stringBuilder.append(" , type is ");
            stringBuilder.append(type);
            Log.i(str, stringBuilder.toString());
        }
        return true;
    }

    public AbsUserBehaviourRecord getRecordCust() {
        if (this.mCust == null) {
            this.mCust = new HwUserBehaviourRecord(this.mContext);
        }
        return this.mCust;
    }

    private void initAppResourceLocked() {
        if (this.mAppResource == null) {
            Log.i(TAG, "init Appresource");
            this.mAppResource = HwFrameworkFactory.getHwResource(18);
        }
    }

    private void initAppServiceResourceLocked() {
        if (this.mAppServiceResource == null) {
            Log.i(TAG, "init AppServiceResource");
            this.mAppServiceResource = HwFrameworkFactory.getHwResource(17);
        }
    }

    public void initAppAndAppServiceResourceLocked() {
        initAppResourceLocked();
        initAppServiceResourceLocked();
    }

    public boolean isAcquireAppServiceResourceLocked(ServiceRecord sr, ProcessRecord app) {
        if (this.mAppServiceResource == null || sr == null || sr.appInfo.uid <= 0 || sr.appInfo.packageName == null || sr.serviceInfo.name == null || 2 != this.mAppServiceResource.acquire(sr.appInfo.uid, sr.appInfo.packageName, getProcessType(app))) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to acquire AppServiceResource:");
        stringBuilder.append(sr.serviceInfo.name);
        stringBuilder.append(" of ");
        stringBuilder.append(sr.appInfo.packageName);
        stringBuilder.append("/");
        stringBuilder.append(sr.appInfo.uid);
        Log.i(str, stringBuilder.toString());
        return false;
    }

    public boolean isAcquireAppResourceLocked(ProcessRecord app) {
        if (!(this.mAppResource == null || app == null || app.uid <= 0 || app.info == null || app.processName == null || app.startTime <= 0)) {
            int processType = ((app.info.flags & 1) != 0 && (app.info.hwFlags & 33554432) == 0 && (app.info.hwFlags & 67108864) == 0) ? 2 : 0;
            Bundle args = new Bundle();
            args.putInt("callingUid", app.uid);
            args.putString("pkg", app.processName);
            args.putLong("startTime", app.startTime);
            args.putInt("processType", processType);
            args.putBoolean("launchfromActivity", app.launchfromActivity);
            args.putBoolean("topProcess", isTopProcessLocked(app));
            if (2 == this.mAppResource.acquire(null, null, args)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to acquire AppResource:");
                stringBuilder.append(app.info.packageName);
                stringBuilder.append("/");
                stringBuilder.append(app.uid);
                Log.i(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private void clearAppServiceResource(ProcessRecord app) {
        if (this.mAppServiceResource != null && app != null) {
            this.mAppServiceResource.clear(app.uid, app.info.packageName, getProcessType(app));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clear AppServiceResource of ");
            stringBuilder.append(app.info.packageName);
            stringBuilder.append("/");
            stringBuilder.append(app.uid);
            Log.i(str, stringBuilder.toString());
        }
    }

    private void clearAppResource(ProcessRecord app) {
        if (this.mAppResource != null && app != null && app.uid > 0 && app.info != null && app.info.packageName != null) {
            int processType = ((app.info.flags & 1) != 0 && (app.info.hwFlags & 33554432) == 0 && (app.info.hwFlags & 67108864) == 0) ? 2 : 0;
            this.mAppResource.clear(app.uid, app.info.packageName, processType);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clear Appresource of ");
            stringBuilder.append(app.info.packageName);
            stringBuilder.append("/");
            stringBuilder.append(app.uid);
            Log.i(str, stringBuilder.toString());
        }
    }

    public void clearAppAndAppServiceResource(ProcessRecord app) {
        clearAppServiceResource(app);
        clearAppResource(app);
    }

    public void trimGLMemory(int level) {
        Slog.i(TAG, " trimGLMemory begin ");
        synchronized (this.mLruProcesses) {
            int list_size = this.mLruProcesses.size();
            for (int i = 0; i < list_size; i++) {
                ProcessRecord app = (ProcessRecord) this.mLruProcesses.get(i);
                if (app.thread != null) {
                    try {
                        app.thread.scheduleTrimMemory(level);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
        Slog.i(TAG, " trimGLMemory end ");
    }

    public Map<Integer, AwareProcessBaseInfo> getAllProcessBaseInfo() {
        ArrayMap<Integer, AwareProcessBaseInfo> list;
        synchronized (this.mPidsSelfLocked) {
            int size = this.mPidsSelfLocked.size();
            list = new ArrayMap(size);
            for (int i = 0; i < size; i++) {
                ProcessRecord p = (ProcessRecord) this.mPidsSelfLocked.valueAt(i);
                AwareProcessBaseInfo baseInfo = new AwareProcessBaseInfo();
                baseInfo.mCurAdj = p.curAdj;
                baseInfo.mForegroundActivities = p.foregroundActivities;
                baseInfo.mAdjType = p.adjType;
                baseInfo.mHasShownUi = p.hasShownUi;
                baseInfo.mUid = p.uid;
                baseInfo.mAppUid = p.info.uid;
                baseInfo.mSetProcState = p.setProcState;
                list.put(Integer.valueOf(p.pid), baseInfo);
            }
        }
        return list;
    }

    public AwareProcessBaseInfo getProcessBaseInfo(int pid) {
        AwareProcessBaseInfo baseInfo;
        synchronized (this.mPidsSelfLocked) {
            baseInfo = new AwareProcessBaseInfo();
            baseInfo.mCurAdj = 1001;
            ProcessRecord p = (ProcessRecord) this.mPidsSelfLocked.get(pid);
            if (p != null) {
                baseInfo.mForegroundActivities = p.foregroundActivities;
                baseInfo.mUid = p.uid;
                baseInfo.mAppUid = p.info.uid;
                baseInfo.mSetProcState = p.setProcState;
                baseInfo.mCurAdj = p.curAdj;
                baseInfo.mAdjType = p.adjType;
            }
        }
        return baseInfo;
    }

    public void reportAssocEnable(ArrayMap<Integer, Integer> forePids) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC)) && forePids != null) {
            synchronized (this) {
                Iterator it = this.mLruProcesses.iterator();
                while (it.hasNext()) {
                    ProcessRecord proc = (ProcessRecord) it.next();
                    if (proc != null) {
                        if (proc.foregroundActivities) {
                            forePids.put(Integer.valueOf(proc.pid), Integer.valueOf(proc.uid));
                        }
                        ArrayList<String> pkgs = new ArrayList();
                        int size = proc.pkgList.size();
                        for (int i = 0; i < size; i++) {
                            String pkg = (String) proc.pkgList.keyAt(i);
                            if (!pkgs.contains(pkg)) {
                                pkgs.add(pkg);
                            }
                        }
                        Bundle args = new Bundle();
                        args.putInt("callPid", proc.pid);
                        args.putInt("callUid", proc.uid);
                        args.putString("callProcName", proc.processName);
                        args.putInt("userid", proc.userId);
                        args.putStringArrayList(MemoryConstant.MEM_PREREAD_ITEM_NAME, pkgs);
                        args.putInt("relationType", 4);
                        HwSysResManager.getInstance().reportData(new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), args));
                        Iterator it2 = proc.connections.iterator();
                        while (it2.hasNext()) {
                            ConnectionRecord cr = (ConnectionRecord) it2.next();
                            if (cr != null) {
                                if (cr.binding != null) {
                                    this.mHwAMSEx.reportServiceRelationIAware(1, cr.binding.service, proc);
                                }
                            }
                        }
                        it2 = proc.conProviders.iterator();
                        while (it2.hasNext()) {
                            ContentProviderConnection cpc = (ContentProviderConnection) it2.next();
                            if (cpc != null) {
                                this.mHwAMSEx.reportServiceRelationIAware(2, cpc.provider, proc);
                            }
                        }
                    }
                }
                this.mHwAMSEx.reportHomeProcess(this.mHomeProcess);
            }
        }
    }

    public void setPackageStoppedState(List<String> packageList, boolean stopped, int targetUid) {
        if (packageList != null) {
            int userId = UserHandle.getUserId(targetUid);
            IPackageManager pm = AppGlobals.getPackageManager();
            String packageName;
            try {
                synchronized (this) {
                    for (String packageName2 : packageList) {
                        pm.setPackageStoppedState(packageName2, stopped, userId);
                    }
                }
            } catch (RemoteException e) {
            } catch (IllegalArgumentException e2) {
                packageName2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed trying to unstop package ");
                stringBuilder.append(packageList.toString());
                stringBuilder.append(": ");
                stringBuilder.append(e2);
                Slog.w(packageName2, stringBuilder.toString());
            }
        }
    }

    protected final boolean cleanUpApplicationRecordLocked(ProcessRecord app, boolean restarting, boolean allowRestart, int index, boolean replacingPid) {
        if (IS_TABLET) {
            if (this.mSplitActivityEntryStack != null && this.mSplitActivityEntryStack.containsKey(Integer.valueOf(app.pid))) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Split main entrance killed, clear sub activities for ");
                stringBuilder.append(app.info.packageName);
                stringBuilder.append(", pid ");
                stringBuilder.append(app.pid);
                Slog.w(str, stringBuilder.toString());
                clearEntryStack(app.pid, null);
                this.mSplitActivityEntryStack.remove(Integer.valueOf(app.pid));
            }
            if (this.mCurrentSplitIntent != null && this.mCurrentSplitIntent.containsKey(Integer.valueOf(app.pid))) {
                this.mCurrentSplitIntent.remove(Integer.valueOf(app.pid));
            }
        }
        return super.cleanUpApplicationRecordLocked(app, restarting, allowRestart, index, replacingPid);
    }

    public void cleanActivityByUid(List<String> packageList, int targetUid) {
        synchronized (this) {
            int userId = UserHandle.getUserId(targetUid);
            for (String packageName : packageList) {
                if (canCleanTaskRecord(packageName)) {
                    this.mStackSupervisor.finishDisabledPackageActivitiesLocked(packageName, null, true, false, userId);
                }
            }
        }
    }

    public int numOfPidWithActivity(int uid) {
        int count = 0;
        synchronized (this.mPidsSelfLocked) {
            int list_size = this.mPidsSelfLocked.size();
            for (int i = 0; i < list_size; i++) {
                ProcessRecord p = (ProcessRecord) this.mPidsSelfLocked.valueAt(i);
                if (p.uid == uid && p.hasShownUi) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void forceValidateHomeButton(int userId) {
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, userId) == 0 || Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
            Global.putInt(this.mContext.getContentResolver(), "device_provisioned", 1);
            Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userId);
            Log.w(TAG, "DEVICE_PROVISIONED or USER_SETUP_COMPLETE set 0 to 1!");
        }
    }

    protected boolean isStartLauncherActivity(Intent intent, int userId) {
        if (intent == null) {
            Log.w(TAG, "intent is null, not start launcher!");
            return false;
        }
        PackageManager pm = this.mContext.getPackageManager();
        Intent mainIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT");
        ComponentName cmp = intent.getComponent();
        if (pm != null && intent.hasCategory("android.intent.category.HOME")) {
            long origId = Binder.clearCallingIdentity();
            try {
                for (ResolveInfo info : pm.queryIntentActivitiesAsUser(mainIntent, 0, userId)) {
                    if (info != null && info.priority == 0 && cmp != null && info.activityInfo != null && cmp.getPackageName().equals(info.activityInfo.packageName)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("info priority is 0, cmp: ");
                        stringBuilder.append(cmp);
                        stringBuilder.append(", userId: ");
                        stringBuilder.append(userId);
                        Log.d(str, stringBuilder.toString());
                        return true;
                    }
                }
                Binder.restoreCallingIdentity(origId);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:10:0x0019, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String getPackageNameForPid(int pid) {
        synchronized (this.mPidsSelfLocked) {
            ProcessRecord proc = (ProcessRecord) this.mPidsSelfLocked.get(pid);
            if (proc != null) {
                String str = proc.info != null ? proc.info.packageName : "android";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ProcessRecord for pid ");
                stringBuilder.append(pid);
                stringBuilder.append(" does not exist");
                Flog.i(100, stringBuilder.toString());
                return null;
            }
        }
    }

    public boolean isInMultiWindowMode() {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                ActivityStack focusedStack = getFocusedStack();
                if (focusedStack == null) {
                    Binder.restoreCallingIdentity(origId);
                    return false;
                }
                boolean inMultiWindowMode = focusedStack.inMultiWindowMode();
                Binder.restoreCallingIdentity(origId);
                return inMultiWindowMode;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isLauncher(String packageName) {
        if (Process.myUid() != 1000 || packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        if (GestureNavConst.DEFAULT_LAUNCHER_PACKAGE.equals(packageName)) {
            return true;
        }
        if (this.mContext != null) {
            List<ResolveInfo> outActivities = new ArrayList();
            PackageManager pm = this.mContext.getPackageManager();
            if (pm != null) {
                ComponentName componentName = pm.getHomeActivities(outActivities);
                if (componentName == null || componentName.getPackageName() == null) {
                    for (ResolveInfo info : outActivities) {
                        String homePkg = info.activityInfo.packageName;
                        if (packageName.equals(homePkg)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("homePkg is ");
                            stringBuilder.append(homePkg);
                            stringBuilder.append(" ,isLauncher");
                            Slog.d(str, stringBuilder.toString());
                            return true;
                        }
                    }
                } else if (packageName.equals(componentName.getPackageName())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private void setIntentInfo(Intent intent, int pid, Bundle bundle, boolean forLast) {
        if (forLast) {
            this.mLastSplitIntent = intent;
            this.mSplitExtras = bundle;
            return;
        }
        if (!this.mCurrentSplitIntent.containsKey(Integer.valueOf(pid))) {
            Log.e(TAG, "CRITICAL_LOG add intent info.");
        }
        this.mCurrentSplitIntent.put(Integer.valueOf(pid), intent);
    }

    private Parcelable[] getIntentInfo(int pid, boolean forLast) {
        if (forLast) {
            return new Parcelable[]{this.mLastSplitIntent, this.mSplitExtras};
        }
        return new Parcelable[]{(Parcelable) this.mCurrentSplitIntent.get(Integer.valueOf(pid)), null};
    }

    public void addToEntryStack(int pid, IBinder token, int resultCode, Intent resultData) {
        if (this.mSplitActivityEntryStack == null) {
            this.mSplitActivityEntryStack = new HashMap();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addToEntryStack, activity is ");
        stringBuilder.append(token);
        Flog.i(100, stringBuilder.toString());
        Stack<IBinder> pkgStack = (Stack) this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
        if (pkgStack == null) {
            pkgStack = new Stack();
        }
        pkgStack.push(token);
        this.mSplitActivityEntryStack.put(Integer.valueOf(pid), pkgStack);
    }

    public void clearEntryStack(int pid, IBinder selfToken) {
        if (this.mSplitActivityEntryStack != null && !this.mSplitActivityEntryStack.isEmpty()) {
            Stack<IBinder> pkgStack = (Stack) this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
            if (pkgStack != null && !pkgStack.empty() && (selfToken == null || selfToken.equals(pkgStack.peek()))) {
                long ident = Binder.clearCallingIdentity();
                while (!pkgStack.empty()) {
                    IBinder token = (IBinder) pkgStack.pop();
                    if (!(token == null || token.equals(selfToken))) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Clearing entry ");
                        stringBuilder.append(token);
                        Flog.i(100, stringBuilder.toString());
                        finishActivity(token, 0, null, 0);
                    }
                }
                Binder.restoreCallingIdentity(ident);
                if (selfToken != null) {
                    pkgStack.push(selfToken);
                }
            }
        }
    }

    public boolean isTopSplitActivity(int pid, IBinder token) {
        if (this.mSplitActivityEntryStack == null || this.mSplitActivityEntryStack.isEmpty() || token == null) {
            return false;
        }
        Stack<IBinder> pkgStack = (Stack) this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
        if (pkgStack == null || pkgStack.empty()) {
            return false;
        }
        return token.equals(pkgStack.peek());
    }

    public void removeFromEntryStack(int pid, IBinder token) {
        if (token != null && this.mSplitActivityEntryStack != null) {
            Stack<IBinder> pkgStack = (Stack) this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
            if (pkgStack != null && pkgStack.empty()) {
                pkgStack.remove(token);
            }
        }
    }

    public boolean isLimitedPackageBroadcast(Intent intent) {
        String action = intent.getAction();
        boolean limitedPackageBroadcast = false;
        if (!"android.intent.action.PACKAGE_ADDED".equals(action) && !"android.intent.action.PACKAGE_REMOVED".equals(action)) {
            return false;
        }
        Bundle intentExtras = intent.getExtras();
        if (intentExtras != null) {
            limitedPackageBroadcast = intentExtras.getBoolean("LimitedPackageBroadcast", false);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Android Wear-isLimitedPackageBroadcast: limitedPackageBroadcast = ");
        stringBuilder.append(limitedPackageBroadcast);
        Flog.d(100, stringBuilder.toString());
        return limitedPackageBroadcast;
    }

    public boolean isPkgHasAlarm(List<String> packageList, int targetUid) {
        if (packageList == null) {
            return false;
        }
        synchronized (this) {
            for (String packageName : packageList) {
                if (this.mIntentSenderRecords.size() > 0) {
                    for (WeakReference<PendingIntentRecord> wpir : this.mIntentSenderRecords.values()) {
                        if (wpir != null) {
                            PendingIntentRecord pir = (PendingIntentRecord) wpir.get();
                            if (!(pir == null || pir.key == null)) {
                                if (pir.key.packageName != null) {
                                    if (pir.key.packageName.equals(packageName)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    continue;
                }
            }
            return false;
        }
    }

    protected void checkAndPrintTestModeLog(List list, String intentAction, String callingMethod, String desciption) {
        if (Log.HWINFO && list != null) {
            PackageManager pm = this.mContext.getPackageManager();
            if ("android.provider.Telephony.SMS_RECEIVED".equals(intentAction) || "android.provider.Telephony.SMS_DELIVER".equals(intentAction)) {
                int list_size = list.size();
                String appName = null;
                String packageName = null;
                for (int ii = 0; ii < list_size; ii++) {
                    boolean is_data_ok;
                    try {
                        Object myReceiver = list.get(ii);
                        if (myReceiver instanceof ResolveInfo) {
                            packageName = ((ResolveInfo) myReceiver).getComponentInfo().packageName;
                        } else if (myReceiver instanceof BroadcastFilter) {
                            packageName = ((BroadcastFilter) myReceiver).packageName;
                        }
                        appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                        is_data_ok = true;
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("checkAndPrintTestModeLog(). error: ");
                        stringBuilder.append(e.toString());
                        Log.e(str, stringBuilder.toString());
                        is_data_ok = false;
                    }
                    if (is_data_ok) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" <");
                        stringBuilder2.append(appName);
                        stringBuilder2.append(">[");
                        stringBuilder2.append(packageName);
                        stringBuilder2.append("][");
                        stringBuilder2.append(callingMethod);
                        stringBuilder2.append("]");
                        stringBuilder2.append(desciption);
                        Log.i(str2, stringBuilder2.toString());
                    }
                }
            }
        }
    }

    public void registerActivityObserver(IActivityObserver observer) {
        enforceCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "registerActivityObserver()");
        synchronized (this) {
            this.mActivityObservers.register(observer);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerActivityObserver:");
        stringBuilder.append(observer);
        stringBuilder.append(", callingPid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", num=");
        stringBuilder.append(this.mActivityObservers.getRegisteredCallbackCount());
        Slog.i(str, stringBuilder.toString());
    }

    public void unregisterActivityObserver(IActivityObserver observer) {
        enforceCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "registerActivityObserver()");
        synchronized (this) {
            this.mActivityObservers.unregister(observer);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterActivityObserver:");
        stringBuilder.append(observer);
        stringBuilder.append(", callingPid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", num=");
        stringBuilder.append(this.mActivityObservers.getRegisteredCallbackCount());
        Slog.i(str, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:20:0x0049, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean requestContentNode(ComponentName componentName, Bundle data, int token) {
        enforceCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "requestContentNode()");
        synchronized (this) {
            ActivityRecord activity = getFocusedStack().getTopActivity();
            ActivityRecord realActivity = getTopActivityAppToken(componentName, activity);
            if (realActivity == null || realActivity.app == null || realActivity.app.thread == null) {
            } else {
                try {
                    realActivity.app.thread.requestContentNode(realActivity.appToken, data, token);
                    return true;
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestContentNode failed: crash calling ");
                    stringBuilder.append(activity);
                    Slog.w(str, stringBuilder.toString());
                    return false;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0049, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean requestContentOther(ComponentName componentName, Bundle data, int token) {
        enforceCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "requestContentOther()");
        synchronized (this) {
            ActivityRecord activity = getFocusedStack().getTopActivity();
            ActivityRecord realActivity = getTopActivityAppToken(componentName, activity);
            if (realActivity == null || realActivity.app == null || realActivity.app.thread == null) {
            } else {
                try {
                    realActivity.app.thread.requestContentOther(realActivity.appToken, data, token);
                    return true;
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestContentOther failed: crash calling ");
                    stringBuilder.append(activity);
                    Slog.w(str, stringBuilder.toString());
                    return false;
                }
            }
        }
    }

    public void dispatchActivityResumed(IBinder token) {
        if (this.mActivityObservers != null) {
            ActivityRecord activityRecord = ActivityRecord.forToken(token);
            if (activityRecord != null && activityRecord.app != null) {
                int i = this.mActivityObservers.beginBroadcast();
                while (i > 0) {
                    i--;
                    IActivityObserver observer = (IActivityObserver) this.mActivityObservers.getBroadcastItem(i);
                    if (observer != null) {
                        try {
                            observer.activityResumed(activityRecord.app.pid, activityRecord.app.uid, activityRecord.realActivity);
                        } catch (RemoteException e) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("observer.activityResumed get RemoteException, remove observer ");
                            stringBuilder.append(observer);
                            Slog.w(str, stringBuilder.toString());
                            unregisterActivityObserver(observer);
                        }
                    }
                }
                this.mActivityObservers.finishBroadcast();
            }
        }
    }

    public void dispatchActivityPaused(IBinder token) {
        if (this.mActivityObservers != null) {
            ActivityRecord activityRecord = ActivityRecord.forToken(token);
            if (activityRecord != null && activityRecord.app != null) {
                int i = this.mActivityObservers.beginBroadcast();
                while (i > 0) {
                    i--;
                    IActivityObserver observer = (IActivityObserver) this.mActivityObservers.getBroadcastItem(i);
                    if (observer != null) {
                        try {
                            observer.activityPaused(activityRecord.app.pid, activityRecord.app.uid, activityRecord.realActivity);
                        } catch (RemoteException e) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("observer.activityResumed get RemoteException, remove observer ");
                            stringBuilder.append(observer);
                            Slog.w(str, stringBuilder.toString());
                            unregisterActivityObserver(observer);
                        }
                    }
                }
                this.mActivityObservers.finishBroadcast();
            }
        }
    }

    private ActivityRecord getTopActivityAppToken(ComponentName componentName, ActivityRecord activity) {
        if (activity == null) {
            Slog.w(TAG, "requestContent failed: no activity");
            return null;
        } else if (componentName == null) {
            return null;
        } else {
            String str;
            if (componentName.equals(activity.realActivity)) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("componentName = ");
                stringBuilder.append(componentName);
                stringBuilder.append(" realActivity = ");
                stringBuilder.append(activity.realActivity);
                stringBuilder.append(" isEqual = ");
                stringBuilder.append(componentName.equals(activity.realActivity));
                Slog.w(str, stringBuilder.toString());
                if (this.mLastActivityRecord != null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" mLastActivityRecord = ");
                    stringBuilder.append(this.mLastActivityRecord.realActivity);
                    Slog.w(str, stringBuilder.toString());
                }
                return activity;
            }
            ActivityRecord lastResumedActivity = getLastResumedActivity();
            StringBuilder stringBuilder2;
            if (lastResumedActivity != null && componentName.equals(lastResumedActivity.realActivity)) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("lastResumedActivity = ");
                stringBuilder2.append(lastResumedActivity.realActivity);
                Slog.w(str, stringBuilder2.toString());
                return lastResumedActivity;
            } else if (this.mLastActivityRecord == null || !componentName.equals(this.mLastActivityRecord.realActivity)) {
                return null;
            } else {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" mLastActivityRecord = ");
                stringBuilder2.append(this.mLastActivityRecord.realActivity);
                Slog.w(str, stringBuilder2.toString());
                return this.mLastActivityRecord;
            }
        }
    }

    public boolean isHiddenSpaceSwitch(UserInfo first, UserInfo second) {
        if (first == null || second == null || first.id == second.id) {
            return false;
        }
        if (first.isHwHiddenSpace() || second.isHwHiddenSpace()) {
            Slog.i(TAG, "isHiddenSpaceSwitch!");
            return true;
        }
        Slog.i(TAG, "not hiddenSpaceSwitch");
        return false;
    }

    public int iawareGetUidState(int uid) {
        UidRecord uidRec = (UidRecord) this.mActiveUids.get(uid);
        return uidRec == null ? 19 : uidRec.curProcState;
    }

    protected final ProcessRecord getProcessRecordLocked(String processName, int uid, boolean keepIfLarge) {
        return super.getProcessRecordLocked(processName, UserHandle.getUid(handleUserForClone(processName, UserHandle.getUserId(uid)), uid), keepIfLarge);
    }

    protected final ContentProviderHolder getContentProviderImpl(IApplicationThread caller, String name, IBinder token, boolean stable, int userId) {
        ContentProviderHolder cph = super.getContentProviderImpl(caller, name, token, stable, handleUserForCloneOrAfw(name, userId));
        if (IS_SUPPORT_CLONE_APP && userId != 0 && cph == null) {
            UserInfo ui = this.mUserController.mInjector.getUserManagerInternal().getUserInfo(userId);
            if (ui != null && ui.isClonedProfile()) {
                return super.getContentProviderImpl(caller, name, token, stable, ui.profileGroupId);
            }
        }
        return cph;
    }

    protected int handleUserForCloneOrAfw(String name, int userId) {
        if (userId == 0 || name == null) {
            return userId;
        }
        int newUserId = userId;
        if (userId != this.mUserController.getCurrentUserIdLU()) {
            long ident = Binder.clearCallingIdentity();
            try {
                UserInfo ui = this.mUserController.getUserInfo(userId);
                if (ui != null && ((IS_SUPPORT_CLONE_APP && ui.isClonedProfile() && sAllowedCrossUserForCloneArrays.contains(name)) || (ui.isManagedProfile() && "com.huawei.android.launcher.settings".equals(name)))) {
                    newUserId = ui.profileGroupId;
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return newUserId;
    }

    protected final void removeContentProviderExternalUnchecked(String name, IBinder token, int userId) {
        super.removeContentProviderExternalUnchecked(name, token, handleUserForClone(name, userId));
    }

    protected int handleUserForClone(String name, int userId) {
        if (!IS_SUPPORT_CLONE_APP || userId == 0 || name == null) {
            return userId;
        }
        int newUserId = userId;
        if (userId != this.mUserController.getCurrentUserIdLU() && sAllowedCrossUserForCloneArrays.contains(name)) {
            long ident = Binder.clearCallingIdentity();
            try {
                UserInfo ui = this.mUserController.getUserInfo(userId);
                if (ui != null && ui.isClonedProfile()) {
                    newUserId = ui.profileGroupId;
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return newUserId;
    }

    final int broadcastIntentLocked(ProcessRecord callerApp, String callerPackage, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean ordered, boolean sticky, int callingPid, int callingUid, int userId) {
        Intent intent2 = intent;
        int i = userId;
        String action = null;
        if (intent2 != null) {
            action = intent.getAction();
        }
        String action2 = action;
        if ("com.android.launcher.action.INSTALL_SHORTCUT".equals(action2)) {
            intent2.putExtra("android.intent.extra.USER_ID", i);
        }
        if (this.mHwGameAssistantController != null) {
            this.mHwGameAssistantController.handleInterestedBroadcast(intent2);
        }
        if ("android.net.wifi.STATE_CHANGE".equals(action2) && getBgBroadcastQueue().getMtmBRManager() != null) {
            if (getBgBroadcastQueue().getMtmBRManager().iawareNeedSkipBroadcastSend(action2, new Object[]{intent2})) {
                return 0;
            }
        }
        return super.broadcastIntentLocked(callerApp, callerPackage, intent2, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, appOp, bOptions, ordered, sticky, callingPid, callingUid, handleUserForClone(action2, i));
    }

    public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType, boolean requireForeground, String callingPackage, int userId) throws TransactionTooLargeException {
        return super.startService(caller, service, resolvedType, requireForeground, callingPackage, handleUserForClone(getTargetFromIntentForClone(service), userId));
    }

    public int stopService(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        return super.stopService(caller, service, resolvedType, handleUserForClone(getTargetFromIntentForClone(service), userId));
    }

    public int bindService(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String callingPackage, int userId) throws TransactionTooLargeException {
        Intent intent = service;
        return super.bindService(caller, token, intent, resolvedType, connection, flags, callingPackage, handleUserForClone(getTargetFromIntentForClone(intent), userId));
    }

    ComponentName startServiceInPackage(int uid, Intent service, String resolvedType, boolean fgRequired, String callingPackage, int userId) throws TransactionTooLargeException {
        return super.startServiceInPackage(uid, service, resolvedType, fgRequired, callingPackage, handleUserForClone(getTargetFromIntentForClone(service), userId));
    }

    private String getTargetFromIntentForClone(Intent intent) {
        if (intent.getAction() == null) {
            return intent.getComponent() != null ? intent.getComponent().getPackageName() : null;
        } else {
            return intent.getAction();
        }
    }

    public int iawareGetUidProcNum(int uid) {
        UidRecord uidRec = (UidRecord) this.mActiveUids.get(uid);
        return uidRec == null ? 0 : uidRec.numProcs;
    }

    protected void notifyProcessWillDie(boolean byForceStop, boolean crashed, boolean byAnr, String packageName, int pid, int uid) {
        AwareFakeActivityRecg.self().notifyProcessWillDie(byForceStop, crashed, byAnr, packageName, pid, uid);
    }

    public void togglePCMode(boolean pcMode, int displayId) {
        synchronized (this) {
            if (!pcMode) {
                ActivityDisplay activityDisplay = (ActivityDisplay) this.mStackSupervisor.mActivityDisplays.get(displayId);
                if (activityDisplay != null && (this.mStackSupervisor instanceof HwActivityStackSupervisor)) {
                    int size = activityDisplay.getChildCount();
                    ArrayList<ActivityStack> stacks = new ArrayList();
                    for (int i = 0; i < size; i++) {
                        stacks.add(activityDisplay.getChildAt(i));
                    }
                    ((HwActivityStackSupervisor) this.mStackSupervisor).onDisplayRemoved(stacks);
                }
            }
            if (this.mWindowManager instanceof HwWindowManagerService) {
                ((HwWindowManagerService) this.mWindowManager).togglePCMode(pcMode, displayId);
            }
        }
    }

    public void updateFingerprintSlideSwitch() {
        if (HwPCUtils.enabled() && (this.mWindowManager instanceof HwWindowManagerService)) {
            this.mWindowManager.updateFingerprintSlideSwitch();
        }
    }

    public void freezeOrThawRotationInPcMode() {
        if (HwPCUtils.enabledInPad()) {
            synchronized (this) {
                this.mWindowManager.freezeRotation(1);
            }
        }
    }

    /* JADX WARNING: Missing block: B:57:0x0161, code:
            android.os.Binder.restoreCallingIdentity(r12);
     */
    /* JADX WARNING: Missing block: B:58:0x0165, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void relaunchIMEIfNecessary() {
        Throwable th;
        String packageName;
        if (checkCallingPermission("android.permission.KILL_BACKGROUND_PROCESSES") == 0 || checkCallingPermission("android.permission.RESTART_PACKAGES") == 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                packageName = Secure.getString(this.mContext.getContentResolver(), "default_input_method");
                List<InputMethodInfo> methodList = null;
                if (HwPCUtils.enabledInPad()) {
                    InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
                    if (imm != null) {
                        methodList = imm.getInputMethodList();
                    }
                }
                List<InputMethodInfo> methodList2 = methodList;
                if (packageName != null) {
                    int index = packageName.indexOf(47);
                    if (index == -1) {
                        Binder.restoreCallingIdentity(callingId);
                        return;
                    }
                    String packageName2 = packageName.substring(0, index);
                    int userId = this.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), UserHandle.myUserId(), true, 2, "killBackgroundProcesses", null);
                    IPackageManager pm = AppGlobals.getPackageManager();
                    synchronized (this) {
                        int appId;
                        IPackageManager iPackageManager;
                        int i;
                        String str;
                        int appId2 = -1;
                        try {
                            appId = UserHandle.getAppId(pm.getPackageUid(packageName2, 268435456, userId));
                        } catch (RemoteException e) {
                            String str2 = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unable to get Package Uid, userId = ");
                            stringBuilder.append(userId);
                            Slog.e(str2, stringBuilder.toString());
                            appId = appId2;
                        } catch (Throwable th2) {
                            th = th2;
                            iPackageManager = pm;
                            i = userId;
                            str = packageName2;
                            throw th;
                        }
                        if (appId == -1) {
                            try {
                                String str3 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Invalid packageName: ");
                                stringBuilder2.append(packageName2);
                                Slog.w(str3, stringBuilder2.toString());
                                Binder.restoreCallingIdentity(callingId);
                            } catch (Throwable th3) {
                                th = th3;
                                iPackageManager = pm;
                                i = userId;
                                str = packageName2;
                                throw th;
                            }
                        } else if (!HwPCUtils.enabledInPad() || methodList2 == null) {
                            killPackageProcessesLocked(packageName2, appId, userId, 100, false, false, true, false, "relaunchIME");
                        } else {
                            Iterator it = methodList2.iterator();
                            while (it.hasNext()) {
                                InputMethodInfo mi = (InputMethodInfo) it.next();
                                Iterator it2 = it;
                                iPackageManager = pm;
                                i = userId;
                                str = packageName2;
                                try {
                                    killPackageProcessesLocked(mi.getPackageName(), appId, userId, 100, false, false, true, false, "relaunchIME");
                                    it = it2;
                                    pm = iPackageManager;
                                    userId = i;
                                    packageName2 = str;
                                } catch (Throwable th4) {
                                    th = th4;
                                    throw th;
                                }
                            }
                            i = userId;
                            str = packageName2;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            packageName = new StringBuilder();
            packageName.append("Permission Denial: killBackgroundProcesses() from pid=");
            packageName.append(Binder.getCallingPid());
            packageName.append(", uid=");
            packageName.append(Binder.getCallingUid());
            packageName.append(" requires ");
            packageName.append("android.permission.KILL_BACKGROUND_PROCESSES");
            packageName = packageName.toString();
            Slog.w(TAG, packageName);
            throw new SecurityException(packageName);
        }
    }

    /* JADX WARNING: Missing block: B:43:0x0098, code:
            android.os.Binder.restoreCallingIdentity(r1);
     */
    /* JADX WARNING: Missing block: B:44:0x009b, code:
            return;
     */
    /* JADX WARNING: Missing block: B:49:0x00a2, code:
            android.os.Binder.restoreCallingIdentity(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void hwRestoreTask(int taskId, float xPos, float yPos) {
        if (HwPCUtils.isPcCastModeInServer()) {
            HwPCMultiWindowManager multiWindowMgr = HwPCMultiWindowManager.getInstance(this);
            if (multiWindowMgr != null) {
                long origId = Binder.clearCallingIdentity();
                try {
                    synchronized (this) {
                        TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                        if (tr == null) {
                            Binder.restoreCallingIdentity(origId);
                        } else if (HwPCMultiWindowCompatibility.isRestorable(tr.mWindowState)) {
                            if (tr.getStack() instanceof HwActivityStack) {
                                ((HwActivityStack) tr.getStack()).resetOtherStacksVisible(true);
                            }
                            Rect rect = multiWindowMgr.getWindowBounds(tr);
                            if (rect == null) {
                                Binder.restoreCallingIdentity(origId);
                                return;
                            }
                            if (!(xPos == -1.0f || yPos == -1.0f)) {
                                Rect bounds = tr.getOverrideBounds();
                                if (bounds == null) {
                                    bounds = multiWindowMgr.getMaximizedBounds();
                                }
                                if (bounds.width() == 0 || bounds.height() == 0) {
                                } else {
                                    rect.offsetTo((int) (xPos - (((float) rect.width()) * ((xPos - ((float) bounds.left)) / ((float) bounds.width())))), (int) (yPos - (((float) rect.height()) * ((yPos - ((float) bounds.top)) / ((float) bounds.height())))));
                                }
                            }
                            tr.resize(rect, 3, true, false);
                        } else {
                            Binder.restoreCallingIdentity(origId);
                        }
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x003b A:{SYNTHETIC, Splitter: B:24:0x003b} */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0036 A:{SKIP, Catch:{ all -> 0x00dc }} */
    /* JADX WARNING: Missing block: B:42:0x00d6, code:
            android.os.Binder.restoreCallingIdentity(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void hwResizeTask(int taskId, Rect bounds) {
        if (HwPCUtils.isPcCastModeInServer()) {
            HwPCMultiWindowManager multiWindowMgr = HwPCMultiWindowManager.getInstance(this);
            if (multiWindowMgr != null) {
                long origId = Binder.clearCallingIdentity();
                try {
                    synchronized (this) {
                        TaskRecord task;
                        boolean isFullscreen = false;
                        boolean isMaximized = false;
                        if (bounds != null) {
                            if (bounds.top < 0) {
                                bounds = null;
                                isFullscreen = true;
                                task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                                StringBuilder stringBuilder;
                                if (task != null) {
                                    Binder.restoreCallingIdentity(origId);
                                    return;
                                } else if (multiWindowMgr.isSupportResize(task, isFullscreen, isMaximized)) {
                                    String str;
                                    String str2 = "HwPCMultiWindowManager";
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("hwResizeTask: ");
                                    if (bounds == null) {
                                        str = "null";
                                    } else {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append(bounds.toShortString());
                                        stringBuilder2.append(" (");
                                        stringBuilder2.append(bounds.width());
                                        stringBuilder2.append(", ");
                                        stringBuilder2.append(bounds.height());
                                        stringBuilder2.append(")");
                                        str = stringBuilder2.toString();
                                    }
                                    stringBuilder.append(str);
                                    HwPCUtils.log(str2, stringBuilder.toString());
                                    task.resize(bounds, 3, true, false);
                                    if ((task.getStack() instanceof HwActivityStack) && (isFullscreen || isMaximized)) {
                                        ((HwActivityStack) task.getStack()).resetOtherStacksVisible(false);
                                    }
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("hwResizeTask-fail: (");
                                    stringBuilder.append(Integer.toHexString(task.mWindowState));
                                    stringBuilder.append(")isFullscreen:");
                                    stringBuilder.append(isFullscreen);
                                    stringBuilder.append("; isMax:");
                                    stringBuilder.append(isMaximized);
                                    HwPCUtils.log("HwPCMultiWindowManager", stringBuilder.toString());
                                    Binder.restoreCallingIdentity(origId);
                                    return;
                                }
                            }
                        }
                        if (bounds != null && bounds.isEmpty()) {
                            bounds = multiWindowMgr.getMaximizedBounds();
                            isMaximized = true;
                        }
                        task = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                        if (task != null) {
                        }
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    public int getWindowState(IBinder token) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return -1;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityRecord r;
            synchronized (this) {
                r = ActivityRecord.isInStackLocked(token);
            }
            if (r != null) {
                if (r.task != null) {
                    int windowState = r.task.getWindowState();
                    Binder.restoreCallingIdentity(ident);
                    return windowState;
                }
            }
            Binder.restoreCallingIdentity(ident);
            return -1;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwRecentTaskInfo getHwRecentTaskInfo(int taskId) {
        synchronized (this) {
            long origId = Binder.clearCallingIdentity();
            try {
                TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(taskId);
                if (tr != null) {
                    HwRecentTaskInfo createHwRecentTaskInfoFromTaskRecord = createHwRecentTaskInfoFromTaskRecord(tr);
                } else {
                    Binder.restoreCallingIdentity(origId);
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    protected void forceGCAfterRebooting() {
        List<RunningAppProcessInfo> runningAppInfo = getRunningAppProcesses();
        if (runningAppInfo == null) {
            HwPCUtils.log("HwPCMultiWindowManager", "forceGCAfterRebooting-fail: runningAppInfo is null");
            return;
        }
        for (RunningAppProcessInfo appProcess : runningAppInfo) {
            Process.sendSignal(appProcess.pid, 10);
        }
    }

    protected HwRecentTaskInfo createHwRecentTaskInfoFromTaskRecord(TaskRecord tr) {
        RecentTaskInfo rti = createRecentTasks().createRecentTaskInfo(tr);
        HwRecentTaskInfo hwRti = new HwRecentTaskInfo();
        hwRti.translateRecentTaskinfo(rti);
        ActivityStack stack = tr.getStack();
        if (stack != null) {
            hwRti.displayId = stack.mDisplayId;
        }
        hwRti.windowState = tr.getWindowState();
        if (!tr.mActivities.isEmpty() && (this.mWindowManager instanceof HwWindowManagerService)) {
            hwRti.systemUiVisibility = ((HwWindowManagerService) this.mWindowManager).getWindowSystemUiVisibility(((ActivityRecord) tr.mActivities.get(0)).appToken);
        }
        return hwRti;
    }

    /* JADX WARNING: Missing block: B:13:0x001c, code:
            super.overridePendingTransition(r3, r4, r5, r6);
     */
    /* JADX WARNING: Missing block: B:14:0x001f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void overridePendingTransition(IBinder token, String packageName, int enterAnim, int exitAnim) {
        synchronized (this) {
            ActivityRecord self = ActivityRecord.isInStackLocked(token);
            if (self == null) {
            } else if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(self.getDisplayId())) {
            }
        }
    }

    public void moveTaskBackwards(int task) {
        boolean handled = false;
        if (HwPCUtils.isPcCastModeInServer()) {
            synchronized (this) {
                long origId = Binder.clearCallingIdentity();
                TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(task);
                if (tr != null && HwPCUtils.isExtDynamicStack(tr.getStackId())) {
                    handled = true;
                    tr.getStack().moveTaskToBackLocked(task);
                }
                Binder.restoreCallingIdentity(origId);
            }
        }
        if (!handled) {
            super.moveTaskBackwards(task);
        }
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

    public Bitmap getDisplayBitmap(int displayId, int width, int height) {
        if (this.mWindowManager instanceof HwWindowManagerService) {
            return ((HwWindowManagerService) this.mWindowManager).getDisplayBitmap(displayId, width, height);
        }
        return null;
    }

    protected boolean isTaskNotResizeableEx(TaskRecord task, Rect bounds) {
        return (isTaskSizeChange(task, bounds) && !HwPCMultiWindowCompatibility.isResizable(task.getWindowState())) || !(isTaskSizeChange(task, bounds) || HwPCMultiWindowCompatibility.isLayoutHadBounds(task.getWindowState()));
    }

    private boolean isTaskSizeChange(TaskRecord task, Rect rect) {
        return (task.getOverrideBounds().width() == rect.width() && task.getOverrideBounds().height() == rect.height()) ? false : true;
    }

    /* JADX WARNING: Missing block: B:10:0x001b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void toggleHome() {
        if (HwPCUtils.isPcCastModeInServer()) {
            synchronized (this) {
                long origId = Binder.clearCallingIdentity();
                try {
                    int displayId = HwPCUtils.getPCDisplayID();
                    if (HwPCUtils.isValidExtDisplayId(displayId)) {
                        ActivityDisplay activityDisplay = (ActivityDisplay) this.mStackSupervisor.mActivityDisplays.get(displayId);
                        if (activityDisplay == null) {
                            Binder.restoreCallingIdentity(origId);
                            return;
                        }
                        int stackNdx;
                        ActivityStack stack;
                        ArrayList<ActivityStack> stacks = new ArrayList();
                        int size = activityDisplay.getChildCount();
                        for (int i = 0; i < size; i++) {
                            stacks.add(activityDisplay.getChildAt(i));
                        }
                        boolean moveAllToBack = true;
                        for (stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                            stack = (ActivityStack) stacks.get(stackNdx);
                            if ((stack instanceof HwActivityStack) && ((HwActivityStack) stack).mHiddenFromHome) {
                                moveAllToBack = false;
                                break;
                            }
                        }
                        for (stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                            stack = (ActivityStack) stacks.get(stackNdx);
                            TaskRecord task = stack.topTask();
                            if (task != null) {
                                if (moveAllToBack) {
                                    if (stack.shouldBeVisible(null) && (stack instanceof HwActivityStack) && !((HwActivityStack) stack).mHiddenFromHome) {
                                        stack.moveTaskToBackLocked(task.taskId);
                                        ((HwActivityStack) stack).mHiddenFromHome = true;
                                    }
                                } else if (!stack.shouldBeVisible(null) && (stack instanceof HwActivityStack) && ((HwActivityStack) stack).mHiddenFromHome) {
                                    TaskRecord taskRecord = task;
                                    stack.moveTaskToFrontLocked(task, true, null, null, "moveToFrontFromHomeKey.");
                                    ((HwActivityStack) stack).mHiddenFromHome = false;
                                } else {
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

    protected boolean isMaximizedPortraitAppOnPCMode(ActivityRecord r) {
        if (!(!HwPCUtils.isPcCastModeInServer() || r.getStack() == null || r.info == null || r.info.getComponentName() == null || !HwPCUtils.isValidExtDisplayId(r.getStack().mDisplayId))) {
            if (HwPCMultiWindowManager.getInstance(this).mPortraitMaximizedPkgList.contains(r.info.getComponentName().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public void registerExternalPointerEventListener(PointerEventListener listener) {
        this.mWindowManager.registerExternalPointerEventListener(listener);
    }

    public void unregisterExternalPointerEventListener(PointerEventListener listener) {
        this.mWindowManager.unregisterExternalPointerEventListener(listener);
    }

    public void setPCScreenDpMode(int mode) {
        if (this.mWindowManager instanceof HwWindowManagerService) {
            ((HwWindowManagerService) this.mWindowManager).setPCScreenDisplayMode(mode);
        }
    }

    public int getPCScreenDisplayMode() {
        if (this.mWindowManager instanceof HwWindowManagerService) {
            return ((HwWindowManagerService) this.mWindowManager).getPCScreenDisplayMode();
        }
        return 0;
    }

    public boolean isPackageRunningOnPCMode(String packageName, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isPackageRunningOnPCMode, packageName:");
        stringBuilder.append(packageName);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        Slog.d(str, stringBuilder.toString());
        if (packageName == null) {
            Slog.e(TAG, "isPackageRunningOnPCMode packageName == null");
            return false;
        }
        synchronized (this) {
            ProcessRecord pr = (ProcessRecord) this.mProcessNames.get(packageName, uid);
            if (pr != null) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pr.mDisplayId:");
                stringBuilder2.append(pr.mDisplayId);
                Slog.d(str, stringBuilder2.toString());
                boolean isValidExtDisplayId = HwPCUtils.isValidExtDisplayId(pr.mDisplayId);
                return isValidExtDisplayId;
            }
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("no pr, packageName:");
            stringBuilder3.append(packageName);
            Slog.d(str2, stringBuilder3.toString());
            return false;
        }
    }

    public Bitmap getTaskThumbnailOnPCMode(int taskId) {
        Bitmap bitmap = null;
        synchronized (this) {
            TaskRecord tr = this.mStackSupervisor.anyTaskForIdLocked(taskId, 1);
            if (!(tr == null || tr.mStack == null || !(this.mWindowManager instanceof HwWindowManagerService))) {
                ActivityRecord r = tr.topRunningActivityLocked();
                if (r != null) {
                    bitmap = ((HwWindowManagerService) this.mWindowManager).getTaskSnapshotForPc(r.getDisplayId(), r.appToken, tr.taskId, tr.userId);
                }
            }
        }
        return bitmap;
    }

    protected final boolean checkUriPermissionLocked(GrantUri grantUri, int uid, int modeFlags) {
        boolean result = super.checkUriPermissionLocked(grantUri, uid, modeFlags);
        if (!(!IS_SUPPORT_CLONE_APP || result || grantUri == null || grantUri.uri == null || UserHandle.getUserId(uid) == this.mUserController.getCurrentUserIdLU() || !sAllowedCrossUserForCloneArrays.contains(grantUri.uri.getAuthority()))) {
            long ident = Binder.clearCallingIdentity();
            try {
                UserInfo ui = this.mUserController.getUserInfo(UserHandle.getUserId(uid));
                if (ui == null || !ui.isClonedProfile()) {
                    Binder.restoreCallingIdentity(ident);
                } else {
                    boolean checkUriPermissionLocked = super.checkUriPermissionLocked(grantUri, UserHandle.getUid(ui.profileGroupId, uid), modeFlags);
                    return checkUriPermissionLocked;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return result;
    }

    public int sendIntentSender(IIntentSender target, IBinder whitelistToken, int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        if (HwPCUtils.isPcCastModeInServer() && (target instanceof PendingIntentRecord)) {
            Key key = ((PendingIntentRecord) target).key;
            if (key != null) {
                int displayId = options != null ? ActivityOptions.fromBundle(options).getLaunchDisplayId() : 0;
                if (!HwPCUtils.enabledInPad() || !"com.android.incallui".equals(key.packageName) || isKeyguardLocked() || HwPCUtils.isValidExtDisplayId(displayId)) {
                    this.mPkgDisplayMaps.put(key.packageName, Integer.valueOf(displayId));
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendIntentSender skip when screen on, packageName: ");
                    stringBuilder.append(key.packageName);
                    stringBuilder.append(",isKeyguardLocked: ");
                    stringBuilder.append(isKeyguardLocked());
                    stringBuilder.append(",displayId: ");
                    stringBuilder.append(displayId);
                    Slog.d(str, stringBuilder.toString());
                    return 0;
                }
            }
        }
        return super.sendIntentSender(target, whitelistToken, code, intent, resolvedType, finishedReceiver, requiredPermission, options);
    }

    public boolean checkTaskId(int taskId) {
        ProcessRecord processRecord = (ProcessRecord) this.mPidsSelfLocked.get(Binder.getCallingPid());
        if (processRecord != null) {
            ArrayList<ActivityRecord> activityRecords = processRecord.activities;
            ArrayList taskIdContainer = new ArrayList();
            int recordsSize = activityRecords.size();
            for (int i = 0; i < recordsSize; i++) {
                TaskRecord task = ((ActivityRecord) activityRecords.get(i)).getTask();
                if (task != null) {
                    taskIdContainer.add(Integer.valueOf(task.taskId));
                }
            }
            if (taskIdContainer.contains(Integer.valueOf(taskId))) {
                return true;
            }
        }
        return false;
    }

    public void cleanAppForHiddenSpace() {
        if (MultiTaskManager.getInstance() != null) {
            MultiTaskManager.getInstance().getAppListForUserClean(new IAppCleanCallback.Stub() {
                public void onCleanFinish(AppCleanParam result) {
                    List<String> pkgNames = result.getStringList();
                    List<Integer> userIds = result.getIntList();
                    List<Integer> killTypes = result.getIntList2();
                    if (pkgNames != null && userIds != null && killTypes != null) {
                        if (pkgNames.size() <= 5) {
                            Slog.d(HwActivityManagerService.TAG, "less then 5 pkgs, abandon cleanAppForHiddenSpace");
                            return;
                        }
                        List<AppCleanInfo> appCleanInfoList = new ArrayList();
                        for (int i = 0; i < pkgNames.size(); i++) {
                            appCleanInfoList.add(new AppCleanInfo((String) pkgNames.get(i), (Integer) userIds.get(i), (Integer) killTypes.get(i)));
                        }
                        IAppCleanCallback callback2 = new IAppCleanCallback.Stub() {
                            public void onCleanFinish(AppCleanParam result2) {
                            }
                        };
                        Slog.d(HwActivityManagerService.TAG, "executeMultiAppClean for hidden space");
                        MultiTaskManager.getInstance().executeMultiAppClean(appCleanInfoList, callback2);
                    }
                }
            });
        }
    }

    public boolean addGameSpacePackageList(List<String> packageList) {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.addGameSpacePackageList(packageList);
        }
        return false;
    }

    public boolean delGameSpacePackageList(List<String> packageList) {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.delGameSpacePackageList(packageList);
        }
        return false;
    }

    public boolean isInGameSpace(String packageName) {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.isInGameSpace(packageName);
        }
        return false;
    }

    public List<String> getGameList() {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.getGameList();
        }
        return null;
    }

    public void registerGameObserver(IGameObserver observer) {
        if (this.mHwGameAssistantController != null) {
            this.mHwGameAssistantController.registerGameObserver(observer);
        }
    }

    public void unregisterGameObserver(IGameObserver observer) {
        if (this.mHwGameAssistantController != null) {
            this.mHwGameAssistantController.unregisterGameObserver(observer);
        }
    }

    public boolean isGameDndOn() {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.isGameDndOn();
        }
        return false;
    }

    public boolean isGameKeyControlOn() {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.isGameKeyControlOn();
        }
        return false;
    }

    public boolean isGameGestureDisabled() {
        if (this.mHwGameAssistantController != null) {
            return this.mHwGameAssistantController.isGameGestureDisabled();
        }
        return false;
    }
}
