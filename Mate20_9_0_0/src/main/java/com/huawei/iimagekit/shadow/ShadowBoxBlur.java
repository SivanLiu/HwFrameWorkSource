package com.huawei.iimagekit.shadow;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ShadowBoxBlur {
    public static void doBlur(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        int w = bitmapForBlur.getWidth();
        int h = bitmapForBlur.getHeight();
        int[] pixels = new int[(w * h)];
        bitmapForBlur.getPixels(pixels, 0, w, 0, 0, w, h);
        if ((radius & 1) == 0) {
            radius--;
        }
        ShadowUtil.processAlphaChannelBefore(pixels);
        for (int i = 0; i < 2; i++) {
            boxBlurHorizontal(pixels, w, h, radius / 2);
            boxBlurVertical(pixels, w, h, radius / 2);
        }
        ShadowUtil.processAlphaChannelAfter(pixels);
        blurredBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private static void boxBlurHorizontal(int[] pixels, int w, int h, int halfRange) {
        int[] iArr = pixels;
        int i = w;
        int newColors = halfRange;
        int[] newColors2 = new int[i];
        int index = 0;
        int y = 0;
        while (y < h) {
            int y2;
            int hits = 0;
            long a = 0;
            long r = 0;
            long g = 0;
            long b = 0;
            int x = -newColors;
            while (x < i) {
                int color;
                int[] newColors3;
                long a2;
                int oldPixel = (x - newColors) - 1;
                if (oldPixel >= 0) {
                    color = iArr[index + oldPixel];
                    if (color != 0) {
                        y2 = y;
                        newColors3 = newColors2;
                        a -= (long) Color.alpha(color);
                        r -= (long) Color.red(color);
                        g -= (long) Color.green(color);
                        b -= (long) Color.blue(color);
                    } else {
                        y2 = y;
                        newColors3 = newColors2;
                    }
                    hits--;
                } else {
                    y2 = y;
                    newColors3 = newColors2;
                }
                y = x + newColors;
                int newPixel;
                if (y < i) {
                    newColors2 = iArr[index + y];
                    if (newColors2 != null) {
                        newPixel = y;
                        a += (long) Color.alpha(newColors2);
                        r += (long) Color.red(newColors2);
                        g += (long) Color.green(newColors2);
                        b += (long) Color.blue(newColors2);
                    } else {
                        newPixel = y;
                    }
                    hits++;
                } else {
                    newPixel = y;
                }
                if (x >= 0) {
                    a2 = a;
                    newColors3[x] = Color.argb((int) (a / ((long) hits)), (int) (r / ((long) hits)), (int) (g / ((long) hits)), (int) (b / ((long) hits)));
                } else {
                    a2 = a;
                }
                x++;
                y = y2;
                newColors2 = newColors3;
                a = a2;
                newColors = halfRange;
                color = h;
            }
            y2 = y;
            int[] newColors4 = newColors2;
            System.arraycopy(newColors4, 0, iArr, index, i);
            index += i;
            y = y2 + 1;
            newColors2 = newColors4;
            newColors = halfRange;
        }
    }

    private static void boxBlurVertical(int[] pixels, int w, int h, int halfRange) {
        int newPixelOffset;
        int i = w;
        int i2 = h;
        int i3 = halfRange;
        int[] newColors = new int[i2];
        int oldPixelOffset = (-(i3 + 1)) * i;
        int newPixelOffset2 = i3 * i;
        int x = 0;
        while (x < i) {
            int oldPixelOffset2;
            int x2;
            int hits = 0;
            long a = 0;
            long r = 0;
            long g = 0;
            long b = 0;
            int index = ((-i3) * i) + x;
            int y = -i3;
            while (y < i2) {
                long a2;
                if ((y - i3) - 1 >= 0) {
                    oldPixelOffset2 = oldPixelOffset;
                    oldPixelOffset = pixels[index + oldPixelOffset];
                    if (oldPixelOffset != 0) {
                        x2 = x;
                        a -= (long) Color.alpha(oldPixelOffset);
                        r -= (long) Color.red(oldPixelOffset);
                        g -= (long) Color.green(oldPixelOffset);
                        b -= (long) Color.blue(oldPixelOffset);
                    } else {
                        x2 = x;
                    }
                    hits--;
                } else {
                    oldPixelOffset2 = oldPixelOffset;
                    x2 = x;
                }
                if (y + i3 < i2) {
                    i = pixels[index + newPixelOffset2];
                    if (i != 0) {
                        newPixelOffset = newPixelOffset2;
                        a += (long) Color.alpha(i);
                        r += (long) Color.red(i);
                        g += (long) Color.green(i);
                        b += (long) Color.blue(i);
                    } else {
                        newPixelOffset = newPixelOffset2;
                    }
                    hits++;
                } else {
                    newPixelOffset = newPixelOffset2;
                }
                if (y >= 0) {
                    a2 = a;
                    newColors[y] = Color.argb((int) (a / ((long) hits)), (int) (r / ((long) hits)), (int) (g / ((long) hits)), (int) (b / ((long) hits)));
                } else {
                    a2 = a;
                }
                i = w;
                index += i;
                y++;
                oldPixelOffset = oldPixelOffset2;
                x = x2;
                newPixelOffset2 = newPixelOffset;
                a = a2;
            }
            oldPixelOffset2 = oldPixelOffset;
            newPixelOffset = newPixelOffset2;
            x2 = x;
            for (int y2 = 0; y2 < i2; y2++) {
                pixels[(y2 * i) + x2] = newColors[y2];
            }
            x = x2 + 1;
            oldPixelOffset = oldPixelOffset2;
            newPixelOffset2 = newPixelOffset;
        }
        newPixelOffset = newPixelOffset2;
    }
}
