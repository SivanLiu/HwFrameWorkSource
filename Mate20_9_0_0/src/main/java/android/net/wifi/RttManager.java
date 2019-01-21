package android.net.wifi;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.net.wifi.rtt.RangingRequest.Builder;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;

@SystemApi
@Deprecated
public class RttManager {
    public static final int BASE = 160256;
    public static final int CMD_OP_ABORTED = 160260;
    public static final int CMD_OP_DISABLE_RESPONDER = 160262;
    public static final int CMD_OP_ENABLE_RESPONDER = 160261;
    public static final int CMD_OP_ENALBE_RESPONDER_FAILED = 160264;
    public static final int CMD_OP_ENALBE_RESPONDER_SUCCEEDED = 160263;
    public static final int CMD_OP_FAILED = 160258;
    public static final int CMD_OP_REG_BINDER = 160265;
    public static final int CMD_OP_START_RANGING = 160256;
    public static final int CMD_OP_STOP_RANGING = 160257;
    public static final int CMD_OP_SUCCEEDED = 160259;
    private static final boolean DBG = false;
    public static final String DESCRIPTION_KEY = "android.net.wifi.RttManager.Description";
    public static final int PREAMBLE_HT = 2;
    public static final int PREAMBLE_LEGACY = 1;
    public static final int PREAMBLE_VHT = 4;
    public static final int REASON_INITIATOR_NOT_ALLOWED_WHEN_RESPONDER_ON = -6;
    public static final int REASON_INVALID_LISTENER = -3;
    public static final int REASON_INVALID_REQUEST = -4;
    public static final int REASON_NOT_AVAILABLE = -2;
    public static final int REASON_PERMISSION_DENIED = -5;
    public static final int REASON_UNSPECIFIED = -1;
    public static final int RTT_BW_10_SUPPORT = 2;
    public static final int RTT_BW_160_SUPPORT = 32;
    public static final int RTT_BW_20_SUPPORT = 4;
    public static final int RTT_BW_40_SUPPORT = 8;
    public static final int RTT_BW_5_SUPPORT = 1;
    public static final int RTT_BW_80_SUPPORT = 16;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_10 = 6;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_160 = 3;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_20 = 0;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_40 = 1;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_5 = 5;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_80 = 2;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_80P80 = 4;
    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_UNSPECIFIED = -1;
    public static final int RTT_PEER_NAN = 5;
    public static final int RTT_PEER_P2P_CLIENT = 4;
    public static final int RTT_PEER_P2P_GO = 3;
    public static final int RTT_PEER_TYPE_AP = 1;
    public static final int RTT_PEER_TYPE_STA = 2;
    @Deprecated
    public static final int RTT_PEER_TYPE_UNSPECIFIED = 0;
    public static final int RTT_STATUS_ABORTED = 8;
    public static final int RTT_STATUS_FAILURE = 1;
    public static final int RTT_STATUS_FAIL_AP_ON_DIFF_CHANNEL = 6;
    public static final int RTT_STATUS_FAIL_BUSY_TRY_LATER = 12;
    public static final int RTT_STATUS_FAIL_FTM_PARAM_OVERRIDE = 15;
    public static final int RTT_STATUS_FAIL_INVALID_TS = 9;
    public static final int RTT_STATUS_FAIL_NOT_SCHEDULED_YET = 4;
    public static final int RTT_STATUS_FAIL_NO_CAPABILITY = 7;
    public static final int RTT_STATUS_FAIL_NO_RSP = 2;
    public static final int RTT_STATUS_FAIL_PROTOCOL = 10;
    public static final int RTT_STATUS_FAIL_REJECTED = 3;
    public static final int RTT_STATUS_FAIL_SCHEDULE = 11;
    public static final int RTT_STATUS_FAIL_TM_TIMEOUT = 5;
    public static final int RTT_STATUS_INVALID_REQ = 13;
    public static final int RTT_STATUS_NO_WIFI = 14;
    public static final int RTT_STATUS_SUCCESS = 0;
    @Deprecated
    public static final int RTT_TYPE_11_MC = 4;
    @Deprecated
    public static final int RTT_TYPE_11_V = 2;
    public static final int RTT_TYPE_ONE_SIDED = 1;
    public static final int RTT_TYPE_TWO_SIDED = 2;
    @Deprecated
    public static final int RTT_TYPE_UNSPECIFIED = 0;
    private static final String TAG = "RttManager";
    private final Context mContext;
    private final WifiRttManager mNewService;
    private RttCapabilities mRttCapabilities = new RttCapabilities();

