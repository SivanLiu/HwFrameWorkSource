package android.text;

import android.net.wifi.WifiEnterpriseConfig;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.reflect.Array;
import libcore.util.EmptyArray;

abstract class SpannableStringInternal {
    private static final int COLUMNS = 3;
    static final Object[] EMPTY = new Object[0];
    private static final int END = 1;
    private static final int FLAGS = 2;
    private static final int START = 0;
    private int mSpanCount;
    private int[] mSpanData;
    private Object[] mSpans;
    private String mText;

    SpannableStringInternal(CharSequence source, int start, int end, boolean ignoreNoCopySpan) {
        if (start == 0 && end == source.length()) {
            this.mText = source.toString();
        } else {
            this.mText = source.toString().substring(start, end);
        }
        this.mSpans = EmptyArray.OBJECT;
        this.mSpanData = EmptyArray.INT;
        if (!(source instanceof Spanned)) {
            return;
        }
        if (source instanceof SpannableStringInternal) {
            copySpans((SpannableStringInternal) source, start, end, ignoreNoCopySpan);
        } else {
            copySpans((Spanned) source, start, end, ignoreNoCopySpan);
        }
    }

    SpannableStringInternal(CharSequence source, int start, int end) {
        this(source, start, end, false);
    }

    private void copySpans(Spanned src, int start, int end, boolean ignoreNoCopySpan) {
        Object[] spans = src.getSpans(start, end, Object.class);
        int i = 0;
        while (i < spans.length) {
            if (!ignoreNoCopySpan || !(spans[i] instanceof NoCopySpan)) {
                int st = src.getSpanStart(spans[i]);
                int en = src.getSpanEnd(spans[i]);
                int fl = src.getSpanFlags(spans[i]);
                if (st < start) {
                    st = start;
                }
                if (en > end) {
                    en = end;
                }
                setSpan(spans[i], st - start, en - start, fl, false);
            }
            i++;
        }
    }

    private void copySpans(SpannableStringInternal src, int start, int end, boolean ignoreNoCopySpan) {
        int i;
        SpannableStringInternal spannableStringInternal = src;
        int i2 = start;
        int i3 = end;
        int[] srcData = spannableStringInternal.mSpanData;
        Object[] srcSpans = spannableStringInternal.mSpans;
        int limit = spannableStringInternal.mSpanCount;
        boolean hasNoCopySpan = false;
        int count = 0;
        for (i = 0; i < limit; i++) {
            if (!isOutOfCopyRange(i2, i3, srcData[(i * 3) + 0], srcData[(i * 3) + 1])) {
                if (srcSpans[i] instanceof NoCopySpan) {
                    hasNoCopySpan = true;
                    if (ignoreNoCopySpan) {
                    }
                }
                count++;
            }
        }
        if (count != 0) {
            if (!hasNoCopySpan && i2 == 0 && i3 == src.length()) {
                this.mSpans = ArrayUtils.newUnpaddedObjectArray(spannableStringInternal.mSpans.length);
                this.mSpanData = new int[spannableStringInternal.mSpanData.length];
                this.mSpanCount = spannableStringInternal.mSpanCount;
                System.arraycopy(spannableStringInternal.mSpans, 0, this.mSpans, 0, spannableStringInternal.mSpans.length);
                System.arraycopy(spannableStringInternal.mSpanData, 0, this.mSpanData, 0, this.mSpanData.length);
            } else {
                this.mSpanCount = count;
                this.mSpans = ArrayUtils.newUnpaddedObjectArray(this.mSpanCount);
                this.mSpanData = new int[(this.mSpans.length * 3)];
                int i4 = 0;
                i = 0;
                while (i4 < limit) {
                    int spanStart = srcData[(i4 * 3) + 0];
                    int spanEnd = srcData[(i4 * 3) + 1];
                    if (!(isOutOfCopyRange(i2, i3, spanStart, spanEnd) || (ignoreNoCopySpan && (srcSpans[i4] instanceof NoCopySpan)))) {
                        if (spanStart < i2) {
                            spanStart = i2;
                        }
                        if (spanEnd > i3) {
                            spanEnd = i3;
                        }
                        this.mSpans[i] = srcSpans[i4];
                        this.mSpanData[(i * 3) + 0] = spanStart - i2;
                        this.mSpanData[(i * 3) + 1] = spanEnd - i2;
                        this.mSpanData[(i * 3) + 2] = srcData[(i4 * 3) + 2];
                        i++;
                    }
                    i4++;
                }
            }
        }
    }

