package com.android.server.connectivity.tethering;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.util.SharedLog;
import android.telephony.TelephonyManager;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;

public class TetheringConfiguration {
    private static final String[] DHCP_DEFAULT_RANGE = new String[]{"192.168.42.2", "192.168.42.254", "192.168.43.2", "192.168.43.254", "192.168.44.2", "192.168.44.254", "192.168.45.2", "192.168.45.254", "192.168.46.2", "192.168.46.254", "192.168.47.2", "192.168.47.254", "192.168.48.2", "192.168.48.254", "192.168.49.2", "192.168.49.254", "192.168.50.2", "192.168.50.254"};
    public static final int DUN_NOT_REQUIRED = 0;
    public static final int DUN_REQUIRED = 1;
    public static final int DUN_UNSPECIFIED = 2;
    private static final String TAG = TetheringConfiguration.class.getSimpleName();
    private final String[] DEFAULT_IPV4_DNS = new String[]{"8.8.4.4", "8.8.8.8"};
    public final String[] defaultIPv4DNS;
    public final String[] dhcpRanges;
    public final int dunCheck;
    public final boolean isDunRequired;
    public final Collection<Integer> preferredUpstreamIfaceTypes;
    public final String[] tetherableBluetoothRegexs;
    public final String[] tetherableUsbRegexs;
    public final String[] tetherableWifiRegexs;

    public TetheringConfiguration(Context ctx, SharedLog log) {
        SharedLog configLog = log.forSubComponent("config");
        this.tetherableUsbRegexs = ctx.getResources().getStringArray(17236039);
        this.tetherableWifiRegexs = ctx.getResources().getStringArray(17236040);
        this.tetherableBluetoothRegexs = ctx.getResources().getStringArray(17236036);
        this.dunCheck = checkDunRequired(ctx);
        configLog.log("DUN check returned: " + dunCheckString(this.dunCheck));
        this.preferredUpstreamIfaceTypes = getUpstreamIfaceTypes(ctx, this.dunCheck);
        this.isDunRequired = this.preferredUpstreamIfaceTypes.contains(Integer.valueOf(4));
        this.dhcpRanges = getDhcpRanges(ctx);
        this.defaultIPv4DNS = copy(this.DEFAULT_IPV4_DNS);
        configLog.log(toString());
    }

    public boolean isUsb(String iface) {
        return matchesDownstreamRegexs(iface, this.tetherableUsbRegexs);
    }

    public boolean isWifi(String iface) {
        return matchesDownstreamRegexs(iface, this.tetherableWifiRegexs);
    }

    public boolean isBluetooth(String iface) {
        return matchesDownstreamRegexs(iface, this.tetherableBluetoothRegexs);
    }

    public void dump(PrintWriter pw) {
        dumpStringArray(pw, "tetherableUsbRegexs", this.tetherableUsbRegexs);
        dumpStringArray(pw, "tetherableWifiRegexs", this.tetherableWifiRegexs);
        dumpStringArray(pw, "tetherableBluetoothRegexs", this.tetherableBluetoothRegexs);
        pw.print("isDunRequired: ");
        pw.println(this.isDunRequired);
        dumpStringArray(pw, "preferredUpstreamIfaceTypes", preferredUpstreamNames(this.preferredUpstreamIfaceTypes));
        dumpStringArray(pw, "dhcpRanges", this.dhcpRanges);
        dumpStringArray(pw, "defaultIPv4DNS", this.defaultIPv4DNS);
    }

    public String toString() {
        StringJoiner sj = new StringJoiner(" ");
        sj.add(String.format("tetherableUsbRegexs:%s", new Object[]{makeString(this.tetherableUsbRegexs)}));
        sj.add(String.format("tetherableWifiRegexs:%s", new Object[]{makeString(this.tetherableWifiRegexs)}));
        sj.add(String.format("tetherableBluetoothRegexs:%s", new Object[]{makeString(this.tetherableBluetoothRegexs)}));
        sj.add(String.format("isDunRequired:%s", new Object[]{Boolean.valueOf(this.isDunRequired)}));
        sj.add(String.format("preferredUpstreamIfaceTypes:%s", new Object[]{makeString(preferredUpstreamNames(this.preferredUpstreamIfaceTypes))}));
        return String.format("TetheringConfiguration{%s}", new Object[]{sj.toString()});
    }

