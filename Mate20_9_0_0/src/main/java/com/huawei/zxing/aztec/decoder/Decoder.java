package com.huawei.zxing.aztec.decoder;

import com.huawei.android.smcs.SmartTrimProcessEvent;
import com.huawei.internal.telephony.PhoneConstantsEx;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.aztec.AztecDetectorResult;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.common.DecoderResult;
import com.huawei.zxing.common.reedsolomon.GenericGF;
import com.huawei.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.huawei.zxing.common.reedsolomon.ReedSolomonException;
import java.util.Arrays;

public final class Decoder {
    private static final String[] DIGIT_TABLE = new String[]{"CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", SmartTrimProcessEvent.ST_EVENT_STRING_TOKEN, ".", "CTRL_UL", "CTRL_US"};
    private static final String[] LOWER_TABLE = new String[]{"CTRL_PS", " ", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "CTRL_US", "CTRL_ML", "CTRL_DL", "CTRL_BS"};
    private static final String[] MIXED_TABLE = new String[]{"CTRL_PS", " ", "\u0001", "\u0002", "\u0003", "\u0004", "\u0005", "\u0006", "\u0007", "\b", "\t", "\n", "\u000b", "\f", "\r", "\u001b", "\u001c", "\u001d", "\u001e", "\u001f", "@", "\\", "^", "_", "`", "|", "~", "", "CTRL_LL", "CTRL_UL", "CTRL_PL", "CTRL_BS"};
    private static final String[] PUNCT_TABLE = new String[]{"", "\r", "\r\n", ". ", ", ", ": ", "!", "\"", "#", "$", "%", "&", "'", "(", ")", PhoneConstantsEx.APN_TYPE_ALL, "+", SmartTrimProcessEvent.ST_EVENT_STRING_TOKEN, "-", ".", "/", ":", SmartTrimProcessEvent.ST_EVENT_INTER_STRING_TOKEN, "<", "=", ">", "?", "[", "]", "{", "}", "CTRL_UL"};
    private static final String[] UPPER_TABLE = new String[]{"CTRL_PS", " ", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "CTRL_LL", "CTRL_ML", "CTRL_DL", "CTRL_BS"};
    private AztecDetectorResult ddata;

    private enum Table {
        UPPER,
        LOWER,
        MIXED,
        DIGIT,
        PUNCT,
        BINARY
    }

    public DecoderResult decode(AztecDetectorResult detectorResult) throws FormatException {
        this.ddata = detectorResult;
        return new DecoderResult(null, getEncodedData(correctBits(extractBits(detectorResult.getBits()))), null, null);
    }

    public static String highLevelDecode(boolean[] correctedBits) {
        return getEncodedData(correctedBits);
    }

    private static String getEncodedData(boolean[] correctedBits) {
        int endIndex = correctedBits.length;
        Table latchTable = Table.UPPER;
        Table shiftTable = Table.UPPER;
        StringBuilder result = new StringBuilder(20);
        Table latchTable2 = latchTable;
        int index = 0;
        while (index < endIndex) {
            int size;
            if (shiftTable != Table.BINARY) {
                size = shiftTable == Table.DIGIT ? 4 : 5;
                if (endIndex - index < size) {
                    break;
                }
                int code = readCode(correctedBits, index, size);
                index += size;
                String str = getCharacter(shiftTable, code);
                if (str.startsWith("CTRL_")) {
                    shiftTable = getTable(str.charAt(5));
                    if (str.charAt(6) == 'L') {
                        latchTable2 = shiftTable;
                    }
                } else {
                    result.append(str);
                    shiftTable = latchTable2;
                }
            } else if (endIndex - index < 5) {
                break;
            } else {
                size = readCode(correctedBits, index, 5);
                index += 5;
                if (size == 0) {
                    if (endIndex - index < 11) {
                        break;
                    }
                    size = readCode(correctedBits, index, 11) + 31;
                    index += 11;
                }
                int index2 = index;
                for (index = 0; index < size; index++) {
                    if (endIndex - index2 < 8) {
                        index2 = endIndex;
                        break;
                    }
                    result.append((char) readCode(correctedBits, index2, 8));
                    index2 += 8;
                }
                index = index2;
                shiftTable = latchTable2;
            }
        }
        return result.toString();
    }

    private static Table getTable(char t) {
        if (t == 'B') {
            return Table.BINARY;
        }
        if (t == 'D') {
            return Table.DIGIT;
        }
        if (t == 'P') {
            return Table.PUNCT;
        }
        switch (t) {
            case 'L':
                return Table.LOWER;
            case 'M':
                return Table.MIXED;
            default:
                return Table.UPPER;
        }
    }

    private static String getCharacter(Table table, int code) {
        switch (table) {
            case UPPER:
                return UPPER_TABLE[code];
            case LOWER:
                return LOWER_TABLE[code];
            case MIXED:
                return MIXED_TABLE[code];
            case PUNCT:
                return PUNCT_TABLE[code];
            case DIGIT:
                return DIGIT_TABLE[code];
            default:
                throw new IllegalStateException("Bad table");
        }
    }

