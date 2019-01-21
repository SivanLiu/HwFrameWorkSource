package android.net.dhcp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;

public class HwArpClient {
    private static final int DEFAULT_FAST_NUM_ARP_PINGS = 1;
    private static final int DEFAULT_FAST_PING_TIMEOUT_MS = 50;
    private static final int DEFAULT_SLOW_NUM_ARP_PINGS = 3;
    private static final int DEFAULT_SLOW_PING_TIMEOUT_MS = 800;
    private static final String TAG = "HwArpClient";
    private ConnectivityManager mCM = null;
    private Context mContext = null;
    private WifiManager mWM = null;

    public HwArpClient(Context context) {
        this.mContext = context;
    }

    public boolean doFastArpTest(Inet4Address requestedAddress) {
        return doArp(1, 50, requestedAddress, false);
    }

    public boolean doSlowArpTest(Inet4Address requestedAddress) {
        return doArp(3, DEFAULT_SLOW_PING_TIMEOUT_MS, requestedAddress, false);
    }

    public boolean doGatewayArpTest(Inet4Address requestedAddress) {
        return doArp(3, DEFAULT_SLOW_PING_TIMEOUT_MS, requestedAddress, true);
    }

    /* JADX WARNING: Missing block: B:11:0x0041, code skipped:
            if (r0 != null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:15:0x004a, code skipped:
            if (r0 == null) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:23:0x007e, code skipped:
            if (r0 == null) goto L_0x0081;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean doArp(int arpNum, int timeout, Inet4Address requestedAddress, boolean fillSenderIp) {
        String str;
        StringBuilder stringBuilder;
        HWArpPacket peer = null;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("doArp() arpnum:");
        stringBuilder2.append(arpNum);
        stringBuilder2.append(", timeout:");
        stringBuilder2.append(timeout);
        stringBuilder2.append(", fillSenderIp = ");
        stringBuilder2.append(fillSenderIp);
        Log.d(str2, stringBuilder2.toString());
        try {
            peer = constructArpPacket();
            for (int i = 0; i < arpNum; i++) {
                if (peer.doArp(timeout, requestedAddress, fillSenderIp)) {
                    if (peer != null) {
                        peer.close();
                    }
                    return true;
                }
            }
        } catch (SocketException se) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception in ARP test: ");
            stringBuilder.append(se);
            Log.e(str, stringBuilder.toString());
        } catch (IllegalArgumentException ae) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception in ARP test:");
            stringBuilder.append(ae);
            Log.e(str, stringBuilder.toString());
            if (peer != null) {
                peer.close();
            }
            return false;
        } catch (Exception e) {
        } catch (Throwable th) {
            if (peer != null) {
                peer.close();
            }
        }
    }

    private HWArpPacket constructArpPacket() throws SocketException {
        if (this.mWM == null) {
            this.mWM = (WifiManager) this.mContext.getSystemService("wifi");
        }
        WifiInfo wifiInfo = this.mWM.getConnectionInfo();
        LinkProperties linkProperties = getCurrentLinkProperties();
        String linkIFName = linkProperties != null ? linkProperties.getInterfaceName() : "wlan0";
        InetAddress linkAddr = null;
        String macAddr = null;
        if (wifiInfo != null) {
            macAddr = wifiInfo.getMacAddress();
            linkAddr = NetworkUtils.intToInetAddress(wifiInfo.getIpAddress());
        }
        return new HWArpPacket(linkIFName, linkAddr, macAddr);
    }

    private LinkProperties getCurrentLinkProperties() {
        if (this.mCM == null) {
            this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mCM.getLinkProperties(1);
    }
}
