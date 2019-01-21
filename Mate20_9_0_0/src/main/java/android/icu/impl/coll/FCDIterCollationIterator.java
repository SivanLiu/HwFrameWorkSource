package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.text.UCharacterIterator;

public final class FCDIterCollationIterator extends IterCollationIterator {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private int limit;
    private final Normalizer2Impl nfcImpl;
    private StringBuilder normalized;
    private int pos;
    private StringBuilder s;
    private int start;
    private State state = State.ITER_CHECK_FWD;

    private enum State {
        ITER_CHECK_FWD,
        ITER_CHECK_BWD,
        ITER_IN_FCD_SEGMENT,
        IN_NORM_ITER_AT_LIMIT,
        IN_NORM_ITER_AT_START
    }

    public FCDIterCollationIterator(CollationData data, boolean numeric, UCharacterIterator ui, int startIndex) {
        super(data, numeric, ui);
        this.start = startIndex;
        this.nfcImpl = data.nfcImpl;
    }

    public void resetToOffset(int newOffset) {
        super.resetToOffset(newOffset);
        this.start = newOffset;
        this.state = State.ITER_CHECK_FWD;
    }

    public int getOffset() {
        if (this.state.compareTo(State.ITER_CHECK_BWD) <= 0) {
            return this.iter.getIndex();
        }
        if (this.state == State.ITER_IN_FCD_SEGMENT) {
            return this.pos;
        }
        if (this.pos == 0) {
            return this.start;
        }
        return this.limit;
    }

