package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.UCharacterName;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator.Factory;
import android.icu.text.Transliterator.Position;

class NameUnicodeTransliterator extends Transliterator {
    static final char CLOSE_DELIM = '}';
    static final char OPEN_DELIM = '\\';
    static final String OPEN_PAT = "\\N~{~";
    static final char SPACE = ' ';
    static final String _ID = "Name-Any";

    static void register() {
        Transliterator.registerFactory(_ID, new Factory() {
            public Transliterator getInstance(String ID) {
                return new NameUnicodeTransliterator(null);
            }
        });
    }

    public NameUnicodeTransliterator(UnicodeFilter filter) {
        super(_ID, filter);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void handleTransliterate(Replaceable text, Position offsets, boolean isIncremental) {
        int c;
        Replaceable replaceable = text;
        Position position = offsets;
        int maxLen = UCharacterName.INSTANCE.getMaxCharNameLength() + 1;
        StringBuffer name = new StringBuffer(maxLen);
        UnicodeSet legal = new UnicodeSet();
        UCharacterName.INSTANCE.getCharNameCharacters(legal);
        int cursor = position.start;
        int mode = 0;
        int limit = position.limit;
        int cursor2 = cursor;
        cursor = -1;
        while (cursor2 < limit) {
            c = replaceable.char32At(cursor2);
            int i;
            switch (mode) {
                case 0:
                    if (c == 92) {
                        cursor = cursor2;
                        i = Utility.parsePattern(OPEN_PAT, replaceable, cursor2, limit);
                        if (i >= 0 && i < limit) {
                            mode = 1;
                            name.setLength(0);
                            cursor2 = i;
                            break;
                        }
                    }
                case 1:
                    if (PatternProps.isWhiteSpace(c)) {
                        if (name.length() > 0 && name.charAt(name.length() - 1) != SPACE) {
                            name.append(SPACE);
                            if (name.length() > maxLen) {
                                mode = 0;
                            }
                        }
                    } else if (c == 125) {
                        i = name.length();
                        if (i > 0 && name.charAt(i - 1) == SPACE) {
                            name.setLength(i - 1);
                        }
                        c = UCharacter.getCharFromExtendedName(name.toString());
                        if (c != -1) {
                            cursor2++;
                            String str = UTF16.valueOf(c);
                            replaceable.replace(cursor, cursor2, str);
                            int delta = (cursor2 - cursor) - str.length();
                            cursor2 -= delta;
                            limit -= delta;
                        }
                        mode = 0;
                        cursor = -1;
                        break;
                    } else if (legal.contains(c)) {
                        UTF16.append(name, c);
                        if (name.length() >= maxLen) {
                            mode = 0;
                        }
                    } else {
                        cursor2--;
                        mode = 0;
                    }
                default:
                    cursor2 += UTF16.getCharCount(c);
                    break;
            }
        }
        position.contextLimit += limit - position.limit;
        position.limit = limit;
        c = (!isIncremental || cursor < 0) ? cursor2 : cursor;
        position.start = c;
    }

    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet myFilter = getFilterAsUnicodeSet(inputFilter);
        if (myFilter.containsAll("\\N{") && myFilter.contains(125)) {
            UnicodeSet items = new UnicodeSet().addAll(48, 57).addAll(65, 70).addAll(97, 122).add(60).add(62).add(40).add(41).add(45).add(32).addAll((CharSequence) "\\N{").add(125);
            items.retainAll(myFilter);
            if (items.size() > 0) {
                sourceSet.addAll(items);
                targetSet.addAll(0, 1114111);
            }
        }
    }
}
