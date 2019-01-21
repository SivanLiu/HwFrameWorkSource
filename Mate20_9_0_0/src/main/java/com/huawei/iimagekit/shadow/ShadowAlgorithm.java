package com.huawei.iimagekit.shadow;

import android.content.Context;
import android.graphics.Bitmap;
import com.huawei.iimagekit.common.HwAlgorithmBase;

public class ShadowAlgorithm extends HwAlgorithmBase {
    private int mShadowMethod;

    public ShadowAlgorithm(Context context, int shadowMethod) {
        this.mShadowMethod = shadowMethod;
    }

    public int checkBlurInputParameter(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        if (!IMAGEKIT_SHADOW_PROP) {
            return 6;
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

    public int doShadow(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        int err_result = checkBlurInputParameter(bitmapForBlur, blurredBitmap, radius);
        if (err_result == 0) {
            switch (this.mShadowMethod) {
                case 6:
                    ShadowBoxBlur.doBlur(bitmapForBlur, blurredBitmap, radius);
                    break;
                case 7:
                    ShadowStackBlur.doBlur(bitmapForBlur, blurredBitmap, radius);
                    break;
                default:
                    NoneShadow.doBlur(bitmapForBlur, blurredBitmap, radius);
                    break;
            }
        }
        return err_result;
    }
}
