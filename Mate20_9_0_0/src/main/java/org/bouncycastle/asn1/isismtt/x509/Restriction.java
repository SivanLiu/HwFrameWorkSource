package org.bouncycastle.asn1.isismtt.x509;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x500.DirectoryString;

public class Restriction extends ASN1Object {
    private DirectoryString restriction;

    public Restriction(String str) {
        this.restriction = new DirectoryString(str);
    }

    private Restriction(DirectoryString directoryString) {
        this.restriction = directoryString;
    }

    public static Restriction getInstance(Object obj) {
        return obj instanceof Restriction ? (Restriction) obj : obj != null ? new Restriction(DirectoryString.getInstance(obj)) : null;
    }

    public DirectoryString getRestriction() {
        return this.restriction;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.restriction.toASN1Primitive();
    }
}
