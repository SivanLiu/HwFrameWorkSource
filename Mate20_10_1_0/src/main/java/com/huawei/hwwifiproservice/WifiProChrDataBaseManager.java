package com.huawei.hwwifiproservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.server.wifi.HwQoE.HwQoEUtils;

public class WifiProChrDataBaseManager {
    private static final int DBG_LOG_LEVEL = 1;
    private static final int ERROR_LOG_LEVEL = 3;
    private static final int INFO_LOG_LEVEL = 2;
    private static final String TAG = "WifiProChrDataBaseManager";
    private static WifiProChrDataBaseManager mChrDataBaseManager;
    private static int printLogLevel = 1;
    private final Object mChrLock = new Object();
    private SQLiteDatabase mDatabase;
    private WifiProChrDataBaseHelper mHelper;

    public WifiProChrDataBaseManager(Context context) {
        logI("WifiProChrDataBaseManager()");
        if (context != null) {
            this.mHelper = new WifiProChrDataBaseHelper(context);
            try {
                this.mDatabase = this.mHelper.getWritableDatabase();
            } catch (SQLiteCantOpenDatabaseException e) {
                logE("WifiProChrDataBaseManager(), can't open database!");
            }
        }
    }

    public static WifiProChrDataBaseManager getInstance(Context context) {
        if (mChrDataBaseManager == null) {
            mChrDataBaseManager = new WifiProChrDataBaseManager(context);
        }
        return mChrDataBaseManager;
    }

