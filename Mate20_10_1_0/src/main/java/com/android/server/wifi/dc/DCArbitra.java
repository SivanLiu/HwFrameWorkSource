package com.android.server.wifi.dc;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.server.wifi.MSS.HwMSSUtils;
import com.android.server.wifi.hwUtil.WifiCommonUtils;
import com.huawei.hwwifiproservice.HwDualBandMessageUtil;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DCArbitra {
    private static final int SCANRESULS_NUM_MAX = 10;
    private static final String TAG = "DCArbitra";
    private static DCArbitra mDCArbitra = null;
    private Context mContext;
    private List<DCConfiguration> mDCConfigList = new ArrayList();
    private final Object mLock = new Object();
    private String mPreferIface = DCUtils.INTERFACE_5G_GAME;
    private List<List<ScanResult>> mScanResults = new ArrayList();
    private DCConfiguration mSelectedDcConfig = null;
    private WifiManager mWifiManager;

    private DCArbitra(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
    }

    public static DCArbitra createDCArbitra(Context context) {
        if (mDCArbitra == null) {
            mDCArbitra = new DCArbitra(context);
        }
        return mDCArbitra;
    }

    public static DCArbitra getInstance() {
        return mDCArbitra;
    }

    public void updateScanResults() {
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        if (scanResults == null || scanResults.size() == 0) {
            HwHiLog.d(TAG, false, "getSavedDcNetworks, WiFi scan results are invalid, getScanResults is null", new Object[0]);
            return;
        }
        synchronized (this.mLock) {
            if (this.mScanResults.size() >= 10) {
                this.mScanResults.remove(0);
            }
            this.mScanResults.add(scanResults);
        }
    }

    private static boolean isEncryptionWep(String encryption) {
        return encryption.contains("WEP");
    }

    private static boolean isEncryptionPsk(String encryption) {
        return encryption.contains("PSK");
    }

    private static boolean isEncryptionEap(String encryption) {
        return encryption.contains("EAP");
    }

    private static boolean isOpenNetwork(String encryption) {
        if (!TextUtils.isEmpty(encryption) && !isEncryptionWep(encryption) && !isEncryptionPsk(encryption) && !isEncryptionEap(encryption)) {
            return true;
        }
        return false;
    }

    private static boolean isSameNotOpenEncryptType(String encryption1, String encryption2) {
        if (TextUtils.isEmpty(encryption1) || TextUtils.isEmpty(encryption2)) {
            return false;
        }
        if (isEncryptionWep(encryption1) && isEncryptionWep(encryption2)) {
            return true;
        }
        if (isEncryptionPsk(encryption1) && isEncryptionPsk(encryption2)) {
            return true;
        }
        if (!isEncryptionEap(encryption1) || !isEncryptionEap(encryption2)) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0080, code lost:
        r2 = r11.mWifiManager.getPrivilegedConfiguredNetworks();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0086, code lost:
        if (r2 == null) goto L_0x011f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x008c, code lost:
        if (r2.size() != 0) goto L_0x0090;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0090, code lost:
        r3 = r11.mDCConfigList.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x009a, code lost:
        if (r3.hasNext() == false) goto L_0x011e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x009c, code lost:
        r4 = r3.next();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00a6, code lost:
        if (r4.getSSID() == null) goto L_0x0096;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00ac, code lost:
        if (r4.getConfigKey() != null) goto L_0x00af;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00af, code lost:
        r5 = "\"" + r4.getSSID() + "\"";
        r7 = r4.getConfigKey();
        r8 = r2.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00d5, code lost:
        if (r8.hasNext() == false) goto L_0x0096;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00d7, code lost:
        r9 = r8.next();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00df, code lost:
        if (r9.SSID == null) goto L_0x00d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00e7, code lost:
        if (r9.SSID.equals(r5) == false) goto L_0x00d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00f1, code lost:
        if (isSameNotOpenEncryptType(r7, r9.configKey()) == false) goto L_0x0107;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00f3, code lost:
        r4.setPreSharedKey(r9.preSharedKey.substring(1, r9.preSharedKey.length() - 1));
        r4.setIsSavedNetworkFlag(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x010b, code lost:
        if (isOpenNetwork(r7) == false) goto L_0x00d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x0115, code lost:
        if (isOpenNetwork(r9.configKey()) == false) goto L_0x00d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x0117, code lost:
        r4.setIsSavedNetworkFlag(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x011e, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x011f, code lost:
        android.util.wifi.HwHiLog.d(com.android.server.wifi.dc.DCArbitra.TAG, false, "getSavedDcNetworks, WiFi configured networks are invalid", new java.lang.Object[0]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0128, code lost:
        return;
     */
    private void updateDcConfig() {
        List<DCConfiguration> list = this.mDCConfigList;
        if (list != null && list.size() != 0) {
            List<ScanResult> scanResults = this.mWifiManager.getScanResults();
            List<DCConfiguration> currentDcConfigList = this.mDCConfigList;
            synchronized (this.mLock) {
                int scanResultsListSize = this.mScanResults.size();
                HwHiLog.d(TAG, false, "scanResultsListSize = %{public}d", new Object[]{Integer.valueOf(scanResultsListSize)});
                if (scanResultsListSize == 0) {
                    if (scanResults != null) {
                        if (scanResults.size() != 0) {
                            this.mScanResults.add(scanResults);
                            scanResultsListSize = this.mScanResults.size();
                        }
                    }
                    HwHiLog.d(TAG, false, "getSavedDcNetworks, WiFi scan results are invalid, getScanResults is null ", new Object[0]);
                    return;
                }
                for (DCConfiguration dcConfig : currentDcConfigList) {
                    if (dcConfig.getBSSID() != null) {
                        int i = scanResultsListSize - 1;
                        while (true) {
                            if (i < 0) {
                                break;
                            } else if (isFrequencyAndRssiSet(dcConfig, this.mScanResults.get(i))) {
                                break;
                            } else {
                                i--;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isHilinkGateway() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager == null) {
            HwHiLog.e(TAG, false, "mWifiManager is null", new Object[0]);
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null || wifiInfo.getBSSID() == null) {
            HwHiLog.e(TAG, false, "wifiInfo or bssid is null", new Object[0]);
            return false;
        }
        String bssid = wifiInfo.getBSSID();
        synchronized (this.mLock) {
            for (int i = this.mScanResults.size() - 1; i >= 0; i--) {
                for (ScanResult scanResult : this.mScanResults.get(i)) {
                    if (scanResult.BSSID.equalsIgnoreCase(bssid)) {
                        if (scanResult.isHiLinkNetwork) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public int getFrequencyForBssid(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return 0;
        }
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        synchronized (this.mLock) {
            int scanResultsListSize = this.mScanResults.size();
            HwHiLog.i(TAG, false, "scanResultsListSize = %{public}d", new Object[]{Integer.valueOf(scanResultsListSize)});
            if (scanResultsListSize == 0) {
                if (scanResults != null) {
                    if (scanResults.size() != 0) {
                        this.mScanResults.add(scanResults);
                        scanResultsListSize = this.mScanResults.size();
                    }
                }
                HwHiLog.i(TAG, false, "getFrequencyForBssid, WiFi scan results are invalid, getScanResults is null", new Object[0]);
                return 0;
            }
            for (int i = scanResultsListSize - 1; i >= 0; i--) {
                for (ScanResult scanResult : this.mScanResults.get(i)) {
                    if (bssid.equalsIgnoreCase(scanResult.BSSID) && (ScanResult.is24GHz(scanResult.frequency) || ScanResult.is5GHz(scanResult.frequency))) {
                        return scanResult.frequency;
                    }
                }
            }
            return 0;
        }
    }

    private boolean isFrequencyAndRssiSet(DCConfiguration dcConfig, List<ScanResult> scanResults) {
        for (ScanResult scanResult : scanResults) {
            if (dcConfig.getBSSID().equalsIgnoreCase(scanResult.BSSID) && (ScanResult.is24GHz(scanResult.frequency) || ScanResult.is5GHz(scanResult.frequency))) {
                if (dcConfig.getFrequency() <= 0) {
                    dcConfig.setFrequency(scanResult.frequency);
                }
                dcConfig.setRssi(scanResult.level);
                return true;
            }
        }
        return false;
    }

    public boolean isValidDcConfigSaved() {
        WifiManager wifiManager;
        List<DCConfiguration> list = this.mDCConfigList;
        if (list == null || list.size() == 0 || (wifiManager = this.mWifiManager) == null) {
            HwHiLog.e(TAG, false, "no valid DcNetwork is saved", new Object[0]);
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return false;
        }
        for (DCConfiguration dcConfig : this.mDCConfigList) {
            if (dcConfig.isSavedNetwork() && ScanResult.is24GHz(wifiInfo.getFrequency()) && dcConfig.getInterface() != null && (DCUtils.INTERFACE_5G.equals(dcConfig.getInterface()) || DCUtils.INTERFACE_5G_GAME.equals(dcConfig.getInterface()))) {
                return true;
            }
            if (dcConfig.isSavedNetwork() && ScanResult.is5GHz(wifiInfo.getFrequency()) && dcConfig.getInterface() != null && DCUtils.INTERFACE_2G.equals(dcConfig.getInterface())) {
                return true;
            }
            HwHiLog.d(TAG, false, "skip invalid DcNetwork", new Object[0]);
        }
        return false;
    }

    public DCConfiguration selectDcNetwork() {
        List<DCConfiguration> list = this.mDCConfigList;
        if (list == null || list.size() == 0) {
            HwHiLog.i(TAG, false, "no DcNetwork to select", new Object[0]);
            return null;
        }
        updateDcConfig();
        DCConfiguration preferDcConfig = null;
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        HwHiLog.d(TAG, false, "connected network: is24GHz:%{public}s", new Object[]{Boolean.valueOf(ScanResult.is24GHz(wifiInfo.getFrequency()))});
        if (!ScanResult.is24GHz(wifiInfo.getFrequency()) || !HwMSSUtils.is1105() || wifiInfo.getSupportedWifiCategory() < 2) {
            int nextPreferRssi = -127;
            for (DCConfiguration dcConfig : this.mDCConfigList) {
                HwHiLog.d(TAG, false, "IsSavedNetwork:%{public}s is24GHz:%{public}s", new Object[]{Boolean.valueOf(dcConfig.isSavedNetwork()), Boolean.valueOf(ScanResult.is24GHz(dcConfig.getFrequency()))});
                if (dcConfig.getBSSID() != null && !dcConfig.getBSSID().equalsIgnoreCase(wifiInfo.getBSSID())) {
                    if (!dcConfig.isSavedNetwork() || ((!ScanResult.is24GHz(dcConfig.getFrequency()) && !ScanResult.is5GHz(dcConfig.getFrequency())) || ((ScanResult.is24GHz(wifiInfo.getFrequency()) && ScanResult.is24GHz(dcConfig.getFrequency())) || (ScanResult.is5GHz(wifiInfo.getFrequency()) && ScanResult.is5GHz(dcConfig.getFrequency()))))) {
                        HwHiLog.i(TAG, false, "skip ssid that is not saved or in same band", new Object[0]);
                    } else if (dcConfig.getInterface() != null && dcConfig.getInterface().equals(this.mPreferIface)) {
                        return dcConfig;
                    } else {
                        if (dcConfig.getRssi() > nextPreferRssi) {
                            nextPreferRssi = dcConfig.getRssi();
                            preferDcConfig = dcConfig;
                        }
                    }
                }
            }
            return preferDcConfig;
        }
        HwHiLog.i(TAG, false, "skip ssid that is support wifi6 but hi110x chip", new Object[0]);
        return null;
    }

    public DCConfiguration selectDCNetworkFromPayload(String dcConfigPayload) {
        this.mSelectedDcConfig = null;
        if (TextUtils.isEmpty(dcConfigPayload)) {
            return null;
        }
        parseDcConfig(dcConfigPayload);
        this.mSelectedDcConfig = selectDcNetwork();
        if (this.mSelectedDcConfig == null) {
            HwHiLog.i(TAG, false, "no network to start DC", new Object[0]);
        }
        return this.mSelectedDcConfig;
    }

    public DCConfiguration getSelectedDCConfig() {
        return this.mSelectedDcConfig;
    }

    public List<DCConfiguration> getDCConfigList() {
        return this.mDCConfigList;
    }

    private void parseDcConfig(String dcConfigPayload) {
        List<DCConfiguration> list = this.mDCConfigList;
        if (list == null) {
            this.mDCConfigList = new ArrayList();
        } else {
            list.clear();
        }
        if (!TextUtils.isEmpty(dcConfigPayload)) {
            try {
                JSONArray dcConfigJsonArray = new JSONObject(dcConfigPayload).getJSONArray("deviceinfo");
                int jsonArrayLength = dcConfigJsonArray.length();
                for (int i = 0; i < jsonArrayLength; i++) {
                    DCConfiguration dcConfig = new DCConfiguration();
                    JSONObject jsonDcConfig = dcConfigJsonArray.getJSONObject(i);
                    dcConfig.setInterface(jsonDcConfig.getString("interface"));
                    dcConfig.setSSID(jsonDcConfig.getString("ssid"));
                    dcConfig.setAuthType(jsonDcConfig.getString(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE));
                    dcConfig.setBSSID(jsonDcConfig.getString("mac"));
                    if (!dcConfig.isAuthTypeAllowed()) {
                        HwHiLog.i(TAG, false, "AuthType is not allowed, skip", new Object[0]);
                    } else {
                        if (jsonDcConfig.has("channel")) {
                            int channel = jsonDcConfig.getInt("channel");
                            dcConfig.setFrequency(WifiCommonUtils.convertChannelToFrequency(channel));
                            HwHiLog.i(TAG, false, "channel=%{public}d", new Object[]{Integer.valueOf(channel)});
                        }
                        this.mDCConfigList.add(dcConfig);
                    }
                }
            } catch (JSONException e) {
                this.mDCConfigList.clear();
                HwHiLog.e(TAG, false, "JSONException when parseDCConfig", new Object[0]);
            }
        }
    }
}
