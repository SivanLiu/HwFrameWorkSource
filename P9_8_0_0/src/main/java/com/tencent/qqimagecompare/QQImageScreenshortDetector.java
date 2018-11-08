package com.tencent.qqimagecompare;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import tmsdk.common.utils.f;

public class QQImageScreenshortDetector {
    private int nL;
    private int nM;
    private Options nN;

    public QQImageScreenshortDetector(Context context) {
        int i;
        Object -l_3_R = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Object -l_4_R;
        if (VERSION.SDK_INT >= 17) {
            -l_4_R = new DisplayMetrics();
            -l_3_R.getRealMetrics(-l_4_R);
            this.nL = -l_4_R.widthPixels;
            i = -l_4_R.heightPixels;
        } else if (VERSION.SDK_INT < 14) {
            this.nL = -l_3_R.getWidth();
            i = -l_3_R.getHeight();
        } else {
            try {
                -l_4_R = Display.class.getMethod("getRawHeight", new Class[0]);
                this.nL = ((Integer) Display.class.getMethod("getRawWidth", new Class[0]).invoke(-l_3_R, new Object[0])).intValue();
                this.nM = ((Integer) -l_4_R.invoke(-l_3_R, new Object[0])).intValue();
            } catch (Exception e) {
                this.nL = -l_3_R.getWidth();
                this.nM = -l_3_R.getHeight();
                f.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }
            this.nN = new Options();
            this.nN.inJustDecodeBounds = true;
        }
        this.nM = i;
        this.nN = new Options();
        this.nN.inJustDecodeBounds = true;
    }

    public boolean isScreenshort(String str) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(str);
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        }
        if (exifInterface == null || exifInterface.getAttributeInt("Orientation", 0) != 0 || exifInterface.getAttributeInt("ImageWidth", 0) != 0) {
            return false;
        }
        BitmapFactory.decodeFile(str, this.nN);
        return this.nN.outWidth == this.nL && this.nN.outHeight == this.nM;
    }
}
