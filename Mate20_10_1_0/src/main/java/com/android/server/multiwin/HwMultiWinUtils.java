package com.android.server.multiwin;

import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerGlobal;
import android.os.UserHandle;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.SurfaceControl;
import android.widget.ImageView;
import com.android.server.magicwin.HwMagicWindowService;
import com.android.server.multiwin.listener.BlurListener;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.HwMultiWindowManager;
import com.huawei.android.os.UserHandleEx;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class HwMultiWinUtils {
    private static final String BLUR_THREAD_NAME = "HwMultiWindow - BlurScreenShotThread";
    private static final int CORE_THREADS_NUM = 1;
    private static final int DEFAULT = -1;
    private static final int DEFAULT_BLUR_RADIUS = 15;
    private static final double EQ_DELTA_TH = 1.0E-6d;
    private static final String FLOAT_TASK_STATE_KEY = "float_task_state";
    private static final long KEEP_ALIVE_DURATION = 60;
    private static final int MAX_THREADS_NUM = 5;
    private static final int SAMPLE_SIZE = 12;
    private static final String TAG = "HwMultiWinUtils";
    private static ThreadPoolExecutor sPoolExecutor;

    private HwMultiWinUtils() {
    }

    public static boolean floatEquals(float f1, float f2) {
        return ((double) ((f1 > f2 ? 1 : (f1 == f2 ? 0 : -1)) > 0 ? f1 - f2 : f2 - f1)) <= EQ_DELTA_TH;
    }

    public static void blurForScreenShot(Bitmap inputBitmap, ImageView dstImageView, BlurListener blurListener, ImageView.ScaleType scaleType, int blurRadius) {
        if (inputBitmap == null || dstImageView == null) {
            Log.w(TAG, "blurForScreenShot failed, cause inputBitmap or dstImageView is null");
            return;
        }
        if (sPoolExecutor == null) {
            sPoolExecutor = new ThreadPoolExecutor(1, 5, KEEP_ALIVE_DURATION, TimeUnit.SECONDS, new LinkedBlockingQueue(), $$Lambda$HwMultiWinUtils$RY3fFIqVfo42JFoSnwPH0DNILAw.INSTANCE, new ThreadPoolExecutor.DiscardOldestPolicy());
        }
        sPoolExecutor.execute(new Runnable(dstImageView, inputBitmap, blurRadius, scaleType, blurListener) {
            /* class com.android.server.multiwin.$$Lambda$HwMultiWinUtils$rtOUP69xPNKsUdySRZpkRh7WRag */
            private final /* synthetic */ ImageView f$0;
            private final /* synthetic */ Bitmap f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ ImageView.ScaleType f$3;
            private final /* synthetic */ BlurListener f$4;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                HwMultiWinUtils.lambda$blurForScreenShot$2(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
            }
        });
    }

    static /* synthetic */ Thread lambda$blurForScreenShot$0(Runnable runnable) {
        return new Thread(runnable, BLUR_THREAD_NAME);
    }

    static /* synthetic */ void lambda$blurForScreenShot$2(ImageView dstImageView, Bitmap inputBitmap, int blurRadius, ImageView.ScaleType scaleType, BlurListener blurListener) {
        Bitmap outputBitmap = rsBlur(dstImageView.getContext(), inputBitmap, blurRadius);
        if (scaleType != ImageView.ScaleType.FIT_XY) {
            outputBitmap = Bitmap.createScaledBitmap(outputBitmap, inputBitmap.getWidth(), inputBitmap.getHeight(), true);
        }
        dstImageView.post(new Runnable(dstImageView, scaleType, outputBitmap, blurListener) {
            /* class com.android.server.multiwin.$$Lambda$HwMultiWinUtils$9rjgYv7IvYLT4z2G4GghCEXi2BU */
            private final /* synthetic */ ImageView f$0;
            private final /* synthetic */ ImageView.ScaleType f$1;
            private final /* synthetic */ Bitmap f$2;
            private final /* synthetic */ BlurListener f$3;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                HwMultiWinUtils.lambda$blurForScreenShot$1(this.f$0, this.f$1, this.f$2, this.f$3);
            }
        });
    }

    static /* synthetic */ void lambda$blurForScreenShot$1(ImageView dstImageView, ImageView.ScaleType scaleType, Bitmap dstBitmap, BlurListener blurListener) {
        dstImageView.setScaleType(scaleType);
        dstImageView.setImageDrawable(bitmap2Drawable(dstBitmap));
        if (blurListener != null) {
            blurListener.onBlurDone();
        }
    }

    public static void blurForScreenShot(ImageView sourceImageView, ImageView dstImageView, BlurListener blurListener, ImageView.ScaleType scaleType) {
        blurForScreenShot(drawable2Bitmap(sourceImageView.getDrawable()), dstImageView, blurListener, scaleType, 15);
    }

    public static void blurForScreenShot(Bitmap inputBitmap, ImageView dstImageView, BlurListener blurListener, ImageView.ScaleType scaleType) {
        blurForScreenShot(inputBitmap, dstImageView, blurListener, scaleType, 15);
    }

    public static int dip2px(Context context, float dipValue) {
        return (int) ((dipValue * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static Bitmap drawable2Bitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Log.w(TAG, "drawable2Bitmap: drawable = " + drawable + " is not instance of BitmapDrawable");
        return null;
    }

    public static Bitmap rsBlur(Context context, Bitmap bmp, int radius) {
        if (context == null) {
            Log.w(TAG, "context is null, rsBlur failed!");
            return bmp;
        } else if (bmp == null) {
            Log.w(TAG, "bmp is null, ruBlur failed!");
            return bmp;
        } else {
            Bitmap blurBmp = Bitmap.createScaledBitmap(bmp, Math.round(((float) bmp.getWidth()) / 12.0f), Math.round(((float) bmp.getHeight()) / 12.0f), false);
            if (blurBmp == null) {
                Log.w(TAG, "rsBlur failed, cause blurBmp is null!");
                return bmp;
            }
            RenderScript renderScript = RenderScript.create(context);
            if (renderScript == null) {
                Log.w(TAG, "rsBlur failed, cause renderScript is null!");
                return bmp;
            }
            Allocation input = Allocation.createFromBitmap(renderScript, blurBmp);
            if (input == null) {
                Log.w(TAG, "rsBlur failed, cause input is null!");
                return bmp;
            }
            Allocation output = Allocation.createTyped(renderScript, input.getType());
            if (output == null) {
                Log.w(TAG, "rsBlur failed, cause output is null!");
                return bmp;
            }
            ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            if (scriptIntrinsicBlur == null) {
                Log.w(TAG, "rsBlur failed, cause scriptIntrinsicBlur is null!");
                return bmp;
            }
            scriptIntrinsicBlur.setInput(input);
            scriptIntrinsicBlur.setRadius((float) radius);
            scriptIntrinsicBlur.forEach(output);
            output.copyTo(blurBmp);
            renderScript.destroy();
            return blurBmp;
        }
    }

    public static Point getDisplaySize() {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Log.w(TAG, "getDisplaySize failed, cause display is null!");
            return null;
        }
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        return displaySize;
    }

    public static Bitmap takeScreenshot(int topHeightSubtraction) {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Log.w(TAG, "takeScreenshot failed, cause display is null!");
            return null;
        }
        Point displaySize = getDisplaySize();
        if (displaySize == null) {
            return null;
        }
        int screenShotWidth = displaySize.x;
        int screenShotHeight = displaySize.y - topHeightSubtraction;
        int rotation = display.getRotation();
        Rect crop = new Rect(0, topHeightSubtraction, displaySize.x, displaySize.y);
        Log.i(TAG, "Taking screenshot of dimensions " + screenShotWidth + " x " + screenShotHeight + ", crop = " + crop);
        Bitmap screenShot = SurfaceControl.screenshot_ext_hw(crop, (int) (((float) screenShotWidth) / 2.0f), (int) (((float) screenShotHeight) / 2.0f), rotation);
        if (screenShot == null) {
            Log.w(TAG, "Failed to take screenshot of dimensions " + screenShotWidth + " x " + screenShotHeight);
            return null;
        }
        Bitmap softBmp = screenShot.copy(Bitmap.Config.ARGB_8888, true);
        if (softBmp == null) {
            Log.w(TAG, "Failed to copy soft bitmap!");
            return null;
        }
        softBmp.setHasAlpha(false);
        return softBmp;
    }

    public static Bitmap getScreenShotBmpWithoutNavBar(Bitmap src, int navBarPos, Rect navBarBound, float srcScale) {
        if (src == null) {
            Slog.w(TAG, "getScreenShotBmpWithoutNavBar failed, cause src is null!");
            return src;
        } else if (navBarPos == -1) {
            return src;
        } else {
            Bitmap temp = src;
            int navBarWidth = (int) (((float) navBarBound.width()) * srcScale);
            int navBarHeight = (int) (((float) navBarBound.height()) * srcScale);
            int sourceHeight = src.getHeight();
            int sourceWidth = src.getWidth();
            if (navBarPos == 1) {
                temp = Bitmap.createBitmap(src, navBarWidth, 0, sourceWidth - navBarWidth, sourceHeight);
            }
            if (navBarPos == 2) {
                temp = Bitmap.createBitmap(src, 0, 0, sourceWidth - navBarWidth, sourceHeight);
            }
            if (navBarPos == 4) {
                return Bitmap.createBitmap(src, 0, 0, sourceWidth, sourceHeight - navBarHeight);
            }
            return temp;
        }
    }

    public static int convertWindowMode2SplitMode(int windowMode, boolean isLandScape) {
        if (WindowConfiguration.isHwSplitScreenPrimaryWindowingMode(windowMode)) {
            return isLandScape ? 1 : 3;
        }
        if (WindowConfiguration.isHwSplitScreenSecondaryWindowingMode(windowMode)) {
            return isLandScape ? 2 : 4;
        }
        if (WindowConfiguration.isHwFreeFormWindowingMode(windowMode)) {
            return 5;
        }
        return 0;
    }

    public static boolean isNeedToResizeWithoutNavBar(int splitMode, int navBarPos) {
        if (splitMode == 1 && (navBarPos == 1 || navBarPos == 4)) {
            return true;
        }
        if (splitMode == 2 && (navBarPos == 2 || navBarPos == 4)) {
            return true;
        }
        if (splitMode == 3 && (navBarPos == 1 || navBarPos == 2)) {
            return true;
        }
        if (splitMode != 4) {
            return false;
        }
        if (navBarPos == 1 || navBarPos == 2 || navBarPos == 4) {
            return true;
        }
        return false;
    }

    public static Rect getBoundWithoutNavBar(int navBarPos, Rect navBarBound, Rect bound) {
        Rect outBound = new Rect(bound);
        if (navBarPos == 1) {
            outBound.left += navBarBound.width();
        }
        if (navBarPos == 2) {
            outBound.right -= navBarBound.width();
        }
        if (navBarPos == 4) {
            outBound.bottom -= navBarBound.height();
        }
        return outBound;
    }

    public static boolean isInNightMode(Context context) {
        if (context == null) {
            Log.w(TAG, "check if isInNightMode failed, cause context is null");
            return false;
        } else if ((context.getResources().getConfiguration().uiMode & 48) == 32) {
            return true;
        } else {
            return false;
        }
    }

    public static Drawable bitmap2Drawable(Bitmap bitmap) {
        return new BitmapDrawable(bitmap);
    }

    public static Drawable getAppIcon(Context context, String pkgName, int userId) {
        PackageManager pm;
        Slog.d(TAG, "get app icon: pkgName = " + pkgName);
        Drawable userBadgedIconDrawable = null;
        if (pkgName == null || (pm = context.getPackageManager()) == null) {
            return null;
        }
        ApplicationInfo info = null;
        try {
            info = pm.getApplicationInfoAsUser(pkgName, 0, userId);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "icon not load!");
        }
        Drawable iconDrawable = info == null ? null : info.loadIcon(pm);
        if (iconDrawable == null) {
            Log.w(TAG, "getAppIcon failed, cause iconDrawable is null!");
            return iconDrawable;
        }
        UserHandle userHandle = UserHandleEx.getUserHandle(userId);
        if (userHandle != null) {
            userBadgedIconDrawable = pm.getUserBadgedIcon(iconDrawable, userHandle);
        }
        Drawable showedIconDrawable = userBadgedIconDrawable == null ? iconDrawable : userBadgedIconDrawable;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm == null) {
            Log.w(TAG, "getAppIcon failed, cause DisplayMetrics is null!");
            return showedIconDrawable;
        }
        float density = dm.density;
        Drawable icon = new BitmapDrawable(drawable2Bitmap(showedIconDrawable));
        icon.setBounds(0, 0, (int) (density * 68.0f), (int) (68.0f * density));
        return icon;
    }

    public static int getFloatTaskState(Context context) {
        if (context == null) {
            return 0;
        }
        int state = Settings.Secure.getIntForUser(context.getContentResolver(), FLOAT_TASK_STATE_KEY, -1, -2);
        Log.i(TAG, "getSettingsSecureIntForUser: key=float_task_state, state=" + state + ", default=" + -1);
        return state;
    }

    public static void putFloatTaskStateToSettings(boolean isEnable, Context context) {
        if (context != null) {
            Settings.Secure.putIntForUser(context.getContentResolver(), FLOAT_TASK_STATE_KEY, isEnable ? 1 : 0, -2);
        }
    }

    public static Bitmap getWallpaperScreenShot(ActivityTaskManagerService atms) {
        if (atms == null) {
            Slog.w(TAG, "getWallpaperScreenShot failed, cause atms is null!");
            return null;
        }
        HwMultiWindowManager manager = HwMultiWindowManager.getInstance(atms);
        if (manager == null) {
            Slog.w(TAG, "getWallpaperScreenShot failed, cause manager is null!");
            return null;
        }
        HwMagicWindowService hwMagicWindowService = manager.getHwMagicWindowService();
        if (hwMagicWindowService != null) {
            return hwMagicWindowService.getWallpaperScreenShot();
        }
        Slog.w(TAG, "getWallpaperScreenShot failed, cause hwMagicWS is null!");
        return null;
    }

    public static Bitmap getStatusBarScreenShot(int statusBarWidth, int statusBarHeight) {
        Rect sourceCrop = new Rect(0, 0, statusBarWidth, statusBarHeight);
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display != null) {
            return SurfaceControl.screenshot_ext_hw(sourceCrop, statusBarWidth, statusBarHeight, display.getRotation());
        }
        Log.w(TAG, "getStatusBarScreenShot failed, cause display is null!");
        return null;
    }
}
