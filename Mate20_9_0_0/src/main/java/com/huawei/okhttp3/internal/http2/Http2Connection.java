package com.huawei.okhttp3.internal.http2;

import com.huawei.okhttp3.Protocol;
import com.huawei.okhttp3.internal.NamedRunnable;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.platform.Platform;
import com.huawei.okio.Buffer;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.BufferedSource;
import com.huawei.okio.ByteString;
import com.huawei.okio.Okio;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Http2Connection implements Closeable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int OKHTTP_CLIENT_WINDOW_SIZE = 16777216;
    static final ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp FramedConnection", true));
    long bytesLeftInWriteWindow;
    final boolean client;
    final Set<Integer> currentPushRequests = new LinkedHashSet();
    final String hostname;
    int lastGoodStreamId;
    final Listener listener;
    int maxStreamId = 0;
    private int nextPingId;
    int nextStreamId;
    Settings okHttpSettings = new Settings();
    final Settings peerSettings = new Settings();
    private Map<Integer, Ping> pings;
    private final ExecutorService pushExecutor;
    final PushObserver pushObserver;
    final ReaderRunnable readerRunnable;
    boolean receivedInitialPeerSettings = false;
    int remainStreams = 0;
    boolean shutdown;
    final Socket socket;
    final Map<Integer, Http2Stream> streams = new LinkedHashMap();
    long unacknowledgedBytesRead = 0;
    final Http2Writer writer;

    public static class Builder {
        boolean client;
        String hostname;
        Listener listener = Listener.REFUSE_INCOMING_STREAMS;
        PushObserver pushObserver = PushObserver.CANCEL;
        BufferedSink sink;
        Socket socket;
        BufferedSource source;

        public Builder(boolean client) {
            this.client = client;
        }

        public Builder socket(Socket socket) throws IOException {
            return socket(socket, ((InetSocketAddress) socket.getRemoteSocketAddress()).getHostName(), Okio.buffer(Okio.source(socket)), Okio.buffer(Okio.sink(socket)));
        }

        public Builder socket(Socket socket, String hostname, BufferedSource source, BufferedSink sink) {
            this.socket = socket;
            this.hostname = hostname;
            this.source = source;
            this.sink = sink;
            return this;
        }

        public Builder listener(Listener listener) {
            this.listener = listener;
            return this;
        }

        public Builder pushObserver(PushObserver pushObserver) {
            this.pushObserver = pushObserver;
            return this;
        }

        public Http2Connection build() throws IOException {
            return new Http2Connection(this);
        }
    }

    public static abstract class Listener {
        public static final Listener REFUSE_INCOMING_STREAMS = new Listener() {
            public void onStream(Http2Stream stream) throws IOException {
                stream.close(ErrorCode.REFUSED_STREAM);
            }
        };

        public abstract void onStream(Http2Stream http2Stream) throws IOException;

        public void onSettings(Http2Connection connection) {
        }
    }

    class ReaderRunnable extends NamedRunnable implements Handler {
        final Http2Reader reader;

        ReaderRunnable(Http2Reader reader) {
            super("OkHttp %s", this$0.hostname);
            this.reader = reader;
        }

        protected void execute() {
            ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
            ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;
            try {
                if (!Http2Connection.this.client) {
                    this.reader.readConnectionPreface();
                }
                while (this.reader.nextFrame(this)) {
                }
                connectionErrorCode = ErrorCode.NO_ERROR;
                streamErrorCode = ErrorCode.CANCEL;
            } catch (IOException e) {
                connectionErrorCode = ErrorCode.PROTOCOL_ERROR;
                streamErrorCode = ErrorCode.PROTOCOL_ERROR;
            } finally {
                try {
                    Http2Connection.this.close(connectionErrorCode, streamErrorCode);
                } catch (IOException e2) {
                }
                Util.closeQuietly(this.reader);
            }
        }

        public void data(boolean inFinished, int streamId, BufferedSource source, int length) throws IOException {
            if (Http2Connection.this.pushedStream(streamId)) {
                Http2Connection.this.pushDataLater(streamId, source, length, inFinished);
                return;
            }
            Http2Stream dataStream = Http2Connection.this.getStream(streamId);
            if (dataStream == null) {
                Http2Connection.this.writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
                source.skip((long) length);
                return;
            }
            dataStream.receiveData(source, length);
            if (inFinished) {
                dataStream.receiveFin();
                boolean closeFlag = false;
                synchronized (Http2Connection.this) {
                    if (Http2Connection.this.maxStreamId > 0) {
                        if (streamId <= Http2Connection.this.maxStreamId) {
                            Http2Connection http2Connection = Http2Connection.this;
                            http2Connection.remainStreams--;
                        }
                        if (Http2Connection.this.remainStreams <= 0) {
                            closeFlag = true;
                        }
                    }
                }
                if (closeFlag) {
                    Http2Connection.this.close();
                }
            }
        }

        /* JADX WARNING: Missing block: B:25:0x006f, code skipped:
            r1.receiveHeaders(r15);
     */
        /* JADX WARNING: Missing block: B:26:0x0072, code skipped:
            if (r12 == false) goto L_0x0077;
     */
        /* JADX WARNING: Missing block: B:27:0x0074, code skipped:
            r1.receiveFin();
     */
        /* JADX WARNING: Missing block: B:28:0x0077, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void headers(boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock) {
            if (Http2Connection.this.pushedStream(streamId)) {
                Http2Connection.this.pushHeadersLater(streamId, headerBlock, inFinished);
                return;
            }
            synchronized (Http2Connection.this) {
                if (Http2Connection.this.shutdown) {
                    return;
                }
                Http2Stream stream = Http2Connection.this.getStream(streamId);
                if (stream == null) {
                    if (streamId <= Http2Connection.this.lastGoodStreamId) {
                    } else if (streamId % 2 == Http2Connection.this.nextStreamId % 2) {
                    } else {
                        final Http2Stream http2Stream = new Http2Stream(streamId, Http2Connection.this, false, inFinished, headerBlock);
                        Http2Connection.this.lastGoodStreamId = streamId;
                        Http2Connection.this.streams.put(Integer.valueOf(streamId), http2Stream);
                        Http2Connection.executor.execute(new NamedRunnable("OkHttp %s stream %d", new Object[]{Http2Connection.this.hostname, Integer.valueOf(streamId)}) {
                            public void execute() {
                                try {
                                    Http2Connection.this.listener.onStream(http2Stream);
                                } catch (IOException e) {
                                    Platform platform = Platform.get();
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("FramedConnection.Listener failure for ");
                                    stringBuilder.append(Http2Connection.this.hostname);
                                    platform.log(4, stringBuilder.toString(), e);
                                    try {
                                        http2Stream.close(ErrorCode.PROTOCOL_ERROR);
                                    } catch (IOException e2) {
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }

        public void rstStream(int streamId, ErrorCode errorCode) {
            if (Http2Connection.this.pushedStream(streamId)) {
                Http2Connection.this.pushResetLater(streamId, errorCode);
                return;
            }
            Http2Stream rstStream = Http2Connection.this.removeStream(streamId);
            if (rstStream != null) {
                rstStream.receiveRstStream(errorCode);
            }
        }

        public void settings(boolean clearPrevious, Settings newSettings) {
            int i;
            long delta = 0;
            Http2Stream[] streamsToNotify = null;
            synchronized (Http2Connection.this) {
                int priorWriteWindowSize = Http2Connection.this.peerSettings.getInitialWindowSize();
                if (clearPrevious) {
                    Http2Connection.this.peerSettings.clear();
                }
                Http2Connection.this.peerSettings.merge(newSettings);
                applyAndAckSettings(newSettings);
                int peerInitialWindowSize = Http2Connection.this.peerSettings.getInitialWindowSize();
                if (!(peerInitialWindowSize == -1 || peerInitialWindowSize == priorWriteWindowSize)) {
                    delta = (long) (peerInitialWindowSize - priorWriteWindowSize);
                    if (!Http2Connection.this.receivedInitialPeerSettings) {
                        Http2Connection.this.addBytesToWriteWindow(delta);
                        Http2Connection.this.receivedInitialPeerSettings = true;
                    }
                    if (!Http2Connection.this.streams.isEmpty()) {
                        streamsToNotify = (Http2Stream[]) Http2Connection.this.streams.values().toArray(new Http2Stream[Http2Connection.this.streams.size()]);
                    }
                }
                ExecutorService executorService = Http2Connection.executor;
                Object[] objArr = new Object[1];
                i = 0;
                objArr[0] = Http2Connection.this.hostname;
                executorService.execute(new NamedRunnable("OkHttp %s settings", objArr) {
                    public void execute() {
                        Http2Connection.this.listener.onSettings(Http2Connection.this);
                    }
                });
            }
            if (streamsToNotify != null && delta != 0) {
                int length = streamsToNotify.length;
                while (i < length) {
                    Http2Stream stream = streamsToNotify[i];
                    synchronized (stream) {
                        stream.addBytesToWriteWindow(delta);
                    }
                    i++;
                }
            }
        }

        private void applyAndAckSettings(final Settings peerSettings) {
            Http2Connection.executor.execute(new NamedRunnable("OkHttp %s ACK Settings", new Object[]{Http2Connection.this.hostname}) {
                public void execute() {
                    try {
                        Http2Connection.this.writer.applyAndAckSettings(peerSettings);
                    } catch (IOException e) {
                    }
                }
            });
        }

        public void ackSettings() {
        }

        public void ping(boolean reply, int payload1, int payload2) {
            if (reply) {
                Ping ping = Http2Connection.this.removePing(payload1);
                if (ping != null) {
                    ping.receive();
                    return;
                }
                return;
            }
            Http2Connection.this.writePingLater(true, payload1, payload2, null);
        }

        public void goAway(int lastGoodStreamId, ErrorCode errorCode, ByteString debugData) {
            Http2Stream[] streamsCopy;
            int i;
            debugData.size();
            synchronized (Http2Connection.this) {
                streamsCopy = (Http2Stream[]) Http2Connection.this.streams.values().toArray(new Http2Stream[Http2Connection.this.streams.size()]);
                i = 0;
                Http2Connection.this.remainStreams = 0;
                for (Http2Stream http2Stream : streamsCopy) {
                    if (http2Stream.getId() <= lastGoodStreamId) {
                        Http2Connection http2Connection = Http2Connection.this;
                        http2Connection.remainStreams++;
                    }
                }
                if (Http2Connection.this.remainStreams <= 0) {
                    Http2Connection.this.shutdown = true;
                }
                Http2Connection.this.maxStreamId = lastGoodStreamId;
            }
            int length = streamsCopy.length;
            while (i < length) {
                Http2Stream http2Stream2 = streamsCopy[i];
                if (http2Stream2.getId() > lastGoodStreamId && http2Stream2.isLocallyInitiated()) {
                    http2Stream2.receiveRstStream(ErrorCode.REFUSED_STREAM);
                    Http2Connection.this.removeStream(http2Stream2.getId());
                }
                i++;
            }
        }

        public void windowUpdate(int streamId, long windowSizeIncrement) {
            if (streamId == 0) {
                synchronized (Http2Connection.this) {
                    Http2Connection http2Connection = Http2Connection.this;
                    http2Connection.bytesLeftInWriteWindow += windowSizeIncrement;
                    Http2Connection.this.notifyAll();
                }
                return;
            }
            Http2Stream stream = Http2Connection.this.getStream(streamId);
            if (stream != null) {
                synchronized (stream) {
                    stream.addBytesToWriteWindow(windowSizeIncrement);
                }
            }
        }

        public void priority(int streamId, int streamDependency, int weight, boolean exclusive) {
        }

        public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) {
            Http2Connection.this.pushRequestLater(promisedStreamId, requestHeaders);
        }

        public void alternateService(int streamId, String origin, ByteString protocol, String host, int port, long maxAge) {
        }
    }

    Http2Connection(Builder builder) {
        this.pushObserver = builder.pushObserver;
        this.client = builder.client;
        this.listener = builder.listener;
        int i = 2;
        this.nextStreamId = builder.client ? 1 : 2;
        if (builder.client) {
            this.nextStreamId += 2;
        }
        if (builder.client) {
            i = 1;
        }
        this.nextPingId = i;
        if (builder.client) {
            this.okHttpSettings.set(7, 16777216);
        }
        this.hostname = builder.hostname;
        this.pushExecutor = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory(Util.format("OkHttp %s Push Observer", this.hostname), true));
        this.peerSettings.set(7, 65535);
        this.peerSettings.set(5, 16384);
        this.bytesLeftInWriteWindow = (long) this.peerSettings.getInitialWindowSize();
        this.socket = builder.socket;
        this.writer = new Http2Writer(builder.sink, this.client);
        this.readerRunnable = new ReaderRunnable(new Http2Reader(builder.source, this.client));
        try {
            this.socket.setTcpNoDelay(true);
        } catch (SocketException e) {
        }
    }

    public Protocol getProtocol() {
        return Protocol.HTTP_2;
    }

    public synchronized int openStreamCount() {
        return this.streams.size();
    }

    synchronized Http2Stream getStream(int id) {
        return (Http2Stream) this.streams.get(Integer.valueOf(id));
    }

    synchronized Http2Stream removeStream(int streamId) {
        Http2Stream stream;
        stream = (Http2Stream) this.streams.remove(Integer.valueOf(streamId));
        notifyAll();
        return stream;
    }

    public synchronized int maxConcurrentStreams() {
        return this.peerSettings.getMaxConcurrentStreams(Integer.MAX_VALUE);
    }

    public Http2Stream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
        if (!this.client) {
            return newStream(associatedStreamId, requestHeaders, out);
        }
        throw new IllegalStateException("Client cannot push requests.");
    }

    public Http2Stream newStream(List<Header> requestHeaders, boolean out) throws IOException {
        return newStream(0, requestHeaders, out);
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x003a A:{Catch:{ IOException -> 0x004d }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Http2Stream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
        Http2Stream stream;
        boolean flushHeaders;
        boolean outFinished = out ^ 1;
        synchronized (this.writer) {
            int streamId;
            synchronized (this) {
                if (this.shutdown) {
                    throw new ConnectionShutdownException();
                }
                streamId = this.nextStreamId;
                this.nextStreamId += 2;
                stream = new Http2Stream(streamId, this, outFinished, false, requestHeaders);
                if (out && this.bytesLeftInWriteWindow != 0) {
                    if (stream.bytesLeftInWriteWindow != 0) {
                        flushHeaders = false;
                        if (stream.isOpen()) {
                            this.streams.put(Integer.valueOf(streamId), stream);
                        }
                    }
                }
                flushHeaders = true;
                if (stream.isOpen()) {
                }
            }
            int streamId2 = streamId;
            if (associatedStreamId == 0) {
                try {
                    this.writer.synStream(outFinished, streamId2, associatedStreamId, requestHeaders);
                } catch (IOException e) {
                    removeStream(stream.getId());
                    throw e;
                }
            } else if (this.client) {
                throw new IllegalArgumentException("client streams shouldn't have associated stream IDs");
            } else {
                this.writer.pushPromise(associatedStreamId, streamId2, requestHeaders);
            }
        }
        if (flushHeaders) {
            this.writer.flush();
        }
        return stream;
    }

    void writeSynReply(int streamId, boolean outFinished, List<Header> alternating) throws IOException {
        this.writer.synReply(outFinished, streamId, alternating);
    }

    public void writeData(int streamId, boolean outFinished, Buffer buffer, long byteCount) throws IOException {
        if (byteCount == 0) {
            this.writer.data(outFinished, streamId, buffer, 0);
            return;
        }
        while (byteCount > 0) {
            int toWrite;
            synchronized (this) {
                while (this.bytesLeftInWriteWindow <= 0) {
                    try {
                        if (this.streams.containsKey(Integer.valueOf(streamId))) {
                            wait();
                        } else {
                            throw new IOException("stream closed");
                        }
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException();
                    }
                }
                toWrite = Math.min((int) Math.min(byteCount, this.bytesLeftInWriteWindow), this.writer.maxDataLength());
                this.bytesLeftInWriteWindow -= (long) toWrite;
            }
            byteCount -= (long) toWrite;
            Http2Writer http2Writer = this.writer;
            boolean z = outFinished && byteCount == 0;
            http2Writer.data(z, streamId, buffer, toWrite);
        }
    }

    void addBytesToWriteWindow(long delta) {
        this.bytesLeftInWriteWindow += delta;
        if (delta > 0) {
            notifyAll();
        }
    }

    void writeSynResetLater(int streamId, ErrorCode errorCode) {
        final int i = streamId;
        final ErrorCode errorCode2 = errorCode;
        executor.execute(new NamedRunnable("OkHttp %s stream %d", new Object[]{this.hostname, Integer.valueOf(streamId)}) {
            public void execute() {
                try {
                    Http2Connection.this.writeSynReset(i, errorCode2);
                } catch (IOException e) {
                }
            }
        });
    }

    void writeSynReset(int streamId, ErrorCode statusCode) throws IOException {
        this.writer.rstStream(streamId, statusCode);
    }

    void writeWindowUpdateLater(int streamId, long unacknowledgedBytesRead) {
        final int i = streamId;
        final long j = unacknowledgedBytesRead;
        executor.execute(new NamedRunnable("OkHttp Window Update %s stream %d", new Object[]{this.hostname, Integer.valueOf(streamId)}) {
            public void execute() {
                try {
                    Http2Connection.this.writer.windowUpdate(i, j);
                } catch (IOException e) {
                }
            }
        });
    }

    public Ping ping() throws IOException {
        int pingId;
        Ping ping = new Ping();
        synchronized (this) {
            if (this.shutdown) {
                throw new ConnectionShutdownException();
            }
            pingId = this.nextPingId;
            this.nextPingId += 2;
            if (this.pings == null) {
                this.pings = new LinkedHashMap();
            }
            this.pings.put(Integer.valueOf(pingId), ping);
        }
        writePing(false, pingId, 1330343787, ping);
        return ping;
    }

    void writePingLater(boolean reply, int payload1, int payload2, Ping ping) {
        final boolean z = reply;
        final int i = payload1;
        final int i2 = payload2;
        final Ping ping2 = ping;
        executor.execute(new NamedRunnable("OkHttp %s ping %08x%08x", new Object[]{this.hostname, Integer.valueOf(payload1), Integer.valueOf(payload2)}) {
            public void execute() {
                try {
                    Http2Connection.this.writePing(z, i, i2, ping2);
                } catch (IOException e) {
                }
            }
        });
    }

    void writePing(boolean reply, int payload1, int payload2, Ping ping) throws IOException {
        synchronized (this.writer) {
            if (ping != null) {
                try {
                    ping.send();
                } catch (Throwable th) {
                }
            }
            this.writer.ping(reply, payload1, payload2);
        }
    }

    synchronized Ping removePing(int id) {
        return this.pings != null ? (Ping) this.pings.remove(Integer.valueOf(id)) : null;
    }

    public void flush() throws IOException {
        this.writer.flush();
    }

    public void shutdown(ErrorCode statusCode) throws IOException {
        synchronized (this.writer) {
            synchronized (this) {
                if (this.shutdown) {
                    return;
                }
                this.shutdown = true;
                int lastGoodStreamId = this.lastGoodStreamId;
                this.writer.goAway(lastGoodStreamId, statusCode, Util.EMPTY_BYTE_ARRAY);
            }
        }
    }

    public void close() throws IOException {
        close(ErrorCode.NO_ERROR, ErrorCode.CANCEL);
    }

    void close(ErrorCode connectionCode, ErrorCode streamCode) throws IOException {
        int length;
        IOException thrown = null;
        try {
            shutdown(connectionCode);
        } catch (IOException e) {
            thrown = e;
        }
        Http2Stream[] streamsToClose = null;
        Ping[] pingsToCancel = null;
        synchronized (this) {
            if (!this.streams.isEmpty()) {
                streamsToClose = (Http2Stream[]) this.streams.values().toArray(new Http2Stream[this.streams.size()]);
                this.streams.clear();
            }
            if (this.pings != null) {
                pingsToCancel = (Ping[]) this.pings.values().toArray(new Ping[this.pings.size()]);
                this.pings = null;
            }
        }
        int i = 0;
        if (streamsToClose != null) {
            IOException thrown2 = thrown;
            for (Http2Stream stream : streamsToClose) {
                try {
                    stream.close(streamCode);
                } catch (IOException e2) {
                    if (thrown2 != null) {
                        thrown2 = e2;
                    }
                }
            }
            thrown = thrown2;
        }
        if (pingsToCancel != null) {
            length = pingsToCancel.length;
            while (i < length) {
                pingsToCancel[i].cancel();
                i++;
            }
        }
        try {
            this.writer.close();
        } catch (IOException e3) {
            if (thrown == null) {
                thrown = e3;
            }
        }
        try {
            this.socket.close();
        } catch (IOException e32) {
            thrown = e32;
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    public void start() throws IOException {
        start(true);
    }

    void start(boolean sendConnectionPreface) throws IOException {
        if (sendConnectionPreface) {
            this.writer.connectionPreface();
            this.writer.settings(this.okHttpSettings);
            int windowSize = this.okHttpSettings.getInitialWindowSize();
            if (windowSize != 65535) {
                this.writer.windowUpdate(0, (long) (windowSize - 65535));
            }
        }
        new Thread(this.readerRunnable).start();
    }

    public void setSettings(Settings settings) throws IOException {
        synchronized (this.writer) {
            synchronized (this) {
                if (this.shutdown) {
                    throw new ConnectionShutdownException();
                }
                this.okHttpSettings.merge(settings);
                this.writer.settings(settings);
            }
        }
    }

    public synchronized boolean isShutdown() {
        return this.shutdown;
    }

    boolean pushedStream(int streamId) {
        return streamId != 0 && (streamId & 1) == 0;
    }

    void pushRequestLater(int streamId, List<Header> requestHeaders) {
        synchronized (this) {
            if (this.currentPushRequests.contains(Integer.valueOf(streamId))) {
                writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
                return;
            }
            this.currentPushRequests.add(Integer.valueOf(streamId));
            final int i = streamId;
            final List<Header> list = requestHeaders;
            this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Request[%s]", new Object[]{this.hostname, Integer.valueOf(streamId)}) {
                public void execute() {
                    if (Http2Connection.this.pushObserver.onRequest(i, list)) {
                        try {
                            Http2Connection.this.writer.rstStream(i, ErrorCode.CANCEL);
                            synchronized (Http2Connection.this) {
                                Http2Connection.this.currentPushRequests.remove(Integer.valueOf(i));
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });
        }
    }

    void pushHeadersLater(int streamId, List<Header> requestHeaders, boolean inFinished) {
        final int i = streamId;
        final List<Header> list = requestHeaders;
        final boolean z = inFinished;
        this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Headers[%s]", new Object[]{this.hostname, Integer.valueOf(streamId)}) {
            public void execute() {
                boolean cancel = Http2Connection.this.pushObserver.onHeaders(i, list, z);
                if (cancel) {
                    try {
                        Http2Connection.this.writer.rstStream(i, ErrorCode.CANCEL);
                    } catch (IOException e) {
                        return;
                    }
                }
                if (cancel || z) {
                    synchronized (Http2Connection.this) {
                        Http2Connection.this.currentPushRequests.remove(Integer.valueOf(i));
                    }
                }
            }
        });
    }

    void pushDataLater(int streamId, BufferedSource source, int byteCount, boolean inFinished) throws IOException {
        Buffer buffer = new Buffer();
        source.require((long) byteCount);
        source.read(buffer, (long) byteCount);
        if (buffer.size() == ((long) byteCount)) {
            final int i = streamId;
            final Buffer buffer2 = buffer;
            final int i2 = byteCount;
            final boolean z = inFinished;
            this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Data[%s]", new Object[]{this.hostname, Integer.valueOf(streamId)}) {
                public void execute() {
                    try {
                        boolean cancel = Http2Connection.this.pushObserver.onData(i, buffer2, i2, z);
                        if (cancel) {
                            Http2Connection.this.writer.rstStream(i, ErrorCode.CANCEL);
                        }
                        if (cancel || z) {
                            synchronized (Http2Connection.this) {
                                Http2Connection.this.currentPushRequests.remove(Integer.valueOf(i));
                            }
                        }
                    } catch (IOException e) {
                    }
                }
            });
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(buffer.size());
        stringBuilder.append(" != ");
        stringBuilder.append(byteCount);
        throw new IOException(stringBuilder.toString());
    }

    void pushResetLater(int streamId, ErrorCode errorCode) {
        final int i = streamId;
        final ErrorCode errorCode2 = errorCode;
        this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Reset[%s]", new Object[]{this.hostname, Integer.valueOf(streamId)}) {
            public void execute() {
                Http2Connection.this.pushObserver.onReset(i, errorCode2);
                synchronized (Http2Connection.this) {
                    Http2Connection.this.currentPushRequests.remove(Integer.valueOf(i));
                }
            }
        });
    }
}
