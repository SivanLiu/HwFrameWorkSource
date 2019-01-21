package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.EncodeHintType;
import com.huawei.zxing.WriterException;
import com.huawei.zxing.common.BitMatrix;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public final class Code128Writer extends OneDimensionalCodeWriter {
    private static final int CODE_CODE_B = 100;
    private static final int CODE_CODE_C = 99;
    private static final int CODE_FNC_1 = 102;
    private static final int CODE_FNC_2 = 97;
    private static final int CODE_FNC_3 = 96;
    private static final int CODE_FNC_4_B = 100;
    private static final int CODE_START_B = 104;
    private static final int CODE_START_C = 105;
    private static final int CODE_STOP = 106;
    private static final char ESCAPE_FNC_1 = 'ñ';
    private static final char ESCAPE_FNC_2 = 'ò';
    private static final char ESCAPE_FNC_3 = 'ó';
    private static final char ESCAPE_FNC_4 = 'ô';

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) throws WriterException {
        if (format == BarcodeFormat.CODE_128) {
            return super.encode(contents, format, width, height, hints);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can only encode CODE_128, but got ");
        stringBuilder.append(format);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean[] encode(String contents) {
        int length = contents.length();
        StringBuilder stringBuilder;
        if (length < 1 || length > 80) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Contents length should be between 1 and 80 characters, but got ");
            stringBuilder.append(length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        for (int i = 0; i < length; i++) {
            char c = contents.charAt(i);
            if (c < ' ' || c > '~') {
                switch (c) {
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad character in input: ");
                        stringBuilder.append(c);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
        Collection<int[]> patterns = new ArrayList();
        int codeSet = 0;
        int checkWeight = 1;
        int checkSum = 0;
        int position = 0;
        while (position < length) {
            int newCodeSet;
            int patternIndex;
            if (isDigits(contents, position, codeSet == 99 ? 2 : 4)) {
                newCodeSet = 99;
            } else {
                newCodeSet = 100;
            }
            if (newCodeSet == codeSet) {
                if (codeSet != 100) {
                    switch (contents.charAt(position)) {
                        case 241:
                            patternIndex = 102;
                            position++;
                            break;
                        case 242:
                            patternIndex = CODE_FNC_2;
                            position++;
                            break;
                        case 243:
                            patternIndex = CODE_FNC_3;
                            position++;
                            break;
                        case 244:
                            patternIndex = 100;
                            position++;
                            break;
                        default:
                            patternIndex = Integer.parseInt(contents.substring(position, position + 2));
                            position += 2;
                            break;
                    }
                }
                patternIndex = contents.charAt(position) - 32;
                position++;
            } else {
                if (codeSet != 0) {
                    patternIndex = newCodeSet;
                } else if (newCodeSet == 100) {
                    patternIndex = 104;
                } else {
                    patternIndex = 105;
                }
                codeSet = newCodeSet;
            }
            patterns.add(Code128Reader.CODE_PATTERNS[patternIndex]);
            checkSum += patternIndex * checkWeight;
            if (position != 0) {
                checkWeight++;
            }
        }
        patterns.add(Code128Reader.CODE_PATTERNS[checkSum % 103]);
        patterns.add(Code128Reader.CODE_PATTERNS[106]);
        int codeWidth = 0;
        for (int[] pattern : patterns) {
            int codeWidth2 = codeWidth;
            for (int width : (int[]) r9.next()) {
                codeWidth2 += width;
            }
            codeWidth = codeWidth2;
        }
        boolean[] result = new boolean[codeWidth];
        int pos = 0;
        for (int[] pattern2 : patterns) {
            pos += OneDimensionalCodeWriter.appendPattern(result, pos, pattern2, true);
        }
        return result;
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0025  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isDigits(CharSequence value, int start, int length) {
        boolean z;
        int end = start + length;
        int last = value.length();
        int end2 = end;
        end = start;
        while (true) {
            z = false;
            if (end < end2 && end < last) {
                char c = value.charAt(end);
                if (c < '0' || c > '9') {
                    if (c != ESCAPE_FNC_1) {
                        return false;
                    }
                    end2++;
                }
                end++;
            } else if (end2 <= last) {
                z = true;
            }
        }
        if (end2 <= last) {
        }
        return z;
    }
}
