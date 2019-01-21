package android.net.arp;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.os.SystemClock;
import android.util.Log;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;
import libcore.net.RawSocket;

public class HWArpPeer {
    private static final int ARP_LENGTH = 28;
    private static final boolean DBG = false;
    private static final int ETHERNET_TYPE = 1;
    private static final int IPV4_LENGTH = 4;
    private static final int MAC_ADDR_LENGTH = 6;
    private static final int MAX_LENGTH = 1500;
    private static final String TAG = "HWArpPeer";
    private final byte[] L2_BROADCAST;
    private String mInterfaceName;
    private final InetAddress mMyAddr;
    private final byte[] mMyMac = new byte[6];
    private final InetAddress mPeer;
    private final RawSocket mSocket;

    public HWArpPeer(String interfaceName, InetAddress myAddr, String mac, InetAddress peer) throws SocketException {
        this.mInterfaceName = interfaceName;
        this.mMyAddr = myAddr;
        if (mac != null) {
            for (int i = 0; i < 6; i++) {
                this.mMyMac[i] = (byte) Integer.parseInt(mac.substring(i * 3, (i * 3) + 2), 16);
            }
        }
        if ((myAddr instanceof Inet6Address) || (peer instanceof Inet6Address)) {
            throw new IllegalArgumentException("IPv6 unsupported");
        }
        this.mPeer = peer;
        this.L2_BROADCAST = new byte[6];
        Arrays.fill(this.L2_BROADCAST, (byte) -1);
        this.mSocket = new RawSocket(this.mInterfaceName, (short) 2054);
    }

    public byte[] doArp(int timeoutMillis) {
        if (this.mMyAddr == null) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.allocate(MAX_LENGTH);
        byte[] desiredIp = this.mPeer.getAddress();
        long timeout = SystemClock.elapsedRealtime() + ((long) timeoutMillis);
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        byte b = (byte) 1;
        buf.putShort((short) 1);
        buf.putShort((short) 2048);
        buf.put((byte) 6);
        byte b2 = (byte) 4;
        buf.put((byte) 4);
        buf.putShort((short) 1);
        buf.put(this.mMyMac);
        buf.put(this.mMyAddr.getAddress());
        buf.put(new byte[6]);
        buf.put(desiredIp);
        buf.flip();
        int i = 0;
        this.mSocket.write(this.L2_BROADCAST, buf.array(), 0, buf.limit());
        byte[] recvBuf = new byte[MAX_LENGTH];
        while (SystemClock.elapsedRealtime() < timeout) {
            int i2;
            long duration = timeout - SystemClock.elapsedRealtime();
            if (this.mSocket.read(recvBuf, 0, recvBuf.length, -1, (int) duration) < 28 || recvBuf[i] != (byte) 0 || recvBuf[b] != b) {
                b2 = b;
                i2 = i;
            } else if (recvBuf[2] == (byte) 8 && recvBuf[3] == (byte) 0 && recvBuf[b2] == (byte) 6 && recvBuf[5] == b2 && recvBuf[6] == (byte) 0 && recvBuf[7] == (byte) 2 && recvBuf[14] == desiredIp[i]) {
                b2 = (byte) 1;
                if (recvBuf[15] == desiredIp[1] && recvBuf[16] == desiredIp[2] && recvBuf[17] == desiredIp[3]) {
                    byte[] result = new byte[6];
                    System.arraycopy(recvBuf, 8, result, 0, 6);
                    return result;
                }
                i2 = 0;
            } else {
                i2 = i;
                b2 = (byte) 1;
            }
            b = b2;
            i = i2;
            b2 = (byte) 4;
        }
        return null;
    }

