package android.text.method;

import android.graphics.Paint;
import android.icu.lang.UCharacter;
import android.text.Editable;
import android.text.Emoji;
import android.text.Layout;
import android.text.NoCopySpan.Concrete;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.TextKeyListener.Capitalize;
import android.text.style.ReplacementSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.android.internal.annotations.GuardedBy;

public abstract class BaseKeyListener extends MetaKeyKeyListener implements KeyListener {
    private static final int CARRIAGE_RETURN = 13;
    private static final int LINE_FEED = 10;
    static final Object OLD_SEL_START = new Concrete();
    @GuardedBy("mLock")
    static Paint sCachedPaint = null;
    private final Object mLock = new Object();

    public boolean backspace(View view, Editable content, int keyCode, KeyEvent event) {
        return backspaceOrForwardDelete(view, content, keyCode, event, false);
    }

    public boolean forwardDelete(View view, Editable content, int keyCode, KeyEvent event) {
        return backspaceOrForwardDelete(view, content, keyCode, event, true);
    }

    private static boolean isVariationSelector(int codepoint) {
        return UCharacter.hasBinaryProperty(codepoint, 36);
    }

    private static int adjustReplacementSpan(CharSequence text, int offset, boolean moveToStart) {
        if (!(text instanceof Spanned)) {
            return offset;
        }
        ReplacementSpan[] spans = (ReplacementSpan[]) ((Spanned) text).getSpans(offset, offset, ReplacementSpan.class);
        for (int i = 0; i < spans.length; i++) {
            int start = ((Spanned) text).getSpanStart(spans[i]);
            int end = ((Spanned) text).getSpanEnd(spans[i]);
            if (start < offset && end > offset) {
                offset = moveToStart ? start : end;
            }
        }
        return offset;
    }

