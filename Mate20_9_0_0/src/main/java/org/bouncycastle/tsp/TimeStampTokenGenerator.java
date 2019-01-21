package org.bouncycastle.tsp;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertID;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificate;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.tsp.Accuracy;
import org.bouncycastle.asn1.tsp.MessageImprint;
import org.bouncycastle.asn1.tsp.TSTInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cms.CMSAttributeTableGenerationException;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.util.CollectionStore;
import org.bouncycastle.util.Store;

public class TimeStampTokenGenerator {
    public static final int R_MICROSECONDS = 2;
    public static final int R_MILLISECONDS = 3;
    public static final int R_SECONDS = 0;
    public static final int R_TENTHS_OF_SECONDS = 1;
    private int accuracyMicros;
    private int accuracyMillis;
    private int accuracySeconds;
    private List attrCerts;
    private List certs;
    private List crls;
    private Locale locale;
    boolean ordering;
    private Map otherRevoc;
    private int resolution;
    private SignerInfoGenerator signerInfoGen;
    GeneralName tsa;
    private ASN1ObjectIdentifier tsaPolicyOID;

    /* renamed from: org.bouncycastle.tsp.TimeStampTokenGenerator$1 */
    class AnonymousClass1 implements CMSAttributeTableGenerator {
        final /* synthetic */ ESSCertID val$essCertid;
        final /* synthetic */ SignerInfoGenerator val$signerInfoGen;

        AnonymousClass1(SignerInfoGenerator signerInfoGenerator, ESSCertID eSSCertID) {
            this.val$signerInfoGen = signerInfoGenerator;
            this.val$essCertid = eSSCertID;
        }

        public AttributeTable getAttributes(Map map) throws CMSAttributeTableGenerationException {
            AttributeTable attributes = this.val$signerInfoGen.getSignedAttributeTableGenerator().getAttributes(map);
            return attributes.get(PKCSObjectIdentifiers.id_aa_signingCertificate) == null ? attributes.add(PKCSObjectIdentifiers.id_aa_signingCertificate, new SigningCertificate(this.val$essCertid)) : attributes;
        }
    }

    /* renamed from: org.bouncycastle.tsp.TimeStampTokenGenerator$2 */
    class AnonymousClass2 implements CMSAttributeTableGenerator {
        final /* synthetic */ ESSCertIDv2 val$essCertid;
        final /* synthetic */ SignerInfoGenerator val$signerInfoGen;

        AnonymousClass2(SignerInfoGenerator signerInfoGenerator, ESSCertIDv2 eSSCertIDv2) {
            this.val$signerInfoGen = signerInfoGenerator;
            this.val$essCertid = eSSCertIDv2;
        }

