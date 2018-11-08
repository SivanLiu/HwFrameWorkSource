package com.huawei.g11n.tmr.datetime.utils.digit;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleDigitZh extends LocaleDigit {
    public LocaleDigitZh() {
        this.pattern = "[0-9零一二三四五六七八九十两整半科钟鍾兩]+";
    }

    public String convert(String inStr) {
        inStr = inStr.replaceAll("半", "30").replaceAll("钟", "00").replaceAll("鍾", "00").replaceAll("整", "00").replaceAll("一刻", "15").replaceAll("三刻", "45");
        HashMap<Character, Integer> numMap = new HashMap();
        numMap.put(Character.valueOf('一'), Integer.valueOf(1));
        numMap.put(Character.valueOf('二'), Integer.valueOf(2));
        numMap.put(Character.valueOf('三'), Integer.valueOf(3));
        numMap.put(Character.valueOf('四'), Integer.valueOf(4));
        numMap.put(Character.valueOf('五'), Integer.valueOf(5));
        numMap.put(Character.valueOf('六'), Integer.valueOf(6));
        numMap.put(Character.valueOf('七'), Integer.valueOf(7));
        numMap.put(Character.valueOf('八'), Integer.valueOf(8));
        numMap.put(Character.valueOf('九'), Integer.valueOf(9));
        numMap.put(Character.valueOf('零'), Integer.valueOf(0));
        numMap.put(Character.valueOf('十'), Integer.valueOf(10));
        numMap.put(Character.valueOf('两'), Integer.valueOf(2));
        numMap.put(Character.valueOf('兩'), Integer.valueOf(2));
        Matcher matcher = Pattern.compile("[零一二三四五六七八九十两兩]{1,10}").matcher(inStr);
        StringBuffer strSB = new StringBuffer(inStr);
        while (matcher.find()) {
            int res = 0;
            String hanzi = matcher.group();
            switch (hanzi.length()) {
                case 1:
                    res = ((Integer) numMap.get(Character.valueOf(hanzi.charAt(0)))).intValue();
                    break;
                case 2:
                    if (hanzi.charAt(0) != '十') {
                        if (hanzi.charAt(1) == '十') {
                            if (hanzi.charAt(0) != '零') {
                                res = ((Integer) numMap.get(Character.valueOf(hanzi.charAt(0)))).intValue() * 10;
                                break;
                            }
                            res = 10;
                            break;
                        }
                        res = (((Integer) numMap.get(Character.valueOf(hanzi.charAt(0)))).intValue() * 10) + ((Integer) numMap.get(Character.valueOf(hanzi.charAt(1)))).intValue();
                        break;
                    }
                    res = ((Integer) numMap.get(Character.valueOf(hanzi.charAt(1)))).intValue() + 10;
                    break;
                case 3:
                    res = (((Integer) numMap.get(Character.valueOf(hanzi.charAt(0)))).intValue() * 10) + ((Integer) numMap.get(Character.valueOf(hanzi.charAt(2)))).intValue();
                    break;
                case 4:
                    res = (((((Integer) numMap.get(Character.valueOf(hanzi.charAt(0)))).intValue() * 1000) + (((Integer) numMap.get(Character.valueOf(hanzi.charAt(1)))).intValue() * 100)) + (((Integer) numMap.get(Character.valueOf(hanzi.charAt(2)))).intValue() * 10)) + ((Integer) numMap.get(Character.valueOf(hanzi.charAt(3)))).intValue();
                    break;
                default:
                    break;
            }
            strSB.replace(strSB.indexOf(hanzi), strSB.indexOf(hanzi) + hanzi.length(), "" + res);
        }
        return strSB.toString();
    }
}
