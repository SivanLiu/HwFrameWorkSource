package com.huawei.zxing.oned.rss;

import com.huawei.android.util.JlogConstantsEx;
import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.ResultPointCallback;
import com.huawei.zxing.common.BitArray;
import com.huawei.zxing.oned.OneDReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class RSS14Reader extends AbstractRSSReader {
    private static final int[][] FINDER_PATTERNS = new int[][]{new int[]{3, 8, 2, 1}, new int[]{3, 5, 5, 1}, new int[]{3, 3, 7, 1}, new int[]{3, 1, 9, 1}, new int[]{2, 7, 4, 1}, new int[]{2, 5, 6, 1}, new int[]{2, 3, 8, 1}, new int[]{1, 5, 7, 1}, new int[]{1, 3, 9, 1}};
    private static final int[] INSIDE_GSUM = new int[]{0, 336, 1036, 1516};
    private static final int[] INSIDE_ODD_TOTAL_SUBSET = new int[]{4, 20, 48, 81};
    private static final int[] INSIDE_ODD_WIDEST = new int[]{2, 4, 6, 8};
    private static final int[] OUTSIDE_EVEN_TOTAL_SUBSET = new int[]{1, 10, 34, 70, JlogConstantsEx.JLID_NEW_CONTACT_SELECT_ACCOUNT};
    private static final int[] OUTSIDE_GSUM = new int[]{0, 161, 961, 2015, 2715};
    private static final int[] OUTSIDE_ODD_WIDEST = new int[]{8, 6, 4, 3, 1};
    private final List<Pair> possibleLeftPairs = new ArrayList();
    private final List<Pair> possibleRightPairs = new ArrayList();

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException {
        addOrTally(this.possibleLeftPairs, decodePair(row, false, rowNumber, hints));
        row.reverse();
        addOrTally(this.possibleRightPairs, decodePair(row, true, rowNumber, hints));
        row.reverse();
        int lefSize = this.possibleLeftPairs.size();
        for (int i = 0; i < lefSize; i++) {
            Pair left = (Pair) this.possibleLeftPairs.get(i);
            if (left.getCount() > 1) {
                int rightSize = this.possibleRightPairs.size();
                for (int j = 0; j < rightSize; j++) {
                    Pair right = (Pair) this.possibleRightPairs.get(j);
                    if (right.getCount() > 1 && checkChecksum(left, right)) {
                        return constructResult(left, right);
                    }
                }
                continue;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static void addOrTally(Collection<Pair> possiblePairs, Pair pair) {
        if (pair != null) {
            boolean found = false;
            for (Pair other : possiblePairs) {
                if (other.getValue() == pair.getValue()) {
                    other.incrementCount();
                    found = true;
                    break;
                }
            }
            if (!found) {
                possiblePairs.add(pair);
            }
        }
    }

    public void reset() {
        this.possibleLeftPairs.clear();
        this.possibleRightPairs.clear();
    }

    private static Result constructResult(Pair leftPair, Pair rightPair) {
        int i;
        String text = String.valueOf((4537077 * ((long) leftPair.getValue())) + ((long) rightPair.getValue()));
        StringBuilder buffer = new StringBuilder(14);
        for (i = 13 - text.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(text);
        int checkDigit = 0;
        for (i = 0; i < 13; i++) {
            int digit = buffer.charAt(i) - 48;
            checkDigit += (i & 1) == 0 ? 3 * digit : digit;
        }
        i = 10 - (checkDigit % 10);
        if (i == 10) {
            i = 0;
        }
        buffer.append(i);
        ResultPoint[] leftPoints = leftPair.getFinderPattern().getResultPoints();
        ResultPoint[] rightPoints = rightPair.getFinderPattern().getResultPoints();
        return new Result(String.valueOf(buffer.toString()), null, new ResultPoint[]{leftPoints[0], leftPoints[1], rightPoints[0], rightPoints[1]}, BarcodeFormat.RSS_14);
    }

    private static boolean checkChecksum(Pair leftPair, Pair rightPair) {
        int checkValue = (leftPair.getChecksumPortion() + (16 * rightPair.getChecksumPortion())) % 79;
        int targetCheckValue = (9 * leftPair.getFinderPattern().getValue()) + rightPair.getFinderPattern().getValue();
        if (targetCheckValue > 72) {
            targetCheckValue--;
        }
        if (targetCheckValue > 8) {
            targetCheckValue--;
        }
        return checkValue == targetCheckValue;
    }

    private Pair decodePair(BitArray row, boolean right, int rowNumber, Map<DecodeHintType, ?> hints) {
        try {
            int[] startEnd = findFinderPattern(row, 0, right);
            FinderPattern pattern = parseFoundFinderPattern(row, rowNumber, right, startEnd);
            ResultPointCallback resultPointCallback = hints == null ? null : (ResultPointCallback) hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
            if (resultPointCallback != null) {
                float center = ((float) (startEnd[0] + startEnd[1])) / 2.0f;
                if (right) {
                    center = ((float) (row.getSize() - 1)) - center;
                }
                resultPointCallback.foundPossibleResultPoint(new ResultPoint(center, (float) rowNumber));
            }
            DataCharacter outside = decodeDataCharacter(row, pattern, true);
            DataCharacter inside = decodeDataCharacter(row, pattern, false);
            return new Pair((1597 * outside.getValue()) + inside.getValue(), outside.getChecksumPortion() + (4 * inside.getChecksumPortion()), pattern);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private DataCharacter decodeDataCharacter(BitArray row, FinderPattern pattern, boolean outsideChar) throws NotFoundException {
        int i;
        int i2;
        int offset;
        int i3;
        BitArray bitArray = row;
        boolean z = outsideChar;
        int[] counters = getDataCharacterCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        counters[4] = 0;
        counters[5] = 0;
        counters[6] = 0;
        counters[7] = 0;
        if (z) {
            OneDReader.recordPatternInReverse(bitArray, pattern.getStartEnd()[0], counters);
        } else {
            OneDReader.recordPattern(bitArray, pattern.getStartEnd()[1] + 1, counters);
            i = 0;
            for (int j = counters.length - 1; i < j; j--) {
                int temp = counters[i];
                counters[i] = counters[j];
                counters[j] = temp;
                i++;
            }
        }
        i = z ? 16 : 15;
        float elementWidth = ((float) AbstractRSSReader.count(counters)) / ((float) i);
        int[] oddCounts = getOddCounts();
        int[] evenCounts = getEvenCounts();
        float[] oddRoundingErrors = getOddRoundingErrors();
        float[] evenRoundingErrors = getEvenRoundingErrors();
        for (i2 = 0; i2 < counters.length; i2++) {
            float value = ((float) counters[i2]) / elementWidth;
            int count = (int) (value + 1056964608);
            if (count < 1) {
                count = 1;
            } else if (count > 8) {
                count = 8;
            }
            offset = i2 >> 1;
            if ((i2 & 1) == 0) {
                oddCounts[offset] = count;
                oddRoundingErrors[offset] = value - ((float) count);
            } else {
                evenCounts[offset] = count;
                evenRoundingErrors[offset] = value - ((float) count);
            }
        }
        adjustOddEvenCounts(z, i);
        offset = 0;
        int oddChecksumPortion = 0;
        for (i2 = oddCounts.length - 1; i2 >= 0; i2--) {
            oddChecksumPortion = (oddChecksumPortion * 9) + oddCounts[i2];
            offset += oddCounts[i2];
        }
        i2 = 0;
        int evenSum = 0;
        for (i3 = evenCounts.length - 1; i3 >= 0; i3--) {
            i2 = (i2 * 9) + evenCounts[i3];
            evenSum += evenCounts[i3];
        }
        i3 = (3 * i2) + oddChecksumPortion;
        int group;
        int oddWidest;
        int vOdd;
        if (!z) {
            if ((evenSum & 1) != 0 || evenSum > 10 || evenSum < 4) {
                throw NotFoundException.getNotFoundInstance();
            }
            group = (10 - evenSum) / 2;
            oddWidest = INSIDE_ODD_WIDEST[group];
            int evenWidest = 9 - oddWidest;
            vOdd = RSSUtils.getRSSvalue(oddCounts, oddWidest, 1);
            oddWidest = RSSUtils.getRSSvalue(evenCounts, evenWidest, 0);
            return new DataCharacter(((oddWidest * INSIDE_ODD_TOTAL_SUBSET[group]) + vOdd) + INSIDE_GSUM[group], i3);
        } else if ((offset & 1) != 0 || offset > 12 || offset < 4) {
            throw NotFoundException.getNotFoundInstance();
        } else {
            vOdd = (12 - offset) / 2;
            group = OUTSIDE_ODD_WIDEST[vOdd];
            oddWidest = 9 - group;
            counters = RSSUtils.getRSSvalue(oddCounts, group, null);
            group = RSSUtils.getRSSvalue(evenCounts, oddWidest, 1);
            return new DataCharacter(((counters * OUTSIDE_EVEN_TOTAL_SUBSET[vOdd]) + group) + OUTSIDE_GSUM[vOdd], i3);
        }
    }

    private int[] findFinderPattern(BitArray row, int rowOffset, boolean rightFinderPattern) throws NotFoundException {
        int[] counters = getDecodeFinderCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        int width = row.getSize();
        int rowOffset2 = rowOffset;
        boolean isWhite = false;
        while (rowOffset2 < width) {
            isWhite = row.get(rowOffset2) ^ 1;
            if (rightFinderPattern == isWhite) {
                break;
            }
            rowOffset2++;
        }
        int patternStart = rowOffset2;
        int counterPosition = 0;
        boolean isWhite2 = isWhite;
        for (rowOffset = rowOffset2; rowOffset < width; rowOffset++) {
            if ((row.get(rowOffset) ^ isWhite2) != 0) {
                counters[counterPosition] = counters[counterPosition] + 1;
            } else {
                if (counterPosition != 3) {
                    counterPosition++;
                } else if (AbstractRSSReader.isFinderPattern(counters)) {
                    return new int[]{patternStart, rowOffset};
                } else {
                    patternStart += counters[0] + counters[1];
                    counters[0] = counters[2];
                    counters[1] = counters[3];
                    counters[2] = 0;
                    counters[3] = 0;
                    counterPosition--;
                }
                counters[counterPosition] = 1;
                isWhite2 = !isWhite2;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private FinderPattern parseFoundFinderPattern(BitArray row, int rowNumber, boolean right, int[] startEnd) throws NotFoundException {
        int end;
        int start;
        BitArray bitArray = row;
        boolean firstIsBlack = bitArray.get(startEnd[0]);
        int firstElementStart = startEnd[0] - 1;
        while (firstElementStart >= 0 && (bitArray.get(firstElementStart) ^ firstIsBlack) != 0) {
            firstElementStart--;
        }
        firstElementStart++;
        int firstCounter = startEnd[0] - firstElementStart;
        int[] counters = getDecodeFinderCounters();
        System.arraycopy(counters, 0, counters, 1, counters.length - 1);
        counters[0] = firstCounter;
        int value = AbstractRSSReader.parseFinderValue(counters, FINDER_PATTERNS);
        int start2 = firstElementStart;
        int end2 = startEnd[1];
        if (right) {
            end = (row.getSize() - 1) - end2;
            start = (row.getSize() - 1) - start2;
        } else {
            start = start2;
            end = end2;
        }
        return new FinderPattern(value, new int[]{firstElementStart, startEnd[1]}, start, end, rowNumber);
    }

    private void adjustOddEvenCounts(boolean outsideChar, int numModules) throws NotFoundException {
        int oddSum = AbstractRSSReader.count(getOddCounts());
        int evenSum = AbstractRSSReader.count(getEvenCounts());
        int mismatch = (oddSum + evenSum) - numModules;
        boolean evenParityBad = false;
        boolean oddParityBad = (oddSum & 1) == outsideChar;
        if ((evenSum & 1) == 1) {
            evenParityBad = true;
        }
        boolean incrementOdd = false;
        boolean decrementOdd = false;
        boolean incrementEven = false;
        boolean decrementEven = false;
        if (outsideChar) {
            if (oddSum > 12) {
                decrementOdd = true;
            } else if (oddSum < 4) {
                incrementOdd = true;
            }
            if (evenSum > 12) {
                decrementEven = true;
            } else if (evenSum < 4) {
                incrementEven = true;
            }
        } else {
            if (oddSum > 11) {
                decrementOdd = true;
            } else if (oddSum < 5) {
                incrementOdd = true;
            }
            if (evenSum > 10) {
                decrementEven = true;
            } else if (evenSum < 4) {
                incrementEven = true;
            }
        }
        if (mismatch == 1) {
            if (oddParityBad) {
                if (evenParityBad) {
                    throw NotFoundException.getNotFoundInstance();
                }
                decrementOdd = true;
            } else if (evenParityBad) {
                decrementEven = true;
            } else {
                throw NotFoundException.getNotFoundInstance();
            }
        } else if (mismatch == -1) {
            if (oddParityBad) {
                if (evenParityBad) {
                    throw NotFoundException.getNotFoundInstance();
                }
                incrementOdd = true;
            } else if (evenParityBad) {
                incrementEven = true;
            } else {
                throw NotFoundException.getNotFoundInstance();
            }
        } else if (mismatch != 0) {
            throw NotFoundException.getNotFoundInstance();
        } else if (oddParityBad) {
            if (!evenParityBad) {
                throw NotFoundException.getNotFoundInstance();
            } else if (oddSum < evenSum) {
                incrementOdd = true;
                decrementEven = true;
            } else {
                decrementOdd = true;
                incrementEven = true;
            }
        } else if (evenParityBad) {
            throw NotFoundException.getNotFoundInstance();
        }
        if (incrementOdd) {
            if (decrementOdd) {
                throw NotFoundException.getNotFoundInstance();
            }
            AbstractRSSReader.increment(getOddCounts(), getOddRoundingErrors());
        }
        if (decrementOdd) {
            AbstractRSSReader.decrement(getOddCounts(), getOddRoundingErrors());
        }
        if (incrementEven) {
            if (decrementEven) {
                throw NotFoundException.getNotFoundInstance();
            }
            AbstractRSSReader.increment(getEvenCounts(), getOddRoundingErrors());
        }
        if (decrementEven) {
            AbstractRSSReader.decrement(getEvenCounts(), getEvenRoundingErrors());
        }
    }
}
