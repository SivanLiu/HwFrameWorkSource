package libcore.util;

import dalvik.system.VMRuntime;
import sun.misc.Cleaner;

public class NativeAllocationRegistry {
    private final ClassLoader classLoader;
    private final long freeFunction;
    private final long size;

    public interface Allocator {
        long allocate();
    }

    private static class CleanerRunner implements Runnable {
        private final Cleaner cleaner;

        public CleanerRunner(Cleaner cleaner) {
            this.cleaner = cleaner;
        }

        public void run() {
            this.cleaner.clean();
        }
    }

    private class CleanerThunk implements Runnable {
        private long nativePtr = 0;

        public void run() {
            if (this.nativePtr != 0) {
                NativeAllocationRegistry.applyFreeFunction(NativeAllocationRegistry.this.freeFunction, this.nativePtr);
                NativeAllocationRegistry.registerNativeFree(NativeAllocationRegistry.this.size);
            }
        }

        public void setNativePtr(long nativePtr) {
            this.nativePtr = nativePtr;
        }
    }

    public static native void applyFreeFunction(long j, long j2);

    public NativeAllocationRegistry(ClassLoader classLoader, long freeFunction, long size) {
        if (size >= 0) {
            this.classLoader = classLoader;
            this.freeFunction = freeFunction;
            this.size = size;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid native allocation size: ");
        stringBuilder.append(size);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public Runnable registerNativeAllocation(Object referent, long nativePtr) {
        if (referent == null) {
            throw new IllegalArgumentException("referent is null");
        } else if (nativePtr != 0) {
            try {
                CleanerThunk thunk = new CleanerThunk();
                CleanerRunner result = new CleanerRunner(Cleaner.create(referent, thunk));
                registerNativeAllocation(this.size);
                thunk.setNativePtr(nativePtr);
                return result;
            } catch (VirtualMachineError vme) {
                applyFreeFunction(this.freeFunction, nativePtr);
                throw vme;
            }
        } else {
            throw new IllegalArgumentException("nativePtr is null");
        }
    }

    public Runnable registerNativeAllocation(Object referent, Allocator allocator) {
        if (referent != null) {
            CleanerThunk thunk = new CleanerThunk();
            Cleaner cleaner = Cleaner.create(referent, thunk);
            CleanerRunner result = new CleanerRunner(cleaner);
            long nativePtr = allocator.allocate();
            if (nativePtr == 0) {
                cleaner.clean();
                return null;
            }
            registerNativeAllocation(this.size);
            thunk.setNativePtr(nativePtr);
            return result;
        }
        throw new IllegalArgumentException("referent is null");
    }

    private static void registerNativeAllocation(long size) {
        VMRuntime.getRuntime().registerNativeAllocation((int) Math.min(size, 2147483647L));
    }

    private static void registerNativeFree(long size) {
        VMRuntime.getRuntime().registerNativeFree((int) Math.min(size, 2147483647L));
    }
}
