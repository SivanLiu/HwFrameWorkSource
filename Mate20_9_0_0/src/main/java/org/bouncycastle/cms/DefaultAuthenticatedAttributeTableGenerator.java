package org.bouncycastle.cms;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAlgorithmProtection;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class DefaultAuthenticatedAttributeTableGenerator implements CMSAttributeTableGenerator {
    private final Hashtable table;

    public DefaultAuthenticatedAttributeTableGenerator() {
        this.table = new Hashtable();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0012 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public DefaultAuthenticatedAttributeTableGenerator(org.bouncycastle.asn1.cms.AttributeTable r1) {
        /*
        r0 = this;
        r0.<init>();
        if (r1 == 0) goto L_0x000c;
    L_0x0005:
        r1 = r1.toHashtable();
    L_0x0009:
        r0.table = r1;
        return;
    L_0x000c:
        r1 = new java.util.Hashtable;
        r1.<init>();
        goto L_0x0009;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.cms.DefaultAuthenticatedAttributeTableGenerator.<init>(org.bouncycastle.asn1.cms.AttributeTable):void");
    }

    protected Hashtable createStandardAttributeTable(Map map) {
        Attribute attribute;
        Hashtable hashtable = new Hashtable();
        Enumeration keys = this.table.keys();
        while (keys.hasMoreElements()) {
            Object nextElement = keys.nextElement();
            hashtable.put(nextElement, this.table.get(nextElement));
        }
        if (!hashtable.containsKey(CMSAttributes.contentType)) {
            attribute = new Attribute(CMSAttributes.contentType, new DERSet(ASN1ObjectIdentifier.getInstance(map.get(CMSAttributeTableGenerator.CONTENT_TYPE))));
            hashtable.put(attribute.getAttrType(), attribute);
        }
        if (!hashtable.containsKey(CMSAttributes.messageDigest)) {
            attribute = new Attribute(CMSAttributes.messageDigest, new DERSet(new DEROctetString((byte[]) map.get(CMSAttributeTableGenerator.DIGEST))));
            hashtable.put(attribute.getAttrType(), attribute);
        }
        if (!hashtable.contains(CMSAttributes.cmsAlgorithmProtect)) {
            Attribute attribute2 = new Attribute(CMSAttributes.cmsAlgorithmProtect, new DERSet(new CMSAlgorithmProtection((AlgorithmIdentifier) map.get(CMSAttributeTableGenerator.DIGEST_ALGORITHM_IDENTIFIER), 2, (AlgorithmIdentifier) map.get(CMSAttributeTableGenerator.MAC_ALGORITHM_IDENTIFIER))));
            hashtable.put(attribute2.getAttrType(), attribute2);
        }
        return hashtable;
    }

    public AttributeTable getAttributes(Map map) {
        return new AttributeTable(createStandardAttributeTable(map));
    }
}
