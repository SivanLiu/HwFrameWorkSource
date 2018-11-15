package android_maps_conflict_avoidance.com.google.image.compression.jpeg;

import com.google.android.maps.MapView.LayoutParams;

public class JpegUtil {
    private static final byte[][] JPEG_QUANT_TABLES = new byte[][]{new byte[]{(byte) 16, (byte) 11, (byte) 12, (byte) 14, (byte) 12, (byte) 10, (byte) 16, (byte) 14, (byte) 13, (byte) 14, (byte) 18, (byte) 17, (byte) 16, (byte) 19, (byte) 24, (byte) 40, (byte) 26, (byte) 24, (byte) 22, (byte) 22, (byte) 24, (byte) 49, (byte) 35, (byte) 37, (byte) 29, (byte) 40, (byte) 58, (byte) 51, (byte) 61, (byte) 60, (byte) 57, (byte) 51, (byte) 56, (byte) 55, (byte) 64, (byte) 72, (byte) 92, (byte) 78, (byte) 64, (byte) 68, (byte) 87, (byte) 69, (byte) 55, (byte) 56, (byte) 80, (byte) 109, (byte) 81, (byte) 87, (byte) 95, (byte) 98, (byte) 103, (byte) 104, (byte) 103, (byte) 62, (byte) 77, (byte) 113, (byte) 121, (byte) 112, (byte) 100, (byte) 120, (byte) 92, (byte) 101, (byte) 103, (byte) 99}, new byte[]{(byte) 17, (byte) 18, (byte) 18, (byte) 24, (byte) 21, (byte) 24, (byte) 47, (byte) 26, (byte) 26, (byte) 47, (byte) 99, (byte) 66, (byte) 56, (byte) 66, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99, (byte) 99}};
    private static final int[] imageIoScaleFactor = new int[]{-1, 1677721601, 838860801, 559240577, 419430401, 335544321, 279620289, 239674513, 209715201, 186413505, 167772161, 152520145, 139810145, 129055513, 119837257, 111848105, 104857601, 98689505, 93206753, 88301137, 83886081, 79891505, 76260073, 72944417, 69905073, 67108865, 64527757, 62137837, 59918629, 57852473, 55924053, 54120053, 52428801, 50840049, 49344753, 47934905, 46603377, 45343829, 44150569, 43018505, 41943041, 40920041, 39945753, 39016781, 38130037, 37282705, 36472209, 35696205, 34952537, 34239217, 33554433, 32883345, 32212257, 31541169, 30870077, 30198989, 29527901, 28856813, 28185725, 27514637, 26843545, 26172457, 25501369, 24830281, 24159193, 23488105, 22817013, 22145925, 21474837, 20803749, 20132661, 19461573, 18790481, 18119393, 17448305, 16777217, 16106129, 15435041, 14763953, 14092861, 13421773, 12750685, 12079597, 11408509, 10737421, 10066329, 9395241, 8724153, 8053065, 7381977, 6710889, 6039797, 5368709, 4697621, 4026533, 3355445, 2684357, 2013265, 1342177, 671089, 1};

    public static byte getScaledQuantizationFactor(int q, int quality, int qualityAlgorithm) {
        int val;
        switch (qualityAlgorithm) {
            case LayoutParams.MODE_MAP /*0*/:
                if (q != 99 || quality != 36) {
                    val = (int) ((((((long) q) * ((long) imageIoScaleFactor[quality])) / 16777216) + 1) / 2);
                    break;
                }
                val = 138;
                break;
                break;
            case 1:
                int iscale;
                if (quality < 50) {
                    iscale = Math.min(5000 / quality, 5000);
                } else {
                    iscale = Math.max(200 - (2 * quality), 0);
                }
                val = ((q * iscale) + 50) / 100;
                break;
            default:
                throw new IllegalArgumentException("qualityAlgorithm");
        }
        if (val < 1) {
            val = 1;
        } else if (val > 255) {
            val = 255;
        }
        return (byte) val;
    }

    public static synchronized byte[] getQuantTable(int quantType, int quality, int qualityAlgorithm) {
        byte[] qtable;
        synchronized (JpegUtil.class) {
            int index = ((154 * quantType) + (77 * qualityAlgorithm)) + (quality - 24);
            qtable = new byte[64];
            byte[] rawTable = JPEG_QUANT_TABLES[quantType];
            for (int j = 0; j < 64; j++) {
                qtable[j] = getScaledQuantizationFactor(rawTable[j] & 255, quality, qualityAlgorithm);
            }
        }
        return qtable;
    }

    static void prependStandardHeader(byte[] src, int soff, int len, byte[] dst, int doff, JpegHeaderParams params) {
        int variant = params.getVariant();
        int width = params.getWidth();
        int height = params.getHeight();
        int quality = params.getQuality();
        int qualityAlgorithm = params.getQualityAlgorithm();
        if (variant == 0) {
            Object obj = dst;
            System.arraycopy(src, soff, obj, doff + GenerateJpegHeader.getHeaderLength(variant), len);
            GenerateJpegHeader.generate(obj, doff, variant, width, height, quality, qualityAlgorithm);
            return;
        }
        byte[] bArr = src;
        int i = soff;
        throw new IllegalArgumentException("variant");
    }

    public static byte[] uncompactJpeg(byte[] compactJpegData, int off, int len) {
        byte[] bArr = compactJpegData;
        int i = off;
        int i2 = len;
        if (bArr[i] == (byte) -1 && bArr[i + 1] == (byte) -40) {
            byte[] data = new byte[i2];
            System.arraycopy(bArr, i, data, 0, i2);
            return data;
        } else if (bArr[i] == (byte) 67 && bArr[i + 1] == (byte) 74 && bArr[i + 2] == (byte) 80 && bArr[i + 3] == (byte) 71) {
            int variant = bArr[i + 4] & 255;
            int width = ((bArr[i + 5] & 255) << 8) | (bArr[i + 6] & 255);
            int height = ((bArr[i + 7] & 255) << 8) | (bArr[i + 8] & 255);
            int quality = bArr[i + 9] & 255;
            int qualityAlgorithm = bArr[i + 10] & 255;
            try {
                int hlen = GenerateJpegHeader.getHeaderLength(variant);
                byte[] jpegData = new byte[((hlen + i2) - 11)];
                JpegHeaderParams params = new JpegHeaderParams(variant, width, height, quality, qualityAlgorithm, hlen);
                byte[] jpegData2 = jpegData;
                prependStandardHeader(bArr, i + 11, i2 - 11, jpegData2, 0, params);
                return jpegData2;
            } catch (IllegalArgumentException e) {
                int i3 = quality;
                int variant2 = variant;
                IllegalArgumentException illegalArgumentException = e;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown variant ");
                stringBuilder.append(variant2);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else {
            throw new IllegalArgumentException("Input is not in compact JPEG format");
        }
    }

    public static byte[] uncompactJpeg(byte[] compactJpegData) {
        return uncompactJpeg(compactJpegData, 0, compactJpegData.length);
    }
}
