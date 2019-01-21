package sun.nio.ch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerSocketAdaptor extends ServerSocket {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private final ServerSocketChannelImpl ssc;
    private volatile int timeout = 0;

    public static ServerSocket create(ServerSocketChannelImpl ssc) {
        try {
            return new ServerSocketAdaptor(ssc);
        } catch (IOException x) {
            throw new Error(x);
        }
    }

    private ServerSocketAdaptor(ServerSocketChannelImpl ssc) throws IOException {
        this.ssc = ssc;
    }

    public void bind(SocketAddress local) throws IOException {
        bind(local, 50);
    }

    public void bind(SocketAddress local, int backlog) throws IOException {
        if (local == null) {
            local = new InetSocketAddress(0);
        }
        try {
            this.ssc.bind(local, backlog);
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public InetAddress getInetAddress() {
        if (this.ssc.isBound()) {
            return Net.getRevealedLocalAddress(this.ssc.localAddress()).getAddress();
        }
        return null;
    }

    public int getLocalPort() {
        if (this.ssc.isBound()) {
            return Net.asInetSocketAddress(this.ssc.localAddress()).getPort();
        }
        return -1;
    }

    public Socket accept() throws IOException {
        synchronized (this.ssc.blockingLock()) {
            if (this.ssc.isBound()) {
                try {
                    Socket socket;
                    if (this.timeout == 0) {
                        SocketChannel sc = this.ssc.accept();
                        if (sc == null) {
                            if (!this.ssc.isBlocking()) {
                                throw new IllegalBlockingModeException();
                            }
                        }
                        socket = sc.socket();
                        return socket;
                    }
                    this.ssc.configureBlocking(false);
                    SocketChannel accept = this.ssc.accept();
                    SocketChannel sc2 = accept;
                    if (accept != null) {
                        socket = sc2.socket();
                        if (this.ssc.isOpen()) {
                            this.ssc.configureBlocking(true);
                        }
                        return socket;
                    }
                    long to = (long) this.timeout;
                    while (this.ssc.isOpen()) {
                        long st = System.currentTimeMillis();
                        if (this.ssc.poll(Net.POLLIN, to) > 0) {
                            SocketChannel accept2 = this.ssc.accept();
                            sc2 = accept2;
                            if (accept2 != null) {
                                Socket socket2 = sc2.socket();
                                if (this.ssc.isOpen()) {
                                    this.ssc.configureBlocking(true);
                                }
                                return socket2;
                            }
                        }
                        to -= System.currentTimeMillis() - st;
                        if (to <= 0) {
                            throw new SocketTimeoutException();
                        }
                    }
                    throw new ClosedChannelException();
                } catch (Exception x) {
                    Net.translateException(x);
                    return null;
                } catch (Throwable th) {
                    if (this.ssc.isOpen()) {
                        this.ssc.configureBlocking(true);
                    }
                }
            }
            throw new IllegalBlockingModeException();
        }
    }

    public void close() throws IOException {
        this.ssc.close();
    }

    public ServerSocketChannel getChannel() {
        return this.ssc;
    }

    public boolean isBound() {
        return this.ssc.isBound();
    }

    public boolean isClosed() {
        return this.ssc.isOpen() ^ 1;
    }

    public void setSoTimeout(int timeout) throws SocketException {
        this.timeout = timeout;
    }

    public int getSoTimeout() throws SocketException {
        return this.timeout;
    }

    public void setReuseAddress(boolean on) throws SocketException {
        try {
            this.ssc.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.valueOf(on));
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    public boolean getReuseAddress() throws SocketException {
        try {
            return ((Boolean) this.ssc.getOption(StandardSocketOptions.SO_REUSEADDR)).booleanValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return false;
        }
    }

    public String toString() {
        if (!isBound()) {
            return "ServerSocket[unbound]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ServerSocket[addr=");
        stringBuilder.append(getInetAddress());
        stringBuilder.append(",localport=");
        stringBuilder.append(getLocalPort());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        if (size > 0) {
            try {
                this.ssc.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(size));
                return;
            } catch (IOException x) {
                Net.translateToSocketException(x);
                return;
            }
        }
        throw new IllegalArgumentException("size cannot be 0 or negative");
    }

    public int getReceiveBufferSize() throws SocketException {
        try {
            return ((Integer) this.ssc.getOption(StandardSocketOptions.SO_RCVBUF)).intValue();
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return -1;
        }
    }
}
