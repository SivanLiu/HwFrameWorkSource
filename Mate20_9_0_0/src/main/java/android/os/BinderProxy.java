package android.os;

import android.os.IBinder.DeathRecipient;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.os.BinderInternal;
import java.io.FileDescriptor;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import libcore.util.NativeAllocationRegistry;

/* compiled from: Binder */
final class BinderProxy implements IBinder {
    private static final int NATIVE_ALLOCATION_SIZE = 1000;
    private static ProxyMap sProxyMap = new ProxyMap();
    private final long mNativeData;
    volatile boolean mWarnOnBlocking = Binder.sWarnOnBlocking;

    /* compiled from: Binder */
    private static class NoImagePreloadHolder {
        public static final long sNativeFinalizer = BinderProxy.getNativeFinalizer();
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(BinderProxy.class.getClassLoader(), sNativeFinalizer, 1000);

        private NoImagePreloadHolder() {
        }
    }

    /* compiled from: Binder */
    private static final class ProxyMap {
        private static final int CRASH_AT_SIZE = 20000;
        private static final int LOG_MAIN_INDEX_SIZE = 8;
        private static final int MAIN_INDEX_MASK = 255;
        private static final int MAIN_INDEX_SIZE = 256;
        private static final int WARN_INCREMENT = 10;
        private final Long[][] mMainIndexKeys;
        private final ArrayList<WeakReference<BinderProxy>>[] mMainIndexValues;
        private int mRandom;
        private int mWarnBucketSize;

        private ProxyMap() {
            this.mWarnBucketSize = 20;
            this.mMainIndexKeys = new Long[256][];
            this.mMainIndexValues = new ArrayList[256];
        }

        private static int hash(long arg) {
            return ((int) ((arg >> 2) ^ (arg >> 10))) & 255;
        }

        private int size() {
            int size = 0;
            for (ArrayList<WeakReference<BinderProxy>> a : this.mMainIndexValues) {
                if (a != null) {
                    size += a.size();
                }
            }
            return size;
        }

        private int unclearedSize() {
            int size = 0;
            for (ArrayList<WeakReference<BinderProxy>> a : this.mMainIndexValues) {
                if (a != null) {
                    Iterator it = a.iterator();
                    while (it.hasNext()) {
                        if (((WeakReference) it.next()).get() != null) {
                            size++;
                        }
                    }
                }
            }
            return size;
        }

        private void remove(int hash, int index) {
            Long[] keyArray = this.mMainIndexKeys[hash];
            ArrayList<WeakReference<BinderProxy>> valueArray = this.mMainIndexValues[hash];
            int size = valueArray.size();
            if (index != size - 1) {
                keyArray[index] = keyArray[size - 1];
                valueArray.set(index, (WeakReference) valueArray.get(size - 1));
            }
            valueArray.remove(size - 1);
        }

        BinderProxy get(long key) {
            int myHash = hash(key);
            Long[] keyArray = this.mMainIndexKeys[myHash];
            if (keyArray == null) {
                return null;
            }
            ArrayList<WeakReference<BinderProxy>> valueArray = this.mMainIndexValues[myHash];
            int bucketSize = valueArray.size();
            for (int i = 0; i < bucketSize; i++) {
                if (key == keyArray[i].longValue()) {
                    BinderProxy bp = (BinderProxy) ((WeakReference) valueArray.get(i)).get();
                    if (bp != null) {
                        return bp;
                    }
                    remove(myHash, i);
                    return null;
                }
            }
            return null;
        }

