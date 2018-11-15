package com.android.server.hidata.histream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;

public class HwHiStreamDataBaseManager {
    private static HwHiStreamDataBaseManager mHwHiStreamDataBaseManager = null;
    private SQLiteDatabase mDatabase;
    private HwHiStreamDataBase mHelper;
    private Object mSQLLock = new Object();

    private HwHiStreamDataBaseManager(Context context) {
        this.mHelper = new HwHiStreamDataBase(context);
        this.mDatabase = this.mHelper.getWritableDatabase();
    }

    public static HwHiStreamDataBaseManager getInstance(Context context) {
        if (mHwHiStreamDataBaseManager == null) {
            mHwHiStreamDataBaseManager = new HwHiStreamDataBaseManager(context);
        }
        return mHwHiStreamDataBaseManager;
    }

    /* JADX WARNING: Missing block: B:11:0x0018, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeHiStreamDB() {
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                this.mDatabase.close();
            }
        }
    }

    public void dropTrafficInfoTable() {
        if (this.mDatabase == null) {
            HwHiStreamUtils.logE("dropTrafficInfoTable mDatabase null:");
            return;
        }
        try {
            this.mDatabase.execSQL("DROP TABLE IF EXISTS HiStreamTrafficTable");
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateHistreamDataBase error:");
            stringBuilder.append(e);
            HwHiStreamUtils.logE(stringBuilder.toString());
        }
        HwHiStreamUtils.logE("dropTrafficInfoTable success");
    }

    private boolean updateApRecordInfo(HiStreamAPInfo dbr) {
        if (dbr == null) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateApRecordInfo:APUSRType=");
        stringBuilder.append(dbr.APUsrType);
        stringBuilder.append(",scenceId=");
        stringBuilder.append(dbr.mScenceId);
        HwHiStreamUtils.logD(stringBuilder.toString());
        ContentValues values = new ContentValues();
        values.put("SSID", dbr.mSSID);
        values.put("mScenario", Integer.valueOf(dbr.mScenceId));
        values.put("APUSRType", Integer.valueOf(dbr.APUsrType));
        try {
            if (this.mDatabase.update(HwHiStreamDataBase.HISTERAM_WECHAT_AP_NAME, values, "(SSID like ?) and (mScenario = ?)", new String[]{dbr.mSSID, String.valueOf(dbr.mScenceId)}) != 0) {
                return true;
            }
            HwHiStreamUtils.logE("updateApInfo update failed.");
            return false;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateApInfo error:");
            stringBuilder2.append(e);
            HwHiStreamUtils.logE(stringBuilder2.toString());
            return false;
        }
    }

    private boolean insertApRecordInfo(HiStreamAPInfo dbr) {
        if (dbr == null) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insertApRecordInfo:APUSRType=");
        stringBuilder.append(dbr.APUsrType);
        stringBuilder.append(",mScenceId=");
        stringBuilder.append(dbr.mScenceId);
        HwHiStreamUtils.logD(stringBuilder.toString());
        try {
            this.mDatabase.execSQL("INSERT INTO HiStreamAPRecordTable VALUES(null,  ?, ?, ?)", new Object[]{dbr.mSSID, Integer.valueOf(dbr.mScenceId), Integer.valueOf(dbr.APUsrType)});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("insertApInfo error:");
            stringBuilder2.append(e);
            HwHiStreamUtils.logE(stringBuilder2.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0028, code:
            if (r2 != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:11:0x002a, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:17:0x0046, code:
            if (r2 == null) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:18:0x0049, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkAPRecordExist(HiStreamAPInfo dbr) {
        if (dbr == null) {
            return false;
        }
        boolean ret = false;
        Cursor c = null;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamAPRecordTable where (SSID like ?) and (mScenario = ?)", new String[]{dbr.mSSID, String.valueOf(dbr.mScenceId)});
            if (c != null && c.getCount() > 0) {
                ret = true;
            }
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkAPRecordExist error:");
            stringBuilder.append(e);
            HwHiStreamUtils.logE(stringBuilder.toString());
            ret = false;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    public boolean addOrUpdateApRecordInfo(HiStreamAPInfo dbr) {
        synchronized (this.mSQLLock) {
            boolean updateApRecordInfo;
            if (this.mDatabase == null || !this.mDatabase.isOpen() || dbr == null || dbr.mSSID == null) {
                HwHiStreamUtils.logE("addOrUpdateApRecordInfo error.");
                return false;
            } else if (checkAPRecordExist(dbr)) {
                updateApRecordInfo = updateApRecordInfo(dbr);
                return updateApRecordInfo;
            } else {
                updateApRecordInfo = insertApRecordInfo(dbr);
                return updateApRecordInfo;
            }
        }
    }

    public HiStreamAPInfo queryApRecordInfo(String ssid, int scenceId) {
        if (this.mDatabase == null || !this.mDatabase.isOpen() || ssid == null) {
            HwHiStreamUtils.logE("queryApRecordInfo null error.");
            return null;
        }
        Cursor c = null;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamAPRecordTable where (SSID like ?) and (mScenario = ?)", new String[]{ssid, String.valueOf(scenceId)});
            if (c == null || c.getCount() <= 0) {
                if (c != null) {
                    c.close();
                }
                return null;
            } else if (c.moveToNext()) {
                HiStreamAPInfo record = new HiStreamAPInfo(ssid, scenceId, c.getInt(c.getColumnIndex("APUSRType")));
                if (c != null) {
                    c.close();
                }
                return record;
            } else {
                if (c != null) {
                    c.close();
                }
                return null;
            }
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("queryApRecordInfo error:");
            stringBuilder.append(e);
            HwHiStreamUtils.logE(stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return null;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private boolean checkTrafficInfoExist(String subId, int callType, int netType, long currDay) {
        if (subId == null) {
            return false;
        }
        Cursor c = null;
        if (800 == netType) {
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY = ?)", new String[]{String.valueOf(callType), String.valueOf(netType), String.valueOf(currDay)});
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkTrafficInfoExist error:");
                stringBuilder.append(e);
                HwHiStreamUtils.logE(stringBuilder.toString());
                return false;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else if (801 == netType) {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (SUBID = ?) and (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY = ?)", new String[]{subId, String.valueOf(callType), String.valueOf(netType), String.valueOf(currDay)});
        }
        if (c == null || c.getCount() <= null) {
            if (c != null) {
                c.close();
            }
            return false;
        }
        if (c != null) {
            c.close();
        }
        return true;
    }

    public HwHiStreamTrafficInfo queryTrafficInfo(String subId, int callType, int netType, long currDay) {
        SQLException e;
        if (this.mDatabase == null || !this.mDatabase.isOpen() || subId == null) {
            HwHiStreamUtils.logE("queryTrafficInfo database error.");
            return null;
        }
        Cursor c = null;
        if (800 == netType) {
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY = ?)", new String[]{String.valueOf(callType), String.valueOf(netType), String.valueOf(currDay)});
            } catch (SQLException e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("queryTrafficInfo error:");
                stringBuilder.append(e2);
                HwHiStreamUtils.logE(stringBuilder.toString());
                return null;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else if (801 == netType) {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (SUBID = ?) and (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY = ?)", new String[]{subId, String.valueOf(callType), String.valueOf(netType), String.valueOf(currDay)});
        }
        if (c == null || c.getCount() <= 0) {
            if (c != null) {
                c.close();
            }
            return null;
        } else if (c.moveToNext()) {
            e2 = new HwHiStreamTrafficInfo();
            e2.mSubId = subId;
            e2.mCallType = callType;
            e2.mNetType = netType;
            e2.mCurrDay = c.getLong(c.getColumnIndex("CURRDAY"));
            e2.mTraffic = c.getLong(c.getColumnIndex("TRAFFIC"));
            if (c != null) {
                c.close();
            }
            return e2;
        } else {
            if (c != null) {
                c.close();
            }
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:21:0x00c6, code:
            if (r5 != null) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:22:0x00c8, code:
            r5.close();
     */
    /* JADX WARNING: Missing block: B:25:0x00e1, code:
            if (r5 == null) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:26:0x00e4, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getRecentWeekTotalTraffic(String subId, int callType, int netType, long currDay) {
        int i = netType;
        long totalTraffic = 0;
        if (this.mDatabase == null || !this.mDatabase.isOpen() || subId == null) {
            HwHiStreamUtils.logE("getRecentWeekTotalTraffic database error.");
            return 0;
        }
        Cursor c = null;
        long queryStartTime = currDay - Constant.MILLISEC_SEVEN_DAYS;
        if (800 == i) {
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY >= ?) and (CURRDAY < ?)", new String[]{String.valueOf(callType), String.valueOf(netType), String.valueOf(queryStartTime), String.valueOf(currDay)});
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getRecentWeekTotalTraffic error:");
                stringBuilder.append(e);
                HwHiStreamUtils.logE(stringBuilder.toString());
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
            }
        } else if (801 == i) {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY >= ?) and (CURRDAY < ?)", new String[]{String.valueOf(callType), String.valueOf(netType), String.valueOf(queryStartTime), String.valueOf(currDay)});
        }
        if (c == null || c.getCount() <= 0) {
            HwHiStreamUtils.logE("getRecentWeekTotalTraffic no records.");
        } else {
            while (c.moveToNext()) {
                totalTraffic += c.getLong(c.getColumnIndex("TRAFFIC"));
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getRecentWeekTotalTraffic TIME = :");
                stringBuilder2.append(c.getLong(c.getColumnIndex("CURRDAY")));
                stringBuilder2.append("TAFFIC = ");
                stringBuilder2.append(c.getLong(c.getColumnIndex("TRAFFIC")));
                HwHiStreamUtils.logD(stringBuilder2.toString());
            }
        }
    }

    private int getTrafficRecordCounts(String subId, int callType, int netType) {
        SQLException e;
        if (subId == null) {
            return 0;
        }
        Cursor c = null;
        if (800 == netType) {
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?)", new String[]{String.valueOf(callType), String.valueOf(netType)});
            } catch (SQLException e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkTrafficInfoExist error:");
                stringBuilder.append(e2);
                HwHiStreamUtils.logE(stringBuilder.toString());
                return 0;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else if (801 == netType) {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (SUBID = ?) and (CALLTYPE = ?) and (NETTYPE = ?)", new String[]{subId, String.valueOf(callType), String.valueOf(netType)});
        }
        if (c == null || c.getCount() <= 0) {
            if (c != null) {
                c.close();
            }
            return 0;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getTrafficRecordCounts count = ");
        stringBuilder2.append(c.getCount());
        HwHiStreamUtils.logD(stringBuilder2.toString());
        e2 = c.getCount();
        if (c != null) {
            c.close();
        }
        return e2;
    }

    public long getEarlierTrafficRecordTime(String subId, int callType, int netType) {
        Cursor c = null;
        if (this.mDatabase == null || !this.mDatabase.isOpen() || subId == null) {
            HwHiStreamUtils.logE("getRecentWeekTotalTraffic error.");
            return 0;
        }
        if (800 == netType) {
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?)ORDER BY CURRDAY ASC", new String[]{String.valueOf(callType), String.valueOf(netType)});
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getEarlierTrafficRecordTime error:");
                stringBuilder.append(e);
                HwHiStreamUtils.logE(stringBuilder.toString());
                return 0;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else if (801 == netType) {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (SUBID = ?) and (CALLTYPE = ?) and (NETTYPE = ?)ORDER BY CURRDAY ASC", new String[]{subId, String.valueOf(callType), String.valueOf(netType)});
        }
        if (c == null || c.getCount() <= 0) {
            HwHiStreamUtils.logE("getEarlierTrafficRecordTime cannot find the Earliest record:");
            if (c != null) {
                c.close();
            }
            return 0;
        } else if (c.moveToNext()) {
            long j = c.getLong(c.getColumnIndex("CURRDAY"));
            if (c != null) {
                c.close();
            }
            return j;
        } else {
            if (c != null) {
                c.close();
            }
            return 0;
        }
    }

    private boolean deleteEarliestTrafficRecord(String subId, int callType, int netType) {
        HwHiStreamUtils.logE("deleteEarliestTrafficRecord enter");
        if (subId == null) {
            return false;
        }
        Cursor c = null;
        int recordId = -1;
        if (800 == netType) {
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (CALLTYPE = ?) and (NETTYPE = ?)ORDER BY CURRDAY ASC", new String[]{String.valueOf(callType), String.valueOf(netType)});
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("deleteEarliestTrafficRecord error:");
                stringBuilder.append(e);
                HwHiStreamUtils.logE(stringBuilder.toString());
                return false;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else if (801 == netType) {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamTrafficTable where (SUBID = ?) and (CALLTYPE = ?) and (NETTYPE = ?)ORDER BY CURRDAY ASC", new String[]{subId, String.valueOf(callType), String.valueOf(netType)});
        }
        if (c == null || c.getCount() <= 0) {
            HwHiStreamUtils.logE("deleteEarliestTrafficRecord cannot find the Earliest record:");
            if (c != null) {
                c.close();
            }
            return false;
        }
        while (c.moveToNext()) {
            recordId = c.getInt(c.getColumnIndex("_id"));
        }
        this.mDatabase.execSQL("DELETE FROM HiStreamTrafficTable where (_id = ?)", new Object[]{Integer.valueOf(recordId)});
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("deleteEarliestTrafficRecord delete the record which id = ");
        stringBuilder2.append(recordId);
        HwHiStreamUtils.logE(stringBuilder2.toString());
        if (c != null) {
            c.close();
        }
        return true;
    }

    private boolean insertTrafficInfo(String subId, int callType, int netType, long currDay, long mCurrTraffic) {
        HwHiStreamUtils.logE("insertTrafficInfo enter");
        if (7 <= getTrafficRecordCounts(subId, callType, netType)) {
            deleteEarliestTrafficRecord(subId, callType, netType);
        }
        try {
            this.mDatabase.execSQL("INSERT INTO HiStreamTrafficTable VALUES(null,  ?, ?, ?, ?, ?)", new Object[]{subId, Integer.valueOf(callType), Integer.valueOf(netType), Long.valueOf(currDay), Long.valueOf(mCurrTraffic)});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertTrafficInfo error:");
            stringBuilder.append(e);
            HwHiStreamUtils.logE(stringBuilder.toString());
            return false;
        }
    }

    private boolean updateTrafficInfo(HwHiStreamTrafficInfo dbr) {
        if (dbr == null) {
            return false;
        }
        int rowChg = 0;
        ContentValues values = new ContentValues();
        values.put("SUBID", dbr.mSubId);
        values.put("CALLTYPE", Integer.valueOf(dbr.mCallType));
        values.put("NETTYPE", Integer.valueOf(dbr.mNetType));
        values.put("CURRDAY", Long.valueOf(dbr.mCurrDay));
        values.put("TRAFFIC", Long.valueOf(dbr.mTraffic));
        try {
            if (800 == dbr.mNetType) {
                rowChg = this.mDatabase.update(HwHiStreamDataBase.HISTREAM_WECHAT_TRAFFIC_NAME, values, "(CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY = ?)", new String[]{String.valueOf(dbr.mCallType), String.valueOf(dbr.mNetType), String.valueOf(dbr.mCurrDay)});
            } else if (801 == dbr.mNetType) {
                rowChg = this.mDatabase.update(HwHiStreamDataBase.HISTREAM_WECHAT_TRAFFIC_NAME, values, "(SUBID = ?) and (CALLTYPE = ?) and (NETTYPE = ?) and (CURRDAY = ?)", new String[]{String.valueOf(dbr.mSubId), String.valueOf(dbr.mCallType), String.valueOf(dbr.mNetType), String.valueOf(dbr.mCurrDay)});
            }
            if (rowChg == 0) {
                HwHiStreamUtils.logE("updateApInfo update failed.");
                return false;
            }
            printTrafficInfo(dbr);
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UpdateTrafficInfo error:");
            stringBuilder.append(e);
            HwHiStreamUtils.logE(stringBuilder.toString());
            return false;
        }
    }

    private boolean updateTrafficInfo(String subId, int callType, int netType, long currDay, long mCurrTraffic) {
        HwHiStreamTrafficInfo record = queryTrafficInfo(subId, callType, netType, currDay);
        HwHiStreamUtils.logD("UpdateTrafficInfo enter");
        if (record == null) {
            HwHiStreamUtils.logE("UpdateTrafficInfo,record is null .");
            return false;
        }
        record.mTraffic += mCurrTraffic;
        return updateTrafficInfo(record);
    }

    public boolean addOrUpdateTrafficInfo(String subId, int callType, int netType, long currDay, long mCurrTraffic) {
        synchronized (this.mSQLLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || subId == null) {
                HwHiStreamUtils.logE("addOrUpdateTrafficInfo error.");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addOrUpdateTrafficInfo calltype=");
            stringBuilder.append(callType);
            stringBuilder.append(",netType=");
            stringBuilder.append(netType);
            stringBuilder.append(",currDay =");
            stringBuilder.append(currDay);
            stringBuilder.append(",mCurrTraffic=");
            stringBuilder.append(mCurrTraffic);
            HwHiStreamUtils.logD(stringBuilder.toString());
            boolean updateTrafficInfo;
            if (checkTrafficInfoExist(subId, callType, netType, currDay)) {
                updateTrafficInfo = updateTrafficInfo(subId, callType, netType, currDay, mCurrTraffic);
                return updateTrafficInfo;
            }
            updateTrafficInfo = insertTrafficInfo(subId, callType, netType, currDay, mCurrTraffic);
            return updateTrafficInfo;
        }
    }

    public void printTrafficInfo(HwHiStreamTrafficInfo trafficInfo) {
        if (trafficInfo == null) {
            HwHiStreamUtils.logD("printTrafficInfo ERROR ");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("trafficInfo,mCallType= ");
        stringBuilder.append(trafficInfo.mCallType);
        stringBuilder.append(" ,mNetType= ");
        stringBuilder.append(trafficInfo.mNetType);
        stringBuilder.append(" ,mCurrDay= ");
        stringBuilder.append(trafficInfo.mCurrDay);
        stringBuilder.append(" ,mTraffic = ");
        stringBuilder.append(trafficInfo.mTraffic);
        HwHiStreamUtils.logD(stringBuilder.toString());
    }

    public void insertAppStatistics(HwHistreamCHRStatisticsInfo chrInfo) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && chrInfo != null) {
            try {
                this.mDatabase.execSQL("INSERT INTO HiStreamAppStatisticsTable VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{Integer.valueOf(chrInfo.mScenario), Integer.valueOf(chrInfo.mNum), Integer.valueOf(chrInfo.mStartInWiFiCnt), Integer.valueOf(chrInfo.mStartInCellularCnt), Integer.valueOf(chrInfo.mCallInCellularDur), Integer.valueOf(chrInfo.mCallInWiFiDur), Integer.valueOf(chrInfo.mCellLv1Cnt), Integer.valueOf(chrInfo.mCellLv2Cnt), Integer.valueOf(chrInfo.mCellLv3Cnt), Integer.valueOf(chrInfo.mWiFiLv1Cnt), Integer.valueOf(chrInfo.mWiFiLv2Cnt), Integer.valueOf(chrInfo.mWiFiLv3Cnt), Integer.valueOf(chrInfo.mTrfficCell), Integer.valueOf(chrInfo.mVipSwitchCnt), Integer.valueOf(chrInfo.mStallSwitchCnt), Integer.valueOf(chrInfo.mStallSwitch0Cnt), Integer.valueOf(chrInfo.mStallSwitch1Cnt), Integer.valueOf(chrInfo.mStallSwitchAbove1Cnt), Integer.valueOf(chrInfo.mSwitch2CellCnt), Integer.valueOf(chrInfo.mSwitch2WifiCnt), Integer.valueOf(chrInfo.mMplinkDur), Integer.valueOf(chrInfo.mMplinkEnCnt), Integer.valueOf(chrInfo.mMplinkDisStallCnt), Integer.valueOf(chrInfo.mMplinkDisWifiGoodCnt), Integer.valueOf(chrInfo.mMplinkEnFailCnt), Integer.valueOf(chrInfo.mMplinkDisFailCnt), Integer.valueOf(chrInfo.mMplinkEnTraf), Integer.valueOf(chrInfo.mMplinkEnFailEnvironCnt), Integer.valueOf(chrInfo.mMplinkEnFailCoexistCnt), Integer.valueOf(chrInfo.mMplinkEnFailPingPongCnt), Integer.valueOf(chrInfo.mMplinkEnFailHistoryQoeCnt), Integer.valueOf(chrInfo.mMplinkEnFailChQoeCnt), Integer.valueOf(chrInfo.mHicureEnCnt), Integer.valueOf(chrInfo.mHicureSucCnt), Long.valueOf(chrInfo.mLastUploadTime)});
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("insertAppStatistics error:");
                stringBuilder.append(e);
                HwHiStreamUtils.logE(stringBuilder.toString());
            }
        }
    }

    public void updateAppStatistics(HwHistreamCHRStatisticsInfo chrInfo) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && chrInfo != null) {
            ContentValues values = new ContentValues();
            values.put("mScenario", Integer.valueOf(chrInfo.mScenario));
            values.put("mNum", Integer.valueOf(chrInfo.mNum));
            values.put("mStartInWiFiCnt", Integer.valueOf(chrInfo.mStartInWiFiCnt));
            values.put("mStartInCellularCnt", Integer.valueOf(chrInfo.mStartInCellularCnt));
            values.put("mCallInCellularDur", Integer.valueOf(chrInfo.mCallInCellularDur));
            values.put("mCallInWiFiDur", Integer.valueOf(chrInfo.mCallInWiFiDur));
            values.put("mCellLv1Cnt", Integer.valueOf(chrInfo.mCellLv1Cnt));
            values.put("mCellLv2Cnt", Integer.valueOf(chrInfo.mCellLv2Cnt));
            values.put("mCellLv3Cnt", Integer.valueOf(chrInfo.mCellLv3Cnt));
            values.put("mWiFiLv1Cnt", Integer.valueOf(chrInfo.mWiFiLv1Cnt));
            values.put("mWiFiLv2Cnt", Integer.valueOf(chrInfo.mWiFiLv2Cnt));
            values.put("mWiFiLv3Cnt", Integer.valueOf(chrInfo.mWiFiLv3Cnt));
            values.put("mTrfficCell", Integer.valueOf(chrInfo.mTrfficCell));
            values.put("mVipSwitchCnt", Integer.valueOf(chrInfo.mVipSwitchCnt));
            values.put("mStallSwitchCnt", Integer.valueOf(chrInfo.mStallSwitchCnt));
            values.put("mStallSwitch0Cnt", Integer.valueOf(chrInfo.mStallSwitch0Cnt));
            values.put("mStallSwitch1Cnt", Integer.valueOf(chrInfo.mStallSwitch1Cnt));
            values.put("mStallSwitchAbove1Cnt", Integer.valueOf(chrInfo.mStallSwitchAbove1Cnt));
            values.put("mSwitch2CellCnt", Integer.valueOf(chrInfo.mSwitch2CellCnt));
            values.put("mSwitch2WifiCnt", Integer.valueOf(chrInfo.mSwitch2WifiCnt));
            values.put("mMplinkDur", Integer.valueOf(chrInfo.mMplinkDur));
            values.put("mMplinkEnCnt", Integer.valueOf(chrInfo.mMplinkEnCnt));
            values.put("mMplinkDisStallCnt", Integer.valueOf(chrInfo.mMplinkDisStallCnt));
            values.put("mMplinkDisWifiGoodCnt", Integer.valueOf(chrInfo.mMplinkDisWifiGoodCnt));
            values.put("mMplinkEnFailCnt", Integer.valueOf(chrInfo.mMplinkEnFailCnt));
            values.put("mMplinkDisFailCnt", Integer.valueOf(chrInfo.mMplinkDisFailCnt));
            values.put("mMplinkEnTraf", Integer.valueOf(chrInfo.mMplinkEnTraf));
            values.put("MplinkEnFailEnvironCnt", Integer.valueOf(chrInfo.mMplinkEnFailEnvironCnt));
            values.put("mMplinkEnFailCoexistCnt", Integer.valueOf(chrInfo.mMplinkEnFailCoexistCnt));
            values.put("mMplinkEnFailPingPongCnt", Integer.valueOf(chrInfo.mMplinkEnFailPingPongCnt));
            values.put("mMplinkEnFailHistoryQoeCnt", Integer.valueOf(chrInfo.mMplinkEnFailHistoryQoeCnt));
            values.put("mMplinkEnFailChQoeCnt", Integer.valueOf(chrInfo.mMplinkEnFailChQoeCnt));
            values.put("mHicureEnCnt", Integer.valueOf(chrInfo.mHicureEnCnt));
            values.put("mHicureSucCnt", Integer.valueOf(chrInfo.mHicureSucCnt));
            values.put("mLastUploadTime", Long.valueOf(chrInfo.mLastUploadTime));
            try {
                this.mDatabase.update(HwHiStreamDataBase.HISTREAM_APP_STATISTICS, values, "mScenario = ?", new String[]{String.valueOf(chrInfo.mScenario)});
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateAppStatistics error:");
                stringBuilder.append(e);
                HwHiStreamUtils.logE(stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x01dc, code:
            if (r0 != null) goto L_0x01de;
     */
    /* JADX WARNING: Missing block: B:14:0x01de, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:19:0x01f9, code:
            if (r0 == null) goto L_0x01fc;
     */
    /* JADX WARNING: Missing block: B:20:0x01fc, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwHistreamCHRStatisticsInfo getAppStatistics(int scenario) {
        HwHistreamCHRStatisticsInfo statistics = null;
        if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            HwHiStreamUtils.logE("getAppStatistics database error.");
            return null;
        }
        Cursor c = null;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM HiStreamAppStatisticsTable where (mScenario = ?)", new String[]{String.valueOf(scenario)});
            if (c != null && c.getCount() > 0 && c.moveToNext()) {
                statistics = new HwHistreamCHRStatisticsInfo(scenario);
                statistics.mScenario = c.getInt(c.getColumnIndex("mScenario"));
                statistics.mNum = c.getInt(c.getColumnIndex("mNum"));
                statistics.mStartInWiFiCnt = c.getInt(c.getColumnIndex("mStartInWiFiCnt"));
                statistics.mStartInCellularCnt = c.getInt(c.getColumnIndex("mStartInCellularCnt"));
                statistics.mCallInCellularDur = c.getInt(c.getColumnIndex("mCallInCellularDur"));
                statistics.mCallInWiFiDur = c.getInt(c.getColumnIndex("mCallInWiFiDur"));
                statistics.mCellLv1Cnt = c.getInt(c.getColumnIndex("mCellLv1Cnt"));
                statistics.mCellLv2Cnt = c.getInt(c.getColumnIndex("mCellLv2Cnt"));
                statistics.mCellLv2Cnt = c.getInt(c.getColumnIndex("mCellLv2Cnt"));
                statistics.mWiFiLv1Cnt = c.getInt(c.getColumnIndex("mWiFiLv1Cnt"));
                statistics.mWiFiLv2Cnt = c.getInt(c.getColumnIndex("mWiFiLv2Cnt"));
                statistics.mWiFiLv3Cnt = c.getInt(c.getColumnIndex("mWiFiLv3Cnt"));
                statistics.mTrfficCell = c.getInt(c.getColumnIndex("mTrfficCell"));
                statistics.mVipSwitchCnt = c.getInt(c.getColumnIndex("mVipSwitchCnt"));
                statistics.mStallSwitchCnt = c.getInt(c.getColumnIndex("mStallSwitchCnt"));
                statistics.mStallSwitch0Cnt = c.getInt(c.getColumnIndex("mStallSwitch0Cnt"));
                statistics.mStallSwitch1Cnt = c.getInt(c.getColumnIndex("mStallSwitch1Cnt"));
                statistics.mStallSwitchAbove1Cnt = c.getInt(c.getColumnIndex("mStallSwitchAbove1Cnt"));
                statistics.mSwitch2CellCnt = c.getInt(c.getColumnIndex("mSwitch2CellCnt"));
                statistics.mSwitch2WifiCnt = c.getInt(c.getColumnIndex("mSwitch2WifiCnt"));
                statistics.mLastUploadTime = c.getLong(c.getColumnIndex("mLastUploadTime"));
                statistics.mMplinkDur = c.getInt(c.getColumnIndex("mMplinkDur"));
                statistics.mMplinkEnCnt = c.getInt(c.getColumnIndex("mMplinkEnCnt"));
                statistics.mMplinkDisStallCnt = c.getInt(c.getColumnIndex("mMplinkDisStallCnt"));
                statistics.mMplinkDisWifiGoodCnt = c.getInt(c.getColumnIndex("mMplinkDisWifiGoodCnt"));
                statistics.mMplinkEnFailCnt = c.getInt(c.getColumnIndex("mMplinkEnFailCnt"));
                statistics.mMplinkDisFailCnt = c.getInt(c.getColumnIndex("mMplinkDisFailCnt"));
                statistics.mMplinkEnTraf = c.getInt(c.getColumnIndex("mMplinkEnTraf"));
                statistics.mMplinkEnFailEnvironCnt = c.getInt(c.getColumnIndex("MplinkEnFailEnvironCnt"));
                statistics.mMplinkEnFailCoexistCnt = c.getInt(c.getColumnIndex("mMplinkEnFailCoexistCnt"));
                statistics.mMplinkEnFailPingPongCnt = c.getInt(c.getColumnIndex("mMplinkEnFailPingPongCnt"));
                statistics.mMplinkEnFailHistoryQoeCnt = c.getInt(c.getColumnIndex("mMplinkEnFailHistoryQoeCnt"));
                statistics.mMplinkEnFailChQoeCnt = c.getInt(c.getColumnIndex("mMplinkEnFailChQoeCnt"));
                statistics.mHicureEnCnt = c.getInt(c.getColumnIndex("mHicureEnCnt"));
                statistics.mHicureSucCnt = c.getInt(c.getColumnIndex("mHicureSucCnt"));
            }
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppStatistics error:");
            stringBuilder.append(e);
            HwHiStreamUtils.logE(stringBuilder.toString());
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }
}
