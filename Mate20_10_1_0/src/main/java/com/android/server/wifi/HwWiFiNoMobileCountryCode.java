package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import java.util.ArrayList;
import java.util.List;

public class HwWiFiNoMobileCountryCode {
    private static final String ACTION_WIFI_COUNTRY_CODE = "com.android.net.wifi.countryCode";
    private static final int COUNTRY_CODE_EID = 7;
    private static final int COUNTRY_CODE_LENGTH = 2;
    private static final String COUNTRY_CODE_NO_MOBILE = "ZZ";
    private static final int COUNTRY_CODE_OFFSET = 0;
    private static final String EXTRA_FORCE_SET_WIFI_CCODE = "isWifiConnected";
    private static final int FEATURE_WIFI_CCODE_AP_AROUND = 2;
    private static final int FEATURE_WIFI_CCODE_AP_CONNECTED = 4;
    private static final int FEATURE_WIFI_CCODE_PASSIVE_SCAN = 1;
    private static final String HW_SYSTEM_PERMISSION = "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM";
    private static final String HW_VSIM_SERVICE_STATE_CHANGED = "com.huawei.vsim.action.VSIM_REG_PLMN_CHANGED";
    private static final int OPERATOR_NUMERIC_LENGTH = 5;
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final String PROPERTY_GLOBAL_WIFI_ONLY = "ro.radio.noril";
    private static final int SCAN_RESULTS_MIN_NUMBER = 3;
    private static final int SUBSCRIPTION_ID = 0;
    private static final String TAG = "HwWiFiNoMobileCountryCode";
    private static final String WHITE_CARD_NETWORK_MCCMNC = "00101";
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public String mCountryCodeFromConnectionInfo = null;
    private String mCountryCodeFromMonitor = null;
    /* access modifiers changed from: private */
    public String mCountryCodeFromScanResults = null;
    private CountryCodeReceiver mCountryCodeReceiver;
    /* access modifiers changed from: private */
    public boolean mIsConnected = false;
    /* access modifiers changed from: private */
    public int mWifiCountryCodeConf = SystemProperties.getInt("ro.config.wifi_ccode_no_mcc", 0);
    /* access modifiers changed from: private */
    public WifiManager mWifiMgr = null;

    public HwWiFiNoMobileCountryCode(Context context) {
        if (context != null) {
            this.mContext = context;
            this.mCountryCodeReceiver = new CountryCodeReceiver();
            IntentFilter myFilter = new IntentFilter();
            if (this.mWifiCountryCodeConf != 0) {
                myFilter.addAction("android.intent.action.AIRPLANE_MODE");
                myFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                myFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            }
            if ((this.mWifiCountryCodeConf & 1) != 0) {
                this.mCountryCodeFromMonitor = COUNTRY_CODE_NO_MOBILE;
            }
            if ((this.mWifiCountryCodeConf & 2) != 0) {
                myFilter.addAction("android.net.wifi.SCAN_RESULTS");
            }
            if ((this.mWifiCountryCodeConf & 4) != 0) {
                myFilter.addAction("android.net.wifi.STATE_CHANGE");
            }
            this.mContext.registerReceiver(this.mCountryCodeReceiver, myFilter);
            return;
        }
        HwHiLog.e(TAG, false, "HwWiFiNoMobileCountryCode context is null", new Object[0]);
    }