    private static void dumpStringArray(PrintWriter pw, String label, String[] values) {
        pw.print(label);
        pw.print(": ");
        if (values != null) {
            StringJoiner sj = new StringJoiner(", ", "[", "]");
            for (String value : values) {
                sj.add(value);
            }
            pw.print(sj.toString());
        } else {
            pw.print("null");
        }
        pw.println();
    }

    private static String makeString(String[] strings) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (String s : strings) {
            sj.add(s);
        }
        return sj.toString();
    }

    private static String[] preferredUpstreamNames(Collection<Integer> upstreamTypes) {
        String[] upstreamNames = null;
        if (upstreamTypes != null) {
            upstreamNames = new String[upstreamTypes.size()];
            int i = 0;
            for (Integer netType : upstreamTypes) {
                upstreamNames[i] = ConnectivityManager.getNetworkTypeName(netType.intValue());
                i++;
            }
        }
        return upstreamNames;
    }

    public static int checkDunRequired(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService("phone");
        return tm != null ? tm.getTetherApnRequired() : 2;
    }

    private static String dunCheckString(int dunCheck) {
        switch (dunCheck) {
            case 0:
                return "DUN_NOT_REQUIRED";
            case 1:
                return "DUN_REQUIRED";
            case 2:
                return "DUN_UNSPECIFIED";
            default:
                return String.format("UNKNOWN (%s)", new Object[]{Integer.valueOf(dunCheck)});
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Collection<Integer> getUpstreamIfaceTypes(Context ctx, int dunCheck) {
        int[] ifaceTypes = ctx.getResources().getIntArray(17236038);
        ArrayList<Integer> upstreamIfaceTypes = new ArrayList(ifaceTypes.length);
        for (int i : ifaceTypes) {
            switch (i) {
                case 0:
                case 5:
                    if (dunCheck == 1) {
                        break;
                    }
                case 4:
                    break;
                default:
                    upstreamIfaceTypes.add(Integer.valueOf(i));
                    break;
            }
        }
        Slog.d(TAG, "upstreamIfaceTypes " + upstreamIfaceTypes + ", dunCheck is " + dunCheck);
        if (dunCheck == 1) {
            appendIfNotPresent(upstreamIfaceTypes, 4);
        } else if (dunCheck == 0) {
            appendIfNotPresent(upstreamIfaceTypes, 0);
            appendIfNotPresent(upstreamIfaceTypes, 5);
        } else {
            if (!containsOneOf(upstreamIfaceTypes, Integer.valueOf(4), Integer.valueOf(0), Integer.valueOf(5))) {
                upstreamIfaceTypes.add(Integer.valueOf(0));
                upstreamIfaceTypes.add(Integer.valueOf(5));
            }
        }
        prependIfNotPresent(upstreamIfaceTypes, 9);
        return upstreamIfaceTypes;
    }

    private static boolean matchesDownstreamRegexs(String iface, String[] regexs) {
        for (String regex : regexs) {
            if (iface.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    private static String[] getDhcpRanges(Context ctx) {
        String[] fromResource = ctx.getResources().getStringArray(17236037);
        if (fromResource.length <= 0 || fromResource.length % 2 != 0) {
            return copy(DHCP_DEFAULT_RANGE);
        }
        return fromResource;
    }

    private static String[] copy(String[] strarray) {
        return (String[]) Arrays.copyOf(strarray, strarray.length);
    }

    private static void prependIfNotPresent(ArrayList<Integer> list, int value) {
        if (!list.contains(Integer.valueOf(value))) {
            list.add(0, Integer.valueOf(value));
        }
    }

    private static void appendIfNotPresent(ArrayList<Integer> list, int value) {
        if (!list.contains(Integer.valueOf(value))) {
            list.add(Integer.valueOf(value));
        }
    }

    private static boolean containsOneOf(ArrayList<Integer> list, Integer... values) {
        for (Integer value : values) {
            if (list.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
