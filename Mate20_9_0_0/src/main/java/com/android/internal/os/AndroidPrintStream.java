package com.android.internal.os;

import android.util.Log;

class AndroidPrintStream extends LoggingPrintStream {
    private final int priority;
    private final String tag;

    public AndroidPrintStream(int priority, String tag) {
        if (tag != null) {
            this.priority = priority;
            this.tag = tag;
            return;
        }
        throw new NullPointerException("tag");
    }

    protected void log(String line) {
        Log.println(this.priority, this.tag, line);
    }
}
