package org.bouncycastle.asn1.cmc;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.crmf.CertTemplate;

public class ModCertTemplate extends ASN1Object {
    private final BodyPartList certReferences;
    private final CertTemplate certTemplate;
    private final BodyPartPath pkiDataReference;
    private final boolean replace;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:13:0x0057 in {4, 6, 9, 11, 12} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private ModCertTemplate(org.bouncycastle.asn1.ASN1Sequence r6) {
        /*
        r5 = this;
        r5.<init>();
        r0 = r6.size();
        r1 = 3;
        r2 = 4;
        if (r0 == r2) goto L_0x001a;
        r0 = r6.size();
        if (r0 != r1) goto L_0x0012;
        goto L_0x001a;
        r6 = new java.lang.IllegalArgumentException;
        r0 = "incorrect sequence size";
        r6.<init>(r0);
        throw r6;
        r0 = 0;
        r0 = r6.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.cmc.BodyPartPath.getInstance(r0);
        r5.pkiDataReference = r0;
        r0 = 1;
        r3 = r6.getObjectAt(r0);
        r3 = org.bouncycastle.asn1.cmc.BodyPartList.getInstance(r3);
        r5.certReferences = r3;
        r3 = r6.size();
        r4 = 2;
        if (r3 != r2) goto L_0x0050;
        r0 = r6.getObjectAt(r4);
        r0 = org.bouncycastle.asn1.ASN1Boolean.getInstance(r0);
        r0 = r0.isTrue();
        r5.replace = r0;
        r6 = r6.getObjectAt(r1);
        r6 = org.bouncycastle.asn1.crmf.CertTemplate.getInstance(r6);
        r5.certTemplate = r6;
        return;
        r5.replace = r0;
        r6 = r6.getObjectAt(r4);
        goto L_0x0049;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.cmc.ModCertTemplate.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public ModCertTemplate(BodyPartPath bodyPartPath, BodyPartList bodyPartList, boolean z, CertTemplate certTemplate) {
        this.pkiDataReference = bodyPartPath;
        this.certReferences = bodyPartList;
        this.replace = z;
        this.certTemplate = certTemplate;
    }

    public static ModCertTemplate getInstance(Object obj) {
        return obj instanceof ModCertTemplate ? (ModCertTemplate) obj : obj != null ? new ModCertTemplate(ASN1Sequence.getInstance(obj)) : null;
    }

    public BodyPartList getCertReferences() {
        return this.certReferences;
    }

    public CertTemplate getCertTemplate() {
        return this.certTemplate;
    }

    public BodyPartPath getPkiDataReference() {
        return this.pkiDataReference;
    }

    public boolean isReplacingFields() {
        return this.replace;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.pkiDataReference);
        aSN1EncodableVector.add(this.certReferences);
        if (!this.replace) {
            aSN1EncodableVector.add(ASN1Boolean.getInstance(this.replace));
        }
        aSN1EncodableVector.add(this.certTemplate);
        return new DERSequence(aSN1EncodableVector);
    }
}
