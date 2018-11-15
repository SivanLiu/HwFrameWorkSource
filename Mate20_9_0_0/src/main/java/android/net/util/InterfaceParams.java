package android.net.util;

import android.net.MacAddress;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.net.NetworkInterface;

public class InterfaceParams {
    public final int defaultMtu;
    public final int index;
    public final MacAddress macAddr;
    public final String name;

    /* JADX WARNING: Removed duplicated region for block: B:7:0x001a A:{Splitter: B:4:0x000c, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Missing block: B:8:0x001b, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static InterfaceParams getByName(String name) {
        NetworkInterface netif = getNetworkInterfaceByName(name);
        if (netif == null) {
            return null;
        }
        try {
            return new InterfaceParams(name, netif.getIndex(), getMacAddress(netif), netif.getMTU());
        } catch (IllegalArgumentException e) {
        }
    }

    public InterfaceParams(String name, int index, MacAddress macAddr) {
        this(name, index, macAddr, NetworkConstants.ETHER_MTU);
    }

    public InterfaceParams(String name, int index, MacAddress macAddr, int defaultMtu) {
        boolean z = true;
        Preconditions.checkArgument(TextUtils.isEmpty(name) ^ true, "impossible interface name");
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

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0005 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Missing block: B:5:0x0007, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static NetworkInterface getNetworkInterfaceByName(String name) {
        try {
            return NetworkInterface.getByName(name);
        } catch (NullPointerException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0009 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0009 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Missing block: B:5:0x000b, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static MacAddress getMacAddress(NetworkInterface netif) {
        try {
            return MacAddress.fromBytes(netif.getHardwareAddress());
        } catch (IllegalArgumentException e) {
        }
    }
}
