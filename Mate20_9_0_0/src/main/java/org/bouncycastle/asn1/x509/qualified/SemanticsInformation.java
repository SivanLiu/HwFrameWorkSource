package org.bouncycastle.asn1.x509.qualified;

import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;

public class SemanticsInformation extends ASN1Object {
    private GeneralName[] nameRegistrationAuthorities;
    private ASN1ObjectIdentifier semanticsIdentifier;

    public SemanticsInformation(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        this.semanticsIdentifier = aSN1ObjectIdentifier;
        this.nameRegistrationAuthorities = null;
    }

    public SemanticsInformation(ASN1ObjectIdentifier aSN1ObjectIdentifier, GeneralName[] generalNameArr) {
        this.semanticsIdentifier = aSN1ObjectIdentifier;
        this.nameRegistrationAuthorities = cloneNames(generalNameArr);
    }

    private SemanticsInformation(ASN1Sequence aSN1Sequence) {
        Enumeration objects = aSN1Sequence.getObjects();
        if (aSN1Sequence.size() >= 1) {
            Object nextElement = objects.nextElement();
            if (nextElement instanceof ASN1ObjectIdentifier) {
                this.semanticsIdentifier = ASN1ObjectIdentifier.getInstance(nextElement);
                nextElement = objects.hasMoreElements() ? objects.nextElement() : null;
            }
            if (nextElement != null) {
                aSN1Sequence = ASN1Sequence.getInstance(nextElement);
                this.nameRegistrationAuthorities = new GeneralName[aSN1Sequence.size()];
                for (int i = 0; i < aSN1Sequence.size(); i++) {
                    this.nameRegistrationAuthorities[i] = GeneralName.getInstance(aSN1Sequence.getObjectAt(i));
                }
                return;
            }
            return;
        }
        throw new IllegalArgumentException("no objects in SemanticsInformation");
    }

    public SemanticsInformation(GeneralName[] generalNameArr) {
        this.semanticsIdentifier = null;
        this.nameRegistrationAuthorities = cloneNames(generalNameArr);
    }

    private static GeneralName[] cloneNames(GeneralName[] generalNameArr) {
        if (generalNameArr == null) {
            return null;
        }
        Object obj = new GeneralName[generalNameArr.length];
        System.arraycopy(generalNameArr, 0, obj, 0, generalNameArr.length);
        return obj;
    }

    public static SemanticsInformation getInstance(Object obj) {
        return obj instanceof SemanticsInformation ? (SemanticsInformation) obj : obj != null ? new SemanticsInformation(ASN1Sequence.getInstance(obj)) : null;
    }

    public GeneralName[] getNameRegistrationAuthorities() {
        return cloneNames(this.nameRegistrationAuthorities);
    }

    public ASN1ObjectIdentifier getSemanticsIdentifier() {
        return this.semanticsIdentifier;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.semanticsIdentifier != null) {
            aSN1EncodableVector.add(this.semanticsIdentifier);
        }
        if (this.nameRegistrationAuthorities != null) {
            ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
            for (ASN1Encodable add : this.nameRegistrationAuthorities) {
                aSN1EncodableVector2.add(add);
            }
            aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
