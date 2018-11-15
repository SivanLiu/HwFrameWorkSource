package com.android.server.wifi.wifipro;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HwDualBandInformationManager {
    private static HwDualBandInformationManager mHwDualBandInformationManager = null;
    private Context mContext;
    private WifiManager mWifiManager = null;
    private WifiProHistoryRecordManager mWifiProHistoryRecordManager;

    public static HwDualBandInformationManager createInstance(Context context) {
        if (mHwDualBandInformationManager == null) {
            mHwDualBandInformationManager = new HwDualBandInformationManager(context);
        }
        return mHwDualBandInformationManager;
    }

    public static HwDualBandInformationManager getInstance() {
        return mHwDualBandInformationManager;
    }

    private HwDualBandInformationManager(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mWifiProHistoryRecordManager = WifiProHistoryRecordManager.getInstance(this.mContext, this.mWifiManager);
    }

    public void updateAPInfo(WifiProDualBandApInfoRcd mApInfo) {
        String str = HwDualBandMessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateAPInfo update AP info ssid = ");
        stringBuilder.append(mApInfo.mApSSID);
        Log.e(str, stringBuilder.toString());
        this.mWifiProHistoryRecordManager.saveDualBandApInfo(mApInfo);
    }

    public boolean saveAPInfo() {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo == null) {
            return false;
        }
        String mCurrentBSSID = mWifiInfo.getBSSID();
        if (mCurrentBSSID == null) {
            Log.e(HwDualBandMessageUtil.TAG, "mCurrentBSSID == null");
            return false;
        }
        WifiProDualBandApInfoRcd apInfo = new WifiProDualBandApInfoRcd(mCurrentBSSID);
        apInfo.apBSSID = mCurrentBSSID;
        apInfo.mApSSID = mWifiInfo.getSSID();
        apInfo.mApAuthType = Short.valueOf((short) getAuthType(mWifiInfo.getNetworkId()));
        apInfo.mChannelFrequency = mWifiInfo.getFrequency();
        apInfo.mInetCapability = Short.valueOf((short) 1);
        apInfo.isInBlackList = 0;
        if (ScanResult.is5GHz(mWifiInfo.getFrequency())) {
            apInfo.mServingBand = Short.valueOf((short) 2);
        } else {
            apInfo.mServingBand = Short.valueOf((short) 1);
        }
        String str = HwDualBandMessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ConnectedState apInfo.apBSSID = ");
        stringBuilder.append(partDisplayBssid(apInfo.apBSSID));
        stringBuilder.append("  apInfo.mApSSID = ");
        stringBuilder.append(apInfo.mApSSID);
        stringBuilder.append(" apInfo.mChannelFrequency = ");
        stringBuilder.append(apInfo.mChannelFrequency);
        stringBuilder.append(" apInfo.mServingBand = ");
        stringBuilder.append(apInfo.mServingBand);
        Log.e(str, stringBuilder.toString());
        this.mWifiProHistoryRecordManager.saveDualBandApInfo(apInfo);
        return true;
    }

    public WifiProDualBandApInfoRcd getDualBandAPInfo(String bssid) {
        List<WifiProRelateApRcd> relateApList = new ArrayList();
        WifiProDualBandApInfoRcd result = this.mWifiProHistoryRecordManager.getDualBandApRecord(bssid);
        if (result != null) {
            this.mWifiProHistoryRecordManager.getRelateApList(bssid, relateApList);
            result.setRelateApRcds(relateApList);
        } else {
            Log.e(HwDualBandMessageUtil.TAG, "getDualBandAPInfo return null");
        }
        return result;
    }

    public void delectDualBandAPInfoBySsid(String ssid, int authtype) {
        String str = HwDualBandMessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("delectDualBandAPInfoBySsid ssid= ");
        stringBuilder.append(ssid);
        Log.e(str, stringBuilder.toString());
        List<WifiProDualBandApInfoRcd> RcdList = this.mWifiProHistoryRecordManager.getDualBandApInfoBySsid(ssid);
        if (RcdList != null && RcdList.size() != 0) {
            String str2 = HwDualBandMessageUtil.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("delectDualBandAPInfoBySsid  RcdList.size() = ");
            stringBuilder2.append(RcdList.size());
            Log.e(str2, stringBuilder2.toString());
            for (WifiProDualBandApInfoRcd rcd : RcdList) {
                if (rcd.mApAuthType.shortValue() == authtype) {
                    String str3 = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("delectDualBandAPInfoBySsid  rcd.mApSSID = ");
                    stringBuilder3.append(rcd.mApSSID);
                    stringBuilder3.append(" rcd.mServingBand = ");
                    stringBuilder3.append(rcd.mServingBand);
                    Log.e(str3, stringBuilder3.toString());
                    this.mWifiProHistoryRecordManager.deleteDualBandApInfo(rcd.apBSSID);
                    if (rcd.mServingBand.shortValue() == (short) 1) {
                        this.mWifiProHistoryRecordManager.deleteRelateApInfo(rcd.apBSSID);
                    } else {
                        this.mWifiProHistoryRecordManager.deleteRelate5GApInfo(rcd.apBSSID);
                    }
                }
            }
        }
    }

    public boolean isEnterpriseAP(String bssid) {
        WifiProApInfoRecord record = this.mWifiProHistoryRecordManager.getApInfoRecord(bssid);
        if (record == null) {
            Log.e(HwDualBandMessageUtil.TAG, "record == null");
            return false;
        }
        String str = HwDualBandMessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("record.isEnterpriseAP = ");
        stringBuilder.append(record.isEnterpriseAP);
        Log.e(str, stringBuilder.toString());
        if (record.isEnterpriseAP) {
            return true;
        }
        return false;
    }

    private boolean isValid(WifiConfiguration config) {
        boolean z = false;
        if (config == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() <= 1) {
            z = true;
        }
        return z;
    }

    public boolean isEnterpriseSecurity(int networkId) {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs != null && configs.size() > 0) {
            for (WifiConfiguration config : configs) {
                if (config != null && networkId == config.networkId) {
                    return config.isEnterprise();
                }
            }
        }
        return false;
    }

    public int getAuthType(int networkId) {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs == null || configs.size() == 0) {
            return -1;
        }
        for (WifiConfiguration config : configs) {
            if (config != null && isValid(config) && networkId == config.networkId) {
                String str = HwDualBandMessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAuthType  networkId= ");
                stringBuilder.append(networkId);
                stringBuilder.append(" config.getAuthType() = ");
                stringBuilder.append(config.getAuthType());
                Log.d(str, stringBuilder.toString());
                return config.getAuthType();
            }
        }
        return -1;
    }

    public boolean saveAPInfo(ScanResult result, int authType) {
        Log.e(HwDualBandMessageUtil.TAG, "saveAPInfo for scan result");
        String mCurrentBSSID = result.BSSID;
        if (mCurrentBSSID == null) {
            Log.e(HwDualBandMessageUtil.TAG, "mCurrentBSSID == null");
            return false;
        }
        WifiProDualBandApInfoRcd apInfo = new WifiProDualBandApInfoRcd(mCurrentBSSID);
        apInfo.apBSSID = mCurrentBSSID;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(result.SSID);
        stringBuilder.append("\"");
        apInfo.mApSSID = stringBuilder.toString();
        apInfo.mApAuthType = Short.valueOf((short) authType);
        apInfo.mChannelFrequency = result.frequency;
        apInfo.mInetCapability = Short.valueOf((short) 1);
        if (ScanResult.is5GHz(result.frequency)) {
            apInfo.mServingBand = Short.valueOf((short) 2);
        } else {
            apInfo.mServingBand = Short.valueOf((short) 1);
        }
        String str = HwDualBandMessageUtil.TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("ConnectedState apInfo.apBSSID = ");
        stringBuilder2.append(partDisplayBssid(apInfo.apBSSID));
        stringBuilder2.append("  apInfo.mApSSID = ");
        stringBuilder2.append(apInfo.mApSSID);
        stringBuilder2.append(" apInfo.mChannelFrequency = ");
        stringBuilder2.append(apInfo.mChannelFrequency);
        stringBuilder2.append(" apInfo.mServingBand = ");
        stringBuilder2.append(apInfo.mServingBand);
        Log.e(str, stringBuilder2.toString());
        this.mWifiProHistoryRecordManager.saveDualBandApInfo(apInfo);
        return true;
    }

    public boolean isHaveMultipleAP(String bssid, String ssid, int type) {
        return this.mWifiProHistoryRecordManager.isHaveMultipleAP(bssid, ssid, type);
    }

    private String partDisplayBssid(String srcBssid) {
        if (srcBssid == null) {
            return "null";
        }
        int len = srcBssid.length();
        if (len < 12) {
            return "Can not display bssid";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(srcBssid.substring(0, 9));
        stringBuilder.append("**:**");
        stringBuilder.append(srcBssid.substring(len - 3, len));
        return stringBuilder.toString();
    }
}
