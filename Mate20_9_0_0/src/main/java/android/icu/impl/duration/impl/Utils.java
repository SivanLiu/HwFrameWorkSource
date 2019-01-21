package android.icu.impl.duration.impl;

import android.icu.impl.locale.BaseLocale;
import java.util.Locale;

public class Utils {

    public static class ChineseDigits {
        public static final ChineseDigits DEBUG = new ChineseDigits("0123456789s", "sbq", "WYZ", 'L', false);
        public static final ChineseDigits KOREAN = new ChineseDigits("영일이삼사오육칠팔구십", "십백천", "만억?", 51060, true);
        public static final ChineseDigits SIMPLIFIED = new ChineseDigits("零一二三四五六七八九十", "十百千", "万亿兆", 20004, false);
        public static final ChineseDigits TRADITIONAL = new ChineseDigits("零一二三四五六七八九十", "十百千", "萬億兆", 20841, false);
        final char[] digits;
        final boolean ko;
        final char[] levels;
        final char liang;
        final char[] units;

        ChineseDigits(String digits, String units, String levels, char liang, boolean ko) {
            this.digits = digits.toCharArray();
            this.units = units.toCharArray();
            this.levels = levels.toCharArray();
            this.liang = liang;
            this.ko = ko;
        }
    }

    public static final Locale localeFromString(String s) {
        String language = s;
        String region = "";
        String variant = "";
        int x = language.indexOf(BaseLocale.SEP);
        if (x != -1) {
            region = language.substring(x + 1);
            language = language.substring(0, x);
        }
        x = region.indexOf(BaseLocale.SEP);
        if (x != -1) {
            variant = region.substring(x + 1);
            region = region.substring(0, x);
        }
        return new Locale(language, region, variant);
    }

    public static String chineseNumber(long n, ChineseDigits zh) {
        long n2 = n;
        ChineseDigits chineseDigits = zh;
        if (n2 < 0) {
            n2 = -n2;
        }
        if (n2 > 10) {
            char[] buf = new char[40];
            char[] digits = String.valueOf(n2).toCharArray();
            int x = buf.length;
            int i = digits.length;
            int u = -1;
            int i2 = -1;
            boolean forcedZero = false;
            boolean inZero = true;
            int l = -1;
            while (true) {
                i += i2;
                boolean z = true;
                if (i < 0) {
                    break;
                }
                if (u == i2) {
                    if (l != i2) {
                        x--;
                        buf[x] = chineseDigits.levels[l];
                        inZero = true;
                        forcedZero = false;
                    }
                    u++;
                } else {
                    x--;
                    int u2 = u + 1;
                    buf[x] = chineseDigits.units[u];
                    if (u2 == 3) {
                        u = -1;
                        l++;
                    } else {
                        u = u2;
                    }
                }
                i2 = digits[i] - 48;
                if (i2 == 0) {
                    if (x < buf.length - 1 && u != 0) {
                        buf[x] = '*';
                    }
                    if (inZero || forcedZero) {
                        x--;
                        buf[x] = '*';
                    } else {
                        x--;
                        buf[x] = chineseDigits.digits[0];
                        inZero = true;
                        if (u != 1) {
                            z = false;
                        }
                        forcedZero = z;
                    }
                } else {
                    inZero = false;
                    x--;
                    buf[x] = chineseDigits.digits[i2];
                }
                i2 = -1;
            }
            if (n2 > 1000000) {
                boolean last = true;
                i = buf.length - 3;
                while (buf[i] != '0') {
                    i -= 8;
                    last = !last;
                    if (i <= x) {
                        break;
                    }
                }
                u = buf.length - 7;
                do {
                    if (buf[u] == chineseDigits.digits[0] && !last) {
                        buf[u] = '*';
                    }
                    u -= 8;
                    last = !last;
                } while (u > x);
                if (n2 >= 100000000) {
                    i = buf.length - 8;
                    do {
                        boolean empty = true;
                        int e = Math.max(x - 1, i - 8);
                        for (i2 = i - 1; i2 > e; i2--) {
                            if (buf[i2] != '*') {
                                empty = false;
                                break;
                            }
                        }
                        if (empty) {
                            if (buf[i + 1] == '*' || buf[i + 1] == chineseDigits.digits[0]) {
                                buf[i] = '*';
                            } else {
                                buf[i] = chineseDigits.digits[0];
                            }
                        }
                        i -= 8;
                    } while (i > x);
                }
            }
            l = x;
            while (l < buf.length) {
                if (buf[l] == chineseDigits.digits[2] && ((l >= buf.length - 1 || buf[l + 1] != chineseDigits.units[0]) && (l <= x || !(buf[l - 1] == chineseDigits.units[0] || buf[l - 1] == chineseDigits.digits[0] || buf[l - 1] == '*')))) {
                    buf[l] = chineseDigits.liang;
                }
                l++;
            }
            if (buf[x] == chineseDigits.digits[1] && (chineseDigits.ko || buf[x + 1] == chineseDigits.units[0])) {
                x++;
            }
            l = x;
            i = l;
            while (l < buf.length) {
                if (buf[l] != '*') {
                    u = i + 1;
                    buf[i] = buf[l];
                    i = u;
                }
                l++;
            }
            return new String(buf, x, i - x);
        } else if (n2 == 2) {
            return String.valueOf(chineseDigits.liang);
        } else {
            return String.valueOf(chineseDigits.digits[(int) n2]);
        }
    }
}
