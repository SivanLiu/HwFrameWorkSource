package org.bouncycastle.asn1.dvcs;

import java.util.Arrays;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.PolicyInformation;

public class PathProcInput extends ASN1Object {
    private PolicyInformation[] acceptablePolicySet;
    private boolean explicitPolicyReqd = false;
    private boolean inhibitAnyPolicy = false;
    private boolean inhibitPolicyMapping = false;

    public PathProcInput(PolicyInformation[] policyInformationArr) {
        this.acceptablePolicySet = policyInformationArr;
    }

    public PathProcInput(PolicyInformation[] policyInformationArr, boolean z, boolean z2, boolean z3) {
        this.acceptablePolicySet = policyInformationArr;
        this.inhibitPolicyMapping = z;
        this.explicitPolicyReqd = z2;
        this.inhibitAnyPolicy = z3;
    }

    private static PolicyInformation[] fromSequence(ASN1Sequence aSN1Sequence) {
        PolicyInformation[] policyInformationArr = new PolicyInformation[aSN1Sequence.size()];
        for (int i = 0; i != policyInformationArr.length; i++) {
            policyInformationArr[i] = PolicyInformation.getInstance(aSN1Sequence.getObjectAt(i));
        }
        return policyInformationArr;
    }

    public static PathProcInput getInstance(Object obj) {
        if (obj instanceof PathProcInput) {
            return (PathProcInput) obj;
        }
        if (obj == null) {
            return null;
        }
        ASN1Sequence instance = ASN1Sequence.getInstance(obj);
        PathProcInput pathProcInput = new PathProcInput(fromSequence(ASN1Sequence.getInstance(instance.getObjectAt(0))));
        for (int i = 1; i < instance.size(); i++) {
            Object objectAt = instance.getObjectAt(i);
            if (objectAt instanceof ASN1Boolean) {
                pathProcInput.setInhibitPolicyMapping(ASN1Boolean.getInstance(objectAt).isTrue());
            } else if (objectAt instanceof ASN1TaggedObject) {
                ASN1TaggedObject instance2 = ASN1TaggedObject.getInstance(objectAt);
                switch (instance2.getTagNo()) {
                    case 0:
                        pathProcInput.setExplicitPolicyReqd(ASN1Boolean.getInstance(instance2, false).isTrue());
                        break;
                    case 1:
                        pathProcInput.setInhibitAnyPolicy(ASN1Boolean.getInstance(instance2, false).isTrue());
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown tag encountered: ");
                        stringBuilder.append(instance2.getTagNo());
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else {
                continue;
            }
        }
        return pathProcInput;
    }

    public static PathProcInput getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    private void setExplicitPolicyReqd(boolean z) {
        this.explicitPolicyReqd = z;
    }

    private void setInhibitAnyPolicy(boolean z) {
        this.inhibitAnyPolicy = z;
    }

    private void setInhibitPolicyMapping(boolean z) {
        this.inhibitPolicyMapping = z;
    }

    public PolicyInformation[] getAcceptablePolicySet() {
        return this.acceptablePolicySet;
    }

    public boolean isExplicitPolicyReqd() {
        return this.explicitPolicyReqd;
    }

    public boolean isInhibitAnyPolicy() {
        return this.inhibitAnyPolicy;
    }

    public boolean isInhibitPolicyMapping() {
        return this.inhibitPolicyMapping;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
        for (int i = 0; i != this.acceptablePolicySet.length; i++) {
            aSN1EncodableVector2.add(this.acceptablePolicySet[i]);
        }
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        if (this.inhibitPolicyMapping) {
            aSN1EncodableVector.add(ASN1Boolean.getInstance(this.inhibitPolicyMapping));
        }
        if (this.explicitPolicyReqd) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, ASN1Boolean.getInstance(this.explicitPolicyReqd)));
        }
        if (this.inhibitAnyPolicy) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, ASN1Boolean.getInstance(this.inhibitAnyPolicy)));
        }
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PathProcInput: {\nacceptablePolicySet: ");
        stringBuilder.append(Arrays.asList(this.acceptablePolicySet));
        stringBuilder.append("\ninhibitPolicyMapping: ");
        stringBuilder.append(this.inhibitPolicyMapping);
        stringBuilder.append("\nexplicitPolicyReqd: ");
        stringBuilder.append(this.explicitPolicyReqd);
        stringBuilder.append("\ninhibitAnyPolicy: ");
        stringBuilder.append(this.inhibitAnyPolicy);
        stringBuilder.append("\n}\n");
        return stringBuilder.toString();
    }
}
