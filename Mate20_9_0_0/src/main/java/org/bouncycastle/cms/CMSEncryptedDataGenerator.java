package org.bouncycastle.cms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetString;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EncryptedData;
import org.bouncycastle.operator.OutputEncryptor;

public class CMSEncryptedDataGenerator extends CMSEncryptedGenerator {
    private CMSEncryptedData doGenerate(CMSTypedData cMSTypedData, OutputEncryptor outputEncryptor) throws CMSException {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            OutputStream outputStream = outputEncryptor.getOutputStream(byteArrayOutputStream);
            cMSTypedData.write(outputStream);
            outputStream.close();
            byte[] toByteArray = byteArrayOutputStream.toByteArray();
            EncryptedContentInfo encryptedContentInfo = new EncryptedContentInfo(cMSTypedData.getContentType(), outputEncryptor.getAlgorithmIdentifier(), new BEROctetString(toByteArray));
            ASN1Set aSN1Set = null;
            if (this.unprotectedAttributeGenerator != null) {
                aSN1Set = new BERSet(this.unprotectedAttributeGenerator.getAttributes(new HashMap()).toASN1EncodableVector());
            }
            return new CMSEncryptedData(new ContentInfo(CMSObjectIdentifiers.encryptedData, new EncryptedData(encryptedContentInfo, aSN1Set)));
        } catch (IOException e) {
            throw new CMSException("");
        }
    }

    public CMSEncryptedData generate(CMSTypedData cMSTypedData, OutputEncryptor outputEncryptor) throws CMSException {
        return doGenerate(cMSTypedData, outputEncryptor);
    }
}
