package android.icu.text;

import android.icu.impl.number.Padder;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator.Position;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import java.text.CharacterIterator;

final class BreakTransliterator extends Transliterator {
    static final int LETTER_OR_MARK_MASK = 510;
    private BreakIterator bi;
    private int[] boundaries;
    private int boundaryCount;
    private String insertion;

    static final class ReplaceableCharacterIterator implements CharacterIterator {
        private int begin;
        private int end;
        private int pos;
        private Replaceable text;

        public ReplaceableCharacterIterator(Replaceable text, int begin, int end, int pos) {
            if (text != null) {
                this.text = text;
                if (begin < 0 || begin > end || end > text.length()) {
                    throw new IllegalArgumentException("Invalid substring range");
                } else if (pos < begin || pos > end) {
                    throw new IllegalArgumentException("Invalid position");
                } else {
                    this.begin = begin;
                    this.end = end;
                    this.pos = pos;
                    return;
                }
            }
            throw new NullPointerException();
        }

        public void setText(Replaceable text) {
            if (text != null) {
                this.text = text;
                this.begin = 0;
                this.end = text.length();
                this.pos = 0;
                return;
            }
            throw new NullPointerException();
        }

        public char first() {
            this.pos = this.begin;
            return current();
        }

        public char last() {
            if (this.end != this.begin) {
                this.pos = this.end - 1;
            } else {
                this.pos = this.end;
            }
            return current();
        }

        public char setIndex(int p) {
            if (p < this.begin || p > this.end) {
                throw new IllegalArgumentException("Invalid index");
            }
            this.pos = p;
            return current();
        }

        public char current() {
            if (this.pos < this.begin || this.pos >= this.end) {
                return 65535;
            }
            return this.text.charAt(this.pos);
        }

        public char next() {
            if (this.pos < this.end - 1) {
                this.pos++;
                return this.text.charAt(this.pos);
            }
            this.pos = this.end;
            return 65535;
        }

        public char previous() {
            if (this.pos <= this.begin) {
                return 65535;
            }
            this.pos--;
            return this.text.charAt(this.pos);
        }

        public int getBeginIndex() {
            return this.begin;
        }

        public int getEndIndex() {
            return this.end;
        }

        public int getIndex() {
            return this.pos;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReplaceableCharacterIterator)) {
                return false;
            }
            ReplaceableCharacterIterator that = (ReplaceableCharacterIterator) obj;
            if (hashCode() == that.hashCode() && this.text.equals(that.text) && this.pos == that.pos && this.begin == that.begin && this.end == that.end) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return ((this.text.hashCode() ^ this.pos) ^ this.begin) ^ this.end;
        }

        public Object clone() {
            try {
                return (ReplaceableCharacterIterator) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException();
            }
        }
    }

    public BreakTransliterator(String ID, UnicodeFilter filter, BreakIterator bi, String insertion) {
        super(ID, filter);
        this.boundaries = new int[50];
        this.boundaryCount = 0;
        this.bi = bi;
        this.insertion = insertion;
    }

    public BreakTransliterator(String ID, UnicodeFilter filter) {
        this(ID, filter, null, Padder.FALLBACK_PADDING_STRING);
    }

    public String getInsertion() {
        return this.insertion;
    }

    public void setInsertion(String insertion) {
        this.insertion = insertion;
    }

    public BreakIterator getBreakIterator() {
        if (this.bi == null) {
            this.bi = BreakIterator.getWordInstance(new ULocale("th_TH"));
        }
        return this.bi;
    }

    public void setBreakIterator(BreakIterator bi) {
        this.bi = bi;
    }

    protected synchronized void handleTransliterate(Replaceable text, Position pos, boolean incremental) {
        int boundary;
        int delta;
        int i;
        this.boundaryCount = 0;
        getBreakIterator();
        this.bi.setText(new ReplaceableCharacterIterator(text, pos.start, pos.limit, pos.start));
        int first = this.bi.first();
        while (true) {
            boundary = first;
            if (boundary == -1 || boundary >= pos.limit) {
                delta = 0;
                first = 0;
            } else {
                if (boundary != 0) {
                    if (((1 << UCharacter.getType(UTF16.charAt(text, boundary - 1))) & LETTER_OR_MARK_MASK) != 0) {
                        if (((1 << UCharacter.getType(UTF16.charAt(text, boundary))) & LETTER_OR_MARK_MASK) != 0) {
                            int[] temp;
                            if (this.boundaryCount >= this.boundaries.length) {
                                temp = new int[(this.boundaries.length * 2)];
                                System.arraycopy(this.boundaries, 0, temp, 0, this.boundaries.length);
                                this.boundaries = temp;
                            }
                            temp = this.boundaries;
                            i = this.boundaryCount;
                            this.boundaryCount = i + 1;
                            temp[i] = boundary;
                        }
                    }
                }
                first = this.bi.next();
            }
        }
        delta = 0;
        first = 0;
        if (this.boundaryCount != 0) {
            delta = this.boundaryCount * this.insertion.length();
            first = this.boundaries[this.boundaryCount - 1];
            while (this.boundaryCount > 0) {
                int[] iArr = this.boundaries;
                i = this.boundaryCount - 1;
                this.boundaryCount = i;
                boundary = iArr[i];
                text.replace(boundary, boundary, this.insertion);
            }
        }
        pos.contextLimit += delta;
        pos.limit += delta;
        pos.start = incremental ? first + delta : pos.limit;
    }

    static void register() {
        Transliterator.registerInstance(new BreakTransliterator("Any-BreakInternal", null), false);
    }

    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        if (getFilterAsUnicodeSet(inputFilter).size() != 0) {
            targetSet.addAll(this.insertion);
        }
    }
}
