package org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1TaggedObject extends ASN1Primitive implements ASN1TaggedObjectParser {
    boolean empty;
    boolean explicit;
    ASN1Encodable obj;
    int tagNo;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:9:0x0025 in {2, 3, 7, 8} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
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
    public ASN1TaggedObject(boolean r3, int r4, org.bouncycastle.asn1.ASN1Encodable r5) {
        /*
        r2 = this;
        r2.<init>();
        r0 = 0;
        r2.empty = r0;
        r0 = 1;
        r2.explicit = r0;
        r1 = 0;
        r2.obj = r1;
        r1 = r5 instanceof org.bouncycastle.asn1.ASN1Choice;
        if (r1 == 0) goto L_0x0013;
    L_0x0010:
        r2.explicit = r0;
        goto L_0x0015;
    L_0x0013:
        r2.explicit = r3;
    L_0x0015:
        r2.tagNo = r4;
        r3 = r2.explicit;
        if (r3 == 0) goto L_0x001e;
    L_0x001b:
        r2.obj = r5;
        return;
    L_0x001e:
        r3 = r5.toASN1Primitive();
        r3 = r3 instanceof org.bouncycastle.asn1.ASN1Set;
        goto L_0x001b;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.ASN1TaggedObject.<init>(boolean, int, org.bouncycastle.asn1.ASN1Encodable):void");
    }

    public static ASN1TaggedObject getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1TaggedObject)) {
            return (ASN1TaggedObject) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct tagged object from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ASN1TaggedObject getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        if (z) {
            return (ASN1TaggedObject) aSN1TaggedObject.getObject();
        }
        throw new IllegalArgumentException("implicitly tagged tagged object");
    }

    /* JADX WARNING: Missing block: B:20:0x0039, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1TaggedObject)) {
            return false;
        }
        ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) aSN1Primitive;
        if (this.tagNo != aSN1TaggedObject.tagNo || this.empty != aSN1TaggedObject.empty || this.explicit != aSN1TaggedObject.explicit) {
            return false;
        }
        if (this.obj == null) {
            if (aSN1TaggedObject.obj != null) {
                return false;
            }
        } else if (!this.obj.toASN1Primitive().equals(aSN1TaggedObject.obj.toASN1Primitive())) {
            return false;
        }
        return true;
    }

    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public ASN1Primitive getLoadedObject() {
        return toASN1Primitive();
    }

    public ASN1Primitive getObject() {
        return this.obj != null ? this.obj.toASN1Primitive() : null;
    }

    public ASN1Encodable getObjectParser(int i, boolean z) throws IOException {
        if (i == 4) {
            return ASN1OctetString.getInstance(this, z).parser();
        }
        switch (i) {
            case 16:
                return ASN1Sequence.getInstance(this, z).parser();
            case 17:
                return ASN1Set.getInstance(this, z).parser();
            default:
                if (z) {
                    return getObject();
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("implicit tagging not implemented for tag: ");
                stringBuilder.append(i);
                throw new ASN1Exception(stringBuilder.toString());
        }
    }

    public int getTagNo() {
        return this.tagNo;
    }

    public int hashCode() {
        int i = this.tagNo;
        return this.obj != null ? i ^ this.obj.hashCode() : i;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public boolean isExplicit() {
        return this.explicit;
    }

    ASN1Primitive toDERObject() {
        return new DERTaggedObject(this.explicit, this.tagNo, this.obj);
    }

    ASN1Primitive toDLObject() {
        return new DLTaggedObject(this.explicit, this.tagNo, this.obj);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.tagNo);
        stringBuilder.append("]");
        stringBuilder.append(this.obj);
        return stringBuilder.toString();
    }
}
