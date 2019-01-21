package sun.nio.ch;

import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jdk.net.ExtendedSocketOptions;
import sun.net.ExtendedOptionsImpl;
import sun.net.ResourceManager;

class DatagramChannelImpl extends DatagramChannel implements SelChImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ST_CONNECTED = 1;
    private static final int ST_KILLED = 2;
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd = new DatagramDispatcher();
    private InetAddress cachedSenderInetAddress;
    private int cachedSenderPort;
    private final ProtocolFamily family;
    final FileDescriptor fd;
    private final int fdVal;
    private final CloseGuard guard = CloseGuard.get();
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object readLock = new Object();
    private volatile long readerThread = 0;
    private MembershipRegistry registry;
    private InetSocketAddress remoteAddress;
    private boolean reuseAddressEmulated;
    private SocketAddress sender;
    private DatagramSocket socket;
    private int state = -1;
    private final Object stateLock = new Object();
    private final Object writeLock = new Object();
    private volatile long writerThread = 0;

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet(8);
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            set.add(StandardSocketOptions.SO_BROADCAST);
            set.add(StandardSocketOptions.IP_TOS);
            set.add(StandardSocketOptions.IP_MULTICAST_IF);
            set.add(StandardSocketOptions.IP_MULTICAST_TTL);
            set.add(StandardSocketOptions.IP_MULTICAST_LOOP);
            if (ExtendedOptionsImpl.flowSupported()) {
                set.add(ExtendedSocketOptions.SO_FLOW_SLA);
            }
            return Collections.unmodifiableSet(set);
        }
    }

    private static native void disconnect0(FileDescriptor fileDescriptor, boolean z) throws IOException;

    private static native void initIDs();

    private native int receive0(FileDescriptor fileDescriptor, long j, int i, boolean z) throws IOException;

    private native int send0(boolean z, FileDescriptor fileDescriptor, long j, int i, InetAddress inetAddress, int i2) throws IOException;

    static {
        initIDs();
    }

    public DatagramChannelImpl(SelectorProvider sp) throws IOException {
        super(sp);
        ResourceManager.beforeUdpCreate();
        try {
            this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
            this.fd = Net.socket(this.family, $assertionsDisabled);
            this.fdVal = IOUtil.fdVal(this.fd);
            this.state = 0;
            if (this.fd != null && this.fd.valid()) {
                this.guard.open("close");
            }
        } catch (IOException ioe) {
            ResourceManager.afterUdpClose();
            throw ioe;
        }
    }

    public DatagramChannelImpl(SelectorProvider sp, ProtocolFamily family) throws IOException {
        super(sp);
        if (family == StandardProtocolFamily.INET || family == StandardProtocolFamily.INET6) {
            if (family != StandardProtocolFamily.INET6 || Net.isIPv6Available()) {
                this.family = family;
                this.fd = Net.socket(family, $assertionsDisabled);
                this.fdVal = IOUtil.fdVal(this.fd);
                this.state = 0;
                if (this.fd != null && this.fd.valid()) {
                    this.guard.open("close");
                    return;
                }
                return;
            }
            throw new UnsupportedOperationException("IPv6 not available");
        } else if (family == null) {
            throw new NullPointerException("'family' is null");
        } else {
            throw new UnsupportedOperationException("Protocol family not supported");
        }
    }

    public DatagramChannelImpl(SelectorProvider sp, FileDescriptor fd) throws IOException {
        super(sp);
        this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        this.state = 0;
        this.localAddress = Net.localAddress(fd);
        if (fd != null && fd.valid()) {
            this.guard.open("close");
        }
    }

    public DatagramSocket socket() {
        DatagramSocket datagramSocket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = DatagramSocketAdaptor.create(this);
            }
            datagramSocket = this.socket;
        }
        return datagramSocket;
    }

    public SocketAddress getLocalAddress() throws IOException {
        InetSocketAddress revealedLocalAddress;
        synchronized (this.stateLock) {
            if (isOpen()) {
                revealedLocalAddress = Net.getRevealedLocalAddress(this.localAddress);
            } else {
                throw new ClosedChannelException();
            }
        }
        return revealedLocalAddress;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            if (isOpen()) {
                inetSocketAddress = this.remoteAddress;
            } else {
                throw new ClosedChannelException();
            }
        }
        return inetSocketAddress;
    }

    /* JADX WARNING: Missing block: B:27:0x0053, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        } else if (supportedOptions().contains(name)) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (!(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL)) {
                    if (name != StandardSocketOptions.IP_MULTICAST_LOOP) {
                        if (name != StandardSocketOptions.IP_MULTICAST_IF) {
                            if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind() && this.localAddress != null) {
                                this.reuseAddressEmulated = true;
                                this.isReuseAddress = ((Boolean) value).booleanValue();
                            }
                            Net.setSocketOption(this.fd, Net.UNSPEC, name, value);
                            return this;
                        } else if (value != null) {
                            NetworkInterface interf = (NetworkInterface) value;
                            if (this.family == StandardProtocolFamily.INET6) {
                                int index = interf.getIndex();
                                if (index != -1) {
                                    Net.setInterface6(this.fd, index);
                                } else {
                                    throw new IOException("Network interface cannot be identified");
                                }
                            }
                            Inet4Address target = Net.anyInet4Address(interf);
                            if (target != null) {
                                Net.setInterface4(this.fd, Net.inet4AsInt(target));
                            } else {
                                throw new IOException("Network interface not configured for IPv4");
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot set IP_MULTICAST_IF to 'null'");
                        }
                    }
                }
                Net.setSocketOption(this.fd, this.family, name, value);
                return this;
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append((Object) name);
            stringBuilder.append("' not supported");
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
    }

    public <T> T getOption(SocketOption<T> name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        } else if (supportedOptions().contains(name)) {
            synchronized (this.stateLock) {
                Object socketOption;
                ensureOpen();
                if (!(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL)) {
                    if (name != StandardSocketOptions.IP_MULTICAST_LOOP) {
                        if (name == StandardSocketOptions.IP_MULTICAST_IF) {
                            int address;
                            if (this.family == StandardProtocolFamily.INET) {
                                address = Net.getInterface4(this.fd);
                                if (address == 0) {
                                    return null;
                                }
                                NetworkInterface ni = NetworkInterface.getByInetAddress(Net.inet4FromInt(address));
                                if (ni != null) {
                                    return ni;
                                }
                                throw new IOException("Unable to map address to interface");
                            }
                            address = Net.getInterface6(this.fd);
                            if (address == 0) {
                                return null;
                            }
                            NetworkInterface ni2 = NetworkInterface.getByIndex(address);
                            if (ni2 != null) {
                                return ni2;
                            }
                            throw new IOException("Unable to map index to interface");
                        } else if (name == StandardSocketOptions.SO_REUSEADDR && this.reuseAddressEmulated) {
                            Boolean valueOf = Boolean.valueOf(this.isReuseAddress);
                            return valueOf;
                        } else {
                            socketOption = Net.getSocketOption(this.fd, Net.UNSPEC, name);
                            return socketOption;
                        }
                    }
                }
                socketOption = Net.getSocketOption(this.fd, this.family, name);
                return socketOption;
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append((Object) name);
            stringBuilder.append("' not supported");
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
    }

    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x00c5 A:{SYNTHETIC, Splitter:B:74:0x00c5} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x00cc  */
    /* JADX WARNING: Missing block: B:36:0x0067, code skipped:
            if (r3 == null) goto L_0x006c;
     */
    /* JADX WARNING: Missing block: B:38:?, code skipped:
            sun.nio.ch.Util.releaseTemporaryDirectBuffer(r3);
     */
    /* JADX WARNING: Missing block: B:39:0x006c, code skipped:
            r14.readerThread = 0;
     */
    /* JADX WARNING: Missing block: B:40:0x006e, code skipped:
            if (r2 > 0) goto L_0x0075;
     */
    /* JADX WARNING: Missing block: B:41:0x0070, code skipped:
            if (r2 != -2) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:43:0x0073, code skipped:
            r4 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:44:0x0075, code skipped:
            end(r4);
     */
    /* JADX WARNING: Missing block: B:46:0x0079, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:52:?, code skipped:
            r3.flip();
            r15.put(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        } else if (dst == null) {
            throw new NullPointerException();
        } else if (this.localAddress == null) {
            return null;
        } else {
            synchronized (this.readLock) {
                ensureOpen();
                int n = 0;
                ByteBuffer bb = null;
                boolean z = true;
                try {
                    begin();
                    if (isOpen()) {
                        SocketAddress socketAddress;
                        SecurityManager security = System.getSecurityManager();
                        this.readerThread = NativeThread.current();
                        if (!isConnected()) {
                            if (security != null) {
                                bb = Util.getTemporaryDirectBuffer(dst.remaining());
                                while (true) {
                                    n = receive(this.fd, bb);
                                    if (n != -3 || !isOpen()) {
                                        if (n != -2) {
                                            InetSocketAddress isa = this.sender;
                                            security.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                                            break;
                                        }
                                        break;
                                    }
                                }
                                socketAddress = this.sender;
                                if (bb != null) {
                                    Util.releaseTemporaryDirectBuffer(bb);
                                }
                                this.readerThread = 0;
                                if (n <= 0) {
                                    if (n != -2) {
                                        z = $assertionsDisabled;
                                    }
                                }
                                end(z);
                                return socketAddress;
                            }
                        }
                        do {
                            n = receive(this.fd, dst);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        if (n == -2) {
                            if (bb != null) {
                                Util.releaseTemporaryDirectBuffer(bb);
                            }
                            this.readerThread = 0;
                            if (n <= 0) {
                                if (n != -2) {
                                    z = $assertionsDisabled;
                                }
                            }
                            end(z);
                            return null;
                        }
                        socketAddress = this.sender;
                        if (bb != null) {
                        }
                        this.readerThread = 0;
                        if (n <= 0) {
                        }
                        end(z);
                        return socketAddress;
                    }
                    if (bb != null) {
                        Util.releaseTemporaryDirectBuffer(bb);
                    }
                    this.readerThread = 0;
                    if (null <= null) {
                        if (null != -2) {
                            z = $assertionsDisabled;
                        }
                    }
                    end(z);
                    return null;
                } catch (SecurityException e) {
                    bb.clear();
                } catch (Throwable th) {
                    if (bb != null) {
                        Util.releaseTemporaryDirectBuffer(bb);
                    }
                    this.readerThread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            z = $assertionsDisabled;
                        }
                    }
                    end(z);
                }
            }
        }
    }

    private int receive(FileDescriptor fd, ByteBuffer dst) throws IOException {
        int pos = dst.position();
        int lim = dst.limit();
        int rem = pos <= lim ? lim - pos : 0;
        if ((dst instanceof DirectBuffer) && rem > 0) {
            return receiveIntoNativeBuffer(fd, dst, rem, pos);
        }
        int newSize = Math.max(rem, 1);
        ByteBuffer bb = Util.getTemporaryDirectBuffer(newSize);
        try {
            BlockGuard.getThreadPolicy().onNetwork();
            int n = receiveIntoNativeBuffer(fd, bb, newSize, 0);
            bb.flip();
            if (n > 0 && rem > 0) {
                dst.put(bb);
            }
            Util.releaseTemporaryDirectBuffer(bb);
            return n;
        } catch (Throwable th) {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    private int receiveIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb, int rem, int pos) throws IOException {
        int n = receive0(fd, ((DirectBuffer) bb).address() + ((long) pos), rem, isConnected());
        if (n > 0) {
            bb.position(pos + n);
        }
        return n;
    }

    /* JADX WARNING: Missing block: B:20:0x003a, code skipped:
            r3 = $assertionsDisabled;
            r4 = 0;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            begin();
     */
    /* JADX WARNING: Missing block: B:23:0x0047, code skipped:
            if (isOpen() != false) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            r11.writerThread = 0;
     */
    /* JADX WARNING: Missing block: B:26:0x004c, code skipped:
            if (r4 > 0) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:27:0x004e, code skipped:
            if (r4 != -2) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:29:0x0051, code skipped:
            r5 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:30:0x0053, code skipped:
            end(r5);
     */
    /* JADX WARNING: Missing block: B:32:0x0057, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r11.writerThread = sun.nio.ch.NativeThread.current();
            dalvik.system.BlockGuard.getThreadPolicy().onNetwork();
     */
    /* JADX WARNING: Missing block: B:35:0x0065, code skipped:
            r4 = send(r11.fd, r12, r1);
     */
    /* JADX WARNING: Missing block: B:36:0x006d, code skipped:
            if (r4 != -3) goto L_0x0075;
     */
    /* JADX WARNING: Missing block: B:38:0x0073, code skipped:
            if (isOpen() != false) goto L_0x0065;
     */
    /* JADX WARNING: Missing block: B:39:0x0075, code skipped:
            r9 = r11.stateLock;
     */
    /* JADX WARNING: Missing block: B:40:0x0077, code skipped:
            monitor-enter(r9);
     */
    /* JADX WARNING: Missing block: B:43:0x007c, code skipped:
            if (isOpen() == false) goto L_0x008a;
     */
    /* JADX WARNING: Missing block: B:45:0x0080, code skipped:
            if (r11.localAddress != null) goto L_0x008a;
     */
    /* JADX WARNING: Missing block: B:46:0x0082, code skipped:
            r11.localAddress = sun.nio.ch.Net.localAddress(r11.fd);
     */
    /* JADX WARNING: Missing block: B:47:0x008a, code skipped:
            monitor-exit(r9);
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            r9 = sun.nio.ch.IOStatus.normalize(r4);
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            r11.writerThread = 0;
     */
    /* JADX WARNING: Missing block: B:52:0x0091, code skipped:
            if (r4 > 0) goto L_0x0097;
     */
    /* JADX WARNING: Missing block: B:53:0x0093, code skipped:
            if (r4 != -2) goto L_0x0096;
     */
    /* JADX WARNING: Missing block: B:56:0x0097, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:57:0x0098, code skipped:
            end(r3);
     */
    /* JADX WARNING: Missing block: B:59:0x009c, code skipped:
            return r9;
     */
    /* JADX WARNING: Missing block: B:67:?, code skipped:
            r11.writerThread = 0;
     */
    /* JADX WARNING: Missing block: B:68:0x00a3, code skipped:
            if (r4 <= 0) goto L_0x00a5;
     */
    /* JADX WARNING: Missing block: B:69:0x00a5, code skipped:
            if (r4 == -2) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:72:0x00a9, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:73:0x00aa, code skipped:
            end(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        if (src != null) {
            synchronized (this.writeLock) {
                ensureOpen();
                InetSocketAddress isa = Net.checkAddress(target);
                InetAddress ia = isa.getAddress();
                if (ia != null) {
                    synchronized (this.stateLock) {
                        if (isConnected()) {
                            if (target.equals(this.remoteAddress)) {
                                int write = write(src);
                                return write;
                            }
                            throw new IllegalArgumentException("Connected address not equal to target address");
                        } else if (target != null) {
                            SecurityManager sm = System.getSecurityManager();
                            if (sm != null) {
                                if (ia.isMulticastAddress()) {
                                    sm.checkMulticast(ia);
                                } else {
                                    sm.checkConnect(ia.getHostAddress(), isa.getPort());
                                }
                            }
                        } else {
                            throw new NullPointerException();
                        }
                    }
                }
                throw new IOException("Target address not resolved");
            }
        }
        throw new NullPointerException();
    }

    private int send(FileDescriptor fd, ByteBuffer src, InetSocketAddress target) throws IOException {
        if (src instanceof DirectBuffer) {
            return sendFromNativeBuffer(fd, src, target);
        }
        int pos = src.position();
        int lim = src.limit();
        ByteBuffer bb = Util.getTemporaryDirectBuffer(pos <= lim ? lim - pos : 0);
        try {
            bb.put(src);
            bb.flip();
            src.position(pos);
            int n = sendFromNativeBuffer(fd, bb, target);
            if (n > 0) {
                src.position(pos + n);
            }
            Util.releaseTemporaryDirectBuffer(bb);
            return n;
        } catch (Throwable th) {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    private int sendFromNativeBuffer(FileDescriptor fd, ByteBuffer bb, InetSocketAddress target) throws IOException {
        PortUnreachableException pue;
        int pos = bb.position();
        int lim = bb.limit();
        boolean z = $assertionsDisabled;
        int rem = pos <= lim ? lim - pos : 0;
        if (this.family != StandardProtocolFamily.INET) {
            z = true;
        }
        try {
            pue = send0(z, fd, ((DirectBuffer) bb).address() + ((long) pos), rem, target.getAddress(), target.getPort());
        } catch (PortUnreachableException pue2) {
            if (isConnected()) {
                throw pue2;
            }
            pue2 = rem;
        }
        if (pue2 > null) {
            bb.position(pos + pue2);
        }
        return pue2;
    }

    /* JADX WARNING: Missing block: B:32:0x005b, code skipped:
            return r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(ByteBuffer buf) throws IOException {
        if (buf != null) {
            synchronized (this.readLock) {
                synchronized (this.stateLock) {
                    ensureOpen();
                    if (isConnected()) {
                    } else {
                        throw new NotYetConnectedException();
                    }
                }
                boolean z = $assertionsDisabled;
                int n = 0;
                boolean z2 = true;
                try {
                    begin();
                    if (isOpen()) {
                        this.readerThread = NativeThread.current();
                        while (true) {
                            n = IOUtil.read(this.fd, buf, -1, nd);
                            if (n == -3) {
                                if (!isOpen()) {
                                    break;
                                }
                            }
                            break;
                        }
                        int normalize = IOStatus.normalize(n);
                    } else {
                        this.readerThread = 0;
                        if (n <= 0) {
                            if (n != -2) {
                                z2 = $assertionsDisabled;
                            }
                        }
                        end(z2);
                        return 0;
                    }
                } finally {
                    this.readerThread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            end(z);
                        }
                    }
                    z = true;
                    end(z);
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    /* JADX WARNING: Missing block: B:36:0x006b, code skipped:
            return r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > dsts.length - length) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.readLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            long n = 0;
            boolean z = true;
            try {
                begin();
                if (isOpen()) {
                    this.readerThread = NativeThread.current();
                    while (true) {
                        n = IOUtil.read(this.fd, dsts, offset, length, nd);
                        if (n == -3) {
                            if (!isOpen()) {
                                break;
                            }
                        }
                        break;
                    }
                    long normalize = IOStatus.normalize(n);
                } else {
                    this.readerThread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            z = $assertionsDisabled;
                        }
                    }
                    end(z);
                    return 0;
                }
            } finally {
                this.readerThread = 0;
                if (n <= 0) {
                    if (n != -2) {
                        z = $assertionsDisabled;
                    }
                }
                end(z);
            }
        }
    }

    /* JADX WARNING: Missing block: B:32:0x005b, code skipped:
            return r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int write(ByteBuffer buf) throws IOException {
        if (buf != null) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpen();
                    if (isConnected()) {
                    } else {
                        throw new NotYetConnectedException();
                    }
                }
                boolean z = $assertionsDisabled;
                int n = 0;
                boolean z2 = true;
                try {
                    begin();
                    if (isOpen()) {
                        this.writerThread = NativeThread.current();
                        while (true) {
                            n = IOUtil.write(this.fd, buf, -1, nd);
                            if (n == -3) {
                                if (!isOpen()) {
                                    break;
                                }
                            }
                            break;
                        }
                        int normalize = IOStatus.normalize(n);
                    } else {
                        this.writerThread = 0;
                        if (n <= 0) {
                            if (n != -2) {
                                z2 = $assertionsDisabled;
                            }
                        }
                        end(z2);
                        return 0;
                    }
                } finally {
                    this.writerThread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            end(z);
                        }
                    }
                    z = true;
                    end(z);
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    /* JADX WARNING: Missing block: B:36:0x006b, code skipped:
            return r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > srcs.length - length) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.writeLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            long n = 0;
            boolean z = true;
            try {
                begin();
                if (isOpen()) {
                    this.writerThread = NativeThread.current();
                    while (true) {
                        n = IOUtil.write(this.fd, srcs, offset, length, nd);
                        if (n == -3) {
                            if (!isOpen()) {
                                break;
                            }
                        }
                        break;
                    }
                    long normalize = IOStatus.normalize(n);
                } else {
                    this.writerThread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            z = $assertionsDisabled;
                        }
                    }
                    end(z);
                    return 0;
                }
            } finally {
                this.writerThread = 0;
                if (n <= 0) {
                    if (n != -2) {
                        z = $assertionsDisabled;
                    }
                }
                end(z);
            }
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        IOUtil.configureBlocking(this.fd, block);
    }

    public SocketAddress localAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.localAddress;
        }
        return inetSocketAddress;
    }

    public SocketAddress remoteAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.remoteAddress;
        }
        return inetSocketAddress;
    }

    public DatagramChannel bind(SocketAddress local) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpen();
                    if (this.localAddress == null) {
                        InetSocketAddress isa;
                        if (local != null) {
                            isa = Net.checkAddress(local);
                            if (this.family == StandardProtocolFamily.INET) {
                                if (!(isa.getAddress() instanceof Inet4Address)) {
                                    throw new UnsupportedAddressTypeException();
                                }
                            }
                        } else if (this.family == StandardProtocolFamily.INET) {
                            isa = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0);
                        } else {
                            isa = new InetSocketAddress(0);
                        }
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkListen(isa.getPort());
                        }
                        Net.bind(this.family, this.fd, isa.getAddress(), isa.getPort());
                        this.localAddress = Net.localAddress(this.fd);
                    } else {
                        throw new AlreadyBoundException();
                    }
                }
            }
        }
        return this;
    }

    public boolean isConnected() {
        boolean z;
        synchronized (this.stateLock) {
            z = true;
            if (this.state != 1) {
                z = $assertionsDisabled;
            }
        }
        return z;
    }

    void ensureOpenAndUnconnected() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (this.state == 0) {
            } else {
                throw new IllegalStateException("Connect already invoked");
            }
        }
    }

    public DatagramChannel connect(SocketAddress sa) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpenAndUnconnected();
                    InetSocketAddress isa = Net.checkAddress(sa);
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
                    }
                    if (Net.connect(this.family, this.fd, isa.getAddress(), isa.getPort()) > 0) {
                        this.state = 1;
                        this.remoteAddress = isa;
                        this.sender = isa;
                        this.cachedSenderInetAddress = isa.getAddress();
                        this.cachedSenderPort = isa.getPort();
                        this.localAddress = Net.localAddress(this.fd);
                        synchronized (blockingLock()) {
                            try {
                                boolean blocking = isBlocking();
                                ByteBuffer tmpBuf = ByteBuffer.allocate(1);
                                if (blocking) {
                                    configureBlocking($assertionsDisabled);
                                }
                                do {
                                    tmpBuf.clear();
                                } while (receive(tmpBuf) != null);
                                if (blocking) {
                                    configureBlocking(true);
                                }
                            } catch (Throwable th) {
                            }
                        }
                    } else {
                        throw new Error();
                    }
                }
            }
        }
        return this;
    }

    public DatagramChannel disconnect() throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    if (isConnected()) {
                        if (isOpen()) {
                            InetSocketAddress isa = this.remoteAddress;
                            SecurityManager sm = System.getSecurityManager();
                            if (sm != null) {
                                sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
                            }
                            disconnect0(this.fd, this.family == StandardProtocolFamily.INET6 ? true : $assertionsDisabled);
                            this.remoteAddress = null;
                            this.state = 0;
                            this.localAddress = Net.localAddress(this.fd);
                            return this;
                        }
                    }
                    return this;
                }
            }
        }
    }

    private MembershipKey innerJoin(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        Throwable th;
        InetAddress inetAddress = group;
        InetAddress inetAddress2 = source;
        NetworkInterface networkInterface;
        if (group.isMulticastAddress()) {
            if (inetAddress instanceof Inet4Address) {
                if (this.family == StandardProtocolFamily.INET6 && !Net.canIPv6SocketJoinIPv4Group()) {
                    throw new IllegalArgumentException("IPv6 socket cannot join IPv4 multicast group");
                }
            } else if (!(inetAddress instanceof Inet6Address)) {
                networkInterface = interf;
                throw new IllegalArgumentException("Address type not supported");
            } else if (this.family != StandardProtocolFamily.INET6) {
                networkInterface = interf;
                throw new IllegalArgumentException("Only IPv6 sockets can join IPv6 multicast group");
            }
            if (inetAddress2 != null) {
                if (source.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("Source address is a wildcard address");
                } else if (source.isMulticastAddress()) {
                    throw new IllegalArgumentException("Source address is multicast address");
                } else if (source.getClass() != group.getClass()) {
                    throw new IllegalArgumentException("Source address is different type to group");
                }
            }
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkMulticast(inetAddress);
            }
            synchronized (this.stateLock) {
                try {
                    if (isOpen()) {
                        if (this.registry == null) {
                            this.registry = new MembershipRegistry();
                            networkInterface = interf;
                        } else {
                            networkInterface = interf;
                            MembershipKey key = this.registry.checkMembership(inetAddress, networkInterface, inetAddress2);
                            if (key != null) {
                                return key;
                            }
                        }
                        int n;
                        MembershipKeyImpl type6;
                        int i;
                        if (this.family == StandardProtocolFamily.INET6 && ((inetAddress instanceof Inet6Address) || Net.canJoin6WithIPv4Group())) {
                            int index = interf.getIndex();
                            if (index != -1) {
                                byte[] bArr;
                                byte[] groupAddress = Net.inet6AsByteArray(group);
                                if (inetAddress2 == null) {
                                    bArr = null;
                                } else {
                                    bArr = Net.inet6AsByteArray(source);
                                }
                                byte[] sourceAddress = bArr;
                                n = Net.join6(this.fd, groupAddress, index, sourceAddress);
                                if (n != -2) {
                                    type6 = new Type6(this, inetAddress, networkInterface, inetAddress2, groupAddress, index, sourceAddress);
                                    index = type6;
                                } else {
                                    i = n;
                                    byte[] bArr2 = sourceAddress;
                                    throw new UnsupportedOperationException();
                                }
                            }
                            throw new IOException("Network interface cannot be identified");
                        }
                        Inet4Address target = Net.anyInet4Address(interf);
                        if (target != null) {
                            int groupAddress2 = Net.inet4AsInt(group);
                            int targetAddress = Net.inet4AsInt(target);
                            n = inetAddress2 == null ? 0 : Net.inet4AsInt(source);
                            int n2 = Net.join4(this.fd, groupAddress2, targetAddress, n);
                            if (n2 != -2) {
                                i = n2;
                                type6 = new Type4(this, inetAddress, networkInterface, inetAddress2, groupAddress2, targetAddress, n);
                            } else {
                                i = n2;
                                int i2 = n;
                                int i3 = targetAddress;
                                throw new UnsupportedOperationException();
                            }
                        }
                        throw new IOException("Network interface not configured for IPv4");
                        MembershipKeyImpl target2 = key;
                        this.registry.add(target2);
                        return target2;
                    }
                    networkInterface = interf;
                    throw new ClosedChannelException();
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        networkInterface = interf;
        throw new IllegalArgumentException("Group not a multicast address");
    }

    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return innerJoin(group, interf, null);
    }

    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        if (source != null) {
            return innerJoin(group, interf, source);
        }
        throw new NullPointerException("source address is null");
    }

    void drop(MembershipKeyImpl key) {
        synchronized (this.stateLock) {
            if (key.isValid()) {
                try {
                    if (key instanceof Type6) {
                        Type6 key6 = (Type6) key;
                        Net.drop6(this.fd, key6.groupAddress(), key6.index(), key6.source());
                    } else {
                        Type4 key4 = (Type4) key;
                        Net.drop4(this.fd, key4.groupAddress(), key4.interfaceAddress(), key4.source());
                    }
                    key.invalidate();
                    this.registry.remove(key);
                    return;
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        }
    }

    void block(MembershipKeyImpl key, InetAddress source) throws IOException {
        synchronized (this.stateLock) {
            if (!key.isValid()) {
                throw new IllegalStateException("key is no longer valid");
            } else if (source.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Source address is a wildcard address");
            } else if (source.isMulticastAddress()) {
                throw new IllegalArgumentException("Source address is multicast address");
            } else if (source.getClass() == key.group().getClass()) {
                int n;
                if (key instanceof Type6) {
                    Type6 key6 = (Type6) key;
                    n = Net.block6(this.fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
                } else {
                    Type4 key4 = (Type4) key;
                    n = Net.block4(this.fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
                }
                if (n != -2) {
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                throw new IllegalArgumentException("Source address is different type to group");
            }
        }
    }

    void unblock(MembershipKeyImpl key, InetAddress source) {
        synchronized (this.stateLock) {
            if (key.isValid()) {
                try {
                    if (key instanceof Type6) {
                        Type6 key6 = (Type6) key;
                        Net.unblock6(this.fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
                    } else {
                        Type4 key4 = (Type4) key;
                        Net.unblock4(this.fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
                    }
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
            throw new IllegalStateException("key is no longer valid");
        }
    }

    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            this.guard.close();
            if (this.state != 2) {
                nd.preClose(this.fd);
            }
            ResourceManager.afterUdpClose();
            if (this.registry != null) {
                this.registry.invalidateAll();
            }
            long j = this.readerThread;
            long th = j;
            if (j != 0) {
                NativeThread.signal(th);
            }
            j = this.writerThread;
            th = j;
            if (j != 0) {
                NativeThread.signal(th);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 2) {
            } else if (this.state == -1) {
                this.state = 2;
            } else {
                nd.close(this.fd);
                this.state = 2;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            if (this.fd != null) {
                close();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl sk) {
        int intOps = sk.nioInterestOps();
        int oldOps = sk.nioReadyOps();
        int newOps = initialOps;
        int i = Net.POLLNVAL & ops;
        boolean z = $assertionsDisabled;
        if (i != 0) {
            return $assertionsDisabled;
        }
        if (((Net.POLLERR | Net.POLLHUP) & ops) != 0) {
            newOps = intOps;
            sk.nioReadyOps(newOps);
            if (((~oldOps) & newOps) != 0) {
                z = true;
            }
            return z;
        }
        if (!((Net.POLLIN & ops) == 0 || (intOps & 1) == 0)) {
            newOps |= 1;
        }
        if (!((Net.POLLOUT & ops) == 0 || (intOps & 4) == 0)) {
            newOps |= 4;
        }
        sk.nioReadyOps(newOps);
        if (((~oldOps) & newOps) != 0) {
            z = true;
        }
        return z;
    }

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, sk.nioReadyOps(), sk);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, 0, sk);
    }

    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r9.readerThread = 0;
     */
    /* JADX WARNING: Missing block: B:12:0x0018, code skipped:
            if (r2 <= 0) goto L_0x001b;
     */
    /* JADX WARNING: Missing block: B:14:0x001b, code skipped:
            r3 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:15:0x001c, code skipped:
            end(r3);
     */
    /* JADX WARNING: Missing block: B:17:0x0020, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:23:0x002e, code skipped:
            r2 = sun.nio.ch.Net.poll(r9.fd, r10, r11);
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            r9.readerThread = 0;
     */
    /* JADX WARNING: Missing block: B:26:0x0031, code skipped:
            if (r2 <= 0) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:27:0x0033, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:28:0x0035, code skipped:
            end(r1);
     */
    /* JADX WARNING: Missing block: B:30:0x003a, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int poll(int events, long timeout) throws IOException {
        synchronized (this.readLock) {
            boolean z = $assertionsDisabled;
            int n = 0;
            boolean z2 = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (isOpen()) {
                        this.readerThread = NativeThread.current();
                    }
                }
            } catch (Throwable th) {
                this.readerThread = 0;
                if (n > 0) {
                    z = true;
                }
                end(z);
            }
        }
    }

    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        int newOps = 0;
        if ((ops & 1) != 0) {
            newOps = 0 | Net.POLLIN;
        }
        if ((ops & 4) != 0) {
            newOps |= Net.POLLOUT;
        }
        if ((ops & 8) != 0) {
            newOps |= Net.POLLIN;
        }
        sk.selector.putEventOps(sk, newOps);
    }

    public FileDescriptor getFD() {
        return this.fd;
    }

    public int getFDVal() {
        return this.fdVal;
    }
}
