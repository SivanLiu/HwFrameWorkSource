package org.bouncycastle.asn1.dvcs;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.PolicyInformation;

public class DVCSRequestInformation extends ASN1Object {
    private static final int DEFAULT_VERSION = 1;
    private static final int TAG_DATA_LOCATIONS = 3;
    private static final int TAG_DVCS = 2;
    private static final int TAG_EXTENSIONS = 4;
    private static final int TAG_REQUESTER = 0;
    private static final int TAG_REQUEST_POLICY = 1;
    private GeneralNames dataLocations;
    private GeneralNames dvcs;
    private Extensions extensions;
    private BigInteger nonce;
    private PolicyInformation requestPolicy;
    private DVCSTime requestTime;
    private GeneralNames requester;
    private ServiceType service;
    private int version = 1;

    private DVCSRequestInformation(ASN1Sequence aSN1Sequence) {
        int i = 1;
        if (aSN1Sequence.getObjectAt(0) instanceof ASN1Integer) {
            this.version = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(0)).getValue().intValue();
        } else {
            this.version = 1;
            i = 0;
        }
        this.service = ServiceType.getInstance(aSN1Sequence.getObjectAt(i));
        for (int i2 = i + 1; i2 < aSN1Sequence.size(); i2++) {
            ASN1Encodable objectAt = aSN1Sequence.getObjectAt(i2);
            if (objectAt instanceof ASN1Integer) {
                this.nonce = ASN1Integer.getInstance(objectAt).getValue();
            } else if (!(objectAt instanceof ASN1GeneralizedTime) && (objectAt instanceof ASN1TaggedObject)) {
                ASN1TaggedObject instance = ASN1TaggedObject.getInstance(objectAt);
                int tagNo = instance.getTagNo();
                switch (tagNo) {
                    case 0:
                        this.requester = GeneralNames.getInstance(instance, false);
                        break;
                    case 1:
                        this.requestPolicy = PolicyInformation.getInstance(ASN1Sequence.getInstance(instance, false));
                        break;
                    case 2:
                        this.dvcs = GeneralNames.getInstance(instance, false);
                        break;
                    case 3:
                        this.dataLocations = GeneralNames.getInstance(instance, false);
                        break;
                    case 4:
                        this.extensions = Extensions.getInstance(instance, false);
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown tag number encountered: ");
                        stringBuilder.append(tagNo);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else {
                this.requestTime = DVCSTime.getInstance(objectAt);
            }
        }
    }

    public static DVCSRequestInformation getInstance(Object obj) {
        return obj instanceof DVCSRequestInformation ? (DVCSRequestInformation) obj : obj != null ? new DVCSRequestInformation(ASN1Sequence.getInstance(obj)) : null;
    }

    public static DVCSRequestInformation getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public GeneralNames getDVCS() {
        return this.dvcs;
    }

    public GeneralNames getDataLocations() {
        return this.dataLocations;
    }

    public Extensions getExtensions() {
        return this.extensions;
    }

    public BigInteger getNonce() {
        return this.nonce;
    }

    public PolicyInformation getRequestPolicy() {
        return this.requestPolicy;
    }

    public DVCSTime getRequestTime() {
        return this.requestTime;
    }

    public GeneralNames getRequester() {
        return this.requester;
    }

    public ServiceType getService() {
        return this.service;
    }

    public int getVersion() {
        return this.version;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.version != 1) {
            aSN1EncodableVector.add(new ASN1Integer((long) this.version));
        }
        aSN1EncodableVector.add(this.service);
        if (this.nonce != null) {
            aSN1EncodableVector.add(new ASN1Integer(this.nonce));
        }
        if (this.requestTime != null) {
            aSN1EncodableVector.add(this.requestTime);
        }
        int[] iArr = new int[]{0, 1, 2, 3, 4};
        ASN1Encodable[] aSN1EncodableArr = new ASN1Encodable[]{this.requester, this.requestPolicy, this.dvcs, this.dataLocations, this.extensions};
        for (int i = 0; i < iArr.length; i++) {
            int i2 = iArr[i];
            ASN1Encodable aSN1Encodable = aSN1EncodableArr[i];
            if (aSN1Encodable != null) {
                aSN1EncodableVector.add(new DERTaggedObject(false, i2, aSN1Encodable));
            }
        }
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("DVCSRequestInformation {\n");
        if (this.version != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("version: ");
            stringBuilder.append(this.version);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("service: ");
        stringBuilder.append(this.service);
        stringBuilder.append("\n");
        stringBuffer.append(stringBuilder.toString());
        if (this.nonce != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("nonce: ");
            stringBuilder.append(this.nonce);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.requestTime != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("requestTime: ");
            stringBuilder.append(this.requestTime);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.requester != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("requester: ");
            stringBuilder.append(this.requester);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.requestPolicy != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("requestPolicy: ");
            stringBuilder.append(this.requestPolicy);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.dvcs != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("dvcs: ");
            stringBuilder.append(this.dvcs);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.dataLocations != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("dataLocations: ");
            stringBuilder.append(this.dataLocations);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.extensions != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("extensions: ");
            stringBuilder.append(this.extensions);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        stringBuffer.append("}\n");
        return stringBuffer.toString();
    }
}
