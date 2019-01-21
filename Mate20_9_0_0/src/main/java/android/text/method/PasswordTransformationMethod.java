package android.text.method;

import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.GetChars;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.UpdateLayout;
import android.view.View;
import java.lang.ref.WeakReference;

public class PasswordTransformationMethod implements TransformationMethod, TextWatcher {
    private static final char ARABIC_DOT = '٭';
    private static char DOT = ENGLISH_DOT;
    private static final char ENGLISH_DOT = '•';
    private static final char FIRST_RIGHT_TO_LEFT = '֐';
    private static final char LAST_RIGHT_TO_LEFT = 'ۿ';
    private static PasswordTransformationMethod sInstance;

    private static class PasswordCharSequence implements CharSequence, GetChars {
        private CharSequence mSource;

        public PasswordCharSequence(CharSequence source) {
            this.mSource = source;
        }

        public int length() {
            return this.mSource.length();
        }

        public char charAt(int i) {
            if (this.mSource instanceof Spanned) {
                Spanned sp = this.mSource;
                int st = sp.getSpanStart(TextKeyListener.ACTIVE);
                int en = sp.getSpanEnd(TextKeyListener.ACTIVE);
                if (i < st || i >= en) {
                    int a = 0;
                    Visible[] visible = (Visible[]) sp.getSpans(0, sp.length(), Visible.class);
                    while (true) {
                        int a2 = a;
                        if (a2 >= visible.length) {
                            break;
                        }
                        if (sp.getSpanStart(visible[a2].mTransformer) >= 0) {
                            st = sp.getSpanStart(visible[a2]);
                            en = sp.getSpanEnd(visible[a2]);
                            if (i >= st && i < en) {
                                return this.mSource.charAt(i);
                            }
                        }
                        a = a2 + 1;
                    }
                } else {
                    return this.mSource.charAt(i);
                }
            }
            return PasswordTransformationMethod.DOT;
        }

        public CharSequence subSequence(int start, int end) {
            char[] buf = new char[(end - start)];
            getChars(start, end, buf, 0);
            return new String(buf);
        }

        public String toString() {
            return subSequence(0, length()).toString();
        }

        public void getChars(int start, int end, char[] dest, int off) {
            int i;
            TextUtils.getChars(this.mSource, start, end, dest, off);
            if (this.mSource.length() > 0) {
                char c = this.mSource.charAt(this.mSource.length() - 1);
                if (c < PasswordTransformationMethod.FIRST_RIGHT_TO_LEFT || c > PasswordTransformationMethod.LAST_RIGHT_TO_LEFT) {
                    PasswordTransformationMethod.DOT = PasswordTransformationMethod.ENGLISH_DOT;
                } else {
                    PasswordTransformationMethod.DOT = PasswordTransformationMethod.ARABIC_DOT;
                }
            }
            int st = -1;
            int en = -1;
            int nvisible = 0;
            int[] starts = null;
            int[] ends = null;
            if (this.mSource instanceof Spanned) {
                Spanned sp = this.mSource;
                st = sp.getSpanStart(TextKeyListener.ACTIVE);
                en = sp.getSpanEnd(TextKeyListener.ACTIVE);
                Visible[] visible = (Visible[]) sp.getSpans(0, sp.length(), Visible.class);
                nvisible = visible.length;
                starts = new int[nvisible];
                ends = new int[nvisible];
                for (i = 0; i < nvisible; i++) {
                    if (sp.getSpanStart(visible[i].mTransformer) >= 0) {
                        starts[i] = sp.getSpanStart(visible[i]);
                        ends[i] = sp.getSpanEnd(visible[i]);
                    }
                }
            }
            int i2 = start;
            while (i2 < end) {
                if (i2 < st || i2 >= en) {
                    boolean visible2 = false;
                    i = 0;
                    while (i < nvisible) {
                        if (i2 >= starts[i] && i2 < ends[i]) {
                            visible2 = true;
                            break;
                        }
                        i++;
                    }
                    if (!visible2) {
                        dest[(i2 - start) + off] = PasswordTransformationMethod.DOT;
                    }
                }
                i2++;
            }
        }
    }

    private static class ViewReference extends WeakReference<View> implements NoCopySpan {
        public ViewReference(View v) {
            super(v);
        }
    }

    private static class Visible extends Handler implements UpdateLayout, Runnable {
        private Spannable mText;
        private PasswordTransformationMethod mTransformer;

        public Visible(Spannable sp, PasswordTransformationMethod ptm) {
            this.mText = sp;
            this.mTransformer = ptm;
            postAtTime(this, SystemClock.uptimeMillis() + 1500);
        }

        public void run() {
            this.mText.removeSpan(this);
        }
    }

    public CharSequence getTransformation(CharSequence source, View view) {
        if (source instanceof Spannable) {
            Spannable sp = (Spannable) source;
            ViewReference[] vr = (ViewReference[]) sp.getSpans(0, sp.length(), ViewReference.class);
            for (Object removeSpan : vr) {
                sp.removeSpan(removeSpan);
            }
            removeVisibleSpans(sp);
            sp.setSpan(new ViewReference(view), 0, 0, 34);
        }
        return new PasswordCharSequence(source);
    }

    public static PasswordTransformationMethod getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        sInstance = new PasswordTransformationMethod();
        return sInstance;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s instanceof Spannable) {
            Spannable sp = (Spannable) s;
            int i = 0;
            ViewReference[] vr = (ViewReference[]) sp.getSpans(0, s.length(), ViewReference.class);
            if (vr.length != 0) {
                View v = null;
                while (v == null && i < vr.length) {
                    v = (View) vr[i].get();
                    i++;
                }
                if (!(v == null || (TextKeyListener.getInstance().getPrefs(v.getContext()) & 8) == 0 || count <= 0)) {
                    removeVisibleSpans(sp);
                    if (count == 1) {
                        sp.setSpan(new Visible(sp, this), start, start + count, 33);
                    }
                }
            }
        }
    }

    public void afterTextChanged(Editable s) {
    }

    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
        if (!focused && (sourceText instanceof Spannable)) {
            removeVisibleSpans((Spannable) sourceText);
        }
    }

    private static void removeVisibleSpans(Spannable sp) {
        int i = 0;
        Visible[] old = (Visible[]) sp.getSpans(0, sp.length(), Visible.class);
        while (true) {
            int i2 = i;
            if (i2 < old.length) {
                sp.removeSpan(old[i2]);
                i = i2 + 1;
            } else {
                return;
            }
        }
    }
}
