package org.bouncycastle.jce.provider;

import org.bouncycastle.jce.X509LDAPCertStoreParameters;
import org.bouncycastle.x509.X509StoreParameters;
import org.bouncycastle.x509.X509StoreSpi;
import org.bouncycastle.x509.util.LDAPStoreHelper;

public class X509StoreLDAPCRLs extends X509StoreSpi {
    private LDAPStoreHelper helper;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:10:0x0049 in {3, 6, 8, 9} preds:[]
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
    public java.util.Collection engineGetMatches(org.bouncycastle.util.Selector r3) throws org.bouncycastle.util.StoreException {
        /*
        r2 = this;
        r0 = r3 instanceof org.bouncycastle.x509.X509CRLStoreSelector;
        if (r0 != 0) goto L_0x0007;
    L_0x0004:
        r3 = java.util.Collections.EMPTY_SET;
        return r3;
    L_0x0007:
        r3 = (org.bouncycastle.x509.X509CRLStoreSelector) r3;
        r0 = new java.util.HashSet;
        r0.<init>();
        r1 = r3.isDeltaCRLIndicatorEnabled();
        if (r1 == 0) goto L_0x001e;
    L_0x0014:
        r1 = r2.helper;
        r3 = r1.getDeltaCertificateRevocationLists(r3);
    L_0x001a:
        r0.addAll(r3);
        return r0;
    L_0x001e:
        r1 = r2.helper;
        r1 = r1.getDeltaCertificateRevocationLists(r3);
        r0.addAll(r1);
        r1 = r2.helper;
        r1 = r1.getAttributeAuthorityRevocationLists(r3);
        r0.addAll(r1);
        r1 = r2.helper;
        r1 = r1.getAttributeCertificateRevocationLists(r3);
        r0.addAll(r1);
        r1 = r2.helper;
        r1 = r1.getAuthorityRevocationLists(r3);
        r0.addAll(r1);
        r1 = r2.helper;
        r3 = r1.getCertificateRevocationLists(r3);
        goto L_0x001a;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.X509StoreLDAPCRLs.engineGetMatches(org.bouncycastle.util.Selector):java.util.Collection");
    }

    public void engineInit(X509StoreParameters x509StoreParameters) {
        if (x509StoreParameters instanceof X509LDAPCertStoreParameters) {
            this.helper = new LDAPStoreHelper((X509LDAPCertStoreParameters) x509StoreParameters);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Initialization parameters must be an instance of ");
        stringBuilder.append(X509LDAPCertStoreParameters.class.getName());
        stringBuilder.append(".");
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