        void set(long key, BinderProxy value) {
            int i;
            int myHash = hash(key);
            ArrayList<WeakReference<BinderProxy>> valueArray = this.mMainIndexValues[myHash];
            if (valueArray == null) {
                ArrayList[] arrayListArr = this.mMainIndexValues;
                ArrayList<WeakReference<BinderProxy>> arrayList = new ArrayList();
                arrayListArr[myHash] = arrayList;
                valueArray = arrayList;
                this.mMainIndexKeys[myHash] = new Long[1];
            }
            int size = valueArray.size();
            WeakReference<BinderProxy> newWr = new WeakReference(value);
            for (int i2 = 0; i2 < size; i2++) {
                if (((WeakReference) valueArray.get(i2)).get() == null) {
                    valueArray.set(i2, newWr);
                    this.mMainIndexKeys[myHash][i2] = Long.valueOf(key);
                    if (i2 < size - 1) {
                        i = this.mRandom + 1;
                        this.mRandom = i;
                        int rnd = Math.floorMod(i, size - (i2 + 1));
                        if (((WeakReference) valueArray.get((i2 + 1) + rnd)).get() == null) {
                            remove(myHash, (i2 + 1) + rnd);
                        }
                    }
                    return;
                }
            }
            valueArray.add(size, newWr);
            Long[] keyArray = this.mMainIndexKeys[myHash];
            if (keyArray.length == size) {
                Long[] newArray = new Long[(((size / 2) + size) + 2)];
                System.arraycopy(keyArray, 0, newArray, 0, size);
                newArray[size] = Long.valueOf(key);
                this.mMainIndexKeys[myHash] = newArray;
            } else {
                keyArray[size] = Long.valueOf(key);
            }
            if (size >= this.mWarnBucketSize) {
                int totalSize = size();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("BinderProxy map growth! bucket size = ");
                stringBuilder.append(size);
                stringBuilder.append(" total = ");
                stringBuilder.append(totalSize);
                Log.v("Binder", stringBuilder.toString());
                this.mWarnBucketSize += 10;
                if (Build.IS_DEBUGGABLE && totalSize >= 20000) {
                    i = unclearedSize();
                    StringBuilder stringBuilder2;
                    if (i >= 20000) {
                        dumpProxyInterfaceCounts();
                        dumpPerUidProxyCounts();
                        Runtime.getRuntime().gc();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Binder ProxyMap has too many entries: ");
                        stringBuilder2.append(totalSize);
                        stringBuilder2.append(" (total), ");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" (uncleared), ");
                        stringBuilder2.append(unclearedSize());
                        stringBuilder2.append(" (uncleared after GC). BinderProxy leak?");
                        throw new AssertionError(stringBuilder2.toString());
                    } else if (totalSize > (3 * i) / 2) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("BinderProxy map has many cleared entries: ");
                        stringBuilder2.append(totalSize - i);
                        stringBuilder2.append(" of ");
                        stringBuilder2.append(totalSize);
                        stringBuilder2.append(" are cleared");
                        Log.v("Binder", stringBuilder2.toString());
                    }
                }
            }
        }

        private void dumpProxyInterfaceCounts() {
            Map<String, Integer> counts = new HashMap();
            int i = 0;
            for (ArrayList<WeakReference<BinderProxy>> a : this.mMainIndexValues) {
                if (a != null) {
                    int size = a.size();
                    for (int index = 0; index < size; index++) {
                        WeakReference<BinderProxy> weakRef = null;
                        try {
                            weakRef = (WeakReference) a.get(index);
                        } catch (Exception e) {
                            Log.w("Binder", "dumpProxyInterfaceCounts");
                        }
                        if (weakRef != null) {
                            String key;
                            BinderProxy bp = (BinderProxy) weakRef.get();
                            if (bp == null) {
                                key = "<cleared weak-ref>";
                            } else {
                                try {
                                    key = bp.getInterfaceDescriptor();
                                } catch (Throwable th) {
                                    key = "<exception during getDescriptor>";
                                }
                            }
                            Integer i2 = (Integer) counts.get(key);
                            if (i2 == null) {
                                counts.put(key, Integer.valueOf(1));
                            } else {
                                counts.put(key, Integer.valueOf(i2.intValue() + 1));
                            }
                        }
                    }
                }
            }
            Entry[] sorted = (Entry[]) counts.entrySet().toArray(new Entry[counts.size()]);
            Arrays.sort(sorted, -$$Lambda$BinderProxy$ProxyMap$huB_NMtOmTDIIYkL7mXm-Otlfnw.INSTANCE);
            Log.v("Binder", "BinderProxy descriptor histogram (top ten):");
            int min = Math.min(10, sorted.length);
            while (i < min) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" #");
                stringBuilder.append(i + 1);
                stringBuilder.append(": ");
                stringBuilder.append((String) sorted[i].getKey());
                stringBuilder.append(" x");
                stringBuilder.append(sorted[i].getValue());
                Log.v("Binder", stringBuilder.toString());
                i++;
            }
        }

        private void dumpPerUidProxyCounts() {
            SparseIntArray counts = BinderInternal.nGetBinderProxyPerUidCounts();
            if (counts.size() != 0) {
                Log.d("Binder", "Per Uid Binder Proxy Counts:");
                for (int i = 0; i < counts.size(); i++) {
                    int uid = counts.keyAt(i);
                    int binderCount = counts.valueAt(i);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("UID : ");
                    stringBuilder.append(uid);
                    stringBuilder.append("  count = ");
                    stringBuilder.append(binderCount);
                    Log.d("Binder", stringBuilder.toString());
                }
            }
        }
    }

    private static native long getNativeFinalizer();

    public native String getInterfaceDescriptor() throws RemoteException;

    public native boolean isBinderAlive();

    public native void linkToDeath(DeathRecipient deathRecipient, int i) throws RemoteException;

    public native boolean pingBinder();

    public native boolean transactNative(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException;

    public native boolean unlinkToDeath(DeathRecipient deathRecipient, int i);

    private static void dumpProxyDebugInfo() {
        if (Build.IS_DEBUGGABLE) {
            sProxyMap.dumpProxyInterfaceCounts();
        }
    }

    private static BinderProxy getInstance(long nativeData, long iBinder) {
        try {
            BinderProxy result = sProxyMap.get(iBinder);
            if (result != null) {
                return result;
            }
            result = new BinderProxy(nativeData);
            NoImagePreloadHolder.sRegistry.registerNativeAllocation(result, nativeData);
            sProxyMap.set(iBinder, result);
            return result;
        } catch (Throwable th) {
            NativeAllocationRegistry.applyFreeFunction(NoImagePreloadHolder.sNativeFinalizer, nativeData);
        }
    }

    private BinderProxy(long nativeData) {
        this.mNativeData = nativeData;
    }

    public IInterface queryLocalInterface(String descriptor) {
        return null;
    }

    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Binder.checkParcel(this, code, data, "Unreasonably large binder buffer");
        if (this.mWarnOnBlocking && (flags & 1) == 0) {
            this.mWarnOnBlocking = false;
            Log.w("Binder", "Outgoing transactions from this process must be FLAG_ONEWAY", new Throwable());
        }
        boolean tracingEnabled = Binder.isTracingEnabled();
        if (tracingEnabled) {
            Throwable tr = new Throwable();
            Binder.getTransactionTracker().addTrace(tr);
            StackTraceElement stackTraceElement = tr.getStackTrace()[1];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(stackTraceElement.getClassName());
            stringBuilder.append(".");
            stringBuilder.append(stackTraceElement.getMethodName());
            Trace.traceBegin(1, stringBuilder.toString());
        }
        try {
            if (BlockMonitor.isNeedMonitor()) {
                long startTime = SystemClock.uptimeMillis();
                boolean result = transactNative(code, data, reply, flags);
                BlockMonitor.checkBinderTime(startTime);
                if (tracingEnabled) {
                    Trace.traceEnd(1);
                }
                return result;
            }
            boolean transactNative = transactNative(code, data, reply, flags);
            return transactNative;
        } finally {
            if (tracingEnabled) {
                Trace.traceEnd(1);
            }
        }
    }

    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(fd);
        data.writeStringArray(args);
        try {
            transact(IBinder.DUMP_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(fd);
        data.writeStringArray(args);
        try {
            transact(IBinder.DUMP_TRANSACTION, data, reply, 1);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(in);
        data.writeFileDescriptor(out);
        data.writeFileDescriptor(err);
        data.writeStringArray(args);
        ShellCallback.writeToParcel(callback, data);
        resultReceiver.writeToParcel(data, 0);
        try {
            transact(IBinder.SHELL_COMMAND_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static final void sendDeathNotice(DeathRecipient recipient) {
        try {
            recipient.binderDied();
        } catch (RuntimeException exc) {
            Log.w("BinderNative", "Uncaught exception from death notification", exc);
        }
    }
}
