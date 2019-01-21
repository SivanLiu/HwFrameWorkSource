package com.android.server.rms.iaware.appmng;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.rms.HwSysResManager;
import android.rms.IUpdateWhiteListCallback;
import android.rms.IUpdateWhiteListCallback.Stub;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.algorithm.AwareUserHabitAlgorithm.HabitProtectListChangeListener;
import com.huawei.android.app.HwActivityManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AwareDefaultConfigList {
    private static final String ADJCUSTTOP_CNT_KEY = "ADJCUSTTOP_CNT";
    private static final String ALARM_CHK_KEY = "ALM";
    public static final String APPMNG_2G_CFG_KEY = "APPMNG_2G_CFG";
    public static final String APPMNG_3G_CFG_KEY = "APPMNG_3G_CFG";
    public static final String APPMNG_4G_CFG_KEY = "APPMNG_4G_CFG";
    private static final int APPMNG_BADAPP_TYPE = 7;
    private static final int APPMNG_CFG_ADJ_2G_TYPE = 4;
    private static final int APPMNG_CFG_ADJ_3G_TYPE = 3;
    private static final int APPMNG_CFG_ADJ_TYPE = 2;
    private static final int APPMNG_CFG_WHITE_TYPE = 1;
    private static final int APPMNG_LOWEND_PROTECTED_ID = 5;
    private static final int APPMNG_RESTARTAPP_TYPE = 6;
    private static final int BG_DECAY_TWO_HOUR_MASK = 4;
    private static final String BG_DELAY_KEY = "BGDCY";
    private static boolean DEBUG = false;
    public static final int GROUP_ID_FORBIDSTOP = 1;
    public static final int GROUP_ID_NOSPEC = 0;
    public static final int GROUP_ID_SHORTAGESTOP = 2;
    private static final int HABIT_PROT_MAX_CNT = 10000;
    public static final int HW_PERCEPTIBLE_APP_ADJ = 260;
    private static final String IM_CNT_KEY = "IM_CNT";
    private static final String KEYPROC_DECAY_KEY = "KEYPROC_DECAY";
    private static final String KILL_MORE_KEY = "KM";
    private static final int KILL_MORE_MASK = 2;
    private static final String LOWEND_KEY = "LOW";
    private static final int LOW_END_MASK = 1;
    private static final int MASK_CLEANRES = 1;
    private static final int MASK_FREQUENTLYUSED = 4;
    private static final int MASK_GROUP = 3840;
    private static final int MASK_RESTART = 2;
    private static final int MEM_OPT_FOR_3G = SystemProperties.getInt("sys.iaware.mem_opt", 0);
    private static final String MEM_THRD_KEY = "MEM_THRD";
    private static final String PG_PROTECT_KEY = "PROT";
    private static final String RESTART_KEY = "RESTART";
    private static final String SCREEN_CHANGED_KEY = "screenChanged";
    private static final String SEPARATOR = "#";
    private static final String SMART_CLEAN_INTERVAL_KEY = "smartCleanInterval";
    private static final String SYSPROC_DECAY_KEY = "SYSPROC_DECAY";
    private static final String TAG = "AwareDefaultConfigList";
    private static final String TOPN_CNT_KEY = "TOPN_CNT";
    private static AwareDefaultConfigList sInstance = null;
    private final ArraySet<String> mAdjustAdjList = new ArraySet();
    private final ArrayList<String> mAllHabitAppList = new ArrayList();
    private final ArraySet<String> mAwareProtectList = new ArraySet();
    private final ArrayMap<String, PackageConfigItem> mAwareProtectMap = new ArrayMap();
    private final ArraySet<String> mBadAppList = new ArraySet();
    private int mCfgAdjTypeId = 4;
    private Context mContext;
    private boolean mEnabled = false;
    private final ArraySet<String> mHabitFrequentUsed = new ArraySet();
    private HabitProtectListChangeListener mHabitListener = new HabitProtectListChangeListener() {
        public void onListChanged() {
            AwareDefaultConfigList.this.setHabitWhiteList();
        }
    };
    private boolean mHasReadXml = false;
    private final ArrayList<String> mKeyHabitAppList = new ArrayList();
    private boolean mLowEnd = false;
    private final ArraySet<String> mRestartAppList = new ArraySet();
    private IUpdateWhiteListCallback mUpdateWhiteListCallback = new Stub() {
        public void update() throws RemoteException {
            if (AwareDefaultConfigList.DEBUG) {
                AwareLog.d(AwareDefaultConfigList.TAG, "IUpdateWhiteListCallback update whiteList.");
            }
            synchronized (AwareDefaultConfigList.this) {
                AwareDefaultConfigList.this.mHasReadXml = false;
                AwareDefaultConfigList.this.mAwareProtectList.clear();
            }
            AwareDefaultConfigList.this.setStaticXmlWhiteList();
        }
    };

    static class AppMngCfgXml {
        int mAdjCustTopN = 0;
        boolean mAlarmChk = true;
        long mBgDecay = 7200;
        int mImCnt = 0;
        long mKeyDecay = 0;
        boolean mKillMore = false;
        boolean mLowEnd = false;
        long mMemThrd = 0;
        boolean mPgProtect = false;
        boolean mRestart = true;
        int mScreenChanged = 30;
        int mSmartCleanInterval = 600;
        long mSysDecay = 0;
        int mTopNCnt = 0;

        AppMngCfgXml() {
        }
    }

    public static class ProcessConfigItem {
        public boolean mFrequentlyUsed;
        public int mGroupId;
        public String mName;
        public boolean mResCleanAllow;
        public boolean mRestartFlag;

        private ProcessConfigItem() {
        }

        public ProcessConfigItem(String name, int value) {
            this.mName = name;
            boolean z = false;
            this.mResCleanAllow = (value & 1) != 0;
            this.mRestartFlag = (value & 2) != 0;
            if ((value & 4) != 0) {
                z = true;
            }
            this.mFrequentlyUsed = z;
            this.mGroupId = ((value & 3840) >> 8) & 15;
        }

        public ProcessConfigItem copy() {
            ProcessConfigItem dst = new ProcessConfigItem();
            dst.mName = this.mName;
            dst.mResCleanAllow = this.mResCleanAllow;
            dst.mRestartFlag = this.mRestartFlag;
            dst.mFrequentlyUsed = this.mFrequentlyUsed;
            dst.mGroupId = this.mGroupId;
            return dst;
        }
    }

    public static class PackageConfigItem extends ProcessConfigItem {
        ArrayMap<String, ProcessConfigItem> mProcessMap = new ArrayMap();

        public PackageConfigItem(String name, int value) {
            super(name, value);
        }

        public void add(ProcessConfigItem item) {
            if (item != null) {
                this.mProcessMap.put(item.mName, item);
            }
        }

        public boolean isEmpty() {
            return this.mProcessMap.isEmpty();
        }

        public ProcessConfigItem getItem(String processName) {
            return (ProcessConfigItem) this.mProcessMap.get(processName);
        }
    }

    private AwareDefaultConfigList() {
    }

    private void initialize(Context context) {
        this.mContext = context;
        this.mEnabled = true;
        setAllWhiteList();
        startObserver();
        if (this.mLowEnd) {
            AwareUserHabit usrhabit = AwareUserHabit.getInstance();
            if (usrhabit != null) {
                usrhabit.setLowEndFlag(true);
            }
        }
    }

    private void deInitialize() {
        AwareUserHabit habitInstance = AwareUserHabit.getInstance();
        if (habitInstance != null) {
            habitInstance.unregistHabitProtectListChangeListener(this.mHabitListener);
        }
        this.mEnabled = false;
        synchronized (this) {
            this.mHasReadXml = false;
            this.mAwareProtectList.clear();
            this.mAwareProtectMap.clear();
            this.mKeyHabitAppList.clear();
            this.mAllHabitAppList.clear();
        }
        synchronized (this.mAdjustAdjList) {
            this.mAdjustAdjList.clear();
        }
        synchronized (this.mRestartAppList) {
            this.mRestartAppList.clear();
        }
        synchronized (this.mBadAppList) {
            this.mBadAppList.clear();
        }
        synchronized (this.mHabitFrequentUsed) {
            this.mHabitFrequentUsed.clear();
        }
    }

    public static void enable(Context context) {
        if (DEBUG) {
            AwareLog.d(TAG, "WhiteList Feature enable!!!");
        }
        getInstance().initialize(context);
    }

    public static void disable() {
        if (DEBUG) {
            AwareLog.d(TAG, "WhiteList Feature disable!!!");
        }
        getInstance().deInitialize();
    }

    private void startObserver() {
        if (DEBUG) {
            AwareLog.d(TAG, "WhiteList Feature startObserver!!!");
        }
        startXmlObserver();
        startHabitObserver();
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public static AwareDefaultConfigList getInstance() {
        AwareDefaultConfigList awareDefaultConfigList;
        synchronized (AwareDefaultConfigList.class) {
            if (sInstance == null) {
                sInstance = new AwareDefaultConfigList();
            }
            awareDefaultConfigList = sInstance;
        }
        return awareDefaultConfigList;
    }

    private void setAllWhiteList() {
        setStaticXmlWhiteList();
        setHabitWhiteList();
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            updateAppMngCfgFromRMS(28);
            updateAdjWhiteListFromRMS(28);
            updateRestartAppListFromRMS(28);
            updateBadAppListFromRMS(28);
            r0 = getWhiteListFromRMS(28);
     */
    /* JADX WARNING: Missing block: B:8:0x001a, code skipped:
            if (r0 != null) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:9:0x001c, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:10:0x001d, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r2.mAwareProtectList.addAll(r0);
            parseAwareProtectList();
            r2.mHasReadXml = true;
     */
    /* JADX WARNING: Missing block: B:13:0x0029, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setStaticXmlWhiteList() {
        synchronized (this) {
            if (this.mHasReadXml) {
            }
        }
    }

    private List<String> getGcmAppList(AwareUserHabit habitInstance) {
        List<String> gcmList = habitInstance.getGCMAppList();
        if (gcmList == null || AppMngConfig.getAbroadFlag()) {
            return gcmList;
        }
        List<String> gcmListFilter = new ArrayList();
        for (String pkg : gcmList) {
            if (isNeedGcmByAppType(AppTypeRecoManager.getInstance().getAppType(pkg))) {
                gcmListFilter.add(pkg);
            }
        }
        return gcmListFilter;
    }

    private boolean isNeedGcmByAppType(int appType) {
        boolean z = true;
        if (appType != 255) {
            switch (appType) {
                case -1:
                case 0:
                case 1:
                    break;
                default:
                    if (appType <= 255) {
                        z = false;
                    }
                    return z;
            }
        }
        return true;
    }

    private void setHabitWhiteList() {
        AwareUserHabit habitInstance = AwareUserHabit.getInstance();
        if (habitInstance != null) {
            List<String> habitProtectList;
            List<String> gcmList = getGcmAppList(habitInstance);
            List<String> habitProtectListAll = habitInstance.getHabitProtectListAll(10000, 10000);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AllHabitListChangeListener onListChanged list:");
                stringBuilder.append(habitProtectListAll);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (habitProtectListAll != null) {
                if (gcmList != null) {
                    habitProtectListAll.addAll(gcmList);
                }
                updateAllImCache(habitProtectListAll);
            }
            if (this.mLowEnd) {
                habitProtectList = AppMngConfig.getImCnt() / 2;
                habitProtectList = habitInstance.queryHabitProtectAppList(AppMngConfig.getImCnt() - habitProtectList, habitProtectList);
            } else {
                habitProtectList = habitInstance.getHabitProtectList(10000, 10000);
            }
            List<String> keyImList = new ArrayList();
            if (habitProtectList != null) {
                keyImList.addAll(habitProtectList);
            }
            if (!(gcmList == null || this.mLowEnd)) {
                keyImList.addAll(gcmList);
            }
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("HabitListChangeListener onListChanged list:");
                stringBuilder2.append(keyImList);
                AwareLog.i(str2, stringBuilder2.toString());
            }
            updateKeyImCache(keyImList);
        }
    }

    private ArraySet<String> getWhiteListFromRMS(int rmsGroupId) {
        String str;
        ArraySet<String> whiteList = new ArraySet();
        HwSysResManager resManager = HwSysResManager.getInstance();
        int i = 0;
        if (this.mLowEnd) {
            str = resManager.getWhiteList(rmsGroupId, 5);
        } else {
            str = resManager.getWhiteList(rmsGroupId, 0);
        }
        if (str == null) {
            if (DEBUG) {
                AwareLog.e(TAG, "getWhiteListFromRMS failed because null whiteList!");
            }
            return null;
        }
        String[] contentArray = str.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        int length = contentArray.length;
        while (i < length) {
            String content = contentArray[i].trim();
            if (!content.isEmpty()) {
                whiteList.add(content);
            }
            i++;
        }
        return whiteList;
    }

    private void updateAppMngCfgFromRMS(int rmsGroupId) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        int i = 1;
        String str = resManager.getWhiteList(rmsGroupId, 1);
        HwSysResManager resManager2;
        if (str == null || !str.contains("{")) {
        } else if (str.contains("}")) {
            AppMngCfgXml cfg2G = new AppMngCfgXml();
            AppMngCfgXml cfg3G = new AppMngCfgXml();
            AppMngCfgXml cfg4G = new AppMngCfgXml();
            AppMngCfgXml cfgCur = null;
            String[] contentArray = str.split("\\}");
            int length = contentArray.length;
            int i2 = 0;
            int i3 = 0;
            while (i3 < length) {
                String content = contentArray[i3];
                if (content != null) {
                    String[] contentArraySplit = content.split("\\{");
                    if (contentArraySplit.length > i) {
                        String keyString = contentArraySplit[i2];
                        String valueString = contentArraySplit[i];
                        if (keyString == null) {
                            resManager2 = resManager;
                        } else if (valueString != null) {
                            keyString = keyString.trim();
                            valueString = valueString.trim();
                            if (keyString.contains(APPMNG_2G_CFG_KEY)) {
                                resManager2 = resManager;
                                if (cfg2G.mMemThrd == 0) {
                                    setAppMngCfg(valueString, cfg2G);
                                }
                            } else {
                                resManager2 = resManager;
                            }
                            if (keyString.contains(APPMNG_3G_CFG_KEY) != null && cfg3G.mMemThrd == 0) {
                                setAppMngCfg(valueString, cfg3G);
                            } else if (keyString.contains(APPMNG_4G_CFG_KEY) != null && cfg4G.mMemThrd == 0) {
                                setAppMngCfg(valueString, cfg4G);
                            }
                        }
                        i3++;
                        resManager = resManager2;
                        i = 1;
                        i2 = 0;
                    }
                }
                resManager2 = resManager;
                i3++;
                resManager = resManager2;
                i = 1;
                i2 = 0;
            }
            resManager = AppMngConfig.getMemorySize();
            if (cfg2G.mMemThrd != 0 && resManager < cfg2G.mMemThrd) {
                cfgCur = cfg2G;
                this.mCfgAdjTypeId = 4;
            } else if (cfg3G.mMemThrd != 0 && resManager < cfg3G.mMemThrd) {
                cfgCur = cfg3G;
                if ((MEM_OPT_FOR_3G & 1) != 0) {
                    cfgCur.mLowEnd = true;
                }
                if ((MEM_OPT_FOR_3G & 2) != 0) {
                    cfgCur.mKillMore = true;
                }
                if ((MEM_OPT_FOR_3G & 4) != 0) {
                    cfgCur.mBgDecay = 120;
                }
                this.mCfgAdjTypeId = 3;
            } else if (cfg4G.mMemThrd != 0) {
                cfgCur = cfg4G;
                this.mCfgAdjTypeId = 2;
            }
            if (cfgCur != null) {
                this.mLowEnd = cfgCur.mLowEnd;
                boolean restartFlag = cfgCur.mRestart;
                AppMngConfig.setAbroadFlag(isAbroadArea());
                AppMngConfig.setRestartFlag(restartFlag);
                AppMngConfig.setTopN(cfgCur.mTopNCnt);
                AppMngConfig.setImCnt(cfgCur.mImCnt);
                AppMngConfig.setSysDecay(cfgCur.mSysDecay);
                AppMngConfig.setKeySysDecay(cfgCur.mKeyDecay);
                AppMngConfig.setAdjCustTopN(cfgCur.mAdjCustTopN);
                AppMngConfig.setBgDecay(cfgCur.mBgDecay);
                AppMngConfig.setPgProtectFlag(cfgCur.mPgProtect);
                AppMngConfig.setAlarmChkFlag(cfgCur.mAlarmChk);
                AppMngConfig.setKillMoreFlag(cfgCur.mKillMore);
                AppMngConfig.setScreenChangedThreshold(cfgCur.mScreenChanged);
                AppMngConfig.setSmartCleanInterval(cfgCur.mSmartCleanInterval);
            }
        } else {
            resManager2 = resManager;
        }
    }

    public boolean isLowEnd() {
        return this.mLowEnd;
    }

    private void updateAdjWhiteListFromRMS(int rmsGroupId) {
        Set<String> adjList = updateListFromRMS(rmsGroupId, this.mCfgAdjTypeId);
        if (adjList != null) {
            synchronized (this.mAdjustAdjList) {
                this.mAdjustAdjList.clear();
                this.mAdjustAdjList.addAll(adjList);
            }
        }
    }

    private void updateRestartAppListFromRMS(int rmsGroupId) {
        Set<String> adjList = updateListFromRMS(rmsGroupId, 6);
        if (adjList != null) {
            synchronized (this.mRestartAppList) {
                this.mRestartAppList.clear();
                this.mRestartAppList.addAll(adjList);
            }
        }
    }

    private void updateBadAppListFromRMS(int rmsGroupId) {
        Set<String> adjList = updateListFromRMS(rmsGroupId, 7);
        if (adjList != null) {
            synchronized (this.mBadAppList) {
                this.mBadAppList.clear();
                this.mBadAppList.addAll(adjList);
            }
        }
    }

    private Set<String> updateListFromRMS(int rmsGroupId, int whiteListType) {
        String str = HwSysResManager.getInstance().getWhiteList(rmsGroupId, whiteListType);
        if (str == null) {
            if (DEBUG) {
                AwareLog.e(TAG, "updateAdjWhiteListFromRMS failed because null whiteList!");
            }
            return null;
        }
        ArraySet<String> adjList = new ArraySet();
        int i = 0;
        String[] contentArray = str.split("#")[0].split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        int length = contentArray.length;
        while (i < length) {
            String content = contentArray[i].trim();
            if (!content.isEmpty()) {
                adjList.add(content);
            }
            i++;
        }
        return adjList;
    }

    private void setCfgParam(AppMngCfgXml cfg2G, String cfgType, int value) {
        if (MEM_THRD_KEY.equals(cfgType)) {
            cfg2G.mMemThrd = (long) value;
        } else if (TOPN_CNT_KEY.equals(cfgType)) {
            cfg2G.mTopNCnt = value;
        } else if (IM_CNT_KEY.equals(cfgType)) {
            cfg2G.mImCnt = value;
        } else if (SYSPROC_DECAY_KEY.equals(cfgType)) {
            cfg2G.mSysDecay = (long) value;
        } else if (KEYPROC_DECAY_KEY.equals(cfgType)) {
            cfg2G.mKeyDecay = (long) value;
        } else if (ADJCUSTTOP_CNT_KEY.equals(cfgType)) {
            cfg2G.mAdjCustTopN = value;
        } else {
            boolean z = false;
            if (RESTART_KEY.equals(cfgType)) {
                if (value != 0) {
                    z = true;
                }
                cfg2G.mRestart = z;
            } else if (BG_DELAY_KEY.equals(cfgType)) {
                cfg2G.mBgDecay = (long) value;
            } else if (PG_PROTECT_KEY.equals(cfgType)) {
                if (value != 0) {
                    z = true;
                }
                cfg2G.mPgProtect = z;
            } else if (ALARM_CHK_KEY.equals(cfgType)) {
                if (value != 0) {
                    z = true;
                }
                cfg2G.mAlarmChk = z;
            } else if (KILL_MORE_KEY.equals(cfgType)) {
                if (value != 0) {
                    z = true;
                }
                cfg2G.mKillMore = z;
            } else if (LOWEND_KEY.equals(cfgType)) {
                if (value != 0) {
                    z = true;
                }
                cfg2G.mLowEnd = z;
            } else if (SCREEN_CHANGED_KEY.equals(cfgType)) {
                cfg2G.mScreenChanged = value;
            } else if (SMART_CLEAN_INTERVAL_KEY.equals(cfgType)) {
                cfg2G.mSmartCleanInterval = value;
            }
        }
    }

    private void setAppMngCfg(String str, AppMngCfgXml cfg2G) {
        if (str != null && cfg2G != null) {
            for (String content : str.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
                if (content != null) {
                    String[] names = content.trim().split(":");
                    if (names.length > 1) {
                        String cfgType = names[0];
                        String cfgValue = names[1];
                        if (!(cfgType == null || cfgValue == null)) {
                            cfgType = cfgType.trim();
                            int value = 0;
                            try {
                                value = Integer.parseInt(cfgValue.trim(), 10);
                            } catch (NumberFormatException e) {
                                AwareLog.e(TAG, "parseInt error");
                            }
                            setCfgParam(cfg2G, cfgType, value);
                        }
                    }
                }
            }
        }
    }

    private void parseAwareProtectList() {
        synchronized (this) {
            if (this.mAwareProtectList == null) {
                return;
            }
            this.mAwareProtectMap.clear();
            int value = 0;
            Iterator it = this.mAwareProtectList.iterator();
            while (it.hasNext()) {
                String str = (String) it.next();
                if (str != null && str.contains("{")) {
                    if (str.contains("}")) {
                        int startIdx = str.indexOf("{");
                        int endIdx = str.indexOf("}");
                        if (startIdx + 1 >= endIdx) {
                            continue;
                        } else if (startIdx + 1 < str.length()) {
                            String pkgName = str.substring(0, startIdx);
                            try {
                                value = Integer.parseInt(str.substring(startIdx + 1, endIdx), 16);
                            } catch (NumberFormatException e) {
                                AwareLog.e(TAG, "parseInt error");
                            }
                            String[] names = pkgName.split("#");
                            pkgName = names[0];
                            String processName = names.length > 1 ? names[1] : null;
                            if (!pkgName.isEmpty()) {
                                PackageConfigItem item = (PackageConfigItem) this.mAwareProtectMap.get(pkgName);
                                if (item == null) {
                                    item = new PackageConfigItem(pkgName, value);
                                    this.mAwareProtectMap.put(pkgName, item);
                                    if (DEBUG) {
                                        String str2 = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("pkgName:");
                                        stringBuilder.append(pkgName);
                                        stringBuilder.append(" mGroupId:");
                                        stringBuilder.append(item.mGroupId);
                                        stringBuilder.append(" restart:");
                                        stringBuilder.append(item.mRestartFlag);
                                        stringBuilder.append(" clean:");
                                        stringBuilder.append(item.mResCleanAllow);
                                        stringBuilder.append(" frequently used:");
                                        stringBuilder.append(item.mFrequentlyUsed);
                                        AwareLog.i(str2, stringBuilder.toString());
                                    }
                                }
                                if (processName != null) {
                                    ProcessConfigItem dItem = new ProcessConfigItem(processName, value);
                                    item.add(dItem);
                                    if (DEBUG) {
                                        String str3 = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("processName:");
                                        stringBuilder2.append(processName);
                                        stringBuilder2.append(" mGroupId:");
                                        stringBuilder2.append(dItem.mGroupId);
                                        stringBuilder2.append(" restart:");
                                        stringBuilder2.append(dItem.mRestartFlag);
                                        stringBuilder2.append(" clean:");
                                        stringBuilder2.append(dItem.mResCleanAllow);
                                        stringBuilder2.append(" frequently used:");
                                        stringBuilder2.append(item.mFrequentlyUsed);
                                        AwareLog.i(str3, stringBuilder2.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void startHabitObserver() {
        setHabitWhiteList();
        AwareUserHabit habitInstance = AwareUserHabit.getInstance();
        if (habitInstance != null) {
            habitInstance.registHabitProtectListChangeListener(this.mHabitListener);
        }
    }

    private void startXmlObserver() {
        HwSysResManager.getInstance().registerResourceCallback(this.mUpdateWhiteListCallback);
    }

    public Map<String, PackageConfigItem> getAwareProtectMap() {
        if (!this.mEnabled) {
            return null;
        }
        ArrayMap<String, PackageConfigItem> map = new ArrayMap();
        setStaticXmlWhiteList();
        synchronized (this) {
            map.putAll(this.mAwareProtectMap);
        }
        return map;
    }

    public List<String> getKeyHabitAppList() {
        List<String> list = new ArrayList();
        if (!this.mEnabled) {
            return list;
        }
        synchronized (this) {
            list.addAll(this.mKeyHabitAppList);
        }
        return list;
    }

    public List<String> getAllHabitAppList() {
        List<String> list = new ArrayList();
        if (!this.mEnabled) {
            return list;
        }
        synchronized (this) {
            list.addAll(this.mAllHabitAppList);
        }
        return list;
    }

    public void updateKeyImCache(List<String> list) {
        if (list != null) {
            synchronized (this) {
                this.mKeyHabitAppList.clear();
                int maxCnt = 0;
                for (String pkgName : list) {
                    if (pkgName != null) {
                        this.mKeyHabitAppList.add(pkgName);
                        maxCnt++;
                        if (maxCnt >= AppMngConfig.getImCnt()) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public void updateAllImCache(List<String> list) {
        if (list != null) {
            synchronized (this) {
                this.mAllHabitAppList.clear();
                for (String pkgName : list) {
                    if (pkgName != null) {
                        this.mAllHabitAppList.add(pkgName);
                    }
                }
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw) {
        if (pw != null) {
            if (this.mEnabled) {
                Iterator it;
                Iterator it2;
                int size;
                int i;
                pw.println("dump iAware Protect WhiteList Apps start --------");
                synchronized (this) {
                    it = this.mAwareProtectList.iterator();
                    while (it.hasNext()) {
                        pw.println((String) it.next());
                    }
                }
                pw.println("dump iAware Protect WhiteList Apps end-----------");
                pw.println("dump iAware Adjust Adj Apps start --------");
                synchronized (this.mAdjustAdjList) {
                    it2 = this.mAdjustAdjList.iterator();
                    while (it2.hasNext()) {
                        pw.println((String) it2.next());
                    }
                }
                pw.println("dump iAware Adjust Adj Apps end-----------");
                pw.println("dump User Habit Frequent Used start-----------");
                synchronized (this.mHabitFrequentUsed) {
                    it = this.mHabitFrequentUsed.iterator();
                    while (it.hasNext()) {
                        pw.println((String) it.next());
                    }
                }
                pw.println("dump User Habit Frequent Used end-----------");
                pw.println("dump User Habit WhiteList Apps start ------------");
                synchronized (this) {
                    size = this.mKeyHabitAppList.size();
                    for (i = 0; i < size; i++) {
                        pw.println((String) this.mKeyHabitAppList.get(i));
                    }
                }
                pw.println("dump User Habit WhiteList Apps end --------------");
                pw.println("dump User All Habit WhiteList Apps start ------------");
                synchronized (this) {
                    size = this.mAllHabitAppList.size();
                    for (i = 0; i < size; i++) {
                        pw.println((String) this.mAllHabitAppList.get(i));
                    }
                }
                pw.println("dump User All Habit WhiteList Apps end --------------");
                pw.println("dump iAware Restart Apps start --------");
                synchronized (this.mRestartAppList) {
                    it2 = this.mRestartAppList.iterator();
                    while (it2.hasNext()) {
                        pw.println((String) it2.next());
                    }
                }
                pw.println("dump iAware Restart Apps end-----------");
                pw.println("dump iAware Bad Apps start --------");
                synchronized (this.mBadAppList) {
                    it = this.mBadAppList.iterator();
                    while (it.hasNext()) {
                        pw.println((String) it.next());
                    }
                }
                pw.println("dump iAware Bad Apps end-----------");
                pw.println("dump AppMng Config start ------------");
                dumpCfg(pw);
                pw.println("dump AppMng Configs end ------------");
                return;
            }
            pw.println("WhiteList feature not enabled.");
        }
    }

    private void dumpCfg(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("memMB:");
        stringBuilder.append(AppMngConfig.getMemorySize());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("topN:");
        stringBuilder.append(AppMngConfig.getTopN());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("imCnt:");
        stringBuilder.append(AppMngConfig.getImCnt());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("sysDecay:");
        stringBuilder.append(AppMngConfig.getSysDecay());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("keySysDecay:");
        stringBuilder.append(AppMngConfig.getKeySysDecay());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("adjCustTopN:");
        stringBuilder.append(AppMngConfig.getAdjCustTopN());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("restart:");
        stringBuilder.append(AppMngConfig.getRestartFlag());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("abroad:");
        stringBuilder.append(AppMngConfig.getAbroadFlag());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("bgDecayMinute:");
        stringBuilder.append(AppMngConfig.getBgDecay());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("pgProtectEn:");
        stringBuilder.append(AppMngConfig.getPgProtectFlag());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("alarmChk:");
        stringBuilder.append(AppMngConfig.getAlarmCheckFlag());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("killMore:");
        stringBuilder.append(AppMngConfig.getKillMoreFlag());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("lowEnd:");
        stringBuilder.append(this.mLowEnd);
        pw.println(stringBuilder.toString());
    }

    public void fillMostFrequentUsedApp(List<String> list) {
        if (this.mEnabled) {
            List<String> listHabit = new ArrayList();
            if (list != null) {
                listHabit.addAll(list);
            }
            List<String> setAdjPkg = new ArrayList();
            setAdjPkg.addAll(listHabit);
            synchronized (this.mAdjustAdjList) {
                setAdjPkg.addAll(this.mAdjustAdjList);
            }
            HwActivityManager.setAndRestoreMaxAdjIfNeed(setAdjPkg);
            synchronized (this.mHabitFrequentUsed) {
                this.mHabitFrequentUsed.clear();
                this.mHabitFrequentUsed.addAll(listHabit);
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0018, code skipped:
            r2 = r4.mHabitFrequentUsed;
     */
    /* JADX WARNING: Missing block: B:14:0x001a, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:17:0x0021, code skipped:
            if (r4.mHabitFrequentUsed.contains(r5) == false) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:18:0x0023, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:19:0x0024, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:20:0x0025, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:21:0x0026, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isAppMngOomAdjCustomized(String pkg) {
        if (!this.mEnabled || pkg == null) {
            return false;
        }
        synchronized (this.mAdjustAdjList) {
            if (this.mAdjustAdjList.contains(pkg)) {
                return true;
            }
        }
    }

    public Set<String> getRestartAppList() {
        ArraySet arraySet;
        synchronized (this.mRestartAppList) {
            arraySet = new ArraySet(this.mRestartAppList);
        }
        return arraySet;
    }

    public Set<String> getBadAppList() {
        ArraySet arraySet;
        synchronized (this.mBadAppList) {
            arraySet = new ArraySet(this.mBadAppList);
        }
        return arraySet;
    }

    public static boolean isAbroadArea() {
        return SystemProperties.get("ro.config.hw_optb", "0").equals("156") ^ 1;
    }
}
