package com.android.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import com.android.server.gesture.GestureNavConst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.lang.reflect.Array;

public class BlurUtils {
    public static Bitmap stackBlur(Bitmap input, int radius) {
        int pixel = radius;
        Bitmap result = input.copy(Config.ARGB_8888, true);
        if (pixel < 1) {
            return null;
        }
        int gsum;
        int rsum;
        int boutsum;
        int goutsum;
        int routsum;
        int binsum;
        int ginsum;
        int wh;
        int bsum;
        Bitmap result2;
        int h;
        int wm;
        int y;
        int w = result.getWidth();
        int h2 = result.getHeight();
        int[] pixels = new int[(w * h2)];
        result.getPixels(pixels, 0, w, 0, 0, w, h2);
        int wm2 = w - 1;
        int hm = h2 - 1;
        int wh2 = w * h2;
        int div = (pixel + pixel) + 1;
        int[] r = new int[wh2];
        int[] g = new int[wh2];
        int[] b = new int[wh2];
        int[] vmin = new int[Math.max(w, h2)];
        int divsum = (div + 1) >> 1;
        int divsum2 = divsum * divsum;
        int[] dv = new int[(256 * divsum2)];
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= 256 * divsum2) {
                break;
            }
            dv[i2] = i2 / divsum2;
            i = i2 + 1;
            i2 = input;
        }
        int yi = 0;
        i = 0;
        int[][] stack = (int[][]) Array.newInstance(int.class, new int[]{div, 3});
        int r1 = pixel + 1;
        divsum = 0;
        while (divsum < h2) {
            int i3;
            int[] sir = null;
            gsum = 0;
            rsum = 0;
            boutsum = 0;
            goutsum = 0;
            routsum = 0;
            binsum = 0;
            ginsum = 0;
            int rinsum = 0;
            wh = wh2;
            wh2 = -pixel;
            bsum = 0;
            while (wh2 <= pixel) {
                result2 = result;
                h = h2;
                result = sir;
                h2 = pixels[yi + Math.min(wm2, Math.max(wh2, result))];
                sir = stack[wh2 + pixel];
                sir[result] = (h2 & MemoryConstant.LARGE_CPU_MASK) >> 16;
                sir[1] = (h2 & 65280) >> 8;
                sir[2] = h2 & 255;
                result = r1 - Math.abs(wh2);
                rsum += sir[0] * result;
                gsum += sir[1] * result;
                bsum += sir[2] * result;
                if (wh2 > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                wh2++;
                result = result2;
                h2 = h;
                sir = null;
            }
            result2 = result;
            h = h2;
            h2 = pixel;
            result = null;
            while (result < w) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                int[] sir2 = stack[((h2 - pixel) + div) % div];
                routsum -= sir2[0];
                goutsum -= sir2[1];
                boutsum -= sir2[2];
                if (divsum == 0) {
                    i3 = wh2;
                    vmin[result] = Math.min((result + pixel) + 1, wm2);
                } else {
                    i3 = wh2;
                }
                wh2 = pixels[i + vmin[result]];
                sir2[0] = (wh2 & MemoryConstant.LARGE_CPU_MASK) >> 16;
                sir2[1] = (wh2 & 65280) >> 8;
                wm = wm2;
                sir2[2] = wh2 & 255;
                rinsum += sir2[0];
                ginsum += sir2[1];
                binsum += sir2[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                h2 = (h2 + 1) % div;
                int[] sir3 = stack[h2 % div];
                routsum += sir3[0];
                goutsum += sir3[1];
                boutsum += sir3[2];
                rinsum -= sir3[0];
                ginsum -= sir3[1];
                binsum -= sir3[2];
                yi++;
                result++;
                wh2 = i3;
                wm2 = wm;
            }
            i3 = wh2;
            wm = wm2;
            i += w;
            divsum++;
            wh2 = wh;
            result = result2;
            h2 = h;
        }
        result2 = result;
        wh = wh2;
        h = h2;
        wm = wm2;
        h2 = divsum;
        int x = 0;
        while (x < w) {
            gsum = 0;
            rsum = 0;
            boutsum = 0;
            goutsum = 0;
            routsum = 0;
            divsum = -pixel;
            bsum = 0;
            wm2 = 0;
            binsum = 0;
            int bsum2 = 0;
            wh2 = (-pixel) * w;
            while (divsum <= pixel) {
                y = h2;
                yi = Math.max(0, wh2) + x;
                int[] sir4 = stack[divsum + pixel];
                sir4[0] = r[yi];
                sir4[1] = g[yi];
                sir4[2] = b[yi];
                h2 = r1 - Math.abs(divsum);
                wm2 += r[yi] * h2;
                bsum += g[yi] * h2;
                bsum2 += b[yi] * h2;
                if (divsum > 0) {
                    routsum += sir4[0];
                    goutsum += sir4[1];
                    boutsum += sir4[2];
                } else {
                    rsum += sir4[0];
                    gsum += sir4[1];
                    binsum += sir4[2];
                }
                if (divsum < hm) {
                    wh2 += w;
                }
                divsum++;
                h2 = y;
            }
            yi = x;
            h2 = 0;
            ginsum = boutsum;
            boutsum = pixel;
            while (true) {
                int yp = wh2;
                wh2 = h;
                if (h2 >= wh2) {
                    break;
                }
                pixels[yi] = (((-16777216 & pixels[yi]) | (dv[wm2] << 16)) | (dv[bsum] << 8)) | dv[bsum2];
                wm2 -= rsum;
                bsum -= gsum;
                bsum2 -= binsum;
                int[] sir5 = stack[((boutsum - pixel) + div) % div];
                rsum -= sir5[0];
                gsum -= sir5[1];
                binsum -= sir5[2];
                if (x == 0) {
                    vmin[h2] = Math.min(h2 + r1, hm) * w;
                }
                pixel = vmin[h2] + x;
                sir5[0] = r[pixel];
                sir5[1] = g[pixel];
                sir5[2] = b[pixel];
                routsum += sir5[0];
                goutsum += sir5[1];
                ginsum += sir5[2];
                wm2 += routsum;
                bsum += goutsum;
                bsum2 += ginsum;
                boutsum = (boutsum + 1) % div;
                sir5 = stack[boutsum];
                rsum += sir5[0];
                gsum += sir5[1];
                binsum += sir5[2];
                routsum -= sir5[0];
                goutsum -= sir5[1];
                ginsum -= sir5[2];
                yi += w;
                h2++;
                h = wh2;
                wh2 = yp;
                pixel = radius;
            }
            x++;
            h = wh2;
            pixel = radius;
        }
        y = h2;
        result2.setPixels(pixels, 0, w, 0, 0, w, h);
        return result2;
    }

    public static Bitmap blurImage(Context context, Bitmap input, float radius) {
        Bitmap tempInput = Bitmap.createScaledBitmap(input, input.getWidth() / 4, input.getHeight() / 4, false);
        Bitmap result = tempInput.copy(tempInput.getConfig(), true);
        RenderScript rsScript = RenderScript.create(context);
        if (rsScript == null) {
            return null;
        }
        Allocation alloc = Allocation.createFromBitmap(rsScript, tempInput, MipmapControl.MIPMAP_NONE, 1);
        Allocation outAlloc = Allocation.createTyped(rsScript, alloc.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
        blur.setRadius(radius);
        blur.setInput(alloc);
        blur.forEach(outAlloc);
        outAlloc.copyTo(result);
        rsScript.destroy();
        return Bitmap.createScaledBitmap(result, input.getWidth(), input.getHeight(), false);
    }

    public static Bitmap addBlackBoard(Bitmap bmp, int color) {
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        Bitmap newBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        canvas.drawBitmap(bmp, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, paint);
        canvas.drawColor(color);
        return newBitmap;
    }
}
