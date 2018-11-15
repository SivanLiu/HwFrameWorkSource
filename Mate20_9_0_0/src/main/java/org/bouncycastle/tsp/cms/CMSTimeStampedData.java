package org.bouncycastle.tsp.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.Evidence;
import org.bouncycastle.asn1.cms.TimeStampAndCRL;
import org.bouncycastle.asn1.cms.TimeStampTokenEvidence;
import org.bouncycastle.asn1.cms.TimeStampedData;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TimeStampToken;

public class CMSTimeStampedData {
    private ContentInfo contentInfo;
    private TimeStampedData timeStampedData;
    private TimeStampDataUtil util;

    public CMSTimeStampedData(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder;
        try {
            initialize(ContentInfo.getInstance(new ASN1InputStream(inputStream).readObject()));
        } catch (ClassCastException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Malformed content: ");
            stringBuilder.append(e);
            throw new IOException(stringBuilder.toString());
        } catch (IllegalArgumentException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Malformed content: ");
            stringBuilder.append(e2);
            throw new IOException(stringBuilder.toString());
        }
    }

    public CMSTimeStampedData(ContentInfo contentInfo) {
        initialize(contentInfo);
    }

    public CMSTimeStampedData(byte[] bArr) throws IOException {
        this(new ByteArrayInputStream(bArr));
    }

    private void initialize(ContentInfo contentInfo) {
        this.contentInfo = contentInfo;
        if (CMSObjectIdentifiers.timestampedData.equals(contentInfo.getContentType())) {
            this.timeStampedData = TimeStampedData.getInstance(contentInfo.getContent());
            this.util = new TimeStampDataUtil(this.timeStampedData);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Malformed content - type must be ");
        stringBuilder.append(CMSObjectIdentifiers.timestampedData.getId());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public CMSTimeStampedData addTimeStamp(TimeStampToken timeStampToken) throws CMSException {
        Object timeStamps = this.util.getTimeStamps();
        TimeStampAndCRL[] timeStampAndCRLArr = new TimeStampAndCRL[(timeStamps.length + 1)];
        System.arraycopy(timeStamps, 0, timeStampAndCRLArr, 0, timeStamps.length);
        timeStampAndCRLArr[timeStamps.length] = new TimeStampAndCRL(timeStampToken.toCMSSignedData().toASN1Structure());
        return new CMSTimeStampedData(new ContentInfo(CMSObjectIdentifiers.timestampedData, new TimeStampedData(this.timeStampedData.getDataUri(), this.timeStampedData.getMetaData(), this.timeStampedData.getContent(), new Evidence(new TimeStampTokenEvidence(timeStampAndCRLArr)))));
    }

    public byte[] calculateNextHash(DigestCalculator digestCalculator) throws CMSException {
        return this.util.calculateNextHash(digestCalculator);
    }

    public byte[] getContent() {
        return this.timeStampedData.getContent() != null ? this.timeStampedData.getContent().getOctets() : null;
    }

    public URI getDataUri() throws URISyntaxException {
        DERIA5String dataUri = this.timeStampedData.getDataUri();
        return dataUri != null ? new URI(dataUri.getString()) : null;
    }

    public byte[] getEncoded() throws IOException {
        return this.contentInfo.getEncoded();
    }

    public String getFileName() {
        return this.util.getFileName();
    }

    public String getMediaType() {
        return this.util.getMediaType();
    }

    public DigestCalculator getMessageImprintDigestCalculator(DigestCalculatorProvider digestCalculatorProvider) throws OperatorCreationException {
        return this.util.getMessageImprintDigestCalculator(digestCalculatorProvider);
    }

    public AttributeTable getOtherMetaData() {
        return this.util.getOtherMetaData();
    }

    public TimeStampToken[] getTimeStampTokens() throws CMSException {
        return this.util.getTimeStampTokens();
    }

    public void initialiseMessageImprintDigestCalculator(DigestCalculator digestCalculator) throws CMSException {
        this.util.initialiseMessageImprintDigestCalculator(digestCalculator);
    }

    public void validate(DigestCalculatorProvider digestCalculatorProvider, byte[] bArr) throws ImprintDigestInvalidException, CMSException {
        this.util.validate(digestCalculatorProvider, bArr);
    }

    public void validate(DigestCalculatorProvider digestCalculatorProvider, byte[] bArr, TimeStampToken timeStampToken) throws ImprintDigestInvalidException, CMSException {
        this.util.validate(digestCalculatorProvider, bArr, timeStampToken);
    }
}
