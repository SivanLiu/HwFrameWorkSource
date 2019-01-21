package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl.Hangul;
import android.icu.impl.Trie2.Range;
import android.icu.impl.Utility;
import android.icu.text.UnicodeSet;
import android.icu.util.CharsTrie;
import android.icu.util.CharsTrie.Entry;
import java.util.Iterator;

public final class TailoredSet {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private CollationData baseData;
    private CollationData data;
    private String suffix;
    private UnicodeSet tailored;
    private StringBuilder unreversedPrefix = new StringBuilder();

    public TailoredSet(UnicodeSet t) {
        this.tailored = t;
    }

    public void forData(CollationData d) {
        this.data = d;
        this.baseData = d.base;
        Iterator<Range> trieIterator = this.data.trie.iterator();
        while (trieIterator.hasNext()) {
            Range range = (Range) trieIterator.next();
            Range range2 = range;
            if (!range.leadSurrogate) {
                enumTailoredRange(range2.startCodePoint, range2.endCodePoint, range2.value, this);
            } else {
                return;
            }
        }
    }

    private void enumTailoredRange(int start, int end, int ce32, TailoredSet ts) {
        if (ce32 != 192) {
            ts.handleCE32(start, end, ce32);
        }
    }

    private void handleCE32(int start, int end, int ce32) {
        if (Collation.isSpecialCE32(ce32)) {
            ce32 = this.data.getIndirectCE32(ce32);
            if (ce32 == 192) {
                return;
            }
        }
        do {
            int baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32(start));
            if (!Collation.isSelfContainedCE32(ce32) || !Collation.isSelfContainedCE32(baseCE32)) {
                compare(start, ce32, baseCE32);
            } else if (ce32 != baseCE32) {
                this.tailored.add(start);
            }
            start++;
        } while (start <= end);
    }

    private void compare(int c, int ce32, int baseCE32) {
        int dataIndex;
        int baseIndex;
        if (Collation.isPrefixCE32(ce32)) {
            dataIndex = Collation.indexFromCE32(ce32);
            ce32 = this.data.getFinalCE32(this.data.getCE32FromContexts(dataIndex));
            if (Collation.isPrefixCE32(baseCE32)) {
                baseIndex = Collation.indexFromCE32(baseCE32);
                baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(baseIndex));
                comparePrefixes(c, this.data.contexts, dataIndex + 2, this.baseData.contexts, baseIndex + 2);
            } else {
                addPrefixes(this.data, c, this.data.contexts, dataIndex + 2);
            }
        } else if (Collation.isPrefixCE32(baseCE32)) {
            dataIndex = Collation.indexFromCE32(baseCE32);
            baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(dataIndex));
            addPrefixes(this.baseData, c, this.baseData.contexts, dataIndex + 2);
        }
        if (Collation.isContractionCE32(ce32)) {
            dataIndex = Collation.indexFromCE32(ce32);
            if ((ce32 & 256) != 0) {
                ce32 = 1;
            } else {
                ce32 = this.data.getFinalCE32(this.data.getCE32FromContexts(dataIndex));
            }
            if (Collation.isContractionCE32(baseCE32)) {
                baseIndex = Collation.indexFromCE32(baseCE32);
                if ((baseCE32 & 256) != 0) {
                    baseCE32 = 1;
                } else {
                    baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(baseIndex));
                }
                compareContractions(c, this.data.contexts, dataIndex + 2, this.baseData.contexts, baseIndex + 2);
            } else {
                addContractions(c, this.data.contexts, dataIndex + 2);
            }
        } else if (Collation.isContractionCE32(baseCE32)) {
            dataIndex = Collation.indexFromCE32(baseCE32);
            baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(dataIndex));
            addContractions(c, this.baseData.contexts, dataIndex + 2);
        }
        baseIndex = -1;
        if (Collation.isSpecialCE32(ce32)) {
            dataIndex = Collation.tagFromCE32(ce32);
        } else {
            dataIndex = -1;
        }
        if (Collation.isSpecialCE32(baseCE32)) {
            baseIndex = Collation.tagFromCE32(baseCE32);
        }
        if (baseIndex == 14) {
            if (Collation.isLongPrimaryCE32(ce32)) {
                if (Collation.primaryFromLongPrimaryCE32(ce32) != Collation.getThreeBytePrimaryForOffsetData(c, this.baseData.ces[Collation.indexFromCE32(baseCE32)])) {
                    add(c);
                    return;
                }
            }
            add(c);
            return;
        }
        if (dataIndex != baseIndex) {
            add(c);
            return;
        }
        int i = 0;
        int length;
        int idx0;
        int idx1;
        if (dataIndex == 5) {
            length = Collation.lengthFromCE32(ce32);
            if (length != Collation.lengthFromCE32(baseCE32)) {
                add(c);
                return;
            }
            idx0 = Collation.indexFromCE32(ce32);
            idx1 = Collation.indexFromCE32(baseCE32);
            while (i < length) {
                if (this.data.ce32s[idx0 + i] != this.baseData.ce32s[idx1 + i]) {
                    add(c);
                    break;
                }
                i++;
            }
        } else if (dataIndex == 6) {
            length = Collation.lengthFromCE32(ce32);
            if (length != Collation.lengthFromCE32(baseCE32)) {
                add(c);
                return;
            }
            idx0 = Collation.indexFromCE32(ce32);
            idx1 = Collation.indexFromCE32(baseCE32);
            while (i < length) {
                if (this.data.ces[idx0 + i] != this.baseData.ces[idx1 + i]) {
                    add(c);
                    break;
                }
                i++;
            }
        } else if (dataIndex == 12) {
            StringBuilder jamos = new StringBuilder();
            int length2 = Hangul.decompose(c, jamos);
            if (this.tailored.contains(jamos.charAt(0)) || this.tailored.contains(jamos.charAt(1)) || (length2 == 3 && this.tailored.contains(jamos.charAt(2)))) {
                add(c);
            }
        } else if (ce32 != baseCE32) {
            add(c);
        }
    }

    private void comparePrefixes(int c, CharSequence p, int pidx, CharSequence q, int qidx) {
        int i = c;
        CharsTrie.Iterator prefixes = new CharsTrie(p, pidx).iterator();
        CharsTrie.Iterator basePrefixes = new CharsTrie(q, qidx).iterator();
        String none = "￿";
        Entry te = null;
        String bp = null;
        String tp = null;
        Entry be = null;
        while (true) {
            if (tp == null) {
                if (prefixes.hasNext()) {
                    te = prefixes.next();
                    tp = te.chars.toString();
                } else {
                    te = null;
                    tp = none;
                }
            }
            if (bp == null) {
                if (basePrefixes.hasNext()) {
                    be = basePrefixes.next();
                    bp = be.chars.toString();
                } else {
                    be = null;
                    bp = none;
                }
            }
            if (!Utility.sameObjects(tp, none) || !Utility.sameObjects(bp, none)) {
                int cmp = tp.compareTo(bp);
                if (cmp < 0) {
                    addPrefix(this.data, tp, i, te.value);
                    te = null;
                    tp = null;
                } else if (cmp > 0) {
                    addPrefix(this.baseData, bp, i, be.value);
                    be = null;
                    bp = null;
                } else {
                    setPrefix(tp);
                    compare(i, te.value, be.value);
                    resetPrefix();
                    be = null;
                    te = null;
                    bp = null;
                    tp = null;
                }
            } else {
                return;
            }
        }
    }

    private void compareContractions(int c, CharSequence p, int pidx, CharSequence q, int qidx) {
        int i = c;
        CharsTrie.Iterator suffixes = new CharsTrie(p, pidx).iterator();
        CharsTrie.Iterator baseSuffixes = new CharsTrie(q, qidx).iterator();
        String none = "￿￿";
        Entry te = null;
        String bs = null;
        String ts = null;
        Entry be = null;
        while (true) {
            if (ts == null) {
                if (suffixes.hasNext()) {
                    te = suffixes.next();
                    ts = te.chars.toString();
                } else {
                    te = null;
                    ts = none;
                }
            }
            if (bs == null) {
                if (baseSuffixes.hasNext()) {
                    be = baseSuffixes.next();
                    bs = be.chars.toString();
                } else {
                    be = null;
                    bs = none;
                }
            }
            if (!Utility.sameObjects(ts, none) || !Utility.sameObjects(bs, none)) {
                int cmp = ts.compareTo(bs);
                if (cmp < 0) {
                    addSuffix(i, ts);
                    te = null;
                    ts = null;
                } else if (cmp > 0) {
                    addSuffix(i, bs);
                    be = null;
                    bs = null;
                } else {
                    this.suffix = ts;
                    compare(i, te.value, be.value);
                    this.suffix = null;
                    be = null;
                    te = null;
                    bs = null;
                    ts = null;
                }
            } else {
                return;
            }
        }
    }

    private void addPrefixes(CollationData d, int c, CharSequence p, int pidx) {
        CharsTrie.Iterator prefixes = new CharsTrie(p, pidx).iterator();
        while (prefixes.hasNext()) {
            Entry e = prefixes.next();
            addPrefix(d, e.chars, c, e.value);
        }
    }

    private void addPrefix(CollationData d, CharSequence pfx, int c, int ce32) {
        setPrefix(pfx);
        ce32 = d.getFinalCE32(ce32);
        if (Collation.isContractionCE32(ce32)) {
            addContractions(c, d.contexts, Collation.indexFromCE32(ce32) + 2);
        }
        this.tailored.add(new StringBuilder(this.unreversedPrefix.appendCodePoint(c)));
        resetPrefix();
    }

    private void addContractions(int c, CharSequence p, int pidx) {
        CharsTrie.Iterator suffixes = new CharsTrie(p, pidx).iterator();
        while (suffixes.hasNext()) {
            addSuffix(c, suffixes.next().chars);
        }
    }

    private void addSuffix(int c, CharSequence sfx) {
        UnicodeSet unicodeSet = this.tailored;
        CharSequence appendCodePoint = new StringBuilder(this.unreversedPrefix).appendCodePoint(c);
        appendCodePoint.append(sfx);
        unicodeSet.add(appendCodePoint);
    }

    private void add(int c) {
        if (this.unreversedPrefix.length() == 0 && this.suffix == null) {
            this.tailored.add(c);
            return;
        }
        CharSequence s = new StringBuilder(this.unreversedPrefix);
        s.appendCodePoint(c);
        if (this.suffix != null) {
            s.append(this.suffix);
        }
        this.tailored.add(s);
    }

    private void setPrefix(CharSequence pfx) {
        this.unreversedPrefix.setLength(0);
        StringBuilder stringBuilder = this.unreversedPrefix;
        stringBuilder.append(pfx);
        stringBuilder.reverse();
    }

    private void resetPrefix() {
        this.unreversedPrefix.setLength(0);
    }
}
