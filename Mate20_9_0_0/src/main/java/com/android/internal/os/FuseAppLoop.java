package com.android.internal.os;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ProxyFileDescriptorCallback;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

public class FuseAppLoop implements Callback {
    private static final int ARGS_POOL_SIZE = 50;
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int FUSE_FSYNC = 20;
    private static final int FUSE_GETATTR = 3;
    private static final int FUSE_LOOKUP = 1;
    private static final int FUSE_MAX_WRITE = 131072;
    private static final int FUSE_OK = 0;
    private static final int FUSE_OPEN = 14;
    private static final int FUSE_READ = 15;
    private static final int FUSE_RELEASE = 18;
    private static final int FUSE_WRITE = 16;
    private static final int MIN_INODE = 2;
    public static final int ROOT_INODE = 1;
    private static final String TAG = "FuseAppLoop";
    private static final ThreadFactory sDefaultThreadFactory = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            return new Thread(r, FuseAppLoop.TAG);
        }
    };
    @GuardedBy("mLock")
    private final LinkedList<Args> mArgsPool = new LinkedList();
    @GuardedBy("mLock")
    private final BytesMap mBytesMap = new BytesMap();
    @GuardedBy("mLock")
    private final SparseArray<CallbackEntry> mCallbackMap = new SparseArray();
    @GuardedBy("mLock")
    private long mInstance;
    private final Object mLock = new Object();
    private final int mMountPointId;
    @GuardedBy("mLock")
    private int mNextInode = 2;
    private final Thread mThread;

    private static class Args {
        byte[] data;
        CallbackEntry entry;
        long inode;
        long offset;
        int size;
        long unique;

        private Args() {
        }

        /* synthetic */ Args(AnonymousClass1 x0) {
            this();
        }
    }

    private static class BytesMap {
        final Map<Long, BytesMapEntry> mEntries;

        private BytesMap() {
            this.mEntries = new HashMap();
        }

        /* synthetic */ BytesMap(AnonymousClass1 x0) {
            this();
        }

        byte[] startUsing(long threadId) {
            BytesMapEntry entry = (BytesMapEntry) this.mEntries.get(Long.valueOf(threadId));
            if (entry == null) {
                entry = new BytesMapEntry();
                this.mEntries.put(Long.valueOf(threadId), entry);
            }
            entry.counter++;
            return entry.bytes;
        }

        void stopUsing(long threadId) {
            BytesMapEntry entry = (BytesMapEntry) this.mEntries.get(Long.valueOf(threadId));
            Preconditions.checkNotNull(entry);
            entry.counter--;
            if (entry.counter <= 0) {
                this.mEntries.remove(Long.valueOf(threadId));
            }
        }

        void clear() {
            this.mEntries.clear();
        }
    }

    private static class BytesMapEntry {
        byte[] bytes;
        int counter;

        private BytesMapEntry() {
            this.counter = 0;
            this.bytes = new byte[131072];
        }

        /* synthetic */ BytesMapEntry(AnonymousClass1 x0) {
            this();
        }
    }

    private static class CallbackEntry {
        final ProxyFileDescriptorCallback callback;
        final Handler handler;
        boolean opened;

        CallbackEntry(ProxyFileDescriptorCallback callback, Handler handler) {
            this.callback = (ProxyFileDescriptorCallback) Preconditions.checkNotNull(callback);
            this.handler = (Handler) Preconditions.checkNotNull(handler);
        }

        long getThreadId() {
            return this.handler.getLooper().getThread().getId();
        }
    }

    public static class UnmountedException extends Exception {
    }

    native void native_delete(long j);

    native long native_new(int i);

    native void native_replyGetAttr(long j, long j2, long j3, long j4);

    native void native_replyLookup(long j, long j2, long j3, long j4);

    native void native_replyOpen(long j, long j2, long j3);

    native void native_replyRead(long j, long j2, int i, byte[] bArr);

    native void native_replySimple(long j, long j2, int i);

    native void native_replyWrite(long j, long j2, int i);

    native void native_start(long j);

    public FuseAppLoop(int mountPointId, ParcelFileDescriptor fd, ThreadFactory factory) {
        this.mMountPointId = mountPointId;
        if (factory == null) {
            factory = sDefaultThreadFactory;
        }
        this.mInstance = native_new(fd.detachFd());
        this.mThread = factory.newThread(new -$$Lambda$FuseAppLoop$e9Yru2f_btesWlxIgerkPnHibpg(this));
        this.mThread.start();
    }

    public static /* synthetic */ void lambda$new$0(FuseAppLoop fuseAppLoop) {
        fuseAppLoop.native_start(fuseAppLoop.mInstance);
        synchronized (fuseAppLoop.mLock) {
            fuseAppLoop.native_delete(fuseAppLoop.mInstance);
            fuseAppLoop.mInstance = 0;
            fuseAppLoop.mBytesMap.clear();
        }
    }

    public int registerCallback(ProxyFileDescriptorCallback callback, Handler handler) throws FuseUnavailableMountException {
        int id;
        synchronized (this.mLock) {
            Preconditions.checkNotNull(callback);
            Preconditions.checkNotNull(handler);
            boolean z = false;
            Preconditions.checkState(this.mCallbackMap.size() < 2147483645, "Too many opened files.");
            if (Thread.currentThread().getId() != handler.getLooper().getThread().getId()) {
                z = true;
            }
            Preconditions.checkArgument(z, "Handler must be different from the current thread");
            if (this.mInstance != 0) {
                do {
                    id = this.mNextInode;
                    this.mNextInode++;
                    if (this.mNextInode < 0) {
                        this.mNextInode = 2;
                    }
                } while (this.mCallbackMap.get(id) != null);
                this.mCallbackMap.put(id, new CallbackEntry(callback, new Handler(handler.getLooper(), this)));
            } else {
                throw new FuseUnavailableMountException(this.mMountPointId);
            }
        }
        return id;
    }

    public void unregisterCallback(int id) {
        synchronized (this.mLock) {
            this.mCallbackMap.remove(id);
        }
    }

    public int getMountPointId() {
        return this.mMountPointId;
    }

    /* JADX WARNING: Removed duplicated region for block: B:149:0x01c9 A:{SYNTHETIC, Splitter:B:149:0x01c9} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x01c9 A:{SYNTHETIC, Splitter:B:149:0x01c9} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x01c9 A:{SYNTHETIC, Splitter:B:149:0x01c9} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x01c9 A:{SYNTHETIC, Splitter:B:149:0x01c9} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x01c9 A:{SYNTHETIC, Splitter:B:149:0x01c9} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x01c9 A:{SYNTHETIC, Splitter:B:149:0x01c9} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean handleMessage(Message msg) {
        boolean z;
        long unique;
        Throwable th;
        Exception e;
        Exception error;
        Message message = msg;
        Args args = message.obj;
        CallbackEntry entry = args.entry;
        long inode = args.inode;
        long unique2 = args.unique;
        int size = args.size;
        long offset = args.offset;
        byte[] data = args.data;
        int i;
        long inode2;
        byte[] data2;
        long j;
        try {
            int i2 = message.what;
            long j2 = 0;
            Object obj;
            Object obj2;
            if (i2 == 1) {
                z = true;
                i = size;
                unique = unique2;
                inode2 = inode;
                inode = offset;
                unique2 = entry.callback.onGetSize();
                Object obj3 = this.mLock;
                synchronized (obj3) {
                    try {
                        if (this.mInstance != 0) {
                            j2 = obj3;
                            native_replyLookup(this.mInstance, unique, inode2, unique2);
                        } else {
                            j2 = obj3;
                        }
                        recycleLocked(args);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } else if (i2 != 3) {
                if (i2 == 18) {
                    z = true;
                    i = size;
                    inode2 = inode;
                    inode = offset;
                    entry.callback.onRelease();
                    synchronized (this.mLock) {
                        if (this.mInstance != 0) {
                            native_replySimple(this.mInstance, unique2, 0);
                        }
                        this.mBytesMap.stopUsing(entry.getThreadId());
                        recycleLocked(args);
                    }
                } else if (i2 != 20) {
                    int readSize;
                    switch (i2) {
                        case 15:
                            data2 = data;
                            z = true;
                            inode2 = inode;
                            try {
                                readSize = entry.callback.onRead(offset, size, data2);
                                obj = this.mLock;
                                synchronized (obj) {
                                    try {
                                        if (this.mInstance != 0) {
                                            obj2 = obj;
                                            native_replyRead(this.mInstance, unique2, readSize, data2);
                                        } else {
                                            obj2 = obj;
                                            i = size;
                                        }
                                        recycleLocked(args);
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                }
                            } catch (Exception e2) {
                                e = e2;
                                i = size;
                                unique = unique2;
                                error = e;
                                synchronized (this.mLock) {
                                }
                            }
                            break;
                        case 16:
                            try {
                                readSize = entry.callback.onWrite(offset, size, data);
                                Object obj4 = this.mLock;
                                synchronized (obj4) {
                                    long offset2 = offset;
                                    try {
                                        if (this.mInstance != 0) {
                                            obj2 = obj4;
                                            z = true;
                                            inode = offset2;
                                            native_replyWrite(this.mInstance, unique2, readSize);
                                        } else {
                                            obj2 = obj4;
                                            data2 = data;
                                            inode2 = inode;
                                            inode = offset2;
                                            z = true;
                                        }
                                        recycleLocked(args);
                                        i = size;
                                        break;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        throw th;
                                    }
                                }
                            } catch (Exception e3) {
                                e = e3;
                                data2 = data;
                                z = true;
                                inode2 = inode;
                                inode = offset;
                                i = size;
                                unique = unique2;
                                error = e;
                                synchronized (this.mLock) {
                                }
                            }
                            break;
                        default:
                            try {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown FUSE command: ");
                                stringBuilder.append(message.what);
                                throw new IllegalArgumentException(stringBuilder.toString());
                            } catch (Exception e4) {
                                e = e4;
                                data2 = data;
                                z = true;
                                i = size;
                                unique = unique2;
                                inode2 = inode;
                                inode = offset;
                                error = e;
                                synchronized (this.mLock) {
                                }
                            }
                            break;
                    }
                } else {
                    z = true;
                    i = size;
                    inode2 = inode;
                    inode = offset;
                    entry.callback.onFsync();
                    synchronized (this.mLock) {
                        if (this.mInstance != 0) {
                            native_replySimple(this.mInstance, unique2, 0);
                        }
                        recycleLocked(args);
                    }
                }
                unique = unique2;
            } else {
                z = true;
                i = size;
                inode2 = inode;
                inode = offset;
                try {
                    long unique3 = unique2;
                    unique2 = entry.callback.onGetSize();
                    try {
                        obj = this.mLock;
                        synchronized (obj) {
                            try {
                                if (this.mInstance != 0) {
                                    obj2 = obj;
                                    unique = unique3;
                                    native_replyGetAttr(this.mInstance, unique3, inode2, unique2);
                                } else {
                                    obj2 = obj;
                                    unique = unique3;
                                }
                                recycleLocked(args);
                            } catch (Throwable th5) {
                                th = th5;
                                throw th;
                            }
                        }
                    } catch (Exception e5) {
                        e = e5;
                        unique = unique3;
                        error = e;
                        synchronized (this.mLock) {
                        }
                    }
                } catch (Exception e6) {
                    e = e6;
                    unique = unique2;
                    error = e;
                    synchronized (this.mLock) {
                    }
                }
            }
            j = unique;
        } catch (Exception e7) {
            e = e7;
            data2 = data;
            z = true;
            i = size;
            unique = unique2;
            inode2 = inode;
            error = e;
            synchronized (this.mLock) {
                try {
                    Log.e(TAG, "", error);
                    replySimpleLocked(unique, getError(error));
                    recycleLocked(args);
                } catch (Throwable th6) {
                    th = th6;
                    throw th;
                }
            }
        }
        return z;
        unique = unique2;
        error = e;
        synchronized (this.mLock) {
        }
        return z;
    }

    private void onCommand(int command, long unique, long inode, long offset, int size, byte[] data) {
        synchronized (this.mLock) {
            try {
                Args args;
                if (this.mArgsPool.size() == 0) {
                    args = new Args();
                } else {
                    args = (Args) this.mArgsPool.pop();
                }
                args.unique = unique;
                args.inode = inode;
                args.offset = offset;
                args.size = size;
                args.data = data;
                args.entry = getCallbackEntryOrThrowLocked(inode);
                if (args.entry.handler.sendMessage(Message.obtain(args.entry.handler, command, 0, 0, args))) {
                } else {
                    throw new ErrnoException("onCommand", OsConstants.EBADF);
                }
            } catch (Exception error) {
                replySimpleLocked(unique, getError(error));
            }
        }
    }

    private byte[] onOpen(long unique, long inode) {
        synchronized (this.mLock) {
            try {
                CallbackEntry entry = getCallbackEntryOrThrowLocked(inode);
                if (entry.opened) {
                    throw new ErrnoException("onOpen", OsConstants.EMFILE);
                }
                if (this.mInstance != 0) {
                    native_replyOpen(this.mInstance, unique, inode);
                    entry.opened = true;
                    byte[] startUsing = this.mBytesMap.startUsing(entry.getThreadId());
                    return startUsing;
                }
                return null;
            } catch (ErrnoException error) {
                replySimpleLocked(unique, getError(error));
            }
        }
    }

    private static int getError(Exception error) {
        if (error instanceof ErrnoException) {
            int errno = ((ErrnoException) error).errno;
            if (errno != OsConstants.ENOSYS) {
                return -errno;
            }
        }
        return -OsConstants.EBADF;
    }

    @GuardedBy("mLock")
    private CallbackEntry getCallbackEntryOrThrowLocked(long inode) throws ErrnoException {
        CallbackEntry entry = (CallbackEntry) this.mCallbackMap.get(checkInode(inode));
        if (entry != null) {
            return entry;
        }
        throw new ErrnoException("getCallbackEntryOrThrowLocked", OsConstants.ENOENT);
    }

    @GuardedBy("mLock")
    private void recycleLocked(Args args) {
        if (this.mArgsPool.size() < 50) {
            this.mArgsPool.add(args);
        }
    }

    @GuardedBy("mLock")
    private void replySimpleLocked(long unique, int result) {
        if (this.mInstance != 0) {
            native_replySimple(this.mInstance, unique, result);
        }
    }

    private static int checkInode(long inode) {
        Preconditions.checkArgumentInRange(inode, 2, 2147483647L, "checkInode");
        return (int) inode;
    }
}
