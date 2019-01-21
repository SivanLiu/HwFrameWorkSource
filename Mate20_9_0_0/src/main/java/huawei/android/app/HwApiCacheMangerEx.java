package huawei.android.app;

import android.app.ActivityThread;
import android.common.IHwApiCacheManagerEx;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import java.util.HashMap;

public class HwApiCacheMangerEx implements IHwApiCacheManagerEx {
    private static final long CACHE_EXPIRED_TIME_NS = ((SystemProperties.getLong("persist.sys.freqinfo.cachems", 10000) * 1000) * 1000);
    private static final boolean DEBUG_PERF = SystemProperties.getBoolean("persist.sys.freqinfo.debugperf", false);
    private static final String TAG = "HwApiCacheMangerEx";
    private static boolean USE_CACHE = SystemProperties.getBoolean("persist.sys.freqinfo.cache", true);
    private static HwApiCacheMangerEx sInstance;
    private boolean bCanCache = false;
    private final Object mAppInfoLock = new Object();
    private long mAppInfoTimes = CACHE_EXPIRED_TIME_NS;
    private long mAppInfoUs = CACHE_EXPIRED_TIME_NS;
    private final Object mPackageInfoLock = new Object();
    private long mPackageInfoTimes = CACHE_EXPIRED_TIME_NS;
    private long mPackageInfoUs = CACHE_EXPIRED_TIME_NS;
    private final Object mPackageUidLock = new Object();
    private long mUidTimes = CACHE_EXPIRED_TIME_NS;
    private long mUidUs = CACHE_EXPIRED_TIME_NS;
    private int mVolumeCacheItemCnt = 0;
    private final Object mVolumeLock = new Object();
    private HashMap<String, ApplicationInfo> sAppInfoCache = new HashMap();
    private HashMap<String, PackageInfo> sPackageInfoCache = new HashMap();
    private HashMap<String, Integer> sPackageUidCache = new HashMap();
    private HashMap<String, StorageVolume[]> sVolumeCache = new HashMap();
    private long totalTimes = CACHE_EXPIRED_TIME_NS;
    private long totalUs = CACHE_EXPIRED_TIME_NS;

    public void disableCache() {
        this.bCanCache = false;
        USE_CACHE = false;
        synchronized (this.mAppInfoLock) {
            this.sAppInfoCache.clear();
            this.sAppInfoCache = null;
        }
        synchronized (this.mPackageInfoLock) {
            this.sPackageInfoCache.clear();
            this.sPackageInfoCache = null;
        }
        synchronized (this.mPackageUidLock) {
            this.sPackageUidCache.clear();
            this.sPackageUidCache = null;
        }
        synchronized (this.mVolumeLock) {
            this.sVolumeCache.clear();
            this.sVolumeCache = null;
        }
    }

    public static synchronized HwApiCacheMangerEx getDefault() {
        HwApiCacheMangerEx hwApiCacheMangerEx;
        synchronized (HwApiCacheMangerEx.class) {
            if (sInstance == null) {
                sInstance = new HwApiCacheMangerEx();
            }
            hwApiCacheMangerEx = sInstance;
        }
        return hwApiCacheMangerEx;
    }

