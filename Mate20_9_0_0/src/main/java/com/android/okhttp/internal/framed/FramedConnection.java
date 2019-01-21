package com.android.okhttp.internal.framed;

import com.android.okhttp.Protocol;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.NamedRunnable;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.framed.FrameReader.Handler;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.ByteString;
import com.android.okhttp.okio.Okio;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FramedConnection implements Closeable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int OKHTTP_CLIENT_WINDOW_SIZE = 16777216;
    private static final ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp FramedConnection", true));
    long bytesLeftInWriteWindow;
    final boolean client;
    private final Set<Integer> currentPushRequests;
    final FrameWriter frameWriter;
    private final String hostName;
    private long idleStartTimeNs;
    private int lastGoodStreamId;
    private final Listener listener;
    private int nextPingId;
    private int nextStreamId;
    Settings okHttpSettings;
    final Settings peerSettings;
    private Map<Integer, Ping> pings;
    final Protocol protocol;
    private final ExecutorService pushExecutor;
    private final PushObserver pushObserver;
    final Reader readerRunnable;
    private boolean receivedInitialPeerSettings;
    private boolean shutdown;
    final Socket socket;
    private final Map<Integer, FramedStream> streams;
    long unacknowledgedBytesRead;
    final Variant variant;

    public static class Builder {
        private boolean client;
        private String hostName;
        private Listener listener = Listener.REFUSE_INCOMING_STREAMS;
        private Protocol protocol = Protocol.SPDY_3;
        private PushObserver pushObserver = PushObserver.CANCEL;
        private BufferedSink sink;
        private Socket socket;
        private BufferedSource source;

        public Builder(boolean client) throws IOException {
            this.client = client;
        }

        public Builder socket(Socket socket) throws IOException {
            return socket(socket, ((InetSocketAddress) socket.getRemoteSocketAddress()).getHostName(), Okio.buffer(Okio.source(socket)), Okio.buffer(Okio.sink(socket)));
        }

        public Builder socket(Socket socket, String hostName, BufferedSource source, BufferedSink sink) {
            this.socket = socket;
            this.hostName = hostName;
            this.source = source;
            this.sink = sink;
            return this;
        }

        public Builder listener(Listener listener) {
            this.listener = listener;
            return this;
        }

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder pushObserver(PushObserver pushObserver) {
            this.pushObserver = pushObserver;
            return this;
        }

        public FramedConnection build() throws IOException {
            return new FramedConnection(this, null);
        }
    }

    public static abstract class Listener {
        public static final Listener REFUSE_INCOMING_STREAMS = new Listener() {
            public void onStream(FramedStream stream) throws IOException {
                stream.close(ErrorCode.REFUSED_STREAM);
            }
        };

        public abstract void onStream(FramedStream framedStream) throws IOException;

        public void onSettings(FramedConnection connection) {
        }
    }

    class Reader extends NamedRunnable implements Handler {
        final FrameReader frameReader;

        /* renamed from: com.android.okhttp.internal.framed.FramedConnection$Reader$1 */
        class AnonymousClass1 extends NamedRunnable {
            final /* synthetic */ FramedStream val$newStream;

            AnonymousClass1(String format, Object[] args, FramedStream framedStream) {
                this.val$newStream = framedStream;
                super(format, args);
            }

            public void execute() {
                try {
                    FramedConnection.this.listener.onStream(this.val$newStream);
                } catch (IOException e) {
                    Logger logger = Internal.logger;
                    Level level = Level.INFO;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("FramedConnection.Listener failure for ");
                    stringBuilder.append(FramedConnection.this.hostName);
                    logger.log(level, stringBuilder.toString(), e);
                    try {
                        this.val$newStream.close(ErrorCode.PROTOCOL_ERROR);
                    } catch (IOException e2) {
                    }
                }
            }
        }

        /* synthetic */ Reader(FramedConnection x0, FrameReader x1, AnonymousClass1 x2) {
            this(x1);
        }

        private Reader(FrameReader frameReader) {
            super("OkHttp %s", this$0.hostName);
            this.frameReader = frameReader;
        }

        protected void execute() {
            ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
            ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;
            try {
                if (!FramedConnection.this.client) {
                    this.frameReader.readConnectionPreface();
                }
                while (this.frameReader.nextFrame(this)) {
                }
                connectionErrorCode = ErrorCode.NO_ERROR;
                streamErrorCode = ErrorCode.CANCEL;
            } catch (IOException e) {
                connectionErrorCode = ErrorCode.PROTOCOL_ERROR;
                streamErrorCode = ErrorCode.PROTOCOL_ERROR;
            } finally {
                try {
                    FramedConnection.this.close(connectionErrorCode, streamErrorCode);
                } catch (IOException e2) {
                }
                Util.closeQuietly(this.frameReader);
            }
        }

        public void data(boolean inFinished, int streamId, BufferedSource source, int length) throws IOException {
            if (FramedConnection.this.pushedStream(streamId)) {
                FramedConnection.this.pushDataLater(streamId, source, length, inFinished);
                return;
            }
            FramedStream dataStream = FramedConnection.this.getStream(streamId);
            if (dataStream == null) {
                FramedConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
                source.skip((long) length);
                return;
            }
            dataStream.receiveData(source, length);
            if (inFinished) {
                dataStream.receiveFin();
            }
        }

        /* JADX WARNING: Missing block: B:28:0x0091, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:31:0x0097, code skipped:
            if (r20.failIfStreamPresent() == false) goto L_0x00a4;
     */
        /* JADX WARNING: Missing block: B:32:0x0099, code skipped:
            r0.closeLater(com.android.okhttp.internal.framed.ErrorCode.PROTOCOL_ERROR);
            r1.this$0.removeStream(r9);
     */
        /* JADX WARNING: Missing block: B:33:0x00a3, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:34:0x00a4, code skipped:
            r0.receiveHeaders(r10, r20);
     */
        /* JADX WARNING: Missing block: B:35:0x00a9, code skipped:
            if (r8 == false) goto L_0x00ae;
     */
        /* JADX WARNING: Missing block: B:36:0x00ab, code skipped:
            r0.receiveFin();
     */
        /* JADX WARNING: Missing block: B:37:0x00ae, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void headers(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock, HeadersMode headersMode) {
            boolean z = inFinished;
            int i = streamId;
            List<Header> list = headerBlock;
            if (FramedConnection.this.pushedStream(i)) {
                FramedConnection.this.pushHeadersLater(i, list, z);
                return;
            }
            synchronized (FramedConnection.this) {
                try {
                    if (FramedConnection.this.shutdown) {
                        return;
                    }
                    FramedStream stream = FramedConnection.this.getStream(i);
                    if (stream == null) {
                        if (headersMode.failIfStreamAbsent()) {
                            FramedConnection.this.writeSynResetLater(i, ErrorCode.INVALID_STREAM);
                        } else if (i <= FramedConnection.this.lastGoodStreamId) {
                        } else if (i % 2 == FramedConnection.this.nextStreamId % 2) {
                        } else {
                            FramedConnection framedConnection = FramedConnection.this;
                        }
                    }
                } finally {
                    HeadersMode headersMode2 = headersMode;
                    while (true) {
                    }
                }
            }
        }

        public void rstStream(int streamId, ErrorCode errorCode) {
            if (FramedConnection.this.pushedStream(streamId)) {
                FramedConnection.this.pushResetLater(streamId, errorCode);
                return;
            }
            FramedStream rstStream = FramedConnection.this.removeStream(streamId);
            if (rstStream != null) {
                rstStream.receiveRstStream(errorCode);
            }
        }

        public void settings(boolean clearPrevious, Settings newSettings) {
            int i;
            long delta = 0;
            FramedStream[] streamsToNotify = null;
            synchronized (FramedConnection.this) {
                int priorWriteWindowSize = FramedConnection.this.peerSettings.getInitialWindowSize(65536);
                if (clearPrevious) {
                    FramedConnection.this.peerSettings.clear();
                }
                FramedConnection.this.peerSettings.merge(newSettings);
                if (FramedConnection.this.getProtocol() == Protocol.HTTP_2) {
                    ackSettingsLater(newSettings);
                }
                int peerInitialWindowSize = FramedConnection.this.peerSettings.getInitialWindowSize(65536);
                if (!(peerInitialWindowSize == -1 || peerInitialWindowSize == priorWriteWindowSize)) {
                    delta = (long) (peerInitialWindowSize - priorWriteWindowSize);
                    if (!FramedConnection.this.receivedInitialPeerSettings) {
                        FramedConnection.this.addBytesToWriteWindow(delta);
                        FramedConnection.this.receivedInitialPeerSettings = true;
                    }
                    if (!FramedConnection.this.streams.isEmpty()) {
                        streamsToNotify = (FramedStream[]) FramedConnection.this.streams.values().toArray(new FramedStream[FramedConnection.this.streams.size()]);
                    }
                }
                ExecutorService access$2100 = FramedConnection.executor;
                Object[] objArr = new Object[1];
                i = 0;
                objArr[0] = FramedConnection.this.hostName;
                access$2100.execute(new NamedRunnable("OkHttp %s settings", objArr) {
                    public void execute() {
                        FramedConnection.this.listener.onSettings(FramedConnection.this);
                    }
                });
            }
            if (streamsToNotify != null && delta != 0) {
                int length = streamsToNotify.length;
                while (i < length) {
                    FramedStream stream = streamsToNotify[i];
                    synchronized (stream) {
                        stream.addBytesToWriteWindow(delta);
                    }
                    i++;
                }
            }
        }

        private void ackSettingsLater(final Settings peerSettings) {
            FramedConnection.executor.execute(new NamedRunnable("OkHttp %s ACK Settings", new Object[]{FramedConnection.this.hostName}) {
                public void execute() {
                    try {
                        FramedConnection.this.frameWriter.ackSettings(peerSettings);
                    } catch (IOException e) {
                    }
                }
            });
        }

        public void ackSettings() {
        }

        public void ping(boolean reply, int payload1, int payload2) {
            if (reply) {
                Ping ping = FramedConnection.this.removePing(payload1);
                if (ping != null) {
                    ping.receive();
                    return;
                }
                return;
            }
            FramedConnection.this.writePingLater(true, payload1, payload2, null);
        }

        public void goAway(int lastGoodStreamId, ErrorCode errorCode, ByteString debugData) {
            debugData.size();
            synchronized (FramedConnection.this) {
                FramedStream[] streamsCopy = (FramedStream[]) FramedConnection.this.streams.values().toArray(new FramedStream[FramedConnection.this.streams.size()]);
                FramedConnection.this.shutdown = true;
            }
            for (FramedStream framedStream : streamsCopy) {
                if (framedStream.getId() > lastGoodStreamId && framedStream.isLocallyInitiated()) {
                    framedStream.receiveRstStream(ErrorCode.REFUSED_STREAM);
                    FramedConnection.this.removeStream(framedStream.getId());
                }
            }
        }

        public void windowUpdate(int streamId, long windowSizeIncrement) {
            if (streamId == 0) {
                synchronized (FramedConnection.this) {
                    FramedConnection framedConnection = FramedConnection.this;
                    framedConnection.bytesLeftInWriteWindow += windowSizeIncrement;
                    FramedConnection.this.notifyAll();
                }
                return;
            }
            FramedStream stream = FramedConnection.this.getStream(streamId);
            if (stream != null) {
                synchronized (stream) {
                    stream.addBytesToWriteWindow(windowSizeIncrement);
                }
            }
        }

        public void priority(int streamId, int streamDependency, int weight, boolean exclusive) {
        }

        public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) {
            FramedConnection.this.pushRequestLater(promisedStreamId, requestHeaders);
        }

        public void alternateService(int streamId, String origin, ByteString protocol, String host, int port, long maxAge) {
        }
    }

    /* synthetic */ FramedConnection(Builder x0, AnonymousClass1 x1) throws IOException {
        this(x0);
    }

    private FramedConnection(Builder builder) throws IOException {
        this.streams = new HashMap();
        this.idleStartTimeNs = System.nanoTime();
        this.unacknowledgedBytesRead = 0;
        this.okHttpSettings = new Settings();
        this.peerSettings = new Settings();
        this.receivedInitialPeerSettings = $assertionsDisabled;
        this.currentPushRequests = new LinkedHashSet();
        this.protocol = builder.protocol;
        this.pushObserver = builder.pushObserver;
        this.client = builder.client;
        this.listener = builder.listener;
        int i = 2;
        this.nextStreamId = builder.client ? 1 : 2;
        if (builder.client && this.protocol == Protocol.HTTP_2) {
            this.nextStreamId += 2;
        }
        if (builder.client) {
            i = 1;
        }
        this.nextPingId = i;
        if (builder.client) {
            this.okHttpSettings.set(7, 0, OKHTTP_CLIENT_WINDOW_SIZE);
        }
        this.hostName = builder.hostName;
        if (this.protocol == Protocol.HTTP_2) {
            this.variant = new Http2();
            this.pushExecutor = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory(String.format("OkHttp %s Push Observer", new Object[]{this.hostName}), true));
            this.peerSettings.set(7, 0, 65535);
            this.peerSettings.set(5, 0, 16384);
        } else if (this.protocol == Protocol.SPDY_3) {
            this.variant = new Spdy3();
            this.pushExecutor = null;
        } else {
            throw new AssertionError(this.protocol);
        }
        this.bytesLeftInWriteWindow = (long) this.peerSettings.getInitialWindowSize(65536);
        this.socket = builder.socket;
        this.frameWriter = this.variant.newWriter(builder.sink, this.client);
        this.readerRunnable = new Reader(this, this.variant.newReader(builder.source, this.client), null);
        new Thread(this.readerRunnable).start();
    }

    public Protocol getProtocol() {
        return this.protocol;
    }

    public synchronized int openStreamCount() {
        return this.streams.size();
    }

    synchronized FramedStream getStream(int id) {
        return (FramedStream) this.streams.get(Integer.valueOf(id));
    }

    synchronized FramedStream removeStream(int streamId) {
        FramedStream stream;
        stream = (FramedStream) this.streams.remove(Integer.valueOf(streamId));
        if (stream != null && this.streams.isEmpty()) {
            setIdle(true);
        }
        notifyAll();
        return stream;
    }

    private synchronized void setIdle(boolean value) {
        long nanoTime;
        if (value) {
            try {
                nanoTime = System.nanoTime();
            } catch (Throwable th) {
            }
        } else {
            nanoTime = Long.MAX_VALUE;
        }
        this.idleStartTimeNs = nanoTime;
    }

    public synchronized boolean isIdle() {
        return this.idleStartTimeNs != Long.MAX_VALUE ? true : $assertionsDisabled;
    }

    public synchronized int maxConcurrentStreams() {
        return this.peerSettings.getMaxConcurrentStreams(Integer.MAX_VALUE);
    }

    public synchronized long getIdleStartTimeNs() {
        return this.idleStartTimeNs;
    }

    public FramedStream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
        if (this.client) {
            throw new IllegalStateException("Client cannot push requests.");
        } else if (this.protocol == Protocol.HTTP_2) {
            return newStream(associatedStreamId, requestHeaders, out, $assertionsDisabled);
        } else {
            throw new IllegalStateException("protocol != HTTP_2");
        }
    }

    public FramedStream newStream(List<Header> requestHeaders, boolean out, boolean in) throws IOException {
        return newStream(0, requestHeaders, out, in);
    }

    /* JADX WARNING: Missing block: B:11:0x0037, code skipped:
            if (r14 != 0) goto L_0x004d;
     */
    /* JADX WARNING: Missing block: B:14:0x003b, code skipped:
            r2 = r13;
     */
    /* JADX WARNING: Missing block: B:16:?, code skipped:
            r7.frameWriter.synStream(r4, r5, r0, r14, r18);
            r6 = r18;
     */
    /* JADX WARNING: Missing block: B:17:0x0048, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:18:0x0049, code skipped:
            r2 = r13;
     */
    /* JADX WARNING: Missing block: B:19:0x004a, code skipped:
            r6 = r18;
     */
    /* JADX WARNING: Missing block: B:20:0x004d, code skipped:
            r2 = r13;
     */
    /* JADX WARNING: Missing block: B:21:0x0050, code skipped:
            if (r7.client != false) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            r7.frameWriter.pushPromise(r14, r0, r18);
     */
    /* JADX WARNING: Missing block: B:26:0x0059, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:27:0x005a, code skipped:
            if (r19 != false) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:28:0x005c, code skipped:
            r7.frameWriter.flush();
     */
    /* JADX WARNING: Missing block: B:29:0x0061, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:30:0x0062, code skipped:
            r6 = r18;
     */
    /* JADX WARNING: Missing block: B:33:0x006b, code skipped:
            throw new java.lang.IllegalArgumentException("client streams shouldn't have associated stream IDs");
     */
    /* JADX WARNING: Missing block: B:34:0x006c, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private FramedStream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out, boolean in) throws IOException {
        Throwable th;
        int i = associatedStreamId;
        boolean outFinished = out ^ 1;
        boolean inFinished = in ^ 1;
        FrameWriter frameWriter = this.frameWriter;
        synchronized (frameWriter) {
            List<Header> list;
            boolean z;
            try {
                synchronized (this) {
                    try {
                        if (this.shutdown) {
                            list = requestHeaders;
                            z = frameWriter;
                            throw new IOException("shutdown");
                        }
                        int streamId = this.nextStreamId;
                        this.nextStreamId += 2;
                        FramedStream stream = new FramedStream(streamId, this, outFinished, inFinished, requestHeaders);
                        z = stream.isOpen();
                        if (z) {
                            this.streams.put(Integer.valueOf(streamId), stream);
                            z = $assertionsDisabled;
                            setIdle($assertionsDisabled);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    void writeSynReply(int streamId, boolean outFinished, List<Header> alternating) throws IOException {
        this.frameWriter.synReply(outFinished, streamId, alternating);
    }

    public void writeData(int streamId, boolean outFinished, Buffer buffer, long byteCount) throws IOException {
        if (byteCount == 0) {
            this.frameWriter.data(outFinished, streamId, buffer, 0);
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
                toWrite = Math.min((int) Math.min(byteCount, this.bytesLeftInWriteWindow), this.frameWriter.maxDataLength());
                this.bytesLeftInWriteWindow -= (long) toWrite;
            }
            byteCount -= (long) toWrite;
            FrameWriter frameWriter = this.frameWriter;
            boolean z = (outFinished && byteCount == 0) ? true : $assertionsDisabled;
            frameWriter.data(z, streamId, buffer, toWrite);
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
        executor.submit(new NamedRunnable("OkHttp %s stream %d", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            public void execute() {
                try {
                    FramedConnection.this.writeSynReset(i, errorCode2);
                } catch (IOException e) {
                }
            }
        });
    }

    void writeSynReset(int streamId, ErrorCode statusCode) throws IOException {
        this.frameWriter.rstStream(streamId, statusCode);
    }

    void writeWindowUpdateLater(int streamId, long unacknowledgedBytesRead) {
        final int i = streamId;
        final long j = unacknowledgedBytesRead;
        executor.execute(new NamedRunnable("OkHttp Window Update %s stream %d", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            public void execute() {
                try {
                    FramedConnection.this.frameWriter.windowUpdate(i, j);
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
                throw new IOException("shutdown");
            }
            pingId = this.nextPingId;
            this.nextPingId += 2;
            if (this.pings == null) {
                this.pings = new HashMap();
            }
            this.pings.put(Integer.valueOf(pingId), ping);
        }
        writePing($assertionsDisabled, pingId, 1330343787, ping);
        return ping;
    }

    private void writePingLater(boolean reply, int payload1, int payload2, Ping ping) {
        final boolean z = reply;
        final int i = payload1;
        final int i2 = payload2;
        final Ping ping2 = ping;
        executor.execute(new NamedRunnable("OkHttp %s ping %08x%08x", new Object[]{this.hostName, Integer.valueOf(payload1), Integer.valueOf(payload2)}) {
            public void execute() {
                try {
                    FramedConnection.this.writePing(z, i, i2, ping2);
                } catch (IOException e) {
                }
            }
        });
    }

    private void writePing(boolean reply, int payload1, int payload2, Ping ping) throws IOException {
        synchronized (this.frameWriter) {
            if (ping != null) {
                try {
                    ping.send();
                } catch (Throwable th) {
                }
            }
            this.frameWriter.ping(reply, payload1, payload2);
        }
    }

    private synchronized Ping removePing(int id) {
        return this.pings != null ? (Ping) this.pings.remove(Integer.valueOf(id)) : null;
    }

    public void flush() throws IOException {
        this.frameWriter.flush();
    }

    public void shutdown(ErrorCode statusCode) throws IOException {
        synchronized (this.frameWriter) {
            synchronized (this) {
                if (this.shutdown) {
                    return;
                }
                this.shutdown = true;
                int lastGoodStreamId = this.lastGoodStreamId;
                this.frameWriter.goAway(lastGoodStreamId, statusCode, Util.EMPTY_BYTE_ARRAY);
            }
        }
    }

    public void close() throws IOException {
        close(ErrorCode.NO_ERROR, ErrorCode.CANCEL);
    }

    private void close(ErrorCode connectionCode, ErrorCode streamCode) throws IOException {
        int i;
        int length;
        IOException thrown = null;
        try {
            shutdown(connectionCode);
        } catch (IOException e) {
            thrown = e;
        }
        FramedStream[] streamsToClose = null;
        Ping[] pingsToCancel = null;
        synchronized (this) {
            i = 0;
            if (!this.streams.isEmpty()) {
                streamsToClose = (FramedStream[]) this.streams.values().toArray(new FramedStream[this.streams.size()]);
                this.streams.clear();
                setIdle($assertionsDisabled);
            }
            if (this.pings != null) {
                pingsToCancel = (Ping[]) this.pings.values().toArray(new Ping[this.pings.size()]);
                this.pings = null;
            }
        }
        if (streamsToClose != null) {
            IOException thrown2 = thrown;
            for (FramedStream stream : streamsToClose) {
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
            this.frameWriter.close();
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

    public void sendConnectionPreface() throws IOException {
        this.frameWriter.connectionPreface();
        this.frameWriter.settings(this.okHttpSettings);
        int windowSize = this.okHttpSettings.getInitialWindowSize(65536);
        if (windowSize != 65536) {
            this.frameWriter.windowUpdate(0, (long) (windowSize - 65536));
        }
    }

    public void setSettings(Settings settings) throws IOException {
        synchronized (this.frameWriter) {
            synchronized (this) {
                if (this.shutdown) {
                    throw new IOException("shutdown");
                }
                this.okHttpSettings.merge(settings);
                this.frameWriter.settings(settings);
            }
        }
    }

    private boolean pushedStream(int streamId) {
        return (this.protocol == Protocol.HTTP_2 && streamId != 0 && (streamId & 1) == 0) ? true : $assertionsDisabled;
    }

    private void pushRequestLater(int streamId, List<Header> requestHeaders) {
        synchronized (this) {
            if (this.currentPushRequests.contains(Integer.valueOf(streamId))) {
                writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
                return;
            }
            this.currentPushRequests.add(Integer.valueOf(streamId));
            final int i = streamId;
            final List<Header> list = requestHeaders;
            this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Request[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
                public void execute() {
                    if (FramedConnection.this.pushObserver.onRequest(i, list)) {
                        try {
                            FramedConnection.this.frameWriter.rstStream(i, ErrorCode.CANCEL);
                            synchronized (FramedConnection.this) {
                                FramedConnection.this.currentPushRequests.remove(Integer.valueOf(i));
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });
        }
    }

    private void pushHeadersLater(int streamId, List<Header> requestHeaders, boolean inFinished) {
        final int i = streamId;
        final List<Header> list = requestHeaders;
        final boolean z = inFinished;
        this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Headers[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            public void execute() {
                boolean cancel = FramedConnection.this.pushObserver.onHeaders(i, list, z);
                if (cancel) {
                    try {
                        FramedConnection.this.frameWriter.rstStream(i, ErrorCode.CANCEL);
                    } catch (IOException e) {
                        return;
                    }
                }
                if (cancel || z) {
                    synchronized (FramedConnection.this) {
                        FramedConnection.this.currentPushRequests.remove(Integer.valueOf(i));
                    }
                }
            }
        });
    }

    private void pushDataLater(int streamId, BufferedSource source, int byteCount, boolean inFinished) throws IOException {
        Buffer buffer = new Buffer();
        source.require((long) byteCount);
        source.read(buffer, (long) byteCount);
        if (buffer.size() == ((long) byteCount)) {
            final int i = streamId;
            final Buffer buffer2 = buffer;
            final int i2 = byteCount;
            final boolean z = inFinished;
            this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Data[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
                public void execute() {
                    try {
                        boolean cancel = FramedConnection.this.pushObserver.onData(i, buffer2, i2, z);
                        if (cancel) {
                            FramedConnection.this.frameWriter.rstStream(i, ErrorCode.CANCEL);
                        }
                        if (cancel || z) {
                            synchronized (FramedConnection.this) {
                                FramedConnection.this.currentPushRequests.remove(Integer.valueOf(i));
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

    private void pushResetLater(int streamId, ErrorCode errorCode) {
        final int i = streamId;
        final ErrorCode errorCode2 = errorCode;
        this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Reset[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            public void execute() {
                FramedConnection.this.pushObserver.onReset(i, errorCode2);
                synchronized (FramedConnection.this) {
                    FramedConnection.this.currentPushRequests.remove(Integer.valueOf(i));
                }
            }
        });
    }
}
