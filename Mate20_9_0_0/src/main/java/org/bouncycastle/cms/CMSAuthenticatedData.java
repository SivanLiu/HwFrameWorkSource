package org.bouncycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.AuthenticatedData;
import org.bouncycastle.asn1.cms.CMSAlgorithmProtection;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Encodable;

public class CMSAuthenticatedData implements Encodable {
    private ASN1Set authAttrs;
    ContentInfo contentInfo;
    private byte[] mac;
    private AlgorithmIdentifier macAlg;
    private OriginatorInformation originatorInfo;
    RecipientInformationStore recipientInfoStore;
    private ASN1Set unauthAttrs;

    public CMSAuthenticatedData(InputStream inputStream) throws CMSException {
        this(CMSUtils.readContentInfo(inputStream));
    }

    public CMSAuthenticatedData(InputStream inputStream, DigestCalculatorProvider digestCalculatorProvider) throws CMSException {
        this(CMSUtils.readContentInfo(inputStream), digestCalculatorProvider);
    }

    public CMSAuthenticatedData(ContentInfo contentInfo) throws CMSException {
        this(contentInfo, null);
    }

    public CMSAuthenticatedData(ContentInfo contentInfo, DigestCalculatorProvider digestCalculatorProvider) throws CMSException {
        this.contentInfo = contentInfo;
        AuthenticatedData instance = AuthenticatedData.getInstance(contentInfo.getContent());
        if (instance.getOriginatorInfo() != null) {
            this.originatorInfo = new OriginatorInformation(instance.getOriginatorInfo());
        }
        ASN1Set recipientInfos = instance.getRecipientInfos();
        this.macAlg = instance.getMacAlgorithm();
        this.authAttrs = instance.getAuthAttrs();
        this.mac = instance.getMac().getOctets();
        this.unauthAttrs = instance.getUnauthAttrs();
        CMSProcessableByteArray cMSProcessableByteArray = new CMSProcessableByteArray(ASN1OctetString.getInstance(instance.getEncapsulatedContentInfo().getContent()).getOctets());
        if (this.authAttrs == null) {
            this.recipientInfoStore = CMSEnvelopedHelper.buildRecipientInformationStore(recipientInfos, this.macAlg, new CMSAuthenticatedSecureReadable(this.macAlg, cMSProcessableByteArray));
        } else if (digestCalculatorProvider != null) {
            ASN1EncodableVector all = new AttributeTable(this.authAttrs).getAll(CMSAttributes.cmsAlgorithmProtect);
            if (all.size() <= 1) {
                if (all.size() > 0) {
                    Attribute instance2 = Attribute.getInstance(all.get(0));
                    if (instance2.getAttrValues().size() == 1) {
                        CMSAlgorithmProtection instance3 = CMSAlgorithmProtection.getInstance(instance2.getAttributeValues()[0]);
                        if (!CMSUtils.isEquivalent(instance3.getDigestAlgorithm(), instance.getDigestAlgorithm())) {
                            throw new CMSException("CMS Algorithm Identifier Protection check failed for digestAlgorithm");
                        } else if (!CMSUtils.isEquivalent(instance3.getMacAlgorithm(), this.macAlg)) {
                            throw new CMSException("CMS Algorithm Identifier Protection check failed for macAlgorithm");
                        }
                    }
                    throw new CMSException("A cmsAlgorithmProtect attribute MUST contain exactly one value");
                }
                try {
                    this.recipientInfoStore = CMSEnvelopedHelper.buildRecipientInformationStore(recipientInfos, this.macAlg, new CMSDigestAuthenticatedSecureReadable(digestCalculatorProvider.get(instance.getDigestAlgorithm()), cMSProcessableByteArray), new AuthAttributesProvider() {
                        public ASN1Set getAuthAttributes() {
                            return CMSAuthenticatedData.this.authAttrs;
                        }
                    });
                    return;
                } catch (OperatorCreationException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to create digest calculator: ");
                    stringBuilder.append(e.getMessage());
                    throw new CMSException(stringBuilder.toString(), e);
                }
            }
            throw new CMSException("Only one instance of a cmsAlgorithmProtect attribute can be present");
        } else {
            throw new CMSException("a digest calculator provider is required if authenticated attributes are present");
        }
    }

    public CMSAuthenticatedData(byte[] bArr) throws CMSException {
        this(CMSUtils.readContentInfo(bArr));
    }

    public CMSAuthenticatedData(byte[] bArr, DigestCalculatorProvider digestCalculatorProvider) throws CMSException {
        this(CMSUtils.readContentInfo(bArr), digestCalculatorProvider);
    }

    private byte[] encodeObj(ASN1Encodable aSN1Encodable) throws IOException {
        return aSN1Encodable != null ? aSN1Encodable.toASN1Primitive().getEncoded() : null;
    }

    public AttributeTable getAuthAttrs() {
        return this.authAttrs == null ? null : new AttributeTable(this.authAttrs);
    }

    public byte[] getContentDigest() {
        return this.authAttrs != null ? ASN1OctetString.getInstance(getAuthAttrs().get(CMSAttributes.messageDigest).getAttrValues().getObjectAt(0)).getOctets() : null;
    }

    public ContentInfo getContentInfo() {
        return this.contentInfo;
    }

    public byte[] getEncoded() throws IOException {
        return this.contentInfo.getEncoded();
    }

    public byte[] getMac() {
        return Arrays.clone(this.mac);
    }

    public String getMacAlgOID() {
        return this.macAlg.getAlgorithm().getId();
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

    public AttributeTable getUnauthAttrs() {
        return this.unauthAttrs == null ? null : new AttributeTable(this.unauthAttrs);
    }

    public ContentInfo toASN1Structure() {
        return this.contentInfo;
    }
}
