package com.huawei.hwwifiproservice;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class HwDualBandRelationManager {
    private static final String ADD_SPLITER_LEN = "(.{2})";
    private static final int BSSID_COMPARE_LENGTH = 11;
    private static final int BSSID_LENGTH_COMMON = 17;
    private static final int BSSID_LENGTH_SPECIAL = 12;
    private static final int BSSID_OFFSET_BEGIN = 3;
    private static final String BSSID_SPLITTER = "$1:";
    private static final int BSSID_START_INDEX = 0;
    public static final int SECURITY_EAP = 3;
    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_WAPI_CERT = 5;
    public static final int SECURITY_WAPI_PSK = 4;
    public static final int SECURITY_WEP = 1;
    private static HwDualBandRelationManager mHwDualBandRelationManager = null;
    private static boolean mIsWpa3SaeSupported = false;
    private Context mContext;
    private Handler mHandler;
    private HwDualBandInformationManager mHwDualBandInformationManager = null;
    private WifiManager mWifiManager = null;
    private WifiProHistoryRecordManager mWifiProHistoryRecordManager;

    public static HwDualBandRelationManager createInstance(Context context, Handler handler) {
        if (mHwDualBandRelationManager == null) {
            mHwDualBandRelationManager = new HwDualBandRelationManager(context, handler);
        }
        return mHwDualBandRelationManager;
    }

    public static HwDualBandRelationManager getInstance() {
        return mHwDualBandRelationManager;
    }

    private HwDualBandRelationManager(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mWifiProHistoryRecordManager = WifiProHistoryRecordManager.getInstance(this.mContext, this.mWifiManager);
        mIsWpa3SaeSupported = this.mWifiManager.isWpa3SaeSupported();
    }

    public void updateAPRelation() {
        WifiProDualBandApInfoRcd scanAPInfo;
        int isDualBandAP;
        Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation");
        this.mHwDualBandInformationManager = HwDualBandInformationManager.getInstance();
        List<ScanResult> lists = WifiproUtils.getScanResultsFromWsm();
        if (lists == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation lists == null");
            return;
        }
        WifiInfo currentWifiInfo = this.mWifiManager.getConnectionInfo();
        if (currentWifiInfo == null || currentWifiInfo.getBSSID() == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation currentWifiInfo == null");
            return;
        }
        String bssid = currentWifiInfo.getBSSID();
        boolean is5GAP = ScanResult.is5GHz(currentWifiInfo.getFrequency());
        Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation lists.size = " + lists.size());
        if (this.mHwDualBandInformationManager.getDualBandAPInfo(bssid) == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation aPInfo == null");
        } else if (!is5GAP) {
            apRecordManager(currentWifiInfo, bssid, lists);
        } else {
            for (ScanResult result : lists) {
                if ((isAPInConfig(result, bssid) && result.BSSID != null && !result.BSSID.equals(bssid)) && ScanResult.is24GHz(result.frequency) && (scanAPInfo = this.mHwDualBandInformationManager.getDualBandAPInfo(result.BSSID)) != null && scanAPInfo.mServingBand.shortValue() == 1 && scanAPInfo.mInetCapability.shortValue() == 1) {
                    WifiProRelateApRcd relateApRcd = getRelateAPInfo(result.BSSID, bssid);
                    if (relateApRcd == null) {
                        if (isDualBandAP(result.BSSID, bssid)) {
                            isDualBandAP = 1;
                        } else {
                            isDualBandAP = 2;
                        }
                        WifiProRelateApRcd relateApRcd2 = new WifiProRelateApRcd(result.BSSID);
                        relateApRcd2.mRelatedBSSID = bssid;
                        relateApRcd2.mRelateType = isDualBandAP;
                        relateApRcd2.mMaxRelatedRSSI = currentWifiInfo.getRssi();
                        relateApRcd2.mMinCurrentRSSI = result.level;
                        this.mWifiProHistoryRecordManager.saveRelateApInfo(relateApRcd2);
                    } else if (currentWifiInfo.getRssi() > relateApRcd.mMaxRelatedRSSI || relateApRcd.mMaxRelatedRSSI == 0) {
                        Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation max 5G AP relate rssi");
                        relateApRcd.mMaxRelatedRSSI = currentWifiInfo.getRssi();
                        relateApRcd.mMinCurrentRSSI = result.level;
                        this.mWifiProHistoryRecordManager.saveRelateApInfo(relateApRcd);
                    }
                }
            }
        }
    }

    private void apRecordManager(WifiInfo currentWifiInfo, String bssid, List<ScanResult> lists) {
        int isDualBandAP;
        Iterator<ScanResult> it = lists.iterator();
        while (true) {
            boolean configValue = false;
            if (!it.hasNext()) {
                break;
            }
            ScanResult result = it.next();
            boolean bssidValue = result.BSSID != null && !result.BSSID.equals(bssid);
            if (isAPInConfig(result, bssid) && ScanResult.is5GHz(result.frequency)) {
                configValue = true;
            }
            if (bssidValue && configValue) {
                WifiProDualBandApInfoRcd scanAPInfo = this.mHwDualBandInformationManager.getDualBandAPInfo(result.BSSID);
                if (scanAPInfo == null) {
                    Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation ssid = " + StringUtilEx.safeDisplaySsid(result.SSID));
                    this.mHwDualBandInformationManager.saveAPInfo(result, getSecurity(result));
                } else if (isDualBandAP(bssid, result.BSSID) && scanAPInfo.mInetCapability.shortValue() != 1) {
                    Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation update dual band internet capability");
                    scanAPInfo.mInetCapability = 1;
                    this.mWifiProHistoryRecordManager.saveDualBandApInfo(scanAPInfo);
                }
                Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation ssid = " + StringUtilEx.safeDisplaySsid(result.SSID));
                WifiProRelateApRcd relateApRcd = getRelateAPInfo(bssid, result.BSSID);
                if (relateApRcd == null) {
                    if (isDualBandAP(result.BSSID, bssid)) {
                        isDualBandAP = 1;
                    } else {
                        isDualBandAP = 2;
                    }
                    WifiProRelateApRcd relateApRcd2 = new WifiProRelateApRcd(bssid);
                    relateApRcd2.mRelateType = isDualBandAP;
                    relateApRcd2.mRelatedBSSID = result.BSSID;
                    relateApRcd2.mMaxCurrentRSSI = currentWifiInfo.getRssi();
                    relateApRcd2.mMinRelatedRSSI = result.level;
                    Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation add new relation  isDualBandAP = " + isDualBandAP);
                    this.mWifiProHistoryRecordManager.saveRelateApInfo(relateApRcd2);
                } else if (currentWifiInfo.getRssi() > relateApRcd.mMaxCurrentRSSI || relateApRcd.mMaxCurrentRSSI == 0) {
                    Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation 2.4G AP relate rssi");
                    relateApRcd.mMaxCurrentRSSI = currentWifiInfo.getRssi();
                    relateApRcd.mMinRelatedRSSI = result.level;
                    this.mWifiProHistoryRecordManager.saveRelateApInfo(relateApRcd);
                }
            }
        }
        WifiProDualBandApInfoRcd aPInfoRcd = this.mHwDualBandInformationManager.getDualBandAPInfo(bssid);
        if (aPInfoRcd == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation aPInfoRcd == null");
            return;
        }
        Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation aPInfoRcd.getRelateApRcds().size() = " + aPInfoRcd.getRelateApRcds().size());
        if (aPInfoRcd.getRelateApRcds().size() <= 0) {
            return;
        }
        if (aPInfoRcd.getRelateApRcds().size() == 1 && aPInfoRcd.getRelateApRcds().get(0).mRelateType == 1) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation MSG_DUAL_BAND_WIFI_TYPE_SINGLE");
            this.mHandler.sendEmptyMessage(16);
            return;
        }
        Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation MSG_DUAL_BAND_WIFI_TYPE_MIX size() = " + aPInfoRcd.getRelateApRcds().size());
        this.mHandler.sendEmptyMessage(17);
    }

    public List<WifiProRelateApRcd> getRelateAPInfos(String bssid) {
        List<WifiProRelateApRcd> relateApList = new ArrayList<>();
        this.mWifiProHistoryRecordManager.getRelateApList(bssid, relateApList);
        return relateApList;
    }

    public WifiProRelateApRcd getRelateAPInfo(String bssid, String relateAPbssid) {
        WifiProRelateApRcd result = null;
        List<WifiProRelateApRcd> relateAPInfos = getRelateAPInfos(bssid);
        if (relateAPInfos.size() == 0) {
            return null;
        }
        for (WifiProRelateApRcd info : relateAPInfos) {
            if (info.mApBSSID.equals(bssid) && info.mRelatedBSSID.equals(relateAPbssid)) {
                result = info;
            }
        }
        return result;
    }

    public static boolean isDualBandAP(String m24GAP, String m5GAP) {
        if (m24GAP == null || m5GAP == null) {
            return false;
        }
        String bssid24gAp = m24GAP.toLowerCase(Locale.ROOT);
        String bssid5gAp = m5GAP.toLowerCase(Locale.ROOT);
        if ((bssid24gAp.length() == 17 || bssid24gAp.length() == 12) && (bssid5gAp.length() == 17 || bssid5gAp.length() == 12)) {
            if (bssid24gAp.length() == 12) {
                bssid24gAp = bssid24gAp.replaceAll(ADD_SPLITER_LEN, BSSID_SPLITTER).substring(0, 17);
            }
            if (bssid5gAp.length() == 12) {
                bssid5gAp = bssid5gAp.replaceAll(ADD_SPLITER_LEN, BSSID_SPLITTER).substring(0, 17);
            }
            return bssid24gAp.regionMatches(3, bssid5gAp, 3, 11);
        }
        Log.w(HwDualBandMessageUtil.TAG, "illegal bssid format");
        return false;
    }

    private boolean isAPInConfig(ScanResult result, String bssid) {
        if (result.SSID == null || result.SSID.length() <= 0) {
            Log.e(HwDualBandMessageUtil.TAG, "isAPInConfig, result.SSID error!");
            return false;
        }
        int authType = getSecurity(result);
        List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            Log.e(HwDualBandMessageUtil.TAG, "isAPInConfig, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        for (WifiConfiguration nextConfig : configNetworks) {
            boolean bssidValue = nextConfig.SSID.contains(result.SSID) && (isAPHaveInternet(nextConfig) || (isDualBandAP(bssid, result.BSSID) && !nextConfig.portalNetwork));
            if (isValidConfig(nextConfig) && bssidValue && authType == nextConfig.getAuthType()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidConfig(WifiConfiguration config) {
        if (config == null || config.SSID == null || config.allowedKeyManagement.cardinality() > 1) {
            return false;
        }
        return true;
    }

    private boolean isAPHaveInternet(WifiConfiguration config) {
        return WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 100);
    }

    private static int getSecurity(ScanResult result) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + result.SSID + "\"";
        if (result.capabilities.contains("WEP")) {
            config.allowedKeyManagement.set(0);
            config.allowedAuthAlgorithms.set(0);
            config.allowedAuthAlgorithms.set(1);
        }
        if (result.capabilities.contains("PSK")) {
            config.allowedKeyManagement.set(1);
        }
        if (result.capabilities.contains("EAP")) {
            config.allowedKeyManagement.set(2);
            config.allowedKeyManagement.set(3);
        }
        if (result.capabilities.contains(WifiProCommonUtils.KEY_MGMT_OWE)) {
            config.allowedKeyManagement.set(9);
        }
        if (result.capabilities.contains(WifiProCommonUtils.KEY_MGMT_SAE) && mIsWpa3SaeSupported) {
            config.allowedKeyManagement.set(8);
        }
        if (result.capabilities.contains(WifiProCommonUtils.KEY_MGMT_SUITE_B)) {
            config.allowedKeyManagement.set(10);
        }
        if (config.allowedKeyManagement.get(9)) {
            return 9;
        }
        if (config.allowedKeyManagement.get(8)) {
            return 8;
        }
        if (config.allowedKeyManagement.get(10)) {
            return 10;
        }
        if (config.allowedKeyManagement.get(1)) {
            return 1;
        }
        if (config.allowedKeyManagement.get(4)) {
            return 4;
        }
        if (config.allowedKeyManagement.get(2)) {
            return 2;
        }
        if (config.allowedKeyManagement.get(3)) {
            return 3;
        }
        if (config.allowedKeyManagement.get(16)) {
            return 16;
        }
        if (config.allowedKeyManagement.get(17)) {
            return 17;
        }
        if (config.allowedKeyManagement.get(18)) {
            return 18;
        }
        if (config.allowedKeyManagement.get(19)) {
            return 19;
        }
        return 0;
    }

    private boolean isHaveDuplicateRecord(ScanResult result, WifiProDualBandApInfoRcd info) {
        if (!("\"" + result.SSID + "\"").equals(info.mApSSID) || getSecurity(result) != info.mApAuthType.shortValue()) {
            return false;
        }
        return true;
    }
}