    public void closeDB() {
        synchronized (this.mChrLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    logI("closeDB()");
                    this.mDatabase.close();
                }
            }
        }
    }

    private boolean checkIfRcdExist() {
        int recCnt = 0;
        logI("checkIfRcdExist enter.");
        Cursor c = null;
        try {
            Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM CHRStatTable where _id like ?", new String[]{"1"});
            while (true) {
                if (!c2.moveToNext()) {
                    break;
                }
                recCnt++;
                if (recCnt > 1) {
                    break;
                }
                String lastStatUploadTime = c2.getString(c2.getColumnIndex("mLastStatUploadTime"));
                logI("checkIfRcdExist read record succ, lastStatUploadTime:" + lastStatUploadTime);
            }
            if (recCnt > 1) {
                logE("more than one record error. ");
            } else if (recCnt == 0) {
                c2.close();
                return false;
            }
            c2.close();
            return true;
        } catch (SQLException e) {
            logE("checkIfRcdExist error:" + e);
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

    private boolean updateChrStatRcd(WifiProStatisticsRecord dbr) {
        ContentValues values = new ContentValues();
        values.put("mLastStatUploadTime", dbr.mLastStatUploadTime);
        values.put("mLastWifiproState", Short.valueOf(dbr.mLastWifiproState));
        values.put("mLastWifiproStateUpdateTime", dbr.mLastWifiproStateUpdateTime);
        values.put("mEnableTotTime", Integer.valueOf(dbr.mEnableTotTime));
        values.put("mNoInetHandoverCount", Short.valueOf(dbr.mNoInetHandoverCount));
        values.put("mPortalUnauthCount", Short.valueOf(dbr.mPortalUnauthCount));
        values.put("mWifiScoCount", Short.valueOf(dbr.mWifiScoCount));
        values.put("mPortalCodeParseCount", Short.valueOf(dbr.mPortalCodeParseCount));
        values.put("mRcvSMS_Count", Short.valueOf(dbr.mRcvSmsCount));
        values.put("mPortalAutoLoginCount", Short.valueOf(dbr.mPortalAutoLoginCount));
        values.put("mCellAutoOpenCount", Short.valueOf(dbr.mCellAutoOpenCount));
        values.put("mCellAutoCloseCount", Short.valueOf(dbr.mCellAutoCloseCount));
        values.put("mTotalBQE_BadROC", Short.valueOf(dbr.mTotalBqeBadRoc));
        values.put("mManualBackROC", Short.valueOf(dbr.mManualBackROC));
        values.put("mRSSI_RO_Tot", Short.valueOf(dbr.mRssiRoTot));
        values.put("mRSSI_ErrRO_Tot", Short.valueOf(dbr.mRssiErrRoTot));
        values.put("mOTA_RO_Tot", Short.valueOf(dbr.mOtaRoTot));
        values.put("mOTA_ErrRO_Tot", Short.valueOf(dbr.mOtaErrRoTot));
        values.put("mTCP_RO_Tot", Short.valueOf(dbr.mTcpRoTot));
        values.put("mTCP_ErrRO_Tot", Short.valueOf(dbr.mTcpErrRoTot));
        values.put("mManualRI_TotTime", Integer.valueOf(dbr.mManualRiTotTime));
        values.put("mAutoRI_TotTime", Integer.valueOf(dbr.mAutoRiTotTime));
        values.put("mAutoRI_TotCount", Short.valueOf(dbr.mAutoRiTotCount));
        values.put("mRSSI_RestoreRI_Count", Short.valueOf(dbr.mRssiRestoreRiCount));
        values.put("mRSSI_BetterRI_Count", Short.valueOf(dbr.mRssiBetterRiCount));
        values.put("mTimerRI_Count", Short.valueOf(dbr.mTimerRiCount));
        values.put("mHisScoRI_Count", Short.valueOf(dbr.mHisScoRiCount));
        values.put("mUserCancelROC", Short.valueOf(dbr.mUserCancelROC));
        values.put("mWifiToWifiSuccCount", Short.valueOf(dbr.mWifiToWifiSuccCount));
        values.put("mNoInetAlarmCount", Short.valueOf(dbr.mNoInetAlarmCount));
        values.put("mWifiOobInitState", Short.valueOf(dbr.mWifiOobInitState));
        values.put("mNotAutoConnPortalCnt", Short.valueOf(dbr.mNotAutoConnPortalCnt));
        values.put("mHighDataRateStopROC", Short.valueOf(dbr.mHighDataRateStopROC));
        values.put("mSelectNotInetAPCount", Short.valueOf(dbr.mSelectNotInetAPCount));
        values.put("mUserUseBgScanAPCount", Short.valueOf(dbr.mUserUseBgScanAPCount));
        values.put("mPingPongCount", Short.valueOf(dbr.mPingPongCount));
        values.put("mBQE_BadSettingCancel", Short.valueOf(dbr.mBqeBadSettingCancel));
        values.put("mNotInetSettingCancel", Short.valueOf(dbr.mNotInetSettingCancel));
        values.put("mNotInetUserCancel", Short.valueOf(dbr.mNotInetUserCancel));
        values.put("mNotInetRestoreRI", Short.valueOf(dbr.mNotInetRestoreRI));
        values.put("mNotInetUserManualRI", Short.valueOf(dbr.mNotInetUserManualRI));
        values.put("mNotInetWifiToWifiCount", Short.valueOf(dbr.mNotInetWifiToWifiCount));
        values.put("mReopenWifiRICount", Short.valueOf(dbr.mReopenWifiRICount));
        values.put("mSelCSPShowDiglogCount", Short.valueOf(dbr.mSelCSPShowDiglogCount));
        values.put("mSelCSPAutoSwCount", Short.valueOf(dbr.mSelCSPAutoSwCount));
        values.put("mSelCSPNotSwCount", Short.valueOf(dbr.mSelCSPNotSwCount));
        values.put("mTotBtnRICount", Short.valueOf(dbr.mTotBtnRICount));
        values.put("mBMD_TenMNotifyCount", Short.valueOf(dbr.mBmdTenmNotifyCount));
        values.put("mBMD_TenM_RI_Count", Short.valueOf(dbr.mBmdTenmRiCount));
        values.put("mBMD_FiftyMNotifyCount", Short.valueOf(dbr.mBmdFiftymNotifyCount));
        values.put("mBMD_FiftyM_RI_Count", Short.valueOf(dbr.mBmdFiftymRiCount));
        values.put("mBMD_UserDelNotifyCount", Short.valueOf(dbr.mBmdUserDelNotifyCount));
        values.put("mRO_TotMobileData", Integer.valueOf(dbr.mRoTotMobileData));
        values.put("mAF_PhoneNumSuccCnt", Short.valueOf(dbr.mAfPhoneNumSuccCnt));
        values.put("mAF_PhoneNumFailCnt", Short.valueOf(dbr.mAfPhoneNumFailCnt));
        values.put("mAF_PasswordSuccCnt", Short.valueOf(dbr.mAfPasswordSuccCnt));
        values.put("mAF_PasswordFailCnt", Short.valueOf(dbr.mAfPasswordFailCnt));
        values.put("mAF_AutoLoginSuccCnt", Short.valueOf(dbr.mAfAutoLoginSuccCnt));
        values.put("mAF_AutoLoginFailCnt", Short.valueOf(dbr.mAfAutoLoginFailCnt));
        values.put("mBG_BgRunCnt", Short.valueOf(dbr.mBgBgRunCnt));
        values.put("mBG_SettingRunCnt", Short.valueOf(dbr.mBgSettingRunCnt));
        values.put("mBG_FreeInetOkApCnt", Short.valueOf(dbr.mBgFreeInetOkApCnt));
        values.put("mBG_FishingApCnt", Short.valueOf(dbr.mBgFishingApCnt));
        values.put("mBG_FreeNotInetApCnt", Short.valueOf(dbr.mBgFreeNotInetApCnt));
        values.put("mBG_PortalApCnt", Short.valueOf(dbr.mBgPortalApCnt));
        values.put("mBG_FailedCnt", Short.valueOf(dbr.mBgFailedCnt));
        values.put("mBG_InetNotOkActiveOk", Short.valueOf(dbr.mBgInetNotOkActiveOk));
        values.put("mBG_InetOkActiveNotOk", Short.valueOf(dbr.mBgInetOkActiveNotOk));
        values.put("mBG_UserSelApFishingCnt", Short.valueOf(dbr.mBgUserSelApFishingCnt));
        values.put("mBG_ConntTimeoutCnt", Short.valueOf(dbr.mBgConntTimeoutCnt));
        values.put("mBG_DNSFailCnt", Short.valueOf(dbr.mBgDnsFailCnt));
        values.put("mBG_DHCPFailCnt", Short.valueOf(dbr.mBgDhcpFailCnt));
        values.put("mBG_AUTH_FailCnt", Short.valueOf(dbr.mBgAuthFailCnt));
        values.put("mBG_AssocRejectCnt", Short.valueOf(dbr.mBgAssocRejectCnt));
        values.put("mBG_UserSelFreeInetOkCnt", Short.valueOf(dbr.mBgUserSelFreeInetOkCnt));
        values.put("mBG_UserSelNoInetCnt", Short.valueOf(dbr.mBgUserSelNoInetCnt));
        values.put("mBG_UserSelPortalCnt", Short.valueOf(dbr.mBgUserSelPortalCnt));
        values.put("mBG_FoundTwoMoreApCnt", Short.valueOf(dbr.mBgFoundTwoMoreApCnt));
        values.put("mAF_FPNSuccNotMsmCnt", Short.valueOf(dbr.mAfFpnsSuccNotMsmCnt));
        values.put("mBSG_RsGoodCnt", Short.valueOf(dbr.mBsgRsGoodCnt));
        values.put("mBSG_RsMidCnt", Short.valueOf(dbr.mBsgRsMidCnt));
        values.put("mBSG_RsBadCnt", Short.valueOf(dbr.mBsgRsBadCnt));
        values.put("mBSG_EndIn4sCnt", Short.valueOf(dbr.mBsgEndIn4sCnt));
        values.put("mBSG_EndIn4s7sCnt", Short.valueOf(dbr.mBsgEndIn4s7sCnt));
        values.put("mBSG_NotEndIn7sCnt", Short.valueOf(dbr.mBsgNotEndIn7sCnt));
        values.put("mBG_NCByConnectFail", Short.valueOf(dbr.mBgNcbyConnectFail));
        values.put("mBG_NCByCheckFail", Short.valueOf(dbr.mBgNcbyCheckFail));
        values.put("mBG_NCByStateErr", Short.valueOf(dbr.mBgNcbyStateErr));
        values.put("mBG_NCByUnknown", Short.valueOf(dbr.mBgNcbyUnknown));
        values.put("mBQE_CNUrl1FailCount", Short.valueOf(dbr.mBqeCnUrl1FailCount));
        values.put("mBQE_CNUrl2FailCount", Short.valueOf(dbr.mBqeCnUrl2FailCount));
        values.put("mBQE_CNUrl3FailCount", Short.valueOf(dbr.mBqeCnUrl3FailCount));
        values.put("mBQE_NCNUrl1FailCount", Short.valueOf(dbr.mBqenCnUrl1FailCount));
        values.put("mBQE_NCNUrl2FailCount", Short.valueOf(dbr.mBqenCnUrl2FailCount));
        values.put("mBQE_NCNUrl3FailCount", Short.valueOf(dbr.mBqenCnUrl3FailCount));
        values.put("mBQE_ScoreUnknownCount", Short.valueOf(dbr.mBqeScoreUnknownCount));
        values.put("mBQE_BindWlanFailCount", Short.valueOf(dbr.mBqeBindWlanFailCount));
        values.put("mBQE_StopBqeFailCount", Short.valueOf(dbr.mBqeStopBqeFailCount));
        values.put("mQOE_AutoRI_TotData", Integer.valueOf(dbr.mQoeAutoRiTotData));
        values.put("mNotInet_AutoRI_TotData", Integer.valueOf(dbr.mNotInetAutoRiTotData));
        values.put("mQOE_RO_DISCONNECT_Cnt", Short.valueOf(dbr.mQoeRoDisconnectCnt));
        values.put("mQOE_RO_DISCONNECT_TotData", Integer.valueOf(dbr.mQoeRoDisconnectTotData));
        values.put("mNotInetRO_DISCONNECT_Cnt", Short.valueOf(dbr.mNotInetRoDisconnectCnt));
        values.put("mNotInetRO_DISCONNECT_TotData", Integer.valueOf(dbr.mNotInetRoDisconnectTotData));
        values.put("mTotWifiConnectTime", Integer.valueOf(dbr.mTotWifiConnectTime));
        values.put("mActiveCheckRS_Diff", Short.valueOf(dbr.mActiveCheckRsDiff));
        values.put("mNoInetAlarmOnConnCnt", Short.valueOf(dbr.mNoInetAlarmOnConnCnt));
        values.put("mPortalNoAutoConnCnt", Short.valueOf(dbr.mPortalNoAutoConnCnt));
        values.put("mHomeAPAddRoPeriodCnt", Short.valueOf(dbr.mHomeAPAddRoPeriodCnt));
        values.put("mHomeAPQoeBadCnt", Short.valueOf(dbr.mHomeAPQoeBadCnt));
        values.put("mHistoryTotWifiConnHour", Integer.valueOf(dbr.mHistoryTotWifiConnHour));
        values.put("mBigRTT_RO_Tot", Short.valueOf(dbr.mBigRttRoTot));
        values.put("mBigRTT_ErrRO_Tot", Short.valueOf(dbr.mBigRttErrRoTot));
        values.put("mTotalPortalConnCount", Short.valueOf(dbr.mTotalPortalConnCount));
        values.put("mTotalPortalAuthSuccCount", Short.valueOf(dbr.mTotalPortalAuthSuccCount));
        values.put("mManualConnBlockPortalCount", Short.valueOf(dbr.mManualConnBlockPortalCount));
        values.put("mWifiproOpenCount", Short.valueOf(dbr.mWifiproOpenCount));
        values.put("mWifiproCloseCount", Short.valueOf(dbr.mWifiproCloseCount));
        values.put("mActiveCheckRS_Same", Short.valueOf(dbr.mActiveCheckRsSame));
        int rowChg = 0;
        try {
            rowChg = this.mDatabase.update(WifiProChrDataBaseHelper.CHR_STAT_TABLE_NAME, values, "_id like ?", new String[]{"1"});
        } catch (SQLException e) {
            logE("update error:" + e);
        }
        if (rowChg == 0) {
            logE("updateChrStatRcd update failed.");
            return false;
        }
        logI("updateChrStatRcd update succ, rowChg=" + rowChg);
        return true;
    }

    private boolean insertChrStatRcd(WifiProStatisticsRecord dbr) {
        SQLiteDatabase sQLiteDatabase;
        logI("insertChrStatRcd enter.");
        synchronized (this.mChrLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (dbr == null) {
                        logE("insertChrStatRcd null error.");
                        return false;
                    }
                    this.mDatabase.beginTransaction();
                    try {
                        SQLiteDatabase sQLiteDatabase2 = this.mDatabase;
                        Object[] objArr = new Object[HwQoEUtils.QOE_MSG_CAMERA_ON];
                        objArr[0] = (short) 1;
                        objArr[1] = dbr.mLastStatUploadTime;
                        objArr[2] = Short.valueOf(dbr.mLastWifiproState);
                        objArr[3] = dbr.mLastWifiproStateUpdateTime;
                        objArr[4] = Integer.valueOf(dbr.mEnableTotTime);
                        objArr[5] = Short.valueOf(dbr.mNoInetHandoverCount);
                        objArr[6] = Short.valueOf(dbr.mPortalUnauthCount);
                        objArr[7] = Short.valueOf(dbr.mWifiScoCount);
                        objArr[8] = Short.valueOf(dbr.mPortalCodeParseCount);
                        objArr[9] = Short.valueOf(dbr.mRcvSmsCount);
                        objArr[10] = Short.valueOf(dbr.mPortalAutoLoginCount);
                        objArr[11] = Short.valueOf(dbr.mCellAutoOpenCount);
                        objArr[12] = Short.valueOf(dbr.mCellAutoCloseCount);
                        objArr[13] = Short.valueOf(dbr.mTotalBqeBadRoc);
                        objArr[14] = Short.valueOf(dbr.mManualBackROC);
                        objArr[15] = Short.valueOf(dbr.mRssiRoTot);
                        objArr[16] = Short.valueOf(dbr.mRssiErrRoTot);
                        objArr[17] = Short.valueOf(dbr.mOtaRoTot);
                        objArr[18] = Short.valueOf(dbr.mOtaErrRoTot);
                        objArr[19] = Short.valueOf(dbr.mTcpRoTot);
                        objArr[20] = Short.valueOf(dbr.mTcpErrRoTot);
                        objArr[21] = Integer.valueOf(dbr.mManualRiTotTime);
                        objArr[22] = Integer.valueOf(dbr.mAutoRiTotTime);
                        objArr[23] = Short.valueOf(dbr.mAutoRiTotCount);
                        objArr[24] = Short.valueOf(dbr.mRssiRestoreRiCount);
                        objArr[25] = Short.valueOf(dbr.mRssiBetterRiCount);
                        objArr[26] = Short.valueOf(dbr.mTimerRiCount);
                        objArr[27] = Short.valueOf(dbr.mHisScoRiCount);
                        objArr[28] = Short.valueOf(dbr.mUserCancelROC);
                        objArr[29] = Short.valueOf(dbr.mWifiToWifiSuccCount);
                        objArr[30] = Short.valueOf(dbr.mNoInetAlarmCount);
                        objArr[31] = Short.valueOf(dbr.mWifiOobInitState);
                        objArr[32] = Short.valueOf(dbr.mNotAutoConnPortalCnt);
                        objArr[33] = Short.valueOf(dbr.mHighDataRateStopROC);
                        objArr[34] = Short.valueOf(dbr.mSelectNotInetAPCount);
                        objArr[35] = Short.valueOf(dbr.mUserUseBgScanAPCount);
                        objArr[36] = Short.valueOf(dbr.mPingPongCount);
                        objArr[37] = Short.valueOf(dbr.mBqeBadSettingCancel);
                        objArr[38] = Short.valueOf(dbr.mNotInetSettingCancel);
                        objArr[39] = Short.valueOf(dbr.mNotInetUserCancel);
                        objArr[40] = Short.valueOf(dbr.mNotInetRestoreRI);
                        objArr[41] = Short.valueOf(dbr.mNotInetUserManualRI);
                        objArr[42] = Short.valueOf(dbr.mNotInetWifiToWifiCount);
                        objArr[43] = Short.valueOf(dbr.mReopenWifiRICount);
                        objArr[44] = Short.valueOf(dbr.mSelCSPShowDiglogCount);
                        objArr[45] = Short.valueOf(dbr.mSelCSPAutoSwCount);
                        objArr[46] = Short.valueOf(dbr.mSelCSPNotSwCount);
                        objArr[47] = Short.valueOf(dbr.mTotBtnRICount);
                        objArr[48] = Short.valueOf(dbr.mBmdTenmNotifyCount);
                        objArr[49] = Short.valueOf(dbr.mBmdTenmRiCount);
                        objArr[50] = Short.valueOf(dbr.mBmdFiftymNotifyCount);
                        objArr[51] = Short.valueOf(dbr.mBmdFiftymRiCount);
                        objArr[52] = Short.valueOf(dbr.mBmdUserDelNotifyCount);
                        objArr[53] = Integer.valueOf(dbr.mRoTotMobileData);
                        objArr[54] = Short.valueOf(dbr.mAfPhoneNumSuccCnt);
                        objArr[55] = Short.valueOf(dbr.mAfPhoneNumFailCnt);
                        objArr[56] = Short.valueOf(dbr.mAfPasswordSuccCnt);
                        objArr[57] = Short.valueOf(dbr.mAfPasswordFailCnt);
                        objArr[58] = Short.valueOf(dbr.mAfAutoLoginSuccCnt);
                        objArr[59] = Short.valueOf(dbr.mAfAutoLoginFailCnt);
                        objArr[60] = Short.valueOf(dbr.mBgBgRunCnt);
                        objArr[61] = Short.valueOf(dbr.mBgSettingRunCnt);
                        objArr[62] = Short.valueOf(dbr.mBgFreeInetOkApCnt);
                        objArr[63] = Short.valueOf(dbr.mBgFishingApCnt);
                        objArr[64] = Short.valueOf(dbr.mBgFreeNotInetApCnt);
                        objArr[65] = Short.valueOf(dbr.mBgPortalApCnt);
                        objArr[66] = Short.valueOf(dbr.mBgFailedCnt);
                        objArr[67] = Short.valueOf(dbr.mBgInetNotOkActiveOk);
                        objArr[68] = Short.valueOf(dbr.mBgInetOkActiveNotOk);
                        objArr[69] = Short.valueOf(dbr.mBgUserSelApFishingCnt);
                        objArr[70] = Short.valueOf(dbr.mBgConntTimeoutCnt);
                        objArr[71] = Short.valueOf(dbr.mBgDnsFailCnt);
                        objArr[72] = Short.valueOf(dbr.mBgDhcpFailCnt);
                        objArr[73] = Short.valueOf(dbr.mBgAuthFailCnt);
                        objArr[74] = Short.valueOf(dbr.mBgAssocRejectCnt);
                        objArr[75] = Short.valueOf(dbr.mBgUserSelFreeInetOkCnt);
                        objArr[76] = Short.valueOf(dbr.mBgUserSelNoInetCnt);
                        objArr[77] = Short.valueOf(dbr.mBgUserSelPortalCnt);
                        objArr[78] = Short.valueOf(dbr.mBgFoundTwoMoreApCnt);
                        objArr[79] = Short.valueOf(dbr.mAfFpnsSuccNotMsmCnt);
                        objArr[80] = Short.valueOf(dbr.mBsgRsGoodCnt);
                        objArr[81] = Short.valueOf(dbr.mBsgRsMidCnt);
                        objArr[82] = Short.valueOf(dbr.mBsgRsBadCnt);
                        objArr[83] = Short.valueOf(dbr.mBsgEndIn4sCnt);
                        objArr[84] = Short.valueOf(dbr.mBsgEndIn4s7sCnt);
                        objArr[85] = Short.valueOf(dbr.mBsgNotEndIn7sCnt);
                        objArr[86] = Short.valueOf(dbr.mBgNcbyConnectFail);
                        objArr[87] = Short.valueOf(dbr.mBgNcbyCheckFail);
                        objArr[88] = Short.valueOf(dbr.mBgNcbyStateErr);
                        objArr[89] = Short.valueOf(dbr.mBgNcbyUnknown);
                        objArr[90] = Short.valueOf(dbr.mBqeCnUrl1FailCount);
                        objArr[91] = Short.valueOf(dbr.mBqeCnUrl2FailCount);
                        objArr[92] = Short.valueOf(dbr.mBqeCnUrl3FailCount);
                        objArr[93] = Short.valueOf(dbr.mBqenCnUrl1FailCount);
                        objArr[94] = Short.valueOf(dbr.mBqenCnUrl2FailCount);
                        objArr[95] = Short.valueOf(dbr.mBqenCnUrl3FailCount);
                        objArr[96] = Short.valueOf(dbr.mBqeScoreUnknownCount);
                        objArr[97] = Short.valueOf(dbr.mBqeBindWlanFailCount);
                        objArr[98] = Short.valueOf(dbr.mBqeStopBqeFailCount);
                        objArr[99] = Integer.valueOf(dbr.mQoeAutoRiTotData);
                        objArr[100] = Integer.valueOf(dbr.mNotInetAutoRiTotData);
                        objArr[101] = Short.valueOf(dbr.mQoeRoDisconnectCnt);
                        objArr[102] = Integer.valueOf(dbr.mQoeRoDisconnectTotData);
                        objArr[103] = Short.valueOf(dbr.mNotInetRoDisconnectCnt);
                        objArr[104] = Integer.valueOf(dbr.mNotInetRoDisconnectTotData);
                        objArr[105] = Integer.valueOf(dbr.mTotWifiConnectTime);
                        objArr[106] = Short.valueOf(dbr.mActiveCheckRsDiff);
                        objArr[107] = Short.valueOf(dbr.mNoInetAlarmOnConnCnt);
                        objArr[108] = Short.valueOf(dbr.mPortalNoAutoConnCnt);
                        objArr[109] = Short.valueOf(dbr.mHomeAPAddRoPeriodCnt);
                        objArr[110] = Short.valueOf(dbr.mHomeAPQoeBadCnt);
                        objArr[111] = Integer.valueOf(dbr.mHistoryTotWifiConnHour);
                        objArr[112] = Short.valueOf(dbr.mBigRttRoTot);
                        objArr[113] = Short.valueOf(dbr.mBigRttErrRoTot);
                        objArr[114] = Short.valueOf(dbr.mTotalPortalConnCount);
                        objArr[115] = Short.valueOf(dbr.mTotalPortalAuthSuccCount);
                        objArr[116] = Short.valueOf(dbr.mManualConnBlockPortalCount);
                        objArr[117] = Short.valueOf(dbr.mWifiproOpenCount);
                        objArr[118] = Short.valueOf(dbr.mWifiproCloseCount);
                        objArr[119] = Short.valueOf(dbr.mActiveCheckRsSame);
                        sQLiteDatabase2.execSQL("INSERT INTO CHRStatTable VALUES(?,   ?, ?, ?, ?, ?,   ?, ?, ?, ?, ?, ?,   ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?,    ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?,   ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,     ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,    ?, ?, ?,    ?,?,?,?,?,?)", objArr);
                        this.mDatabase.setTransactionSuccessful();
                        logI("insertChrStatRcd update or add a record succ");
                        sQLiteDatabase = this.mDatabase;
                    } catch (SQLException e) {
                        logE("insertChrStatRcd error:" + e);
                        sQLiteDatabase = this.mDatabase;
                    } catch (Throwable th) {
                        this.mDatabase.endTransaction();
                        throw th;
                    }
                    sQLiteDatabase.endTransaction();
                    return true;
                }
            }
            logE("insertChrStatRcd database error.");
            return false;
        }
    }

    public boolean addOrUpdateChrStatRcd(WifiProStatisticsRecord dbr) {
        synchronized (this.mChrLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (dbr != null) {
                    if (checkIfRcdExist()) {
                        return updateChrStatRcd(dbr);
                    }
                    return insertChrStatRcd(dbr);
                }
            }
            logE("insertChrStatRcd error.");
            return false;
        }
    }

    public boolean queryChrStatRcd(WifiProStatisticsRecord dbr) {
        logD("queryChrStatRcd enter.");
        synchronized (this.mChrLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    if (dbr == null) {
                        logE("queryChrStatRcd null error.");
                        return false;
                    }
                    Cursor c = null;
                    try {
                        Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM CHRStatTable where _id like ?", new String[]{"1"});
                        int recCnt = 0;
                        while (true) {
                            if (!c2.moveToNext()) {
                                break;
                            }
                            recCnt++;
                            if (recCnt > 1) {
                                break;
                            }
                            dbr.mLastStatUploadTime = c2.getString(c2.getColumnIndex("mLastStatUploadTime"));
                            dbr.mLastWifiproState = c2.getShort(c2.getColumnIndex("mLastWifiproState"));
                            dbr.mLastWifiproStateUpdateTime = c2.getString(c2.getColumnIndex("mLastWifiproStateUpdateTime"));
                            dbr.mEnableTotTime = c2.getInt(c2.getColumnIndex("mEnableTotTime"));
                            dbr.mNoInetHandoverCount = c2.getShort(c2.getColumnIndex("mNoInetHandoverCount"));
                            dbr.mPortalUnauthCount = c2.getShort(c2.getColumnIndex("mPortalUnauthCount"));
                            dbr.mWifiScoCount = c2.getShort(c2.getColumnIndex("mWifiScoCount"));
                            dbr.mPortalCodeParseCount = c2.getShort(c2.getColumnIndex("mPortalCodeParseCount"));
                            dbr.mRcvSmsCount = c2.getShort(c2.getColumnIndex("mRcvSMS_Count"));
                            dbr.mPortalAutoLoginCount = c2.getShort(c2.getColumnIndex("mPortalAutoLoginCount"));
                            dbr.mCellAutoOpenCount = c2.getShort(c2.getColumnIndex("mCellAutoOpenCount"));
                            dbr.mCellAutoCloseCount = c2.getShort(c2.getColumnIndex("mCellAutoCloseCount"));
                            dbr.mTotalBqeBadRoc = c2.getShort(c2.getColumnIndex("mTotalBQE_BadROC"));
                            dbr.mManualBackROC = c2.getShort(c2.getColumnIndex("mManualBackROC"));
                            dbr.mRssiRoTot = c2.getShort(c2.getColumnIndex("mRSSI_RO_Tot"));
                            dbr.mRssiErrRoTot = c2.getShort(c2.getColumnIndex("mRSSI_ErrRO_Tot"));
                            dbr.mOtaRoTot = c2.getShort(c2.getColumnIndex("mOTA_RO_Tot"));
                            dbr.mOtaErrRoTot = c2.getShort(c2.getColumnIndex("mOTA_ErrRO_Tot"));
                            dbr.mTcpRoTot = c2.getShort(c2.getColumnIndex("mTCP_RO_Tot"));
                            dbr.mTcpErrRoTot = c2.getShort(c2.getColumnIndex("mTCP_ErrRO_Tot"));
                            dbr.mManualRiTotTime = c2.getInt(c2.getColumnIndex("mManualRI_TotTime"));
                            dbr.mAutoRiTotTime = c2.getInt(c2.getColumnIndex("mAutoRI_TotTime"));
                            dbr.mAutoRiTotCount = c2.getShort(c2.getColumnIndex("mAutoRI_TotCount"));
                            dbr.mRssiRestoreRiCount = c2.getShort(c2.getColumnIndex("mRSSI_RestoreRI_Count"));
                            dbr.mRssiBetterRiCount = c2.getShort(c2.getColumnIndex("mRSSI_BetterRI_Count"));
                            dbr.mTimerRiCount = c2.getShort(c2.getColumnIndex("mTimerRI_Count"));
                            dbr.mHisScoRiCount = c2.getShort(c2.getColumnIndex("mHisScoRI_Count"));
                            dbr.mUserCancelROC = c2.getShort(c2.getColumnIndex("mUserCancelROC"));
                            dbr.mWifiToWifiSuccCount = c2.getShort(c2.getColumnIndex("mWifiToWifiSuccCount"));
                            dbr.mNoInetAlarmCount = c2.getShort(c2.getColumnIndex("mNoInetAlarmCount"));
                            dbr.mWifiOobInitState = c2.getShort(c2.getColumnIndex("mWifiOobInitState"));
                            dbr.mNotAutoConnPortalCnt = c2.getShort(c2.getColumnIndex("mNotAutoConnPortalCnt"));
                            dbr.mHighDataRateStopROC = c2.getShort(c2.getColumnIndex("mHighDataRateStopROC"));
                            dbr.mSelectNotInetAPCount = c2.getShort(c2.getColumnIndex("mSelectNotInetAPCount"));
                            dbr.mUserUseBgScanAPCount = c2.getShort(c2.getColumnIndex("mUserUseBgScanAPCount"));
                            dbr.mPingPongCount = c2.getShort(c2.getColumnIndex("mPingPongCount"));
                            dbr.mBqeBadSettingCancel = c2.getShort(c2.getColumnIndex("mBQE_BadSettingCancel"));
                            dbr.mNotInetSettingCancel = c2.getShort(c2.getColumnIndex("mNotInetSettingCancel"));
                            dbr.mNotInetUserCancel = c2.getShort(c2.getColumnIndex("mNotInetUserCancel"));
                            dbr.mNotInetRestoreRI = c2.getShort(c2.getColumnIndex("mNotInetRestoreRI"));
                            dbr.mNotInetUserManualRI = c2.getShort(c2.getColumnIndex("mNotInetUserManualRI"));
                            dbr.mNotInetWifiToWifiCount = c2.getShort(c2.getColumnIndex("mNotInetWifiToWifiCount"));
                            dbr.mReopenWifiRICount = c2.getShort(c2.getColumnIndex("mReopenWifiRICount"));
                            dbr.mSelCSPShowDiglogCount = c2.getShort(c2.getColumnIndex("mSelCSPShowDiglogCount"));
                            dbr.mSelCSPAutoSwCount = c2.getShort(c2.getColumnIndex("mSelCSPAutoSwCount"));
                            dbr.mSelCSPNotSwCount = c2.getShort(c2.getColumnIndex("mSelCSPNotSwCount"));
                            dbr.mTotBtnRICount = c2.getShort(c2.getColumnIndex("mTotBtnRICount"));
                            dbr.mBmdTenmNotifyCount = c2.getShort(c2.getColumnIndex("mBMD_TenMNotifyCount"));
                            dbr.mBmdTenmRiCount = c2.getShort(c2.getColumnIndex("mBMD_TenM_RI_Count"));
                            dbr.mBmdFiftymNotifyCount = c2.getShort(c2.getColumnIndex("mBMD_FiftyMNotifyCount"));
                            dbr.mBmdFiftymRiCount = c2.getShort(c2.getColumnIndex("mBMD_FiftyM_RI_Count"));
                            dbr.mBmdUserDelNotifyCount = c2.getShort(c2.getColumnIndex("mBMD_UserDelNotifyCount"));
                            dbr.mRoTotMobileData = c2.getInt(c2.getColumnIndex("mRO_TotMobileData"));
                            dbr.mAfPhoneNumSuccCnt = c2.getShort(c2.getColumnIndex("mAF_PhoneNumSuccCnt"));
                            dbr.mAfPhoneNumFailCnt = c2.getShort(c2.getColumnIndex("mAF_PhoneNumFailCnt"));
                            dbr.mAfPasswordSuccCnt = c2.getShort(c2.getColumnIndex("mAF_PasswordSuccCnt"));
                            dbr.mAfPasswordFailCnt = c2.getShort(c2.getColumnIndex("mAF_PasswordFailCnt"));
                            dbr.mAfAutoLoginSuccCnt = c2.getShort(c2.getColumnIndex("mAF_AutoLoginSuccCnt"));
                            dbr.mAfAutoLoginFailCnt = c2.getShort(c2.getColumnIndex("mAF_AutoLoginFailCnt"));
                            dbr.mBgBgRunCnt = c2.getShort(c2.getColumnIndex("mBG_BgRunCnt"));
                            dbr.mBgSettingRunCnt = c2.getShort(c2.getColumnIndex("mBG_SettingRunCnt"));
                            dbr.mBgFreeInetOkApCnt = c2.getShort(c2.getColumnIndex("mBG_FreeInetOkApCnt"));
                            dbr.mBgFishingApCnt = c2.getShort(c2.getColumnIndex("mBG_FishingApCnt"));
                            dbr.mBgFreeNotInetApCnt = c2.getShort(c2.getColumnIndex("mBG_FreeNotInetApCnt"));
                            dbr.mBgPortalApCnt = c2.getShort(c2.getColumnIndex("mBG_PortalApCnt"));
                            dbr.mBgFailedCnt = c2.getShort(c2.getColumnIndex("mBG_FailedCnt"));
                            dbr.mBgInetNotOkActiveOk = c2.getShort(c2.getColumnIndex("mBG_InetNotOkActiveOk"));
                            dbr.mBgInetOkActiveNotOk = c2.getShort(c2.getColumnIndex("mBG_InetOkActiveNotOk"));
                            dbr.mBgUserSelApFishingCnt = c2.getShort(c2.getColumnIndex("mBG_UserSelApFishingCnt"));
                            dbr.mBgConntTimeoutCnt = c2.getShort(c2.getColumnIndex("mBG_ConntTimeoutCnt"));
                            dbr.mBgDnsFailCnt = c2.getShort(c2.getColumnIndex("mBG_DNSFailCnt"));
                            dbr.mBgDhcpFailCnt = c2.getShort(c2.getColumnIndex("mBG_DHCPFailCnt"));
                            dbr.mBgAuthFailCnt = c2.getShort(c2.getColumnIndex("mBG_AUTH_FailCnt"));
                            dbr.mBgAssocRejectCnt = c2.getShort(c2.getColumnIndex("mBG_AssocRejectCnt"));
                            dbr.mBgUserSelFreeInetOkCnt = c2.getShort(c2.getColumnIndex("mBG_UserSelFreeInetOkCnt"));
                            dbr.mBgUserSelNoInetCnt = c2.getShort(c2.getColumnIndex("mBG_UserSelNoInetCnt"));
                            dbr.mBgUserSelPortalCnt = c2.getShort(c2.getColumnIndex("mBG_UserSelPortalCnt"));
                            dbr.mBgFoundTwoMoreApCnt = c2.getShort(c2.getColumnIndex("mBG_FoundTwoMoreApCnt"));
                            dbr.mAfFpnsSuccNotMsmCnt = c2.getShort(c2.getColumnIndex("mAF_FPNSuccNotMsmCnt"));
                            dbr.mBsgRsGoodCnt = c2.getShort(c2.getColumnIndex("mBSG_RsGoodCnt"));
                            dbr.mBsgRsMidCnt = c2.getShort(c2.getColumnIndex("mBSG_RsMidCnt"));
                            dbr.mBsgRsBadCnt = c2.getShort(c2.getColumnIndex("mBSG_RsBadCnt"));
                            dbr.mBsgEndIn4sCnt = c2.getShort(c2.getColumnIndex("mBSG_EndIn4sCnt"));
                            dbr.mBsgEndIn4s7sCnt = c2.getShort(c2.getColumnIndex("mBSG_EndIn4s7sCnt"));
                            dbr.mBsgNotEndIn7sCnt = c2.getShort(c2.getColumnIndex("mBSG_NotEndIn7sCnt"));
                            dbr.mBgNcbyConnectFail = c2.getShort(c2.getColumnIndex("mBG_NCByConnectFail"));
                            dbr.mBgNcbyCheckFail = c2.getShort(c2.getColumnIndex("mBG_NCByCheckFail"));
                            dbr.mBgNcbyStateErr = c2.getShort(c2.getColumnIndex("mBG_NCByStateErr"));
                            dbr.mBgNcbyUnknown = c2.getShort(c2.getColumnIndex("mBG_NCByUnknown"));
                            dbr.mBqeCnUrl1FailCount = c2.getShort(c2.getColumnIndex("mBQE_CNUrl1FailCount"));
                            dbr.mBqeCnUrl2FailCount = c2.getShort(c2.getColumnIndex("mBQE_CNUrl2FailCount"));
                            dbr.mBqeCnUrl3FailCount = c2.getShort(c2.getColumnIndex("mBQE_CNUrl3FailCount"));
                            dbr.mBqenCnUrl1FailCount = c2.getShort(c2.getColumnIndex("mBQE_NCNUrl1FailCount"));
                            dbr.mBqenCnUrl2FailCount = c2.getShort(c2.getColumnIndex("mBQE_NCNUrl2FailCount"));
                            dbr.mBqenCnUrl3FailCount = c2.getShort(c2.getColumnIndex("mBQE_NCNUrl3FailCount"));
                            dbr.mBqeScoreUnknownCount = c2.getShort(c2.getColumnIndex("mBQE_ScoreUnknownCount"));
                            dbr.mBqeBindWlanFailCount = c2.getShort(c2.getColumnIndex("mBQE_BindWlanFailCount"));
                            dbr.mBqeStopBqeFailCount = c2.getShort(c2.getColumnIndex("mBQE_StopBqeFailCount"));
                            dbr.mQoeAutoRiTotData = c2.getInt(c2.getColumnIndex("mQOE_AutoRI_TotData"));
                            dbr.mNotInetAutoRiTotData = c2.getInt(c2.getColumnIndex("mNotInet_AutoRI_TotData"));
                            dbr.mQoeRoDisconnectCnt = c2.getShort(c2.getColumnIndex("mQOE_RO_DISCONNECT_Cnt"));
                            dbr.mQoeRoDisconnectTotData = c2.getInt(c2.getColumnIndex("mQOE_RO_DISCONNECT_TotData"));
                            dbr.mNotInetRoDisconnectCnt = c2.getShort(c2.getColumnIndex("mNotInetRO_DISCONNECT_Cnt"));
                            dbr.mNotInetRoDisconnectTotData = c2.getInt(c2.getColumnIndex("mNotInetRO_DISCONNECT_TotData"));
                            dbr.mTotWifiConnectTime = c2.getInt(c2.getColumnIndex("mTotWifiConnectTime"));
                            dbr.mActiveCheckRsDiff = c2.getShort(c2.getColumnIndex("mActiveCheckRS_Diff"));
                            dbr.mNoInetAlarmOnConnCnt = c2.getShort(c2.getColumnIndex("mNoInetAlarmOnConnCnt"));
                            dbr.mPortalNoAutoConnCnt = c2.getShort(c2.getColumnIndex("mPortalNoAutoConnCnt"));
                            dbr.mHomeAPAddRoPeriodCnt = c2.getShort(c2.getColumnIndex("mHomeAPAddRoPeriodCnt"));
                            dbr.mHomeAPQoeBadCnt = c2.getShort(c2.getColumnIndex("mHomeAPQoeBadCnt"));
                            dbr.mHistoryTotWifiConnHour = c2.getInt(c2.getColumnIndex("mHistoryTotWifiConnHour"));
                            dbr.mBigRttRoTot = c2.getShort(c2.getColumnIndex("mBigRTT_RO_Tot"));
                            dbr.mBigRttErrRoTot = c2.getShort(c2.getColumnIndex("mBigRTT_ErrRO_Tot"));
                            dbr.mTotalPortalConnCount = c2.getShort(c2.getColumnIndex("mTotalPortalConnCount"));
                            dbr.mTotalPortalAuthSuccCount = c2.getShort(c2.getColumnIndex("mTotalPortalAuthSuccCount"));
                            dbr.mManualConnBlockPortalCount = c2.getShort(c2.getColumnIndex("mManualConnBlockPortalCount"));
                            dbr.mWifiproOpenCount = c2.getShort(c2.getColumnIndex("mWifiproOpenCount"));
                            dbr.mWifiproCloseCount = c2.getShort(c2.getColumnIndex("mWifiproCloseCount"));
                            dbr.mActiveCheckRsSame = c2.getShort(c2.getColumnIndex("mActiveCheckRS_Same"));
                            logI("read record succ, LastStatUploadTime:" + dbr.mLastStatUploadTime);
                        }
                        if (recCnt > 1) {
                            logE("more than one record error. use first record.");
                        } else if (recCnt == 0) {
                            logI("queryChrStatRcd not CHR statistics record.");
                        }
                        c2.close();
                        return true;
                    } catch (SQLException e) {
                        logE("queryChrStatRcd error:" + e);
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
            logE("queryChrStatRcd database error.");
            return false;
        }
    }

    private void logD(String msg) {
        if (printLogLevel <= 1) {
            Log.d(TAG, msg);
        }
    }

    private void logI(String msg) {
        if (printLogLevel <= 2) {
            Log.i(TAG, msg);
        }
    }

    private void logE(String msg) {
        if (printLogLevel <= 3) {
            Log.e(TAG, msg);
        }
    }
}
