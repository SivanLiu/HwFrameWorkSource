package com.huawei.hwwifiproservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.util.wifi.HwHiLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class WifiProHistoryDBManager {
    private static long AGING_MS_OF_EACH_DAY = 1800000;
    private static final int DBG_LOG_LEVEL = 1;
    private static final int ERROR_LOG_LEVEL = 3;
    private static final int INFO_LOG_LEVEL = 2;
    private static final short MAX_AP_INFO_RECORD_NUM = 1;
    private static final String TAG = "WifiProHistoryDBManager";
    private static WifiProHistoryDBManager mBQEDataBaseManager;
    private static long msOfOneDay = 86400000;
    private static int printLogLevel = 1;
    private static long tooOldValidDay = 10;
    private int mApRecordCount = 0;
    private final Object mBqeLock = new Object();
    private SQLiteDatabase mDatabase;
    private WifiProHistoryDBHelper mHelper;
    private int mHomeApRecordCount = 0;
    private boolean mNeedDelOldDualBandApInfo;

    public WifiProHistoryDBManager(Context context) {
        HwHiLog.w(TAG, false, "WifiProHistoryDBManager()", new Object[0]);
        this.mHelper = new WifiProHistoryDBHelper(context);
        try {
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            HwHiLog.e(TAG, false, "WifiProHistoryDBManager(), can't open database!", new Object[0]);
        }
    }

    public static WifiProHistoryDBManager getInstance(Context context) {
        if (mBQEDataBaseManager == null) {
            mBQEDataBaseManager = new WifiProHistoryDBManager(context);
        }
        return mBQEDataBaseManager;
    }

    public void closeDB() {
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    HwHiLog.w(TAG, false, "closeDB()", new Object[0]);
                    this.mDatabase.close();
                }
            }
        }
    }

    private boolean deleteHistoryRecord(String dbTableName, String apBssid) {
        HwHiLog.i(TAG, false, "deleteHistoryRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (apBssid == null) {
                        HwHiLog.e(TAG, false, "deleteHistoryRecord null error.", new Object[0]);
                        return false;
                    }
                    try {
                        this.mDatabase.delete(dbTableName, "apBSSID like ?", new String[]{apBssid});
                        return true;
                    } catch (SQLException e) {
                        HwHiLog.e(TAG, false, "deleteHistoryRecord error", new Object[0]);
                        return false;
                    }
                }
            }
            HwHiLog.e(TAG, false, "deleteHistoryRecord database error.", new Object[0]);
            return false;
        }
    }

    public boolean deleteApInfoRecord(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, apBssid);
    }

    private boolean checkHistoryRecordExist(String dbTableName, String apBssid) throws CheckHistoryRecordException {
        boolean ret = false;
        Cursor c = null;
        try {
            SQLiteDatabase sQLiteDatabase = this.mDatabase;
            Cursor c2 = sQLiteDatabase.rawQuery("SELECT * FROM " + dbTableName + " where apBSSID like ?", new String[]{apBssid});
            int rcdCount = c2.getCount();
            if (rcdCount > 0) {
                ret = true;
            }
            HwHiLog.d(TAG, false, "checkHistoryRecordExist read from:%{public}s, get record: %{public}d", new Object[]{dbTableName, Integer.valueOf(rcdCount)});
            c2.close();
            return ret;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "checkHistoryRecordExist error", new Object[0]);
            throw new CheckHistoryRecordException();
        } catch (Throwable th) {
            if (0 != 0) {
                c.close();
            }
            throw th;
        }
    }

    private boolean updateApInfoRecord(WifiProApInfoRecord dbr) {
        ContentValues values = new ContentValues();
        values.put("apSSID", dbr.apSSID);
        values.put("apSecurityType", Integer.valueOf(dbr.apSecurityType));
        values.put("firstConnectTime", Long.valueOf(dbr.firstConnectTime));
        values.put("lastConnectTime", Long.valueOf(dbr.lastConnectTime));
        values.put("lanDataSize", Integer.valueOf(dbr.lanDataSize));
        values.put("highSpdFreq", Integer.valueOf(dbr.highSpdFreq));
        values.put("totalUseTime", Integer.valueOf(dbr.totalUseTime));
        values.put("totalUseTimeAtNight", Integer.valueOf(dbr.totalUseTimeAtNight));
        values.put("totalUseTimeAtWeekend", Integer.valueOf(dbr.totalUseTimeAtWeekend));
        values.put("judgeHomeAPTime", Long.valueOf(dbr.judgeHomeAPTime));
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, values, "apBSSID like ?", new String[]{dbr.apBSSID});
            if (rowChg == 0) {
                HwHiLog.e(TAG, false, "updateApInfoRecord update failed.", new Object[0]);
                return false;
            }
            HwHiLog.d(TAG, false, "updateApInfoRecord update succ, rowChg=%{public}d", new Object[]{Integer.valueOf(rowChg)});
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "updateApInfoRecord error", new Object[0]);
            return false;
        }
    }

    private boolean insertApInfoRecord(WifiProApInfoRecord dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProApInfoRecodTable VALUES(null,  ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,?)", new Object[]{dbr.apBSSID, dbr.apSSID, Integer.valueOf(dbr.apSecurityType), Long.valueOf(dbr.firstConnectTime), Long.valueOf(dbr.lastConnectTime), Integer.valueOf(dbr.lanDataSize), Integer.valueOf(dbr.highSpdFreq), Integer.valueOf(dbr.totalUseTime), Integer.valueOf(dbr.totalUseTimeAtNight), Integer.valueOf(dbr.totalUseTimeAtWeekend), Long.valueOf(dbr.judgeHomeAPTime)});
            HwHiLog.i(TAG, false, "insertApInfoRecord add a record succ.", new Object[0]);
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "insertApInfoRecord error", new Object[0]);
            return false;
        }
    }

    public boolean addOrUpdateApInfoRecord(WifiProApInfoRecord dbr) {
        HwHiLog.d(TAG, false, "addOrUpdateApInfoRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (dbr != null) {
                    if (dbr.apBSSID == null) {
                        HwHiLog.e(TAG, false, "addOrUpdateApInfoRecord null error.", new Object[0]);
                        return false;
                    }
                    try {
                        if (checkHistoryRecordExist(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, dbr.apBSSID)) {
                            return updateApInfoRecord(dbr);
                        }
                        return insertApInfoRecord(dbr);
                    } catch (CheckHistoryRecordException e) {
                        HwHiLog.e(TAG, false, "Exceptions happened in addOrUpdateApInfoRecord()", new Object[0]);
                        return false;
                    }
                }
            }
            HwHiLog.e(TAG, false, "addOrUpdateApInfoRecord error.", new Object[0]);
            return false;
        }
    }

    public boolean queryApInfoRecord(String apBssid, WifiProApInfoRecord dbr) {
        HwHiLog.d(TAG, false, "queryApInfoRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "queryApInfoRecord database error.", new Object[0]);
                return false;
            } else if (apBssid == null || dbr == null) {
                HwHiLog.e(TAG, false, "queryApInfoRecord null error.", new Object[0]);
                return false;
            } else {
                Cursor c = null;
                int recCnt = 0;
                try {
                    Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable where apBSSID like ?", new String[]{apBssid});
                    while (true) {
                        if (!c2.moveToNext()) {
                            break;
                        }
                        recCnt++;
                        if (recCnt > 1) {
                            break;
                        }
                        HwHiLog.d(TAG, false, "read record id = %{public}d", new Object[]{Integer.valueOf(c2.getInt(c2.getColumnIndex("_id")))});
                        if (recCnt == 1) {
                            dbr.apBSSID = apBssid;
                            dbr.apSSID = c2.getString(c2.getColumnIndex("apSSID"));
                            dbr.apSecurityType = c2.getInt(c2.getColumnIndex("apSecurityType"));
                            dbr.firstConnectTime = c2.getLong(c2.getColumnIndex("firstConnectTime"));
                            dbr.lastConnectTime = c2.getLong(c2.getColumnIndex("lastConnectTime"));
                            dbr.lanDataSize = c2.getInt(c2.getColumnIndex("lanDataSize"));
                            dbr.highSpdFreq = c2.getInt(c2.getColumnIndex("highSpdFreq"));
                            dbr.totalUseTime = c2.getInt(c2.getColumnIndex("totalUseTime"));
                            dbr.totalUseTimeAtNight = c2.getInt(c2.getColumnIndex("totalUseTimeAtNight"));
                            dbr.totalUseTimeAtWeekend = c2.getInt(c2.getColumnIndex("totalUseTimeAtWeekend"));
                            dbr.judgeHomeAPTime = c2.getLong(c2.getColumnIndex("judgeHomeAPTime"));
                            HwHiLog.i(TAG, false, "read record succ, LastConnectTime:%{public}s", new Object[]{String.valueOf(dbr.lastConnectTime)});
                        }
                    }
                    c2.close();
                    if (recCnt > 1) {
                        HwHiLog.e(TAG, false, "more than one record error. use first record.", new Object[0]);
                    } else if (recCnt == 0) {
                        HwHiLog.i(TAG, false, "queryApInfoRecord not record.", new Object[0]);
                    }
                    return true;
                } catch (SQLException e) {
                    HwHiLog.e(TAG, false, "queryApInfoRecord error", new Object[0]);
                    if (0 != 0) {
                        c.close();
                    }
                    return false;
                } catch (Throwable th) {
                    if (0 != 0) {
                        c.close();
                    }
                    throw th;
                }
            }
        }
    }

    public int querySameSSIDApCount(String apBSSID, String apSsid, int secType) {
        int recCnt = 0;
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "querySameSSIDApCount database error.", new Object[0]);
                return 0;
            } else if (apBSSID == null || apSsid == null) {
                HwHiLog.e(TAG, false, "querySameSSIDApCount null error.", new Object[0]);
                return 0;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable where (apSSID like ?) and (apSecurityType = ?) and (apBSSID != ?)", new String[]{apSsid, String.valueOf(secType), apBSSID});
                    recCnt = c.getCount();
                    HwHiLog.i(TAG, false, "querySameSSIDApCount read same (SSID:%{public}s, secType:%{public}d) and different BSSID record count=%{public}d", new Object[]{WifiProCommonUtils.safeDisplaySsid(apSsid), Integer.valueOf(secType), Integer.valueOf(recCnt)});
                    c.close();
                    return recCnt;
                } catch (SQLException e) {
                    HwHiLog.e(TAG, false, "querySameSSIDApCount error", new Object[0]);
                    if (c != null) {
                        c.close();
                    }
                    return recCnt;
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                    throw th;
                }
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v3, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r4v0 */
    /* JADX WARN: Type inference failed for: r4v7 */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x016a  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0173  */
    public boolean removeTooOldApInfoRecord() {
        long totalConnectTime;
        boolean z;
        Date currDate;
        boolean z2 = false;
        Vector<String> delRecordsVector = new Vector<>();
        int i = 0;
        HwHiLog.d(TAG, false, "removeTooOldApInfoRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            try {
                if (this.mDatabase != null) {
                    if (this.mDatabase.isOpen()) {
                        delRecordsVector.clear();
                        Cursor c = null;
                        c = null;
                        try {
                            c = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable", null);
                            HwHiLog.d(TAG, false, "all record count=%{public}d", new Object[]{Integer.valueOf(c.getCount())});
                            Date currDate2 = new Date();
                            while (c.moveToNext()) {
                                long lastConnDateMsTime = c.getLong(c.getColumnIndex("lastConnectTime"));
                                long totalConnectTime2 = (long) c.getInt(c.getColumnIndex("totalUseTime"));
                                try {
                                    long currDateMsTime = currDate2.getTime();
                                    long pastDays = (currDateMsTime - lastConnDateMsTime) / msOfOneDay;
                                    if (pastDays <= tooOldValidDay) {
                                        currDate = currDate2;
                                        z = z2;
                                        totalConnectTime = totalConnectTime2;
                                    } else if (totalConnectTime2 - (AGING_MS_OF_EACH_DAY * pastDays) < 0) {
                                        currDate = currDate2;
                                        HwHiLog.i(TAG, i, "check result: need delete.", new Object[i]);
                                        int id = c.getInt(c.getColumnIndex("_id"));
                                        String ssid = c.getString(c.getColumnIndex("apSSID"));
                                        String bssid = c.getString(c.getColumnIndex("apBSSID"));
                                        z = z2;
                                        totalConnectTime = totalConnectTime2;
                                        try {
                                            HwHiLog.i(TAG, false, "check record: ssid:%{public}s, id:%{public}d, pass time:%{public}s", new Object[]{WifiProCommonUtils.safeDisplaySsid(ssid), Integer.valueOf(id), String.valueOf(currDateMsTime - lastConnDateMsTime)});
                                            delRecordsVector.add(bssid);
                                        } catch (SQLException e) {
                                        } catch (Throwable th) {
                                            e = th;
                                            if (c != null) {
                                            }
                                            throw e;
                                        }
                                    } else {
                                        currDate = currDate2;
                                        z = z2;
                                        totalConnectTime = totalConnectTime2;
                                    }
                                    currDate2 = currDate;
                                    z2 = z;
                                    i = 0;
                                } catch (SQLException e2) {
                                    try {
                                        HwHiLog.e(TAG, false, "removeTooOldApInfoRecord error", new Object[0]);
                                        if (c != null) {
                                        }
                                        return false;
                                    } catch (Throwable th2) {
                                        e = th2;
                                    }
                                } catch (Throwable th3) {
                                    e = th3;
                                    if (c != null) {
                                    }
                                    throw e;
                                }
                            }
                            try {
                                int delSize = delRecordsVector.size();
                                HwHiLog.i(TAG, false, "start delete %{public}d records.", new Object[]{Integer.valueOf(delSize)});
                                int i2 = 0;
                                while (true) {
                                    if (i2 < delSize) {
                                        String delBSSID = delRecordsVector.get(i2);
                                        if (delBSSID == null) {
                                            break;
                                        }
                                        this.mDatabase.delete(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, "apBSSID like ?", new String[]{delBSSID});
                                        i2++;
                                    }
                                }
                                try {
                                    c.close();
                                    return true;
                                } catch (Throwable th4) {
                                    th = th4;
                                    throw th;
                                }
                            } catch (SQLException e3) {
                                HwHiLog.e(TAG, false, "removeTooOldApInfoRecord error", new Object[0]);
                                if (c != null) {
                                }
                                return false;
                            }
                        } catch (SQLException e4) {
                            HwHiLog.e(TAG, false, "removeTooOldApInfoRecord error", new Object[0]);
                            if (c != null) {
                                c.close();
                            }
                            return false;
                        } catch (Throwable th5) {
                            e = th5;
                            if (c != null) {
                                c.close();
                            }
                            throw e;
                        }
                    }
                }
                HwHiLog.e(TAG, false, "removeTooOldApInfoRecord database error.", new Object[0]);
                return false;
            } catch (Throwable th6) {
                th = th6;
                throw th;
            }
        }
    }

    public boolean statisticApInfoRecord() {
        HwHiLog.d(TAG, false, "statisticApInfoRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    this.mHomeApRecordCount = 0;
                    this.mApRecordCount = 0;
                    Cursor c = null;
                    try {
                        Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable", null);
                        this.mApRecordCount = c2.getCount();
                        HwHiLog.i(TAG, false, "all record count=%{public}d", new Object[]{Integer.valueOf(this.mApRecordCount)});
                        while (c2.moveToNext()) {
                            if (c2.getLong(c2.getColumnIndex("judgeHomeAPTime")) > 0) {
                                String ssid = c2.getString(c2.getColumnIndex("apSSID"));
                                this.mHomeApRecordCount++;
                                HwHiLog.i(TAG, false, "check record: Home ap ssid:%{public}s, total:%{public}d", new Object[]{WifiProCommonUtils.safeDisplaySsid(ssid), Integer.valueOf(this.mHomeApRecordCount)});
                            }
                        }
                        c2.close();
                        return true;
                    } catch (SQLException e) {
                        HwHiLog.e(TAG, false, "removeTooOldApInfoRecord error", new Object[0]);
                        if (0 != 0) {
                            c.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        if (0 != 0) {
                            c.close();
                        }
                        throw th;
                    }
                }
            }
            HwHiLog.e(TAG, false, "statisticApInfoRecord database error.", new Object[0]);
            return false;
        }
    }

    public int getTotRecordCount() {
        int i;
        synchronized (this.mBqeLock) {
            i = this.mApRecordCount;
        }
        return i;
    }

    public int getHomeApRecordCount() {
        int i;
        synchronized (this.mBqeLock) {
            i = this.mHomeApRecordCount;
        }
        return i;
    }

    private boolean insertEnterpriseApRecord(String apSSID, int secType) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProEnterpriseAPTable VALUES(null,  ?, ?)", new Object[]{apSSID, Integer.valueOf(secType)});
            HwHiLog.i(TAG, false, "insertEnterpriseApRecord add a record succ.", new Object[0]);
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "insertEnterpriseApRecord error", new Object[0]);
            return false;
        }
    }

    public boolean addOrUpdateEnterpriseApRecord(String apSSID, int secType) {
        HwHiLog.d(TAG, false, "addOrUpdateEnterpriseApRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (apSSID != null) {
                    if (!queryEnterpriseApRecord(apSSID, secType)) {
                        HwHiLog.i(TAG, false, "add record here ssid:%{public}s", new Object[]{WifiProCommonUtils.safeDisplaySsid(apSSID)});
                        return insertEnterpriseApRecord(apSSID, secType);
                    }
                    HwHiLog.i(TAG, false, "already exist the record ssid:%{public}s", new Object[]{WifiProCommonUtils.safeDisplaySsid(apSSID)});
                    return true;
                }
            }
            HwHiLog.e(TAG, false, "addOrUpdateEnterpriseApRecord error.", new Object[0]);
            return false;
        }
    }

    public boolean queryEnterpriseApRecord(String apSSID, int secType) {
        HwHiLog.d(TAG, false, "queryEnterpriseApRecord enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (apSSID == null) {
                        HwHiLog.e(TAG, false, "queryEnterpriseApRecord null error.", new Object[0]);
                        return false;
                    }
                    Cursor c = null;
                    try {
                        c = this.mDatabase.rawQuery("SELECT * FROM WifiProEnterpriseAPTable where (apSSID like ?) and (apSecurityType = ?)", new String[]{apSSID, String.valueOf(secType)});
                        int recCnt = c.getCount();
                        c.close();
                        if (recCnt > 0) {
                            HwHiLog.i(TAG, false, "SSID:%{public}s, security: %{public}d is in Enterprise Ap table. count:%{public}d", new Object[]{WifiProCommonUtils.safeDisplaySsid(apSSID), Integer.valueOf(secType), Integer.valueOf(recCnt)});
                            return true;
                        }
                        HwHiLog.i(TAG, false, "SSID:%{public}s, security: %{public}d is not in Enterprise Ap table. count:%{public}d", new Object[]{WifiProCommonUtils.safeDisplaySsid(apSSID), Integer.valueOf(secType), Integer.valueOf(recCnt)});
                        return false;
                    } catch (SQLException e) {
                        HwHiLog.e(TAG, false, "queryEnterpriseApRecord error", new Object[0]);
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
            }
            HwHiLog.e(TAG, false, "queryEnterpriseApRecord database error.", new Object[0]);
            return false;
        }
    }

    public boolean deleteEnterpriseApRecord(String tableName, String ssid, int secType) {
        if (tableName == null || ssid == null) {
            HwHiLog.e(TAG, false, "deleteHistoryRecord null error.", new Object[0]);
            return false;
        }
        HwHiLog.i(TAG, false, "delete record of same (SSID:%{public}s, secType:%{public}d) from %{public}s", new Object[]{WifiProCommonUtils.safeDisplaySsid(ssid), Integer.valueOf(secType), tableName});
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "deleteHistoryRecord database error.", new Object[0]);
                return false;
            }
            try {
                HwHiLog.d(TAG, false, "delete record count=%{public}d", new Object[]{Integer.valueOf(this.mDatabase.delete(tableName, "(apSSID like ?) and (apSecurityType = ?)", new String[]{ssid, String.valueOf(secType)}))});
                return true;
            } catch (SQLException e) {
                HwHiLog.e(TAG, false, "deleteHistoryRecord error", new Object[0]);
                return false;
            }
        }
    }

    public boolean deleteRelateApRcd(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, apBssid);
    }

    public boolean deleteApQualityRcd(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_QUALITY_TB_NAME, apBssid);
    }

    public boolean deleteDualBandApInfoRcd(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, apBssid);
    }

    public boolean deleteRelateApRcd(String apBssid, String relatedBSSID) {
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "deleteRelateApRcd database error.", new Object[0]);
                return false;
            } else if (apBssid == null || relatedBSSID == null) {
                HwHiLog.e(TAG, false, "deleteRelateApRcd null error.", new Object[0]);
                return false;
            } else {
                try {
                    this.mDatabase.delete(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, "(apBSSID like ?) and (RelatedBSSID like ?)", new String[]{apBssid, relatedBSSID});
                    return true;
                } catch (SQLException e) {
                    HwHiLog.e(TAG, false, "deleteRelateApRcd error", new Object[0]);
                    return false;
                }
            }
        }
    }

    public boolean deleteRelate5GAPRcd(String relatedBSSID) {
        HwHiLog.i(TAG, false, "deleteRelateApRcd enter", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (relatedBSSID == null) {
                        HwHiLog.e(TAG, false, "deleteRelateApRcd null error.", new Object[0]);
                        return false;
                    }
                    try {
                        this.mDatabase.delete(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, "(RelatedBSSID like ?)", new String[]{relatedBSSID});
                        return true;
                    } catch (SQLException e) {
                        HwHiLog.e(TAG, false, "deleteRelateApRcd error", new Object[0]);
                        return false;
                    }
                }
            }
            HwHiLog.e(TAG, false, "deleteRelateApRcd database error.", new Object[0]);
            return false;
        }
    }

    private boolean deleteAllDualBandAPRcd(String apBssid) {
        return deleteDualBandApInfoRcd(apBssid) && deleteApQualityRcd(apBssid) && deleteRelateApRcd(apBssid) && deleteRelate5GAPRcd(apBssid);
    }

    private boolean updateApQualityRcd(WifiProApQualityRcd dbr) {
        ContentValues values = new ContentValues();
        values.put("RTT_Product", dbr.mRttProduct);
        values.put("RTT_PacketVolume", dbr.mRttPacketVolume);
        values.put("HistoryAvgRtt", dbr.mHistoryAvgRtt);
        values.put("OTA_LostRateValue", dbr.mOtaLostRateValue);
        values.put("OTA_PktVolume", dbr.mOtaPktVolume);
        values.put("OTA_BadPktProduct", dbr.mOtaBadPktProduct);
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_QUALITY_TB_NAME, values, "apBSSID like ?", new String[]{dbr.apBSSID});
            if (rowChg == 0) {
                HwHiLog.e(TAG, false, "updateApQualityRcd update failed.", new Object[0]);
                return false;
            }
            HwHiLog.d(TAG, false, "updateApQualityRcd update succ, rowChg=%{public}d", new Object[]{Integer.valueOf(rowChg)});
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "updateApQualityRcd error", new Object[0]);
            return false;
        }
    }

    private boolean insertApQualityRcd(WifiProApQualityRcd dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProApQualityTable VALUES(null,  ?, ?, ?, ?, ?,   ?, ?)", new Object[]{dbr.apBSSID, dbr.mRttProduct, dbr.mRttPacketVolume, dbr.mHistoryAvgRtt, dbr.mOtaLostRateValue, dbr.mOtaPktVolume, dbr.mOtaBadPktProduct});
            HwHiLog.i(TAG, false, "insertApQualityRcd add a record succ.", new Object[0]);
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "insertApQualityRcd error", new Object[0]);
            return false;
        }
    }

    public boolean addOrUpdateApQualityRcd(WifiProApQualityRcd dbr) {
        HwHiLog.d(TAG, false, "addOrUpdateApQualityRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (dbr != null) {
                    if (dbr.apBSSID == null) {
                        HwHiLog.e(TAG, false, "addOrUpdateApQualityRcd null error.", new Object[0]);
                        return false;
                    }
                    try {
                        if (checkHistoryRecordExist(WifiProHistoryDBHelper.WP_QUALITY_TB_NAME, dbr.apBSSID)) {
                            return updateApQualityRcd(dbr);
                        }
                        return insertApQualityRcd(dbr);
                    } catch (CheckHistoryRecordException e) {
                        HwHiLog.e(TAG, false, "Exceptions happened in addOrUpdateApQualityRcd()", new Object[0]);
                        return false;
                    }
                }
            }
            HwHiLog.e(TAG, false, "addOrUpdateApQualityRcd error.", new Object[0]);
            return false;
        }
    }

    public boolean queryApQualityRcd(String apBssid, WifiProApQualityRcd dbr) {
        HwHiLog.d(TAG, false, "queryApQualityRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "queryApQualityRcd database error.", new Object[0]);
                return false;
            } else if (apBssid == null || dbr == null) {
                HwHiLog.e(TAG, false, "queryApQualityRcd null error.", new Object[0]);
                return false;
            } else {
                Cursor c = null;
                int recCnt = 0;
                try {
                    Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM WifiProApQualityTable where apBSSID like ?", new String[]{apBssid});
                    while (true) {
                        if (!c2.moveToNext()) {
                            break;
                        }
                        recCnt++;
                        if (recCnt > 1) {
                            break;
                        } else if (recCnt == 1) {
                            dbr.apBSSID = apBssid;
                            dbr.mRttProduct = c2.getBlob(c2.getColumnIndex("RTT_Product"));
                            dbr.mRttPacketVolume = c2.getBlob(c2.getColumnIndex("RTT_PacketVolume"));
                            dbr.mHistoryAvgRtt = c2.getBlob(c2.getColumnIndex("HistoryAvgRtt"));
                            dbr.mOtaLostRateValue = c2.getBlob(c2.getColumnIndex("OTA_LostRateValue"));
                            dbr.mOtaPktVolume = c2.getBlob(c2.getColumnIndex("OTA_PktVolume"));
                            dbr.mOtaBadPktProduct = c2.getBlob(c2.getColumnIndex("OTA_BadPktProduct"));
                            HwHiLog.i(TAG, false, "read record succ", new Object[0]);
                        }
                    }
                    c2.close();
                    if (recCnt > 1) {
                        HwHiLog.e(TAG, false, "more than one record error. use first record.", new Object[0]);
                    } else if (recCnt == 0) {
                        HwHiLog.i(TAG, false, "queryApQualityRcd not record.", new Object[0]);
                        return false;
                    }
                    return true;
                } catch (SQLException e) {
                    HwHiLog.e(TAG, false, "queryApQualityRcd error", new Object[0]);
                    if (0 != 0) {
                        c.close();
                    }
                    return false;
                } catch (Throwable th) {
                    if (0 != 0) {
                        c.close();
                    }
                    throw th;
                }
            }
        }
    }

    private boolean updateRelateApRcd(WifiProRelateApRcd dbr) {
        ContentValues values = new ContentValues();
        values.put("RelateType", Integer.valueOf(dbr.mRelateType));
        values.put("MaxCurrentRSSI", Integer.valueOf(dbr.mMaxCurrentRSSI));
        values.put("MaxRelatedRSSI", Integer.valueOf(dbr.mMaxRelatedRSSI));
        values.put("MinCurrentRSSI", Integer.valueOf(dbr.mMinCurrentRSSI));
        values.put("MinRelatedRSSI", Integer.valueOf(dbr.mMinRelatedRSSI));
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, values, "(apBSSID like ?) and (RelatedBSSID like ?)", new String[]{dbr.mApBSSID, dbr.mRelatedBSSID});
            if (rowChg == 0) {
                HwHiLog.e(TAG, false, "updateRelateApRcd update failed.", new Object[0]);
                return false;
            }
            HwHiLog.d(TAG, false, "updateRelateApRcd update succ, rowChg=%{public}d", new Object[]{Integer.valueOf(rowChg)});
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "updateRelateApRcd error", new Object[0]);
            return false;
        }
    }

    private boolean insertRelateApRcd(WifiProRelateApRcd dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProRelateApTable VALUES(null,  ?, ?, ?, ?, ?, ?, ?)", new Object[]{dbr.mApBSSID, dbr.mRelatedBSSID, Integer.valueOf(dbr.mRelateType), Integer.valueOf(dbr.mMaxCurrentRSSI), Integer.valueOf(dbr.mMaxRelatedRSSI), Integer.valueOf(dbr.mMinCurrentRSSI), Integer.valueOf(dbr.mMinRelatedRSSI)});
            HwHiLog.i(TAG, false, "insertRelateApRcd add a record succ.", new Object[0]);
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "insertRelateApRcd error", new Object[0]);
            return false;
        }
    }

    private boolean checkRelateApRcdExist(String apBssid, String relatedBSSID) {
        boolean ret = false;
        Cursor c = null;
        try {
            Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM WifiProRelateApTable where (apBSSID like ?) and (RelatedBSSID like ?)", new String[]{apBssid, relatedBSSID});
            int rcdCount = c2.getCount();
            if (rcdCount > 0) {
                ret = true;
            }
            HwHiLog.d(TAG, false, "checkRelateApRcdExist get record: %{public}d", new Object[]{Integer.valueOf(rcdCount)});
            c2.close();
            return ret;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "checkRelateApRcdExist error", new Object[0]);
            if (0 != 0) {
                c.close();
            }
            return false;
        } catch (Throwable th) {
            if (0 != 0) {
                c.close();
            }
            throw th;
        }
    }

    public boolean addOrUpdateRelateApRcd(WifiProRelateApRcd dbr) {
        HwHiLog.d(TAG, false, "addOrUpdateRelateApRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (dbr != null) {
                    if (dbr.mApBSSID != null) {
                        if (dbr.mRelatedBSSID != null) {
                            if (checkRelateApRcdExist(dbr.mApBSSID, dbr.mRelatedBSSID)) {
                                return updateRelateApRcd(dbr);
                            }
                            return insertRelateApRcd(dbr);
                        }
                    }
                    HwHiLog.e(TAG, false, "addOrUpdateRelateApRcd null error.", new Object[0]);
                    return false;
                }
            }
            HwHiLog.e(TAG, false, "addOrUpdateRelateApRcd error.", new Object[0]);
            return false;
        }
    }

    public boolean queryRelateApRcd(String apBssid, List<WifiProRelateApRcd> relateApList) {
        HwHiLog.d(TAG, false, "queryRelateApRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (apBssid != null) {
                        if (relateApList != null) {
                            relateApList.clear();
                            Cursor c = null;
                            int recCnt = 0;
                            try {
                                Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM WifiProRelateApTable where apBSSID like ?", new String[]{apBssid});
                                while (true) {
                                    if (!c2.moveToNext()) {
                                        break;
                                    }
                                    recCnt++;
                                    if (recCnt > 20) {
                                        break;
                                    }
                                    WifiProRelateApRcd dbr = new WifiProRelateApRcd(apBssid);
                                    dbr.mRelatedBSSID = c2.getString(c2.getColumnIndex("RelatedBSSID"));
                                    dbr.mRelateType = c2.getShort(c2.getColumnIndex("RelateType"));
                                    dbr.mMaxCurrentRSSI = c2.getInt(c2.getColumnIndex("MaxCurrentRSSI"));
                                    dbr.mMaxRelatedRSSI = c2.getInt(c2.getColumnIndex("MaxRelatedRSSI"));
                                    dbr.mMinCurrentRSSI = c2.getInt(c2.getColumnIndex("MinCurrentRSSI"));
                                    dbr.mMinRelatedRSSI = c2.getInt(c2.getColumnIndex("MinRelatedRSSI"));
                                    relateApList.add(dbr);
                                }
                                c2.close();
                                if (recCnt != 0) {
                                    return true;
                                }
                                HwHiLog.i(TAG, false, "queryRelateApRcd not record.", new Object[0]);
                                return false;
                            } catch (SQLException e) {
                                HwHiLog.e(TAG, false, "queryRelateApRcd error", new Object[0]);
                                if (0 != 0) {
                                    c.close();
                                }
                                return false;
                            } catch (Throwable th) {
                                if (0 != 0) {
                                    c.close();
                                }
                                throw th;
                            }
                        }
                    }
                    HwHiLog.e(TAG, false, "queryRelateApRcd null error.", new Object[0]);
                    return false;
                }
            }
            HwHiLog.e(TAG, false, "queryRelateApRcd database error.", new Object[0]);
            return false;
        }
    }

    private boolean updateDualBandApInfoRcd(WifiProDualBandApInfoRcd dbr) {
        ContentValues values = new ContentValues();
        values.put("apSSID", dbr.mApSSID);
        values.put("InetCapability", dbr.mInetCapability);
        values.put("ServingBand", dbr.mServingBand);
        values.put("ApAuthType", dbr.mApAuthType);
        values.put("ChannelFrequency", Integer.valueOf(dbr.mChannelFrequency));
        values.put("DisappearCount", Integer.valueOf(dbr.mDisappearCount));
        values.put("isInBlackList", Integer.valueOf(dbr.mInBlackList));
        values.put("UpdateTime", Long.valueOf(dbr.mUpdateTime));
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, values, "apBSSID like ?", new String[]{dbr.mApBSSID});
            if (rowChg == 0) {
                HwHiLog.e(TAG, false, "updateDualBandApInfoRcd update failed.", new Object[0]);
                return false;
            }
            HwHiLog.d(TAG, false, "updateDualBandApInfoRcd update succ, rowChg=%{public}d", new Object[]{Integer.valueOf(rowChg)});
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "updateDualBandApInfoRcd error", new Object[0]);
            return false;
        }
    }

    private boolean insertDualBandApInfoRcd(WifiProDualBandApInfoRcd dbr) {
        if (this.mNeedDelOldDualBandApInfo || getDualBandApInfoSize() >= 500) {
            if (!this.mNeedDelOldDualBandApInfo) {
                this.mNeedDelOldDualBandApInfo = true;
            }
            if (!deleteOldestDualBandApInfo()) {
                return false;
            }
        }
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProDualBandApInfoRcdTable VALUES(null,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{dbr.mApBSSID, dbr.mApSSID, dbr.mInetCapability, dbr.mServingBand, dbr.mApAuthType, Integer.valueOf(dbr.mChannelFrequency), Integer.valueOf(dbr.mDisappearCount), Integer.valueOf(dbr.mInBlackList), Long.valueOf(dbr.mUpdateTime)});
            HwHiLog.i(TAG, false, "insertDualBandApInfoRcd add a record succ.", new Object[0]);
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "insertDualBandApInfoRcd error", new Object[0]);
            return false;
        }
    }

    public boolean addOrUpdateDualBandApInfoRcd(WifiProDualBandApInfoRcd dbr) {
        HwHiLog.d(TAG, false, "addOrUpdateDualBandApInfoRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (dbr != null) {
                    if (dbr.mApBSSID == null) {
                        HwHiLog.e(TAG, false, "addOrUpdateDualBandApInfoRcd null error.", new Object[0]);
                        return false;
                    }
                    dbr.mUpdateTime = System.currentTimeMillis();
                    try {
                        if (checkHistoryRecordExist(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, dbr.mApBSSID)) {
                            return updateDualBandApInfoRcd(dbr);
                        }
                        return insertDualBandApInfoRcd(dbr);
                    } catch (CheckHistoryRecordException e) {
                        HwHiLog.e(TAG, false, "Exceptions happened in addOrUpdateDualBandApInfoRcd()", new Object[0]);
                        return false;
                    }
                }
            }
            HwHiLog.e(TAG, false, "addOrUpdateDualBandApInfoRcd error.", new Object[0]);
            return false;
        }
    }

    public boolean queryDualBandApInfoRcd(String apBssid, WifiProDualBandApInfoRcd dbr) {
        HwHiLog.d(TAG, false, "queryDualBandApInfoRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "queryDualBandApInfoRcd database error.", new Object[0]);
                return false;
            } else if (apBssid == null || dbr == null) {
                HwHiLog.e(TAG, false, "queryDualBandApInfoRcd null error.", new Object[0]);
                return false;
            } else {
                Cursor c = null;
                int recCnt = 0;
                try {
                    Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable where apBSSID like ?", new String[]{apBssid});
                    while (true) {
                        if (!c2.moveToNext()) {
                            break;
                        }
                        recCnt++;
                        if (recCnt > 1) {
                            break;
                        } else if (recCnt == 1) {
                            dbr.mApBSSID = apBssid;
                            dbr.mApSSID = c2.getString(c2.getColumnIndex("apSSID"));
                            dbr.mInetCapability = Short.valueOf(c2.getShort(c2.getColumnIndex("InetCapability")));
                            dbr.mServingBand = Short.valueOf(c2.getShort(c2.getColumnIndex("ServingBand")));
                            dbr.mApAuthType = Short.valueOf(c2.getShort(c2.getColumnIndex("ApAuthType")));
                            dbr.mChannelFrequency = c2.getInt(c2.getColumnIndex("ChannelFrequency"));
                            dbr.mDisappearCount = c2.getShort(c2.getColumnIndex("DisappearCount"));
                            dbr.mInBlackList = c2.getShort(c2.getColumnIndex("isInBlackList"));
                            dbr.mUpdateTime = c2.getLong(c2.getColumnIndex("UpdateTime"));
                            HwHiLog.i(TAG, false, "read record succ", new Object[0]);
                        }
                    }
                    c2.close();
                    if (recCnt > 1) {
                        HwHiLog.e(TAG, false, "more than one record error. use first record.", new Object[0]);
                    } else if (recCnt == 0) {
                        HwHiLog.i(TAG, false, "queryDualBandApInfoRcd not record.", new Object[0]);
                        return false;
                    }
                    return true;
                } catch (SQLException e) {
                    HwHiLog.e(TAG, false, "queryDualBandApInfoRcd error", new Object[0]);
                    if (0 != 0) {
                        c.close();
                    }
                    return false;
                } catch (Throwable th) {
                    if (0 != 0) {
                        c.close();
                    }
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:0x00de, code lost:
        if (0 == 0) goto L_0x00e1;
     */
    public List<WifiProDualBandApInfoRcd> queryDualBandApInfoRcdBySsid(String ssid) {
        List<WifiProDualBandApInfoRcd> apInfoRcdList = new ArrayList<>();
        HwHiLog.d(TAG, false, "queryDualBandApInfoRcd enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (ssid == null) {
                        HwHiLog.e(TAG, false, "queryDualBandApInfoRcdBySsid null error.", new Object[0]);
                        return Collections.emptyList();
                    }
                    Cursor c = null;
                    try {
                        c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable where apSSID like ?", new String[]{ssid});
                        while (c.moveToNext()) {
                            WifiProDualBandApInfoRcd dbr = new WifiProDualBandApInfoRcd(null);
                            dbr.mApBSSID = c.getString(c.getColumnIndex("apBSSID"));
                            dbr.mApSSID = ssid;
                            dbr.mInetCapability = Short.valueOf(c.getShort(c.getColumnIndex("InetCapability")));
                            dbr.mServingBand = Short.valueOf(c.getShort(c.getColumnIndex("ServingBand")));
                            dbr.mApAuthType = Short.valueOf(c.getShort(c.getColumnIndex("ApAuthType")));
                            dbr.mChannelFrequency = c.getInt(c.getColumnIndex("ChannelFrequency"));
                            dbr.mDisappearCount = c.getShort(c.getColumnIndex("DisappearCount"));
                            dbr.mInBlackList = c.getShort(c.getColumnIndex("isInBlackList"));
                            dbr.mUpdateTime = c.getLong(c.getColumnIndex("UpdateTime"));
                            HwHiLog.i(TAG, false, "read record succ", new Object[0]);
                            apInfoRcdList.add(dbr);
                        }
                    } catch (SQLException e) {
                        HwHiLog.e(TAG, false, "queryDualBandApInfoRcdBySsid error", new Object[0]);
                        Collections.emptyList();
                    } catch (Throwable th) {
                        if (0 != 0) {
                            c.close();
                        }
                        throw th;
                    }
                    c.close();
                    if (apInfoRcdList.size() == 0) {
                        HwHiLog.i(TAG, false, "queryDualBandApInfoRcdBySsid not record.", new Object[0]);
                    }
                    return apInfoRcdList;
                }
            }
            HwHiLog.e(TAG, false, "queryDualBandApInfoRcdBySsid database error.", new Object[0]);
            return Collections.emptyList();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x00c3, code lost:
        if (0 == 0) goto L_0x00c6;
     */
    public List<WifiProDualBandApInfoRcd> getAllDualBandApInfo() {
        HwHiLog.d(TAG, false, "getAllDualBandApInfo enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            ArrayList<WifiProDualBandApInfoRcd> apInfoList = new ArrayList<>();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "queryDualBandApInfoRcd database error.", new Object[0]);
                return apInfoList;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable", null);
                while (c.moveToNext()) {
                    WifiProDualBandApInfoRcd dbr = new WifiProDualBandApInfoRcd(c.getString(c.getColumnIndex("apBSSID")));
                    dbr.mApSSID = c.getString(c.getColumnIndex("apSSID"));
                    dbr.mInetCapability = Short.valueOf(c.getShort(c.getColumnIndex("InetCapability")));
                    dbr.mServingBand = Short.valueOf(c.getShort(c.getColumnIndex("ServingBand")));
                    dbr.mApAuthType = Short.valueOf(c.getShort(c.getColumnIndex("ApAuthType")));
                    dbr.mChannelFrequency = c.getInt(c.getColumnIndex("ChannelFrequency"));
                    dbr.mDisappearCount = c.getShort(c.getColumnIndex("DisappearCount"));
                    dbr.mInBlackList = c.getShort(c.getColumnIndex("isInBlackList"));
                    dbr.mUpdateTime = c.getLong(c.getColumnIndex("UpdateTime"));
                    apInfoList.add(dbr);
                }
            } catch (SQLException e) {
                HwHiLog.e(TAG, false, "queryDualBandApInfoRcd error", new Object[0]);
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
            c.close();
            return apInfoList;
        }
    }

    private boolean updateApRSSIThreshold(String apBSSID, String rssiThreshold) {
        ContentValues values = new ContentValues();
        values.put("RSSIThreshold", rssiThreshold);
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, values, "apBSSID like ?", new String[]{apBSSID});
            if (rowChg == 0) {
                HwHiLog.e(TAG, false, "updateApRSSIThreshold update failed.", new Object[0]);
                return false;
            }
            HwHiLog.d(TAG, false, "updateApRSSIThreshold update succ, rowChg=%{public}d", new Object[]{Integer.valueOf(rowChg)});
            return true;
        } catch (SQLException e) {
            HwHiLog.e(TAG, false, "updateApRSSIThreshold error", new Object[0]);
            return false;
        }
    }

    public boolean addOrUpdateApRSSIThreshold(String apBSSID, String rssiThreshold) {
        HwHiLog.d(TAG, false, "addOrUpdateApRSSIThreshold enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || apBSSID == null || rssiThreshold == null) {
                HwHiLog.e(TAG, false, "addOrUpdateApRSSIThreshold error.", new Object[0]);
                return false;
            }
            try {
                if (!checkHistoryRecordExist(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, apBSSID)) {
                    insertDualBandApInfoRcd(new WifiProDualBandApInfoRcd(apBSSID));
                }
                return updateApRSSIThreshold(apBSSID, rssiThreshold);
            } catch (CheckHistoryRecordException e) {
                HwHiLog.e(TAG, false, "Exceptions happened in addOrUpdateApRSSIThreshold()", new Object[0]);
                return false;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0062, code lost:
        if (0 == 0) goto L_0x0065;
     */
    public String queryApRSSIThreshold(String apBssid) {
        String result = null;
        HwHiLog.d(TAG, false, "queryApRSSIThreshold enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || apBssid == null) {
                HwHiLog.e(TAG, false, "queryApRSSIThreshold database error.", new Object[0]);
                return null;
            }
            Cursor c = null;
            int recCnt = 0;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable where apBSSID like ?", new String[]{apBssid});
                while (true) {
                    if (!c.moveToNext()) {
                        break;
                    }
                    recCnt++;
                    if (recCnt > 1) {
                        break;
                    } else if (recCnt == 1) {
                        result = c.getString(c.getColumnIndex("RSSIThreshold"));
                        HwHiLog.i(TAG, false, "read record succ, RSSIThreshold = %{public}s", new Object[]{result});
                    }
                }
            } catch (SQLException e) {
                HwHiLog.e(TAG, false, "queryApRSSIThreshold error", new Object[0]);
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
            c.close();
            return result;
        }
    }

    public int getDualBandApInfoSize() {
        String str;
        String str2;
        Object[] objArr;
        HwHiLog.d(TAG, false, "getDualBandApInfoSize enter.", new Object[0]);
        synchronized (this.mBqeLock) {
            int result = -1;
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                HwHiLog.e(TAG, false, "getDualBandApInfoSize database error.", new Object[0]);
                return -1;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable", null);
                result = c.getCount();
                c.close();
                str = TAG;
                str2 = "getDualBandApInfoSize: %{public}d";
                objArr = new Object[]{Integer.valueOf(result)};
            } catch (SQLException e) {
                HwHiLog.e(TAG, false, "getDualBandApInfoSize error", new Object[0]);
                if (c != null) {
                    c.close();
                }
                str = TAG;
                str2 = "getDualBandApInfoSize: %{public}d";
                objArr = new Object[]{-1};
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                HwHiLog.d(TAG, false, "getDualBandApInfoSize: %{public}d", new Object[]{-1});
                throw th;
            }
            HwHiLog.d(str, false, str2, objArr);
            return result;
        }
    }

    private boolean deleteOldestDualBandApInfo() {
        List<WifiProDualBandApInfoRcd> allApInfos = getAllDualBandApInfo();
        if (allApInfos.size() <= 0) {
            return false;
        }
        WifiProDualBandApInfoRcd oldestApInfo = allApInfos.get(0);
        for (WifiProDualBandApInfoRcd apInfo : allApInfos) {
            if (apInfo.mUpdateTime < oldestApInfo.mUpdateTime) {
                oldestApInfo = apInfo;
            }
        }
        return deleteAllDualBandAPRcd(oldestApInfo.mApBSSID);
    }

    private static class CheckHistoryRecordException extends Exception {
        CheckHistoryRecordException() {
        }
    }
}
