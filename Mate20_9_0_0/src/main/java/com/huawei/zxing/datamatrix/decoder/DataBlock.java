package com.huawei.zxing.datamatrix.decoder;

final class DataBlock {
    private final byte[] codewords;
    private final int numDataCodewords;

    private DataBlock(int numDataCodewords, byte[] codewords) {
        this.numDataCodewords = numDataCodewords;
        this.codewords = codewords;
    }

    static DataBlock[] getDataBlocks(byte[] rawCodewords, Version version) {
        int numResultBlocks;
        int numDataCodewords;
        int numResultBlocks2;
        int rawCodewordsOffset;
        int rawCodewordsOffset2;
        int rawCodewordsOffset3;
        byte[] bArr = rawCodewords;
        ECBlocks ecBlocks = version.getECBlocks();
        ECB[] ecBlockArray = ecBlocks.getECBlocks();
        int i = 0;
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
                result[numResultBlocks] = new DataBlock(numDataCodewords, new byte[(ecBlocks.getECCodewords() + numDataCodewords)]);
                numResultBlocks3++;
                numResultBlocks = numResultBlocks2;
            }
            numResultBlocks4++;
            numResultBlocks3 = numResultBlocks;
        }
        length = result[0].codewords.length - ecBlocks.getECCodewords();
        int shorterBlocksNumDataCodewords = length - 1;
        numDataCodewords = 0;
        numResultBlocks = 0;
        while (numResultBlocks < shorterBlocksNumDataCodewords) {
            rawCodewordsOffset = numDataCodewords;
            numDataCodewords = 0;
            while (numDataCodewords < numResultBlocks3) {
                rawCodewordsOffset2 = rawCodewordsOffset + 1;
                result[numDataCodewords].codewords[numResultBlocks] = bArr[rawCodewordsOffset];
                numDataCodewords++;
                rawCodewordsOffset = rawCodewordsOffset2;
            }
            numResultBlocks++;
            numDataCodewords = rawCodewordsOffset;
        }
        boolean specialVersion = version.getVersionNumber() == 24;
        rawCodewordsOffset = specialVersion ? 8 : numResultBlocks3;
        numResultBlocks2 = numDataCodewords;
        numDataCodewords = 0;
        while (numDataCodewords < rawCodewordsOffset) {
            rawCodewordsOffset3 = numResultBlocks2 + 1;
            result[numDataCodewords].codewords[length - 1] = bArr[numResultBlocks2];
            numDataCodewords++;
            numResultBlocks2 = rawCodewordsOffset3;
        }
        numDataCodewords = result[0].codewords.length;
        rawCodewordsOffset2 = numResultBlocks2;
        numResultBlocks2 = length;
        while (numResultBlocks2 < numDataCodewords) {
            int rawCodewordsOffset4 = rawCodewordsOffset2;
            rawCodewordsOffset2 = i;
            while (rawCodewordsOffset2 < numResultBlocks3) {
                i = (!specialVersion || rawCodewordsOffset2 <= 7) ? numResultBlocks2 : numResultBlocks2 - 1;
                ECBlocks ecBlocks2 = ecBlocks;
                rawCodewordsOffset3 = rawCodewordsOffset4 + 1;
                result[rawCodewordsOffset2].codewords[i] = bArr[rawCodewordsOffset4];
                rawCodewordsOffset2++;
                rawCodewordsOffset4 = rawCodewordsOffset3;
                ecBlocks = ecBlocks2;
            }
            numResultBlocks2++;
            rawCodewordsOffset2 = rawCodewordsOffset4;
            i = 0;
        }
        if (rawCodewordsOffset2 == bArr.length) {
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
