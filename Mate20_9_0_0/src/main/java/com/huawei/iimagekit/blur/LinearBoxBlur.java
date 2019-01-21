package com.huawei.iimagekit.blur;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.ArrayList;

public class LinearBoxBlur {
    public static void doBlur(Bitmap bitmapForBlur, Bitmap blurredBitmap, int r) {
        Bitmap image = bitmapForBlur;
        int w = image.getWidth();
        int h = image.getHeight();
        if (r >= w || r >= h) {
            r = w + -1 < h + -1 ? w - 1 : h - 1;
        }
        int[] pixels = new int[(w * h)];
        int[] changedPixels = new int[pixels.length];
        int i = w;
        int i2 = w;
        int i3 = h;
        image.getPixels(pixels, 0, i, 0, 0, i2, i3);
        FastGaussianBlur(pixels, changedPixels, w, h, r);
        blurredBitmap.setPixels(changedPixels, 0, i, 0, 0, i2, i3);
    }

    private static void FastGaussianBlur(int[] source, int[] output, int w, int h, int r) {
        ArrayList<Integer> gaussianBoxes = CreateGausianBoxes1(r, 2);
        BoxBlur(source, output, w, h, (((Integer) gaussianBoxes.get(0)).intValue() - 1) / 2);
        BoxBlur(output, source, w, h, (((Integer) gaussianBoxes.get(1)).intValue() - 1) / 2);
        System.arraycopy(source, 0, output, 0, source.length);
    }

