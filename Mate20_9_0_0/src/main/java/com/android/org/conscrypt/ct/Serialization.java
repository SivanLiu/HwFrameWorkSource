package com.android.org.conscrypt.ct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Serialization {
    private static final int DER_LENGTH_LONG_FORM_FLAG = 128;
    private static final int DER_TAG_MASK = 63;
    private static final int DER_TAG_OCTET_STRING = 4;

    private Serialization() {
    }

    public static byte[] readDEROctetString(byte[] input) throws SerializationException {
        return readDEROctetString(new ByteArrayInputStream(input));
    }

    public static byte[] readDEROctetString(InputStream input) throws SerializationException {
        int tag = readByte(input) & DER_TAG_MASK;
        if (tag == DER_TAG_OCTET_STRING) {
            int length;
            int width = readNumber(input, 1);
            if ((width & 128) != 0) {
                length = readNumber(input, width & -129);
            } else {
                length = width;
            }
            return readFixedBytes(input, length);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Wrong DER tag, expected OCTET STRING, got ");
        stringBuilder.append(tag);
        throw new SerializationException(stringBuilder.toString());
    }

    public static byte[][] readList(byte[] input, int listWidth, int elemWidth) throws SerializationException {
        return readList(new ByteArrayInputStream(input), listWidth, elemWidth);
    }

    public static byte[][] readList(InputStream input, int listWidth, int elemWidth) throws SerializationException {
        ArrayList<byte[]> result = new ArrayList();
        input = new ByteArrayInputStream(readVariableBytes(input, listWidth));
        while (input.available() > 0) {
            try {
                result.add(readVariableBytes(input, elemWidth));
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
        return (byte[][]) result.toArray(new byte[result.size()][]);
    }

    public static byte[] readVariableBytes(InputStream input, int width) throws SerializationException {
        return readFixedBytes(input, readNumber(input, width));
    }

    public static byte[] readFixedBytes(InputStream input, int length) throws SerializationException {
        if (length >= 0) {
            try {
                byte[] data = new byte[length];
                int count = input.read(data);
                if (count >= length) {
                    return data;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Premature end of input, expected ");
                stringBuilder.append(length);
                stringBuilder.append(" bytes, only read ");
                stringBuilder.append(count);
                throw new SerializationException(stringBuilder.toString());
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Negative length: ");
        stringBuilder2.append(length);
        throw new SerializationException(stringBuilder2.toString());
    }

    public static int readNumber(InputStream input, int width) throws SerializationException {
        if (width > DER_TAG_OCTET_STRING || width < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid width: ");
            stringBuilder.append(width);
            throw new SerializationException(stringBuilder.toString());
        }
        int result = 0;
        for (int i = 0; i < width; i++) {
            result = (result << 8) | (readByte(input) & 255);
        }
        return result;
    }

    public static long readLong(InputStream input, int width) throws SerializationException {
        if (width > 8 || width < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid width: ");
            stringBuilder.append(width);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        long result = 0;
        for (int i = 0; i < width; i++) {
            result = (result << 8) | ((long) (readByte(input) & 255));
        }
        return result;
    }

    public static byte readByte(InputStream input) throws SerializationException {
        try {
            int b = input.read();
            if (b != -1) {
                return (byte) b;
            }
            throw new SerializationException("Premature end of input, could not read byte.");
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public static void writeVariableBytes(OutputStream output, byte[] data, int width) throws SerializationException {
        writeNumber(output, (long) data.length, width);
        writeFixedBytes(output, data);
    }

    public static void writeFixedBytes(OutputStream output, byte[] data) throws SerializationException {
        try {
            output.write(data);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public static void writeNumber(OutputStream output, long value, int width) throws SerializationException {
        StringBuilder stringBuilder;
        if (width < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Negative width: ");
            stringBuilder.append(width);
            throw new SerializationException(stringBuilder.toString());
        } else if (width >= 8 || value < (1 << (8 * width))) {
            while (width > 0) {
                long shift = ((long) (width - 1)) * 8;
                if (shift < 64) {
                    try {
                        output.write((byte) ((int) ((value >> ((int) shift)) & 255)));
                    } catch (IOException e) {
                        throw new SerializationException(e);
                    }
                }
                output.write(0);
                width--;
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Number too large, ");
            stringBuilder.append(value);
            stringBuilder.append(" does not fit in ");
            stringBuilder.append(width);
            stringBuilder.append(" bytes");
            throw new SerializationException(stringBuilder.toString());
        }
    }
}
