package com.huawei.g11n.tmr.address.it;

import com.huawei.g11n.tmr.util.PatternCache;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

public class SerEngine {
    private static HashSet<String> blackDictionary = new HashSet<String>() {
        private static final long serialVersionUID = 2852005425220592267L;

        {
            add("Piazzapulita ");
            add("Tokio Hotel ");
            add("Piazza Università ");
            add("Ristorante Cinese ");
            add("Giardino Inglese ");
            add("Ristorante Italiano ");
            add("Facoltà di Lettere ");
            add("Strada Grande Comunicazione ");
        }
    };

    public static int[] search(String sentence) {
        int i;
        boolean matcherregione;
        int i2;
        int i3;
        String temp;
        int nED;
        Matcher matcherPT;
        int m2;
        Matcher matchercountry;
        int m3;
        int n3;
        Matcher matcherPT2;
        int n1;
        String str = sentence;
        List<int[]> addrIndxTemp = new LinkedList();
        int senlen = sentence.length();
        Matcher matcher = matchers(str, "patternEDnopre");
        String ss = "";
        while (true) {
            i = 4;
            matcherregione = false;
            i2 = 2;
            i3 = 1;
            if (!matcher.find()) {
                break;
            }
            Matcher matcher2;
            ss = matcher.group(4);
            if (blackDictionary.contains(matcher.group()) || deleteBuiBlack(ss).booleanValue()) {
                matcher2 = matcher;
            } else {
                addrIndxTemp.add(new int[]{matcher.start(), matcher.end()});
                temp = str.substring(matcher.end(), senlen);
                nED = matcher.end();
                matcherPT = matchers(temp, "patternPCcity");
                if (matcherPT.lookingAt()) {
                    i3 = matcherPT.start() + nED;
                    int n12 = matcherPT.end() + nED;
                    addrIndxTemp.add(new int[]{i3, n12});
                    String temp1 = str.substring(n12, senlen);
                    Matcher matcherregione2 = matchers(temp1, "patternregion");
                    if (matcherregione2.lookingAt()) {
                        m2 = matcherregione2.start() + n12;
                        int n2 = matcherregione2.end() + n12;
                        matcher2 = matcher;
                        addrIndxTemp.add(new int[]{m2, n2});
                        matchercountry = matchers(str.substring(n2, senlen), "patterncountry");
                        if (matchercountry.lookingAt()) {
                            m3 = matchercountry.start() + n2;
                            n3 = matchercountry.end() + n2;
                            addrIndxTemp.add(new int[]{m3, n3});
                        }
                    } else {
                        matcher2 = matcher;
                        Matcher matchercountry2 = matchers(temp1, "patterncountry");
                        if (matchercountry2.lookingAt() != null) {
                            matcher = matchercountry2.start() + n12;
                            m2 = matchercountry2.end() + n12;
                            addrIndxTemp.add(new int[]{matcher, m2});
                        }
                    }
                } else {
                    matcher2 = matcher;
                }
            }
            matcher = matcher2;
            str = sentence;
        }
        Matcher matcherjie = matchers(str, "patternED");
        while (matcherjie.find()) {
            String str2;
            Matcher matcher3;
            ss = matcherjie.group(i);
            if (blackDictionary.contains(matcherjie.group()) || deleteBuiBlack(ss).booleanValue()) {
                str2 = ss;
                matcher3 = matcherjie;
            } else {
                addrIndxTemp.add(new int[]{matcherjie.start(), matcherjie.end()});
                String temp2 = str.substring(matcherjie.end(), senlen);
                i3 = matcherjie.end();
                matcherPT2 = matchers(temp2, "patternPCcity");
                if (matcherPT2.lookingAt()) {
                    int m1 = matcherPT2.start() + i3;
                    n1 = matcherPT2.end() + i3;
                    addrIndxTemp.add(new int[]{m1, n1});
                    String temp12 = str.substring(n1, senlen);
                    Matcher matcherregione3 = matchers(temp12, "patternregion");
                    if (matcherregione3.lookingAt()) {
                        int m22 = matcherregione3.start() + n1;
                        i = matcherregione3.end() + n1;
                        str2 = ss;
                        matcher3 = matcherjie;
                        addrIndxTemp.add(new int[]{m22, i});
                        ss = str.substring(i, senlen);
                        matcherjie = matchers(ss, "patterncountry");
                        if (matcherjie.lookingAt()) {
                            m3 = matcherjie.start() + i;
                            n3 = matcherjie.end() + i;
                            String temp22 = ss;
                            addrIndxTemp.add(new int[]{m3, n3});
                        }
                    } else {
                        str2 = ss;
                        matcher3 = matcherjie;
                        ss = matchers(temp12, "patterncountry");
                        if (ss.lookingAt()) {
                            matcherjie = ss.start() + n1;
                            i = ss.end() + n1;
                            String matchercountry3 = ss;
                            addrIndxTemp.add(new int[]{matcherjie, i});
                        }
                    }
                } else {
                    str2 = ss;
                    matcher3 = matcherjie;
                }
            }
            ss = str2;
            matcherjie = matcher3;
            i = 4;
            matcherregione = false;
            i2 = 2;
            i3 = 1;
        }
        matcherPT2 = matchers(str, "patternST");
        while (matcherPT2.find()) {
            temp = matcherPT2.group(3);
            Boolean valueOf = Boolean.valueOf(matcherregione);
            valueOf = deleteStrBlack(temp);
            n1 = matcherPT2.start();
            m2 = matcherPT2.end();
            if (!valueOf.booleanValue()) {
                int[] iArr = new int[i2];
                iArr[matcherregione] = n1;
                iArr[i3] = m2;
                addrIndxTemp.add(iArr);
            }
            matchercountry = matchers(str.substring(m2, senlen), "patternPCcity");
            if (matchercountry.lookingAt()) {
                i3 = matchercountry.end() + m2;
                String ss2 = ss;
                ss = new int[i2];
                ss[matcherregione] = matchercountry.start() + m2;
                ss[1] = i3;
                addrIndxTemp.add(ss);
                ss = str.substring(i3, senlen);
                Matcher matcherregione4 = matchers(ss, "patternregion");
                if (matcherregione4.lookingAt()) {
                    n3 = matcherregione4.start() + i3;
                    temp = matcherregione4.end() + i3;
                    int[] iArr2 = new int[i2];
                    iArr2[0] = n3;
                    iArr2[1] = temp;
                    addrIndxTemp.add(iArr2);
                    String temp3 = str.substring(temp, senlen);
                    matcherPT = matchers(temp3, "patterncountry");
                    if (matcherPT.lookingAt()) {
                        int m32 = matcherPT.start() + temp;
                        int n32 = matcherPT.end() + temp;
                        int n22 = temp;
                        addrIndxTemp.add(new int[]{m32, n32});
                    }
                } else {
                    String str3 = temp;
                    Matcher matcher4 = matcherregione4;
                    Matcher matchercountry4 = matchers(ss, "patterncountry");
                    if (matchercountry4.lookingAt()) {
                        nED = matchercountry4.start() + i3;
                        i2 = matchercountry4.end() + i3;
                        String temp23 = ss;
                        addrIndxTemp.add(new int[]{nED, i2});
                    }
                }
                ss = ss2;
                matcherregione = false;
                i2 = 2;
                i3 = 1;
            }
        }
        i = addrIndxTemp.size();
        if (i == 0) {
            return new int[i3];
        }
        i = merge(addrIndxTemp, i);
        int[] addrIndx = new int[((i2 * i) + i3)];
        addrIndx[matcherregione] = i;
        for (n1 = 0; n1 < i; n1++) {
            addrIndx[(i2 * n1) + i3] = ((int[]) addrIndxTemp.get(n1))[matcherregione];
            addrIndx[(i2 * n1) + i2] = ((int[]) addrIndxTemp.get(n1))[i3] - i3;
        }
        return addrIndx;
    }

    private static int merge(List<int[]> indx, int len) {
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
            }
            i++;
        }
        return len + 1;
    }

    private static Boolean deleteBuiBlack(String addressB) {
        if (addressB == null) {
            return Boolean.valueOf(false);
        }
        if (matchers(addressB, "patternbb").matches()) {
            return Boolean.valueOf(true);
        }
        return Boolean.valueOf(false);
    }

    private static Boolean deleteStrBlack(String addressS) {
        if (addressS == null) {
            return Boolean.valueOf(false);
        }
        if (matchers(addressS, "patternbs").matches()) {
            return Boolean.valueOf(true);
        }
        return Boolean.valueOf(false);
    }

    private static Matcher matchers(String t, String reg) {
        return PatternCache.getPattern(reg, "com.huawei.g11n.tmr.address.it.ReguExp").matcher(t);
    }
}
