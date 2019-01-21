package com.huawei.zxing.oned;

import java.util.Arrays;

public final class CodaBarWriter extends OneDimensionalCodeWriter {
    private static final char[] ALT_START_END_CHARS = new char[]{'T', 'N', '*', 'E'};
    private static final char[] START_END_CHARS = new char[]{'A', 'B', 'C', 'D'};

    public boolean[] encode(String contents) {
        String str = contents;
        if (contents.length() >= 2) {
            int i = 0;
            char firstChar = Character.toUpperCase(str.charAt(0));
            int i2 = 1;
            char lastChar = Character.toUpperCase(str.charAt(contents.length() - 1));
            boolean startsEndsNormal = CodaBarReader.arrayContains(START_END_CHARS, firstChar) && CodaBarReader.arrayContains(START_END_CHARS, lastChar);
            boolean startsEndsAlt = CodaBarReader.arrayContains(ALT_START_END_CHARS, firstChar) && CodaBarReader.arrayContains(ALT_START_END_CHARS, lastChar);
            StringBuilder stringBuilder;
            if (startsEndsNormal || startsEndsAlt) {
                char[] charsWhichAreTenLengthEachAfterDecoded = new char[]{'/', ':', '+', '.'};
                int resultLength = 20;
                int i3 = 1;
                while (i3 < contents.length() - 1) {
                    if (Character.isDigit(str.charAt(i3)) || str.charAt(i3) == '-' || str.charAt(i3) == '$') {
                        resultLength += 9;
                    } else if (CodaBarReader.arrayContains(charsWhichAreTenLengthEachAfterDecoded, str.charAt(i3))) {
                        resultLength += 10;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot encode : '");
                        stringBuilder.append(str.charAt(i3));
                        stringBuilder.append('\'');
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    i3++;
                }
                boolean[] result = new boolean[(resultLength + (contents.length() - 1))];
                int position = 0;
                int index = 0;
                while (index < contents.length()) {
                    int i4;
                    char firstChar2;
                    boolean z;
                    char c = Character.toUpperCase(str.charAt(index));
                    if (index == contents.length() - i2) {
                        if (c == '*') {
                            c = 'C';
                        } else if (c == 'E') {
                            c = 'D';
                        } else if (c == 'N') {
                            c = 'B';
                        } else if (c == 'T') {
                            c = 'A';
                        }
                    }
                    int code = 0;
                    for (i4 = i; i4 < CodaBarReader.ALPHABET.length; i4++) {
                        if (c == CodaBarReader.ALPHABET[i4]) {
                            code = CodaBarReader.CHARACTER_ENCODINGS[i4];
                            break;
                        }
                    }
                    int i5 = position;
                    position = i;
                    i = 0;
                    boolean color = true;
                    i4 = i5;
                    while (position < 7) {
                        result[i4] = color;
                        i4++;
                        firstChar2 = firstChar;
                        if (((code >> (6 - position)) & 1) == 0 || i == 1) {
                            color = !color;
                            position++;
                            i = 0;
                        } else {
                            i++;
                        }
                        firstChar = firstChar2;
                    }
                    firstChar2 = firstChar;
                    i2 = 1;
                    if (index < contents.length() - 1) {
                        z = false;
                        result[i4] = false;
                        i4++;
                    } else {
                        z = false;
                    }
                    position = i4;
                    index++;
                    boolean i6 = z;
                    firstChar = firstChar2;
                }
                return result;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Codabar should start/end with ");
            stringBuilder.append(Arrays.toString(START_END_CHARS));
            stringBuilder.append(", or start/end with ");
            stringBuilder.append(Arrays.toString(ALT_START_END_CHARS));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        throw new IllegalArgumentException("Codabar should start/end with start/stop symbols");
    }
}
