package com.android.okhttp.okio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ByteString implements Serializable, Comparable<ByteString> {
    public static final ByteString EMPTY = of(new byte[0]);
    static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final long serialVersionUID = 1;
    final byte[] data;
    transient int hashCode;
    transient String utf8;

    ByteString(byte[] data) {
        this.data = data;
    }

    public static ByteString of(byte... data) {
        if (data != null) {
            return new ByteString((byte[]) data.clone());
        }
        throw new IllegalArgumentException("data == null");
    }

    public static ByteString of(byte[] data, int offset, int byteCount) {
        if (data != null) {
            Util.checkOffsetAndCount((long) data.length, (long) offset, (long) byteCount);
            byte[] copy = new byte[byteCount];
            System.arraycopy(data, offset, copy, 0, byteCount);
            return new ByteString(copy);
        }
        throw new IllegalArgumentException("data == null");
    }

    public static ByteString encodeUtf8(String s) {
        if (s != null) {
            ByteString byteString = new ByteString(s.getBytes(Util.UTF_8));
            byteString.utf8 = s;
            return byteString;
        }
        throw new IllegalArgumentException("s == null");
    }

    public String utf8() {
        String result = this.utf8;
        if (result != null) {
            return result;
        }
        String str = new String(this.data, Util.UTF_8);
        this.utf8 = str;
        return str;
    }

    public String base64() {
        return Base64.encode(this.data);
    }

    public ByteString md5() {
        return digest("MD5");
    }

    public ByteString sha256() {
        return digest("SHA-256");
    }

    private ByteString digest(String digest) {
        try {
            return of(MessageDigest.getInstance(digest).digest(this.data));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public String base64Url() {
        return Base64.encodeUrl(this.data);
    }

    public static ByteString decodeBase64(String base64) {
        if (base64 != null) {
            byte[] decoded = Base64.decode(base64);
            return decoded != null ? new ByteString(decoded) : null;
        } else {
            throw new IllegalArgumentException("base64 == null");
        }
    }

    public String hex() {
        char[] result = new char[(this.data.length * 2)];
        int c = 0;
        for (byte b : this.data) {
            int c2 = c + 1;
            result[c] = HEX_DIGITS[(b >> 4) & 15];
            c = c2 + 1;
            result[c2] = HEX_DIGITS[b & 15];
        }
        return new String(result);
    }

    public static ByteString decodeHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex == null");
        } else if (hex.length() % 2 == 0) {
            byte[] result = new byte[(hex.length() / 2)];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) ((decodeHexDigit(hex.charAt(i * 2)) << 4) + decodeHexDigit(hex.charAt((i * 2) + 1)));
            }
            return of(result);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected hex string: ");
            stringBuilder.append(hex);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 97) + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 65) + 10;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected hex digit: ");
        stringBuilder.append(c);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ByteString read(InputStream in, int byteCount) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("in == null");
        } else if (byteCount >= 0) {
            byte[] result = new byte[byteCount];
            int offset = 0;
            while (offset < byteCount) {
                int read = in.read(result, offset, byteCount - offset);
                if (read != -1) {
                    offset += read;
                } else {
                    throw new EOFException();
                }
            }
            return new ByteString(result);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("byteCount < 0: ");
            stringBuilder.append(byteCount);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public ByteString toAsciiLowercase() {
        int i = 0;
        while (i < this.data.length) {
            byte c = this.data[i];
            if (c < (byte) 65 || c > (byte) 90) {
                i++;
            } else {
                byte[] lowercase = (byte[]) this.data.clone();
                lowercase[i] = (byte) (c + 32);
                for (int i2 = i + 1; i2 < lowercase.length; i2++) {
                    c = lowercase[i2];
                    if (c >= (byte) 65 && c <= (byte) 90) {
                        lowercase[i2] = (byte) (c + 32);
                    }
                }
                return new ByteString(lowercase);
            }
        }
        return this;
    }

    public ByteString toAsciiUppercase() {
        int i = 0;
        while (i < this.data.length) {
            byte c = this.data[i];
            if (c < (byte) 97 || c > (byte) 122) {
                i++;
            } else {
                byte[] lowercase = (byte[]) this.data.clone();
                lowercase[i] = (byte) (c - 32);
                for (int i2 = i + 1; i2 < lowercase.length; i2++) {
                    c = lowercase[i2];
                    if (c >= (byte) 97 && c <= (byte) 122) {
                        lowercase[i2] = (byte) (c - 32);
                    }
                }
                return new ByteString(lowercase);
            }
        }
        return this;
    }

    public ByteString substring(int beginIndex) {
        return substring(beginIndex, this.data.length);
    }

    public ByteString substring(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex < 0");
        } else if (endIndex <= this.data.length) {
            int subLen = endIndex - beginIndex;
            if (subLen < 0) {
                throw new IllegalArgumentException("endIndex < beginIndex");
            } else if (beginIndex == 0 && endIndex == this.data.length) {
                return this;
            } else {
                byte[] copy = new byte[subLen];
                System.arraycopy(this.data, beginIndex, copy, 0, subLen);
                return new ByteString(copy);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("endIndex > length(");
            stringBuilder.append(this.data.length);
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public byte getByte(int pos) {
        return this.data[pos];
    }

    public int size() {
        return this.data.length;
    }

    public byte[] toByteArray() {
        return (byte[]) this.data.clone();
    }

    public void write(OutputStream out) throws IOException {
        if (out != null) {
            out.write(this.data);
            return;
        }
        throw new IllegalArgumentException("out == null");
    }

    void write(Buffer buffer) {
        buffer.write(this.data, 0, this.data.length);
    }

    public boolean rangeEquals(int offset, ByteString other, int otherOffset, int byteCount) {
        return other.rangeEquals(otherOffset, this.data, offset, byteCount);
    }

    public boolean rangeEquals(int offset, byte[] other, int otherOffset, int byteCount) {
        return offset <= this.data.length - byteCount && otherOffset <= other.length - byteCount && Util.arrayRangeEquals(this.data, offset, other, otherOffset, byteCount);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (o == this) {
            return true;
        }
        if (!((o instanceof ByteString) && ((ByteString) o).size() == this.data.length && ((ByteString) o).rangeEquals(0, this.data, 0, this.data.length))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int result = this.hashCode;
        if (result != 0) {
            return result;
        }
        int hashCode = Arrays.hashCode(this.data);
        this.hashCode = hashCode;
        return hashCode;
    }

    public int compareTo(ByteString byteString) {
        int sizeA = size();
        int sizeB = byteString.size();
        int i = 0;
        int size = Math.min(sizeA, sizeB);
        while (true) {
            int i2 = -1;
            if (i < size) {
                int byteA = getByte(i) & 255;
                int byteB = byteString.getByte(i) & 255;
                if (byteA == byteB) {
                    i++;
                } else {
                    if (byteA >= byteB) {
                        i2 = 1;
                    }
                    return i2;
                }
            } else if (sizeA == sizeB) {
                return 0;
            } else {
                if (sizeA >= sizeB) {
                    i2 = 1;
                }
                return i2;
            }
        }
    }

    public String toString() {
        if (this.data.length == 0) {
            return "ByteString[size=0]";
        }
        if (this.data.length <= 16) {
            return String.format("ByteString[size=%s data=%s]", new Object[]{Integer.valueOf(this.data.length), hex()});
        }
        return String.format("ByteString[size=%s md5=%s]", new Object[]{Integer.valueOf(this.data.length), md5().hex()});
    }

    private void readObject(ObjectInputStream in) throws IOException {
        ByteString byteString = read(in, in.readInt());
        try {
            Field field = ByteString.class.getDeclaredField("data");
            field.setAccessible(true);
            field.set(this, byteString.data);
        } catch (NoSuchFieldException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e2) {
            throw new AssertionError();
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(this.data.length);
        out.write(this.data);
    }
}
