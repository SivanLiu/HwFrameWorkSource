package com.huawei.g11n.tmr.datetime.utils.digit;

import java.util.HashMap;
import java.util.Map.Entry;

public class LocaleDigitFa extends LocaleDigit {
    public LocaleDigitFa() {
        this.pattern = "[۰۱۲۳۴۵۶۷۸۹]+";
    }

    public String convert(String inStr) {
        HashMap<Character, Integer> numMap = new HashMap();
        numMap.put(Character.valueOf('۱'), Integer.valueOf(1));
        numMap.put(Character.valueOf('۲'), Integer.valueOf(2));
        numMap.put(Character.valueOf('۳'), Integer.valueOf(3));
        numMap.put(Character.valueOf('۴'), Integer.valueOf(4));
        numMap.put(Character.valueOf('۵'), Integer.valueOf(5));
        numMap.put(Character.valueOf('۶'), Integer.valueOf(6));
        numMap.put(Character.valueOf('۷'), Integer.valueOf(7));
        numMap.put(Character.valueOf('۸'), Integer.valueOf(8));
        numMap.put(Character.valueOf('۹'), Integer.valueOf(9));
        numMap.put(Character.valueOf('۰'), Integer.valueOf(0));
        for (Entry<Character, Integer> entry : numMap.entrySet()) {
            inStr = inStr.replaceAll(((Character) entry.getKey()).toString(), ((Integer) entry.getValue()).toString());
        }
        return inStr;
    }
}
