package com.android.okhttp.internal;

public final class Version {
    public static String userAgent() {
        String agent = System.getProperty("http.agent");
        if (agent != null) {
            return agent;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Java");
        stringBuilder.append(System.getProperty("java.version"));
        return stringBuilder.toString();
    }

    private Version() {
    }
}
