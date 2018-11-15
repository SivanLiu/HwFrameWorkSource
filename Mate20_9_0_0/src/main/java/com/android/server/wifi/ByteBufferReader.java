package com.android.server.wifi;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferReader {
    @VisibleForTesting
    public static final int MAXIMUM_INTEGER_SIZE = 8;
    @VisibleForTesting
    public static final int MINIMUM_INTEGER_SIZE = 1;

    public static long readInteger(ByteBuffer payload, ByteOrder byteOrder, int size) {
        if (size < 1 || size > 8) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid size ");
            stringBuilder.append(size);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        byte[] octets = new byte[size];
        payload.get(octets);
        long value = 0;
        int n;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            n = octets.length - 1;
            while (true) {
                int n2 = n;
                if (n2 < 0) {
                    break;
                }
                value = (value << 8) | ((long) (octets[n2] & Constants.BYTE_MASK));
                n = n2 - 1;
            }
        } else {
            for (byte octet : octets) {
                value = (value << 8) | ((long) (octet & Constants.BYTE_MASK));
            }
        }
        return value;
    }

    public static String readString(ByteBuffer payload, int length, Charset charset) {
        byte[] octets = new byte[length];
        payload.get(octets);
        return new String(octets, charset);
    }

    public static String readStringWithByteLength(ByteBuffer payload, Charset charset) {
        return readString(payload, payload.get() & Constants.BYTE_MASK, charset);
    }
}
