package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;

public final class FCDUTF16CollationIterator extends UTF16CollationIterator {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int rawStart = 0;
    private int checkDir;
    private final Normalizer2Impl nfcImpl;
    private StringBuilder normalized;
    private int rawLimit;
    private CharSequence rawSeq;
    private int segmentLimit;
    private int segmentStart;

    public FCDUTF16CollationIterator(CollationData d) {
        super(d);
        this.nfcImpl = d.nfcImpl;
    }

    public FCDUTF16CollationIterator(CollationData data, boolean numeric, CharSequence s, int p) {
        super(data, numeric, s, p);
        this.rawSeq = s;
        this.segmentStart = p;
        this.rawLimit = s.length();
        this.nfcImpl = data.nfcImpl;
        this.checkDir = 1;
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof CollationIterator) || !equals(other) || !(other instanceof FCDUTF16CollationIterator)) {
            return false;
        }
        FCDUTF16CollationIterator o = (FCDUTF16CollationIterator) other;
        if (this.checkDir != o.checkDir) {
            return false;
        }
        if (this.checkDir == 0) {
            if ((this.seq == this.rawSeq) != (o.seq == o.rawSeq)) {
                return false;
            }
        }
        if (this.checkDir != 0 || this.seq == this.rawSeq) {
            if (this.pos - 0 == o.pos - 0) {
                z = true;
            }
            return z;
        }
        if (this.segmentStart - 0 == o.segmentStart - 0 && this.pos - this.start == o.pos - o.start) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return 42;
    }

    public void resetToOffset(int newOffset) {
        reset();
        this.seq = this.rawSeq;
        int i = 0 + newOffset;
        this.pos = i;
        this.segmentStart = i;
        this.start = i;
        this.limit = this.rawLimit;
        this.checkDir = 1;
    }

    public int getOffset() {
        if (this.checkDir != 0 || this.seq == this.rawSeq) {
            return this.pos + 0;
        }
        if (this.pos == this.start) {
            return this.segmentStart + 0;
        }
        return this.segmentLimit + 0;
    }

    public void setText(boolean numeric, CharSequence s, int p) {
        super.setText(numeric, s, p);
        this.rawSeq = s;
        this.segmentStart = p;
        int length = s.length();
        this.limit = length;
        this.rawLimit = length;
        this.checkDir = 1;
    }

    public int nextCodePoint() {
        int i;
        char c;
        while (this.checkDir <= 0) {
            if (this.checkDir == 0 && this.pos != this.limit) {
                CharSequence charSequence = this.seq;
                i = this.pos;
                this.pos = i + 1;
                c = charSequence.charAt(i);
                break;
            }
            switchToForward();
        }
        if (this.pos == this.limit) {
            return -1;
        }
        c = this.seq;
        i = this.pos;
        this.pos = i + 1;
        c = c.charAt(i);
        if (CollationFCD.hasTccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || (this.pos != this.limit && CollationFCD.hasLccc(this.seq.charAt(this.pos))))) {
            this.pos--;
            nextSegment();
            CharSequence charSequence2 = this.seq;
            int i2 = this.pos;
            this.pos = i2 + 1;
            c = charSequence2.charAt(i2);
        }
        if (Character.isHighSurrogate(c) && this.pos != this.limit) {
            char charAt = this.seq.charAt(this.pos);
            char trail = charAt;
            if (Character.isLowSurrogate(charAt)) {
                this.pos++;
                return Character.toCodePoint(c, trail);
            }
        }
        return c;
    }

    public int previousCodePoint() {
        int i;
        char c;
        while (this.checkDir >= 0) {
            if (this.checkDir == 0 && this.pos != this.start) {
                CharSequence charSequence = this.seq;
                i = this.pos - 1;
                this.pos = i;
                c = charSequence.charAt(i);
                break;
            }
            switchToBackward();
        }
        if (this.pos == this.start) {
            return -1;
        }
        c = this.seq;
        i = this.pos - 1;
        this.pos = i;
        c = c.charAt(i);
        if (CollationFCD.hasLccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || (this.pos != this.start && CollationFCD.hasTccc(this.seq.charAt(this.pos - 1))))) {
            this.pos++;
            previousSegment();
            CharSequence charSequence2 = this.seq;
            int i2 = this.pos - 1;
            this.pos = i2;
            c = charSequence2.charAt(i2);
        }
        if (Character.isLowSurrogate(c) && this.pos != this.start) {
            char charAt = this.seq.charAt(this.pos - 1);
            char lead = charAt;
            if (Character.isHighSurrogate(charAt)) {
                this.pos--;
                return Character.toCodePoint(lead, c);
            }
        }
        return c;
    }

    protected long handleNextCE32() {
        int i;
        char c;
        while (this.checkDir <= 0) {
            if (this.checkDir == 0 && this.pos != this.limit) {
                CharSequence charSequence = this.seq;
                i = this.pos;
                this.pos = i + 1;
                c = charSequence.charAt(i);
                break;
            }
            switchToForward();
        }
        if (this.pos == this.limit) {
            return -4294967104L;
        }
        c = this.seq;
        i = this.pos;
        this.pos = i + 1;
        c = c.charAt(i);
        if (CollationFCD.hasTccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || (this.pos != this.limit && CollationFCD.hasLccc(this.seq.charAt(this.pos))))) {
            this.pos--;
            nextSegment();
            CharSequence charSequence2 = this.seq;
            int i2 = this.pos;
            this.pos = i2 + 1;
            c = charSequence2.charAt(i2);
        }
        return makeCodePointAndCE32Pair(c, this.trie.getFromU16SingleLead(c));
    }

    protected void forwardNumCodePoints(int num) {
        while (num > 0 && nextCodePoint() >= 0) {
            num--;
        }
    }

    protected void backwardNumCodePoints(int num) {
        while (num > 0 && previousCodePoint() >= 0) {
            num--;
        }
    }

    private void switchToForward() {
        int i;
        if (this.checkDir < 0) {
            i = this.pos;
            this.segmentStart = i;
            this.start = i;
            if (this.pos == this.segmentLimit) {
                this.limit = this.rawLimit;
                this.checkDir = 1;
                return;
            }
            this.checkDir = 0;
            return;
        }
        if (this.seq != this.rawSeq) {
            this.seq = this.rawSeq;
            i = this.segmentLimit;
            this.segmentStart = i;
            this.start = i;
            this.pos = i;
        }
        this.limit = this.rawLimit;
        this.checkDir = 1;
    }

    /* JADX WARNING: Missing block: B:10:0x002f, code skipped:
            r3 = r2;
     */
    /* JADX WARNING: Missing block: B:11:0x0032, code skipped:
            if (r2 != r9.rawLimit) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:12:0x0035, code skipped:
            r4 = java.lang.Character.codePointAt(r9.seq, r2);
            r2 = r2 + java.lang.Character.charCount(r4);
     */
    /* JADX WARNING: Missing block: B:13:0x0048, code skipped:
            if (r9.nfcImpl.getFCD16(r4) > 255) goto L_0x002f;
     */
    /* JADX WARNING: Missing block: B:14:0x004a, code skipped:
            normalize(r9.pos, r3);
            r9.pos = r9.start;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void nextSegment() {
        int p = this.pos;
        int prevCC = 0;
        while (true) {
            int q = p;
            int c = Character.codePointAt(this.seq, p);
            p += Character.charCount(c);
            int fcd16 = this.nfcImpl.getFCD16(c);
            int leadCC = fcd16 >> 8;
            if (leadCC == 0 && q != this.pos) {
                this.segmentLimit = q;
                this.limit = q;
                break;
            } else if (leadCC == 0 || (prevCC <= leadCC && !CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                prevCC = fcd16 & 255;
                if (p == this.rawLimit || prevCC == 0) {
                    this.segmentLimit = p;
                    this.limit = p;
                }
            }
        }
        this.checkDir = 0;
    }

    private void switchToBackward() {
        int i;
        if (this.checkDir > 0) {
            i = this.pos;
            this.segmentLimit = i;
            this.limit = i;
            if (this.pos == this.segmentStart) {
                this.start = 0;
                this.checkDir = -1;
                return;
            }
            this.checkDir = 0;
            return;
        }
        if (this.seq != this.rawSeq) {
            this.seq = this.rawSeq;
            i = this.segmentStart;
            this.segmentLimit = i;
            this.limit = i;
            this.pos = i;
        }
        this.start = 0;
        this.checkDir = -1;
    }

    /* JADX WARNING: Missing block: B:11:0x0031, code skipped:
            r3 = r2;
     */
    /* JADX WARNING: Missing block: B:12:0x0034, code skipped:
            if (r5 <= 255) goto L_0x004d;
     */
    /* JADX WARNING: Missing block: B:13:0x0036, code skipped:
            if (r2 != 0) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:14:0x0039, code skipped:
            r4 = java.lang.Character.codePointBefore(r8.seq, r2);
            r2 = r2 - java.lang.Character.charCount(r4);
            r7 = r8.nfcImpl.getFCD16(r4);
            r5 = r7;
     */
    /* JADX WARNING: Missing block: B:15:0x004b, code skipped:
            if (r7 != 0) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:16:0x004d, code skipped:
            normalize(r3, r8.pos);
            r8.pos = r8.limit;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void previousSegment() {
        int p = this.pos;
        int nextCC = 0;
        while (true) {
            int q = p;
            int c = Character.codePointBefore(this.seq, p);
            p -= Character.charCount(c);
            int fcd16 = this.nfcImpl.getFCD16(c);
            int trailCC = fcd16 & 255;
            if (trailCC == 0 && q != this.pos) {
                this.segmentStart = q;
                this.start = q;
                break;
            } else if (trailCC == 0 || ((nextCC == 0 || trailCC <= nextCC) && !CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                nextCC = fcd16 >> 8;
                if (p == 0 || nextCC == 0) {
                    this.segmentStart = p;
                    this.start = p;
                }
            }
        }
        this.checkDir = 0;
    }

    private void normalize(int from, int to) {
        if (this.normalized == null) {
            this.normalized = new StringBuilder();
        }
        this.nfcImpl.decompose(this.rawSeq, from, to, this.normalized, to - from);
        this.segmentStart = from;
        this.segmentLimit = to;
        this.seq = this.normalized;
        this.start = 0;
        this.limit = this.start + this.normalized.length();
    }
}
