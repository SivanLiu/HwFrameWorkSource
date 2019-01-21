package com.huawei.okhttp3;

import com.huawei.okhttp3.internal.Util;

public final class Challenge {
    private final String realm;
    private final String scheme;

    public Challenge(String scheme, String realm) {
        this.scheme = scheme;
        this.realm = realm;
    }

    public String scheme() {
        return this.scheme;
    }

    public String realm() {
        return this.realm;
    }

    public boolean equals(Object o) {
        return (o instanceof Challenge) && Util.equal(this.scheme, ((Challenge) o).scheme) && Util.equal(this.realm, ((Challenge) o).realm);
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * 29) + (this.realm != null ? this.realm.hashCode() : 0));
        if (this.scheme != null) {
            i = this.scheme.hashCode();
        }
        return result + i;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.scheme);
        stringBuilder.append(" realm=\"");
        stringBuilder.append(this.realm);
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }
}
