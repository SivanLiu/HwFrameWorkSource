package android.icu.impl;

import android.icu.util.VersionInfo;
import java.io.PrintStream;

public final class ICUDebug {
    private static boolean debug = (params != null);
    private static boolean help;
    public static final boolean isJDK14OrHigher;
    public static final VersionInfo javaVersion = getInstanceLenient(javaVersionString);
    public static final String javaVersionString = System.getProperty("java.version", AndroidHardcodedSystemProperties.JAVA_VERSION);
    private static String params;

    static {
        try {
            params = System.getProperty("ICUDebug");
        } catch (SecurityException e) {
        }
        boolean z = false;
        boolean z2 = debug && (params.equals("") || params.indexOf("help") != -1);
        help = z2;
        if (debug) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\nICUDebug=");
            stringBuilder.append(params);
            printStream.println(stringBuilder.toString());
        }
        if (javaVersion.compareTo(VersionInfo.getInstance("1.4.0")) >= 0) {
            z = true;
        }
        isJDK14OrHigher = z;
    }

    public static VersionInfo getInstanceLenient(String s) {
        int[] ver = new int[4];
        char c = 0;
        boolean numeric = false;
        int vidx = 0;
        while (c < s.length()) {
            char i = c + 1;
            c = s.charAt(c);
            if (c < '0' || c > '9') {
                if (!numeric) {
                    continue;
                } else if (vidx != 3) {
                    numeric = false;
                    vidx++;
                }
                c = i;
            } else {
                if (numeric) {
                    ver[vidx] = (ver[vidx] * 10) + (c - 48);
                    if (ver[vidx] > 255) {
                        ver[vidx] = 0;
                    }
                } else {
                    numeric = true;
                    ver[vidx] = c - 48;
                }
                c = i;
            }
            c = i;
        }
        return VersionInfo.getInstance(ver[0], ver[1], ver[2], ver[3]);
    }

    public static boolean enabled() {
        return debug;
    }

    public static boolean enabled(String arg) {
        boolean z = false;
        if (!debug) {
            return false;
        }
        if (params.indexOf(arg) != -1) {
            z = true;
        }
        boolean result = z;
        if (help) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\nICUDebug.enabled(");
            stringBuilder.append(arg);
            stringBuilder.append(") = ");
            stringBuilder.append(result);
            printStream.println(stringBuilder.toString());
        }
        return result;
    }

    public static String value(String arg) {
        String result = "false";
        if (debug) {
            int index = params.indexOf(arg);
            if (index != -1) {
                index += arg.length();
                if (params.length() <= index || params.charAt(index) != '=') {
                    result = "true";
                } else {
                    index++;
                    int limit = params.indexOf(",", index);
                    result = params.substring(index, limit == -1 ? params.length() : limit);
                }
            }
            if (help) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\nICUDebug.value(");
                stringBuilder.append(arg);
                stringBuilder.append(") = ");
                stringBuilder.append(result);
                printStream.println(stringBuilder.toString());
            }
        }
        return result;
    }
}
