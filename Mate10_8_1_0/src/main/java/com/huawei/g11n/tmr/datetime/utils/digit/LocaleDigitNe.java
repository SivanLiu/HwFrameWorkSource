package com.huawei.g11n.tmr.datetime.utils.digit;

import java.util.HashMap;
import java.util.Map.Entry;

public class LocaleDigitNe extends LocaleDigit {
    public LocaleDigitNe() {
        this.pattern = "[०१२३४५६७८९]+";
    }

    public String convert(String inStr) {
        HashMap<Character, Integer> numMap = new HashMap();
        numMap.put(Character.valueOf('१'), Integer.valueOf(1));
        numMap.put(Character.valueOf('२'), Integer.valueOf(2));
        numMap.put(Character.valueOf('३'), Integer.valueOf(3));
        numMap.put(Character.valueOf('४'), Integer.valueOf(4));
        numMap.put(Character.valueOf('५'), Integer.valueOf(5));
        numMap.put(Character.valueOf('६'), Integer.valueOf(6));
        numMap.put(Character.valueOf('७'), Integer.valueOf(7));
        numMap.put(Character.valueOf('८'), Integer.valueOf(8));
        numMap.put(Character.valueOf('९'), Integer.valueOf(9));
        numMap.put(Character.valueOf('०'), Integer.valueOf(0));
        for (Entry<Character, Integer> entry : numMap.entrySet()) {
            inStr = inStr.replaceAll(((Character) entry.getKey()).toString(), ((Integer) entry.getValue()).toString());
        }
        return inStr;
    }
}
