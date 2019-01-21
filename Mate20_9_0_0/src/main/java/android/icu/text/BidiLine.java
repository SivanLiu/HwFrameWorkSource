package android.icu.text;

import java.util.Arrays;

final class BidiLine {
    BidiLine() {
    }

    static void setTrailingWSStart(Bidi bidi) {
        byte[] dirProps = bidi.dirProps;
        byte[] levels = bidi.levels;
        int start = bidi.length;
        byte paraLevel = bidi.paraLevel;
        if (dirProps[start - 1] == (byte) 7) {
            bidi.trailingWSStart = start;
            return;
        }
        while (start > 0 && (Bidi.DirPropFlag(dirProps[start - 1]) & Bidi.MASK_WS) != 0) {
            start--;
        }
        while (start > 0 && levels[start - 1] == paraLevel) {
            start--;
        }
        bidi.trailingWSStart = start;
    }

    static Bidi setLine(Bidi paraBidi, int start, int limit) {
        Bidi lineBidi = new Bidi();
        int length = limit - start;
        lineBidi.resultLength = length;
        lineBidi.originalLength = length;
        lineBidi.length = length;
        lineBidi.text = new char[length];
        System.arraycopy(paraBidi.text, start, lineBidi.text, 0, length);
        lineBidi.paraLevel = paraBidi.GetParaLevelAt(start);
        lineBidi.paraCount = paraBidi.paraCount;
        lineBidi.runs = new BidiRun[0];
        lineBidi.reorderingMode = paraBidi.reorderingMode;
        lineBidi.reorderingOptions = paraBidi.reorderingOptions;
        if (paraBidi.controlCount > 0) {
            for (int j = start; j < limit; j++) {
                if (Bidi.IsBidiControlChar(paraBidi.text[j])) {
                    lineBidi.controlCount++;
                }
            }
            lineBidi.resultLength -= lineBidi.controlCount;
        }
        lineBidi.getDirPropsMemory(length);
        lineBidi.dirProps = lineBidi.dirPropsMemory;
        System.arraycopy(paraBidi.dirProps, start, lineBidi.dirProps, 0, length);
        lineBidi.getLevelsMemory(length);
        lineBidi.levels = lineBidi.levelsMemory;
        System.arraycopy(paraBidi.levels, start, lineBidi.levels, 0, length);
        lineBidi.runCount = -1;
        if (paraBidi.direction == (byte) 2) {
            byte[] levels = lineBidi.levels;
            setTrailingWSStart(lineBidi);
            int trailingWSStart = lineBidi.trailingWSStart;
            if (trailingWSStart == 0) {
                lineBidi.direction = (byte) (lineBidi.paraLevel & 1);
            } else {
                byte level = (byte) (levels[0] & (byte) 1);
                if (trailingWSStart >= length || (lineBidi.paraLevel & 1) == level) {
                    for (int i = 1; i != trailingWSStart; i++) {
                        if ((levels[i] & 1) != level) {
                            lineBidi.direction = (byte) 2;
                            break;
                        }
                    }
                    lineBidi.direction = level;
                } else {
                    lineBidi.direction = (byte) 2;
                }
            }
            switch (lineBidi.direction) {
                case (byte) 0:
                    lineBidi.paraLevel = (byte) ((lineBidi.paraLevel + 1) & -2);
                    lineBidi.trailingWSStart = 0;
                    break;
                case (byte) 1:
                    lineBidi.paraLevel = (byte) (1 | lineBidi.paraLevel);
                    lineBidi.trailingWSStart = 0;
                    break;
            }
        }
        lineBidi.direction = paraBidi.direction;
        if (paraBidi.trailingWSStart <= start) {
            lineBidi.trailingWSStart = 0;
        } else if (paraBidi.trailingWSStart < limit) {
            lineBidi.trailingWSStart = paraBidi.trailingWSStart - start;
        } else {
            lineBidi.trailingWSStart = length;
        }
        lineBidi.paraBidi = paraBidi;
        return lineBidi;
    }

    static byte getLevelAt(Bidi bidi, int charIndex) {
        if (bidi.direction != (byte) 2 || charIndex >= bidi.trailingWSStart) {
            return bidi.GetParaLevelAt(charIndex);
        }
        return bidi.levels[charIndex];
    }

