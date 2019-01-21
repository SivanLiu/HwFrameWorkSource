package android.text.method;

import android.text.AutoText;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.TextKeyListener.Capitalize;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;

public class QwertyKeyListener extends BaseKeyListener {
    private static SparseArray<String> PICKER_SETS = new SparseArray();
    private static QwertyKeyListener sFullKeyboardInstance;
    private static QwertyKeyListener[] sInstance = new QwertyKeyListener[(Capitalize.values().length * 2)];
    private Capitalize mAutoCap;
    private boolean mAutoText;
    private boolean mFullKeyboard;

    static class Replaced implements NoCopySpan {
        private char[] mText;

        public Replaced(char[] text) {
            this.mText = text;
        }
    }

    static {
        PICKER_SETS.put(65, "ÀÁÂÄÆÃÅĄĀ");
        PICKER_SETS.put(67, "ÇĆČ");
        PICKER_SETS.put(68, "Ď");
        PICKER_SETS.put(69, "ÈÉÊËĘĚĒ");
        PICKER_SETS.put(71, "Ğ");
        PICKER_SETS.put(76, "Ł");
        PICKER_SETS.put(73, "ÌÍÎÏĪİ");
        PICKER_SETS.put(78, "ÑŃŇ");
        PICKER_SETS.put(79, "ØŒÕÒÓÔÖŌ");
        PICKER_SETS.put(82, "Ř");
        PICKER_SETS.put(83, "ŚŠŞ");
        PICKER_SETS.put(84, "Ť");
        PICKER_SETS.put(85, "ÙÚÛÜŮŪ");
        PICKER_SETS.put(89, "ÝŸ");
        PICKER_SETS.put(90, "ŹŻŽ");
        PICKER_SETS.put(97, "àáâäæãåąā");
        PICKER_SETS.put(99, "çćč");
        PICKER_SETS.put(100, "ď");
        PICKER_SETS.put(101, "èéêëęěē");
        PICKER_SETS.put(103, "ğ");
        PICKER_SETS.put(105, "ìíîïīı");
        PICKER_SETS.put(108, "ł");
        PICKER_SETS.put(110, "ñńň");
        PICKER_SETS.put(111, "øœõòóôöō");
        PICKER_SETS.put(114, "ř");
        PICKER_SETS.put(115, "§ßśšş");
        PICKER_SETS.put(116, "ť");
        PICKER_SETS.put(117, "ùúûüůū");
        PICKER_SETS.put(121, "ýÿ");
        PICKER_SETS.put(122, "źżž");
        PICKER_SETS.put(61185, "…¥•®©±[]{}\\|");
        PICKER_SETS.put(47, "\\");
        PICKER_SETS.put(49, "¹½⅓¼⅛");
        PICKER_SETS.put(50, "²⅔");
        PICKER_SETS.put(51, "³¾⅜");
        PICKER_SETS.put(52, "⁴");
        PICKER_SETS.put(53, "⅝");
        PICKER_SETS.put(55, "⅞");
        PICKER_SETS.put(48, "ⁿ∅");
        PICKER_SETS.put(36, "¢£€¥₣₤₱");
        PICKER_SETS.put(37, "‰");
        PICKER_SETS.put(42, "†‡");
        PICKER_SETS.put(45, "–—");
        PICKER_SETS.put(43, "±");
        PICKER_SETS.put(40, "[{<");
        PICKER_SETS.put(41, "]}>");
        PICKER_SETS.put(33, "¡");
        PICKER_SETS.put(34, "“”«»˝");
        PICKER_SETS.put(63, "¿");
        PICKER_SETS.put(44, "‚„");
        PICKER_SETS.put(61, "≠≈∞");
        PICKER_SETS.put(60, "≤«‹");
        PICKER_SETS.put(62, "≥»›");
    }

    private QwertyKeyListener(Capitalize cap, boolean autoText, boolean fullKeyboard) {
        this.mAutoCap = cap;
        this.mAutoText = autoText;
        this.mFullKeyboard = fullKeyboard;
    }

    public QwertyKeyListener(Capitalize cap, boolean autoText) {
        this(cap, autoText, false);
    }

