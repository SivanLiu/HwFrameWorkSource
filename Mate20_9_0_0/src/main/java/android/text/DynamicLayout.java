package android.text;

import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.os.Process;
import android.text.Layout.Alignment;
import android.text.Layout.Directions;
import android.text.TextUtils.TruncateAt;
import android.text.style.ReplacementSpan;
import android.text.style.UpdateLayout;
import android.text.style.WrapTogetherSpan;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Pools.SynchronizedPool;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.ref.WeakReference;

public class DynamicLayout extends Layout {
    private static final int BLOCK_MINIMUM_CHARACTER_LENGTH = 400;
    private static final int COLUMNS_ELLIPSIZE = 7;
    private static final int COLUMNS_NORMAL = 5;
    private static final int DESCENT = 2;
    private static final int DIR = 0;
    private static final int DIR_SHIFT = 30;
    private static final int ELLIPSIS_COUNT = 6;
    private static final int ELLIPSIS_START = 5;
    private static final int ELLIPSIS_UNDEFINED = Integer.MIN_VALUE;
    private static final int EXTRA = 3;
    private static final int HYPHEN = 4;
    private static final int HYPHEN_MASK = 255;
    public static final int INVALID_BLOCK_INDEX = -1;
    private static final int MAY_PROTRUDE_FROM_TOP_OR_BOTTOM = 4;
    private static final int MAY_PROTRUDE_FROM_TOP_OR_BOTTOM_MASK = 256;
    private static final int PRIORITY = 128;
    private static final int START = 0;
    private static final int START_MASK = 536870911;
    private static final int TAB = 0;
    private static final int TAB_MASK = 536870912;
    private static final String TAG = "DynamicLayout";
    private static final int TOP = 1;
    private static android.text.StaticLayout.Builder sBuilder = null;
    private static final Object[] sLock = new Object[0];
    private static StaticLayout sStaticLayout = null;
    private CharSequence mBase;
    private int[] mBlockEndLines;
    private int[] mBlockIndices;
    private ArraySet<Integer> mBlocksAlwaysNeedToBeRedrawn;
    private int mBottomPadding;
    private int mBreakStrategy;
    private CharSequence mDisplay;
    private boolean mEllipsize;
    private TruncateAt mEllipsizeAt;
    private int mEllipsizedWidth;
    private boolean mFallbackLineSpacing;
    private int mHyphenationFrequency;
    private boolean mIncludePad;
    private int mIndexFirstChangedBlock;
    private PackedIntVector mInts;
    private int mJustificationMode;
    private int mNumberOfBlocks;
    private PackedObjectVector<Directions> mObjects;
    private Rect mTempRect;
    private int mTopPadding;
    private ChangeWatcher mWatcher;

    public static final class Builder {
        private static final SynchronizedPool<Builder> sPool = new SynchronizedPool(3);
        private Alignment mAlignment;
        private CharSequence mBase;
        private int mBreakStrategy;
        private CharSequence mDisplay;
        private TruncateAt mEllipsize;
        private int mEllipsizedWidth;
        private boolean mFallbackLineSpacing;
        private final FontMetricsInt mFontMetricsInt = new FontMetricsInt();
        private int mHyphenationFrequency;
        private boolean mIncludePad;
        private int mJustificationMode;
        private TextPaint mPaint;
        private float mSpacingAdd;
        private float mSpacingMult;
        private TextDirectionHeuristic mTextDir;
        private int mWidth;

        private Builder() {
        }

        public static Builder obtain(CharSequence base, TextPaint paint, int width) {
            Builder b = (Builder) sPool.acquire();
            if (b == null) {
                b = new Builder();
            }
            b.mBase = base;
            b.mDisplay = base;
            b.mPaint = paint;
            b.mWidth = width;
            b.mAlignment = Alignment.ALIGN_NORMAL;
            b.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            b.mSpacingMult = 1.0f;
            b.mSpacingAdd = 0.0f;
            b.mIncludePad = true;
            b.mFallbackLineSpacing = false;
            b.mEllipsizedWidth = width;
            b.mEllipsize = null;
            b.mBreakStrategy = 0;
            b.mHyphenationFrequency = 0;
            b.mJustificationMode = 0;
            return b;
        }

        private static void recycle(Builder b) {
            b.mBase = null;
            b.mDisplay = null;
            b.mPaint = null;
            sPool.release(b);
        }

        public Builder setDisplayText(CharSequence display) {
            this.mDisplay = display;
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            this.mAlignment = alignment;
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic textDir) {
            this.mTextDir = textDir;
            return this;
        }

