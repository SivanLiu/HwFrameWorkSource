package android.net.dhcp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.LruCache;

public class DhcpResultsInfoDBManager {
    private static final int DHCP_RESULTS_DB_SIZE = 50;
    private static final String TAG = "DhcpResultsInfoDBManager";
    private static DhcpResultsInfoDBManager mDataBaseManager;
    private SQLiteDatabase mDatabase;
    private DhcpResultsInfoDBHelper mHelper;
    private Object mdbLock = new Object();

    public DhcpResultsInfoDBManager(Context context) {
        this.mHelper = new DhcpResultsInfoDBHelper(context);
        this.mDatabase = this.mHelper.getWritableDatabase();
    }

    public static DhcpResultsInfoDBManager getInstance(Context context) {
        if (mDataBaseManager == null) {
            mDataBaseManager = new DhcpResultsInfoDBManager(context);
        }
        return mDataBaseManager;
    }

    private boolean deleteHistoryRecord(String dbTableName, String apssid) {
        synchronized (this.mdbLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (apssid == null) {
                        Log.e(TAG, "deleteHistoryRecord null error.");
                        return false;
                    }
                    try {
                        this.mDatabase.delete(dbTableName, "apSSID like ?", new String[]{apssid});
                        return true;
                    } catch (SQLException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("deleteHistoryRecord error:");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        return false;
                    }
                }
            }
            Log.e(TAG, "deleteHistoryRecord database error.");
            return false;
        }
    }

    public boolean deleteDhcpResultsInfoRecord(String apssid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deleteApInfoRecord delete record of ssid:");
        stringBuilder.append(apssid);
        Log.d(str, stringBuilder.toString());
        return deleteHistoryRecord(DhcpResultsInfoDBHelper.DHCP_RESULTS_INFO_DB_NAME, apssid);
    }

    private boolean checkHistoryRecordExist(String dbTableName, String apssid) {
        boolean ret = false;
        Cursor c = null;
        String str;
        StringBuilder stringBuilder;
        try {
            SQLiteDatabase sQLiteDatabase = this.mDatabase;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SELECT * FROM ");
            stringBuilder2.append(dbTableName);
            stringBuilder2.append(" where apSSID like ?");
            c = sQLiteDatabase.rawQuery(stringBuilder2.toString(), new String[]{apssid});
            int rcdCount = c.getCount();
            if (rcdCount > 0) {
                ret = true;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkHistoryRecordExist read from:");
            stringBuilder.append(dbTableName);
            stringBuilder.append(", get record: ");
            stringBuilder.append(rcdCount);
            Log.d(str, stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return ret;
        } catch (SQLException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkHistoryRecordExist error:");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return false;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private boolean updateDhcpResultsInfoRecord(DhcpResultsInfoRecord dbr) {
        ContentValues values = new ContentValues();
        values.put("apSSID", dbr.apSSID);
        values.put("IP", dbr.staIP);
        values.put("DHCPServer", dbr.apDhcpServer);
        values.put("EX1", "ex1");
        values.put("EX2", "ex2");
        try {
            int rowChg = this.mDatabase.update(DhcpResultsInfoDBHelper.DHCP_RESULTS_INFO_DB_NAME, values, "apSSID like ?", new String[]{dbr.apSSID});
            if (rowChg == 0) {
                Log.e(TAG, "updateDhcpResultsInfoRecord update failed.");
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDhcpResultsInfoRecord update succ, rowChg=");
            stringBuilder.append(rowChg);
            Log.d(str, stringBuilder.toString());
            return true;
        } catch (SQLException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateDhcpResultsInfoRecord error:");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    private boolean insertDhcpResultsInfoRecord(DhcpResultsInfoRecord dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO DhcpResults VALUES(?, ?, ?, ?, ?)", new Object[]{dbr.apSSID, dbr.staIP, dbr.apDhcpServer, "ex1", "ex2"});
            Log.i(TAG, "insertDhcpResultsInfoRecord add a record succ.");
            return true;
        } catch (SQLException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertDhcpResultsInfoRecord error:");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean addOrUpdateDhcpResultsInfoRecord(DhcpResultsInfoRecord dbr) {
        synchronized (this.mdbLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (dbr != null) {
                    boolean updateDhcpResultsInfoRecord;
                    if (dbr.apSSID == null) {
                        Log.e(TAG, "addOrUpdateApInfoRecord null error.");
                        return false;
                    } else if (checkHistoryRecordExist(DhcpResultsInfoDBHelper.DHCP_RESULTS_INFO_DB_NAME, dbr.apSSID)) {
                        updateDhcpResultsInfoRecord = updateDhcpResultsInfoRecord(dbr);
                        return updateDhcpResultsInfoRecord;
                    } else {
                        updateDhcpResultsInfoRecord = insertDhcpResultsInfoRecord(dbr);
                        return updateDhcpResultsInfoRecord;
                    }
                }
            }
            Log.e(TAG, "addOrUpdateApInfoRecord error.");
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0034, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:22:0x0064, code skipped:
            if (r2 != null) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:29:0x0084, code skipped:
            if (r2 == null) goto L_0x0087;
     */
    /* JADX WARNING: Missing block: B:32:0x0088, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public LruCache<String, DhcpResultsInfoRecord> getAllDhcpResultsInfo() {
        synchronized (this.mdbLock) {
            LruCache<String, DhcpResultsInfoRecord> dhcpResultsInfo = new LruCache(50);
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                Log.e(TAG, "getAllDhcpResultsInfo database error.");
                return null;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM DhcpResults", null);
                if (c == null) {
                    Log.e(TAG, "query database error.");
                    if (c != null) {
                        c.close();
                    }
                } else {
                    while (c.moveToNext()) {
                        DhcpResultsInfoRecord dbr = new DhcpResultsInfoRecord(c.getString(c.getColumnIndex("apSSID")), c.getString(c.getColumnIndex("IP")), c.getString(c.getColumnIndex("DHCPServer")));
                        dhcpResultsInfo.put(dbr.apSSID, dbr);
                    }
                }
            } catch (Exception e) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("queryDhcpResultsInfo error:");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }
}
