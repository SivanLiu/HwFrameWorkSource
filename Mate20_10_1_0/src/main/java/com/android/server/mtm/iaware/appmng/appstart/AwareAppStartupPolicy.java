package com.android.server.mtm.iaware.appmng.appstart;

import android.app.mtm.iaware.HwAppStartupSetting;
import android.app.mtm.iaware.HwAppStartupSettingFilter;
import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.webkit.WebViewZygote;
import com.android.server.am.HwActivityManagerService;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupInfo;
import com.android.server.mtm.iaware.appmng.policy.AppStartPolicy;
import com.android.server.mtm.iaware.appmng.policy.Policy;
import com.android.server.rms.dualfwk.AwareMiddleware;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareAppStartStatusCache;
import com.android.server.rms.iaware.appmng.AwareAppStartStatusCacheExt;
import com.android.server.rms.iaware.appmng.AwareComponentPreloadManager;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.srms.AppStartupFeature;
import com.android.server.rms.iaware.srms.BroadcastExFeature;
import com.android.server.rms.iaware.srms.SRMSDumpRadar;
import com.android.server.wm.WindowProcessController;
import com.huawei.android.app.HwActivityManager;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class AwareAppStartupPolicy {
    private static final String ACTION_APPSTARTUP_CHANGED = "com.huawei.android.APPSTARTUP_CHANGED";
    private static final String ACTION_APPSTARTUP_RECORD = "com.huawei.android.hsm.APPSTARTUP_RECORD";
    private static final int ARGS_CMD = 2;
    private static final int ARGS_PARAM = 3;
    private static final String BAD_COMMAND_INFO = "Bad command";
    private static final long DELAY_MILLS_NOTIFY_PG = 5000;
    private static final long DELAY_MILLS_REMOVE_RESTART_DATA = 1200000;
    private static final long DELAY_PRINT_LOG = 1000;
    private static final int HASHCODE_INIT_VALUE = 17;
    private static final int HASHCODE_MULTI_VALUE = 31;
    private static final String HSM_PACKAGE_NAME = "com.huawei.systemmanager";
    private static final String IGNORE = "NA";
    private static final String LOG_OFF_PATAM = "0";
    private static final String LOG_ON_PATAM = "1";
    private static final int MAX_LOG_MERGER = 10;
    private static final int MIN_CMD_LEN = 3;
    private static boolean MM_HOTA_AUTOSTART_HAS_DONE = false;
    private static final int MSG_CODE_INIT_STARTUP_SETTING = 5;
    private static final int MSG_CODE_NOTIFY_CHANGED_TO_PG = 4;
    private static final int MSG_CODE_NOTIFY_PROCESS_START = 6;
    private static final int MSG_CODE_POLICY_INIT = 1;
    private static final int MSG_CODE_REMOVE_RESTART_DATA = 7;
    private static final int MSG_CODE_REPORT_RECORD_TO_HSM = 3;
    private static final int MSG_CODE_REPORT_RECORD_TO_LOG = 8;
    private static final int MSG_CODE_WRITE_STARTUP_SETTING = 2;
    private static final String PG_PACKAGE_NAME = "com.huawei.powergenie";
    private static final String PKG_PREFIX = "com.";
    private static final Object POLICY_LOCK = new Object();
    private static final String PREVENT_SEPERATE = "#";
    private static final int REPORT_HSM_INTERVAL = 30000;
    private static final int REPORT_HSM_MAX = 30;
    private static final StringBuilder REQ_LOG = new StringBuilder(256);
    private static final StringBuilder REQ_LOG_MERGE = new StringBuilder(256);
    private static final String STR_ONLY_DIFF = "2";
    private static final String TAG = "AwareAppStartupPolicy";
    private static final String TAG_SIMPLE = "AppStart";
    private static final Set<String> WEARCALLER_ALLOWPKGS = new ArraySet();
    private static AwareAppStartupPolicy sInstance = null;
    private final RestartData mAllowData = new RestartData(null, null, 0, 0, null);
    private Context mContext = null;
    private AppStartupDataMgr mDataMgr = new AppStartupDataMgr();
    private boolean mDebugCost = false;
    private boolean mDebugDetail = false;
    private int mForegroundAppLevel = 2;
    private int mForegroundSleepAppLevel = 7;
    private Handler mHandler = null;
    private HandlerThread mHandlerThread = null;
    private final ArrayMap<HsmRecord, Integer> mHsmRecordList = new ArrayMap<>();
    private AtomicBoolean mInitFinished = new AtomicBoolean(false);
    private boolean mIsAbroadArea = false;
    private boolean mIsUpgrade = false;
    private long mLastRemoveMsgTime = 0;
    private final ArrayMap<AppStartLogRecord, ArraySet<String>> mLogRecordMap = new ArrayMap<>();
    private AtomicBoolean mPolicyInited = new AtomicBoolean(false);
    private final ArrayMap<String, RestartData> mRestartDataList = new ArrayMap<>();
    private boolean mSubUserAppCtrl = true;

    /* access modifiers changed from: private */
    public enum CacheType {
        STARTUP_SETTING
    }

    static {
        WEARCALLER_ALLOWPKGS.add("com.huawei.iconnect");
    }

    private static class RestartData {
        /* access modifiers changed from: private */
        public int alwType;
        private boolean dirty;
        /* access modifiers changed from: private */
        public String pkg;
        /* access modifiers changed from: private */
        public int policyType;
        /* access modifiers changed from: private */
        public String reason;
        /* access modifiers changed from: private */
        public long timeStamp;

        /* synthetic */ RestartData(String x0, String x1, int x2, int x3, AnonymousClass1 x4) {
            this(x0, x1, x2, x3);
        }

        private RestartData(String pkgName, String rsn, int allowtype, int policy) {
            this.pkg = pkgName;
            this.reason = rsn;
            this.alwType = allowtype;
            this.policyType = policy;
            this.timeStamp = SystemClock.elapsedRealtime();
            this.dirty = true;
        }

        public String toString() {
            return "RestartData: {" + this.pkg + " reason:" + this.reason + " policy:" + this.policyType + "}";
        }

        public void setDirty(boolean dirty2) {
            this.dirty = dirty2;
        }

        public boolean getDirty() {
            return this.dirty;
        }

        public void set(String reason2, int alwType2, int policyType2) {
            this.reason = reason2;
            this.alwType = alwType2;
            this.policyType = policyType2;
        }
    }

    private static class HsmRecord {
        /* access modifiers changed from: private */
        public boolean autoStart;
        /* access modifiers changed from: private */
        public String callerType;
        /* access modifiers changed from: private */
        public int callerUid;
        /* access modifiers changed from: private */
        public String pkg;
        /* access modifiers changed from: private */
        public boolean result;
        /* access modifiers changed from: private */
        public long timeStamp;

        /* synthetic */ HsmRecord(String x0, String x1, int x2, boolean x3, boolean x4, AnonymousClass1 x5) {
            this(x0, x1, x2, x3, x4);
        }

        private HsmRecord(String pkgName, String type, int uid, boolean res, boolean selfStart) {
            this.pkg = pkgName;
            this.callerType = type;
            this.callerUid = uid;
            this.result = res;
            this.autoStart = selfStart;
            this.timeStamp = System.currentTimeMillis();
        }

        public int hashCode() {
            String str = this.pkg;
            int i = 0;
            int hashCode = str != null ? str.hashCode() : 0;
            String str2 = this.callerType;
            if (str2 != null) {
                i = str2.hashCode();
            }
            return hashCode + i + (this.callerUid * ((1 << (this.result ? 1 : 0)) + (this.autoStart ? 1 : 0)));
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof HsmRecord)) {
                return false;
            }
            HsmRecord record = (HsmRecord) obj;
            if (Objects.equals(this.pkg, record.pkg) && Objects.equals(this.callerType, record.callerType) && this.callerUid == record.callerUid && this.result == record.result && this.autoStart == record.autoStart) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "HsmRecord: {" + this.pkg + " type:" + this.callerType + " uid:" + this.callerUid + " res:" + this.result + " auto:" + this.autoStart + " time:" + this.timeStamp + '}';
        }
    }

    private static class AppStartLogRecord {
        public String action;
        public String alw;
        public String caller;
        public int callerUid;
        public AppMngConstant.AppStartSource req;

        /* synthetic */ AppStartLogRecord(AppMngConstant.AppStartSource x0, String x1, int x2, String x3, String x4, AnonymousClass1 x5) {
            this(x0, x1, x2, x3, x4);
        }

        private AppStartLogRecord(AppMngConstant.AppStartSource requestSource, String alwDetail, int callerUid2, String callerApp, String action2) {
            this.req = requestSource;
            this.alw = alwDetail;
            this.callerUid = callerUid2;
            this.caller = callerApp;
            this.action = action2;
        }

        public int hashCode() {
            return (((((((((17 * 31) + this.req.ordinal()) * 31) + this.alw.hashCode()) * 31) + this.callerUid) * 31) + this.caller.hashCode()) * 31) + this.action.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof AppStartLogRecord)) {
                return false;
            }
            AppStartLogRecord record = (AppStartLogRecord) obj;
            if (!this.req.equals(record.req) || !this.alw.equals(record.alw) || this.callerUid != record.callerUid || !this.caller.equals(record.caller) || !this.action.equals(record.action)) {
                return false;
            }
            return true;
        }
    }

    private class ServiceReqInfo {
        public int callerUid;
        public int hwFlag;
        public boolean isSrvBind;
        public ServiceInfo servInfo;
        public Intent service;
        public int unPercetibleAlarm;

        private ServiceReqInfo() {
        }

        /* synthetic */ ServiceReqInfo(AwareAppStartupPolicy x0, AnonymousClass1 x1) {
            this();
        }
    }

    private AwareAppStartupPolicy(Context context, HandlerThread mtmThread) {
        this.mContext = context;
        if (AppStartupFeature.isAppStartupEnabled()) {
            init();
            return;
        }
        this.mHandler = new StartupPolicyHandler(mtmThread.getLooper());
        this.mHandler.sendEmptyMessage(5);
    }

    public static AwareAppStartupPolicy getInstance(Context context, HandlerThread mtmThread) {
        AwareAppStartupPolicy awareAppStartupPolicy;
        synchronized (POLICY_LOCK) {
            if (!(sInstance != null || context == null || mtmThread == null)) {
                sInstance = new AwareAppStartupPolicy(context, mtmThread);
            }
            awareAppStartupPolicy = sInstance;
        }
        return awareAppStartupPolicy;
    }

    public static AwareAppStartupPolicy self() {
        AwareAppStartupPolicy awareAppStartupPolicy;
        synchronized (POLICY_LOCK) {
            awareAppStartupPolicy = sInstance;
        }
        return awareAppStartupPolicy;
    }

    private void init() {
        if (this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread(TAG);
            this.mHandlerThread.start();
            this.mHandler = new StartupPolicyHandler(this.mHandlerThread.getLooper());
        }
        this.mHandler.sendEmptyMessage(1);
    }

    private void unInit() {
        if (this.mPolicyInited.get()) {
            setInitState(false);
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(3);
            AwareIntelligentRecg.getInstance().appStartDisable();
            this.mDataMgr.unInitData();
        }
    }

    private void setInitState(boolean isFinished) {
        this.mPolicyInited.set(isFinished);
        if (isFinished) {
            this.mInitFinished.set(true);
        }
    }

    private void setAppStartupSyncFlag(boolean needSync) {
        SystemProperties.set("persist.sys.appstart.sync", needSync ? "true" : "false");
        if (this.mDebugDetail) {
            AwareLog.e(TAG, "setAppStartupSyncFlag needSync=" + needSync);
        }
    }

    /* access modifiers changed from: private */
    public void policyInit() {
        long start = System.nanoTime();
        policyInitInner();
        AwareLog.i(TAG, "policyInit finished, cost=" + ((System.nanoTime() - start) / 1000));
    }

    private void policyInitInner() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm != null && pm.isUpgrade()) {
            this.mIsUpgrade = true;
        }
        AwareLog.i(TAG, "policyInit mIsUpgrade=" + this.mIsUpgrade);
        this.mIsAbroadArea = AwareDefaultConfigList.isAbroadArea();
        DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.COMMON, this.mContext);
        DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.APP_START, this.mContext);
        this.mDataMgr.initData(this.mContext);
        AwareIntelligentRecg.getInstance().appStartEnable(this.mDataMgr, this.mContext);
        setInitState(true);
    }

    /* access modifiers changed from: private */
    public void initStartupSetting() {
        long start = System.nanoTime();
        this.mDataMgr.initStartupSetting();
        setInitState(true);
        AwareLog.i(TAG, "initStartupSetting finished, cost=" + ((System.nanoTime() - start) / 1000));
    }

    public void initSystemUidCache() {
        this.mDataMgr.initSystemUidCache(this.mContext);
    }

    private boolean isPolicyEnabled() {
        return this.mPolicyInited.get() && AppStartupFeature.isAppStartupEnabled();
    }

    private boolean isSubUser() {
        return AwareAppAssociate.getInstance().getCurSwitchUser() != 0;
    }

    private boolean isSubUserAppControl(int uid, AppMngConstant.AppStartSource requestSource) {
        boolean subUser = isSubUser();
        if (!this.mSubUserAppCtrl) {
            return subUser;
        }
        if (!subUser) {
            return false;
        }
        if (AppMngConstant.AppStartSource.SCHEDULE_RESTART.equals(requestSource)) {
            return true;
        }
        if (UserHandle.getUserId(uid) != 0) {
            return true;
        }
        return false;
    }

    private class StartupPolicyHandler extends Handler {
        public StartupPolicyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AwareAppStartupPolicy.this.policyInit();
                    return;
                case 2:
                    AwareAppStartupPolicy.this.writeCacheFile(CacheType.STARTUP_SETTING);
                    return;
                case 3:
                    AwareAppStartupPolicy.this.startRecordService();
                    return;
                case 4:
                    AwareAppStartupPolicy.this.startNotifyChangedService();
                    return;
                case 5:
                    AwareAppStartupPolicy.this.initStartupSetting();
                    return;
                case 6:
                    AwareAppStartupPolicy.this.reportProcessStart(msg);
                    return;
                case 7:
                    AwareAppStartupPolicy.this.periodRemoveRestartData();
                    return;
                case 8:
                    AwareAppStartupPolicy.this.printMergeReqLog();
                    return;
                default:
                    return;
            }
        }
    }

    public HwAppStartupSetting getAppStartupSetting(String pkgName) {
        if (!this.mInitFinished.get()) {
            return null;
        }
        return this.mDataMgr.getAppStartupSetting(pkgName);
    }

    public List<HwAppStartupSetting> retrieveAppStartupSettings(List<String> pkgList, HwAppStartupSettingFilter filter) {
        if (!this.mInitFinished.get()) {
            return null;
        }
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "retrieveAppStartupSettings pkgList=" + pkgList + ", filter=" + filter);
        }
        return this.mDataMgr.retrieveAppStartupSettings(pkgList, filter);
    }

    public List<String> retrieveAppStartupPackages(List<String> pkgList, int[] policy, int[] modifier, int[] show) {
        if (!this.mInitFinished.get()) {
            return null;
        }
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "retrieveAppStartupPackages pkgList=" + pkgList + ", policy=" + Arrays.toString(policy) + ", modifier=" + Arrays.toString(modifier) + ", show=" + Arrays.toString(show));
        }
        List<HwAppStartupSetting> settingList = this.mDataMgr.retrieveAppStartupSettings(pkgList, new HwAppStartupSettingFilter().setPolicy(policy).setShow(show).setModifier(modifier));
        List<String> list = new ArrayList<>();
        for (HwAppStartupSetting item : settingList) {
            if (item != null) {
                list.add(item.getPackageName());
            }
        }
        return list;
    }

    public boolean updateAppStartupSettings(List<HwAppStartupSetting> settingList, boolean clearFirst) {
        if (!this.mInitFinished.get()) {
            AwareLog.i(TAG, "updateAppStartupSettings policy is not finish init");
            return false;
        }
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "updateAppStartupSettings clearFirst=" + clearFirst + ", settingList=" + settingList);
        }
        if (!this.mDataMgr.updateAppStartupSettings(settingList, clearFirst)) {
            return false;
        }
        scheduleFastWriteCache(CacheType.STARTUP_SETTING);
        sendAppStartupChangedMsg();
        return true;
    }

    public boolean removeAppStartupSetting(String pkgName) {
        if (!this.mInitFinished.get()) {
            return false;
        }
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "removeAppStartupSetting pkgName=" + pkgName);
        }
        if (!this.mDataMgr.removeAppStartupSetting(pkgName)) {
            return false;
        }
        scheduleFastWriteCache(CacheType.STARTUP_SETTING);
        sendAppStartupChangedMsg();
        return true;
    }

    public boolean updateAppMngConfig() {
        if (!this.mInitFinished.get()) {
            return false;
        }
        AwareLog.i(TAG, "updateAppMngConfig");
        DecisionMaker.getInstance().updateRule(null, this.mContext);
        AwareIntelligentRecg.getInstance().updateAppMngConfig();
        BroadcastExFeature.updateConfig();
        if (!isPolicyEnabled()) {
            return true;
        }
        this.mDataMgr.updateAppMngConfig();
        return true;
    }

    public void notifyProcessStart(String pkgName, String process, String hostingType, int pid, int uid) {
        AwareIntelligentRecg.getInstance().setHwStopFlag(UserHandle.getUserId(uid), pkgName, false);
        if (!isPolicyEnabled() || isSubUser()) {
            return;
        }
        if ((AppStartupFeature.isBetaUser() || !this.mDataMgr.isSpecialCaller(uid)) && !this.mIsAbroadArea) {
            Bundle bundle = new Bundle();
            bundle.putString("PACKAGE", pkgName);
            bundle.putString("HOST_TYPE", hostingType);
            Message msg = this.mHandler.obtainMessage(6);
            msg.setData(bundle);
            this.mHandler.sendMessage(msg);
        }
    }

    public int getBigdataThreshold(boolean beta) {
        return this.mDataMgr.getBigdataThreshold(beta);
    }

    public void setEnable(boolean enable) {
        if (enable) {
            init();
        } else {
            unInit();
        }
    }

    private AppMngConstant.AppStartSource getReceiverReqSource(boolean isSpecCaller, boolean isAlarmFlag, int callerUid, ApplicationInfo applicationInfo, int unPercetibleAlarm) {
        if (isSpecCaller) {
            return isAlarmFlag ? AppMngConstant.AppStartSource.ALARM : AppMngConstant.AppStartSource.SYSTEM_BROADCAST;
        }
        if (!isAlarmFlag) {
            return AppMngConstant.AppStartSource.THIRD_BROADCAST;
        }
        if (callerUid == applicationInfo.uid) {
            return AppMngConstant.AppStartSource.ALARM;
        }
        if (this.mDataMgr.isSystemBaseApp(applicationInfo)) {
            return AppMngConstant.AppStartSource.ALARM;
        }
        if (unPercetibleAlarm != 0) {
            return AppMngConstant.AppStartSource.ALARM;
        }
        return AppMngConstant.AppStartSource.THIRD_BROADCAST;
    }

    private AppMngConstant.AppStartSource getServiceReqSource(ServiceReqInfo serviceReqInfo) {
        AppMngConstant.AppStartSource requestResource = AppMngConstant.AppStartSource.BIND_SERVICE;
        int hwFlag = serviceReqInfo.hwFlag;
        Intent service = serviceReqInfo.service;
        if ((hwFlag & 32) != 0 && service != null) {
            AppMngConstant.AppStartSource requestResource2 = AppMngConstant.AppStartSource.JOB_SCHEDULE;
            service.setHwFlags((hwFlag & -33) | 8192);
            return requestResource2;
        } else if ((hwFlag & 64) != 0) {
            return AppMngConstant.AppStartSource.ACCOUNT_SYNC;
        } else {
            if (serviceReqInfo.isSrvBind) {
                return requestResource;
            }
            if ((hwFlag & 256) == 0) {
                return AppMngConstant.AppStartSource.START_SERVICE;
            }
            if (serviceReqInfo.servInfo.applicationInfo.uid == serviceReqInfo.callerUid) {
                return AppMngConstant.AppStartSource.ALARM;
            }
            if (serviceReqInfo.unPercetibleAlarm != 0) {
                return AppMngConstant.AppStartSource.ALARM;
            }
            return AppMngConstant.AppStartSource.START_SERVICE;
        }
    }

    private AppMngConstant.AppStartSource getActivityReqSource(int uid, int callerUid, int hwFlag) {
        AppMngConstant.AppStartSource requestResource = AppMngConstant.AppStartSource.THIRD_ACTIVITY;
        if ((hwFlag & 256) == 0) {
            return requestResource;
        }
        if (uid == callerUid) {
            return AppMngConstant.AppStartSource.ALARM;
        }
        if ((hwFlag & 2048) == 0) {
            return AppMngConstant.AppStartSource.ALARM;
        }
        return requestResource;
    }

    public boolean shouldPreventSendReceiver(AppStartupInfo appStartupInfo, ResolveInfo resolveInfo) {
        long start;
        if (appStartupInfo == null || resolveInfo == null || !isPolicyEnabled()) {
            return false;
        }
        Intent intent = appStartupInfo.intent;
        int callPid = appStartupInfo.callerPid;
        int callerUid = appStartupInfo.callerUid;
        WindowProcessController targetApp = appStartupInfo.targetApp;
        WindowProcessController callerApp = appStartupInfo.callerApp;
        if (intent == null) {
            return false;
        }
        if (this.mDebugCost) {
            start = System.nanoTime();
        } else {
            start = 0;
        }
        int callerPid = callerApp != null ? callerApp.mPid : callPid;
        String action = intent.getAction();
        boolean isSpecCaller = this.mDataMgr.isSpecialCaller(callerUid);
        int hwFlag = intent.getHwFlags();
        boolean isAlarmFlag = (hwFlag & 256) != 0;
        int unPercetibleAlarm = getUnPercetibleAlarm(isAlarmFlag, hwFlag, callerUid, action);
        ApplicationInfo ai = resolveInfo.activityInfo.applicationInfo;
        appStartupInfo.setProcName(resolveInfo.activityInfo.processName).setApplicationInfo(ai).setCallerPid(callerPid).setCallerUid(callerUid).setTargetApp(targetApp).setCallerApp(callerApp).setRequestSource(getReceiverReqSource(isSpecCaller, isAlarmFlag, callerUid, ai, unPercetibleAlarm)).setCompName(resolveInfo.activityInfo.getComponentName().flattenToShortString()).setAction(action).setUnPercetibleAlarm(unPercetibleAlarm).setFromRecent(false).setIntent(null);
        int pkgAlwType = getPackageAllowType(appStartupInfo);
        if (this.mDebugCost) {
            AwareLog.i(TAG, "shouldPreventSendReceiver cost=" + ((System.nanoTime() - start) / 1000));
        }
        if (pkgAlwType <= 0) {
            return true;
        }
        return false;
    }

    private int getUnPercetibleAlarm(boolean isAlarmFlag, int hwFlag, int callerUid, String action) {
        if (!isAlarmFlag) {
            return 2;
        }
        if ((hwFlag & 512) != 0) {
            return 1;
        }
        if ((hwFlag & 2048) != 0) {
            return 0;
        }
        if (callerUid != 1000 || !"android.intent.action.DATE_CHANGED".equals(action)) {
            return 2;
        }
        return 1;
    }

    public boolean shouldPreventStartService(AppStartupInfo appStartupInfo, ServiceInfo servInfo, int servFlag, boolean servExist) {
        if (servInfo == null || !isPolicyEnabled() || appStartupInfo == null) {
            return false;
        }
        int callerUid = appStartupInfo.callerUid;
        Intent service = appStartupInfo.intent;
        long start = 0;
        if (this.mDebugCost) {
            start = System.nanoTime();
        }
        boolean isSrvBind = servFlag == 2;
        if (servFlag != 0) {
            if (!isSrvBind || !servExist) {
                int hwFlag = 0;
                String action = null;
                if (service != null) {
                    hwFlag = service.getHwFlags();
                    action = service.getAction();
                }
                int unPercetibleAlarm = getUnPercetibleAlarmByHwFlag(hwFlag);
                ServiceReqInfo serviceReqInfo = new ServiceReqInfo(this, null);
                serviceReqInfo.unPercetibleAlarm = unPercetibleAlarm;
                serviceReqInfo.servInfo = servInfo;
                serviceReqInfo.service = service;
                serviceReqInfo.isSrvBind = isSrvBind;
                serviceReqInfo.hwFlag = hwFlag;
                serviceReqInfo.callerUid = callerUid;
                AppMngConstant.AppStartSource requestResource = getServiceReqSource(serviceReqInfo);
                if (isUserSync(requestResource, hwFlag)) {
                    return false;
                }
                appStartupInfo.setProcName(servInfo.processName).setApplicationInfo(servInfo.applicationInfo).setCallerUid(callerUid).setTargetApp(null).setRequestSource(requestResource).setCompName(servInfo.getComponentName().flattenToShortString()).setAction(action).setUnPercetibleAlarm(unPercetibleAlarm).setFromRecent(false).setIntent(service);
                int pkgAlwType = getPackageAllowType(appStartupInfo);
                if (this.mDebugCost) {
                    AwareLog.i(TAG, "shouldPreventStartService cost=" + ((System.nanoTime() - start) / 1000));
                }
                reportToAwareCompPreMgr(pkgAlwType, servInfo.getComponentName(), appStartupInfo.callerApp);
                return pkgAlwType <= 0;
            }
        }
        if (!this.mDebugDetail) {
            return false;
        }
        AwareLog.i(TAG, "shouldPreventStartService pkg=" + servInfo.applicationInfo.packageName + ", comp=" + servInfo.name + ", callerPid=" + appStartupInfo.callerPid + ", callerUid=" + callerUid + ", servFlag=" + servFlag + ", servExist=" + servExist);
        return false;
    }

    private void reportToAwareCompPreMgr(int pkgAlwType, ComponentName comp, WindowProcessController callerApp) {
        if (pkgAlwType > 0 && comp != null && callerApp != null) {
            AwareComponentPreloadManager.getInstance().reportServiceStart(comp.getPackageName(), comp.getClassName(), callerApp.mName, callerApp.mUid);
        }
    }

    private int getUnPercetibleAlarmByHwFlag(int hwFlag) {
        if ((hwFlag & 256) == 0 || (hwFlag & 2048) == 0) {
            return 2;
        }
        return 0;
    }

    private boolean isUserSync(AppMngConstant.AppStartSource requestResource, int hwFlag) {
        if (!AppMngConstant.AppStartSource.ACCOUNT_SYNC.equals(requestResource) || (hwFlag & 128) == 0) {
            return false;
        }
        if (!this.mDebugDetail) {
            return true;
        }
        AwareLog.i(TAG, "we donnot forbiden sync when user initiated");
        return true;
    }

    public boolean shouldPreventStartActivity(Intent intent, ActivityInfo activityInfo, int callerPid, int callerUid, WindowProcessController callerApp) {
        boolean shouldPrevent = false;
        if (activityInfo != null) {
            if (intent != null) {
                if (!isPolicyEnabled()) {
                    return false;
                }
                long start = 0;
                if (this.mDebugCost) {
                    start = System.nanoTime();
                }
                AppMngConstant.AppStartSource requestResource = getActivityReqSource(activityInfo.applicationInfo.uid, callerUid, intent.getHwFlags());
                boolean fromRecent = false;
                if ((intent.getFlags() & HighBitsCompModeID.MODE_COLOR_ENHANCE) != 0) {
                    fromRecent = true;
                }
                String compName = activityInfo.getComponentName().flattenToShortString();
                AppStartupInfo appStartupInfo = new AppStartupInfo();
                appStartupInfo.setProcName(activityInfo.processName).setApplicationInfo(activityInfo.applicationInfo).setCallerPid(callerPid).setCallerUid(callerUid).setTargetApp(null).setCallerApp(callerApp).setRequestSource(requestResource).setCompName(compName).setAction(null).setUnPercetibleAlarm(2).setFromRecent(fromRecent).setIntent(null);
                if (getPackageAllowType(appStartupInfo) <= 0) {
                    shouldPrevent = true;
                }
                if (this.mDebugCost) {
                    AwareLog.i(TAG, "shouldPreventStartActivity cost=" + ((System.nanoTime() - start) / 1000));
                }
                return shouldPrevent;
            }
        }
        return false;
    }

    public boolean shouldPreventStartProvider(ProviderInfo cpi, int callerPid, int callerUid, WindowProcessController callerApp) {
        if (cpi == null || !isPolicyEnabled()) {
            return false;
        }
        long start = 0;
        if (this.mDebugCost) {
            start = System.nanoTime();
        }
        String compName = cpi.getComponentName().flattenToShortString();
        AppStartupInfo appStartupInfo = new AppStartupInfo();
        appStartupInfo.setProcName(cpi.processName).setApplicationInfo(cpi.applicationInfo).setCallerPid(callerPid).setCallerUid(callerUid).setTargetApp(null).setCallerApp(callerApp).setRequestSource(AppMngConstant.AppStartSource.PROVIDER).setCompName(compName).setAction(null).setUnPercetibleAlarm(2).setFromRecent(false).setIntent(null);
        int pkgAlwType = getPackageAllowType(appStartupInfo);
        if (this.mDebugCost) {
            AwareLog.i(TAG, "shouldPreventStartProvider cost=" + ((System.nanoTime() - start) / 1000));
        }
        if (pkgAlwType <= 0) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Multiple debug info for r4v3 android.app.mtm.iaware.appmng.AppMngConstant$AppStartSource: [D('key' java.lang.String), D('requestSource' android.app.mtm.iaware.appmng.AppMngConstant$AppStartSource)] */
    public boolean shouldPreventRestartService(ServiceInfo serInfo, boolean realStart) {
        int pkgAlwType;
        RestartData restartData;
        if (serInfo == null || !isPolicyEnabled()) {
            return false;
        }
        long start = 0;
        if (this.mDebugCost) {
            start = System.nanoTime();
        }
        if (realStart) {
            String key = serInfo.applicationInfo.packageName + "#" + serInfo.processName;
            synchronized (this.mRestartDataList) {
                restartData = this.mRestartDataList.remove(key);
            }
            if (restartData != null) {
                if (!this.mIsAbroadArea && !isSubUser()) {
                    updateAllowedBigData(restartData.pkg, restartData.reason, restartData.alwType, restartData.policyType, true);
                }
                pkgAlwType = restartData.alwType;
            } else {
                pkgAlwType = 4;
            }
            AwareLog.i(TAG_SIMPLE, "shouldPreventRestartService remove " + key + " " + restartData);
        } else {
            AppMngConstant.AppStartSource requestSource = AppMngConstant.AppStartSource.SCHEDULE_RESTART;
            int callerPid = Binder.getCallingPid();
            int callerUid = Binder.getCallingUid();
            String compName = serInfo.getComponentName().flattenToShortString();
            AppStartupInfo appStartupInfo = new AppStartupInfo();
            appStartupInfo.setProcName(serInfo.processName).setApplicationInfo(serInfo.applicationInfo).setCallerPid(callerPid).setCallerUid(callerUid).setTargetApp(null).setCallerApp(null).setRequestSource(requestSource).setCompName(compName).setAction(null).setUnPercetibleAlarm(2).setFromRecent(false).setIntent(null);
            pkgAlwType = getPackageAllowType(appStartupInfo);
        }
        if (this.mDebugCost) {
            AwareLog.i(TAG, "shouldPreventRestartService cost=" + ((System.nanoTime() - start) / 1000));
        }
        if (pkgAlwType <= 0) {
            return true;
        }
        return false;
    }

    private boolean isForbidApp(String pkg, AppMngConstant.AppStartSource requestSource, boolean isAppStop, WindowProcessController callerApp) {
        if (!this.mDataMgr.isPgCleanApp(pkg)) {
            return false;
        }
        if (AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[requestSource.ordinal()] == 1) {
            this.mDataMgr.removePgCleanApp(pkg);
            return false;
        } else if (isAppStop && !isWearcallerAllow(callerApp)) {
            return true;
        } else {
            return false;
        }
    }

    /* renamed from: com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource = new int[AppMngConstant.AppStartSource.values().length];

        static {
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.THIRD_ACTIVITY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.ALARM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.SYSTEM_BROADCAST.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.JOB_SCHEDULE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.ACCOUNT_SYNC.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.SCHEDULE_RESTART.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.BIND_SERVICE.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.START_SERVICE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.PROVIDER.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[AppMngConstant.AppStartSource.THIRD_BROADCAST.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
        }
    }

    private boolean isWearcallerAllow(WindowProcessController callerApp) {
        if (callerApp == null || callerApp.mPkgList == null || callerApp.mPkgList.isEmpty() || callerApp.mPkgList.valueAt(0) == null) {
            return false;
        }
        return WEARCALLER_ALLOWPKGS.contains(callerApp.mPkgList.valueAt(0));
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x003b, code lost:
        r3 = 0;
        r4 = r6.size();
        r5 = com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy.REQ_LOG_MERGE;
        r6 = r6.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x004a, code lost:
        if (r6.hasNext() == false) goto L_0x000a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x004c, code lost:
        r7 = r6.next();
        r8 = r3 % 10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0056, code lost:
        if (r8 != 0) goto L_0x00a5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0058, code lost:
        r5.setLength(0);
        r5.append("req:");
        r5.append(r6.req.getDesc());
        r5.append(',');
        r5.append("alw:");
        r5.append(r6.alw);
        r5.append(',');
        r5.append("call:");
        r5.append(r6.callerUid);
        r5.append(',');
        r5.append(r6.caller);
        r5.append(',');
        r5.append("act:");
        r5.append(r6.action);
        r5.append(' ');
        r5.append("{");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x00a5, code lost:
        if (r7 == null) goto L_0x00b3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x00ad, code lost:
        if (r7.startsWith(com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy.PKG_PREFIX) == false) goto L_0x00b3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x00af, code lost:
        r7 = r7.substring(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x00b5, code lost:
        if (r8 == 9) goto L_0x00c3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x00b9, code lost:
        if (r3 != (r4 - 1)) goto L_0x00bc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x00bc, code lost:
        r5.append(r7);
        r5.append(',');
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x00c3, code lost:
        r5.append(r7);
        r5.append("}");
        android.rms.iaware.AwareLog.i(com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy.TAG_SIMPLE, r5.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x00d5, code lost:
        r3 = r3 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0036, code lost:
        if (r6 == null) goto L_0x000a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0038, code lost:
        if (r6 != null) goto L_0x003b;
     */
    public void printMergeReqLog() {
        int pkgPrefixStart = PKG_PREFIX.length() - 1;
        while (true) {
            synchronized (this.mLogRecordMap) {
                Iterator<Map.Entry<AppStartLogRecord, ArraySet<String>>> it = this.mLogRecordMap.entrySet().iterator();
                if (it.hasNext()) {
                    Map.Entry<AppStartLogRecord, ArraySet<String>> entry = it.next();
                    AppStartLogRecord logRecord = entry.getKey();
                    ArraySet<String> pkgSet = entry.getValue();
                    it.remove();
                } else {
                    return;
                }
            }
        }
    }

    private boolean isMergeReqLog(AppMngConstant.AppStartSource requestSource, int alwType) {
        int i;
        if (this.mDebugDetail || alwType > 0 || (i = AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[requestSource.ordinal()]) == 1 || i == 2) {
            return false;
        }
        return true;
    }

    private boolean isNeedPrintCmp(AppMngConstant.AppStartSource requestSource) {
        if (this.mDebugDetail) {
            return true;
        }
        int i = AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[requestSource.ordinal()];
        if (i == 3 || i == 4 || i == 5 || i == 6) {
            return false;
        }
        return true;
    }

    private void printReqLog(AppStartupInfo appStartupInfo, int alwType, String procName) {
        AppMngConstant.AppStartSource requestSource = appStartupInfo.requestSource;
        StringBuilder sbReason = appStartupInfo.sbReason;
        int uid = appStartupInfo.applicationInfo.uid;
        String pkg = appStartupInfo.applicationInfo.packageName;
        int callerUid = appStartupInfo.callerUid;
        WindowProcessController callerApp = appStartupInfo.callerApp;
        if (isMergeReqLog(requestSource, alwType)) {
            AppStartLogRecord logRecord = new AppStartLogRecord(requestSource, Integer.toString(alwType) + '[' + sbReason.toString() + ']', callerUid, callerApp == null ? "null" : callerApp.mName, appStartupInfo.action == null ? "null" : appStartupInfo.action, null);
            synchronized (this.mLogRecordMap) {
                ArraySet<String> pkgSet = this.mLogRecordMap.get(logRecord);
                if (pkg == null) {
                    pkg = "NA";
                }
                String pkgUid = pkg + "#" + uid;
                if (pkgSet != null) {
                    pkgSet.add(pkgUid);
                } else {
                    ArraySet<String> pkgSet2 = new ArraySet<>();
                    pkgSet2.add(pkgUid);
                    this.mLogRecordMap.put(logRecord, pkgSet2);
                }
            }
            if (!this.mHandler.hasMessages(8)) {
                this.mHandler.sendEmptyMessageDelayed(8, 1000);
            }
            return;
        }
        StringBuilder buf = REQ_LOG;
        buf.setLength(0);
        buf.append("req:");
        buf.append(requestSource.getDesc());
        buf.append(',');
        buf.append("alw:");
        buf.append(alwType);
        buf.append('[');
        buf.append(sbReason.toString());
        buf.append(']');
        buf.append(',');
        buf.append("flg:");
        buf.append(appStartupInfo.isSysApp ? 1 : 0);
        buf.append(appStartupInfo.isAppStop ? 1 : 0);
        int[] fgFlags = appStartupInfo.fgFlags;
        buf.append(fgFlags[0]);
        buf.append(fgFlags[1]);
        buf.append(fgFlags[2]);
        buf.append(',');
        buf.append("proc:");
        buf.append(uid);
        buf.append(',');
        buf.append(procName);
        buf.append(',');
        buf.append("call:");
        buf.append(callerUid);
        buf.append(',');
        buf.append(callerApp == null ? "null" : callerApp.mName);
        buf.append(' ');
        buf.append("act:");
        buf.append(appStartupInfo.action);
        buf.append(',');
        if (isNeedPrintCmp(requestSource)) {
            buf.append("cmp:");
            buf.append(appStartupInfo.compName);
        } else {
            buf.append("cmp:");
            buf.append("NA");
        }
        AwareLog.i(TAG_SIMPLE, buf.toString());
    }

    private boolean isProcessExist(WindowProcessController targetApp, String procName, int uid) {
        if (targetApp == null) {
            return HwActivityManager.isProcessExistLocked(procName, uid);
        }
        return true;
    }

    private int getPackageAllowType(AppStartupInfo appStartupInfo) {
        boolean reportData;
        String procName = appStartupInfo.procName;
        ApplicationInfo applicationInfo = appStartupInfo.applicationInfo;
        int callerPid = appStartupInfo.callerPid;
        int callerUid = appStartupInfo.callerUid;
        WindowProcessController targetApp = appStartupInfo.targetApp;
        WindowProcessController callerApp = appStartupInfo.callerApp;
        AppMngConstant.AppStartSource requestSource = appStartupInfo.requestSource;
        String compName = appStartupInfo.compName;
        String action = appStartupInfo.action;
        int unPercetibleAlarm = appStartupInfo.unPercetibleAlarm;
        boolean fromRecent = appStartupInfo.fromRecent;
        Intent intent = appStartupInfo.intent;
        int[] fgFlags = {-1, -1, -1};
        String pkg = applicationInfo.packageName;
        StringBuilder sbReason = new StringBuilder(3);
        HwAppStartupSetting startupSetting = getAppStartupSetting(pkg);
        boolean isSysApp = this.mDataMgr.isSystemBaseApp(applicationInfo);
        boolean existedProc = isProcessExist(targetApp, procName, applicationInfo.uid);
        boolean reportData2 = !existedProc;
        boolean allow = true;
        boolean isAppStop = isApplicationStop(applicationInfo, existedProc) || iawareGetPreloadState(applicationInfo.uid);
        appStartupInfo.setApplicationInfo(applicationInfo).setRequestSource(requestSource).setCallerApp(callerApp).setCallerPid(callerPid).setCallerUid(callerUid).setPolicyType(getPolicyType(startupSetting, requestSource)).setIsAppStop(isAppStop).setIsSysApp(isSysApp).setCompName(compName).setAction(action).setReason(sbReason).setFgFlags(fgFlags).setUnPercetibleAlarm(unPercetibleAlarm).setFromRecent(fromRecent).setIntent(intent).setHwAppStartupSetting(startupSetting);
        int alwType = getStartupAllowType(appStartupInfo);
        if (alwType <= 0) {
            allow = false;
        }
        if (isNeedReportRecordToHSM(startupSetting, requestSource, alwType, isAppStop, isSysApp)) {
            saveAppStartupRecord(pkg, requestSource.getDesc(), callerUid, allow, isAppSelfStart(requestSource));
        }
        if (!allow || AppMngConstant.AppStartSource.SCHEDULE_RESTART.equals(requestSource)) {
            reportData = true;
        } else {
            reportData = reportData2;
        }
        updateStartupBigData(appStartupInfo, reportData, alwType, procName);
        if (this.mDebugDetail || (Log.HWINFO && reportData)) {
            printReqLog(appStartupInfo, alwType, procName);
        }
        return alwType;
    }

    private AwareAppStartStatusCacheExt getAwareAppStartStatusCacheExt(String pkg) {
        AwareAppStartStatusCacheExt statusCacheExt = new AwareAppStartStatusCacheExt();
        statusCacheExt.abroad = this.mIsAbroadArea;
        statusCacheExt.gmsNeedCtrl = AwareIntelligentRecg.getInstance().isGmsAppAndNeedCtrl(pkg);
        return statusCacheExt;
    }

    private int getStartupAllowType(AppStartupInfo appStartupInfo) {
        String reason = "";
        ApplicationInfo applicationInfo = appStartupInfo.applicationInfo;
        AppMngConstant.AppStartSource source = appStartupInfo.requestSource;
        WindowProcessController callerApp = appStartupInfo.callerApp;
        StringBuilder sbReason = appStartupInfo.sbReason;
        HwAppStartupSetting startupSetting = appStartupInfo.startupSetting;
        String pkg = appStartupInfo.applicationInfo.packageName;
        AwareAppStartStatusCacheExt statusCacheExt = getAwareAppStartStatusCacheExt(pkg);
        int alwType = 0;
        if (isForbidApp(pkg, source, appStartupInfo.isAppStop, callerApp)) {
            reason = AppMngConstant.AppStartReason.DEFAULT.getDesc();
        } else if (isSubUserAppControl(applicationInfo.uid, source)) {
            alwType = 4;
            reason = AppMngConstant.AppStartReason.DEFAULT.getDesc();
        } else if (isAllowStartPkgs(appStartupInfo.policyType, startupSetting, pkg)) {
            alwType = 4;
            reason = AppMngConstant.AppStartReason.DEFAULT.getDesc();
        } else if (appStartupInfo.policyType == 0 && !isNeedAutoAppStartManage(pkg, source, appStartupInfo.callerUid, statusCacheExt)) {
            alwType = 4;
            reason = AppMngConstant.AppStartReason.DEFAULT.getDesc();
        } else if (appStartupInfo.policyType == 2) {
            alwType = 3;
            reason = "U";
        } else {
            int i = 0;
            appStartupInfo.setApplicationInfo(applicationInfo).setCallerApp(callerApp).setRequestSource(source).setIsCallerBtMediaBrowser(false).setIsCallerNotifyListener(false).setStatusCacheExt(statusCacheExt);
            alwType = this.mDataMgr.getDefaultAllowedRequestType(appStartupInfo);
            if (alwType > 0) {
                reason = getReasonWhenAllow(reason, alwType, source);
            } else {
                Policy policyTmp = DecisionMaker.getInstance().decide(pkg, source, getAwareAppStartStatusCache(appStartupInfo, applicationInfo, source, statusCacheExt), appStartupInfo.policyType);
                if (policyTmp instanceof AppStartPolicy) {
                    AppStartPolicy policy = (AppStartPolicy) policyTmp;
                    if (policy.getPolicy() != 0) {
                        i = 4;
                    }
                    alwType = i;
                    reason = policy.getReason();
                }
            }
        }
        if (isAllowAppWhenUpgrade(pkg, alwType, source)) {
            alwType = 4;
            reason = AppMngConstant.AppStartReason.DEFAULT.getDesc();
        }
        updateSbReason(sbReason, reason);
        return alwType;
    }

    private AwareAppStartStatusCache getAwareAppStartStatusCache(AppStartupInfo appStartupInfo, ApplicationInfo applicationInfo, AppMngConstant.AppStartSource requestSource, AwareAppStartStatusCacheExt statusCacheExt) {
        int[] fgFlags = appStartupInfo.fgFlags;
        getFgFlags(fgFlags, applicationInfo, appStartupInfo.callerUid, appStartupInfo.callerPid, requestSource);
        return new AwareAppStartStatusCache(appStartupInfo, applicationInfo, fgFlags[1] == 1, fgFlags[0] == 1, statusCacheExt);
    }

    private void updateSbReason(StringBuilder sbReason, String reason) {
        if (sbReason != null) {
            sbReason.setLength(0);
            sbReason.append(reason);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: int[] */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [boolean] */
    private void getFgFlags(int[] fgFlags, ApplicationInfo applicationInfo, int callerUid, int callerPid, AppMngConstant.AppStartSource requestSource) {
        ?? isAppForeground = isAppForeground(applicationInfo.uid);
        boolean fgCaller = isAppForeground(callerUid);
        int i = 0;
        if (!fgCaller) {
            fgCaller = AwareAppAssociate.getInstance().isRecentFgApp(callerUid) || isAppForegroundExt(requestSource, callerPid);
            fgFlags[2] = fgCaller ? 1 : 0;
        }
        fgFlags[0] = isAppForeground;
        if (fgCaller) {
            i = 1;
        }
        fgFlags[1] = i;
    }

    private String getReasonWhenAllow(String oriReason, int alwType, AppMngConstant.AppStartSource requestSource) {
        if (AppMngConstant.AppStartSource.THIRD_ACTIVITY.equals(requestSource)) {
            return requestSource.getDesc() + AppMngConstant.AppStartReason.DEFAULT.getDesc();
        } else if (alwType == 1) {
            return "O";
        } else {
            if (alwType == 2) {
                return AwarenessInnerConstants.TIME_FENCE_HOUR_MINUTE_SPLIT_KEY;
            }
            return oriReason;
        }
    }

    private boolean isAllowAppWhenUpgrade(String pkgName, int alwType, AppMngConstant.AppStartSource requestSource) {
        boolean allow = false;
        if (!AppMngConstant.AppStartSource.THIRD_BROADCAST.equals(requestSource) && !AppMngConstant.AppStartSource.SYSTEM_BROADCAST.equals(requestSource)) {
            return false;
        }
        String topImCN = AwareIntelligentRecg.getInstance().getActTopIMCN();
        if (!MM_HOTA_AUTOSTART_HAS_DONE && alwType <= 0 && topImCN != null && topImCN.equals(pkgName)) {
            if (this.mIsUpgrade) {
                AwareLog.i(TAG, topImCN + " is allowed to auto start for MEDIA_MOUNTED when ota");
                allow = true;
            }
            MM_HOTA_AUTOSTART_HAS_DONE = true;
        }
        return allow;
    }

    private boolean isAppForegroundExt(AppMngConstant.AppStartSource requestSource, int callerPid) {
        if (AppMngConstant.AppStartSource.THIRD_ACTIVITY.equals(requestSource)) {
            return AwareAppAssociate.getInstance().isVisibleWindow(callerPid);
        }
        return false;
    }

    public boolean needAppKeepAlv(HwAppStartupSetting setting) {
        if (setting != null && setting.getPolicy(0) == 0 && setting.getPolicy(3) == 1) {
            return true;
        }
        return false;
    }

    private boolean needCheckAllowStartPkgs(int policyType, HwAppStartupSetting setting) {
        if (needAppKeepAlv(setting)) {
            return true;
        }
        if (!this.mIsAbroadArea || policyType != 0) {
            return false;
        }
        return true;
    }

    private boolean isAllowStartPkgs(int policyType, HwAppStartupSetting setting, String pkg) {
        return needCheckAllowStartPkgs(policyType, setting) && AwareIntelligentRecg.getInstance().isAllowStartPkgs(pkg);
    }

    private boolean isNeedAutoAppStartManage(String pkg, AppMngConstant.AppStartSource requestSource, int callerUid, AwareAppStartStatusCacheExt statusCacheExt) {
        if (AwareMiddleware.getInstance().isZApp(pkg)) {
            return false;
        }
        if (!this.mIsAbroadArea) {
            return true;
        }
        statusCacheExt.appSrcRange = AwareIntelligentRecg.getInstance().getAppStartSpecAppSrcRangeResult(pkg);
        if (statusCacheExt.appSrcRange != 0 && AwareIntelligentRecg.getInstance().isGmsCaller(callerUid)) {
            return false;
        }
        return true;
    }

    private boolean isNeedReportRecordToHSM(HwAppStartupSetting startupSetting, AppMngConstant.AppStartSource requestSource, int alwType, boolean isAppStop, boolean isSysApp) {
        if (isSysApp || isSubUser()) {
            return false;
        }
        if (startupSetting != null) {
            if (startupSetting.getShow(0) == 0) {
                return false;
            }
            if (startupSetting.getShow(isAppSelfStart(requestSource) ? 1 : 2) == 0) {
                return false;
            }
        }
        if (alwType <= 0) {
            return true;
        }
        int i = AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[requestSource.ordinal()];
        if (i != 1) {
            switch (i) {
                case 6:
                    break;
                case 7:
                case 8:
                case 9:
                case 10:
                    if (!isAppStop || alwType == 1 || alwType == 2) {
                        return false;
                    }
                    return true;
                default:
                    return isAppStop;
            }
        }
        return false;
    }

    private void sendAppStartupChangedMsg() {
        this.mHandler.removeMessages(4);
        this.mHandler.sendEmptyMessageDelayed(4, 5000);
    }

    private void saveAppStartupRecord(String pkgName, String callerType, int callerUid, boolean result, boolean selfStart) {
        int size;
        int count = 1;
        boolean trimed = false;
        HsmRecord hr = new HsmRecord(pkgName, callerType, callerUid, result, selfStart, null);
        synchronized (this.mHsmRecordList) {
            Integer val = this.mHsmRecordList.remove(hr);
            if (val != null) {
                count = val.intValue() + 1;
                trimed = true;
            }
            this.mHsmRecordList.put(hr, Integer.valueOf(count));
            size = this.mHsmRecordList.size();
        }
        if (size > 30) {
            this.mHandler.removeMessages(3);
            this.mHandler.sendEmptyMessage(3);
        } else if (!this.mHandler.hasMessages(3)) {
            this.mHandler.sendEmptyMessageDelayed(3, HwArbitrationDEFS.DelayTimeMillisA);
        }
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "saveAppStartupRecord trimed=" + trimed + ", size=" + size + ", num=" + count + ", " + hr);
        }
    }

    /* access modifiers changed from: private */
    public void startNotifyChangedService() {
        Intent intentService = new Intent(ACTION_APPSTARTUP_CHANGED);
        intentService.setPackage(PG_PACKAGE_NAME);
        this.mContext.sendBroadcastAsUser(intentService, UserHandle.ALL);
        Intent intentHwPush = new Intent(ACTION_APPSTARTUP_CHANGED);
        intentHwPush.setPackage(AppStartupDataMgr.HWPUSH_PKGNAME);
        this.mContext.sendBroadcastAsUser(intentHwPush, UserHandle.ALL);
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "startNotifyChangedService called");
        }
    }

    /* access modifiers changed from: private */
    public void startRecordService() {
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "startRecordService called.");
        }
        synchronized (this.mHsmRecordList) {
            int size = this.mHsmRecordList.size();
            if (size > 0) {
                String[] pkgList = new String[size];
                String[] typeList = new String[size];
                int[] uidList = new int[size];
                int[] resList = new int[size];
                int[] autoList = new int[size];
                int[] countList = new int[size];
                long[] timeList = new long[size];
                for (int index = 0; index < size; index++) {
                    HsmRecord hr = this.mHsmRecordList.keyAt(index);
                    pkgList[index] = hr.pkg;
                    typeList[index] = hr.callerType;
                    uidList[index] = hr.callerUid;
                    int i = 0;
                    resList[index] = hr.result ? 1 : 0;
                    if (hr.autoStart) {
                        i = 1;
                    }
                    autoList[index] = i;
                    timeList[index] = hr.timeStamp;
                    countList[index] = this.mHsmRecordList.valueAt(index).intValue();
                }
                this.mHsmRecordList.clear();
                Bundle bundle = new Bundle();
                bundle.putStringArray("B_TARGET_PKG", pkgList);
                bundle.putStringArray("B_CALL_TYPE", typeList);
                bundle.putIntArray("B_CALL_UID", uidList);
                bundle.putIntArray("B_RESULT", resList);
                bundle.putIntArray("B_AUTO_START", autoList);
                bundle.putIntArray("B_COUNT", countList);
                bundle.putLongArray("B_TIME_STAMP", timeList);
                startRecordServiceWithBundle(bundle);
            }
        }
    }

    private void startRecordServiceWithBundle(Bundle bundle) {
        Intent intentService = new Intent(ACTION_APPSTARTUP_RECORD);
        intentService.setPackage("com.huawei.systemmanager");
        intentService.putExtras(bundle);
        try {
            this.mContext.startServiceAsUser(intentService, UserHandle.CURRENT);
        } catch (SecurityException e) {
            AwareLog.e(TAG, "startRecordService catch SecurityException");
        } catch (IllegalStateException e2) {
            AwareLog.e(TAG, "startRecordService catch IllegalStateException");
        }
    }

    /* access modifiers changed from: private */
    public void reportProcessStart(Message msg) {
        Bundle bundle = msg.getData();
        if (bundle != null) {
            String pkgName = bundle.getString("PACKAGE");
            String hostingType = bundle.getString("HOST_TYPE");
            boolean iawareCtrl = BigMemoryConstant.BIGMEMINFO_ITEM_TAG.equals(hostingType) || AwareAppMngSort.ADJTYPE_SERVICE.equals(hostingType) || "broadcast".equals(hostingType) || "content provider".equals(hostingType);
            if ("webview_service".equals(hostingType)) {
                pkgName = WebViewZygote.getPackageName();
            }
            SRMSDumpRadar instance = SRMSDumpRadar.getInstance();
            String[] strArr = {"T"};
            int[][] iArr = new int[1][];
            int[] iArr2 = new int[2];
            iArr2[0] = 1;
            iArr2[1] = iawareCtrl ? 0 : 1;
            iArr[0] = iArr2;
            instance.updateStartupData(pkgName, strArr, iArr);
        }
    }

    /* access modifiers changed from: private */
    public void periodRemoveRestartData() {
        int removeCount = 0;
        long curTime = SystemClock.elapsedRealtime();
        synchronized (this.mRestartDataList) {
            for (int i = this.mRestartDataList.size() - 1; i >= 0; i--) {
                if (curTime - this.mRestartDataList.valueAt(i).timeStamp > DELAY_MILLS_REMOVE_RESTART_DATA) {
                    this.mRestartDataList.removeAt(i);
                    removeCount++;
                }
            }
        }
        this.mLastRemoveMsgTime = 0;
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "periodRemoveRestartData removeCount: " + removeCount);
        }
    }

    private void updateStartupBigData(AppStartupInfo appStartupInfo, boolean reportData, int alwType, String procName) {
        this.mAllowData.setDirty(true);
        if ((AppStartupFeature.isBetaUser() || !appStartupInfo.isSysApp) && !this.mIsAbroadArea && !isSubUser()) {
            String pkg = appStartupInfo.applicationInfo.packageName;
            String reason = appStartupInfo.sbReason.toString();
            int policyType = appStartupInfo.policyType;
            AppMngConstant.AppStartSource requestSource = appStartupInfo.requestSource;
            if (alwType <= 0) {
                updateStartupData(pkg, reason, policyType);
            } else if (!reportData) {
            } else {
                if (AppMngConstant.AppStartSource.SCHEDULE_RESTART.equals(requestSource)) {
                    synchronized (this.mRestartDataList) {
                        try {
                            ArrayMap<String, RestartData> arrayMap = this.mRestartDataList;
                            try {
                                arrayMap.put(pkg + "#" + procName, new RestartData(pkg, reason, alwType, policyType, null));
                                removeRestartDataIfNeed();
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                } else if (!AwareIntelligentRecg.getInstance().isWebViewUid(appStartupInfo.applicationInfo.uid)) {
                    if (AppMngConstant.AppStartSource.SYSTEM_BROADCAST.equals(requestSource) || AppMngConstant.AppStartSource.THIRD_BROADCAST.equals(requestSource)) {
                        this.mAllowData.set(reason, alwType, policyType);
                        this.mAllowData.setDirty(false);
                    }
                    updateAllowedBigData(pkg, reason, alwType, policyType, true);
                }
            }
        }
    }

    private void removeRestartDataIfNeed() {
        long curTime = SystemClock.elapsedRealtime();
        if (curTime - this.mLastRemoveMsgTime > DELAY_MILLS_REMOVE_RESTART_DATA) {
            this.mHandler.sendEmptyMessageDelayed(7, DELAY_MILLS_REMOVE_RESTART_DATA);
            this.mLastRemoveMsgTime = curTime;
        }
    }

    private void updateStartupData(String pkg, String reason, int policyType) {
        SRMSDumpRadar dumpRadar = SRMSDumpRadar.getInstance();
        if (policyType == 1) {
            dumpRadar.updateStartupData(pkg, new String[]{"U"}, new int[]{0, 0, 1});
        } else if (policyType == 0) {
            int[] smtFbdFmt = {0, 1};
            dumpRadar.updateStartupData(pkg, new String[]{"I", reason}, smtFbdFmt, smtFbdFmt);
        }
    }

    private void updateAllowedBigData(String pkg, String reason, int alwType, int policyType, boolean increase) {
        int increaseVal = increase ? 1 : -1;
        SRMSDumpRadar dumpRadar = SRMSDumpRadar.getInstance();
        if (policyType == 2) {
            dumpRadar.updateStartupData(pkg, new String[]{"U"}, new int[]{increaseVal, 0, 0});
        } else if (policyType == 1) {
            dumpRadar.updateStartupData(pkg, new String[]{"U"}, new int[]{0, increaseVal, 0});
        } else if (policyType == 0) {
            int[] smtAlwFmt = {increaseVal, 0};
            int[] smtSingleAlwFmt = {increaseVal};
            if (alwType == 1) {
                dumpRadar.updateStartupData(pkg, new String[]{"I", "O"}, smtAlwFmt, smtSingleAlwFmt);
            } else if (alwType == 2) {
                dumpRadar.updateStartupData(pkg, new String[]{"I", AwarenessInnerConstants.TIME_FENCE_HOUR_MINUTE_SPLIT_KEY}, smtAlwFmt, smtSingleAlwFmt);
            } else {
                dumpRadar.updateStartupData(pkg, new String[]{"I", reason}, smtAlwFmt, smtAlwFmt);
            }
        }
    }

    public void updateBroadJobCtrlBigData(String pkg) {
        if (isPolicyEnabled() && !this.mAllowData.getDirty() && !isSubUser()) {
            updateAllowedBigData(pkg, this.mAllowData.reason, this.mAllowData.alwType, this.mAllowData.policyType, false);
        }
    }

    private int getPolicyType(HwAppStartupSetting startupSetting, AppMngConstant.AppStartSource requestSource) {
        if (startupSetting == null || startupSetting.getPolicy(0) == 1) {
            return 0;
        }
        int value = startupSetting.getPolicy(isAppSelfStart(requestSource) ? 1 : 2);
        if (value == 1) {
            return 2;
        }
        if (value == 0) {
            return 1;
        }
        return 0;
    }

    public static boolean isAppSelfStart(AppMngConstant.AppStartSource requestSource) {
        if (requestSource == null) {
            return false;
        }
        int i = AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppStartSource[requestSource.ordinal()];
        if (i == 2 || i == 3 || i == 4 || i == 5 || i == 6) {
            return true;
        }
        return false;
    }

    private void scheduleFastWriteCache(CacheType cacheType) {
        if (cacheType == CacheType.STARTUP_SETTING) {
            setAppStartupSyncFlag(true);
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessage(2);
        }
    }

    /* access modifiers changed from: private */
    public void writeCacheFile(CacheType cacheType) {
        if (this.mDebugDetail) {
            AwareLog.i(TAG, "writeCacheFile type=" + cacheType);
        }
        if (cacheType == CacheType.STARTUP_SETTING) {
            this.mDataMgr.flushStartupSettingToDisk();
            setAppStartupSyncFlag(false);
        }
    }

    private boolean isAppForeground(int uid) {
        if (AwareAppAssociate.isDealAsPkgUid(uid)) {
            return AwareAppAssociate.getInstance().isForeGroundApp(uid);
        }
        HwActivityManagerService hwAms = HwActivityManagerService.self();
        if (hwAms != null) {
            int uidState = hwAms.iawareGetUidState(uid);
            if (uidState <= this.mForegroundAppLevel || uidState > this.mForegroundSleepAppLevel) {
                if (uidState > this.mForegroundSleepAppLevel || uidState == 21) {
                    return false;
                }
            } else if (AwareAppAssociate.getInstance().isForeGroundApp(uid)) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean iawareGetPreloadState(int uid) {
        HwActivityManagerService hwAMS = HwActivityManagerService.self();
        if (hwAMS != null) {
            return hwAMS.iawareGetPreloadState(uid);
        }
        return false;
    }

    private boolean isApplicationStop(ApplicationInfo applicationInfo, boolean existedProc) {
        boolean isAppStop = true;
        if (AwareAppAssociate.isDealAsPkgUid(applicationInfo.uid)) {
            if (existedProc || AwareAppAssociate.getInstance().isPkgHasProc(applicationInfo.packageName)) {
                isAppStop = false;
            }
            return isAppStop;
        }
        HwActivityManagerService hwAms = HwActivityManagerService.self();
        if (hwAms == null) {
            return false;
        }
        if (hwAms.iawareGetUidProcNum(applicationInfo.uid) > 0) {
            isAppStop = false;
        }
        return isAppStop;
    }

    public void reportPgClean(String pkg) {
        if (isPolicyEnabled() && this.mDataMgr.isPGForbidRestart(pkg)) {
            this.mDataMgr.addPgCleanApp(pkg);
        }
    }

    public void dump(PrintWriter pw, String[] args) {
        if (args != null && pw != null) {
            if (args.length < 3) {
                pw.println(BAD_COMMAND_INFO);
            } else if (args.length != 3) {
                String cmd = args[2];
                String param = args[3];
                if ("switch".equals(cmd)) {
                    if ("0".equals(param)) {
                        AppStartupFeature.setEnable(false);
                    } else if ("1".equals(param)) {
                        AppStartupFeature.setEnable(true);
                    } else {
                        pw.println(BAD_COMMAND_INFO);
                    }
                } else if (!"multiuser".equals(cmd)) {
                    dumpOthers(pw, args);
                } else if ("0".equals(param)) {
                    this.mSubUserAppCtrl = false;
                } else if ("1".equals(param)) {
                    this.mSubUserAppCtrl = true;
                } else {
                    pw.println(BAD_COMMAND_INFO);
                }
            } else if ("cache".equals(args[2])) {
                this.mDataMgr.dump(pw, args);
            } else if (!isPolicyEnabled()) {
                pw.println("App startup feature is disabled or not inited.");
            } else {
                if ("info".equals(args[2])) {
                    pw.println("betaUser=" + AppStartupFeature.isBetaUser() + ", inited=" + this.mPolicyInited.get() + ", switch=" + AppStartupFeature.isAppStartupEnabled() + ", multiuser=" + this.mSubUserAppCtrl + ", userId=" + AwareAppAssociate.getInstance().getCurSwitchUser());
                }
                this.mDataMgr.dump(pw, args);
            }
        }
    }

    private void dumpOthers(PrintWriter pw, String[] args) {
        String cmd = args[2];
        String param = args[3];
        if (!isPolicyEnabled()) {
            pw.println("App startup feature is disabled or not inited.");
            return;
        }
        if ("log".equals(cmd)) {
            if (!dumpOthersOfLog(param)) {
                pw.println(BAD_COMMAND_INFO);
                return;
            }
        } else if ("cost".equals(cmd)) {
            if (!dumpOthersOfCost(param)) {
                pw.println(BAD_COMMAND_INFO);
                return;
            }
        } else if ("bigdata".equals(cmd)) {
            boolean clear = false;
            boolean onlyDiff = false;
            if ("0".equals(param)) {
                clear = false;
            } else if ("1".equals(param)) {
                clear = true;
            } else if ("2".equals(param)) {
                onlyDiff = true;
            } else {
                pw.println(BAD_COMMAND_INFO);
                return;
            }
            String bigdata = SRMSDumpRadar.getInstance().saveStartupBigData(AppStartupFeature.isBetaUser(), clear, onlyDiff);
            pw.println(bigdata);
            pw.println("Total size: " + bigdata.length());
            return;
        } else if ("widgetinterval".equals(cmd)) {
            try {
                AwareIntelligentRecg.getInstance().dumpWidgetUpdateInterval(pw, Integer.parseInt(param));
                return;
            } catch (NumberFormatException e) {
                pw.println("widgetinterval value error");
                return;
            }
        } else {
            pw.println("Try Dump DataMgr");
        }
        this.mDataMgr.dump(pw, args);
    }

    private boolean dumpOthersOfLog(String param) {
        if ("0".equals(param)) {
            this.mDebugDetail = false;
            return true;
        } else if (!"1".equals(param)) {
            return false;
        } else {
            this.mDebugDetail = true;
            return true;
        }
    }

    private boolean dumpOthersOfCost(String param) {
        if ("0".equals(param)) {
            this.mDebugCost = false;
            return true;
        } else if (!"1".equals(param)) {
            return false;
        } else {
            this.mDebugCost = true;
            return true;
        }
    }
}