    /* JADX WARNING: Missing block: B:34:0x0084, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ApplicationInfo getApplicationInfoAsUser(IPackageManager pm, String packageName, int flags, int userId) throws RemoteException {
        RemoteException e;
        int i;
        String str = packageName;
        StringBuffer key = new StringBuffer();
        ApplicationInfo ai = null;
        long start = CACHE_EXPIRED_TIME_NS;
        boolean bNeedCache = false;
        if (this.bCanCache && DEBUG_PERF) {
            start = System.nanoTime();
        }
        String curPackageName = ActivityThread.currentPackageName();
        if (!(!this.bCanCache || str == null || curPackageName == null)) {
            bNeedCache = str.equals(curPackageName);
        }
        boolean bNeedCache2 = bNeedCache;
        int i2;
        try {
            if (this.bCanCache && bNeedCache2) {
                key.append(str);
                key.append("#");
                try {
                    key.append(userId);
                    key.append("#");
                } catch (RemoteException e2) {
                    e = e2;
                    i = flags;
                    throw e.rethrowFromSystemServer();
                }
                try {
                    key.append(flags);
                    synchronized (this.mAppInfoLock) {
                        if (this.sAppInfoCache != null) {
                            ai = (ApplicationInfo) this.sAppInfoCache.get(key.toString());
                        }
                        if (ai == null) {
                            ai = pm.getApplicationInfo(packageName, flags, userId);
                            if (this.sAppInfoCache != null) {
                                this.sAppInfoCache.put(key.toString(), ai);
                            }
                        } else if (DEBUG_PERF && start > CACHE_EXPIRED_TIME_NS) {
                            this.mAppInfoUs += (System.nanoTime() - start) / 1000;
                            this.mAppInfoTimes++;
                        }
                    }
                } catch (RemoteException e3) {
                    e = e3;
                    throw e.rethrowFromSystemServer();
                }
            }
            i = flags;
            i2 = userId;
            ai = pm.getApplicationInfo(packageName, flags, userId);
            if (ai == null) {
                return null;
            }
            if (this.bCanCache && bNeedCache2 && DEBUG_PERF && start > CACHE_EXPIRED_TIME_NS) {
                this.mAppInfoUs += (System.nanoTime() - start) / 1000;
                this.mAppInfoTimes++;
            }
            return ai;
        } catch (RemoteException e4) {
            e = e4;
            i = flags;
            i2 = userId;
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX WARNING: Missing block: B:34:0x0084, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public PackageInfo getPackageInfoAsUser(IPackageManager pm, String packageName, int flags, int userId) throws RemoteException {
        RemoteException e;
        int i;
        String str = packageName;
        PackageInfo pi = null;
        StringBuffer key = new StringBuffer();
        long start = CACHE_EXPIRED_TIME_NS;
        boolean bNeedCache = false;
        if (this.bCanCache && DEBUG_PERF) {
            start = System.nanoTime();
        }
        String curPackageName = ActivityThread.currentPackageName();
        if (!(!this.bCanCache || str == null || curPackageName == null)) {
            bNeedCache = str.equals(curPackageName);
        }
        boolean bNeedCache2 = bNeedCache;
        int i2;
        try {
            if (this.bCanCache && bNeedCache2) {
                key.append(str);
                key.append("#");
                try {
                    key.append(userId);
                    key.append("#");
                } catch (RemoteException e2) {
                    e = e2;
                    i = flags;
                    throw e.rethrowFromSystemServer();
                }
                try {
                    key.append(flags);
                    synchronized (this.mPackageInfoLock) {
                        if (this.sPackageInfoCache != null) {
                            pi = (PackageInfo) this.sPackageInfoCache.get(key.toString());
                        }
                        if (pi == null) {
                            pi = pm.getPackageInfo(packageName, flags, userId);
                            if (this.sPackageInfoCache != null) {
                                this.sPackageInfoCache.put(key.toString(), pi);
                            }
                        } else if (DEBUG_PERF && start > CACHE_EXPIRED_TIME_NS) {
                            this.mPackageInfoUs += (System.nanoTime() - start) / 1000;
                            this.mPackageInfoTimes++;
                        }
                    }
                } catch (RemoteException e3) {
                    e = e3;
                    throw e.rethrowFromSystemServer();
                }
            }
            i = flags;
            i2 = userId;
            pi = pm.getPackageInfo(packageName, flags, userId);
            if (pi == null) {
                return null;
            }
            if (this.bCanCache && bNeedCache2 && DEBUG_PERF && start > CACHE_EXPIRED_TIME_NS) {
                this.mPackageInfoUs += (System.nanoTime() - start) / 1000;
                this.mPackageInfoTimes++;
            }
            return pi;
        } catch (RemoteException e4) {
            e = e4;
            i = flags;
            i2 = userId;
            throw e.rethrowFromSystemServer();
        }
    }

    public int getPackageUidAsUser(IPackageManager pm, String packageName, int flags, int userId) throws RemoteException {
        Throwable th;
        String str;
        boolean z;
        RemoteException e;
        int i;
        String str2 = packageName;
        Integer oUid = null;
        StringBuffer key = new StringBuffer();
        long start = CACHE_EXPIRED_TIME_NS;
        boolean bNeedCache = false;
        if (this.bCanCache && DEBUG_PERF) {
            start = System.nanoTime();
        }
        String curPackageName = ActivityThread.currentPackageName();
        if (!(!this.bCanCache || str2 == null || curPackageName == null)) {
            bNeedCache = str2.equals(curPackageName);
        }
        boolean bNeedCache2 = bNeedCache;
        int i2;
        try {
            int uid;
            if (this.bCanCache && bNeedCache2) {
                key.append(str2);
                key.append("#");
                try {
                    key.append(userId);
                    key.append("#");
                    try {
                        key.append(flags);
                        synchronized (this.mPackageUidLock) {
                            try {
                                if (this.sPackageUidCache != null) {
                                    try {
                                        oUid = (Integer) this.sPackageUidCache.get(key.toString());
                                    } catch (Throwable th2) {
                                        th = th2;
                                        str = curPackageName;
                                        z = bNeedCache2;
                                        try {
                                            throw th;
                                        } catch (RemoteException e2) {
                                            e = e2;
                                            throw e.rethrowFromSystemServer();
                                        }
                                    }
                                }
                                if (oUid != null) {
                                    if (!DEBUG_PERF || start <= CACHE_EXPIRED_TIME_NS) {
                                        z = bNeedCache2;
                                    } else {
                                        this.mUidUs += (System.nanoTime() - start) / 1000;
                                        this.mUidTimes++;
                                    }
                                    int intValue = oUid.intValue();
                                    return intValue;
                                }
                                z = bNeedCache2;
                                uid = pm.getPackageUid(packageName, flags, userId);
                                if (uid >= 0 && this.sPackageUidCache != null) {
                                    this.sPackageUidCache.put(key.toString(), Integer.valueOf(uid));
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        }
                    } catch (RemoteException e3) {
                        e = e3;
                        str = curPackageName;
                        z = bNeedCache2;
                        throw e.rethrowFromSystemServer();
                    }
                } catch (RemoteException e4) {
                    e = e4;
                    i = flags;
                    str = curPackageName;
                    z = bNeedCache2;
                    throw e.rethrowFromSystemServer();
                }
            }
            i = flags;
            i2 = userId;
            str = curPackageName;
            z = bNeedCache2;
            uid = pm.getPackageUid(packageName, flags, userId);
            if (uid < 0) {
                return -1;
            }
            if (this.bCanCache && bNeedCache && DEBUG_PERF && start > CACHE_EXPIRED_TIME_NS) {
                this.mUidUs += (System.nanoTime() - start) / 1000;
                this.mUidTimes++;
            }
            return uid;
        } catch (RemoteException e5) {
            e = e5;
            i = flags;
            i2 = userId;
            str = curPackageName;
            z = bNeedCache2;
            throw e.rethrowFromSystemServer();
        }
    }

    public StorageVolume[] getVolumeList(IStorageManager storageManager, String packageName, int userId, int flags) throws RemoteException {
        long end;
        Throwable th;
        boolean z;
        boolean z2;
        RemoteException e;
        IStorageManager iStorageManager = storageManager;
        String str = packageName;
        int i = flags;
        long start = CACHE_EXPIRED_TIME_NS;
        long end2 = CACHE_EXPIRED_TIME_NS;
        if (iStorageManager == null) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("apicache storageManager is null for ");
            stringBuilder.append(str);
            Log.i(str2, stringBuilder.toString());
            return new StorageVolume[0];
        }
        boolean canCache = SystemProperties.getBoolean("persist.sys.getvolumelist.cache", true);
        boolean bNeedCache = doVolumeCacheItem(canCache, str);
        if (this.bCanCache && bNeedCache && DEBUG_PERF) {
            start = System.nanoTime();
        }
        int uid = getUid(this.bCanCache, str, userId);
        if (uid <= 0) {
            return new StorageVolume[0];
        }
        StorageVolume[] volumes = null;
        try {
            if (this.bCanCache && bNeedCache) {
                StringBuffer key = new StringBuffer();
                key.append(str);
                key.append("#");
                key.append(uid);
                key.append("#");
                key.append(i);
                synchronized (this.mVolumeLock) {
                    try {
                        if (this.sVolumeCache != null) {
                            HashMap hashMap;
                            try {
                                hashMap = this.sVolumeCache;
                                end = CACHE_EXPIRED_TIME_NS;
                            } catch (Throwable th2) {
                                th = th2;
                                end = CACHE_EXPIRED_TIME_NS;
                                z = canCache;
                                z2 = bNeedCache;
                                try {
                                    throw th;
                                } catch (RemoteException e2) {
                                    e = e2;
                                    end = end2;
                                }
                            }
                            try {
                                volumes = (StorageVolume[]) hashMap.get(key.toString());
                            } catch (Throwable th3) {
                                th = th3;
                                end2 = end;
                                throw th;
                            }
                        } else {
                            end = CACHE_EXPIRED_TIME_NS;
                        }
                        if (volumes != null) {
                            try {
                                if (!DEBUG_PERF || start <= CACHE_EXPIRED_TIME_NS) {
                                    z2 = bNeedCache;
                                } else {
                                    end2 = System.nanoTime();
                                    z = canCache;
                                    z2 = bNeedCache;
                                    try {
                                        this.totalUs += (end2 - start) / 1000;
                                        this.totalTimes++;
                                        end = end2;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        throw th;
                                    }
                                }
                                return volumes;
                            } catch (Throwable th5) {
                                th = th5;
                                end2 = end;
                                throw th;
                            }
                        } else {
                            z2 = bNeedCache;
                            volumes = iStorageManager.getVolumeList(uid, str, i);
                            doStorageVolume(volumes, key.toString());
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        end = CACHE_EXPIRED_TIME_NS;
                        z = canCache;
                        z2 = bNeedCache;
                        throw th;
                    }
                }
            } else {
                end = CACHE_EXPIRED_TIME_NS;
                z = canCache;
                z2 = bNeedCache;
                try {
                    if (DEBUG_PERF && this.bCanCache) {
                        String str3 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("get volume without apicache for ");
                        stringBuilder2.append(str);
                        Log.i(str3, stringBuilder2.toString());
                    }
                    volumes = iStorageManager.getVolumeList(uid, str, i);
                } catch (RemoteException e3) {
                    e = e3;
                    throw e.rethrowFromSystemServer();
                }
            }
            if (this.bCanCache && bNeedCache && DEBUG_PERF && start > CACHE_EXPIRED_TIME_NS) {
                end2 = System.nanoTime();
                this.totalUs += (end2 - start) / 1000;
                this.totalTimes++;
                end = end2;
            }
            return volumes;
        } catch (RemoteException e4) {
            e = e4;
            end = CACHE_EXPIRED_TIME_NS;
            z = canCache;
            z2 = bNeedCache;
            throw e.rethrowFromSystemServer();
        }
    }

    private int getUid(boolean bCanCache, String packageName, int userId) throws RemoteException {
        if (!bCanCache) {
            return ActivityThread.getPackageManager().getPackageUid(packageName, 268435456, userId);
        }
        try {
            return ActivityThread.currentActivityThread().getSystemContext().getPackageManager().getPackageUidAsUser(packageName, 268435456, userId);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("apicache getPackageUidAsUser excption:");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
            return -2;
        }
    }

    private boolean doVolumeCacheItem(boolean canCache, String packageName) {
        if (canCache) {
            String curPackageName = ActivityThread.currentPackageName();
            if (!this.bCanCache || packageName == null || curPackageName == null) {
                return false;
            }
            return packageName.equals(curPackageName);
        }
        Log.i(TAG, "clear apicache now,maybe insert card or remove card or rebooting system_server");
        synchronized (this.mVolumeLock) {
            if (this.mVolumeCacheItemCnt > 0) {
                this.sVolumeCache.clear();
                this.mVolumeCacheItemCnt = 0;
            }
        }
        return false;
    }

    private void doStorageVolume(StorageVolume[] volumes, String key) {
        boolean cantCache = false;
        int i = 0;
        while (volumes != null && i < volumes.length) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("apicache path=");
            stringBuilder.append(volumes[i].getPath());
            stringBuilder.append(" state=");
            stringBuilder.append(volumes[i].getState());
            stringBuilder.append(" key=");
            stringBuilder.append(key);
            Log.i(str, stringBuilder.toString());
            if (!volumes[i].getState().equals("mounted")) {
                cantCache = true;
                break;
            }
            i++;
        }
        if (volumes == null) {
            cantCache = true;
            Log.i(TAG, "cant apicache now,because volumes is null");
        }
        if (!cantCache && this.sVolumeCache != null) {
            if (this.mVolumeCacheItemCnt > 0 && volumes != null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("need clear apicache,because volumes changed,oldCnt=");
                stringBuilder2.append(this.mVolumeCacheItemCnt);
                stringBuilder2.append(" newCnt=");
                stringBuilder2.append(volumes.length);
                Log.i(str2, stringBuilder2.toString());
                this.sVolumeCache.clear();
                this.mVolumeCacheItemCnt = volumes.length;
            }
            this.sVolumeCache.put(key, volumes);
        }
    }

    public void apiPreCache(PackageManager app) {
        if (USE_CACHE) {
            this.bCanCache = true;
            String packageName = ActivityThread.currentPackageName();
            if (DEBUG_PERF) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("apicache mCurPackageName=");
                stringBuilder.append(packageName);
                stringBuilder.append(" uptimes=");
                stringBuilder.append(SystemClock.elapsedRealtime());
                Log.i(str, stringBuilder.toString());
            }
            long start = CACHE_EXPIRED_TIME_NS;
            if (packageName != null) {
                String str2;
                StringBuilder stringBuilder2;
                int userId = UserHandle.myUserId();
                cacheVolumeList(userId);
                if (DEBUG_PERF) {
                    start = System.nanoTime();
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("apicache async read begin packageName=");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(" userid=");
                    stringBuilder2.append(userId);
                    Log.i(str2, stringBuilder2.toString());
                }
                cachePackageInfo(app, packageName);
                if (DEBUG_PERF) {
                    long end = System.nanoTime();
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("apicache async read finished packageName=");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(" userid=");
                    stringBuilder2.append(userId);
                    stringBuilder2.append(" totalus=");
                    stringBuilder2.append((end - start) / 1000);
                    Log.i(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public void notifyVolumeStateChanged(int oldState, int newState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notify for apicache oldState=");
        stringBuilder.append(oldState);
        stringBuilder.append(" newState=");
        stringBuilder.append(newState);
        Log.i(str, stringBuilder.toString());
        if (newState == 2 || newState == 3 || newState == 7 || newState == 8) {
            SystemProperties.set("persist.sys.getvolumelist.cache", "true");
        } else {
            SystemProperties.set("persist.sys.getvolumelist.cache", "false");
        }
    }

    private void cacheVolumeList(int userId) {
        try {
            StorageManager.getVolumeList(userId, 256);
            StorageManager.getVolumeList(userId, 0);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("apicache getVolumeList excption:");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
        }
    }

    private void cachePackageInfo(PackageManager app, String packageName) {
        if (app != null) {
            try {
                app.getPackageInfo(packageName, 0);
                app.getPackageInfo(packageName, 64);
                app.getPackageInfo(packageName, 4096);
                app.getPackageUid(packageName, 0);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("apicache getPackageInfo excption:");
                stringBuilder.append(e.getMessage());
                Log.w(str, stringBuilder.toString());
            }
        }
    }
}
