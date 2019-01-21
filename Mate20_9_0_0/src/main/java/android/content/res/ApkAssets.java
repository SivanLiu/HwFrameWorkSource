package android.content.res;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;
import java.io.IOException;

public final class ApkAssets {
    @GuardedBy("this")
    private final long mNativePtr;
    @GuardedBy("this")
    private StringBlock mStringBlock = new StringBlock(nativeGetStringBlock(this.mNativePtr), true);

    private static native void nativeDestroy(long j);

    private static native String nativeGetAssetPath(long j);

    private static native long nativeGetStringBlock(long j);

    private static native boolean nativeIsUpToDate(long j);

    private static native long nativeLoad(String str, boolean z, boolean z2, boolean z3) throws IOException;

    private static native long nativeLoadFromFd(FileDescriptor fileDescriptor, String str, boolean z, boolean z2) throws IOException;

    private static native long nativeOpenXml(long j, String str) throws IOException;

    public static ApkAssets loadFromPath(String path) throws IOException {
        return new ApkAssets(path, false, false, false);
    }

    public static ApkAssets loadFromPath(String path, boolean system) throws IOException {
        return new ApkAssets(path, system, false, false);
    }

    public static ApkAssets loadFromPath(String path, boolean system, boolean forceSharedLibrary) throws IOException {
        return new ApkAssets(path, system, forceSharedLibrary, false);
    }

    public static ApkAssets loadFromFd(FileDescriptor fd, String friendlyName, boolean system, boolean forceSharedLibrary) throws IOException {
        return new ApkAssets(fd, friendlyName, system, forceSharedLibrary);
    }

    public static ApkAssets loadOverlayFromPath(String idmapPath, boolean system) throws IOException {
        return new ApkAssets(idmapPath, system, false, true);
    }

    private ApkAssets(String path, boolean system, boolean forceSharedLib, boolean overlay) throws IOException {
        Preconditions.checkNotNull(path, "path");
        this.mNativePtr = nativeLoad(path, system, forceSharedLib, overlay);
    }

    private ApkAssets(FileDescriptor fd, String friendlyName, boolean system, boolean forceSharedLib) throws IOException {
        Preconditions.checkNotNull(fd, "fd");
        Preconditions.checkNotNull(friendlyName, "friendlyName");
        this.mNativePtr = nativeLoadFromFd(fd, friendlyName, system, forceSharedLib);
    }

    public String getAssetPath() {
        String nativeGetAssetPath;
        synchronized (this) {
            nativeGetAssetPath = nativeGetAssetPath(this.mNativePtr);
        }
        return nativeGetAssetPath;
    }

    CharSequence getStringFromPool(int idx) {
        CharSequence charSequence;
        synchronized (this) {
            charSequence = this.mStringBlock.get(idx);
        }
        return charSequence;
    }

    /* JADX WARNING: Missing block: B:18:0x002a, code skipped:
            if (r3 != null) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:21:0x0030, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r3.addSuppressed(r5);
     */
    /* JADX WARNING: Missing block: B:24:0x0035, code skipped:
            r2.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public XmlResourceParser openXml(String fileName) throws IOException {
        XmlResourceParser parser;
        Preconditions.checkNotNull(fileName, "fileName");
        synchronized (this) {
            XmlBlock block = new XmlBlock(null, nativeOpenXml(this.mNativePtr, fileName));
            parser = block.newParser();
            if (parser != null) {
                block.close();
            } else {
                throw new AssertionError("block.newParser() returned a null parser");
            }
        }
        return parser;
    }

    public boolean isUpToDate() {
        boolean nativeIsUpToDate;
        synchronized (this) {
            nativeIsUpToDate = nativeIsUpToDate(this.mNativePtr);
        }
        return nativeIsUpToDate;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ApkAssets{path=");
        stringBuilder.append(getAssetPath());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    protected void finalize() throws Throwable {
        nativeDestroy(this.mNativePtr);
    }
}