        public AttributeTable getAttributes(Map map) throws CMSAttributeTableGenerationException {
            AttributeTable attributes = this.val$signerInfoGen.getSignedAttributeTableGenerator().getAttributes(map);
            return attributes.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2) == null ? attributes.add(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new SigningCertificateV2(this.val$essCertid)) : attributes;
        }
    }

    public TimeStampTokenGenerator(SignerInfoGenerator signerInfoGenerator, DigestCalculator digestCalculator, ASN1ObjectIdentifier aSN1ObjectIdentifier) throws IllegalArgumentException, TSPException {
        this(signerInfoGenerator, digestCalculator, aSN1ObjectIdentifier, false);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:17:0x00d5 in {8, 9, 11, 14, 16, 20, 22} preds:[]
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
    public TimeStampTokenGenerator(org.bouncycastle.cms.SignerInfoGenerator r6, org.bouncycastle.operator.DigestCalculator r7, org.bouncycastle.asn1.ASN1ObjectIdentifier r8, boolean r9) throws java.lang.IllegalArgumentException, org.bouncycastle.tsp.TSPException {
        /*
        r5 = this;
        r5.<init>();
        r0 = 0;
        r5.resolution = r0;
        r1 = 0;
        r5.locale = r1;
        r2 = -1;
        r5.accuracySeconds = r2;
        r5.accuracyMillis = r2;
        r5.accuracyMicros = r2;
        r5.ordering = r0;
        r5.tsa = r1;
        r0 = new java.util.ArrayList;
        r0.<init>();
        r5.certs = r0;
        r0 = new java.util.ArrayList;
        r0.<init>();
        r5.crls = r0;
        r0 = new java.util.ArrayList;
        r0.<init>();
        r5.attrCerts = r0;
        r0 = new java.util.HashMap;
        r0.<init>();
        r5.otherRevoc = r0;
        r5.signerInfoGen = r6;
        r5.tsaPolicyOID = r8;
        r8 = r6.hasAssociatedCertificate();
        if (r8 == 0) goto L_0x00df;
        r8 = r6.getAssociatedCertificate();
        org.bouncycastle.tsp.TSPUtil.validateCertificate(r8);
        r0 = r7.getOutputStream();	 Catch:{ IOException -> 0x00d6 }
        r2 = r8.getEncoded();	 Catch:{ IOException -> 0x00d6 }
        r0.write(r2);	 Catch:{ IOException -> 0x00d6 }
        r0.close();	 Catch:{ IOException -> 0x00d6 }
        r0 = r7.getAlgorithmIdentifier();	 Catch:{ IOException -> 0x00d6 }
        r0 = r0.getAlgorithm();	 Catch:{ IOException -> 0x00d6 }
        r2 = org.bouncycastle.asn1.oiw.OIWObjectIdentifiers.idSHA1;	 Catch:{ IOException -> 0x00d6 }
        r0 = r0.equals(r2);	 Catch:{ IOException -> 0x00d6 }
        if (r0 == 0) goto L_0x0092;	 Catch:{ IOException -> 0x00d6 }
        r0 = new org.bouncycastle.asn1.ess.ESSCertID;	 Catch:{ IOException -> 0x00d6 }
        r7 = r7.getDigest();	 Catch:{ IOException -> 0x00d6 }
        if (r9 == 0) goto L_0x007e;	 Catch:{ IOException -> 0x00d6 }
        r1 = new org.bouncycastle.asn1.x509.IssuerSerial;	 Catch:{ IOException -> 0x00d6 }
        r9 = new org.bouncycastle.asn1.x509.GeneralNames;	 Catch:{ IOException -> 0x00d6 }
        r2 = new org.bouncycastle.asn1.x509.GeneralName;	 Catch:{ IOException -> 0x00d6 }
        r3 = r8.getIssuer();	 Catch:{ IOException -> 0x00d6 }
        r2.<init>(r3);	 Catch:{ IOException -> 0x00d6 }
        r9.<init>(r2);	 Catch:{ IOException -> 0x00d6 }
        r8 = r8.getSerialNumber();	 Catch:{ IOException -> 0x00d6 }
        r1.<init>(r9, r8);	 Catch:{ IOException -> 0x00d6 }
        r0.<init>(r7, r1);	 Catch:{ IOException -> 0x00d6 }
        r7 = new org.bouncycastle.cms.SignerInfoGenerator;	 Catch:{ IOException -> 0x00d6 }
        r8 = new org.bouncycastle.tsp.TimeStampTokenGenerator$1;	 Catch:{ IOException -> 0x00d6 }
        r8.<init>(r6, r0);	 Catch:{ IOException -> 0x00d6 }
        r9 = r6.getUnsignedAttributeTableGenerator();	 Catch:{ IOException -> 0x00d6 }
        r7.<init>(r6, r8, r9);	 Catch:{ IOException -> 0x00d6 }
        r5.signerInfoGen = r7;	 Catch:{ IOException -> 0x00d6 }
        return;	 Catch:{ IOException -> 0x00d6 }
        r0 = new org.bouncycastle.asn1.x509.AlgorithmIdentifier;	 Catch:{ IOException -> 0x00d6 }
        r2 = r7.getAlgorithmIdentifier();	 Catch:{ IOException -> 0x00d6 }
        r2 = r2.getAlgorithm();	 Catch:{ IOException -> 0x00d6 }
        r0.<init>(r2);	 Catch:{ IOException -> 0x00d6 }
        r2 = new org.bouncycastle.asn1.ess.ESSCertIDv2;	 Catch:{ IOException -> 0x00d6 }
        r7 = r7.getDigest();	 Catch:{ IOException -> 0x00d6 }
        if (r9 == 0) goto L_0x00c3;	 Catch:{ IOException -> 0x00d6 }
        r1 = new org.bouncycastle.asn1.x509.IssuerSerial;	 Catch:{ IOException -> 0x00d6 }
        r9 = new org.bouncycastle.asn1.x509.GeneralNames;	 Catch:{ IOException -> 0x00d6 }
        r3 = new org.bouncycastle.asn1.x509.GeneralName;	 Catch:{ IOException -> 0x00d6 }
        r4 = r8.getIssuer();	 Catch:{ IOException -> 0x00d6 }
        r3.<init>(r4);	 Catch:{ IOException -> 0x00d6 }
        r9.<init>(r3);	 Catch:{ IOException -> 0x00d6 }
        r3 = new org.bouncycastle.asn1.ASN1Integer;	 Catch:{ IOException -> 0x00d6 }
        r8 = r8.getSerialNumber();	 Catch:{ IOException -> 0x00d6 }
        r3.<init>(r8);	 Catch:{ IOException -> 0x00d6 }
        r1.<init>(r9, r3);	 Catch:{ IOException -> 0x00d6 }
        r2.<init>(r0, r7, r1);	 Catch:{ IOException -> 0x00d6 }
        r7 = new org.bouncycastle.cms.SignerInfoGenerator;	 Catch:{ IOException -> 0x00d6 }
        r8 = new org.bouncycastle.tsp.TimeStampTokenGenerator$2;	 Catch:{ IOException -> 0x00d6 }
        r8.<init>(r6, r2);	 Catch:{ IOException -> 0x00d6 }
        r9 = r6.getUnsignedAttributeTableGenerator();	 Catch:{ IOException -> 0x00d6 }
        r7.<init>(r6, r8, r9);	 Catch:{ IOException -> 0x00d6 }
        goto L_0x008f;
        return;
        r6 = move-exception;
        r7 = new org.bouncycastle.tsp.TSPException;
        r8 = "Exception processing certificate.";
        r7.<init>(r8, r6);
        throw r7;
        r6 = new java.lang.IllegalArgumentException;
        r7 = "SignerInfoGenerator must have an associated certificate";
        r6.<init>(r7);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.tsp.TimeStampTokenGenerator.<init>(org.bouncycastle.cms.SignerInfoGenerator, org.bouncycastle.operator.DigestCalculator, org.bouncycastle.asn1.ASN1ObjectIdentifier, boolean):void");
    }

    /* JADX WARNING: Missing block: B:13:0x0062, code skipped:
            if (r0.length() > r2) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:15:0x006b, code skipped:
            if (r0.length() > r2) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:16:0x006d, code skipped:
            r0.delete(r2, r0.length());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ASN1GeneralizedTime createGeneralizedTime(Date date) throws TSPException {
        String str = "yyyyMMddHHmmss.SSS";
        SimpleDateFormat simpleDateFormat = this.locale == null ? new SimpleDateFormat(str) : new SimpleDateFormat(str, this.locale);
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        StringBuilder stringBuilder = new StringBuilder(simpleDateFormat.format(date));
        int indexOf = stringBuilder.indexOf(".");
        if (indexOf < 0) {
            stringBuilder.append("Z");
            return new ASN1GeneralizedTime(stringBuilder.toString());
        }
        int i;
        switch (this.resolution) {
            case 1:
                i = indexOf + 2;
                break;
            case 2:
                i = indexOf + 3;
                break;
            case 3:
                break;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("unknown time-stamp resolution: ");
                stringBuilder.append(this.resolution);
                throw new TSPException(stringBuilder.toString());
        }
        while (stringBuilder.charAt(stringBuilder.length() - 1) == '0') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        if (stringBuilder.length() - 1 == indexOf) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        stringBuilder.append("Z");
        return new ASN1GeneralizedTime(stringBuilder.toString());
    }

    public void addAttributeCertificates(Store store) {
        this.attrCerts.addAll(store.getMatches(null));
    }

    public void addCRLs(Store store) {
        this.crls.addAll(store.getMatches(null));
    }

    public void addCertificates(Store store) {
        this.certs.addAll(store.getMatches(null));
    }

    public void addOtherRevocationInfo(ASN1ObjectIdentifier aSN1ObjectIdentifier, Store store) {
        this.otherRevoc.put(aSN1ObjectIdentifier, store.getMatches(null));
    }

    public TimeStampToken generate(TimeStampRequest timeStampRequest, BigInteger bigInteger, Date date) throws TSPException {
        return generate(timeStampRequest, bigInteger, date, null);
    }

    public TimeStampToken generate(TimeStampRequest timeStampRequest, BigInteger bigInteger, Date date, Extensions extensions) throws TSPException {
        Accuracy accuracy;
        Extensions generate;
        ASN1GeneralizedTime aSN1GeneralizedTime;
        Date date2 = date;
        Extensions extensions2 = extensions;
        MessageImprint messageImprint = new MessageImprint(new AlgorithmIdentifier(timeStampRequest.getMessageImprintAlgOID(), DERNull.INSTANCE), timeStampRequest.getMessageImprintDigest());
        if (this.accuracySeconds > 0 || this.accuracyMillis > 0 || this.accuracyMicros > 0) {
            accuracy = new Accuracy(this.accuracySeconds > 0 ? new ASN1Integer((long) this.accuracySeconds) : null, this.accuracyMillis > 0 ? new ASN1Integer((long) this.accuracyMillis) : null, this.accuracyMicros > 0 ? new ASN1Integer((long) this.accuracyMicros) : null);
        } else {
            accuracy = null;
        }
        ASN1Boolean instance = this.ordering ? ASN1Boolean.getInstance(this.ordering) : null;
        ASN1Integer aSN1Integer = timeStampRequest.getNonce() != null ? new ASN1Integer(timeStampRequest.getNonce()) : null;
        ASN1ObjectIdentifier aSN1ObjectIdentifier = this.tsaPolicyOID;
        if (timeStampRequest.getReqPolicy() != null) {
            aSN1ObjectIdentifier = timeStampRequest.getReqPolicy();
        }
        ASN1ObjectIdentifier aSN1ObjectIdentifier2 = aSN1ObjectIdentifier;
        Extensions extensions3 = timeStampRequest.getExtensions();
        if (extensions2 != null) {
            ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
            if (extensions3 != null) {
                Enumeration oids = extensions3.oids();
                while (oids.hasMoreElements()) {
                    extensionsGenerator.addExtension(extensions3.getExtension(ASN1ObjectIdentifier.getInstance(oids.nextElement())));
                }
            }
            Enumeration oids2 = extensions.oids();
            while (oids2.hasMoreElements()) {
                extensionsGenerator.addExtension(extensions2.getExtension(ASN1ObjectIdentifier.getInstance(oids2.nextElement())));
            }
            generate = extensionsGenerator.generate();
        } else {
            generate = extensions3;
        }
        if (this.resolution == 0) {
            aSN1GeneralizedTime = this.locale == null ? new ASN1GeneralizedTime(date2) : new ASN1GeneralizedTime(date2, this.locale);
        } else {
            aSN1GeneralizedTime = createGeneralizedTime(date2);
        }
        TSTInfo tSTInfo = new TSTInfo(aSN1ObjectIdentifier2, messageImprint, new ASN1Integer(bigInteger), aSN1GeneralizedTime, accuracy, instance, aSN1Integer, this.tsa, generate);
        try {
            CMSSignedDataGenerator cMSSignedDataGenerator = new CMSSignedDataGenerator();
            if (timeStampRequest.getCertReq()) {
                cMSSignedDataGenerator.addCertificates(new CollectionStore(this.certs));
                cMSSignedDataGenerator.addAttributeCertificates(new CollectionStore(this.attrCerts));
            }
            cMSSignedDataGenerator.addCRLs(new CollectionStore(this.crls));
            if (!this.otherRevoc.isEmpty()) {
                for (ASN1ObjectIdentifier aSN1ObjectIdentifier3 : this.otherRevoc.keySet()) {
                    cMSSignedDataGenerator.addOtherRevocationInfo(aSN1ObjectIdentifier3, (Store) new CollectionStore((Collection) this.otherRevoc.get(aSN1ObjectIdentifier3)));
                }
            }
            cMSSignedDataGenerator.addSignerInfoGenerator(this.signerInfoGen);
            return new TimeStampToken(cMSSignedDataGenerator.generate(new CMSProcessableByteArray(PKCSObjectIdentifiers.id_ct_TSTInfo, tSTInfo.getEncoded(ASN1Encoding.DER)), true));
        } catch (CMSException e) {
            throw new TSPException("Error generating time-stamp token", e);
        } catch (IOException e2) {
            throw new TSPException("Exception encoding info", e2);
        }
    }

    public void setAccuracyMicros(int i) {
        this.accuracyMicros = i;
    }

    public void setAccuracyMillis(int i) {
        this.accuracyMillis = i;
    }

    public void setAccuracySeconds(int i) {
        this.accuracySeconds = i;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setOrdering(boolean z) {
        this.ordering = z;
    }

    public void setResolution(int i) {
        this.resolution = i;
    }

    public void setTSA(GeneralName generalName) {
        this.tsa = generalName;
    }
}
