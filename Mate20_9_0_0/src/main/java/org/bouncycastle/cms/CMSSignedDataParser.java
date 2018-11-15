package org.bouncycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Generator;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetStringParser;
import org.bouncycastle.asn1.ASN1SequenceParser;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1SetParser;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.BERSequenceGenerator;
import org.bouncycastle.asn1.BERSetParser;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfoParser;
import org.bouncycastle.asn1.cms.SignedDataParser;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.io.Streams;

public class CMSSignedDataParser extends CMSContentInfoParser {
    private static final CMSSignedHelper HELPER = CMSSignedHelper.INSTANCE;
    private ASN1Set _certSet;
    private ASN1Set _crlSet;
    private boolean _isCertCrlParsed;
    private CMSTypedStream _signedContent;
    private ASN1ObjectIdentifier _signedContentType;
    private SignedDataParser _signedData;
    private SignerInformationStore _signerInfoStore;
    private Set<AlgorithmIdentifier> digestAlgorithms;
    private Map digests;

    public CMSSignedDataParser(DigestCalculatorProvider digestCalculatorProvider, InputStream inputStream) throws CMSException {
        this(digestCalculatorProvider, null, inputStream);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:31:0x0096 in {10, 12, 18, 19, 23, 24, 26, 28, 30, 34} preds:[]
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
    public CMSSignedDataParser(org.bouncycastle.operator.DigestCalculatorProvider r5, org.bouncycastle.cms.CMSTypedStream r6, java.io.InputStream r7) throws org.bouncycastle.cms.CMSException {
        /*
        r4 = this;
        r4.<init>(r7);
        r4._signedContent = r6;	 Catch:{ IOException -> 0x0097 }
        r7 = r4._contentInfo;	 Catch:{ IOException -> 0x0097 }
        r0 = 16;	 Catch:{ IOException -> 0x0097 }
        r7 = r7.getContent(r0);	 Catch:{ IOException -> 0x0097 }
        r7 = org.bouncycastle.asn1.cms.SignedDataParser.getInstance(r7);	 Catch:{ IOException -> 0x0097 }
        r4._signedData = r7;	 Catch:{ IOException -> 0x0097 }
        r7 = new java.util.HashMap;	 Catch:{ IOException -> 0x0097 }
        r7.<init>();	 Catch:{ IOException -> 0x0097 }
        r4.digests = r7;	 Catch:{ IOException -> 0x0097 }
        r7 = r4._signedData;	 Catch:{ IOException -> 0x0097 }
        r7 = r7.getDigestAlgorithms();	 Catch:{ IOException -> 0x0097 }
        r0 = new java.util.HashSet;	 Catch:{ IOException -> 0x0097 }
        r0.<init>();	 Catch:{ IOException -> 0x0097 }
    L_0x0025:
        r1 = r7.readObject();	 Catch:{ IOException -> 0x0097 }
        if (r1 == 0) goto L_0x0044;	 Catch:{ IOException -> 0x0097 }
    L_0x002b:
        r1 = org.bouncycastle.asn1.x509.AlgorithmIdentifier.getInstance(r1);	 Catch:{ IOException -> 0x0097 }
        r0.add(r1);	 Catch:{ IOException -> 0x0097 }
        r2 = r5.get(r1);	 Catch:{ OperatorCreationException -> 0x0042 }
        if (r2 == 0) goto L_0x0025;	 Catch:{ OperatorCreationException -> 0x0042 }
    L_0x0038:
        r3 = r4.digests;	 Catch:{ OperatorCreationException -> 0x0042 }
        r1 = r1.getAlgorithm();	 Catch:{ OperatorCreationException -> 0x0042 }
        r3.put(r1, r2);	 Catch:{ OperatorCreationException -> 0x0042 }
        goto L_0x0025;
    L_0x0042:
        r1 = move-exception;
        goto L_0x0025;
    L_0x0044:
        r5 = java.util.Collections.unmodifiableSet(r0);	 Catch:{ IOException -> 0x0097 }
        r4.digestAlgorithms = r5;	 Catch:{ IOException -> 0x0097 }
        r5 = r4._signedData;	 Catch:{ IOException -> 0x0097 }
        r5 = r5.getEncapContentInfo();	 Catch:{ IOException -> 0x0097 }
        r7 = 4;	 Catch:{ IOException -> 0x0097 }
        r7 = r5.getContent(r7);	 Catch:{ IOException -> 0x0097 }
        r0 = r7 instanceof org.bouncycastle.asn1.ASN1OctetStringParser;	 Catch:{ IOException -> 0x0097 }
        if (r0 == 0) goto L_0x0073;	 Catch:{ IOException -> 0x0097 }
    L_0x0059:
        r7 = (org.bouncycastle.asn1.ASN1OctetStringParser) r7;	 Catch:{ IOException -> 0x0097 }
        r0 = new org.bouncycastle.cms.CMSTypedStream;	 Catch:{ IOException -> 0x0097 }
        r1 = r5.getContentType();	 Catch:{ IOException -> 0x0097 }
        r7 = r7.getOctetStream();	 Catch:{ IOException -> 0x0097 }
        r0.<init>(r1, r7);	 Catch:{ IOException -> 0x0097 }
        r7 = r4._signedContent;	 Catch:{ IOException -> 0x0097 }
        if (r7 != 0) goto L_0x006f;	 Catch:{ IOException -> 0x0097 }
    L_0x006c:
        r4._signedContent = r0;	 Catch:{ IOException -> 0x0097 }
        goto L_0x0086;	 Catch:{ IOException -> 0x0097 }
    L_0x006f:
        r0.drain();	 Catch:{ IOException -> 0x0097 }
        goto L_0x0086;	 Catch:{ IOException -> 0x0097 }
    L_0x0073:
        if (r7 == 0) goto L_0x0086;	 Catch:{ IOException -> 0x0097 }
    L_0x0075:
        r0 = new org.bouncycastle.cms.PKCS7TypedStream;	 Catch:{ IOException -> 0x0097 }
        r1 = r5.getContentType();	 Catch:{ IOException -> 0x0097 }
        r0.<init>(r1, r7);	 Catch:{ IOException -> 0x0097 }
        r7 = r4._signedContent;	 Catch:{ IOException -> 0x0097 }
        if (r7 != 0) goto L_0x0083;	 Catch:{ IOException -> 0x0097 }
    L_0x0082:
        goto L_0x006c;	 Catch:{ IOException -> 0x0097 }
    L_0x0083:
        r0.drain();	 Catch:{ IOException -> 0x0097 }
    L_0x0086:
        if (r6 != 0) goto L_0x008f;	 Catch:{ IOException -> 0x0097 }
    L_0x0088:
        r5 = r5.getContentType();	 Catch:{ IOException -> 0x0097 }
    L_0x008c:
        r4._signedContentType = r5;	 Catch:{ IOException -> 0x0097 }
        return;	 Catch:{ IOException -> 0x0097 }
    L_0x008f:
        r5 = r4._signedContent;	 Catch:{ IOException -> 0x0097 }
        r5 = r5.getContentType();	 Catch:{ IOException -> 0x0097 }
        goto L_0x008c;
        return;
    L_0x0097:
        r5 = move-exception;
        r6 = new org.bouncycastle.cms.CMSException;
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r0 = "io exception: ";
        r7.append(r0);
        r0 = r5.getMessage();
        r7.append(r0);
        r7 = r7.toString();
        r6.<init>(r7, r5);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.cms.CMSSignedDataParser.<init>(org.bouncycastle.operator.DigestCalculatorProvider, org.bouncycastle.cms.CMSTypedStream, java.io.InputStream):void");
    }

    public CMSSignedDataParser(DigestCalculatorProvider digestCalculatorProvider, CMSTypedStream cMSTypedStream, byte[] bArr) throws CMSException {
        this(digestCalculatorProvider, cMSTypedStream, new ByteArrayInputStream(bArr));
    }

    public CMSSignedDataParser(DigestCalculatorProvider digestCalculatorProvider, byte[] bArr) throws CMSException {
        this(digestCalculatorProvider, new ByteArrayInputStream(bArr));
    }

    private static ASN1Set getASN1Set(ASN1SetParser aSN1SetParser) {
        return aSN1SetParser == null ? null : ASN1Set.getInstance(aSN1SetParser.toASN1Primitive());
    }

    private static void pipeEncapsulatedOctetString(ContentInfoParser contentInfoParser, OutputStream outputStream) throws IOException {
        ASN1OctetStringParser aSN1OctetStringParser = (ASN1OctetStringParser) contentInfoParser.getContent(4);
        if (aSN1OctetStringParser != null) {
            pipeOctetString(aSN1OctetStringParser, outputStream);
        }
    }

    private static void pipeOctetString(ASN1OctetStringParser aSN1OctetStringParser, OutputStream outputStream) throws IOException {
        outputStream = CMSUtils.createBEROctetOutputStream(outputStream, 0, true, 0);
        Streams.pipeAll(aSN1OctetStringParser.getOctetStream(), outputStream);
        outputStream.close();
    }

    private void populateCertCrlSets() throws CMSException {
        if (!this._isCertCrlParsed) {
            this._isCertCrlParsed = true;
            try {
                this._certSet = getASN1Set(this._signedData.getCertificates());
                this._crlSet = getASN1Set(this._signedData.getCrls());
            } catch (Exception e) {
                throw new CMSException("problem parsing cert/crl sets", e);
            }
        }
    }

    public static OutputStream replaceCertificatesAndCRLs(InputStream inputStream, Store store, Store store2, Store store3, OutputStream outputStream) throws CMSException, IOException {
        ASN1Encodable createBerSetFromList;
        SignedDataParser instance = SignedDataParser.getInstance(new ContentInfoParser((ASN1SequenceParser) new ASN1StreamParser(inputStream).readObject()).getContent(16));
        BERSequenceGenerator bERSequenceGenerator = new BERSequenceGenerator(outputStream);
        bERSequenceGenerator.addObject(CMSObjectIdentifiers.signedData);
        BERSequenceGenerator bERSequenceGenerator2 = new BERSequenceGenerator(bERSequenceGenerator.getRawOutputStream(), 0, true);
        bERSequenceGenerator2.addObject(instance.getVersion());
        bERSequenceGenerator2.getRawOutputStream().write(instance.getDigestAlgorithms().toASN1Primitive().getEncoded());
        ContentInfoParser encapContentInfo = instance.getEncapContentInfo();
        BERSequenceGenerator bERSequenceGenerator3 = new BERSequenceGenerator(bERSequenceGenerator2.getRawOutputStream());
        bERSequenceGenerator3.addObject(encapContentInfo.getContentType());
        pipeEncapsulatedOctetString(encapContentInfo, bERSequenceGenerator3.getRawOutputStream());
        bERSequenceGenerator3.close();
        getASN1Set(instance.getCertificates());
        getASN1Set(instance.getCrls());
        if (!(store == null && store3 == null)) {
            List arrayList = new ArrayList();
            if (store != null) {
                arrayList.addAll(CMSUtils.getCertificatesFromStore(store));
            }
            if (store3 != null) {
                arrayList.addAll(CMSUtils.getAttributeCertificatesFromStore(store3));
            }
            createBerSetFromList = CMSUtils.createBerSetFromList(arrayList);
            if (createBerSetFromList.size() > 0) {
                bERSequenceGenerator2.getRawOutputStream().write(new DERTaggedObject(false, 0, createBerSetFromList).getEncoded());
            }
        }
        if (store2 != null) {
            createBerSetFromList = CMSUtils.createBerSetFromList(CMSUtils.getCRLsFromStore(store2));
            if (createBerSetFromList.size() > 0) {
                bERSequenceGenerator2.getRawOutputStream().write(new DERTaggedObject(false, 1, createBerSetFromList).getEncoded());
            }
        }
        bERSequenceGenerator2.getRawOutputStream().write(instance.getSignerInfos().toASN1Primitive().getEncoded());
        bERSequenceGenerator2.close();
        bERSequenceGenerator.close();
        return outputStream;
    }

    public static OutputStream replaceSigners(InputStream inputStream, SignerInformationStore signerInformationStore, OutputStream outputStream) throws CMSException, IOException {
        SignedDataParser instance = SignedDataParser.getInstance(new ContentInfoParser((ASN1SequenceParser) new ASN1StreamParser(inputStream).readObject()).getContent(16));
        BERSequenceGenerator bERSequenceGenerator = new BERSequenceGenerator(outputStream);
        bERSequenceGenerator.addObject(CMSObjectIdentifiers.signedData);
        ASN1Generator bERSequenceGenerator2 = new BERSequenceGenerator(bERSequenceGenerator.getRawOutputStream(), 0, true);
        bERSequenceGenerator2.addObject(instance.getVersion());
        instance.getDigestAlgorithms().toASN1Primitive();
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (SignerInformation digestAlgorithmID : signerInformationStore.getSigners()) {
            aSN1EncodableVector.add(CMSSignedHelper.INSTANCE.fixAlgID(digestAlgorithmID.getDigestAlgorithmID()));
        }
        bERSequenceGenerator2.getRawOutputStream().write(new DERSet(aSN1EncodableVector).getEncoded());
        ContentInfoParser encapContentInfo = instance.getEncapContentInfo();
        BERSequenceGenerator bERSequenceGenerator3 = new BERSequenceGenerator(bERSequenceGenerator2.getRawOutputStream());
        bERSequenceGenerator3.addObject(encapContentInfo.getContentType());
        pipeEncapsulatedOctetString(encapContentInfo, bERSequenceGenerator3.getRawOutputStream());
        bERSequenceGenerator3.close();
        writeSetToGeneratorTagged(bERSequenceGenerator2, instance.getCertificates(), 0);
        writeSetToGeneratorTagged(bERSequenceGenerator2, instance.getCrls(), 1);
        ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
        for (SignerInformation toASN1Structure : signerInformationStore.getSigners()) {
            aSN1EncodableVector2.add(toASN1Structure.toASN1Structure());
        }
        bERSequenceGenerator2.getRawOutputStream().write(new DERSet(aSN1EncodableVector2).getEncoded());
        bERSequenceGenerator2.close();
        bERSequenceGenerator.close();
        return outputStream;
    }

    private static void writeSetToGeneratorTagged(ASN1Generator aSN1Generator, ASN1SetParser aSN1SetParser, int i) throws IOException {
        ASN1Encodable aSN1Set = getASN1Set(aSN1SetParser);
        if (aSN1Set != null) {
            OutputStream rawOutputStream;
            byte[] encoded;
            if (aSN1SetParser instanceof BERSetParser) {
                rawOutputStream = aSN1Generator.getRawOutputStream();
                encoded = new BERTaggedObject(false, i, aSN1Set).getEncoded();
            } else {
                rawOutputStream = aSN1Generator.getRawOutputStream();
                encoded = new DERTaggedObject(false, i, aSN1Set).getEncoded();
            }
            rawOutputStream.write(encoded);
        }
    }

    public Store getAttributeCertificates() throws CMSException {
        populateCertCrlSets();
        return HELPER.getAttributeCertificates(this._certSet);
    }

    public Store getCRLs() throws CMSException {
        populateCertCrlSets();
        return HELPER.getCRLs(this._crlSet);
    }

    public Store getCertificates() throws CMSException {
        populateCertCrlSets();
        return HELPER.getCertificates(this._certSet);
    }

    public Set<AlgorithmIdentifier> getDigestAlgorithmIDs() {
        return this.digestAlgorithms;
    }

    public Store getOtherRevocationInfo(ASN1ObjectIdentifier aSN1ObjectIdentifier) throws CMSException {
        populateCertCrlSets();
        return HELPER.getOtherRevocationInfo(aSN1ObjectIdentifier, this._crlSet);
    }

    public CMSTypedStream getSignedContent() {
        if (this._signedContent == null) {
            return null;
        }
        return new CMSTypedStream(this._signedContent.getContentType(), CMSUtils.attachDigestsToInputStream(this.digests.values(), this._signedContent.getContentStream()));
    }

    public String getSignedContentTypeOID() {
        return this._signedContentType.getId();
    }

    public SignerInformationStore getSignerInfos() throws CMSException {
        if (this._signerInfoStore == null) {
            populateCertCrlSets();
            Collection arrayList = new ArrayList();
            Map hashMap = new HashMap();
            for (Object next : this.digests.keySet()) {
                hashMap.put(next, ((DigestCalculator) this.digests.get(next)).getDigest());
            }
            try {
                ASN1SetParser signerInfos = this._signedData.getSignerInfos();
                while (true) {
                    ASN1Encodable readObject = signerInfos.readObject();
                    if (readObject == null) {
                        break;
                    }
                    SignerInfo instance = SignerInfo.getInstance(readObject.toASN1Primitive());
                    arrayList.add(new SignerInformation(instance, this._signedContentType, null, (byte[]) hashMap.get(instance.getDigestAlgorithm().getAlgorithm())));
                }
                this._signerInfoStore = new SignerInformationStore(arrayList);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("io exception: ");
                stringBuilder.append(e.getMessage());
                throw new CMSException(stringBuilder.toString(), e);
            }
        }
        return this._signerInfoStore;
    }

    public int getVersion() {
        return this._signedData.getVersion().getValue().intValue();
    }
}
