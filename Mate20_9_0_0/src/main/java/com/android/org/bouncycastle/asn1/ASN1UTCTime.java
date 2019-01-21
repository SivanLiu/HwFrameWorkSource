package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;

public class ASN1UTCTime extends ASN1Primitive {
    private byte[] time;

    public static ASN1UTCTime getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1UTCTime)) {
            return (ASN1UTCTime) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1UTCTime) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("encoding error in getInstance: ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("illegal object in getInstance: ");
        stringBuilder2.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public static ASN1UTCTime getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Object o = obj.getObject();
        if (explicit || (o instanceof ASN1UTCTime)) {
            return getInstance(o);
        }
        return new ASN1UTCTime(((ASN1OctetString) o).getOctets());
    }

    public ASN1UTCTime(String time) {
        this.time = Strings.toByteArray(time);
        try {
            getDate();
        } catch (ParseException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid date string: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public ASN1UTCTime(Date time) {
        SimpleDateFormat dateF = new SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US);
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(dateF.format(time));
    }

    public ASN1UTCTime(Date time, Locale locale) {
        SimpleDateFormat dateF = new SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US);
        dateF.setCalendar(Calendar.getInstance(locale));
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(dateF.format(time));
    }

    ASN1UTCTime(byte[] time) {
        this.time = time;
    }

    public Date getDate() throws ParseException {
        return new SimpleDateFormat("yyMMddHHmmssz", Locale.US).parse(getTime());
    }

    public Date getAdjustedDate() throws ParseException {
        SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz", Locale.US);
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        return dateF.parse(getAdjustedTime());
    }

    public String getTime() {
        String stime = Strings.fromByteArray(this.time);
        StringBuilder stringBuilder;
        if (stime.indexOf(45) >= 0 || stime.indexOf(43) >= 0) {
            int index = stime.indexOf(45);
            if (index < 0) {
                index = stime.indexOf(43);
            }
            String d = stime;
            if (index == stime.length() - 3) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(d);
                stringBuilder2.append("00");
                d = stringBuilder2.toString();
            }
            if (index == 10) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(d.substring(0, 10));
                stringBuilder3.append("00GMT");
                stringBuilder3.append(d.substring(10, 13));
                stringBuilder3.append(":");
                stringBuilder3.append(d.substring(13, 15));
                return stringBuilder3.toString();
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(d.substring(0, 12));
            stringBuilder4.append("GMT");
            stringBuilder4.append(d.substring(12, 15));
            stringBuilder4.append(":");
            stringBuilder4.append(d.substring(15, 17));
            return stringBuilder4.toString();
        } else if (stime.length() == 11) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stime.substring(0, 10));
            stringBuilder.append("00GMT+00:00");
            return stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stime.substring(0, 12));
            stringBuilder.append("GMT+00:00");
            return stringBuilder.toString();
        }
    }

    public String getAdjustedTime() {
        String d = getTime();
        StringBuilder stringBuilder;
        if (d.charAt(0) < '5') {
            stringBuilder = new StringBuilder();
            stringBuilder.append("20");
            stringBuilder.append(d);
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("19");
        stringBuilder.append(d);
        return stringBuilder.toString();
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() {
        int length = this.time.length;
        return (1 + StreamUtil.calculateBodyLength(length)) + length;
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.write(23);
        int length = this.time.length;
        out.writeLength(length);
        for (int i = 0; i != length; i++) {
            out.write(this.time[i]);
        }
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof ASN1UTCTime) {
            return Arrays.areEqual(this.time, ((ASN1UTCTime) o).time);
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(this.time);
    }

    public String toString() {
        return Strings.fromByteArray(this.time);
    }
}
