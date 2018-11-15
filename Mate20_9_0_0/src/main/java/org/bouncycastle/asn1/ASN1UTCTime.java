package org.bouncycastle.asn1;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class ASN1UTCTime extends ASN1Primitive {
    private byte[] time;

    public ASN1UTCTime(String str) {
        this.time = Strings.toByteArray(str);
        try {
            getDate();
        } catch (ParseException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid date string: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public ASN1UTCTime(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss'Z'");
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(simpleDateFormat.format(date));
    }

    public ASN1UTCTime(Date date, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss'Z'", locale);
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(simpleDateFormat.format(date));
    }

    ASN1UTCTime(byte[] bArr) {
        this.time = bArr;
    }

    public static ASN1UTCTime getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1UTCTime)) {
            return (ASN1UTCTime) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1UTCTime) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("encoding error in getInstance: ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ASN1UTCTime getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof ASN1UTCTime)) ? getInstance(object) : new ASN1UTCTime(((ASN1OctetString) object).getOctets());
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        return !(aSN1Primitive instanceof ASN1UTCTime) ? false : Arrays.areEqual(this.time, ((ASN1UTCTime) aSN1Primitive).time);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.write(23);
        int length = this.time.length;
        aSN1OutputStream.writeLength(length);
        for (int i = 0; i != length; i++) {
            aSN1OutputStream.write(this.time[i]);
        }
    }

    int encodedLength() {
        int length = this.time.length;
        return (1 + StreamUtil.calculateBodyLength(length)) + length;
    }

    public Date getAdjustedDate() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssz");
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        return simpleDateFormat.parse(getAdjustedTime());
    }

    public String getAdjustedTime() {
        StringBuilder stringBuilder;
        String str;
        String time = getTime();
        if (time.charAt(0) < '5') {
            stringBuilder = new StringBuilder();
            str = "20";
        } else {
            stringBuilder = new StringBuilder();
            str = "19";
        }
        stringBuilder.append(str);
        stringBuilder.append(time);
        return stringBuilder.toString();
    }

    public Date getDate() throws ParseException {
        return new SimpleDateFormat("yyMMddHHmmssz").parse(getTime());
    }

    public String getTime() {
        StringBuilder stringBuilder;
        String fromByteArray = Strings.fromByteArray(this.time);
        if (fromByteArray.indexOf(45) >= 0 || fromByteArray.indexOf(43) >= 0) {
            int indexOf = fromByteArray.indexOf(45);
            if (indexOf < 0) {
                indexOf = fromByteArray.indexOf(43);
            }
            if (indexOf == fromByteArray.length() - 3) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(fromByteArray);
                stringBuilder2.append("00");
                fromByteArray = stringBuilder2.toString();
            }
            if (indexOf == 10) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(fromByteArray.substring(0, 10));
                stringBuilder.append("00GMT");
                stringBuilder.append(fromByteArray.substring(10, 13));
                stringBuilder.append(":");
                fromByteArray = fromByteArray.substring(13, 15);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(fromByteArray.substring(0, 12));
                stringBuilder.append("GMT");
                stringBuilder.append(fromByteArray.substring(12, 15));
                stringBuilder.append(":");
                fromByteArray = fromByteArray.substring(15, 17);
            }
        } else if (fromByteArray.length() == 11) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(fromByteArray.substring(0, 10));
            fromByteArray = "00GMT+00:00";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(fromByteArray.substring(0, 12));
            fromByteArray = "GMT+00:00";
        }
        stringBuilder.append(fromByteArray);
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Arrays.hashCode(this.time);
    }

    boolean isConstructed() {
        return false;
    }

    public String toString() {
        return Strings.fromByteArray(this.time);
    }
}
