package sun.nio.ch;

import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jdk.net.ExtendedSocketOptions;
import sun.net.ExtendedOptionsImpl;
import sun.net.NetHooks;

class SocketChannelImpl extends SocketChannel implements SelChImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ST_CONNECTED = 2;
    private static final int ST_KILLED = 4;
    private static final int ST_KILLPENDING = 3;
    private static final int ST_PENDING = 1;
    private static final int ST_UNCONNECTED = 0;
    private static final int ST_UNINITIALIZED = -1;
    private static NativeDispatcher nd = new SocketDispatcher();
    private final FileDescriptor fd;
    private final int fdVal;
    private final CloseGuard guard;
    private boolean isInputOpen;
    private boolean isOutputOpen;
    private boolean isReuseAddress;
    private InetSocketAddress localAddress;
    private final Object readLock;
    private volatile long readerThread;
    private boolean readyToConnect;
    private InetSocketAddress remoteAddress;
    private Socket socket;
    private int state;
    private final Object stateLock;
    private final Object writeLock;
    private volatile long writerThread;

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private DefaultOptionsHolder() {
        }

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet(8);
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_KEEPALIVE);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            set.add(StandardSocketOptions.SO_LINGER);
            set.add(StandardSocketOptions.TCP_NODELAY);
            set.add(StandardSocketOptions.IP_TOS);
            set.add(ExtendedSocketOption.SO_OOBINLINE);
            if (ExtendedOptionsImpl.flowSupported()) {
                set.add(ExtendedSocketOptions.SO_FLOW_SLA);
            }
            return Collections.unmodifiableSet(set);
        }
    }

    private static native int checkConnect(FileDescriptor fileDescriptor, boolean z, boolean z2) throws IOException;

    private static native int sendOutOfBandData(FileDescriptor fileDescriptor, byte b) throws IOException;

    SocketChannelImpl(SelectorProvider sp) throws IOException {
        super(sp);
        this.readerThread = 0;
        this.writerThread = 0;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.isInputOpen = true;
        this.isOutputOpen = true;
        this.readyToConnect = $assertionsDisabled;
        this.guard = CloseGuard.get();
        this.fd = Net.socket(true);
        this.fdVal = IOUtil.fdVal(this.fd);
        this.state = 0;
        if (this.fd != null && this.fd.valid()) {
            this.guard.open("close");
        }
    }

    SocketChannelImpl(SelectorProvider sp, FileDescriptor fd, boolean bound) throws IOException {
        super(sp);
        this.readerThread = 0;
        this.writerThread = 0;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.isInputOpen = true;
        this.isOutputOpen = true;
        this.readyToConnect = $assertionsDisabled;
        this.guard = CloseGuard.get();
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        this.state = 0;
        if (fd != null && fd.valid()) {
            this.guard.open("close");
        }
        if (bound) {
            this.localAddress = Net.localAddress(fd);
        }
    }

    SocketChannelImpl(SelectorProvider sp, FileDescriptor fd, InetSocketAddress remote) throws IOException {
        super(sp);
        this.readerThread = 0;
        this.writerThread = 0;
        this.readLock = new Object();
        this.writeLock = new Object();
        this.stateLock = new Object();
        this.state = -1;
        this.isInputOpen = true;
        this.isOutputOpen = true;
        this.readyToConnect = $assertionsDisabled;
        this.guard = CloseGuard.get();
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        this.state = 2;
        this.localAddress = Net.localAddress(fd);
        this.remoteAddress = remote;
        if (fd != null && fd.valid()) {
            this.guard.open("close");
        }
    }

    public Socket socket() {
        Socket socket;
        synchronized (this.stateLock) {
            if (this.socket == null) {
                this.socket = SocketAdaptor.create(this);
            }
            socket = this.socket;
        }
        return socket;
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

    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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
                    return this;
                } else {
                    Net.setSocketOption(this.fd, Net.UNSPEC, name, value);
                    return this;
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
                } else if (name == StandardSocketOptions.IP_TOS) {
                    Object socketOption = Net.getSocketOption(this.fd, Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET, name);
                    return socketOption;
                } else {
                    Object socketOption2 = Net.getSocketOption(this.fd, Net.UNSPEC, name);
                    return socketOption2;
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

    private boolean ensureReadOpen() throws ClosedChannelException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (!isConnected()) {
                throw new NotYetConnectedException();
            } else if (this.isInputOpen) {
                return true;
            } else {
                return $assertionsDisabled;
            }
        }
    }

    private void ensureWriteOpen() throws ClosedChannelException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (!this.isOutputOpen) {
                throw new ClosedChannelException();
            } else if (isConnected()) {
            } else {
                throw new NotYetConnectedException();
            }
        }
    }

    private void readerCleanup() throws IOException {
        synchronized (this.stateLock) {
            this.readerThread = 0;
            if (this.state == 3) {
                kill();
            }
        }
    }

    private void writerCleanup() throws IOException {
        synchronized (this.stateLock) {
            this.writerThread = 0;
            if (this.state == 3) {
                kill();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:94:0x0093  */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:18:0x0022, code skipped:
            if (r3 > 0) goto L_0x0029;
     */
    /* JADX WARNING: Missing block: B:19:0x0024, code skipped:
            if (r3 != -2) goto L_0x0027;
     */
    /* JADX WARNING: Missing block: B:21:0x0027, code skipped:
            r4 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:22:0x0029, code skipped:
            end(r4);
            r4 = r10.stateLock;
     */
    /* JADX WARNING: Missing block: B:23:0x002e, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:24:0x002f, code skipped:
            if (r3 > 0) goto L_0x003a;
     */
    /* JADX WARNING: Missing block: B:27:0x0033, code skipped:
            if (r10.isInputOpen != false) goto L_0x003a;
     */
    /* JADX WARNING: Missing block: B:28:0x0035, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:31:0x0037, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:37:0x003c, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            r3 = sun.nio.ch.IOUtil.read(r10.fd, r11, -1, nd);
     */
    /* JADX WARNING: Missing block: B:47:0x0052, code skipped:
            if (r3 != -3) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:49:0x0058, code skipped:
            if (isOpen() == false) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:51:0x005b, code skipped:
            r6 = sun.nio.ch.IOStatus.normalize(r3);
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:54:0x0062, code skipped:
            if (r3 > 0) goto L_0x0068;
     */
    /* JADX WARNING: Missing block: B:55:0x0064, code skipped:
            if (r3 != -2) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:58:0x0068, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:59:0x0069, code skipped:
            end(r1);
            r1 = r10.stateLock;
     */
    /* JADX WARNING: Missing block: B:60:0x006e, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:61:0x006f, code skipped:
            if (r3 > 0) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:64:0x0073, code skipped:
            if (r10.isInputOpen != false) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:65:0x0075, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:68:0x0077, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:69:0x0078, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:71:?, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:74:0x007c, code skipped:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(ByteBuffer buf) throws IOException {
        if (buf != null) {
            synchronized (this.readLock) {
                if (ensureReadOpen()) {
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
                        readerCleanup();
                        if (n <= 0) {
                            if (n != -2) {
                                end(z);
                                synchronized (this.stateLock) {
                                    if (n <= 0) {
                                        try {
                                            if (!this.isInputOpen) {
                                                return -1;
                                            }
                                        } catch (Throwable th2) {
                                            Throwable th3;
                                            while (true) {
                                                th3 = th2;
                                            }
                                            throw th3;
                                        }
                                    }
                                }
                            }
                        }
                        z = true;
                        end(z);
                        synchronized (this.stateLock) {
                        }
                    }
                } else {
                    return -1;
                }
            }
        }
        throw new NullPointerException();
        while (true) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:103:0x00b7  */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:22:0x0035, code skipped:
            if (r10 > 0) goto L_0x003d;
     */
    /* JADX WARNING: Missing block: B:24:0x0039, code skipped:
            if (r10 != -2) goto L_0x003c;
     */
    /* JADX WARNING: Missing block: B:27:0x003d, code skipped:
            r13 = true;
     */
    /* JADX WARNING: Missing block: B:28:0x003e, code skipped:
            end(r13);
            r12 = r1.stateLock;
     */
    /* JADX WARNING: Missing block: B:29:0x0043, code skipped:
            monitor-enter(r12);
     */
    /* JADX WARNING: Missing block: B:31:0x0046, code skipped:
            if (r10 > 0) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:34:0x004a, code skipped:
            if (r1.isInputOpen != false) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:35:0x004c, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:38:0x004e, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:39:0x004f, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:41:?, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:44:0x0053, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            r10 = sun.nio.ch.IOUtil.read(r1.fd, r2, r3, r4, nd);
     */
    /* JADX WARNING: Missing block: B:54:0x006a, code skipped:
            if (r10 != -3) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:56:0x0070, code skipped:
            if (isOpen() == false) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:58:0x0073, code skipped:
            r6 = sun.nio.ch.IOStatus.normalize(r10);
     */
    /* JADX WARNING: Missing block: B:60:?, code skipped:
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:61:0x007c, code skipped:
            if (r10 > 0) goto L_0x0084;
     */
    /* JADX WARNING: Missing block: B:63:0x0080, code skipped:
            if (r10 != -2) goto L_0x0083;
     */
    /* JADX WARNING: Missing block: B:66:0x0084, code skipped:
            r13 = true;
     */
    /* JADX WARNING: Missing block: B:67:0x0085, code skipped:
            end(r13);
            r12 = r1.stateLock;
     */
    /* JADX WARNING: Missing block: B:68:0x008a, code skipped:
            monitor-enter(r12);
     */
    /* JADX WARNING: Missing block: B:70:0x008d, code skipped:
            if (r10 > 0) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:73:0x0091, code skipped:
            if (r1.isInputOpen != false) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:74:0x0093, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:79:0x0098, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:81:?, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:84:0x009c, code skipped:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        ByteBuffer[] byteBufferArr = dsts;
        int i = offset;
        int i2 = length;
        if (i < 0 || i2 < 0 || i > byteBufferArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.readLock) {
            if (ensureReadOpen()) {
                long n = 0;
                boolean z = $assertionsDisabled;
                try {
                    begin();
                    synchronized (this.stateLock) {
                        if (isOpen()) {
                            this.readerThread = NativeThread.current();
                        }
                    }
                } catch (Throwable th) {
                    readerCleanup();
                    if (n <= 0) {
                        if (n != -2) {
                            end(z);
                            synchronized (this.stateLock) {
                                if (n <= 0) {
                                    try {
                                        if (!this.isInputOpen) {
                                            return -1;
                                        }
                                    } catch (Throwable th2) {
                                        Throwable th3;
                                        while (true) {
                                            th3 = th2;
                                        }
                                        throw th3;
                                    }
                                }
                            }
                        }
                    }
                    z = true;
                    end(z);
                    synchronized (this.stateLock) {
                    }
                }
            } else {
                return -1;
            }
        }
        while (true) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:83:0x0095  */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:22:0x002b, B:84:0x0097] */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            writerCleanup();
     */
    /* JADX WARNING: Missing block: B:15:0x001c, code skipped:
            if (r2 > 0) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:16:0x001e, code skipped:
            if (r2 != -2) goto L_0x0021;
     */
    /* JADX WARNING: Missing block: B:18:0x0021, code skipped:
            r3 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:19:0x0023, code skipped:
            end(r3);
            r3 = r9.stateLock;
     */
    /* JADX WARNING: Missing block: B:20:0x0028, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:21:0x0029, code skipped:
            if (r2 > 0) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:24:0x002d, code skipped:
            if (r9.isOutputOpen == false) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:26:0x0035, code skipped:
            throw new java.nio.channels.AsynchronousCloseException();
     */
    /* JADX WARNING: Missing block: B:29:0x0038, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:32:0x003a, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:41:?, code skipped:
            r2 = sun.nio.ch.IOUtil.write(r9.fd, r10, -1, nd);
     */
    /* JADX WARNING: Missing block: B:42:0x0050, code skipped:
            if (r2 != -3) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:44:0x0056, code skipped:
            if (isOpen() == false) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:46:0x0059, code skipped:
            r5 = sun.nio.ch.IOStatus.normalize(r2);
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            writerCleanup();
     */
    /* JADX WARNING: Missing block: B:49:0x0060, code skipped:
            if (r2 > 0) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:50:0x0062, code skipped:
            if (r2 != -2) goto L_0x0065;
     */
    /* JADX WARNING: Missing block: B:53:0x0066, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:54:0x0067, code skipped:
            end(r1);
            r1 = r9.stateLock;
     */
    /* JADX WARNING: Missing block: B:55:0x006c, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:56:0x006d, code skipped:
            if (r2 > 0) goto L_0x007c;
     */
    /* JADX WARNING: Missing block: B:59:0x0071, code skipped:
            if (r9.isOutputOpen == false) goto L_0x0074;
     */
    /* JADX WARNING: Missing block: B:62:0x0079, code skipped:
            throw new java.nio.channels.AsynchronousCloseException();
     */
    /* JADX WARNING: Missing block: B:63:0x007a, code skipped:
            r3 = th;
     */
    /* JADX WARNING: Missing block: B:65:0x007c, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:68:0x007e, code skipped:
            return r5;
     */
    /* JADX WARNING: Missing block: B:72:?, code skipped:
            throw r3;
     */
    /* JADX WARNING: Missing block: B:89:0x00a2, code skipped:
            r3 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int write(ByteBuffer buf) throws IOException {
        int i;
        if (buf != null) {
            synchronized (this.writeLock) {
                ensureWriteOpen();
                boolean z = $assertionsDisabled;
                int n = 0;
                boolean z2 = true;
                try {
                    begin();
                    synchronized (this.stateLock) {
                        if (isOpen()) {
                            this.writerThread = NativeThread.current();
                        }
                    }
                } catch (Throwable th) {
                    writerCleanup();
                    if (n <= 0) {
                        if (n != -2) {
                            end(z);
                            i = this.stateLock;
                            synchronized (i) {
                                if (n <= 0) {
                                    Object obj = this.isOutputOpen;
                                    if (obj == null) {
                                        AsynchronousCloseException asynchronousCloseException = new AsynchronousCloseException();
                                    }
                                }
                            }
                        }
                    }
                    z = true;
                    end(z);
                    i = this.stateLock;
                    synchronized (i) {
                    }
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    /* JADX WARNING: Missing block: B:17:?, code skipped:
            writerCleanup();
     */
    /* JADX WARNING: Missing block: B:18:0x0027, code skipped:
            if (r3 > 0) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:20:0x002b, code skipped:
            if (r3 != -2) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:22:0x002e, code skipped:
            r5 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:23:0x0030, code skipped:
            end(r5);
            r5 = r12.stateLock;
     */
    /* JADX WARNING: Missing block: B:24:0x0035, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:26:0x0038, code skipped:
            if (r3 > 0) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:29:0x003c, code skipped:
            if (r12.isOutputOpen == false) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:32:0x0044, code skipped:
            throw new java.nio.channels.AsynchronousCloseException();
     */
    /* JADX WARNING: Missing block: B:33:0x0045, code skipped:
            r1 = th;
     */
    /* JADX WARNING: Missing block: B:35:0x0047, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:38:0x0049, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            r3 = sun.nio.ch.IOUtil.write(r12.fd, r13, r14, r15, nd);
     */
    /* JADX WARNING: Missing block: B:48:0x0060, code skipped:
            if (r3 != -3) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:50:0x0066, code skipped:
            if (isOpen() == false) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:52:0x0069, code skipped:
            r9 = sun.nio.ch.IOStatus.normalize(r3);
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            writerCleanup();
     */
    /* JADX WARNING: Missing block: B:55:0x0072, code skipped:
            if (r3 > 0) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:57:0x0076, code skipped:
            if (r3 != -2) goto L_0x0079;
     */
    /* JADX WARNING: Missing block: B:59:0x0079, code skipped:
            r5 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:60:0x007b, code skipped:
            end(r5);
            r5 = r12.stateLock;
     */
    /* JADX WARNING: Missing block: B:61:0x0080, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:63:0x0083, code skipped:
            if (r3 > 0) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:66:0x0087, code skipped:
            if (r12.isOutputOpen == false) goto L_0x008a;
     */
    /* JADX WARNING: Missing block: B:69:0x008f, code skipped:
            throw new java.nio.channels.AsynchronousCloseException();
     */
    /* JADX WARNING: Missing block: B:70:0x0090, code skipped:
            r1 = th;
     */
    /* JADX WARNING: Missing block: B:72:0x0092, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:75:0x0094, code skipped:
            return r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > srcs.length - length) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (this.writeLock) {
            ensureWriteOpen();
            long n = 0;
            boolean z = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (isOpen()) {
                        this.writerThread = NativeThread.current();
                    }
                }
            } catch (Throwable th) {
                writerCleanup();
                if (n <= 0) {
                    if (n != -2) {
                        z = $assertionsDisabled;
                    }
                }
                end(z);
                synchronized (this.stateLock) {
                    if (n <= 0) {
                        try {
                            if (!this.isOutputOpen) {
                                AsynchronousCloseException asynchronousCloseException = new AsynchronousCloseException();
                            }
                        } catch (Throwable th2) {
                            Throwable th3;
                            while (true) {
                                th3 = th2;
                            }
                            throw th3;
                        }
                    }
                }
            }
        }
        while (true) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:82:0x008f  */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:21:0x0029, B:83:0x0091] */
    /* JADX WARNING: Missing block: B:13:?, code skipped:
            writerCleanup();
     */
    /* JADX WARNING: Missing block: B:14:0x001a, code skipped:
            if (r2 > 0) goto L_0x0021;
     */
    /* JADX WARNING: Missing block: B:15:0x001c, code skipped:
            if (r2 != -2) goto L_0x001f;
     */
    /* JADX WARNING: Missing block: B:17:0x001f, code skipped:
            r3 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:18:0x0021, code skipped:
            end(r3);
            r3 = r8.stateLock;
     */
    /* JADX WARNING: Missing block: B:19:0x0026, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:20:0x0027, code skipped:
            if (r2 > 0) goto L_0x0036;
     */
    /* JADX WARNING: Missing block: B:23:0x002b, code skipped:
            if (r8.isOutputOpen == false) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:25:0x0033, code skipped:
            throw new java.nio.channels.AsynchronousCloseException();
     */
    /* JADX WARNING: Missing block: B:28:0x0036, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:31:0x0038, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            r2 = sendOutOfBandData(r8.fd, r9);
     */
    /* JADX WARNING: Missing block: B:41:0x004a, code skipped:
            if (r2 != -3) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:43:0x0050, code skipped:
            if (isOpen() == false) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:45:0x0053, code skipped:
            r5 = sun.nio.ch.IOStatus.normalize(r2);
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            writerCleanup();
     */
    /* JADX WARNING: Missing block: B:48:0x005a, code skipped:
            if (r2 > 0) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:49:0x005c, code skipped:
            if (r2 != -2) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:52:0x0060, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:53:0x0061, code skipped:
            end(r1);
            r1 = r8.stateLock;
     */
    /* JADX WARNING: Missing block: B:54:0x0066, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:55:0x0067, code skipped:
            if (r2 > 0) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:58:0x006b, code skipped:
            if (r8.isOutputOpen == false) goto L_0x006e;
     */
    /* JADX WARNING: Missing block: B:61:0x0073, code skipped:
            throw new java.nio.channels.AsynchronousCloseException();
     */
    /* JADX WARNING: Missing block: B:62:0x0074, code skipped:
            r3 = th;
     */
    /* JADX WARNING: Missing block: B:64:0x0076, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:67:0x0078, code skipped:
            return r5;
     */
    /* JADX WARNING: Missing block: B:71:?, code skipped:
            throw r3;
     */
    /* JADX WARNING: Missing block: B:88:0x009c, code skipped:
            r3 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int sendOutOfBandData(byte b) throws IOException {
        int i;
        synchronized (this.writeLock) {
            ensureWriteOpen();
            boolean z = $assertionsDisabled;
            int n = 0;
            boolean z2 = true;
            try {
                begin();
                synchronized (this.stateLock) {
                    if (isOpen()) {
                        this.writerThread = NativeThread.current();
                    }
                }
            } catch (Throwable th) {
                writerCleanup();
                if (n <= 0) {
                    if (n != -2) {
                        end(z);
                        i = this.stateLock;
                        synchronized (i) {
                            if (n <= 0) {
                                Object obj = this.isOutputOpen;
                                if (obj == null) {
                                    AsynchronousCloseException asynchronousCloseException = new AsynchronousCloseException();
                                }
                            }
                        }
                    }
                }
                z = true;
                end(z);
                i = this.stateLock;
                synchronized (i) {
                }
            }
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        IOUtil.configureBlocking(this.fd, block);
    }

    public InetSocketAddress localAddress() {
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

    public SocketChannel bind(SocketAddress local) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    if (!isOpen()) {
                        throw new ClosedChannelException();
                    } else if (this.state == 1) {
                        throw new ConnectionPendingException();
                    } else if (this.localAddress == null) {
                        InetSocketAddress isa = local == null ? new InetSocketAddress(0) : Net.checkAddress(local);
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkListen(isa.getPort());
                        }
                        NetHooks.beforeTcpBind(this.fd, isa.getAddress(), isa.getPort());
                        Net.bind(this.fd, isa.getAddress(), isa.getPort());
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
            z = this.state == 2 ? true : $assertionsDisabled;
        }
        return z;
    }

    public boolean isConnectionPending() {
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
            } else if (this.state == 2) {
                throw new AlreadyConnectedException();
            } else if (this.state != 1) {
            } else {
                throw new ConnectionPendingException();
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:?, code skipped:
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:21:0x003c, code skipped:
            if (r7 > 0) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:22:0x003e, code skipped:
            if (r7 != -2) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:24:0x0041, code skipped:
            r9 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:25:0x0043, code skipped:
            end(r9);
     */
    /* JADX WARNING: Missing block: B:32:0x0049, code skipped:
            return $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            r10 = r3.getAddress();
     */
    /* JADX WARNING: Missing block: B:41:0x006a, code skipped:
            if (r10.isAnyLocalAddress() == false) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:42:0x006c, code skipped:
            r10 = java.net.InetAddress.getLocalHost();
     */
    /* JADX WARNING: Missing block: B:43:0x0071, code skipped:
            r7 = sun.nio.ch.Net.connect(r14.fd, r10, r3.getPort());
     */
    /* JADX WARNING: Missing block: B:44:0x007d, code skipped:
            if (r7 != -3) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:46:0x0083, code skipped:
            if (isOpen() == false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:50:0x0089, code skipped:
            if (r7 > 0) goto L_0x0090;
     */
    /* JADX WARNING: Missing block: B:51:0x008b, code skipped:
            if (r7 != -2) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:53:0x008e, code skipped:
            r8 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:54:0x0090, code skipped:
            r8 = true;
     */
    /* JADX WARNING: Missing block: B:55:0x0091, code skipped:
            end(r8);
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            r8 = r14.stateLock;
     */
    /* JADX WARNING: Missing block: B:58:0x0098, code skipped:
            monitor-enter(r8);
     */
    /* JADX WARNING: Missing block: B:60:?, code skipped:
            r14.remoteAddress = r3;
     */
    /* JADX WARNING: Missing block: B:61:0x009b, code skipped:
            if (r7 <= 0) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:62:0x009d, code skipped:
            r14.state = 2;
     */
    /* JADX WARNING: Missing block: B:63:0x00a4, code skipped:
            if (isOpen() == false) goto L_0x00ae;
     */
    /* JADX WARNING: Missing block: B:64:0x00a6, code skipped:
            r14.localAddress = sun.nio.ch.Net.localAddress(r14.fd);
     */
    /* JADX WARNING: Missing block: B:65:0x00ae, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:72:0x00b2, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:75:0x00b7, code skipped:
            if (isBlocking() != false) goto L_0x00c9;
     */
    /* JADX WARNING: Missing block: B:76:0x00b9, code skipped:
            r14.state = 1;
     */
    /* JADX WARNING: Missing block: B:77:0x00bf, code skipped:
            if (isOpen() == false) goto L_0x00c9;
     */
    /* JADX WARNING: Missing block: B:78:0x00c1, code skipped:
            r14.localAddress = sun.nio.ch.Net.localAddress(r14.fd);
     */
    /* JADX WARNING: Missing block: B:79:0x00c9, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:86:0x00cd, code skipped:
            return $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:108:0x00e5, code skipped:
            r6 = move-exception;
     */
    /* JADX WARNING: Missing block: B:110:?, code skipped:
            close();
     */
    /* JADX WARNING: Missing block: B:111:0x00e9, code skipped:
            throw r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean connect(SocketAddress sa) throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                ensureOpenAndUnconnected();
                InetSocketAddress isa = Net.checkAddress(sa);
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
                }
                synchronized (blockingLock()) {
                    boolean z = $assertionsDisabled;
                    int n = 0;
                    boolean z2 = true;
                    try {
                        begin();
                        synchronized (this.stateLock) {
                            if (isOpen()) {
                                if (this.localAddress == null) {
                                    NetHooks.beforeTcpConnect(this.fd, isa.getAddress(), isa.getPort());
                                }
                                this.readerThread = NativeThread.current();
                            }
                        }
                    } catch (Throwable th) {
                        readerCleanup();
                        if (n <= 0) {
                            if (n != -2) {
                                end(z);
                            }
                        }
                        z = true;
                        end(z);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:55:0x0054=Splitter:B:55:0x0054, B:88:0x00a8=Splitter:B:88:0x00a8, B:136:0x00eb=Splitter:B:136:0x00eb, B:45:0x004c=Splitter:B:45:0x004c} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:34:0x0037, B:129:0x00d9] */
    /* JADX WARNING: Missing block: B:23:0x001e, code skipped:
            r2 = $assertionsDisabled;
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            begin();
            r10 = blockingLock();
     */
    /* JADX WARNING: Missing block: B:26:0x002b, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:30:0x0033, code skipped:
            if (isOpen() != false) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:31:0x0035, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:33:?, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r4 = r14.stateLock;
     */
    /* JADX WARNING: Missing block: B:36:0x0039, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:38:?, code skipped:
            r14.readerThread = 0;
     */
    /* JADX WARNING: Missing block: B:39:0x003e, code skipped:
            if (r14.state != 3) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:40:0x0040, code skipped:
            kill();
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:41:0x0044, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:42:0x0045, code skipped:
            if (r3 > 0) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:43:0x0047, code skipped:
            if (r3 != -2) goto L_0x004a;
     */
    /* JADX WARNING: Missing block: B:44:0x004a, code skipped:
            r5 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            end(r5);
     */
    /* JADX WARNING: Missing block: B:51:0x0051, code skipped:
            return $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:52:0x0052, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:56:?, code skipped:
            throw r2;
     */
    /* JADX WARNING: Missing block: B:58:?, code skipped:
            r14.readerThread = sun.nio.ch.NativeThread.current();
     */
    /* JADX WARNING: Missing block: B:59:0x005b, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:61:?, code skipped:
            dalvik.system.BlockGuard.getThreadPolicy().onNetwork();
     */
    /* JADX WARNING: Missing block: B:62:0x0068, code skipped:
            if (isBlocking() != false) goto L_0x007c;
     */
    /* JADX WARNING: Missing block: B:63:0x006a, code skipped:
            r3 = checkConnect(r14.fd, $assertionsDisabled, r14.readyToConnect);
     */
    /* JADX WARNING: Missing block: B:64:0x0073, code skipped:
            if (r3 != -3) goto L_0x0091;
     */
    /* JADX WARNING: Missing block: B:66:0x0079, code skipped:
            if (isOpen() == false) goto L_0x0091;
     */
    /* JADX WARNING: Missing block: B:68:0x007c, code skipped:
            r3 = checkConnect(r14.fd, true, r14.readyToConnect);
     */
    /* JADX WARNING: Missing block: B:69:0x0085, code skipped:
            if (r3 != 0) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:71:0x0088, code skipped:
            if (r3 != -3) goto L_0x0091;
     */
    /* JADX WARNING: Missing block: B:73:0x008e, code skipped:
            if (isOpen() == false) goto L_0x0091;
     */
    /* JADX WARNING: Missing block: B:75:0x0091, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:77:?, code skipped:
            r10 = r14.stateLock;
     */
    /* JADX WARNING: Missing block: B:78:0x0094, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:80:?, code skipped:
            r14.readerThread = 0;
     */
    /* JADX WARNING: Missing block: B:81:0x0099, code skipped:
            if (r14.state != 3) goto L_0x009f;
     */
    /* JADX WARNING: Missing block: B:82:0x009b, code skipped:
            kill();
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:83:0x009f, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:84:0x00a0, code skipped:
            if (r3 > 0) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:85:0x00a2, code skipped:
            if (r3 != -2) goto L_0x00a5;
     */
    /* JADX WARNING: Missing block: B:86:0x00a5, code skipped:
            r6 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:87:0x00a7, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:89:?, code skipped:
            end(r6);
     */
    /* JADX WARNING: Missing block: B:90:0x00ad, code skipped:
            if (r3 <= 0) goto L_0x00c9;
     */
    /* JADX WARNING: Missing block: B:92:?, code skipped:
            r2 = r14.stateLock;
     */
    /* JADX WARNING: Missing block: B:93:0x00b1, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:95:?, code skipped:
            r14.state = 2;
     */
    /* JADX WARNING: Missing block: B:96:0x00b8, code skipped:
            if (isOpen() == false) goto L_0x00c2;
     */
    /* JADX WARNING: Missing block: B:97:0x00ba, code skipped:
            r14.localAddress = sun.nio.ch.Net.localAddress(r14.fd);
     */
    /* JADX WARNING: Missing block: B:98:0x00c2, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:103:0x00c5, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:112:0x00cb, code skipped:
            return $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:113:0x00cc, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:128:0x00d8, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:130:?, code skipped:
            r14.readerThread = 0;
     */
    /* JADX WARNING: Missing block: B:131:0x00dd, code skipped:
            if (r14.state == 3) goto L_0x00df;
     */
    /* JADX WARNING: Missing block: B:132:0x00df, code skipped:
            kill();
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:135:0x00ea, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:137:?, code skipped:
            end(r2);
     */
    /* JADX WARNING: Missing block: B:139:0x00ef, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:142:0x00f3, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:144:?, code skipped:
            close();
     */
    /* JADX WARNING: Missing block: B:145:0x00f7, code skipped:
            throw r2;
     */
    /* JADX WARNING: Missing block: B:169:?, code skipped:
            r10 = r14.stateLock;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean finishConnect() throws IOException {
        synchronized (this.readLock) {
            synchronized (this.writeLock) {
                synchronized (this.stateLock) {
                    if (isOpen()) {
                        boolean z = true;
                        if (this.state == 2) {
                            return true;
                        } else if (this.state == 1) {
                        } else {
                            throw new NoConnectionPendingException();
                        }
                    }
                    throw new ClosedChannelException();
                }
            }
        }
    }

    public SocketChannel shutdownInput() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (isConnected()) {
                if (this.isInputOpen) {
                    Net.shutdown(this.fd, 0);
                    if (this.readerThread != 0) {
                        NativeThread.signal(this.readerThread);
                    }
                    this.isInputOpen = $assertionsDisabled;
                }
            } else {
                throw new NotYetConnectedException();
            }
        }
        return this;
    }

    public SocketChannel shutdownOutput() throws IOException {
        synchronized (this.stateLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            } else if (isConnected()) {
                if (this.isOutputOpen) {
                    Net.shutdown(this.fd, 1);
                    if (this.writerThread != 0) {
                        NativeThread.signal(this.writerThread);
                    }
                    this.isOutputOpen = $assertionsDisabled;
                }
            } else {
                throw new NotYetConnectedException();
            }
        }
        return this;
    }

    public boolean isInputOpen() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.isInputOpen;
        }
        return z;
    }

    public boolean isOutputOpen() {
        boolean z;
        synchronized (this.stateLock) {
            z = this.isOutputOpen;
        }
        return z;
    }

    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            this.isInputOpen = $assertionsDisabled;
            this.isOutputOpen = $assertionsDisabled;
            if (this.state != 4) {
                this.guard.close();
                nd.preClose(this.fd);
            }
            if (this.readerThread != 0) {
                NativeThread.signal(this.readerThread);
            }
            if (this.writerThread != 0) {
                NativeThread.signal(this.writerThread);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0030, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 4) {
            } else if (this.state == -1) {
                this.state = 4;
            } else if (this.readerThread == 0 && this.writerThread == 0) {
                nd.close(this.fd);
                this.state = 4;
            } else {
                this.state = 3;
            }
        }
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
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
            this.readyToConnect = true;
            if (((~oldOps) & newOps) != 0) {
                z = true;
            }
            return z;
        }
        if (!((Net.POLLIN & ops) == 0 || (intOps & 1) == 0 || this.state != 2)) {
            newOps |= 1;
        }
        if (!((Net.POLLCONN & ops) == 0 || (intOps & 8) == 0 || (this.state != 0 && this.state != 1))) {
            newOps |= 8;
            this.readyToConnect = true;
        }
        if (!((Net.POLLOUT & ops) == 0 || (intOps & 4) == 0 || this.state != 2)) {
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
            readerCleanup();
     */
    /* JADX WARNING: Missing block: B:12:0x0017, code skipped:
            if (r2 <= 0) goto L_0x001a;
     */
    /* JADX WARNING: Missing block: B:14:0x001a, code skipped:
            r3 = $assertionsDisabled;
     */
    /* JADX WARNING: Missing block: B:15:0x001b, code skipped:
            end(r3);
     */
    /* JADX WARNING: Missing block: B:17:0x001f, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:23:0x002d, code skipped:
            r2 = sun.nio.ch.Net.poll(r7.fd, r8, r9);
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            readerCleanup();
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
                readerCleanup();
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
            newOps |= Net.POLLCONN;
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
        sb.append(getClass().getSuperclass().getName());
        sb.append('[');
        if (isOpen()) {
            synchronized (this.stateLock) {
                switch (this.state) {
                    case 0:
                        sb.append("unconnected");
                        break;
                    case 1:
                        sb.append("connection-pending");
                        break;
                    case 2:
                        sb.append("connected");
                        if (!this.isInputOpen) {
                            sb.append(" ishut");
                        }
                        if (!this.isOutputOpen) {
                            sb.append(" oshut");
                            break;
                        }
                        break;
                    default:
                        break;
                }
                InetSocketAddress addr = localAddress();
                if (addr != null) {
                    sb.append(" local=");
                    sb.append(Net.getRevealedLocalAddressAsString(addr));
                }
                if (remoteAddress() != null) {
                    sb.append(" remote=");
                    sb.append(remoteAddress().toString());
                }
            }
        } else {
            sb.append("closed");
        }
        sb.append(']');
        return sb.toString();
    }
}
