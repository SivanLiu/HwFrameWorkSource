package com.huawei.zxing.qrcode.encoder;

final class MaskUtil {
    private static final int N1 = 3;
    private static final int N2 = 3;
    private static final int N3 = 40;
    private static final int N4 = 10;

    private MaskUtil() {
    }

    static int applyMaskPenaltyRule1(ByteMatrix matrix) {
        return applyMaskPenaltyRule1Internal(matrix, true) + applyMaskPenaltyRule1Internal(matrix, false);
    }

    static int applyMaskPenaltyRule2(ByteMatrix matrix) {
        byte[][] array = matrix.getArray();
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int penalty = 0;
        int y = 0;
        while (y < height - 1) {
            int penalty2 = penalty;
            penalty = 0;
            while (penalty < width - 1) {
                byte value = array[y][penalty];
                if (value == array[y][penalty + 1] && value == array[y + 1][penalty] && value == array[y + 1][penalty + 1]) {
                    penalty2++;
                }
                penalty++;
            }
            y++;
            penalty = penalty2;
        }
        return 3 * penalty;
    }

    static int applyMaskPenaltyRule3(ByteMatrix matrix) {
        byte[][] array = matrix.getArray();
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int numPenalties = 0;
        int y = 0;
        while (y < height) {
            int numPenalties2 = numPenalties;
            numPenalties = 0;
            while (numPenalties < width) {
                byte[] arrayY = array[y];
                if (numPenalties + 6 < width && arrayY[numPenalties] == (byte) 1 && arrayY[numPenalties + 1] == (byte) 0 && arrayY[numPenalties + 2] == (byte) 1 && arrayY[numPenalties + 3] == (byte) 1 && arrayY[numPenalties + 4] == (byte) 1 && arrayY[numPenalties + 5] == (byte) 0 && arrayY[numPenalties + 6] == (byte) 1 && (isWhiteHorizontal(arrayY, numPenalties - 4, numPenalties) || isWhiteHorizontal(arrayY, numPenalties + 7, numPenalties + 11))) {
                    numPenalties2++;
                }
                if (y + 6 < height && array[y][numPenalties] == (byte) 1 && array[y + 1][numPenalties] == (byte) 0 && array[y + 2][numPenalties] == (byte) 1 && array[y + 3][numPenalties] == (byte) 1 && array[y + 4][numPenalties] == (byte) 1 && array[y + 5][numPenalties] == (byte) 0 && array[y + 6][numPenalties] == (byte) 1 && (isWhiteVertical(array, numPenalties, y - 4, y) || isWhiteVertical(array, numPenalties, y + 7, y + 11))) {
                    numPenalties2++;
                }
                numPenalties++;
            }
            y++;
            numPenalties = numPenalties2;
        }
        return numPenalties * 40;
    }

    private static boolean isWhiteHorizontal(byte[] rowArray, int from, int to) {
        int i = from;
        while (i < to) {
            if (i >= 0 && i < rowArray.length && rowArray[i] == (byte) 1) {
                return false;
            }
            i++;
        }
        return true;
    }

    private static boolean isWhiteVertical(byte[][] array, int col, int from, int to) {
        int i = from;
        while (i < to) {
            if (i >= 0 && i < array.length && array[i][col] == (byte) 1) {
                return false;
            }
            i++;
        }
        return true;
    }

    static int applyMaskPenaltyRule4(ByteMatrix matrix) {
        byte[][] array = matrix.getArray();
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int numDarkCells = 0;
        int y = 0;
        while (y < height) {
            byte[] arrayY = array[y];
            int numDarkCells2 = numDarkCells;
            for (numDarkCells = 0; numDarkCells < width; numDarkCells++) {
                if (arrayY[numDarkCells] == (byte) 1) {
                    numDarkCells2++;
                }
            }
            y++;
            numDarkCells = numDarkCells2;
        }
        y = matrix.getHeight() * matrix.getWidth();
        return ((Math.abs((numDarkCells * 2) - y) * 10) / y) * 10;
    }

    static boolean getDataMaskBit(int maskPattern, int x, int y) {
        int intermediate;
        int temp;
        switch (maskPattern) {
            case 0:
                intermediate = (y + x) & 1;
                break;
            case 1:
                intermediate = y & 1;
                break;
            case 2:
                intermediate = x % 3;
                break;
            case 3:
                intermediate = (y + x) % 3;
                break;
            case 4:
                intermediate = ((y >>> 1) + (x / 3)) & 1;
                break;
            case 5:
                temp = y * x;
                intermediate = (temp & 1) + (temp % 3);
                break;
            case 6:
                temp = y * x;
                intermediate = ((temp & 1) + (temp % 3)) & 1;
                break;
            case 7:
                intermediate = (((y * x) % 3) + ((y + x) & 1)) & 1;
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid mask pattern: ");
                stringBuilder.append(maskPattern);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        if (intermediate == 0) {
            return true;
        }
        return false;
    }

    private static int applyMaskPenaltyRule1Internal(ByteMatrix matrix, boolean isHorizontal) {
        int iLimit = isHorizontal ? matrix.getHeight() : matrix.getWidth();
        int jLimit = isHorizontal ? matrix.getWidth() : matrix.getHeight();
        byte[][] array = matrix.getArray();
        int penalty = 0;
        int i = 0;
        while (i < iLimit) {
            int numSameBitCells = 0;
            int prevBit = -1;
            int penalty2 = penalty;
            penalty = 0;
            while (penalty < jLimit) {
                int bit = isHorizontal ? array[i][penalty] : array[penalty][i];
                if (bit == prevBit) {
                    numSameBitCells++;
                } else {
                    if (numSameBitCells >= 5) {
                        penalty2 += 3 + (numSameBitCells - 5);
                    }
                    numSameBitCells = 1;
                    prevBit = bit;
                }
                penalty++;
            }
            if (numSameBitCells >= 5) {
                penalty2 += 3 + (numSameBitCells - 5);
            }
            penalty = penalty2;
            i++;
        }
        return penalty;
    }
}
