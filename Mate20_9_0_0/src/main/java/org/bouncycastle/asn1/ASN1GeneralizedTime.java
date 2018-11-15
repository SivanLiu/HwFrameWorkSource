package org.bouncycastle.asn1;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import org.bouncycastle.pqc.math.linearalgebra.Matrix;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class ASN1GeneralizedTime extends ASN1Primitive {
    protected byte[] time;

    public ASN1GeneralizedTime(String str) {
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

    public ASN1GeneralizedTime(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(simpleDateFormat.format(date));
    }

    public ASN1GeneralizedTime(Date date, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'", locale);
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(simpleDateFormat.format(date));
    }

    ASN1GeneralizedTime(byte[] bArr) {
        this.time = bArr;
    }

    private String calculateGMTOffset() {
        String str = "+";
        TimeZone timeZone = TimeZone.getDefault();
        int rawOffset = timeZone.getRawOffset();
        if (rawOffset < 0) {
            str = "-";
            rawOffset = -rawOffset;
        }
        int i = rawOffset / 3600000;
        rawOffset = (rawOffset - (((i * 60) * 60) * 1000)) / 60000;
        try {
            if (timeZone.useDaylightTime() && timeZone.inDaylightTime(getDate())) {
                i += str.equals("+") ? 1 : -1;
            }
        } catch (ParseException e) {
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GMT");
        stringBuilder.append(str);
        stringBuilder.append(convert(i));
        stringBuilder.append(":");
        stringBuilder.append(convert(rawOffset));
        return stringBuilder.toString();
    }

    private String convert(int i) {
        if (i >= 10) {
            return Integer.toString(i);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0");
        stringBuilder.append(i);
        return stringBuilder.toString();
    }

    public static ASN1GeneralizedTime getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1GeneralizedTime)) {
            return (ASN1GeneralizedTime) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1GeneralizedTime) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static ASN1GeneralizedTime getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof ASN1GeneralizedTime)) ? getInstance(object) : new ASN1GeneralizedTime(((ASN1OctetString) object).getOctets());
    }

    private boolean isDigit(int i) {
        return this.time.length > i && this.time[i] >= (byte) 48 && this.time[i] <= (byte) 57;
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        return !(aSN1Primitive instanceof ASN1GeneralizedTime) ? false : Arrays.areEqual(this.time, ((ASN1GeneralizedTime) aSN1Primitive).time);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(24, this.time);
    }

    int encodedLength() {
        int length = this.time.length;
        return (1 + StreamUtil.calculateBodyLength(length)) + length;
    }

    public Date getDate() throws ParseException {
        SimpleDateFormat simpleDateFormat;
        TimeZone simpleTimeZone;
        String fromByteArray = Strings.fromByteArray(this.time);
        if (fromByteArray.endsWith("Z")) {
            simpleDateFormat = hasFractionalSeconds() ? new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'") : hasSeconds() ? new SimpleDateFormat("yyyyMMddHHmmss'Z'") : hasMinutes() ? new SimpleDateFormat("yyyyMMddHHmm'Z'") : new SimpleDateFormat("yyyyMMddHH'Z'");
            simpleTimeZone = new SimpleTimeZone(0, "Z");
        } else if (fromByteArray.indexOf(45) > 0 || fromByteArray.indexOf(43) > 0) {
            fromByteArray = getTime();
            simpleDateFormat = hasFractionalSeconds() ? new SimpleDateFormat("yyyyMMddHHmmss.SSSz") : hasSeconds() ? new SimpleDateFormat("yyyyMMddHHmmssz") : hasMinutes() ? new SimpleDateFormat("yyyyMMddHHmmz") : new SimpleDateFormat("yyyyMMddHHz");
            simpleTimeZone = new SimpleTimeZone(0, "Z");
        } else {
            simpleDateFormat = hasFractionalSeconds() ? new SimpleDateFormat("yyyyMMddHHmmss.SSS") : hasSeconds() ? new SimpleDateFormat("yyyyMMddHHmmss") : hasMinutes() ? new SimpleDateFormat("yyyyMMddHHmm") : new SimpleDateFormat("yyyyMMddHH");
            simpleTimeZone = new SimpleTimeZone(0, TimeZone.getDefault().getID());
        }
        simpleDateFormat.setTimeZone(simpleTimeZone);
        if (hasFractionalSeconds()) {
            StringBuilder stringBuilder;
            String substring = fromByteArray.substring(14);
            int i = 1;
            while (i < substring.length()) {
                char charAt = substring.charAt(i);
                if ('0' > charAt || charAt > '9') {
                    break;
                }
                i++;
            }
            int i2 = i - 1;
            if (i2 > 3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(substring.substring(0, 4));
                stringBuilder.append(substring.substring(i));
                substring = stringBuilder.toString();
                stringBuilder = new StringBuilder();
            } else if (i2 == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(substring.substring(0, i));
                stringBuilder.append("00");
                stringBuilder.append(substring.substring(i));
                substring = stringBuilder.toString();
                stringBuilder = new StringBuilder();
            } else if (i2 == 2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(substring.substring(0, i));
                stringBuilder.append("0");
                stringBuilder.append(substring.substring(i));
                substring = stringBuilder.toString();
                stringBuilder = new StringBuilder();
            }
            stringBuilder.append(fromByteArray.substring(0, 14));
            stringBuilder.append(substring);
            fromByteArray = stringBuilder.toString();
        }
        return simpleDateFormat.parse(fromByteArray);
    }

    public String getTime() {
        String fromByteArray = Strings.fromByteArray(this.time);
        StringBuilder stringBuilder;
        if (fromByteArray.charAt(fromByteArray.length() - 1) == Matrix.MATRIX_TYPE_ZERO) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(fromByteArray.substring(0, fromByteArray.length() - 1));
            stringBuilder.append("GMT+00:00");
            return stringBuilder.toString();
        }
        int length = fromByteArray.length() - 5;
        char charAt = fromByteArray.charAt(length);
        StringBuilder stringBuilder2;
        if (charAt == '-' || charAt == '+') {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(fromByteArray.substring(0, length));
            stringBuilder2.append("GMT");
            int i = length + 3;
            stringBuilder2.append(fromByteArray.substring(length, i));
            stringBuilder2.append(":");
            stringBuilder2.append(fromByteArray.substring(i));
            return stringBuilder2.toString();
        }
        length = fromByteArray.length() - 3;
        charAt = fromByteArray.charAt(length);
        if (charAt == '-' || charAt == '+') {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(fromByteArray.substring(0, length));
            stringBuilder2.append("GMT");
            stringBuilder2.append(fromByteArray.substring(length));
            stringBuilder2.append(":00");
            return stringBuilder2.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(fromByteArray);
        stringBuilder.append(calculateGMTOffset());
        return stringBuilder.toString();
    }

    public String getTimeString() {
        return Strings.fromByteArray(this.time);
    }

    protected boolean hasFractionalSeconds() {
        int i = 0;
        while (i != this.time.length) {
            if (this.time[i] == (byte) 46 && i == 14) {
                return true;
            }
            i++;
        }
        return false;
    }

    protected boolean hasMinutes() {
        return isDigit(10) && isDigit(11);
    }

    protected boolean hasSeconds() {
        return isDigit(12) && isDigit(13);
    }

    public int hashCode() {
        return Arrays.hashCode(this.time);
    }

    boolean isConstructed() {
        return false;
    }

    ASN1Primitive toDERObject() {
        return new DERGeneralizedTime(this.time);
    }
}
