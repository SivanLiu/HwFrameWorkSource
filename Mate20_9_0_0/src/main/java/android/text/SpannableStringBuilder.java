package android.text;

import android.graphics.BaseCanvas;
import android.graphics.Paint;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import libcore.util.EmptyArray;

public class SpannableStringBuilder implements CharSequence, GetChars, Spannable, Editable, Appendable, GraphicsOperations {
    private static final int END_MASK = 15;
    private static final int MARK = 1;
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final int PARAGRAPH = 3;
    private static final int POINT = 2;
    private static final int SPAN_ADDED = 2048;
    private static final int SPAN_END_AT_END = 32768;
    private static final int SPAN_END_AT_START = 16384;
    private static final int SPAN_START_AT_END = 8192;
    private static final int SPAN_START_AT_START = 4096;
    private static final int SPAN_START_END_MASK = 61440;
    private static final int START_MASK = 240;
    private static final int START_SHIFT = 4;
    private static final String TAG = "SpannableStringBuilder";
    @GuardedBy("sCachedIntBuffer")
    private static final int[][] sCachedIntBuffer = ((int[][]) Array.newInstance(int.class, new int[]{6, 0}));
    private InputFilter[] mFilters;
    private int mGapLength;
    private int mGapStart;
    private IdentityHashMap<Object, Integer> mIndexOfSpan;
    private int mLowWaterMark;
    private int mSpanCount;
    private int[] mSpanEnds;
    private int[] mSpanFlags;
    private int mSpanInsertCount;
    private int[] mSpanMax;
    private int[] mSpanOrder;
    private int[] mSpanStarts;
    private Object[] mSpans;
    private char[] mText;
    private int mTextWatcherDepth;

    public SpannableStringBuilder() {
        this("");
    }

    public SpannableStringBuilder(CharSequence text) {
        this(text, 0, text.length());
    }

    public SpannableStringBuilder(CharSequence text, int start, int end) {
        CharSequence charSequence = text;
        int i = start;
        int i2 = end;
        this.mFilters = NO_FILTERS;
        int srclen = i2 - i;
        if (srclen >= 0) {
            this.mText = ArrayUtils.newUnpaddedCharArray(GrowingArrayUtils.growSize(srclen));
            this.mGapStart = srclen;
            this.mGapLength = this.mText.length - srclen;
            int i3 = 0;
            TextUtils.getChars(charSequence, i, i2, this.mText, 0);
            this.mSpanCount = 0;
            this.mSpanInsertCount = 0;
            this.mSpans = EmptyArray.OBJECT;
            this.mSpanStarts = EmptyArray.INT;
            this.mSpanEnds = EmptyArray.INT;
            this.mSpanFlags = EmptyArray.INT;
            this.mSpanMax = EmptyArray.INT;
            this.mSpanOrder = EmptyArray.INT;
            if (charSequence instanceof Spanned) {
                Spanned sp = (Spanned) charSequence;
                Object[] spans = sp.getSpans(i, i2, Object.class);
                while (true) {
                    int i4 = i3;
                    if (i4 < spans.length) {
                        if (!(spans[i4] instanceof NoCopySpan)) {
                            int en;
                            int st = sp.getSpanStart(spans[i4]) - i;
                            i3 = sp.getSpanEnd(spans[i4]) - i;
                            int fl = sp.getSpanFlags(spans[i4]);
                            if (st < 0) {
                                st = 0;
                            }
                            if (st > i2 - i) {
                                st = i2 - i;
                            }
                            int st2 = st;
                            if (i3 < 0) {
                                i3 = 0;
                            }
                            if (i3 > i2 - i) {
                                en = i2 - i;
                            } else {
                                en = i3;
                            }
                            setSpan(false, spans[i4], st2, en, fl, false);
                        }
                        i3 = i4 + 1;
                    } else {
                        restoreInvariants();
                        return;
                    }
                }
            }
            return;
        }
        throw new StringIndexOutOfBoundsException();
    }

    public static SpannableStringBuilder valueOf(CharSequence source) {
        if (source instanceof SpannableStringBuilder) {
            return (SpannableStringBuilder) source;
        }
        return new SpannableStringBuilder(source);
    }

    public char charAt(int where) {
        int len = length();
        StringBuilder stringBuilder;
        if (where < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("charAt: ");
            stringBuilder.append(where);
            stringBuilder.append(" < 0");
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (where >= len) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("charAt: ");
            stringBuilder.append(where);
            stringBuilder.append(" >= length ");
            stringBuilder.append(len);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (where >= this.mGapStart) {
            return this.mText[this.mGapLength + where];
        } else {
            return this.mText[where];
        }
    }

    public int length() {
        return this.mText.length - this.mGapLength;
    }

    private void resizeFor(int size) {
        int oldLength = this.mText.length;
        if (size + 1 > oldLength) {
            char[] newText = ArrayUtils.newUnpaddedCharArray(GrowingArrayUtils.growSize(size));
            int i = 0;
            System.arraycopy(this.mText, 0, newText, 0, this.mGapStart);
            int newLength = newText.length;
            int delta = newLength - oldLength;
            int after = oldLength - (this.mGapStart + this.mGapLength);
            System.arraycopy(this.mText, oldLength - after, newText, newLength - after, after);
            this.mText = newText;
            this.mGapLength += delta;
            if (this.mGapLength < 1) {
                new Exception("mGapLength < 1").printStackTrace();
            }
            if (this.mSpanCount != 0) {
                while (i < this.mSpanCount) {
                    int[] iArr;
                    if (this.mSpanStarts[i] > this.mGapStart) {
                        iArr = this.mSpanStarts;
                        iArr[i] = iArr[i] + delta;
                    }
                    if (this.mSpanEnds[i] > this.mGapStart) {
                        iArr = this.mSpanEnds;
                        iArr[i] = iArr[i] + delta;
                    }
                    i++;
                }
                calcMax(treeRoot());
            }
        }
    }

    private void moveGapTo(int where) {
        if (where != this.mGapStart) {
            int overlap;
            int i = 0;
            boolean atEnd = where == length();
            if (where < this.mGapStart) {
                overlap = this.mGapStart - where;
                System.arraycopy(this.mText, where, this.mText, (this.mGapStart + this.mGapLength) - overlap, overlap);
            } else {
                overlap = where - this.mGapStart;
                System.arraycopy(this.mText, (this.mGapLength + where) - overlap, this.mText, this.mGapStart, overlap);
            }
            if (this.mSpanCount != 0) {
                while (i < this.mSpanCount) {
                    int flag;
                    overlap = this.mSpanStarts[i];
                    int end = this.mSpanEnds[i];
                    if (overlap > this.mGapStart) {
                        overlap -= this.mGapLength;
                    }
                    if (overlap > where) {
                        overlap += this.mGapLength;
                    } else if (overlap == where) {
                        flag = (this.mSpanFlags[i] & 240) >> 4;
                        if (flag == 2 || (atEnd && flag == 3)) {
                            overlap += this.mGapLength;
                        }
                    }
                    if (end > this.mGapStart) {
                        end -= this.mGapLength;
                    }
                    if (end > where) {
                        end += this.mGapLength;
                    } else if (end == where) {
                        flag = this.mSpanFlags[i] & 15;
                        if (flag == 2 || (atEnd && flag == 3)) {
                            end += this.mGapLength;
                        }
                    }
                    this.mSpanStarts[i] = overlap;
                    this.mSpanEnds[i] = end;
                    i++;
                }
                calcMax(treeRoot());
            }
            this.mGapStart = where;
        }
    }

    public SpannableStringBuilder insert(int where, CharSequence tb, int start, int end) {
        return replace(where, where, tb, start, end);
    }

    public SpannableStringBuilder insert(int where, CharSequence tb) {
        return replace(where, where, tb, 0, tb.length());
    }

    public SpannableStringBuilder delete(int start, int end) {
        SpannableStringBuilder ret = replace(start, end, (CharSequence) "", 0, 0);
        if (this.mGapLength > 2 * length()) {
            resizeFor(length());
        }
        return ret;
    }

    public void clear() {
        replace(0, length(), (CharSequence) "", 0, 0);
        this.mSpanInsertCount = 0;
    }

    public void clearSpans() {
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            Object what = this.mSpans[i];
            int ostart = this.mSpanStarts[i];
            int oend = this.mSpanEnds[i];
            if (ostart > this.mGapStart) {
                ostart -= this.mGapLength;
            }
            if (oend > this.mGapStart) {
                oend -= this.mGapLength;
            }
            this.mSpanCount = i;
            this.mSpans[i] = null;
            sendSpanRemoved(what, ostart, oend);
        }
        if (this.mIndexOfSpan != null) {
            this.mIndexOfSpan.clear();
        }
        this.mSpanInsertCount = 0;
    }

