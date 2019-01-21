package org.bouncycastle.asn1.cryptopro;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.Arrays;

public class Gost2814789KeyWrapParameters extends ASN1Object {
    private final ASN1ObjectIdentifier encryptionParamSet;
    private final byte[] ukm;

    public Gost2814789KeyWrapParameters(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        this(aSN1ObjectIdentifier, null);
    }

    public Gost2814789KeyWrapParameters(ASN1ObjectIdentifier aSN1ObjectIdentifier, byte[] bArr) {
        this.encryptionParamSet = aSN1ObjectIdentifier;
        this.ukm = Arrays.clone(bArr);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0037 in {2, 4, 7, 10} preds:[]
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
    private Gost2814789KeyWrapParameters(org.bouncycastle.asn1.ASN1Sequence r5) {
        /*
        r4 = this;
        r4.<init>();
        r0 = r5.size();
        r1 = 1;
        r2 = 0;
        r3 = 2;
        if (r0 != r3) goto L_0x0025;
        r0 = r5.getObjectAt(r2);
        r0 = org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(r0);
        r4.encryptionParamSet = r0;
        r5 = r5.getObjectAt(r1);
        r5 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r5);
        r5 = r5.getOctets();
        r4.ukm = r5;
        return;
        r0 = r5.size();
        if (r0 != r1) goto L_0x0038;
        r5 = r5.getObjectAt(r2);
        r5 = org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(r5);
        r4.encryptionParamSet = r5;
        r5 = 0;
        goto L_0x0022;
        return;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "unknown sequence length: ";
        r1.append(r2);
        r5 = r5.size();
        r1.append(r5);
        r5 = r1.toString();
        r0.<init>(r5);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.cryptopro.Gost2814789KeyWrapParameters.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public static Gost2814789KeyWrapParameters getInstance(Object obj) {
        return obj instanceof Gost2814789KeyWrapParameters ? (Gost2814789KeyWrapParameters) obj : obj != null ? new Gost2814789KeyWrapParameters(ASN1Sequence.getInstance(obj)) : null;
    }

    public ASN1ObjectIdentifier getEncryptionParamSet() {
        return this.encryptionParamSet;
    }

    public byte[] getUkm() {
        return this.ukm;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.encryptionParamSet);
        if (this.ukm != null) {
            aSN1EncodableVector.add(new DEROctetString(this.ukm));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
