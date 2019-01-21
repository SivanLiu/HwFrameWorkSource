package com.android.server.wallpaper;

import android.app.IWallpaperManagerCallback;
import android.app.WallpaperInfo;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hwtheme.HwThemeManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.IWindowManager.Stub;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import com.android.server.BlurUtils;
import com.android.server.SystemService;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.wallpaper.WallpaperManagerService.WallpaperData;
import huawei.android.hwpicaveragenoises.HwPicAverageNoises;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HwWallpaperManagerService extends WallpaperManagerService {
    public static final boolean DEBUG = false;
    private static final String PERMISSION = "com.huawei.wallpaperservcie.permission.SET_WALLPAPER_OFFSET";
    private static final String ROG_KILL_APP_END_ACTION = "com.huawei.systemmamanger.action.KILL_ROGAPP_END";
    static final String TAG = "HwWallpaperService";
    public static final String WALLPAPER_MANAGER_SERVICE_READY_ACTION = "android.intent.action.WALLPAPER_MANAGER_SERVICE_READY";
    protected int disHeight;
    protected int disWidth;

    public static class Lifecycle extends SystemService {
        private HwWallpaperManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mService = new HwWallpaperManagerService(getContext());
            publishBinderService("wallpaper", this.mService);
        }

        public void onBootPhase(int phase) {
            String str = HwWallpaperManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwWallpaperManagerService.java#Lifecycle#systemRunning() phase:");
            stringBuilder.append(phase);
            Slog.d(str, stringBuilder.toString());
            if (phase == 550) {
                this.mService.systemRunning();
            } else if (phase == 600) {
                this.mService.switchUser(0, null);
            } else if (phase == 1000) {
                this.mService.wallpaperManagerServiceReady();
            }
        }

        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    public HwWallpaperManagerService(Context context) {
        super(context);
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay().getMetrics(dm);
        this.disWidth = dm.widthPixels;
        this.disHeight = dm.heightPixels;
    }

    public void wallpaperManagerServiceReady() {
        createBlurWallpaper(false);
        this.mContext.sendBroadcastAsUser(new Intent(WALLPAPER_MANAGER_SERVICE_READY_ACTION), new UserHandle(this.mCurrentUserId));
        Slog.i(TAG, "HwWallpaperManagerService.java#wallpaperManagerServiceReady() sendBroadcast[WALLPAPER_MANAGER_SERVICE_READY_ACTION]");
    }

    public void systemRunning() {
        super.systemReady();
        WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(0);
        synchronized (getLock()) {
            notifyBlurCallbacksLocked(wallpaper);
        }
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction(ROG_KILL_APP_END_ACTION);
        getContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (HwWallpaperManagerService.ROG_KILL_APP_END_ACTION.equals(intent.getAction())) {
                    HwWallpaperManagerService.this.restartLiveWallpaperService();
                }
            }
        }, userFilter);
    }

    public void handleWallpaperObserverEvent(final WallpaperData wallpaper) {
        new Thread("BlurWallpaperThread") {
            public void run() {
                Slog.d(HwWallpaperManagerService.TAG, "onEvent createBlurBitmap Blur wallpaper Begin");
                long timeBegin = System.currentTimeMillis();
                HwWallpaperManagerService.this.createBlurWallpaper(true);
                long takenTime = System.currentTimeMillis() - timeBegin;
                String str = HwWallpaperManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onEvent createBlurBitmap Blur takenTime = ");
                stringBuilder.append(takenTime);
                Slog.d(str, stringBuilder.toString());
                synchronized (HwWallpaperManagerService.this.getLock()) {
                    HwWallpaperManagerService.this.notifyBlurCallbacksLocked(wallpaper);
                }
            }
        }.start();
    }

    public void createBlurWallpaper(boolean force) {
        synchronized (getLock()) {
            Bitmap wallpaper;
            Bitmap wallpaper2;
            if (!force) {
                try {
                    if (new File(Environment.getUserSystemDirectory(getWallpaperUserId()), "blurwallpaper").exists()) {
                        return;
                    }
                } catch (OutOfMemoryError e) {
                    Slog.w(TAG, "No memory load current wallpaper", e);
                } catch (Throwable th) {
                }
            }
            String bitmapStyle = "";
            WallpaperInfo info = getWallpaperInfo(getWallpaperUserId());
            String bitmapStyle2;
            if (info == null) {
                wallpaper = getCurrentWallpaperLocked(getContext());
                bitmapStyle2 = "static";
            } else {
                Drawable dr = HwFrameworkFactory.getHwWallpaperInfoStub(info).loadThumbnailWithoutTheme(getContext().getPackageManager());
                if (dr == null) {
                    wallpaper2 = null;
                } else if (dr instanceof BitmapDrawable) {
                    wallpaper2 = ((BitmapDrawable) dr).getBitmap();
                } else {
                    try {
                        wallpaper = Bitmap.createBitmap(dr.getIntrinsicWidth(), dr.getIntrinsicHeight(), dr.getOpacity() != -1 ? Config.ARGB_8888 : Config.RGB_565);
                        Canvas c = new Canvas(wallpaper);
                        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
                        dr.draw(c);
                    } catch (RuntimeException e2) {
                        Slog.e(TAG, "create blurwallpaper draw has some errors");
                        wallpaper2 = null;
                    }
                    bitmapStyle2 = "live";
                }
                wallpaper = wallpaper2;
                bitmapStyle2 = "live";
            }
            if (wallpaper == null || wallpaper.isRecycled()) {
                Slog.d(TAG, "wallpaper is null or has been recycled");
                wallpaper = getDefaultWallpaperLocked(getContext());
            }
            if (wallpaper != null) {
                Bitmap bitmap;
                int h = this.disHeight;
                int w = this.disWidth;
                if (!ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
                    h += getContext().getResources().getDimensionPixelSize(17105186);
                }
                if (wallpaper.getHeight() == h && wallpaper.getWidth() == 2 * w) {
                    int[] inPixels = new int[(this.disWidth * h)];
                    Bitmap bitmap2 = Bitmap.createBitmap(this.disWidth, h, Config.ARGB_8888);
                    wallpaper.getPixels(inPixels, 0, this.disWidth, 0, 0, this.disWidth, h);
                    bitmap2.setPixels(inPixels, 0, this.disWidth, 0, 0, this.disWidth, h);
                    bitmap = bitmap2;
                    if (!(bitmap == wallpaper || wallpaper.isRecycled())) {
                        wallpaper.recycle();
                        wallpaper = bitmap;
                        if (info != null) {
                            removeThumbnailCache(info);
                        }
                    }
                }
                bitmap = wallpaper;
                if (!(wallpaper.getHeight() == h / 4 && wallpaper.getWidth() == w / 4)) {
                    wallpaper = Bitmap.createScaledBitmap(bitmap, this.disWidth / 4, h / 4, true);
                    if (!(wallpaper == null || wallpaper == bitmap || bitmap.isRecycled())) {
                        bitmap.recycle();
                        if (info != null) {
                            removeThumbnailCache(info);
                        }
                    }
                }
            }
            if (wallpaper != null) {
                wallpaper2 = BlurUtils.stackBlur(wallpaper, 80);
                if (wallpaper2 != null) {
                    wallpaper.recycle();
                    if (HwPicAverageNoises.isAverageNoiseSupported()) {
                        saveBlurWallpaperBitmapLocked(wallpaper2);
                    } else {
                        Bitmap blurImg = BlurUtils.addBlackBoard(wallpaper2, 1275068416);
                        if (blurImg != null) {
                            wallpaper2.recycle();
                            saveBlurWallpaperBitmapLocked(blurImg);
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:29:0x0056, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ParcelFileDescriptor getBlurWallpaper(IWallpaperManagerCallback cb) {
        synchronized (getLock()) {
            int wallpaperUserId = getWallpaperUserId();
            WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(wallpaperUserId);
            if (wallpaper != null) {
                if (wallpaper.getCallbacks() != null) {
                    if (!(cb == null || wallpaper.getCallbacks().isContainIBinder(cb))) {
                        wallpaper.getCallbacks().register(cb);
                    }
                    try {
                        File f = new File(Environment.getUserSystemDirectory(wallpaperUserId), "blurwallpaper");
                        if (f.exists()) {
                            ParcelFileDescriptor open = ParcelFileDescriptor.open(f, 268435456);
                            return open;
                        }
                        return null;
                    } catch (FileNotFoundException e) {
                        Slog.w(TAG, "Error getting wallpaper", e);
                        return null;
                    }
                }
            }
        }
    }

    public int getWallpaperUserId() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            return getCurrentUserId();
        }
        return UserHandle.getUserId(callingUid);
    }

    private Bitmap getCurrentWallpaperLocked(Context context) {
        ParcelFileDescriptor fd = getWallpaper(new Bundle());
        if (fd != null) {
            try {
                Bitmap bm = scaleWallpaperBitmapToScreenSize(BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, new Options()));
                try {
                    fd.close();
                } catch (IOException e) {
                }
                return bm;
            } catch (OutOfMemoryError e2) {
                Slog.w(TAG, "Can't decode file", e2);
                try {
                    fd.close();
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                try {
                    fd.close();
                } catch (IOException e4) {
                }
                throw th;
            }
        }
        return null;
    }

    private Bitmap getDefaultWallpaperLocked(Context context) {
        InputStream is = HwThemeManager.getDefaultWallpaperIS(context, getCurrentUserId());
        if (is != null) {
            try {
                Bitmap bm = scaleWallpaperBitmapToScreenSize(BitmapFactory.decodeStream(is, null, new Options()));
                try {
                    is.close();
                } catch (IOException e) {
                }
                return bm;
            } catch (OutOfMemoryError e2) {
                Slog.w(TAG, "Can't decode stream", e2);
                try {
                    is.close();
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                try {
                    is.close();
                } catch (IOException e4) {
                }
                throw th;
            }
        }
        return null;
    }

    private void notifyBlurCallbacksLocked(WallpaperData wallpaper) {
        RemoteCallbackList<IWallpaperManagerCallback> callbacks = wallpaper.getCallbacks();
        synchronized (callbacks) {
            int n = callbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((IWallpaperManagerCallback) callbacks.getBroadcastItem(i)).onBlurWallpaperChanged();
                } catch (RemoteException e) {
                }
            }
            callbacks.finishBroadcast();
        }
    }

    public ParcelFileDescriptor getWallpaper(Bundle outParams) {
        synchronized (getLock()) {
            int wallpaperUserId = getWallpaperUserId();
            WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(wallpaperUserId);
            if (outParams != null) {
                try {
                    outParams.putInt("width", wallpaper.getWidth());
                    outParams.putInt("height", wallpaper.getHeight());
                } catch (FileNotFoundException e) {
                    Slog.w(TAG, "Error getting wallpaper", e);
                    return null;
                }
            }
            File f = new File(Environment.getUserSystemDirectory(wallpaperUserId), "wallpaper");
            if (f.exists()) {
                ParcelFileDescriptor open = ParcelFileDescriptor.open(f, 268435456);
                return open;
            }
            return null;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:30:0x0087=Splitter:B:30:0x0087, B:19:0x005a=Splitter:B:19:0x005a} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void saveBlurWallpaperBitmapLocked(Bitmap blurImg) {
        Slog.d(TAG, "saveBlurWallpaperBitmapLocked begin");
        long timeBegin = System.currentTimeMillis();
        ParcelFileDescriptor fd;
        FileOutputStream fos;
        try {
            File dir = Environment.getUserSystemDirectory(getWallpaperUserId());
            if (!dir.exists() && dir.mkdir()) {
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            File file = new File(dir, "blurwallpaper");
            fd = ParcelFileDescriptor.open(file, 939524096);
            if (SELinux.restorecon(file)) {
                fos = null;
                fos = new AutoCloseOutputStream(fd);
                blurImg.compress(CompressFormat.PNG, 90, fos);
                try {
                    fos.close();
                } catch (IOException e) {
                }
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e2) {
                    }
                }
                long takenTime = System.currentTimeMillis() - timeBegin;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("saveBlurWallpaperBitmapLocked takenTime = ");
                stringBuilder.append(takenTime);
                Slog.d(str, stringBuilder.toString());
            }
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e3) {
                }
            }
        } catch (FileNotFoundException e4) {
            Slog.w(TAG, "Error setting wallpaper", e4);
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e5) {
                }
            }
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e6) {
                }
            }
        }
    }

    private Bitmap generateBitmap(Context context, Bitmap bm, int width, int height, WallpaperInfo info) {
        OutOfMemoryError e;
        Throwable th;
        Context context2 = context;
        Bitmap bm2 = bm;
        int i = width;
        int i2 = height;
        WallpaperInfo wallpaperInfo = info;
        synchronized (getLock()) {
            if (context2 == null || bm2 == null) {
                return null;
            }
            try {
                WindowManager wm = (WindowManager) context2.getSystemService("window");
                DisplayMetrics metrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(metrics);
                bm2.setDensity(metrics.noncompatDensityDpi);
                if (i <= 0 || i2 <= 0 || (bm.getWidth() == i && bm.getHeight() == i2)) {
                    return bm2;
                }
                try {
                    Bitmap newBimap = Bitmap.createBitmap(i, i2, Config.ARGB_8888);
                    if (newBimap == null) {
                        Slog.w(TAG, "Can't generate bitmap, newBimap = null");
                        return bm2;
                    }
                    newBimap.setDensity(metrics.noncompatDensityDpi);
                    Canvas c = new Canvas(newBimap);
                    Rect targetRect = new Rect();
                    targetRect.right = bm.getWidth();
                    targetRect.bottom = bm.getHeight();
                    int deltaw = i - targetRect.right;
                    int deltah = i2 - targetRect.bottom;
                    if (deltaw > 0 || deltah > 0) {
                        float tempWidth = ((float) i) / ((float) targetRect.right);
                        float tempHeight = ((float) i2) / ((float) targetRect.bottom);
                        float scale = tempWidth > tempHeight ? tempWidth : tempHeight;
                        targetRect.right = (int) (((float) targetRect.right) * scale);
                        targetRect.bottom = (int) (((float) targetRect.bottom) * scale);
                        deltaw = i - targetRect.right;
                        deltah = i2 - targetRect.bottom;
                    }
                    targetRect.offset(deltaw / 2, deltah / 2);
                    Paint paint = new Paint();
                    paint.setFilterBitmap(true);
                    paint.setXfermode(new PorterDuffXfermode(Mode.SRC));
                    c.drawBitmap(bm2, null, targetRect, paint);
                    bm.recycle();
                    bm2 = null;
                    if (wallpaperInfo != null) {
                        try {
                            removeThumbnailCache(wallpaperInfo);
                        } catch (OutOfMemoryError e2) {
                            e = e2;
                        }
                    }
                    return newBimap;
                } catch (OutOfMemoryError e3) {
                    e = e3;
                    Slog.w(TAG, "Can't generate bitmap:", e);
                    return bm2;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private void removeThumbnailCache(WallpaperInfo info) {
        if (info != null) {
            info.removeThumbnailCache(getContext().getPackageManager());
        }
    }

    protected boolean getHwWallpaperWidth(WallpaperData wallpaper, boolean success) {
        if (success) {
            return false;
        }
        Display d = ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay();
        Point pSize = new Point();
        d.getSize(pSize);
        wallpaper.setWidth(SystemProperties.getInt("ro.config.hwwallpaperwidth", 2 * pSize.x));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadSettings, mWidth:");
        stringBuilder.append(wallpaper.getWidth());
        stringBuilder.append(", mHeight:");
        stringBuilder.append(wallpaper.getHeight());
        stringBuilder.append(", display size:");
        stringBuilder.append(pSize);
        Slog.i(str, stringBuilder.toString());
        return true;
    }

    private void saveWallpaperOffsets(int[] offsets) {
        if (offsets != null) {
            int i;
            StringBuilder sb = new StringBuilder();
            for (i = 0; i < offsets.length; i++) {
                sb.append(offsets[i]);
                if (i < offsets.length - 1) {
                    sb.append(",");
                }
            }
            i = getWallpaperUserId();
            System.putStringForUser(getContext().getContentResolver(), "curr_wallpaper_offsets", sb.toString(), i);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("saveWallpaperOffsets(");
            stringBuilder.append(i);
            stringBuilder.append("):");
            stringBuilder.append(sb);
            Slog.i(str, stringBuilder.toString());
        }
    }

    protected void updateWallpaperOffsets(WallpaperData wallpaper) {
        WallpaperData wallpaperData = wallpaper;
        Bitmap bm = getCurrentWallpaperLocked(getContext());
        int x_port;
        int y_port;
        if (bm != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            Display display = ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay();
            display.getMetrics(metrics);
            bm.setDensity(metrics.noncompatDensityDpi);
            Point size = new Point();
            display.getRealSize(size);
            int min = size.x < size.y ? size.x : size.y;
            int max = size.x > size.y ? size.x : size.y;
            int width = bm.getWidth();
            int height = bm.getHeight();
            int i = 0;
            if ((height != max && width != max) || (width != 2 * min && width != max)) {
                String str = TAG;
                x_port = 0;
                int x_port2 = new StringBuilder();
                y_port = 1;
                x_port2.append("Irregular bitmap: width=");
                x_port2.append(width);
                x_port2.append(", height=");
                x_port2.append(height);
                Slog.w(str, x_port2.toString());
                while (true) {
                    x_port2 = i;
                    if (x_port2 >= wallpaperData.currOffsets.length) {
                        break;
                    }
                    wallpaperData.currOffsets[x_port2] = -1;
                    i = x_port2 + 1;
                }
            } else {
                x_port = 0;
                y_port = 1;
                if (wallpaperData.nextOffsets[0] == -1) {
                    wallpaperData.currOffsets[0] = -1;
                } else if (wallpaperData.nextOffsets[0] < -1) {
                    wallpaperData.currOffsets[0] = 0;
                } else if (wallpaperData.nextOffsets[0] <= width - min) {
                    wallpaperData.currOffsets[0] = wallpaperData.nextOffsets[0];
                } else if (wallpaperData.nextOffsets[0] > width - min) {
                    wallpaperData.currOffsets[0] = width - min;
                }
                wallpaperData.currOffsets[1] = 0;
                wallpaperData.currOffsets[2] = 0;
                if (wallpaperData.nextOffsets[3] == -1) {
                    wallpaperData.currOffsets[3] = -1;
                } else if (wallpaperData.nextOffsets[3] < -1) {
                    wallpaperData.currOffsets[3] = 0;
                } else if (wallpaperData.nextOffsets[3] <= height - min) {
                    wallpaperData.currOffsets[3] = wallpaperData.nextOffsets[3];
                } else if (wallpaperData.nextOffsets[3] > height - min) {
                    wallpaperData.currOffsets[3] = height - min;
                }
            }
            saveWallpaperOffsets(wallpaperData.currOffsets);
            if (!bm.isRecycled()) {
                bm.recycle();
                return;
            }
            return;
        }
        x_port = 0;
        y_port = 1;
    }

    public int[] getCurrOffsets() throws RemoteException {
        int[] offsets;
        synchronized (getLock()) {
            offsets = new int[]{-1, -1, -1, -1};
            int userId = getWallpaperUserId();
            WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(userId);
            String wallpaperOffsets = System.getStringForUser(getContext().getContentResolver(), "curr_wallpaper_offsets", userId);
            if (wallpaperOffsets != null) {
                String[] str = wallpaperOffsets.split(",");
                if (str.length == wallpaper.currOffsets.length) {
                    for (int i = 0; i < wallpaper.currOffsets.length; i++) {
                        wallpaper.currOffsets[i] = Integer.parseInt(str[i]);
                    }
                }
            }
            System.arraycopy(wallpaper.currOffsets, 0, offsets, 0, wallpaper.currOffsets.length);
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrOffsets(");
            stringBuilder.append(userId);
            stringBuilder.append("):");
            stringBuilder.append(offsets[0]);
            stringBuilder.append(",");
            stringBuilder.append(offsets[1]);
            stringBuilder.append(",");
            stringBuilder.append(offsets[2]);
            stringBuilder.append(",");
            stringBuilder.append(offsets[3]);
            Slog.i(str2, stringBuilder.toString());
        }
        return offsets;
    }

    public void setCurrOffsets(int[] offsets) throws RemoteException {
        getContext().enforceCallingOrSelfPermission(PERMISSION, null);
        synchronized (getLock()) {
            WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(getWallpaperUserId());
            if (!(offsets == null || wallpaper == null || offsets.length != wallpaper.currOffsets.length)) {
                System.arraycopy(offsets, 0, wallpaper.currOffsets, 0, wallpaper.currOffsets.length);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setCurrOffsets:");
                stringBuilder.append(offsets[0]);
                stringBuilder.append(",");
                stringBuilder.append(offsets[1]);
                stringBuilder.append(",");
                stringBuilder.append(offsets[2]);
                stringBuilder.append(",");
                stringBuilder.append(offsets[3]);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    public void setNextOffsets(int[] offsets) throws RemoteException {
        synchronized (getLock()) {
            WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(UserHandle.getCallingUserId());
            if (!(offsets == null || wallpaper == null || offsets.length != wallpaper.nextOffsets.length)) {
                System.arraycopy(offsets, 0, wallpaper.nextOffsets, 0, wallpaper.nextOffsets.length);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setNextOffsets:");
                stringBuilder.append(offsets[0]);
                stringBuilder.append(",");
                stringBuilder.append(offsets[1]);
                stringBuilder.append(",");
                stringBuilder.append(offsets[2]);
                stringBuilder.append(",");
                stringBuilder.append(offsets[3]);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    public Bitmap scaleWallpaperBitmapToScreenSize(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap bmp;
        Matrix m = new Matrix();
        Point size = new Point();
        int longSideLength = 0;
        int shortSideLength = 0;
        int bitmap_height = bitmap.getHeight();
        int bitmap_width = bitmap.getWidth();
        int bitmapLongSideLength = bitmap_height > bitmap_width ? bitmap_height : bitmap_width;
        int bitmapShortSideLength = bitmap_height > bitmap_width ? bitmap_width : bitmap_height;
        try {
            Stub.asInterface(ServiceManager.checkService("window")).getBaseDisplaySize(0, size);
            longSideLength = size.x > size.y ? size.x : size.y;
            shortSideLength = size.x > size.y ? size.y : size.x;
        } catch (RemoteException e) {
            Slog.e(TAG, "IWindowManager null");
        }
        int shortSideLength2 = shortSideLength;
        if (longSideLength == 0 || shortSideLength2 == 0 || bitmapLongSideLength == longSideLength || (bitmapLongSideLength == 2 * shortSideLength2 && bitmapShortSideLength == longSideLength)) {
            int i = longSideLength;
            bmp = bitmap;
        } else {
            if ((((float) bitmapLongSideLength) / ((float) shortSideLength2)) / (((float) bitmapShortSideLength) / ((float) longSideLength)) == 2.0f) {
                longSideLength = shortSideLength2 * 2;
            }
            int longSideLength2 = longSideLength;
            float scale = ((float) longSideLength2) / ((float) bitmapLongSideLength);
            m.setScale(scale, scale);
            bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap_width, bitmap_height, m, 1);
        }
        return bmp;
    }

    private void restartLiveWallpaperService() {
        WallpaperData wallpaper = (WallpaperData) getWallpaperMap().get(getWallpaperUserId());
        if (wallpaper != null && wallpaper.wallpaperComponent != null) {
            Slog.d(TAG, "restartLiveWallpaperService ");
            bindWallpaperComponentLocked(wallpaper.wallpaperComponent, true, false, wallpaper, null);
        }
    }

    public void reportWallpaper(ComponentName componentName) {
        if (componentName != null) {
            HwSysResManager resManager = HwSysResManager.getInstance();
            if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC))) {
                Bundle bundleArgs = new Bundle();
                bundleArgs.putString(MemoryConstant.MEM_PREREAD_ITEM_NAME, componentName.getPackageName());
                bundleArgs.putInt("relationType", 17);
                CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
                long id = Binder.clearCallingIdentity();
                resManager.reportData(data);
                Binder.restoreCallingIdentity(id);
            }
        }
    }
}
