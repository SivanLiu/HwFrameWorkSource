package com.huawei.zxing.oned;

import java.util.Arrays;

public final class CodaBarWriter extends OneDimensionalCodeWriter {
    private static final char[] ALT_START_END_CHARS = new char[]{'T', 'N', '*', 'E'};
    private static final char[] START_END_CHARS = new char[]{'A', 'B', 'C', 'D'};

    public boolean[] encode(String contents) {
        if (contents.length() < 2) {
            throw new IllegalArgumentException("Codabar should start/end with start/stop symbols");
        }
        boolean arrayContains;
        boolean arrayContains2;
        char firstChar = Character.toUpperCase(contents.charAt(0));
        char lastChar = Character.toUpperCase(contents.charAt(contents.length() - 1));
        if (CodaBarReader.arrayContains(START_END_CHARS, firstChar)) {
            arrayContains = CodaBarReader.arrayContains(START_END_CHARS, lastChar);
        } else {
            arrayContains = false;
        }
        if (CodaBarReader.arrayContains(ALT_START_END_CHARS, firstChar)) {
            arrayContains2 = CodaBarReader.arrayContains(ALT_START_END_CHARS, lastChar);
        } else {
            arrayContains2 = false;
        }
        if (arrayContains) {
            arrayContains2 = true;
        }
        if (arrayContains2) {
            int resultLength = 20;
            int i = 4;
            char[] charsWhichAreTenLengthEachAfterDecoded = new char[]{'/', ':', '+', '.'};
            int i2 = 1;
            while (i2 < contents.length() - 1) {
                if (Character.isDigit(contents.charAt(i2)) || contents.charAt(i2) == '-' || contents.charAt(i2) == '$') {
                    resultLength += 9;
                } else if (CodaBarReader.arrayContains(charsWhichAreTenLengthEachAfterDecoded, contents.charAt(i2))) {
                    resultLength += 10;
                } else {
                    throw new IllegalArgumentException("Cannot encode : '" + contents.charAt(i2) + '\'');
                }
                i2++;
            }
            boolean[] result = new boolean[(resultLength + (contents.length() - 1))];
            int position = 0;
            for (int index = 0; index < contents.length(); index++) {
                boolean color;
                int counter;
                int bit;
                char c = Character.toUpperCase(contents.charAt(index));
                if (index == contents.length() - 1) {
                    switch (c) {
                        case '*':
                            c = 'C';
                            break;
                        case 'E':
                            c = 'D';
                            break;
                        case 'N':
                            c = 'B';
                            break;
                        case 'T':
                            c = 'A';
                            break;
                    }
                }
                int code = 0;
                i2 = 0;
                while (i2 < CodaBarReader.ALPHABET.length) {
                    if (c == CodaBarReader.ALPHABET[i2]) {
                        code = CodaBarReader.CHARACTER_ENCODINGS[i2];
                        color = true;
                        counter = 0;
                        bit = 0;
                        while (bit < 7) {
                            result[position] = color;
                            position++;
                            if (((code >> (6 - bit)) & 1) != 0 || counter == 1) {
                                color ^= 1;
                                bit++;
                                counter = 0;
                            } else {
                                counter++;
                            }
                        }
                        if (index < contents.length() - 1) {
                            result[position] = false;
                            position++;
                        }
                    } else {
                        i2++;
                    }
                }
                color = true;
                counter = 0;
                bit = 0;
                while (bit < 7) {
                    result[position] = color;
                    position++;
                    if (((code >> (6 - bit)) & 1) != 0) {
                    }
                    color ^= 1;
                    bit++;
                    counter = 0;
                }
                if (index < contents.length() - 1) {
                    result[position] = false;
                    position++;
                }
            }
            return result;
        }
        throw new IllegalArgumentException("Codabar should start/end with " + Arrays.toString(START_END_CHARS) + ", or start/end with " + Arrays.toString(ALT_START_END_CHARS));
    }
}