    public SpannableStringBuilder append(CharSequence text) {
        int length = length();
        return replace(length, length, text, 0, text.length());
    }

    public SpannableStringBuilder append(CharSequence text, Object what, int flags) {
        int start = length();
        append(text);
        setSpan(what, start, length(), flags);
        return this;
    }

    public SpannableStringBuilder append(CharSequence text, int start, int end) {
        int length = length();
        return replace(length, length, text, start, end);
    }

    public SpannableStringBuilder append(char text) {
        return append(String.valueOf(text));
    }

    private boolean removeSpansForChange(int start, int end, boolean textIsRemoved, int i) {
        boolean z = true;
        if ((i & 1) != 0 && resolveGap(this.mSpanMax[i]) >= start && removeSpansForChange(start, end, textIsRemoved, leftChild(i))) {
            return true;
        }
        if (i >= this.mSpanCount) {
            return false;
        }
        if ((this.mSpanFlags[i] & 33) != 33 || this.mSpanStarts[i] < start || this.mSpanStarts[i] >= this.mGapStart + this.mGapLength || this.mSpanEnds[i] < start || this.mSpanEnds[i] >= this.mGapStart + this.mGapLength || (!textIsRemoved && this.mSpanStarts[i] <= start && this.mSpanEnds[i] >= this.mGapStart)) {
            if (resolveGap(this.mSpanStarts[i]) > end || (i & 1) == 0 || !removeSpansForChange(start, end, textIsRemoved, rightChild(i))) {
                z = false;
            }
            return z;
        }
        this.mIndexOfSpan.remove(this.mSpans[i]);
        removeSpan(i, 0);
        return true;
    }

