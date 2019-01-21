package com.android.server.wifi.MSS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HwMSSBlackListManager implements IHwMSSBlacklistMgr {
    private static final int DATA_BASE_MAX_NUM = 500;
    private static final boolean DEBUG = false;
    private static final String TAG = "HwMSSBlackListManager";
    private static final long TIME_EXPIRED = 86400000;
    private static HwMSSBlackListManager mHwMSSBlackListManager = null;
    private SQLiteDatabase mDatabase;
    private HwMSSBlackListHelper mHelper;
    private Object mLock = new Object();

    class HwMSSBlackListHelper extends SQLiteOpenHelper {
        public static final String BLACKLIST_TABLE_NAME = "BlackListTable";
        private static final String DATABASE_NAME = "/data/system/HwMSSBlackList.db";
        private static final int DATABASE_VERSION = 1;

        public HwMSSBlackListHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        public HwMSSBlackListHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            StringBuffer sBuffer = new StringBuffer();
            sBuffer.append("CREATE TABLE [BlackListTable] (");
            sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
            sBuffer.append("[ssid] TEXT,");
            sBuffer.append("[bssid] TEXT,");
            sBuffer.append("[reason_code] INTEGER,");
            sBuffer.append("[direction] INTEGER,");
            sBuffer.append("[update_time] LONG,");
            sBuffer.append("[reserved] INTEGER)");
            db.execSQL(sBuffer.toString());
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS BlackListTable");
            onCreate(db);
        }
    }

    private HwMSSBlackListManager(Context context) {
        try {
            this.mHelper = new HwMSSBlackListHelper(context);
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            this.mDatabase = null;
            Log.e(TAG, "SQLiteDatabase is null, error!");
        }
    }

    public static synchronized IHwMSSBlacklistMgr getInstance(Context context) {
        HwMSSBlackListManager hwMSSBlackListManager;
        synchronized (HwMSSBlackListManager.class) {
            if (mHwMSSBlackListManager == null) {
                mHwMSSBlackListManager = new HwMSSBlackListManager(context);
            }
            hwMSSBlackListManager = mHwMSSBlackListManager;
        }
        return hwMSSBlackListManager;
    }

    /* JADX WARNING: Missing block: B:12:0x0018, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeDB() {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    this.mDatabase.close();
                }
            }
        }
    }

    public boolean addToBlacklist(String ssid, String bssid, int reasoncode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Try to add to blacklist. ssid:");
        stringBuilder.append(ssid);
        stringBuilder.append("; bssid:");
        stringBuilder.append(hideBssid(bssid));
        stringBuilder.append("; reasoncode: ");
        stringBuilder.append(reasoncode);
        dbg(stringBuilder.toString());
        synchronized (this.mLock) {
            StringBuilder stringBuilder2;
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (ssid != null) {
                    bssid = hideBssid(bssid);
                    boolean inlineUpdateBlacklist;
                    if (inlineIsRecordInDatabase(ssid)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(ssid);
                        stringBuilder2.append(" is already in blacklist, update it.");
                        dbg(stringBuilder2.toString());
                        inlineUpdateBlacklist = inlineUpdateBlacklist(ssid, bssid, reasoncode);
                        return inlineUpdateBlacklist;
                    }
                    inlineCheckIfDatabaseFull();
                    inlineUpdateBlacklist = inlineAddBlacklist(ssid, bssid, reasoncode);
                    return inlineUpdateBlacklist;
                }
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to add to blacklist. ssid: ");
            stringBuilder2.append(ssid);
            dbg(stringBuilder2.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:20:0x005d, code skipped:
            if (r2 != null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:27:0x006d, code skipped:
            if (r2 == null) goto L_0x0070;
     */
    /* JADX WARNING: Missing block: B:30:0x0071, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:35:0x0079, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isInBlacklist(String ssid) {
        boolean find = false;
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || ssid == null) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM BlackListTable where ssid like ?", new String[]{ssid});
                    if (c != null && c.moveToNext()) {
                        find = true;
                    }
                    if (find) {
                        long time = c.getLong(c.getColumnIndex("update_time"));
                        if (System.currentTimeMillis() - time > TIME_EXPIRED) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(ssid);
                            stringBuilder.append(" is expired, last update time: ");
                            stringBuilder.append(time);
                            dbg(stringBuilder.toString());
                            find = false;
                        }
                    }
                } catch (SQLException e) {
                    try {
                        Log.w(TAG, "Failed to query the blacklist");
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    public boolean addToBlacklist(HwMSSDatabaseItem item) {
        return addToBlacklist(item.ssid, item.bssid, item.reasoncode);
    }

    public boolean isInBlacklistByBssid(String bssid) {
        return false;
    }

    public List<HwMSSDatabaseItem> getBlacklist(boolean noexpired) {
        return new ArrayList();
    }

    /* JADX WARNING: Missing block: B:14:0x0019, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void delBlacklistBySsid(String ssid) {
        if (ssid != null) {
            synchronized (this.mLock) {
                if (this.mDatabase != null) {
                    if (this.mDatabase.isOpen()) {
                        inlineDeleteBlacklistBySsid(ssid);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void delBlacklistAll() {
        dbg("delete all blacklist");
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    this.mDatabase.execSQL("DELETE FROM BlackListTable");
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0082, code skipped:
            if (r2 != null) goto L_0x0084;
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:22:0x0092, code skipped:
            if (r2 == null) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:25:0x0096, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:30:0x009e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void dumpBlacklist() {
        dbg("dump blacklist:");
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM BlackListTable", null);
                    while (c != null && c.moveToNext()) {
                        String ssid = c.getString(c.getColumnIndex("ssid"));
                        String bssid = c.getString(c.getColumnIndex("bssid"));
                        int reason = c.getInt(c.getColumnIndex("reason_code"));
                        long time = c.getLong(c.getColumnIndex("update_time"));
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ssid:");
                        stringBuilder.append(ssid);
                        stringBuilder.append(";bssid:");
                        stringBuilder.append(hideBssid(bssid));
                        stringBuilder.append(";reason_code:");
                        stringBuilder.append(reason);
                        stringBuilder.append(";time:");
                        stringBuilder.append(time);
                        dbg(stringBuilder.toString());
                    }
                } catch (SQLException e) {
                    try {
                        Log.w(TAG, "Failed to dump the blacklist");
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    private void inlineDeleteBlacklistBySsid(String ssid) {
        if (ssid != null) {
            this.mDatabase.delete("BlackListTable", "ssid like ?", new String[]{ssid});
        }
    }

    private boolean inlineAddBlacklist(String ssid, String bssid, int reasoncode) {
        try {
            this.mDatabase.execSQL("INSERT INTO BlackListTable VALUES(null, ?, ?, ?, ?, ?, ?)", new Object[]{ssid, bssid, Integer.valueOf(reasoncode), Integer.valueOf(-1), Long.valueOf(System.currentTimeMillis()), Integer.valueOf(0)});
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "Failed to add the blacklist");
            return false;
        }
    }

    private boolean inlineUpdateBlacklist(String ssid, String bssid, int reasoncode) {
        ContentValues values = new ContentValues();
        values.put("bssid", bssid);
        values.put("ssid", ssid);
        values.put("reason_code", Integer.valueOf(reasoncode));
        values.put("update_time", Long.valueOf(System.currentTimeMillis()));
        this.mDatabase.update("BlackListTable", values, "ssid like ?", new String[]{ssid});
        return true;
    }

    /* JADX WARNING: Missing block: B:11:0x0037, code skipped:
            if (r5 != null) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:12:0x0039, code skipped:
            r5.close();
     */
    /* JADX WARNING: Missing block: B:17:0x0047, code skipped:
            if (r5 == null) goto L_0x004a;
     */
    /* JADX WARNING: Missing block: B:19:0x004c, code skipped:
            if (r3 < 500) goto L_?;
     */
    /* JADX WARNING: Missing block: B:20:0x004e, code skipped:
            if (r2 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:21:0x0050, code skipped:
            inlineDeleteBlacklistBySsid(r2);
     */
    /* JADX WARNING: Missing block: B:29:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:31:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void inlineCheckIfDatabaseFull() {
        long earliestTime = Long.MAX_VALUE;
        String delSsid = null;
        int dataNum = 0;
        Cursor c = null;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM BlackListTable", null);
            while (c != null && c.moveToNext()) {
                dataNum++;
                long time = c.getLong(c.getColumnIndex("update_time"));
                String ssid = c.getString(c.getColumnIndex("ssid"));
                if (time < earliestTime) {
                    earliestTime = time;
                    delSsid = ssid;
                }
            }
        } catch (SQLException e) {
            Log.w(TAG, "Failed to check the blacklist");
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x001a, code skipped:
            if (r1 != null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:8:0x001c, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:13:0x002a, code skipped:
            if (r1 == null) goto L_0x002d;
     */
    /* JADX WARNING: Missing block: B:14:0x002d, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean inlineIsRecordInDatabase(String ssid) {
        boolean find = false;
        Cursor c = null;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM BlackListTable where ssid like ?", new String[]{ssid});
            if (c != null && c.moveToNext()) {
                find = true;
            }
        } catch (SQLException e) {
            Log.w(TAG, "Failed to query the blacklist when check records");
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    private String hideBssid(String bssid) {
        if (bssid == null || !bssid.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")) {
            return "unknown";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(bssid.substring(0, 6));
        stringBuilder.append("xx:xx");
        stringBuilder.append(bssid.substring(11));
        return stringBuilder.toString();
    }

    private void dbg(String msg) {
    }
}
