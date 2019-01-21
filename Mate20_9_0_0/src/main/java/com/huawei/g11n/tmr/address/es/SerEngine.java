package com.huawei.g11n.tmr.address.es;

import com.huawei.g11n.tmr.util.PatternCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerEngine {
    public static int[] search(String sentence) {
        int i;
        boolean hasPrep;
        int start;
        int end;
        String str = sentence;
        boolean hasPrep2 = false;
        int end2 = 2;
        int[] otherIndex = new int[2];
        List<int[]> listOther = new ArrayList();
        String temp = null;
        List<int[]> indAddr = new LinkedList();
        Matcher mAll = matchers(str, "p1");
        HashSet<String> blackDictionary = new HashSet<String>() {
            private static final long serialVersionUID = -2265126958429208163L;

            {
                add("cine Mexicano");
                add("Parque Nacional");
                add("esto");
                add("aquello");
                add("mío");
                add("tuyo");
                add("estos");
                add("bonito");
                add("grande");
                add("pequeo");
                add("bueno");
                add("puente de suspensión");
                add("Puente Colgante");
                add("puente");
                add("puente de acero");
                add("puente de arco");
                add("calle de dirección única");
                add("calle de dos carriles");
                add("restaurante antigua");
                add("taberna");
                add("institución");
                add("banco");
                add("club");
                add("tribunal");
                add("mercado de Navidad");
                add("mercado de pulgas");
                add("Mercado Libre");
                add("TEATRO MIS AMORES");
                add("gimnasio EN VERANO");
                add("Puente Genil");
                add("CALLE SEGUIREMOS");
                add("HOTEL OXIDAO");
                add("colegio JAJAJAAAJAJAJAJJAAJAJJAJAJA");
            }
        };
        while (true) {
            i = 1;
            if (!mAll.find()) {
                break;
            }
            temp = mAll.group();
            if (blackDictionary.contains(temp)) {
                break;
            }
            hasPrep = hasPrep2;
            start = mAll.start();
            int end3 = mAll.group().length() + start;
            Matcher cAll = matchers(str.substring(end3), "p2");
            if (cAll.lookingAt()) {
                end3 += cAll.end();
            }
            indAddr.add(new int[]{1, start, end3});
            otherIndex[0] = start;
            otherIndex[1] = end3;
            listOther.add(otherIndex);
            cAll = end3;
            end2 = 2;
            hasPrep2 = hasPrep;
        }
        temp = matchers(str, "p3");
        while (temp.find()) {
            end2 = temp.group().length() + temp.start();
            mAll = matchers(str.substring(end2), "p2");
            if (mAll.lookingAt()) {
                end2 += mAll.end();
            }
            hasPrep = hasPrep2;
            indAddr.add(new int[]{2, start, end2});
            hasPrep2 = hasPrep;
            end2 = 2;
            i = 1;
        }
        mAll = matchers(str, "p5");
        boolean hasOther = false;
        while (mAll.find()) {
            int start2 = mAll.start();
            end = mAll.group().length() + start2;
            temp = matchers(str.substring(null, start2), "pgrep");
            if (temp.lookingAt()) {
                hasPrep2 = Pattern.compile("\\p{Blank}{1,6}").matcher(str.substring(temp.end(), start2)).matches();
            }
            boolean hasPrep3 = hasPrep2;
            for (int[] eachOther : listOther) {
                boolean hasOther2;
                if (eachOther[0] <= end || eachOther[0] - end >= 10) {
                    if (eachOther[1] < start2 && start2 - eachOther[1] < 10 && matchers(str.substring(eachOther[1], start2), "pPrepAndSc").matches()) {
                        hasOther2 = true;
                    }
                } else if (matchers(str.substring(end, eachOther[0]), "pPrepAndSc").matches()) {
                    hasOther2 = true;
                }
                hasOther = hasOther2;
            }
            if (hasPrep3 || hasOther) {
                Matcher cAll2 = matchers(str.substring(end), "p2");
                if (cAll2.lookingAt()) {
                    end += cAll2.end();
                }
                indAddr.add(new int[]{2, start2, end});
            }
            hasPrep2 = hasPrep3;
            start2 = 2;
            end2 = 2;
            i = 1;
        }
        end = indAddr.size();
        if (end == 0) {
            return new int[i];
        }
        end = merge(indAddr, end, str);
        temp = new int[((end2 * end) + i)];
        temp[0] = end;
        for (int n = 0; n < end; n++) {
            temp[(end2 * n) + 1] = ((int[]) indAddr.get(n))[i];
            temp[(end2 * n) + 2] = ((int[]) indAddr.get(n))[end2] - 1;
        }
        return temp;
    }

    private static int merge(List<int[]> indx, int len, String sentence) {
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

    private static Matcher matchers(String t, String reg) {
        return PatternCache.getPattern(reg, "com.huawei.g11n.tmr.address.es.ReguExp").matcher(t);
    }
}