    static byte[] getLevels(Bidi bidi) {
        int start = bidi.trailingWSStart;
        int length = bidi.length;
        if (start != length) {
            Arrays.fill(bidi.levels, start, length, bidi.paraLevel);
            bidi.trailingWSStart = length;
        }
        if (length >= bidi.levels.length) {
            return bidi.levels;
        }
        byte[] levels = new byte[length];
        System.arraycopy(bidi.levels, 0, levels, 0, length);
        return levels;
    }

    static BidiRun getLogicalRun(Bidi bidi, int logicalPosition) {
        BidiRun newRun = new BidiRun();
        getRuns(bidi);
        int runCount = bidi.runCount;
        int visualStart = 0;
        int logicalLimit = 0;
        int i = 0;
        BidiRun iRun = bidi.runs[0];
        while (i < runCount) {
            iRun = bidi.runs[i];
            logicalLimit = (iRun.start + iRun.limit) - visualStart;
            if (logicalPosition >= iRun.start && logicalPosition < logicalLimit) {
                break;
            }
            visualStart = iRun.limit;
            i++;
        }
        newRun.start = iRun.start;
        newRun.limit = logicalLimit;
        newRun.level = iRun.level;
        return newRun;
    }

    static BidiRun getVisualRun(Bidi bidi, int runIndex) {
        int limit;
        int start = bidi.runs[runIndex].start;
        byte level = bidi.runs[runIndex].level;
        if (runIndex > 0) {
            limit = (bidi.runs[runIndex].limit + start) - bidi.runs[runIndex - 1].limit;
        } else {
            limit = bidi.runs[0].limit + start;
        }
        return new BidiRun(start, limit, level);
    }

    static void getSingleRun(Bidi bidi, byte level) {
        bidi.runs = bidi.simpleRuns;
        bidi.runCount = 1;
        bidi.runs[0] = new BidiRun(0, bidi.length, level);
    }

    private static void reorderLine(Bidi bidi, byte minLevel, byte maxLevel) {
        if (maxLevel > (minLevel | 1)) {
            int firstRun;
            minLevel = (byte) (minLevel + 1);
            BidiRun[] runs = bidi.runs;
            byte[] levels = bidi.levels;
            int runCount = bidi.runCount;
            if (bidi.trailingWSStart < bidi.length) {
                runCount--;
            }
            while (true) {
                maxLevel = (byte) (maxLevel - 1);
                if (maxLevel < minLevel) {
                    break;
                }
                firstRun = 0;
                while (true) {
                    if (firstRun < runCount && levels[runs[firstRun].start] < maxLevel) {
                        firstRun++;
                    } else if (firstRun >= runCount) {
                        break;
                    } else {
                        int limitRun = firstRun;
                        while (true) {
                            limitRun++;
                            if (limitRun >= runCount || levels[runs[limitRun].start] < maxLevel) {
                            }
                        }
                        for (int endRun = limitRun - 1; firstRun < endRun; endRun--) {
                            BidiRun tempRun = runs[firstRun];
                            runs[firstRun] = runs[endRun];
                            runs[endRun] = tempRun;
                            firstRun++;
                        }
                        if (limitRun == runCount) {
                            break;
                        }
                        firstRun = limitRun + 1;
                    }
                }
            }
            if ((minLevel & 1) == 0) {
                firstRun = 0;
                if (bidi.trailingWSStart == bidi.length) {
                    runCount--;
                }
                while (firstRun < runCount) {
                    BidiRun tempRun2 = runs[firstRun];
                    runs[firstRun] = runs[runCount];
                    runs[runCount] = tempRun2;
                    firstRun++;
                    runCount--;
                }
            }
        }
    }

    static int getRunFromLogicalIndex(Bidi bidi, int logicalIndex) {
        BidiRun[] runs = bidi.runs;
        int runCount = bidi.runCount;
        int visualStart = 0;
        for (int i = 0; i < runCount; i++) {
            int length = runs[i].limit - visualStart;
            int logicalStart = runs[i].start;
            if (logicalIndex >= logicalStart && logicalIndex < logicalStart + length) {
                return i;
            }
            visualStart += length;
        }
        throw new IllegalStateException("Internal ICU error in getRunFromLogicalIndex");
    }

