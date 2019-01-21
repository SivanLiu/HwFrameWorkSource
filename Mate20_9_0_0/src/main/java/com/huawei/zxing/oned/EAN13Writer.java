package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.EncodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.WriterException;
import com.huawei.zxing.common.BitMatrix;
import java.util.Map;

public final class EAN13Writer extends UPCEANWriter {
    private static final int CODE_WIDTH = 95;

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) throws WriterException {
        if (format == BarcodeFormat.EAN_13) {
            return super.encode(contents, format, width, height, hints);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can only encode EAN_13, but got ");
        stringBuilder.append(format);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean[] encode(String contents) {
        if (contents.length() == 13) {
            try {
                if (UPCEANReader.checkStandardUPCEANChecksum(contents)) {
                    int parities = EAN13Reader.FIRST_DIGIT_ENCODINGS[Integer.parseInt(contents.substring(0, 1))];
                    boolean[] result = new boolean[95];
                    int pos = 0 + OneDimensionalCodeWriter.appendPattern(result, 0, UPCEANReader.START_END_PATTERN, true);
                    for (int i = 1; i <= 6; i++) {
                        int digit = Integer.parseInt(contents.substring(i, i + 1));
                        if (((parities >> (6 - i)) & 1) == 1) {
                            digit += 10;
                        }
                        pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], false);
                    }
                    pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, false);
                    for (int i2 = 7; i2 <= 12; i2++) {
                        pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.L_PATTERNS[Integer.parseInt(contents.substring(i2, i2 + 1))], true);
                    }
                    pos += OneDimensionalCodeWriter.appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);
                    return result;
                }
                throw new IllegalArgumentException("Contents do not pass checksum");
            } catch (FormatException e) {
                throw new IllegalArgumentException("Illegal contents");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Requested contents should be 13 digits long, but got ");
        stringBuilder.append(contents.length());
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
