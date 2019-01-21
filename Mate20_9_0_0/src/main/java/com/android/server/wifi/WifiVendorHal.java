package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.IWifiChip.ChipDebugInfo;
import android.hardware.wifi.V1_0.IWifiChipEventCallback.Stub;
import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiRttControllerEventCallback;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.IWifiStaIfaceEventCallback;
import android.hardware.wifi.V1_0.RttConfig;
import android.hardware.wifi.V1_0.RttResponder;
import android.hardware.wifi.V1_0.RttResult;
import android.hardware.wifi.V1_0.StaApfPacketFilterCapabilities;
import android.hardware.wifi.V1_0.StaBackgroundScanBucketParameters;
import android.hardware.wifi.V1_0.StaBackgroundScanCapabilities;
import android.hardware.wifi.V1_0.StaBackgroundScanParameters;
import android.hardware.wifi.V1_0.StaLinkLayerRadioStats;
import android.hardware.wifi.V1_0.StaLinkLayerStats;
import android.hardware.wifi.V1_0.StaRoamingCapabilities;
import android.hardware.wifi.V1_0.StaRoamingConfig;
import android.hardware.wifi.V1_0.StaScanData;
import android.hardware.wifi.V1_0.StaScanResult;
import android.hardware.wifi.V1_0.WifiDebugHostWakeReasonStats;
import android.hardware.wifi.V1_0.WifiDebugRingBufferStatus;
import android.hardware.wifi.V1_0.WifiDebugRxPacketFateReport;
import android.hardware.wifi.V1_0.WifiDebugTxPacketFateReport;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_2.IWifiChipEventCallback;
import android.hardware.wifi.V1_2.IWifiChipEventCallback.IfaceInfo;
import android.hardware.wifi.V1_2.IWifiChipEventCallback.RadioModeInfo;
import android.net.MacAddress;
import android.net.apf.ApfCapabilities;
import android.net.wifi.RttManager;
import android.net.wifi.RttManager.ResponderConfig;
import android.net.wifi.RttManager.RttCapabilities;
import android.net.wifi.RttManager.RttParams;
import android.net.wifi.RttManager.WifiInformationElement;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanResult.InformationElement;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiSsid;
import android.net.wifi.WifiWakeReasonAndCounts;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.MutableInt;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.HexDump;
import com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener;
import com.android.server.wifi.HalDeviceManager.ManagerStatusListener;
import com.android.server.wifi.WifiLog.LogMessage;
import com.android.server.wifi.WifiNative.BucketSettings;
import com.android.server.wifi.WifiNative.ChannelSettings;
import com.android.server.wifi.WifiNative.RingBufferStatus;
import com.android.server.wifi.WifiNative.RoamingCapabilities;
import com.android.server.wifi.WifiNative.RoamingConfig;
import com.android.server.wifi.WifiNative.RttEventHandler;
import com.android.server.wifi.WifiNative.RxFateReport;
import com.android.server.wifi.WifiNative.ScanCapabilities;
import com.android.server.wifi.WifiNative.ScanEventHandler;
import com.android.server.wifi.WifiNative.ScanSettings;
import com.android.server.wifi.WifiNative.TxFateReport;
import com.android.server.wifi.WifiNative.VendorHalDeathEventHandler;
import com.android.server.wifi.WifiNative.VendorHalRadioModeChangeEventHandler;
import com.android.server.wifi.WifiNative.WifiLoggerEventHandler;
import com.android.server.wifi.WifiNative.WifiRssiEventHandler;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.util.BitMask;
import com.android.server.wifi.util.InformationElementUtil.Capabilities;
import com.android.server.wifi.util.NativeUtil;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class WifiVendorHal {
    private static final int CAPABILITY_SIZE = 16;
    private static final int[][] sChipFeatureCapabilityTranslation = new int[][]{new int[]{67108864, 256}, new int[]{128, 512}, new int[]{256, 1024}};
    public static final Object sLock = new Object();
    private static final ApfCapabilities sNoApfCapabilities = new ApfCapabilities(0, 0, 0);
    private static final WifiLog sNoLog = new FakeWifiLog();
    @VisibleForTesting
    static final int sRssiMonCmdId = 7551;
    private static final int[][] sStaFeatureCapabilityTranslation = new int[][]{new int[]{2, 128}, new int[]{4, 256}, new int[]{32, 2}, new int[]{1024, 512}, new int[]{4096, 1024}, new int[]{8192, 2048}, new int[]{65536, 4}, new int[]{524288, 8}, new int[]{1048576, 8192}, new int[]{2097152, 4096}, new int[]{8388608, 16}, new int[]{16777216, 32}, new int[]{33554432, 64}};
    private VendorHalDeathEventHandler mDeathEventHandler;
    private String mDriverDescription;
    private String mFirmwareDescription;
    private final HalDeviceManager mHalDeviceManager;
    private final HalDeviceManagerStatusListener mHalDeviceManagerStatusCallbacks;
    private final Handler mHalEventHandler;
    private HashMap<String, IWifiApIface> mIWifiApIfaces = new HashMap();
    private IWifiChip mIWifiChip;
    private final ChipEventCallback mIWifiChipEventCallback;
    private final ChipEventCallbackV12 mIWifiChipEventCallbackV12;
    private IWifiRttController mIWifiRttController;
    private final IWifiStaIfaceEventCallback mIWifiStaIfaceEventCallback;
    private HashMap<String, IWifiStaIface> mIWifiStaIfaces = new HashMap();
    private int mLastScanCmdId;
    @VisibleForTesting
    boolean mLinkLayerStatsDebug = false;
    @VisibleForTesting
    WifiLog mLog = new LogcatLog("WifiVendorHal");
    private WifiLoggerEventHandler mLogEventHandler = null;
    private final Looper mLooper;
    private VendorHalRadioModeChangeEventHandler mRadioModeChangeEventHandler;
    private int mRttCmdId;
    private int mRttCmdIdNext = 1;
    private final RttEventCallback mRttEventCallback;
    private RttEventHandler mRttEventHandler;
    private int mRttResponderCmdId = 0;
    @VisibleForTesting
    CurrentBackgroundScan mScan = null;
    @VisibleForTesting
    WifiLog mVerboseLog = sNoLog;
    private WifiRssiEventHandler mWifiRssiEventHandler;

    @VisibleForTesting
    class CurrentBackgroundScan {
        public int cmdId;
        public ScanEventHandler eventHandler = null;
        public ScanData[] latestScanResults;
        public StaBackgroundScanParameters param;
        public boolean paused;

        CurrentBackgroundScan(int id, ScanSettings settings) {
            int i = 0;
            this.paused = false;
            this.latestScanResults = null;
            this.cmdId = id;
            this.param = new StaBackgroundScanParameters();
            this.param.basePeriodInMs = settings.base_period_ms;
            this.param.maxApPerScan = settings.max_ap_per_scan;
            this.param.reportThresholdPercent = settings.report_threshold_percent;
            this.param.reportThresholdNumScans = settings.report_threshold_num_scans;
            if (settings.buckets != null) {
                BucketSettings[] bucketSettingsArr = settings.buckets;
                int length = bucketSettingsArr.length;
                while (i < length) {
                    this.param.buckets.add(WifiVendorHal.this.makeStaBackgroundScanBucketParametersFromBucketSettings(bucketSettingsArr[i]));
                    i++;
                }
            }
        }
    }

    private class ApInterfaceDestroyedListenerInternal implements InterfaceDestroyedListener {
        private final InterfaceDestroyedListener mExternalListener;

        ApInterfaceDestroyedListenerInternal(InterfaceDestroyedListener externalListener) {
            this.mExternalListener = externalListener;
        }

        public void onDestroyed(String ifaceName) {
            synchronized (WifiVendorHal.sLock) {
                WifiVendorHal.this.mIWifiApIfaces.remove(ifaceName);
            }
            if (this.mExternalListener != null) {
                this.mExternalListener.onDestroyed(ifaceName);
            }
        }
    }

    public class HalDeviceManagerStatusListener implements ManagerStatusListener {
        public void onStatusChanged() {
            boolean isReady = WifiVendorHal.this.mHalDeviceManager.isReady();
            boolean isStarted = WifiVendorHal.this.mHalDeviceManager.isStarted();
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Device Manager onStatusChanged. isReady(): ");
            stringBuilder.append(isReady);
            stringBuilder.append(", isStarted(): ");
            stringBuilder.append(isStarted);
            wifiLog.i(stringBuilder.toString());
            if (!isReady) {
                VendorHalDeathEventHandler handler;
                synchronized (WifiVendorHal.sLock) {
                    WifiVendorHal.this.clearState();
                    handler = WifiVendorHal.this.mDeathEventHandler;
                }
                if (handler != null) {
                    handler.onDeath();
                }
            }
        }
    }

    private class StaInterfaceDestroyedListenerInternal implements InterfaceDestroyedListener {
        private final InterfaceDestroyedListener mExternalListener;

        StaInterfaceDestroyedListenerInternal(InterfaceDestroyedListener externalListener) {
            this.mExternalListener = externalListener;
        }

        public void onDestroyed(String ifaceName) {
            synchronized (WifiVendorHal.sLock) {
                WifiVendorHal.this.mIWifiStaIfaces.remove(ifaceName);
            }
            if (this.mExternalListener != null) {
                this.mExternalListener.onDestroyed(ifaceName);
            }
        }
    }

    private class ChipEventCallback extends Stub {
        private ChipEventCallback() {
        }

        public void onChipReconfigured(int modeId) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onChipReconfigured ");
            stringBuilder.append(modeId);
            wifiLog.d(stringBuilder.toString());
        }

        public void onChipReconfigureFailure(WifiStatus status) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onChipReconfigureFailure ");
            stringBuilder.append(status);
            wifiLog.d(stringBuilder.toString());
        }

        public void onIfaceAdded(int type, String name) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onIfaceAdded ");
            stringBuilder.append(type);
            stringBuilder.append(", name: ");
            stringBuilder.append(name);
            wifiLog.d(stringBuilder.toString());
        }

        public void onIfaceRemoved(int type, String name) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onIfaceRemoved ");
            stringBuilder.append(type);
            stringBuilder.append(", name: ");
            stringBuilder.append(name);
            wifiLog.d(stringBuilder.toString());
        }

        public void onDebugRingBufferDataAvailable(WifiDebugRingBufferStatus status, ArrayList<Byte> data) {
            WifiVendorHal.this.mHalEventHandler.post(new -$$Lambda$WifiVendorHal$ChipEventCallback$AqzJie2OoFIziDRxaXrZoSkKfNw(this, status, data));
        }

        /* JADX WARNING: Missing block: B:10:0x0017, code skipped:
            r0 = r8.size();
            r2 = false;
     */
        /* JADX WARNING: Missing block: B:12:?, code skipped:
            r1.onRingBufferData(com.android.server.wifi.WifiVendorHal.access$1500(r7), com.android.server.wifi.util.NativeUtil.byteArrayFromArrayList(r8));
     */
        /* JADX WARNING: Missing block: B:13:0x002c, code skipped:
            if (r8.size() == r0) goto L_0x0032;
     */
        /* JADX WARNING: Missing block: B:14:0x002e, code skipped:
            r2 = true;
     */
        /* JADX WARNING: Missing block: B:16:0x0031, code skipped:
            r2 = true;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static /* synthetic */ void lambda$onDebugRingBufferDataAvailable$0(ChipEventCallback chipEventCallback, WifiDebugRingBufferStatus status, ArrayList data) {
            synchronized (WifiVendorHal.sLock) {
                if (!(WifiVendorHal.this.mLogEventHandler == null || status == null)) {
                    if (data != null) {
                        WifiLoggerEventHandler eventHandler = WifiVendorHal.this.mLogEventHandler;
                    }
                }
                return;
            }
            if (conversionFailure) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Conversion failure detected in onDebugRingBufferDataAvailable. The input ArrayList |data| is potentially corrupted. Starting size=");
                stringBuilder.append(sizeBefore);
                stringBuilder.append(", final size=");
                stringBuilder.append(data.size());
                Log.wtf("WifiVendorHal", stringBuilder.toString());
            }
        }

        public void onDebugErrorAlert(int errorCode, ArrayList<Byte> debugData) {
            WifiLog wifiLog = WifiVendorHal.this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDebugErrorAlert ");
            stringBuilder.append(errorCode);
            wifiLog.w(stringBuilder.toString());
            WifiVendorHal.this.mHalEventHandler.post(new -$$Lambda$WifiVendorHal$ChipEventCallback$opFP1g0mCa0rIEtg63LvzlqySHc(this, debugData, errorCode));
        }

        public static /* synthetic */ void lambda$onDebugErrorAlert$1(ChipEventCallback chipEventCallback, ArrayList debugData, int errorCode) {
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mLogEventHandler != null) {
                    if (debugData != null) {
                        WifiLoggerEventHandler eventHandler = WifiVendorHal.this.mLogEventHandler;
                        eventHandler.onWifiAlert(errorCode, NativeUtil.byteArrayFromArrayList(debugData));
                        return;
                    }
                }
            }
        }
    }

    private class RttEventCallback extends IWifiRttControllerEventCallback.Stub {
        private RttEventCallback() {
        }

        /* JADX WARNING: Missing block: B:10:0x0021, code skipped:
            r0 = new android.net.wifi.RttManager.RttResult[r6.size()];
     */
        /* JADX WARNING: Missing block: B:11:0x0028, code skipped:
            r2 = r3;
     */
        /* JADX WARNING: Missing block: B:12:0x002a, code skipped:
            if (r2 >= r0.length) goto L_0x003b;
     */
        /* JADX WARNING: Missing block: B:13:0x002c, code skipped:
            r0[r2] = com.android.server.wifi.WifiVendorHal.frameworkRttResultFromHalRttResult((android.hardware.wifi.V1_0.RttResult) r6.get(r2));
            r3 = r2 + 1;
     */
        /* JADX WARNING: Missing block: B:14:0x003b, code skipped:
            r1.onRttResults(r0);
     */
        /* JADX WARNING: Missing block: B:15:0x003e, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onResults(int cmdId, ArrayList<RttResult> results) {
            synchronized (WifiVendorHal.sLock) {
                if (cmdId == WifiVendorHal.this.mRttCmdId) {
                    if (WifiVendorHal.this.mRttEventHandler != null) {
                        RttEventHandler eventHandler = WifiVendorHal.this.mRttEventHandler;
                        int i = 0;
                        WifiVendorHal.this.mRttCmdId = 0;
                    }
                }
            }
        }
    }

    private class StaIfaceEventCallback extends IWifiStaIfaceEventCallback.Stub {
        private StaIfaceEventCallback() {
        }

        public void onBackgroundScanFailure(int cmdId) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBackgroundScanFailure ");
            stringBuilder.append(cmdId);
            wifiLog.d(stringBuilder.toString());
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mScan != null) {
                    if (cmdId == WifiVendorHal.this.mScan.cmdId) {
                        ScanEventHandler eventHandler = WifiVendorHal.this.mScan.eventHandler;
                        eventHandler.onScanStatus(3);
                        return;
                    }
                }
            }
        }

        public void onBackgroundFullScanResult(int cmdId, int bucketsScanned, StaScanResult result) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBackgroundFullScanResult ");
            stringBuilder.append(cmdId);
            wifiLog.d(stringBuilder.toString());
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mScan != null) {
                    if (cmdId == WifiVendorHal.this.mScan.cmdId) {
                        ScanEventHandler eventHandler = WifiVendorHal.this.mScan.eventHandler;
                        eventHandler.onFullScanResult(WifiVendorHal.hidlToFrameworkScanResult(result), bucketsScanned);
                        return;
                    }
                }
            }
        }

        public void onBackgroundScanResults(int cmdId, ArrayList<StaScanData> scanDatas) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBackgroundScanResults ");
            stringBuilder.append(cmdId);
            wifiLog.d(stringBuilder.toString());
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mScan != null) {
                    if (cmdId == WifiVendorHal.this.mScan.cmdId) {
                        ScanEventHandler eventHandler = WifiVendorHal.this.mScan.eventHandler;
                        WifiVendorHal.this.mScan.latestScanResults = WifiVendorHal.hidlToFrameworkScanDatas(cmdId, scanDatas);
                        eventHandler.onScanStatus(0);
                        return;
                    }
                }
            }
        }

        public void onRssiThresholdBreached(int cmdId, byte[] currBssid, int currRssi) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRssiThresholdBreached ");
            stringBuilder.append(cmdId);
            stringBuilder.append("currRssi ");
            stringBuilder.append(currRssi);
            wifiLog.d(stringBuilder.toString());
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mWifiRssiEventHandler != null) {
                    if (cmdId == WifiVendorHal.sRssiMonCmdId) {
                        WifiRssiEventHandler eventHandler = WifiVendorHal.this.mWifiRssiEventHandler;
                        eventHandler.onRssiThresholdBreached((byte) currRssi);
                        return;
                    }
                }
            }
        }
    }

    private class ChipEventCallbackV12 extends IWifiChipEventCallback.Stub {
        private ChipEventCallbackV12() {
        }

        public void onChipReconfigured(int modeId) {
            WifiVendorHal.this.mIWifiChipEventCallback.onChipReconfigured(modeId);
        }

        public void onChipReconfigureFailure(WifiStatus status) {
            WifiVendorHal.this.mIWifiChipEventCallback.onChipReconfigureFailure(status);
        }

        public void onIfaceAdded(int type, String name) {
            WifiVendorHal.this.mIWifiChipEventCallback.onIfaceAdded(type, name);
        }

        public void onIfaceRemoved(int type, String name) {
            WifiVendorHal.this.mIWifiChipEventCallback.onIfaceRemoved(type, name);
        }

        public void onDebugRingBufferDataAvailable(WifiDebugRingBufferStatus status, ArrayList<Byte> data) {
            WifiVendorHal.this.mIWifiChipEventCallback.onDebugRingBufferDataAvailable(status, data);
        }

        public void onDebugErrorAlert(int errorCode, ArrayList<Byte> debugData) {
            WifiVendorHal.this.mIWifiChipEventCallback.onDebugErrorAlert(errorCode, debugData);
        }

        private boolean areSameIfaceNames(List<IfaceInfo> ifaceList1, List<IfaceInfo> ifaceList2) {
            return ((List) ifaceList1.stream().map(-$$Lambda$WifiVendorHal$ChipEventCallbackV12$SrMSOw3LUVF_Z64G_aL0Tguwt3A.INSTANCE).collect(Collectors.toList())).containsAll((List) ifaceList2.stream().map(-$$Lambda$WifiVendorHal$ChipEventCallbackV12$nAuRYe8SQ_MJ2XaULMZNtUE0izo.INSTANCE).collect(Collectors.toList()));
        }

        private boolean areSameIfaces(List<IfaceInfo> ifaceList1, List<IfaceInfo> ifaceList2) {
            return ifaceList1.containsAll(ifaceList2);
        }

        /* JADX WARNING: Missing block: B:10:0x0032, code skipped:
            if (r9.size() == 0) goto L_0x010a;
     */
        /* JADX WARNING: Missing block: B:12:0x0039, code skipped:
            if (r9.size() <= 2) goto L_0x003d;
     */
        /* JADX WARNING: Missing block: B:13:0x003d, code skipped:
            r3 = (android.hardware.wifi.V1_2.IWifiChipEventCallback.RadioModeInfo) r9.get(0);
     */
        /* JADX WARNING: Missing block: B:14:0x0049, code skipped:
            if (r9.size() != 2) goto L_0x0052;
     */
        /* JADX WARNING: Missing block: B:15:0x004b, code skipped:
            r4 = (android.hardware.wifi.V1_2.IWifiChipEventCallback.RadioModeInfo) r9.get(1);
     */
        /* JADX WARNING: Missing block: B:16:0x0052, code skipped:
            r4 = null;
     */
        /* JADX WARNING: Missing block: B:17:0x0053, code skipped:
            if (r4 == null) goto L_0x0090;
     */
        /* JADX WARNING: Missing block: B:19:0x0061, code skipped:
            if (r3.ifaceInfos.size() == r4.ifaceInfos.size()) goto L_0x0090;
     */
        /* JADX WARNING: Missing block: B:20:0x0063, code skipped:
            r0 = r8.this$0.mLog;
            r2 = new java.lang.StringBuilder();
            r2.append("Unexpected number of iface info in list ");
            r2.append(r3.ifaceInfos.size());
            r2.append(", ");
            r2.append(r4.ifaceInfos.size());
            r0.e(r2.toString());
     */
        /* JADX WARNING: Missing block: B:21:0x008f, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:22:0x0090, code skipped:
            r6 = r3.ifaceInfos.size();
     */
        /* JADX WARNING: Missing block: B:23:0x0096, code skipped:
            if (r6 == 0) goto L_0x00f1;
     */
        /* JADX WARNING: Missing block: B:24:0x0098, code skipped:
            if (r6 <= 2) goto L_0x009b;
     */
        /* JADX WARNING: Missing block: B:26:0x009f, code skipped:
            if (r9.size() != 2) goto L_0x00c7;
     */
        /* JADX WARNING: Missing block: B:27:0x00a1, code skipped:
            if (r6 != 1) goto L_0x00c7;
     */
        /* JADX WARNING: Missing block: B:29:0x00ab, code skipped:
            if (areSameIfaceNames(r3.ifaceInfos, r4.ifaceInfos) == false) goto L_0x00b7;
     */
        /* JADX WARNING: Missing block: B:30:0x00ad, code skipped:
            r8.this$0.mLog.e("Unexpected for both radio infos to have same iface");
     */
        /* JADX WARNING: Missing block: B:31:0x00b6, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:33:0x00bb, code skipped:
            if (r3.bandInfo == r4.bandInfo) goto L_0x00c1;
     */
        /* JADX WARNING: Missing block: B:34:0x00bd, code skipped:
            r1.onDbs();
     */
        /* JADX WARNING: Missing block: B:35:0x00c1, code skipped:
            r1.onSbs(r3.bandInfo);
     */
        /* JADX WARNING: Missing block: B:37:0x00cb, code skipped:
            if (r9.size() != 1) goto L_0x00f0;
     */
        /* JADX WARNING: Missing block: B:38:0x00cd, code skipped:
            if (r6 != 2) goto L_0x00f0;
     */
        /* JADX WARNING: Missing block: B:40:0x00e3, code skipped:
            if (((android.hardware.wifi.V1_2.IWifiChipEventCallback.IfaceInfo) r3.ifaceInfos.get(0)).channel == ((android.hardware.wifi.V1_2.IWifiChipEventCallback.IfaceInfo) r3.ifaceInfos.get(1)).channel) goto L_0x00eb;
     */
        /* JADX WARNING: Missing block: B:41:0x00e5, code skipped:
            r1.onMcc(r3.bandInfo);
     */
        /* JADX WARNING: Missing block: B:42:0x00eb, code skipped:
            r1.onScc(r3.bandInfo);
     */
        /* JADX WARNING: Missing block: B:43:0x00f0, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:44:0x00f1, code skipped:
            r0 = r8.this$0.mLog;
            r2 = new java.lang.StringBuilder();
            r2.append("Unexpected number of iface info in list ");
            r2.append(r6);
            r0.e(r2.toString());
     */
        /* JADX WARNING: Missing block: B:45:0x0109, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:46:0x010a, code skipped:
            r0 = r8.this$0.mLog;
            r2 = new java.lang.StringBuilder();
            r2.append("Unexpected number of radio info in list ");
            r2.append(r9.size());
            r0.e(r2.toString());
     */
        /* JADX WARNING: Missing block: B:47:0x0126, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onRadioModeChange(ArrayList<RadioModeInfo> radioModeInfoList) {
            WifiLog wifiLog = WifiVendorHal.this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRadioModeChange ");
            stringBuilder.append(radioModeInfoList);
            wifiLog.d(stringBuilder.toString());
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mRadioModeChangeEventHandler != null) {
                    if (radioModeInfoList != null) {
                        VendorHalRadioModeChangeEventHandler handler = WifiVendorHal.this.mRadioModeChangeEventHandler;
                    }
                }
            }
        }
    }

    public void enableVerboseLogging(boolean verbose) {
        synchronized (sLock) {
            if (verbose) {
                try {
                    this.mVerboseLog = this.mLog;
                    enter("verbose=true").flush();
                } catch (Throwable th) {
                }
            } else {
                enter("verbose=false").flush();
                this.mVerboseLog = sNoLog;
            }
        }
    }

    private boolean ok(WifiStatus status) {
        if (status.code == 0) {
            return true;
        }
        this.mLog.err("% failed %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(status.toString()).flush();
        return false;
    }

    private boolean boolResult(boolean result) {
        if (this.mVerboseLog == sNoLog) {
            return result;
        }
        this.mVerboseLog.err("% returns %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(result).flush();
        return result;
    }

    private String stringResult(String result) {
        if (this.mVerboseLog == sNoLog) {
            return result;
        }
        this.mVerboseLog.err("% returns %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(result).flush();
        return result;
    }

    private byte[] byteArrayResult(byte[] result) {
        if (this.mVerboseLog == sNoLog) {
            return result;
        }
        this.mVerboseLog.err("% returns %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(HexDump.dumpHexString(result)).flush();
        return result;
    }

    private LogMessage enter(String format) {
        if (this.mVerboseLog == sNoLog) {
            return sNoLog.info(format);
        }
        return this.mVerboseLog.trace(format, 1);
    }

    private static String niceMethodName(StackTraceElement[] trace, int start) {
        if (start >= trace.length) {
            return "";
        }
        StackTraceElement s = trace[start];
        String name = s.getMethodName();
        if (name.contains("lambda$")) {
            String myFile = s.getFileName();
            if (myFile != null) {
                for (int i = start + 1; i < trace.length; i++) {
                    if (myFile.equals(trace[i].getFileName())) {
                        name = trace[i].getMethodName();
                        break;
                    }
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append("(l.");
        stringBuilder.append(s.getLineNumber());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public WifiVendorHal(HalDeviceManager halDeviceManager, Looper looper) {
        this.mHalDeviceManager = halDeviceManager;
        this.mLooper = looper;
        this.mHalEventHandler = new Handler(looper);
        this.mHalDeviceManagerStatusCallbacks = new HalDeviceManagerStatusListener();
        this.mIWifiStaIfaceEventCallback = new StaIfaceEventCallback();
        this.mIWifiChipEventCallback = new ChipEventCallback();
        this.mIWifiChipEventCallbackV12 = new ChipEventCallbackV12();
        this.mRttEventCallback = new RttEventCallback();
    }

    private void handleRemoteException(RemoteException e) {
        this.mVerboseLog.err("% RemoteException in HIDL call %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(e.toString()).flush();
        clearState();
    }

    public boolean initialize(VendorHalDeathEventHandler handler) {
        synchronized (sLock) {
            this.mHalDeviceManager.initialize();
            this.mHalDeviceManager.registerStatusListener(this.mHalDeviceManagerStatusCallbacks, null);
            this.mDeathEventHandler = handler;
        }
        return true;
    }

    public void registerRadioModeChangeHandler(VendorHalRadioModeChangeEventHandler handler) {
        synchronized (sLock) {
            this.mRadioModeChangeEventHandler = handler;
        }
    }

    public boolean isVendorHalSupported() {
        boolean isSupported;
        synchronized (sLock) {
            isSupported = this.mHalDeviceManager.isSupported();
        }
        return isSupported;
    }

    public boolean startVendorHalAp() {
        synchronized (sLock) {
            if (!startVendorHal()) {
                return false;
            } else if (TextUtils.isEmpty(createApIface(null))) {
                stopVendorHal();
                return false;
            } else {
                return true;
            }
        }
    }

    public boolean startVendorHalSta() {
        synchronized (sLock) {
            if (!startVendorHal()) {
                return false;
            } else if (TextUtils.isEmpty(createStaIface(false, null))) {
                stopVendorHal();
                return false;
            } else {
                return true;
            }
        }
    }

    public boolean startVendorHal() {
        synchronized (sLock) {
            if (this.mHalDeviceManager.start()) {
                this.mLog.info("Vendor Hal started successfully").flush();
                return true;
            }
            this.mLog.err("Failed to start vendor HAL").flush();
            return false;
        }
    }

    private IWifiStaIface getStaIface(String ifaceName) {
        IWifiStaIface iWifiStaIface;
        synchronized (sLock) {
            iWifiStaIface = (IWifiStaIface) this.mIWifiStaIfaces.get(ifaceName);
        }
        return iWifiStaIface;
    }

    public String createStaIface(boolean lowPrioritySta, InterfaceDestroyedListener destroyedListener) {
        synchronized (sLock) {
            IWifiStaIface iface = this.mHalDeviceManager.createStaIface(lowPrioritySta, new StaInterfaceDestroyedListenerInternal(destroyedListener), null);
            String stringResult;
            if (iface == null) {
                this.mLog.err("Failed to create STA iface").flush();
                stringResult = stringResult(null);
                return stringResult;
            }
            HalDeviceManager halDeviceManager = this.mHalDeviceManager;
            stringResult = HalDeviceManager.getName(iface);
            String stringResult2;
            if (TextUtils.isEmpty(stringResult)) {
                this.mLog.err("Failed to get iface name").flush();
                stringResult2 = stringResult(null);
                return stringResult2;
            } else if (registerStaIfaceCallback(iface)) {
                this.mIWifiRttController = this.mHalDeviceManager.createRttController();
                if (this.mIWifiRttController == null) {
                    this.mLog.err("Failed to create RTT controller").flush();
                    stringResult2 = stringResult(null);
                    return stringResult2;
                } else if (!registerRttEventCallback()) {
                    this.mLog.err("Failed to register RTT controller callback").flush();
                    stringResult2 = stringResult(null);
                    return stringResult2;
                } else if (retrieveWifiChip(iface)) {
                    enableLinkLayerStats(iface);
                    this.mIWifiStaIfaces.put(stringResult, iface);
                    return stringResult;
                } else {
                    this.mLog.err("Failed to get wifi chip").flush();
                    stringResult2 = stringResult(null);
                    return stringResult2;
                }
            } else {
                this.mLog.err("Failed to register STA iface callback").flush();
                stringResult2 = stringResult(null);
                return stringResult2;
            }
        }
    }

    public boolean removeStaIface(String ifaceName) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            boolean boolResult;
            if (iface == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mHalDeviceManager.removeIface(iface)) {
                this.mIWifiStaIfaces.remove(ifaceName);
                return true;
            } else {
                this.mLog.err("Failed to remove STA iface").flush();
                boolResult = boolResult(false);
                return boolResult;
            }
        }
    }

    private IWifiApIface getApIface(String ifaceName) {
        IWifiApIface iWifiApIface;
        synchronized (sLock) {
            iWifiApIface = (IWifiApIface) this.mIWifiApIfaces.get(ifaceName);
        }
        return iWifiApIface;
    }

    public String createApIface(InterfaceDestroyedListener destroyedListener) {
        synchronized (sLock) {
            IWifiApIface iface = this.mHalDeviceManager.createApIface(new ApInterfaceDestroyedListenerInternal(destroyedListener), null);
            String stringResult;
            if (iface == null) {
                this.mLog.err("Failed to create AP iface").flush();
                stringResult = stringResult(null);
                return stringResult;
            }
            HalDeviceManager halDeviceManager = this.mHalDeviceManager;
            stringResult = HalDeviceManager.getName(iface);
            String stringResult2;
            if (TextUtils.isEmpty(stringResult)) {
                this.mLog.err("Failed to get iface name").flush();
                stringResult2 = stringResult(null);
                return stringResult2;
            } else if (retrieveWifiChip(iface)) {
                this.mIWifiApIfaces.put(stringResult, iface);
                return stringResult;
            } else {
                this.mLog.err("Failed to get wifi chip").flush();
                stringResult2 = stringResult(null);
                return stringResult2;
            }
        }
    }

    public boolean removeApIface(String ifaceName) {
        synchronized (sLock) {
            IWifiApIface iface = getApIface(ifaceName);
            boolean boolResult;
            if (iface == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mHalDeviceManager.removeIface(iface)) {
                this.mIWifiApIfaces.remove(ifaceName);
                return true;
            } else {
                this.mLog.err("Failed to remove AP iface").flush();
                boolResult = boolResult(false);
                return boolResult;
            }
        }
    }

    private boolean retrieveWifiChip(IWifiIface iface) {
        synchronized (sLock) {
            boolean registrationNeeded = this.mIWifiChip == null;
            this.mIWifiChip = this.mHalDeviceManager.getChip(iface);
            if (this.mIWifiChip == null) {
                this.mLog.err("Failed to get the chip created for the Iface").flush();
                return false;
            } else if (!registrationNeeded) {
                return true;
            } else if (registerChipCallback()) {
                return true;
            } else {
                this.mLog.err("Failed to register chip callback").flush();
                return false;
            }
        }
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:4:0x0006, B:15:0x0018] */
    /* JADX WARNING: Missing block: B:20:0x0024, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:0x0025, code skipped:
            handleRemoteException(r2);
     */
    /* JADX WARNING: Missing block: B:23:0x0029, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean registerStaIfaceCallback(IWifiStaIface iface) {
        synchronized (sLock) {
            boolean boolResult;
            if (iface == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mIWifiStaIfaceEventCallback == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else {
                boolean ok = ok(iface.registerEventCallback(this.mIWifiStaIfaceEventCallback));
                return ok;
            }
        }
    }

    private boolean registerChipCallback() {
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                WifiStatus status;
                android.hardware.wifi.V1_2.IWifiChip iWifiChipV12 = getWifiChipForV1_2Mockable();
                if (iWifiChipV12 != null) {
                    status = iWifiChipV12.registerEventCallback_1_2(this.mIWifiChipEventCallbackV12);
                } else {
                    status = this.mIWifiChip.registerEventCallback(this.mIWifiChipEventCallback);
                }
                boolean ok = ok(status);
                return ok;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    private boolean registerRttEventCallback() {
        synchronized (sLock) {
            boolean boolResult;
            if (this.mIWifiRttController == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mRttEventCallback == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else {
                try {
                    boolean ok = ok(this.mIWifiRttController.registerEventCallback(this.mRttEventCallback));
                    return ok;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            }
        }
    }

    public void stopVendorHal() {
        synchronized (sLock) {
            this.mHalDeviceManager.stop();
            clearState();
            this.mLog.info("Vendor Hal stopped").flush();
        }
    }

    private void clearState() {
        this.mIWifiChip = null;
        this.mIWifiStaIfaces.clear();
        this.mIWifiApIfaces.clear();
        this.mIWifiRttController = null;
        this.mDriverDescription = null;
        this.mFirmwareDescription = null;
    }

    public boolean isHalStarted() {
        boolean z;
        synchronized (sLock) {
            if (this.mIWifiStaIfaces.isEmpty()) {
                if (this.mIWifiApIfaces.isEmpty()) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public boolean getBgScanCapabilities(String ifaceName, ScanCapabilities capabilities) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                MutableBoolean ans = new MutableBoolean(false);
                iface.getBackgroundScanCapabilities(new -$$Lambda$WifiVendorHal$qPUuRnlo2XMDrsA1gI_KLrbvPAI(this, capabilities, ans));
                boolean z = ans.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static /* synthetic */ void lambda$getBgScanCapabilities$0(WifiVendorHal wifiVendorHal, ScanCapabilities out, MutableBoolean ans, WifiStatus status, StaBackgroundScanCapabilities cap) {
        if (wifiVendorHal.ok(status)) {
            wifiVendorHal.mVerboseLog.info("scan capabilities %").c(cap.toString()).flush();
            out.max_scan_cache_size = cap.maxCacheSize;
            out.max_ap_cache_per_scan = cap.maxApCachePerScan;
            out.max_scan_buckets = cap.maxBuckets;
            out.max_rssi_sample_size = 0;
            out.max_scan_reporting_threshold = cap.maxReportingThreshold;
            ans.value = true;
        }
    }

    private StaBackgroundScanBucketParameters makeStaBackgroundScanBucketParametersFromBucketSettings(BucketSettings bs) {
        StaBackgroundScanBucketParameters pa = new StaBackgroundScanBucketParameters();
        pa.bucketIdx = bs.bucket;
        pa.band = makeWifiBandFromFrameworkBand(bs.band);
        if (bs.channels != null) {
            for (ChannelSettings cs : bs.channels) {
                pa.frequencies.add(Integer.valueOf(cs.frequency));
            }
        }
        pa.periodInMs = bs.period_ms;
        pa.eventReportScheme = makeReportSchemeFromBucketSettingsReportEvents(bs.report_events);
        pa.exponentialMaxPeriodInMs = bs.max_period_ms;
        pa.exponentialBase = 2;
        pa.exponentialStepCount = bs.step_count;
        return pa;
    }

    private int makeWifiBandFromFrameworkBand(int frameworkBand) {
        switch (frameworkBand) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad band ");
                stringBuilder.append(frameworkBand);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private int makeReportSchemeFromBucketSettingsReportEvents(int reportUnderscoreEvents) {
        int ans = 0;
        BitMask in = new BitMask(reportUnderscoreEvents);
        if (in.testAndClear(1)) {
            ans = 0 | 1;
        }
        if (in.testAndClear(2)) {
            ans |= 2;
        }
        if (in.testAndClear(4)) {
            ans |= 4;
        }
        if (in.value == 0) {
            return ans;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad ");
        stringBuilder.append(reportUnderscoreEvents);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean startBgScan(String ifaceName, ScanSettings settings, ScanEventHandler eventHandler) {
        if (eventHandler == null) {
            return boolResult(false);
        }
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                if (!(this.mScan == null || this.mScan.paused)) {
                    ok(iface.stopBackgroundScan(this.mScan.cmdId));
                    this.mScan = null;
                }
                this.mLastScanCmdId = (this.mLastScanCmdId % 9) + 1;
                CurrentBackgroundScan scan = new CurrentBackgroundScan(this.mLastScanCmdId, settings);
                if (ok(iface.startBackgroundScan(scan.cmdId, scan.param))) {
                    scan.eventHandler = eventHandler;
                    this.mScan = scan;
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public void stopBgScan(String ifaceName) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                return;
            }
            try {
                if (this.mScan != null) {
                    ok(iface.stopBackgroundScan(this.mScan.cmdId));
                    this.mScan = null;
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void pauseBgScan(String ifaceName) {
        synchronized (sLock) {
            try {
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface == null) {
                    return;
                } else if (!(this.mScan == null || this.mScan.paused)) {
                    if (ok(iface.stopBackgroundScan(this.mScan.cmdId))) {
                        this.mScan.paused = true;
                    } else {
                        return;
                    }
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void restartBgScan(String ifaceName) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                return;
            }
            try {
                if (this.mScan != null && this.mScan.paused) {
                    if (ok(iface.startBackgroundScan(this.mScan.cmdId, this.mScan.param))) {
                        this.mScan.paused = false;
                    } else {
                        return;
                    }
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public ScanData[] getBgScanResults(String ifaceName) {
        synchronized (sLock) {
            if (getStaIface(ifaceName) == null) {
                return null;
            } else if (this.mScan == null) {
                return null;
            } else {
                ScanData[] scanDataArr = this.mScan.latestScanResults;
                return scanDataArr;
            }
        }
    }

    public WifiLinkLayerStats getWifiLinkLayerStats(String ifaceName) {
        AnonymousClass1AnswerBox answer = new Object() {
            public StaLinkLayerStats value = null;
        };
        synchronized (sLock) {
            try {
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface == null) {
                    return null;
                }
                iface.getLinkLayerStats(new -$$Lambda$WifiVendorHal$Cu5ECBYZ9xFCAH1Q99vuft6nyvY(this, answer));
                return frameworkFromHalLinkLayerStats(answer.value);
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getWifiLinkLayerStats$1(WifiVendorHal wifiVendorHal, AnonymousClass1AnswerBox answer, WifiStatus status, StaLinkLayerStats stats) {
        if (wifiVendorHal.ok(status)) {
            answer.value = stats;
        }
    }

    @VisibleForTesting
    static WifiLinkLayerStats frameworkFromHalLinkLayerStats(StaLinkLayerStats stats) {
        if (stats == null) {
            return null;
        }
        WifiLinkLayerStats out = new WifiLinkLayerStats();
        out.beacon_rx = stats.iface.beaconRx;
        out.rssi_mgmt = stats.iface.avgRssiMgmt;
        out.rxmpdu_be = stats.iface.wmeBePktStats.rxMpdu;
        out.txmpdu_be = stats.iface.wmeBePktStats.txMpdu;
        out.lostmpdu_be = stats.iface.wmeBePktStats.lostMpdu;
        out.retries_be = stats.iface.wmeBePktStats.retries;
        out.rxmpdu_bk = stats.iface.wmeBkPktStats.rxMpdu;
        out.txmpdu_bk = stats.iface.wmeBkPktStats.txMpdu;
        out.lostmpdu_bk = stats.iface.wmeBkPktStats.lostMpdu;
        out.retries_bk = stats.iface.wmeBkPktStats.retries;
        out.rxmpdu_vi = stats.iface.wmeViPktStats.rxMpdu;
        out.txmpdu_vi = stats.iface.wmeViPktStats.txMpdu;
        out.lostmpdu_vi = stats.iface.wmeViPktStats.lostMpdu;
        out.retries_vi = stats.iface.wmeViPktStats.retries;
        out.rxmpdu_vo = stats.iface.wmeVoPktStats.rxMpdu;
        out.txmpdu_vo = stats.iface.wmeVoPktStats.txMpdu;
        out.lostmpdu_vo = stats.iface.wmeVoPktStats.lostMpdu;
        out.retries_vo = stats.iface.wmeVoPktStats.retries;
        if (stats.radios.size() > 0) {
            int i = 0;
            StaLinkLayerRadioStats radioStats = (StaLinkLayerRadioStats) stats.radios.get(0);
            out.on_time = radioStats.onTimeInMs;
            out.tx_time = radioStats.txTimeInMs;
            out.tx_time_per_level = new int[radioStats.txTimeInMsPerLevel.size()];
            while (i < out.tx_time_per_level.length) {
                out.tx_time_per_level[i] = ((Integer) radioStats.txTimeInMsPerLevel.get(i)).intValue();
                i++;
            }
            out.rx_time = radioStats.rxTimeInMs;
            out.on_time_scan = radioStats.onTimeInMsForScan;
        }
        out.timeStampInMs = stats.timeStampInMs;
        return out;
    }

    private void enableLinkLayerStats(IWifiStaIface iface) {
        synchronized (sLock) {
            try {
                if (!ok(iface.enableLinkLayerStatsCollection(this.mLinkLayerStatsDebug))) {
                    this.mLog.err("unable to enable link layer stats collection").flush();
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    @VisibleForTesting
    int wifiFeatureMaskFromChipCapabilities(int capabilities) {
        int features = 0;
        for (int i = 0; i < sChipFeatureCapabilityTranslation.length; i++) {
            if ((sChipFeatureCapabilityTranslation[i][1] & capabilities) != 0) {
                features |= sChipFeatureCapabilityTranslation[i][0];
            }
        }
        return features;
    }

    @VisibleForTesting
    int wifiFeatureMaskFromStaCapabilities(int capabilities) {
        int features = 0;
        for (int i = 0; i < sStaFeatureCapabilityTranslation.length; i++) {
            if ((sStaFeatureCapabilityTranslation[i][1] & capabilities) != 0) {
                features |= sStaFeatureCapabilityTranslation[i][0];
            }
        }
        return features;
    }

    public int getSupportedFeatureSet(String ifaceName) {
        if (!this.mHalDeviceManager.isStarted()) {
            return 0;
        }
        try {
            MutableInt feat = new MutableInt(0);
            synchronized (sLock) {
                if (this.mIWifiChip != null) {
                    this.mIWifiChip.getCapabilities(new -$$Lambda$WifiVendorHal$bXzROfFjRqOgC9QmMk6fP3MnLSg(this, feat));
                }
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface != null) {
                    iface.getCapabilities(new -$$Lambda$WifiVendorHal$Lnl0TvBZpgQMVgoYAtSlApp_k88(this, feat));
                }
            }
            int featureSet = feat.value;
            feat = this.mHalDeviceManager.getSupportedIfaceTypes();
            if (feat.contains(Integer.valueOf(0))) {
                featureSet |= 1;
            }
            if (feat.contains(Integer.valueOf(1))) {
                featureSet |= 16;
            }
            if (feat.contains(Integer.valueOf(2))) {
                featureSet |= 8;
            }
            if (feat.contains(Integer.valueOf(3))) {
                featureSet |= 64;
            }
            return featureSet;
        } catch (RemoteException e) {
            handleRemoteException(e);
            return 0;
        }
    }

    public static /* synthetic */ void lambda$getSupportedFeatureSet$2(WifiVendorHal wifiVendorHal, MutableInt feat, WifiStatus status, int capabilities) {
        if (wifiVendorHal.ok(status)) {
            feat.value = wifiVendorHal.wifiFeatureMaskFromChipCapabilities(capabilities);
        }
    }

    public static /* synthetic */ void lambda$getSupportedFeatureSet$3(WifiVendorHal wifiVendorHal, MutableInt feat, WifiStatus status, int capabilities) {
        if (wifiVendorHal.ok(status)) {
            feat.value |= wifiVendorHal.wifiFeatureMaskFromStaCapabilities(capabilities);
        }
    }

    public RttCapabilities getRttCapabilities() {
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return null;
            }
            try {
                AnonymousClass2AnswerBox box = new Object() {
                    public RttCapabilities value = null;
                };
                this.mIWifiRttController.getCapabilities(new -$$Lambda$WifiVendorHal$j9-GquCvCUY0kL-ke7FWj2rB-_I(this, box));
                RttCapabilities rttCapabilities = box.value;
                return rttCapabilities;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getRttCapabilities$4(WifiVendorHal wifiVendorHal, AnonymousClass2AnswerBox box, WifiStatus status, android.hardware.wifi.V1_0.RttCapabilities capabilities) {
        if (wifiVendorHal.ok(status)) {
            wifiVendorHal.mVerboseLog.info("rtt capabilites %").c(capabilities.toString()).flush();
            RttCapabilities ans = new RttCapabilities();
            ans.oneSidedRttSupported = capabilities.rttOneSidedSupported;
            ans.twoSided11McRttSupported = capabilities.rttFtmSupported;
            ans.lciSupported = capabilities.lciSupported;
            ans.lcrSupported = capabilities.lcrSupported;
            ans.preambleSupported = frameworkPreambleFromHalPreamble(capabilities.preambleSupport);
            ans.bwSupported = frameworkBwFromHalBw(capabilities.bwSupport);
            ans.responderSupported = capabilities.responderSupported;
            ans.secureRttSupported = false;
            ans.mcVersion = capabilities.mcVersion & Constants.BYTE_MASK;
            box.value = ans;
        }
    }

    @VisibleForTesting
    static RttManager.RttResult frameworkRttResultFromHalRttResult(RttResult result) {
        RttManager.RttResult ans = new RttManager.RttResult();
        ans.bssid = NativeUtil.macAddressFromByteArray(result.addr);
        ans.burstNumber = result.burstNum;
        ans.measurementFrameNumber = result.measurementNumber;
        ans.successMeasurementFrameNumber = result.successNumber;
        ans.frameNumberPerBurstPeer = result.numberPerBurstPeer;
        ans.status = result.status;
        ans.retryAfterDuration = result.retryAfterDuration;
        ans.measurementType = result.type;
        ans.rssi = result.rssi;
        ans.rssiSpread = result.rssiSpread;
        ans.txRate = result.txRate.bitRateInKbps;
        ans.rxRate = result.rxRate.bitRateInKbps;
        ans.rtt = result.rtt;
        ans.rttStandardDeviation = result.rttSd;
        ans.rttSpread = result.rttSpread;
        ans.distance = result.distanceInMm / 10;
        ans.distanceStandardDeviation = result.distanceSdInMm / 10;
        ans.distanceSpread = result.distanceSpreadInMm / 10;
        ans.ts = result.timeStampInUs;
        ans.burstDuration = result.burstDurationInMs;
        ans.negotiatedBurstNum = result.negotiatedBurstNum;
        ans.LCI = ieFromHal(result.lci);
        ans.LCR = ieFromHal(result.lcr);
        ans.secure = false;
        return ans;
    }

    @VisibleForTesting
    static WifiInformationElement ieFromHal(android.hardware.wifi.V1_0.WifiInformationElement ie) {
        if (ie == null) {
            return null;
        }
        WifiInformationElement ans = new WifiInformationElement();
        ans.id = ie.id;
        ans.data = NativeUtil.byteArrayFromArrayList(ie.data);
        return ans;
    }

    @VisibleForTesting
    static RttConfig halRttConfigFromFrameworkRttParams(RttParams params) {
        RttConfig rttConfig = new RttConfig();
        if (params.bssid != null) {
            byte[] addr = NativeUtil.macAddressToByteArray(params.bssid);
            for (int i = 0; i < rttConfig.addr.length; i++) {
                rttConfig.addr[i] = addr[i];
            }
        }
        rttConfig.type = halRttTypeFromFrameworkRttType(params.requestType);
        rttConfig.peer = halPeerFromFrameworkPeer(params.deviceType);
        rttConfig.channel.width = halChannelWidthFromFrameworkChannelWidth(params.channelWidth);
        rttConfig.channel.centerFreq = params.frequency;
        rttConfig.channel.centerFreq0 = params.centerFreq0;
        rttConfig.channel.centerFreq1 = params.centerFreq1;
        rttConfig.burstPeriod = params.interval;
        rttConfig.numBurst = params.numberBurst;
        rttConfig.numFramesPerBurst = params.numSamplesPerBurst;
        rttConfig.numRetriesPerRttFrame = params.numRetriesPerMeasurementFrame;
        rttConfig.numRetriesPerFtmr = params.numRetriesPerFTMR;
        rttConfig.mustRequestLci = params.LCIRequest;
        rttConfig.mustRequestLcr = params.LCRRequest;
        rttConfig.burstDuration = params.burstTimeout;
        rttConfig.preamble = halPreambleFromFrameworkPreamble(params.preamble);
        rttConfig.bw = halBwFromFrameworkBw(params.bandwidth);
        return rttConfig;
    }

    @VisibleForTesting
    static int halRttTypeFromFrameworkRttType(int frameworkRttType) {
        switch (frameworkRttType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(frameworkRttType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static int frameworkRttTypeFromHalRttType(int halType) {
        switch (halType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(halType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static int halPeerFromFrameworkPeer(int frameworkPeer) {
        switch (frameworkPeer) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(frameworkPeer);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static int frameworkPeerFromHalPeer(int halPeer) {
        switch (halPeer) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(halPeer);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static int halChannelWidthFromFrameworkChannelWidth(int frameworkChannelWidth) {
        switch (frameworkChannelWidth) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(frameworkChannelWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static int frameworkChannelWidthFromHalChannelWidth(int halChannelWidth) {
        switch (halChannelWidth) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(halChannelWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static int halPreambleFromFrameworkPreamble(int rttManagerPreamble) {
        BitMask checkoff = new BitMask(rttManagerPreamble);
        int flags = 0;
        if (checkoff.testAndClear(1)) {
            flags = 0 | 1;
        }
        if (checkoff.testAndClear(2)) {
            flags |= 2;
        }
        if (checkoff.testAndClear(4)) {
            flags |= 4;
        }
        if (checkoff.value == 0) {
            return flags;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad ");
        stringBuilder.append(rttManagerPreamble);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @VisibleForTesting
    static int frameworkPreambleFromHalPreamble(int halPreamble) {
        BitMask checkoff = new BitMask(halPreamble);
        int flags = 0;
        if (checkoff.testAndClear(1)) {
            flags = 0 | 1;
        }
        if (checkoff.testAndClear(2)) {
            flags |= 2;
        }
        if (checkoff.testAndClear(4)) {
            flags |= 4;
        }
        if (checkoff.value == 0) {
            return flags;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad ");
        stringBuilder.append(halPreamble);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @VisibleForTesting
    static int halBwFromFrameworkBw(int rttManagerBandwidth) {
        BitMask checkoff = new BitMask(rttManagerBandwidth);
        int flags = 0;
        if (checkoff.testAndClear(1)) {
            flags = 0 | 1;
        }
        if (checkoff.testAndClear(2)) {
            flags |= 2;
        }
        if (checkoff.testAndClear(4)) {
            flags |= 4;
        }
        if (checkoff.testAndClear(8)) {
            flags |= 8;
        }
        if (checkoff.testAndClear(16)) {
            flags |= 16;
        }
        if (checkoff.testAndClear(32)) {
            flags |= 32;
        }
        if (checkoff.value == 0) {
            return flags;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad ");
        stringBuilder.append(rttManagerBandwidth);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @VisibleForTesting
    static int frameworkBwFromHalBw(int rttBw) {
        BitMask checkoff = new BitMask(rttBw);
        int flags = 0;
        if (checkoff.testAndClear(1)) {
            flags = 0 | 1;
        }
        if (checkoff.testAndClear(2)) {
            flags |= 2;
        }
        if (checkoff.testAndClear(4)) {
            flags |= 4;
        }
        if (checkoff.testAndClear(8)) {
            flags |= 8;
        }
        if (checkoff.testAndClear(16)) {
            flags |= 16;
        }
        if (checkoff.testAndClear(32)) {
            flags |= 32;
        }
        if (checkoff.value == 0) {
            return flags;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad ");
        stringBuilder.append(rttBw);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @VisibleForTesting
    static ArrayList<RttConfig> halRttConfigArrayFromFrameworkRttParamsArray(RttParams[] params) {
        ArrayList<RttConfig> configs = new ArrayList(length);
        for (RttConfig config : params) {
            RttConfig config2 = halRttConfigFromFrameworkRttParams(config2);
            if (config2 != null) {
                configs.add(config2);
            }
        }
        return configs;
    }

    public boolean requestRtt(RttParams[] params, RttEventHandler handler) {
        try {
            ArrayList<RttConfig> rttConfigs = halRttConfigArrayFromFrameworkRttParamsArray(params);
            synchronized (sLock) {
                boolean boolResult;
                if (this.mIWifiRttController == null) {
                    boolResult = boolResult(false);
                    return boolResult;
                } else if (this.mRttCmdId != 0) {
                    boolResult = boolResult(false);
                    return boolResult;
                } else {
                    int i = this.mRttCmdIdNext;
                    this.mRttCmdIdNext = i + 1;
                    this.mRttCmdId = i;
                    this.mRttEventHandler = handler;
                    if (this.mRttCmdIdNext <= 0) {
                        this.mRttCmdIdNext = 1;
                    }
                    try {
                        if (ok(this.mIWifiRttController.rangeRequest(this.mRttCmdId, rttConfigs))) {
                            return true;
                        }
                        this.mRttCmdId = 0;
                        return false;
                    } catch (RemoteException e) {
                        handleRemoteException(e);
                        return false;
                    }
                }
            }
        } catch (IllegalArgumentException e2) {
            this.mLog.err("Illegal argument for RTT request").c(e2.toString()).flush();
            return false;
        }
    }

    public boolean cancelRtt(RttParams[] params) {
        ArrayList<RttConfig> rttConfigs = halRttConfigArrayFromFrameworkRttParamsArray(params);
        synchronized (sLock) {
            boolean boolResult;
            if (this.mIWifiRttController == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mRttCmdId == 0) {
                boolResult = boolResult(false);
                return boolResult;
            } else {
                ArrayList<byte[]> addrs = new ArrayList(rttConfigs.size());
                Iterator it = rttConfigs.iterator();
                while (it.hasNext()) {
                    addrs.add(((RttConfig) it.next()).addr);
                }
                try {
                    WifiStatus status = this.mIWifiRttController.rangeCancel(this.mRttCmdId, addrs);
                    this.mRttCmdId = 0;
                    if (ok(status)) {
                        return true;
                    }
                    return false;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            }
        }
    }

    private RttResponder getRttResponder() {
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return null;
            }
            AnonymousClass3AnswerBox answer = new Object() {
                public RttResponder value = null;
            };
            try {
                this.mIWifiRttController.getResponderInfo(new -$$Lambda$WifiVendorHal$xptizMJG5Idss3aicEI09xlMbnE(this, answer));
                RttResponder rttResponder = answer.value;
                return rttResponder;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getRttResponder$5(WifiVendorHal wifiVendorHal, AnonymousClass3AnswerBox answer, WifiStatus status, RttResponder info) {
        if (wifiVendorHal.ok(status)) {
            answer.value = info;
        }
    }

    private ResponderConfig frameworkResponderConfigFromHalRttResponder(RttResponder info) {
        ResponderConfig config = new ResponderConfig();
        config.frequency = info.channel.centerFreq;
        config.centerFreq0 = info.channel.centerFreq0;
        config.centerFreq1 = info.channel.centerFreq1;
        config.channelWidth = frameworkChannelWidthFromHalChannelWidth(info.channel.width);
        config.preamble = frameworkPreambleFromHalPreamble(info.preamble);
        return config;
    }

    public ResponderConfig enableRttResponder(int timeoutSeconds) {
        RttResponder info = getRttResponder();
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return null;
            } else if (this.mRttResponderCmdId != 0) {
                this.mLog.err("responder mode already enabled - this shouldn't happen").flush();
                return null;
            } else {
                ResponderConfig config = null;
                int id = this.mRttCmdIdNext;
                this.mRttCmdIdNext = id + 1;
                if (this.mRttCmdIdNext <= 0) {
                    this.mRttCmdIdNext = 1;
                }
                try {
                    if (ok(this.mIWifiRttController.enableResponder(id, null, timeoutSeconds, info))) {
                        this.mRttResponderCmdId = id;
                        config = frameworkResponderConfigFromHalRttResponder(info);
                        WifiLog wifiLog = this.mVerboseLog;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("enabling rtt ");
                        stringBuilder.append(this.mRttResponderCmdId);
                        wifiLog.i(stringBuilder.toString());
                    }
                    return config;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return null;
                }
            }
        }
    }

    public boolean disableRttResponder() {
        synchronized (sLock) {
            boolean boolResult;
            if (this.mIWifiRttController == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mRttResponderCmdId == 0) {
                boolResult = boolResult(false);
                return boolResult;
            } else {
                try {
                    WifiStatus status = this.mIWifiRttController.disableResponder(this.mRttResponderCmdId);
                    this.mRttResponderCmdId = 0;
                    if (ok(status)) {
                        return true;
                    }
                    return false;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            }
        }
    }

    public boolean setScanningMacOui(String ifaceName, byte[] oui) {
        if (oui == null) {
            return boolResult(false);
        }
        if (oui.length != 3) {
            return boolResult(false);
        }
        synchronized (sLock) {
            try {
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface == null) {
                    boolean boolResult = boolResult(false);
                    return boolResult;
                } else if (ok(iface.setScanningMacOui(oui))) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            } catch (Throwable th) {
            }
        }
    }

    public boolean setMacAddress(String ifaceName, MacAddress mac) {
        byte[] macByteArray = mac.toByteArray();
        synchronized (sLock) {
            try {
                android.hardware.wifi.V1_2.IWifiStaIface ifaceV12 = getWifiStaIfaceForV1_2Mockable(ifaceName);
                if (ifaceV12 == null) {
                    boolean boolResult = boolResult(false);
                    return boolResult;
                } else if (ok(ifaceV12.setMacAddress(macByteArray))) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            } catch (Throwable th) {
            }
        }
    }

    public ApfCapabilities getApfCapabilities(String ifaceName) {
        synchronized (sLock) {
            try {
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface == null) {
                    this.mLog.e("mIWifiStaIface is null, getApfCapabilities(0, 0, 0)");
                    ApfCapabilities apfCapabilities = sNoApfCapabilities;
                    return apfCapabilities;
                }
                AnonymousClass4AnswerBox box = new Object() {
                    public ApfCapabilities value = WifiVendorHal.sNoApfCapabilities;
                };
                iface.getApfPacketFilterCapabilities(new -$$Lambda$WifiVendorHal$nzLDa8bqkjnOhiEpwrQr8oy-Abg(this, box));
                ApfCapabilities apfCapabilities2 = box.value;
                return apfCapabilities2;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return sNoApfCapabilities;
            } catch (Throwable th) {
            }
        }
    }

    public static /* synthetic */ void lambda$getApfCapabilities$6(WifiVendorHal wifiVendorHal, AnonymousClass4AnswerBox box, WifiStatus status, StaApfPacketFilterCapabilities capabilities) {
        if (wifiVendorHal.ok(status)) {
            box.value = new ApfCapabilities(capabilities.version, capabilities.maxLength, OsConstants.ARPHRD_ETHER);
            WifiLog wifiLog = wifiVendorHal.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getApfCapabilities:(version:");
            stringBuilder.append(capabilities.version);
            stringBuilder.append(", maxLength:");
            stringBuilder.append(capabilities.maxLength);
            stringBuilder.append(", ");
            stringBuilder.append(OsConstants.ARPHRD_ETHER);
            stringBuilder.append(")");
            wifiLog.d(stringBuilder.toString());
            return;
        }
        wifiVendorHal.mLog.e("getApfCapabilities failed");
    }

    public boolean installPacketFilter(String ifaceName, byte[] filter) {
        if (filter == null) {
            return boolResult(false);
        }
        ArrayList<Byte> program = NativeUtil.byteArrayToArrayList(filter);
        enter("filter length %").c((long) filter.length).flush();
        synchronized (sLock) {
            try {
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface == null) {
                    boolean boolResult = boolResult(false);
                    return boolResult;
                } else if (ok(iface.installApfPacketFilter(0, program))) {
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            } catch (Throwable th) {
            }
        }
    }

    public byte[] readPacketFilter(String ifaceName) {
        AnonymousClass5AnswerBox answer = new Object() {
            public byte[] data = null;
        };
        enter("").flush();
        synchronized (sLock) {
            try {
                android.hardware.wifi.V1_2.IWifiStaIface ifaceV12 = getWifiStaIfaceForV1_2Mockable(ifaceName);
                byte[] byteArrayResult;
                if (ifaceV12 == null) {
                    byteArrayResult = byteArrayResult(null);
                    return byteArrayResult;
                }
                ifaceV12.readApfPacketFilterData(new -$$Lambda$WifiVendorHal$ZD_VoFx-B8racz66daaqFreli3E(this, answer));
                byteArrayResult = byteArrayResult(answer.data);
                return byteArrayResult;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return byteArrayResult(null);
            } catch (Throwable th) {
            }
        }
    }

    public static /* synthetic */ void lambda$readPacketFilter$7(WifiVendorHal wifiVendorHal, AnonymousClass5AnswerBox answer, WifiStatus status, ArrayList dataByteArray) {
        if (wifiVendorHal.ok(status)) {
            answer.data = NativeUtil.byteArrayFromArrayList(dataByteArray);
        }
    }

    public boolean setCountryCodeHal(String ifaceName, String countryCode) {
        if (countryCode == null) {
            return boolResult(false);
        }
        if (countryCode.length() != 2) {
            return boolResult(false);
        }
        try {
            byte[] code = NativeUtil.stringToByteArray(countryCode);
            synchronized (sLock) {
                try {
                    IWifiApIface iface = getApIface(ifaceName);
                    if (iface == null) {
                        boolean boolResult = boolResult(false);
                        return boolResult;
                    } else if (ok(iface.setCountryCode(code))) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                } catch (Throwable th) {
                }
            }
        } catch (IllegalArgumentException e2) {
            return boolResult(false);
        }
    }

    public boolean setLoggingEventHandler(WifiLoggerEventHandler handler) {
        if (handler == null) {
            return boolResult(false);
        }
        synchronized (sLock) {
            boolean boolResult;
            if (this.mIWifiChip == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mLogEventHandler != null) {
                boolResult = boolResult(false);
                return boolResult;
            } else {
                try {
                    if (ok(this.mIWifiChip.enableDebugErrorAlerts(true))) {
                        this.mLogEventHandler = handler;
                        return true;
                    }
                    return false;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            }
        }
    }

    public boolean resetLogHandler() {
        synchronized (sLock) {
            boolean boolResult;
            if (this.mIWifiChip == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else if (this.mLogEventHandler == null) {
                boolResult = boolResult(false);
                return boolResult;
            } else {
                try {
                    if (!ok(this.mIWifiChip.enableDebugErrorAlerts(false))) {
                        return false;
                    } else if (ok(this.mIWifiChip.stopLoggingToDebugRingBuffer())) {
                        this.mLogEventHandler = null;
                        return true;
                    } else {
                        return false;
                    }
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            }
        }
    }

    public boolean startLoggingRingBuffer(int verboseLevel, int flags, int maxIntervalInSec, int minDataSizeInBytes, String ringName) {
        enter("verboseLevel=%, flags=%, maxIntervalInSec=%, minDataSizeInBytes=%, ringName=%").c((long) verboseLevel).c((long) flags).c((long) maxIntervalInSec).c((long) minDataSizeInBytes).c(ringName).flush();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                boolean ok = ok(this.mIWifiChip.startLoggingToDebugRingBuffer(ringName, verboseLevel, maxIntervalInSec, minDataSizeInBytes));
                return ok;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public int getSupportedLoggerFeatureSet() {
        return -1;
    }

    public String getDriverVersion() {
        String str;
        synchronized (sLock) {
            if (this.mDriverDescription == null) {
                requestChipDebugInfo();
            }
            str = this.mDriverDescription;
        }
        return str;
    }

    public String getFirmwareVersion() {
        String str;
        synchronized (sLock) {
            if (this.mFirmwareDescription == null) {
                requestChipDebugInfo();
            }
            str = this.mFirmwareDescription;
        }
        return str;
    }

    private void requestChipDebugInfo() {
        this.mDriverDescription = null;
        this.mFirmwareDescription = null;
        try {
            if (this.mIWifiChip != null) {
                this.mIWifiChip.requestChipDebugInfo(new -$$Lambda$WifiVendorHal$78Olu6lZcZThVdxrs2nTDEfDswQ(this));
                this.mLog.info("Driver: % Firmware: %").c(this.mDriverDescription).c(this.mFirmwareDescription).flush();
            }
        } catch (RemoteException e) {
            handleRemoteException(e);
        }
    }

    public static /* synthetic */ void lambda$requestChipDebugInfo$8(WifiVendorHal wifiVendorHal, WifiStatus status, ChipDebugInfo chipDebugInfo) {
        if (wifiVendorHal.ok(status)) {
            wifiVendorHal.mDriverDescription = chipDebugInfo.driverDescription;
            wifiVendorHal.mFirmwareDescription = chipDebugInfo.firmwareDescription;
        }
    }

    private static RingBufferStatus ringBufferStatus(WifiDebugRingBufferStatus h) {
        RingBufferStatus ans = new RingBufferStatus();
        ans.name = h.ringName;
        ans.flag = frameworkRingBufferFlagsFromHal(h.flags);
        ans.ringBufferId = h.ringId;
        ans.ringBufferByteSize = h.sizeInBytes;
        ans.verboseLevel = h.verboseLevel;
        return ans;
    }

    private static int frameworkRingBufferFlagsFromHal(int wifiDebugRingBufferFlag) {
        BitMask checkoff = new BitMask(wifiDebugRingBufferFlag);
        int flags = 0;
        if (checkoff.testAndClear(1)) {
            flags = 0 | 1;
        }
        if (checkoff.testAndClear(2)) {
            flags |= 2;
        }
        if (checkoff.testAndClear(4)) {
            flags |= 4;
        }
        if (checkoff.value == 0) {
            return flags;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown WifiDebugRingBufferFlag ");
        stringBuilder.append(checkoff.value);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static RingBufferStatus[] makeRingBufferStatusArray(ArrayList<WifiDebugRingBufferStatus> ringBuffers) {
        RingBufferStatus[] ans = new RingBufferStatus[ringBuffers.size()];
        int i = 0;
        Iterator it = ringBuffers.iterator();
        while (it.hasNext()) {
            int i2 = i + 1;
            ans[i] = ringBufferStatus((WifiDebugRingBufferStatus) it.next());
            i = i2;
        }
        return ans;
    }

    public RingBufferStatus[] getRingBufferStatus() {
        AnonymousClass6AnswerBox ans = new Object() {
            public RingBufferStatus[] value = null;
        };
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.getDebugRingBuffersStatus(new -$$Lambda$WifiVendorHal$dLmE-Gt21lNab7JkIiohEIIEf6Q(this, ans));
                return ans.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getRingBufferStatus$9(WifiVendorHal wifiVendorHal, AnonymousClass6AnswerBox ans, WifiStatus status, ArrayList ringBuffers) {
        if (wifiVendorHal.ok(status)) {
            ans.value = makeRingBufferStatusArray(ringBuffers);
        }
    }

    public boolean getRingBufferData(String ringName) {
        enter("ringName %").c(ringName).flush();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                boolean ok = ok(this.mIWifiChip.forceDumpToDebugRingBuffer(ringName));
                return ok;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public byte[] getFwMemoryDump() {
        AnonymousClass7AnswerBox ans = new Object() {
            public byte[] value;
        };
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.requestFirmwareDebugDump(new -$$Lambda$WifiVendorHal$0nn1d2XVTxIXDSyzfYz5nuiMmaM(this, ans));
                return ans.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getFwMemoryDump$10(WifiVendorHal wifiVendorHal, AnonymousClass7AnswerBox ans, WifiStatus status, ArrayList blob) {
        if (wifiVendorHal.ok(status)) {
            ans.value = NativeUtil.byteArrayFromArrayList(blob);
        }
    }

    public byte[] getDriverStateDump() {
        AnonymousClass8AnswerBox ans = new Object() {
            public byte[] value;
        };
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.requestDriverDebugDump(new -$$Lambda$WifiVendorHal$tzHRLpLug6A0mb6rrMUdhsh-NDU(this, ans));
                return ans.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getDriverStateDump$11(WifiVendorHal wifiVendorHal, AnonymousClass8AnswerBox ans, WifiStatus status, ArrayList blob) {
        if (wifiVendorHal.ok(status)) {
            ans.value = NativeUtil.byteArrayFromArrayList(blob);
        }
    }

    public boolean startPktFateMonitoring(String ifaceName) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                boolean ok = ok(iface.startDebugPacketFateMonitoring());
                return ok;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    private byte halToFrameworkPktFateFrameType(int type) {
        switch (type) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            case 2:
                return (byte) 2;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private byte halToFrameworkRxPktFate(int type) {
        switch (type) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            case 2:
                return (byte) 2;
            case 3:
                return (byte) 3;
            case 4:
                return (byte) 4;
            case 5:
                return (byte) 5;
            case 6:
                return (byte) 6;
            case 7:
                return (byte) 7;
            case 8:
                return (byte) 8;
            case 9:
                return (byte) 9;
            case 10:
                return (byte) 10;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private byte halToFrameworkTxPktFate(int type) {
        switch (type) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            case 2:
                return (byte) 2;
            case 3:
                return (byte) 3;
            case 4:
                return (byte) 4;
            case 5:
                return (byte) 5;
            case 6:
                return (byte) 6;
            case 7:
                return (byte) 7;
            case 8:
                return (byte) 8;
            case 9:
                return (byte) 9;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public boolean getTxPktFates(String ifaceName, TxFateReport[] reportBufs) {
        if (ArrayUtils.isEmpty(reportBufs)) {
            return boolResult(false);
        }
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                MutableBoolean ok = new MutableBoolean(false);
                iface.getDebugTxPacketFates(new -$$Lambda$WifiVendorHal$sRX80xmV169NEPfDVRtnwl0y95Q(this, reportBufs, ok));
                boolean z = ok.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static /* synthetic */ void lambda$getTxPktFates$12(WifiVendorHal wifiVendorHal, TxFateReport[] reportBufs, MutableBoolean ok, WifiStatus status, ArrayList fates) {
        WifiVendorHal wifiVendorHal2 = wifiVendorHal;
        TxFateReport[] txFateReportArr = reportBufs;
        if (wifiVendorHal2.ok(status)) {
            int i = 0;
            Iterator it = fates.iterator();
            while (it.hasNext()) {
                WifiDebugTxPacketFateReport fate = (WifiDebugTxPacketFateReport) it.next();
                if (i >= txFateReportArr.length) {
                    break;
                }
                int i2 = i + 1;
                txFateReportArr[i] = new TxFateReport(wifiVendorHal2.halToFrameworkTxPktFate(fate.fate), fate.frameInfo.driverTimestampUsec, wifiVendorHal2.halToFrameworkPktFateFrameType(fate.frameInfo.frameType), NativeUtil.byteArrayFromArrayList(fate.frameInfo.frameContent));
                i = i2;
            }
            ok.value = true;
        }
    }

    public boolean getRxPktFates(String ifaceName, RxFateReport[] reportBufs) {
        if (ArrayUtils.isEmpty(reportBufs)) {
            return boolResult(false);
        }
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                MutableBoolean ok = new MutableBoolean(false);
                iface.getDebugRxPacketFates(new -$$Lambda$WifiVendorHal$0gGojGcifgvfhGv7aD4Qbmyl79k(this, reportBufs, ok));
                boolean z = ok.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static /* synthetic */ void lambda$getRxPktFates$13(WifiVendorHal wifiVendorHal, RxFateReport[] reportBufs, MutableBoolean ok, WifiStatus status, ArrayList fates) {
        WifiVendorHal wifiVendorHal2 = wifiVendorHal;
        RxFateReport[] rxFateReportArr = reportBufs;
        if (wifiVendorHal2.ok(status)) {
            int i = 0;
            Iterator it = fates.iterator();
            while (it.hasNext()) {
                WifiDebugRxPacketFateReport fate = (WifiDebugRxPacketFateReport) it.next();
                if (i >= rxFateReportArr.length) {
                    break;
                }
                int i2 = i + 1;
                rxFateReportArr[i] = new RxFateReport(wifiVendorHal2.halToFrameworkRxPktFate(fate.fate), fate.frameInfo.driverTimestampUsec, wifiVendorHal2.halToFrameworkPktFateFrameType(fate.frameInfo.frameType), NativeUtil.byteArrayFromArrayList(fate.frameInfo.frameContent));
                i = i2;
            }
            ok.value = true;
        }
    }

    public int startSendingOffloadedPacket(String ifaceName, int slot, byte[] srcMac, byte[] dstMac, byte[] packet, int protocol, int periodInMs) {
        Throwable th;
        int i = slot;
        int i2 = periodInMs;
        enter("slot=% periodInMs=%").c((long) i).c((long) i2).flush();
        ArrayList<Byte> data = NativeUtil.byteArrayToArrayList(packet);
        synchronized (sLock) {
            try {
                IWifiStaIface iface = getStaIface(ifaceName);
                if (iface == null) {
                    return -1;
                }
                try {
                    if (ok(iface.startSendingKeepAlivePackets(i, data, (short) protocol, srcMac, dstMac, i2))) {
                        return 0;
                    }
                    return -1;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return -1;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                int i3 = protocol;
                throw th;
            }
        }
    }

    public int stopSendingOffloadedPacket(String ifaceName, int slot) {
        enter("slot=%").c((long) slot).flush();
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                return -1;
            }
            try {
                if (ok(iface.stopSendingKeepAlivePackets(slot))) {
                    return 0;
                }
                return -1;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    public int startRssiMonitoring(String ifaceName, byte maxRssi, byte minRssi, WifiRssiEventHandler rssiEventHandler) {
        enter("maxRssi=% minRssi=%").c((long) maxRssi).c((long) minRssi).flush();
        if (maxRssi <= minRssi || rssiEventHandler == null) {
            return -1;
        }
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                return -1;
            }
            try {
                iface.stopRssiMonitoring(sRssiMonCmdId);
                if (ok(iface.startRssiMonitoring(sRssiMonCmdId, maxRssi, minRssi))) {
                    this.mWifiRssiEventHandler = rssiEventHandler;
                    return 0;
                }
                return -1;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    public int stopRssiMonitoring(String ifaceName) {
        synchronized (sLock) {
            this.mWifiRssiEventHandler = null;
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                return -1;
            }
            try {
                if (ok(iface.stopRssiMonitoring(sRssiMonCmdId))) {
                    return 0;
                }
                return -1;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    private static int[] intsFromArrayList(ArrayList<Integer> a) {
        if (a == null) {
            return null;
        }
        int[] b = new int[a.size()];
        int i = 0;
        Iterator it = a.iterator();
        while (it.hasNext()) {
            int i2 = i + 1;
            b[i] = ((Integer) it.next()).intValue();
            i = i2;
        }
        return b;
    }

    private static WifiWakeReasonAndCounts halToFrameworkWakeReasons(WifiDebugHostWakeReasonStats h) {
        if (h == null) {
            return null;
        }
        WifiWakeReasonAndCounts ans = new WifiWakeReasonAndCounts();
        ans.totalCmdEventWake = h.totalCmdEventWakeCnt;
        ans.totalDriverFwLocalWake = h.totalDriverFwLocalWakeCnt;
        ans.totalRxDataWake = h.totalRxPacketWakeCnt;
        ans.rxUnicast = h.rxPktWakeDetails.rxUnicastCnt;
        ans.rxMulticast = h.rxPktWakeDetails.rxMulticastCnt;
        ans.rxBroadcast = h.rxPktWakeDetails.rxBroadcastCnt;
        ans.icmp = h.rxIcmpPkWakeDetails.icmpPkt;
        ans.icmp6 = h.rxIcmpPkWakeDetails.icmp6Pkt;
        ans.icmp6Ra = h.rxIcmpPkWakeDetails.icmp6Ra;
        ans.icmp6Na = h.rxIcmpPkWakeDetails.icmp6Na;
        ans.icmp6Ns = h.rxIcmpPkWakeDetails.icmp6Ns;
        ans.ipv4RxMulticast = h.rxMulticastPkWakeDetails.ipv4RxMulticastAddrCnt;
        ans.ipv6Multicast = h.rxMulticastPkWakeDetails.ipv6RxMulticastAddrCnt;
        ans.otherRxMulticast = h.rxMulticastPkWakeDetails.otherRxMulticastAddrCnt;
        ans.cmdEventWakeCntArray = intsFromArrayList(h.cmdEventWakeCntPerType);
        ans.driverFWLocalWakeCntArray = intsFromArrayList(h.driverFwLocalWakeCntPerType);
        return ans;
    }

    public WifiWakeReasonAndCounts getWlanWakeReasonCount() {
        AnonymousClass9AnswerBox ans = new Object() {
            public WifiDebugHostWakeReasonStats value = null;
        };
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.getDebugHostWakeReasonStats(new -$$Lambda$WifiVendorHal$9OKuBaEsJa-3ksFDFIHk8H-fn6Q(this, ans));
                WifiWakeReasonAndCounts halToFrameworkWakeReasons = halToFrameworkWakeReasons(ans.value);
                return halToFrameworkWakeReasons;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static /* synthetic */ void lambda$getWlanWakeReasonCount$14(WifiVendorHal wifiVendorHal, AnonymousClass9AnswerBox ans, WifiStatus status, WifiDebugHostWakeReasonStats stats) {
        if (wifiVendorHal.ok(status)) {
            ans.value = stats;
        }
    }

    public boolean configureNeighborDiscoveryOffload(String ifaceName, boolean enabled) {
        enter("enabled=%").c(enabled).flush();
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                if (ok(iface.enableNdOffload(enabled))) {
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public boolean getRoamingCapabilities(String ifaceName, RoamingCapabilities capabilities) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                MutableBoolean ok = new MutableBoolean(false);
                iface.getRoamingCapabilities(new -$$Lambda$WifiVendorHal$dFBsbco7FdXhMfSsRSt5MvRa-No(this, capabilities, ok));
                boolean z = ok.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static /* synthetic */ void lambda$getRoamingCapabilities$15(WifiVendorHal wifiVendorHal, RoamingCapabilities out, MutableBoolean ok, WifiStatus status, StaRoamingCapabilities cap) {
        if (wifiVendorHal.ok(status)) {
            out.maxBlacklistSize = cap.maxBlacklistSize;
            out.maxWhitelistSize = cap.maxWhitelistSize;
            ok.value = true;
        }
    }

    public int enableFirmwareRoaming(String ifaceName, int state) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                return 6;
            }
            byte val;
            switch (state) {
                case 0:
                    val = (byte) 0;
                    break;
                case 1:
                    val = (byte) 1;
                    break;
                default:
                    try {
                        this.mLog.err("enableFirmwareRoaming invalid argument %").c((long) state).flush();
                        return 7;
                    } catch (RemoteException e) {
                        handleRemoteException(e);
                        return 9;
                    }
            }
            WifiStatus status = iface.setRoamingState(val);
            WifiLog wifiLog = this.mVerboseLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setRoamingState returned ");
            stringBuilder.append(status.code);
            wifiLog.d(stringBuilder.toString());
            int i = status.code;
            return i;
        }
    }

    public boolean configureRoaming(String ifaceName, RoamingConfig config) {
        synchronized (sLock) {
            IWifiStaIface iface = getStaIface(ifaceName);
            if (iface == null) {
                boolean boolResult = boolResult(false);
                return boolResult;
            }
            try {
                Iterator it;
                StaRoamingConfig roamingConfig = new StaRoamingConfig();
                if (config.blacklistBssids != null) {
                    it = config.blacklistBssids.iterator();
                    while (it.hasNext()) {
                        roamingConfig.bssidBlacklist.add(NativeUtil.macAddressToByteArray((String) it.next()));
                    }
                }
                if (config.whitelistSsids != null) {
                    it = config.whitelistSsids.iterator();
                    while (it.hasNext()) {
                        String unquotedSsidStr = WifiInfo.removeDoubleQuotes((String) it.next());
                        int len = unquotedSsidStr.length();
                        if (len > 32) {
                            this.mLog.err("configureRoaming: skip invalid SSID %").r(unquotedSsidStr).flush();
                        } else {
                            byte[] ssid = new byte[len];
                            for (int i = 0; i < len; i++) {
                                ssid[i] = (byte) unquotedSsidStr.charAt(i);
                            }
                            roamingConfig.ssidWhitelist.add(ssid);
                        }
                    }
                }
                if (ok(iface.configureRoaming(roamingConfig))) {
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            } catch (IllegalArgumentException e2) {
                this.mLog.err("Illegal argument for roaming configuration").c(e2.toString()).flush();
                return false;
            }
        }
    }

    protected android.hardware.wifi.V1_1.IWifiChip getWifiChipForV1_1Mockable() {
        if (this.mIWifiChip == null) {
            return null;
        }
        return android.hardware.wifi.V1_1.IWifiChip.castFrom(this.mIWifiChip);
    }

    protected android.hardware.wifi.V1_2.IWifiChip getWifiChipForV1_2Mockable() {
        if (this.mIWifiChip == null) {
            return null;
        }
        return android.hardware.wifi.V1_2.IWifiChip.castFrom(this.mIWifiChip);
    }

    protected android.hardware.wifi.V1_2.IWifiStaIface getWifiStaIfaceForV1_2Mockable(String ifaceName) {
        IWifiStaIface iface = getStaIface(ifaceName);
        if (iface == null) {
            return null;
        }
        return android.hardware.wifi.V1_2.IWifiStaIface.castFrom(iface);
    }

    private int frameworkToHalTxPowerScenario(int scenario) {
        if (scenario == 1) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad scenario: ");
        stringBuilder.append(scenario);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean selectTxPowerScenario(int scenario) {
        synchronized (sLock) {
            try {
                android.hardware.wifi.V1_1.IWifiChip iWifiChipV11 = getWifiChipForV1_1Mockable();
                if (iWifiChipV11 == null) {
                    boolean boolResult = boolResult(false);
                    return boolResult;
                }
                WifiStatus status;
                if (scenario != 0) {
                    try {
                        status = iWifiChipV11.selectTxPowerScenario(frameworkToHalTxPowerScenario(scenario));
                    } catch (IllegalArgumentException e) {
                        this.mLog.err("Illegal argument for select tx power scenario").c(e.toString()).flush();
                        return false;
                    }
                }
                status = iWifiChipV11.resetTxPowerScenario();
                if (ok(status)) {
                    return true;
                }
                return false;
            } catch (RemoteException e2) {
                handleRemoteException(e2);
                return false;
            } catch (Throwable th) {
            }
        }
    }

    private static byte[] hidlIeArrayToFrameworkIeBlob(ArrayList<android.hardware.wifi.V1_0.WifiInformationElement> ies) {
        if (ies == null || ies.isEmpty()) {
            return new byte[0];
        }
        ArrayList<Byte> ieBlob = new ArrayList();
        Iterator it = ies.iterator();
        while (it.hasNext()) {
            android.hardware.wifi.V1_0.WifiInformationElement ie = (android.hardware.wifi.V1_0.WifiInformationElement) it.next();
            ieBlob.add(Byte.valueOf(ie.id));
            ieBlob.addAll(ie.data);
        }
        return NativeUtil.byteArrayFromArrayList(ieBlob);
    }

    private static ScanResult hidlToFrameworkScanResult(StaScanResult scanResult) {
        if (scanResult == null) {
            return null;
        }
        ScanResult frameworkScanResult = new ScanResult();
        frameworkScanResult.SSID = NativeUtil.encodeSsid(scanResult.ssid);
        frameworkScanResult.wifiSsid = WifiSsid.createFromByteArray(NativeUtil.byteArrayFromArrayList(scanResult.ssid));
        frameworkScanResult.BSSID = NativeUtil.macAddressFromByteArray(scanResult.bssid);
        frameworkScanResult.level = scanResult.rssi;
        frameworkScanResult.frequency = scanResult.frequency;
        frameworkScanResult.timestamp = scanResult.timeStampInUs;
        frameworkScanResult.capabilities = generateScanResultCapabilities(frameworkScanResult.informationElements, scanResult.capability);
        return frameworkScanResult;
    }

    private static String generateScanResultCapabilities(InformationElement[] ies, short capabilityInt) {
        BitSet hidlCapability = new BitSet(16);
        for (int i = 0; i < 16; i++) {
            if (((1 << i) & capabilityInt) != 0) {
                hidlCapability.set(i);
            }
        }
        Capabilities capabilities = new Capabilities();
        capabilities.from(ies, hidlCapability);
        return capabilities.generateCapabilitiesString();
    }

    private static ScanResult[] hidlToFrameworkScanResults(ArrayList<StaScanResult> scanResults) {
        if (scanResults == null || scanResults.isEmpty()) {
            return new ScanResult[0];
        }
        ScanResult[] frameworkScanResults = new ScanResult[scanResults.size()];
        int i = 0;
        Iterator it = scanResults.iterator();
        while (it.hasNext()) {
            int i2 = i + 1;
            frameworkScanResults[i] = hidlToFrameworkScanResult((StaScanResult) it.next());
            i = i2;
        }
        return frameworkScanResults;
    }

    private static int hidlToFrameworkScanDataFlags(int flag) {
        if (flag == 1) {
            return 1;
        }
        return 0;
    }

    private static ScanData[] hidlToFrameworkScanDatas(int cmdId, ArrayList<StaScanData> scanDatas) {
        if (scanDatas == null || scanDatas.isEmpty()) {
            return new ScanData[0];
        }
        ScanData[] frameworkScanDatas = new ScanData[scanDatas.size()];
        int i = 0;
        Iterator it = scanDatas.iterator();
        while (it.hasNext()) {
            StaScanData scanData = (StaScanData) it.next();
            int flags = hidlToFrameworkScanDataFlags(scanData.flags);
            int i2 = i + 1;
            int i3 = cmdId;
            int i4 = flags;
            frameworkScanDatas[i] = new ScanData(i3, i4, scanData.bucketsScanned, false, hidlToFrameworkScanResults(scanData.results));
            i = i2;
        }
        return frameworkScanDatas;
    }
}
