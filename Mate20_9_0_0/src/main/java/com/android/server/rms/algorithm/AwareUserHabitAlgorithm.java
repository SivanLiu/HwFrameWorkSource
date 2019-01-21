package com.android.server.rms.algorithm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LruCache;
import com.android.internal.os.BackgroundThread;
import com.android.server.gesture.GestureNavConst;
import com.android.server.rms.algorithm.utils.IAwareHabitUtils;
import com.android.server.rms.algorithm.utils.IAwareHabitUtils.UsageDistribution;
import com.android.server.rms.algorithm.utils.ProtectApp;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AwareUserHabitAlgorithm {
    private static final double DECREASE_RATE_PER_TRAINING = 0.9d;
    private static final String LRU = "lru";
    private static final int LRU_APP_NUM_LIMITED = 2;
    private static final long LRU_TIME_LIMIT = 1800000;
    private static final int MATCHTYPE_LRU = 2;
    private static final int MATCHTYPE_NONE_LRU = 3;
    private static final int MATCHTYPE_TRANS = 1;
    private static final int MAX_DYNAMIC_TOPN_INDEX_VER1 = 6;
    private static final long MIN_THRESHOLD = -31536000000L;
    private static final String NONE_LRU = "nonelru";
    private static final long STAY_IN_BACKGROUND_LIMIT_TIME = 86400000;
    private static final String TAG = "AwareUserHabitAlgorithm";
    private static final String TRANS = "trans";
    private static final int TRANS_APP_NUM_LIMITED = 2;
    private static final double TRANS_PRO_THRESHOLD = 0.3d;
    private Map<Integer, ArrayList<String>> mAppTypeInfo = new ArrayMap();
    private ContentResolver mContentResolver = null;
    private Context mContext = null;
    private final ArrayMap<String, Long> mGCMAppsArrayMap = new ArrayMap();
    private final Object mHabitLock = new Object();
    private final List<ProtectApp> mHabitProtectAppsList = new ArrayList();
    private final ArrayMap<String, Long> mHabitProtectArrayMap = new ArrayMap();
    private final ArrayList<String> mHabitProtectListTopN = new ArrayList();
    private LinkedHashMap<Integer, String> mIdToPkgNameMap = new LinkedHashMap();
    private ArraySet<String> mInterfaceFilterAppSet = new ArraySet();
    private final ArraySet<HabitProtectListChangeListener> mListeners = new ArraySet();
    private LruCache<String, Long> mLruCache = new LruCache(500);
    private ArraySet<String> mNoInterfaceFilterAppSet = new ArraySet();
    private LinkedHashMap<String, Integer> mPkgNameToIdMap = new LinkedHashMap();
    private ArrayList<ArrayList<Integer>> mTransProMatrix = new ArrayList();
    private LinkedHashMap<String, Integer> mUsageCount = new LinkedHashMap();
    private Map<String, UsageDistribution> mUsageDistributionMap = new ArrayMap();
    private final AtomicInteger mUserId = new AtomicInteger(0);
    private LinkedHashMap<String, Integer> mUserTrackList = new LinkedHashMap();

    private static class AppCountDescComparator implements Comparator<Entry<String, Integer>>, Serializable {
        private static final long serialVersionUID = 1;

        private AppCountDescComparator() {
        }

        /* synthetic */ AppCountDescComparator(AnonymousClass1 x0) {
            this();
        }

        public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
            return ((Integer) o2.getValue()).compareTo((Integer) o1.getValue());
        }
    }

    private static class DoubleAscComparator implements Comparator<Entry<Integer, Double>>, Serializable {
        private static final long serialVersionUID = 1;

        private DoubleAscComparator() {
        }

        /* synthetic */ DoubleAscComparator(AnonymousClass1 x0) {
            this();
        }

        public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
            return ((Double) o1.getValue()).compareTo((Double) o2.getValue());
        }
    }

    public interface HabitProtectListChangeListener {
        void onListChanged();
    }

    private static class IntDescComparator implements Comparator<Entry<Integer, Integer>>, Serializable {
        private static final long serialVersionUID = 1;

        private IntDescComparator() {
        }

        /* synthetic */ IntDescComparator(AnonymousClass1 x0) {
            this();
        }

        public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
            return ((Integer) o2.getValue()).compareTo((Integer) o1.getValue());
        }
    }

    static class PkgInfo {
        private int hitType;
        private int id;

        public PkgInfo(int index, int type) {
            this.id = index;
            this.hitType = type;
        }

        public int getId() {
            return this.id;
        }

        public int getHitType() {
            return this.hitType;
        }
    }

    public AwareUserHabitAlgorithm(Context ctx) {
        if (ctx != null) {
            this.mContext = ctx;
            this.mContentResolver = this.mContext.getContentResolver();
        }
    }

    public void deinit() {
        clearHabitProtectList();
        synchronized (this.mHabitLock) {
            clearData();
        }
        synchronized (this.mAppTypeInfo) {
            this.mAppTypeInfo.clear();
        }
    }

    private void initFilterPkg() {
        addDefaultApp();
        addHomeApp();
        addRemovedApp();
    }

    void reloadFilterPkg() {
        clearFilterPkg();
        initFilterPkg();
    }

    void updateAppUsage(String pkg) {
        int timeType = IAwareHabitUtils.getTimeType(System.currentTimeMillis());
        synchronized (this.mHabitLock) {
            if (this.mUsageCount.containsKey(pkg)) {
                this.mUsageCount.put(pkg, Integer.valueOf(((Integer) this.mUsageCount.get(pkg)).intValue() + 1));
            } else {
                this.mUsageCount.put(pkg, Integer.valueOf(1));
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAppUsage, pkg:");
            stringBuilder.append(pkg);
            stringBuilder.append(", usage total:");
            stringBuilder.append(this.mUsageCount.get(pkg));
            AwareLog.d(str, stringBuilder.toString());
            UsageDistribution historyDis = (UsageDistribution) this.mUsageDistributionMap.get(pkg);
            if (historyDis != null) {
                if (timeType == 0) {
                    historyDis.mDay++;
                } else {
                    historyDis.mNight++;
                }
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateAppUsage, pkg:");
                stringBuilder.append(pkg);
                stringBuilder.append(" cur distribution day:");
                stringBuilder.append(historyDis.mDay);
                stringBuilder.append(" night:");
                stringBuilder.append(historyDis.mNight);
                AwareLog.d(str2, stringBuilder.toString());
            } else {
                UsageDistribution usageDistribution;
                if (timeType == 0) {
                    usageDistribution = new UsageDistribution(1, 0);
                } else {
                    usageDistribution = new UsageDistribution(0, 1);
                }
                this.mUsageDistributionMap.put(pkg, usageDistribution);
                String str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateAppUsage, pkg:");
                stringBuilder2.append(pkg);
                stringBuilder2.append(" new distribution day:");
                stringBuilder2.append(usageDistribution.mDay);
                stringBuilder2.append(" night:");
                stringBuilder2.append(usageDistribution.mNight);
                AwareLog.d(str3, stringBuilder2.toString());
            }
        }
        int appType = AppTypeRecoManager.getInstance().convertType(AppTypeRecoManager.getInstance().getAppType(pkg));
        if (-1 != appType) {
            synchronized (this.mAppTypeInfo) {
                ArrayList<String> list = (ArrayList) this.mAppTypeInfo.get(Integer.valueOf(appType));
                if (list == null) {
                    list = new ArrayList();
                    list.add(pkg);
                    this.mAppTypeInfo.put(Integer.valueOf(appType), list);
                } else if (!list.contains(pkg)) {
                    list.add(pkg);
                }
            }
        }
    }

    void sortUsageCount() {
        loadAppTypeInfo();
    }

    void addFilterPkg(String pkgName) {
        synchronized (this.mNoInterfaceFilterAppSet) {
            this.mNoInterfaceFilterAppSet.add(pkgName);
        }
    }

    private void addDefaultApp() {
        boolean isAdded;
        ArraySet<String> appfilterSet1 = new ArraySet();
        synchronized (this.mInterfaceFilterAppSet) {
            isAdded = this.mInterfaceFilterAppSet.isEmpty() ^ 1;
        }
        ArraySet<String> appfilterSet2 = null;
        if (!isAdded) {
            appfilterSet2 = new ArraySet();
        }
        IAwareHabitUtils.getHabitFilterListFromCMS(appfilterSet1, appfilterSet2);
        if (!appfilterSet1.isEmpty()) {
            synchronized (this.mNoInterfaceFilterAppSet) {
                this.mNoInterfaceFilterAppSet.addAll(appfilterSet1);
            }
        }
        if (appfilterSet2 != null && !appfilterSet2.isEmpty()) {
            synchronized (this.mInterfaceFilterAppSet) {
                this.mInterfaceFilterAppSet.addAll(appfilterSet2);
            }
        }
    }

    private void addRemovedApp() {
        Set<String> set = IAwareHabitUtils.loadRemovedPkg(this.mContentResolver, this.mUserId.get());
        if (set != null) {
            synchronized (this.mNoInterfaceFilterAppSet) {
                this.mNoInterfaceFilterAppSet.addAll(set);
            }
        }
    }

    private void addHomeApp() {
        if (this.mContext != null) {
            PackageManager pm = this.mContext.getPackageManager();
            if (pm != null) {
                int i = 0;
                List<ResolveInfo> applist = pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT"), 0);
                if (applist != null) {
                    while (i < applist.size()) {
                        String homePkg = ((ResolveInfo) applist.get(i)).activityInfo.packageName;
                        if (homePkg != null) {
                            addFilterPkg(homePkg);
                        }
                        i++;
                    }
                }
            }
        }
    }

    ArraySet<String> getFilterApp() {
        ArraySet<String> filter = new ArraySet();
        synchronized (this.mNoInterfaceFilterAppSet) {
            filter.addAll(this.mNoInterfaceFilterAppSet);
        }
        return filter;
    }

    protected void removePkgFromLru(String pkgName) {
        synchronized (this.mLruCache) {
            this.mLruCache.remove(pkgName);
        }
    }

    protected void addPkgToLru(String pkgName, long time) {
        synchronized (this.mLruCache) {
            this.mLruCache.put(pkgName, Long.valueOf(time));
        }
    }

    protected LinkedHashMap<String, Long> getLruCache() {
        LinkedHashMap<String, Long> lru = null;
        synchronized (this.mLruCache) {
            Map<String, Long> tmp = this.mLruCache.snapshot();
            if (tmp instanceof LinkedHashMap) {
                lru = (LinkedHashMap) tmp;
            }
        }
        return lru;
    }

    void clearLruCache() {
        synchronized (this.mLruCache) {
            this.mLruCache.evictAll();
        }
    }

    protected List<String> getAppPkgNamesFromLRU() {
        LinkedHashMap<String, Long> lru = getLruCache();
        if (lru != null) {
            return new ArrayList(lru.keySet());
        }
        return null;
    }

    protected List<String> getForceProtectAppsFromLRU(List<String> homePkg, int lruCount) {
        if (homePkg == null) {
            return null;
        }
        List<String> lruList = getAppPkgNamesFromLRU();
        if (lruList == null) {
            return null;
        }
        List<String> result = new ArrayList();
        int lruTmpCount = 0;
        for (int i = lruList.size() - 1; i >= 0; i--) {
            String pkg = (String) lruList.get(i);
            if (lruCount == lruTmpCount) {
                break;
            }
            if (!(homePkg.contains(pkg) || containsFilterPkg(pkg) || containsFilter2Pkg(pkg))) {
                result.add(pkg);
                lruTmpCount++;
            }
        }
        return result;
    }

    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean isPredictHit(String actualUsedPkgName, int topN) {
        synchronized (this.mUserTrackList) {
            if (!this.mUserTrackList.containsKey(actualUsedPkgName) || ((Integer) this.mUserTrackList.get(actualUsedPkgName)).intValue() > topN) {
            } else {
                return true;
            }
        }
    }

    private void clearFilterPkg() {
        synchronized (this.mNoInterfaceFilterAppSet) {
            this.mNoInterfaceFilterAppSet.clear();
        }
    }

    void removeFilterPkg(String pkgName) {
        synchronized (this.mNoInterfaceFilterAppSet) {
            this.mNoInterfaceFilterAppSet.remove(pkgName);
        }
    }

    boolean containsFilterPkg(String pkg) {
        boolean contains;
        synchronized (this.mNoInterfaceFilterAppSet) {
            contains = this.mNoInterfaceFilterAppSet.contains(pkg);
        }
        return contains;
    }

    boolean containsFilter2Pkg(String pkg) {
        boolean contains;
        synchronized (this.mInterfaceFilterAppSet) {
            contains = this.mInterfaceFilterAppSet.contains(pkg);
        }
        return contains;
    }

    void init() {
        synchronized (this.mHabitLock) {
            this.mUsageCount.clear();
            this.mUsageDistributionMap.clear();
            loadData();
        }
        loadAppTypeInfo();
    }

    void reloadDataInfo() {
        synchronized (this.mHabitLock) {
            clearData();
            loadData();
        }
        loadAppTypeInfo();
    }

    private void clearData() {
        this.mPkgNameToIdMap.clear();
        this.mIdToPkgNameMap.clear();
        this.mTransProMatrix.clear();
        this.mUsageCount.clear();
        this.mUsageDistributionMap.clear();
        clearFilterPkg();
    }

    private void loadData() {
        int userId = this.mUserId.get();
        IAwareHabitUtils.loadPkgInfo(this.mContentResolver, this.mPkgNameToIdMap, this.mIdToPkgNameMap, this.mUsageCount, this.mUsageDistributionMap, userId);
        IAwareHabitUtils.loadAppAssociateInfo(this.mContentResolver, this.mPkgNameToIdMap, this.mTransProMatrix, userId);
        initFilterPkg();
    }

    private void loadAppTypeInfo() {
        Map<Integer, ArrayList<String>> tmpAppTypeInfo = new ArrayMap();
        synchronized (this.mHabitLock) {
            if (this.mUsageCount.size() > 0) {
                List<Entry<String, Integer>> entries = new ArrayList(this.mUsageCount.entrySet());
                Collections.sort(entries, new AppCountDescComparator());
                for (Entry<String, Integer> entry : entries) {
                    String name = (String) entry.getKey();
                    int appType = AppTypeRecoManager.getInstance().convertType(AppTypeRecoManager.getInstance().getAppType(name));
                    if (appType != -1) {
                        ArrayList<String> list = (ArrayList) tmpAppTypeInfo.get(Integer.valueOf(appType));
                        if (list == null) {
                            list = new ArrayList();
                            list.add(name);
                            tmpAppTypeInfo.put(Integer.valueOf(appType), list);
                        } else {
                            list.add(name);
                        }
                    }
                }
            }
        }
        synchronized (this.mAppTypeInfo) {
            this.mAppTypeInfo.clear();
            this.mAppTypeInfo.putAll(tmpAppTypeInfo);
        }
    }

    void updatePkgNameMap(String pkgName) {
        if (!this.mPkgNameToIdMap.containsKey(pkgName)) {
            int size = this.mPkgNameToIdMap.size();
            this.mPkgNameToIdMap.put(pkgName, Integer.valueOf(size));
            this.mIdToPkgNameMap.put(Integer.valueOf(size), pkgName);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:0x0073 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    List<String> getMostFreqAppByType(int appType, int appNum) {
        if (appNum <= 0) {
            return null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getMostFreqAppByType appType:");
        stringBuilder.append(appType);
        stringBuilder.append(" appNum:");
        stringBuilder.append(appNum);
        AwareLog.d(str, stringBuilder.toString());
        long startTime = System.currentTimeMillis();
        int num = appNum;
        synchronized (this.mAppTypeInfo) {
            if (this.mAppTypeInfo.isEmpty()) {
                return null;
            }
            List<String> appList = (List) this.mAppTypeInfo.get(Integer.valueOf(appType));
            if (appList == null) {
                return null;
            }
            ArrayList<String> result = new ArrayList();
            for (String pkgName : appList) {
                if (num <= 0) {
                    break;
                }
                boolean isFilted;
                if (!containsFilterPkg(pkgName)) {
                    if (!containsFilter2Pkg(pkgName)) {
                        isFilted = false;
                        if (isFilted) {
                            result.add(pkgName);
                            num--;
                        }
                    }
                }
                isFilted = true;
                if (isFilted) {
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getMostFreqAppByType spend time ");
            stringBuilder2.append(System.currentTimeMillis() - startTime);
            stringBuilder2.append("ms");
            AwareLog.d(str2, stringBuilder2.toString());
            return result;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x006f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x006a  */
    /* JADX WARNING: Missing block: B:33:0x0071, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    List<String> getMostFrequentUsedApp(int n, int minCount, Set<String> filterAppSet) {
        if (n <= 0) {
            return null;
        }
        ArrayList<String> result = new ArrayList();
        int num = n;
        synchronized (this.mHabitLock) {
            if (this.mUsageCount.isEmpty()) {
                return result;
            }
            List<Entry<String, Integer>> entries = new ArrayList(this.mUsageCount.entrySet());
            Collections.sort(entries, new AppCountDescComparator());
            for (Entry<String, Integer> entry : entries) {
                if (num > 0) {
                    if (((Integer) entry.getValue()).intValue() > minCount) {
                        boolean isFilted;
                        String pkgName = (String) entry.getKey();
                        int i = 0;
                        if (!containsFilterPkg(pkgName)) {
                            if (!containsFilter2Pkg(pkgName)) {
                                isFilted = false;
                                if (filterAppSet == null) {
                                    i = filterAppSet.contains(pkgName);
                                }
                                if (isFilted | i) {
                                    result.add(pkgName);
                                    num--;
                                }
                            }
                        }
                        isFilted = true;
                        if (filterAppSet == null) {
                        }
                        if (isFilted | i) {
                        }
                    }
                }
            }
            return result;
        }
    }

    private List<PkgInfo> getTopMove(String lastPkgName, Set<Integer> topMoveSet) {
        String str = lastPkgName;
        ArrayList<PkgInfo> topMoveList = new ArrayList();
        Set<Integer> set;
        if (!this.mPkgNameToIdMap.containsKey(str) || ((Integer) this.mPkgNameToIdMap.get(str)).intValue() >= this.mTransProMatrix.size()) {
            set = topMoveSet;
            return topMoveList;
        }
        int i;
        int id = ((Integer) this.mPkgNameToIdMap.get(str)).intValue();
        ArrayList<Integer> appmovelist = (ArrayList) this.mTransProMatrix.get(id);
        ArrayList<Entry<Integer, Integer>> slist = new ArrayList();
        double sum = 0.0d;
        for (i = 0; i < appmovelist.size(); i++) {
            if (i != id) {
                sum += (double) ((Integer) appmovelist.get(i)).intValue();
                slist.add(new SimpleEntry(Integer.valueOf(i), appmovelist.get(i)));
            }
        }
        Collections.sort(slist, new IntDescComparator());
        i = 0;
        Iterator<Entry<Integer, Integer>> it = slist.iterator();
        while (it.hasNext()) {
            Entry<Integer, Integer> entry = (Entry) it.next();
            if (((double) ((Integer) entry.getValue()).intValue()) / sum < TRANS_PRO_THRESHOLD || i >= 2) {
                break;
            }
            topMoveList.add(new PkgInfo(((Integer) entry.getKey()).intValue(), 1));
            topMoveSet.add(entry.getKey());
            i++;
        }
        set = topMoveSet;
        return topMoveList;
    }

    private List<Integer> getLRUAppList(Map<String, Long> lruCache, long curTime, String realCurapp, Set<Integer> topMoveSet) {
        List<Integer> lrulist = new ArrayList();
        List<String> sortedlru = getAppPkgNamesFromLRU();
        if (sortedlru == null) {
            return lrulist;
        }
        int lruMaxNums = 2;
        if (this.mUsageCount.isEmpty()) {
            lruMaxNums = sortedlru.size();
        }
        for (int i = sortedlru.size() - 1; i >= 0; i--) {
            String lruappname = (String) sortedlru.get(i);
            if (!(lruappname == null || lruappname.equals(realCurapp) || containsFilterPkg(lruappname) || topMoveSet.contains(this.mPkgNameToIdMap.get(lruappname)) || !this.mPkgNameToIdMap.containsKey(lruappname) || !lruCache.containsKey(lruappname))) {
                if (Math.abs(curTime - ((Long) lruCache.get(lruappname)).longValue()) >= 1800000 || lrulist.size() >= lruMaxNums) {
                    break;
                }
                lrulist.add(this.mPkgNameToIdMap.get(lruappname));
            }
        }
        return lrulist;
    }

    private List<PkgInfo> getUsePatternList(String lastPkgName, Map<String, Long> lruCache, long curTime, String realCurapp) {
        int distributionType;
        int realCurappID;
        AwareUserHabitAlgorithm awareUserHabitAlgorithm = this;
        Map<String, Long> map = lruCache;
        String str = realCurapp;
        ArraySet<Integer> topMoveSet = new ArraySet();
        List<PkgInfo> topList = awareUserHabitAlgorithm.getTopMove(lastPkgName, topMoveSet);
        ArrayList<PkgInfo> patternList = new ArrayList();
        patternList.addAll(topList);
        List<Integer> lrulist = awareUserHabitAlgorithm.getLRUAppList(map, curTime, str, topMoveSet);
        ArrayList<Entry<Integer, Double>> notinlrulist = new ArrayList();
        int distributionType2 = IAwareHabitUtils.getTimeType(System.currentTimeMillis());
        for (Integer appID : lrulist) {
            patternList.add(new PkgInfo(appID.intValue(), 2));
        }
        int realCurappID2 = awareUserHabitAlgorithm.mPkgNameToIdMap.size();
        if (awareUserHabitAlgorithm.mPkgNameToIdMap.containsKey(str)) {
            realCurappID2 = ((Integer) awareUserHabitAlgorithm.mPkgNameToIdMap.get(str)).intValue();
        }
        int i = 0;
        while (i < awareUserHabitAlgorithm.mPkgNameToIdMap.size()) {
            List<Integer> lrulist2;
            if (topMoveSet.contains(Integer.valueOf(i)) || lrulist.contains(Integer.valueOf(i)) || i == realCurappID2) {
                lrulist2 = lrulist;
                distributionType = distributionType2;
                realCurappID = realCurappID2;
            } else {
                String name = (String) awareUserHabitAlgorithm.mIdToPkgNameMap.get(Integer.valueOf(i));
                UsageDistribution curDistribution = (UsageDistribution) awareUserHabitAlgorithm.mUsageDistributionMap.get(name);
                int curUsage = 0;
                if (curDistribution != null) {
                    curUsage = distributionType2 == 0 ? curDistribution.mDay : curDistribution.mNight;
                }
                if (curUsage != 0) {
                    lrulist2 = lrulist;
                    distributionType = distributionType2;
                    realCurappID = realCurappID2;
                    notinlrulist.add(new SimpleEntry(Integer.valueOf(i), Double.valueOf(0.0d - ((double) curUsage))));
                } else {
                    lrulist2 = lrulist;
                    distributionType = distributionType2;
                    realCurappID = realCurappID2;
                    long lastTime = MIN_THRESHOLD;
                    if (map.containsKey(name)) {
                        lastTime = ((Long) map.get(name)).longValue();
                    }
                    notinlrulist.add(new SimpleEntry(Integer.valueOf(i), Double.valueOf((double) Math.abs(curTime - lastTime))));
                }
            }
            i++;
            lrulist = lrulist2;
            distributionType2 = distributionType;
            realCurappID2 = realCurappID;
            awareUserHabitAlgorithm = this;
        }
        distributionType = distributionType2;
        realCurappID = realCurappID2;
        Collections.sort(notinlrulist, new DoubleAscComparator());
        Iterator<Entry<Integer, Double>> it = notinlrulist.iterator();
        while (it.hasNext()) {
            patternList.add(new PkgInfo(((Integer) ((Entry) it.next()).getKey()).intValue(), 3));
        }
        return patternList;
    }

    protected Map<String, String> getUserTrackPredictDumpInfo(String lastPkgName, Map<String, Long> lruCache, long curTime, String realCurapp) {
        LinkedHashMap<String, String> userTrackList = null;
        synchronized (this.mHabitLock) {
            if (!(lastPkgName == null || realCurapp == null || lruCache == null)) {
                List<PkgInfo> dumpList = getUsePatternList(lastPkgName, lruCache, curTime, realCurapp);
                userTrackList = new LinkedHashMap();
                for (PkgInfo info : dumpList) {
                    String appName = (String) this.mIdToPkgNameMap.get(Integer.valueOf(info.getId()));
                    if (!(appName == null || containsFilterPkg(appName))) {
                        if (1 == info.getHitType()) {
                            userTrackList.put(appName, TRANS);
                        } else if (2 == info.getHitType()) {
                            userTrackList.put(appName, LRU);
                        } else {
                            userTrackList.put(appName, NONE_LRU);
                        }
                    }
                }
            }
        }
        return userTrackList;
    }

    void triggerUserTrackPredict(String lastPkgName, Map<String, Long> lruCache, long curTime, String realCurapp) {
        if (lastPkgName != null && realCurapp != null && lruCache != null) {
            synchronized (this.mHabitLock) {
                predict(lastPkgName, lruCache, curTime, realCurapp);
            }
        }
    }

    private void predict(String lastPkgName, Map<String, Long> lruCache, long curTime, String realCurapp) {
        updatePkgNameMap(realCurapp);
        List<PkgInfo> numList = getUsePatternList(lastPkgName, lruCache, curTime, realCurapp);
        int order = 1;
        synchronized (this.mUserTrackList) {
            int i;
            this.mUserTrackList.clear();
            ArrayList<String> filterAppList = new ArrayList();
            Iterator it = numList.iterator();
            while (true) {
                i = 0;
                if (!it.hasNext()) {
                    break;
                }
                String appName = (String) this.mIdToPkgNameMap.get(Integer.valueOf(((PkgInfo) it.next()).getId()));
                if (!(appName == null || containsFilterPkg(appName))) {
                    if (order < 6 && containsFilter2Pkg(appName)) {
                        filterAppList.add(appName);
                    } else if (order != 6 || filterAppList.isEmpty()) {
                        this.mUserTrackList.put(appName, Integer.valueOf(order));
                        order++;
                    } else {
                        while (i < filterAppList.size()) {
                            this.mUserTrackList.put(filterAppList.get(i), Integer.valueOf(order));
                            order++;
                            i++;
                        }
                    }
                }
            }
            if (this.mUserTrackList.size() < 6 && !filterAppList.isEmpty()) {
                while (true) {
                    int i2 = i;
                    if (i2 >= filterAppList.size()) {
                        break;
                    }
                    this.mUserTrackList.put(filterAppList.get(i2), Integer.valueOf(order));
                    order++;
                    i = i2 + 1;
                }
            }
        }
    }

    protected Map<String, Integer> getUserTrackList() {
        LinkedHashMap<String, Integer> result = new LinkedHashMap();
        synchronized (this.mUserTrackList) {
            result.putAll(this.mUserTrackList);
        }
        return result;
    }

    List<String> getTopN(int n) {
        List<String> result = new ArrayList();
        Iterator<Entry<String, Integer>> iterator = getUserTrackList().entrySet().iterator();
        int num = n;
        while (iterator.hasNext() && num != 0) {
            result.add(((Entry) iterator.next()).getKey());
            num--;
        }
        return result;
    }

    protected Map<String, Long> getLongTimePkgsFromLru(long bgtime) {
        LinkedHashMap<String, Long> lru = getLruCache();
        if (lru == null) {
            return null;
        }
        Map<String, Long> lruPkgList = new ArrayMap();
        long now = SystemClock.elapsedRealtime();
        for (Entry entry : lru.entrySet()) {
            String pkg = (String) entry.getKey();
            long time = ((Long) entry.getValue()).longValue();
            if (now - time >= bgtime) {
                lruPkgList.put(pkg, Long.valueOf(time));
            }
        }
        return lruPkgList;
    }

    protected String getLastPkgNameExcludeLauncher(String curApp, List<String> homePkg) {
        if (curApp == null || homePkg == null) {
            return null;
        }
        List<String> infoPkg = getAppPkgNamesFromLRU();
        if (infoPkg == null) {
            return null;
        }
        String lastPkgName = null;
        for (int i = infoPkg.size() - 1; i >= 0; i--) {
            String pkg = (String) infoPkg.get(i);
            if (!homePkg.contains(pkg) && !pkg.equals(curApp)) {
                lastPkgName = pkg;
                break;
            }
        }
        return lastPkgName;
    }

    void initHabitProtectList() {
        synchronized (this.mHabitProtectAppsList) {
            this.mHabitProtectAppsList.clear();
            IAwareHabitUtils.loadHabitProtectList(this.mContentResolver, this.mHabitProtectAppsList, this.mUserId.get());
        }
        List<String> topN = getHabitProtectAppList(1, 3, null, true);
        synchronized (this.mHabitProtectListTopN) {
            this.mHabitProtectListTopN.clear();
            this.mHabitProtectListTopN.addAll(topN);
        }
    }

    private void clearHabitProtectList() {
        synchronized (this.mHabitProtectListTopN) {
            this.mHabitProtectListTopN.clear();
        }
        synchronized (this.mHabitProtectAppsList) {
            this.mHabitProtectAppsList.clear();
        }
    }

    protected List<String> queryHabitProtectAppList(int imCount, int emailCount) {
        List<String> result = new ArrayList();
        int curImCount = 0;
        int curEmailCount = 0;
        synchronized (this.mHabitProtectListTopN) {
            Iterator it = this.mHabitProtectListTopN.iterator();
            while (it.hasNext()) {
                String pkgName = (String) it.next();
                int appType = AppTypeRecoManager.getInstance().getAppType(pkgName);
                if (appType != 0) {
                    if (appType != MemoryConstant.MSG_SET_PREREAD_PATH) {
                        if ((appType == 1 || appType == MemoryConstant.MSG_DIRECT_SWAPPINESS) && curEmailCount < emailCount) {
                            result.add(pkgName);
                            curEmailCount++;
                        }
                        if (curImCount != imCount && curEmailCount == emailCount) {
                            break;
                        }
                    }
                }
                if (curImCount < imCount) {
                    result.add(pkgName);
                    curImCount++;
                }
                if (curImCount != imCount) {
                }
            }
        }
        return result;
    }

    protected void foregroundUpdateHabitProtectList(String pkgName) {
        boolean isFirstUsed = true;
        boolean isReInstallApp = false;
        synchronized (this.mHabitProtectAppsList) {
            for (ProtectApp app : this.mHabitProtectAppsList) {
                if (app.getAppPkgName().equals(pkgName)) {
                    isFirstUsed = false;
                    if (app.getDeletedTag() == 1) {
                        app.setDeletedTag(0);
                        isReInstallApp = true;
                    }
                }
            }
        }
        if (isFirstUsed) {
            int appType = AppTypeRecoManager.getInstance().getAppType(pkgName);
            if (appType == 0 || appType == MemoryConstant.MSG_SET_PREREAD_PATH) {
                appType = 0;
            } else if (appType == 1 || appType == MemoryConstant.MSG_DIRECT_SWAPPINESS) {
                appType = 1;
            } else {
                return;
            }
            synchronized (this.mHabitProtectAppsList) {
                this.mHabitProtectAppsList.add(new ProtectApp(pkgName, appType, 0, (float) GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO));
            }
        }
        if (isFirstUsed || isReInstallApp) {
            synchronized (this.mHabitProtectListTopN) {
                if (!this.mHabitProtectListTopN.contains(pkgName)) {
                    this.mHabitProtectListTopN.add(pkgName);
                }
            }
        }
    }

    protected void backgroundActivityChangedEvent(String pkgName, Long time) {
        if (pkgName != null) {
            int type = AppTypeRecoManager.getInstance().getAppType(pkgName);
            boolean isNeedUpdate = false;
            if (type == 0 || type == 1 || type == MemoryConstant.MSG_SET_PREREAD_PATH || type == MemoryConstant.MSG_DIRECT_SWAPPINESS) {
                synchronized (this.mHabitProtectArrayMap) {
                    if (!this.mHabitProtectArrayMap.containsKey(pkgName)) {
                        isNeedUpdate = true;
                    }
                    this.mHabitProtectArrayMap.put(pkgName, time);
                }
            }
            synchronized (this.mGCMAppsArrayMap) {
                boolean isGcm = this.mGCMAppsArrayMap.containsKey(pkgName);
                if (!isGcm) {
                    isGcm = IAwareHabitUtils.isGCMApp(this.mContext, pkgName);
                    isNeedUpdate |= isGcm;
                }
                if (isGcm) {
                    this.mGCMAppsArrayMap.put(pkgName, time);
                }
            }
            if (isNeedUpdate) {
                updateHabitProtectList();
            }
        }
    }

    protected void trainedUpdateHabitProtectList() {
        boolean isNeedNotifyUpdate = false;
        synchronized (this.mHabitProtectAppsList) {
            this.mHabitProtectAppsList.clear();
            IAwareHabitUtils.loadHabitProtectList(this.mContentResolver, this.mHabitProtectAppsList, this.mUserId.get());
        }
        synchronized (this.mHabitProtectArrayMap) {
            Iterator<Entry<String, Long>> it = this.mHabitProtectArrayMap.entrySet().iterator();
            while (it.hasNext()) {
                if (SystemClock.elapsedRealtime() - ((Long) ((Entry) it.next()).getValue()).longValue() > 86400000) {
                    it.remove();
                    if (!isNeedNotifyUpdate) {
                        isNeedNotifyUpdate = true;
                    }
                }
            }
        }
        synchronized (this.mGCMAppsArrayMap) {
            Iterator<Entry<String, Long>> it2 = this.mGCMAppsArrayMap.entrySet().iterator();
            while (it2.hasNext()) {
                if (SystemClock.elapsedRealtime() - ((Long) ((Entry) it2.next()).getValue()).longValue() > 86400000) {
                    it2.remove();
                    if (!isNeedNotifyUpdate) {
                        isNeedNotifyUpdate = true;
                    }
                }
            }
        }
        List<String> topNList = getHabitProtectAppList(1, 3, null, true);
        synchronized (this.mHabitProtectListTopN) {
            int size = topNList.size();
            boolean changed = false;
            if (size == this.mHabitProtectListTopN.size()) {
                for (int i = 0; i < size; i++) {
                    String dstPkg = (String) this.mHabitProtectListTopN.get(i);
                    String srcPkg = (String) topNList.get(i);
                    if (dstPkg != null && !dstPkg.equals(srcPkg)) {
                        changed = true;
                        break;
                    }
                }
            } else {
                changed = true;
            }
            if (changed) {
                this.mHabitProtectListTopN.clear();
                this.mHabitProtectListTopN.addAll(topNList);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("new habitProtectList topN:");
                stringBuilder.append(this.mHabitProtectListTopN);
                AwareLog.d(str, stringBuilder.toString());
            }
        }
        if (isNeedNotifyUpdate) {
            updateHabitProtectList();
        }
    }

    private void updateHabitProtectList() {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                synchronized (AwareUserHabitAlgorithm.this.mListeners) {
                    if (AwareUserHabitAlgorithm.this.mListeners.isEmpty()) {
                        return;
                    }
                    Iterator it = AwareUserHabitAlgorithm.this.mListeners.iterator();
                    while (it.hasNext()) {
                        ((HabitProtectListChangeListener) it.next()).onListChanged();
                    }
                }
            }
        });
    }

    protected void uninstallUpdateHabitProtectList(String pkgName) {
        boolean isProtectApp = false;
        boolean isNeedNotifyUpdate = false;
        synchronized (this.mHabitProtectAppsList) {
            for (ProtectApp app : this.mHabitProtectAppsList) {
                if (app.getAppPkgName().equals(pkgName)) {
                    isProtectApp = true;
                    if (app.getDeletedTag() == 0) {
                        app.setDeletedTag(1);
                    }
                }
            }
        }
        if (isProtectApp) {
            synchronized (this.mHabitProtectListTopN) {
                if (this.mHabitProtectListTopN.contains(pkgName)) {
                    this.mHabitProtectListTopN.remove(pkgName);
                    List<String> result = getHabitProtectAppList(1, 3, null, true);
                    if (result.size() > 0) {
                        for (int i = 0; i < result.size(); i++) {
                            String pkgNameStr = (String) result.get(i);
                            if (!this.mHabitProtectListTopN.contains(pkgNameStr)) {
                                this.mHabitProtectListTopN.add(pkgNameStr);
                                break;
                            }
                        }
                    }
                }
            }
        }
        synchronized (this.mHabitProtectArrayMap) {
            if (this.mHabitProtectArrayMap.containsKey(pkgName)) {
                Iterator<Entry<String, Long>> it = this.mHabitProtectArrayMap.entrySet().iterator();
                while (it.hasNext()) {
                    if (((String) ((Entry) it.next()).getKey()).equals(pkgName)) {
                        it.remove();
                        isNeedNotifyUpdate = true;
                        break;
                    }
                }
            }
        }
        synchronized (this.mGCMAppsArrayMap) {
            if (this.mGCMAppsArrayMap.containsKey(pkgName)) {
                Iterator<Entry<String, Long>> it2 = this.mGCMAppsArrayMap.entrySet().iterator();
                while (it2.hasNext()) {
                    if (((String) ((Entry) it2.next()).getKey()).equals(pkgName)) {
                        it2.remove();
                        if (!isNeedNotifyUpdate) {
                            isNeedNotifyUpdate = true;
                        }
                    }
                }
            }
        }
        if (isNeedNotifyUpdate) {
            updateHabitProtectList();
        }
    }

    protected List<String> getForceProtectAppsFromHabitProtect(int emailCount, int imCount, Set<String> filterSet) {
        return getHabitProtectAppList(emailCount, imCount, filterSet, true);
    }

    private List<String> getHabitProtectAppList(int emailCount, int imCount, Set<String> filterSet, boolean flag) {
        List<String> result = new ArrayList();
        synchronized (this.mHabitProtectAppsList) {
            int imTmpCount = 0;
            int emailTmpCount = 0;
            for (ProtectApp app : this.mHabitProtectAppsList) {
                String pkg = app.getAppPkgName();
                if (app.getDeletedTag() != 1) {
                    if (flag) {
                        if (!(app.getAvgUsedFrequency() <= 0.5f || containsFilterPkg(pkg) || containsFilter2Pkg(pkg))) {
                            if (filterSet != null && filterSet.contains(pkg)) {
                            }
                        }
                    }
                    if (app.getAppType() == 0) {
                        if (imTmpCount < imCount) {
                            imTmpCount++;
                            result.add(pkg);
                        }
                    } else if (app.getAppType() == 1 && emailTmpCount < emailCount) {
                        emailTmpCount++;
                        result.add(pkg);
                    }
                    if (imTmpCount == imCount && emailTmpCount == emailCount) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public void dumpHabitProtectList(PrintWriter pw) {
        if (pw != null) {
            pw.println("dump user habit protect list.");
            synchronized (this.mHabitProtectListTopN) {
                Iterator it = this.mHabitProtectListTopN.iterator();
                while (it.hasNext()) {
                    pw.println((String) it.next());
                }
            }
        }
    }

    protected void registHabitProtectListChangeListener(HabitProtectListChangeListener listener) {
        if (listener != null) {
            synchronized (this.mListeners) {
                this.mListeners.add(listener);
            }
        }
    }

    protected void unregistHabitProtectListChangeListener(HabitProtectListChangeListener listener) {
        if (listener != null) {
            synchronized (this.mListeners) {
                this.mListeners.remove(listener);
            }
        }
    }

    protected List<String> getHabitProtectAppsAll(int emailCount, int imCount) {
        return getHabitProtectAppList(emailCount, imCount, null, false);
    }

    protected List<String> getHabitProtectList(int emailCount, int imCount) {
        List<String> result = getHabitProtectAppList(emailCount, imCount, null, true);
        int total = emailCount + imCount;
        synchronized (this.mHabitProtectArrayMap) {
            if (result.size() < total) {
                for (int i = 0; i < this.mHabitProtectArrayMap.size(); i++) {
                    String str = (String) this.mHabitProtectArrayMap.keyAt(i);
                    if (!result.contains(str)) {
                        result.add(str);
                    }
                    if (result.size() >= total) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    protected List<String> getGCMAppsList() {
        ArrayList arrayList;
        synchronized (this.mGCMAppsArrayMap) {
            arrayList = new ArrayList(this.mGCMAppsArrayMap.keySet());
        }
        return arrayList;
    }

    void setUserId(int userId) {
        this.mUserId.set(userId);
    }

    void clearHabitProtectApps() {
        synchronized (this.mGCMAppsArrayMap) {
            this.mGCMAppsArrayMap.clear();
        }
        synchronized (this.mHabitProtectArrayMap) {
            this.mHabitProtectArrayMap.clear();
        }
        updateHabitProtectList();
    }
}
