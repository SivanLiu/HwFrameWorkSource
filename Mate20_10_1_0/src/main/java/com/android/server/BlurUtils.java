package com.android.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.lang.reflect.Array;

public class BlurUtils {
    public static Bitmap stackBlur(Bitmap input, int radius) {
        int i;
        int pixel = radius;
        Bitmap result = input.copy(Bitmap.Config.ARGB_8888, true);
        if (pixel < 1) {
            return null;
        }
        int w = result.getWidth();
        int pixel2 = result.getHeight();
        int[] pixels = new int[(w * pixel2)];
        result.getPixels(pixels, 0, w, 0, 0, w, pixel2);
        int wm = w - 1;
        int hm = pixel2 - 1;
        int wh = w * pixel2;
        int div = pixel + pixel + 1;
        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int[] vmin = new int[Math.max(w, pixel2)];
        int divsum = (div + 1) >> 1;
        int divsum2 = divsum * divsum;
        int[] dv = new int[(divsum2 * 256)];
        int i2 = 0;
        while (i2 < divsum2 * 256) {
            dv[i2] = i2 / divsum2;
            i2++;
            wh = wh;
        }
        int yi = 0;
        int yw = 0;
        int[][] stack = (int[][]) Array.newInstance(int.class, div, 3);
        int r1 = pixel + 1;
        int y = 0;
        while (y < pixel2) {
            int h = 0;
            int gsum = 0;
            int rsum = 0;
            int boutsum = 0;
            int goutsum = 0;
            int routsum = 0;
            int binsum = 0;
            int ginsum = 0;
            int rinsum = 0;
            int i3 = -pixel;
            int bsum = 0;
            while (i3 <= pixel) {
                int pixel3 = pixels[yi + Math.min(wm, Math.max(i3, h))];
                int[] sir = stack[i3 + pixel];
                sir[h] = (pixel3 & MemoryConstant.LARGE_CPU_MASK) >> 16;
                sir[1] = (pixel3 & 65280) >> 8;
                sir[2] = pixel3 & 255;
                int rbs = r1 - Math.abs(i3);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i3 > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                i3++;
                pixel2 = pixel2;
                result = result;
                h = 0;
            }
            int stackpointer = radius;
            int x = 0;
            while (x < w) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];
                int rsum2 = rsum - routsum;
                int gsum2 = gsum - goutsum;
                int bsum2 = bsum - boutsum;
                int[] sir2 = stack[((stackpointer - pixel) + div) % div];
                int routsum2 = routsum - sir2[0];
                int goutsum2 = goutsum - sir2[1];
                int boutsum2 = boutsum - sir2[2];
                if (y == 0) {
                    i = i3;
                    vmin[x] = Math.min(x + pixel + 1, wm);
                } else {
                    i = i3;
                }
                int pixel4 = pixels[yw + vmin[x]];
                sir2[0] = (pixel4 & MemoryConstant.LARGE_CPU_MASK) >> 16;
                sir2[1] = (pixel4 & 65280) >> 8;
                sir2[2] = pixel4 & 255;
                int rinsum2 = rinsum + sir2[0];
                int ginsum2 = ginsum + sir2[1];
                int binsum2 = binsum + sir2[2];
                rsum = rsum2 + rinsum2;
                gsum = gsum2 + ginsum2;
                bsum = bsum2 + binsum2;
                stackpointer = (stackpointer + 1) % div;
                int[] sir3 = stack[stackpointer % div];
                routsum = routsum2 + sir3[0];
                goutsum = goutsum2 + sir3[1];
                boutsum = boutsum2 + sir3[2];
                rinsum = rinsum2 - sir3[0];
                ginsum = ginsum2 - sir3[1];
                binsum = binsum2 - sir3[2];
                yi++;
                x++;
                wm = wm;
                i3 = i;
            }
            yw += w;
            y++;
            pixel2 = pixel2;
            divsum2 = divsum2;
            result = result;
        }
        int stackstart = pixel2;
        int x2 = 0;
        int rbs2 = y;
        while (x2 < w) {
            int goutsum3 = 0;
            int routsum3 = 0;
            int ginsum3 = 0;
            int rinsum3 = 0;
            int i4 = -pixel;
            int gsum3 = 0;
            int rsum3 = 0;
            int bsum3 = 0;
            int yp = (-pixel) * w;
            int binsum3 = 0;
            int boutsum3 = 0;
            while (i4 <= pixel) {
                int yi2 = Math.max(0, yp) + x2;
                int[] sir4 = stack[i4 + pixel];
                sir4[0] = r[yi2];
                sir4[1] = g[yi2];
                sir4[2] = b[yi2];
                int rbs3 = r1 - Math.abs(i4);
                rsum3 += r[yi2] * rbs3;
                gsum3 += g[yi2] * rbs3;
                bsum3 += b[yi2] * rbs3;
                if (i4 > 0) {
                    rinsum3 += sir4[0];
                    ginsum3 += sir4[1];
                    binsum3 += sir4[2];
                } else {
                    routsum3 += sir4[0];
                    goutsum3 += sir4[1];
                    boutsum3 += sir4[2];
                }
                if (i4 < hm) {
                    yp += w;
                }
                i4++;
                rbs2 = rbs2;
            }
            int stackpointer2 = radius;
            int yi3 = x2;
            rbs2 = 0;
            while (rbs2 < stackstart) {
                pixels[yi3] = (pixels[yi3] & -16777216) | (dv[rsum3] << 16) | (dv[gsum3] << 8) | dv[bsum3];
                int rsum4 = rsum3 - routsum3;
                int gsum4 = gsum3 - goutsum3;
                int bsum4 = bsum3 - boutsum3;
                int[] sir5 = stack[((stackpointer2 - pixel) + div) % div];
                int routsum4 = routsum3 - sir5[0];
                int goutsum4 = goutsum3 - sir5[1];
                int boutsum4 = boutsum3 - sir5[2];
                if (x2 == 0) {
                    vmin[rbs2] = Math.min(rbs2 + r1, hm) * w;
                }
                int pixel5 = vmin[rbs2] + x2;
                sir5[0] = r[pixel5];
                sir5[1] = g[pixel5];
                sir5[2] = b[pixel5];
                int rinsum4 = rinsum3 + sir5[0];
                int ginsum4 = ginsum3 + sir5[1];
                int binsum4 = binsum3 + sir5[2];
                rsum3 = rsum4 + rinsum4;
                gsum3 = gsum4 + ginsum4;
                bsum3 = bsum4 + binsum4;
                stackpointer2 = (stackpointer2 + 1) % div;
                int[] sir6 = stack[stackpointer2];
                routsum3 = routsum4 + sir6[0];
                goutsum3 = goutsum4 + sir6[1];
                boutsum3 = boutsum4 + sir6[2];
                rinsum3 = rinsum4 - sir6[0];
                ginsum3 = ginsum4 - sir6[1];
                binsum3 = binsum4 - sir6[2];
                yi3 += w;
                rbs2++;
                pixel = radius;
                stackstart = stackstart;
                yp = yp;
            }
            x2++;
            pixel = radius;
            stackstart = stackstart;
        }
        result.setPixels(pixels, 0, w, 0, 0, w, stackstart);
        return result;
    }

    public static Bitmap blurImage(Context context, Bitmap input, float radius) {
        Bitmap tempInput = Bitmap.createScaledBitmap(input, input.getWidth() / 4, input.getHeight() / 4, false);
        Bitmap result = tempInput.copy(tempInput.getConfig(), true);
        RenderScript rsScript = RenderScript.create(context);
        if (rsScript == null) {
            return null;
        }
        Allocation alloc = Allocation.createFromBitmap(rsScript, tempInput, Allocation.MipmapControl.MIPMAP_NONE, 1);
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
        Bitmap newBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        canvas.drawBitmap(bmp, 0.0f, 0.0f, paint);
        canvas.drawColor(color);
        return newBitmap;
    }
}
