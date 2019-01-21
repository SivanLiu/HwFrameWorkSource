package org.bouncycastle.cms;

import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.cms.OriginatorInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.Store;

public class OriginatorInfoGenerator {
    private final List origCRLs;
    private final List origCerts;

    public OriginatorInfoGenerator(X509CertificateHolder x509CertificateHolder) {
        this.origCerts = new ArrayList(1);
        this.origCRLs = null;
        this.origCerts.add(x509CertificateHolder.toASN1Structure());
    }

    public OriginatorInfoGenerator(Store store) throws CMSException {
        this(store, null);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0014 in {2, 4, 5} preds:[]
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
    public OriginatorInfoGenerator(org.bouncycastle.util.Store r1, org.bouncycastle.util.Store r2) throws org.bouncycastle.cms.CMSException {
        /*
        r0 = this;
        r0.<init>();
        r1 = org.bouncycastle.cms.CMSUtils.getCertificatesFromStore(r1);
        r0.origCerts = r1;
        if (r2 == 0) goto L_0x0012;
        r1 = org.bouncycastle.cms.CMSUtils.getCRLsFromStore(r2);
        r0.origCRLs = r1;
        return;
        r1 = 0;
        goto L_0x000f;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.cms.OriginatorInfoGenerator.<init>(org.bouncycastle.util.Store, org.bouncycastle.util.Store):void");
    }

    public OriginatorInformation generate() {
        return this.origCRLs != null ? new OriginatorInformation(new OriginatorInfo(CMSUtils.createDerSetFromList(this.origCerts), CMSUtils.createDerSetFromList(this.origCRLs))) : new OriginatorInformation(new OriginatorInfo(CMSUtils.createDerSetFromList(this.origCerts), null));
    }
}
