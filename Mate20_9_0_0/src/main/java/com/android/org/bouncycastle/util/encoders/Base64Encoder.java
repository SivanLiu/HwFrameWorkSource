package com.android.org.bouncycastle.util.encoders;

import java.io.IOException;
import java.io.OutputStream;

public class Base64Encoder implements Encoder {
    protected final byte[] decodingTable = new byte[128];
    protected final byte[] encodingTable = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};
    protected byte padding = (byte) 61;

    protected void initialiseDecodingTable() {
        int i = 0;
        for (int i2 = 0; i2 < this.decodingTable.length; i2++) {
            this.decodingTable[i2] = (byte) -1;
        }
        while (i < this.encodingTable.length) {
            this.decodingTable[this.encodingTable[i]] = (byte) i;
            i++;
        }
    }

    public Base64Encoder() {
        initialiseDecodingTable();
    }

    public int encode(byte[] data, int off, int length, OutputStream out) throws IOException {
        int i;
        int a1;
        int a2;
        int a3;
        int modulus = length % 3;
        int dataLength = length - modulus;
        for (i = off; i < off + dataLength; i += 3) {
            a1 = data[i] & 255;
            a2 = data[i + 1] & 255;
            a3 = data[i + 2] & 255;
            out.write(this.encodingTable[(a1 >>> 2) & 63]);
            out.write(this.encodingTable[((a1 << 4) | (a2 >>> 4)) & 63]);
            out.write(this.encodingTable[((a2 << 2) | (a3 >>> 6)) & 63]);
            out.write(this.encodingTable[a3 & 63]);
        }
        switch (modulus) {
            case 1:
                i = data[off + dataLength] & 255;
                a2 = (i << 4) & 63;
                out.write(this.encodingTable[(i >>> 2) & 63]);
                out.write(this.encodingTable[a2]);
                out.write(this.padding);
                out.write(this.padding);
                break;
            case 2:
                i = data[off + dataLength] & 255;
                a1 = data[(off + dataLength) + 1] & 255;
                a3 = ((i << 4) | (a1 >>> 4)) & 63;
                int b3 = (a1 << 2) & 63;
                out.write(this.encodingTable[(i >>> 2) & 63]);
                out.write(this.encodingTable[a3]);
                out.write(this.encodingTable[b3]);
                out.write(this.padding);
                break;
        }
        a1 = 4;
        i = (dataLength / 3) * 4;
        if (modulus == 0) {
            a1 = 0;
        }
        return i + a1;
    }

    private boolean ignore(char c) {
        return c == 10 || c == 13 || c == 9 || c == ' ';
    }

    public int decode(byte[] data, int off, int length, OutputStream out) throws IOException {
        int outLen = 0;
        int end = off + length;
        while (end > off && ignore((char) data[end - 1])) {
            end--;
        }
        int finish = end - 4;
        int i = nextI(data, off, finish);
        while (i < finish) {
            int i2 = i + 1;
            byte b1 = this.decodingTable[data[i]];
            int i3 = nextI(data, i2, finish);
            int i4 = i3 + 1;
            byte b2 = this.decodingTable[data[i3]];
            i2 = nextI(data, i4, finish);
            int i5 = i2 + 1;
            byte b3 = this.decodingTable[data[i2]];
            i4 = nextI(data, i5, finish);
            int i6 = i4 + 1;
            byte b4 = this.decodingTable[data[i4]];
            if ((((b1 | b2) | b3) | b4) >= 0) {
                out.write((b1 << 2) | (b2 >> 4));
                out.write((b2 << 4) | (b3 >> 2));
                out.write((b3 << 6) | b4);
                outLen += 3;
                i = nextI(data, i6, finish);
            } else {
                throw new IOException("invalid characters encountered in base64 data");
            }
        }
        return outLen + decodeLastBlock(out, (char) data[end - 4], (char) data[end - 3], (char) data[end - 2], (char) data[end - 1]);
    }

    private int nextI(byte[] data, int i, int finish) {
        while (i < finish && ignore((char) data[i])) {
            i++;
        }
        return i;
    }

    public int decode(String data, OutputStream out) throws IOException {
        int length = 0;
        int end = data.length();
        while (end > 0 && ignore(data.charAt(end - 1))) {
            end--;
        }
        int finish = end - 4;
        int i = nextI(data, 0, finish);
        while (i < finish) {
            int i2 = i + 1;
            byte b1 = this.decodingTable[data.charAt(i)];
            int i3 = nextI(data, i2, finish);
            int i4 = i3 + 1;
            byte b2 = this.decodingTable[data.charAt(i3)];
            i2 = nextI(data, i4, finish);
            int i5 = i2 + 1;
            byte b3 = this.decodingTable[data.charAt(i2)];
            i4 = nextI(data, i5, finish);
            int i6 = i4 + 1;
            byte b4 = this.decodingTable[data.charAt(i4)];
            if ((((b1 | b2) | b3) | b4) >= 0) {
                out.write((b1 << 2) | (b2 >> 4));
                out.write((b2 << 4) | (b3 >> 2));
                out.write((b3 << 6) | b4);
                length += 3;
                i = nextI(data, i6, finish);
            } else {
                throw new IOException("invalid characters encountered in base64 data");
            }
        }
        return length + decodeLastBlock(out, data.charAt(end - 4), data.charAt(end - 3), data.charAt(end - 2), data.charAt(end - 1));
    }

    private int decodeLastBlock(OutputStream out, char c1, char c2, char c3, char c4) throws IOException {
        byte b1;
        byte b2;
        byte b3;
        if (c3 == this.padding) {
            if (c4 == this.padding) {
                b1 = this.decodingTable[c1];
                b2 = this.decodingTable[c2];
                if ((b1 | b2) >= 0) {
                    out.write((b1 << 2) | (b2 >> 4));
                    return 1;
                }
                throw new IOException("invalid characters encountered at end of base64 data");
            }
            throw new IOException("invalid characters encountered at end of base64 data");
        } else if (c4 == this.padding) {
            b1 = this.decodingTable[c1];
            b2 = this.decodingTable[c2];
            b3 = this.decodingTable[c3];
            if (((b1 | b2) | b3) >= 0) {
                out.write((b1 << 2) | (b2 >> 4));
                out.write((b2 << 4) | (b3 >> 2));
                return 2;
            }
            throw new IOException("invalid characters encountered at end of base64 data");
        } else {
            b1 = this.decodingTable[c1];
            b2 = this.decodingTable[c2];
            b3 = this.decodingTable[c3];
            byte b4 = this.decodingTable[c4];
            if ((((b1 | b2) | b3) | b4) >= 0) {
                out.write((b1 << 2) | (b2 >> 4));
                out.write((b2 << 4) | (b3 >> 2));
                out.write((b3 << 6) | b4);
                return 3;
            }
            throw new IOException("invalid characters encountered at end of base64 data");
        }
    }

    private int nextI(String data, int i, int finish) {
        while (i < finish && ignore(data.charAt(i))) {
            i++;
        }
        return i;
    }
}
