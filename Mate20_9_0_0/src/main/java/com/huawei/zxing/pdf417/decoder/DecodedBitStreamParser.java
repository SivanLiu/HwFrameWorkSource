package com.huawei.zxing.pdf417.decoder;

import com.huawei.zxing.FormatException;
import com.huawei.zxing.common.DecoderResult;
import com.huawei.zxing.pdf417.PDF417ResultMetadata;
import java.math.BigInteger;
import java.util.Arrays;

final class DecodedBitStreamParser {
    private static final int AL = 28;
    private static final int AS = 27;
    private static final int BEGIN_MACRO_PDF417_CONTROL_BLOCK = 928;
    private static final int BEGIN_MACRO_PDF417_OPTIONAL_FIELD = 923;
    private static final int BYTE_COMPACTION_MODE_LATCH = 901;
    private static final int BYTE_COMPACTION_MODE_LATCH_6 = 924;
    private static final BigInteger[] EXP900 = new BigInteger[16];
    private static final int LL = 27;
    private static final int MACRO_PDF417_TERMINATOR = 922;
    private static final int MAX_NUMERIC_CODEWORDS = 15;
    private static final char[] MIXED_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '&', 13, 9, ',', ':', '#', '-', '.', '$', '/', '+', '%', '*', '=', '^'};
    private static final int ML = 28;
    private static final int MODE_SHIFT_TO_BYTE_COMPACTION_MODE = 913;
    private static final int NUMBER_OF_SEQUENCE_CODEWORDS = 2;
    private static final int NUMERIC_COMPACTION_MODE_LATCH = 902;
    private static final int PAL = 29;
    private static final int PL = 25;
    private static final int PS = 29;
    private static final char[] PUNCT_CHARS = new char[]{';', '<', '>', '@', '[', '\\', '}', '_', '`', '~', '!', 13, 9, ',', ':', 10, '-', '.', '$', '/', '\"', '|', '*', '(', ')', '?', '{', '}', '\''};
    private static final int TEXT_COMPACTION_MODE_LATCH = 900;

    private enum Mode {
        ALPHA,
        LOWER,
        MIXED,
        PUNCT,
        ALPHA_SHIFT,
        PUNCT_SHIFT
    }

    static {
        EXP900[0] = BigInteger.ONE;
        BigInteger nineHundred = BigInteger.valueOf(900);
        EXP900[1] = nineHundred;
        for (int i = 2; i < EXP900.length; i++) {
            EXP900[i] = EXP900[i - 1].multiply(nineHundred);
        }
    }

    private DecodedBitStreamParser() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x004e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0048 A:{LOOP_END, LOOP:0: B:1:0x0012->B:18:0x0048} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static DecoderResult decode(int[] codewords, String ecLevel) throws FormatException {
        StringBuilder result = new StringBuilder(codewords.length * 2);
        int codeIndex = 1 + 1;
        int code = codewords[1];
        PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
        while (codeIndex < codewords[0]) {
            if (code != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                if (code != 928) {
                    switch (code) {
                        case 900:
                            codeIndex = textCompaction(codewords, codeIndex, result);
                            break;
                        case 901:
                            break;
                        case NUMERIC_COMPACTION_MODE_LATCH /*902*/:
                            codeIndex = numericCompaction(codewords, codeIndex, result);
                            break;
                        default:
                            switch (code) {
                                case MACRO_PDF417_TERMINATOR /*922*/:
                                case BEGIN_MACRO_PDF417_OPTIONAL_FIELD /*923*/:
                                    throw FormatException.getFormatInstance();
                                case BYTE_COMPACTION_MODE_LATCH_6 /*924*/:
                                    break;
                                default:
                                    codeIndex = textCompaction(codewords, codeIndex - 1, result);
                                    break;
                            }
                    }
                }
                codeIndex = decodeMacroBlock(codewords, codeIndex, resultMetadata);
                if (codeIndex >= codewords.length) {
                    int codeIndex2 = codeIndex + 1;
                    code = codewords[codeIndex];
                    codeIndex = codeIndex2;
                } else {
                    throw FormatException.getFormatInstance();
                }
            }
            codeIndex = byteCompaction(code, codewords, codeIndex, result);
            if (codeIndex >= codewords.length) {
            }
        }
        if (result.length() != 0) {
            DecoderResult decoderResult = new DecoderResult(null, result.toString(), null, ecLevel);
            decoderResult.setOther(resultMetadata);
            return decoderResult;
        }
        throw FormatException.getFormatInstance();
    }

    private static int decodeMacroBlock(int[] codewords, int codeIndex, PDF417ResultMetadata resultMetadata) throws FormatException {
        if (codeIndex + 2 <= codewords[0]) {
            int[] segmentIndexArray = new int[2];
            int codeIndex2 = codeIndex;
            codeIndex = 0;
            while (codeIndex < 2) {
                segmentIndexArray[codeIndex] = codewords[codeIndex2];
                codeIndex++;
                codeIndex2++;
            }
            resultMetadata.setSegmentIndex(Integer.parseInt(decodeBase900toBase10(segmentIndexArray, 2)));
            codeIndex = new StringBuilder();
            int codeIndex3 = textCompaction(codewords, codeIndex2, codeIndex);
            resultMetadata.setFileId(codeIndex.toString());
            if (codewords[codeIndex3] == BEGIN_MACRO_PDF417_OPTIONAL_FIELD) {
                codeIndex3++;
                int[] additionalOptionCodeWords = new int[(codewords[0] - codeIndex3)];
                int additionalOptionCodeWordsIndex = 0;
                int codeIndex4 = codeIndex3;
                codeIndex3 = 0;
                while (codeIndex4 < codewords[0] && codeIndex == 0) {
                    int codeIndex5 = codeIndex4 + 1;
                    codeIndex4 = codewords[codeIndex4];
                    if (codeIndex4 < 900) {
                        int additionalOptionCodeWordsIndex2 = additionalOptionCodeWordsIndex + 1;
                        additionalOptionCodeWords[additionalOptionCodeWordsIndex] = codeIndex4;
                        codeIndex4 = codeIndex5;
                        additionalOptionCodeWordsIndex = additionalOptionCodeWordsIndex2;
                    } else if (codeIndex4 == MACRO_PDF417_TERMINATOR) {
                        resultMetadata.setLastSegment(true);
                        codeIndex3 = 1;
                        codeIndex4 = codeIndex5 + 1;
                    } else {
                        throw FormatException.getFormatInstance();
                    }
                }
                resultMetadata.setOptionalData(Arrays.copyOf(additionalOptionCodeWords, additionalOptionCodeWordsIndex));
                return codeIndex4;
            } else if (codewords[codeIndex3] != MACRO_PDF417_TERMINATOR) {
                return codeIndex3;
            } else {
                resultMetadata.setLastSegment(true);
                return codeIndex3 + 1;
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static int textCompaction(int[] codewords, int codeIndex, StringBuilder result) {
        int[] textCompactionData = new int[((codewords[0] - codeIndex) << 1)];
        int[] byteCompactionData = new int[((codewords[0] - codeIndex) << 1)];
        int index = 0;
        int codeIndex2 = codeIndex;
        boolean end = false;
        while (codeIndex2 < codewords[0] && !end) {
            int codeIndex3 = codeIndex2 + 1;
            codeIndex2 = codewords[codeIndex2];
            if (codeIndex2 < 900) {
                textCompactionData[index] = codeIndex2 / 30;
                textCompactionData[index + 1] = codeIndex2 % 30;
                index += 2;
            } else if (codeIndex2 != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                if (codeIndex2 != 928) {
                    switch (codeIndex2) {
                        case 900:
                            int index2 = index + 1;
                            textCompactionData[index] = 900;
                            codeIndex2 = codeIndex3;
                            index = index2;
                            continue;
                        case 901:
                        case NUMERIC_COMPACTION_MODE_LATCH /*902*/:
                            break;
                        default:
                            switch (codeIndex2) {
                                case MACRO_PDF417_TERMINATOR /*922*/:
                                case BEGIN_MACRO_PDF417_OPTIONAL_FIELD /*923*/:
                                case BYTE_COMPACTION_MODE_LATCH_6 /*924*/:
                                    break;
                            }
                            break;
                    }
                }
                codeIndex3--;
                end = true;
            } else {
                textCompactionData[index] = MODE_SHIFT_TO_BYTE_COMPACTION_MODE;
                int codeIndex4 = codeIndex3 + 1;
                byteCompactionData[index] = codewords[codeIndex3];
                index++;
                codeIndex2 = codeIndex4;
            }
            codeIndex2 = codeIndex3;
            continue;
        }
        decodeTextCompaction(textCompactionData, byteCompactionData, index, result);
        return codeIndex2;
    }

    private static void decodeTextCompaction(int[] textCompactionData, int[] byteCompactionData, int length, StringBuilder result) {
        StringBuilder stringBuilder = result;
        Mode subMode = Mode.ALPHA;
        Mode priorToShiftMode = Mode.ALPHA;
        for (int i = 0; i < length; i++) {
            int subModeCh = textCompactionData[i];
            char ch = 0;
            switch (subMode) {
                case ALPHA:
                    if (subModeCh >= 26) {
                        if (subModeCh != 26) {
                            if (subModeCh != 27) {
                                if (subModeCh != 28) {
                                    if (subModeCh != 29) {
                                        if (subModeCh != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                                            if (subModeCh == 900) {
                                                subMode = Mode.ALPHA;
                                                break;
                                            }
                                        }
                                        stringBuilder.append((char) byteCompactionData[i]);
                                        break;
                                    }
                                    priorToShiftMode = subMode;
                                    subMode = Mode.PUNCT_SHIFT;
                                    break;
                                }
                                subMode = Mode.MIXED;
                                break;
                            }
                            subMode = Mode.LOWER;
                            break;
                        }
                        ch = ' ';
                        break;
                    }
                    ch = (char) (65 + subModeCh);
                    break;
                    break;
                case LOWER:
                    if (subModeCh >= 26) {
                        if (subModeCh != 26) {
                            if (subModeCh != 27) {
                                if (subModeCh != 28) {
                                    if (subModeCh != 29) {
                                        if (subModeCh != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                                            if (subModeCh == 900) {
                                                subMode = Mode.ALPHA;
                                                break;
                                            }
                                        }
                                        stringBuilder.append((char) byteCompactionData[i]);
                                        break;
                                    }
                                    priorToShiftMode = subMode;
                                    subMode = Mode.PUNCT_SHIFT;
                                    break;
                                }
                                subMode = Mode.MIXED;
                                break;
                            }
                            priorToShiftMode = subMode;
                            subMode = Mode.ALPHA_SHIFT;
                            break;
                        }
                        ch = ' ';
                        break;
                    }
                    ch = (char) (97 + subModeCh);
                    break;
                    break;
                case MIXED:
                    if (subModeCh >= 25) {
                        if (subModeCh != 25) {
                            if (subModeCh != 26) {
                                if (subModeCh != 27) {
                                    if (subModeCh != 28) {
                                        if (subModeCh != 29) {
                                            if (subModeCh != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                                                if (subModeCh == 900) {
                                                    subMode = Mode.ALPHA;
                                                    break;
                                                }
                                            }
                                            stringBuilder.append((char) byteCompactionData[i]);
                                            break;
                                        }
                                        priorToShiftMode = subMode;
                                        subMode = Mode.PUNCT_SHIFT;
                                        break;
                                    }
                                    subMode = Mode.ALPHA;
                                    break;
                                }
                                subMode = Mode.LOWER;
                                break;
                            }
                            ch = ' ';
                            break;
                        }
                        subMode = Mode.PUNCT;
                        break;
                    }
                    ch = MIXED_CHARS[subModeCh];
                    break;
                    break;
                case PUNCT:
                    if (subModeCh >= 29) {
                        if (subModeCh != 29) {
                            if (subModeCh != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                                if (subModeCh == 900) {
                                    subMode = Mode.ALPHA;
                                    break;
                                }
                            }
                            stringBuilder.append((char) byteCompactionData[i]);
                            break;
                        }
                        subMode = Mode.ALPHA;
                        break;
                    }
                    ch = PUNCT_CHARS[subModeCh];
                    break;
                    break;
                case ALPHA_SHIFT:
                    subMode = priorToShiftMode;
                    if (subModeCh >= 26) {
                        if (subModeCh != 26) {
                            if (subModeCh == 900) {
                                subMode = Mode.ALPHA;
                                break;
                            }
                        }
                        ch = ' ';
                        break;
                    }
                    ch = (char) (65 + subModeCh);
                    break;
                    break;
                case PUNCT_SHIFT:
                    subMode = priorToShiftMode;
                    if (subModeCh >= 29) {
                        if (subModeCh != 29) {
                            if (subModeCh != MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
                                if (subModeCh == 900) {
                                    subMode = Mode.ALPHA;
                                    break;
                                }
                            }
                            stringBuilder.append((char) byteCompactionData[i]);
                            break;
                        }
                        subMode = Mode.ALPHA;
                        break;
                    }
                    ch = PUNCT_CHARS[subModeCh];
                    break;
                    break;
            }
            if (ch != 0) {
                stringBuilder.append(ch);
            }
        }
    }

    /* JADX WARNING: Missing block: B:54:0x00d0, code skipped:
            if (r3 == MACRO_PDF417_TERMINATOR) goto L_0x00df;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int byteCompaction(int mode, int[] codewords, int codeIndex, StringBuilder result) {
        int i = mode;
        StringBuilder stringBuilder = result;
        int i2 = 928;
        int i3 = NUMERIC_COMPACTION_MODE_LATCH;
        long j = 900;
        int i4 = BYTE_COMPACTION_MODE_LATCH_6;
        int i5 = 901;
        int i6 = 0;
        int count;
        if (i == 901) {
            int count2 = 0;
            long value = 0;
            char[] decodedData = new char[6];
            int[] byteCompactedCodewords = new int[6];
            boolean end = false;
            int codeIndex2 = codeIndex + 1;
            int nextCode = codewords[codeIndex];
            int codeIndex3 = codeIndex2;
            while (codeIndex3 < codewords[0] && !end) {
                count = count2 + 1;
                byteCompactedCodewords[count2] = nextCode;
                value = (j * value) + ((long) nextCode);
                int codeIndex4 = codeIndex3 + 1;
                nextCode = codewords[codeIndex3];
                if (nextCode == 900 || nextCode == 901 || nextCode == i3 || nextCode == BYTE_COMPACTION_MODE_LATCH_6 || nextCode == i2 || nextCode == BEGIN_MACRO_PDF417_OPTIONAL_FIELD || nextCode == MACRO_PDF417_TERMINATOR) {
                    codeIndex3 = codeIndex4 - 1;
                    end = true;
                    count2 = count;
                } else {
                    if (count % 5 != 0 || count <= 0) {
                        count2 = count;
                    } else {
                        for (int j2 = 0; j2 < 6; j2++) {
                            decodedData[5 - j2] = (char) ((int) (value % 256));
                            value >>= 8;
                        }
                        stringBuilder.append(decodedData);
                        count2 = 0;
                    }
                    codeIndex3 = codeIndex4;
                }
                i2 = 928;
                i3 = NUMERIC_COMPACTION_MODE_LATCH;
                j = 900;
            }
            if (codeIndex3 != codewords[0] || nextCode >= 900) {
                count = count2;
            } else {
                count = count2 + 1;
                byteCompactedCodewords[count2] = nextCode;
            }
            while (true) {
                i2 = i6;
                if (i2 >= count) {
                    return codeIndex3;
                }
                stringBuilder.append((char) byteCompactedCodewords[i2]);
                i6 = i2 + 1;
            }
        } else if (i != BYTE_COMPACTION_MODE_LATCH_6) {
            return codeIndex;
        } else {
            long value2 = 0;
            boolean end2 = false;
            int count3 = 0;
            int codeIndex5 = codeIndex;
            while (codeIndex5 < codewords[0] && !end2) {
                i3 = codeIndex5 + 1;
                codeIndex5 = codewords[codeIndex5];
                if (codeIndex5 < 900) {
                    count3++;
                    value2 = (900 * value2) + ((long) codeIndex5);
                } else {
                    if (codeIndex5 != 900 && codeIndex5 != i5 && codeIndex5 != NUMERIC_COMPACTION_MODE_LATCH && codeIndex5 != i4) {
                        if (codeIndex5 != 928) {
                            if (codeIndex5 != BEGIN_MACRO_PDF417_OPTIONAL_FIELD) {
                            }
                            i3--;
                            end2 = true;
                        }
                    }
                    i3--;
                    end2 = true;
                }
                if (count3 % 5 == 0 && count3 > 0) {
                    char[] decodedData2 = new char[6];
                    long value3 = value2;
                    for (count = 0; count < 6; count++) {
                        decodedData2[5 - count] = (char) ((int) (value3 & 255));
                        value3 >>= 8;
                    }
                    stringBuilder.append(decodedData2);
                    count3 = 0;
                    value2 = value3;
                }
                codeIndex5 = i3;
                i4 = BYTE_COMPACTION_MODE_LATCH_6;
                i5 = 901;
            }
            return codeIndex5;
        }
    }

    private static int numericCompaction(int[] codewords, int codeIndex, StringBuilder result) throws FormatException {
        int count = 0;
        boolean end = false;
        int[] numericCodewords = new int[15];
        while (codeIndex < codewords[0] && !end) {
            int codeIndex2 = codeIndex + 1;
            codeIndex = codewords[codeIndex];
            if (codeIndex2 == codewords[0]) {
                end = true;
            }
            if (codeIndex < 900) {
                numericCodewords[count] = codeIndex;
                count++;
            } else if (codeIndex == 900 || codeIndex == 901 || codeIndex == BYTE_COMPACTION_MODE_LATCH_6 || codeIndex == 928 || codeIndex == BEGIN_MACRO_PDF417_OPTIONAL_FIELD || codeIndex == MACRO_PDF417_TERMINATOR) {
                codeIndex2--;
                end = true;
            }
            if (count % 15 == 0 || codeIndex == NUMERIC_COMPACTION_MODE_LATCH || end) {
                result.append(decodeBase900toBase10(numericCodewords, count));
                count = 0;
            }
            codeIndex = codeIndex2;
        }
        return codeIndex;
    }

    private static String decodeBase900toBase10(int[] codewords, int count) throws FormatException {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            result = result.add(EXP900[(count - i) - 1].multiply(BigInteger.valueOf((long) codewords[i])));
        }
        String resultString = result.toString();
        if (resultString.charAt(0) == '1') {
            return resultString.substring(1);
        }
        throw FormatException.getFormatInstance();
    }
}
