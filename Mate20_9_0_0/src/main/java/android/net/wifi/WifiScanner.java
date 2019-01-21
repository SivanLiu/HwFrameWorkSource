package android.net.wifi;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.WorkSource;
import android.provider.SettingsStringUtil;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SystemApi
public class WifiScanner {
    private static final int BASE = 159744;
    public static final int CMD_DEREGISTER_SCAN_LISTENER = 159772;
    public static final int CMD_FULL_SCAN_RESULT = 159764;
    public static final int CMD_GET_SCAN_RESULTS = 159748;
    public static final int CMD_GET_SINGLE_SCAN_RESULTS = 159773;
    public static final int CMD_OP_FAILED = 159762;
    public static final int CMD_OP_SUCCEEDED = 159761;
    public static final int CMD_PNO_NETWORK_FOUND = 159770;
    public static final int CMD_REGISTER_SCAN_LISTENER = 159771;
    public static final int CMD_SCAN_RESULT = 159749;
    public static final int CMD_SET_SINGLE_SCAN_KEYS = 159774;
    public static final int CMD_SINGLE_SCAN_COMPLETED = 159767;
    public static final int CMD_START_BACKGROUND_SCAN = 159746;
    public static final int CMD_START_PNO_SCAN = 159768;
    public static final int CMD_START_SINGLE_SCAN = 159765;
    public static final int CMD_STOP_BACKGROUND_SCAN = 159747;
    public static final int CMD_STOP_PNO_SCAN = 159769;
    public static final int CMD_STOP_SINGLE_SCAN = 159766;
    private static final boolean DBG = false;
    public static final String GET_AVAILABLE_CHANNELS_EXTRA = "Channels";
    private static final int GET_SINGLE_SCAN_RESULTS_FAIL_RETRY_COUNT = 3;
    private static final int INVALID_KEY = 0;
    private static final int MAX_KEY = 100;
    public static final int MAX_SCAN_PERIOD_MS = 1024000;
    public static final int MIN_SCAN_PERIOD_MS = 1000;
    public static final String PNO_PARAMS_PNO_SETTINGS_KEY = "PnoSettings";
    public static final String PNO_PARAMS_SCAN_SETTINGS_KEY = "ScanSettings";
    public static final int REASON_DUPLICATE_REQEUST = -5;
    public static final int REASON_INVALID_LISTENER = -2;
    public static final int REASON_INVALID_REQUEST = -3;
    public static final int REASON_NOT_AUTHORIZED = -4;
    public static final int REASON_SUCCEEDED = 0;
    public static final int REASON_UNSPECIFIED = -1;
    @Deprecated
    public static final int REPORT_EVENT_AFTER_BUFFER_FULL = 0;
    public static final int REPORT_EVENT_AFTER_EACH_SCAN = 1;
    public static final int REPORT_EVENT_FULL_SCAN_RESULT = 2;
    public static final int REPORT_EVENT_NO_BATCH = 4;
    public static final String SCAN_PARAMS_SCAN_SETTINGS_KEY = "ScanSettings";
    public static final String SCAN_PARAMS_WORK_SOURCE_KEY = "WorkSource";
    private static final String TAG = "WifiScanner";
    public static final int TYPE_HIGH_ACCURACY = 2;
    public static final int TYPE_LOW_LATENCY = 0;
    public static final int TYPE_LOW_POWER = 1;
    public static final int WIFI_BAND_24_GHZ = 1;
    public static final int WIFI_BAND_5_GHZ = 2;
    public static final int WIFI_BAND_5_GHZ_DFS_ONLY = 4;
    public static final int WIFI_BAND_5_GHZ_WITH_DFS = 6;
    public static final int WIFI_BAND_BOTH = 3;
    public static final int WIFI_BAND_BOTH_WITH_DFS = 7;
    public static final int WIFI_BAND_UNSPECIFIED = 0;
    private AsyncChannel mAsyncChannel;
    private Context mContext;
    public String mCurrentScanKeys = "";
    private final Handler mInternalHandler;
    private int mListenerKey = 1;
    private final SparseArray mListenerMap = new SparseArray();
    private final Object mListenerMapLock = new Object();
    private IWifiScanner mService;
    private boolean mVerboseLoggingEnabled = false;

