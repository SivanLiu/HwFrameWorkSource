package android.net;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.Log;
import java.net.Inet4Address;
import java.util.Objects;

public class DhcpResults extends StaticIpConfiguration {
    public static final Creator<DhcpResults> CREATOR = new Creator<DhcpResults>() {
        public DhcpResults createFromParcel(Parcel in) {
            DhcpResults dhcpResults = new DhcpResults();
            DhcpResults.readFromParcel(dhcpResults, in);
            return dhcpResults;
        }

        public DhcpResults[] newArray(int size) {
            return new DhcpResults[size];
        }
    };
    private static final String TAG = "DhcpResults";
    public int leaseDuration;
    public int mtu;
    public Inet4Address serverAddress;
    public String vendorInfo;

    public DhcpResults(StaticIpConfiguration source) {
        super(source);
    }

    public DhcpResults(DhcpResults source) {
        super(source);
        if (source != null) {
            this.serverAddress = source.serverAddress;
            this.vendorInfo = source.vendorInfo;
            this.leaseDuration = source.leaseDuration;
            this.mtu = source.mtu;
        }
    }

    public boolean hasMeteredHint() {
        boolean z = false;
        if (this.vendorInfo != null && !this.vendorInfo.isEmpty()) {
            if (this.vendorInfo.contains("ANDROID_METERED") || (this.vendorInfo.startsWith("hostname:") && this.ipAddress.getAddress() != null && this.ipAddress.getAddress().toString().startsWith("/172.20.10."))) {
                z = true;
            }
            return z;
        } else if (this.domains == null || this.domains.isEmpty()) {
            return false;
        } else {
            return this.domains.equals("mshome.net");
        }
    }

    public void clear() {
        super.clear();
        this.vendorInfo = null;
        this.leaseDuration = 0;
        this.mtu = 0;
    }

    public String toString() {
        StringBuffer str = new StringBuffer(super.toString());
        str.append(" DHCP server ");
        str.append(this.serverAddress);
        str.append(" Vendor info ");
        str.append(this.vendorInfo);
        str.append(" lease ");
        str.append(this.leaseDuration);
        str.append(" seconds");
        if (this.mtu != 0) {
            str.append(" MTU ");
            str.append(this.mtu);
        }
        return str.toString();
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpResults)) {
            return false;
        }
        DhcpResults target = (DhcpResults) obj;
        if (!(super.equals((StaticIpConfiguration) obj) && Objects.equals(this.serverAddress, target.serverAddress) && Objects.equals(this.vendorInfo, target.vendorInfo) && this.leaseDuration == target.leaseDuration && this.mtu == target.mtu)) {
            z = false;
        }
        return z;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.leaseDuration);
        dest.writeInt(this.mtu);
        NetworkUtils.parcelInetAddress(dest, this.serverAddress, flags);
        dest.writeString(this.vendorInfo);
    }

    private static void readFromParcel(DhcpResults dhcpResults, Parcel in) {
        StaticIpConfiguration.readFromParcel(dhcpResults, in);
        dhcpResults.leaseDuration = in.readInt();
        dhcpResults.mtu = in.readInt();
        dhcpResults.serverAddress = (Inet4Address) NetworkUtils.unparcelInetAddress(in);
        dhcpResults.vendorInfo = in.readString();
    }

    public boolean setIpAddress(String addrString, int prefixLength) {
        try {
            this.ipAddress = new LinkAddress((Inet4Address) NetworkUtils.numericToInetAddress(addrString), prefixLength);
            return false;
        } catch (ClassCastException | IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setIpAddress failed with addrString ");
            stringBuilder.append(addrString);
            stringBuilder.append("/");
            stringBuilder.append(prefixLength);
            Log.e(str, stringBuilder.toString());
            return true;
        }
    }

    public boolean setGateway(String addrString) {
        try {
            this.gateway = NetworkUtils.numericToInetAddress(addrString);
            return false;
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setGateway failed with addrString ");
            stringBuilder.append(addrString);
            Log.e(str, stringBuilder.toString());
            return true;
        }
    }

    public boolean addDns(String addrString) {
        if (!TextUtils.isEmpty(addrString)) {
            try {
                this.dnsServers.add(NetworkUtils.numericToInetAddress(addrString));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addDns failed with addrString ");
                stringBuilder.append(addrString);
                Log.e(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    public boolean setServerAddress(String addrString) {
        try {
            this.serverAddress = (Inet4Address) NetworkUtils.numericToInetAddress(addrString);
            return false;
        } catch (ClassCastException | IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setServerAddress failed with addrString ");
            stringBuilder.append(addrString);
            Log.e(str, stringBuilder.toString());
            return true;
        }
    }

    public void setLeaseDuration(int duration) {
        this.leaseDuration = duration;
    }

    public void setVendorInfo(String info) {
        this.vendorInfo = info;
    }

    public void setDomains(String newDomains) {
        this.domains = newDomains;
    }
}
