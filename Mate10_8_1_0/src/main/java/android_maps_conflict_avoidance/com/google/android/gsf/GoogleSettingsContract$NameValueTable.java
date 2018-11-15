package android_maps_conflict_avoidance.com.google.android.gsf;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.BaseColumns;
import java.util.HashMap;

public class GoogleSettingsContract$NameValueTable implements BaseColumns {
    static HashMap<Uri, GoogleSettingsContract$UriCacheValue> sCache = new HashMap();

    private static GoogleSettingsContract$UriCacheValue ensureCacheInitializedLocked(ContentResolver cr, Uri uri) {
        GoogleSettingsContract$UriCacheValue cacheValue = (GoogleSettingsContract$UriCacheValue) sCache.get(uri);
        if (cacheValue == null) {
            cacheValue = new GoogleSettingsContract$UriCacheValue();
            sCache.put(uri, cacheValue);
            GoogleSettingsContract$UriCacheValue finalCacheValue = cacheValue;
            cr.registerContentObserver(uri, true, new ContentObserver(null) {
                public void onChange(boolean selfChange) {
                    cacheValue.invalidateCache.set(true);
                }
            });
            return cacheValue;
        } else if (!cacheValue.invalidateCache.getAndSet(false)) {
            return cacheValue;
        } else {
            synchronized (cacheValue) {
                cacheValue.valueCache.clear();
                cacheValue.versionToken = new Object();
            }
            return cacheValue;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected static String getString(ContentResolver resolver, Uri uri, String name) {
        synchronized (GoogleSettingsContract$NameValueTable.class) {
            GoogleSettingsContract$UriCacheValue cacheValue = ensureCacheInitializedLocked(resolver, uri);
        }
        synchronized (cacheValue) {
            Object version = cacheValue.versionToken;
            if (cacheValue.valueCache.containsKey(name)) {
                String str = (String) cacheValue.valueCache.get(name);
                return str;
            }
        }
    }

    private static void putCache(GoogleSettingsContract$UriCacheValue cacheValue, Object version, String key, String value) {
        synchronized (cacheValue) {
            if (version == cacheValue.versionToken) {
                cacheValue.valueCache.put(key, value);
            }
        }
    }
}
