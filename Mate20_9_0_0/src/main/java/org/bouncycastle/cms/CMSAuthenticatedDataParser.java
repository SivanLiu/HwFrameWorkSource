package org.bouncycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1OctetStringParser;
import org.bouncycastle.asn1.ASN1SequenceParser;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1SetParser;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.AuthenticatedDataParser;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.OriginatorInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.util.Arrays;

public class CMSAuthenticatedDataParser extends CMSContentInfoParser {
    private boolean authAttrNotRead;
    private ASN1Set authAttrSet;
    private AttributeTable authAttrs;
    AuthenticatedDataParser authData;
    private byte[] mac;
    private AlgorithmIdentifier macAlg;
    private OriginatorInformation originatorInfo;
    RecipientInformationStore recipientInfoStore;
    private boolean unauthAttrNotRead;
    private AttributeTable unauthAttrs;

    public CMSAuthenticatedDataParser(InputStream inputStream) throws CMSException, IOException {
        this(inputStream, null);
    }

    public CMSAuthenticatedDataParser(InputStream inputStream, DigestCalculatorProvider digestCalculatorProvider) throws CMSException, IOException {
        super(inputStream);
        this.authAttrNotRead = true;
        this.authData = new AuthenticatedDataParser((ASN1SequenceParser) this._contentInfo.getContent(16));
        OriginatorInfo originatorInfo = this.authData.getOriginatorInfo();
        if (originatorInfo != null) {
            this.originatorInfo = new OriginatorInformation(originatorInfo);
        }
        ASN1Set instance = ASN1Set.getInstance(this.authData.getRecipientInfos().toASN1Primitive());
        this.macAlg = this.authData.getMacAlgorithm();
        AlgorithmIdentifier digestAlgorithm = this.authData.getDigestAlgorithm();
        if (digestAlgorithm == null) {
            this.recipientInfoStore = CMSEnvelopedHelper.buildRecipientInformationStore(instance, this.macAlg, new CMSAuthenticatedSecureReadable(this.macAlg, new CMSProcessableInputStream(((ASN1OctetStringParser) this.authData.getEncapsulatedContentInfo().getContent(4)).getOctetStream())));
        } else if (digestCalculatorProvider != null) {
            try {
                this.recipientInfoStore = CMSEnvelopedHelper.buildRecipientInformationStore(instance, this.macAlg, new CMSDigestAuthenticatedSecureReadable(digestCalculatorProvider.get(digestAlgorithm), new CMSProcessableInputStream(((ASN1OctetStringParser) this.authData.getEncapsulatedContentInfo().getContent(4)).getOctetStream())), new AuthAttributesProvider() {
                    public ASN1Set getAuthAttributes() {
                        try {
                            return CMSAuthenticatedDataParser.this.getAuthAttrSet();
                        } catch (IOException e) {
                            throw new IllegalStateException("can't parse authenticated attributes!");
                        }
                    }
                });
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to create digest calculator: ");
                stringBuilder.append(e.getMessage());
                throw new CMSException(stringBuilder.toString(), e);
            }
        } else {
            throw new CMSException("a digest calculator provider is required if authenticated attributes are present");
        }
    }

    public CMSAuthenticatedDataParser(byte[] bArr) throws CMSException, IOException {
        this(new ByteArrayInputStream(bArr));
    }

    public CMSAuthenticatedDataParser(byte[] bArr, DigestCalculatorProvider digestCalculatorProvider) throws CMSException, IOException {
        this(new ByteArrayInputStream(bArr), digestCalculatorProvider);
    }

    private byte[] encodeObj(ASN1Encodable aSN1Encodable) throws IOException {
        return aSN1Encodable != null ? aSN1Encodable.toASN1Primitive().getEncoded() : null;
    }

    private ASN1Set getAuthAttrSet() throws IOException {
        if (this.authAttrs == null && this.authAttrNotRead) {
            ASN1SetParser authAttrs = this.authData.getAuthAttrs();
            if (authAttrs != null) {
                this.authAttrSet = (ASN1Set) authAttrs.toASN1Primitive();
            }
            this.authAttrNotRead = false;
        }
        return this.authAttrSet;
    }

    public AttributeTable getAuthAttrs() throws IOException {
        if (this.authAttrs == null && this.authAttrNotRead) {
            ASN1Set authAttrSet = getAuthAttrSet();
            if (authAttrSet != null) {
                this.authAttrs = new AttributeTable(authAttrSet);
            }
        }
        return this.authAttrs;
    }

    public byte[] getContentDigest() {
        return this.authAttrs != null ? ASN1OctetString.getInstance(this.authAttrs.get(CMSAttributes.messageDigest).getAttrValues().getObjectAt(0)).getOctets() : null;
    }

    public byte[] getMac() throws IOException {
        if (this.mac == null) {
            getAuthAttrs();
            this.mac = this.authData.getMac().getOctets();
        }
        return Arrays.clone(this.mac);
    }

    public String getMacAlgOID() {
        return this.macAlg.getAlgorithm().toString();
    }

    public byte[] getMacAlgParams() {
        try {
            return encodeObj(this.macAlg.getParameters());
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception getting encryption parameters ");
            stringBuilder.append(e);
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public AlgorithmIdentifier getMacAlgorithm() {
        return this.macAlg;
    }

    public OriginatorInformation getOriginatorInfo() {
        return this.originatorInfo;
    }

    public RecipientInformationStore getRecipientInfos() {
        return this.recipientInfoStore;
    }

    public AttributeTable getUnauthAttrs() throws IOException {
        if (this.unauthAttrs == null && this.unauthAttrNotRead) {
            ASN1SetParser unauthAttrs = this.authData.getUnauthAttrs();
            this.unauthAttrNotRead = false;
            if (unauthAttrs != null) {
                ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
                while (true) {
                    ASN1Encodable readObject = unauthAttrs.readObject();
                    if (readObject == null) {
                        break;
                    }
                    aSN1EncodableVector.add(((ASN1SequenceParser) readObject).toASN1Primitive());
                }
                this.unauthAttrs = new AttributeTable(new DERSet(aSN1EncodableVector));
            }
        }
        return this.unauthAttrs;
    }
}
