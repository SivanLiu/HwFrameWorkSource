package com.huawei.iimagekit.shadow;

import android.graphics.Bitmap;
import android.util.IMonitorKeys;

public class ShadowStackBlur {
    private static final short[] stack_blur_mul = new short[]{(short) 512, (short) 512, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, (short) 512, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, (short) 512, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, (short) 271, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 512, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, (short) 271, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 273, (short) 512, IMonitorKeys.E904200009_DSAPP5SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_PSDURNOREG_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_DSFAILNUM_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, (short) 284, (short) 271, (short) 259, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP2RTTL1_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DSAPP1RTT_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_LUSUCC_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_DSTOTALNUM_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_MTCSFBREDIRTOTDSCALLSUC_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID5_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 282, (short) 273, (short) 265, (short) 512, IMonitorKeys.E904200009_DSAPP5RTTL4_INT, IMonitorKeys.E904200009_DSAPP5SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP4TCPSUCCNUM_INT, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP2RTTL5_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1DELAYL6_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_ACCESSEDCELLCOUNT_SMALLINT, IMonitorKeys.E904200009_PSDURNOREG_INT, IMonitorKeys.E904200009_RAUFAIL_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSFAILNUM_INT, IMonitorKeys.E904200009_CALLDURATIONRE_60_180_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_MTSRVCCCALLSUCC_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB900TIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID0_INT, (short) 284, (short) 278, (short) 271, (short) 265, (short) 259, IMonitorKeys.E904200009_DSAPP6DELAYL1_INT, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP5TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP4NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP3NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP2RTTL1_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DSAPP1DELAYL1_INT, IMonitorKeys.E904200009_DSAPP1RTT_INT, IMonitorKeys.E904200009_IT310REGION1_SMALLINT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_PSDURREG3G_INT, IMonitorKeys.E904200009_LUSUCC_INT, IMonitorKeys.E904200009_GU3GPDPSUCC_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSTOTALNUM_INT, IMonitorKeys.E904200009_CALLDURATIONLO_180_I_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBANDOTHER_INT, IMonitorKeys.E904200009_MTCSFBREDIRTOTDSCALLSUC_INT, IMonitorKeys.E904200009_VOLTECSREDIALCALLSUCC_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB850TIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID5_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID0_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 287, (short) 282, (short) 278, (short) 273, (short) 269, (short) 265, (short) 261, (short) 512, IMonitorKeys.E904200009_DSAPP6TCPTOTALNUM_INT, IMonitorKeys.E904200009_DSAPP5RTTL4_INT, IMonitorKeys.E904200009_DSAPP5DELAYL2_INT, IMonitorKeys.E904200009_DSAPP5SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP4TCPSUCCNUM_INT, IMonitorKeys.E904200009_DSAPP4RTT_INT, IMonitorKeys.E904200009_DSAPP3DELAYL5_INT, IMonitorKeys.E904200009_DSAPP3TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP2RTTL5_INT, IMonitorKeys.E904200009_DSAPP2DELAYL5_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP1RTTL5_INT, IMonitorKeys.E904200009_DSAPP1DELAYL6_INT, IMonitorKeys.E904200009_DSAPP1TCPSUCCNUM_INT, IMonitorKeys.E904200009_DSAPP1WEBDELAY_INT, IMonitorKeys.E904200009_NORMALCALLBCELL_INT, IMonitorKeys.E904200009_ACCESSEDCELLCOUNT_SMALLINT, IMonitorKeys.E904200009_DNSDELAY_20_150_INT, IMonitorKeys.E904200009_PSDURNOREG_INT, IMonitorKeys.E904200009_CSDURREG4G_INT, IMonitorKeys.E904200009_RAUFAIL_INT, IMonitorKeys.E904200009_GU3GPDPFAIL_INT, IMonitorKeys.E904200009_GU2GPDPSUCC_INT, IMonitorKeys.E904200009_DSRTTNUML5_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSDELAYNUML2_INT, IMonitorKeys.E904200009_DSFAILNUM_INT, IMonitorKeys.E904200009_CALLDURATIONLO_180_I_INT, IMonitorKeys.E904200009_CALLDURATIONRE_60_180_INT, IMonitorKeys.E904200009_LTECSFBFRREDIRDUR_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND8_INT, IMonitorKeys.E904200009_MTCSFBLTEACFAILCALLSUC_INT, IMonitorKeys.E904200009_MTSRVCCCALLSUCC_INT, IMonitorKeys.E904200009_CALLNUMWITHLAC_CLASS, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLCHFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB900TIMELEN_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID4_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID1_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID3_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID0_INT, (short) 287, (short) 284, (short) 281, (short) 278, (short) 274, (short) 271, (short) 268, (short) 265, (short) 262, (short) 259, (short) 257, IMonitorKeys.E904200009_DSAPP6DELAYL1_INT, IMonitorKeys.E904200009_DSAPP6SUCCNUM_INT, IMonitorKeys.E904200009_DSAPP5RTTL3_INT, IMonitorKeys.E904200009_DSAPP5DELAYL4_INT, IMonitorKeys.E904200009_DSAPP5TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP5RTT_INT, IMonitorKeys.E904200009_DSAPP4RTTL1_INT, IMonitorKeys.E904200009_DSAPP4DELAYL2_INT, IMonitorKeys.E904200009_DSAPP4NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP3RTTL5_INT, IMonitorKeys.E904200009_DSAPP3RTTL1_INT, IMonitorKeys.E904200009_DSAPP3DELAYL2_INT, IMonitorKeys.E904200009_DSAPP3NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP3RTT_INT, IMonitorKeys.E904200009_DSAPP2RTTL1_INT, IMonitorKeys.E904200009_DSAPP2DELAYL3_INT, IMonitorKeys.E904200009_DSAPP2TOTALNUM_INT, IMonitorKeys.E904200009_DSAPP2WEBDELAY_INT, IMonitorKeys.E904200009_DSAPP1RTTL3_INT, IMonitorKeys.E904200009_DSAPP1DELAYL5_INT, IMonitorKeys.E904200009_DSAPP1DELAYL1_INT, IMonitorKeys.E904200009_DSAPP1NOACKNUM_INT, IMonitorKeys.E904200009_DSAPP1RTT_INT, IMonitorKeys.E904200009_ABNORMALCALLBCELL_INT, IMonitorKeys.E904200009_IT310REGION1_SMALLINT, IMonitorKeys.E904200009_DNSDELAY1000_2000_INT, IMonitorKeys.E904200009_DNSDELAY_0_20_INT, IMonitorKeys.E904200009_DNSTOTALDELAY_INT, IMonitorKeys.E904200009_PSDURREG3G_INT, IMonitorKeys.E904200009_CSDURREG3G_INT, IMonitorKeys.E904200009_LUSUCC_INT, IMonitorKeys.E904200009_GUATTSUCC_INT, IMonitorKeys.E904200009_GU3GPDPSUCC_INT, IMonitorKeys.E904200009_EPSTAUFAIL_INT, IMonitorKeys.E904200009_EPSATTACHSUCC_INT, IMonitorKeys.E904200009_DSRTTNUML3_INT, IMonitorKeys.E904200009_DSDELAYNUML6_INT, IMonitorKeys.E904200009_DSDELAYNUML2_INT, IMonitorKeys.E904200009_DSTOTALNUM_INT, IMonitorKeys.E904200009_DSSUCCNUM_INT, IMonitorKeys.E904200009_CALLDURATIONLO_180_I_INT, IMonitorKeys.E904200009_CALLDURATIONRE_180_I_INT, IMonitorKeys.E904200009_ACTIVECALLLOHANGUPNUM_INT, IMonitorKeys.E904200009_LTECSFBFRREDIRDUR_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBANDOTHER_INT, IMonitorKeys.E904200009_WCDMAVOICETIMEONBAND2_INT, IMonitorKeys.E904200009_MTCSFBREDIRTOTDSCALLSUC_INT, IMonitorKeys.E904200009_MTSRVCCCALLSUCC_INT, IMonitorKeys.E904200009_VOLTECSREDIALCALLSUCC_INT, IMonitorKeys.E904200009_DSDACALLNUM_INT, IMonitorKeys.E904200009_GSMCSCALLCHAFSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLCHHSTIMELEN_INT, IMonitorKeys.E904200009_GSMCSCALLB850TIMELEN_INT, IMonitorKeys.E904200009_CHRRECORDNUM_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID5_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID2_INT, IMonitorKeys.E904200009_TIMEHDRSIGGRID0_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID3_INT, IMonitorKeys.E904200009_TIMECDMASIGGRID1_INT, (short) 289, (short) 287, (short) 285, (short) 282, (short) 280, (short) 278, (short) 275, (short) 273, (short) 271, (short) 269, (short) 267, (short) 265, (short) 263, (short) 261, (short) 259};
    private static final byte[] stack_blur_shr = new byte[]{(byte) 9, (byte) 11, (byte) 12, (byte) 13, (byte) 13, (byte) 14, (byte) 14, (byte) 15, (byte) 15, (byte) 15, (byte) 15, (byte) 16, (byte) 16, (byte) 16, (byte) 16, (byte) 17, (byte) 17, (byte) 17, (byte) 17, (byte) 17, (byte) 17, (byte) 17, (byte) 18, (byte) 18, (byte) 18, (byte) 18, (byte) 18, (byte) 18, (byte) 18, (byte) 18, (byte) 18, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 19, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 20, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 21, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 23, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24, (byte) 24};

