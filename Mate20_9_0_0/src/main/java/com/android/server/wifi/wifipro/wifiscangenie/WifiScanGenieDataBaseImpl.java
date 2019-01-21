package com.android.server.wifi.wifipro.wifiscangenie;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class WifiScanGenieDataBaseImpl extends SQLiteOpenHelper {
    public static final String CHANNEL_TABLE_BSSID = "bssid";
    public static final String CHANNEL_TABLE_CELLID = "cellid";
    public static final String CHANNEL_TABLE_FREQUENCY = "frequency";
    public static final String CHANNEL_TABLE_NAME = "bssid_channel_tables";
    public static final String CHANNEL_TABLE_PRIORITY = "priority";
    public static final String CHANNEL_TABLE_SSID = "ssid";
    public static final String CHANNEL_TABLE_TIME = "time";
    public static final String CHANNEL_TABLE_TYPE = "ap_type";
    public static final String DATABASE_NAME = "/data/system/wifisangenie.db";
    public static final int DATABASE_VERSION = 2;
    public static final int SCAN_GENIE_MAX_RECORD = 2000;
    private static final String TAG = "WifiScanGenie_DataBaseImpl";
    private SQLiteDatabase mDatabase;
    private Object mLock = new Object();

    public static class ScanRecord {
        String bssid;
        int cellid;
        int frequency;
        int priority;
        String ssid;

        public ScanRecord(String bssid, String ssid, int frequency, int cellid) {
            this.bssid = bssid;
            this.ssid = ssid;
            this.frequency = frequency;
            this.cellid = cellid;
            this.priority = 0;
        }

        public ScanRecord(String bssid, String ssid, int priority) {
            this.bssid = bssid;
            this.ssid = ssid;
            this.priority = priority;
            this.cellid = -1;
            this.frequency = -1;
        }

        public ScanRecord(String bssid, String ssid, int frequency, int cellid, int priority) {
            this.bssid = bssid;
            this.ssid = ssid;
            this.frequency = frequency;
            this.cellid = cellid;
            this.priority = priority;
        }

        public String getBssid() {
            return this.bssid;
        }

        public String getSsid() {
            return this.ssid;
        }

        public int getCurrentFrequency() {
            return this.frequency;
        }

        public int getCellid() {
            return this.cellid;
        }

        public int gerPriority() {
            return this.priority;
        }
    }

    public WifiScanGenieDataBaseImpl(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public WifiScanGenieDataBaseImpl(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    public void onCreate(SQLiteDatabase db) {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE [bssid_channel_tables] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
        sBuffer.append("[bssid] VARCHAR (64),");
        sBuffer.append("[ssid] VARCHAR (64),");
        sBuffer.append("[frequency] INT (32),");
        sBuffer.append("[priority] INT (32),");
        sBuffer.append("[cellid] INT (32),");
        sBuffer.append("[time] INT (64),");
        sBuffer.append("[ap_type] INT DEFAULT (0) )");
        db.execSQL(sBuffer.toString());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS bssid_channel_tables");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS bssid_channel_tables");
        onCreate(db);
    }

    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeDB() {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    Log.e(TAG, " closeDB()");
                    this.mDatabase.close();
                }
            }
        }
    }

    public void openDB() {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                try {
                    this.mDatabase = getWritableDatabase();
                } catch (SQLiteCantOpenDatabaseException e) {
                    Log.w(TAG, "openDB(), can't open database!");
                }
            }
        }
    }

    private boolean isValidChannel(int frequency) {
        return frequency > 0;
    }

    /* JADX WARNING: Missing block: B:22:0x009f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addNewChannelRecord(String bssid, String ssid, int frequency, int cellids, int priority) {
        if (frequency == -100 || !(TextUtils.isEmpty(bssid) || TextUtils.isEmpty(ssid) || !isValidChannel(frequency))) {
            synchronized (this.mLock) {
                if (this.mDatabase != null) {
                    if (this.mDatabase.isOpen()) {
                        ContentValues values = new ContentValues();
                        values.put("bssid", bssid);
                        values.put("ssid", ssid);
                        values.put(CHANNEL_TABLE_FREQUENCY, Integer.valueOf(frequency));
                        values.put(CHANNEL_TABLE_CELLID, Integer.valueOf(cellids));
                        values.put(CHANNEL_TABLE_PRIORITY, Integer.valueOf(priority));
                        values.put(CHANNEL_TABLE_TIME, Long.valueOf(System.currentTimeMillis()));
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("try insert ");
                        stringBuilder.append(ssid);
                        stringBuilder.append(" , frequency:");
                        stringBuilder.append(frequency);
                        stringBuilder.append(" to db");
                        Log.i(str, stringBuilder.toString());
                        if (this.mDatabase.insert(CHANNEL_TABLE_NAME, null, values) > 0) {
                            Log.i(TAG, "insert succeed");
                        }
                    }
                }
                Log.w(TAG, "Database isnot opend!, ignor add");
                return;
            }
        }
        Log.w(TAG, "New Channel Record is illegal ! ignor add");
    }

    public void deleteLastRecords(String tableName) {
        if (TextUtils.isEmpty(tableName)) {
            Log.w(TAG, "tableName Record is illegal ! ignor delete");
            return;
        }
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("try delete ");
                    stringBuilder.append(tableName);
                    stringBuilder.append(" LastRecords form db");
                    Log.i(str, stringBuilder.toString());
                    try {
                        if (this.mDatabase.delete(CHANNEL_TABLE_NAME, " time = (select time from bssid_channel_tables order by time LIMIT 1) ", null) > 0) {
                            Log.i(TAG, "delete succeed");
                        }
                    } catch (SQLiteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("delete error SQLite: ");
                        stringBuilder2.append(e);
                        Log.i(str2, stringBuilder2.toString());
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor delete");
            return;
        }
    }

    public void deleteBssidRecord(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            Log.w(TAG, "bssid Record is illegal ! ignor delete");
            return;
        }
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("try delete ");
                    stringBuilder.append(bssid);
                    stringBuilder.append(" form db");
                    Log.i(str, stringBuilder.toString());
                    try {
                        if (this.mDatabase.delete(CHANNEL_TABLE_NAME, " bssid like ?", new String[]{bssid}) > 0) {
                            Log.i(TAG, "delete succeed");
                        }
                    } catch (SQLiteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("delete error SQLite: ");
                        stringBuilder2.append(e);
                        Log.i(str2, stringBuilder2.toString());
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor delete");
            return;
        }
    }

    public void deleteCellIdRecord(int cellId) {
        if (cellId <= 0) {
            Log.w(TAG, "cellId Record is illegal ! ignore delete");
            return;
        }
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                Log.w(TAG, "Database isnot opend!, ignore delete");
                return;
            }
            try {
                int ret = this.mDatabase.delete(CHANNEL_TABLE_NAME, " cellid = ?", new String[]{Integer.toString(cellId)});
                if (ret >= 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("deleteCellIdRecord, delete succeed, ret = ");
                    stringBuilder.append(ret);
                    Log.i(str, stringBuilder.toString());
                }
            } catch (SQLiteException e) {
                Log.i(TAG, "deleteCellIdRecord, delete error SQLite.");
            }
        }
    }

    public void deleteSsidRecord(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            Log.w(TAG, "ssid Record is illegal ! ignor delete");
            return;
        }
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("try delete ");
                    stringBuilder.append(ssid);
                    stringBuilder.append(" form db");
                    Log.i(str, stringBuilder.toString());
                    try {
                        if (this.mDatabase.delete(CHANNEL_TABLE_NAME, " ssid like ?", new String[]{ssid}) > 0) {
                            Log.i(TAG, "delete succeed");
                        }
                    } catch (SQLiteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("delete error SQLite: ");
                        stringBuilder2.append(e);
                        Log.i(str2, stringBuilder2.toString());
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor delete");
            return;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0060, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateBssidChannelRecord(String bssid, String ssid, int frequency, int priority) {
        if (TextUtils.isEmpty(bssid) || !isValidChannel(frequency)) {
            Log.w(TAG, "update Record is illegal ! ignor update");
            return;
        }
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    ContentValues values = new ContentValues();
                    values.put("ssid", ssid);
                    values.put(CHANNEL_TABLE_FREQUENCY, Integer.valueOf(frequency));
                    values.put(CHANNEL_TABLE_PRIORITY, Integer.valueOf(priority));
                    values.put(CHANNEL_TABLE_TIME, Long.valueOf(System.currentTimeMillis()));
                    if (this.mDatabase.update(CHANNEL_TABLE_NAME, values, " bssid like ?", new String[]{bssid}) > 0) {
                        Log.i(TAG, "update succeed");
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor update");
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0053, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateBssidPriorityRecord(String ssid, int priority) {
        if (TextUtils.isEmpty(ssid)) {
            Log.w(TAG, "update Record is illegal ! ignor update");
            return;
        }
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    ContentValues values = new ContentValues();
                    values.put(CHANNEL_TABLE_PRIORITY, Integer.valueOf(priority));
                    values.put(CHANNEL_TABLE_TIME, Long.valueOf(System.currentTimeMillis()));
                    if (this.mDatabase.update(CHANNEL_TABLE_NAME, values, " ssid like ?", new String[]{ssid}) > 0) {
                        Log.i(TAG, "update succeed");
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor update");
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0067, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:24:0x0068, code skipped:
            if (r3 != null) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:25:0x006a, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:30:0x007a, code skipped:
            if (r3 == null) goto L_0x007d;
     */
    /* JADX WARNING: Missing block: B:33:0x007e, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int queryTableSize(String tableName) {
        if (TextUtils.isEmpty(tableName)) {
            return 0;
        }
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                Log.w(TAG, "Database isnot opend!, ignor queryScanRecordsByCellid");
                return 0;
            }
            Cursor cursor = null;
            try {
                SQLiteDatabase sQLiteDatabase = this.mDatabase;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SELECT COUNT (_id) FROM ");
                stringBuilder.append(tableName);
                cursor = sQLiteDatabase.rawQuery(stringBuilder.toString(), null);
                if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int count = cursor.getInt(0) + 1;
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tableName);
                    stringBuilder.append(" count is : ");
                    stringBuilder.append(count);
                    Log.i(str, stringBuilder.toString());
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                try {
                    Log.e(TAG, e.getMessage());
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:32:0x009d, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:36:0x00ac, code skipped:
            if (r3 != null) goto L_0x00ae;
     */
    /* JADX WARNING: Missing block: B:38:?, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:41:0x00c9, code skipped:
            if (r3 == null) goto L_0x00cc;
     */
    /* JADX WARNING: Missing block: B:44:0x00cd, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<ScanRecord> queryScanRecordsByCellid(int cellid) {
        String str;
        synchronized (this.mLock) {
            Log.i(TAG, "queryScanRecordsByCellid enter");
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    List<ScanRecord> scanRecords = new ArrayList();
                    Cursor cursor = null;
                    if (cellid > 0) {
                        StringBuilder stringBuilder;
                        try {
                            SQLiteDatabase sQLiteDatabase = this.mDatabase;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("SELECT * FROM bssid_channel_tables WHERE cellid = ");
                            stringBuilder.append(cellid);
                            stringBuilder.append(" GROUP BY frequency ORDER BY time DESC ");
                            cursor = sQLiteDatabase.rawQuery(stringBuilder.toString(), null);
                        } catch (Exception e) {
                            try {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("queryScanRecordsByCellid:");
                                stringBuilder.append(e);
                                Log.e(str, stringBuilder.toString());
                            } catch (Throwable th) {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    } else {
                        cursor = this.mDatabase.rawQuery("SELECT * FROM bssid_channel_tables GROUP BY frequency ORDER BY time DESC LIMIT 5", null);
                    }
                    while (cursor != null && cursor.moveToNext()) {
                        str = cursor.getString(cursor.getColumnIndex("bssid"));
                        String ssid = cursor.getString(cursor.getColumnIndex("ssid"));
                        int frequency = cursor.getInt(cursor.getColumnIndex(CHANNEL_TABLE_FREQUENCY));
                        int priority = cursor.getInt(cursor.getColumnIndex(CHANNEL_TABLE_PRIORITY));
                        if (frequency == -100 || (!TextUtils.isEmpty(str) && isValidChannel(frequency))) {
                            scanRecords.add(new ScanRecord(str, ssid, frequency, cellid, priority));
                        } else {
                            Log.w(TAG, "queryScanRecordsByCellid Record is illegal ! ignor query");
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor queryScanRecordsByCellid");
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:25:0x0071, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:29:0x007f, code skipped:
            if (r3 != null) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:31:?, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:36:0x009e, code skipped:
            if (r3 == null) goto L_0x00a1;
     */
    /* JADX WARNING: Missing block: B:39:0x00a2, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<ScanRecord> queryScanRecordsByBssid(String bssid) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (TextUtils.isEmpty(bssid)) {
                        return null;
                    }
                    List<ScanRecord> scanRecords = new ArrayList();
                    Cursor cursor = null;
                    try {
                        cursor = this.mDatabase.rawQuery("SELECT * FROM bssid_channel_tables WHERE bssid LIKE ?", new String[]{bssid});
                        while (cursor.moveToNext()) {
                            String ssid = cursor.getString(cursor.getColumnIndex("ssid"));
                            int frequency = cursor.getInt(cursor.getColumnIndex(CHANNEL_TABLE_FREQUENCY));
                            int cellid = cursor.getInt(cursor.getColumnIndex(CHANNEL_TABLE_CELLID));
                            int priority = cursor.getInt(cursor.getColumnIndex(CHANNEL_TABLE_PRIORITY));
                            if (isValidChannel(frequency)) {
                                scanRecords.add(new ScanRecord(bssid, ssid, frequency, cellid, priority));
                            } else {
                                Log.w(TAG, "queryScanRecordsByBssid Record is illegal ! ignor query");
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    } catch (Exception e) {
                        try {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("queryScanRecordsByBssid:");
                            stringBuilder.append(e);
                            Log.e(str, stringBuilder.toString());
                        } catch (Throwable th) {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                }
            }
            Log.w(TAG, "Database isnot opend!, ignor queryScanRecordsByCellid");
            return null;
        }
    }
}
