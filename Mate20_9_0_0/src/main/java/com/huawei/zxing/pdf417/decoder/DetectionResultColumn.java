package com.huawei.zxing.pdf417.decoder;

import java.util.Formatter;

class DetectionResultColumn {
    private static final int MAX_NEARBY_DISTANCE = 5;
    private final BoundingBox boundingBox;
    private final Codeword[] codewords;

    DetectionResultColumn(BoundingBox boundingBox) {
        this.boundingBox = new BoundingBox(boundingBox);
        this.codewords = new Codeword[((boundingBox.getMaxY() - boundingBox.getMinY()) + 1)];
    }

    final Codeword getCodewordNearby(int imageRow) {
        Codeword codeword = getCodeword(imageRow);
        if (codeword != null) {
            return codeword;
        }
        for (int i = 1; i < 5; i++) {
            int nearImageRow = imageRowToCodewordIndex(imageRow) - i;
            if (nearImageRow >= 0) {
                codeword = this.codewords[nearImageRow];
                if (codeword != null) {
                    return codeword;
                }
            }
            int nearImageRow2 = imageRowToCodewordIndex(imageRow) + i;
            if (nearImageRow2 < this.codewords.length) {
                codeword = this.codewords[nearImageRow2];
                if (codeword != null) {
                    return codeword;
                }
            }
        }
        return null;
    }

    final int imageRowToCodewordIndex(int imageRow) {
        return imageRow - this.boundingBox.getMinY();
    }

    final void setCodeword(int imageRow, Codeword codeword) {
        this.codewords[imageRowToCodewordIndex(imageRow)] = codeword;
    }

    final Codeword getCodeword(int imageRow) {
        return this.codewords[imageRowToCodewordIndex(imageRow)];
    }

    final BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    final Codeword[] getCodewords() {
        return this.codewords;
    }

    public String toString() {
        Formatter formatter = new Formatter();
        int row = 0;
        for (Codeword codeword : this.codewords) {
            if (codeword == null) {
                Object[] objArr = new Object[1];
                int row2 = row + 1;
                objArr[0] = Integer.valueOf(row);
                formatter.format("%3d:    |   \n", objArr);
                row = row2;
            } else {
                r9 = new Object[3];
                int row3 = row + 1;
                r9[0] = Integer.valueOf(row);
                r9[1] = Integer.valueOf(codeword.getRowNumber());
                r9[2] = Integer.valueOf(codeword.getValue());
                formatter.format("%3d: %3d|%3d\n", r9);
                row = row3;
            }
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
