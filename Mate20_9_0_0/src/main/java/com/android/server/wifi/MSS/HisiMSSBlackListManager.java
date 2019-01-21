package com.android.server.wifi.MSS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HisiMSSBlackListManager implements IHwMSSBlacklistMgr {
    private static final int DATA_BASE_MAX_NUM = 64;
    private static final boolean DEBUG = false;
    private static final String TAG = "HisiMSSBlackListManager";
    private static final long TIME_EXPIRED = 86400000;
    private static final int TYPE_QUERY_BY_BSSID = 1;
    private static final int TYPE_QUERY_BY_SSID = 0;
    private static HisiMSSBlackListManager mHisiMSSBlackListManager = null;
    private SQLiteDatabase mDatabase;
    private HisiMSSBlackListHelper mHelper;
    private Object mLock = new Object();

    static class HisiMSSBlackListHelper extends SQLiteOpenHelper {
        public static final String BLACKLIST_TABLE_NAME = "BlackListTable";
        private static final String DATABASE_NAME = "/data/system/HisiMSSBlackList.db";
        private static final int DATABASE_VERSION = 1;

        public HisiMSSBlackListHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        public HisiMSSBlackListHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            StringBuffer sBuffer = new StringBuffer();
            sBuffer.append("CREATE TABLE [");
            sBuffer.append("BlackListTable");
            sBuffer.append("] (");
            sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
            sBuffer.append("[ssid] TEXT,");
            sBuffer.append("[bssid] TEXT,");
            sBuffer.append("[action_type] INTEGER,");
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

    private HisiMSSBlackListManager(Context context) {
        try {
            this.mHelper = new HisiMSSBlackListHelper(context);
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            this.mDatabase = null;
            Log.e(TAG, "SQLiteDatabase is null, error!");
        }
    }

    public static synchronized IHwMSSBlacklistMgr getInstance(Context context) {
        HisiMSSBlackListManager hisiMSSBlackListManager;
        synchronized (HisiMSSBlackListManager.class) {
            if (mHisiMSSBlackListManager == null) {
                mHisiMSSBlackListManager = new HisiMSSBlackListManager(context);
            }
            hisiMSSBlackListManager = mHisiMSSBlackListManager;
        }
        return hisiMSSBlackListManager;
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

    public boolean addToBlacklist(HwMSSDatabaseItem item) {
        if (item != null) {
            return addToBlacklist(item.ssid, item.bssid, item.reasoncode);
        }
        return false;
    }

    public boolean addToBlacklist(String ssid, String bssid, int actiontype) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Try to add to blacklist. ssid:");
        stringBuilder.append(ssid);
        stringBuilder.append("; bssid:");
        stringBuilder.append(hideBssid(bssid));
        stringBuilder.append("; actiontype: ");
        stringBuilder.append(actiontype);
        HwMSSUtils.logd(str, stringBuilder.toString());
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (bssid != null) {
                    boolean innerUpdateBlacklist;
                    if (innerIsRecordInDatabase(1, bssid, false)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(bssid);
                        stringBuilder.append(" is already in blacklist, update it.");
                        dbg(stringBuilder.toString());
                        innerUpdateBlacklist = innerUpdateBlacklist(ssid, bssid, actiontype);
                        return innerUpdateBlacklist;
                    }
                    innerCheckIfDatabaseFull();
                    innerUpdateBlacklist = innerAddBlacklist(ssid, bssid, actiontype);
                    return innerUpdateBlacklist;
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to add to blacklist. bssid: ");
            stringBuilder.append(bssid);
            dbg(stringBuilder.toString());
            return false;
        }
    }

    public boolean isInBlacklist(String ssid) {
        return innerIsRecordInDatabase(0, ssid, true);
    }

    public boolean isInBlacklistByBssid(String bssid) {
        return innerIsRecordInDatabase(1, bssid, true);
    }

    /* JADX WARNING: Missing block: B:20:0x009b, code skipped:
            if (r2 != null) goto L_0x009d;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:27:0x00ab, code skipped:
            if (r2 == null) goto L_0x00ae;
     */
    /* JADX WARNING: Missing block: B:30:0x00af, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:35:0x00b7, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<HwMSSDatabaseItem> getBlacklist(boolean noexpired) {
        ArrayList<HwMSSDatabaseItem> lists = new ArrayList();
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    Cursor c = null;
                    long curr = System.currentTimeMillis();
                    try {
                        c = this.mDatabase.rawQuery("SELECT * FROM BlackListTable", null);
                        while (c != null && c.moveToNext()) {
                            String ssid = c.getString(c.getColumnIndex("ssid"));
                            String bssid = c.getString(c.getColumnIndex("bssid"));
                            int actiontype = c.getInt(c.getColumnIndex("action_type"));
                            long time = c.getLong(c.getColumnIndex("update_time"));
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("ssid:");
                            stringBuilder.append(ssid);
                            stringBuilder.append(";bssid:");
                            stringBuilder.append(hideBssid(bssid));
                            stringBuilder.append(";action_type:");
                            stringBuilder.append(actiontype);
                            stringBuilder.append(";time:");
                            stringBuilder.append(time);
                            dbg(stringBuilder.toString());
                            if (!noexpired || curr - time < TIME_EXPIRED) {
                                HwMSSDatabaseItem item = new HwMSSDatabaseItem(ssid, bssid, actiontype);
                                item.updatetime = time;
                                lists.add(item);
                            }
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
    }

    /* JADX WARNING: Missing block: B:12:0x0016, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void delBlacklistByBssid(String bssid) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    innerDeleteBlacklistByBssid(bssid);
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
                        int actiontype = c.getInt(c.getColumnIndex("action_type"));
                        long time = c.getLong(c.getColumnIndex("update_time"));
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ssid:");
                        stringBuilder.append(ssid);
                        stringBuilder.append(";bssid:");
                        stringBuilder.append(hideBssid(bssid));
                        stringBuilder.append(";action_type:");
                        stringBuilder.append(actiontype);
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

    /* JADX WARNING: Missing block: B:25:0x007c, code skipped:
            if (r7 != null) goto L_0x007e;
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:32:0x008c, code skipped:
            if (r7 == null) goto L_0x008f;
     */
    /* JADX WARNING: Missing block: B:35:0x0090, code skipped:
            return r3;
     */
    /* JADX WARNING: Missing block: B:40:0x0098, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean innerIsRecordInDatabase(int type, String value, boolean noexpired) {
        String str = value;
        boolean find = false;
        String strCond = type == 0 ? "ssid" : "bssid";
        String strQuery = "";
        String strQuery2 = String.format(Locale.US, "SELECT * FROM %s where %s like ?", new Object[]{"BlackListTable", strCond});
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (str != null) {
                    Cursor c = null;
                    long curr = System.currentTimeMillis();
                    try {
                        c = this.mDatabase.rawQuery(strQuery2, new String[]{str});
                        while (c != null && c.moveToNext()) {
                            if (noexpired) {
                                long time = c.getLong(c.getColumnIndex("update_time"));
                                if (curr - time > TIME_EXPIRED) {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(str);
                                    stringBuilder.append(" is expired, last update time: ");
                                    stringBuilder.append(time);
                                    dbg(stringBuilder.toString());
                                }
                            }
                            find = true;
                            break;
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
    }

    private void innerDeleteBlacklistByBssid(String bssid) {
        if (!TextUtils.isEmpty(bssid)) {
            this.mDatabase.delete("BlackListTable", "bssid like ?", new String[]{bssid});
        }
    }

    private boolean innerAddBlacklist(String ssid, String bssid, int actiontype) {
        String strcmd = String.format(Locale.US, "INSERT INTO %s VALUES(null, ?, ?, ?, ?, ?, ?)", new Object[]{"BlackListTable"});
        try {
            this.mDatabase.execSQL(strcmd, new Object[]{ssid, bssid, Integer.valueOf(actiontype), Integer.valueOf(-1), Long.valueOf(System.currentTimeMillis()), Integer.valueOf(0)});
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "Failed to add the blacklist");
            return false;
        }
    }

    private boolean innerUpdateBlacklist(String ssid, String bssid, int actiontype) {
        ContentValues values = new ContentValues();
        values.put("bssid", bssid);
        values.put("ssid", ssid);
        values.put("action_type", Integer.valueOf(actiontype));
        values.put("update_time", Long.valueOf(System.currentTimeMillis()));
        this.mDatabase.update("BlackListTable", values, "bssid like ?", new String[]{bssid});
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
            if (r3 < DATA_BASE_MAX_NUM) goto L_?;
     */
    /* JADX WARNING: Missing block: B:20:0x004e, code skipped:
            if (r2 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:21:0x0050, code skipped:
            innerDeleteBlacklistByBssid(r2);
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
    private void innerCheckIfDatabaseFull() {
        long earliestTime = Long.MAX_VALUE;
        String delBssid = null;
        int dataNum = 0;
        Cursor c = null;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM BlackListTable", null);
            while (c != null && c.moveToNext()) {
                dataNum++;
                long time = c.getLong(c.getColumnIndex("update_time"));
                String bssid = c.getString(c.getColumnIndex("bssid"));
                if (time < earliestTime) {
                    earliestTime = time;
                    delBssid = bssid;
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
