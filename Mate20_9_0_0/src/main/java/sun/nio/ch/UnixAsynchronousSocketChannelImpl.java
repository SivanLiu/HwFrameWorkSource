package sun.nio.ch;

import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.AccessController;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import sun.net.NetHooks;
import sun.security.action.GetPropertyAction;

class UnixAsynchronousSocketChannelImpl extends AsynchronousSocketChannelImpl implements PollableChannel {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final boolean disableSynchronousRead;
    private static final NativeDispatcher nd = new SocketDispatcher();
    private Object connectAttachment;
    private PendingFuture<Void, Object> connectFuture;
    private CompletionHandler<Void, Object> connectHandler;
    private boolean connectPending;
    private final int fdVal;
    private final CloseGuard guard = CloseGuard.get();
    private boolean isGatheringWrite;
    private boolean isScatteringRead;
    private SocketAddress pendingRemote;
    private final Port port;
    private Object readAttachment;
    private ByteBuffer readBuffer;
    private ByteBuffer[] readBuffers;
    private PendingFuture<Number, Object> readFuture;
    private CompletionHandler<Number, Object> readHandler;
    private boolean readPending;
    private Runnable readTimeoutTask = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0030, code skipped:
            r6.this$0.enableReading(true);
            r3 = new java.nio.channels.InterruptedByTimeoutException();
     */
        /* JADX WARNING: Missing block: B:10:0x003b, code skipped:
            if (r0 != null) goto L_0x0041;
     */
        /* JADX WARNING: Missing block: B:11:0x003d, code skipped:
            r2.setFailure(r3);
     */
        /* JADX WARNING: Missing block: B:12:0x0041, code skipped:
            sun.nio.ch.Invoker.invokeIndirectly(r6.this$0, r0, r1, null, r3);
     */
        /* JADX WARNING: Missing block: B:13:0x0047, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (UnixAsynchronousSocketChannelImpl.this.updateLock) {
                if (UnixAsynchronousSocketChannelImpl.this.readPending) {
                    UnixAsynchronousSocketChannelImpl.this.readPending = false;
                    CompletionHandler handler = UnixAsynchronousSocketChannelImpl.this.readHandler;
                    Object att = UnixAsynchronousSocketChannelImpl.this.readAttachment;
                    PendingFuture<Number, Object> future = UnixAsynchronousSocketChannelImpl.this.readFuture;
                }
            }
        }
    };
    private Future<?> readTimer;
    private final Object updateLock = new Object();
    private Object writeAttachment;
    private ByteBuffer writeBuffer;
    private ByteBuffer[] writeBuffers;
    private PendingFuture<Number, Object> writeFuture;
    private CompletionHandler<Number, Object> writeHandler;
    private boolean writePending;
    private Runnable writeTimeoutTask = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0030, code skipped:
            r6.this$0.enableWriting(true);
            r3 = new java.nio.channels.InterruptedByTimeoutException();
     */
        /* JADX WARNING: Missing block: B:10:0x003b, code skipped:
            if (r0 == null) goto L_0x0044;
     */
        /* JADX WARNING: Missing block: B:11:0x003d, code skipped:
            sun.nio.ch.Invoker.invokeIndirectly(r6.this$0, r0, r1, null, r3);
     */
        /* JADX WARNING: Missing block: B:12:0x0044, code skipped:
            r2.setFailure(r3);
     */
        /* JADX WARNING: Missing block: B:13:0x0047, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (UnixAsynchronousSocketChannelImpl.this.updateLock) {
                if (UnixAsynchronousSocketChannelImpl.this.writePending) {
                    UnixAsynchronousSocketChannelImpl.this.writePending = false;
                    CompletionHandler handler = UnixAsynchronousSocketChannelImpl.this.writeHandler;
                    Object att = UnixAsynchronousSocketChannelImpl.this.writeAttachment;
                    PendingFuture<Number, Object> future = UnixAsynchronousSocketChannelImpl.this.writeFuture;
                }
            }
        }
    };
    private Future<?> writeTimer;

    private enum OpType {
        CONNECT,
        READ,
        WRITE
    }

    private static native void checkConnect(int i) throws IOException;

    static {
        String propValue = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSynchronousRead", "false"));
        disableSynchronousRead = propValue.length() == 0 ? true : Boolean.valueOf(propValue).booleanValue();
    }

    UnixAsynchronousSocketChannelImpl(Port port) throws IOException {
        super(port);
        try {
            IOUtil.configureBlocking(this.fd, false);
            this.port = port;
            this.fdVal = IOUtil.fdVal(this.fd);
            port.register(this.fdVal, this);
            this.guard.open("close");
        } catch (IOException x) {
            nd.close(this.fd);
            throw x;
        }
    }

    UnixAsynchronousSocketChannelImpl(Port port, FileDescriptor fd, InetSocketAddress remote) throws IOException {
        super(port, fd, remote);
        this.fdVal = IOUtil.fdVal(fd);
        IOUtil.configureBlocking(fd, false);
        try {
            port.register(this.fdVal, this);
            this.port = port;
            this.guard.open("close");
        } catch (ShutdownChannelGroupException x) {
            throw new IOException(x);
        }
    }

    public AsynchronousChannelGroupImpl group() {
        return this.port;
    }

    private void updateEvents() {
        int events = 0;
        if (this.readPending) {
            events = 0 | Net.POLLIN;
        }
        if (this.connectPending || this.writePending) {
            events |= Net.POLLOUT;
        }
        if (events != 0) {
            this.port.startPoll(this.fdVal, events);
        }
    }

    private void lockAndUpdateEvents() {
        synchronized (this.updateLock) {
            updateEvents();
        }
    }

    private void finish(boolean mayInvokeDirect, boolean readable, boolean writable) {
        boolean finishRead = false;
        boolean finishWrite = false;
        boolean finishConnect = false;
        synchronized (this.updateLock) {
            if (readable) {
                try {
                    if (this.readPending) {
                        this.readPending = false;
                        finishRead = true;
                    }
                } finally {
                }
            }
            if (writable) {
                if (this.writePending) {
                    this.writePending = false;
                    finishWrite = true;
                } else if (this.connectPending) {
                    this.connectPending = false;
                    finishConnect = true;
                }
            }
        }
        if (finishRead) {
            if (finishWrite) {
                finishWrite(false);
            }
            finishRead(mayInvokeDirect);
            return;
        }
        if (finishWrite) {
            finishWrite(mayInvokeDirect);
        }
        if (finishConnect) {
            finishConnect(mayInvokeDirect);
        }
    }

    public void onEvent(int events, boolean mayInvokeDirect) {
        boolean writable = false;
        boolean readable = (Net.POLLIN & events) > 0;
        if ((Net.POLLOUT & events) > 0) {
            writable = true;
        }
        if (((Net.POLLERR | Net.POLLHUP) & events) > 0) {
            readable = true;
            writable = true;
        }
        finish(mayInvokeDirect, readable, writable);
    }

    void implClose() throws IOException {
        this.guard.close();
        this.port.unregister(this.fdVal);
        nd.close(this.fd);
        finish(false, true, true);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public void onCancel(PendingFuture<?, ?> task) {
        if (task.getContext() == OpType.CONNECT) {
            killConnect();
        }
        if (task.getContext() == OpType.READ) {
            killReading();
        }
        if (task.getContext() == OpType.WRITE) {
            killWriting();
        }
    }

    private void setConnected() throws IOException {
        synchronized (this.stateLock) {
            this.state = 2;
            this.localAddress = Net.localAddress(this.fd);
            this.remoteAddress = (InetSocketAddress) this.pendingRemote;
        }
    }

    private void finishConnect(boolean mayInvokeDirect) {
        Throwable x;
        Throwable e = null;
        try {
            begin();
            checkConnect(this.fdVal);
            setConnected();
        } catch (Throwable th) {
            end();
        }
        end();
        if (e != null) {
            try {
                close();
            } catch (Throwable x2) {
                e.addSuppressed(x2);
            }
        }
        CompletionHandler handler = this.connectHandler;
        Object att = this.connectAttachment;
        PendingFuture<Void, Object> future = this.connectFuture;
        if (handler == null) {
            future.setResult(null, e);
        } else if (mayInvokeDirect) {
            Invoker.invokeUnchecked(handler, att, null, e);
        } else {
            Invoker.invokeIndirectly((AsynchronousChannel) this, handler, att, null, e);
        }
    }

    <A> Future<Void> implConnect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        Throwable suppressed;
        if (isOpen()) {
            boolean notifyBeforeTcpConnect;
            InetSocketAddress isa = Net.checkAddress(remote);
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
            }
            synchronized (this.stateLock) {
                if (this.state == 2) {
                    throw new AlreadyConnectedException();
                } else if (this.state != 1) {
                    this.state = 1;
                    this.pendingRemote = remote;
                    notifyBeforeTcpConnect = this.localAddress == null;
                } else {
                    throw new ConnectionPendingException();
                }
            }
            Throwable e = null;
            try {
                begin();
                if (notifyBeforeTcpConnect) {
                    NetHooks.beforeTcpConnect(this.fd, isa.getAddress(), isa.getPort());
                }
                if (Net.connect(this.fd, isa.getAddress(), isa.getPort()) == -2) {
                    PendingFuture<Void, A> result = null;
                    synchronized (this.updateLock) {
                        if (handler == null) {
                            result = new PendingFuture(this, OpType.CONNECT);
                            this.connectFuture = result;
                        } else {
                            this.connectHandler = handler;
                            this.connectAttachment = attachment;
                        }
                        this.connectPending = true;
                        updateEvents();
                    }
                    end();
                    return result;
                }
                setConnected();
                end();
                if (e != null) {
                    try {
                        close();
                    } catch (Throwable suppressed2) {
                        e.addSuppressed(suppressed2);
                    }
                }
                if (handler == null) {
                    return CompletedFuture.withResult(null, e);
                }
                Invoker.invoke(this, handler, attachment, null, e);
                return null;
            } catch (Throwable th) {
                suppressed2 = th;
                try {
                    if (suppressed2 instanceof ClosedChannelException) {
                        suppressed2 = new AsynchronousCloseException();
                    }
                    e = suppressed2;
                } catch (Throwable th2) {
                    end();
                }
            }
        } else {
            Throwable e2 = new ClosedChannelException();
            if (handler == null) {
                return CompletedFuture.withFailure(e2);
            }
            Invoker.invoke(this, handler, attachment, null, e2);
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:26:0x004e, code skipped:
            if ((null instanceof java.nio.channels.AsynchronousCloseException) == false) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:35:0x0064, code skipped:
            if ((r1 instanceof java.nio.channels.AsynchronousCloseException) != false) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:36:0x0066, code skipped:
            lockAndUpdateEvents();
     */
    /* JADX WARNING: Missing block: B:37:0x0069, code skipped:
            end();
     */
    /* JADX WARNING: Missing block: B:38:0x006d, code skipped:
            if (r6 == null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:39:0x006f, code skipped:
            r6.cancel(false);
     */
    /* JADX WARNING: Missing block: B:40:0x0073, code skipped:
            if (r1 == null) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:41:0x0076, code skipped:
            if (r2 == false) goto L_0x007e;
     */
    /* JADX WARNING: Missing block: B:42:0x0078, code skipped:
            r7 = java.lang.Long.valueOf((long) r0);
     */
    /* JADX WARNING: Missing block: B:43:0x007e, code skipped:
            r7 = java.lang.Integer.valueOf(r0);
     */
    /* JADX WARNING: Missing block: B:44:0x0082, code skipped:
            if (r3 != null) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:45:0x0084, code skipped:
            r5.setResult(r7, r1);
     */
    /* JADX WARNING: Missing block: B:46:0x0088, code skipped:
            if (r14 == false) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:47:0x008a, code skipped:
            sun.nio.ch.Invoker.invokeUnchecked(r3, r4, r7, r1);
     */
    /* JADX WARNING: Missing block: B:48:0x008e, code skipped:
            sun.nio.ch.Invoker.invokeIndirectly((java.nio.channels.AsynchronousChannel) r13, r3, r4, r7, r1);
     */
    /* JADX WARNING: Missing block: B:49:0x0091, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void finishRead(boolean mayInvokeDirect) {
        int n = -1;
        Throwable exc = null;
        boolean scattering = this.isScatteringRead;
        CompletionHandler handler = this.readHandler;
        Object att = this.readAttachment;
        PendingFuture<Number, Object> future = this.readFuture;
        Future<?> timeout = this.readTimer;
        Object result = null;
        try {
            begin();
            if (scattering) {
                n = (int) IOUtil.read(this.fd, this.readBuffers, nd);
            } else {
                n = IOUtil.read(this.fd, this.readBuffer, -1, nd);
            }
            if (n == -2) {
                synchronized (this.updateLock) {
                    this.readPending = true;
                }
                if (!(null instanceof AsynchronousCloseException)) {
                    lockAndUpdateEvents();
                }
                end();
                return;
            }
            this.readBuffer = null;
            this.readBuffers = null;
            this.readAttachment = null;
            enableReading();
        } catch (Throwable th) {
            Throwable x = th;
            try {
                enableReading();
                if (x instanceof ClosedChannelException) {
                    x = new AsynchronousCloseException();
                }
                exc = x;
            } catch (Throwable th2) {
                if (!(null instanceof AsynchronousCloseException)) {
                    lockAndUpdateEvents();
                }
                end();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:89:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x012c  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x0136  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00fe A:{Catch:{ all -> 0x0131 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0107  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x012c  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x0136  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00fe A:{Catch:{ all -> 0x0131 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0107  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x012c  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x0136  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00fe A:{Catch:{ all -> 0x0131 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0107  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x012c  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x0136  */
    /* JADX WARNING: Missing block: B:54:0x00c3, code skipped:
            if (true != false) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:55:0x00c5, code skipped:
            enableReading();
     */
    /* JADX WARNING: Missing block: B:56:0x00c8, code skipped:
            end();
     */
    /* JADX WARNING: Missing block: B:57:0x00cb, code skipped:
            return r12;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    <V extends Number, A> Future<V> implRead(boolean isScatteringRead, ByteBuffer dst, ByteBuffer[] dsts, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        int n;
        Throwable x;
        TimeUnit timeUnit;
        Throwable exc;
        Number valueOf;
        boolean z = isScatteringRead;
        ByteBuffer byteBuffer = dst;
        ByteBuffer[] byteBufferArr = dsts;
        long j = timeout;
        Object obj = attachment;
        CompletionHandler completionHandler = handler;
        GroupAndInvokeCount myGroupAndInvokeCount = null;
        boolean invokeDirect = false;
        boolean attemptRead = false;
        if (!disableSynchronousRead) {
            if (completionHandler == null) {
                attemptRead = true;
            } else {
                myGroupAndInvokeCount = Invoker.getGroupAndInvokeCount();
                invokeDirect = Invoker.mayInvokeDirect(myGroupAndInvokeCount, this.port);
                boolean z2 = invokeDirect || !this.port.isFixedThreadPool();
                attemptRead = z2;
            }
        }
        GroupAndInvokeCount myGroupAndInvokeCount2 = myGroupAndInvokeCount;
        int n2 = -2;
        Throwable exc2;
        Object result;
        try {
            begin();
            if (!attemptRead) {
                n = -2;
                exc2 = null;
            } else if (z) {
                try {
                    n = -2;
                    exc2 = null;
                } catch (Throwable th) {
                    x = th;
                    n = -2;
                    exc2 = null;
                    timeUnit = unit;
                    if (null == null) {
                    }
                    end();
                    throw x;
                }
                try {
                    n2 = (int) IOUtil.read(this.fd, byteBufferArr, nd);
                } catch (Throwable th2) {
                    x = th2;
                    timeUnit = unit;
                    if (null == null) {
                    }
                    end();
                    throw x;
                }
            } else {
                n = -2;
                exc2 = null;
                n2 = IOUtil.read(this.fd, byteBuffer, -1, nd);
            }
            if (n2 == -2) {
                PendingFuture<V, A> result2 = null;
                try {
                    synchronized (this.updateLock) {
                        try {
                            this.isScatteringRead = z;
                            this.readBuffer = byteBuffer;
                            this.readBuffers = byteBufferArr;
                            if (completionHandler == null) {
                                this.readHandler = null;
                                result2 = new PendingFuture(this, OpType.READ);
                                this.readFuture = result2;
                                this.readAttachment = null;
                            } else {
                                this.readHandler = completionHandler;
                                this.readAttachment = obj;
                                this.readFuture = null;
                            }
                            if (j > 0) {
                                try {
                                    this.readTimer = this.port.schedule(this.readTimeoutTask, j, unit);
                                } catch (Throwable th3) {
                                    x = th3;
                                    try {
                                        throw x;
                                    } catch (Throwable th4) {
                                        x = th4;
                                    }
                                }
                            } else {
                                timeUnit = unit;
                            }
                            this.readPending = true;
                            updateEvents();
                        } catch (Throwable th5) {
                            x = th5;
                            timeUnit = unit;
                            throw x;
                        }
                    }
                } catch (Throwable th6) {
                    x = th6;
                    timeUnit = unit;
                    if (null == null) {
                    }
                    end();
                    throw x;
                }
            }
            timeUnit = unit;
            if (null == null) {
                enableReading();
            }
            end();
            exc = exc2;
            valueOf = exc != null ? null : z ? Long.valueOf((long) n2) : Integer.valueOf(n2);
            result = valueOf;
            if (completionHandler == null) {
                return CompletedFuture.withResult(result, exc);
            }
            if (invokeDirect) {
                Invoker.invokeDirect(myGroupAndInvokeCount2, completionHandler, obj, result, exc);
            } else {
                Invoker.invokeIndirectly(this, completionHandler, obj, result, exc);
            }
            return null;
        } catch (Throwable th7) {
            x = th7;
            timeUnit = unit;
            n = -2;
            exc2 = null;
            if (null == null) {
            }
            end();
            throw x;
        }
    }

    /* JADX WARNING: Missing block: B:26:0x004e, code skipped:
            if ((null instanceof java.nio.channels.AsynchronousCloseException) == false) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:35:0x0064, code skipped:
            if ((r1 instanceof java.nio.channels.AsynchronousCloseException) != false) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:36:0x0066, code skipped:
            lockAndUpdateEvents();
     */
    /* JADX WARNING: Missing block: B:37:0x0069, code skipped:
            end();
     */
    /* JADX WARNING: Missing block: B:38:0x006d, code skipped:
            if (r6 == null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:39:0x006f, code skipped:
            r6.cancel(false);
     */
    /* JADX WARNING: Missing block: B:40:0x0073, code skipped:
            if (r1 == null) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:41:0x0076, code skipped:
            if (r2 == false) goto L_0x007e;
     */
    /* JADX WARNING: Missing block: B:42:0x0078, code skipped:
            r7 = java.lang.Long.valueOf((long) r0);
     */
    /* JADX WARNING: Missing block: B:43:0x007e, code skipped:
            r7 = java.lang.Integer.valueOf(r0);
     */
    /* JADX WARNING: Missing block: B:44:0x0082, code skipped:
            if (r3 != null) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:45:0x0084, code skipped:
            r5.setResult(r7, r1);
     */
    /* JADX WARNING: Missing block: B:46:0x0088, code skipped:
            if (r14 == false) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:47:0x008a, code skipped:
            sun.nio.ch.Invoker.invokeUnchecked(r3, r4, r7, r1);
     */
    /* JADX WARNING: Missing block: B:48:0x008e, code skipped:
            sun.nio.ch.Invoker.invokeIndirectly((java.nio.channels.AsynchronousChannel) r13, r3, r4, r7, r1);
     */
    /* JADX WARNING: Missing block: B:49:0x0091, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void finishWrite(boolean mayInvokeDirect) {
        int n = -1;
        Throwable exc = null;
        boolean gathering = this.isGatheringWrite;
        CompletionHandler handler = this.writeHandler;
        Object att = this.writeAttachment;
        PendingFuture<Number, Object> future = this.writeFuture;
        Future<?> timer = this.writeTimer;
        Object result = null;
        try {
            begin();
            if (gathering) {
                n = (int) IOUtil.write(this.fd, this.writeBuffers, nd);
            } else {
                n = IOUtil.write(this.fd, this.writeBuffer, -1, nd);
            }
            if (n == -2) {
                synchronized (this.updateLock) {
                    this.writePending = true;
                }
                if (!(null instanceof AsynchronousCloseException)) {
                    lockAndUpdateEvents();
                }
                end();
                return;
            }
            this.writeBuffer = null;
            this.writeBuffers = null;
            this.writeAttachment = null;
            enableWriting();
        } catch (Throwable th) {
            Throwable x = th;
            try {
                enableWriting();
                if (x instanceof ClosedChannelException) {
                    x = new AsynchronousCloseException();
                }
                exc = x;
            } catch (Throwable th2) {
                if (!(null instanceof AsynchronousCloseException)) {
                    lockAndUpdateEvents();
                }
                end();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:77:0x00f0 A:{Catch:{ all -> 0x011d }} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0122  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0122  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00f0 A:{Catch:{ all -> 0x011d }} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0122  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00f0 A:{Catch:{ all -> 0x011d }} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0122  */
    /* JADX WARNING: Missing block: B:50:0x00b8, code skipped:
            if (true != false) goto L_0x00bd;
     */
    /* JADX WARNING: Missing block: B:51:0x00ba, code skipped:
            enableWriting();
     */
    /* JADX WARNING: Missing block: B:52:0x00bd, code skipped:
            end();
     */
    /* JADX WARNING: Missing block: B:53:0x00c0, code skipped:
            return r12;
     */
    /* JADX WARNING: Missing block: B:67:0x00d4, code skipped:
            if (null == null) goto L_0x00d6;
     */
    /* JADX WARNING: Missing block: B:68:0x00d6, code skipped:
            enableWriting();
     */
    /* JADX WARNING: Missing block: B:69:0x00d9, code skipped:
            end();
     */
    /* JADX WARNING: Missing block: B:80:0x00f7, code skipped:
            if (null == null) goto L_0x00d6;
     */
    /* JADX WARNING: Missing block: B:81:0x00fa, code skipped:
            if (r14 == null) goto L_0x00fe;
     */
    /* JADX WARNING: Missing block: B:82:0x00fc, code skipped:
            r15 = null;
     */
    /* JADX WARNING: Missing block: B:83:0x00fe, code skipped:
            if (r2 == false) goto L_0x0106;
     */
    /* JADX WARNING: Missing block: B:84:0x0100, code skipped:
            r15 = java.lang.Long.valueOf((long) r13);
     */
    /* JADX WARNING: Missing block: B:85:0x0106, code skipped:
            r15 = java.lang.Integer.valueOf(r13);
     */
    /* JADX WARNING: Missing block: B:86:0x010a, code skipped:
            r0 = r15;
     */
    /* JADX WARNING: Missing block: B:87:0x010b, code skipped:
            if (r8 == null) goto L_0x0118;
     */
    /* JADX WARNING: Missing block: B:88:0x010d, code skipped:
            if (r10 == false) goto L_0x0113;
     */
    /* JADX WARNING: Missing block: B:89:0x010f, code skipped:
            sun.nio.ch.Invoker.invokeDirect(r9, r8, r7, r0, r14);
     */
    /* JADX WARNING: Missing block: B:90:0x0113, code skipped:
            sun.nio.ch.Invoker.invokeIndirectly(r1, r8, r7, r0, r14);
     */
    /* JADX WARNING: Missing block: B:92:0x0117, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:94:0x011c, code skipped:
            return sun.nio.ch.CompletedFuture.withResult(r0, r14);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    <V extends Number, A> Future<V> implWrite(boolean isGatheringWrite, ByteBuffer src, ByteBuffer[] srcs, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        int i;
        Throwable x;
        TimeUnit timeUnit;
        boolean z;
        boolean z2 = isGatheringWrite;
        ByteBuffer byteBuffer = src;
        ByteBuffer[] byteBufferArr = srcs;
        long j = timeout;
        Object obj = attachment;
        CompletionHandler completionHandler = handler;
        GroupAndInvokeCount myGroupAndInvokeCount = Invoker.getGroupAndInvokeCount();
        boolean invokeDirect = Invoker.mayInvokeDirect(myGroupAndInvokeCount, this.port);
        boolean attemptWrite = completionHandler == null || invokeDirect || !this.port.isFixedThreadPool();
        int n = -2;
        Throwable exc = null;
        try {
            begin();
            if (!attemptWrite) {
                i = -2;
            } else if (z2) {
                try {
                    i = -2;
                    try {
                        n = (int) IOUtil.write(this.fd, byteBufferArr, nd);
                    } catch (Throwable th) {
                        x = th;
                        timeUnit = unit;
                        if (null == null) {
                            enableWriting();
                        }
                        end();
                        throw x;
                    }
                } catch (Throwable th2) {
                    x = th2;
                    z = attemptWrite;
                    i = -2;
                    timeUnit = unit;
                    if (null == null) {
                    }
                    end();
                    throw x;
                }
            } else {
                z = attemptWrite;
                i = -2;
                n = IOUtil.write(this.fd, byteBuffer, -1, nd);
            }
            if (n == -2) {
                attemptWrite = false;
                try {
                    synchronized (this.updateLock) {
                        try {
                            this.isGatheringWrite = z2;
                            this.writeBuffer = byteBuffer;
                            this.writeBuffers = byteBufferArr;
                            if (completionHandler == null) {
                                this.writeHandler = null;
                                attemptWrite = new PendingFuture(this, OpType.WRITE);
                                this.writeFuture = attemptWrite;
                                this.writeAttachment = null;
                            } else {
                                this.writeHandler = completionHandler;
                                this.writeAttachment = obj;
                                this.writeFuture = null;
                            }
                            if (j > 0) {
                                try {
                                    this.writeTimer = this.port.schedule(this.writeTimeoutTask, j, unit);
                                } catch (Throwable th3) {
                                    x = th3;
                                    try {
                                        throw x;
                                    } catch (Throwable th4) {
                                        x = th4;
                                    }
                                }
                            } else {
                                timeUnit = unit;
                            }
                            this.writePending = true;
                            updateEvents();
                        } catch (Throwable th5) {
                            x = th5;
                            timeUnit = unit;
                            throw x;
                        }
                    }
                } catch (Throwable th6) {
                    x = th6;
                    timeUnit = unit;
                    if (null == null) {
                    }
                    end();
                    throw x;
                }
            }
            timeUnit = unit;
        } catch (Throwable th7) {
            x = th7;
            timeUnit = unit;
            z = attemptWrite;
            i = -2;
            if (null == null) {
            }
            end();
            throw x;
        }
    }
}