    @SystemApi
    public interface ActionListener {
        void onFailure(int i, String str);

        void onSuccess();
    }

    @Deprecated
    public static class BssidInfo {
        public String bssid;
        public int frequencyHint;
        public int high;
        public int low;
    }

    public static class ChannelSpec {
        public int dwellTimeMS = 0;
        public int frequency;
        public boolean passive = false;

        public ChannelSpec(int frequency) {
            this.frequency = frequency;
        }
    }

    @Deprecated
    public interface BssidListener extends ActionListener {
        void onFound(ScanResult[] scanResultArr);

        void onLost(ScanResult[] scanResultArr);
    }

    @SystemApi
    @Deprecated
    public static class HotlistSettings implements Parcelable {
        public static final Creator<HotlistSettings> CREATOR = new Creator<HotlistSettings>() {
            public HotlistSettings createFromParcel(Parcel in) {
                return new HotlistSettings();
            }

            public HotlistSettings[] newArray(int size) {
                return new HotlistSettings[size];
            }
        };
        public int apLostThreshold;
        public BssidInfo[] bssidInfos;

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
        }
    }

    public static class OperationResult implements Parcelable {
        public static final Creator<OperationResult> CREATOR = new Creator<OperationResult>() {
            public OperationResult createFromParcel(Parcel in) {
                return new OperationResult(in.readInt(), in.readString());
            }

            public OperationResult[] newArray(int size) {
                return new OperationResult[size];
            }
        };
        public String description;
        public int reason;

        public OperationResult(int reason, String description) {
            this.reason = reason;
            this.description = description;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.reason);
            dest.writeString(this.description);
        }
    }

    public static class ParcelableScanData implements Parcelable {
        public static final Creator<ParcelableScanData> CREATOR = new Creator<ParcelableScanData>() {
            public ParcelableScanData createFromParcel(Parcel in) {
                int n = in.readInt();
                ScanData[] results = new ScanData[n];
                for (int i = 0; i < n; i++) {
                    results[i] = (ScanData) ScanData.CREATOR.createFromParcel(in);
                }
                return new ParcelableScanData(results);
            }

            public ParcelableScanData[] newArray(int size) {
                return new ParcelableScanData[size];
            }
        };
        public ScanData[] mResults;

        public ParcelableScanData(ScanData[] results) {
            this.mResults = results;
        }

        public ScanData[] getResults() {
            return this.mResults;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i = 0;
            if (this.mResults != null) {
                dest.writeInt(this.mResults.length);
                while (true) {
                    int i2 = i;
                    if (i2 < this.mResults.length) {
                        this.mResults[i2].writeToParcel(dest, flags);
                        i = i2 + 1;
                    } else {
                        return;
                    }
                }
            }
            dest.writeInt(0);
        }
    }

    public static class ParcelableScanResults implements Parcelable {
        public static final Creator<ParcelableScanResults> CREATOR = new Creator<ParcelableScanResults>() {
            public ParcelableScanResults createFromParcel(Parcel in) {
                int n = in.readInt();
                ScanResult[] results = new ScanResult[n];
                for (int i = 0; i < n; i++) {
                    results[i] = (ScanResult) ScanResult.CREATOR.createFromParcel(in);
                }
                return new ParcelableScanResults(results);
            }

            public ParcelableScanResults[] newArray(int size) {
                return new ParcelableScanResults[size];
            }
        };
        public ScanResult[] mResults;

        public ParcelableScanResults(ScanResult[] results) {
            this.mResults = results;
        }

        public ScanResult[] getResults() {
            return this.mResults;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i = 0;
            if (this.mResults != null) {
                dest.writeInt(this.mResults.length);
                while (true) {
                    int i2 = i;
                    if (i2 < this.mResults.length) {
                        this.mResults[i2].writeToParcel(dest, flags);
                        i = i2 + 1;
                    } else {
                        return;
                    }
                }
            }
            dest.writeInt(0);
        }
    }

    public static class PnoSettings implements Parcelable {
        public static final Creator<PnoSettings> CREATOR = new Creator<PnoSettings>() {
            public PnoSettings createFromParcel(Parcel in) {
                PnoSettings settings = new PnoSettings();
                int i = 0;
                boolean z = true;
                if (in.readInt() != 1) {
                    z = false;
                }
                settings.isConnected = z;
                settings.min5GHzRssi = in.readInt();
                settings.min24GHzRssi = in.readInt();
                settings.initialScoreMax = in.readInt();
                settings.currentConnectionBonus = in.readInt();
                settings.sameNetworkBonus = in.readInt();
                settings.secureBonus = in.readInt();
                settings.band5GHzBonus = in.readInt();
                int numNetworks = in.readInt();
                settings.networkList = new PnoNetwork[numNetworks];
                while (i < numNetworks) {
                    PnoNetwork network = new PnoNetwork(in.readString());
                    network.flags = in.readByte();
                    network.authBitField = in.readByte();
                    settings.networkList[i] = network;
                    i++;
                }
                return settings;
            }

            public PnoSettings[] newArray(int size) {
                return new PnoSettings[size];
            }
        };
        public int band5GHzBonus;
        public int currentConnectionBonus;
        public int initialScoreMax;
        public boolean isConnected;
        public int min24GHzRssi;
        public int min5GHzRssi;
        public PnoNetwork[] networkList;
        public int sameNetworkBonus;
        public int secureBonus;

        public static class PnoNetwork {
            public static final byte AUTH_CODE_EAPOL = (byte) 4;
            public static final byte AUTH_CODE_OPEN = (byte) 1;
            public static final byte AUTH_CODE_PSK = (byte) 2;
            public static final byte FLAG_A_BAND = (byte) 2;
            public static final byte FLAG_DIRECTED_SCAN = (byte) 1;
            public static final byte FLAG_G_BAND = (byte) 4;
            public static final byte FLAG_SAME_NETWORK = (byte) 16;
            public static final byte FLAG_STRICT_MATCH = (byte) 8;
            public byte authBitField = (byte) 0;
            public byte flags = (byte) 0;
            public String ssid;

            public PnoNetwork(String ssid) {
                this.ssid = ssid;
            }
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.isConnected);
            dest.writeInt(this.min5GHzRssi);
            dest.writeInt(this.min24GHzRssi);
            dest.writeInt(this.initialScoreMax);
            dest.writeInt(this.currentConnectionBonus);
            dest.writeInt(this.sameNetworkBonus);
            dest.writeInt(this.secureBonus);
            dest.writeInt(this.band5GHzBonus);
            int i = 0;
            if (this.networkList != null) {
                dest.writeInt(this.networkList.length);
                while (true) {
                    int i2 = i;
                    if (i2 < this.networkList.length) {
                        dest.writeString(this.networkList[i2].ssid);
                        dest.writeByte(this.networkList[i2].flags);
                        dest.writeByte(this.networkList[i2].authBitField);
                        i = i2 + 1;
                    } else {
                        return;
                    }
                }
            }
            dest.writeInt(0);
        }
    }

    public static class ScanData implements Parcelable {
        public static final Creator<ScanData> CREATOR = new Creator<ScanData>() {
            public ScanData createFromParcel(Parcel in) {
                int id = in.readInt();
                int flags = in.readInt();
                int bucketsScanned = in.readInt();
                boolean allChannelsScanned = in.readInt() != 0;
                int n = in.readInt();
                ScanResult[] results = new ScanResult[n];
                for (int i = 0; i < n; i++) {
                    results[i] = (ScanResult) ScanResult.CREATOR.createFromParcel(in);
                }
                return new ScanData(id, flags, bucketsScanned, allChannelsScanned, results, in.readInt() != 0);
            }

            public ScanData[] newArray(int size) {
                return new ScanData[size];
            }
        };
        private boolean mAllChannelsScanned;
        private int mBucketsScanned;
        private int mFlags;
        private int mId;
        private boolean mIsHiddenScanResult;
        private ScanResult[] mResults;

        ScanData() {
        }

        public ScanData(int id, int flags, ScanResult[] results) {
            this.mId = id;
            this.mFlags = flags;
            this.mResults = results;
        }

        public ScanData(int id, int flags, int bucketsScanned, boolean allChannelsScanned, ScanResult[] results) {
            this.mId = id;
            this.mFlags = flags;
            this.mBucketsScanned = bucketsScanned;
            this.mAllChannelsScanned = allChannelsScanned;
            this.mResults = results;
        }

        public ScanData(int id, int flags, int bucketsScanned, boolean allChannelsScanned, ScanResult[] results, boolean isHiddenScanResult) {
            this.mId = id;
            this.mFlags = flags;
            this.mBucketsScanned = bucketsScanned;
            this.mAllChannelsScanned = allChannelsScanned;
            this.mResults = results;
            this.mIsHiddenScanResult = isHiddenScanResult;
        }

        public ScanData(ScanData s) {
            this.mId = s.mId;
            this.mFlags = s.mFlags;
            this.mBucketsScanned = s.mBucketsScanned;
            this.mAllChannelsScanned = s.mAllChannelsScanned;
            this.mResults = new ScanResult[s.mResults.length];
            for (int i = 0; i < s.mResults.length; i++) {
                this.mResults[i] = new ScanResult(s.mResults[i]);
            }
            this.mIsHiddenScanResult = s.mIsHiddenScanResult;
        }

        public int getId() {
            return this.mId;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public int getBucketsScanned() {
            return this.mBucketsScanned;
        }

        public boolean isAllChannelsScanned() {
            return this.mAllChannelsScanned;
        }

        public ScanResult[] getResults() {
            return this.mResults;
        }

        public int describeContents() {
            return 0;
        }

        public boolean isHiddenScanResult() {
            return this.mIsHiddenScanResult;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i = 0;
            if (this.mResults != null) {
                dest.writeInt(this.mId);
                dest.writeInt(this.mFlags);
                dest.writeInt(this.mBucketsScanned);
                dest.writeInt(this.mAllChannelsScanned);
                dest.writeInt(this.mResults.length);
                while (true) {
                    int i2 = i;
                    if (i2 < this.mResults.length) {
                        this.mResults[i2].writeToParcel(dest, flags);
                        i = i2 + 1;
                    } else {
                        dest.writeInt(this.mIsHiddenScanResult);
                        return;
                    }
                }
            }
            dest.writeInt(0);
        }
    }

    public interface ScanListener extends ActionListener {
        void onFullResult(ScanResult scanResult);

        void onPeriodChanged(int i);

        void onResults(ScanData[] scanDataArr);
    }

    public static class ScanSettings implements Parcelable {
        public static final Creator<ScanSettings> CREATOR = new Creator<ScanSettings>() {
            public ScanSettings createFromParcel(Parcel in) {
                int i;
                ScanSettings settings = new ScanSettings();
                settings.band = in.readInt();
                settings.periodInMs = in.readInt();
                settings.reportEvents = in.readInt();
                settings.numBssidsPerScan = in.readInt();
                settings.maxScansToCache = in.readInt();
                settings.maxPeriodInMs = in.readInt();
                settings.stepCount = in.readInt();
                boolean z = false;
                settings.isPnoScan = in.readInt() == 1;
                settings.type = in.readInt();
                int num_channels = in.readInt();
                settings.channels = new ChannelSpec[num_channels];
                for (i = 0; i < num_channels; i++) {
                    ChannelSpec spec = new ChannelSpec(in.readInt());
                    spec.dwellTimeMS = in.readInt();
                    spec.passive = in.readInt() == 1;
                    settings.channels[i] = spec;
                }
                i = in.readInt();
                settings.hiddenNetworks = new HiddenNetwork[i];
                for (int i2 = 0; i2 < i; i2++) {
                    settings.hiddenNetworks[i2] = new HiddenNetwork(in.readString());
                }
                if (in.readInt() == 1) {
                    z = true;
                }
                settings.isHiddenSigleScan = z;
                return settings;
            }

            public ScanSettings[] newArray(int size) {
                return new ScanSettings[size];
            }
        };
        public int band;
        public ChannelSpec[] channels;
        public HiddenNetwork[] hiddenNetworks;
        public boolean isHiddenSigleScan;
        public boolean isPnoScan;
        public int maxPeriodInMs;
        public int maxScansToCache;
        public int numBssidsPerScan;
        public int periodInMs;
        public int reportEvents;
        public int stepCount;
        public int type = 0;

        public static class HiddenNetwork {
            public String ssid;

            public HiddenNetwork(String ssid) {
                this.ssid = ssid;
            }
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            dest.writeInt(this.band);
            dest.writeInt(this.periodInMs);
            dest.writeInt(this.reportEvents);
            dest.writeInt(this.numBssidsPerScan);
            dest.writeInt(this.maxScansToCache);
            dest.writeInt(this.maxPeriodInMs);
            dest.writeInt(this.stepCount);
            dest.writeInt(this.isPnoScan);
            dest.writeInt(this.type);
            int i2 = 0;
            if (this.channels != null) {
                dest.writeInt(this.channels.length);
                for (i = 0; i < this.channels.length; i++) {
                    dest.writeInt(this.channels[i].frequency);
                    dest.writeInt(this.channels[i].dwellTimeMS);
                    dest.writeInt(this.channels[i].passive);
                }
            } else {
                dest.writeInt(0);
            }
            if (this.hiddenNetworks != null) {
                dest.writeInt(this.hiddenNetworks.length);
                while (true) {
                    i = i2;
                    if (i >= this.hiddenNetworks.length) {
                        break;
                    }
                    dest.writeString(this.hiddenNetworks[i].ssid);
                    i2 = i + 1;
                }
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(this.isHiddenSigleScan);
        }
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 69634) {
                if (WifiScanner.this.mVerboseLoggingEnabled) {
                    Log.i(WifiScanLog.TAG, "ServiceHandler.CMD_CHANNEL_FULLY_CONNECTED");
                }
            } else if (i != 69636) {
                Object listener = WifiScanner.this.getListener(msg.arg2);
                String str;
                if (listener == null) {
                    str = WifiScanLog.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("invalid listener key = ");
                    stringBuilder.append(msg.arg2);
                    stringBuilder.append(", mCurrentScanKeys ");
                    stringBuilder.append(WifiScanner.this.mCurrentScanKeys);
                    stringBuilder.append(", msg:");
                    stringBuilder.append(msg.what);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                if (WifiScanner.this.mVerboseLoggingEnabled || (msg.arg2 == 1 && msg.what == WifiScanner.CMD_SCAN_RESULT)) {
                    str = WifiScanLog.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("listener key = ");
                    stringBuilder2.append(msg.arg2);
                    stringBuilder2.append(", mCurrentScanKeys ");
                    stringBuilder2.append(WifiScanner.this.mCurrentScanKeys);
                    stringBuilder2.append(", msg:");
                    stringBuilder2.append(msg.what);
                    Log.d(str, stringBuilder2.toString());
                }
                switch (msg.what) {
                    case WifiScanner.CMD_SCAN_RESULT /*159749*/:
                        ((ScanListener) listener).onResults(((ParcelableScanData) msg.obj).getResults());
                        return;
                    case WifiScanner.CMD_OP_SUCCEEDED /*159761*/:
                        ((ActionListener) listener).onSuccess();
                        break;
                    case WifiScanner.CMD_OP_FAILED /*159762*/:
                        str = new StringBuilder();
                        str.append("Key#");
                        str.append(msg.arg2);
                        str.append(SettingsStringUtil.DELIMITER);
                        WifiScanner.this.localLog(str.toString(), WifiScanLog.EVENT_KEY3, "received msg CMD_OP_FAILED in WifiScanner.", null);
                        OperationResult result = msg.obj;
                        ((ActionListener) listener).onFailure(result.reason, result.description);
                        WifiScanner.this.removeListener(msg.arg2);
                        break;
                    case WifiScanner.CMD_FULL_SCAN_RESULT /*159764*/:
                        ((ScanListener) listener).onFullResult(msg.obj);
                        return;
                    case WifiScanner.CMD_SINGLE_SCAN_COMPLETED /*159767*/:
                        str = new StringBuilder();
                        str.append("Key#");
                        str.append(msg.arg2);
                        str.append(SettingsStringUtil.DELIMITER);
                        WifiScanner.this.localLog(str.toString(), WifiScanLog.EVENT_KEY4, "received msg CMD_SINGLE_SCAN_COMPLETED in WifiScanner.", null);
                        WifiScanner.this.removeListener(msg.arg2);
                        break;
                    case WifiScanner.CMD_PNO_NETWORK_FOUND /*159770*/:
                        ((PnoScanListener) listener).onPnoNetworkFound(((ParcelableScanResults) msg.obj).getResults());
                        return;
                    case WifiScanner.CMD_SET_SINGLE_SCAN_KEYS /*159774*/:
                        WifiScanner.this.mCurrentScanKeys = (String) msg.obj;
                        return;
                    default:
                        return;
                }
            } else {
                Log.e(WifiScanner.TAG, "Channel connection lost");
                WifiScanner.this.mAsyncChannel = null;
                getLooper().quit();
            }
        }
    }

    @Deprecated
    public interface WifiChangeListener extends ActionListener {
        void onChanging(ScanResult[] scanResultArr);

        void onQuiescence(ScanResult[] scanResultArr);
    }

    @SystemApi
    @Deprecated
    public static class WifiChangeSettings implements Parcelable {
        public static final Creator<WifiChangeSettings> CREATOR = new Creator<WifiChangeSettings>() {
            public WifiChangeSettings createFromParcel(Parcel in) {
                return new WifiChangeSettings();
            }

            public WifiChangeSettings[] newArray(int size) {
                return new WifiChangeSettings[size];
            }
        };
        public BssidInfo[] bssidInfos;
        public int lostApSampleSize;
        public int minApsBreachingThreshold;
        public int periodInMs;
        public int rssiSampleSize;
        public int unchangedSampleSize;

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
        }
    }

    public interface PnoScanListener extends ScanListener {
        void onPnoNetworkFound(ScanResult[] scanResultArr);
    }

    public List<Integer> getAvailableChannels(int band) {
        try {
            return this.mService.getAvailableChannels(band).getIntegerArrayList(GET_AVAILABLE_CHANNELS_EXTRA);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            Log.e(TAG, "getAvailableChannels NullPointerException ");
            return null;
        }
    }

    public void registerScanListener(ScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key != 0) {
            validateChannel();
            this.mAsyncChannel.sendMessage(CMD_REGISTER_SCAN_LISTENER, 0, key);
        }
    }

    public void deregisterScanListener(ScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener((Object) listener);
        if (key != 0) {
            validateChannel();
            this.mAsyncChannel.sendMessage(CMD_DEREGISTER_SCAN_LISTENER, 0, key);
        }
    }

    public void startBackgroundScan(ScanSettings settings, ScanListener listener) {
        startBackgroundScan(settings, listener, null);
    }

    public void startBackgroundScan(ScanSettings settings, ScanListener listener, WorkSource workSource) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key != 0) {
            validateChannel();
            Bundle scanParams = new Bundle();
            scanParams.putParcelable("ScanSettings", settings);
            scanParams.putParcelable(SCAN_PARAMS_WORK_SOURCE_KEY, workSource);
            this.mAsyncChannel.sendMessage(CMD_START_BACKGROUND_SCAN, 0, key, scanParams);
        }
    }

    public void stopBackgroundScan(ScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener((Object) listener);
        if (key != 0) {
            validateChannel();
            this.mAsyncChannel.sendMessage(CMD_STOP_BACKGROUND_SCAN, 0, key);
        }
    }

    public boolean getScanResults() {
        validateChannel();
        if (this.mAsyncChannel.sendMessageSynchronously(CMD_GET_SCAN_RESULTS, 0).what == CMD_OP_SUCCEEDED) {
            return true;
        }
        return false;
    }

    public void startScan(ScanSettings settings, ScanListener listener) {
        startScan(settings, listener, null);
    }

    public void startScan(ScanSettings settings, ScanListener listener, WorkSource workSource) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key != 0) {
            validateChannel();
            Bundle scanParams = new Bundle();
            scanParams.putParcelable("ScanSettings", settings);
            scanParams.putParcelable(SCAN_PARAMS_WORK_SOURCE_KEY, workSource);
            this.mAsyncChannel.sendMessage(CMD_START_SINGLE_SCAN, 0, key, scanParams);
        }
    }

    public void stopScan(ScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener((Object) listener);
        if (key != 0) {
            validateChannel();
            this.mAsyncChannel.sendMessage(CMD_STOP_SINGLE_SCAN, 0, key);
        }
    }

    public List<ScanResult> getSingleScanResults() {
        validateChannel();
        for (int i = 0; i < 3; i++) {
            Message reply = this.mAsyncChannel.sendMessageSynchronously(CMD_GET_SINGLE_SCAN_RESULTS, 0);
            if (reply.what == CMD_OP_SUCCEEDED) {
                return Arrays.asList(((ParcelableScanResults) reply.obj).getResults());
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error retrieving SingleScan results: ");
            stringBuilder.append(reply.obj);
            stringBuilder.append(", what =");
            stringBuilder.append(reply.what);
            Log.e(str, stringBuilder.toString());
        }
        return new ArrayList();
    }

    private void startPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, int key) {
        Bundle pnoParams = new Bundle();
        scanSettings.isPnoScan = true;
        pnoParams.putParcelable("ScanSettings", scanSettings);
        pnoParams.putParcelable(PNO_PARAMS_PNO_SETTINGS_KEY, pnoSettings);
        this.mAsyncChannel.sendMessage(CMD_START_PNO_SCAN, 0, key, pnoParams);
    }

    public void startConnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, PnoScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        Preconditions.checkNotNull(pnoSettings, "pnoSettings cannot be null");
        int key = addListener(listener);
        if (key != 0) {
            validateChannel();
            pnoSettings.isConnected = true;
            startPnoScan(scanSettings, pnoSettings, key);
        }
    }

    public void startDisconnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, PnoScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        Preconditions.checkNotNull(pnoSettings, "pnoSettings cannot be null");
        int key = addListener(listener);
        if (key != 0) {
            validateChannel();
            pnoSettings.isConnected = false;
            startPnoScan(scanSettings, pnoSettings, key);
        }
    }

    public void stopPnoScan(ScanListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener((Object) listener);
        if (key != 0) {
            validateChannel();
            this.mAsyncChannel.sendMessage(CMD_STOP_PNO_SCAN, 0, key);
        }
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void configureWifiChange(int rssiSampleSize, int lostApSampleSize, int unchangedSampleSize, int minApsBreachingThreshold, int periodInMs, BssidInfo[] bssidInfos) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void startTrackingWifiChange(WifiChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void stopTrackingWifiChange(WifiChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @SystemApi
    @Deprecated
    public void configureWifiChange(WifiChangeSettings settings) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void startTrackingBssids(BssidInfo[] bssidInfos, int apLostThreshold, BssidListener listener) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void stopTrackingBssids(BssidListener listener) {
        throw new UnsupportedOperationException();
    }

    public WifiScanner(Context context, IWifiScanner service, Looper looper) {
        this.mContext = context;
        this.mService = service;
        try {
            Messenger messenger = this.mService.getMessenger();
            if (messenger != null) {
                this.mAsyncChannel = new AsyncChannel();
                this.mInternalHandler = new ServiceHandler(looper);
                this.mAsyncChannel.connectSync(this.mContext, this.mInternalHandler, messenger);
                this.mAsyncChannel.sendMessage(69633);
                return;
            }
            throw new IllegalStateException("getMessenger() returned null!  This is invalid.");
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void validateChannel() {
        if (this.mAsyncChannel == null) {
            throw new IllegalStateException("No permission to access and change wifi or a bad initialization");
        }
    }

    private int addListener(ActionListener listener) {
        synchronized (this.mListenerMapLock) {
            boolean keyExists = getListenerKey(listener) != 0;
            int key = putListener(listener);
            if (keyExists) {
                Message.obtain(this.mInternalHandler, CMD_OP_FAILED, 0, key, new OperationResult(-5, "Outstanding request with same key not stopped yet")).sendToTarget();
                return 0;
            }
            return key;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0024, code skipped:
            r8.mListenerMap.put(r2, r9);
            r3 = r9.getClass().getSimpleName();
     */
    /* JADX WARNING: Missing block: B:14:0x0039, code skipped:
            if (r9.getClass().isAnonymousClass() == false) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:16:0x0043, code skipped:
            if (r9.getClass().getEnclosingMethod() == null) goto L_0x0077;
     */
    /* JADX WARNING: Missing block: B:17:0x0045, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append(r9.getClass().getEnclosingMethod().getDeclaringClass().getSimpleName());
            r4.append(".");
            r4.append(r9.getClass().getEnclosingMethod().getName());
            r3 = r4.toString();
     */
    /* JADX WARNING: Missing block: B:19:0x007f, code skipped:
            if (r9.getClass().getEnclosingConstructor() == null) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:20:0x0081, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append(r9.getClass().getEnclosingConstructor().getDeclaringClass().getSimpleName());
            r4.append(".");
            r4.append(r9.getClass().getEnclosingConstructor().getName());
     */
    /* JADX WARNING: Missing block: B:21:0x00b1, code skipped:
            r3 = r4.toString();
     */
    /* JADX WARNING: Missing block: B:22:0x00b3, code skipped:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("NullpointerException ");
            r6.append(r3);
            android.util.Log.i(r5, r6.toString(), r4);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int putListener(Object listener) {
        int key;
        if (listener == null || this.mListenerMap.size() >= 99) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            while (true) {
                key = this.mListenerKey;
                this.mListenerKey = key + 1;
                key %= 100;
                if (key != 0 && this.mListenerMap.get(key) == null) {
                    break;
                }
            }
        }
        return key;
        Log.i(WifiScanLog.TAG, String.format("%s start new Scan by --%s ", new Object[]{getScanKey(key), listenerKey}));
        return key;
    }

    private Object getListener(int key) {
        if (key == 0) {
            return null;
        }
        Object listener;
        synchronized (this.mListenerMapLock) {
            listener = this.mListenerMap.get(key);
        }
        return listener;
    }

    private int getListenerKey(Object listener) {
        if (listener == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            int index = this.mListenerMap.indexOfValue(listener);
            if (index == -1) {
                return 0;
            }
            int keyAt = this.mListenerMap.keyAt(index);
            return keyAt;
        }
    }

    private Object removeListener(int key) {
        if (key == 0) {
            return null;
        }
        Object listener;
        synchronized (this.mListenerMapLock) {
            listener = this.mListenerMap.get(key);
            this.mListenerMap.remove(key);
            if (listener != null) {
                localLog(getScanKey(key), "1", "this scan was completed!", null);
            }
            WifiScanLog.getDefault().flush();
        }
        return listener;
    }

    private int removeListener(Object listener) {
        int key = getListenerKey(listener);
        if (key == 0) {
            Log.e(TAG, "listener cannot be found");
            return key;
        }
        synchronized (this.mListenerMapLock) {
            this.mListenerMap.remove(key);
            localLog(getScanKey(key), WifiScanLog.EVENT_KEY2, "this scan was completed!", null);
            WifiScanLog.getDefault().flush();
        }
        return key;
    }

    private void localLog(String scanKey, String eventKey, String log, Object... params) {
        WifiScanLog.getDefault().addEvent(scanKey, eventKey, log, params);
    }

    public int getScanKey(Object listener) {
        return getListenerKey(listener);
    }

    public static String getScanKey(int key) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Key#");
        stringBuilder.append(key);
        stringBuilder.append(SettingsStringUtil.DELIMITER);
        return stringBuilder.toString();
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }
}
