package android.view.inputmethod;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Editable.Factory;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.util.JlogConstants;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;

public class BaseInputConnection implements InputConnection {
    static final Object COMPOSING = new ComposingText();
    private static final boolean DEBUG = false;
    private static int INVALID_INDEX = -1;
    private static final String TAG = "BaseInputConnection";
    private Object[] mDefaultComposingSpans;
    final boolean mDummyMode;
    Editable mEditable;
    protected final InputMethodManager mIMM;
    KeyCharacterMap mKeyCharacterMap;
    final View mTargetView;

    BaseInputConnection(InputMethodManager mgr, boolean fullEditor) {
        this.mIMM = mgr;
        this.mTargetView = null;
        this.mDummyMode = fullEditor ^ 1;
    }

    public BaseInputConnection(View targetView, boolean fullEditor) {
        this.mIMM = (InputMethodManager) targetView.getContext().getSystemService("input_method");
        this.mTargetView = targetView;
        this.mDummyMode = fullEditor ^ 1;
    }

    public static final void removeComposingSpans(Spannable text) {
        text.removeSpan(COMPOSING);
        Object[] sps = text.getSpans(0, text.length(), Object.class);
        if (sps != null) {
            for (int i = sps.length - 1; i >= 0; i--) {
                Object o = sps[i];
                if ((text.getSpanFlags(o) & 256) != 0) {
                    text.removeSpan(o);
                }
            }
        }
    }

    public static void setComposingSpans(Spannable text) {
        setComposingSpans(text, 0, text.length());
    }

    public static void setComposingSpans(Spannable text, int start, int end) {
        Object[] sps = text.getSpans(start, end, Object.class);
        if (sps != null) {
            for (int i = sps.length - 1; i >= 0; i--) {
                Object o = sps[i];
                if (o == COMPOSING) {
                    text.removeSpan(o);
                } else {
                    int fl = text.getSpanFlags(o);
                    if ((fl & 307) != JlogConstants.JLID_CAMERA3_TAF_CMD) {
                        text.setSpan(o, text.getSpanStart(o), text.getSpanEnd(o), ((fl & -52) | 256) | 33);
                    }
                }
            }
        }
        text.setSpan(COMPOSING, start, end, JlogConstants.JLID_CAMERA3_TAF_CMD);
    }

    public static int getComposingSpanStart(Spannable text) {
        return text.getSpanStart(COMPOSING);
    }

    public static int getComposingSpanEnd(Spannable text) {
        return text.getSpanEnd(COMPOSING);
    }

    public Editable getEditable() {
        if (this.mEditable == null) {
            this.mEditable = Factory.getInstance().newEditable("");
            Selection.setSelection(this.mEditable, 0);
        }
        return this.mEditable;
    }

    public boolean beginBatchEdit() {
        return false;
    }

    public boolean endBatchEdit() {
        return false;
    }

    public void closeConnection() {
        finishComposingText();
    }