    private static ArrayList<Integer> CreateGausianBoxes1(int filterw, int n) {
        double sigma = Math.sqrt(((double) ((filterw ^ 1) * n)) / 12.0d);
        if (filterw % 2 == 0) {
            filterw--;
        }
        int filterwU = filterw + 2;
        double m = (double) Math.round((((((12.0d * sigma) * sigma) - ((double) ((n * filterw) * filterw))) - ((double) ((4 * n) * filterw))) - ((double) (3 * n))) / ((double) ((-4 * filterw) - 4)));
        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < n; i++) {
            result.add(Integer.valueOf(((double) i) < m ? filterw : filterwU));
        }
        return result;
    }

    private static void BoxBlur(int[] source, int[] output, int w, int h, int r) {
        System.arraycopy(source, 0, output, 0, source.length);
        BoxBlurHorizontal(output, source, w, h, r);
        BoxBlurVertical(source, output, w, h, r);
    }

    private static void BoxBlurHorizontal(int[] scl, int[] tcl, int w, int h, int r) {
        int i = w;
        int i2 = r;
        float iarr = 1.0f / ((float) ((i2 + i2) + 1));
        int i3 = 0;
        while (i3 < h) {
            int fv_r;
            int fv_g;
            int ti = i3 * i;
            int li = ti;
            int ri = ti + i2;
            int fv = scl[ti];
            int lv = scl[(ti + i) - 1];
            int fv_r2 = (fv >>> 16) & 255;
            int fv_g2 = (fv >>> 8) & 255;
            int fv_b = fv & 255;
            int lv_r = (lv >>> 16) & 255;
            int li2 = li;
            li = (lv >>> 8) & 255;
            int ri2 = ri;
            ri = lv & 255;
            int bval = (i2 + 1) * fv_b;
            int gval = (i2 + 1) * fv_g2;
            int rval = (i2 + 1) * fv_r2;
            int j = 0;
            while (true) {
                int fv2 = fv;
                fv = j;
                if (fv >= i2) {
                    break;
                }
                rval += Color.red(scl[ti + fv]);
                gval += Color.green(scl[ti + fv]);
                bval += Color.blue(scl[ti + fv]);
                j = fv + 1;
                fv = fv2;
                lv = lv;
            }
            fv = ti;
            ti = 0;
            while (ti <= i2) {
                lv = rval + (Color.red(scl[ri2]) - fv_r2);
                fv_r = fv_r2;
                fv_r2 = gval + (Color.green(scl[ri2]) - fv_g2);
                fv_g = fv_g2;
                fv_g2 = bval + (Color.blue(scl[ri2]) - fv_b);
                j = fv + 1;
                int fv_b2 = fv_b;
                int rval2 = lv;
                int gval2 = fv_r2;
                tcl[fv] = Color.argb(255, Math.round(((float) lv) * iarr), Math.round(((float) fv_r2) * iarr), Math.round(((float) fv_g2) * iarr));
                ri2++;
                ti++;
                bval = fv_g2;
                fv = j;
                fv_r2 = fv_r;
                fv_g2 = fv_g;
                fv_b = fv_b2;
                rval = rval2;
                gval = gval2;
            }
            fv_r = fv_r2;
            fv_g = fv_g2;
            fv_b = i2 + 1;
            while (fv_b < i - i2) {
                ti = rval + (Color.red(scl[ri2]) - Color.red(scl[li2]));
                lv = gval + (Color.green(scl[ri2]) - Color.green(scl[li2]));
                fv_r2 = bval + (Color.blue(scl[ri2]) - Color.blue(scl[li2]));
                int ti2 = fv + 1;
                int rval3 = ti;
                int gval3 = lv;
                tcl[fv] = Color.argb(255, Math.round(((float) ti) * iarr), Math.round(((float) lv) * iarr), Math.round(((float) fv_r2) * iarr));
                ri2++;
                li2++;
                fv_b++;
                bval = fv_r2;
                fv = ti2;
                rval = rval3;
                gval = gval3;
            }
            fv_b = i - i2;
            while (fv_b < i) {
                ti = rval + (lv_r - Color.red(scl[li2]));
                lv = gval + (li - Color.green(scl[li2]));
                fv_r2 = bval + (ri - Color.blue(scl[li2]));
                fv_g2 = fv + 1;
                tcl[fv] = Color.argb(255, Math.round(((float) ti) * iarr), Math.round(((float) lv) * iarr), Math.round(((float) fv_r2) * iarr));
                li2++;
                fv_b++;
                rval = ti;
                gval = lv;
                bval = fv_r2;
                fv = fv_g2;
                i = w;
                i2 = r;
            }
            i3++;
            i = w;
            i2 = r;
        }
    }

    private static void BoxBlurVertical(int[] scl, int[] tcl, int w, int h, int r) {
        int i = w;
        int i2 = h;
        int i3 = r;
        float iarr = 1.0f / ((float) ((i3 + i3) + 1));
        int i4 = 0;
        while (i4 < i) {
            int ti;
            int fv_r;
            int fv_g;
            int ti2 = i4;
            int li = ti2;
            int ri = (i3 * i) + ti2;
            int fv = scl[ti2];
            int lv = scl[((i2 - 1) * i) + ti2];
            int fv_r2 = (fv >>> 16) & 255;
            int fv_g2 = (fv >>> 8) & 255;
            int fv_b = fv & 255;
            int li2 = li;
            li = (lv >>> 16) & 255;
            int ri2 = ri;
            ri = (lv >>> 8) & 255;
            fv = lv & 255;
            int bval = (i3 + 1) * fv_b;
            int gval = (i3 + 1) * fv_g2;
            int rval = (i3 + 1) * fv_r2;
            int j = 0;
            while (true) {
                int lv2 = lv;
                lv = j;
                if (lv >= i3) {
                    break;
                }
                ti = ti2;
                ti2 = scl[ti2 + (lv * i)];
                rval += Color.red(ti2);
                gval += Color.green(ti2);
                bval += Color.blue(ti2);
                j = lv + 1;
                lv = lv2;
                ti2 = ti;
            }
            ti = ti2;
            ti2 = 0;
            while (ti2 <= i3) {
                lv = rval + (Color.red(scl[ri2]) - fv_r2);
                fv_r = fv_r2;
                fv_r2 = gval + (Color.green(scl[ri2]) - fv_g2);
                fv_g = fv_g2;
                fv_g2 = bval + (Color.blue(scl[ri2]) - fv_b);
                int fv_b2 = fv_b;
                int rval2 = lv;
                int gval2 = fv_r2;
                tcl[ti] = Color.argb(255, Math.round(((float) lv) * iarr), Math.round(((float) fv_r2) * iarr), Math.round(((float) fv_g2) * iarr));
                ri2 += i;
                ti += i;
                ti2++;
                bval = fv_g2;
                fv_r2 = fv_r;
                fv_g2 = fv_g;
                fv_b = fv_b2;
                rval = rval2;
                gval = gval2;
            }
            fv_r = fv_r2;
            fv_g = fv_g2;
            fv_b = i3 + 1;
            while (fv_b < i2 - i3) {
                ti2 = rval + (Color.red(scl[ri2]) - Color.red(scl[li2]));
                lv = gval + (Color.green(scl[ri2]) - Color.green(scl[li2]));
                fv_r2 = bval + (Color.blue(scl[ri2]) - Color.blue(scl[li2]));
                int rval3 = ti2;
                int gval3 = lv;
                tcl[ti] = Color.argb(255, Math.round(((float) ti2) * iarr), Math.round(((float) lv) * iarr), Math.round(((float) fv_r2) * iarr));
                li2 += i;
                ri2 += i;
                ti += i;
                fv_b++;
                bval = fv_r2;
                rval = rval3;
                gval = gval3;
            }
            fv_b = i2 - i3;
            while (fv_b < i2) {
                ti2 = rval + (li - Color.red(scl[li2]));
                lv = gval + (ri - Color.green(scl[li2]));
                fv_r2 = bval + (fv - Color.blue(scl[li2]));
                tcl[ti] = Color.argb(255, Math.round(((float) ti2) * iarr), Math.round(((float) lv) * iarr), Math.round(((float) fv_r2) * iarr));
                li2 += i;
                ti += i;
                fv_b++;
                rval = ti2;
                gval = lv;
                bval = fv_r2;
                i2 = h;
            }
            i4++;
            i2 = h;
        }
    }
}
