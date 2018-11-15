package org.bouncycastle.asn1.x509.sigi;

import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.DirectoryString;

public class NameOrPseudonym extends ASN1Object implements ASN1Choice {
    private ASN1Sequence givenName;
    private DirectoryString pseudonym;
    private DirectoryString surname;

    public NameOrPseudonym(String str) {
        this(new DirectoryString(str));
    }

    private NameOrPseudonym(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() != 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad sequence size: ");
            stringBuilder.append(aSN1Sequence.size());
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (aSN1Sequence.getObjectAt(0) instanceof ASN1String) {
            this.surname = DirectoryString.getInstance(aSN1Sequence.getObjectAt(0));
            this.givenName = ASN1Sequence.getInstance(aSN1Sequence.getObjectAt(1));
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bad object encountered: ");
            stringBuilder2.append(aSN1Sequence.getObjectAt(0).getClass());
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    public NameOrPseudonym(DirectoryString directoryString) {
        this.pseudonym = directoryString;
    }

    public NameOrPseudonym(DirectoryString directoryString, ASN1Sequence aSN1Sequence) {
        this.surname = directoryString;
        this.givenName = aSN1Sequence;
    }

    public static NameOrPseudonym getInstance(Object obj) {
        if (obj == null || (obj instanceof NameOrPseudonym)) {
            return (NameOrPseudonym) obj;
        }
        if (obj instanceof ASN1String) {
            return new NameOrPseudonym(DirectoryString.getInstance(obj));
        }
        if (obj instanceof ASN1Sequence) {
            return new NameOrPseudonym((ASN1Sequence) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public DirectoryString[] getGivenName() {
        DirectoryString[] directoryStringArr = new DirectoryString[this.givenName.size()];
        Enumeration objects = this.givenName.getObjects();
        int i = 0;
        while (objects.hasMoreElements()) {
            int i2 = i + 1;
            directoryStringArr[i] = DirectoryString.getInstance(objects.nextElement());
            i = i2;
        }
        return directoryStringArr;
    }

    public DirectoryString getPseudonym() {
        return this.pseudonym;
    }

    public DirectoryString getSurname() {
        return this.surname;
    }

    public ASN1Primitive toASN1Primitive() {
        if (this.pseudonym != null) {
            return this.pseudonym.toASN1Primitive();
        }
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.surname);
        aSN1EncodableVector.add(this.givenName);
        return new DERSequence(aSN1EncodableVector);
    }
}
