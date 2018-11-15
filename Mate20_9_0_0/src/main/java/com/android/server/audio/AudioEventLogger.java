package com.android.server.audio;

import android.util.Log;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class AudioEventLogger {
    private final LinkedList<Event> mEvents = new LinkedList();
    private final int mMemSize;
    private final String mTitle;

    public static abstract class Event {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
        private final long mTimestamp = System.currentTimeMillis();

        public abstract String eventToString();

        Event() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(sFormat.format(new Date(this.mTimestamp)));
            stringBuilder.append(" ");
            stringBuilder.append(eventToString());
            return stringBuilder.toString();
        }

        public Event printLog(String tag) {
            Log.i(tag, eventToString());
            return this;
        }
    }

    public static class StringEvent extends Event {
        private final String mMsg;

        public StringEvent(String msg) {
            this.mMsg = msg;
        }

        public String eventToString() {
            return this.mMsg;
        }
    }

    public AudioEventLogger(int size, String title) {
        this.mMemSize = size;
        this.mTitle = title;
    }

    public synchronized void log(Event evt) {
        if (this.mEvents.size() >= this.mMemSize) {
            this.mEvents.removeFirst();
        }
        this.mEvents.add(evt);
    }

    public synchronized void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Audio event log: ");
        stringBuilder.append(this.mTitle);
        pw.println(stringBuilder.toString());
        Iterator it = this.mEvents.iterator();
        while (it.hasNext()) {
            pw.println(((Event) it.next()).toString());
        }
    }
}
