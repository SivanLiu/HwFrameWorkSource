package java.lang;

import android.icu.text.UTF16;
import dalvik.bytecode.Opcodes;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import libcore.util.CharsetUtils;
import libcore.util.EmptyArray;

public final class StringFactory {
    private static final char REPLACEMENT_CHAR = '�';
    private static final int[] TABLE_UTF8_NEEDED = new int[]{0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static native String newStringFromBytes(byte[] bArr, int i, int i2, int i3);

    static native String newStringFromChars(int i, int i2, char[] cArr);

    public static native String newStringFromString(String str);

    public static String newEmptyString() {
        return newStringFromChars(EmptyArray.CHAR, 0, 0);
    }

    public static String newStringFromBytes(byte[] data) {
        return newStringFromBytes(data, 0, data.length);
    }

    public static String newStringFromBytes(byte[] data, int high) {
        return newStringFromBytes(data, high, 0, data.length);
    }

    public static String newStringFromBytes(byte[] data, int offset, int byteCount) {
        return newStringFromBytes(data, offset, byteCount, Charset.defaultCharset());
    }

    public static String newStringFromBytes(byte[] data, int offset, int byteCount, String charsetName) throws UnsupportedEncodingException {
        return newStringFromBytes(data, offset, byteCount, Charset.forNameUEE(charsetName));
    }

    public static String newStringFromBytes(byte[] data, String charsetName) throws UnsupportedEncodingException {
        return newStringFromBytes(data, 0, data.length, Charset.forNameUEE(charsetName));
    }

    public static String newStringFromBytes(byte[] data, int offset, int byteCount, Charset charset) {
        byte[] bArr = data;
        int i = offset;
        int i2 = byteCount;
        Charset charset2;
        if ((i | i2) < 0 || i2 > bArr.length - i) {
            charset2 = charset;
            throw new StringIndexOutOfBoundsException(bArr.length, i, i2);
        }
        int idx;
        char[] value;
        int length;
        String canonicalCharsetName = charset.name();
        if (canonicalCharsetName.equals("UTF-8")) {
            int s;
            byte[] d = bArr;
            char[] v = new char[i2];
            idx = i;
            int last = i + i2;
            int s2 = 0;
            int codePoint = 0;
            int utf8BytesSeen = 0;
            int utf8BytesNeeded = 0;
            int lowerBound = 128;
            int upperBound = 191;
            while (idx < last) {
                int idx2 = idx + 1;
                idx = d[idx] & 255;
                if (utf8BytesNeeded == 0) {
                    if ((idx & 128) == 0) {
                        s = s2 + 1;
                        v[s2] = (char) idx;
                    } else if ((idx & 64) == 0) {
                        s = s2 + 1;
                        v[s2] = REPLACEMENT_CHAR;
                    } else {
                        utf8BytesNeeded = TABLE_UTF8_NEEDED[idx & 63];
                        if (utf8BytesNeeded == 0) {
                            int s3 = s2 + 1;
                            v[s2] = REPLACEMENT_CHAR;
                            idx = idx2;
                            s2 = s3;
                        } else {
                            codePoint = idx & (63 >> utf8BytesNeeded);
                            if (idx == 224) {
                                lowerBound = 160;
                            } else if (idx == 237) {
                                upperBound = 159;
                            } else if (idx == 240) {
                                lowerBound = 144;
                            } else if (idx == 244) {
                                upperBound = 143;
                            }
                        }
                    }
                    s2 = s;
                } else if (idx < lowerBound || idx > upperBound) {
                    s = s2 + 1;
                    v[s2] = REPLACEMENT_CHAR;
                    codePoint = 0;
                    utf8BytesNeeded = 0;
                    utf8BytesSeen = 0;
                    lowerBound = 128;
                    upperBound = 191;
                    idx = idx2 - 1;
                    s2 = s;
                } else {
                    lowerBound = 128;
                    upperBound = 191;
                    codePoint = (codePoint << 6) | (idx & 63);
                    utf8BytesSeen++;
                    if (utf8BytesNeeded == utf8BytesSeen) {
                        if (codePoint < 65536) {
                            s = s2 + 1;
                            v[s2] = (char) codePoint;
                        } else {
                            s = s2 + 1;
                            v[s2] = (char) ((codePoint >> 10) + 55232);
                            s2 = s + 1;
                            v[s] = (char) ((codePoint & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE);
                            s = s2;
                        }
                        codePoint = 0;
                        utf8BytesNeeded = 0;
                        utf8BytesSeen = 0;
                        s2 = s;
                    }
                }
                idx = idx2;
            }
            if (utf8BytesNeeded != 0) {
                s = s2 + 1;
                v[s2] = REPLACEMENT_CHAR;
            } else {
                s = s2;
            }
            if (s == i2) {
                value = v;
                length = s;
            } else {
                value = new char[s];
                length = s;
                byte[] bArr2 = d;
                System.arraycopy(v, 0, value, 0, s);
            }
        } else if (canonicalCharsetName.equals("ISO-8859-1")) {
            value = new char[i2];
            length = i2;
            CharsetUtils.isoLatin1BytesToChars(bArr, i, i2, value);
        } else if (canonicalCharsetName.equals("US-ASCII")) {
            value = new char[i2];
            length = i2;
            CharsetUtils.asciiBytesToChars(bArr, i, i2, value);
        } else {
            CharBuffer cb = charset.decode(ByteBuffer.wrap(data, offset, byteCount));
            length = cb.length();
            if (length > 0) {
                value = new char[length];
                idx = 0;
                System.arraycopy(cb.array(), 0, value, 0, length);
            } else {
                idx = 0;
                value = EmptyArray.CHAR;
            }
            return newStringFromChars(value, idx, length);
        }
        charset2 = charset;
        idx = 0;
        return newStringFromChars(value, idx, length);
    }

    public static String newStringFromBytes(byte[] data, Charset charset) {
        return newStringFromBytes(data, 0, data.length, charset);
    }

    public static String newStringFromChars(char[] data) {
        return newStringFromChars(data, 0, data.length);
    }

    public static String newStringFromChars(char[] data, int offset, int charCount) {
        if ((offset | charCount) >= 0 && charCount <= data.length - offset) {
            return newStringFromChars(offset, charCount, data);
        }
        throw new StringIndexOutOfBoundsException(data.length, offset, charCount);
    }

    public static String newStringFromStringBuffer(StringBuffer stringBuffer) {
        String newStringFromChars;
        synchronized (stringBuffer) {
            newStringFromChars = newStringFromChars(stringBuffer.getValue(), 0, stringBuffer.length());
        }
        return newStringFromChars;
    }

    public static String newStringFromCodePoints(int[] codePoints, int offset, int count) {
        if (codePoints == null) {
            throw new NullPointerException("codePoints == null");
        } else if ((offset | count) < 0 || count > codePoints.length - offset) {
            throw new StringIndexOutOfBoundsException(codePoints.length, offset, count);
        } else {
            char[] value = new char[(count * 2)];
            int length = 0;
            for (int i = offset; i < offset + count; i++) {
                length += Character.toChars(codePoints[i], value, length);
            }
            return newStringFromChars(value, 0, length);
        }
    }

    public static String newStringFromStringBuilder(StringBuilder stringBuilder) {
        return newStringFromChars(stringBuilder.getValue(), 0, stringBuilder.length());
    }
}