    private void change(int start, int end, CharSequence cs, int csStart, int csEnd) {
        int spanStart;
        int ost;
        int clen;
        int i;
        int spanEnd;
        int spanEnd2;
        int spanStart2;
        CharSequence charSequence;
        int i2 = start;
        int i3 = end;
        CharSequence charSequence2 = cs;
        int i4 = csStart;
        int i5 = csEnd;
        int replacedLength = i3 - i2;
        int replacementLength = i5 - i4;
        int nbNewChars = replacementLength - replacedLength;
        int i6 = this.mSpanCount - 1;
        boolean changed = false;
        while (true) {
            int i7 = i6;
            if (i7 < 0) {
                break;
            }
            spanStart = this.mSpanStarts[i7];
            if (spanStart > this.mGapStart) {
                spanStart -= this.mGapLength;
            }
            i6 = this.mSpanEnds[i7];
            if (i6 > this.mGapStart) {
                i6 -= this.mGapLength;
            }
            if ((this.mSpanFlags[i7] & 51) == 51) {
                ost = spanStart;
                int oen = i6;
                clen = length();
                if (spanStart > i2 && spanStart <= i3) {
                    spanStart = i3;
                    while (spanStart < clen && (spanStart <= i3 || charAt(spanStart - 1) != 10)) {
                        spanStart++;
                    }
                }
                i = spanStart;
                if (i6 <= i2 || i6 > i3) {
                    spanEnd = i6;
                } else {
                    spanStart = i3;
                    while (spanStart < clen && (spanStart <= i3 || charAt(spanStart - 1) != 10)) {
                        spanStart++;
                    }
                    spanEnd = spanStart;
                }
                if (i == ost && spanEnd == oen) {
                    i6 = spanEnd;
                    spanStart = i;
                } else {
                    spanEnd2 = spanEnd;
                    spanStart2 = i;
                    setSpan(false, this.mSpans[i7], i, spanEnd2, this.mSpanFlags[i7], 1);
                    changed = true;
                    spanStart = spanStart2;
                    i6 = spanEnd2;
                }
            }
            spanEnd = 0;
            if (spanStart == i2) {
                spanEnd = 0 | 4096;
            } else if (spanStart == i3 + nbNewChars) {
                spanEnd = 0 | 8192;
            }
            if (i6 == i2) {
                spanEnd |= 16384;
            } else if (i6 == i3 + nbNewChars) {
                spanEnd |= 32768;
            }
            int[] iArr = this.mSpanFlags;
            iArr[i7] = iArr[i7] | spanEnd;
            i6 = i7 - 1;
        }
        if (changed) {
            restoreInvariants();
        }
        moveGapTo(i3);
        if (nbNewChars >= this.mGapLength) {
            resizeFor((this.mText.length + nbNewChars) - this.mGapLength);
        }
        spanStart2 = 0;
        boolean textIsRemoved = replacementLength == 0;
        if (replacedLength > 0) {
            while (this.mSpanCount > 0 && removeSpansForChange(i2, i3, textIsRemoved, treeRoot())) {
            }
        }
        this.mGapStart += nbNewChars;
        this.mGapLength -= nbNewChars;
        if (this.mGapLength < 1) {
            new Exception("mGapLength < 1").printStackTrace();
        }
        TextUtils.getChars(charSequence2, i4, i5, this.mText, i2);
        if (replacedLength > 0) {
            boolean atEnd = this.mGapStart + this.mGapLength == this.mText.length;
            spanStart = 0;
            while (true) {
                int i8 = spanStart;
                if (i8 >= this.mSpanCount) {
                    break;
                }
                this.mSpanStarts[i8] = updatedIntervalBound(this.mSpanStarts[i8], i2, nbNewChars, (this.mSpanFlags[i8] & 240) >> 4, atEnd, textIsRemoved);
                i = i8;
                boolean textIsRemoved2 = textIsRemoved;
                clen = i5;
                spanEnd = i4;
                charSequence = charSequence2;
                this.mSpanEnds[i] = updatedIntervalBound(this.mSpanEnds[i8], i2, nbNewChars, this.mSpanFlags[i8] & 15, atEnd, textIsRemoved2);
                spanStart = i + 1;
                i4 = spanEnd;
                i5 = clen;
                charSequence2 = charSequence;
                textIsRemoved = textIsRemoved2;
                i3 = end;
            }
            spanEnd2 = nbNewChars;
            clen = i5;
            spanEnd = i4;
            charSequence = charSequence2;
            restoreInvariants();
        } else {
            spanEnd2 = nbNewChars;
            clen = i5;
            spanEnd = i4;
            charSequence = charSequence2;
        }
        if (charSequence instanceof Spanned) {
            Spanned sp = (Spanned) charSequence;
            Object[] spans = sp.getSpans(spanEnd, clen, Object.class);
            while (true) {
                nbNewChars = spanStart2;
                if (nbNewChars < spans.length) {
                    spanStart = sp.getSpanStart(spans[nbNewChars]);
                    i6 = sp.getSpanEnd(spans[nbNewChars]);
                    if (spanStart < spanEnd) {
                        spanStart = spanEnd;
                    }
                    i5 = spanStart;
                    if (i6 > clen) {
                        i6 = clen;
                    }
                    i4 = i6;
                    if (getSpanStart(spans[nbNewChars]) < 0) {
                        ost = sp.getSpanFlags(spans[nbNewChars]) | 2048;
                        setSpan(false, spans[nbNewChars], (i5 - spanEnd) + i2, (i4 - spanEnd) + i2, ost, 0);
                    }
                    spanStart2 = nbNewChars + 1;
                    spanEnd = csStart;
                    clen = csEnd;
                } else {
                    restoreInvariants();
                    return;
                }
            }
        }
    }

    private int updatedIntervalBound(int offset, int start, int nbNewChars, int flag, boolean atEnd, boolean textIsRemoved) {
        if (offset >= start && offset < this.mGapStart + this.mGapLength) {
            if (flag == 2) {
                if (textIsRemoved || offset > start) {
                    return this.mGapStart + this.mGapLength;
                }
            } else if (flag == 3) {
                if (atEnd) {
                    return this.mGapStart + this.mGapLength;
                }
            } else if (textIsRemoved || offset < this.mGapStart - nbNewChars) {
                return start;
            } else {
                return this.mGapStart;
            }
        }
        return offset;
    }

    private void removeSpan(int i, int flags) {
        Object object = this.mSpans[i];
        int start = this.mSpanStarts[i];
        int end = this.mSpanEnds[i];
        if (start > this.mGapStart) {
            start -= this.mGapLength;
        }
        if (end > this.mGapStart) {
            end -= this.mGapLength;
        }
        int count = this.mSpanCount - (i + 1);
        System.arraycopy(this.mSpans, i + 1, this.mSpans, i, count);
        System.arraycopy(this.mSpanStarts, i + 1, this.mSpanStarts, i, count);
        System.arraycopy(this.mSpanEnds, i + 1, this.mSpanEnds, i, count);
        System.arraycopy(this.mSpanFlags, i + 1, this.mSpanFlags, i, count);
        System.arraycopy(this.mSpanOrder, i + 1, this.mSpanOrder, i, count);
        this.mSpanCount--;
        invalidateIndex(i);
        this.mSpans[this.mSpanCount] = null;
        restoreInvariants();
        if ((flags & 512) == 0) {
            sendSpanRemoved(object, start, end);
        }
    }

    public SpannableStringBuilder replace(int start, int end, CharSequence tb) {
        return replace(start, end, tb, 0, tb.length());
    }

    public SpannableStringBuilder replace(int start, int end, CharSequence tb, int tbstart, int tbend) {
        int i;
        int i2 = start;
        int i3 = end;
        checkRange("replace", i2, i3);
        int filtercount = this.mFilters.length;
        boolean adjustSelection = false;
        CharSequence tb2 = tb;
        int tbstart2 = tbstart;
        int tbend2 = tbend;
        int i4 = 0;
        while (true) {
            i = i4;
            if (i >= filtercount) {
                break;
            }
            i4 = this.mFilters[i].filter(tb2, tbstart2, tbend2, this, i2, i3);
            if (i4 != 0) {
                CharSequence tb3 = i4;
                tbend2 = i4.length();
                tb2 = tb3;
                tbstart2 = 0;
            }
            i4 = i + 1;
        }
        i = i3 - i2;
        boolean newLen = tbend2 - tbstart2;
        if (i == 0 && !newLen && !hasNonExclusiveExclusiveSpanAt(tb2, tbstart2)) {
            return this;
        }
        int newLen2;
        TextWatcher[] textWatchers = (TextWatcher[]) getSpans(i2, i2 + i, TextWatcher.class);
        sendBeforeTextChanged(textWatchers, i2, i, newLen);
        if (i != 0 && newLen) {
            adjustSelection = true;
        }
        i4 = 0;
        int selectionEnd = 0;
        if (adjustSelection) {
            i4 = Selection.getSelectionStart(this);
            selectionEnd = Selection.getSelectionEnd(this);
        }
        filtercount = selectionEnd;
        CharSequence charSequence = tb2;
        int selectionStart = i4;
        int i5 = tbstart2;
        TextWatcher[] textWatchers2 = textWatchers;
        change(i2, i3, charSequence, i5, tbend2);
        if (adjustSelection) {
            int selectionEnd2;
            boolean changed = false;
            if (selectionStart <= i2 || selectionStart >= i3) {
                newLen2 = newLen;
            } else {
                long diff = (long) (selectionStart - i2);
                selectionStart = Math.min(i2 + Math.toIntExact((((long) newLen) * diff) / ((long) i)), length());
                newLen2 = newLen;
                setSpan(false, Selection.SELECTION_START, selectionStart, selectionStart, 34, 1);
                changed = true;
            }
            if (filtercount <= i2 || filtercount >= i3) {
                selectionEnd2 = filtercount;
            } else {
                long diff2 = (long) (filtercount - i2);
                filtercount = Math.min(i2 + Math.toIntExact((((long) newLen2) * diff2) / ((long) i)), length());
                selectionEnd2 = filtercount;
                setSpan(false, Selection.SELECTION_END, selectionEnd2, filtercount, 34, true);
                changed = true;
            }
            if (changed) {
                restoreInvariants();
            }
            filtercount = selectionEnd2;
        } else {
            newLen2 = newLen;
        }
        sendTextChanged(textWatchers2, i2, i, newLen2);
        sendAfterTextChanged(textWatchers2);
        sendToSpanWatchers(i2, i3, newLen2 - i);
        return this;
    }

