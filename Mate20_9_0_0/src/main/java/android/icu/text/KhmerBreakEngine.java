package android.icu.text;

import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import java.io.IOException;
import java.text.CharacterIterator;

class KhmerBreakEngine extends DictionaryBreakEngine {
    private static final byte KHMER_LOOKAHEAD = (byte) 3;
    private static final byte KHMER_MIN_WORD = (byte) 2;
    private static final byte KHMER_MIN_WORD_SPAN = (byte) 4;
    private static final byte KHMER_PREFIX_COMBINE_THRESHOLD = (byte) 3;
    private static final byte KHMER_ROOT_COMBINE_THRESHOLD = (byte) 3;
    private static UnicodeSet fBeginWordSet = new UnicodeSet();
    private static UnicodeSet fEndWordSet = new UnicodeSet(fKhmerWordSet);
    private static UnicodeSet fKhmerWordSet = new UnicodeSet();
    private static UnicodeSet fMarkSet = new UnicodeSet();
    private DictionaryMatcher fDictionary = DictionaryData.loadDictionaryFor("Khmr");

    static {
        fKhmerWordSet.applyPattern("[[:Khmer:]&[:LineBreak=SA:]]");
        fKhmerWordSet.compact();
        fMarkSet.applyPattern("[[:Khmer:]&[:LineBreak=SA:]&[:M:]]");
        fMarkSet.add(32);
        fBeginWordSet.add(6016, 6067);
        fEndWordSet.remove(6098);
        fMarkSet.compact();
        fEndWordSet.compact();
        fBeginWordSet.compact();
        fKhmerWordSet.freeze();
        fMarkSet.freeze();
        fEndWordSet.freeze();
        fBeginWordSet.freeze();
    }

    public KhmerBreakEngine() throws IOException {
        super(Integer.valueOf(1), Integer.valueOf(2));
        setCharacters(fKhmerWordSet);
    }

    public boolean equals(Object obj) {
        return obj instanceof KhmerBreakEngine;
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean handles(int c, int breakType) {
        boolean z = false;
        if (breakType != 1 && breakType != 2) {
            return false;
        }
        if (UCharacter.getIntPropertyValue(c, UProperty.SCRIPT) == 23) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x013b  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x012b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
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
            i3 = 0;
            int candidates = words[wordsFound % 3].candidates(characterIterator, this.fDictionary, i);
            if (candidates == 1) {
                i3 = words[wordsFound % 3].acceptMarked(characterIterator);
                wordsFound++;
            } else if (candidates > 1) {
                boolean foundBest = false;
                if (fIter.getIndex() < i) {
                    do {
                        if (words[(wordsFound + 1) % i4].candidates(characterIterator, this.fDictionary, i) > 0) {
                            if (1 < 2) {
                                words[wordsFound % 3].markCurrent();
                            }
                            if (fIter.getIndex() >= i) {
                                break;
                            }
                            while (words[(wordsFound + 2) % i4].candidates(characterIterator, this.fDictionary, i) <= 0) {
                                if (!words[(wordsFound + 1) % i4].backUp(characterIterator)) {
                                    break;
                                }
                            }
                            words[wordsFound % 3].markCurrent();
                            foundBest = true;
                        }
                        if (!words[wordsFound % 3].backUp(characterIterator)) {
                            break;
                        }
                    } while (!foundBest);
                }
                i3 = words[wordsFound % 3].acceptMarked(characterIterator);
                wordsFound++;
            }
            if (fIter.getIndex() < i && i3 < i4) {
                if (words[wordsFound % 3].candidates(characterIterator, this.fDictionary, i) > 0 || (i3 != 0 && words[wordsFound % 3].longestPrefix() >= i4)) {
                    characterIterator.setIndex(current + i3);
                } else {
                    int remaining = i - (current + i3);
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
                } else if (i3 <= 0) {
                    foundBreaks.push(Integer.valueOf(current + i3).intValue());
                } else {
                    dequeI = foundBreaks;
                }
            }
            if (i3 <= 0) {
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
