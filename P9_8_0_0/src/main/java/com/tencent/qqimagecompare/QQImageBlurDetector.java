package com.tencent.qqimagecompare;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import com.tencent.qqimagecompare.QQImageBitmap.eColorFormat;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;

public class QQImageBlurDetector extends QQImageNativeObject {
    private native int Detect1j1bmpC(long j, Bitmap bitmap);

    private native int Detect1j1jC(long j, long j2);

    public static int aliginInt(int -l_2_I, int i) {
        int -l_3_I = -l_2_I % i;
        return -l_3_I == 0 ? -l_2_I : -l_2_I + (i - -l_3_I);
    }

    public static int pad_n(int i, int i2) {
        return ((i + i2) - 1) & ((i2 - 1) ^ -1);
    }

    protected native long createNativeObject();

    protected native void destroyNativeObject(long j);

    public int detect(Bitmap bitmap) {
        return Detect1j1bmpC(this.mThisC, bitmap);
    }

    public int detect(QQImageBitmap qQImageBitmap) {
        return Detect1j1jC(this.mThisC, qQImageBitmap.mThisC);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int detect(String str) {
        f.f("QQImageBlurDetector", str);
        int -l_2_I = 0;
        QQImageLoaderHeadInfo -l_3_R = QQImageLoader.GetImageHeadInfo(str);
        int -l_4_I = -l_3_R.width;
        int -l_5_I = -l_3_R.height;
        int -l_6_I = Math.max(-l_4_I, -l_5_I);
        int -l_7_I = Math.min(-l_4_I, -l_5_I);
        if (-l_7_I >= 200) {
            int -l_8_I;
            if (-l_6_I < 1800) {
                -l_8_I = 0;
            } else if (-l_6_I >= 1800 && -l_6_I < 3600) {
                -l_8_I = 1;
                -l_7_I >>= 1;
            } else if (-l_6_I >= 3600 && -l_6_I < 7200) {
                -l_8_I = 2;
                -l_7_I >>= 2;
            } else {
                -l_8_I = 3;
                -l_7_I >>= 3;
            }
            if (-l_7_I >= 200) {
                Object -l_9_R = new int[3];
                int -l_11_I;
                int -l_19_I;
                int -l_20_I;
                int -l_22_I;
                if (-l_3_R.bJpeg) {
                    Object -l_10_R = new QQImageBitmap();
                    if (QQImageLoader.DecodeJpegFileScale(str, -l_8_I, -l_10_R) == 0) {
                        -l_11_I = -l_10_R.getWidth();
                        int -l_17_I = (-l_10_R.getHeight() >> 1) - 100;
                        int -l_18_I = (-l_11_I / 3) - 100;
                        -l_19_I = (-l_11_I >> 1) - 100;
                        -l_20_I = ((-l_11_I << 1) / 3) - 100;
                        QQImageBitmap qQImageBitmap = new QQImageBitmap(eColorFormat.QQIMAGE_CLR_RGBA8888, SmsCheckResult.ESCT_200, SmsCheckResult.ESCT_200, 4);
                        if (qQImageBitmap != null) {
                            -l_10_R.clip(qQImageBitmap, -l_18_I, -l_17_I);
                            -l_9_R[0] = detect(qQImageBitmap);
                            -l_10_R.clip(qQImageBitmap, -l_19_I, -l_17_I);
                            -l_9_R[1] = detect(qQImageBitmap);
                            if (-l_9_R[0] != -l_9_R[1]) {
                                -l_10_R.clip(qQImageBitmap, -l_20_I, -l_17_I);
                                -l_9_R[2] = detect(qQImageBitmap);
                                -l_22_I = 0;
                                for (int -l_23_I = 0; -l_23_I < 3; -l_23_I++) {
                                    if (-l_9_R[-l_23_I] < null) {
                                        -l_22_I++;
                                    }
                                }
                            }
                            -l_2_I = 1;
                            qQImageBitmap.delete();
                        }
                        -l_10_R.delete();
                    }
                } else {
                    int -l_10_I = -l_4_I;
                    -l_11_I = -l_5_I;
                    switch (-l_8_I) {
                        case 1:
                            -l_10_I = -l_4_I >> 1;
                            -l_11_I = -l_5_I >> 1;
                            break;
                        case 2:
                            -l_10_I = -l_4_I >> 2;
                            -l_11_I = -l_5_I >> 2;
                            break;
                        case 3:
                            -l_10_I = -l_4_I >> 3;
                            -l_11_I = -l_5_I >> 3;
                            break;
                    }
                    Object -l_12_R = QQImageLoader.decodeSampledBitmapFromFile(str, -l_10_I, -l_11_I);
                    if (-l_12_R != null) {
                        int -l_13_I = -l_12_R.getWidth();
                        -l_19_I = (-l_12_R.getHeight() >> 1) - 100;
                        -l_20_I = (-l_13_I / 3) - 100;
                        int -l_21_I = (-l_13_I >> 1) - 100;
                        -l_22_I = ((-l_13_I << 1) / 3) - 100;
                        Bitmap -l_23_R = Bitmap.createBitmap(SmsCheckResult.ESCT_200, SmsCheckResult.ESCT_200, Config.ARGB_8888);
                        if (-l_23_R != null) {
                            Canvas canvas = new Canvas(-l_23_R);
                            Rect -l_25_R = new Rect();
                            -l_25_R.set(-l_20_I, -l_19_I, -l_20_I + SmsCheckResult.ESCT_200, -l_19_I + SmsCheckResult.ESCT_200);
                            Rect -l_26_R = new Rect();
                            -l_26_R.set(0, 0, SmsCheckResult.ESCT_200, SmsCheckResult.ESCT_200);
                            canvas.drawBitmap(-l_12_R, -l_25_R, -l_26_R, null);
                            -l_9_R[0] = detect(-l_23_R);
                            -l_25_R.set(-l_21_I, -l_19_I, -l_21_I + SmsCheckResult.ESCT_200, -l_19_I + SmsCheckResult.ESCT_200);
                            canvas.drawBitmap(-l_12_R, -l_25_R, -l_26_R, null);
                            -l_9_R[1] = detect(-l_23_R);
                            if (-l_9_R[0] != -l_9_R[1]) {
                                -l_25_R.set(-l_22_I, -l_19_I, -l_22_I + SmsCheckResult.ESCT_200, -l_19_I + SmsCheckResult.ESCT_200);
                                canvas.drawBitmap(-l_12_R, -l_25_R, -l_26_R, null);
                                -l_9_R[2] = detect(-l_23_R);
                                int -l_27_I = 0;
                                for (int -l_28_I = 0; -l_28_I < 3; -l_28_I++) {
                                    if (-l_9_R[-l_28_I] < null) {
                                        -l_27_I++;
                                    }
                                }
                            }
                            -l_2_I = 1;
                            -l_23_R.recycle();
                        }
                        -l_12_R.recycle();
                    }
                }
            }
        }
        f.f("QQImageBlurDetector", "nRet = " + -l_2_I);
        f.f("QQImageBlurDetector", "detect out");
        return -l_2_I;
    }
}
