package org.bouncycastle.cms;

import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;

public class KeyTransRecipientInformation extends RecipientInformation {
    private KeyTransRecipientInfo info;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0041 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    KeyTransRecipientInformation(org.bouncycastle.asn1.cms.KeyTransRecipientInfo r2, org.bouncycastle.asn1.x509.AlgorithmIdentifier r3, org.bouncycastle.cms.CMSSecureReadable r4, org.bouncycastle.cms.AuthAttributesProvider r5) {
        /*
        r1 = this;
        r0 = r2.getKeyEncryptionAlgorithm();
        r1.<init>(r0, r3, r4, r5);
        r1.info = r2;
        r2 = r2.getRecipientIdentifier();
        r3 = r2.isTagged();
        if (r3 == 0) goto L_0x0027;
        r2 = r2.getId();
        r2 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r2);
        r3 = new org.bouncycastle.cms.KeyTransRecipientId;
        r2 = r2.getOctets();
        r3.<init>(r2);
        r1.rid = r3;
        return;
        r2 = r2.getId();
        r2 = org.bouncycastle.asn1.cms.IssuerAndSerialNumber.getInstance(r2);
        r3 = new org.bouncycastle.cms.KeyTransRecipientId;
        r4 = r2.getName();
        r2 = r2.getSerialNumber();
        r2 = r2.getValue();
        r3.<init>(r4, r2);
        goto L_0x0024;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.cms.KeyTransRecipientInformation.<init>(org.bouncycastle.asn1.cms.KeyTransRecipientInfo, org.bouncycastle.asn1.x509.AlgorithmIdentifier, org.bouncycastle.cms.CMSSecureReadable, org.bouncycastle.cms.AuthAttributesProvider):void");
    }

    protected RecipientOperator getRecipientOperator(Recipient recipient) throws CMSException {
        return ((KeyTransRecipient) recipient).getRecipientOperator(this.keyEncAlg, this.messageAlgorithm, this.info.getEncryptedKey().getOctets());
    }
}
