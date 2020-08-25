package com.android.server.security.permissionmanager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.security.permissionmanager.util.CursorHelper;
import com.android.server.security.permissionmanager.util.PermConst;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HwPermDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DB_NAME = "hw_permission.db";
    private static final int DEFAULT_CAPACITY_ARRAY = 10;
    private static final int DEFAULT_ROW = 0;
    private static final String GET_TABLE_NAME = "select name from sqlite_master where type='table' order by name";
    private static final int ILLEGAL_ROW = -1;
    private static final int ILLEGAL_USER_ID = -1;
    private static final Object LOCK = new Object();
    private static final String LOG_TAG = "HwPermDbHelper";
    private static final int OWNER_USER_ID = 0;
    private static volatile HwPermDbHelper sUniqueInstance = null;
    /* access modifiers changed from: private */
    public Context mContext = null;
    private SQLiteDatabase mDatabase;
    private ExecutorService mPermissionSingleExecutor = null;
    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        /* class com.android.server.security.permissionmanager.HwPermDbHelper.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (context != null && intent != null) {
                String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    Slog.e(HwPermDbHelper.LOG_TAG, "onReceive action is null");
                    return;
                }
                Slog.v(HwPermDbHelper.LOG_TAG, "mUserChangeReceiver action = " + action);
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    HwPermDbHelper.this.dropPermTable(HwPermDbAdapter.getPermCfgTableName(userId));
                    HwPermDbAdapter.getInstance(HwPermDbHelper.this.mContext).removeHwPermissionCache(null, userId);
                } else if ("android.intent.action.USER_ADDED".equals(action)) {
                    HwPermDbHelper.this.createPermissionCfgTableAsUser(userId);
                } else {
                    Slog.e(HwPermDbHelper.LOG_TAG, "error Intent");
                }
            }
        }
    };

    private HwPermDbHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, 2);
        this.mContext = context;
        openDatabase();
        registerUserChange();
        if (this.mPermissionSingleExecutor == null) {
            this.mPermissionSingleExecutor = Executors.newSingleThreadExecutor();
        }
        Slog.v(LOG_TAG, "create HwPermDbHelper:");
    }

    public static HwPermDbHelper getInstance(Context context) {
        if (sUniqueInstance == null) {
            synchronized (LOCK) {
                if (sUniqueInstance == null) {
                    sUniqueInstance = new HwPermDbHelper(context);
                }
            }
        }
        return sUniqueInstance;
    }

    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Slog.i(LOG_TAG, "onCreate HwPermDbHelper");
        this.mPermissionSingleExecutor = Executors.newSingleThreadExecutor();
        this.mPermissionSingleExecutor.submit(new Runnable(sqLiteDatabase) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$nGs1wFOEBKqRbOPtt_FHcZkTc */
            private final /* synthetic */ SQLiteDatabase f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$onCreate$0$HwPermDbHelper(this.f$1);
            }
        });
        this.mPermissionSingleExecutor.submit(new Runnable(sqLiteDatabase) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$TpIsGY0L6vwQAG34FZrikCa4Ke0 */
            private final /* synthetic */ SQLiteDatabase f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$onCreate$1$HwPermDbHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$onCreate$0$HwPermDbHelper(SQLiteDatabase sqLiteDatabase) {
        createPermissionCfgTable(sqLiteDatabase, 0);
    }

    public /* synthetic */ void lambda$onCreate$1$HwPermDbHelper(SQLiteDatabase sqLiteDatabase) {
        createSmsBlockTable(sqLiteDatabase, 0);
    }

    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Slog.i(LOG_TAG, "onUpgrade oldVersion:" + oldVersion + " newVersion " + newVersion);
        if (this.mPermissionSingleExecutor == null) {
            this.mPermissionSingleExecutor = Executors.newSingleThreadExecutor();
        }
        this.mPermissionSingleExecutor.submit(new Runnable(sqLiteDatabase) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$wkFZSpvRD25rLHwxFtqZo6DDc */
            private final /* synthetic */ SQLiteDatabase f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$onUpgrade$2$HwPermDbHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$onUpgrade$2$HwPermDbHelper(SQLiteDatabase sqLiteDatabase) {
        createSmsBlockTable(sqLiteDatabase, 0);
    }

    private void createPermissionCfgTable(SQLiteDatabase db, int userId) {
        String tableName = HwPermDbAdapter.getPermCfgTableName(userId);
        db.execSQL("create table if not exists " + tableName + " ( " + "packageName" + " text primary key, " + "uid" + " int DEFAULT (-1), " + PermConst.PERMISSION_CODE + " bigint DEFAULT (0), " + PermConst.PERMISSION_CFG + " bigint DEFAULT (0));");
        StringBuilder sb = new StringBuilder();
        sb.append("has created table: ");
        sb.append(tableName);
        Slog.i(LOG_TAG, sb.toString());
    }

    private void createSmsBlockTable(SQLiteDatabase db, int userId) {
        String tableName = HwSmsBlockDbAdapter.getSmsBlockTableName(userId);
        db.execSQL("create table if not exists " + tableName + " ( " + "packageName" + " text primary key, " + PermConst.SUB_PERMISSION + " int DEFAULT (0), " + PermConst.PRE_DEFINED_SMS_CONFIG + " int DEFAULT (0));");
        StringBuilder sb = new StringBuilder();
        sb.append("has created table: ");
        sb.append(tableName);
        Slog.i(LOG_TAG, sb.toString());
    }

    private void openDatabase() {
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase == null) {
            this.mDatabase = getWritableDatabase();
            Slog.i(LOG_TAG, "HwPermDbHelper open Database:" + this.mDatabase);
        } else if (!new File(sQLiteDatabase.getPath()).exists()) {
            Slog.i(LOG_TAG, " db file is not exist, close db ");
            closeDatabase();
        }
    }

    private void closeDatabase() {
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase != null) {
            sQLiteDatabase.close();
            this.mDatabase = null;
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Slog.i(LOG_TAG, "onDowngrade oldVersion:" + oldVersion + " newVersion " + newVersion);
    }

    /* access modifiers changed from: package-private */
    public Cursor query(String table, String selection, String[] selectionArgs) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                return db.query(table, null, selection, selectionArgs, null, null, null);
            }
            Slog.e(LOG_TAG, "query db is null");
            return null;
        } catch (SQLException e) {
            Slog.e(LOG_TAG, "query SQLException");
            return null;
        }
    }

    /* access modifiers changed from: package-private */
    public void delete(String tableName, String whereClause, String[] whereArgs) {
        this.mPermissionSingleExecutor.submit(new Runnable(tableName, whereClause, whereArgs) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$KjQ8xhZhVszBtbAyi_YuPX3_Bj0 */
            private final /* synthetic */ String f$1;
            private final /* synthetic */ String f$2;
            private final /* synthetic */ String[] f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$delete$3$HwPermDbHelper(this.f$1, this.f$2, this.f$3);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: deleteRunnable */
    public void lambda$delete$3$HwPermDbHelper(String tableName, String whereClause, String[] whereArgs) {
        openDatabase();
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase == null) {
            Slog.e(LOG_TAG, "mDatabase can't be null");
            return;
        }
        try {
            int result = sQLiteDatabase.delete(tableName, whereClause, whereArgs);
            Slog.v(LOG_TAG, "delete result: " + result);
        } catch (SQLException e) {
            Slog.e(LOG_TAG, "db delete Exception");
        }
    }

    /* access modifiers changed from: package-private */
    public void update(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        this.mPermissionSingleExecutor.submit(new Runnable(tableName, values, whereClause, whereArgs) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$UjXiq0EP_4Nn3fYEcXFukdBmKLY */
            private final /* synthetic */ String f$1;
            private final /* synthetic */ ContentValues f$2;
            private final /* synthetic */ String f$3;
            private final /* synthetic */ String[] f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$update$4$HwPermDbHelper(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: updateRunnable */
    public void lambda$update$4$HwPermDbHelper(String tableName, ContentValues values, String whereClause, String[] args) {
        openDatabase();
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase == null) {
            Slog.e(LOG_TAG, "mDatabase can't be null");
            return;
        }
        try {
            int result = sQLiteDatabase.update(tableName, values, whereClause, args);
            Slog.v(LOG_TAG, "update result: " + result);
        } catch (SQLException e) {
            Slog.e(LOG_TAG, "db delete Exception");
        }
    }

    /* access modifiers changed from: package-private */
    public void insert(ContentValues values, String tableName) {
        this.mPermissionSingleExecutor.submit(new Runnable(values, tableName) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$ir8PXniW2D2TtxQAIZMzfZZIZl8 */
            private final /* synthetic */ ContentValues f$1;
            private final /* synthetic */ String f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$insert$5$HwPermDbHelper(this.f$1, this.f$2);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: insertRunnable */
    public void lambda$insert$5$HwPermDbHelper(ContentValues values, String tableName) {
        openDatabase();
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase == null) {
            Slog.e(LOG_TAG, "mDatabase can't be null");
            return;
        }
        long result = sQLiteDatabase.insert(tableName, null, values);
        Slog.v(LOG_TAG, "insert result: " + result);
    }

    /* access modifiers changed from: package-private */
    public void replace(ContentValues value, String tableName) {
        this.mPermissionSingleExecutor.submit(new Runnable(value, tableName) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$nx2nb6t2YebS7F7FmPegpnm3zM */
            private final /* synthetic */ ContentValues f$1;
            private final /* synthetic */ String f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$replace$6$HwPermDbHelper(this.f$1, this.f$2);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: replaceRunnable */
    public void lambda$replace$6$HwPermDbHelper(ContentValues value, String tableName) {
        openDatabase();
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase == null) {
            Slog.e(LOG_TAG, "mDatabase can't be null");
            return;
        }
        long result = sQLiteDatabase.replace(tableName, null, value);
        Slog.v(LOG_TAG, "replace result: " + result);
    }

    private void registerUserChange() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(this.mUserChangeReceiver, filter);
    }

    /* access modifiers changed from: private */
    public void dropPermTable(String tableName) {
        this.mPermissionSingleExecutor.submit(new Runnable(tableName) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$md0mkzwwlBhtE981rXD_lLe3Zl0 */
            private final /* synthetic */ String f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$dropPermTable$7$HwPermDbHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$dropPermTable$7$HwPermDbHelper(String tableName) {
        try {
            openDatabase();
            if (this.mDatabase == null) {
                Slog.e(LOG_TAG, "mDatabase can't be null");
                return;
            }
            SQLiteDatabase sQLiteDatabase = this.mDatabase;
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS " + tableName);
        } catch (SQLiteException e) {
            Slog.e(LOG_TAG, "dropPermTable SQLiteException");
        }
    }

    /* access modifiers changed from: private */
    public void createPermissionCfgTableAsUser(int userId) {
        this.mPermissionSingleExecutor.submit(new Runnable(userId) {
            /* class com.android.server.security.permissionmanager.$$Lambda$HwPermDbHelper$1CfvqBRYi0IghimHpFmZYALQhZ4 */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwPermDbHelper.this.lambda$createPermissionCfgTableAsUser$8$HwPermDbHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$createPermissionCfgTableAsUser$8$HwPermDbHelper(int userId) {
        try {
            openDatabase();
            if (this.mDatabase == null) {
                Slog.e(LOG_TAG, "mDatabase can't be null");
            } else {
                createPermissionCfgTable(this.mDatabase, userId);
            }
        } catch (SQLiteException e) {
            Slog.e(LOG_TAG, "dropPermTable SQLiteException");
        }
    }

    public ArrayList<String> getTableNames() {
        ArrayList<String> list = new ArrayList<>(10);
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (db == null) {
                CursorHelper.closeCursor(null);
                return list;
            }
            cursor = db.rawQuery(GET_TABLE_NAME, null);
            if (CursorHelper.checkCursorValid(cursor)) {
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0));
                }
            }
            CursorHelper.closeCursor(cursor);
            return list;
        } catch (SQLiteException e) {
            Slog.e(LOG_TAG, "getAllAppsPermInfo error");
        } catch (Throwable th) {
            CursorHelper.closeCursor(null);
            throw th;
        }
    }
}
