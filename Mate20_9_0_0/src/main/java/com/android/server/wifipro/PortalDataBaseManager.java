package com.android.server.wifipro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.android.server.HwNetworkPropertyChecker.StarndardPortalInfo;

public class PortalDataBaseManager {
    public static final boolean DBG = false;
    private static final String DB_NAME = "wifipro_portal_page_info.db";
    private static final int DB_VERSION = 9;
    public static final int MSG_INSERT_UPLOADED_TABLE = 101;
    private static final long TABLE5_MAX_SIZE = 5000;
    private static final String TAG = "PortalDataBaseManager";
    private static PortalDataBaseManager portalDataBaseManager = null;
    private SQLiteDatabase database = null;
    private PortalDbHelper dbHelper = null;

    public PortalDataBaseManager(Context context) {
        this.dbHelper = new PortalDbHelper(context, DB_NAME, null, 9);
        try {
            this.database = this.dbHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            LOGD("PortalDataBaseManager(), can't open database!");
        }
    }

    public static synchronized PortalDataBaseManager getInstance(Context context) {
        PortalDataBaseManager portalDataBaseManager;
        synchronized (PortalDataBaseManager.class) {
            if (portalDataBaseManager == null) {
                portalDataBaseManager = new PortalDataBaseManager(context);
            }
            portalDataBaseManager = portalDataBaseManager;
        }
        return portalDataBaseManager;
    }

    private long getCurrentRowNumber(String dbName) {
        long count = -1;
        if (this.database == null || !this.database.isOpen() || dbName == null || dbName.length() <= 0) {
            return -1;
        }
        String sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ");
        sql.append(dbName);
        try {
            count = this.database.compileStatement(sql.toString()).simpleQueryForLong();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrentRowNumber, dbName = ");
            stringBuilder.append(dbName);
            stringBuilder.append(", row num = ");
            stringBuilder.append(count);
            LOGD(stringBuilder.toString());
            return count;
        } catch (SQLException e) {
            LOGW("getCurrentRowNumber, SQLException");
            return count;
        }
    }

