package android.text;

import android.icu.lang.UCharacter;
import android.icu.text.Bidi;
import android.icu.text.BidiClassifier;
import android.text.Layout.Directions;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;

@VisibleForTesting(visibility = Visibility.PACKAGE)
public class AndroidBidi {
    private static final EmojiBidiOverride sEmojiBidiOverride = new EmojiBidiOverride();

    public static class EmojiBidiOverride extends BidiClassifier {
        private static final int NO_OVERRIDE = (UCharacter.getIntPropertyMaxValue(4096) + 1);

        public EmojiBidiOverride() {
            super(null);
        }

        public int classify(int c) {
            if (Emoji.isNewEmoji(c)) {
                return 10;
            }
            return NO_OVERRIDE;
        }
    }

    public static int bidi(int dir, char[] chs, byte[] chInfo) {
        if (chs == null || chInfo == null) {
            throw new NullPointerException();
        }
        int length = chs.length;
        if (chInfo.length >= length) {
            byte paraLevel;
            int i = 0;
            switch (dir) {
                case -2:
                    paraLevel = Byte.MAX_VALUE;
                    break;
                case -1:
                    paraLevel = (byte) 1;
                    break;
                case 1:
                    paraLevel = (byte) 0;
                    break;
                case 2:
                    paraLevel = (byte) 126;
                    break;
                default:
                    paraLevel = (byte) 0;
                    break;
            }
            Bidi icuBidi = new Bidi(length, 0);
            icuBidi.setCustomClassifier(sEmojiBidiOverride);
            icuBidi.setPara(chs, paraLevel, null);
            while (i < length) {
                chInfo[i] = icuBidi.getLevelAt(i);
                i++;
            }
            return (icuBidi.getParaLevel() & 1) == 0 ? 1 : -1;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public static Directions directions(int dir, byte[] levels, int lstart, char[] chars, int cstart, int len) {
        int i = len;
        if (i == 0) {
            return Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
        int i2;
        int baseLevel = dir == 1 ? 0 : 1;
        int curLevel = levels[lstart];
        int curLevel2 = curLevel;
        int runCount = 1;
        int e = lstart + i;
        for (i2 = lstart + 1; i2 < e; i2++) {
            int level = levels[i2];
            if (level != curLevel) {
                curLevel = level;
                runCount++;
            }
        }
        i2 = i;
        if ((curLevel & 1) != (baseLevel & 1)) {
            while (true) {
                i2--;
                if (i2 < 0) {
                    break;
                }
                char ch = chars[cstart + i2];
                if (ch != 10) {
                    if (ch != ' ' && ch != 9) {
                        break;
                    }
                } else {
                    i2--;
                    break;
                }
            }
            i2++;
            if (i2 != i) {
                runCount++;
            }
        }
        if (runCount != 1 || curLevel2 != baseLevel) {
            boolean swap;
            int[] ld = new int[(runCount * 2)];
            int maxLevel = curLevel2;
            int levelBits = curLevel2 << 26;
            int n = 1;
            int prev = lstart;
            int e2 = lstart + i2;
            int minLevel = curLevel2;
            curLevel = lstart;
            while (true) {
                int e3 = e2;
                if (curLevel >= e3) {
                    break;
                }
                int e4 = e3;
                e3 = levels[curLevel];
                if (e3 != curLevel2) {
                    curLevel2 = e3;
                    if (e3 > maxLevel) {
                        maxLevel = e3;
                    } else if (e3 < minLevel) {
                        minLevel = e3;
                    }
                    int n2 = n + 1;
                    ld[n] = (curLevel - prev) | levelBits;
                    n = n2 + 1;
                    ld[n2] = curLevel - lstart;
                    levelBits = curLevel2 << 26;
                    prev = curLevel;
                }
                curLevel++;
                e2 = e4;
            }
            ld[n] = ((lstart + i2) - prev) | levelBits;
            if (i2 < i) {
                n++;
                ld[n] = i2;
                ld[n + 1] = (i - i2) | (baseLevel << 26);
            }
            if ((minLevel & 1) == baseLevel) {
                minLevel++;
                swap = maxLevel > minLevel;
            } else {
                swap = true;
                if (runCount <= 1) {
                    swap = false;
                }
            }
            if (swap) {
                byte level2 = maxLevel - 1;
                while (level2 >= minLevel) {
                    n = 0;
                    while (true) {
                        i = n;
                        if (i >= ld.length) {
                            break;
                        }
                        int e5;
                        int minLevel2;
                        if (levels[ld[i]] >= level2) {
                            int e6;
                            e5 = i + 2;
                            while (true) {
                                e6 = e5;
                                minLevel2 = minLevel;
                                if (e6 >= ld.length || levels[ld[e6]] < level2) {
                                    minLevel = i;
                                    n = e6 - 2;
                                } else {
                                    e5 = e6 + 2;
                                    minLevel = minLevel2;
                                }
                            }
                            minLevel = i;
                            n = e6 - 2;
                            while (true) {
                                int hi = n;
                                if (minLevel >= hi) {
                                    break;
                                }
                                n = ld[minLevel];
                                ld[minLevel] = ld[hi];
                                ld[hi] = n;
                                n = ld[minLevel + 1];
                                ld[minLevel + 1] = ld[hi + 1];
                                ld[hi + 1] = n;
                                minLevel += 2;
                                n = hi - 2;
                            }
                            e5 = e6 + 2;
                        } else {
                            minLevel2 = minLevel;
                            e5 = i;
                        }
                        n = e5 + 2;
                        minLevel = minLevel2;
                        i = len;
                    }
                    level2--;
                    i = len;
                }
            }
            return new Directions(ld);
        } else if ((curLevel2 & 1) != 0) {
            return Layout.DIRS_ALL_RIGHT_TO_LEFT;
        } else {
            return Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
    }
}
