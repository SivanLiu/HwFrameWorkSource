package com.huawei.zxing.aztec.encoder;

import com.huawei.zxing.common.BitArray;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.common.reedsolomon.GenericGF;
import com.huawei.zxing.common.reedsolomon.ReedSolomonEncoder;

public final class Encoder {
    public static final int DEFAULT_AZTEC_LAYERS = 0;
    public static final int DEFAULT_EC_PERCENT = 33;
    private static final int MAX_NB_BITS = 32;
    private static final int MAX_NB_BITS_COMPACT = 4;
    private static final int[] WORD_SIZE = new int[]{4, 6, 6, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12};

    private Encoder() {
    }

    public static AztecCode encode(byte[] data) {
        return encode(data, 33, 0);
    }

    public static AztecCode encode(byte[] data, int minECCPercent, int userSpecifiedLayers) {
        int wordSize;
        int usableBitsInLayers;
        BitArray stuffedBits;
        boolean compact;
        int wordSize2;
        BitArray stuffedBits2;
        int i;
        BitArray bits;
        int totalSizeBits;
        int matrixSize;
        int origCenter;
        int i2;
        int rowSize;
        int wordSize3;
        int totalBitsInLayer;
        BitArray stuffedBits3;
        BitArray bits2 = new HighLevelEncoder(data).encode();
        int eccBits = ((bits2.getSize() * minECCPercent) / 100) + 11;
        int totalSizeBits2 = bits2.getSize() + eccBits;
        int i3 = 32;
        boolean z = false;
        boolean z2 = true;
        if (userSpecifiedLayers != 0) {
            boolean compact2 = userSpecifiedLayers < 0;
            int layers = Math.abs(userSpecifiedLayers);
            if (compact2) {
                i3 = 4;
            }
            if (layers <= i3) {
                i3 = totalBitsInLayer(layers, compact2);
                wordSize = WORD_SIZE[layers];
                usableBitsInLayers = i3 - (i3 % wordSize);
                stuffedBits = stuffBits(bits2, wordSize);
                if (stuffedBits.getSize() + eccBits > usableBitsInLayers) {
                    throw new IllegalArgumentException("Data to large for user specified layer");
                } else if (!compact2 || stuffedBits.getSize() <= wordSize * 64) {
                    usableBitsInLayers = i3;
                    int i4 = layers;
                    compact = compact2;
                    wordSize2 = wordSize;
                    wordSize = i4;
                } else {
                    throw new IllegalArgumentException("Data to large for user specified layer");
                }
            }
            throw new IllegalArgumentException(String.format("Illegal value %s for layers", new Object[]{Integer.valueOf(userSpecifiedLayers)}));
        }
        int eccBits2;
        stuffedBits2 = null;
        wordSize2 = 0;
        i = 0;
        while (i <= i3) {
            boolean totalSizeBits3;
            compact = i <= 3 ? z2 : z;
            wordSize = compact ? i + 1 : i;
            usableBitsInLayers = totalBitsInLayer(wordSize, compact);
            if (totalSizeBits2 > usableBitsInLayers) {
                bits = bits2;
                eccBits2 = eccBits;
                totalSizeBits = totalSizeBits2;
                totalSizeBits3 = z2;
            } else {
                if (wordSize2 != WORD_SIZE[wordSize]) {
                    wordSize2 = WORD_SIZE[wordSize];
                    stuffedBits2 = stuffBits(bits2, wordSize2);
                }
                stuffedBits = stuffedBits2;
                int usableBitsInLayers2 = usableBitsInLayers - (usableBitsInLayers % wordSize2);
                if (compact && stuffedBits.getSize() > wordSize2 * 64) {
                    bits = bits2;
                    eccBits2 = eccBits;
                    totalSizeBits = totalSizeBits2;
                    totalSizeBits3 = z2;
                } else if (stuffedBits.getSize() + eccBits > usableBitsInLayers2) {
                    bits = bits2;
                    eccBits2 = eccBits;
                    totalSizeBits = totalSizeBits2;
                    totalSizeBits3 = z2;
                }
                stuffedBits2 = stuffedBits;
            }
            i++;
            z2 = totalSizeBits3;
            bits2 = bits;
            eccBits = eccBits2;
            totalSizeBits2 = totalSizeBits;
            byte[] bArr = data;
            i3 = 32;
            z = false;
        }
        eccBits2 = eccBits;
        totalSizeBits = totalSizeBits2;
        throw new IllegalArgumentException("Data too large for an Aztec code");
        i = generateCheckWords(stuffedBits, usableBitsInLayers, wordSize2);
        i3 = stuffedBits.getSize() / wordSize2;
        stuffedBits2 = generateModeMessage(compact, wordSize, i3);
        int baseMatrixSize = compact ? 11 + (wordSize * 4) : 14 + (wordSize * 4);
        int[] alignmentMap = new int[baseMatrixSize];
        int i5;
        if (!compact) {
            matrixSize = (baseMatrixSize + 1) + ((((baseMatrixSize / 2) - 1) / 15) * 2);
            origCenter = baseMatrixSize / 2;
            bits2 = matrixSize / 2;
            i5 = 0;
            while (true) {
                i2 = i5;
                if (i2 >= origCenter) {
                    break;
                }
                int newOffset = i2 + (i2 / 15);
                alignmentMap[(origCenter - i2) - 1] = (bits2 - newOffset) - 1;
                alignmentMap[origCenter + i2] = (bits2 + newOffset) + 1;
                i5 = i2 + 1;
                i2 = data;
            }
        } else {
            matrixSize = baseMatrixSize;
            i5 = 0;
            while (true) {
                bits = bits2;
                bits2 = i5;
                if (bits2 >= alignmentMap.length) {
                    break;
                }
                alignmentMap[bits2] = bits2;
                i5 = bits2 + 1;
                bits2 = bits;
            }
        }
        origCenter = matrixSize;
        bits2 = new BitMatrix(origCenter);
        eccBits = 0;
        i2 = 0;
        while (eccBits < wordSize) {
            int rowSize2 = compact ? ((wordSize - eccBits) * 4) + 9 : ((wordSize - eccBits) * 4) + 12;
            matrixSize = 0;
            while (true) {
                totalSizeBits = totalSizeBits2;
                rowSize = rowSize2;
                totalSizeBits2 = matrixSize;
                if (totalSizeBits2 >= rowSize) {
                    break;
                }
                int columnOffset = totalSizeBits2 * 2;
                matrixSize = 0;
                while (true) {
                    wordSize3 = wordSize2;
                    totalBitsInLayer = usableBitsInLayers;
                    wordSize2 = matrixSize;
                    if (wordSize2 >= 2) {
                        break;
                    }
                    if (i.get((i2 + columnOffset) + wordSize2)) {
                        stuffedBits3 = stuffedBits;
                        bits2.set(alignmentMap[(eccBits * 2) + wordSize2], alignmentMap[(eccBits * 2) + totalSizeBits2]);
                    } else {
                        stuffedBits3 = stuffedBits;
                    }
                    if (i.get(((i2 + (rowSize * 2)) + columnOffset) + wordSize2)) {
                        bits2.set(alignmentMap[(eccBits * 2) + totalSizeBits2], alignmentMap[((baseMatrixSize - 1) - (eccBits * 2)) - wordSize2]);
                    }
                    if (i.get(((i2 + (rowSize * 4)) + columnOffset) + wordSize2)) {
                        bits2.set(alignmentMap[((baseMatrixSize - 1) - (eccBits * 2)) - wordSize2], alignmentMap[((baseMatrixSize - 1) - (eccBits * 2)) - totalSizeBits2]);
                    }
                    if (i.get(((i2 + (rowSize * 6)) + columnOffset) + wordSize2)) {
                        bits2.set(alignmentMap[((baseMatrixSize - 1) - (eccBits * 2)) - totalSizeBits2], alignmentMap[(eccBits * 2) + wordSize2]);
                    }
                    matrixSize = wordSize2 + 1;
                    wordSize2 = wordSize3;
                    usableBitsInLayers = totalBitsInLayer;
                    stuffedBits = stuffedBits3;
                }
                matrixSize = totalSizeBits2 + 1;
                rowSize2 = rowSize;
                totalSizeBits2 = totalSizeBits;
                wordSize2 = wordSize3;
                usableBitsInLayers = totalBitsInLayer;
            }
            totalBitsInLayer = usableBitsInLayers;
            stuffedBits3 = stuffedBits;
            i2 += rowSize * 8;
            eccBits++;
            totalSizeBits2 = totalSizeBits;
        }
        wordSize3 = wordSize2;
        totalBitsInLayer = usableBitsInLayers;
        stuffedBits3 = stuffedBits;
        drawModeMessage(bits2, compact, origCenter, stuffedBits2);
        if (!compact) {
            drawBullsEye(bits2, origCenter / 2, 7);
            i2 = 0;
            int j = 0;
            while (true) {
                eccBits = j;
                if (i2 >= (baseMatrixSize / 2) - 1) {
                    break;
                }
                for (rowSize = (origCenter / 2) & 1; rowSize < origCenter; rowSize += 2) {
                    bits2.set((origCenter / 2) - eccBits, rowSize);
                    bits2.set((origCenter / 2) + eccBits, rowSize);
                    bits2.set(rowSize, (origCenter / 2) - eccBits);
                    bits2.set(rowSize, (origCenter / 2) + eccBits);
                }
                i2 += 15;
                j = eccBits + 16;
            }
        } else {
            drawBullsEye(bits2, origCenter / 2, 5);
        }
        AztecCode aztec = new AztecCode();
        aztec.setCompact(compact);
        aztec.setSize(origCenter);
        aztec.setLayers(wordSize);
        aztec.setCodeWords(i3);
        aztec.setMatrix(bits2);
        return aztec;
    }

