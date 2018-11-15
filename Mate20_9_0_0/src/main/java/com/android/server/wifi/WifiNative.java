package com.android.server.wifi;

import android.net.InterfaceConfiguration;
import android.net.MacAddress;
import android.net.TrafficStats;
import android.net.apf.ApfCapabilities;
import android.net.wifi.RttManager.ResponderConfig;
import android.net.wifi.RttManager.RttCapabilities;
import android.net.wifi.RttManager.RttParams;
import android.net.wifi.RttManager.RttResult;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiWakeReasonAndCounts;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.HexDump;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener;
import com.android.server.wifi.util.FrameParser;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiCommonUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

public class WifiNative {
    public static final int BLUETOOTH_COEXISTENCE_MODE_DISABLED = 1;
    public static final int BLUETOOTH_COEXISTENCE_MODE_ENABLED = 0;
    public static final int BLUETOOTH_COEXISTENCE_MODE_SENSE = 2;
    private static final int CMD_GET_FEATURE_CAPAB = 101;
    private static final int CONNECT_TO_HOSTAPD_RETRY_INTERVAL_MS = 100;
    private static final int CONNECT_TO_HOSTAPD_RETRY_TIMES = 50;
    private static final int CONNECT_TO_SUPPLICANT_RETRY_INTERVAL_MS = 50;
    private static final int CONNECT_TO_SUPPLICANT_RETRY_TIMES = 70;
    public static final int DISABLE_FIRMWARE_ROAMING = 0;
    public static final int EAP_SIM_VENDOR_SPECIFIC_CERT_EXPIRED = 16385;
    public static final int ENABLE_FIRMWARE_ROAMING = 1;
    public static final int PRIV_FEATURE_DOT11_K = 1;
    public static final int PRIV_FEATURE_DOT11_R = 4;
    public static final int PRIV_FEATURE_DOT11_V = 2;
    public static final int PRIV_FEATURE_UNKNOWN = -1;
    public static final int RX_FILTER_TYPE_V4_MULTICAST = 0;
    public static final int RX_FILTER_TYPE_V6_MULTICAST = 1;
    public static final int SCAN_TYPE_HIGH_ACCURACY = 2;
    public static final int SCAN_TYPE_LOW_LATENCY = 0;
    public static final int SCAN_TYPE_LOW_POWER = 1;
    public static final String SIM_AUTH_RESP_TYPE_GSM_AUTH = "GSM-AUTH";
    public static final String SIM_AUTH_RESP_TYPE_UMTS_AUTH = "UMTS-AUTH";
    public static final String SIM_AUTH_RESP_TYPE_UMTS_AUTS = "UMTS-AUTS";
    private static final String TAG = "WifiNative";
    public static final int TX_POWER_SCENARIO_NORMAL = 0;
    public static final int TX_POWER_SCENARIO_VOICE_CALL = 1;
    private static final String VOWIFI_DETECT_SET_PREFIX = "VOWIFI_DETECT SET ";
    public static final int WIFI_SCAN_FAILED = 3;
    public static final int WIFI_SCAN_RESULTS_AVAILABLE = 0;
    public static final int WIFI_SCAN_THRESHOLD_NUM_SCANS = 1;
    public static final int WIFI_SCAN_THRESHOLD_PERCENT = 2;
    private static final int WPA_SUPP_TYPE_CONFIG = 0;
    private static final int WPA_SUPP_TYPE_RAW_PSK = 1;
    private final HostapdHal mHostapdHal;
    private final IfaceManager mIfaceMgr = new IfaceManager();
    private Object mLock = new Object();
    private final INetworkManagementService mNwManagementService;
    private int mPrivFeatureCapab = -1;
    private final PropertyService mPropertyService;
    private HashSet<StatusListener> mStatusListeners = new HashSet();
    private final SupplicantStaIfaceHal mSupplicantStaIfaceHal;
    private final String mTAG = TAG;
    private boolean mVerboseLoggingEnabled = false;
    private final WifiMetrics mWifiMetrics;
    private final WifiMonitor mWifiMonitor;
    private final WifiVendorHal mWifiVendorHal;
    private final WificondControl mWificondControl;
    private Set<String> moldSsidList = new HashSet();

    public static class BucketSettings {
        public int band;
        public int bucket;
        public ChannelSettings[] channels;
        public int max_period_ms;
        public int num_channels;
        public int period_ms;
        public int report_events;
        public int step_count;
    }

    public static class ChannelSettings {
        public int dwell_time_ms;
        public int frequency;
        public boolean passive;
    }

    @Immutable
    static abstract class FateReport {
        static final int MAX_DRIVER_TIMESTAMP_MSEC = 4294967;
        static final int USEC_PER_MSEC = 1000;
        static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
        final long mDriverTimestampUSec;
        final long mEstimatedWallclockMSec = convertDriverTimestampUSecToWallclockMSec(this.mDriverTimestampUSec);
        final byte mFate;
        final byte[] mFrameBytes;
        final byte mFrameType;

        protected abstract String directionToString();

        protected abstract String fateToString();

        FateReport(byte fate, long driverTimestampUSec, byte frameType, byte[] frameBytes) {
            this.mFate = fate;
            this.mDriverTimestampUSec = driverTimestampUSec;
            this.mFrameType = frameType;
            this.mFrameBytes = frameBytes;
        }

        public String toTableRowString() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            FrameParser parser = new FrameParser(this.mFrameType, this.mFrameBytes);
            dateFormatter.setTimeZone(TimeZone.getDefault());
            pw.format("%-15s  %12s  %-9s  %-32s  %-12s  %-23s  %s\n", new Object[]{Long.valueOf(this.mDriverTimestampUSec), dateFormatter.format(new Date(this.mEstimatedWallclockMSec)), directionToString(), fateToString(), parser.mMostSpecificProtocolString, parser.mTypeString, parser.mResultString});
            return sw.toString();
        }

