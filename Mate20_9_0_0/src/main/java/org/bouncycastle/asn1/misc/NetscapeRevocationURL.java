package org.bouncycastle.asn1.misc;

import org.bouncycastle.asn1.DERIA5String;

public class NetscapeRevocationURL extends DERIA5String {
    public NetscapeRevocationURL(DERIA5String dERIA5String) {
        super(dERIA5String.getString());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NetscapeRevocationURL: ");
        stringBuilder.append(getString());
        return stringBuilder.toString();
    }
}
