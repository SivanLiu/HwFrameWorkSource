package org.bouncycastle.asn1;

import java.io.IOException;

public class ASN1Boolean extends ASN1Primitive {
    public static final ASN1Boolean FALSE = new ASN1Boolean(false);
    private static final byte[] FALSE_VALUE = new byte[]{(byte) 0};
    public static final ASN1Boolean TRUE = new ASN1Boolean(true);
    private static final byte[] TRUE_VALUE = new byte[]{(byte) -1};
    private final byte[] value;

    public ASN1Boolean(boolean z) {
        this.value = z ? TRUE_VALUE : FALSE_VALUE;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:11:0x0020 in {4, 6, 9, 10, 13} preds:[]
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
    ASN1Boolean(byte[] r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = r3.length;
        r1 = 1;
        if (r0 != r1) goto L_0x0021;
        r0 = 0;
        r1 = r3[r0];
        if (r1 != 0) goto L_0x0011;
        r3 = FALSE_VALUE;
        r2.value = r3;
        return;
        r0 = r3[r0];
        r1 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r0 = r0 & r1;
        if (r0 != r1) goto L_0x001b;
        r3 = TRUE_VALUE;
        goto L_0x000e;
        r3 = org.bouncycastle.util.Arrays.clone(r3);
        goto L_0x000e;
        return;
        r3 = new java.lang.IllegalArgumentException;
        r0 = "byte value should have 1 byte in it";
        r3.<init>(r0);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.ASN1Boolean.<init>(byte[]):void");
    }

    static ASN1Boolean fromOctetString(byte[] bArr) {
        if (bArr.length == 1) {
            return bArr[0] == (byte) 0 ? FALSE : (bArr[0] & 255) == 255 ? TRUE : new ASN1Boolean(bArr);
        } else {
            throw new IllegalArgumentException("BOOLEAN value should have 1 byte in it");
        }
    }

    public static ASN1Boolean getInstance(int i) {
        return i != 0 ? TRUE : FALSE;
    }

    public static ASN1Boolean getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1Boolean)) {
            return (ASN1Boolean) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1Boolean) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct boolean from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ASN1Boolean getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        Object object = aSN1TaggedObject.getObject();
        return (z || (object instanceof ASN1Boolean)) ? getInstance(object) : fromOctetString(((ASN1OctetString) object).getOctets());
    }

    public static ASN1Boolean getInstance(boolean z) {
        return z ? TRUE : FALSE;
    }

    protected boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        return (aSN1Primitive instanceof ASN1Boolean) && this.value[0] == ((ASN1Boolean) aSN1Primitive).value[0];
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(1, this.value);
    }

    int encodedLength() {
        return 3;
    }

    public int hashCode() {
        return this.value[0];
    }

    boolean isConstructed() {
        return false;
    }

    public boolean isTrue() {
        return this.value[0] != (byte) 0;
    }

    public String toString() {
        return this.value[0] != (byte) 0 ? "TRUE" : "FALSE";
    }
}
