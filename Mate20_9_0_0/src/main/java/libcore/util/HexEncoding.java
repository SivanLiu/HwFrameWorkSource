package libcore.util;

public class HexEncoding {
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private HexEncoding() {
    }

    public static char[] encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    public static char[] encode(byte[] data, int offset, int len) {
        char[] result = new char[(len * 2)];
        for (int i = 0; i < len; i++) {
            byte b = data[offset + i];
            int resultIndex = 2 * i;
            result[resultIndex] = HEX_DIGITS[(b >>> 4) & 15];
            result[resultIndex + 1] = HEX_DIGITS[b & 15];
        }
        return result;
    }

    public static String encodeToString(byte[] data) {
        return new String(encode(data));
    }

    public static byte[] decode(String encoded) throws IllegalArgumentException {
        return decode(encoded.toCharArray());
    }

    public static byte[] decode(String encoded, boolean allowSingleChar) throws IllegalArgumentException {
        return decode(encoded.toCharArray(), allowSingleChar);
    }

    public static byte[] decode(char[] encoded) throws IllegalArgumentException {
        return decode(encoded, false);
    }

    public static byte[] decode(char[] encoded, boolean allowSingleChar) throws IllegalArgumentException {
        int resultOffset;
        byte[] result = new byte[((encoded.length + 1) / 2)];
        int resultOffset2 = 0;
        int i = 0;
        if (allowSingleChar) {
            if (encoded.length % 2 != 0) {
                resultOffset = 0 + 1;
                result[0] = (byte) toDigit(encoded, 0);
                i = 0 + 1;
                resultOffset2 = resultOffset;
            }
        } else if (encoded.length % 2 != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid input length: ");
            stringBuilder.append(encoded.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        resultOffset = encoded.length;
        while (i < resultOffset) {
            int resultOffset3 = resultOffset2 + 1;
            result[resultOffset2] = (byte) ((toDigit(encoded, i) << 4) | toDigit(encoded, i + 1));
            i += 2;
            resultOffset2 = resultOffset3;
        }
        return result;
    }

    private static int toDigit(char[] str, int offset) throws IllegalArgumentException {
        int pseudoCodePoint = str[offset];
        if (48 <= pseudoCodePoint && pseudoCodePoint <= 57) {
            return pseudoCodePoint - 48;
        }
        if (97 <= pseudoCodePoint && pseudoCodePoint <= 102) {
            return 10 + (pseudoCodePoint - 97);
        }
        if (65 <= pseudoCodePoint && pseudoCodePoint <= 70) {
            return 10 + (pseudoCodePoint - 65);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal char: ");
        stringBuilder.append(str[offset]);
        stringBuilder.append(" at offset ");
        stringBuilder.append(offset);
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
