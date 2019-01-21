package com.huawei.zxing.oned;

import com.huawei.android.util.JlogConstantsEx;
import com.huawei.facerecognition.FaceRecognizeManager.AcquireInfo;
import com.huawei.motiondetection.MotionTypeApps;
import com.huawei.systemmanager.power.HwHistoryItem;
import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.ChecksumException;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import java.util.Arrays;
import java.util.Map;

public final class Code39Reader extends OneDReader {
    private static final char[] ALPHABET = ALPHABET_STRING.toCharArray();
    static final String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
    private static final int ASTERISK_ENCODING = CHARACTER_ENCODINGS[39];
    static final int[] CHARACTER_ENCODINGS = new int[]{52, 289, 97, 352, 49, MotionTypeApps.TYPE_PROXIMITY_SPEAKER, 112, 37, 292, 100, 265, 73, 328, 25, 280, 88, 13, 268, 76, 28, 259, 67, 322, 19, 274, 82, 7, 262, 70, 22, 385, 193, HwHistoryItem.STATE_PHONE_STATE_MASK, 145, 400, 208, 133, 388, 196, 148, 168, 162, JlogConstantsEx.JLID_MMS_MESSAGES_DELETE, 42};
    private final int[] counters;
    private final StringBuilder decodeRowResult;
    private final boolean extendedMode;
    private final boolean usingCheckDigit;

    public Code39Reader() {
        this(false);
    }

    public Code39Reader(boolean usingCheckDigit) {
        this(usingCheckDigit, false);
    }

