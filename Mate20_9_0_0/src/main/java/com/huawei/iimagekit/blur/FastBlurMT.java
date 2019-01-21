package com.huawei.iimagekit.blur;

import android.cover.CoverManager;
import android.graphics.Bitmap;
import android.util.IMonitorKeys;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastBlurMT {
    private static ExecutorService EXECUTOR = Executors.newFixedThreadPool(EXECUTOR_THREADS);
    private static int EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();
    private static final short[] stackblur_mul = new short[]{(short) 512, (short) 512, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, (short) 512, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, (short) 512, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, (short) 271, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 512, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, (short) 271, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 273, (short) 512, IMonitorKeys.E904200009_DSAPP5SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_PSDURNOREG_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_DSFAILNUM_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, (short) 284, (short) 271, (short) 259, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP2RTTL1_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DSAPP1RTT_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_LUSUCC_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_DSTOTALNUM_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_MTCSFBREDIRTOTDSCALLSUC_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID5_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 282, (short) 273, (short) 265, (short) 512, IMonitorKeys.E904200009_DSAPP5RTTL4_INT, IMonitorKeys.E904200009_DSAPP5SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP4TCPSUCCNUM_INT, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP2RTTL5_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1DELAYL6_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_ACCESSEDCELLCOUNT_SMALLINT, IMonitorKeys.E904200009_PSDURNOREG_INT, IMonitorKeys.E904200009_RAUFAIL_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSFAILNUM_INT, IMonitorKeys.E904200009_CALLDURATIONRE_60_180_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_MTSRVCCCALLSUCC_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB900TIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID0_INT, (short) 284, (short) 278, (short) 271, (short) 265, (short) 259, IMonitorKeys.E904200009_DSAPP6DELAYL1_INT, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP5TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP4NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP3NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP2RTTL1_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DSAPP1DELAYL1_INT, IMonitorKeys.E904200009_DSAPP1RTT_INT, IMonitorKeys.E904200009_IT310REGION1_SMALLINT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_PSDURREG3G_INT, IMonitorKeys.E904200009_LUSUCC_INT, IMonitorKeys.E904200009_GU3GPDPSUCC_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSTOTALNUM_INT, IMonitorKeys.E904200009_CALLDURATIONLO_180_I_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBANDOTHER_INT, IMonitorKeys.E904200009_MTCSFBREDIRTOTDSCALLSUC_INT, IMonitorKeys.E904200009_VOLTECSREDIALCALLSUCC_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB850TIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID5_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID0_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 287, (short) 282, (short) 278, (short) 273, (short) 269, (short) 265, (short) 261, (short) 512, IMonitorKeys.E904200009_DSAPP6TCPTOTALNUM_INT, IMonitorKeys.E904200009_DSAPP5RTTL4_INT, IMonitorKeys.E904200009_DSAPP5DELAYL2_INT, IMonitorKeys.E904200009_DSAPP5SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP4TCPSUCCNUM_INT, IMonitorKeys.E904200009_DSAPP4RTT_INT, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP3TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP2RTTL5_INT, IMonitorKeys.E904200009_DSAPP2DELAYL5_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1RTTL5_INT, IMonitorKeys.E904200009_DSAPP1DELAYL6_INT, IMonitorKeys.E904200009_DSAPP1TCPSUCCNUM_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_NORMALCALLBCELL_INT, IMonitorKeys.E904200009_ACCESSEDCELLCOUNT_SMALLINT, IMonitorKeys.E904200009_DNSDELAY_20_150_INT, IMonitorKeys.E904200009_PSDURNOREG_INT, IMonitorKeys.E904200009_CSDURREG4G_INT, IMonitorKeys.E904200009_RAUFAIL_INT, IMonitorKeys.E904200009_GU3GPDPFAIL_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_DSRTTNUML5_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSDELAYNUML2_INT, IMonitorKeys.E904200009_DSFAILNUM_INT, IMonitorKeys.E904200009_CALLDURATIONLO_180_I_INT, IMonitorKeys.E904200009_CALLDURATIONRE_60_180_INT, IMonitorKeys.E904200009_LTECSFBFRREDIRDUR_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_MTCSFBLTEACFAILCALLSUC_INT, IMonitorKeys.E904200009_MTSRVCCCALLSUCC_INT, IMonitorKeys.E904200009_CALLNUMWITHLAC_CLASS, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLCHFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB900TIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID4_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID3_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID0_INT, (short) 287, (short) 284, (short) 281, (short) 278, (short) 274, (short) 271, (short) 268, (short) 265, (short) 262, (short) 259, (short) 257, IMonitorKeys.E904200009_DSAPP6DELAYL1_INT, IMonitorKeys.E904200009_DSAPP6SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP5DELAYL4_INT, IMonitorKeys.E904200009_DSAPP5TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP5RTT_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP4DELAYL2_INT, IMonitorKeys.E904200009_DSAPP4NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP3RTTL5_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP3DELAYL2_INT, IMonitorKeys.E904200009_DSAPP3NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP3RTT_INT, IMonitorKeys.E904200009_DSAPP2RTTL1_INT, IMonitorKeys.E904200009_DSAPP2DELAYL3_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP2WEBDELAY_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DSAPP1DELAYL5_INT, IMonitorKeys.E904200009_DSAPP1DELAYL1_INT, IMonitorKeys.E904200009_DSAPP1NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP1RTT_INT, IMonitorKeys.E904200009_ABNORMALCALLBCELL_INT, IMonitorKeys.E904200009_IT310REGION1_SMALLINT, IMonitorKeys.E904200009_DNSDELAY1000_2000_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_DNSTOTALDELAY_INT, IMonitorKeys.E904200009_PSDURREG3G_INT, IMonitorKeys.E904200009_CSDURREG3G_INT, IMonitorKeys.E904200009_LUSUCC_INT, IMonitorKeys.E904200009_GUATTSUCC_INT, IMonitorKeys.E904200009_GU3GPDPSUCC_INT, IMonitorKeys.E904200009_EPSTAUFAIL_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_DSRTTNUML3_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSDELAYNUML2_INT, IMonitorKeys.E904200009_DSTOTALNUM_INT, IMonitorKeys.E904200009_DSSUCCNUM_INT, IMonitorKeys.E904200009_CALLDURATIONLO_180_I_INT, IMonitorKeys.E904200009_CALLDURATIONRE_180_I_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_LTECSFBFRREDIRDUR_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBANDOTHER_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND2_INT, IMonitorKeys.E904200009_MTCSFBREDIRTOTDSCALLSUC_INT, IMonitorKeys.E904200009_MTSRVCCCALLSUCC_INT, IMonitorKeys.E904200009_VOLTECSREDIALCALLSUCC_INT, IMonitorKeys.E904200009_DSDACALLNUM_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLCHHSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB850TIMELEN_INT, IMonitorKeys.E904200009_CHRRECORDNUM_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID5_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID2_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID0_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID3_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 289, (short) 287, (short) 285, (short) 282, (short) 280, (short) 278, (short) 275, (short) 273, (short) 271, (short) 269, (short) 267, (short) 265, (short) 263, (short) 261, (short) 259};
    private static final short[] stackblur_shr = new short[]{(short) 9, (short) 11, (short) 12, (short) 13, (short) 13, (short) 14, (short) 14, (short) 15, (short) 15, (short) 15, (short) 15, (short) 16, (short) 16, (short) 16, (short) 16, (short) 17, (short) 17, (short) 17, (short) 17, (short) 17, (short) 17, (short) 17, (short) 18, (short) 18, (short) 18, (short) 18, (short) 18, (short) 18, (short) 18, (short) 18, (short) 18, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 19, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 20, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 21, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 22, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 23, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24, (short) 24};

    private static class BlurTask implements Callable<Void> {
        private final int _coreIndex;
        private final int _h;
        private final int _radius;
        private final int _round;
        private final int[] _src;
        private final int _totalCores;
        private final int _w;

        BlurTask(int[] src, int w, int h, int radius, int totalCores, int coreIndex, int round) {
            this._src = src;
            this._w = w;
            this._h = h;
            this._radius = radius;
            this._totalCores = totalCores;
            this._coreIndex = coreIndex;
            this._round = round;
        }

        public Void call() throws Exception {
            FastBlurMT.blurIteration(this._src, this._w, this._h, this._radius, this._totalCores, this._coreIndex, this._round);
            return null;
        }
    }

    public static void doBlur(Bitmap bitmapForBlur, Bitmap blurredBitmap, float radius) {
        ArrayList<BlurTask> vertical;
        float f = radius;
        int w = bitmapForBlur.getWidth();
        int h = bitmapForBlur.getHeight();
        int[] currentPixels = new int[(w * h)];
        bitmapForBlur.getPixels(currentPixels, 0, w, 0, 0, w, h);
        int cores = EXECUTOR_THREADS;
        ArrayList<BlurTask> horizontal = new ArrayList(cores);
        ArrayList<BlurTask> vertical2 = new ArrayList(cores);
        int i = 0;
        while (i < cores) {
            int[] iArr = currentPixels;
            int i2 = w;
            int i3 = h;
            int i4 = cores;
            int i5 = i;
            ArrayList<BlurTask> vertical3 = vertical2;
            BlurTask blurTask = r2;
            BlurTask blurTask2 = new BlurTask(iArr, i2, i3, (int) f, i4, i5, 1);
            horizontal.add(blurTask);
            vertical = vertical3;
            vertical.add(new BlurTask(iArr, i2, i3, (int) f, i4, i5, 2));
            i++;
            vertical2 = vertical;
        }
        vertical = vertical2;
        try {
            EXECUTOR.invokeAll(horizontal);
        } catch (InterruptedException e) {
        }
        try {
            EXECUTOR.invokeAll(vertical);
        } catch (InterruptedException e2) {
        }
        vertical2 = vertical;
        blurredBitmap.setPixels(currentPixels, 0, w, 0, 0, w, h);
    }

    private static void blurIteration(int[] src, int w, int h, int radius, int cores, int core, int step) {
        int i = w;
        int i2 = h;
        int i3 = radius;
        int i4 = step;
        short mul_sum = stackblur_mul[i3];
        short shr_sum = stackblur_shr[i3];
        int div = (i3 * 2) + 1;
        int[] stack = new int[div];
        long j = 0;
        int wm;
        int maxY;
        int y;
        long sumRi;
        long sumR;
        long sumGi;
        long sumG;
        long sumBi;
        long sumB;
        int dst_i;
        long sumGo;
        long sumRo;
        int i5;
        int rgb;
        int g;
        int b;
        short shr_sum2;
        int r;
        short mul_sum2;
        short div2;
        if (i4 == 1) {
            wm = i - 1;
            maxY = ((core + 1) * i2) / cores;
            int y2 = (core * i2) / cores;
            while (true) {
                y = y2;
                if (y < maxY) {
                    int maxY2;
                    sumRi = j;
                    sumR = j;
                    sumGi = j;
                    sumG = j;
                    sumBi = j;
                    sumB = j;
                    y2 = i * y;
                    dst_i = y * i;
                    long sumBo = j;
                    sumGo = j;
                    sumRo = j;
                    int i6 = 0;
                    while (true) {
                        i5 = i6;
                        if (i5 > i3) {
                            break;
                        }
                        stack[i5] = src[y2];
                        rgb = src[y2];
                        maxY2 = maxY;
                        maxY = (rgb >>> 16) & 255;
                        g = (rgb >>> 8) & 255;
                        b = rgb & 255;
                        sumR += (long) (maxY * (i5 + 1));
                        sumG += (long) (g * (i5 + 1));
                        sumB += (long) (b * (i5 + 1));
                        sumRo += (long) maxY;
                        sumGo += (long) g;
                        sumBo += (long) b;
                        i6 = i5 + 1;
                        maxY = maxY2;
                        shr_sum = shr_sum;
                        div = div;
                    }
                    shr_sum2 = shr_sum;
                    int div3 = div;
                    maxY2 = maxY;
                    g = 1;
                    while (g <= i3) {
                        y2 += g <= wm ? 1 : 0;
                        stack[g + i3] = src[y2];
                        b = src[y2];
                        r = (b >>> 16) & 255;
                        div = (b >>> 8) & 255;
                        maxY = b & 255;
                        sumR += (long) (((i3 + 1) - g) * r);
                        sumG += (long) (((i3 + 1) - g) * div);
                        sumB += (long) (((i3 + 1) - g) * maxY);
                        sumRi += (long) r;
                        sumGi += (long) div;
                        sumBi += (long) maxY;
                        g++;
                    }
                    g = i3;
                    b = i3;
                    if (b > wm) {
                        b = wm;
                    }
                    r = (y * i) + b;
                    div = b;
                    b = g;
                    g = 0;
                    while (g < i) {
                        maxY = ((int) ((((long) mul_sum) * sumR) >>> shr_sum2)) & 255;
                        i5 = ((int) ((((long) mul_sum) * sumG) >>> shr_sum2)) & 255;
                        i = ((int) ((((long) mul_sum) * sumB) >>> shr_sum2)) & 255;
                        src[dst_i] = (((src[dst_i] & CoverManager.DEFAULT_COLOR) | (maxY << 16)) | (i5 << 8)) | i;
                        sumR -= sumRo;
                        sumG -= sumGo;
                        sumB -= sumBo;
                        i2 = (b + div3) - i3;
                        rgb = div3;
                        if (i2 >= rgb) {
                            i2 -= rgb;
                        }
                        y2 = i2;
                        i = stack[y2];
                        mul_sum2 = mul_sum;
                        sumRo -= (long) ((i >>> 16) & 255);
                        sumGo -= (long) ((i >>> 8) & 255);
                        sumBo -= (long) (i & 255);
                        if (div < wm) {
                            r++;
                            div++;
                        }
                        stack[y2] = src[r];
                        i = src[r];
                        sumRi += (long) ((i >>> 16) & 255);
                        sumGi += (long) ((i >>> 8) & 255);
                        sumBi += (long) (i & 255);
                        sumR += sumRi;
                        sumG += sumGi;
                        sumB += sumBi;
                        b++;
                        if (b >= rgb) {
                            b = 0;
                        }
                        i = stack[b];
                        i2 = (i >>> 16) & 255;
                        i4 = (i >>> 8) & 255;
                        mul_sum = i & 255;
                        sumRo += (long) i2;
                        sumGo += (long) i4;
                        sumBo += (long) mul_sum;
                        sumRi -= (long) i2;
                        sumGi -= (long) i4;
                        sumBi -= (long) mul_sum;
                        g++;
                        dst_i++;
                        div3 = rgb;
                        mul_sum = mul_sum2;
                        r = r;
                        div = div;
                        i = w;
                        i2 = h;
                        i4 = step;
                    }
                    y2 = y + 1;
                    div = div3;
                    maxY = maxY2;
                    shr_sum = shr_sum2;
                    i = w;
                    i2 = h;
                    i4 = step;
                    j = 0;
                } else {
                    shr_sum2 = shr_sum;
                    rgb = div;
                    div2 = mul_sum;
                    b = w;
                    return;
                }
            }
        }
        mul_sum2 = mul_sum;
        shr_sum2 = shr_sum;
        rgb = div;
        if (step == 2) {
            i = h;
            g = i - 1;
            b = w;
            int minX = (core * b) / cores;
            div = ((core + 1) * b) / cores;
            wm = minX;
            while (wm < div) {
                int minX2;
                long sumRi2 = 0;
                sumRi = 0;
                sumRo = 0;
                sumGi = 0;
                sumGo = 0;
                sumBi = 0;
                i5 = wm;
                dst_i = wm;
                sumB = 0;
                sumG = 0;
                sumR = 0;
                int i7 = 0;
                while (true) {
                    y = i7;
                    if (y > i3) {
                        break;
                    }
                    stack[y] = src[i5];
                    maxY = src[i5];
                    i2 = (maxY >>> 16) & 255;
                    i4 = (maxY >>> 8) & 255;
                    minX2 = minX;
                    minX = maxY & 255;
                    sumRi += (long) (i2 * (y + 1));
                    sumGi += (long) (i4 * (y + 1));
                    sumBi += (long) (minX * (y + 1));
                    sumR += (long) i2;
                    sumG += (long) i4;
                    sumB += (long) minX;
                    i7 = y + 1;
                    minX = minX2;
                    div = div;
                    i2 = step;
                }
                minX2 = minX;
                int maxX = div;
                i2 = 1;
                while (i2 <= i3) {
                    i5 += i2 <= g ? b : 0;
                    stack[i2 + i3] = src[i5];
                    i4 = src[i5];
                    minX = (i4 >>> 16) & 255;
                    r = (i4 >>> 8) & 255;
                    div = i4 & 255;
                    sumRi += (long) (((i3 + 1) - i2) * minX);
                    sumGi += (long) (((i3 + 1) - i2) * r);
                    sumBi += (long) (((i3 + 1) - i2) * div);
                    sumRi2 += (long) minX;
                    sumRo += (long) r;
                    sumGo += (long) div;
                    i2++;
                }
                i2 = i3;
                i4 = i3;
                if (i4 > g) {
                    i4 = g;
                }
                minX = (i4 * b) + wm;
                r = i4;
                i4 = i2;
                i2 = 0;
                while (i2 < i) {
                    div2 = mul_sum2;
                    y = ((int) ((((long) div2) * sumRi) >>> shr_sum2)) & 255;
                    maxY = ((int) ((((long) div2) * sumGi) >>> shr_sum2)) & 255;
                    int y3 = i2;
                    i = ((int) ((((long) div2) * sumBi) >>> shr_sum2)) & 255;
                    src[dst_i] = (((src[dst_i] & CoverManager.DEFAULT_COLOR) | (y << 16)) | (maxY << 8)) | i;
                    sumRi -= sumR;
                    sumGi -= sumG;
                    sumBi -= sumB;
                    i2 = (i4 + rgb) - i3;
                    if (i2 >= rgb) {
                        i2 -= rgb;
                    }
                    i7 = i2;
                    i5 = stack[i7];
                    sumR -= (long) ((i5 >>> 16) & 255);
                    sumG -= (long) ((i5 >>> 8) & 255);
                    sumB -= (long) (i5 & 255);
                    if (r < g) {
                        minX += b;
                        r++;
                    }
                    stack[i7] = src[minX];
                    y = src[minX];
                    sumRi2 += (long) ((y >>> 16) & 255);
                    sumRo += (long) ((y >>> 8) & 255);
                    sumGo += (long) (y & 255);
                    sumRi += sumRi2;
                    sumGi += sumRo;
                    sumBi += sumGo;
                    i4++;
                    if (i4 >= rgb) {
                        i4 = 0;
                    }
                    y = stack[i4];
                    i = (y >>> 16) & 255;
                    i2 = (y >>> 8) & 255;
                    i3 = y & 255;
                    sumR += (long) i;
                    sumG += (long) i2;
                    sumB += (long) i3;
                    sumRi2 -= (long) i;
                    sumRo -= (long) i2;
                    sumGo -= (long) i3;
                    i2 = y3 + 1;
                    dst_i += b;
                    mul_sum2 = div2;
                    minX = minX;
                    i4 = i4;
                    i = h;
                    i3 = radius;
                }
                wm++;
                minX = minX2;
                div = maxX;
                i = h;
                i2 = step;
                i3 = radius;
            }
            return;
        }
        div2 = mul_sum2;
        b = w;
    }
}
