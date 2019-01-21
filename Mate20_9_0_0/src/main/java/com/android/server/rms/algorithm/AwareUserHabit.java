package com.android.server.rms.algorithm;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.IUserSwitchObserver;
import android.app.IUserSwitchObserver.Stub;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.LogIAware;
import android.rms.iaware.StatisticsData;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.iaware.appmng.AwareProcessBaseInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.InnerUtils;
import com.android.server.rms.algorithm.AwareUserHabitAlgorithm.HabitProtectListChangeListener;
import com.android.server.rms.algorithm.utils.IAwareHabitUtils;
import com.android.server.rms.iaware.appmng.AppMngConfig;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AwareUserHabit {
    private static final int ACTIVITIES_CHANGED_MESSAGE_DELAY = 300;
    private static final int ALL_DEVICE_PAY_NUM = 3;
    private static final String APP_STATUS = "app_status";
    private static final String DEFAULT_PKG_NAME = "com.huawei.android.launcher";
    private static final int FOREGROUND_ACTIVITIES_CHANGED = 1;
    private static final int HIGH_DEVICE_BUSINESS_NUM = 5;
    private static final int HIGH_DEVICE_EMAIL_NUM = 2;
    private static final int HIGH_DEVICE_IM_NUM = 3;
    private static final int LOAD_APPTYPE_MESSAGE_DELAY = 20000;
    private static final int LOW_DEVICE_BUSINESS_NUM = 3;
    private static final int LOW_DEVICE_EMAIL_NUM = 1;
    private static final int LOW_DEVICE_IM_NUM = 2;
    private static final long ONE_MINUTE = 60000;
    private static final String REPORT_DYNAMIC_TOPN_KEY = "dynamicTopN";
    private static final int REPORT_MESSAGE = 2;
    private static final String REPORT_SMALL_SAMPLE_LIST_KEY = "SmallSampleList";
    private static final String REPORT_SORT_KEY = "predictSorted";
    private static final String REPORT_SWITCHOFF_KEY = "predictV2Off";
    private static final int SCREEN_STATE_CHANGED = 4;
    private static final long SREEN_OFF_SORT_THRESHOLD = 1800000;
    private static final long STAY_IN_BACKGROUND_ONE_DAYS = 86400000;
    private static final String TAG = "AwareUserHabit";
    private static final int TOPN = 5;
    private static final int UID_VALUE = 10000;
    private static final int UNINSTALL_CHECK_HABIT_PROTECT_LIST = 3;
    private static final String UNINSTALL_PKGNAME = "uninstall_pkgName";
    public static final int USERHABIT_INSTALL_APP = 1;
    public static final String USERHABIT_INSTALL_APP_UPDATE = "install_app_update";
    public static final String USERHABIT_PACKAGE_NAME = "package_name";
    public static final int USERHABIT_PRECOG_INITIALIZED = 6;
    public static final int USERHABIT_SCREEN_OFF = 7;
    public static final int USERHABIT_TRAIN_COMPLETED = 3;
    public static final String USERHABIT_UID = "uid";
    public static final int USERHABIT_UNINSTALL_APP = 2;
    public static final int USERHABIT_UPDATE_CONFIG = 4;
    public static final int USERHABIT_USER_SWITCH = 5;
    private static final String VISIBLE_ADJ_TYPE = "visible";
    private static AwareUserHabit mAwareUserHabit = null;
    private static boolean mEnabled = false;
    private String mAppMngLastPkgName = "com.huawei.android.launcher";
    private final AtomicInteger mAppTypeLoadCount = new AtomicInteger(2);
    private AwareHSMListHandler mAwareHSMListHandler = null;
    private AwareUserHabitAlgorithm mAwareUserHabitAlgorithm = null;
    private AwareUserHabitRadar mAwareUserHabitRadar = null;
    private Context mContext = null;
    private String mCurPkgName = "com.huawei.android.launcher";
    private int mDynamicTopN = 5;
    private int mEmailCount = 1;
    private long mFeatureStartTime = 0;
    final Handler mHandler = new UserHabitHandler(BackgroundThread.get().getLooper());
    private long mHighEndDeviceStayInBgTime = 432000000;
    private HwActivityManagerService mHwAMS = null;
    private int mImCount = 2;
    private boolean mIsFirstTime = true;
    private boolean mIsLowEnd = false;
    private boolean mIsNewAlgorithmSwitched = false;
    private String mLastAppPkgName = "com.huawei.android.launcher";
    private long mLastSortTime = 0;
    private boolean mLauncherIsVisible = false;
    private long mLowEndDeviceStayInBgTime = 172800000;
    private int mLruCount = 1;
    private int mMostUsedCount = 1;
    private final Object mPredictLock = new Object();
    private UserHabitProcessObserver mProcessObserver = new UserHabitProcessObserver();
    private final AtomicInteger mUserId = new AtomicInteger(0);
    private IUserSwitchObserver mUserSwitchObserver = new Stub() {
        public void onUserSwitching(int newUserId, IRemoteCallback reply) {
            if (reply != null) {
                try {
                    reply.sendResult(null);
                } catch (RemoteException e) {
                    AwareLog.e(AwareUserHabit.TAG, "RemoteException onUserSwitching");
                }
            }
        }

        public void onUserSwitchComplete(int newUserId) throws RemoteException {
            AwareUserHabit.this.mUserId.set(newUserId);
            AwareUserHabit.this.setUserId(newUserId);
            if (newUserId != 0) {
                AwareUserHabit.this.mIsNewAlgorithmSwitched = false;
            }
            Message msg = AwareUserHabit.this.mHandler.obtainMessage();
            msg.what = 2;
            msg.arg1 = 5;
            AwareUserHabit.this.mHandler.sendMessage(msg);
        }

        public void onForegroundProfileSwitch(int newProfileId) {
        }

        public void onLockedBootComplete(int newUserId) {
        }
    };
    private LinkedHashMap<String, Integer> mUserTrackListVer2 = new LinkedHashMap();

    private class UserHabitHandler extends Handler {
        public UserHabitHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 4) {
                switch (i) {
                    case 1:
                        String pkgName = InnerUtils.getAwarePkgName(msg.arg1);
                        handleActivityChangedEvent(msg, pkgName);
                        if (!msg.getData().getBoolean(AwareUserHabit.APP_STATUS)) {
                            AwareIntelligentRecg.getInstance().reportAppChangeToBackground(pkgName, SystemClock.elapsedRealtime());
                            return;
                        }
                        return;
                    case 2:
                        handleReportMessage(msg);
                        return;
                    default:
                        return;
                }
            }
            handleScreenState(msg);
        }

        private void handleScreenState(Message msg) {
            if (AwareUserHabit.this.mAwareUserHabitAlgorithm != null && msg.arg1 == 7 && Math.abs(System.currentTimeMillis() - AwareUserHabit.this.mLastSortTime) > 1800000) {
                AwareUserHabit.this.mAwareUserHabitAlgorithm.sortUsageCount();
                AwareUserHabit.this.mLastSortTime = System.currentTimeMillis();
            }
        }

        private void handleActivityChangedEvent(Message msg, String pkgName) {
            if (AwareUserHabit.this.mAwareUserHabitAlgorithm != null && pkgName != null) {
                if (AwareUserHabit.this.mIsFirstTime) {
                    AwareUserHabit.this.mAwareUserHabitAlgorithm.init();
                    AwareUserHabit.this.mIsFirstTime = false;
                    AwareDefaultConfigList.getInstance().fillMostFrequentUsedApp(AwareUserHabit.this.getMostFrequentUsedApp(AppMngConfig.getAdjCustTopN(), -1));
                    LogIAware.report(HwArbitrationDEFS.MSG_Stop_MPLink_By_Notification, "userhabit inited");
                    AwareLog.i(AwareUserHabit.TAG, "report first data loading finished");
                }
                long time = SystemClock.elapsedRealtime();
                boolean isForeground = msg.getData().getBoolean(AwareUserHabit.APP_STATUS);
                int homePkgPid = AwareAppAssociate.getInstance().getCurHomeProcessPid();
                AwareProcessBaseInfo baseInfo = AwareUserHabit.this.mHwAMS != null ? AwareUserHabit.this.mHwAMS.getProcessBaseInfo(homePkgPid) : null;
                if (isForeground) {
                    AwareUserHabit.this.triggerUserTrackPredict(pkgName, AwareUserHabit.this.getLruCache(), time);
                    AwareUserHabit.this.mAwareUserHabitAlgorithm.foregroundUpdateHabitProtectList(pkgName);
                    AwareUserHabit.this.mAwareUserHabitAlgorithm.updateAppUsage(pkgName);
                    if (!(homePkgPid == msg.arg1 || baseInfo == null || !AwareUserHabit.VISIBLE_ADJ_TYPE.equals(baseInfo.mAdjType))) {
                        AwareUserHabit.this.mLauncherIsVisible = true;
                    }
                } else {
                    if (AwareUserHabit.this.mLauncherIsVisible && homePkgPid != msg.arg1 && baseInfo != null && baseInfo.mCurAdj == 0) {
                        String homePkgName = InnerUtils.getAwarePkgName(homePkgPid);
                        if (homePkgName != null) {
                            AwareUserHabit.this.triggerUserTrackPredict(homePkgName, AwareUserHabit.this.getLruCache(), time);
                        }
                    }
                    AwareUserHabit.this.mLauncherIsVisible = false;
                    AwareUserHabit.this.mAwareUserHabitAlgorithm.backgroundActivityChangedEvent(pkgName, Long.valueOf(time));
                }
                AwareUserHabit.this.mAwareUserHabitAlgorithm.addPkgToLru(pkgName, time);
            }
        }

        private void handleReportMessage(Message msg) {
            if (AwareUserHabit.this.mAwareUserHabitAlgorithm != null) {
                int eventId = msg.arg1;
                Bundle args = msg.getData();
                String pkgName;
                switch (eventId) {
                    case 1:
                        pkgName = args.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
                        if (pkgName != null) {
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.removeFilterPkg(pkgName);
                            break;
                        }
                        break;
                    case 2:
                        pkgName = args.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
                        if (pkgName != null) {
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.removePkgFromLru(pkgName);
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.addFilterPkg(pkgName);
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.uninstallUpdateHabitProtectList(pkgName);
                            break;
                        }
                        break;
                    case 3:
                    case 5:
                        AwareUserHabit.this.mAwareUserHabitAlgorithm.reloadDataInfo();
                        if (3 == eventId) {
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.trainedUpdateHabitProtectList();
                        } else {
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.clearLruCache();
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.initHabitProtectList();
                            AwareUserHabit.this.mAwareUserHabitAlgorithm.clearHabitProtectApps();
                            AwareUserHabit.this.mLastSortTime = System.currentTimeMillis();
                        }
                        AwareDefaultConfigList.getInstance().fillMostFrequentUsedApp(AwareUserHabit.this.getMostFrequentUsedApp(AppMngConfig.getAdjCustTopN(), -1));
                        LogIAware.report(HwArbitrationDEFS.MSG_Stop_MPLink_By_Notification, "train completed");
                        AwareLog.i(AwareUserHabit.TAG, "report train completed and data loading finished");
                        break;
                    case 4:
                        AwareLog.i(AwareUserHabit.TAG, "reloadFilterPkg");
                        AwareUserHabit.this.mAwareUserHabitAlgorithm.reloadFilterPkg();
                        AwareUserHabit.this.updateHabitConfig();
                        break;
                    case 6:
                        AwareLog.i(AwareUserHabit.TAG, "AwareUserHabit load precog pkg type info");
                        if (!AppTypeRecoManager.getInstance().loadInstalledAppTypeInfo()) {
                            if (AwareUserHabit.this.mAppTypeLoadCount.get() < 0) {
                                AwareLog.e(AwareUserHabit.TAG, "AwareUserHabit precog service is error");
                                break;
                            }
                            String str = AwareUserHabit.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("AwareUserHabit send load precog pkg message again mAppTypeLoadCount=");
                            stringBuilder.append(AwareUserHabit.this.mAppTypeLoadCount.get() - 1);
                            AwareLog.i(str, stringBuilder.toString());
                            AwareUserHabit.this.sendLoadAppTypeMessage();
                            AwareUserHabit.this.mAppTypeLoadCount.decrementAndGet();
                            break;
                        }
                        AwareLog.i(AwareUserHabit.TAG, "AwareUserHabit load precog pkg OK ");
                        break;
                }
            }
        }
    }

    class UserHabitProcessObserver extends IProcessObserver.Stub {
        UserHabitProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            Message msg = AwareUserHabit.this.mHandler.obtainMessage();
            msg.what = 1;
            msg.getData().putBoolean(AwareUserHabit.APP_STATUS, foregroundActivities);
            msg.arg1 = pid;
            AwareUserHabit.this.mHandler.sendMessageDelayed(msg, 300);
        }

        public void onProcessDied(int pid, int uid) {
        }
    }

    private AwareUserHabit(Context context) {
        this.mContext = context;
    }

    public static synchronized AwareUserHabit getInstance(Context context) {
        AwareUserHabit awareUserHabit;
        synchronized (AwareUserHabit.class) {
            if (mAwareUserHabit == null) {
                mAwareUserHabit = new AwareUserHabit(context);
                if (mEnabled) {
                    mAwareUserHabit.init();
                }
            }
            awareUserHabit = mAwareUserHabit;
        }
        return awareUserHabit;
    }

    public static synchronized AwareUserHabit getInstance() {
        AwareUserHabit awareUserHabit;
        synchronized (AwareUserHabit.class) {
            awareUserHabit = mAwareUserHabit;
        }
        return awareUserHabit;
    }

    public static void enable() {
        AwareLog.i(TAG, "AwareUserHabit enable is called");
        if (!mEnabled) {
            AwareUserHabit habit = getInstance();
            if (habit != null) {
                habit.init();
            } else {
                AwareLog.i(TAG, "user habit is not ready");
            }
            mEnabled = true;
        }
    }

    public static void disable() {
        AwareLog.i(TAG, "AwareUserHabit disable is called");
        if (mEnabled) {
            AwareUserHabit habit = getInstance();
            if (habit != null) {
                habit.deinit();
            } else {
                AwareLog.i(TAG, "user habit is not ready");
            }
        }
        mEnabled = false;
    }

    public boolean isEnable() {
        return mEnabled;
    }

    public void setLowEndFlag(boolean flag) {
        synchronized (this) {
            this.mIsLowEnd = flag;
        }
    }

    public List<String> getLRUAppList(int lruCount) {
        return this.mAwareUserHabitAlgorithm.getForceProtectAppsFromLRU(AwareAppAssociate.getInstance().getDefaultHomePackages(), lruCount);
    }

    private void deinit() {
        unregisterObserver();
        if (this.mAwareUserHabitAlgorithm != null) {
            this.mAwareUserHabitAlgorithm.deinit();
            this.mIsFirstTime = true;
        }
        if (this.mAwareHSMListHandler != null) {
            this.mAwareHSMListHandler.deinit();
        }
        AppTypeRecoManager.getInstance().deinit();
        AwareLog.d(TAG, "AwareUserHabit deinit finished");
    }

    private void init() {
        if (this.mContext != null) {
            if (this.mAwareUserHabitAlgorithm == null) {
                this.mAwareUserHabitAlgorithm = new AwareUserHabitAlgorithm(this.mContext);
            }
            this.mHwAMS = HwActivityManagerService.self();
            this.mAwareUserHabitAlgorithm.initHabitProtectList();
            AppTypeRecoManager.getInstance().init(this.mContext);
            registerObserver();
            this.mFeatureStartTime = System.currentTimeMillis();
            if (this.mAwareUserHabitRadar == null) {
                this.mAwareUserHabitRadar = new AwareUserHabitRadar(this.mFeatureStartTime);
            }
            if (this.mAwareHSMListHandler == null) {
                this.mAwareHSMListHandler = new AwareHSMListHandler(this.mContext);
            }
            this.mAwareHSMListHandler.init();
            updateHabitConfig();
            sendLoadAppTypeMessage();
            this.mLastSortTime = System.currentTimeMillis();
            AwareLog.d(TAG, "AwareUserHabit init finished");
        }
    }

    private void sendLoadAppTypeMessage() {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 2;
        msg.arg1 = 6;
        this.mHandler.sendMessageDelayed(msg, HwRecentsTaskUtils.MAX_REMOVE_TASK_TIME);
    }

    private void triggerUserTrackPredict(String pkgName, Map<String, Long> lruCache, long curTime) {
        if (!this.mIsNewAlgorithmSwitched) {
            synchronized (this.mPredictLock) {
                if (this.mAwareUserHabitAlgorithm != null) {
                    this.mAppMngLastPkgName = this.mAwareUserHabitAlgorithm.getLastPkgNameExcludeLauncher(pkgName, AwareAppAssociate.getInstance().getDefaultHomePackages());
                    if (!(this.mAwareUserHabitAlgorithm.containsFilterPkg(pkgName) || this.mAwareUserHabitAlgorithm.containsFilter2Pkg(pkgName))) {
                        this.mLastAppPkgName = pkgName;
                    }
                    this.mCurPkgName = pkgName;
                    this.mAwareUserHabitAlgorithm.triggerUserTrackPredict(this.mLastAppPkgName, lruCache, curTime, pkgName);
                }
            }
        }
    }

    public Map<String, String> getUserTrackAppSortDumpInfo() {
        if (this.mIsNewAlgorithmSwitched) {
            LinkedHashMap<String, String> result = new LinkedHashMap();
            for (Entry<String, Integer> entry : getUserTrackListVer2().entrySet()) {
                result.put((String) entry.getKey(), String.valueOf(entry.getValue()));
            }
            return result;
        }
        Map<String, String> dumpInfo = null;
        synchronized (this.mPredictLock) {
            if (this.mAwareUserHabitAlgorithm != null) {
                dumpInfo = this.mAwareUserHabitAlgorithm.getUserTrackPredictDumpInfo(this.mLastAppPkgName, getLruCache(), SystemClock.elapsedRealtime(), this.mCurPkgName);
            }
        }
        return dumpInfo;
    }

    public Set<String> getAllProtectApps() {
        if (this.mAwareHSMListHandler == null) {
            return null;
        }
        return this.mAwareHSMListHandler.getAllProtectSet();
    }

    public Set<String> getAllUnProtectApps() {
        if (this.mAwareHSMListHandler == null) {
            return null;
        }
        return this.mAwareHSMListHandler.getAllUnProtectSet();
    }

    /* JADX WARNING: Missing block: B:14:0x0037, code skipped:
            r0 = r1.mAwareUserHabitAlgorithm.getForceProtectAppsFromHabitProtect(r8, r9, r6);
            r5.addAll(r0);
            r6.addAll(r0);
            r11 = r1.mAwareUserHabitAlgorithm.getMostFrequentUsedApp(r10, -1, r6);
     */
    /* JADX WARNING: Missing block: B:15:0x004a, code skipped:
            if (r11 == null) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:17:0x0050, code skipped:
            if (r11.size() <= 0) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:18:0x0052, code skipped:
            r5.addAll(r11);
            r6.addAll(r11);
     */
    /* JADX WARNING: Missing block: B:19:0x0058, code skipped:
            r13 = r1.mAwareUserHabitAlgorithm.getForceProtectAppsFromLRU(com.android.server.rms.iaware.appmng.AwareAppAssociate.getInstance().getDefaultHomePackages(), r7);
     */
    /* JADX WARNING: Missing block: B:20:0x0066, code skipped:
            if (r13 == null) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:22:0x006c, code skipped:
            if (r13.size() <= 0) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:23:0x006e, code skipped:
            r14 = r13.iterator();
     */
    /* JADX WARNING: Missing block: B:25:0x0076, code skipped:
            if (r14.hasNext() == false) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:26:0x0078, code skipped:
            r15 = (java.lang.String) r14.next();
     */
    /* JADX WARNING: Missing block: B:27:0x0082, code skipped:
            if (r6.contains(r15) != false) goto L_0x0087;
     */
    /* JADX WARNING: Missing block: B:28:0x0084, code skipped:
            r5.add(r15);
     */
    /* JADX WARNING: Missing block: B:30:0x0088, code skipped:
            r14 = TAG;
            r15 = new java.lang.StringBuilder();
            r17 = r0;
            r15.append("getForceProtectApps spend time:");
            r20 = r6;
            r21 = r7;
            r15.append(java.lang.System.currentTimeMillis() - r3);
            r15.append(" ms result:");
            r15.append(r5);
            android.rms.iaware.AwareLog.d(r14, r15.toString());
     */
    /* JADX WARNING: Missing block: B:31:0x00b2, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getForceProtectApps(int num) {
        Throwable th;
        Set<String> set;
        AwareLog.i(TAG, "getForceProtectApps is called");
        if (num <= 0 || this.mAwareHSMListHandler == null || this.mAwareUserHabitAlgorithm == null) {
            return null;
        }
        long start_time = System.currentTimeMillis();
        ArrayList result = new ArrayList();
        Set<String> filterSet = this.mAwareHSMListHandler.getUnProtectSet();
        synchronized (this) {
            try {
                int lruCount = this.mLruCount;
                try {
                    int emailCount = this.mEmailCount;
                    int imCount = this.mImCount;
                    int mostUsedCount = this.mMostUsedCount;
                } catch (Throwable th2) {
                    th = th2;
                    set = filterSet;
                    int i = lruCount;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                set = filterSet;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    private void updateHabitConfig() {
        Map<String, Integer> config = IAwareHabitUtils.getConfigFromCMS(IAwareHabitUtils.APPMNG, IAwareHabitUtils.HABIT_CONFIG);
        if (config != null) {
            synchronized (this) {
                if (config.containsKey(IAwareHabitUtils.HABIT_LRU_COUNT)) {
                    this.mLruCount = ((Integer) config.get(IAwareHabitUtils.HABIT_LRU_COUNT)).intValue();
                }
                if (config.containsKey(IAwareHabitUtils.HABIT_EMAIL_COUNT)) {
                    this.mEmailCount = ((Integer) config.get(IAwareHabitUtils.HABIT_EMAIL_COUNT)).intValue();
                }
                if (config.containsKey(IAwareHabitUtils.HABIT_IM_COUNT)) {
                    this.mImCount = ((Integer) config.get(IAwareHabitUtils.HABIT_IM_COUNT)).intValue();
                }
                if (config.containsKey(IAwareHabitUtils.HABIT_MOST_USED_COUNT)) {
                    this.mMostUsedCount = ((Integer) config.get(IAwareHabitUtils.HABIT_MOST_USED_COUNT)).intValue();
                }
                if (config.containsKey(IAwareHabitUtils.HABIT_LOW_END)) {
                    this.mLowEndDeviceStayInBgTime = ((long) ((Integer) config.get(IAwareHabitUtils.HABIT_LOW_END)).intValue()) * 60000;
                }
                if (config.containsKey(IAwareHabitUtils.HABIT_HIGH_END)) {
                    this.mHighEndDeviceStayInBgTime = ((long) ((Integer) config.get(IAwareHabitUtils.HABIT_HIGH_END)).intValue()) * 60000;
                }
            }
        }
    }

    public LinkedHashMap<String, Long> getLruCache() {
        if (this.mAwareUserHabitAlgorithm != null) {
            return this.mAwareUserHabitAlgorithm.getLruCache();
        }
        return null;
    }

    public Set<String> getBackgroundApps(long duringTime) {
        if (duringTime <= 0) {
            return null;
        }
        LinkedHashMap<String, Long> lru = getLruCache();
        if (lru == null) {
            return null;
        }
        duringTime *= 1000;
        ArraySet<String> result = new ArraySet();
        ArrayList<String> pkgList = new ArrayList(lru.keySet());
        long now = SystemClock.elapsedRealtime();
        for (int i = pkgList.size() - 1; i >= 0; i--) {
            String pkg = (String) pkgList.get(i);
            if (now - ((Long) lru.get(pkg)).longValue() > duringTime) {
                break;
            }
            result.add(pkg);
        }
        return result;
    }

    public String getLastPkgName() {
        return this.mAppMngLastPkgName;
    }

    /* JADX WARNING: Missing block: B:40:0x0150, code skipped:
            return r19;
     */
    /* JADX WARNING: Missing block: B:44:0x0157, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized List<String> recognizeLongTimeRunningApps() {
        synchronized (this) {
            AwareLog.i(TAG, "recognizeLongTimeRunningApps is called");
            List<Integer> keyPidList = new ArrayList();
            ArrayMap<Integer, ArrayList<String>> pidToPkgMap = new ArrayMap();
            getRunningPidInfo(keyPidList, pidToPkgMap);
            long stayInBackgroudTime = this.mHighEndDeviceStayInBgTime;
            if (this.mIsLowEnd) {
                stayInBackgroudTime = this.mLowEndDeviceStayInBgTime;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stayInBackgroudTime=");
            stringBuilder.append(stayInBackgroudTime);
            AwareLog.i(str, stringBuilder.toString());
            Map<String, Long> killingPkgMap = getLongTimeRunningPkgs(keyPidList, stayInBackgroudTime);
            ArrayMap<Integer, ArrayList<String>> arrayMap;
            List<Integer> list;
            if (killingPkgMap == null) {
                arrayMap = pidToPkgMap;
            } else if (killingPkgMap.isEmpty()) {
                list = keyPidList;
                arrayMap = pidToPkgMap;
            } else {
                String p;
                Set<String> killingPkgSet = new ArraySet();
                int keyPidListSize = keyPidList.size();
                for (int i = 0; i < keyPidListSize; i++) {
                    ArraySet<Integer> strong = new ArraySet();
                    AwareAppAssociate.getInstance().getAssocListForPid(((Integer) keyPidList.get(i)).intValue(), strong);
                    int strongSize = strong.size();
                    int j = 0;
                    while (j < strongSize) {
                        int pid = ((Integer) strong.valueAt(j)).intValue();
                        if (pidToPkgMap.containsKey(Integer.valueOf(pid))) {
                            ArrayList<String> pkg = (ArrayList) pidToPkgMap.get(Integer.valueOf(pid));
                            int pkgSize = pkg.size();
                            int k = 0;
                            while (true) {
                                int pkgSize2 = pkgSize;
                                int k2 = k;
                                if (k2 >= pkgSize2) {
                                    break;
                                }
                                String p2 = (String) pkg.get(k2);
                                list = keyPidList;
                                keyPidList = TAG;
                                arrayMap = pidToPkgMap;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                int pkgSize3 = pkgSize2;
                                stringBuilder2.append("strong associate app:");
                                p = p2;
                                stringBuilder2.append(p);
                                AwareLog.d(keyPidList, stringBuilder2.toString());
                                killingPkgMap.remove(p);
                                k = k2 + 1;
                                keyPidList = list;
                                pidToPkgMap = arrayMap;
                                pkgSize = pkgSize3;
                            }
                        }
                        j++;
                        keyPidList = keyPidList;
                        pidToPkgMap = pidToPkgMap;
                    }
                    arrayMap = pidToPkgMap;
                }
                arrayMap = pidToPkgMap;
                keyPidList = killingPkgMap.entrySet().iterator();
                while (keyPidList.hasNext()) {
                    Entry entry = (Entry) keyPidList.next();
                    p = (String) entry.getKey();
                    if (SystemClock.elapsedRealtime() - ((Long) entry.getValue()).longValue() >= stayInBackgroudTime) {
                        killingPkgSet.add(p);
                    }
                }
                pidToPkgMap = killingPkgSet.size();
                if (pidToPkgMap > null && this.mAwareUserHabitRadar != null) {
                    this.mAwareUserHabitRadar.insertStatisticData("habit_kill", 0, pidToPkgMap);
                    p = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("recognizeLongTimeRunningApps  num:");
                    stringBuilder3.append(pidToPkgMap);
                    AwareLog.d(p, stringBuilder3.toString());
                }
                List<String> arrayList = pidToPkgMap > null ? new ArrayList(killingPkgSet) : null;
            }
        }
    }

    private Map<String, Long> getLongTimeRunningPkgs(List<Integer> pidList, long bgtime) {
        if (this.mAwareUserHabitAlgorithm == null || this.mAwareHSMListHandler == null) {
            return null;
        }
        Map<String, Long> lruPkgMap = this.mAwareUserHabitAlgorithm.getLongTimePkgsFromLru(bgtime);
        if (lruPkgMap == null) {
            return null;
        }
        Map<String, Long> ltrPkgs = new ArrayMap();
        List<Integer> rulist = new ArrayList();
        Set<String> filterSet = this.mAwareHSMListHandler.getProtectSet();
        AppTypeRecoManager recomanger = AppTypeRecoManager.getInstance();
        int pidListSize = pidList.size();
        for (int i = 0; i < pidListSize; i++) {
            String pkg = InnerUtils.getAwarePkgName(((Integer) pidList.get(i)).intValue());
            if (filterSet.contains(pkg)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hsm filter set pkg:");
                stringBuilder.append(pkg);
                AwareLog.d(str, stringBuilder.toString());
            } else if (pkg == null || !lruPkgMap.containsKey(pkg)) {
                rulist.add((Integer) pidList.get(i));
            } else {
                int type = recomanger.getAppType(pkg);
                if (!(5 == type || 2 == type || 310 == type || 301 == type)) {
                    ltrPkgs.put(pkg, (Long) lruPkgMap.get(pkg));
                }
            }
        }
        pidList.clear();
        pidList.addAll(rulist);
        return ltrPkgs;
    }

    private void getRunningPidInfo(List<Integer> pidList, Map<Integer, ArrayList<String>> pidToPkgMap) {
        ArrayList<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        int procListSize = procList.size();
        for (int i = 0; i < procListSize; i++) {
            ProcessInfo process = (ProcessInfo) procList.get(i);
            if (process != null && UserHandle.getAppId(process.mAppUid) > 10000) {
                int pid = Integer.valueOf(process.mPid).intValue();
                ArrayList<String> plist = new ArrayList();
                plist.addAll(process.mPackageName);
                pidToPkgMap.put(Integer.valueOf(pid), plist);
                pidList.add(Integer.valueOf(pid));
            }
        }
    }

    public List<String> getMostFrequentUsedApp(int n, int minCount) {
        AwareLog.i(TAG, "getMostFrequentUsedApp is called");
        if (this.mAwareUserHabitAlgorithm == null || n <= 0) {
            return null;
        }
        return this.mAwareUserHabitAlgorithm.getMostFrequentUsedApp(n, minCount, null);
    }

    public List<String> getMostFreqAppByType(int appType, int appNum) {
        if (this.mAwareUserHabitAlgorithm == null) {
            return null;
        }
        if (appNum == -1) {
            int i = 3;
            int i2 = 2;
            if (appType == 0) {
                if (this.mIsLowEnd) {
                    i = 2;
                }
                appNum = i;
            } else if (1 == appType) {
                if (this.mIsLowEnd) {
                    i2 = 1;
                }
                appNum = i2;
            } else if (11 == appType) {
                if (!this.mIsLowEnd) {
                    i = 5;
                }
                appNum = i;
            } else if (34 == appType) {
                appNum = 3;
            }
        }
        if (appNum <= 0) {
            return null;
        }
        return this.mAwareUserHabitAlgorithm.getMostFreqAppByType(appType, appNum);
    }

    public List<String> getHabitProtectList(int emailCount, int imCount) {
        if (imCount <= 0 || emailCount <= 0 || this.mAwareUserHabitAlgorithm == null) {
            return null;
        }
        return this.mAwareUserHabitAlgorithm.getHabitProtectList(emailCount, imCount);
    }

    public List<String> getHabitProtectListAll(int emailCount, int imCount) {
        if (imCount <= 0 || emailCount <= 0 || this.mAwareUserHabitAlgorithm == null) {
            return null;
        }
        return this.mAwareUserHabitAlgorithm.getHabitProtectAppsAll(emailCount, imCount);
    }

    public List<String> getTopN(int n) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTopN is called n:");
        stringBuilder.append(n);
        AwareLog.d(str, stringBuilder.toString());
        if (n <= 0) {
            return null;
        }
        List<String> list = null;
        if (this.mIsNewAlgorithmSwitched) {
            n = this.mDynamicTopN > n ? n : this.mDynamicTopN;
            list = new ArrayList(n);
            synchronized (this.mUserTrackListVer2) {
                Iterator it = new ArrayList(this.mUserTrackListVer2.entrySet()).iterator();
                while (it.hasNext()) {
                    Entry<String, Integer> entry = (Entry) it.next();
                    if (n == 0) {
                        break;
                    }
                    list.add((String) entry.getKey());
                    n--;
                }
            }
        } else if (this.mAwareUserHabitAlgorithm != null) {
            list = this.mAwareUserHabitAlgorithm.getTopN(n);
        }
        return list;
    }

    public Set<String> getFilterApp() {
        if (this.mAwareUserHabitAlgorithm != null) {
            return this.mAwareUserHabitAlgorithm.getFilterApp();
        }
        return null;
    }

    public Map<Integer, Integer> getTopList(Map<Integer, AwareProcessInfo> appProcesses) {
        AwareLog.i(TAG, "getTopList is called");
        if (appProcesses == null) {
            return null;
        }
        return getUserTopList(appProcesses);
    }

    private Map<Integer, Integer> getUserTopList(Map<Integer, AwareProcessInfo> appProcesses) {
        List<Entry<Integer, Integer>> list = new ArrayList();
        ArrayMap<Integer, Integer> map = new ArrayMap();
        sortPidByPkgs(list, appProcesses);
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            Entry<Integer, Integer> entry = (Entry) list.get(i);
            map.put(Integer.valueOf(((Integer) entry.getKey()).intValue()), Integer.valueOf(((Integer) entry.getValue()).intValue()));
        }
        return map;
    }

    public void reportHabitData(Bundle bdl) {
        if (bdl != null) {
            int topN = bdl.getInt(REPORT_DYNAMIC_TOPN_KEY, -1);
            if (topN >= 0) {
                this.mDynamicTopN = topN;
            }
            List<String> sortedAppList = bdl.getStringArrayList(REPORT_SORT_KEY);
            if (sortedAppList != null) {
                setTopList(sortedAppList);
                return;
            }
            List<String> smallSampleList = bdl.getStringArrayList(REPORT_SMALL_SAMPLE_LIST_KEY);
            if (smallSampleList != null) {
                AwareIntelligentRecg.getInstance().setSmallSampleList(smallSampleList);
                return;
            }
            if (bdl.getBoolean(REPORT_SWITCHOFF_KEY, false)) {
                this.mIsNewAlgorithmSwitched = false;
            }
        }
    }

    private void setTopList(List<String> topList) {
        if (topList != null) {
            synchronized (this.mUserTrackListVer2) {
                this.mUserTrackListVer2.clear();
                int topListSize = topList.size();
                for (int i = 0; i < topListSize; i++) {
                    this.mUserTrackListVer2.put((String) topList.get(i), Integer.valueOf(i + 1));
                }
            }
            this.mIsNewAlgorithmSwitched = true;
        }
    }

    private Map<String, Integer> getUserTrackListVer2() {
        LinkedHashMap<String, Integer> result = new LinkedHashMap();
        synchronized (this.mUserTrackListVer2) {
            result.putAll(this.mUserTrackListVer2);
        }
        return result;
    }

    public Map<String, Integer> getAllTopList() {
        if (this.mIsNewAlgorithmSwitched) {
            return getUserTrackListVer2();
        }
        return this.mAwareUserHabitAlgorithm.getUserTrackList();
    }

    private void sortPidByPkgs(List<Entry<Integer, Integer>> list, Map<Integer, AwareProcessInfo> appProcesses) {
        if (this.mAwareUserHabitAlgorithm != null) {
            Map<String, Integer> result;
            if (this.mIsNewAlgorithmSwitched) {
                result = getUserTrackListVer2();
            } else {
                result = this.mAwareUserHabitAlgorithm.getUserTrackList();
            }
            for (Entry<Integer, AwareProcessInfo> entry : appProcesses.entrySet()) {
                AwareProcessInfo info = (AwareProcessInfo) entry.getValue();
                if (this.mUserId.get() == UserHandle.getUserId(info.mProcInfo.mUid)) {
                    ArrayList<String> pkgList = info.mProcInfo.mPackageName;
                    int pid = info.mPid;
                    int j = 0;
                    if (pkgList.size() != 1) {
                        int index = -1;
                        int pkgListSize = pkgList.size();
                        while (j < pkgListSize) {
                            if (result.containsKey(pkgList.get(j))) {
                                if (index == -1) {
                                    index = ((Integer) result.get(pkgList.get(j))).intValue();
                                } else {
                                    int order = ((Integer) result.get(pkgList.get(j))).intValue();
                                    if (order < index) {
                                        index = order;
                                    }
                                }
                            }
                            j++;
                        }
                        if (index != -1) {
                            list.add(new SimpleEntry(Integer.valueOf(pid), Integer.valueOf(index)));
                        }
                    } else if (result.containsKey(pkgList.get(0))) {
                        list.add(new SimpleEntry(Integer.valueOf(pid), result.get(pkgList.get(0))));
                    }
                }
            }
        }
    }

    public void report(int eventId, Bundle args) {
        if (mEnabled && args != null) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 2;
            msg.arg1 = eventId;
            msg.setData(args);
            this.mHandler.sendMessage(msg);
        }
    }

    public void reportScreenState(int state) {
        if (state == ResourceType.RESOURCE_SCREEN_OFF.ordinal()) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 4;
            msg.arg1 = 7;
            this.mHandler.sendMessage(msg);
        }
    }

    private void registerObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "AwareUserHabit register process observer failed");
        }
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(this.mUserSwitchObserver, TAG);
        } catch (RemoteException e2) {
            AwareLog.e(TAG, "AwareUserHabit registerUserSwitchObserver failed!");
        }
    }

    private void unregisterObserver() {
        try {
            ActivityManagerNative.getDefault().unregisterProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "AwareUserHabit unregister process observer failed");
        }
        try {
            ActivityManagerNative.getDefault().unregisterUserSwitchObserver(this.mUserSwitchObserver);
        } catch (RemoteException e2) {
            AwareLog.e(TAG, "AwareUserHabit unregisterProcessObserver failed!");
        }
    }

    private void setUserId(int userId) {
        if (this.mAwareHSMListHandler != null) {
            this.mAwareHSMListHandler.setUserId(userId);
        }
        if (this.mAwareUserHabitAlgorithm != null) {
            this.mAwareUserHabitAlgorithm.setUserId(userId);
        }
    }

    public void registHabitProtectListChangeListener(HabitProtectListChangeListener listener) {
        if (this.mAwareUserHabitAlgorithm != null) {
            this.mAwareUserHabitAlgorithm.registHabitProtectListChangeListener(listener);
        }
    }

    public void unregistHabitProtectListChangeListener(HabitProtectListChangeListener listener) {
        if (this.mAwareUserHabitAlgorithm != null) {
            this.mAwareUserHabitAlgorithm.unregistHabitProtectListChangeListener(listener);
        }
    }

    public List<String> queryHabitProtectAppList(int imCount, int emailCount) {
        if (this.mAwareUserHabitAlgorithm != null) {
            return this.mAwareUserHabitAlgorithm.queryHabitProtectAppList(imCount, emailCount);
        }
        return null;
    }

    public List<String> getGCMAppList() {
        if (this.mAwareUserHabitAlgorithm != null) {
            return this.mAwareUserHabitAlgorithm.getGCMAppsList();
        }
        return null;
    }

    public void dumpHabitProtectList(PrintWriter pw) {
        if (this.mAwareUserHabitAlgorithm != null) {
            this.mAwareUserHabitAlgorithm.dumpHabitProtectList(pw);
        }
    }

    public ArrayList<StatisticsData> getStatisticsData() {
        ArrayList<StatisticsData> tempList = new ArrayList();
        if (this.mAwareUserHabitRadar != null) {
            ArrayList<StatisticsData> list = this.mAwareUserHabitRadar.getStatisticsData();
            if (list != null) {
                tempList.addAll(list);
            }
        }
        return tempList;
    }
}
