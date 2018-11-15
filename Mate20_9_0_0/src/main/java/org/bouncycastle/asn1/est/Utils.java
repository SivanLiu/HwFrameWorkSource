package org.bouncycastle.asn1.est;

class Utils {
    Utils() {
    }

    static AttrOrOID[] clone(AttrOrOID[] attrOrOIDArr) {
        Object obj = new AttrOrOID[attrOrOIDArr.length];
        System.arraycopy(attrOrOIDArr, 0, obj, 0, attrOrOIDArr.length);
        return obj;
    }
}
