package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class SocketAdaptor extends Socket {
    private final SocketChannelImpl sc;
    private InputStream socketInputStream = null;
    private volatile int timeout = 0;

    private class SocketInputStream extends ChannelInputStream {
        /* synthetic */ SocketInputStream(SocketAdaptor x0, AnonymousClass1 x1) {
            this();
        }

        private SocketInputStream() {
            super(SocketAdaptor.this.sc);
        }

        /* JADX WARNING: Missing block: B:21:0x005a, code skipped:
            return r3;
     */
        /* JADX WARNING: Missing block: B:33:0x00a4, code skipped:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected int read(ByteBuffer bb) throws IOException {
            int n;
            synchronized (SocketAdaptor.this.sc.blockingLock()) {
                if (!SocketAdaptor.this.sc.isBlocking()) {
                    throw new IllegalBlockingModeException();
                } else if (SocketAdaptor.this.timeout == 0) {
                    int read = SocketAdaptor.this.sc.read(bb);
                    return read;
                } else {
                    SocketAdaptor.this.sc.configureBlocking(false);
                    try {
                        int read2 = SocketAdaptor.this.sc.read(bb);
                        n = read2;
                        if (read2 == 0) {
                            long to = (long) SocketAdaptor.this.timeout;
                            while (SocketAdaptor.this.sc.isOpen()) {
                                long st = System.currentTimeMillis();
                                if (SocketAdaptor.this.sc.poll(Net.POLLIN, to) > 0) {
                                    int read3 = SocketAdaptor.this.sc.read(bb);
                                    n = read3;
                                    if (read3 != 0) {
                                    }
                                }
                                to -= System.currentTimeMillis() - st;
                                if (to <= 0) {
                                    throw new SocketTimeoutException();
                                }
                            }
                            throw new ClosedChannelException();
                        } else if (SocketAdaptor.this.sc.isOpen()) {
                            SocketAdaptor.this.sc.configureBlocking(true);
                        }
                    } finally {
                        n = SocketAdaptor.this.sc.isOpen();
                        if (n != 0) {
                            n = SocketAdaptor.this.sc;
                            n.configureBlocking(true);
                        }
                    }
                }
            }
        }
    }

    private SocketAdaptor(SocketChannelImpl sc) throws SocketException {
        super(new FileDescriptorHolderSocketImpl(sc.getFD()));
        this.sc = sc;
    }

    public static Socket create(SocketChannelImpl sc) {
        try {
            return new SocketAdaptor(sc);
        } catch (SocketException e) {
            throw new InternalError("Should not reach here");
        }
    }

    public SocketChannel getChannel() {
        return this.sc;
    }

    public void connect(SocketAddress remote) throws IOException {
        connect(remote, 0);
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:12:0x001d, B:30:0x0040] */
    /* JADX WARNING: Missing block: B:57:0x0094, code skipped:
            if (r9.sc.isOpen() != false) goto L_0x0096;
     */
    /* JADX WARNING: Missing block: B:58:0x0096, code skipped:
            r9.sc.configureBlocking(true);
     */
    /* JADX WARNING: Missing block: B:60:0x009c, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:62:?, code skipped:
            sun.nio.ch.Net.translateException(r2, true);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void connect(SocketAddress remote, int timeout) throws IOException {
        if (remote == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        } else if (timeout >= 0) {
            synchronized (this.sc.blockingLock()) {
                if (!this.sc.isBlocking()) {
                    throw new IllegalBlockingModeException();
                } else if (timeout == 0) {
                    try {
                        this.sc.connect(remote);
                    } catch (Exception ex) {
                        Net.translateException(ex);
                    }
                    return;
                } else {
                    this.sc.configureBlocking(false);
                    if (this.sc.connect(remote)) {
                        if (this.sc.isOpen()) {
                            this.sc.configureBlocking(true);
                        }
                        return;
                    }
                    long to = (long) timeout;
                    while (this.sc.isOpen()) {
                        long st = System.currentTimeMillis();
                        if (this.sc.poll(Net.POLLCONN, to) <= 0 || !this.sc.finishConnect()) {
                            to -= System.currentTimeMillis() - st;
                            if (to <= 0) {
                                try {
                                    this.sc.close();
                                } catch (IOException e) {
                                }
                                throw new SocketTimeoutException();
                            }
                        } else if (this.sc.isOpen()) {
                            this.sc.configureBlocking(true);
                        }
                    }
                    throw new ClosedChannelException();
                }
            }
        } else {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
    }

    public void bind(SocketAddress local) throws IOException {
        try {
            this.sc.bind(local);
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public InetAddress getInetAddress() {
        if (!isConnected()) {
            return null;
        }
        SocketAddress remote = this.sc.remoteAddress();
        if (remote == null) {
            return null;
        }
        return ((InetSocketAddress) remote).getAddress();
    }

    public InetAddress getLocalAddress() {
        if (this.sc.isOpen()) {
            InetSocketAddress local = this.sc.localAddress();
            if (local != null) {
                return Net.getRevealedLocalAddress(local).getAddress();
            }
        }
        return new InetSocketAddress(0).getAddress();
    }

    public int getPort() {
        if (!isConnected()) {
            return 0;
        }
        SocketAddress remote = this.sc.remoteAddress();
        if (remote == null) {
            return 0;
        }
        return ((InetSocketAddress) remote).getPort();
    }

    public int getLocalPort() {
        SocketAddress local = this.sc.localAddress();
        if (local == null) {
            return -1;
        }
        return ((InetSocketAddress) local).getPort();
    }

    public InputStream getInputStream() throws IOException {
        if (!this.sc.isOpen()) {
            throw new SocketException("Socket is closed");
        } else if (!this.sc.isConnected()) {
            throw new SocketException("Socket is not connected");
        } else if (this.sc.isInputOpen()) {
            if (this.socketInputStream == null) {
                try {
                    this.socketInputStream = (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                        public InputStream run() throws IOException {
                            return new SocketInputStream(SocketAdaptor.this, null);
                        }
                    });
                } catch (PrivilegedActionException e) {
                    throw ((IOException) e.getException());
                }
            }
            return this.socketInputStream;
        } else {
            throw new SocketException("Socket input is shutdown");
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (!this.sc.isOpen()) {
            throw new SocketException("Socket is closed");
        } else if (!this.sc.isConnected()) {
            throw new SocketException("Socket is not connected");
        } else if (this.sc.isOutputOpen()) {
            try {
                return (OutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                    public OutputStream run() throws IOException {
                        return Channels.newOutputStream(SocketAdaptor.this.sc);
                    }
                });
            } catch (PrivilegedActionException e) {
                throw ((IOException) e.getException());
            }
        } else {
            throw new SocketException("Socket output is shutdown");
        }
    }

    private void setBooleanOption(SocketOption<Boolean> name, boolean value) throws SocketException {
        try {
            this.sc.setOption((SocketOption) name, Boolean.valueOf(value));
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    private void setIntOption(SocketOption<Integer> name, int value) throws SocketException {
        try {
            this.sc.setOption((SocketOption) name, Integer.valueOf(value));
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    private boolean getBooleanOption(SocketOption<Boolean> name) throws SocketException {
        try {
            return ((Boolean) this.sc.getOption(name)).booleanValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return false;
        }
    }

    private int getIntOption(SocketOption<Integer> name) throws SocketException {
        try {
            return ((Integer) this.sc.getOption(name)).intValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return -1;
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.TCP_NODELAY, on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return getBooleanOption(StandardSocketOptions.TCP_NODELAY);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (!on) {
            linger = -1;
        }
        setIntOption(StandardSocketOptions.SO_LINGER, linger);
    }

    public int getSoLinger() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_LINGER);
    }

    public void sendUrgentData(int data) throws IOException {
        if (this.sc.sendOutOfBandData((byte) data) == 0) {
            throw new IOException("Socket buffer full");
        }
    }

    public void setOOBInline(boolean on) throws SocketException {
        setBooleanOption(ExtendedSocketOption.SO_OOBINLINE, on);
    }

    public boolean getOOBInline() throws SocketException {
        return getBooleanOption(ExtendedSocketOption.SO_OOBINLINE);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        if (timeout >= 0) {
            this.timeout = timeout;
            return;
        }
        throw new IllegalArgumentException("timeout can't be negative");
    }

    public int getSoTimeout() throws SocketException {
        return this.timeout;
    }

    public void setSendBufferSize(int size) throws SocketException {
        if (size > 0) {
            setIntOption(StandardSocketOptions.SO_SNDBUF, size);
            return;
        }
        throw new IllegalArgumentException("Invalid send size");
    }

    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        if (size > 0) {
            setIntOption(StandardSocketOptions.SO_RCVBUF, size);
            return;
        }
        throw new IllegalArgumentException("Invalid receive size");
    }

    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }

    public void setKeepAlive(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_KEEPALIVE, on);
    }

    public boolean getKeepAlive() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_KEEPALIVE);
    }

    public void setTrafficClass(int tc) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, tc);
    }

    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, on);
    }

    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
    }

    public void close() throws IOException {
        this.sc.close();
    }

    public void shutdownInput() throws IOException {
        try {
            this.sc.shutdownInput();
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public void shutdownOutput() throws IOException {
        try {
            this.sc.shutdownOutput();
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public String toString() {
        if (!this.sc.isConnected()) {
            return "Socket[unconnected]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Socket[addr=");
        stringBuilder.append(getInetAddress());
        stringBuilder.append(",port=");
        stringBuilder.append(getPort());
        stringBuilder.append(",localport=");
        stringBuilder.append(getLocalPort());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public boolean isConnected() {
        return this.sc.isConnected();
    }

    public boolean isBound() {
        return this.sc.localAddress() != null;
    }

    public boolean isClosed() {
        return this.sc.isOpen() ^ 1;
    }

    public boolean isInputShutdown() {
        return this.sc.isInputOpen() ^ 1;
    }

    public boolean isOutputShutdown() {
        return this.sc.isOutputOpen() ^ 1;
    }

    public FileDescriptor getFileDescriptor$() {
        return this.sc.getFD();
    }
}
