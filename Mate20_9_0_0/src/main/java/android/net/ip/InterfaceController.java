package android.net.ip;

import android.net.INetd;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.OsConstants;
import java.net.InetAddress;

public class InterfaceController {
    private static final boolean DBG = false;
    private final String mIfName;
    private final SharedLog mLog;
    private final INetworkManagementService mNMS;
    private final INetd mNetd;

    public InterfaceController(String ifname, INetworkManagementService nms, INetd netd, SharedLog log) {
        this.mIfName = ifname;
        this.mNMS = nms;
        this.mNetd = netd;
        this.mLog = log;
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0012 A:{Splitter: B:1:0x0009, ExcHandler: java.lang.IllegalStateException (r2_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x0012, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0013, code:
            logError("IPv4 configuration failed: %s", r2);
     */
    /* JADX WARNING: Missing block: B:6:0x001d, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setIPv4Address(LinkAddress address) {
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        ifcg.setLinkAddress(address);
        try {
            this.mNMS.setInterfaceConfig(this.mIfName, ifcg);
            return true;
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0019 A:{Splitter: B:1:0x0001, ExcHandler: java.lang.IllegalStateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x0019, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x001a, code:
            logError("Failed to clear IPv4 address on interface %s: %s", r6.mIfName, r1);
     */
    /* JADX WARNING: Missing block: B:6:0x0029, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean clearIPv4Address() {
        try {
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(new LinkAddress("0.0.0.0/0"));
            this.mNMS.setInterfaceConfig(this.mIfName, ifcg);
            return true;
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000a A:{Splitter: B:1:0x0001, ExcHandler: java.lang.IllegalStateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000a, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000b, code:
            logError("enabling IPv6 failed: %s", r1);
     */
    /* JADX WARNING: Missing block: B:6:0x0015, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean enableIPv6() {
        try {
            this.mNMS.enableIpv6(this.mIfName);
            return true;
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000a A:{Splitter: B:1:0x0001, ExcHandler: java.lang.IllegalStateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000a, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000b, code:
            logError("disabling IPv6 failed: %s", r1);
     */
    /* JADX WARNING: Missing block: B:6:0x0015, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean disableIPv6() {
        try {
            this.mNMS.disableIpv6(this.mIfName);
            return true;
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000a A:{Splitter: B:1:0x0001, ExcHandler: java.lang.IllegalStateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000a, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000b, code:
            logError("error setting IPv6 privacy extensions: %s", r1);
     */
    /* JADX WARNING: Missing block: B:6:0x0015, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setIPv6PrivacyExtensions(boolean enabled) {
        try {
            this.mNMS.setInterfaceIpv6PrivacyExtensions(this.mIfName, enabled);
            return true;
        } catch (Exception e) {
        }
    }

    public boolean setIPv6AddrGenModeIfSupported(int mode) {
        try {
            this.mNMS.setIPv6AddrGenMode(this.mIfName, mode);
        } catch (RemoteException e) {
            logError("Unable to set IPv6 addrgen mode: %s", e);
            return false;
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode != OsConstants.EOPNOTSUPP) {
                logError("Unable to set IPv6 addrgen mode: %s", e2);
                return false;
            }
        }
        return true;
    }

    public boolean addAddress(LinkAddress addr) {
        return addAddress(addr.getAddress(), addr.getPrefixLength());
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000e A:{Splitter: B:1:0x0001, ExcHandler: android.os.ServiceSpecificException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000e, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000f, code:
            logError("failed to add %s/%d: %s", r7, java.lang.Integer.valueOf(r8), r1);
     */
    /* JADX WARNING: Missing block: B:6:0x0023, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean addAddress(InetAddress ip, int prefixLen) {
        try {
            this.mNetd.interfaceAddAddress(this.mIfName, ip.getHostAddress(), prefixLen);
            return true;
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000e A:{Splitter: B:1:0x0001, ExcHandler: android.os.ServiceSpecificException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000e, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000f, code:
            logError("failed to remove %s/%d: %s", r7, java.lang.Integer.valueOf(r8), r1);
     */
    /* JADX WARNING: Missing block: B:6:0x0023, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean removeAddress(InetAddress ip, int prefixLen) {
        try {
            this.mNetd.interfaceDelAddress(this.mIfName, ip.getHostAddress(), prefixLen);
            return true;
        } catch (Exception e) {
        }
    }

    public boolean clearAllAddresses() {
        try {
            this.mNMS.clearInterfaceAddresses(this.mIfName);
            return true;
        } catch (Exception e) {
            logError("Failed to clear addresses: %s", e);
            return false;
        }
    }

    private void logError(String fmt, Object... args) {
        this.mLog.e(String.format(fmt, args));
    }
}
