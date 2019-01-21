package android.net.wifi;

import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.Log;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Locale;

public class WifiInfo implements Parcelable {
    public static final Creator<WifiInfo> CREATOR = new Creator<WifiInfo>() {
        public WifiInfo createFromParcel(Parcel in) {
            WifiInfo info = new WifiInfo();
            info.setNetworkId(in.readInt());
            info.setLastNetIdForAp(in.readInt());
            info.setRssi(in.readInt());
            info.setLinkSpeed(in.readInt());
            info.setFrequency(in.readInt());
            boolean z = true;
            if (in.readByte() == (byte) 1) {
                try {
                    info.setInetAddress(InetAddress.getByAddress(in.createByteArray()));
                } catch (UnknownHostException e) {
                }
            }
            if (in.readInt() == 1) {
                info.mWifiSsid = (WifiSsid) WifiSsid.CREATOR.createFromParcel(in);
            }
            info.mBSSID = in.readString();
            info.mMacAddress = in.readString();
            info.mMeteredHint = in.readInt() != 0;
            if (in.readInt() == 0) {
                z = false;
            }
            info.mEphemeral = z;
            info.score = in.readInt();
            info.txSuccessRate = in.readDouble();
            info.txRetriesRate = in.readDouble();
            info.txBadRate = in.readDouble();
            info.rxSuccessRate = in.readDouble();
            info.mSupplicantState = (SupplicantState) SupplicantState.CREATOR.createFromParcel(in);
            return info;
        }

        public WifiInfo[] newArray(int size) {
            return new WifiInfo[size];
        }
    };
    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    public static final int DUPLEX_PATH_SELECTED = 1;
    public static final String FREQUENCY_UNITS = "MHz";
    public static final int INVALID_RSSI = -127;
    public static final String LINK_SPEED_UNITS = "Mbps";
    public static final int MAX_RSSI = 200;
    public static final int MIN_RSSI = -126;
    private static final int NET_ID_NONE = -1;
    private static final String TAG = "WifiInfo";
    public static final int WIFI_AP_TYPE_AI_DEVICE = 1;
    public static final int WIFI_AP_TYPE_BASE = 0;
    public static final int WIFI_AP_TYPE_NORMAL = 0;
    private static final EnumMap<SupplicantState, DetailedState> stateMap = new EnumMap(SupplicantState.class);
    private String mBSSID;
    private int mChload;
    private boolean mEphemeral;
    private int mFrequency;
    private InetAddress mIpAddress;
    private int mLinkSpeed;
    private String mMacAddress;
    private boolean mMeteredHint;
    private int mNetworkId;
    private int mNoise;
    private int mRssi;
    private int mSnr;
    private SupplicantState mSupplicantState;
    private int mWifiApType;
    private WifiSsid mWifiSsid;
    private int mlastNetIdForAp;
    public long rxSuccess;
    public double rxSuccessRate;
    public int score;
    public long txBad;
    public double txBadRate;
    public long txRetries;
    public double txRetriesRate;
    public long txSuccess;
    public double txSuccessRate;

    static {
        stateMap.put(SupplicantState.DISCONNECTED, DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.INTERFACE_DISABLED, DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.INACTIVE, DetailedState.IDLE);
        stateMap.put(SupplicantState.SCANNING, DetailedState.SCANNING);
        stateMap.put(SupplicantState.AUTHENTICATING, DetailedState.CONNECTING);
        stateMap.put(SupplicantState.ASSOCIATING, DetailedState.CONNECTING);
        stateMap.put(SupplicantState.ASSOCIATED, DetailedState.CONNECTING);
        stateMap.put(SupplicantState.FOUR_WAY_HANDSHAKE, DetailedState.AUTHENTICATING);
        stateMap.put(SupplicantState.GROUP_HANDSHAKE, DetailedState.AUTHENTICATING);
        stateMap.put(SupplicantState.COMPLETED, DetailedState.OBTAINING_IPADDR);
        stateMap.put(SupplicantState.DORMANT, DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.UNINITIALIZED, DetailedState.IDLE);
        stateMap.put(SupplicantState.INVALID, DetailedState.FAILED);
    }

    public WifiInfo() {
        this.mWifiApType = 0;
        this.mMacAddress = DEFAULT_MAC_ADDRESS;
        this.mWifiSsid = null;
        this.mBSSID = null;
        this.mNetworkId = -1;
        this.mSupplicantState = SupplicantState.UNINITIALIZED;
        this.mRssi = INVALID_RSSI;
        this.mLinkSpeed = -1;
        this.mFrequency = -1;
        this.mNoise = 0;
        this.mlastNetIdForAp = -1;
        this.mSnr = -1;
        this.mChload = -1;
    }

