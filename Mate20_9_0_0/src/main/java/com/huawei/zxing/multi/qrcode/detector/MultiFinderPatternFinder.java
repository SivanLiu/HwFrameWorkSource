package com.huawei.zxing.multi.qrcode.detector;

import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.ResultPointCallback;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.qrcode.detector.FinderPattern;
import com.huawei.zxing.qrcode.detector.FinderPatternFinder;
import com.huawei.zxing.qrcode.detector.FinderPatternInfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class MultiFinderPatternFinder extends FinderPatternFinder {
    private static final float DIFF_MODSIZE_CUTOFF = 0.5f;
    private static final float DIFF_MODSIZE_CUTOFF_PERCENT = 0.05f;
    private static final FinderPatternInfo[] EMPTY_RESULT_ARRAY = new FinderPatternInfo[0];
    private static final float MAX_MODULE_COUNT_PER_EDGE = 180.0f;
    private static final float MIN_MODULE_COUNT_PER_EDGE = 9.0f;

    private static final class ModuleSizeComparator implements Comparator<FinderPattern>, Serializable {
        private ModuleSizeComparator() {
        }

        public int compare(FinderPattern center1, FinderPattern center2) {
            float value = center2.getEstimatedModuleSize() - center1.getEstimatedModuleSize();
            if (((double) value) < 0.0d) {
                return -1;
            }
            return ((double) value) > 0.0d ? 1 : 0;
        }
    }

    MultiFinderPatternFinder(BitMatrix image) {
        super(image);
    }

    MultiFinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback) {
        super(image, resultPointCallback);
    }

    private FinderPattern[][] selectMutipleBestPatterns() throws NotFoundException {
        List<FinderPattern> possibleCenters = getPossibleCenters();
        int size = possibleCenters.size();
        int i = 3;
        int size2;
        if (size >= 3) {
            int i2 = 2;
            int i3 = 0;
            int i4 = 1;
            if (size == 3) {
                FinderPattern[][] finderPatternArr = new FinderPattern[1][];
                finderPatternArr[0] = new FinderPattern[]{(FinderPattern) possibleCenters.get(0), (FinderPattern) possibleCenters.get(1), (FinderPattern) possibleCenters.get(2)};
                return finderPatternArr;
            }
            Collections.sort(possibleCenters, new ModuleSizeComparator());
            List<FinderPattern[]> results = new ArrayList();
            int i1 = 0;
            while (i1 < size - 2) {
                FinderPattern p1 = (FinderPattern) possibleCenters.get(i1);
                if (p1 != null) {
                    int i22 = i1 + 1;
                    while (i22 < size - 1) {
                        List<FinderPattern> possibleCenters2;
                        int i5;
                        int i6;
                        int i7;
                        FinderPattern p2 = (FinderPattern) possibleCenters.get(i22);
                        if (p2 != null) {
                            float vModSize12 = (p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize()) / Math.min(p1.getEstimatedModuleSize(), p2.getEstimatedModuleSize());
                            float f = 0.5f;
                            int i8 = (Math.abs(p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize()) > 0.5f ? 1 : (Math.abs(p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize()) == 0.5f ? 0 : -1));
                            float f2 = DIFF_MODSIZE_CUTOFF_PERCENT;
                            if (i8 > 0 && vModSize12 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
                                break;
                            }
                            i8 = i22 + 1;
                            while (i8 < size) {
                                FinderPattern p3 = (FinderPattern) possibleCenters.get(i8);
                                if (p3 != null) {
                                    float vModSize23 = (p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize()) / Math.min(p2.getEstimatedModuleSize(), p3.getEstimatedModuleSize());
                                    if (Math.abs(p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize()) > f && vModSize23 >= f2) {
                                        possibleCenters2 = possibleCenters;
                                        size2 = size;
                                        i5 = 2;
                                        i6 = 0;
                                        i7 = 1;
                                        break;
                                    }
                                    FinderPattern[] test = new FinderPattern[i];
                                    i6 = 0;
                                    test[0] = p1;
                                    i7 = 1;
                                    test[1] = p2;
                                    i5 = 2;
                                    test[2] = p3;
                                    ResultPoint.orderBestPatterns(test);
                                    FinderPatternInfo info = new FinderPatternInfo(test);
                                    f = ResultPoint.distance(info.getTopLeft(), info.getBottomLeft());
                                    possibleCenters2 = possibleCenters;
                                    possibleCenters = ResultPoint.distance(info.getTopRight(), info.getBottomLeft());
                                    size2 = size;
                                    float dB = ResultPoint.distance(info.getTopLeft(), info.getTopRight());
                                    f2 = (f + dB) / (p1.getEstimatedModuleSize() * 2.0f);
                                    if (f2 <= MAX_MODULE_COUNT_PER_EDGE && f2 >= MIN_MODULE_COUNT_PER_EDGE) {
                                        float vABBC = Math.abs((f - dB) / Math.min(f, dB));
                                        if (vABBC < 0.1f) {
                                            dB = (float) Math.sqrt((double) ((f * f) + (dB * dB)));
                                            if (Math.abs((possibleCenters - dB) / Math.min(possibleCenters, dB)) < 0.1f) {
                                                results.add(test);
                                            }
                                        }
                                    }
                                } else {
                                    possibleCenters2 = possibleCenters;
                                    size2 = size;
                                    i6 = i3;
                                    i7 = i4;
                                    i5 = 2;
                                }
                                i8++;
                                i2 = i5;
                                i3 = i6;
                                i4 = i7;
                                possibleCenters = possibleCenters2;
                                size = size2;
                                i = 3;
                                f = 0.5f;
                                f2 = DIFF_MODSIZE_CUTOFF_PERCENT;
                            }
                            possibleCenters2 = possibleCenters;
                            size2 = size;
                            i5 = i2;
                            i6 = i3;
                            i7 = i4;
                        } else {
                            possibleCenters2 = possibleCenters;
                            size2 = size;
                            i5 = i2;
                            i6 = i3;
                            i7 = i4;
                        }
                        i22++;
                        i2 = i5;
                        i3 = i6;
                        i4 = i7;
                        possibleCenters = possibleCenters2;
                        size = size2;
                        i = 3;
                    }
                }
                i1++;
                i2 = i2;
                i3 = i3;
                i4 = i4;
                possibleCenters = possibleCenters;
                size = size;
                i = 3;
            }
            size2 = size;
            if (!results.isEmpty()) {
                return (FinderPattern[][]) results.toArray(new FinderPattern[results.size()][]);
            }
            throw NotFoundException.getNotFoundInstance();
        }
        size2 = size;
        throw NotFoundException.getNotFoundInstance();
    }

    public FinderPatternInfo[] findMulti(Map<DecodeHintType, ?> hints) throws NotFoundException {
        Map<DecodeHintType, ?> map = hints;
        int i = 0;
        boolean tryHarder = map != null && map.containsKey(DecodeHintType.TRY_HARDER);
        BitMatrix image = getImage();
        int maxI = image.getHeight();
        int maxJ = image.getWidth();
        int iSkip = (int) ((((float) maxI) / 228.0f) * 1077936128);
        int i2 = 3;
        if (iSkip < 3 || tryHarder) {
            iSkip = 3;
        }
        int[] stateCount = new int[5];
        int i3 = iSkip - 1;
        while (i3 < maxI) {
            int i4;
            stateCount[0] = 0;
            stateCount[1] = 0;
            stateCount[2] = 0;
            stateCount[i2] = 0;
            stateCount[4] = 0;
            int currentState = 0;
            int j = 0;
            while (j < maxJ) {
                if (image.get(j, i3)) {
                    if ((currentState & 1) == 1) {
                        currentState++;
                    }
                    stateCount[currentState] = stateCount[currentState] + 1;
                    i4 = 3;
                } else if ((currentState & 1) != 0) {
                    i4 = 3;
                    stateCount[currentState] = stateCount[currentState] + 1;
                } else if (currentState == 4) {
                    if (FinderPatternFinder.foundPatternCross(stateCount) && handlePossibleCenter(stateCount, i3, j)) {
                        i2 = 0;
                        stateCount[0] = 0;
                        stateCount[1] = 0;
                        stateCount[2] = 0;
                        i4 = 3;
                        stateCount[3] = 0;
                        stateCount[4] = 0;
                    } else {
                        i4 = 3;
                        stateCount[0] = stateCount[2];
                        stateCount[1] = stateCount[3];
                        stateCount[2] = stateCount[4];
                        stateCount[3] = 1;
                        stateCount[4] = 0;
                        i2 = 3;
                    }
                    currentState = i2;
                } else {
                    i4 = 3;
                    currentState++;
                    stateCount[currentState] = stateCount[currentState] + 1;
                }
                j++;
                i2 = i4;
            }
            i4 = i2;
            if (FinderPatternFinder.foundPatternCross(stateCount)) {
                handlePossibleCenter(stateCount, i3, maxJ);
            }
            i3 += iSkip;
            i2 = i4;
        }
        FinderPattern[][] patternInfo = selectMutipleBestPatterns();
        List<FinderPatternInfo> result = new ArrayList();
        i3 = patternInfo.length;
        while (i < i3) {
            FinderPattern[] pattern = patternInfo[i];
            ResultPoint.orderBestPatterns(pattern);
            result.add(new FinderPatternInfo(pattern));
            i++;
        }
        if (result.isEmpty()) {
            return EMPTY_RESULT_ARRAY;
        }
        return (FinderPatternInfo[]) result.toArray(new FinderPatternInfo[result.size()]);
    }
}
