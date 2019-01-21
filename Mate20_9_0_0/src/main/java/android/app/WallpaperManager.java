package android.app;

import android.annotation.SystemApi;
import android.app.IWallpaperManagerCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hwtheme.HwThemeManager;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.DeadSystemException;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.google.android.collect.Lists;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class WallpaperManager {
    public static final String ACTION_CHANGE_LIVE_WALLPAPER = "android.service.wallpaper.CHANGE_LIVE_WALLPAPER";
    public static final String ACTION_CROP_AND_SET_WALLPAPER = "android.service.wallpaper.CROP_AND_SET_WALLPAPER";
    public static final String ACTION_LIVE_WALLPAPER_CHOOSER = "android.service.wallpaper.LIVE_WALLPAPER_CHOOSER";
    public static final String COMMAND_DROP = "android.home.drop";
    public static final String COMMAND_SECONDARY_TAP = "android.wallpaper.secondaryTap";
    public static final String COMMAND_TAP = "android.wallpaper.tap";
    private static boolean DEBUG = false;
    public static final String EXTRA_LIVE_WALLPAPER_COMPONENT = "android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT";
    public static final String EXTRA_NEW_WALLPAPER_ID = "android.service.wallpaper.extra.ID";
    public static final int FLAG_LOCK = 2;
    public static final int FLAG_SYSTEM = 1;
    private static final String PROP_LOCK_WALLPAPER = "ro.config.lock_wallpaper";
    private static final String PROP_WALLPAPER = "ro.config.wallpaper";
    private static final String PROP_WALLPAPER_COMPONENT = "ro.config.wallpaper_component";
    private static String TAG = "WallpaperManager";
    public static final String WALLPAPER_PREVIEW_META_DATA = "android.wallpaper.preview";
    protected static final ArrayList<WeakReference<Object>> mCallbacks = Lists.newArrayList();
    private static Globals sGlobals;
    private static final Object sSync = new Object[0];
    private final Context mContext;
    private float mWallpaperXStep = -1.0f;
    private float mWallpaperYStep = -1.0f;

    public interface OnColorsChangedListener {
        void onColorsChanged(WallpaperColors wallpaperColors, int i);

        void onColorsChanged(WallpaperColors colors, int which, int userId) {
            onColorsChanged(colors, which);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SetWallpaperFlags {
    }

    static class FastBitmapDrawable extends Drawable {
        private final Bitmap mBitmap;
        private int mDrawLeft;
        private int mDrawTop;
        private final int mHeight;
        private final Paint mPaint;
        private final int mWidth;

        private FastBitmapDrawable(Bitmap bitmap) {
            this.mBitmap = bitmap;
            this.mWidth = bitmap.getWidth();
            this.mHeight = bitmap.getHeight();
            setBounds(0, 0, this.mWidth, this.mHeight);
            this.mPaint = new Paint();
            this.mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC));
        }

        public void draw(Canvas canvas) {
            canvas.drawBitmap(this.mBitmap, (float) this.mDrawLeft, (float) this.mDrawTop, this.mPaint);
        }

        public int getOpacity() {
            return -1;
        }

        public void setBounds(int left, int top, int right, int bottom) {
            this.mDrawLeft = (((right - left) - this.mWidth) / 2) + left;
            this.mDrawTop = (((bottom - top) - this.mHeight) / 2) + top;
        }

        public void setAlpha(int alpha) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        public void setColorFilter(ColorFilter colorFilter) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        public void setDither(boolean dither) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        public void setFilterBitmap(boolean filter) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        public int getIntrinsicWidth() {
            return this.mWidth;
        }

        public int getIntrinsicHeight() {
            return this.mHeight;
        }

        public int getMinimumWidth() {
            return this.mWidth;
        }

        public int getMinimumHeight() {
            return this.mHeight;
        }
    }

    private static class Globals extends Stub {
        private Bitmap mBlurWallpaper;
        private Bitmap mCachedWallpaper;
        private int mCachedWallpaperUserId;
        private boolean mColorCallbackRegistered;
        private final ArrayList<Pair<OnColorsChangedListener, Handler>> mColorListeners = new ArrayList();
        private Bitmap mDefaultWallpaper;
        private Handler mMainLooperHandler;
        private final IWallpaperManager mService;
        private int wallpaperHeight = 0;
        private int wallpaperWidth = 0;

        Globals(IWallpaperManager service, Looper looper) {
            this.mService = service;
            this.mMainLooperHandler = new Handler(looper);
            forgetLoadedWallpaper();
        }

        public void onWallpaperChanged() {
            forgetLoadedWallpaper();
        }

        public void addOnColorsChangedListener(OnColorsChangedListener callback, Handler handler, int userId) {
            synchronized (this) {
                if (!this.mColorCallbackRegistered) {
                    try {
                        this.mService.registerWallpaperColorsCallback(this, userId);
                        this.mColorCallbackRegistered = true;
                    } catch (RemoteException e) {
                        Log.w(WallpaperManager.TAG, "Can't register for color updates", e);
                    }
                }
                this.mColorListeners.add(new Pair(callback, handler));
            }
        }

        public void removeOnColorsChangedListener(OnColorsChangedListener callback, int userId) {
            synchronized (this) {
                this.mColorListeners.removeIf(new -$$Lambda$WallpaperManager$Globals$2yG7V1sbMECCnlFTLyjKWKqNoYI(callback));
                if (this.mColorListeners.size() == 0 && this.mColorCallbackRegistered) {
                    this.mColorCallbackRegistered = false;
                    try {
                        this.mService.unregisterWallpaperColorsCallback(this, userId);
                    } catch (RemoteException e) {
                        Log.w(WallpaperManager.TAG, "Can't unregister color updates", e);
                    }
                }
            }
        }

        static /* synthetic */ boolean lambda$removeOnColorsChangedListener$0(OnColorsChangedListener callback, Pair pair) {
            return pair.first == callback;
        }

        public void onWallpaperColorsChanged(WallpaperColors colors, int which, int userId) {
            synchronized (this) {
                Iterator it = this.mColorListeners.iterator();
                while (it.hasNext()) {
                    Pair<OnColorsChangedListener, Handler> listener = (Pair) it.next();
                    Handler handler = listener.second;
                    if (listener.second == null) {
                        handler = this.mMainLooperHandler;
                    }
                    handler.post(new -$$Lambda$WallpaperManager$Globals$1AcnQUORvPlCjJoNqdxfQT4o4Nw(this, listener, colors, which, userId));
                }
            }
        }

        public static /* synthetic */ void lambda$onWallpaperColorsChanged$1(Globals globals, Pair listener, WallpaperColors colors, int which, int userId) {
            boolean stillExists;
            synchronized (WallpaperManager.sGlobals) {
                stillExists = globals.mColorListeners.contains(listener);
            }
            if (stillExists) {
                ((OnColorsChangedListener) listener.first).onColorsChanged(colors, which, userId);
            }
        }

        WallpaperColors getWallpaperColors(int which, int userId) {
            if (which == 2 || which == 1) {
                try {
                    return this.mService.getWallpaperColors(which, userId);
                } catch (RemoteException e) {
                    return null;
                }
            }
            throw new IllegalArgumentException("Must request colors for exactly one kind of wallpaper");
        }

        public Bitmap peekWallpaperBitmap(Context context, boolean returnDefault, int which) {
            return peekWallpaperBitmap(context, returnDefault, which, context.getUserId(), false);
        }

        /* JADX WARNING: Missing block: B:38:0x0089, code skipped:
            if (r8 == false) goto L_0x00b0;
     */
        /* JADX WARNING: Missing block: B:39:0x008b, code skipped:
            r1 = r6.mDefaultWallpaper;
     */
        /* JADX WARNING: Missing block: B:40:0x008d, code skipped:
            if (r1 != null) goto L_0x00af;
     */
        /* JADX WARNING: Missing block: B:41:0x008f, code skipped:
            r2 = getDefaultWallpaper(r7, r10);
     */
        /* JADX WARNING: Missing block: B:42:0x0093, code skipped:
            if (r2 == null) goto L_0x00a2;
     */
        /* JADX WARNING: Missing block: B:43:0x0095, code skipped:
            r6.wallpaperWidth = r2.getWidth();
            r6.wallpaperHeight = r2.getHeight();
     */
        /* JADX WARNING: Missing block: B:44:0x00a2, code skipped:
            r6.wallpaperWidth = 0;
            r6.wallpaperHeight = 0;
     */
        /* JADX WARNING: Missing block: B:45:0x00a6, code skipped:
            monitor-enter(r6);
     */
        /* JADX WARNING: Missing block: B:47:?, code skipped:
            r6.mDefaultWallpaper = r2;
     */
        /* JADX WARNING: Missing block: B:48:0x00a9, code skipped:
            monitor-exit(r6);
     */
        /* JADX WARNING: Missing block: B:49:0x00aa, code skipped:
            r1 = r2;
     */
        /* JADX WARNING: Missing block: B:53:0x00af, code skipped:
            return r1;
     */
        /* JADX WARNING: Missing block: B:54:0x00b0, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Bitmap peekWallpaperBitmap(Context context, boolean returnDefault, int which, int userId, boolean hardware) {
            if (this.mService != null) {
                try {
                    if (!this.mService.isWallpaperSupported(context.getOpPackageName())) {
                        return null;
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            synchronized (this) {
                Bitmap bitmap;
                if (this.mCachedWallpaper == null || this.mCachedWallpaperUserId != userId || this.mCachedWallpaper.isRecycled()) {
                    this.mCachedWallpaper = null;
                    this.mCachedWallpaperUserId = 0;
                    try {
                        this.mCachedWallpaper = getCurrentWallpaperLocked(context, userId, hardware);
                        this.mCachedWallpaperUserId = userId;
                    } catch (OutOfMemoryError e2) {
                        String access$000 = WallpaperManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Out of memory loading the current wallpaper: ");
                        stringBuilder.append(e2);
                        Log.w(access$000, stringBuilder.toString());
                    } catch (SecurityException e3) {
                        if (context.getApplicationInfo().targetSdkVersion < 27) {
                            Log.w(WallpaperManager.TAG, "No permission to access wallpaper, suppressing exception to avoid crashing legacy app.");
                        } else {
                            throw e3;
                        }
                    }
                    if (this.mCachedWallpaper != null) {
                        this.wallpaperWidth = this.mCachedWallpaper.getWidth();
                        this.wallpaperHeight = this.mCachedWallpaper.getHeight();
                        bitmap = this.mCachedWallpaper;
                        return bitmap;
                    }
                    this.wallpaperWidth = 0;
                    this.wallpaperHeight = 0;
                } else {
                    bitmap = this.mCachedWallpaper;
                    return bitmap;
                }
            }
        }

        void forgetLoadedWallpaper() {
            synchronized (this) {
                this.mCachedWallpaper = null;
                this.mCachedWallpaperUserId = 0;
                this.mDefaultWallpaper = null;
            }
        }

        private Bitmap getCurrentWallpaperLocked(Context context, int userId, boolean hardware) {
            if (this.mService == null) {
                Log.w(WallpaperManager.TAG, "WallpaperService not running");
                return null;
            }
            ParcelFileDescriptor fd;
            try {
                Bundle params = new Bundle();
                fd = this.mService.getWallpaper(context.getOpPackageName(), this, 1, params, userId);
                if (fd != null) {
                    int width = params.getInt(MediaFormat.KEY_WIDTH, 0);
                    int height = params.getInt(MediaFormat.KEY_HEIGHT, 0);
                    try {
                        Options options = new Options();
                        if (hardware) {
                            options.inPreferredConfig = Config.HARDWARE;
                        }
                        Bitmap generateBitmap = HwThemeManager.generateBitmap(context, this.mService.scaleWallpaperBitmapToScreenSize(BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options)), width, height);
                        IoUtils.closeQuietly(fd);
                        return generateBitmap;
                    } catch (OutOfMemoryError e) {
                        Log.w(WallpaperManager.TAG, "Can't decode file", e);
                        IoUtils.closeQuietly(fd);
                    }
                }
                return null;
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            } catch (Throwable th) {
                IoUtils.closeQuietly(fd);
            }
        }

        private Bitmap getDefaultWallpaper(Context context, int which) {
            InputStream is = HwThemeManager.getDefaultWallpaperIS(context, which);
            if (is == null) {
                long time1 = System.currentTimeMillis();
                is = WallpaperManager.openDefaultWallpaper(context, 1);
                String access$000 = WallpaperManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getDefaultWallpaper in openDefaultWallpaper cost time: ");
                stringBuilder.append(System.currentTimeMillis() - time1);
                stringBuilder.append("ms");
                Log.d(access$000, stringBuilder.toString());
                Log.d(WallpaperManager.TAG, "WallpaperManager getDefaultWallpaper HwThemeManager.getDefaultWallpaperIS is null");
            } else {
                Context context2 = context;
            }
            InputStream is2 = is;
            if (is2 != null) {
                try {
                    long time3;
                    Options options = new Options();
                    long time2 = System.currentTimeMillis();
                    Bitmap bm = BitmapFactory.decodeStream(is2, null, options);
                    String access$0002 = WallpaperManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getDefaultWallpaper in decodeStream cost time: ");
                    stringBuilder2.append(System.currentTimeMillis() - time2);
                    stringBuilder2.append("ms");
                    Log.d(access$0002, stringBuilder2.toString());
                    try {
                        if (this.mService != null) {
                            time3 = System.currentTimeMillis();
                            bm = this.mService.scaleWallpaperBitmapToScreenSize(bm);
                            access$0002 = WallpaperManager.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("getDefaultWallpaper in scaleWallpaperBitmapToScreenSize cost time: ");
                            stringBuilder3.append(System.currentTimeMillis() - time3);
                            stringBuilder3.append("ms");
                            Log.d(access$0002, stringBuilder3.toString());
                        }
                    } catch (RemoteException e) {
                        Log.w(WallpaperManager.TAG, "scaleWallpaperBitmapToScreenSize fail");
                    }
                    String[] location = SystemProperties.get("ro.config.wallpaper_offset").split(",");
                    if (location.length == 4 && bm != null) {
                        int xStart = Integer.parseInt(location[0]);
                        int yStart = Integer.parseInt(location[1]);
                        int xWidth = Integer.parseInt(location[2]);
                        int yHeight = Integer.parseInt(location[3]);
                        if (xStart >= 0 && yStart >= 0 && xStart + xWidth <= bm.getWidth() && yStart + yHeight <= bm.getHeight()) {
                            Bitmap bm2 = Bitmap.createBitmap(bm, xStart, yStart, xWidth, yHeight);
                            IoUtils.closeQuietly(is2);
                            return bm2;
                        }
                    }
                    if (bm != null) {
                        time3 = System.currentTimeMillis();
                        Bitmap bitmap = WallpaperManager.getInstance(context).createDefaultWallpaperBitmap(bm);
                        String access$0003 = WallpaperManager.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("getDefaultWallpaper createDefaultWallpaperBitmap cost time: ");
                        stringBuilder4.append(System.currentTimeMillis() - time3);
                        stringBuilder4.append("ms");
                        Log.d(access$0003, stringBuilder4.toString());
                        IoUtils.closeQuietly(is2);
                        return bitmap;
                    }
                } catch (OutOfMemoryError e2) {
                    Log.w(WallpaperManager.TAG, "Can't decode stream");
                } catch (Throwable th) {
                    IoUtils.closeQuietly(is2);
                }
                IoUtils.closeQuietly(is2);
            }
            return null;
        }

        public void onBlurWallpaperChanged() {
            synchronized (this) {
                this.mBlurWallpaper = null;
            }
            if (WallpaperManager.mCallbacks != null) {
                int count = WallpaperManager.mCallbacks.size();
                for (int i = 0; i < count; i++) {
                    Object obj = ((WeakReference) WallpaperManager.mCallbacks.get(i)).get();
                    if (obj != null) {
                        try {
                            final Method callback = obj.getClass().getDeclaredMethod("onBlurWallpaperChanged", new Class[0]);
                            AccessController.doPrivileged(new PrivilegedAction() {
                                public Object run() {
                                    callback.setAccessible(true);
                                    return null;
                                }
                            });
                            callback.invoke(obj, new Object[0]);
                        } catch (RuntimeException e) {
                        } catch (Exception ex) {
                            throw new IllegalStateException("Unable to get onBlurWallpaperChanged method", ex);
                        }
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:67:0x00e4, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Bitmap peekBlurWallpaperBitmap(Rect rect) {
            synchronized (this) {
                if (rect.width() > 0) {
                    if (rect.height() > 0) {
                        Bitmap bitmap;
                        if (this.mBlurWallpaper == null || rect.bottom > this.mBlurWallpaper.getHeight()) {
                            this.mBlurWallpaper = null;
                            if (WallpaperManager.DEBUG) {
                                Log.d(WallpaperManager.TAG, "WallpaperManager peekBlurWallpaperBitmap begin");
                            }
                            long timeBegin = System.currentTimeMillis();
                            ParcelFileDescriptor fd;
                            try {
                                fd = this.mService.getBlurWallpaper(this);
                                if (fd != null) {
                                    try {
                                        Options options = new Options();
                                        if (fd.getFileDescriptor() == null) {
                                            Log.e(WallpaperManager.TAG, "peekBlurWallpaperBitmap() has invalid fd!");
                                            try {
                                                fd.close();
                                            } catch (IOException e) {
                                            }
                                            return null;
                                        }
                                        Bitmap bitmap2 = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
                                        if (bitmap2.getWidth() > 0 && bitmap2.getHeight() > 0 && rect.bottom > 0) {
                                            this.mBlurWallpaper = Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), Math.min(bitmap2.getHeight(), rect.bottom));
                                        }
                                        try {
                                            fd.close();
                                        } catch (IOException e2) {
                                        }
                                    } catch (OutOfMemoryError e3) {
                                        this.mBlurWallpaper = null;
                                        Log.w(WallpaperManager.TAG, "Can't decode file", e3);
                                        fd.close();
                                    } catch (Exception ex) {
                                        this.mBlurWallpaper = null;
                                        Log.w(WallpaperManager.TAG, "Can't decode file", ex);
                                        fd.close();
                                    }
                                }
                            } catch (RemoteException e4) {
                            } catch (Throwable th) {
                                try {
                                    fd.close();
                                } catch (IOException e5) {
                                }
                            }
                            if (WallpaperManager.DEBUG) {
                                long takenTime = System.currentTimeMillis() - timeBegin;
                                String access$000 = WallpaperManager.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("WallpaperManager peekBlurWallpaperBitmap takenTime = ");
                                stringBuilder.append(takenTime);
                                Log.d(access$000, stringBuilder.toString());
                            }
                            bitmap = this.mBlurWallpaper;
                            return bitmap;
                        }
                        bitmap = this.mBlurWallpaper;
                        return bitmap;
                    }
                }
            }
        }

        public void forgetLoadedBlurWallpaper() {
            synchronized (this) {
                if (!(this.mBlurWallpaper == null || this.mBlurWallpaper.isRecycled())) {
                    this.mBlurWallpaper.recycle();
                    this.mBlurWallpaper = null;
                }
            }
        }

        public Bundle peekCurrentWallpaperBounds(Context context) {
            synchronized (this) {
                if (this.mService == null) {
                    Log.w(WallpaperManager.TAG, "WallpaperService not running");
                    return null;
                }
                ParcelFileDescriptor fd = null;
                try {
                    Bundle params = new Bundle();
                    fd = this.mService.getWallpaper(context.getOpPackageName(), this, 1, params, this.mService.getWallpaperUserId());
                    params.remove(MediaFormat.KEY_WIDTH);
                    params.remove(MediaFormat.KEY_HEIGHT);
                    if (fd == null) {
                        Log.w(WallpaperManager.TAG, "cannot get ParcelFileDescriptor of current user wallpaper in read only mode, return null!");
                        if (fd != null) {
                            try {
                                fd.close();
                            } catch (IOException e) {
                            }
                        }
                        return null;
                    }
                    Options options = new Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
                    params.putInt("wallpaper_file_width", options.outWidth);
                    params.putInt("wallpaper_file_height", options.outHeight);
                    if (WallpaperManager.DEBUG) {
                        String access$000 = WallpaperManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getUserWallpaperBounds return wallpaper_file_width=");
                        stringBuilder.append(params.getInt("wallpaper_file_width"));
                        stringBuilder.append(", wallpaper_file_height=");
                        stringBuilder.append(params.getInt("wallpaper_file_height"));
                        Log.d(access$000, stringBuilder.toString());
                    }
                    if (fd != null) {
                        try {
                            fd.close();
                        } catch (IOException e2) {
                        }
                    }
                    return params;
                } catch (RemoteException e3) {
                    if (fd != null) {
                        fd.close();
                    }
                    return null;
                } catch (OutOfMemoryError oome) {
                    Log.e(WallpaperManager.TAG, "No memory load current wallpaper bounds", oome);
                    if (fd != null) {
                        fd.close();
                    }
                    return null;
                } catch (Exception e4) {
                    try {
                        Log.e(WallpaperManager.TAG, "Error while getting current wallpaper bounds", e4);
                        if (fd != null) {
                            try {
                                fd.close();
                            } catch (IOException e5) {
                            }
                        }
                        return null;
                    } catch (Throwable th) {
                        if (fd != null) {
                            try {
                                fd.close();
                            } catch (IOException e6) {
                            }
                        }
                    }
                }
            }
        }

        public synchronized int getWallpaperWidth() {
            return this.wallpaperWidth;
        }

        public synchronized int getWallpaperHeight() {
            return this.wallpaperHeight;
        }
    }

    private class WallpaperSetCompletion extends Stub {
        final CountDownLatch mLatch = new CountDownLatch(1);

        public void waitForCompletion() {
            try {
                this.mLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }

        public void onWallpaperChanged() throws RemoteException {
            this.mLatch.countDown();
        }

        public void onWallpaperColorsChanged(WallpaperColors colors, int which, int userId) throws RemoteException {
            WallpaperManager.sGlobals.onWallpaperColorsChanged(colors, which, userId);
        }

        public void onBlurWallpaperChanged() throws RemoteException {
        }
    }

    static void initGlobals(IWallpaperManager service, Looper looper) {
        synchronized (sSync) {
            if (sGlobals == null) {
                sGlobals = new Globals(service, looper);
            }
        }
    }

    WallpaperManager(IWallpaperManager service, Context context, Handler handler) {
        this.mContext = context;
        initGlobals(service, context.getMainLooper());
    }

    public static WallpaperManager getInstance(Context context) {
        return (WallpaperManager) context.getSystemService("wallpaper");
    }

    public IWallpaperManager getIWallpaperManager() {
        return sGlobals.mService;
    }

    public Drawable getDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, true, 1);
        if (bm == null) {
            return null;
        }
        Drawable dr = new BitmapDrawable(this.mContext.getResources(), bm);
        dr.setDither(false);
        return dr;
    }

    public Drawable getBuiltInDrawable() {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f, 1);
    }

    public Drawable getBuiltInDrawable(int which) {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f, which);
    }

    public Drawable getBuiltInDrawable(int outWidth, int outHeight, boolean scaleToFit, float horizontalAlignment, float verticalAlignment) {
        return getBuiltInDrawable(outWidth, outHeight, scaleToFit, horizontalAlignment, verticalAlignment, 1);
    }

    public Drawable getBuiltInDrawable(int outWidth, int outHeight, boolean scaleToFit, float horizontalAlignment, float verticalAlignment, int which) {
        int i = outWidth;
        int outHeight2 = outHeight;
        int i2 = which;
        if (sGlobals.mService == null) {
            float f = verticalAlignment;
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        } else if (i2 == 1 || i2 == 2) {
            Resources resources = this.mContext.getResources();
            float horizontalAlignment2 = Math.max(0.0f, Math.min(1.0f, horizontalAlignment));
            float verticalAlignment2 = Math.max(0.0f, Math.min(1.0f, verticalAlignment));
            InputStream wpStream = openDefaultWallpaper(this.mContext, i2);
            if (wpStream == null) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("default wallpaper stream ");
                    stringBuilder.append(i2);
                    stringBuilder.append(" is null");
                    Log.w(str, stringBuilder.toString());
                }
                return null;
            }
            Bitmap fullSize;
            InputStream is = new BufferedInputStream(wpStream);
            float f2;
            if (i <= 0) {
                fullSize = null;
            } else if (outHeight2 <= 0) {
                f2 = verticalAlignment2;
                fullSize = null;
            } else {
                Options options = new Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, options);
                if (options.outWidth == 0 || options.outHeight == 0) {
                    Drawable drawable = null;
                    Log.e(TAG, "default wallpaper dimensions are 0");
                    return drawable;
                }
                InputStream is2;
                Drawable drawable2;
                RectF cropRectF;
                int inWidth = options.outWidth;
                int inHeight = options.outHeight;
                InputStream is3 = new BufferedInputStream(openDefaultWallpaper(this.mContext, i2));
                int outWidth2 = Math.min(inWidth, i);
                outHeight2 = Math.min(inHeight, outHeight2);
                if (scaleToFit) {
                    int outWidth3 = outWidth2;
                    is2 = is3;
                    drawable2 = null;
                    cropRectF = getMaxCropRect(inWidth, inHeight, outWidth3, outHeight2, horizontalAlignment2, verticalAlignment2);
                    is = outWidth3;
                } else {
                    is2 = is3;
                    drawable2 = null;
                    is = outWidth2;
                    float left = ((float) (inWidth - is)) * horizontalAlignment2;
                    float top = ((float) (inHeight - outHeight2)) * verticalAlignment2;
                    cropRectF = new RectF(left, top, ((float) is) + left, ((float) outHeight2) + top);
                }
                RectF options2 = cropRectF;
                Rect roundedTrueCrop = new Rect();
                options2.roundOut(roundedTrueCrop);
                int i3;
                if (roundedTrueCrop.width() <= 0) {
                    f2 = verticalAlignment2;
                } else if (roundedTrueCrop.height() <= 0) {
                    i3 = outHeight2;
                    f2 = verticalAlignment2;
                } else {
                    inHeight = Math.min(roundedTrueCrop.width() / is, roundedTrueCrop.height() / outHeight2);
                    BitmapRegionDecoder decoder = drawable2;
                    try {
                        decoder = BitmapRegionDecoder.newInstance(is2, true);
                    } catch (IOException e) {
                        IOException iOException = e;
                        Log.w(TAG, "cannot open region decoder for default wallpaper");
                    }
                    Bitmap crop = null;
                    if (decoder != null) {
                        Options options3 = new Options();
                        if (inHeight > 1) {
                            options3.inSampleSize = inHeight;
                        }
                        crop = decoder.decodeRegion(roundedTrueCrop, options3);
                        decoder.recycle();
                    }
                    InputStream inputStream;
                    if (crop == null) {
                        InputStream is4 = new BufferedInputStream(openDefaultWallpaper(this.mContext, i2));
                        Options options4 = new Options();
                        if (inHeight > 1) {
                            options4.inSampleSize = inHeight;
                        }
                        Bitmap is5 = BitmapFactory.decodeStream(is4, drawable2, options4);
                        if (is5 != null) {
                            crop = Bitmap.createBitmap(is5, roundedTrueCrop.left, roundedTrueCrop.top, roundedTrueCrop.width(), roundedTrueCrop.height());
                        } else {
                            Bitmap bitmap = crop;
                            inputStream = is4;
                        }
                    } else {
                        inputStream = is2;
                    }
                    if (crop == null) {
                        Log.w(TAG, "cannot decode default wallpaper");
                        return null;
                    }
                    if (is <= null || outHeight2 <= 0) {
                        f2 = verticalAlignment2;
                    } else if (crop.getWidth() == is && crop.getHeight() == outHeight2) {
                        i3 = outHeight2;
                        f2 = verticalAlignment2;
                    } else {
                        Matrix m = new Matrix();
                        RectF cropRect = new RectF(0.0f, 0.0f, (float) crop.getWidth(), (float) crop.getHeight());
                        RectF returnRect = new RectF(0.0f, 0.0f, (float) is, (float) outHeight2);
                        m.setRectToRect(cropRect, returnRect, ScaleToFit.FILL);
                        Bitmap verticalAlignment3 = Bitmap.createBitmap((int) returnRect.width(), (int) returnRect.height(), Config.ARGB_8888);
                        if (verticalAlignment3 != null) {
                            is2 = new Canvas(verticalAlignment3);
                            Paint p = new Paint();
                            p.setFilterBitmap(1);
                            is2.drawBitmap(crop, m, p);
                            crop = verticalAlignment3;
                        }
                    }
                    return new BitmapDrawable(resources, crop);
                }
                Log.w(TAG, "crop has bad values for full size image");
                return null;
            }
            return new BitmapDrawable(resources, BitmapFactory.decodeStream(is, fullSize, fullSize));
        } else {
            throw new IllegalArgumentException("Must request exactly one kind of wallpaper");
        }
    }

    private static RectF getMaxCropRect(int inWidth, int inHeight, int outWidth, int outHeight, float horizontalAlignment, float verticalAlignment) {
        RectF cropRect = new RectF();
        float cropWidth;
        if (((float) inWidth) / ((float) inHeight) > ((float) outWidth) / ((float) outHeight)) {
            cropRect.top = 0.0f;
            cropRect.bottom = (float) inHeight;
            cropWidth = ((float) outWidth) * (((float) inHeight) / ((float) outHeight));
            cropRect.left = (((float) inWidth) - cropWidth) * horizontalAlignment;
            cropRect.right = cropRect.left + cropWidth;
        } else {
            cropRect.left = 0.0f;
            cropRect.right = (float) inWidth;
            cropWidth = ((float) outHeight) * (((float) inWidth) / ((float) outWidth));
            cropRect.top = (((float) inHeight) - cropWidth) * verticalAlignment;
            cropRect.bottom = cropRect.top + cropWidth;
        }
        return cropRect;
    }

    public Drawable peekDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, false, 1);
        if (bm == null) {
            return null;
        }
        Drawable dr = new BitmapDrawable(this.mContext.getResources(), bm);
        dr.setDither(false);
        return dr;
    }

    public Drawable getFastDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, true, 1);
        if (bm != null) {
            return new FastBitmapDrawable(bm);
        }
        return null;
    }

    public Drawable peekFastDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, false, 1);
        if (bm != null) {
            return new FastBitmapDrawable(bm);
        }
        return null;
    }

    public Bitmap getBitmap() {
        return getBitmap(false);
    }

    public Bitmap getBitmap(boolean hardware) {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isValidExtDisplayId(this.mContext)) {
            forgetLoadedWallpaper();
        }
        long time1 = System.currentTimeMillis();
        Bitmap bitmap = getBitmapAsUser(this.mContext.getUserId(), hardware);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getBitmapAsUser cost time: ");
        stringBuilder.append(System.currentTimeMillis() - time1);
        stringBuilder.append("ms");
        Log.d(str, stringBuilder.toString());
        return bitmap;
    }

    public Bitmap getBitmapAsUser(int userId, boolean hardware) {
        return sGlobals.peekWallpaperBitmap(this.mContext, true, 1, userId, hardware);
    }

    public ParcelFileDescriptor getWallpaperFile(int which) {
        return getWallpaperFile(which, this.mContext.getUserId());
    }

    public void addOnColorsChangedListener(OnColorsChangedListener listener, Handler handler) {
        addOnColorsChangedListener(listener, handler, this.mContext.getUserId());
    }

    public void addOnColorsChangedListener(OnColorsChangedListener listener, Handler handler, int userId) {
        sGlobals.addOnColorsChangedListener(listener, handler, userId);
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener callback) {
        removeOnColorsChangedListener(callback, this.mContext.getUserId());
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener callback, int userId) {
        sGlobals.removeOnColorsChangedListener(callback, userId);
    }

    public WallpaperColors getWallpaperColors(int which) {
        return getWallpaperColors(which, this.mContext.getUserId());
    }

    public WallpaperColors getWallpaperColors(int which, int userId) {
        return sGlobals.getWallpaperColors(which, userId);
    }

    public ParcelFileDescriptor getWallpaperFile(int which, int userId) {
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must request exactly one kind of wallpaper");
        } else if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getWallpaper(this.mContext.getOpPackageName(), null, which, new Bundle(), userId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (SecurityException e2) {
                if (this.mContext.getApplicationInfo().targetSdkVersion < 27) {
                    Log.w(TAG, "No permission to access wallpaper, suppressing exception to avoid crashing legacy app.");
                    return null;
                }
                throw e2;
            }
        } else {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
    }

    public void forgetLoadedWallpaper() {
        sGlobals.forgetLoadedWallpaper();
    }

    public WallpaperInfo getWallpaperInfo() {
        try {
            if (sGlobals.mService != null) {
                return sGlobals.mService.getWallpaperInfo(this.mContext.getUserId());
            }
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public WallpaperInfo getWallpaperInfo(int userId) {
        try {
            if (sGlobals.mService != null) {
                return sGlobals.mService.getWallpaperInfo(userId);
            }
            Log.w(TAG, "sGlobals.mService no running");
            throw new RuntimeException(new DeadSystemException());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getWallpaperId(int which) {
        return getWallpaperIdForUser(which, this.mContext.getUserId());
    }

    public int getWallpaperIdForUser(int which, int userId) {
        try {
            if (sGlobals.mService != null) {
                return sGlobals.mService.getWallpaperIdForUser(which, userId);
            }
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent getCropAndSetWallpaperIntent(Uri imageUri) {
        if (imageUri == null) {
            throw new IllegalArgumentException("Image URI must not be null");
        } else if ("content".equals(imageUri.getScheme())) {
            PackageManager packageManager = this.mContext.getPackageManager();
            Intent cropAndSetWallpaperIntent = new Intent(ACTION_CROP_AND_SET_WALLPAPER, imageUri);
            cropAndSetWallpaperIntent.addFlags(1);
            ResolveInfo resolvedHome = packageManager.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 65536);
            if (resolvedHome != null) {
                cropAndSetWallpaperIntent.setPackage(resolvedHome.activityInfo.packageName);
                if (packageManager.queryIntentActivities(cropAndSetWallpaperIntent, 0).size() > 0) {
                    return cropAndSetWallpaperIntent;
                }
            }
            cropAndSetWallpaperIntent.setPackage(this.mContext.getString(R.string.config_wallpaperCropperPackage));
            if (packageManager.queryIntentActivities(cropAndSetWallpaperIntent, 0).size() > 0) {
                return cropAndSetWallpaperIntent;
            }
            throw new IllegalArgumentException("Cannot use passed URI to set wallpaper; check that the type returned by ContentProvider matches image/*");
        } else {
            throw new IllegalArgumentException("Image URI must be of the content scheme type");
        }
    }

    public void setResource(int resid) throws IOException {
        setResource(resid, 3);
    }

    public int setResource(int resid, int which) throws IOException {
        if (sGlobals.mService != null) {
            Bundle result = new Bundle();
            WallpaperSetCompletion completion = new WallpaperSetCompletion();
            FileOutputStream fos;
            try {
                Resources resources = this.mContext.getResources();
                ParcelFileDescriptor fd = sGlobals.mService;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("res:");
                stringBuilder.append(resources.getResourceName(resid));
                fd = fd.setWallpaper(stringBuilder.toString(), this.mContext.getOpPackageName(), null, false, result, which, completion, this.mContext.getUserId());
                if (fd != null) {
                    fos = null;
                    boolean ok = false;
                    fos = new AutoCloseOutputStream(fd);
                    copyStreamToWallpaperFile(resources.openRawResource(resid), fos);
                    fos.close();
                    completion.waitForCompletion();
                    IoUtils.closeQuietly(fos);
                }
                return result.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Throwable th) {
                IoUtils.closeQuietly(fos);
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void setBitmap(Bitmap bitmap) throws IOException {
        setBitmap(bitmap, null, true);
    }

    public int setBitmap(Bitmap fullImage, Rect visibleCropHint, boolean allowBackup) throws IOException {
        return setBitmap(fullImage, visibleCropHint, allowBackup, 3);
    }

    public int setBitmap(Bitmap fullImage, Rect visibleCropHint, boolean allowBackup, int which) throws IOException {
        return setBitmap(fullImage, visibleCropHint, allowBackup, which, this.mContext.getUserId());
    }

    public int setBitmap(Bitmap fullImage, Rect visibleCropHint, boolean allowBackup, int which, int userId) throws IOException {
        RemoteException e;
        Throwable th;
        Bitmap bitmap;
        Rect rect = visibleCropHint;
        validateRect(rect);
        if (sGlobals.mService != null) {
            Bundle result = new Bundle();
            WallpaperSetCompletion completion = new WallpaperSetCompletion();
            try {
                ParcelFileDescriptor fd = sGlobals.mService.setWallpaper(null, this.mContext.getOpPackageName(), rect, allowBackup, result, which, completion, userId);
                if (fd != null) {
                    FileOutputStream fos = null;
                    try {
                        fos = new AutoCloseOutputStream(fd);
                        try {
                            fullImage.compress(CompressFormat.PNG, 90, fos);
                            fos.close();
                            completion.waitForCompletion();
                            try {
                                IoUtils.closeQuietly(fos);
                            } catch (RemoteException e2) {
                                e = e2;
                                throw e.rethrowFromSystemServer();
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            IoUtils.closeQuietly(fos);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        bitmap = fullImage;
                        IoUtils.closeQuietly(fos);
                        throw th;
                    }
                }
                bitmap = fullImage;
                return result.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
            } catch (RemoteException e3) {
                e = e3;
                bitmap = fullImage;
                throw e.rethrowFromSystemServer();
            }
        }
        bitmap = fullImage;
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    private final void validateRect(Rect rect) {
        if (rect != null && rect.isEmpty()) {
            throw new IllegalArgumentException("visibleCrop rectangle must be valid and non-empty");
        }
    }

    public void setStream(InputStream bitmapData) throws IOException {
        setStream(bitmapData, null, true);
    }

    private void copyStreamToWallpaperFile(InputStream data, FileOutputStream fos) throws IOException {
        FileUtils.copy(data, fos);
    }

    public int setStream(InputStream bitmapData, Rect visibleCropHint, boolean allowBackup) throws IOException {
        return setStream(bitmapData, visibleCropHint, allowBackup, 3);
    }

    public int setStream(InputStream bitmapData, Rect visibleCropHint, boolean allowBackup, int which) throws IOException {
        validateRect(visibleCropHint);
        if (sGlobals.mService != null) {
            Bundle result = new Bundle();
            WallpaperSetCompletion completion = new WallpaperSetCompletion();
            FileOutputStream fos;
            try {
                ParcelFileDescriptor fd = sGlobals.mService.setWallpaper(null, this.mContext.getOpPackageName(), visibleCropHint, allowBackup, result, which, completion, this.mContext.getUserId());
                if (fd != null) {
                    fos = null;
                    fos = new AutoCloseOutputStream(fd);
                    copyStreamToWallpaperFile(bitmapData, fos);
                    fos.close();
                    completion.waitForCompletion();
                    IoUtils.closeQuietly(fos);
                }
                return result.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Throwable th) {
                IoUtils.closeQuietly(fos);
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public boolean hasResourceWallpaper(int resid) {
        if (sGlobals.mService != null) {
            try {
                Resources resources = this.mContext.getResources();
                String name = new StringBuilder();
                name.append("res:");
                name.append(resources.getResourceName(resid));
                return sGlobals.mService.hasNamedWallpaper(name.toString());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public int getDesiredMinimumWidth() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getWidthHint();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public int getDesiredMinimumHeight() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getHeightHint();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void suggestDesiredDimensions(int minimumWidth, int minimumHeight) {
        RemoteException e = null;
        try {
            e = SystemProperties.getInt("sys.max_texture_size", 0);
        } catch (Exception e2) {
        }
        if (e > null && (minimumWidth > e || minimumHeight > e)) {
            float aspect = ((float) minimumHeight) / ((float) minimumWidth);
            if (minimumWidth > minimumHeight) {
                minimumWidth = e;
                minimumHeight = (int) (((double) (((float) minimumWidth) * aspect)) + 0.5d);
            } else {
                minimumHeight = e;
                minimumWidth = (int) (((double) (((float) minimumHeight) / aspect)) + 0.5d);
            }
        }
        try {
            if (sGlobals.mService != null) {
                sGlobals.mService.setDimensionHints(minimumWidth, minimumHeight, this.mContext.getOpPackageName());
            } else {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
        } catch (RemoteException e3) {
            throw e3.rethrowFromSystemServer();
        }
    }

    public void setDisplayPadding(Rect padding) {
        try {
            if (sGlobals.mService != null) {
                sGlobals.mService.setDisplayPadding(padding, this.mContext.getOpPackageName());
            } else {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setDisplayOffset(IBinder windowToken, int x, int y) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperDisplayOffset(windowToken, x, y);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearWallpaper() {
        clearWallpaper(2, this.mContext.getUserId());
        clearWallpaper(1, this.mContext.getUserId());
    }

    @SystemApi
    public void clearWallpaper(int which, int userId) {
        if (sGlobals.mService != null) {
            try {
                sGlobals.mService.clearWallpaper(this.mContext.getOpPackageName(), which, userId);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    @SystemApi
    public boolean setWallpaperComponent(ComponentName name) {
        return setWallpaperComponent(name, this.mContext.getUserId());
    }

    public boolean setWallpaperComponent(ComponentName name, int userId) {
        if (sGlobals.mService != null) {
            try {
                sGlobals.mService.setWallpaperComponentChecked(name, this.mContext.getOpPackageName(), userId);
                return true;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void setWallpaperOffsets(IBinder windowToken, float xOffset, float yOffset) {
        RemoteException e;
        float f;
        try {
            Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            int width = size.y > size.x ? size.x : size.y;
            int height = size.y > size.x ? size.y : size.x;
            int wallpaperWidth = getWallpaperWidth();
            int wallpaperHeight = getWallpaperHeight();
            if (wallpaperWidth == 0 && wallpaperHeight == 0) {
                Bitmap currentWallpaper = getBitmap();
                if (currentWallpaper != null) {
                    wallpaperWidth = currentWallpaper.getWidth();
                    wallpaperHeight = currentWallpaper.getHeight();
                    currentWallpaper.recycle();
                }
            }
            boolean z = false;
            boolean isSquare = wallpaperWidth == height && wallpaperHeight == height;
            boolean is_scroll = false;
            if (height >= 2 * width && isSquare) {
                if (System.getInt(this.mContext.getContentResolver(), "is_scroll", -1) == 1) {
                    z = true;
                }
                is_scroll = z;
            }
            if (!(width == wallpaperWidth && height == wallpaperHeight) && (!isSquare || is_scroll)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setWallpaperOffsets second wallpaper =[");
                f = xOffset;
                try {
                    stringBuilder.append(f);
                    stringBuilder.append(",");
                    stringBuilder.append(yOffset);
                    stringBuilder.append(",");
                    stringBuilder.append(this.mWallpaperXStep);
                    stringBuilder.append(",");
                    stringBuilder.append(this.mWallpaperYStep);
                    stringBuilder.append("]");
                    Log.i(str, stringBuilder.toString());
                    WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, f, yOffset, this.mWallpaperXStep, this.mWallpaperYStep);
                } catch (RemoteException e2) {
                    e = e2;
                }
            } else {
                Log.i(TAG, "setWallpaperOffsets  first wallpaper =[0]");
                WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, 0.0f, 0.0f, 0.0f, 0.0f);
                f = xOffset;
            }
        } catch (RemoteException e3) {
            e = e3;
            f = xOffset;
            throw e.rethrowFromSystemServer();
        }
    }

    public void setWallpaperOffsetSteps(float xStep, float yStep) {
        this.mWallpaperXStep = xStep;
        this.mWallpaperYStep = yStep;
    }

    public void sendWallpaperCommand(IBinder windowToken, String action, int x, int y, int z, Bundle extras) {
        try {
            WindowManagerGlobal.getWindowSession().sendWallpaperCommand(windowToken, action, x, y, z, extras, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isWallpaperSupported() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.isWallpaperSupported(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public boolean isSetWallpaperAllowed() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.isSetWallpaperAllowed(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void clearWallpaperOffsets(IBinder windowToken) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, -1.0f, -1.0f, -1.0f, -1.0f);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clear() throws IOException {
        setStream(openDefaultWallpaper(this.mContext, 1), null, false);
    }

    public void clear(int which) throws IOException {
        if ((which & 1) != 0) {
            clear();
        }
        if ((which & 2) != 0) {
            clearWallpaper(2, this.mContext.getUserId());
        }
    }

    public static InputStream openDefaultWallpaper(Context context, int which) {
        if (which == 2) {
            return null;
        }
        String path = SystemProperties.get(PROP_WALLPAPER);
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (IOException e) {
                }
            }
        }
        try {
            return context.getResources().openRawResource(R.drawable.default_wallpaper);
        } catch (NotFoundException e2) {
            return null;
        }
    }

    public static ComponentName getDefaultWallpaperComponent(Context context) {
        ComponentName cn;
        String flat = SystemProperties.get(PROP_WALLPAPER_COMPONENT);
        if (TextUtils.isEmpty(flat)) {
            flat = HwThemeManager.getDefaultLiveWallpaper(UserHandle.myUserId());
        }
        if (!TextUtils.isEmpty(flat)) {
            cn = ComponentName.unflattenFromString(flat);
            if (cn != null) {
                return cn;
            }
        }
        flat = context.getString(R.string.default_wallpaper_component);
        if (!TextUtils.isEmpty(flat)) {
            cn = ComponentName.unflattenFromString(flat);
            if (cn != null) {
                return cn;
            }
        }
        return null;
    }

    public static ComponentName getDefaultWallpaperComponent(int userId) {
        String flat = HwThemeManager.getDefaultLiveWallpaper(userId);
        if (!TextUtils.isEmpty(flat)) {
            ComponentName cn = ComponentName.unflattenFromString(flat);
            if (cn != null) {
                return cn;
            }
        }
        return null;
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback callback) {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.setLockWallpaperCallback(callback);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public boolean isWallpaperBackupEligible(int which) {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.isWallpaperBackupEligible(which, this.mContext.getUserId());
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception querying wallpaper backup eligibility: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public Context getContext() {
        return this.mContext;
    }

    public Bitmap peekBlurWallpaperBitmap(Rect rect) {
        return sGlobals.peekBlurWallpaperBitmap(rect);
    }

    public Bundle getUserWallpaperBounds(Context context) {
        return sGlobals.peekCurrentWallpaperBounds(context);
    }

    public Bitmap getBlurBitmap(Rect rect) {
        return null;
    }

    public void setCallback(Object callback) {
    }

    public void forgetLoadedBlurWallpaper() {
        sGlobals.forgetLoadedBlurWallpaper();
    }

    protected Bitmap createDefaultWallpaperBitmap(Bitmap bm) {
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return HwThemeManager.generateBitmap(this.mContext, bm, size.x < size.y ? size.x : size.y, size.x > size.y ? size.x : size.y);
    }

    public int[] getWallpaperStartingPoints() {
        return new int[]{-1, -1, -1, -1};
    }

    public void setBitmapWithOffsets(Bitmap bitmap, int[] offsets) throws IOException {
        setBitmap(bitmap);
    }

    public void setStreamWithOffsets(InputStream data, int[] offsets) throws IOException {
        setStream(data);
    }

    public int[] parseWallpaperOffsets(String srcFile) {
        return new int[]{-1, -1, -1, -1};
    }

    private int getWallpaperWidth() {
        return sGlobals.getWallpaperWidth();
    }

    private int getWallpaperHeight() {
        return sGlobals.getWallpaperHeight();
    }
}
