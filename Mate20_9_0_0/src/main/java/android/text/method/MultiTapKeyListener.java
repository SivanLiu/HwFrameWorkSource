package android.text.method;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.method.TextKeyListener.Capitalize;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;

public class MultiTapKeyListener extends BaseKeyListener implements SpanWatcher {
    private static MultiTapKeyListener[] sInstance = new MultiTapKeyListener[(Capitalize.values().length * 2)];
    private static final SparseArray<String> sRecs = new SparseArray();
    private boolean mAutoText;
    private Capitalize mCapitalize;

    private class Timeout extends Handler implements Runnable {
        private Editable mBuffer;

        public Timeout(Editable buffer) {
            this.mBuffer = buffer;
            this.mBuffer.setSpan(this, 0, this.mBuffer.length(), 18);
            postAtTime(this, SystemClock.uptimeMillis() + 2000);
        }

        public void run() {
            Spannable buf = this.mBuffer;
            if (buf != null) {
                int st = Selection.getSelectionStart(buf);
                int en = Selection.getSelectionEnd(buf);
                int start = buf.getSpanStart(TextKeyListener.ACTIVE);
                int end = buf.getSpanEnd(TextKeyListener.ACTIVE);
                if (st == start && en == end) {
                    Selection.setSelection(buf, Selection.getSelectionEnd(buf));
                }
                buf.removeSpan(this);
            }
        }
    }

    static {
        sRecs.put(8, ".,1!@#$%^&*:/?'=()");
        sRecs.put(9, "abc2ABC");
        sRecs.put(10, "def3DEF");
        sRecs.put(11, "ghi4GHI");
        sRecs.put(12, "jkl5JKL");
        sRecs.put(13, "mno6MNO");
        sRecs.put(14, "pqrs7PQRS");
        sRecs.put(15, "tuv8TUV");
        sRecs.put(16, "wxyz9WXYZ");
        sRecs.put(7, "0+");
        sRecs.put(18, WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
    }

    public MultiTapKeyListener(Capitalize cap, boolean autotext) {
        this.mCapitalize = cap;
        this.mAutoText = autotext;
    }

    public static MultiTapKeyListener getInstance(boolean autotext, Capitalize cap) {
        int off = (cap.ordinal() * 2) + autotext;
        if (sInstance[off] == null) {
            sInstance[off] = new MultiTapKeyListener(cap, autotext);
        }
        return sInstance[off];
    }

    public int getInputType() {
        return BaseKeyListener.makeTextContentType(this.mCapitalize, this.mAutoText);
    }

    /* JADX WARNING: Removed duplicated region for block: B:56:0x0180  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x00e9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        Editable editable = content;
        int i = keyCode;
        int pref = 0;
        if (view != null) {
            pref = TextKeyListener.getInstance().getPrefs(view.getContext());
        }
        int pref2 = pref;
        pref = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        int rec = Math.min(pref, b);
        int selEnd = Math.max(pref, b);
        int activeStart = editable.getSpanStart(TextKeyListener.ACTIVE);
        int activeEnd = editable.getSpanEnd(TextKeyListener.ACTIVE);
        int rec2 = (editable.getSpanFlags(TextKeyListener.ACTIVE) & -16777216) >>> 24;
        boolean z;
        if (activeStart == rec && activeEnd == selEnd && selEnd - rec == 1 && rec2 >= 0 && rec2 < sRecs.size()) {
            Timeout timeout;
            int ix;
            if (i == 17) {
                char current = editable.charAt(rec);
                if (Character.isLowerCase(current)) {
                    editable.replace(rec, selEnd, String.valueOf(current).toUpperCase());
                    removeTimeouts(content);
                    timeout = new Timeout(editable);
                    return true;
                } else if (Character.isUpperCase(current)) {
                    editable.replace(rec, selEnd, String.valueOf(current).toLowerCase());
                    removeTimeouts(content);
                    timeout = new Timeout(editable);
                    return true;
                }
            }
            if (sRecs.indexOfKey(i) == rec2) {
                String val = (String) sRecs.valueAt(rec2);
                char ch = editable.charAt(rec);
                b = val.indexOf(ch);
                if (b >= 0) {
                    ix = (b + 1) % val.length();
                    z = true;
                    editable.replace(rec, selEnd, val, ix, ix + 1);
                    removeTimeouts(content);
                    timeout = new Timeout(editable);
                    return z;
                }
            }
            z = true;
            b = sRecs.indexOfKey(i);
            if (b >= 0) {
                Selection.setSelection(editable, selEnd, selEnd);
                rec = b;
                rec2 = selEnd;
                if (rec < 0) {
                    String val2 = (String) sRecs.valueAt(rec);
                    b = 0;
                    if ((pref2 & 1) != 0 && TextKeyListener.shouldCap(this.mCapitalize, editable, rec2)) {
                        for (int i2 = 0; i2 < val2.length(); i2++) {
                            if (Character.isUpperCase(val2.charAt(i2))) {
                                b = i2;
                                break;
                            }
                        }
                    }
                    ix = b;
                    if (rec2 != selEnd) {
                        Selection.setSelection(editable, selEnd);
                    }
                    editable.setSpan(OLD_SEL_START, rec2, rec2, 17);
                    pref2 = 0;
                    editable.replace(rec2, selEnd, val2, ix, ix + 1);
                    pref = editable.getSpanStart(OLD_SEL_START);
                    b = Selection.getSelectionEnd(content);
                    if (b != pref) {
                        Selection.setSelection(editable, pref, b);
                        editable.setSpan(TextKeyListener.LAST_TYPED, pref, b, 33);
                        editable.setSpan(TextKeyListener.ACTIVE, pref, b, 33 | (rec << 24));
                    }
                    removeTimeouts(content);
                    Timeout timeout2 = new Timeout(editable);
                    if (editable.getSpanStart(this) < 0) {
                        KeyListener[] methods = (KeyListener[]) editable.getSpans(pref2, content.length(), KeyListener.class);
                        int length = methods.length;
                        for (int i3 = pref2; i3 < length; i3++) {
                            editable.removeSpan(methods[i3]);
                        }
                        editable.setSpan(this, pref2, content.length(), 18);
                    }
                    return z;
                }
                return super.onKeyDown(view, content, keyCode, event);
            }
        }
        z = true;
        b = sRecs.indexOfKey(i);
        rec2 = rec;
        rec = b;
        if (rec < 0) {
        }
    }

    public void onSpanChanged(Spannable buf, Object what, int s, int e, int start, int stop) {
        if (what == Selection.SELECTION_END) {
            buf.removeSpan(TextKeyListener.ACTIVE);
            removeTimeouts(buf);
        }
    }

    private static void removeTimeouts(Spannable buf) {
        int i = 0;
        Timeout[] timeout = (Timeout[]) buf.getSpans(0, buf.length(), Timeout.class);
        while (true) {
            int i2 = i;
            if (i2 < timeout.length) {
                Timeout t = timeout[i2];
                t.removeCallbacks(t);
                t.mBuffer = null;
                buf.removeSpan(t);
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    public void onSpanAdded(Spannable s, Object what, int start, int end) {
    }

    public void onSpanRemoved(Spannable s, Object what, int start, int end) {
    }
}
