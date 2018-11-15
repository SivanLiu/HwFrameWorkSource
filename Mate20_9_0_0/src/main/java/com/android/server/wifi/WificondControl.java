package com.android.server.wifi;

import android.net.MacAddress;
import android.net.wifi.IApInterface;
import android.net.wifi.IApInterfaceEventCallback;
import android.net.wifi.IApInterfaceEventCallback.Stub;
import android.net.wifi.IClientInterface;
import android.net.wifi.IHwVendorEvent;
import android.net.wifi.IPnoScanEvent;
import android.net.wifi.IScanEvent;
import android.net.wifi.IWifiScannerImpl;
import android.net.wifi.IWificond;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanResult.InformationElement;
import android.net.wifi.ScanResult.RadioChainInfo;
import android.net.wifi.WifiSsid;
import android.os.Binder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.WifiNative.PnoNetwork;
import com.android.server.wifi.WifiNative.PnoSettings;
import com.android.server.wifi.WifiNative.SignalPollResult;
import com.android.server.wifi.WifiNative.SoftApListener;
import com.android.server.wifi.WifiNative.TxPacketCounters;
import com.android.server.wifi.WifiNative.WificondDeathEventHandler;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.scanner.ScanResultRecords;
import com.android.server.wifi.util.InformationElementUtil;
import com.android.server.wifi.util.InformationElementUtil.Capabilities;
import com.android.server.wifi.util.InformationElementUtil.Dot11vNetwork;
import com.android.server.wifi.util.InformationElementUtil.HiLinkNetwork;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.wificond.ChannelSettings;
import com.android.server.wifi.wificond.HiddenNetwork;
import com.android.server.wifi.wificond.NativeMssResult;
import com.android.server.wifi.wificond.NativeScanResult;
import com.android.server.wifi.wificond.SingleScanSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class WificondControl implements DeathRecipient {
    public static final int SCAN_TYPE_PNO_SCAN = 1;
    public static final int SCAN_TYPE_SINGLE_SCAN = 0;
    private static final String TAG = "WificondControl";
    private HashMap<String, IApInterfaceEventCallback> mApInterfaceListeners = new HashMap();
    private HashMap<String, IApInterface> mApInterfaces = new HashMap();
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private HashMap<String, IClientInterface> mClientInterfaces = new HashMap();
    private WificondDeathEventHandler mDeathEventHandler;
    private IHwVendorEvent mHwVendorEventHandler;
    private Set<String> mOldSsidList = new HashSet();
    private HashMap<String, IPnoScanEvent> mPnoScanEventHandlers = new HashMap();
    private HashMap<String, IScanEvent> mScanEventHandlers = new HashMap();
    private boolean mVerboseLoggingEnabled = false;
    private WifiInjector mWifiInjector;
    private WifiMonitor mWifiMonitor;
    private IWificond mWificond;
    private HashMap<String, IWifiScannerImpl> mWificondScanners = new HashMap();

    private class ApInterfaceEventCallback extends Stub {
        private SoftApListener mSoftApListener;

        ApInterfaceEventCallback(SoftApListener listener) {
            this.mSoftApListener = listener;
        }

        public void onNumAssociatedStationsChanged(int numStations) {
            this.mSoftApListener.onNumAssociatedStationsChanged(numStations);
        }

        public void onSoftApChannelSwitched(int frequency, int bandwidth) {
            this.mSoftApListener.onSoftApChannelSwitched(frequency, bandwidth);
        }

        public void OnApLinkedStaJoin(String macAddress) {
            this.mSoftApListener.OnApLinkedStaJoin(macAddress);
        }

        public void OnApLinkedStaLeave(String macAddress) {
            this.mSoftApListener.OnApLinkedStaLeave(macAddress);
        }
    }

    private class HwVendorEventHandler extends IHwVendorEvent.Stub {
        private HwVendorEventHandler() {
        }

        public void OnMssSyncReport(NativeMssResult mssStru) {
            WifiStateMachine machine = WificondControl.this.mWifiInjector.getWifiStateMachine();
            if (machine != null) {
                machine.onMssSyncResultEvent(mssStru);
            }
        }

        public void OnTasRssiReport(int index, int rssi, int[] rsv) {
            String str = WificondControl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OnTasRssiReport index:");
            stringBuilder.append(index);
            stringBuilder.append("rssi:");
            stringBuilder.append(rssi);
            Log.e(str, stringBuilder.toString());
        }
    }

    private class PnoScanEventHandler extends IPnoScanEvent.Stub {
        private String mIfaceName;

        PnoScanEventHandler(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        public void OnPnoNetworkFound() {
            Log.d(WificondControl.TAG, "Pno scan result event");
            WificondControl.this.mWifiMonitor.broadcastPnoScanResultEvent(this.mIfaceName);
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoFoundNetworkEventCount();
        }

        public void OnPnoScanFailed() {
            Log.d(WificondControl.TAG, "Pno Scan failed event");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedCount();
        }

        public void OnPnoScanOverOffloadStarted() {
            Log.d(WificondControl.TAG, "Pno scan over offload started");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanStartedOverOffloadCount();
        }

        public void OnPnoScanOverOffloadFailed(int reason) {
            Log.d(WificondControl.TAG, "Pno scan over offload failed");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedOverOffloadCount();
        }
    }

    private class ScanEventHandler extends IScanEvent.Stub {
        private String mIfaceName;

        ScanEventHandler(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        public void OnScanResultReady() {
            Log.d(WificondControl.TAG, "Scan result ready event");
            WificondControl.this.mWifiMonitor.broadcastScanResultEvent(this.mIfaceName);
        }

        public void OnScanFailed() {
            Log.d(WificondControl.TAG, "Scan failed event");
            WificondControl.this.mWifiMonitor.broadcastScanFailedEvent(this.mIfaceName);
        }
    }

    WificondControl(WifiInjector wifiInjector, WifiMonitor wifiMonitor, CarrierNetworkConfig carrierNetworkConfig) {
        this.mWifiInjector = wifiInjector;
        this.mWifiMonitor = wifiMonitor;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
    }

    public void binderDied() {
        Log.e(TAG, "Wificond died!");
        clearState();
        this.mWificond = null;
        if (this.mDeathEventHandler != null) {
            this.mDeathEventHandler.onDeath();
        }
    }

    public void enableVerboseLogging(boolean enable) {
        this.mVerboseLoggingEnabled = enable;
    }

    public boolean initialize(WificondDeathEventHandler handler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = handler;
        tearDownInterfaces();
        return true;
    }

    private boolean retrieveWificondAndRegisterForDeath() {
        if (this.mWificond != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Wificond handle already retrieved");
            }
            return true;
        }
        this.mWificond = this.mWifiInjector.makeWificond();
        if (this.mWificond == null) {
            Log.e(TAG, "Failed to get reference to wificond");
            return false;
        }
        try {
            this.mWificond.asBinder().linkToDeath(this, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register death notification for wificond");
            return false;
        }
    }

    public IClientInterface setupInterfaceForClientMode(String ifaceName) {
        Log.d(TAG, "Setting up interface for client mode");
        if (!retrieveWificondAndRegisterForDeath()) {
            return null;
        }
        IClientInterface clientInterface = null;
        try {
            clientInterface = this.mWificond.createClientInterface(ifaceName);
            if (clientInterface == null) {
                Log.e(TAG, "Could not get IClientInterface instance from wificond");
                return null;
            }
            Binder.allowBlocking(clientInterface.asBinder());
            this.mClientInterfaces.put(ifaceName, clientInterface);
            try {
                IWifiScannerImpl wificondScanner = clientInterface.getWifiScannerImpl();
                if (wificondScanner == null) {
                    Log.e(TAG, "Failed to get WificondScannerImpl");
                    return null;
                }
                this.mWificondScanners.put(ifaceName, wificondScanner);
                Binder.allowBlocking(wificondScanner.asBinder());
                ScanEventHandler scanEventHandler = new ScanEventHandler(ifaceName);
                this.mScanEventHandlers.put(ifaceName, scanEventHandler);
                wificondScanner.subscribeScanEvents(scanEventHandler);
                PnoScanEventHandler pnoScanEventHandler = new PnoScanEventHandler(ifaceName);
                this.mPnoScanEventHandlers.put(ifaceName, pnoScanEventHandler);
                wificondScanner.subscribePnoScanEvents(pnoScanEventHandler);
                this.mHwVendorEventHandler = new HwVendorEventHandler();
                clientInterface.subscribeVendorEvents(this.mHwVendorEventHandler);
                return clientInterface;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to refresh wificond scanner due to remote exception");
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to get IClientInterface due to remote exception");
            return null;
        }
    }

    public boolean tearDownClientInterface(String ifaceName) {
        IClientInterface clientInterface = getClientInterface(ifaceName);
        if (clientInterface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return false;
        }
        try {
            IWifiScannerImpl scannerImpl = (IWifiScannerImpl) this.mWificondScanners.get(ifaceName);
            if (scannerImpl != null) {
                scannerImpl.unsubscribeScanEvents();
                scannerImpl.unsubscribePnoScanEvents();
            }
            clientInterface.unsubscribeVendorEvents();
            try {
                if (this.mWificond.tearDownClientInterface(ifaceName)) {
                    this.mClientInterfaces.remove(ifaceName);
                    this.mWificondScanners.remove(ifaceName);
                    this.mScanEventHandlers.remove(ifaceName);
                    this.mPnoScanEventHandlers.remove(ifaceName);
                    return true;
                }
                Log.e(TAG, "Failed to teardown client interface");
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to teardown client interface due to remote exception");
                return false;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to unsubscribe wificond scanner due to remote exception");
            return false;
        }
    }

    public IApInterface setupInterfaceForSoftApMode(String ifaceName) {
        Log.d(TAG, "Setting up interface for soft ap mode");
        if (!retrieveWificondAndRegisterForDeath()) {
            return null;
        }
        IApInterface apInterface = null;
        try {
            apInterface = this.mWificond.createApInterface(ifaceName);
            if (apInterface == null) {
                Log.e(TAG, "Could not get IApInterface instance from wificond");
                return null;
            }
            Binder.allowBlocking(apInterface.asBinder());
            this.mApInterfaces.put(ifaceName, apInterface);
            return apInterface;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get IApInterface due to remote exception");
            return null;
        }
    }

    public boolean tearDownSoftApInterface(String ifaceName) {
        if (getApInterface(ifaceName) == null) {
            Log.e(TAG, "No valid wificond ap interface handler");
            return false;
        }
        try {
            if (this.mWificond.tearDownApInterface(ifaceName)) {
                this.mApInterfaces.remove(ifaceName);
                this.mApInterfaceListeners.remove(ifaceName);
                return true;
            }
            Log.e(TAG, "Failed to teardown AP interface");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to teardown AP interface due to remote exception");
            return false;
        }
    }

    public boolean tearDownInterfaces() {
        Log.d(TAG, "tearing down interfaces in wificond");
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            for (Entry<String, IWifiScannerImpl> entry : this.mWificondScanners.entrySet()) {
                ((IWifiScannerImpl) entry.getValue()).unsubscribeScanEvents();
                ((IWifiScannerImpl) entry.getValue()).unsubscribePnoScanEvents();
            }
            for (Entry<String, IClientInterface> entry2 : this.mClientInterfaces.entrySet()) {
                ((IClientInterface) entry2.getValue()).unsubscribeVendorEvents();
            }
            if (this.mWificond == null) {
                Log.e(TAG, "mWificond is null, Wificond may be dead");
                return false;
            }
            this.mWificond.tearDownInterfaces();
            clearState();
            this.mHwVendorEventHandler = null;
            synchronized (this.mOldSsidList) {
                this.mOldSsidList.clear();
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to tear down interfaces due to remote exception");
            return false;
        }
    }

    private IClientInterface getClientInterface(String ifaceName) {
        return (IClientInterface) this.mClientInterfaces.get(ifaceName);
    }

    public boolean disableSupplicant() {
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            return this.mWificond.disableSupplicant();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disable supplicant due to remote exception");
            return false;
        }
    }

    public boolean enableSupplicant() {
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            return this.mWificond.enableSupplicant();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to enable supplicant due to remote exception");
            return false;
        }
    }

    public SignalPollResult signalPoll(String ifaceName) {
        IClientInterface iface = getClientInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return null;
        }
        try {
            int[] resultArray = iface.signalPoll();
            if (resultArray == null || !(resultArray.length == 6 || resultArray.length == 3)) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            SignalPollResult pollResult = new SignalPollResult();
            pollResult.currentRssi = resultArray[0];
            pollResult.txBitrate = resultArray[1];
            pollResult.associationFrequency = resultArray[2];
            if (resultArray.length == 6) {
                pollResult.currentNoise = resultArray[3];
                pollResult.currentSnr = resultArray[4];
                pollResult.currentChload = resultArray[5];
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Noise: ");
                stringBuilder.append(pollResult.currentNoise);
                stringBuilder.append(", Snr: ");
                stringBuilder.append(pollResult.currentSnr);
                stringBuilder.append(", Chload: ");
                stringBuilder.append(pollResult.currentChload);
                Log.e(str, stringBuilder.toString());
            }
            return pollResult;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    public TxPacketCounters getTxPacketCounters(String ifaceName) {
        IClientInterface iface = getClientInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return null;
        }
        try {
            int[] resultArray = iface.getPacketCounters();
            if (resultArray == null || resultArray.length != 2) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            TxPacketCounters counters = new TxPacketCounters();
            counters.txSucceeded = resultArray[0];
            counters.txFailed = resultArray[1];
            return counters;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    private IWifiScannerImpl getScannerImpl(String ifaceName) {
        return (IWifiScannerImpl) this.mWificondScanners.get(ifaceName);
    }

    /* JADX WARNING: Removed duplicated region for block: B:95:0x0264  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ArrayList<ScanDetail> getScanResults(String ifaceName, int scanType) {
        IWifiScannerImpl iWifiScannerImpl;
        IllegalArgumentException e;
        String bssid;
        String str;
        StringBuilder stringBuilder;
        String flags;
        ArrayList<ScanDetail> results = new ArrayList();
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return results;
        }
        NativeScanResult[] nativeResults;
        String str2;
        String str3;
        if (scanType == 0) {
            try {
                nativeResults = scannerImpl.getScanResults();
            } catch (RemoteException e2) {
                iWifiScannerImpl = scannerImpl;
            }
        } else {
            try {
                nativeResults = scannerImpl.getPnoScanResults();
            } catch (RemoteException e3) {
                iWifiScannerImpl = scannerImpl;
            }
        }
        NativeScanResult[] nativeResults2 = nativeResults;
        ScanResultRecords.getDefault().clearOrdSsidRecords();
        int length = nativeResults2.length;
        int i = 0;
        while (i < length) {
            NativeScanResult result = nativeResults2[i];
            WifiSsid wifiSsid = WifiSsid.createFromByteArray(result.ssid);
            String flags2 = null;
            WifiSsid wifiSsid2;
            try {
                String bssid2 = NativeUtil.macAddressFromByteArray(result.bssid);
                try {
                    ScanResultRecords.getDefault().recordOriSsid(bssid2, wifiSsid.toString(), result.ssid);
                    wifiSsid.oriSsid = NativeUtil.hexStringFromByteArray(result.ssid);
                    if (bssid2 == null) {
                        try {
                            Log.e(TAG, "Illegal null bssid");
                            iWifiScannerImpl = scannerImpl;
                        } catch (IllegalArgumentException e4) {
                            e = e4;
                            iWifiScannerImpl = scannerImpl;
                            wifiSsid2 = wifiSsid;
                            bssid = bssid2;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Illegal argument for scan result with bssid: ");
                            stringBuilder.append(bssid);
                            Log.e(str, stringBuilder.toString(), e);
                            i++;
                            scannerImpl = iWifiScannerImpl;
                        }
                    } else {
                        Capabilities capabilities;
                        String bssid3;
                        ScanDetail scanDetail;
                        long j;
                        NetworkDetail networkDetail;
                        boolean z;
                        WifiSsid wifiSsid3;
                        InformationElement[] ies = InformationElementUtil.parseInformationElements(result.infoElement);
                        try {
                            capabilities = new Capabilities();
                            capabilities.from(ies, result.capability);
                            flags = capabilities.generateCapabilitiesString();
                        } catch (IllegalArgumentException e5) {
                            e = e5;
                            iWifiScannerImpl = scannerImpl;
                            wifiSsid2 = wifiSsid;
                            bssid = bssid2;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Illegal argument for scan result with bssid: ");
                            stringBuilder.append(bssid);
                            Log.e(str, stringBuilder.toString(), e);
                            i++;
                            scannerImpl = iWifiScannerImpl;
                        }
                        try {
                            boolean z2;
                            ScanResultRecords.getDefault().recordPmf(bssid2, capabilities.pmfCapabilities);
                            NetworkDetail networkDetail2 = new NetworkDetail(bssid2, ies, null, result.frequency);
                            if (wifiSsid.toString().equals(networkDetail2.getTrimmedSSID())) {
                                z2 = true;
                            } else {
                                String str4 = TAG;
                                r13 = new Object[3];
                                z2 = true;
                                r13[1] = wifiSsid.toString();
                                r13[2] = networkDetail2.getTrimmedSSID();
                                Log.d(str4, String.format("Inconsistent SSID on BSSID '%s': '%s' vs '%s' ", r13));
                            }
                            bssid3 = bssid2;
                            scanDetail = scanDetail;
                            j = result.tsf;
                            networkDetail = networkDetail2;
                            z = z2;
                            iWifiScannerImpl = scannerImpl;
                            wifiSsid3 = wifiSsid;
                        } catch (IllegalArgumentException e6) {
                            e = e6;
                            iWifiScannerImpl = scannerImpl;
                            wifiSsid2 = wifiSsid;
                            bssid = bssid2;
                            InformationElement[] informationElementArr = ies;
                            flags2 = flags;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Illegal argument for scan result with bssid: ");
                            stringBuilder.append(bssid);
                            Log.e(str, stringBuilder.toString(), e);
                            i++;
                            scannerImpl = iWifiScannerImpl;
                        }
                        try {
                            scanDetail = new ScanDetail(networkDetail, wifiSsid, bssid3, flags, result.signalMbm / 100, result.frequency, j, ies, null);
                            ScanResult scanResult = scanDetail.getScanResult();
                            if (ScanResultUtil.isScanResultForEapNetwork(scanDetail.getScanResult()) && this.mCarrierNetworkConfig.isCarrierNetwork(wifiSsid3.toString())) {
                                scanResult.isCarrierAp = z;
                                scanResult.carrierApEapType = this.mCarrierNetworkConfig.getNetworkEapType(wifiSsid3.toString());
                                scanResult.carrierName = this.mCarrierNetworkConfig.getCarrierName(wifiSsid3.toString());
                            }
                            if (result.radioChainInfos != null) {
                                scanResult.radioChainInfos = new RadioChainInfo[result.radioChainInfos.size()];
                                int idx = 0;
                                Iterator it = result.radioChainInfos.iterator();
                                while (it.hasNext()) {
                                    com.android.server.wifi.wificond.RadioChainInfo nativeRadioChainInfo = (com.android.server.wifi.wificond.RadioChainInfo) it.next();
                                    wifiSsid2 = wifiSsid3;
                                    scanResult.radioChainInfos[idx] = new RadioChainInfo();
                                    scanResult.radioChainInfos[idx].id = nativeRadioChainInfo.chainId;
                                    scanResult.radioChainInfos[idx].level = nativeRadioChainInfo.level;
                                    idx++;
                                    wifiSsid3 = wifiSsid2;
                                }
                            }
                            HiLinkNetwork hiLinkNetwork = new HiLinkNetwork();
                            hiLinkNetwork.from(ies);
                            scanDetail.getScanResult().hilinkTag = hiLinkNetwork.parseHiLogoTag(ies);
                            str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("hilinkTag:");
                            stringBuilder2.append(scanDetail.getScanResult().hilinkTag);
                            Log.d(str2, stringBuilder2.toString());
                            if (hiLinkNetwork.isHiLinkNetwork) {
                                wifiSsid = bssid3;
                            } else {
                                wifiSsid = bssid3;
                                if (!ScanResultRecords.getDefault().isHiLink(wifiSsid)) {
                                    scanDetail.getScanResult().isHiLinkNetwork = false;
                                    scannerImpl = new Dot11vNetwork();
                                    scannerImpl.from(ies);
                                    scanDetail.getScanResult().dot11vNetwork = scannerImpl.dot11vNetwork;
                                    results.add(scanDetail);
                                }
                            }
                            scanDetail.getScanResult().isHiLinkNetwork = true;
                            ScanResultRecords.getDefault().recordHiLink(wifiSsid);
                            scannerImpl = new Dot11vNetwork();
                            scannerImpl.from(ies);
                            scanDetail.getScanResult().dot11vNetwork = scannerImpl.dot11vNetwork;
                            results.add(scanDetail);
                        } catch (RemoteException e7) {
                        }
                    }
                } catch (IllegalArgumentException e8) {
                    e = e8;
                    iWifiScannerImpl = scannerImpl;
                    wifiSsid2 = wifiSsid;
                    bssid = bssid2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal argument for scan result with bssid: ");
                    stringBuilder.append(bssid);
                    Log.e(str, stringBuilder.toString(), e);
                    i++;
                    scannerImpl = iWifiScannerImpl;
                }
            } catch (IllegalArgumentException e9) {
                e = e9;
                iWifiScannerImpl = scannerImpl;
                wifiSsid2 = wifiSsid;
                bssid = null;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument for scan result with bssid: ");
                stringBuilder.append(bssid);
                Log.e(str, stringBuilder.toString(), e);
                i++;
                scannerImpl = iWifiScannerImpl;
            }
            i++;
            scannerImpl = iWifiScannerImpl;
        }
        str2 = "";
        synchronized (this.mOldSsidList) {
            str2 = ScanResultUtil.getScanResultLogs(this.mOldSsidList, results);
        }
        if (str2.length() > 0) {
            str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("get results:");
            stringBuilder3.append(str2);
            Log.d(str3, stringBuilder3.toString());
        }
        if (this.mVerboseLoggingEnabled) {
            str3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("get ");
            stringBuilder4.append(results.size());
            stringBuilder4.append(" scan results from wificond");
            Log.d(str3, stringBuilder4.toString());
        }
        return results;
        Log.e(TAG, "Failed to create ScanDetail ArrayList");
        if (this.mVerboseLoggingEnabled) {
        }
        return results;
    }

    private static int getScanType(int scanType) {
        switch (scanType) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid scan type ");
                stringBuilder.append(scanType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public boolean scan(String ifaceName, int scanType, Set<Integer> freqs, List<String> hiddenNetworkSSIDs) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        SingleScanSettings settings = new SingleScanSettings();
        try {
            settings.scanType = getScanType(scanType);
            settings.channelSettings = new ArrayList();
            settings.hiddenNetworks = new ArrayList();
            if (freqs != null) {
                for (Integer freq : freqs) {
                    ChannelSettings channel = new ChannelSettings();
                    channel.frequency = freq.intValue();
                    settings.channelSettings.add(channel);
                }
            }
            if (hiddenNetworkSSIDs != null) {
                for (String ssid : hiddenNetworkSSIDs) {
                    HiddenNetwork network = new HiddenNetwork();
                    try {
                        network.ssid = NativeUtil.byteArrayFromArrayList(NativeUtil.decodeSsid(ssid));
                        settings.hiddenNetworks.add(network);
                    } catch (IllegalArgumentException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Illegal argument ");
                        stringBuilder.append(ssid);
                        Log.e(str, stringBuilder.toString(), e);
                    }
                }
            }
            try {
                return scannerImpl.scan(settings);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to request scan due to remote exception");
                return false;
            }
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "Invalid scan type ", e3);
            return false;
        }
    }

    public boolean startPnoScan(String ifaceName, PnoSettings pnoSettings) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        com.android.server.wifi.wificond.PnoSettings settings = new com.android.server.wifi.wificond.PnoSettings();
        settings.pnoNetworks = new ArrayList();
        settings.intervalMs = pnoSettings.periodInMs;
        settings.min2gRssi = pnoSettings.min24GHzRssi;
        settings.min5gRssi = pnoSettings.min5GHzRssi;
        if (pnoSettings.networkList != null) {
            for (PnoNetwork network : pnoSettings.networkList) {
                com.android.server.wifi.wificond.PnoNetwork condNetwork = new com.android.server.wifi.wificond.PnoNetwork();
                boolean z = true;
                if ((network.flags & 1) == 0) {
                    z = false;
                }
                condNetwork.isHidden = z;
                try {
                    condNetwork.ssid = NativeUtil.byteArrayFromArrayList(NativeUtil.decodeSsid(network.ssid));
                    settings.pnoNetworks.add(condNetwork);
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal argument ");
                    stringBuilder.append(network.ssid);
                    Log.e(str, stringBuilder.toString(), e);
                }
            }
        }
        try {
            boolean success = scannerImpl.startPnoScan(settings);
            this.mWifiInjector.getWifiMetrics().incrementPnoScanStartAttempCount();
            if (!success) {
                this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedCount();
            }
            return success;
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to start pno scan due to remote exception");
            return false;
        }
    }

    public boolean stopPnoScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        try {
            return scannerImpl.stopPnoScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop pno scan due to remote exception");
            return false;
        }
    }

    public void abortScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return;
        }
        try {
            scannerImpl.abortScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request abortScan due to remote exception");
        }
    }

    public int[] getChannelsForBand(int band) {
        if (this.mWificond == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return null;
        } else if (band == 4) {
            return this.mWificond.getAvailableDFSChannels();
        } else {
            switch (band) {
                case 1:
                    return this.mWificond.getAvailable2gChannels();
                case 2:
                    return this.mWificond.getAvailable5gNonDFSChannels();
                default:
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unsupported band ");
                        stringBuilder.append(band);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to request getChannelsForBand due to remote exception");
                        return null;
                    }
            }
        }
    }

    private IApInterface getApInterface(String ifaceName) {
        return (IApInterface) this.mApInterfaces.get(ifaceName);
    }

    public boolean startHostapd(String ifaceName, SoftApListener listener) {
        IApInterface iface = getApInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid ap interface handler");
            return false;
        }
        try {
            IApInterfaceEventCallback callback = new ApInterfaceEventCallback(listener);
            this.mApInterfaceListeners.put(ifaceName, callback);
            if (iface.startHostapd(callback)) {
                return true;
            }
            Log.e(TAG, "Failed to start hostapd.");
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in starting soft AP: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean stopHostapd(String ifaceName) {
        IApInterface iface = getApInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid ap interface handler");
            return false;
        }
        try {
            if (iface.stopHostapd()) {
                this.mApInterfaceListeners.remove(ifaceName);
                return true;
            }
            Log.e(TAG, "Failed to stop hostapd.");
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in stopping soft AP: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean setMacAddress(String interfaceName, MacAddress mac) {
        IClientInterface mClientInterface = getClientInterface(interfaceName);
        if (mClientInterface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return false;
        }
        try {
            mClientInterface.setMacAddress(mac.toByteArray());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to setMacAddress due to remote exception");
            return false;
        }
    }

    private void clearState() {
        this.mClientInterfaces.clear();
        this.mWificondScanners.clear();
        this.mPnoScanEventHandlers.clear();
        this.mScanEventHandlers.clear();
        this.mApInterfaces.clear();
        this.mApInterfaceListeners.clear();
    }
}
