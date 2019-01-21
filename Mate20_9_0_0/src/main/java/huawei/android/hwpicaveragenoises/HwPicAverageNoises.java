package huawei.android.hwpicaveragenoises;

import android.app.ActivityThread;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.FreezeScreenScene;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.WindowManager;
import java.math.BigDecimal;

public class HwPicAverageNoises {
    private static final double PAD_SCREEN_SIZE_MIN = 6.5d;
    private static final String TAG = "HwPicAverageNoises";
    private static boolean isJniAverageLibExist;
    private static double mDeviceSize;
    private static IWindowManager mIWindowManager = Stub.asInterface(ServiceManager.checkService(FreezeScreenScene.WINDOW_PARAM));
    private static WindowManager mWindowManager = ((WindowManager) ActivityThread.currentApplication().getApplicationContext().getSystemService(FreezeScreenScene.WINDOW_PARAM));

    public native Bitmap jniFromNoiseBitmap(Bitmap bitmap);

    static {
        isJniAverageLibExist = true;
        try {
            System.loadLibrary("jniaverage");
        } catch (UnsatisfiedLinkError e) {
            isJniAverageLibExist = false;
            Log.e(TAG, "libjniaverage.so couldn't be found.");
        }
    }

    public Bitmap jniNoiseBitmap(Bitmap bitmapOld) {
        if (!isAverageNoiseSupported()) {
            return bitmapOld;
        }
        try {
            return jniFromNoiseBitmap(bitmapOld);
        } catch (UnsatisfiedLinkError e) {
            return bitmapOld;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x0115  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0115  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0115  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0112  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isAverageNoiseSupported() {
        RemoteException e;
        StringBuilder stringBuilder;
        boolean z = false;
        if (!isJniAverageLibExist) {
            return false;
        }
        String str;
        if (mDeviceSize > 0.0d) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mDeviceSize = ");
            stringBuilder2.append(mDeviceSize);
            Log.d(str, stringBuilder2.toString());
            if (mDeviceSize > PAD_SCREEN_SIZE_MIN) {
                z = true;
            }
            return z;
        }
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(dm);
        str = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getRealMetrics: dm.x = ");
        stringBuilder3.append(dm.xdpi);
        stringBuilder3.append(", dm.y = ");
        stringBuilder3.append(dm.ydpi);
        Log.i(str, stringBuilder3.toString());
        if (mIWindowManager != null) {
            double xInch;
            double yInch;
            Point point = new Point();
            try {
                mIWindowManager.getInitialDisplaySize(0, point);
                xInch = Math.pow((double) (((float) point.x) / dm.xdpi), 2.0d);
                try {
                    yInch = Math.pow((double) (((float) point.y) / dm.ydpi), 2.0d);
                } catch (RemoteException e2) {
                    e = e2;
                    Log.e(TAG, "RemoteException while calculate device size", e);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("dm.widthPixels = ");
                    stringBuilder.append(dm.widthPixels);
                    stringBuilder.append(", dm.heightPixels = ");
                    stringBuilder.append(dm.heightPixels);
                    Log.i(str, stringBuilder.toString());
                    mDeviceSize = new BigDecimal(Math.sqrt(Math.pow((double) (((float) dm.widthPixels) / dm.xdpi), 2.0d) + Math.pow((double) (((float) dm.heightPixels) / dm.ydpi), 2.0d))).setScale(2, 4).doubleValue();
                    if (mDeviceSize <= PAD_SCREEN_SIZE_MIN) {
                    }
                    return mDeviceSize <= PAD_SCREEN_SIZE_MIN;
                }
            } catch (RemoteException e3) {
                e = e3;
                Log.e(TAG, "RemoteException while calculate device size", e);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("dm.widthPixels = ");
                stringBuilder.append(dm.widthPixels);
                stringBuilder.append(", dm.heightPixels = ");
                stringBuilder.append(dm.heightPixels);
                Log.i(str, stringBuilder.toString());
                mDeviceSize = new BigDecimal(Math.sqrt(Math.pow((double) (((float) dm.widthPixels) / dm.xdpi), 2.0d) + Math.pow((double) (((float) dm.heightPixels) / dm.ydpi), 2.0d))).setScale(2, 4).doubleValue();
                if (mDeviceSize <= PAD_SCREEN_SIZE_MIN) {
                }
                return mDeviceSize <= PAD_SCREEN_SIZE_MIN;
            }
            try {
                mDeviceSize = new BigDecimal(Math.sqrt(xInch + yInch)).setScale(2, 4).doubleValue();
                return mDeviceSize > PAD_SCREEN_SIZE_MIN;
            } catch (RemoteException e4) {
                e = e4;
                double d = yInch;
                Log.e(TAG, "RemoteException while calculate device size", e);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("dm.widthPixels = ");
                stringBuilder.append(dm.widthPixels);
                stringBuilder.append(", dm.heightPixels = ");
                stringBuilder.append(dm.heightPixels);
                Log.i(str, stringBuilder.toString());
                mDeviceSize = new BigDecimal(Math.sqrt(Math.pow((double) (((float) dm.widthPixels) / dm.xdpi), 2.0d) + Math.pow((double) (((float) dm.heightPixels) / dm.ydpi), 2.0d))).setScale(2, 4).doubleValue();
                if (mDeviceSize <= PAD_SCREEN_SIZE_MIN) {
                }
                return mDeviceSize <= PAD_SCREEN_SIZE_MIN;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("dm.widthPixels = ");
        stringBuilder.append(dm.widthPixels);
        stringBuilder.append(", dm.heightPixels = ");
        stringBuilder.append(dm.heightPixels);
        Log.i(str, stringBuilder.toString());
        mDeviceSize = new BigDecimal(Math.sqrt(Math.pow((double) (((float) dm.widthPixels) / dm.xdpi), 2.0d) + Math.pow((double) (((float) dm.heightPixels) / dm.ydpi), 2.0d))).setScale(2, 4).doubleValue();
        return mDeviceSize <= PAD_SCREEN_SIZE_MIN;
    }

    public Bitmap addNoiseWithBlackBoard(Bitmap bitmap, int color) {
        Bitmap scaleBitmap = scaleBitmapToScreenSize(bitmap);
        Bitmap tmp = addBlackBoard(jniNoiseBitmap(scaleBitmap), color);
        if (scaleBitmap != null) {
            scaleBitmap.recycle();
        }
        return tmp;
    }

    private static Bitmap addBlackBoard(Bitmap bmp, int color) {
        Bitmap newBitmap;
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        synchronized (canvas) {
            newBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888);
            canvas.setBitmap(newBitmap);
            canvas.drawBitmap(bmp, 0.0f, 0.0f, paint);
            canvas.drawColor(color);
        }
        return newBitmap;
    }

    private static Bitmap scaleBitmapToScreenSize(Bitmap bitmap) {
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (context == null) {
        } else if (bitmap == null) {
            Context context2 = context;
        } else {
            int screenW = context.getResources().getDisplayMetrics().widthPixels;
            int screenH = context.getResources().getDisplayMetrics().heightPixels;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Matrix matrix = new Matrix();
            int scaleH = screenH / height;
            int scaleW = screenW / width;
            float scale = scaleH > scaleW ? (float) scaleH : (float) scaleW;
            matrix.postScale(scale, scale);
            context = scale;
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, Float.MIN_VALUE);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("width:");
            stringBuilder.append(width);
            stringBuilder.append(",height:");
            stringBuilder.append(height);
            stringBuilder.append(",screenW:");
            stringBuilder.append(screenW);
            stringBuilder.append(",screenH:");
            stringBuilder.append(screenH);
            stringBuilder.append(",scale:");
            stringBuilder.append(context);
            Log.i(str, stringBuilder.toString());
            return resizedBitmap;
        }
        return null;
    }
}
