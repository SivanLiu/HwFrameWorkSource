package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.EncodeHintType;
import com.huawei.zxing.WriterException;
import com.huawei.zxing.common.BitMatrix;
import java.util.Map;

public final class Code39Writer extends OneDimensionalCodeWriter {
    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) throws WriterException {
        if (format == BarcodeFormat.CODE_39) {
            return super.encode(contents, format, width, height, hints);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can only encode CODE_39, but got ");
        stringBuilder.append(format);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean[] encode(String contents) {
        int length = contents.length();
        if (length <= 80) {
            int codeWidth;
            int[] widths = new int[9];
            int codeWidth2 = 25 + length;
            int i = 0;
            while (i < length) {
                int indexInString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%".indexOf(contents.charAt(i));
                if (indexInString >= 0) {
                    toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
                    codeWidth = codeWidth2;
                    for (int width : widths) {
                        codeWidth += width;
                    }
                    i++;
                    codeWidth2 = codeWidth;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad contents: ");
                    stringBuilder.append(contents);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            boolean[] result = new boolean[codeWidth2];
            toIntArray(Code39Reader.CHARACTER_ENCODINGS[39], widths);
            codeWidth = OneDimensionalCodeWriter.appendPattern(result, 0, widths, true);
            int[] narrowWhite = new int[]{1};
            codeWidth += OneDimensionalCodeWriter.appendPattern(result, codeWidth, narrowWhite, false);
            for (int i2 = length - 1; i2 >= 0; i2--) {
                toIntArray(Code39Reader.CHARACTER_ENCODINGS["0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%".indexOf(contents.charAt(i2))], widths);
                codeWidth += OneDimensionalCodeWriter.appendPattern(result, codeWidth, widths, true);
                codeWidth += OneDimensionalCodeWriter.appendPattern(result, codeWidth, narrowWhite, false);
            }
            toIntArray(Code39Reader.CHARACTER_ENCODINGS[39], widths);
            codeWidth += OneDimensionalCodeWriter.appendPattern(result, codeWidth, widths, true);
            return result;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Requested contents should be less than 80 digits long, but got ");
        stringBuilder2.append(length);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private static void toIntArray(int a, int[] toReturn) {
        for (int i = 0; i < 9; i++) {
            int i2 = 1;
            if (((1 << i) & a) != 0) {
                i2 = 2;
            }
            toReturn[i] = i2;
        }
    }
}
