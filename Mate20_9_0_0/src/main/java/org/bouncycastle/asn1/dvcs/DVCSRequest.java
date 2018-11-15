package org.bouncycastle.asn1.dvcs;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;

public class DVCSRequest extends ASN1Object {
    private Data data;
    private DVCSRequestInformation requestInformation;
    private GeneralName transactionIdentifier;

    private DVCSRequest(ASN1Sequence aSN1Sequence) {
        this.requestInformation = DVCSRequestInformation.getInstance(aSN1Sequence.getObjectAt(0));
        this.data = Data.getInstance(aSN1Sequence.getObjectAt(1));
        if (aSN1Sequence.size() > 2) {
            this.transactionIdentifier = GeneralName.getInstance(aSN1Sequence.getObjectAt(2));
        }
    }

    public DVCSRequest(DVCSRequestInformation dVCSRequestInformation, Data data) {
        this(dVCSRequestInformation, data, null);
    }

    public DVCSRequest(DVCSRequestInformation dVCSRequestInformation, Data data, GeneralName generalName) {
        this.requestInformation = dVCSRequestInformation;
        this.data = data;
        this.transactionIdentifier = generalName;
    }

    public static DVCSRequest getInstance(Object obj) {
        return obj instanceof DVCSRequest ? (DVCSRequest) obj : obj != null ? new DVCSRequest(ASN1Sequence.getInstance(obj)) : null;
    }

    public static DVCSRequest getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public Data getData() {
        return this.data;
    }

    public DVCSRequestInformation getRequestInformation() {
        return this.requestInformation;
    }

    public GeneralName getTransactionIdentifier() {
        return this.transactionIdentifier;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.requestInformation);
        aSN1EncodableVector.add(this.data);
        if (this.transactionIdentifier != null) {
            aSN1EncodableVector.add(this.transactionIdentifier);
        }
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        String stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("DVCSRequest {\nrequestInformation: ");
        stringBuilder2.append(this.requestInformation);
        stringBuilder2.append("\ndata: ");
        stringBuilder2.append(this.data);
        stringBuilder2.append("\n");
        if (this.transactionIdentifier != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("transactionIdentifier: ");
            stringBuilder3.append(this.transactionIdentifier);
            stringBuilder3.append("\n");
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = "";
        }
        stringBuilder2.append(stringBuilder);
        stringBuilder2.append("}\n");
        return stringBuilder2.toString();
    }
}