    public static QwertyKeyListener getInstance(boolean autoText, Capitalize cap) {
        int off = (cap.ordinal() * 2) + autoText;
        if (sInstance[off] == null) {
            sInstance[off] = new QwertyKeyListener(cap, autoText);
        }
        return sInstance[off];
    }

    public static QwertyKeyListener getInstanceForFullKeyboard() {
        if (sFullKeyboardInstance == null) {
            sFullKeyboardInstance = new QwertyKeyListener(Capitalize.NONE, false, true);
        }
        return sFullKeyboardInstance;
    }

    public int getInputType() {
        return BaseKeyListener.makeTextContentType(this.mAutoCap, this.mAutoText);
    }

    /* JADX WARNING: Removed duplicated region for block: B:138:0x023d  */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x023d  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x009d  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x008c  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x008c  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x009d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        int i;
        int activeEnd;
        View view2 = view;
        Editable editable = content;
        KeyEvent keyEvent = event;
        int pref = 0;
        if (view2 != null) {
            pref = TextKeyListener.getInstance().getPrefs(view.getContext());
        }
        int pref2 = pref;
        pref = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        int selStart = Math.min(pref, b);
        int selEnd = Math.max(pref, b);
        if (selStart < 0 || selEnd < 0) {
            selEnd = 0;
            selStart = 0;
            Selection.setSelection(editable, 0, 0);
        }
        int selStart2 = selStart;
        int selEnd2 = selEnd;
        int activeStart = editable.getSpanStart(TextKeyListener.ACTIVE);
        int activeEnd2 = editable.getSpanEnd(TextKeyListener.ACTIVE);
        char i2 = keyEvent.getUnicodeChar(MetaKeyKeyListener.getMetaState((CharSequence) editable, keyEvent));
        if (!this.mFullKeyboard) {
            pref = event.getRepeatCount();
            if (pref > 0 && selStart2 == selEnd2 && selStart2 > 0) {
                char charAt = editable.charAt(selStart2 - 1);
                if (charAt != i2 && charAt != Character.toUpperCase(i2)) {
                    i = i2;
                    activeEnd = activeEnd2;
                    if (i == 61185) {
                    }
                } else if (view2 != null) {
                    char c = charAt;
                    i = i2;
                    activeEnd = activeEnd2;
                    if (showCharacterPicker(view2, editable, charAt, false, pref)) {
                        MetaKeyKeyListener.resetMetaState(content);
                        return true;
                    }
                    if (i == 61185) {
                        if (view2 != null) {
                            showCharacterPicker(view2, editable, KeyCharacterMap.PICKER_DIALOG_INPUT, true, 1);
                        }
                        MetaKeyKeyListener.resetMetaState(content);
                        return true;
                    }
                    int i3;
                    if (i == 61184) {
                        if (selStart2 == selEnd2) {
                            b = selEnd2;
                            while (b > 0 && selEnd2 - b < 4 && Character.digit(editable.charAt(b - 1), 16) >= 0) {
                                b--;
                            }
                        } else {
                            b = selStart2;
                        }
                        selStart = -1;
                        try {
                            selStart = Integer.parseInt(TextUtils.substring(editable, b, selEnd2), 16);
                        } catch (NumberFormatException e) {
                        }
                        if (selStart >= 0) {
                            selStart2 = b;
                            Selection.setSelection(editable, selStart2, selEnd2);
                            i3 = selStart;
                        } else {
                            i3 = 0;
                        }
                    } else {
                        i3 = i;
                    }
                    if (i3 != 0) {
                        int where;
                        int i4;
                        char c2;
                        boolean dead = false;
                        if ((Integer.MIN_VALUE & i3) != 0) {
                            dead = true;
                            i3 &= Integer.MAX_VALUE;
                        }
                        if (activeStart == selStart2 && activeEnd == selEnd2) {
                            boolean replace = false;
                            if ((selEnd2 - selStart2) - 1 == 0) {
                                i = KeyEvent.getDeadChar(editable.charAt(selStart2), i3);
                                if (i != 0) {
                                    i3 = i;
                                    replace = true;
                                    dead = false;
                                }
                            }
                            if (!replace) {
                                Selection.setSelection(editable, selEnd2);
                                editable.removeSpan(TextKeyListener.ACTIVE);
                                selStart2 = selEnd2;
                            }
                        }
                        if ((pref2 & 1) != 0 && Character.isLowerCase(i3) && TextKeyListener.shouldCap(this.mAutoCap, editable, selStart2)) {
                            where = editable.getSpanEnd(TextKeyListener.CAPPED);
                            i = editable.getSpanFlags(TextKeyListener.CAPPED);
                            if (where == selStart2 && ((i >> 16) & 65535) == i3) {
                                editable.removeSpan(TextKeyListener.CAPPED);
                            } else {
                                b = i3 << 16;
                                i3 = Character.toUpperCase(i3);
                                if (selStart2 == 0) {
                                    editable.setSpan(TextKeyListener.CAPPED, 0, 0, 17 | b);
                                } else {
                                    editable.setSpan(TextKeyListener.CAPPED, selStart2 - 1, selStart2, 33 | b);
                                }
                            }
                        }
                        if (selStart2 != selEnd2) {
                            Selection.setSelection(editable, selEnd2);
                        }
                        editable.setSpan(OLD_SEL_START, selStart2, selStart2, 17);
                        editable.replace(selStart2, selEnd2, String.valueOf((char) i3));
                        pref = editable.getSpanStart(OLD_SEL_START);
                        b = Selection.getSelectionEnd(content);
                        if (pref < b) {
                            editable.setSpan(TextKeyListener.LAST_TYPED, pref, b, 33);
                            if (dead) {
                                Selection.setSelection(editable, pref, b);
                                editable.setSpan(TextKeyListener.ACTIVE, pref, b, 33);
                            }
                        }
                        MetaKeyKeyListener.adjustMetaAfterKeypress((Spannable) content);
                        if ((pref2 & 2) != 0 && this.mAutoText) {
                            if (i3 != 32 && i3 != 9 && i3 != 10 && i3 != 44 && i3 != 46 && i3 != 33 && i3 != 63 && i3 != 34 && Character.getType(i3) != 22) {
                                i4 = b;
                                b = Selection.getSelectionEnd(content);
                                c2 = editable.charAt(b - 3);
                                while (where > 0) {
                                }
                                editable.replace(b - 2, b - 1, ".");
                                i4 = b;
                                return true;
                            } else if (editable.getSpanEnd(TextKeyListener.INHIBIT_REPLACEMENT) != pref) {
                                where = pref;
                                while (where > 0) {
                                    char c3 = editable.charAt(where - 1);
                                    if (c3 != DateFormat.QUOTE && !Character.isLetter(c3)) {
                                        break;
                                    }
                                    where--;
                                }
                                String rep = getReplacement(editable, where, pref, view2);
                                if (rep != null) {
                                    Replaced[] repl = (Replaced[]) editable.getSpans(0, content.length(), Replaced.class);
                                    for (Object removeSpan : repl) {
                                        editable.removeSpan(removeSpan);
                                    }
                                    char[] orig = new char[(pref - where)];
                                    TextUtils.getChars(editable, where, pref, orig, 0);
                                    editable.setSpan(new Replaced(orig), where, pref, 33);
                                    editable.replace(where, pref, rep);
                                    if ((pref2 & 4) != 0 && this.mAutoText) {
                                        b = Selection.getSelectionEnd(content);
                                        if (b - 3 >= 0 && editable.charAt(b - 1) == ' ' && editable.charAt(b - 2) == ' ') {
                                            c2 = editable.charAt(b - 3);
                                            for (where = b - 3; where > 0; where--) {
                                                if (c2 != '\"') {
                                                    if (Character.getType(c2) != 22) {
                                                        break;
                                                    }
                                                }
                                                c2 = editable.charAt(where - 1);
                                            }
                                            if (Character.isLetter(c2) || Character.isDigit(c2)) {
                                                editable.replace(b - 2, b - 1, ".");
                                            }
                                        }
                                        i4 = b;
                                    }
                                    return true;
                                }
                            }
                        }
                        b = Selection.getSelectionEnd(content);
                        c2 = editable.charAt(b - 3);
                        while (where > 0) {
                        }
                        editable.replace(b - 2, b - 1, ".");
                        i4 = b;
                        return true;
                    }
                    KeyEvent keyEvent2;
                    if (keyCode == 67) {
                        selEnd = activeEnd;
                        keyEvent2 = event;
                        if (event.hasNoModifiers() || keyEvent2.hasModifiers(2)) {
                            if (selStart2 == selEnd2) {
                                pref = 1;
                                if (editable.getSpanEnd(TextKeyListener.LAST_TYPED) == selStart2 && editable.charAt(selStart2 - 1) != 10) {
                                    pref = 2;
                                }
                                Replaced[] repl2 = (Replaced[]) editable.getSpans(selStart2 - pref, selStart2, Replaced.class);
                                if (repl2.length > 0) {
                                    activeEnd = editable.getSpanStart(repl2[0]);
                                    i = editable.getSpanEnd(repl2[0]);
                                    String old = new String(repl2[0].mText);
                                    editable.removeSpan(repl2[0]);
                                    if (selStart2 >= i) {
                                        editable.setSpan(TextKeyListener.INHIBIT_REPLACEMENT, i, i, 34);
                                        editable.replace(activeEnd, i, old);
                                        selEnd = editable.getSpanStart(TextKeyListener.INHIBIT_REPLACEMENT);
                                        if (selEnd - 1 >= 0) {
                                            editable.setSpan(TextKeyListener.INHIBIT_REPLACEMENT, selEnd - 1, selEnd, 33);
                                        } else {
                                            editable.removeSpan(TextKeyListener.INHIBIT_REPLACEMENT);
                                        }
                                        MetaKeyKeyListener.adjustMetaAfterKeypress((Spannable) content);
                                        return true;
                                    }
                                    MetaKeyKeyListener.adjustMetaAfterKeypress((Spannable) content);
                                    return super.onKeyDown(view, content, keyCode, event);
                                }
                            }
                        } else {
                            int i5 = selEnd;
                        }
                    } else {
                        keyEvent2 = event;
                    }
                    return super.onKeyDown(view, content, keyCode, event);
                }
            }
        }
        i = i2;
        activeEnd = activeEnd2;
        if (i == 61185) {
        }
    }

    private String getReplacement(CharSequence src, int start, int end, View view) {
        String out;
        int len = end - start;
        boolean changecase = false;
        String replacement = AutoText.get(src, start, end, view);
        if (replacement == null) {
            replacement = AutoText.get(TextUtils.substring(src, start, end).toLowerCase(), 0, end - start, view);
            changecase = true;
            if (replacement == null) {
                return null;
            }
        }
        int caps = 0;
        if (changecase) {
            int caps2 = 0;
            for (caps = start; caps < end; caps++) {
                if (Character.isUpperCase(src.charAt(caps))) {
                    caps2++;
                }
            }
            caps = caps2;
        }
        if (caps == 0) {
            out = replacement;
        } else if (caps == 1) {
            out = toTitleCase(replacement);
        } else if (caps == len) {
            out = replacement.toUpperCase();
        } else {
            out = toTitleCase(replacement);
        }
        if (out.length() == len && TextUtils.regionMatches(src, start, out, 0, len)) {
            return null;
        }
        return out;
    }

    public static void markAsReplaced(Spannable content, int start, int end, String original) {
        Replaced[] repl = (Replaced[]) content.getSpans(0, content.length(), Replaced.class);
        for (Object removeSpan : repl) {
            content.removeSpan(removeSpan);
        }
        int a = original.length();
        char[] orig = new char[a];
        original.getChars(0, a, orig, 0);
        content.setSpan(new Replaced(orig), start, end, 33);
    }

    private boolean showCharacterPicker(View view, Editable content, char c, boolean insert, int count) {
        String set = (String) PICKER_SETS.get(c);
        if (set == null) {
            return false;
        }
        if (count == 1) {
            new CharacterPickerDialog(view.getContext(), view, content, set, insert).show();
        }
        return true;
    }

    private static String toTitleCase(String src) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Character.toUpperCase(src.charAt(0)));
        stringBuilder.append(src.substring(1));
        return stringBuilder.toString();
    }
}
