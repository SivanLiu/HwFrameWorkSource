package android.webkit;

import android.app.ActivityManagerInternal;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@VisibleForTesting
public class WebViewLibraryLoader {
    private static final long CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES = 104857600;
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_32 = "/data/misc/shared_relro/libwebviewchromium32.relro";
    private static final String CHROMIUM_WEBVIEW_NATIVE_RELRO_64 = "/data/misc/shared_relro/libwebviewchromium64.relro";
    private static final boolean DEBUG = false;
    private static final String LOGTAG = WebViewLibraryLoader.class.getSimpleName();
    private static boolean sAddressSpaceReserved = false;

    private static class RelroFileCreator {
        private RelroFileCreator() {
        }

        public static void main(String[] args) {
            boolean is64Bit = VMRuntime.getRuntime().is64Bit();
            try {
                String access$000;
                StringBuilder stringBuilder;
                if (args.length == 1) {
                    if (args[0] != null) {
                        access$000 = WebViewLibraryLoader.LOGTAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("RelroFileCreator (64bit = ");
                        stringBuilder.append(is64Bit);
                        stringBuilder.append("), lib: ");
                        stringBuilder.append(args[0]);
                        Log.v(access$000, stringBuilder.toString());
                        if (WebViewLibraryLoader.sAddressSpaceReserved) {
                            String str;
                            access$000 = args[0];
                            if (is64Bit) {
                                str = WebViewLibraryLoader.CHROMIUM_WEBVIEW_NATIVE_RELRO_64;
                            } else {
                                str = WebViewLibraryLoader.CHROMIUM_WEBVIEW_NATIVE_RELRO_32;
                            }
                            boolean result = WebViewLibraryLoader.nativeCreateRelroFile(access$000, str);
                            try {
                                WebViewFactory.getUpdateServiceUnchecked().notifyRelroCreationCompleted();
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
                            WebViewFactory.getUpdateServiceUnchecked().notifyRelroCreationCompleted();
                        } catch (RemoteException e2) {
                            Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e2);
                        }
                        if (null == null) {
                            Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                        }
                        System.exit(0);
                        return;
                    }
                }
                access$000 = WebViewLibraryLoader.LOGTAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid RelroFileCreator args: ");
                stringBuilder.append(Arrays.toString(args));
                Log.e(access$000, stringBuilder.toString());
            } finally {
                try {
                    WebViewFactory.getUpdateServiceUnchecked().notifyRelroCreationCompleted();
                } catch (RemoteException e3) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "error notifying update service", e3);
                }
                if (null == null) {
                    Log.e(WebViewLibraryLoader.LOGTAG, "failed to create relro file");
                }
                System.exit(0);
            }
        }
    }

    @VisibleForTesting
    public static class WebViewNativeLibrary {
        public final String path;
        public final long size;

        WebViewNativeLibrary(String path, long size) {
            this.path = path;
            this.size = size;
        }
    }

    static native boolean nativeCreateRelroFile(String str, String str2);

    static native int nativeLoadWithRelroFile(String str, String str2, ClassLoader classLoader);

    static native boolean nativeReserveAddressSpace(long j);

    static void createRelroFile(boolean is64Bit, WebViewNativeLibrary nativeLib) {
        final String abi = is64Bit ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        Runnable crashHandler = new Runnable() {
            public void run() {
                try {
                    String access$000 = WebViewLibraryLoader.LOGTAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("relro file creator for ");
                    stringBuilder.append(abi);
                    stringBuilder.append(" crashed. Proceeding without");
                    Log.e(access$000, stringBuilder.toString());
                    WebViewFactory.getUpdateService().notifyRelroCreationCompleted();
                } catch (RemoteException e) {
                    String access$0002 = WebViewLibraryLoader.LOGTAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Cannot reach WebViewUpdateService. ");
                    stringBuilder2.append(e.getMessage());
                    Log.e(access$0002, stringBuilder2.toString());
                }
            }
        };
        if (nativeLib != null) {
            try {
                if (nativeLib.path != null) {
                    ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                    String name = RelroFileCreator.class.getName();
                    String[] strArr = new String[]{nativeLib.path};
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("WebViewLoader-");
                    stringBuilder.append(abi);
                    if (!activityManagerInternal.startIsolatedProcess(name, strArr, stringBuilder.toString(), abi, 1037, crashHandler)) {
                        throw new Exception("Failed to start the relro file creator process");
                    }
                    return;
                }
            } catch (Throwable t) {
                String str = LOGTAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error starting relro file creator for abi ");
                stringBuilder2.append(abi);
                Log.e(str, stringBuilder2.toString(), t);
                crashHandler.run();
                return;
            }
        }
        throw new IllegalArgumentException("Native library paths to the WebView RelRo process must not be null!");
    }

    static int prepareNativeLibraries(PackageInfo webviewPackageInfo) throws MissingWebViewPackageException {
        WebViewNativeLibrary nativeLib32bit = getWebViewNativeLibrary(webviewPackageInfo, null);
        WebViewNativeLibrary nativeLib64bit = getWebViewNativeLibrary(webviewPackageInfo, true);
        updateWebViewZygoteVmSize(nativeLib32bit, nativeLib64bit);
        return createRelros(nativeLib32bit, nativeLib64bit);
    }

    private static int createRelros(WebViewNativeLibrary nativeLib32bit, WebViewNativeLibrary nativeLib64bit) {
        int numRelros = 0;
        if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
            if (nativeLib32bit == null) {
                Log.e(LOGTAG, "No 32-bit WebView library path, skipping relro creation.");
            } else {
                createRelroFile(false, nativeLib32bit);
                numRelros = 0 + 1;
            }
        }
        if (Build.SUPPORTED_64_BIT_ABIS.length <= 0) {
            return numRelros;
        }
        if (nativeLib64bit == null) {
            Log.e(LOGTAG, "No 64-bit WebView library path, skipping relro creation.");
            return numRelros;
        }
        createRelroFile(true, nativeLib64bit);
        return numRelros + 1;
    }

    private static void updateWebViewZygoteVmSize(WebViewNativeLibrary nativeLib32bit, WebViewNativeLibrary nativeLib64bit) throws MissingWebViewPackageException {
        long newVmSize = 0;
        if (nativeLib32bit != null) {
            newVmSize = Math.max(0, nativeLib32bit.size);
        }
        if (nativeLib64bit != null) {
            newVmSize = Math.max(newVmSize, nativeLib64bit.size);
        }
        newVmSize = Math.max(2 * newVmSize, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
        String str = LOGTAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting new address space to ");
        stringBuilder.append(newVmSize);
        Log.d(str, stringBuilder.toString());
        setWebViewZygoteVmSize(newVmSize);
    }

    static void reserveAddressSpaceInZygote() {
        System.loadLibrary("webviewchromium_loader");
        long addressSpaceToReserve = SystemProperties.getLong(WebViewFactory.CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, CHROMIUM_WEBVIEW_DEFAULT_VMSIZE_BYTES);
        sAddressSpaceReserved = nativeReserveAddressSpace(addressSpaceToReserve);
        if (!sAddressSpaceReserved) {
            String str = LOGTAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reserving ");
            stringBuilder.append(addressSpaceToReserve);
            stringBuilder.append(" bytes of address space failed");
            Log.e(str, stringBuilder.toString());
        }
    }

    public static int loadNativeLibrary(ClassLoader clazzLoader, String libraryFileName) {
        if (sAddressSpaceReserved) {
            String relroPath;
            if (VMRuntime.getRuntime().is64Bit()) {
                relroPath = CHROMIUM_WEBVIEW_NATIVE_RELRO_64;
            } else {
                relroPath = CHROMIUM_WEBVIEW_NATIVE_RELRO_32;
            }
            int result = nativeLoadWithRelroFile(libraryFileName, relroPath, clazzLoader);
            if (result != 0) {
                Log.w(LOGTAG, "failed to load with relro file, proceeding without");
            }
            return result;
        }
        Log.e(LOGTAG, "can't load with relro file; address space not reserved");
        return 2;
    }

    @VisibleForTesting
    public static WebViewNativeLibrary getWebViewNativeLibrary(PackageInfo packageInfo, boolean is64bit) throws MissingWebViewPackageException {
        ApplicationInfo ai = packageInfo.applicationInfo;
        return findNativeLibrary(ai, WebViewFactory.getWebViewLibrary(ai), is64bit ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS, getWebViewNativeLibraryDirectory(ai, is64bit));
    }

    @VisibleForTesting
    public static String getWebViewNativeLibraryDirectory(ApplicationInfo ai, boolean is64bit) {
        if (is64bit == VMRuntime.is64BitAbi(ai.primaryCpuAbi)) {
            return ai.nativeLibraryDir;
        }
        if (TextUtils.isEmpty(ai.secondaryCpuAbi)) {
            return "";
        }
        return ai.secondaryNativeLibraryDir;
    }

    private static WebViewNativeLibrary findNativeLibrary(ApplicationInfo ai, String nativeLibFileName, String[] abiList, String libDirectory) throws MissingWebViewPackageException {
        if (TextUtils.isEmpty(libDirectory)) {
            return null;
        }
        String libPath = new StringBuilder();
        libPath.append(libDirectory);
        libPath.append("/");
        libPath.append(nativeLibFileName);
        libPath = libPath.toString();
        File f = new File(libPath);
        if (f.exists()) {
            return new WebViewNativeLibrary(libPath, f.length());
        }
        return getLoadFromApkPath(ai.sourceDir, abiList, nativeLibFileName);
    }

    private static WebViewNativeLibrary getLoadFromApkPath(String apkPath, String[] abiList, String nativeLibFileName) throws MissingWebViewPackageException {
        ZipFile z;
        try {
            z = new ZipFile(apkPath);
            int length = abiList.length;
            int i = 0;
            while (i < length) {
                String abi = abiList[i];
                String entry = new StringBuilder();
                entry.append("lib/");
                entry.append(abi);
                entry.append("/");
                entry.append(nativeLibFileName);
                entry = entry.toString();
                ZipEntry e = z.getEntry(entry);
                if (e == null || e.getMethod() != 0) {
                    i++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(apkPath);
                    stringBuilder.append("!/");
                    stringBuilder.append(entry);
                    WebViewNativeLibrary webViewNativeLibrary = new WebViewNativeLibrary(stringBuilder.toString(), e.getSize());
                    z.close();
                    return webViewNativeLibrary;
                }
            }
            z.close();
            return null;
        } catch (IOException e2) {
            throw new MissingWebViewPackageException(e2);
        } catch (Throwable th) {
            r1.addSuppressed(th);
        }
    }

    private static void setWebViewZygoteVmSize(long vmSize) {
        SystemProperties.set(WebViewFactory.CHROMIUM_WEBVIEW_VMSIZE_SIZE_PROPERTY, Long.toString(vmSize));
    }
}
