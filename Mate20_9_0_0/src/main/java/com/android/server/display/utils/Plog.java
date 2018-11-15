package com.android.server.display.utils;

import android.util.Slog;

public abstract class Plog {
    private long mId;

    public static class SystemPlog extends Plog {
        private final String mTag;

        public SystemPlog(String tag) {
            this.mTag = tag;
        }

        protected void emit(String message) {
            Slog.d(this.mTag, message);
        }
    }

    protected abstract void emit(String str);

    public static Plog createSystemPlog(String tag) {
        return new SystemPlog(tag);
    }

    public Plog start(String title) {
        this.mId = System.currentTimeMillis();
        write(formatTitle(title));
        return this;
    }

    public Plog logPoint(String name, float x, float y) {
        write(formatPoint(name, x, y));
        return this;
    }

    public Plog logCurve(String name, float[] xs, float[] ys) {
        write(formatCurve(name, xs, ys));
        return this;
    }

    private String formatTitle(String title) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("title: ");
        stringBuilder.append(title);
        return stringBuilder.toString();
    }

    private String formatPoint(String name, float x, float y) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("point: ");
        stringBuilder.append(name);
        stringBuilder.append(": (");
        stringBuilder.append(x);
        stringBuilder.append(",");
        stringBuilder.append(y);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private String formatCurve(String name, float[] xs, float[] ys) {
        StringBuilder sb = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("curve: ");
        stringBuilder.append(name);
        stringBuilder.append(": [");
        sb.append(stringBuilder.toString());
        int n = xs.length <= ys.length ? xs.length : ys.length;
        for (int i = 0; i < n; i++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("(");
            stringBuilder2.append(xs[i]);
            stringBuilder2.append(",");
            stringBuilder2.append(ys[i]);
            stringBuilder2.append("),");
            sb.append(stringBuilder2.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    private void write(String message) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PLOG ");
        stringBuilder.append(this.mId);
        stringBuilder.append("] ");
        stringBuilder.append(message);
        emit(stringBuilder.toString());
    }
}
