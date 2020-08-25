package com.android.server.wifi;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.wifi.HwHiLog;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.wifipro.HwWifiProServiceManager;
import com.android.server.wifipro.WifiProCommonUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HwScanRequestProxyEx implements IHwScanRequestProxyEx {
    private static final int ALLOW_SEND_HILINK_SCAN_BROADCAST_TRIES = 10;
    private static final String COMBINE_WRONG_PASSWORD_ACTION = "com.huawei.wifi.action.COMBINE_WRONG_PASSWORD";
    private static final int ENCLOSING_QUOTES_LEN = 2;
    private static final String HILINK_STATE_CHANGE_ACTION = "com.android.server.wifi.huawei.action.NETWORK_CONNECTED";
    private static final String HW_SYSTEM_PERMISSION = "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM";
    private static final String PMFR_IN_CAPABILITY = "PMFR";
    private static final int PSK_ASCII_MIN_LEN = 10;
    private static final int PSK_HEX_MAX_LEN = 64;
    private static final int PSK_SAE_ASCII_MAX_LEN = 65;
    private static final int SAE_ASCII_MIN_LEN = 3;
    private static final int SAE_HEX_MAX_LEN = 63;
    private static final String TAG = "HwScanRequestProxyEx";
    private static final String WIFICATEGORY_CHANGED_ACTION = "com.huawei.wifi.action.WIFICATEGORY_CHANGED_ACTION";
    private static HwScanRequestProxyEx mHwScanRequestProxyEx;
    private boolean mAllowSendHiLinkScanResultsBroadcast = false;
    /* access modifiers changed from: private */
    public Comparator<ScanResult> mCompareBySignalAndSecurity = new Comparator<ScanResult>() {
        /* class com.android.server.wifi.HwScanRequestProxyEx.AnonymousClass1 */

        public int compare(ScanResult resultA, ScanResult resultB) {
            return HwScanRequestProxyEx.this.calculateScoreForResult(resultB) - HwScanRequestProxyEx.this.calculateScoreForResult(resultA);
        }
    };
    private Context mContext;
    private long mHilinkLastHashCode = 0;
    private long mHilinkLastLevelCode = 0;
    private final IHwScanRequestProxyInner mHwScanRequestProxyInner;
    private HwWifiProServiceManager mHwWifiProServiceManager;
    /* access modifiers changed from: private */
    public boolean mIsCombineWrongPassword = false;
    private int mSendHiLinkScanResultsBroadcastTries = 0;
    /* access modifiers changed from: private */
    public WifiConfigManager mWifiConfigManager;

    public static HwScanRequestProxyEx createHwScanRequestProxyEx(IHwScanRequestProxyInner hwScanRequestProxyInner, Context context, WifiInjector wifiInjector) {
        return new HwScanRequestProxyEx(hwScanRequestProxyInner, context, wifiInjector);
    }

    HwScanRequestProxyEx(IHwScanRequestProxyInner hwScanRequestProxyInner, Context context, WifiInjector wifiInjector) {
        this.mHwScanRequestProxyInner = hwScanRequestProxyInner;
        this.mWifiConfigManager = wifiInjector.getWifiConfigManager();
        this.mContext = context;
        this.mHwWifiProServiceManager = HwWifiProServiceManager.createHwWifiProServiceManager(context);
    }

    public void setAllowHiLinkScanResultsBroadcast(boolean allowBroadcast) {
        this.mAllowSendHiLinkScanResultsBroadcast = allowBroadcast;
        this.mSendHiLinkScanResultsBroadcastTries = 0;
    }

    public boolean getAllowHiLinkScanResultsBroadcast() {
        return this.mAllowSendHiLinkScanResultsBroadcast;
    }

    public void sendWifiCategoryChangeBroadcast() {
        ClientModeImpl clientModeImpl;
        WifiInfo wifiInfo;
        ScanDetailCache scanDetailCache;
        String connectBssid;
        ScanResult scanResult;
        if (WifiInjector.getInstance() != null && this.mWifiConfigManager != null && (clientModeImpl = WifiInjector.getInstance().getClientModeImpl()) != null && (wifiInfo = clientModeImpl.getWifiInfo()) != null && (scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(wifiInfo.getNetworkId())) != null && (scanResult = scanDetailCache.getScanResult((connectBssid = wifiInfo.getBSSID()))) != null && !TextUtils.isEmpty(connectBssid) && connectBssid.equals(scanResult.BSSID) && wifiInfo.getSupportedWifiCategory() != scanResult.getSupportedWifiCategory()) {
            Log.i(TAG, "sendWifiCategoryChangeBroadcast, old category = " + wifiInfo.getSupportedWifiCategory() + "new category = " + scanResult.getSupportedWifiCategory());
            wifiInfo.setSupportedWifiCategory(scanResult.getSupportedWifiCategory());
            this.mContext.sendBroadcastAsUser(new Intent(WIFICATEGORY_CHANGED_ACTION), UserHandle.ALL, HW_SYSTEM_PERMISSION);
        }
    }

    public void sendHilinkscanResultBroadcast() {
        long currentHilinkHashCode = 0;
        long currentLevelHilinkHashCode = 0;
        synchronized (this.mHwScanRequestProxyInner) {
            int resultSize = this.mHwScanRequestProxyInner.getScanResult().size();
            for (int i = 0; i < resultSize; i++) {
                ScanResult scanResult = (ScanResult) this.mHwScanRequestProxyInner.getScanResult().get(i);
                if (scanResult != null && scanResult.SSID != null && scanResult.SSID.length() == 32 && scanResult.SSID.startsWith("Hi")) {
                    int itemHashCode = scanResult.SSID.hashCode();
                    if (itemHashCode < 0) {
                        itemHashCode = -itemHashCode;
                    }
                    currentHilinkHashCode += (long) itemHashCode;
                    if (scanResult.level >= -45) {
                        currentLevelHilinkHashCode += (long) itemHashCode;
                    }
                }
            }
        }
        if (currentHilinkHashCode != this.mHilinkLastHashCode || currentLevelHilinkHashCode > this.mHilinkLastLevelCode) {
            HwHiLog.d(TAG, false, "Hilink sendHilinkscanResultBroadcast", new Object[0]);
            Intent broadcastIntent = new Intent(HILINK_STATE_CHANGE_ACTION);
            broadcastIntent.putExtra("TYPE", "SCAN_RESULTS");
            this.mContext.sendBroadcastAsUser(broadcastIntent, UserHandle.ALL);
            this.mHilinkLastHashCode = currentHilinkHashCode;
            this.mAllowSendHiLinkScanResultsBroadcast = false;
        }
        this.mHilinkLastLevelCode = currentLevelHilinkHashCode;
        int i2 = this.mSendHiLinkScanResultsBroadcastTries + 1;
        this.mSendHiLinkScanResultsBroadcastTries = i2;
        if (i2 > 10) {
            this.mAllowSendHiLinkScanResultsBroadcast = false;
        }
    }

    public void startScanForHiddenNetwork(int uid, WifiConfiguration config) {
        if (!this.mHwScanRequestProxyInner.hwRetrieveWifiScannerIfNecessary()) {
            HwHiLog.e(TAG, false, "Failed to retrieve wifiscanner", new Object[0]);
            return;
        }
        WifiScanner.ScanListener listener = new HiddenScanListener(config, uid);
        List<WifiScanner.ScanSettings.HiddenNetwork> hiddenNetworkList = new ArrayList<>();
        for (String charset : SystemProperties.getBoolean("ro.config.wifi_use_euc-kr", false) ? new String[]{"UTF-8", "EUC-KR", "KSC5601"} : new String[]{"UTF-8", "GBK"}) {
            String hex = StringUtilEx.quotedAsciiStringToHex(config.SSID, charset);
            if (hex != null) {
                hiddenNetworkList.add(new WifiScanner.ScanSettings.HiddenNetwork(hex));
            }
        }
        WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
        settings.band = 7;
        settings.reportEvents = 3;
        settings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) hiddenNetworkList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[hiddenNetworkList.size()]);
        settings.isHiddenSigleScan = true;
        HwHiLog.d(TAG, false, "startScanForHiddenNetwork", new Object[0]);
        this.mHwScanRequestProxyInner.getWifiScanner().startScan(settings, listener);
    }

    private boolean isHighSecurity(ScanResult scanResult) {
        if (ScanResultUtil.isScanResultForOweNetwork(scanResult) || ScanResultUtil.isScanResultForSaeNetwork(scanResult) || ScanResultUtil.isScanResultForEapSuiteBNetwork(scanResult)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public int calculateScoreForResult(ScanResult scanResult) {
        boolean isHighSecurity = isHighSecurity(scanResult);
        return (((isHighSecurity ? 1 : 0) + HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(scanResult.frequency, scanResult.level)) * 100) + scanResult.level;
    }

    /* access modifiers changed from: private */
    public void deleteOldConfigWithSameKeyIfExist(WifiConfiguration config) {
        WifiConfiguration oldConfig = this.mWifiConfigManager.getConfiguredNetwork(config.configKey());
        if (oldConfig != null) {
            HwHiLog.d(TAG, false, "old netId = %{public}d, new netId = %{public}d", new Object[]{Integer.valueOf(oldConfig.networkId), Integer.valueOf(config.networkId)});
            this.mWifiConfigManager.removeNetwork(oldConfig.networkId, 1000);
        }
    }

    protected class HiddenScanListener implements WifiScanner.ScanListener {
        private WifiConfiguration mConfig = null;
        private List<ScanResult> mScanResults = new ArrayList();
        private int mSendingUid = -1;
        private ClientModeImpl mWifiStateMachine = WifiInjector.getInstance().getClientModeImpl();

        HiddenScanListener(WifiConfiguration config, int uid) {
            this.mConfig = config;
            this.mSendingUid = uid;
        }

        private void quit() {
            this.mConfig = null;
            this.mSendingUid = -1;
            this.mScanResults.clear();
        }

        public void onResults(WifiScanner.ScanData[] scanDatas) {
            if (this.mConfig == null || HwScanRequestProxyEx.this.mWifiConfigManager.getConfiguredNetwork(this.mConfig.configKey()) == null || this.mScanResults.size() == 0) {
                HwHiLog.d(HwScanRequestProxyEx.TAG, false, "HiddenScanListener: return since config removed.", new Object[0]);
                return;
            }
            String ssid = NativeUtil.removeEnclosingQuotes(this.mConfig.SSID);
            this.mScanResults.size();
            boolean isCombinationType = this.mConfig.isCombinationType() && !this.mConfig.getNetworkSelectionStatus().getHasEverConnected();
            if (isCombinationType) {
                Collections.sort(this.mScanResults, HwScanRequestProxyEx.this.mCompareBySignalAndSecurity);
            }
            boolean unused = HwScanRequestProxyEx.this.mIsCombineWrongPassword = false;
            ScanResult candidate = selectBestCandidate(ssid);
            if (candidate != null) {
                if (isCombinationType && (this.mConfig.allowedKeyManagement.get(9) || this.mConfig.allowedKeyManagement.get(8) || this.mConfig.allowedKeyManagement.get(10))) {
                    HwScanRequestProxyEx.this.deleteOldConfigWithSameKeyIfExist(this.mConfig);
                }
                this.mConfig.oriSsid = candidate.wifiSsid.oriSsid;
                HwHiLog.d(HwScanRequestProxyEx.TAG, false, "HiddenScanListener: find SSID=%{public}s oriSsid=%{public}s isCombinationType=%{public}s", new Object[]{StringUtilEx.safeDisplaySsid(ssid), this.mConfig.oriSsid, String.valueOf(isCombinationType)});
                HwScanRequestProxyEx.this.mWifiConfigManager.addOrUpdateNetwork(this.mConfig, this.mSendingUid);
                this.mWifiStateMachine.startConnectToUserSelectNetwork(this.mConfig.networkId, this.mSendingUid, candidate.BSSID);
                quit();
                return;
            }
            HwHiLog.d(HwScanRequestProxyEx.TAG, false, "HiddenScanListener: can't find SSID=%{public}s isCombinationType=%{public}s", new Object[]{StringUtilEx.safeDisplaySsid(ssid), String.valueOf(isCombinationType)});
            if (HwScanRequestProxyEx.this.mIsCombineWrongPassword) {
                HwScanRequestProxyEx.this.notifyCombineWrongPassword(this.mConfig.networkId);
            }
            quit();
        }

        private ScanResult selectBestCandidate(String ssid) {
            ScanResult candidate = null;
            int size = this.mScanResults.size();
            for (int i = 0; i < size; i++) {
                ScanResult result = this.mScanResults.get(i);
                if (result != null && result.wifiSsid != null && !TextUtils.isEmpty(result.wifiSsid.oriSsid) && !TextUtils.isEmpty(result.SSID) && result.SSID.equals(ssid)) {
                    HwScanRequestProxyEx hwScanRequestProxyEx = HwScanRequestProxyEx.this;
                    WifiConfiguration wifiConfiguration = this.mConfig;
                    if (hwScanRequestProxyEx.isMatchedInResults(result, wifiConfiguration, wifiConfiguration.isCombinationType())) {
                        if (candidate != null) {
                            int newScore = WifiProCommonUtils.calculateScore(result);
                            int currentScore = WifiProCommonUtils.calculateScore(candidate);
                            HwHiLog.i(HwScanRequestProxyEx.TAG, false, "BSSID = %{public}s, is5g = %{public}s, WifiCategory = %{public}d, rssi = %{public}d, newScore = %{public}d, currentScore= %{public}d", new Object[]{StringUtilEx.safeDisplayBssid(result.BSSID), String.valueOf(ScanResult.is5GHz(result.frequency)), Integer.valueOf(result.supportedWifiCategory), Integer.valueOf(result.level), Integer.valueOf(newScore), Integer.valueOf(currentScore)});
                            if (newScore > currentScore || (newScore == currentScore && result.level > candidate.level)) {
                                candidate = result;
                            }
                        } else {
                            candidate = result;
                        }
                    }
                }
            }
            return candidate;
        }

        public void onFullResult(ScanResult scanResult) {
            this.mScanResults.add(scanResult);
        }

        public void onSuccess() {
        }

        public void onFailure(int i, String s) {
        }

        public void onPeriodChanged(int i) {
        }
    }

    private boolean isValidatePassword(String password, boolean isSae) {
        int targetMinLength;
        if (password == null) {
            HwHiLog.e(TAG, false, "validate password: null string", new Object[0]);
            return false;
        } else if (password.isEmpty()) {
            HwHiLog.e(TAG, false, "validate password: empty string", new Object[0]);
            return false;
        } else if (password.startsWith("\"")) {
            byte[] passwordBytes = password.getBytes(StandardCharsets.US_ASCII);
            if (isSae) {
                targetMinLength = 3;
            } else {
                targetMinLength = 10;
            }
            if (passwordBytes.length < targetMinLength) {
                HwHiLog.e(TAG, false, "failed: ASCII string size too small", new Object[0]);
                return false;
            } else if (passwordBytes.length <= 65) {
                return true;
            } else {
                HwHiLog.e(TAG, false, "failed: ASCII string size too large", new Object[0]);
                return false;
            }
        } else if (!isSae || password.length() <= 63) {
            return true;
        } else {
            HwHiLog.e(TAG, false, "failed: HEX string size too large", new Object[0]);
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void notifyCombineWrongPassword(int networkId) {
        this.mWifiConfigManager.updateNetworkSelectionStatus(networkId, 13);
        this.mContext.sendBroadcast(new Intent(COMBINE_WRONG_PASSWORD_ACTION), HW_SYSTEM_PERMISSION);
    }

    private boolean isWapiPskNetworkMatched(WifiConfiguration config) {
        return config.allowedKeyManagement.get(16) || config.allowedKeyManagement.get(18);
    }

    private boolean isCertNetworkMatched(WifiConfiguration config) {
        return config.allowedKeyManagement.get(17) || config.allowedKeyManagement.get(19);
    }

    private boolean shouldUpdateEapSuiteBNetwork(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(10)) {
            config.requirePMF = true;
            return true;
        } else if (!config.allowedKeyManagement.get(2) && !config.allowedKeyManagement.get(3) && !config.allowedKeyManagement.get(7)) {
            return false;
        } else {
            config.allowedKeyManagement.set(10);
            config.requirePMF = true;
            config.allowedPairwiseCiphers.set(3);
            config.allowedGroupCiphers.set(5);
            config.allowedGroupManagementCiphers.set(2);
            return true;
        }
    }

    private boolean shouldUpdateEapNetwork(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(10)) {
            return false;
        }
        if (!config.allowedKeyManagement.get(2) && !config.allowedKeyManagement.get(3) && !config.allowedKeyManagement.get(7)) {
            return false;
        }
        config.requirePMF = false;
        return true;
    }

    private boolean shouldUpdateSaeNetwork(ScanResult scanResult, WifiConfiguration config) {
        if (config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(6)) {
            WifiConfiguration networkConfig = this.mWifiConfigManager.getConfiguredNetworkWithoutMasking(config.networkId);
            if (networkConfig == null) {
                HwHiLog.e(TAG, false, "sae: get no mask config fail", new Object[0]);
                return false;
            }
            String password = networkConfig.preSharedKey;
            if (ScanResultUtil.isScanResultForPskNetwork(scanResult) && password != null && !password.startsWith("\"") && password.length() == 64) {
                return true;
            }
            if (!isValidatePassword(password, true)) {
                this.mIsCombineWrongPassword = true;
                return false;
            }
            config.allowedKeyManagement.clear();
            config.allowedKeyManagement.set(8);
            config.requirePMF = true;
            return true;
        } else if (!config.allowedKeyManagement.get(8)) {
            return false;
        } else {
            config.requirePMF = true;
            return true;
        }
    }

    private boolean shouldUpdatePskNetwork(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(6)) {
            WifiConfiguration networkConfig = this.mWifiConfigManager.getConfiguredNetworkWithoutMasking(config.networkId);
            if (networkConfig == null) {
                HwHiLog.e(TAG, false, "psk: get no mask config fail", new Object[0]);
                return false;
            } else if (isValidatePassword(networkConfig.preSharedKey, false)) {
                return true;
            } else {
                this.mIsCombineWrongPassword = true;
                return false;
            }
        } else if (!config.allowedKeyManagement.get(8)) {
            return false;
        } else {
            config.allowedKeyManagement.clear();
            config.allowedKeyManagement.set(1);
            config.requirePMF = false;
            return true;
        }
    }

    private boolean isWepNetworkMatched(WifiConfiguration config) {
        return config.wepKeys[0] != null;
    }

    private boolean shouldUpdateOweTransitionNetwork(ScanResult scanResult, WifiConfiguration config) {
        if (scanResult.capabilities.contains(PMFR_IN_CAPABILITY)) {
            return false;
        }
        if (config.allowedKeyManagement.get(0)) {
            config.allowedKeyManagement.clear();
            config.allowedKeyManagement.set(9);
            config.requirePMF = true;
            return true;
        } else if (!config.allowedKeyManagement.get(9)) {
            return false;
        } else {
            config.requirePMF = true;
            return true;
        }
    }

    private boolean shouldUpdateOweNetwork(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(9)) {
            config.requirePMF = true;
            return true;
        } else if (!config.allowedKeyManagement.get(0)) {
            return false;
        } else {
            config.allowedKeyManagement.clear();
            config.allowedKeyManagement.set(9);
            config.requirePMF = true;
            return true;
        }
    }

    private boolean shouldUpdateNormalOpenNetwork(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(0)) {
            config.requirePMF = false;
            return true;
        } else if (!config.allowedKeyManagement.get(9)) {
            return false;
        } else {
            config.allowedKeyManagement.clear();
            config.allowedKeyManagement.set(0);
            config.requirePMF = false;
            return true;
        }
    }

    /* access modifiers changed from: private */
    public boolean isMatchedInResults(ScanResult scanResult, WifiConfiguration config, boolean isCombinationType) {
        if (isCombinationType) {
            return isResultAndCombinationConfigSecurityMatched(scanResult, config);
        }
        return mactchResultAndConfigSecurity(scanResult, config);
    }

    private boolean isResultAndCombinationConfigSecurityMatched(ScanResult scanResult, WifiConfiguration config) {
        HwHiLog.d(TAG, false, "scanResult.capabilities=%{public}s", new Object[]{scanResult.capabilities});
        if (ScanResultUtil.isScanResultForWapiPskNetwork(scanResult)) {
            return isWapiPskNetworkMatched(config);
        }
        if (ScanResultUtil.isScanResultForCertNetwork(scanResult)) {
            return isCertNetworkMatched(config);
        }
        if (ScanResultUtil.isScanResultForEapSuiteBNetwork(scanResult)) {
            return shouldUpdateEapSuiteBNetwork(config);
        }
        if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
            return shouldUpdateEapNetwork(config);
        }
        if (ScanResultUtil.isScanResultForSaeNetwork(scanResult)) {
            return shouldUpdateSaeNetwork(scanResult, config);
        }
        if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
            return shouldUpdatePskNetwork(config);
        }
        if (ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
            return isWepNetworkMatched(config);
        }
        if (ScanResultUtil.isScanResultForOweTransitionNetwork(scanResult)) {
            return shouldUpdateOweTransitionNetwork(scanResult, config);
        }
        if (ScanResultUtil.isScanResultForOweNetwork(scanResult)) {
            return shouldUpdateOweNetwork(config);
        }
        return shouldUpdateNormalOpenNetwork(config);
    }

    /* access modifiers changed from: protected */
    public boolean mactchResultAndConfigSecurity(ScanResult scanResult, WifiConfiguration config) {
        if (ScanResultUtil.isScanResultForWapiPskNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForWapiPskNetwork", new Object[0]);
            return config.allowedKeyManagement.get(16) || config.allowedKeyManagement.get(18);
        } else if (ScanResultUtil.isScanResultForCertNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForCertNetwork", new Object[0]);
            return config.allowedKeyManagement.get(17) || config.allowedKeyManagement.get(19);
        } else if (ScanResultUtil.isScanResultForEapSuiteBNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForEapSuiteBNetwork", new Object[0]);
            return config.allowedKeyManagement.get(10);
        } else if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForEapNetwork", new Object[0]);
            return config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3) || config.allowedKeyManagement.get(7);
        } else if (ScanResultUtil.isScanResultForSaeNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForSaeNetwork", new Object[0]);
            if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
                return config.allowedKeyManagement.get(8) || config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(6);
            }
            return config.allowedKeyManagement.get(8);
        } else if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForPskNetwork", new Object[0]);
            return config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(6);
        } else if (ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForWepNetwork", new Object[0]);
            return config.wepKeys[0] != null;
        } else if (ScanResultUtil.isScanResultForOweTransitionNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForOweTransitionNetwork", new Object[0]);
            if (scanResult.capabilities.contains(PMFR_IN_CAPABILITY)) {
                return false;
            }
            return config.allowedKeyManagement.get(0) || config.allowedKeyManagement.get(9);
        } else if (ScanResultUtil.isScanResultForOweNetwork(scanResult)) {
            HwHiLog.d(TAG, false, "isScanResultForOweNetwork", new Object[0]);
            return config.allowedKeyManagement.get(9);
        } else {
            HwHiLog.d(TAG, false, "isScanResultForNone", new Object[0]);
            return config.allowedKeyManagement.get(0);
        }
    }

    public void updateScanResultByWifiPro(List<ScanResult> scanResults) {
        if (scanResults != null && WifiProCommonUtils.isWifiProSwitchOn(this.mContext)) {
            this.mHwWifiProServiceManager.updateScanResultByWifiPro(scanResults);
        }
    }
}
