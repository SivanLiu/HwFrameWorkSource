package com.huawei.zxing.qrcode.decoder;

import com.huawei.zxing.qrcode.decoder.Version.ECB;
import com.huawei.zxing.qrcode.decoder.Version.ECBlocks;

final class DataBlock {
    private final byte[] codewords;
    private final int numDataCodewords;

    private DataBlock(int numDataCodewords, byte[] codewords) {
        this.numDataCodewords = numDataCodewords;
        this.codewords = codewords;
    }

    static DataBlock[] getDataBlocks(byte[] rawCodewords, Version version, ErrorCorrectionLevel ecLevel) {
        byte[] bArr = rawCodewords;
        if (bArr.length == version.getTotalCodewords()) {
            int numResultBlocks;
            int numDataCodewords;
            int numResultBlocks2;
            int rawCodewordsOffset;
            ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
            ECB[] ecBlockArray = ecBlocks.getECBlocks();
            int totalBlocks = 0;
            for (ECB ecBlock : ecBlockArray) {
                totalBlocks += ecBlock.getCount();
            }
            DataBlock[] result = new DataBlock[totalBlocks];
            int length = ecBlockArray.length;
            int numResultBlocks3 = 0;
            int numResultBlocks4 = 0;
            while (numResultBlocks4 < length) {
                ECB ecBlock2 = ecBlockArray[numResultBlocks4];
                numResultBlocks = numResultBlocks3;
                numResultBlocks3 = 0;
                while (numResultBlocks3 < ecBlock2.getCount()) {
                    numDataCodewords = ecBlock2.getDataCodewords();
                    numResultBlocks2 = numResultBlocks + 1;
                    result[numResultBlocks] = new DataBlock(numDataCodewords, new byte[(ecBlocks.getECCodewordsPerBlock() + numDataCodewords)]);
                    numResultBlocks3++;
                    numResultBlocks = numResultBlocks2;
                }
                numResultBlocks4++;
                numResultBlocks3 = numResultBlocks;
            }
            numResultBlocks4 = result[0].codewords.length;
            length = result.length - 1;
            while (length >= 0 && result[length].codewords.length != numResultBlocks4) {
                length--;
            }
            length++;
            int shorterBlocksNumDataCodewords = numResultBlocks4 - ecBlocks.getECCodewordsPerBlock();
            numDataCodewords = 0;
            numResultBlocks = 0;
            while (numResultBlocks < shorterBlocksNumDataCodewords) {
                rawCodewordsOffset = numDataCodewords;
                numDataCodewords = 0;
                while (numDataCodewords < numResultBlocks3) {
                    int rawCodewordsOffset2 = rawCodewordsOffset + 1;
                    result[numDataCodewords].codewords[numResultBlocks] = bArr[rawCodewordsOffset];
                    numDataCodewords++;
                    rawCodewordsOffset = rawCodewordsOffset2;
                }
                numResultBlocks++;
                numDataCodewords = rawCodewordsOffset;
            }
            numResultBlocks = length;
            while (numResultBlocks < numResultBlocks3) {
                numResultBlocks2 = numDataCodewords + 1;
                result[numResultBlocks].codewords[shorterBlocksNumDataCodewords] = bArr[numDataCodewords];
                numResultBlocks++;
                numDataCodewords = numResultBlocks2;
            }
            numResultBlocks = result[0].codewords.length;
            rawCodewordsOffset = numDataCodewords;
            numDataCodewords = shorterBlocksNumDataCodewords;
            while (numDataCodewords < numResultBlocks) {
                numResultBlocks2 = rawCodewordsOffset;
                rawCodewordsOffset = 0;
                while (rawCodewordsOffset < numResultBlocks3) {
                    int rawCodewordsOffset3 = numResultBlocks2 + 1;
                    result[rawCodewordsOffset].codewords[rawCodewordsOffset < length ? numDataCodewords : numDataCodewords + 1] = bArr[numResultBlocks2];
                    rawCodewordsOffset++;
                    numResultBlocks2 = rawCodewordsOffset3;
                }
                numDataCodewords++;
                rawCodewordsOffset = numResultBlocks2;
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    int getNumDataCodewords() {
        return this.numDataCodewords;
    }

    byte[] getCodewords() {
        return this.codewords;
    }
}
