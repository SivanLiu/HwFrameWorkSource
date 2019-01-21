package com.huawei.okhttp3.internal.connection;

import com.huawei.okhttp3.Address;
import com.huawei.okhttp3.ConnectionPool;
import com.huawei.okhttp3.OkHttpClient;
import com.huawei.okhttp3.Request;
import com.huawei.okhttp3.Route;
import com.huawei.okhttp3.internal.Internal;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.http.HttpCodec;
import com.huawei.okhttp3.internal.http1.Http1Codec;
import com.huawei.okhttp3.internal.http2.ConnectionShutdownException;
import com.huawei.okhttp3.internal.http2.ErrorCode;
import com.huawei.okhttp3.internal.http2.Http2Codec;
import com.huawei.okhttp3.internal.http2.StreamResetException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public final class StreamAllocation {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int HTTP2_CONNECTION_WAIT_TIME = 1000;
    public final Address address;
    private final Object callStackTrace;
    private boolean canceled;
    private HttpCodec codec;
    private RealConnection connection;
    private final int connectionAttemptDelay;
    private final ConnectionPool connectionPool;
    private boolean http2Indicator = false;
    private int refusedStreamCount;
    private boolean released;
    private Route route;
    private final RouteSelector routeSelector;

    public static final class StreamAllocationReference extends WeakReference<StreamAllocation> {
        public final Object callStackTrace;

        StreamAllocationReference(StreamAllocation referent, Object callStackTrace) {
            super(referent);
            this.callStackTrace = callStackTrace;
        }
    }

    public StreamAllocation(ConnectionPool connectionPool, Address address, Object callStackTrace, Request request, int connectionAttemptDelay) {
        this.connectionPool = connectionPool;
        this.address = address;
        this.routeSelector = new RouteSelector(address, routeDatabase(), request.concurrentConnectEnabled(), request.additionalInetAddresses());
        this.callStackTrace = callStackTrace;
        this.connectionAttemptDelay = connectionAttemptDelay;
    }

    public StreamAllocation(ConnectionPool connectionPool, Address address, Object callStackTrace) {
        this.connectionPool = connectionPool;
        this.address = address;
        this.routeSelector = new RouteSelector(address, routeDatabase());
        this.callStackTrace = callStackTrace;
        this.connectionAttemptDelay = 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0050  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x007e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0050  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x007e A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void newHttp2Connection(OkHttpClient client, Request request) throws IOException {
        RealConnection newConnection;
        Route selectedRoute;
        int i;
        Exception e;
        int connectTimeout = client.connectTimeoutMillis();
        int readTimeout = client.readTimeoutMillis();
        int writeTimeout = client.writeTimeoutMillis();
        boolean connectionRetryEnabled = client.retryOnConnectionFailure();
        if (Integer.parseInt(request.header("Http2ConnectionIndex")) > this.connectionPool.http2ConnectionCount(this.address)) {
            Route selectedRoute2;
            int i2 = this.connectionPool;
            synchronized (i2) {
                try {
                    selectedRoute2 = this.route;
                } finally {
                    connectTimeout = 
/*
Method generation error in method: com.huawei.okhttp3.internal.connection.StreamAllocation.newHttp2Connection(com.huawei.okhttp3.OkHttpClient, com.huawei.okhttp3.Request):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r8_4 'connectTimeout' int) = (r8_0 'connectTimeout' int), (r2_8 'i2' int) in method: com.huawei.okhttp3.internal.connection.StreamAllocation.newHttp2Connection(com.huawei.okhttp3.OkHttpClient, com.huawei.okhttp3.Request):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:102)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:52)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeSynchronizedRegion(RegionGen.java:230)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:67)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 35 more

*/

    public HttpCodec newStream(OkHttpClient client, boolean doExtensiveHealthChecks) {
        int connectTimeout = client.connectTimeoutMillis();
        int readTimeout = client.readTimeoutMillis();
        int writeTimeout = client.writeTimeoutMillis();
        try {
            HttpCodec resultCodec;
            RealConnection resultConnection = findHealthyConnection(client, connectTimeout, readTimeout, writeTimeout, client.retryOnConnectionFailure(), doExtensiveHealthChecks);
            if (resultConnection.http2Connection != null) {
                resultCodec = new Http2Codec(client, this, resultConnection.http2Connection);
            } else {
                resultConnection.socket().setSoTimeout(readTimeout);
                resultConnection.source.timeout().timeout((long) readTimeout, TimeUnit.MILLISECONDS);
                resultConnection.sink.timeout().timeout((long) writeTimeout, TimeUnit.MILLISECONDS);
                resultCodec = new Http1Codec(client, this, resultConnection.source, resultConnection.sink);
            }
            synchronized (this.connectionPool) {
                this.codec = resultCodec;
            }
            return resultCodec;
        } catch (IOException e) {
            throw new RouteException(e);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            if (r0.isHealthy(r9) != false) goto L_0x0018;
     */
    /* JADX WARNING: Missing block: B:11:0x0018, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private RealConnection findHealthyConnection(OkHttpClient client, int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks) throws IOException {
        while (true) {
            RealConnection candidate = findConnection(client, connectTimeout, readTimeout, writeTimeout, connectionRetryEnabled);
            synchronized (this.connectionPool) {
                if (candidate.successCount == 0) {
                    return candidate;
                }
            }
            noNewStreams();
        }
        while (true) {
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0030, code skipped:
            if (r10.http2Indicator == false) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:25:0x003a, code skipped:
            if (r10.connectionPool.http2ConnectionCount(r10.address) != 0) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:26:0x003c, code skipped:
            r0 = false;
            r3 = r10.connectionPool.h2AvailableLock;
     */
    /* JADX WARNING: Missing block: B:27:0x0041, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:30:0x0046, code skipped:
            if (r10.connectionPool.h2ConnectionIsCreating == false) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            r10.connectionPool.h2AvailableLock.wait(1000);
     */
    /* JADX WARNING: Missing block: B:33:0x0051, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:36:?, code skipped:
            r10.connectionPool.h2ConnectionIsCreating = true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private RealConnection findConnection(OkHttpClient client, int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled) throws IOException {
        RealConnection pooledConnection;
        Route selectedRoute;
        synchronized (this.connectionPool) {
            if (this.released) {
                throw new IllegalStateException("released");
            } else if (this.codec != null) {
                throw new IllegalStateException("codec != null");
            } else if (this.canceled) {
                throw new IOException("Canceled");
            } else {
                RealConnection allocatedConnection = this.connection;
                if (allocatedConnection == null || allocatedConnection.noNewStreams) {
                    pooledConnection = Internal.instance.get(this.connectionPool, this.address, this);
                    if (pooledConnection != null) {
                        this.connection = pooledConnection;
                        return pooledConnection;
                    }
                    selectedRoute = this.route;
                } else {
                    return allocatedConnection;
                }
            }
        }
        RealConnection newConnection;
        if (reFindConnection) {
            return findConnection(client, connectTimeout, readTimeout, writeTimeout, connectionRetryEnabled);
        }
        if (selectedRoute == null) {
            selectedRoute = this.routeSelector.next();
        }
        synchronized (this.connectionPool) {
            this.route = selectedRoute;
            this.refusedStreamCount = 0;
            newConnection = new RealConnection(selectedRoute);
            if (this.routeSelector.concurrentConnectEnabled()) {
                newConnection.prepareConcurrentConnect(this.routeSelector.concurrentInetSocketAddresses(), this.connectionAttemptDelay);
                newConnection.setRouteSelector(this.routeSelector);
            }
            acquire(newConnection);
        }
        pooledConnection = newConnection;
        if (this.canceled) {
            if (this.http2Indicator) {
                synchronized (this.connectionPool.h2AvailableLock) {
                    this.connectionPool.h2ConnectionIsCreating = false;
                    this.connectionPool.h2AvailableLock.notifyAll();
                }
            }
            throw new IOException("Canceled");
        }
        Closeable closeable = null;
        try {
            pooledConnection.connect(connectTimeout, readTimeout, writeTimeout, this.address.connectionSpecs(), connectionRetryEnabled);
            this.routeSelector.connected(pooledConnection.route());
            synchronized (this.connectionPool) {
                Internal.instance.put(this.connectionPool, pooledConnection);
                if (pooledConnection.http2Connection != null) {
                    closeable = Internal.instance.deduplicate(client, this.connectionPool, this.address, this);
                    pooledConnection = this.connection;
                }
            }
            if (this.http2Indicator) {
                synchronized (this.connectionPool.h2AvailableLock) {
                    this.connectionPool.h2ConnectionIsCreating = false;
                    this.connectionPool.h2AvailableLock.notifyAll();
                }
            }
            Util.closeQuietly(closeable);
            return pooledConnection;
        } catch (Exception e) {
            try {
                throw e;
            } catch (Throwable th) {
                if (this.http2Indicator) {
                    synchronized (this.connectionPool.h2AvailableLock) {
                        this.connectionPool.h2ConnectionIsCreating = false;
                        this.connectionPool.h2AvailableLock.notifyAll();
                    }
                }
            }
        }
    }

    public void streamFinished(boolean noNewStreams, HttpCodec codec) {
        Closeable closeable;
        synchronized (this.connectionPool) {
            if (codec != null) {
                if (codec == this.codec) {
                    if (!noNewStreams) {
                        RealConnection realConnection = this.connection;
                        realConnection.successCount++;
                    }
                    closeable = deallocate(noNewStreams, false, true);
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("expected ");
            stringBuilder.append(this.codec);
            stringBuilder.append(" but was ");
            stringBuilder.append(codec);
            throw new IllegalStateException(stringBuilder.toString());
        }
        Util.closeQuietly(closeable);
    }

    public HttpCodec codec() {
        HttpCodec httpCodec;
        synchronized (this.connectionPool) {
            httpCodec = this.codec;
        }
        return httpCodec;
    }

    private RouteDatabase routeDatabase() {
        return Internal.instance.routeDatabase(this.connectionPool);
    }

    public synchronized RealConnection connection() {
        return this.connection;
    }

    public void release() {
        Closeable closeable;
        synchronized (this.connectionPool) {
            closeable = deallocate(false, true, false);
        }
        Util.closeQuietly(closeable);
    }

    public void noNewStreams() {
        Closeable closeable;
        synchronized (this.connectionPool) {
            closeable = deallocate(true, false, false);
        }
        Util.closeQuietly(closeable);
    }

    private Closeable deallocate(boolean noNewStreams, boolean released, boolean streamFinished) {
        Closeable closeable = null;
        if (streamFinished) {
            this.codec = null;
        }
        if (released) {
            this.released = true;
        }
        if (this.connection != null) {
            if (noNewStreams) {
                this.connection.noNewStreams = true;
            }
            if (this.codec == null && (this.released || this.connection.noNewStreams)) {
                release(this.connection);
                if (this.connection.allocations.isEmpty()) {
                    this.connection.idleAtNanos = System.nanoTime();
                    if (Internal.instance.connectionBecameIdle(this.connectionPool, this.connection)) {
                        closeable = this.connection.socket();
                    }
                }
                this.connection = null;
            }
        }
        return closeable;
    }

    public void cancel() {
        HttpCodec codecToCancel;
        RealConnection connectionToCancel;
        synchronized (this.connectionPool) {
            this.canceled = true;
            codecToCancel = this.codec;
            connectionToCancel = this.connection;
        }
        if (codecToCancel != null) {
            codecToCancel.cancel();
        } else if (connectionToCancel != null) {
            connectionToCancel.cancel();
        }
    }

    public void streamFailed(IOException e) {
        Closeable closeable;
        boolean noNewStreams = false;
        synchronized (this.connectionPool) {
            if (e instanceof StreamResetException) {
                StreamResetException streamResetException = (StreamResetException) e;
                if (streamResetException.errorCode == ErrorCode.REFUSED_STREAM) {
                    this.refusedStreamCount++;
                }
                if (streamResetException.errorCode != ErrorCode.REFUSED_STREAM || this.refusedStreamCount > 1) {
                    noNewStreams = true;
                    this.route = null;
                }
            } else if (this.connection != null && (!this.connection.isMultiplexed() || (e instanceof ConnectionShutdownException))) {
                noNewStreams = true;
                if (this.connection.successCount == 0) {
                    if (!(this.route == null || e == null)) {
                        this.routeSelector.connectFailed(this.route, e);
                    }
                    this.route = null;
                }
            }
            closeable = deallocate(noNewStreams, null, true);
        }
        Util.closeQuietly(closeable);
    }

    public void acquire(RealConnection connection) {
        if (this.connection == null || this.connection.noNewStreams) {
            this.connection = connection;
            connection.allocations.add(new StreamAllocationReference(this, this.callStackTrace));
            return;
        }
        throw new IllegalStateException();
    }

    private void release(RealConnection connection) {
        int size = connection.allocations.size();
        for (int i = 0; i < size; i++) {
            if (((Reference) connection.allocations.get(i)).get() == this) {
                connection.allocations.remove(i);
                return;
            }
        }
        throw new IllegalStateException();
    }

    public Closeable releaseAndAcquire(RealConnection newConnection) {
        if (this.codec == null && this.connection.allocations.size() == 1) {
            Reference<StreamAllocation> onlyAllocation = (Reference) this.connection.allocations.get(0);
            Closeable closeable = deallocate(true, false, false);
            this.connection = newConnection;
            newConnection.allocations.add(onlyAllocation);
            return closeable;
        }
        throw new IllegalStateException();
    }

    public boolean hasMoreRoutes() {
        boolean routeIsNotNull;
        synchronized (this.connectionPool) {
            routeIsNotNull = this.route != null;
        }
        if (routeIsNotNull || this.routeSelector.hasNext()) {
            return true;
        }
        return false;
    }

    public void setHttp2Indicator() {
        this.http2Indicator = true;
    }

    public String toString() {
        return this.address.toString();
    }
}
