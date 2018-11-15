package com.android.server.wifi.wifipro.hwintelligencewifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ApInfoManager {
    public static final int AUTO_OPEN_RSSI_VALUE = -75;
    public static final int MSG_SCAN_AGAIN = 3;
    public static final int MSG_START_SCAN = 1;
    public static final int MSG_UPDATE_SCAN_RESULT = 2;
    public static final int SCAN_INTERVAL_NORMAL_1 = 60000;
    public static final int SCAN_INTERVAL_NORMAL_3 = 180000;
    public static final int SCAN_INTERVAL_NORMAL_5 = 300000;
    public static final int SCAN_INTERVAL_SHORT = 20000;
    public static final int SCAN_TYPE_FINE_MIN = 3;
    public static final int SCAN_TYPE_ONE_MIN = 1;
    public static final int SCAN_TYPE_SHORT = 0;
    public static final int SCAN_TYPE_THREE_MIN = 2;
    private Context mContext;
    private DataBaseManager mDbManager;
    private Handler mHandler;
    private HwintelligenceWiFiCHR mHwintelligenceWiFiCHR;
    private List<APInfoData> mInfos = new ArrayList();
    private boolean mIsRunning = false;
    private boolean mIsScanAlwaysAvailable = false;
    private boolean mIsScanInShort = false;
    private boolean mIsScaning = false;
    private Object mLock = new Object();
    private int mScanTimes = 0;
    private int mScanTotleTimes = 20;
    private int mScanType = 1;
    private HwIntelligenceStateMachine mStateMachine;
    private Handler mWifiHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    Log.e(MessageUtil.TAG, "MSG_UPDATE_SCAN_RESULT");
                    List<ScanResult> mLists = ApInfoManager.this.mWifiManager.getScanResults();
                    if (mLists != null) {
                        String str = MessageUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mLists.size() = ");
                        stringBuilder.append(mLists.size());
                        Log.e(str, stringBuilder.toString());
                    }
                    if (!ApInfoManager.this.handleScanResult(mLists)) {
                        ApInfoManager.this.mScanTimes = ApInfoManager.this.mScanTimes + 1;
                        if (ApInfoManager.this.mScanTimes <= ApInfoManager.this.mScanTotleTimes) {
                            if (!(mLists == null || !ApInfoManager.this.isInMonitorNearbyAp(mLists) || ApInfoManager.this.mIsScanInShort)) {
                                ApInfoManager.this.mScanType = 0;
                                ApInfoManager.this.mIsScanInShort = true;
                            }
                            ApInfoManager.this.setScanInterval(ApInfoManager.this.mScanType);
                            return;
                        } else if (ApInfoManager.this.mScanType == 3) {
                            ApInfoManager.this.stopScanAp();
                            return;
                        } else {
                            ApInfoManager.this.mScanType = ApInfoManager.this.mScanType + 1;
                            ApInfoManager.this.mScanTimes = 0;
                            String str2 = MessageUtil.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("MSG_UPDATE_SCAN_RESULT set scan Interval mScanType = ");
                            stringBuilder2.append(ApInfoManager.this.mScanType);
                            Log.e(str2, stringBuilder2.toString());
                            ApInfoManager.this.setScanInterval(ApInfoManager.this.mScanType);
                            return;
                        }
                    }
                    return;
                case 3:
                    Log.e(MessageUtil.TAG, "MSG_SCAN_AGAIN");
                    removeMessages(3);
                    ApInfoManager.this.setScanAlwaysEnable();
                    return;
                default:
                    return;
            }
        }
    };
    private WifiManager mWifiManager;

    public ApInfoManager(Context context, HwIntelligenceStateMachine stateMachine, Handler handler) {
        this.mContext = context;
        this.mStateMachine = stateMachine;
        this.mHandler = handler;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mHwintelligenceWiFiCHR = HwintelligenceWiFiCHR.getInstance(stateMachine);
        resetScanAlwaysEnable();
    }

    public void start() {
        synchronized (this.mLock) {
            this.mDbManager = new DataBaseManager(this.mContext);
            this.mInfos = this.mDbManager.getAllApInfos();
            this.mIsRunning = true;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void stop() {
        synchronized (this.mLock) {
            if (!this.mIsRunning || this.mDbManager == null) {
            } else {
                stopScanAp();
                this.mDbManager.closeDB();
                this.mIsRunning = false;
            }
        }
    }

    public void addCurrentApInfo(String cellid) {
        synchronized (this.mLock) {
            if (cellid == null) {
                return;
            }
            inlineAddCurrentApInfo(cellid);
        }
    }

    private void inlineAddCurrentApInfo(String cellid) {
        if (this.mIsRunning) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addCurrentApInfo cellid =");
            stringBuilder.append(cellid);
            Log.e(str, stringBuilder.toString());
            WifiInfo Info = this.mWifiManager.getConnectionInfo();
            if (Info == null || Info.getBSSID() == null || cellid == null || Info.getBSSID().equals("any") || Info.getBSSID().equals(MessageUtil.ILLEGAL_BSSID_02)) {
                Log.e(MessageUtil.TAG, "inlineAddCurrentApInfo invalid AP info");
                return;
            }
            APInfoData data = getApInfo(Info.getBSSID());
            if (data == null) {
                APInfoData oldestData;
                Log.e(MessageUtil.TAG, "addCurrentApInfo addApInfos");
                long mTime = System.currentTimeMillis();
                int mAuthType = getAuthType(Info.getNetworkId());
                if (this.mInfos.size() == 499) {
                    this.mHwintelligenceWiFiCHR.uploadWhiteNum((short) 500);
                }
                if (this.mInfos.size() >= 500) {
                    oldestData = getOldestApInfoData();
                    if (oldestData != null) {
                        this.mDbManager.delAPInfos(oldestData.getBssid());
                        this.mInfos.remove(oldestData);
                    }
                }
                this.mDbManager.addApInfos(Info.getBSSID(), Info.getSSID(), cellid, getAuthType(Info.getNetworkId()));
                oldestData = new APInfoData(Info.getBSSID(), Info.getSSID(), 0, mAuthType, mTime, 0);
                List<CellInfoData> cellInfos = this.mDbManager.queryCellInfoByBssid(oldestData.getBssid());
                if (cellInfos.size() != 0) {
                    oldestData.setCellInfo(cellInfos);
                }
                List<String> nearbyApInfosList = this.mDbManager.getNearbyApInfo(oldestData.getBssid());
                if (nearbyApInfosList.size() != 0) {
                    oldestData.setNearbyAPInfos(nearbyApInfosList);
                }
                this.mInfos.add(oldestData);
            } else if (data.getCellInfos().size() >= 50) {
                Log.e(MessageUtil.TAG, "addCurrentApInfo MessageUtil.DB_NEARBY_CELLID_MAX_QUANTA");
                this.mDbManager.delAPInfos(data.getBssid());
                this.mInfos.remove(data);
            } else {
                String str2;
                StringBuilder stringBuilder2;
                int authtype = getAuthType(Info.getNetworkId());
                if (isCellIdExit(data, cellid)) {
                    Log.e(MessageUtil.TAG, "addCurrentApInfo info is already there");
                } else {
                    Log.e(MessageUtil.TAG, "addCurrentApInfo addCellIdInfo");
                    this.mDbManager.addCellInfo(Info.getBSSID(), cellid);
                    this.mDbManager.addNearbyApInfo(Info.getBSSID());
                    List<CellInfoData> cellInfos2 = this.mDbManager.queryCellInfoByBssid(data.getBssid());
                    if (cellInfos2.size() != 0) {
                        data.setCellInfo(cellInfos2);
                    }
                    List<String> nearbyApInfosList2 = this.mDbManager.getNearbyApInfo(data.getBssid());
                    if (nearbyApInfosList2.size() != 0) {
                        data.setNearbyAPInfos(nearbyApInfosList2);
                    }
                }
                this.mDbManager.updateBssidTimer(Info.getBSSID());
                data.setLastTime(System.currentTimeMillis());
                if (!data.getSsid().equals(Info.getSSID())) {
                    str2 = MessageUtil.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("inlineAddCurrentApInfo updateSsid  data.getSsid() = ");
                    stringBuilder2.append(data.getSsid());
                    stringBuilder2.append("  Info.getSSID() = ");
                    stringBuilder2.append(Info.getSSID());
                    Log.d(str2, stringBuilder2.toString());
                    this.mDbManager.updateSsid(Info.getBSSID(), Info.getSSID());
                    data.setSsid(Info.getSSID());
                }
                if (data.getAuthType() != authtype) {
                    str2 = MessageUtil.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("inlineAddCurrentApInfo updateSsid  data.getAuthType() = ");
                    stringBuilder2.append(data.getAuthType());
                    stringBuilder2.append("  authtype = ");
                    stringBuilder2.append(authtype);
                    Log.d(str2, stringBuilder2.toString());
                    this.mDbManager.updateAuthType(Info.getBSSID(), authtype);
                    data.setAuthType(authtype);
                }
            }
            String str3 = MessageUtil.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("inlineAddCurrentApInfo mInfos.size()=");
            stringBuilder3.append(this.mInfos.size());
            Log.e(str3, stringBuilder3.toString());
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0026, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updataApInfo(String cellid) {
        synchronized (this.mLock) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updataApInfo cellid =");
            stringBuilder.append(cellid);
            Log.e(str, stringBuilder.toString());
            if (!this.mIsRunning || cellid == null) {
            } else {
                inlineAddCurrentApInfo(cellid);
            }
        }
    }

    public void updateCurrentApHomebySsid(String ssid, int authtype, boolean isHome) {
        boolean value = isHome;
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCurrentApHomebySsid ssid = ");
        stringBuilder.append(ssid);
        stringBuilder.append(", isHome = ");
        stringBuilder.append(isHome);
        Log.d(str, stringBuilder.toString());
        for (APInfoData info : this.mInfos) {
            if (info.getSsid().equals(ssid) && info.getAuthType() == authtype && info.isHomeAp() != isHome) {
                info.setHomeAp(isHome);
                this.mDbManager.updateBssidIsHome(info.getBssid(), value);
            }
        }
    }

    public void startScanAp() {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startScanAp mIsScaning = ");
        stringBuilder.append(this.mIsScaning);
        Log.e(str, stringBuilder.toString());
        if (!(this.mIsScaning || this.mWifiManager == null)) {
            if (this.mWifiManager.isWifiEnabled()) {
                Log.e(MessageUtil.TAG, "startScanAp wifi is opened");
                stopScanAp();
            } else {
                this.mIsScaning = true;
                this.mScanTimes = 0;
                this.mScanType = 1;
                this.mIsScanAlwaysAvailable = this.mWifiManager.isScanAlwaysAvailable();
                str = MessageUtil.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startScanAp isScanAlwaysAvailable  = ");
                stringBuilder.append(this.mIsScanAlwaysAvailable);
                Log.w(str, stringBuilder.toString());
                if (this.mIsScanAlwaysAvailable) {
                    this.mWifiManager.startScan();
                }
            }
        }
    }

    public void stopScanAp() {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopScanAp mIsScaning = ");
        stringBuilder.append(this.mIsScaning);
        Log.e(str, stringBuilder.toString());
        if (this.mIsScaning) {
            resetScanAlwaysEnable();
        }
        this.mIsScaning = false;
        this.mIsScanInShort = false;
        this.mScanTimes = 0;
        this.mScanType = 1;
        this.mWifiHandler.removeMessages(3);
    }

    public boolean isScaning() {
        return this.mIsScaning;
    }

    private void setScanAlwaysEnable() {
        if (this.mScanType == 3 || this.mScanType == 2) {
            this.mIsScanAlwaysAvailable = this.mWifiManager.isScanAlwaysAvailable();
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isScanAlwaysAvailable  = ");
            stringBuilder.append(this.mIsScanAlwaysAvailable);
            Log.e(str, stringBuilder.toString());
            if (this.mIsScanAlwaysAvailable) {
                this.mWifiManager.startScan();
                return;
            }
            return;
        }
        this.mWifiManager.startScan();
    }

    private void resetScanAlwaysEnable() {
    }

    public void updateScanResult() {
        if (this.mIsScaning) {
            this.mWifiHandler.sendEmptyMessage(2);
        }
    }

    public boolean processScanResult(String cellid) {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processScanResult cellid = ");
        stringBuilder.append(cellid);
        Log.e(str, stringBuilder.toString());
        List<ScanResult> mLists = this.mWifiManager.getScanResults();
        if (mLists != null) {
            String str2 = MessageUtil.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mLists.size() = ");
            stringBuilder2.append(mLists.size());
            Log.e(str2, stringBuilder2.toString());
        }
        boolean checkResult = false;
        synchronized (this.mLock) {
            if (mLists != null) {
                if (mLists.size() > 0) {
                    for (ScanResult result : mLists) {
                        APInfoData data = getApInfo(result.BSSID);
                        if (data != null) {
                            if (isCellIdExit(data, cellid)) {
                                Log.e(MessageUtil.TAG, "addCurrentApInfo info is already there");
                            } else {
                                Log.e(MessageUtil.TAG, "addCurrentApInfo addCellIdInfo");
                                this.mDbManager.addCellInfo(data.getBssid(), cellid);
                                checkResult = true;
                                List<CellInfoData> cellInfos = this.mDbManager.queryCellInfoByBssid(data.getBssid());
                                if (cellInfos.size() != 0) {
                                    data.setCellInfo(cellInfos);
                                }
                            }
                        }
                    }
                }
            }
        }
        return checkResult;
    }

    public APInfoData getApInfoByBssid(String bssid) {
        synchronized (this.mLock) {
            if (bssid == null) {
                return null;
            }
            APInfoData apInfo = getApInfo(bssid);
            return apInfo;
        }
    }

    private APInfoData getApInfo(String bssid) {
        for (APInfoData info : this.mInfos) {
            if (info.getBssid().equals(bssid)) {
                return info;
            }
        }
        return null;
    }

    private boolean isCellIdExit(APInfoData info, String cellid) {
        for (CellInfoData data : info.getCellInfos()) {
            if (data.getCellid().equals(cellid)) {
                return true;
            }
        }
        return false;
    }

    private void setScanInterval(int scanType) {
        int scanInterval;
        switch (scanType) {
            case 0:
                scanInterval = 20000;
                break;
            case 1:
                scanInterval = 60000;
                break;
            case 2:
                scanInterval = SCAN_INTERVAL_NORMAL_3;
                resetScanAlwaysEnable();
                break;
            case 3:
                scanInterval = 300000;
                resetScanAlwaysEnable();
                break;
            default:
                scanInterval = 60000;
                break;
        }
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setScanInterval scanInterval = ");
        stringBuilder.append(scanInterval);
        Log.e(str, stringBuilder.toString());
        this.mWifiHandler.sendEmptyMessageDelayed(3, (long) scanInterval);
    }

    /* JADX WARNING: Missing block: B:12:0x0064, code:
            if (r6.equals(r7.toString()) != false) goto L_0x0066;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean handleScanResult(List<ScanResult> lists) {
        boolean hasApInBlackList = false;
        boolean hasTargetAp = false;
        if (lists == null) {
            return false;
        }
        for (ScanResult data : lists) {
            APInfoData apInfo = getApInfo(data.BSSID);
            String str = data.BSSID;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            stringBuilder.append(data.SSID);
            stringBuilder.append("\"");
            if (!isInTargetAp(str, stringBuilder.toString())) {
                if (!(apInfo == null || apInfo.getSsid() == null)) {
                    str = apInfo.getSsid();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(data.SSID);
                    stringBuilder.append("\"");
                }
            }
            if (isInBlackList(data.BSSID)) {
                String str2 = MessageUtil.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleScanResult AP in balcklist SSID = ");
                stringBuilder2.append(data.SSID);
                Log.d(str2, stringBuilder2.toString());
                hasApInBlackList = true;
                break;
            }
            str = data.BSSID;
            stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            stringBuilder.append(data.SSID);
            stringBuilder.append("\"");
            if (!isInTargetAp(str, stringBuilder.toString())) {
                str = this.mStateMachine.getCellStateMonitor().getCurrentCellid();
                if (!(apInfo == null || str == null)) {
                    inlineUpdataApCellInfo(apInfo, str);
                    List<APInfoData> targetDataList = this.mStateMachine.getTargetApInfoDatas();
                    if (targetDataList != null) {
                        targetDataList.add(apInfo);
                    }
                }
            }
            if (data.level >= -75) {
                hasTargetAp = true;
            } else {
                str = MessageUtil.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleScanResult AP RSSI is weak SSID = ");
                stringBuilder.append(data.SSID);
                stringBuilder.append(" BSSID = ");
                stringBuilder.append(partDisplayBssid(data.BSSID));
                stringBuilder.append(" data.level = ");
                stringBuilder.append(data.level);
                Log.e(str, stringBuilder.toString());
            }
        }
        if (hasApInBlackList) {
            Log.d(MessageUtil.TAG, "Has tartget in blacklist, update record.");
            resetBlackList(lists, true);
            return true;
        } else if (!hasTargetAp) {
            return false;
        } else {
            Log.d(MessageUtil.TAG, "handleScanResult find target AP!");
            this.mHandler.sendEmptyMessage(5);
            return true;
        }
    }

    private boolean isInTargetAp(String bssid, String ssid) {
        List<APInfoData> targetDataList = this.mStateMachine.getTargetApInfoDatas();
        if (targetDataList == null || targetDataList.size() == 0) {
            return false;
        }
        for (APInfoData data : targetDataList) {
            if (data.getBssid().equals(bssid) && data.getSsid() != null && data.getSsid().equals(ssid)) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:18:0x0046, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isMonitorCellId(String cellid) {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isMonitorCellId mInfos.size() = ");
        stringBuilder.append(this.mInfos.size());
        Log.e(str, stringBuilder.toString());
        synchronized (this.mLock) {
            if (!this.mIsRunning || cellid == null) {
            } else {
                for (APInfoData info : this.mInfos) {
                    if (isCellIdExit(info, cellid)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public List<APInfoData> getMonitorDatas(String cellid) {
        synchronized (this.mLock) {
            List<APInfoData> datas = new ArrayList();
            if (this.mIsRunning) {
                for (APInfoData info : this.mInfos) {
                    if (isCellIdExit(info, cellid)) {
                        datas.add(info);
                    }
                }
                return datas;
            }
            return datas;
        }
    }

    public boolean isInBlackList(String bssid) {
        synchronized (this.mLock) {
            if (bssid == null) {
                return false;
            }
            APInfoData data = getApInfo(bssid);
            if (data != null) {
                boolean isInBlackList = data.isInBlackList();
                return isInBlackList;
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0052, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void delectApInfoByBssid(String bssid) {
        synchronized (this.mLock) {
            if (this.mIsRunning) {
                String str = MessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("delectApInfoByBssid mInfos.size = ");
                stringBuilder.append(this.mInfos.size());
                Log.e(str, stringBuilder.toString());
                APInfoData data = getApInfo(bssid);
                if (data != null) {
                    this.mDbManager.delAPInfos(bssid);
                    this.mInfos.remove(data);
                    String str2 = MessageUtil.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("delectApInfoByBssid mInfos.size()=");
                    stringBuilder2.append(this.mInfos.size());
                    Log.e(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public void delectApInfoBySsid(String ssid) {
        synchronized (this.mLock) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getApInfoBySsid mInfos.size = ");
            stringBuilder.append(this.mInfos.size());
            Log.e(str, stringBuilder.toString());
            if (this.mIsRunning) {
                List<APInfoData> delList = new ArrayList();
                for (APInfoData info : this.mInfos) {
                    if (info.getSsid().equals(ssid) && !isInConfigNetworks(ssid, info.getAuthType())) {
                        String str2 = MessageUtil.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("delectApInfoBySsid  ssid = ");
                        stringBuilder2.append(ssid);
                        stringBuilder2.append("  info.getAuthType() = ");
                        stringBuilder2.append(info.getAuthType());
                        Log.e(str2, stringBuilder2.toString());
                        this.mDbManager.delAPInfos(info.getBssid());
                        delList.add(info);
                    }
                }
                if (delList.size() > 0) {
                    for (APInfoData info2 : delList) {
                        this.mInfos.remove(info2);
                    }
                }
                String str3 = MessageUtil.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("delectApInfoBySsid mInfos.size()=");
                stringBuilder3.append(this.mInfos.size());
                Log.e(str3, stringBuilder3.toString());
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:29:0x00d0, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void delectApInfoBySsidForPortal(WifiInfo mConnectionInfo) {
        synchronized (this.mLock) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("delectApInfoBySsidForPortal mInfos.size = ");
            stringBuilder.append(this.mInfos.size());
            Log.e(str, stringBuilder.toString());
            if (!this.mIsRunning) {
            } else if (mConnectionInfo == null || mConnectionInfo.getBSSID() == null) {
            } else {
                List<APInfoData> delList = new ArrayList();
                for (APInfoData info : this.mInfos) {
                    if (info.getSsid().equals(mConnectionInfo.getSSID()) && info.getAuthType() == getAuthType(mConnectionInfo.getNetworkId())) {
                        String str2 = MessageUtil.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("delectApInfoBySsid\tssid = ");
                        stringBuilder2.append(info.getSsid());
                        stringBuilder2.append("  info.getAuthType() = ");
                        stringBuilder2.append(info.getAuthType());
                        Log.e(str2, stringBuilder2.toString());
                        this.mDbManager.delAPInfos(info.getBssid());
                        delList.add(info);
                    }
                }
                if (delList.size() > 0) {
                    for (APInfoData info2 : delList) {
                        this.mInfos.remove(info2);
                    }
                }
                String str3 = MessageUtil.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("delectApInfoBySsidForPortal mInfos.size()=");
                stringBuilder3.append(this.mInfos.size());
                Log.e(str3, stringBuilder3.toString());
            }
        }
    }

    private boolean isInMonitorNearbyAp(List<ScanResult> lists) {
        List<APInfoData> targetDataList = this.mStateMachine.getTargetApInfoDatas();
        if (targetDataList == null || targetDataList.size() == 0) {
            return false;
        }
        for (ScanResult data : lists) {
            for (APInfoData targetData : targetDataList) {
                if (targetData.getNearbyAPInfos().contains(data.BSSID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private APInfoData getOldestApInfoData() {
        if (this.mInfos.size() <= 0) {
            return null;
        }
        APInfoData oldestData = (APInfoData) this.mInfos.get(0);
        for (APInfoData data : this.mInfos) {
            if (data.getLastTime() < oldestData.getLastTime()) {
                oldestData = data;
            }
        }
        return oldestData;
    }

    public boolean handleAutoScanResult(List<ScanResult> lists) {
        Log.e(MessageUtil.TAG, "handleAutoScanResult enter");
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            Log.e(MessageUtil.TAG, "handleAutoScanResult configNetworks == null || configNetworks.size()");
            return false;
        }
        for (ScanResult scanResult : lists) {
            if (scanResult.capabilities != null) {
                String capStr = getCapString(scanResult.capabilities);
                for (WifiConfiguration config : configNetworks) {
                    boolean found = false;
                    String SSID = new StringBuilder();
                    SSID.append("\"");
                    SSID.append(scanResult.SSID);
                    SSID.append("\"");
                    SSID = SSID.toString();
                    if (config.SSID != null && config.SSID.equals(SSID)) {
                        if (!config.isTempCreated) {
                            if (!isValid(config)) {
                                found = true;
                            } else if (capStr.equals("NONE") && config.getAuthType() == 0) {
                                found = true;
                            } else if (config.configKey().contains(capStr)) {
                                found = true;
                            }
                            if (found) {
                                String str = MessageUtil.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("handleAutoScanResult scanResult.SSID = ");
                                stringBuilder.append(scanResult.SSID);
                                Log.e(str, stringBuilder.toString());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        Log.e(MessageUtil.TAG, "handleAutoScanResult return false");
        return false;
    }

    public List<APInfoData> getDatasByCellId(String preCellID, String cellid) {
        List<APInfoData> datas;
        synchronized (this.mLock) {
            datas = new ArrayList();
            for (APInfoData info : this.mInfos) {
                if (isCellIdExit(info, preCellID) && isCellIdExit(info, cellid)) {
                    datas.add(info);
                }
            }
        }
        return datas;
    }

    private String getCapString(String cap) {
        if (cap == null) {
            return "NULL";
        }
        String capStr;
        if (cap.contains("WEP")) {
            capStr = "WEP";
        } else if (cap.contains("PSK")) {
            capStr = "PSK";
        } else if (cap.contains("EAP")) {
            capStr = "EAP";
        } else {
            capStr = "NONE";
        }
        return capStr;
    }

    public void resetBlackList(List<ScanResult> mLists, boolean isAddtoBlack) {
        synchronized (this.mLock) {
            if (mLists != null) {
                if (mLists.size() > 0) {
                    String str = MessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("processManualClose mLists.size() = ");
                    stringBuilder.append(mLists.size());
                    Log.e(str, stringBuilder.toString());
                    for (ScanResult result : mLists) {
                        APInfoData data = getApInfo(result.BSSID);
                        if (!(data == null || data.isInBlackList() == isAddtoBlack)) {
                            setBlackListBySsid(data.getSsid(), data.getAuthType(), isAddtoBlack);
                        }
                    }
                }
            }
            Log.e(MessageUtil.TAG, "processManualClose mLists = null");
        }
    }

    public void setBlackListBySsid(String ssid, int authtype, boolean isAddtoBlack) {
        boolean value = isAddtoBlack;
        for (APInfoData info : this.mInfos) {
            if (info.getSsid().equals(ssid) && info.getAuthType() == authtype && info.isInBlackList() != isAddtoBlack) {
                info.setBlackListFlag(isAddtoBlack);
                String str = MessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setBlackListBySsid info.getBssid() = ");
                stringBuilder.append(partDisplayBssid(info.getBssid()));
                stringBuilder.append(" info.getSsid() = ");
                stringBuilder.append(info.getSsid());
                stringBuilder.append("isAddtoBlack = ");
                stringBuilder.append(isAddtoBlack);
                Log.e(str, stringBuilder.toString());
                this.mDbManager.updateBssidIsInBlackList(info.getBssid(), value);
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x004f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void resetBlackListByBssid(String bssid, boolean isAddtoBlack) {
        synchronized (this.mLock) {
            if (bssid == null) {
                return;
            }
            boolean value = isAddtoBlack;
            APInfoData data = getApInfo(bssid);
            if (data != null) {
                String str = MessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("resetBlackListByBssid data.getBssid() = ");
                stringBuilder.append(partDisplayBssid(data.getBssid()));
                stringBuilder.append(" data.getSsid() = ");
                stringBuilder.append(data.getSsid());
                stringBuilder.append("isAddtoBlack = ");
                stringBuilder.append(isAddtoBlack);
                Log.e(str, stringBuilder.toString());
                this.mDbManager.updateBssidIsInBlackList(data.getBssid(), value);
                data.setBlackListFlag(isAddtoBlack);
            }
        }
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

    private boolean isInConfigNetworks(String ssid, int AuthType) {
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            return false;
        }
        for (WifiConfiguration mConfiguration : configNetworks) {
            if (mConfiguration != null && isValid(mConfiguration)) {
                if (mConfiguration.status != 1) {
                    if (mConfiguration.SSID != null && mConfiguration.SSID.equals(ssid) && mConfiguration.getAuthType() == AuthType) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int getAuthType(int networkId) {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs == null || configs.size() == 0) {
            return -1;
        }
        for (WifiConfiguration config : configs) {
            if (config != null && isValid(config) && networkId == config.networkId) {
                String str = MessageUtil.TAG;
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

    public void resetAllBlackList() {
        synchronized (this.mLock) {
            for (APInfoData info : this.mInfos) {
                if (info.isInBlackList()) {
                    info.setBlackListFlag(false);
                    String str = MessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("resetAllBlackList info.ssid() = ");
                    stringBuilder.append(info.getSsid());
                    Log.e(str, stringBuilder.toString());
                    this.mDbManager.updateBssidIsInBlackList(info.getBssid(), 0);
                }
            }
        }
    }

    public boolean isHasTargetAp(List<ScanResult> lists) {
        for (ScanResult result : lists) {
            String str = result.BSSID;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            stringBuilder.append(result.SSID);
            stringBuilder.append("\"");
            if (isInTargetAp(str, stringBuilder.toString()) && !isInBlackList(result.BSSID)) {
                String str2 = MessageUtil.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isHasTargetAp find target AP result.BSSID = ");
                stringBuilder2.append(partDisplayBssid(result.BSSID));
                Log.e(str2, stringBuilder2.toString());
                return true;
            }
        }
        return false;
    }

    private void inlineUpdataApCellInfo(APInfoData data, String cellid) {
        if (this.mIsRunning && data != null) {
            if (isCellIdExit(data, cellid)) {
                Log.e(MessageUtil.TAG, "inlineUpdataApCellInfo info is already there");
            } else {
                Log.e(MessageUtil.TAG, "addCurrentApInfo addCellIdInfo");
                this.mDbManager.addCellInfo(data.getBssid(), cellid);
                List<CellInfoData> cellInfos = this.mDbManager.queryCellInfoByBssid(data.getBssid());
                if (cellInfos.size() != 0) {
                    data.setCellInfo(cellInfos);
                }
            }
        }
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
