package org.bouncycastle.jcajce.provider.asymmetric.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;

public class PKCS12BagAttributeCarrierImpl implements PKCS12BagAttributeCarrier {
    private Hashtable pkcs12Attributes;
    private Vector pkcs12Ordering;

    public PKCS12BagAttributeCarrierImpl() {
        this(new Hashtable(), new Vector());
    }

    PKCS12BagAttributeCarrierImpl(Hashtable hashtable, Vector vector) {
        this.pkcs12Attributes = hashtable;
        this.pkcs12Ordering = vector;
    }

    Hashtable getAttributes() {
        return this.pkcs12Attributes;
    }

    public ASN1Encodable getBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return (ASN1Encodable) this.pkcs12Attributes.get(aSN1ObjectIdentifier);
    }

    public Enumeration getBagAttributeKeys() {
        return this.pkcs12Ordering.elements();
    }

    Vector getOrdering() {
        return this.pkcs12Ordering;
    }

    public void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        Object readObject = objectInputStream.readObject();
        if (readObject instanceof Hashtable) {
            this.pkcs12Attributes = (Hashtable) readObject;
            this.pkcs12Ordering = (Vector) objectInputStream.readObject();
            return;
        }
        ASN1InputStream aSN1InputStream = new ASN1InputStream((byte[]) readObject);
        while (true) {
            ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) aSN1InputStream.readObject();
            if (aSN1ObjectIdentifier != null) {
                setBagAttribute(aSN1ObjectIdentifier, aSN1InputStream.readObject());
            } else {
                return;
            }
        }
    }

    public void setBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        if (this.pkcs12Attributes.containsKey(aSN1ObjectIdentifier)) {
            this.pkcs12Attributes.put(aSN1ObjectIdentifier, aSN1Encodable);
            return;
        }
        this.pkcs12Attributes.put(aSN1ObjectIdentifier, aSN1Encodable);
        this.pkcs12Ordering.addElement(aSN1ObjectIdentifier);
    }

    int size() {
        return this.pkcs12Ordering.size();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:10:0x0047 in {2, 4, 8, 9} preds:[]
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
    public void writeObject(java.io.ObjectOutputStream r6) throws java.io.IOException {
        /*
        r5 = this;
        r0 = r5.pkcs12Ordering;
        r0 = r0.size();
        if (r0 != 0) goto L_0x0019;
        r0 = new java.util.Hashtable;
        r0.<init>();
        r6.writeObject(r0);
        r0 = new java.util.Vector;
        r0.<init>();
        r6.writeObject(r0);
        return;
        r0 = new java.io.ByteArrayOutputStream;
        r0.<init>();
        r1 = new org.bouncycastle.asn1.ASN1OutputStream;
        r1.<init>(r0);
        r2 = r5.getBagAttributeKeys();
        r3 = r2.hasMoreElements();
        if (r3 == 0) goto L_0x0042;
        r3 = r2.nextElement();
        r3 = (org.bouncycastle.asn1.ASN1ObjectIdentifier) r3;
        r1.writeObject(r3);
        r4 = r5.pkcs12Attributes;
        r3 = r4.get(r3);
        r3 = (org.bouncycastle.asn1.ASN1Encodable) r3;
        r1.writeObject(r3);
        goto L_0x0027;
        r0 = r0.toByteArray();
        goto L_0x0015;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl.writeObject(java.io.ObjectOutputStream):void");
    }
}