        public Builder setLineSpacing(float spacingAdd, float spacingMult) {
            this.mSpacingAdd = spacingAdd;
            this.mSpacingMult = spacingMult;
            return this;
        }

        public Builder setIncludePad(boolean includePad) {
            this.mIncludePad = includePad;
            return this;
        }

        public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks) {
            this.mFallbackLineSpacing = useLineSpacingFromFallbacks;
            return this;
        }

        public Builder setEllipsizedWidth(int ellipsizedWidth) {
            this.mEllipsizedWidth = ellipsizedWidth;
            return this;
        }

        public Builder setEllipsize(TruncateAt ellipsize) {
            this.mEllipsize = ellipsize;
            return this;
        }

        public Builder setBreakStrategy(int breakStrategy) {
            this.mBreakStrategy = breakStrategy;
            return this;
        }

        public Builder setHyphenationFrequency(int hyphenationFrequency) {
            this.mHyphenationFrequency = hyphenationFrequency;
            return this;
        }

        public Builder setJustificationMode(int justificationMode) {
            this.mJustificationMode = justificationMode;
            return this;
        }

        public DynamicLayout build() {
            DynamicLayout result = new DynamicLayout(this);
            recycle(this);
            return result;
        }
    }

    private static class ChangeWatcher implements TextWatcher, SpanWatcher {
        private WeakReference<DynamicLayout> mLayout;

        public ChangeWatcher(DynamicLayout layout) {
            this.mLayout = new WeakReference(layout);
        }

        private void reflow(CharSequence s, int where, int before, int after) {
            DynamicLayout ml = (DynamicLayout) this.mLayout.get();
            if (ml != null) {
                ml.reflow(s, where, before, after);
            } else if (s instanceof Spannable) {
                ((Spannable) s).removeSpan(this);
            }
        }

        public void beforeTextChanged(CharSequence s, int where, int before, int after) {
        }

        public void onTextChanged(CharSequence s, int where, int before, int after) {
            reflow(s, where, before, after);
        }

        public void afterTextChanged(Editable s) {
        }

        public void onSpanAdded(Spannable s, Object o, int start, int end) {
            if (o instanceof UpdateLayout) {
                reflow(s, start, end - start, end - start);
            }
        }

        public void onSpanRemoved(Spannable s, Object o, int start, int end) {
            if (o instanceof UpdateLayout) {
                reflow(s, start, end - start, end - start);
            }
        }

        public void onSpanChanged(Spannable s, Object o, int start, int end, int nstart, int nend) {
            if (o instanceof UpdateLayout) {
                if (start > end) {
                    start = 0;
                }
                reflow(s, start, end - start, end - start);
                reflow(s, nstart, nend - nstart, nend - nstart);
            }
        }
    }

    @Deprecated
    public DynamicLayout(CharSequence base, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(base, base, paint, width, align, spacingmult, spacingadd, includepad);
    }

    @Deprecated
    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(base, display, paint, width, align, spacingmult, spacingadd, includepad, null, 0);
    }

    @Deprecated
    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad, TruncateAt ellipsize, int ellipsizedWidth) {
        this(base, display, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingmult, spacingadd, includepad, 0, 0, 0, ellipsize, ellipsizedWidth);
    }

    @Deprecated
    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment align, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad, int breakStrategy, int hyphenationFrequency, int justificationMode, TruncateAt ellipsize, int ellipsizedWidth) {
        CharSequence charSequence = display;
        TruncateAt truncateAt = ellipsize;
        Alignment alignment = align;
        TextDirectionHeuristic textDirectionHeuristic = textDir;
        float f = spacingmult;
        float f2 = spacingadd;
        super(createEllipsizer(truncateAt, charSequence), paint, width, alignment, textDirectionHeuristic, f, f2);
        this.mTempRect = new Rect();
        Builder b = Builder.obtain(base, paint, width).setAlignment(alignment).setTextDirection(textDirectionHeuristic).setLineSpacing(f2, f).setEllipsizedWidth(ellipsizedWidth).setEllipsize(truncateAt);
        this.mDisplay = charSequence;
        this.mIncludePad = includepad;
        this.mBreakStrategy = breakStrategy;
        this.mJustificationMode = justificationMode;
        this.mHyphenationFrequency = hyphenationFrequency;
        generate(b);
        Builder.recycle(b);
    }

    private DynamicLayout(Builder b) {
        super(createEllipsizer(b.mEllipsize, b.mDisplay), b.mPaint, b.mWidth, b.mAlignment, b.mTextDir, b.mSpacingMult, b.mSpacingAdd);
        this.mTempRect = new Rect();
        this.mDisplay = b.mDisplay;
        this.mIncludePad = b.mIncludePad;
        this.mBreakStrategy = b.mBreakStrategy;
        this.mJustificationMode = b.mJustificationMode;
        this.mHyphenationFrequency = b.mHyphenationFrequency;
        generate(b);
    }

    private static CharSequence createEllipsizer(TruncateAt ellipsize, CharSequence display) {
        if (ellipsize == null) {
            return display;
        }
        if (display instanceof Spanned) {
            return new SpannedEllipsizer(display);
        }
        return new Ellipsizer(display);
    }

    private void generate(Builder b) {
        int[] start;
        this.mBase = b.mBase;
        this.mFallbackLineSpacing = b.mFallbackLineSpacing;
        if (b.mEllipsize != null) {
            this.mInts = new PackedIntVector(7);
            this.mEllipsizedWidth = b.mEllipsizedWidth;
            this.mEllipsizeAt = b.mEllipsize;
            Ellipsizer e = (Ellipsizer) getText();
            e.mLayout = this;
            e.mWidth = b.mEllipsizedWidth;
            e.mMethod = b.mEllipsize;
            this.mEllipsize = true;
        } else {
            this.mInts = new PackedIntVector(5);
            this.mEllipsizedWidth = b.mWidth;
            this.mEllipsizeAt = null;
        }
        this.mObjects = new PackedObjectVector(1);
        if (b.mEllipsize != null) {
            start = new int[7];
            start[5] = Integer.MIN_VALUE;
        } else {
            start = new int[5];
        }
        Directions[] dirs = new Directions[]{DIRS_ALL_LEFT_TO_RIGHT};
        FontMetricsInt fm = b.mFontMetricsInt;
        b.mPaint.getFontMetricsInt(fm);
        int asc = fm.ascent;
        int desc = fm.descent;
        start[0] = 1073741824;
        start[1] = 0;
        start[2] = desc;
        this.mInts.insertAt(0, start);
        start[1] = desc - asc;
        this.mInts.insertAt(1, start);
        this.mObjects.insertAt(0, dirs);
        int baseLength = this.mBase.length();
        reflow(this.mBase, 0, 0, baseLength);
        if (this.mBase instanceof Spannable) {
            if (this.mWatcher == null) {
                this.mWatcher = new ChangeWatcher(this);
            }
            Spannable sp = this.mBase;
            ChangeWatcher[] spans = (ChangeWatcher[]) sp.getSpans(0, baseLength, ChangeWatcher.class);
            for (Object removeSpan : spans) {
                sp.removeSpan(removeSpan);
            }
            sp.setSpan(this.mWatcher, 0, baseLength, 8388626);
        }
    }

    /* JADX WARNING: Missing block: B:42:0x00b4, code skipped:
            if (r0 != null) goto L_0x00d1;
     */
    /* JADX WARNING: Missing block: B:43:0x00b6, code skipped:
            r20 = new android.text.StaticLayout(null);
            r5 = android.text.StaticLayout.Builder.obtain(r2, r9, r9 + r10, getPaint(), getWidth());
            r14 = r20;
     */
    /* JADX WARNING: Missing block: B:44:0x00d1, code skipped:
            r14 = r0;
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:45:0x00d4, code skipped:
            r21 = r6;
            r22 = r8;
            r0 = r5.setText(r2, r9, r9 + r10).setPaint(getPaint()).setWidth(getWidth()).setTextDirection(getTextDirectionHeuristic()).setLineSpacing(getSpacingAdd(), getSpacingMultiplier()).setUseLineSpacingFromFallbacks(r1.mFallbackLineSpacing).setEllipsizedWidth(r1.mEllipsizedWidth).setEllipsize(r1.mEllipsizeAt).setBreakStrategy(r1.mBreakStrategy).setHyphenationFrequency(r1.mHyphenationFrequency).setJustificationMode(r1.mJustificationMode);
     */
    /* JADX WARNING: Missing block: B:46:0x0126, code skipped:
            if (r13 != false) goto L_0x012a;
     */
    /* JADX WARNING: Missing block: B:47:0x0128, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:48:0x012a, code skipped:
            r6 = false;
     */
    /* JADX WARNING: Missing block: B:49:0x012c, code skipped:
            r0.setAddLastLineLineSpacing(r6);
            r14.generate(r5, false, true);
            r0 = r14.getLineCount();
     */
    /* JADX WARNING: Missing block: B:50:0x013a, code skipped:
            if ((r9 + r10) == r4) goto L_0x0148;
     */
    /* JADX WARNING: Missing block: B:52:0x0144, code skipped:
            if (r14.getLineStart(r0 - 1) != (r9 + r10)) goto L_0x0148;
     */
    /* JADX WARNING: Missing block: B:53:0x0146, code skipped:
            r0 = r0 - 1;
     */
    /* JADX WARNING: Missing block: B:54:0x0148, code skipped:
            r6 = r0;
            r1.mInts.deleteAt(r3, r11 - r3);
            r1.mObjects.deleteAt(r3, r11 - r3);
            r0 = r14.getLineTop(r6);
            r8 = 0;
            r18 = 0;
            r23 = r4;
     */
    /* JADX WARNING: Missing block: B:55:0x0162, code skipped:
            if (r1.mIncludePad == 0) goto L_0x016d;
     */
    /* JADX WARNING: Missing block: B:56:0x0164, code skipped:
            if (r3 != 0) goto L_0x016d;
     */
    /* JADX WARNING: Missing block: B:57:0x0166, code skipped:
            r8 = r14.getTopPadding();
            r1.mTopPadding = r8;
            r0 = r0 - r8;
     */
    /* JADX WARNING: Missing block: B:59:0x016f, code skipped:
            if (r1.mIncludePad == false) goto L_0x017c;
     */
    /* JADX WARNING: Missing block: B:60:0x0171, code skipped:
            if (r13 == false) goto L_0x017c;
     */
    /* JADX WARNING: Missing block: B:61:0x0173, code skipped:
            r4 = r14.getBottomPadding();
            r1.mBottomPadding = r4;
            r0 = r0 + r4;
            r18 = r4;
     */
    /* JADX WARNING: Missing block: B:62:0x017c, code skipped:
            r4 = r0;
            r24 = r13;
            r25 = r15;
            r1.mInts.adjustValuesBelow(r3, 0, r10 - r15);
            r1.mInts.adjustValuesBelow(r3, 1, (r7 - r12) + r4);
     */
    /* JADX WARNING: Missing block: B:63:0x0195, code skipped:
            if (r1.mEllipsize == false) goto L_0x019f;
     */
    /* JADX WARNING: Missing block: B:64:0x0197, code skipped:
            r0 = new int[7];
            r0[5] = Integer.MIN_VALUE;
     */
    /* JADX WARNING: Missing block: B:65:0x019f, code skipped:
            r0 = new int[5];
     */
    /* JADX WARNING: Missing block: B:66:0x01a1, code skipped:
            r15 = r0;
            r13 = new android.text.Layout.Directions[1];
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:67:0x01a6, code skipped:
            if (r0 >= r6) goto L_0x0257;
     */
    /* JADX WARNING: Missing block: B:68:0x01a8, code skipped:
            r27 = r4;
            r4 = r14.getLineStart(r0);
            r15[0] = r4;
            r15[0] = r15[0] | (r14.getParagraphDirection(r0) << 30);
            r20 = r15[0];
     */
    /* JADX WARNING: Missing block: B:69:0x01c4, code skipped:
            if (r14.getLineContainsTab(r0) == false) goto L_0x01c9;
     */
    /* JADX WARNING: Missing block: B:70:0x01c6, code skipped:
            r28 = 536870912;
     */
    /* JADX WARNING: Missing block: B:71:0x01c9, code skipped:
            r28 = 0;
     */
    /* JADX WARNING: Missing block: B:72:0x01cb, code skipped:
            r15[0] = r20 | r28;
            r20 = r14.getLineTop(r0) + r7;
     */
    /* JADX WARNING: Missing block: B:73:0x01d5, code skipped:
            if (r0 <= 0) goto L_0x01d9;
     */
    /* JADX WARNING: Missing block: B:74:0x01d7, code skipped:
            r20 = r20 - r8;
     */
    /* JADX WARNING: Missing block: B:75:0x01d9, code skipped:
            r15[1] = r20;
            r28 = r14.getLineDescent(r0);
            r29 = r7;
     */
    /* JADX WARNING: Missing block: B:76:0x01e5, code skipped:
            if (r0 != (r6 - 1)) goto L_0x01e9;
     */
    /* JADX WARNING: Missing block: B:77:0x01e7, code skipped:
            r28 = r28 + r18;
     */
    /* JADX WARNING: Missing block: B:78:0x01e9, code skipped:
            r15[2] = r28;
            r15[3] = r14.getLineExtra(r0);
            r13[0] = r14.getLineDirections(r0);
     */
    /* JADX WARNING: Missing block: B:79:0x01fd, code skipped:
            if (r0 != (r6 - 1)) goto L_0x0202;
     */
    /* JADX WARNING: Missing block: B:80:0x01ff, code skipped:
            r7 = r9 + r10;
     */
    /* JADX WARNING: Missing block: B:81:0x0202, code skipped:
            r7 = r14.getLineStart(r0 + 1);
     */
    /* JADX WARNING: Missing block: B:82:0x0208, code skipped:
            r31 = r8;
            r15[4] = r14.getHyphen(r0) & 255;
            r8 = r15[4];
     */
    /* JADX WARNING: Missing block: B:83:0x021a, code skipped:
            if (contentMayProtrudeFromLineTopOrBottom(r2, r4, r7) == false) goto L_0x021f;
     */
    /* JADX WARNING: Missing block: B:84:0x021c, code skipped:
            r32 = 256;
     */
    /* JADX WARNING: Missing block: B:85:0x021f, code skipped:
            r32 = 0;
     */
    /* JADX WARNING: Missing block: B:86:0x0221, code skipped:
            r15[4] = r8 | r32;
     */
    /* JADX WARNING: Missing block: B:87:0x0227, code skipped:
            if (r1.mEllipsize == 0) goto L_0x0239;
     */
    /* JADX WARNING: Missing block: B:88:0x0229, code skipped:
            r15[5] = r14.getEllipsisStart(r0);
            r15[6] = r14.getEllipsisCount(r0);
     */
    /* JADX WARNING: Missing block: B:90:0x023b, code skipped:
            r33 = r2;
            r1.mInts.insertAt(r3 + r0, r15);
            r1.mObjects.insertAt(r3 + r0, r13);
            r0 = r0 + 1;
            r4 = r27;
            r7 = r29;
            r8 = r31;
            r2 = r33;
     */
    /* JADX WARNING: Missing block: B:91:0x0257, code skipped:
            r33 = r2;
            r27 = r4;
            r29 = r7;
            r31 = r8;
            updateBlocks(r3, r11 - 1, r6);
            r5.finish();
            r2 = sLock;
     */
    /* JADX WARNING: Missing block: B:92:0x0269, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:94:?, code skipped:
            sStaticLayout = r14;
            sBuilder = r5;
     */
    /* JADX WARNING: Missing block: B:95:0x026e, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:96:0x026f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void reflow(CharSequence s, int where, int before, int after) {
        Throwable th;
        CharSequence charSequence;
        int i;
        int i2;
        int i3;
        int i4;
        boolean z;
        int i5;
        if (s == this.mBase) {
            boolean again;
            int before2;
            int st;
            int en;
            CharSequence text = this.mDisplay;
            int len = text.length();
            int find = TextUtils.lastIndexOf(text, 10, where - 1);
            if (find < 0) {
                find = 0;
            } else {
                find++;
            }
            int diff = where - find;
            int before3 = before + diff;
            int after2 = after + diff;
            int where2 = where - diff;
            int look = TextUtils.indexOf(text, 10, where2 + after2);
            if (look < 0) {
                look = len;
            } else {
                look++;
            }
            diff = look - (where2 + after2);
            before3 += diff;
            after2 += diff;
            if (text instanceof Spanned) {
                Spanned sp = (Spanned) text;
                while (true) {
                    CharSequence charSequence2;
                    again = false;
                    Object[] force = sp.getSpans(where2, where2 + after2, WrapTogetherSpan.class);
                    before2 = before3;
                    before3 = where2;
                    where2 = 0;
                    while (where2 < force.length) {
                        st = sp.getSpanStart(force[where2]);
                        en = sp.getSpanEnd(force[where2]);
                        if (st < before3) {
                            again = true;
                            int diff2 = before3 - st;
                            before2 += diff2;
                            after2 += diff2;
                            before3 -= diff2;
                        }
                        if (en > before3 + after2) {
                            int diff3 = en - (before3 + after2);
                            before2 += diff3;
                            after2 += diff3;
                            again = true;
                        }
                        where2++;
                        charSequence2 = s;
                    }
                    if (!again) {
                        break;
                    }
                    where2 = before3;
                    before3 = before2;
                    charSequence2 = s;
                }
            } else {
                before2 = before3;
                before3 = where2;
            }
            int startline = getLineForOffset(before3);
            st = getLineTop(startline);
            where2 = getLineForOffset(before3 + before2);
            if (before3 + after2 == len) {
                where2 = getLineCount();
            }
            int endline = where2;
            en = getLineTop(endline);
            again = endline == getLineCount();
            synchronized (sLock) {
                try {
                    StaticLayout reflowed = sStaticLayout;
                    android.text.StaticLayout.Builder b = sBuilder;
                    try {
                        sStaticLayout = null;
                        sBuilder = null;
                    } catch (Throwable th2) {
                        th = th2;
                        charSequence = text;
                        i = len;
                        i2 = look;
                        i3 = st;
                        i4 = diff;
                        z = again;
                        i5 = before2;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    charSequence = text;
                    i = len;
                    int i6 = find;
                    i2 = look;
                    i3 = st;
                    i4 = diff;
                    z = again;
                    i5 = before2;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    private boolean contentMayProtrudeFromLineTopOrBottom(CharSequence text, int start, int end) {
        boolean z = true;
        if ((text instanceof Spanned) && ((ReplacementSpan[]) ((Spanned) text).getSpans(start, end, ReplacementSpan.class)).length > 0) {
            return true;
        }
        Paint paint = getPaint();
        if (text instanceof PrecomputedText) {
            ((PrecomputedText) text).getBounds(start, end, this.mTempRect);
        } else {
            paint.getTextBounds(text, start, end, this.mTempRect);
        }
        FontMetricsInt fm = paint.getFontMetricsInt();
        if (this.mTempRect.top >= fm.top && this.mTempRect.bottom <= fm.bottom) {
            z = false;
        }
        return z;
    }

    private void createBlocks() {
        int offset = 400;
        int i = 0;
        this.mNumberOfBlocks = 0;
        CharSequence text = this.mDisplay;
        while (true) {
            offset = TextUtils.indexOf(text, 10, offset);
            if (offset < 0) {
                break;
            }
            addBlockAtOffset(offset);
            offset += 400;
        }
        addBlockAtOffset(text.length());
        this.mBlockIndices = new int[this.mBlockEndLines.length];
        while (i < this.mBlockEndLines.length) {
            this.mBlockIndices[i] = -1;
            i++;
        }
    }

    public ArraySet<Integer> getBlocksAlwaysNeedToBeRedrawn() {
        return this.mBlocksAlwaysNeedToBeRedrawn;
    }

    private void updateAlwaysNeedsToBeRedrawn(int blockIndex) {
        int startLine = blockIndex == 0 ? 0 : this.mBlockEndLines[blockIndex - 1] + 1;
        int endLine = this.mBlockEndLines[blockIndex];
        for (int i = startLine; i <= endLine; i++) {
            if (getContentMayProtrudeFromTopOrBottom(i)) {
                if (this.mBlocksAlwaysNeedToBeRedrawn == null) {
                    this.mBlocksAlwaysNeedToBeRedrawn = new ArraySet();
                }
                this.mBlocksAlwaysNeedToBeRedrawn.add(Integer.valueOf(blockIndex));
                return;
            }
        }
        if (this.mBlocksAlwaysNeedToBeRedrawn != null) {
            this.mBlocksAlwaysNeedToBeRedrawn.remove(Integer.valueOf(blockIndex));
        }
    }

    private void addBlockAtOffset(int offset) {
        int line = getLineForOffset(offset);
        if (this.mBlockEndLines == null) {
            this.mBlockEndLines = ArrayUtils.newUnpaddedIntArray(1);
            this.mBlockEndLines[this.mNumberOfBlocks] = line;
            updateAlwaysNeedsToBeRedrawn(this.mNumberOfBlocks);
            this.mNumberOfBlocks++;
            return;
        }
        if (line > this.mBlockEndLines[this.mNumberOfBlocks - 1]) {
            this.mBlockEndLines = GrowingArrayUtils.append(this.mBlockEndLines, this.mNumberOfBlocks, line);
            updateAlwaysNeedsToBeRedrawn(this.mNumberOfBlocks);
            this.mNumberOfBlocks++;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void updateBlocks(int startLine, int endLine, int newLineCount) {
        int i = startLine;
        int i2 = endLine;
        if (this.mBlockEndLines == null) {
            createBlocks();
            return;
        }
        int i3;
        int firstBlock = -1;
        int lastBlock = -1;
        for (i3 = 0; i3 < this.mNumberOfBlocks; i3++) {
            if (this.mBlockEndLines[i3] >= i) {
                firstBlock = i3;
                break;
            }
        }
        for (i3 = firstBlock; i3 < this.mNumberOfBlocks; i3++) {
            if (this.mBlockEndLines[i3] >= i2) {
                lastBlock = i3;
                break;
            }
        }
        i3 = this.mBlockEndLines[lastBlock];
        boolean createBlockBefore = i > (firstBlock == 0 ? 0 : this.mBlockEndLines[firstBlock + -1] + 1);
        boolean createBlock = newLineCount > 0;
        boolean createBlockAfter = i2 < this.mBlockEndLines[lastBlock];
        int numAddedBlocks = 0;
        if (createBlockBefore) {
            numAddedBlocks = 0 + 1;
        }
        if (createBlock) {
            numAddedBlocks++;
        }
        if (createBlockAfter) {
            numAddedBlocks++;
        }
        int numRemovedBlocks = (lastBlock - firstBlock) + 1;
        int newNumberOfBlocks = (this.mNumberOfBlocks + numAddedBlocks) - numRemovedBlocks;
        if (newNumberOfBlocks == 0) {
            this.mBlockEndLines[0] = 0;
            this.mBlockIndices[0] = -1;
            this.mNumberOfBlocks = 1;
            return;
        }
        int[] blockEndLines;
        int lastBlockEndLine;
        boolean createBlockAfter2;
        boolean createBlock2;
        int newFirstChangedBlock;
        if (newNumberOfBlocks > this.mBlockEndLines.length) {
            blockEndLines = ArrayUtils.newUnpaddedIntArray(Math.max(this.mBlockEndLines.length * 2, newNumberOfBlocks));
            int[] blockIndices = new int[blockEndLines.length];
            lastBlockEndLine = i3;
            System.arraycopy(this.mBlockEndLines, 0, blockEndLines, 0, firstBlock);
            System.arraycopy(this.mBlockIndices, 0, blockIndices, 0, firstBlock);
            createBlockAfter2 = createBlockAfter;
            createBlock2 = createBlock;
            System.arraycopy(this.mBlockEndLines, lastBlock + 1, blockEndLines, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
            System.arraycopy(this.mBlockIndices, lastBlock + 1, blockIndices, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
            this.mBlockEndLines = blockEndLines;
            this.mBlockIndices = blockIndices;
        } else {
            lastBlockEndLine = i3;
            createBlock2 = createBlock;
            createBlockAfter2 = createBlockAfter;
            if (numAddedBlocks + numRemovedBlocks != 0) {
                System.arraycopy(this.mBlockEndLines, lastBlock + 1, this.mBlockEndLines, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
                System.arraycopy(this.mBlockIndices, lastBlock + 1, this.mBlockIndices, firstBlock + numAddedBlocks, (this.mNumberOfBlocks - lastBlock) - 1);
            }
        }
        if (numAddedBlocks + numRemovedBlocks == 0 || this.mBlocksAlwaysNeedToBeRedrawn == null) {
        } else {
            ArraySet<Integer> set = new ArraySet();
            i3 = numAddedBlocks - numRemovedBlocks;
            int i4 = 0;
            while (true) {
                int i5 = i4;
                if (i5 >= this.mBlocksAlwaysNeedToBeRedrawn.size()) {
                    break;
                }
                int lastBlock2;
                Integer block = (Integer) this.mBlocksAlwaysNeedToBeRedrawn.valueAt(i5);
                if (block.intValue() < firstBlock) {
                    set.add(block);
                }
                if (block.intValue() > lastBlock) {
                    block = Integer.valueOf(block.intValue() + i3);
                    set.add(block);
                }
                if (block.intValue() >= 0 || !HwPCUtils.enabledInPad() || this.mBlockIndices == null) {
                    lastBlock2 = lastBlock;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    lastBlock2 = lastBlock;
                    stringBuilder.append("block : ");
                    stringBuilder.append(block);
                    stringBuilder.append(" numAddedBlocks : ");
                    stringBuilder.append(numAddedBlocks);
                    stringBuilder.append(" numRemovedBlocks : ");
                    stringBuilder.append(numRemovedBlocks);
                    stringBuilder.append(" mBlocksAlwaysNeedToBeRedrawn size : ");
                    stringBuilder.append(this.mBlocksAlwaysNeedToBeRedrawn.size());
                    stringBuilder.append(" mBlockIndices.length :");
                    stringBuilder.append(this.mBlockIndices.length);
                    stringBuilder.append(" pid : ");
                    stringBuilder.append(Process.myPid());
                    Log.e(str, stringBuilder.toString());
                }
                i4 = i5 + 1;
                lastBlock = lastBlock2;
            }
            this.mBlocksAlwaysNeedToBeRedrawn = set;
        }
        this.mNumberOfBlocks = newNumberOfBlocks;
        lastBlock = newLineCount - ((i2 - i) + 1);
        if (lastBlock != 0) {
            newFirstChangedBlock = firstBlock + numAddedBlocks;
            for (i3 = newFirstChangedBlock; i3 < this.mNumberOfBlocks; i3++) {
                blockEndLines = this.mBlockEndLines;
                blockEndLines[i3] = blockEndLines[i3] + lastBlock;
            }
        } else {
            newFirstChangedBlock = this.mNumberOfBlocks;
        }
        this.mIndexFirstChangedBlock = Math.min(this.mIndexFirstChangedBlock, newFirstChangedBlock);
        i3 = firstBlock;
        if (createBlockBefore) {
            this.mBlockEndLines[i3] = i - 1;
            updateAlwaysNeedsToBeRedrawn(i3);
            this.mBlockIndices[i3] = -1;
            i3++;
        }
        if (createBlock2) {
            this.mBlockEndLines[i3] = (i + newLineCount) - 1;
            updateAlwaysNeedsToBeRedrawn(i3);
            this.mBlockIndices[i3] = -1;
            i3++;
        }
        if (createBlockAfter2) {
            this.mBlockEndLines[i3] = lastBlockEndLine + lastBlock;
            updateAlwaysNeedsToBeRedrawn(i3);
            this.mBlockIndices[i3] = -1;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void setBlocksDataForTest(int[] blockEndLines, int[] blockIndices, int numberOfBlocks, int totalLines) {
        this.mBlockEndLines = new int[blockEndLines.length];
        this.mBlockIndices = new int[blockIndices.length];
        System.arraycopy(blockEndLines, 0, this.mBlockEndLines, 0, blockEndLines.length);
        System.arraycopy(blockIndices, 0, this.mBlockIndices, 0, blockIndices.length);
        this.mNumberOfBlocks = numberOfBlocks;
        while (this.mInts.size() < totalLines) {
            this.mInts.insertAt(this.mInts.size(), new int[5]);
        }
    }

    public int[] getBlockEndLines() {
        return this.mBlockEndLines;
    }

    public int[] getBlockIndices() {
        return this.mBlockIndices;
    }

    public int getBlockIndex(int index) {
        return this.mBlockIndices[index];
    }

    public void setBlockIndex(int index, int blockIndex) {
        this.mBlockIndices[index] = blockIndex;
    }

    public int getNumberOfBlocks() {
        return this.mNumberOfBlocks;
    }

    public int getIndexFirstChangedBlock() {
        return this.mIndexFirstChangedBlock;
    }

    public void setIndexFirstChangedBlock(int i) {
        this.mIndexFirstChangedBlock = i;
    }

    public int getLineCount() {
        return this.mInts.size() - 1;
    }

    public int getLineTop(int line) {
        return this.mInts.getValue(line, 1);
    }

    public int getLineDescent(int line) {
        return this.mInts.getValue(line, 2);
    }

    public int getLineExtra(int line) {
        return this.mInts.getValue(line, 3);
    }

    public int getLineStart(int line) {
        return this.mInts.getValue(line, 0) & START_MASK;
    }

    public boolean getLineContainsTab(int line) {
        return (this.mInts.getValue(line, 0) & 536870912) != 0;
    }

    public int getParagraphDirection(int line) {
        return this.mInts.getValue(line, 0) >> 30;
    }

    public final Directions getLineDirections(int line) {
        return (Directions) this.mObjects.getValue(line, 0);
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    public int getBottomPadding() {
        return this.mBottomPadding;
    }

    public int getHyphen(int line) {
        return this.mInts.getValue(line, 4) & 255;
    }

    private boolean getContentMayProtrudeFromTopOrBottom(int line) {
        return (this.mInts.getValue(line, 4) & 256) != 0;
    }

    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    public int getEllipsisStart(int line) {
        if (this.mEllipsizeAt == null) {
            return 0;
        }
        return this.mInts.getValue(line, 5);
    }

    public int getEllipsisCount(int line) {
        if (this.mEllipsizeAt == null) {
            return 0;
        }
        return this.mInts.getValue(line, 6);
    }
}
