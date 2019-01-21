package sun.security.util;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public class Debug {
    private static String args;
    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
    private String prefix;

    public static Debug getInstance(String option) {
        return getInstance(option, option);
    }

    public static Debug getInstance(String option, String prefix) {
        if (!isOn(option)) {
            return null;
        }
        Debug d = new Debug();
        d.prefix = prefix;
        return d;
    }

    public static boolean isOn(String option) {
        boolean z = false;
        if (args == null) {
            return false;
        }
        if (args.indexOf("all") != -1) {
            return true;
        }
        if (args.indexOf(option) != -1) {
            z = true;
        }
        return z;
    }

    public void println(String message) {
        PrintStream printStream = System.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.prefix);
        stringBuilder.append(": ");
        stringBuilder.append(message);
        printStream.println(stringBuilder.toString());
    }

    public void println() {
        PrintStream printStream = System.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.prefix);
        stringBuilder.append(":");
        printStream.println(stringBuilder.toString());
    }

    public static String toHexString(BigInteger b) {
        String hexValue = b.toString(16);
        StringBuffer buf = new StringBuffer(hexValue.length() * 2);
        if (hexValue.startsWith(LanguageTag.SEP)) {
            buf.append("   -");
            hexValue = hexValue.substring(1);
        } else {
            buf.append("    ");
        }
        if (hexValue.length() % 2 != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0");
            stringBuilder.append(hexValue);
            hexValue = stringBuilder.toString();
        }
        int i = 0;
        while (i < hexValue.length()) {
            buf.append(hexValue.substring(i, i + 2));
            i += 2;
            if (i != hexValue.length()) {
                if (i % 64 == 0) {
                    buf.append("\n    ");
                } else if (i % 8 == 0) {
                    buf.append(" ");
                }
            }
        }
        return buf.toString();
    }

    private static String marshal(String args) {
        if (args == null) {
            return null;
        }
        StringBuffer target = new StringBuffer();
        StringBuffer source = new StringBuffer(args);
        String keyReg = "[Pp][Ee][Rr][Mm][Ii][Ss][Ss][Ii][Oo][Nn]=";
        String keyStr = "permission=";
        String reg = new StringBuilder();
        reg.append(keyReg);
        reg.append("[a-zA-Z_$][a-zA-Z0-9_$]*([.][a-zA-Z_$][a-zA-Z0-9_$]*)*");
        Matcher matcher = Pattern.compile(reg.toString()).matcher(source);
        StringBuffer left = new StringBuffer();
        while (matcher.find()) {
            target.append(matcher.group().replaceFirst(keyReg, keyStr));
            target.append("  ");
            matcher.appendReplacement(left, "");
        }
        matcher.appendTail(left);
        source = left;
        keyReg = "[Cc][Oo][Dd][Ee][Bb][Aa][Ss][Ee]=";
        keyStr = "codebase=";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(keyReg);
        stringBuilder.append("[^, ;]*");
        matcher = Pattern.compile(stringBuilder.toString()).matcher(source);
        left = new StringBuffer();
        while (matcher.find()) {
            target.append(matcher.group().replaceFirst(keyReg, keyStr));
            target.append("  ");
            matcher.appendReplacement(left, "");
        }
        matcher.appendTail(left);
        target.append(left.toString().toLowerCase(Locale.ENGLISH));
        return target.toString();
    }

    public static String toString(byte[] b) {
        if (b == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 255;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 15]);
        }
        return sb.toString();
    }
}
