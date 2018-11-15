package com.android.server.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.webkit.UserPackage;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import java.io.PrintWriter;
import java.util.List;

public class WebViewUpdateServiceImpl {
    private static final int MULTIPROCESS_SETTING_OFF_VALUE = Integer.MIN_VALUE;
    private static final int MULTIPROCESS_SETTING_ON_VALUE = Integer.MAX_VALUE;
    private static final String TAG = WebViewUpdateServiceImpl.class.getSimpleName();
    private final Context mContext;
    private SystemInterface mSystemInterface;
    private WebViewUpdater mWebViewUpdater = new WebViewUpdater(this.mContext, this.mSystemInterface);

    public WebViewUpdateServiceImpl(Context context, SystemInterface systemInterface) {
        this.mContext = context;
        this.mSystemInterface = systemInterface;
    }

    void packageStateChanged(String packageName, int changedState, int userId) {
        updateFallbackStateOnPackageChange(packageName, changedState);
        this.mWebViewUpdater.packageStateChanged(packageName, changedState);
    }

    void prepareWebViewInSystemServer() {
        updateFallbackStateOnBoot();
        this.mWebViewUpdater.prepareWebViewInSystemServer();
        this.mSystemInterface.notifyZygote(isMultiProcessEnabled());
    }

    private boolean existsValidNonFallbackProvider(WebViewProviderInfo[] providers) {
        for (WebViewProviderInfo provider : providers) {
            if (provider.availableByDefault && !provider.isFallback) {
                List<UserPackage> userPackages = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, provider);
                if (WebViewUpdater.isInstalledAndEnabledForAllUsers(userPackages) && this.mWebViewUpdater.isValidProvider(provider, ((UserPackage) userPackages.get(0)).getPackageInfo())) {
                    return true;
                }
            }
        }
        return false;
    }

    void handleNewUser(int userId) {
        if (userId != 0) {
            handleUserChange();
        }
    }

    void handleUserRemoved(int userId) {
        handleUserChange();
    }

    private void handleUserChange() {
        if (this.mSystemInterface.isFallbackLogicEnabled()) {
            updateFallbackState(this.mSystemInterface.getWebViewPackages());
        }
        this.mWebViewUpdater.updateCurrentWebViewPackage(null);
    }

    void notifyRelroCreationCompleted() {
        this.mWebViewUpdater.notifyRelroCreationCompleted();
    }

    WebViewProviderResponse waitForAndGetProvider() {
        return this.mWebViewUpdater.waitForAndGetProvider();
    }

    String changeProviderAndSetting(String newProvider) {
        return this.mWebViewUpdater.changeProviderAndSetting(newProvider);
    }

    WebViewProviderInfo[] getValidWebViewPackages() {
        return this.mWebViewUpdater.getValidWebViewPackages();
    }

    WebViewProviderInfo[] getWebViewPackages() {
        return this.mSystemInterface.getWebViewPackages();
    }

    PackageInfo getCurrentWebViewPackage() {
        return this.mWebViewUpdater.getCurrentWebViewPackage();
    }

    void enableFallbackLogic(boolean enable) {
        this.mSystemInterface.enableFallbackLogic(enable);
    }

    private void updateFallbackStateOnBoot() {
        if (this.mSystemInterface.isFallbackLogicEnabled()) {
            updateFallbackState(this.mSystemInterface.getWebViewPackages());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x002a  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0029 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateFallbackStateOnPackageChange(String changedPackage, int changedState) {
        if (this.mSystemInterface.isFallbackLogicEnabled()) {
            WebViewProviderInfo[] webviewProviders = this.mSystemInterface.getWebViewPackages();
            boolean changedPackageAvailableByDefault = false;
            for (WebViewProviderInfo provider : webviewProviders) {
                if (provider.packageName.equals(changedPackage)) {
                    if (provider.availableByDefault) {
                        changedPackageAvailableByDefault = true;
                    }
                    if (!changedPackageAvailableByDefault) {
                        updateFallbackState(webviewProviders);
                        return;
                    }
                    return;
                }
            }
            if (!changedPackageAvailableByDefault) {
            }
        }
    }

    private void updateFallbackState(WebViewProviderInfo[] webviewProviders) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Number of webviewProviders in config_webview_packages.xml is = : ");
        stringBuilder.append(webviewProviders.length);
        stringBuilder.append(" , 6 means oversea version; 1 means china version");
        Log.i(str, stringBuilder.toString());
        WebViewProviderInfo fallbackProvider = getFallbackProvider(webviewProviders);
        if (fallbackProvider != null) {
            boolean existsValidNonFallbackProvider = existsValidNonFallbackProvider(webviewProviders);
            List<UserPackage> userPackages = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, fallbackProvider);
            if (existsValidNonFallbackProvider && !isDisabledForAllUsers(userPackages)) {
                Log.i(TAG, "uninstallAndDisable com.google.android.webview for all users");
                this.mSystemInterface.uninstallAndDisablePackageForAllUsers(this.mContext, fallbackProvider.packageName);
            } else if (!(existsValidNonFallbackProvider || WebViewUpdater.isInstalledAndEnabledForAllUsers(userPackages))) {
                Log.i(TAG, "Enable com.google.android.webview for all users");
                this.mSystemInterface.enablePackageForAllUsers(this.mContext, fallbackProvider.packageName, true);
            }
        }
    }

    private static WebViewProviderInfo getFallbackProvider(WebViewProviderInfo[] webviewPackages) {
        for (WebViewProviderInfo provider : webviewPackages) {
            if (provider.isFallback) {
                return provider;
            }
        }
        return null;
    }

    boolean isFallbackPackage(String packageName) {
        boolean z = false;
        if (packageName == null || !this.mSystemInterface.isFallbackLogicEnabled()) {
            return false;
        }
        WebViewProviderInfo fallbackProvider = getFallbackProvider(this.mSystemInterface.getWebViewPackages());
        if (fallbackProvider != null && packageName.equals(fallbackProvider.packageName)) {
            z = true;
        }
        return z;
    }

    boolean isMultiProcessEnabled() {
        int settingValue = this.mSystemInterface.getMultiProcessSetting(this.mContext);
        boolean z = false;
        if (this.mSystemInterface.isMultiProcessDefaultEnabled()) {
            if (settingValue > Integer.MIN_VALUE) {
                z = true;
            }
            return z;
        }
        if (settingValue >= Integer.MAX_VALUE) {
            z = true;
        }
        return z;
    }

    void enableMultiProcess(boolean enable) {
        PackageInfo current = getCurrentWebViewPackage();
        this.mSystemInterface.setMultiProcessSetting(this.mContext, enable ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        this.mSystemInterface.notifyZygote(enable);
        if (current != null) {
            this.mSystemInterface.killPackageDependents(current.packageName);
        }
    }

    private static boolean isDisabledForAllUsers(List<UserPackage> userPackages) {
        for (UserPackage userPackage : userPackages) {
            if (userPackage.getPackageInfo() != null && userPackage.isEnabledPackage()) {
                return false;
            }
        }
        return true;
    }

    void dumpState(PrintWriter pw) {
        pw.println("Current WebView Update Service state");
        pw.println(String.format("  Fallback logic enabled: %b", new Object[]{Boolean.valueOf(this.mSystemInterface.isFallbackLogicEnabled())}));
        pw.println(String.format("  Multiprocess enabled: %b", new Object[]{Boolean.valueOf(isMultiProcessEnabled())}));
        this.mWebViewUpdater.dumpState(pw);
    }
}
