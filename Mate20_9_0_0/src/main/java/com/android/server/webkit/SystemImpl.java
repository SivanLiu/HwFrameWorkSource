package com.android.server.webkit;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver.Stub;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewZygote;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.DumpState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class SystemImpl implements SystemInterface {
    private static final int PACKAGE_FLAGS = 272630976;
    private static final String TAG = SystemImpl.class.getSimpleName();
    private static final String TAG_AVAILABILITY = "availableByDefault";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_FALLBACK = "isFallback";
    private static final String TAG_PACKAGE_NAME = "packageName";
    private static final String TAG_SIGNATURE = "signature";
    private static final String TAG_START = "webviewproviders";
    private static final String TAG_WEBVIEW_PROVIDER = "webviewprovider";
    private final WebViewProviderInfo[] mWebViewProviderPackages;

    private static class LazyHolder {
        private static final SystemImpl INSTANCE = new SystemImpl();

        private LazyHolder() {
        }
    }

    /* synthetic */ SystemImpl(AnonymousClass1 x0) {
        this();
    }

    public static SystemImpl getInstance() {
        return LazyHolder.INSTANCE;
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00e0 A:{PHI: r3 , Splitter: B:1:0x000c, ExcHandler: org.xmlpull.v1.XmlPullParserException (r5_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:45:0x00e0, code:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:47:?, code:
            r7 = new java.lang.StringBuilder();
            r7.append("Error when parsing WebView config ");
            r7.append(r5);
     */
    /* JADX WARNING: Missing block: B:48:0x00f7, code:
            throw new android.util.AndroidRuntimeException(r7.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SystemImpl() {
        int numFallbackPackages = 0;
        int numAvailableByDefaultPackages = 0;
        int numAvByDefaultAndNotFallback = 0;
        XmlResourceParser parser = null;
        List<WebViewProviderInfo> webViewProviders = new ArrayList();
        try {
            parser = AppGlobals.getInitialApplication().getResources().getXml(18284550);
            XmlUtils.beginDocument(parser, TAG_START);
            while (true) {
                XmlUtils.nextElement(parser);
                String element = parser.getName();
                if (element == null) {
                    if (parser != null) {
                        parser.close();
                    }
                    if (numAvailableByDefaultPackages == 0) {
                        throw new AndroidRuntimeException("There must be at least one WebView package that is available by default");
                    } else if (numAvByDefaultAndNotFallback != 0) {
                        this.mWebViewProviderPackages = (WebViewProviderInfo[]) webViewProviders.toArray(new WebViewProviderInfo[webViewProviders.size()]);
                        return;
                    } else {
                        throw new AndroidRuntimeException("There must be at least one WebView package that is available by default and not a fallback");
                    }
                } else if (element.equals(TAG_WEBVIEW_PROVIDER)) {
                    String packageName = parser.getAttributeValue(null, "packageName");
                    if (packageName != null) {
                        String description = parser.getAttributeValue(null, TAG_DESCRIPTION);
                        if (description != null) {
                            WebViewProviderInfo webViewProviderInfo = new WebViewProviderInfo(packageName, description, "true".equals(parser.getAttributeValue(null, TAG_AVAILABILITY)), "true".equals(parser.getAttributeValue(null, TAG_FALLBACK)), readSignatures(parser));
                            if (webViewProviderInfo.isFallback) {
                                numFallbackPackages++;
                                if (!webViewProviderInfo.availableByDefault) {
                                    throw new AndroidRuntimeException("Each WebView fallback package must be available by default.");
                                } else if (numFallbackPackages > 1) {
                                    throw new AndroidRuntimeException("There can be at most one WebView fallback package.");
                                }
                            }
                            if (webViewProviderInfo.availableByDefault) {
                                numAvailableByDefaultPackages++;
                                if (!webViewProviderInfo.isFallback) {
                                    numAvByDefaultAndNotFallback++;
                                }
                            }
                            webViewProviders.add(webViewProviderInfo);
                        } else {
                            throw new AndroidRuntimeException("WebView provider in framework resources missing description");
                        }
                    }
                    throw new AndroidRuntimeException("WebView provider in framework resources missing package name");
                } else {
                    Log.e(TAG, "Found an element that is not a WebView provider");
                }
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public WebViewProviderInfo[] getWebViewPackages() {
        return this.mWebViewProviderPackages;
    }

    public long getFactoryPackageVersion(String packageName) throws NameNotFoundException {
        return AppGlobals.getInitialApplication().getPackageManager().getPackageInfo(packageName, DumpState.DUMP_COMPILER_STATS).getLongVersionCode();
    }

    private static String[] readSignatures(XmlResourceParser parser) throws IOException, XmlPullParserException {
        List<String> signatures = new ArrayList();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_SIGNATURE)) {
                signatures.add(parser.nextText());
            } else {
                Log.e(TAG, "Found an element in a webview provider that is not a signature");
            }
        }
        return (String[]) signatures.toArray(new String[signatures.size()]);
    }

    public int onWebViewProviderChanged(PackageInfo packageInfo) {
        return WebViewFactory.onWebViewProviderChanged(packageInfo);
    }

    public String getUserChosenWebViewProvider(Context context) {
        return Global.getString(context.getContentResolver(), "webview_provider");
    }

    public void updateUserSetting(Context context, String newProviderName) {
        String str;
        ContentResolver contentResolver = context.getContentResolver();
        String str2 = "webview_provider";
        if (newProviderName == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else {
            str = newProviderName;
        }
        Global.putString(contentResolver, str2, str);
    }

    public void killPackageDependents(String packageName) {
        try {
            ActivityManager.getService().killPackageDependents(packageName, -1);
        } catch (RemoteException e) {
        }
    }

    public boolean isFallbackLogicEnabled() {
        return Global.getInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", 1) == 1;
    }

    public void enableFallbackLogic(boolean enable) {
        Global.putInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", enable);
    }

    public void uninstallAndDisablePackageForAllUsers(final Context context, String packageName) {
        enablePackageForAllUsers(context, packageName, false);
        try {
            PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            if (applicationInfo != null && applicationInfo.isUpdatedSystemApp()) {
                pm.deletePackage(packageName, new Stub() {
                    public void packageDeleted(String packageName, int returnCode) {
                        SystemImpl.this.enablePackageForAllUsers(context, packageName, false);
                    }
                }, 6);
            }
        } catch (NameNotFoundException e) {
        }
    }

    public void enablePackageForAllUsers(Context context, String packageName, boolean enable) {
        for (UserInfo userInfo : ((UserManager) context.getSystemService("user")).getUsers()) {
            enablePackageForUser(packageName, enable, userInfo.id);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:7:0x0013 A:{Splitter: B:0:0x0000, ExcHandler: android.os.RemoteException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:7:0x0013, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:8:0x0014, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Tried to ");
     */
    /* JADX WARNING: Missing block: B:9:0x0020, code:
            if (r8 != false) goto L_0x0022;
     */
    /* JADX WARNING: Missing block: B:10:0x0022, code:
            r3 = "enable ";
     */
    /* JADX WARNING: Missing block: B:11:0x0025, code:
            r3 = "disable ";
     */
    /* JADX WARNING: Missing block: B:12:0x0027, code:
            r2.append(r3);
            r2.append(r7);
            r2.append(" for user ");
            r2.append(r9);
            r2.append(": ");
            r2.append(r0);
            android.util.Log.w(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void enablePackageForUser(String packageName, boolean enable, int userId) {
        try {
            int i;
            IPackageManager packageManager = AppGlobals.getPackageManager();
            if (enable) {
                i = 0;
            } else {
                i = 3;
            }
            packageManager.setApplicationEnabledSetting(packageName, i, 0, userId, null);
        } catch (Exception e) {
        }
    }

    public boolean systemIsDebuggable() {
        return Build.IS_DEBUGGABLE;
    }

    public PackageInfo getPackageInfoForProvider(WebViewProviderInfo configInfo) throws NameNotFoundException {
        return AppGlobals.getInitialApplication().getPackageManager().getPackageInfo(configInfo.packageName, PACKAGE_FLAGS);
    }

    public List<UserPackage> getPackageInfoForProviderAllUsers(Context context, WebViewProviderInfo configInfo) {
        return UserPackage.getPackageInfosAllUsers(context, configInfo.packageName, PACKAGE_FLAGS);
    }

    public int getMultiProcessSetting(Context context) {
        return Global.getInt(context.getContentResolver(), "webview_multiprocess", 0);
    }

    public void setMultiProcessSetting(Context context, int value) {
        Global.putInt(context.getContentResolver(), "webview_multiprocess", value);
    }

    public void notifyZygote(boolean enableMultiProcess) {
        WebViewZygote.setMultiprocessEnabled(enableMultiProcess);
    }

    public boolean isMultiProcessDefaultEnabled() {
        return Build.SUPPORTED_64_BIT_ABIS.length > 0 || !ActivityManager.isLowRamDeviceStatic();
    }
}