    public boolean clearMetaKeyStates(int states) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        MetaKeyKeyListener.clearMetaKeyState(content, states);
        return true;
    }

    public boolean commitCompletion(CompletionInfo text) {
        return false;
    }

    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return false;
    }

    public boolean commitText(CharSequence text, int newCursorPosition) {
        replaceText(text, newCursorPosition, false);
        sendCurrentText();
        return true;
    }

    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        beginBatchEdit();
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a < 0 || b < 0) {
            return false;
        }
        int tmp;
        int tmp2;
        int start;
        if (a > b) {
            tmp = a;
            a = b;
            b = tmp;
        }
        tmp = getComposingSpanStart(content);
        int cb = getComposingSpanEnd(content);
        if (cb < tmp) {
            tmp2 = tmp;
            tmp = cb;
            cb = tmp2;
        }
        if (!(tmp == -1 || cb == -1)) {
            if (tmp < a) {
                a = tmp;
            }
            if (cb > b) {
                b = cb;
            }
        }
        tmp2 = 0;
        if (beforeLength > 0) {
            start = a - beforeLength;
            if (start < 0) {
                start = 0;
            }
            content.delete(start, a);
            tmp2 = a - start;
        }
        if (afterLength > 0) {
            b -= tmp2;
            start = b + afterLength;
            if (start > content.length()) {
                start = content.length();
            }
            content.delete(b, start);
        }
        endBatchEdit();
        return true;
    }

    private static int findIndexBackward(CharSequence cs, int from, int numCodePoints) {
        boolean currentIndex = from;
        boolean N = cs.length();
        if (currentIndex >= false || N < currentIndex) {
            return INVALID_INDEX;
        }
        if (numCodePoints < 0) {
            return INVALID_INDEX;
        }
        boolean waitingHighSurrogate = false;
        boolean waitingHighSurrogate2 = currentIndex;
        int currentIndex2 = numCodePoints;
        while (currentIndex2 != 0) {
            waitingHighSurrogate2--;
            if (waitingHighSurrogate2 < false) {
                char c = cs.charAt(waitingHighSurrogate2);
                if (waitingHighSurrogate) {
                    if (!Character.isHighSurrogate(c)) {
                        return INVALID_INDEX;
                    }
                    waitingHighSurrogate = false;
                    currentIndex2--;
                } else if (!Character.isSurrogate(c)) {
                    currentIndex2--;
                } else if (Character.isHighSurrogate(c)) {
                    return INVALID_INDEX;
                } else {
                    waitingHighSurrogate = true;
                }
            } else if (waitingHighSurrogate) {
                return INVALID_INDEX;
            } else {
                return 0;
            }
        }
        return waitingHighSurrogate2;
    }

    private static int findIndexForward(CharSequence cs, int from, int numCodePoints) {
        boolean currentIndex = from;
        boolean N = cs.length();
        if (currentIndex >= false || N < currentIndex) {
            return INVALID_INDEX;
        }
        if (numCodePoints < 0) {
            return INVALID_INDEX;
        }
        boolean waitingLowSurrogate = false;
        boolean waitingLowSurrogate2 = currentIndex;
        int currentIndex2 = numCodePoints;
        while (currentIndex2 != 0) {
            if (waitingLowSurrogate2 < N) {
                char c = cs.charAt(waitingLowSurrogate2);
                if (waitingLowSurrogate) {
                    if (!Character.isLowSurrogate(c)) {
                        return INVALID_INDEX;
                    }
                    currentIndex2--;
                    waitingLowSurrogate = false;
                    waitingLowSurrogate2++;
                } else if (!Character.isSurrogate(c)) {
                    currentIndex2--;
                    waitingLowSurrogate2++;
                } else if (Character.isLowSurrogate(c)) {
                    return INVALID_INDEX;
                } else {
                    waitingLowSurrogate = true;
                    waitingLowSurrogate2++;
                }
            } else if (waitingLowSurrogate) {
                return INVALID_INDEX;
            } else {
                return N;
            }
        }
        return waitingLowSurrogate2;
    }

    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        int tmp;
        int tmp2;
        beginBatchEdit();
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            tmp = a;
            a = b;
            b = tmp;
        }
        tmp = getComposingSpanStart(content);
        int cb = getComposingSpanEnd(content);
        if (cb < tmp) {
            tmp2 = tmp;
            tmp = cb;
            cb = tmp2;
        }
        if (!(tmp == -1 || cb == -1)) {
            if (tmp < a) {
                a = tmp;
            }
            if (cb > b) {
                b = cb;
            }
        }
        if (a >= 0 && b >= 0) {
            tmp2 = findIndexBackward(content, a, Math.max(beforeLength, 0));
            if (tmp2 != INVALID_INDEX) {
                int end = findIndexForward(content, b, Math.max(afterLength, 0));
                if (end != INVALID_INDEX) {
                    int numDeleteBefore = a - tmp2;
                    if (numDeleteBefore > 0) {
                        content.delete(tmp2, a);
                    }
                    if (end - b > 0) {
                        content.delete(b - numDeleteBefore, end - numDeleteBefore);
                    }
                }
            }
        }
        endBatchEdit();
        return true;
    }

    public boolean finishComposingText() {
        Editable content = getEditable();
        if (content != null) {
            beginBatchEdit();
            removeComposingSpans(content);
            sendCurrentText();
            endBatchEdit();
        }
        return true;
    }

    public int getCursorCapsMode(int reqModes) {
        if (this.mDummyMode) {
            return 0;
        }
        Editable content = getEditable();
        if (content == null) {
            return 0;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        return TextUtils.getCapsMode(content, a, reqModes);
    }

    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        return null;
    }

    public CharSequence getTextBeforeCursor(int length, int flags) {
        Editable content = getEditable();
        if (content == null) {
            return null;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        if (a <= 0) {
            return "";
        }
        if (length > a) {
            length = a;
        }
        if ((flags & 1) != 0) {
            return content.subSequence(a - length, a);
        }
        return TextUtils.substring(content, a - length, a);
    }

    public CharSequence getSelectedText(int flags) {
        Editable content = getEditable();
        if (content == null) {
            return null;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        if (a == b || a < 0) {
            return null;
        }
        if ((flags & 1) != 0) {
            return content.subSequence(a, b);
        }
        return TextUtils.substring(content, a, b);
    }

    public CharSequence getTextAfterCursor(int length, int flags) {
        Editable content = getEditable();
        if (content == null) {
            return null;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        if (b < 0) {
            b = 0;
        }
        if (b + length > content.length()) {
            length = content.length() - b;
        }
        if ((flags & 1) != 0) {
            return content.subSequence(b, b + length);
        }
        return TextUtils.substring(content, b, b + length);
    }

    public boolean performEditorAction(int actionCode) {
        long eventTime = SystemClock.uptimeMillis();
        long j = eventTime;
        sendKeyEvent(new KeyEvent(eventTime, j, 0, 66, 0, 0, -1, 0, 22));
        sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), j, 1, 66, 0, 0, -1, 0, 22));
        return true;
    }

    public boolean performContextMenuAction(int id) {
        return false;
    }

    public boolean performPrivateCommand(String action, Bundle data) {
        return false;
    }

    public boolean requestCursorUpdates(int cursorUpdateMode) {
        return false;
    }

    public Handler getHandler() {
        return null;
    }

    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        replaceText(text, newCursorPosition, true);
        return true;
    }

    public boolean setComposingRegion(int start, int end) {
        Editable content = getEditable();
        if (content != null) {
            int tmp;
            beginBatchEdit();
            removeComposingSpans(content);
            int a = start;
            int b = end;
            if (a > b) {
                tmp = a;
                a = b;
                b = tmp;
            }
            tmp = content.length();
            if (a < 0) {
                a = 0;
            }
            if (b < 0) {
                b = 0;
            }
            if (a > tmp) {
                a = tmp;
            }
            if (b > tmp) {
                b = tmp;
            }
            ensureDefaultComposingSpans();
            if (this.mDefaultComposingSpans != null) {
                for (Object span : this.mDefaultComposingSpans) {
                    content.setSpan(span, a, b, JlogConstants.JLID_CAMERA3_TAF_CMD);
                }
            }
            content.setSpan(COMPOSING, a, b, JlogConstants.JLID_CAMERA3_TAF_CMD);
            sendCurrentText();
            endBatchEdit();
        }
        return true;
    }

    public boolean setSelection(int start, int end) {
        CharSequence content = getEditable();
        if (content == null) {
            return false;
        }
        int len = content.length();
        if (start > len || end > len || start < 0 || end < 0) {
            return true;
        }
        if (start != end || MetaKeyKeyListener.getMetaState(content, 2048) == 0) {
            Selection.setSelection(content, start, end);
        } else {
            Selection.extendSelection(content, start);
        }
        return true;
    }

    public boolean sendKeyEvent(KeyEvent event) {
        this.mIMM.dispatchKeyEventFromInputMethod(this.mTargetView, event);
        return false;
    }

    public boolean reportFullscreenMode(boolean enabled) {
        return true;
    }

    private void sendCurrentText() {
        if (this.mDummyMode) {
            Editable content = getEditable();
            if (content != null) {
                int N = content.length();
                if (N != 0) {
                    if (N == 1) {
                        if (this.mKeyCharacterMap == null) {
                            this.mKeyCharacterMap = KeyCharacterMap.load(-1);
                        }
                        char[] chars = new char[1];
                        int i = 0;
                        content.getChars(0, 1, chars, 0);
                        KeyEvent[] events = this.mKeyCharacterMap.getEvents(chars);
                        if (events != null) {
                            while (i < events.length) {
                                sendKeyEvent(events[i]);
                                i++;
                            }
                            content.clear();
                            return;
                        }
                    }
                    sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), content.toString(), -1, 0));
                    content.clear();
                }
            }
        }
    }

    private void ensureDefaultComposingSpans() {
        if (this.mDefaultComposingSpans == null) {
            Context context;
            if (this.mTargetView != null) {
                context = this.mTargetView.getContext();
            } else if (this.mIMM.mServedView != null) {
                context = this.mIMM.mServedView.getContext();
            } else {
                context = null;
            }
            if (context != null) {
                TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{16843312});
                CharSequence style = ta.getText(0);
                ta.recycle();
                if (style != null && (style instanceof Spanned)) {
                    this.mDefaultComposingSpans = ((Spanned) style).getSpans(0, style.length(), Object.class);
                }
            }
        }
    }

    private void replaceText(CharSequence text, int newCursorPosition, boolean composing) {
        Editable content = getEditable();
        if (content != null) {
            int tmp;
            beginBatchEdit();
            int a = getComposingSpanStart(content);
            int b = getComposingSpanEnd(content);
            if (b < a) {
                tmp = a;
                a = b;
                b = tmp;
            }
            if (a == -1 || b == -1) {
                a = Selection.getSelectionStart(content);
                b = Selection.getSelectionEnd(content);
                if (a < 0) {
                    a = 0;
                }
                if (b < 0) {
                    b = 0;
                }
                if (b < a) {
                    tmp = a;
                    a = b;
                    b = tmp;
                }
            } else {
                removeComposingSpans(content);
            }
            if (composing) {
                Spannable sp;
                if (text instanceof Spannable) {
                    sp = (Spannable) text;
                } else {
                    sp = new SpannableStringBuilder(text);
                    text = sp;
                    ensureDefaultComposingSpans();
                    if (this.mDefaultComposingSpans != null) {
                        for (Object span : this.mDefaultComposingSpans) {
                            sp.setSpan(span, 0, sp.length(), JlogConstants.JLID_CAMERA3_TAF_CMD);
                        }
                    }
                }
                setComposingSpans(sp);
            }
            if (newCursorPosition > 0) {
                newCursorPosition += b - 1;
            } else {
                newCursorPosition += a;
            }
            if (newCursorPosition < 0) {
                newCursorPosition = 0;
            }
            if (newCursorPosition > content.length()) {
                newCursorPosition = content.length();
            }
            Selection.setSelection(content, newCursorPosition);
            content.replace(a, b, text);
            endBatchEdit();
        }
    }

    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        return false;
    }
}
