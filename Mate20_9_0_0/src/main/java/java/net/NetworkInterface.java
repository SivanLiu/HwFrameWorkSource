package java.net;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructIfaddrs;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import libcore.io.IoUtils;
import libcore.io.Libcore;

public final class NetworkInterface {
    private static final int defaultIndex;
    private static final NetworkInterface defaultInterface = DefaultInterface.getDefault();
    private InetAddress[] addrs;
    private InterfaceAddress[] bindings;
    private List<NetworkInterface> childs;
    private String displayName;
    private byte[] hardwareAddr;
    private int index;
    private String name;
    private NetworkInterface parent = null;
    private boolean virtual = false;

    static {
        if (defaultInterface != null) {
            defaultIndex = defaultInterface.getIndex();
        } else {
            defaultIndex = 0;
        }
    }

    NetworkInterface() {
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.index = index;
        this.addrs = addrs;
    }

    public String getName() {
        return this.name;
    }

    public Enumeration<InetAddress> getInetAddresses() {
        return new Enumeration<InetAddress>() {
            private int count = 0;
            private int i = 0;
            private InetAddress[] local_addrs;

            {
                int j = 0;
                this.local_addrs = new InetAddress[NetworkInterface.this.addrs.length];
                boolean trusted = true;
                SecurityManager sec = System.getSecurityManager();
                if (sec != null) {
                    try {
                        sec.checkPermission(new NetPermission("getNetworkInformation"));
                    } catch (SecurityException e) {
                        trusted = false;
                    }
                }
                while (j < NetworkInterface.this.addrs.length) {
                    if (!(sec == null || trusted)) {
                        try {
                            sec.checkConnect(NetworkInterface.this.addrs[j].getHostAddress(), -1);
                        } catch (SecurityException e2) {
                        }
                    }
                    InetAddress[] inetAddressArr = this.local_addrs;
                    int i = this.count;
                    this.count = i + 1;
                    inetAddressArr[i] = NetworkInterface.this.addrs[j];
                    j++;
                }
            }

            public InetAddress nextElement() {
                if (this.i < this.count) {
                    InetAddress[] inetAddressArr = this.local_addrs;
                    int i = this.i;
                    this.i = i + 1;
                    return inetAddressArr[i];
                }
                throw new NoSuchElementException();
            }

            public boolean hasMoreElements() {
                return this.i < this.count;
            }
        };
    }

    public List<InterfaceAddress> getInterfaceAddresses() {
        List<InterfaceAddress> lst = new ArrayList(1);
        if (this.bindings != null) {
            SecurityManager sec = System.getSecurityManager();
            for (int j = 0; j < this.bindings.length; j++) {
                if (sec != null) {
                    try {
                        sec.checkConnect(this.bindings[j].getAddress().getHostAddress(), -1);
                    } catch (SecurityException e) {
                    }
                }
                lst.add(this.bindings[j]);
            }
        }
        return lst;
    }

    public Enumeration<NetworkInterface> getSubInterfaces() {
        return Collections.enumeration(this.childs);
    }

    public NetworkInterface getParent() {
        return this.parent;
    }

    public int getIndex() {
        return this.index;
    }

    public String getDisplayName() {
        return "".equals(this.displayName) ? null : this.displayName;
    }

