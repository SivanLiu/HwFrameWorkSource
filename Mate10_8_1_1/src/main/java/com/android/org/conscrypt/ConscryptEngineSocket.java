package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

final class ConscryptEngineSocket extends OpenSSLSocketImpl {
    private static final /* synthetic */ int[] -javax-net-ssl-SSLEngineResult$HandshakeStatusSwitchesValues = null;
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private final ConscryptEngine engine;
    private final Object handshakeLock = new Object();
    private SSLInputStream in;
    private SSLOutputStream out;
    private int state = 0;
    private final Object stateLock = new Object();

    private final class SSLInputStream extends InputStream {
        private static final /* synthetic */ int[] -javax-net-ssl-SSLEngineResult$StatusSwitchesValues = null;
        final /* synthetic */ int[] $SWITCH_TABLE$javax$net$ssl$SSLEngineResult$Status;
        private final ByteBuffer fromEngine;
        private ByteBuffer fromSocket;
        private final Object readLock = new Object();
        private final byte[] singleByte = new byte[1];
        private SocketChannel socketChannel;
        private InputStream socketInputStream;

        private static /* synthetic */ int[] -getjavax-net-ssl-SSLEngineResult$StatusSwitchesValues() {
            if (-javax-net-ssl-SSLEngineResult$StatusSwitchesValues != null) {
                return -javax-net-ssl-SSLEngineResult$StatusSwitchesValues;
            }
            int[] iArr = new int[Status.values().length];
            try {
                iArr[Status.BUFFER_OVERFLOW.ordinal()] = 4;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Status.BUFFER_UNDERFLOW.ordinal()] = 1;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Status.CLOSED.ordinal()] = 2;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Status.OK.ordinal()] = 3;
            } catch (NoSuchFieldError e4) {
            }
            -javax-net-ssl-SSLEngineResult$StatusSwitchesValues = iArr;
            return iArr;
        }

        SSLInputStream() {
            this.fromEngine = ByteBuffer.allocateDirect(ConscryptEngineSocket.this.engine.getSession().getApplicationBufferSize());
            this.fromEngine.flip();
        }

        public void close() throws IOException {
            ConscryptEngineSocket.this.close();
        }

        public int read() throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                int count = read(this.singleByte, 0, 1);
                if (count == -1) {
                    return -1;
                } else if (count != 1) {
                    throw new SSLException("read incorrect number of bytes " + count);
                } else {
                    byte b = this.singleByte[0];
                    return b;
                }
            }
        }

        public int read(byte[] b) throws IOException {
            int read;
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                read = read(b, 0, b.length);
            }
            return read;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int readInternal;
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                readInternal = readInternal(b, off, len);
            }
            return readInternal;
        }

        public int available() throws IOException {
            int i = 0;
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                init();
                int remaining = this.fromEngine.remaining();
                if (this.fromSocket.hasRemaining() || this.socketInputStream.available() > 0) {
                    i = 1;
                }
                i += remaining;
            }
            return i;
        }

        private int readInternal(byte[] b, int off, int len) throws IOException {
            Platform.blockGuardOnNetwork();
            ConscryptEngineSocket.this.checkOpen();
            init();
            while (this.fromEngine.remaining() <= 0) {
                boolean needMoreDataFromSocket = true;
                this.fromSocket.flip();
                this.fromEngine.clear();
                SSLEngineResult engineResult = ConscryptEngineSocket.this.engine.unwrap(this.fromSocket, this.fromEngine);
                this.fromSocket.compact();
                this.fromEngine.flip();
                switch (-getjavax-net-ssl-SSLEngineResult$StatusSwitchesValues()[engineResult.getStatus().ordinal()]) {
                    case 1:
                        if (engineResult.bytesProduced() != 0) {
                            needMoreDataFromSocket = false;
                            break;
                        }
                        break;
                    case 2:
                        return -1;
                    case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                        needMoreDataFromSocket = false;
                        break;
                    default:
                        throw new SSLException("Unexpected engine result " + engineResult.getStatus());
                }
                if (!needMoreDataFromSocket && engineResult.bytesProduced() == 0) {
                    return 0;
                }
                if (needMoreDataFromSocket && readFromSocket() == -1) {
                    return -1;
                }
            }
            int readFromEngine = Math.min(this.fromEngine.remaining(), len);
            this.fromEngine.get(b, off, readFromEngine);
            return readFromEngine;
        }

        private void init() throws IOException {
            if (this.socketInputStream == null) {
                this.socketInputStream = ConscryptEngineSocket.this.getUnderlyingInputStream();
                this.socketChannel = ConscryptEngineSocket.this.getUnderlyingChannel();
                if (this.socketChannel != null) {
                    this.fromSocket = ByteBuffer.allocateDirect(ConscryptEngineSocket.this.engine.getSession().getPacketBufferSize());
                } else {
                    this.fromSocket = ByteBuffer.allocate(ConscryptEngineSocket.this.engine.getSession().getPacketBufferSize());
                }
            }
        }

        private int readFromSocket() throws IOException {
            if (this.socketChannel != null) {
                return this.socketChannel.read(this.fromSocket);
            }
            int read = this.socketInputStream.read(this.fromSocket.array(), this.fromSocket.position(), this.fromSocket.remaining());
            if (read > 0) {
                this.fromSocket.position(this.fromSocket.position() + read);
            }
            return read;
        }
    }

    private final class SSLOutputStream extends OutputStream {
        private SocketChannel socketChannel;
        private OutputStream socketOutputStream;
        private ByteBuffer target;
        private final Object writeLock = new Object();

        SSLOutputStream() {
        }

        public void close() throws IOException {
            ConscryptEngineSocket.this.close();
        }

        public void write(int b) throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                write(new byte[]{(byte) b});
            }
        }

        public void write(byte[] b) throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                writeInternal(ByteBuffer.wrap(b));
            }
        }

        public void write(byte[] b, int off, int len) throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                writeInternal(ByteBuffer.wrap(b, off, len));
            }
        }

        private void writeInternal(ByteBuffer buffer) throws IOException {
            Platform.blockGuardOnNetwork();
            ConscryptEngineSocket.this.checkOpen();
            init();
            int len = buffer.remaining();
            do {
                this.target.clear();
                SSLEngineResult engineResult = ConscryptEngineSocket.this.engine.wrap(buffer, this.target);
                if (engineResult.getStatus() != Status.OK) {
                    throw new SSLException("Unexpected engine result " + engineResult.getStatus());
                } else if (this.target.position() != engineResult.bytesProduced()) {
                    throw new SSLException("Engine bytesProduced " + engineResult.bytesProduced() + " does not match bytes written " + this.target.position());
                } else {
                    len -= engineResult.bytesConsumed();
                    if (len != buffer.remaining()) {
                        throw new SSLException("Engine did not read the correct number of bytes");
                    }
                    this.target.flip();
                    writeToSocket();
                }
            } while (len > 0);
        }

        public void flush() throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                flushInternal();
            }
        }

        private void flushInternal() throws IOException {
            ConscryptEngineSocket.this.checkOpen();
            init();
            this.socketOutputStream.flush();
        }

        private void init() throws IOException {
            if (this.socketOutputStream == null) {
                this.socketOutputStream = ConscryptEngineSocket.this.getUnderlyingOutputStream();
                this.socketChannel = ConscryptEngineSocket.this.getUnderlyingChannel();
                if (this.socketChannel != null) {
                    this.target = ByteBuffer.allocateDirect(ConscryptEngineSocket.this.engine.getSession().getPacketBufferSize());
                } else {
                    this.target = ByteBuffer.allocate(ConscryptEngineSocket.this.engine.getSession().getPacketBufferSize());
                }
            }
        }

        private void writeToSocket() throws IOException {
            if (this.socketChannel != null) {
                while (this.target.hasRemaining()) {
                    this.socketChannel.write(this.target);
                }
                return;
            }
            this.socketOutputStream.write(this.target.array(), 0, this.target.limit());
        }
    }

    private static /* synthetic */ int[] -getjavax-net-ssl-SSLEngineResult$HandshakeStatusSwitchesValues() {
        if (-javax-net-ssl-SSLEngineResult$HandshakeStatusSwitchesValues != null) {
            return -javax-net-ssl-SSLEngineResult$HandshakeStatusSwitchesValues;
        }
        int[] iArr = new int[HandshakeStatus.values().length];
        try {
            iArr[HandshakeStatus.FINISHED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[HandshakeStatus.NEED_TASK.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[HandshakeStatus.NEED_UNWRAP.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[HandshakeStatus.NEED_WRAP.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[HandshakeStatus.NOT_HANDSHAKING.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        -javax-net-ssl-SSLEngineResult$HandshakeStatusSwitchesValues = iArr;
        return iArr;
    }

    ConscryptEngineSocket(SSLParametersImpl sslParameters) throws IOException {
        this.engine = newEngine(sslParameters, this);
    }

    ConscryptEngineSocket(String hostname, int port, SSLParametersImpl sslParameters) throws IOException {
        super(hostname, port);
        this.engine = newEngine(sslParameters, this);
    }

    ConscryptEngineSocket(InetAddress address, int port, SSLParametersImpl sslParameters) throws IOException {
        super(address, port);
        this.engine = newEngine(sslParameters, this);
    }

    ConscryptEngineSocket(String hostname, int port, InetAddress clientAddress, int clientPort, SSLParametersImpl sslParameters) throws IOException {
        super(hostname, port, clientAddress, clientPort);
        this.engine = newEngine(sslParameters, this);
    }

    ConscryptEngineSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort, SSLParametersImpl sslParameters) throws IOException {
        super(address, port, clientAddress, clientPort);
        this.engine = newEngine(sslParameters, this);
    }

    ConscryptEngineSocket(Socket socket, String hostname, int port, boolean autoClose, SSLParametersImpl sslParameters) throws IOException {
        super(socket, hostname, port, autoClose);
        this.engine = newEngine(sslParameters, this);
    }

    private static ConscryptEngine newEngine(SSLParametersImpl sslParameters, final ConscryptEngineSocket socket) {
        ConscryptEngine engine = new ConscryptEngine(sslParameters, socket.peerInfoProvider());
        engine.setHandshakeListener(new HandshakeListener() {
            public void onHandshakeFinished() {
                socket.onHandshakeFinished();
            }
        });
        engine.setUseClientMode(sslParameters.getUseClientMode());
        return engine;
    }

    public SSLParameters getSSLParameters() {
        return this.engine.getSSLParameters();
    }

    public void setSSLParameters(SSLParameters sslParameters) {
        this.engine.setSSLParameters(sslParameters);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startHandshake() throws IOException {
        checkOpen();
        if (!isHandshakeFinished()) {
            try {
                synchronized (this.handshakeLock) {
                    synchronized (this.stateLock) {
                        if (this.state == 0) {
                            this.state = 2;
                            this.engine.beginHandshake();
                            this.in = new SSLInputStream();
                            this.out = new SSLOutputStream();
                        }
                    }
                }
            } catch (SSLException e) {
                close();
                throw e;
            } catch (IOException e2) {
                close();
                throw e2;
            } catch (Exception e3) {
                close();
                throw SSLUtils.toSSLHandshakeException(e3);
            }
        }
    }

    public InputStream getInputStream() throws IOException {
        checkOpen();
        waitForHandshake();
        return this.in;
    }

    public OutputStream getOutputStream() throws IOException {
        checkOpen();
        waitForHandshake();
        return this.out;
    }

    public SSLSession getHandshakeSession() {
        return this.engine.handshakeSession();
    }

    public SSLSession getSession() {
        SSLSession session = this.engine.getSession();
        if (SSLNullSession.isNullSession(session)) {
            boolean handshakeCompleted = false;
            try {
                if (isConnected()) {
                    waitForHandshake();
                    handshakeCompleted = true;
                }
            } catch (IOException e) {
            }
            if (!handshakeCompleted) {
                return session;
            }
            session = this.engine.getSession();
        }
        return session;
    }

    SSLSession getActiveSession() {
        return this.engine.getSession();
    }

    public boolean getEnableSessionCreation() {
        return this.engine.getEnableSessionCreation();
    }

    public void setEnableSessionCreation(boolean flag) {
        this.engine.setEnableSessionCreation(flag);
    }

    public String[] getSupportedCipherSuites() {
        return this.engine.getSupportedCipherSuites();
    }

    public String[] getEnabledCipherSuites() {
        return this.engine.getEnabledCipherSuites();
    }

    public void setEnabledCipherSuites(String[] suites) {
        this.engine.setEnabledCipherSuites(suites);
    }

    public String[] getSupportedProtocols() {
        return this.engine.getSupportedProtocols();
    }

    public String[] getEnabledProtocols() {
        return this.engine.getEnabledProtocols();
    }

    public void setEnabledProtocols(String[] protocols) {
        this.engine.setEnabledProtocols(protocols);
    }

    public void setHostname(String hostname) {
        this.engine.setHostname(hostname);
        super.setHostname(hostname);
    }

    public void setUseSessionTickets(boolean useSessionTickets) {
        this.engine.setUseSessionTickets(useSessionTickets);
    }

    public void setChannelIdEnabled(boolean enabled) {
        this.engine.setChannelIdEnabled(enabled);
    }

    public byte[] getChannelId() throws SSLException {
        return this.engine.getChannelId();
    }

    public void setChannelIdPrivateKey(PrivateKey privateKey) {
        this.engine.setChannelIdPrivateKey(privateKey);
    }

    public boolean getUseClientMode() {
        return this.engine.getUseClientMode();
    }

    public void setUseClientMode(boolean mode) {
        this.engine.setUseClientMode(mode);
    }

    public boolean getWantClientAuth() {
        return this.engine.getWantClientAuth();
    }

    public boolean getNeedClientAuth() {
        return this.engine.getNeedClientAuth();
    }

    public void setNeedClientAuth(boolean need) {
        this.engine.setNeedClientAuth(need);
    }

    public void setWantClientAuth(boolean want) {
        this.engine.setWantClientAuth(want);
    }

    public void close() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 8) {
                return;
            }
            this.state = 8;
            this.stateLock.notifyAll();
            super.close();
            this.engine.closeInbound();
            this.engine.closeOutbound();
        }
    }

    public byte[] getAlpnSelectedProtocol() {
        return this.engine.getAlpnSelectedProtocol();
    }

    public void setAlpnProtocols(byte[] alpnProtocols) {
        this.engine.setAlpnProtocols(alpnProtocols);
    }

    public void setAlpnProtocols(String[] alpnProtocols) {
        this.engine.setAlpnProtocols(alpnProtocols);
    }

    private boolean isHandshakeFinished() {
        return this.state >= 4;
    }

    private void onHandshakeFinished() {
        boolean notify = false;
        synchronized (this.stateLock) {
            if (this.state != 8) {
                if (this.state == 2) {
                    this.state = 4;
                } else if (this.state == 3) {
                    this.state = 5;
                }
                this.stateLock.notifyAll();
                notify = true;
            }
        }
        if (notify) {
            notifyHandshakeCompletedListeners();
        }
    }

    private void waitForHandshake() throws IOException {
        startHandshake();
        synchronized (this.stateLock) {
            while (this.state != 5 && this.state != 4 && this.state != 8) {
                try {
                    this.stateLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for handshake", e);
                }
            }
            if (this.state == 8) {
                throw new SocketException("Socket is closed");
            }
        }
    }

    private OutputStream getUnderlyingOutputStream() throws IOException {
        return super.getOutputStream();
    }

    private InputStream getUnderlyingInputStream() throws IOException {
        return super.getInputStream();
    }

    private SocketChannel getUnderlyingChannel() throws IOException {
        return super.getChannel();
    }
}