    public void reset() {
        setInetAddress(null);
        setBSSID(null);
        setSSID(null);
        setNetworkId(-1);
        setRssi(INVALID_RSSI);
        setLinkSpeed(-1);
        setFrequency(-1);
        setMeteredHint(false);
        setEphemeral(false);
        setNoise(0);
        setSnr(-1);
        setChload(-1);
        this.txBad = 0;
        this.txSuccess = 0;
        this.rxSuccess = 0;
        this.txRetries = 0;
        this.txBadRate = 0.0d;
        this.txSuccessRate = 0.0d;
        this.rxSuccessRate = 0.0d;
        this.txRetriesRate = 0.0d;
        this.score = 0;
    }

    public WifiInfo(WifiInfo source) {
        this.mWifiApType = 0;
        this.mMacAddress = DEFAULT_MAC_ADDRESS;
        if (source != null) {
            this.mSupplicantState = source.mSupplicantState;
            this.mBSSID = source.mBSSID;
            this.mWifiSsid = source.mWifiSsid;
            this.mNetworkId = source.mNetworkId;
            this.mRssi = source.mRssi;
            this.mLinkSpeed = source.mLinkSpeed;
            this.mFrequency = source.mFrequency;
            this.mIpAddress = source.mIpAddress;
            this.mMacAddress = source.mMacAddress;
            this.mMeteredHint = source.mMeteredHint;
            this.mEphemeral = source.mEphemeral;
            this.txBad = source.txBad;
            this.txRetries = source.txRetries;
            this.txSuccess = source.txSuccess;
            this.rxSuccess = source.rxSuccess;
            this.txBadRate = source.txBadRate;
            this.txRetriesRate = source.txRetriesRate;
            this.txSuccessRate = source.txSuccessRate;
            this.rxSuccessRate = source.rxSuccessRate;
            this.score = source.score;
            this.mNoise = source.mNoise;
            this.mlastNetIdForAp = source.mlastNetIdForAp;
            this.mSnr = source.mSnr;
            this.mChload = source.mChload;
        }
    }

    public void setSSID(WifiSsid wifiSsid) {
        this.mWifiSsid = wifiSsid;
    }

    public String getSSID() {
        if (this.mWifiSsid != null) {
            String hex;
            try {
                String unicode = this.mWifiSsid.toString();
                if (TextUtils.isEmpty(unicode)) {
                    hex = this.mWifiSsid.getHexString();
                    return hex != null ? hex : WifiSsid.NONE;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\"");
                stringBuilder.append(unicode);
                stringBuilder.append("\"");
                return stringBuilder.toString();
            } catch (NullPointerException e) {
                hex = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("NullPointerException e: ");
                stringBuilder2.append(e);
                Log.d(hex, stringBuilder2.toString());
            }
        }
        return WifiSsid.NONE;
    }

    public WifiSsid getWifiSsid() {
        return this.mWifiSsid;
    }

    public void setBSSID(String BSSID) {
        this.mBSSID = BSSID;
    }

    public String getBSSID() {
        return this.mBSSID;
    }

    public int getRssi() {
        return this.mRssi;
    }

    public void setRssi(int rssi) {
        if (rssi < INVALID_RSSI) {
            rssi = INVALID_RSSI;
        }
        if (rssi > 200) {
            rssi = 200;
        }
        this.mRssi = rssi;
    }

    public int getLinkSpeed() {
        return this.mLinkSpeed;
    }

    public void setLinkSpeed(int linkSpeed) {
        this.mLinkSpeed = linkSpeed;
    }

    public int getFrequency() {
        return this.mFrequency;
    }

    public void setFrequency(int frequency) {
        this.mFrequency = frequency;
    }

    public int getNoise() {
        return this.mNoise;
    }

    public void setNoise(int noise) {
        this.mNoise = noise;
    }

    public int getSnr() {
        return this.mSnr;
    }

    public void setSnr(int snr) {
        this.mSnr = snr;
    }

    public int getChload() {
        return this.mChload;
    }

    public void setChload(int chload) {
        this.mChload = chload;
    }

    public int getLastNetIdForAp() {
        return this.mlastNetIdForAp;
    }

    public void setLastNetIdForAp(int lastNetIdForAp) {
        this.mlastNetIdForAp = lastNetIdForAp;
    }