    @Deprecated
    public class Capabilities {
        public int supportedPeerType;
        public int supportedType;
    }

    @Deprecated
    public static abstract class ResponderCallback {
        public abstract void onResponderEnableFailure(int i);

        public abstract void onResponderEnabled(ResponderConfig responderConfig);
    }

    @Deprecated
    public interface RttListener {
        void onAborted();

        void onFailure(int i, String str);

        void onSuccess(RttResult[] rttResultArr);
    }

    @Deprecated
    public static class RttParams {
        public boolean LCIRequest;
        public boolean LCRRequest;
        public int bandwidth = 4;
        public String bssid;
        public int burstTimeout = 15;
        public int centerFreq0;
        public int centerFreq1;
        public int channelWidth;
        public int deviceType = 1;
        public int frequency;
        public int interval;
        public int numRetriesPerFTMR = 0;
        public int numRetriesPerMeasurementFrame = 0;
        public int numSamplesPerBurst = 8;
        @Deprecated
        public int num_retries;
        @Deprecated
        public int num_samples;
        public int numberBurst = 0;
        public int preamble = 2;
        public int requestType = 1;
        public boolean secure;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deviceType=");
            stringBuilder.append(this.deviceType);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", requestType=");
            stringBuilder.append(this.requestType);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", secure=");
            stringBuilder.append(this.secure);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", bssid=");
            stringBuilder.append(this.bssid);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", frequency=");
            stringBuilder.append(this.frequency);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", channelWidth=");
            stringBuilder.append(this.channelWidth);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", centerFreq0=");
            stringBuilder.append(this.centerFreq0);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", centerFreq1=");
            stringBuilder.append(this.centerFreq1);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", num_samples=");
            stringBuilder.append(this.num_samples);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", num_retries=");
            stringBuilder.append(this.num_retries);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", numberBurst=");
            stringBuilder.append(this.numberBurst);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", interval=");
            stringBuilder.append(this.interval);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", numSamplesPerBurst=");
            stringBuilder.append(this.numSamplesPerBurst);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", numRetriesPerMeasurementFrame=");
            stringBuilder.append(this.numRetriesPerMeasurementFrame);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", numRetriesPerFTMR=");
            stringBuilder.append(this.numRetriesPerFTMR);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", LCIRequest=");
            stringBuilder.append(this.LCIRequest);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", LCRRequest=");
            stringBuilder.append(this.LCRRequest);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", burstTimeout=");
            stringBuilder.append(this.burstTimeout);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", preamble=");
            stringBuilder.append(this.preamble);
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(", bandwidth=");
            stringBuilder.append(this.bandwidth);
            sb.append(stringBuilder.toString());
            return sb.toString();
        }
    }

    @Deprecated
    public static class RttResult {
        public WifiInformationElement LCI;
        public WifiInformationElement LCR;
        public String bssid;
        public int burstDuration;
        public int burstNumber;
        public int distance;
        public int distanceSpread;
        public int distanceStandardDeviation;
        @Deprecated
        public int distance_cm;
        @Deprecated
        public int distance_sd_cm;
        @Deprecated
        public int distance_spread_cm;
        public int frameNumberPerBurstPeer;
        public int measurementFrameNumber;
        public int measurementType;
        public int negotiatedBurstNum;
        @Deprecated
        public int requestType;
        public int retryAfterDuration;
        public int rssi;
        public int rssiSpread;
        @Deprecated
        public int rssi_spread;
        public long rtt;
        public long rttSpread;
        public long rttStandardDeviation;
        @Deprecated
        public long rtt_ns;
        @Deprecated
        public long rtt_sd_ns;
        @Deprecated
        public long rtt_spread_ns;
        public int rxRate;
        public boolean secure;
        public int status;
        public int successMeasurementFrameNumber;
        public long ts;
        public int txRate;
        @Deprecated
        public int tx_rate;
    }

    @Deprecated
    public static class WifiInformationElement {
        public byte[] data;
        public byte id;
    }

    @Deprecated
    public static class ParcelableRttParams implements Parcelable {
        public static final Creator<ParcelableRttParams> CREATOR = new Creator<ParcelableRttParams>() {
            public ParcelableRttParams createFromParcel(Parcel in) {
                int num = in.readInt();
                RttParams[] params = new RttParams[num];
                for (int i = 0; i < num; i++) {
                    params[i] = new RttParams();
                    params[i].deviceType = in.readInt();
                    params[i].requestType = in.readInt();
                    boolean z = true;
                    params[i].secure = in.readByte() != (byte) 0;
                    params[i].bssid = in.readString();
                    params[i].channelWidth = in.readInt();
                    params[i].frequency = in.readInt();
                    params[i].centerFreq0 = in.readInt();
                    params[i].centerFreq1 = in.readInt();
                    params[i].numberBurst = in.readInt();
                    params[i].interval = in.readInt();
                    params[i].numSamplesPerBurst = in.readInt();
                    params[i].numRetriesPerMeasurementFrame = in.readInt();
                    params[i].numRetriesPerFTMR = in.readInt();
                    params[i].LCIRequest = in.readInt() == 1;
                    RttParams rttParams = params[i];
                    if (in.readInt() != 1) {
                        z = false;
                    }
                    rttParams.LCRRequest = z;
                    params[i].burstTimeout = in.readInt();
                    params[i].preamble = in.readInt();
                    params[i].bandwidth = in.readInt();
                }
                return new ParcelableRttParams(params);
            }

            public ParcelableRttParams[] newArray(int size) {
                return new ParcelableRttParams[size];
            }
        };
        public RttParams[] mParams;

        @VisibleForTesting
        public ParcelableRttParams(RttParams[] params) {
            this.mParams = params == null ? new RttParams[0] : params;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mParams.length);
            for (RttParams params : this.mParams) {
                dest.writeInt(params.deviceType);
                dest.writeInt(params.requestType);
                dest.writeByte(params.secure);
                dest.writeString(params.bssid);
                dest.writeInt(params.channelWidth);
                dest.writeInt(params.frequency);
                dest.writeInt(params.centerFreq0);
                dest.writeInt(params.centerFreq1);
                dest.writeInt(params.numberBurst);
                dest.writeInt(params.interval);
                dest.writeInt(params.numSamplesPerBurst);
                dest.writeInt(params.numRetriesPerMeasurementFrame);
                dest.writeInt(params.numRetriesPerFTMR);
                dest.writeInt(params.LCIRequest);
                dest.writeInt(params.LCRRequest);
                dest.writeInt(params.burstTimeout);
                dest.writeInt(params.preamble);
                dest.writeInt(params.bandwidth);
            }
        }
    }

    @Deprecated
    public static class ParcelableRttResults implements Parcelable {
        public static final Creator<ParcelableRttResults> CREATOR = new Creator<ParcelableRttResults>() {
            public ParcelableRttResults createFromParcel(Parcel in) {
                int num = in.readInt();
                if (num == 0) {
                    return new ParcelableRttResults(null);
                }
                RttResult[] results = new RttResult[num];
                for (int i = 0; i < num; i++) {
                    results[i] = new RttResult();
                    results[i].bssid = in.readString();
                    results[i].burstNumber = in.readInt();
                    results[i].measurementFrameNumber = in.readInt();
                    results[i].successMeasurementFrameNumber = in.readInt();
                    results[i].frameNumberPerBurstPeer = in.readInt();
                    results[i].status = in.readInt();
                    results[i].measurementType = in.readInt();
                    results[i].retryAfterDuration = in.readInt();
                    results[i].ts = in.readLong();
                    results[i].rssi = in.readInt();
                    results[i].rssiSpread = in.readInt();
                    results[i].txRate = in.readInt();
                    results[i].rtt = in.readLong();
                    results[i].rttStandardDeviation = in.readLong();
                    results[i].rttSpread = in.readLong();
                    results[i].distance = in.readInt();
                    results[i].distanceStandardDeviation = in.readInt();
                    results[i].distanceSpread = in.readInt();
                    results[i].burstDuration = in.readInt();
                    results[i].negotiatedBurstNum = in.readInt();
                    results[i].LCI = new WifiInformationElement();
                    results[i].LCI.id = in.readByte();
                    if (results[i].LCI.id != (byte) -1) {
                        results[i].LCI.data = new byte[in.readByte()];
                        in.readByteArray(results[i].LCI.data);
                    }
                    results[i].LCR = new WifiInformationElement();
                    results[i].LCR.id = in.readByte();
                    if (results[i].LCR.id != (byte) -1) {
                        results[i].LCR.data = new byte[in.readByte()];
                        in.readByteArray(results[i].LCR.data);
                    }
                    results[i].secure = in.readByte() != (byte) 0;
                }
                return new ParcelableRttResults(results);
            }

            public ParcelableRttResults[] newArray(int size) {
                return new ParcelableRttResults[size];
            }
        };
        public RttResult[] mResults;

        public ParcelableRttResults(RttResult[] results) {
            this.mResults = results;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.mResults.length; i++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(i);
                stringBuilder.append("]: ");
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("bssid=");
                stringBuilder.append(this.mResults[i].bssid);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", burstNumber=");
                stringBuilder.append(this.mResults[i].burstNumber);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", measurementFrameNumber=");
                stringBuilder.append(this.mResults[i].measurementFrameNumber);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", successMeasurementFrameNumber=");
                stringBuilder.append(this.mResults[i].successMeasurementFrameNumber);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", frameNumberPerBurstPeer=");
                stringBuilder.append(this.mResults[i].frameNumberPerBurstPeer);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", status=");
                stringBuilder.append(this.mResults[i].status);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", requestType=");
                stringBuilder.append(this.mResults[i].requestType);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", measurementType=");
                stringBuilder.append(this.mResults[i].measurementType);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", retryAfterDuration=");
                stringBuilder.append(this.mResults[i].retryAfterDuration);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", ts=");
                stringBuilder.append(this.mResults[i].ts);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rssi=");
                stringBuilder.append(this.mResults[i].rssi);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rssi_spread=");
                stringBuilder.append(this.mResults[i].rssi_spread);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rssiSpread=");
                stringBuilder.append(this.mResults[i].rssiSpread);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", tx_rate=");
                stringBuilder.append(this.mResults[i].tx_rate);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", txRate=");
                stringBuilder.append(this.mResults[i].txRate);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rxRate=");
                stringBuilder.append(this.mResults[i].rxRate);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rtt_ns=");
                stringBuilder.append(this.mResults[i].rtt_ns);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rtt=");
                stringBuilder.append(this.mResults[i].rtt);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rtt_sd_ns=");
                stringBuilder.append(this.mResults[i].rtt_sd_ns);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rttStandardDeviation=");
                stringBuilder.append(this.mResults[i].rttStandardDeviation);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rtt_spread_ns=");
                stringBuilder.append(this.mResults[i].rtt_spread_ns);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", rttSpread=");
                stringBuilder.append(this.mResults[i].rttSpread);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", distance_cm=");
                stringBuilder.append(this.mResults[i].distance_cm);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", distance=");
                stringBuilder.append(this.mResults[i].distance);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", distance_sd_cm=");
                stringBuilder.append(this.mResults[i].distance_sd_cm);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", distanceStandardDeviation=");
                stringBuilder.append(this.mResults[i].distanceStandardDeviation);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", distance_spread_cm=");
                stringBuilder.append(this.mResults[i].distance_spread_cm);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", distanceSpread=");
                stringBuilder.append(this.mResults[i].distanceSpread);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", burstDuration=");
                stringBuilder.append(this.mResults[i].burstDuration);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", negotiatedBurstNum=");
                stringBuilder.append(this.mResults[i].negotiatedBurstNum);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", LCI=");
                stringBuilder.append(this.mResults[i].LCI);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", LCR=");
                stringBuilder.append(this.mResults[i].LCR);
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", secure=");
                stringBuilder.append(this.mResults[i].secure);
                sb.append(stringBuilder.toString());
            }
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i = 0;
            if (this.mResults != null) {
                dest.writeInt(this.mResults.length);
                RttResult[] rttResultArr = this.mResults;
                int length = rttResultArr.length;
                while (i < length) {
                    RttResult result = rttResultArr[i];
                    dest.writeString(result.bssid);
                    dest.writeInt(result.burstNumber);
                    dest.writeInt(result.measurementFrameNumber);
                    dest.writeInt(result.successMeasurementFrameNumber);
                    dest.writeInt(result.frameNumberPerBurstPeer);
                    dest.writeInt(result.status);
                    dest.writeInt(result.measurementType);
                    dest.writeInt(result.retryAfterDuration);
                    dest.writeLong(result.ts);
                    dest.writeInt(result.rssi);
                    dest.writeInt(result.rssiSpread);
                    dest.writeInt(result.txRate);
                    dest.writeLong(result.rtt);
                    dest.writeLong(result.rttStandardDeviation);
                    dest.writeLong(result.rttSpread);
                    dest.writeInt(result.distance);
                    dest.writeInt(result.distanceStandardDeviation);
                    dest.writeInt(result.distanceSpread);
                    dest.writeInt(result.burstDuration);
                    dest.writeInt(result.negotiatedBurstNum);
                    dest.writeByte(result.LCI.id);
                    if (result.LCI.id != (byte) -1) {
                        dest.writeByte((byte) result.LCI.data.length);
                        dest.writeByteArray(result.LCI.data);
                    }
                    dest.writeByte(result.LCR.id);
                    if (result.LCR.id != (byte) -1) {
                        dest.writeByte((byte) result.LCR.data.length);
                        dest.writeByteArray(result.LCR.data);
                    }
                    dest.writeByte(result.secure);
                    i++;
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    @Deprecated
    public static class ResponderConfig implements Parcelable {
        public static final Creator<ResponderConfig> CREATOR = new Creator<ResponderConfig>() {
            public ResponderConfig createFromParcel(Parcel in) {
                ResponderConfig config = new ResponderConfig();
                config.macAddress = in.readString();
                config.frequency = in.readInt();
                config.centerFreq0 = in.readInt();
                config.centerFreq1 = in.readInt();
                config.channelWidth = in.readInt();
                config.preamble = in.readInt();
                return config;
            }

            public ResponderConfig[] newArray(int size) {
                return new ResponderConfig[size];
            }
        };
        public int centerFreq0;
        public int centerFreq1;
        public int channelWidth;
        public int frequency;
        public String macAddress = "";
        public int preamble;

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("macAddress = ");
            builder.append(this.macAddress);
            builder.append(" frequency = ");
            builder.append(this.frequency);
            builder.append(" centerFreq0 = ");
            builder.append(this.centerFreq0);
            builder.append(" centerFreq1 = ");
            builder.append(this.centerFreq1);
            builder.append(" channelWidth = ");
            builder.append(this.channelWidth);
            builder.append(" preamble = ");
            builder.append(this.preamble);
            return builder.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.macAddress);
            dest.writeInt(this.frequency);
            dest.writeInt(this.centerFreq0);
            dest.writeInt(this.centerFreq1);
            dest.writeInt(this.channelWidth);
            dest.writeInt(this.preamble);
        }
    }

    @Deprecated
    public static class RttCapabilities implements Parcelable {
        public static final Creator<RttCapabilities> CREATOR = new Creator<RttCapabilities>() {
            public RttCapabilities createFromParcel(Parcel in) {
                RttCapabilities capabilities = new RttCapabilities();
                boolean z = false;
                capabilities.oneSidedRttSupported = in.readInt() == 1;
                capabilities.twoSided11McRttSupported = in.readInt() == 1;
                capabilities.lciSupported = in.readInt() == 1;
                capabilities.lcrSupported = in.readInt() == 1;
                capabilities.preambleSupported = in.readInt();
                capabilities.bwSupported = in.readInt();
                capabilities.responderSupported = in.readInt() == 1;
                if (in.readInt() == 1) {
                    z = true;
                }
                capabilities.secureRttSupported = z;
                capabilities.mcVersion = in.readInt();
                return capabilities;
            }

            public RttCapabilities[] newArray(int size) {
                return new RttCapabilities[size];
            }
        };
        public int bwSupported;
        public boolean lciSupported;
        public boolean lcrSupported;
        public int mcVersion;
        public boolean oneSidedRttSupported;
        public int preambleSupported;
        public boolean responderSupported;
        public boolean secureRttSupported;
        @Deprecated
        public boolean supportedPeerType;
        @Deprecated
        public boolean supportedType;
        public boolean twoSided11McRttSupported;

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("oneSidedRtt ");
            sb.append(this.oneSidedRttSupported ? "is Supported. " : "is not supported. ");
            sb.append("twoSided11McRtt ");
            sb.append(this.twoSided11McRttSupported ? "is Supported. " : "is not supported. ");
            sb.append("lci ");
            sb.append(this.lciSupported ? "is Supported. " : "is not supported. ");
            sb.append("lcr ");
            sb.append(this.lcrSupported ? "is Supported. " : "is not supported. ");
            if ((this.preambleSupported & 1) != 0) {
                sb.append("Legacy ");
            }
            if ((this.preambleSupported & 2) != 0) {
                sb.append("HT ");
            }
            if ((this.preambleSupported & 4) != 0) {
                sb.append("VHT ");
            }
            sb.append("is supported. ");
            if ((this.bwSupported & 1) != 0) {
                sb.append("5 MHz ");
            }
            if ((this.bwSupported & 2) != 0) {
                sb.append("10 MHz ");
            }
            if ((this.bwSupported & 4) != 0) {
                sb.append("20 MHz ");
            }
            if ((this.bwSupported & 8) != 0) {
                sb.append("40 MHz ");
            }
            if ((this.bwSupported & 16) != 0) {
                sb.append("80 MHz ");
            }
            if ((this.bwSupported & 32) != 0) {
                sb.append("160 MHz ");
            }
            sb.append("is supported.");
            sb.append(" STA responder role is ");
            sb.append(this.responderSupported ? "supported" : "not supported");
            sb.append(" Secure RTT protocol is ");
            sb.append(this.secureRttSupported ? "supported" : "not supported");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" 11mc version is ");
            stringBuilder.append(this.mcVersion);
            sb.append(stringBuilder.toString());
            return sb.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.oneSidedRttSupported);
            dest.writeInt(this.twoSided11McRttSupported);
            dest.writeInt(this.lciSupported);
            dest.writeInt(this.lcrSupported);
            dest.writeInt(this.preambleSupported);
            dest.writeInt(this.bwSupported);
            dest.writeInt(this.responderSupported);
            dest.writeInt(this.secureRttSupported);
            dest.writeInt(this.mcVersion);
        }
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public Capabilities getCapabilities() {
        throw new UnsupportedOperationException("getCapabilities is not supported in the adaptation layer");
    }

    public RttCapabilities getRttCapabilities() {
        return this.mRttCapabilities;
    }

    public void startRanging(RttParams[] params, final RttListener listener) {
        String str;
        Log.i(TAG, "Send RTT request to RTT Service");
        if (this.mNewService.isAvailable()) {
            Builder builder = new Builder();
            for (RttParams rttParams : params) {
                if (rttParams.deviceType != 1) {
                    listener.onFailure(-4, "Only AP peers are supported");
                    return;
                }
                ScanResult reconstructed = new ScanResult();
                reconstructed.BSSID = rttParams.bssid;
                if (rttParams.requestType == 2) {
                    reconstructed.setFlag(2);
                }
                reconstructed.channelWidth = rttParams.channelWidth;
                reconstructed.frequency = rttParams.frequency;
                reconstructed.centerFreq0 = rttParams.centerFreq0;
                reconstructed.centerFreq1 = rttParams.centerFreq1;
                builder.addResponder(android.net.wifi.rtt.ResponderConfig.fromScanResult(reconstructed));
            }
            try {
                this.mNewService.startRanging(builder.build(), this.mContext.getMainExecutor(), new RangingResultCallback() {
                    public void onRangingFailure(int code) {
                        int localCode = -1;
                        if (code == 2) {
                            localCode = -2;
                        }
                        listener.onFailure(localCode, "");
                    }

                    public void onRangingResults(List<RangingResult> results) {
                        RttResult[] legacyResults = new RttResult[results.size()];
                        int i = 0;
                        for (RangingResult result : results) {
                            legacyResults[i] = new RttResult();
                            legacyResults[i].status = result.getStatus();
                            legacyResults[i].bssid = result.getMacAddress().toString();
                            if (result.getStatus() == 0) {
                                legacyResults[i].distance = result.getDistanceMm() / 10;
                                legacyResults[i].distanceStandardDeviation = result.getDistanceStdDevMm() / 10;
                                legacyResults[i].rssi = result.getRssi() * -2;
                                legacyResults[i].ts = result.getRangingTimestampMillis() * 1000;
                                legacyResults[i].measurementFrameNumber = result.getNumAttemptedMeasurements();
                                legacyResults[i].successMeasurementFrameNumber = result.getNumSuccessfulMeasurements();
                            } else {
                                legacyResults[i].ts = SystemClock.elapsedRealtime() * 1000;
                            }
                            i++;
                        }
                        listener.onSuccess(legacyResults);
                    }
                });
            } catch (IllegalArgumentException e) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startRanging: invalid arguments - ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                listener.onFailure(-4, e.getMessage());
            } catch (SecurityException e2) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startRanging: security exception - ");
                stringBuilder2.append(e2);
                Log.e(str, stringBuilder2.toString());
                listener.onFailure(-5, e2.getMessage());
            }
            return;
        }
        listener.onFailure(-2, "");
    }

    public void stopRanging(RttListener listener) {
        Log.e(TAG, "stopRanging: unsupported operation - nop");
    }

    public void enableResponder(ResponderCallback callback) {
        throw new UnsupportedOperationException("enableResponder is not supported in the adaptation layer");
    }

    public void disableResponder(ResponderCallback callback) {
        throw new UnsupportedOperationException("disableResponder is not supported in the adaptation layer");
    }

    public RttManager(Context context, WifiRttManager service) {
        this.mNewService = service;
        this.mContext = context;
        boolean rttSupported = context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt");
        this.mRttCapabilities.oneSidedRttSupported = rttSupported;
        this.mRttCapabilities.twoSided11McRttSupported = rttSupported;
        this.mRttCapabilities.lciSupported = false;
        this.mRttCapabilities.lcrSupported = false;
        this.mRttCapabilities.preambleSupported = 6;
        this.mRttCapabilities.bwSupported = 24;
        this.mRttCapabilities.responderSupported = false;
        this.mRttCapabilities.secureRttSupported = false;
    }
}
