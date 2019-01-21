package java.net;

import android.system.ErrnoException;
import android.system.GaiException;
import android.system.OsConstants;
import android.system.StructAddrinfo;
import android.system.StructIcmpHdr;
import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Enumeration;
import libcore.io.IoBridge;
import libcore.io.Libcore;

class Inet6AddressImpl implements InetAddressImpl {
    private static final AddressCache addressCache = new AddressCache();
    private static InetAddress anyLocalAddress;
    private static InetAddress[] loopbackAddresses;

    Inet6AddressImpl() {
    }

    public InetAddress[] lookupAllHostAddr(String host, int netId) throws UnknownHostException {
        if (host == null || host.isEmpty()) {
            return loopbackAddresses();
        }
        InetAddress result = InetAddress.parseNumericAddressNoThrow(host);
        if (result == null) {
            return lookupHostByName(host, netId);
        }
        if (InetAddress.disallowDeprecatedFormats(host, result) != null) {
            return new InetAddress[]{InetAddress.disallowDeprecatedFormats(host, result)};
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Deprecated IPv4 address format: ");
        stringBuilder.append(host);
        throw new UnknownHostException(stringBuilder.toString());
    }

    private static InetAddress[] lookupHostByName(String host, int netId) throws UnknownHostException {
        BlockGuard.getThreadPolicy().onNetwork();
        Object cachedResult = addressCache.get(host, netId);
        if (cachedResult == null) {
            try {
                StructAddrinfo hints = new StructAddrinfo();
                hints.ai_flags = OsConstants.AI_ADDRCONFIG;
                hints.ai_family = OsConstants.AF_UNSPEC;
                hints.ai_socktype = OsConstants.SOCK_STREAM;
                InetAddress[] addresses = Libcore.os.android_getaddrinfo(host, hints, netId);
                for (InetAddress address : addresses) {
                    address.holder().hostName = host;
                    address.holder().originalHostName = host;
                }
                addressCache.put(host, netId, addresses);
                return addresses;
            } catch (GaiException gaiException) {
                if ((gaiException.getCause() instanceof ErrnoException) && ((ErrnoException) gaiException.getCause()).errno == OsConstants.EACCES) {
                    throw new SecurityException("Permission denied (missing INTERNET permission?)", gaiException);
                }
                String detailMessage = new StringBuilder();
                detailMessage.append("Unable to resolve host \"");
                detailMessage.append(host);
                detailMessage.append("\": ");
                detailMessage.append(Libcore.os.gai_strerror(gaiException.error));
                detailMessage = detailMessage.toString();
                addressCache.putUnknownHost(host, netId, detailMessage);
                throw gaiException.rethrowAsUnknownHostException(detailMessage);
            }
        } else if (cachedResult instanceof InetAddress[]) {
            return (InetAddress[]) cachedResult;
        } else {
            throw new UnknownHostException((String) cachedResult);
        }
    }

    public String getHostByAddr(byte[] addr) throws UnknownHostException {
        BlockGuard.getThreadPolicy().onNetwork();
        return getHostByAddr0(addr);
    }

    public void clearAddressCache() {
        addressCache.clear();
    }

    public boolean isReachable(InetAddress addr, int timeout, NetworkInterface netif, int ttl) throws IOException {
        InetAddress sourceAddr = null;
        if (netif != null) {
            Enumeration<InetAddress> it = netif.getInetAddresses();
            while (it.hasMoreElements()) {
                InetAddress inetaddr = (InetAddress) it.nextElement();
                if (inetaddr.getClass().isInstance(addr)) {
                    sourceAddr = inetaddr;
                    break;
                }
            }
            if (sourceAddr == null) {
                return false;
            }
        }
        if (icmpEcho(addr, timeout, sourceAddr, ttl)) {
            return true;
        }
        return tcpEcho(addr, timeout, sourceAddr, ttl);
    }

    private boolean tcpEcho(InetAddress addr, int timeout, InetAddress sourceAddr, int ttl) throws IOException {
        boolean z = true;
        try {
            FileDescriptor fd = IoBridge.socket(OsConstants.AF_INET6, OsConstants.SOCK_STREAM, 0);
            if (ttl > 0) {
                IoBridge.setSocketOption(fd, 25, Integer.valueOf(ttl));
            }
            if (sourceAddr != null) {
                IoBridge.bind(fd, sourceAddr, 0);
            }
            IoBridge.connect(fd, addr, 7, timeout);
            IoBridge.closeAndSignalBlockedThreads(fd);
            return true;
        } catch (IOException e) {
            Throwable cause = e.getCause();
            if (!((cause instanceof ErrnoException) && ((ErrnoException) cause).errno == OsConstants.ECONNREFUSED)) {
                z = false;
            }
            IoBridge.closeAndSignalBlockedThreads(null);
            return z;
        } catch (Throwable th) {
            IoBridge.closeAndSignalBlockedThreads(null);
            throw th;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:83:0x0112 A:{SYNTHETIC, Splitter:B:83:0x0112} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0106 A:{SYNTHETIC, Splitter:B:76:0x0106} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0112 A:{SYNTHETIC, Splitter:B:83:0x0112} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0106 A:{SYNTHETIC, Splitter:B:76:0x0106} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0112 A:{SYNTHETIC, Splitter:B:83:0x0112} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0106 A:{SYNTHETIC, Splitter:B:76:0x0106} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean icmpEcho(InetAddress addr, int timeout, InetAddress sourceAddr, int ttl) throws IOException {
        Throwable th;
        Throwable th2;
        FileDescriptor fd;
        InetAddress inetAddress = addr;
        InetAddress inetAddress2 = sourceAddr;
        FileDescriptor fd2 = null;
        FileDescriptor fd3;
        try {
            boolean isIPv4 = inetAddress instanceof Inet4Address;
            fd3 = IoBridge.socket(isIPv4 ? OsConstants.AF_INET : OsConstants.AF_INET6, OsConstants.SOCK_DGRAM, isIPv4 ? OsConstants.IPPROTO_ICMP : OsConstants.IPPROTO_ICMPV6);
            if (ttl > 0) {
                try {
                    IoBridge.setSocketOption(fd3, 25, Integer.valueOf(ttl));
                } catch (IOException e) {
                    if (fd3 != null) {
                    }
                    return false;
                } catch (Throwable th22) {
                    th = th22;
                    fd = fd3;
                    if (fd != null) {
                    }
                    throw th;
                }
            }
            if (inetAddress2 != null) {
                IoBridge.bind(fd3, inetAddress2, 0);
            }
            int to = timeout;
            int seq = 0;
            while (true) {
                int seq2 = seq;
                if (to > 0) {
                    byte[] packet;
                    int sockTo;
                    int seq3;
                    int icmpId;
                    byte[] received;
                    DatagramPacket receivedPacket;
                    int to2;
                    seq = 1000;
                    if (to < 1000) {
                        seq = to;
                    }
                    int sockTo2 = seq;
                    try {
                        IoBridge.setSocketOption(fd3, SocketOptions.SO_TIMEOUT, Integer.valueOf(sockTo2));
                        byte[] packet2 = StructIcmpHdr.IcmpEchoHdr(isIPv4, seq2).getBytes();
                        packet = packet2;
                        sockTo = sockTo2;
                        seq3 = seq2;
                        IoBridge.sendto(fd3, packet2, 0, packet2.length, 0, inetAddress, 0);
                        icmpId = IoBridge.getLocalInetSocketAddress(fd3).getPort();
                        received = new byte[packet.length];
                        receivedPacket = new DatagramPacket(received, packet.length);
                        to2 = to;
                        fd = fd3;
                    } catch (IOException e2) {
                        fd = fd3;
                        if (fd3 != null) {
                        }
                        return false;
                    } catch (Throwable th222) {
                        fd = fd3;
                        th = th222;
                        if (fd != null) {
                        }
                        throw th;
                    }
                    try {
                        if (IoBridge.recvfrom(true, fd3, received, 0, received.length, 0, receivedPacket, false) == packet.length) {
                            byte b;
                            if (isIPv4) {
                                b = (byte) OsConstants.ICMP_ECHOREPLY;
                            } else {
                                b = (byte) OsConstants.ICMP6_ECHO_REPLY;
                            }
                            byte expectedType = b;
                            if (receivedPacket.getAddress().equals(inetAddress) && received[0] == expectedType && received[4] == ((byte) (icmpId >> 8)) && received[5] == ((byte) icmpId)) {
                                to = seq3;
                                if (received[6] == ((byte) (to >> 8)) && received[7] == ((byte) to)) {
                                    if (fd != null) {
                                        try {
                                            Libcore.os.close(fd);
                                        } catch (ErrnoException e3) {
                                        }
                                    }
                                    return true;
                                }
                                icmpId = to + 1;
                                to = to2 - sockTo;
                                seq = icmpId;
                                fd3 = fd;
                            }
                        }
                        to = seq3;
                        icmpId = to + 1;
                        to = to2 - sockTo;
                        seq = icmpId;
                        fd3 = fd;
                    } catch (IOException e4) {
                        fd3 = fd;
                        if (fd3 != null) {
                        }
                        return false;
                    } catch (Throwable th3) {
                        th222 = th3;
                        th = th222;
                        if (fd != null) {
                        }
                        throw th;
                    }
                }
                fd = fd3;
                if (fd != null) {
                    try {
                        Libcore.os.close(fd);
                    } catch (ErrnoException e5) {
                    }
                }
                fd3 = fd;
            }
        } catch (IOException e6) {
            fd3 = fd2;
            if (fd3 != null) {
                try {
                    Libcore.os.close(fd3);
                } catch (ErrnoException e7) {
                }
            }
            return false;
        } catch (Throwable th4) {
            th222 = th4;
            fd = fd2;
            th = th222;
            if (fd != null) {
                try {
                    Libcore.os.close(fd);
                } catch (ErrnoException e8) {
                }
            }
            throw th;
        }
        return false;
    }

    public InetAddress anyLocalAddress() {
        InetAddress inetAddress;
        synchronized (Inet6AddressImpl.class) {
            if (anyLocalAddress == null) {
                Inet6Address anyAddress = new Inet6Address();
                anyAddress.holder().hostName = "::";
                anyLocalAddress = anyAddress;
            }
            inetAddress = anyLocalAddress;
        }
        return inetAddress;
    }

    public InetAddress[] loopbackAddresses() {
        InetAddress[] inetAddressArr;
        synchronized (Inet6AddressImpl.class) {
            if (loopbackAddresses == null) {
                loopbackAddresses = new InetAddress[]{Inet6Address.LOOPBACK, Inet4Address.LOOPBACK};
            }
            inetAddressArr = loopbackAddresses;
        }
        return inetAddressArr;
    }

    private String getHostByAddr0(byte[] addr) throws UnknownHostException {
        InetAddress hostaddr = InetAddress.getByAddress(addr);
        try {
            return Libcore.os.getnameinfo(hostaddr, OsConstants.NI_NAMEREQD);
        } catch (GaiException e) {
            UnknownHostException uhe = new UnknownHostException(hostaddr.toString());
            uhe.initCause(e);
            throw uhe;
        }
    }
}