    public int getWifiApType() {
        return this.mWifiApType;
    }

    public void setWifiApType(int type) {
        this.mWifiApType = type;
    }

    public boolean is24GHz() {
        return ScanResult.is24GHz(this.mFrequency);
    }

    public boolean is5GHz() {
        return ScanResult.is5GHz(this.mFrequency);
    }

    public void setMacAddress(String macAddress) {
        this.mMacAddress = macAddress;
    }

    public String getMacAddress() {
        return this.mMacAddress;
    }

    public boolean hasRealMacAddress() {
        return (this.mMacAddress == null || DEFAULT_MAC_ADDRESS.equals(this.mMacAddress)) ? false : true;
    }

    public void setMeteredHint(boolean meteredHint) {
        this.mMeteredHint = meteredHint;
    }

    public boolean getMeteredHint() {
        return this.mMeteredHint;
    }

    public void setEphemeral(boolean ephemeral) {
        this.mEphemeral = ephemeral;
    }

    public boolean isEphemeral() {
        return this.mEphemeral;
    }

    public void setNetworkId(int id) {
        this.mNetworkId = id;
    }

    public int getNetworkId() {
        return this.mNetworkId;
    }

    public SupplicantState getSupplicantState() {
        return this.mSupplicantState;
    }

    public void setSupplicantState(SupplicantState state) {
        this.mSupplicantState = state;
    }

    public void setInetAddress(InetAddress address) {
        this.mIpAddress = address;
    }

    public int getIpAddress() {
        if (this.mIpAddress instanceof Inet4Address) {
            return NetworkUtils.inetAddressToInt((Inet4Address) this.mIpAddress);
        }
        return 0;
    }

    public boolean getHiddenSSID() {
        if (this.mWifiSsid == null) {
            return false;
        }
        return this.mWifiSsid.isHidden();
    }

    public static DetailedState getDetailedStateOf(SupplicantState suppState) {
        return (DetailedState) stateMap.get(suppState);
    }

    void setSupplicantState(String stateName) {
        this.mSupplicantState = valueOf(stateName);
    }

    static SupplicantState valueOf(String stateName) {
        if ("4WAY_HANDSHAKE".equalsIgnoreCase(stateName)) {
            return SupplicantState.FOUR_WAY_HANDSHAKE;
        }
        try {
            return SupplicantState.valueOf(stateName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SupplicantState.INVALID;
        }
    }

    public static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String none = "<none>";
        sb.append("SSID: ");
        sb.append(this.mWifiSsid == null ? WifiSsid.NONE : this.mWifiSsid);
        sb.append(", BSSID: ");
        sb.append(this.mBSSID == null ? none : ParcelUtil.safeDisplayMac(this.mBSSID));
        sb.append(", Supplicant state: ");
        sb.append(this.mSupplicantState == null ? none : this.mSupplicantState);
        sb.append(", RSSI: ");
        sb.append(this.mRssi);
        sb.append(", Link speed: ");
        sb.append(this.mLinkSpeed);
        sb.append(LINK_SPEED_UNITS);
        sb.append(", Frequency: ");
        sb.append(this.mFrequency);
        sb.append(FREQUENCY_UNITS);
        sb.append(", Net ID: ");
        sb.append(this.mNetworkId);
        sb.append(", Metered hint: ");
        sb.append(this.mMeteredHint);
        sb.append(", score: ");
        sb.append(Integer.toString(this.score));
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mNetworkId);
        dest.writeInt(this.mlastNetIdForAp);
        dest.writeInt(this.mRssi);
        dest.writeInt(this.mLinkSpeed);
        dest.writeInt(this.mFrequency);
        InetAddress address = this.mIpAddress;
        if (address != null) {
            dest.writeByte((byte) 1);
            dest.writeByteArray(address.getAddress());
        } else {
            dest.writeByte((byte) 0);
        }
        WifiSsid ssid = this.mWifiSsid;
        if (ssid != null) {
            dest.writeInt(1);
            ssid.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.mBSSID);
        dest.writeString(this.mMacAddress);
        dest.writeInt(this.mMeteredHint);
        dest.writeInt(this.mEphemeral);
        dest.writeInt(this.score);
        dest.writeDouble(this.txSuccessRate);
        dest.writeDouble(this.txRetriesRate);
        dest.writeDouble(this.txBadRate);
        dest.writeDouble(this.rxSuccessRate);
        SupplicantState state = this.mSupplicantState;
        if (state != null) {
            state.writeToParcel(dest, flags);
        }
    }
}
