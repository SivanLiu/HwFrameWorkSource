package com.android.internal.content;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.os.Build;
import android.os.SELinux;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import dalvik.system.CloseGuard;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class NativeLibraryHelper {
    private static final int BITCODE_PRESENT = 1;
    public static final String CLEAR_ABI_OVERRIDE = "-";
    private static final boolean DEBUG_NATIVE = false;
    private static final boolean HAS_NATIVE_BRIDGE = ("0".equals(SystemProperties.get("ro.dalvik.vm.native.bridge", "0")) ^ 1);
    public static final String LIB64_DIR_NAME = "lib64";
    public static final String LIB_DIR_NAME = "lib";
    private static final String TAG = "NativeHelper";
    private static final Object mRestoreconSync = new Object();

    public static class Handle implements Closeable {
        final long[] apkHandles;
        final boolean debuggable;
        final boolean extractNativeLibs;
        private volatile boolean mClosed;
        private final CloseGuard mGuard = CloseGuard.get();
        final boolean multiArch;

        public static Handle create(File packageFile) throws IOException {
            try {
                return create(PackageParser.parsePackageLite(packageFile, null));
            } catch (PackageParserException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse package: ");
                stringBuilder.append(packageFile);
                throw new IOException(stringBuilder.toString(), e);
            }
        }

        public static Handle create(Package pkg) throws IOException {
            List allCodePaths = pkg.getAllCodePaths();
            boolean z = false;
            boolean z2 = (pkg.applicationInfo.flags & Integer.MIN_VALUE) != 0;
            boolean z3 = (pkg.applicationInfo.flags & 268435456) != 0;
            if ((pkg.applicationInfo.flags & 2) != 0) {
                z = true;
            }
            return create(allCodePaths, z2, z3, z);
        }

        public static Handle create(PackageLite lite) throws IOException {
            return create(lite.getAllCodePaths(), lite.multiArch, lite.extractNativeLibs, lite.debuggable);
        }

        private static Handle create(List<String> codePaths, boolean multiArch, boolean extractNativeLibs, boolean debuggable) throws IOException {
            int size = codePaths.size();
            long[] apkHandles = new long[size];
            int j = 0;
            for (int i = 0; i < size; i++) {
                String path = (String) codePaths.get(i);
                apkHandles[i] = NativeLibraryHelper.nativeOpenApk(path);
                if (apkHandles[i] == 0) {
                    while (j < i) {
                        NativeLibraryHelper.nativeClose(apkHandles[j]);
                        j++;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to open APK: ");
                    stringBuilder.append(path);
                    throw new IOException(stringBuilder.toString());
                }
            }
            return new Handle(apkHandles, multiArch, extractNativeLibs, debuggable);
        }

        public static Handle createFd(PackageLite lite, FileDescriptor fd) throws IOException {
            long[] apkHandles = new long[1];
            String path = lite.baseCodePath;
            apkHandles[0] = NativeLibraryHelper.nativeOpenApkFd(fd, path);
            if (apkHandles[0] != 0) {
                return new Handle(apkHandles, lite.multiArch, lite.extractNativeLibs, lite.debuggable);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to open APK ");
            stringBuilder.append(path);
            stringBuilder.append(" from fd ");
            stringBuilder.append(fd);
            throw new IOException(stringBuilder.toString());
        }

        Handle(long[] apkHandles, boolean multiArch, boolean extractNativeLibs, boolean debuggable) {
            this.apkHandles = apkHandles;
            this.multiArch = multiArch;
            this.extractNativeLibs = extractNativeLibs;
            this.debuggable = debuggable;
            this.mGuard.open("close");
        }

        public void close() {
            for (long apkHandle : this.apkHandles) {
                NativeLibraryHelper.nativeClose(apkHandle);
            }
            this.mGuard.close();
            this.mClosed = true;
        }

        protected void finalize() throws Throwable {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
            }
            try {
                if (!this.mClosed) {
                    close();
                }
                super.finalize();
            } catch (Throwable th) {
                super.finalize();
            }
        }
    }

    private static native int hasRenderscriptBitcode(long j);

    private static native void nativeClose(long j);

    private static native int nativeCopyNativeBinaries(long j, String str, String str2, boolean z, boolean z2, boolean z3);

    private static native int nativeFindSupportedAbi(long j, String[] strArr, boolean z);

    private static native long nativeOpenApk(String str);

    private static native long nativeOpenApkFd(FileDescriptor fileDescriptor, String str);

    private static native long nativeSumNativeBinaries(long j, String str, boolean z);

    private static long sumNativeBinaries(Handle handle, String abi) {
        long sum = 0;
        for (long apkHandle : handle.apkHandles) {
            sum += nativeSumNativeBinaries(apkHandle, abi, handle.debuggable);
        }
        return sum;
    }

    public static int copyNativeBinaries(Handle handle, File sharedLibraryDir, String abi) {
        for (long apkHandle : handle.apkHandles) {
            int res = nativeCopyNativeBinaries(apkHandle, sharedLibraryDir.getPath(), abi, handle.extractNativeLibs, HAS_NATIVE_BRIDGE, handle.debuggable);
            if (res != 1) {
                return res;
            }
        }
        return 1;
    }

    public static int findSupportedAbi(Handle handle, String[] supportedAbis) {
        int finalRes = -114;
        for (long apkHandle : handle.apkHandles) {
            int res = nativeFindSupportedAbi(apkHandle, supportedAbis, handle.debuggable);
            if (res != -114) {
                if (res == -113) {
                    if (finalRes < 0) {
                        finalRes = -113;
                    }
                } else if (res < 0) {
                    return res;
                } else {
                    if (finalRes < 0 || res < finalRes) {
                        finalRes = res;
                    }
                }
            }
        }
        return finalRes;
    }

    public static void removeNativeBinariesLI(String nativeLibraryPath) {
        if (nativeLibraryPath != null) {
            removeNativeBinariesFromDirLI(new File(nativeLibraryPath), false);
        }
    }

    public static void removeNativeBinariesFromDirLI(File nativeLibraryRoot, boolean deleteRootDir) {
        if (nativeLibraryRoot.exists()) {
            File[] files = nativeLibraryRoot.listFiles();
            if (files != null) {
                for (int nn = 0; nn < files.length; nn++) {
                    if (files[nn].isDirectory()) {
                        removeNativeBinariesFromDirLI(files[nn], true);
                    } else if (!files[nn].delete()) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not delete native binary: ");
                        stringBuilder.append(files[nn].getPath());
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            }
            if (deleteRootDir && !nativeLibraryRoot.delete()) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Could not delete native binary directory: ");
                stringBuilder2.append(nativeLibraryRoot.getPath());
                Slog.w(str2, stringBuilder2.toString());
            }
        }
    }

    public static void createNativeLibrarySubdir(File path) throws IOException {
        StringBuilder stringBuilder;
        if (path.isDirectory()) {
            synchronized (mRestoreconSync) {
                if (SELinux.restorecon(path)) {
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot set SELinux context for ");
                    stringBuilder.append(path.getPath());
                    throw new IOException(stringBuilder.toString());
                }
            }
            return;
        }
        path.delete();
        if (path.mkdir()) {
            try {
                Os.chmod(path.getPath(), (((OsConstants.S_IRWXU | OsConstants.S_IRGRP) | OsConstants.S_IXGRP) | OsConstants.S_IROTH) | OsConstants.S_IXOTH);
                return;
            } catch (ErrnoException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot chmod native library directory ");
                stringBuilder.append(path.getPath());
                throw new IOException(stringBuilder.toString(), e);
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Cannot create ");
        stringBuilder2.append(path.getPath());
        throw new IOException(stringBuilder2.toString());
    }

    private static long sumNativeBinariesForSupportedAbi(Handle handle, String[] abiList) {
        int abi = findSupportedAbi(handle, abiList);
        if (abi >= 0) {
            return sumNativeBinaries(handle, abiList[abi]);
        }
        return 0;
    }

    public static int copyNativeBinariesForSupportedAbi(Handle handle, File libraryRoot, String[] abiList, boolean useIsaSubdir) throws IOException {
        createNativeLibrarySubdir(libraryRoot);
        int abi = findSupportedAbi(handle, abiList);
        if (abi >= 0) {
            File subDir;
            String instructionSet = VMRuntime.getInstructionSet(abiList[abi]);
            if (useIsaSubdir) {
                subDir = new File(libraryRoot, instructionSet);
                createNativeLibrarySubdir(subDir);
            } else {
                subDir = libraryRoot;
            }
            int copyRet = copyNativeBinaries(handle, subDir, abiList[abi]);
            if (copyRet != 1) {
                return copyRet;
            }
        }
        return abi;
    }

    public static int copyNativeBinariesWithOverride(Handle handle, File libraryRoot, String abiOverride) {
        try {
            String str;
            StringBuilder stringBuilder;
            if (handle.multiArch) {
                int copyRet;
                if (!(abiOverride == null || CLEAR_ABI_OVERRIDE.equals(abiOverride))) {
                    Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                }
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                    copyRet = copyNativeBinariesForSupportedAbi(handle, libraryRoot, Build.SUPPORTED_32_BIT_ABIS, true);
                    if (!(copyRet >= 0 || copyRet == -114 || copyRet == -113)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failure copying 32 bit native libraries; copyRet=");
                        stringBuilder.append(copyRet);
                        Slog.w(str, stringBuilder.toString());
                        return copyRet;
                    }
                }
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    copyRet = copyNativeBinariesForSupportedAbi(handle, libraryRoot, Build.SUPPORTED_64_BIT_ABIS, true);
                    if (!(copyRet >= 0 || copyRet == -114 || copyRet == -113)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failure copying 64 bit native libraries; copyRet=");
                        stringBuilder.append(copyRet);
                        Slog.w(str, stringBuilder.toString());
                        return copyRet;
                    }
                }
            }
            String cpuAbiOverride = null;
            if (CLEAR_ABI_OVERRIDE.equals(abiOverride)) {
                cpuAbiOverride = null;
            } else if (abiOverride != null) {
                cpuAbiOverride = abiOverride;
            }
            String[] abiList = cpuAbiOverride != null ? new String[]{cpuAbiOverride} : Build.SUPPORTED_ABIS;
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null && hasRenderscriptBitcode(handle)) {
                abiList = Build.SUPPORTED_32_BIT_ABIS;
            }
            int copyRet2 = copyNativeBinariesForSupportedAbi(handle, libraryRoot, abiList, true);
            if (copyRet2 < 0 && copyRet2 != -114) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failure copying native libraries [errorCode=");
                stringBuilder.append(copyRet2);
                stringBuilder.append("]");
                Slog.w(str, stringBuilder.toString());
                return copyRet2;
            }
            return 1;
        } catch (IOException e) {
            Slog.e(TAG, "Copying native libraries failed", e);
            return -110;
        }
    }

    public static long sumNativeBinariesWithOverride(Handle handle, String abiOverride) throws IOException {
        long sum = 0;
        if (handle.multiArch) {
            if (!(abiOverride == null || CLEAR_ABI_OVERRIDE.equals(abiOverride))) {
                Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
            }
            if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                sum = 0 + sumNativeBinariesForSupportedAbi(handle, Build.SUPPORTED_32_BIT_ABIS);
            }
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                return sum + sumNativeBinariesForSupportedAbi(handle, Build.SUPPORTED_64_BIT_ABIS);
            }
            return sum;
        }
        String cpuAbiOverride = null;
        if (CLEAR_ABI_OVERRIDE.equals(abiOverride)) {
            cpuAbiOverride = null;
        } else if (abiOverride != null) {
            cpuAbiOverride = abiOverride;
        }
        String[] abiList = cpuAbiOverride != null ? new String[]{cpuAbiOverride} : Build.SUPPORTED_ABIS;
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null && hasRenderscriptBitcode(handle)) {
            abiList = Build.SUPPORTED_32_BIT_ABIS;
        }
        return 0 + sumNativeBinariesForSupportedAbi(handle, abiList);
    }

    public static boolean hasRenderscriptBitcode(Handle handle) throws IOException {
        long[] jArr = handle.apkHandles;
        int length = jArr.length;
        int i = 0;
        while (i < length) {
            int res = hasRenderscriptBitcode(jArr[i]);
            if (res < 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error scanning APK, code: ");
                stringBuilder.append(res);
                throw new IOException(stringBuilder.toString());
            } else if (res == 1) {
                return true;
            } else {
                i++;
            }
        }
        return false;
    }
}