    private static boolean hasNonExclusiveExclusiveSpanAt(CharSequence text, int offset) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            for (Object span : spanned.getSpans(offset, offset, Object.class)) {
                if (spanned.getSpanFlags(span) != 33) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendToSpanWatchers(int replaceStart, int replaceEnd, int nbNewChars) {
        int i = replaceStart;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int i4 = i3;
            if (i4 >= this.mSpanCount) {
                break;
            }
            int spanFlags = this.mSpanFlags[i4];
            if ((spanFlags & 2048) == 0) {
                i3 = this.mSpanStarts[i4];
                int spanEnd = this.mSpanEnds[i4];
                if (i3 > this.mGapStart) {
                    i3 -= this.mGapLength;
                }
                int spanStart = i3;
                if (spanEnd > this.mGapStart) {
                    spanEnd -= this.mGapLength;
                }
                int spanEnd2 = spanEnd;
                int newReplaceEnd = replaceEnd + nbNewChars;
                boolean spanChanged = false;
                spanEnd = spanStart;
                if (spanStart > newReplaceEnd) {
                    if (nbNewChars != 0) {
                        spanEnd -= nbNewChars;
                        spanChanged = true;
                    }
                } else if (spanStart >= i && !((spanStart == i && (spanFlags & 4096) == 4096) || (spanStart == newReplaceEnd && (spanFlags & 8192) == 8192))) {
                    spanChanged = true;
                }
                int previousSpanStart = spanEnd;
                spanEnd = spanEnd2;
                if (spanEnd2 > newReplaceEnd) {
                    if (nbNewChars != 0) {
                        spanEnd -= nbNewChars;
                        spanChanged = true;
                    }
                } else if (spanEnd2 >= i && !((spanEnd2 == i && (spanFlags & 16384) == 16384) || (spanEnd2 == newReplaceEnd && (spanFlags & 32768) == 32768))) {
                    spanChanged = true;
                }
                int previousSpanEnd = spanEnd;
                if (spanChanged) {
                    sendSpanChanged(this.mSpans[i4], previousSpanStart, previousSpanEnd, spanStart, spanEnd2);
                }
                int[] iArr = this.mSpanFlags;
                iArr[i4] = iArr[i4] & -61441;
            }
            i3 = i4 + 1;
        }
        while (true) {
            i3 = i2;
            if (i3 < this.mSpanCount) {
                if ((this.mSpanFlags[i3] & 2048) != 0) {
                    int[] iArr2 = this.mSpanFlags;
                    iArr2[i3] = iArr2[i3] & -2049;
                    int spanStart2 = this.mSpanStarts[i3];
                    int spanEnd3 = this.mSpanEnds[i3];
                    if (spanStart2 > this.mGapStart) {
                        spanStart2 -= this.mGapLength;
                    }
                    if (spanEnd3 > this.mGapStart) {
                        spanEnd3 -= this.mGapLength;
                    }
                    sendSpanAdded(this.mSpans[i3], spanStart2, spanEnd3);
                }
                i2 = i3 + 1;
            } else {
                return;
            }
        }
    }

