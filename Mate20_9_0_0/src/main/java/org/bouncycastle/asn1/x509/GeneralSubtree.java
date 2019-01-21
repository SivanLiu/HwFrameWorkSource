package org.bouncycastle.asn1.x509;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class GeneralSubtree extends ASN1Object {
    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private GeneralName base;
    private ASN1Integer maximum;
    private ASN1Integer minimum;

    /* JADX WARNING: Missing block: B:16:0x00b5, code skipped:
            r4.maximum = org.bouncycastle.asn1.ASN1Integer.getInstance(r5, false);
     */
    /* JADX WARNING: Missing block: B:17:0x00bb, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:0x00c2, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private GeneralSubtree(ASN1Sequence aSN1Sequence) {
        this.base = GeneralName.getInstance(aSN1Sequence.getObjectAt(0));
        ASN1TaggedObject instance;
        StringBuilder stringBuilder;
        switch (aSN1Sequence.size()) {
            case 1:
                break;
            case 2:
                instance = ASN1TaggedObject.getInstance(aSN1Sequence.getObjectAt(1));
                switch (instance.getTagNo()) {
                    case 0:
                        this.minimum = ASN1Integer.getInstance(instance, false);
                        break;
                    case 1:
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad tag number: ");
                        stringBuilder.append(instance.getTagNo());
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            case 3:
                ASN1TaggedObject instance2 = ASN1TaggedObject.getInstance(aSN1Sequence.getObjectAt(1));
                if (instance2.getTagNo() == 0) {
                    this.minimum = ASN1Integer.getInstance(instance2, false);
                    instance = ASN1TaggedObject.getInstance(aSN1Sequence.getObjectAt(2));
                    if (instance.getTagNo() != 1) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad tag number for 'maximum': ");
                        stringBuilder.append(instance.getTagNo());
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Bad tag number for 'minimum': ");
                stringBuilder2.append(instance2.getTagNo());
                throw new IllegalArgumentException(stringBuilder2.toString());
                break;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad sequence size: ");
                stringBuilder.append(aSN1Sequence.size());
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public GeneralSubtree(GeneralName generalName) {
        this(generalName, null, null);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x001a in {2, 4, 6, 7} preds:[]
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
    public GeneralSubtree(org.bouncycastle.asn1.x509.GeneralName r1, java.math.BigInteger r2, java.math.BigInteger r3) {
        /*
        r0 = this;
        r0.<init>();
        r0.base = r1;
        if (r3 == 0) goto L_0x000e;
        r1 = new org.bouncycastle.asn1.ASN1Integer;
        r1.<init>(r3);
        r0.maximum = r1;
        if (r2 != 0) goto L_0x0014;
        r1 = 0;
        r0.minimum = r1;
        return;
        r1 = new org.bouncycastle.asn1.ASN1Integer;
        r1.<init>(r2);
        goto L_0x0011;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.GeneralSubtree.<init>(org.bouncycastle.asn1.x509.GeneralName, java.math.BigInteger, java.math.BigInteger):void");
    }

    public static GeneralSubtree getInstance(Object obj) {
        return obj == null ? null : obj instanceof GeneralSubtree ? (GeneralSubtree) obj : new GeneralSubtree(ASN1Sequence.getInstance(obj));
    }

    public static GeneralSubtree getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return new GeneralSubtree(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public GeneralName getBase() {
        return this.base;
    }

    public BigInteger getMaximum() {
        return this.maximum == null ? null : this.maximum.getValue();
    }

    public BigInteger getMinimum() {
        return this.minimum == null ? ZERO : this.minimum.getValue();
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.base);
        if (!(this.minimum == null || this.minimum.getValue().equals(ZERO))) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.minimum));
        }
        if (this.maximum != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, this.maximum));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
