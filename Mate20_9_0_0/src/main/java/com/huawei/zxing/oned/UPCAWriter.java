package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.EncodeHintType;
import com.huawei.zxing.Writer;
import com.huawei.zxing.WriterException;
import com.huawei.zxing.common.BitMatrix;
import java.util.Map;

public final class UPCAWriter implements Writer {
    private final EAN13Writer subWriter = new EAN13Writer();

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) throws WriterException {
        return encode(contents, format, width, height, null);
    }

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) throws WriterException {
        if (format == BarcodeFormat.UPC_A) {
            return this.subWriter.encode(preencode(contents), BarcodeFormat.EAN_13, width, height, hints);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can only encode UPC-A, but got ");
        stringBuilder.append(format);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static String preencode(String contents) {
        StringBuilder stringBuilder;
        int length = contents.length();
        if (length == 11) {
            int sum = 0;
            for (int i = 0; i < 11; i++) {
                sum += (contents.charAt(i) - 48) * (i % 2 == 0 ? 3 : 1);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(contents);
            stringBuilder.append((1000 - sum) % 10);
            contents = stringBuilder.toString();
        } else if (length != 12) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Requested contents should be 11 or 12 digits long, but got ");
            stringBuilder.append(contents.length());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append('0');
        stringBuilder.append(contents);
        return stringBuilder.toString();
    }
}
