package com.huawei.zxing.oned.rss.expanded;

import android.telephony.HwCarrierConfigManager;
import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.android.hishow.AlarmInfoEx;
import com.huawei.android.util.JlogConstantsEx;
import com.huawei.internal.telephony.SmsConstantsEx;
import com.huawei.internal.telephony.uicc.IccConstantsEx;
import com.huawei.lcagent.client.MetricConstant;
import com.huawei.motiondetection.MotionTypeApps;
import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import com.huawei.zxing.oned.OneDReader;
import com.huawei.zxing.oned.rss.AbstractRSSReader;
import com.huawei.zxing.oned.rss.DataCharacter;
import com.huawei.zxing.oned.rss.FinderPattern;
import com.huawei.zxing.oned.rss.RSSUtils;
import com.huawei.zxing.oned.rss.expanded.decoders.AbstractExpandedDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class RSSExpandedReader extends AbstractRSSReader {
    private static final int[] EVEN_TOTAL_SUBSET = new int[]{4, 20, 52, 104, 204};
    private static final int[][] FINDER_PATTERNS = new int[][]{new int[]{1, 8, 4, 1}, new int[]{3, 6, 4, 1}, new int[]{3, 4, 6, 1}, new int[]{3, 2, 8, 1}, new int[]{2, 6, 5, 1}, new int[]{2, 2, 9, 1}};
    private static final int[][] FINDER_PATTERN_SEQUENCES = new int[][]{new int[]{0, 0}, new int[]{0, 1, 1}, new int[]{0, 2, 1, 3}, new int[]{0, 4, 1, 3, 2}, new int[]{0, 4, 1, 3, 3, 5}, new int[]{0, 4, 1, 3, 4, 5, 5}, new int[]{0, 0, 1, 1, 2, 2, 3, 3}, new int[]{0, 0, 1, 1, 2, 2, 3, 4, 4}, new int[]{0, 0, 1, 1, 2, 2, 3, 4, 5, 5}, new int[]{0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5}};
    private static final int FINDER_PAT_A = 0;
    private static final int FINDER_PAT_B = 1;
    private static final int FINDER_PAT_C = 2;
    private static final int FINDER_PAT_D = 3;
    private static final int FINDER_PAT_E = 4;
    private static final int FINDER_PAT_F = 5;
    private static final int[] GSUM = new int[]{0, 348, 1388, 2948, 3988};
    private static final int MAX_PAIRS = 11;
    private static final int[] SYMBOL_WIDEST = new int[]{7, 5, 4, 3, 1};
    private static final int[][] WEIGHTS = new int[][]{new int[]{1, 3, 9, 27, 81, 32, 96, 77}, new int[]{20, 60, 180, JlogConstantsEx.JLID_CONTACT_MULTISELECT_ACTIVITY_ONCREATE, 143, 7, 21, 63}, new int[]{189, 145, 13, 39, JlogConstantsEx.JLID_DIALPAD_AFTER_TEXT_CHANGE, 140, 209, 205}, new int[]{193, 157, 49, 147, 19, 57, 171, 91}, new int[]{62, 186, 136, 197, 169, 85, 44, JlogConstantsEx.JLID_EDIT_CONTACT_END}, new int[]{185, 133, 188, 142, 4, 12, 36, MetricConstant.GPS_METRIC_ID_EX}, new int[]{113, AppOpsManagerEx.TYPE_MICROPHONE, 173, 97, 80, 29, 87, 50}, new int[]{150, 28, 84, 41, JlogConstantsEx.JLID_NEW_CONTACT_CLICK, 158, 52, 156}, new int[]{46, JlogConstantsEx.JLID_MMS_MESSAGES_DELETE, MotionTypeApps.TYPE_FLIP_MUTE_AOD, 187, JlogConstantsEx.JLID_MMS_MESSAGE_SEARCH, 206, 196, 166}, new int[]{76, 17, 51, 153, 37, 111, JlogConstantsEx.JLID_DEF_CONTACT_ITEM_CLICK, 155}, new int[]{43, 129, IccConstantsEx.SMS_RECORD_LENGTH, MetricConstant.WIFI_METRIC_ID_EX, MetricConstant.BLUETOOTH_METRIC_ID_EX, 110, JlogConstantsEx.JLID_CONTACT_MULTISELECT_BIND_VIEW, 146}, new int[]{16, 48, 144, 10, 30, 90, 59, 177}, new int[]{MetricConstant.SCREEN_METRIC_ID_EX, JlogConstantsEx.JLID_CONTACT_BIND_EDITOR_FOR_NEW, JlogConstantsEx.JLID_MMS_CONVERSATIONS_DELETE, 200, 178, 112, JlogConstantsEx.JLID_EDIT_CONTACT_CLICK, 164}, new int[]{70, 210, 208, MotionTypeApps.TYPE_FLIP_MUTE_CLOCK, 184, 130, 179, JlogConstantsEx.JLID_CONTACT_DETAIL_BIND_VIEW}, new int[]{SmsConstantsEx.MAX_USER_DATA_BYTES_WITH_HEADER, 191, 151, 31, 93, 68, 204, 190}, new int[]{148, 22, 66, 198, 172, 94, 71, 2}, new int[]{6, 18, 54, 162, 64, HwCarrierConfigManager.HD_ICON_MASK_DIALER, 154, 40}, new int[]{JlogConstantsEx.JLID_DIALPAD_ONTOUCH_NOT_FIRST_DOWN, 149, 25, 75, 14, 42, JlogConstantsEx.JLID_NEW_CONTACT_SELECT_ACCOUNT, 167}, new int[]{79, 26, 78, 23, 69, 207, 199, 175}, new int[]{103, 98, 83, 38, 114, 131, 182, JlogConstantsEx.JLID_NEW_CONTACT_SAVE_CLICK}, new int[]{161, 61, 183, AlarmInfoEx.EVERYDAY_CODE, 170, 88, 53, 159}, new int[]{55, 165, 73, 8, 24, 72, 5, 15}, new int[]{45, 135, 194, SmsConstantsEx.MAX_USER_DATA_SEPTETS, 58, 174, 100, 89}};
    private final List<ExpandedPair> pairs = new ArrayList(11);
    private final List<ExpandedRow> rows = new ArrayList();
    private final int[] startEnd = new int[2];
    private boolean startFromEven = false;

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> map) throws NotFoundException, FormatException {
        this.pairs.clear();
        this.startFromEven = false;
        try {
            return constructResult(decodeRow2pairs(rowNumber, row));
        } catch (NotFoundException e) {
            this.pairs.clear();
            this.startFromEven = true;
            return constructResult(decodeRow2pairs(rowNumber, row));
        }
    }

    public void reset() {
        this.pairs.clear();
        this.rows.clear();
    }

    List<ExpandedPair> decodeRow2pairs(int rowNumber, BitArray row) throws NotFoundException {
        while (true) {
            try {
                this.pairs.add(retrieveNextPair(row, this.pairs, rowNumber));
            } catch (NotFoundException nfe) {
                NotFoundException nfe2;
                if (this.pairs.isEmpty()) {
                    throw nfe2;
                } else if (checkChecksum() != null) {
                    return this.pairs;
                } else {
                    nfe2 = this.rows.isEmpty() ^ 1;
                    storeRow(rowNumber, false);
                    if (nfe2 != null) {
                        List<ExpandedPair> ps = checkRows(null);
                        if (ps != null) {
                            return ps;
                        }
                        List<ExpandedPair> ps2 = checkRows(true);
                        if (ps2 != null) {
                            return ps2;
                        }
                    }
                    throw NotFoundException.getNotFoundInstance();
                }
            }
        }
    }

    private List<ExpandedPair> checkRows(boolean reverse) {
        if (this.rows.size() > 25) {
            this.rows.clear();
            return null;
        }
        this.pairs.clear();
        if (reverse) {
            Collections.reverse(this.rows);
        }
        List<ExpandedPair> ps = null;
        try {
            ps = checkRows(new ArrayList(), 0);
        } catch (NotFoundException e) {
        }
        if (reverse) {
            Collections.reverse(this.rows);
        }
        return ps;
    }

    private List<ExpandedPair> checkRows(List<ExpandedRow> collectedRows, int currentRow) throws NotFoundException {
        for (int i = currentRow; i < this.rows.size(); i++) {
            ExpandedRow row = (ExpandedRow) this.rows.get(i);
            this.pairs.clear();
            int size = collectedRows.size();
            for (int j = 0; j < size; j++) {
                this.pairs.addAll(((ExpandedRow) collectedRows.get(j)).getPairs());
            }
            this.pairs.addAll(row.getPairs());
            if (isValidSequence(this.pairs)) {
                if (checkChecksum()) {
                    return this.pairs;
                }
                List<ExpandedRow> rs = new ArrayList();
                rs.addAll(collectedRows);
                rs.add(row);
                try {
                    return checkRows(rs, i + 1);
                } catch (NotFoundException e) {
                }
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static boolean isValidSequence(List<ExpandedPair> pairs) {
        for (int[] sequence : FINDER_PATTERN_SEQUENCES) {
            if (pairs.size() <= sequence.length) {
                boolean stop = true;
                for (int j = 0; j < pairs.size(); j++) {
                    if (((ExpandedPair) pairs.get(j)).getFinderPattern().getValue() != sequence[j]) {
                        stop = false;
                        break;
                    }
                }
                if (stop) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:14:0x004d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void storeRow(int rowNumber, boolean wasReversed) {
        int insertPos = 0;
        boolean prevIsSame = false;
        boolean nextIsSame = false;
        while (insertPos < this.rows.size()) {
            ExpandedRow erow = (ExpandedRow) this.rows.get(insertPos);
            if (erow.getRowNumber() > rowNumber) {
                nextIsSame = erow.isEquivalent(this.pairs);
                break;
            } else {
                prevIsSame = erow.isEquivalent(this.pairs);
                insertPos++;
            }
        }
        if (!nextIsSame && !prevIsSame && !isPartialRow(this.pairs, this.rows)) {
            this.rows.add(insertPos, new ExpandedRow(this.pairs, rowNumber, wasReversed));
            removePartialRows(this.pairs, this.rows);
        }
    }

    private static void removePartialRows(List<ExpandedPair> pairs, List<ExpandedRow> rows) {
        Iterator<ExpandedRow> iterator = rows.iterator();
        while (iterator.hasNext()) {
            ExpandedRow r = (ExpandedRow) iterator.next();
            if (r.getPairs().size() != pairs.size()) {
                boolean allFound = true;
                for (ExpandedPair p : r.getPairs()) {
                    boolean found = false;
                    for (ExpandedPair pp : pairs) {
                        if (p.equals(pp)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        allFound = false;
                        break;
                    }
                }
                if (allFound) {
                    iterator.remove();
                }
            }
        }
    }

    private static boolean isPartialRow(Iterable<ExpandedPair> pairs, Iterable<ExpandedRow> rows) {
        for (ExpandedRow r : rows) {
            boolean allFound = true;
            for (ExpandedPair p : pairs) {
                boolean found = false;
                for (ExpandedPair pp : r.getPairs()) {
                    if (p.equals(pp)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    allFound = false;
                    break;
                }
            }
            if (allFound) {
                return true;
            }
        }
        return false;
    }

    List<ExpandedRow> getRows() {
        return this.rows;
    }

    static Result constructResult(List<ExpandedPair> pairs) throws NotFoundException, FormatException {
        String resultingString = AbstractExpandedDecoder.createDecoder(BitArrayBuilder.buildBitArray(pairs)).parseInformation();
        ResultPoint[] firstPoints = ((ExpandedPair) pairs.get(0)).getFinderPattern().getResultPoints();
        ResultPoint[] lastPoints = ((ExpandedPair) pairs.get(pairs.size() - 1)).getFinderPattern().getResultPoints();
        return new Result(resultingString, null, new ResultPoint[]{firstPoints[0], firstPoints[1], lastPoints[0], lastPoints[1]}, BarcodeFormat.RSS_EXPANDED);
    }

    private boolean checkChecksum() {
        boolean z = false;
        ExpandedPair firstPair = (ExpandedPair) this.pairs.get(0);
        DataCharacter checkCharacter = firstPair.getLeftChar();
        DataCharacter firstCharacter = firstPair.getRightChar();
        if (firstCharacter == null) {
            return false;
        }
        int s = 2;
        int checksum = firstCharacter.getChecksumPortion();
        for (int i = 1; i < this.pairs.size(); i++) {
            ExpandedPair currentPair = (ExpandedPair) this.pairs.get(i);
            checksum += currentPair.getLeftChar().getChecksumPortion();
            s++;
            DataCharacter currentRightChar = currentPair.getRightChar();
            if (currentRightChar != null) {
                checksum += currentRightChar.getChecksumPortion();
                s++;
            }
        }
        if ((211 * (s - 4)) + (checksum % 211) == checkCharacter.getValue()) {
            z = true;
        }
        return z;
    }

    private static int getNextSecondBar(BitArray row, int initialPos) {
        if (row.get(initialPos)) {
            return row.getNextSet(row.getNextUnset(initialPos));
        }
        return row.getNextUnset(row.getNextSet(initialPos));
    }

    ExpandedPair retrieveNextPair(BitArray row, List<ExpandedPair> previousPairs, int rowNumber) throws NotFoundException {
        FinderPattern pattern;
        boolean isOddPattern = previousPairs.size() % 2 == 0;
        if (this.startFromEven) {
            isOddPattern = !isOddPattern ? true : null;
        }
        boolean keepFinding = true;
        int forcedOffset = -1;
        do {
            findNextPair(row, previousPairs, forcedOffset);
            pattern = parseFoundFinderPattern(row, rowNumber, isOddPattern);
            if (pattern == null) {
                forcedOffset = getNextSecondBar(row, this.startEnd[0]);
                continue;
            } else {
                keepFinding = false;
                continue;
            }
        } while (keepFinding);
        DataCharacter leftChar = decodeDataCharacter(row, pattern, isOddPattern, true);
        if (previousPairs.isEmpty() || !((ExpandedPair) previousPairs.get(previousPairs.size() - 1)).mustBeLast()) {
            DataCharacter rightChar;
            try {
                rightChar = decodeDataCharacter(row, pattern, isOddPattern, false);
            } catch (NotFoundException e) {
                rightChar = null;
            }
            return new ExpandedPair(leftChar, rightChar, pattern, true);
        }
        throw NotFoundException.getNotFoundInstance();
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0047  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0045  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x004c  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x006a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void findNextPair(BitArray row, List<ExpandedPair> previousPairs, int forcedOffset) throws NotFoundException {
        int rowOffset;
        boolean searchingEvenPair;
        int rowOffset2;
        boolean isWhite;
        int patternStart;
        int counterPosition;
        boolean isWhite2;
        BitArray bitArray = row;
        int[] counters = getDecodeFinderCounters();
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        int width = row.getSize();
        if (forcedOffset >= 0) {
            rowOffset = forcedOffset;
        } else if (previousPairs.isEmpty()) {
            rowOffset = 0;
        } else {
            rowOffset = ((ExpandedPair) previousPairs.get(previousPairs.size() - 1)).getFinderPattern().getStartEnd()[1];
            searchingEvenPair = previousPairs.size() % 2 == 0;
            if (this.startFromEven) {
                searchingEvenPair = !searchingEvenPair;
            }
            rowOffset2 = rowOffset;
            isWhite = false;
            while (rowOffset2 < width) {
                isWhite = bitArray.get(rowOffset2) ^ 1;
                if (!isWhite) {
                    break;
                }
                rowOffset2++;
            }
            patternStart = rowOffset2;
            counterPosition = 0;
            isWhite2 = isWhite;
            for (rowOffset = rowOffset2; rowOffset < width; rowOffset++) {
                if ((bitArray.get(rowOffset) ^ isWhite2) != 0) {
                    counters[counterPosition] = counters[counterPosition] + 1;
                } else {
                    if (counterPosition == 3) {
                        if (searchingEvenPair) {
                            reverseCounters(counters);
                        }
                        if (AbstractRSSReader.isFinderPattern(counters)) {
                            this.startEnd[0] = patternStart;
                            this.startEnd[1] = rowOffset;
                            return;
                        }
                        if (searchingEvenPair) {
                            reverseCounters(counters);
                        }
                        patternStart += counters[0] + counters[1];
                        counters[0] = counters[2];
                        counters[1] = counters[3];
                        counters[2] = 0;
                        counters[3] = 0;
                        counterPosition--;
                    } else {
                        counterPosition++;
                    }
                    counters[counterPosition] = 1;
                    isWhite2 = !isWhite2;
                }
            }
            throw NotFoundException.getNotFoundInstance();
        }
        List<ExpandedPair> list = previousPairs;
        if (previousPairs.size() % 2 == 0) {
        }
        if (this.startFromEven) {
        }
        rowOffset2 = rowOffset;
        isWhite = false;
        while (rowOffset2 < width) {
        }
        patternStart = rowOffset2;
        counterPosition = 0;
        isWhite2 = isWhite;
        while (rowOffset < width) {
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static void reverseCounters(int[] counters) {
        int length = counters.length;
        for (int i = 0; i < length / 2; i++) {
            int tmp = counters[i];
            counters[i] = counters[(length - i) - 1];
            counters[(length - i) - 1] = tmp;
        }
    }

    private FinderPattern parseFoundFinderPattern(BitArray row, int rowNumber, boolean oddPattern) {
        int firstElementStart;
        int firstCounter;
        int start;
        BitArray bitArray = row;
        if (oddPattern) {
            firstElementStart = this.startEnd[0] - 1;
            while (firstElementStart >= 0 && !bitArray.get(firstElementStart)) {
                firstElementStart--;
            }
            firstElementStart++;
            firstCounter = this.startEnd[0] - firstElementStart;
            start = firstElementStart;
            firstElementStart = this.startEnd[1];
        } else {
            start = this.startEnd[0];
            firstElementStart = bitArray.getNextUnset(this.startEnd[1] + 1);
            firstCounter = firstElementStart - this.startEnd[1];
        }
        int start2 = start;
        int[] counters = getDecodeFinderCounters();
        System.arraycopy(counters, 0, counters, 1, counters.length - 1);
        counters[0] = firstCounter;
        try {
            return new FinderPattern(AbstractRSSReader.parseFinderValue(counters, FINDER_PATTERNS), new int[]{start2, firstElementStart}, start2, firstElementStart, rowNumber);
        } catch (NotFoundException e) {
            return null;
        }
    }

    DataCharacter decodeDataCharacter(BitArray row, FinderPattern pattern, boolean isOddPattern, boolean leftChar) throws NotFoundException {
        BitArray bitArray = row;
        int[] counters = getDataCharacterCounters();
        counters[0] = 0;
        int i = 1;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        counters[4] = 0;
        counters[5] = 0;
        counters[6] = 0;
        counters[7] = 0;
        if (leftChar) {
            OneDReader.recordPatternInReverse(bitArray, pattern.getStartEnd()[0], counters);
        } else {
            OneDReader.recordPattern(bitArray, pattern.getStartEnd()[1], counters);
            int i2 = 0;
            for (int j = counters.length - 1; i2 < j; j--) {
                int temp = counters[i2];
                counters[i2] = counters[j];
                counters[j] = temp;
                i2++;
            }
        }
        float elementWidth = ((float) AbstractRSSReader.count(counters)) / ((float) 17);
        float expectedElementWidth = ((float) (pattern.getStartEnd()[1] - pattern.getStartEnd()[0])) / 15.0f;
        float f = 0.3f;
        if (Math.abs(elementWidth - expectedElementWidth) / expectedElementWidth <= 0.3f) {
            int offset;
            int i3;
            int[] oddCounts = getOddCounts();
            int[] evenCounts = getEvenCounts();
            float[] oddRoundingErrors = getOddRoundingErrors();
            float[] evenRoundingErrors = getEvenRoundingErrors();
            int i4 = 0;
            while (i4 < counters.length) {
                float value = (1.0f * ((float) counters[i4])) / elementWidth;
                int count = (int) (1056964608 + value);
                if (count < i) {
                    if (value >= f) {
                        count = 1;
                    } else {
                        throw NotFoundException.getNotFoundInstance();
                    }
                } else if (count > 8) {
                    if (value <= 8.7f) {
                        count = 8;
                    } else {
                        throw NotFoundException.getNotFoundInstance();
                    }
                }
                offset = i4 >> 1;
                if ((i4 & 1) == 0) {
                    oddCounts[offset] = count;
                    oddRoundingErrors[offset] = value - ((float) count);
                } else {
                    evenCounts[offset] = count;
                    evenRoundingErrors[offset] = value - ((float) count);
                }
                i4++;
                i = 1;
                f = 0.3f;
            }
            adjustOddEvenCounts(17);
            i = (((pattern.getValue() * 4) + (isOddPattern ? 0 : 2)) + (leftChar ^ 1)) - 1;
            i4 = 0;
            int oddChecksumPortion = 0;
            for (i3 = oddCounts.length - 1; i3 >= 0; i3--) {
                if (isNotA1left(pattern, isOddPattern, leftChar)) {
                    oddChecksumPortion += oddCounts[i3] * WEIGHTS[i][2 * i3];
                }
                i4 += oddCounts[i3];
            }
            i3 = 0;
            for (offset = evenCounts.length - 1; offset >= 0; offset--) {
                if (isNotA1left(pattern, isOddPattern, leftChar)) {
                    i3 += evenCounts[offset] * WEIGHTS[i][(2 * offset) + 1];
                }
            }
            offset = oddChecksumPortion + i3;
            if ((i4 & 1) != 0 || i4 > 13 || i4 < 4) {
                throw NotFoundException.getNotFoundInstance();
            }
            int group = (13 - i4) / 2;
            int oddWidest = SYMBOL_WIDEST[group];
            counters = 9 - oddWidest;
            int vOdd = RSSUtils.getRSSvalue(oddCounts, oddWidest, 1);
            oddWidest = RSSUtils.getRSSvalue(evenCounts, counters, 0);
            return new DataCharacter(((vOdd * EVEN_TOTAL_SUBSET[group]) + oddWidest) + GSUM[group], offset);
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static boolean isNotA1left(FinderPattern pattern, boolean isOddPattern, boolean leftChar) {
        return (pattern.getValue() == 0 && isOddPattern && leftChar) ? false : true;
    }

    private void adjustOddEvenCounts(int numModules) throws NotFoundException {
        int oddSum = AbstractRSSReader.count(getOddCounts());
        int evenSum = AbstractRSSReader.count(getEvenCounts());
        int mismatch = (oddSum + evenSum) - numModules;
        boolean evenParityBad = false;
        boolean oddParityBad = (oddSum & 1) == 1;
        if ((evenSum & 1) == 0) {
            evenParityBad = true;
        }
        boolean incrementOdd = false;
        boolean decrementOdd = false;
        if (oddSum > 13) {
            decrementOdd = true;
        } else if (oddSum < 4) {
            incrementOdd = true;
        }
        boolean incrementEven = false;
        boolean decrementEven = false;
        if (evenSum > 13) {
            decrementEven = true;
        } else if (evenSum < 4) {
            incrementEven = true;
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
