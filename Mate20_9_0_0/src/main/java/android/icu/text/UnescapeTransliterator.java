package android.icu.text;

import android.icu.impl.PatternTokenizer;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator.Factory;
import android.icu.text.Transliterator.Position;
import android.icu.util.ULocale;

class UnescapeTransliterator extends Transliterator {
    private static final char END = '￿';
    private char[] spec;

    static void register() {
        Transliterator.registerFactory("Hex-Any/Unicode", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/Unicode", new char[]{2, 0, 16, 4, 6, 'U', '+', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/Java", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/Java", new char[]{2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/C", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/C", new char[]{2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 2, 0, 16, 8, 8, PatternTokenizer.BACK_SLASH, 'U', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/XML", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/XML", new char[]{3, 1, 16, 1, 6, '&', '#', ULocale.PRIVATE_USE_EXTENSION, ';', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/XML10", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/XML10", new char[]{2, 1, 10, 1, 7, '&', '#', ';', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/Perl", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/Perl", new char[]{3, 1, 16, 1, 6, PatternTokenizer.BACK_SLASH, ULocale.PRIVATE_USE_EXTENSION, '{', '}', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any", new Factory() {
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any", new char[]{2, 0, 16, 4, 6, 'U', '+', 2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 2, 0, 16, 8, 8, PatternTokenizer.BACK_SLASH, 'U', 3, 1, 16, 1, 6, '&', '#', ULocale.PRIVATE_USE_EXTENSION, ';', 2, 1, 10, 1, 7, '&', '#', ';', 3, 1, 16, 1, 6, PatternTokenizer.BACK_SLASH, ULocale.PRIVATE_USE_EXTENSION, '{', '}', 65535});
            }
        });
    }

    UnescapeTransliterator(String ID, char[] spec) {
        super(ID, null);
        this.spec = spec;
    }

    protected void handleTransliterate(Replaceable text, Position pos, boolean isIncremental) {
        Replaceable replaceable = text;
        Position position = pos;
        int start = position.start;
        int limit = position.limit;
        loop0:
        while (start < limit) {
            int ipat;
            int ipat2 = 0;
            while (this.spec[ipat2] != 65535) {
                int s;
                int ipat3 = ipat2 + 1;
                ipat2 = this.spec[ipat2];
                int ipat4 = ipat3 + 1;
                int suffixLen = this.spec[ipat3];
                int ipat5 = ipat4 + 1;
                ipat3 = this.spec[ipat4];
                int ipat6 = ipat5 + 1;
                ipat4 = this.spec[ipat5];
                ipat = ipat6 + 1;
                ipat5 = this.spec[ipat6];
                boolean match = true;
                int s2 = start;
                ipat6 = 0;
                while (ipat6 < ipat2) {
                    if (s2 < limit || ipat6 <= 0) {
                        s = s2 + 1;
                        if (replaceable.charAt(s2) != this.spec[ipat + ipat6]) {
                            match = false;
                            s2 = s;
                            break;
                        }
                        ipat6++;
                        s2 = s;
                    } else if (isIncremental) {
                        break loop0;
                    } else {
                        match = false;
                    }
                }
                if (match) {
                    int i;
                    s = 0;
                    int digitCount = 0;
                    while (s2 < limit) {
                        i = ipat6;
                        ipat6 = replaceable.char32At(s2);
                        int digit = UCharacter.digit(ipat6, ipat3);
                        if (digit < 0) {
                            break;
                        }
                        s2 += UTF16.getCharCount(ipat6);
                        s = (s * ipat3) + digit;
                        digitCount++;
                        if (digitCount == ipat5) {
                            ipat6 = s;
                            break;
                        }
                        ipat6 = i;
                    }
                    if (s2 > start && isIncremental) {
                        break loop0;
                    }
                    i = ipat6;
                    ipat6 = s;
                    match = digitCount >= ipat4;
                    if (match) {
                        digitCount = s2;
                        s2 = 0;
                        while (s2 < suffixLen) {
                            if (digitCount >= limit) {
                                if (digitCount > start && isIncremental) {
                                    break loop0;
                                }
                                match = false;
                            } else {
                                s = digitCount + 1;
                                if (replaceable.charAt(digitCount) != this.spec[(ipat + ipat2) + s2]) {
                                    match = false;
                                    digitCount = s;
                                    break;
                                }
                                s2++;
                                digitCount = s;
                            }
                        }
                        if (match) {
                            String str = UTF16.valueOf(ipat6);
                            replaceable.replace(start, digitCount, str);
                            limit -= (digitCount - start) - str.length();
                            break;
                        }
                        i = s2;
                        s2 = digitCount;
                    } else {
                        continue;
                    }
                }
                ipat2 = ipat + (ipat2 + suffixLen);
            }
            ipat = ipat2;
            if (start < limit) {
                start += UTF16.getCharCount(replaceable.char32At(start));
            }
        }
        position.contextLimit += limit - position.limit;
        position.limit = limit;
        position.start = start;
    }

    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet myFilter = getFilterAsUnicodeSet(inputFilter);
        UnicodeSet items = new UnicodeSet();
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (this.spec[i] != 65535) {
            int j;
            int end = ((this.spec[i] + i) + this.spec[i + 1]) + 5;
            int radix = this.spec[i + 2];
            for (j = 0; j < radix; j++) {
                Utility.appendNumber(buffer, j, radix, 0);
            }
            for (j = i + 5; j < end; j++) {
                items.add(this.spec[j]);
            }
            i = end;
        }
        items.addAll(buffer.toString());
        items.retainAll(myFilter);
        if (items.size() > 0) {
            sourceSet.addAll(items);
            targetSet.addAll(0, 1114111);
        }
    }
}
