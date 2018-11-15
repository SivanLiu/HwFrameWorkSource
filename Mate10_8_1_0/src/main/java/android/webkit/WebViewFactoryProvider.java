package android.webkit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView.PrivateAccess;
import java.util.List;

public interface WebViewFactoryProvider {

    public interface Statics {
        void clearClientCertPreferences(Runnable runnable);

        void enableSlowWholeDocumentDraw();

        String findAddress(String str);

        void freeMemoryForTests();

        String getDefaultUserAgent(Context context);

        Uri getSafeBrowsingPrivacyPolicyUrl();

        void initSafeBrowsing(Context context, ValueCallback<Boolean> valueCallback);

        Uri[] parseFileChooserResult(int i, Intent intent);

        void setSafeBrowsingWhitelist(List<String> list, ValueCallback<Boolean> valueCallback);

        void setWebContentsDebuggingEnabled(boolean z);
    }

    WebViewProvider createWebView(WebView webView, PrivateAccess privateAccess);

    CookieManager getCookieManager();

    GeolocationPermissions getGeolocationPermissions();

    ServiceWorkerController getServiceWorkerController();

    Statics getStatics();

    TokenBindingService getTokenBindingService();

    WebIconDatabase getWebIconDatabase();

    WebStorage getWebStorage();

    WebViewDatabase getWebViewDatabase(Context context);
}
