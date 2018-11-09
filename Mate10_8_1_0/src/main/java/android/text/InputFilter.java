package android.text;

import com.android.internal.util.Preconditions;
import java.util.Locale;

public interface InputFilter {

    public static class AllCaps implements InputFilter {
        private final Locale mLocale;

        private static class CharSequenceWrapper implements CharSequence, Spanned {
            private final int mEnd;
            private final int mLength;
            private final CharSequence mSource;
            private final int mStart;

            CharSequenceWrapper(CharSequence source, int start, int end) {
                this.mSource = source;
                this.mStart = start;
                this.mEnd = end;
                this.mLength = end - start;
            }

            public int length() {
                return this.mLength;
            }

            public char charAt(int index) {
                if (index >= 0 && index < this.mLength) {
                    return this.mSource.charAt(this.mStart + index);
                }
                throw new IndexOutOfBoundsException();
            }

            public CharSequence subSequence(int start, int end) {
                if (start >= 0 && end >= 0 && end <= this.mLength && start <= end) {
                    return new CharSequenceWrapper(this.mSource, this.mStart + start, this.mStart + end);
                }
                throw new IndexOutOfBoundsException();
            }

            public String toString() {
                return this.mSource.subSequence(this.mStart, this.mEnd).toString();
            }

            public <T> T[] getSpans(int start, int end, Class<T> type) {
                return ((Spanned) this.mSource).getSpans(this.mStart + start, this.mStart + end, type);
            }

            public int getSpanStart(Object tag) {
                return ((Spanned) this.mSource).getSpanStart(tag) - this.mStart;
            }

            public int getSpanEnd(Object tag) {
                return ((Spanned) this.mSource).getSpanEnd(tag) - this.mStart;
            }

            public int getSpanFlags(Object tag) {
                return ((Spanned) this.mSource).getSpanFlags(tag);
            }

            public int nextSpanTransition(int start, int limit, Class type) {
                return ((Spanned) this.mSource).nextSpanTransition(this.mStart + start, this.mStart + limit, type) - this.mStart;
            }
        }

        public AllCaps() {
            this.mLocale = null;
        }

        public AllCaps(Locale locale) {
            Preconditions.checkNotNull(locale);
            this.mLocale = locale;
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            CharSequence wrapper = new CharSequenceWrapper(source, start, end);
            boolean lowerOrTitleFound = false;
            int length = end - start;
            int i = 0;
            while (i < length) {
                int cp = Character.codePointAt(wrapper, i);
                if (Character.isLowerCase(cp) || Character.isTitleCase(cp)) {
                    lowerOrTitleFound = true;
                    break;
                }
                i += Character.charCount(cp);
            }
            if (!lowerOrTitleFound) {
                return null;
            }
            boolean copySpans = source instanceof Spanned;
            CharSequence upper = TextUtils.toUpperCase(this.mLocale, wrapper, copySpans);
            if (upper == wrapper) {
                return null;
            }
            return copySpans ? new SpannableString(upper) : upper.toString();
        }
    }

    public static class LengthFilter implements InputFilter {
        private boolean mDisableFilter;
        private final int mMax;

        public LengthFilter(int max) {
            this.mMax = max;
        }

        public void disableFilter() {
            this.mDisableFilter = true;
        }

        public void enableFilter() {
            this.mDisableFilter = false;
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (this.mDisableFilter) {
                return null;
            }
            int keep = this.mMax - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                return "";
            }
            if (keep >= end - start) {
                return null;
            }
            keep += start;
            if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                keep--;
                if (keep == start) {
                    return "";
                }
            }
            return source.subSequence(start, keep);
        }

        public int getMax() {
            return this.mMax;
        }
    }

    CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4);
}
