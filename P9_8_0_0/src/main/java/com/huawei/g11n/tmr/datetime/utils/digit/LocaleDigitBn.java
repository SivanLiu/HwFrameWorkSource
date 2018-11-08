package com.huawei.g11n.tmr.datetime.utils.digit;

import java.util.HashMap;
import java.util.Map.Entry;

public class LocaleDigitBn extends LocaleDigit {
    public LocaleDigitBn() {
        this.pattern = "[০১২৩৪৫৬৭৮৯]+";
    }

    public String convert(String inStr) {
        HashMap<Character, Integer> numMap = new HashMap();
        numMap.put(Character.valueOf('১'), Integer.valueOf(1));
        numMap.put(Character.valueOf('২'), Integer.valueOf(2));
        numMap.put(Character.valueOf('৩'), Integer.valueOf(3));
        numMap.put(Character.valueOf('৪'), Integer.valueOf(4));
        numMap.put(Character.valueOf('৫'), Integer.valueOf(5));
        numMap.put(Character.valueOf('৬'), Integer.valueOf(6));
        numMap.put(Character.valueOf('৭'), Integer.valueOf(7));
        numMap.put(Character.valueOf('৮'), Integer.valueOf(8));
        numMap.put(Character.valueOf('৯'), Integer.valueOf(9));
        numMap.put(Character.valueOf('০'), Integer.valueOf(0));
        for (Entry<Character, Integer> entry : numMap.entrySet()) {
            inStr = inStr.replaceAll(((Character) entry.getKey()).toString(), ((Integer) entry.getValue()).toString());
        }
        return inStr;
    }
}
