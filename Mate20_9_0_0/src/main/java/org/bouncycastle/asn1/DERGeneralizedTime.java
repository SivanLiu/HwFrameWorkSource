package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Date;
import org.bouncycastle.util.Strings;

public class DERGeneralizedTime extends ASN1GeneralizedTime {
    public DERGeneralizedTime(String str) {
        super(str);
    }

    public DERGeneralizedTime(Date date) {
        super(date);
    }

    public DERGeneralizedTime(byte[] bArr) {
        super(bArr);
    }

    private byte[] getDERTime() {
        if (this.time[this.time.length - 1] != (byte) 90) {
            return this.time;
        }
        Object obj;
        Object toByteArray;
        int length;
        int i;
        if (!hasMinutes()) {
            obj = new byte[(this.time.length + 4)];
            System.arraycopy(this.time, 0, obj, 0, this.time.length - 1);
            toByteArray = Strings.toByteArray("0000Z");
            length = this.time.length - 1;
            i = 5;
        } else if (!hasSeconds()) {
            obj = new byte[(this.time.length + 2)];
            System.arraycopy(this.time, 0, obj, 0, this.time.length - 1);
            toByteArray = Strings.toByteArray("00Z");
            length = this.time.length - 1;
            i = 3;
        } else if (!hasFractionalSeconds()) {
            return this.time;
        } else {
            int length2 = this.time.length - 2;
            while (length2 > 0 && this.time[length2] == (byte) 48) {
                length2--;
            }
            Object obj2;
            if (this.time[length2] == (byte) 46) {
                obj2 = new byte[(length2 + 1)];
                System.arraycopy(this.time, 0, obj2, 0, length2);
                obj2[length2] = 90;
                return obj2;
            }
            obj2 = new byte[(length2 + 2)];
            length2++;
            System.arraycopy(this.time, 0, obj2, 0, length2);
            obj2[length2] = 90;
            return obj2;
        }
        System.arraycopy(toByteArray, 0, obj, length, i);
        return obj;
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(24, getDERTime());
    }

    int encodedLength() {
        int length = getDERTime().length;
        return (1 + StreamUtil.calculateBodyLength(length)) + length;
    }
}
