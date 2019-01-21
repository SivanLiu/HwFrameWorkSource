package org.bouncycastle.cms;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAlgorithmProtection;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class DefaultSignedAttributeTableGenerator implements CMSAttributeTableGenerator {
    private final Hashtable table;

    public DefaultSignedAttributeTableGenerator() {
        this.table = new Hashtable();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0012 in {2, 4, 5} preds:[]
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
    public DefaultSignedAttributeTableGenerator(org.bouncycastle.asn1.cms.AttributeTable r1) {
        /*
        r0 = this;
        r0.<init>();
        if (r1 == 0) goto L_0x000c;
        r1 = r1.toHashtable();
        r0.table = r1;
        return;
        r1 = new java.util.Hashtable;
        r1.<init>();
        goto L_0x0009;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.cms.DefaultSignedAttributeTableGenerator.<init>(org.bouncycastle.asn1.cms.AttributeTable):void");
    }

    private static Hashtable copyHashTable(Hashtable hashtable) {
        Hashtable hashtable2 = new Hashtable();
        Enumeration keys = hashtable.keys();
        while (keys.hasMoreElements()) {
            Object nextElement = keys.nextElement();
            hashtable2.put(nextElement, hashtable.get(nextElement));
        }
        return hashtable2;
    }

    protected Hashtable createStandardAttributeTable(Map map) {
        Attribute attribute;
        Hashtable copyHashTable = copyHashTable(this.table);
        if (!copyHashTable.containsKey(CMSAttributes.contentType)) {
            ASN1Encodable instance = ASN1ObjectIdentifier.getInstance(map.get(CMSAttributeTableGenerator.CONTENT_TYPE));
            if (instance != null) {
                attribute = new Attribute(CMSAttributes.contentType, new DERSet(instance));
                copyHashTable.put(attribute.getAttrType(), attribute);
            }
        }
        if (!copyHashTable.containsKey(CMSAttributes.signingTime)) {
            attribute = new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date())));
            copyHashTable.put(attribute.getAttrType(), attribute);
        }
        if (!copyHashTable.containsKey(CMSAttributes.messageDigest)) {
            attribute = new Attribute(CMSAttributes.messageDigest, new DERSet(new DEROctetString((byte[]) map.get(CMSAttributeTableGenerator.DIGEST))));
            copyHashTable.put(attribute.getAttrType(), attribute);
        }
        if (!copyHashTable.contains(CMSAttributes.cmsAlgorithmProtect)) {
            Attribute attribute2 = new Attribute(CMSAttributes.cmsAlgorithmProtect, new DERSet(new CMSAlgorithmProtection((AlgorithmIdentifier) map.get(CMSAttributeTableGenerator.DIGEST_ALGORITHM_IDENTIFIER), 1, (AlgorithmIdentifier) map.get(CMSAttributeTableGenerator.SIGNATURE_ALGORITHM_IDENTIFIER))));
            copyHashTable.put(attribute2.getAttrType(), attribute2);
        }
        return copyHashTable;
    }

    public AttributeTable getAttributes(Map map) {
        return new AttributeTable(createStandardAttributeTable(map));
    }
}
