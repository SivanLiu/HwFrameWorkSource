package org.bouncycastle.tsp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.ess.ESSCertID;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Store;

public class TimeStampToken {
    CertID certID;
    Date genTime;
    CMSSignedData tsToken;
    SignerInformation tsaSignerInfo;
    TimeStampTokenInfo tstInfo;

    private class CertID {
        private ESSCertID certID;
        private ESSCertIDv2 certIDv2;

        CertID(ESSCertID eSSCertID) {
            this.certID = eSSCertID;
            this.certIDv2 = null;
        }

        CertID(ESSCertIDv2 eSSCertIDv2) {
            this.certIDv2 = eSSCertIDv2;
            this.certID = null;
        }

        public byte[] getCertHash() {
            return this.certID != null ? this.certID.getCertHash() : this.certIDv2.getCertHash();
        }

        public AlgorithmIdentifier getHashAlgorithm() {
            return this.certID != null ? new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1) : this.certIDv2.getHashAlgorithm();
        }

        public String getHashAlgorithmName() {
            return this.certID != null ? McElieceCCA2KeyGenParameterSpec.SHA1 : NISTObjectIdentifiers.id_sha256.equals(this.certIDv2.getHashAlgorithm().getAlgorithm()) ? McElieceCCA2KeyGenParameterSpec.SHA256 : this.certIDv2.getHashAlgorithm().getAlgorithm().getId();
        }

