package com.huawei.hwwifiproservice;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WifiProStatisticsManager {
    public static final int AC_FAIL_TYPE_REFUSE = 1;
    public static final int AC_FAIL_TYPE_RESET = 2;
    public static final int AC_FAIL_TYPE_UNKNOWN = 0;
    public static final int AF_FAILURE = 0;
    public static final int AF_SUCCESS = 1;
    public static final int AF_TYPE_AUTO_LOGIN = 2;
    public static final int AF_TYPE_FILL_PASSWORD = 1;
    public static final int AF_TYPE_FILL_PHONE_NUM = 0;
    public static final int AF_TYPE_FPN_SUCC_NOT_MSM = 3;
    private static final long BETA_USER_UPLOAD_PERIOD = 86400000;
    public static final int BG_CHR_TYPE_ASSOC_REJECT_CNT = 19;
    public static final int BG_CHR_TYPE_AUTH_FAIL_CNT = 18;
    public static final int BG_CHR_TYPE_BACK_GROUND_RUN_CNT = 1;
    public static final int BG_CHR_TYPE_BG_INET_OK_ACTIVE_NOT_OK = 13;
    public static final int BG_CHR_TYPE_BG_NOT_INET_ACTIVE_IOK = 12;
    public static final int BG_CHR_TYPE_CONNT_TIMEOUT_CNT = 15;
    public static final int BG_CHR_TYPE_DHCP_FAIL_CNT = 17;
    public static final int BG_CHR_TYPE_DNS_FAIL_CNT = 16;
    public static final int BG_CHR_TYPE_FAILED_CNT = 11;
    public static final int BG_CHR_TYPE_FISHING_AP_CNT = 4;
    public static final int BG_CHR_TYPE_FOUND_TWO_MORE_AP_CNT = 10;
    public static final int BG_CHR_TYPE_FREE_INET_OK_AP_CNT = 3;
    public static final int BG_CHR_TYPE_FREE_NOT_INET_AP_CNT = 5;
    public static final int BG_CHR_TYPE_NC_BY_CHECK_FAIL = 21;
    public static final int BG_CHR_TYPE_NC_BY_CONNECT_FAIL = 20;
    public static final int BG_CHR_TYPE_NC_BY_STATE_ERR = 22;
    public static final int BG_CHR_TYPE_NC_BY_UNKNOWN = 23;
    public static final int BG_CHR_TYPE_PORTAL_AP_CNT = 6;
    public static final int BG_CHR_TYPE_SETTING_RUN_CNT = 2;
    public static final int BG_CHR_TYPE_USER_SEL_AP_FISHING_CNT = 14;
    public static final int BG_CHR_TYPE_USER_SEL_FREE_IOK_CNT = 7;
    public static final int BG_CHR_TYPE_USER_SEL_NOT_INET_CNT = 8;
    public static final int BG_CHR_TYPE_USER_SEL_PORTAL_CNT = 9;
    public static final int BQE_BAD_W2W_HANDOVER = 0;
    public static final int BQE_RI_REASON_RSSI_RESTORE = 1;
    public static final int BQE_RI_REASON_RSSI_RISE_15DB = 2;
    public static final int BQE_RI_REASON_TIMER_TIMEOUT = 3;
    public static final int BQE_RI_REASON_UNKNOWN = 0;
    public static final int BSG_CHR_TYPE_RS_BAD_CNT = 3;
    public static final int BSG_CHR_TYPE_RS_GOOD_CNT = 1;
    public static final int BSG_CHR_TYPE_RS_MID_CNT = 2;
    private static final int CHECK_UPLOAD_TIME_INTERVAL = 1800000;
    public static final int CHR_TYPE_BG_AV_CN_NAV = 1;
    public static final int CHR_TYPE_BG_AV_CN_POT = 2;
    public static final int CHR_TYPE_BG_CGT_CN_AV = 7;
    public static final int CHR_TYPE_BG_CGT_CN_NAV = 8;
    public static final int CHR_TYPE_BG_CGT_CN_POT = 9;
    public static final int CHR_TYPE_BG_CN_UNKNOWN = 0;
    public static final int CHR_TYPE_BG_NAV_CN_AV = 3;
    public static final int CHR_TYPE_BG_NAV_CN_POT = 4;
    public static final int CHR_TYPE_BG_POT_CN_AV = 5;
    public static final int CHR_TYPE_BG_POT_CN_NAV = 6;
    private static final int CLICK_CANCEL_RI_DEFAULT_TIME = 1;
    private static final long COMMERCIAL_USER_UPLOAD_PERIOD = 172800000;
    private static final int DBG_LOG_LEVEL = 1;
    private static final int DEBUG_MODE_CHECK_UPLOAD_TIME_INTERVAL = 60000;
    private static final long DEBUG_MODE_UPLOAD_PERIOD = 480000;
    private static final int ERROR_LOG_LEVEL = 3;
    private static final int ERROR_TIME_INTERVAL = -1;
    public static final int FALSE_INT_VAL = 0;
    public static final int HMD_10_NOTIFY_TYPE = 1;
    public static final int HMD_50_NOTIFY_TYPE = 2;
    private static final int HMD_DATA_SIZE_10000KB = 10240;
    private static final int HMD_DATA_SIZE_50000KB = 51200;
    private static final int INFO_LOG_LEVEL = 2;
    private static final short INVALID_MOBILE_SIGNAL_LEVEL = -1;
    private static final int INVALID_TIME_VALUE = 0;
    private static final long MAX_BETA_USER_UPLOAD_PERIOD = 432000000;
    private static final long MAX_COMMERCIAL_USER_UPLOAD_PERIOD = 1296000000;
    private static final int MAX_INT_TYPE_VALUE = 2147483632;
    private static final int MAX_SHORT_TYPE_VALUE = 32760;
    private static final int MILLISECONDS_OF_ONE_MINUTE = 60000;
    private static final int MILLISECONDS_OF_ONE_SECOND = 1000;
    public static final int NOT_INET_W2W_HANDOVER = 1;
    public static final int OOBE_WIFIPRO_DISABLE = 2;
    public static final int OOBE_WIFIPRO_ENABLE = 1;
    public static final int OOBE_WIFIPRO_UNKNOWN = 0;
    public static final int RI_REASON_DATA_SERVICE_CLOSE = 3;
    public static final int RI_REASON_DATA_SERVICE_POOR_QUILITY = 7;
    public static final int RI_REASON_HISTORY_RECORD_RI = 6;
    public static final int RI_REASON_HMD_10M_USER_BT_RI = 8;
    public static final int RI_REASON_HMD_50M_USER_BT_RI = 9;
    public static final int RI_REASON_UNKNOWN = 0;
    public static final int RI_REASON_USER_CLICK_CANCEL_BUTTON = 2;
    public static final int RI_REASON_USER_CLOSE_WIFIPRO = 5;
    public static final int RI_REASON_USER_MANUAL_RI_BOTTON = 4;
    public static final int RI_REASON_WIFI_BQE_REPORT_GOOD = 1;
    private static final int ROVE_OUT_MOBILE_DATA_NOTIFY_FIFTY_M = 2;
    private static final int ROVE_OUT_MOBILE_DATA_NOTIFY_TEM_M = 1;
    private static final int ROVE_OUT_MOBILE_DATA_NOTIFY_UNKNOWN = 0;
    private static final int RO_REASON_BIG_RTT = 8;
    public static final int RO_REASON_BQE_REPORT_BAD = 1;
    public static final int RO_REASON_NOT_INET_CAPABILITY = 2;
    private static final int RO_REASON_OTA_TCP_BAD = 2;
    private static final int RO_REASON_RSSI_TCP_BAD = 1;
    private static final int RO_REASON_TCP_BAD = 4;
    public static final int RO_REASON_UNKNOWN = 0;
    private static final int SECONDS_OF_ONE_HOUR = 3600;
    private static final int SECONDS_OF_ONE_MINUTE = 60;
    private static final int STAT_MSG_ACTIVE_CHECK_RS_DIFF = 145;
    private static final int STAT_MSG_ADD_TOTAL_BQE_ROC = 112;
    private static final int STAT_MSG_AF_CHR_UPDATE = 135;
    private static final int STAT_MSG_BACK_GRADING_CHR_UPDATE = 136;
    private static final int STAT_MSG_BQE_BAD_RO_DISCONNECT_MOBILE_DATA = 138;
    private static final int STAT_MSG_BQE_BAD_SETTING_CANCEL = 125;
    private static final int STAT_MSG_BQE_GRADING_SVC_CHR_UPDATE = 137;
    private static final int STAT_MSG_BQE_RO_PARA_UPDATE = 115;
    private static final int STAT_MSG_CELL_AUTO_CLOSE_COUNT = 111;
    private static final int STAT_MSG_CELL_AUTO_OPEN_COUNT = 110;
    private static final int STAT_MSG_CHECK_NEED_UPLOAD_EVENT = 101;
    private static final int STAT_MSG_HIGH_DATA_RATE_STOP_ROC = 123;
    private static final int STAT_MSG_HISTORY_SCORE_RI_COUNT = 116;
    private static final int STAT_MSG_HMD_NOTIFY_CNT = 133;
    private static final int STAT_MSG_HMD_USER_DEL_NOTIFY_CNT = 134;
    private static final int STAT_MSG_HOME_AP_ADD_RO_PERIOD_CNT = 142;
    private static final int STAT_MSG_INCREASE_AC_RS_SAME_COUNT = 149;
    private static final int STAT_MSG_INCREASE_CONN_BLOCK_PORTAL_COUNT = 148;
    private static final int STAT_MSG_INCREASE_HMD_BTN_RI_COUNT = 150;
    private static final int STAT_MSG_INCREASE_PORTAL_AUTH_SUCC_COUNT = 147;
    private static final int STAT_MSG_INCREASE_PORTAL_CONN_COUNT = 146;
    private static final int STAT_MSG_LOAD_DB_RECORD = 100;
    private static final int STAT_MSG_NOT_AUTO_CONN_PORTAL_COUNT = 122;
    private static final int STAT_MSG_NOT_INET_ALARM_COUNT = 118;
    private static final int STAT_MSG_NOT_INET_NET_RESTORE_RI = 128;
    private static final int STAT_MSG_NOT_INET_RO_DISCONNECT_MOBILE_DATA = 139;
    private static final int STAT_MSG_NOT_INET_SETTING_CANCEL = 126;
    private static final int STAT_MSG_NOT_INET_USER_CANCEL = 127;
    private static final int STAT_MSG_NOT_INET_USER_MANUAL_RI = 129;
    private static final int STAT_MSG_NO_INET_HANDOVER_COUNT = 104;
    private static final int STAT_MSG_OOB_INIT_STATE = 117;
    private static final int STAT_MSG_PING_PONG_COUNT = 124;
    private static final int STAT_MSG_PORTAL_AP_NOT_AUTO_CONN = 141;
    private static final int STAT_MSG_PORTAL_AUTO_LOGIN_COUNT = 109;
    private static final int STAT_MSG_PORTAL_CODE_PARSE_COUNT = 107;
    private static final int STAT_MSG_PORTAL_UNAUTH_COUNT = 105;
    private static final int STAT_MSG_RCVSMS_COUNT = 108;
    private static final int STAT_MSG_RI_EVENT = 114;
    private static final int STAT_MSG_RO_EVENT = 113;
    private static final int STAT_MSG_SCREEN_ON = 130;
    private static final int STAT_MSG_SELECT_NOT_INET_AP_COUNT = 121;
    private static final int STAT_MSG_SEL_CSP_SETTING_CHG_CNT = 132;
    private static final int STAT_MSG_UPDATE_SATTISTIC_TO_DB = 144;
    private static final int STAT_MSG_UPDATE_WIFI_CONNECTION_STATE = 140;
    private static final int STAT_MSG_USER_REOPEN_WIFI_RI_CNT = 131;
    private static final int STAT_MSG_USER_USE_BG_SCAN_COUNT = 119;
    private static final int STAT_MSG_WIFIPRO_STATE_CHG = 103;
    private static final int STAT_MSG_WIFI_SCO_COUNT = 106;
    private static final int STAT_MSG_WIFI_TO_WIFI_SUCC_COUNT = 120;
    private static final String TAG = "WifiProStatisticsManager";
    public static final int TRUE_INT_VAL = 1;
    private static final short UNKNOWN_RAT_TYPE = 0;
    private static final int VALUE_WIFI_TO_PDP_ALWAYS_SHOW_DIALOG = 0;
    private static final int VALUE_WIFI_TO_PDP_AUTO_HANDOVER_MOBILE = 1;
    private static final int VALUE_WIFI_TO_PDP_CANNOT_HANDOVER_MOBILE = 2;
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    private static boolean debugMode = false;
    private static WifiProStatisticsManager mStatisticsManager;
    private static int printLogLevel = 1;
    /* access modifiers changed from: private */
    public int mBQEROReasonFlag = 0;
    private int mBQERoveInReason = 0;
    private String mBgApSsid;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public WifiProChrDataBaseManager mDataBaseManager;
    private IGetApRecordCount mGetApRecordCountCallBack = null;
    /* access modifiers changed from: private */
    public IGetMobileInfoCallBack mGetMobileInfoCallBack = null;
    /* access modifiers changed from: private */
    public boolean mNeedSaveCHRStatistic = false;
    /* access modifiers changed from: private */
    public WifiProStatisticsRecord mNewestStatRcd;
    /* access modifiers changed from: private */
    public boolean mQoeOrNotInetRoveOutStarted = false;
    /* access modifiers changed from: private */
    public int mROReason = 0;
    /* access modifiers changed from: private */
    public long mROTime = 0;
    /* access modifiers changed from: private */
    public WifiProRoveOutParaRecord mRoveOutPara = null;
    /* access modifiers changed from: private */
    public int mRoveoutMobileDataNotifyType = 0;
    /* access modifiers changed from: private */
    public SimpleDateFormat mSimpleDateFmt;
    private Handler mStatHandler;
    /* access modifiers changed from: private */
    public long mWifiConnectStartTime = 0;

    private WifiProStatisticsManager(Context context, Looper looper) {
        logD("WifiProStatisticsManager enter.");
        this.mContext = context;
        initStatHandler(looper);
        this.mNewestStatRcd = new WifiProStatisticsRecord();
        this.mSimpleDateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.mStatHandler.sendEmptyMessage(100);
    }

    public static void initStatisticsManager(Context context, Looper looper) {
        if (mStatisticsManager == null) {
            mStatisticsManager = new WifiProStatisticsManager(context, looper);
        }
    }

    public static WifiProStatisticsManager getInstance() {
        if (mStatisticsManager == null) {
            mStatisticsManager = new WifiProStatisticsManager(null, null);
        }
        return mStatisticsManager;
    }

    private boolean checkDateValid(String dateStr) {
        Date objDate = null;
        SimpleDateFormat simpleDateFormat = this.mSimpleDateFmt;
        if (simpleDateFormat == null || dateStr == null) {
            return false;
        }
        try {
            objDate = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            logE("checkDateValid date string invalid:" + dateStr);
        }
        if (objDate == null) {
            return false;
        }
        logI("date string valid: " + dateStr);
        return true;
    }

    /* access modifiers changed from: private */
    public void loadStatDBRecord(WifiProStatisticsRecord statRecord) {
        WifiProChrDataBaseManager wifiProChrDataBaseManager;
        if (statRecord == null || (wifiProChrDataBaseManager = this.mDataBaseManager) == null || this.mStatHandler == null) {
            logE("loadStatDBRecord null error.");
        } else if (wifiProChrDataBaseManager.queryChrStatRcd(statRecord) && this.mNewestStatRcd.mLastStatUploadTime.equals("DEAULT_STR")) {
            logI("Not record in database now.");
            resetStatRecord(statRecord, "new phone, save first record.");
        } else if (!checkDateValid(statRecord.mLastStatUploadTime) || !checkDateValid(statRecord.mLastWifiproStateUpdateTime)) {
            resetStatRecord(statRecord, "date invalid, save new record.");
        } else {
            logI("get record from database success.");
        }
    }

    /* access modifiers changed from: private */
    public long calcTimeInterval(String oldTimeStr, Date nowDate) {
        Date oldDate = null;
        SimpleDateFormat simpleDateFormat = this.mSimpleDateFmt;
        if (simpleDateFormat == null || oldTimeStr == null || nowDate == null) {
            return -1;
        }
        try {
            oldDate = simpleDateFormat.parse(oldTimeStr);
        } catch (ParseException ex) {
            logE("There has exception in Date parse" + ex);
        }
        if (oldDate == null) {
            return -1;
        }
        return nowDate.getTime() - oldDate.getTime();
    }

    /* access modifiers changed from: private */
    public boolean checkIfNeedUpload(WifiProStatisticsRecord statRecord) {
        if (statRecord == null || this.mDataBaseManager == null) {
            logE("checkIfNeedUpload null error.");
            return false;
        }
        Date currDate = new Date();
        long statIntervalMinutes = calcTimeInterval(statRecord.mLastStatUploadTime, currDate);
        if (statIntervalMinutes <= -1) {
            resetStatRecord(statRecord, "checkIfNeedUpload LastStatUploadTime time record invalid");
            return false;
        }
        long intervalTime = COMMERCIAL_USER_UPLOAD_PERIOD;
        if (debugMode) {
            intervalTime = DEBUG_MODE_UPLOAD_PERIOD;
        }
        if (statIntervalMinutes >= MAX_COMMERCIAL_USER_UPLOAD_PERIOD) {
            logI("checkIfNeedUpload too big upload interval , reset start time.");
            statRecord.mLastStatUploadTime = this.mSimpleDateFmt.format(currDate);
            this.mDataBaseManager.addOrUpdateChrStatRcd(statRecord);
        }
        if (statIntervalMinutes >= intervalTime) {
            logI("checkIfNeedUpload ret true.");
            return true;
        }
        logI("checkIfNeedUpload time left(mins):" + ((intervalTime - statIntervalMinutes) / 60000));
        return false;
    }

    /* access modifiers changed from: private */
    public void resetStatRecord(WifiProStatisticsRecord statRecord, String reasonStr) {
        logI("ChrStatLog resetStatRecord enter, reason:" + reasonStr);
        if (statRecord == null || this.mDataBaseManager == null) {
            logE("resetStatRecord null error.");
            return;
        }
        String currDateStr = this.mSimpleDateFmt.format(new Date());
        statRecord.resetRecord();
        statRecord.mLastStatUploadTime = currDateStr;
        statRecord.mLastWifiproState = getWifiproState();
        statRecord.mLastWifiproStateUpdateTime = currDateStr;
        this.mDataBaseManager.addOrUpdateChrStatRcd(statRecord);
    }

    /* access modifiers changed from: private */
    public boolean uploadStatisticsCHREvent(WifiProStatisticsRecord statRecord) {
        logD("uploadStatisticsCHREvent enter.");
        if (statRecord == null || this.mDataBaseManager == null || this.mSimpleDateFmt == null) {
            logE("uploadStatisticsCHREvent null error.");
            return false;
        } else if (this.mNewestStatRcd.mLastStatUploadTime == null || this.mNewestStatRcd.mLastWifiproStateUpdateTime == null) {
            logE("last upload time error, give up upload.");
            resetStatRecord(statRecord, "time record null");
            return false;
        } else {
            Date currDate = new Date();
            String currDateStr = this.mSimpleDateFmt.format(currDate);
            long statIntervalMinutes = calcTimeInterval(this.mNewestStatRcd.mLastStatUploadTime, currDate);
            if (statIntervalMinutes <= -1) {
                resetStatRecord(statRecord, "LastStatUploadTime time record invalid");
                return false;
            }
            long statIntervalMinutes2 = statIntervalMinutes / 60000;
            if (statRecord.mLastWifiproState == 1) {
                long enableTotTime = calcTimeInterval(this.mNewestStatRcd.mLastWifiproStateUpdateTime, currDate);
                if (enableTotTime <= -1) {
                    resetStatRecord(statRecord, "LastWifiproStateUpdateTime time record invalid");
                    return false;
                }
                long enableTotTime2 = enableTotTime / 1000;
                logI("uploadStatisticsCHREvent currDateStr:" + currDateStr + ", last en minutes:" + (enableTotTime2 / 60));
                statRecord.mEnableTotTime = (int) (((long) statRecord.mEnableTotTime) + enableTotTime2);
            }
            long enableTotTime3 = (long) (statRecord.mEnableTotTime / 60);
            if (statIntervalMinutes2 == 0 || statIntervalMinutes2 > 2147483632 || enableTotTime3 > 2147483632) {
                resetStatRecord(statRecord, "interval time abnormal data record invalid");
                return false;
            }
            if (statIntervalMinutes2 < enableTotTime3) {
                statIntervalMinutes2 = enableTotTime3;
            }
            if (enableTotTime3 != 0) {
                statRecord.mHistoryTotWifiConnHour += statRecord.mTotWifiConnectTime;
                int i = statRecord.mHistoryTotWifiConnHour / 3600;
                IGetApRecordCount iGetApRecordCount = this.mGetApRecordCountCallBack;
                if (iGetApRecordCount != null) {
                    iGetApRecordCount.statisticApInfoRecord();
                    statRecord.mTotAPRecordCnt = (short) this.mGetApRecordCountCallBack.getTotRecordCount();
                    statRecord.mTotHomeAPCnt = (short) this.mGetApRecordCountCallBack.getHomeApRecordCount();
                }
                logI("upload stat CHR, curr date:" + currDateStr + ", interval mins:" + statIntervalMinutes2 + ", tot ap record:" + ((int) statRecord.mTotAPRecordCnt) + ", tot home ap record:" + ((int) statRecord.mTotHomeAPCnt));
                statRecord.mWifiproStateAtReportTime = getWifiproState();
                Bundle wifiproStatPara = new Bundle();
                wifiproStatPara.putInt("mWifiOobInitState", statRecord.mWifiOobInitState);
                wifiproStatPara.putInt("mWifiproOpenCount", statRecord.mWifiproOpenCount);
                wifiproStatPara.putInt("mCellAutoOpenCount", statRecord.mCellAutoOpenCount);
                wifiproStatPara.putInt("mWifiToWifiSuccCount", statRecord.mWifiToWifiSuccCount);
                wifiproStatPara.putInt("mTotalBQE_BadROC", statRecord.mTotalBqeBadRoc);
                wifiproStatPara.putInt("mManualBackROC", statRecord.mManualBackROC);
                wifiproStatPara.putInt("mSelectNotInetAPCount", statRecord.mSelectNotInetAPCount);
                wifiproStatPara.putInt("mNotInetWifiToWifiCount", statRecord.mNotInetWifiToWifiCount);
                wifiproStatPara.putInt("mReopenWifiRICount", statRecord.mReopenWifiRICount);
                wifiproStatPara.putInt("mBG_FreeInetOkApCnt", statRecord.mBgFreeInetOkApCnt);
                wifiproStatPara.putInt("mBG_FishingApCnt", statRecord.mBgFishingApCnt);
                wifiproStatPara.putInt("mBG_FreeNotInetApCnt", statRecord.mBgFreeNotInetApCnt);
                wifiproStatPara.putInt("mBG_PortalApCnt", statRecord.mBgPortalApCnt);
                wifiproStatPara.putInt("mBG_FailedCnt", statRecord.mBgFailedCnt);
                wifiproStatPara.putInt("mBG_UserSelApFishingCnt", statRecord.mBgUserSelApFishingCnt);
                wifiproStatPara.putInt("mBG_UserSelNoInetCnt", statRecord.mBgUserSelNoInetCnt);
                wifiproStatPara.putInt("mBG_UserSelPortalCnt", statRecord.mBgUserSelPortalCnt);
                wifiproStatPara.putInt("mManualConnBlockPortalCount", statRecord.mManualConnBlockPortalCount);
                Bundle DFTEventData = new Bundle();
                DFTEventData.putInt("eventId", 909002032);
                DFTEventData.putBundle("eventData", wifiproStatPara);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, DFTEventData);
            } else {
                logI("wifipro not enable at all, not upload stat CHR. curr Date:" + currDateStr + ", last upload Date" + this.mNewestStatRcd.mLastStatUploadTime);
            }
            int saveOldHistoryConnTime = statRecord.mHistoryTotWifiConnHour;
            resetStatRecord(statRecord, "statistics CHR event upload success, new period start.");
            statRecord.mHistoryTotWifiConnHour = saveOldHistoryConnTime;
            return true;
        }
    }

    private boolean checkInitOk() {
        if (this.mContext != null && this.mNewestStatRcd != null && this.mDataBaseManager != null && this.mSimpleDateFmt != null && this.mStatHandler != null) {
            return true;
        }
        logE("checkInitOk null error.");
        return false;
    }

    public void updateInitialWifiproState(boolean boolState) {
        if (checkInitOk()) {
            logI("updateInitialWifiproState enter.");
            sendStatMsg(117, boolState ? 1 : 2, 0);
        }
    }

    public void updateWifiproState(boolean boolState) {
        if (checkInitOk()) {
            short state = 2;
            if (boolState) {
                state = 1;
                logI("updateWifiproState rs: enable");
            } else {
                logI("updateWifiproState rs: disable");
            }
            sendStatMsg(103, state, 0);
        }
    }

    /* access modifiers changed from: private */
    public short getWifiproState() {
        if (!checkInitOk()) {
            return 0;
        }
        if (WifiProCommonUtils.isWifiProSwitchOn(this.mContext)) {
            logI("getWifiproState rs: enable");
            return 1;
        }
        logI("getWifiproState rs: disable");
        return 2;
    }

    public void registerMobileInfoCallback(IGetMobileInfoCallBack infoCallback) {
        logI("registerMobileInfoCallback enter.");
        this.mGetMobileInfoCallBack = infoCallback;
    }

    public void registerGetApRecordCountCallBack(IGetApRecordCount callback) {
        logI("registerGetApRecordCountCallBack enter.");
        this.mGetApRecordCountCallBack = callback;
    }

    /* access modifiers changed from: private */
    public int getMobileSignalLevel() {
        IGetMobileInfoCallBack iGetMobileInfoCallBack = this.mGetMobileInfoCallBack;
        if (iGetMobileInfoCallBack == null) {
            return -1;
        }
        int mobileSignalLevel = iGetMobileInfoCallBack.onGetMobileSignalLevel();
        logI("getMobileSignalLevel new level:" + mobileSignalLevel);
        return mobileSignalLevel;
    }

    /* access modifiers changed from: private */
    public int getMobileRATType() {
        IGetMobileInfoCallBack iGetMobileInfoCallBack = this.mGetMobileInfoCallBack;
        if (iGetMobileInfoCallBack == null) {
            return 0;
        }
        int mobileRatType = iGetMobileInfoCallBack.onGetMobileRATType();
        logI("getMobileRATType new type:" + mobileRatType);
        return mobileRatType;
    }

    public void setBQERoveOutReason(boolean rssiTcpBad, boolean otaTcpBad, boolean tcpBad, boolean bigRtt, WifiProRoveOutParaRecord roveOutPara) {
        if (checkInitOk()) {
            this.mBQEROReasonFlag = 0;
            if (rssiTcpBad) {
                this.mBQEROReasonFlag |= 1;
            }
            if (otaTcpBad) {
                this.mBQEROReasonFlag |= 2;
            }
            if (tcpBad) {
                this.mBQEROReasonFlag |= 4;
            }
            if (bigRtt) {
                this.mBQEROReasonFlag |= 8;
            }
            logI("setBQERoveOutReason enter, reason:" + this.mBQEROReasonFlag);
            this.mRoveOutPara = roveOutPara;
            sendStatEmptyMsg(115);
        }
    }

    public void sendWifiproRoveOutEvent(int reason) {
        if (checkInitOk()) {
            logI("sendWifiproRoveOutEvent enter, reason:" + reason);
            sendStatMsg(113, reason, this.mBQEROReasonFlag);
        }
    }

    public void setBQERoveInReason(int roveInReason) {
        if (checkInitOk()) {
            logI("setBQERoveInReason enter, reason:" + roveInReason);
            this.mBQERoveInReason = roveInReason;
        }
    }

    public void sendWifiproRoveInEvent(int riReason) {
        if (checkInitOk()) {
            logI("sendWifiproRoveInEvent enter, reason:" + riReason);
            sendStatMsg(114, riReason, this.mBQERoveInReason);
        }
    }

    public void increaseNoInetHandoverCount() {
        if (checkInitOk()) {
            logI("increaseNoInetHandoverCount enter.");
            sendStatEmptyMsg(104);
        }
    }

    public void increaseWiFiHandoverWiFiCount(int handOverType) {
        if (checkInitOk()) {
            logI("increaseWiFiHandoverWiFiCount enter. handOverType =" + handOverType);
            sendStatMsg(120, handOverType, 0);
        }
    }

    public void increaseUserReopenWifiRiCount() {
        if (checkInitOk()) {
            logD("increaseUserReopenWifiRiCount enter.");
            sendStatEmptyMsg(STAT_MSG_USER_REOPEN_WIFI_RI_CNT);
        }
    }

    public void increaseSelCspSettingChgCount(int newSettingValue) {
        if (checkInitOk()) {
            logI("increaseSelCspSettingChgCount enter. csp val=" + newSettingValue);
            sendStatMsg(STAT_MSG_SEL_CSP_SETTING_CHG_CNT, newSettingValue, 0);
        }
    }

    public void increaseHMDNotifyCount(int notifyType) {
        if (checkInitOk()) {
            logI("increaseSelCspSettingChgCount enter. notifyType val=" + notifyType);
            sendStatMsg(STAT_MSG_HMD_NOTIFY_CNT, notifyType, 0);
        }
    }

    public void increaseUserDelNotifyCount() {
        if (checkInitOk()) {
            logD("increaseUserDelNotifyCount enter.");
            sendStatEmptyMsg(STAT_MSG_HMD_USER_DEL_NOTIFY_CNT);
        }
    }

    public void increaseHighMobileDataBtnRiCount() {
        if (checkInitOk()) {
            logI("increaseHighMobileDataBtnRiCount enter.");
            sendStatEmptyMsg(STAT_MSG_INCREASE_HMD_BTN_RI_COUNT);
        }
    }

    public void uploadPortalAutoFillStatus(boolean success, int type) {
        if (checkInitOk()) {
            logI("uploadPortalAutoFillStatus enter. state=" + success + ", type=" + type);
            sendStatMsg(STAT_MSG_AF_CHR_UPDATE, success ? 1 : 0, type);
        }
    }

    public void increasePortalNoAutoConnCnt() {
        if (checkInitOk()) {
            logD("increasePortalNoAutoConnCnt enter.");
            sendStatEmptyMsg(STAT_MSG_PORTAL_AP_NOT_AUTO_CONN);
        }
    }

    public void increaseHomeAPAddRoPeriodCnt(int periodCount) {
        if (checkInitOk()) {
            logI("increaseHomeAPAddRoPeriodCnt enter. periodCount=" + periodCount);
            sendStatMsg(STAT_MSG_HOME_AP_ADD_RO_PERIOD_CNT, periodCount, 0);
        }
    }

    public void updateStatisticToDB() {
        if (checkInitOk()) {
            logD("updateStatisticToDB enter.");
            if (!this.mStatHandler.hasMessages(STAT_MSG_UPDATE_SATTISTIC_TO_DB)) {
                sendStatEmptyMsg(STAT_MSG_UPDATE_SATTISTIC_TO_DB);
            }
        }
    }

    public void increaseBgAcDiffType(int diffType) {
        if (checkInitOk()) {
            logI("increaseBG_AC_DiffType enter. diffType=" + diffType);
            sendStatMsg(STAT_MSG_ACTIVE_CHECK_RS_DIFF, diffType, 0);
        }
    }

    public void updateBgApSsid(String bgAPSSID) {
        if (bgAPSSID == null) {
            this.mBgApSsid = "";
        } else {
            this.mBgApSsid = bgAPSSID;
        }
        logI("updateBG_AP_SSID enter. BG SSID=" + StringUtilEx.safeDisplaySsid(this.mBgApSsid));
    }

    public void updateBGChrStatistic(int bgChrType) {
        if (checkInitOk()) {
            logI("updateBGChrStatistic enter. notifyType val=" + bgChrType);
            sendStatMsg(STAT_MSG_BACK_GRADING_CHR_UPDATE, bgChrType, 0);
        }
    }

    public void updateBqeSvcChrStatistic(int bqeSvcChrType) {
        if (checkInitOk()) {
            logI("updateBGChrStatistic enter. notifyType val=" + bqeSvcChrType);
            sendStatMsg(STAT_MSG_BQE_GRADING_SVC_CHR_UPDATE, bqeSvcChrType, 0);
        }
    }

    public void increaseNoInetRemindCount(boolean isConnAlarm) {
        if (checkInitOk()) {
            logI("increaseNoInetRemindCount enter. isConnAlarm = " + isConnAlarm);
            sendStatMsg(118, isConnAlarm ? 1 : 0, 0);
        }
    }

    public void increaseUserUseBgScanAPCount() {
        if (checkInitOk()) {
            logD("increaseUserUseBgScanAPCount enter.");
            sendStatEmptyMsg(119);
        }
    }

    public void increasePingPongCount() {
        if (checkInitOk()) {
            logD("increasePingPongCount enter.");
            sendStatEmptyMsg(124);
        }
    }

    public void increaseBqeBadSettingCancelCount() {
        if (checkInitOk()) {
            logD("increaseBQE_BadSettingCancelCount enter.");
            sendStatEmptyMsg(125);
        }
    }

    public void increaseNotInetSettingCancelCount() {
        if (checkInitOk()) {
            logD("increaseNotInetSettingCancelCount enter.");
            sendStatEmptyMsg(126);
        }
    }

    public void increaseNotInetUserCancelCount() {
        if (checkInitOk()) {
            logD("increaseNotInetUserCancelCount enter.");
            sendStatEmptyMsg(127);
        }
    }

    public void increaseNotInetRestoreRICount() {
        if (checkInitOk()) {
            logD("increaseNotInetRestoreRICount enter.");
            sendStatEmptyMsg(128);
        }
    }

    public void increaseNotInetUserManualRICount() {
        if (checkInitOk()) {
            logD("increaseNotInetUserManualRICount enter.");
            sendStatEmptyMsg(STAT_MSG_NOT_INET_USER_MANUAL_RI);
        }
    }

    public void increaseSelectNotInetAPCount() {
        if (checkInitOk()) {
            logD("increaseSelectNotInetAPCount enter.");
            sendStatEmptyMsg(121);
        }
    }

    public void increaseNotAutoConnPortalCnt() {
        if (checkInitOk()) {
            logD("increaseNotAutoConnPortalCnt enter.");
            sendStatEmptyMsg(122);
        }
    }

    public void increasePortalUnauthCount() {
        if (checkInitOk()) {
            logD("increasePortalUnauthCount enter.");
            sendStatEmptyMsg(105);
        }
    }

    public void increaseWifiScoCount() {
        if (checkInitOk()) {
            logD("increaseWifiScoCount enter.");
            sendStatEmptyMsg(106);
        }
    }

    public void increasePortalCodeParseCount() {
        if (checkInitOk()) {
            logD("increasePortalCodeParseCount enter.");
            sendStatEmptyMsg(107);
        }
    }

    public void increaseRcvSmsCount() {
        if (checkInitOk()) {
            logD("increaseRcvSMS_Count enter.");
            sendStatEmptyMsg(108);
        }
    }

    public void increasePortalAutoLoginCount() {
        if (checkInitOk()) {
            logD("increasePortalAutoLoginCount enter.");
            sendStatEmptyMsg(109);
        }
    }

    public void increaseAutoOpenCount() {
        if (checkInitOk()) {
            logD("increaseCellAutoOpenCount enter.");
            sendStatEmptyMsg(110);
        }
    }

    public void increaseAutoCloseCount() {
        if (checkInitOk()) {
            logD("increaseCellAutoCloseCount enter.");
            sendStatEmptyMsg(111);
        }
    }

    public void increaseHighDataRateStopROC() {
        if (checkInitOk()) {
            logD("increaseHighDataRateStopROC enter.");
            sendStatEmptyMsg(123);
        }
    }

    public void increasePortalConnectedCnt() {
        if (checkInitOk()) {
            logD("increasePortalConnectedCnt enter.");
            sendStatEmptyMsg(STAT_MSG_INCREASE_PORTAL_CONN_COUNT);
        }
    }

    public void increasePortalConnectedAndAuthenCnt() {
        if (checkInitOk()) {
            logD("increasePortalConnectedAndAuthenCnt enter.");
            sendStatEmptyMsg(STAT_MSG_INCREASE_PORTAL_AUTH_SUCC_COUNT);
        }
    }

    public void increasePortalRefusedButUserTouchCnt() {
        if (checkInitOk()) {
            logD("increasePortalRefusedButUserTouchCnt enter.");
            sendStatEmptyMsg(STAT_MSG_INCREASE_CONN_BLOCK_PORTAL_COUNT);
        }
    }

    public void increaseActiveCheckRsSame() {
        if (checkInitOk()) {
            logD("increaseActiveCheckRS_Same enter.");
            sendStatEmptyMsg(STAT_MSG_INCREASE_AC_RS_SAME_COUNT);
        }
    }

    public void accuQOEBadRoDisconnectData() {
        if (checkInitOk()) {
            logD("accuQOERoDisconnectData enter.");
            sendStatEmptyMsg(STAT_MSG_BQE_BAD_RO_DISCONNECT_MOBILE_DATA);
        }
    }

    public void accuNotInetRoDisconnectData() {
        if (checkInitOk()) {
            logD("accuNotInetRoDisconnectData enter.");
            sendStatEmptyMsg(STAT_MSG_NOT_INET_RO_DISCONNECT_MOBILE_DATA);
        }
    }

    public void updateWifiConnectState(int newWifiState) {
        if (checkInitOk()) {
            logD("updateWifiConnectState enter.");
            sendStatMsg(STAT_MSG_UPDATE_WIFI_CONNECTION_STATE, newWifiState, 0);
        }
    }

    public void sendScreenOnEvent() {
        if (checkInitOk()) {
            logD("sendScreenOnEvent enter.");
            sendStatEmptyMsg(STAT_MSG_SCREEN_ON);
        }
    }

    /* access modifiers changed from: private */
    public void logD(String msg) {
        if (printLogLevel <= 1) {
            Log.d(TAG, msg);
        }
    }

    /* access modifiers changed from: private */
    public void logI(String msg) {
        if (printLogLevel <= 2) {
            Log.i(TAG, msg);
        }
    }

    /* access modifiers changed from: private */
    public void logE(String msg) {
        if (printLogLevel <= 3) {
            Log.e(TAG, msg);
        }
    }

    public void sendStatEmptyMsg(int what) {
        Handler handler = this.mStatHandler;
        if (handler != null) {
            handler.sendEmptyMessage(what);
        }
    }

    public void sendStatMsg(int what, int arg1, int arg2) {
        Handler handler = this.mStatHandler;
        if (handler != null) {
            handler.sendMessage(Message.obtain(handler, what, arg1, arg2));
        }
    }

    public void sendStatEmptyMsgDelayed(int what, long delayMillis) {
        Handler handler = this.mStatHandler;
        if (handler != null) {
            handler.sendEmptyMessageDelayed(what, delayMillis);
        }
    }

    /* access modifiers changed from: private */
    public void roveOutEventProcess(int roReason, int bqeRoReasonFlag) {
        logI("roveOutEventProcess enter, RO reason:" + roReason + ", BQE RO reason:" + bqeRoReasonFlag);
        if (1 == roReason) {
            if ((bqeRoReasonFlag & 1) != 0) {
                WifiProStatisticsRecord wifiProStatisticsRecord = this.mNewestStatRcd;
                wifiProStatisticsRecord.mRssiRoTot = (short) (wifiProStatisticsRecord.mRssiRoTot + 1);
            }
            if ((bqeRoReasonFlag & 2) != 0) {
                WifiProStatisticsRecord wifiProStatisticsRecord2 = this.mNewestStatRcd;
                wifiProStatisticsRecord2.mOtaRoTot = (short) (wifiProStatisticsRecord2.mOtaRoTot + 1);
            }
            if ((bqeRoReasonFlag & 4) != 0) {
                WifiProStatisticsRecord wifiProStatisticsRecord3 = this.mNewestStatRcd;
                wifiProStatisticsRecord3.mTcpRoTot = (short) (wifiProStatisticsRecord3.mTcpRoTot + 1);
            }
            if ((bqeRoReasonFlag & 8) != 0) {
                WifiProStatisticsRecord wifiProStatisticsRecord4 = this.mNewestStatRcd;
                wifiProStatisticsRecord4.mBigRttRoTot = (short) (wifiProStatisticsRecord4.mBigRttRoTot + 1);
            }
            WifiProStatisticsRecord wifiProStatisticsRecord5 = this.mNewestStatRcd;
            wifiProStatisticsRecord5.mTotalBqeBadRoc = (short) (wifiProStatisticsRecord5.mTotalBqeBadRoc + 1);
            this.mDataBaseManager.addOrUpdateChrStatRcd(this.mNewestStatRcd);
        }
        this.mROReason = roReason;
        this.mROTime = SystemClock.elapsedRealtime();
    }

    /* access modifiers changed from: private */
    public int getRoMobileData() {
        IGetMobileInfoCallBack iGetMobileInfoCallBack = this.mGetMobileInfoCallBack;
        if (iGetMobileInfoCallBack != null) {
            return iGetMobileInfoCallBack.getTotalRoMobileData();
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public void roveInEventProcess(int riReason, int bqeRiReasonFlag, int bqeRoReasonFlag, int roReason) {
        long passTime;
        boolean needUpdateDB = false;
        int currentRoReason = roReason;
        logI("roveInEventProcess enter, RI reason:" + riReason + ", BQE RI reason:" + bqeRiReasonFlag + ", roReasonFlag:" + bqeRoReasonFlag + ", roReason:" + roReason);
        if (2 == riReason) {
            passTime = 1;
            logI("update ro reason by cancel ri event.");
            currentRoReason = 1;
            roveOutEventProcess(1, bqeRoReasonFlag);
        } else if (this.mROTime == 0) {
            passTime = 0;
        } else {
            passTime = (SystemClock.elapsedRealtime() - this.mROTime) / 1000;
            if (passTime == 1) {
                passTime = 2;
            }
        }
        this.mROTime = 0;
        if (1 == riReason) {
            if (1 == bqeRiReasonFlag) {
                WifiProStatisticsRecord wifiProStatisticsRecord = this.mNewestStatRcd;
                wifiProStatisticsRecord.mRssiRestoreRiCount = (short) (wifiProStatisticsRecord.mRssiRestoreRiCount + 1);
            } else if (2 == bqeRiReasonFlag) {
                WifiProStatisticsRecord wifiProStatisticsRecord2 = this.mNewestStatRcd;
                wifiProStatisticsRecord2.mRssiBetterRiCount = (short) (wifiProStatisticsRecord2.mRssiBetterRiCount + 1);
            } else if (3 == bqeRiReasonFlag) {
                WifiProStatisticsRecord wifiProStatisticsRecord3 = this.mNewestStatRcd;
                wifiProStatisticsRecord3.mTimerRiCount = (short) (wifiProStatisticsRecord3.mTimerRiCount + 1);
            }
            WifiProStatisticsRecord wifiProStatisticsRecord4 = this.mNewestStatRcd;
            wifiProStatisticsRecord4.mAutoRiTotCount = (short) (wifiProStatisticsRecord4.mAutoRiTotCount + 1);
            WifiProStatisticsRecord wifiProStatisticsRecord5 = this.mNewestStatRcd;
            wifiProStatisticsRecord5.mAutoRiTotTime = (int) (((long) wifiProStatisticsRecord5.mAutoRiTotTime) + passTime);
            this.mNewestStatRcd.mQoeAutoRiTotData += getRoMobileData();
            needUpdateDB = true;
        } else if (6 == riReason) {
            WifiProStatisticsRecord wifiProStatisticsRecord6 = this.mNewestStatRcd;
            wifiProStatisticsRecord6.mHisScoRiCount = (short) (wifiProStatisticsRecord6.mHisScoRiCount + 1);
            needUpdateDB = true;
        }
        boolean isManualRI = 3 == riReason || 4 == riReason || 5 == riReason;
        boolean isUserCancelRI = 2 == riReason;
        if (1 == currentRoReason) {
            if (isUserCancelRI || isManualRI) {
                logI("abnormal BQE bad rove out statistics process.");
                if ((bqeRoReasonFlag & 1) != 0) {
                    WifiProStatisticsRecord wifiProStatisticsRecord7 = this.mNewestStatRcd;
                    wifiProStatisticsRecord7.mRssiErrRoTot = (short) (wifiProStatisticsRecord7.mRssiErrRoTot + 1);
                }
                if ((bqeRoReasonFlag & 2) != 0) {
                    WifiProStatisticsRecord wifiProStatisticsRecord8 = this.mNewestStatRcd;
                    wifiProStatisticsRecord8.mOtaErrRoTot = (short) (wifiProStatisticsRecord8.mOtaErrRoTot + 1);
                }
                if ((bqeRoReasonFlag & 4) != 0) {
                    WifiProStatisticsRecord wifiProStatisticsRecord9 = this.mNewestStatRcd;
                    wifiProStatisticsRecord9.mTcpErrRoTot = (short) (wifiProStatisticsRecord9.mTcpErrRoTot + 1);
                }
                if ((bqeRoReasonFlag & 8) != 0) {
                    WifiProStatisticsRecord wifiProStatisticsRecord10 = this.mNewestStatRcd;
                    wifiProStatisticsRecord10.mBigRttErrRoTot = (short) (wifiProStatisticsRecord10.mBigRttErrRoTot + 1);
                }
                if (passTime > 32760) {
                    passTime = 32760;
                }
                sendUnexpectedROParaCHREvent(this.mRoveOutPara, (short) ((int) passTime));
            }
            if (isManualRI) {
                WifiProStatisticsRecord wifiProStatisticsRecord11 = this.mNewestStatRcd;
                wifiProStatisticsRecord11.mManualRiTotTime = (int) (((long) wifiProStatisticsRecord11.mManualRiTotTime) + passTime);
                WifiProStatisticsRecord wifiProStatisticsRecord12 = this.mNewestStatRcd;
                wifiProStatisticsRecord12.mManualBackROC = (short) (wifiProStatisticsRecord12.mManualBackROC + 1);
                IGetMobileInfoCallBack iGetMobileInfoCallBack = this.mGetMobileInfoCallBack;
                int roDataKB = iGetMobileInfoCallBack != null ? iGetMobileInfoCallBack.getTotalRoMobileData() : 0;
                if (4 == riReason) {
                    WifiProStatisticsRecord wifiProStatisticsRecord13 = this.mNewestStatRcd;
                    wifiProStatisticsRecord13.mTotBtnRICount = (short) (wifiProStatisticsRecord13.mTotBtnRICount + 1);
                }
                this.mNewestStatRcd.mRoTotMobileData += roDataKB;
            }
            if (isUserCancelRI) {
                WifiProStatisticsRecord wifiProStatisticsRecord14 = this.mNewestStatRcd;
                wifiProStatisticsRecord14.mUserCancelROC = (short) (wifiProStatisticsRecord14.mUserCancelROC + 1);
            }
            needUpdateDB = true;
        }
        if (needUpdateDB) {
            this.mDataBaseManager.addOrUpdateChrStatRcd(this.mNewestStatRcd);
        }
    }

    private void sendUnexpectedROParaCHREvent(WifiProRoveOutParaRecord roveOutPara, short roTime) {
        if (roveOutPara != null) {
            logI("unexpected RO para CHR event send.");
            roveOutPara.mRoDuration = roTime;
        }
    }

    /* access modifiers changed from: private */
    public void sendPingpongCHREvent(WifiProRoveOutParaRecord roveOutPara, short roTime) {
        if (roveOutPara != null) {
            logI("Pingpong RO para CHR event send.");
            roveOutPara.mRoDuration = roTime;
        }
    }

    private void initStatHandler(Looper looper) {
        Looper currentLooper = looper;
        if (currentLooper == null) {
            logI("looper null, force create single thread");
            HandlerThread thread = new HandlerThread("StatisticsCHRMsgHandler");
            thread.start();
            currentLooper = thread.getLooper();
        }
        this.mStatHandler = new StatisticsCHRMsgHandler(currentLooper);
    }

    /* access modifiers changed from: private */
    public void sendCheckUploadMsg() {
        if (this.mStatHandler.hasMessages(101)) {
            logE("There has CHECK_NEED_UPLOAD_EVENT msg at queue.");
        } else if (debugMode) {
            sendStatEmptyMsgDelayed(101, 60000);
        } else {
            sendStatEmptyMsgDelayed(101, 1800000);
        }
    }

    /* access modifiers changed from: private */
    public void checkMsgLoopRunning() {
        if (!this.mStatHandler.hasMessages(101)) {
            logI("restart msg Loop.");
            sendStatEmptyMsg(101);
            return;
        }
        logD("msg Loop is running.");
    }

    /* access modifiers changed from: private */
    public void updateBGChrProcess(int bgChrType) {
        logI("updateBGChrProcess enter for bg type:" + bgChrType);
        if (bgChrType == 20) {
            WifiProStatisticsRecord wifiProStatisticsRecord = this.mNewestStatRcd;
            wifiProStatisticsRecord.mBgNcbyConnectFail = (short) (wifiProStatisticsRecord.mBgNcbyConnectFail + 1);
        } else if (bgChrType != 21) {
            switch (bgChrType) {
                case 1:
                    WifiProStatisticsRecord wifiProStatisticsRecord2 = this.mNewestStatRcd;
                    wifiProStatisticsRecord2.mBgBgRunCnt = (short) (wifiProStatisticsRecord2.mBgBgRunCnt + 1);
                    return;
                case 2:
                    return;
                case 3:
                    WifiProStatisticsRecord wifiProStatisticsRecord3 = this.mNewestStatRcd;
                    wifiProStatisticsRecord3.mBgFreeInetOkApCnt = (short) (wifiProStatisticsRecord3.mBgFreeInetOkApCnt + 1);
                    return;
                case 4:
                    WifiProStatisticsRecord wifiProStatisticsRecord4 = this.mNewestStatRcd;
                    wifiProStatisticsRecord4.mBgFishingApCnt = (short) (wifiProStatisticsRecord4.mBgFishingApCnt + 1);
                    return;
                case 5:
                    WifiProStatisticsRecord wifiProStatisticsRecord5 = this.mNewestStatRcd;
                    wifiProStatisticsRecord5.mBgFreeNotInetApCnt = (short) (wifiProStatisticsRecord5.mBgFreeNotInetApCnt + 1);
                    return;
                case 6:
                    WifiProStatisticsRecord wifiProStatisticsRecord6 = this.mNewestStatRcd;
                    wifiProStatisticsRecord6.mBgPortalApCnt = (short) (wifiProStatisticsRecord6.mBgPortalApCnt + 1);
                    return;
                case 7:
                    WifiProStatisticsRecord wifiProStatisticsRecord7 = this.mNewestStatRcd;
                    wifiProStatisticsRecord7.mBgUserSelFreeInetOkCnt = (short) (wifiProStatisticsRecord7.mBgUserSelFreeInetOkCnt + 1);
                    return;
                case 8:
                    WifiProStatisticsRecord wifiProStatisticsRecord8 = this.mNewestStatRcd;
                    wifiProStatisticsRecord8.mBgUserSelNoInetCnt = (short) (wifiProStatisticsRecord8.mBgUserSelNoInetCnt + 1);
                    return;
                case 9:
                    WifiProStatisticsRecord wifiProStatisticsRecord9 = this.mNewestStatRcd;
                    wifiProStatisticsRecord9.mBgUserSelPortalCnt = (short) (wifiProStatisticsRecord9.mBgUserSelPortalCnt + 1);
                    return;
                case 10:
                    WifiProStatisticsRecord wifiProStatisticsRecord10 = this.mNewestStatRcd;
                    wifiProStatisticsRecord10.mBgFoundTwoMoreApCnt = (short) (wifiProStatisticsRecord10.mBgFoundTwoMoreApCnt + 1);
                    return;
                case 11:
                    WifiProStatisticsRecord wifiProStatisticsRecord11 = this.mNewestStatRcd;
                    wifiProStatisticsRecord11.mBgFailedCnt = (short) (wifiProStatisticsRecord11.mBgFailedCnt + 1);
                    return;
                default:
                    logE("updateBGChrProcess error type:" + bgChrType);
                    return;
            }
        } else {
            WifiProStatisticsRecord wifiProStatisticsRecord12 = this.mNewestStatRcd;
            wifiProStatisticsRecord12.mBgNcbyCheckFail = (short) (wifiProStatisticsRecord12.mBgNcbyCheckFail + 1);
        }
    }

    /* access modifiers changed from: private */
    public void updateBqeSvcChrProcess(int bqeSvcChrType) {
    }

    class StatisticsCHRMsgHandler extends Handler {
        private StatisticsCHRMsgHandler(Looper looper) {
            super(looper);
            WifiProStatisticsManager.this.logD("new StatisticsCHRMsgHandler");
        }

        private void handleChrDbInit() {
            WifiProStatisticsManager.this.logI("ChrDataBaseManager init start.");
            WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
            WifiProChrDataBaseManager unused = wifiProStatisticsManager.mDataBaseManager = WifiProChrDataBaseManager.getInstance(wifiProStatisticsManager.mContext);
            WifiProStatisticsManager wifiProStatisticsManager2 = WifiProStatisticsManager.this;
            wifiProStatisticsManager2.loadStatDBRecord(wifiProStatisticsManager2.mNewestStatRcd);
            WifiProStatisticsManager.this.sendStatEmptyMsg(101);
        }

        private void checkUpload() {
            WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
            if (wifiProStatisticsManager.checkIfNeedUpload(wifiProStatisticsManager.mNewestStatRcd)) {
                WifiProStatisticsManager wifiProStatisticsManager2 = WifiProStatisticsManager.this;
                boolean unused = wifiProStatisticsManager2.uploadStatisticsCHREvent(wifiProStatisticsManager2.mNewestStatRcd);
            }
            if (WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproState == 0) {
                WifiProStatisticsManager.this.logE("wifipro state abnormal, try reget it.");
                short wifiproState = WifiProStatisticsManager.this.getWifiproState();
                if (wifiproState != 0) {
                    WifiProStatisticsManager.this.sendStatMsg(103, wifiproState, 0);
                }
            }
            WifiProStatisticsManager.this.sendCheckUploadMsg();
        }

        private void countHmdNotification(Message msg) {
            int notifyType = msg.arg1;
            if (1 == notifyType) {
                WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$500.mBmdTenmNotifyCount = (short) (access$500.mBmdTenmNotifyCount + 1);
                int unused = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 1;
            } else if (2 == notifyType) {
                WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$5002.mBmdFiftymNotifyCount = (short) (access$5002.mBmdFiftymNotifyCount + 1);
                int unused2 = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 2;
            }
            WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
        }

        private void handleStateChange(Message msg) {
            short wifiproState = (short) msg.arg1;
            if (wifiproState != WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproState) {
                Date currDate = new Date();
                String currDateStr = WifiProStatisticsManager.this.mSimpleDateFmt.format(currDate);
                if (wifiproState == 2 && WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproState == 1) {
                    WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
                    long enableTotTime = wifiProStatisticsManager.calcTimeInterval(wifiProStatisticsManager.mNewestStatRcd.mLastWifiproStateUpdateTime, currDate);
                    if (enableTotTime <= -1) {
                        WifiProStatisticsManager wifiProStatisticsManager2 = WifiProStatisticsManager.this;
                        wifiProStatisticsManager2.resetStatRecord(wifiProStatisticsManager2.mNewestStatRcd, "last WifiproStateUpdateTime time record invalid");
                        return;
                    }
                    long enableTotTime2 = enableTotTime / 1000;
                    WifiProStatisticsManager.this.mNewestStatRcd.mEnableTotTime += (int) enableTotTime2;
                    WifiProStatisticsManager.this.logI("last state en seconds:" + enableTotTime2);
                    WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$500.mWifiproCloseCount = (short) (access$500.mWifiproCloseCount + 1);
                } else if (wifiproState == 1 && WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproState == 2) {
                    WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5002.mWifiproOpenCount = (short) (access$5002.mWifiproOpenCount + 1);
                }
                WifiProStatisticsManager.this.logI("wifipro old state:" + ((int) WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproState) + ", new state:" + ((int) wifiproState) + ", date str:" + currDateStr);
                WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproState = wifiproState;
                WifiProStatisticsManager.this.mNewestStatRcd.mLastWifiproStateUpdateTime = currDateStr;
                WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                return;
            }
            WifiProStatisticsManager.this.logE("wifipro state unknow or not changed msg receive:" + ((int) wifiproState));
        }

        private void sendPingPongEvent() {
            int passTime = (int) ((SystemClock.elapsedRealtime() - WifiProStatisticsManager.this.mROTime) / 1000);
            if (passTime == 1) {
                passTime = 2;
            }
            if (passTime > WifiProStatisticsManager.MAX_SHORT_TYPE_VALUE) {
                passTime = WifiProStatisticsManager.MAX_SHORT_TYPE_VALUE;
            }
            WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
            wifiProStatisticsManager.sendPingpongCHREvent(wifiProStatisticsManager.mRoveOutPara, (short) passTime);
        }

        private void handleRiEvent(Message msg) {
            int riReason = msg.arg1;
            int bqeRiReason = msg.arg2;
            if (WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted || 2 == riReason) {
                boolean unused = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = false;
                WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
                wifiProStatisticsManager.roveInEventProcess(riReason, bqeRiReason, wifiProStatisticsManager.mBQEROReasonFlag, WifiProStatisticsManager.this.mROReason);
                return;
            }
            WifiProStatisticsManager wifiProStatisticsManager2 = WifiProStatisticsManager.this;
            wifiProStatisticsManager2.logI("Ignore duplicate qoe RI reason:" + riReason);
        }

        private void countCspChange(Message msg) {
            int cspVal = msg.arg1;
            if (cspVal == 0) {
                WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$500.mSelCSPShowDiglogCount = (short) (access$500.mSelCSPShowDiglogCount + 1);
            } else if (1 == cspVal) {
                WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$5002.mSelCSPAutoSwCount = (short) (access$5002.mSelCSPAutoSwCount + 1);
            } else if (2 == cspVal) {
                WifiProStatisticsRecord access$5003 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$5003.mSelCSPNotSwCount = (short) (access$5003.mSelCSPNotSwCount + 1);
            }
            WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
        }

        private void afChrUpdate(Message msg) {
            int state = msg.arg1;
            int type = msg.arg2;
            if (type == 0) {
                if (1 == state) {
                    WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$500.mAfPhoneNumSuccCnt = (short) (access$500.mAfPhoneNumSuccCnt + 1);
                } else {
                    WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5002.mAfPhoneNumFailCnt = (short) (access$5002.mAfPhoneNumFailCnt + 1);
                }
            } else if (1 == type) {
                if (1 == state) {
                    WifiProStatisticsRecord access$5003 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5003.mAfPasswordSuccCnt = (short) (access$5003.mAfPasswordSuccCnt + 1);
                } else {
                    WifiProStatisticsRecord access$5004 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5004.mAfPasswordFailCnt = (short) (access$5004.mAfPasswordFailCnt + 1);
                }
            } else if (2 == type) {
                if (1 == state) {
                    WifiProStatisticsRecord access$5005 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5005.mAfAutoLoginSuccCnt = (short) (access$5005.mAfAutoLoginSuccCnt + 1);
                } else {
                    WifiProStatisticsRecord access$5006 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5006.mAfAutoLoginFailCnt = (short) (access$5006.mAfAutoLoginFailCnt + 1);
                }
            } else if (3 == type) {
                WifiProStatisticsRecord access$5007 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$5007.mAfFpnsSuccNotMsmCnt = (short) (access$5007.mAfFpnsSuccNotMsmCnt + 1);
            }
            boolean unused = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
        }

        private void handleWiFiConnection(Message msg) {
            int wifiState = msg.arg1;
            if (1 == wifiState) {
                if (WifiProStatisticsManager.this.mWifiConnectStartTime == 0) {
                    long unused = WifiProStatisticsManager.this.mWifiConnectStartTime = SystemClock.elapsedRealtime();
                    WifiProStatisticsManager.this.logI("wifi connect start here.");
                    return;
                }
                WifiProStatisticsManager.this.logD("wifi already connected.");
            } else if (2 != wifiState) {
                WifiProStatisticsManager.this.logD("STAT_MSG_UPDATE_WIFI_CONNECTION_STATE: wifi state is not connected or disconnected ");
            } else if (WifiProStatisticsManager.this.mWifiConnectStartTime != 0) {
                long connectionTime = SystemClock.elapsedRealtime() - WifiProStatisticsManager.this.mWifiConnectStartTime;
                if (connectionTime > 0 && connectionTime < WifiProStatisticsManager.MAX_COMMERCIAL_USER_UPLOAD_PERIOD) {
                    long acctime = connectionTime / 1000;
                    WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$500.mTotWifiConnectTime = (int) (((long) access$500.mTotWifiConnectTime) + acctime);
                    WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
                    wifiProStatisticsManager.logI("acc wifi connection time:" + acctime + " s, total" + WifiProStatisticsManager.this.mNewestStatRcd.mTotWifiConnectTime);
                }
                WifiProStatisticsManager.this.logI("wifi connected end.");
                long unused2 = WifiProStatisticsManager.this.mWifiConnectStartTime = 0;
            }
        }

        private void countBmdNotification() {
            if (2 == WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType) {
                WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$500.mBmdFiftymRiCount = (short) (access$500.mBmdFiftymRiCount + 1);
            } else if (1 == WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType) {
                WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$5002.mBmdTenmRiCount = (short) (access$5002.mBmdTenmRiCount + 1);
            } else {
                WifiProStatisticsManager.this.logI("mRoveoutMobileDataNotifyType is not FIFTY_M or TEM_M");
            }
            int unused = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 0;
            WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
        }

        public void handleMessage(Message msg) {
            int notInetRoDataKB = 0;
            switch (msg.what) {
                case 100:
                    handleChrDbInit();
                    return;
                case 101:
                    checkUpload();
                    return;
                case 102:
                case 112:
                case 113:
                case 114:
                case 115:
                case 116:
                case 121:
                case 122:
                default:
                    keepHandleMessage(msg);
                    return;
                case 103:
                    handleStateChange(msg);
                    return;
                case 104:
                    WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$500.mNoInetHandoverCount = (short) (access$500.mNoInetHandoverCount + 1);
                    boolean unused = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = true;
                    int unused2 = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 0;
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 105:
                    WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5002.mPortalUnauthCount = (short) (access$5002.mPortalUnauthCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 106:
                    WifiProStatisticsRecord access$5003 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5003.mWifiScoCount = (short) (access$5003.mWifiScoCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 107:
                    WifiProStatisticsRecord access$5004 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5004.mPortalCodeParseCount = (short) (access$5004.mPortalCodeParseCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 108:
                    WifiProStatisticsRecord access$5005 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5005.mRcvSmsCount = (short) (access$5005.mRcvSmsCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 109:
                    WifiProStatisticsRecord access$5006 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5006.mPortalAutoLoginCount = (short) (access$5006.mPortalAutoLoginCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 110:
                    WifiProStatisticsRecord access$5007 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5007.mCellAutoOpenCount = (short) (access$5007.mCellAutoOpenCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 111:
                    WifiProStatisticsRecord access$5008 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5008.mCellAutoCloseCount = (short) (access$5008.mCellAutoCloseCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 117:
                    WifiProStatisticsManager.this.mNewestStatRcd.mWifiOobInitState = (short) msg.arg1;
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 118:
                    WifiProStatisticsRecord access$5009 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$5009.mNoInetAlarmCount = (short) (access$5009.mNoInetAlarmCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    if (1 == msg.arg1) {
                        WifiProStatisticsRecord access$50010 = WifiProStatisticsManager.this.mNewestStatRcd;
                        access$50010.mNoInetAlarmOnConnCnt = (short) (access$50010.mNoInetAlarmOnConnCnt + 1);
                        return;
                    }
                    return;
                case 119:
                    WifiProStatisticsRecord access$50011 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50011.mUserUseBgScanAPCount = (short) (access$50011.mUserUseBgScanAPCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 120:
                    int handOverType = (short) msg.arg1;
                    WifiProStatisticsRecord access$50012 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50012.mWifiToWifiSuccCount = (short) (access$50012.mWifiToWifiSuccCount + 1);
                    if (1 == handOverType) {
                        WifiProStatisticsRecord access$50013 = WifiProStatisticsManager.this.mNewestStatRcd;
                        access$50013.mNotInetWifiToWifiCount = (short) (access$50013.mNotInetWifiToWifiCount + 1);
                    }
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 123:
                    WifiProStatisticsRecord access$50014 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50014.mHighDataRateStopROC = (short) (access$50014.mHighDataRateStopROC + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 124:
                    WifiProStatisticsRecord access$50015 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50015.mPingPongCount = (short) (access$50015.mPingPongCount + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    sendPingPongEvent();
                    return;
                case 125:
                    WifiProStatisticsRecord access$50016 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50016.mBqeBadSettingCancel = (short) (access$50016.mBqeBadSettingCancel + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 126:
                    WifiProStatisticsRecord access$50017 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50017.mNotInetSettingCancel = (short) (access$50017.mNotInetSettingCancel + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 127:
                    WifiProStatisticsRecord access$50018 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50018.mNotInetUserCancel = (short) (access$50018.mNotInetUserCancel + 1);
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case 128:
                    if (!WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted) {
                        WifiProStatisticsManager.this.logI("Ignore duplicate not inet restore RI event.");
                        return;
                    }
                    boolean unused3 = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = false;
                    WifiProStatisticsRecord access$50019 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50019.mNotInetRestoreRI = (short) (access$50019.mNotInetRestoreRI + 1);
                    WifiProStatisticsManager.this.mNewestStatRcd.mNotInetAutoRiTotData += WifiProStatisticsManager.this.getRoMobileData();
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case WifiProStatisticsManager.STAT_MSG_NOT_INET_USER_MANUAL_RI /*{ENCODED_INT: 129}*/:
                    if (!WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted) {
                        WifiProStatisticsManager.this.logI("Ignore duplicate not inet user manual RI event.");
                        return;
                    }
                    boolean unused4 = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = false;
                    WifiProStatisticsRecord access$50020 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50020.mNotInetUserManualRI = (short) (access$50020.mNotInetUserManualRI + 1);
                    if (WifiProStatisticsManager.this.mGetMobileInfoCallBack != null) {
                        notInetRoDataKB = WifiProStatisticsManager.this.mGetMobileInfoCallBack.getTotalRoMobileData();
                    }
                    WifiProStatisticsRecord access$50021 = WifiProStatisticsManager.this.mNewestStatRcd;
                    access$50021.mTotBtnRICount = (short) (access$50021.mTotBtnRICount + 1);
                    WifiProStatisticsManager.this.mNewestStatRcd.mRoTotMobileData += notInetRoDataKB;
                    WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                    return;
                case WifiProStatisticsManager.STAT_MSG_SCREEN_ON /*{ENCODED_INT: 130}*/:
                    WifiProStatisticsManager.this.checkMsgLoopRunning();
                    return;
            }
        }

        /* JADX INFO: Multiple debug info for r1v28 int: [D('bgChrType' int), D('bqeSvcChrType' int)] */
        public void keepHandleMessage(Message msg) {
            int i = msg.what;
            if (i == 121) {
                WifiProStatisticsRecord access$500 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$500.mSelectNotInetAPCount = (short) (access$500.mSelectNotInetAPCount + 1);
                WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
            } else if (i != 122) {
                switch (i) {
                    case 113:
                        int roReason = msg.arg1;
                        int bqeRoReason = msg.arg2;
                        boolean unused = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = true;
                        int unused2 = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 0;
                        WifiProStatisticsManager.this.roveOutEventProcess(roReason, bqeRoReason);
                        return;
                    case 114:
                        handleRiEvent(msg);
                        return;
                    case 115:
                        if (WifiProStatisticsManager.this.mRoveOutPara != null) {
                            WifiProStatisticsManager.this.mRoveOutPara.mMobileSignalLevel = (short) WifiProStatisticsManager.this.getMobileSignalLevel();
                            WifiProStatisticsManager.this.mRoveOutPara.mRatType = (short) WifiProStatisticsManager.this.getMobileRATType();
                            return;
                        }
                        return;
                    case 116:
                        WifiProStatisticsRecord access$5002 = WifiProStatisticsManager.this.mNewestStatRcd;
                        access$5002.mHisScoRiCount = (short) (access$5002.mHisScoRiCount + 1);
                        WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                        return;
                    default:
                        switch (i) {
                            case WifiProStatisticsManager.STAT_MSG_USER_REOPEN_WIFI_RI_CNT /*{ENCODED_INT: 131}*/:
                                WifiProStatisticsRecord access$5003 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5003.mReopenWifiRICount = (short) (access$5003.mReopenWifiRICount + 1);
                                WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_SEL_CSP_SETTING_CHG_CNT /*{ENCODED_INT: 132}*/:
                                countCspChange(msg);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_HMD_NOTIFY_CNT /*{ENCODED_INT: 133}*/:
                                countHmdNotification(msg);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_HMD_USER_DEL_NOTIFY_CNT /*{ENCODED_INT: 134}*/:
                                WifiProStatisticsRecord access$5004 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5004.mBmdUserDelNotifyCount = (short) (access$5004.mBmdUserDelNotifyCount + 1);
                                WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_AF_CHR_UPDATE /*{ENCODED_INT: 135}*/:
                                afChrUpdate(msg);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_BACK_GRADING_CHR_UPDATE /*{ENCODED_INT: 136}*/:
                                WifiProStatisticsManager.this.updateBGChrProcess(msg.arg1);
                                boolean unused3 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                return;
                            case WifiProStatisticsManager.STAT_MSG_BQE_GRADING_SVC_CHR_UPDATE /*{ENCODED_INT: 137}*/:
                                WifiProStatisticsManager.this.updateBqeSvcChrProcess(msg.arg1);
                                boolean unused4 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                return;
                            case WifiProStatisticsManager.STAT_MSG_BQE_BAD_RO_DISCONNECT_MOBILE_DATA /*{ENCODED_INT: 138}*/:
                                WifiProStatisticsRecord access$5005 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5005.mQoeRoDisconnectCnt = (short) (access$5005.mQoeRoDisconnectCnt + 1);
                                int mobileData = WifiProStatisticsManager.this.getRoMobileData();
                                WifiProStatisticsManager wifiProStatisticsManager = WifiProStatisticsManager.this;
                                wifiProStatisticsManager.logI("qoe ro disconnect mobileData=" + mobileData);
                                WifiProStatisticsRecord access$5006 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5006.mQoeRoDisconnectTotData = access$5006.mQoeRoDisconnectTotData + mobileData;
                                boolean unused5 = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = false;
                                int unused6 = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 0;
                                boolean unused7 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                return;
                            case WifiProStatisticsManager.STAT_MSG_NOT_INET_RO_DISCONNECT_MOBILE_DATA /*{ENCODED_INT: 139}*/:
                                WifiProStatisticsRecord access$5007 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5007.mNotInetRoDisconnectCnt = (short) (access$5007.mNotInetRoDisconnectCnt + 1);
                                int mobileData2 = WifiProStatisticsManager.this.getRoMobileData();
                                WifiProStatisticsManager wifiProStatisticsManager2 = WifiProStatisticsManager.this;
                                wifiProStatisticsManager2.logI("inet ro disconnect mobileData=" + mobileData2);
                                WifiProStatisticsRecord access$5008 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5008.mNotInetRoDisconnectTotData = access$5008.mNotInetRoDisconnectTotData + mobileData2;
                                boolean unused8 = WifiProStatisticsManager.this.mQoeOrNotInetRoveOutStarted = false;
                                int unused9 = WifiProStatisticsManager.this.mRoveoutMobileDataNotifyType = 0;
                                WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_UPDATE_WIFI_CONNECTION_STATE /*{ENCODED_INT: 140}*/:
                                handleWiFiConnection(msg);
                                return;
                            case WifiProStatisticsManager.STAT_MSG_PORTAL_AP_NOT_AUTO_CONN /*{ENCODED_INT: 141}*/:
                                WifiProStatisticsRecord access$5009 = WifiProStatisticsManager.this.mNewestStatRcd;
                                access$5009.mPortalNoAutoConnCnt = (short) (access$5009.mPortalNoAutoConnCnt + 1);
                                boolean unused10 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                return;
                            case WifiProStatisticsManager.STAT_MSG_HOME_AP_ADD_RO_PERIOD_CNT /*{ENCODED_INT: 142}*/:
                                return;
                            default:
                                switch (i) {
                                    case WifiProStatisticsManager.STAT_MSG_UPDATE_SATTISTIC_TO_DB /*{ENCODED_INT: 144}*/:
                                        if (WifiProStatisticsManager.this.mNeedSaveCHRStatistic) {
                                            WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
                                            boolean unused11 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = false;
                                            return;
                                        }
                                        return;
                                    case WifiProStatisticsManager.STAT_MSG_ACTIVE_CHECK_RS_DIFF /*{ENCODED_INT: 145}*/:
                                        WifiProStatisticsRecord access$50010 = WifiProStatisticsManager.this.mNewestStatRcd;
                                        access$50010.mActiveCheckRsDiff = (short) (access$50010.mActiveCheckRsDiff + 1);
                                        boolean unused12 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                        return;
                                    case WifiProStatisticsManager.STAT_MSG_INCREASE_PORTAL_CONN_COUNT /*{ENCODED_INT: 146}*/:
                                        WifiProStatisticsRecord access$50011 = WifiProStatisticsManager.this.mNewestStatRcd;
                                        access$50011.mTotalPortalConnCount = (short) (access$50011.mTotalPortalConnCount + 1);
                                        boolean unused13 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                        return;
                                    case WifiProStatisticsManager.STAT_MSG_INCREASE_PORTAL_AUTH_SUCC_COUNT /*{ENCODED_INT: 147}*/:
                                        WifiProStatisticsRecord access$50012 = WifiProStatisticsManager.this.mNewestStatRcd;
                                        access$50012.mTotalPortalAuthSuccCount = (short) (access$50012.mTotalPortalAuthSuccCount + 1);
                                        boolean unused14 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                        return;
                                    case WifiProStatisticsManager.STAT_MSG_INCREASE_CONN_BLOCK_PORTAL_COUNT /*{ENCODED_INT: 148}*/:
                                        WifiProStatisticsRecord access$50013 = WifiProStatisticsManager.this.mNewestStatRcd;
                                        access$50013.mManualConnBlockPortalCount = (short) (access$50013.mManualConnBlockPortalCount + 1);
                                        boolean unused15 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                        return;
                                    case WifiProStatisticsManager.STAT_MSG_INCREASE_AC_RS_SAME_COUNT /*{ENCODED_INT: 149}*/:
                                        WifiProStatisticsRecord access$50014 = WifiProStatisticsManager.this.mNewestStatRcd;
                                        access$50014.mActiveCheckRsSame = (short) (access$50014.mActiveCheckRsSame + 1);
                                        boolean unused16 = WifiProStatisticsManager.this.mNeedSaveCHRStatistic = true;
                                        return;
                                    case WifiProStatisticsManager.STAT_MSG_INCREASE_HMD_BTN_RI_COUNT /*{ENCODED_INT: 150}*/:
                                        countBmdNotification();
                                        return;
                                    default:
                                        WifiProStatisticsManager.this.logE("statistics manager got unknow message.");
                                        return;
                                }
                        }
                }
            } else {
                WifiProStatisticsRecord access$50015 = WifiProStatisticsManager.this.mNewestStatRcd;
                access$50015.mNotAutoConnPortalCnt = (short) (access$50015.mNotAutoConnPortalCnt + 1);
                WifiProStatisticsManager.this.mDataBaseManager.addOrUpdateChrStatRcd(WifiProStatisticsManager.this.mNewestStatRcd);
            }
        }
    }

    public void uploadWifiproEvent(int eventId) {
        HwWifiConnectivityMonitor mWifiConnectivityMonitor = HwWifiConnectivityMonitor.getInstance();
        if (mWifiConnectivityMonitor != null) {
            List<String> mCircleStat = mWifiConnectivityMonitor.getCircleStat();
            if (mCircleStat != null) {
                int circleStatSize = mCircleStat.size();
                Bundle data = new Bundle();
                for (int i = 0; i < circleStatSize; i++) {
                    logI("uploadWifiproEvent circle-" + i + ": " + mCircleStat.get(i));
                    StringBuilder sb = new StringBuilder();
                    sb.append("circle-");
                    sb.append(i);
                    data.putString(sb.toString(), mCircleStat.get(i));
                }
                Bundle DFTEventData = new Bundle();
                DFTEventData.putInt("eventId", eventId);
                DFTEventData.putBundle("eventData", data);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, DFTEventData);
            }
            mWifiConnectivityMonitor.resetCircleStat();
        }
    }
}
