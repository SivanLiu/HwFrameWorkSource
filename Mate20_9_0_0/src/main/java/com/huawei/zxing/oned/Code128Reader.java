package com.huawei.zxing.oned;

import com.huawei.lcagent.client.MetricConstant;
import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.ChecksumException;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Code128Reader extends OneDReader {
    private static final int CODE_CODE_A = 101;
    private static final int CODE_CODE_B = 100;
    private static final int CODE_CODE_C = 99;
    private static final int CODE_FNC_1 = 102;
    private static final int CODE_FNC_2 = 97;
    private static final int CODE_FNC_3 = 96;
    private static final int CODE_FNC_4_A = 101;
    private static final int CODE_FNC_4_B = 100;
    static final int[][] CODE_PATTERNS;
    private static final int CODE_SHIFT = 98;
    private static final int CODE_START_A = 103;
    private static final int CODE_START_B = 104;
    private static final int CODE_START_C = 105;
    private static final int CODE_STOP = 106;
    private static final int MAX_AVG_VARIANCE = 64;
    private static final int MAX_INDIVIDUAL_VARIANCE = 179;

    static {
        int[][] iArr = new int[MetricConstant.BLUETOOTH_METRIC_ID_EX][];
        iArr[0] = new int[]{2, 1, 2, 2, 2, 2};
        iArr[1] = new int[]{2, 2, 2, 1, 2, 2};
        iArr[2] = new int[]{2, 2, 2, 2, 2, 1};
        iArr[3] = new int[]{1, 2, 1, 2, 2, 3};
        iArr[4] = new int[]{1, 2, 1, 3, 2, 2};
        iArr[5] = new int[]{1, 3, 1, 2, 2, 2};
        iArr[6] = new int[]{1, 2, 2, 2, 1, 3};
        iArr[7] = new int[]{1, 2, 2, 3, 1, 2};
        iArr[8] = new int[]{1, 3, 2, 2, 1, 2};
        iArr[9] = new int[]{2, 2, 1, 2, 1, 3};
        iArr[10] = new int[]{2, 2, 1, 3, 1, 2};
        iArr[11] = new int[]{2, 3, 1, 2, 1, 2};
        iArr[12] = new int[]{1, 1, 2, 2, 3, 2};
        iArr[13] = new int[]{1, 2, 2, 1, 3, 2};
        iArr[14] = new int[]{1, 2, 2, 2, 3, 1};
        iArr[15] = new int[]{1, 1, 3, 2, 2, 2};
        iArr[16] = new int[]{1, 2, 3, 1, 2, 2};
        iArr[17] = new int[]{1, 2, 3, 2, 2, 1};
        iArr[18] = new int[]{2, 2, 3, 2, 1, 1};
        iArr[19] = new int[]{2, 2, 1, 1, 3, 2};
        iArr[20] = new int[]{2, 2, 1, 2, 3, 1};
        iArr[21] = new int[]{2, 1, 3, 2, 1, 2};
        iArr[22] = new int[]{2, 2, 3, 1, 1, 2};
        iArr[23] = new int[]{3, 1, 2, 1, 3, 1};
        iArr[24] = new int[]{3, 1, 1, 2, 2, 2};
        iArr[25] = new int[]{3, 2, 1, 1, 2, 2};
        iArr[26] = new int[]{3, 2, 1, 2, 2, 1};
        iArr[27] = new int[]{3, 1, 2, 2, 1, 2};
        iArr[28] = new int[]{3, 2, 2, 1, 1, 2};
        iArr[29] = new int[]{3, 2, 2, 2, 1, 1};
        iArr[30] = new int[]{2, 1, 2, 1, 2, 3};
        iArr[31] = new int[]{2, 1, 2, 3, 2, 1};
        iArr[32] = new int[]{2, 3, 2, 1, 2, 1};
        iArr[33] = new int[]{1, 1, 1, 3, 2, 3};
        iArr[34] = new int[]{1, 3, 1, 1, 2, 3};
        iArr[35] = new int[]{1, 3, 1, 3, 2, 1};
        iArr[36] = new int[]{1, 1, 2, 3, 1, 3};
        iArr[37] = new int[]{1, 3, 2, 1, 1, 3};
        iArr[38] = new int[]{1, 3, 2, 3, 1, 1};
        iArr[39] = new int[]{2, 1, 1, 3, 1, 3};
        iArr[40] = new int[]{2, 3, 1, 1, 1, 3};
        iArr[41] = new int[]{2, 3, 1, 3, 1, 1};
        iArr[42] = new int[]{1, 1, 2, 1, 3, 3};
        iArr[43] = new int[]{1, 1, 2, 3, 3, 1};
        iArr[44] = new int[]{1, 3, 2, 1, 3, 1};
        iArr[45] = new int[]{1, 1, 3, 1, 2, 3};
        iArr[46] = new int[]{1, 1, 3, 3, 2, 1};
        iArr[47] = new int[]{1, 3, 3, 1, 2, 1};
        iArr[48] = new int[]{3, 1, 3, 1, 2, 1};
        iArr[49] = new int[]{2, 1, 1, 3, 3, 1};
        iArr[50] = new int[]{2, 3, 1, 1, 3, 1};
        iArr[51] = new int[]{2, 1, 3, 1, 1, 3};
        iArr[52] = new int[]{2, 1, 3, 3, 1, 1};
        iArr[53] = new int[]{2, 1, 3, 1, 3, 1};
        iArr[54] = new int[]{3, 1, 1, 1, 2, 3};
        iArr[55] = new int[]{3, 1, 1, 3, 2, 1};
        iArr[56] = new int[]{3, 3, 1, 1, 2, 1};
        iArr[57] = new int[]{3, 1, 2, 1, 1, 3};
        iArr[58] = new int[]{3, 1, 2, 3, 1, 1};
        iArr[59] = new int[]{3, 3, 2, 1, 1, 1};
        iArr[60] = new int[]{3, 1, 4, 1, 1, 1};
        iArr[61] = new int[]{2, 2, 1, 4, 1, 1};
        iArr[62] = new int[]{4, 3, 1, 1, 1, 1};
        iArr[63] = new int[]{1, 1, 1, 2, 2, 4};
        iArr[64] = new int[]{1, 1, 1, 4, 2, 2};
        iArr[65] = new int[]{1, 2, 1, 1, 2, 4};
        iArr[66] = new int[]{1, 2, 1, 4, 2, 1};
        iArr[67] = new int[]{1, 4, 1, 1, 2, 2};
        iArr[68] = new int[]{1, 4, 1, 2, 2, 1};
        iArr[69] = new int[]{1, 1, 2, 2, 1, 4};
        iArr[70] = new int[]{1, 1, 2, 4, 1, 2};
        iArr[71] = new int[]{1, 2, 2, 1, 1, 4};
        iArr[72] = new int[]{1, 2, 2, 4, 1, 1};
        iArr[73] = new int[]{1, 4, 2, 1, 1, 2};
        iArr[74] = new int[]{1, 4, 2, 2, 1, 1};
        iArr[75] = new int[]{2, 4, 1, 2, 1, 1};
        iArr[76] = new int[]{2, 2, 1, 1, 1, 4};
        iArr[77] = new int[]{4, 1, 3, 1, 1, 1};
        iArr[78] = new int[]{2, 4, 1, 1, 1, 2};
        iArr[79] = new int[]{1, 3, 4, 1, 1, 1};
        iArr[80] = new int[]{1, 1, 1, 2, 4, 2};
        iArr[81] = new int[]{1, 2, 1, 1, 4, 2};
        iArr[82] = new int[]{1, 2, 1, 2, 4, 1};
        iArr[83] = new int[]{1, 1, 4, 2, 1, 2};
        iArr[84] = new int[]{1, 2, 4, 1, 1, 2};
        iArr[85] = new int[]{1, 2, 4, 2, 1, 1};
        iArr[86] = new int[]{4, 1, 1, 2, 1, 2};
        iArr[87] = new int[]{4, 2, 1, 1, 1, 2};
        iArr[88] = new int[]{4, 2, 1, 2, 1, 1};
        iArr[89] = new int[]{2, 1, 2, 1, 4, 1};
        iArr[90] = new int[]{2, 1, 4, 1, 2, 1};
        iArr[91] = new int[]{4, 1, 2, 1, 2, 1};
        iArr[92] = new int[]{1, 1, 1, 1, 4, 3};
        iArr[93] = new int[]{1, 1, 1, 3, 4, 1};
        iArr[94] = new int[]{1, 3, 1, 1, 4, 1};
        iArr[95] = new int[]{1, 1, 4, 1, 1, 3};
        iArr[CODE_FNC_3] = new int[]{1, 1, 4, 3, 1, 1};
        iArr[CODE_FNC_2] = new int[]{4, 1, 1, 1, 1, 3};
        iArr[CODE_SHIFT] = new int[]{4, 1, 1, 3, 1, 1};
        iArr[99] = new int[]{1, 1, 3, 1, 4, 1};
        iArr[100] = new int[]{1, 1, 4, 1, 3, 1};
        iArr[101] = new int[]{3, 1, 1, 1, 4, 1};
        iArr[102] = new int[]{4, 1, 1, 1, 3, 1};
        iArr[103] = new int[]{2, 1, 1, 4, 1, 2};
        iArr[104] = new int[]{2, 1, 1, 2, 1, 4};
        iArr[105] = new int[]{2, 1, 1, 2, 3, 2};
        iArr[106] = new int[]{2, 3, 3, 1, 1, 1, 2};
        CODE_PATTERNS = iArr;
    }

    private static int[] findStartPattern(BitArray row) throws NotFoundException {
        int width = row.getSize();
        int rowOffset = row.getNextSet(0);
        int[] counters = new int[6];
        int patternStart = rowOffset;
        boolean isWhite = false;
        int patternLength = counters.length;
        int patternStart2 = patternStart;
        patternStart = 0;
        int i = rowOffset;
        while (i < width) {
            boolean z = true;
            if ((row.get(i) ^ isWhite) != 0) {
                counters[patternStart] = counters[patternStart] + 1;
            } else {
                if (patternStart == patternLength - 1) {
                    int bestVariance = 64;
                    int bestMatch = -1;
                    for (int startCode = 103; startCode <= 105; startCode++) {
                        int variance = OneDReader.patternMatchVariance(counters, CODE_PATTERNS[startCode], MAX_INDIVIDUAL_VARIANCE);
                        if (variance < bestVariance) {
                            bestVariance = variance;
                            bestMatch = startCode;
                        }
                    }
                    if (bestMatch < 0 || !row.isRange(Math.max(0, patternStart2 - ((i - patternStart2) / 2)), patternStart2, false)) {
                        patternStart2 += counters[0] + counters[1];
                        System.arraycopy(counters, 2, counters, 0, patternLength - 2);
                        counters[patternLength - 2] = 0;
                        counters[patternLength - 1] = 0;
                        patternStart--;
                    } else {
                        return new int[]{patternStart2, i, bestMatch};
                    }
                }
                patternStart++;
                counters[patternStart] = 1;
                if (isWhite) {
                    z = false;
                }
                isWhite = z;
            }
            i++;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static int decodeCode(BitArray row, int[] counters, int rowOffset) throws NotFoundException {
        OneDReader.recordPattern(row, rowOffset, counters);
        int bestVariance = 64;
        int bestMatch = -1;
        for (int d = 0; d < CODE_PATTERNS.length; d++) {
            int variance = OneDReader.patternMatchVariance(counters, CODE_PATTERNS[d], MAX_INDIVIDUAL_VARIANCE);
            if (variance < bestVariance) {
                bestVariance = variance;
                bestMatch = d;
            }
        }
        if (bestMatch >= 0) {
            return bestMatch;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException, ChecksumException {
        int codeSet;
        int code;
        int i = rowNumber;
        BitArray bitArray = row;
        Map<DecodeHintType, ?> map = hints;
        boolean convertFNC1 = map != null && map.containsKey(DecodeHintType.ASSUME_GS1);
        int[] startPatternInfo = findStartPattern(row);
        int startCode = startPatternInfo[2];
        List<Byte> rawCodes = new ArrayList(20);
        rawCodes.add(Byte.valueOf((byte) startCode));
        switch (startCode) {
            case 103:
                codeSet = 101;
                break;
            case 104:
                codeSet = 100;
                break;
            case 105:
                codeSet = 99;
                break;
            default:
                throw FormatException.getFormatInstance();
        }
        boolean done = false;
        boolean isNextShifted = false;
        StringBuilder result = new StringBuilder(20);
        int lastStart = startPatternInfo[0];
        int nextStart = startPatternInfo[1];
        int[] counters = new int[6];
        int code2 = 0;
        int checksumTotal = startCode;
        int multiplier = 0;
        int codeSet2 = codeSet;
        int lastStart2 = lastStart;
        lastStart = 0;
        boolean lastCharacterWasPrintable = true;
        while (!done) {
            boolean unshift = isNextShifted;
            isNextShifted = false;
            lastStart = code2;
            code = decodeCode(bitArray, counters, nextStart);
            int startCode2 = startCode;
            rawCodes.add(Byte.valueOf((byte) code));
            if (code != 106) {
                lastCharacterWasPrintable = true;
            }
            if (code != 106) {
                multiplier++;
                checksumTotal += multiplier * code;
            }
            lastStart2 = nextStart;
            code2 = nextStart;
            for (int counter : counters) {
                code2 += counter;
            }
            switch (code) {
                case 103:
                case 104:
                case 105:
                    throw FormatException.getFormatInstance();
                default:
                    switch (codeSet2) {
                        case 99:
                            startCode = 100;
                            if (code >= 100) {
                                if (code != 106) {
                                    lastCharacterWasPrintable = false;
                                }
                                if (code == 106) {
                                    done = true;
                                    break;
                                }
                                switch (code) {
                                    case 100:
                                        codeSet2 = 100;
                                        break;
                                    case 101:
                                        codeSet2 = 101;
                                        break;
                                    case 102:
                                        if (convertFNC1) {
                                            if (result.length() != 0) {
                                                result.append(29);
                                                break;
                                            }
                                            result.append("]C1");
                                            break;
                                        }
                                        break;
                                }
                            }
                            if (code < 10) {
                                result.append('0');
                            }
                            result.append(code);
                            break;
                            break;
                        case 100:
                            if (code >= CODE_FNC_3) {
                                if (code != 106) {
                                    lastCharacterWasPrintable = false;
                                }
                                if (code == 106) {
                                    done = true;
                                    break;
                                }
                                switch (code) {
                                    case CODE_SHIFT /*98*/:
                                        isNextShifted = true;
                                        codeSet2 = 101;
                                        break;
                                    case 99:
                                        codeSet2 = 99;
                                        break;
                                    case 101:
                                        codeSet2 = 101;
                                        break;
                                    case 102:
                                        if (convertFNC1) {
                                            if (result.length() != 0) {
                                                result.append(29);
                                                break;
                                            }
                                            result.append("]C1");
                                            break;
                                        }
                                        break;
                                }
                            }
                            result.append((char) (32 + code));
                            break;
                            break;
                        case 101:
                            if (code >= 64) {
                                if (code >= CODE_FNC_3) {
                                    if (code != 106) {
                                        lastCharacterWasPrintable = false;
                                    }
                                    if (code == 106) {
                                        done = true;
                                        break;
                                    }
                                    switch (code) {
                                        case CODE_SHIFT /*98*/:
                                            isNextShifted = true;
                                            codeSet2 = 100;
                                            break;
                                        case 99:
                                            codeSet2 = 99;
                                            break;
                                        case 100:
                                            codeSet2 = 100;
                                            break;
                                        case 102:
                                            if (convertFNC1) {
                                                if (result.length() != 0) {
                                                    result.append(29);
                                                    break;
                                                }
                                                result.append("]C1");
                                                break;
                                            }
                                            break;
                                    }
                                }
                                result.append((char) (code - 64));
                                break;
                            }
                            result.append((char) (32 + code));
                            break;
                            break;
                    }
                    startCode = 100;
                    if (unshift) {
                        nextStart = 101;
                        if (codeSet2 == 101) {
                            nextStart = startCode;
                        }
                        codeSet2 = nextStart;
                    }
                    nextStart = code2;
                    startCode = startCode2;
                    code2 = code;
                    code = hints;
            }
        }
        code = nextStart - lastStart2;
        startCode = bitArray.getNextUnset(nextStart);
        int i2;
        boolean z;
        int[] iArr;
        int i3;
        if (!bitArray.isRange(startCode, Math.min(row.getSize(), startCode + ((startCode - lastStart2) / 2)), false)) {
            i2 = lastStart2;
            z = convertFNC1;
            iArr = startPatternInfo;
            i3 = codeSet2;
            throw NotFoundException.getNotFoundInstance();
        } else if ((checksumTotal - (multiplier * lastStart)) % 103 == lastStart) {
            int resultLength = result.length();
            int resultLength2;
            if (resultLength != 0) {
                if (resultLength > 0 && lastCharacterWasPrintable) {
                    if (codeSet2 == 99) {
                        result.delete(resultLength - 2, resultLength);
                    } else {
                        result.delete(resultLength - 1, resultLength);
                    }
                }
                float left = ((float) (startPatternInfo[1] + startPatternInfo[0])) / 2.0f;
                float right = ((float) lastStart2) + (((float) code) / 2.0f);
                lastStart2 = rawCodes.size();
                code = new byte[lastStart2];
                int i4 = 0;
                while (true) {
                    resultLength2 = resultLength;
                    resultLength = i4;
                    if (resultLength < lastStart2) {
                        int rawCodesSize = lastStart2;
                        code[resultLength] = ((Byte) rawCodes.get(resultLength)).byteValue();
                        i4 = resultLength + 1;
                        resultLength = resultLength2;
                        lastStart2 = rawCodesSize;
                    } else {
                        resultLength = result.toString();
                        z = convertFNC1;
                        ResultPoint[] resultPointArr = new ResultPoint[true];
                        iArr = startPatternInfo;
                        resultPointArr[0] = new ResultPoint(left, (float) i);
                        resultPointArr[1] = new ResultPoint(right, (float) i);
                        return new Result(resultLength, code, resultPointArr, BarcodeFormat.CODE_128);
                    }
                }
            }
            i2 = lastStart2;
            resultLength2 = resultLength;
            z = convertFNC1;
            iArr = startPatternInfo;
            i3 = codeSet2;
            throw NotFoundException.getNotFoundInstance();
        } else {
            i2 = lastStart2;
            z = convertFNC1;
            iArr = startPatternInfo;
            i3 = codeSet2;
            throw ChecksumException.getChecksumInstance();
        }
    }
}
