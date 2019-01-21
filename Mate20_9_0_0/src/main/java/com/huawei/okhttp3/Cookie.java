package com.huawei.okhttp3;

import com.huawei.android.hishow.AlarmInfoEx;
import com.huawei.android.util.JlogConstantsEx;
import com.huawei.immersion.Vibetonz;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.http.HttpDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Cookie {
    private static final Pattern DAY_OF_MONTH_PATTERN = Pattern.compile("(\\d{1,2})[^\\d]*");
    private static final Pattern MONTH_PATTERN = Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})[^\\d]*");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{2,4})[^\\d]*");
    private final String domain;
    private final long expiresAt;
    private final boolean hostOnly;
    private final boolean httpOnly;
    private final String name;
    private final String path;
    private final boolean persistent;
    private final boolean secure;
    private final String value;

    public static final class Builder {
        String domain;
        long expiresAt = HttpDate.MAX_DATE;
        boolean hostOnly;
        boolean httpOnly;
        String name;
        String path = "/";
        boolean persistent;
        boolean secure;
        String value;

        public Builder name(String name) {
            if (name == null) {
                throw new NullPointerException("name == null");
            } else if (name.trim().equals(name)) {
                this.name = name;
                return this;
            } else {
                throw new IllegalArgumentException("name is not trimmed");
            }
        }

        public Builder value(String value) {
            if (value == null) {
                throw new NullPointerException("value == null");
            } else if (value.trim().equals(value)) {
                this.value = value;
                return this;
            } else {
                throw new IllegalArgumentException("value is not trimmed");
            }
        }

        public Builder expiresAt(long expiresAt) {
            if (expiresAt <= 0) {
                expiresAt = Long.MIN_VALUE;
            }
            if (expiresAt > HttpDate.MAX_DATE) {
                expiresAt = HttpDate.MAX_DATE;
            }
            this.expiresAt = expiresAt;
            this.persistent = true;
            return this;
        }

        public Builder domain(String domain) {
            return domain(domain, false);
        }

        public Builder hostOnlyDomain(String domain) {
            return domain(domain, true);
        }

        private Builder domain(String domain, boolean hostOnly) {
            if (domain != null) {
                String canonicalDomain = Util.domainToAscii(domain);
                if (canonicalDomain != null) {
                    this.domain = canonicalDomain;
                    this.hostOnly = hostOnly;
                    return this;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected domain: ");
                stringBuilder.append(domain);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            throw new NullPointerException("domain == null");
        }

        public Builder path(String path) {
            if (path.startsWith("/")) {
                this.path = path;
                return this;
            }
            throw new IllegalArgumentException("path must start with '/'");
        }

        public Builder secure() {
            this.secure = true;
            return this;
        }

        public Builder httpOnly() {
            this.httpOnly = true;
            return this;
        }

        public Cookie build() {
            return new Cookie(this);
        }
    }

    private Cookie(String name, String value, long expiresAt, String domain, String path, boolean secure, boolean httpOnly, boolean hostOnly, boolean persistent) {
        this.name = name;
        this.value = value;
        this.expiresAt = expiresAt;
        this.domain = domain;
        this.path = path;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.hostOnly = hostOnly;
        this.persistent = persistent;
    }

    Cookie(Builder builder) {
        if (builder.name == null) {
            throw new NullPointerException("builder.name == null");
        } else if (builder.value == null) {
            throw new NullPointerException("builder.value == null");
        } else if (builder.domain != null) {
            this.name = builder.name;
            this.value = builder.value;
            this.expiresAt = builder.expiresAt;
            this.domain = builder.domain;
            this.path = builder.path;
            this.secure = builder.secure;
            this.httpOnly = builder.httpOnly;
            this.persistent = builder.persistent;
            this.hostOnly = builder.hostOnly;
        } else {
            throw new NullPointerException("builder.domain == null");
        }
    }

    public String name() {
        return this.name;
    }

    public String value() {
        return this.value;
    }

    public boolean persistent() {
        return this.persistent;
    }

    public long expiresAt() {
        return this.expiresAt;
    }

    public boolean hostOnly() {
        return this.hostOnly;
    }

    public String domain() {
        return this.domain;
    }

    public String path() {
        return this.path;
    }

    public boolean httpOnly() {
        return this.httpOnly;
    }

    public boolean secure() {
        return this.secure;
    }

    public boolean matches(HttpUrl url) {
        boolean domainMatch;
        if (this.hostOnly) {
            domainMatch = url.host().equals(this.domain);
        } else {
            domainMatch = domainMatch(url, this.domain);
        }
        if (!domainMatch || !pathMatch(url, this.path)) {
            return false;
        }
        if (!this.secure || url.isHttps()) {
            return true;
        }
        return false;
    }

    private static boolean domainMatch(HttpUrl url, String domain) {
        String urlHost = url.host();
        if (urlHost.equals(domain)) {
            return true;
        }
        if (urlHost.endsWith(domain) && urlHost.charAt((urlHost.length() - domain.length()) - 1) == '.' && !Util.verifyAsIpAddress(urlHost)) {
            return true;
        }
        return false;
    }

    private static boolean pathMatch(HttpUrl url, String path) {
        String urlPath = url.encodedPath();
        if (urlPath.equals(path)) {
            return true;
        }
        if (urlPath.startsWith(path) && (path.endsWith("/") || urlPath.charAt(path.length()) == '/')) {
            return true;
        }
        return false;
    }

    public static Cookie parse(HttpUrl url, String setCookie) {
        return parse(System.currentTimeMillis(), url, setCookie);
    }

    static Cookie parse(long currentTimeMillis, HttpUrl url, String setCookie) {
        String str = setCookie;
        int limit = setCookie.length();
        char c = ';';
        int cookiePairEnd = Util.delimiterOffset(str, 0, limit, ';');
        char c2 = '=';
        int pairEqualsSign = Util.delimiterOffset(str, 0, cookiePairEnd, '=');
        if (pairEqualsSign == cookiePairEnd) {
            return null;
        }
        String cookieName = Util.trimSubstring(str, 0, pairEqualsSign);
        if (cookieName.isEmpty()) {
            return null;
        }
        int attributePairEnd;
        String attributeName;
        String trimSubstring;
        long expiresAt;
        String cookieValue = Util.trimSubstring(str, pairEqualsSign + 1, cookiePairEnd);
        long expiresAt2 = HttpDate.MAX_DATE;
        String domain = null;
        long deltaSeconds = -1;
        String path = null;
        boolean secureOnly = false;
        boolean httpOnly = false;
        boolean hostOnly = true;
        boolean persistent = false;
        int pos = cookiePairEnd + 1;
        while (pos < limit) {
            int limit2;
            attributePairEnd = Util.delimiterOffset(str, pos, limit, c);
            int attributeEqualsSign = Util.delimiterOffset(str, pos, attributePairEnd, c2);
            attributeName = Util.trimSubstring(str, pos, attributeEqualsSign);
            if (attributeEqualsSign < attributePairEnd) {
                trimSubstring = Util.trimSubstring(str, attributeEqualsSign + 1, attributePairEnd);
            } else {
                trimSubstring = "";
            }
            String attributeValue = trimSubstring;
            if (attributeName.equalsIgnoreCase("expires")) {
                str = attributeValue;
                try {
                    limit2 = limit;
                    try {
                        expiresAt2 = parseExpires(str, 0, str.length());
                        persistent = true;
                    } catch (IllegalArgumentException e) {
                    }
                } catch (IllegalArgumentException e2) {
                    limit2 = limit;
                }
            } else {
                limit2 = limit;
                str = attributeValue;
                if (attributeName.equalsIgnoreCase("max-age")) {
                    try {
                        persistent = true;
                        deltaSeconds = parseMaxAge(str);
                    } catch (NumberFormatException e3) {
                    }
                } else if (attributeName.equalsIgnoreCase("domain")) {
                    try {
                        hostOnly = false;
                        domain = parseDomain(str);
                    } catch (IllegalArgumentException e4) {
                    }
                } else if (attributeName.equalsIgnoreCase("path")) {
                    path = str;
                } else if (attributeName.equalsIgnoreCase("secure")) {
                    secureOnly = true;
                } else if (attributeName.equalsIgnoreCase("httponly")) {
                    httpOnly = true;
                }
            }
            pos = attributePairEnd + 1;
            limit = limit2;
            str = setCookie;
            c = ';';
            c2 = '=';
        }
        if (deltaSeconds == Long.MIN_VALUE) {
            expiresAt = Long.MIN_VALUE;
        } else if (deltaSeconds != -1) {
            if (deltaSeconds <= 9223372036854775L) {
                expiresAt = 1000 * deltaSeconds;
            } else {
                expiresAt = Long.MAX_VALUE;
            }
            long expiresAt3 = currentTimeMillis + expiresAt;
            expiresAt = (expiresAt3 < currentTimeMillis || expiresAt3 > HttpDate.MAX_DATE) ? HttpDate.MAX_DATE : expiresAt3;
        } else {
            expiresAt = expiresAt2;
        }
        if (domain == null) {
            trimSubstring = url.host();
            HttpUrl httpUrl = url;
        } else if (!domainMatch(url, domain)) {
            return null;
        } else {
            trimSubstring = domain;
        }
        if (path == null || !path.startsWith("/")) {
            attributeName = url.encodedPath();
            attributePairEnd = attributeName.lastIndexOf(47);
            attributeName = attributePairEnd != 0 ? attributeName.substring(0, attributePairEnd) : "/";
        } else {
            attributeName = path;
        }
        return new Cookie(cookieName, cookieValue, expiresAt, trimSubstring, attributeName, secureOnly, httpOnly, hostOnly, persistent);
    }

    private static long parseExpires(String s, int pos, int limit) {
        pos = dateCharacterOffset(s, pos, limit, false);
        int hour = -1;
        int minute = -1;
        int second = -1;
        int dayOfMonth = -1;
        int month = -1;
        int year = -1;
        Matcher matcher = TIME_PATTERN.matcher(s);
        while (pos < limit) {
            int end = dateCharacterOffset(s, pos + 1, limit, true);
            matcher.region(pos, end);
            if (hour == -1 && matcher.usePattern(TIME_PATTERN).matches()) {
                hour = Integer.parseInt(matcher.group(1));
                minute = Integer.parseInt(matcher.group(2));
                second = Integer.parseInt(matcher.group(3));
            } else if (dayOfMonth == -1 && matcher.usePattern(DAY_OF_MONTH_PATTERN).matches()) {
                dayOfMonth = Integer.parseInt(matcher.group(1));
            } else if (month == -1 && matcher.usePattern(MONTH_PATTERN).matches()) {
                month = MONTH_PATTERN.pattern().indexOf(matcher.group(1).toLowerCase(Locale.US)) / 4;
            } else if (year == -1 && matcher.usePattern(YEAR_PATTERN).matches()) {
                year = Integer.parseInt(matcher.group(1));
            }
            pos = dateCharacterOffset(s, end + 1, limit, false);
        }
        if (year >= 70 && year <= 99) {
            year += Vibetonz.HAPTIC_EVENT_TEXTVIEW_TAPWORD;
        }
        if (year >= 0 && year <= 69) {
            year += 2000;
        }
        if (year < 1601) {
            throw new IllegalArgumentException();
        } else if (month == -1) {
            throw new IllegalArgumentException();
        } else if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException();
        } else if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException();
        } else if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException();
        } else if (second < 0 || second > 59) {
            throw new IllegalArgumentException();
        } else {
            Calendar calendar = new GregorianCalendar(Util.UTC);
            calendar.setLenient(false);
            calendar.set(1, year);
            calendar.set(2, month - 1);
            calendar.set(5, dayOfMonth);
            calendar.set(11, hour);
            calendar.set(12, minute);
            calendar.set(13, second);
            calendar.set(14, 0);
            return calendar.getTimeInMillis();
        }
    }

    private static int dateCharacterOffset(String input, int pos, int limit, boolean invert) {
        for (int i = pos; i < limit; i++) {
            int c = input.charAt(i);
            boolean dateCharacter = (c < 32 && c != 9) || c >= AlarmInfoEx.EVERYDAY_CODE || ((c >= 48 && c <= 57) || ((c >= 97 && c <= JlogConstantsEx.JLID_DEF_CONTACT_ITEM_CLICK) || ((c >= 65 && c <= 90) || c == 58)));
            if (dateCharacter == (invert ^ 1)) {
                return i;
            }
        }
        return limit;
    }

    private static long parseMaxAge(String s) {
        long j = Long.MIN_VALUE;
        try {
            long parsed = Long.parseLong(s);
            if (parsed > 0) {
                j = parsed;
            }
            return j;
        } catch (NumberFormatException e) {
            if (s.matches("-?\\d+")) {
                if (!s.startsWith("-")) {
                    j = Long.MAX_VALUE;
                }
                return j;
            }
            throw e;
        }
    }

    private static String parseDomain(String s) {
        if (s.endsWith(".")) {
            throw new IllegalArgumentException();
        }
        if (s.startsWith(".")) {
            s = s.substring(1);
        }
        String canonicalDomain = Util.domainToAscii(s);
        if (canonicalDomain != null) {
            return canonicalDomain;
        }
        throw new IllegalArgumentException();
    }

    public static List<Cookie> parseAll(HttpUrl url, Headers headers) {
        List<String> cookieStrings = headers.values("Set-Cookie");
        List<Cookie> cookies = null;
        int size = cookieStrings.size();
        for (int i = 0; i < size; i++) {
            Cookie cookie = parse(url, (String) cookieStrings.get(i));
            if (cookie != null) {
                if (cookies == null) {
                    cookies = new ArrayList();
                }
                cookies.add(cookie);
            }
        }
        if (cookies != null) {
            return Collections.unmodifiableList(cookies);
        }
        return Collections.emptyList();
    }

    public String toString() {
        return toString(false);
    }

    String toString(boolean forObsoleteRfc2965) {
        StringBuilder result = new StringBuilder();
        result.append(this.name);
        result.append('=');
        result.append(this.value);
        if (this.persistent) {
            if (this.expiresAt == Long.MIN_VALUE) {
                result.append("; max-age=0");
            } else {
                result.append("; expires=");
                result.append(HttpDate.format(new Date(this.expiresAt)));
            }
        }
        if (!this.hostOnly) {
            result.append("; domain=");
            if (forObsoleteRfc2965) {
                result.append(".");
            }
            result.append(this.domain);
        }
        result.append("; path=");
        result.append(this.path);
        if (this.secure) {
            result.append("; secure");
        }
        if (this.httpOnly) {
            result.append("; httponly");
        }
        return result.toString();
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof Cookie)) {
            return false;
        }
        Cookie that = (Cookie) other;
        if (that.name.equals(this.name) && that.value.equals(this.value) && that.domain.equals(this.domain) && that.path.equals(this.path) && that.expiresAt == this.expiresAt && that.secure == this.secure && that.httpOnly == this.httpOnly && that.persistent == this.persistent && that.hostOnly == this.hostOnly) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * 17) + this.name.hashCode())) + this.value.hashCode())) + this.domain.hashCode())) + this.path.hashCode())) + ((int) (this.expiresAt ^ (this.expiresAt >>> 32))))) + (this.secure ^ 1))) + (this.httpOnly ^ 1))) + (this.persistent ^ 1))) + (this.hostOnly ^ 1);
    }
}
