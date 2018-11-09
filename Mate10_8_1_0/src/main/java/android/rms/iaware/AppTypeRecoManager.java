package android.rms.iaware;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.rms.iaware.AwareConstant.AppUsageDatabase;
import android.rms.iaware.AwareConstant.Database;
import android.rms.iaware.ICMSManager.Stub;
import android.rms.iaware.utils.AppTypeRecoUtils;
import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AppTypeRecoManager {
    public static final String APPTYPE_INIT_ACTION = "APPTYPE_INIT_ACTION";
    public static final String APP_ATTR = "appAttr";
    public static final int APP_FROM_ABROAD = 1;
    public static final int APP_FROM_CHINA = 0;
    public static final int APP_FROM_UNKNOWN = -1;
    public static final String APP_PKGNAME = "pkgName";
    public static final String APP_STATUS = "appsSatus";
    public static final String APP_TYPE = "appType";
    public static final int APP_USEINDAY = 7;
    public static final long ONE_DAY = 86400000;
    private static final String TAG = "AppTypeRecoManager";
    private static AppTypeRecoManager mAppTypeRecoManager = null;
    private final List<String> mAppUsedInfos = new ArrayList();
    private final ArrayMap<String, AppTypeCacheInfo> mAppsTypeMap = new ArrayMap();
    private boolean mIsReady = false;
    private final List<String> mTopIMList = new ArrayList();

    public static class AppTypeCacheInfo {
        private int mAttr;
        private int mSource;
        private int mType;

        public AppTypeCacheInfo(int type, int attr, int source) {
            this.mType = type;
            this.mAttr = attr;
            this.mSource = source;
        }

        public AppTypeCacheInfo(int type, int attr) {
            this.mType = type;
            this.mAttr = attr;
            this.mSource = 0;
        }

        public int getType() {
            return this.mType;
        }

        public int getAttribute() {
            return this.mAttr;
        }

        public int getRecogSource() {
            return this.mSource;
        }

        public void setInfo(int type, int attr) {
            this.mType = type;
            this.mAttr = attr;
        }
    }

    public static synchronized AppTypeRecoManager getInstance() {
        AppTypeRecoManager appTypeRecoManager;
        synchronized (AppTypeRecoManager.class) {
            if (mAppTypeRecoManager == null) {
                mAppTypeRecoManager = new AppTypeRecoManager();
            }
            appTypeRecoManager = mAppTypeRecoManager;
        }
        return appTypeRecoManager;
    }

    private AppTypeRecoManager() {
    }

    public synchronized void init(Context ctx) {
        AwareLog.i(TAG, "init begin.");
        if (ctx == null || this.mIsReady) {
            AwareLog.i(TAG, "no need to init");
            return;
        }
        ContentResolver resolver = ctx.getContentResolver();
        ArrayMap<String, AppTypeCacheInfo> map = new ArrayMap();
        AppTypeRecoUtils.loadAppType(resolver, map);
        if (!map.isEmpty()) {
            synchronized (this.mAppsTypeMap) {
                this.mAppsTypeMap.putAll(map);
            }
            List<String> allImList = new ArrayList();
            loadAllIM(resolver, allImList);
            synchronized (this.mTopIMList) {
                this.mTopIMList.addAll(allImList);
                AwareLog.d(TAG, "IMList:" + this.mTopIMList);
            }
            List<String> usedIMList = new ArrayList();
            loadUsedApp(resolver, usedIMList, allImList, 0, 7);
            synchronized (this.mAppUsedInfos) {
                this.mAppUsedInfos.addAll(usedIMList);
                AwareLog.d(TAG, "NewUsedIMList:" + usedIMList);
            }
            this.mIsReady = true;
            AwareLog.i(TAG, "init end.");
        }
    }

    public void deinit() {
        synchronized (this.mAppsTypeMap) {
            this.mAppsTypeMap.clear();
        }
        synchronized (this.mTopIMList) {
            this.mTopIMList.clear();
        }
        synchronized (this) {
            this.mIsReady = false;
        }
        synchronized (this.mAppUsedInfos) {
            this.mAppUsedInfos.clear();
        }
        AwareLog.i(TAG, "deinit.");
    }

    public boolean loadInstalledAppTypeInfo() {
        List<AppTypeInfo> list = null;
        try {
            ICMSManager awareservice = Stub.asInterface(ServiceManager.getService("IAwareCMSService"));
            if (awareservice != null) {
                ParceledListSlice<AppTypeInfo> slice = awareservice.getAllAppTypeInfo();
                if (slice != null) {
                    list = slice.getList();
                }
            } else {
                AwareLog.e(TAG, "can not find service IAwareCMSService.");
            }
        } catch (RemoteException e) {
            AwareLog.e(TAG, "loadAppTypeInfo RemoteException");
        }
        if (list == null) {
            return false;
        }
        for (AppTypeInfo info : list) {
            if (-1 == getAppType(info.getPkgName()) || info.getType() != 255) {
                addAppType(info.getPkgName(), info.getType(), info.getAttribute());
            }
        }
        return true;
    }

    private void loadAllIM(ContentResolver resolver, List<String> imList) {
        if (resolver != null && imList != null) {
            Cursor cursor = null;
            try {
                ContentResolver contentResolver = resolver;
                cursor = contentResolver.query(Database.HABITPROTECTLIST_URI, new String[]{"HabitProtectList.appPkgName"}, " HabitProtectList.deleted = 0 and HabitProtectList.appType = ? and HabitProtectList.userID = 0", new String[]{String.valueOf(0)}, "CAST(HabitProtectList.avgUsedFrequency AS REAL) desc");
                if (cursor == null) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                while (cursor.moveToNext()) {
                    String pkgName = cursor.getString(0);
                    if (!(pkgName == null || pkgName.isEmpty())) {
                        imList.add(pkgName);
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLiteException e) {
                AwareLog.e(TAG, "Error SQLiteException: loadAllIM");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalStateException e2) {
                AwareLog.e(TAG, "Error IllegalStateException: loadAllIM");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void loadUsedApp(ContentResolver resolver, List<String> appUsedInfos, List<String> imList, int userId, int dayNum) {
        if (resolver != null && appUsedInfos != null && imList != null && imList.size() != 0) {
            Cursor cursor = null;
            try {
                long timeDiff = System.currentTimeMillis() - (((long) dayNum) * 86400000);
                ContentResolver contentResolver = resolver;
                cursor = contentResolver.query(AppUsageDatabase.APPUSAGE_URI, new String[]{APP_PKGNAME}, "foregroungTime > ? and foregroungTime < ? and userID = ?", new String[]{String.valueOf(timeDiff), String.valueOf(now), String.valueOf(userId)}, null);
                if (cursor == null) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                while (cursor.moveToNext()) {
                    String pkgName = cursor.getString(0);
                    if (!(pkgName == null || pkgName.isEmpty() || (imList.contains(pkgName) ^ 1) != 0)) {
                        appUsedInfos.add(pkgName);
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLiteException e) {
                AwareLog.e(TAG, "Error SQLiteException: loadUsedApp");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalStateException e2) {
                AwareLog.e(TAG, "Error IllegalStateException: loadUsedApp");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void loadAppUsedInfo(Context cxt, Map<String, Long> appUsedMap, int userId, int dayNum) {
        if (cxt != null && appUsedMap != null) {
            ContentResolver resolver = cxt.getContentResolver();
            if (resolver != null) {
                Cursor cursor = null;
                try {
                    long timeDiff = System.currentTimeMillis() - (((long) dayNum) * 86400000);
                    cursor = resolver.query(AppUsageDatabase.APPUSAGE_URI, new String[]{APP_PKGNAME, "foregroungTime"}, "foregroungTime > ? and foregroungTime < ? and userID = ? ", new String[]{String.valueOf(timeDiff), String.valueOf(now), String.valueOf(userId)}, null);
                    if (cursor == null) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        return;
                    }
                    while (cursor.moveToNext()) {
                        String pkgName = cursor.getString(0);
                        if (!(pkgName == null || pkgName.isEmpty())) {
                            appUsedMap.put(pkgName, Long.valueOf(cursor.getLong(1)));
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (SQLiteException e) {
                    AwareLog.e(TAG, "Error SQLiteException: loadAppUsedInfo");
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (IllegalStateException e2) {
                    AwareLog.e(TAG, "Error IllegalStateException: loadAppUsedInfo");
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    public boolean isTopIM(String pkgName, int count) {
        if (pkgName == null || count <= 0) {
            return false;
        }
        return getTopIMList(count).contains(pkgName);
    }

    private List<String> getTopIMList(int topN) {
        List<String> result;
        List<String> tmpAppUsedInfos = new ArrayList();
        synchronized (this.mAppUsedInfos) {
            tmpAppUsedInfos.addAll(this.mAppUsedInfos);
        }
        synchronized (this.mTopIMList) {
            result = new ArrayList();
            int length = this.mTopIMList.size();
            for (int i = 0; i < length; i++) {
                String pkgName = (String) this.mTopIMList.get(i);
                if (pkgName != null && (tmpAppUsedInfos.contains(pkgName) ^ 1) == 0) {
                    result.add(pkgName);
                    if (result.size() >= topN) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<String> dumpTopIMList(int topN) {
        if (topN <= 0) {
            return null;
        }
        return getTopIMList(topN);
    }

    public int getAppType(String pkgName) {
        synchronized (this.mAppsTypeMap) {
            AppTypeCacheInfo info = (AppTypeCacheInfo) this.mAppsTypeMap.get(pkgName);
        }
        if (info == null || info.getType() == -2) {
            return -1;
        }
        return info.getType();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getAppWhereFrom(String pkgName) {
        synchronized (this.mAppsTypeMap) {
            if (this.mAppsTypeMap.containsKey(pkgName)) {
                AppTypeCacheInfo appTypeInfo = (AppTypeCacheInfo) this.mAppsTypeMap.get(pkgName);
                if (appTypeInfo == null) {
                    return -1;
                }
                int appAttr = appTypeInfo.getAttribute();
                if (appAttr == -1) {
                    return -1;
                }
                int i = (appAttr & AppTypeInfo.APP_ATTRIBUTE_OVERSEA) == AppTypeInfo.APP_ATTRIBUTE_OVERSEA ? 1 : 0;
            } else {
                return -1;
            }
        }
    }

    public boolean containsAppType(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        synchronized (this.mAppsTypeMap) {
            if (this.mAppsTypeMap.containsKey(pkgName)) {
                return true;
            }
            return false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Set<String> getAppsByType(int appType) {
        Throwable th;
        synchronized (this.mAppsTypeMap) {
            try {
                ArrayMap<String, Integer> appList = new ArrayMap(this.mAppsTypeMap.size());
                try {
                    for (Entry<String, AppTypeCacheInfo> entry : this.mAppsTypeMap.entrySet()) {
                        appList.put((String) entry.getKey(), Integer.valueOf(((AppTypeCacheInfo) entry.getValue()).getType()));
                    }
                } catch (Throwable th2) {
                    th = th2;
                    ArrayMap<String, Integer> arrayMap = appList;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Set<String> getAlarmApps() {
        Throwable th;
        synchronized (this.mAppsTypeMap) {
            try {
                ArrayMap<String, Integer> appList = new ArrayMap(this.mAppsTypeMap.size());
                try {
                    for (Entry<String, AppTypeCacheInfo> entry : this.mAppsTypeMap.entrySet()) {
                        appList.put((String) entry.getKey(), Integer.valueOf(((AppTypeCacheInfo) entry.getValue()).getType()));
                    }
                } catch (Throwable th2) {
                    th = th2;
                    ArrayMap<String, Integer> arrayMap = appList;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public void removeAppType(String pkgName) {
        synchronized (this.mAppsTypeMap) {
            this.mAppsTypeMap.remove(pkgName);
        }
    }

    public void addAppType(String pkgName, int type, int attr) {
        synchronized (this.mAppsTypeMap) {
            AppTypeCacheInfo cacheInfo = (AppTypeCacheInfo) this.mAppsTypeMap.get(pkgName);
            if (cacheInfo == null) {
                this.mAppsTypeMap.put(pkgName, new AppTypeCacheInfo(type, attr));
            } else {
                cacheInfo.setInfo(type, attr);
            }
        }
    }

    public int convertType(int appType) {
        if (appType <= 255) {
            return appType;
        }
        int type = appType;
        switch (appType) {
            case AppTypeInfo.PG_APP_TYPE_LAUNCHER /*301*/:
                type = 28;
                break;
            case AppTypeInfo.PG_APP_TYPE_SMS /*302*/:
                type = 27;
                break;
            case AppTypeInfo.PG_APP_TYPE_EMAIL /*303*/:
                type = 1;
                break;
            case AppTypeInfo.PG_APP_TYPE_INPUTMETHOD /*304*/:
                type = 19;
                break;
            case AppTypeInfo.PG_APP_TYPE_GAME /*305*/:
                type = 9;
                break;
            case AppTypeInfo.PG_APP_TYPE_BROWSER /*306*/:
                type = 18;
                break;
            case AppTypeInfo.PG_APP_TYPE_EBOOK /*307*/:
                type = 6;
                break;
            case AppTypeInfo.PG_APP_TYPE_VIDEO /*308*/:
                type = 8;
                break;
            case AppTypeInfo.PG_APP_TYPE_ALARM /*310*/:
                type = 5;
                break;
            case AppTypeInfo.PG_APP_TYPE_IM /*311*/:
                type = 0;
                break;
            case AppTypeInfo.PG_APP_TYPE_MUSIC /*312*/:
                type = 7;
                break;
            case AppTypeInfo.PG_APP_TYPE_NAVIGATION /*313*/:
                type = 3;
                break;
            case AppTypeInfo.PG_APP_TYPE_OFFICE /*315*/:
                type = 12;
                break;
            case AppTypeInfo.PG_APP_TYPE_GALLERY /*316*/:
                type = 29;
                break;
            case AppTypeInfo.PG_APP_TYPE_SIP /*317*/:
                type = 30;
                break;
            case AppTypeInfo.PG_APP_TYPE_NEWS_CLIENT /*318*/:
                type = 26;
                break;
            case AppTypeInfo.PG_APP_TYPE_SHOP /*319*/:
                type = 14;
                break;
            case AppTypeInfo.PG_APP_TYPE_APP_MARKET /*320*/:
                type = 31;
                break;
            case AppTypeInfo.PG_APP_TYPE_LIFE_TOOL /*321*/:
                type = 32;
                break;
            case AppTypeInfo.PG_APP_TYPE_EDUCATION /*322*/:
                type = 33;
                break;
            case AppTypeInfo.PG_APP_TYPE_MONEY /*323*/:
                type = 34;
                break;
            case AppTypeInfo.PG_APP_TYPE_CAMERA /*324*/:
                type = 17;
                break;
            case AppTypeInfo.PG_APP_TYPE_PEDOMETER /*325*/:
                type = 2;
                break;
        }
        return type;
    }
}
