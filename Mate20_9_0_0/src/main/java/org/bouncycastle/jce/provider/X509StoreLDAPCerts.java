package org.bouncycastle.jce.provider;

import java.util.Collection;
import java.util.HashSet;
import org.bouncycastle.jce.X509LDAPCertStoreParameters;
import org.bouncycastle.util.StoreException;
import org.bouncycastle.x509.X509CertPairStoreSelector;
import org.bouncycastle.x509.X509CertStoreSelector;
import org.bouncycastle.x509.X509CertificatePair;
import org.bouncycastle.x509.X509StoreParameters;
import org.bouncycastle.x509.X509StoreSpi;
import org.bouncycastle.x509.util.LDAPStoreHelper;

public class X509StoreLDAPCerts extends X509StoreSpi {
    private LDAPStoreHelper helper;

    private Collection getCertificatesFromCrossCertificatePairs(X509CertStoreSelector x509CertStoreSelector) throws StoreException {
        HashSet hashSet = new HashSet();
        X509CertPairStoreSelector x509CertPairStoreSelector = new X509CertPairStoreSelector();
        x509CertPairStoreSelector.setForwardSelector(x509CertStoreSelector);
        x509CertPairStoreSelector.setReverseSelector(new X509CertStoreSelector());
        HashSet<X509CertificatePair> hashSet2 = new HashSet(this.helper.getCrossCertificatePairs(x509CertPairStoreSelector));
        HashSet hashSet3 = new HashSet();
        HashSet hashSet4 = new HashSet();
        for (X509CertificatePair x509CertificatePair : hashSet2) {
            if (x509CertificatePair.getForward() != null) {
                hashSet3.add(x509CertificatePair.getForward());
            }
            if (x509CertificatePair.getReverse() != null) {
                hashSet4.add(x509CertificatePair.getReverse());
            }
        }
        hashSet.addAll(hashSet3);
        hashSet.addAll(hashSet4);
        return hashSet;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:13:0x003d in {3, 6, 8, 11, 12} preds:[]
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
    public java.util.Collection engineGetMatches(org.bouncycastle.util.Selector r4) throws org.bouncycastle.util.StoreException {
        /*
        r3 = this;
        r0 = r4 instanceof org.bouncycastle.x509.X509CertStoreSelector;
        if (r0 != 0) goto L_0x0007;
        r4 = java.util.Collections.EMPTY_SET;
        return r4;
        r4 = (org.bouncycastle.x509.X509CertStoreSelector) r4;
        r0 = new java.util.HashSet;
        r0.<init>();
        r1 = r4.getBasicConstraints();
        if (r1 <= 0) goto L_0x0025;
        r1 = r3.helper;
        r1 = r1.getCACertificates(r4);
        r0.addAll(r1);
        r4 = r3.getCertificatesFromCrossCertificatePairs(r4);
        r0.addAll(r4);
        return r0;
        r1 = r4.getBasicConstraints();
        r2 = -2;
        if (r1 != r2) goto L_0x0033;
        r1 = r3.helper;
        r4 = r1.getUserCertificates(r4);
        goto L_0x0021;
        r1 = r3.helper;
        r1 = r1.getUserCertificates(r4);
        r0.addAll(r1);
        goto L_0x0014;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.X509StoreLDAPCerts.engineGetMatches(org.bouncycastle.util.Selector):java.util.Collection");
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