    public static void doBlur(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        int w = bitmapForBlur.getWidth();
        int h = bitmapForBlur.getHeight();
        int[] pixels = new int[(w * h)];
        int[] iArr = pixels;
        int i = w;
        int i2 = w;
        int i3 = h;
        bitmapForBlur.getPixels(iArr, 0, i, 0, 0, i2, i3);
        ShadowUtil.processAlphaChannelBefore(pixels);
        stackBlur(pixels, w, h, radius);
        ShadowUtil.processAlphaChannelAfter(pixels);
        blurredBitmap.setPixels(iArr, 0, i, 0, 0, i2, i3);
    }

    private static void stackBlur(int[] src, int w, int h, int radius) {
        long sum_r_o;
        long sum_g;
        long sum_g_i;
        long sum_g_o;
        long sum_b;
        long sum_b_i;
        long sum_b_o;
        long sum_a;
        long sum_a_i;
        long sum_a_o;
        int rgb;
        int a;
        int r;
        int g;
        int b;
        int i;
        int[] stack;
        int src_i;
        short mul_sum;
        short shr_sum;
        int g2;
        int y = w;
        int i2 = h;
        int r2 = radius;
        short mul_sum2 = stack_blur_mul[r2];
        short shr_sum2 = (short) stack_blur_shr[r2];
        int div = (r2 * 2) + 1;
        int[] stack2 = new int[div];
        int wm = y - 1;
        int y2 = 0;
        while (y2 < i2) {
            sum_r_o = 0;
            sum_g = 0;
            sum_g_i = 0;
            sum_g_o = 0;
            sum_b = 0;
            sum_b_i = 0;
            sum_b_o = 0;
            sum_a = 0;
            sum_a_i = 0;
            sum_a_o = 0;
            int src_i2 = y * y2;
            int dst_i = y2 * y;
            rgb = src[src_i2];
            a = (rgb >>> 24) & 255;
            long sum_r = 0;
            r = (rgb >>> 16) & 255;
            g = (rgb >>> 8) & 255;
            long sum_r_i = 0;
            b = rgb & 255;
            i = 0;
            while (i <= r2) {
                stack2[i] = rgb;
                sum_a += (long) (a * (i + 1));
                sum_r += (long) ((i + 1) * r);
                sum_g += (long) ((i + 1) * g);
                sum_b += (long) ((i + 1) * b);
                sum_a_o += (long) a;
                sum_r_o += (long) r;
                sum_g_o += (long) g;
                sum_b_o += (long) b;
                i++;
                rgb = rgb;
                div = div;
            }
            int div2 = div;
            int i3 = rgb;
            div = 1;
            while (div <= r2) {
                if (div <= wm) {
                    src_i2++;
                }
                rgb = src[src_i2];
                a = (rgb >>> 24) & 255;
                r = (rgb >>> 16) & 255;
                g = (rgb >>> 8) & 255;
                b = rgb & 255;
                stack2[div + r2] = rgb;
                sum_a += (long) (((r2 + 1) - div) * a);
                sum_r += (long) (((r2 + 1) - div) * r);
                sum_g += (long) (((r2 + 1) - div) * g);
                sum_b += (long) (((r2 + 1) - div) * b);
                sum_a_i += (long) a;
                sum_r_i += (long) r;
                sum_g_i += (long) g;
                sum_b_i += (long) b;
                div++;
                stack2 = stack2;
                i3 = rgb;
            }
            stack = stack2;
            div = r2;
            rgb = r2;
            if (rgb > wm) {
                rgb = wm;
            }
            src_i = (y2 * y) + rgb;
            i = rgb;
            rgb = div;
            div = 0;
            while (div < y) {
                r = ((int) ((((long) mul_sum2) * sum_a) >>> shr_sum2)) & 255;
                g = ((int) ((((long) mul_sum2) * sum_r) >>> shr_sum2)) & 255;
                y = ((int) ((((long) mul_sum2) * sum_g) >>> shr_sum2)) & 255;
                int y3 = y2;
                i2 = ((int) ((((long) mul_sum2) * sum_b) >>> shr_sum2)) & 255;
                src[dst_i] = (((r << 24) | (g << 16)) | (y << 8)) | i2;
                dst_i++;
                sum_a -= sum_a_o;
                sum_r -= sum_r_o;
                sum_g -= sum_g_o;
                sum_b -= sum_b_o;
                a = (rgb + div2) - r2;
                y2 = div2;
                if (a >= y2) {
                    a -= y2;
                }
                b = a;
                y = stack[b];
                mul_sum = mul_sum2;
                shr_sum = shr_sum2;
                sum_a_o -= (long) ((y >>> 24) & 255);
                sum_r_o -= (long) ((y >>> 16) & 255);
                sum_g_o -= (long) ((y >>> 8) & 255);
                sum_b_o -= (long) (y & 255);
                if (i < wm) {
                    src_i++;
                    i++;
                }
                y = src[src_i];
                i2 = (y >>> 24) & 255;
                mul_sum2 = (y >>> 16) & 255;
                g2 = (y >>> 8) & 255;
                a = y & 255;
                stack[b] = y;
                sum_a_i += (long) i2;
                sum_r_i += (long) mul_sum2;
                sum_g_i += (long) g2;
                sum_b_i += (long) a;
                sum_a += sum_a_i;
                sum_r += sum_r_i;
                sum_g += sum_g_i;
                sum_b += sum_b_i;
                rgb++;
                if (rgb >= y2) {
                    rgb = 0;
                }
                y = stack[rgb];
                i2 = (y >>> 24) & 255;
                mul_sum2 = (y >>> 16) & 255;
                g = (y >>> 8) & 255;
                b = y & 255;
                sum_a_o += (long) i2;
                sum_r_o += (long) mul_sum2;
                sum_g_o += (long) g;
                sum_b_o += (long) b;
                sum_a_i -= (long) i2;
                sum_r_i -= (long) mul_sum2;
                sum_g_i -= (long) g;
                sum_b_i -= (long) b;
                div++;
                i3 = y;
                a = i2;
                short r3 = mul_sum2;
                div2 = y2;
                y2 = y3;
                mul_sum2 = mul_sum;
                shr_sum2 = shr_sum;
                src_i = src_i;
                rgb = rgb;
                y = w;
                i2 = h;
            }
            shr_sum = shr_sum2;
            div = div2;
            stack2 = stack;
            i2 = h;
            y2++;
            y = w;
        }
        mul_sum = mul_sum2;
        shr_sum = shr_sum2;
        y2 = div;
        stack = stack2;
        y = h;
        i2 = y - 1;
        int x = 0;
        while (true) {
            g2 = w;
            int wm2;
            short mul_sum3;
            if (x < g2) {
                long sum_a_i2 = 0;
                sum_r_o = 0;
                sum_g = 0;
                sum_g_o = 0;
                sum_b = 0;
                sum_b_o = 0;
                sum_a = 0;
                src_i = x;
                a = x;
                long sum_a2 = 0;
                div = src[src_i];
                rgb = (div >>> 24) & 255;
                int src_i3 = src_i;
                src_i = (div >>> 16) & 255;
                wm2 = wm;
                wm = (div >>> 8) & 255;
                int dst_i2 = a;
                a = div & 255;
                sum_a_o = 0;
                sum_a_i = 0;
                sum_b_i = 0;
                sum_g_i = 0;
                b = 0;
                while (b <= r2) {
                    stack[b] = div;
                    sum_a2 += (long) ((b + 1) * rgb);
                    sum_r_o += (long) ((b + 1) * src_i);
                    sum_g_o += (long) ((b + 1) * wm);
                    sum_b_o += (long) ((b + 1) * a);
                    sum_g_i += (long) rgb;
                    sum_b_i += (long) src_i;
                    sum_a_i += (long) wm;
                    sum_a_o += (long) a;
                    b++;
                    sum_a_i2 = sum_a_i2;
                }
                long sum_a_i3 = sum_a_i2;
                a = wm;
                wm = src_i;
                src_i = rgb;
                rgb = div;
                for (div = 1; div <= r2; div++) {
                    if (div <= i2) {
                        src_i3 += g2;
                    }
                    rgb = src[src_i3];
                    src_i = (rgb >>> 24) & 255;
                    wm = (rgb >>> 16) & 255;
                    a = (rgb >>> 8) & 255;
                    r = rgb & 255;
                    stack[div + r2] = rgb;
                    sum_a2 += (long) (((r2 + 1) - div) * src_i);
                    sum_r_o += (long) (((r2 + 1) - div) * wm);
                    sum_g_o += (long) (((r2 + 1) - div) * a);
                    sum_b_o += (long) (((r2 + 1) - div) * r);
                    sum_a_i3 += (long) src_i;
                    sum_g += (long) wm;
                    sum_b += (long) a;
                    sum_a += (long) r;
                }
                div = r2;
                g = r2;
                if (g > i2) {
                    g = i2;
                }
                b = (g * g2) + x;
                i = g;
                g = div;
                div = 0;
                while (div < y) {
                    short mul_sum4 = mul_sum;
                    rgb = ((int) ((((long) mul_sum4) * sum_a2) >>> shr_sum)) & 255;
                    src_i = ((int) ((((long) mul_sum4) * sum_r_o) >>> shr_sum)) & 255;
                    wm = ((int) ((((long) mul_sum4) * sum_g_o) >>> shr_sum)) & 255;
                    int src_i4 = b;
                    int yp = i;
                    a = ((int) ((((long) mul_sum4) * sum_b_o) >>> shr_sum)) & 255;
                    src[dst_i2] = (((rgb << 24) | (src_i << 16)) | (wm << 8)) | a;
                    dst_i2 += g2;
                    sum_a2 -= sum_g_i;
                    sum_r_o -= sum_b_i;
                    sum_g_o -= sum_a_i;
                    sum_b_o -= sum_a_o;
                    b = (g + y2) - r2;
                    if (b >= y2) {
                        b -= y2;
                    }
                    i = b;
                    mul_sum3 = mul_sum4;
                    y = stack[i];
                    sum_g_i -= (long) ((y >>> 24) & 255);
                    sum_b_i -= (long) ((y >>> 16) & 255);
                    sum_a_i -= (long) ((y >>> 8) & 255);
                    sum_a_o -= (long) (y & 255);
                    wm = yp;
                    if (wm < i2) {
                        a = src_i4 + g2;
                        wm++;
                    } else {
                        a = src_i4;
                    }
                    y = src[a];
                    int a2 = (y >>> 24) & 255;
                    r2 = (y >>> 16) & 255;
                    rgb = (y >>> 8) & 255;
                    src_i = y & 255;
                    stack[i] = y;
                    int hm = i2;
                    sum_a_i3 += (long) a2;
                    sum_g += (long) r2;
                    sum_b += (long) rgb;
                    sum_a += (long) src_i;
                    sum_a2 += sum_a_i3;
                    sum_r_o += sum_g;
                    sum_g_o += sum_b;
                    sum_b_o += sum_a;
                    g++;
                    if (g >= y2) {
                        g = 0;
                    }
                    y = g;
                    i2 = stack[y];
                    a2 = (i2 >>> 24) & 255;
                    r2 = (i2 >>> 16) & 255;
                    rgb = (i2 >>> 8) & 255;
                    r = i2 & 255;
                    int rgb2 = i2;
                    sum_g_i += (long) a2;
                    sum_b_i += (long) r2;
                    sum_a_i += (long) rgb;
                    sum_a_o += (long) r;
                    sum_a_i3 -= (long) a2;
                    sum_g -= (long) r2;
                    sum_b -= (long) rgb;
                    sum_a -= (long) r;
                    div++;
                    src_i = a2;
                    i = wm;
                    b = a;
                    mul_sum = mul_sum3;
                    i2 = hm;
                    y = h;
                    wm = r2;
                    a = rgb;
                    rgb = rgb2;
                    r2 = radius;
                }
                mul_sum3 = mul_sum;
                x++;
                wm = wm2;
                y = h;
                r2 = radius;
            } else {
                wm2 = wm;
                mul_sum3 = mul_sum;
                return;
            }
        }
    }
}
