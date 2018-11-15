package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class RoleSyntax extends ASN1Object {
    private GeneralNames roleAuthority;
    private GeneralName roleName;

    public RoleSyntax(String str) {
        if (str == null) {
            str = "";
        }
        this(new GeneralName(6, str));
    }

    private RoleSyntax(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() < 1 || aSN1Sequence.size() > 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad sequence size: ");
            stringBuilder.append(aSN1Sequence.size());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        for (int i = 0; i != aSN1Sequence.size(); i++) {
            ASN1TaggedObject instance = ASN1TaggedObject.getInstance(aSN1Sequence.getObjectAt(i));
            switch (instance.getTagNo()) {
                case 0:
                    this.roleAuthority = GeneralNames.getInstance(instance, false);
                    break;
                case 1:
                    this.roleName = GeneralName.getInstance(instance, true);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown tag in RoleSyntax");
            }
        }
    }

    public RoleSyntax(GeneralName generalName) {
        this(null, generalName);
    }

    public RoleSyntax(GeneralNames generalNames, GeneralName generalName) {
        if (generalName == null || generalName.getTagNo() != 6 || ((ASN1String) generalName.getName()).getString().equals("")) {
            throw new IllegalArgumentException("the role name MUST be non empty and MUST use the URI option of GeneralName");
        }
        this.roleAuthority = generalNames;
        this.roleName = generalName;
    }

    public static RoleSyntax getInstance(Object obj) {
        return obj instanceof RoleSyntax ? (RoleSyntax) obj : obj != null ? new RoleSyntax(ASN1Sequence.getInstance(obj)) : null;
    }

    public GeneralNames getRoleAuthority() {
        return this.roleAuthority;
    }

    public String[] getRoleAuthorityAsString() {
        int i = 0;
        if (this.roleAuthority == null) {
            return new String[0];
        }
        GeneralName[] names = this.roleAuthority.getNames();
        String[] strArr = new String[names.length];
        while (i < names.length) {
            ASN1Encodable name = names[i].getName();
            if (name instanceof ASN1String) {
                strArr[i] = ((ASN1String) name).getString();
            } else {
                strArr[i] = name.toString();
            }
            i++;
        }
        return strArr;
    }

    public GeneralName getRoleName() {
        return this.roleName;
    }

    public String getRoleNameAsString() {
        return ((ASN1String) this.roleName.getName()).getString();
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.roleAuthority != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.roleAuthority));
        }
        aSN1EncodableVector.add(new DERTaggedObject(true, 1, this.roleName));
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Name: ");
        stringBuilder.append(getRoleNameAsString());
        stringBuilder.append(" - Auth: ");
        StringBuffer stringBuffer = new StringBuffer(stringBuilder.toString());
        if (this.roleAuthority == null || this.roleAuthority.getNames().length == 0) {
            stringBuffer.append("N/A");
        } else {
            String[] roleAuthorityAsString = getRoleAuthorityAsString();
            stringBuffer.append('[');
            stringBuffer.append(roleAuthorityAsString[0]);
            for (int i = 1; i < roleAuthorityAsString.length; i++) {
                stringBuffer.append(", ");
                stringBuffer.append(roleAuthorityAsString[i]);
            }
            stringBuffer.append(']');
        }
        return stringBuffer.toString();
    }
}
