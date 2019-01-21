package android.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.Typeface.Builder;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.LruCache;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FontsContract {
    private static final long SYNC_FONT_FETCH_TIMEOUT_MS = 500;
    private static final String TAG = "FontsContract";
    private static final int THREAD_RENEWAL_THRESHOLD_MS = 10000;
    private static final Comparator<byte[]> sByteArrayComparator = -$$Lambda$FontsContract$3FDNQd-WsglsyDhif-aHVbzkfrA.INSTANCE;
    private static volatile Context sContext;
    @GuardedBy("sLock")
    private static Handler sHandler;
    @GuardedBy("sLock")
    private static Set<String> sInQueueSet;
    private static final Object sLock = new Object();
    private static final Runnable sReplaceDispatcherThreadRunnable = new Runnable() {
        public void run() {
            synchronized (FontsContract.sLock) {
                if (FontsContract.sThread != null) {
                    FontsContract.sThread.quitSafely();
                    FontsContract.sThread = null;
                    FontsContract.sHandler = null;
                }
            }
        }
    };
    @GuardedBy("sLock")
    private static HandlerThread sThread;
    private static final LruCache<String, Typeface> sTypefaceCache = new LruCache(16);

    public static class FontFamilyResult {
        public static final int STATUS_OK = 0;
        public static final int STATUS_REJECTED = 3;
        public static final int STATUS_UNEXPECTED_DATA_PROVIDED = 2;
        public static final int STATUS_WRONG_CERTIFICATES = 1;
        private final FontInfo[] mFonts;
        private final int mStatusCode;

        @Retention(RetentionPolicy.SOURCE)
        @interface FontResultStatus {
        }

        public FontFamilyResult(int statusCode, FontInfo[] fonts) {
            this.mStatusCode = statusCode;
            this.mFonts = fonts;
        }

        public int getStatusCode() {
            return this.mStatusCode;
        }

        public FontInfo[] getFonts() {
            return this.mFonts;
        }
    }

    public static class FontInfo {
        private final FontVariationAxis[] mAxes;
        private final boolean mItalic;
        private final int mResultCode;
        private final int mTtcIndex;
        private final Uri mUri;
        private final int mWeight;

        public FontInfo(Uri uri, int ttcIndex, FontVariationAxis[] axes, int weight, boolean italic, int resultCode) {
            this.mUri = (Uri) Preconditions.checkNotNull(uri);
            this.mTtcIndex = ttcIndex;
            this.mAxes = axes;
            this.mWeight = weight;
            this.mItalic = italic;
            this.mResultCode = resultCode;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public int getTtcIndex() {
            return this.mTtcIndex;
        }

        public FontVariationAxis[] getAxes() {
            return this.mAxes;
        }

        public int getWeight() {
            return this.mWeight;
        }

        public boolean isItalic() {
            return this.mItalic;
        }

        public int getResultCode() {
            return this.mResultCode;
        }
    }

    public static class FontRequestCallback {
        public static final int FAIL_REASON_FONT_LOAD_ERROR = -3;
        public static final int FAIL_REASON_FONT_NOT_FOUND = 1;
        public static final int FAIL_REASON_FONT_UNAVAILABLE = 2;
        public static final int FAIL_REASON_MALFORMED_QUERY = 3;
        public static final int FAIL_REASON_PROVIDER_NOT_FOUND = -1;
        public static final int FAIL_REASON_WRONG_CERTIFICATES = -2;

        @Retention(RetentionPolicy.SOURCE)
        @interface FontRequestFailReason {
        }

        public void onTypefaceRetrieved(Typeface typeface) {
        }

        public void onTypefaceRequestFailed(int reason) {
        }
    }

    public static final class Columns implements BaseColumns {
        public static final String FILE_ID = "file_id";
        public static final String ITALIC = "font_italic";
        public static final String RESULT_CODE = "result_code";
        public static final int RESULT_CODE_FONT_NOT_FOUND = 1;
        public static final int RESULT_CODE_FONT_UNAVAILABLE = 2;
        public static final int RESULT_CODE_MALFORMED_QUERY = 3;
        public static final int RESULT_CODE_OK = 0;
        public static final String TTC_INDEX = "font_ttc_index";
        public static final String VARIATION_SETTINGS = "font_variation_settings";
        public static final String WEIGHT = "font_weight";

        private Columns() {
        }
    }

    private FontsContract() {
    }

    public static void setApplicationContextForResources(Context context) {
        sContext = context.getApplicationContext();
    }

    public static Typeface getFontSync(FontRequest request) {
        Throwable th;
        String str;
        Typeface typeface;
        String id = request.getIdentifier();
        Typeface cachedTypeface = (Typeface) sTypefaceCache.get(id);
        if (cachedTypeface != null) {
            return cachedTypeface;
        }
        synchronized (sLock) {
            try {
                if (sHandler == null) {
                    try {
                        sThread = new HandlerThread("fonts", 10);
                        sThread.start();
                        sHandler = new Handler(sThread.getLooper());
                    } catch (Throwable th2) {
                        th = th2;
                        str = id;
                        typeface = cachedTypeface;
                    }
                }
                Lock lock = new ReentrantLock();
                Condition cond = lock.newCondition();
                AtomicReference<Typeface> holder = new AtomicReference();
                AtomicBoolean waiting = new AtomicBoolean(true);
                AtomicBoolean timeout = new AtomicBoolean(false);
                Handler handler = sHandler;
                -$$Lambda$FontsContract$rqfIZKvP1frnI9vP1hVA8jQN_RE -__lambda_fontscontract_rqfizkvp1frni9vp1hva8jqn_re = -__lambda_fontscontract_rqfizkvp1frni9vp1hva8jqn_re;
                String str2 = id;
                -$$Lambda$FontsContract$rqfIZKvP1frnI9vP1hVA8jQN_RE -__lambda_fontscontract_rqfizkvp1frni9vp1hva8jqn_re2 = -__lambda_fontscontract_rqfizkvp1frni9vp1hva8jqn_re;
                AtomicBoolean id2 = timeout;
                AtomicBoolean cachedTypeface2 = waiting;
                try {
                    -__lambda_fontscontract_rqfizkvp1frni9vp1hva8jqn_re = new -$$Lambda$FontsContract$rqfIZKvP1frnI9vP1hVA8jQN_RE(request, str2, holder, lock, timeout, waiting, cond);
                    handler.post(-__lambda_fontscontract_rqfizkvp1frni9vp1hva8jqn_re2);
                    sHandler.removeCallbacks(sReplaceDispatcherThreadRunnable);
                    sHandler.postDelayed(sReplaceDispatcherThreadRunnable, 10000);
                    long remaining = TimeUnit.MILLISECONDS.toNanos(500);
                    lock.lock();
                    Typeface typeface2;
                    if (cachedTypeface2.get()) {
                        do {
                            try {
                                remaining = cond.awaitNanos(remaining);
                            } catch (InterruptedException e) {
                            }
                            if (!cachedTypeface2.get()) {
                                typeface2 = (Typeface) holder.get();
                                lock.unlock();
                                return typeface2;
                            }
                        } while (remaining > 0);
                        id2.set(true);
                        String str3 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Remote font fetch timed out: ");
                        stringBuilder.append(request.getProviderAuthority());
                        stringBuilder.append("/");
                        stringBuilder.append(request.getQuery());
                        Log.w(str3, stringBuilder.toString());
                        lock.unlock();
                        return null;
                    }
                    typeface2 = (Typeface) holder.get();
                    lock.unlock();
                    return typeface2;
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                str = id;
                typeface = cachedTypeface;
                throw th;
            }
        }
    }

    static /* synthetic */ void lambda$getFontSync$0(FontRequest request, String id, AtomicReference holder, Lock lock, AtomicBoolean timeout, AtomicBoolean waiting, Condition cond) {
        try {
            FontFamilyResult result = fetchFonts(sContext, null, request);
            if (result.getStatusCode() == 0) {
                Typeface typeface = buildTypeface(sContext, null, result.getFonts());
                if (typeface != null) {
                    sTypefaceCache.put(id, typeface);
                }
                holder.set(typeface);
            }
        } catch (NameNotFoundException e) {
            Log.w(TAG, "find NameNotFoundException.");
        }
        lock.lock();
        try {
            if (!timeout.get()) {
                waiting.set(false);
                cond.signal();
            }
            lock.unlock();
        } catch (Throwable th) {
            lock.unlock();
        }
    }

    public static void requestFonts(Context context, FontRequest request, Handler handler, CancellationSignal cancellationSignal, FontRequestCallback callback) {
        Handler callerThreadHandler = new Handler();
        Typeface cachedTypeface = (Typeface) sTypefaceCache.get(request.getIdentifier());
        if (cachedTypeface != null) {
            callerThreadHandler.post(new -$$Lambda$FontsContract$p_tsXYYYpEH0-EJSp2uPrJ33dkU(callback, cachedTypeface));
        } else {
            handler.post(new -$$Lambda$FontsContract$dFs2m4XF5xdir4W3T-ncUQAVX8k(context, cancellationSignal, request, callerThreadHandler, callback));
        }
    }

    static /* synthetic */ void lambda$requestFonts$12(Context context, CancellationSignal cancellationSignal, FontRequest request, Handler callerThreadHandler, FontRequestCallback callback) {
        try {
            FontFamilyResult result = fetchFonts(context, cancellationSignal, request);
            Typeface anotherCachedTypeface = (Typeface) sTypefaceCache.get(request.getIdentifier());
            if (anotherCachedTypeface != null) {
                callerThreadHandler.post(new -$$Lambda$FontsContract$xDMhIK5JxjXFDIXBeQbZ_hdXTBc(callback, anotherCachedTypeface));
            } else if (result.getStatusCode() != 0) {
                switch (result.getStatusCode()) {
                    case 1:
                        callerThreadHandler.post(new -$$Lambda$FontsContract$YhiTIVckhFBdgNR2V1bGY3Q1Nqg(callback));
                        return;
                    case 2:
                        callerThreadHandler.post(new -$$Lambda$FontsContract$FCawscMFN_8Qxcb2EdA5gdE-O2k(callback));
                        return;
                    default:
                        callerThreadHandler.post(new -$$Lambda$FontsContract$DV4gvjPxJzdQvcfoIJqGrzFtTQs(callback));
                        return;
                }
            } else {
                FontInfo[] fonts = result.getFonts();
                if (fonts == null || fonts.length == 0) {
                    callerThreadHandler.post(new -$$Lambda$FontsContract$LJ3jfZobcxq5xTMmb88GlM1r9Jk(callback));
                    return;
                }
                int resultCode;
                for (FontInfo font : fonts) {
                    if (font.getResultCode() != 0) {
                        resultCode = font.getResultCode();
                        if (resultCode < 0) {
                            callerThreadHandler.post(new -$$Lambda$FontsContract$Qvl9aVA7txTF3tFcFbbKD_nWpuM(callback));
                        } else {
                            callerThreadHandler.post(new -$$Lambda$FontsContract$rvEOORTXb3mMYTLkoH9nlHQr9Iw(callback, resultCode));
                        }
                        return;
                    }
                }
                Typeface typeface = buildTypeface(context, cancellationSignal, fonts);
                if (typeface == null) {
                    callerThreadHandler.post(new -$$Lambda$FontsContract$rqmVfWYeZ5NL5MtBx5LOdhNAOP4(callback));
                    return;
                }
                sTypefaceCache.put(request.getIdentifier(), typeface);
                callerThreadHandler.post(new -$$Lambda$FontsContract$gJeQYFM3pOm-NcWmWnWDAEk3vlM(callback, typeface));
            }
        } catch (NameNotFoundException e) {
            callerThreadHandler.post(new -$$Lambda$FontsContract$bLFahJqnd9gkPbDqB-OCiChzm_E(callback));
        }
    }

    public static FontFamilyResult fetchFonts(Context context, CancellationSignal cancellationSignal, FontRequest request) throws NameNotFoundException {
        if (context.isRestricted()) {
            return new FontFamilyResult(3, null);
        }
        ProviderInfo providerInfo = getProvider(context.getPackageManager(), request);
        if (providerInfo == null) {
            return new FontFamilyResult(1, null);
        }
        try {
            return new FontFamilyResult(0, getFontFromProvider(context, request, providerInfo.authority, cancellationSignal));
        } catch (IllegalArgumentException e) {
            return new FontFamilyResult(2, null);
        }
    }

    public static Typeface buildTypeface(Context context, CancellationSignal cancellationSignal, FontInfo[] fonts) {
        if (context.isRestricted()) {
            return null;
        }
        Map<Uri, ByteBuffer> uriBuffer = prepareFontData(context, fonts, cancellationSignal);
        if (uriBuffer.isEmpty()) {
            return null;
        }
        return new Builder(fonts, uriBuffer).build();
    }

    private static Map<Uri, ByteBuffer> prepareFontData(Context context, FontInfo[] fonts, CancellationSignal cancellationSignal) {
        FileInputStream fis;
        Throwable th;
        Throwable th2;
        Throwable th3;
        CancellationSignal cancellationSignal2;
        FontInfo[] fontInfoArr = fonts;
        HashMap<Uri, ByteBuffer> out = new HashMap();
        ContentResolver resolver = context.getContentResolver();
        for (FontInfo font : fontInfoArr) {
            if (font.getResultCode() == 0) {
                Uri uri = font.getUri();
                if (!out.containsKey(uri)) {
                    ByteBuffer buffer = null;
                    try {
                        ParcelFileDescriptor pfd;
                        try {
                            pfd = resolver.openFileDescriptor(uri, "r", cancellationSignal);
                            if (pfd != null) {
                                fis = new FileInputStream(pfd.getFileDescriptor());
                                try {
                                    FileChannel fileChannel = fis.getChannel();
                                    buffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
                                    $closeResource(null, fis);
                                } catch (Throwable th4) {
                                    th2 = th4;
                                }
                            }
                        } catch (IOException e) {
                        } catch (IOException e2) {
                            Log.w(TAG, "find IOException.");
                            out.put(uri, buffer);
                        } catch (Throwable th5) {
                            if (pfd != null) {
                                $closeResource(th3, pfd);
                            }
                        }
                        if (pfd != null) {
                            $closeResource(null, pfd);
                        }
                    } catch (IOException e3) {
                        cancellationSignal2 = cancellationSignal;
                        Log.w(TAG, "find IOException.");
                        out.put(uri, buffer);
                    }
                    out.put(uri, buffer);
                }
            }
            cancellationSignal2 = cancellationSignal;
        }
        cancellationSignal2 = cancellationSignal;
        return Collections.unmodifiableMap(out);
        $closeResource(th, fis);
        throw th2;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    @VisibleForTesting
    public static ProviderInfo getProvider(PackageManager packageManager, FontRequest request) throws NameNotFoundException {
        String providerAuthority = request.getProviderAuthority();
        int i = 0;
        ProviderInfo info = packageManager.resolveContentProvider(providerAuthority, 0);
        StringBuilder stringBuilder;
        if (info == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No package found for authority: ");
            stringBuilder.append(providerAuthority);
            throw new NameNotFoundException(stringBuilder.toString());
        } else if (!info.packageName.equals(request.getProviderPackage())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Found content provider ");
            stringBuilder.append(providerAuthority);
            stringBuilder.append(", but package was not ");
            stringBuilder.append(request.getProviderPackage());
            throw new NameNotFoundException(stringBuilder.toString());
        } else if (info.applicationInfo.isSystemApp()) {
            return info;
        } else {
            List<byte[]> signatures = convertToByteArrayList(packageManager.getPackageInfo(info.packageName, 64).signatures);
            Collections.sort(signatures, sByteArrayComparator);
            List<List<byte[]>> requestCertificatesList = request.getCertificates();
            while (i < requestCertificatesList.size()) {
                List<byte[]> requestSignatures = new ArrayList((Collection) requestCertificatesList.get(i));
                Collections.sort(requestSignatures, sByteArrayComparator);
                if (equalsByteArrayList(signatures, requestSignatures)) {
                    return info;
                }
                i++;
            }
            return null;
        }
    }

    static /* synthetic */ int lambda$static$13(byte[] l, byte[] r) {
        if (l.length != r.length) {
            return l.length - r.length;
        }
        for (int i = 0; i < l.length; i++) {
            if (l[i] != r[i]) {
                return l[i] - r[i];
            }
        }
        return 0;
    }

    private static boolean equalsByteArrayList(List<byte[]> signatures, List<byte[]> requestSignatures) {
        if (signatures.size() != requestSignatures.size()) {
            return false;
        }
        for (int i = 0; i < signatures.size(); i++) {
            if (!Arrays.equals((byte[]) signatures.get(i), (byte[]) requestSignatures.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<byte[]> convertToByteArrayList(Signature[] signatures) {
        List<byte[]> shas = new ArrayList();
        for (Signature toByteArray : signatures) {
            shas.add(toByteArray.toByteArray());
        }
        return shas;
    }

    @VisibleForTesting
    public static FontInfo[] getFontFromProvider(Context context, FontRequest request, String authority, CancellationSignal cancellationSignal) {
        Throwable th;
        Throwable th2;
        String str = authority;
        ArrayList<FontInfo> result = new ArrayList();
        Uri uri = new Uri.Builder().scheme("content").authority(str).build();
        Uri fileBaseUri = new Uri.Builder().scheme("content").authority(str).appendPath("file").build();
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_id", Columns.FILE_ID, Columns.TTC_INDEX, Columns.VARIATION_SETTINGS, Columns.WEIGHT, Columns.ITALIC, Columns.RESULT_CODE}, "query = ?", new String[]{request.getQuery()}, null, cancellationSignal);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    int resultCodeColumnIndex = cursor.getColumnIndex(Columns.RESULT_CODE);
                    result = new ArrayList();
                    int idColumnIndex = cursor.getColumnIndexOrThrow("_id");
                    int fileIdColumnIndex = cursor.getColumnIndex(Columns.FILE_ID);
                    int ttcIndexColumnIndex = cursor.getColumnIndex(Columns.TTC_INDEX);
                    int vsColumnIndex = cursor.getColumnIndex(Columns.VARIATION_SETTINGS);
                    int weightColumnIndex = cursor.getColumnIndex(Columns.WEIGHT);
                    int italicColumnIndex = cursor.getColumnIndex(Columns.ITALIC);
                    while (cursor.moveToNext()) {
                        int resultCodeColumnIndex2;
                        Uri fileUri;
                        int weight;
                        boolean italic;
                        int resultCode = resultCodeColumnIndex != -1 ? cursor.getInt(resultCodeColumnIndex) : 0;
                        int ttcIndex = ttcIndexColumnIndex != -1 ? cursor.getInt(ttcIndexColumnIndex) : 0;
                        String variationSettings = vsColumnIndex != -1 ? cursor.getString(vsColumnIndex) : null;
                        if (fileIdColumnIndex == -1) {
                            resultCodeColumnIndex2 = resultCodeColumnIndex;
                            fileUri = ContentUris.withAppendedId(uri, cursor.getLong(idColumnIndex));
                        } else {
                            resultCodeColumnIndex2 = resultCodeColumnIndex;
                            fileUri = ContentUris.withAppendedId(fileBaseUri, cursor.getLong(fileIdColumnIndex));
                        }
                        if (weightColumnIndex == -1 || italicColumnIndex == -1) {
                            weight = 400;
                            italic = false;
                        } else {
                            weight = cursor.getInt(weightColumnIndex);
                            italic = cursor.getInt(italicColumnIndex) == 1;
                        }
                        result.add(new FontInfo(fileUri, ttcIndex, FontVariationAxis.fromFontVariationSettings(variationSettings), weight, italic, resultCode));
                        resultCodeColumnIndex = resultCodeColumnIndex2;
                    }
                }
            } catch (Throwable th3) {
                th2 = th3;
            }
        }
        if (cursor != null) {
            $closeResource(null, cursor);
        }
        return (FontInfo[]) result.toArray(new FontInfo[0]);
        if (cursor != null) {
            $closeResource(th, cursor);
        }
        throw th2;
    }
}
