package com.android.server.wifi.wifipro;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.List;

public class HwDualBandRelationManager {
    public static final int SECURITY_EAP = 3;
    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_WAPI_CERT = 5;
    public static final int SECURITY_WAPI_PSK = 4;
    public static final int SECURITY_WEP = 1;
    private static HwDualBandRelationManager mHwDualBandRelationManager = null;
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
    }

    public void updateAPRelation() {
        Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation");
        this.mHwDualBandInformationManager = HwDualBandInformationManager.getInstance();
        List<ScanResult> lists = WifiproUtils.getScanResultsFromWsm();
        if (lists == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation lists == null");
            return;
        }
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation mWifiInfo == null");
            return;
        }
        String bssid = mWifiInfo.getBSSID();
        boolean is5GAP = ScanResult.is5GHz(mWifiInfo.getFrequency());
        String str = HwDualBandMessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateAPRelation lists.size = ");
        stringBuilder.append(lists.size());
        Log.w(str, stringBuilder.toString());
        if (this.mHwDualBandInformationManager.getDualBandAPInfo(bssid) == null) {
            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation APInfo == null");
            return;
        }
        String ssid;
        String str2;
        StringBuilder stringBuilder2;
        WifiProDualBandApInfoRcd dualBandAPInfo;
        WifiProDualBandApInfoRcd ScanAPInfo;
        WifiProRelateApRcd RelateAPInfo;
        int isDualBandAP;
        if (is5GAP) {
            for (ScanResult result : lists) {
                if (isAPInConfig(result, bssid) && result.BSSID != null && !result.BSSID.equals(bssid) && ScanResult.is24GHz(result.frequency)) {
                    if (isHaveMultipleRecord(result)) {
                        ssid = new StringBuilder();
                        ssid.append("\"");
                        ssid.append(result.SSID);
                        ssid.append("\"");
                        ssid = ssid.toString();
                        str2 = HwDualBandMessageUtil.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("isHaveMultipleRecord  2.4G ssid == ");
                        stringBuilder2.append(ssid);
                        Log.w(str2, stringBuilder2.toString());
                        this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(ssid, getSecurity(result));
                    } else {
                        dualBandAPInfo = this.mHwDualBandInformationManager.getDualBandAPInfo(result.BSSID);
                        ScanAPInfo = dualBandAPInfo;
                        if (dualBandAPInfo != null && ScanAPInfo.mServingBand.shortValue() == (short) 1 && ScanAPInfo.mInetCapability.shortValue() == (short) 1) {
                            RelateAPInfo = getRelateAPInfo(result.BSSID, bssid);
                            if (RelateAPInfo == null) {
                                if (isDualBandAP(result.BSSID, bssid)) {
                                    isDualBandAP = 1;
                                } else {
                                    isDualBandAP = 2;
                                }
                                RelateAPInfo = new WifiProRelateApRcd(result.BSSID);
                                RelateAPInfo.mRelatedBSSID = bssid;
                                RelateAPInfo.mRelateType = isDualBandAP;
                                RelateAPInfo.mMaxRelatedRSSI = mWifiInfo.getRssi();
                                RelateAPInfo.mMinCurrentRSSI = result.level;
                                this.mWifiProHistoryRecordManager.saveRelateApInfo(RelateAPInfo);
                            } else if (mWifiInfo.getRssi() > RelateAPInfo.mMaxRelatedRSSI || RelateAPInfo.mMaxRelatedRSSI == 0) {
                                Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation max 5G AP relate rssi");
                                RelateAPInfo.mMaxRelatedRSSI = mWifiInfo.getRssi();
                                RelateAPInfo.mMinCurrentRSSI = result.level;
                                this.mWifiProHistoryRecordManager.saveRelateApInfo(RelateAPInfo);
                            }
                        }
                    }
                }
            }
        } else {
            for (ScanResult result2 : lists) {
                if (isAPInConfig(result2, bssid) && result2.BSSID != null && !result2.BSSID.equals(bssid) && ScanResult.is5GHz(result2.frequency)) {
                    if (isHaveMultipleRecord(result2)) {
                        ssid = new StringBuilder();
                        ssid.append("\"");
                        ssid.append(result2.SSID);
                        ssid.append("\"");
                        ssid = ssid.toString();
                        str2 = HwDualBandMessageUtil.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("isHaveMultipleRecord 5G ssid == ");
                        stringBuilder2.append(ssid);
                        Log.w(str2, stringBuilder2.toString());
                        this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(ssid, getSecurity(result2));
                    } else {
                        dualBandAPInfo = this.mHwDualBandInformationManager.getDualBandAPInfo(result2.BSSID);
                        ScanAPInfo = dualBandAPInfo;
                        if (dualBandAPInfo == null) {
                            ssid = HwDualBandMessageUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("updateAPRelation ssid = ");
                            stringBuilder2.append(result2.SSID);
                            Log.w(ssid, stringBuilder2.toString());
                            this.mHwDualBandInformationManager.saveAPInfo(result2, getSecurity(result2));
                        } else if (isDualBandAP(bssid, result2.BSSID) && ScanAPInfo.mInetCapability.shortValue() != (short) 1) {
                            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation update dual band internet capability");
                            ScanAPInfo.mInetCapability = Short.valueOf((short) 1);
                            this.mWifiProHistoryRecordManager.saveDualBandApInfo(ScanAPInfo);
                        }
                        ssid = HwDualBandMessageUtil.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateAPRelation ssid = ");
                        stringBuilder2.append(result2.SSID);
                        Log.w(ssid, stringBuilder2.toString());
                        RelateAPInfo = getRelateAPInfo(bssid, result2.BSSID);
                        if (RelateAPInfo == null) {
                            if (isDualBandAP(result2.BSSID, bssid)) {
                                isDualBandAP = 1;
                            } else {
                                isDualBandAP = 2;
                            }
                            RelateAPInfo = new WifiProRelateApRcd(bssid);
                            RelateAPInfo.mRelateType = isDualBandAP;
                            RelateAPInfo.mRelatedBSSID = result2.BSSID;
                            RelateAPInfo.mMaxCurrentRSSI = mWifiInfo.getRssi();
                            RelateAPInfo.mMinRelatedRSSI = result2.level;
                            String str3 = HwDualBandMessageUtil.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("updateAPRelation add new relation  isDualBandAP = ");
                            stringBuilder3.append(isDualBandAP);
                            Log.w(str3, stringBuilder3.toString());
                            this.mWifiProHistoryRecordManager.saveRelateApInfo(RelateAPInfo);
                        } else if (mWifiInfo.getRssi() > RelateAPInfo.mMaxCurrentRSSI || RelateAPInfo.mMaxCurrentRSSI == 0) {
                            Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation 2.4G AP relate rssi");
                            RelateAPInfo.mMaxCurrentRSSI = mWifiInfo.getRssi();
                            RelateAPInfo.mMinRelatedRSSI = result2.level;
                            this.mWifiProHistoryRecordManager.saveRelateApInfo(RelateAPInfo);
                        }
                    }
                }
            }
            WifiProDualBandApInfoRcd APInfo = this.mHwDualBandInformationManager.getDualBandAPInfo(bssid);
            if (APInfo == null) {
                Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation APInfo == null");
                return;
            }
            String str4 = HwDualBandMessageUtil.TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("updateAPRelation APInfo.getRelateApRcds().size() = ");
            stringBuilder4.append(APInfo.getRelateApRcds().size());
            Log.w(str4, stringBuilder4.toString());
            if (APInfo.getRelateApRcds().size() > 0) {
                if (APInfo.getRelateApRcds().size() == 1 && ((WifiProRelateApRcd) APInfo.getRelateApRcds().get(0)).mRelateType == 1) {
                    Log.w(HwDualBandMessageUtil.TAG, "updateAPRelation MSG_DUAL_BAND_WIFI_TYPE_SINGLE");
                    this.mHandler.sendEmptyMessage(16);
                } else {
                    str = HwDualBandMessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAPRelation MSG_DUAL_BAND_WIFI_TYPE_MIX size() = ");
                    stringBuilder.append(APInfo.getRelateApRcds().size());
                    Log.w(str, stringBuilder.toString());
                    this.mHandler.sendEmptyMessage(17);
                }
            }
        }
    }

    public List<WifiProRelateApRcd> getRelateAPInfos(String bssid) {
        List<WifiProRelateApRcd> relateApList = new ArrayList();
        this.mWifiProHistoryRecordManager.getRelateApList(bssid, relateApList);
        return relateApList;
    }

    public WifiProRelateApRcd getRelateAPInfo(String bssid, String relateAPbssid) {
        WifiProRelateApRcd result = null;
        List<WifiProRelateApRcd> RelateAPInfos = getRelateAPInfos(bssid);
        if (RelateAPInfos.size() == 0) {
            return null;
        }
        for (WifiProRelateApRcd info : RelateAPInfos) {
            if (info.apBSSID.equals(bssid) && info.mRelatedBSSID.equals(relateAPbssid)) {
                result = info;
            }
        }
        return result;
    }

    /* JADX WARNING: Missing block: B:7:0x0021, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isDualBandAP(String m24GAP, String m5GAP) {
        if (m24GAP == null || m5GAP == null || !m24GAP.substring(0, m24GAP.length() - 1).equals(m5GAP.substring(0, m5GAP.length() - 1))) {
            return false;
        }
        return true;
    }

    private boolean isAPInConfig(ScanResult result, String bssid) {
        if (result.SSID == null || result.SSID.length() <= 0) {
            Log.d(HwDualBandMessageUtil.TAG, "isAPInConfig, result.SSID error!");
            return false;
        }
        int authType = getSecurity(result);
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            Log.d(HwDualBandMessageUtil.TAG, "isAPInConfig, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        for (WifiConfiguration nextConfig : configNetworks) {
            if (isValidConfig(nextConfig) && nextConfig.SSID.contains(result.SSID) && authType == nextConfig.getAuthType() && (isAPHaveInternet(nextConfig) || isDualBandAP(bssid, result.BSSID))) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidConfig(WifiConfiguration config) {
        boolean z = false;
        if (config == null || config.SSID == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() <= 1) {
            z = true;
        }
        return z;
    }

    private boolean isAPHaveInternet(WifiConfiguration config) {
        return WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 100);
    }

    private static int getSecurity(ScanResult result) {
        WifiConfiguration config = new WifiConfiguration();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(result.SSID);
        stringBuilder.append("\"");
        config.SSID = stringBuilder.toString();
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
        if (config.allowedKeyManagement.get(8)) {
            return 8;
        }
        if (config.allowedKeyManagement.get(9)) {
            return 9;
        }
        if (config.allowedKeyManagement.get(10)) {
            return 10;
        }
        if (config.allowedKeyManagement.get(11)) {
            return 11;
        }
        return 0;
    }

    private boolean isHaveDuplicateRecord(ScanResult result, WifiProDualBandApInfoRcd info) {
        String ssid = new StringBuilder();
        ssid.append("\"");
        ssid.append(result.SSID);
        ssid.append("\"");
        if (ssid.toString().equals(info.mApSSID) && getSecurity(result) == info.mApAuthType.shortValue()) {
            return true;
        }
        return false;
    }

    private boolean isHaveMultipleRecord(ScanResult result) {
        int num = 0;
        String ssid = new StringBuilder();
        ssid.append("\"");
        ssid.append(result.SSID);
        ssid.append("\"");
        ssid = ssid.toString();
        List<WifiProDualBandApInfoRcd> list = this.mWifiProHistoryRecordManager.getDualBandApInfoBySsid(ssid);
        if (list != null && list.size() >= 1) {
            for (WifiProDualBandApInfoRcd record : list) {
                if (getSecurity(result) == record.mApAuthType.shortValue() && !record.apBSSID.equals(result.BSSID)) {
                    num++;
                }
            }
        }
        if (this.mWifiProHistoryRecordManager.isEnterpriseAP(ssid, getSecurity(result))) {
            return true;
        }
        return false;
    }
}
