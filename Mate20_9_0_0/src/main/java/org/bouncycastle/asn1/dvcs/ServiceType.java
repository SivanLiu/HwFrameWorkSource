package org.bouncycastle.asn1.dvcs;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;

public class ServiceType extends ASN1Object {
    public static final ServiceType CCPD = new ServiceType(4);
    public static final ServiceType CPD = new ServiceType(1);
    public static final ServiceType VPKC = new ServiceType(3);
    public static final ServiceType VSD = new ServiceType(2);
    private ASN1Enumerated value;

    public ServiceType(int i) {
        this.value = new ASN1Enumerated(i);
    }

    private ServiceType(ASN1Enumerated aSN1Enumerated) {
        this.value = aSN1Enumerated;
    }

    public static ServiceType getInstance(Object obj) {
        return obj instanceof ServiceType ? (ServiceType) obj : obj != null ? new ServiceType(ASN1Enumerated.getInstance(obj)) : null;
    }

    public static ServiceType getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Enumerated.getInstance(aSN1TaggedObject, z));
    }

    public BigInteger getValue() {
        return this.value.getValue();
    }

    public ASN1Primitive toASN1Primitive() {
        return this.value;
    }

    public String toString() {
        int intValue = this.value.getValue().intValue();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(intValue);
        String str = intValue == CPD.getValue().intValue() ? "(CPD)" : intValue == VSD.getValue().intValue() ? "(VSD)" : intValue == VPKC.getValue().intValue() ? "(VPKC)" : intValue == CCPD.getValue().intValue() ? "(CCPD)" : "?";
        stringBuilder.append(str);
        return stringBuilder.toString();
    }
}
