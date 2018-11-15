package android_maps_conflict_avoidance.com.google.android.gsf;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.BaseColumns;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GoogleSettingsContract {

    public static class NameValueTable implements BaseColumns {
        static HashMap<Uri, UriCacheValue> sCache = new HashMap();

        private static UriCacheValue ensureCacheInitializedLocked(ContentResolver cr, Uri uri) {
            UriCacheValue cacheValue = (UriCacheValue) sCache.get(uri);
            if (cacheValue == null) {
                cacheValue = new UriCacheValue();
                sCache.put(uri, cacheValue);
                final UriCacheValue finalCacheValue = cacheValue;
                cr.registerContentObserver(uri, true, new ContentObserver(null) {
                    public void onChange(boolean selfChange) {
                        finalCacheValue.invalidateCache.set(true);
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

        /* JADX WARNING: Missing block: B:13:0x001e, code:
            r2 = null;
            r4 = null;
     */
        /* JADX WARNING: Missing block: B:15:?, code:
            r6 = r12;
            r7 = r13;
            r4 = r6.query(r7, new java.lang.String[]{"value"}, "name=?", new java.lang.String[]{r14}, null);
     */
        /* JADX WARNING: Missing block: B:16:0x0037, code:
            if (r4 == null) goto L_0x004e;
     */
        /* JADX WARNING: Missing block: B:18:0x003d, code:
            if (r4.moveToFirst() != false) goto L_0x0040;
     */
        /* JADX WARNING: Missing block: B:19:0x0040, code:
            r2 = r4.getString(0);
            putCache(r1, r0, r14, r2);
     */
        /* JADX WARNING: Missing block: B:20:0x0048, code:
            if (r4 == null) goto L_0x007c;
     */
        /* JADX WARNING: Missing block: B:21:0x004a, code:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:23:?, code:
            putCache(r1, r0, r14, null);
     */
        /* JADX WARNING: Missing block: B:24:0x0052, code:
            if (r4 == null) goto L_0x0057;
     */
        /* JADX WARNING: Missing block: B:25:0x0054, code:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:26:0x0057, code:
            return null;
     */
        /* JADX WARNING: Missing block: B:28:0x005a, code:
            r3 = move-exception;
     */
        /* JADX WARNING: Missing block: B:30:?, code:
            r6 = new java.lang.StringBuilder();
            r6.append("Can't get key ");
            r6.append(r14);
            r6.append(" from ");
            r6.append(r13);
            android.util.Log.e("GoogleSettings", r6.toString(), r3);
     */
        /* JADX WARNING: Missing block: B:31:0x0079, code:
            if (r4 == null) goto L_0x007c;
     */
        /* JADX WARNING: Missing block: B:32:0x007c, code:
            return r2;
     */
        /* JADX WARNING: Missing block: B:33:0x007d, code:
            if (r4 != null) goto L_0x007f;
     */
        /* JADX WARNING: Missing block: B:34:0x007f, code:
            r4.close();
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected static String getString(ContentResolver resolver, Uri uri, String name) {
            UriCacheValue cacheValue;
            synchronized (NameValueTable.class) {
                cacheValue = ensureCacheInitializedLocked(resolver, uri);
            }
            synchronized (cacheValue) {
                Object version = cacheValue.versionToken;
                if (cacheValue.valueCache.containsKey(name)) {
                    String str = (String) cacheValue.valueCache.get(name);
                    return str;
                }
            }
        }

        private static void putCache(UriCacheValue cacheValue, Object version, String key, String value) {
            synchronized (cacheValue) {
                if (version == cacheValue.versionToken) {
                    cacheValue.valueCache.put(key, value);
                }
            }
        }
    }

    static class UriCacheValue {
        AtomicBoolean invalidateCache = new AtomicBoolean(false);
        HashMap<String, String> valueCache = new HashMap();
        Object versionToken = new Object();

        UriCacheValue() {
        }
    }

    public static final class Partner extends NameValueTable {
        public static final Uri CONTENT_URI = Uri.parse("content://com.google.settings/partner");

        public static String getString(ContentResolver resolver, String name) {
            return NameValueTable.getString(resolver, CONTENT_URI, name);
        }

        public static String getString(ContentResolver resolver, String name, String defaultValue) {
            String value = getString(resolver, name);
            if (value == null) {
                return defaultValue;
            }
            return value;
        }
    }
}
