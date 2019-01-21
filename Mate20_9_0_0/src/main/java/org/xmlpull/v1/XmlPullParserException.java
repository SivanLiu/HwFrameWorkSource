package org.xmlpull.v1;

import android.icu.impl.number.Padder;
import java.io.PrintStream;

public class XmlPullParserException extends Exception {
    protected int column;
    protected Throwable detail;
    protected int row;

    public XmlPullParserException(String s) {
        super(s);
        this.row = -1;
        this.column = -1;
    }

    public XmlPullParserException(String msg, XmlPullParser parser, Throwable chain) {
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        if (msg == null) {
            str = "";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
            str = stringBuilder.toString();
        }
        stringBuilder2.append(str);
        if (parser == null) {
            str = "";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("(position:");
            stringBuilder.append(parser.getPositionDescription());
            stringBuilder.append(") ");
            str = stringBuilder.toString();
        }
        stringBuilder2.append(str);
        if (chain == null) {
            str = "";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("caused by: ");
            stringBuilder.append(chain);
            str = stringBuilder.toString();
        }
        stringBuilder2.append(str);
        super(stringBuilder2.toString());
        this.row = -1;
        this.column = -1;
        if (parser != null) {
            this.row = parser.getLineNumber();
            this.column = parser.getColumnNumber();
        }
        this.detail = chain;
    }

    public Throwable getDetail() {
        return this.detail;
    }

    public int getLineNumber() {
        return this.row;
    }

    public int getColumnNumber() {
        return this.column;
    }

    public void printStackTrace() {
        if (this.detail == null) {
            super.printStackTrace();
            return;
        }
        synchronized (System.err) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(super.getMessage());
            stringBuilder.append("; nested exception is:");
            printStream.println(stringBuilder.toString());
            this.detail.printStackTrace();
        }
    }
}
