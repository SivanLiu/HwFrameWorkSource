package com.android.okhttp;

import com.android.okhttp.okio.Buffer;
import java.net.IDN;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HttpUrl {
    static final String FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~";
    static final String FRAGMENT_ENCODE_SET = "";
    static final String FRAGMENT_ENCODE_SET_URI = " \"#<>\\^`{|}";
    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static final String PASSWORD_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
    static final String PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#";
    static final String PATH_SEGMENT_ENCODE_SET_URI = "[]";
    static final String QUERY_COMPONENT_ENCODE_SET = " \"<>#&=";
    static final String QUERY_COMPONENT_ENCODE_SET_URI = "\\^`{|}";
    static final String QUERY_ENCODE_SET = " \"<>#";
    static final String USERNAME_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
    private final String fragment;
    private final String host;
    private final String password;
    private final List<String> pathSegments;
    private final int port;
    private final List<String> queryNamesAndValues;
    private final String scheme;
    private final String url;
    private final String username;

    public static final class Builder {
        String encodedFragment;
        String encodedPassword = HttpUrl.FRAGMENT_ENCODE_SET;
        final List<String> encodedPathSegments = new ArrayList();
        List<String> encodedQueryNamesAndValues;
        String encodedUsername = HttpUrl.FRAGMENT_ENCODE_SET;
        String host;
        int port = -1;
        String scheme;

        enum ParseResult {
            SUCCESS,
            MISSING_SCHEME,
            UNSUPPORTED_SCHEME,
            INVALID_PORT,
            INVALID_HOST
        }

        public Builder() {
            this.encodedPathSegments.add(HttpUrl.FRAGMENT_ENCODE_SET);
        }

        public Builder scheme(String scheme) {
            if (scheme != null) {
                if (scheme.equalsIgnoreCase("http")) {
                    this.scheme = "http";
                } else if (scheme.equalsIgnoreCase("https")) {
                    this.scheme = "https";
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unexpected scheme: ");
                    stringBuilder.append(scheme);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                return this;
            }
            throw new IllegalArgumentException("scheme == null");
        }

        public Builder username(String username) {
            if (username != null) {
                this.encodedUsername = HttpUrl.canonicalize(username, " \"':;<=>@[]^`{}|/\\?#", false, false, false, true);
                return this;
            }
            throw new IllegalArgumentException("username == null");
        }

        public Builder encodedUsername(String encodedUsername) {
            if (encodedUsername != null) {
                this.encodedUsername = HttpUrl.canonicalize(encodedUsername, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true);
                return this;
            }
            throw new IllegalArgumentException("encodedUsername == null");
        }

        public Builder password(String password) {
            if (password != null) {
                this.encodedPassword = HttpUrl.canonicalize(password, " \"':;<=>@[]^`{}|/\\?#", false, false, false, true);
                return this;
            }
            throw new IllegalArgumentException("password == null");
        }

        public Builder encodedPassword(String encodedPassword) {
            if (encodedPassword != null) {
                this.encodedPassword = HttpUrl.canonicalize(encodedPassword, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true);
                return this;
            }
            throw new IllegalArgumentException("encodedPassword == null");
        }

        public Builder host(String host) {
            if (host != null) {
                String encoded = canonicalizeHost(host, null, host.length());
                if (encoded != null) {
                    this.host = encoded;
                    return this;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected host: ");
                stringBuilder.append(host);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            throw new IllegalArgumentException("host == null");
        }

        public Builder port(int port) {
            if (port <= 0 || port > 65535) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected port: ");
                stringBuilder.append(port);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.port = port;
            return this;
        }

        int effectivePort() {
            return this.port != -1 ? this.port : HttpUrl.defaultPort(this.scheme);
        }

        public Builder addPathSegment(String pathSegment) {
            if (pathSegment != null) {
                push(pathSegment, 0, pathSegment.length(), false, false);
                return this;
            }
            throw new IllegalArgumentException("pathSegment == null");
        }

        public Builder addEncodedPathSegment(String encodedPathSegment) {
            if (encodedPathSegment != null) {
                push(encodedPathSegment, 0, encodedPathSegment.length(), false, true);
                return this;
            }
            throw new IllegalArgumentException("encodedPathSegment == null");
        }

        public Builder setPathSegment(int index, String pathSegment) {
            if (pathSegment != null) {
                String canonicalPathSegment = HttpUrl.canonicalize(pathSegment, 0, pathSegment.length(), HttpUrl.PATH_SEGMENT_ENCODE_SET, false, false, false, true);
                if (isDot(canonicalPathSegment) || isDotDot(canonicalPathSegment)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unexpected path segment: ");
                    stringBuilder.append(pathSegment);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                this.encodedPathSegments.set(index, canonicalPathSegment);
                return this;
            }
            throw new IllegalArgumentException("pathSegment == null");
        }

        public Builder setEncodedPathSegment(int index, String encodedPathSegment) {
            if (encodedPathSegment != null) {
                String canonicalPathSegment = HttpUrl.canonicalize(encodedPathSegment, 0, encodedPathSegment.length(), HttpUrl.PATH_SEGMENT_ENCODE_SET, true, false, false, true);
                this.encodedPathSegments.set(index, canonicalPathSegment);
                if (!isDot(canonicalPathSegment) && !isDotDot(canonicalPathSegment)) {
                    return this;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected path segment: ");
                stringBuilder.append(encodedPathSegment);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            throw new IllegalArgumentException("encodedPathSegment == null");
        }

        public Builder removePathSegment(int index) {
            this.encodedPathSegments.remove(index);
            if (this.encodedPathSegments.isEmpty()) {
                this.encodedPathSegments.add(HttpUrl.FRAGMENT_ENCODE_SET);
            }
            return this;
        }

        public Builder encodedPath(String encodedPath) {
            if (encodedPath == null) {
                throw new IllegalArgumentException("encodedPath == null");
            } else if (encodedPath.startsWith("/")) {
                resolvePath(encodedPath, 0, encodedPath.length());
                return this;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected encodedPath: ");
                stringBuilder.append(encodedPath);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public Builder query(String query) {
            List queryStringToNamesAndValues;
            if (query != null) {
                queryStringToNamesAndValues = HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(query, HttpUrl.QUERY_ENCODE_SET, false, false, true, true));
            } else {
                queryStringToNamesAndValues = null;
            }
            this.encodedQueryNamesAndValues = queryStringToNamesAndValues;
            return this;
        }

        public Builder encodedQuery(String encodedQuery) {
            List queryStringToNamesAndValues;
            if (encodedQuery != null) {
                queryStringToNamesAndValues = HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(encodedQuery, HttpUrl.QUERY_ENCODE_SET, true, false, true, true));
            } else {
                queryStringToNamesAndValues = null;
            }
            this.encodedQueryNamesAndValues = queryStringToNamesAndValues;
            return this;
        }

        public Builder addQueryParameter(String name, String value) {
            if (name != null) {
                Object canonicalize;
                if (this.encodedQueryNamesAndValues == null) {
                    this.encodedQueryNamesAndValues = new ArrayList();
                }
                this.encodedQueryNamesAndValues.add(HttpUrl.canonicalize(name, HttpUrl.QUERY_COMPONENT_ENCODE_SET, false, false, true, true));
                List list = this.encodedQueryNamesAndValues;
                if (value != null) {
                    canonicalize = HttpUrl.canonicalize(value, HttpUrl.QUERY_COMPONENT_ENCODE_SET, false, false, true, true);
                } else {
                    canonicalize = null;
                }
                list.add(canonicalize);
                return this;
            }
            throw new IllegalArgumentException("name == null");
        }

        public Builder addEncodedQueryParameter(String encodedName, String encodedValue) {
            if (encodedName != null) {
                Object canonicalize;
                if (this.encodedQueryNamesAndValues == null) {
                    this.encodedQueryNamesAndValues = new ArrayList();
                }
                this.encodedQueryNamesAndValues.add(HttpUrl.canonicalize(encodedName, HttpUrl.QUERY_COMPONENT_ENCODE_SET, true, false, true, true));
                List list = this.encodedQueryNamesAndValues;
                if (encodedValue != null) {
                    canonicalize = HttpUrl.canonicalize(encodedValue, HttpUrl.QUERY_COMPONENT_ENCODE_SET, true, false, true, true);
                } else {
                    canonicalize = null;
                }
                list.add(canonicalize);
                return this;
            }
            throw new IllegalArgumentException("encodedName == null");
        }

        public Builder setQueryParameter(String name, String value) {
            removeAllQueryParameters(name);
            addQueryParameter(name, value);
            return this;
        }

        public Builder setEncodedQueryParameter(String encodedName, String encodedValue) {
            removeAllEncodedQueryParameters(encodedName);
            addEncodedQueryParameter(encodedName, encodedValue);
            return this;
        }

        public Builder removeAllQueryParameters(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name == null");
            } else if (this.encodedQueryNamesAndValues == null) {
                return this;
            } else {
                removeAllCanonicalQueryParameters(HttpUrl.canonicalize(name, HttpUrl.QUERY_COMPONENT_ENCODE_SET, false, false, true, true));
                return this;
            }
        }

        public Builder removeAllEncodedQueryParameters(String encodedName) {
            if (encodedName == null) {
                throw new IllegalArgumentException("encodedName == null");
            } else if (this.encodedQueryNamesAndValues == null) {
                return this;
            } else {
                removeAllCanonicalQueryParameters(HttpUrl.canonicalize(encodedName, HttpUrl.QUERY_COMPONENT_ENCODE_SET, true, false, true, true));
                return this;
            }
        }

        private void removeAllCanonicalQueryParameters(String canonicalName) {
            for (int i = this.encodedQueryNamesAndValues.size() - 2; i >= 0; i -= 2) {
                if (canonicalName.equals(this.encodedQueryNamesAndValues.get(i))) {
                    this.encodedQueryNamesAndValues.remove(i + 1);
                    this.encodedQueryNamesAndValues.remove(i);
                    if (this.encodedQueryNamesAndValues.isEmpty()) {
                        this.encodedQueryNamesAndValues = null;
                        return;
                    }
                }
            }
        }

        public Builder fragment(String fragment) {
            String canonicalize;
            if (fragment != null) {
                canonicalize = HttpUrl.canonicalize(fragment, HttpUrl.FRAGMENT_ENCODE_SET, false, false, false, false);
            } else {
                canonicalize = null;
            }
            this.encodedFragment = canonicalize;
            return this;
        }

        public Builder encodedFragment(String encodedFragment) {
            String canonicalize;
            if (encodedFragment != null) {
                canonicalize = HttpUrl.canonicalize(encodedFragment, HttpUrl.FRAGMENT_ENCODE_SET, true, false, false, false);
            } else {
                canonicalize = null;
            }
            this.encodedFragment = canonicalize;
            return this;
        }

        Builder reencodeForUri() {
            int i;
            int size = this.encodedPathSegments.size();
            for (i = 0; i < size; i++) {
                this.encodedPathSegments.set(i, HttpUrl.canonicalize((String) this.encodedPathSegments.get(i), HttpUrl.PATH_SEGMENT_ENCODE_SET_URI, true, true, false, true));
            }
            if (this.encodedQueryNamesAndValues != null) {
                size = this.encodedQueryNamesAndValues.size();
                for (i = 0; i < size; i++) {
                    String component = (String) this.encodedQueryNamesAndValues.get(i);
                    if (component != null) {
                        this.encodedQueryNamesAndValues.set(i, HttpUrl.canonicalize(component, HttpUrl.QUERY_COMPONENT_ENCODE_SET_URI, true, true, true, true));
                    }
                }
            }
            if (this.encodedFragment != null) {
                this.encodedFragment = HttpUrl.canonicalize(this.encodedFragment, HttpUrl.FRAGMENT_ENCODE_SET_URI, true, true, false, false);
            }
            return this;
        }

        public HttpUrl build() {
            if (this.scheme == null) {
                throw new IllegalStateException("scheme == null");
            } else if (this.host != null) {
                return new HttpUrl(this);
            } else {
                throw new IllegalStateException("host == null");
            }
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(this.scheme);
            result.append("://");
            if (!(this.encodedUsername.isEmpty() && this.encodedPassword.isEmpty())) {
                result.append(this.encodedUsername);
                if (!this.encodedPassword.isEmpty()) {
                    result.append(':');
                    result.append(this.encodedPassword);
                }
                result.append('@');
            }
            if (this.host.indexOf(58) != -1) {
                result.append('[');
                result.append(this.host);
                result.append(']');
            } else {
                result.append(this.host);
            }
            int effectivePort = effectivePort();
            if (effectivePort != HttpUrl.defaultPort(this.scheme)) {
                result.append(':');
                result.append(effectivePort);
            }
            HttpUrl.pathSegmentsToString(result, this.encodedPathSegments);
            if (this.encodedQueryNamesAndValues != null) {
                result.append('?');
                HttpUrl.namesAndValuesToQueryString(result, this.encodedQueryNamesAndValues);
            }
            if (this.encodedFragment != null) {
                result.append('#');
                result.append(this.encodedFragment);
            }
            return result.toString();
        }

        ParseResult parse(HttpUrl base, String input) {
            int componentDelimiterOffset;
            int passwordColonOffset;
            String str = input;
            int pos = skipLeadingAsciiWhitespace(str, 0, input.length());
            int limit = skipTrailingAsciiWhitespace(str, pos, input.length());
            char c = 65535;
            if (schemeDelimiterOffset(str, pos, limit) != -1) {
                if (str.regionMatches(true, pos, "https:", 0, 6)) {
                    this.scheme = "https";
                    pos += "https:".length();
                } else {
                    if (!str.regionMatches(true, pos, "http:", 0, 5)) {
                        return ParseResult.UNSUPPORTED_SCHEME;
                    }
                    this.scheme = "http";
                    pos += "http:".length();
                }
            } else if (base == null) {
                return ParseResult.MISSING_SCHEME;
            } else {
                this.scheme = base.scheme;
            }
            int slashCount = slashCount(str, pos, limit);
            char c2 = '#';
            boolean hasUsername;
            boolean hasPassword;
            if (slashCount >= 2 || base == null || !base.scheme.equals(this.scheme)) {
                char c3;
                hasUsername = false;
                hasPassword = false;
                int pos2 = pos + slashCount;
                while (true) {
                    char charAt;
                    pos = HttpUrl.delimiterOffset(str, pos2, limit, "@/\\?#");
                    if (pos != limit) {
                        charAt = str.charAt(pos);
                    } else {
                        charAt = c;
                    }
                    c3 = charAt;
                    if (!(c3 == c || c3 == c2 || c3 == '/' || c3 == '\\')) {
                        switch (c3) {
                            case '?':
                                break;
                            case '@':
                                if (hasPassword) {
                                    componentDelimiterOffset = pos;
                                    int pos3 = pos2;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(this.encodedPassword);
                                    stringBuilder.append("%40");
                                    stringBuilder.append(HttpUrl.canonicalize(str, pos3, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true));
                                    this.encodedPassword = stringBuilder.toString();
                                } else {
                                    String stringBuilder2;
                                    int passwordColonOffset2 = HttpUrl.delimiterOffset(str, pos2, pos, ":");
                                    passwordColonOffset = passwordColonOffset2;
                                    componentDelimiterOffset = pos;
                                    pos2 = HttpUrl.canonicalize(str, pos2, passwordColonOffset2, " \"':;<=>@[]^`{}|/\\?#", true, false, false, 1);
                                    if (hasUsername) {
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append(this.encodedUsername);
                                        stringBuilder3.append("%40");
                                        stringBuilder3.append(pos2);
                                        stringBuilder2 = stringBuilder3.toString();
                                    } else {
                                        stringBuilder2 = pos2;
                                    }
                                    this.encodedUsername = stringBuilder2;
                                    String canonicalUsername;
                                    if (passwordColonOffset != componentDelimiterOffset) {
                                        hasPassword = true;
                                        canonicalUsername = pos2;
                                        this.encodedPassword = HttpUrl.canonicalize(str, passwordColonOffset + 1, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, false, 1);
                                    } else {
                                        canonicalUsername = pos2;
                                    }
                                    hasUsername = true;
                                }
                                pos2 = componentDelimiterOffset + 1;
                                continue;
                            default:
                                break;
                        }
                    }
                    c = 65535;
                    c2 = '#';
                }
                char c4 = c3;
                componentDelimiterOffset = pos;
                pos = pos2;
                int portColonOffset = portColonOffset(str, pos, componentDelimiterOffset);
                if (portColonOffset + 1 < componentDelimiterOffset) {
                    this.host = canonicalizeHost(str, pos, portColonOffset);
                    this.port = parsePort(str, portColonOffset + 1, componentDelimiterOffset);
                    if (this.port == -1) {
                        return ParseResult.INVALID_PORT;
                    }
                }
                this.host = canonicalizeHost(str, pos, portColonOffset);
                this.port = HttpUrl.defaultPort(this.scheme);
                if (this.host == null) {
                    return ParseResult.INVALID_HOST;
                }
                pos = componentDelimiterOffset;
            } else {
                this.encodedUsername = base.encodedUsername();
                this.encodedPassword = base.encodedPassword();
                this.host = base.host;
                this.port = base.port;
                this.encodedPathSegments.clear();
                this.encodedPathSegments.addAll(base.encodedPathSegments());
                if (pos == limit || str.charAt(pos) == '#') {
                    encodedQuery(base.encodedQuery());
                }
                hasUsername = false;
                hasPassword = false;
            }
            componentDelimiterOffset = HttpUrl.delimiterOffset(str, pos, limit, "?#");
            resolvePath(str, pos, componentDelimiterOffset);
            passwordColonOffset = componentDelimiterOffset;
            if (passwordColonOffset < limit && str.charAt(passwordColonOffset) == '?') {
                int queryDelimiterOffset = HttpUrl.delimiterOffset(str, passwordColonOffset, limit, "#");
                this.encodedQueryNamesAndValues = HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(str, passwordColonOffset + 1, queryDelimiterOffset, HttpUrl.QUERY_ENCODE_SET, true, false, true, true));
                passwordColonOffset = queryDelimiterOffset;
            }
            if (passwordColonOffset < limit && str.charAt(passwordColonOffset) == '#') {
                this.encodedFragment = HttpUrl.canonicalize(str, passwordColonOffset + 1, limit, HttpUrl.FRAGMENT_ENCODE_SET, true, false, false, false);
            }
            return ParseResult.SUCCESS;
        }

        private void resolvePath(String input, int pos, int limit) {
            if (pos != limit) {
                char c = input.charAt(pos);
                if (c == '/' || c == '\\') {
                    this.encodedPathSegments.clear();
                    this.encodedPathSegments.add(HttpUrl.FRAGMENT_ENCODE_SET);
                    pos++;
                } else {
                    this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, HttpUrl.FRAGMENT_ENCODE_SET);
                }
                int i = pos;
                while (i < limit) {
                    int pathSegmentDelimiterOffset = HttpUrl.delimiterOffset(input, i, limit, "/\\");
                    boolean segmentHasTrailingSlash = pathSegmentDelimiterOffset < limit;
                    push(input, i, pathSegmentDelimiterOffset, segmentHasTrailingSlash, true);
                    i = pathSegmentDelimiterOffset;
                    if (segmentHasTrailingSlash) {
                        i++;
                    }
                }
            }
        }

        private void push(String input, int pos, int limit, boolean addTrailingSlash, boolean alreadyEncoded) {
            String segment = HttpUrl.canonicalize(input, pos, limit, HttpUrl.PATH_SEGMENT_ENCODE_SET, alreadyEncoded, false, false, true);
            if (!isDot(segment)) {
                if (isDotDot(segment)) {
                    pop();
                    return;
                }
                if (((String) this.encodedPathSegments.get(this.encodedPathSegments.size() - 1)).isEmpty()) {
                    this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, segment);
                } else {
                    this.encodedPathSegments.add(segment);
                }
                if (addTrailingSlash) {
                    this.encodedPathSegments.add(HttpUrl.FRAGMENT_ENCODE_SET);
                }
            }
        }

        private boolean isDot(String input) {
            return input.equals(".") || input.equalsIgnoreCase("%2e");
        }

        private boolean isDotDot(String input) {
            return input.equals("..") || input.equalsIgnoreCase("%2e.") || input.equalsIgnoreCase(".%2e") || input.equalsIgnoreCase("%2e%2e");
        }

        private void pop() {
            if (!((String) this.encodedPathSegments.remove(this.encodedPathSegments.size() - 1)).isEmpty() || this.encodedPathSegments.isEmpty()) {
                this.encodedPathSegments.add(HttpUrl.FRAGMENT_ENCODE_SET);
            } else {
                this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, HttpUrl.FRAGMENT_ENCODE_SET);
            }
        }

        private int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
            int i = pos;
            while (i < limit) {
                switch (input.charAt(i)) {
                    case 9:
                    case 10:
                    case 12:
                    case 13:
                    case ' ':
                        i++;
                    default:
                        return i;
                }
            }
            return limit;
        }

        private int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
            int i = limit - 1;
            while (i >= pos) {
                switch (input.charAt(i)) {
                    case 9:
                    case 10:
                    case 12:
                    case 13:
                    case ' ':
                        i--;
                    default:
                        return i + 1;
                }
            }
            return pos;
        }

        private static int schemeDelimiterOffset(String input, int pos, int limit) {
            if (limit - pos < 2) {
                return -1;
            }
            char c0 = input.charAt(pos);
            if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) {
                return -1;
            }
            int i = pos + 1;
            while (i < limit) {
                char c = input.charAt(i);
                if ((c >= 'a' && c <= 'z') || ((c >= 'A' && c <= 'Z') || ((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.'))) {
                    i++;
                } else if (c == ':') {
                    return i;
                } else {
                    return -1;
                }
            }
            return -1;
        }

        private static int slashCount(String input, int pos, int limit) {
            int slashCount = 0;
            while (pos < limit) {
                char c = input.charAt(pos);
                if (c != '\\' && c != '/') {
                    break;
                }
                slashCount++;
                pos++;
            }
            return slashCount;
        }

        private static int portColonOffset(String input, int pos, int limit) {
            int i = pos;
            while (i < limit) {
                char charAt = input.charAt(i);
                if (charAt == ':') {
                    return i;
                }
                if (charAt == '[') {
                    do {
                        i++;
                        if (i >= limit) {
                            break;
                        }
                    } while (input.charAt(i) != ']');
                }
                i++;
            }
            return limit;
        }

        private static String canonicalizeHost(String input, int pos, int limit) {
            String percentDecoded = HttpUrl.percentDecode(input, pos, limit, false);
            if (!percentDecoded.contains(":")) {
                return domainToAscii(percentDecoded);
            }
            InetAddress inetAddress;
            if (percentDecoded.startsWith("[") && percentDecoded.endsWith("]")) {
                inetAddress = decodeIpv6(percentDecoded, 1, percentDecoded.length() - 1);
            } else {
                inetAddress = decodeIpv6(percentDecoded, 0, percentDecoded.length());
            }
            if (inetAddress == null) {
                return null;
            }
            byte[] address = inetAddress.getAddress();
            if (address.length == 16) {
                return inet6AddressToAscii(address);
            }
            throw new AssertionError();
        }

        private static InetAddress decodeIpv6(String input, int pos, int limit) {
            byte[] address = new byte[16];
            int groupOffset = -1;
            int compress = -1;
            int b = 0;
            int i = pos;
            while (i < limit) {
                if (b == address.length) {
                    return null;
                }
                if (i + 2 <= limit && input.regionMatches(i, "::", 0, 2)) {
                    if (compress == -1) {
                        i += 2;
                        b += 2;
                        compress = b;
                        if (i == limit) {
                            break;
                        }
                    }
                    return null;
                } else if (b != 0) {
                    if (input.regionMatches(i, ":", 0, 1)) {
                        i++;
                    } else if (!input.regionMatches(i, ".", 0, 1) || !decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) {
                        return null;
                    } else {
                        b += 2;
                    }
                }
                int value = 0;
                groupOffset = i;
                while (i < limit) {
                    int hexDigit = HttpUrl.decodeHexDigit(input.charAt(i));
                    if (hexDigit == -1) {
                        break;
                    }
                    value = (value << 4) + hexDigit;
                    i++;
                }
                int groupLength = i - groupOffset;
                if (groupLength == 0 || groupLength > 4) {
                    return null;
                }
                int b2 = b + 1;
                address[b] = (byte) ((value >>> 8) & 255);
                b = b2 + 1;
                address[b2] = (byte) (value & 255);
            }
            if (b != address.length) {
                if (compress == -1) {
                    return null;
                }
                System.arraycopy(address, compress, address, address.length - (b - compress), b - compress);
                Arrays.fill(address, compress, (address.length - b) + compress, (byte) 0);
            }
            try {
                return InetAddress.getByAddress(address);
            } catch (UnknownHostException e) {
                throw new AssertionError();
            }
        }

        private static boolean decodeIpv4Suffix(String input, int pos, int limit, byte[] address, int addressOffset) {
            int b = addressOffset;
            int groupOffset = pos;
            while (groupOffset < limit) {
                if (b == address.length) {
                    return false;
                }
                if (b != addressOffset) {
                    if (input.charAt(groupOffset) != '.') {
                        return false;
                    }
                    groupOffset++;
                }
                int value = 0;
                int i = groupOffset;
                while (i < limit) {
                    char c = input.charAt(i);
                    if (c < '0' || c > '9') {
                        break;
                    } else if (value == 0 && groupOffset != i) {
                        return false;
                    } else {
                        value = ((value * 10) + c) - 48;
                        if (value > 255) {
                            return false;
                        }
                        i++;
                    }
                }
                if (i - groupOffset == 0) {
                    return false;
                }
                int b2 = b + 1;
                address[b] = (byte) value;
                b = b2;
                groupOffset = i;
            }
            if (b != addressOffset + 4) {
                return false;
            }
            return true;
        }

        private static String domainToAscii(String input) {
            try {
                String result = IDN.toASCII(input).toLowerCase(Locale.US);
                if (result.isEmpty() || containsInvalidHostnameAsciiCodes(result)) {
                    return null;
                }
                return result;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        /* JADX WARNING: Missing block: B:11:0x0023, code skipped:
            return true;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii) {
            for (int i = 0; i < hostnameAscii.length(); i++) {
                char c = hostnameAscii.charAt(i);
                if (c <= 31 || c >= 127 || " #%/:?@[\\]".indexOf(c) != -1) {
                    return true;
                }
            }
            return false;
        }

        private static String inet6AddressToAscii(byte[] address) {
            int i = 0;
            int longestRunLength = 0;
            int longestRunOffset = -1;
            int currentRunOffset = 0;
            while (currentRunOffset < address.length) {
                int i2 = currentRunOffset;
                while (i2 < 16 && address[i2] == (byte) 0 && address[i2 + 1] == (byte) 0) {
                    i2 += 2;
                }
                int currentRunLength = i2 - currentRunOffset;
                if (currentRunLength > longestRunLength) {
                    longestRunOffset = currentRunOffset;
                    longestRunLength = currentRunLength;
                }
                currentRunOffset = i2 + 2;
            }
            Buffer result = new Buffer();
            while (i < address.length) {
                if (i == longestRunOffset) {
                    result.writeByte(58);
                    i += longestRunLength;
                    if (i == 16) {
                        result.writeByte(58);
                    }
                } else {
                    if (i > 0) {
                        result.writeByte(58);
                    }
                    result.writeHexadecimalUnsignedLong((long) (((address[i] & 255) << 8) | (address[i + 1] & 255)));
                    i += 2;
                }
            }
            return result.readUtf8();
        }

        private static int parsePort(String input, int pos, int limit) {
            try {
                int i = Integer.parseInt(HttpUrl.canonicalize(input, pos, limit, HttpUrl.FRAGMENT_ENCODE_SET, false, false, false, true));
                if (i <= 0 || i > 65535) {
                    return -1;
                }
                return i;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    private HttpUrl(Builder builder) {
        List percentDecode;
        this.scheme = builder.scheme;
        this.username = percentDecode(builder.encodedUsername, false);
        this.password = percentDecode(builder.encodedPassword, false);
        this.host = builder.host;
        this.port = builder.effectivePort();
        this.pathSegments = percentDecode(builder.encodedPathSegments, false);
        String str = null;
        if (builder.encodedQueryNamesAndValues != null) {
            percentDecode = percentDecode(builder.encodedQueryNamesAndValues, true);
        } else {
            percentDecode = null;
        }
        this.queryNamesAndValues = percentDecode;
        if (builder.encodedFragment != null) {
            str = percentDecode(builder.encodedFragment, false);
        }
        this.fragment = str;
        this.url = builder.toString();
    }

    public URL url() {
        try {
            return new URL(this.url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URI uri() {
        String uri = newBuilder().reencodeForUri().toString();
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            try {
                return URI.create(uri.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\p{javaWhitespace}]", FRAGMENT_ENCODE_SET));
            } catch (Exception e2) {
                throw new RuntimeException(e);
            }
        }
    }

    public String scheme() {
        return this.scheme;
    }

    public boolean isHttps() {
        return this.scheme.equals("https");
    }

    public String encodedUsername() {
        if (this.username.isEmpty()) {
            return FRAGMENT_ENCODE_SET;
        }
        int usernameStart = this.scheme.length() + 3;
        return this.url.substring(usernameStart, delimiterOffset(this.url, usernameStart, this.url.length(), ":@"));
    }

    public String username() {
        return this.username;
    }

    public String encodedPassword() {
        if (this.password.isEmpty()) {
            return FRAGMENT_ENCODE_SET;
        }
        return this.url.substring(this.url.indexOf(58, this.scheme.length() + 3) + 1, this.url.indexOf(64));
    }

    public String password() {
        return this.password;
    }

    public String host() {
        return this.host;
    }

    public int port() {
        return this.port;
    }

    public static int defaultPort(String scheme) {
        if (scheme.equals("http")) {
            return 80;
        }
        if (scheme.equals("https")) {
            return 443;
        }
        return -1;
    }

    public int pathSize() {
        return this.pathSegments.size();
    }

    public String encodedPath() {
        int pathStart = this.url.indexOf(47, this.scheme.length() + 3);
        return this.url.substring(pathStart, delimiterOffset(this.url, pathStart, this.url.length(), "?#"));
    }

    static void pathSegmentsToString(StringBuilder out, List<String> pathSegments) {
        int size = pathSegments.size();
        for (int i = 0; i < size; i++) {
            out.append('/');
            out.append((String) pathSegments.get(i));
        }
    }

    public List<String> encodedPathSegments() {
        int pathStart = this.url.indexOf(47, this.scheme.length() + 3);
        int pathEnd = delimiterOffset(this.url, pathStart, this.url.length(), "?#");
        List<String> result = new ArrayList();
        int i = pathStart;
        while (i < pathEnd) {
            i++;
            int segmentEnd = delimiterOffset(this.url, i, pathEnd, "/");
            result.add(this.url.substring(i, segmentEnd));
            i = segmentEnd;
        }
        return result;
    }

    public List<String> pathSegments() {
        return this.pathSegments;
    }

    public String encodedQuery() {
        if (this.queryNamesAndValues == null) {
            return null;
        }
        int queryStart = this.url.indexOf(63) + 1;
        return this.url.substring(queryStart, delimiterOffset(this.url, queryStart + 1, this.url.length(), "#"));
    }

    static void namesAndValuesToQueryString(StringBuilder out, List<String> namesAndValues) {
        int size = namesAndValues.size();
        for (int i = 0; i < size; i += 2) {
            String name = (String) namesAndValues.get(i);
            String value = (String) namesAndValues.get(i + 1);
            if (i > 0) {
                out.append('&');
            }
            out.append(name);
            if (value != null) {
                out.append('=');
                out.append(value);
            }
        }
    }

    static List<String> queryStringToNamesAndValues(String encodedQuery) {
        List<String> result = new ArrayList();
        int pos = 0;
        while (pos <= encodedQuery.length()) {
            int ampersandOffset = encodedQuery.indexOf(38, pos);
            if (ampersandOffset == -1) {
                ampersandOffset = encodedQuery.length();
            }
            int equalsOffset = encodedQuery.indexOf(61, pos);
            if (equalsOffset == -1 || equalsOffset > ampersandOffset) {
                result.add(encodedQuery.substring(pos, ampersandOffset));
                result.add(null);
            } else {
                result.add(encodedQuery.substring(pos, equalsOffset));
                result.add(encodedQuery.substring(equalsOffset + 1, ampersandOffset));
            }
            pos = ampersandOffset + 1;
        }
        return result;
    }

    public String query() {
        if (this.queryNamesAndValues == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        namesAndValuesToQueryString(result, this.queryNamesAndValues);
        return result.toString();
    }

    public int querySize() {
        return this.queryNamesAndValues != null ? this.queryNamesAndValues.size() / 2 : 0;
    }

    public String queryParameter(String name) {
        if (this.queryNamesAndValues == null) {
            return null;
        }
        int size = this.queryNamesAndValues.size();
        for (int i = 0; i < size; i += 2) {
            if (name.equals(this.queryNamesAndValues.get(i))) {
                return (String) this.queryNamesAndValues.get(i + 1);
            }
        }
        return null;
    }

    public Set<String> queryParameterNames() {
        if (this.queryNamesAndValues == null) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet();
        int size = this.queryNamesAndValues.size();
        for (int i = 0; i < size; i += 2) {
            result.add(this.queryNamesAndValues.get(i));
        }
        return Collections.unmodifiableSet(result);
    }

    public List<String> queryParameterValues(String name) {
        if (this.queryNamesAndValues == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList();
        int size = this.queryNamesAndValues.size();
        for (int i = 0; i < size; i += 2) {
            if (name.equals(this.queryNamesAndValues.get(i))) {
                result.add(this.queryNamesAndValues.get(i + 1));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public String queryParameterName(int index) {
        return (String) this.queryNamesAndValues.get(index * 2);
    }

    public String queryParameterValue(int index) {
        return (String) this.queryNamesAndValues.get((index * 2) + 1);
    }

    public String encodedFragment() {
        if (this.fragment == null) {
            return null;
        }
        return this.url.substring(this.url.indexOf(35) + 1);
    }

    public String fragment() {
        return this.fragment;
    }

    public HttpUrl resolve(String link) {
        Builder builder = new Builder();
        return builder.parse(this, link) == ParseResult.SUCCESS ? builder.build() : null;
    }

    public Builder newBuilder() {
        Builder result = new Builder();
        result.scheme = this.scheme;
        result.encodedUsername = encodedUsername();
        result.encodedPassword = encodedPassword();
        result.host = this.host;
        result.port = this.port != defaultPort(this.scheme) ? this.port : -1;
        result.encodedPathSegments.clear();
        result.encodedPathSegments.addAll(encodedPathSegments());
        result.encodedQuery(encodedQuery());
        result.encodedFragment = encodedFragment();
        return result;
    }

    public static HttpUrl parse(String url) {
        Builder builder = new Builder();
        if (builder.parse(null, url) == ParseResult.SUCCESS) {
            return builder.build();
        }
        return null;
    }

    public static HttpUrl get(URL url) {
        return parse(url.toString());
    }

    static HttpUrl getChecked(String url) throws MalformedURLException, UnknownHostException {
        Builder builder = new Builder();
        ParseResult result = builder.parse(null, url);
        StringBuilder stringBuilder;
        switch (result) {
            case SUCCESS:
                return builder.build();
            case INVALID_HOST:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid host: ");
                stringBuilder.append(url);
                throw new UnknownHostException(stringBuilder.toString());
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid URL: ");
                stringBuilder.append(result);
                stringBuilder.append(" for ");
                stringBuilder.append(url);
                throw new MalformedURLException(stringBuilder.toString());
        }
    }

    public static HttpUrl get(URI uri) {
        return parse(uri.toString());
    }

    public boolean equals(Object o) {
        return (o instanceof HttpUrl) && ((HttpUrl) o).url.equals(this.url);
    }

    public int hashCode() {
        return this.url.hashCode();
    }

    public String toString() {
        return this.url;
    }

    private static int delimiterOffset(String input, int pos, int limit, String delimiters) {
        for (int i = pos; i < limit; i++) {
            if (delimiters.indexOf(input.charAt(i)) != -1) {
                return i;
            }
        }
        return limit;
    }

    static String percentDecode(String encoded, boolean plusIsSpace) {
        return percentDecode(encoded, 0, encoded.length(), plusIsSpace);
    }

    private List<String> percentDecode(List<String> list, boolean plusIsSpace) {
        List<String> result = new ArrayList(list.size());
        for (String s : list) {
            result.add(s != null ? percentDecode(s, plusIsSpace) : null);
        }
        return Collections.unmodifiableList(result);
    }

    static String percentDecode(String encoded, int pos, int limit, boolean plusIsSpace) {
        for (int i = pos; i < limit; i++) {
            char c = encoded.charAt(i);
            if (c == '%' || (c == '+' && plusIsSpace)) {
                Buffer out = new Buffer();
                out.writeUtf8(encoded, pos, i);
                percentDecode(out, encoded, i, limit, plusIsSpace);
                return out.readUtf8();
            }
        }
        return encoded.substring(pos, limit);
    }

    static void percentDecode(Buffer out, String encoded, int pos, int limit, boolean plusIsSpace) {
        int i = pos;
        while (i < limit) {
            int codePoint = encoded.codePointAt(i);
            if (codePoint == 37 && i + 2 < limit) {
                int d1 = decodeHexDigit(encoded.charAt(i + 1));
                int d2 = decodeHexDigit(encoded.charAt(i + 2));
                if (!(d1 == -1 || d2 == -1)) {
                    out.writeByte((d1 << 4) + d2);
                    i += 2;
                    i += Character.charCount(codePoint);
                }
            } else if (codePoint == 43 && plusIsSpace) {
                out.writeByte(32);
                i += Character.charCount(codePoint);
            }
            out.writeUtf8CodePoint(codePoint);
            i += Character.charCount(codePoint);
        }
    }

    static boolean percentEncoded(String encoded, int pos, int limit) {
        if (pos + 2 >= limit || encoded.charAt(pos) != '%' || decodeHexDigit(encoded.charAt(pos + 1)) == -1 || decodeHexDigit(encoded.charAt(pos + 2)) == -1) {
            return false;
        }
        return true;
    }

    static int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 97) + 10;
        }
        if (c < 'A' || c > 'F') {
            return -1;
        }
        return (c - 65) + 10;
    }

    static String canonicalize(String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
        int i;
        String str;
        String str2 = input;
        int i2 = limit;
        int i3 = pos;
        while (true) {
            i = i3;
            if (i < i2) {
                int codePoint = str2.codePointAt(i);
                if (codePoint >= 32 && codePoint != 127 && (codePoint < 128 || !asciiOnly)) {
                    str = encodeSet;
                    if (str.indexOf(codePoint) != -1 || ((codePoint == 37 && (!alreadyEncoded || (strict && !percentEncoded(str2, i, i2)))) || (codePoint == 43 && plusIsSpace))) {
                        break;
                    }
                    i3 = Character.charCount(codePoint) + i;
                } else {
                    str = encodeSet;
                }
            } else {
                str = encodeSet;
                return input.substring(pos, limit);
            }
        }
        str = encodeSet;
        Buffer buffer = new Buffer();
        Buffer out = buffer;
        out.writeUtf8(str2, pos, i);
        Buffer out2 = out;
        canonicalize(buffer, str2, i, i2, str, alreadyEncoded, strict, plusIsSpace, asciiOnly);
        return out2.readUtf8();
    }

    static void canonicalize(Buffer out, String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
        Buffer utf8Buffer = null;
        int i = pos;
        while (i < limit) {
            int codePoint = input.codePointAt(i);
            if (!(alreadyEncoded && (codePoint == 9 || codePoint == 10 || codePoint == 12 || codePoint == 13))) {
                if (codePoint == 43 && plusIsSpace) {
                    out.writeUtf8(alreadyEncoded ? "+" : "%2B");
                } else if (codePoint < 32 || codePoint == 127 || ((codePoint >= 128 && asciiOnly) || encodeSet.indexOf(codePoint) != -1 || (codePoint == 37 && (!alreadyEncoded || (strict && !percentEncoded(input, i, limit)))))) {
                    if (utf8Buffer == null) {
                        utf8Buffer = new Buffer();
                    }
                    utf8Buffer.writeUtf8CodePoint(codePoint);
                    while (!utf8Buffer.exhausted()) {
                        int b = utf8Buffer.readByte() & 255;
                        out.writeByte(37);
                        out.writeByte(HEX_DIGITS[(b >> 4) & 15]);
                        out.writeByte(HEX_DIGITS[b & 15]);
                    }
                } else {
                    out.writeUtf8CodePoint(codePoint);
                }
            }
            i += Character.charCount(codePoint);
        }
    }

    static String canonicalize(String input, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
        return canonicalize(input, 0, input.length(), encodeSet, alreadyEncoded, strict, plusIsSpace, asciiOnly);
    }
}
