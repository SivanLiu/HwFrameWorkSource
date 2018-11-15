package android_maps_conflict_avoidance.com.google.common.io;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;

public class ByteArrayDataInput implements DataInput {
    private byte[] mBytes;
    private int mLength = this.mBytes.length;
    private int mPos = 0;
    private char[] mUtfCharBuf = new char[128];

    public ByteArrayDataInput(byte[] bytes) {
        this.mBytes = bytes;
    }

    public boolean readBoolean() throws IOException {
        try {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            return bArr[i] != (byte) 0;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public byte readByte() throws IOException {
        try {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            return bArr[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public char readChar() throws IOException {
        try {
            int a = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            a = a[i];
            byte[] bArr = this.mBytes;
            int i2 = this.mPos;
            this.mPos = i2 + 1;
            return (char) ((a << 8) | (bArr[i2] & 255));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public void readFully(byte[] buffer) throws IOException {
        readFully(buffer, 0, buffer.length);
    }

    public void readFully(byte[] buffer, int offset, int length) throws IOException {
        if (length != 0) {
            if (offset + length > buffer.length) {
                throw new IndexOutOfBoundsException();
            } else if (length <= this.mLength - this.mPos) {
                System.arraycopy(this.mBytes, this.mPos, buffer, offset, length);
                this.mPos += length;
            } else {
                this.mPos = this.mLength;
                throw new EOFException();
            }
        }
    }

    public int readInt() throws IOException {
        try {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            int a = bArr[i] & 255;
            byte[] bArr2 = this.mBytes;
            int i2 = this.mPos;
            this.mPos = i2 + 1;
            i = bArr2[i2] & 255;
            byte[] bArr3 = this.mBytes;
            int i3 = this.mPos;
            this.mPos = i3 + 1;
            i2 = bArr3[i3] & 255;
            byte[] bArr4 = this.mBytes;
            int i4 = this.mPos;
            this.mPos = i4 + 1;
            return (((a << 24) | (i << 16)) | (i2 << 8)) | (bArr4[i4] & 255);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public String readLine() {
        if (this.mPos >= this.mLength) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        while (true) {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            char c = (char) bArr[i];
            if (c == 10) {
                return result.toString();
            }
            if (c == 13) {
                if (this.mPos < this.mLength && this.mBytes[this.mPos] == (byte) 10) {
                    this.mPos++;
                }
                return result.toString();
            }
            result.append(c);
            if (this.mPos == this.mLength) {
                return result.toString();
            }
        }
    }

    public long readLong() throws IOException {
        try {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            long a = (long) (bArr[i] & 255);
            bArr = this.mBytes;
            int i2 = this.mPos;
            this.mPos = i2 + 1;
            long b = (long) (bArr[i2] & 255);
            bArr = this.mBytes;
            int i3 = this.mPos;
            this.mPos = i3 + 1;
            long c = (long) (bArr[i3] & 255);
            bArr = this.mBytes;
            int i4 = this.mPos;
            this.mPos = i4 + 1;
            long d = (long) (bArr[i4] & 255);
            bArr = this.mBytes;
            int i5 = this.mPos;
            this.mPos = i5 + 1;
            long e = (long) (bArr[i5] & 255);
            bArr = this.mBytes;
            int i6 = this.mPos;
            this.mPos = i6 + 1;
            long f = (long) (bArr[i6] & 255);
            bArr = this.mBytes;
            int i7 = this.mPos;
            this.mPos = i7 + 1;
            long g = (long) (bArr[i7] & 255);
            bArr = this.mBytes;
            long g2 = g;
            i7 = this.mPos;
            this.mPos = i7 + 1;
            return (((((((a << 56) | (b << 48)) | (c << 40)) | (d << 32)) | (e << 24)) | (f << 16)) | (g2 << 8)) | ((long) (bArr[i7] & 255));
        } catch (ArrayIndexOutOfBoundsException e2) {
            throw new EOFException();
        }
    }

    public short readShort() throws IOException {
        try {
            int a = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            a = a[i];
            byte[] bArr = this.mBytes;
            int i2 = this.mPos;
            this.mPos = i2 + 1;
            return (short) ((a << 8) | (bArr[i2] & 255));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public String readUTF() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return "";
        }
        if (length <= this.mLength - this.mPos) {
            if (length > this.mUtfCharBuf.length) {
                this.mUtfCharBuf = new char[length];
            }
            String result = convertUTF8WithBuf(this.mBytes, this.mUtfCharBuf, this.mPos, length);
            this.mPos += length;
            return result;
        }
        this.mPos = this.mLength;
        throw new EOFException();
    }

    public int readUnsignedByte() throws IOException {
        try {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            return bArr[i] & 255;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public int readUnsignedShort() throws IOException {
        try {
            byte[] bArr = this.mBytes;
            int i = this.mPos;
            this.mPos = i + 1;
            int a = bArr[i] & 255;
            byte[] bArr2 = this.mBytes;
            int i2 = this.mPos;
            this.mPos = i2 + 1;
            return (a << 8) | (bArr2[i2] & 255);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EOFException();
        }
    }

    public int skipBytes(int length) {
        if (length > this.mLength - this.mPos) {
            length = this.mLength - this.mPos;
        }
        this.mPos += length;
        return length;
    }

    public static String convertUTF8WithBuf(byte[] buf, char[] out, int offset, int utfSize) throws UTFDataFormatException {
        int count = 0;
        int s = 0;
        while (count < utfSize) {
            int count2 = count + 1;
            char c = (char) buf[count + offset];
            out[s] = c;
            if (c < 128) {
                s++;
                count = count2;
            } else {
                c = out[s];
                char a = c;
                int s2;
                StringBuilder stringBuilder;
                StringBuilder stringBuilder2;
                if ((c & 224) == 192) {
                    if (count2 < utfSize) {
                        count = count2 + 1;
                        count2 = buf[count2 + offset];
                        if ((count2 & 192) == 128) {
                            s2 = s + 1;
                            out[s] = (char) (((a & 31) << 6) | (count2 & 63));
                            s = s2;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Second byte at ");
                            stringBuilder.append(count - 1);
                            stringBuilder.append(" does not match UTF8 Specification");
                            throw new UTFDataFormatException(stringBuilder.toString());
                        }
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Second byte at ");
                    stringBuilder2.append(count2);
                    stringBuilder2.append(" does not match ");
                    stringBuilder2.append("UTF8 Specification");
                    throw new UTFDataFormatException(stringBuilder2.toString());
                } else if ((a & 240) != 224) {
                    count = new StringBuilder();
                    count.append("Input at ");
                    count.append(count2 - 1);
                    count.append(" does not match UTF8 ");
                    count.append("Specification");
                    throw new UTFDataFormatException(count.toString());
                } else if (count2 + 1 < utfSize) {
                    count = count2 + 1;
                    count2 = buf[count2 + offset];
                    int count3 = count + 1;
                    count = buf[count + offset];
                    if ((count2 & 192) == 128 && (count & 192) == 128) {
                        s2 = s + 1;
                        out[s] = (char) ((((a & 15) << 12) | ((count2 & 63) << 6)) | (count & 63));
                        s = s2;
                        count = count3;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Second or third byte at ");
                        stringBuilder.append(count3 - 2);
                        stringBuilder.append(" does not match UTF8 Specification");
                        throw new UTFDataFormatException(stringBuilder.toString());
                    }
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Third byte at ");
                    stringBuilder2.append(count2 + 1);
                    stringBuilder2.append(" does not match UTF8 Specification");
                    throw new UTFDataFormatException(stringBuilder2.toString());
                }
            }
        }
        return new String(out, 0, s);
    }
}