    public Code39Reader(boolean usingCheckDigit, boolean extendedMode) {
        this.usingCheckDigit = usingCheckDigit;
        this.extendedMode = extendedMode;
        this.decodeRowResult = new StringBuilder(20);
        this.counters = new int[9];
    }

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> map) throws NotFoundException, ChecksumException, FormatException {
        int i = rowNumber;
        BitArray bitArray = row;
        int[] theCounters = this.counters;
        Arrays.fill(theCounters, 0);
        StringBuilder result = this.decodeRowResult;
        result.setLength(0);
        int[] start = findAsteriskPattern(bitArray, theCounters);
        int nextStart = bitArray.getNextSet(start[1]);
        int end = row.getSize();
        while (true) {
            OneDReader.recordPattern(bitArray, nextStart, theCounters);
            int pattern = toNarrowWidePattern(theCounters);
            if (pattern >= 0) {
                int counter;
                char decodedChar = patternToChar(pattern);
                result.append(decodedChar);
                int lastStart = nextStart;
                int nextStart2 = nextStart;
                for (int counter2 : theCounters) {
                    nextStart2 += counter2;
                }
                nextStart = bitArray.getNextSet(nextStart2);
                if (decodedChar == '*') {
                    result.setLength(result.length() - 1);
                    nextStart2 = 0;
                    for (int counter22 : theCounters) {
                        nextStart2 += counter22;
                    }
                    pattern = (nextStart - lastStart) - nextStart2;
                    if (nextStart == end || (pattern >> 1) >= nextStart2) {
                        if (this.usingCheckDigit) {
                            int max = result.length() - 1;
                            int total = 0;
                            for (counter22 = 0; counter22 < max; counter22++) {
                                total += ALPHABET_STRING.indexOf(this.decodeRowResult.charAt(counter22));
                            }
                            if (result.charAt(max) == ALPHABET[total % 43]) {
                                result.setLength(max);
                            } else {
                                throw ChecksumException.getChecksumInstance();
                            }
                        }
                        if (result.length() != 0) {
                            String resultString;
                            if (this.extendedMode) {
                                resultString = decodeExtended(result);
                            } else {
                                resultString = result.toString();
                            }
                            float right = ((float) lastStart) + (((float) nextStart2) / 2.0f);
                            r13 = new ResultPoint[2];
                            r13[0] = new ResultPoint(((float) (start[1] + start[0])) / 2.0f, (float) i);
                            r13[1] = new ResultPoint(right, (float) i);
                            return new Result(resultString, null, r13, BarcodeFormat.CODE_39);
                        }
                        throw NotFoundException.getNotFoundInstance();
                    }
                    throw NotFoundException.getNotFoundInstance();
                }
            } else {
                throw NotFoundException.getNotFoundInstance();
            }
        }
    }

    private static int[] findAsteriskPattern(BitArray row, int[] counters) throws NotFoundException {
        int width = row.getSize();
        int rowOffset = row.getNextSet(0);
        int patternStart = rowOffset;
        boolean isWhite = false;
        int patternLength = counters.length;
        int patternStart2 = patternStart;
        patternStart = 0;
        int i = rowOffset;
        while (i < width) {
            boolean z = true;
            if ((row.get(i) ^ isWhite) != 0) {
                counters[patternStart] = counters[patternStart] + 1;
            } else {
                if (patternStart != patternLength - 1) {
                    patternStart++;
                } else if (toNarrowWidePattern(counters) == ASTERISK_ENCODING && row.isRange(Math.max(0, patternStart2 - ((i - patternStart2) >> 1)), patternStart2, false)) {
                    return new int[]{patternStart2, i};
                } else {
                    patternStart2 += counters[0] + counters[1];
                    System.arraycopy(counters, 2, counters, 0, patternLength - 2);
                    counters[patternLength - 2] = 0;
                    counters[patternLength - 1] = 0;
                    patternStart--;
                }
                counters[patternStart] = 1;
                if (isWhite) {
                    z = false;
                }
                isWhite = z;
            }
            i++;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static int toNarrowWidePattern(int[] counters) {
        int numCounters = counters.length;
        int i = 0;
        int maxNarrowCounter = 0;
        int wideCounters;
        do {
            int minCounter;
            int minCounter2 = Integer.MAX_VALUE;
            for (int counter : counters) {
                if (counter < minCounter2 && counter > maxNarrowCounter) {
                    minCounter2 = counter;
                }
            }
            maxNarrowCounter = minCounter2;
            int pattern = 0;
            int counter2 = 0;
            wideCounters = 0;
            for (minCounter = 0; minCounter < numCounters; minCounter++) {
                int counter3 = counters[minCounter];
                if (counter3 > maxNarrowCounter) {
                    pattern |= 1 << ((numCounters - 1) - minCounter);
                    wideCounters++;
                    counter2 += counter3;
                }
            }
            if (wideCounters == 3) {
                while (i < numCounters && wideCounters > 0) {
                    minCounter = counters[i];
                    if (minCounter > maxNarrowCounter) {
                        wideCounters--;
                        if ((minCounter << 1) >= counter2) {
                            return -1;
                        }
                    }
                    i++;
                }
                return pattern;
            }
        } while (wideCounters > 3);
        return -1;
    }

    private static char patternToChar(int pattern) throws NotFoundException {
        for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
            if (CHARACTER_ENCODINGS[i] == pattern) {
                return ALPHABET[i];
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static String decodeExtended(CharSequence encoded) throws FormatException {
        int length = encoded.length();
        StringBuilder decoded = new StringBuilder(length);
        int i = 0;
        while (i < length) {
            char c = encoded.charAt(i);
            if (c == '+' || c == '$' || c == '%' || c == '/') {
                char next = encoded.charAt(i + 1);
                char decodedChar = 0;
                if (c != '+') {
                    if (c != '/') {
                        switch (c) {
                            case '$':
                                if (next >= 'A' && next <= 'Z') {
                                    decodedChar = (char) (next - 64);
                                    break;
                                }
                                throw FormatException.getFormatInstance();
                                break;
                            case AcquireInfo.FACEID_HAS_REGISTERED /*37*/:
                                if (next < 'A' || next > 'E') {
                                    if (next >= 'F' && next <= 'W') {
                                        decodedChar = (char) (next - 11);
                                        break;
                                    }
                                    throw FormatException.getFormatInstance();
                                }
                                decodedChar = (char) (next - 38);
                                break;
                                break;
                        }
                    } else if (next >= 'A' && next <= 'O') {
                        decodedChar = (char) (next - 32);
                    } else if (next == 'Z') {
                        decodedChar = ':';
                    } else {
                        throw FormatException.getFormatInstance();
                    }
                } else if (next < 'A' || next > 'Z') {
                    throw FormatException.getFormatInstance();
                } else {
                    decodedChar = (char) (next + 32);
                }
                decoded.append(decodedChar);
                i++;
            } else {
                decoded.append(c);
            }
            i++;
        }
        return decoded.toString();
    }
}
