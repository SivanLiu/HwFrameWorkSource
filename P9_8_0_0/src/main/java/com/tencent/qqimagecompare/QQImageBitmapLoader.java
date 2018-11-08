package com.tencent.qqimagecompare;

public class QQImageBitmapLoader {
    private static native long DecodeJpegFileSubImage1s6iC(String str, int i, int i2, int i3, int i4, int i5, int i6);

    public static QQImageBitmap loadBitmapSubImage(String str, int i, int i2, int i3, int i4, QQImageLoaderHeadInfo qQImageLoaderHeadInfo) {
        if (!qQImageLoaderHeadInfo.bJpeg) {
            return null;
        }
        Object -l_6_R = new QQImageBitmap(true);
        -l_6_R.mThisC = DecodeJpegFileSubImage1s6iC(str, i, i2, i3, i4, qQImageLoaderHeadInfo.mMCUWidth, qQImageLoaderHeadInfo.mMCUHeight);
        return -l_6_R;
    }
}
