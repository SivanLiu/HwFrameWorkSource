package com.tencent.qqimagecompare;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.ExifInterface;
import tmsdk.common.utils.f;

public class QQImageLoader {
    static boolean nK = false;

    public static int DecodeJpegFileScale(String str, int i, QQImageBitmap qQImageBitmap) {
        return DecodeJpegFileScaleC1s1i1l(str, i, qQImageBitmap.mThisC);
    }

    private static native int DecodeJpegFileScaleC(String str, int i, Bitmap bitmap);

    private static native int DecodeJpegFileScaleC1s1i1l(String str, int i, long j);

    private static native int DecodeJpegFileScaleMemC(byte[] bArr, int i, Bitmap bitmap);

    private static native int DecodeJpegFileSubImageC(String str, int i, int i2, int i3, int i4, int i5, int i6, Bitmap bitmap);

    public static QQImageLoaderHeadInfo GetImageHeadInfo(String str) {
        Object -l_1_R = new QQImageLoaderHeadInfo();
        if (nK && IsJpegFileC(str)) {
            GetJpegHeadInfoC(str, -l_1_R);
            -l_1_R.bJpeg = true;
        } else {
            Object -l_2_R = new Options();
            -l_2_R.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(str, -l_2_R);
            -l_1_R.width = -l_2_R.outWidth;
            -l_1_R.height = -l_2_R.outHeight;
            -l_1_R.bJpeg = false;
        }
        return -l_1_R;
    }

    private static native int GetJpegHeadInfoC(String str, QQImageLoaderHeadInfo qQImageLoaderHeadInfo);

    private static native int GetJpegHeadInfoMemC(byte[] bArr, QQImageLoaderHeadInfo qQImageLoaderHeadInfo);

    static native boolean IsJpegFileC(String str);

    private static QQImageLoaderHeadInfo X(String str) {
        Object -l_1_R = new QQImageLoaderHeadInfo();
        return GetJpegHeadInfoC(str, -l_1_R) == 0 ? -l_1_R : null;
    }

    private static Bitmap Y(String str) {
        Object -l_2_R = null;
        try {
            -l_2_R = new ExifInterface(str);
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
        }
        if (-l_2_R == null) {
            return null;
        }
        byte[] -l_3_R2 = -l_2_R.getThumbnail();
        return -l_3_R2 != null ? a(-l_3_R2, 100, 100) : null;
    }

    private static int a(int i, int i2) {
        return ((i + i2) - 1) / i2;
    }

    private static Bitmap a(String str, int i, int i2) {
        Object -l_4_R = new QQImageLoaderHeadInfo();
        if (GetJpegHeadInfoC(str, -l_4_R) != 0) {
            return null;
        }
        -l_4_R.bJpeg = true;
        int -l_5_I = calculateInSampleSize_1_8_max(-l_4_R.width, -l_4_R.height, i, i2);
        int -l_6_I = -l_4_R.width;
        int -l_7_I = -l_4_R.height;
        int -l_8_I = 1;
        switch (-l_5_I) {
            case 1:
                -l_8_I = 0;
                break;
            case 2:
                -l_8_I = 1;
                -l_6_I = a(-l_4_R.width << 2, 8);
                -l_7_I = a(-l_4_R.height << 2, 8);
                break;
            case 4:
                -l_8_I = 2;
                -l_6_I = a(-l_4_R.width << 1, 8);
                -l_7_I = a(-l_4_R.height << 1, 8);
                break;
            case 8:
                -l_8_I = 3;
                -l_6_I = a(-l_4_R.width, 8);
                -l_7_I = a(-l_4_R.height, 8);
                break;
        }
        Object -l_3_R = Bitmap.createBitmap(-l_6_I, -l_7_I, Config.ARGB_8888);
        if (DecodeJpegFileScaleC(str, -l_8_I, -l_3_R) == 0) {
            return -l_3_R;
        }
        -l_3_R.recycle();
        return null;
    }

