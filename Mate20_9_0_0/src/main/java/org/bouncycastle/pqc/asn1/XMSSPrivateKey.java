package org.bouncycastle.pqc.asn1;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.util.Arrays;

public class XMSSPrivateKey extends ASN1Object {
    private final byte[] bdsState;
    private final int index;
    private final byte[] publicSeed;
    private final byte[] root;
    private final byte[] secretKeyPRF;
    private final byte[] secretKeySeed;

    public XMSSPrivateKey(int i, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, byte[] bArr5) {
        this.index = i;
        this.secretKeySeed = Arrays.clone(bArr);
        this.secretKeyPRF = Arrays.clone(bArr2);
        this.publicSeed = Arrays.clone(bArr3);
        this.root = Arrays.clone(bArr4);
        this.bdsState = Arrays.clone(bArr5);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:15:0x00b6 in {6, 8, 11, 13, 14, 17} preds:[]
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
    private XMSSPrivateKey(org.bouncycastle.asn1.ASN1Sequence r6) {
        /*
        r5 = this;
        r5.<init>();
        r0 = 0;
        r1 = r6.getObjectAt(r0);
        r1 = org.bouncycastle.asn1.ASN1Integer.getInstance(r1);
        r1 = r1.getValue();
        r2 = 0;
        r2 = java.math.BigInteger.valueOf(r2);
        r1 = r1.equals(r2);
        if (r1 == 0) goto L_0x00b7;
        r1 = r6.size();
        r2 = 3;
        r3 = 2;
        if (r1 == r3) goto L_0x0033;
        r1 = r6.size();
        if (r1 != r2) goto L_0x002b;
        goto L_0x0033;
        r6 = new java.lang.IllegalArgumentException;
        r0 = "key sequence wrong size";
        r6.<init>(r0);
        throw r6;
        r1 = 1;
        r4 = r6.getObjectAt(r1);
        r4 = org.bouncycastle.asn1.ASN1Sequence.getInstance(r4);
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1Integer.getInstance(r0);
        r0 = r0.getValue();
        r0 = r0.intValue();
        r5.index = r0;
        r0 = r4.getObjectAt(r1);
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0);
        r0 = r0.getOctets();
        r0 = org.bouncycastle.util.Arrays.clone(r0);
        r5.secretKeySeed = r0;
        r0 = r4.getObjectAt(r3);
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0);
        r0 = r0.getOctets();
        r0 = org.bouncycastle.util.Arrays.clone(r0);
        r5.secretKeyPRF = r0;
        r0 = r4.getObjectAt(r2);
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0);
        r0 = r0.getOctets();
        r0 = org.bouncycastle.util.Arrays.clone(r0);
        r5.publicSeed = r0;
        r0 = 4;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0);
        r0 = r0.getOctets();
        r0 = org.bouncycastle.util.Arrays.clone(r0);
        r5.root = r0;
        r0 = r6.size();
        if (r0 != r2) goto L_0x00b4;
        r6 = r6.getObjectAt(r3);
        r6 = org.bouncycastle.asn1.ASN1TaggedObject.getInstance(r6);
        r6 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r6, r1);
        r6 = r6.getOctets();
        r6 = org.bouncycastle.util.Arrays.clone(r6);
        r5.bdsState = r6;
        return;
        r6 = 0;
        goto L_0x00b1;
        return;
        r6 = new java.lang.IllegalArgumentException;
        r0 = "unknown version of sequence";
        r6.<init>(r0);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.pqc.asn1.XMSSPrivateKey.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public static XMSSPrivateKey getInstance(Object obj) {
        return obj instanceof XMSSPrivateKey ? (XMSSPrivateKey) obj : obj != null ? new XMSSPrivateKey(ASN1Sequence.getInstance(obj)) : null;
    }

    public byte[] getBdsState() {
        return Arrays.clone(this.bdsState);
    }

    public int getIndex() {
        return this.index;
    }

    public byte[] getPublicSeed() {
        return Arrays.clone(this.publicSeed);
    }

    public byte[] getRoot() {
        return Arrays.clone(this.root);
    }

    public byte[] getSecretKeyPRF() {
        return Arrays.clone(this.secretKeyPRF);
    }

    public byte[] getSecretKeySeed() {
        return Arrays.clone(this.secretKeySeed);
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new ASN1Integer(0));
        ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
        aSN1EncodableVector2.add(new ASN1Integer((long) this.index));
        aSN1EncodableVector2.add(new DEROctetString(this.secretKeySeed));
        aSN1EncodableVector2.add(new DEROctetString(this.secretKeyPRF));
        aSN1EncodableVector2.add(new DEROctetString(this.publicSeed));
        aSN1EncodableVector2.add(new DEROctetString(this.root));
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector.add(new DERTaggedObject(true, 0, new DEROctetString(this.bdsState)));
        return new DERSequence(aSN1EncodableVector);
    }
}