        public IssuerSerial getIssuerSerial() {
            return this.certID != null ? this.certID.getIssuerSerial() : this.certIDv2.getIssuerSerial();
        }
    }

    public TimeStampToken(ContentInfo contentInfo) throws TSPException, IOException {
        this(getSignedData(contentInfo));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:14:0x00b6 in {8, 10, 13, 16, 19, 21, 23} preds:[]
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
    public TimeStampToken(org.bouncycastle.cms.CMSSignedData r4) throws org.bouncycastle.tsp.TSPException, java.io.IOException {
        /*
        r3 = this;
        r3.<init>();
        r3.tsToken = r4;
        r4 = r3.tsToken;
        r4 = r4.getSignedContentTypeOID();
        r0 = org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_ct_TSTInfo;
        r0 = r0.getId();
        r4 = r4.equals(r0);
        if (r4 == 0) goto L_0x00ee;
        r4 = r3.tsToken;
        r4 = r4.getSignerInfos();
        r4 = r4.getSigners();
        r0 = r4.size();
        r1 = 1;
        if (r0 != r1) goto L_0x00ce;
        r4 = r4.iterator();
        r4 = r4.next();
        r4 = (org.bouncycastle.cms.SignerInformation) r4;
        r3.tsaSignerInfo = r4;
        r4 = r3.tsToken;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getSignedContent();	 Catch:{ CMSException -> 0x00bf }
        r0 = new java.io.ByteArrayOutputStream;	 Catch:{ CMSException -> 0x00bf }
        r0.<init>();	 Catch:{ CMSException -> 0x00bf }
        r4.write(r0);	 Catch:{ CMSException -> 0x00bf }
        r4 = new org.bouncycastle.asn1.ASN1InputStream;	 Catch:{ CMSException -> 0x00bf }
        r1 = new java.io.ByteArrayInputStream;	 Catch:{ CMSException -> 0x00bf }
        r0 = r0.toByteArray();	 Catch:{ CMSException -> 0x00bf }
        r1.<init>(r0);	 Catch:{ CMSException -> 0x00bf }
        r4.<init>(r1);	 Catch:{ CMSException -> 0x00bf }
        r0 = new org.bouncycastle.tsp.TimeStampTokenInfo;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.readObject();	 Catch:{ CMSException -> 0x00bf }
        r4 = org.bouncycastle.asn1.tsp.TSTInfo.getInstance(r4);	 Catch:{ CMSException -> 0x00bf }
        r0.<init>(r4);	 Catch:{ CMSException -> 0x00bf }
        r3.tstInfo = r0;	 Catch:{ CMSException -> 0x00bf }
        r4 = r3.tsaSignerInfo;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getSignedAttributes();	 Catch:{ CMSException -> 0x00bf }
        r0 = org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_aa_signingCertificate;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.get(r0);	 Catch:{ CMSException -> 0x00bf }
        r0 = 0;	 Catch:{ CMSException -> 0x00bf }
        if (r4 == 0) goto L_0x008c;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getAttrValues();	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getObjectAt(r0);	 Catch:{ CMSException -> 0x00bf }
        r4 = org.bouncycastle.asn1.ess.SigningCertificate.getInstance(r4);	 Catch:{ CMSException -> 0x00bf }
        r1 = new org.bouncycastle.tsp.TimeStampToken$CertID;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getCerts();	 Catch:{ CMSException -> 0x00bf }
        r4 = r4[r0];	 Catch:{ CMSException -> 0x00bf }
        r4 = org.bouncycastle.asn1.ess.ESSCertID.getInstance(r4);	 Catch:{ CMSException -> 0x00bf }
        r1.<init>(r4);	 Catch:{ CMSException -> 0x00bf }
        r3.certID = r1;	 Catch:{ CMSException -> 0x00bf }
        return;	 Catch:{ CMSException -> 0x00bf }
        r4 = r3.tsaSignerInfo;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getSignedAttributes();	 Catch:{ CMSException -> 0x00bf }
        r1 = org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_aa_signingCertificateV2;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.get(r1);	 Catch:{ CMSException -> 0x00bf }
        if (r4 == 0) goto L_0x00b7;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getAttrValues();	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getObjectAt(r0);	 Catch:{ CMSException -> 0x00bf }
        r4 = org.bouncycastle.asn1.ess.SigningCertificateV2.getInstance(r4);	 Catch:{ CMSException -> 0x00bf }
        r1 = new org.bouncycastle.tsp.TimeStampToken$CertID;	 Catch:{ CMSException -> 0x00bf }
        r4 = r4.getCerts();	 Catch:{ CMSException -> 0x00bf }
        r4 = r4[r0];	 Catch:{ CMSException -> 0x00bf }
        r4 = org.bouncycastle.asn1.ess.ESSCertIDv2.getInstance(r4);	 Catch:{ CMSException -> 0x00bf }
        r1.<init>(r4);	 Catch:{ CMSException -> 0x00bf }
        goto L_0x0089;	 Catch:{ CMSException -> 0x00bf }
        return;	 Catch:{ CMSException -> 0x00bf }
        r4 = new org.bouncycastle.tsp.TSPValidationException;	 Catch:{ CMSException -> 0x00bf }
        r0 = "no signing certificate attribute found, time stamp invalid.";	 Catch:{ CMSException -> 0x00bf }
        r4.<init>(r0);	 Catch:{ CMSException -> 0x00bf }
        throw r4;	 Catch:{ CMSException -> 0x00bf }
        r4 = move-exception;
        r0 = new org.bouncycastle.tsp.TSPException;
        r1 = r4.getMessage();
        r4 = r4.getUnderlyingException();
        r0.<init>(r1, r4);
        throw r0;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Time-stamp token signed by ";
        r1.append(r2);
        r4 = r4.size();
        r1.append(r4);
        r4 = " signers, but it must contain just the TSA signature.";
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        r4 = new org.bouncycastle.tsp.TSPValidationException;
        r0 = "ContentInfo object not for a time stamp.";
        r4.<init>(r0);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.tsp.TimeStampToken.<init>(org.bouncycastle.cms.CMSSignedData):void");
    }

    private static CMSSignedData getSignedData(ContentInfo contentInfo) throws TSPException {
        try {
            return new CMSSignedData(contentInfo);
        } catch (CMSException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TSP parsing error: ");
            stringBuilder.append(e.getMessage());
            throw new TSPException(stringBuilder.toString(), e.getCause());
        }
    }

    public Store getAttributeCertificates() {
        return this.tsToken.getAttributeCertificates();
    }

    public Store getCRLs() {
        return this.tsToken.getCRLs();
    }

    public Store getCertificates() {
        return this.tsToken.getCertificates();
    }

    public byte[] getEncoded() throws IOException {
        return this.tsToken.getEncoded();
    }

    public SignerId getSID() {
        return this.tsaSignerInfo.getSID();
    }

    public AttributeTable getSignedAttributes() {
        return this.tsaSignerInfo.getSignedAttributes();
    }

    public TimeStampTokenInfo getTimeStampInfo() {
        return this.tstInfo;
    }

    public AttributeTable getUnsignedAttributes() {
        return this.tsaSignerInfo.getUnsignedAttributes();
    }

    public boolean isSignatureValid(SignerInformationVerifier signerInformationVerifier) throws TSPException {
        try {
            return this.tsaSignerInfo.verify(signerInformationVerifier);
        } catch (CMSException e) {
            if (e.getUnderlyingException() != null) {
                throw new TSPException(e.getMessage(), e.getUnderlyingException());
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CMS exception: ");
            stringBuilder.append(e);
            throw new TSPException(stringBuilder.toString(), e);
        }
    }

    public CMSSignedData toCMSSignedData() {
        return this.tsToken;
    }

    public void validate(SignerInformationVerifier signerInformationVerifier) throws TSPException, TSPValidationException {
        StringBuilder stringBuilder;
        if (signerInformationVerifier.hasAssociatedCertificate()) {
            try {
                X509CertificateHolder associatedCertificate = signerInformationVerifier.getAssociatedCertificate();
                DigestCalculator digestCalculator = signerInformationVerifier.getDigestCalculator(this.certID.getHashAlgorithm());
                OutputStream outputStream = digestCalculator.getOutputStream();
                outputStream.write(associatedCertificate.getEncoded());
                outputStream.close();
                if (Arrays.constantTimeAreEqual(this.certID.getCertHash(), digestCalculator.getDigest())) {
                    if (this.certID.getIssuerSerial() != null) {
                        IssuerAndSerialNumber issuerAndSerialNumber = new IssuerAndSerialNumber(associatedCertificate.toASN1Structure());
                        if (this.certID.getIssuerSerial().getSerial().equals(issuerAndSerialNumber.getSerialNumber())) {
                            GeneralName[] names = this.certID.getIssuerSerial().getIssuer().getNames();
                            Object obj = null;
                            int i = 0;
                            while (i != names.length) {
                                if (names[i].getTagNo() == 4 && X500Name.getInstance(names[i].getName()).equals(X500Name.getInstance(issuerAndSerialNumber.getName()))) {
                                    obj = 1;
                                    break;
                                }
                                i++;
                            }
                            if (obj == null) {
                                throw new TSPValidationException("certificate name does not match certID for signature. ");
                            }
                        } else {
                            throw new TSPValidationException("certificate serial number does not match certID for signature.");
                        }
                    }
                    TSPUtil.validateCertificate(associatedCertificate);
                    if (!associatedCertificate.isValidOn(this.tstInfo.getGenTime())) {
                        throw new TSPValidationException("certificate not valid when time stamp created.");
                    } else if (!this.tsaSignerInfo.verify(signerInformationVerifier)) {
                        throw new TSPValidationException("signature not created by certificate.");
                    } else {
                        return;
                    }
                }
                throw new TSPValidationException("certificate hash does not match certID hash.");
            } catch (CMSException e) {
                if (e.getUnderlyingException() != null) {
                    throw new TSPException(e.getMessage(), e.getUnderlyingException());
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("CMS exception: ");
                stringBuilder.append(e);
                throw new TSPException(stringBuilder.toString(), e);
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("problem processing certificate: ");
                stringBuilder.append(e2);
                throw new TSPException(stringBuilder.toString(), e2);
            } catch (OperatorCreationException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to create digest: ");
                stringBuilder.append(e3.getMessage());
                throw new TSPException(stringBuilder.toString(), e3);
            }
        }
        throw new IllegalArgumentException("verifier provider needs an associated certificate");
    }
}
