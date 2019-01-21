package com.android.okhttp;

import com.android.okhttp.internal.http.HeaderParser;
import java.util.concurrent.TimeUnit;

public final class CacheControl {
    public static final CacheControl FORCE_CACHE = new Builder().onlyIfCached().maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS).build();
    public static final CacheControl FORCE_NETWORK = new Builder().noCache().build();
    String headerValue;
    private final boolean isPrivate;
    private final boolean isPublic;
    private final int maxAgeSeconds;
    private final int maxStaleSeconds;
    private final int minFreshSeconds;
    private final boolean mustRevalidate;
    private final boolean noCache;
    private final boolean noStore;
    private final boolean noTransform;
    private final boolean onlyIfCached;
    private final int sMaxAgeSeconds;

    public static final class Builder {
        int maxAgeSeconds = -1;
        int maxStaleSeconds = -1;
        int minFreshSeconds = -1;
        boolean noCache;
        boolean noStore;
        boolean noTransform;
        boolean onlyIfCached;

        public Builder noCache() {
            this.noCache = true;
            return this;
        }

        public Builder noStore() {
            this.noStore = true;
            return this;
        }

        public Builder maxAge(int maxAge, TimeUnit timeUnit) {
            if (maxAge >= 0) {
                int i;
                long maxAgeSecondsLong = timeUnit.toSeconds((long) maxAge);
                if (maxAgeSecondsLong > 2147483647L) {
                    i = Integer.MAX_VALUE;
                } else {
                    i = (int) maxAgeSecondsLong;
                }
                this.maxAgeSeconds = i;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("maxAge < 0: ");
            stringBuilder.append(maxAge);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder maxStale(int maxStale, TimeUnit timeUnit) {
            if (maxStale >= 0) {
                int i;
                long maxStaleSecondsLong = timeUnit.toSeconds((long) maxStale);
                if (maxStaleSecondsLong > 2147483647L) {
                    i = Integer.MAX_VALUE;
                } else {
                    i = (int) maxStaleSecondsLong;
                }
                this.maxStaleSeconds = i;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("maxStale < 0: ");
            stringBuilder.append(maxStale);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder minFresh(int minFresh, TimeUnit timeUnit) {
            if (minFresh >= 0) {
                int i;
                long minFreshSecondsLong = timeUnit.toSeconds((long) minFresh);
                if (minFreshSecondsLong > 2147483647L) {
                    i = Integer.MAX_VALUE;
                } else {
                    i = (int) minFreshSecondsLong;
                }
                this.minFreshSeconds = i;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("minFresh < 0: ");
            stringBuilder.append(minFresh);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder onlyIfCached() {
            this.onlyIfCached = true;
            return this;
        }

        public Builder noTransform() {
            this.noTransform = true;
            return this;
        }

        public CacheControl build() {
            return new CacheControl(this);
        }
    }

    private CacheControl(boolean noCache, boolean noStore, int maxAgeSeconds, int sMaxAgeSeconds, boolean isPrivate, boolean isPublic, boolean mustRevalidate, int maxStaleSeconds, int minFreshSeconds, boolean onlyIfCached, boolean noTransform, String headerValue) {
        this.noCache = noCache;
        this.noStore = noStore;
        this.maxAgeSeconds = maxAgeSeconds;
        this.sMaxAgeSeconds = sMaxAgeSeconds;
        this.isPrivate = isPrivate;
        this.isPublic = isPublic;
        this.mustRevalidate = mustRevalidate;
        this.maxStaleSeconds = maxStaleSeconds;
        this.minFreshSeconds = minFreshSeconds;
        this.onlyIfCached = onlyIfCached;
        this.noTransform = noTransform;
        this.headerValue = headerValue;
    }

    private CacheControl(Builder builder) {
        this.noCache = builder.noCache;
        this.noStore = builder.noStore;
        this.maxAgeSeconds = builder.maxAgeSeconds;
        this.sMaxAgeSeconds = -1;
        this.isPrivate = false;
        this.isPublic = false;
        this.mustRevalidate = false;
        this.maxStaleSeconds = builder.maxStaleSeconds;
        this.minFreshSeconds = builder.minFreshSeconds;
        this.onlyIfCached = builder.onlyIfCached;
        this.noTransform = builder.noTransform;
    }

    public boolean noCache() {
        return this.noCache;
    }

    public boolean noStore() {
        return this.noStore;
    }

    public int maxAgeSeconds() {
        return this.maxAgeSeconds;
    }

    public int sMaxAgeSeconds() {
        return this.sMaxAgeSeconds;
    }

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public boolean mustRevalidate() {
        return this.mustRevalidate;
    }

    public int maxStaleSeconds() {
        return this.maxStaleSeconds;
    }

    public int minFreshSeconds() {
        return this.minFreshSeconds;
    }

    public boolean onlyIfCached() {
        return this.onlyIfCached;
    }

    public boolean noTransform() {
        return this.noTransform;
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x00ba  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00b0  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static CacheControl parse(Headers headers) {
        boolean noTransform;
        Headers headers2 = headers;
        boolean noCache = false;
        boolean noStore = false;
        int maxAgeSeconds = -1;
        int sMaxAgeSeconds = -1;
        boolean isPrivate = false;
        boolean isPublic = false;
        boolean mustRevalidate = false;
        int maxStaleSeconds = -1;
        int minFreshSeconds = -1;
        boolean onlyIfCached = false;
        boolean noTransform2 = false;
        boolean canUseHeaderValue = true;
        String headerValue = null;
        int i = 0;
        int size = headers.size();
        while (i < size) {
            boolean noCache2;
            boolean noStore2;
            int maxAgeSeconds2;
            int size2 = size;
            String name = headers2.name(i);
            noTransform = noTransform2;
            String value = headers2.value(i);
            if (name.equalsIgnoreCase("Cache-Control")) {
                if (headerValue != null) {
                    canUseHeaderValue = false;
                } else {
                    headerValue = value;
                }
            } else if (name.equalsIgnoreCase("Pragma")) {
                canUseHeaderValue = false;
            } else {
                noTransform2 = noTransform;
                i++;
                size = size2;
                headers2 = headers;
            }
            boolean pos = false;
            while (true) {
                noCache2 = noCache;
                if (pos >= value.length()) {
                    break;
                }
                String parameter;
                boolean pos2;
                noCache = pos;
                noStore2 = noStore;
                int pos3 = HeaderParser.skipUntil(value, pos, "=,;");
                noStore = value.substring(noCache, pos3).trim();
                if (pos3 != value.length()) {
                    maxAgeSeconds2 = maxAgeSeconds;
                    if (!(value.charAt(pos3) == ',' || value.charAt(pos3) == ';')) {
                        pos3 = HeaderParser.skipWhitespace(value, pos3 + 1);
                        int parameterStart;
                        if (pos3 >= value.length() || value.charAt(pos3) != '\"') {
                            parameterStart = pos3;
                            pos = HeaderParser.skipUntil(value, pos3, ",;");
                            parameter = value.substring(parameterStart, pos).trim();
                        } else {
                            pos3++;
                            parameterStart = pos3;
                            pos3 = HeaderParser.skipUntil(value, pos3, "\"");
                            parameter = value.substring(parameterStart, pos3);
                            pos = pos3 + 1;
                        }
                        noCache = parameter;
                        if (!"no-cache".equalsIgnoreCase(noStore)) {
                            pos2 = pos;
                            noCache = true;
                        } else if ("no-store".equalsIgnoreCase(noStore)) {
                            pos2 = pos;
                            noStore = true;
                            noCache = noCache2;
                            maxAgeSeconds = maxAgeSeconds2;
                            pos = pos2;
                        } else {
                            pos2 = pos;
                            if ("max-age".equalsIgnoreCase(noStore)) {
                                maxAgeSeconds = HeaderParser.parseSeconds(noCache, -1);
                                noCache = noCache2;
                                noStore = noStore2;
                                pos = pos2;
                            } else {
                                if ("s-maxage".equalsIgnoreCase(noStore) != 0) {
                                    sMaxAgeSeconds = HeaderParser.parseSeconds(noCache, -1);
                                } else if ("private".equalsIgnoreCase(noStore)) {
                                    isPrivate = true;
                                } else if ("public".equalsIgnoreCase(noStore)) {
                                    isPublic = true;
                                } else if ("must-revalidate".equalsIgnoreCase(noStore)) {
                                    mustRevalidate = true;
                                } else if ("max-stale".equalsIgnoreCase(noStore)) {
                                    maxStaleSeconds = HeaderParser.parseSeconds(noCache, Integer.MAX_VALUE);
                                } else if ("min-fresh".equalsIgnoreCase(noStore)) {
                                    minFreshSeconds = HeaderParser.parseSeconds(noCache, -1);
                                } else if ("only-if-cached".equalsIgnoreCase(noStore)) {
                                    onlyIfCached = true;
                                } else if ("no-transform".equalsIgnoreCase(noStore)) {
                                    noTransform = true;
                                }
                                noCache = noCache2;
                            }
                        }
                        noStore = noStore2;
                        maxAgeSeconds = maxAgeSeconds2;
                        pos = pos2;
                    }
                } else {
                    maxAgeSeconds2 = maxAgeSeconds;
                }
                pos = pos3 + 1;
                parameter = null;
                noCache = parameter;
                if (!"no-cache".equalsIgnoreCase(noStore)) {
                }
                noStore = noStore2;
                maxAgeSeconds = maxAgeSeconds2;
                pos = pos2;
            }
            noStore2 = noStore;
            maxAgeSeconds2 = maxAgeSeconds;
            noTransform2 = noTransform;
            noCache = noCache2;
            i++;
            size = size2;
            headers2 = headers;
        }
        noTransform = noTransform2;
        if (!canUseHeaderValue) {
            headerValue = null;
        }
        return new CacheControl(noCache, noStore, maxAgeSeconds, sMaxAgeSeconds, isPrivate, isPublic, mustRevalidate, maxStaleSeconds, minFreshSeconds, onlyIfCached, noTransform, headerValue);
    }

    public String toString() {
        String result = this.headerValue;
        if (result != null) {
            return result;
        }
        String headerValue = headerValue();
        this.headerValue = headerValue;
        return headerValue;
    }

    private String headerValue() {
        StringBuilder result = new StringBuilder();
        if (this.noCache) {
            result.append("no-cache, ");
        }
        if (this.noStore) {
            result.append("no-store, ");
        }
        if (this.maxAgeSeconds != -1) {
            result.append("max-age=");
            result.append(this.maxAgeSeconds);
            result.append(", ");
        }
        if (this.sMaxAgeSeconds != -1) {
            result.append("s-maxage=");
            result.append(this.sMaxAgeSeconds);
            result.append(", ");
        }
        if (this.isPrivate) {
            result.append("private, ");
        }
        if (this.isPublic) {
            result.append("public, ");
        }
        if (this.mustRevalidate) {
            result.append("must-revalidate, ");
        }
        if (this.maxStaleSeconds != -1) {
            result.append("max-stale=");
            result.append(this.maxStaleSeconds);
            result.append(", ");
        }
        if (this.minFreshSeconds != -1) {
            result.append("min-fresh=");
            result.append(this.minFreshSeconds);
            result.append(", ");
        }
        if (this.onlyIfCached) {
            result.append("only-if-cached, ");
        }
        if (this.noTransform) {
            result.append("no-transform, ");
        }
        if (result.length() == 0) {
            return "";
        }
        result.delete(result.length() - 2, result.length());
        return result.toString();
    }
}
