package java.util.prefs;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

class Base64 {
    private static final byte[] altBase64ToInt = new byte[]{(byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) -1, (byte) 62, (byte) 9, (byte) 10, (byte) 11, (byte) -1, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 58, (byte) 59, (byte) 60, (byte) 61, (byte) 12, (byte) 13, (byte) 14, (byte) -1, (byte) 15, (byte) 63, (byte) 16, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE, (byte) -1, (byte) 18, (byte) 19, Character.START_PUNCTUATION, (byte) 20, Character.CURRENCY_SYMBOL, (byte) 27, (byte) 28, Character.INITIAL_QUOTE_PUNCTUATION, (byte) 30, (byte) 31, (byte) 32, (byte) 33, (byte) 34, (byte) 35, (byte) 36, (byte) 37, (byte) 38, (byte) 39, (byte) 40, (byte) 41, (byte) 42, (byte) 43, (byte) 44, (byte) 45, (byte) 46, (byte) 47, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 22, (byte) 23, (byte) 24, Character.MATH_SYMBOL};
    private static final byte[] base64ToInt = new byte[]{(byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) 62, (byte) -1, (byte) -1, (byte) -1, (byte) 63, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 58, (byte) 59, (byte) 60, (byte) 61, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE, (byte) 18, (byte) 19, (byte) 20, Character.START_PUNCTUATION, (byte) 22, (byte) 23, (byte) 24, Character.MATH_SYMBOL, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, Character.CURRENCY_SYMBOL, (byte) 27, (byte) 28, Character.INITIAL_QUOTE_PUNCTUATION, (byte) 30, (byte) 31, (byte) 32, (byte) 33, (byte) 34, (byte) 35, (byte) 36, (byte) 37, (byte) 38, (byte) 39, (byte) 40, (byte) 41, (byte) 42, (byte) 43, (byte) 44, (byte) 45, (byte) 46, (byte) 47, (byte) 48, (byte) 49, (byte) 50, (byte) 51};
    private static final char[] intToAltBase64 = new char[]{'!', '\"', '#', '$', '%', '&', '\'', '(', ')', ',', '-', '.', ':', ';', '<', '>', '@', '[', ']', '^', '`', '_', '{', '|', '}', '~', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '?'};
    private static final char[] intToBase64 = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};

    Base64() {
    }

    static String byteArrayToBase64(byte[] a) {
        return byteArrayToBase64(a, false);
    }

    static String byteArrayToAltBase64(byte[] a) {
        return byteArrayToBase64(a, true);
    }

    private static String byteArrayToBase64(byte[] a, boolean alternate) {
        int inCursor;
        int aLen = a.length;
        int numFullGroups = aLen / 3;
        int numBytesInPartialGroup = aLen - (3 * numFullGroups);
        StringBuffer result = new StringBuffer(4 * ((aLen + 2) / 3));
        char[] intToAlpha = alternate ? intToAltBase64 : intToBase64;
        int inCursor2 = 0;
        int i = 0;
        while (i < numFullGroups) {
            inCursor = inCursor2 + 1;
            inCursor2 = a[inCursor2] & 255;
            int inCursor3 = inCursor + 1;
            inCursor = a[inCursor] & 255;
            int inCursor4 = inCursor3 + 1;
            inCursor3 = a[inCursor3] & 255;
            result.append(intToAlpha[inCursor2 >> 2]);
            result.append(intToAlpha[((inCursor2 << 4) & 63) | (inCursor >> 4)]);
            result.append(intToAlpha[((inCursor << 2) & 63) | (inCursor3 >> 6)]);
            result.append(intToAlpha[inCursor3 & 63]);
            i++;
            inCursor2 = inCursor4;
        }
        if (numBytesInPartialGroup != 0) {
            i = inCursor2 + 1;
            inCursor2 = a[inCursor2] & 255;
            result.append(intToAlpha[inCursor2 >> 2]);
            if (numBytesInPartialGroup == 1) {
                result.append(intToAlpha[(inCursor2 << 4) & 63]);
                result.append("==");
                inCursor2 = i;
            } else {
                inCursor = i + 1;
                i = a[i] & 255;
                result.append(intToAlpha[((inCursor2 << 4) & 63) | (i >> 4)]);
                result.append(intToAlpha[(i << 2) & 63]);
                result.append('=');
            }
        }
        return result.toString();
    }

    static byte[] base64ToByteArray(String s) {
        return base64ToByteArray(s, false);
    }

    static byte[] altBase64ToByteArray(String s) {
        return base64ToByteArray(s, true);
    }

    private static byte[] base64ToByteArray(String s, boolean alternate) {
        String str = s;
        byte[] alphaToInt = alternate ? altBase64ToInt : base64ToInt;
        int sLen = s.length();
        int numGroups = sLen / 4;
        if (4 * numGroups == sLen) {
            int inCursor;
            int inCursor2;
            int outCursor;
            int missingBytesInLastGroup = 0;
            int numFullGroups = numGroups;
            if (sLen != 0) {
                if (str.charAt(sLen - 1) == '=') {
                    missingBytesInLastGroup = 0 + 1;
                    numFullGroups--;
                }
                if (str.charAt(sLen - 2) == '=') {
                    missingBytesInLastGroup++;
                }
            }
            byte[] result = new byte[((3 * numGroups) - missingBytesInLastGroup)];
            int inCursor3 = 0;
            int outCursor2 = 0;
            int i = 0;
            while (i < numFullGroups) {
                inCursor = inCursor3 + 1;
                inCursor3 = base64toInt(str.charAt(inCursor3), alphaToInt);
                inCursor2 = inCursor + 1;
                inCursor = base64toInt(str.charAt(inCursor), alphaToInt);
                int inCursor4 = inCursor2 + 1;
                inCursor2 = base64toInt(str.charAt(inCursor2), alphaToInt);
                int inCursor5 = inCursor4 + 1;
                inCursor4 = base64toInt(str.charAt(inCursor4), alphaToInt);
                int outCursor3 = outCursor2 + 1;
                result[outCursor2] = (byte) ((inCursor3 << 2) | (inCursor >> 4));
                outCursor = outCursor3 + 1;
                result[outCursor3] = (byte) ((inCursor << 4) | (inCursor2 >> 2));
                outCursor2 = outCursor + 1;
                result[outCursor] = (byte) ((inCursor2 << 6) | inCursor4);
                i++;
                inCursor3 = inCursor5;
            }
            if (missingBytesInLastGroup != 0) {
                outCursor = inCursor3 + 1;
                inCursor3 = base64toInt(str.charAt(inCursor3), alphaToInt);
                i = outCursor + 1;
                outCursor = base64toInt(str.charAt(outCursor), alphaToInt);
                inCursor = outCursor2 + 1;
                result[outCursor2] = (byte) ((inCursor3 << 2) | (outCursor >> 4));
                if (missingBytesInLastGroup == 1) {
                    outCursor2 = i + 1;
                    inCursor2 = inCursor + 1;
                    result[inCursor] = (byte) ((outCursor << 4) | (base64toInt(str.charAt(i), alphaToInt) >> 2));
                    inCursor3 = outCursor2;
                } else {
                    inCursor2 = inCursor;
                }
            } else {
                inCursor2 = outCursor2;
            }
            return result;
        }
        throw new IllegalArgumentException("String length must be a multiple of four.");
    }

    private static int base64toInt(char c, byte[] alphaToInt) {
        int result = alphaToInt[c];
        if (result >= 0) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal character ");
        stringBuilder.append(c);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static void main(String[] args) {
        int numRuns = Integer.parseInt(args[0]);
        int numBytes = Integer.parseInt(args[1]);
        Random rnd = new Random();
        for (int i = 0; i < numRuns; i++) {
            for (int j = 0; j < numBytes; j++) {
                byte[] arr = new byte[j];
                for (int k = 0; k < j; k++) {
                    arr[k] = (byte) rnd.nextInt();
                }
                if (!Arrays.equals(arr, base64ToByteArray(byteArrayToBase64(arr)))) {
                    System.out.println("Dismal failure!");
                }
                if (!Arrays.equals(arr, altBase64ToByteArray(byteArrayToAltBase64(arr)))) {
                    System.out.println("Alternate dismal failure!");
                }
            }
        }
    }
}