    private static Bitmap a(byte[] bArr, int i, int i2) {
        Object -l_4_R = new QQImageLoaderHeadInfo();
        if (GetJpegHeadInfoMemC(bArr, -l_4_R) != 0) {
            return null;
        }
        -l_4_R.bJpeg = true;
        int -l_5_I = calculateInSampleSize_1_8_max(-l_4_R.width, -l_4_R.height, i, i2);
        int -l_6_I = -l_4_R.width;
        int -l_7_I = -l_4_R.height;
        int -l_8_I = 1;
        switch (-l_5_I) {
            case 1:
                -l_8_I = 0;
                break;
            case 2:
                -l_8_I = 1;
                -l_6_I = a(-l_4_R.width << 2, 8);
                -l_7_I = a(-l_4_R.height << 2, 8);
                break;
            case 4:
                -l_8_I = 2;
                -l_6_I = a(-l_4_R.width << 1, 8);
                -l_7_I = a(-l_4_R.height << 1, 8);
                break;
            case 8:
                -l_8_I = 3;
                -l_6_I = a(-l_4_R.width, 8);
                -l_7_I = a(-l_4_R.height, 8);
                break;
        }
        Object -l_3_R = Bitmap.createBitmap(-l_6_I, -l_7_I, Config.ARGB_8888);
        if (DecodeJpegFileScaleMemC(bArr, -l_8_I, -l_3_R) == 0) {
            return -l_3_R;
        }
        -l_3_R.recycle();
        return null;
    }

    public static int calculateInSampleSize(Options options, int i, int i2) {
        int -l_3_I = options.outHeight;
        int -l_4_I = options.outWidth;
        int -l_5_I = 1;
        if (-l_3_I > i2 || -l_4_I > i) {
            int -l_6_I = -l_3_I / 2;
            int -l_7_I = -l_4_I / 2;
            while (-l_6_I / -l_5_I > i2 && -l_7_I / -l_5_I > i) {
                -l_5_I *= 2;
            }
        }
        return -l_5_I;
    }

    public static int calculateInSampleSize_1_8_max(int i, int i2, int i3, int i4) {
        int -l_4_I = 1;
        if (i > i4 || i2 > i3) {
            int -l_5_I = i / 2;
            int -l_6_I = i2 / 2;
            while (-l_5_I / -l_4_I > i4 && -l_6_I / -l_4_I > i3) {
                -l_4_I *= 2;
                if (-l_4_I == 8) {
                    break;
                }
            }
        }
        return -l_4_I;
    }

    public static int calculateInSampleSize_1_8_max(Options options, int i, int i2) {
        int -l_3_I = options.outHeight;
        int -l_4_I = options.outWidth;
        int -l_5_I = 1;
        if (-l_3_I > i2 || -l_4_I > i) {
            int -l_6_I = -l_3_I / 2;
            int -l_7_I = -l_4_I / 2;
            while (-l_6_I / -l_5_I > i2 && -l_7_I / -l_5_I > i) {
                -l_5_I *= 2;
                if (-l_5_I == 8) {
                    break;
                }
            }
        }
        return -l_5_I;
    }

