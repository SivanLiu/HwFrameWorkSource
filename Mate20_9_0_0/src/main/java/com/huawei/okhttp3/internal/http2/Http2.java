package com.huawei.okhttp3.internal.http2;

import com.huawei.okhttp3.internal.Util;
import com.huawei.okio.ByteString;
import java.io.IOException;

public final class Http2 {
    static final String[] BINARY = new String[256];
    static final ByteString CONNECTION_PREFACE = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
    static final String[] FLAGS = new String[64];
    static final byte FLAG_ACK = (byte) 1;
    static final byte FLAG_COMPRESSED = (byte) 32;
    static final byte FLAG_END_HEADERS = (byte) 4;
    static final byte FLAG_END_PUSH_PROMISE = (byte) 4;
    static final byte FLAG_END_STREAM = (byte) 1;
    static final byte FLAG_NONE = (byte) 0;
    static final byte FLAG_PADDED = (byte) 8;
    static final byte FLAG_PRIORITY = (byte) 32;
    private static final String[] FRAME_NAMES = new String[]{"DATA", "HEADERS", "PRIORITY", "RST_STREAM", "SETTINGS", "PUSH_PROMISE", "PING", "GOAWAY", "WINDOW_UPDATE", "CONTINUATION"};
    static final int INITIAL_MAX_FRAME_SIZE = 16384;
    static final byte TYPE_CONTINUATION = (byte) 9;
    static final byte TYPE_DATA = (byte) 0;
    static final byte TYPE_GOAWAY = (byte) 7;
    static final byte TYPE_HEADERS = (byte) 1;
    static final byte TYPE_PING = (byte) 6;
    static final byte TYPE_PRIORITY = (byte) 2;
    static final byte TYPE_PUSH_PROMISE = (byte) 5;
    static final byte TYPE_RST_STREAM = (byte) 3;
    static final byte TYPE_SETTINGS = (byte) 4;
    static final byte TYPE_WINDOW_UPDATE = (byte) 8;

    static {
        int i;
        int i2 = 0;
        for (int i3 = 0; i3 < BINARY.length; i3++) {
            BINARY[i3] = Util.format("%8s", Integer.toBinaryString(i3)).replace(' ', '0');
        }
        FLAGS[0] = "";
        FLAGS[1] = "END_STREAM";
        int[] prefixFlags = new int[]{1};
        FLAGS[8] = "PADDED";
        for (int prefixFlag : prefixFlags) {
            String[] strArr = FLAGS;
            i = prefixFlag | 8;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(FLAGS[prefixFlag]);
            stringBuilder.append("|PADDED");
            strArr[i] = stringBuilder.toString();
        }
        FLAGS[4] = "END_HEADERS";
        FLAGS[32] = "PRIORITY";
        FLAGS[36] = "END_HEADERS|PRIORITY";
        for (int prefixFlag2 : new int[]{4, 32, 36}) {
            for (int prefixFlag3 : prefixFlags) {
                String[] strArr2 = FLAGS;
                int i4 = prefixFlag3 | prefixFlag2;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(FLAGS[prefixFlag3]);
                stringBuilder2.append('|');
                stringBuilder2.append(FLAGS[prefixFlag2]);
                strArr2[i4] = stringBuilder2.toString();
                strArr2 = FLAGS;
                i4 = (prefixFlag3 | prefixFlag2) | 8;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(FLAGS[prefixFlag3]);
                stringBuilder2.append('|');
                stringBuilder2.append(FLAGS[prefixFlag2]);
                stringBuilder2.append("|PADDED");
                strArr2[i4] = stringBuilder2.toString();
            }
        }
        while (i2 < FLAGS.length) {
            if (FLAGS[i2] == null) {
                FLAGS[i2] = BINARY[i2];
            }
            i2++;
        }
    }

    private Http2() {
    }

    static IllegalArgumentException illegalArgument(String message, Object... args) {
        throw new IllegalArgumentException(Util.format(message, args));
    }

    static IOException ioException(String message, Object... args) throws IOException {
        throw new IOException(Util.format(message, args));
    }

    static String frameLog(boolean inbound, int streamId, int length, byte type, byte flags) {
        String formattedType = type < FRAME_NAMES.length ? FRAME_NAMES[type] : Util.format("0x%02x", Byte.valueOf(type));
        String formattedFlags = formatFlags(type, flags);
        String str = "%s 0x%08x %5d %-13s %s";
        Object[] objArr = new Object[5];
        objArr[0] = inbound ? "<<" : ">>";
        objArr[1] = Integer.valueOf(streamId);
        objArr[2] = Integer.valueOf(length);
        objArr[3] = formattedType;
        objArr[4] = formattedFlags;
        return Util.format(str, objArr);
    }

    static String formatFlags(byte type, byte flags) {
        if (flags == (byte) 0) {
            return "";
        }
        switch (type) {
            case (byte) 2:
            case (byte) 3:
            case (byte) 7:
            case (byte) 8:
                return BINARY[flags];
            case (byte) 4:
            case (byte) 6:
                return flags == (byte) 1 ? "ACK" : BINARY[flags];
            default:
                String result;
                if (flags < FLAGS.length) {
                    result = FLAGS[flags];
                } else {
                    result = BINARY[flags];
                }
                if (type == (byte) 5 && (flags & 4) != 0) {
                    return result.replace("HEADERS", "PUSH_PROMISE");
                }
                if (type != (byte) 0 || (flags & 32) == 0) {
                    return result;
                }
                return result.replace("PRIORITY", "COMPRESSED");
        }
    }
}
