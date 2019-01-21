package com.android.server.mtm.iaware.appmng;

import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.app.mtm.iaware.appmng.AppMngConstant.CleanReason;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo.XmlConfig;
import com.android.server.mtm.iaware.appmng.appclean.CleanSource;
import com.android.server.mtm.iaware.appmng.appclean.SmartClean;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.AppStatusUtils;
import com.android.server.mtm.utils.AppStatusUtils.Status;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.appmng.AppMngConfig;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppLruBase;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList.PackageConfigItem;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList.ProcessConfigItem;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.srms.AppCleanupDumpRadar;
import com.huawei.android.app.HwActivityManager;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AwareAppMngSort {
    public static final String ACTIVITY_RECENT_TASK = "com.android.systemui/.recents.RecentsActivity";
    public static final int ACTIVITY_TASK_IMPORT_CNT = 2;
    public static final String ADJTYPE_SERVICE = "service";
    public static final int APPMNG_MEM_ALLOWSTOP_GROUP = 2;
    public static final int APPMNG_MEM_ALL_GROUP = 3;
    public static final int APPMNG_MEM_FORBIDSTOP_GROUP = 0;
    public static final int APPMNG_MEM_SHORTAGESTOP_GROUP = 1;
    public static final int APPSORT_FORCOMPACT = 1;
    public static final int APPSORT_FORMEM = 0;
    public static final int APPSORT_FORMEMCLEAN = 2;
    private static final int BETA_LOG_PRINT_INTERVEL = 60000;
    private static final Comparator<AwareProcessBlockInfo> BLOCK_BY_ADJ = new Comparator<AwareProcessBlockInfo>() {
        public int compare(AwareProcessBlockInfo arg0, AwareProcessBlockInfo arg1) {
            if (arg0 == null) {
                if (arg1 == null) {
                    return 0;
                }
                return -1;
            } else if (arg1 == null) {
                return 1;
            } else {
                return arg0.mMinAdj - arg1.mMinAdj;
            }
        }
    };
    private static final Comparator<AwareProcessBlockInfo> BLOCK_BY_IMPORTANCE = new Comparator<AwareProcessBlockInfo>() {
        public int compare(AwareProcessBlockInfo arg0, AwareProcessBlockInfo arg1) {
            return arg1.mImportance - arg0.mImportance;
        }
    };
    private static final Comparator<AwareProcessBlockInfo> BLOCK_BY_USER_HABIT = new Comparator<AwareProcessBlockInfo>() {
        public int compare(AwareProcessBlockInfo arg0, AwareProcessBlockInfo arg1) {
            if (arg0 == null) {
                if (arg1 == null) {
                    return 0;
                }
                return -1;
            } else if (arg1 == null) {
                return 1;
            } else {
                return arg0.mImportance - arg1.mImportance;
            }
        }
    };
    private static final Comparator<AwareProcessBlockInfo> BLOCK_BY_WEIGHT = new Comparator<AwareProcessBlockInfo>() {
        public int compare(AwareProcessBlockInfo arg1, AwareProcessBlockInfo arg0) {
            if (arg0 == null) {
                if (arg1 == null) {
                    return 0;
                }
                return -1;
            } else if (arg1 == null) {
                return 1;
            } else {
                return arg0.mWeight - arg1.mWeight;
            }
        }
    };
    private static final int CLASSRATE_KEY_OFFSET = 8;
    private static final String CSP_APPS = "csp_apps";
    private static boolean DEBUG = false;
    public static final String EXEC_SERVICES = "exec-service";
    public static final String FG_SERVICE = "fg-service";
    public static final long FOREVER_DECAYTIME = -1;
    public static final int HABITMAX_IMPORT = 10000;
    public static final int HABITMIN_IMPORT = 0;
    private static final int INVALID_VALUE = -1;
    private static final String LIST_FILTER_LEVEL2 = "list-filter-lv2";
    private static final int MAX_IMPORTANCE_VAL = 10000;
    private static final int MAX_TOP_N_NUM = 8;
    public static final int MEM_LEVEL0 = 0;
    public static final int MEM_LEVEL1 = 1;
    public static final int MEM_LEVEL2 = 2;
    public static final int MEM_LEVEL3 = 3;
    public static final int MEM_LEVEL_DEFAULT = 0;
    public static final int MEM_LEVEL_KILL_MORE = 1;
    private static final int MSG_PRINT_BETA_LOG = 1;
    public static final long PREVIOUS_APP_DIRCACTIVITY_DECAYTIME = 600000;
    private static final int SEC_PER_MIN = 60;
    private static final String SUBTYPE_ASSOCIATION = "assoc";
    private static final String TAG = "AwareAppMngSort";
    private static final int TOP_N_IMPORT_RATE = -100;
    private static ArrayList<String> fileredApps = null;
    private static boolean mEnabled = false;
    private static ArraySet<String> mListFilerLevel2 = null;
    private static AwareAppMngSort sInstance = null;
    private boolean mAssocEnable = true;
    private final Context mContext;
    private Handler mHandler = null;
    private HwActivityManagerService mHwAMS = null;
    private long mLastBetaLogOutTime = 0;

    public enum AllowStopSubClassRate {
        NONE("none"),
        PREVIOUS("previous"),
        TOPN("user_topN"),
        KEY_SYS_SERVICE("keySysService"),
        HEALTH("health"),
        FG_SERVICES("fg_services"),
        OTHER("other"),
        NONCURUSER("nonCurUser"),
        UNKNOWN("unknown");
        
        String mDescription;

        private AllowStopSubClassRate(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    static class AppBlockKeyBase {
        public int mPid = 0;
        public int mUid = 0;

        public AppBlockKeyBase(int pid, int uid) {
            this.mPid = pid;
            this.mUid = uid;
            if (AwareAppMngSort.DEBUG) {
                String str = AwareAppMngSort.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AppBlockKeyBase constructor pid:");
                stringBuilder.append(this.mPid);
                stringBuilder.append(",uid:");
                stringBuilder.append(this.mUid);
                AwareLog.d(str, stringBuilder.toString());
            }
        }
    }

    private static final class BetaLog {
        private static final char FLAG_ITEM_INNER_SPLIT = ',';
        private static final char FLAG_ITEM_SPLIT = ';';
        private static final char FLAG_NEW_LINE = '\n';
        private static final int ITEMS_ONE_LINE = 10;
        private static final int PROCESS_INFO_CNT = 2;
        private List<String> mData = new ArrayList();

        BetaLog(AwareAppMngSortPolicy policy) {
            if (policy.getForbidStopProcBlockList() != null) {
                inflat(policy.getForbidStopProcBlockList());
                inflat(policy.getShortageStopProcBlockList());
                inflat(policy.getAllowStopProcBlockList());
            }
        }

        private void inflat(List<AwareProcessBlockInfo> list) {
            if (list != null) {
                for (AwareProcessBlockInfo pinfo : list) {
                    if (pinfo != null) {
                        if (pinfo.mProcessList != null) {
                            this.mData.add(pinfo.mPackageName);
                            addDetailedReason(pinfo);
                        }
                    }
                }
            }
        }

        private void addDetailedReason(AwareProcessBlockInfo info) {
            if (info != null && info.mDetailedReason != null) {
                Integer specialReason = (Integer) info.mDetailedReason.get("spec");
                int reasonLength;
                if (specialReason != null) {
                    reasonLength = CleanReason.values().length;
                    if (specialReason.intValue() >= 0 && specialReason.intValue() < reasonLength) {
                        this.mData.add(CleanReason.values()[specialReason.intValue()].getAbbr());
                        return;
                    }
                    return;
                }
                reasonLength = AppMngTag.values().length;
                StringBuilder ruleValue = new StringBuilder();
                for (int i = 0; i < reasonLength; i++) {
                    Integer value = (Integer) info.mDetailedReason.get(AppMngTag.values()[i].getDesc());
                    if (value == null) {
                        ruleValue.append(Integer.toString(-1));
                    } else {
                        ruleValue.append(value.toString());
                    }
                    if (i != reasonLength - 1) {
                        ruleValue.append(FLAG_ITEM_INNER_SPLIT);
                    }
                }
                this.mData.add(ruleValue.toString());
            }
        }

        public void print() {
            int size = this.mData.size();
            if (size != 0 && size % 2 == 0) {
                StringBuilder outStr = new StringBuilder();
                int cnt = 0;
                for (String cur : this.mData) {
                    outStr.append(cur);
                    cnt++;
                    if (cnt % 2 != 0) {
                        outStr.append(FLAG_ITEM_INNER_SPLIT);
                    } else if (cnt % 20 == 0) {
                        outStr.append(FLAG_NEW_LINE);
                    } else {
                        outStr.append(FLAG_ITEM_SPLIT);
                    }
                }
                AwareLog.i(AwareAppMngSort.TAG, outStr.toString());
            }
        }
    }

    private static final class BetaLogHandler extends Handler {
        public BetaLogHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                BetaLog betaLog = msg.obj;
                if (betaLog != null) {
                    betaLog.print();
                }
            }
        }
    }

    static class CachedWhiteList {
        private final ArraySet<String> mAllHabitCacheList = new ArraySet();
        final Set<String> mAllProtectApp = new ArraySet();
        final Set<String> mAllUnProtectApp = new ArraySet();
        private Map<String, PackageConfigItem> mAwareProtectCacheMap = new ArrayMap();
        final ArraySet<String> mBadAppList = new ArraySet();
        final ArraySet<String> mBgNonDecayPkg = new ArraySet();
        private final ArraySet<String> mKeyHabitCacheList = new ArraySet();
        private boolean mLowEnd = false;
        final ArraySet<String> mRestartAppList = new ArraySet();

        public void updateCachedList() {
            AwareDefaultConfigList whiteListInstance = AwareDefaultConfigList.getInstance();
            if (whiteListInstance != null) {
                this.mLowEnd = whiteListInstance.isLowEnd();
                this.mKeyHabitCacheList.addAll(whiteListInstance.getKeyHabitAppList());
                this.mAllHabitCacheList.addAll(whiteListInstance.getAllHabitAppList());
                this.mRestartAppList.addAll(whiteListInstance.getRestartAppList());
                this.mBadAppList.addAll(whiteListInstance.getBadAppList());
                AwareUserHabit habit = AwareUserHabit.getInstance();
                if (habit != null) {
                    Set<String> bgNonDcyApp = habit.getBackgroundApps(AppMngConfig.getBgDecay() * 60);
                    if (bgNonDcyApp != null) {
                        this.mBgNonDecayPkg.addAll(bgNonDcyApp);
                    }
                }
                this.mAwareProtectCacheMap = whiteListInstance.getAwareProtectMap();
            }
        }

        private boolean isLowEnd() {
            return this.mLowEnd;
        }

        public boolean isInKeyHabitList(ArrayList<String> packageNames) {
            if (packageNames == null || packageNames.isEmpty()) {
                return false;
            }
            int size = packageNames.size();
            for (int i = 0; i < size; i++) {
                if (this.mKeyHabitCacheList.contains((String) packageNames.get(i))) {
                    return true;
                }
            }
            return false;
        }

        public boolean isInAllHabitList(ArrayList<String> packageNames) {
            if (packageNames == null || packageNames.isEmpty()) {
                return false;
            }
            int size = packageNames.size();
            for (int i = 0; i < size; i++) {
                if (this.mAllHabitCacheList.contains((String) packageNames.get(i))) {
                    return true;
                }
            }
            return false;
        }

        private ProcessConfigItem getAwareWhiteListItem(ArrayList<String> packageNames, String processName) {
            if (packageNames == null || packageNames.isEmpty() || this.mAwareProtectCacheMap == null) {
                return null;
            }
            int size = packageNames.size();
            for (int i = 0; i < size; i++) {
                String pkgName = (String) packageNames.get(i);
                if (this.mAwareProtectCacheMap.containsKey(pkgName)) {
                    PackageConfigItem pkgItem = (PackageConfigItem) this.mAwareProtectCacheMap.get(pkgName);
                    if (pkgItem == null) {
                        return null;
                    }
                    if (pkgItem.isEmpty()) {
                        return pkgItem.copy();
                    }
                    ProcessConfigItem procItem = pkgItem.getItem(processName);
                    if (procItem == null) {
                        return null;
                    }
                    return procItem.copy();
                }
            }
            return null;
        }

        private int getGroupId(ProcessConfigItem item) {
            if (item == null) {
                return 2;
            }
            int group = 2;
            switch (item.mGroupId) {
                case 1:
                    group = 0;
                    break;
                case 2:
                    group = 1;
                    break;
            }
            return group;
        }

        private void updateProcessInfoByConfig(AwareProcessInfo processInfo) {
            if (processInfo != null && processInfo.mProcInfo != null) {
                ProcessConfigItem item = getAwareWhiteListItem(processInfo.mProcInfo.mPackageName, processInfo.mProcInfo.mProcessName);
                if (item != null) {
                    processInfo.mXmlConfig = new XmlConfig(getGroupId(item), item.mFrequentlyUsed, item.mResCleanAllow, item.mRestartFlag);
                }
            }
        }
    }

    public enum ClassRate {
        NONE("none"),
        PERSIST("persist"),
        FOREGROUND(MemoryConstant.MEM_REPAIR_CONSTANT_FG),
        KEYBACKGROUND("keybackground"),
        HOME("home"),
        KEYSERVICES("keyservices"),
        NORMAL("normal"),
        UNKNOWN("unknown");
        
        String mDescription;

        private ClassRate(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    public enum ForbidSubClassRate {
        NONE("none"),
        PREVIOUS("previous"),
        AWARE_PROTECTED("awareProtected");
        
        String mDescription;

        private ForbidSubClassRate(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    private static class MemSortGroup {
        public List<AwareProcessBlockInfo> mProcAllowStopList = null;
        public List<AwareProcessBlockInfo> mProcForbidStopList = null;
        public List<AwareProcessBlockInfo> mProcShortageStopList = null;

        public MemSortGroup(List<AwareProcessBlockInfo> procForbidStopList, List<AwareProcessBlockInfo> procShortageStopList, List<AwareProcessBlockInfo> procAllowStopList) {
            this.mProcForbidStopList = procForbidStopList;
            this.mProcShortageStopList = procShortageStopList;
            this.mProcAllowStopList = procAllowStopList;
        }
    }

    private static class ProcessHabitCompare implements Comparator<AwareProcessInfo>, Serializable {
        private static final long serialVersionUID = 1;

        private ProcessHabitCompare() {
        }

        public int compare(AwareProcessInfo arg0, AwareProcessInfo arg1) {
            if (arg0 == null || arg1 == null) {
                return 0;
            }
            return arg0.mImportance - arg1.mImportance;
        }
    }

    static class ShortageProcessInfo {
        public Map<Integer, AwareProcessInfo> mAllProcNeedSort;
        final ArrayMap<Integer, AwareProcessInfo> mAudioIn = new ArrayMap();
        final ArrayMap<Integer, AwareProcessInfo> mAudioOut = new ArrayMap();
        private final ArraySet<Integer> mForeGroundServiceUid = new ArraySet();
        private final ArrayMap<Integer, ArrayList<String>> mForeGroundUid = new ArrayMap();
        Set<Integer> mHabitTopN;
        private int mHomeProcessPid;
        private final Set<Integer> mHomeStrong = new ArraySet();
        private Set<Integer> mKeyPercepServicePid;
        public boolean mKillMore;
        final ArrayMap<Integer, AwareProcessInfo> mNonCurUserProc;
        public AwareAppLruBase mPrevAmsBase;
        public AwareAppLruBase mPrevAwareBase;
        public AwareAppLruBase mRecentTaskAppBase;
        private boolean mRecentTaskShow;
        final Set<String> mVisibleWinPkg;
        final Set<String> mWidgetPkg;

        public boolean isRecentTaskShow() {
            return this.mRecentTaskShow;
        }

        public boolean isFgServicesUid(int uid) {
            return this.mForeGroundServiceUid.contains(Integer.valueOf(uid));
        }

        public void recordFgServicesUid(int uid) {
            this.mForeGroundServiceUid.add(Integer.valueOf(uid));
        }

        public boolean isForegroundUid(ProcessInfo procInfo) {
            if (procInfo == null || !this.mForeGroundUid.containsKey(Integer.valueOf(procInfo.mUid))) {
                return false;
            }
            if (!AwareAppAssociate.isDealAsPkgUid(procInfo.mUid)) {
                return true;
            }
            return AwareAppMngSort.isPkgIncludeForTgt(procInfo.mPackageName, (ArrayList) this.mForeGroundUid.get(Integer.valueOf(procInfo.mUid)));
        }

        public boolean isKeyPercepService(int pid) {
            if (this.mKeyPercepServicePid == null) {
                return false;
            }
            return this.mKeyPercepServicePid.contains(Integer.valueOf(pid));
        }

        public void recordForegroundUid(int uid, ArrayList<String> packageList) {
            if (AwareAppAssociate.isDealAsPkgUid(uid)) {
                this.mForeGroundUid.put(Integer.valueOf(uid), packageList);
            } else {
                this.mForeGroundUid.put(Integer.valueOf(uid), null);
            }
        }

        private boolean isAudioSubClass(ProcessInfo procInfo, Map<Integer, AwareProcessInfo> audioInfo) {
            if (procInfo == null) {
                return false;
            }
            for (Entry<Integer, AwareProcessInfo> m : audioInfo.entrySet()) {
                AwareProcessInfo info = (AwareProcessInfo) m.getValue();
                if (procInfo.mPid == info.mProcInfo.mPid) {
                    return true;
                }
                if (procInfo.mUid == info.mProcInfo.mUid) {
                    if (!AwareAppAssociate.isDealAsPkgUid(procInfo.mUid) || AwareAppMngSort.isPkgIncludeForTgt(procInfo.mPackageName, info.mProcInfo.mPackageName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void updateBaseInfo(Map<Integer, AwareProcessInfo> allProcNeedSort, int homePid, boolean recentTaskShow, Set<Integer> keyPercepServicePid) {
            this.mAllProcNeedSort = allProcNeedSort;
            this.mHabitTopN = getHabitAppTopNVer2(allProcNeedSort, AppMngConfig.getTopN());
            updateVisibleWin();
            updateWidget();
            this.mHomeProcessPid = homePid;
            this.mRecentTaskShow = recentTaskShow;
            this.mKeyPercepServicePid = keyPercepServicePid;
            loadHomeAssoc(this.mHomeProcessPid, allProcNeedSort);
            this.mPrevAmsBase = AwareAppAssociate.getInstance().getPreviousByAmsInfo();
            this.mPrevAwareBase = AwareAppAssociate.getInstance().getPreviousAppInfo();
            this.mRecentTaskAppBase = AwareAppAssociate.getInstance().getRecentTaskPrevInfo();
        }

        public ShortageProcessInfo(int memLevel) {
            boolean z = false;
            this.mRecentTaskShow = false;
            this.mAllProcNeedSort = null;
            this.mPrevAmsBase = null;
            this.mPrevAwareBase = null;
            this.mRecentTaskAppBase = null;
            this.mNonCurUserProc = new ArrayMap();
            this.mVisibleWinPkg = new ArraySet();
            this.mWidgetPkg = new ArraySet();
            this.mKillMore = false;
            if (AppMngConfig.getKillMoreFlag() && memLevel == 1) {
                z = true;
            }
            this.mKillMore = z;
        }

        private boolean isHomeAssocStrong(AwareProcessInfo awareProcInfo) {
            if (awareProcInfo == null || awareProcInfo.mProcInfo == null) {
                return false;
            }
            if (awareProcInfo.mProcInfo.mCurAdj == 600 && (awareProcInfo.mProcInfo.mType == 2 || awareProcInfo.mProcInfo.mType == 3)) {
                return true;
            }
            return this.mHomeStrong.contains(Integer.valueOf(awareProcInfo.mPid));
        }

        private void loadHomeAssoc(int homePid, Map<Integer, AwareProcessInfo> allProc) {
            Set<Integer> homeStrong = new ArraySet();
            AwareAppAssociate.getInstance().getAssocListForPid(homePid, homeStrong);
            for (Integer pid : homeStrong) {
                AwareProcessInfo awareProcInfo = (AwareProcessInfo) allProc.get(pid);
                if (awareProcInfo != null) {
                    if (awareProcInfo.mProcInfo != null) {
                        if (!awareProcInfo.mHasShownUi) {
                            if (awareProcInfo.mProcInfo.mType == 2) {
                                this.mHomeStrong.add(pid);
                            }
                        }
                    }
                }
            }
        }

        private boolean isHabitTopN(int pid) {
            return this.mHabitTopN != null && this.mHabitTopN.contains(Integer.valueOf(pid));
        }

        public boolean isHomeProcess(int pid) {
            return this.mHomeProcessPid == pid;
        }

        public int getKeyBackgroupTypeInternal(ProcessInfo procInfo) {
            if (procInfo == null) {
                return -1;
            }
            return AwareAppKeyBackgroup.getInstance().getKeyBackgroupTypeInternal(procInfo.mPid, procInfo.mUid, procInfo.mPackageName);
        }

        public int getKeyBackgroupTypeInternalByPid(ProcessInfo procInfo) {
            if (procInfo == null) {
                return -1;
            }
            return AwareAppKeyBackgroup.getInstance().getKeyBackgroupTypeInternal(procInfo.mPid, procInfo.mUid, null);
        }

        private Set<Integer> getHabitAppTopNVer2(Map<Integer, AwareProcessInfo> proc, int topN) {
            Set<Integer> procTopN = new ArraySet();
            if (proc == null) {
                AwareLog.e(AwareAppMngSort.TAG, "proc = null!");
                return procTopN;
            }
            AwareUserHabit habit = AwareUserHabit.getInstance();
            if (habit == null) {
                AwareLog.e(AwareAppMngSort.TAG, "AwareUserHabit is null");
                return procTopN;
            }
            List<String> pkgTopN = habit.getTopN(topN);
            if (pkgTopN == null) {
                AwareLog.e(AwareAppMngSort.TAG, "pkgTopN = null!");
                return procTopN;
            }
            for (Entry<Integer, AwareProcessInfo> pm : proc.entrySet()) {
                AwareProcessInfo info = (AwareProcessInfo) pm.getValue();
                if (info != null) {
                    if (info.mImportance != 10000) {
                        if (info.mProcInfo != null) {
                            if (AwareAppMngSort.isPkgIncludeForTgt(pkgTopN, info.mProcInfo.mPackageName)) {
                                procTopN.add(Integer.valueOf(info.mPid));
                            }
                        }
                    }
                }
            }
            return procTopN;
        }

        private void updateVisibleWin() {
            if (this.mAllProcNeedSort != null) {
                Set<Integer> visibleWindows = new ArraySet();
                AwareAppAssociate.getInstance().getVisibleWindows(visibleWindows, null);
                for (Integer pid : visibleWindows) {
                    AwareProcessInfo procInfo = (AwareProcessInfo) this.mAllProcNeedSort.get(pid);
                    if (!(procInfo == null || procInfo.mProcInfo.mPackageName == null)) {
                        this.mVisibleWinPkg.addAll(procInfo.mProcInfo.mPackageName);
                    }
                }
            }
        }

        private void updateWidget() {
            if (this.mAllProcNeedSort != null) {
                Set<String> widgets = AwareAppAssociate.getInstance().getWidgetsPkg();
                if (widgets != null) {
                    this.mWidgetPkg.addAll(widgets);
                }
            }
        }
    }

    public enum ShortageSubClassRate {
        NONE("none"),
        PREV_ONECLEAN("prevOneclean"),
        MUSIC_PLAY("musicPlay"),
        SOUND_RECORD("soundRecord"),
        FGSERVICES_TOPN("fgservice_topN"),
        SERVICE_ADJ_TOPN("service_adj_topN"),
        HW_SYSTEM("hw_system"),
        KEY_IM("key_im"),
        GUIDE("guide"),
        DOWN_UP_LOAD("downupLoad"),
        HEALTH("health"),
        PREVIOUS("previous"),
        AWARE_PROTECTED("awareProtected"),
        KEY_SYS_SERVICE("keySysService"),
        ASSOC_WITH_FG("assocWithFg"),
        VISIBLEWIN("visibleWin"),
        FREQN("user_freqN"),
        TOPN("user_topN"),
        WIDGET("widget"),
        UNKNOWN("unknown");
        
        String mDescription;
        int subClass;

        private ShortageSubClassRate(String description) {
            this.mDescription = description;
            this.subClass = -1;
        }

        public String description() {
            return this.mDescription;
        }

        public int getSubClassRate() {
            return this.subClass;
        }
    }

    private AwareAppMngSort(Context context) {
        this.mContext = context;
        this.mHandler = new BetaLogHandler(BackgroundThread.get().getLooper());
        init();
    }

    public static synchronized AwareAppMngSort getInstance(Context context) {
        AwareAppMngSort awareAppMngSort;
        synchronized (AwareAppMngSort.class) {
            if (sInstance == null) {
                sInstance = new AwareAppMngSort(context);
            }
            awareAppMngSort = sInstance;
        }
        return awareAppMngSort;
    }

    public static synchronized AwareAppMngSort getInstance() {
        AwareAppMngSort awareAppMngSort;
        synchronized (AwareAppMngSort.class) {
            awareAppMngSort = sInstance;
        }
        return awareAppMngSort;
    }

    private void init() {
        this.mHwAMS = HwActivityManagerService.self();
    }

    public static void enable() {
        mEnabled = true;
    }

    public static void disable() {
        mEnabled = false;
    }

    private boolean containsVisibleWindow(Set<String> visibleWindowList, List<String> pkgList) {
        if (visibleWindowList == null || pkgList == null || visibleWindowList.isEmpty()) {
            return false;
        }
        for (String pkg : pkgList) {
            if (visibleWindowList.contains(pkg)) {
                return true;
            }
        }
        return false;
    }

    private void loadAppAssoc(List<AwareProcessInfo> procs, Map<Integer, AwareProcessInfo> pidsClass, Map<Integer, AwareProcessInfo> strongAssocProc) {
        if (procs != null && !procs.isEmpty() && pidsClass != null && strongAssocProc != null) {
            Set<Integer> strong = new ArraySet();
            for (AwareProcessInfo procInfoBase : procs) {
                int pid = procInfoBase.mPid;
                strong.clear();
                loadAssocListForPid(pid, pidsClass, strong, strongAssocProc);
            }
        }
    }

    private boolean isAssocRelation(AwareProcessInfo client, AwareProcessInfo app) {
        if (app == null || client == null || client.mProcInfo == null) {
            return false;
        }
        if (!app.mHasShownUi || app.mPid == getCurHomeProcessPid() || client.mProcInfo.mCurAdj <= 200) {
            return true;
        }
        return false;
    }

    private void loadAssocListForPid(int pid, Map<Integer, AwareProcessInfo> pidsClass, Set<Integer> strong, Map<Integer, AwareProcessInfo> strongAssocProc) {
        if (pidsClass != null && strongAssocProc != null && strong != null) {
            AwareAppAssociate.getInstance().getAssocListForPid(pid, strong);
            for (Integer sPid : strong) {
                AwareProcessInfo procInfo = (AwareProcessInfo) pidsClass.get(sPid);
                if (procInfo != null) {
                    if (isAssocRelation((AwareProcessInfo) pidsClass.get(Integer.valueOf(pid)), procInfo)) {
                        strongAssocProc.put(sPid, procInfo);
                    }
                }
            }
        }
    }

    private ArrayMap<Integer, AwareProcessInfo> getNeedSortedProcesses(Map<Integer, AwareProcessInfo> foreGrdProc, Set<Integer> keyPercepServicePid, CachedWhiteList cachedWhitelist, ShortageProcessInfo shortageProc) {
        AwareAppMngSort awareAppMngSort = this;
        Set<Integer> set = keyPercepServicePid;
        ShortageProcessInfo shortageProcessInfo = shortageProc;
        ArrayList<ProcessInfo> procs = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procs.isEmpty()) {
            return null;
        }
        Map<Integer, AwareProcessBaseInfo> baseInfos = awareAppMngSort.mHwAMS != null ? awareAppMngSort.mHwAMS.getAllProcessBaseInfo() : null;
        Map<Integer, AwareProcessInfo> map;
        CachedWhiteList cachedWhiteList;
        ArrayList<ProcessInfo> arrayList;
        Map<Integer, AwareProcessBaseInfo> map2;
        if (baseInfos == null) {
            map = foreGrdProc;
            cachedWhiteList = cachedWhitelist;
            arrayList = procs;
            map2 = baseInfos;
        } else if (baseInfos.isEmpty()) {
            map = foreGrdProc;
            cachedWhiteList = cachedWhitelist;
        } else {
            int curUserUid = AwareAppAssociate.getInstance().getCurUserId();
            Set<Integer> fgServiceUid = new ArraySet();
            ArraySet<Integer> importUid = new ArraySet();
            ArrayMap<Integer, Integer> percepServicePid = new ArrayMap();
            ArrayMap<Integer, AwareProcessInfo> allProcNeedSort = new ArrayMap();
            int i = 0;
            int size = procs.size();
            while (i < size) {
                ProcessInfo procInfo = (ProcessInfo) procs.get(i);
                if (procInfo != null) {
                    AwareProcessBaseInfo updateInfo = (AwareProcessBaseInfo) baseInfos.get(Integer.valueOf(procInfo.mPid));
                    if (updateInfo != null) {
                        procInfo.mCurAdj = updateInfo.mCurAdj;
                        procInfo.mForegroundActivities = updateInfo.mForegroundActivities;
                        procInfo.mAdjType = updateInfo.mAdjType;
                        arrayList = procs;
                        AwareProcessBaseInfo updateInfo2 = updateInfo;
                        AwareProcessInfo awareProcInfo = new AwareProcessInfo(procInfo.mPid, 0, 0, ClassRate.NORMAL.ordinal(), procInfo);
                        awareProcInfo.mHasShownUi = updateInfo2.mHasShownUi;
                        cachedWhitelist.updateProcessInfoByConfig(awareProcInfo);
                        if (curUserUid != 0 || awareAppMngSort.isCurUserProc(procInfo.mUid, curUserUid)) {
                            allProcNeedSort.put(Integer.valueOf(procInfo.mPid), awareProcInfo);
                            if (procInfo.mForegroundActivities) {
                                foreGrdProc.put(Integer.valueOf(procInfo.mPid), awareProcInfo);
                                shortageProcessInfo.recordForegroundUid(procInfo.mUid, procInfo.mPackageName);
                            } else {
                                map = foreGrdProc;
                                AwareProcessBaseInfo awareProcessBaseInfo = updateInfo2;
                            }
                            if (procInfo.mCurAdj >= 200) {
                                boolean z = false;
                                map2 = baseInfos;
                                boolean audioOut = shortageProcessInfo.getKeyBackgroupTypeInternalByPid(procInfo) == 2;
                                if (audioOut) {
                                    shortageProcessInfo.mAudioOut.put(Integer.valueOf(procInfo.mPid), awareProcInfo);
                                } else {
                                    if (shortageProcessInfo.getKeyBackgroupTypeInternalByPid(procInfo) == 1) {
                                        z = true;
                                    }
                                    if (z) {
                                        shortageProcessInfo.mAudioIn.put(Integer.valueOf(procInfo.mPid), awareProcInfo);
                                    }
                                }
                            } else {
                                map2 = baseInfos;
                            }
                            if (procInfo.mCurAdj < 200) {
                                importUid.add(Integer.valueOf(procInfo.mUid));
                            } else if (procInfo.mCurAdj == 200 && FG_SERVICE.equals(procInfo.mAdjType)) {
                                fgServiceUid.add(Integer.valueOf(procInfo.mUid));
                            } else if (procInfo.mCurAdj == 200 && ADJTYPE_SERVICE.equals(procInfo.mAdjType)) {
                                percepServicePid.put(Integer.valueOf(procInfo.mPid), Integer.valueOf(procInfo.mUid));
                            } else if (procInfo.mCurAdj == 200) {
                                importUid.add(Integer.valueOf(procInfo.mUid));
                            }
                            i++;
                            procs = arrayList;
                            baseInfos = map2;
                            awareAppMngSort = this;
                        } else {
                            shortageProcessInfo.mNonCurUserProc.put(Integer.valueOf(procInfo.mPid), awareProcInfo);
                            map = foreGrdProc;
                            map2 = baseInfos;
                            i++;
                            procs = arrayList;
                            baseInfos = map2;
                            awareAppMngSort = this;
                        }
                    }
                }
                map = foreGrdProc;
                cachedWhiteList = cachedWhitelist;
                arrayList = procs;
                map2 = baseInfos;
                i++;
                procs = arrayList;
                baseInfos = map2;
                awareAppMngSort = this;
            }
            map = foreGrdProc;
            cachedWhiteList = cachedWhitelist;
            arrayList = procs;
            map2 = baseInfos;
            int myPid = Process.myPid();
            for (Entry<Integer, Integer> m : percepServicePid.entrySet()) {
                int i2;
                int i3;
                procs = ((Integer) m.getKey()).intValue();
                int uid = ((Integer) m.getValue()).intValue();
                if (importUid.contains(Integer.valueOf(uid))) {
                    set.add(Integer.valueOf(procs));
                } else if (fgServiceUid.contains(Integer.valueOf(uid))) {
                    Set<Integer> strong = new ArraySet();
                    AwareAppAssociate.getInstance().getAssocClientListForPid(procs, strong);
                    for (Integer clientPid : strong) {
                        i2 = uid;
                        if (clientPid.intValue() == myPid) {
                            set.add(Integer.valueOf(procs));
                            i3 = myPid;
                            break;
                        }
                        AwareProcessInfo awareProcInfo2 = (AwareProcessInfo) allProcNeedSort.get(clientPid);
                        if (awareProcInfo2 != null) {
                            i3 = myPid;
                            if (awareProcInfo2.mProcInfo != 0) {
                                if (awareProcInfo2.mProcInfo.mCurAdj <= 200) {
                                    set.add(Integer.valueOf(procs));
                                    break;
                                }
                            }
                            uid = i2;
                            myPid = i3;
                            shortageProcessInfo = shortageProc;
                        } else {
                            uid = i2;
                            shortageProcessInfo = shortageProc;
                        }
                    }
                    i3 = myPid;
                    i2 = uid;
                    baseInfos = i2;
                    myPid = i3;
                    shortageProcessInfo = shortageProc;
                } else {
                    set.add(Integer.valueOf(procs));
                }
                i3 = myPid;
                i2 = uid;
                baseInfos = i2;
                myPid = i3;
                shortageProcessInfo = shortageProc;
            }
            return allProcNeedSort;
        }
        return null;
    }

    private boolean isCurUserProc(int checkUid, int curUserUid) {
        int userId = UserHandle.getUserId(checkUid);
        boolean isCloned = false;
        if (this.mContext != null) {
            UserManager usm = UserManager.get(this.mContext);
            if (usm != null) {
                UserInfo info = usm.getUserInfo(userId);
                isCloned = info != null ? info.isClonedProfile() : false;
            }
        }
        if (userId == curUserUid || isCloned) {
            return true;
        }
        return false;
    }

    private void groupNonCurUserProc(ArrayMap<Integer, AwareProcessBlockInfo> classNormal, ArrayMap<Integer, AwareProcessInfo> nonCurUserProc) {
        if (classNormal != null && nonCurUserProc != null) {
            for (Entry<Integer, AwareProcessInfo> m : nonCurUserProc.entrySet()) {
                AwareProcessInfo awareProcInfo = (AwareProcessInfo) m.getValue();
                if (awareProcInfo != null) {
                    awareProcInfo.mClassRate = ClassRate.NORMAL.ordinal();
                    awareProcInfo.mSubClassRate = AllowStopSubClassRate.NONCURUSER.ordinal();
                    addProcessInfoToBlock(classNormal, awareProcInfo, awareProcInfo.mPid);
                }
            }
        }
    }

    private boolean isSystemProcess(ProcessInfo procInfo) {
        boolean z = false;
        if (procInfo == null) {
            return false;
        }
        if (procInfo.mType == 2) {
            z = true;
        }
        return z;
    }

    private boolean groupIntoForbidstop(ShortageProcessInfo shortageProc, AwareProcessInfo awareProcInfo) {
        if (awareProcInfo == null) {
            return false;
        }
        int curAdj = awareProcInfo.mProcInfo.mCurAdj;
        int classType = ClassRate.UNKNOWN.ordinal();
        int subClassType = ForbidSubClassRate.NONE.ordinal();
        boolean isGroup = true;
        if (curAdj < 0) {
            classType = ClassRate.PERSIST.ordinal();
        } else if (curAdj < 200) {
            classType = ClassRate.FOREGROUND.ordinal();
        } else if (awareProcInfo.mXmlConfig == null || !isCfgDefaultGroup(awareProcInfo, 0)) {
            isGroup = false;
        } else {
            classType = ClassRate.FOREGROUND.ordinal();
            subClassType = ForbidSubClassRate.AWARE_PROTECTED.ordinal();
        }
        if (isGroup) {
            awareProcInfo.mClassRate = classType;
            awareProcInfo.mSubClassRate = subClassType;
        }
        return isGroup;
    }

    private static boolean isPkgIncludeForTgt(List<String> tgtPkg, List<String> dstPkg) {
        if (tgtPkg == null || tgtPkg.isEmpty() || dstPkg == null) {
            return false;
        }
        for (String pkg : dstPkg) {
            if (pkg != null) {
                if (tgtPkg.contains(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:21:0x0045, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isLastRecentlyUsedBase(ProcessInfo procInfo, Map<Integer, AwareProcessInfo> allProcNeedSort, AwareAppLruBase appLruBase, long decayTime) {
        if (procInfo == null || allProcNeedSort == null || appLruBase == null || procInfo.mUid != appLruBase.mUid) {
            return false;
        }
        if (decayTime != -1 && SystemClock.elapsedRealtime() - appLruBase.mTime > decayTime) {
            return false;
        }
        if (!AwareAppAssociate.isDealAsPkgUid(procInfo.mUid)) {
            return true;
        }
        AwareProcessInfo prevProcInfo = (AwareProcessInfo) allProcNeedSort.get(Integer.valueOf(appLruBase.mPid));
        if (prevProcInfo == null) {
            return false;
        }
        return isPkgIncludeForTgt(procInfo.mPackageName, prevProcInfo.mProcInfo.mPackageName);
    }

    private boolean isPerceptable(ProcessInfo procInfo) {
        if (procInfo == null || procInfo.mCurAdj != 200 || FG_SERVICE.equals(procInfo.mAdjType) || ADJTYPE_SERVICE.equals(procInfo.mAdjType)) {
            return false;
        }
        return true;
    }

    private boolean isFgServices(ProcessInfo procInfo, ShortageProcessInfo shortageProc) {
        if (procInfo == null || procInfo.mCurAdj != 200) {
            return false;
        }
        if (FG_SERVICE.equals(procInfo.mAdjType) && (isSystemProcess(procInfo) || shortageProc.isForegroundUid(procInfo))) {
            return true;
        }
        if (ADJTYPE_SERVICE.equals(procInfo.mAdjType) && (isSystemProcess(procInfo) || shortageProc.isForegroundUid(procInfo))) {
            return true;
        }
        return false;
    }

    public boolean isFgServicesImportantByAdjtype(String adjType) {
        if (FG_SERVICE.equals(adjType) || ADJTYPE_SERVICE.equals(adjType)) {
            return true;
        }
        return false;
    }

    private boolean isFgServicesImportant(ProcessInfo procInfo) {
        if (procInfo != null && procInfo.mCurAdj == 200) {
            return isFgServicesImportantByAdjtype(procInfo.mAdjType);
        }
        return false;
    }

    private boolean groupIntoShortageStop(AwareProcessInfo awareProcInfo, ShortageProcessInfo shortageProc, CachedWhiteList cachedWhitelist) {
        if (awareProcInfo == null) {
            return false;
        }
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        int curAdj = procInfo.mCurAdj;
        int classType = ClassRate.UNKNOWN.ordinal();
        int subClass = ShortageSubClassRate.HW_SYSTEM.ordinal();
        boolean isGroup = true;
        if (isPerceptable(procInfo) || shortageProc.isKeyPercepService(procInfo.mPid)) {
            classType = ClassRate.KEYBACKGROUND.ordinal();
            subClass = ShortageSubClassRate.NONE.ordinal();
        } else if (curAdj == 300) {
            classType = ClassRate.KEYBACKGROUND.ordinal();
            subClass = ShortageSubClassRate.NONE.ordinal();
        } else if (curAdj == 400) {
            classType = ClassRate.KEYBACKGROUND.ordinal();
            subClass = ShortageSubClassRate.NONE.ordinal();
        } else if (shortageProc.isHomeProcess(procInfo.mPid) || shortageProc.isHomeAssocStrong(awareProcInfo)) {
            classType = ClassRate.HOME.ordinal();
            subClass = ShortageSubClassRate.NONE.ordinal();
        } else if (shortageProc.isRecentTaskShow() && isRecentTaskShowApp(procInfo, shortageProc)) {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.PREV_ONECLEAN.ordinal();
        } else if (shortageProc.isAudioSubClass(procInfo, shortageProc.mAudioOut)) {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.MUSIC_PLAY.ordinal();
        } else if (shortageProc.isAudioSubClass(procInfo, shortageProc.mAudioIn)) {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.SOUND_RECORD.ordinal();
        } else if (isFgServices(procInfo, shortageProc)) {
            classType = ClassRate.KEYSERVICES.ordinal();
            shortageProc.recordFgServicesUid(procInfo.mUid);
            subClass = ShortageSubClassRate.FGSERVICES_TOPN.ordinal();
        } else if (cachedWhitelist.isInKeyHabitList(procInfo.mPackageName)) {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.KEY_IM.ordinal();
        } else if (shortageProc.getKeyBackgroupTypeInternal(procInfo) == 3) {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.GUIDE.ordinal();
        } else if (shortageProc.getKeyBackgroupTypeInternal(procInfo) == 5) {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.DOWN_UP_LOAD.ordinal();
        } else if (cachedWhitelist.isLowEnd() || shortageProc.getKeyBackgroupTypeInternal(procInfo) != 4) {
            if (isLastRecentlyUsed(procInfo, shortageProc, shortageProc.mKillMore ? PREVIOUS_APP_DIRCACTIVITY_DECAYTIME : -1)) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.PREVIOUS.ordinal();
            } else if (awareProcInfo.mXmlConfig != null && isCfgDefaultGroup(awareProcInfo, 1) && (!awareProcInfo.mXmlConfig.mFrequentlyUsed || shortageProc.isHabitTopN(procInfo.mPid))) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.AWARE_PROTECTED.ordinal();
            } else if (!cachedWhitelist.isLowEnd() && isKeySysProc(awareProcInfo)) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.KEY_SYS_SERVICE.ordinal();
            } else if (shortageProc.isForegroundUid(awareProcInfo.mProcInfo)) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.ASSOC_WITH_FG.ordinal();
            } else if (containsVisibleWindow(shortageProc.mVisibleWinPkg, procInfo.mPackageName)) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.VISIBLEWIN.ordinal();
            } else if (!shortageProc.mKillMore && shortageProc.isHabitTopN(procInfo.mPid)) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.TOPN.ordinal();
            } else if (isWidget(shortageProc.mWidgetPkg, procInfo.mPackageName)) {
                classType = ClassRate.KEYSERVICES.ordinal();
                subClass = ShortageSubClassRate.WIDGET.ordinal();
            } else {
                isGroup = false;
            }
        } else {
            classType = ClassRate.KEYSERVICES.ordinal();
            subClass = ShortageSubClassRate.HEALTH.ordinal();
        }
        if (isGroup) {
            awareProcInfo.mClassRate = classType;
            awareProcInfo.mSubClassRate = subClass;
        }
        return isGroup;
    }

    private boolean isLastRecentlyUsed(ProcessInfo procInfo, ShortageProcessInfo shortageProc, long decayTime) {
        boolean z = true;
        if (decayTime == -1 && procInfo.mCurAdj == 700) {
            return true;
        }
        if (!isLastRecentlyUsedBase(procInfo, shortageProc.mAllProcNeedSort, shortageProc.mPrevAmsBase, decayTime)) {
            if (!isLastRecentlyUsedBase(procInfo, shortageProc.mAllProcNeedSort, shortageProc.mPrevAwareBase, decayTime)) {
                z = false;
            }
        }
        return z;
    }

    private boolean isWidget(Set<String> widgets, List<String> pkgList) {
        if (widgets == null || pkgList == null || widgets.isEmpty()) {
            return false;
        }
        for (String pkg : pkgList) {
            if (widgets.contains(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClock(Set<String> clocks, ArrayList<String> packageNames) {
        if (!(clocks == null || clocks.isEmpty() || packageNames == null || packageNames.isEmpty())) {
            int size = packageNames.size();
            for (int i = 0; i < size; i++) {
                String pkg = (String) packageNames.get(i);
                if (pkg != null && clocks.contains(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRecentTaskShow(ArrayMap<Integer, AwareProcessInfo> foreGrdProc) {
        if (foreGrdProc == null || foreGrdProc.size() > 0) {
            return false;
        }
        return true;
    }

    private boolean isRecentTaskShowApp(ProcessInfo procInfo, ShortageProcessInfo shortageProc) {
        return isLastRecentlyUsedBase(procInfo, shortageProc.mAllProcNeedSort, shortageProc.mRecentTaskAppBase, -1);
    }

    private boolean isKeySysProc(AwareProcessInfo awareProcInfo) {
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (!isSystemProcess(procInfo)) {
            return false;
        }
        if (procInfo.mCurAdj == 500) {
            return true;
        }
        if (procInfo.mUid < 10000 && procInfo.mCurAdj == 800) {
            return true;
        }
        if (procInfo.mUid < 10000 && !awareProcInfo.mHasShownUi && procInfo.mCreatedTime != -1 && SystemClock.elapsedRealtime() - procInfo.mCreatedTime < AppMngConfig.getKeySysDecay()) {
            return true;
        }
        if (procInfo.mUid < 10000 || awareProcInfo.mHasShownUi || procInfo.mCreatedTime == -1 || SystemClock.elapsedRealtime() - procInfo.mCreatedTime >= AppMngConfig.getSysDecay()) {
            return false;
        }
        return true;
    }

    private boolean groupIntoAllowStop(AwareProcessInfo awareProcInfo, ShortageProcessInfo shortageProc, CachedWhiteList cachedWhitelist) {
        if (awareProcInfo == null) {
            return false;
        }
        int subClassType;
        ProcessInfo procInfo = awareProcInfo.mProcInfo;
        if (shortageProc.mKillMore && isLastRecentlyUsed(procInfo, shortageProc, -1)) {
            subClassType = AllowStopSubClassRate.PREVIOUS.ordinal();
        } else if (shortageProc.mKillMore && shortageProc.isHabitTopN(procInfo.mPid)) {
            subClassType = AllowStopSubClassRate.TOPN.ordinal();
        } else if (cachedWhitelist.isLowEnd() && isKeySysProc(awareProcInfo)) {
            subClassType = AllowStopSubClassRate.KEY_SYS_SERVICE.ordinal();
        } else if (cachedWhitelist.isLowEnd() && shortageProc.getKeyBackgroupTypeInternal(procInfo) == 4) {
            subClassType = AllowStopSubClassRate.HEALTH.ordinal();
        } else if (isFgServicesImportant(procInfo)) {
            subClassType = AllowStopSubClassRate.FG_SERVICES.ordinal();
        } else {
            subClassType = AllowStopSubClassRate.OTHER.ordinal();
        }
        awareProcInfo.mClassRate = ClassRate.NORMAL.ordinal();
        awareProcInfo.mSubClassRate = subClassType;
        return true;
    }

    private void addProcessInfoToBlock(Map<Integer, AwareProcessBlockInfo> appAllClass, AwareProcessInfo pinfo, int key) {
        if (appAllClass != null && pinfo != null) {
            AwareProcessBlockInfo info = new AwareProcessBlockInfo(pinfo.mProcInfo.mUid, false, pinfo.mClassRate);
            info.mProcessList.add(pinfo);
            info.mSubClassRate = pinfo.mSubClassRate;
            info.mImportance = pinfo.mImportance;
            info.mMinAdj = pinfo.mProcInfo.mCurAdj;
            appAllClass.put(Integer.valueOf(key), info);
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0011, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isBgDecayApp(String pkgName, CachedWhiteList cachedWhitelist) {
        if (pkgName == null || cachedWhitelist == null || cachedWhitelist.mBgNonDecayPkg.contains(pkgName)) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:60:0x00bb, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getRestartFlagByProc(int classRate, int subRate, List<String> pkg, CachedWhiteList cachedWhitelist, boolean isRestartByAppType, ProcessInfo procInfo, int appType, AwareProcessBlockInfo value) {
        if (pkg == null || cachedWhitelist == null || subRate == AllowStopSubClassRate.NONE.ordinal() || subRate == AllowStopSubClassRate.PREVIOUS.ordinal() || subRate == AllowStopSubClassRate.TOPN.ordinal() || subRate == AllowStopSubClassRate.HEALTH.ordinal() || subRate == AllowStopSubClassRate.NONCURUSER.ordinal() || subRate == AllowStopSubClassRate.UNKNOWN.ordinal()) {
            return true;
        }
        if (cachedWhitelist.isLowEnd() && subRate == AllowStopSubClassRate.KEY_SYS_SERVICE.ordinal()) {
            return false;
        }
        for (String pkgName : pkg) {
            if (cachedWhitelist.mRestartAppList.contains(pkgName)) {
                return true;
            }
            if (cachedWhitelist.mAllProtectApp.contains(pkgName) && (!cachedWhitelist.isLowEnd() || !isBgDecayApp(pkgName, cachedWhitelist))) {
                return true;
            }
            if (!cachedWhitelist.mBadAppList.contains(pkgName)) {
                if (isRestartByAppType) {
                    if (AppMngConfig.getAbroadFlag() && cachedWhitelist.mAllUnProtectApp.contains(pkgName)) {
                        value.mAlarmChk = true;
                    } else if (cachedWhitelist.isLowEnd()) {
                        if (!isBgDecayApp(pkgName, cachedWhitelist)) {
                            return true;
                        }
                        if (!isDecayAppByAppType(appType)) {
                            value.mAlarmChk = true;
                        }
                    } else if (!(isDecayAppByAppType(appType) && isBgDecayApp(pkgName, cachedWhitelist))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean getAllowCleanResByProc(int classRate, int subRate, CachedWhiteList cachedWhitelist, ArrayList<String> pkgList, boolean isRestartByAppType) {
        boolean z = false;
        if (cachedWhitelist == null) {
            return false;
        }
        if (!cachedWhitelist.isLowEnd()) {
            return true;
        }
        if (subRate != AllowStopSubClassRate.FG_SERVICES.ordinal()) {
            if (subRate == AllowStopSubClassRate.OTHER.ordinal()) {
                z = true;
            }
            return z;
        } else if (!isRestartByAppType) {
            return true;
        } else {
            if (!AppMngConfig.getAbroadFlag() || pkgList == null || pkgList.isEmpty()) {
                return false;
            }
            int size = pkgList.size();
            for (int i = 0; i < size; i++) {
                if (!cachedWhitelist.mAllUnProtectApp.contains((String) pkgList.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean getAllowCleanResByAppType(int appType) {
        if (appType != 14) {
            return true;
        }
        return false;
    }

    private void updateAppType(AwareProcessBlockInfo info, CachedWhiteList cachedWhitelist) {
        if (info != null) {
            for (AwareProcessInfo procInfo : info.mProcessList) {
                if (!(procInfo == null || procInfo.mProcInfo == null)) {
                    if (procInfo.mProcInfo.mPackageName != null) {
                        int size = procInfo.mProcInfo.mPackageName.size();
                        for (int i = 0; i < size; i++) {
                            int type = AppTypeRecoManager.getInstance().getAppType((String) procInfo.mProcInfo.mPackageName.get(i));
                            if (!getRestartFlagByAppType(type)) {
                                info.mAppType = type;
                            } else if (isDecayAppByAppType(type)) {
                                info.mAppType = type;
                            } else {
                                info.mAppType = -1;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean getRestartFlagByAppType(int appType) {
        switch (appType) {
            case 3:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 12:
            case 13:
            case 14:
            case 15:
            case 17:
            case 18:
            case 19:
                return false;
            default:
                return true;
        }
    }

    private boolean isDecayAppByAppType(int appType) {
        switch (appType) {
            case 20:
            case 21:
                return true;
            default:
                return false;
        }
    }

    public boolean needCheckAlarm(AwareProcessBlockInfo info) {
        if (info == null) {
            return false;
        }
        if (AppMngConfig.getAlarmCheckFlag()) {
            return true;
        }
        return info.mAlarmChk;
    }

    /* JADX WARNING: Removed duplicated region for block: B:63:0x014d A:{LOOP_END, LOOP:2: B:61:0x0147->B:63:0x014d} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addClassToAllClass(Map<Integer, AwareProcessBlockInfo> appAllClass, Map<Integer, AwareProcessBlockInfo> blocks, ShortageProcessInfo shortageProc, Map<Integer, AwareProcessBlockInfo> allUids, CachedWhiteList cachedWhitelist, Set<String> clocks, boolean chkRestartFlag) {
        Map<Integer, AwareProcessBlockInfo> map;
        ShortageProcessInfo shortageProcessInfo;
        Set<String> procInfo;
        Map<Integer, AwareProcessBlockInfo> map2;
        CachedWhiteList cachedWhiteList = cachedWhitelist;
        Iterator it = blocks.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, AwareProcessBlockInfo> m = (Entry) it.next();
            AwareProcessBlockInfo value = (AwareProcessBlockInfo) m.getValue();
            map = appAllClass;
            map.put(Integer.valueOf(((Integer) m.getKey()).intValue()), value);
            if (chkRestartFlag) {
                Iterator it2;
                boolean isAllowCleanRes = true;
                boolean isRestart = false;
                boolean isRestartByAppType = false;
                if (AppMngConfig.getAbroadFlag() || AppMngConfig.getRestartFlag()) {
                    isRestartByAppType = true;
                } else {
                    updateAppType(value, cachedWhiteList);
                    if (getRestartFlagByAppType(value.mAppType)) {
                        isRestartByAppType = true;
                    } else if (!(cachedWhitelist.isLowEnd() || getAllowCleanResByAppType(value.mAppType))) {
                        isAllowCleanRes = false;
                    }
                }
                boolean isRestartByAppType2 = isRestartByAppType;
                Iterator it3 = value.mProcessList.iterator();
                boolean isWidgetApp = false;
                boolean isClockApp = false;
                boolean isImApp = false;
                boolean isAllowCleanRes2 = isAllowCleanRes;
                while (it3.hasNext()) {
                    AwareProcessInfo procInfo2 = (AwareProcessInfo) it3.next();
                    AwareProcessInfo awareProcessInfo;
                    if (AppMngConfig.getRestartFlag()) {
                        shortageProcessInfo = shortageProc;
                        it2 = it;
                        awareProcessInfo = procInfo2;
                        procInfo = clocks;
                    } else {
                        it2 = it;
                        awareProcessInfo = procInfo2;
                        Iterator it4 = it3;
                        if (getRestartFlagByProc(value.mClassRate, value.mSubClassRate, procInfo2.mProcInfo.mPackageName, cachedWhiteList, isRestartByAppType2, procInfo2.mProcInfo, value.mAppType, value)) {
                            shortageProcessInfo = shortageProc;
                            procInfo = clocks;
                        } else {
                            if (isAllowCleanRes2) {
                                if (isWidgetApp) {
                                    shortageProcessInfo = shortageProc;
                                } else {
                                    isWidgetApp = isWidget(shortageProc.mWidgetPkg, awareProcessInfo.mProcInfo.mPackageName);
                                }
                                if (isClockApp) {
                                    procInfo = clocks;
                                } else {
                                    isClockApp = isClock(clocks, awareProcessInfo.mProcInfo.mPackageName);
                                }
                                if (!isImApp) {
                                    isImApp = cachedWhiteList.isInAllHabitList(awareProcessInfo.mProcInfo.mPackageName);
                                }
                                if (awareProcessInfo.mXmlConfig != null) {
                                    isAllowCleanRes2 = awareProcessInfo.mXmlConfig.mResCleanAllow;
                                }
                                if (isAllowCleanRes2) {
                                    if (!getAllowCleanResByProc(value.mClassRate, value.mSubClassRate, cachedWhiteList, awareProcessInfo.mProcInfo.mPackageName, isRestartByAppType2)) {
                                        isAllowCleanRes2 = false;
                                    }
                                }
                            }
                            it3 = it4;
                            it = it2;
                        }
                    }
                    isRestart = true;
                }
                shortageProcessInfo = shortageProc;
                procInfo = clocks;
                it2 = it;
                if (isRestart || isWidgetApp || isClockApp || isImApp || !isAllowCleanRes2) {
                    map2 = allUids;
                } else {
                    if (inSameUids(allUids, value.mProcessList)) {
                        value.mResCleanAllow = true;
                        value.mCleanAlarm = true;
                        if (isRestart || isImApp) {
                            for (AwareProcessInfo procInfo3 : value.mProcessList) {
                                procInfo3.mRestartFlag = true;
                            }
                        }
                        it = it2;
                    }
                }
                value.mResCleanAllow = false;
                value.mCleanAlarm = false;
                while (r1.hasNext()) {
                }
                it = it2;
            }
        }
        map = appAllClass;
        shortageProcessInfo = shortageProc;
        map2 = allUids;
        procInfo = clocks;
    }

    private void addProcessInfoToGroupBlock(Map<Integer, AwareProcessBlockInfo> appAllClass, AwareProcessInfo pinfo, int key, Map<Integer, Map<Integer, AwareProcessBlockInfo>> groupBlock) {
        Integer groupKey = Integer.valueOf((pinfo.mClassRate << 8) + pinfo.mSubClassRate);
        Map<Integer, AwareProcessBlockInfo> groupUid = (Map) groupBlock.get(groupKey);
        if (groupUid == null) {
            groupUid = new ArrayMap();
            groupBlock.put(groupKey, groupUid);
        }
        AwareProcessBlockInfo block = (AwareProcessBlockInfo) groupUid.get(Integer.valueOf(pinfo.mProcInfo.mUid));
        if (block == null) {
            block = new AwareProcessBlockInfo(pinfo.mProcInfo.mUid, false, pinfo.mClassRate);
            block.mProcessList.add(pinfo);
            block.mSubClassRate = pinfo.mSubClassRate;
            block.mImportance = pinfo.mImportance;
            block.mMinAdj = pinfo.mProcInfo.mCurAdj;
            groupUid.put(Integer.valueOf(pinfo.mProcInfo.mUid), block);
            appAllClass.put(Integer.valueOf(key), block);
            return;
        }
        if (block.mImportance > pinfo.mImportance) {
            block.mImportance = pinfo.mImportance;
        }
        if (block.mMinAdj > pinfo.mProcInfo.mCurAdj) {
            block.mMinAdj = pinfo.mProcInfo.mCurAdj;
        }
        block.mProcessList.add(pinfo);
    }

    private boolean isCfgDefaultGroup(AwareProcessInfo procInfo, int groupId) {
        boolean z = false;
        if (procInfo == null || procInfo.mXmlConfig == null) {
            return false;
        }
        if (procInfo.mXmlConfig.mCfgDefaultGroup == groupId) {
            z = true;
        }
        return z;
    }

    private ArrayMap<Integer, AwareProcessBlockInfo> getAppMemSortClassGroup(int subType) {
        ArraySet<Integer> keyPercepServicePid = new ArraySet();
        ArrayMap<Integer, AwareProcessInfo> foreGrdProc = new ArrayMap();
        ShortageProcessInfo shortageProc = new ShortageProcessInfo(subType);
        CachedWhiteList cachedWhitelist = new CachedWhiteList();
        cachedWhitelist.updateCachedList();
        Set<String> clocks = AppTypeRecoManager.getInstance().getAlarmApps();
        ArrayMap<Integer, AwareProcessInfo> allProcNeedSort = getNeedSortedProcesses(foreGrdProc, keyPercepServicePid, cachedWhitelist, shortageProc);
        if (allProcNeedSort == null) {
            return null;
        }
        ArrayMap<Integer, AwareProcessBlockInfo> appAllClass = new ArrayMap();
        ArrayMap<Integer, AwareProcessBlockInfo> classShort = new ArrayMap();
        ArrayMap<Integer, AwareProcessBlockInfo> classNormal = new ArrayMap();
        Map<Integer, Map<Integer, AwareProcessBlockInfo>> groupBlock = new ArrayMap();
        Map<Integer, AwareProcessBlockInfo> allUids = groupByUid(allProcNeedSort);
        shortageProc.updateBaseInfo(allProcNeedSort, getCurHomeProcessPid(), isRecentTaskShow(foreGrdProc), keyPercepServicePid);
        Iterator it = allProcNeedSort.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, AwareProcessInfo> m = (Entry) it.next();
            AwareProcessInfo awareProcInfo = (AwareProcessInfo) m.getValue();
            boolean isGroup = groupIntoForbidstop(shortageProc, awareProcInfo);
            if (!isGroup) {
                isGroup = groupIntoShortageStop(awareProcInfo, shortageProc, cachedWhitelist);
            }
            if (!isGroup) {
                groupIntoAllowStop(awareProcInfo, shortageProc, cachedWhitelist);
            }
            Iterator it2 = it;
            if (awareProcInfo.mClassRate < ClassRate.KEYBACKGROUND.ordinal()) {
                addProcessInfoToBlock(appAllClass, awareProcInfo, awareProcInfo.mPid);
            } else if (awareProcInfo.mClassRate < ClassRate.NORMAL.ordinal()) {
                addProcessInfoToGroupBlock(classShort, awareProcInfo, awareProcInfo.mPid, groupBlock);
            } else {
                addProcessInfoToBlock(classNormal, awareProcInfo, awareProcInfo.mPid);
            }
            it = it2;
        }
        groupNonCurUserProc(classNormal, shortageProc.mNonCurUserProc);
        ArrayMap<Integer, AwareProcessBlockInfo> classShort2 = classShort;
        ArrayMap<Integer, AwareProcessBlockInfo> appAllClass2 = appAllClass;
        adjustClassRate(allProcNeedSort, classShort, classNormal, appAllClass, shortageProc, allUids, cachedWhitelist, clocks);
        addClassToAllClass(appAllClass2, classShort2, shortageProc, allUids, cachedWhitelist, clocks, false);
        return appAllClass2;
    }

    private Map<AppBlockKeyBase, AwareProcessBlockInfo> convertToUidBlock(Map<Integer, AwareProcessBlockInfo> pidsBlock, ArrayMap<Integer, AppBlockKeyBase> pidsAppBlock, ArrayMap<Integer, AppBlockKeyBase> uidAppBlock) {
        ArrayMap<Integer, AppBlockKeyBase> arrayMap = pidsAppBlock;
        ArrayMap<Integer, AppBlockKeyBase> arrayMap2 = uidAppBlock;
        if (pidsBlock == null) {
            return null;
        }
        Map<AppBlockKeyBase, AwareProcessBlockInfo> uids = new ArrayMap();
        for (Entry<Integer, AwareProcessBlockInfo> m : pidsBlock.entrySet()) {
            AwareProcessBlockInfo blockInfo = (AwareProcessBlockInfo) m.getValue();
            if (blockInfo.mProcessList != null) {
                for (AwareProcessInfo awareProcInfo : blockInfo.mProcessList) {
                    AppBlockKeyBase blockKeyValue;
                    if (AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid)) {
                        blockKeyValue = (AppBlockKeyBase) arrayMap.get(Integer.valueOf(awareProcInfo.mProcInfo.mPid));
                    } else {
                        blockKeyValue = (AppBlockKeyBase) arrayMap2.get(Integer.valueOf(awareProcInfo.mProcInfo.mUid));
                    }
                    AwareProcessBlockInfo info = blockKeyValue == null ? null : (AwareProcessBlockInfo) uids.get(blockKeyValue);
                    if (info == null) {
                        info = new AwareProcessBlockInfo(awareProcInfo.mProcInfo.mUid, false, awareProcInfo.mClassRate);
                        info.mProcessList.add(awareProcInfo);
                        info.mClassRate = blockInfo.mClassRate;
                        info.mSubClassRate = blockInfo.mSubClassRate;
                        AppBlockKeyBase keyBase = new AppBlockKeyBase(awareProcInfo.mProcInfo.mPid, awareProcInfo.mProcInfo.mUid);
                        if (AwareAppAssociate.isDealAsPkgUid(awareProcInfo.mProcInfo.mUid)) {
                            arrayMap.put(Integer.valueOf(awareProcInfo.mProcInfo.mPid), keyBase);
                        } else {
                            arrayMap2.put(Integer.valueOf(awareProcInfo.mProcInfo.mUid), keyBase);
                        }
                        uids.put(keyBase, info);
                    } else {
                        if (info.mSubClassRate > awareProcInfo.mSubClassRate) {
                            info.mSubClassRate = awareProcInfo.mSubClassRate;
                        }
                        info.mProcessList.add(awareProcInfo);
                    }
                }
            }
        }
        return uids;
    }

    private Map<Integer, AwareProcessBlockInfo> groupByUid(Map<Integer, AwareProcessInfo> allProcNeedSort) {
        if (allProcNeedSort == null) {
            return null;
        }
        Map<Integer, AwareProcessBlockInfo> uids = new ArrayMap();
        Map<Integer, Integer> userHabitMap = null;
        AwareUserHabit habit = AwareUserHabit.getInstance();
        if (habit != null) {
            userHabitMap = habit.getTopList(allProcNeedSort);
        }
        for (Entry<Integer, AwareProcessInfo> m : allProcNeedSort.entrySet()) {
            AwareProcessInfo info = (AwareProcessInfo) m.getValue();
            if (info != null) {
                Integer importance = null;
                if (userHabitMap != null) {
                    importance = (Integer) userHabitMap.get(Integer.valueOf(info.mPid));
                }
                if (importance != null) {
                    info.mImportance = importance.intValue();
                } else {
                    info.mImportance = 10000;
                }
                AwareProcessBlockInfo block = (AwareProcessBlockInfo) uids.get(Integer.valueOf(info.mProcInfo.mUid));
                if (block == null) {
                    block = new AwareProcessBlockInfo(info.mProcInfo.mUid, false, info.mClassRate);
                    block.mProcessList.add(info);
                    uids.put(Integer.valueOf(info.mProcInfo.mUid), block);
                    block.mImportance = info.mImportance;
                } else {
                    block.mProcessList.add(info);
                    if (block.mImportance > info.mImportance) {
                        block.mImportance = info.mImportance;
                    }
                }
            }
        }
        return uids;
    }

    private void adjustClassByStrongAssoc(AwareProcessBlockInfo blockInfo, Map<Integer, AwareProcessInfo> allProcNeedSort, Map<AppBlockKeyBase, AwareProcessBlockInfo> uids, ArrayMap<Integer, AppBlockKeyBase> pidsAppBlock, ArrayMap<Integer, AppBlockKeyBase> uidAppBlock, Map<AppBlockKeyBase, AwareProcessBlockInfo> assocNormalUids) {
        ArrayMap<Integer, AwareProcessInfo> strong = new ArrayMap();
        loadAppAssoc(blockInfo.mProcessList, allProcNeedSort, strong);
        for (Entry<Integer, AwareProcessInfo> sm : strong.entrySet()) {
            AwareProcessInfo procInfo = (AwareProcessInfo) allProcNeedSort.get(sm.getKey());
            if (procInfo != null) {
                if (procInfo.mProcInfo != null) {
                    AppBlockKeyBase blockKeyValue;
                    if (AwareAppAssociate.isDealAsPkgUid(procInfo.mProcInfo.mUid)) {
                        blockKeyValue = (AppBlockKeyBase) pidsAppBlock.get(Integer.valueOf(procInfo.mProcInfo.mPid));
                    } else {
                        blockKeyValue = (AppBlockKeyBase) uidAppBlock.get(Integer.valueOf(procInfo.mProcInfo.mUid));
                    }
                    AwareProcessBlockInfo blockInfoAssoc = blockKeyValue == null ? null : (AwareProcessBlockInfo) uids.get(blockKeyValue);
                    if (!(blockInfoAssoc == null || blockInfoAssoc.mProcessList == null)) {
                        if (blockInfoAssoc.mProcessList.size() > 0) {
                            if (blockInfoAssoc.mClassRate > blockInfo.mClassRate) {
                                blockInfoAssoc.mClassRate = blockInfo.mClassRate;
                                blockInfoAssoc.mSubClassRate = blockInfo.mSubClassRate;
                                blockInfoAssoc.mSubTypeStr = SUBTYPE_ASSOCIATION;
                                if (assocNormalUids != null) {
                                    assocNormalUids.put(blockKeyValue, blockInfoAssoc);
                                }
                            } else if (blockInfoAssoc.mClassRate == blockInfo.mClassRate && blockInfoAssoc.mSubClassRate > blockInfo.mSubClassRate) {
                                blockInfoAssoc.mSubClassRate = blockInfo.mSubClassRate;
                                blockInfoAssoc.mSubTypeStr = SUBTYPE_ASSOCIATION;
                            }
                        }
                    }
                }
            }
        }
    }

    private void adjustClassRate(Map<Integer, AwareProcessInfo> allProcNeedSort, Map<Integer, AwareProcessBlockInfo> classShort, Map<Integer, AwareProcessBlockInfo> classNormal, Map<Integer, AwareProcessBlockInfo> allClass, ShortageProcessInfo shortageProc, Map<Integer, AwareProcessBlockInfo> allUids, CachedWhiteList cachedWhitelist, Set<String> clocks) {
        Map<Integer, AwareProcessBlockInfo> map = classNormal;
        ShortageProcessInfo shortageProcessInfo;
        if (allProcNeedSort == null || map == null) {
            shortageProcessInfo = shortageProc;
            return;
        }
        ArrayMap<Integer, AppBlockKeyBase> pidsAppBlock = new ArrayMap();
        ArrayMap<Integer, AppBlockKeyBase> uidAppBlock = new ArrayMap();
        Map<AppBlockKeyBase, AwareProcessBlockInfo> uids = convertToUidBlock(map, pidsAppBlock, uidAppBlock);
        if (uids != null) {
            AwareProcessBlockInfo blockInfoAssoc;
            Map<Integer, AwareProcessBlockInfo> assocNormalClass = new ArrayMap();
            for (Entry<Integer, AwareProcessBlockInfo> m : classShort.entrySet()) {
                AwareProcessBlockInfo blockInfo = (AwareProcessBlockInfo) m.getValue();
                if (blockInfo.mClassRate == ClassRate.KEYSERVICES.ordinal()) {
                    Map<AppBlockKeyBase, AwareProcessBlockInfo> assocNormalUids = new ArrayMap();
                    adjustClassByStrongAssoc(blockInfo, allProcNeedSort, uids, pidsAppBlock, uidAppBlock, assocNormalUids);
                    for (Entry<AppBlockKeyBase, AwareProcessBlockInfo> assocBlock : assocNormalUids.entrySet()) {
                        AppBlockKeyBase blockKeyValue = (AppBlockKeyBase) assocBlock.getKey();
                        blockInfoAssoc = (AwareProcessBlockInfo) assocBlock.getValue();
                        if (AwareAppAssociate.isDealAsPkgUid(blockKeyValue.mUid)) {
                            pidsAppBlock.remove(Integer.valueOf(blockKeyValue.mPid));
                            uids.remove(blockKeyValue);
                        } else {
                            uidAppBlock.remove(Integer.valueOf(blockKeyValue.mUid));
                            uids.remove(blockKeyValue);
                        }
                        assocNormalClass.put(Integer.valueOf(blockKeyValue.mPid), blockInfoAssoc);
                    }
                }
            }
            classShort.putAll(assocNormalClass);
            Map<Integer, AwareProcessBlockInfo> classNormalBlock = new ArrayMap();
            Iterator it = uids.entrySet().iterator();
            while (it.hasNext()) {
                Entry<AppBlockKeyBase, AwareProcessBlockInfo> m2 = (Entry) it.next();
                blockInfoAssoc = (AwareProcessBlockInfo) m2.getValue();
                AwareProcessBlockInfo blockInfo2 = blockInfoAssoc;
                Iterator it2 = it;
                adjustClassByStrongAssoc(blockInfoAssoc, allProcNeedSort, uids, pidsAppBlock, uidAppBlock, null);
                boolean addToAll = false;
                int subClass = AllowStopSubClassRate.UNKNOWN.ordinal();
                AwareProcessBlockInfo blockInfo3 = blockInfo2;
                for (AwareProcessInfo procInfo : blockInfo3.mProcessList) {
                    if (addToAll) {
                        shortageProcessInfo = shortageProc;
                        if (blockInfo3.mSubClassRate > procInfo.mSubClassRate) {
                            blockInfo3.mSubClassRate = procInfo.mSubClassRate;
                        }
                        if (blockInfo3.mImportance > procInfo.mImportance) {
                            blockInfo3.mImportance = procInfo.mImportance;
                        }
                        if (blockInfo3.mMinAdj > procInfo.mProcInfo.mCurAdj) {
                            blockInfo3.mMinAdj = procInfo.mProcInfo.mCurAdj;
                        }
                    } else {
                        blockInfo3.mImportance = procInfo.mImportance;
                        blockInfo3.mMinAdj = procInfo.mProcInfo.mCurAdj;
                        blockInfo3.mSubClassRate = procInfo.mSubClassRate;
                        classNormalBlock.put(Integer.valueOf(procInfo.mProcInfo.mPid), blockInfo3);
                        if (shortageProc.isFgServicesUid(blockInfo3.mUid)) {
                            subClass = AllowStopSubClassRate.FG_SERVICES.ordinal();
                        }
                        if (blockInfo3.mSubClassRate > subClass) {
                            blockInfo3.mSubClassRate = subClass;
                        }
                        addToAll = true;
                    }
                    map = classNormal;
                }
                shortageProcessInfo = shortageProc;
                it = it2;
                map = classNormal;
            }
            addClassToAllClass(allClass, classNormalBlock, shortageProc, allUids, cachedWhitelist, clocks, true);
        }
    }

    private boolean inSameUids(Map<Integer, AwareProcessBlockInfo> allUids, List<AwareProcessInfo> lists) {
        if (lists == null || lists.isEmpty()) {
            return false;
        }
        AwareProcessBlockInfo info = (AwareProcessBlockInfo) allUids.get(Integer.valueOf(((AwareProcessInfo) lists.get(0)).mProcInfo.mUid));
        if (info == null || info.mProcessList == null) {
            return false;
        }
        return info.mProcessList.equals(lists);
    }

    private MemSortGroup getAppMemSortGroup(int subType) {
        ArrayMap<Integer, AwareProcessBlockInfo> pidsClass = getAppMemSortClassGroup(subType);
        if (pidsClass == null) {
            return null;
        }
        List<AwareProcessBlockInfo> procForbidStopList = new ArrayList();
        List<AwareProcessBlockInfo> procShortageStopList = new ArrayList();
        List<AwareProcessBlockInfo> procAllowStopList = new ArrayList();
        for (Entry<Integer, AwareProcessBlockInfo> m : pidsClass.entrySet()) {
            AwareProcessBlockInfo awareProcInfo = (AwareProcessBlockInfo) m.getValue();
            awareProcInfo.mUpdateTime = SystemClock.elapsedRealtime();
            int groupId = 0;
            if (awareProcInfo.mClassRate <= ClassRate.FOREGROUND.ordinal()) {
                procForbidStopList.add(awareProcInfo);
            } else if (awareProcInfo.mClassRate < ClassRate.NORMAL.ordinal()) {
                procShortageStopList.add(awareProcInfo);
                groupId = 1;
            } else {
                procAllowStopList.add(awareProcInfo);
                groupId = 2;
            }
            awareProcInfo.setMemGroup(groupId);
        }
        Collections.sort(procShortageStopList);
        Collections.sort(procAllowStopList);
        return new MemSortGroup(procForbidStopList, procShortageStopList, procAllowStopList);
    }

    private MemSortGroup getAppMemCompactSortGroup(int subType) {
        List<AwareProcessBlockInfo> procForbidList = new ArrayList();
        List<AwareProcessBlockInfo> procCompactibleList = new ArrayList();
        List<AwareProcessBlockInfo> procFrozenList = new ArrayList();
        ArrayList<AwareProcessInfo> allProcNeedSortList = AppStatusUtils.getInstance().getAllProcNeedSort();
        if (allProcNeedSortList == null) {
            AwareLog.e(TAG, "getAllProcNeedSort failed for memCompact!");
            return null;
        }
        List<AwareProcessBlockInfo> resultInfo = DecisionMaker.getInstance().decideAllWithoutList(allProcNeedSortList, subType, AppMngFeature.APP_CLEAN, AppCleanSource.COMPACT);
        if (resultInfo == null) {
            AwareLog.e(TAG, "decideAll get null for memCompact!");
            return null;
        }
        int i;
        Map<Integer, AwareProcessInfo> allProcNeedSort = new ArrayMap();
        AppStatusUtils appStatus = AppStatusUtils.getInstance();
        Iterator it = resultInfo.iterator();
        while (true) {
            i = 0;
            if (!it.hasNext()) {
                break;
            }
            AwareProcessBlockInfo awareProcBlkInfo = (AwareProcessBlockInfo) it.next();
            if (isBlockInfoValid(awareProcBlkInfo)) {
                AwareProcessInfo item = (AwareProcessInfo) awareProcBlkInfo.mProcessList.get(0);
                if (awareProcBlkInfo.mCleanType == CleanType.NONE) {
                    procForbidList.add(awareProcBlkInfo);
                } else if (appStatus.checkAppStatus(Status.FROZEN, item)) {
                    procFrozenList.add(awareProcBlkInfo);
                } else {
                    procCompactibleList.add(awareProcBlkInfo);
                    allProcNeedSort.put(Integer.valueOf(item.mPid), item);
                }
            }
        }
        Map<Integer, AwareProcessBlockInfo> allUids = groupByUid(allProcNeedSort);
        if (fileredApps == null) {
            updateFileredApps();
        }
        for (AwareProcessBlockInfo awareBlkProc : procCompactibleList) {
            AwareProcessInfo printItem = (AwareProcessInfo) awareBlkProc.mProcessList.get(i);
            if (fileredApps.contains(awareBlkProc.mPackageName)) {
                i = 0;
                awareBlkProc.mImportance = 0;
            } else {
                i = ((AwareProcessBlockInfo) allUids.get(Integer.valueOf(awareBlkProc.mUid))).mImportance;
                if (i >= printItem.mImportance || AwareAppAssociate.isDealAsPkgUid(awareBlkProc.mUid)) {
                    awareBlkProc.mImportance = printItem.mImportance;
                } else {
                    awareBlkProc.mImportance = i;
                }
                i = 0;
            }
        }
        Collections.sort(procCompactibleList, BLOCK_BY_IMPORTANCE);
        return new MemSortGroup(procForbidList, procCompactibleList, procFrozenList);
    }

    private boolean isBlockInfoValid(AwareProcessBlockInfo awareProcBlkInfo) {
        if (awareProcBlkInfo == null || awareProcBlkInfo.mProcessList == null || awareProcBlkInfo.mProcessList.isEmpty() || awareProcBlkInfo.mProcessList.get(0) == null) {
            return false;
        }
        return true;
    }

    private static void updateFileredApps() {
        fileredApps = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_CLEAN.getDesc(), CSP_APPS);
        if (fileredApps == null) {
            AwareLog.e(TAG, "get csp_apps failed!");
            fileredApps = new ArrayList(0);
        }
    }

    public static String getClassRateStr(int classRate) {
        for (ClassRate rate : ClassRate.values()) {
            if (rate.ordinal() == classRate) {
                return rate.description();
            }
        }
        return ClassRate.UNKNOWN.description();
    }

    public boolean isGroupBeHigher(int pid, int uid, String processName, ArrayList<String> arrayList, int groupId) {
        if (!mEnabled || !this.mAssocEnable) {
            return false;
        }
        AwareAppAssociate awareAssoc = AwareAppAssociate.getInstance();
        if (awareAssoc == null) {
            return false;
        }
        Set<Integer> forePid = new ArraySet();
        awareAssoc.getForeGroundApp(forePid);
        if (forePid.contains(Integer.valueOf(pid))) {
            return true;
        }
        AwareProcessBaseInfo info = this.mHwAMS != null ? this.mHwAMS.getProcessBaseInfo(pid) : null;
        if (info == null) {
            return false;
        }
        if (info.mCurAdj < 200) {
            return true;
        }
        if (groupId == 2) {
            if (info.mCurAdj == 300 || info.mCurAdj == 400) {
                return true;
            }
            if (info.mCurAdj != 200 || isFgServicesImportantByAdjtype(info.mAdjType)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean checkAppMngEnable() {
        return mEnabled;
    }

    private boolean needReplaceAllowStopList(int resourceType) {
        return 2 == resourceType;
    }

    public AwareAppMngSortPolicy getAppMngSortPolicy(int resourceType, int subType, int groupId) {
        if (!mEnabled) {
            return null;
        }
        long startTime = 0;
        if (DEBUG) {
            startTime = System.currentTimeMillis();
        }
        ArrayMap<Integer, List<AwareProcessBlockInfo>> appGroup = new ArrayMap();
        if (needReplaceAllowStopList(resourceType)) {
            appGroup = getAppMngSortGroupForMemCleanChina(groupId, subType);
        } else {
            MemSortGroup sortGroup;
            if (1 == resourceType) {
                sortGroup = getAppMemCompactSortGroup(subType);
            } else {
                sortGroup = getAppMemSortGroup(subType);
            }
            if (sortGroup == null) {
                return null;
            }
            if (groupId == 0) {
                appGroup.put(Integer.valueOf(groupId), sortGroup.mProcForbidStopList);
            } else if (groupId == 1) {
                appGroup.put(Integer.valueOf(groupId), sortGroup.mProcShortageStopList);
            } else if (groupId == 2) {
                appGroup.put(Integer.valueOf(2), sortGroup.mProcAllowStopList);
            } else if (groupId == 3) {
                appGroup.put(Integer.valueOf(0), sortGroup.mProcForbidStopList);
                appGroup.put(Integer.valueOf(1), sortGroup.mProcShortageStopList);
                appGroup.put(Integer.valueOf(2), sortGroup.mProcAllowStopList);
            }
        }
        AwareAppMngSortPolicy sortPolicy = new AwareAppMngSortPolicy(this.mContext, appGroup);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("        getAppMngSortPolicy eclipse time     :");
            stringBuilder.append(System.currentTimeMillis() - startTime);
            AwareLog.i(str, stringBuilder.toString());
            long availableRam = MemoryReader.getInstance().getMemAvailable();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("MemAvailable(KB): ");
            stringBuilder2.append(availableRam);
            AwareLog.i(str2, stringBuilder2.toString());
            dumpPolicy(sortPolicy, null, false);
        }
        if (Log.HWINFO && 2 == resourceType) {
            printBetaLog(sortPolicy);
        }
        return sortPolicy;
    }

    private void printBetaLog(AwareAppMngSortPolicy sortPolicy) {
        long curTime = System.currentTimeMillis();
        if (curTime - this.mLastBetaLogOutTime > AppHibernateCst.DELAY_ONE_MINS && this.mHandler != null) {
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = new BetaLog(sortPolicy);
            this.mHandler.sendMessage(msg);
            this.mLastBetaLogOutTime = curTime;
        }
    }

    private ArrayMap<Integer, List<AwareProcessBlockInfo>> getAppMngSortGroupForMemCleanChina(int groupId, int memLevel) {
        ArrayMap<Integer, List<AwareProcessBlockInfo>> appGroup = new ArrayMap();
        List<AwareProcessInfo> allAwareProcNeedProcess = AppStatusUtils.getInstance().getAllProcNeedSort();
        if (allAwareProcNeedProcess == null || allAwareProcNeedProcess.isEmpty()) {
            AwareLog.e(TAG, "getAllProcNeedSort failed!");
            return null;
        }
        ArraySet<String> listFilter = new ArraySet();
        if (mListFilerLevel2 == null) {
            updateLevel2ListFilter();
        }
        if (memLevel == 2 && mListFilerLevel2 != null) {
            listFilter.addAll(mListFilerLevel2);
        }
        List<AwareProcessBlockInfo> rawInfo = DecisionMaker.getInstance().decideAll(allAwareProcNeedProcess, memLevel, AppMngFeature.APP_CLEAN, AppCleanSource.MEMORY, listFilter);
        List<AwareProcessBlockInfo> needClean = CleanSource.mergeBlockForMemory(rawInfo, DecisionMaker.getInstance().getProcessList(AppMngFeature.APP_CLEAN, AppCleanSource.MEMORY));
        AppCleanupDumpRadar.getInstance().reportMemoryData(rawInfo, -1);
        if (!(needClean == null || needClean.isEmpty())) {
            if (memLevel == 3) {
                Collections.sort(needClean, BLOCK_BY_WEIGHT);
            } else {
                List<String> pkgTopN = null;
                Map<String, Integer> allTopList = null;
                AwareUserHabit habit = AwareUserHabit.getInstance();
                if (habit != null) {
                    allTopList = habit.getAllTopList();
                    pkgTopN = habit.getTopN(AppMngConfig.getTopN());
                }
                for (AwareProcessBlockInfo block : needClean) {
                    setPropImportance(block, allTopList, pkgTopN);
                }
                Collections.sort(needClean, BLOCK_BY_USER_HABIT);
            }
        }
        if (groupId != 0) {
            switch (groupId) {
                case 2:
                    appGroup.put(Integer.valueOf(2), needClean);
                    break;
                case 3:
                    appGroup.put(Integer.valueOf(0), rawInfo);
                    appGroup.put(Integer.valueOf(2), needClean);
                    break;
                default:
                    return null;
            }
        }
        appGroup.put(Integer.valueOf(0), rawInfo);
        return appGroup;
    }

    public AwareAppMngSortPolicy getAppMngSortPolicyForMemRepair(int sceneType) {
        if (!mEnabled) {
            return null;
        }
        ArrayMap<Integer, List<AwareProcessBlockInfo>> appGroup = new ArrayMap();
        List<AwareProcessInfo> allAwareProcNeedProcess = AppStatusUtils.getInstance().getAllProcNeedSort();
        if (allAwareProcNeedProcess == null || allAwareProcNeedProcess.isEmpty()) {
            AwareLog.e(TAG, "getAllProcNeedSort failed!");
            return null;
        }
        List<AwareProcessBlockInfo> rawInfo = DecisionMaker.getInstance().decideAll(allAwareProcNeedProcess, sceneType == 0 ? 0 : 1, AppMngFeature.APP_CLEAN, AppCleanSource.MEMORY_REPAIR);
        if (rawInfo == null || rawInfo.isEmpty()) {
            AwareLog.e(TAG, "decideAll get null for memRepair!");
            return null;
        }
        List<AwareProcessBlockInfo> resultInfo = CleanSource.mergeBlock(rawInfo);
        if (resultInfo == null) {
            AwareLog.e(TAG, "mergeBlock get null for memRepair!");
            return null;
        }
        List<AwareProcessBlockInfo> procForbidList = new ArrayList();
        List<AwareProcessBlockInfo> procAllowStopList = new ArrayList();
        for (AwareProcessBlockInfo blockInfo : resultInfo) {
            if (blockInfo != null) {
                if (blockInfo.mProcessList != null) {
                    if (!blockInfo.mProcessList.isEmpty()) {
                        if (blockInfo.mCleanType == CleanType.NONE) {
                            procForbidList.add(blockInfo);
                        } else {
                            procAllowStopList.add(blockInfo);
                        }
                    }
                }
            }
        }
        appGroup.put(Integer.valueOf(0), procForbidList);
        appGroup.put(Integer.valueOf(2), procAllowStopList);
        return new AwareAppMngSortPolicy(this.mContext, appGroup);
    }

    public List<AwareProcessInfo> getAppMngSortPolicyForSystemTrim() {
        if (mEnabled) {
            return AppStatusUtils.getInstance().getAllProcNeedSort();
        }
        return null;
    }

    public boolean checkNonSystemUser(AwareProcessInfo awareProcInfo) {
        boolean z = false;
        if (awareProcInfo == null || awareProcInfo.mProcInfo == null) {
            return false;
        }
        int curUserUid = AwareAppAssociate.getInstance().getCurUserId();
        if (curUserUid == 0 && !isCurUserProc(awareProcInfo.mProcInfo.mUid, curUserUid)) {
            z = true;
        }
        return z;
    }

    private boolean isNonSystemUser(int checkUid) {
        return AwareAppAssociate.getInstance().getCurUserId() == 0 && !isCurUserProc(checkUid, AwareAppAssociate.getInstance().getCurUserId());
    }

    private void setPropImportance(AwareProcessBlockInfo block, Map<String, Integer> allTopList, List<String> pkgTopN) {
        if (block != null) {
            if (isNonSystemUser(block.mUid)) {
                block.mImportance = 10000;
                return;
            }
            Integer importance = null;
            if (allTopList != null) {
                importance = (Integer) allTopList.get(block.mPackageName);
            }
            if (pkgTopN != null) {
                int topIdx = pkgTopN.indexOf(block.mPackageName);
                if (topIdx >= 0 && topIdx < 8) {
                    importance = Integer.valueOf(-100 * (8 - topIdx));
                }
            }
            block.mImportance = importance != null ? importance.intValue() : 0;
        }
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public void enableAssocDebug() {
        this.mAssocEnable = true;
    }

    public void disableAssocDebug() {
        this.mAssocEnable = false;
    }

    public boolean getAssocDebug() {
        return this.mAssocEnable;
    }

    private static String getForbidSubClassRateStr(int classRate) {
        for (ForbidSubClassRate rate : ForbidSubClassRate.values()) {
            if (rate.ordinal() == classRate) {
                return rate.description();
            }
        }
        return ForbidSubClassRate.NONE.description();
    }

    private static String getShortageSubClassRateStr(int classRate) {
        for (ShortageSubClassRate rate : ShortageSubClassRate.values()) {
            if (rate.ordinal() == classRate) {
                return rate.description();
            }
        }
        return ShortageSubClassRate.NONE.description();
    }

    private static String getAllowSubClassRateStr(int classRate) {
        for (AllowStopSubClassRate rate : AllowStopSubClassRate.values()) {
            if (rate.ordinal() == classRate) {
                return rate.description();
            }
        }
        return AllowStopSubClassRate.NONE.description();
    }

    private String getClassStr(int classRate, int subClassRate) {
        if (classRate == ClassRate.FOREGROUND.ordinal()) {
            return getForbidSubClassRateStr(subClassRate);
        }
        if (classRate == ClassRate.KEYSERVICES.ordinal()) {
            return getShortageSubClassRateStr(subClassRate);
        }
        return getAllowSubClassRateStr(subClassRate);
    }

    private void updateProcessInfo() {
        ProcessInfoCollector processInfoCollector = ProcessInfoCollector.getInstance();
        if (processInfoCollector != null) {
            ArrayList<ProcessInfo> procs = processInfoCollector.getProcessInfoList();
            if (!procs.isEmpty()) {
                int size = procs.size();
                for (int i = 0; i < size; i++) {
                    ProcessInfo procInfo = (ProcessInfo) procs.get(i);
                    if (procInfo != null) {
                        processInfoCollector.recordProcessInfo(procInfo.mPid, procInfo.mUid);
                    }
                }
            }
        }
    }

    private int getCurHomeProcessPid() {
        return AwareAppAssociate.getInstance().getCurHomeProcessPid();
    }

    public boolean isProcessBlockPidChanged(AwareProcessBlockInfo procGroup) {
        if (!mEnabled || procGroup == null) {
            return false;
        }
        ArrayList<ProcessInfo> procs = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procs.isEmpty()) {
            return false;
        }
        int uid = procGroup.mUid;
        int size = procs.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo procInfo = (ProcessInfo) procs.get(i);
            if (procInfo != null && procInfo.mUid == uid && procInfo.mCreatedTime - procGroup.mUpdateTime > 0) {
                return true;
            }
        }
        return false;
    }

    private void dumpBlockList(PrintWriter pw, List<AwareProcessBlockInfo> list, boolean toPrint) {
        if (list != null && (pw != null || !toPrint)) {
            for (AwareProcessBlockInfo pinfo : list) {
                if (pinfo != null) {
                    boolean allow = pinfo.mResCleanAllow;
                    String dumpInfo = new StringBuilder();
                    dumpInfo.append("AppProc:uid:");
                    dumpInfo.append(pinfo.mUid);
                    dumpInfo.append(",import:");
                    dumpInfo.append(pinfo.mImportance);
                    dumpInfo.append(",classRates:");
                    dumpInfo.append(pinfo.mClassRate);
                    dumpInfo.append(",classStr:");
                    dumpInfo.append(getClassRateStr(pinfo.mClassRate));
                    dumpInfo.append(",subStr:");
                    dumpInfo.append(getClassStr(pinfo.mClassRate, pinfo.mSubClassRate));
                    dumpInfo.append(",subTypeStr:");
                    dumpInfo.append(pinfo.mSubTypeStr);
                    dumpInfo.append(",appType:");
                    dumpInfo.append(pinfo.mAppType);
                    dumpInfo.append(",reason:");
                    dumpInfo.append(pinfo.mReason);
                    dumpInfo.append(",weight:");
                    dumpInfo.append(pinfo.mWeight);
                    print(pw, dumpInfo.toString());
                    if (pinfo.mProcessList != null) {
                        for (AwareProcessInfo info : pinfo.mProcessList) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("     name:");
                            stringBuilder.append(info.mProcInfo.mProcessName);
                            stringBuilder.append(",pid:");
                            stringBuilder.append(info.mProcInfo.mPid);
                            stringBuilder.append(",uid:");
                            stringBuilder.append(info.mProcInfo.mUid);
                            stringBuilder.append(",group:");
                            stringBuilder.append(info.mMemGroup);
                            stringBuilder.append(",import:");
                            stringBuilder.append(info.mImportance);
                            stringBuilder.append(",classRate:");
                            stringBuilder.append(info.mClassRate);
                            stringBuilder.append(",adj:");
                            stringBuilder.append(info.mProcInfo.mCurAdj);
                            stringBuilder.append(",");
                            stringBuilder.append(info.mProcInfo.mAdjType);
                            stringBuilder.append(",classStr:");
                            stringBuilder.append(getClassRateStr(info.mClassRate));
                            stringBuilder.append(",subStr:");
                            stringBuilder.append(getClassStr(info.mClassRate, info.mSubClassRate));
                            stringBuilder.append(",mResCleanAllow:");
                            stringBuilder.append(allow);
                            stringBuilder.append(",mRestartFlag:");
                            stringBuilder.append(info.getRestartFlag());
                            stringBuilder.append(",ui:");
                            stringBuilder.append(info.mHasShownUi);
                            print(pw, stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private void dumpStringList(PrintWriter pw, List<String> list) {
        if (list != null) {
            for (String pinfo : list) {
                if (pinfo != null) {
                    print(pw, pinfo);
                }
            }
        }
    }

    private void dumpBlock(PrintWriter pw, int memLevel, boolean isMemClean) {
        if (pw != null) {
            if (mEnabled) {
                AwareAppMngSortPolicy policy;
                if (isMemClean) {
                    policy = getAppMngSortPolicy(2, memLevel, 3);
                } else {
                    policy = getAppMngSortPolicy(0, memLevel, 3);
                }
                if (policy == null) {
                    pw.println("getAppMngSortPolicy return null!");
                    return;
                } else {
                    dumpPolicy(policy, pw, true);
                    return;
                }
            }
            pw.println("AwareAppMngSort disabled!");
        }
    }

    private void dumpGroupBlock(PrintWriter pw, int group) {
        if (pw != null) {
            if (mEnabled) {
                AwareAppMngSortPolicy policy = getAppMngSortPolicy(0, 0, group);
                if (policy == null) {
                    pw.println("getAppMngSortPolicy return null!");
                    return;
                } else {
                    dumpPolicy(policy, pw, true);
                    return;
                }
            }
            pw.println("AwareAppMngSort disabled!");
        }
    }

    private void dumpPolicy(AwareAppMngSortPolicy policy, PrintWriter pw, boolean toPrint) {
        if (policy != null && (pw != null || !toPrint)) {
            print(pw, "------------------start dump Group  forbidstop ------------------");
            dumpBlockList(pw, policy.getForbidStopProcBlockList(), toPrint);
            print(pw, "------------------start dump Group  shortagestop ------------------");
            dumpBlockList(pw, policy.getShortageStopProcBlockList(), toPrint);
            print(pw, "------------------start dump Group  allowstop ------------------");
            dumpBlockList(pw, policy.getAllowStopProcBlockList(), toPrint);
        }
    }

    private void print(PrintWriter pw, String info) {
        if (pw != null) {
            pw.println(info);
        } else if (DEBUG) {
            AwareLog.i(TAG, info);
        }
    }

    public void dump(PrintWriter pw, String type) {
        if (pw != null && type != null) {
            pw.println("  App Group Manager Information dump :");
            if (!dumpForResourceType(pw, type)) {
                if (type.equals("memForbid")) {
                    dumpGroupBlock(pw, 0);
                } else if (type.equals("memShortage")) {
                    dumpGroupBlock(pw, 1);
                } else if (type.equals("memAllow")) {
                    dumpGroupBlock(pw, 2);
                } else if (type.equals("enable")) {
                    enable();
                } else if (type.equals("disable")) {
                    disable();
                } else if (type.equals("checkEnabled")) {
                    boolean status = checkAppMngEnable();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AwareAppMngSort is ");
                    stringBuilder.append(status);
                    pw.println(stringBuilder.toString());
                } else if (!type.equals("procinfo")) {
                    pw.println("  dump parameter error!");
                } else if (mEnabled) {
                    ProcessInfoCollector mProcInfo = ProcessInfoCollector.getInstance();
                    if (mProcInfo != null) {
                        updateProcessInfo();
                        mProcInfo.dump(pw);
                    }
                } else {
                    pw.println("AwareAppMngSort disabled!");
                }
            }
        }
    }

    public void dumpRemoveAlarm(PrintWriter pw, String[] args) {
        String pkg = null;
        if (args.length > 2) {
            pkg = args[2];
        }
        if (pkg == null) {
            pw.println("dumpRemoveAlarm package can not be null!");
            return;
        }
        String tag = null;
        if (args.length > 3) {
            tag = args[3];
        }
        int uid = 0;
        if (args.length > 4) {
            try {
                uid = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                pw.println("  please check input uid!");
                return;
            }
        }
        List<String> tags = new ArrayList();
        tags.add(tag);
        HwActivityManager.removePackageAlarm(pkg, tags, uid);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dumpRemoveAlarm sucessfull tag:");
        stringBuilder.append(tag);
        pw.println(stringBuilder.toString());
    }

    public void dumpRemoveInvalidAlarm(PrintWriter pw, String[] args) {
        String pkg = null;
        if (args.length > 2) {
            pkg = args[2];
        }
        if (pkg == null) {
            pw.println("dumpRemoveAlarm package can not be null!");
            return;
        }
        int uid = 0;
        if (args.length > 3) {
            try {
                uid = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                pw.println("  please check input uid!");
                return;
            }
        }
        List<String> tags = AwareIntelligentRecg.getInstance().getAllInvalidAlarmTags(uid, pkg);
        if (tags != null && !tags.isEmpty()) {
            HwActivityManager.removePackageAlarm(pkg, tags, uid);
            pw.println("dumpRemoveAlarm sucessfull tags:");
            for (String tag : tags) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("tag: ");
                stringBuilder.append(tag);
                pw.println(stringBuilder.toString());
            }
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean dumpForResourceType(PrintWriter pw, String type) {
        int i;
        switch (type.hashCode()) {
            case -1831967232:
                if (type.equals("smartClean")) {
                    i = 5;
                    break;
                }
            case -1389206271:
                if (type.equals("bigApp")) {
                    i = 4;
                    break;
                }
            case -677872268:
                if (type.equals("memClean")) {
                    i = 2;
                    break;
                }
            case 107989:
                if (type.equals("mem")) {
                    i = false;
                    break;
                }
            case 3347709:
                if (type.equals("mem2")) {
                    i = 1;
                    break;
                }
            case 460796222:
                if (type.equals("memClean2")) {
                    i = 6;
                    break;
                }
            case 460796223:
                if (type.equals("memClean3")) {
                    i = 3;
                    break;
                }
            case 884096450:
                if (type.equals(MemoryConstant.MEM_POLICY_REPAIR)) {
                    i = 8;
                    break;
                }
            case 1493492622:
                if (type.equals("memCompact")) {
                    i = 7;
                    break;
                }
            case 1637186224:
                if (type.equals("memRepair2")) {
                    i = 9;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                dumpBlock(pw, 0, false);
                break;
            case 1:
                dumpBlock(pw, 1, false);
                break;
            case 2:
                dumpBlock(pw, 0, true);
                break;
            case 3:
                dumpBlock(pw, 2, true);
                break;
            case 4:
                dumpBlock(pw, 3, true);
                break;
            case 5:
                dumpSmartClean(pw);
                break;
            case 6:
                dumpBlock(pw, 1, true);
                break;
            case 7:
                dumpMemCompactGroup(pw);
                break;
            case 8:
                dumpMemRepair(pw, 0);
                break;
            case 9:
                dumpMemRepair(pw, 1);
                break;
            default:
                return false;
        }
        return true;
    }

    private void dumpSmartClean(PrintWriter pw) {
        if (pw != null) {
            print(pw, "------------------start dump Group  small sample list ------------------");
            dumpStringList(pw, AwareIntelligentRecg.getInstance().getSmallSampleList());
            print(pw, "------------------start dump Group  clean Group ------------------");
            dumpBlockList(pw, new SmartClean(this.mContext).getSmartCleanList(null), true);
        }
    }

    private void dumpMemCompactGroup(PrintWriter pw) {
        AwareAppMngSortPolicy policy = getAppMngSortPolicy(1, null, 3);
        if (policy != null) {
            print(pw, "------------------start dump Group forbid group ------------------");
            dumpBlockList(pw, policy.getForbidStopProcBlockList(), true);
            print(pw, "------------------start dump Group compactible group ------------------");
            dumpBlockList(pw, policy.getShortageStopProcBlockList(), true);
            print(pw, "------------------start dump Group frozen group ------------------");
            dumpBlockList(pw, policy.getAllowStopProcBlockList(), true);
        }
    }

    private void dumpMemRepair(PrintWriter pw, int sceneType) {
        AwareAppMngSortPolicy policy = getAppMngSortPolicyForMemRepair(sceneType);
        if (policy != null) {
            print(pw, "------------------start dump Group forbid group ------------------");
            dumpBlockList(pw, policy.getForbidStopProcBlockList(), true);
            print(pw, "------------------start dump Group allowstop group ------------------");
            dumpBlockList(pw, policy.getAllowStopProcBlockList(), true);
        }
    }

    private void dumpShortageSubClassRate(PrintWriter pw) {
        if (pw != null) {
            for (ShortageSubClassRate rate : ShortageSubClassRate.values()) {
                String classRate = rate.description();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    sub");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(": value=");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(",");
                stringBuilder.append(classRate);
                pw.println(stringBuilder.toString());
            }
        }
    }

    private void dumpForbidSubClassRate(PrintWriter pw) {
        if (pw != null) {
            for (ForbidSubClassRate rate : ForbidSubClassRate.values()) {
                String classRate = rate.description();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    sub");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(": value=");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(",");
                stringBuilder.append(classRate);
                pw.println(stringBuilder.toString());
            }
        }
    }

    private void dumpAllowStopSubClassRate(PrintWriter pw) {
        if (pw != null) {
            for (AllowStopSubClassRate rate : AllowStopSubClassRate.values()) {
                String classRate = rate.description();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    sub");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(": value=");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(",");
                stringBuilder.append(classRate);
                pw.println(stringBuilder.toString());
            }
        }
    }

    public void dumpClassInfo(PrintWriter pw) {
        if (!mEnabled) {
            pw.println("AwareAppMngSort disabled!");
        } else if (pw != null) {
            for (ClassRate rate : ClassRate.values()) {
                String classRate = rate.description();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Class");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(": value=");
                stringBuilder.append(rate.ordinal());
                stringBuilder.append(",");
                stringBuilder.append(classRate);
                pw.println(stringBuilder.toString());
                String subClass = ShortageSubClassRate.NONE.description();
                if (rate == ClassRate.FOREGROUND) {
                    dumpForbidSubClassRate(pw);
                } else if (rate == ClassRate.KEYSERVICES) {
                    dumpShortageSubClassRate(pw);
                } else if (rate == ClassRate.NORMAL) {
                    dumpAllowStopSubClassRate(pw);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    sub");
                    stringBuilder2.append(ShortageSubClassRate.NONE.ordinal());
                    stringBuilder2.append(": value=");
                    stringBuilder2.append(ShortageSubClassRate.NONE.ordinal());
                    stringBuilder2.append(",");
                    stringBuilder2.append(subClass);
                    pw.println(stringBuilder2.toString());
                }
            }
        }
    }

    public void dumpAlarm(PrintWriter pw, String[] args) {
        String pkg = null;
        if (args.length > 2) {
            pkg = args[2];
        }
        if (pkg == null) {
            pw.println("dumpAlarm package can not be null!");
            return;
        }
        int uid = 0;
        if (args.length > 3) {
            try {
                uid = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                pw.println("  please check input uid!");
                return;
            }
        }
        List<String> tags = AwareIntelligentRecg.getInstance().getAllInvalidAlarmTags(uid, pkg);
        if (tags == null || tags.isEmpty()) {
            pw.println("getAllInvalidAlarmTags is null or empty.");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAllInvalidAlarmTags:");
            stringBuilder.append(tags);
            pw.println(stringBuilder.toString());
        }
        List<String> packageList = new ArrayList();
        packageList.add(pkg);
        boolean has = AwareIntelligentRecg.getInstance().hasPerceptAlarm(uid, packageList);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("hasPerceptAlarm: ");
        stringBuilder2.append(has);
        pw.println(stringBuilder2.toString());
    }

    private void updateLevel2ListFilter() {
        ArrayList<String> rawFilter = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_CLEAN.getDesc(), LIST_FILTER_LEVEL2);
        ArraySet<String> listFilter = new ArraySet();
        if (rawFilter != null) {
            listFilter.addAll(rawFilter);
        }
        mListFilerLevel2 = listFilter;
    }

    public void updateCloudData() {
        updateFileredApps();
        updateLevel2ListFilter();
    }
}
