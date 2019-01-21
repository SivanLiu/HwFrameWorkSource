package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.EncodeHintType;
import com.huawei.zxing.WriterException;
import com.huawei.zxing.common.BitMatrix;
import java.util.Map;

public final class EAN8Writer extends UPCEANWriter {
    private static final int CODE_WIDTH = 67;

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) throws WriterException {
        if (format == BarcodeFormat.EAN_8) {
            return super.encode(contents, format, width, height, hints);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can only encode EAN_8, but got ");
        stringBuilder.append(format);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean[] encode(String contents) {
        if (contents.length() == 8) {
            int i;
            boolean[] result = new boolean[67];
            int pos = 0 + OneDimensionalCodeWriter.appendPattern(result, 0, UPCEANReader.START_END_PATTERN, true);
            for (i = 0; i <= 3; i++) {
                pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.L_PATTERNS[Integer.parseInt(contents.substring(i, i + 1))], false);
            }
            pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, false);
            for (i = 4; i <= 7; i++) {
                pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.L_PATTERNS[Integer.parseInt(contents.substring(i, i + 1))], true);
            }
            pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Requested contents should be 8 digits long, but got ");
        stringBuilder.append(contents.length());
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