    static void getRuns(Bidi bidi) {
        Bidi bidi2 = bidi;
        if (bidi2.runCount < 0) {
            int length;
            BidiRun bidiRun;
            if (bidi2.direction != (byte) 2) {
                getSingleRun(bidi2, bidi2.paraLevel);
            } else {
                int i;
                length = bidi2.length;
                byte[] levels = bidi2.levels;
                int limit = bidi2.trailingWSStart;
                int runCount = 0;
                byte level = (byte) -1;
                for (i = 0; i < limit; i++) {
                    if (levels[i] != level) {
                        runCount++;
                        level = levels[i];
                    }
                }
                if (runCount == 1 && limit == length) {
                    getSingleRun(bidi2, levels[0]);
                } else {
                    byte minLevel = Bidi.LEVEL_DEFAULT_LTR;
                    byte maxLevel = (byte) 0;
                    if (limit < length) {
                        runCount++;
                    }
                    bidi2.getRunsMemory(runCount);
                    BidiRun[] runs = bidi2.runsMemory;
                    int runIndex = 0;
                    i = 0;
                    do {
                        int start = i;
                        level = levels[i];
                        if (level < minLevel) {
                            minLevel = level;
                        }
                        if (level > maxLevel) {
                            maxLevel = level;
                        }
                        while (true) {
                            i++;
                            if (i >= limit || levels[i] != level) {
                                runs[runIndex] = new BidiRun(start, i - start, level);
                                runIndex++;
                            }
                        }
                        runs[runIndex] = new BidiRun(start, i - start, level);
                        runIndex++;
                    } while (i < limit);
                    if (limit < length) {
                        runs[runIndex] = new BidiRun(limit, length - limit, bidi2.paraLevel);
                        if (bidi2.paraLevel < minLevel) {
                            minLevel = bidi2.paraLevel;
                        }
                    }
                    bidi2.runs = runs;
                    bidi2.runCount = runCount;
                    reorderLine(bidi2, minLevel, maxLevel);
                    int limit2 = 0;
                    for (i = 0; i < runCount; i++) {
                        runs[i].level = levels[runs[i].start];
                        BidiRun bidiRun2 = runs[i];
                        int i2 = bidiRun2.limit + limit2;
                        bidiRun2.limit = i2;
                        limit2 = i2;
                    }
                    if (runIndex < runCount) {
                        runs[(bidi2.paraLevel & 1) != 0 ? 0 : runIndex].level = bidi2.paraLevel;
                    }
                }
            }
            if (bidi2.insertPoints.size > 0) {
                for (length = 0; length < bidi2.insertPoints.size; length++) {
                    Point point = bidi2.insertPoints.points[length];
                    bidiRun = bidi2.runs[getRunFromLogicalIndex(bidi2, point.pos)];
                    bidiRun.insertRemove |= point.flag;
                }
            }
            if (bidi2.controlCount > 0) {
                int ic = 0;
                while (true) {
                    length = ic;
                    if (length >= bidi2.length) {
                        break;
                    }
                    if (Bidi.IsBidiControlChar(bidi2.text[length])) {
                        bidiRun = bidi2.runs[getRunFromLogicalIndex(bidi2, length)];
                        bidiRun.insertRemove--;
                    }
                    ic = length + 1;
                }
            }
        }
    }

