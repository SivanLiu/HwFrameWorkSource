package org.bouncycastle.dvcs;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.cms.CMSSignedData;

public class DVCSResponse extends DVCSMessage {
    private org.bouncycastle.asn1.dvcs.DVCSResponse asn1;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:10:0x0037 in {5, 7, 9, 13, 15} preds:[]
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
    public DVCSResponse(org.bouncycastle.asn1.cms.ContentInfo r4) throws org.bouncycastle.dvcs.DVCSConstructionException {
        /*
        r3 = this;
        r3.<init>(r4);
        r0 = org.bouncycastle.asn1.dvcs.DVCSObjectIdentifiers.id_ct_DVCSResponseData;
        r1 = r4.getContentType();
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x0054;
    L_0x000f:
        r0 = r4.getContent();	 Catch:{ Exception -> 0x0038 }
        r0 = r0.toASN1Primitive();	 Catch:{ Exception -> 0x0038 }
        r0 = r0 instanceof org.bouncycastle.asn1.ASN1Sequence;	 Catch:{ Exception -> 0x0038 }
        if (r0 == 0) goto L_0x0026;	 Catch:{ Exception -> 0x0038 }
    L_0x001b:
        r4 = r4.getContent();	 Catch:{ Exception -> 0x0038 }
        r4 = org.bouncycastle.asn1.dvcs.DVCSResponse.getInstance(r4);	 Catch:{ Exception -> 0x0038 }
    L_0x0023:
        r3.asn1 = r4;	 Catch:{ Exception -> 0x0038 }
        return;	 Catch:{ Exception -> 0x0038 }
    L_0x0026:
        r4 = r4.getContent();	 Catch:{ Exception -> 0x0038 }
        r4 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r4);	 Catch:{ Exception -> 0x0038 }
        r4 = r4.getOctets();	 Catch:{ Exception -> 0x0038 }
        r4 = org.bouncycastle.asn1.dvcs.DVCSResponse.getInstance(r4);	 Catch:{ Exception -> 0x0038 }
        goto L_0x0023;
        return;
    L_0x0038:
        r4 = move-exception;
        r0 = new org.bouncycastle.dvcs.DVCSConstructionException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Unable to parse content: ";
        r1.append(r2);
        r2 = r4.getMessage();
        r1.append(r2);
        r1 = r1.toString();
        r0.<init>(r1, r4);
        throw r0;
    L_0x0054:
        r4 = new org.bouncycastle.dvcs.DVCSConstructionException;
        r0 = "ContentInfo not a DVCS Response";
        r4.<init>(r0);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.dvcs.DVCSResponse.<init>(org.bouncycastle.asn1.cms.ContentInfo):void");
    }

    public DVCSResponse(CMSSignedData cMSSignedData) throws DVCSConstructionException {
        this(SignedData.getInstance(cMSSignedData.toASN1Structure().getContent()).getEncapContentInfo());
    }

    public ASN1Encodable getContent() {
        return this.asn1;
    }
}
