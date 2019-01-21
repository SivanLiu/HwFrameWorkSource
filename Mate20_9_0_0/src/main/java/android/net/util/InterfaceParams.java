package android.net.util;

import android.net.MacAddress;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.net.NetworkInterface;
import java.net.SocketException;

public class InterfaceParams {
    public final int defaultMtu;
    public final int index;
    public final MacAddress macAddr;
    public final String name;

    public static InterfaceParams getByName(String name) {
        NetworkInterface netif = getNetworkInterfaceByName(name);
        if (netif == null) {
            return null;
        }
        try {
            return new InterfaceParams(name, netif.getIndex(), getMacAddress(netif), netif.getMTU());
        } catch (IllegalArgumentException | SocketException e) {
            return null;
        }
    }

    public InterfaceParams(String name, int index, MacAddress macAddr) {
        this(name, index, macAddr, NetworkConstants.ETHER_MTU);
    }

    public InterfaceParams(String name, int index, MacAddress macAddr, int defaultMtu) {
        boolean z = true;
        Preconditions.checkArgument(TextUtils.isEmpty(name) ^ 1, "impossible interface name");
        if (index <= 0) {
            z = false;
        }
        Preconditions.checkArgument(z, "invalid interface index");
        this.name = name;
        this.index = index;
        this.macAddr = macAddr != null ? macAddr : MacAddress.ALL_ZEROS_ADDRESS;
        int i = 1280;
        if (defaultMtu > 1280) {
            i = defaultMtu;
        }
        this.defaultMtu = i;
    }

    public String toString() {
        return String.format("%s/%d/%s/%d", new Object[]{this.name, Integer.valueOf(this.index), this.macAddr, Integer.valueOf(this.defaultMtu)});
    }

    private static NetworkInterface getNetworkInterfaceByName(String name) {
        try {
            return NetworkInterface.getByName(name);
        } catch (NullPointerException | SocketException e) {
            return null;
        }
    }

    private static MacAddress getMacAddress(NetworkInterface netif) {
        try {
            return MacAddress.fromBytes(netif.getHardwareAddress());
        } catch (IllegalArgumentException | NullPointerException | SocketException e) {
            return null;
        }
    }
}
