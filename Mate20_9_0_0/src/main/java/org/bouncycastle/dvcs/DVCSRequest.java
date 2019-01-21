package org.bouncycastle.dvcs;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cms.CMSSignedData;

public class DVCSRequest extends DVCSMessage {
    private org.bouncycastle.asn1.dvcs.DVCSRequest asn1;
    private DVCSRequestData data;
    private DVCSRequestInfo reqInfo;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:23:0x00ac in {5, 8, 11, 13, 16, 19, 22, 25, 28, 30} preds:[]
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
    public DVCSRequest(org.bouncycastle.asn1.cms.ContentInfo r4) throws org.bouncycastle.dvcs.DVCSConstructionException {
        /*
        r3 = this;
        r3.<init>(r4);
        r0 = org.bouncycastle.asn1.dvcs.DVCSObjectIdentifiers.id_ct_DVCSRequestData;
        r1 = r4.getContentType();
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x00e0;
        r0 = r4.getContent();	 Catch:{ Exception -> 0x00c4 }
        r0 = r0.toASN1Primitive();	 Catch:{ Exception -> 0x00c4 }
        r0 = r0 instanceof org.bouncycastle.asn1.ASN1Sequence;	 Catch:{ Exception -> 0x00c4 }
        if (r0 == 0) goto L_0x0026;	 Catch:{ Exception -> 0x00c4 }
        r4 = r4.getContent();	 Catch:{ Exception -> 0x00c4 }
        r4 = org.bouncycastle.asn1.dvcs.DVCSRequest.getInstance(r4);	 Catch:{ Exception -> 0x00c4 }
        r3.asn1 = r4;	 Catch:{ Exception -> 0x00c4 }
        goto L_0x0037;	 Catch:{ Exception -> 0x00c4 }
        r4 = r4.getContent();	 Catch:{ Exception -> 0x00c4 }
        r4 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r4);	 Catch:{ Exception -> 0x00c4 }
        r4 = r4.getOctets();	 Catch:{ Exception -> 0x00c4 }
        r4 = org.bouncycastle.asn1.dvcs.DVCSRequest.getInstance(r4);	 Catch:{ Exception -> 0x00c4 }
        goto L_0x0023;
        r4 = new org.bouncycastle.dvcs.DVCSRequestInfo;
        r0 = r3.asn1;
        r0 = r0.getRequestInformation();
        r4.<init>(r0);
        r3.reqInfo = r4;
        r4 = r3.reqInfo;
        r4 = r4.getServiceType();
        r0 = org.bouncycastle.asn1.dvcs.ServiceType.CPD;
        r0 = r0.getValue();
        r0 = r0.intValue();
        if (r4 != r0) goto L_0x0064;
        r4 = new org.bouncycastle.dvcs.CPDRequestData;
        r0 = r3.asn1;
        r0 = r0.getData();
        r4.<init>(r0);
        r3.data = r4;
        return;
        r0 = org.bouncycastle.asn1.dvcs.ServiceType.VSD;
        r0 = r0.getValue();
        r0 = r0.intValue();
        if (r4 != r0) goto L_0x007c;
        r4 = new org.bouncycastle.dvcs.VSDRequestData;
        r0 = r3.asn1;
        r0 = r0.getData();
        r4.<init>(r0);
        goto L_0x0061;
        r0 = org.bouncycastle.asn1.dvcs.ServiceType.VPKC;
        r0 = r0.getValue();
        r0 = r0.intValue();
        if (r4 != r0) goto L_0x0094;
        r4 = new org.bouncycastle.dvcs.VPKCRequestData;
        r0 = r3.asn1;
        r0 = r0.getData();
        r4.<init>(r0);
        goto L_0x0061;
        r0 = org.bouncycastle.asn1.dvcs.ServiceType.CCPD;
        r0 = r0.getValue();
        r0 = r0.intValue();
        if (r4 != r0) goto L_0x00ad;
        r4 = new org.bouncycastle.dvcs.CCPDRequestData;
        r0 = r3.asn1;
        r0 = r0.getData();
        r4.<init>(r0);
        goto L_0x0061;
        return;
        r0 = new org.bouncycastle.dvcs.DVCSConstructionException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Unknown service type: ";
        r1.append(r2);
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
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
        r4 = new org.bouncycastle.dvcs.DVCSConstructionException;
        r0 = "ContentInfo not a DVCS Request";
        r4.<init>(r0);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.dvcs.DVCSRequest.<init>(org.bouncycastle.asn1.cms.ContentInfo):void");
    }

    public DVCSRequest(CMSSignedData cMSSignedData) throws DVCSConstructionException {
        this(SignedData.getInstance(cMSSignedData.toASN1Structure().getContent()).getEncapContentInfo());
    }

    public ASN1Encodable getContent() {
        return this.asn1;
    }

    public DVCSRequestData getData() {
        return this.data;
    }

    public DVCSRequestInfo getRequestInfo() {
        return this.reqInfo;
    }

    public GeneralName getTransactionIdentifier() {
        return this.asn1.getTransactionIdentifier();
    }
}
