package org.bouncycastle.cms;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetString;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.AuthenticatedData;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.MacCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.io.TeeOutputStream;

public class CMSAuthenticatedDataGenerator extends CMSAuthenticatedGenerator {
    public CMSAuthenticatedData generate(CMSTypedData cMSTypedData, MacCalculator macCalculator) throws CMSException {
        return generate(cMSTypedData, macCalculator, null);
    }

    public CMSAuthenticatedData generate(CMSTypedData cMSTypedData, MacCalculator macCalculator, DigestCalculator digestCalculator) throws CMSException {
        ASN1Encodable bEROctetString;
        CMSTypedData cMSTypedData2 = cMSTypedData;
        final DigestCalculator digestCalculator2 = digestCalculator;
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (RecipientInfoGenerator generate : this.recipientInfoGenerators) {
            aSN1EncodableVector.add(generate.generate(macCalculator.getKey()));
        }
        ASN1Set aSN1Set = null;
        OutputStream byteArrayOutputStream;
        OutputStream teeOutputStream;
        ASN1Encodable bEROctetString2;
        if (digestCalculator2 != null) {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                teeOutputStream = new TeeOutputStream(digestCalculator.getOutputStream(), byteArrayOutputStream);
                cMSTypedData2.write(teeOutputStream);
                teeOutputStream.close();
                bEROctetString2 = new BEROctetString(byteArrayOutputStream.toByteArray());
                Map baseParameters = getBaseParameters(cMSTypedData.getContentType(), digestCalculator.getAlgorithmIdentifier(), macCalculator.getAlgorithmIdentifier(), digestCalculator.getDigest());
                if (this.authGen == null) {
                    this.authGen = new DefaultAuthenticatedAttributeTableGenerator();
                }
                ASN1Set dERSet = new DERSet(this.authGen.getAttributes(Collections.unmodifiableMap(baseParameters)).toASN1EncodableVector());
                try {
                    byteArrayOutputStream = macCalculator.getOutputStream();
                    byteArrayOutputStream.write(dERSet.getEncoded(ASN1Encoding.DER));
                    byteArrayOutputStream.close();
                    ASN1OctetString dEROctetString = new DEROctetString(macCalculator.getMac());
                    if (this.unauthGen != null) {
                        aSN1Set = new BERSet(this.unauthGen.getAttributes(Collections.unmodifiableMap(baseParameters)).toASN1EncodableVector());
                    }
                    ASN1Set aSN1Set2 = aSN1Set;
                    ASN1Encodable authenticatedData = new AuthenticatedData(this.originatorInfo, new DERSet(aSN1EncodableVector), macCalculator.getAlgorithmIdentifier(), digestCalculator.getAlgorithmIdentifier(), new ContentInfo(CMSObjectIdentifiers.data, bEROctetString2), dERSet, dEROctetString, aSN1Set2);
                } catch (Exception e) {
                    throw new CMSException("exception decoding algorithm parameters.", e);
                }
            } catch (Exception e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to perform digest calculation: ");
                stringBuilder.append(e2.getMessage());
                throw new CMSException(stringBuilder.toString(), e2);
            }
        }
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            teeOutputStream = new TeeOutputStream(byteArrayOutputStream, macCalculator.getOutputStream());
            cMSTypedData2.write(teeOutputStream);
            teeOutputStream.close();
            bEROctetString = new BEROctetString(byteArrayOutputStream.toByteArray());
            ASN1OctetString dEROctetString2 = new DEROctetString(macCalculator.getMac());
            if (this.unauthGen != null) {
                aSN1Set = new BERSet(this.unauthGen.getAttributes(new HashMap()).toASN1EncodableVector());
            }
            ASN1Set aSN1Set3 = aSN1Set;
            bEROctetString2 = new AuthenticatedData(this.originatorInfo, new DERSet(aSN1EncodableVector), macCalculator.getAlgorithmIdentifier(), null, new ContentInfo(CMSObjectIdentifiers.data, bEROctetString), null, dEROctetString2, aSN1Set3);
        } catch (Exception e22) {
            throw new CMSException("exception decoding algorithm parameters.", e22);
        }
        return new CMSAuthenticatedData(new ContentInfo(CMSObjectIdentifiers.authenticatedData, bEROctetString), new DigestCalculatorProvider() {
            public DigestCalculator get(AlgorithmIdentifier algorithmIdentifier) throws OperatorCreationException {
                return digestCalculator2;
            }
        });
    }
}
