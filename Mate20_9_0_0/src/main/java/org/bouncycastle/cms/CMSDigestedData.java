package org.bouncycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.DigestedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Encodable;

public class CMSDigestedData implements Encodable {
    private ContentInfo contentInfo;
    private DigestedData digestedData;

    public CMSDigestedData(InputStream inputStream) throws CMSException {
        this(CMSUtils.readContentInfo(inputStream));
    }

    public CMSDigestedData(ContentInfo contentInfo) throws CMSException {
        this.contentInfo = contentInfo;
        try {
            this.digestedData = DigestedData.getInstance(contentInfo.getContent());
        } catch (ClassCastException e) {
            throw new CMSException("Malformed content.", e);
        } catch (IllegalArgumentException e2) {
            throw new CMSException("Malformed content.", e2);
        }
    }

    public CMSDigestedData(byte[] bArr) throws CMSException {
        this(CMSUtils.readContentInfo(bArr));
    }

    public ASN1ObjectIdentifier getContentType() {
        return this.contentInfo.getContentType();
    }

    public AlgorithmIdentifier getDigestAlgorithm() {
        return this.digestedData.getDigestAlgorithm();
    }

    public CMSProcessable getDigestedContent() throws CMSException {
        ContentInfo encapContentInfo = this.digestedData.getEncapContentInfo();
        try {
            return new CMSProcessableByteArray(encapContentInfo.getContentType(), ((ASN1OctetString) encapContentInfo.getContent()).getOctets());
        } catch (Exception e) {
            throw new CMSException("exception reading digested stream.", e);
        }
    }

    public byte[] getEncoded() throws IOException {
        return this.contentInfo.getEncoded();
    }

    public ContentInfo toASN1Structure() {
        return this.contentInfo;
    }

    public boolean verify(DigestCalculatorProvider digestCalculatorProvider) throws CMSException {
        StringBuilder stringBuilder;
        try {
            ContentInfo encapContentInfo = this.digestedData.getEncapContentInfo();
            DigestCalculator digestCalculator = digestCalculatorProvider.get(this.digestedData.getDigestAlgorithm());
            digestCalculator.getOutputStream().write(((ASN1OctetString) encapContentInfo.getContent()).getOctets());
            return Arrays.areEqual(this.digestedData.getDigest(), digestCalculator.getDigest());
        } catch (OperatorCreationException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unable to create digest calculator: ");
            stringBuilder.append(e.getMessage());
            throw new CMSException(stringBuilder.toString(), e);
        } catch (IOException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unable process content: ");
            stringBuilder.append(e2.getMessage());
            throw new CMSException(stringBuilder.toString(), e2);
        }
    }
}