    private boolean[] correctBits(boolean[] rawbits) throws FormatException {
        int codewordSize;
        GenericGF gf;
        boolean[] zArr = rawbits;
        if (this.ddata.getNbLayers() <= 2) {
            codewordSize = 6;
            gf = GenericGF.AZTEC_DATA_6;
        } else if (this.ddata.getNbLayers() <= 8) {
            codewordSize = 8;
            gf = GenericGF.AZTEC_DATA_8;
        } else if (this.ddata.getNbLayers() <= 22) {
            codewordSize = 10;
            gf = GenericGF.AZTEC_DATA_10;
        } else {
            codewordSize = 12;
            gf = GenericGF.AZTEC_DATA_12;
        }
        int codewordSize2 = codewordSize;
        int numDataCodewords = this.ddata.getNbDatablocks();
        int numCodewords = zArr.length / codewordSize2;
        int numECCodewords = numCodewords - numDataCodewords;
        int[] dataWords = new int[numCodewords];
        int offset = zArr.length % codewordSize2;
        codewordSize = 0;
        while (codewordSize < numCodewords) {
            dataWords[codewordSize] = readCode(zArr, offset, codewordSize2);
            codewordSize++;
            offset += codewordSize2;
        }
        try {
            int dataWord;
            new ReedSolomonDecoder(gf).decode(dataWords, numECCodewords);
            codewordSize = 1;
            int mask = (1 << codewordSize2) - 1;
            int stuffedBits = 0;
            for (int i = 0; i < numDataCodewords; i++) {
                dataWord = dataWords[i];
                if (dataWord == 0 || dataWord == mask) {
                    throw FormatException.getFormatInstance();
                }
                if (dataWord == 1 || dataWord == mask - 1) {
                    stuffedBits++;
                }
            }
            boolean[] correctedBits = new boolean[((numDataCodewords * codewordSize2) - stuffedBits)];
            int index = 0;
            dataWord = 0;
            while (dataWord < numDataCodewords) {
                int dataWord2 = dataWords[dataWord];
                if (dataWord2 == codewordSize || dataWord2 == mask - 1) {
                    boolean z = true;
                    codewordSize = (index + codewordSize2) - 1;
                    if (dataWord2 <= 1) {
                        z = false;
                    }
                    Arrays.fill(correctedBits, index, codewordSize, z);
                    index += codewordSize2 - 1;
                } else {
                    codewordSize = codewordSize2 - 1;
                    while (codewordSize >= 0) {
                        int index2 = index + 1;
                        correctedBits[index] = (dataWord2 & (1 << codewordSize)) != 0;
                        codewordSize--;
                        index = index2;
                    }
                }
                dataWord++;
                codewordSize = 1;
            }
            return correctedBits;
        } catch (ReedSolomonException e) {
            throw FormatException.getFormatInstance();
        }
    }

    boolean[] extractBits(BitMatrix matrix) {
        int i;
        int origCenter;
        int i2;
        int newOffset;
        BitMatrix bitMatrix = matrix;
        boolean compact = this.ddata.isCompact();
        int layers = this.ddata.getNbLayers();
        int baseMatrixSize = (compact ? 11 : 14) + (layers * 4);
        int[] alignmentMap = new int[baseMatrixSize];
        boolean[] rawbits = new boolean[totalBitsInLayer(layers, compact)];
        int i3 = 2;
        if (compact) {
            for (i = 0; i < alignmentMap.length; i++) {
                alignmentMap[i] = i;
            }
        } else {
            origCenter = baseMatrixSize / 2;
            int center = ((baseMatrixSize + 1) + ((((baseMatrixSize / 2) - 1) / 15) * 2)) / 2;
            for (i2 = 0; i2 < origCenter; i2++) {
                newOffset = (i2 / 15) + i2;
                alignmentMap[(origCenter - i2) - 1] = (center - newOffset) - 1;
                alignmentMap[origCenter + i2] = (center + newOffset) + 1;
            }
        }
        origCenter = 0;
        i = 0;
        while (origCenter < layers) {
            i2 = compact ? ((layers - origCenter) * 4) + 9 : ((layers - origCenter) * 4) + 12;
            newOffset = origCenter * 2;
            int high = (baseMatrixSize - 1) - newOffset;
            int j = 0;
            while (j < i2) {
                int columnOffset = j * 2;
                int k = 0;
                while (true) {
                    int k2 = k;
                    if (k2 >= i3) {
                        break;
                    }
                    rawbits[(i + columnOffset) + k2] = bitMatrix.get(alignmentMap[newOffset + k2], alignmentMap[newOffset + j]);
                    rawbits[(((2 * i2) + i) + columnOffset) + k2] = bitMatrix.get(alignmentMap[newOffset + j], alignmentMap[high - k2]);
                    rawbits[(((4 * i2) + i) + columnOffset) + k2] = bitMatrix.get(alignmentMap[high - k2], alignmentMap[high - j]);
                    rawbits[(((6 * i2) + i) + columnOffset) + k2] = bitMatrix.get(alignmentMap[high - j], alignmentMap[newOffset + k2]);
                    k = k2 + 1;
                    i3 = 2;
                }
                j++;
                i3 = 2;
            }
            i += i2 * 8;
            origCenter++;
            i3 = 2;
        }
        return rawbits;
    }

    private static int readCode(boolean[] rawbits, int startIndex, int length) {
        int res = 0;
        for (int i = startIndex; i < startIndex + length; i++) {
            res <<= 1;
            if (rawbits[i]) {
                res++;
            }
        }
        return res;
    }

    private static int totalBitsInLayer(int layers, boolean compact) {
        return ((compact ? 88 : 112) + (16 * layers)) * layers;
    }
}
