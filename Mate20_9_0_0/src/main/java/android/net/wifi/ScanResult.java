package android.net.wifi;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.PreciseDisconnectCause;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ScanResult implements Parcelable {
    public static final int AP_TYPE_INTERNET_ACCESS = 3;
    public static final int AP_TYPE_NO_INTERNET = 1;
    public static final int AP_TYPE_PORTAL = 2;
    public static final int AP_TYPE_UNKOWN = 0;
    public static final int CHANNEL_WIDTH_160MHZ = 3;
    public static final int CHANNEL_WIDTH_20MHZ = 0;
    public static final int CHANNEL_WIDTH_40MHZ = 1;
    public static final int CHANNEL_WIDTH_80MHZ = 2;
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
    public static final int CIPHER_CCMP = 3;
    public static final int CIPHER_NONE = 0;
    public static final int CIPHER_NO_GROUP_ADDRESSED = 1;
    public static final int CIPHER_TKIP = 2;
    public static final Creator<ScanResult> CREATOR = new Creator<ScanResult>() {
        public ScanResult createFromParcel(Parcel in) {
            int i;
            Parcel parcel = in;
            WifiSsid wifiSsid = null;
            boolean z = true;
            if (in.readInt() == 1) {
                wifiSsid = (WifiSsid) WifiSsid.CREATOR.createFromParcel(parcel);
            }
            ScanResult scanResult = new ScanResult(wifiSsid, in.readString(), in.readString(), in.readLong(), in.readInt(), in.readString(), in.readInt(), in.readInt(), in.readLong(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), false);
            scanResult.seen = in.readLong();
            int i2 = 0;
            scanResult.untrusted = in.readInt() != 0;
            scanResult.numUsage = in.readInt();
            scanResult.venueName = in.readString();
            scanResult.operatorFriendlyName = in.readString();
            scanResult.flags = in.readLong();
            scanResult.internetAccessType = in.readInt();
            scanResult.networkQosLevel = in.readInt();
            scanResult.networkSecurity = in.readInt();
            scanResult.networkQosScore = in.readInt();
            scanResult.isHiLinkNetwork = in.readInt() != 0;
            scanResult.dot11vNetwork = in.readInt() != 0;
            scanResult.hilinkTag = in.readInt();
            int n = in.readInt();
            if (n != 0) {
                scanResult.informationElements = new InformationElement[n];
                for (i = 0; i < n; i++) {
                    scanResult.informationElements[i] = new InformationElement();
                    scanResult.informationElements[i].id = in.readInt();
                    scanResult.informationElements[i].bytes = new byte[in.readInt()];
                    parcel.readByteArray(scanResult.informationElements[i].bytes);
                }
            }
            n = in.readInt();
            if (n != 0) {
                scanResult.anqpLines = new ArrayList();
                for (i = 0; i < n; i++) {
                    scanResult.anqpLines.add(in.readString());
                }
            }
            n = in.readInt();
            if (n != 0) {
                scanResult.anqpElements = new AnqpInformationElement[n];
                for (i = 0; i < n; i++) {
                    int vendorId = in.readInt();
                    int elementId = in.readInt();
                    byte[] payload = new byte[in.readInt()];
                    parcel.readByteArray(payload);
                    scanResult.anqpElements[i] = new AnqpInformationElement(vendorId, elementId, payload);
                }
            }
            if (in.readInt() == 0) {
                z = false;
            }
            scanResult.isCarrierAp = z;
            scanResult.carrierApEapType = in.readInt();
            scanResult.carrierName = in.readString();
            int n2 = in.readInt();
            if (n2 != 0) {
                scanResult.radioChainInfos = new RadioChainInfo[n2];
                while (true) {
                    n = i2;
                    if (n >= n2) {
                        break;
                    }
                    scanResult.radioChainInfos[n] = new RadioChainInfo();
                    scanResult.radioChainInfos[n].id = in.readInt();
                    scanResult.radioChainInfos[n].level = in.readInt();
                    i2 = n + 1;
                }
            }
            return scanResult;
        }

        public ScanResult[] newArray(int size) {
            return new ScanResult[size];
        }
    };
    public static final long FLAG_80211mc_RESPONDER = 2;
    public static final long FLAG_PASSPOINT_NETWORK = 1;
    public static final int KEY_MGMT_CERT = 8;
    public static final int KEY_MGMT_EAP = 2;
    public static final int KEY_MGMT_EAP_SHA256 = 6;
    public static final int KEY_MGMT_FT_EAP = 4;
    public static final int KEY_MGMT_FT_PSK = 3;
    public static final int KEY_MGMT_NONE = 0;
    public static final int KEY_MGMT_OSEN = 7;
    public static final int KEY_MGMT_PSK = 1;
    public static final int KEY_MGMT_PSK_SHA256 = 5;
    public static final int PROTOCOL_NONE = 0;
    public static final int PROTOCOL_OSEN = 3;
    public static final int PROTOCOL_WAPI = 4;
    public static final int PROTOCOL_WPA = 1;
    public static final int PROTOCOL_WPA2 = 2;
    public static final int QOS_LEVEL_GOOD = 3;
    public static final int QOS_LEVEL_NORMAL = 2;
    public static final int QOS_LEVEL_POOR = 1;
    public static final int QOS_LEVEL_UNKOWN = 0;
    public static final int UNSPECIFIED = -1;
    public String BSSID;
    public String SSID;
    public int anqpDomainId;
    public AnqpInformationElement[] anqpElements;
    public List<String> anqpLines;
    public String capabilities;
    public int carrierApEapType;
    public String carrierName;
    public int centerFreq0;
    public int centerFreq1;
    public int channelWidth;
    public int distanceCm;
    public int distanceSdCm;
    public boolean dot11vNetwork;
    public long flags;
    public int frequency;
    public long hessid;
    public int hilinkTag;
    public InformationElement[] informationElements;
    public int internetAccessType;
    public boolean is80211McRTTResponder;
    public boolean isCarrierAp;
    public boolean isHiLinkNetwork;
    public int level;
    public int networkQosLevel;
    public int networkQosScore;
    public int networkSecurity;
    public int numUsage;
    public CharSequence operatorFriendlyName;
    public RadioChainInfo[] radioChainInfos;
    public long seen;
    public long timestamp;
    @SystemApi
    public boolean untrusted;
    public CharSequence venueName;
    public WifiSsid wifiSsid;

    public static class InformationElement {
        public static final int EID_BSS_LOAD = 11;
        public static final int EID_ERP = 42;
        public static final int EID_EXTENDED_CAPS = 127;
        public static final int EID_EXTENDED_SUPPORTED_RATES = 50;
        public static final int EID_HT_CAP = 45;
        public static final int EID_HT_CAPABILITIES = 45;
        public static final int EID_HT_OPERATION = 61;
        public static final int EID_INTERWORKING = 107;
        public static final int EID_MDIE = 54;
        public static final int EID_RM = 70;
        public static final int EID_ROAMING_CONSORTIUM = 111;
        public static final int EID_RSN = 48;
        public static final int EID_SSID = 0;
        public static final int EID_SUPPORTED_RATES = 1;
        public static final int EID_TIM = 5;
        public static final int EID_VHT_CAPABILITIES = 191;
        public static final int EID_VHT_OPERATION = 192;
        public static final int EID_VSA = 221;
        public static final int EID_WAPI = 68;
        public byte[] bytes;
        public int id;

        public InformationElement(InformationElement rhs) {
            this.id = rhs.id;
            this.bytes = (byte[]) rhs.bytes.clone();
        }
    }

    public static class RadioChainInfo {
        public int id;
        public int level;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RadioChainInfo: id=");
            stringBuilder.append(this.id);
            stringBuilder.append(", level=");
            stringBuilder.append(this.level);
            return stringBuilder.toString();
        }

        public boolean equals(Object otherObj) {
            boolean z = true;
            if (this == otherObj) {
                return true;
            }
            if (!(otherObj instanceof RadioChainInfo)) {
                return false;
            }
            RadioChainInfo other = (RadioChainInfo) otherObj;
            if (!(this.id == other.id && this.level == other.level)) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(this.id), Integer.valueOf(this.level)});
        }
    }

    public void setFlag(long flag) {
        this.flags |= flag;
    }

    public void clearFlag(long flag) {
        this.flags &= ~flag;
    }

    public boolean is80211mcResponder() {
        return (this.flags & 2) != 0;
    }

    public boolean isPasspointNetwork() {
        return (this.flags & 1) != 0;
    }

    public boolean is24GHz() {
        return is24GHz(this.frequency);
    }

    public static boolean is24GHz(int freq) {
        return freq > 2400 && freq < PreciseDisconnectCause.EPDG_TUNNEL_ESTABLISH_FAILURE;
    }

    public boolean is5GHz() {
        return is5GHz(this.frequency);
    }

    public static boolean is5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }

    public ScanResult(WifiSsid wifiSsid, String BSSID, long hessid, int anqpDomainId, byte[] osuProviders, String caps, int level, int frequency, long tsf) {
        WifiSsid wifiSsid2 = wifiSsid;
        byte[] bArr = osuProviders;
        this.isHiLinkNetwork = false;
        this.dot11vNetwork = false;
        this.wifiSsid = wifiSsid2;
        this.SSID = wifiSsid2 != null ? wifiSsid.toString() : WifiSsid.NONE;
        this.BSSID = BSSID;
        this.hessid = hessid;
        this.anqpDomainId = anqpDomainId;
        if (bArr != null) {
            this.anqpElements = new AnqpInformationElement[1];
            this.anqpElements[0] = new AnqpInformationElement(AnqpInformationElement.HOTSPOT20_VENDOR_ID, 8, bArr);
        }
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = -1;
        this.distanceSdCm = -1;
        this.channelWidth = -1;
        this.centerFreq0 = -1;
        this.centerFreq1 = -1;
        this.flags = 0;
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public ScanResult(WifiSsid wifiSsid, String BSSID, String caps, int level, int frequency, long tsf, int distCm, int distSdCm) {
        this.isHiLinkNetwork = false;
        this.dot11vNetwork = false;
        this.wifiSsid = wifiSsid;
        this.SSID = wifiSsid != null ? wifiSsid.toString() : WifiSsid.NONE;
        this.BSSID = BSSID;
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
        this.channelWidth = -1;
        this.centerFreq0 = -1;
        this.centerFreq1 = -1;
        this.flags = 0;
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public ScanResult(String Ssid, String BSSID, long hessid, int anqpDomainId, String caps, int level, int frequency, long tsf, int distCm, int distSdCm, int channelWidth, int centerFreq0, int centerFreq1, boolean is80211McRTTResponder) {
        this.isHiLinkNetwork = false;
        this.dot11vNetwork = false;
        this.SSID = Ssid;
        this.BSSID = BSSID;
        this.hessid = hessid;
        this.anqpDomainId = anqpDomainId;
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
        this.channelWidth = channelWidth;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        if (is80211McRTTResponder) {
            this.flags = 2;
        } else {
            this.flags = 0;
        }
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public ScanResult(WifiSsid wifiSsid, String Ssid, String BSSID, long hessid, int anqpDomainId, String caps, int level, int frequency, long tsf, int distCm, int distSdCm, int channelWidth, int centerFreq0, int centerFreq1, boolean is80211McRTTResponder) {
        this(Ssid, BSSID, hessid, anqpDomainId, caps, level, frequency, tsf, distCm, distSdCm, channelWidth, centerFreq0, centerFreq1, is80211McRTTResponder);
        this.wifiSsid = wifiSsid;
    }

    public ScanResult(ScanResult source) {
        this.isHiLinkNetwork = false;
        this.dot11vNetwork = false;
        if (source != null) {
            this.wifiSsid = source.wifiSsid;
            this.SSID = source.SSID;
            this.BSSID = source.BSSID;
            this.hessid = source.hessid;
            this.anqpDomainId = source.anqpDomainId;
            this.informationElements = source.informationElements;
            this.anqpElements = source.anqpElements;
            this.capabilities = source.capabilities;
            this.level = source.level;
            this.frequency = source.frequency;
            this.channelWidth = source.channelWidth;
            this.centerFreq0 = source.centerFreq0;
            this.centerFreq1 = source.centerFreq1;
            this.timestamp = source.timestamp;
            this.distanceCm = source.distanceCm;
            this.distanceSdCm = source.distanceSdCm;
            this.seen = source.seen;
            this.untrusted = source.untrusted;
            this.numUsage = source.numUsage;
            this.venueName = source.venueName;
            this.operatorFriendlyName = source.operatorFriendlyName;
            this.flags = source.flags;
            this.isCarrierAp = source.isCarrierAp;
            this.carrierApEapType = source.carrierApEapType;
            this.carrierName = source.carrierName;
            this.radioChainInfos = source.radioChainInfos;
            this.internetAccessType = source.internetAccessType;
            this.networkQosLevel = source.networkQosLevel;
            this.networkSecurity = source.networkSecurity;
            this.networkQosScore = source.networkQosScore;
            this.isHiLinkNetwork = source.isHiLinkNetwork;
            this.dot11vNetwork = source.dot11vNetwork;
            this.hilinkTag = source.hilinkTag;
        }
    }

    public ScanResult() {
        this.isHiLinkNetwork = false;
        this.dot11vNetwork = false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String none = "<none>";
        sb.append("SSID: ");
        sb.append(this.wifiSsid == null ? WifiSsid.NONE : this.wifiSsid);
        sb.append(", BSSID: ");
        sb.append(this.BSSID == null ? none : this.BSSID);
        sb.append(", capabilities: ");
        sb.append(this.capabilities == null ? none : this.capabilities);
        sb.append(", level: ");
        sb.append(this.level);
        sb.append(", frequency: ");
        sb.append(this.frequency);
        sb.append(", timestamp: ");
        sb.append(this.timestamp);
        sb.append(", distance: ");
        sb.append(this.distanceCm != -1 ? Integer.valueOf(this.distanceCm) : "?");
        sb.append("(cm)");
        sb.append(", distanceSd: ");
        sb.append(this.distanceSdCm != -1 ? Integer.valueOf(this.distanceSdCm) : "?");
        sb.append("(cm)");
        sb.append(", passpoint: ");
        sb.append((this.flags & 1) != 0 ? "yes" : "no");
        sb.append(", ChannelBandwidth: ");
        sb.append(this.channelWidth);
        sb.append(", centerFreq0: ");
        sb.append(this.centerFreq0);
        sb.append(", centerFreq1: ");
        sb.append(this.centerFreq1);
        sb.append(", 80211mcResponder: ");
        sb.append((this.flags & 2) != 0 ? "is supported" : "is not supported");
        sb.append(", Carrier AP: ");
        sb.append(this.isCarrierAp ? "yes" : "no");
        sb.append(", Carrier AP EAP Type: ");
        sb.append(this.carrierApEapType);
        sb.append(", Carrier name: ");
        sb.append(this.carrierName);
        sb.append(", Radio Chain Infos: ");
        sb.append(Arrays.toString(this.radioChainInfos));
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2 = 0;
        if (this.wifiSsid != null) {
            dest.writeInt(1);
            this.wifiSsid.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.SSID);
        dest.writeString(this.BSSID);
        dest.writeLong(this.hessid);
        dest.writeInt(this.anqpDomainId);
        dest.writeString(this.capabilities);
        dest.writeInt(this.level);
        dest.writeInt(this.frequency);
        dest.writeLong(this.timestamp);
        dest.writeInt(this.distanceCm);
        dest.writeInt(this.distanceSdCm);
        dest.writeInt(this.channelWidth);
        dest.writeInt(this.centerFreq0);
        dest.writeInt(this.centerFreq1);
        dest.writeLong(this.seen);
        dest.writeInt(this.untrusted);
        dest.writeInt(this.numUsage);
        dest.writeString(this.venueName != null ? this.venueName.toString() : "");
        dest.writeString(this.operatorFriendlyName != null ? this.operatorFriendlyName.toString() : "");
        dest.writeLong(this.flags);
        dest.writeInt(this.internetAccessType);
        dest.writeInt(this.networkQosLevel);
        dest.writeInt(this.networkSecurity);
        dest.writeInt(this.networkQosScore);
        dest.writeInt(this.isHiLinkNetwork);
        dest.writeInt(this.dot11vNetwork);
        dest.writeInt(this.hilinkTag);
        if (this.informationElements != null) {
            dest.writeInt(this.informationElements.length);
            for (i = 0; i < this.informationElements.length; i++) {
                dest.writeInt(this.informationElements[i].id);
                dest.writeInt(this.informationElements[i].bytes.length);
                dest.writeByteArray(this.informationElements[i].bytes);
            }
        } else {
            dest.writeInt(0);
        }
        if (this.anqpLines != null) {
            dest.writeInt(this.anqpLines.size());
            for (i = 0; i < this.anqpLines.size(); i++) {
                dest.writeString((String) this.anqpLines.get(i));
            }
        } else {
            dest.writeInt(0);
        }
        if (this.anqpElements != null) {
            dest.writeInt(this.anqpElements.length);
            for (AnqpInformationElement element : this.anqpElements) {
                dest.writeInt(element.getVendorId());
                dest.writeInt(element.getElementId());
                dest.writeInt(element.getPayload().length);
                dest.writeByteArray(element.getPayload());
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.isCarrierAp);
        dest.writeInt(this.carrierApEapType);
        dest.writeString(this.carrierName);
        if (this.radioChainInfos != null) {
            dest.writeInt(this.radioChainInfos.length);
            while (true) {
                i = i2;
                if (i < this.radioChainInfos.length) {
                    dest.writeInt(this.radioChainInfos[i].id);
                    dest.writeInt(this.radioChainInfos[i].level);
                    i2 = i + 1;
                } else {
                    return;
                }
            }
        }
        dest.writeInt(0);
    }
}
