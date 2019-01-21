package android.widget.sr;

import android.cover.CoverManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.ColorSpace;
import android.graphics.ColorSpace.Named;
import android.graphics.ColorSpace.Rgb;
import android.graphics.ColorSpace.Rgb.TransferParameters;
import android.util.DisplayMetrics;

public class AshMemBitmap {
    public static final int NOT_ASH_BITMAP_FD = -1;
    public static final String TAG = "AshMemBitmap";

    public static NativeBitmap createSrcNativeBitmap(Bitmap bmp) {
        NativeBitmap resBmp;
        long ptr = BitmapUtils.getAshBitmapPtr(bmp);
        int fd = HwSuperResolution.nativeGetFdFromPtr(ptr);
        if (-1 == fd) {
            Bitmap ashSrcBmp = BitmapUtils.createAshBitmap(bmp);
            long srcPtr = BitmapUtils.getAshBitmapPtr(ashSrcBmp);
            int srcFd = HwSuperResolution.nativeGetFdFromPtr(srcPtr);
            if (-1 == srcFd) {
                return null;
            }
            resBmp = new NativeBitmap(ashSrcBmp, srcPtr, srcFd);
        } else {
            resBmp = new NativeBitmap(bmp, ptr, fd);
        }
        return resBmp;
    }

    public static NativeBitmap createDesNativeBitmap(int w, int h, int c, int ratio) {
        NativeBitmap resBmp = null;
        Bitmap desAshBmp = createSRBitmap(w * ratio, h * ratio, Config.ARGB_8888);
        if (desAshBmp != null) {
            long desPtr = BitmapUtils.getAshBitmapPtr(desAshBmp);
            int desFd = HwSuperResolution.nativeGetFdFromPtr(desPtr);
            if (-1 == desFd) {
                return null;
            }
            resBmp = new NativeBitmap(desAshBmp, desPtr, desFd);
        }
        return resBmp;
    }

    private static Bitmap createSRBitmap(int width, int height, Config config) {
        return createSRBitmap(null, width, height, config, true, ColorSpace.get(Named.SRGB));
    }

    private static Bitmap createSRBitmap(DisplayMetrics display, int width, int height, Config config, boolean hasAlpha, ColorSpace colorSpace) {
        DisplayMetrics displayMetrics = display;
        Config config2 = config;
        boolean z = hasAlpha;
        ColorSpace colorSpace2 = colorSpace;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        } else if (config2 == Config.HARDWARE) {
            throw new IllegalArgumentException("can't create mutable bitmap with Config.HARDWARE");
        } else if (colorSpace2 != null) {
            Bitmap bm;
            if (config2 != Config.ARGB_8888 || colorSpace2 == ColorSpace.get(Named.SRGB)) {
                bm = HwSuperResolution.nativeSRCreate(null, 0, width, width, height, 5, true, null, null);
            } else if (colorSpace2 instanceof Rgb) {
                Rgb rgb = (Rgb) colorSpace2;
                TransferParameters parameters = rgb.getTransferParameters();
                if (parameters != null) {
                    Rgb d50 = (Rgb) ColorSpace.adapt(rgb, ColorSpace.ILLUMINANT_D50);
                    bm = HwSuperResolution.nativeSRCreate(null, 0, width, width, height, 5, true, d50.getTransform(), parameters);
                } else {
                    Rgb rgb2 = rgb;
                    throw new IllegalArgumentException("colorSpace must use an ICC parametric transfer function");
                }
            } else {
                throw new IllegalArgumentException("colorSpace must be an RGB color space");
            }
            if (bm == null) {
                return null;
            }
            if (displayMetrics != null) {
                bm.setDensity(displayMetrics.densityDpi);
            }
            bm.setHasAlpha(z);
            if ((config2 == Config.ARGB_8888 || config2 == Config.RGBA_F16) && !z) {
                HwSuperResolution.nativeErase(BitmapUtils.getAshBitmapPtr(bm), CoverManager.DEFAULT_COLOR);
            }
            return bm;
        } else {
            throw new IllegalArgumentException("can't create bitmap without a color space");
        }
    }
}