    private final boolean isOutOfCopyRange(int start, int end, int spanStart, int spanEnd) {
        if (spanStart > end || spanEnd < start) {
            return true;
        }
        if (spanStart == spanEnd || start == end || (spanStart != end && spanEnd != start)) {
            return false;
        }
        return true;
    }

    public final int length() {
        return this.mText.length();
    }

    public final char charAt(int i) {
        return this.mText.charAt(i);
    }

    public final String toString() {
        return this.mText;
    }

    public final void getChars(int start, int end, char[] dest, int off) {
        this.mText.getChars(start, end, dest, off);
    }

    void setSpan(Object what, int start, int end, int flags) {
        setSpan(what, start, end, flags, true);
    }

    private boolean isIndexFollowsNextLine(int index) {
        return (index == 0 || index == length() || charAt(index - 1) == 10) ? false : true;
    }

    private void setSpan(Object what, int start, int end, int flags, boolean enforceParagraph) {
        Object obj = what;
        int i = start;
        int i2 = end;
        int nstart = i;
        int nend = i2;
        checkRange("setSpan", i, i2);
        if ((flags & 51) == 51) {
            StringBuilder stringBuilder;
            if (isIndexFollowsNextLine(i)) {
                if (enforceParagraph) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("PARAGRAPH span must start at paragraph boundary (");
                    stringBuilder.append(i);
                    stringBuilder.append(" follows ");
                    stringBuilder.append(charAt(i - 1));
                    stringBuilder.append(")");
                    throw new RuntimeException(stringBuilder.toString());
                }
                return;
            } else if (isIndexFollowsNextLine(i2)) {
                if (enforceParagraph) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("PARAGRAPH span must end at paragraph boundary (");
                    stringBuilder.append(i2);
                    stringBuilder.append(" follows ");
                    stringBuilder.append(charAt(i2 - 1));
                    stringBuilder.append(")");
                    throw new RuntimeException(stringBuilder.toString());
                }
                return;
            }
        }
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        int i3 = 0;
        while (true) {
            int i4 = i3;
            if (i4 >= count) {
                if (this.mSpanCount + 1 >= this.mSpans.length) {
                    Object[] newtags = ArrayUtils.newUnpaddedObjectArray(GrowingArrayUtils.growSize(this.mSpanCount));
                    int[] newdata = new int[(newtags.length * 3)];
                    System.arraycopy(this.mSpans, 0, newtags, 0, this.mSpanCount);
                    System.arraycopy(this.mSpanData, 0, newdata, 0, this.mSpanCount * 3);
                    this.mSpans = newtags;
                    this.mSpanData = newdata;
                }
                this.mSpans[this.mSpanCount] = obj;
                this.mSpanData[(this.mSpanCount * 3) + 0] = i;
                this.mSpanData[(this.mSpanCount * 3) + 1] = i2;
                this.mSpanData[(this.mSpanCount * 3) + 2] = flags;
                this.mSpanCount++;
                if (this instanceof Spannable) {
                    sendSpanAdded(obj, nstart, nend);
                }
                return;
            } else if (spans[i4] == obj) {
                int ostart = data[(i4 * 3) + 0];
                int oend = data[(i4 * 3) + 1];
                data[(i4 * 3) + 0] = i;
                data[(i4 * 3) + 1] = i2;
                data[(i4 * 3) + 2] = flags;
                sendSpanChanged(obj, ostart, oend, nstart, nend);
                return;
            } else {
                int[] iArr = data;
                i3 = i4 + 1;
            }
        }
    }

    void removeSpan(Object what) {
        removeSpan(what, 0);
    }

    public void removeSpan(Object what, int flags) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                int ostart = data[(i * 3) + 0];
                int oend = data[(i * 3) + 1];
                int c = count - (i + 1);
                System.arraycopy(spans, i + 1, spans, i, c);
                System.arraycopy(data, (i + 1) * 3, data, i * 3, c * 3);
                this.mSpanCount--;
                if ((flags & 512) == 0) {
                    sendSpanRemoved(what, ostart, oend);
                }
                return;
            }
        }
    }

    public int getSpanStart(Object what) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                return data[(i * 3) + 0];
            }
        }
        return -1;
    }

    public int getSpanEnd(Object what) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                return data[(i * 3) + 1];
            }
        }
        return -1;
    }

    public int getSpanFlags(Object what) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                return data[(i * 3) + 2];
            }
        }
        return 0;
    }

    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind) {
        int i = queryStart;
        int i2 = queryEnd;
        Class<T> cls = kind;
        int spanCount = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        int i3 = 0;
        Object ret1 = null;
        Object[] ret = null;
        int count = 0;
        int i4 = 0;
        while (i4 < spanCount) {
            int spanStart = data[(i4 * 3) + i3];
            int spanEnd = data[(i4 * 3) + 1];
            if (spanStart <= i2 && spanEnd >= i && ((spanStart == spanEnd || i == i2 || !(spanStart == i2 || spanEnd == i)) && (cls == null || cls == Object.class || cls.isInstance(spans[i4])))) {
                if (count == 0) {
                    ret1 = spans[i4];
                    count++;
                } else {
                    if (count == 1) {
                        ret = (Object[]) Array.newInstance(cls, (spanCount - i4) + 1);
                        ret[i3] = ret1;
                    }
                    int prio = data[(i4 * 3) + 2] & Spanned.SPAN_PRIORITY;
                    if (prio != 0) {
                        int j = i3;
                        while (true) {
                            i3 = j;
                            if (i3 >= count || prio > (getSpanFlags(ret[i3]) & Spanned.SPAN_PRIORITY)) {
                                System.arraycopy(ret, i3, ret, i3 + 1, count - i3);
                                ret[i3] = spans[i4];
                                count++;
                            } else {
                                j = i3 + 1;
                                i = queryStart;
                            }
                        }
                        System.arraycopy(ret, i3, ret, i3 + 1, count - i3);
                        ret[i3] = spans[i4];
                        count++;
                    } else {
                        i = count + 1;
                        ret[count] = spans[i4];
                        count = i;
                    }
                }
            }
            i4++;
            i = queryStart;
            i3 = 0;
        }
        if (count == 0) {
            return ArrayUtils.emptyArray(kind);
        }
        Object[] ret2;
        if (count == 1) {
            ret2 = (Object[]) Array.newInstance(cls, 1);
            ret2[0] = ret1;
            return ret2;
        } else if (count == ret.length) {
            return ret;
        } else {
            ret2 = (Object[]) Array.newInstance(cls, count);
            System.arraycopy(ret, 0, ret2, 0, count);
            return ret2;
        }
    }

    public int nextSpanTransition(int start, int limit, Class kind) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] data = this.mSpanData;
        if (kind == null) {
            kind = Object.class;
        }
        int limit2 = limit;
        limit = 0;
        while (limit < count) {
            int st = data[(limit * 3) + 0];
            int en = data[(limit * 3) + 1];
            if (st > start && st < limit2 && kind.isInstance(spans[limit])) {
                limit2 = st;
            }
            if (en > start && en < limit2 && kind.isInstance(spans[limit])) {
                limit2 = en;
            }
            limit++;
        }
        return limit2;
    }

    private void sendSpanAdded(Object what, int start, int end) {
        for (SpanWatcher onSpanAdded : (SpanWatcher[]) getSpans(start, end, SpanWatcher.class)) {
            onSpanAdded.onSpanAdded((Spannable) this, what, start, end);
        }
    }

    private void sendSpanRemoved(Object what, int start, int end) {
        for (SpanWatcher onSpanRemoved : (SpanWatcher[]) getSpans(start, end, SpanWatcher.class)) {
            onSpanRemoved.onSpanRemoved((Spannable) this, what, start, end);
        }
    }

    private void sendSpanChanged(Object what, int s, int e, int st, int en) {
        for (SpanWatcher onSpanChanged : (SpanWatcher[]) getSpans(Math.min(s, st), Math.max(e, en), SpanWatcher.class)) {
            onSpanChanged.onSpanChanged((Spannable) this, what, s, e, st, en);
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

    public boolean equals(Object o) {
        if ((o instanceof Spanned) && toString().equals(o.toString())) {
            Spanned other = (Spanned) o;
            Object[] otherSpans = other.getSpans(0, other.length(), Object.class);
            if (this.mSpanCount == otherSpans.length) {
                for (int i = 0; i < this.mSpanCount; i++) {
                    SpannableStringInternal thisSpan = this.mSpans[i];
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
            SpannableStringInternal span = this.mSpans[i];
            if (span != this) {
                hash = (hash * 31) + span.hashCode();
            }
            hash = (((((hash * 31) + getSpanStart(span)) * 31) + getSpanEnd(span)) * 31) + getSpanFlags(span);
        }
        return hash;
    }

    private void copySpans(Spanned src, int start, int end) {
        copySpans(src, start, end, false);
    }

    private void copySpans(SpannableStringInternal src, int start, int end) {
        copySpans(src, start, end, false);
    }
}
