package com.huawei.g11n.tmr.address.pt;

import com.huawei.g11n.tmr.util.PatternCache;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

public class SerEngine {
    private static final int BUI_MARK = 2;
    private static final int POS_MARK = 4;
    private static final int STR_MARK = 1;

    public static void main(String[] arg) {
        String a = "Rua da Torre 274 RC ESQ  Edifício EPAL, Rua da Torre 234  Edifício EPAL, RUA FLORES 14 RC ESQ,BAIRRO DA PEDRA,2785-174 SÃO DOMINGOS DE RANA,PORTUGAL";
        int[] r = search(a);
        for (int i = 0; i < r[0]; i++) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder("识别总个数：");
            stringBuilder.append(r[0]);
            printStream.println(stringBuilder.toString());
            PrintStream printStream2 = System.out;
            StringBuilder stringBuilder2 = new StringBuilder(String.valueOf(r[(i * 2) + 1]));
            stringBuilder2.append(",");
            stringBuilder2.append(r[(i * 2) + 2]);
            printStream2.println(stringBuilder2.toString());
            System.out.println(a.substring(r[(i * 2) + 1], r[(i * 2) + 2] + 1));
        }
    }

    public static int[] search(String sentence) {
        int type;
        int start;
        int end;
        String restSen;
        Matcher mTemp;
        List<int[]> indAddr = new LinkedList();
        Matcher mAll = matchers(sentence, "pbs");
        while (mAll.find()) {
            type = 0;
            if (mAll.group("street1") != null) {
                type = 0 | 1;
            }
            if (mAll.group("street2") != null) {
                type |= 1;
            }
            if (mAll.group("build") != null) {
                type |= 2;
            }
            start = mAll.start();
            end = mAll.end();
            restSen = sentence.substring(end);
            if (matchers(restSen, "pnum").find()) {
                mTemp = matchers(restSen, "pCityWithCode");
                if (mTemp.lookingAt()) {
                    type |= 4;
                    end += mTemp.end();
                }
            } else {
                mTemp = matchers(restSen, "pCityNoCode");
                if (mTemp.lookingAt()) {
                    end += mTemp.end();
                }
            }
            indAddr.add(new int[]{start, end, type});
        }
        Matcher mAll2 = matchers(sentence, "pat");
        while (mAll2.find()) {
            mAll = 2;
            start = mAll2.start(1);
            end = mAll2.end(1);
            restSen = sentence.substring(end);
            if (matchers(restSen, "pnum").find()) {
                mTemp = matchers(restSen, "pCityWithCode");
                if (mTemp.lookingAt()) {
                    mAll = 2 | 4;
                    end += mTemp.end();
                }
            } else {
                mTemp = matchers(restSen, "pCityNoCode");
                if (mTemp.lookingAt()) {
                    end += mTemp.end();
                }
            }
            indAddr.add(new int[]{start, end, mAll});
        }
        mAll = matchers(sentence, "pPos");
        while (mAll.find()) {
            indAddr.add(new int[]{mAll.start(), mAll.end(), 4});
        }
        int n = indAddr.size();
        if (n == 0) {
            return new int[1];
        }
        type = merge(indAddr, n, sentence);
        int[] re = new int[((2 * type) + 1)];
        re[0] = type;
        for (n = 0; n < type; n++) {
            re[(2 * n) + 1] = ((int[]) indAddr.get(n))[0];
            re[(2 * n) + 2] = ((int[]) indAddr.get(n))[1] - 1;
        }
        return re;
    }

    private static int merge(List<int[]> indx, int len, String source) {
        Collections.sort(indx, new Comparator<int[]>() {
            public int compare(int[] pair1, int[] pair2) {
                if (pair1[0] < pair2[0]) {
                    return -1;
                }
                if (pair1[0] == pair2[0]) {
                    return 0;
                }
                return 1;
            }
        });
        len--;
        int i = 0;
        while (i < len) {
            if (((int[]) indx.get(i))[1] >= ((int[]) indx.get(i + 1))[0]) {
                if (((int[]) indx.get(i))[1] < ((int[]) indx.get(i + 1))[1]) {
                    ((int[]) indx.get(i))[1] = ((int[]) indx.get(i + 1))[1];
                }
                indx.remove(i + 1);
                len--;
                i--;
            } else {
                Matcher m = matchers(source.substring(((int[]) indx.get(i))[1], ((int[]) indx.get(i + 1))[0]), "pCat");
                int t1 = ((int[]) indx.get(i))[2];
                int t2 = ((int[]) indx.get(i + 1))[2];
                if (m.matches() && (t1 & t2) == 0) {
                    ((int[]) indx.get(i))[1] = ((int[]) indx.get(i + 1))[1];
                    indx.remove(i + 1);
                    len--;
                    i--;
                }
            }
            i++;
        }
        return len + 1;
    }

    private static Matcher matchers(String t, String reg) {
        return PatternCache.getPattern(reg, "com.huawei.g11n.tmr.address.pt.ReguExp").matcher(t);
    }
}