    /* JADX WARNING: Missing block: B:70:0x0164, code skipped:
            r19 = r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getOffsetForBackspaceKey(CharSequence text, int offset) {
        CharSequence charSequence = text;
        int i = offset;
        if (i <= 1) {
            return 0;
        }
        int STATE_START = 0;
        int STATE_LF = 1;
        int state = 0;
        int lastSeenVSCharCount = 0;
        int deleteCharCount = 0;
        int tmpOffset = i;
        while (true) {
            int STATE_START2 = STATE_START;
            int STATE_LF2 = STATE_LF;
            STATE_START = tmpOffset;
            STATE_LF = Character.codePointBefore(charSequence, STATE_START);
            tmpOffset = STATE_START - Character.charCount(STATE_LF);
            switch (state) {
                case 0:
                    int deleteCharCount2 = Character.charCount(STATE_LF);
                    if (STATE_LF == 10) {
                        state = 1;
                    } else if (isVariationSelector(STATE_LF)) {
                        state = 6;
                    } else if (Emoji.isRegionalIndicatorSymbol(STATE_LF)) {
                        state = 10;
                    } else if (Emoji.isEmojiModifier(STATE_LF)) {
                        state = 4;
                    } else if (STATE_LF == Emoji.COMBINING_ENCLOSING_KEYCAP) {
                        state = 2;
                    } else if (Emoji.isEmoji(STATE_LF)) {
                        state = 7;
                    } else if (STATE_LF == Emoji.CANCEL_TAG) {
                        state = 12;
                    } else {
                        state = 13;
                    }
                    deleteCharCount = deleteCharCount2;
                    break;
                case 1:
                    if (STATE_LF == 13) {
                        deleteCharCount++;
                    }
                    state = 13;
                    break;
                case 2:
                    if (!isVariationSelector(STATE_LF)) {
                        if (Emoji.isKeycapBase(STATE_LF)) {
                            deleteCharCount += Character.charCount(STATE_LF);
                        }
                        state = 13;
                        break;
                    }
                    STATE_START = Character.charCount(STATE_LF);
                    state = 3;
                case 3:
                    if (Emoji.isKeycapBase(STATE_LF)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(STATE_LF);
                    }
                    state = 13;
                    break;
                case 4:
                    if (!isVariationSelector(STATE_LF)) {
                        if (Emoji.isEmojiModifierBase(STATE_LF)) {
                            deleteCharCount += Character.charCount(STATE_LF);
                        }
                        state = 13;
                        break;
                    }
                    STATE_START = Character.charCount(STATE_LF);
                    state = 5;
                case 5:
                    if (Emoji.isEmojiModifierBase(STATE_LF)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(STATE_LF);
                    }
                    state = 13;
                    break;
                case 6:
                    if (!Emoji.isEmoji(STATE_LF)) {
                        if (!isVariationSelector(STATE_LF) && UCharacter.getCombiningClass(STATE_LF) == 0) {
                            deleteCharCount += Character.charCount(STATE_LF);
                        }
                        state = 13;
                        break;
                    }
                    deleteCharCount += Character.charCount(STATE_LF);
                    state = 7;
                    break;
                case 7:
                    if (STATE_LF != Emoji.ZERO_WIDTH_JOINER) {
                        state = 13;
                        break;
                    }
                    state = 8;
                    break;
                case 8:
                    if (!Emoji.isEmoji(STATE_LF)) {
                        if (!isVariationSelector(STATE_LF)) {
                            state = 13;
                            break;
                        }
                        lastSeenVSCharCount = Character.charCount(STATE_LF);
                        state = 9;
                        break;
                    }
                    deleteCharCount += Character.charCount(STATE_LF) + 1;
                    state = Emoji.isEmojiModifier(STATE_LF) ? 4 : 7;
                    break;
                case 9:
                    if (!Emoji.isEmoji(STATE_LF)) {
                        state = 13;
                        break;
                    }
                    deleteCharCount += (lastSeenVSCharCount + 1) + Character.charCount(STATE_LF);
                    lastSeenVSCharCount = 0;
                    state = 7;
                    break;
                case 10:
                    if (!Emoji.isRegionalIndicatorSymbol(STATE_LF)) {
                        state = 13;
                        break;
                    }
                    deleteCharCount += 2;
                    state = 11;
                    break;
                case 11:
                    if (!Emoji.isRegionalIndicatorSymbol(STATE_LF)) {
                        state = 13;
                        break;
                    }
                    deleteCharCount -= 2;
                    state = 10;
                    break;
                case 12:
                    if (!Emoji.isTagSpecChar(STATE_LF)) {
                        if (!Emoji.isEmoji(STATE_LF)) {
                            deleteCharCount = 2;
                            state = 13;
                            break;
                        }
                        deleteCharCount += Character.charCount(STATE_LF);
                        state = 13;
                        break;
                    }
                    deleteCharCount += 2;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("state ");
                    stringBuilder.append(state);
                    stringBuilder.append(" is unknown");
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            if (tmpOffset > 0 && state != 13) {
                STATE_START = STATE_START2;
                STATE_LF = STATE_LF2;
            }
        }
        return adjustReplacementSpan(charSequence, i - deleteCharCount, true);
    }

    private static int getOffsetForForwardDeleteKey(CharSequence text, int offset, Paint paint) {
        int len = text.length();
        if (offset >= len - 1) {
            return len;
        }
        return adjustReplacementSpan(text, paint.getTextRunCursor(text, offset, len, 0, offset, 0), false);
    }

    private boolean backspaceOrForwardDelete(View view, Editable content, int keyCode, KeyEvent event, boolean isForwardDelete) {
        if (!KeyEvent.metaStateHasNoModifiers(event.getMetaState() & -28916)) {
            return false;
        }
        if (deleteSelection(view, content)) {
            return true;
        }
        boolean isCtrlActive = (event.getMetaState() & 4096) != 0;
        boolean isShiftActive = MetaKeyKeyListener.getMetaState(content, 1, event) == 1;
        boolean isAltActive = MetaKeyKeyListener.getMetaState(content, 2, event) == 1;
        if (isCtrlActive) {
            if (isAltActive || isShiftActive) {
                return false;
            }
            return deleteUntilWordBoundary(view, content, isForwardDelete);
        } else if (isAltActive && deleteLine(view, content)) {
            return true;
        } else {
            int end;
            int start = Selection.getSelectionEnd(content);
            if (isForwardDelete) {
                if (view instanceof TextView) {
                    end = ((TextView) view).getPaint();
                } else {
                    Paint paint;
                    synchronized (this.mLock) {
                        if (sCachedPaint == null) {
                            sCachedPaint = new Paint();
                        }
                        paint = sCachedPaint;
                    }
                    end = paint;
                }
                end = getOffsetForForwardDeleteKey(content, start, end);
            } else {
                end = getOffsetForBackspaceKey(content, start);
            }
            if (start == end) {
                return false;
            }
            content.delete(Math.min(start, end), Math.max(start, end));
            return true;
        }
    }

    private boolean deleteUntilWordBoundary(View view, Editable content, boolean isForwardDelete) {
        int currentCursorOffset = Selection.getSelectionStart(content);
        if (currentCursorOffset != Selection.getSelectionEnd(content)) {
            return false;
        }
        if ((!isForwardDelete && currentCursorOffset == 0) || (isForwardDelete && currentCursorOffset == content.length())) {
            return false;
        }
        int deleteFrom;
        int deleteTo;
        WordIterator wordIterator = null;
        if (view instanceof TextView) {
            wordIterator = ((TextView) view).getWordIterator();
        }
        if (wordIterator == null) {
            wordIterator = new WordIterator();
        }
        if (isForwardDelete) {
            deleteFrom = currentCursorOffset;
            wordIterator.setCharSequence(content, deleteFrom, content.length());
            deleteTo = wordIterator.following(currentCursorOffset);
            if (deleteTo == -1) {
                deleteTo = content.length();
            }
        } else {
            deleteTo = currentCursorOffset;
            wordIterator.setCharSequence(content, 0, deleteTo);
            deleteFrom = wordIterator.preceding(currentCursorOffset);
            if (deleteFrom == -1) {
                deleteFrom = 0;
            }
        }
        content.delete(deleteFrom, deleteTo);
        return true;
    }

    private boolean deleteSelection(View view, Editable content) {
        int selectionStart = Selection.getSelectionStart(content);
        int selectionEnd = Selection.getSelectionEnd(content);
        if (selectionEnd < selectionStart) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }
        if (selectionStart == selectionEnd) {
            return false;
        }
        content.delete(selectionStart, selectionEnd);
        return true;
    }

    private boolean deleteLine(View view, Editable content) {
        if (view instanceof TextView) {
            Layout layout = ((TextView) view).getLayout();
            if (layout != null) {
                int line = layout.getLineForOffset(Selection.getSelectionStart(content));
                int start = layout.getLineStart(line);
                int end = layout.getLineEnd(line);
                if (end != start) {
                    content.delete(start, end);
                    return true;
                }
            }
        }
        return false;
    }

    static int makeTextContentType(Capitalize caps, boolean autoText) {
        int contentType = 1;
        switch (caps) {
            case CHARACTERS:
                contentType = 1 | 4096;
                break;
            case WORDS:
                contentType = 1 | 8192;
                break;
            case SENTENCES:
                contentType = 1 | 16384;
                break;
        }
        if (autoText) {
            return contentType | 32768;
        }
        return contentType;
    }

    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        boolean handled;
        if (keyCode == 67) {
            handled = backspace(view, content, keyCode, event);
        } else if (keyCode != 112) {
            handled = false;
        } else {
            handled = forwardDelete(view, content, keyCode, event);
        }
        if (!handled) {
            return super.onKeyDown(view, content, keyCode, event);
        }
        MetaKeyKeyListener.adjustMetaAfterKeypress((Spannable) content);
        return true;
    }

    public boolean onKeyOther(View view, Editable content, KeyEvent event) {
        if (event.getAction() != 2 || event.getKeyCode() != 0) {
            return false;
        }
        int selectionStart = Selection.getSelectionStart(content);
        int selectionEnd = Selection.getSelectionEnd(content);
        if (selectionEnd < selectionStart) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }
        CharSequence text = event.getCharacters();
        if (text == null) {
            return false;
        }
        content.replace(selectionStart, selectionEnd, text);
        return true;
    }
}
