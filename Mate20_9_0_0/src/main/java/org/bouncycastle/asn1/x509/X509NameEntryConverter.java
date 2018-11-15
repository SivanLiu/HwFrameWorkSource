package org.bouncycastle.asn1.x509;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.util.Strings;

public abstract class X509NameEntryConverter {
    protected boolean canBePrintable(String str) {
        return DERPrintableString.isPrintableString(str);
    }

    protected ASN1Primitive convertHexEncoded(String str, int i) throws IOException {
        str = Strings.toLowerCase(str);
        byte[] bArr = new byte[((str.length() - i) / 2)];
        for (int i2 = 0; i2 != bArr.length; i2++) {
            int i3 = (i2 * 2) + i;
            char charAt = str.charAt(i3);
            char charAt2 = str.charAt(i3 + 1);
            if (charAt < 'a') {
                bArr[i2] = (byte) ((charAt - 48) << 4);
            } else {
                bArr[i2] = (byte) (((charAt - 97) + 10) << 4);
            }
            if (charAt2 < 'a') {
                bArr[i2] = (byte) (((byte) (charAt2 - 48)) | bArr[i2]);
            } else {
                bArr[i2] = (byte) (((byte) ((charAt2 - 97) + 10)) | bArr[i2]);
            }
        }
        return new ASN1InputStream(bArr).readObject();
    }

    public abstract ASN1Primitive getConvertedValue(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str);
}
