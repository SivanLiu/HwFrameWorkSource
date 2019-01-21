package android.icu.text;

import android.icu.impl.Utility;
import android.icu.impl.number.Padder;

class StringReplacer implements UnicodeReplacer {
    private int cursorPos;
    private final Data data;
    private boolean hasCursor;
    private boolean isComplex;
    private String output;

    public StringReplacer(String theOutput, int theCursorPos, Data theData) {
        this.output = theOutput;
        this.cursorPos = theCursorPos;
        this.hasCursor = true;
        this.data = theData;
        this.isComplex = true;
    }

    public StringReplacer(String theOutput, Data theData) {
        this.output = theOutput;
        this.cursorPos = 0;
        this.hasCursor = false;
        this.data = theData;
        this.isComplex = true;
    }

    public int replace(Replaceable text, int start, int limit, int[] cursor) {
        int newStart;
        int oOutput;
        int tempExtra;
        int outLen;
        Replaceable replaceable = text;
        int i = start;
        int i2 = limit;
        int[] iArr = cursor;
        if (this.isComplex) {
            int len;
            StringBuffer buf = new StringBuffer();
            this.isComplex = false;
            int tempStart = text.length();
            int destStart = tempStart;
            boolean z = true;
            if (i > 0) {
                len = UTF16.getCharCount(replaceable.char32At(i - 1));
                replaceable.copy(i - len, i, tempStart);
                destStart += len;
            } else {
                replaceable.replace(tempStart, tempStart, "￿");
                destStart++;
            }
            len = destStart;
            int tempExtra2 = 0;
            newStart = 0;
            oOutput = 0;
            while (oOutput < this.output.length()) {
                if (oOutput == this.cursorPos) {
                    newStart = (buf.length() + len) - destStart;
                }
                int c = UTF16.charAt(this.output, oOutput);
                int nextIndex = UTF16.getCharCount(c) + oOutput;
                if (nextIndex == this.output.length()) {
                    tempExtra = UTF16.getCharCount(replaceable.char32At(i2));
                    replaceable.copy(i2, i2 + tempExtra, len);
                    tempExtra2 = tempExtra;
                }
                UnicodeReplacer r = this.data.lookupReplacer(c);
                if (r == null) {
                    UTF16.append(buf, c);
                } else {
                    this.isComplex = z;
                    if (buf.length() > 0) {
                        replaceable.replace(len, len, buf.toString());
                        len += buf.length();
                        buf.setLength(0);
                    }
                    len += r.replace(replaceable, len, len, iArr);
                }
                oOutput = nextIndex;
                z = true;
            }
            if (buf.length() > 0) {
                replaceable.replace(len, len, buf.toString());
                len += buf.length();
            }
            if (oOutput == this.cursorPos) {
                newStart = len - destStart;
            }
            tempExtra = len - destStart;
            replaceable.copy(destStart, len, i);
            replaceable.replace(tempStart + tempExtra, (len + tempExtra2) + tempExtra, "");
            replaceable.replace(i + tempExtra, i2 + tempExtra, "");
            outLen = tempExtra;
        } else {
            replaceable.replace(i, i2, this.output);
            outLen = this.output.length();
            newStart = this.cursorPos;
        }
        if (this.hasCursor) {
            if (this.cursorPos < 0) {
                oOutput = i;
                tempExtra = this.cursorPos;
                while (tempExtra < 0 && oOutput > 0) {
                    oOutput -= UTF16.getCharCount(replaceable.char32At(oOutput - 1));
                    tempExtra++;
                }
                oOutput += tempExtra;
            } else if (this.cursorPos > this.output.length()) {
                oOutput = i + outLen;
                tempExtra = this.cursorPos - this.output.length();
                while (tempExtra > 0 && oOutput < text.length()) {
                    oOutput += UTF16.getCharCount(replaceable.char32At(oOutput));
                    tempExtra--;
                }
                oOutput += tempExtra;
            } else {
                newStart += i;
                iArr[0] = newStart;
            }
            newStart = oOutput;
            iArr[0] = newStart;
        }
        return outLen;
    }

    public String toReplacerPattern(boolean escapeUnprintable) {
        int cursor;
        StringBuffer rule = new StringBuffer();
        StringBuffer quoteBuf = new StringBuffer();
        int cursor2 = this.cursorPos;
        if (this.hasCursor && cursor2 < 0) {
            while (true) {
                cursor = cursor2 + 1;
                if (cursor2 >= 0) {
                    break;
                }
                Utility.appendToRule(rule, 64, true, escapeUnprintable, quoteBuf);
                cursor2 = cursor;
            }
            cursor2 = cursor;
        }
        int i = 0;
        while (i < this.output.length()) {
            if (this.hasCursor && i == cursor2) {
                Utility.appendToRule(rule, 124, true, escapeUnprintable, quoteBuf);
            }
            int c = this.output.charAt(i);
            UnicodeReplacer r = this.data.lookupReplacer(c);
            if (r == null) {
                Utility.appendToRule(rule, c, false, escapeUnprintable, quoteBuf);
            } else {
                StringBuffer buf = new StringBuffer(Padder.FALLBACK_PADDING_STRING);
                buf.append(r.toReplacerPattern(escapeUnprintable));
                buf.append(' ');
                Utility.appendToRule(rule, buf.toString(), true, escapeUnprintable, quoteBuf);
            }
            i++;
        }
        if (this.hasCursor && cursor2 > this.output.length()) {
            cursor2 -= this.output.length();
            while (true) {
                cursor = cursor2 - 1;
                if (cursor2 <= 0) {
                    break;
                }
                Utility.appendToRule(rule, 64, true, escapeUnprintable, quoteBuf);
                cursor2 = cursor;
            }
            Utility.appendToRule(rule, 124, true, escapeUnprintable, quoteBuf);
            cursor2 = cursor;
        }
        Utility.appendToRule(rule, -1, true, escapeUnprintable, quoteBuf);
        return rule.toString();
    }

    public void addReplacementSetTo(UnicodeSet toUnionTo) {
        int i = 0;
        while (i < this.output.length()) {
            int ch = UTF16.charAt(this.output, i);
            UnicodeReplacer r = this.data.lookupReplacer(ch);
            if (r == null) {
                toUnionTo.add(ch);
            } else {
                r.addReplacementSetTo(toUnionTo);
            }
            i += UTF16.getCharCount(ch);
        }
    }
}
