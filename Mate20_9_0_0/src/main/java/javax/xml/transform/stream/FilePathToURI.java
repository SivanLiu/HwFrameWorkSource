package javax.xml.transform.stream;

import android.icu.impl.PatternTokenizer;
import java.io.File;
import java.io.UnsupportedEncodingException;

class FilePathToURI {
    private static char[] gAfterEscaping1 = new char[128];
    private static char[] gAfterEscaping2 = new char[128];
    private static char[] gHexChs = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static boolean[] gNeedEscaping = new boolean[128];

    FilePathToURI() {
    }

    static {
        int i = 0;
        for (int i2 = 0; i2 <= 31; i2++) {
            gNeedEscaping[i2] = true;
            gAfterEscaping1[i2] = gHexChs[i2 >> 4];
            gAfterEscaping2[i2] = gHexChs[i2 & 15];
        }
        gNeedEscaping[127] = true;
        gAfterEscaping1[127] = '7';
        gAfterEscaping2[127] = 'F';
        char[] escChs = new char[]{' ', '<', '>', '#', '%', '\"', '{', '}', '|', PatternTokenizer.BACK_SLASH, '^', '~', '[', ']', '`'};
        int len = escChs.length;
        while (i < len) {
            char ch = escChs[i];
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 15];
            i++;
        }
    }

    public static String filepath2URI(String path) {
        if (path == null) {
            return null;
        }
        int i;
        path = path.replace(File.separatorChar, '/');
        int len = path.length();
        StringBuilder buffer = new StringBuilder(len * 3);
        buffer.append("file://");
        int i2 = 0;
        if (len >= 2 && path.charAt(1) == ':') {
            int ch = Character.toUpperCase(path.charAt(0));
            if (ch >= 65 && ch <= 90) {
                buffer.append('/');
            }
        }
        while (true) {
            i = i2;
            if (i >= len) {
                break;
            }
            i2 = path.charAt(i);
            if (i2 >= 128) {
                break;
            }
            if (gNeedEscaping[i2]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[i2]);
                buffer.append(gAfterEscaping2[i2]);
            } else {
                buffer.append((char) i2);
            }
            i2 = i + 1;
        }
        if (i < len) {
            try {
                for (byte b : path.substring(i).getBytes("UTF-8")) {
                    if (b < (byte) 0) {
                        int ch2 = b + 256;
                        buffer.append('%');
                        buffer.append(gHexChs[ch2 >> 4]);
                        buffer.append(gHexChs[ch2 & 15]);
                    } else if (gNeedEscaping[b]) {
                        buffer.append('%');
                        buffer.append(gAfterEscaping1[b]);
                        buffer.append(gAfterEscaping2[b]);
                    } else {
                        buffer.append((char) b);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                return path;
            }
        }
        return buffer.toString();
    }
}
