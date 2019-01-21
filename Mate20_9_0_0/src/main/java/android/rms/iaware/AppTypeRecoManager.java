package android.rms.iaware;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;
import android.os.RemoteException;
import android.rms.iaware.AwareConstant.Database;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
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
    private static final String TAG = "AppTypeRecoManager";
    private static AppTypeRecoManager mAppTypeRecoManager = null;
    private final ArrayMap<String, AppTypeCacheInfo> mAppsTypeMap = new ArrayMap();
    private boolean mIsReady = false;
    private final SparseArray<List<String>> mTopIMMap = new SparseArray();

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
        if (ctx != null) {
            if (!this.mIsReady) {
                ContentResolver resolver = ctx.getContentResolver();
                ArrayMap<String, AppTypeCacheInfo> map = new ArrayMap();
                loadAppType(resolver, map);
                if (!map.isEmpty()) {
                    synchronized (this.mAppsTypeMap) {
                        this.mAppsTypeMap.putAll(map);
                    }
                    SparseArray<List<String>> imAppMap = new SparseArray();
                    loadAllIM(resolver, imAppMap);
                    synchronized (this.mTopIMMap) {
                        int length = imAppMap.size();
                        for (int i = 0; i < length; i++) {
                            int key = imAppMap.keyAt(i);
                            List<String> list = (List) imAppMap.valueAt(i);
                            if (list != null) {
                                this.mTopIMMap.put(key, list);
                            }
                        }
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("TopIM:");
                        stringBuilder.append(this.mTopIMMap);
                        AwareLog.d(str, stringBuilder.toString());
                    }
                    this.mIsReady = true;
                    AwareLog.i(TAG, "init end.");
                    return;
                }
                return;
            }
        }
        AwareLog.i(TAG, "no need to init");
    }

    public void deinit() {
        synchronized (this.mAppsTypeMap) {
            this.mAppsTypeMap.clear();
        }
        synchronized (this.mTopIMMap) {
            this.mTopIMMap.clear();
        }
        synchronized (this) {
            this.mIsReady = false;
        }
        AwareLog.i(TAG, "deinit.");
    }

    public boolean loadInstalledAppTypeInfo() {
        List<AppTypeInfo> list = null;
        try {
            IBinder binder = IAwareCMSManager.getICMSManager();
            if (binder != null) {
                list = IAwareCMSManager.getAllAppTypeInfo(binder);
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

    /* JADX WARNING: Missing block: B:23:0x0059, code skipped:
            if (r0 != null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:33:0x0074, code skipped:
            if (r0 == null) goto L_0x0077;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadAllIM(ContentResolver resolver, SparseArray<List<String>> map) {
        if (resolver != null && map != null) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(Database.HABITPROTECTLIST_URI, new String[]{"HabitProtectList.userID", "HabitProtectList.appPkgName"}, " HabitProtectList.deleted = 0 and HabitProtectList.appType = ?", new String[]{String.valueOf(0)}, "CAST(HabitProtectList.avgUsedFrequency AS REAL) desc");
                if (cursor == null) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                while (cursor.moveToNext()) {
                    int userId = cursor.getInt(0);
                    String pkgName = cursor.getString(1);
                    if (pkgName != null) {
                        if (!pkgName.isEmpty()) {
                            List<String> tmpList = (List) map.get(userId);
                            if (tmpList == null) {
                                tmpList = new ArrayList();
                                map.put(userId, tmpList);
                            }
                            tmpList.add(pkgName);
                        }
                    }
                }
            } catch (SQLiteException e) {
                AwareLog.e(TAG, "Error SQLiteException: loadAllIM");
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

    /* JADX WARNING: Missing block: B:20:0x0031, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isTopIM(String pkgName, int count, int userId) {
        if (pkgName == null || count <= 0) {
            return false;
        }
        synchronized (this.mTopIMMap) {
            List<String> imList = (List) this.mTopIMMap.get(userId);
            if (imList != null) {
                if (!imList.isEmpty()) {
                    boolean contains;
                    if (count < imList.size()) {
                        contains = imList.subList(0, count).contains(pkgName);
                        return contains;
                    }
                    contains = imList.contains(pkgName);
                    return contains;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x005a, code skipped:
            if (r0 != null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:29:0x0075, code skipped:
            if (r0 == null) goto L_0x0078;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadAppType(ContentResolver resolver, Map<String, AppTypeCacheInfo> typeMap) {
        if (resolver != null && typeMap != null) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(Database.APPTYPE_URI, new String[]{"appPkgName", "typeAttri", APP_TYPE, "source"}, null, null, null);
                if (cursor == null) {
                    AwareLog.e(TAG, "loadAppType cursor is null.");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                while (cursor.moveToNext()) {
                    String pkgName = cursor.getString(null);
                    int attri = cursor.getInt(1);
                    int apptype = cursor.getInt(2);
                    int source = cursor.getInt(3);
                    if (!(TextUtils.isEmpty(pkgName) || apptype == AppTypeInfo.PG_APP_TYPE_GAME)) {
                        typeMap.put(pkgName, new AppTypeCacheInfo(apptype, attri, source));
                    }
                }
            } catch (SQLiteException e) {
                AwareLog.e(TAG, "Error: loadAppType SQLiteException");
            } catch (IllegalStateException e2) {
                AwareLog.e(TAG, "Error: loadAppType IllegalStateException");
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

    public int getAppType(String pkgName) {
        AppTypeCacheInfo info;
        synchronized (this.mAppsTypeMap) {
            info = (AppTypeCacheInfo) this.mAppsTypeMap.get(pkgName);
        }
        if (info == null || info.getType() == -2) {
            return -1;
        }
        return info.getType();
    }

    public int getAppAttribute(String pkgName) {
        AppTypeCacheInfo info;
        synchronized (this.mAppsTypeMap) {
            info = (AppTypeCacheInfo) this.mAppsTypeMap.get(pkgName);
        }
        if (info == null) {
            return -1;
        }
        return info.getAttribute();
    }

    /* JADX WARNING: Missing block: B:18:0x002a, code skipped:
            return r2;
     */
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

    public Set<String> getAppsByType(int appType) {
        ArrayMap<String, Integer> appList;
        synchronized (this.mAppsTypeMap) {
            appList = new ArrayMap(this.mAppsTypeMap.size());
            for (Entry<String, AppTypeCacheInfo> entry : this.mAppsTypeMap.entrySet()) {
                appList.put((String) entry.getKey(), Integer.valueOf(((AppTypeCacheInfo) entry.getValue()).getType()));
            }
        }
        ArraySet<String> appSet = new ArraySet();
        int size = appList.size();
        for (int i = 0; i < size; i++) {
            if (((Integer) appList.valueAt(i)).intValue() == appType) {
                appSet.add((String) appList.keyAt(i));
            }
        }
        return appSet;
    }

    public Set<String> getAlarmApps() {
        ArrayMap<String, Integer> appList;
        synchronized (this.mAppsTypeMap) {
            appList = new ArrayMap(this.mAppsTypeMap.size());
            for (Entry<String, AppTypeCacheInfo> entry : this.mAppsTypeMap.entrySet()) {
                appList.put((String) entry.getKey(), Integer.valueOf(((AppTypeCacheInfo) entry.getValue()).getType()));
            }
        }
        ArraySet<String> appSet = new ArraySet();
        int size = appList.size();
        int i = 0;
        while (i < size) {
            if (((Integer) appList.valueAt(i)).intValue() == 5 || ((Integer) appList.valueAt(i)).intValue() == AppTypeInfo.PG_APP_TYPE_ALARM) {
                appSet.add((String) appList.keyAt(i));
            }
            i++;
        }
        return appSet;
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
