package org.bouncycastle.jce;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.MacData;
import org.bouncycastle.asn1.pkcs.Pfx;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;

public class PKCS12Util {
    private static byte[] calculatePbeMac(ASN1ObjectIdentifier aSN1ObjectIdentifier, byte[] bArr, int i, char[] cArr, byte[] bArr2, String str) throws Exception {
        SecretKeyFactory instance = SecretKeyFactory.getInstance(aSN1ObjectIdentifier.getId(), str);
        AlgorithmParameterSpec pBEParameterSpec = new PBEParameterSpec(bArr, i);
        Key generateSecret = instance.generateSecret(new PBEKeySpec(cArr));
        Mac instance2 = Mac.getInstance(aSN1ObjectIdentifier.getId(), str);
        instance2.init(generateSecret, pBEParameterSpec);
        instance2.update(bArr2);
        return instance2.doFinal();
    }

    public static byte[] convertToDefiniteLength(byte[] bArr) throws IOException {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DEROutputStream dEROutputStream = new DEROutputStream(byteArrayOutputStream);
        ASN1Encodable instance = Pfx.getInstance(bArr);
        byteArrayOutputStream.reset();
        dEROutputStream.writeObject(instance);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] convertToDefiniteLength(byte[] bArr, char[] cArr, String str) throws IOException {
        Pfx instance = Pfx.getInstance(bArr);
        ContentInfo authSafe = instance.getAuthSafe();
        ASN1OctetString instance2 = ASN1OctetString.getInstance(authSafe.getContent());
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DEROutputStream dEROutputStream = new DEROutputStream(byteArrayOutputStream);
        dEROutputStream.writeObject(new ASN1InputStream(instance2.getOctets()).readObject());
        ContentInfo contentInfo = new ContentInfo(authSafe.getContentType(), new DEROctetString(byteArrayOutputStream.toByteArray()));
        MacData macData = instance.getMacData();
        try {
            int intValue = macData.getIterationCount().intValue();
            ASN1Encodable pfx = new Pfx(contentInfo, new MacData(new DigestInfo(new AlgorithmIdentifier(macData.getMac().getAlgorithmId().getAlgorithm(), DERNull.INSTANCE), calculatePbeMac(macData.getMac().getAlgorithmId().getAlgorithm(), macData.getSalt(), intValue, cArr, ASN1OctetString.getInstance(contentInfo.getContent()).getOctets(), str)), macData.getSalt(), intValue));
            byteArrayOutputStream.reset();
            dEROutputStream.writeObject(pfx);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error constructing MAC: ");
            stringBuilder.append(e.toString());
            throw new IOException(stringBuilder.toString());
        }
    }
}
