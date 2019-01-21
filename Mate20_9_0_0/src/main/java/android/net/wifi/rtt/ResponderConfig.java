package android.net.wifi.rtt;

import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanResult.InformationElement;
import android.net.wifi.aware.PeerHandle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

@SystemApi
public final class ResponderConfig implements Parcelable {
    private static final int AWARE_BAND_2_DISCOVERY_CHANNEL = 2437;
    public static final int CHANNEL_WIDTH_160MHZ = 3;
    public static final int CHANNEL_WIDTH_20MHZ = 0;
    public static final int CHANNEL_WIDTH_40MHZ = 1;
    public static final int CHANNEL_WIDTH_80MHZ = 2;
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
    public static final Creator<ResponderConfig> CREATOR = new Creator<ResponderConfig>() {
        public ResponderConfig[] newArray(int size) {
            return new ResponderConfig[size];
        }

        public ResponderConfig createFromParcel(Parcel in) {
            MacAddress macAddress = null;
            if (in.readBoolean()) {
                macAddress = (MacAddress) MacAddress.CREATOR.createFromParcel(in);
            } else {
                Parcel parcel = in;
            }
            PeerHandle peerHandle = null;
            if (in.readBoolean()) {
                peerHandle = new PeerHandle(in.readInt());
            }
            PeerHandle peerHandle2 = peerHandle;
            int responderType = in.readInt();
            boolean supports80211mc = in.readInt() == 1;
            int channelWidth = in.readInt();
            int frequency = in.readInt();
            int centerFreq0 = in.readInt();
            int centerFreq1 = in.readInt();
            int preamble = in.readInt();
            if (peerHandle2 == null) {
                return new ResponderConfig(macAddress, responderType, supports80211mc, channelWidth, frequency, centerFreq0, centerFreq1, preamble);
            }
            return new ResponderConfig(peerHandle2, responderType, supports80211mc, channelWidth, frequency, centerFreq0, centerFreq1, preamble);
        }
    };
    public static final int PREAMBLE_HT = 1;
    public static final int PREAMBLE_LEGACY = 0;
    public static final int PREAMBLE_VHT = 2;
    public static final int RESPONDER_AP = 0;
    public static final int RESPONDER_AWARE = 4;
    public static final int RESPONDER_P2P_CLIENT = 3;
    public static final int RESPONDER_P2P_GO = 2;
    public static final int RESPONDER_STA = 1;
    private static final String TAG = "ResponderConfig";
    public final int centerFreq0;
    public final int centerFreq1;
    public final int channelWidth;
    public final int frequency;
    public final MacAddress macAddress;
    public final PeerHandle peerHandle;
    public final int preamble;
    public final int responderType;
    public final boolean supports80211mc;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ChannelWidth {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PreambleType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResponderType {
    }

    public ResponderConfig(MacAddress macAddress, int responderType, boolean supports80211mc, int channelWidth, int frequency, int centerFreq0, int centerFreq1, int preamble) {
        if (macAddress != null) {
            this.macAddress = macAddress;
            this.peerHandle = null;
            this.responderType = responderType;
            this.supports80211mc = supports80211mc;
            this.channelWidth = channelWidth;
            this.frequency = frequency;
            this.centerFreq0 = centerFreq0;
            this.centerFreq1 = centerFreq1;
            this.preamble = preamble;
            return;
        }
        throw new IllegalArgumentException("Invalid ResponderConfig - must specify a MAC address");
    }

    public ResponderConfig(PeerHandle peerHandle, int responderType, boolean supports80211mc, int channelWidth, int frequency, int centerFreq0, int centerFreq1, int preamble) {
        this.macAddress = null;
        this.peerHandle = peerHandle;
        this.responderType = responderType;
        this.supports80211mc = supports80211mc;
        this.channelWidth = channelWidth;
        this.frequency = frequency;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        this.preamble = preamble;
    }

    public ResponderConfig(MacAddress macAddress, PeerHandle peerHandle, int responderType, boolean supports80211mc, int channelWidth, int frequency, int centerFreq0, int centerFreq1, int preamble) {
        this.macAddress = macAddress;
        this.peerHandle = peerHandle;
        this.responderType = responderType;
        this.supports80211mc = supports80211mc;
        this.channelWidth = channelWidth;
        this.frequency = frequency;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        this.preamble = preamble;
    }

    public static ResponderConfig fromScanResult(ScanResult scanResult) {
        int i;
        ScanResult scanResult2 = scanResult;
        MacAddress macAddress = MacAddress.fromString(scanResult2.BSSID);
        boolean supports80211mc = scanResult.is80211mcResponder();
        int channelWidth = translateScanResultChannelWidth(scanResult2.channelWidth);
        int frequency = scanResult2.frequency;
        int centerFreq0 = scanResult2.centerFreq0;
        int centerFreq1 = scanResult2.centerFreq1;
        if (scanResult2.informationElements == null || scanResult2.informationElements.length == 0) {
            Log.e(TAG, "Scan Results do not contain IEs - using backup method to select preamble");
            if (channelWidth == 2 || channelWidth == 3) {
                i = 2;
            } else {
                i = 1;
            }
        } else {
            int preamble;
            boolean vhtCapabilitiesPresent = false;
            boolean htCapabilitiesPresent = false;
            for (InformationElement ie : scanResult2.informationElements) {
                if (ie.id == 45) {
                    htCapabilitiesPresent = true;
                } else if (ie.id == 191) {
                    vhtCapabilitiesPresent = true;
                }
            }
            if (vhtCapabilitiesPresent) {
                preamble = 2;
            } else if (htCapabilitiesPresent) {
                preamble = 1;
            } else {
                preamble = 0;
            }
            i = preamble;
        }
        return new ResponderConfig(macAddress, 0, supports80211mc, channelWidth, frequency, centerFreq0, centerFreq1, i);
    }

    public static ResponderConfig fromWifiAwarePeerMacAddressWithDefaults(MacAddress macAddress) {
        return new ResponderConfig(macAddress, 4, true, 0, (int) AWARE_BAND_2_DISCOVERY_CHANNEL, 0, 0, 1);
    }

    public static ResponderConfig fromWifiAwarePeerHandleWithDefaults(PeerHandle peerHandle) {
        return new ResponderConfig(peerHandle, 4, true, 0, (int) AWARE_BAND_2_DISCOVERY_CHANNEL, 0, 0, 1);
    }

    public boolean isValid(boolean awareSupported) {
        if ((this.macAddress == null && this.peerHandle == null) || (this.macAddress != null && this.peerHandle != null)) {
            return false;
        }
        if (awareSupported || this.responderType != 4) {
            return true;
        }
        return false;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (this.macAddress == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            this.macAddress.writeToParcel(dest, flags);
        }
        if (this.peerHandle == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            dest.writeInt(this.peerHandle.peerId);
        }
        dest.writeInt(this.responderType);
        dest.writeInt(this.supports80211mc);
        dest.writeInt(this.channelWidth);
        dest.writeInt(this.frequency);
        dest.writeInt(this.centerFreq0);
        dest.writeInt(this.centerFreq1);
        dest.writeInt(this.preamble);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResponderConfig)) {
            return false;
        }
        ResponderConfig lhs = (ResponderConfig) o;
        if (!(Objects.equals(this.macAddress, lhs.macAddress) && Objects.equals(this.peerHandle, lhs.peerHandle) && this.responderType == lhs.responderType && this.supports80211mc == lhs.supports80211mc && this.channelWidth == lhs.channelWidth && this.frequency == lhs.frequency && this.centerFreq0 == lhs.centerFreq0 && this.centerFreq1 == lhs.centerFreq1 && this.preamble == lhs.preamble)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.macAddress, this.peerHandle, Integer.valueOf(this.responderType), Boolean.valueOf(this.supports80211mc), Integer.valueOf(this.channelWidth), Integer.valueOf(this.frequency), Integer.valueOf(this.centerFreq0), Integer.valueOf(this.centerFreq1), Integer.valueOf(this.preamble)});
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer("ResponderConfig: macAddress=");
        stringBuffer.append(this.macAddress);
        stringBuffer.append(", peerHandle=");
        stringBuffer.append(this.peerHandle == null ? "<null>" : Integer.valueOf(this.peerHandle.peerId));
        stringBuffer.append(", responderType=");
        stringBuffer.append(this.responderType);
        stringBuffer.append(", supports80211mc=");
        stringBuffer.append(this.supports80211mc);
        stringBuffer.append(", channelWidth=");
        stringBuffer.append(this.channelWidth);
        stringBuffer.append(", frequency=");
        stringBuffer.append(this.frequency);
        stringBuffer.append(", centerFreq0=");
        stringBuffer.append(this.centerFreq0);
        stringBuffer.append(", centerFreq1=");
        stringBuffer.append(this.centerFreq1);
        stringBuffer.append(", preamble=");
        stringBuffer.append(this.preamble);
        return stringBuffer.toString();
    }

    static int translateScanResultChannelWidth(int scanResultChannelWidth) {
        switch (scanResultChannelWidth) {
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
                stringBuilder.append("translateScanResultChannelWidth: bad ");
                stringBuilder.append(scanResultChannelWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
