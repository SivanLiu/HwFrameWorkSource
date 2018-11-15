package com.android.server.wifi.ABS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class HwABSDataBaseManager {
    private static final int DATA_BASE_MAX_NUM = 500;
    private static final String TAG = "DataBaseManager";
    private static HwABSDataBaseManager mHwABSDataBaseManager = null;
    private SQLiteDatabase mDatabase;
    private HwABSDataBaseHelper mHelper;
    private Object mLock = new Object();

    private HwABSDataBaseManager(Context context) {
        HwABSUtils.logD("HwABSDataBaseManager()");
        try {
            this.mHelper = new HwABSDataBaseHelper(context);
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            this.mDatabase = null;
        }
    }

    public static HwABSDataBaseManager getInstance(Context context) {
        if (mHwABSDataBaseManager == null) {
            mHwABSDataBaseManager = new HwABSDataBaseManager(context);
        }
        return mHwABSDataBaseManager;
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeDB() {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                HwABSUtils.logD("HwABSDataBaseManager closeDB()");
                this.mDatabase.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x002f, code:
            return;
     */
    /* JADX WARNING: Missing block: B:15:0x0031, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addOrUpdateApInfos(HwABSApInfoData data) {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || data == null) {
            } else if (getApInfoByBssid(data.mBssid) == null) {
                HwABSUtils.logD("addOrUpdateApInfos inlineAddApInfo");
                checkIfAllCaseNumSatisfy();
                inlineAddApInfo(data);
            } else {
                HwABSUtils.logD("addOrUpdateApInfos");
                inlineUpdateApInfo(data);
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x00a5, code:
            return r3;
     */
    /* JADX WARNING: Missing block: B:32:0x00cc, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<HwABSApInfoData> getApInfoBySsid(String ssid) {
        synchronized (this.mLock) {
            ArrayList lists = new ArrayList();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable where ssid like ?", new String[]{ssid});
                    while (c.moveToNext()) {
                        lists.add(new HwABSApInfoData(c.getString(c.getColumnIndex("bssid")), c.getString(c.getColumnIndex("ssid")), c.getInt(c.getColumnIndex("switch_mimo_type")), c.getInt(c.getColumnIndex("switch_siso_type")), c.getInt(c.getColumnIndex("auth_type")), c.getInt(c.getColumnIndex("in_black_list")), c.getInt(c.getColumnIndex("reassociate_times")), c.getInt(c.getColumnIndex("failed_times")), c.getInt(c.getColumnIndex("continuous_failure_times")), c.getLong(c.getColumnIndex("last_connect_time"))));
                    }
                    if (c != null) {
                        c.close();
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getApInfoBySsid:");
                        stringBuilder.append(e);
                        HwABSUtils.logE(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return null;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0096, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwABSApInfoData getApInfoByBssid(String bssid) {
        synchronized (this.mLock) {
            HwABSApInfoData data = null;
            if (bssid == null) {
                return null;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable where bssid like ?", new String[]{bssid});
                if (c.moveToNext()) {
                    data = new HwABSApInfoData(c.getString(c.getColumnIndex("bssid")), c.getString(c.getColumnIndex("ssid")), c.getInt(c.getColumnIndex("switch_mimo_type")), c.getInt(c.getColumnIndex("switch_siso_type")), c.getInt(c.getColumnIndex("auth_type")), c.getInt(c.getColumnIndex("in_black_list")), c.getInt(c.getColumnIndex("reassociate_times")), c.getInt(c.getColumnIndex("failed_times")), c.getInt(c.getColumnIndex("continuous_failure_times")), c.getLong(c.getColumnIndex("last_connect_time")));
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getApInfoByBssid:");
                    stringBuilder.append(e);
                    HwABSUtils.logE(stringBuilder.toString());
                    if (c != null) {
                        c.close();
                    }
                    return null;
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x00a7, code:
            return r4;
     */
    /* JADX WARNING: Missing block: B:33:0x00ce, code:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<HwABSApInfoData> getApInfoInBlackList() {
        synchronized (this.mLock) {
            ArrayList lists = new ArrayList();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable where in_black_list like ?", new String[]{"1"});
                    while (c.moveToNext()) {
                        lists.add(new HwABSApInfoData(c.getString(c.getColumnIndex("bssid")), c.getString(c.getColumnIndex("ssid")), c.getInt(c.getColumnIndex("switch_mimo_type")), c.getInt(c.getColumnIndex("switch_siso_type")), c.getInt(c.getColumnIndex("auth_type")), c.getInt(c.getColumnIndex("in_black_list")), c.getInt(c.getColumnIndex("reassociate_times")), c.getInt(c.getColumnIndex("failed_times")), c.getInt(c.getColumnIndex("continuous_failure_times")), c.getLong(c.getColumnIndex("last_connect_time"))));
                    }
                    if (c != null) {
                        c.close();
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getApInfoByBssid:");
                        stringBuilder.append(e);
                        HwABSUtils.logE(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return lists;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void deleteAPInfosByBssid(HwABSApInfoData data) {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || data == null) {
            } else {
                inlineDeleteApInfoByBssid(data.mBssid);
            }
        }
    }

    public void deleteAPInfosBySsid(HwABSApInfoData data) {
        synchronized (this.mLock) {
            if (data == null) {
                return;
            }
            inlineDeleteApInfoBySsid(data.mSsid);
        }
    }

    private void inlineDeleteApInfoBySsid(String ssid) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && ssid != null) {
            this.mDatabase.delete(HwABSDataBaseHelper.MIMO_AP_TABLE_NAME, "ssid like ?", new String[]{ssid});
        }
    }

    private void inlineDeleteApInfoByBssid(String bssid) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && bssid != null) {
            this.mDatabase.delete(HwABSDataBaseHelper.MIMO_AP_TABLE_NAME, "bssid like ?", new String[]{bssid});
        }
    }

    private void inlineAddApInfo(HwABSApInfoData data) {
        if (data.mBssid != null) {
            this.mDatabase.execSQL("INSERT INTO MIMOApInfoTable VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{data.mBssid, data.mSsid, Integer.valueOf(data.mSwitch_mimo_type), Integer.valueOf(data.mSwitch_siso_type), Integer.valueOf(data.mAuth_type), Integer.valueOf(data.mIn_black_List), Integer.valueOf(0), Integer.valueOf(data.mReassociate_times), Integer.valueOf(data.mFailed_times), Integer.valueOf(data.mContinuous_failure_times), Long.valueOf(data.mLast_connect_time), Integer.valueOf(0)});
        }
    }

    private void inlineUpdateApInfo(HwABSApInfoData data) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && data.mBssid != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("inlineUpdateApInfo ssid = ");
            stringBuilder.append(data.mSsid);
            HwABSUtils.logD(stringBuilder.toString());
            ContentValues values = new ContentValues();
            values.put("bssid", data.mBssid);
            values.put("ssid", data.mSsid);
            values.put("switch_mimo_type", Integer.valueOf(data.mSwitch_mimo_type));
            values.put("switch_siso_type", Integer.valueOf(data.mSwitch_siso_type));
            values.put("auth_type", Integer.valueOf(data.mAuth_type));
            values.put("in_black_list", Integer.valueOf(data.mIn_black_List));
            values.put("in_vowifi_black_list", Integer.valueOf(0));
            values.put("reassociate_times", Integer.valueOf(data.mReassociate_times));
            values.put("failed_times", Integer.valueOf(data.mFailed_times));
            values.put("continuous_failure_times", Integer.valueOf(data.mContinuous_failure_times));
            values.put("last_connect_time", Long.valueOf(data.mLast_connect_time));
            this.mDatabase.update(HwABSDataBaseHelper.MIMO_AP_TABLE_NAME, values, "bssid like ?", new String[]{data.mBssid});
        }
    }

    private void checkIfAllCaseNumSatisfy() {
        List<HwABSApInfoData> lists = getAllApInfo();
        long last_connect_time = 0;
        String bssid = null;
        boolean isDeleteRecord = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkIfAllCaseNumSatisfy lists.size() = ");
        stringBuilder.append(lists.size());
        HwABSUtils.logD(stringBuilder.toString());
        if (lists.size() >= 500) {
            isDeleteRecord = true;
            for (HwABSApInfoData data : lists) {
                long current_connect_time = data.mLast_connect_time;
                if (last_connect_time == 0 || last_connect_time > current_connect_time) {
                    last_connect_time = current_connect_time;
                    bssid = data.mBssid;
                }
            }
        }
        if (isDeleteRecord) {
            synchronized (this.mLock) {
                inlineDeleteApInfoByBssid(bssid);
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x009f, code:
            return r4;
     */
    /* JADX WARNING: Missing block: B:33:0x00c6, code:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<HwABSApInfoData> getAllApInfo() {
        synchronized (this.mLock) {
            Cursor c = null;
            ArrayList lists = new ArrayList();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable", null);
                    while (c.moveToNext()) {
                        lists.add(new HwABSApInfoData(c.getString(c.getColumnIndex("bssid")), c.getString(c.getColumnIndex("ssid")), c.getInt(c.getColumnIndex("switch_mimo_type")), c.getInt(c.getColumnIndex("switch_siso_type")), c.getInt(c.getColumnIndex("auth_type")), c.getInt(c.getColumnIndex("in_black_list")), c.getInt(c.getColumnIndex("reassociate_times")), c.getInt(c.getColumnIndex("failed_times")), c.getInt(c.getColumnIndex("continuous_failure_times")), c.getLong(c.getColumnIndex("last_connect_time"))));
                    }
                    if (c != null) {
                        c.close();
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getAllApInfo:");
                        stringBuilder.append(e);
                        HwABSUtils.logE(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return null;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0118, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:31:0x013f, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwABSCHRStatistics getCHRStatistics() {
        synchronized (this.mLock) {
            HwABSCHRStatistics statistics = null;
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM StatisticsTable", null);
                    if (c.moveToNext()) {
                        statistics = new HwABSCHRStatistics();
                        statistics.long_connect_event = c.getInt(c.getColumnIndex("long_connect_event"));
                        statistics.short_connect_event = c.getInt(c.getColumnIndex("short_connect_event"));
                        statistics.search_event = c.getInt(c.getColumnIndex("search_event"));
                        statistics.antenna_preempted_screen_on_event = c.getInt(c.getColumnIndex("antenna_preempted_screen_on_event"));
                        statistics.antenna_preempted_screen_off_event = c.getInt(c.getColumnIndex("antenna_preempted_screen_off_event"));
                        statistics.mo_mt_call_event = c.getInt(c.getColumnIndex("mo_mt_call_event"));
                        statistics.siso_to_mimo_event = c.getInt(c.getColumnIndex("siso_to_mimo_event"));
                        statistics.ping_pong_times = c.getInt(c.getColumnIndex("ping_pong_times"));
                        statistics.max_ping_pong_times = c.getInt(c.getColumnIndex("max_ping_pong_times"));
                        statistics.siso_time = (long) c.getInt(c.getColumnIndex("siso_time"));
                        statistics.mimo_time = (long) c.getInt(c.getColumnIndex("mimo_time"));
                        statistics.mimo_screen_on_time = (long) c.getInt(c.getColumnIndex("mimo_screen_on_time"));
                        statistics.siso_screen_on_time = (long) c.getInt(c.getColumnIndex("siso_screen_on_time"));
                        statistics.last_upload_time = c.getLong(c.getColumnIndex("last_upload_time"));
                        statistics.mRssiL0 = c.getInt(c.getColumnIndex("rssiL0"));
                        statistics.mRssiL1 = c.getInt(c.getColumnIndex("rssiL1"));
                        statistics.mRssiL2 = c.getInt(c.getColumnIndex("rssiL2"));
                        statistics.mRssiL3 = c.getInt(c.getColumnIndex("rssiL3"));
                        statistics.mRssiL4 = c.getInt(c.getColumnIndex("rssiL4"));
                    }
                    if (c != null) {
                        c.close();
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getCHRStatistics: ");
                        stringBuilder.append(e);
                        HwABSUtils.logE(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return null;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x00dd, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void inlineAddCHRInfo(HwABSCHRStatistics data) {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                this.mDatabase.execSQL("INSERT INTO StatisticsTable VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{Integer.valueOf(data.long_connect_event), Integer.valueOf(data.short_connect_event), Integer.valueOf(data.search_event), Integer.valueOf(data.antenna_preempted_screen_on_event), Integer.valueOf(data.antenna_preempted_screen_off_event), Integer.valueOf(data.mo_mt_call_event), Integer.valueOf(data.siso_to_mimo_event), Integer.valueOf(data.ping_pong_times), Integer.valueOf(data.max_ping_pong_times), Long.valueOf(data.mimo_time), Long.valueOf(data.siso_time), Long.valueOf(data.mimo_screen_on_time), Long.valueOf(data.siso_screen_on_time), Long.valueOf(data.last_upload_time), Integer.valueOf(data.mRssiL0), Integer.valueOf(data.mRssiL1), Integer.valueOf(data.mRssiL2), Integer.valueOf(data.mRssiL3), Integer.valueOf(data.mRssiL4), Integer.valueOf(0)});
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x00fe, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void inlineUpdateCHRInfo(HwABSCHRStatistics data) {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                HwABSUtils.logD("inlineUpdateCHRInfo ");
                ContentValues values = new ContentValues();
                values.put("long_connect_event", Integer.valueOf(data.long_connect_event));
                values.put("short_connect_event", Integer.valueOf(data.short_connect_event));
                values.put("search_event", Integer.valueOf(data.search_event));
                values.put("antenna_preempted_screen_on_event", Integer.valueOf(data.antenna_preempted_screen_on_event));
                values.put("antenna_preempted_screen_off_event", Integer.valueOf(data.antenna_preempted_screen_off_event));
                values.put("mo_mt_call_event", Integer.valueOf(data.mo_mt_call_event));
                values.put("siso_to_mimo_event", Integer.valueOf(data.siso_to_mimo_event));
                values.put("ping_pong_times", Integer.valueOf(data.ping_pong_times));
                values.put("max_ping_pong_times", Integer.valueOf(data.max_ping_pong_times));
                values.put("mimo_time", Long.valueOf(data.mimo_time));
                values.put("siso_time", Long.valueOf(data.siso_time));
                values.put("mimo_screen_on_time", Long.valueOf(data.mimo_screen_on_time));
                values.put("siso_screen_on_time", Long.valueOf(data.siso_screen_on_time));
                values.put("last_upload_time", Long.valueOf(data.last_upload_time));
                values.put("rssiL0", Integer.valueOf(data.mRssiL0));
                values.put("rssiL1", Integer.valueOf(data.mRssiL1));
                values.put("rssiL2", Integer.valueOf(data.mRssiL2));
                values.put("rssiL3", Integer.valueOf(data.mRssiL3));
                values.put("rssiL4", Integer.valueOf(data.mRssiL4));
                this.mDatabase.update(HwABSDataBaseHelper.STATISTICS_TABLE_NAME, values, "_id like ?", new String[]{"1"});
            }
        }
    }
}
