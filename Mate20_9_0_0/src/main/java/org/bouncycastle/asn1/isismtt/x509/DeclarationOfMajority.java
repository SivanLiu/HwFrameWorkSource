package org.bouncycastle.asn1.isismtt.x509;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class DeclarationOfMajority extends ASN1Object implements ASN1Choice {
    public static final int dateOfBirth = 2;
    public static final int fullAgeAtCountry = 1;
    public static final int notYoungerThan = 0;
    private ASN1TaggedObject declaration;

    public DeclarationOfMajority(int i) {
        this.declaration = new DERTaggedObject(false, 0, new ASN1Integer((long) i));
    }

    public DeclarationOfMajority(ASN1GeneralizedTime aSN1GeneralizedTime) {
        this.declaration = new DERTaggedObject(false, 2, aSN1GeneralizedTime);
    }

    private DeclarationOfMajority(ASN1TaggedObject aSN1TaggedObject) {
        if (aSN1TaggedObject.getTagNo() <= 2) {
            this.declaration = aSN1TaggedObject;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad tag number: ");
        stringBuilder.append(aSN1TaggedObject.getTagNo());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public DeclarationOfMajority(boolean z, String str) {
        if (str.length() > 2) {
            throw new IllegalArgumentException("country can only be 2 characters");
        } else if (z) {
            this.declaration = new DERTaggedObject(false, 1, new DERSequence(new DERPrintableString(str, true)));
        } else {
            ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
            aSN1EncodableVector.add(ASN1Boolean.FALSE);
            aSN1EncodableVector.add(new DERPrintableString(str, true));
            this.declaration = new DERTaggedObject(false, 1, new DERSequence(aSN1EncodableVector));
        }
    }

    public static DeclarationOfMajority getInstance(Object obj) {
        if (obj == null || (obj instanceof DeclarationOfMajority)) {
            return (DeclarationOfMajority) obj;
        }
        if (obj instanceof ASN1TaggedObject) {
            return new DeclarationOfMajority((ASN1TaggedObject) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ASN1Sequence fullAgeAtCountry() {
        return this.declaration.getTagNo() != 1 ? null : ASN1Sequence.getInstance(this.declaration, false);
    }

    public ASN1GeneralizedTime getDateOfBirth() {
        return this.declaration.getTagNo() != 2 ? null : ASN1GeneralizedTime.getInstance(this.declaration, false);
    }

    public int getType() {
        return this.declaration.getTagNo();
    }

    public int notYoungerThan() {
        return this.declaration.getTagNo() != 0 ? -1 : ASN1Integer.getInstance(this.declaration, false).getValue().intValue();
    }

    public ASN1Primitive toASN1Primitive() {
        return this.declaration;
    }
}
