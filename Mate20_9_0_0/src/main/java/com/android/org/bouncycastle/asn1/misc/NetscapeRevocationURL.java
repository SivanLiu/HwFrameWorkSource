package com.android.org.bouncycastle.asn1.misc;

import com.android.org.bouncycastle.asn1.DERIA5String;

public class NetscapeRevocationURL extends DERIA5String {
    public NetscapeRevocationURL(DERIA5String str) {
        super(str.getString());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NetscapeRevocationURL: ");
        stringBuilder.append(getString());
        return stringBuilder.toString();
    }
}
