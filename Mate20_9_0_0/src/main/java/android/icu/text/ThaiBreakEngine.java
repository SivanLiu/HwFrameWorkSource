package android.icu.text;

import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import java.io.IOException;
import java.text.CharacterIterator;

class ThaiBreakEngine extends DictionaryBreakEngine {
    private static final byte THAI_LOOKAHEAD = (byte) 3;
    private static final char THAI_MAIYAMOK = 'ๆ';
    private static final byte THAI_MIN_WORD = (byte) 2;
    private static final byte THAI_MIN_WORD_SPAN = (byte) 4;
    private static final char THAI_PAIYANNOI = 'ฯ';
    private static final byte THAI_PREFIX_COMBINE_THRESHOLD = (byte) 3;
    private static final byte THAI_ROOT_COMBINE_THRESHOLD = (byte) 3;
    private static UnicodeSet fBeginWordSet = new UnicodeSet();
    private static UnicodeSet fEndWordSet = new UnicodeSet(fThaiWordSet);
    private static UnicodeSet fMarkSet = new UnicodeSet();
    private static UnicodeSet fSuffixSet = new UnicodeSet();
    private static UnicodeSet fThaiWordSet = new UnicodeSet();
    private DictionaryMatcher fDictionary = DictionaryData.loadDictionaryFor("Thai");

    static {
        fThaiWordSet.applyPattern("[[:Thai:]&[:LineBreak=SA:]]");
        fThaiWordSet.compact();
        fMarkSet.applyPattern("[[:Thai:]&[:LineBreak=SA:]&[:M:]]");
        fMarkSet.add(32);
        fEndWordSet.remove(3633);
        fEndWordSet.remove(3648, 3652);
        fBeginWordSet.add(3585, 3630);
        fBeginWordSet.add(3648, 3652);
        fSuffixSet.add(3631);
        fSuffixSet.add(3654);
        fMarkSet.compact();
        fEndWordSet.compact();
        fBeginWordSet.compact();
        fSuffixSet.compact();
        fThaiWordSet.freeze();
        fMarkSet.freeze();
        fEndWordSet.freeze();
        fBeginWordSet.freeze();
        fSuffixSet.freeze();
    }

    public ThaiBreakEngine() throws IOException {
        super(Integer.valueOf(1), Integer.valueOf(2));
        setCharacters(fThaiWordSet);
    }

    public boolean equals(Object obj) {
        return obj instanceof ThaiBreakEngine;
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean handles(int c, int breakType) {
        boolean z = false;
        if (breakType != 1 && breakType != 2) {
            return false;
        }
        if (UCharacter.getIntPropertyValue(c, UProperty.SCRIPT) == 38) {
            z = true;
        }
        return z;
    }

    public int divideUpDictionaryRange(CharacterIterator fIter, int rangeStart, int rangeEnd, DequeI foundBreaks) {
        CharacterIterator characterIterator = fIter;
        int i = rangeEnd;
        int i2 = 0;
        if (i - rangeStart < 4) {
            return 0;
        }
        int i3;
        DequeI dequeI;
        int wordsFound = 0;
        int i4 = 3;
        PossibleWord[] words = new PossibleWord[3];
        for (i3 = 0; i3 < 3; i3++) {
            words[i3] = new PossibleWord();
        }
        fIter.setIndex(rangeStart);
        while (true) {
            i3 = fIter.getIndex();
            int current = i3;
            if (i3 >= i) {
                break;
            }
            int remaining;
            i3 = 0;
            int candidates = words[wordsFound % 3].candidates(characterIterator, this.fDictionary, i);
            if (candidates == 1) {
                i3 = words[wordsFound % 3].acceptMarked(characterIterator);
                wordsFound++;
            } else if (candidates > 1) {
                if (fIter.getIndex() < i) {
                    do {
                        if (words[(wordsFound + 1) % i4].candidates(characterIterator, this.fDictionary, i) > 0) {
                            if (1 < 2) {
                                words[wordsFound % 3].markCurrent();
                            }
                            if (fIter.getIndex() < i) {
                                while (words[(wordsFound + 2) % i4].candidates(characterIterator, this.fDictionary, i) <= 0) {
                                    if (!words[(wordsFound + 1) % i4].backUp(characterIterator)) {
                                    }
                                }
                                words[wordsFound % 3].markCurrent();
                                break;
                            }
                            break;
                        }
                    } while (words[wordsFound % 3].backUp(characterIterator));
                    break;
                }
                i3 = words[wordsFound % 3].acceptMarked(characterIterator);
                wordsFound++;
            }
            if (fIter.getIndex() < i && i3 < i4) {
                if (words[wordsFound % 3].candidates(characterIterator, this.fDictionary, i) > 0 || (i3 != 0 && words[wordsFound % 3].longestPrefix() >= i4)) {
                    characterIterator.setIndex(current + i3);
                } else {
                    remaining = i - (current + i3);
                    int pc = fIter.current();
                    int remaining2 = remaining;
                    remaining = i2;
                    while (true) {
                        fIter.next();
                        int uc = fIter.current();
                        remaining++;
                        remaining2--;
                        if (remaining2 <= 0) {
                            break;
                        }
                        if (fEndWordSet.contains(pc) && fBeginWordSet.contains(uc)) {
                            i2 = words[(wordsFound + 1) % i4].candidates(characterIterator, this.fDictionary, i);
                            characterIterator.setIndex((current + i3) + remaining);
                            if (i2 > 0) {
                                break;
                            }
                        }
                        pc = uc;
                        i4 = 3;
                    }
                    if (i3 <= 0) {
                        wordsFound++;
                    }
                    i3 += remaining;
                }
            }
            while (true) {
                i2 = fIter.getIndex();
                i4 = i2;
                if (i2 < i && fMarkSet.contains(fIter.current())) {
                    fIter.next();
                    i3 += fIter.getIndex() - i4;
                }
            }
            if (fIter.getIndex() < i && i3 > 0) {
                if (words[wordsFound % 3].candidates(characterIterator, this.fDictionary, i) <= 0) {
                    UnicodeSet unicodeSet = fSuffixSet;
                    int current2 = fIter.current();
                    remaining = current2;
                    if (unicodeSet.contains(current2)) {
                        if (remaining == 3631) {
                            if (fSuffixSet.contains(fIter.previous())) {
                                fIter.next();
                            } else {
                                fIter.next();
                                fIter.next();
                                i3++;
                                remaining = fIter.current();
                            }
                        }
                        if (remaining == 3654) {
                            if (fIter.previous() != THAI_MAIYAMOK) {
                                fIter.next();
                                fIter.next();
                                i3++;
                            } else {
                                fIter.next();
                            }
                        }
                    }
                }
                characterIterator.setIndex(current + i3);
            }
            if (i3 > 0) {
                foundBreaks.push(Integer.valueOf(current + i3).intValue());
            } else {
                dequeI = foundBreaks;
            }
            i2 = 0;
            i4 = 3;
        }
        dequeI = foundBreaks;
        if (foundBreaks.peek() >= i) {
            foundBreaks.pop();
            wordsFound--;
        }
        return wordsFound;
    }
}
