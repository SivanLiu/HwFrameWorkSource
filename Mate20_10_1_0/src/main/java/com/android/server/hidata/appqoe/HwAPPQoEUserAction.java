package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.huawei.lcagent.client.LogCollectManager;

public class HwAPPQoEUserAction {
    public static final String BCM_CHIP_4345 = "4345";
    public static final String BCM_CHIP_4359 = "4359";
    public static final String CHIPSET_TYPE_PROP = "ro.connectivity.sub_chiptype";
    public static final String DEFAULT_CHIP_TYPE = "none";
    public static final String HISI_CHIP_1102A = "1102a";
    public static final String HISI_CHIP_1103 = "1103";
    public static final String HISI_CHIP_1105 = "1105";
    private static final int INITIAL_USER_TYPE_CNT = 0;
    private static final String TAG = "HiData_HwAPPQoEUserAction";
    private static final int USER_TYPE_CNT_THRESHOLD = 3;
    private static final int WIFI_BAD_SIGNAL_THRESHOLD = 1;
    private static final int WIFI_DISABLE_TIME_ADVANCED = 1000;
    private static final int WIFI_DISABLE_TIME_DELTA = 10000;
    private static HwAPPQoEUserAction mUserAction = null;
    /* access modifiers changed from: private */
    public WifiStateChangeInfo curWifiStateInfo = new WifiStateChangeInfo();
    /* access modifiers changed from: private */
    public WifiSwitchChangeInfo currWifiDisableInfo = new WifiSwitchChangeInfo(-1);
    private IntentFilter intentFilter = new IntentFilter();
    /* access modifiers changed from: private */
    public boolean isWifiConnected = false;
    /* access modifiers changed from: private */
    public int lastWifiSwitchState = 4;
    private int mAppScenceId = -1;
    private BroadcastReceiver mBroadcastReceiver = new WifiBroadcastReceiver();
    private HwAPPChrExcpReport mChrExcpReport = null;
    private LogCollectManager mCollectManger = null;
    /* access modifiers changed from: private */
    public Context mContext;
    private HwAPPQoEDataBase mDataManger = null;
    private SQLiteDatabase mDatabase = null;
    private boolean mIsDefaultRadicalUser = false;
    private final Object mSqlLock = new Object();
    /* access modifiers changed from: private */
    public int recentWifiRssi = -127;

    private HwAPPQoEUserAction(Context context) {
        this.mContext = context;
        this.mIsDefaultRadicalUser = isDefaultRadicalUser();
        registerBroadcastReceiver();
        this.mCollectManger = new LogCollectManager(context);
        this.mChrExcpReport = HwAPPChrExcpReport.getInstance();
        try {
            this.mDataManger = new HwAPPQoEDataBase(context);
            this.mDatabase = this.mDataManger.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            this.mDatabase = null;
            HwAPPQoEUtils.logD(TAG, false, "Init Error:%{public}s", e.getMessage());
        }
    }

    public static HwAPPQoEUserAction createHwAPPQoEUserAction(Context context) {
        if (mUserAction == null) {
            mUserAction = new HwAPPQoEUserAction(context);
        }
        return mUserAction;
    }

    public static HwAPPQoEUserAction getInstance() {
        return mUserAction;
    }