    public static NetworkInterface getByName(String name) throws SocketException {
        if (name != null) {
            for (NetworkInterface ni : getAll()) {
                if (ni.getName().equals(name)) {
                    return ni;
                }
            }
            return null;
        }
        throw new NullPointerException();
    }

    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index >= 0) {
            for (NetworkInterface ni : getAll()) {
                if (ni.getIndex() == index) {
                    return ni;
                }
            }
            return null;
        }
        throw new IllegalArgumentException("Interface index can't be negative");
    }

    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        } else if ((addr instanceof Inet4Address) || (addr instanceof Inet6Address)) {
            for (NetworkInterface ni : getAll()) {
                Iterator it = Collections.list(ni.getInetAddresses()).iterator();
                while (it.hasNext()) {
                    if (((InetAddress) it.next()).equals(addr)) {
                        return ni;
                    }
                }
            }
            return null;
        } else {
            throw new IllegalArgumentException("invalid address type");
        }
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        NetworkInterface[] netifs = getAll();
        if (netifs.length == 0) {
            return null;
        }
        return Collections.enumeration(Arrays.asList(netifs));
    }

    private static NetworkInterface[] getAll() throws SocketException {
        HashMap inetMap = new HashMap();
        try {
            String name;
            int index;
            for (StructIfaddrs ifa : Libcore.os.getifaddrs()) {
                name = ifa.ifa_name;
                List<StructIfaddrs> list = (List) inetMap.get(name);
                List<StructIfaddrs> ifas = list;
                if (list == null) {
                    ifas = new ArrayList();
                    inetMap.put(name, ifas);
                }
                ifas.add(ifa);
            }
            Map<String, NetworkInterface> nis = new HashMap(inetMap.size());
            for (Entry<String, List<StructIfaddrs>> e : inetMap.entrySet()) {
                name = (String) e.getKey();
                index = Libcore.os.if_nametoindex((String) e.getKey());
                if (index != 0) {
                    NetworkInterface ni = new NetworkInterface(name, index, null);
                    ni.displayName = name;
                    List<InetAddress> addrs = new ArrayList();
                    List<InterfaceAddress> binds = new ArrayList();
                    for (StructIfaddrs ifa2 : (List) e.getValue()) {
                        if (ifa2.ifa_addr != null) {
                            addrs.add(ifa2.ifa_addr);
                            binds.add(new InterfaceAddress(ifa2.ifa_addr, (Inet4Address) ifa2.ifa_broadaddr, ifa2.ifa_netmask));
                        }
                        if (ifa2.hwaddr != null) {
                            ni.hardwareAddr = ifa2.hwaddr;
                        }
                    }
                    ni.addrs = (InetAddress[]) addrs.toArray(new InetAddress[addrs.size()]);
                    ni.bindings = (InterfaceAddress[]) binds.toArray(new InterfaceAddress[binds.size()]);
                    ni.childs = new ArrayList(0);
                    nis.put(name, ni);
                }
            }
            for (Entry<String, NetworkInterface> e2 : nis.entrySet()) {
                NetworkInterface ni2 = (NetworkInterface) e2.getValue();
                name = ni2.getName();
                index = name.indexOf(58);
                if (index != -1) {
                    NetworkInterface parent = (NetworkInterface) nis.get(name.substring(0, index));
                    ni2.virtual = true;
                    ni2.parent = parent;
                    parent.childs.add(ni2);
                }
            }
            return (NetworkInterface[]) nis.values().toArray(new NetworkInterface[nis.size()]);
        } catch (ErrnoException e3) {
            throw e3.rethrowAsSocketException();
        }
    }

    public boolean isUp() throws SocketException {
        int mask = OsConstants.IFF_UP | OsConstants.IFF_RUNNING;
        return (getFlags() & mask) == mask;
    }

    public boolean isLoopback() throws SocketException {
        return (getFlags() & OsConstants.IFF_LOOPBACK) != 0;
    }

    public boolean isPointToPoint() throws SocketException {
        return (getFlags() & OsConstants.IFF_POINTOPOINT) != 0;
    }

    public boolean supportsMulticast() throws SocketException {
        return (getFlags() & OsConstants.IFF_MULTICAST) != 0;
    }

    public byte[] getHardwareAddress() throws SocketException {
        NetworkInterface ni = getByName(this.name);
        if (ni != null) {
            return ni.hardwareAddr;
        }
        throw new SocketException("NetworkInterface doesn't exist anymore");
    }

    public int getMTU() throws SocketException {
        FileDescriptor fd = null;
        try {
            fd = Libcore.rawOs.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, 0);
            int ioctlMTU = Libcore.rawOs.ioctlMTU(fd, this.name);
            IoUtils.closeQuietly(fd);
            return ioctlMTU;
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        } catch (Exception ex) {
            throw new SocketException(ex);
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
        }
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    private int getFlags() throws SocketException {
        FileDescriptor fd = null;
        try {
            fd = Libcore.rawOs.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, 0);
            int ioctlFlags = Libcore.rawOs.ioctlFlags(fd, this.name);
            IoUtils.closeQuietly(fd);
            return ioctlFlags;
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        } catch (Exception ex) {
            throw new SocketException(ex);
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
        }
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface that = (NetworkInterface) obj;
        if (this.name != null) {
            if (!this.name.equals(that.name)) {
                return false;
            }
        } else if (that.name != null) {
            return false;
        }
        if (this.addrs == null) {
            if (that.addrs == null) {
                z = true;
            }
            return z;
        } else if (that.addrs == null || this.addrs.length != that.addrs.length) {
            return false;
        } else {
            for (int i = 0; i < count; i++) {
                boolean found = false;
                for (Object equals : that.addrs) {
                    if (this.addrs[i].equals(equals)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
    }

    public int hashCode() {
        return this.name == null ? 0 : this.name.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("name:");
        stringBuilder.append(this.name == null ? "null" : this.name);
        String result = stringBuilder.toString();
        if (this.displayName == null) {
            return result;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(result);
        stringBuilder.append(" (");
        stringBuilder.append(this.displayName);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    static NetworkInterface getDefault() {
        return defaultInterface;
    }
}
