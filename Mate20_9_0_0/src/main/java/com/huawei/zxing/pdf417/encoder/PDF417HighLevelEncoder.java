package com.huawei.zxing.pdf417.encoder;

import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.zxing.WriterException;
import java.math.BigInteger;
import java.util.Arrays;

final class PDF417HighLevelEncoder {
    private static final int BYTE_COMPACTION = 1;
    private static final int LATCH_TO_BYTE = 924;
    private static final int LATCH_TO_BYTE_PADDED = 901;
    private static final int LATCH_TO_NUMERIC = 902;
    private static final int LATCH_TO_TEXT = 900;
    private static final byte[] MIXED = new byte[AppOpsManagerEx.TYPE_MICROPHONE];
    private static final int NUMERIC_COMPACTION = 2;
    private static final byte[] PUNCTUATION = new byte[AppOpsManagerEx.TYPE_MICROPHONE];
    private static final int SHIFT_TO_BYTE = 913;
    private static final int SUBMODE_ALPHA = 0;
    private static final int SUBMODE_LOWER = 1;
    private static final int SUBMODE_MIXED = 2;
    private static final int SUBMODE_PUNCTUATION = 3;
    private static final int TEXT_COMPACTION = 0;
    private static final byte[] TEXT_MIXED_RAW = new byte[]{(byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 38, (byte) 13, (byte) 9, (byte) 44, (byte) 58, (byte) 35, (byte) 45, (byte) 46, (byte) 36, (byte) 47, (byte) 43, (byte) 37, (byte) 42, (byte) 61, (byte) 94, (byte) 0, (byte) 32, (byte) 0, (byte) 0, (byte) 0};
    private static final byte[] TEXT_PUNCTUATION_RAW = new byte[]{(byte) 59, (byte) 60, (byte) 62, (byte) 64, (byte) 91, (byte) 92, (byte) 93, (byte) 95, (byte) 96, (byte) 126, (byte) 33, (byte) 13, (byte) 9, (byte) 44, (byte) 58, (byte) 10, (byte) 45, (byte) 46, (byte) 36, (byte) 47, (byte) 34, (byte) 124, (byte) 42, (byte) 40, (byte) 41, (byte) 63, (byte) 123, (byte) 125, (byte) 39, (byte) 0};

    static {
        Arrays.fill(MIXED, (byte) -1);
        byte i = (byte) 0;
        for (byte i2 = (byte) 0; i2 < TEXT_MIXED_RAW.length; i2 = (byte) (i2 + 1)) {
            byte b = TEXT_MIXED_RAW[i2];
            if (b > (byte) 0) {
                MIXED[b] = i2;
            }
        }
        Arrays.fill(PUNCTUATION, (byte) -1);
        while (i < TEXT_PUNCTUATION_RAW.length) {
            byte b2 = TEXT_PUNCTUATION_RAW[i];
            if (b2 > (byte) 0) {
                PUNCTUATION[b2] = i;
            }
            i = (byte) (i + 1);
        }
    }

    private PDF417HighLevelEncoder() {
    }

    private static byte[] getBytesForMessage(String msg) {
        return msg.getBytes();
    }

    static String encodeHighLevel(String msg, Compaction compaction) throws WriterException {
        StringBuilder sb = new StringBuilder(msg.length());
        int len = msg.length();
        int p = 0;
        int textSubMode = 0;
        if (compaction == Compaction.TEXT) {
            encodeText(msg, 0, len, sb, 0);
        } else if (compaction == Compaction.BYTE) {
            byte[] bytes = getBytesForMessage(msg);
            encodeBinary(bytes, 0, bytes.length, 1, sb);
        } else if (compaction == Compaction.NUMERIC) {
            sb.append(902);
            encodeNumeric(msg, 0, len, sb);
        } else {
            byte[] bytes2 = null;
            int encodingMode = 0;
            while (p < len) {
                int n = determineConsecutiveDigitCount(msg, p);
                if (n >= 13) {
                    sb.append(902);
                    encodingMode = 2;
                    textSubMode = 0;
                    encodeNumeric(msg, p, n, sb);
                    p += n;
                } else {
                    int t = determineConsecutiveTextCount(msg, p);
                    if (t >= 5 || n == len) {
                        if (encodingMode != 0) {
                            sb.append(900);
                            encodingMode = 0;
                            textSubMode = 0;
                        }
                        textSubMode = encodeText(msg, p, t, sb, textSubMode);
                        p += t;
                    } else {
                        if (bytes2 == null) {
                            bytes2 = getBytesForMessage(msg);
                        }
                        int b = determineConsecutiveBinaryCount(msg, bytes2, p);
                        if (b == 0) {
                            b = 1;
                        }
                        if (b == 1 && encodingMode == 0) {
                            encodeBinary(bytes2, p, 1, 0, sb);
                        } else {
                            encodeBinary(bytes2, p, b, encodingMode, sb);
                            encodingMode = 1;
                            textSubMode = 0;
                        }
                        p += b;
                    }
                }
            }
        }
        return sb.toString();
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int encodeText(CharSequence msg, int startpos, int count, StringBuilder sb, int initialSubmode) {
        CharSequence charSequence = msg;
        int i = count;
        StringBuilder stringBuilder = sb;
        StringBuilder tmp = new StringBuilder(i);
        int submode = initialSubmode;
        int idx = 0;
        while (true) {
            char ch = charSequence.charAt(startpos + idx);
            switch (submode) {
                case 0:
                    if (isAlphaUpper(ch)) {
                        if (ch == ' ') {
                            tmp.append(26);
                        } else {
                            tmp.append((char) (ch - 65));
                        }
                    } else if (isAlphaLower(ch)) {
                        submode = 1;
                        tmp.append(27);
                        break;
                    } else if (isMixed(ch)) {
                        submode = 2;
                        tmp.append(28);
                        break;
                    } else {
                        tmp.append(29);
                        tmp.append((char) PUNCTUATION[ch]);
                    }
                case 1:
                    if (isAlphaLower(ch)) {
                        if (ch == ' ') {
                            tmp.append(26);
                        } else {
                            tmp.append((char) (ch - 97));
                        }
                    } else if (isAlphaUpper(ch)) {
                        tmp.append(27);
                        tmp.append((char) (ch - 65));
                    } else if (isMixed(ch)) {
                        submode = 2;
                        tmp.append(28);
                        break;
                    } else {
                        tmp.append(29);
                        tmp.append((char) PUNCTUATION[ch]);
                    }
                case 2:
                    if (!isMixed(ch)) {
                        if (!isAlphaUpper(ch)) {
                            if (!isAlphaLower(ch)) {
                                if ((startpos + idx) + 1 < i && isPunctuation(charSequence.charAt((startpos + idx) + 1))) {
                                    submode = 3;
                                    tmp.append(25);
                                    break;
                                }
                                tmp.append(29);
                                tmp.append((char) PUNCTUATION[ch]);
                            } else {
                                submode = 1;
                                tmp.append(27);
                                break;
                            }
                        }
                        submode = 0;
                        tmp.append(28);
                        break;
                    }
                    tmp.append((char) MIXED[ch]);
                    break;
                default:
                    if (!isPunctuation(ch)) {
                        submode = 0;
                        tmp.append(29);
                        break;
                    }
                    tmp.append((char) PUNCTUATION[ch]);
                    idx++;
                    if (idx < i) {
                        break;
                    }
                    char len = tmp.length();
                    char h = 0;
                    for (ch = 0; ch < len; ch++) {
                        if (ch % 2 != 0) {
                            h = (char) ((h * 30) + tmp.charAt(ch));
                            stringBuilder.append(h);
                        } else {
                            h = tmp.charAt(ch);
                        }
                    }
                    if (len % 2 != 0) {
                        stringBuilder.append((char) ((h * 30) + 29));
                    }
                    return submode;
            }
        }
    }

    private static void encodeBinary(byte[] bytes, int startpos, int count, int startmode, StringBuilder sb) {
        int i = count;
        StringBuilder stringBuilder = sb;
        if (i == 1 && startmode == 0) {
            stringBuilder.append(913);
        } else {
            if (i % 6 == 0) {
                stringBuilder.append(924);
            } else {
                stringBuilder.append(901);
            }
        }
        int idx = startpos;
        int i2 = 6;
        if (i >= 6) {
            int i3 = 5;
            char[] chars = new char[5];
            while ((startpos + i) - idx >= i2) {
                int i4;
                long t = 0;
                for (i4 = 0; i4 < i2; i4++) {
                    t = (t << 8) + ((long) (bytes[idx + i4] & 255));
                }
                i4 = 0;
                for (i3 = 
/*
Method generation error in method: com.huawei.zxing.pdf417.encoder.PDF417HighLevelEncoder.encodeBinary(byte[], int, int, int, java.lang.StringBuilder):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r6_1 'i3' int) = (r6_0 'i3' int), (r6_5 'i3' int) binds: {(r6_0 'i3' int)=B:13:0x0028, (r6_5 'i3' int)=B:25:0x0061} in method: com.huawei.zxing.pdf417.encoder.PDF417HighLevelEncoder.encodeBinary(byte[], int, int, int, java.lang.StringBuilder):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:220)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 25 more

*/

    private static void encodeNumeric(String msg, int startpos, int count, StringBuilder sb) {
        int idx = 0;
        StringBuilder tmp = new StringBuilder((count / 3) + 1);
        BigInteger num900 = BigInteger.valueOf(900);
        BigInteger num0 = BigInteger.valueOf(null);
        while (idx < count - 1) {
            tmp.setLength(0);
            int len = Math.min(44, count - idx);
            String part = new StringBuilder();
            part.append('1');
            part.append(msg.substring(startpos + idx, (startpos + idx) + len));
            BigInteger bigint = new BigInteger(part.toString());
            do {
                tmp.append((char) bigint.mod(num900).intValue());
                bigint = bigint.divide(num900);
            } while (!bigint.equals(num0));
            for (int i = tmp.length() - 1; i >= 0; i--) {
                sb.append(tmp.charAt(i));
            }
            idx += len;
        }
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isAlphaUpper(char ch) {
        return ch == ' ' || (ch >= 'A' && ch <= 'Z');
    }

    private static boolean isAlphaLower(char ch) {
        return ch == ' ' || (ch >= 'a' && ch <= 'z');
    }

    private static boolean isMixed(char ch) {
        return MIXED[ch] != (byte) -1;
    }

    private static boolean isPunctuation(char ch) {
        return PUNCTUATION[ch] != (byte) -1;
    }

    private static boolean isText(char ch) {
        return ch == 9 || ch == 10 || ch == 13 || (ch >= ' ' && ch <= '~');
    }

    private static int determineConsecutiveDigitCount(CharSequence msg, int startpos) {
        int count = 0;
        int len = msg.length();
        int idx = startpos;
        if (idx < len) {
            char ch = msg.charAt(idx);
            while (isDigit(ch) && idx < len) {
                count++;
                idx++;
                if (idx < len) {
                    ch = msg.charAt(idx);
                }
            }
        }
        return count;
    }

    private static int determineConsecutiveTextCount(CharSequence msg, int startpos) {
        int len = msg.length();
        int idx = startpos;
        while (idx < len) {
            char ch = msg.charAt(idx);
            int numericCount = 0;
            while (numericCount < 13 && isDigit(ch) && idx < len) {
                numericCount++;
                idx++;
                if (idx < len) {
                    ch = msg.charAt(idx);
                }
            }
            if (numericCount >= 13) {
                return (idx - startpos) - numericCount;
            }
            if (numericCount <= 0) {
                if (!isText(msg.charAt(idx))) {
                    break;
                }
                idx++;
            }
        }
        return idx - startpos;
    }

    private static int determineConsecutiveBinaryCount(CharSequence msg, byte[] bytes, int startpos) throws WriterException {
        int len = msg.length();
        int idx = startpos;
        while (idx < len) {
            int i;
            int textCount = 0;
            char ch = msg.charAt(idx);
            int numericCount = 0;
            while (numericCount < 13 && isDigit(ch)) {
                numericCount++;
                i = idx + numericCount;
                if (i >= len) {
                    break;
                }
                ch = msg.charAt(i);
            }
            if (numericCount >= 13) {
                return idx - startpos;
            }
            while (textCount < 5 && isText(ch)) {
                textCount++;
                i = idx + textCount;
                if (i >= len) {
                    break;
                }
                ch = msg.charAt(i);
            }
            if (textCount >= 5) {
                return idx - startpos;
            }
            ch = msg.charAt(idx);
            if (bytes[idx] != (byte) 63 || ch == '?') {
                idx++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Non-encodable character detected: ");
                stringBuilder.append(ch);
                stringBuilder.append(" (Unicode: ");
                stringBuilder.append(ch);
                stringBuilder.append(')');
                throw new WriterException(stringBuilder.toString());
            }
        }
        return idx - startpos;
    }
}
