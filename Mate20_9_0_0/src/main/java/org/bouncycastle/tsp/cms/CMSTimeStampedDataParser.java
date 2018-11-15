package org.bouncycastle.tsp.cms;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfoParser;
import org.bouncycastle.asn1.cms.TimeStampedDataParser;
import org.bouncycastle.cms.CMSContentInfoParser;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.io.Streams;

public class CMSTimeStampedDataParser extends CMSContentInfoParser {
    private TimeStampedDataParser timeStampedData;
    private TimeStampDataUtil util;

    public CMSTimeStampedDataParser(InputStream inputStream) throws CMSException {
        super(inputStream);
        initialize(this._contentInfo);
    }

    public CMSTimeStampedDataParser(byte[] bArr) throws CMSException {
        this(new ByteArrayInputStream(bArr));
    }

    private void initialize(ContentInfoParser contentInfoParser) throws CMSException {
        try {
            if (CMSObjectIdentifiers.timestampedData.equals(contentInfoParser.getContentType())) {
                this.timeStampedData = TimeStampedDataParser.getInstance(contentInfoParser.getContent(16));
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Malformed content - type must be ");
            stringBuilder.append(CMSObjectIdentifiers.timestampedData.getId());
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("parsing exception: ");
            stringBuilder2.append(e.getMessage());
            throw new CMSException(stringBuilder2.toString(), e);
        }
    }

    private void parseTimeStamps() throws CMSException {
        try {
            if (this.util == null) {
                InputStream content = getContent();
                if (content != null) {
                    Streams.drain(content);
                }
                this.util = new TimeStampDataUtil(this.timeStampedData);
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to parse evidence block: ");
            stringBuilder.append(e.getMessage());
            throw new CMSException(stringBuilder.toString(), e);
        }
    }

    public byte[] calculateNextHash(DigestCalculator digestCalculator) throws CMSException {
        return this.util.calculateNextHash(digestCalculator);
    }

    public InputStream getContent() {
        return this.timeStampedData.getContent() != null ? this.timeStampedData.getContent().getOctetStream() : null;
    }

    public URI getDataUri() throws URISyntaxException {
        DERIA5String dataUri = this.timeStampedData.getDataUri();
        return dataUri != null ? new URI(dataUri.getString()) : null;
    }

    public String getFileName() {
        return this.util.getFileName();
    }

    public String getMediaType() {
        return this.util.getMediaType();
    }

    public DigestCalculator getMessageImprintDigestCalculator(DigestCalculatorProvider digestCalculatorProvider) throws OperatorCreationException {
        try {
            parseTimeStamps();
            return this.util.getMessageImprintDigestCalculator(digestCalculatorProvider);
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to extract algorithm ID: ");
            stringBuilder.append(e.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e);
        }
    }

    public AttributeTable getOtherMetaData() {
        return this.util.getOtherMetaData();
    }

    public TimeStampToken[] getTimeStampTokens() throws CMSException {
        parseTimeStamps();
        return this.util.getTimeStampTokens();
    }

    public void initialiseMessageImprintDigestCalculator(DigestCalculator digestCalculator) throws CMSException {
        this.util.initialiseMessageImprintDigestCalculator(digestCalculator);
    }

    public void validate(DigestCalculatorProvider digestCalculatorProvider, byte[] bArr) throws ImprintDigestInvalidException, CMSException {
        parseTimeStamps();
        this.util.validate(digestCalculatorProvider, bArr);
    }

    public void validate(DigestCalculatorProvider digestCalculatorProvider, byte[] bArr, TimeStampToken timeStampToken) throws ImprintDigestInvalidException, CMSException {
        parseTimeStamps();
        this.util.validate(digestCalculatorProvider, bArr, timeStampToken);
    }
}