    public void setSpan(Object what, int start, int end, int flags) {
        setSpan(true, what, start, end, flags, true);
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x00cc  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0168  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x016e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setSpan(boolean send, Object what, int start, int end, int flags, boolean enforceParagraph) {
        Object obj = what;
        int i = start;
        int i2 = end;
        int i3 = flags;
        checkRange("setSpan", i, i2);
        int flagsStart = (i3 & 240) >> 4;
        StringBuilder stringBuilder;
        if (!isInvalidParagraph(i, flagsStart)) {
            int flagsEnd = i3 & 15;
            if (isInvalidParagraph(i2, flagsEnd)) {
                if (enforceParagraph) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("PARAGRAPH span must end at paragraph boundary (");
                    stringBuilder.append(i2);
                    stringBuilder.append(" follows ");
                    stringBuilder.append(charAt(i2 - 1));
                    stringBuilder.append(")");
                    throw new RuntimeException(stringBuilder.toString());
                }
            } else if (flagsStart == 2 && flagsEnd == 1 && i == i2) {
                if (send) {
                    Log.e(TAG, "SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length");
                }
            } else {
                int end2;
                int i4;
                int sizeOfMax;
                int nstart = i;
                int nend = i2;
                if (i > this.mGapStart) {
                    i += this.mGapLength;
                } else if (i == this.mGapStart && (flagsStart == 2 || (flagsStart == 3 && i == length()))) {
                    i += this.mGapLength;
                }
                int start2 = i;
                if (i2 > this.mGapStart) {
                    i = this.mGapLength + i2;
                } else if (i2 == this.mGapStart && (flagsEnd == 2 || (flagsEnd == 3 && i2 == length()))) {
                    i = this.mGapLength + i2;
                } else {
                    end2 = i2;
                    if (this.mIndexOfSpan != null) {
                        Integer index = (Integer) this.mIndexOfSpan.get(obj);
                        if (index != null) {
                            int i5 = index.intValue();
                            i = this.mSpanStarts[i5];
                            i2 = this.mSpanEnds[i5];
                            if (i > this.mGapStart) {
                                i -= this.mGapLength;
                            }
                            int ostart = i;
                            if (i2 > this.mGapStart) {
                                i2 -= this.mGapLength;
                            }
                            int oend = i2;
                            this.mSpanStarts[i5] = start2;
                            this.mSpanEnds[i5] = end2;
                            this.mSpanFlags[i5] = i3;
                            if (send) {
                                restoreInvariants();
                                sendSpanChanged(obj, ostart, oend, nstart, nend);
                            } else {
                                i4 = end2;
                            }
                            return;
                        }
                    }
                    i4 = end2;
                    this.mSpans = GrowingArrayUtils.append(this.mSpans, this.mSpanCount, obj);
                    this.mSpanStarts = GrowingArrayUtils.append(this.mSpanStarts, this.mSpanCount, start2);
                    this.mSpanEnds = GrowingArrayUtils.append(this.mSpanEnds, this.mSpanCount, i4);
                    this.mSpanFlags = GrowingArrayUtils.append(this.mSpanFlags, this.mSpanCount, i3);
                    this.mSpanOrder = GrowingArrayUtils.append(this.mSpanOrder, this.mSpanCount, this.mSpanInsertCount);
                    invalidateIndex(this.mSpanCount);
                    this.mSpanCount++;
                    this.mSpanInsertCount++;
                    sizeOfMax = (2 * treeRoot()) + 1;
                    if (this.mSpanMax.length < sizeOfMax) {
                        this.mSpanMax = new int[sizeOfMax];
                    }
                    if (send) {
                        restoreInvariants();
                        sendSpanAdded(obj, nstart, nend);
                    }
                }
                end2 = i;
                if (this.mIndexOfSpan != null) {
                }
                i4 = end2;
                this.mSpans = GrowingArrayUtils.append(this.mSpans, this.mSpanCount, obj);
                this.mSpanStarts = GrowingArrayUtils.append(this.mSpanStarts, this.mSpanCount, start2);
                this.mSpanEnds = GrowingArrayUtils.append(this.mSpanEnds, this.mSpanCount, i4);
                this.mSpanFlags = GrowingArrayUtils.append(this.mSpanFlags, this.mSpanCount, i3);
                this.mSpanOrder = GrowingArrayUtils.append(this.mSpanOrder, this.mSpanCount, this.mSpanInsertCount);
                invalidateIndex(this.mSpanCount);
                this.mSpanCount++;
                this.mSpanInsertCount++;
                sizeOfMax = (2 * treeRoot()) + 1;
                if (this.mSpanMax.length < sizeOfMax) {
                }
                if (send) {
                }
            }
        } else if (enforceParagraph) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("PARAGRAPH span must start at paragraph boundary (");
            stringBuilder.append(i);
            stringBuilder.append(" follows ");
            stringBuilder.append(charAt(i - 1));
            stringBuilder.append(")");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    private boolean isInvalidParagraph(int index, int flag) {
        return (flag != 3 || index == 0 || index == length() || charAt(index - 1) == 10) ? false : true;
    }

    public void removeSpan(Object what) {
        removeSpan(what, 0);
    }

    public void removeSpan(Object what, int flags) {
        if (this.mIndexOfSpan != null) {
            Integer i = (Integer) this.mIndexOfSpan.remove(what);
            if (i != null) {
                removeSpan(i.intValue(), flags);
            }
        }
    }

    private int resolveGap(int i) {
        return i > this.mGapStart ? i - this.mGapLength : i;
    }

    public int getSpanStart(Object what) {
        int i = -1;
        if (this.mIndexOfSpan == null) {
            return -1;
        }
        Integer i2 = (Integer) this.mIndexOfSpan.get(what);
        if (i2 != null) {
            i = resolveGap(this.mSpanStarts[i2.intValue()]);
        }
        return i;
    }

    public int getSpanEnd(Object what) {
        int i = -1;
        if (this.mIndexOfSpan == null) {
            return -1;
        }
        Integer i2 = (Integer) this.mIndexOfSpan.get(what);
        if (i2 != null) {
            i = resolveGap(this.mSpanEnds[i2.intValue()]);
        }
        return i;
    }

    public int getSpanFlags(Object what) {
        int i = 0;
        if (this.mIndexOfSpan == null) {
            return 0;
        }
        Integer i2 = (Integer) this.mIndexOfSpan.get(what);
        if (i2 != null) {
            i = this.mSpanFlags[i2.intValue()];
        }
        return i;
    }

    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind) {
        return getSpans(queryStart, queryEnd, kind, true);
    }

    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind, boolean sortByInsertionOrder) {
        Class<T> cls = kind;
        if (cls == null) {
            return ArrayUtils.emptyArray(Object.class);
        }
        if (this.mSpanCount == 0) {
            return ArrayUtils.emptyArray(kind);
        }
        int i = queryStart;
        int i2 = queryEnd;
        int count = countSpans(i, i2, cls, treeRoot());
        if (count == 0) {
            return ArrayUtils.emptyArray(kind);
        }
        T[] ret = (Object[]) Array.newInstance(cls, count);
        int[] prioSortBuffer = sortByInsertionOrder ? obtain(count) : EmptyArray.INT;
        int[] obtain = sortByInsertionOrder ? obtain(count) : EmptyArray.INT;
        Class<T> cls2 = cls;
        int[] orderSortBuffer = obtain;
        int[] prioSortBuffer2 = prioSortBuffer;
        T[] ret2 = ret;
        getSpansRec(i, i2, cls2, treeRoot(), ret, prioSortBuffer, obtain, 0, sortByInsertionOrder);
        if (sortByInsertionOrder) {
            int[] orderSortBuffer2 = orderSortBuffer;
            sort(ret2, prioSortBuffer2, orderSortBuffer2);
            recycle(prioSortBuffer2);
            recycle(orderSortBuffer2);
        }
        return ret2;
    }

    private int countSpans(int queryStart, int queryEnd, Class kind, int i) {
        int left;
        int spanMax;
        int count = 0;
        if ((i & 1) != 0) {
            left = leftChild(i);
            spanMax = this.mSpanMax[left];
            if (spanMax > this.mGapStart) {
                spanMax -= this.mGapLength;
            }
            if (spanMax >= queryStart) {
                count = countSpans(queryStart, queryEnd, kind, left);
            }
        }
        if (i >= this.mSpanCount) {
            return count;
        }
        left = this.mSpanStarts[i];
        if (left > this.mGapStart) {
            left -= this.mGapLength;
        }
        if (left > queryEnd) {
            return count;
        }
        spanMax = this.mSpanEnds[i];
        if (spanMax > this.mGapStart) {
            spanMax -= this.mGapLength;
        }
        if (spanMax >= queryStart && ((left == spanMax || queryStart == queryEnd || !(left == queryEnd || spanMax == queryStart)) && (Object.class == kind || kind.isInstance(this.mSpans[i])))) {
            count++;
        }
        if ((i & 1) != 0) {
            return count + countSpans(queryStart, queryEnd, kind, rightChild(i));
        }
        return count;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x003f  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x003e A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private <T> int getSpansRec(int queryStart, int queryEnd, Class<T> kind, int i, T[] ret, int[] priority, int[] insertionOrder, int count, boolean sort) {
        int spanMax;
        int i2 = queryStart;
        int i3 = queryEnd;
        Class<T> cls = kind;
        int i4 = i;
        T[] tArr = ret;
        if ((i4 & 1) != 0) {
            int left = leftChild(i);
            spanMax = this.mSpanMax[left];
            if (spanMax > this.mGapStart) {
                spanMax -= this.mGapLength;
            }
            int spanMax2 = spanMax;
            if (spanMax2 >= i2) {
                spanMax = getSpansRec(i2, i3, cls, left, tArr, priority, insertionOrder, count, sort);
                if (i4 < this.mSpanCount) {
                    return spanMax;
                }
                int spanStart = this.mSpanStarts[i4];
                if (spanStart > this.mGapStart) {
                    spanStart -= this.mGapLength;
                }
                spanMax2 = spanStart;
                if (spanMax2 <= i3) {
                    spanStart = this.mSpanEnds[i4];
                    if (spanStart > this.mGapStart) {
                        spanStart -= this.mGapLength;
                    }
                    int spanEnd = spanStart;
                    if (spanEnd >= i2 && ((spanMax2 == spanEnd || i2 == i3 || !(spanMax2 == i3 || spanEnd == i2)) && (Object.class == cls || cls.isInstance(this.mSpans[i4])))) {
                        spanStart = this.mSpanFlags[i4] & Spanned.SPAN_PRIORITY;
                        int target = spanMax;
                        if (sort) {
                            priority[target] = spanStart;
                            insertionOrder[target] = this.mSpanOrder[i4];
                        } else if (spanStart != 0) {
                            int j = 0;
                            while (j < spanMax && spanStart <= (getSpanFlags(tArr[j]) & Spanned.SPAN_PRIORITY)) {
                                j++;
                            }
                            System.arraycopy(tArr, j, tArr, j + 1, spanMax - j);
                            target = j;
                        }
                        tArr[target] = this.mSpans[i4];
                        spanMax++;
                    }
                    int count2 = spanMax;
                    if (count2 >= tArr.length || (i4 & 1) == 0) {
                        int i5 = spanMax2;
                        spanMax = count2;
                    } else {
                        spanMax = getSpansRec(i2, i3, cls, rightChild(i), tArr, priority, insertionOrder, count2, sort);
                    }
                }
                return spanMax;
            }
        }
        spanMax = count;
        if (i4 < this.mSpanCount) {
        }
    }

    private static int[] obtain(int elementCount) {
        int[] result = null;
        synchronized (sCachedIntBuffer) {
            int candidateIndex = -1;
            for (int i = sCachedIntBuffer.length - 1; i >= 0; i--) {
                if (sCachedIntBuffer[i] != null) {
                    if (sCachedIntBuffer[i].length >= elementCount) {
                        candidateIndex = i;
                        break;
                    } else if (candidateIndex == -1) {
                        candidateIndex = i;
                    }
                }
            }
            if (candidateIndex != -1) {
                result = sCachedIntBuffer[candidateIndex];
                sCachedIntBuffer[candidateIndex] = null;
            }
        }
        return checkSortBuffer(result, elementCount);
    }

    private static void recycle(int[] buffer) {
        synchronized (sCachedIntBuffer) {
            int i = 0;
            while (i < sCachedIntBuffer.length) {
                if (sCachedIntBuffer[i] != null) {
                    if (buffer.length <= sCachedIntBuffer[i].length) {
                        i++;
                    }
                }
                sCachedIntBuffer[i] = buffer;
            }
        }
    }

    private static int[] checkSortBuffer(int[] buffer, int size) {
        if (buffer == null || size > buffer.length) {
            return ArrayUtils.newUnpaddedIntArray(GrowingArrayUtils.growSize(size));
        }
        return buffer;
    }

    private final <T> void sort(T[] array, int[] priority, int[] insertionOrder) {
        int i;
        T[] tArr = array;
        int size = tArr.length;
        int i2 = (size / 2) - 1;
        while (true) {
            i = i2;
            if (i < 0) {
                break;
            }
            siftDown(i, tArr, size, priority, insertionOrder);
            i2 = i - 1;
        }
        i2 = size - 1;
        while (true) {
            i = i2;
            if (i > 0) {
                T tmpSpan = tArr[0];
                tArr[0] = tArr[i];
                tArr[i] = tmpSpan;
                int tmpPriority = priority[0];
                priority[0] = priority[i];
                priority[i] = tmpPriority;
                int tmpOrder = insertionOrder[0];
                insertionOrder[0] = insertionOrder[i];
                insertionOrder[i] = tmpOrder;
                siftDown(0, tArr, i, priority, insertionOrder);
                i2 = i - 1;
            } else {
                return;
            }
        }
    }

    private final <T> void siftDown(int index, T[] array, int size, int[] priority, int[] insertionOrder) {
        int left = (2 * index) + 1;
        while (left < size) {
            if (left < size - 1 && compareSpans(left, left + 1, priority, insertionOrder) < 0) {
                left++;
            }
            if (compareSpans(index, left, priority, insertionOrder) < 0) {
                T tmpSpan = array[index];
                array[index] = array[left];
                array[left] = tmpSpan;
                int tmpPriority = priority[index];
                priority[index] = priority[left];
                priority[left] = tmpPriority;
                int tmpOrder = insertionOrder[index];
                insertionOrder[index] = insertionOrder[left];
                insertionOrder[left] = tmpOrder;
                index = left;
                left = (2 * index) + 1;
            } else {
                return;
            }
        }
    }

    private final int compareSpans(int left, int right, int[] priority, int[] insertionOrder) {
        int priority1 = priority[left];
        int priority2 = priority[right];
        if (priority1 == priority2) {
            return Integer.compare(insertionOrder[left], insertionOrder[right]);
        }
        return Integer.compare(priority2, priority1);
    }

    public int nextSpanTransition(int start, int limit, Class kind) {
        if (this.mSpanCount == 0) {
            return limit;
        }
        if (kind == null) {
            kind = Object.class;
        }
        return nextSpanTransitionRec(start, limit, kind, treeRoot());
    }

    private int nextSpanTransitionRec(int start, int limit, Class kind, int i) {
        int left;
        if ((i & 1) != 0) {
            left = leftChild(i);
            if (resolveGap(this.mSpanMax[left]) > start) {
                limit = nextSpanTransitionRec(start, limit, kind, left);
            }
        }
        if (i >= this.mSpanCount) {
            return limit;
        }
        left = resolveGap(this.mSpanStarts[i]);
        int en = resolveGap(this.mSpanEnds[i]);
        if (left > start && left < limit && kind.isInstance(this.mSpans[i])) {
            limit = left;
        }
        if (en > start && en < limit && kind.isInstance(this.mSpans[i])) {
            limit = en;
        }
        if (left >= limit || (i & 1) == 0) {
            return limit;
        }
        return nextSpanTransitionRec(start, limit, kind, rightChild(i));
    }

    public CharSequence subSequence(int start, int end) {
        return new SpannableStringBuilder(this, start, end);
    }

    public void getChars(int start, int end, char[] dest, int destoff) {
        checkRange("getChars", start, end);
        if (end <= this.mGapStart) {
            System.arraycopy(this.mText, start, dest, destoff, end - start);
        } else if (start >= this.mGapStart) {
            System.arraycopy(this.mText, this.mGapLength + start, dest, destoff, end - start);
        } else {
            System.arraycopy(this.mText, start, dest, destoff, this.mGapStart - start);
            System.arraycopy(this.mText, this.mGapStart + this.mGapLength, dest, (this.mGapStart - start) + destoff, end - this.mGapStart);
        }
    }

    public String toString() {
        int len = length();
        char[] buf = new char[len];
        getChars(0, len, buf, 0);
        return new String(buf);
    }

    public String substring(int start, int end) {
        char[] buf = new char[(end - start)];
        getChars(start, end, buf, 0);
        return new String(buf);
    }

    public int getTextWatcherDepth() {
        return this.mTextWatcherDepth;
    }

    private void sendBeforeTextChanged(TextWatcher[] watchers, int start, int before, int after) {
        this.mTextWatcherDepth++;
        for (TextWatcher beforeTextChanged : watchers) {
            beforeTextChanged.beforeTextChanged(this, start, before, after);
        }
        this.mTextWatcherDepth--;
    }

    private void sendTextChanged(TextWatcher[] watchers, int start, int before, int after) {
        this.mTextWatcherDepth++;
        for (TextWatcher onTextChanged : watchers) {
            onTextChanged.onTextChanged(this, start, before, after);
        }
        this.mTextWatcherDepth--;
    }

    private void sendAfterTextChanged(TextWatcher[] watchers) {
        this.mTextWatcherDepth++;
        for (TextWatcher afterTextChanged : watchers) {
            afterTextChanged.afterTextChanged(this);
        }
        this.mTextWatcherDepth--;
    }

    private void sendSpanAdded(Object what, int start, int end) {
        for (SpanWatcher onSpanAdded : (SpanWatcher[]) getSpans(start, end, SpanWatcher.class)) {
            onSpanAdded.onSpanAdded(this, what, start, end);
        }
    }

    private void sendSpanRemoved(Object what, int start, int end) {
        for (SpanWatcher onSpanRemoved : (SpanWatcher[]) getSpans(start, end, SpanWatcher.class)) {
            onSpanRemoved.onSpanRemoved(this, what, start, end);
        }
    }

    private void sendSpanChanged(Object what, int oldStart, int oldEnd, int start, int end) {
        for (SpanWatcher onSpanChanged : (SpanWatcher[]) getSpans(Math.min(oldStart, start), Math.min(Math.max(oldEnd, end), length()), SpanWatcher.class)) {
            onSpanChanged.onSpanChanged(this, what, oldStart, oldEnd, start, end);
        }
    }

    private static String region(int start, int end) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder.append(start);
        stringBuilder.append(" ... ");
        stringBuilder.append(end);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private void checkRange(String operation, int start, int end) {
        if (end >= start) {
            int len = length();
            StringBuilder stringBuilder;
            if (start > len || end > len) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(operation);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(region(start, end));
                stringBuilder.append(" ends beyond length ");
                stringBuilder.append(len);
                throw new IndexOutOfBoundsException(stringBuilder.toString());
            } else if (start < 0 || end < 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(operation);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(region(start, end));
                stringBuilder.append(" starts before 0");
                throw new IndexOutOfBoundsException(stringBuilder.toString());
            } else {
                return;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(operation);
        stringBuilder2.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuilder2.append(region(start, end));
        stringBuilder2.append(" has end before start");
        throw new IndexOutOfBoundsException(stringBuilder2.toString());
    }

    public void drawText(BaseCanvas c, int start, int end, float x, float y, Paint p) {
        checkRange("drawText", start, end);
        if (end <= this.mGapStart) {
            c.drawText(this.mText, start, end - start, x, y, p);
        } else if (start >= this.mGapStart) {
            c.drawText(this.mText, start + this.mGapLength, end - start, x, y, p);
        } else {
            char[] buf = TextUtils.obtain(end - start);
            getChars(start, end, buf, 0);
            c.drawText(buf, 0, end - start, x, y, p);
            TextUtils.recycle(buf);
        }
    }

    public void drawTextRun(BaseCanvas c, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, Paint p) {
        int i = start;
        int i2 = end;
        int i3 = contextStart;
        int i4 = contextEnd;
        checkRange("drawTextRun", i, i2);
        int contextLen = i4 - i3;
        int len = i2 - i;
        int i5;
        if (i4 <= this.mGapStart) {
            c.drawTextRun(this.mText, i, len, i3, contextLen, x, y, isRtl, p);
            i5 = contextLen;
        } else if (i3 >= this.mGapStart) {
            c.drawTextRun(this.mText, i + this.mGapLength, len, i3 + this.mGapLength, contextLen, x, y, isRtl, p);
        } else {
            i5 = contextLen;
            char[] buf = TextUtils.obtain(i5);
            getChars(i3, i4, buf, 0);
            c.drawTextRun(buf, i - i3, len, 0, i5, x, y, isRtl, p);
            TextUtils.recycle(buf);
        }
    }

    public float measureText(int start, int end, Paint p) {
        checkRange("measureText", start, end);
        if (end <= this.mGapStart) {
            return p.measureText(this.mText, start, end - start);
        }
        if (start >= this.mGapStart) {
            return p.measureText(this.mText, this.mGapLength + start, end - start);
        }
        char[] buf = TextUtils.obtain(end - start);
        getChars(start, end, buf, 0);
        float ret = p.measureText(buf, 0, end - start);
        TextUtils.recycle(buf);
        return ret;
    }

    public int getTextWidths(int start, int end, float[] widths, Paint p) {
        checkRange("getTextWidths", start, end);
        if (end <= this.mGapStart) {
            return p.getTextWidths(this.mText, start, end - start, widths);
        }
        if (start >= this.mGapStart) {
            return p.getTextWidths(this.mText, this.mGapLength + start, end - start, widths);
        }
        char[] buf = TextUtils.obtain(end - start);
        getChars(start, end, buf, 0);
        int ret = p.getTextWidths(buf, 0, end - start, widths);
        TextUtils.recycle(buf);
        return ret;
    }

    public float getTextRunAdvances(int start, int end, int contextStart, int contextEnd, boolean isRtl, float[] advances, int advancesPos, Paint p) {
        int i = start;
        int i2 = end;
        int i3 = contextStart;
        int i4 = contextEnd;
        int contextLen = i4 - i3;
        int len = i2 - i;
        if (i2 <= this.mGapStart) {
            return p.getTextRunAdvances(this.mText, i, len, i3, contextLen, isRtl, advances, advancesPos);
        } else if (i >= this.mGapStart) {
            return p.getTextRunAdvances(this.mText, i + this.mGapLength, len, i3 + this.mGapLength, contextLen, isRtl, advances, advancesPos);
        } else {
            char[] buf = TextUtils.obtain(contextLen);
            getChars(i3, i4, buf, 0);
            char[] buf2 = buf;
            float ret = p.getTextRunAdvances(buf, i - i3, len, 0, contextLen, isRtl, advances, advancesPos);
            TextUtils.recycle(buf2);
            return ret;
        }
    }

    @Deprecated
    public int getTextRunCursor(int contextStart, int contextEnd, int dir, int offset, int cursorOpt, Paint p) {
        int contextLen = contextEnd - contextStart;
        if (contextEnd <= this.mGapStart) {
            return p.getTextRunCursor(this.mText, contextStart, contextLen, dir, offset, cursorOpt);
        } else if (contextStart >= this.mGapStart) {
            return p.getTextRunCursor(this.mText, contextStart + this.mGapLength, contextLen, dir, offset + this.mGapLength, cursorOpt) - this.mGapLength;
        } else {
            char[] buf = TextUtils.obtain(contextLen);
            getChars(contextStart, contextEnd, buf, 0);
            int ret = p.getTextRunCursor(buf, 0, contextLen, dir, offset - contextStart, cursorOpt) + contextStart;
            TextUtils.recycle(buf);
            return ret;
        }
    }

    public void setFilters(InputFilter[] filters) {
        if (filters != null) {
            this.mFilters = filters;
            return;
        }
        throw new IllegalArgumentException();
    }

    public InputFilter[] getFilters() {
        return this.mFilters;
    }

    public boolean equals(Object o) {
        if ((o instanceof Spanned) && toString().equals(o.toString())) {
            Spanned other = (Spanned) o;
            Object[] otherSpans = other.getSpans(0, other.length(), Object.class);
            if (this.mSpanCount == otherSpans.length) {
                for (int i = 0; i < this.mSpanCount; i++) {
                    SpannableStringBuilder thisSpan = this.mSpans[i];
                    Spanned otherSpan = otherSpans[i];
                    if (thisSpan == this) {
                        if (other != otherSpan || getSpanStart(thisSpan) != other.getSpanStart(otherSpan) || getSpanEnd(thisSpan) != other.getSpanEnd(otherSpan) || getSpanFlags(thisSpan) != other.getSpanFlags(otherSpan)) {
                            return false;
                        }
                    } else if (!thisSpan.equals(otherSpan) || getSpanStart(thisSpan) != other.getSpanStart(otherSpan) || getSpanEnd(thisSpan) != other.getSpanEnd(otherSpan) || getSpanFlags(thisSpan) != other.getSpanFlags(otherSpan)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        int hash = (toString().hashCode() * 31) + this.mSpanCount;
        for (int i = 0; i < this.mSpanCount; i++) {
            SpannableStringBuilder span = this.mSpans[i];
            if (span != this) {
                hash = (hash * 31) + span.hashCode();
            }
            hash = (((((hash * 31) + getSpanStart(span)) * 31) + getSpanEnd(span)) * 31) + getSpanFlags(span);
        }
        return hash;
    }

    private int treeRoot() {
        return Integer.highestOneBit(this.mSpanCount) - 1;
    }

    private static int leftChild(int i) {
        return i - (((i + 1) & (~i)) >> 1);
    }

    private static int rightChild(int i) {
        return (((i + 1) & (~i)) >> 1) + i;
    }

    private int calcMax(int i) {
        int max = 0;
        if ((i & 1) != 0) {
            max = calcMax(leftChild(i));
        }
        if (i < this.mSpanCount) {
            max = Math.max(max, this.mSpanEnds[i]);
            if ((i & 1) != 0) {
                max = Math.max(max, calcMax(rightChild(i)));
            }
        }
        this.mSpanMax[i] = max;
        return max;
    }

    private void restoreInvariants() {
        if (this.mSpanCount != 0) {
            int i;
            for (i = 1; i < this.mSpanCount; i++) {
                if (this.mSpanStarts[i] < this.mSpanStarts[i - 1]) {
                    Object span = this.mSpans[i];
                    int start = this.mSpanStarts[i];
                    int end = this.mSpanEnds[i];
                    int flags = this.mSpanFlags[i];
                    int insertionOrder = this.mSpanOrder[i];
                    int j = i;
                    do {
                        this.mSpans[j] = this.mSpans[j - 1];
                        this.mSpanStarts[j] = this.mSpanStarts[j - 1];
                        this.mSpanEnds[j] = this.mSpanEnds[j - 1];
                        this.mSpanFlags[j] = this.mSpanFlags[j - 1];
                        this.mSpanOrder[j] = this.mSpanOrder[j - 1];
                        j--;
                        if (j <= 0) {
                            break;
                        }
                    } while (start < this.mSpanStarts[j - 1]);
                    this.mSpans[j] = span;
                    this.mSpanStarts[j] = start;
                    this.mSpanEnds[j] = end;
                    this.mSpanFlags[j] = flags;
                    this.mSpanOrder[j] = insertionOrder;
                    invalidateIndex(j);
                }
            }
            calcMax(treeRoot());
            if (this.mIndexOfSpan == null) {
                this.mIndexOfSpan = new IdentityHashMap();
            }
            i = this.mLowWaterMark;
            while (i < this.mSpanCount) {
                Integer existing = (Integer) this.mIndexOfSpan.get(this.mSpans[i]);
                if (existing == null || existing.intValue() != i) {
                    this.mIndexOfSpan.put(this.mSpans[i], Integer.valueOf(i));
                }
                i++;
            }
            this.mLowWaterMark = Integer.MAX_VALUE;
        }
    }

    private void invalidateIndex(int i) {
        this.mLowWaterMark = Math.min(i, this.mLowWaterMark);
    }
}
