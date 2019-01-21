package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.CharacterIteration;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

class CjkBreakEngine extends DictionaryBreakEngine {
    private static final UnicodeSet fHanWordSet = new UnicodeSet();
    private static final UnicodeSet fHangulWordSet = new UnicodeSet();
    private static final UnicodeSet fHiraganaWordSet = new UnicodeSet();
    private static final UnicodeSet fKatakanaWordSet = new UnicodeSet();
    private static final int kMaxKatakanaGroupLength = 20;
    private static final int kMaxKatakanaLength = 8;
    private static final int kint32max = Integer.MAX_VALUE;
    private static final int maxSnlp = 255;
    private DictionaryMatcher fDictionary;

    static {
        fHangulWordSet.applyPattern("[\\uac00-\\ud7a3]");
        fHanWordSet.applyPattern("[:Han:]");
        fKatakanaWordSet.applyPattern("[[:Katakana:]\\uff9e\\uff9f]");
        fHiraganaWordSet.applyPattern("[:Hiragana:]");
        fHangulWordSet.freeze();
        fHanWordSet.freeze();
        fKatakanaWordSet.freeze();
        fHiraganaWordSet.freeze();
    }

    public CjkBreakEngine(boolean korean) throws IOException {
        super(Integer.valueOf(1));
        this.fDictionary = null;
        this.fDictionary = DictionaryData.loadDictionaryFor("Hira");
        if (korean) {
            setCharacters(fHangulWordSet);
            return;
        }
        UnicodeSet cjSet = new UnicodeSet();
        cjSet.addAll(fHanWordSet);
        cjSet.addAll(fKatakanaWordSet);
        cjSet.addAll(fHiraganaWordSet);
        cjSet.add(65392);
        cjSet.add(12540);
        setCharacters(cjSet);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CjkBreakEngine)) {
            return false;
        }
        return this.fSet.equals(((CjkBreakEngine) obj).fSet);
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    private static int getKatakanaCost(int wordlength) {
        return wordlength > 8 ? 8192 : new int[]{8192, 984, 408, 240, 204, 252, 300, 372, 480}[wordlength];
    }

    private static boolean isKatakana(int value) {
        return (value >= 12449 && value <= 12542 && value != 12539) || (value >= 65382 && value <= 65439);
    }

    public int divideUpDictionaryRange(CharacterIterator inText, int startPos, int endPos, DequeI foundBreaks) {
        int i = startPos;
        int i2 = endPos;
        DequeI dequeI = foundBreaks;
        if (i >= i2) {
            return 0;
        }
        int index;
        int[] charPositions;
        CharacterIterator text;
        CharacterIterator text2;
        int index2;
        int inputLength;
        StringBuffer s;
        int[] values;
        int[] prev;
        int[] lengths;
        int newSnlp;
        inText.setIndex(startPos);
        int inputLength2 = i2 - i;
        int[] charPositions2 = new int[(inputLength2 + 1)];
        StringBuffer s2 = new StringBuffer("");
        inText.setIndex(startPos);
        while (inText.getIndex() < i2) {
            s2.append(inText.current());
            inText.next();
        }
        String prenormstr = s2.toString();
        boolean isNormalized = Normalizer.quickCheck(prenormstr, Normalizer.NFKC) == Normalizer.YES || Normalizer.isNormalized(prenormstr, Normalizer.NFKC, 0);
        int numChars = 0;
        if (isNormalized) {
            CharacterIterator text3 = new StringCharacterIterator(prenormstr);
            index = 0;
            charPositions2[0] = 0;
            while (index < prenormstr.length()) {
                index += Character.charCount(prenormstr.codePointAt(index));
                numChars++;
                charPositions2[numChars] = index;
            }
            charPositions = charPositions2;
            text = text3;
        } else {
            String normStr = Normalizer.normalize(prenormstr, Normalizer.NFKC);
            text2 = new StringCharacterIterator(normStr);
            charPositions2 = new int[(normStr.length() + 1)];
            Normalizer normalizer = new Normalizer(prenormstr, Normalizer.NFKC, 0);
            index2 = 0;
            charPositions2[0] = 0;
            while (index2 < normalizer.endIndex()) {
                normalizer.next();
                numChars++;
                index2 = normalizer.getIndex();
                charPositions2[numChars] = index2;
            }
            charPositions = charPositions2;
            text = text2;
        }
        int[] bestSnlp = new int[(numChars + 1)];
        bestSnlp[0] = 0;
        int i3 = 1;
        while (true) {
            index2 = Integer.MAX_VALUE;
            if (i3 > numChars) {
                break;
            }
            bestSnlp[i3] = Integer.MAX_VALUE;
            i3++;
        }
        int[] prev2 = new int[(numChars + 1)];
        for (i3 = 0; i3 <= numChars; i3++) {
            prev2[i3] = -1;
        }
        int[] values2 = new int[numChars];
        int[] lengths2 = new int[numChars];
        boolean is_prev_katakana = false;
        int i4 = 0;
        while (true) {
            int i5 = i4;
            if (i5 >= numChars) {
                break;
            }
            int[] bestSnlp2;
            text.setIndex(i5);
            if (bestSnlp[i5] == index2) {
                inputLength = inputLength2;
                s = s2;
                values = values2;
                prev = prev2;
                lengths = lengths2;
                bestSnlp2 = bestSnlp;
            } else {
                inputLength = inputLength2;
                lengths = lengths2;
                int maxSearchLength = i5 + 20 < numChars ? 20 : numChars - i5;
                int[] count_ = new int[1];
                values = values2;
                prev = prev2;
                s = s2;
                bestSnlp2 = bestSnlp;
                this.fDictionary.matches(text, maxSearchLength, lengths, count_, maxSearchLength, values);
                index = count_[0];
                text.setIndex(i5);
                if (!((index != 0 && lengths[0] == 1) || CharacterIteration.current32(text) == Integer.MAX_VALUE || fHangulWordSet.contains(CharacterIteration.current32(text)))) {
                    values[index] = 255;
                    lengths[index] = 1;
                    index++;
                }
                for (i3 = 0; i3 < index; i3++) {
                    index2 = bestSnlp2[i5] + values[i3];
                    if (index2 < bestSnlp2[lengths[i3] + i5]) {
                        bestSnlp2[lengths[i3] + i5] = index2;
                        prev[lengths[i3] + i5] = i5;
                    }
                }
                boolean is_katakana = isKatakana(CharacterIteration.current32(text));
                if (!is_prev_katakana && is_katakana) {
                    index2 = i5 + 1;
                    CharacterIteration.next32(text);
                    while (index2 < numChars && index2 - i5 < 20 && isKatakana(CharacterIteration.current32(text))) {
                        CharacterIteration.next32(text);
                        index2++;
                    }
                    if (index2 - i5 < 20) {
                        newSnlp = bestSnlp2[i5] + getKatakanaCost(index2 - i5);
                        if (newSnlp < bestSnlp2[index2]) {
                            bestSnlp2[index2] = newSnlp;
                            prev[index2] = i5;
                        }
                    }
                }
                is_prev_katakana = is_katakana;
            }
            i4 = i5 + 1;
            lengths2 = lengths;
            inputLength2 = inputLength;
            bestSnlp = bestSnlp2;
            values2 = values;
            prev2 = prev;
            s2 = s;
            index2 = Integer.MAX_VALUE;
        }
        inputLength = inputLength2;
        s = s2;
        values = values2;
        prev = prev2;
        lengths = lengths2;
        int[] t_boundary = new int[(numChars + 1)];
        if (bestSnlp[numChars] == Integer.MAX_VALUE) {
            t_boundary[0] = numChars;
            i3 = 0 + 1;
        } else {
            boolean z = true;
            i3 = 0;
            for (inputLength2 = numChars; inputLength2 > 0; inputLength2 = prev[inputLength2]) {
                t_boundary[i3] = inputLength2;
                i3++;
            }
            if (prev[t_boundary[i3 - 1]] != 0) {
                z = false;
            }
            Assert.assrt(z);
        }
        if (foundBreaks.size() == 0 || foundBreaks.peek() < i) {
            inputLength2 = i3 + 1;
            t_boundary[i3] = 0;
            i3 = inputLength2;
        }
        inputLength2 = 0;
        for (newSnlp = i3 - 1; newSnlp >= 0; newSnlp--) {
            index = charPositions[t_boundary[newSnlp]] + i;
            if (!(dequeI.contains(index) || index == i)) {
                dequeI.push(charPositions[t_boundary[newSnlp]] + i);
                inputLength2++;
            }
        }
        if (!foundBreaks.isEmpty() && foundBreaks.peek() == i2) {
            foundBreaks.pop();
            inputLength2--;
        }
        if (foundBreaks.isEmpty()) {
            text2 = inText;
        } else {
            inText.setIndex(foundBreaks.peek());
        }
        return inputLength2;
    }
}
