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
    static final /* synthetic */ boolean -assertionsDisabled = (DatagramChannelImpl.class.desiredAssertionStatus() ^ 1);
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

    public boolean translateReadyOps(int r1, int r2, sun.nio.ch.SelectionKeyImpl r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: sun.nio.ch.DatagramChannelImpl.translateReadyOps(int, int, sun.nio.ch.SelectionKeyImpl):boolean
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: sun.nio.ch.DatagramChannelImpl.translateReadyOps(int, int, sun.nio.ch.SelectionKeyImpl):boolean");
    }

    static {
        initIDs();
    }

    public DatagramChannelImpl(SelectorProvider sp) throws IOException {
        super(sp);
        ResourceManager.beforeUdpCreate();
        try {
            this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
            this.fd = Net.socket(this.family, -assertionsDisabled);
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
                this.fd = Net.socket(family, -assertionsDisabled);
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
        SocketAddress revealedLocalAddress;
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
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            if (isOpen()) {
                socketAddress = this.remoteAddress;
            } else {
                throw new ClosedChannelException();
            }
        }
        return socketAddress;
    }

    /* JADX WARNING: inconsistent code. */
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
                        } else if (value == null) {
                            throw new IllegalArgumentException("Cannot set IP_MULTICAST_IF to 'null'");
                        } else {
                            NetworkInterface interf = (NetworkInterface) value;
                            if (this.family == StandardProtocolFamily.INET6) {
                                int index = interf.getIndex();
                                if (index == -1) {
                                    throw new IOException("Network interface cannot be identified");
                                }
                                Net.setInterface6(this.fd, index);
                            } else {
                                Inet4Address target = Net.anyInet4Address(interf);
                                if (target == null) {
                                    throw new IOException("Network interface not configured for IPv4");
                                }
                                Net.setInterface4(this.fd, Net.inet4AsInt(target));
                            }
                        }
                    }
                }
                Net.setSocketOption(this.fd, this.family, name, value);
                return this;
            }
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
    }

    public <T> T getOption(SocketOption<T> name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        } else if (supportedOptions().contains(name)) {
            synchronized (this.stateLock) {
                T valueOf;
                ensureOpen();
                if (!(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL)) {
                    if (name != StandardSocketOptions.IP_MULTICAST_LOOP) {
                        if (name == StandardSocketOptions.IP_MULTICAST_IF) {
                            NetworkInterface ni;
                            if (this.family == StandardProtocolFamily.INET) {
                                int address = Net.getInterface4(this.fd);
                                if (address == 0) {
                                    return null;
                                }
                                ni = NetworkInterface.getByInetAddress(Net.inet4FromInt(address));
                                if (ni == null) {
                                    throw new IOException("Unable to map address to interface");
                                }
                                return ni;
                            }
                            int index = Net.getInterface6(this.fd);
                            if (index == 0) {
                                return null;
                            }
                            ni = NetworkInterface.getByIndex(index);
                            if (ni == null) {
                                throw new IOException("Unable to map index to interface");
                            }
                            return ni;
                        } else if (name == StandardSocketOptions.SO_REUSEADDR && this.reuseAddressEmulated) {
                            valueOf = Boolean.valueOf(this.isReuseAddress);
                            return valueOf;
                        } else {
                            valueOf = Net.getSocketOption(this.fd, Net.UNSPEC, name);
                            return valueOf;
                        }
                    }
                }
                valueOf = Net.getSocketOption(this.fd, this.family, name);
                return valueOf;
            }
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
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

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        boolean z = true;
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
                ByteBuffer byteBuffer = null;
                begin();
                if (isOpen()) {
                    SecurityManager security = System.getSecurityManager();
                    this.readerThread = NativeThread.current();
                    if (isConnected() || security == null) {
                        do {
                            n = receive(this.fd, dst);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        if (n == -2) {
                            this.readerThread = 0;
                            if (n <= 0 && n != -2) {
                                z = -assertionsDisabled;
                            }
                            end(z);
                            if (-assertionsDisabled || IOStatus.check(n)) {
                            } else {
                                throw new AssertionError();
                            }
                        }
                    }
                    byteBuffer = Util.getTemporaryDirectBuffer(dst.remaining());
                    while (true) {
                        n = receive(this.fd, byteBuffer);
                        if (n != -3 || !isOpen()) {
                            if (n == -2) {
                                break;
                            }
                            InetSocketAddress isa = this.sender;
                            try {
                                security.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                                break;
                            } catch (SecurityException e) {
                                byteBuffer.clear();
                            } catch (Throwable th) {
                                if (byteBuffer != null) {
                                    Util.releaseTemporaryDirectBuffer(byteBuffer);
                                }
                                this.readerThread = 0;
                                if (n <= 0 && n != -2) {
                                    z = -assertionsDisabled;
                                }
                                end(z);
                                if (!-assertionsDisabled && !IOStatus.check(n)) {
                                    AssertionError assertionError = new AssertionError();
                                }
                            }
                        }
                    }
                    if (byteBuffer != null) {
                        Util.releaseTemporaryDirectBuffer(byteBuffer);
                    }
                    this.readerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                    } else {
                        throw new AssertionError();
                    }
                    SocketAddress socketAddress = this.sender;
                    if (byteBuffer != null) {
                        Util.releaseTemporaryDirectBuffer(byteBuffer);
                    }
                    this.readerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.readerThread = 0;
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                } else {
                    throw new AssertionError();
                }
            }
        }
    }

    private int receive(FileDescriptor fd, ByteBuffer dst) throws IOException {
        int pos = dst.position();
        int lim = dst.limit();
        if (-assertionsDisabled || pos <= lim) {
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
        } else {
            throw new AssertionError();
        }
    }

    private int receiveIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb, int rem, int pos) throws IOException {
        FileDescriptor fileDescriptor = fd;
        int i = rem;
        int n = receive0(fileDescriptor, ((long) pos) + ((DirectBuffer) bb).address(), i, isConnected());
        if (n > 0) {
            bb.position(pos + n);
        }
        return n;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        boolean z = true;
        if (src == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            ensureOpen();
            InetSocketAddress isa = Net.checkAddress(target);
            InetAddress ia = isa.getAddress();
            if (ia == null) {
                throw new IOException("Target address not resolved");
            }
            synchronized (this.stateLock) {
                if (isConnected()) {
                    if (target.equals(this.remoteAddress)) {
                        int write = write(src);
                        return write;
                    }
                    throw new IllegalArgumentException("Connected address not equal to target address");
                } else if (target == null) {
                    throw new NullPointerException();
                } else {
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        if (ia.isMulticastAddress()) {
                            sm.checkMulticast(ia);
                        } else {
                            sm.checkConnect(ia.getHostAddress(), isa.getPort());
                        }
                    }
                }
            }
        }
    }

    private int send(FileDescriptor fd, ByteBuffer src, InetSocketAddress target) throws IOException {
        if (src instanceof DirectBuffer) {
            return sendFromNativeBuffer(fd, src, target);
        }
        int pos = src.position();
        int lim = src.limit();
        if (-assertionsDisabled || pos <= lim) {
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
        } else {
            throw new AssertionError();
        }
    }

    private int sendFromNativeBuffer(FileDescriptor fd, ByteBuffer bb, InetSocketAddress target) throws IOException {
        int pos = bb.position();
        int lim = bb.limit();
        if (-assertionsDisabled || pos <= lim) {
            int written;
            int rem = pos <= lim ? lim - pos : 0;
            try {
                written = send0(this.family != StandardProtocolFamily.INET ? true : -assertionsDisabled, fd, ((DirectBuffer) bb).address() + ((long) pos), rem, target.getAddress(), target.getPort());
            } catch (PortUnreachableException pue) {
                if (isConnected()) {
                    throw pue;
                }
                written = rem;
            }
            if (written > 0) {
                bb.position(pos + written);
            }
            return written;
        }
        throw new AssertionError();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(ByteBuffer buf) throws IOException {
        boolean z = true;
        if (buf == null) {
            throw new NullPointerException();
        }
        synchronized (this.readLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            try {
                begin();
                if (isOpen()) {
                    int n;
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
                    this.readerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.readerThread = 0;
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.readerThread = 0;
                if (null <= null && 0 != -2) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
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
            try {
                begin();
                long isOpen = isOpen();
                if (isOpen == null) {
                    this.readerThread = isOpen;
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                        return 0;
                    }
                    throw new AssertionError();
                }
                long n;
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
                this.readerThread = 0;
                boolean z = (n > 0 || n == -2) ? true : -assertionsDisabled;
                end(z);
                if (-assertionsDisabled || IOStatus.check(n)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.readerThread = 0;
                boolean z2 = (0 > 0 || 0 == -2) ? true : -assertionsDisabled;
                end(z2);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int write(ByteBuffer buf) throws IOException {
        boolean z = true;
        if (buf == null) {
            throw new NullPointerException();
        }
        synchronized (this.writeLock) {
            synchronized (this.stateLock) {
                ensureOpen();
                if (isConnected()) {
                } else {
                    throw new NotYetConnectedException();
                }
            }
            try {
                begin();
                if (isOpen()) {
                    int n;
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
                    this.writerThread = 0;
                    if (n <= 0 && n != -2) {
                        z = -assertionsDisabled;
                    }
                    end(z);
                    if (-assertionsDisabled || IOStatus.check(n)) {
                    } else {
                        throw new AssertionError();
                    }
                }
                this.writerThread = 0;
                end(-assertionsDisabled);
                if (-assertionsDisabled || IOStatus.check(0)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.writerThread = 0;
                if (null <= null && 0 != -2) {
                    z = -assertionsDisabled;
                }
                end(z);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
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
            try {
                begin();
                long isOpen = isOpen();
                if (isOpen == null) {
                    this.writerThread = isOpen;
                    end(-assertionsDisabled);
                    if (-assertionsDisabled || IOStatus.check(0)) {
                        return 0;
                    }
                    throw new AssertionError();
                }
                long n;
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
                this.writerThread = 0;
                boolean z = (n > 0 || n == -2) ? true : -assertionsDisabled;
                end(z);
                if (-assertionsDisabled || IOStatus.check(n)) {
                } else {
                    throw new AssertionError();
                }
            } finally {
                this.writerThread = 0;
                boolean z2 = (0 > 0 || 0 == -2) ? true : -assertionsDisabled;
                end(z2);
                if (!-assertionsDisabled && !IOStatus.check(0)) {
                    AssertionError assertionError = new AssertionError();
                }
            }
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        IOUtil.configureBlocking(this.fd, block);
    }

    public SocketAddress localAddress() {
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            socketAddress = this.localAddress;
        }
        return socketAddress;
    }

    public SocketAddress remoteAddress() {
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            socketAddress = this.remoteAddress;
        }
        return socketAddress;
    }

    public DatagramChannel bind(SocketAddress local) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    ensureOpen();
                    if (this.localAddress != null) {
                        throw new AlreadyBoundException();
                    }
                    InetSocketAddress isa;
                    if (local == null) {
                        isa = this.family == StandardProtocolFamily.INET ? new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0) : new InetSocketAddress(0);
                    } else {
                        isa = Net.checkAddress(local);
                        if (this.family == StandardProtocolFamily.INET && !(isa.getAddress() instanceof Inet4Address)) {
                            throw new UnsupportedAddressTypeException();
                        }
                    }
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkListen(isa.getPort());
                    }
                    Net.bind(this.family, this.fd, isa.getAddress(), isa.getPort());
                    this.localAddress = Net.localAddress(this.fd);
                }
            }
        }
        return this;
    }

    public boolean isConnected() {
        boolean z = true;
        synchronized (this.stateLock) {
            if (this.state != 1) {
                z = -assertionsDisabled;
            }
        }
        return z;
    }

    void ensureOpenAndUnconnected() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (this.state != 0) {
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
                    if (Net.connect(this.family, this.fd, isa.getAddress(), isa.getPort()) <= 0) {
                        throw new Error();
                    }
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
                                configureBlocking(-assertionsDisabled);
                            }
                            do {
                                tmpBuf.clear();
                            } while (receive(tmpBuf) != null);
                            if (blocking) {
                                configureBlocking(true);
                            }
                        } catch (Throwable th) {
                            if (null != null) {
                                configureBlocking(true);
                            }
                        }
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
                    if (isConnected() && (isOpen() ^ 1) == 0) {
                        InetSocketAddress isa = this.remoteAddress;
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
                        }
                        disconnect0(this.fd, this.family == StandardProtocolFamily.INET6 ? true : -assertionsDisabled);
                        this.remoteAddress = null;
                        this.state = 0;
                        this.localAddress = Net.localAddress(this.fd);
                        return this;
                    }
                    return this;
                }
            }
        }
    }

    private MembershipKey innerJoin(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        if (group.isMulticastAddress()) {
            if (group instanceof Inet4Address) {
                if (this.family == StandardProtocolFamily.INET6 && (Net.canIPv6SocketJoinIPv4Group() ^ 1) != 0) {
                    throw new IllegalArgumentException("IPv6 socket cannot join IPv4 multicast group");
                }
            } else if (!(group instanceof Inet6Address)) {
                throw new IllegalArgumentException("Address type not supported");
            } else if (this.family != StandardProtocolFamily.INET6) {
                throw new IllegalArgumentException("Only IPv6 sockets can join IPv6 multicast group");
            }
            if (source != null) {
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
                sm.checkMulticast(group);
            }
            synchronized (this.stateLock) {
                if (isOpen()) {
                    MembershipKeyImpl key;
                    if (this.registry == null) {
                        this.registry = new MembershipRegistry();
                    } else {
                        MembershipKey key2 = this.registry.checkMembership(group, interf, source);
                        if (key2 != null) {
                            return key2;
                        }
                    }
                    if (this.family == StandardProtocolFamily.INET6 && ((group instanceof Inet6Address) || Net.canJoin6WithIPv4Group())) {
                        int index = interf.getIndex();
                        if (index == -1) {
                            throw new IOException("Network interface cannot be identified");
                        }
                        byte[] bArr;
                        byte[] groupAddress = Net.inet6AsByteArray(group);
                        if (source == null) {
                            bArr = null;
                        } else {
                            bArr = Net.inet6AsByteArray(source);
                        }
                        if (Net.join6(this.fd, groupAddress, index, bArr) == -2) {
                            throw new UnsupportedOperationException();
                        }
                        key = new Type6(this, group, interf, source, groupAddress, index, bArr);
                    } else {
                        Inet4Address target = Net.anyInet4Address(interf);
                        if (target == null) {
                            throw new IOException("Network interface not configured for IPv4");
                        }
                        int groupAddress2 = Net.inet4AsInt(group);
                        int targetAddress = Net.inet4AsInt(target);
                        int sourceAddress = source == null ? 0 : Net.inet4AsInt(source);
                        if (Net.join4(this.fd, groupAddress2, targetAddress, sourceAddress) == -2) {
                            throw new UnsupportedOperationException();
                        }
                        MembershipKeyImpl type4 = new Type4(this, group, interf, source, groupAddress2, targetAddress, sourceAddress);
                    }
                    this.registry.add(key);
                    return key;
                }
                throw new ClosedChannelException();
            }
        }
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
        if (-assertionsDisabled || key.channel() == this) {
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
                    } catch (Object ioe) {
                        throw new AssertionError(ioe);
                    }
                }
                return;
            }
        }
        throw new AssertionError();
    }

    void block(MembershipKeyImpl key, InetAddress source) throws IOException {
        if (!-assertionsDisabled && key.channel() != this) {
            throw new AssertionError();
        } else if (-assertionsDisabled || key.sourceAddress() == null) {
            synchronized (this.stateLock) {
                if (!key.isValid()) {
                    throw new IllegalStateException("key is no longer valid");
                } else if (source.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("Source address is a wildcard address");
                } else if (source.isMulticastAddress()) {
                    throw new IllegalArgumentException("Source address is multicast address");
                } else if (source.getClass() != key.group().getClass()) {
                    throw new IllegalArgumentException("Source address is different type to group");
                } else {
                    int n;
                    if (key instanceof Type6) {
                        Type6 key6 = (Type6) key;
                        n = Net.block6(this.fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
                    } else {
                        Type4 key4 = (Type4) key;
                        n = Net.block4(this.fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
                    }
                    if (n == -2) {
                        throw new UnsupportedOperationException();
                    }
                }
            }
        } else {
            throw new AssertionError();
        }
    }

    void unblock(MembershipKeyImpl key, InetAddress source) {
        if (!-assertionsDisabled && key.channel() != this) {
            throw new AssertionError();
        } else if (-assertionsDisabled || key.sourceAddress() == null) {
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
                    } catch (Object ioe) {
                        throw new AssertionError(ioe);
                    }
                }
                throw new IllegalStateException("key is no longer valid");
            }
        } else {
            throw new AssertionError();
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
            long th = this.readerThread;
            if (th != 0) {
                NativeThread.signal(th);
            }
            th = this.writerThread;
            if (th != 0) {
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
            } else if (-assertionsDisabled || !(isOpen() || isRegistered())) {
                nd.close(this.fd);
                this.state = 2;
            } else {
                throw new AssertionError();
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

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, sk.nioReadyOps(), sk);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, 0, sk);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int poll(int events, long timeout) throws IOException {
        boolean z = -assertionsDisabled;
        if (-assertionsDisabled || (Thread.holdsLock(blockingLock()) && !isBlocking())) {
            synchronized (this.readLock) {
                long isOpen;
                try {
                    begin();
                    synchronized (this.stateLock) {
                        isOpen = isOpen();
                        if (isOpen != null) {
                            this.readerThread = NativeThread.current();
                        }
                    }
                } finally {
                    isOpen = 0;
                    this.readerThread = 0;
                    end(-assertionsDisabled);
                }
            }
        } else {
            throw new AssertionError();
        }
    }

    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        int newOps = 0;
        if ((ops & 1) != 0) {
            newOps = Net.POLLIN | 0;
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