    private static void drawBullsEye(BitMatrix matrix, int center, int size) {
        for (int i = 0; i < size; i += 2) {
            for (int j = center - i; j <= center + i; j++) {
                matrix.set(j, center - i);
                matrix.set(j, center + i);
                matrix.set(center - i, j);
                matrix.set(center + i, j);
            }
        }
        matrix.set(center - size, center - size);
        matrix.set((center - size) + 1, center - size);
        matrix.set(center - size, (center - size) + 1);
        matrix.set(center + size, center - size);
        matrix.set(center + size, (center - size) + 1);
        matrix.set(center + size, (center + size) - 1);
    }

    static BitArray generateModeMessage(boolean compact, int layers, int messageSizeInWords) {
        BitArray modeMessage = new BitArray();
        if (compact) {
            modeMessage.appendBits(layers - 1, 2);
            modeMessage.appendBits(messageSizeInWords - 1, 6);
            return generateCheckWords(modeMessage, 28, 4);
        }
        modeMessage.appendBits(layers - 1, 5);
        modeMessage.appendBits(messageSizeInWords - 1, 11);
        return generateCheckWords(modeMessage, 40, 4);
    }

    private static void drawModeMessage(BitMatrix matrix, boolean compact, int matrixSize, BitArray modeMessage) {
        int center = matrixSize / 2;
        int i = 0;
        int offset;
        if (compact) {
            while (i < 7) {
                offset = (center - 3) + i;
                if (modeMessage.get(i)) {
                    matrix.set(offset, center - 5);
                }
                if (modeMessage.get(i + 7)) {
                    matrix.set(center + 5, offset);
                }
                if (modeMessage.get(20 - i)) {
                    matrix.set(offset, center + 5);
                }
                if (modeMessage.get(27 - i)) {
                    matrix.set(center - 5, offset);
                }
                i++;
            }
            return;
        }
        while (i < 10) {
            offset = ((center - 5) + i) + (i / 5);
            if (modeMessage.get(i)) {
                matrix.set(offset, center - 7);
            }
            if (modeMessage.get(i + 10)) {
                matrix.set(center + 7, offset);
            }
            if (modeMessage.get(29 - i)) {
                matrix.set(offset, center + 7);
            }
            if (modeMessage.get(39 - i)) {
                matrix.set(center - 7, offset);
            }
            i++;
        }
    }

