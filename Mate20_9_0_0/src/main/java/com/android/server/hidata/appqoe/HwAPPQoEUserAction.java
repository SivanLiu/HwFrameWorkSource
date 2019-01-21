package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.lcagent.client.LogCollectManager;

public class HwAPPQoEUserAction {
    public static final String CHIPSET_TYPE_PROP = "ro.connectivity.sub_chiptype";
    private static final int INITIAL_USER_TYPE_CNT = 0;
    private static final String TAG = "HiData_HwAPPQoEUserAction";
    private static final int USER_TYPE_CNT_THRESHOLD = 3;
    private static final int WIFI_BAD_SIGNAL_THRESHOLD = 1;
    private static final int WIFI_DISABLE_TIME_ADVANCED = 1000;
    private static final int WIFI_DISABLE_TIME_DELTA = 10000;
    private static HwAPPQoEUserAction mUserAction = null;
    private WifiStateChangeInfo curWifiStateInfo = new WifiStateChangeInfo();
    private WifiSwitchChangeInfo currWifiDisableInfo = new WifiSwitchChangeInfo(-1);
    private IntentFilter intentFilter = new IntentFilter();
    private boolean isWifiConnected = false;
    private int lastWifiSwitchState = 4;
    private int mAppScenceId = -1;
    private BroadcastReceiver mBroadcastReceiver = new WifiBroadcastReceiver();
    private HwAPPChrExcpReport mChrExcpReport = null;
    private LogCollectManager mCollectManger = null;
    private Context mContext;
    private HwAPPQoEDataBase mDataManger = null;
    private SQLiteDatabase mDatabase = null;
    private boolean mIsDefaultRadicalUser = false;
    private Object mSqlLock = new Object();
    private int recentWifiRssi = -127;

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        private WifiBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str;
            StringBuilder stringBuilder;
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                int wifistatus = intent.getIntExtra("wifi_state", 4);
                if (wifistatus == 1) {
                    if (1 != HwAPPQoEUserAction.this.lastWifiSwitchState) {
                        str = HwAPPQoEUserAction.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WIFI_STATE_DISABLED:");
                        stringBuilder.append(wifistatus);
                        HwAPPQoEUtils.logD(str, stringBuilder.toString());
                        HwAPPQoEUserAction.this.currWifiDisableInfo = new WifiSwitchChangeInfo(wifistatus);
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiDisconnTime = HwAPPQoEUserAction.this.curWifiStateInfo.wifiDisconnTime;
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiSSID = HwAPPQoEUserAction.this.curWifiStateInfo.wifiSSID;
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiRssi = HwAPPQoEUserAction.this.curWifiStateInfo.wifiRssi;
                        HwAPPQoEUserAction.this.currWifiDisableInfo.wifiState.wifiFreq = HwAPPQoEUserAction.this.curWifiStateInfo.wifiFreq;
                        HwAPPQoEUserAction.this.reportExceptionInfo();
                    }
                    HwAPPQoEUserAction.this.lastWifiSwitchState = wifistatus;
                } else if (wifistatus == 3) {
                    str = HwAPPQoEUserAction.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("WIFI_STATE_ENABLED:");
                    stringBuilder.append(wifistatus);
                    HwAPPQoEUtils.logD(str, stringBuilder.toString());
                    HwAPPQoEUserAction.this.lastWifiSwitchState = wifistatus;
                }
            } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netInfo == null) {
                    HwAPPQoEUtils.logD(HwAPPQoEUserAction.TAG, "NETWORK_STATE_CHANGED_ACTION, netInfo is null.");
                    return;
                }
                String str2 = HwAPPQoEUserAction.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("NETWORK_STATE_CHANGED_ACTION:");
                stringBuilder2.append(netInfo.getState());
                HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
                if (netInfo.getState() == State.CONNECTED) {
                    WifiManager mWifiManager = (WifiManager) HwAPPQoEUserAction.this.mContext.getSystemService("wifi");
                    if (mWifiManager != null) {
                        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                        if (!(wifiInfo == null || HwAPPQoEUserAction.this.isWifiConnected)) {
                            HwAPPQoEUserAction.this.curWifiStateInfo = new WifiStateChangeInfo();
                            HwAPPQoEUserAction.this.curWifiStateInfo.wifiSSID = wifiInfo.getSSID();
                            HwAPPQoEUserAction.this.curWifiStateInfo.wifiConnTime = System.currentTimeMillis();
                            HwAPPQoEUserAction.this.curWifiStateInfo.wifiFreq = wifiInfo.getFrequency();
                            HwAPPQoEUserAction.this.isWifiConnected = true;
                            str = HwAPPQoEUserAction.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("CONNECTED:");
                            stringBuilder3.append(HwAPPQoEUserAction.this.curWifiStateInfo.wifiConnTime);
                            HwAPPQoEUtils.logD(str, stringBuilder3.toString());
                        }
                    }
                } else if (netInfo.getState() == State.DISCONNECTED && HwAPPQoEUserAction.this.curWifiStateInfo.wifiSSID != null && HwAPPQoEUserAction.this.isWifiConnected) {
                    HwAPPQoEUserAction.this.curWifiStateInfo.wifiDisconnTime = System.currentTimeMillis();
                    HwAPPQoEUserAction.this.curWifiStateInfo.wifiRssi = HwAPPQoEUserAction.this.recentWifiRssi;
                    HwAPPQoEUserAction.this.isWifiConnected = false;
                    str = HwAPPQoEUserAction.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DISCONNECTED:");
                    stringBuilder.append(HwAPPQoEUserAction.this.curWifiStateInfo.wifiDisconnTime);
                    HwAPPQoEUtils.logD(str, stringBuilder.toString());
                }
            } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                HwAPPQoEUserAction.this.recentWifiRssi = intent.getIntExtra("newRssi", -127);
                String str3 = HwAPPQoEUserAction.TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("RSSI_CHANGE:");
                stringBuilder4.append(HwAPPQoEUserAction.this.recentWifiRssi);
                HwAPPQoEUtils.logD(str3, stringBuilder4.toString());
            }
        }
    }

    public static class WifiStateChangeInfo {
        public long wifiConnTime = -1;
        public long wifiDisconnTime = -1;
        public int wifiFreq = -1;
        public int wifiRssi = -1;
        public String wifiSSID = HwAPPQoEUtils.INVALID_STRING_VALUE;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WifiStateChangeInfo:");
            stringBuilder.append(this.wifiConnTime);
            stringBuilder.append(",");
            stringBuilder.append(this.wifiDisconnTime);
            stringBuilder.append(",");
            stringBuilder.append(this.wifiSSID);
            stringBuilder.append(",");
            stringBuilder.append(this.wifiRssi);
            stringBuilder.append(",");
            stringBuilder.append(this.wifiFreq);
            return stringBuilder.toString();
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Init Error:");
            stringBuilder.append(e);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
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
        this.intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.intentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter);
    }

    /* JADX WARNING: Missing block: B:12:0x0056, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:13:0x0057, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyAPPStateChange(long apkStartTime, long apkEndTime, int appId) {
        if (apkStartTime - MemoryConstant.MIN_INTERVAL_OP_TIMEOUT <= this.currWifiDisableInfo.wifiSwitchTime && apkEndTime >= this.currWifiDisableInfo.wifiSwitchTime && this.currWifiDisableInfo.wifiSwitchState == 1 && this.currWifiDisableInfo.wifiSwitchTime - this.currWifiDisableInfo.wifiState.wifiDisconnTime <= 1000 && HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.currWifiDisableInfo.wifiState.wifiFreq, this.currWifiDisableInfo.wifiState.wifiRssi) > 1) {
            HwAPPQoEUtils.logD(TAG, "notifyAPPStateChange, wifi signal was better than threshold, RADICAL");
            updateUserActionData(2, appId, this.currWifiDisableInfo.wifiState.wifiSSID);
        }
    }

    /* JADX WARNING: Missing block: B:22:0x00da, code skipped:
            if (r12 != null) goto L_0x00dc;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            r12.close();
     */
    /* JADX WARNING: Missing block: B:29:0x00fb, code skipped:
            if (r12 == null) goto L_0x00fe;
     */
    /* JADX WARNING: Missing block: B:30:0x00fe, code skipped:
            if (r8 == false) goto L_0x0175;
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            r0 = TAG;
            r13 = new java.lang.StringBuilder();
            r13.append("updateUserActionData userType :");
            r13.append(r2);
            r13.append(", newCommonCnt:");
            r13.append(r4);
            r13.append(", newRadicalCnt:");
            r13.append(r5);
            com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(r0, r13.toString());
            r0 = new android.content.ContentValues();
            r0.put("appId", java.lang.Integer.valueOf(r23));
            r0.put("wifiSSID", r3);
            r0.put("cardInfo", r9);
            r0.put("commonCnt", java.lang.Integer.valueOf(r4));
            r0.put("radicalCnt", java.lang.Integer.valueOf(r5));
     */
    /* JADX WARNING: Missing block: B:33:0x0158, code skipped:
            r20 = r6;
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r1.mDatabase.update(com.android.server.hidata.appqoe.HwAPPQoEDataBase.TABLE_USER_ACTION, r0, " (wifiSSID like ?) and (cardInfo = ?) and (appId = ?)", new java.lang.String[]{r3, r9, java.lang.String.valueOf(r23)});
     */
    /* JADX WARNING: Missing block: B:36:0x0171, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:37:0x0172, code skipped:
            r20 = r6;
     */
    /* JADX WARNING: Missing block: B:38:0x0175, code skipped:
            r20 = r6;
            r0 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("insert userType :");
            r6.append(r2);
            r6.append(", newCommonCnt:");
            r6.append(r4);
            r6.append(", newRadicalCnt:");
            r6.append(r5);
            com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(r0, r6.toString());
            r1.mDatabase.execSQL("INSERT INTO APPQoEUserAction VALUES(null, ?, ?, ?, ?, ?)", new java.lang.Object[]{java.lang.Integer.valueOf(r23), r3, r9, java.lang.Integer.valueOf(r4), java.lang.Integer.valueOf(r5)});
     */
    /* JADX WARNING: Missing block: B:40:0x01c3, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:41:0x01c4, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:42:0x01c5, code skipped:
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:52:0x01d9, code skipped:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateUserActionData(int userType, int appId, String wifiSSID) {
        int i = userType;
        String str = wifiSSID;
        int newCommonCnt = 0;
        int newRadicalCnt = 0;
        int cachedCommonCnt = 0;
        int cachedRadicalCnt = 0;
        boolean isEntryFound = false;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateUserActionData wifiSSID :");
        stringBuilder.append(str);
        stringBuilder.append(" appId = ");
        stringBuilder.append(appId);
        stringBuilder.append(" userType = ");
        stringBuilder.append(i);
        HwAPPQoEUtils.logD(str2, stringBuilder.toString());
        String cardInfo = getCurrentDefaultDataImsi();
        synchronized (this.mSqlLock) {
            Cursor c;
            try {
                if (this.mDatabase == null || !this.mDatabase.isOpen() || cardInfo == null || str == null) {
                    HwAPPQoEUtils.logD(TAG, "database invalid when update data.");
                    return;
                }
                c = null;
                try {
                    StringBuilder stringBuilder2;
                    int newRadicalCnt2;
                    c = this.mDatabase.rawQuery("SELECT * FROM APPQoEUserAction where (wifiSSID like ?) and (cardInfo = ?) and (appId = ?)", new String[]{str, cardInfo, String.valueOf(appId)});
                    if (c.getCount() > 0 && c.moveToNext()) {
                        cachedCommonCnt = c.getInt(c.getColumnIndex("commonCnt"));
                        cachedRadicalCnt = c.getInt(c.getColumnIndex("radicalCnt"));
                        isEntryFound = true;
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateUserActionData cachedCommonCnt :");
                        stringBuilder2.append(cachedCommonCnt);
                        stringBuilder2.append(", cachedRadicalCnt:");
                        stringBuilder2.append(cachedRadicalCnt);
                        HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
                    }
                    if (1 == i) {
                        newRadicalCnt2 = 0;
                        cachedCommonCnt++;
                        newCommonCnt = cachedCommonCnt;
                    } else {
                        newRadicalCnt2 = cachedRadicalCnt + 1;
                        newCommonCnt = 0;
                    }
                    newRadicalCnt = newRadicalCnt2;
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateUserActionData newRadicalCnt :");
                    stringBuilder2.append(newRadicalCnt);
                    stringBuilder2.append(", newCommonCnt:");
                    stringBuilder2.append(newCommonCnt);
                    HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
                } catch (SQLException e) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("updateUserActionData error:");
                    stringBuilder3.append(e);
                    HwAPPQoEUtils.logD(str3, stringBuilder3.toString());
                }
            } catch (Throwable th) {
                Throwable th2 = th;
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

    /* JADX WARNING: Missing block: B:20:0x0057, code skipped:
            if (r2 != null) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:27:0x0076, code skipped:
            if (r2 == null) goto L_0x0079;
     */
    /* JADX WARNING: Missing block: B:30:0x007a, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int queryUserActionData(String wifiSSID, int appId, String cardInfo) {
        int number = 0;
        synchronized (this.mSqlLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || cardInfo == null || wifiSSID == null) {
                HwAPPQoEUtils.logD(TAG, "database invalid when get action data.");
                return 0;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM APPQoEUserAction where (wifiSSID like ?) and (cardInfo = ?) and (appId = ?)", new String[]{wifiSSID, cardInfo, String.valueOf(appId)});
                if (c.getCount() > 0 && c.moveToNext()) {
                    number = this.mIsDefaultRadicalUser ? c.getInt(c.getColumnIndex("commonCnt")) : c.getInt(c.getColumnIndex("radicalCnt"));
                }
            } catch (SQLException e) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("queryUserActionData error:");
                    stringBuilder.append(e);
                    HwAPPQoEUtils.logD(str, stringBuilder.toString());
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    private String getCurrentDefaultDataImsi() {
        if (this.mCollectManger == null) {
            return null;
        }
        TelephonyManager mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (mTelephonyManager == null) {
            return null;
        }
        String imsi = mTelephonyManager.getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (imsi != null) {
            try {
                imsi = this.mCollectManger.doEncrypt(imsi);
            } catch (RemoteException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get card info error:");
                stringBuilder.append(ex);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
            }
        }
        return imsi;
    }

    public void setLatestAPPScenceId(int appScenceId) {
        this.mAppScenceId = appScenceId;
    }

    /* JADX WARNING: Missing block: B:8:0x003b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void reportExceptionInfo() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reportExceptionInfo, mAppScenceId:");
        stringBuilder.append(this.mAppScenceId);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (this.mAppScenceId > 0 && this.mChrExcpReport != null && System.currentTimeMillis() - this.currWifiDisableInfo.wifiState.wifiDisconnTime < 1000) {
            this.mChrExcpReport.reportAPPQoExcpInfo(1, this.mAppScenceId);
        }
    }

    public void resetUserActionType(int appId) {
        if (this.curWifiStateInfo.wifiSSID != null && !HwAPPQoEUtils.INVALID_STRING_VALUE.equals(this.curWifiStateInfo.wifiSSID)) {
            updateUserActionData(1, appId, this.curWifiStateInfo.wifiSSID);
        }
    }

    private boolean isDefaultRadicalUser() {
        String chipset = SystemProperties.get(CHIPSET_TYPE_PROP, "none");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isDefaultRadicalUser, chipset = ");
        stringBuilder.append(chipset);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (chipset == null || (!chipset.contains("4345") && !chipset.contains("4359") && !chipset.contains("1103"))) {
            return false;
        }
        return true;
    }
}
