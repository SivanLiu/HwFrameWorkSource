package android.net;

import android.content.Context;
import android.util.Log;
import com.android.org.conscrypt.ClientSessionContext;
import com.android.org.conscrypt.FileClientSessionCache;
import com.android.org.conscrypt.SSLClientSessionCache;
import java.io.File;
import java.io.IOException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;

public final class SSLSessionCache {
    private static final String TAG = "SSLSessionCache";
    final SSLClientSessionCache mSessionCache;

    public static void install(SSLSessionCache cache, SSLContext context) {
        SSLSessionContext clientContext = context.getClientSessionContext();
        if (clientContext instanceof ClientSessionContext) {
            ((ClientSessionContext) clientContext).setPersistentCache(cache == null ? null : cache.mSessionCache);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Incompatible SSLContext: ");
        stringBuilder.append(context);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public SSLSessionCache(Object cache) {
        this.mSessionCache = (SSLClientSessionCache) cache;
    }

    public SSLSessionCache(File dir) throws IOException {
        this.mSessionCache = FileClientSessionCache.usingDirectory(dir);
    }

    public SSLSessionCache(Context context) {
        File dir = context.getDir("sslcache", 0);
        SSLClientSessionCache cache = null;
        try {
            cache = FileClientSessionCache.usingDirectory(dir);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to create SSL session cache in ");
            stringBuilder.append(dir);
            Log.w(str, stringBuilder.toString(), e);
        }
        this.mSessionCache = cache;
    }
}
