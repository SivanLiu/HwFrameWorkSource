package com.huawei.iimagekit.blur;

import android.content.Context;
import android.graphics.Bitmap;
import com.huawei.iimagekit.common.HwAlgorithmBase;
import com.huawei.iimagekit.shadow.ShadowBoxBlur;

public class BlurAlgorithm extends HwAlgorithmBase {
    private int mBlurMethod;

    public BlurAlgorithm(Context context, int blurMethod, boolean use_4channel) {
        if (use_4channel) {
            this.mBlurMethod = 6;
        } else {
            this.mBlurMethod = blurMethod;
        }
        if (this.mBlurMethod != 3 && this.mBlurMethod != 4 && this.mBlurMethod != 6) {
            System.loadLibrary("iimagekit_jni");
        }
    }

    public BlurAlgorithm(Context context, int blurMethod) {
        this(context, blurMethod, false);
    }

    protected int checkBlurInputParameter(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        if (!IMAGEKIT_BLUR_PROP) {
            return 5;
        }
        if (bitmapForBlur == null) {
            return 1;
        }
        if (blurredBitmap == null) {
            return 2;
        }
        if (bitmapForBlur.getHeight() != blurredBitmap.getHeight() || bitmapForBlur.getWidth() != blurredBitmap.getWidth()) {
            return 3;
        }
        if (radius <= 2 || radius > 25) {
            return 4;
        }
        if (bitmapForBlur.getHeight() < 3 || bitmapForBlur.getWidth() < 3) {
            return 7;
        }
        return 0;
    }

    public int blur(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        int err_result = checkBlurInputParameter(bitmapForBlur, blurredBitmap, radius);
        if (err_result == 0) {
            int i = this.mBlurMethod;
            if (i == 6) {
                ShadowBoxBlur.doBlur(bitmapForBlur, blurredBitmap, radius);
            } else if (i != 11) {
                switch (i) {
                    case 3:
                        FastBlurMT.doBlur(bitmapForBlur, blurredBitmap, (float) radius);
                        break;
                    case 4:
                        LinearBoxBlur.doBlur(bitmapForBlur, blurredBitmap, radius);
                        break;
                    default:
                        CPUBoxBlur.getInstance().doBlur(bitmapForBlur, blurredBitmap, radius);
                        break;
                }
            } else {
                CPUFastBlur.getInstance().doBlur(bitmapForBlur, blurredBitmap, radius);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ErrorCode:");
            stringBuilder.append(err_result);
            localLog(4, stringBuilder.toString());
        }
        return err_result;
    }
}
