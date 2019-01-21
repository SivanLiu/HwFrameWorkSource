package android.icu.impl;

import android.icu.impl.ICUResourceBundle.OpenType;
import android.icu.text.BreakIterator;
import android.icu.text.FilteredBreakIteratorBuilder;
import android.icu.text.UCharacterIterator;
import android.icu.util.BytesTrie.Result;
import android.icu.util.CharsTrie;
import android.icu.util.CharsTrieBuilder;
import android.icu.util.StringTrieBuilder.Option;
import android.icu.util.ULocale;
import java.text.CharacterIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class SimpleFilteredSentenceBreakIterator extends BreakIterator {
    private CharsTrie backwardsTrie;
    private BreakIterator delegate;
    private CharsTrie forwardsPartialTrie;
    private UCharacterIterator text;

    public static class Builder extends FilteredBreakIteratorBuilder {
        static final int AddToForward = 2;
        static final int MATCH = 2;
        static final int PARTIAL = 1;
        static final int SuppressInReverse = 1;
        private HashSet<CharSequence> filterSet;

        public Builder(Locale loc) {
            this(ULocale.forLocale(loc));
        }

        public Builder(ULocale loc) {
            this.filterSet = new HashSet();
            ICUResourceBundle breaks = ICUResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME, loc, OpenType.LOCALE_ROOT).findWithFallback("exceptions/SentenceBreak");
            if (breaks != null) {
                int size = breaks.getSize();
                for (int index = 0; index < size; index++) {
                    this.filterSet.add(((ICUResourceBundle) breaks.get(index)).getString());
                }
            }
        }

        public Builder() {
            this.filterSet = new HashSet();
        }

        public boolean suppressBreakAfter(CharSequence str) {
            return this.filterSet.add(str);
        }

        public boolean unsuppressBreakAfter(CharSequence str) {
            return this.filterSet.remove(str);
        }

        public BreakIterator wrapIteratorWithFilter(BreakIterator adoptBreakIterator) {
            BreakIterator breakIterator = adoptBreakIterator;
            if (this.filterSet.isEmpty()) {
                return breakIterator;
            }
            int i;
            int fwdCount;
            CharsTrie backwardsTrie;
            CharsTrieBuilder builder = new CharsTrieBuilder();
            CharsTrieBuilder builder2 = new CharsTrieBuilder();
            int revCount = 0;
            int fwdCount2 = 0;
            int subCount = this.filterSet.size();
            CharSequence[] ustrs = new CharSequence[subCount];
            int[] partials = new int[subCount];
            CharsTrie backwardsTrie2 = null;
            CharsTrie forwardsPartialTrie = null;
            int i2 = 0;
            Iterator it = this.filterSet.iterator();
            while (true) {
                i = 0;
                if (!it.hasNext()) {
                    break;
                }
                ustrs[i2] = (CharSequence) it.next();
                partials[i2] = 0;
                i2++;
            }
            i2 = 0;
            while (i2 < subCount) {
                int backwardsTrie3;
                String thisStr = ustrs[i2].toString();
                int nn = thisStr.indexOf(46);
                if (nn <= -1) {
                    fwdCount = fwdCount2;
                    backwardsTrie = backwardsTrie2;
                    backwardsTrie3 = i;
                } else if (nn + 1 != thisStr.length()) {
                    int sameAs = -1;
                    i = 0;
                    while (i < subCount) {
                        if (i == i2) {
                            fwdCount = fwdCount2;
                            backwardsTrie = backwardsTrie2;
                        } else {
                            fwdCount = fwdCount2;
                            backwardsTrie = backwardsTrie2;
                            if (thisStr.regionMatches(0, ustrs[i].toString(), 0, nn + 1)) {
                                if (partials[i] == 0) {
                                    partials[i] = 3;
                                } else if ((partials[i] & 1) != 0) {
                                    sameAs = i;
                                }
                            }
                        }
                        i++;
                        fwdCount2 = fwdCount;
                        backwardsTrie2 = backwardsTrie;
                    }
                    fwdCount = fwdCount2;
                    backwardsTrie = backwardsTrie2;
                    if (sameAs == -1 && partials[i2] == 0) {
                        backwardsTrie3 = 0;
                        StringBuilder prefix = new StringBuilder(thisStr.substring(0, nn + 1));
                        prefix.reverse();
                        builder.add(prefix, 1);
                        revCount++;
                        partials[i2] = 3;
                    } else {
                        backwardsTrie3 = 0;
                    }
                } else {
                    fwdCount = fwdCount2;
                    backwardsTrie = backwardsTrie2;
                    backwardsTrie3 = 0;
                }
                i2++;
                i = backwardsTrie3;
                fwdCount2 = fwdCount;
                backwardsTrie2 = backwardsTrie;
            }
            fwdCount = fwdCount2;
            backwardsTrie = backwardsTrie2;
            for (int i3 = 0; i3 < subCount; i3++) {
                String thisStr2 = ustrs[i3].toString();
                if (partials[i3] == 0) {
                    builder.add(new StringBuilder(thisStr2).reverse(), 2);
                    revCount++;
                } else {
                    builder2.add(thisStr2, 2);
                    fwdCount++;
                }
            }
            if (revCount > 0) {
                backwardsTrie2 = builder.build(Option.FAST);
            } else {
                backwardsTrie2 = backwardsTrie;
            }
            if (fwdCount > 0) {
                forwardsPartialTrie = builder2.build(Option.FAST);
            }
            return new SimpleFilteredSentenceBreakIterator(breakIterator, forwardsPartialTrie, backwardsTrie2);
        }
    }

    public SimpleFilteredSentenceBreakIterator(BreakIterator adoptBreakIterator, CharsTrie forwardsPartialTrie, CharsTrie backwardsTrie) {
        this.delegate = adoptBreakIterator;
        this.forwardsPartialTrie = forwardsPartialTrie;
        this.backwardsTrie = backwardsTrie;
    }

    private final void resetState() {
        this.text = UCharacterIterator.getInstance((CharacterIterator) this.delegate.getText().clone());
    }

    private final boolean breakExceptionAt(int n) {
        Result nextForCodePoint;
        int bestPosn = -1;
        int bestValue = -1;
        this.text.setIndex(n);
        this.backwardsTrie.reset();
        int previousCodePoint = this.text.previousCodePoint();
        int uch = previousCodePoint;
        if (previousCodePoint != 32) {
            uch = this.text.nextCodePoint();
        }
        Result r = Result.INTERMEDIATE_VALUE;
        while (true) {
            int previousCodePoint2 = this.text.previousCodePoint();
            uch = previousCodePoint2;
            if (previousCodePoint2 == -1) {
                break;
            }
            nextForCodePoint = this.backwardsTrie.nextForCodePoint(uch);
            r = nextForCodePoint;
            if (!nextForCodePoint.hasNext()) {
                break;
            } else if (r.hasValue()) {
                bestPosn = this.text.getIndex();
                bestValue = this.backwardsTrie.getValue();
            }
        }
        if (r.matches()) {
            bestValue = this.backwardsTrie.getValue();
            bestPosn = this.text.getIndex();
        }
        if (bestPosn >= 0) {
            if (bestValue == 2) {
                return true;
            }
            if (bestValue == 1 && this.forwardsPartialTrie != null) {
                this.forwardsPartialTrie.reset();
                nextForCodePoint = Result.INTERMEDIATE_VALUE;
                this.text.setIndex(bestPosn);
                while (true) {
                    int nextCodePoint = this.text.nextCodePoint();
                    uch = nextCodePoint;
                    if (nextCodePoint == -1) {
                        break;
                    }
                    Result nextForCodePoint2 = this.forwardsPartialTrie.nextForCodePoint(uch);
                    nextForCodePoint = nextForCodePoint2;
                    if (!nextForCodePoint2.hasNext()) {
                        break;
                    }
                }
                if (nextForCodePoint.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:11:0x0023, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final int internalNext(int n) {
        if (n == -1 || this.backwardsTrie == null) {
            return n;
        }
        resetState();
        int textLen = this.text.getLength();
        while (n != -1 && n != textLen && breakExceptionAt(n)) {
            n = this.delegate.next();
        }
        return n;
    }

    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final int internalPrev(int n) {
        if (n == 0 || n == -1 || this.backwardsTrie == null) {
            return n;
        }
        resetState();
        while (n != -1 && n != 0 && breakExceptionAt(n)) {
            n = this.delegate.previous();
        }
        return n;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimpleFilteredSentenceBreakIterator other = (SimpleFilteredSentenceBreakIterator) obj;
        if (this.delegate.equals(other.delegate) && this.text.equals(other.text) && this.backwardsTrie.equals(other.backwardsTrie) && this.forwardsPartialTrie.equals(other.forwardsPartialTrie)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return ((this.forwardsPartialTrie.hashCode() * 39) + (this.backwardsTrie.hashCode() * 11)) + this.delegate.hashCode();
    }

    public Object clone() {
        return (SimpleFilteredSentenceBreakIterator) super.clone();
    }

    public int first() {
        return this.delegate.first();
    }

    public int preceding(int offset) {
        return internalPrev(this.delegate.preceding(offset));
    }

    public int previous() {
        return internalPrev(this.delegate.previous());
    }

    public int current() {
        return this.delegate.current();
    }

    public boolean isBoundary(int offset) {
        if (!this.delegate.isBoundary(offset)) {
            return false;
        }
        if (this.backwardsTrie == null) {
            return true;
        }
        resetState();
        return breakExceptionAt(offset) ^ 1;
    }

    public int next() {
        return internalNext(this.delegate.next());
    }

    public int next(int n) {
        return internalNext(this.delegate.next(n));
    }

    public int following(int offset) {
        return internalNext(this.delegate.following(offset));
    }

    public int last() {
        return this.delegate.last();
    }

    public CharacterIterator getText() {
        return this.delegate.getText();
    }

    public void setText(CharacterIterator newText) {
        this.delegate.setText(newText);
    }
}
