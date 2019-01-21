package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class PasswordRecipientInfo extends ASN1Object {
    private ASN1OctetString encryptedKey;
    private AlgorithmIdentifier keyDerivationAlgorithm;
    private AlgorithmIdentifier keyEncryptionAlgorithm;
    private ASN1Integer version;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0045 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public PasswordRecipientInfo(org.bouncycastle.asn1.ASN1Sequence r5) {
        /*
        r4 = this;
        r4.<init>();
        r0 = 0;
        r1 = r5.getObjectAt(r0);
        r1 = (org.bouncycastle.asn1.ASN1Integer) r1;
        r4.version = r1;
        r1 = 1;
        r2 = r5.getObjectAt(r1);
        r2 = r2 instanceof org.bouncycastle.asn1.ASN1TaggedObject;
        r3 = 2;
        if (r2 == 0) goto L_0x0036;
        r1 = r5.getObjectAt(r1);
        r1 = (org.bouncycastle.asn1.ASN1TaggedObject) r1;
        r0 = org.bouncycastle.asn1.x509.AlgorithmIdentifier.getInstance(r1, r0);
        r4.keyDerivationAlgorithm = r0;
        r0 = r5.getObjectAt(r3);
        r0 = org.bouncycastle.asn1.x509.AlgorithmIdentifier.getInstance(r0);
        r4.keyEncryptionAlgorithm = r0;
        r0 = 3;
        r5 = r5.getObjectAt(r0);
        r5 = (org.bouncycastle.asn1.ASN1OctetString) r5;
        r4.encryptedKey = r5;
        return;
        r0 = r5.getObjectAt(r1);
        r0 = org.bouncycastle.asn1.x509.AlgorithmIdentifier.getInstance(r0);
        r4.keyEncryptionAlgorithm = r0;
        r5 = r5.getObjectAt(r3);
        goto L_0x0031;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.cms.PasswordRecipientInfo.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public PasswordRecipientInfo(AlgorithmIdentifier algorithmIdentifier, ASN1OctetString aSN1OctetString) {
        this.version = new ASN1Integer(0);
        this.keyEncryptionAlgorithm = algorithmIdentifier;
        this.encryptedKey = aSN1OctetString;
    }

    public PasswordRecipientInfo(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2, ASN1OctetString aSN1OctetString) {
        this.version = new ASN1Integer(0);
        this.keyDerivationAlgorithm = algorithmIdentifier;
        this.keyEncryptionAlgorithm = algorithmIdentifier2;
        this.encryptedKey = aSN1OctetString;
    }

    public static PasswordRecipientInfo getInstance(Object obj) {
        return obj instanceof PasswordRecipientInfo ? (PasswordRecipientInfo) obj : obj != null ? new PasswordRecipientInfo(ASN1Sequence.getInstance(obj)) : null;
    }

    public static PasswordRecipientInfo getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public ASN1OctetString getEncryptedKey() {
        return this.encryptedKey;
    }

    public AlgorithmIdentifier getKeyDerivationAlgorithm() {
        return this.keyDerivationAlgorithm;
    }

    public AlgorithmIdentifier getKeyEncryptionAlgorithm() {
        return this.keyEncryptionAlgorithm;
    }

    public ASN1Integer getVersion() {
        return this.version;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.version);
        if (this.keyDerivationAlgorithm != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.keyDerivationAlgorithm));
        }
        aSN1EncodableVector.add(this.keyEncryptionAlgorithm);
        aSN1EncodableVector.add(this.encryptedKey);
        return new DERSequence(aSN1EncodableVector);
    }
}
