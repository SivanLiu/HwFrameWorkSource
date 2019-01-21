package com.huawei.okhttp3.internal.ws;

import com.huawei.okhttp3.Call;
import com.huawei.okhttp3.Callback;
import com.huawei.okhttp3.OkHttpClient;
import com.huawei.okhttp3.Protocol;
import com.huawei.okhttp3.Request;
import com.huawei.okhttp3.Response;
import com.huawei.okhttp3.WebSocket;
import com.huawei.okhttp3.WebSocketListener;
import com.huawei.okhttp3.internal.Internal;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.connection.StreamAllocation;
import com.huawei.okhttp3.internal.ws.WebSocketReader.FrameCallback;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.BufferedSource;
import com.huawei.okio.ByteString;
import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class RealWebSocket implements WebSocket, FrameCallback {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long CANCEL_AFTER_CLOSE_MILLIS = 60000;
    private static final long MAX_QUEUE_SIZE = 16777216;
    private static final List<Protocol> ONLY_HTTP1 = Collections.singletonList(Protocol.HTTP_1_1);
    private Call call;
    private ScheduledFuture<?> cancelFuture;
    private boolean enqueuedClose;
    private ScheduledExecutorService executor;
    private boolean failed;
    private final String key;
    final WebSocketListener listener;
    private final ArrayDeque<Object> messageAndCloseQueue = new ArrayDeque();
    private final Request originalRequest;
    int pingCount;
    int pongCount;
    private final ArrayDeque<ByteString> pongQueue = new ArrayDeque();
    private long queueSize;
    private final Random random;
    private WebSocketReader reader;
    private int receivedCloseCode = -1;
    private String receivedCloseReason;
    private Streams streams;
    private WebSocketWriter writer;
    private final Runnable writerRunnable;

    final class CancelRunnable implements Runnable {
        CancelRunnable() {
        }

        public void run() {
            RealWebSocket.this.cancel();
        }
    }

    static final class Close {
        final long cancelAfterCloseMillis;
        final int code;
        final ByteString reason;

        Close(int code, ByteString reason, long cancelAfterCloseMillis) {
            this.code = code;
            this.reason = reason;
            this.cancelAfterCloseMillis = cancelAfterCloseMillis;
        }
    }

    static final class Message {
        final ByteString data;
        final int formatOpcode;

        Message(int formatOpcode, ByteString data) {
            this.formatOpcode = formatOpcode;
            this.data = data;
        }
    }

    private final class PingRunnable implements Runnable {
        private PingRunnable() {
        }

        /* synthetic */ PingRunnable(RealWebSocket x0, AnonymousClass1 x1) {
            this();
        }

        public void run() {
            RealWebSocket.this.writePingFrame();
        }
    }

    public static abstract class Streams implements Closeable {
        public final boolean client;
        public final BufferedSink sink;
        public final BufferedSource source;

        public Streams(boolean client, BufferedSource source, BufferedSink sink) {
            this.client = client;
            this.source = source;
            this.sink = sink;
        }
    }

    static final class ClientStreams extends Streams {
        private final StreamAllocation streamAllocation;

        ClientStreams(StreamAllocation streamAllocation) {
            super(true, streamAllocation.connection().source, streamAllocation.connection().sink);
            this.streamAllocation = streamAllocation;
        }

        public void close() {
            this.streamAllocation.streamFinished(true, this.streamAllocation.codec());
        }
    }

    public RealWebSocket(Request request, WebSocketListener listener, Random random) {
        if ("GET".equals(request.method())) {
            this.originalRequest = request;
            this.listener = listener;
            this.random = random;
            byte[] nonce = new byte[16];
            random.nextBytes(nonce);
            this.key = ByteString.of(nonce).base64();
            this.writerRunnable = new Runnable() {
                public void run() {
                    while (RealWebSocket.this.writeOneFrame()) {
                        try {
                        } catch (IOException e) {
                            RealWebSocket.this.failWebSocket(e, null);
                            return;
                        }
                    }
                }
            };
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Request must be GET: ");
        stringBuilder.append(request.method());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public Request request() {
        return this.originalRequest;
    }

    public synchronized long queueSize() {
        return this.queueSize;
    }

    public void cancel() {
        this.call.cancel();
    }

    public void connect(OkHttpClient client) {
        client = client.newBuilder().protocols(ONLY_HTTP1).build();
        final int pingIntervalMillis = client.pingIntervalMillis();
        final Request request = this.originalRequest.newBuilder().header("Upgrade", "websocket").header("Connection", "Upgrade").header("Sec-WebSocket-Key", this.key).header("Sec-WebSocket-Version", "13").build();
        this.call = Internal.instance.newWebSocketCall(client, request);
        this.call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) {
                try {
                    RealWebSocket.this.checkResponse(response);
                    StreamAllocation streamAllocation = Internal.instance.streamAllocation(call);
                    streamAllocation.noNewStreams();
                    Streams streams = new ClientStreams(streamAllocation);
                    try {
                        RealWebSocket.this.listener.onOpen(RealWebSocket.this, response);
                        String name = new StringBuilder();
                        name.append("OkHttp WebSocket ");
                        name.append(request.url().redact());
                        RealWebSocket.this.initReaderAndWriter(name.toString(), (long) pingIntervalMillis, streams);
                        streamAllocation.connection().socket().setSoTimeout(0);
                        RealWebSocket.this.loopReader();
                    } catch (Exception e) {
                        RealWebSocket.this.failWebSocket(e, null);
                    }
                } catch (ProtocolException e2) {
                    RealWebSocket.this.failWebSocket(e2, response);
                    Util.closeQuietly((Closeable) response);
                }
            }

            public void onFailure(Call call, IOException e) {
                RealWebSocket.this.failWebSocket(e, null);
            }
        });
    }

    void checkResponse(Response response) throws ProtocolException {
        if (response.code() == 101) {
            String headerConnection = response.header("Connection");
            if ("Upgrade".equalsIgnoreCase(headerConnection)) {
                String headerUpgrade = response.header("Upgrade");
                StringBuilder stringBuilder;
                if ("websocket".equalsIgnoreCase(headerUpgrade)) {
                    String headerAccept = response.header("Sec-WebSocket-Accept");
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.key);
                    stringBuilder.append("258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
                    String acceptExpected = ByteString.encodeUtf8(stringBuilder.toString()).sha1().base64();
                    if (!acceptExpected.equals(headerAccept)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Expected 'Sec-WebSocket-Accept' header value '");
                        stringBuilder2.append(acceptExpected);
                        stringBuilder2.append("' but was '");
                        stringBuilder2.append(headerAccept);
                        stringBuilder2.append("'");
                        throw new ProtocolException(stringBuilder2.toString());
                    }
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected 'Upgrade' header value 'websocket' but was '");
                stringBuilder.append(headerUpgrade);
                stringBuilder.append("'");
                throw new ProtocolException(stringBuilder.toString());
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Expected 'Connection' header value 'Upgrade' but was '");
            stringBuilder3.append(headerConnection);
            stringBuilder3.append("'");
            throw new ProtocolException(stringBuilder3.toString());
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Expected HTTP 101 response but was '");
        stringBuilder4.append(response.code());
        stringBuilder4.append(" ");
        stringBuilder4.append(response.message());
        stringBuilder4.append("'");
        throw new ProtocolException(stringBuilder4.toString());
    }

    public void initReaderAndWriter(String name, long pingIntervalMillis, Streams streams) throws IOException {
        synchronized (this) {
            this.streams = streams;
            this.writer = new WebSocketWriter(streams.client, streams.sink, this.random);
            this.executor = new ScheduledThreadPoolExecutor(1, Util.threadFactory(name, false));
            if (pingIntervalMillis != 0) {
                this.executor.scheduleAtFixedRate(new PingRunnable(this, null), pingIntervalMillis, pingIntervalMillis, TimeUnit.MILLISECONDS);
            }
            if (!this.messageAndCloseQueue.isEmpty()) {
                runWriter();
            }
        }
        this.reader = new WebSocketReader(streams.client, streams.source, this);
    }

    public void loopReader() throws IOException {
        while (this.receivedCloseCode == -1) {
            this.reader.processNextFrame();
        }
    }

    boolean processNextFrame() throws IOException {
        boolean z = false;
        try {
            this.reader.processNextFrame();
            if (this.receivedCloseCode == -1) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            failWebSocket(e, null);
            return false;
        }
    }

    synchronized int pingCount() {
        return this.pingCount;
    }

    synchronized int pongCount() {
        return this.pongCount;
    }

    public void onReadMessage(String text) throws IOException {
        this.listener.onMessage((WebSocket) this, text);
    }

    public void onReadMessage(ByteString bytes) throws IOException {
        this.listener.onMessage((WebSocket) this, bytes);
    }

    /* JADX WARNING: Missing block: B:13:0x0023, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void onReadPing(ByteString payload) {
        if (!this.failed) {
            if (!this.enqueuedClose || !this.messageAndCloseQueue.isEmpty()) {
                this.pongQueue.add(payload);
                runWriter();
                this.pingCount++;
            }
        }
    }

    public synchronized void onReadPong(ByteString buffer) {
        this.pongCount++;
    }

    public void onReadClose(int code, String reason) {
        if (code != -1) {
            Closeable toClose = null;
            synchronized (this) {
                if (this.receivedCloseCode == -1) {
                    this.receivedCloseCode = code;
                    this.receivedCloseReason = reason;
                    if (this.enqueuedClose && this.messageAndCloseQueue.isEmpty()) {
                        toClose = this.streams;
                        this.streams = null;
                        if (this.cancelFuture != null) {
                            this.cancelFuture.cancel(false);
                        }
                        this.executor.shutdown();
                    }
                } else {
                    throw new IllegalStateException("already closed");
                }
            }
            try {
                this.listener.onClosing(this, code, reason);
                if (toClose != null) {
                    this.listener.onClosed(this, code, reason);
                }
                Util.closeQuietly(toClose);
            } catch (Throwable th) {
                Util.closeQuietly(toClose);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean send(String text) {
        if (text != null) {
            return send(ByteString.encodeUtf8(text), 1);
        }
        throw new NullPointerException("text == null");
    }

    public boolean send(ByteString bytes) {
        if (bytes != null) {
            return send(bytes, 2);
        }
        throw new NullPointerException("bytes == null");
    }

    /* JADX WARNING: Missing block: B:18:0x003d, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean send(ByteString data, int formatOpcode) {
        if (!this.failed) {
            if (!this.enqueuedClose) {
                if (this.queueSize + ((long) data.size()) > MAX_QUEUE_SIZE) {
                    close(1001, null);
                    return false;
                }
                this.queueSize += (long) data.size();
                this.messageAndCloseQueue.add(new Message(formatOpcode, data));
                runWriter();
                return true;
            }
        }
    }

    synchronized boolean pong(ByteString payload) {
        if (!this.failed) {
            if (!this.enqueuedClose || !this.messageAndCloseQueue.isEmpty()) {
                this.pongQueue.add(payload);
                runWriter();
                return true;
            }
        }
        return false;
    }

    public boolean close(int code, String reason) {
        return close(code, reason, CANCEL_AFTER_CLOSE_MILLIS);
    }

    synchronized boolean close(int code, String reason, long cancelAfterCloseMillis) {
        WebSocketProtocol.validateCloseCode(code);
        ByteString reasonBytes = null;
        if (reason != null) {
            reasonBytes = ByteString.encodeUtf8(reason);
            if (((long) reasonBytes.size()) > 123) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reason.size() > 123: ");
                stringBuilder.append(reason);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (!this.failed) {
            if (!this.enqueuedClose) {
                this.enqueuedClose = true;
                this.messageAndCloseQueue.add(new Close(code, reasonBytes, cancelAfterCloseMillis));
                runWriter();
                return true;
            }
        }
        return false;
    }

    private void runWriter() {
        if (this.executor != null) {
            this.executor.execute(this.writerRunnable);
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0052, code skipped:
            r5 = r6;
     */
    /* JADX WARNING: Missing block: B:20:0x0053, code skipped:
            if (r5 == null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            r4.writePong(r5);
     */
    /* JADX WARNING: Missing block: B:26:0x005d, code skipped:
            if ((r0 instanceof com.huawei.okhttp3.internal.ws.RealWebSocket.Message) == false) goto L_0x008c;
     */
    /* JADX WARNING: Missing block: B:27:0x005f, code skipped:
            r6 = ((com.huawei.okhttp3.internal.ws.RealWebSocket.Message) r0).data;
            r7 = com.huawei.okio.Okio.buffer(r4.newMessageSink(((com.huawei.okhttp3.internal.ws.RealWebSocket.Message) r0).formatOpcode, (long) r6.size()));
            r7.write(r6);
            r7.close();
     */
    /* JADX WARNING: Missing block: B:28:0x007c, code skipped:
            monitor-enter(r12);
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            r12.queueSize -= (long) r6.size();
     */
    /* JADX WARNING: Missing block: B:31:0x0087, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:38:0x008e, code skipped:
            if ((r0 instanceof com.huawei.okhttp3.internal.ws.RealWebSocket.Close) == false) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:39:0x0090, code skipped:
            r6 = r0;
            r4.writeClose(r6.code, r6.reason);
     */
    /* JADX WARNING: Missing block: B:40:0x009a, code skipped:
            if (r3 == null) goto L_0x00a2;
     */
    /* JADX WARNING: Missing block: B:41:0x009c, code skipped:
            r12.listener.onClosed(r12, r1, r2);
     */
    /* JADX WARNING: Missing block: B:42:0x00a2, code skipped:
            com.huawei.okhttp3.internal.Util.closeQuietly(r3);
     */
    /* JADX WARNING: Missing block: B:43:0x00a6, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:46:0x00ac, code skipped:
            throw new java.lang.AssertionError();
     */
    /* JADX WARNING: Missing block: B:47:0x00ad, code skipped:
            com.huawei.okhttp3.internal.Util.closeQuietly(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean writeOneFrame() throws IOException {
        Close messageOrClose = null;
        int receivedCloseCode = -1;
        String receivedCloseReason = null;
        Closeable streamsToClose = null;
        synchronized (this) {
            if (this.failed) {
                return false;
            }
            WebSocketWriter writer = this.writer;
            ByteString pong = (ByteString) this.pongQueue.poll();
            if (pong == null) {
                messageOrClose = this.messageAndCloseQueue.poll();
                if (messageOrClose instanceof Close) {
                    receivedCloseCode = this.receivedCloseCode;
                    receivedCloseReason = this.receivedCloseReason;
                    if (receivedCloseCode != -1) {
                        streamsToClose = this.streams;
                        this.streams = null;
                        this.executor.shutdown();
                    } else {
                        this.cancelFuture = this.executor.schedule(new CancelRunnable(), messageOrClose.cancelAfterCloseMillis, TimeUnit.MILLISECONDS);
                    }
                } else if (messageOrClose == null) {
                    return false;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:?, code skipped:
            r0.writePing(com.huawei.okio.ByteString.EMPTY);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            failWebSocket(r1, null);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writePingFrame() {
        synchronized (this) {
            if (this.failed) {
                return;
            }
            WebSocketWriter writer = this.writer;
        }
    }

    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r3.listener.onFailure(r3, r4, r5);
     */
    /* JADX WARNING: Missing block: B:17:0x002e, code skipped:
            com.huawei.okhttp3.internal.Util.closeQuietly(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void failWebSocket(Exception e, Response response) {
        synchronized (this) {
            if (this.failed) {
                return;
            }
            this.failed = true;
            Closeable streamsToClose = this.streams;
            this.streams = null;
            if (this.cancelFuture != null) {
                this.cancelFuture.cancel(false);
            }
            if (this.executor != null) {
                this.executor.shutdown();
            }
        }
    }
}
