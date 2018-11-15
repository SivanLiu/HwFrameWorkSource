package org.apache.xml.utils.res;

public class XResources_ka extends XResourceBundle {
    public Object[][] getContents() {
        Object[][] objArr = new Object[13][];
        objArr[0] = new Object[]{"ui_language", "ka"};
        objArr[1] = new Object[]{"help_language", "ka"};
        objArr[2] = new Object[]{"language", "ka"};
        objArr[3] = new Object[]{XResourceBundle.LANG_ALPHABET, new CharArrayWrapper(new char[]{'ა', 'ბ', 'გ', 'დ', 'ე', 'ვ', 'ზ', 'ჱ', 'თ', 'ი', 'კ', 'ლ', 'მ', 'ნ', 'ჲ', 'ო', 'პ', 'ჟ', 'რ', 'ს', 'ტ', 'უ', 'ფ', 'ქ', 'ღ', 'ყ', 'შ', 'ჩ', 'ც', 'ძ', 'წ', 'ჭ', 'ხ', 'ჴ', 'ჯ', 'ჰ'})};
        objArr[4] = new Object[]{XResourceBundle.LANG_TRAD_ALPHABET, new CharArrayWrapper(new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'})};
        objArr[5] = new Object[]{XResourceBundle.LANG_ORIENTATION, "LeftToRight"};
        objArr[6] = new Object[]{XResourceBundle.LANG_NUMBERING, XResourceBundle.LANG_ADDITIVE};
        objArr[7] = new Object[]{XResourceBundle.LANG_NUMBERGROUPS, new IntArrayWrapper(new int[]{1000, 100, 10, 1})};
        objArr[8] = new Object[]{"digits", new CharArrayWrapper(new char[]{'ა', 'ბ', 'გ', 'დ', 'ე', 'ვ', 'ზ', 'ჱ', 'თ'})};
        objArr[9] = new Object[]{"tens", new CharArrayWrapper(new char[]{'ი', 'კ', 'ლ', 'მ', 'ნ', 'ჲ', 'ო', 'პ', 'ჟ'})};
        objArr[10] = new Object[]{"hundreds", new CharArrayWrapper(new char[]{'რ', 'ს', 'ტ', 'უ', 'ფ', 'ქ', 'ღ', 'ყ', 'შ'})};
        objArr[11] = new Object[]{"thousands", new CharArrayWrapper(new char[]{'ჩ', 'ც', 'ძ', 'წ', 'ჭ', 'ხ', 'ჴ', 'ჯ', 'ჰ'})};
        Object[] objArr2 = new Object[2];
        objArr2[0] = XResourceBundle.LANG_NUM_TABLES;
        objArr2[1] = new StringArrayWrapper(new String[]{"thousands", "hundreds", "tens", "digits"});
        objArr[12] = objArr2;
        return objArr;
    }
}
