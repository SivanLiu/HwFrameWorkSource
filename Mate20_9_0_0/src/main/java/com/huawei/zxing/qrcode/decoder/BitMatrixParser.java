package com.huawei.zxing.qrcode.decoder;

import com.huawei.zxing.FormatException;
import com.huawei.zxing.common.BitMatrix;

final class BitMatrixParser {
    private final BitMatrix bitMatrix;
    private boolean mirror;
    private FormatInformation parsedFormatInfo;
    private Version parsedVersion;

    BitMatrixParser(BitMatrix bitMatrix) throws FormatException {
        int dimension = bitMatrix.getHeight();
        if (dimension < 21 || (dimension & 3) != 1) {
            throw FormatException.getFormatInstance();
        }
        this.bitMatrix = bitMatrix;
    }

    FormatInformation readFormatInformation() throws FormatException {
        if (this.parsedFormatInfo != null) {
            return this.parsedFormatInfo;
        }
        int i;
        int j;
        int formatInfoBits1 = 0;
        for (i = 0; i < 6; i++) {
            formatInfoBits1 = copyBit(i, 8, formatInfoBits1);
        }
        formatInfoBits1 = copyBit(8, 7, copyBit(8, 8, copyBit(7, 8, formatInfoBits1)));
        for (i = 5; i >= 0; i--) {
            formatInfoBits1 = copyBit(8, i, formatInfoBits1);
        }
        i = this.bitMatrix.getHeight();
        int formatInfoBits2 = 0;
        int jMin = i - 7;
        for (j = i - 1; j >= jMin; j--) {
            formatInfoBits2 = copyBit(8, j, formatInfoBits2);
        }
        for (j = i - 8; j < i; j++) {
            formatInfoBits2 = copyBit(j, 8, formatInfoBits2);
        }
        this.parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits1, formatInfoBits2);
        if (this.parsedFormatInfo != null) {
            return this.parsedFormatInfo;
        }
        throw FormatException.getFormatInstance();
    }

    Version readVersion() throws FormatException {
        if (this.parsedVersion != null) {
            return this.parsedVersion;
        }
        int dimension = this.bitMatrix.getHeight();
        int provisionalVersion = (dimension - 17) >> 2;
        if (provisionalVersion <= 6) {
            return Version.getVersionForNumber(provisionalVersion);
        }
        int i;
        int ijMin = dimension - 11;
        int i2 = 5;
        int versionBits = 0;
        for (int j = 5; j >= 0; j--) {
            for (i = dimension - 9; i >= ijMin; i--) {
                versionBits = copyBit(i, j, versionBits);
            }
        }
        Version theParsedVersion = Version.decodeVersionInformation(versionBits);
        if (theParsedVersion == null || theParsedVersion.getDimensionForVersion() != dimension) {
            versionBits = 0;
            while (i2 >= 0) {
                for (i = dimension - 9; i >= ijMin; i--) {
                    versionBits = copyBit(i2, i, versionBits);
                }
                i2--;
            }
            theParsedVersion = Version.decodeVersionInformation(versionBits);
            if (theParsedVersion == null || theParsedVersion.getDimensionForVersion() != dimension) {
                throw FormatException.getFormatInstance();
            }
            this.parsedVersion = theParsedVersion;
            return theParsedVersion;
        }
        this.parsedVersion = theParsedVersion;
        return theParsedVersion;
    }

    private int copyBit(int i, int j, int versionBits) {
        return this.mirror ? this.bitMatrix.get(j, i) : this.bitMatrix.get(i, j) ? (versionBits << 1) | 1 : versionBits << 1;
    }

    byte[] readCodewords() throws FormatException {
        BitMatrixParser bitMatrixParser = this;
        FormatInformation formatInfo = readFormatInformation();
        Version version = readVersion();
        DataMask dataMask = DataMask.forReference(formatInfo.getDataMask());
        int dimension = bitMatrixParser.bitMatrix.getHeight();
        dataMask.unmaskBitMatrix(bitMatrixParser.bitMatrix, dimension);
        BitMatrix functionPattern = version.buildFunctionPattern();
        boolean readingUp = true;
        byte[] result = new byte[version.getTotalCodewords()];
        int resultOffset = 0;
        int currentByte = 0;
        int bitsRead = 0;
        int j = dimension - 1;
        while (j > 0) {
            if (j == 6) {
                j--;
            }
            int bitsRead2 = bitsRead;
            bitsRead = currentByte;
            currentByte = resultOffset;
            resultOffset = 0;
            while (resultOffset < dimension) {
                int i = readingUp ? (dimension - 1) - resultOffset : resultOffset;
                int bitsRead3 = bitsRead2;
                bitsRead2 = bitsRead;
                bitsRead = currentByte;
                currentByte = 0;
                while (currentByte < 2) {
                    if (!functionPattern.get(j - currentByte, i)) {
                        bitsRead3++;
                        int currentByte2 = bitsRead2 << 1;
                        if (bitMatrixParser.bitMatrix.get(j - currentByte, i)) {
                            currentByte2 |= 1;
                        }
                        if (bitsRead3 == 8) {
                            int resultOffset2 = bitsRead + 1;
                            result[bitsRead] = (byte) currentByte2;
                            bitsRead3 = 0;
                            bitsRead2 = 0;
                            bitsRead = resultOffset2;
                        } else {
                            bitsRead2 = currentByte2;
                        }
                    }
                    currentByte++;
                    bitMatrixParser = this;
                }
                resultOffset++;
                currentByte = bitsRead;
                bitsRead = bitsRead2;
                bitsRead2 = bitsRead3;
                bitMatrixParser = this;
            }
            readingUp ^= 1;
            j -= 2;
            resultOffset = currentByte;
            currentByte = bitsRead;
            bitsRead = bitsRead2;
            bitMatrixParser = this;
        }
        if (resultOffset == version.getTotalCodewords()) {
            return result;
        }
        throw FormatException.getFormatInstance();
    }

    void remask() {
        if (this.parsedFormatInfo != null) {
            DataMask.forReference(this.parsedFormatInfo.getDataMask()).unmaskBitMatrix(this.bitMatrix, this.bitMatrix.getHeight());
        }
    }

    void setMirror(boolean mirror) {
        this.parsedVersion = null;
        this.parsedFormatInfo = null;
        this.mirror = mirror;
    }

    void mirror() {
        for (int x = 0; x < this.bitMatrix.getWidth(); x++) {
            for (int y = x + 1; y < this.bitMatrix.getHeight(); y++) {
                if (this.bitMatrix.get(x, y) != this.bitMatrix.get(y, x)) {
                    this.bitMatrix.flip(y, x);
                    this.bitMatrix.flip(x, y);
                }
            }
        }
    }
}