    /* JADX WARNING: Missing block: B:29:0x00d1, code skipped:
            if (r1 != null) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:31:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:36:0x00e2, code skipped:
            if (r1 == null) goto L_0x00ee;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateStandardPortalTable(StarndardPortalInfo portalInfo) {
        if (portalInfo != null) {
            if (portalInfo.currentSsid != null && portalInfo.timestamp > 0 && this.database != null && this.database.isOpen()) {
                Cursor cursor = null;
                boolean found = false;
                try {
                    cursor = this.database.rawQuery(PortalDbHelper.QUERY_TABLE4, null);
                    while (cursor.moveToNext()) {
                        if (portalInfo.currentSsid.equals(cursor.getString(cursor.getColumnIndex(PortalDbHelper.ITEM_SSID)))) {
                            found = true;
                            break;
                        }
                    }
                    ContentValues cv = new ContentValues();
                    if (found) {
                        cv.put(PortalDbHelper.ITEM_CHECK_TIMESTAMP, String.valueOf(portalInfo.timestamp));
                        cv.put(PortalDbHelper.ITEM_CHECK_LAC, Integer.valueOf(portalInfo.lac));
                        String[] whereArgs = new String[]{portalInfo.currentSsid};
                        long ret0 = (long) this.database.update(PortalDbHelper.TABLE_STANDARD_PORTAL_302, cv, "ssid=?", whereArgs);
                        if (ret0 > 0) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("updateStandardPortalTable, update ret = ");
                            stringBuilder.append(ret0);
                            Log.d(str, stringBuilder.toString());
                        }
                    } else {
                        cv.put(PortalDbHelper.ITEM_SSID, portalInfo.currentSsid);
                        cv.put(PortalDbHelper.ITEM_CHECK_TIMESTAMP, String.valueOf(portalInfo.timestamp));
                        cv.put(PortalDbHelper.ITEM_CHECK_LAC, Integer.valueOf(portalInfo.lac));
                        long ret = this.database.insert(PortalDbHelper.TABLE_STANDARD_PORTAL_302, null, cv);
                        if (ret > 0) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("updateStandardPortalTable, insert ret = ");
                            stringBuilder2.append(ret);
                            Log.d(str2, stringBuilder2.toString());
                        }
                    }
                } catch (SQLiteException e) {
                    try {
                        Log.w(TAG, "updateStandardPortalTable, SQLiteException happened");
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:27:0x007d, code skipped:
            if (r0 == null) goto L_0x00a6;
     */
    /* JADX WARNING: Missing block: B:38:0x009a, code skipped:
            if (r0 == null) goto L_0x00a6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void syncQueryStarndardPortalNetwork(StarndardPortalInfo portalInfo) {
        Cursor cursor = null;
        if (portalInfo != null) {
            if (!(portalInfo.currentSsid == null || this.database == null || !this.database.isOpen())) {
                try {
                    cursor = this.database.rawQuery(PortalDbHelper.QUERY_TABLE4, null);
                    while (cursor.moveToNext()) {
                        if (portalInfo.currentSsid.equals(cursor.getString(cursor.getColumnIndex(PortalDbHelper.ITEM_SSID)))) {
                            String ts = cursor.getString(cursor.getColumnIndex(PortalDbHelper.ITEM_CHECK_TIMESTAMP));
                            int lac = cursor.getInt(cursor.getColumnIndex(PortalDbHelper.ITEM_CHECK_LAC));
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("syncQueryStarndardPortalNetwork, matched, currentSsid = ");
                            stringBuilder.append(portalInfo.currentSsid);
                            LOGD(stringBuilder.toString());
                            if (ts != null && ts.length() > 0) {
                                portalInfo.timestamp = Long.parseLong(ts);
                            }
                            if (lac > Integer.MIN_VALUE && lac < Integer.MAX_VALUE) {
                                portalInfo.lac = lac;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "syncQueryStarndardPortalNetwork, NumberFormatException happened");
                } catch (SQLiteException e2) {
                    try {
                        Log.w(TAG, "syncQueryStarndardPortalNetwork, SQLiteException happened");
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
    }

    /* JADX WARNING: Missing block: B:17:0x0025, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:38:0x00bc, code skipped:
            if (r1 != null) goto L_0x00be;
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:45:0x00cb, code skipped:
            if (r1 == null) goto L_0x00d7;
     */
    /* JADX WARNING: Missing block: B:54:0x00d8, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateDhcpResultsByBssid(String currBssid, String dhcpResults) {
        if (!(currBssid == null || dhcpResults == null)) {
            if (this.database != null && this.database.isOpen()) {
                Cursor cursor = null;
                try {
                    long currRows = getCurrentRowNumber(PortalDbHelper.TABLE_DHCP_RESULTS_INTERNET_OK);
                    if (currRows != -1) {
                        boolean found = false;
                        cursor = this.database.rawQuery(PortalDbHelper.QUERY_TABLE5, null);
                        while (cursor.moveToNext()) {
                            if (currBssid.equals(cursor.getString(cursor.getColumnIndex(PortalDbHelper.ITEM_BSSID)))) {
                                found = true;
                                break;
                            }
                        }
                        ContentValues cv = new ContentValues();
                        if (found) {
                            cv.put(PortalDbHelper.ITEM_DHCP_RESULTS, String.valueOf(dhcpResults));
                            String[] whereArgs = new String[]{currBssid};
                            int ret0 = this.database.update(PortalDbHelper.TABLE_DHCP_RESULTS_INTERNET_OK, cv, "bssid=?", whereArgs);
                            if (ret0 > 0) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("updateDhcpResultsByBssid, update ret = ");
                                stringBuilder.append(ret0);
                                Log.d(str, stringBuilder.toString());
                            }
                        } else if (currRows < TABLE5_MAX_SIZE) {
                            cv.put(PortalDbHelper.ITEM_BSSID, currBssid);
                            cv.put(PortalDbHelper.ITEM_DHCP_RESULTS, String.valueOf(dhcpResults));
                            long ret1 = this.database.insert(PortalDbHelper.TABLE_DHCP_RESULTS_INTERNET_OK, null, cv);
                            if (ret1 > 0) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("updateDhcpResultsByBssid, insert ret = ");
                                stringBuilder2.append(ret1);
                                Log.d(str2, stringBuilder2.toString());
                            }
                        }
                    } else if (cursor != null) {
                        cursor.close();
                    }
                } catch (SQLiteException e) {
                    try {
                        LOGW("updateDhcpResultsByBssid, SQLiteException happend!");
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0046, code skipped:
            if (r2 == null) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:28:0x0055, code skipped:
            if (r2 == null) goto L_0x0061;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized String syncQueryDhcpResultsByBssid(String currentBssid) {
        String dhcpResults;
        dhcpResults = null;
        if (currentBssid != null) {
            if (this.database != null && this.database.isOpen()) {
                Cursor cursor = null;
                try {
                    cursor = this.database.rawQuery(PortalDbHelper.QUERY_TABLE5, null);
                    while (cursor.moveToNext()) {
                        if (currentBssid.equals(cursor.getString(cursor.getColumnIndex(PortalDbHelper.ITEM_BSSID)))) {
                            String matchedDhcp = cursor.getString(cursor.getColumnIndex(PortalDbHelper.ITEM_DHCP_RESULTS));
                            if (matchedDhcp != null && matchedDhcp.length() > 0) {
                                dhcpResults = matchedDhcp;
                            }
                        }
                    }
                } catch (SQLiteException e) {
                    try {
                        LOGW("syncQueryDhcpResultsByBssid, SQLiteException msg happend.");
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }
        }
        return dhcpResults;
    }

    public void LOGD(String msg) {
        Log.d(TAG, msg);
    }

    public void LOGW(String msg) {
        Log.w(TAG, msg);
    }
}
