package org.apache.xml.utils.res;

public class XResources_he extends XResourceBundle {
    public Object[][] getContents() {
        r0 = new Object[11][];
        r0[0] = new Object[]{"ui_language", "he"};
        r0[1] = new Object[]{"help_language", "he"};
        r0[2] = new Object[]{"language", "he"};
        r0[3] = new Object[]{XResourceBundle.LANG_ALPHABET, new CharArrayWrapper(new char[]{'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י', 'ך', 'כ', 'ל', 'ם', 'מ', 'ן', 'נ', 'ס'})};
        r0[4] = new Object[]{XResourceBundle.LANG_TRAD_ALPHABET, new CharArrayWrapper(new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'})};
        r0[5] = new Object[]{XResourceBundle.LANG_ORIENTATION, "RightToLeft"};
        r0[6] = new Object[]{XResourceBundle.LANG_NUMBERING, XResourceBundle.LANG_ADDITIVE};
        r0[7] = new Object[]{XResourceBundle.LANG_NUMBERGROUPS, new IntArrayWrapper(new int[]{10, 1})};
        r0[8] = new Object[]{"digits", new CharArrayWrapper(new char[]{'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט'})};
        r0[9] = new Object[]{"tens", new CharArrayWrapper(new char[]{'י', 'ך', 'כ', 'ל', 'ם', 'מ', 'ן', 'נ', 'ס'})};
        Object[] objArr = new Object[2];
        objArr[0] = XResourceBundle.LANG_NUM_TABLES;
        objArr[1] = new StringArrayWrapper(new String[]{"tens", "digits"});
        r0[10] = objArr;
        return r0;
    }
}
