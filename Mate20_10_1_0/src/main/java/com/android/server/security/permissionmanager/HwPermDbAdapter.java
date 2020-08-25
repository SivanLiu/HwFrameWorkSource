package com.android.server.security.permissionmanager;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.security.permissionmanager.sms.smsutils.Utils;
import com.android.server.security.permissionmanager.util.CursorHelper;
import com.android.server.security.permissionmanager.util.PermConst;
import com.android.server.security.permissionmanager.util.PermissionClass;
import com.android.server.security.permissionmanager.util.PermissionType;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.securitycenter.permission.ui.model.DbPermissionItem;
import huawei.android.security.IOnHwPermissionChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HwPermDbAdapter {
    private static final int APPLY_PERMISSION = 1;
    private static final int DEFAULT_CAPACITY = 16;
    private static final int DEFAULT_CAPACITY_ARRAY = 10;
    private static final long DEFAULT_TYPE_VALUE = 4294967295L;
    private static final int FLAGS_PERMISSION_WHITELIST_ALL = 7;
    private static final int ILLEGAL = -1;
    private static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(LOG_TAG, 4)));
    private static final String KEY_CALLER = "caller";
    private static final String KEY_OPERATION = "operation";
    private static final String KEY_PACKAGE_NAME = "pkgName";
    private static final String KEY_PERM_TYPE = "permType";
    private static final Object LOCK = new Object();
    private static final String LOG_TAG = "HwPermDBAdapter";
    private static final int MIN_MONITOR_UID = 10000;
    private static final int NO_APPLY_PERMISSION = 0;
    private static final int OWNER_ID = 0;
    private static volatile HwPermDbAdapter sUniqueInstance = null;
    private Context mContext = null;
    /* access modifiers changed from: private */
    public HwPermDbHelper mHwPermDbHelper;
    /* access modifiers changed from: private */
    public final SparseArray<ArrayMap<String, DbPermissionItem>> mHwPermissionCache;
    private final OnPermissionChangeListeners mOnPermissionChangeListeners;
    private volatile SparseArray<String> mUidToPkgNameCache = null;

    private HwPermDbAdapter(Context context) {
        this.mContext = context;
        Slog.i(LOG_TAG, "create HwPermDBAdapter");
        this.mHwPermDbHelper = HwPermDbHelper.getInstance(this.mContext);
        HandlerThread handlerThread = new HandlerThread(LOG_TAG);
        handlerThread.start();
        this.mOnPermissionChangeListeners = new OnPermissionChangeListeners(handlerThread.getLooper());
        this.mUidToPkgNameCache = new SparseArray<>(10);
        this.mHwPermissionCache = new SparseArray<>(10);
        this.mHwPermissionCache.put(0, new ArrayMap<>(10));
        this.mOnPermissionChangeListeners.getPermInfoFromDb();
    }

    public static HwPermDbAdapter getInstance(Context context) {
        if (sUniqueInstance == null) {
            synchronized (LOCK) {
                if (sUniqueInstance == null) {
                    sUniqueInstance = new HwPermDbAdapter(context);
                }
            }
        }
        return sUniqueInstance;
    }

    public void addOnPermissionsChangeListener(IOnHwPermissionChangeListener listener) {
        if (listener != null) {
            synchronized (this.mHwPermissionCache) {
                Slog.i(LOG_TAG, "addOnPermissionsChangeListener: " + listener);
                this.mOnPermissionChangeListeners.addListenerLocked(listener);
            }
        }
    }

    public void removeOnPermissionsChangeListener(IOnHwPermissionChangeListener listener) {
        if (listener != null) {
            synchronized (this.mHwPermissionCache) {
                Slog.i(LOG_TAG, "removeOnPermissionsChangeListener: " + listener);
                this.mOnPermissionChangeListeners.removeListenerLocked(listener);
            }
        }
    }

    private final class OnPermissionChangeListeners extends Handler {
        private static final int MSG_ON_PERMISSIONS_CHANGED = 1;
        private static final int MSG_ON_PERMISSIONS_INIT = 2;
        private final RemoteCallbackList<IOnHwPermissionChangeListener> mPermissionListeners = new RemoteCallbackList<>();

        OnPermissionChangeListeners(Looper looper) {
            super(looper);
        }

        /* JADX INFO: Multiple debug info for r0v1 java.lang.Object: [D('obj' java.lang.Object), D('userIdList' java.util.ArrayList<java.lang.Integer>)] */
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                Object obj = msg.obj;
                if (obj instanceof Bundle) {
                    Bundle bundle = (Bundle) obj;
                    String caller = bundle.getString(HwPermDbAdapter.KEY_CALLER);
                    String pkgName = bundle.getString("pkgName");
                    long permType = bundle.getLong(HwPermDbAdapter.KEY_PERM_TYPE);
                    int operation = bundle.getInt(HwPermDbAdapter.KEY_OPERATION);
                    if (TextUtils.isEmpty(caller) || TextUtils.isEmpty(pkgName)) {
                        Slog.i(HwPermDbAdapter.LOG_TAG, "handleMessage caller or pkgName is null");
                    } else {
                        handleOnPermissionsChanged(caller, pkgName, permType, operation);
                    }
                }
            } else if (i == 2) {
                Slog.i(HwPermDbAdapter.LOG_TAG, "begin init cache");
                HwPermDbAdapter hwPermDbAdapter = HwPermDbAdapter.this;
                ArrayList<Integer> userIdList = hwPermDbAdapter.getUserIdForTableName(hwPermDbAdapter.mHwPermDbHelper.getTableNames());
                int userIdListSize = userIdList.size();
                for (int i2 = 0; i2 < userIdListSize; i2++) {
                    int userId = userIdList.get(i2).intValue();
                    ArrayMap<String, DbPermissionItem> map = HwPermDbAdapter.this.initPermInfoFromDb(userId);
                    synchronized (HwPermDbAdapter.this.mHwPermissionCache) {
                        HwPermDbAdapter.this.mHwPermissionCache.put(userId, map);
                    }
                    Slog.i(HwPermDbAdapter.LOG_TAG, "PERMISSION_INIT: " + map + ", userId:" + userId);
                }
            }
        }

        /* access modifiers changed from: private */
        public void addListenerLocked(IOnHwPermissionChangeListener listener) {
            Slog.i(HwPermDbAdapter.LOG_TAG, "addListenerLocked: " + listener);
            this.mPermissionListeners.register(listener);
        }

        /* access modifiers changed from: private */
        public void removeListenerLocked(IOnHwPermissionChangeListener listener) {
            Slog.i(HwPermDbAdapter.LOG_TAG, "removeListenerLocked: " + listener);
            this.mPermissionListeners.unregister(listener);
        }

        /* access modifiers changed from: private */
        public void onPermissionChanged(String caller, String pkgName, long permType, int operation) {
            if (caller != null && pkgName != null) {
                int callbackCount = this.mPermissionListeners.getRegisteredCallbackCount();
                Slog.i(HwPermDbAdapter.LOG_TAG, "CallbackCount: " + callbackCount);
                if (callbackCount > 0) {
                    Bundle bundle = new Bundle();
                    bundle.putString(HwPermDbAdapter.KEY_CALLER, caller);
                    bundle.putString("pkgName", pkgName);
                    bundle.putLong(HwPermDbAdapter.KEY_PERM_TYPE, permType);
                    bundle.putInt(HwPermDbAdapter.KEY_OPERATION, operation);
                    obtainMessage(1, bundle).sendToTarget();
                }
            }
        }

        private void handleOnPermissionsChanged(String caller, String pkgName, long permType, int operation) {
            int count = this.mPermissionListeners.beginBroadcast();
            Slog.i(HwPermDbAdapter.LOG_TAG, "handleOnPermissionsChanged: " + count);
            int i = 0;
            while (i < count) {
                try {
                    try {
                        this.mPermissionListeners.getBroadcastItem(i).onPermissionChanged(caller, pkgName, permType, operation);
                    } catch (RemoteException e) {
                        Slog.e(HwPermDbAdapter.LOG_TAG, "Permission listener is dead");
                    }
                    i++;
                } catch (Throwable th) {
                    this.mPermissionListeners.finishBroadcast();
                    throw th;
                }
            }
            this.mPermissionListeners.finishBroadcast();
        }

        /* access modifiers changed from: private */
        public void getPermInfoFromDb() {
            Slog.i(HwPermDbAdapter.LOG_TAG, "getPermInfoFromDb");
            obtainMessage(2).sendToTarget();
        }
    }

    public ArrayList<DbPermissionItem> getAllAppsPermInfo(int userId) {
        synchronized (this.mHwPermissionCache) {
            ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
            if (map == null) {
                Slog.e(LOG_TAG, "current user cache is null: " + userId);
                return new ArrayList<>(0);
            }
            return new ArrayList<>((Collection<? extends DbPermissionItem>) map.values());
        }
    }

    public DbPermissionItem getHwPermInfo(String pkgName, int userId) {
        synchronized (this.mHwPermissionCache) {
            ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
            if (map == null) {
                Slog.e(LOG_TAG, "current user cache is null: " + userId);
                return new DbPermissionItem(pkgName);
            }
            return map.get(pkgName);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0055, code lost:
        r4 = r8 & r13;
        r6 = r8 & r13;
        android.util.Slog.i(com.android.server.security.permissionmanager.HwPermDbAdapter.LOG_TAG, "checkHwPerm, pkgName: " + r12 + ", cfg:" + r8 + ", permCfgResult:" + r6 + ", permType :" + r13 + ", code:" + r8 + ", permCodeResult:" + r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x009b, code lost:
        if (r4 != 0) goto L_0x009f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x009d, code lost:
        return 4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x00a1, code lost:
        if (r6 == 0) goto L_0x00a5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x00a3, code lost:
        return 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x00a5, code lost:
        return 1;
     */
    public int checkHwPerm(String pkgName, long permType, int userId) {
        synchronized (this.mHwPermissionCache) {
            ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
            if (map == null) {
                Slog.e(LOG_TAG, "current user cache is null: " + userId);
                return -1;
            }
            DbPermissionItem item = map.get(pkgName);
            if (item == null) {
                Slog.e(LOG_TAG, "DbPermissionItem cache is null: " + pkgName);
                return -1;
            }
            long permissionCode = item.getPermissionCode();
            long permissionCfg = item.getPermissionCfg();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x004e, code lost:
        r2 = r6 & r11;
        android.util.Slog.i(com.android.server.security.permissionmanager.HwPermDbAdapter.LOG_TAG, "checkHwPerm, pkgName: " + r10 + ", permType :" + r11 + ", code:" + r6 + ", permCodeResult:" + r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0082, code lost:
        if (r2 != 0) goto L_0x0086;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0084, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0086, code lost:
        return 1;
     */
    public int checkHwPermCode(String pkgName, long permType, int userId) {
        synchronized (this.mHwPermissionCache) {
            ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
            if (map == null) {
                Slog.e(LOG_TAG, "current user cache is null: " + userId);
                return -1;
            }
            DbPermissionItem item = map.get(pkgName);
            if (item == null) {
                Slog.e(LOG_TAG, "DbPermissionItem cache is null: " + pkgName);
                return -1;
            }
            long permissionCode = item.getPermissionCode();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0035, code lost:
        r0 = r5.size();
        r4 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x003a, code lost:
        if (r4 >= r0) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x003c, code lost:
        r12 = r5.get(r4).getPermissionCode();
        r14 = r5.get(r4).getPermissionCfg();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0056, code lost:
        if (isPermType(r12, r17) == false) goto L_0x006d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0058, code lost:
        r5.add(new com.huawei.securitycenter.permission.ui.model.DbPermissionItem(r5.get(r4).getPackageName(), r14, r12));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x006d, code lost:
        r4 = r4 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0070, code lost:
        return r5;
     */
    public ArrayList<DbPermissionItem> getHwPermApps(long permType, int userId) {
        synchronized (this.mHwPermissionCache) {
            ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
            if (map == null) {
                Slog.e(LOG_TAG, "current user cache is null: " + userId);
                return null;
            }
            ArrayList<DbPermissionItem> list = new ArrayList<>((Collection<? extends DbPermissionItem>) map.values());
        }
    }

    public void setHwPermission(String pkgName, int userId, long permType, int operation, int callingUid) {
        Cursor cursor;
        int uid;
        ContentValues values;
        long permissionCodeResult;
        long permissionCfgResult;
        Cursor cursor2 = null;
        long permissionCode = 0;
        try {
            Cursor cursor3 = this.mHwPermDbHelper.query(getPermCfgTableName(userId), "packageName= ?", new String[]{pkgName});
            try {
                if (!CursorHelper.checkCursorValid(cursor3)) {
                    cursor = cursor3;
                } else if (!cursor3.moveToFirst()) {
                    cursor = cursor3;
                } else {
                    long permissionCode2 = cursor3.getLong(cursor3.getColumnIndex(PermConst.PERMISSION_CODE));
                    try {
                        long permissionCfg = cursor3.getLong(cursor3.getColumnIndex(PermConst.PERMISSION_CFG));
                        try {
                            uid = cursor3.getInt(cursor3.getColumnIndex("uid"));
                            values = setPermContentValues(pkgName, permType, operation, permissionCode2, permissionCfg);
                            values.put("uid", Integer.valueOf(uid));
                            Long permissionCfgLong = values.getAsLong(PermConst.PERMISSION_CFG);
                            Long permissionCodeLong = values.getAsLong(PermConst.PERMISSION_CODE);
                            permissionCodeResult = 0;
                            permissionCfgResult = permissionCfgLong == null ? 0 : permissionCfgLong.longValue();
                            if (permissionCodeLong != null) {
                                permissionCodeResult = permissionCodeLong.longValue();
                            }
                            cursor = cursor3;
                        } catch (SQLiteException e) {
                            permissionCode = permissionCode2;
                            cursor2 = cursor3;
                            try {
                                Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                                CursorHelper.closeCursor(cursor2);
                            } catch (Throwable th) {
                                th = th;
                                cursor = cursor2;
                                CursorHelper.closeCursor(cursor);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            cursor = cursor3;
                            CursorHelper.closeCursor(cursor);
                            throw th;
                        }
                        try {
                            addHwPermissionCache(pkgName, permissionCfgResult, permissionCodeResult, userId, uid);
                            this.mHwPermDbHelper.replace(values, getPermCfgTableName(userId));
                            try {
                                String caller = this.mContext.getPackageManager().getNameForUid(callingUid);
                                StringBuilder sb = new StringBuilder();
                                sb.append("callback, caller");
                                sb.append(caller);
                                sb.append(", pkgName");
                                sb.append(pkgName);
                                sb.append(",permType");
                                sb.append(permType);
                                sb.append(", ");
                                try {
                                    sb.append(operation);
                                    Slog.i(LOG_TAG, sb.toString());
                                    if (TextUtils.isEmpty(caller) || !caller.contains("android.uid.system:")) {
                                        synchronized (this.mOnPermissionChangeListeners) {
                                            this.mOnPermissionChangeListeners.onPermissionChanged(caller, pkgName, permType, operation);
                                        }
                                        CursorHelper.closeCursor(cursor);
                                        return;
                                    }
                                    CursorHelper.closeCursor(cursor);
                                    return;
                                } catch (SQLiteException e2) {
                                    permissionCode = permissionCode2;
                                    cursor2 = cursor;
                                    Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                                    CursorHelper.closeCursor(cursor2);
                                } catch (Throwable th3) {
                                    th = th3;
                                    CursorHelper.closeCursor(cursor);
                                    throw th;
                                }
                            } catch (SQLiteException e3) {
                                permissionCode = permissionCode2;
                                cursor2 = cursor;
                                Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                                CursorHelper.closeCursor(cursor2);
                            } catch (Throwable th4) {
                                th = th4;
                                CursorHelper.closeCursor(cursor);
                                throw th;
                            }
                        } catch (SQLiteException e4) {
                            permissionCode = permissionCode2;
                            cursor2 = cursor;
                            Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                            CursorHelper.closeCursor(cursor2);
                        } catch (Throwable th5) {
                            th = th5;
                            CursorHelper.closeCursor(cursor);
                            throw th;
                        }
                    } catch (SQLiteException e5) {
                        permissionCode = permissionCode2;
                        cursor2 = cursor3;
                        Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                        CursorHelper.closeCursor(cursor2);
                    } catch (Throwable th6) {
                        th = th6;
                        cursor = cursor3;
                        CursorHelper.closeCursor(cursor);
                        throw th;
                    }
                }
            } catch (SQLiteException e6) {
                cursor2 = cursor3;
                Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                CursorHelper.closeCursor(cursor2);
            } catch (Throwable th7) {
                th = th7;
                cursor = cursor3;
                CursorHelper.closeCursor(cursor);
                throw th;
            }
            try {
                Slog.e(LOG_TAG, "cursor is null");
                CursorHelper.closeCursor(cursor);
            } catch (SQLiteException e7) {
                cursor2 = cursor;
                Slog.e(LOG_TAG, "setHwPermission SQLiteException");
                CursorHelper.closeCursor(cursor2);
            } catch (Throwable th8) {
                th = th8;
                CursorHelper.closeCursor(cursor);
                throw th;
            }
        } catch (SQLiteException e8) {
            Slog.e(LOG_TAG, "setHwPermission SQLiteException");
            CursorHelper.closeCursor(cursor2);
        } catch (Throwable th9) {
            th = th9;
            cursor = null;
            CursorHelper.closeCursor(cursor);
            throw th;
        }
    }

    private ContentValues setPermContentValues(String pkgName, long permType, int operation, long permissionCode, long permissionCfg) {
        long permissionCfgResult;
        ContentValues values = new ContentValues();
        if (operation == 1 || operation == 2) {
            long permissionCodeResult = permissionCode | permType;
            values.put(PermConst.PERMISSION_CODE, Long.valueOf(permissionCodeResult));
            if (operation == 1) {
                permissionCfgResult = permissionCfg & (~permType);
            } else {
                permissionCfgResult = permissionCfg | permType;
            }
            values.put(PermConst.PERMISSION_CFG, Long.valueOf(permissionCfgResult));
            values.put("packageName", pkgName);
            Slog.i(LOG_TAG, "pkgName= " + pkgName + ", permCode= " + permissionCodeResult + ", permCfg= " + permissionCfgResult + ",operation: " + operation);
            return values;
        }
        Slog.e(LOG_TAG, "Illegal operation: " + operation);
        return values;
    }

    public void setHwPermission(HashMap<String, Integer> apps, int userId, long permissionType) {
        int callingUid = Binder.getCallingUid();
        for (Map.Entry<String, Integer> entry : apps.entrySet()) {
            Integer operationInteger = entry.getValue();
            if (operationInteger != null) {
                setHwPermission(entry.getKey(), userId, permissionType, operationInteger.intValue(), callingUid);
            }
        }
    }

    public void replacePermForAllApps(ArrayList<DbPermissionItem> apps, int userId) {
        int appsSize = apps.size();
        for (int i = 0; i < appsSize; i++) {
            DbPermissionItem item = apps.get(i);
            replaceHwPermInfo(item.getPackageName(), userId, item.getPermissionCode(), item.getPermissionCfg(), item.getUid());
        }
    }

    public void replaceHwPermInfo(String pkgName, int userId, long permCode, long permCfg, int uid) {
        try {
            ContentValues values = new ContentValues();
            values.put(PermConst.PERMISSION_CODE, Long.valueOf(permCode));
            values.put(PermConst.PERMISSION_CFG, Long.valueOf(permCfg));
            try {
                values.put("packageName", pkgName);
                values.put("uid", Integer.valueOf(uid));
                addHwPermissionCache(pkgName, permCfg, permCode, userId, uid);
                try {
                    this.mHwPermDbHelper.replace(values, getPermCfgTableName(userId));
                } catch (SQLiteException e) {
                }
            } catch (SQLiteException e2) {
                Slog.e(LOG_TAG, "replaceHwPermInfo SQLiteException");
            }
        } catch (SQLiteException e3) {
            Slog.e(LOG_TAG, "replaceHwPermInfo SQLiteException");
        }
    }

    public void removeHwPermission(String pkgName, int userId) {
        removeHwPermissionCache(pkgName, userId);
        try {
            this.mHwPermDbHelper.delete(getPermCfgTableName(userId), "packageName = ?", new String[]{pkgName});
        } catch (SQLiteException e) {
            Slog.e(LOG_TAG, "removeHwPermission SQLiteException");
        }
    }

    private Cursor getCursorOfPermCfg(String pkgName, int userId) {
        return this.mHwPermDbHelper.query(getPermCfgTableName(userId), "packageName = ?", new String[]{pkgName});
    }

    private boolean isPermType(long permCode, long permType) {
        return (permCode & permType) != 0;
    }

    public static String getPermCfgTableName(int userId) {
        return PermConst.HW_PERM_SET_TABLE_NAME + userId;
    }

    public void removeHwPermissionCache(String pkgName, int userId) {
        if (TextUtils.isEmpty(pkgName)) {
            this.mHwPermissionCache.remove(userId);
            return;
        }
        int uid = -1;
        synchronized (this.mHwPermissionCache) {
            ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
            if (map != null) {
                DbPermissionItem item = map.get(pkgName);
                if (item != null) {
                    uid = item.getUid();
                }
                map.remove(pkgName);
                Slog.i(LOG_TAG, "user: " + userId + ", remove cache: " + pkgName);
            } else {
                Slog.e(LOG_TAG, "current user cache is null: " + userId);
            }
            this.mUidToPkgNameCache.remove(uid);
            Slog.i(LOG_TAG, "removeHwPermissionCache mUidToPkgNameCache: " + this.mUidToPkgNameCache);
        }
    }

    /* access modifiers changed from: private */
    public ArrayMap<String, DbPermissionItem> initPermInfoFromDb(int userId) {
        ArrayMap<String, DbPermissionItem> map = new ArrayMap<>(16);
        Cursor cursor = null;
        try {
            cursor = this.mHwPermDbHelper.query(getPermCfgTableName(userId), null, null);
            if (CursorHelper.checkCursorValid(cursor)) {
                int packageNameIndex = cursor.getColumnIndex("packageName");
                int permissionCodeIndex = cursor.getColumnIndex(PermConst.PERMISSION_CODE);
                int permissionCfgIndex = cursor.getColumnIndex(PermConst.PERMISSION_CFG);
                int uidIndex = cursor.getColumnIndex("uid");
                while (cursor.moveToNext()) {
                    String pkgName = cursor.getString(packageNameIndex);
                    if (!TextUtils.isEmpty(pkgName)) {
                        long permissionCode = cursor.getLong(permissionCodeIndex);
                        long permissionCfg = cursor.getLong(permissionCfgIndex);
                        int uid = cursor.getInt(uidIndex);
                        map.put(pkgName, new DbPermissionItem(pkgName, permissionCfg, permissionCode, uid));
                        synchronized (this.mHwPermissionCache) {
                            try {
                                this.mUidToPkgNameCache.put(uid, pkgName);
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                    }
                }
            }
            Slog.i(LOG_TAG, "mUidToPkgNameCache: " + this.mUidToPkgNameCache + ", map: " + map);
        } catch (IllegalArgumentException e) {
            Slog.e(LOG_TAG, "getAllAppsPermInfo error");
        } catch (Throwable th2) {
            CursorHelper.closeCursor(null);
            throw th2;
        }
        CursorHelper.closeCursor(cursor);
        return map;
    }

    /* access modifiers changed from: private */
    public ArrayList<Integer> getUserIdForTableName(ArrayList<String> tableNameList) {
        ArrayList<Integer> userIdList = new ArrayList<>(10);
        int tableNameListSize = tableNameList.size();
        for (int i = 0; i < tableNameListSize; i++) {
            Slog.i(LOG_TAG, "table name: " + tableNameList.get(i));
            if (tableNameList.get(i).contains(PermConst.HW_PERM_SET_TABLE_NAME)) {
                String userIdString = tableNameList.get(i).replace(PermConst.HW_PERM_SET_TABLE_NAME, "");
                Slog.i(LOG_TAG, "userId: " + userIdString);
                if (!TextUtils.isEmpty(userIdString)) {
                    try {
                        userIdList.add(Integer.valueOf(Integer.parseInt(userIdString)));
                    } catch (NumberFormatException e) {
                        Slog.e(LOG_TAG, "NumberFormatException: " + e.getMessage());
                    }
                }
            }
        }
        return userIdList;
    }

    /* access modifiers changed from: package-private */
    public boolean shouldMonitor(int uid) {
        boolean z = false;
        if (uid < 10000) {
            return false;
        }
        Slog.i(LOG_TAG, "shouldMonitor mUidToPkgNameCache: " + this.mUidToPkgNameCache);
        synchronized (this.mHwPermissionCache) {
            if (this.mUidToPkgNameCache.indexOfKey(uid) >= 0) {
                z = true;
            }
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public int checkHwPermission(int uid, int pid, int permissionType) {
        if (IS_HW_DEBUG) {
            Slog.i(LOG_TAG, "checkHwPermission uid: " + uid + ", pid: " + pid);
        }
        long newPermissionType = toNewPermissionType(permissionType);
        if (!PermissionClass.isClassEType(newPermissionType)) {
            return 1;
        }
        String pkgName = Utils.getAppInfoByUidAndPid(this.mContext, uid, pid);
        if (TextUtils.isEmpty(pkgName)) {
            Slog.e(LOG_TAG, "pkgName: " + pkgName + "is empty");
            return 1;
        } else if (checkRestrictedPermission(uid, pkgName, permissionType) == 2) {
            return 2;
        } else {
            if (!shouldMonitor(uid)) {
                return 1;
            }
            int result = checkHwPerm(pkgName, newPermissionType, UserHandle.getUserId(uid));
            Slog.i(LOG_TAG, "checkHwPermissionForFwk: " + result);
            if (result != 2) {
                return 1;
            }
            return 2;
        }
    }

    private int checkRestrictedPermission(int uid, String pkgName, int permissionType) {
        if (((long) permissionType) != PermissionType.SEND_MMS) {
            return 1;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            PackageManager pm = this.mContext.getPackageManager();
            if (pm == null) {
                return 1;
            }
            if (!Utils.isPackageShouldMonitor(this.mContext, pkgName, uid)) {
                Binder.restoreCallingIdentity(identity);
                return 1;
            }
            int flag = pm.getPermissionFlags("android.permission.SEND_SMS", pkgName, UserHandle.getUserHandleForUid(uid));
            Set<String> whitePermissions = pm.getWhitelistedRestrictedPermissions(pkgName, 7);
            Slog.i(LOG_TAG, pkgName + ", flag:" + flag);
            if ((flag & AwarenessConstants.PHONE_STATE_CHANGED_ACTION) == 0 || whitePermissions.contains("android.permission.SEND_SMS")) {
                Binder.restoreCallingIdentity(identity);
                return 1;
            }
            Slog.i(LOG_TAG, pkgName + " is restricted");
            Binder.restoreCallingIdentity(identity);
            return 2;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static long toNewPermissionType(int value) {
        return ((long) value) & DEFAULT_TYPE_VALUE;
    }

    private void addHwPermissionCache(String pkgName, long permCfg, long permCode, int userId, int uid) {
        if (uid >= 10000) {
            synchronized (this.mHwPermissionCache) {
                if (uid >= 10000) {
                    try {
                        this.mUidToPkgNameCache.put(uid, pkgName);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                ArrayMap<String, DbPermissionItem> map = this.mHwPermissionCache.get(userId);
                if (map == null) {
                    Slog.e(LOG_TAG, "current user cache is null: " + userId);
                    map = new ArrayMap<>(10);
                    this.mHwPermissionCache.put(userId, map);
                }
                DbPermissionItem dbPermissionItem = map.get(pkgName);
                if (dbPermissionItem == null) {
                    map.put(pkgName, new DbPermissionItem(pkgName, permCfg, permCode, uid));
                    return;
                }
                try {
                    dbPermissionItem.setPermissionCode(permCode);
                    dbPermissionItem.setPermissionCfg(permCfg);
                    dbPermissionItem.setUid(uid);
                    map.put(pkgName, dbPermissionItem);
                    Slog.i(LOG_TAG, "replace dbPermissionItem: " + dbPermissionItem);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
    }
}
