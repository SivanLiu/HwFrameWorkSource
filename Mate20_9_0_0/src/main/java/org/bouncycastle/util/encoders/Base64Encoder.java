package org.bouncycastle.util.encoders;

import java.io.IOException;
import java.io.OutputStream;

public class Base64Encoder implements Encoder {
    protected final byte[] decodingTable = new byte[128];
    protected final byte[] encodingTable = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};
    protected byte padding = (byte) 61;

    public Base64Encoder() {
        initialiseDecodingTable();
    }

    private int decodeLastBlock(OutputStream outputStream, char c, char c2, char c3, char c4) throws IOException {
        byte b;
        byte b2;
        byte b3;
        if (c3 == this.padding) {
            if (c4 == this.padding) {
                b = this.decodingTable[c];
                b2 = this.decodingTable[c2];
                if ((b | b2) >= 0) {
                    outputStream.write((b << 2) | (b2 >> 4));
                    return 1;
                }
                throw new IOException("invalid characters encountered at end of base64 data");
            }
            throw new IOException("invalid characters encountered at end of base64 data");
        } else if (c4 == this.padding) {
            b = this.decodingTable[c];
            b2 = this.decodingTable[c2];
            b3 = this.decodingTable[c3];
            if (((b | b2) | b3) >= 0) {
                outputStream.write((b << 2) | (b2 >> 4));
                outputStream.write((b2 << 4) | (b3 >> 2));
                return 2;
            }
            throw new IOException("invalid characters encountered at end of base64 data");
        } else {
            b = this.decodingTable[c];
            b2 = this.decodingTable[c2];
            b3 = this.decodingTable[c3];
            byte b4 = this.decodingTable[c4];
            if ((((b | b2) | b3) | b4) >= 0) {
                outputStream.write((b << 2) | (b2 >> 4));
                outputStream.write((b2 << 4) | (b3 >> 2));
                outputStream.write((b3 << 6) | b4);
                return 3;
            }
            throw new IOException("invalid characters encountered at end of base64 data");
        }
    }

    private boolean ignore(char c) {
        return c == 10 || c == 13 || c == 9 || c == ' ';
    }

    private int nextI(String str, int i, int i2) {
        while (i < i2 && ignore(str.charAt(i))) {
            i++;
        }
        return i;
    }

    private int nextI(byte[] bArr, int i, int i2) {
        while (i < i2 && ignore((char) bArr[i])) {
            i++;
        }
        return i;
    }

    public int decode(String str, OutputStream outputStream) throws IOException {
        int nextI;
        int length = str.length();
        while (length > 0 && ignore(str.charAt(length - 1))) {
            length--;
        }
        int i = 0;
        int i2 = length;
        int i3 = 0;
        while (i2 > 0 && i3 != 4) {
            if (!ignore(str.charAt(i2 - 1))) {
                i3++;
            }
            i2--;
        }
        i3 = nextI(str, 0, i2);
        while (i3 < i2) {
            int i4 = i3 + 1;
            byte b = this.decodingTable[str.charAt(i3)];
            nextI = nextI(str, i4, i2);
            int i5 = nextI + 1;
            byte b2 = this.decodingTable[str.charAt(nextI)];
            i4 = nextI(str, i5, i2);
            int i6 = i4 + 1;
            byte b3 = this.decodingTable[str.charAt(i4)];
            i5 = nextI(str, i6, i2);
            int i7 = i5 + 1;
            byte b4 = this.decodingTable[str.charAt(i5)];
            if ((((b | b2) | b3) | b4) >= 0) {
                outputStream.write((b << 2) | (b2 >> 4));
                outputStream.write((b2 << 4) | (b3 >> 2));
                outputStream.write((b3 << 6) | b4);
                i += 3;
                i3 = nextI(str, i7, i2);
            } else {
                throw new IOException("invalid characters encountered in base64 data");
            }
        }
        i2 = nextI(str, i3, length);
        i3 = nextI(str, i2 + 1, length);
        nextI = nextI(str, i3 + 1, length);
        return i + decodeLastBlock(outputStream, str.charAt(i2), str.charAt(i3), str.charAt(nextI), str.charAt(nextI(str, nextI + 1, length)));
    }

    public int decode(byte[] bArr, int i, int i2, OutputStream outputStream) throws IOException {
        i2 += i;
        while (i2 > i && ignore((char) bArr[i2 - 1])) {
            i2--;
        }
        int i3 = 0;
        int i4 = i2;
        int i5 = 0;
        while (i4 > i && i5 != 4) {
            if (!ignore((char) bArr[i4 - 1])) {
                i5++;
            }
            i4--;
        }
        i = nextI(bArr, i, i4);
        while (i < i4) {
            int i6 = i + 1;
            byte b = this.decodingTable[bArr[i]];
            i5 = nextI(bArr, i6, i4);
            int i7 = i5 + 1;
            byte b2 = this.decodingTable[bArr[i5]];
            i6 = nextI(bArr, i7, i4);
            int i8 = i6 + 1;
            byte b3 = this.decodingTable[bArr[i6]];
            i7 = nextI(bArr, i8, i4);
            int i9 = i7 + 1;
            byte b4 = this.decodingTable[bArr[i7]];
            if ((((b | b2) | b3) | b4) >= 0) {
                outputStream.write((b << 2) | (b2 >> 4));
                outputStream.write((b2 << 4) | (b3 >> 2));
                outputStream.write((b3 << 6) | b4);
                i3 += 3;
                i = nextI(bArr, i9, i4);
            } else {
                throw new IOException("invalid characters encountered in base64 data");
            }
        }
        i = nextI(bArr, i, i2);
        i4 = nextI(bArr, i + 1, i2);
        i5 = nextI(bArr, i4 + 1, i2);
        return i3 + decodeLastBlock(outputStream, (char) bArr[i], (char) bArr[i4], (char) bArr[i5], (char) bArr[nextI(bArr, i5 + 1, i2)]);
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x00a2  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int encode(byte[] bArr, int i, int i2, OutputStream outputStream) throws IOException {
        int i3;
        int i4;
        int i5;
        int i6 = i2 % 3;
        i2 -= i6;
        int i7 = i;
        while (true) {
            i3 = i + i2;
            i4 = 4;
            if (i7 >= i3) {
                break;
            }
            i3 = bArr[i7] & 255;
            int i8 = bArr[i7 + 1] & 255;
            int i9 = bArr[i7 + 2] & 255;
            outputStream.write(this.encodingTable[(i3 >>> 2) & 63]);
            outputStream.write(this.encodingTable[((i3 << 4) | (i8 >>> 4)) & 63]);
            outputStream.write(this.encodingTable[((i8 << 2) | (i9 >>> 6)) & 63]);
            outputStream.write(this.encodingTable[i9 & 63]);
            i7 += 3;
        }
        switch (i6) {
            case 1:
                i5 = bArr[i3] & 255;
                i = (i5 >>> 2) & 63;
                i5 = (i5 << 4) & 63;
                outputStream.write(this.encodingTable[i]);
                outputStream.write(this.encodingTable[i5]);
                i5 = this.padding;
                break;
            case 2:
                i = bArr[i3] & 255;
                i5 = bArr[i3 + 1] & 255;
                i7 = (i >>> 2) & 63;
                i = ((i << 4) | (i5 >>> 4)) & 63;
                i5 = (i5 << 2) & 63;
                outputStream.write(this.encodingTable[i7]);
                outputStream.write(this.encodingTable[i]);
                i5 = this.encodingTable[i5];
                break;
            default:
                i2 = (i2 / 3) * 4;
                if (i6 == 0) {
                    i4 = 0;
                }
                return i2 + i4;
        }
        outputStream.write(i5);
        outputStream.write(this.padding);
        i2 = (i2 / 3) * 4;
        if (i6 == 0) {
        }
        return i2 + i4;
    }

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
}