    public int nextCodePoint() {
        int c;
        while (true) {
            if (this.state == State.ITER_CHECK_FWD) {
                c = this.iter.next();
                if (c < 0) {
                    return c;
                }
                if (CollationFCD.hasTccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || CollationFCD.hasLccc(this.iter.current()))) {
                    this.iter.previous();
                    if (!nextSegment()) {
                        return -1;
                    }
                }
            } else if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.limit) {
                c = this.iter.nextCodePoint();
                this.pos += Character.charCount(c);
                return c;
            } else if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) < 0 || this.pos == this.normalized.length()) {
                switchToForward();
            } else {
                c = this.normalized.codePointAt(this.pos);
                this.pos += Character.charCount(c);
                return c;
            }
        }
        if (CollationIterator.isLeadSurrogate(c)) {
            int trail = this.iter.next();
            if (CollationIterator.isTrailSurrogate(trail)) {
                return Character.toCodePoint((char) c, (char) trail);
            }
            if (trail >= 0) {
                this.iter.previous();
            }
        }
        return c;
    }

    public int previousCodePoint() {
        int c;
        while (true) {
            if (this.state == State.ITER_CHECK_BWD) {
                c = this.iter.previous();
                if (c >= 0) {
                    if (!CollationFCD.hasLccc(c)) {
                        break;
                    }
                    int prev = -1;
                    if (!CollationFCD.maybeTibetanCompositeVowel(c)) {
                        int previous = this.iter.previous();
                        prev = previous;
                        if (!CollationFCD.hasTccc(previous)) {
                            if (CollationIterator.isTrailSurrogate(c)) {
                                if (prev < 0) {
                                    prev = this.iter.previous();
                                }
                                if (CollationIterator.isLeadSurrogate(prev)) {
                                    return Character.toCodePoint((char) prev, (char) c);
                                }
                            }
                            if (prev >= 0) {
                                this.iter.next();
                            }
                        }
                    }
                    this.iter.next();
                    if (prev >= 0) {
                        this.iter.next();
                    }
                    if (!previousSegment()) {
                        return -1;
                    }
                } else {
                    this.pos = 0;
                    this.start = 0;
                    this.state = State.ITER_IN_FCD_SEGMENT;
                    return -1;
                }
            } else if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.start) {
                c = this.iter.previousCodePoint();
                this.pos -= Character.charCount(c);
                return c;
            } else if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) < 0 || this.pos == 0) {
                switchToBackward();
            } else {
                c = this.normalized.codePointBefore(this.pos);
                this.pos -= Character.charCount(c);
                return c;
            }
        }
        return c;
    }

    protected long handleNextCE32() {
        int c;
        while (true) {
            if (this.state != State.ITER_CHECK_FWD) {
                if (this.state != State.ITER_IN_FCD_SEGMENT || this.pos == this.limit) {
                    if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != this.normalized.length()) {
                        StringBuilder stringBuilder = this.normalized;
                        int i = this.pos;
                        this.pos = i + 1;
                        c = stringBuilder.charAt(i);
                        break;
                    }
                    switchToForward();
                } else {
                    c = this.iter.next();
                    this.pos++;
                    break;
                }
            }
            c = this.iter.next();
            if (c >= 0) {
                if (!CollationFCD.hasTccc(c) || (!CollationFCD.maybeTibetanCompositeVowel(c) && !CollationFCD.hasLccc(this.iter.current()))) {
                    break;
                }
                this.iter.previous();
                if (!nextSegment()) {
                    return 192;
                }
            } else {
                return -4294967104L;
            }
        }
        return makeCodePointAndCE32Pair(c, this.trie.getFromU16SingleLead((char) c));
    }

    protected char handleGetTrailSurrogate() {
        if (this.state.compareTo(State.ITER_IN_FCD_SEGMENT) <= 0) {
            int trail = this.iter.next();
            if (CollationIterator.isTrailSurrogate(trail)) {
                if (this.state == State.ITER_IN_FCD_SEGMENT) {
                    this.pos++;
                }
            } else if (trail >= 0) {
                this.iter.previous();
            }
            return (char) trail;
        }
        char charAt = this.normalized.charAt(this.pos);
        char trail2 = charAt;
        if (Character.isLowSurrogate(charAt)) {
            this.pos++;
        }
        return trail2;
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
        if (this.state == State.ITER_CHECK_BWD) {
            int index = this.iter.getIndex();
            this.pos = index;
            this.start = index;
            if (this.pos == this.limit) {
                this.state = State.ITER_CHECK_FWD;
                return;
            } else {
                this.state = State.ITER_IN_FCD_SEGMENT;
                return;
            }
        }
        if (this.state != State.ITER_IN_FCD_SEGMENT) {
            if (this.state == State.IN_NORM_ITER_AT_START) {
                this.iter.moveIndex(this.limit - this.start);
            }
            this.start = this.limit;
        }
        this.state = State.ITER_CHECK_FWD;
    }

    /* JADX WARNING: Missing block: B:17:0x004d, code skipped:
            r2 = r8.iter.nextCodePoint();
     */
    /* JADX WARNING: Missing block: B:18:0x0053, code skipped:
            if (r2 >= 0) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:20:0x005e, code skipped:
            if (r8.nfcImpl.getFCD16(r2) > 255) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:21:0x0060, code skipped:
            r8.iter.previousCodePoint();
     */
    /* JADX WARNING: Missing block: B:22:0x0066, code skipped:
            normalize(r8.s);
            r8.start = r8.pos;
            r8.limit = r8.pos + r8.s.length();
            r8.state = android.icu.impl.coll.FCDIterCollationIterator.State.IN_NORM_ITER_AT_LIMIT;
            r8.pos = 0;
     */
    /* JADX WARNING: Missing block: B:23:0x0080, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:24:0x0081, code skipped:
            r8.s.appendCodePoint(r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean nextSegment() {
        this.pos = this.iter.getIndex();
        if (this.s == null) {
            this.s = new StringBuilder();
        } else {
            this.s.setLength(0);
        }
        int prevCC = 0;
        while (true) {
            int c = this.iter.nextCodePoint();
            if (c >= 0) {
                int fcd16 = this.nfcImpl.getFCD16(c);
                int leadCC = fcd16 >> 8;
                if (leadCC == 0 && this.s.length() != 0) {
                    this.iter.previousCodePoint();
                    break;
                }
                this.s.appendCodePoint(c);
                if (leadCC == 0 || (prevCC <= leadCC && !CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                    prevCC = fcd16 & 255;
                    if (prevCC == 0) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        this.limit = this.pos + this.s.length();
        this.iter.moveIndex(-this.s.length());
        this.state = State.ITER_IN_FCD_SEGMENT;
        return true;
    }

    private void switchToBackward() {
        if (this.state == State.ITER_CHECK_FWD) {
            int index = this.iter.getIndex();
            this.pos = index;
            this.limit = index;
            if (this.pos == this.start) {
                this.state = State.ITER_CHECK_BWD;
                return;
            } else {
                this.state = State.ITER_IN_FCD_SEGMENT;
                return;
            }
        }
        if (this.state != State.ITER_IN_FCD_SEGMENT) {
            if (this.state == State.IN_NORM_ITER_AT_LIMIT) {
                this.iter.moveIndex(this.start - this.limit);
            }
            this.limit = this.start;
        }
        this.state = State.ITER_CHECK_BWD;
    }

    /* JADX WARNING: Missing block: B:18:0x0053, code skipped:
            if (r3 <= 255) goto L_0x0072;
     */
    /* JADX WARNING: Missing block: B:19:0x0055, code skipped:
            r1 = r7.iter.previousCodePoint();
     */
    /* JADX WARNING: Missing block: B:20:0x005b, code skipped:
            if (r1 >= 0) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:21:0x005e, code skipped:
            r3 = r7.nfcImpl.getFCD16(r1);
     */
    /* JADX WARNING: Missing block: B:22:0x0064, code skipped:
            if (r3 != 0) goto L_0x006c;
     */
    /* JADX WARNING: Missing block: B:23:0x0066, code skipped:
            r7.iter.nextCodePoint();
     */
    /* JADX WARNING: Missing block: B:24:0x006c, code skipped:
            r7.s.appendCodePoint(r1);
     */
    /* JADX WARNING: Missing block: B:25:0x0072, code skipped:
            r7.s.reverse();
            normalize(r7.s);
            r7.limit = r7.pos;
            r7.start = r7.pos - r7.s.length();
            r7.state = android.icu.impl.coll.FCDIterCollationIterator.State.IN_NORM_ITER_AT_START;
            r7.pos = r7.normalized.length();
     */
    /* JADX WARNING: Missing block: B:26:0x0097, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean previousSegment() {
        this.pos = this.iter.getIndex();
        int nextCC = 0;
        if (this.s == null) {
            this.s = new StringBuilder();
        } else {
            this.s.setLength(0);
        }
        while (true) {
            int nextCC2 = nextCC;
            nextCC = this.iter.previousCodePoint();
            if (nextCC >= 0) {
                int fcd16 = this.nfcImpl.getFCD16(nextCC);
                int trailCC = fcd16 & 255;
                if (trailCC == 0 && this.s.length() != 0) {
                    this.iter.nextCodePoint();
                    break;
                }
                this.s.appendCodePoint(nextCC);
                if (trailCC == 0 || ((nextCC2 == 0 || trailCC <= nextCC2) && !CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                    nextCC2 = fcd16 >> 8;
                    if (nextCC2 == 0) {
                        break;
                    }
                    nextCC = nextCC2;
                }
            } else {
                break;
            }
        }
        this.start = this.pos - this.s.length();
        this.iter.moveIndex(this.s.length());
        this.state = State.ITER_IN_FCD_SEGMENT;
        return true;
    }

    private void normalize(CharSequence s) {
        if (this.normalized == null) {
            this.normalized = new StringBuilder();
        }
        this.nfcImpl.decompose(s, this.normalized);
    }
}
