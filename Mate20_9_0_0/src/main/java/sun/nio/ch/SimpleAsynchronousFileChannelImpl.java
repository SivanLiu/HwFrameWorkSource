package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SimpleAsynchronousFileChannelImpl extends AsynchronousFileChannelImpl {
    private static final FileDispatcher nd = new FileDispatcherImpl();
    private final NativeThreadSet threads = new NativeThreadSet(2);

    private static class DefaultExecutorHolder {
        static final ExecutorService defaultExecutor = ThreadPool.createDefault().executor();

        private DefaultExecutorHolder() {
        }
    }

    SimpleAsynchronousFileChannelImpl(FileDescriptor fdObj, boolean reading, boolean writing, ExecutorService executor) {
        super(fdObj, reading, writing, executor);
    }

    public static AsynchronousFileChannel open(FileDescriptor fdo, boolean reading, boolean writing, ThreadPool pool) {
        return new SimpleAsynchronousFileChannelImpl(fdo, reading, writing, pool == null ? DefaultExecutorHolder.defaultExecutor : pool.executor());
    }

    public void close() throws IOException {
        synchronized (this.fdObj) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            invalidateAllLocks();
            this.threads.signalAndWait();
            this.closeLock.writeLock().lock();
            this.closeLock.writeLock().unlock();
            nd.close(this.fdObj);
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:10:0x002a=Splitter:B:10:0x002a, B:18:0x003a=Splitter:B:18:0x003a} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long size() throws IOException {
        int ti = this.threads.add();
        boolean z = false;
        try {
            long n;
            begin();
            do {
                n = nd.size(this.fdObj);
                if (n != -3) {
                    break;
                }
            } while (isOpen());
            if (n >= 0) {
                z = true;
            }
            end(z);
            this.threads.remove(ti);
            return n;
        } catch (Throwable th) {
            this.threads.remove(ti);
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:22:0x004f=Splitter:B:22:0x004f, B:30:0x005f=Splitter:B:30:0x005f} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AsynchronousFileChannel truncate(long size) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (this.writing) {
            int ti = this.threads.add();
            boolean z = false;
            try {
                long n;
                begin();
                do {
                    n = nd.size(this.fdObj);
                    if (n != -3) {
                        break;
                    }
                } while (isOpen());
                if (size < n && isOpen()) {
                    do {
                        n = (long) nd.truncate(this.fdObj, size);
                        if (n != -3) {
                            break;
                        }
                    } while (isOpen());
                }
                if (n > 0) {
                    z = true;
                }
                end(z);
                this.threads.remove(ti);
                return this;
            } catch (Throwable th) {
                this.threads.remove(ti);
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:9:0x0022=Splitter:B:9:0x0022, B:16:0x0032=Splitter:B:16:0x0032} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void force(boolean metaData) throws IOException {
        int ti = this.threads.add();
        boolean z = false;
        try {
            int n;
            begin();
            do {
                n = nd.force(this.fdObj, metaData);
                if (n != -3) {
                    break;
                }
            } while (isOpen());
            if (n >= 0) {
                z = true;
            }
            end(z);
            this.threads.remove(ti);
        } catch (Throwable th) {
            this.threads.remove(ti);
        }
    }

    <A> Future<FileLock> implLock(long position, long size, boolean shared, A attachment, CompletionHandler<FileLock, ? super A> handler) {
        CompletionHandler completionHandler = handler;
        if (shared && !this.reading) {
            throw new NonReadableChannelException();
        } else if (shared || this.writing) {
            FileLockImpl fli = addToFileLockTable(position, size, shared);
            PendingFuture<FileLock, A> pendingFuture = null;
            if (fli == null) {
                Throwable exc = new ClosedChannelException();
                if (completionHandler == null) {
                    return CompletedFuture.withFailure(exc);
                }
                Invoker.invokeIndirectly(completionHandler, (Object) attachment, null, exc, this.executor);
                return null;
            }
            A a = attachment;
            if (completionHandler == null) {
                pendingFuture = new PendingFuture(this);
            }
            PendingFuture<FileLock, A> result = pendingFuture;
            final long j = position;
            final long j2 = size;
            final boolean z = shared;
            final FileLockImpl fileLockImpl = fli;
            final CompletionHandler completionHandler2 = completionHandler;
            final PendingFuture<FileLock, A> pendingFuture2 = result;
            a = attachment;
            AnonymousClass1 task = new Runnable() {
                /* JADX WARNING: Exception block dominator not found, dom blocks: [B:10:0x0038, B:17:0x0047] */
                /* JADX WARNING: Missing block: B:30:?, code skipped:
            r11.this$0.end();
     */
                /* JADX WARNING: Missing block: B:33:0x008a, code skipped:
            sun.nio.ch.SimpleAsynchronousFileChannelImpl.access$000(r11.this$0).remove(r1);
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void run() {
                    Throwable exc = null;
                    int ti = SimpleAsynchronousFileChannelImpl.this.threads.add();
                    try {
                        int n;
                        SimpleAsynchronousFileChannelImpl.this.begin();
                        do {
                            n = SimpleAsynchronousFileChannelImpl.nd.lock(SimpleAsynchronousFileChannelImpl.this.fdObj, true, j, j2, z);
                            if (n != 2) {
                                break;
                            }
                        } while (SimpleAsynchronousFileChannelImpl.this.isOpen());
                        if (n == 0 && SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                            SimpleAsynchronousFileChannelImpl.this.end();
                            SimpleAsynchronousFileChannelImpl.this.threads.remove(ti);
                            if (completionHandler2 == null) {
                                pendingFuture2.setResult(fileLockImpl, exc);
                                return;
                            } else {
                                Invoker.invokeUnchecked(completionHandler2, a, fileLockImpl, exc);
                                return;
                            }
                        }
                        throw new AsynchronousCloseException();
                    } catch (IOException e) {
                        Throwable x = e;
                        SimpleAsynchronousFileChannelImpl.this.removeFromFileLockTable(fileLockImpl);
                        if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                            x = new AsynchronousCloseException();
                        }
                        exc = x;
                        SimpleAsynchronousFileChannelImpl.this.end();
                    }
                }
            };
            boolean z2 = null;
            boolean executed = false;
            try {
                z2 = this.executor;
                z2.execute(task);
                z2 = true;
                return result;
            } finally {
                if (!(
/*
Method generation error in method: sun.nio.ch.SimpleAsynchronousFileChannelImpl.implLock(long, long, boolean, java.lang.Object, java.nio.channels.CompletionHandler):java.util.concurrent.Future<java.nio.channels.FileLock>, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r2_3 'executed' boolean) = (r2_2 'executed' boolean), (r0_13 'z2' boolean) in method: sun.nio.ch.SimpleAsynchronousFileChannelImpl.implLock(long, long, boolean, java.lang.Object, java.nio.channels.CompletionHandler):java.util.concurrent.Future<java.nio.channels.FileLock>, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:101)
	at jadx.core.codegen.ConditionGen.wrap(ConditionGen.java:94)
	at jadx.core.codegen.ConditionGen.addCompare(ConditionGen.java:116)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:56)
	at jadx.core.codegen.ConditionGen.wrap(ConditionGen.java:83)
	at jadx.core.codegen.ConditionGen.addNot(ConditionGen.java:143)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:64)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:45)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:118)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
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
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
	... 39 more

*/

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        if (shared && !this.reading) {
            throw new NonReadableChannelException();
        } else if (shared || this.writing) {
            FileLockImpl fli = addToFileLockTable(position, size, shared);
            if (fli != null) {
                int ti = this.threads.add();
                boolean gotLock = false;
                try {
                    int n;
                    begin();
                    do {
                        n = nd.lock(this.fdObj, false, position, size, shared);
                        if (n != 2) {
                            break;
                        }
                    } while (isOpen());
                    if (n == 0 && isOpen()) {
                        if (!true) {
                            removeFromFileLockTable(fli);
                        }
                        end();
                        this.threads.remove(ti);
                        return fli;
                    } else if (n == -1) {
                        if (!gotLock) {
                            removeFromFileLockTable(fli);
                        }
                        end();
                        this.threads.remove(ti);
                        return null;
                    } else if (n == 2) {
                        throw new AsynchronousCloseException();
                    } else {
                        throw new AssertionError();
                    }
                } catch (Throwable th) {
                    if (!gotLock) {
                        removeFromFileLockTable(fli);
                    }
                    end();
                    this.threads.remove(ti);
                    throw th;
                }
            }
            throw new ClosedChannelException();
        } else {
            throw new NonWritableChannelException();
        }
    }

    protected void implRelease(FileLockImpl fli) throws IOException {
        nd.release(this.fdObj, fli.position(), fli.size());
    }

    <A> Future<Integer> implRead(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (!this.reading) {
            throw new NonReadableChannelException();
        } else if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        } else {
            Future<Integer> future = null;
            if (!isOpen() || dst.remaining() == 0) {
                Throwable exc = isOpen() ? null : new ClosedChannelException();
                if (handler == null) {
                    return CompletedFuture.withResult(Integer.valueOf(0), exc);
                }
                Invoker.invokeIndirectly((CompletionHandler) handler, (Object) attachment, Integer.valueOf(0), exc, this.executor);
                return null;
            }
            if (handler == null) {
                future = new PendingFuture(this);
            }
            Future<Integer> result = future;
            final ByteBuffer byteBuffer = dst;
            final long j = position;
            final CompletionHandler<Integer, ? super A> completionHandler = handler;
            final Future<Integer> future2 = result;
            final A a = attachment;
            this.executor.execute(new Runnable() {
                public void run() {
                    int n = 0;
                    Throwable exc = null;
                    int ti = SimpleAsynchronousFileChannelImpl.this.threads.add();
                    try {
                        SimpleAsynchronousFileChannelImpl.this.begin();
                        do {
                            n = IOUtil.read(SimpleAsynchronousFileChannelImpl.this.fdObj, byteBuffer, j, SimpleAsynchronousFileChannelImpl.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (SimpleAsynchronousFileChannelImpl.this.isOpen());
                        if (n < 0) {
                            if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                throw new AsynchronousCloseException();
                            }
                        }
                    } catch (IOException e) {
                        Throwable x = e;
                        if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                            x = new AsynchronousCloseException();
                        }
                        exc = x;
                    } catch (Throwable th) {
                        SimpleAsynchronousFileChannelImpl.this.end();
                        SimpleAsynchronousFileChannelImpl.this.threads.remove(ti);
                    }
                    SimpleAsynchronousFileChannelImpl.this.end();
                    SimpleAsynchronousFileChannelImpl.this.threads.remove(ti);
                    if (completionHandler == null) {
                        future2.setResult(Integer.valueOf(n), exc);
                    } else {
                        Invoker.invokeUnchecked(completionHandler, a, Integer.valueOf(n), exc);
                    }
                }
            });
            return result;
        }
    }

    <A> Future<Integer> implWrite(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (this.writing) {
            Future<Integer> future = null;
            if (!isOpen() || src.remaining() == 0) {
                Throwable exc = isOpen() ? null : new ClosedChannelException();
                if (handler == null) {
                    return CompletedFuture.withResult(Integer.valueOf(0), exc);
                }
                Invoker.invokeIndirectly((CompletionHandler) handler, (Object) attachment, Integer.valueOf(0), exc, this.executor);
                return null;
            }
            if (handler == null) {
                future = new PendingFuture(this);
            }
            Future<Integer> result = future;
            final ByteBuffer byteBuffer = src;
            final long j = position;
            final CompletionHandler<Integer, ? super A> completionHandler = handler;
            final Future<Integer> future2 = result;
            final A a = attachment;
            this.executor.execute(new Runnable() {
                public void run() {
                    int n = 0;
                    Throwable exc = null;
                    int ti = SimpleAsynchronousFileChannelImpl.this.threads.add();
                    try {
                        SimpleAsynchronousFileChannelImpl.this.begin();
                        do {
                            n = IOUtil.write(SimpleAsynchronousFileChannelImpl.this.fdObj, byteBuffer, j, SimpleAsynchronousFileChannelImpl.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (SimpleAsynchronousFileChannelImpl.this.isOpen());
                        if (n < 0) {
                            if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                                throw new AsynchronousCloseException();
                            }
                        }
                    } catch (IOException e) {
                        Throwable x = e;
                        if (!SimpleAsynchronousFileChannelImpl.this.isOpen()) {
                            x = new AsynchronousCloseException();
                        }
                        exc = x;
                    } catch (Throwable th) {
                        SimpleAsynchronousFileChannelImpl.this.end();
                        SimpleAsynchronousFileChannelImpl.this.threads.remove(ti);
                    }
                    SimpleAsynchronousFileChannelImpl.this.end();
                    SimpleAsynchronousFileChannelImpl.this.threads.remove(ti);
                    if (completionHandler == null) {
                        future2.setResult(Integer.valueOf(n), exc);
                    } else {
                        Invoker.invokeUnchecked(completionHandler, a, Integer.valueOf(n), exc);
                    }
                }
            });
            return result;
        } else {
            throw new NonWritableChannelException();
        }
    }
}
