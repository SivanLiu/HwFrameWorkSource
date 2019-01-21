package android.webkit;

import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ChildZygoteProcess;
import android.os.Process;
import android.os.ZygoteProcess;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.File;
import java.util.ArrayList;

public class WebViewZygote {
    private static final String LOGTAG = "WebViewZygote";
    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static boolean sMultiprocessEnabled = false;
    @GuardedBy("sLock")
    private static PackageInfo sPackage;
    @GuardedBy("sLock")
    private static ApplicationInfo sPackageOriginalAppInfo;
    @GuardedBy("sLock")
    private static ChildZygoteProcess sZygote;

    public static ZygoteProcess getProcess() {
        synchronized (sLock) {
            ChildZygoteProcess childZygoteProcess;
            if (sZygote != null) {
                childZygoteProcess = sZygote;
                return childZygoteProcess;
            }
            connectToZygoteIfNeededLocked();
            childZygoteProcess = sZygote;
            return childZygoteProcess;
        }
    }

    public static String getPackageName() {
        synchronized (sLock) {
            if (sPackage == null) {
                Log.e(LOGTAG, "sPackage is NULL, return null");
                return null;
            }
            String str = sPackage.packageName;
            return str;
        }
    }

    public static boolean isMultiprocessEnabled() {
        boolean z;
        synchronized (sLock) {
            z = sMultiprocessEnabled && sPackage != null;
        }
        return z;
    }

    public static void setMultiprocessEnabled(boolean enabled) {
        synchronized (sLock) {
            sMultiprocessEnabled = enabled;
            if (enabled) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(-$$Lambda$xYTrYQCPf1HcdlWzDof3mq93ihs.INSTANCE);
            } else {
                stopZygoteLocked();
            }
        }
    }

    public static void onWebViewProviderChanged(PackageInfo packageInfo, ApplicationInfo originalAppInfo) {
        synchronized (sLock) {
            sPackage = packageInfo;
            sPackageOriginalAppInfo = originalAppInfo;
            if (sMultiprocessEnabled) {
                stopZygoteLocked();
                return;
            }
        }
    }

    @GuardedBy("sLock")
    private static void stopZygoteLocked() {
        if (sZygote != null) {
            sZygote.close();
            Process.killProcess(sZygote.getPid());
            sZygote = null;
        }
    }

    @GuardedBy("sLock")
    private static void connectToZygoteIfNeededLocked() {
        if (sZygote == null) {
            if (sPackage == null) {
                Log.e(LOGTAG, "Cannot connect to zygote, no package specified");
                return;
            }
            try {
                String str;
                String str2;
                sZygote = Process.zygoteProcess.startChildZygote("com.android.internal.os.WebViewZygoteInit", "webview_zygote", Process.WEBVIEW_ZYGOTE_UID, Process.WEBVIEW_ZYGOTE_UID, null, 0, "webview_zygote", sPackage.applicationInfo.primaryCpuAbi, null);
                Iterable zipPaths = new ArrayList(10);
                Iterable libPaths = new ArrayList(10);
                LoadedApk.makePaths(null, false, sPackage.applicationInfo, zipPaths, libPaths);
                String librarySearchPath = TextUtils.join(File.pathSeparator, libPaths);
                if (zipPaths.size() == 1) {
                    str = (String) zipPaths.get(0);
                } else {
                    str = TextUtils.join(File.pathSeparator, zipPaths);
                }
                String zip = str;
                String libFileName = WebViewFactory.getWebViewLibrary(sPackage.applicationInfo);
                LoadedApk.makePaths(null, false, sPackageOriginalAppInfo, zipPaths, null);
                if (zipPaths.size() == 1) {
                    str2 = (String) zipPaths.get(0);
                } else {
                    str2 = TextUtils.join(File.pathSeparator, zipPaths);
                }
                String cacheKey = str2;
                ZygoteProcess.waitForConnectionToZygote(sZygote.getPrimarySocketAddress());
                str2 = LOGTAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Preloading package ");
                stringBuilder.append(zip);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(librarySearchPath);
                Log.d(str2, stringBuilder.toString());
                sZygote.preloadPackageForAbi(zip, librarySearchPath, libFileName, cacheKey, Build.SUPPORTED_ABIS[0]);
            } catch (Exception e) {
                Log.e(LOGTAG, "Error connecting to webview zygote", e);
                stopZygoteLocked();
            }
        }
    }
}
