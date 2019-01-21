package huawei.android.hwutil;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import java.io.IOException;
import java.io.InputStream;

public class AssetsFileCache {
    public static Drawable getDrawableEntry(AssetManager assets, Resources res, TypedValue value, String fileName, Options opts) {
        InputStream inputStream = null;
        if (assets == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDrawableEntry fileName : ");
            stringBuilder.append(fileName);
            stringBuilder.append("  fail , assets null");
            Log.w("AssetsFileCache ", stringBuilder.toString());
            return null;
        }
        Drawable dr = null;
        try {
            inputStream = assets.open(fileName);
            dr = Drawable.createFromResourceStream(res, value, inputStream, fileName, opts);
        } catch (IOException e) {
        } catch (Throwable th) {
            closeInputStream(inputStream);
        }
        closeInputStream(inputStream);
        return dr;
    }

    public static InputStream getInputStreamEntry(AssetManager assets, String fileName) {
        InputStream inputStream = null;
        if (assets == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDrawableEntry fileName : ");
            stringBuilder.append(fileName);
            stringBuilder.append("  fail , mAssets null");
            Log.w("AssetsFileCache ", stringBuilder.toString());
            return null;
        }
        try {
            inputStream = assets.open(fileName);
        } catch (IOException e) {
        }
        return inputStream;
    }

    public static Bitmap getBitmapEntry(AssetManager assets, Resources res, TypedValue value, String fileName, Rect padding) {
        if (assets == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getBitmapEntry fileName:");
            stringBuilder.append(fileName);
            stringBuilder.append(" fail , assets null");
            Log.w("AssetsFileCache ", stringBuilder.toString());
            return null;
        }
        int i;
        if (padding == null) {
            padding = new Rect();
        }
        Options opts = new Options();
        if (res != null) {
            i = res.getDisplayMetrics().noncompatDensityDpi;
        } else {
            i = DisplayMetrics.DENSITY_DEVICE;
        }
        opts.inScreenDensity = i;
        try {
            InputStream inputStream = assets.open(fileName);
            Bitmap bmp = BitmapFactory.decodeResourceStream(res, value, inputStream, padding, opts);
            if (bmp != null) {
                bmp.setDensity(res != null ? res.getDisplayMetrics().densityDpi : DisplayMetrics.DENSITY_DEVICE);
            }
            closeInputStream(inputStream);
            return bmp;
        } catch (Exception e) {
            closeInputStream(null);
            return null;
        } catch (Throwable th) {
            closeInputStream(null);
            throw th;
        }
    }

    private static void closeInputStream(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                Log.e("AssetsFileCache ", "closeInputStream IO Error");
            }
        }
    }
}
