package com.android.server.wifi.wifipro;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HwQoE.HidataWechatTraffic;
import com.android.server.wifi.HwWifiServiceFactory;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class WiFiProEvaluateController {
    private static final int BACK_EVALUATE_RSSI_CHANGE_VALIDITY = 20;
    private static final int BACK_EVALUATE_TIME_VALIDITY = 14400000;
    private static final String INVAILD_SSID = "<unknown ssid>";
    public static final int MAX_FAIL_COUNTER = 2;
    public static final int MIN_RSSI_LEVEL_EVALUATE_BACK = 3;
    public static final int MIN_RSSI_LEVEL_EVALUATE_SETTINGS = 2;
    private static final int SCORE_PROTECTION_DURATION = 60000;
    private static final int SETTINGS_EVALUATE_RSSI_CHANGE_VALIDITY = 20;
    private static final int SETTINGS_EVALUATE_TIME_VALIDITY = 1800000;
    private static final String TAG = "WiFi_PRO_EvaluateController";
    private static Map<String, WiFiProScoreInfo> mEvaluateAPHashMap = new HashMap();
    private Context mContext;
    private boolean mIsWiFiProEvaluateEnabled;
    private List<WiFiProScoreInfo> mOpenApList;
    private List<WiFiProScoreInfo> mSavedApList;
    private Map<String, ScanResult> mScanResultHashMap;
    private Queue<String> mUnEvaluateAPQueue;
    private List<ScanResult> mUnRepetitionScanResultList;
    private List<String> mUntrustedOpenApList;
    private WifiManager mWifiManager;

    public WiFiProEvaluateController(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    public static boolean isEvaluateRecordsEmpty() {
        if (mEvaluateAPHashMap == null || mEvaluateAPHashMap.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isUnEvaluateAPRecordsEmpty() {
        if (this.mUnEvaluateAPQueue == null || this.mUnEvaluateAPQueue.isEmpty()) {
            return true;
        }
        return false;
    }

    public void wiFiProEvaluateEnable(boolean enable) {
        this.mIsWiFiProEvaluateEnabled = enable;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsWiFiProEvaluateEnabled = ");
        stringBuilder.append(enable);
        Log.d(str, stringBuilder.toString());
    }

    public static void evaluateAPHashMapDump() {
        if (mEvaluateAPHashMap != null && !mEvaluateAPHashMap.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mEvaluateAPHashMap size =");
            stringBuilder.append(mEvaluateAPHashMap.size());
            Log.d(str, stringBuilder.toString());
        }
    }

    public void unEvaluateAPQueueDump() {
        if (this.mUnEvaluateAPQueue == null || this.mUnEvaluateAPQueue.isEmpty()) {
            Log.d(TAG, "null == mUnEvaluateAPQueue || mUnEvaluateAPQueue.isEmpty()");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mUnEvaluateAPQueue size =");
        stringBuilder.append(this.mUnEvaluateAPQueue.size());
        Log.d(str, stringBuilder.toString());
        for (String ssid : this.mUnEvaluateAPQueue) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mUnEvaluateAPQueue ssid =");
            stringBuilder2.append(ssid);
            Log.d(str2, stringBuilder2.toString());
        }
    }

    public boolean isWiFiProEvaluateEnable() {
        return this.mIsWiFiProEvaluateEnabled;
    }

    public boolean isLastEvaluateValid(WifiInfo wifiinfo, int evalate_type) {
        if (wifiinfo == null || TextUtils.isEmpty(wifiinfo.getSSID())) {
            return false;
        }
        String ssid = wifiinfo.getSSID();
        if (mEvaluateAPHashMap.containsKey(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
            if (wiFiProScoreInfo == null || wiFiProScoreInfo.internetAccessType == 0 || wiFiProScoreInfo.networkQosLevel == 0) {
                return false;
            }
            String str;
            StringBuilder stringBuilder;
            if (System.currentTimeMillis() - wiFiProScoreInfo.lastScoreTime >= 1800000) {
                long time = (System.currentTimeMillis() - wiFiProScoreInfo.lastScoreTime) / 1000;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(ssid);
                stringBuilder2.append(",crurrent rssi =  ");
                stringBuilder2.append(wifiinfo.getRssi());
                stringBuilder2.append(", last  rssi = ");
                stringBuilder2.append(wiFiProScoreInfo.rssi);
                stringBuilder2.append(",interval  time = ");
                stringBuilder2.append(time);
                stringBuilder2.append("s, last evaluate is NOT Valid");
                Log.d(str2, stringBuilder2.toString());
            } else if (calculateSignalLevelHW(wiFiProScoreInfo.is5GHz, wiFiProScoreInfo.rssi) == calculateSignalLevelHW(wifiinfo.is5GHz(), wifiinfo.getRssi())) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(",crurrent Signal =  ");
                stringBuilder.append(calculateSignalLevelHW(wifiinfo.is5GHz(), wifiinfo.getRssi()));
                stringBuilder.append(", last  Signal = ");
                stringBuilder.append(calculateSignalLevelHW(wiFiProScoreInfo.is5GHz, wiFiProScoreInfo.rssi));
                stringBuilder.append(", isSemiAuto, last evaluate is Valid");
                Log.d(str, stringBuilder.toString());
                return true;
            } else if (Math.abs(wifiinfo.getRssi() - wiFiProScoreInfo.rssi) < 20) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(",crurrent rssi =  ");
                stringBuilder.append(wifiinfo.getRssi());
                stringBuilder.append(", last  rssi = ");
                stringBuilder.append(wiFiProScoreInfo.rssi);
                stringBuilder.append(", isSemiAuto, last evaluate is Valid");
                Log.d(str, stringBuilder.toString());
                return true;
            } else {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(ssid);
                stringBuilder3.append(",Signal level change,and rssi >20");
                Log.d(str3, stringBuilder3.toString());
            }
        }
        return false;
    }

    public boolean isLastEvaluateValid(ScanResult scanResult, int evalate_type) {
        if (scanResult == null || TextUtils.isEmpty(scanResult.SSID)) {
            return true;
        }
        if (isEvaluateRecordsEmpty()) {
            return false;
        }
        String ssid = new StringBuilder();
        ssid.append("\"");
        ssid.append(scanResult.SSID);
        ssid.append("\"");
        ssid = ssid.toString();
        if (!mEvaluateAPHashMap.containsKey(ssid)) {
            return false;
        }
        WiFiProScoreInfo wiFiProScoreInfo = (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
        if (wiFiProScoreInfo == null || wiFiProScoreInfo.internetAccessType == 0) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (System.currentTimeMillis() - wiFiProScoreInfo.lastScoreTime < HidataWechatTraffic.MIN_VALID_TIME) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(ssid);
            stringBuilder.append(", evaluate  protection duration, is valid");
            Log.d(str, stringBuilder.toString());
            return true;
        }
        String str2;
        if (evalate_type == 1) {
            if (wiFiProScoreInfo.internetAccessType == 4 && wiFiProScoreInfo.networkQosLevel == 0) {
                return false;
            }
            if (wiFiProScoreInfo.internetAccessType == 1) {
                if (wiFiProScoreInfo.failCounter < 2) {
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(ssid);
                    stringBuilder.append(", is network congestion, activity is Settings, failCounter = ");
                    stringBuilder.append(wiFiProScoreInfo.failCounter);
                    Log.d(str2, stringBuilder.toString());
                    return false;
                } else if (isEvaluateConditionChange(scanResult, evalate_type)) {
                    reSetEvaluateRecord(ssid);
                    return true;
                }
            }
        } else if (wiFiProScoreInfo.internetAccessType == 3 || wiFiProScoreInfo.internetAccessType == 2) {
            return true;
        } else {
            if (wiFiProScoreInfo.internetAccessType == 4) {
                if (isEvaluateConditionChange(scanResult, evalate_type)) {
                    updateScoreInfoLevel(ssid, 0);
                }
                return true;
            } else if (wiFiProScoreInfo.internetAccessType == 1) {
                if (wiFiProScoreInfo.failCounter >= 2) {
                    return true;
                }
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(", is network congestion, activity is Not settings, failCounter = ");
                stringBuilder.append(wiFiProScoreInfo.failCounter);
                Log.d(str2, stringBuilder.toString());
                return false;
            }
        }
        if (isEvaluateConditionChange(scanResult, evalate_type)) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(ssid);
            stringBuilder.append(",last evaluate is NOT valid ");
            Log.d(str2, stringBuilder.toString());
            return false;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(ssid);
        stringBuilder.append(",last evaluate is valid ");
        Log.d(str, stringBuilder.toString());
        return true;
    }

    /* JADX WARNING: Missing block: B:42:0x0120, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isEvaluateConditionChange(ScanResult scanResult, int evalate_type) {
        if (scanResult == null || TextUtils.isEmpty(scanResult.SSID) || isEvaluateRecordsEmpty()) {
            return false;
        }
        String ssid = new StringBuilder();
        ssid.append("\"");
        ssid.append(scanResult.SSID);
        ssid.append("\"");
        ssid = ssid.toString();
        if (mEvaluateAPHashMap.containsKey(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
            if (wiFiProScoreInfo == null) {
                return false;
            }
            long valid_time = 0;
            int valid_rssi_change = 0;
            if (evalate_type == 0) {
                valid_time = 14400000;
                valid_rssi_change = 20;
            } else if (evalate_type == 1) {
                valid_time = 1800000;
                valid_rssi_change = 20;
            } else if (evalate_type == 2) {
                valid_time = 1800000;
                valid_rssi_change = 20;
            }
            String str;
            StringBuilder stringBuilder;
            if (wiFiProScoreInfo.internetAccessType == 2 || wiFiProScoreInfo.internetAccessType == 3) {
                if (System.currentTimeMillis() - wiFiProScoreInfo.lastScoreTime < 12 * valid_time) {
                    return false;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(",last evaluate is  PORTA or no intenet ,  is timeout, last evaluate date: ");
                stringBuilder.append(WifiproUtils.formatTime(wiFiProScoreInfo.lastScoreTime));
                Log.d(str, stringBuilder.toString());
                return true;
            } else if (System.currentTimeMillis() - wiFiProScoreInfo.lastScoreTime > valid_time) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(",last evaluate is NOT Valid,last evaluate date: ");
                stringBuilder.append(WifiproUtils.formatTime(wiFiProScoreInfo.lastScoreTime));
                Log.d(str, stringBuilder.toString());
                return true;
            } else if (calculateSignalLevelHW(wiFiProScoreInfo.is5GHz, wiFiProScoreInfo.rssi) != calculateSignalLevelHW(scanResult.is5GHz(), scanResult.level) && Math.abs(scanResult.level - wiFiProScoreInfo.rssi) > valid_rssi_change) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(",last evaluate is NOT Valid,SignalLevel change, and rssi change >20, last rssi=");
                stringBuilder.append(wiFiProScoreInfo.rssi);
                stringBuilder.append(", now rssi = ");
                stringBuilder.append(scanResult.level);
                Log.d(str, stringBuilder.toString());
                return true;
            } else if (Math.abs(calculateSignalLevelHW(wiFiProScoreInfo.is5GHz, wiFiProScoreInfo.rssi) - calculateSignalLevelHW(scanResult.is5GHz(), scanResult.level)) > 2) {
                return true;
            }
        }
        return false;
    }

    public WiFiProScoreInfo getCurrentWiFiProScoreInfo(String ssid) {
        if (!isEvaluateRecordsEmpty() && !TextUtils.isEmpty(ssid)) {
            return (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
        }
        Log.w(TAG, "getCurrentWifiProperties is null!");
        return null;
    }

    public static WiFiProScoreInfo getCurrentWiFiProScore(String ssid) {
        if (!isEvaluateRecordsEmpty() && !TextUtils.isEmpty(ssid)) {
            return (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
        }
        Log.w(TAG, "getCurrentWifiProperties is null!");
        return null;
    }

    public boolean isAbandonEvaluate(String ssid) {
        WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
        if (wiFiProScoreInfo == null) {
            return true;
        }
        if (calculateSignalLevelHW(wiFiProScoreInfo.is5GHz, wiFiProScoreInfo.rssi) >= 2) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ssid);
        stringBuilder.append(" rssi = ");
        stringBuilder.append(wiFiProScoreInfo.rssi);
        Log.d(str, stringBuilder.toString());
        return true;
    }

    public void cleanEvaluateRecords() {
        if (!(this.mUnEvaluateAPQueue == null || this.mUnEvaluateAPQueue.isEmpty())) {
            Log.d(TAG, "clean mUnEvaluateAPQueue");
            this.mUnEvaluateAPQueue.clear();
        }
        cleanEvaluateCacheRecords();
    }

    private void initEvaluateRecords() {
        if (mEvaluateAPHashMap == null) {
            mEvaluateAPHashMap = new HashMap();
        }
        if (this.mUnEvaluateAPQueue == null) {
            this.mUnEvaluateAPQueue = new LinkedList();
        }
        if (this.mUntrustedOpenApList == null) {
            this.mUntrustedOpenApList = new ArrayList();
        }
    }

    private void initEvaluateCacheRecords() {
        if (this.mSavedApList == null) {
            this.mSavedApList = new ArrayList();
        }
        if (this.mOpenApList == null) {
            this.mOpenApList = new ArrayList();
        }
    }

    public void cleanEvaluateCacheRecords() {
        if (this.mSavedApList != null) {
            this.mSavedApList.clear();
        }
        if (this.mOpenApList != null) {
            this.mOpenApList.clear();
        }
    }

    public boolean isSaveAP(ScanResult scanResult) {
        if (scanResult == null) {
            return false;
        }
        String ssid = new StringBuilder();
        ssid.append("\"");
        ssid.append(scanResult.SSID);
        ssid.append("\"");
        if (getWifiConfiguration(ssid.toString()) != null) {
            return true;
        }
        return false;
    }

    public boolean isAllowEvaluate(ScanResult scanResult, int evalate_type) {
        if (scanResult == null) {
            return false;
        }
        int level = 3;
        if (evalate_type == 1) {
            level = 2;
        }
        if (calculateSignalLevelHW(scanResult.is5GHz(), scanResult.level) < level) {
            return false;
        }
        String ssid = new StringBuilder();
        ssid.append("\"");
        ssid.append(scanResult.SSID);
        ssid.append("\"");
        if (getWifiConfiguration(ssid.toString()) == null && isOpenAP(scanResult)) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:19:0x004e, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:20:0x004f, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isOpenAP(ScanResult result) {
        if (result.capabilities.contains("WEP") || result.capabilities.contains("WAPI-PSK") || result.capabilities.contains("QUALCOMM-WAPI-PSK") || result.capabilities.contains("WAPI-CERT") || result.capabilities.contains("QUALCOMM-WAPI-CERT") || result.capabilities.contains("PSK") || result.capabilities.contains("EAP")) {
            return false;
        }
        return true;
    }

    public void initWifiProEvaluateRecords() {
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks != null) {
            for (WifiConfiguration wifiConfiguration : configNetworks) {
                if (!(wifiConfiguration.SSID == null || mEvaluateAPHashMap.containsKey(wifiConfiguration.SSID))) {
                    WiFiProScoreInfo info = new WiFiProScoreInfo();
                    info.ssid = wifiConfiguration.SSID;
                    if (wifiConfiguration.noInternetAccess) {
                        info.internetAccessType = 2;
                        mEvaluateAPHashMap.put(wifiConfiguration.SSID, info);
                    } else if (wifiConfiguration.portalNetwork) {
                        info.internetAccessType = 3;
                        mEvaluateAPHashMap.put(wifiConfiguration.SSID, info);
                    } else if (!wifiConfiguration.wifiProNoInternetAccess) {
                        info.internetAccessType = 4;
                        mEvaluateAPHashMap.put(wifiConfiguration.SSID, info);
                    }
                }
            }
            return;
        }
        Log.d(TAG, "configNetworks  == null ");
    }

    public boolean isAllowAutoEvaluate(List<ScanResult> ScanResultList) {
        if (ScanResultList == null || ScanResultList.isEmpty()) {
            return false;
        }
        for (ScanResult scanResult : ScanResultList) {
            String ssid = new StringBuilder();
            ssid.append("\"");
            ssid.append(scanResult.SSID);
            ssid.append("\"");
            ssid = ssid.toString();
            WifiConfiguration cfg = getWifiConfiguration(ssid);
            String str;
            StringBuilder stringBuilder;
            if (WifiProCommonUtils.isWifiSelfCuring() || (cfg != null && ((!cfg.noInternetAccess || WifiProCommonUtils.allowWifiConfigRecovery(cfg.internetHistory)) && !cfg.isTempCreated))) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(scanResult.SSID);
                stringBuilder.append(" , has save, not allow Evaluate************ ");
                Log.d(str, stringBuilder.toString());
                return false;
            } else if (WifiProCommonUtils.allowRecheckForNoInternet(cfg, scanResult, this.mContext)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("don't allow to auto evaluating because of background recheck for no internet, candidate = ");
                stringBuilder.append(ssid);
                Log.d(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:10:0x0048, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reSetNotAllowEvaluateRecord(ScanResult scanResult) {
        if (scanResult != null && !TextUtils.isEmpty(scanResult.SSID) && !isEvaluateRecordsEmpty()) {
            String ssid = new StringBuilder();
            ssid.append("\"");
            ssid.append(scanResult.SSID);
            ssid.append("\"");
            ssid = ssid.toString();
            if (updateScoreInfoLevel(ssid, 0)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reset ssid :");
                stringBuilder.append(ssid);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x008b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void addEvaluateRecords(WifiInfo wifiInfo, int evalate_type) {
        initEvaluateRecords();
        if (!(wifiInfo == null || TextUtils.isEmpty(wifiInfo.getSSID()))) {
            String ssid = wifiInfo.getSSID();
            if (!INVAILD_SSID.equals(ssid)) {
                WiFiProScoreInfo wiFiProScoreInfo;
                String str;
                StringBuilder stringBuilder;
                if (!mEvaluateAPHashMap.containsKey(ssid)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("new wiFiProScoreInfo wifiinfo ssid :");
                    stringBuilder.append(ssid);
                    Log.d(str, stringBuilder.toString());
                    wiFiProScoreInfo = new WiFiProScoreInfo(wifiInfo);
                } else if (!isLastEvaluateValid(wifiInfo, evalate_type)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("update  wiFiProScoreInfo wifiinfo ssid :");
                    stringBuilder.append(ssid);
                    Log.d(str, stringBuilder.toString());
                    wiFiProScoreInfo = (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
                    wiFiProScoreInfo.trusted = true;
                    wiFiProScoreInfo.evaluated = false;
                    wiFiProScoreInfo.invalid = true;
                    wiFiProScoreInfo.is5GHz = wifiInfo.is5GHz();
                    wiFiProScoreInfo.rssi = wifiInfo.getRssi();
                    wiFiProScoreInfo.lastUpdateTime = System.currentTimeMillis();
                } else {
                    return;
                }
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
            }
        }
    }

    /* JADX WARNING: Missing block: B:32:0x00ff, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void addEvaluateRecords(ScanResult scanResult, int evalate_type) {
        initEvaluateRecords();
        initEvaluateCacheRecords();
        if (!(scanResult == null || TextUtils.isEmpty(scanResult.SSID))) {
            String ssid = new StringBuilder();
            ssid.append("\"");
            ssid.append(scanResult.SSID);
            ssid.append("\"");
            ssid = ssid.toString();
            if (!INVAILD_SSID.equals(scanResult.SSID)) {
                WiFiProScoreInfo wiFiProScoreInfo;
                String str;
                StringBuilder stringBuilder;
                if (mEvaluateAPHashMap.containsKey(ssid)) {
                    wiFiProScoreInfo = (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
                    if (System.currentTimeMillis() - wiFiProScoreInfo.lastUpdateTime < 2000 && scanResult.level < wiFiProScoreInfo.rssi) {
                        return;
                    }
                    if (!isLastEvaluateValid(scanResult, evalate_type)) {
                        wiFiProScoreInfo.trusted = scanResult.untrusted ^ 1;
                        wiFiProScoreInfo.evaluated = false;
                        wiFiProScoreInfo.invalid = true;
                        wiFiProScoreInfo.rssi = scanResult.level;
                        wiFiProScoreInfo.is5GHz = scanResult.is5GHz();
                        wiFiProScoreInfo.lastUpdateTime = System.currentTimeMillis();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("update  wiFiProScoreInfo ScanResult ssid :");
                        stringBuilder.append(ssid);
                        stringBuilder.append(", rssi = ");
                        stringBuilder.append(scanResult.level);
                        Log.i(str, stringBuilder.toString());
                    } else {
                        return;
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("add  wiFiProScoreInfo ScanResult ssid :");
                stringBuilder.append(ssid);
                Log.d(str, stringBuilder.toString());
                wiFiProScoreInfo = new WiFiProScoreInfo(scanResult);
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                if (isSaveAP(scanResult)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(ssid);
                    stringBuilder.append(" is saved ap");
                    Log.d(str, stringBuilder.toString());
                    this.mSavedApList.add(wiFiProScoreInfo);
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(ssid);
                    stringBuilder.append(" is open ap");
                    Log.d(str, stringBuilder.toString());
                    this.mOpenApList.add(wiFiProScoreInfo);
                }
            }
        }
    }

    public List<ScanResult> scanResultListFilter(List<ScanResult> scanResultList) {
        if (scanResultList == null) {
            return null;
        }
        this.mUnRepetitionScanResultList = new ArrayList();
        this.mScanResultHashMap = new HashMap();
        for (ScanResult scanResult : scanResultList) {
            if (!TextUtils.isEmpty(scanResult.SSID)) {
                if (this.mScanResultHashMap.containsKey(scanResult.SSID)) {
                    ScanResult scan_result = (ScanResult) this.mScanResultHashMap.get(scanResult.SSID);
                    if (scan_result != null && scan_result.level < scanResult.level) {
                        this.mScanResultHashMap.put(scanResult.SSID, scanResult);
                    }
                } else {
                    this.mScanResultHashMap.put(scanResult.SSID, scanResult);
                }
            }
        }
        Iterator it = this.mScanResultHashMap.values().iterator();
        if (it != null) {
            while (it.hasNext()) {
                this.mUnRepetitionScanResultList.add((ScanResult) it.next());
            }
        }
        return this.mUnRepetitionScanResultList;
    }

    public void orderByRssi() {
        String str;
        StringBuilder stringBuilder;
        if (!(this.mSavedApList == null || this.mSavedApList.isEmpty())) {
            Collections.sort(this.mSavedApList);
            Collections.reverse(this.mSavedApList);
            try {
                for (WiFiProScoreInfo saveAPInfo : this.mSavedApList) {
                    if (!this.mUnEvaluateAPQueue.contains(saveAPInfo.ssid) && this.mUnEvaluateAPQueue.offer(saveAPInfo.ssid)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(saveAPInfo.ssid);
                        stringBuilder.append(" offer to Queue:  rssi : ");
                        stringBuilder.append(saveAPInfo.rssi);
                        Log.d(str, stringBuilder.toString());
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
            } catch (Throwable th) {
                this.mSavedApList.clear();
            }
            this.mSavedApList.clear();
        }
        if (this.mOpenApList != null && !this.mOpenApList.isEmpty()) {
            Collections.sort(this.mOpenApList);
            Collections.reverse(this.mOpenApList);
            try {
                for (WiFiProScoreInfo saveAPInfo2 : this.mOpenApList) {
                    if (!this.mUnEvaluateAPQueue.contains(saveAPInfo2.ssid) && this.mUnEvaluateAPQueue.offer(saveAPInfo2.ssid)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(saveAPInfo2.ssid);
                        stringBuilder.append(" offer to Queue:  rssi : ");
                        stringBuilder.append(saveAPInfo2.rssi);
                        Log.d(str, stringBuilder.toString());
                    }
                }
            } catch (Exception e2) {
                Log.w(TAG, e2.getMessage());
            } catch (Throwable th2) {
                this.mOpenApList.clear();
            }
            this.mOpenApList.clear();
        }
    }

    /* JADX WARNING: Missing block: B:30:0x0065, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateEvaluateRecords(List<ScanResult> scanResultList, int evaluate_type, String currentSsid) {
        if (scanResultList != null) {
            if (!scanResultList.isEmpty()) {
                initEvaluateRecords();
                for (ScanResult scanResult : scanResultList) {
                    if (!(scanResult == null || TextUtils.isEmpty(scanResult.SSID))) {
                        String ssid = new StringBuilder();
                        ssid.append("\"");
                        ssid.append(scanResult.SSID);
                        ssid.append("\"");
                        ssid = ssid.toString();
                        if (TextUtils.isEmpty(currentSsid) || !currentSsid.equals(ssid)) {
                            if (mEvaluateAPHashMap.containsKey(ssid) && !isLastEvaluateValid(scanResult, evaluate_type)) {
                                updateScoreInfoLevel(ssid, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean updateEvaluateRecords(String ssid, WiFiProScoreInfo wifiScoreProperties) {
        if (TextUtils.isEmpty(ssid) || isEvaluateRecordsEmpty()) {
            Log.w(TAG, "updateEvaluateRecords fail!");
            return false;
        }
        mEvaluateAPHashMap.put(ssid, wifiScoreProperties);
        return true;
    }

    public synchronized boolean isAccessAPOutOfRange(List<ScanResult> scanResultList) {
        if (scanResultList == null) {
            Log.w(TAG, "scanResultList is null, Access AP uut of range");
            return true;
        }
        for (ScanResult scanResult : scanResultList) {
            if (scanResult != null && isOpenAP(scanResult) && scanResult.internetAccessType == 4) {
                return false;
            }
        }
        Log.w(TAG, "scanResultList internetAccessType is not normal, Access AP out of range");
        return true;
    }

    public synchronized void increaseFailCounter(String ssid) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null) {
                wiFiProScoreInfo.failCounter++;
                wiFiProScoreInfo.lastScoreTime = System.currentTimeMillis();
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x004c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean updateScoreInfoType(String ssid, int internetAccessType) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null) {
                if (wiFiProScoreInfo.internetAccessType != internetAccessType) {
                    wiFiProScoreInfo.internetAccessType = internetAccessType;
                    if (internetAccessType != 1) {
                        wiFiProScoreInfo.failCounter = 0;
                    }
                    wiFiProScoreInfo.networkQosLevel = 0;
                    wiFiProScoreInfo.invalid = false;
                    wiFiProScoreInfo.evaluated = true;
                    wiFiProScoreInfo.abandon = false;
                    wiFiProScoreInfo.lastScoreTime = System.currentTimeMillis();
                    mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateEvaluateRecords internetAccessType succeed! new type =  ");
                    stringBuilder.append(internetAccessType);
                    Log.d(str, stringBuilder.toString());
                    return true;
                }
                Log.d(TAG, "internetAccessType not change, can not update");
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x004b, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean updateScoreInfoLevel(String ssid, int networkQosLevel) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null) {
                if (wiFiProScoreInfo.internetAccessType != 4) {
                    wiFiProScoreInfo.networkQosLevel = 0;
                } else {
                    wiFiProScoreInfo.networkQosLevel = networkQosLevel;
                }
                wiFiProScoreInfo.networkQosScore = WiFiProScoreInfo.calculateWiFiScore(wiFiProScoreInfo);
                wiFiProScoreInfo.invalid = false;
                wiFiProScoreInfo.evaluated = true;
                wiFiProScoreInfo.abandon = false;
                wiFiProScoreInfo.lastScoreTime = System.currentTimeMillis();
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateEvaluate Level  succeed!.new Level = ");
                stringBuilder.append(wiFiProScoreInfo.networkQosLevel);
                Log.d(str, stringBuilder.toString());
                return true;
            }
        }
    }

    public void updateWifiSecurityInfo(String ssid, int secStatus) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo scoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (scoreInfo != null) {
                scoreInfo.networkSecurity = secStatus;
            }
        }
    }

    public int getWifiSecurityInfo(String ssid) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo scoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (scoreInfo != null) {
                return scoreInfo.networkSecurity;
            }
        }
        return -1;
    }

    /* JADX WARNING: Missing block: B:16:0x002c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean updateScoreInfoAbandon(String ssid, boolean abandon) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (!(wiFiProScoreInfo == null || wiFiProScoreInfo.abandon == abandon)) {
                wiFiProScoreInfo.abandon = abandon;
                if (abandon) {
                    wiFiProScoreInfo.networkQosLevel = 0;
                    wiFiProScoreInfo.internetAccessType = 0;
                }
                wiFiProScoreInfo.invalid = false;
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                Log.d(TAG, "updateEvaluateRecords abandon succeed!");
                return true;
            }
        }
    }

    public synchronized boolean updateWifiProbeMode(String ssid, int mode) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null) {
                wiFiProScoreInfo.probeMode = mode;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("*****updateWifiProbeMode, new probeMode  ");
                stringBuilder.append(mode);
                Log.d(str, stringBuilder.toString());
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:11:0x0023, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean updateScoreEvaluateStatus(String ssid, boolean evaluate) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null) {
                wiFiProScoreInfo.evaluated = evaluate;
                wiFiProScoreInfo.invalid = false;
                wiFiProScoreInfo.lastScoreTime = System.currentTimeMillis();
                wiFiProScoreInfo.abandon = false;
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                return true;
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x003b, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean updateScoreEvaluateInvalid(String ssid, boolean invalid) {
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null && wiFiProScoreInfo.evaluated) {
                wiFiProScoreInfo.invalid = invalid;
                if (invalid) {
                    wiFiProScoreInfo.evaluated = false;
                }
                mEvaluateAPHashMap.put(ssid, wiFiProScoreInfo);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateEvaluate Invalid succeed!, wiFiProScoreInfo ");
                stringBuilder.append(wiFiProScoreInfo.dump());
                Log.d(str, stringBuilder.toString());
                return true;
            }
        }
    }

    public synchronized void reSetEvaluateRecord(String ssid) {
        if (!TextUtils.isEmpty(ssid)) {
            if (mEvaluateAPHashMap != null && mEvaluateAPHashMap.containsKey(ssid)) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" remove ");
                    stringBuilder.append(ssid);
                    stringBuilder.append(" form EvaluateRecord");
                    Log.d(str, stringBuilder.toString());
                    mEvaluateAPHashMap.remove(ssid);
                } catch (UnsupportedOperationException e) {
                    Log.w(TAG, " unsupportedOperationException ");
                }
            }
        } else {
            return;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x002b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void reSetEvaluateRecord(Intent intent) {
        if (intent != null) {
            WifiConfiguration config = (WifiConfiguration) intent.getParcelableExtra("wifiConfiguration");
            int changereson = intent.getIntExtra("changeReason", -1);
            if (!(config == null || TextUtils.isEmpty(config.SSID) || changereson != 1 || config.isTempCreated)) {
                reSetEvaluateRecord(config.SSID);
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0039, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:17:0x003b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void restorePortalEvaluateRecord(String currSsid) {
        if (!TextUtils.isEmpty(currSsid)) {
            if (!INVAILD_SSID.equals(currSsid)) {
                WiFiProScoreInfo scoreInfo = getCurrentWiFiProScoreInfo(currSsid);
                if (!(scoreInfo == null || scoreInfo.internetAccessType == 0 || scoreInfo.internetAccessType == 3)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("restore to portal, ssid = ");
                    stringBuilder.append(currSsid);
                    Log.d(str, stringBuilder.toString());
                    updateScoreInfoType(currSsid, 3);
                }
            }
        }
    }

    public String getNextEvaluateWiFiSSID() {
        if (this.mUnEvaluateAPQueue == null || this.mUnEvaluateAPQueue.isEmpty()) {
            return null;
        }
        return (String) this.mUnEvaluateAPQueue.poll();
    }

    public boolean connectWifi(String ssid) {
        WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
        if (wiFiProScoreInfo == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ssid);
            stringBuilder.append("conenct  connect fail");
            Log.d(str, stringBuilder.toString());
            evaluateAPHashMapDump();
            return false;
        }
        WifiConfiguration wifiConfiguration = getWifiConfiguration(ssid);
        if (wifiConfiguration == null) {
            if (wiFiProScoreInfo.trusted) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(ssid);
                stringBuilder2.append(" trusted : ");
                stringBuilder2.append(wiFiProScoreInfo.trusted);
                Log.d(str2, stringBuilder2.toString());
            }
            Log.d(TAG, "conenct open ap, create a new confg");
            wifiConfiguration = createOpenWifiConfiguration(ssid);
        }
        if (HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(wifiConfiguration, false)) {
            Log.w(TAG, "MDM deny connect!");
            return false;
        }
        this.mWifiManager.connect(wifiConfiguration, null);
        return true;
    }

    public void addUntrustedOpenApList(String ssid) {
        if (this.mUntrustedOpenApList != null && ssid != null && !this.mUntrustedOpenApList.contains(ssid)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("add: ");
            stringBuilder.append(ssid);
            stringBuilder.append(" to  UntrustedOpenApList ");
            Log.d(str, stringBuilder.toString());
            this.mUntrustedOpenApList.add(ssid);
        }
    }

    public void clearUntrustedOpenApList() {
        if (this.mUntrustedOpenApList != null && !this.mUntrustedOpenApList.isEmpty()) {
            this.mUntrustedOpenApList.clear();
        }
    }

    public void forgetUntrustedOpenAp() {
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks != null) {
            for (WifiConfiguration wifiConfiguration : configNetworks) {
                if (wifiConfiguration.isTempCreated) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isTempCreated, forget: ");
                    stringBuilder.append(wifiConfiguration.SSID);
                    stringBuilder.append(" config ");
                    Log.d(str, stringBuilder.toString());
                    this.mWifiManager.forget(wifiConfiguration.networkId, null);
                }
            }
            if (this.mUntrustedOpenApList != null && this.mUntrustedOpenApList.isEmpty()) {
                this.mUntrustedOpenApList.clear();
            }
        }
    }

    private WifiConfiguration createOpenWifiConfiguration(String ssid) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = ssid;
        config.allowedKeyManagement.set(0);
        config.isTempCreated = true;
        String oriSsid = getOriSsid(ssid);
        if (TextUtils.isEmpty(oriSsid)) {
            Log.d(TAG, "oriSsid is null");
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ssid:");
            stringBuilder.append(ssid);
            stringBuilder.append(", oriSsid:");
            stringBuilder.append(oriSsid);
            Log.d(str, stringBuilder.toString());
            config.oriSsid = oriSsid;
        }
        return config;
    }

    private String getOriSsid(String ssid) {
        if (TextUtils.isEmpty(ssid) || this.mWifiManager == null) {
            return null;
        }
        List<ScanResult> resultLists = this.mWifiManager.getScanResults();
        if (resultLists == null) {
            return null;
        }
        for (ScanResult scanResult : resultLists) {
            String scanResultSSID = new StringBuilder();
            scanResultSSID.append("\"");
            scanResultSSID.append(scanResult.SSID);
            scanResultSSID.append("\"");
            if (scanResultSSID.toString().equals(ssid) && scanResult.wifiSsid != null) {
                return scanResult.wifiSsid.oriSsid;
            }
        }
        return null;
    }

    public WifiConfiguration getWifiConfiguration(String ssid) {
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks != null) {
            for (WifiConfiguration wifiConfiguration : configNetworks) {
                if (wifiConfiguration.SSID != null && wifiConfiguration.SSID.equals(ssid)) {
                    return wifiConfiguration;
                }
            }
        } else {
            Log.d(TAG, "configNetworks  == null ");
        }
        return null;
    }

    public int calculateSignalLevelHW(boolean is5G, int rssi) {
        return HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(is5G ? 5180 : 2412, rssi);
    }

    /* JADX WARNING: Missing block: B:26:0x004a, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int calculateTestWiFiLevel(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            Log.d(TAG, "calculateTestWiFiLevel ssid == null ");
            return 0;
        } else if (!mEvaluateAPHashMap.containsKey(ssid)) {
            return 0;
        } else {
            WiFiProScoreInfo properties = (WiFiProScoreInfo) mEvaluateAPHashMap.get(ssid);
            if (properties == null || properties.abandon || 2 == properties.internetAccessType || 3 == properties.internetAccessType) {
                return 0;
            }
            int newSignalLevel = calculateSignalLevelHW(properties.is5GHz, properties.rssi);
            int boost_5G = 0;
            if (properties.is5GHz) {
                boost_5G = 1;
            }
            int level = newSignalLevel + boost_5G;
            if (level > 3) {
                return 3;
            }
            if (level > 1) {
                return 2;
            }
            return 1;
        }
    }

    public synchronized int getOldNetworkType(String ssid) {
        int oldNetworkType;
        oldNetworkType = 0;
        if (!TextUtils.isEmpty(ssid)) {
            WiFiProScoreInfo wiFiProScoreInfo = getCurrentWiFiProScoreInfo(ssid);
            if (wiFiProScoreInfo != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("probeMode: ");
                stringBuilder.append(wiFiProScoreInfo.probeMode);
                Log.d(str, stringBuilder.toString());
            }
            if (wiFiProScoreInfo != null && wiFiProScoreInfo.probeMode == 1) {
                oldNetworkType = wiFiProScoreInfo.internetAccessType;
            }
        }
        return oldNetworkType;
    }

    public synchronized int getNewNetworkType(int checkResult) {
        int newNetworkType;
        newNetworkType = 0;
        if (5 == checkResult) {
            newNetworkType = 4;
        } else if (6 == checkResult) {
            newNetworkType = 3;
        } else if (-1 == checkResult) {
            newNetworkType = 2;
        }
        return newNetworkType;
    }

    /* JADX WARNING: Missing block: B:35:0x0043, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:37:0x0045, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int getChrDiffType(int oldType, int newType) {
        int diffType = 0;
        if (oldType != 0 && oldType != newType) {
            if (oldType == 4 && newType == 2) {
                diffType = 1;
            } else if (oldType == 4 && newType == 3) {
                diffType = 2;
            } else if (oldType == 2 && newType == 4) {
                diffType = 3;
            } else if (oldType == 2 && newType == 3) {
                diffType = 4;
            } else if (oldType == 3 && newType == 4) {
                diffType = 5;
            } else if (oldType == 3 && newType == 2) {
                diffType = 6;
            } else if (oldType == 1 && newType == 4) {
                diffType = 7;
            } else if (oldType == 1 && newType == 2) {
                diffType = 8;
            } else if (oldType == 1 && newType == 3) {
                diffType = 9;
            }
        }
    }
}
