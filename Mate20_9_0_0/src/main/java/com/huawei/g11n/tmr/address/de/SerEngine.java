package com.huawei.g11n.tmr.address.de;

import com.huawei.g11n.tmr.util.PatternCache;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerEngine {
    /* JADX WARNING: Removed duplicated region for block: B:47:0x01a2  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0184  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int[] search(String sentence) {
        int i;
        int i2;
        int i3;
        int offset;
        int end;
        Matcher mTemp;
        int start;
        String str = sentence;
        int start2 = 0;
        boolean flag = true;
        List<int[]> indAddr = new LinkedList();
        Matcher mAddr = matchers(str, "pRegWT");
        while (true) {
            i = 3;
            i2 = 1;
            if (!mAddr.find()) {
                break;
            }
            i3 = 1;
            indAddr.add(new int[]{7, mAddr.start(), mAddr.end()});
        }
        Matcher mAddr2 = matchers(str, "pRegED");
        while (mAddr2.find()) {
            offset = noBlack(mAddr2.group(i2), mAddr2.group(2), mAddr2.group(3));
            if (1 != null) {
                start2 = mAddr2.start() + offset;
                end = mAddr2.end();
                mTemp = matchers(str.substring(end), "pReg_city");
                if (mTemp.lookingAt()) {
                    end += mTemp.end();
                    i3 = 1;
                    if (mTemp.group(1) != null) {
                        flag = false;
                    }
                } else {
                    i3 = 1;
                }
                i = 3;
                indAddr.add(new int[]{2, start2, end});
                i2 = i3;
            } else {
                i = 3;
                i2 = 1;
            }
        }
        boolean hasPrep = false;
        boolean hasOther = false;
        String sub = "";
        Matcher mAddr3 = matchers(str, "pRegED_Independ");
        while (mAddr3.find()) {
            boolean flag2;
            if (keyNotOnly(mAddr3.group(i2), mAddr3.group(i), mAddr3.group(4))) {
                int start3 = start2;
                offset = noBlack(mAddr3.group(1), mAddr3.group(2), mAddr3.group(3));
                if (1 != null) {
                    String sub2;
                    boolean flag3;
                    int[] iArr;
                    start2 = mAddr3.start();
                    end = mAddr3.end();
                    mAddr2 = str.substring(start2, end);
                    String temp = str.substring(end);
                    Matcher prepTemp = matchers(str.substring(0, start2), "pRegPrep");
                    mTemp = matchers(temp, "pReg_city");
                    Matcher strTemp = matchers(temp, "pRegST");
                    int offset2;
                    if (strTemp.lookingAt()) {
                        sub2 = str.substring(end, strTemp.start() + end);
                        offset2 = offset;
                        flag2 = flag;
                        hasOther = Pattern.compile("(\\p{Blank}+|\\p{Blank}*(-)\\p{Blank}*|\\p{Blank}*,\\p{Blank}*)").matcher(sub2).matches();
                    } else {
                        offset2 = offset;
                        flag2 = flag;
                        sub2 = sub;
                    }
                    if (mTemp.lookingAt() != 0) {
                        end += mTemp.end();
                        if (mTemp.group(1) != null) {
                            flag = false;
                            if (prepTemp.find() == 0) {
                                offset = prepTemp.end();
                                sub2 = str.substring(offset, start2);
                                int prepEnd = offset;
                                flag3 = flag;
                                hasPrep = Pattern.compile("\\p{Blank}{0,6}").matcher(sub2).matches();
                                sub = sub2;
                            } else {
                                flag3 = flag;
                                sub = sub2;
                            }
                            if (!hasPrep || hasOther) {
                                iArr = new int[3];
                                i2 = 1;
                                iArr[1] = start2;
                                iArr[2] = end;
                                indAddr.add(iArr);
                            } else {
                                i2 = 1;
                            }
                            flag = flag3;
                            i = 3;
                        }
                    }
                    flag = flag2;
                    if (prepTemp.find() == 0) {
                    }
                    if (hasPrep) {
                    }
                    iArr = new int[3];
                    i2 = 1;
                    iArr[1] = start2;
                    iArr[2] = end;
                    indAddr.add(iArr);
                    flag = flag3;
                    i = 3;
                } else {
                    flag2 = flag;
                    start2 = start3;
                    i = 3;
                    i2 = 1;
                }
            } else {
                flag2 = flag;
                i = 3;
                i2 = 1;
            }
        }
        mAddr2 = matchers(str, "pRegST");
        while (mAddr2.find()) {
            start = start2;
            offset = noBlack(mAddr2.group(i2), mAddr2.group(2), mAddr2.group(i));
            if (1 != null) {
                start2 = mAddr2.start();
                end = mAddr2.end();
                mAddr3 = str.substring(start2, end);
                mTemp = matchers(str.substring(end), "pReg_city");
                if (mTemp.lookingAt()) {
                    end += mTemp.end();
                    i2 = 1;
                    if (mTemp.group(1) != null) {
                    }
                } else {
                    i2 = 1;
                }
                int offset3 = offset;
                int[] iArr2 = new int[i];
                iArr2[0] = 1;
                iArr2[i2] = start2;
                iArr2[2] = end;
                indAddr.add(iArr2);
                offset = offset3;
            } else {
                start2 = start;
                i2 = 1;
            }
        }
        i3 = indAddr.size();
        if (i3 == 0) {
            return new int[i2];
        }
        i3 = merge(indAddr, i3);
        int[] re = new int[((i * i3) + i2)];
        re[0] = i3;
        i2 = 0;
        while (i2 < i3) {
            start = start2;
            re[(2 * i2) + 1] = ((int[]) indAddr.get(i2))[1];
            re[(2 * i2) + 2] = ((int[]) indAddr.get(i2))[2] - 1;
            i2++;
            start2 = start;
        }
        return re;
    }

    private static int merge(List<int[]> indx, int len) {
        Collections.sort(indx, new Comparator<int[]>() {
            public int compare(int[] pair1, int[] pair2) {
                if (pair1[1] < pair2[1]) {
                    return -1;
                }
                if (pair1[1] == pair2[1]) {
                    return 0;
                }
                return 1;
            }
        });
        len--;
        int i = 0;
        while (i < len) {
            if (((int[]) indx.get(i))[2] >= ((int[]) indx.get(i + 1))[1] && ((int[]) indx.get(i))[2] < ((int[]) indx.get(i + 1))[2]) {
                if (((int[]) indx.get(i))[2] > ((int[]) indx.get(i + 1))[1]) {
                    ((int[]) indx.get(i))[2] = ((int[]) indx.get(i + 1))[1];
                }
                if ((((int[]) indx.get(i))[0] & ((int[]) indx.get(i + 1))[0]) == 0 && !(((int[]) indx.get(i))[0] == 1 && ((int[]) indx.get(i + 1))[0] == 2)) {
                    ((int[]) indx.get(i))[2] = ((int[]) indx.get(i + 1))[2];
                    ((int[]) indx.get(i))[0] = ((int[]) indx.get(i))[0] | ((int[]) indx.get(i + 1))[0];
                    indx.remove(i + 1);
                    len--;
                    i--;
                }
            }
            i++;
        }
        return len + 1;
    }

    private static int noBlack(String wordsFirst, String wordSecond, String wordBlank) {
        if (wordsFirst.length() > 0) {
            Matcher mBlack = matchers(wordsFirst, "pRegBlackKeyIndi_withBlank");
            if (!mBlack.lookingAt()) {
                return 0;
            }
            int offset = mBlack.group().length();
            if (offset >= wordsFirst.length() && wordBlank.length() <= 0 && matchers(wordSecond, "pRegBlackKeyUnIndi").matches()) {
                return -1;
            }
            return offset;
        } else if (wordBlank.length() > 0) {
            if (matchers(wordSecond, "pRegBlackKeyIndi").matches()) {
                return -1;
            }
            return 0;
        } else if (matchers(wordSecond, "pRegBlackKeyUnIndi").matches()) {
            return -1;
        } else {
            return 0;
        }
    }

    private static boolean keyNotOnly(String wordsFirst, String wordBlank, String keyword) {
        if (wordsFirst == null || wordBlank == null || keyword == null) {
            return false;
        }
        if (matchers(keyword, "pRegBlackKeyNoSingal").matches() && wordBlank.length() == 0 && wordsFirst.length() == 0) {
            return false;
        }
        return true;
    }

    private static Matcher matchers(String t, String reg) {
        return PatternCache.getPattern(reg, "com.huawei.g11n.tmr.address.de.ReguExp").matcher(t);
    }
}