        public String toVerboseStringWithPiiAllowed() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            FrameParser parser = new FrameParser(this.mFrameType, this.mFrameBytes);
            pw.format("Frame direction: %s\n", new Object[]{directionToString()});
            pw.format("Frame timestamp: %d\n", new Object[]{Long.valueOf(this.mDriverTimestampUSec)});
            pw.format("Frame fate: %s\n", new Object[]{fateToString()});
            pw.format("Frame type: %s\n", new Object[]{frameTypeToString(this.mFrameType)});
            pw.format("Frame protocol: %s\n", new Object[]{parser.mMostSpecificProtocolString});
            pw.format("Frame protocol type: %s\n", new Object[]{parser.mTypeString});
            pw.format("Frame length: %d\n", new Object[]{Integer.valueOf(this.mFrameBytes.length)});
            pw.append("Frame bytes");
            pw.append(HexDump.dumpHexString(this.mFrameBytes));
            pw.append("\n");
            return sw.toString();
        }

        public static String getTableHeader() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.format("\n%-15s  %-12s  %-9s  %-32s  %-12s  %-23s  %s\n", new Object[]{"Time usec", "Walltime", "Direction", "Fate", "Protocol", "Type", "Result"});
            pw.format("%-15s  %-12s  %-9s  %-32s  %-12s  %-23s  %s\n", new Object[]{"---------", "--------", "---------", "----", "--------", "----", "------"});
            return sw.toString();
        }

        private static String frameTypeToString(byte frameType) {
            switch (frameType) {
                case (byte) 0:
                    return "unknown";
                case (byte) 1:
                    return "data";
                case (byte) 2:
                    return "802.11 management";
                default:
                    return Byte.toString(frameType);
            }
        }

        private static long convertDriverTimestampUSecToWallclockMSec(long driverTimestampUSec) {
            long wallclockMillisNow = System.currentTimeMillis();
            long driverTimestampMillis = driverTimestampUSec / 1000;
            long boottimeTimestampMillis = SystemClock.elapsedRealtime() % 4294967;
            if (boottimeTimestampMillis < driverTimestampMillis) {
                boottimeTimestampMillis += 4294967;
            }
            return wallclockMillisNow - (boottimeTimestampMillis - driverTimestampMillis);
        }
    }

    public static class HiddenNetwork {
        public String ssid;

        public boolean equals(Object otherObj) {
            if (this == otherObj) {
                return true;
            }
            if (otherObj == null || getClass() != otherObj.getClass()) {
                return false;
            }
            return Objects.equals(this.ssid, ((HiddenNetwork) otherObj).ssid);
        }

        public int hashCode() {
            return this.ssid == null ? 0 : this.ssid.hashCode();
        }
    }

    public interface HostapdDeathEventHandler {
        void onDeath();
    }

    private static class Iface {
        public static final int IFACE_TYPE_AP = 0;
        public static final int IFACE_TYPE_STA = 1;
        public InterfaceCallback externalListener;
        public final int id;
        public boolean isUp;
        public String name;
        public NetworkObserverInternal networkObserver;
        public final int type;

        @Retention(RetentionPolicy.SOURCE)
        public @interface IfaceType {
        }

        Iface(int id, int type) {
            this.id = id;
            this.type = type;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Iface:");
            sb.append("{");
            sb.append("Name=");
            sb.append(this.name);
            sb.append(",");
            sb.append("Id=");
            sb.append(this.id);
            sb.append(",");
            sb.append("Type=");
            sb.append(this.type == 1 ? "STA" : "AP");
            sb.append("}");
            return sb.toString();
        }
    }

    private static class IfaceManager {
        private Object mIfaceLock;
        private HashMap<Integer, Iface> mIfaces;
        private int mNextId;

        private IfaceManager() {
            this.mIfaces = new HashMap();
            this.mIfaceLock = new Object();
        }

        private Iface allocateIface(int type) {
            Iface iface;
            synchronized (this.mIfaceLock) {
                iface = new Iface(this.mNextId, type);
                this.mIfaces.put(Integer.valueOf(this.mNextId), iface);
                this.mNextId++;
            }
            return iface;
        }

        private Iface removeIface(int id) {
            Iface iface;
            synchronized (this.mIfaceLock) {
                iface = (Iface) this.mIfaces.remove(Integer.valueOf(id));
            }
            return iface;
        }

        private Iface getIface(int id) {
            return (Iface) this.mIfaces.get(Integer.valueOf(id));
        }

        private Iface getIface(String ifaceName) {
            synchronized (this.mIfaceLock) {
                for (Iface iface : this.mIfaces.values()) {
                    if (TextUtils.equals(iface.name, ifaceName)) {
                        return iface;
                    }
                }
                return null;
            }
        }

        private Iterator<Integer> getIfaceIdIter() {
            return this.mIfaces.keySet().iterator();
        }

        private boolean hasAnyIface() {
            return this.mIfaces.isEmpty() ^ 1;
        }

        private boolean hasAnyIfaceOfType(int type) {
            synchronized (this.mIfaceLock) {
                for (Iface iface : this.mIfaces.values()) {
                    if (iface.type == type) {
                        return true;
                    }
                }
                return false;
            }
        }

        private Iface findAnyIfaceOfType(int type) {
            synchronized (this.mIfaceLock) {
                for (Iface iface : this.mIfaces.values()) {
                    if (iface.type == type) {
                        return iface;
                    }
                }
                return null;
            }
        }

        private boolean hasAnyStaIface() {
            return hasAnyIfaceOfType(1);
        }

        private boolean hasAnyApIface() {
            return hasAnyIfaceOfType(0);
        }

        private String findAnyStaIfaceName() {
            Iface iface = findAnyIfaceOfType(1);
            if (iface == null) {
                return null;
            }
            return iface.name;
        }

        private String findAnyApIfaceName() {
            Iface iface = findAnyIfaceOfType(null);
            if (iface == null) {
                return null;
            }
            return iface.name;
        }

        public Iface removeExistingIface(int newIfaceId) {
            Iface removedIface;
            synchronized (this.mIfaceLock) {
                removedIface = null;
                if (this.mIfaces.size() > 2) {
                    Log.wtf(WifiNative.TAG, "More than 1 existing interface found");
                }
                Iterator<Entry<Integer, Iface>> iter = this.mIfaces.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<Integer, Iface> entry = (Entry) iter.next();
                    if (((Integer) entry.getKey()).intValue() != newIfaceId) {
                        removedIface = (Iface) entry.getValue();
                        iter.remove();
                    }
                }
            }
            return removedIface;
        }
    }

    public interface InterfaceCallback {
        void onDestroyed(String str);

        void onDown(String str);

        void onUp(String str);
    }

    private class NetworkObserverInternal extends BaseNetworkObserver {
        private final int mInterfaceId;

        NetworkObserverInternal(int id) {
            this.mInterfaceId = id;
        }

        /* JADX WARNING: Missing block: B:9:0x0036, code:
            return;
     */
        /* JADX WARNING: Missing block: B:20:0x0072, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void interfaceLinkStateChanged(String ifaceName, boolean unusedIsLinkUp) {
            synchronized (WifiNative.this.mLock) {
                Iface ifaceWithId = WifiNative.this.mIfaceMgr.getIface(this.mInterfaceId);
                if (ifaceWithId != null) {
                    Iface ifaceWithName = WifiNative.this.mIfaceMgr.getIface(ifaceName);
                    if (ifaceWithName != null && ifaceWithName == ifaceWithId) {
                        WifiNative.this.onInterfaceStateChanged(ifaceWithName, WifiNative.this.isInterfaceUp(ifaceName));
                    } else if (WifiNative.this.mVerboseLoggingEnabled) {
                        String str = WifiNative.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Received iface link up/down notification on an invalid iface=");
                        stringBuilder.append(ifaceName);
                        Log.v(str, stringBuilder.toString());
                    }
                } else if (WifiNative.this.mVerboseLoggingEnabled) {
                    String str2 = WifiNative.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Received iface link up/down notification on an invalid iface=");
                    stringBuilder2.append(this.mInterfaceId);
                    Log.v(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public interface PnoEventHandler {
        void onPnoNetworkFound(ScanResult[] scanResultArr);

        void onPnoScanFailed();
    }

    public static class PnoNetwork {
        public byte auth_bit_field;
        public byte flags;
        public String ssid;

        public boolean equals(Object otherObj) {
            boolean z = true;
            if (this == otherObj) {
                return true;
            }
            if (otherObj == null || getClass() != otherObj.getClass()) {
                return false;
            }
            PnoNetwork other = (PnoNetwork) otherObj;
            if (!(Objects.equals(this.ssid, other.ssid) && this.flags == other.flags && this.auth_bit_field == other.auth_bit_field)) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (this.ssid == null ? 0 : this.ssid.hashCode()) ^ ((this.flags * 31) + (this.auth_bit_field << 8));
        }
    }

    public static class PnoSettings {
        public int band5GHzBonus;
        public int currentConnectionBonus;
        public int initialScoreMax;
        public boolean isConnected;
        public int min24GHzRssi;
        public int min5GHzRssi;
        public PnoNetwork[] networkList;
        public int periodInMs;
        public int sameNetworkBonus;
        public int secureBonus;
    }

    public static class RingBufferStatus {
        public static final int HAS_ASCII_ENTRIES = 2;
        public static final int HAS_BINARY_ENTRIES = 1;
        public static final int HAS_PER_PACKET_ENTRIES = 4;
        int flag;
        String name;
        int readBytes;
        int ringBufferByteSize;
        int ringBufferId;
        int verboseLevel;
        int writtenBytes;
        int writtenRecords;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("name: ");
            stringBuilder.append(this.name);
            stringBuilder.append(" flag: ");
            stringBuilder.append(this.flag);
            stringBuilder.append(" ringBufferId: ");
            stringBuilder.append(this.ringBufferId);
            stringBuilder.append(" ringBufferByteSize: ");
            stringBuilder.append(this.ringBufferByteSize);
            stringBuilder.append(" verboseLevel: ");
            stringBuilder.append(this.verboseLevel);
            stringBuilder.append(" writtenBytes: ");
            stringBuilder.append(this.writtenBytes);
            stringBuilder.append(" readBytes: ");
            stringBuilder.append(this.readBytes);
            stringBuilder.append(" writtenRecords: ");
            stringBuilder.append(this.writtenRecords);
            return stringBuilder.toString();
        }
    }

    public static class RoamingCapabilities {
        public int maxBlacklistSize;
        public int maxWhitelistSize;
    }

    public static class RoamingConfig {
        public ArrayList<String> blacklistBssids;
        public ArrayList<String> whitelistSsids;
    }

    public interface RttEventHandler {
        void onRttResults(RttResult[] rttResultArr);
    }

    public static class ScanCapabilities {
        public int max_ap_cache_per_scan;
        public int max_rssi_sample_size;
        public int max_scan_buckets;
        public int max_scan_cache_size;
        public int max_scan_reporting_threshold;
    }

    public interface ScanEventHandler {
        void onFullScanResult(ScanResult scanResult, int i);

        void onScanPaused(ScanData[] scanDataArr);

        void onScanRestarted();

        void onScanStatus(int i);
    }

    public static class ScanSettings {
        public int base_period_ms;
        public BucketSettings[] buckets;
        public String handlerId;
        public HiddenNetwork[] hiddenNetworks;
        public boolean isHiddenSingleScan;
        public int max_ap_per_scan;
        public int num_buckets;
        public int report_threshold_num_scans;
        public int report_threshold_percent;
        public int scanType;

        public String toString() {
            return this.handlerId;
        }
    }

    public static class SignalPollResult {
        public int associationFrequency;
        public int currentChload;
        public int currentNoise;
        public int currentRssi;
        public int currentSnr;
        public int txBitrate;
    }

    public interface SoftApListener {
        void OnApLinkedStaJoin(String str);

        void OnApLinkedStaLeave(String str);

        void onNumAssociatedStationsChanged(int i);

        void onSoftApChannelSwitched(int i, int i2);
    }

    public interface StatusListener {
        void onStatusChanged(boolean z);
    }

    public interface SupplicantDeathEventHandler {
        void onDeath();
    }

    public static class TxPacketCounters {
        public int txFailed;
        public int txSucceeded;
    }

    public interface VendorHalDeathEventHandler {
        void onDeath();
    }

    public interface VendorHalRadioModeChangeEventHandler {
        void onDbs();

        void onMcc(int i);

        void onSbs(int i);

        void onScc(int i);
    }

    public interface WifiLoggerEventHandler {
        void onRingBufferData(RingBufferStatus ringBufferStatus, byte[] bArr);

        void onWifiAlert(int i, byte[] bArr);
    }

    public interface WifiRssiEventHandler {
        void onRssiThresholdBreached(byte b);
    }

    public interface WificondDeathEventHandler {
        void onDeath();
    }

    private class HostapdDeathHandlerInternal implements HostapdDeathEventHandler {
        private HostapdDeathHandlerInternal() {
        }

        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "hostapd died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumHostapdCrashes();
            }
        }
    }

    private class InterfaceDestoyedListenerInternal implements InterfaceDestroyedListener {
        private final int mInterfaceId;

        InterfaceDestoyedListenerInternal(int ifaceId) {
            this.mInterfaceId = ifaceId;
        }

        /* JADX WARNING: Missing block: B:9:0x0034, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onDestroyed(String ifaceName) {
            synchronized (WifiNative.this.mLock) {
                Iface iface = WifiNative.this.mIfaceMgr.removeIface(this.mInterfaceId);
                String str;
                StringBuilder stringBuilder;
                if (iface != null) {
                    WifiNative.this.onInterfaceDestroyed(iface);
                    str = WifiNative.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Successfully torn down ");
                    stringBuilder.append(iface);
                    Log.i(str, stringBuilder.toString());
                } else if (WifiNative.this.mVerboseLoggingEnabled) {
                    str = WifiNative.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Received iface destroyed notification on an invalid iface=");
                    stringBuilder.append(ifaceName);
                    Log.v(str, stringBuilder.toString());
                }
            }
        }
    }

    @Immutable
    public static final class RxFateReport extends FateReport {
        RxFateReport(byte fate, long driverTimestampUSec, byte frameType, byte[] frameBytes) {
            super(fate, driverTimestampUSec, frameType, frameBytes);
        }

        protected String directionToString() {
            return "RX";
        }

        protected String fateToString() {
            switch (this.mFate) {
                case (byte) 0:
                    return "success";
                case (byte) 1:
                    return "firmware queued";
                case (byte) 2:
                    return "firmware dropped (filter)";
                case (byte) 3:
                    return "firmware dropped (invalid frame)";
                case (byte) 4:
                    return "firmware dropped (no bufs)";
                case (byte) 5:
                    return "firmware dropped (other)";
                case (byte) 6:
                    return "driver queued";
                case (byte) 7:
                    return "driver dropped (filter)";
                case (byte) 8:
                    return "driver dropped (invalid frame)";
                case (byte) 9:
                    return "driver dropped (no bufs)";
                case (byte) 10:
                    return "driver dropped (other)";
                default:
                    return Byte.toString(this.mFate);
            }
        }
    }

    private class SupplicantDeathHandlerInternal implements SupplicantDeathEventHandler {
        private SupplicantDeathHandlerInternal() {
        }

        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "wpa_supplicant died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumSupplicantCrashes();
            }
        }
    }

    @Immutable
    public static final class TxFateReport extends FateReport {
        TxFateReport(byte fate, long driverTimestampUSec, byte frameType, byte[] frameBytes) {
            super(fate, driverTimestampUSec, frameType, frameBytes);
        }

        protected String directionToString() {
            return "TX";
        }

        protected String fateToString() {
            switch (this.mFate) {
                case (byte) 0:
                    return "acked";
                case (byte) 1:
                    return "sent";
                case (byte) 2:
                    return "firmware queued";
                case (byte) 3:
                    return "firmware dropped (invalid frame)";
                case (byte) 4:
                    return "firmware dropped (no bufs)";
                case (byte) 5:
                    return "firmware dropped (other)";
                case (byte) 6:
                    return "driver queued";
                case (byte) 7:
                    return "driver dropped (invalid frame)";
                case (byte) 8:
                    return "driver dropped (no bufs)";
                case (byte) 9:
                    return "driver dropped (other)";
                default:
                    return Byte.toString(this.mFate);
            }
        }
    }

    private class VendorHalDeathHandlerInternal implements VendorHalDeathEventHandler {
        private VendorHalDeathHandlerInternal() {
        }

        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Vendor HAL died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumHalCrashes();
            }
        }
    }

    private class VendorHalRadioModeChangeHandlerInternal implements VendorHalRadioModeChangeEventHandler {
        private VendorHalRadioModeChangeHandlerInternal() {
        }

        public void onMcc(int band) {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in MCC mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToMcc();
            }
        }

        public void onScc(int band) {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in SCC mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToScc();
            }
        }

        public void onSbs(int band) {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in SBS mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToSbs();
            }
        }

        public void onDbs() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in DBS mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToDbs();
            }
        }
    }

    private class WificondDeathHandlerInternal implements WificondDeathEventHandler {
        private WificondDeathHandlerInternal() {
        }

        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "wificond died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumWificondCrashes();
            }
        }
    }

    private native int deauthLastRoamingBssidHwNative(String str, String str2, String str3);

    private native int disassociateSoftapStaHwNative(String str, String str2);

    private native int gameKOGAdjustSpeedNative(int i, int i2);

    private native String getSoftapClientsHwNative(String str);

    private native int getWifiAntNative(String str, int i);

    private native int hwDelArpItemNative(String str, String str2);

    private native int hwSetArpItemNative(String str, String str2, String str3);

    private native int hwSetPwrBoostNative(int i);

    private static native byte[] readKernelLogNative();

    private native String readSoftapDhcpLeaseFileHwNative(String str);

    private static native int registerNatives();

    private native int sendCmdToDriverNative(String str, int i, byte[] bArr, int i2);

    private native int setCmdToWifiChipNative(String str, int i, int i2, int i3, int i4);

    private native int setSoftapHwNative(String str, String str2, String str3);

    private native int setSoftapMacFltrHwNative(String str, String str2);

    private native int setWifiAntNative(String str, int i, int i2);

    private native int setWifiTxPowerNative(int i);

    public WifiNative(WifiVendorHal vendorHal, SupplicantStaIfaceHal staIfaceHal, HostapdHal hostapdHal, WificondControl condControl, WifiMonitor wifiMonitor, INetworkManagementService nwService, PropertyService propertyService, WifiMetrics wifiMetrics) {
        this.mWifiVendorHal = vendorHal;
        this.mSupplicantStaIfaceHal = staIfaceHal;
        this.mHostapdHal = hostapdHal;
        this.mWificondControl = condControl;
        this.mWifiMonitor = wifiMonitor;
        this.mNwManagementService = nwService;
        this.mPropertyService = propertyService;
        this.mWifiMetrics = wifiMetrics;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        this.mWificondControl.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mSupplicantStaIfaceHal.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiVendorHal.enableVerboseLogging(this.mVerboseLoggingEnabled);
    }

    private boolean startHal() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyIface()) {
                if (!this.mWifiVendorHal.isVendorHalSupported()) {
                    Log.i(TAG, "Vendor Hal not supported, ignoring start.");
                } else if (!this.mWifiVendorHal.startVendorHal()) {
                    Log.e(TAG, "Failed to start vendor HAL");
                    return false;
                }
            }
            return true;
        }
    }

    private void stopHalAndWificondIfNecessary() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyIface()) {
                if (!this.mWificondControl.tearDownInterfaces()) {
                    Log.e(TAG, "Failed to teardown ifaces from wificond");
                }
                if (this.mWifiVendorHal.isVendorHalSupported()) {
                    this.mWifiVendorHal.stopVendorHal();
                } else {
                    Log.i(TAG, "Vendor Hal not supported, ignoring stop.");
                }
            }
        }
    }

    private boolean waitForSupplicantConnection() {
        int connectTries = 0;
        if (!this.mSupplicantStaIfaceHal.isInitializationStarted() && !this.mSupplicantStaIfaceHal.initialize()) {
            return false;
        }
        boolean connected = false;
        while (!connected) {
            int connectTries2 = connectTries + 1;
            if (connectTries >= CONNECT_TO_SUPPLICANT_RETRY_TIMES) {
                break;
            }
            connected = this.mSupplicantStaIfaceHal.isInitializationComplete();
            if (connected) {
                break;
            }
            try {
                Log.w(TAG, "startMonitoring connectTries sleep:70");
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            connectTries = connectTries2;
        }
        return connected;
    }

    private boolean startSupplicant() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyStaIface()) {
                if (!this.mWificondControl.enableSupplicant()) {
                    Log.e(TAG, "Failed to enable supplicant");
                    return false;
                } else if (!waitForSupplicantConnection()) {
                    Log.e(TAG, "Failed to connect to supplicant");
                    return false;
                } else if (!this.mSupplicantStaIfaceHal.registerDeathHandler(new SupplicantDeathHandlerInternal())) {
                    Log.e(TAG, "Failed to register supplicant death handler");
                    return false;
                }
            }
            return true;
        }
    }

    private void stopSupplicantIfNecessary() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyStaIface()) {
                if (!this.mSupplicantStaIfaceHal.deregisterDeathHandler()) {
                    Log.e(TAG, "Failed to deregister supplicant death handler");
                }
                if (!this.mWificondControl.disableSupplicant()) {
                    Log.e(TAG, "Failed to disable supplicant");
                }
            }
        }
    }

    private boolean registerNetworkObserver(NetworkObserverInternal observer) {
        if (observer == null) {
            return false;
        }
        try {
            this.mNwManagementService.registerObserver(observer);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean unregisterNetworkObserver(NetworkObserverInternal observer) {
        if (observer == null) {
            return false;
        }
        try {
            this.mNwManagementService.unregisterObserver(observer);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private void onClientInterfaceDestroyed(Iface iface) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            this.mWifiMonitor.stopMonitoring(iface.name);
            if (!unregisterNetworkObserver(iface.networkObserver)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to unregister network observer on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            if (!this.mSupplicantStaIfaceHal.teardownIface(iface.name)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to teardown iface in supplicant on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            if (!this.mWificondControl.tearDownClientInterface(iface.name)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to teardown iface in wificond on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            stopSupplicantIfNecessary();
            stopHalAndWificondIfNecessary();
        }
    }

    private void onSoftApInterfaceDestroyed(Iface iface) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (!unregisterNetworkObserver(iface.networkObserver)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to unregister network observer on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            if (!this.mHostapdHal.removeAccessPoint(iface.name)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove access point on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            if (!this.mHostapdHal.deregisterDeathHandler()) {
                Log.e(TAG, "Failed to deregister supplicant death handler");
            }
            if (!this.mWificondControl.stopHostapd(iface.name)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to stop hostapd on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            if (!this.mWificondControl.tearDownSoftApInterface(iface.name)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to teardown iface in wificond on ");
                stringBuilder.append(iface);
                Log.e(str, stringBuilder.toString());
            }
            stopHalAndWificondIfNecessary();
        }
    }

    private void onInterfaceDestroyed(Iface iface) {
        synchronized (this.mLock) {
            if (iface.type == 1) {
                onClientInterfaceDestroyed(iface);
            } else if (iface.type == 0) {
                onSoftApInterfaceDestroyed(iface);
            }
            iface.externalListener.onDestroyed(iface.name);
        }
    }

    private void onNativeDaemonDeath() {
        synchronized (this.mLock) {
            Iterator it = this.mStatusListeners.iterator();
            while (it.hasNext()) {
                ((StatusListener) it.next()).onStatusChanged(false);
            }
            it = this.mStatusListeners.iterator();
            while (it.hasNext()) {
                ((StatusListener) it.next()).onStatusChanged(true);
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x002f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onInterfaceStateChanged(Iface iface, boolean isUp) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (isUp != iface.isUp) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Interface state changed on ");
                stringBuilder.append(iface);
                stringBuilder.append(", isUp=");
                stringBuilder.append(isUp);
                Log.i(str, stringBuilder.toString());
                if (isUp) {
                    iface.externalListener.onUp(iface.name);
                } else {
                    iface.externalListener.onDown(iface.name);
                    if (iface.type == 1) {
                        this.mWifiMetrics.incrementNumClientInterfaceDown();
                    } else if (iface.type == 0) {
                        this.mWifiMetrics.incrementNumSoftApInterfaceDown();
                    }
                }
                iface.isUp = isUp;
            } else if (this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Interface status unchanged on ");
                stringBuilder.append(iface);
                stringBuilder.append(" from ");
                stringBuilder.append(isUp);
                stringBuilder.append(", Ignoring...");
                Log.v(str, stringBuilder.toString());
            }
        }
    }

    private String handleIfaceCreationWhenVendorHalNotSupported(Iface newIface) {
        String str;
        synchronized (this.mLock) {
            Iface existingIface = this.mIfaceMgr.removeExistingIface(newIface.id);
            if (existingIface != null) {
                onInterfaceDestroyed(existingIface);
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Successfully torn down ");
                stringBuilder.append(existingIface);
                Log.i(str, stringBuilder.toString());
            }
            str = this.mPropertyService.getString("wifi.interface", "wlan0");
        }
        return str;
    }

    private String createStaIface(Iface iface, boolean lowPrioritySta) {
        synchronized (this.mLock) {
            String createStaIface;
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                createStaIface = this.mWifiVendorHal.createStaIface(lowPrioritySta, new InterfaceDestoyedListenerInternal(iface.id));
                return createStaIface;
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring createStaIface.");
            createStaIface = handleIfaceCreationWhenVendorHalNotSupported(iface);
            return createStaIface;
        }
    }

    private String createApIface(Iface iface) {
        synchronized (this.mLock) {
            String createApIface;
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                createApIface = this.mWifiVendorHal.createApIface(new InterfaceDestoyedListenerInternal(iface.id));
                return createApIface;
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring createApIface.");
            createApIface = handleIfaceCreationWhenVendorHalNotSupported(iface);
            return createApIface;
        }
    }

    private boolean handleIfaceRemovalWhenVendorHalNotSupported(Iface iface) {
        synchronized (this.mLock) {
            this.mIfaceMgr.removeIface(iface.id);
            onInterfaceDestroyed(iface);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Successfully torn down ");
            stringBuilder.append(iface);
            Log.i(str, stringBuilder.toString());
        }
        return true;
    }

    private boolean removeStaIface(Iface iface) {
        synchronized (this.mLock) {
            boolean removeStaIface;
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                removeStaIface = this.mWifiVendorHal.removeStaIface(iface.name);
                return removeStaIface;
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring removeStaIface.");
            removeStaIface = handleIfaceRemovalWhenVendorHalNotSupported(iface);
            return removeStaIface;
        }
    }

    private boolean removeApIface(Iface iface) {
        synchronized (this.mLock) {
            boolean removeApIface;
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                removeApIface = this.mWifiVendorHal.removeApIface(iface.name);
                return removeApIface;
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring removeApIface.");
            removeApIface = handleIfaceRemovalWhenVendorHalNotSupported(iface);
            return removeApIface;
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (!this.mWifiVendorHal.initialize(new VendorHalDeathHandlerInternal())) {
                Log.e(TAG, "Failed to initialize vendor HAL");
                return false;
            } else if (this.mWificondControl.initialize(new WificondDeathHandlerInternal())) {
                this.mWifiVendorHal.registerRadioModeChangeHandler(new VendorHalRadioModeChangeHandlerInternal());
                return true;
            } else {
                Log.e(TAG, "Failed to initialize wificond");
                return false;
            }
        }
    }

    public void registerStatusListener(StatusListener listener) {
        this.mStatusListeners.add(listener);
    }

    private void initializeNwParamsForClientInterface(String ifaceName) {
        String str;
        StringBuilder stringBuilder;
        try {
            this.mNwManagementService.clearInterfaceAddresses(ifaceName);
            this.mNwManagementService.setInterfaceIpv6PrivacyExtensions(ifaceName, true);
            this.mNwManagementService.disableIpv6(ifaceName);
        } catch (RemoteException re) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to change interface settings: ");
            stringBuilder.append(re);
            Log.e(str, stringBuilder.toString());
        } catch (IllegalStateException ie) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to change interface settings: ");
            stringBuilder.append(ie);
            Log.e(str, stringBuilder.toString());
        }
    }

    public String setupInterfaceForClientMode(boolean lowPrioritySta, InterfaceCallback interfaceCallback) {
        synchronized (this.mLock) {
            if (!startHal()) {
                Log.e(TAG, "Failed to start Hal");
                this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToHal();
                return null;
            } else if (startSupplicant()) {
                Iface iface = this.mIfaceMgr.allocateIface(1);
                if (iface == null) {
                    Log.e(TAG, "Failed to allocate new STA iface");
                    return null;
                }
                iface.externalListener = interfaceCallback;
                iface.name = createStaIface(iface, lowPrioritySta);
                String str;
                StringBuilder stringBuilder;
                if (TextUtils.isEmpty(iface.name)) {
                    Log.e(TAG, "Failed to create STA iface in vendor HAL");
                    this.mIfaceMgr.removeIface(iface.id);
                    this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToHal();
                    return null;
                } else if (this.mWificondControl.setupInterfaceForClientMode(iface.name) == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to setup iface in wificond on ");
                    stringBuilder.append(iface);
                    Log.e(str, stringBuilder.toString());
                    teardownInterface(iface.name);
                    this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToWificond();
                    return null;
                } else if (this.mSupplicantStaIfaceHal.setupIface(iface.name)) {
                    iface.networkObserver = new NetworkObserverInternal(iface.id);
                    if (registerNetworkObserver(iface.networkObserver)) {
                        this.mWifiMonitor.startMonitoring(iface.name);
                        onInterfaceStateChanged(iface, isInterfaceUp(iface.name));
                        initializeNwParamsForClientInterface(iface.name);
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Successfully setup ");
                        stringBuilder2.append(iface);
                        Log.i(str2, stringBuilder2.toString());
                        str2 = iface.name;
                        return str2;
                    }
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to register network observer on ");
                    stringBuilder.append(iface);
                    Log.e(str, stringBuilder.toString());
                    teardownInterface(iface.name);
                    return null;
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to setup iface in supplicant on ");
                    stringBuilder.append(iface);
                    Log.e(str, stringBuilder.toString());
                    teardownInterface(iface.name);
                    this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToSupplicant();
                    return null;
                }
            } else {
                Log.e(TAG, "Failed to start supplicant");
                this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToSupplicant();
                return null;
            }
        }
    }

    public String setupInterfaceForSoftApMode(InterfaceCallback interfaceCallback) {
        synchronized (this.mLock) {
            if (startHal()) {
                Iface iface = this.mIfaceMgr.allocateIface(0);
                if (iface == null) {
                    Log.e(TAG, "Failed to allocate new AP iface");
                    return null;
                }
                iface.externalListener = interfaceCallback;
                iface.name = createApIface(iface);
                String str;
                StringBuilder stringBuilder;
                if (TextUtils.isEmpty(iface.name)) {
                    Log.e(TAG, "Failed to create AP iface in vendor HAL");
                    this.mIfaceMgr.removeIface(iface.id);
                    this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHal();
                    return null;
                } else if (this.mWificondControl.setupInterfaceForSoftApMode(iface.name) == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to setup iface in wificond on ");
                    stringBuilder.append(iface);
                    Log.e(str, stringBuilder.toString());
                    teardownInterface(iface.name);
                    this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToWificond();
                    return null;
                } else {
                    iface.networkObserver = new NetworkObserverInternal(iface.id);
                    if (registerNetworkObserver(iface.networkObserver)) {
                        onInterfaceStateChanged(iface, isInterfaceUp(iface.name));
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Successfully setup ");
                        stringBuilder2.append(iface);
                        Log.i(str2, stringBuilder2.toString());
                        str2 = iface.name;
                        return str2;
                    }
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to register network observer on ");
                    stringBuilder.append(iface);
                    Log.e(str, stringBuilder.toString());
                    teardownInterface(iface.name);
                    return null;
                }
            }
            Log.e(TAG, "Failed to start Hal");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHal();
            return null;
        }
    }

    public boolean isInterfaceUp(String ifaceName) {
        synchronized (this.mLock) {
            if (this.mIfaceMgr.getIface(ifaceName) == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Trying to get iface state on invalid iface=");
                stringBuilder.append(ifaceName);
                Log.e(str, stringBuilder.toString());
                return false;
            }
            InterfaceConfiguration config = null;
            try {
                config = this.mNwManagementService.getInterfaceConfig(ifaceName);
            } catch (RemoteException e) {
            }
            if (config == null) {
                return false;
            }
            boolean isUp = config.isUp();
            return isUp;
        }
    }

    public void teardownInterface(String ifaceName) {
        synchronized (this.mLock) {
            Iface iface = this.mIfaceMgr.getIface(ifaceName);
            String str;
            StringBuilder stringBuilder;
            if (iface == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Trying to teardown an invalid iface=");
                stringBuilder.append(ifaceName);
                Log.e(str, stringBuilder.toString());
                return;
            }
            if (iface.type == 1) {
                if (!removeStaIface(iface)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to remove iface in vendor HAL=");
                    stringBuilder.append(ifaceName);
                    Log.e(str, stringBuilder.toString());
                    return;
                }
            } else if (iface.type == 0 && !removeApIface(iface)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove iface in vendor HAL=");
                stringBuilder.append(ifaceName);
                Log.e(str, stringBuilder.toString());
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Successfully initiated teardown for iface=");
            stringBuilder.append(ifaceName);
            Log.i(str, stringBuilder.toString());
        }
    }

    public void teardownAllInterfaces() {
        synchronized (this.mLock) {
            Iterator<Integer> ifaceIdIter = this.mIfaceMgr.getIfaceIdIter();
            while (ifaceIdIter.hasNext()) {
                Iface iface = this.mIfaceMgr.getIface(((Integer) ifaceIdIter.next()).intValue());
                ifaceIdIter.remove();
                onInterfaceDestroyed(iface);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Successfully torn down ");
                stringBuilder.append(iface);
                Log.i(str, stringBuilder.toString());
            }
            Log.i(TAG, "Successfully torn down all ifaces");
        }
    }

    public String getClientInterfaceName() {
        String access$1900;
        synchronized (this.mLock) {
            access$1900 = this.mIfaceMgr.findAnyStaIfaceName();
        }
        return access$1900;
    }

    public String getSoftApInterfaceName() {
        String access$2000;
        synchronized (this.mLock) {
            access$2000 = this.mIfaceMgr.findAnyApIfaceName();
        }
        return access$2000;
    }

    public SignalPollResult signalPoll(String ifaceName) {
        return this.mWificondControl.signalPoll(ifaceName);
    }

    public TxPacketCounters getTxPacketCounters(String ifaceName) {
        return this.mWificondControl.getTxPacketCounters(ifaceName);
    }

    public int[] getChannelsForBand(int band) {
        return this.mWificondControl.getChannelsForBand(band);
    }

    public boolean scan(String ifaceName, int scanType, Set<Integer> freqs, List<String> hiddenNetworkSSIDs) {
        return this.mWificondControl.scan(ifaceName, scanType, freqs, hiddenNetworkSSIDs);
    }

    public ArrayList<ScanDetail> getScanResults(String ifaceName) {
        return this.mWificondControl.getScanResults(ifaceName, 0);
    }

    public ArrayList<ScanDetail> getPnoScanResults(String ifaceName) {
        return this.mWificondControl.getScanResults(ifaceName, 1);
    }

    public boolean startPnoScan(String ifaceName, PnoSettings pnoSettings) {
        return this.mWificondControl.startPnoScan(ifaceName, pnoSettings);
    }

    public boolean stopPnoScan(String ifaceName) {
        return this.mWificondControl.stopPnoScan(ifaceName);
    }

    private boolean waitForHostapdConnection() {
        int connectTries = 0;
        if (!this.mHostapdHal.isInitializationStarted() && !this.mHostapdHal.initialize()) {
            return false;
        }
        boolean connected = false;
        while (!connected) {
            int connectTries2 = connectTries + 1;
            if (connectTries >= 50) {
                break;
            }
            connected = this.mHostapdHal.isInitializationComplete();
            if (connected) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            connectTries = connectTries2;
        }
        return connected;
    }

    public boolean startSoftAp(String ifaceName, WifiConfiguration config, SoftApListener listener) {
        if (!this.mWificondControl.startHostapd(ifaceName, listener)) {
            Log.e(TAG, "Failed to start hostapd");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        } else if (!waitForHostapdConnection()) {
            Log.e(TAG, "Failed to establish connection to hostapd");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        } else if (!this.mHostapdHal.registerDeathHandler(new HostapdDeathHandlerInternal())) {
            Log.e(TAG, "Failed to register hostapd death handler");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        } else if (this.mHostapdHal.addAccessPoint(ifaceName, config)) {
            return true;
        } else {
            Log.e(TAG, "Failed to add acccess point");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        }
    }

    public boolean stopSoftAp(String ifaceName) {
        if (!this.mHostapdHal.removeAccessPoint(ifaceName)) {
            Log.e(TAG, "Failed to remove access point");
        }
        return this.mWificondControl.stopHostapd(ifaceName);
    }

    public boolean setMacAddress(String interfaceName, MacAddress mac) {
        return this.mWifiVendorHal.setMacAddress(interfaceName, mac);
    }

    public void setSupplicantLogLevel(boolean turnOnVerbose) {
        this.mSupplicantStaIfaceHal.setLogLevel(turnOnVerbose);
    }

    public boolean reconnect(String ifaceName) {
        boolean result = this.mSupplicantStaIfaceHal.reconnect(ifaceName);
        if (result) {
            WifiCommonUtils.notifyDeviceState(WifiCommonUtils.DEVICE_WLAN, WifiCommonUtils.STATE_CONNECT_START, "");
        }
        return result;
    }

    public boolean reassociate(String ifaceName) {
        boolean result = this.mSupplicantStaIfaceHal.reassociate(ifaceName);
        if (result) {
            WifiCommonUtils.notifyDeviceState(WifiCommonUtils.DEVICE_WLAN, WifiCommonUtils.STATE_CONNECT_START, "");
        }
        return result;
    }

    public boolean disconnect(String ifaceName) {
        return this.mSupplicantStaIfaceHal.disconnect(ifaceName);
    }

    public String getMacAddress(String ifaceName) {
        return this.mSupplicantStaIfaceHal.getMacAddress(ifaceName);
    }

    public boolean startFilteringMulticastV4Packets(String ifaceName) {
        if (this.mSupplicantStaIfaceHal.stopRxFilter(ifaceName) && this.mSupplicantStaIfaceHal.removeRxFilter(ifaceName, 0) && this.mSupplicantStaIfaceHal.startRxFilter(ifaceName)) {
            return true;
        }
        return false;
    }

    public boolean stopFilteringMulticastV4Packets(String ifaceName) {
        if (this.mSupplicantStaIfaceHal.stopRxFilter(ifaceName) && this.mSupplicantStaIfaceHal.addRxFilter(ifaceName, 0) && this.mSupplicantStaIfaceHal.startRxFilter(ifaceName)) {
            return true;
        }
        return false;
    }

    public boolean startFilteringMulticastV6Packets(String ifaceName) {
        if (this.mSupplicantStaIfaceHal.stopRxFilter(ifaceName) && this.mSupplicantStaIfaceHal.removeRxFilter(ifaceName, 1) && this.mSupplicantStaIfaceHal.startRxFilter(ifaceName)) {
            return true;
        }
        return false;
    }

    public boolean stopFilteringMulticastV6Packets(String ifaceName) {
        if (this.mSupplicantStaIfaceHal.stopRxFilter(ifaceName) && this.mSupplicantStaIfaceHal.addRxFilter(ifaceName, 1) && this.mSupplicantStaIfaceHal.startRxFilter(ifaceName)) {
            return true;
        }
        return false;
    }

    public boolean startRxFilter(String ifaceName) {
        return this.mSupplicantStaIfaceHal.startRxFilter(ifaceName);
    }

    public boolean stopRxFilter(String ifaceName) {
        return this.mSupplicantStaIfaceHal.stopRxFilter(ifaceName);
    }

    public boolean setBluetoothCoexistenceMode(String ifaceName, int mode) {
        return this.mSupplicantStaIfaceHal.setBtCoexistenceMode(ifaceName, mode);
    }

    public boolean setBluetoothCoexistenceScanMode(String ifaceName, boolean setCoexScanMode) {
        return this.mSupplicantStaIfaceHal.setBtCoexistenceScanModeEnabled(ifaceName, setCoexScanMode);
    }

    public boolean setSuspendOptimizations(String ifaceName, boolean enabled) {
        return this.mSupplicantStaIfaceHal.setSuspendModeEnabled(ifaceName, enabled);
    }

    public boolean setCountryCode(String ifaceName, String countryCode) {
        return this.mSupplicantStaIfaceHal.setCountryCode(ifaceName, countryCode);
    }

    public boolean setFilterEnable(String ifaceName, boolean enable) {
        return this.mSupplicantStaIfaceHal.setFilterEnable(ifaceName, enable);
    }

    public void startTdls(String ifaceName, String macAddr, boolean enable) {
        if (enable) {
            this.mSupplicantStaIfaceHal.initiateTdlsDiscover(ifaceName, macAddr);
            this.mSupplicantStaIfaceHal.initiateTdlsSetup(ifaceName, macAddr);
            return;
        }
        this.mSupplicantStaIfaceHal.initiateTdlsTeardown(ifaceName, macAddr);
    }

    public boolean startWpsPbc(String ifaceName, String bssid) {
        return this.mSupplicantStaIfaceHal.startWpsPbc(ifaceName, bssid);
    }

    public boolean startWpsPinKeypad(String ifaceName, String pin) {
        return this.mSupplicantStaIfaceHal.startWpsPinKeypad(ifaceName, pin);
    }

    public String startWpsPinDisplay(String ifaceName, String bssid) {
        return this.mSupplicantStaIfaceHal.startWpsPinDisplay(ifaceName, bssid);
    }

    public boolean setExternalSim(String ifaceName, boolean external) {
        return this.mSupplicantStaIfaceHal.setExternalSim(ifaceName, external);
    }

    public boolean simAuthResponse(String ifaceName, int id, String type, String response) {
        if (SIM_AUTH_RESP_TYPE_GSM_AUTH.equals(type)) {
            return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimGsmAuthResponse(ifaceName, response);
        }
        if (SIM_AUTH_RESP_TYPE_UMTS_AUTH.equals(type)) {
            return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimUmtsAuthResponse(ifaceName, response);
        }
        if (SIM_AUTH_RESP_TYPE_UMTS_AUTS.equals(type)) {
            return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimUmtsAutsResponse(ifaceName, response);
        }
        return false;
    }

    public boolean simAuthFailedResponse(String ifaceName, int id) {
        return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimGsmAuthFailure(ifaceName);
    }

    public boolean umtsAuthFailedResponse(String ifaceName, int id) {
        return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimUmtsAuthFailure(ifaceName);
    }

    public boolean simIdentityResponse(String ifaceName, int id, String unencryptedResponse, String encryptedResponse) {
        return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapIdentityResponse(ifaceName, unencryptedResponse, encryptedResponse);
    }

    public String getEapAnonymousIdentity(String ifaceName) {
        return this.mSupplicantStaIfaceHal.getCurrentNetworkEapAnonymousIdentity(ifaceName);
    }

    public boolean startWpsRegistrar(String ifaceName, String bssid, String pin) {
        return this.mSupplicantStaIfaceHal.startWpsRegistrar(ifaceName, bssid, pin);
    }

    public boolean cancelWps(String ifaceName) {
        return this.mSupplicantStaIfaceHal.cancelWps(ifaceName);
    }

    public boolean setDeviceName(String ifaceName, String name) {
        return this.mSupplicantStaIfaceHal.setWpsDeviceName(ifaceName, name);
    }

    public boolean setDeviceType(String ifaceName, String type) {
        return this.mSupplicantStaIfaceHal.setWpsDeviceType(ifaceName, type);
    }

    public boolean setConfigMethods(String ifaceName, String cfg) {
        return this.mSupplicantStaIfaceHal.setWpsConfigMethods(ifaceName, cfg);
    }

    public boolean setManufacturer(String ifaceName, String value) {
        return this.mSupplicantStaIfaceHal.setWpsManufacturer(ifaceName, value);
    }

    public boolean setModelName(String ifaceName, String value) {
        return this.mSupplicantStaIfaceHal.setWpsModelName(ifaceName, value);
    }

    public boolean setModelNumber(String ifaceName, String value) {
        return this.mSupplicantStaIfaceHal.setWpsModelNumber(ifaceName, value);
    }

    public boolean setSerialNumber(String ifaceName, String value) {
        return this.mSupplicantStaIfaceHal.setWpsSerialNumber(ifaceName, value);
    }

    public void setPowerSave(String ifaceName, boolean enabled) {
        this.mSupplicantStaIfaceHal.setPowerSave(ifaceName, enabled);
    }

    public boolean setConcurrencyPriority(boolean isStaHigherPriority) {
        return this.mSupplicantStaIfaceHal.setConcurrencyPriority(isStaHigherPriority);
    }

    public boolean enableStaAutoReconnect(String ifaceName, boolean enable) {
        return this.mSupplicantStaIfaceHal.enableAutoReconnect(ifaceName, enable);
    }

    public boolean migrateNetworksFromSupplicant(String ifaceName, Map<String, WifiConfiguration> configs, SparseArray<Map<String, String>> networkExtras) {
        return this.mSupplicantStaIfaceHal.loadNetworks(ifaceName, configs, networkExtras);
    }

    public boolean connectToNetwork(String ifaceName, WifiConfiguration configuration) {
        this.mWificondControl.abortScan(ifaceName);
        boolean result = this.mSupplicantStaIfaceHal.connectToNetwork(ifaceName, configuration);
        if (result) {
            WifiCommonUtils.notifyDeviceState(WifiCommonUtils.DEVICE_WLAN, WifiCommonUtils.STATE_CONNECT_START, "");
        }
        return result;
    }

    public boolean roamToNetwork(String ifaceName, WifiConfiguration configuration) {
        this.mWificondControl.abortScan(ifaceName);
        boolean result = this.mSupplicantStaIfaceHal.roamToNetwork(ifaceName, configuration);
        if (result) {
            WifiCommonUtils.notifyDeviceState(WifiCommonUtils.DEVICE_WLAN, WifiCommonUtils.STATE_CONNECT_START, "");
        }
        return result;
    }

    public int getFrameworkNetworkId(int supplicantNetworkId) {
        return supplicantNetworkId;
    }

    public boolean removeAllNetworks(String ifaceName) {
        return this.mSupplicantStaIfaceHal.removeAllNetworks(ifaceName);
    }

    public boolean setConfiguredNetworkBSSID(String ifaceName, String bssid) {
        return this.mSupplicantStaIfaceHal.setCurrentNetworkBssid(ifaceName, bssid);
    }

    public boolean requestAnqp(String ifaceName, String bssid, Set<Integer> anqpIds, Set<Integer> hs20Subtypes) {
        if (bssid == null || ((anqpIds == null || anqpIds.isEmpty()) && (hs20Subtypes == null || hs20Subtypes.isEmpty()))) {
            Log.e(TAG, "Invalid arguments for ANQP request.");
            return false;
        }
        ArrayList anqpIdList = new ArrayList();
        if (anqpIds != null) {
            for (Integer anqpId : anqpIds) {
                anqpIdList.add(Short.valueOf(anqpId.shortValue()));
            }
        }
        ArrayList hs20SubtypeList = new ArrayList();
        hs20SubtypeList.addAll(hs20Subtypes);
        return this.mSupplicantStaIfaceHal.initiateAnqpQuery(ifaceName, bssid, anqpIdList, hs20SubtypeList);
    }

    public boolean requestIcon(String ifaceName, String bssid, String fileName) {
        if (bssid != null && fileName != null) {
            return this.mSupplicantStaIfaceHal.initiateHs20IconQuery(ifaceName, bssid, fileName);
        }
        Log.e(TAG, "Invalid arguments for Icon request.");
        return false;
    }

    public String getCurrentNetworkWpsNfcConfigurationToken(String ifaceName) {
        return this.mSupplicantStaIfaceHal.getCurrentNetworkWpsNfcConfigurationToken(ifaceName);
    }

    public void removeNetworkIfCurrent(String ifaceName, int networkId) {
        this.mSupplicantStaIfaceHal.removeNetworkIfCurrent(ifaceName, networkId);
    }

    public boolean isHalStarted() {
        return this.mWifiVendorHal.isHalStarted();
    }

    public boolean getBgScanCapabilities(String ifaceName, ScanCapabilities capabilities) {
        return this.mWifiVendorHal.getBgScanCapabilities(ifaceName, capabilities);
    }

    public boolean startBgScan(String ifaceName, ScanSettings settings, ScanEventHandler eventHandler) {
        return this.mWifiVendorHal.startBgScan(ifaceName, settings, eventHandler);
    }

    public void stopBgScan(String ifaceName) {
        this.mWifiVendorHal.stopBgScan(ifaceName);
    }

    public void pauseBgScan(String ifaceName) {
        this.mWifiVendorHal.pauseBgScan(ifaceName);
    }

    public void restartBgScan(String ifaceName) {
        this.mWifiVendorHal.restartBgScan(ifaceName);
    }

    public ScanData[] getBgScanResults(String ifaceName) {
        return this.mWifiVendorHal.getBgScanResults(ifaceName);
    }

    public WifiLinkLayerStats getWifiLinkLayerStats(String ifaceName) {
        return this.mWifiVendorHal.getWifiLinkLayerStats(ifaceName);
    }

    public int getSupportedFeatureSet(String ifaceName) {
        return this.mWifiVendorHal.getSupportedFeatureSet(ifaceName);
    }

    public boolean requestRtt(RttParams[] params, RttEventHandler handler) {
        return this.mWifiVendorHal.requestRtt(params, handler);
    }

    public boolean cancelRtt(RttParams[] params) {
        return this.mWifiVendorHal.cancelRtt(params);
    }

    public ResponderConfig enableRttResponder(int timeoutSeconds) {
        return this.mWifiVendorHal.enableRttResponder(timeoutSeconds);
    }

    public boolean disableRttResponder() {
        return this.mWifiVendorHal.disableRttResponder();
    }

    public boolean setScanningMacOui(String ifaceName, byte[] oui) {
        return this.mWifiVendorHal.setScanningMacOui(ifaceName, oui);
    }

    public RttCapabilities getRttCapabilities() {
        return this.mWifiVendorHal.getRttCapabilities();
    }

    public ApfCapabilities getApfCapabilities(String ifaceName) {
        return this.mWifiVendorHal.getApfCapabilities(ifaceName);
    }

    public boolean installPacketFilter(String ifaceName, byte[] filter) {
        return this.mWifiVendorHal.installPacketFilter(ifaceName, filter);
    }

    public byte[] readPacketFilter(String ifaceName) {
        return this.mWifiVendorHal.readPacketFilter(ifaceName);
    }

    public boolean setCountryCodeHal(String ifaceName, String countryCode) {
        return this.mWifiVendorHal.setCountryCodeHal(ifaceName, countryCode);
    }

    public boolean setLoggingEventHandler(WifiLoggerEventHandler handler) {
        return this.mWifiVendorHal.setLoggingEventHandler(handler);
    }

    public boolean startLoggingRingBuffer(int verboseLevel, int flags, int maxInterval, int minDataSize, String ringName) {
        return this.mWifiVendorHal.startLoggingRingBuffer(verboseLevel, flags, maxInterval, minDataSize, ringName);
    }

    public int getSupportedLoggerFeatureSet() {
        return this.mWifiVendorHal.getSupportedLoggerFeatureSet();
    }

    public boolean resetLogHandler() {
        return this.mWifiVendorHal.resetLogHandler();
    }

    public String getDriverVersion() {
        return this.mWifiVendorHal.getDriverVersion();
    }

    public String getFirmwareVersion() {
        return this.mWifiVendorHal.getFirmwareVersion();
    }

    public RingBufferStatus[] getRingBufferStatus() {
        return this.mWifiVendorHal.getRingBufferStatus();
    }

    public boolean getRingBufferData(String ringName) {
        return this.mWifiVendorHal.getRingBufferData(ringName);
    }

    public byte[] getFwMemoryDump() {
        return this.mWifiVendorHal.getFwMemoryDump();
    }

    public byte[] getDriverStateDump() {
        return this.mWifiVendorHal.getDriverStateDump();
    }

    public boolean startPktFateMonitoring(String ifaceName) {
        return this.mWifiVendorHal.startPktFateMonitoring(ifaceName);
    }

    public boolean getTxPktFates(String ifaceName, TxFateReport[] reportBufs) {
        return this.mWifiVendorHal.getTxPktFates(ifaceName, reportBufs);
    }

    public boolean getRxPktFates(String ifaceName, RxFateReport[] reportBufs) {
        return this.mWifiVendorHal.getRxPktFates(ifaceName, reportBufs);
    }

    public long getTxPackets(String ifaceName) {
        return TrafficStats.getTxPackets(ifaceName);
    }

    public long getRxPackets(String ifaceName) {
        return TrafficStats.getRxPackets(ifaceName);
    }

    public int startSendingOffloadedPacket(String ifaceName, int slot, byte[] dstMac, byte[] packet, int protocol, int period) {
        return this.mWifiVendorHal.startSendingOffloadedPacket(ifaceName, slot, NativeUtil.macAddressToByteArray(getMacAddress(ifaceName)), dstMac, packet, protocol, period);
    }

    public int stopSendingOffloadedPacket(String ifaceName, int slot) {
        return this.mWifiVendorHal.stopSendingOffloadedPacket(ifaceName, slot);
    }

    public int startRssiMonitoring(String ifaceName, byte maxRssi, byte minRssi, WifiRssiEventHandler rssiEventHandler) {
        return this.mWifiVendorHal.startRssiMonitoring(ifaceName, maxRssi, minRssi, rssiEventHandler);
    }

    public int stopRssiMonitoring(String ifaceName) {
        return this.mWifiVendorHal.stopRssiMonitoring(ifaceName);
    }

    public WifiWakeReasonAndCounts getWlanWakeReasonCount() {
        return this.mWifiVendorHal.getWlanWakeReasonCount();
    }

    public boolean configureNeighborDiscoveryOffload(String ifaceName, boolean enabled) {
        return this.mWifiVendorHal.configureNeighborDiscoveryOffload(ifaceName, enabled);
    }

    public boolean getRoamingCapabilities(String ifaceName, RoamingCapabilities capabilities) {
        return this.mWifiVendorHal.getRoamingCapabilities(ifaceName, capabilities);
    }

    public int enableFirmwareRoaming(String ifaceName, int state) {
        return this.mWifiVendorHal.enableFirmwareRoaming(ifaceName, state);
    }

    public boolean configureRoaming(String ifaceName, RoamingConfig config) {
        return this.mWifiVendorHal.configureRoaming(ifaceName, config);
    }

    public boolean resetRoamingConfiguration(String ifaceName) {
        return this.mWifiVendorHal.configureRoaming(ifaceName, new RoamingConfig());
    }

    public boolean selectTxPowerScenario(int scenario) {
        return this.mWifiVendorHal.selectTxPowerScenario(scenario);
    }

    static {
        System.loadLibrary("wifi-service");
        registerNatives();
    }

    public synchronized String readKernelLog() {
        byte[] bytes = readKernelLogNative();
        if (bytes != null) {
            try {
                return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException e) {
                return new String(bytes, StandardCharsets.ISO_8859_1);
            }
        }
        return "*** failed to read kernel log ***";
    }

    public String getWpaSuppConfig() {
        return this.mSupplicantStaIfaceHal.getWpasConfig(getClientInterfaceName(), 0);
    }

    public void sendWifiPowerCommand(int level) {
        this.mSupplicantStaIfaceHal.setTxPower(getClientInterfaceName(), level);
    }

    public void setIsmcoexMode(boolean enable) {
    }

    public boolean isSupportVoWifiDetect() {
        String ret = this.mSupplicantStaIfaceHal.voWifiDetect(getClientInterfaceName(), "VOWIFI_DETECT VOWIFi_IS_SUPPORT");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSupportVoWifiDetect ret :");
        stringBuilder.append(ret);
        Log.e(str, stringBuilder.toString());
        return ret != null && (ret.equals("true") || ret.equals("OK"));
    }

    public boolean voWifiDetectSet(String cmd) {
        String ret = this.mSupplicantStaIfaceHal;
        String clientInterfaceName = getClientInterfaceName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(VOWIFI_DETECT_SET_PREFIX);
        stringBuilder.append(cmd);
        ret = ret.voWifiDetect(clientInterfaceName, stringBuilder.toString());
        clientInterfaceName = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("voWifiDetectSet ret :");
        stringBuilder.append(ret);
        Log.d(clientInterfaceName, stringBuilder.toString());
        return ret != null && (ret.equals("true") || ret.equals("OK"));
    }

    public String heartBeat(String param) {
        return this.mSupplicantStaIfaceHal.heartBeat(getClientInterfaceName(), param);
    }

    public void enableHiLinkHandshake(boolean uiEnable, String bssid) {
        this.mSupplicantStaIfaceHal.enableHiLinkHandshake(getClientInterfaceName(), uiEnable, bssid);
    }

    public boolean hwABSSetCapability(int capability) {
        Log.d(TAG, "SET_ABS_CAPABILITY ");
        return this.mSupplicantStaIfaceHal.setAbsCapability(getClientInterfaceName(), capability);
    }

    public boolean hwABSSoftHandover(int type) {
        Log.d(TAG, "hwABSSoftHandover ");
        return this.mSupplicantStaIfaceHal.absPowerCtrl(getClientInterfaceName(), type);
    }

    public boolean hwABSBlackList(String bssidList) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwABSBlackList bssidList = ");
        stringBuilder.append(bssidList);
        Log.d(str, stringBuilder.toString());
        return this.mSupplicantStaIfaceHal.setAbsBlacklist(getClientInterfaceName(), bssidList);
    }

    public void query11vRoamingNetwork(int reason, String preferredBssid) {
    }

    public void query11vRoamingNetwork(int reason) {
        if (isSupportDot11V()) {
            this.mSupplicantStaIfaceHal.query11vRoamingNetwork(getClientInterfaceName(), reason);
        } else {
            Log.d(TAG, "unsupport 11v, dont trigger bss query");
        }
    }

    public boolean isSupportRsdbByDriver() {
        String result = this.mSupplicantStaIfaceHal.getRsdbCapability(getClientInterfaceName());
        if (result != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSupportRsdbByDriver: ");
            stringBuilder.append(result);
            Log.d(str, stringBuilder.toString());
            return "RSDB:1".equals(result);
        }
        Log.i(TAG, "isSupportRsdbByDriver: ");
        return false;
    }

    public boolean isDfsChannel(int frequency) {
        int[] channelsDfs = getChannelsForBand(4);
        String str;
        if (channelsDfs == null) {
            Log.d(TAG, "Failed to get channels for 5GHz DFS only band,get 5GHz band");
            int[] channels5G = getChannelsForBand(2);
            if (channels5G == null) {
                Log.d(TAG, "Failed to get channels for 5GHz band");
                return false;
            }
            for (int channel5G : channels5G) {
                if (frequency == channel5G) {
                    return false;
                }
            }
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isDfsChannel: true, frequency not in channels5G: ");
            stringBuilder.append(frequency);
            Log.d(str, stringBuilder.toString());
            return true;
        }
        for (int i : channelsDfs) {
            if (frequency == i) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isDfsChannel: true, DfsChannel: ");
                stringBuilder2.append(i);
                Log.d(str, stringBuilder2.toString());
                return true;
            }
        }
        return false;
    }

    public String getConnectionRawPsk() {
        return this.mSupplicantStaIfaceHal.getWpasConfig(getClientInterfaceName(), 1);
    }

    public boolean setStaticARP(String ipSrc, String mac) {
        boolean z = false;
        if (TextUtils.isEmpty(ipSrc) || TextUtils.isEmpty(mac)) {
            return false;
        }
        String ifaceName = getClientInterfaceName();
        if (TextUtils.isEmpty(ifaceName)) {
            return false;
        }
        Log.d(TAG, "setStaticARP entered");
        if (hwSetArpItemNative(ifaceName, ipSrc, mac) == 0) {
            z = true;
        }
        return z;
    }

    public boolean delStaticARP(String ipSrc) {
        boolean z = false;
        if (TextUtils.isEmpty(ipSrc)) {
            return false;
        }
        String ifaceName = getClientInterfaceName();
        if (TextUtils.isEmpty(ifaceName)) {
            return false;
        }
        Log.d(TAG, "delStaticARP entered");
        if (hwDelArpItemNative(ifaceName, ipSrc) == 0) {
            z = true;
        }
        return z;
    }

    private String getAndCheckSoftApInterfaceName(String operation) {
        String ifaceName = getSoftApInterfaceName();
        if (ifaceName != null) {
            return ifaceName;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No softap interfaces, do not ");
        stringBuilder.append(operation);
        Log.e(str, stringBuilder.toString());
        return null;
    }

    public boolean setSoftapHw(String chan, String mscb) {
        Log.d(TAG, "setSoftapHw entered");
        boolean z = false;
        if (TextUtils.isEmpty(chan) || TextUtils.isEmpty(mscb)) {
            Log.e(TAG, "Got empty string parameter input");
            return false;
        }
        String softApInterfaceName = getAndCheckSoftApInterfaceName("setSoftapHw");
        if (softApInterfaceName != null && setSoftapHwNative(softApInterfaceName, chan, mscb) == 0) {
            z = true;
        }
        return z;
    }

    public String getSoftapClientsHw() {
        Log.d(TAG, "getSoftapClientsHw entered");
        String softApInterfaceName = getAndCheckSoftApInterfaceName("getSoftapClientsHw");
        return softApInterfaceName != null ? getSoftapClientsHwNative(softApInterfaceName) : null;
    }

    public String readSoftapDhcpLeaseFileHw() {
        Log.d(TAG, "readSoftapDhcpLeaseFileHw entered");
        String softApInterfaceName = getAndCheckSoftApInterfaceName("readSoftapDhcpLeaseFileHw");
        return softApInterfaceName != null ? readSoftapDhcpLeaseFileHwNative(softApInterfaceName) : null;
    }

    public boolean setSoftapMacFltrHw(String filter_str) {
        Log.d(TAG, "setSoftapMacFltrHw entered");
        boolean z = false;
        if (TextUtils.isEmpty(filter_str)) {
            Log.e(TAG, "Got empty mac filter string");
            return false;
        }
        String softApInterfaceName = getAndCheckSoftApInterfaceName("setSoftapMacFltrHw");
        if (softApInterfaceName != null && setSoftapMacFltrHwNative(softApInterfaceName, filter_str) == 0) {
            z = true;
        }
        return z;
    }

    public boolean disassociateSoftapStaHw(String dis_mac) {
        Log.d(TAG, "disassociateSoftapStaHw entered");
        boolean z = false;
        if (TextUtils.isEmpty(dis_mac)) {
            Log.e(TAG, "Got empty disassociate mac string");
            return false;
        }
        String softApInterfaceName = getAndCheckSoftApInterfaceName("disassociateSoftapStaHw");
        if (softApInterfaceName != null && disassociateSoftapStaHwNative(softApInterfaceName, dis_mac) == 0) {
            z = true;
        }
        return z;
    }

    public boolean deauthLastRoamingBssidHw(String mode, String bssid) {
        boolean z = false;
        if (TextUtils.isEmpty(mode)) {
            return false;
        }
        Log.d(TAG, "deauthLastRoamingBssidHw entered");
        if (deauthLastRoamingBssidHwNative(getClientInterfaceName(), mode, bssid) == 0) {
            z = true;
        }
        return z;
    }

    public int setWifiTxPowerHw(int power) {
        return setWifiTxPowerNative(power);
    }

    public void pwrPercentBoostModeset(int rssi) {
        this.mSupplicantStaIfaceHal.pwrPercentBoostModeset(getClientInterfaceName(), rssi);
    }

    public String getMssState() {
        return this.mSupplicantStaIfaceHal.getMssState(getClientInterfaceName());
    }

    public String getApVendorInfo() {
        return this.mSupplicantStaIfaceHal.getApVendorInfo(getClientInterfaceName());
    }

    public void gameKOGAdjustSpeed(int freq, int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gameKOGAdjustSpeed entered: ");
        stringBuilder.append(freq);
        stringBuilder.append(" mode: ");
        stringBuilder.append(mode);
        Log.d(str, stringBuilder.toString());
        gameKOGAdjustSpeedNative(freq, mode);
    }

    public boolean setPwrBoost(int enable) {
        Log.d(TAG, "pwr:setPwrBoost entered");
        return hwSetPwrBoostNative(enable) == 0;
    }

    public int setWifiAnt(String iface, int mode, int operation) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWifiAnt iface: ");
        stringBuilder.append(iface);
        stringBuilder.append(" mode: ");
        stringBuilder.append(mode);
        stringBuilder.append(" operation: ");
        stringBuilder.append(operation);
        Log.d(str, stringBuilder.toString());
        return setWifiAntNative(iface, mode, operation);
    }

    public int getWifiAnt(String iface, int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getWifiAnt iface: ");
        stringBuilder.append(iface);
        stringBuilder.append(" mode: ");
        stringBuilder.append(mode);
        Log.d(str, stringBuilder.toString());
        return getWifiAntNative(iface, mode);
    }

    public int setCmdToWifiChip(String iface, int mode, int type, int action, int param) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCmdToWifiChip iface: ");
        stringBuilder.append(iface);
        stringBuilder.append(" mode: ");
        stringBuilder.append(mode);
        stringBuilder.append(" type: ");
        stringBuilder.append(type);
        stringBuilder.append(" action: ");
        stringBuilder.append(action);
        stringBuilder.append(" param: ");
        stringBuilder.append(param);
        Log.d(str, stringBuilder.toString());
        return setCmdToWifiChipNative(iface, mode, type, action, param);
    }

    public int sendCmdToDriver(String iface, int cmdid, byte[] buffers) {
        if (iface == null || buffers == null) {
            return -1;
        }
        return sendCmdToDriverNative(iface, cmdid, buffers, buffers.length);
    }

    public int getPrivFeatureCapability() {
        return this.mPrivFeatureCapab;
    }

    public void initPrivFeatureCapability() {
        byte[] buff = new byte[4];
        String ifaceName = getClientInterfaceName();
        if (TextUtils.isEmpty(ifaceName)) {
            Log.d(TAG, "PrivFeatureCapab invalid interfaceName");
            return;
        }
        this.mPrivFeatureCapab = sendCmdToDriver(ifaceName, 101, buff);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PrivFeatureCapab value: ");
        stringBuilder.append(this.mPrivFeatureCapab);
        Log.d(str, stringBuilder.toString());
    }

    public boolean isSupportDot11V() {
        boolean z = false;
        if (this.mPrivFeatureCapab <= 0) {
            return false;
        }
        if ((this.mPrivFeatureCapab & 2) == 2) {
            z = true;
        }
        return z;
    }
}
