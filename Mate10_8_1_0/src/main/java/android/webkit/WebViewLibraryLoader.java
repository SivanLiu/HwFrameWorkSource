package android.webkit;

import android.app.ActivityManagerInternal;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class WebViewLibraryLoader {
    private static final long CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES = 104857600;
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_32 = "/data/misc/shared_relro/libwebviewchromium32.relro";
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_64 = "/data/misc/shared_relro/libwebviewchromium64.relro";
    private static final boolean DEBUG = false;
    private static final String LOGTAG = WebViewLibraryLoader.class.getSimpleName();
    private static boolean sAddressSpaceReserved = false;

    private static class RelroFileCreator {
        private RelroFileCreator() {
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static void main(String[] args) {
            boolean result = false;
            boolean is64Bit = VMRuntime.getRuntime().is64Bit();
            try {
                if (args.length == 2 && args[0] != null) {
                    if (args[1] != null) {
                        Log.v(WebViewLibraryLoader.LOGTAG, "RelroFileCreator (64bit = " + is64Bit + "), " + " 32-bit lib: " + args[0] + ", 64-bit lib: " + args[1]);
                        if (WebViewLibraryLoader.sAddressSpaceReserved) {
                            result = WebViewLibraryLoader.nativeCreateRelroFile(args[0], args[1], WebViewLibraryLoader.CHROMIUM_WEBVIEW_NATIVE_RELRO_32, WebViewLibraryLoader.CHROMIUM_WEBVIEW_NATIVE_RELRO_64);
                            try {
                                WebViewFactory.getUpdateService().notifyRelroCreationCompleted();
                            } catch (RemoteException e) {
                                Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e);
                            }
                            if (!result) {
                                Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                            }
                            System.exit(0);
                            return;
                        }
                        Log.e(WebViewLibraryLoader.LOGTAG, "can't create relro file; address space not reserved");
                        try {
                            WebViewFactory.getUpdateService().notifyRelroCreationCompleted();
                        } catch (RemoteException e2) {
                            Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e2);
                        }
                        if (!result) {
                            Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                        }
                        System.exit(0);
                        return;
                    }
                }
                Log.e(WebViewLibraryLoader.LOGTAG, "Invalid RelroFileCreator args: " + Arrays.toString(args));
            } finally {
                try {
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted();
                } catch (RemoteException e22) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e22);
                }
                if (!result) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                }
                System.exit(0);
            }
        }
    }

    static native boolean nativeCreateRelroFile(String str, String str2, String str3, String str4);

    static native int nativeLoadWithRelroFile(String str, String str2, String str3, ClassLoader classLoader);

    static native boolean nativeReserveAddressSpace(long j);

    WebViewLibraryLoader() {
    }

    static void createRelroFile(boolean is64Bit, String[] nativeLibraryPaths) {
        final String abi = is64Bit ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        Runnable crashHandler = new Runnable() {
            public void run() {
                try {
                    Log.e(WebViewLibraryLoader.LOGTAG, "relro file creator for " + abi + " crashed. Proceeding without");
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted();
                } catch (RemoteException e) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "Cannot reach WebViewUpdateService. " + e.getMessage());
                }
            }
        };
        if (nativeLibraryPaths != null) {
            try {
                if (nativeLibraryPaths[0] != null) {
                    if (nativeLibraryPaths[1] != null) {
                        if (((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).startIsolatedProcess(RelroFileCreator.class.getName(), nativeLibraryPaths, "WebViewLoader-" + abi, abi, 1037, crashHandler) <= 0) {
                            throw new Exception("Failed to start the relro file creator process");
                        }
                        return;
                    }
                }
            } catch (Throwable t) {
                Log.e(LOGTAG, "error starting relro file creator for abi " + abi, t);
                crashHandler.run();
                return;
            }
        }
        throw new IllegalArgumentException("Native library paths to the WebView RelRo process must not be null!");
    }

    static String[] updateWebViewZygoteVmSize(PackageInfo packageInfo) throws MissingWebViewPackageException {
        String[] split;
        IOException e;
        Throwable th;
        String[] nativeLibs = getWebViewNativeLibraryPaths(packageInfo);
        if (nativeLibs != null) {
            long newVmSize = 0;
            for (String path : nativeLibs) {
                if (!(path == null || TextUtils.isEmpty(path))) {
                    File f = new File(path);
                    if (f.exists()) {
                        newVmSize = Math.max(newVmSize, f.length());
                    } else {
                        if (path.contains("!/")) {
                            split = TextUtils.split(path, "!/");
                            if (split.length == 2) {
                                Throwable th2 = null;
                                ZipFile zipFile = null;
                                try {
                                    ZipFile z = new ZipFile(split[0]);
                                    try {
                                        ZipEntry e2 = z.getEntry(split[1]);
                                        if (e2 == null || e2.getMethod() != 0) {
                                            if (z != null) {
                                                try {
                                                    z.close();
                                                } catch (Throwable th3) {
                                                    th2 = th3;
                                                }
                                            }
                                            if (th2 != null) {
                                                throw th2;
                                            }
                                        } else {
                                            newVmSize = Math.max(newVmSize, e2.getSize());
                                            if (z != null) {
                                                try {
                                                    z.close();
                                                } catch (Throwable th4) {
                                                    th2 = th4;
                                                }
                                            }
                                            if (th2 != null) {
                                                try {
                                                    throw th2;
                                                } catch (IOException e3) {
                                                    e = e3;
                                                    zipFile = z;
                                                }
                                            }
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        zipFile = z;
                                        if (zipFile != null) {
                                            try {
                                                zipFile.close();
                                            } catch (Throwable th6) {
                                                if (th2 == null) {
                                                    th2 = th6;
                                                } else if (th2 != th6) {
                                                    th2.addSuppressed(th6);
                                                }
                                            }
                                        }
                                        if (th2 == null) {
                                            try {
                                                throw th2;
                                            } catch (IOException e4) {
                                                e = e4;
                                            }
                                        } else {
                                            throw th;
                                        }
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    if (zipFile != null) {
                                        zipFile.close();
                                    }
                                    if (th2 == null) {
                                        throw th;
                                    }
                                    throw th2;
                                }
                            }
                        }
                        Log.e(LOGTAG, "error sizing load for " + path);
                    }
                }
            }
            newVmSize = Math.max(2 * newVmSize, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
            Log.d(LOGTAG, "Setting new address space to " + newVmSize);
            setWebViewZygoteVmSize(newVmSize);
        }
        return nativeLibs;
        Log.e(LOGTAG, "error reading APK file " + split[0] + ", ", e);
        Log.e(LOGTAG, "error sizing load for " + path);
    }

    static void reserveAddressSpaceInZygote() {
        System.loadLibrary("webviewchromium_loader");
        long addressSpaceToReserve = SystemProperties.getLong(WebViewFactory.CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
        sAddressSpaceReserved = nativeReserveAddressSpace(addressSpaceToReserve);
        if (!sAddressSpaceReserved) {
            Log.e(LOGTAG, "reserving " + addressSpaceToReserve + " bytes of address space failed");
        }
    }

    static int loadNativeLibrary(ClassLoader clazzLoader, PackageInfo packageInfo) throws MissingWebViewPackageException {
        if (sAddressSpaceReserved) {
            int result = nativeLoadWithRelroFile(WebViewFactory.getWebViewLibrary(packageInfo.applicationInfo), CHROMIUM_WEBVIEW_NATIVE_RELRO_32, CHROMIUM_WEBVIEW_NATIVE_RELRO_64, clazzLoader);
            if (result != 0) {
                Log.w(LOGTAG, "failed to load with relro file, proceeding without");
            }
            return result;
        }
        Log.e(LOGTAG, "can't load with relro file; address space not reserved");
        return 2;
    }

    static String[] getWebViewNativeLibraryPaths(PackageInfo packageInfo) throws MissingWebViewPackageException {
        String path64;
        String path32;
        ApplicationInfo ai = packageInfo.applicationInfo;
        String nativeLibFileName = WebViewFactory.getWebViewLibrary(ai);
        boolean primaryArchIs64bit = VMRuntime.is64BitAbi(ai.primaryCpuAbi);
        if (TextUtils.isEmpty(ai.secondaryCpuAbi)) {
            if (primaryArchIs64bit) {
                path64 = ai.nativeLibraryDir;
                path32 = "";
            } else {
                path32 = ai.nativeLibraryDir;
                path64 = "";
            }
        } else if (primaryArchIs64bit) {
            path64 = ai.nativeLibraryDir;
            path32 = ai.secondaryNativeLibraryDir;
        } else {
            path64 = ai.secondaryNativeLibraryDir;
            path32 = ai.nativeLibraryDir;
        }
        if (!TextUtils.isEmpty(path32)) {
            path32 = path32 + "/" + nativeLibFileName;
            if (!new File(path32).exists()) {
                path32 = getLoadFromApkPath(ai.sourceDir, Build.SUPPORTED_32_BIT_ABIS, nativeLibFileName);
            }
        }
        if (!TextUtils.isEmpty(path64)) {
            path64 = path64 + "/" + nativeLibFileName;
            if (!new File(path64).exists()) {
                path64 = getLoadFromApkPath(ai.sourceDir, Build.SUPPORTED_64_BIT_ABIS, nativeLibFileName);
            }
        }
        return new String[]{path32, path64};
    }

    private static String getLoadFromApkPath(String apkPath, String[] abiList, String nativeLibFileName) throws MissingWebViewPackageException {
        Exception e;
        Throwable th;
        int i = 0;
        Throwable th2 = null;
        ZipFile zipFile = null;
        try {
            ZipFile z = new ZipFile(apkPath);
            try {
                int length = abiList.length;
                while (i < length) {
                    String entry = "lib/" + abiList[i] + "/" + nativeLibFileName;
                    ZipEntry e2 = z.getEntry(entry);
                    if (e2 == null || e2.getMethod() != 0) {
                        i++;
                    } else {
                        String str = apkPath + "!/" + entry;
                        if (z != null) {
                            try {
                                z.close();
                            } catch (Throwable th3) {
                                th2 = th3;
                            }
                        }
                        if (th2 == null) {
                            return str;
                        }
                        try {
                            throw th2;
                        } catch (IOException e3) {
                            e = e3;
                            zipFile = z;
                        }
                    }
                }
                if (z != null) {
                    try {
                        z.close();
                    } catch (Throwable th4) {
                        th2 = th4;
                    }
                }
                if (th2 == null) {
                    return "";
                }
                throw th2;
            } catch (Throwable th5) {
                th = th5;
                zipFile = z;
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Throwable th6) {
                        if (th2 == null) {
                            th2 = th6;
                        } else if (th2 != th6) {
                            th2.addSuppressed(th6);
                        }
                    }
                }
                if (th2 == null) {
                    try {
                        throw th2;
                    } catch (IOException e4) {
                        e = e4;
                        throw new MissingWebViewPackageException(e);
                    }
                }
                throw th;
            }
        } catch (Throwable th7) {
            th = th7;
            if (zipFile != null) {
                zipFile.close();
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }

    private static void setWebViewZygoteVmSize(long vmSize) {
        SystemProperties.set(WebViewFactory.CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, Long.toString(vmSize));
    }
}
