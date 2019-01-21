package org.bouncycastle.cms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetString;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EnvelopedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.GenericKey;
import org.bouncycastle.operator.OutputEncryptor;

public class CMSEnvelopedDataGenerator extends CMSEnvelopedGenerator {
    private CMSEnvelopedData doGenerate(CMSTypedData cMSTypedData, OutputEncryptor outputEncryptor) throws CMSException {
        if (this.oldRecipientInfoGenerators.isEmpty()) {
            ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                OutputStream outputStream = outputEncryptor.getOutputStream(byteArrayOutputStream);
                cMSTypedData.write(outputStream);
                outputStream.close();
                byte[] toByteArray = byteArrayOutputStream.toByteArray();
                AlgorithmIdentifier algorithmIdentifier = outputEncryptor.getAlgorithmIdentifier();
                BEROctetString bEROctetString = new BEROctetString(toByteArray);
                GenericKey key = outputEncryptor.getKey();
                for (RecipientInfoGenerator generate : this.recipientInfoGenerators) {
                    aSN1EncodableVector.add(generate.generate(key));
                }
                EncryptedContentInfo encryptedContentInfo = new EncryptedContentInfo(cMSTypedData.getContentType(), algorithmIdentifier, bEROctetString);
                ASN1Set aSN1Set = null;
                if (this.unprotectedAttributeGenerator != null) {
                    aSN1Set = new BERSet(this.unprotectedAttributeGenerator.getAttributes(new HashMap()).toASN1EncodableVector());
                }
                return new CMSEnvelopedData(new ContentInfo(CMSObjectIdentifiers.envelopedData, new EnvelopedData(this.originatorInfo, new DERSet(aSN1EncodableVector), encryptedContentInfo, aSN1Set)));
            } catch (IOException e) {
                throw new CMSException("");
            }
        }
        throw new IllegalStateException("can only use addRecipientGenerator() with this method");
    }

    public CMSEnvelopedData generate(CMSTypedData cMSTypedData, OutputEncryptor outputEncryptor) throws CMSException {
        return doGenerate(cMSTypedData, outputEncryptor);
    }
}