    public static Bitmap decodeSampledBitmapFromFile(String str, int i, int i2) {
        Bitmap bitmap = null;
        Object -l_4_R = new Options();
        -l_4_R.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeFile(str, -l_4_R);
            -l_4_R.inSampleSize = calculateInSampleSize(-l_4_R, i, i2);
            -l_4_R.inJustDecodeBounds = false;
            long -l_5_J = System.currentTimeMillis();
            bitmap = BitmapFactory.decodeFile(str, -l_4_R);
            String str2 = "QQImageCompare";
            f.f(str2, "decode file time: " + (System.currentTimeMillis() - -l_5_J));
            return bitmap;
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
            return bitmap;
        }
    }

    private static QQImageLoaderHeadInfo e(byte[] bArr) {
        Object -l_1_R = new QQImageLoaderHeadInfo();
        return GetJpegHeadInfoMemC(bArr, -l_1_R) == 0 ? -l_1_R : null;
    }

    public static Bitmap loadBitmap100x100FromFile(String str) {
        Bitmap -l_6_R;
        Bitmap -l_3_R = null;
        long -l_4_J = System.currentTimeMillis();
        if (IsJpegFileC(str)) {
            -l_6_R = Y(str);
            if (-l_6_R == null) {
                -l_6_R = a(str, 100, 100);
            }
        } else {
            -l_6_R = decodeSampledBitmapFromFile(str, 100, 100);
        }
        if (-l_6_R != null) {
            if (-l_6_R.getWidth() == 100 && -l_6_R.getHeight() == 100) {
                -l_3_R = -l_6_R;
            } else {
                -l_3_R = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
                Object -l_7_R = new Canvas(-l_3_R);
                Object -l_8_R = new Rect();
                -l_8_R.set(0, 0, -l_6_R.getWidth(), -l_6_R.getHeight());
                Object -l_9_R = new Rect();
                -l_9_R.set(0, 0, 100, 100);
                -l_7_R.drawBitmap(-l_6_R, -l_8_R, -l_9_R, null);
                -l_6_R.recycle();
            }
        }
        String str2 = "QQImageCompare";
        f.f(str2, "loadBitmap100x100FromFile t = " + (System.currentTimeMillis() - -l_4_J));
        return -l_3_R;
    }

    public static Bitmap loadBitmapFromFile(String str, int i, int i2) {
        if (!IsJpegFileC(str)) {
            return decodeSampledBitmapFromFile(str, i, i2);
        }
        Object -l_3_R;
        Object -l_5_R = null;
        try {
            -l_5_R = new ExifInterface(str);
        } catch (Object -l_6_R) {
            -l_6_R.printStackTrace();
        }
        if (-l_5_R == null) {
            -l_3_R = decodeSampledBitmapFromFile(str, i, i2);
        } else {
            byte[] -l_6_R2 = -l_5_R.getThumbnail();
            Object -l_4_R;
            if (-l_6_R2 == null) {
                -l_4_R = X(str);
                -l_3_R = (i <= (-l_4_R.width >> 3) && i2 <= (-l_4_R.height >> 3)) ? a(str, i, i2) : decodeSampledBitmapFromFile(str, i, i2);
            } else {
                -l_4_R = e(-l_6_R2);
                -l_3_R = -l_4_R == null ? decodeSampledBitmapFromFile(str, i, i2) : (i <= -l_4_R.width && i2 <= -l_4_R.height) ? a(-l_6_R2, i, i2) : (i <= (-l_4_R.width >> 3) && i2 <= (-l_4_R.height >> 3)) ? a(str, i, i2) : decodeSampledBitmapFromFile(str, i, i2);
            }
        }
        return -l_3_R == null ? decodeSampledBitmapFromFile(str, i, i2) : -l_3_R;
    }

    public static Bitmap loadBitmapSubImage(String str, int i, int i2, int i3, int i4, QQImageLoaderHeadInfo qQImageLoaderHeadInfo) {
        Object -l_6_R;
        if (qQImageLoaderHeadInfo.bJpeg) {
            -l_6_R = Bitmap.createBitmap(i3, i4, Config.ARGB_8888);
            if (DecodeJpegFileSubImageC(str, i, i2, i3, i4, qQImageLoaderHeadInfo.mMCUWidth, qQImageLoaderHeadInfo.mMCUHeight, -l_6_R) == 0) {
                return -l_6_R;
            }
            -l_6_R.recycle();
            return null;
        }
        BitmapRegionDecoder -l_7_R = null;
        try {
            -l_7_R = BitmapRegionDecoder.newInstance(str, true);
        } catch (Object -l_8_R) {
            Object -l_8_R2;
            -l_8_R2.printStackTrace();
        }
        if (-l_7_R == null) {
            return null;
        }
        -l_8_R2 = new Rect();
        -l_8_R2.set(i, i2, i + i3, i2 + i4);
        -l_6_R = -l_7_R.decodeRegion(-l_8_R2, null);
        -l_7_R.recycle();
        return -l_6_R;
    }
}
