package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.net.NetHooks;

class ServerSocketChannelImpl extends ServerSocketChannel implements SelChImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ST_INUSE = 0;
    private static final int ST_KILLED = 1;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd = new SocketDispatcher();
    private final FileDescriptor fd;
    private int fdVal;
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object lock;
    ServerSocket socket;
    private int state;
    private final Object stateLock;
    private volatile long thread;

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet(2);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            set.add(StandardSocketOptions.IP_TOS);
            return Collections.unmodifiableSet(set);
        }
    }

    private native int accept0(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, InetSocketAddress[] inetSocketAddressArr) throws IOException;

    private static native void initIDs();

    static {
        initIDs();
    }

    ServerSocketChannelImpl(SelectorProvider sp) throws IOException {
        super(sp);
        this.thread = 0;
        this.lock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.fd = Net.serverSocket(true);
        this.fdVal = IOUtil.fdVal(this.fd);
        this.state = 0;
    }

    ServerSocketChannelImpl(SelectorProvider sp, FileDescriptor fd, boolean bound) throws IOException {
        super(sp);
        this.thread = 0;
        this.lock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        this.state = 0;
        if (bound) {
            this.localAddress = Net.localAddress(fd);
        }
    }

    public ServerSocket socket() {
        ServerSocket serverSocket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = ServerSocketAdaptor.create(this);
            }
            serverSocket = this.socket;
        }
        return serverSocket;
    }

    public SocketAddress getLocalAddress() throws IOException {
        SocketAddress socketAddress;
        synchronized (this.stateLock) {
            if (isOpen()) {
                if (this.localAddress == null) {
                    socketAddress = this.localAddress;
                } else {
                    socketAddress = Net.getRevealedLocalAddress(Net.asInetSocketAddress(this.localAddress));
                }
            } else {
                throw new ClosedChannelException();
            }
        }
        return socketAddress;
    }

    /* JADX WARNING: Missing block: B:24:0x0047, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        } else if (supportedOptions().contains(name)) {
            synchronized (this.stateLock) {
                if (!isOpen()) {
                    throw new ClosedChannelException();
                } else if (name == StandardSocketOptions.IP_TOS) {
                    Net.setSocketOption(this.fd, Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET, name, value);
                    return this;
                } else if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                    this.isReuseAddress = ((Boolean) value).booleanValue();
                } else {
                    Net.setSocketOption(this.fd, Net.UNSPEC, name, value);
                }
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
                if (!isOpen()) {
                    throw new ClosedChannelException();
                } else if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                    Boolean valueOf = Boolean.valueOf(this.isReuseAddress);
                    return valueOf;
                } else {
                    Object socketOption = Net.getSocketOption(this.fd, Net.UNSPEC, name);
                    return socketOption;
                }
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

    public boolean isBound() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.localAddress != null ? true : $assertionsDisabled;
        }
        return z;
    }

    public InetSocketAddress localAddress() {
        InetSocketAddress inetSocketAddress;
        synchronized (this.stateLock) {
            inetSocketAddress = this.localAddress;
        }
        return inetSocketAddress;
    }

    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        synchronized (this.lock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (isBound()) {
                throw new AlreadyBoundException();
            } else {
                InetSocketAddress isa;
                if (local == null) {
                    isa = new InetSocketAddress(0);
                } else {
                    isa = Net.checkAddress(local);
                }
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkListen(isa.getPort());
                }
                NetHooks.beforeTcpBind(this.fd, isa.getAddress(), isa.getPort());
                Net.bind(this.fd, isa.getAddress(), isa.getPort());
                Net.listen(this.fd, backlog < 1 ? 50 : backlog);
                synchronized (this.stateLock) {
                    this.localAddress = Net.localAddress(this.fd);
                }
            }
        }
        return this;
    }

    /* JADX WARNING: Missing block: B:29:?, code skipped:
            r13.thread = 0;
     */
    /* JADX WARNING: Missing block: B:30:0x004b, code skipped:
            if (r2 <= 0) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:31:0x004d, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:32:0x004f, code skipped:
            r6 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:33:0x0050, code skipped:
            end(r6);
     */
    /* JADX WARNING: Missing block: B:34:0x0054, code skipped:
            if (r2 >= 1) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:36:0x0057, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:37:0x0058, code skipped:
            sun.nio.ch.IOUtil.configureBlocking(r3, true);
            r4 = r5[0];
            r1 = new sun.nio.ch.SocketChannelImpl(provider(), r3, r4);
            r6 = java.lang.System.getSecurityManager();
     */
    /* JADX WARNING: Missing block: B:38:0x006b, code skipped:
            if (r6 == null) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            r6.checkAccept(r4.getAddress().getHostAddress(), r4.getPort());
     */
    /* JADX WARNING: Missing block: B:46:0x0083, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SocketChannel accept() throws IOException {
        synchronized (this.lock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (isBound()) {
                FileDescriptor newfd = new FileDescriptor();
                boolean z = true;
                InetSocketAddress[] isaa = new InetSocketAddress[1];
                try {
                    begin();
                    if (isOpen()) {
                        this.thread = NativeThread.current();
                        while (true) {
                            int n = accept(this.fd, newfd, isaa);
                            if (n != -3 || !isOpen()) {
                                break;
                            }
                        }
                    } else {
                        this.thread = 0;
                        if (null <= null) {
                            z = $assertionsDisabled;
                        }
                        end(z);
                        return null;
                    }
                } catch (SecurityException x) {
                    sc.close();
                    throw x;
                } catch (Throwable th) {
                    this.thread = 0;
                    if (null <= null) {
                        z = $assertionsDisabled;
                    }
                    end(z);
                }
            } else {
                throw new NotYetBoundException();
            }
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        IOUtil.configureBlocking(this.fd, block);
    }

    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            if (this.state != 1) {
                nd.preClose(this.fd);
            }
            long th = this.thread;
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
            if (this.state == 1) {
            } else if (this.state == -1) {
                this.state = 1;
            } else {
                nd.close(this.fd);
                this.state = 1;
            }
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
        if (!((Net.POLLIN & ops) == 0 || (intOps & 16) == 0)) {
            newOps |= 16;
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
            r9.thread = 0;
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
            r9.thread = 0;
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
        synchronized (this.lock) {
            boolean z = $assertionsDisabled;
            int n = 0;
            boolean z2 = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (isOpen()) {
                        this.thread = NativeThread.current();
                    }
                }
            } catch (Throwable th) {
                this.thread = 0;
                if (n > 0) {
                    z = true;
                }
                end(z);
            }
        }
    }

    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        int newOps = 0;
        if ((ops & 16) != 0) {
            newOps = 0 | Net.POLLIN;
        }
        sk.selector.putEventOps(sk, newOps);
    }

    public FileDescriptor getFD() {
        return this.fd;
    }

    public int getFDVal() {
        return this.fdVal;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append('[');
        if (isOpen()) {
            synchronized (this.stateLock) {
                InetSocketAddress addr = localAddress();
                if (addr == null) {
                    sb.append("unbound");
                } else {
                    sb.append(Net.getRevealedLocalAddressAsString(addr));
                }
            }
        } else {
            sb.append("closed");
        }
        sb.append(']');
        return sb.toString();
    }

    private int accept(FileDescriptor ssfd, FileDescriptor newfd, InetSocketAddress[] isaa) throws IOException {
        return accept0(ssfd, newfd, isaa);
    }
}