    public HWMultiGW getGateWayARPResponses(int timeoutMillis) {
        if (this.mMyAddr == null) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.allocate(MAX_LENGTH);
        byte[] desiredIp = this.mPeer.getAddress();
        long timeout = SystemClock.elapsedRealtime() + ((long) timeoutMillis);
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 1);
        buf.putShort((short) 2048);
        buf.put((byte) 6);
        buf.put((byte) 4);
        buf.putShort((short) 1);
        buf.put(this.mMyMac);
        buf.put(this.mMyAddr.getAddress());
        buf.put(new byte[6]);
        buf.put(desiredIp);
        buf.flip();
        int i = 0;
        this.mSocket.write(this.L2_BROADCAST, buf.array(), 0, buf.limit());
        byte[] recvBuf = new byte[MAX_LENGTH];
        HWMultiGW resultGW = new HWMultiGW();
        byte[] result = new byte[6];
        long lStart = SystemClock.elapsedRealtime();
        long lEnd = 0;
        while (true) {
            long lEnd2 = lEnd;
            if (SystemClock.elapsedRealtime() >= timeout) {
                break;
            }
            int i2;
            int i3;
            int i4;
            int i5;
            int i6;
            ByteBuffer buf2 = buf;
            long duration = timeout - SystemClock.elapsedRealtime();
            if (this.mSocket.read(recvBuf, 0, recvBuf.length, -1, (int) duration) < 28 || recvBuf[i] != (byte) 0) {
                i2 = i;
                i3 = 6;
            } else {
                if (recvBuf[1] == (byte) 1) {
                    if (recvBuf[2] == (byte) 8 && recvBuf[3] == (byte) 0) {
                        if (recvBuf[4] != (byte) 6 || recvBuf[5] != (byte) 4) {
                            i3 = 6;
                            i2 = 0;
                        } else if (recvBuf[6] == (byte) 0 && recvBuf[7] == (byte) 2 && recvBuf[14] == desiredIp[0]) {
                            i4 = 1;
                            if (recvBuf[15] == desiredIp[1] && recvBuf[16] == desiredIp[2] && recvBuf[17] == desiredIp[3]) {
                                long lEnd3 = SystemClock.elapsedRealtime();
                                resultGW.setArpRTT(lEnd3 - lStart);
                                i3 = 6;
                                i2 = 0;
                                System.arraycopy(recvBuf, 8, result, 0, 6);
                                resultGW.setGWMACAddr(result);
                                lEnd = lEnd3;
                                i5 = i3;
                                i = i2;
                                i6 = i4;
                                buf = buf2;
                            } else {
                                i3 = 6;
                                i2 = 0;
                            }
                        }
                    }
                    i3 = 6;
                    i2 = 0;
                } else {
                    i4 = 1;
                    i2 = i;
                    i3 = 6;
                }
                lEnd = lEnd2;
                i5 = i3;
                i = i2;
                i6 = i4;
                buf = buf2;
            }
            i4 = 1;
            lEnd = lEnd2;
            i5 = i3;
            i = i2;
            i6 = i4;
            buf = buf2;
        }
        if (resultGW.getGWNum() > 0) {
            return resultGW;
        }
        return null;
    }

    public byte[] doGratuitousArp(int timeoutMillis) {
        ByteBuffer buf = ByteBuffer.allocate(MAX_LENGTH);
        byte[] desiredIp = this.mMyAddr.getAddress();
        long timeout = SystemClock.elapsedRealtime() + ((long) timeoutMillis);
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        byte b = (byte) 1;
        buf.putShort((short) 1);
        buf.putShort((short) 2048);
        buf.put((byte) 6);
        byte b2 = (byte) 4;
        buf.put((byte) 4);
        buf.putShort((short) 1);
        buf.put(this.mMyMac);
        buf.put(this.mMyAddr.getAddress());
        buf.put(new byte[6]);
        buf.put(desiredIp);
        buf.flip();
        int i = 0;
        this.mSocket.write(this.L2_BROADCAST, buf.array(), 0, buf.limit());
        byte[] recvBuf = new byte[MAX_LENGTH];
        while (SystemClock.elapsedRealtime() < timeout) {
            int i2;
            if (this.mSocket.read(recvBuf, 0, recvBuf.length, -1, (int) (timeout - SystemClock.elapsedRealtime())) < 28 || recvBuf[i] != (byte) 0 || recvBuf[b] != b) {
                b2 = b;
                i2 = i;
            } else if (recvBuf[2] == (byte) 8 && recvBuf[3] == (byte) 0 && recvBuf[b2] == (byte) 6 && recvBuf[5] == b2 && recvBuf[6] == (byte) 0 && recvBuf[7] == (byte) 2 && recvBuf[14] == desiredIp[i]) {
                b2 = (byte) 1;
                if (recvBuf[15] == desiredIp[1] && recvBuf[16] == desiredIp[2] && recvBuf[17] == desiredIp[3]) {
                    byte[] result = new byte[6];
                    System.arraycopy(recvBuf, 8, result, 0, 6);
                    return result;
                }
                i2 = 0;
            } else {
                i2 = i;
                b2 = (byte) 1;
            }
            b = b2;
            i = i2;
            b2 = (byte) 4;
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:27:0x008f, code skipped:
            if (r3 == null) goto L_0x0093;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean doArp(String myMacAddress, LinkProperties linkProperties, int timeoutMillis, int numArpPings, int minArpResponses) {
        boolean success;
        String str;
        StringBuilder stringBuilder;
        String interfaceName = linkProperties.getInterfaceName();
        InetAddress inetAddress = null;
        InetAddress gateway = null;
        Iterator it = linkProperties.getLinkAddresses().iterator();
        if (it.hasNext()) {
            inetAddress = ((LinkAddress) it.next()).getAddress();
        }
        it = linkProperties.getRoutes().iterator();
        if (it.hasNext()) {
            gateway = ((RouteInfo) it.next()).getGateway();
        }
        HWArpPeer peer = null;
        try {
            peer = new HWArpPeer(interfaceName, inetAddress, myMacAddress, gateway);
            boolean z = false;
            int responses = 0;
            for (int i = 0; i < numArpPings; i++) {
                if (peer.doArp(timeoutMillis) != null) {
                    responses++;
                }
            }
            if (responses >= minArpResponses) {
                z = true;
            }
            success = z;
        } catch (SocketException se) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("ARP test initiation failure: ");
            stringBuilder.append(se);
            Log.e(str, stringBuilder.toString());
            success = true;
        } catch (Exception ae) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("ARP failre: ");
            stringBuilder.append(ae);
            Log.e(str, stringBuilder.toString());
            success = true;
            if (peer != null) {
            }
        } catch (Throwable th) {
            if (peer != null) {
                peer.close();
            }
        }
        peer.close();
        return success;
    }

    public void close() {
        try {
            this.mSocket.close();
        } catch (IOException e) {
        }
    }
}
