package java.nio.charset;

import android.icu.text.Bidi;
import java.io.UTFDataFormatException;

public class ModifiedUtf8 {
    public static long countBytes(String s, boolean shortLength) throws UTFDataFormatException {
        long counter = 0;
        int strLen = s.length();
        for (int i = 0; i < strLen; i++) {
            char c = s.charAt(i);
            if (c < 128) {
                counter++;
                if (c == 0) {
                    counter++;
                }
            } else if (c < 2048) {
                counter += 2;
            } else {
                counter += 3;
            }
        }
        if (!shortLength || counter <= 65535) {
            return counter;
        }
        throw new UTFDataFormatException("Size of the encoded string doesn't fit in two bytes");
    }

    public static void encode(byte[] dst, int offset, String s) {
        int strLen = s.length();
        for (int i = 0; i < strLen; i++) {
            char c = s.charAt(i);
            int offset2;
            if (c < 128) {
                int offset3;
                if (c == 0) {
                    offset3 = offset + 1;
                    dst[offset] = (byte) -64;
                    offset = offset3 + 1;
                    dst[offset3] = Bidi.LEVEL_OVERRIDE;
                } else {
                    offset3 = offset + 1;
                    dst[offset] = (byte) c;
                    offset = offset3;
                }
            } else if (c < 2048) {
                offset2 = offset + 1;
                dst[offset] = (byte) ((c >>> 6) | 192);
                offset = offset2 + 1;
                dst[offset2] = (byte) (128 | (c & 63));
            } else {
                offset2 = offset + 1;
                dst[offset] = (byte) ((c >>> 12) | 224);
                offset = offset2 + 1;
                dst[offset2] = (byte) (((c >>> 6) & 63) | 128);
                offset2 = offset + 1;
                dst[offset] = (byte) (128 | (c & 63));
                offset = offset2;
            }
        }
    }

    public static byte[] encode(String s) throws UTFDataFormatException {
        long size = countBytes(s, true);
        byte[] output = new byte[(((int) size) + 2)];
        encode(output, 2, s);
        output[0] = (byte) ((int) (size >>> 8));
        output[1] = (byte) ((int) size);
        return output;
    }

    public static String decode(byte[] in, char[] out, int offset, int length) throws UTFDataFormatException {
        if (offset < 0 || length < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal arguments: offset ");
            stringBuilder.append(offset);
            stringBuilder.append(". Length: ");
            stringBuilder.append(length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        int outputIndex = 0;
        int limitIndex = offset + length;
        while (offset < limitIndex) {
            int i = in[offset] & 255;
            offset++;
            if (i < 128) {
                out[outputIndex] = (char) i;
                outputIndex++;
            } else {
                StringBuilder stringBuilder2;
                if (192 <= i && i < 224) {
                    i = (i & 31) << 6;
                    if (offset == limitIndex) {
                        throw new UTFDataFormatException("unexpected end of input");
                    } else if ((192 & in[offset]) == 128) {
                        out[outputIndex] = (char) ((in[offset] & 63) | i);
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("bad second byte at ");
                        stringBuilder2.append(offset);
                        throw new UTFDataFormatException(stringBuilder2.toString());
                    }
                } else if (i < 240) {
                    i = (i & 31) << 12;
                    if (offset + 1 >= limitIndex) {
                        throw new UTFDataFormatException("unexpected end of input");
                    } else if ((in[offset] & 192) == 128) {
                        i |= (in[offset] & 63) << 6;
                        offset++;
                        if ((192 & in[offset]) == 128) {
                            out[outputIndex] = (char) ((in[offset] & 63) | i);
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("bad third byte at ");
                            stringBuilder2.append(offset);
                            throw new UTFDataFormatException(stringBuilder2.toString());
                        }
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("bad second byte at ");
                        stringBuilder2.append(offset);
                        throw new UTFDataFormatException(stringBuilder2.toString());
                    }
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid UTF8 byte ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" at position ");
                    stringBuilder2.append(offset - 1);
                    throw new UTFDataFormatException(stringBuilder2.toString());
                }
                offset++;
                outputIndex++;
            }
        }
        return String.valueOf(out, 0, outputIndex);
    }
}
