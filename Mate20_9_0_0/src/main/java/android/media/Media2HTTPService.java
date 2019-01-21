package android.media;

import android.util.Log;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.List;

public class Media2HTTPService {
    private static final String TAG = "Media2HTTPService";
    private Boolean mCookieStoreInitialized = new Boolean(false);
    private List<HttpCookie> mCookies;

    public Media2HTTPService(List<HttpCookie> cookies) {
        this.mCookies = cookies;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Media2HTTPService(");
        stringBuilder.append(this);
        stringBuilder.append("): Cookies: ");
        stringBuilder.append(cookies);
        Log.v(str, stringBuilder.toString());
    }

    public Media2HTTPConnection makeHTTPConnection() {
        synchronized (this.mCookieStoreInitialized) {
            if (!this.mCookieStoreInitialized.booleanValue()) {
                String str;
                StringBuilder stringBuilder;
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (cookieHandler == null) {
                    cookieHandler = new CookieManager();
                    CookieHandler.setDefault(cookieHandler);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("makeHTTPConnection: CookieManager created: ");
                    stringBuilder.append(cookieHandler);
                    Log.v(str, stringBuilder.toString());
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("makeHTTPConnection: CookieHandler (");
                    stringBuilder.append(cookieHandler);
                    stringBuilder.append(") exists.");
                    Log.v(str, stringBuilder.toString());
                }
                if (this.mCookies != null) {
                    if (cookieHandler instanceof CookieManager) {
                        CookieStore store = ((CookieManager) cookieHandler).getCookieStore();
                        for (HttpCookie cookie : this.mCookies) {
                            try {
                                store.add(null, cookie);
                            } catch (Exception e) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("makeHTTPConnection: CookieStore.add");
                                stringBuilder2.append(e);
                                Log.v(str2, stringBuilder2.toString());
                            }
                        }
                    } else {
                        Log.w(TAG, "makeHTTPConnection: The installed CookieHandler is not a CookieManager. Can’t add the provided cookies to the cookie store.");
                    }
                }
                this.mCookieStoreInitialized = Boolean.valueOf(true);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("makeHTTPConnection(");
                stringBuilder.append(this);
                stringBuilder.append("): cookieHandler: ");
                stringBuilder.append(cookieHandler);
                stringBuilder.append(" Cookies: ");
                stringBuilder.append(this.mCookies);
                Log.v(str, stringBuilder.toString());
            }
        }
        return new Media2HTTPConnection();
    }

    static Media2HTTPService createHTTPService(String path) {
        return createHTTPService(path, null);
    }

    static Media2HTTPService createHTTPService(String path, List<HttpCookie> cookies) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return new Media2HTTPService(cookies);
        }
        if (path.startsWith("widevine://")) {
            Log.d(TAG, "Widevine classic is no longer supported");
        }
        return null;
    }
}