    private class CountryCodeReceiver extends BroadcastReceiver {
        private CountryCodeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (HwWiFiNoMobileCountryCode.this.mWifiMgr == null) {
                    WifiManager unused = HwWiFiNoMobileCountryCode.this.mWifiMgr = (WifiManager) context.getSystemService("wifi");
                }
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction()) || "android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction()) || HwWiFiNoMobileCountryCode.HW_VSIM_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
                    if (!HwWiFiNoMobileCountryCode.this.isMobilePhoneNoService()) {
                        String unused2 = HwWiFiNoMobileCountryCode.this.mCountryCodeFromScanResults = null;
                        String unused3 = HwWiFiNoMobileCountryCode.this.mCountryCodeFromConnectionInfo = null;
                    }
                    Intent intentCountryCode = new Intent(HwWiFiNoMobileCountryCode.ACTION_WIFI_COUNTRY_CODE);
                    intentCountryCode.putExtra(HwWiFiNoMobileCountryCode.EXTRA_FORCE_SET_WIFI_CCODE, HwWiFiNoMobileCountryCode.this.mIsConnected);
                    HwWiFiNoMobileCountryCode.this.mContext.sendBroadcast(intentCountryCode, HwWiFiNoMobileCountryCode.HW_SYSTEM_PERMISSION);
                } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())) {
                    if (intent.getIntExtra("wifi_state", 4) == 3) {
                        HwWiFiNoMobileCountryCode.this.mWifiMgr.setCountryCode(HwWiFiNoMobileCountryCode.COUNTRY_CODE_NO_MOBILE);
                    }
                } else if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction())) {
                    if ((HwWiFiNoMobileCountryCode.this.mWifiCountryCodeConf & 2) == 0 || !HwWiFiNoMobileCountryCode.this.isMobilePhoneNoService()) {
                        String unused4 = HwWiFiNoMobileCountryCode.this.mCountryCodeFromScanResults = null;
                        return;
                    }
                    String countryCode = HwWiFiNoMobileCountryCode.this.getCountryCodeFromScanResults(HwWiFiNoMobileCountryCode.this.mWifiMgr.getScanResults());
                    if (!TextUtils.isEmpty(countryCode) && !countryCode.equalsIgnoreCase(HwWiFiNoMobileCountryCode.this.mCountryCodeFromScanResults)) {
                        String unused5 = HwWiFiNoMobileCountryCode.this.mCountryCodeFromScanResults = countryCode;
                        HwWiFiNoMobileCountryCode.this.mWifiMgr.setCountryCode(HwWiFiNoMobileCountryCode.this.mCountryCodeFromScanResults);
                    }
                } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    HwWiFiNoMobileCountryCode.this.handleNetworkStateChangeAction(intent);
                } else {
                    HwHiLog.d(HwWiFiNoMobileCountryCode.TAG, false, "Do not process the broadcast", new Object[0]);
                }
            }
        }
    }

    public String getCountryCodeInNoMobileScene() {
        if (!isMobilePhoneNoService()) {
            return null;
        }
        if (this.mIsConnected && !TextUtils.isEmpty(this.mCountryCodeFromConnectionInfo)) {
            return this.mCountryCodeFromConnectionInfo;
        }
        if (!TextUtils.isEmpty(this.mCountryCodeFromScanResults)) {
            return this.mCountryCodeFromScanResults;
        }
        return this.mCountryCodeFromMonitor;
    }

    /* access modifiers changed from: private */
    public boolean isMobilePhoneNoService() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0) {
            return true;
        }
        String registerOperator = TelephonyManager.from(this.mContext).getNetworkOperator(0);
        return TextUtils.isEmpty(registerOperator) || registerOperator.length() < 5 || WHITE_CARD_NETWORK_MCCMNC.equals(registerOperator) || SystemProperties.getBoolean(PROPERTY_GLOBAL_WIFI_ONLY, false);
    }

    private String parseCountryCodeElement(ScanResult.InformationElement[] ies) {
        if (ies == null) {
            return null;
        }
        for (ScanResult.InformationElement ie : ies) {
            if (ie.id == 7 && ie.bytes.length >= 2) {
                return new String(ie.bytes, 0, 2);
            }
        }
        return null;
    }

    private String getCountryCode(List<String> countryCodeLists) {
        String countryCode = null;
        int listSize = countryCodeLists.size();
        if (listSize < 3) {
            return null;
        }
        for (int i = 0; i < listSize - 1; i++) {
            int count = 1;
            String countryCode2 = countryCodeLists.get(i);
            for (int j = listSize - 1; j > i; j--) {
                if (!TextUtils.isEmpty(countryCode2) && countryCode2.equalsIgnoreCase(countryCodeLists.get(j))) {
                    count++;
                }
            }
            if (count * 2 > listSize) {
                return countryCode2;
            }
            countryCode = "";
        }
        return countryCode;
    }

    /* access modifiers changed from: private */
    public String getCountryCodeFromScanResults(List<ScanResult> scanLists) {
        if (scanLists == null) {
            return null;
        }
        List<String> countryCodeLists = new ArrayList<>();
        for (ScanResult entry : scanLists) {
            String countryCode = parseCountryCodeElement(entry.informationElements);
            if (!TextUtils.isEmpty(countryCode)) {
                countryCodeLists.add(countryCode);
            }
        }
        return getCountryCode(countryCodeLists);
    }

    private String getCountryCodeFromConnectionInfo(List<ScanResult> scanLists, WifiInfo wifiInfo) {
        if (scanLists == null || wifiInfo == null) {
            return null;
        }
        String currentBssid = wifiInfo.getBSSID();
        if (TextUtils.isEmpty(currentBssid)) {
            return null;
        }
        for (ScanResult entry : scanLists) {
            if (currentBssid.equalsIgnoreCase(entry.BSSID)) {
                return parseCountryCodeElement(entry.informationElements);
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void handleNetworkStateChangeAction(Intent intent) {
        if ((this.mWifiCountryCodeConf & 4) == 0 || !isMobilePhoneNoService()) {
            this.mCountryCodeFromConnectionInfo = null;
            return;
        }
        NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        if (netInfo == null || !netInfo.isConnected()) {
            this.mIsConnected = false;
            return;
        }
        this.mIsConnected = true;
        String countryCode = getCountryCodeFromConnectionInfo(this.mWifiMgr.getScanResults(), this.mWifiMgr.getConnectionInfo());
        if (!TextUtils.isEmpty(countryCode) && !countryCode.equalsIgnoreCase(this.mCountryCodeFromConnectionInfo)) {
            this.mCountryCodeFromConnectionInfo = countryCode;
            Intent intentCountryCode = new Intent(ACTION_WIFI_COUNTRY_CODE);
            intentCountryCode.putExtra(EXTRA_FORCE_SET_WIFI_CCODE, true);
            this.mContext.sendBroadcast(intentCountryCode, HW_SYSTEM_PERMISSION);
        }
    }
}
