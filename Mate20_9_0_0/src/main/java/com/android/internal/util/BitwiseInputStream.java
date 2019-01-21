package com.android.internal.util;

public class BitwiseInputStream {
    private byte[] mBuf;
    private int mEnd;
    private int mPos = 0;

    public static class AccessException extends Exception {
        public AccessException(String s) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BitwiseInputStream access failed: ");
            stringBuilder.append(s);
            super(stringBuilder.toString());
        }
    }

    public BitwiseInputStream(byte[] buf) {
        this.mBuf = buf;
        this.mEnd = buf.length << 3;
    }

    public int available() {
        return this.mEnd - this.mPos;
    }

    public int read(int bits) throws AccessException {
        int index = this.mPos >>> 3;
        int offset = (16 - (this.mPos & 7)) - bits;
        if (bits < 0 || bits > 8 || this.mPos + bits > this.mEnd) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("illegal read (pos ");
            stringBuilder.append(this.mPos);
            stringBuilder.append(", end ");
            stringBuilder.append(this.mEnd);
            stringBuilder.append(", bits ");
            stringBuilder.append(bits);
            stringBuilder.append(")");
            throw new AccessException(stringBuilder.toString());
        }
        int data = (this.mBuf[index] & 255) << 8;
        if (offset < 8) {
            data |= this.mBuf[index + 1] & 255;
        }
        int data2 = (data >>> offset) & (-1 >>> (32 - bits));
        this.mPos += bits;
        return data2;
    }

    public byte[] readByteArray(int bits) throws AccessException {
        int i = 0;
        int bytes = (bits >>> 3) + ((bits & 7) > 0 ? 1 : 0);
        byte[] arr = new byte[bytes];
        while (i < bytes) {
            int increment = Math.min(8, bits - (i << 3));
            arr[i] = (byte) (read(increment) << (8 - increment));
            i++;
        }
        return arr;
    }

    public void skip(int bits) throws AccessException {
        if (this.mPos + bits <= this.mEnd) {
            this.mPos += bits;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal skip (pos ");
        stringBuilder.append(this.mPos);
        stringBuilder.append(", end ");
        stringBuilder.append(this.mEnd);
        stringBuilder.append(", bits ");
        stringBuilder.append(bits);
        stringBuilder.append(")");
        throw new AccessException(stringBuilder.toString());
    }
}