    private void registerBroadcastReceiver() {
        this.intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED);
        this.intentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter);
    }

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        private WifiBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            WifiInfo wifiInfo;
            if (intent == null) {
                HwAPPQoEUtils.logE(HwAPPQoEUserAction.TAG, false, "intent is null", new Object[0]);
                return;
            }
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                int wifistatus = intent.getIntExtra("wifi_state", 4);
                if (wifistatus == 1) {
                    if (1 != HwAPPQoEUserAction.this.lastWifiSwitchState) {
                        HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "WIFI_STATE_DISABLED:%{public}d", Integer.valueOf(wifistatus));
                        WifiSwitchChangeInfo unused = HwAPPQoEUserAction.this.currWifiDisableInfo = new WifiSwitchChangeInfo(wifistatus);
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiDisconnTime = HwAPPQoEUserAction.this.curWifiStateInfo.wifiDisconnTime;
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiSSID = HwAPPQoEUserAction.this.curWifiStateInfo.wifiSSID;
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiRssi = HwAPPQoEUserAction.this.curWifiStateInfo.wifiRssi;
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiFreq = HwAPPQoEUserAction.this.curWifiStateInfo.wifiFreq;
                        HwAPPQoEUserAction.this.reportExceptionInfo();
                    }
                    int unused2 = HwAPPQoEUserAction.this.lastWifiSwitchState = wifistatus;
                } else if (wifistatus == 3) {
                    HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "WIFI_STATE_ENABLED:%{public}d", Integer.valueOf(wifistatus));
                    int unused3 = HwAPPQoEUserAction.this.lastWifiSwitchState = wifistatus;
                }
            } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED.equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netInfo == null) {
                    HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "NETWORK_STATE_CHANGED_ACTION, netInfo is null.", new Object[0]);
                    return;
                }
                HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "NETWORK_STATE_CHANGED_ACTION:%{public}s", netInfo.getState());
                if (netInfo.getState() == NetworkInfo.State.CONNECTED) {
                    WifiManager mWifiManager = (WifiManager) HwAPPQoEUserAction.this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
                    if (mWifiManager != null && (wifiInfo = mWifiManager.getConnectionInfo()) != null && !HwAPPQoEUserAction.this.isWifiConnected) {
                        WifiStateChangeInfo unused4 = HwAPPQoEUserAction.this.curWifiStateInfo = new WifiStateChangeInfo();
                        HwAPPQoEUserAction.this.curWifiStateInfo.wifiSSID = wifiInfo.getSSID();
                        HwAPPQoEUserAction.this.curWifiStateInfo.wifiConnTime = System.currentTimeMillis();
                        HwAPPQoEUserAction.this.curWifiStateInfo.wifiFreq = wifiInfo.getFrequency();
                        boolean unused5 = HwAPPQoEUserAction.this.isWifiConnected = true;
                        HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "CONNECTED:%{public}s", String.valueOf(HwAPPQoEUserAction.this.curWifiStateInfo.wifiConnTime));
                    }
                } else if (netInfo.getState() == NetworkInfo.State.DISCONNECTED && HwAPPQoEUserAction.this.curWifiStateInfo.wifiSSID != null && HwAPPQoEUserAction.this.isWifiConnected) {
                    HwAPPQoEUserAction.this.curWifiStateInfo.wifiDisconnTime = System.currentTimeMillis();
                    HwAPPQoEUserAction.this.curWifiStateInfo.wifiRssi = HwAPPQoEUserAction.this.recentWifiRssi;
                    boolean unused6 = HwAPPQoEUserAction.this.isWifiConnected = false;
                    HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "DISCONNECTED:%{public}s", String.valueOf(HwAPPQoEUserAction.this.curWifiStateInfo.wifiDisconnTime));
                }
            } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                int unused7 = HwAPPQoEUserAction.this.recentWifiRssi = intent.getIntExtra("newRssi", -127);
                HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, false, "RSSI_CHANGE:%{public}d", Integer.valueOf(HwAPPQoEUserAction.this.recentWifiRssi));
            }
        }
    }

    public void notifyAPPStateChange(long apkStartTime, long apkEndTime, int appId) {
        if (apkStartTime - 10000 <= this.currWifiDisableInfo.wifiSwitchTime && apkEndTime >= this.currWifiDisableInfo.wifiSwitchTime && this.currWifiDisableInfo.wifiSwitchState == 1 && this.currWifiDisableInfo.wifiSwitchTime - this.currWifiDisableInfo.wifiState.wifiDisconnTime <= 1000 && HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.currWifiDisableInfo.wifiState.wifiFreq, this.currWifiDisableInfo.wifiState.wifiRssi) > 1) {
            HwAPPQoEUtils.logD(TAG, false, "notifyAPPStateChange, wifi signal was better than threshold, RADICAL", new Object[0]);
            updateUserActionData(2, appId, this.currWifiDisableInfo.wifiState.wifiSSID);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00f0  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x015b  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x01ab  */
    public void updateUserActionData(int userType, int appId, String wifiSSID) {
        int newCommonCnt = 0;
        int newRadicalCnt = 0;
        int cachedCommonCnt = 0;
        int cachedRadicalCnt = 0;
        boolean isEntryFound = false;
        HwAPPQoEUtils.logD(TAG, false, "updateUserActionData wifiSSID :%{private}s appId = %{public}d userType = %{public}d", wifiSSID, Integer.valueOf(appId), Integer.valueOf(userType));
        String cardInfo = getCurrentDefaultDataImsi();
        synchronized (this.mSqlLock) {
            try {
                if (this.mDatabase != null && this.mDatabase.isOpen() && cardInfo != null) {
                    if (wifiSSID != null) {
                        Cursor c = null;
                        try {
                            c = this.mDatabase.rawQuery("SELECT * FROM APPQoEUserAction where (wifiSSID like ?) and (cardInfo = ?) and (appId = ?)", new String[]{wifiSSID, cardInfo, String.valueOf(appId)});
                            if (c.getCount() > 0 && c.moveToNext()) {
                                cachedCommonCnt = c.getInt(c.getColumnIndex("commonCnt"));
                                cachedRadicalCnt = c.getInt(c.getColumnIndex("radicalCnt"));
                                isEntryFound = true;
                                HwAPPQoEUtils.logD(TAG, false, "updateUserActionData cachedCommonCnt :%{public}d, cachedRadicalCnt:%{public}d", Integer.valueOf(cachedCommonCnt), Integer.valueOf(cachedRadicalCnt));
                            }
                            if (1 == userType) {
                                newCommonCnt = cachedCommonCnt + 1;
                                newRadicalCnt = 0;
                            } else {
                                newCommonCnt = 0;
                                newRadicalCnt = cachedRadicalCnt + 1;
                            }
                            try {
                                HwAPPQoEUtils.logD(TAG, false, "updateUserActionData newRadicalCnt :%{public}d, newCommonCnt:%{public}d", Integer.valueOf(newRadicalCnt), Integer.valueOf(newCommonCnt));
                                try {
                                    c.close();
                                } catch (Throwable th) {
                                    th = th;
                                }
                            } catch (SQLException e) {
                                e = e;
                            } catch (Throwable th2) {
                                th = th2;
                                if (c != null) {
                                }
                                throw th;
                            }
                        } catch (SQLException e2) {
                            e = e2;
                            try {
                                Object[] objArr = new Object[1];
                                try {
                                    objArr[0] = e.getMessage();
                                    HwAPPQoEUtils.logD(TAG, false, "updateUserActionData error:%{public}s", objArr);
                                    if (c != null) {
                                        try {
                                            c.close();
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                    newCommonCnt = newCommonCnt;
                                    if (!isEntryFound) {
                                    }
                                    return;
                                } catch (Throwable th4) {
                                    th = th4;
                                    if (c != null) {
                                        c.close();
                                    }
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                if (c != null) {
                                }
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            if (c != null) {
                            }
                            throw th;
                        }
                        if (!isEntryFound) {
                            HwAPPQoEUtils.logD(TAG, false, "updateUserActionData userType :%{public}d, newCommonCnt:%{public}d, newRadicalCnt:%{public}d", Integer.valueOf(userType), Integer.valueOf(newCommonCnt), Integer.valueOf(newRadicalCnt));
                            ContentValues values = new ContentValues();
                            values.put("appId", Integer.valueOf(appId));
                            values.put("wifiSSID", wifiSSID);
                            values.put("cardInfo", cardInfo);
                            values.put("commonCnt", Integer.valueOf(newCommonCnt));
                            values.put("radicalCnt", Integer.valueOf(newRadicalCnt));
                            this.mDatabase.update(HwAPPQoEDataBase.TABLE_USER_ACTION, values, " (wifiSSID like ?) and (cardInfo = ?) and (appId = ?)", new String[]{wifiSSID, cardInfo, String.valueOf(appId)});
                        } else {
                            HwAPPQoEUtils.logD(TAG, false, "insert userType :%{public}d, newCommonCnt:%{public}d, newRadicalCnt:%{public}d", Integer.valueOf(userType), Integer.valueOf(newCommonCnt), Integer.valueOf(newRadicalCnt));
                            this.mDatabase.execSQL("INSERT INTO APPQoEUserAction VALUES(null, ?, ?, ?, ?, ?)", new Object[]{Integer.valueOf(appId), wifiSSID, cardInfo, Integer.valueOf(newCommonCnt), Integer.valueOf(newRadicalCnt)});
                        }
                        return;
                    }
                }
                HwAPPQoEUtils.logD(TAG, false, "database invalid when update data.", new Object[0]);
            } catch (Throwable th7) {
                th = th7;
                throw th;
            }
        }
    }

    public int getUserActionType(int appId) {
        int userType;
        synchronized (this.mSqlLock) {
            int number = queryUserActionData(this.curWifiStateInfo.wifiSSID, appId, getCurrentDefaultDataImsi());
            if (this.mIsDefaultRadicalUser) {
                if (number >= 3) {
                    userType = 1;
                } else {
                    userType = 2;
                }
            } else if (number >= 3) {
                userType = 2;
            } else {
                userType = 1;
            }
        }
        return userType;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:26:0x006f, code lost:
        if (0 == 0) goto L_0x0072;
     */
    private int queryUserActionData(String wifiSSID, int appId, String cardInfo) {
        int number = 0;
        synchronized (this.mSqlLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || cardInfo == null || wifiSSID == null) {
                HwAPPQoEUtils.logD(TAG, false, "database invalid when get action data.", new Object[0]);
                return 0;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM APPQoEUserAction where (wifiSSID like ?) and (cardInfo = ?) and (appId = ?)", new String[]{wifiSSID, cardInfo, String.valueOf(appId)});
                if (c.getCount() > 0 && c.moveToNext()) {
                    number = this.mIsDefaultRadicalUser ? c.getInt(c.getColumnIndex("commonCnt")) : c.getInt(c.getColumnIndex("radicalCnt"));
                }
            } catch (SQLException e) {
                HwAPPQoEUtils.logD(TAG, false, "queryUserActionData error:%{public}s", e.getMessage());
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
            c.close();
            return number;
        }
    }

    private String getCurrentDefaultDataImsi() {
        TelephonyManager mTelephonyManager;
        if (this.mCollectManger == null || (mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone")) == null) {
            return null;
        }
        String imsi = mTelephonyManager.getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (imsi == null) {
            return imsi;
        }
        try {
            return this.mCollectManger.doEncrypt(imsi);
        } catch (RemoteException ex) {
            HwAPPQoEUtils.logD(TAG, false, "get card info error:%{public}s", ex.getMessage());
            return imsi;
        }
    }

    public void setLatestAPPScenceId(int appScenceId) {
        this.mAppScenceId = appScenceId;
    }

    /* access modifiers changed from: private */
    public void reportExceptionInfo() {
        HwAPPQoEUtils.logD(TAG, false, "reportExceptionInfo, mAppScenceId:%{public}d", Integer.valueOf(this.mAppScenceId));
        if (this.mAppScenceId > 0 && this.mChrExcpReport != null) {
            HwAPPQoEManager appQoeManager = HwAPPQoEManager.getInstance();
            if ((appQoeManager == null || this.mAppScenceId == appQoeManager.getCurAPPStateInfo().mScenceId) && System.currentTimeMillis() - this.currWifiDisableInfo.wifiState.wifiDisconnTime < 1000) {
                this.mChrExcpReport.reportAPPQoExcpInfo(1, this.mAppScenceId);
            }
        }
    }

    public void resetUserActionType(int appId) {
        if (this.curWifiStateInfo.wifiSSID != null && !HwAPPQoEUtils.INVALID_STRING_VALUE.equals(this.curWifiStateInfo.wifiSSID)) {
            updateUserActionData(1, appId, this.curWifiStateInfo.wifiSSID);
        }
    }

    public static class WifiStateChangeInfo {
        public long wifiConnTime = -1;
        public long wifiDisconnTime = -1;
        public int wifiFreq = -1;
        public int wifiRssi = -1;
        public String wifiSSID = HwAPPQoEUtils.INVALID_STRING_VALUE;

        public String toString() {
            return "WifiStateChangeInfo:" + this.wifiConnTime + "," + this.wifiDisconnTime + "," + this.wifiSSID + "," + this.wifiRssi + "," + this.wifiFreq;
        }
    }

    public static class WifiSwitchChangeInfo {
        public WifiStateChangeInfo wifiState = new WifiStateChangeInfo();
        public int wifiSwitchState;
        public long wifiSwitchTime = System.currentTimeMillis();

        public WifiSwitchChangeInfo(int state) {
            this.wifiSwitchState = state;
        }
    }

    private boolean isDefaultRadicalUser() {
        String chipset = SystemProperties.get(CHIPSET_TYPE_PROP, DEFAULT_CHIP_TYPE);
        HwAPPQoEUtils.logD(TAG, false, "isDefaultRadicalUser, chipset = %{public}s", chipset);
        return chipset != null && (chipset.contains(BCM_CHIP_4345) || chipset.contains(BCM_CHIP_4359) || chipset.contains(HISI_CHIP_1103) || chipset.contains(HISI_CHIP_1105));
    }
}
