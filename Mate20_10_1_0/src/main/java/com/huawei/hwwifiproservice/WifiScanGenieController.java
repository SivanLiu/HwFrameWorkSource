package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.hwwifiproservice.WifiScanGenieDataBaseImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WifiScanGenieController {
    private static final int[] COMMON_CHANNELS = {2412, 2437, 2462, 5180, 5220, 5745, 5765, 5785, 5805, 5825};
    private static final String DEV_5G_AVAILABLE_FREQ_FALG = "SCAN_GENIE_DEV_5G_AVAILABLE_FREQ_FALG";
    private static final String DEV_5G_CAPABILITY_FALG = "SCAN_GENIE_DEV_5G_CAPABILITY_FALG";
    private static final int DEV_5G_CAPABILITY_SUPPORTED = 101;
    private static final int DEV_5G_CAPABILITY_UNKNOWN = 100;
    private static final int DEV_5G_CAPABILITY_UNSUPPORTED = 102;
    private static final int INVAILD_ID = -1;
    public static final int INVALID_FREQ_PUNISHED = -100;
    private static final int MAX_RECENT_CONNECTED_CHANNELS_NUM = 4;
    public static final int MSG_5G_CAPABILITY_QUERY = 6;
    public static final int MSG_BOOT_COMPLETED = 5;
    public static final int MSG_CONFIGURED_CHANGED = 1;
    public static final int MSG_INIT_PREFERRED_CHANNELS = 7;
    public static final int MSG_NETWORK_CONNECTED_NOTIFIED = 2;
    public static final int MSG_NETWORK_DISCONNECTED_NOTIFIED = 8;
    public static final int MSG_NETWORK_ROAMING_COMPLETED_NOTIFIED = 4;
    public static final int MSG_SCAN_RESULTS_AVAILABLE = 3;
    private static final String OOB_STARTUP_GUIDE = "com.huawei.hwstartupguide";
    private static final boolean SCANGENIE_ENABLE = true;
    private static final String TAG = "WifiScanGenie_Controller";
    private static WifiScanGenieController mWifiScanGenieController;
    /* access modifiers changed from: private */
    public boolean m11vSupported = false;
    private final Object m5GCapabilityLock = new Object();
    private int m5GFreqCapability = 100;
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public CellIdChangedListener mCellIdChangedListener;
    /* access modifiers changed from: private */
    public final Object mChannelsLock = new Object();
    private List<Integer> mCommonFrequencys = new ArrayList();
    /* access modifiers changed from: private */
    public boolean mConncetedBackGround = false;
    /* access modifiers changed from: private */
    public boolean mConncetedUseSpecifiedChannels = false;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver = null;
    private Context mContext;
    private String mCurrentBssid = null;
    /* access modifiers changed from: private */
    public int mCurrentCellId = -1;
    /* access modifiers changed from: private */
    public int mCurrentFrequency;
    private int mCurrentPriority = -1;
    private String mCurrentSsid = null;
    /* access modifiers changed from: private */
    public WifiConfiguration mCurrentWifiConfig;
    /* access modifiers changed from: private */
    public WifiScanGenieDataBaseImpl mDataBaseImpl;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public final Object mNetworkInfoLock = new Object();
    /* access modifiers changed from: private */
    public int mPunishedCellId = -1;
    /* access modifiers changed from: private */
    public List<Integer> mRecentConnectedChannels = new ArrayList();
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    /* access modifiers changed from: private */
    public Handler mWifiScanGenieHandler;

    public static synchronized WifiScanGenieController createWifiScanGenieControllerImpl(Context context) {
        WifiScanGenieController wifiScanGenieController;
        synchronized (WifiScanGenieController.class) {
            if (mWifiScanGenieController == null) {
                mWifiScanGenieController = new WifiScanGenieController(context);
            }
            wifiScanGenieController = mWifiScanGenieController;
        }
        return wifiScanGenieController;
    }

    private WifiScanGenieController(Context context) {
        initController(context);
    }

    private void initController(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mDataBaseImpl = new WifiScanGenieDataBaseImpl(context);
        this.mDataBaseImpl.openDB();
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        initWifiScanGenieControllerHandler();
        registerBroadcastReceiver();
        synchronized (this.m5GCapabilityLock) {
            this.m5GFreqCapability = Settings.Secure.getInt(this.mContentResolver, DEV_5G_CAPABILITY_FALG, 100);
            if (this.m5GFreqCapability == 101 && this.mWifiScanGenieHandler != null) {
                this.mWifiScanGenieHandler.sendMessageDelayed(this.mWifiScanGenieHandler.obtainMessage(7), 1000);
            }
        }
        HwHiLog.d(TAG, false, "WifiScanGenieController init!", new Object[0]);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0075, code lost:
        r2 = "";
        r5 = com.huawei.hwwifiproservice.HwAutoConnectManager.getInstance();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x007c, code lost:
        if (r5 == null) goto L_0x0088;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x007e, code lost:
        r6 = r5.getCurrentTopUid();
        r2 = r5.getCurrentPackageName();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x008e, code lost:
        if (com.huawei.hwwifiproservice.WifiScanGenieController.OOB_STARTUP_GUIDE.equals(r2) == false) goto L_0x00a0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0090, code lost:
        android.util.wifi.HwHiLog.d(com.huawei.hwwifiproservice.WifiScanGenieController.TAG, false, "getScanfrequencys, OOB_STARTUP_GUIDE matched!", new java.lang.Object[0]);
        r3 = new java.util.ArrayList();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x009f, code lost:
        return r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00a2, code lost:
        if (r11.mDataBaseImpl == null) goto L_0x00f9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00a4, code lost:
        r6 = r11.mDataBaseImpl.queryScanRecordsByCellid(r11.mCurrentCellId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00ac, code lost:
        if (r6 == null) goto L_0x00f0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00ae, code lost:
        r7 = r6.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00b6, code lost:
        if (r7.hasNext() == false) goto L_0x00db;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00c4, code lost:
        if (r7.next().getCurrentFrequency() != -100) goto L_0x00b2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00c6, code lost:
        r11.mPunishedCellId = r11.mCurrentCellId;
        android.util.wifi.HwHiLog.d(com.huawei.hwwifiproservice.WifiScanGenieController.TAG, false, "getScanfrequencys, INVALID_FREQ_PUNISHED", new java.lang.Object[0]);
        r3 = new java.util.ArrayList();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00d9, code lost:
        return r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x00db, code lost:
        android.util.wifi.HwHiLog.d(com.huawei.hwwifiproservice.WifiScanGenieController.TAG, false, "no punished, use the specified channels to scan", new java.lang.Object[0]);
        r7 = fusefrequencys(frequencyDb(r6));
        r11.mConncetedUseSpecifiedChannels = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x00ef, code lost:
        return r7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x00f0, code lost:
        android.util.wifi.HwHiLog.w(com.huawei.hwwifiproservice.WifiScanGenieController.TAG, false, "queryScanRecordsByCellid is null", new java.lang.Object[0]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x00f9, code lost:
        r3 = new java.util.ArrayList();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x00ff, code lost:
        return r3;
     */
    public List<Integer> getScanfrequencys() {
        synchronized (this.mNetworkInfoLock) {
            this.mCurrentCellId = WifiProCommonUtils.getCurrentCellId();
            if (this.mCurrentCellId != -1) {
                if (this.mCurrentCellId != this.mPunishedCellId) {
                    synchronized (this.m5GCapabilityLock) {
                        if (this.m5GFreqCapability == 100) {
                            HwHiLog.d(TAG, false, "getScanfrequencys, DEV_5G_CAPABILITY_UNKNOWN", new Object[0]);
                            this.mWifiScanGenieHandler.sendMessage(this.mWifiScanGenieHandler.obtainMessage(6));
                            return new ArrayList();
                        } else if (this.m5GFreqCapability == 102) {
                            HwHiLog.d(TAG, false, "getScanfrequencys, DEV_5G_CAPABILITY_UNSUPPORTED", new Object[0]);
                            return new ArrayList();
                        } else if (this.m5GFreqCapability == 101) {
                            HwHiLog.d(TAG, false, "getScanfrequencys, DEV_5G_CAPABILITY_SUPPORTED", new Object[0]);
                            if (this.mCommonFrequencys.size() == 0) {
                                return new ArrayList();
                            }
                        }
                    }
                }
            }
            HwHiLog.d(TAG, false, "getScanfrequencys, INVAILD_ID or unallowed id = %{private}d", new Object[]{Integer.valueOf(this.mPunishedCellId)});
            return new ArrayList();
        }
    }

    public void notifyUseFullChannels() {
        synchronized (this.mNetworkInfoLock) {
            if (this.mCurrentWifiConfig == null) {
                this.mConncetedUseSpecifiedChannels = false;
            }
        }
    }

    public void notifyNetworkRoamingCompleted(String bssid) {
        synchronized (this.mNetworkInfoLock) {
            if (bssid != null) {
                if (!(this.mCurrentBssid == null || bssid.equals(this.mCurrentBssid) || this.mCurrentWifiConfig == null)) {
                    this.mWifiScanGenieHandler.sendMessage(this.mWifiScanGenieHandler.obtainMessage(4, this.mCurrentWifiConfig));
                }
            }
        }
    }

    public void notifyWifiConnectedBackground() {
        synchronized (this.mNetworkInfoLock) {
            this.mConncetedBackGround = true;
        }
    }

    public List<WifiScanGenieDataBaseImpl.ScanRecord> getWifiScanRecordbyCellid() {
        if (this.mDataBaseImpl == null) {
            return new ArrayList();
        }
        return new ArrayList();
    }

    public void handleWiFiDisconnected() {
        this.mWifiScanGenieHandler.sendMessage(this.mWifiScanGenieHandler.obtainMessage(8));
    }

    public void handleWiFiConnected(WifiConfiguration currentWifiConfig, boolean cellIdChanged) {
        this.mWifiScanGenieHandler.sendMessage(this.mWifiScanGenieHandler.obtainMessage(2, currentWifiConfig));
    }

    public void handleWifiConnectedMsg(WifiConfiguration currentWifiConfig, boolean cellIdChanged) {
        List<ScanResult> scanResults;
        synchronized (this.mNetworkInfoLock) {
            HwHiLog.d(TAG, false, "handleWifiConnectedMsg, cellIdChanged:%{public}s", new Object[]{String.valueOf(cellIdChanged)});
            if (this.mDataBaseImpl != null) {
                if (currentWifiConfig != null) {
                    if (HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(this.mContext)) {
                        HwHiLog.d(TAG, false, "handleWifiConnectedMsg, this is mobile ap,ignore it", new Object[0]);
                        return;
                    }
                    if (this.mCurrentCellId == -1) {
                        this.mCurrentCellId = WifiProCommonUtils.getCurrentCellId();
                        HwHiLog.d(TAG, false, "handleWifiConnectedMsg, id  = %{private}d", new Object[]{Integer.valueOf(this.mCurrentCellId)});
                    }
                    if (this.mCurrentCellId != -1) {
                        if (this.mCurrentCellId != this.mPunishedCellId) {
                            this.mCurrentWifiConfig = currentWifiConfig;
                            if (this.mCurrentWifiConfig != null) {
                                this.mCurrentPriority = this.mCurrentWifiConfig.priority;
                                HwHiLog.d(TAG, false, "wifi connect,mCurrentPriority: %{public}d", new Object[]{Integer.valueOf(this.mCurrentPriority)});
                            }
                            if (!cellIdChanged) {
                                getCurrentAPInfo();
                            }
                            List<WifiScanGenieDataBaseImpl.ScanRecord> scanRecords = this.mDataBaseImpl.queryScanRecordsByCellid(this.mCurrentCellId);
                            if (scanRecords != null) {
                                for (WifiScanGenieDataBaseImpl.ScanRecord scanRecord : scanRecords) {
                                    if (scanRecord.getCurrentFrequency() == -100) {
                                        this.mPunishedCellId = this.mCurrentCellId;
                                        HwHiLog.d(TAG, false, "handleWifiConnectedMsg, INVALID_FREQ_PUNISHED", new Object[0]);
                                        return;
                                    }
                                }
                            }
                            List<WifiScanGenieDataBaseImpl.ScanRecord> scanRecordList = this.mDataBaseImpl.queryScanRecordsByBssid(this.mCurrentBssid);
                            if (scanRecordList == null || scanRecordList.size() <= 0) {
                                addNewRecord();
                            } else {
                                boolean isNewChannel = true;
                                if (scanRecordList.get(0).getCurrentFrequency() == this.mCurrentFrequency) {
                                    isNewChannel = false;
                                }
                                boolean isNewCellId = true;
                                if (this.mCurrentCellId != -1) {
                                    for (WifiScanGenieDataBaseImpl.ScanRecord record : scanRecordList) {
                                        if (record.getCellid() == this.mCurrentCellId) {
                                            isNewCellId = false;
                                        }
                                    }
                                }
                                if (isNewChannel && !cellIdChanged) {
                                    HwHiLog.d(TAG, false, "wifi connect, is isNewChannel ,mCurrentFrequency: %{public}d", new Object[]{Integer.valueOf(this.mCurrentFrequency)});
                                    this.mDataBaseImpl.updateBssidChannelRecord(this.mCurrentBssid, this.mCurrentSsid, this.mCurrentFrequency, this.mCurrentPriority);
                                }
                                if (isNewCellId && this.mCurrentCellId != -1) {
                                    HwHiLog.d(TAG, false, "wifi connect, is isNewCellId", new Object[0]);
                                    addNewRecord();
                                }
                                if (!isNewChannel && !isNewCellId && !cellIdChanged) {
                                    HwHiLog.d(TAG, false, "wifi connect, only update ssid , priority ,use time ", new Object[0]);
                                    this.mDataBaseImpl.updateBssidPriorityRecord(this.mCurrentSsid, this.mCurrentPriority);
                                }
                            }
                            if (!(!ScanResult.is24GHz(this.mCurrentFrequency) || this.mCurrentBssid == null || (scanResults = WifiproUtils.getScanResultsFromWsm()) == null)) {
                                for (ScanResult scanResult : scanResults) {
                                    if (this.mCurrentBssid.equals(scanResult.BSSID) && scanResult.dot11vNetwork) {
                                        HwHiLog.d(TAG, false, "handleWifiConnectedMsg, ssid = %{public}s, freq = %{public}d, 11v = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(scanResult.SSID), Integer.valueOf(scanResult.frequency), String.valueOf(scanResult.dot11vNetwork)});
                                        this.m11vSupported = true;
                                    }
                                }
                            }
                            return;
                        }
                    }
                    HwHiLog.w(TAG, false, "handleWifiConnectedMsg, INVAILD_ID, id  = %{private}d", new Object[]{Integer.valueOf(this.mPunishedCellId)});
                }
            }
        }
    }

    private boolean belongCommonAndSavedChannels(int freq) {
        if (isCommonChannel(freq)) {
            return true;
        }
        synchronized (this.mChannelsLock) {
            for (Integer num : this.mRecentConnectedChannels) {
                int connectedChannel = num.intValue();
                if (freq == connectedChannel && connectedChannel != -1) {
                    return true;
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void handleScanResultsAvailable(List<ScanResult> scanResults) {
        if (this.mCurrentBssid != null && this.mCurrentWifiConfig != null && this.mCurrentSsid != null && !this.mConncetedBackGround && scanResults != null) {
            boolean currentCellIdPunished = false;
            List<Integer> all5gFreqs = new ArrayList<>();
            Iterator<ScanResult> it = scanResults.iterator();
            while (true) {
                boolean sameConfigKey = true;
                if (!it.hasNext()) {
                    break;
                }
                ScanResult result = it.next();
                String scanSsid = "\"" + result.SSID + "\"";
                String scanResultEncrypt = result.capabilities;
                boolean sameBssid = this.mCurrentBssid.equals(result.BSSID);
                if (!this.mCurrentSsid.equals(scanSsid) || !WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, this.mCurrentWifiConfig.configKey())) {
                    sameConfigKey = false;
                }
                if (!sameBssid && sameConfigKey && result.is5GHz()) {
                    all5gFreqs.add(Integer.valueOf(result.frequency));
                }
            }
            Iterator<Integer> it2 = all5gFreqs.iterator();
            while (true) {
                if (it2.hasNext()) {
                    if (!belongCommonAndSavedChannels(it2.next().intValue())) {
                        currentCellIdPunished = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (currentCellIdPunished) {
                HwHiLog.d(TAG, false, "handleScanResultsAvailable, currentCellIdPunished = %{public}s", new Object[]{String.valueOf(currentCellIdPunished)});
                int i = this.mCurrentCellId;
                this.mPunishedCellId = i;
                this.mDataBaseImpl.deleteCellIdRecord(i);
                this.mDataBaseImpl.addNewChannelRecord("", "", -100, this.mCurrentCellId, -1);
            }
        }
    }

    private void addNewRecord() {
        HwHiLog.d(TAG, false, "WifiScanGenie addNewRecord", new Object[0]);
        int count = this.mDataBaseImpl.queryTableSize(WifiScanGenieDataBaseImpl.CHANNEL_TABLE_NAME);
        if (count > 2000) {
            this.mDataBaseImpl.deleteLastRecords(WifiScanGenieDataBaseImpl.CHANNEL_TABLE_NAME);
        }
        HwHiLog.d(TAG, false, "add new Record,mCurrentSsid : %{public}s, mCurrentFrequency :%{public}d, count :%{public}d", new Object[]{StringUtilEx.safeDisplaySsid(this.mCurrentSsid), Integer.valueOf(this.mCurrentFrequency), Integer.valueOf(count)});
        this.mDataBaseImpl.addNewChannelRecord(this.mCurrentBssid, this.mCurrentSsid, this.mCurrentFrequency, this.mCurrentCellId, this.mCurrentPriority);
    }

    private List<Integer> frequencyDb(List<WifiScanGenieDataBaseImpl.ScanRecord> scanRecords) {
        if (scanRecords == null) {
            return new ArrayList();
        }
        List<Integer> frequencys = new ArrayList<>();
        for (WifiScanGenieDataBaseImpl.ScanRecord scanRecord : scanRecords) {
            if (isValidChannel(scanRecord.getCurrentFrequency()) && !isCommonChannel(scanRecord.getCurrentFrequency())) {
                frequencys.add(Integer.valueOf(scanRecord.getCurrentFrequency()));
                synchronized (this.mChannelsLock) {
                    this.mRecentConnectedChannels.add(Integer.valueOf(scanRecord.getCurrentFrequency()));
                }
                if (frequencys.size() == 4) {
                    return frequencys;
                }
            }
        }
        return frequencys;
    }

    private List<Integer> fusefrequencys(List<Integer> frequencyDb) {
        List<Integer> fusefrequencyList;
        HwHiLog.d(TAG, false, "start fusefrequencys", new Object[0]);
        synchronized (this.m5GCapabilityLock) {
            List<Integer> temp = new ArrayList<>(frequencyDb);
            temp.retainAll(this.mCommonFrequencys);
            frequencyDb.removeAll(temp);
            fusefrequencyList = new ArrayList<>();
            fusefrequencyList.addAll(frequencyDb);
            fusefrequencyList.addAll(this.mCommonFrequencys);
        }
        return fusefrequencyList;
    }

    private void getCurrentAPInfo() {
        WifiInfo conInfo = this.mWifiManager.getConnectionInfo();
        if (conInfo == null || conInfo.getSupplicantState() != SupplicantState.COMPLETED) {
            reSetCurrentAPInfo();
            return;
        }
        this.mCurrentBssid = conInfo.getBSSID();
        this.mCurrentSsid = conInfo.getSSID();
        this.mCurrentFrequency = conInfo.getFrequency();
    }

    /* access modifiers changed from: private */
    public void reSetCurrentAPInfo() {
        HwHiLog.i(TAG, false, "reSetCurrentAPInfo !", new Object[0]);
        this.mCurrentBssid = null;
        this.mCurrentSsid = null;
        this.mCurrentFrequency = -1;
        this.mCurrentPriority = -1;
        this.mCurrentWifiConfig = null;
    }

    /* access modifiers changed from: private */
    public void parseAvailablePreferredChannels(String strAvailable5GChannels) {
        synchronized (this.m5GCapabilityLock) {
            if (!TextUtils.isEmpty(strAvailable5GChannels)) {
                List<String> available5GChannels = Arrays.asList(strAvailable5GChannels.replace("[", "").replace("]", "").split(", "));
                if (available5GChannels != null) {
                    int avai5GSize = available5GChannels.size();
                    int[] iArr = COMMON_CHANNELS;
                    for (int channel : iArr) {
                        boolean curr5gAvailable = false;
                        if (ScanResult.is5GHz(channel)) {
                            int j = 0;
                            while (true) {
                                if (j >= avai5GSize) {
                                    break;
                                }
                                if (!TextUtils.isEmpty(available5GChannels.get(j))) {
                                    int available5GFreq = -1;
                                    try {
                                        available5GFreq = Integer.parseInt(available5GChannels.get(j));
                                    } catch (NumberFormatException e) {
                                        HwHiLog.e(TAG, false, "Exception happened in parseAvailablePreferredChannels()", new Object[0]);
                                    }
                                    if (channel == available5GFreq) {
                                        curr5gAvailable = true;
                                        break;
                                    }
                                }
                                j++;
                            }
                        }
                        if (ScanResult.is24GHz(channel) || curr5gAvailable) {
                            this.mCommonFrequencys.add(Integer.valueOf(channel));
                        }
                    }
                }
            }
        }
    }

    private void initWifiScanGenieControllerHandler() {
        HandlerThread handlerThread = new HandlerThread("WifiScanGenie_handler_thread");
        handlerThread.start();
        this.mWifiScanGenieHandler = new Handler(handlerThread.getLooper()) {
            /* class com.huawei.hwwifiproservice.WifiScanGenieController.AnonymousClass1 */

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        WifiScanGenieController.this.handleMsgConfiguredChanged(msg);
                        return;
                    case 2:
                    case 4:
                        WifiConfiguration config = null;
                        if (msg.obj instanceof WifiConfiguration) {
                            config = (WifiConfiguration) msg.obj;
                        }
                        WifiScanGenieController.this.handleWifiConnectedMsg(config, false);
                        return;
                    case 3:
                        boolean needParseScanResults = false;
                        synchronized (WifiScanGenieController.this.mNetworkInfoLock) {
                            if (WifiScanGenieController.this.mConncetedUseSpecifiedChannels && !WifiScanGenieController.this.m11vSupported && WifiScanGenieController.this.mCurrentFrequency != -1 && WifiScanGenieController.this.mCurrentCellId != WifiScanGenieController.this.mPunishedCellId && WifiScanGenieController.this.mCurrentCellId != -1 && ScanResult.is24GHz(WifiScanGenieController.this.mCurrentFrequency)) {
                                needParseScanResults = true;
                            }
                        }
                        if (needParseScanResults) {
                            List<ScanResult> scanResults = WifiScanGenieController.this.mWifiManager.getScanResults();
                            synchronized (WifiScanGenieController.this.mNetworkInfoLock) {
                                WifiScanGenieController.this.handleScanResultsAvailable(scanResults);
                            }
                            return;
                        }
                        return;
                    case 5:
                        HwHiLog.d(WifiScanGenieController.TAG, false, "MSG_BOOT_COMPLETED", new Object[0]);
                        if (WifiScanGenieController.this.mCellIdChangedListener == null) {
                            WifiScanGenieController wifiScanGenieController = WifiScanGenieController.this;
                            CellIdChangedListener unused = wifiScanGenieController.mCellIdChangedListener = new CellIdChangedListener();
                            WifiScanGenieController.this.mTelephonyManager.listen(WifiScanGenieController.this.mCellIdChangedListener, 16);
                            return;
                        }
                        return;
                    case 6:
                        WifiScanGenieController.this.handleMsg5GCapabilityQuery();
                        return;
                    case 7:
                        HwHiLog.d(WifiScanGenieController.TAG, false, "###MSG_INIT_PREFERRED_CHANNELS", new Object[0]);
                        WifiScanGenieController.this.parseAvailablePreferredChannels(Settings.Secure.getString(WifiScanGenieController.this.mContentResolver, WifiScanGenieController.DEV_5G_AVAILABLE_FREQ_FALG));
                        return;
                    case 8:
                        HwHiLog.d(WifiScanGenieController.TAG, false, "WifiScanGenie handleWiFiDisconnected", new Object[0]);
                        if (WifiScanGenieController.this.mDataBaseImpl != null) {
                            synchronized (WifiScanGenieController.this.mNetworkInfoLock) {
                                WifiScanGenieController.this.reSetCurrentAPInfo();
                                boolean unused2 = WifiScanGenieController.this.mConncetedUseSpecifiedChannels = false;
                                boolean unused3 = WifiScanGenieController.this.mConncetedBackGround = false;
                                boolean unused4 = WifiScanGenieController.this.m11vSupported = false;
                                synchronized (WifiScanGenieController.this.mChannelsLock) {
                                    WifiScanGenieController.this.mRecentConnectedChannels.clear();
                                }
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    /* access modifiers changed from: private */
    public void handleMsgConfiguredChanged(Message msg) {
        if (msg.obj instanceof Intent) {
            Intent confgIntent = (Intent) msg.obj;
            WifiConfiguration connCfg = null;
            Object obj = confgIntent.getParcelableExtra("wifiConfiguration");
            if (obj instanceof WifiConfiguration) {
                connCfg = (WifiConfiguration) obj;
            }
            if (connCfg != null && !connCfg.isTempCreated) {
                int changeReason = -1;
                Object objChangeRes = Integer.valueOf(confgIntent.getIntExtra("changeReason", -1));
                if (objChangeRes instanceof Integer) {
                    changeReason = ((Integer) objChangeRes).intValue();
                }
                if (changeReason == 1) {
                    HwHiLog.d(TAG, false, "user forget %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(connCfg.SSID)});
                    this.mDataBaseImpl.deleteSsidRecord(connCfg.SSID);
                    return;
                }
                return;
            }
            return;
        }
        HwHiLog.e(TAG, false, "handleMsgConfiguredChanged: msg.obj is null or not intent", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void handleMsg5GCapabilityQuery() {
        List<Integer> availableChannels;
        HwHiLog.d(TAG, false, "###MSG_5G_CAPABILITY_QUERY", new Object[0]);
        boolean is5GHzBandSupported = this.mWifiManager.is5GHzBandSupported();
        synchronized (this.m5GCapabilityLock) {
            HwHiLog.d(TAG, false, "is5GHzBandSupported = %{public}s", new Object[]{String.valueOf(is5GHzBandSupported)});
            int i = 101;
            this.m5GFreqCapability = is5GHzBandSupported ? 101 : 102;
            ContentResolver contentResolver = this.mContentResolver;
            if (!is5GHzBandSupported) {
                i = 102;
            }
            Settings.Secure.putInt(contentResolver, DEV_5G_CAPABILITY_FALG, i);
        }
        if (is5GHzBandSupported) {
            Bundle data = new Bundle();
            data.putInt("WIFI_BAND_5_GHZ", 2);
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 13, data);
            if (result != null && (availableChannels = result.getIntegerArrayList("availableChannels")) != null && !availableChannels.isEmpty()) {
                Settings.Secure.putString(this.mContentResolver, DEV_5G_AVAILABLE_FREQ_FALG, availableChannels.toString());
                parseAvailablePreferredChannels(availableChannels.toString());
            }
        }
    }

    private void registerBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.WifiScanGenieController.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action)) {
                    WifiScanGenieController.this.mWifiScanGenieHandler.sendMessage(WifiScanGenieController.this.mWifiScanGenieHandler.obtainMessage(1, intent));
                } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                    WifiScanGenieController.this.mWifiScanGenieHandler.sendMessage(WifiScanGenieController.this.mWifiScanGenieHandler.obtainMessage(5));
                } else if ("android.net.wifi.SCAN_RESULTS".equals(action) && intent.getBooleanExtra("resultsUpdated", false)) {
                    WifiScanGenieController.this.mWifiScanGenieHandler.sendMessage(WifiScanGenieController.this.mWifiScanGenieHandler.obtainMessage(3));
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mIntentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    /* access modifiers changed from: private */
    public class CellIdChangedListener extends PhoneStateListener {
        private CellIdChangedListener() {
        }

        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);
            synchronized (WifiScanGenieController.this.mNetworkInfoLock) {
                int currentId = WifiProCommonUtils.getCurrentCellId();
                if (!(currentId == -1 || WifiScanGenieController.this.mCurrentCellId == currentId)) {
                    HwHiLog.d(WifiScanGenieController.TAG, false, "mCurrentCellId has Changed, try update = %{private}d", new Object[]{Integer.valueOf(currentId)});
                    int unused = WifiScanGenieController.this.mCurrentCellId = currentId;
                    if (WifiScanGenieController.this.mCurrentWifiConfig != null) {
                        WifiScanGenieController.this.handleWiFiConnected(WifiScanGenieController.this.mCurrentWifiConfig, true);
                    }
                }
            }
        }

        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            synchronized (WifiScanGenieController.this.mNetworkInfoLock) {
                if (serviceState.getState() != 0) {
                    int unused = WifiScanGenieController.this.mCurrentCellId = -1;
                }
            }
        }
    }

    private boolean isValidChannel(int frequency) {
        return frequency > 0;
    }

    private boolean isCommonChannel(int frequency) {
        for (int channel : COMMON_CHANNELS) {
            if (channel == frequency) {
                return true;
            }
        }
        return false;
    }
}
