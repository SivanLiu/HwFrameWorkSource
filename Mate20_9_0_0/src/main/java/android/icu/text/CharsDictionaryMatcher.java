package android.icu.text;

import android.icu.util.BytesTrie.Result;
import android.icu.util.CharsTrie;
import java.text.CharacterIterator;

class CharsDictionaryMatcher extends DictionaryMatcher {
    private CharSequence characters;

    public CharsDictionaryMatcher(CharSequence chars) {
        this.characters = chars;
    }

    public int matches(CharacterIterator text_, int maxLength, int[] lengths, int[] count_, int limit, int[] values) {
        UCharacterIterator text = UCharacterIterator.getInstance(text_);
        CharsTrie uct = new CharsTrie(this.characters, 0);
        int c = text.nextCodePoint();
        if (c == -1) {
            return 0;
        }
        Result result = uct.firstForCodePoint(c);
        int numChars = 1;
        c = 0;
        while (true) {
            if (!result.hasValue()) {
                if (result == Result.NO_MATCH) {
                    break;
                }
            }
            if (c < limit) {
                if (values != null) {
                    values[c] = uct.getValue();
                }
                lengths[c] = numChars;
                c++;
            }
            if (result == Result.FINAL_VALUE) {
                break;
            }
            if (numChars >= maxLength) {
                break;
            }
            int c2 = text.nextCodePoint();
            if (c2 == -1) {
                break;
            }
            numChars++;
            result = uct.nextForCodePoint(c2);
        }
        count_[0] = c;
        return numChars;
    }

    public int getType() {
        return 1;
    }
}