    static int[] prepareReorder(byte[] levels, byte[] pMinLevel, byte[] pMaxLevel) {
        if (levels == null || levels.length <= 0) {
            return null;
        }
        byte minLevel = Bidi.LEVEL_DEFAULT_LTR;
        byte maxLevel = (byte) 0;
        int start = levels.length;
        while (start > 0) {
            start--;
            byte level = levels[start];
            if (level < (byte) 0 || level > Bidi.LEVEL_DEFAULT_LTR) {
                return null;
            }
            if (level < minLevel) {
                minLevel = level;
            }
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        pMinLevel[0] = minLevel;
        pMaxLevel[0] = maxLevel;
        int[] indexMap = new int[levels.length];
        start = levels.length;
        while (start > 0) {
            start--;
            indexMap[start] = start;
        }
        return indexMap;
    }

    static int[] reorderLogical(byte[] levels) {
        byte[] aMinLevel = new byte[1];
        byte[] aMaxLevel = new byte[1];
        int[] indexMap = prepareReorder(levels, aMinLevel, aMaxLevel);
        if (indexMap == null) {
            return null;
        }
        byte minLevel = aMinLevel[0];
        byte maxLevel = aMaxLevel[0];
        if (minLevel == maxLevel && (minLevel & 1) == 0) {
            return indexMap;
        }
        minLevel = (byte) (minLevel | 1);
        do {
            int start = 0;
            while (true) {
                if (start < levels.length && levels[start] < maxLevel) {
                    start++;
                } else if (start >= levels.length) {
                    break;
                } else {
                    int sumOfSosEos;
                    int limit = start;
                    while (true) {
                        limit++;
                        if (limit >= levels.length || levels[limit] < maxLevel) {
                            sumOfSosEos = (start + limit) - 1;
                        }
                    }
                    sumOfSosEos = (start + limit) - 1;
                    do {
                        indexMap[start] = sumOfSosEos - indexMap[start];
                        start++;
                    } while (start < limit);
                    if (limit == levels.length) {
                        break;
                    }
                    start = limit + 1;
                }
            }
            maxLevel = (byte) (maxLevel - 1);
        } while (maxLevel >= minLevel);
        return indexMap;
    }

    static int[] reorderVisual(byte[] levels) {
        byte[] aMinLevel = new byte[1];
        byte[] aMaxLevel = new byte[1];
        int[] indexMap = prepareReorder(levels, aMinLevel, aMaxLevel);
        if (indexMap == null) {
            return null;
        }
        byte minLevel = aMinLevel[0];
        byte maxLevel = aMaxLevel[0];
        if (minLevel == maxLevel && (minLevel & 1) == 0) {
            return indexMap;
        }
        minLevel = (byte) (minLevel | 1);
        do {
            int start = 0;
            while (true) {
                if (start < levels.length && levels[start] < maxLevel) {
                    start++;
                } else if (start >= levels.length) {
                    break;
                } else {
                    int limit = start;
                    while (true) {
                        limit++;
                        if (limit >= levels.length || levels[limit] < maxLevel) {
                        }
                    }
                    for (int end = limit - 1; start < end; end--) {
                        int temp = indexMap[start];
                        indexMap[start] = indexMap[end];
                        indexMap[end] = temp;
                        start++;
                    }
                    if (limit == levels.length) {
                        break;
                    }
                    start = limit + 1;
                }
            }
            maxLevel = (byte) (maxLevel - 1);
        } while (maxLevel >= minLevel);
        return indexMap;
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0045 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int getVisualIndex(Bidi bidi, int logicalIndex) {
        int visualIndex;
        int visualStart;
        int i;
        int length;
        int offset;
        int visualIndex2 = -1;
        int i2 = 0;
        switch (bidi.direction) {
            case (byte) 0:
                visualIndex = logicalIndex;
                break;
            case (byte) 1:
                visualIndex = (bidi.length - logicalIndex) - 1;
                break;
            default:
                getRuns(bidi);
                BidiRun[] runs = bidi.runs;
                visualStart = 0;
                for (i = 0; i < bidi.runCount; i++) {
                    length = runs[i].limit - visualStart;
                    offset = logicalIndex - runs[i].start;
                    if (offset >= 0 && offset < length) {
                        if (runs[i].isEvenRun()) {
                            visualIndex2 = visualStart + offset;
                        } else {
                            visualIndex2 = ((visualStart + length) - offset) - 1;
                        }
                        if (i >= bidi.runCount) {
                            visualIndex = visualIndex2;
                            break;
                        }
                        return -1;
                    }
                    visualStart += length;
                }
                if (i >= bidi.runCount) {
                }
                break;
        }
        BidiRun[] runs2;
        int visualStart2;
        if (bidi.insertPoints.size > 0) {
            runs2 = bidi.runs;
            visualStart2 = 0;
            i = 0;
            while (true) {
                visualStart = runs2[i2].limit - visualStart2;
                length = runs2[i2].insertRemove;
                if ((length & 5) > 0) {
                    i++;
                }
                if (visualIndex < runs2[i2].limit) {
                    return visualIndex + i;
                }
                if ((length & 10) > 0) {
                    i++;
                }
                i2++;
                visualStart2 += visualStart;
            }
        } else if (bidi.controlCount <= 0) {
            return visualIndex;
        } else {
            runs2 = bidi.runs;
            i = 0;
            visualStart = 0;
            if (Bidi.IsBidiControlChar(bidi.text[logicalIndex])) {
                return -1;
            }
            while (true) {
                visualStart2 = i2;
                i2 = runs2[visualStart2].limit - i;
                offset = runs2[visualStart2].insertRemove;
                if (visualIndex >= runs2[visualStart2].limit) {
                    visualStart -= offset;
                    i += i2;
                    i2 = visualStart2 + 1;
                } else if (offset == 0) {
                    return visualIndex - visualStart;
                } else {
                    int start;
                    int limit;
                    if (runs2[visualStart2].isEvenRun()) {
                        start = runs2[visualStart2].start;
                        limit = logicalIndex;
                    } else {
                        start = logicalIndex + 1;
                        limit = runs2[visualStart2].start + i2;
                    }
                    int controlFound = visualStart;
                    for (visualStart = start; visualStart < limit; visualStart++) {
                        if (Bidi.IsBidiControlChar(bidi.text[visualStart])) {
                            controlFound++;
                        }
                    }
                    return visualIndex - controlFound;
                }
            }
        }
    }

    static int getLogicalIndex(Bidi bidi, int visualIndex) {
        int i;
        int i2;
        Bidi bidi2 = bidi;
        int visualIndex2 = visualIndex;
        BidiRun[] runs = bidi2.runs;
        int runCount = bidi2.runCount;
        int visualStart;
        int markFound;
        int length;
        int insertRemove;
        if (bidi2.insertPoints.size > 0) {
            visualStart = 0;
            markFound = 0;
            i = 0;
            while (true) {
                length = runs[i].limit - visualStart;
                insertRemove = runs[i].insertRemove;
                if ((insertRemove & 5) > 0) {
                    if (visualIndex2 <= visualStart + markFound) {
                        return -1;
                    }
                    markFound++;
                }
                if (visualIndex2 < runs[i].limit + markFound) {
                    visualIndex2 -= markFound;
                    break;
                }
                if ((insertRemove & 10) > 0) {
                    if (visualIndex2 == (visualStart + length) + markFound) {
                        return -1;
                    }
                    markFound++;
                }
                i++;
                visualStart += length;
            }
        } else if (bidi2.controlCount > 0) {
            visualStart = 0;
            markFound = 0;
            i = 0;
            while (true) {
                length = runs[i].limit - visualStart;
                insertRemove = runs[i].insertRemove;
                if (visualIndex2 < (runs[i].limit - markFound) + insertRemove) {
                    break;
                }
                markFound -= insertRemove;
                i++;
                visualStart += length;
            }
            if (insertRemove == 0) {
                visualIndex2 += markFound;
            } else {
                int logicalStart = runs[i].start;
                boolean evenRun = runs[i].isEvenRun();
                int logicalEnd = (logicalStart + length) - 1;
                int controlFound = markFound;
                markFound = 0;
                while (markFound < length) {
                    if (Bidi.IsBidiControlChar(bidi2.text[evenRun ? logicalStart + markFound : logicalEnd - markFound])) {
                        controlFound++;
                    }
                    if (visualIndex2 + controlFound == visualStart + markFound) {
                        break;
                    }
                    markFound++;
                    bidi2 = bidi;
                }
                visualIndex2 += controlFound;
            }
        }
        if (runCount <= 10) {
            int i3 = 0;
            while (true) {
                i2 = i3;
                if (visualIndex2 < runs[i2].limit) {
                    break;
                }
                i3 = i2 + 1;
            }
        } else {
            int i4;
            i = 0;
            i2 = runCount;
            while (true) {
                i4 = (i + i2) >>> 1;
                if (visualIndex2 >= runs[i4].limit) {
                    i = i4 + 1;
                } else if (i4 == 0 || visualIndex2 >= runs[i4 - 1].limit) {
                    i2 = i4;
                } else {
                    i2 = i4;
                }
            }
            i2 = i4;
        }
        i = runs[i2].start;
        if (!runs[i2].isEvenRun()) {
            return ((runs[i2].limit + i) - visualIndex2) - 1;
        }
        if (i2 > 0) {
            visualIndex2 -= runs[i2 - 1].limit;
        }
        return i + visualIndex2;
    }

    static int[] getLogicalMap(Bidi bidi) {
        int j;
        int logicalStart;
        int visualLimit;
        int logicalStart2;
        int visualStart;
        Bidi bidi2 = bidi;
        BidiRun[] runs = bidi2.runs;
        int[] indexMap = new int[bidi2.length];
        if (bidi2.length > bidi2.resultLength) {
            Arrays.fill(indexMap, -1);
        }
        int visualStart2 = 0;
        for (j = 0; j < bidi2.runCount; j++) {
            logicalStart = runs[j].start;
            visualLimit = runs[j].limit;
            if (runs[j].isEvenRun()) {
                while (true) {
                    logicalStart2 = logicalStart + 1;
                    visualStart = visualStart2 + 1;
                    indexMap[logicalStart] = visualStart2;
                    if (visualStart >= visualLimit) {
                        break;
                    }
                    logicalStart = logicalStart2;
                    visualStart2 = visualStart;
                }
                logicalStart = logicalStart2;
                visualStart2 = visualStart;
            } else {
                logicalStart += visualLimit - visualStart2;
                while (true) {
                    logicalStart--;
                    logicalStart2 = visualStart2 + 1;
                    indexMap[logicalStart] = visualStart2;
                    if (logicalStart2 >= visualLimit) {
                        break;
                    }
                    visualStart2 = logicalStart2;
                }
                visualStart2 = logicalStart2;
            }
        }
        int j2;
        if (bidi2.insertPoints.size > 0) {
            j = 0;
            int runCount = bidi2.runCount;
            runs = bidi2.runs;
            visualStart2 = 0;
            int i = 0;
            while (i < runCount) {
                logicalStart = runs[i].limit - visualStart2;
                visualLimit = runs[i].insertRemove;
                if ((visualLimit & 5) > 0) {
                    j++;
                }
                if (j > 0) {
                    logicalStart2 = runs[i].start;
                    visualStart = logicalStart2 + logicalStart;
                    for (j2 = logicalStart2; j2 < visualStart; j2++) {
                        indexMap[j2] = indexMap[j2] + j;
                    }
                }
                if ((visualLimit & 10) > 0) {
                    j++;
                }
                i++;
                visualStart2 += logicalStart;
            }
        } else if (bidi2.controlCount > 0) {
            logicalStart = bidi2.runCount;
            runs = bidi2.runs;
            visualLimit = 0;
            visualStart2 = 0;
            j = 0;
            while (j < logicalStart) {
                logicalStart2 = runs[j].limit - visualLimit;
                visualStart = runs[j].insertRemove;
                if (visualStart2 - visualStart != 0) {
                    j2 = runs[j].start;
                    boolean evenRun = runs[j].isEvenRun();
                    int logicalLimit = j2 + logicalStart2;
                    int j3;
                    if (visualStart == 0) {
                        for (j3 = j2; j3 < logicalLimit; j3++) {
                            indexMap[j3] = indexMap[j3] - visualStart2;
                        }
                    } else {
                        j3 = visualStart2;
                        visualStart2 = 0;
                        while (visualStart2 < logicalStart2) {
                            int k = evenRun ? j2 + visualStart2 : (logicalLimit - visualStart2) - 1;
                            if (Bidi.IsBidiControlChar(bidi2.text[k])) {
                                j3++;
                                indexMap[k] = -1;
                            } else {
                                indexMap[k] = indexMap[k] - j3;
                            }
                            visualStart2++;
                        }
                        visualStart2 = j3;
                    }
                }
                j++;
                visualLimit += logicalStart2;
            }
        }
        return indexMap;
    }

    static int[] getVisualMap(Bidi bidi) {
        int allocLength;
        int j;
        int logicalStart;
        int visualLimit;
        int idx;
        int logicalStart2;
        Bidi bidi2 = bidi;
        BidiRun[] runs = bidi2.runs;
        if (bidi2.length > bidi2.resultLength) {
            allocLength = bidi2.length;
        } else {
            allocLength = bidi2.resultLength;
        }
        int[] indexMap = new int[allocLength];
        int idx2 = 0;
        int visualStart = 0;
        for (j = 0; j < bidi2.runCount; j++) {
            logicalStart = runs[j].start;
            visualLimit = runs[j].limit;
            if (runs[j].isEvenRun()) {
                while (true) {
                    idx = idx2 + 1;
                    logicalStart2 = logicalStart + 1;
                    indexMap[idx2] = logicalStart;
                    visualStart++;
                    if (visualStart >= visualLimit) {
                        break;
                    }
                    idx2 = idx;
                    logicalStart = logicalStart2;
                }
                idx2 = idx;
                logicalStart = logicalStart2;
            } else {
                logicalStart += visualLimit - visualStart;
                while (true) {
                    logicalStart2 = idx2 + 1;
                    logicalStart--;
                    indexMap[idx2] = logicalStart;
                    visualStart++;
                    if (visualStart >= visualLimit) {
                        break;
                    }
                    idx2 = logicalStart2;
                }
                idx2 = logicalStart2;
            }
        }
        int insertRemove;
        int j2;
        if (bidi2.insertPoints.size > 0) {
            logicalStart = bidi2.runCount;
            runs = bidi2.runs;
            visualLimit = 0;
            for (j = 0; j < logicalStart; j++) {
                logicalStart2 = runs[j].insertRemove;
                if ((logicalStart2 & 5) > 0) {
                    visualLimit++;
                }
                if ((logicalStart2 & 10) > 0) {
                    visualLimit++;
                }
            }
            logicalStart2 = bidi2.resultLength;
            j = logicalStart - 1;
            while (j >= 0 && visualLimit > 0) {
                insertRemove = runs[j].insertRemove;
                if ((insertRemove & 10) > 0) {
                    logicalStart2--;
                    indexMap[logicalStart2] = -1;
                    visualLimit--;
                }
                visualStart = j > 0 ? runs[j - 1].limit : 0;
                for (j2 = runs[j].limit - 1; j2 >= visualStart && visualLimit > 0; j2--) {
                    logicalStart2--;
                    indexMap[logicalStart2] = indexMap[j2];
                }
                if ((insertRemove & 5) > 0) {
                    logicalStart2--;
                    indexMap[logicalStart2] = -1;
                    visualLimit--;
                }
                j--;
            }
        } else if (bidi2.controlCount > 0) {
            j = bidi2.runCount;
            runs = bidi2.runs;
            logicalStart = 0;
            idx = 0;
            visualStart = 0;
            while (visualStart < j) {
                visualLimit = runs[visualStart].limit - idx;
                logicalStart2 = runs[visualStart].insertRemove;
                int k;
                if (logicalStart2 == 0 && logicalStart == idx) {
                    logicalStart += visualLimit;
                } else if (logicalStart2 == 0) {
                    insertRemove = runs[visualStart].limit;
                    j2 = logicalStart;
                    logicalStart = idx;
                    while (logicalStart < insertRemove) {
                        k = j2 + 1;
                        indexMap[j2] = indexMap[logicalStart];
                        logicalStart++;
                        j2 = k;
                    }
                    logicalStart = j2;
                } else {
                    insertRemove = runs[visualStart].start;
                    boolean evenRun = runs[visualStart].isEvenRun();
                    k = (insertRemove + visualLimit) - 1;
                    int k2 = logicalStart;
                    logicalStart = 0;
                    while (logicalStart < visualLimit) {
                        int m = evenRun ? insertRemove + logicalStart : k - logicalStart;
                        if (!Bidi.IsBidiControlChar(bidi2.text[m])) {
                            int k3 = k2 + 1;
                            indexMap[k2] = m;
                            k2 = k3;
                        }
                        logicalStart++;
                    }
                    logicalStart = k2;
                }
                visualStart++;
                idx += visualLimit;
            }
        }
        if (allocLength == bidi2.resultLength) {
            return indexMap;
        }
        int[] newMap = new int[bidi2.resultLength];
        System.arraycopy(indexMap, 0, newMap, 0, bidi2.resultLength);
        return newMap;
    }

    static int[] invertMap(int[] srcMap) {
        int i;
        int destLength = -1;
        int count = 0;
        for (int srcEntry : srcMap) {
            if (srcEntry > destLength) {
                destLength = srcEntry;
            }
            if (srcEntry >= 0) {
                count++;
            }
        }
        destLength++;
        int[] destMap = new int[destLength];
        if (count < destLength) {
            Arrays.fill(destMap, -1);
        }
        for (i = 0; i < srcLength; i++) {
            int srcEntry2 = srcMap[i];
            if (srcEntry2 >= 0) {
                destMap[srcEntry2] = i;
            }
        }
        return destMap;
    }
}
