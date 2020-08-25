package com.android.server.rms.iaware.appmng;

import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IProcessObserver;
import android.app.IUserSwitchObserver;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LruCache;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.app.ProcessMap;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.MemInfoReader;
import com.android.server.HwBluetoothManagerServiceEx;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessWindowInfo;
import com.android.server.mtm.iaware.appmng.appclean.CrashClean;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.SystemUnremoveUidCache;
import com.android.server.mtm.utils.SparseSet;
import com.android.server.rms.iaware.AwareCallback;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.qos.AwareBinderSchedManager;
import com.android.server.rms.iaware.srms.AppCleanupFeature;
import com.android.server.security.permissionmanager.util.PermConst;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.view.HwWindowManager;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AwareAppAssociate {
    public static final int ASSOC_DECAY_MIN_TIME = 120000;
    public static final int ASSOC_REPORT_MIN_TIME = 60000;
    public static final int CLEAN_LEVEL = 0;
    private static final int CONSTANT_MORE_PREVIOUS_DISABLE = 0;
    private static final int CONSTANT_MORE_PREVIOUS_LEVEL1 = 1;
    private static final int CONSTANT_MORE_PREVIOUS_LEVEL2 = 2;
    private static final int CONSTANT_MORE_PREVIOUS_LEVEL_HIGH = 4;
    private static final int COUNT_PROTECT_DEFAULT = 1;
    private static final int COUNT_PROTECT_HIGH = 4;
    private static final int COUNT_PROTECT_MORE = 2;
    /* access modifiers changed from: private */
    public static boolean DEBUG = false;
    public static final int FIRST_START_TIMES = 1;
    private static final int FIVE_SECONDS = 5000;
    private static final int HIGH_MEM_THRESHOLD = 6144;
    private static final String INTERNALAPP_PKGNAME = "com.huawei.android.internal.app";
    private static final int INVALID_VALUE = -1;
    private static final int LOW_MEM_THRESHOLD = 3072;
    private static final int MSG_CHECK_RECENT_FORE = 3;
    private static final int MSG_CLEAN = 2;
    private static final int MSG_CLEAR_BAKUP_VISWIN = 4;
    private static final int MSG_INIT = 1;
    public static final int MS_TO_SEC = 1000;
    private static final int ONE_SECOND = 1000;
    private static final String PERMISSIOM_CONTROLLER = "com.android.permissioncontroller";
    private static final int PREVIOUS_HIGH_DEFAULT = -1;
    private static final int PREVIOUS_HIGH_DISABLE = 0;
    private static final int PREVIOUS_HIGH_ENABLE = 1;
    private static final int RECENT_TIME_INTERVAL = 10000;
    private static boolean RECORD = false;
    private static final long REINIT_TIME = 2000;
    public static final int RESTART_MAX_INTERVAL = 300;
    public static final int RESTART_MAX_TIMES = 30;
    private static final int SMCS_APP_WIDGET_SERVICE_GET_BY_USERID = 2;
    private static final String SYSTEM = "system";
    private static final String SYSTEM_UI_PKGNAME = "com.android.systemui";
    private static final String TAG = "RMS.AwareAppAssociate";
    private static final int VISIBLEWINDOWS_ADD_WINDOW = 4;
    private static final int VISIBLEWINDOWS_CACHE_CHANGE_MODE = 3;
    private static final int VISIBLEWINDOWS_CACHE_CLR = 2;
    private static final int VISIBLEWINDOWS_CACHE_DEL = 1;
    private static final int VISIBLEWINDOWS_CACHE_UPDATE = 0;
    private static final int VISIBLEWINDOWS_REMOVE_WINDOW = 5;
    private static final int WIDGET_INVISIBLE = 0;
    private static final int WIDGET_VISIBLE = 1;
    private static AwareAppAssociate mAwareAppAssociate = null;
    private static boolean mEnabled = false;
    private final AwareAppLruBase mAmsPrevBase;
    private final SparseArray<AssocPidRecord> mAssocRecordMap;
    /* access modifiers changed from: private */
    public final ArrayMap<Integer, ProcessData> mBgRecentForcePids;
    private final ArraySet<IAwareVisibleCallback> mCallbacks;
    /* access modifiers changed from: private */
    public int mCurSwitchUser;
    /* access modifiers changed from: private */
    public int mCurUserId;
    /* access modifiers changed from: private */
    public final SparseIntArray mForePids;
    /* access modifiers changed from: private */
    public AppAssocHandler mHandler;
    private ArrayList<String> mHomePackageList;
    /* access modifiers changed from: private */
    public int mHomeProcessPid;
    private int mHomeProcessUid;
    private HwActivityManagerService mHwAMS;
    private AtomicBoolean mIsInitialized;
    private boolean mIsLowDevice;
    private int mIsPreviousHighEnable;
    private final Object mLock;
    private LruCache<Integer, AwareAppLruBase> mLruCache;
    private int mMorePreviousLevel;
    /* access modifiers changed from: private */
    public MultiTaskManagerService mMtmService;
    private int mMyPid;
    private final AwareAppLruBase mPrevNonHomeBase;
    private final ProcessMap<AssocBaseRecord> mProcInfoMap;
    private Map<Integer, Map<String, Map<String, LaunchData>>> mProcLaunchMap;
    private final SparseArray<AssocBaseRecord> mProcPidMap;
    private final ArrayMap<String, SparseSet> mProcPkgMap;
    private final SparseArray<SparseSet> mProcUidMap;
    private IProcessObserver mProcessObserver;
    private final AwareAppLruBase mRecentTaskPrevBase;
    private boolean mScreenOff;
    private SystemUnremoveUidCache mSystemUnremoveUidCache;
    IUserSwitchObserver mUserSwitchObserver;
    private final SparseSet mVisWinDurScreenOff;
    private ArrayMap<Integer, AwareProcessWindowInfo> mVisibleWindows;
    private ArrayMap<Integer, AwareProcessWindowInfo> mVisibleWindowsCache;
    private SparseArray<SparseArray<Widget>> mWidgets;

    public interface IAwareVisibleCallback {
        void onVisibleWindowsChanged(int i, int i2, int i3);
    }

    /* access modifiers changed from: private */
    public static class ProcessData {
        /* access modifiers changed from: private */
        public long mTimeStamp;
        /* access modifiers changed from: private */
        public int mUid;

        private ProcessData(int uid, long timeStamp) {
            this.mUid = uid;
            this.mTimeStamp = timeStamp;
        }
    }

    /* access modifiers changed from: private */
    public void checkRecentForce() {
        int removeCount = 0;
        long curTime = SystemClock.elapsedRealtime();
        synchronized (this.mBgRecentForcePids) {
            for (int i = this.mBgRecentForcePids.size() - 1; i >= 0; i--) {
                if (curTime - this.mBgRecentForcePids.valueAt(i).mTimeStamp > 10000) {
                    this.mBgRecentForcePids.removeAt(i);
                    removeCount++;
                }
            }
        }
        AwareLog.d(TAG, "checkRecentForce removeCount: " + removeCount);
    }

    private void registerProcessObserver() {
        AwareCallback.getInstance().registerProcessObserver(this.mProcessObserver);
    }

    private void unregisterProcessObserver() {
        AwareCallback.getInstance().unregisterProcessObserver(this.mProcessObserver);
    }

    private LinkedHashMap<Integer, AwareAppLruBase> getActivityLruCache() {
        LinkedHashMap<Integer, AwareAppLruBase> lru = null;
        synchronized (this.mLruCache) {
            Map<Integer, AwareAppLruBase> tmp = this.mLruCache.snapshot();
            if (tmp instanceof LinkedHashMap) {
                lru = (LinkedHashMap) tmp;
            }
        }
        return lru;
    }

    private boolean updateActivityLruCache(int pid, int uid) {
        long timeNow = SystemClock.elapsedRealtime();
        synchronized (this.mLruCache) {
            if (this.mLruCache.size() == 0) {
                this.mLruCache.put(Integer.valueOf(uid), new AwareAppLruBase(pid, uid, timeNow));
                return false;
            }
            LinkedHashMap<Integer, AwareAppLruBase> lru = getActivityLruCache();
            if (lru == null) {
                return false;
            }
            List<Integer> list = new ArrayList<>(lru.keySet());
            if (list.size() < 1) {
                return false;
            }
            int prevUid = list.get(list.size() - 1).intValue();
            AwareAppLruBase lruBase = lru.get(Integer.valueOf(prevUid));
            if (lruBase == null) {
                return false;
            }
            if (prevUid == uid) {
                this.mLruCache.put(Integer.valueOf(prevUid), new AwareAppLruBase(pid, prevUid, lruBase.mTime));
                return false;
            }
            if (isSystemDialogProc(lruBase.mPid, prevUid, lruBase.mTime, timeNow)) {
                this.mLruCache.remove(Integer.valueOf(prevUid));
            } else {
                this.mLruCache.put(Integer.valueOf(prevUid), new AwareAppLruBase(lruBase.mPid, prevUid, timeNow));
            }
            this.mLruCache.put(Integer.valueOf(uid), new AwareAppLruBase(pid, uid, timeNow));
            return true;
        }
    }

    private void updatePrevApp(int pid, int uid) {
        LinkedHashMap<Integer, AwareAppLruBase> lru;
        List<Integer> list;
        int listSize;
        if (!isAppLock(uid) && updateActivityLruCache(pid, uid) && (lru = getActivityLruCache()) != null && (listSize = (list = new ArrayList<>(lru.keySet())).size()) >= 2) {
            int prevUid = list.get(listSize - 2).intValue();
            if (prevUid != this.mHomeProcessUid) {
                AwareAppLruBase.copyLruBaseInfo(lru.get(Integer.valueOf(prevUid)), this.mPrevNonHomeBase);
            } else if (listSize < 3) {
                this.mPrevNonHomeBase.setInitValue();
            } else {
                AwareAppLruBase.copyLruBaseInfo(lru.get(Integer.valueOf(list.get(listSize - 3).intValue())), this.mPrevNonHomeBase);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updatePreviousAppInfo(int pid, int uid, boolean foregroundActivities, SparseIntArray forePids) {
        if (this.mHwAMS != null) {
            if (!foregroundActivities) {
                if (forePids != null) {
                    if (forePids.indexOfValue(uid) < 0 && pid != this.mHomeProcessPid) {
                        this.mRecentTaskPrevBase.setValue(pid, uid, SystemClock.elapsedRealtime());
                    }
                    for (int i = forePids.size() - 1; i >= 0; i--) {
                        int forePid = forePids.keyAt(i);
                        if (isForgroundPid(forePid)) {
                            updatePrevApp(forePid, forePids.valueAt(i));
                            return;
                        }
                    }
                }
            } else if (isForgroundPid(pid)) {
                updatePrevApp(pid, uid);
            }
        }
    }

    private boolean isSystemDialogProc(int pid, int uid, long prevActTime, long curTime) {
        if (UserHandle.getAppId(uid) != 1000) {
            return false;
        }
        synchronized (this.mLock) {
            AssocBaseRecord br = this.mProcPidMap.get(pid);
            if (br != null) {
                if (br.pkgList != null) {
                    if (br.pkgList.size() != 1) {
                        return false;
                    }
                    if (br.pkgList.contains(INTERNALAPP_PKGNAME)) {
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }
    }

    private boolean isForgroundPid(int pid) {
        if (this.mHwAMS.getProcessBaseInfo(pid).curAdj == 0) {
            return true;
        }
        return false;
    }

    public static boolean isDealAsPkgUid(int uid) {
        int appId = UserHandle.getAppId(uid);
        return appId >= 1000 && appId <= 1001;
    }

    private static final class AssocBaseRecord {
        public boolean isStrong = true;
        public HashSet<String> mComponents = new HashSet<>();
        public ArraySet<String> mComponentsJob = new ArraySet<>();
        public long miniTime;
        public int pid;
        public ArraySet<String> pkgList = new ArraySet<>();
        public String processName;
        public int uid;

        public AssocBaseRecord(String name, int uid2, int pid2) {
            this.processName = name;
            this.uid = uid2;
            this.pid = pid2;
            this.miniTime = SystemClock.elapsedRealtime();
        }
    }

    private final class AssocPidRecord {
        public final ProcessMap<AssocBaseRecord> mAssocBindService = new ProcessMap<>();
        public final ProcessMap<AssocBaseRecord> mAssocProvider = new ProcessMap<>();
        public int pid;
        public String processName;
        public int uid;

        public AssocPidRecord(int pid2, int uid2, String name) {
            this.pid = pid2;
            this.uid = uid2;
            this.processName = name;
        }

        public ProcessMap<AssocBaseRecord> getMap(int type) {
            if (type != 1) {
                if (type == 2) {
                    return this.mAssocProvider;
                }
                if (type != 3) {
                    return null;
                }
            }
            return this.mAssocBindService;
        }

        public boolean isEmpty() {
            return this.mAssocBindService.getMap().isEmpty() && this.mAssocProvider.getMap().isEmpty();
        }

        public int size() {
            return this.mAssocBindService.getMap().size() + this.mAssocProvider.getMap().size();
        }

        public String toString() {
            String sameUid;
            AssocPidRecord assocPidRecord = this;
            StringBuilder sb = new StringBuilder();
            sb.append("Pid:");
            sb.append(assocPidRecord.pid);
            String str = ",Uid:";
            sb.append(str);
            sb.append(assocPidRecord.uid);
            String str2 = ",ProcessName:";
            sb.append(str2);
            sb.append(assocPidRecord.processName);
            sb.append("\n");
            String sameUid2 = AwareAppAssociate.this.sameUid(assocPidRecord.pid);
            if (sameUid2 != null) {
                sb.append(sameUid2);
            }
            int NP = assocPidRecord.mAssocBindService.getMap().size();
            boolean flag = true;
            for (int i = 0; i < NP; i++) {
                SparseArray<AssocBaseRecord> brs = (SparseArray) assocPidRecord.mAssocBindService.getMap().valueAt(i);
                int NB = brs.size();
                int j = 0;
                while (j < NB) {
                    AssocBaseRecord br = brs.valueAt(j);
                    if (flag) {
                        sameUid = sameUid2;
                        sb.append("    [BindService] depend on:\n");
                        flag = false;
                    } else {
                        sameUid = sameUid2;
                    }
                    Iterator<String> it = br.mComponents.iterator();
                    while (it.hasNext()) {
                        sb.append("        Pid:");
                        sb.append(br.pid);
                        sb.append(str);
                        sb.append(br.uid);
                        sb.append(str2);
                        sb.append(br.processName);
                        sb.append(",Time:");
                        sb.append(SystemClock.elapsedRealtime() - br.miniTime);
                        sb.append(",Component:");
                        sb.append(it.next());
                        sb.append("\n");
                        NP = NP;
                        it = it;
                        flag = flag;
                    }
                    Iterator<String> it2 = br.mComponentsJob.iterator();
                    while (it2.hasNext()) {
                        sb.append("        [jobService] Pid:");
                        sb.append(br.pid);
                        sb.append(str);
                        sb.append(br.uid);
                        sb.append(str2);
                        sb.append(br.processName);
                        sb.append(",componentJob:");
                        sb.append(it2.next());
                        sb.append(System.lineSeparator());
                    }
                    j++;
                    sameUid2 = sameUid;
                    NP = NP;
                    flag = flag;
                }
            }
            int NP2 = assocPidRecord.mAssocProvider.getMap().size();
            boolean flag2 = true;
            int i2 = 0;
            while (i2 < NP2) {
                SparseArray<AssocBaseRecord> brs2 = (SparseArray) assocPidRecord.mAssocProvider.getMap().valueAt(i2);
                int NB2 = brs2.size();
                for (int j2 = 0; j2 < NB2; j2++) {
                    AssocBaseRecord br2 = brs2.valueAt(j2);
                    if (flag2) {
                        sb.append("    [Provider] depend on:\n");
                        flag2 = false;
                    }
                    Iterator<String> it3 = br2.mComponents.iterator();
                    while (it3.hasNext()) {
                        String component = it3.next();
                        if (SystemClock.elapsedRealtime() - br2.miniTime >= 120000) {
                            NP2 = NP2;
                            flag2 = flag2;
                        } else {
                            sb.append("        Pid:");
                            sb.append(br2.pid);
                            sb.append(str);
                            sb.append(br2.uid);
                            sb.append(str2);
                            sb.append(br2.processName);
                            sb.append(",Time:");
                            sb.append(SystemClock.elapsedRealtime() - br2.miniTime);
                            sb.append(",Component:");
                            sb.append(component);
                            sb.append(",Strong:");
                            sb.append(br2.isStrong);
                            sb.append("\n");
                            str = str;
                            str2 = str2;
                            NP2 = NP2;
                            flag2 = flag2;
                        }
                    }
                }
                i2++;
                assocPidRecord = this;
            }
            return sb.toString();
        }
    }

    private AwareAppAssociate() {
        this.mMyPid = Process.myPid();
        this.mIsInitialized = new AtomicBoolean(false);
        this.mHandler = null;
        this.mForePids = new SparseIntArray();
        this.mLruCache = new LruCache<>(9);
        this.mMorePreviousLevel = 0;
        this.mProcLaunchMap = new HashMap();
        this.mPrevNonHomeBase = new AwareAppLruBase();
        this.mRecentTaskPrevBase = new AwareAppLruBase();
        this.mAmsPrevBase = new AwareAppLruBase();
        this.mCurUserId = 0;
        this.mCurSwitchUser = 0;
        this.mVisibleWindows = new ArrayMap<>();
        this.mVisibleWindowsCache = new ArrayMap<>();
        this.mWidgets = new SparseArray<>();
        this.mHomeProcessPid = 0;
        this.mHomeProcessUid = 0;
        this.mHomePackageList = new ArrayList<>();
        this.mLock = new Object();
        this.mAssocRecordMap = new SparseArray<>();
        this.mProcInfoMap = new ProcessMap<>();
        this.mProcPidMap = new SparseArray<>();
        this.mProcUidMap = new SparseArray<>();
        this.mProcPkgMap = new ArrayMap<>();
        this.mCallbacks = new ArraySet<>();
        this.mVisWinDurScreenOff = new SparseSet();
        this.mScreenOff = false;
        this.mBgRecentForcePids = new ArrayMap<>();
        this.mIsLowDevice = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
        this.mIsPreviousHighEnable = SystemProperties.getInt("persist.sys.iaware.previoushigh", -1);
        this.mProcessObserver = new IProcessObserver.Stub() {
            /* class com.android.server.rms.iaware.appmng.AwareAppAssociate.AnonymousClass1 */

            public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
                if (AwareAppAssociate.DEBUG) {
                    AwareLog.i(AwareAppAssociate.TAG, "Pid:" + pid + ",Uid:" + uid + " come to foreground." + foregroundActivities);
                }
                SparseIntArray forePidsBak = new SparseIntArray();
                synchronized (AwareAppAssociate.this.mForePids) {
                    if (foregroundActivities) {
                        AwareAppAssociate.this.mForePids.put(pid, uid);
                    } else {
                        AwareAppAssociate.this.mForePids.delete(pid);
                    }
                    AwareAppAssociate.this.addAllForSparseIntArray(AwareAppAssociate.this.mForePids, forePidsBak);
                }
                synchronized (AwareAppAssociate.this.mBgRecentForcePids) {
                    if (foregroundActivities) {
                        AwareAppAssociate.this.mBgRecentForcePids.remove(Integer.valueOf(pid));
                    } else {
                        AwareAppAssociate.this.mBgRecentForcePids.put(Integer.valueOf(pid), new ProcessData(uid, SystemClock.elapsedRealtime()));
                        if (AwareAppAssociate.this.mHandler != null) {
                            AwareAppAssociate.this.mHandler.sendEmptyMessageDelayed(3, 10000);
                        }
                    }
                }
                AwareAppAssociate.this.updatePreviousAppInfo(pid, uid, foregroundActivities, forePidsBak);
                AwareSwitchCleanManager.getInstance().notifyFgActivitiesChanged(pid, uid, foregroundActivities);
                AwareAppUseDataManager.getInstance().updateFgActivityChange(pid, uid, foregroundActivities);
                if (foregroundActivities && pid == AwareAppAssociate.this.mHomeProcessPid) {
                    ContinuePowerDevMng.getInstance().tryPreLoadPermanentApplication();
                }
                AwareComponentPreloadManager.getInstance().updateFgActivityChange(pid, uid, foregroundActivities);
            }

            public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
            }

            public void onProcessDied(int pid, int uid) {
                synchronized (AwareAppAssociate.this.mForePids) {
                    AwareAppAssociate.this.mForePids.delete(pid);
                }
                synchronized (AwareAppAssociate.this.mBgRecentForcePids) {
                    AwareAppAssociate.this.mBgRecentForcePids.remove(Integer.valueOf(pid));
                }
                try {
                    AwareAppAssociate.this.removeDiedProcessRelation(pid, uid);
                } catch (NullPointerException e) {
                    AwareLog.d(AwareAppAssociate.TAG, "remove died processrelation failed caused by null pointer");
                } catch (Exception e2) {
                    AwareLog.d(AwareAppAssociate.TAG, "remove died processrelation failed");
                }
                AwareAppAssociate.this.removeDiedRecordProc(uid, pid);
                HwActivityManager.reportProcessDied(pid);
            }
        };
        this.mUserSwitchObserver = new IUserSwitchObserver.Stub() {
            /* class com.android.server.rms.iaware.appmng.AwareAppAssociate.AnonymousClass2 */

            public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                if (reply != null) {
                    try {
                        reply.sendResult((Bundle) null);
                        int unused = AwareAppAssociate.this.mCurSwitchUser = newUserId;
                    } catch (RemoteException e) {
                        AwareLog.e(AwareAppAssociate.TAG, "RemoteException onUserSwitching");
                    }
                }
            }

            public void onUserSwitchComplete(int newUserId) throws RemoteException {
                long startTime = System.currentTimeMillis();
                AwareAppAssociate.this.checkAndInitWidgetObj(newUserId);
                int unused = AwareAppAssociate.this.mCurUserId = newUserId;
                int unused2 = AwareAppAssociate.this.mCurSwitchUser = newUserId;
                AwareAppAssociate awareAppAssociate = AwareAppAssociate.this;
                awareAppAssociate.updateWidgets(awareAppAssociate.mCurUserId);
                AwareIntelligentRecg.getInstance().initUserSwitch(newUserId);
                AwareFakeActivityRecg.self().initUserSwitch(newUserId);
                if (newUserId == 0) {
                    AwareIntelligentRecg.getInstance().updateWidget(AwareAppAssociate.this.getWidgetsPkg(newUserId), null);
                }
                AwareAppUseDataManager.getInstance().initUserSwitch();
                AwareLog.i(AwareAppAssociate.TAG, "onUserSwitchComplete cost: " + (System.currentTimeMillis() - startTime));
            }

            public void onForegroundProfileSwitch(int newProfileId) {
            }

            public void onLockedBootComplete(int newUserId) {
            }
        };
        this.mHwAMS = HwActivityManagerService.self();
        this.mHandler = new AppAssocHandler(BackgroundThread.get().getLooper());
        this.mMorePreviousLevel = decideMorePreviousLevel();
    }

    public static synchronized AwareAppAssociate getInstance() {
        AwareAppAssociate awareAppAssociate;
        synchronized (AwareAppAssociate.class) {
            if (mAwareAppAssociate == null) {
                mAwareAppAssociate = new AwareAppAssociate();
            }
            awareAppAssociate = mAwareAppAssociate;
        }
        return awareAppAssociate;
    }

    public void getVisibleWindowsInRestriction(SparseSet windowPids) {
        if (mEnabled && windowPids != null) {
            synchronized (this.mVisibleWindows) {
                for (Map.Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                    AwareProcessWindowInfo winInfo = window.getValue();
                    if (winInfo.inRestriction && !winInfo.isEvil()) {
                        windowPids.add(window.getKey().intValue());
                    }
                }
            }
            if (DEBUG) {
                AwareLog.d(TAG, "WindowPids in restriction:" + windowPids);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x007a  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0087 A[SYNTHETIC] */
    public void getVisibleWindows(SparseSet windowPids, SparseSet evilPids) {
        boolean allowedWindow;
        if (mEnabled && windowPids != null) {
            synchronized (this.mVisibleWindows) {
                for (Map.Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                    AwareProcessWindowInfo winInfo = window.getValue();
                    if (winInfo.mode != 0) {
                        if (winInfo.mode != 3) {
                            allowedWindow = false;
                            AwareLog.i(TAG, "[getVisibleWindows]:" + window.getKey() + " [allowedWindow]:" + allowedWindow + " isEvil:" + winInfo.isEvil());
                            if (!allowedWindow && !winInfo.isEvil()) {
                                windowPids.add(window.getKey().intValue());
                            } else if (evilPids == null) {
                                evilPids.add(window.getKey().intValue());
                            }
                        }
                    }
                    allowedWindow = true;
                    AwareLog.i(TAG, "[getVisibleWindows]:" + window.getKey() + " [allowedWindow]:" + allowedWindow + " isEvil:" + winInfo.isEvil());
                    if (!allowedWindow) {
                    }
                    if (evilPids == null) {
                    }
                }
            }
            synchronized (this.mVisWinDurScreenOff) {
                if (!this.mVisWinDurScreenOff.isEmpty()) {
                    windowPids.addAll(this.mVisWinDurScreenOff);
                }
            }
            if (DEBUG) {
                AwareLog.d(TAG, "WindowPids:" + windowPids + ", evilPids:" + evilPids);
            }
            if (RECORD) {
                recordWindowDetail(windowPids);
            }
        }
    }

    public boolean isVisibleWindows(int userid, String pkg) {
        if (!mEnabled || pkg == null) {
            return true;
        }
        synchronized (this.mVisibleWindows) {
            for (Map.Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                AwareProcessWindowInfo winInfo = window.getValue();
                boolean allowedWindow = isAllowedAlertWindowOps(winInfo);
                AwareLog.i(TAG, "[isVisibleWindows]:" + window.getKey() + " pkg:" + pkg + " [allowedWindow]:" + allowedWindow + " isEvil:" + winInfo.isEvil());
                if (pkg.equals(winInfo.pkg) && ((userid == -1 || userid == UserHandle.getUserId(winInfo.uid)) && allowedWindow && !winInfo.isEvil())) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean hasWindow(int uid) {
        synchronized (this.mVisibleWindows) {
            for (Map.Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                if (uid == window.getValue().uid) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isAllowedAlertWindowOps(AwareProcessWindowInfo winInfo) {
        return winInfo.mode == 0 || winInfo.mode == 3;
    }

    public boolean isEvilAlertWindow(int window, int code) {
        boolean result;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mVisibleWindows) {
            AwareProcessWindowInfo winInfo = this.mVisibleWindows.get(Integer.valueOf(window));
            if (winInfo == null || (isAllowedAlertWindowOps(winInfo) && !winInfo.isEvil(code))) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }

    /* access modifiers changed from: private */
    public void updateWidgets(int userId) {
        if (DEBUG) {
            AwareLog.i(TAG, "updateWidgets, userId: " + userId);
        }
        IBinder service = ServiceManager.getService("appwidget");
        if (service != null) {
            SparseArray<Widget> widgets = new SparseArray<>();
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInt(2);
            data.writeInt(userId);
            try {
                service.transact(1599297111, data, reply, 0);
                int size = reply.readInt();
                if (DEBUG) {
                    AwareLog.i(TAG, "updateWidgets, transact finish, widgets size: " + size);
                }
                for (int i = 0; i < size; i++) {
                    int id = reply.readInt();
                    String pkg = reply.readString();
                    boolean visibleB = true;
                    if (reply.readInt() != 1) {
                        visibleB = false;
                    }
                    if (pkg != null && pkg.length() > 0) {
                        widgets.put(id, new Widget(id, pkg, visibleB));
                    }
                    if (DEBUG) {
                        AwareLog.i(TAG, "updateWidgets, widget: " + id + ", " + pkg + ", " + visibleB);
                    }
                }
            } catch (RemoteException e) {
                AwareLog.e(TAG, "getWidgetsPkg, transact error!");
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
                throw th;
            }
            reply.recycle();
            data.recycle();
            synchronized (this.mWidgets) {
                this.mWidgets.put(userId, widgets);
            }
        }
    }

    public Set<String> getWidgetsPkg() {
        return getWidgetsPkg(this.mCurUserId);
    }

    public Set<String> getWidgetsPkg(int userId) {
        if (!mEnabled) {
            return null;
        }
        Set<String> widgets = new ArraySet<>();
        synchronized (this.mWidgets) {
            SparseArray<Widget> widgetMap = this.mWidgets.get(userId);
            if (widgetMap != null) {
                for (int i = widgetMap.size() - 1; i >= 0; i--) {
                    Widget widget = widgetMap.valueAt(i);
                    if (widget.isVisible) {
                        widgets.add(widget.pkgName);
                    }
                    if (DEBUG) {
                        AwareLog.i(TAG, "getWidgetsPkg:" + widget.appWidgetId + ", " + widget.pkgName + ", " + widget.isVisible);
                    }
                }
            }
        }
        return widgets;
    }

    public void getForeGroundApp(SparseSet forePids) {
        if (mEnabled && forePids != null) {
            synchronized (this.mForePids) {
                for (int i = this.mForePids.size() - 1; i >= 0; i--) {
                    forePids.add(this.mForePids.keyAt(i));
                }
            }
        }
    }

    public boolean isForeGroundApp(int uid) {
        boolean z = false;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mForePids) {
            if (this.mForePids.indexOfValue(uid) >= 0) {
                z = true;
            }
        }
        return z;
    }

    public boolean isRecentFgApp(int uid) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mBgRecentForcePids) {
            for (Map.Entry<Integer, ProcessData> map : this.mBgRecentForcePids.entrySet()) {
                ProcessData data = map.getValue();
                if (data != null && data.mUid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    public void getAssocListForPid(int pid, SparseSet strong) {
        if (mEnabled && pid > 0 && strong != null) {
            getStrongAssoc(pid, strong);
            if (DEBUG) {
                AwareLog.i(TAG, "[" + pid + "]strongList:" + strong);
            }
            if (RECORD) {
                recordAssocDetail(pid);
            }
        }
    }

    public void getAssocClientListForPid(int pid, SparseSet strong) {
        if (mEnabled && pid > 0 && strong != null) {
            getStrongAssocClient(pid, strong);
            if (DEBUG) {
                AwareLog.i(TAG, "[" + pid + "]strongList:" + strong);
            }
            if (RECORD) {
                recordAssocDetail(pid);
            }
        }
    }

    private void getStrongAssocClient(int pid, SparseSet strong) {
        if (pid > 0 && strong != null) {
            synchronized (this.mLock) {
                for (int k = this.mAssocRecordMap.size() - 1; k >= 0; k--) {
                    int clientPid = this.mAssocRecordMap.keyAt(k);
                    AssocPidRecord record = this.mAssocRecordMap.valueAt(k);
                    int NP = record.mAssocBindService.getMap().size();
                    for (int i = 0; i < NP; i++) {
                        SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                        int NB = brs.size();
                        for (int j = 0; j < NB; j++) {
                            AssocBaseRecord br = brs.valueAt(j);
                            if (br != null && br.pid == pid) {
                                strong.add(clientPid);
                            }
                        }
                    }
                }
            }
        }
    }

    public void getAssocClientListForUid(int uid, Set<String> strong) {
        if (mEnabled && uid > 0 && strong != null) {
            synchronized (this.mLock) {
                for (int k = this.mAssocRecordMap.size() - 1; k >= 0; k--) {
                    AssocPidRecord record = this.mAssocRecordMap.valueAt(k);
                    if (UserHandle.getAppId(record.uid) >= 10000) {
                        boolean bfound = false;
                        int NP = record.mAssocBindService.getMap().size();
                        int i = 0;
                        while (true) {
                            if (i >= NP) {
                                break;
                            }
                            SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                            int NB = brs.size();
                            int j = 0;
                            while (true) {
                                if (j < NB) {
                                    AssocBaseRecord br = brs.valueAt(j);
                                    if (br != null && br.uid == uid) {
                                        strong.addAll(getPackageNameForUid(record.uid, record.pid));
                                        bfound = true;
                                        break;
                                    }
                                    j++;
                                } else {
                                    break;
                                }
                            }
                            if (bfound) {
                                break;
                            }
                            i++;
                        }
                    }
                }
            }
            if (DEBUG) {
                AwareLog.i(TAG, "[" + uid + "]strongList:" + strong);
            }
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
                switch (eventId) {
                    case 1:
                    case 2:
                        addProcessRelation(bundleArgs.getInt("callPid"), bundleArgs.getInt("callUid"), bundleArgs.getString("callProcName"), bundleArgs.getInt("tgtUid"), bundleArgs.getString("tgtProcName"), bundleArgs.getString("compName"), eventId, bundleArgs.getInt("hwFlag"));
                        return;
                    case 3:
                        removeProcessRelation(bundleArgs.getInt("callPid"), bundleArgs.getInt("callUid"), bundleArgs.getString("callProcName"), bundleArgs.getInt("tgtUid"), bundleArgs.getString("tgtProcName"), bundleArgs.getString("compName"), eventId, bundleArgs.getInt("hwFlag"));
                        return;
                    case 4:
                        try {
                            int callerPid = bundleArgs.getInt("callPid");
                            try {
                                int callerUid = bundleArgs.getInt("callUid");
                                try {
                                    String callerProcessName = bundleArgs.getString("callProcName");
                                    updateProcessRelation(callerPid, callerUid, callerProcessName, bundleArgs.getStringArrayList(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                                    AwareBinderSchedManager.getInstance().reportProcessStarted(callerPid, callerUid, callerProcessName);
                                    return;
                                } catch (ArrayIndexOutOfBoundsException e) {
                                }
                            } catch (ArrayIndexOutOfBoundsException e2) {
                                AwareLog.e(TAG, "getStringArrayList out of bounds exception!");
                                return;
                            }
                        } catch (ArrayIndexOutOfBoundsException e3) {
                            AwareLog.e(TAG, "getStringArrayList out of bounds exception!");
                            return;
                        }
                    case 5:
                        addWidget(bundleArgs.getInt("userid"), bundleArgs.getInt("widgetId", -1), bundleArgs.getString("widget"), bundleArgs.getBundle("widgetOpt"));
                        break;
                    case 6:
                        removeWidget(bundleArgs.getInt("userid"), bundleArgs.getInt("widgetId", -1), bundleArgs.getString("widget"));
                        break;
                    case 7:
                        clearWidget();
                        break;
                    case 8:
                        addWindow(bundleArgs.getInt("window"), bundleArgs.getInt("windowmode"), bundleArgs.getInt("hashcode"), bundleArgs.getInt("width"), bundleArgs.getInt("height"), bundleArgs.getFloat("alpha"), bundleArgs.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME), bundleArgs.getInt("uid"));
                        break;
                    case 9:
                        removeWindow(bundleArgs.getInt("window"), bundleArgs.getInt("hashcode"), bundleArgs.getInt("uid"));
                        break;
                    case 10:
                        updateWindowOps(bundleArgs.getString(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                        break;
                    case 11:
                        try {
                            reportHome(bundleArgs.getInt(SceneRecogFeature.DATA_PID), bundleArgs.getInt("tgtUid"), bundleArgs.getStringArrayList(MemoryConstant.MEM_PREREAD_ITEM_NAME));
                            break;
                        } catch (ArrayIndexOutOfBoundsException e4) {
                            AwareLog.e(TAG, "getStringArrayList out of bounds exception!");
                            return;
                        }
                    case 12:
                        reportPrevInfo(bundleArgs.getInt(SceneRecogFeature.DATA_PID), bundleArgs.getInt("tgtUid"));
                        break;
                    default:
                        switch (eventId) {
                            case 24:
                                updateWidgetOptions(bundleArgs.getInt("userid"), bundleArgs.getInt("widgetId", -1), bundleArgs.getString("widget"), bundleArgs.getBundle("widgetOpt"));
                                break;
                            case 25:
                                AwareIntelligentRecg.getInstance().addScreenRecord(bundleArgs.getInt("callUid"), bundleArgs.getInt("callPid"));
                                return;
                            case 26:
                                AwareIntelligentRecg.getInstance().removeScreenRecord(bundleArgs.getInt("callUid"), bundleArgs.getInt("callPid"));
                                return;
                            case 27:
                                updateWindow(bundleArgs);
                                break;
                            default:
                                switch (eventId) {
                                    case 30:
                                        AwareIntelligentRecg.getInstance().addCamera(bundleArgs.getInt("callUid"));
                                        return;
                                    case 31:
                                        AwareIntelligentRecg.getInstance().removeCamera(bundleArgs.getInt("callUid"));
                                        return;
                                    case 32:
                                        updateWidgetFlush(bundleArgs.getInt("userid"), bundleArgs.getString("widget"));
                                        break;
                                    case 33:
                                        AwareIntelligentRecg.getInstance().reportGoogleConn(bundleArgs.getBoolean("gms_conn"));
                                        break;
                                    default:
                                        if (DEBUG) {
                                            AwareLog.e(TAG, "Unknown EventID: " + eventId);
                                            break;
                                        }
                                        break;
                                }
                        }
                }
            }
        } else if (DEBUG) {
            AwareLog.d(TAG, "AwareAppAssociate feature disabled!");
        }
    }

    private void getStrongAssoc(int pid, SparseSet strong) {
        if (pid > 0 && strong != null) {
            synchronized (this.mLock) {
                long curElapse = SystemClock.elapsedRealtime();
                AssocPidRecord record = this.mAssocRecordMap.get(pid);
                if (record != null) {
                    int NP = record.mAssocBindService.getMap().size();
                    for (int i = 0; i < NP; i++) {
                        SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                        int NB = brs.size();
                        for (int j = 0; j < NB; j++) {
                            int targetPid = brs.valueAt(j).pid;
                            if (targetPid != 0) {
                                strong.add(targetPid);
                            }
                        }
                    }
                    int NP2 = record.mAssocProvider.getMap().size();
                    for (int i2 = 0; i2 < NP2; i2++) {
                        SparseArray<AssocBaseRecord> brs2 = (SparseArray) record.mAssocProvider.getMap().valueAt(i2);
                        int NB2 = brs2.size();
                        for (int j2 = 0; j2 < NB2; j2++) {
                            AssocBaseRecord br = brs2.valueAt(j2);
                            int targetPid2 = br.pid;
                            if (targetPid2 != 0 && br.isStrong && curElapse - br.miniTime < 120000) {
                                strong.add(targetPid2);
                            }
                        }
                    }
                }
            }
        }
    }

    public void getAssocProvider(int pid, SparseSet assocProvider) {
        if (pid > 0 && assocProvider != null) {
            synchronized (this.mLock) {
                long curElapse = SystemClock.elapsedRealtime();
                AssocPidRecord record = this.mAssocRecordMap.get(pid);
                if (record != null) {
                    int NP = record.mAssocProvider.getMap().size();
                    for (int i = 0; i < NP; i++) {
                        SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocProvider.getMap().valueAt(i);
                        int NB = brs.size();
                        for (int j = 0; j < NB; j++) {
                            AssocBaseRecord br = brs.valueAt(j);
                            int targetPid = br.pid;
                            if (targetPid != 0 && br.isStrong && curElapse - br.miniTime < 120000) {
                                assocProvider.add(targetPid);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addWidget(int userId, int widgetId, String pkgName, Bundle options) {
        if (pkgName != null) {
            if (DEBUG) {
                AwareLog.i(TAG, "addWidget, userId:" + userId + ", widgetId: " + widgetId + ", pkg:" + pkgName + ", vis: " + isWidgetVisible(options));
            }
            synchronized (this.mWidgets) {
                checkAndInitWidgetObj(userId);
                if (this.mWidgets.get(userId).indexOfKey(widgetId) < 0) {
                    this.mWidgets.get(userId).put(widgetId, new Widget(widgetId, pkgName, isWidgetVisible(options)));
                }
            }
            if (userId == 0) {
                AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(userId), pkgName);
            }
        }
    }

    private void removeWidget(int userId, int widgetId, String pkgName) {
        if (pkgName != null) {
            if (DEBUG) {
                AwareLog.i(TAG, "removeWidget, userId:" + userId + ", widgetId: " + widgetId + ", pkg:" + pkgName);
            }
            synchronized (this.mWidgets) {
                checkAndInitWidgetObj(userId);
                this.mWidgets.get(userId).delete(widgetId);
            }
            if (userId == 0) {
                AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(userId), pkgName);
            }
        }
    }

    private void updateWidgetOptions(int userId, int widgetId, String pkgName, Bundle options) {
        if (widgetId >= 0 && pkgName != null) {
            if (DEBUG) {
                AwareLog.i(TAG, "updateWidgetOptions, userId:" + userId + ", widgetId: " + widgetId + ", pkg:" + pkgName + ", options: " + options);
            }
            boolean visible = isWidgetVisible(options);
            synchronized (this.mWidgets) {
                checkAndInitWidgetObj(userId);
                SparseArray<Widget> widgetMap = this.mWidgets.get(userId);
                if (widgetMap.get(widgetId) != null) {
                    widgetMap.get(widgetId).isVisible = visible;
                } else {
                    widgetMap.put(widgetId, new Widget(widgetId, pkgName, visible));
                }
                if (userId == 0) {
                    AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(userId), pkgName);
                }
            }
        }
    }

    private void updateWidgetFlush(int userId, String pkgName) {
        AwareIntelligentRecg.getInstance().widgetTrigUpdate(pkgName);
    }

    public boolean isWidgetVisible(Bundle options) {
        if (options == null) {
            return false;
        }
        int maxHeight = options.getInt("appWidgetMaxHeight");
        int maxWidth = options.getInt("appWidgetMaxWidth");
        int minHeight = options.getInt("appWidgetMinHeight");
        int minWidth = options.getInt("appWidgetMinWidth");
        if (maxHeight == 0 && maxWidth == 0 && minHeight == 0 && minWidth == 0) {
            return false;
        }
        return true;
    }

    private void clearWidget() {
        if (DEBUG) {
            AwareLog.d(TAG, "clearWidget");
        }
        synchronized (this.mWidgets) {
            for (int i = this.mWidgets.size() - 1; i >= 0; i--) {
                SparseArray<Widget> userWdigets = this.mWidgets.valueAt(i);
                if (userWdigets != null) {
                    userWdigets.clear();
                }
            }
        }
        AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(0), null);
    }

    private static final class Widget {
        int appWidgetId;
        boolean isVisible = false;
        String pkgName = "";

        public Widget(int appWidgetId2, String pkgName2, boolean isVisible2) {
            this.appWidgetId = appWidgetId2;
            this.pkgName = pkgName2;
            this.isVisible = isVisible2;
        }
    }

    private void initVisibleWindows() {
        ArrayMap<Integer, AwareProcessWindowInfo> arrayMap;
        AwareProcessWindowInfo winInfo;
        int window;
        int mode;
        List<Bundle> windowsList = HwWindowManager.getVisibleWindows(24);
        if (windowsList == null) {
            AwareLog.w(TAG, "Catch null when initVisibleWindows.");
            return;
        }
        ArrayMap<Integer, AwareProcessWindowInfo> arrayMap2 = this.mVisibleWindows;
        synchronized (arrayMap2) {
            try {
                this.mVisibleWindows.clear();
                updateVisibleWindowsCache(2, -1, -1, null, -1, -1, false);
                for (Bundle windowState : windowsList) {
                    if (windowState != null) {
                        int window2 = windowState.getInt("window_pid");
                        int mode2 = windowState.getInt("window_value");
                        int code = windowState.getInt("window_state");
                        int width = windowState.getInt("window_width");
                        int height = windowState.getInt("window_height");
                        boolean hasSurface = windowState.getBoolean("hasSurface");
                        float alpha = windowState.getFloat("window_alpha");
                        windowState.getBoolean("window_hidden");
                        String pkg = windowState.getString("window_package");
                        int uid = windowState.getInt("window_uid");
                        if (DEBUG) {
                            AwareLog.i(TAG, "initVisibleWindows pid:" + window2 + " mode:" + mode2 + " code:" + code + " width:" + width + " height:" + height);
                        }
                        AwareProcessWindowInfo winInfo2 = this.mVisibleWindows.get(Integer.valueOf(window2));
                        if (winInfo2 == null) {
                            winInfo = new AwareProcessWindowInfo(mode2, pkg, uid, width, height);
                        } else {
                            winInfo = winInfo2;
                        }
                        boolean isEvil = checkAndUpdateWindowInfo(winInfo, width, height, alpha, hasSurface);
                        this.mVisibleWindows.put(Integer.valueOf(window2), winInfo);
                        arrayMap = arrayMap2;
                        updateVisibleWindowsCache(0, window2, mode2, pkg, uid, -1, false);
                        if (!isEvil) {
                            mode = mode2;
                            window = window2;
                            notifyVisibleWindowsChange(2, window, mode);
                        } else {
                            mode = mode2;
                            window = window2;
                        }
                        winInfo.addWindow(Integer.valueOf(code), isEvil);
                        updateVisibleWindowsCache(4, window, -1, null, -1, Integer.valueOf(code).intValue(), isEvil);
                        arrayMap2 = arrayMap;
                    }
                }
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        }
    }

    private void deinitVisibleWindows() {
        synchronized (this.mVisibleWindows) {
            this.mVisibleWindows.clear();
            notifyVisibleWindowsChange(0, -1, -1);
            updateVisibleWindowsCache(2, -1, -1, null, -1, -1, false);
        }
    }

    private void addWindow(int window, int mode, int code, int width, int height, float alpha, String pkg, int uid) {
        ArrayMap<Integer, AwareProcessWindowInfo> arrayMap;
        boolean isEvil;
        AwareLog.i(TAG, "[addWindow]:" + window + " [mode]:" + mode + " [code]:" + code + " width:" + width + " height:" + height + " alpha:" + alpha);
        if (window > 0) {
            boolean isEvil2 = false;
            AwareBinderSchedManager.getInstance().setProcessQos(uid, window, true, 0);
            ArrayMap<Integer, AwareProcessWindowInfo> arrayMap2 = this.mVisibleWindows;
            synchronized (arrayMap2) {
                try {
                    AwareProcessWindowInfo winInfo = this.mVisibleWindows.get(Integer.valueOf(window));
                    if ((width <= AwareProcessWindowInfo.getMinWindowWidth() && width > 0) || ((height <= AwareProcessWindowInfo.getMinWindowHeight() && height > 0) || alpha == 0.0f)) {
                        isEvil2 = true;
                    }
                    if (winInfo == null) {
                        winInfo = new AwareProcessWindowInfo(mode, pkg, uid, width, height);
                        this.mVisibleWindows.put(Integer.valueOf(window), winInfo);
                        arrayMap = arrayMap2;
                        try {
                            updateVisibleWindowsCache(0, window, mode, pkg, uid, -1, false);
                            isEvil = isEvil2;
                            if (!isEvil) {
                                notifyVisibleWindowsChange(2, window, mode);
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        isEvil = isEvil2;
                        arrayMap = arrayMap2;
                    }
                    winInfo.addWindow(Integer.valueOf(code), isEvil);
                    updateVisibleWindowsCache(4, window, -1, null, -1, Integer.valueOf(code).intValue(), isEvil);
                    AwareLog.i(TAG, "[addWindow]:" + window + " [mode]:" + mode + " [code]:" + code + " isEvil:" + isEvil);
                    if (DEBUG) {
                        AwareLog.i(TAG, "[addVisibleWindows]:" + window + " [mode]:" + mode + " [code]:" + code);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    arrayMap = arrayMap2;
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0091, code lost:
        if (r11 == false) goto L_0x00ae;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0095, code lost:
        if (r15.mScreenOff == false) goto L_0x00ae;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0097, code lost:
        if (r1 != false) goto L_0x00ae;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0099, code lost:
        r1 = r15.mVisWinDurScreenOff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x009b, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:?, code lost:
        r15.mVisWinDurScreenOff.add(java.lang.Integer.valueOf(r16).intValue());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x00a9, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00b0, code lost:
        if (com.android.server.rms.iaware.appmng.AwareAppAssociate.DEBUG == false) goto L_0x00d3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00b2, code lost:
        android.rms.iaware.AwareLog.d(com.android.server.rms.iaware.appmng.AwareAppAssociate.TAG, "[removeVisibleWindows]:" + r16 + " [code]:" + r17);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00db, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:?, code lost:
        return;
     */
    private void removeWindow(int window, int code, int uid) {
        if (window > 0) {
            boolean removed = false;
            AwareBinderSchedManager.getInstance().setProcessQos(uid, window, false, 0);
            synchronized (this.mVisibleWindows) {
                AwareProcessWindowInfo winInfo = this.mVisibleWindows.get(Integer.valueOf(window));
                if (winInfo == null) {
                    this.mVisibleWindows.remove(Integer.valueOf(window));
                    updateVisibleWindowsCache(1, Integer.valueOf(window).intValue(), -1, null, -1, -1, false);
                    return;
                }
                boolean isEvil = winInfo.isEvil();
                winInfo.removeWindow(Integer.valueOf(code));
                updateVisibleWindowsCache(5, Integer.valueOf(window).intValue(), -1, null, -1, Integer.valueOf(code).intValue(), false);
                if (winInfo.windows.size() == 0) {
                    this.mVisibleWindows.remove(Integer.valueOf(window));
                    updateVisibleWindowsCache(1, Integer.valueOf(window).intValue(), -1, null, -1, -1, false);
                    if (!isEvil) {
                        notifyVisibleWindowsChange(1, window, -1);
                    }
                    removed = true;
                }
            }
        } else {
            return;
        }
        while (true) {
        }
    }

    private void updateWindowOpsList() {
        synchronized (this.mVisibleWindows) {
            for (Map.Entry<Integer, AwareProcessWindowInfo> window : this.mVisibleWindows.entrySet()) {
                AwareProcessWindowInfo winInfo = window.getValue();
                int mode = ((AppOpsManager) this.mMtmService.context().getSystemService("appops")).checkOpNoThrow(24, winInfo.uid, winInfo.pkg);
                winInfo.inRestriction = isInRestriction(winInfo.mode, mode);
                winInfo.mode = mode;
                updateVisibleWindowsCache(3, window.getKey().intValue(), mode, null, -1, -1, false);
            }
        }
    }

    private boolean isInRestriction(int oldmode, int newmode) {
        return (oldmode == 0 || oldmode == 3) && newmode == 1;
    }

    private void updateWindowOps(String pkgName) {
        if (this.mMtmService != null) {
            if (DEBUG) {
                AwareLog.d(TAG, "updateWindowOps pkg:" + pkgName);
            }
            if (pkgName == null) {
                updateWindowOpsList();
                return;
            }
            synchronized (this.mLock) {
                synchronized (this.mVisibleWindows) {
                    Iterator<Map.Entry<Integer, AwareProcessWindowInfo>> it = this.mVisibleWindows.entrySet().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        Map.Entry<Integer, AwareProcessWindowInfo> window = it.next();
                        int pid = window.getKey().intValue();
                        AwareProcessWindowInfo winInfo = window.getValue();
                        AssocBaseRecord record = this.mProcPidMap.get(pid);
                        if (record != null && record.pkgList != null) {
                            if (winInfo != null) {
                                if (record.pkgList.contains(pkgName)) {
                                    int mode = ((AppOpsManager) this.mMtmService.context().getSystemService("appops")).checkOpNoThrow(24, record.uid, pkgName);
                                    winInfo.mode = mode;
                                    updateVisibleWindowsCache(3, pid, mode, null, -1, -1, false);
                                    if (!winInfo.isEvil()) {
                                        notifyVisibleWindowsChange(2, pid, mode);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateWindow(Bundle bundleArgs) {
        ArrayMap<Integer, AwareProcessWindowInfo> arrayMap;
        boolean isEvil;
        if (bundleArgs != null) {
            int window = bundleArgs.getInt("window");
            int mode = bundleArgs.getInt("windowmode");
            int code = bundleArgs.getInt("hashcode");
            int width = bundleArgs.getInt("width");
            int height = bundleArgs.getInt("height");
            float alpha = bundleArgs.getFloat("alpha");
            boolean hasSurface = bundleArgs.getBoolean("hasSurface");
            AwareLog.i(TAG, "[updateWindow]:" + window + " [mode]:" + mode + " [code]:" + code + " width:" + width + " height:" + height + " alpha:" + alpha + " hasSurface:" + hasSurface);
            if (window > 0) {
                ArrayMap<Integer, AwareProcessWindowInfo> arrayMap2 = this.mVisibleWindows;
                synchronized (arrayMap2) {
                    try {
                        AwareProcessWindowInfo winInfo = this.mVisibleWindows.get(Integer.valueOf(window));
                        if (winInfo == null) {
                            try {
                                AwareLog.i(TAG, "[updateWindow]: do not update before add ?" + window + " [mode]:" + mode + " [code]:" + code);
                            } catch (Throwable th) {
                                th = th;
                                arrayMap = arrayMap2;
                                throw th;
                            }
                        } else {
                            arrayMap = arrayMap2;
                            try {
                                boolean isEvil2 = checkAndUpdateWindowInfo(winInfo, width, height, alpha, hasSurface);
                                if (winInfo.containsWindow(code)) {
                                    winInfo.addWindow(Integer.valueOf(code), isEvil2);
                                    isEvil = isEvil2;
                                    try {
                                        updateVisibleWindowsCache(4, window, -1, null, -1, Integer.valueOf(code).intValue(), isEvil);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                } else {
                                    isEvil = isEvil2;
                                }
                                AwareLog.i(TAG, "[updateWindow]:" + window + " [mode]:" + mode + " [code]:" + code + " isEvil:" + isEvil);
                                if (DEBUG) {
                                    AwareLog.i(TAG, "[updateWindow]:" + window + " [mode]:" + mode + " [code]:" + code);
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        arrayMap = arrayMap2;
                        throw th;
                    }
                }
            }
        }
    }

    private void reportHome(int pid, int uid, ArrayList<String> pkgname) {
        this.mHomeProcessPid = pid;
        this.mHomeProcessUid = uid;
        synchronized (this.mHomePackageList) {
            this.mHomePackageList.clear();
            if (pkgname != null && pkgname.size() > 0) {
                this.mHomePackageList.addAll(pkgname);
            }
        }
    }

    private void reportPrevInfo(int pid, int uid) {
        this.mAmsPrevBase.setValue(pid, uid, SystemClock.elapsedRealtime());
    }

    public List<String> getDefaultHomePackages() {
        ArrayList<String> pkgs = new ArrayList<>();
        synchronized (this.mHomePackageList) {
            pkgs.addAll(this.mHomePackageList);
        }
        return pkgs;
    }

    public int getCurHomeProcessPid() {
        return this.mHomeProcessPid;
    }

    public int getCurHomeProcessUid() {
        return this.mHomeProcessUid;
    }

    public AwareAppLruBase getRecentTaskPrevInfo() {
        return new AwareAppLruBase(this.mRecentTaskPrevBase.mPid, this.mRecentTaskPrevBase.mUid, this.mRecentTaskPrevBase.mTime);
    }

    public AwareAppLruBase getPreviousAppInfo() {
        return new AwareAppLruBase(this.mPrevNonHomeBase.mPid, this.mPrevNonHomeBase.mUid, this.mPrevNonHomeBase.mTime);
    }

    public AwareAppLruBase getPreviousByAmsInfo() {
        return new AwareAppLruBase(this.mAmsPrevBase.mPid, this.mAmsPrevBase.mUid, this.mAmsPrevBase.mTime);
    }

    private boolean checkType(int type) {
        if (type == 1 || type == 2 || type == 3) {
            return true;
        }
        return false;
    }

    private String typeToString(int type) {
        if (type == 1) {
            return "ADD_ASSOC_BINDSERVICE";
        }
        if (type == 2) {
            return "ADD_ASSOC_PROVIDER";
        }
        if (type == 3) {
            return "DEL_ASSOC_BINDSERVICE";
        }
        if (type == 4) {
            return "APP_ASSOC_PROCESSUPDATE";
        }
        return "[Error type]" + type;
    }

    private void addProcessRelation(int callerPid, int callerUid, String callerName, int targetUid, String targetName, String comp, int type, int hwFlag) {
        if (checkType(type)) {
            if (callerUid == targetUid) {
                if (DEBUG) {
                    AwareLog.i(TAG, typeToString(type) + " in the same UID.Pass.");
                }
            } else if (callerPid <= 0 || callerUid <= 0 || targetUid <= 0) {
                if (DEBUG) {
                    AwareLog.i(TAG, typeToString(type) + " with wrong pid or uid");
                }
            } else if (callerName != null && targetName != null) {
                String comp2 = comp == null ? HwBluetoothManagerServiceEx.DEFAULT_PACKAGE_NAME : comp;
                if (DEBUG) {
                    AwareLog.i(TAG, typeToString(type) + ". Caller[Pid:" + callerPid + "][Uid:" + callerUid + "][Name:" + callerName + "] Target[Uid:" + targetUid + "][pName:" + targetName + "][hash:" + comp2 + "]");
                }
                int targetPid = 0;
                if (targetUid != 1000 || !targetName.equals(SYSTEM)) {
                    synchronized (this.mLock) {
                        AssocBaseRecord br = (AssocBaseRecord) this.mProcInfoMap.get(targetName, targetUid);
                        if (br != null) {
                            targetPid = br.pid;
                        }
                        AssocPidRecord pidRecord = this.mAssocRecordMap.get(callerPid);
                        if (pidRecord == null) {
                            AssocPidRecord pidRecord2 = new AssocPidRecord(callerPid, callerUid, callerName);
                            AssocBaseRecord baseRecord = new AssocBaseRecord(targetName, targetUid, targetPid);
                            baseRecord.mComponents.add(comp2);
                            addAppCompJobLock(baseRecord, comp2, hwFlag);
                            ProcessMap<AssocBaseRecord> relations = pidRecord2.getMap(type);
                            if (relations == null) {
                                if (DEBUG) {
                                    AwareLog.e(TAG, "Error type:" + type);
                                }
                                return;
                            }
                            relations.put(targetName, targetUid, baseRecord);
                            this.mAssocRecordMap.put(callerPid, pidRecord2);
                            return;
                        }
                        ProcessMap<AssocBaseRecord> relations2 = pidRecord.getMap(type);
                        if (relations2 == null) {
                            if (DEBUG) {
                                AwareLog.e(TAG, "Error type:" + type);
                            }
                            return;
                        }
                        AssocBaseRecord baseRecord2 = (AssocBaseRecord) relations2.get(targetName, targetUid);
                        if (baseRecord2 == null) {
                            AssocBaseRecord baseRecord3 = new AssocBaseRecord(targetName, targetUid, targetPid);
                            baseRecord3.mComponents.add(comp2);
                            addAppCompJobLock(baseRecord3, comp2, hwFlag);
                            relations2.put(targetName, targetUid, baseRecord3);
                            return;
                        }
                        baseRecord2.miniTime = SystemClock.elapsedRealtime();
                        baseRecord2.isStrong = true;
                        baseRecord2.mComponents.add(comp2);
                        addAppCompJobLock(baseRecord2, comp2, hwFlag);
                    }
                }
            } else if (DEBUG) {
                AwareLog.i(TAG, typeToString(type) + " with wrong callerName or targetName");
            }
        }
    }

    private void removeProcessRelation(int callerPid, int callerUid, String callerName, int targetUid, String targetName, String comp, int type, int hwFlag) {
        if (checkType(type)) {
            if (callerUid == targetUid) {
                if (DEBUG) {
                    AwareLog.i(TAG, typeToString(type) + " in the same UID.Pass.");
                }
            } else if (callerPid <= 0 || callerUid <= 0 || targetUid <= 0) {
                if (DEBUG) {
                    AwareLog.i(TAG, typeToString(type) + " with wrong pid or uid");
                }
            } else if (targetName != null) {
                if (DEBUG) {
                    AwareLog.i(TAG, typeToString(type) + ". Caller[Pid:" + callerPid + "] target[" + targetUid + AwarenessInnerConstants.COLON_KEY + targetName + AwarenessInnerConstants.COLON_KEY + comp + "]");
                }
                String comp2 = comp == null ? HwBluetoothManagerServiceEx.DEFAULT_PACKAGE_NAME : comp;
                synchronized (this.mLock) {
                    AssocPidRecord pr = this.mAssocRecordMap.get(callerPid);
                    if (pr != null) {
                        ProcessMap<AssocBaseRecord> relations = pr.getMap(type);
                        if (relations == null) {
                            if (DEBUG) {
                                AwareLog.e(TAG, "Error type:" + type);
                            }
                            return;
                        }
                        AssocBaseRecord br = (AssocBaseRecord) relations.get(targetName, targetUid);
                        if (br != null && br.mComponents.contains(comp2)) {
                            br.mComponents.remove(comp2);
                            removeAppCompJobLock(br, comp2, hwFlag);
                            if (br.mComponents.isEmpty()) {
                                relations.remove(targetName, targetUid);
                                if (pr.isEmpty()) {
                                    this.mAssocRecordMap.remove(pr.pid);
                                }
                            }
                        }
                    }
                }
            } else if (DEBUG) {
                AwareLog.i(TAG, typeToString(type) + " with wrong targetName");
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeDiedProcessRelation(int pid, int uid) {
        if (pid > 0 && uid > 0) {
            if (DEBUG) {
                AwareLog.i(TAG, "remove died. Pid:" + pid + " Uid:" + uid);
            }
            synchronized (this.mLock) {
                AssocBaseRecord br = (AssocBaseRecord) this.mProcPidMap.removeReturnOld(pid);
                if (br != null) {
                    this.mProcInfoMap.remove(br.processName, br.uid);
                    if (br.pkgList != null) {
                        Iterator<String> it = br.pkgList.iterator();
                        while (it.hasNext()) {
                            String pkg = it.next();
                            synchronized (this.mProcPkgMap) {
                                SparseSet pids = this.mProcPkgMap.get(pkg);
                                if (pids != null && pids.contains(pid)) {
                                    pids.remove(Integer.valueOf(pid).intValue());
                                    if (pids.isEmpty()) {
                                        this.mProcPkgMap.remove(pkg);
                                    }
                                }
                            }
                        }
                    }
                }
                SparseSet pids2 = this.mProcUidMap.get(uid);
                if (pids2 != null && pids2.contains(pid)) {
                    pids2.remove(pid);
                    if (pids2.isEmpty()) {
                        this.mProcUidMap.remove(uid);
                    }
                }
                for (int k = this.mAssocRecordMap.size() - 1; k >= 0; k--) {
                    AssocPidRecord record = this.mAssocRecordMap.valueAt(k);
                    if (record.pid == pid) {
                        this.mAssocRecordMap.removeAt(k);
                    } else {
                        if (br != null) {
                            record.mAssocBindService.remove(br.processName, br.uid);
                            record.mAssocProvider.remove(br.processName, br.uid);
                        }
                        if (record.isEmpty()) {
                            this.mAssocRecordMap.removeAt(k);
                        }
                    }
                }
            }
        } else if (DEBUG) {
            AwareLog.i(TAG, "removeDiedProcessRelation with wrong pid or uid");
        }
    }

    /* access modifiers changed from: private */
    public void removeDiedRecordProc(int uid, int pid) {
        if (uid <= 0) {
            AwareLog.i(TAG, "removeDiedRecodrProc with wrong pid or uid");
        } else {
            AwareIntelligentRecg.getInstance().removeDiedScreenProc(uid, pid);
        }
    }

    private static class LaunchData {
        private long mFirstTime;
        private int mLaunchTimes;

        private LaunchData(int launchTimes, long firstTime) {
            this.mLaunchTimes = launchTimes;
            this.mFirstTime = firstTime;
        }

        /* access modifiers changed from: private */
        public LaunchData increase() {
            this.mLaunchTimes++;
            return this;
        }

        /* access modifiers changed from: private */
        public long getFirstTime() {
            return this.mFirstTime;
        }

        /* access modifiers changed from: private */
        public int getLaunchTimes() {
            return this.mLaunchTimes;
        }
    }

    private void updateProcLaunchData(int uid, String proc, ArrayList<String> pkgList) {
        if (AppCleanupFeature.isAppCleanEnable() && UserHandle.getAppId(uid) >= 10000 && !UserHandle.isIsolated(uid)) {
            if (DEBUG) {
                AwareLog.i(TAG, "updateProcLaunchData, proc: " + proc + ", uid: " + uid + ", pkgList: " + pkgList);
            }
            synchronized (this.mProcLaunchMap) {
                int userId = UserHandle.getUserId(uid);
                Map<String, Map<String, LaunchData>> pkgMap = this.mProcLaunchMap.get(Integer.valueOf(userId));
                if (pkgMap == null) {
                    pkgMap = new HashMap();
                    this.mProcLaunchMap.put(Integer.valueOf(userId), pkgMap);
                }
                Iterator<String> it = pkgList.iterator();
                while (it.hasNext()) {
                    String pkg = it.next();
                    if (pkg != null) {
                        Map<String, LaunchData> procMap = pkgMap.get(pkg);
                        if (procMap == null) {
                            procMap = new HashMap();
                            pkgMap.put(pkg, procMap);
                        }
                        LaunchData launchData = procMap.get(proc);
                        if (launchData != null) {
                            procMap.put(proc, launchData.increase());
                            if (DEBUG) {
                                AwareLog.i(TAG, "updateProcLaunchData, pkg: " + pkg + ", launcTimes: " + launchData.getLaunchTimes());
                            }
                            if (launchData.getLaunchTimes() >= 30) {
                                if (SystemClock.elapsedRealtime() - launchData.getFirstTime() <= 300000) {
                                    Map<String, String> cleanMsg = new HashMap<>();
                                    cleanMsg.put("proc", proc);
                                    cleanMsg.put("pkg", pkg);
                                    cleanMsg.put(PermConst.USER_ID, "" + userId);
                                    Message msg = this.mHandler.obtainMessage();
                                    msg.what = 2;
                                    msg.obj = cleanMsg;
                                    this.mHandler.sendMessage(msg);
                                }
                                pkgMap.remove(pkg);
                            }
                        } else {
                            LaunchData launchData2 = new LaunchData(1, SystemClock.elapsedRealtime());
                            procMap.put(proc, launchData2);
                            if (DEBUG) {
                                AwareLog.i(TAG, "updateProcLaunchData, pkg: " + pkg + ", launcTimes: " + launchData2.getLaunchTimes());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateProcessRelation(int pid, int uid, String name, ArrayList<String> pkgList) {
        if (pid <= 0 || uid <= 0) {
            if (DEBUG) {
                AwareLog.i(TAG, "updateProcessRelation with wrong pid or uid");
            }
        } else if (name != null && pkgList != null) {
            if (DEBUG) {
                AwareLog.i(TAG, "update relation. Pid:" + pid + " Uid:" + uid + ",ProcessName:" + name);
            }
            updateProcLaunchData(uid, name, pkgList);
            synchronized (this.mLock) {
                for (int k = this.mAssocRecordMap.size() - 1; k >= 0; k--) {
                    AssocPidRecord record = this.mAssocRecordMap.valueAt(k);
                    if (record.pid == pid) {
                        this.mAssocRecordMap.removeAt(k);
                    } else {
                        AssocBaseRecord br = (AssocBaseRecord) record.mAssocBindService.get(name, uid);
                        if (br != null) {
                            br.pid = pid;
                        }
                        AssocBaseRecord br2 = (AssocBaseRecord) record.mAssocProvider.get(name, uid);
                        if (br2 != null) {
                            br2.pid = pid;
                        }
                    }
                }
                AssocBaseRecord br3 = this.mProcPidMap.get(pid);
                if (br3 == null) {
                    AssocBaseRecord br4 = new AssocBaseRecord(name, uid, pid);
                    br4.pkgList.addAll(pkgList);
                    this.mProcPidMap.put(pid, br4);
                } else {
                    br3.processName = name;
                    br3.uid = uid;
                    br3.pid = pid;
                    br3.pkgList.addAll(pkgList);
                }
                AssocBaseRecord br5 = (AssocBaseRecord) this.mProcInfoMap.get(name, uid);
                if (br5 == null) {
                    this.mProcInfoMap.put(name, uid, new AssocBaseRecord(name, uid, pid));
                } else {
                    br5.pid = pid;
                }
                SparseSet pids = this.mProcUidMap.get(uid);
                if (pids == null) {
                    SparseSet pids2 = new SparseSet();
                    pids2.add(pid);
                    this.mProcUidMap.put(uid, pids2);
                } else {
                    pids.add(pid);
                }
                int listSize = pkgList.size();
                for (int i = 0; i < listSize; i++) {
                    String pkg = pkgList.get(i);
                    synchronized (this.mProcPkgMap) {
                        SparseSet pids3 = this.mProcPkgMap.get(pkg);
                        if (pids3 == null) {
                            SparseSet pids4 = new SparseSet();
                            pids4.add(pid);
                            this.mProcPkgMap.put(pkg, pids4);
                        } else {
                            pids3.add(pid);
                        }
                    }
                }
            }
        } else if (DEBUG) {
            AwareLog.i(TAG, "updateProcessRelation with wrong name");
        }
    }

    /* access modifiers changed from: private */
    public void checkAndInitWidgetObj(int userId) {
        synchronized (this.mWidgets) {
            if (this.mWidgets.get(userId) == null) {
                this.mWidgets.put(userId, new SparseArray<>());
            }
        }
    }

    public int getCurUserId() {
        return this.mCurUserId;
    }

    public int getCurSwitchUser() {
        return this.mCurSwitchUser;
    }

    private void initSwitchUser() {
        try {
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            if (currentUser != null) {
                checkAndInitWidgetObj(currentUser.id);
                this.mCurUserId = currentUser.id;
                this.mCurSwitchUser = currentUser.id;
            }
            AwareCallback.getInstance().registerUserSwitchObserver(this.mUserSwitchObserver);
        } catch (RemoteException e) {
            AwareLog.d(TAG, "Activity manager not running, initSwitchUser error!");
        }
    }

    private void deInitSwitchUser() {
        AwareCallback.getInstance().unregisterUserSwitchObserver(this.mUserSwitchObserver);
    }

    /* access modifiers changed from: private */
    public void initialize() {
        if (!this.mIsInitialized.get()) {
            if (this.mMtmService == null) {
                this.mMtmService = MultiTaskManagerService.self();
            }
            if (!isUserUnlocked()) {
                if (this.mHandler.hasMessages(1)) {
                    this.mHandler.removeMessages(1);
                }
                this.mHandler.sendEmptyMessageDelayed(1, REINIT_TIME);
                return;
            }
            MultiTaskManagerService multiTaskManagerService = this.mMtmService;
            if (multiTaskManagerService != null) {
                if (multiTaskManagerService.context() != null) {
                    this.mSystemUnremoveUidCache = SystemUnremoveUidCache.getInstance(this.mMtmService.context());
                }
                initAssoc();
                registerProcessObserver();
                this.mIsInitialized.set(true);
            } else if (DEBUG) {
                AwareLog.w(TAG, "MultiTaskManagerService has not been started.");
            }
        }
    }

    private boolean isUserUnlocked() {
        UserManager userManager;
        MultiTaskManagerService multiTaskManagerService = this.mMtmService;
        if (multiTaskManagerService == null || (userManager = (UserManager) multiTaskManagerService.context().getSystemService("user")) == null) {
            return false;
        }
        return userManager.isUserUnlocked();
    }

    private synchronized void deInitialize() {
        if (this.mIsInitialized.get()) {
            unregisterProcessObserver();
            if (this.mMtmService != null) {
                this.mMtmService = null;
            }
            HwActivityManager.reportAssocDisable();
            deinitVisibleWindows();
            clearWidget();
            deinitAssoc();
            this.mIsInitialized.set(false);
        }
    }

    /* access modifiers changed from: private */
    public class AppAssocHandler extends Handler {
        public AppAssocHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (AwareAppAssociate.DEBUG) {
                AwareLog.e(AwareAppAssociate.TAG, "handleMessage message " + msg.what);
            }
            if (msg.what == 1) {
                AwareAppAssociate.this.initialize();
            } else if (msg.what == 2) {
                HashMap<String, String> cleanMsg = (HashMap) msg.obj;
                String pkg = cleanMsg.get("pkg");
                String proc = cleanMsg.get("proc");
                try {
                    int userId = Integer.parseInt(cleanMsg.get(PermConst.USER_ID));
                    if (userId < 0 || AwareAppAssociate.this.mMtmService == null) {
                        AwareLog.e(AwareAppAssociate.TAG, "MSG_CLEAN, userId or mMtmService error!");
                        return;
                    }
                    CrashClean crashClean = new CrashClean(userId, 0, pkg, AwareAppAssociate.this.mMtmService.context());
                    if (AwareAppAssociate.DEBUG) {
                        AwareLog.i(AwareAppAssociate.TAG, "Pkg:" + pkg + " will be cleaned due to high-freq-restart of proc:" + proc);
                    }
                    crashClean.clean();
                } catch (NumberFormatException e) {
                    AwareLog.e(AwareAppAssociate.TAG, "MSG_CLEAN, userId format error!");
                }
            } else if (msg.what == 3) {
                AwareAppAssociate.this.checkRecentForce();
            } else if (msg.what == 4) {
                AwareAppAssociate.this.clearRemoveVisWinDurScreenOff();
            }
        }
    }

    private void initAssoc() {
        if (this.mHwAMS != null) {
            synchronized (this.mLock) {
                AssocBaseRecord br = new AssocBaseRecord(SYSTEM, 1000, this.mMyPid);
                this.mProcPidMap.put(this.mMyPid, br);
                this.mProcInfoMap.put(SYSTEM, 1000, br);
                SparseSet pids = new SparseSet();
                pids.add(this.mMyPid);
                this.mProcUidMap.put(1000, pids);
            }
            initSwitchUser();
            initVisibleWindows();
            updateWidgets(this.mCurUserId);
            if (this.mCurUserId == 0) {
                AwareIntelligentRecg.getInstance().updateWidget(getWidgetsPkg(this.mCurUserId), null);
            }
            ArrayMap<Integer, Integer> forePids = new ArrayMap<>();
            this.mHwAMS.reportAssocEnable(forePids);
            synchronized (this.mForePids) {
                this.mForePids.clear();
                addAllForSparseIntArray(forePids, this.mForePids);
            }
            synchronized (this.mBgRecentForcePids) {
                this.mBgRecentForcePids.clear();
            }
        }
    }

    private void deinitAssoc() {
        synchronized (this.mForePids) {
            this.mForePids.clear();
        }
        synchronized (this.mBgRecentForcePids) {
            this.mBgRecentForcePids.clear();
        }
        synchronized (this.mLock) {
            this.mAssocRecordMap.clear();
            this.mProcInfoMap.getMap().clear();
            this.mProcPidMap.clear();
            this.mProcUidMap.clear();
            synchronized (this.mProcPkgMap) {
                this.mProcPkgMap.clear();
            }
        }
        deInitSwitchUser();
    }

    public void getPidsByUid(int uid, SparseSet pids) {
        if (mEnabled && uid > 0 && pids != null) {
            synchronized (this.mLock) {
                SparseSet procPids = this.mProcUidMap.get(uid);
                if (procPids != null) {
                    pids.addAll(procPids);
                }
            }
        }
    }

    public int getPidByNameAndUid(String procName, int uid) {
        if (!mEnabled || uid <= 0 || procName == null || procName.isEmpty()) {
            return -1;
        }
        synchronized (this.mLock) {
            AssocBaseRecord br = (AssocBaseRecord) this.mProcInfoMap.get(procName, uid);
            if (br == null) {
                return -1;
            }
            return br.pid;
        }
    }

    /* access modifiers changed from: private */
    public String sameUid(int pid) {
        StringBuilder sb = new StringBuilder();
        boolean flag = true;
        synchronized (this.mLock) {
            AssocBaseRecord br = this.mProcPidMap.get(pid);
            if (br == null) {
                return null;
            }
            SparseSet pids = this.mProcUidMap.get(br.uid);
            if (pids == null) {
                return null;
            }
            for (int i = pids.size() - 1; i >= 0; i--) {
                int tmp = pids.keyAt(i);
                if (tmp != pid) {
                    if (flag) {
                        sb.append("    [SameUID] depend on:\n");
                        flag = false;
                    }
                    AssocBaseRecord br2 = this.mProcPidMap.get(tmp);
                    if (br2 != null) {
                        sb.append("        Pid:");
                        sb.append(br2.pid);
                        sb.append(",Uid:");
                        sb.append(br2.uid);
                        sb.append(",ProcessName:");
                        sb.append(br2.processName);
                        sb.append("\n");
                    }
                }
            }
            return sb.toString();
        }
    }

    private Set<String> getPackageNameForUid(int uid, int pidForUid) {
        ArraySet<String> pkgList = new ArraySet<>();
        synchronized (this.mLock) {
            if (pidForUid != 0) {
                AssocBaseRecord br = this.mProcPidMap.get(pidForUid);
                if (!(br == null || br.pkgList == null)) {
                    pkgList.addAll(br.pkgList);
                }
                return pkgList;
            }
            SparseSet pids = this.mProcUidMap.get(uid);
            if (pids != null) {
                if (!pids.isEmpty()) {
                    for (int i = pids.size() - 1; i >= 0; i--) {
                        AssocBaseRecord br2 = this.mProcPidMap.get(pids.keyAt(i));
                        if (!(br2 == null || br2.pkgList == null)) {
                            pkgList.addAll(br2.pkgList);
                        }
                    }
                    return pkgList;
                }
            }
            return pkgList;
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            synchronized (this.mLock) {
                int listSize = this.mAssocRecordMap.size();
                for (int s = 0; s < listSize; s++) {
                    AssocPidRecord record = this.mAssocRecordMap.valueAt(s);
                    if (record != null) {
                        pw.println(record);
                    }
                }
            }
            dumpWidget(pw);
            dumpVisibleWindow(pw);
            pw.println("[mIsPreviousHighEnable] : " + this.mIsPreviousHighEnable);
        }
    }

    public void dumpFore(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            SparseSet tmp = new SparseSet();
            synchronized (this.mForePids) {
                for (int i = this.mForePids.size() - 1; i >= 0; i--) {
                    tmp.add(this.mForePids.keyAt(i));
                }
            }
            for (int j = tmp.size() - 1; j >= 0; j--) {
                dumpPid(tmp.keyAt(j), pw);
            }
        }
    }

    public void dumpRecentFore(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            SparseSet tmp = new SparseSet();
            synchronized (this.mBgRecentForcePids) {
                tmp.addAll(this.mBgRecentForcePids.keySet());
            }
            for (int i = tmp.size() - 1; i >= 0; i--) {
                dumpPid(tmp.keyAt(i), pw);
            }
        }
    }

    public void dumpPkgProc(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            synchronized (this.mProcPkgMap) {
                for (String pkg : this.mProcPkgMap.keySet()) {
                    pw.println(pkg + AwarenessInnerConstants.COLON_KEY + this.mProcPkgMap.get(pkg));
                }
            }
            pw.println("proc launch data:");
            synchronized (this.mProcLaunchMap) {
                for (Map.Entry<Integer, Map<String, Map<String, LaunchData>>> uidEntry : this.mProcLaunchMap.entrySet()) {
                    pw.println("  userId: " + uidEntry.getKey());
                    Map<String, Map<String, LaunchData>> pkgMap = uidEntry.getValue();
                    if (pkgMap != null) {
                        for (Map.Entry<String, Map<String, LaunchData>> pkgEntry : pkgMap.entrySet()) {
                            pw.println("    pkg: " + pkgEntry.getKey());
                            Map<String, LaunchData> procMap = pkgEntry.getValue();
                            if (procMap != null) {
                                for (Map.Entry<String, LaunchData> procEntry : procMap.entrySet()) {
                                    LaunchData lData = procEntry.getValue();
                                    if (lData != null) {
                                        pw.println("      proc: " + procEntry.getKey() + ", launchTime: " + lData.getLaunchTimes());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void dumpPid(int pid, PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            synchronized (this.mLock) {
                AssocPidRecord record = this.mAssocRecordMap.get(pid);
                if (record != null) {
                    pw.println(record);
                } else {
                    AssocBaseRecord br = this.mProcPidMap.get(pid);
                    if (br != null) {
                        pw.println("Pid:" + br.pid + ",Uid:" + br.uid + ",ProcessName:" + br.processName);
                    }
                    pw.println(sameUid(pid));
                }
            }
        }
    }

    public void dumpVisibleWindow(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            SparseSet windows = new SparseSet();
            SparseSet windowsEvil = new SparseSet();
            getVisibleWindows(windows, windowsEvil);
            boolean flag = true;
            pw.println("");
            synchronized (this.mLock) {
                for (int i = windows.size() - 1; i >= 0; i--) {
                    AssocBaseRecord br = this.mProcPidMap.get(windows.keyAt(i));
                    if (br != null) {
                        if (flag) {
                            pw.println("[WindowList] :");
                            flag = false;
                        }
                        pw.println("    Pid:" + br.pid + ",Uid:" + br.uid + ",ProcessName:" + br.processName + ",PkgList:" + br.pkgList);
                    }
                }
                boolean flag2 = true;
                for (int i2 = windowsEvil.size() - 1; i2 >= 0; i2--) {
                    AssocBaseRecord br2 = this.mProcPidMap.get(windowsEvil.keyAt(i2));
                    if (br2 != null) {
                        if (flag2) {
                            pw.println("[WindowEvilList] :");
                            flag2 = false;
                        }
                        pw.println("    Pid:" + br2.pid + ",Uid:" + br2.uid + ",ProcessName:" + br2.processName + ",PkgList:" + br2.pkgList);
                    }
                }
            }
            SparseSet windowsClean = new SparseSet();
            getVisibleWindowsInRestriction(windowsClean);
            pw.println("[WindowList in restriction] :" + windowsClean);
        }
    }

    public void dumpWidget(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            Set<String> widgets = getWidgetsPkg();
            pw.println("[Widgets] : " + widgets.size());
            Iterator<String> it = widgets.iterator();
            while (it.hasNext()) {
                pw.println("    " + it.next());
            }
        }
    }

    public void dumpHome(PrintWriter pw) {
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            synchronized (this.mLock) {
                AssocBaseRecord br = this.mProcPidMap.get(this.mHomeProcessPid);
                if (br != null) {
                    pw.println("[Home]Pid:" + this.mHomeProcessPid + ",Uid:" + br.uid + ",ProcessName:" + br.processName + ",pkg:" + br.pkgList);
                }
            }
        }
    }

    public void dumpPrev(PrintWriter pw) {
        String eclipseTime;
        String eclipseTime2;
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            int pid = 0;
            Set<String> pkgList = getPackageNameForUid(this.mPrevNonHomeBase.mUid, isDealAsPkgUid(this.mPrevNonHomeBase.mUid) ? this.mPrevNonHomeBase.mPid : 0);
            if (this.mPrevNonHomeBase.mUid == 0) {
                eclipseTime = " none";
            } else {
                eclipseTime = " " + ((SystemClock.elapsedRealtime() - this.mPrevNonHomeBase.mTime) / 1000);
            }
            pw.println("[Prev Non Home] Uid:" + this.mPrevNonHomeBase.mUid + ",pid:" + this.mPrevNonHomeBase.mPid + ",pkg:" + pkgList + ",eclipse(s):" + eclipseTime);
            boolean isRecentTaskShow = false;
            synchronized (this.mForePids) {
                if (this.mForePids.size() == 0) {
                    isRecentTaskShow = true;
                }
            }
            if (isRecentTaskShow) {
                pw.println("[Prev Recent Task] Uid:" + this.mRecentTaskPrevBase.mUid + ",pid:" + this.mRecentTaskPrevBase.mPid + ",pkg:" + getPackageNameForUid(this.mRecentTaskPrevBase.mUid, isDealAsPkgUid(this.mRecentTaskPrevBase.mUid) ? this.mRecentTaskPrevBase.mPid : 0));
            } else {
                pw.println("[Prev Recent Task] Uid: None");
            }
            if (isDealAsPkgUid(this.mAmsPrevBase.mUid)) {
                pid = this.mAmsPrevBase.mPid;
            }
            Set<String> pkgList2 = getPackageNameForUid(this.mAmsPrevBase.mUid, pid);
            if (this.mAmsPrevBase.mUid == 0) {
                eclipseTime2 = " none";
            } else {
                eclipseTime2 = " " + ((SystemClock.elapsedRealtime() - this.mAmsPrevBase.mTime) / 1000);
            }
            pw.println("[Prev By Ams] Uid:" + this.mAmsPrevBase.mUid + ",pid:" + this.mAmsPrevBase.mPid + ",pkg:" + pkgList2 + ",eclipse(s):" + eclipseTime2);
        }
    }

    /* JADX INFO: Multiple debug info for r15v1 int: [D('curcompsize' int), D('curpidsize' int)] */
    public void dumpRecord(PrintWriter pw) {
        int bindSizeAll;
        int bindSize;
        int providerSize;
        int sameuid;
        int curpidsize;
        int curpidsize2;
        int curpiduidsize;
        int pidsize;
        int compSize;
        AwareAppAssociate awareAppAssociate = this;
        if (pw != null) {
            if (!mEnabled) {
                pw.println("AwareAppAssociate feature disabled.");
                return;
            }
            int pidsize2 = 0;
            int compSize2 = 0;
            Set<String> widgets = getWidgetsPkg();
            pw.println("Widget Size: " + widgets.size());
            SparseSet windows = new SparseSet();
            SparseSet windowsEvil = new SparseSet();
            awareAppAssociate.getVisibleWindows(windows, windowsEvil);
            pw.println("Window Size: " + windows.size() + ", EvilWindow Size: " + windowsEvil.size());
            synchronized (awareAppAssociate.mLock) {
                try {
                    int pidSize = awareAppAssociate.mAssocRecordMap.size();
                    int s = 0;
                    while (s < pidSize) {
                        int providerSize2 = 0;
                        int providerSizeAll = 0;
                        int sameuid2 = 0;
                        try {
                            AssocPidRecord record = awareAppAssociate.mAssocRecordMap.valueAt(s);
                            try {
                                int NP = record.mAssocBindService.getMap().size();
                                bindSizeAll = 0;
                                bindSize = 0;
                                int i = 0;
                                while (i < NP) {
                                    try {
                                        SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                                        int NB = brs.size();
                                        bindSize += NB;
                                        int bindSizeAll2 = bindSizeAll;
                                        int j = 0;
                                        while (j < NB) {
                                            bindSizeAll2 += brs.valueAt(j).mComponents.size();
                                            j++;
                                            brs = brs;
                                            bindSize = bindSize;
                                        }
                                        i++;
                                        NP = NP;
                                        providerSize2 = providerSize2;
                                        bindSizeAll = bindSizeAll2;
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                                int NP2 = record.mAssocProvider.getMap().size();
                                int i2 = 0;
                                providerSize = providerSize2;
                                while (i2 < NP2) {
                                    SparseArray<AssocBaseRecord> brs2 = (SparseArray) record.mAssocProvider.getMap().valueAt(i2);
                                    int NB2 = brs2.size();
                                    providerSize += NB2;
                                    int providerSizeAll2 = providerSizeAll;
                                    int j2 = 0;
                                    while (j2 < NB2) {
                                        providerSizeAll2 += brs2.valueAt(j2).mComponents.size();
                                        j2++;
                                        brs2 = brs2;
                                        providerSize = providerSize;
                                    }
                                    i2++;
                                    NP2 = NP2;
                                    sameuid2 = sameuid2;
                                    providerSizeAll = providerSizeAll2;
                                }
                                SparseSet pids = awareAppAssociate.mProcUidMap.get(record.uid);
                                if (pids != null) {
                                    sameuid = pids.size() - 1;
                                } else {
                                    sameuid = sameuid2;
                                }
                                curpidsize = bindSize + providerSize;
                                curpidsize2 = bindSizeAll + providerSizeAll;
                                curpiduidsize = curpidsize + sameuid;
                                pidsize = pidsize2 + curpidsize;
                                compSize = compSize2 + curpidsize2;
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                            try {
                                StringBuilder sb = new StringBuilder();
                                try {
                                    sb.append("[");
                                    sb.append(record.uid);
                                    sb.append("][");
                                    sb.append(record.processName);
                                    sb.append("]: bind[");
                                    sb.append(bindSize);
                                    sb.append(AwarenessInnerConstants.DASH_KEY);
                                    sb.append(bindSizeAll);
                                    sb.append("]provider[");
                                    sb.append(providerSize);
                                    sb.append(AwarenessInnerConstants.DASH_KEY);
                                    sb.append(providerSizeAll);
                                    sb.append("]SameUID[");
                                    sb.append(sameuid);
                                    sb.append("]pids:[");
                                    sb.append(curpidsize);
                                    sb.append("]comps:[");
                                    sb.append(curpidsize2);
                                    sb.append("]piduids:[");
                                    sb.append(curpiduidsize);
                                    sb.append("]");
                                    pw.println(sb.toString());
                                    s++;
                                    awareAppAssociate = this;
                                    pidsize2 = pidsize;
                                    compSize2 = compSize;
                                    widgets = widgets;
                                    windows = windows;
                                    windowsEvil = windowsEvil;
                                } catch (Throwable th3) {
                                    th = th3;
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            throw th;
                        }
                    }
                    pw.println("PidRecord Size: " + pidSize + " " + pidsize2 + " " + compSize2);
                } catch (Throwable th6) {
                    th = th6;
                    throw th;
                }
            }
        }
    }

    private void recordWindowDetail(SparseSet list) {
        if (list != null && !list.isEmpty()) {
            synchronized (this.mLock) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    AssocBaseRecord br = this.mProcPidMap.get(list.keyAt(i));
                    if (br != null) {
                        AwareLog.i(TAG, "[Window]Pid:" + br.pid + ",Uid:" + br.uid + ",ProcessName:" + br.processName);
                    }
                }
            }
        }
    }

    private void recordAssocDetail(int pid) {
        synchronized (this.mLock) {
            AssocPidRecord record = this.mAssocRecordMap.get(pid);
            if (record != null) {
                AwareLog.i(TAG, "" + record);
            } else {
                AssocBaseRecord br = this.mProcPidMap.get(pid);
                if (br != null) {
                    AwareLog.i(TAG, "Pid:" + br.pid + ",Uid:" + br.uid + ",ProcessName:" + br.processName);
                }
                AwareLog.i(TAG, "" + sameUid(pid));
            }
        }
    }

    public static void enable() {
        mEnabled = true;
        AwareAppAssociate awareAppAssociate = mAwareAppAssociate;
        if (awareAppAssociate != null) {
            awareAppAssociate.initialize();
        }
    }

    public static void disable() {
        mEnabled = false;
        AwareAppAssociate awareAppAssociate = mAwareAppAssociate;
        if (awareAppAssociate != null) {
            awareAppAssociate.deInitialize();
        }
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public static void enableRecord() {
        RECORD = true;
    }

    public static void disableRecord() {
        RECORD = false;
    }

    public boolean isPkgHasProc(String pkg) {
        boolean z = true;
        if (!mEnabled) {
            return true;
        }
        synchronized (this.mProcPkgMap) {
            if (this.mProcPkgMap.get(pkg) == null) {
                z = false;
            }
        }
        return z;
    }

    public void registerVisibleCallback(IAwareVisibleCallback callback) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                if (!this.mCallbacks.contains(callback)) {
                    this.mCallbacks.add(callback);
                }
            }
        }
    }

    public void unregisterVisibleCallback(IAwareVisibleCallback callback) {
        if (callback != null) {
            synchronized (this.mCallbacks) {
                if (this.mCallbacks.contains(callback)) {
                    this.mCallbacks.remove(callback);
                }
            }
        }
    }

    public void notifyVisibleWindowsChange(int type, int window, int mode) {
        synchronized (this.mCallbacks) {
            if (!this.mCallbacks.isEmpty()) {
                int callbackSize = this.mCallbacks.size();
                for (int i = 0; i < callbackSize; i++) {
                    this.mCallbacks.valueAt(i).onVisibleWindowsChanged(type, window, mode);
                }
            }
        }
    }

    public void screenStateChange(boolean screenOff) {
        AppAssocHandler appAssocHandler;
        this.mScreenOff = screenOff;
        if (screenOff && (appAssocHandler = this.mHandler) != null) {
            appAssocHandler.removeMessages(4);
        }
    }

    /* access modifiers changed from: private */
    public void clearRemoveVisWinDurScreenOff() {
        synchronized (this.mVisWinDurScreenOff) {
            if (!this.mVisWinDurScreenOff.isEmpty()) {
                this.mVisWinDurScreenOff.clear();
            }
        }
    }

    public void checkBakUpVisWin() {
        AppAssocHandler appAssocHandler = this.mHandler;
        if (appAssocHandler != null) {
            appAssocHandler.removeMessages(4);
            this.mHandler.sendEmptyMessageDelayed(4, 5000);
        }
    }

    private void updateVisibleWindowsCache(int type, int pid, int mode, String pkg, int uid, int code, boolean evil) {
        if (type == 0) {
            AwareProcessWindowInfo winInfoCache = new AwareProcessWindowInfo(mode, pkg, uid);
            synchronized (this.mVisibleWindowsCache) {
                this.mVisibleWindowsCache.put(Integer.valueOf(pid), winInfoCache);
            }
        } else if (type == 1) {
            synchronized (this.mVisibleWindowsCache) {
                this.mVisibleWindowsCache.remove(Integer.valueOf(pid));
            }
        } else if (type == 2) {
            synchronized (this.mVisibleWindowsCache) {
                this.mVisibleWindowsCache.clear();
            }
        } else if (type == 3) {
            synchronized (this.mVisibleWindowsCache) {
                AwareProcessWindowInfo winInfo = this.mVisibleWindowsCache.get(Integer.valueOf(pid));
                if (winInfo != null) {
                    winInfo.mode = mode;
                }
            }
        } else if (type == 4) {
            synchronized (this.mVisibleWindowsCache) {
                AwareProcessWindowInfo winInfo2 = this.mVisibleWindowsCache.get(Integer.valueOf(pid));
                if (winInfo2 != null) {
                    winInfo2.addWindow(Integer.valueOf(code), evil);
                }
            }
        } else if (type == 5) {
            synchronized (this.mVisibleWindowsCache) {
                AwareProcessWindowInfo winInfo3 = this.mVisibleWindowsCache.get(Integer.valueOf(pid));
                if (winInfo3 != null) {
                    winInfo3.removeWindow(Integer.valueOf(code));
                }
            }
        }
    }

    public boolean isVisibleWindow(int pid) {
        boolean allowedWindow;
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mVisibleWindowsCache) {
            AwareProcessWindowInfo winInfo = this.mVisibleWindowsCache.get(Integer.valueOf(pid));
            if (winInfo != null) {
                if (winInfo.mode != 0) {
                    if (winInfo.mode != 3) {
                        allowedWindow = false;
                        if (allowedWindow && !winInfo.isEvil()) {
                            return true;
                        }
                    }
                }
                allowedWindow = true;
                return true;
            }
            return false;
        }
    }

    public Set<AwareAppLruBase> getPreviousAppOpt() {
        Set<AwareAppLruBase> previousApp = new ArraySet<>();
        SparseSet foregroundPids = new SparseSet();
        synchronized (this.mForePids) {
            for (int i = this.mForePids.size() - 1; i >= 0; i--) {
                foregroundPids.add(this.mForePids.keyAt(i));
            }
        }
        int previousCount = getPreviousCount(foregroundPids);
        LinkedHashMap<Integer, AwareAppLruBase> lruCache = getActivityLruCache();
        ListIterator<Map.Entry<Integer, AwareAppLruBase>> iter = new ArrayList(lruCache.entrySet()).listIterator(lruCache.size());
        synchronized (this.mLock) {
            while (true) {
                if (!iter.hasPrevious()) {
                    break;
                }
                Map.Entry<Integer, AwareAppLruBase> entry = iter.previous();
                if (previousCount <= 0) {
                    break;
                }
                AwareAppLruBase app = entry.getValue();
                AssocBaseRecord assocBaseRecord = this.mProcPidMap.get(app.mPid);
                if (!foregroundPids.contains(app.mPid)) {
                    if (!(assocBaseRecord == null || assocBaseRecord.pkgList == null)) {
                        if (!assocBaseRecord.pkgList.contains("com.android.systemui")) {
                            if (assocBaseRecord.pkgList.contains(PERMISSIOM_CONTROLLER)) {
                            }
                        }
                    }
                    if (app.mPid != this.mHomeProcessPid) {
                        previousApp.add(app);
                        previousCount--;
                        if (this.mMorePreviousLevel == 2 && assocBaseRecord != null && assocBaseRecord.pkgList != null && !assocBaseRecord.pkgList.contains(AwareIntelligentRecg.getInstance().getDefaultSmsPackage())) {
                            previousCount--;
                        }
                    }
                }
            }
        }
        return previousApp;
    }

    private int getPreviousCount(SparseSet foregroundPids) {
        int i = this.mMorePreviousLevel;
        if (i == 4) {
            return 4;
        }
        if (i == 0 || i > 2) {
            return 1;
        }
        synchronized (this.mLock) {
            for (int i2 = foregroundPids.size() - 1; i2 >= 0; i2--) {
                int forePid = foregroundPids.keyAt(i2);
                if (this.mHomeProcessPid == forePid) {
                    return 2;
                }
                AssocBaseRecord br = this.mProcPidMap.get(forePid);
                if (br != null) {
                    if (br.pkgList != null) {
                        if (br.pkgList.contains("com.android.systemui")) {
                            return 2;
                        }
                    }
                }
            }
            return 1;
        }
    }

    public void setMorePreviousLevel(int levelValue) {
        this.mMorePreviousLevel = levelValue;
    }

    private int decideMorePreviousLevel() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long totalMemMb = minfo.getTotalSize() / 1048576;
        if (isPreviousHighEnable() && totalMemMb > 6144) {
            return 4;
        }
        if (totalMemMb > 3072) {
            return 1;
        }
        return 2;
    }

    private boolean isPreviousHighEnable() {
        int i = this.mIsPreviousHighEnable;
        if (i == 1) {
            return true;
        }
        if (i == 0) {
            return false;
        }
        return !this.mIsLowDevice;
    }

    private boolean checkAndUpdateWindowInfo(AwareProcessWindowInfo winInfo, int width, int height, float alpha, boolean hasSurface) {
        if (winInfo == null) {
            return false;
        }
        if (width == -1 && height == -1) {
            width = winInfo.width;
            height = winInfo.height;
        }
        winInfo.width = width;
        winInfo.height = height;
        boolean isMiniWindow = width <= AwareProcessWindowInfo.getMinWindowWidth() || height <= AwareProcessWindowInfo.getMinWindowHeight();
        boolean isInvisible = alpha == 0.0f || (!hasSurface && AwareIntelligentRecg.getInstance().isRecogOptEnable());
        if (isMiniWindow || isInvisible) {
            return true;
        }
        return false;
    }

    public boolean isSystemUnRemoveApp(int uid) {
        if (!mEnabled) {
            return false;
        }
        int appUid = UserHandle.getAppId(uid);
        if (appUid < 10000) {
            return true;
        }
        SystemUnremoveUidCache systemUnremoveUidCache = this.mSystemUnremoveUidCache;
        if (systemUnremoveUidCache == null || !systemUnremoveUidCache.checkUidExist(appUid)) {
            return false;
        }
        return true;
    }

    private void addAppCompJobLock(AssocBaseRecord baseRecord, String comp, int hwFlag) {
        if (baseRecord != null && needRecgCompJob(baseRecord.uid, hwFlag)) {
            baseRecord.mComponentsJob.add(comp);
        }
    }

    private void removeAppCompJobLock(AssocBaseRecord baseRecord, String comp, int hwFlag) {
        if (baseRecord != null && needRecgCompJob(baseRecord.uid, hwFlag)) {
            baseRecord.mComponentsJob.remove(comp);
        }
    }

    private boolean isJobFlag(int hwFlag) {
        return (hwFlag & 8224) != 0;
    }

    private boolean needRecgCompJob(int uid, int hwFlag) {
        if (isJobFlag(hwFlag) && !isSystemUnRemoveApp(uid)) {
            return true;
        }
        return false;
    }

    public boolean isJobDoingForUid(int uid) {
        if (!mEnabled) {
            return false;
        }
        synchronized (this.mLock) {
            AssocPidRecord record = this.mAssocRecordMap.get(this.mMyPid);
            if (record == null) {
                return false;
            }
            int mapSize = record.mAssocBindService.getMap().size();
            for (int i = 0; i < mapSize; i++) {
                SparseArray<AssocBaseRecord> brs = (SparseArray) record.mAssocBindService.getMap().valueAt(i);
                int arraySize = brs.size();
                for (int j = 0; j < arraySize; j++) {
                    AssocBaseRecord br = brs.valueAt(j);
                    if (br != null) {
                        if (br.uid != uid) {
                            continue;
                        } else if (!br.mComponentsJob.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private void addAllForSparseIntArray(Map<Integer, Integer> from, SparseIntArray to) {
        for (Map.Entry<Integer, Integer> map : from.entrySet()) {
            to.put(map.getKey().intValue(), map.getValue().intValue());
        }
    }

    /* access modifiers changed from: private */
    public void addAllForSparseIntArray(SparseIntArray from, SparseIntArray to) {
        for (int i = from.size() - 1; i >= 0; i--) {
            to.put(from.keyAt(i), from.valueAt(i));
        }
    }

    public static boolean isEnabled() {
        return mEnabled;
    }

    private boolean isAppLock(int uid) {
        ActivityInfo activityInfo;
        ComponentName componentName;
        if (UserHandle.getAppId(uid) != 1000 || (activityInfo = HwActivityTaskManager.getLastResumedActivity()) == null || (componentName = activityInfo.getComponentName()) == null) {
            return false;
        }
        return AwareIntelligentRecg.getInstance().isAppLockClassName(componentName.getClassName());
    }
}
