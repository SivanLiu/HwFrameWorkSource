package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemProperties;

public class PPPOEConfig implements Parcelable {
    public static final Creator<PPPOEConfig> CREATOR = new Creator<PPPOEConfig>() {
        public PPPOEConfig createFromParcel(Parcel source) {
            PPPOEConfig config = new PPPOEConfig();
            config.username = source.readString();
            config.password = source.readString();
            config.interf = source.readString();
            config.lcp_echo_interval = source.readInt();
            config.lcp_echo_failure = source.readInt();
            config.mtu = source.readInt();
            config.mru = source.readInt();
            config.timeout = source.readInt();
            config.MSS = source.readInt();
            return config;
        }

        public PPPOEConfig[] newArray(int size) {
            return new PPPOEConfig[size];
        }
    };
    public int MSS = 1412;
    public String interf = SystemProperties.get("wifi.interface", "eth0");
    public int lcp_echo_failure = 3;
    public int lcp_echo_interval = 30;
    public int mru = 1480;
    public int mtu = 1480;
    public String password;
    public int timeout = 70;
    public String username;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeString(this.interf);
        dest.writeInt(this.lcp_echo_interval);
        dest.writeInt(this.lcp_echo_failure);
        dest.writeInt(this.mtu);
        dest.writeInt(this.mru);
        dest.writeInt(this.timeout);
        dest.writeInt(this.MSS);
    }

    public String[] getArgs() {
        String[] strArr = new String[9];
        strArr[0] = this.username;
        strArr[1] = this.password;
        strArr[2] = this.interf;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.lcp_echo_interval);
        strArr[3] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.lcp_echo_failure);
        strArr[4] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.mtu);
        strArr[5] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.mru);
        strArr[6] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.timeout);
        strArr[7] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.MSS);
        strArr[8] = stringBuilder.toString();
        return strArr;
    }

    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("username=");
        strBuilder.append(this.username);
        strBuilder.append(",interf=");
        strBuilder.append(this.interf);
        strBuilder.append(",lcp_echo_interval=");
        strBuilder.append(this.lcp_echo_interval);
        strBuilder.append(",lcp_echo_failure=");
        strBuilder.append(this.lcp_echo_failure);
        strBuilder.append(",mtu=");
        strBuilder.append(this.mtu);
        strBuilder.append(",mru=");
        strBuilder.append(this.mru);
        strBuilder.append(",timeout=");
        strBuilder.append(this.timeout);
        strBuilder.append(",MSS=");
        strBuilder.append(this.MSS);
        return strBuilder.toString();
    }
}
