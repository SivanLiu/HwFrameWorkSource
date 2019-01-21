package com.huawei.zxing.common;

public final class BitMatrix {
    private final int[] bits;
    private final int height;
    private final int rowSize;
    private final int width;

    public BitMatrix(int dimension) {
        this(dimension, dimension);
    }

    public BitMatrix(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Both dimensions must be greater than 0");
        }
        this.width = width;
        this.height = height;
        this.rowSize = (width + 31) >> 5;
        this.bits = new int[(this.rowSize * height)];
    }

    public boolean get(int x, int y) {
        return ((this.bits[(this.rowSize * y) + (x >> 5)] >>> (x & 31)) & 1) != 0;
    }

    public void set(int x, int y) {
        int offset = (this.rowSize * y) + (x >> 5);
        int[] iArr = this.bits;
        iArr[offset] = iArr[offset] | (1 << (x & 31));
    }

    public void flip(int x, int y) {
        int offset = (this.rowSize * y) + (x >> 5);
        int[] iArr = this.bits;
        iArr[offset] = iArr[offset] ^ (1 << (x & 31));
    }

    public void clear() {
        int max = this.bits.length;
        for (int i = 0; i < max; i++) {
            this.bits[i] = 0;
        }
    }

    public void setRegion(int left, int top, int width, int height) {
        if (top < 0 || left < 0) {
            throw new IllegalArgumentException("Left and top must be nonnegative");
        } else if (height < 1 || width < 1) {
            throw new IllegalArgumentException("Height and width must be at least 1");
        } else {
            int right = left + width;
            int bottom = top + height;
            if (bottom > this.height || right > this.width) {
                throw new IllegalArgumentException("The region must fit inside the matrix");
            }
            for (int y = top; y < bottom; y++) {
                int offset = this.rowSize * y;
                for (int x = left; x < right; x++) {
                    int[] iArr = this.bits;
                    int i = (x >> 5) + offset;
                    iArr[i] = iArr[i] | (1 << (x & 31));
                }
            }
        }
    }

    public BitArray getRow(int y, BitArray row) {
        if (row == null || row.getSize() < this.width) {
            row = new BitArray(this.width);
        }
        int offset = this.rowSize * y;
        for (int x = 0; x < this.rowSize; x++) {
            row.setBulk(x << 5, this.bits[offset + x]);
        }
        return row;
    }

    public void setRow(int y, BitArray row) {
        System.arraycopy(row.getBitArray(), 0, this.bits, this.rowSize * y, this.rowSize);
    }

    public int[] getEnclosingRectangle() {
        int bottom;
        int left = this.width;
        int right = -1;
        int bottom2 = -1;
        int top = this.height;
        int left2 = left;
        left = 0;
        while (left < this.height) {
            bottom = bottom2;
            bottom2 = right;
            right = left2;
            for (left2 = 0; left2 < this.rowSize; left2++) {
                int theBits = this.bits[(this.rowSize * left) + left2];
                if (theBits != 0) {
                    int bit;
                    if (left < top) {
                        top = left;
                    }
                    if (left > bottom) {
                        bottom = left;
                    }
                    int bit2 = 31;
                    if (left2 * 32 < right) {
                        bit = 0;
                        while ((theBits << (31 - bit)) == 0) {
                            bit++;
                        }
                        if ((left2 * 32) + bit < right) {
                            right = (left2 * 32) + bit;
                        }
                    }
                    if ((left2 * 32) + 31 > bottom2) {
                        while (true) {
                            bit = bit2;
                            if ((theBits >>> bit) != 0) {
                                break;
                            }
                            bit2 = bit - 1;
                        }
                        if ((left2 * 32) + bit > bottom2) {
                            bottom2 = (left2 * 32) + bit;
                        }
                    }
                }
            }
            left++;
            left2 = right;
            right = bottom2;
            bottom2 = bottom;
        }
        bottom = bottom2 - top;
        if (right - left2 < 0 || bottom < 0) {
            return null;
        }
        return new int[]{left2, top, left, bottom};
    }

    public int[] getTopLeftOnBit() {
        int bitsOffset = 0;
        while (bitsOffset < this.bits.length && this.bits[bitsOffset] == 0) {
            bitsOffset++;
        }
        if (bitsOffset == this.bits.length) {
            return null;
        }
        int bit;
        int y = bitsOffset / this.rowSize;
        int x = (bitsOffset % this.rowSize) << 5;
        for (bit = 0; (this.bits[bitsOffset] << (31 - bit)) == 0; bit++) {
        }
        return new int[]{x + bit, y};
    }

    public int[] getBottomRightOnBit() {
        int bitsOffset = this.bits.length - 1;
        while (bitsOffset >= 0 && this.bits[bitsOffset] == 0) {
            bitsOffset--;
        }
        if (bitsOffset < 0) {
            return null;
        }
        int bit;
        int y = bitsOffset / this.rowSize;
        int x = (bitsOffset % this.rowSize) << 5;
        for (bit = 31; (this.bits[bitsOffset] >>> bit) == 0; bit--) {
        }
        return new int[]{x + bit, y};
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean equals(Object o) {
        if (!(o instanceof BitMatrix)) {
            return false;
        }
        BitMatrix other = (BitMatrix) o;
        if (this.width != other.width || this.height != other.height || this.rowSize != other.rowSize || this.bits.length != other.bits.length) {
            return false;
        }
        for (int i = 0; i < this.bits.length; i++) {
            if (this.bits[i] != other.bits[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = (31 * ((31 * ((31 * this.width) + this.width)) + this.height)) + this.rowSize;
        for (int bit : this.bits) {
            hash = (31 * hash) + bit;
        }
        return hash;
    }

    public String toString() {
        StringBuilder result = new StringBuilder(this.height * (this.width + 1));
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                result.append(get(x, y) ? "X " : "  ");
            }
            result.append(10);
        }
        return result.toString();
    }
}
