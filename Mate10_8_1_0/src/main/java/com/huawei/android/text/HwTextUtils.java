package com.huawei.android.text;

import android.text.SpannableString;
import android.text.SpannedString;
import huawei.android.provider.HwSettings.System;
import java.util.HashMap;

public class HwTextUtils {
    private static HashMap<Character, CharSequence> SyrillicLatinMap = SyrillicToLatin();

    private static boolean isSyrillic(String chs, int len) {
        for (int i = 0; i < len; i++) {
            char c = chs.charAt(i);
            if (c > 'Ѐ' && c < 'Ѡ') {
                return true;
            }
        }
        return false;
    }

    private static StringBuilder getLatinString(String chs, int len) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = chs.charAt(i);
            if (c <= 'Ѐ' || c >= 'Ѡ') {
                out.append(c);
            } else {
                out.append((CharSequence) SyrillicLatinMap.get(Character.valueOf(c)));
            }
        }
        return out;
    }

    private static int getLatinStringLen(CharSequence chs, int len) {
        return getLatinString(chs.toString(), len).length();
    }

    public static String serbianSyrillic2Latin(String text) {
        if (text == null) {
            return null;
        }
        int len = text.length();
        if (isSyrillic(text, len)) {
            return getLatinString(text, len).toString();
        }
        return text;
    }

    public static CharSequence serbianSyrillic2Latin(CharSequence text) {
        if (text == null) {
            return null;
        }
        if (text instanceof String) {
            return serbianSyrillic2Latin((String) text);
        }
        if (!(text instanceof SpannedString)) {
            return text;
        }
        int len = text.length();
        if (!isSyrillic(text.toString(), len)) {
            return text;
        }
        SpannableString newText = new SpannableString(getLatinString(text.toString(), len));
        SpannedString sp = (SpannedString) text;
        int end = sp.length();
        Object[] spans = sp.getSpans(0, end, Object.class);
        for (int i = 0; i < spans.length; i++) {
            int st = sp.getSpanStart(spans[i]);
            int en = sp.getSpanEnd(spans[i]);
            int fl = sp.getSpanFlags(spans[i]);
            if (st < 0) {
                st = 0;
            }
            if (en > end) {
                en = end;
            }
            newText.setSpan(spans[i], getLatinStringLen(text.subSequence(0, st), st), getLatinStringLen(text.subSequence(0, en), en), fl);
        }
        return newText;
    }

    private static HashMap<Character, CharSequence> SyrillicToLatin() {
        HashMap<Character, CharSequence> map = new HashMap();
        map.put(Character.valueOf('Ё'), "Ё");
        map.put(Character.valueOf('Ђ'), "Đ");
        map.put(Character.valueOf('Ѓ'), "Ѓ");
        map.put(Character.valueOf('Є'), "Є");
        map.put(Character.valueOf('Ѕ'), "Ѕ");
        map.put(Character.valueOf('І'), "І");
        map.put(Character.valueOf('Ї'), "Ї");
        map.put(Character.valueOf('Ј'), "J");
        map.put(Character.valueOf('Љ'), "Lj");
        map.put(Character.valueOf('Њ'), "Nj");
        map.put(Character.valueOf('Ћ'), "Ć");
        map.put(Character.valueOf('Ќ'), "Ќ");
        map.put(Character.valueOf('Ѝ'), "Ѝ");
        map.put(Character.valueOf('Ў'), "Ў");
        map.put(Character.valueOf('Џ'), "Dž");
        map.put(Character.valueOf('А'), "A");
        map.put(Character.valueOf('Б'), "B");
        map.put(Character.valueOf('В'), "V");
        map.put(Character.valueOf('Г'), "G");
        map.put(Character.valueOf('Д'), "D");
        map.put(Character.valueOf('Е'), "E");
        map.put(Character.valueOf('Ж'), "Ž");
        map.put(Character.valueOf('З'), "Z");
        map.put(Character.valueOf('И'), "I");
        map.put(Character.valueOf('Й'), "Й");
        map.put(Character.valueOf('К'), "K");
        map.put(Character.valueOf('Л'), "L");
        map.put(Character.valueOf('М'), "M");
        map.put(Character.valueOf('Н'), "N");
        map.put(Character.valueOf('О'), "O");
        map.put(Character.valueOf('П'), "P");
        map.put(Character.valueOf('Р'), "R");
        map.put(Character.valueOf('С'), "S");
        map.put(Character.valueOf('Т'), "T");
        map.put(Character.valueOf('У'), "U");
        map.put(Character.valueOf('Ф'), "F");
        map.put(Character.valueOf('Х'), "H");
        map.put(Character.valueOf('Ц'), "C");
        map.put(Character.valueOf('Ч'), "Č");
        map.put(Character.valueOf('Ш'), "Š");
        map.put(Character.valueOf('Щ'), "Щ");
        map.put(Character.valueOf('Ъ'), "Ъ");
        map.put(Character.valueOf('Ы'), "Ы");
        map.put(Character.valueOf('Ь'), "Ь");
        map.put(Character.valueOf('Э'), "Э");
        map.put(Character.valueOf('Ю'), "Ю");
        map.put(Character.valueOf('Я'), "Я");
        map.put(Character.valueOf('а'), "a");
        map.put(Character.valueOf('б'), "b");
        map.put(Character.valueOf('в'), System.FINGERSENSE_KNUCKLE_GESTURE_V_SUFFIX);
        map.put(Character.valueOf('г'), "g");
        map.put(Character.valueOf('д'), "d");
        map.put(Character.valueOf('е'), System.FINGERSENSE_KNUCKLE_GESTURE_E_SUFFIX);
        map.put(Character.valueOf('ж'), "ž");
        map.put(Character.valueOf('з'), System.FINGERSENSE_KNUCKLE_GESTURE_Z_SUFFIX);
        map.put(Character.valueOf('и'), "i");
        map.put(Character.valueOf('й'), "й");
        map.put(Character.valueOf('к'), "k");
        map.put(Character.valueOf('л'), "l");
        map.put(Character.valueOf('м'), System.FINGERSENSE_KNUCKLE_GESTURE_M_SUFFIX);
        map.put(Character.valueOf('н'), "n");
        map.put(Character.valueOf('о'), "o");
        map.put(Character.valueOf('п'), "p");
        map.put(Character.valueOf('р'), "r");
        map.put(Character.valueOf('с'), System.FINGERSENSE_KNUCKLE_GESTURE_S_SUFFIX);
        map.put(Character.valueOf('т'), "t");
        map.put(Character.valueOf('у'), "u");
        map.put(Character.valueOf('ф'), "f");
        map.put(Character.valueOf('х'), "h");
        map.put(Character.valueOf('ц'), System.FINGERSENSE_KNUCKLE_GESTURE_C_SUFFIX);
        map.put(Character.valueOf('ч'), "č");
        map.put(Character.valueOf('ш'), "š");
        map.put(Character.valueOf('щ'), "щ");
        map.put(Character.valueOf('ъ'), "ъ");
        map.put(Character.valueOf('ы'), "ы");
        map.put(Character.valueOf('ь'), "ь");
        map.put(Character.valueOf('э'), "э");
        map.put(Character.valueOf('ю'), "ю");
        map.put(Character.valueOf('я'), "я");
        map.put(Character.valueOf('ѐ'), "ѐ");
        map.put(Character.valueOf('ё'), "ё");
        map.put(Character.valueOf('ђ'), "đ");
        map.put(Character.valueOf('ѓ'), "ѓ");
        map.put(Character.valueOf('є'), "є");
        map.put(Character.valueOf('ѕ'), "ѕ");
        map.put(Character.valueOf('і'), "і");
        map.put(Character.valueOf('ї'), "ї");
        map.put(Character.valueOf('ј'), "j");
        map.put(Character.valueOf('љ'), "lj");
        map.put(Character.valueOf('њ'), "nj");
        map.put(Character.valueOf('ћ'), "ć");
        map.put(Character.valueOf('ќ'), "ќ");
        map.put(Character.valueOf('ѝ'), "ѝ");
        map.put(Character.valueOf('ў'), "ў");
        map.put(Character.valueOf('џ'), "dž");
        return map;
    }
}