    private static BitArray generateCheckWords(BitArray bitArray, int totalBits, int wordSize) {
        int messageSizeInWords = bitArray.getSize() / wordSize;
        GenericGF gfTmp = getGF(wordSize);
        if (gfTmp != null) {
            ReedSolomonEncoder rs = new ReedSolomonEncoder(gfTmp);
            int totalWords = totalBits / wordSize;
            int[] messageWords = bitsToWords(bitArray, wordSize, totalWords);
            rs.encode(messageWords, totalWords - messageSizeInWords);
            int startPad = totalBits % wordSize;
            BitArray messageBits = new BitArray();
            int i = 0;
            messageBits.appendBits(0, startPad);
            int length = messageWords.length;
            while (i < length) {
                messageBits.appendBits(messageWords[i], wordSize);
                i++;
            }
            return messageBits;
        }
        throw new IllegalArgumentException("the wrong wordSize.");
    }

    private static int[] bitsToWords(BitArray stuffedBits, int wordSize, int totalWords) {
        int[] message = new int[totalWords];
        int n = stuffedBits.getSize() / wordSize;
        for (int i = 0; i < n; i++) {
            int value = 0;
            for (int j = 0; j < wordSize; j++) {
                value |= stuffedBits.get((i * wordSize) + j) ? 1 << ((wordSize - j) - 1) : 0;
            }
            message[i] = value;
        }
        return message;
    }

    private static GenericGF getGF(int wordSize) {
        if (wordSize == 4) {
            return GenericGF.AZTEC_PARAM;
        }
        if (wordSize == 6) {
            return GenericGF.AZTEC_DATA_6;
        }
        if (wordSize == 8) {
            return GenericGF.AZTEC_DATA_8;
        }
        if (wordSize == 10) {
            return GenericGF.AZTEC_DATA_10;
        }
        if (wordSize != 12) {
            return null;
        }
        return GenericGF.AZTEC_DATA_12;
    }

    static BitArray stuffBits(BitArray bits, int wordSize) {
        BitArray out = new BitArray();
        int n = bits.getSize();
        int mask = (1 << wordSize) - 2;
        int i = 0;
        while (i < n) {
            int word = 0;
            int j = 0;
            while (j < wordSize) {
                if (i + j >= n || bits.get(i + j)) {
                    word |= 1 << ((wordSize - 1) - j);
                }
                j++;
            }
            if ((word & mask) == mask) {
                out.appendBits(word & mask, wordSize);
                i--;
            } else if ((word & mask) == 0) {
                out.appendBits(word | 1, wordSize);
                i--;
            } else {
                out.appendBits(word, wordSize);
            }
            i += wordSize;
        }
        return out;
    }

    private static int totalBitsInLayer(int layers, boolean compact) {
        return ((compact ? 88 : 112) + (16 * layers)) * layers;
    }
}
