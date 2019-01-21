package com.android.org.bouncycastle.asn1;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

class StreamUtil {
    StreamUtil() {
    }

    static int findLimit(InputStream in) {
        if (in instanceof LimitedInputStream) {
            return ((LimitedInputStream) in).getRemaining();
        }
        if (in instanceof ASN1InputStream) {
            return ((ASN1InputStream) in).getLimit();
        }
        if (in instanceof ByteArrayInputStream) {
            return ((ByteArrayInputStream) in).available();
        }
        long size;
        if (in instanceof FileInputStream) {
            try {
                FileChannel channel = ((FileInputStream) in).getChannel();
                size = channel != null ? channel.size() : 2147483647L;
                if (size < 2147483647L) {
                    return (int) size;
                }
            } catch (IOException e) {
            }
        }
        size = Runtime.getRuntime().maxMemory();
        if (size > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        return (int) size;
    }

    static int calculateBodyLength(int length) {
        int count = 1;
        if (length > 127) {
            int i;
            int size = 1;
            int val = length;
            while (true) {
                i = val >>> 8;
                val = i;
                if (i == 0) {
                    break;
                }
                size++;
            }
            for (i = (size - 1) * 8; i >= 0; i -= 8) {
                count++;
            }
        }
        return count;
    }

    static int calculateTagLength(int tagNo) throws IOException {
        if (tagNo < 31) {
            return 1;
        }
        if (tagNo < 128) {
            return 1 + 1;
        }
        byte[] stack = new byte[5];
        int pos = stack.length - 1;
        stack[pos] = (byte) (tagNo & 127);
        do {
            tagNo >>= 7;
            pos--;
            stack[pos] = (byte) ((tagNo & 127) | 128);
        } while (tagNo > 127);
        return 1 + (stack.length - pos);
    }
}
